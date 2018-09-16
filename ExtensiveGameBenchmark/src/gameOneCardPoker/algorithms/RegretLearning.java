package gameOneCardPoker.algorithms;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import gameOneCardPoker.Action;
import gameOneCardPoker.Card;
import gameOneCardPoker.GameState;
import gameOneCardPoker.Helper;
import gameOneCardPoker.InformationSet;
import gameOneCardPoker.OneCardPoker;

/**
 * just have a try
 * @author dearhude1
 *
 */
public class RegretLearning extends CFR_Series {

	
	private double ALPHA = 0.05;
	private double GAMMA = 1;
	private double EPSILON = 0.05;
	
	private double[][][] Nash_policy;
	
	public RegretLearning()
	{
		super();
		
		Nash_policy = new double[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		
		/**
		 * the variables from the super class are enough!
		 */
		
		readNash();
	}
	
	public RegretLearning( double alpha, double gamma, double epsilon )
	{
		super();
		
		ALPHA = alpha;
		GAMMA = gamma;
		EPSILON = epsilon;
	}
	
	
	public void initSelfPlay()
	{
		for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
			
			for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
				
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					policy[rank][hisID][a] = 0.5;
					average_policy[rank][hisID][a] = 0.5;
					
					regret[rank][hisID][a] = 0.0; 
				}
			}
		}
	}
	
	public void selfPlay()
	{
		
		initSelfPlay();
		
		long startTime = System.nanoTime();
		long lastTime = startTime;
		long currentTime = startTime;
		ArrayList<Double> epsilonList = new ArrayList<Double>();
		
		int c = 0;
		long T = 0;
		while( (currentTime - startTime) < 100000000000L ) { //for( int T = 1; T <= 10000000; T++ ) {
			
			/**
			 * initialize game state
			 */
			GameState gameState = new GameState();
			
			/**
			 * deal cards
			 */
			Random random = new Random();
			ArrayList<Integer> deck = new ArrayList<Integer>();
			for( int card = Card.FIRST_CARD_INDEX; card <= Card.LAST_CARD_INDEX; card++ )
				deck.add( card );
			int card1 = deck.remove( random.nextInt( deck.size() ) );
			int card2 = deck.remove( random.nextInt( deck.size() ) );
			
			gameState.receiveCard( 0, card1 );
			gameState.receiveCard( 1, card2 );
			
			/**
			 * one RL episode
			 */
			episode_RegretLearning( gameState, T );
			
			/**
			 * updating??
			 */
			currentTime = System.nanoTime();
			if( currentTime - lastTime > 10000000L ) {
				
				long count = (currentTime - lastTime) / 10000000L;
				Helper.computeAverageStrategy( acc_policy, average_policy );
				
				//double v1 = Helper.computeStrategyValue( 1, average_policy, Nash_policy );
				//System.out.println( "value1: "+v1 );
				
				double ep = Helper.computeExploitability( average_policy );
				//for( int i = 0; i < count; i++ )
					//epsilonList.add(ep);
				
				lastTime = currentTime;
			}
		}
		long estimatedTime = System.nanoTime() - startTime;
		System.out.println( estimatedTime );
		
		
		Helper.displayPolicy( average_policy );
		
		/**
		 * write to file
		 */
		try {
			
			String fileName = "./epsilons";
			fileName += ".csv";
			
			BufferedWriter bufWriter = new BufferedWriter(new FileWriter(fileName));
			System.out.println(epsilonList.size());
			while( epsilonList.size() > 0 ) {
				
				bufWriter.write( epsilonList.remove(0)+", " );
			}
			
			bufWriter.close();
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
	}
	
	
	private void episode_RegretLearning( GameState state, long T )
	{
		
		GameState nextState = null;
		
		/**
		 * get the current information set for each player
		 */
		int hisID = OneCardPoker.historyID( state );
		int[] ranks = new int[2];
		ranks[0] = Card.rankOfCard(state.getPlayerCard(0));
		ranks[1] = Card.rankOfCard(state.getPlayerCard(1));
		
		InformationSet[] I = new InformationSet[2];
		I[0] = new InformationSet( ranks[0], hisID );
		I[1] = new InformationSet( ranks[1], hisID );
		
		//for storage
		ArrayList<GameState> states[] = new ArrayList[2];
		ArrayList<Integer> actions[] = new ArrayList[2]; 
		states[0] = new ArrayList<GameState>();
		states[1] = new ArrayList<GameState>();
		actions[0] = new ArrayList<Integer>();
		actions[1] = new ArrayList<Integer>();
		
		while( !state.isGameOver() ) {
			
			
			/**
			 * get the acting player 
			 * and take an action according to the current policy with epsilon-greedy
			 */
			int actingPlayer = state.getActingPlayer();
			int curAction = 0;
			double[] pro = new double[Action.NUM_ACTION_TYPES];
			if( new Random().nextDouble() < EPSILON ) {
				
				pro[0] = 1.0 / Action.NUM_ACTION_TYPES;
				for( int a = 1; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					pro[a] = pro[a-1] + 1.0 / Action.NUM_ACTION_TYPES;
				}
			}
			else {
				
				//pro[0] = average_policy[I[actingPlayer].getCardRank()][hisID][0];
				pro[0] = policy[I[actingPlayer].getCardRank()][hisID][0];
				for( int a = 1; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					//pro[a] = pro[a-1] + average_policy[I[actingPlayer].getCardRank()][hisID][a];
					pro[a] = pro[a-1] + policy[I[actingPlayer].getCardRank()][hisID][a];
				}	
			}
			
			double playerPro = new Random().nextDouble();
			for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
				
				if( playerPro < pro[a] ) {
					
					curAction = a;
					break;
				}
			}
			
			//store the state and action
			states[actingPlayer].add( state );
			actions[actingPlayer].add( curAction );
			
			/**
			 * then observe the next state and reward
			 */
			nextState = new GameState( state );
			OneCardPoker.doAction( nextState, curAction );

			int nextHisID = OneCardPoker.historyID( nextState );
			InformationSet I1p = new InformationSet( Card.rankOfCard(nextState.getPlayerCard(0)), nextHisID );
			InformationSet I2p = new InformationSet( Card.rankOfCard(nextState.getPlayerCard(1)), nextHisID );
			
			
			/**
			 * then we change to the next state
			 */
			state = null;
			state = nextState;
			hisID = nextHisID;
			I[0] = null;
			I[0] = I1p;
			I[1] = null;
			I[1] = I2p;
			
		}
		
		
		/**
		 * then we update the regret
		 */
		double learningRate = 1.0 / ((double) (T+1));
		
		for( int playerSeat = 0; playerSeat <= 1; playerSeat++ ) {
			
			nextState = state;
			int nextHisID = OneCardPoker.historyID( nextState );
			int nextAction = 0;
			
			double v_Ia = OneCardPoker.getUtility( nextState, playerSeat );
			
			while( !states[playerSeat].isEmpty() ) {
				
				GameState curState = states[playerSeat].remove(states[playerSeat].size()-1);
				int curAction = actions[playerSeat].remove(actions[playerSeat].size()-1);
				int curHisID = OneCardPoker.historyID(curState);
				
				double reward = 0.0;
				if( nextState.isGameOver() )
					reward = OneCardPoker.getUtility( nextState, playerSeat );
				
				//for immediate regret
				double v_I = v_Ia * policy[ranks[playerSeat]][curHisID][curAction];
				
				//update regret
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					double R = regret[ranks[playerSeat]][curHisID][a];
					
					if( a == curAction && reward > 0 )
						R = ( 1 - ALPHA ) * R + ALPHA * ( reward + GAMMA * regret[ranks[playerSeat]][nextHisID][nextAction] );
						//R = ( 1 - ALPHA ) * R + ALPHA * ( v_Ia - v_I );
						//R = ( 1 - ALPHA ) * R + ALPHA * ( v_Ia - v_I + GAMMA * regret[ranks[playerSeat]][nextHisID][nextAction] );
					else
						//R = ( 1 - ALPHA ) * R + ALPHA * ( 0 - v_I );
						R = ( 1 - ALPHA ) * R;
					
					regret[ranks[playerSeat]][curHisID][a] = R;
				}
				
				
				nextState = curState;
				nextHisID = curHisID;
				nextAction = curAction;
				
				v_Ia = v_I;
			}
		}
		
		/**
		 * update strategy immediately
		 */
		GameState gameState = new GameState();
		gameState.receiveCard( 0, Card.NUM_SUITS * ranks[0] );
		gameState.receiveCard( 1, Card.NUM_SUITS * ranks[1] );
		walkTrees_ComputeStrategy( 1, gameState, 1, 1 );

	}
	
	
	
	private void walkTrees_ComputeStrategy( int currentPlayer, GameState state, 
			double p1, double p2 )
	{
		
		int hisID = OneCardPoker.historyID( state );
		int rank1 = Card.rankOfCard( state.getPlayerCard(0) );
		int rank2 = Card.rankOfCard( state.getPlayerCard(1) );
		
		OneCardPoker.checkOver(state);
		
		if( state.isGameOver() ) {
			
			return;
		}
		else {
			
			if( currentPlayer == 1 ) {
				
				/**
				 * regret matching
				 */
				double sumPositiveRegret = 0.0;
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					double posRegret = regret[rank1][hisID][a];
					if( posRegret < 0.00001 )
						posRegret = 0;
					sumPositiveRegret += posRegret;
				}
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					double posRegret = regret[rank1][hisID][a];
					if( posRegret < 0.00001 )
						posRegret = 0;
					if( sumPositiveRegret < 0.00001 )
						policy[rank1][hisID][a] = 1.0 / Action.NUM_ACTION_TYPES;
					else
						policy[rank1][hisID][a] = posRegret / sumPositiveRegret;
				}
				
				/**
				 * try each action
				 */
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					GameState nextState = new GameState( state );
					OneCardPoker.doAction( nextState, a );
					
					//recursive
					int nextPlayer = 2;
					
					double p1_next = p1 * policy[rank1][hisID][a];
					walkTrees_ComputeStrategy( nextPlayer, nextState, p1_next, p2);
					
					nextState = null;
				}
				
				/**
				 * update accumulative policy
				 */
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					acc_policy[rank1][hisID][a] += p1 * policy[rank1][hisID][a];
				}
			}
			else {
				
				/**
				 * regret matching
				 */
				double sumPositiveRegret = 0.0;
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					double posRegret = regret[rank2][hisID][a];
					if( posRegret < 0.00001 )
						posRegret = 0;
					sumPositiveRegret += posRegret;
				}
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					double posRegret = regret[rank2][hisID][a];
					if( posRegret < 0.00001 )
						posRegret = 0;
					if( sumPositiveRegret < 0.00001 )
						policy[rank2][hisID][a] = 1.0 / Action.NUM_ACTION_TYPES;
					else
						policy[rank2][hisID][a] = posRegret / sumPositiveRegret;
				}
				
				/**
				 * try each action
				 */
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					GameState nextState = new GameState( state );
					OneCardPoker.doAction( nextState, a );
					
					//recursive
					int nextPlayer = 1;
					
					double p2_next = p2 * policy[rank2][hisID][a];
					walkTrees_ComputeStrategy( nextPlayer, nextState, p1, p2_next );
					
					nextState = null;
				}
				
				/**
				 * update accumulative policy
				 */
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					acc_policy[rank2][hisID][a] += p2 * policy[rank2][hisID][a];
				}
			}
		}
	}
	
	
	 protected void readNash()
	 {
		 try {
			 
			 BufferedReader bufReader = new BufferedReader(new FileReader("./Nash.txt"));
			 
			 String line = "";
			 int rank = 0;
			 int his = 0;
			 int a = 0;
			 while( (line = bufReader.readLine()) != null ) {
					
				 if( line.length() == 0 )
					 continue;
					
				 Nash_policy[rank][his][a] = Double.parseDouble(line);
					
				 /**
				  * for testing best response computing
				  */
				 //opponentStrategy[rank][his][a] = 0.5;//Nash_policy[rank][his][a];
				 
				 a++;
				 if( a >= Action.NUM_ACTION_TYPES ) {
						
					 a = 0;
					 his++;
					 if( his >= OneCardPoker.NUM_HISTORY_IDS ) {
							
						 his = 0;
						 rank++;
					 }
				 }
			 }
				
			 bufReader.close();
		 }
		 catch(IOException ioe)
		 {
			 ioe.printStackTrace();
		 }
	 }
	
	
}
