package gameOneCardPoker.algorithms;

import gameOneCardPoker.Action;
import gameOneCardPoker.Card;
import gameOneCardPoker.GameState;
import gameOneCardPoker.Helper;
import gameOneCardPoker.OneCardPoker;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class onCFRRL extends CFR_Series {

	private double ALPHA = 0.1;
	private double GAMMA = 1.0;
	private double EPSILON = 0.01;
	private double BETA = 0.8;
	
	private double[][][][] Qs;
	
	private double[][][] Nash_policy;
	private double NashWeight = 1;
	
	/**
	 * record states and action experienced 
	 * in each episode
	 */
	private ArrayList<GameState> gameStates;
	private ArrayList<Integer> actionList;
	
	/**
	 * the count for online play
	 */
	private long online_c1 = 0;
	private long online_c2 = 0;
	
	/*
	 * the number of learning episodes
	 */
	private long L = 2;
	
	
	public onCFRRL()
	{
		super();
		
		Nash_policy = new double[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		
		Qs = new double[2][Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		gameStates = new ArrayList<GameState>();
		actionList = new ArrayList<Integer>();
		
	}
	
	public onCFRRL( double alpha, double gamma, double epsilon )
	{
		super();
		
		ALPHA = alpha;
		GAMMA = gamma;
		EPSILON = epsilon;
		
		Qs = new double[2][Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		gameStates = new ArrayList<GameState>();
		actionList = new ArrayList<Integer>();
		
	}
	
	public void initOnlinePlay()
	{
		/**
		 * init each table
		 */
		for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
			for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					Qs[0][rank][hisID][a] = 0.0;
					Qs[1][rank][hisID][a] = 0.0;
				}
			}
		}
		
		/**
		 * read nash policy from file
		 */
		readNash();
		
		/**/
		for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
			
			GameState state = new GameState();
			
			state.receiveCard( 0, rank*Card.NUM_SUITS );
			state.receiveCard( 1, rank*Card.NUM_SUITS );
			
			walkTrees_NashInit( 1, 1, state, 1 );
			walkTrees_NashInit( 2, 1, state, 1 );
		}
		
		Helper.computeAverageStrategy(acc_policy, average_policy);
	}
	
	
	public int onlinePlay_ChooseAction(  GameState state, long T )
	{
		int hisID = OneCardPoker.historyID( state );
		int playerRank = Card.rankOfCard( state.getPlayerCard(seat) );
		
		OneCardPoker.checkOver( state );
		if( state.isGameOver() ) {
			
			return Action.ACTION_INVALID;
		}
		else {
			
			if( state.getActingPlayer() != seat ){
				
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
					//pro[0] = average_policy[playerRank][hisID][0];
					for( int a = 1; a < Action.NUM_ACTION_TYPES; a++ ) {
						
						pro[a] = pro[a-1] + policy[playerRank][hisID][a];
						//pro[a] = pro[a-1] + average_policy[playerRank][hisID][a];
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
				gameStates.add( new GameState(state) );
				actionList.add( retAction );
				
				return retAction;
			}
		}
	}
	
	
	public void onlinePlay_GameOver( GameState state, long T )
	{
		OneCardPoker.checkOver( state );
		if( !state.isGameOver() ) {
			
			return;
		}
		
		int playerRank = Card.rankOfCard( state.getPlayerCard(seat) );
		
		/**
		 * compute reaching probability of each experienced information set
		 */
		ArrayList<Double> reachingPros = new ArrayList<Double>();
		double pi_i = 1.0;
		reachingPros.add( pi_i );
		for( int i = 0; i < gameStates.size()-1; i++ ) {
			
			GameState gs = gameStates.get(i);
			int a = actionList.get(i);
			int h = OneCardPoker.historyID( gs );
			
			pi_i *= EPSILON / ((double)Action.NUM_ACTION_TYPES) + 
						(1-EPSILON) * policy[playerRank][h][a];
			//pi_i *= EPSILON / ((double)Action.NUM_ACTION_TYPES) + 
						//(1-EPSILON) * average_policy[playerRank][h][a];
			
			reachingPros.add( pi_i );
		}
		
		/**
		 * update Q-value and regret in each experienced information set
		 */
		GameState nextState = state;
		int nextHisID = OneCardPoker.historyID(nextState);
		int nextAction = 0;
		while( !gameStates.isEmpty() ) {
			
			GameState curState = gameStates.remove(gameStates.size()-1);
			int curAction = actionList.remove(actionList.size()-1);
			int curHisID = OneCardPoker.historyID(curState);
			double reachingPro = reachingPros.remove( reachingPros.size()-1);
			
			//get the reward
			double reward = 0.0;
			if( nextState.isGameOver() )
				reward = OneCardPoker.getUtility( nextState, seat );
			
			//update state-action values
			double Q_I = Qs[seat][playerRank][curHisID][curAction];
			double Q_Ip = Qs[seat][playerRank][nextHisID][nextAction];
			Q_I = (1 - ALPHA) * Q_I + ALPHA * ( reward + GAMMA * Q_Ip );
			Qs[seat][playerRank][curHisID][curAction] = Q_I;
			
			double v_I = 0.0;
			for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
				
				v_I += policy[playerRank][curHisID][a] * Qs[seat][playerRank][curHisID][a];
				
				//v_I += average_policy[playerRank][curHisID][a] * Qs[seat][playerRank][curHisID][a];
			}
			
			double delta = reward + GAMMA * Q_Ip - Q_I;
			for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
				
				double R = regret[playerRank][curHisID][a];
				
				/**
				 * use values to update regret
				 */
				R = R + ( Qs[seat][playerRank][curHisID][a] - v_I ) / reachingPro;
				
				/**
				 * use TD-error to update regret
				 *
				if( a == curAction ) {
					
					//R = R + BETA * delta;
					R = R + BETA * delta * (1-policy[playerRank][curHisID][a]);
				}
				*/
				
				
				
				
				regret[playerRank][curHisID][a] = R;
			}
			
			/**
			 * update strategy immdietely?
			 */
			GameState gameState = new GameState();
			gameState.receiveCard( 0, Card.NUM_SUITS * playerRank );
			gameState.receiveCard( 1, Card.NUM_SUITS * playerRank );
			walkTrees_UpdateStrategy( (seat+1), 1, gameState, 1 );
			
			nextState = curState;
			nextHisID = curHisID;
			nextAction = curAction;
		}
		gameStates.clear();
		actionList.clear();
		
		/**
		 * update strategy if needed
		 *
		if( seat == 0 ) {
			
			online_c1++;
			if( online_c1 >= L ) {
				
				for( int r = 0; r < Card.NUM_RANKS; r++ ) {
					
					GameState gs = new GameState();
					gs.receiveCard( 0, r*Card.NUM_SUITS );
					gs.receiveCard( 1, r*Card.NUM_SUITS );
					
					walkTrees_UpdateStrategy( 1, 1, gs, 1 );
				}
				Helper.computeAverageStrategy(acc_policy, average_policy);
				online_c1 = 0;
			}
		}
		else {
			
			online_c2++;
			if( online_c2 >= L ) {
				
				for( int r = 0; r < Card.NUM_RANKS; r++ ) {
					
					GameState gs = new GameState();
					gs.receiveCard( 0, r * Card.NUM_SUITS );
					gs.receiveCard( 1, r * Card.NUM_SUITS );
					
					walkTrees_UpdateStrategy( 2, 1, gs, 1 );
				}
				Helper.computeAverageStrategy(acc_policy, average_policy);
				online_c2 = 0;
			}
		}
		*/
	}
	
	
	private void walkTrees_UpdateStrategy( int viewer, int currentPlayer, 
			GameState state, double p_i )
	{
		
		int hisID = OneCardPoker.historyID( state );
		
		int rank = Card.rankOfCard( state.getPlayerCard(0) );
		if( viewer == 2 )
			rank = Card.rankOfCard( state.getPlayerCard(1) );
		
		OneCardPoker.checkOver(state);
		if( state.isGameOver() ) {
			
			return;
		}
		else {
			
			if( currentPlayer == viewer ) {
				
				/**
				 * regret matching
				 */
				double sumPositiveRegret = 0.0;
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					double posRegret = regret[rank][hisID][a];
					if( posRegret < 0.00001 )
						posRegret = 0;
					
					sumPositiveRegret += posRegret;
				}
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					double posRegret = regret[rank][hisID][a];
					if( posRegret < 0.00001 )
						posRegret = 0;
					if( sumPositiveRegret < 0.00001 )
						policy[rank][hisID][a] = 1.0 / Action.NUM_ACTION_TYPES;
					else
						policy[rank][hisID][a] = posRegret / sumPositiveRegret;
				}
				
				/**
				 * try each action
				 */
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					GameState nextState = new GameState( state );
					OneCardPoker.doAction( nextState, a );
					
					int nextPlayer = 2;
					if( viewer == 2 )
						nextPlayer = 1;
					
					double p_i_next = p_i * policy[rank][hisID][a];
					walkTrees_UpdateStrategy( viewer, nextPlayer, nextState, p_i_next );
					
					nextState = null;
				}
				
				/**
				 * update accumulative policy
				 * why not compute strategy here?
				 */
				double sum = 0.0;
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					acc_policy[rank][hisID][a] += p_i * policy[rank][hisID][a];
					sum += acc_policy[rank][hisID][a];
				}
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
				
					if( sum < 0.00001 )
						average_policy[rank][hisID][a] = 0;
					else {
						
						average_policy[rank][hisID][a] = acc_policy[rank][hisID][a] / sum;
					}
				}
				//check
				for( int action = 0; action < Action.NUM_ACTION_TYPES; action++ ) {
					
					if( average_policy[rank][hisID][action] < 0.0001 ) {
						
						average_policy[rank][hisID][action] = 0.0;
						average_policy[rank][hisID][1-action] = 1.0;
					}
				}
			}
			else {
				
				/**
				 * try each action
				 */
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					GameState nextState = new GameState( state );
					OneCardPoker.doAction( nextState, a );
					
					//recursive
					int nextPlayer = viewer;
					walkTrees_UpdateStrategy( viewer, nextPlayer, nextState, p_i );
					
					nextState = null;
				}
			}
		}
	}

	private void walkTrees_NashInit( int viewer, int currentPlayer, GameState state, 
			double p_i )
	{
		
		int viewerSeat = 0;
		if( viewer == 2 )
			viewerSeat = 1;
		
		int hisID = OneCardPoker.historyID( state );
		int viewerRank = Card.rankOfCard(state.getPlayerCard(viewerSeat));
		
		
		OneCardPoker.checkOver( state );
		if( state.isGameOver() ) {
			
			return;
		}
		else {
			
			/**
			 * if we are acting currently
			 */
			if( currentPlayer == viewer ) {
				
				/**
				 * update values
				 */
				int nextPlayer = 1;
				if( viewer == 1 )
					nextPlayer = 2;

				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					GameState nextState = new GameState(state);
					OneCardPoker.doAction( nextState, a );
					
					double p_i_next = p_i * Nash_policy[viewerRank][hisID][a];
					walkTrees_NashInit( viewer, nextPlayer, nextState, p_i_next );
					
					nextState = null;
				}
				
				
				/**
				 * update average strategy
				 */
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					acc_policy[viewerRank][hisID][a] += NashWeight * p_i * Nash_policy[viewerRank][hisID][a];
				}
			}
			
			/**
			 * we are not acting at this node
			 */
			else {
				
				int nextPlayer = viewer;
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					GameState nextState = new GameState(state);
					OneCardPoker.doAction( nextState, a );
					
					walkTrees_NashInit( viewer, nextPlayer, nextState, p_i );
					
					nextState = null;
				}
			}
		}
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
				
				Nash_policy[rank][his][a] = Double.parseDouble(line);
				
				policy[rank][his][a] = Nash_policy[rank][his][a];
				//average_policy[rank][his][a] = Nash_policy[rank][his][a];
				
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
	
	
	public void displayPolicy()
	{
		Helper.displayPolicy( average_policy );
	}

}
