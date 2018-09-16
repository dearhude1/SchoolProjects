package gameOneCardPoker.algorithms;

import gameOneCardPoker.Action;
import gameOneCardPoker.Card;
import gameOneCardPoker.GameState;
import gameOneCardPoker.OneCardPoker;

import java.util.ArrayList;
import java.util.Random;

public class RL_Sarsa extends Algorithm {
	private double ALPHA = 0.1;
	private double GAMMA = 0.9;
	private double EPSILON = 0.01;
	
	
	
	/**
	 * state-action values for each player
	 * Q(s,a)
	 */
	protected double[][][][] Qs;
	
	
	/**
	 * record states and action experienced 
	 * in each episode
	 */
	private ArrayList<GameState> gameStates;
	private ArrayList<Integer> actionList;
	
	public RL_Sarsa()
	{
		super();
		
		Qs = new double[2][Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		
		gameStates = new ArrayList<GameState>();
		actionList = new ArrayList<Integer>();
		
	}
	
	
	public RL_Sarsa( double alpha, double gamma, double epsilon )
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
		 * init each table for online playing
		 */
		for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
			
			for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
				
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					//policy[rank][hisID][a] = 0.5;
					Qs[0][rank][hisID][a] = 0.0;
					Qs[1][rank][hisID][a] = 0.0;
				}
			}
		}
		
		gameStates.clear();
		actionList.clear();
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
			 * choose an action using epsilon-greedy
			 */
			else {
				
				int retAction = 0;
				double[] pro = new double[Action.NUM_ACTION_TYPES];
				if( new Random().nextDouble() < EPSILON ) {
					
					pro[0] = 1.0 / Action.NUM_ACTION_TYPES;
					for( int a = 1; a < Action.NUM_ACTION_TYPES; a++ ) {
						
						pro[a] = pro[a-1] + 1.0 / Action.NUM_ACTION_TYPES;
					}
					
					double playerPro = new Random().nextDouble();
					for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
						
						if( playerPro < pro[a] ) {
							
							retAction = a;
							break;
						}
					}
				}
				else {
				
					int maxAction = 0;
					for( int a = 1; a < Action.NUM_ACTION_TYPES; a++ ) {
						
						if( Qs[seat][playerRank][hisID][a] > 
							Qs[seat][playerRank][hisID][maxAction] ) {
							
							maxAction = a;
						}
					}
					retAction = maxAction;
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
		 * Add the last state to the list
		 */
		gameStates.add(gameState);
		actionList.add(0);
		
		/**
		 * then we update state-action values
		 */
		GameState curState = gameStates.remove(0);
		int curAction = actionList.remove(0);
		int curHisID = OneCardPoker.historyID( curState );
		while( !gameStates.isEmpty() ) {
			
			GameState nextState = gameStates.remove(0);
			int nextAction = actionList.remove(0);
			int nextHisID = OneCardPoker.historyID(curState);
			
			/**
			 * get the reward
			 */
			double reward = 0.0;
			if( nextState.isGameOver() )
				reward = OneCardPoker.getUtility( nextState, seat );
			
			/**
			 * update state values
			 */
			double Q_Ia = Qs[seat][playerRank][curHisID][curAction];
			double Q_Ip = Qs[seat][playerRank][nextHisID][nextAction];
			
			/**
			 * Sarsa
			 */
			Q_Ia = ( 1 - ALPHA ) * Q_Ia + ALPHA * ( reward + GAMMA * Q_Ip );
			
			
			Qs[seat][playerRank][curHisID][curAction] = Q_Ia;
			
			
			curState = nextState;
			curHisID = nextHisID;
			curAction = nextAction;
		}
		gameStates.clear();
		actionList.clear();
	}
}
