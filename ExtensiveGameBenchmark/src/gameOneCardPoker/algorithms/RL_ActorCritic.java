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

public class RL_ActorCritic extends Algorithm {

	
	private double ALPHA = 0.1;
	private double GAMMA = 0.9;
	private double EPSILON = 0.01;
	private double BETA = 0.8;
	
	/**
	 * policy for agents
	 * pi(s,a)
	 */
	protected double[][][] policy;
	
	/**
	 * state values for each player
	 * V(s)
	 */
	protected double[][][] Vs;
	
	/**
	 * Preference for each action in each state
	 * p(s,a)
	 */
	protected double[][][] preference;
	
	
	/**
	 * record states and action experienced 
	 * in each episode
	 */
	private ArrayList<GameState> gameStates;
	private ArrayList<Integer> actionList;
	
	public RL_ActorCritic()
	{
		super();
		
		policy = new double[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		preference = new double[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		Vs = new double[2][Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS];
		
		gameStates = new ArrayList<GameState>();
		actionList = new ArrayList<Integer>();
	}
	
	public RL_ActorCritic( double alpha, double gamma, double epsilon, double beta )
	{
		super();
		
		ALPHA = alpha;
		GAMMA = gamma;
		EPSILON = epsilon;
		BETA = beta;
		
		policy = new double[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		preference = new double[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		Vs = new double[2][Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS];
		
		gameStates = new ArrayList<GameState>();
		actionList = new ArrayList<Integer>();
		
	}
	
	
	public void initSelfPlay()
	{
		for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
			
			for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
				
				Vs[0][rank][hisID] = 0.0;
				Vs[1][rank][hisID] = 0.0;
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					policy[rank][hisID][a] = 0.5;
					preference[rank][hisID][a] = 0.0; 
				}
			}
		}
		
		
		gameStates.clear();
		actionList.clear();
	}
	
	public void selfPlay()
	{
		initSelfPlay();
		
		long startTime = System.nanoTime();
		long lastTime = startTime;
		long currentTime = startTime;
		ArrayList<Double> epsilonList = new ArrayList<Double>();
		
		long T = 0;
		for( T = 1; T <= 10000000; T++ ) {
			
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
			episode( gameState, T );
			
			
			/**
			 * updating??
			 */
			currentTime = System.nanoTime();
			if( currentTime - lastTime > 10000000L ) {
				
				long count = (currentTime - lastTime) / 10000000L;
				
				double ep = Helper.computeExploitability( policy );
				//for( int i = 0; i < count; i++ )
					//epsilonList.add(ep);
				
				lastTime = currentTime;
			}
		}
		long estimatedTime = System.nanoTime() - startTime;
		System.out.println( estimatedTime );
		
		
		Helper.displayPolicy( policy );
		
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
	
	private void episode( GameState state, long T )
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
		
		/**
		 * during the game
		 * we just sample the action according to our policy
		 */
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
				
				/**
				 * use average strategy to play
				 */
				pro[0] = policy[I[actingPlayer].getCardRank()][hisID][0];
				for( int a = 1; a < Action.NUM_ACTION_TYPES; a++ ) {
					
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
			
			/**
			 * store the experienced states and actions
			 */
			states[actingPlayer].add( state );
			actions[actingPlayer].add( curAction );
			
			
			/**
			 * then observe the next state
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
		 * when the game is over
		 * we then update state values and our poilcy
		 */
		for( int playerSeat = 0; playerSeat <= 1; playerSeat++ ) {
			
			nextState = state;
			int nextHisID = OneCardPoker.historyID( nextState );
			int nextAction = 0;
			
			while( !states[playerSeat].isEmpty() ) {
				
				GameState curState = states[playerSeat].remove(states[playerSeat].size()-1);
				int curAction = actions[playerSeat].remove(actions[playerSeat].size()-1);
				int curHisID = OneCardPoker.historyID(curState);
				
				/**
				 * get the reward
				 */
				double reward = 0.0;
				if( nextState.isGameOver() )
					reward = OneCardPoker.getUtility( nextState, playerSeat );
				
				/**
				 * update state values
				 */
				double V_I = Vs[playerSeat][ranks[playerSeat]][curHisID];
				double V_Ip = Vs[playerSeat][ranks[playerSeat]][nextHisID];
				double delta = reward + GAMMA * V_Ip - V_I;
				V_I = V_I + ALPHA * delta;
				Vs[playerSeat][ranks[playerSeat]][curHisID] = V_I;
				
				/**
				 * update preference
				 */
				double pre_sa = preference[ranks[playerSeat]][curHisID][curAction];
				//pre_sa = pre_sa + BETA * delta;
				pre_sa = pre_sa + BETA * delta * ( 1 - policy[ranks[playerSeat]][curHisID][curAction] );
				preference[ranks[playerSeat]][curHisID][curAction] = pre_sa;
				
				/**
				 * update policy
				 */
				double sum = 0.0;
				double[] powers = new double[Action.NUM_ACTION_TYPES];
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					double pre = preference[ranks[playerSeat]][curHisID][a];
					//powers[a] = Math.exp(pre);
					powers[a] = Math.pow( 1.5, pre );
					sum += powers[a];
				}
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					policy[ranks[playerSeat]][curHisID][a] = powers[a] / sum;
				}
				powers = null;
				
				
				nextState = curState;
				nextHisID = curHisID;
				nextAction = curAction;
			}
		}
	}
	
