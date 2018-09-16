package gameOneCardPoker.algorithms;

import gameOneCardPoker.Action;
import gameOneCardPoker.Card;
import gameOneCardPoker.GameState;
import gameOneCardPoker.Helper;
import gameOneCardPoker.InformationSet;
import gameOneCardPoker.OneCardPoker;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

public class onLCFR extends CFR_Series {


	private double LAMBDA = 0.01;
	private double[][] V1_table;
	private double[][] V2_table;

	
	private double[][][] Nash_policy;
	private double NashWeight = 1;
	
	/**
	 * from each of the players 
	 */
	private boolean[][] visited1;
	private boolean[][] visited2;

	/**
	 * the count for online play
	 */
	private long online_c1 = 0;
	private long online_c2 = 0;
	
	/*
	 * the number of learning episodes
	 */
	private long L = 1;

	
	
	//for recording all results obtained from the beginning of the game
	double allResults = 0.0;
	
	public onLCFR()
	{
		super();
		
		/**
		 * for CFR Reinforcement Learning
		 */
		V1_table = new double[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS];
		V2_table = new double[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS];
		visited1 = new boolean[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS];
		visited2 = new boolean[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS];
		Nash_policy = new double[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
	}
	
	public onLCFR( long l, double lambda )
	{
		super();
		
		L = l;
		LAMBDA = lambda;
		
		
		/**
		 * for CFR Reinforcement Learning
		 */
		V1_table = new double[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS];
		V2_table = new double[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS];
		visited1 = new boolean[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS];
		visited2 = new boolean[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS];		
		Nash_policy = new double[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];

	}
	
	
	public void initOnlinePlay()
	{
		for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
			
			for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
				
				V1_table[rank][hisID] = 0.0;
				V2_table[rank][hisID] = 0.0;
				visited1[rank][hisID] = false;
				visited2[rank][hisID] = false;
				
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					Nash_policy[rank][hisID][a] = 0.0;
				}
			}
		}
		
		//init_Empty();
		init_Nash();

	}
	
	public void init_Empty()
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
			
			for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
				
				V1_table[rank][hisID] = 0.0;
				V2_table[rank][hisID] = 0.0;
				visited1[rank][hisID] = false;
				visited2[rank][hisID] = false;
			}
		}
		
		/**
		 * do forget this step
		 * because we need an initial strategy in online playing
		 */
		for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
			
			GameState state = new GameState();
			
			state.receiveCard( 0, rank*Card.NUM_SUITS );
			state.receiveCard( 1, rank*Card.NUM_SUITS );
			
			walkTrees_ComputeAverageStrategy( 1, 1, state, 1 );
			walkTrees_ComputeAverageStrategy( 2, 1, state, 1 );
		}
	}
	
	public void init_Nash()
	{
		readNash();
		
		for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
			
			GameState state = new GameState();
			
			state.receiveCard( 0, rank*Card.NUM_SUITS );
			state.receiveCard( 1, rank*Card.NUM_SUITS );
			
			walkTrees_NashInit( 1, 1, state, 1 );
			walkTrees_NashInit( 2, 1, state, 1 );
		}
		
		/**
		 * do forget this step
		 * because we need an initial strategy in online playing
		 */
		for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
			
			GameState state = new GameState();
			
			state.receiveCard( 0, rank*Card.NUM_SUITS );
			state.receiveCard( 1, rank*Card.NUM_SUITS );
			
			walkTrees_ComputeAverageStrategy( 1, 1, state, 1 );
			walkTrees_ComputeAverageStrategy( 2, 1, state, 1 );
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
	
	
	public int onlinePlay_ChooseAction(  GameState state, long T )
	{
		int hisID = OneCardPoker.historyID( state );
		int playerRank = Card.rankOfCard( state.getPlayerCard(seat) );
		
		OneCardPoker.checkOver( state );
		if( state.isGameOver() ) {
			
			return Action.ACTION_INVALID;
		}
		else {
			
			//mark visited
			if( seat == 0 ) {
				
				visited1[playerRank][hisID] = true;
			}
			else {
				
				visited2[playerRank][hisID] = true;
			}
			
			if( state.getActingPlayer() != seat ){
				
				return Action.ACTION_INVALID;
			}
			
			//choose an action and return
			else {
				
				int retAction = 0;
				double[] pro = new double[Action.NUM_ACTION_TYPES];
				if( new Random().nextDouble() < LAMBDA ) {
					
					pro[0] = 1.0 / Action.NUM_ACTION_TYPES;
					for( int a = 1; a < Action.NUM_ACTION_TYPES; a++ ) {
						
						pro[a] = pro[a-1] + 1.0 / Action.NUM_ACTION_TYPES;
					}
				}
				else {
					
					/**
					 * average policy for match
					 */
					//pro[0] = average_policy[playerRank][hisID][0];
					
					/**
					 * immediate policy for naive opponents
					 */
					pro[0] = policy[playerRank][hisID][0];
					for( int a = 1; a < Action.NUM_ACTION_TYPES; a++ ) {
					
						//pro[a] = pro[a-1] + average_policy[playerRank][hisID][a];
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
				
				return retAction;
			}
		}
	}
	
	public void onlinePlay_GameOver( GameState state, long T )
	{
		int hisID = OneCardPoker.historyID( state );
		int playerRank = Card.rankOfCard( state.getPlayerCard(seat) );
		
		OneCardPoker.checkOver( state );
		
		if( !state.isGameOver() ) {
			
			return;
		}
		else {
			
			if( seat == 0 ) {
				
				visited1[playerRank][hisID] = true;
				V1_table[playerRank][hisID] += OneCardPoker.getUtility( state, seat );
				
				online_c1++;
				
				if( online_c1 >= L ) {
					
					
					for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
						
						GameState gs = new GameState();
						gs.receiveCard( 0, rank*Card.NUM_SUITS );
						gs.receiveCard( 1, rank*Card.NUM_SUITS );
						
						walkTrees_onLCFR_TransformValue( 1, 1, gs, 1, 1 );
						gs = null;
					}
					for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
						
						GameState gs = new GameState();
						gs.receiveCard( 0, rank * Card.NUM_SUITS );
						gs.receiveCard( 1, rank * Card.NUM_SUITS );
						
						walkTrees_onLCFR_UpdateValue( 1, 1, gs, 1, 1 );
					}
					
					for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
						for( int h = 0; h < OneCardPoker.NUM_HISTORY_IDS; h++ ) {
							
							V1_table[rank][h] = 0.0;
							visited1[rank][h] = false;
						}
					}
					
					online_c1 = 0;
				}
			}
			else {
				
				visited2[playerRank][hisID] = true;
				V2_table[playerRank][hisID] += OneCardPoker.getUtility( state, seat );
				
				online_c2++;
				
				if( online_c2 >= L ) {
					
					for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
						
						GameState gs = new GameState();
						gs.receiveCard( 0, rank*Card.NUM_SUITS );
						gs.receiveCard( 1, rank*Card.NUM_SUITS );
						
						walkTrees_onLCFR_TransformValue( 2, 1, gs, 1, 1 );
						gs = null;
					}
					for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
						
						GameState gs = new GameState();
						gs.receiveCard( 0, rank * Card.NUM_SUITS );
						gs.receiveCard( 1, rank * Card.NUM_SUITS );
						
						walkTrees_onLCFR_UpdateValue( 2, 1, gs, 1, 1 );
					}
					
					for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
						for( int h = 0; h < OneCardPoker.NUM_HISTORY_IDS; h++ ) {
							
							V2_table[rank][h] = 0.0;
							visited2[rank][h] = false;
						}
					}
					
					online_c2 = 0;
					
					//System.out.println("value: "+Helper.computeStrategyValue(2, average_policy, average_policy ));
				}
			}
		}
	}
	
	private void walkTrees_onLCFR_TransformValue( int targetPlayer, int currentPlayer, 
			GameState state, double p_i, double pp_i )
	{
		int hisID = OneCardPoker.historyID( state );
		
		
		int tarSeat = 0;
		if( targetPlayer == 2 )
			tarSeat = 1;
		
		int tarRank = Card.rankOfCard(state.getPlayerCard(tarSeat));
		
		InformationSet infoSets[] = new InformationSet[2];
		infoSets[0] = new InformationSet( Card.rankOfCard(state.getPlayerCard(0)), hisID );
		infoSets[1] = new InformationSet( Card.rankOfCard(state.getPlayerCard(1)), hisID );
		
		OneCardPoker.checkOver( state );
		if( (targetPlayer == 1 && !visited1[tarRank][hisID]) || 
			(targetPlayer == 2 && !visited2[tarRank][hisID]) ) {
				
				return;
		}
		else if( state.isGameOver() ) {
			
			//transform the value in terminal state
			double v = V1_table[infoSets[0].getCardRank()][hisID];
			if( targetPlayer == 2 )
				v = V2_table[infoSets[1].getCardRank()][hisID];
			
			double v_p = v * pp_i / p_i;
			//double v_p = v / (L * p_i);
			
			if( targetPlayer == 1 )
				V1_table[infoSets[0].getCardRank()][hisID] = v_p;
			else
				V2_table[infoSets[1].getCardRank()][hisID] = v_p;
			
			return;
		}
		else {
			
			if( currentPlayer == targetPlayer ) {
				
				int nextPlayer = 2;
				if( currentPlayer == 2 )
					nextPlayer = 1;
				
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					GameState nextState = new GameState(state);
					OneCardPoker.doAction( nextState, a );
					
					double sigma = LAMBDA / ((double)Action.NUM_ACTION_TYPES) + 
							(1-LAMBDA) * average_policy[infoSets[targetPlayer-1].getCardRank()][hisID][a];
					//double sigma_p = EPSILON_RL / ((double)Action.NUM_ACTION_TYPES) + 
							//(1-EPSILON_RL) * policy[infoSets[targetPlayer-1].getCardRank()][hisID][a];
					double sigma_p = policy[infoSets[targetPlayer-1].getCardRank()][hisID][a];
					double p_i_next = p_i * sigma;
					double pp_i_next = pp_i * sigma_p;
					
					walkTrees_onLCFR_TransformValue( targetPlayer, nextPlayer, nextState, p_i_next, pp_i_next );
					
					nextState = null;
				}
			}
			else {
				
				int nextPlayer = 2;
				if( currentPlayer == 2 )
					nextPlayer = 1;
				
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					GameState nextState = new GameState(state);
					OneCardPoker.doAction( nextState, a );
					
					walkTrees_onLCFR_TransformValue( targetPlayer, nextPlayer, nextState, p_i, pp_i );
					
					nextState = null;
				}
			}
		}
	}
	
	/**
	 * transform the terminal node value of playing strategy
	 * to that of immediate policy
	 * @param targetPlayer: whose values need to be transformed
	 * @param currentPlayer
	 * @param state
	 * @param p_i_playing: reaching probability of playing strategy
	 * @param p_i_imm: reaching probability of immediate strategy
	 *
	private void walkTrees_TransformValue_IsChanger( int targetPlayer, int currentPlayer, 
			GameState state, double p_i, double pp_i )
	{
		int hisID = OneCardPoker.historyID( state );
		
		
		InformationSet infoSets[] = new InformationSet[2];
		infoSets[0] = new InformationSet( Card.rankOfCard(state.getPlayerCard(0)), hisID );
		infoSets[1] = new InformationSet( Card.rankOfCard(state.getPlayerCard(1)), hisID );
		
		OneCardPoker.checkOver( state );
		if( state.isGameOver() ) {
			
			//transform the value in terminal state
			double v = V1_table[infoSets[0].getCardRank()][hisID];
			if( targetPlayer == 2 )
				v = V2_table[infoSets[1].getCardRank()][hisID];
			
			double v_p = v * pp_i / p_i;
			
			if( targetPlayer == 1 )
				V1_table[infoSets[0].getCardRank()][hisID] = v_p;
			else
				V2_table[infoSets[1].getCardRank()][hisID] = v_p;
			
			return;
		}
		else {
			
			if( currentPlayer == targetPlayer ) {
				
				int nextPlayer = 2;
				if( currentPlayer == 2 )
					nextPlayer = 1;
				
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					GameState nextState = new GameState(state);
					OneCardPoker.doAction( nextState, a );
					
					double sigma = EPSILON_RL / ((double)Action.NUM_ACTION_TYPES) + 
							(1-EPSILON_RL) * average_policy[infoSets[targetPlayer-1].getCardRank()][hisID][a];
					//double sigma_p = EPSILON_RL / ((double)Action.NUM_ACTION_TYPES) + 
							//(1-EPSILON_RL) * policy[infoSets[targetPlayer-1].getCardRank()][hisID][a];
					double sigma_p = policy[infoSets[targetPlayer-1].getCardRank()][hisID][a];
					double p_i_next = p_i * sigma;
					double pp_i_next = pp_i * sigma_p;
					
					walkTrees_TransformValue_IsChanger( targetPlayer, nextPlayer, nextState, p_i_next, pp_i_next );
					
					nextState = null;
				}
			}
			else {
				
				int nextPlayer = 2;
				if( currentPlayer == 2 )
					nextPlayer = 1;
				
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					GameState nextState = new GameState(state);
					OneCardPoker.doAction( nextState, a );
					
					walkTrees_TransformValue_IsChanger( targetPlayer, nextPlayer, nextState, p_i, pp_i );
					
					nextState = null;
				}
			}
		}
	}
	*/
	
	/**
	private void walkTrees_TransformValue_NotChanger( int targetPlayer, int currentPlayer, 
			GameState state, double[] p_i, double[] pp_i )
	{
		int hisID = OneCardPoker.historyID( state );
		
		
		InformationSet infoSets[] = new InformationSet[2];
		infoSets[0] = new InformationSet( Card.rankOfCard(state.getPlayerCard(0)), hisID );
		infoSets[1] = new InformationSet( Card.rankOfCard(state.getPlayerCard(1)), hisID );
		
		OneCardPoker.checkOver( state );
		if( state.isGameOver() ) {
			
			//transform the value in terminal state
			double v = V1_table[infoSets[0].getCardRank()][hisID];
			if( targetPlayer == 2 )
				v = V2_table[infoSets[1].getCardRank()][hisID];
			
			double sum = 0;
			double sum_p = 0;
			for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
				
				//fold node
				if( hisID == 5 || hisID == 7 ) {
					
					sum -= p_i[rank] * 2;
					sum_p -= pp_i[rank] * 2;
				}
				else if( rank < infoSets[targetPlayer-1].getCardRank() ) {
					
					sum += p_i[rank] * 2;
					sum_p += pp_i[rank] * 2;
				}
				else if( rank == infoSets[targetPlayer-1].getCardRank() ) {
					
					sum += p_i[rank];
					sum_p += pp_i[rank];
				}
				else {
					
					sum -= p_i[rank] * 2;
					sum_p -= pp_i[rank] * 2; 
				}
			}
			
			double v_p = v * sum_p / sum;
			
			if( targetPlayer == 1 )
				V1_table[infoSets[0].getCardRank()][hisID] = v_p;
			else
				V2_table[infoSets[1].getCardRank()][hisID] = v_p;
			
			return;
		}
		else {
			
			if( currentPlayer != targetPlayer ) {
				
				int nextPlayer = 2;
				if( currentPlayer == 2 )
					nextPlayer = 1;
				
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					GameState nextState = new GameState(state);
					OneCardPoker.doAction( nextState, a );
					
					double p_i_next[] = new double[Card.NUM_RANKS];
					double pp_i_next[] = new double[Card.NUM_RANKS];
					for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
						
						double sigma = EPSILON_RL / ((double)Action.NUM_ACTION_TYPES) + 
								(1-EPSILON_RL) * average_policy[rank][hisID][a];
						double sigma_p = EPSILON_RL / ((double)Action.NUM_ACTION_TYPES) + 
								(1-EPSILON_RL) * policy[rank][hisID][a];
						
						p_i_next[rank] = p_i[rank] * sigma;
						pp_i_next[rank] = pp_i[rank] * sigma_p;
					}
					
					walkTrees_TransformValue_NotChanger( targetPlayer, nextPlayer, nextState, p_i_next, pp_i_next );
					
					nextState = null;
					p_i_next = null;
					pp_i_next = null;
				}
			}
			else {
				
				int nextPlayer = 2;
				if( currentPlayer == 2 )
					nextPlayer = 1;
				
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					GameState nextState = new GameState(state);
					OneCardPoker.doAction( nextState, a );
					
					walkTrees_TransformValue_NotChanger( targetPlayer, nextPlayer, nextState, p_i, pp_i );
					
					nextState = null;
				}
			}
		}
	}
	*/
	
	private double walkTrees_onLCFR_UpdateValue( int viewer, int currentPlayer, GameState state, 
			double p_i, double ep_i )
	{
		double v_I = 0.0;
		
		int viewerSeat = 0;
		if( viewer == 2 )
			viewerSeat = 1;
		
		int hisID = OneCardPoker.historyID( state );
		int viewerRank = Card.rankOfCard(state.getPlayerCard(viewerSeat));
		
		
		OneCardPoker.checkOver( state );
		/**/
		if( (viewer == 1 && !visited1[viewerRank][hisID]) || 
			(viewer == 2 && !visited2[viewerRank][hisID]) ) {
			
			return v_I;
		}
		else if( state.isGameOver() ) {
			
			v_I = V1_table[viewerRank][hisID];
			if( viewer == 2 )
				v_I = V2_table[viewerRank][hisID];
			
			v_I = v_I / (ep_i * L);
			
			/**
			if( p_i > 0.00001 )
				v_I = v_I / (p_i * L);
			else
				v_I = 0.0;
			*/
			
			
			if( viewer == 1 )
				V1_table[viewerRank][hisID] = v_I;
			else
				V2_table[viewerRank][hisID] = v_I;
			
			return v_I;
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
				double[] v_Ia = new double[Action.NUM_ACTION_TYPES];
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					GameState nextState = new GameState(state);
					OneCardPoker.doAction( nextState, a );
					
					double p = LAMBDA / ((double)Action.NUM_ACTION_TYPES) + 
							(1 - LAMBDA) * policy[viewerRank][hisID][a];
					double p_i_next = p_i * policy[viewerRank][hisID][a];
					double ep_i_next = ep_i * p;
					
					v_Ia[a] = walkTrees_onLCFR_UpdateValue( viewer, nextPlayer, nextState, p_i_next, ep_i_next );
					v_I += v_Ia[a] * policy[viewerRank][hisID][a];
					
					nextState = null;
				}
				
				/**
				 * update regrets and next playing strategy
				 */
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					double R = regret[viewerRank][hisID][a];
					R = R + ( v_Ia[a] - v_I );
					regret[viewerRank][hisID][a] = R;
				}
				
				/**
				 * regret matching
				 */
				double sumPositiveRegret = 0.0;
				double[] sigma = new double[Action.NUM_ACTION_TYPES];
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					double posRegret = regret[viewerRank][hisID][a];
					if( posRegret < 0.00001 )
						posRegret = 0;
					sumPositiveRegret += posRegret;
				}
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					double posRegret = regret[viewerRank][hisID][a];
					if( posRegret < 0.00001 )
						posRegret = 0;
					if( sumPositiveRegret < 0.00001 )
						sigma[a] = 1.0 / Action.NUM_ACTION_TYPES;
					else
						sigma[a] = posRegret / sumPositiveRegret;
					
					policy[viewerRank][hisID][a] = sigma[a];
					
				}
				
				/**
				 * update average strategy
				 */
				double Y = 0.0;
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					acc_policy[viewerRank][hisID][a] += p_i * policy[viewerRank][hisID][a];
					
					Y += acc_policy[viewerRank][hisID][a];
				}
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					if( Y < 0.000001 )
						average_policy[viewerRank][hisID][a] = 0.0;
					else
						average_policy[viewerRank][hisID][a] = acc_policy[viewerRank][hisID][a] / Y;
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
					
					v_Ia[a] = walkTrees_onLCFR_UpdateValue( viewer, nextPlayer, nextState, p_i, ep_i );
					v_I += v_Ia[a];
					
					nextState = null;
				}
			}
		}
		
		if( viewer == 1 )
			V1_table[viewerRank][hisID] = v_I;
		else
			V2_table[viewerRank][hisID] = v_I;
		
		return v_I;
	}
	
	
	/**
	 * compute only average strategy!!
	 */
	private void walkTrees_ComputeAverageStrategy( int viewer, int currentPlayer, GameState state, double p_i )
	{
		
		int viewerSeat = 0;
		if( viewer == 2 )
			viewerSeat = 1;
		int hisID = OneCardPoker.historyID( state );
		InformationSet infoSet_Viewer = new InformationSet( Card.rankOfCard(state.getPlayerCard(viewerSeat)), hisID );
		
		
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
				 * take each action?
				 */
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					GameState nextState = new GameState( state );
					OneCardPoker.doAction( nextState, a );
					
					int nextPlayer = 2;
					if( viewer == 2 )
						nextPlayer = 1;
					
					double p_i_next = p_i * policy[infoSet_Viewer.getCardRank()][hisID][a];
					
					walkTrees_ComputeAverageStrategy( viewer, nextPlayer, nextState, p_i_next );
				}
				
				/**
				 * update average strategy
				 */
				//Y[infoSet_Viewer.getCardRank()][hisID] += p_i;
				double Y = 0.0;
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					acc_policy[infoSet_Viewer.getCardRank()][hisID][a] += p_i * policy[infoSet_Viewer.getCardRank()][hisID][a];
					
					Y += acc_policy[infoSet_Viewer.getCardRank()][hisID][a];
				}
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					//average_policy[infoSet_Viewer.getCardRank()][hisID][a] = X[infoSet_Viewer.getCardRank()][hisID][a] / Y;
					
					/**/
					if( Y < 0.000001 )
						average_policy[infoSet_Viewer.getCardRank()][hisID][a] = 0.0;
					else
						average_policy[infoSet_Viewer.getCardRank()][hisID][a] = acc_policy[infoSet_Viewer.getCardRank()][hisID][a] / Y;
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
					
					walkTrees_ComputeAverageStrategy( viewer, nextPlayer, nextState, p_i );
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
