package gameOneCardPoker.algorithms;

import gameOneCardPoker.Action;
import gameOneCardPoker.Card;
import gameOneCardPoker.GameState;
import gameOneCardPoker.InformationSet;
import gameOneCardPoker.OneCardPoker;
import gameOneCardPoker.Helper;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class LCFR extends CFR_Series {
	
	
	private double LAMBDA = 0.02;
	private double[][] V1_table;
	private double[][] V2_table;
	private double[] rootCount1;
	private double[] rootCount2;
	
	/**
	 * for LCFR
	 */
	private boolean[][] visited;

	
	/*
	 * the number of learning episodes
	 */
	private long L = 1000;

	//private long[] Ls = {2000, 1000, 500, 200, 100, 10, 1};
	private long[] Ls = { 1000 };
	
	//for recording all results obtained from the beginning of the game
	double allResults = 0.0;
	
	public LCFR()
	{
		super();
		
		
		/**
		 * for CFR Reinforcement Learning
		 */
		V1_table = new double[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS];
		V2_table = new double[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS];
		rootCount1 = new double[Card.NUM_RANKS];
		rootCount2 = new double[Card.NUM_RANKS];
		
		visited = new boolean[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS];
		
		for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
			
			for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
				
				V1_table[rank][hisID] = 0.0;
				V2_table[rank][hisID] = 0.0;
				visited[rank][hisID] = false;
			}
			rootCount1[rank] = rootCount2[rank] = 0.0;
		}
	}

	public LCFR( long l, double lambda )
	{
		super();
		
		L = l;
		LAMBDA = lambda;
		
		
		/**
		 * for CFR Reinforcement Learning
		 */
		V1_table = new double[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS];
		V2_table = new double[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS];
		rootCount1 = new double[Card.NUM_RANKS];
		rootCount2 = new double[Card.NUM_RANKS];
		visited = new boolean[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS];
		
		for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
			
			for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
				
				V1_table[rank][hisID] = 0.0;
				V2_table[rank][hisID] = 0.0;
				visited[rank][hisID] = false;
			}
			rootCount1[rank] = rootCount2[rank] = 0.0;
		}
	}
	
	public void initSelfPlay()
	{

		for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
			
			for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
				
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					policy[rank][hisID][a] = 0.5;
					average_policy[rank][hisID][a] = 0.0;
				}
			}
		}
		
		for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
			
			rootCount1[rank] = rootCount2[rank] = 0.0;
			
			for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
				
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					acc_policy[rank][hisID][a] = 0.0;
				}
				
				V1_table[rank][hisID] = 0.0;
				V2_table[rank][hisID] = 0.0;
				visited[rank][hisID] = false;
			}
		}
	}
	
	/**
	 * Self play of CFR with opponent's policy invisible (CFRhope)
	 * offline version
	 */
	public void selfPlay()
	{
		
		for( int index = 0; index < Ls.length; index++ ) {
			
			L = Ls[index];
			
			initSelfPlay();
			
			long startTime = System.nanoTime();
			long lastTime = startTime;
			long currentTime = startTime;
			ArrayList<Double> epsilonList = new ArrayList<Double>();
			
			int c = 0;
			long T = 0;
			while( (currentTime - startTime) < 30000000000L ) { //for( int T = 1; T <= 10000000; T++ ) {
				
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
				 * for learning the value of terminal node
				 * the learned values are influenced by both
				 * players' strategies
				 */
				episode_LCFR( gameState, T );
				
				
				/**
				 * after every M each episodes
				 * we update average strategy
				 */
				c++;
				if( c >= L ) {
					
					/**
					 * update
					 */
					for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
						
						GameState state = new GameState();
						
						state.receiveCard( 0, rank*Card.NUM_SUITS );
						state.receiveCard( 1, rank*Card.NUM_SUITS );
						
						walkTrees_LCFR( 1, 1, state, 1, 1 );
						walkTrees_LCFR( 2, 1, state, 1, 1 );
					}
					
					/**
					 * we need to initialize table V?
					 */
					for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
						
						rootCount1[rank] = 0;
						rootCount2[rank] = 0; 
						for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
							
							V1_table[rank][hisID] = 0.0;
							V2_table[rank][hisID] = 0.0;
							
							visited[rank][hisID] = false;
						}
					}
					
					c = 0;
					
					currentTime = System.nanoTime();
					if( currentTime - lastTime > 10000000L ) {
						
						long count = (currentTime - lastTime) / 10000000L;
						double ep = Helper.computeExploitability( average_policy );
						for( int i = 0; i < count; i++ )
							epsilonList.add(ep);
						
						lastTime = currentTime;
					}
				}
			}
			long estimatedTime = System.nanoTime() - startTime;
			System.out.println( estimatedTime );
			
			
			/**
			 * write to file
			 */
			try {
				
				String fileName = "./epsilons_L";
				fileName += String.valueOf(L);
				//fileName += "_" + String.valueOf(epsilonList.size());
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
	}
	
	private void episode_LCFR( GameState state, long T )
	{
		GameState nextState = null;
		
		/**
		 * get the current information set for each player
		 */
		int hisID = OneCardPoker.historyID( state );
		InformationSet[] I = new InformationSet[2];
		I[0] = new InformationSet( Card.rankOfCard(state.getPlayerCard(0)), hisID );
		I[1] = new InformationSet( Card.rankOfCard(state.getPlayerCard(1)), hisID );
		
		double[] reachingPro = new double[2];
		reachingPro[0] = reachingPro[1] = 1.0;
		
		double[] sampro = new double[2];
		sampro[0] = sampro[1] = 1.0;
		
		while( !state.isGameOver() ) {
			
			/**
			 * mark visited information set
			 */
			visited[I[0].getCardRank()][hisID] = true;
			visited[I[1].getCardRank()][hisID] = true;
			
			/**
			 * get the acting player 
			 * and take an action according to average strategy with epsilon-greedy
			 */
			int actingPlayer = state.getActingPlayer();
			int curAction = 0;
			double[] pro = new double[Action.NUM_ACTION_TYPES];
			if( new Random().nextDouble() < LAMBDA ) {
				
				pro[0] = 1.0 / Action.NUM_ACTION_TYPES;
				for( int a = 1; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					pro[a] = pro[a-1] + 1.0 / Action.NUM_ACTION_TYPES;
				}
			}
			else {
				
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
			 * then observe the next state and reward
			 */
			nextState = new GameState( state );
			OneCardPoker.doAction( nextState, curAction );
			double[] reward = new double[2];
			if( nextState.isGameOver() ) {
				
				reward[0] = OneCardPoker.getUtility( nextState, 0 );
				reward[1] = OneCardPoker.getUtility( nextState, 1 );
			}
			else {
				
				reward[0] = 0.0;
				reward[1] = 0.0;
			}
			int nextHisID = OneCardPoker.historyID( nextState );
			InformationSet I1p = new InformationSet( Card.rankOfCard(nextState.getPlayerCard(0)), nextHisID );
			InformationSet I2p = new InformationSet( Card.rankOfCard(nextState.getPlayerCard(1)), nextHisID );
			
			/**
			 * update state values and regret values
			 */
			double[] v_I = new double[2];
			double[] v_Ip = new double[2];
			v_I[0] = V1_table[I[0].getCardRank()][hisID];
			v_I[1] = V2_table[I[1].getCardRank()][hisID];
			v_Ip[0] = V1_table[I1p.getCardRank()][nextHisID];
			v_Ip[1] = V2_table[I2p.getCardRank()][nextHisID];
			
			
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
			
			/**/
			if( nextState.isGameOver() ) {
				
				/**
				 * mark visited information set
				 */
				visited[I1p.getCardRank()][nextHisID] = true;
				visited[I2p.getCardRank()][nextHisID] = true;
				
				/**
				 * use root count to learn values
				 */
				double rc1 = rootCount1[I1p.getCardRank()]+1;
				double rc2 = rootCount2[I2p.getCardRank()]+1;
				for( int h = 0; h < OneCardPoker.NUM_HISTORY_IDS; h++ ) {
					
					if( !OneCardPoker.isTerminalHistory(h) )
						continue;
					
					double v_Ip1 = V1_table[I1p.getCardRank()][h];
					double v_Ip2 = V2_table[I2p.getCardRank()][h];
					
					if( h == nextHisID ) {
						
						//v_Ip1 = (1-1.0/rc1)* v_Ip1 + (1.0/rc1)* (reward[0] + 0);
						//v_Ip2 = (1-1.0/rc2)* v_Ip2 + (1.0/rc2)* (reward[1] + 0);
						
						//have a try
						v_Ip1 += reward[0];
						v_Ip2 += reward[1];
						
					}
					else {
						
						//v_Ip1 = (1-1.0/rc1)* v_Ip1;
						//v_Ip2 = (1-1.0/rc2)* v_Ip2; 
						
					}
					V1_table[I1p.getCardRank()][h] = v_Ip1;
					V2_table[I2p.getCardRank()][h] = v_Ip2;
				}
				
				rootCount1[I1p.getCardRank()] = rc1;
				rootCount2[I2p.getCardRank()] = rc2;
				
			}
		}
	}
	
	private double walkTrees_LCFR( int viewer, int currentPlayer, GameState state, 
			double p_i, double ep_i )
	{
		double v_I = 0.0;
		
		int viewerSeat = 0;
		if( viewer == 2 )
			viewerSeat = 1;
		int hisID = OneCardPoker.historyID( state );
		InformationSet infoSet_Viewer = new InformationSet( Card.rankOfCard(state.getPlayerCard(viewerSeat)), hisID );
		
		
		OneCardPoker.checkOver( state );
		
		/**/
		if( !visited[infoSet_Viewer.getCardRank()][hisID] ) {
			
			return v_I;
		}
		/**
		if( (viewer == 1 && !visited1[infoSet_Viewer.getCardRank()][hisID]) || 
				(viewer == 2 && !visited2[infoSet_Viewer.getCardRank()][hisID]) ) {
				
				return v_I;
			}
		*/
		else if( state.isGameOver() ) {
			
			v_I = V1_table[infoSet_Viewer.getCardRank()][hisID];
			if( viewer == 2 )
				v_I = V2_table[infoSet_Viewer.getCardRank()][hisID];
			
			v_I = v_I / ep_i /(double)L;
			
			if( viewer == 1 )
				V1_table[infoSet_Viewer.getCardRank()][hisID] = v_I;
			else
				V2_table[infoSet_Viewer.getCardRank()][hisID] = v_I;
			
			return v_I;
		}
		else {
			
			/**
			 * if we are acting currently
			 */
			if( currentPlayer == viewer ) {
				
				/**
				 * update average strategy
				 */
				double Y = 0.0;
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					acc_policy[infoSet_Viewer.getCardRank()][hisID][a] += p_i * policy[infoSet_Viewer.getCardRank()][hisID][a];
					
					Y += acc_policy[infoSet_Viewer.getCardRank()][hisID][a];
				}
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					if( Y < 0.000001 )
						average_policy[infoSet_Viewer.getCardRank()][hisID][a] = 0.0;
					else
						average_policy[infoSet_Viewer.getCardRank()][hisID][a] = acc_policy[infoSet_Viewer.getCardRank()][hisID][a] / Y;
				}
				
				/**
				 * update values
				 */
				int nextPlayer = 1;
				if( viewer == 1 )
					nextPlayer = 2;
				double[] v_Ia = new double[Action.NUM_ACTION_TYPES];
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					GameState nextState = new GameState(state);
					OneCardPoker.doAction( nextState, a );
					
					double p = LAMBDA / ((double)Action.NUM_ACTION_TYPES) + 
							(1 - LAMBDA) * policy[infoSet_Viewer.getCardRank()][hisID][a];
					double p_i_next = p_i * policy[infoSet_Viewer.getCardRank()][hisID][a];
					double ep_i_next = ep_i * p;
					
					v_Ia[a] = walkTrees_LCFR( viewer, nextPlayer, nextState, p_i_next, ep_i_next );
					v_I += v_Ia[a] * policy[infoSet_Viewer.getCardRank()][hisID][a];
					
					nextState = null;
				}
				
				/**
				 * update regrets and next playing strategy
				 */
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					double R = regret[infoSet_Viewer.getCardRank()][hisID][a];
					R = R + ( v_Ia[a] - v_I );
					regret[infoSet_Viewer.getCardRank()][hisID][a] = R;
				}
				
				/**
				 * regret matching
				 */
				double sumPositiveRegret = 0.0;
				double[] sigma = new double[Action.NUM_ACTION_TYPES];
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					double posRegret = regret[infoSet_Viewer.getCardRank()][hisID][a];
					if( posRegret < 0.00001 )
						posRegret = 0;
					sumPositiveRegret += posRegret;
				}
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					double posRegret = regret[infoSet_Viewer.getCardRank()][hisID][a];
					if( posRegret < 0.00001 )
						posRegret = 0;
					if( sumPositiveRegret < 0.00001 )
						sigma[a] = 1.0 / Action.NUM_ACTION_TYPES;
					else
						sigma[a] = posRegret / sumPositiveRegret;
					
					policy[infoSet_Viewer.getCardRank()][hisID][a] = sigma[a];
				}
				
			}
			
			/**
			 * we are not acting at this node
			 */
			else {
				
				/**
				 * only update values
				 */
				int nextPlayer = viewer;
				double[] v_Ia = new double[Action.NUM_ACTION_TYPES];
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					GameState nextState = new GameState(state);
					OneCardPoker.doAction( nextState, a );
					
					v_Ia[a] = walkTrees_LCFR( viewer, nextPlayer, nextState, p_i, ep_i );
					v_I += v_Ia[a];
					
					nextState = null;
				}
			}
		}
		
		return v_I;
	}
}