	public void initOnlinePlay()
	{
		/**
		 * init each table for online playing
		 */
		for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
			
			for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
				
				Vs[0][rank][hisID] = 0.0;
				Vs[1][rank][hisID] = 0.0;
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					policy[rank][hisID][a] = 0.5;
					preference[rank][hisID][a] = 0.0; 
				}
			}
		}
		readNash();
	}
	
	
	public int onlinePlay_ChooseAction( GameState gameState, long T )
	{
		int hisID = OneCardPoker.historyID( gameState );
		int playerRank = Card.rankOfCard( gameState.getPlayerCard(seat) );
		
		OneCardPoker.checkOver( gameState );
		if( gameState.isGameOver() ) {
			
			return Action.ACTION_INVALID;
		}
		else {
			
			if( gameState.getActingPlayer() != seat ){
				
				return Action.ACTION_INVALID;
			}
			
			/**
			 * choose an action according to policy
			 */
			else {
				
				int retAction = 0;
				double[] pro = new double[Action.NUM_ACTION_TYPES];
				if( new Random().nextDouble() < EPSILON ) {
					
					pro[0] = 1.0 / Action.NUM_ACTION_TYPES;
					for( int a = 1; a < Action.NUM_ACTION_TYPES; a++ ) {
						
						pro[a] = pro[a-1] + 1.0 / Action.NUM_ACTION_TYPES;
					}
				}
				else {
					
					pro[0] = policy[playerRank][hisID][0];
					for( int a = 1; a < Action.NUM_ACTION_TYPES; a++ ) {
						
						pro[a] = pro[a-1] + policy[playerRank][hisID][a];
					}	
				}
				
				double playerPro = new Random().nextDouble();
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					if( playerPro < pro[a] ) {
						
						retAction = a;
						break;
					}
				}
				
				/**
				 * record the current state and action
				 */
				gameStates.add( new GameState(gameState) );
				actionList.add( retAction );
				
				return retAction;
			}
		}
	}
	
	public void onlinePlay_GameOver( GameState gameState, long T )
	{
		OneCardPoker.checkOver( gameState );
		if( !gameState.isGameOver() ) {
			
			return;
		}
		
		int playerRank = Card.rankOfCard( gameState.getPlayerCard(seat) );
		
		/**
		 * update state values and policy
		 */
		GameState nextState = gameState;
		int nextHisID = OneCardPoker.historyID(nextState);
		int nextAction = 0;
		while( !gameStates.isEmpty() ) {
			
			GameState curState = gameStates.remove(gameStates.size()-1);
			int curAction = actionList.remove(actionList.size()-1);
			int curHisID = OneCardPoker.historyID(curState);
			
			/**
			 * get the reward
			 */
			double reward = 0.0;
			if( nextState.isGameOver() )
				reward = OneCardPoker.getUtility( nextState, seat );
			
			/**
			 * update state values
			 */
			double V_I = Vs[seat][playerRank][curHisID];
			double V_Ip = Vs[seat][playerRank][nextHisID];
			double delta = reward + GAMMA * V_Ip - V_I;
			V_I = V_I + ALPHA * delta;
			Vs[seat][playerRank][curHisID] = V_I;
			
			/**
			 * update preference
			 */
			double pre_sa = preference[playerRank][curHisID][curAction];
			//pre_sa = pre_sa + BETA * delta;
			pre_sa = pre_sa + BETA * delta * ( 1 - policy[playerRank][curHisID][curAction] );
			preference[playerRank][curHisID][curAction] = pre_sa;
			
			/**
			 * update policy
			 */
			double sum = 0.0;
			double[] powers = new double[Action.NUM_ACTION_TYPES];
			for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
				
				double pre = preference[playerRank][curHisID][a];
				//powers[a] = Math.exp(pre);
				powers[a] = Math.pow( 2.0, pre );
				sum += powers[a];
			}
			for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
				
				policy[playerRank][curHisID][a] = powers[a] / sum;
			}
			powers = null;
			
			nextState = curState;
			nextHisID = curHisID;
			nextAction = curAction;
		}
		gameStates.clear();
		actionList.clear();
	}
	
	
	private void readNash()
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
				
				//Nash_policy[rank][his][a] = Double.parseDouble(line);
				
				policy[rank][his][a] = Double.parseDouble(line);
				
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
