package gameOneCardPoker.algorithms;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import drasys.or.mp.*;
import drasys.or.mp.lp.DenseSimplex;
import drasys.or.mp.lp.LinearProgrammingI;
import drasys.or.matrix.VectorI;
import gameOneCardPoker.Action;
import gameOneCardPoker.Card;
import gameOneCardPoker.GameState;
import gameOneCardPoker.Helper;
import gameOneCardPoker.OneCardPoker;

public class DBBR  extends Algorithm {

	
	/**
	 * table alpha
	 * for posterior action probabilities
	 * 
	 * action probability in each public information set
	 * so in One-Card Poker, public information set is action sequence
	 */
	protected double[][] alpha;
	
	/**
	 * table beta
	 * the probability of opponent being in each bucket of public information set
	 * 
	 * so in One-Card Poker, this value stands for the probability of opponent having
	 * each private card in each action sequence 
	 */
	protected double[][] beta;
	
	/**
	 * action count observed in public informatin set n
	 */
	protected double[][] count_na;
	
	/**
	 * opponent model
	 * namely the strategy of the opponent
	 */
	protected double[][][] opponentStrategy;
	
	/**
	 * playing strategy for DBBR agent
	 */
	protected double[][][] bestResponse;
	
	protected double[][][] Nash_policy;
	
	/**
	 * the weight of Nash equilibrium 
	 * for computing posterior action probabilities
	 */
	protected double N_prior = 5;
	
	/**
	 * iteration number before first computing the opponent model
	 * within these iterations, a Nash equilibrium is used for play
	 * 
	 * Match:
	 * 30000 games: T = 2000, k = 500
	 * 1000 games: T = 200, k = 100
	 * 500 games: T = 100, k = 40
	 * 300 games: T = 50, k = 25
	 * 100 games: T = 40, k = 20
	 * 50 games: T = 10, k = 5
	 * 10 games: T = 4, k = 2
	 */
	protected long T_equil = 200;
	
	/**
	 * iteration number for update the opponent modeling once
	 */
	protected long k_updateModel = 100;
	
	/**
	 * value of the game for each player
	 */
	protected double[] gameValues;
	
	
	public DBBR()
	{
		super();
		
		alpha = new double[OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		beta = new double[OneCardPoker.NUM_HISTORY_IDS][Card.NUM_RANKS];
		count_na = new double[OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		opponentStrategy = new double[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		bestResponse = new double[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		Nash_policy = new double[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		
		for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
			
			for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
				
				alpha[hisID][a] = 0.0;
				count_na[hisID][a] = 0.0;
			}
			for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
				
				beta[hisID][rank] = 0.0;
			}
		}
		
		for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
			
			for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
				
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					Nash_policy[rank][hisID][a] = 0.0;
					opponentStrategy[rank][hisID][a] = 0.0;
					bestResponse[rank][hisID][a] = 0.0;
				}
			}
		}
		
		readNash();
		
		gameValues = new double[2];
		gameValues[0] = gameValues[1] = 0.0;
		computeGameValues();
		
		//System.out.println(gameValues[0]);
		
	}
	
	
	public DBBR( int gameNum )
	{
		super();
		
		alpha = new double[OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		beta = new double[OneCardPoker.NUM_HISTORY_IDS][Card.NUM_RANKS];
		count_na = new double[OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		opponentStrategy = new double[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		bestResponse = new double[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		Nash_policy = new double[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		
		for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
			
			for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
				
				alpha[hisID][a] = 0.0;
				count_na[hisID][a] = 0.0;
			}
			for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
				
				beta[hisID][rank] = 0.0;
			}
		}
		
		for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
			
			for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
				
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					Nash_policy[rank][hisID][a] = 0.0;
					opponentStrategy[rank][hisID][a] = 0.0;
					bestResponse[rank][hisID][a] = 0.0;
				}
			}
		}
		
		/**
		 * set the parameters T and k
		 */
		T_equil = gameNum / 5;
		k_updateModel = gameNum / 10;
		if( gameNum <= 10 ) {
			
			T_equil = 4;
			k_updateModel = 2;
		}
		
		
		readNash();
		
		gameValues = new double[2];
		gameValues[0] = gameValues[1] = 0.0;
		computeGameValues();
	}
	
	
	public DBBR( double N, int T, int k )
	{
		super();
		
		alpha = new double[OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		beta = new double[OneCardPoker.NUM_HISTORY_IDS][Card.NUM_RANKS];
		count_na = new double[OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		opponentStrategy = new double[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		bestResponse = new double[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		Nash_policy = new double[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		
		for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
			
			for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
				
				alpha[hisID][a] = 0.0;
				count_na[hisID][a] = 0.0;
			}
			for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
				
				beta[hisID][rank] = 0.0;
			}
		}
		
		for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
			
			for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
				
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					Nash_policy[rank][hisID][a] = 0.0;
					opponentStrategy[rank][hisID][a] = 0.0;
					bestResponse[rank][hisID][a] = 0.0;
				}
			}
		}
		
		readNash();
		
		N_prior = N;
		T_equil = T;
		k_updateModel = k;
		
		gameValues = new double[2];
		gameValues[0] = gameValues[1] = 0.0;
		computeGameValues();
	}
	 
	 protected void constructOpponentModel()
	 {
		 /**
		  * compute posterior action probabilities first
		  */
		 computePostActionPro();
		 
		 /**
		  * a queue for BFS
		  */
		 ArrayList<Integer> queueList = new ArrayList<Integer>();
		 queueList.add(0);
		 
		 int parentHisID = -1;
		 
		 /**
		  * opponent's reaching probability of parent public information set
		  */
		 while( !queueList.isEmpty() ) {
			 
			 /**
			  * visit the first element in this queue
			  */
			 int hisID = queueList.remove(0);
			 parentHisID = OneCardPoker.getParentHisID(hisID);
			 
			 if( OneCardPoker.isTerminalHistory(hisID) )
				 continue;
			 
			 /**
			  * compute posterior bucket probability
			  */
			 int actionFromParent = OneCardPoker.getActionFromParent( hisID, parentHisID );
			 computePostBucketPro( hisID, parentHisID, actionFromParent );
			 
			 /**
			  * then compute opponent's strategy in this public information set
			  */
			 computeOpponentStrategy( hisID );
			 
			 /**
			  * find the child nodes and add them into the queue
			  */
			 if( hisID == 0 ) {
				 
				 queueList.add(1);
				 queueList.add(2);
			 }
			 else if( hisID == 1 ) {
				 
				 queueList.add(3);
				 queueList.add(4);
			 }
			 else if( hisID == 2 ) {
				 
				 queueList.add(5);
				 queueList.add(6);
			 }
			 else if( hisID == 4 ) {
				 
				 queueList.add(7);
				 queueList.add(8);
			 }
		 }
	 }
	 
	 protected void computePostActionPro()
	 {
		
		for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
				
			/**
			* if this history is not a decision node, then continue
			*/
			if( OneCardPoker.isTerminalHistory(hisID) )
				continue;
				
			double allCount = 0.0;
			for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
				allCount += count_na[hisID][a];
				
				//System.out.println("hisID "+hisID+" action "+a+"count "+count_na[hisID][a]);
			}
				
			for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
				/**
				 * compute the action probilities of Nash equilibrium
				*/
				double p_na = 0.0;
				for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
					
					p_na += Nash_policy[rank][hisID][a] /13.0;
				}
					
				/**
				* compute posterior action probabilities
				*/
				alpha[hisID][a] = (p_na * N_prior + count_na[hisID][a]) / (N_prior + allCount); 
			}
		}
	 }
	 
	 protected void computePostBucketPro( int hisID, int parentHisID, int actionFromParent )
	 {
		 
		 double parentReachingPro = reachingProToParent( parentHisID );
		 
		 if( hisID == 0 || actionFromParent == Action.ACTION_BET_ZERO ||
				 actionFromParent == Action.ACTION_BET_ONE ) {
			 
			 double betaSum = 0.0;
			 for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
				 
				double chancePro = 1.0 / 13.0;
				
				if( hisID == 0 )
					beta[hisID][rank] = chancePro;
				else if( hisID == 1 || hisID == 2 )
					beta[hisID][rank] = chancePro * parentReachingPro * 
							opponentStrategy[rank][parentHisID][actionFromParent];
				else
					beta[hisID][rank] = chancePro * parentReachingPro * 
							opponentStrategy[rank][parentHisID][actionFromParent];
				
				betaSum += beta[hisID][rank];
			 }
			 
			 /**
			  * normalize
			  */
			 for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
				 
				 beta[hisID][rank] /= betaSum; 
			 }
		 }
		 else {
			 
			 System.out.println("Something Wrong!");
			 System.out.println("hisID "+hisID+"parentID "+parentHisID);
			 System.out.println("actionFromParent: "+actionFromParent);
		 }
	 }
	 
	 protected void computeOpponentStrategy( int hisID )
	 {
		 /**
		  * initialized to a Nash equilibrium
		  */
		 for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
			 
			 for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
				 
				 opponentStrategy[rank][hisID][a] = Nash_policy[rank][hisID][a];
			 }
		 }
		 double[] gamma = new double[Action.NUM_ACTION_TYPES];
		 for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
			 
			 gamma[a] = 0.0;
			 for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
				 
				 gamma[a] += Nash_policy[rank][hisID][a] / 13.0;
			 }
		 }
		 
		 //outer-loop action
		 for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
			 
			 /**
			  * compare gamma with alpha
			  */
			 if( alpha[hisID][a] > gamma[a] ) {
				 
				 //find the max bucket
				 ArrayList<Integer> bucketList = new ArrayList<Integer>();
				 for( int b = 0; b < Card.NUM_RANKS; b++ ) 
					 bucketList.add(b);
				 
				 int highestIndex = 0;
				 int highestBucket = bucketList.get(highestIndex);
				 for( int index = 1; index < bucketList.size(); index++ ) {
					 
					 int bucket = bucketList.get(index);
					 if( opponentStrategy[bucket][hisID][a] > 
					 	opponentStrategy[highestBucket][hisID][a] ) {
						 
						 highestIndex = index;
						 highestBucket = bucket;
					 }
				 }
				 
				 boolean loop = true;
				 while( loop ) {
					 
					 //increase the action probability in highest bucket
					 double Delta = 0.0;
					 if( gamma[a] + beta[hisID][highestBucket] * (1 - opponentStrategy[highestBucket][hisID][a]) < 
							 alpha[hisID][a] ) {
						 
						 Delta = 1 - opponentStrategy[highestBucket][hisID][a];
						 opponentStrategy[highestBucket][hisID][a] = 1;
					 }
					 else{
						 
						 Delta = (alpha[hisID][a] - gamma[a]) / beta[hisID][highestBucket]; //if beta == 0???
						 opponentStrategy[highestBucket][hisID][a] += Delta;
					 }
					 gamma[a] += beta[hisID][highestBucket] * Delta;
					 
					 //compensate for the increase
					 if( opponentStrategy[highestBucket][hisID][1-a] >= Delta ) {
						 
						 opponentStrategy[highestBucket][hisID][1-a] -= Delta;
						 gamma[1-a] -= beta[hisID][highestBucket] * Delta;
					 }
					 else {
						 
						 gamma[1-a] -= beta[hisID][highestBucket] * opponentStrategy[highestBucket][hisID][1-a]; 
						 opponentStrategy[highestBucket][hisID][1-a] = 0;
					 }
					 
					 /**
					  * find the next highest bucket
					  */
					 bucketList.remove(highestIndex);
					 if( bucketList.isEmpty() )
						 loop = false;
					 else if( gamma[a] + beta[hisID][highestBucket] * (1 - opponentStrategy[highestBucket][hisID][a]) < 
							 alpha[hisID][a] ) {
						 
						 highestIndex = 0;
						 highestBucket = bucketList.get(highestIndex);
						 for( int index = 1; index < bucketList.size(); index++ ) {
							 
							 int bucket = bucketList.get(index);
							 if( opponentStrategy[bucket][hisID][a] > 
							 	opponentStrategy[highestBucket][hisID][a] ) {
								 
								 highestIndex = index;
								 highestBucket = bucket;
							 }
						 }
						 
						 loop = true;
					 }
					 else
						 loop = false;
				 }
			 }
			 else {
				 
				 //find the min bucket
				 ArrayList<Integer> bucketList = new ArrayList<Integer>();
				 for( int b = 0; b < Card.NUM_RANKS; b++ ) 
					 bucketList.add(b);
				 
				 int lowestIndex = 0;
				 int lowestBucket = bucketList.get(lowestIndex);
				 for( int index = 1; index < bucketList.size(); index++ ) {
					 
					 int bucket = bucketList.get(index);
					 if( opponentStrategy[bucket][hisID][a] < 
					 	opponentStrategy[lowestBucket][hisID][a] ) {
						 
						 lowestIndex = index;
						 lowestBucket = bucket;
					 }
				 }
				 
				 boolean loop = true;
				 while( loop ) {
					 
					 //decrease the probability in the lowest bucket
					 double Delta = 0.0;
					 if( alpha[hisID][a] + beta[hisID][lowestBucket] * (1 - opponentStrategy[lowestBucket][hisID][a]) < 
							 gamma[a] ) {
						 
						 Delta = opponentStrategy[lowestBucket][hisID][a];
						 opponentStrategy[lowestBucket][hisID][a] = 0;
					 }
					 else {
						 
						 Delta = (gamma[a]-alpha[hisID][a]) / beta[hisID][lowestBucket]; //if beta == 0???
						 opponentStrategy[lowestBucket][hisID][a] -= Delta;
					 }
					 gamma[a] -= Delta * beta[hisID][lowestBucket];
					 
					 
					 //compensate for the decrease
					 if( opponentStrategy[lowestBucket][hisID][1-a] + Delta <= 1 ) {
						 
						 opponentStrategy[lowestBucket][hisID][1-a] += Delta;
						 gamma[1-a] += Delta * beta[hisID][lowestBucket];
					 }
					 else {
						 
						 Delta = 1 - opponentStrategy[lowestBucket][hisID][1-a];
						 opponentStrategy[lowestBucket][hisID][1-a] = 1;
						 gamma[1-a] += Delta * beta[hisID][lowestBucket];
					 }
					 
					 bucketList.remove(lowestIndex);
					 if(bucketList.isEmpty())
						 loop = false;
					 else if( alpha[hisID][a] + beta[hisID][lowestBucket] * (1 - opponentStrategy[lowestBucket][hisID][a]) < 
							 gamma[a] ) {
						 
						 lowestIndex = 0;
						 lowestBucket = bucketList.get(lowestIndex);
						 for( int index = 1; index < bucketList.size(); index++ ) {
							 
							 int bucket = bucketList.get(index);
							 if( opponentStrategy[bucket][hisID][a] < 
							 	opponentStrategy[lowestBucket][hisID][a] ) {
								 
								 lowestIndex = index;
								 lowestBucket = bucket;
							 }
						 }
						 
						 loop = true;
					 }
					 else
						 loop = false;
				 }
			 }
		 }
		 
	 }
	 
	 protected double reachingProToParent( int parentHisID )
	 {
		 double reachingPro = 1.0;
		 
		 int ppHisID = OneCardPoker.getParentHisID( parentHisID );
		 int action = OneCardPoker.getActionFromParent( parentHisID, ppHisID );
		 
		 while( ppHisID >= 0 ) {
			 
			 reachingPro *= alpha[ppHisID][action];
			 
			 parentHisID = OneCardPoker.getParentHisID( ppHisID );
			 ppHisID = OneCardPoker.getParentHisID( parentHisID );
			 action = OneCardPoker.getActionFromParent( parentHisID, ppHisID );
		 }
		 
		 return reachingPro;
	 }
	 
	 /**
	 protected void computeBestResponse_WalkTrees()
	 {
		 for( int rank2 = 0; rank2 < Card.NUM_RANKS; rank2++ ) {
		 
			 double[] reachingPro = new double[Card.NUM_RANKS];
			 for( int rank1 = 0; rank1 < Card.NUM_RANKS; rank1++ ) {
					
				 reachingPro[rank1] = 1.0 / 13.0;  
			 }
				
			 GameState gameState = new GameState();
			 int cardPlayer2 = rank2 * Card.NUM_SUITS;
			 gameState.receiveCard( 1, cardPlayer2 );
			 
			 Helper.walkTrees_ComputeBestResponse( 2, 1, gameState, reachingPro, 
					 				opponentStrategy, bestResponse);
		 }
		 
		 for( int rank1 = 0; rank1 < Card.NUM_RANKS; rank1++ ) {
				
			 double[] reachingPro = new double[Card.NUM_RANKS];
			 for( int rank2 = 0; rank2 < Card.NUM_RANKS; rank2++ ) {
					
				 if( rank2 == rank1 ) 
					 reachingPro[rank2] = 3.0 / 51.0;
				 else
					 reachingPro[rank2] = 4.0 / 51.0;
			 }
			 
			 GameState gameState = new GameState();
			 int cardPlayer1 = rank1 * Card.NUM_SUITS;
			 gameState.receiveCard( 0, cardPlayer1 );
			 
			 Helper.walkTrees_ComputeBestResponse( 1, 1, gameState, reachingPro, 
					 				opponentStrategy, bestResponse);
		}
	 }
	 */
	 
	 /**
	 protected double walkTrees_ComputeBR( int viewer, int currentPlayer, GameState state, 
				double[] reachingPro )
	 {
		 int viewerSeat = 0;
		 int oppSeat = 1;
		 if( viewer == 2 ) {
				
			 viewerSeat = 1;
			 oppSeat = 0;
		 }
		 int viewerRank = Card.rankOfCard(state.getPlayerCard(viewerSeat));
			
		 double retValue = 0.0;
			
		 OneCardPoker.checkOver( state );
		 if( state.isGameOver() ) {
				
			 for( int oppRank = 0; oppRank < Card.NUM_RANKS; oppRank++ ) {
				
				 state.receiveCard( oppSeat, oppRank * 4 );
				 retValue += OneCardPoker.getUtility( state, viewerSeat ) * reachingPro[oppRank];
			 }
		 }
		 
		 else {
				
			 int historyID = OneCardPoker.historyID( state );
			 
			 if( viewer == currentPlayer ) {
					
				 double[] v_Ia = new double[Action.NUM_ACTION_TYPES];
				 for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
						
					 GameState nextState = new GameState( state );
					 OneCardPoker.doAction( nextState, a );
						
					 int nextPlayer = 1;
					 if( viewer == 1 )
						 nextPlayer = 2;
					 v_Ia[a] = walkTrees_ComputeBR( viewer, nextPlayer, nextState, reachingPro );
						
					 nextState = null;
				 }
				 
				 int maxAction = Action.ACTION_BET_ZERO;
				 retValue = v_Ia[0];
				 for( int a = 1; a < Action.NUM_ACTION_TYPES; a++ ) {
						
					 if( v_Ia[a] > retValue ) {
						 
						 maxAction = a;
						 retValue = v_Ia[a];
					 }
				 }
				 bestResponse[viewerRank][historyID][maxAction] = 1.0;
				 bestResponse[viewerRank][historyID][1-maxAction] = 0.0;
			 }
				
			 else {
					
				 for( int oppAction = 0; oppAction < Action.NUM_ACTION_TYPES; oppAction++ ) {
						
					 GameState nextState = new GameState( state );
					 OneCardPoker.doAction( nextState, oppAction );
					
					 double[] rp = new double[Card.NUM_RANKS];
					 for( int oppRank = 0; oppRank < Card.NUM_RANKS; oppRank++ ) {
							
						 rp[oppRank] = reachingPro[oppRank] * opponentStrategy[oppRank][historyID][oppAction];
					 }
					 
					 int nextPlayer = viewer;
					 double v_Ia = walkTrees_ComputeBR( viewer, nextPlayer, nextState, rp );
					 retValue += v_Ia;
					 
					 rp = null;
				 }
			 }

		 }
			
		 return retValue;
	 }
	 */
	 
	 
	 private void computeBestResponse_Mix()
	 {
		 /**
		  * some matrices and vectors
		  */
		 double[] e = {1,0,0,0,0,0,0};
		 double[] f = {1,0,0,0,0,0,0};
		 
		 double[][] E = generateMatrixE();
		 double[][] F = generateMatrixF();
		 
		 /**
		  * first player 1
		  */
		 for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
			 
			 double[] Ay = new double[OneCardPoker.NUM_HISTORY_IDS];
			 for( int h = 0; h < OneCardPoker.NUM_HISTORY_IDS; h++ )
				 Ay[h] = 0;
			 
			 GameState gameState = new GameState();
			 int cardPlayer1 = rank * Card.NUM_SUITS;
			 gameState.receiveCard( 0, cardPlayer1 );
			 
			 //generate payoff matrix A
			 double[] oppReachPro = new double[Card.NUM_RANKS];
			 for( int oppRank = 0; oppRank < Card.NUM_RANKS; oppRank++ ) {
				 
				 if( oppRank == rank )
					 oppReachPro[oppRank] = 3.0 / 51.0;
				 else
					 oppReachPro[oppRank] = 4.0 / 51.0;
				 
			 }
			 walkTrees_PayOffs_Ay( 1, gameState, Ay, oppReachPro );
			 
			 /**
			  * debug
			  *
			 for( int h = 0; h < OneCardPoker.NUM_HISTORY_IDS; h++ ) {
				 
				 System.out.println(Ay[h]+" "); 
			 }
			 System.out.println("==========");
			 */
			 
			 
			 
			 //set the goal
			 double[] x = new double[OneCardPoker.NUM_HISTORY_IDS];
			 
			 /**
			  * the number of constraints: 7+9+9 = 25
			  * the number of variables: History Number
			  */
			 SizableProblemI problem = new Problem( 25, OneCardPoker.NUM_HISTORY_IDS );
			 problem.getMetadata().put("lp.isMaximize", "true");
			 
			 
			 try {
				 
				 //set the goal
				 for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
					 
					 problem.newVariable("x:"+String.valueOf(hisID)).setObjectiveCoefficient(Ay[hisID]);
					 
					 //problem.getVariable("x:"+hisID).setType(VariableI.REAL);
				 }
				 
				 /**
				  * set the constraints
				  * firstly, we should let x[0] = 1, it is determined!!
				  */
				 int firstRow = 0;
				 String firstRow_ConsString = "EqualToOne"+String.valueOf(firstRow);
				 problem.newConstraint(firstRow_ConsString).setType(Constraint.EQUAL).setRightHandSide(1.0);
				 problem.setCoefficientAt(firstRow_ConsString, "x:"+String.valueOf(firstRow), 1.0);
				 
				 //set the constraints: E x = e 
				 for( int count = 0; count < e.length; count++ ) {
					 
					 String consString = "Equal"+String.valueOf(count);
					 problem.newConstraint(consString).setType(Constraint.EQUAL).setRightHandSide(e[count]);
					 
					 for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
						 
						 problem.setCoefficientAt(consString, "x:"+String.valueOf(hisID), E[count][hisID]);
					 }
				 }
				 
				 //set the constraints: x >= 0 (from history 1)
				 for( int hisID = 1; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
					 
					 String consString = "LargerThanZero"+String.valueOf(hisID);
					 problem.newConstraint(consString).setType(Constraint.GREATER).setRightHandSide(0.0);
					 problem.setCoefficientAt(consString, "x:"+String.valueOf(hisID), 1.0);
				 }
				 
				 //set the constraints: x <= 1 (from history 1)
				 for( int hisID = 1; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
					 
					 String consString = "SmallerThanOne"+String.valueOf(hisID);
					 problem.newConstraint(consString).setType(Constraint.LESS).setRightHandSide(1.0);
					 
					 problem.setCoefficientAt(consString, "x:"+String.valueOf(hisID), 1.0);
				 }
				 
				 //solve this problem
				 LinearProgrammingI iLP;
				 iLP = new DenseSimplex(problem);
				 iLP.solve();
				 VectorI v = iLP.getSolution();
				 x = v.getArray();
				 
				 /**
				  * debug
				  *
				 System.out.println("rank: "+rank);
				 for( int i = 0; i < x.length; i++ ) {
					 
					 System.out.println("x"+i+": "+x[i]);
				 }
				 System.out.println("==========");
				 */
				 
				 
				 //then we can compute the best response strategy
				 if( x[4] < 0.00001 ) {
					 
					 bestResponse[rank][4][Action.ACTION_BET_ZERO] = 1.0 / Action.NUM_ACTION_TYPES;
					 bestResponse[rank][4][Action.ACTION_BET_ONE] = 1.0 / Action.NUM_ACTION_TYPES;
				 }
				 else {
					 
					 bestResponse[rank][4][Action.ACTION_BET_ZERO] = x[7] / x[4];
					 bestResponse[rank][4][Action.ACTION_BET_ONE] = x[8] / x[4];
				 }
				 if( x[0] < 0.00001 ) {
					 
					 System.out.println("x[0]: "+x[0]);
					 bestResponse[rank][0][Action.ACTION_BET_ZERO] = 1.0 / Action.NUM_ACTION_TYPES;
					 bestResponse[rank][0][Action.ACTION_BET_ONE] = 1.0 / Action.NUM_ACTION_TYPES;
				 }
				 else {
					 
					 bestResponse[rank][0][Action.ACTION_BET_ZERO] = x[1] / x[0];
					 bestResponse[rank][0][Action.ACTION_BET_ONE] = x[2] / x[0];
				 }
				 
				 
			 }
			 catch(Exception exception) {
				 
				 exception.printStackTrace();
			 }
		 }
		 
		 /**
		  * then player 2
		  */
		 for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
			 
			 double[] Bx = new double[OneCardPoker.NUM_HISTORY_IDS];
			 for( int h = 0; h < OneCardPoker.NUM_HISTORY_IDS; h++ )
				 Bx[h] = 0; 
			 
			 GameState gameState = new GameState();
			 int cardPlayer2 = rank * Card.NUM_SUITS;
			 gameState.receiveCard( 1, cardPlayer2 );
			 
			 //generate payoff matrix B
			 double[] oppReachPro = new double[Card.NUM_RANKS];
			 for( int oppRank = 0; oppRank < Card.NUM_RANKS; oppRank++ ) {
				 
				 oppReachPro[oppRank] = 1.0 / 13.0;
			 }
			 walkTrees_PayOffs_Bx( 1, gameState, Bx, oppReachPro );
			 
			 /**
			  * debug
			  *
			 for( int h = 0; h < OneCardPoker.NUM_HISTORY_IDS; h++ ) {
				 
				 System.out.println(Bx[h]+" "); 
			 }
			 System.out.println("==========");
			 */
			 
			 //set the goal
			 double[] y = new double[OneCardPoker.NUM_HISTORY_IDS];
			 
			 /**
			  * the number of constraints: 7+9+9 = 25
			  * the number of variables: History Number
			  */
			 SizableProblemI problem = new Problem( 25, OneCardPoker.NUM_HISTORY_IDS);
			 problem.getMetadata().put("lp.isMaximize", "true");
			 //problem.getMetadata().put("lp.isMaximize", "false");
			 
			 try {
				 
				 //set the goal
				 for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
					 
					 problem.newVariable("y:"+String.valueOf(hisID)).setObjectiveCoefficient(Bx[hisID]);
				 }
				 
				//set the constraints: y[0] = 1
				 int firstRow = 0;
				 String firstRow_ConsString = "EqualToOne"+String.valueOf(firstRow);
				 problem.newConstraint(firstRow_ConsString).setType(Constraint.EQUAL).setRightHandSide(1.0);
				 problem.setCoefficientAt(firstRow_ConsString, "y:"+String.valueOf(firstRow), 1.0);
				 
				 //set the constraints: F y = f
				 for( int count = 0; count < f.length; count++ ) {
					 
					 String consString = "Equal"+String.valueOf(count);
					 problem.newConstraint(consString).setType(Constraint.EQUAL).setRightHandSide(f[count]);
					 
					 for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
						 
						 problem.setCoefficientAt(consString, "y:"+String.valueOf(hisID), F[count][hisID]);
					 }
				 }
				 
				 
				 //y >= 0
				 for( int hisID = 1; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
					 
					 String consString = "LargerThanZero"+String.valueOf(hisID);
					 problem.newConstraint(consString).setType(Constraint.GREATER).setRightHandSide(0.0);
					 problem.setCoefficientAt(consString, "y:"+String.valueOf(hisID), 1.0);
				 }
				 
				 //set the constraints: y <= 1
				 for( int hisID = 1; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
					 
					 String consString = "SmallerThanOne"+String.valueOf(hisID);
					 problem.newConstraint(consString).setType(Constraint.LESS).setRightHandSide(1.0);
					 
					 problem.setCoefficientAt(consString, "y:"+String.valueOf(hisID), 1.0);
				 }
				 
				 //solve this problem
				 LinearProgrammingI iLP;
				 iLP = new DenseSimplex(problem);
				 iLP.solve();
				 VectorI v = iLP.getSolution();
				 y = v.getArray();
				 
				 /**
				  * debug
				  *
				 System.out.println("rank: "+rank);
				 for( int i = 0; i < y.length; i++ ) {
					 
					 System.out.println("y"+i+": "+y[i]);
				 }
				 System.out.println("==========");
				 */
				 
				 
				 //then we can compute the best response strategy
				 if( y[1] < 0.00001 ) {
					 
					 bestResponse[rank][1][Action.ACTION_BET_ZERO] = 1.0 / Action.NUM_ACTION_TYPES;
					 bestResponse[rank][1][Action.ACTION_BET_ONE] = 1.0 / Action.NUM_ACTION_TYPES;
				 }
				 else {
					 
					 bestResponse[rank][1][Action.ACTION_BET_ZERO] = y[3] / y[1];
					 bestResponse[rank][1][Action.ACTION_BET_ONE] = y[4] / y[1];
				 }
				 if( y[2] < 0.00001 ) {
					 
					 bestResponse[rank][2][Action.ACTION_BET_ZERO] = 1.0 / Action.NUM_ACTION_TYPES;
					 bestResponse[rank][2][Action.ACTION_BET_ONE] = 1.0 / Action.NUM_ACTION_TYPES;
				 }
				 else {
					 
					 bestResponse[rank][2][Action.ACTION_BET_ZERO] = y[5] / y[2];
					 bestResponse[rank][2][Action.ACTION_BET_ONE] = y[6] / y[2];
				 }
				 
			 }
			 catch(Exception exception) {
				 
				 exception.printStackTrace();
			 }
		 }
		 
		 /**
		  * display 
		  */
		 Helper.computeExploitability( bestResponse );
	 }
	 
	 
	 protected double[][] generateMatrixE()
	 {
		 ArrayList<Integer> queueList = new ArrayList<Integer>();
		 for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ )
			 queueList.add(hisID);
		 
		 ArrayList<double[]> rowList = new ArrayList<double[]>();
		 
		 double[] firstRow = new double[OneCardPoker.NUM_HISTORY_IDS];
		 firstRow[0] = 1.0;
		 for( int h = 1; h < OneCardPoker.NUM_HISTORY_IDS; h++ )
			 firstRow[h] = 0.0;
		 rowList.add(firstRow);
		 
		 while( !queueList.isEmpty() ) {
			 
			 int hisID = queueList.remove(0);
			 
			 if( OneCardPoker.isTerminalHistory(hisID) )
				 continue;
			 
			 /**
			  * find the child
			  */
			 int[] childIDs = OneCardPoker.getChildHisIDs( hisID );
			 if( childIDs != null ) {
				 
				 /**
				  * matrix E is generate for player 1
				  */
				 if( hisID == 0 || hisID == 4 ) {
					 
					 double[] row = new double[OneCardPoker.NUM_HISTORY_IDS];
					 for( int h = 0; h < OneCardPoker.NUM_HISTORY_IDS; h++ )
						 row[h] = 0.0;
					 
					 row[hisID] = -1;
					 for( int i = 0; i < childIDs.length; i++ ) {
						 
						 int childHisID = childIDs[i];
						 row[childHisID] = 1;
					 }
					 rowList.add(row);
				 }
				 /**
				  * the opponent's decision node
				  */
				 else {
					 
					 for( int i = 0; i < childIDs.length; i++ ) {
						 
						 double[] row = new double[OneCardPoker.NUM_HISTORY_IDS];
						 for( int h = 0; h < OneCardPoker.NUM_HISTORY_IDS; h++ )
							 row[h] = 0.0;
						 
						 int childHisID = childIDs[i];
						 row[hisID] = -1;
						 row[childHisID] = 1;
						 
						 rowList.add(row);
					 }
				 }
			 }
		 }
		 
		 double[][] E = new double[rowList.size()][OneCardPoker.NUM_HISTORY_IDS];
		 for( int rowIndex = 0; rowIndex < rowList.size(); rowIndex++ ) {
			 
			 double[] row = rowList.get(rowIndex);
			 for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
				 
				 E[rowIndex][hisID] = row[hisID];
			 }
		 }
		 
		 return E;
	 }
	 
	 
	 
	 protected double[][] generateMatrixF()
	 {
		 ArrayList<Integer> queueList = new ArrayList<Integer>();
		 for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ )
			 queueList.add(hisID);
		 
		 ArrayList<double[]> rowList = new ArrayList<double[]>();
		 
		 double[] firstRow = new double[OneCardPoker.NUM_HISTORY_IDS];
		 firstRow[0] = 1.0;
		 for( int h = 1; h < OneCardPoker.NUM_HISTORY_IDS; h++ )
			 firstRow[h] = 0.0;
		 rowList.add(firstRow);
		 
		 while( !queueList.isEmpty() ) {
			 
			 int hisID = queueList.remove(0);
			 if( OneCardPoker.isTerminalHistory(hisID) )
				 continue;
			 
			 /**
			  * find the child
			  */
			 int[] childIDs = OneCardPoker.getChildHisIDs( hisID );
			 if( childIDs != null ) {
				 
				 /**
				  * matrix F is generate for player 2
				  */
				 if( hisID == 1 || hisID == 2 ) {
					 
					 double[] row = new double[OneCardPoker.NUM_HISTORY_IDS];
					 for( int h = 0; h < OneCardPoker.NUM_HISTORY_IDS; h++ )
						 row[h] = 0.0;
					 
					 row[hisID] = -1;
					 for( int i = 0; i < childIDs.length; i++ ) {
						 
						 int childHisID = childIDs[i];
						 row[childHisID] = 1;
					 }
					 rowList.add(row);
				 }
				 /**
				  * the opponent's decision node
				  */
				 else {
					 
					 for( int i = 0; i < childIDs.length; i++ ) {
						 
						 double[] row = new double[OneCardPoker.NUM_HISTORY_IDS];
						 for( int h = 0; h < OneCardPoker.NUM_HISTORY_IDS; h++ )
							 row[h] = 0.0;
						 
						 int childHisID = childIDs[i];
						 row[hisID] = -1;
						 row[childHisID] = 1;
						 
						 rowList.add(row);
					 }
				 }
			 }
		 }
		 
		 double[][] F = new double[rowList.size()][OneCardPoker.NUM_HISTORY_IDS];
		 for( int rowIndex = 0; rowIndex < rowList.size(); rowIndex++ ) {
			 
			 double[] row = rowList.get(rowIndex);
			 for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
				 
				 F[rowIndex][hisID] = row[hisID];
			 }
		 }
		 
		 return F;
	 }
	 
	 
	 
	 
	 protected void walkTrees_PayOffs_Ay( int currentPlayer, GameState state, double[] Ay, 
			 double[] oppReachPro)
	 {
		 int viewer = 1;
		 int viewerSeat = 0;
		 int oppSeat = 1;
		 
		 int viewerRank = Card.rankOfCard(state.getPlayerCard(viewerSeat));
		 int historyID = OneCardPoker.historyID( state );
		 
		 OneCardPoker.checkOver( state );
		 if( state.isGameOver() ) {
				
			 double payoffs = 0.0;
			 for( int oppRank = 0; oppRank < Card.NUM_RANKS; oppRank++ ) {
				
				 state.receiveCard( oppSeat, oppRank * 4 );
				 
				 if( oppRank == viewerRank )
					 payoffs += OneCardPoker.getUtility( state, viewerSeat ) * oppReachPro[oppRank] / 13.0;
				 else
					 payoffs += OneCardPoker.getUtility( state, viewerSeat ) * oppReachPro[oppRank] / 13.0;
			 }
			 Ay[historyID] = payoffs;
		 }
		 else {
			
			 if( currentPlayer == 1 ) {
				 
				 for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
						
					 GameState nextState = new GameState( state );
					 OneCardPoker.doAction( nextState, a );
					 
					 int nextPlayer = 2;
					 
					 walkTrees_PayOffs_Ay( nextPlayer, nextState, Ay, oppReachPro );	
					 nextState = null;
				 } 
			 }
			 else {
				 
				  
				 for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					 double[] rp = new double[Card.NUM_RANKS];
					 
					 GameState nextState = new GameState( state );
					 OneCardPoker.doAction( nextState, a );
					 
					 int nextPlayer = 1;
					 for( int r = 0; r < Card.NUM_RANKS; r++ )
						 rp[r] = oppReachPro[r] * opponentStrategy[r][historyID][a];
							 
					 walkTrees_PayOffs_Ay( nextPlayer, nextState, Ay, rp );	
					 nextState = null;
					 
					 rp = null;
				 }  
			 }
		 }
	 }
	 
	 
	 
	 protected void walkTrees_PayOffs_Bx( int currentPlayer, GameState state, double[] Bx, 
			 double[] oppReachPro )
	 {
		 int viewer = 2;
		 int viewerSeat = 1;
		 int oppSeat = 0;
		 
		 int viewerRank = Card.rankOfCard(state.getPlayerCard(viewerSeat));
		 int historyID = OneCardPoker.historyID( state );
		 
		 OneCardPoker.checkOver( state );
		 if( state.isGameOver() ) {
				
			 double payoffs = 0.0;
			 for( int oppRank = 0; oppRank < Card.NUM_RANKS; oppRank++ ) {
				
				 state.receiveCard( oppSeat, oppRank * Card.NUM_SUITS );
				 
				 if( oppRank == viewerRank )
					 payoffs += OneCardPoker.getUtility( state, viewerSeat ) * oppReachPro[oppRank] * 3.0 / 51.0;
				 else
					 payoffs += OneCardPoker.getUtility( state, viewerSeat ) * oppReachPro[oppRank] * 4.0 / 51.0;
			 }
			 Bx[historyID] = payoffs;
		 }
		 else {
			
			 if( currentPlayer == 1 ) {
				 
				 for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
						
					 double[] rp = new double[Card.NUM_RANKS];
					 
					 GameState nextState = new GameState( state );
					 OneCardPoker.doAction( nextState, a );
					 
					 for( int r = 0; r < Card.NUM_RANKS; r++ )
						 rp[r] = oppReachPro[r] * opponentStrategy[r][historyID][a];
					 
					 int nextPlayer = 2;
					 
					 walkTrees_PayOffs_Bx( nextPlayer, nextState, Bx, rp );	
					 nextState = null;
					 
					 rp = null;
				 }
			 }
			 else {
				 
				 for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					 
					 GameState nextState = new GameState( state );
					 OneCardPoker.doAction( nextState, a );
					 
					 int nextPlayer = 1;
					 
					 walkTrees_PayOffs_Bx( nextPlayer, nextState, Bx, oppReachPro );	
					 nextState = null;
					 
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
				 bestResponse[rank][his][a] = Nash_policy[rank][his][a];
					
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
	 
	 
	 /**
	  * compute the value of the game according to a Nash equilibrium
	  */
	 protected void computeGameValues()
	 {
		 double[] p1 = new double[Card.NUM_RANKS];
		 double[] p2 = new double[Card.NUM_RANKS];
		 
		 for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
			 
			 p1[rank] = 1.0;
			 p2[rank] = 1.0;
		 }
		 
		 GameState gameState = new GameState();
		 
		 gameValues[0] = Helper.walkTrees_ComputeStrategyValue(1, 1, gameState, p1, p2, Nash_policy, Nash_policy);
		 gameValues[1] = 0 - gameValues[0];
		 
		 p1 = null;
		 p2 = null;
		 gameState = null;
	 }
	 
	 /**
	 private void walkTrees_ComputeGameValues( int currentPlayer, GameState gameState, 
			 double[] p1, double[] p2 )
	 {		 
		 int hisID = OneCardPoker.historyID( gameState );
		 
		 OneCardPoker.checkOver( gameState );
		 if( gameState.isGameOver() ) {
				
			 for( int rank1 = 0; rank1 < Card.NUM_RANKS; rank1++ ) {
				 
				 gameState.receiveCard( 0, rank1 * Card.NUM_SUITS );
				 double chancePro1 = 1.0 / 13.0;
				 
				 for( int rank2 = 0; rank2 < Card.NUM_RANKS; rank2++ ) {
					 
					 gameState.receiveCard( 1, rank2 * Card.NUM_SUITS );
					 double chancePro2 = 4.0 / 51.0;
					 if( rank2 == rank1 )
						 chancePro2 = 3.0 / 51.0;
					 
					 gameValues[0] += OneCardPoker.getUtility( gameState, 0 ) * 
					 					chancePro1 * chancePro2 * p1[rank1] * p2[rank2];
					 gameValues[1] += OneCardPoker.getUtility( gameState, 1 ) * 
	 									chancePro1 * chancePro2 * p1[rank1] * p2[rank2];
				 }
			 }
		 }
		 else {
			 
			 if( currentPlayer == 1 ) {
				 
				 for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
						
					 GameState nextState = new GameState( gameState );
					 OneCardPoker.doAction( nextState, a );
						
					 int nextPlayer = 2;
					 
					 double[] p1_p = new double[Card.NUM_RANKS];
					 for( int rank1 = 0; rank1 < Card.NUM_RANKS; rank1++ ) {
						 
						 p1_p[rank1] = p1[rank1] * Nash_policy[rank1][hisID][a]; 
					 }
					 walkTrees_ComputeGameValues( nextPlayer, nextState, p1_p, p2 );
						
					 nextState = null;
					 p1_p = null;
				 }
			 }
			 else {
				 
				 for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
						
					 GameState nextState = new GameState( gameState );
					 OneCardPoker.doAction( nextState, a );
						
					 int nextPlayer = 1;
					 
					 double[] p2_p = new double[Card.NUM_RANKS];
					 for( int rank2 = 0; rank2 < Card.NUM_RANKS; rank2++ ) {
						 
						 p2_p[rank2] = p2[rank2] * Nash_policy[rank2][hisID][a]; 
					 }
					 walkTrees_ComputeGameValues( nextPlayer, nextState, p1, p2_p );
						
					 nextState = null;
					 p2_p = null;
				 }
			 }
		 }
	 }
	 */
	 
	 
	 
	 private void computeNemesis_Mix()
	 {
		 /**
		  * some matrices and vectors
		  */
		 double[] e = {1,0,0,0,0,0,0};
		 double[] f = {1,0,0,0,0,0,0};
		 
		 double[][] E = generateMatrixE();
		 double[][] F = generateMatrixF();
		 
		 /**
		  * first player 1
		  * compute the nemesis to player 1
		  * use player 1's payoff 
		  * but what we 
		  */
		 for( int rank2 = 0; rank2 < Card.NUM_RANKS; rank2++ ) {
			 
			 double[] xA = new double[OneCardPoker.NUM_HISTORY_IDS];
			 for( int h = 0; h < OneCardPoker.NUM_HISTORY_IDS; h++ )
				 xA[h] = 0;
			 
			 GameState gameState = new GameState();
			 int cardPlayer2 = rank2 * Card.NUM_SUITS;
			 gameState.receiveCard( 1, cardPlayer2 );
			 
			 //generate payoff matrix A
			 double[] reachPro = new double[Card.NUM_RANKS];
			 for( int rank1 = 0; rank1 < Card.NUM_RANKS; rank1++ ) {

				 reachPro[rank1] = 1.0 / 13.0;
			 }
			 walkTrees_PayOffs_xA( 1, gameState, xA, reachPro );
			 
			 /**
			  * debug
			  *
			 for( int h = 0; h < OneCardPoker.NUM_HISTORY_IDS; h++ ) {
				 
				 System.out.println(Ay[h]+" "); 
			 }
			 System.out.println("==========");
			 */
			 
			 
			 
			 //set the goal
			 double[] y = new double[OneCardPoker.NUM_HISTORY_IDS];
			 
			 /**
			  * the number of constraints: 7+9+9 = 25
			  * the number of variables: History Number
			  */
			 SizableProblemI problem = new Problem( 25, OneCardPoker.NUM_HISTORY_IDS );
			 //problem.getMetadata().put("lp.isMaximize", "true");
			 problem.getMetadata().put("lp.isMinimize", "true");
			 
			 
			 try {
				 
				 //set the goal
				 for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
					 
					 problem.newVariable("y:"+String.valueOf(hisID)).setObjectiveCoefficient(xA[hisID]);
				 }
				 
				 /**
				  * set the constraints
				  * firstly, we should let y[0] = 1, it is determined!!
				  */
				 int firstRow = 0;
				 String firstRow_ConsString = "EqualToOne"+String.valueOf(firstRow);
				 problem.newConstraint(firstRow_ConsString).setType(Constraint.EQUAL).setRightHandSide(1.0);
				 problem.setCoefficientAt(firstRow_ConsString, "y:"+String.valueOf(firstRow), 1.0);
				 
				 //set the constraints: F y = f 
				 for( int count = 0; count < f.length; count++ ) {
					 
					 String consString = "Equal"+String.valueOf(count);
					 problem.newConstraint(consString).setType(Constraint.EQUAL).setRightHandSide(e[count]);
					 
					 for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
						 
						 problem.setCoefficientAt(consString, "y:"+String.valueOf(hisID), F[count][hisID]);
					 }
				 }
				 
				 //set the constraints: y >= 0 (from history 1)
				 for( int hisID = 1; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
					 
					 String consString = "LargerThanZero"+String.valueOf(hisID);
					 problem.newConstraint(consString).setType(Constraint.GREATER).setRightHandSide(0.0);
					 problem.setCoefficientAt(consString, "y:"+String.valueOf(hisID), 1.0);
				 }
				 
				 //set the constraints: y <= 1 (from history 1)
				 for( int hisID = 1; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
					 
					 String consString = "SmallerThanOne"+String.valueOf(hisID);
					 problem.newConstraint(consString).setType(Constraint.LESS).setRightHandSide(1.0);
					 
					 problem.setCoefficientAt(consString, "y:"+String.valueOf(hisID), 1.0);
				 }
				 
				 //solve this problem
				 LinearProgrammingI iLP;
				 iLP = new DenseSimplex(problem);
				 iLP.solve();
				 VectorI v = iLP.getSolution();
				 y = v.getArray();
				 
				 /**
				  * debug
				  *
				 System.out.println("rank: "+rank2);
				 for( int i = 0; i < y.length; i++ ) {
					 
					 System.out.println("y"+i+": "+y[i]);
				 }
				 System.out.println("==========");
				 */
				 
				 
				 
				 //then we can compute the best response strategy
				 if( y[1] < 0.00001 ) {
					 
					 bestResponse[rank2][1][Action.ACTION_BET_ZERO] = 1.0 / Action.NUM_ACTION_TYPES;
					 bestResponse[rank2][1][Action.ACTION_BET_ONE] = 1.0 / Action.NUM_ACTION_TYPES;
				 }
				 else {
					 
					 bestResponse[rank2][1][Action.ACTION_BET_ZERO] = y[3] / y[1];
					 bestResponse[rank2][1][Action.ACTION_BET_ONE] = y[4] / y[1];
				 }
				 if( y[2] < 0.00001 ) {
					 
					 bestResponse[rank2][2][Action.ACTION_BET_ZERO] = 1.0 / Action.NUM_ACTION_TYPES;
					 bestResponse[rank2][2][Action.ACTION_BET_ONE] = 1.0 / Action.NUM_ACTION_TYPES;
				 }
				 else {
					 
					 bestResponse[rank2][2][Action.ACTION_BET_ZERO] = y[5] / y[2];
					 bestResponse[rank2][2][Action.ACTION_BET_ONE] = y[6] / y[2];
				 }
			 }
			 catch(Exception exception) {
				 
				 exception.printStackTrace();
			 }
		 }
		 
		 /**
		  * then player 2
		  */
		 for( int rank1 = 0; rank1 < Card.NUM_RANKS; rank1++ ) {
			 
			 double[] yB = new double[OneCardPoker.NUM_HISTORY_IDS];
			 for( int h = 0; h < OneCardPoker.NUM_HISTORY_IDS; h++ )
				 yB[h] = 0; 
			 
			 GameState gameState = new GameState();
			 int cardPlayer1 = rank1 * Card.NUM_SUITS;
			 gameState.receiveCard( 0, cardPlayer1 );
			 
			 //generate payoff matrix B
			 double[] reachPro = new double[Card.NUM_RANKS];
			 for( int rank2 = 0; rank2 < Card.NUM_RANKS; rank2++ ) {
				 
				 reachPro[rank2] = 4.0 / 51.0;
				 if( rank2 == rank1 )
					 reachPro[rank2] = 3.0 / 51.0; 
			 }
			 walkTrees_PayOffs_yB( 1, gameState, yB, reachPro );
			 
			 /**
			  * debug
			  *
			 for( int h = 0; h < OneCardPoker.NUM_HISTORY_IDS; h++ ) {
				 
				 System.out.println(Bx[h]+" "); 
			 }
			 System.out.println("==========");
			 */
			 
			 //set the goal
			 double[] x = new double[OneCardPoker.NUM_HISTORY_IDS];
			 
			 /**
			  * the number of constraints: 7+9+9 = 25
			  * the number of variables: History Number
			  */
			 SizableProblemI problem = new Problem( 25, OneCardPoker.NUM_HISTORY_IDS);
			 //problem.getMetadata().put("lp.isMaximize", "true");
			 //problem.getMetadata().put("lp.isMaximize", "false");
			 problem.getMetadata().put("lp.isMinimize", "true");
			 try {
				 
				 //set the goal
				 for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
					 
					 problem.newVariable("x:"+String.valueOf(hisID)).setObjectiveCoefficient(yB[hisID]);
				 }
				 
				//set the constraints: x[0] = 1
				 int firstRow = 0;
				 String firstRow_ConsString = "EqualToOne"+String.valueOf(firstRow);
				 problem.newConstraint(firstRow_ConsString).setType(Constraint.EQUAL).setRightHandSide(1.0);
				 problem.setCoefficientAt(firstRow_ConsString, "x:"+String.valueOf(firstRow), 1.0);
				 
				 //set the constraints: E x = e
				 for( int count = 0; count < e.length; count++ ) {
					 
					 String consString = "Equal"+String.valueOf(count);
					 problem.newConstraint(consString).setType(Constraint.EQUAL).setRightHandSide(f[count]);
					 
					 for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
						 
						 problem.setCoefficientAt(consString, "x:"+String.valueOf(hisID), E[count][hisID]);
					 }
				 }
				 
				 
				 //x >= 0
				 for( int hisID = 1; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
					 
					 String consString = "LargerThanZero"+String.valueOf(hisID);
					 problem.newConstraint(consString).setType(Constraint.GREATER).setRightHandSide(0.0);
					 problem.setCoefficientAt(consString, "x:"+String.valueOf(hisID), 1.0);
				 }
				 
				 //set the constraints: x <= 1
				 for( int hisID = 1; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
					 
					 String consString = "SmallerThanOne"+String.valueOf(hisID);
					 problem.newConstraint(consString).setType(Constraint.LESS).setRightHandSide(1.0);
					 
					 problem.setCoefficientAt(consString, "x:"+String.valueOf(hisID), 1.0);
				 }
				 
				 //solve this problem
				 LinearProgrammingI iLP;
				 iLP = new DenseSimplex(problem);
				 iLP.solve();
				 VectorI v = iLP.getSolution();
				 x = v.getArray();
				 
				 /**
				  * debug
				  *
				 System.out.println("rank: "+rank);
				 for( int i = 0; i < y.length; i++ ) {
					 
					 System.out.println("y"+i+": "+y[i]);
				 }
				 System.out.println("==========");
				 */
				 
				 
				 //then we can compute the best response strategy
				 
				 if( x[4] < 0.00001 ) {
					 
					 bestResponse[rank1][4][Action.ACTION_BET_ZERO] = 1.0 / Action.NUM_ACTION_TYPES;
					 bestResponse[rank1][4][Action.ACTION_BET_ONE] = 1.0 / Action.NUM_ACTION_TYPES;
				 }
				 else {
					 
					 bestResponse[rank1][4][Action.ACTION_BET_ZERO] = x[7] / x[4];
					 bestResponse[rank1][4][Action.ACTION_BET_ONE] = x[8] / x[4];
				 }
				 if( x[0] < 0.00001 ) {
					 
					 System.out.println("x[0]: "+x[0]);
					 bestResponse[rank1][0][Action.ACTION_BET_ZERO] = 1.0 / Action.NUM_ACTION_TYPES;
					 bestResponse[rank1][0][Action.ACTION_BET_ONE] = 1.0 / Action.NUM_ACTION_TYPES;
				 }
				 else {
					 
					 bestResponse[rank1][0][Action.ACTION_BET_ZERO] = x[1] / x[0];
					 bestResponse[rank1][0][Action.ACTION_BET_ONE] = x[2] / x[0];
				 }
				 
			 }
			 catch(Exception exception) {
				 
				 exception.printStackTrace();
			 }
		 }
		 
		 /**
		  * display 
		  */
		 Helper.computeExploitability( bestResponse );
	 }
	 
	 
	 
	 protected void walkTrees_PayOffs_xA( int currentPlayer, GameState state, double[] xA, 
			 double[] reachPro )
	 {
		 int viewer = 1;
		 int viewerSeat = 0;
		 int oppSeat = 1;
		 
		 int oppRank = Card.rankOfCard(state.getPlayerCard(oppSeat));
		 int historyID = OneCardPoker.historyID( state );
		 
		 OneCardPoker.checkOver( state );
		 if( state.isGameOver() ) {
				
			 double payoffs = 0.0;
			 for( int viewerRank = 0; viewerRank < Card.NUM_RANKS; viewerRank++ ) {
				
				 state.receiveCard( viewerSeat, viewerRank * Card.NUM_SUITS );
				 
				 if( oppRank == viewerRank )
					 payoffs += OneCardPoker.getUtility( state, viewerSeat ) * reachPro[viewerRank] * 3.0 / 51.0;
				 else
					 payoffs += OneCardPoker.getUtility( state, viewerSeat ) * reachPro[viewerRank] * 4.0 / 51.0;
			 }
			 xA[historyID] = payoffs;
		 }
		 else {
			
			 if( currentPlayer == 1 ) {
				 
				 for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
						
					 GameState nextState = new GameState( state );
					 OneCardPoker.doAction( nextState, a );
					 
					 int nextPlayer = 2;
					 
					 double[] rp = new double[Card.NUM_RANKS];
					 for( int r = 0; r < Card.NUM_RANKS; r++ )
						 rp[r] = reachPro[r] * opponentStrategy[r][historyID][a];
					 
					 walkTrees_PayOffs_xA( nextPlayer, nextState, xA, rp );
					 
					 nextState = null;
					 rp = null;
				 } 
			 }
			 else {
				 
				  
				 for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {	
					 
					 GameState nextState = new GameState( state );
					 OneCardPoker.doAction( nextState, a );
					 
					 int nextPlayer = 1;
							 
					 walkTrees_PayOffs_xA( nextPlayer, nextState, xA, reachPro );	
					 nextState = null;
				 }  
			 }
		 }
	 }
	 
	 protected void walkTrees_PayOffs_yB( int currentPlayer, GameState state, double[] yB, 
			 double[] reachPro )
	 {
		 int viewer = 2;
		 int viewerSeat = 1;
		 int oppSeat = 0;
		 
		 int oppRank = Card.rankOfCard(state.getPlayerCard(oppSeat));
		 int historyID = OneCardPoker.historyID( state );
		 
		 OneCardPoker.checkOver( state );
		 if( state.isGameOver() ) {
				
			 double payoffs = 0.0;
			 for( int viewerRank = 0; viewerRank < Card.NUM_RANKS; viewerRank++ ) {
				
				 state.receiveCard( viewerSeat, viewerRank * Card.NUM_SUITS );
				 
				 payoffs += OneCardPoker.getUtility( state, viewerSeat ) * reachPro[viewerRank] / 13.0;
			 }
			 yB[historyID] = payoffs;
		 }
		 else {
			
			 if( currentPlayer == 1 ) {
				 
				 for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					 GameState nextState = new GameState( state );
					 OneCardPoker.doAction( nextState, a );
					 
					 int nextPlayer = 2;
					 
					 walkTrees_PayOffs_yB( nextPlayer, nextState, yB, reachPro );	
					 nextState = null;
				 }
			 }
			 else {
				 
				 for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					 
					 double[] rp = new double[Card.NUM_RANKS];
					 GameState nextState = new GameState( state );
					 OneCardPoker.doAction( nextState, a );
					 
					 int nextPlayer = 1;
					 for( int r = 0; r < Card.NUM_RANKS; r++ )
						 rp[r] = reachPro[r] * opponentStrategy[r][historyID][a];
					 
					 walkTrees_PayOffs_yB( nextPlayer, nextState, yB, rp );	
					 nextState = null;
					 
					 rp = null;
				 }
			 }
		 }
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
				
			 int retAction = 0;
			 
			 double[] pro = new double[Action.NUM_ACTION_TYPES];
			 pro[0] = bestResponse[playerRank][hisID][0];
			 for( int a = 1; a < Action.NUM_ACTION_TYPES; a++ ) {
					
				 pro[a] = pro[a-1] + bestResponse[playerRank][hisID][a];
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
	 
	 public void onlinePlay_GameOver( GameState gameState, long T )
	 {
		
		 if((T == T_equil) || 
			(T > T_equil && (T % k_updateModel == 0)) ) {
			 
			 constructOpponentModel();
			 
			 /**
			  * For computing a best response, we can either use 
			  * walkTrees or linear programming. The two methods 
			  * are equivalence. 
			  * 
			  */
			 Helper.computeBestResponse( opponentStrategy, bestResponse );
			 //Helper.computeExploitability( bestResponse );

			 //computeBestResponse_Mix();
			 
			 //Helper.displayPolicy(opponentStrategy);
			 //Helper.displayPolicy(myPolicy);
			 //System.out.println("=============");
			 
			 //reset or not?? Not!!!
			 for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
				 
				 for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					 
					 //count_na[hisID][a] = 0.0;
				 }
			 }
		 }
	 }
	 
	 public void onlinePlay_ObserveOpponent( GameState gameState, long T, int action )
	 {
		 
		 int hisID = OneCardPoker.historyID( gameState );
		 
		 OneCardPoker.checkOver( gameState );
		 if( gameState.isGameOver() ) {
				
			 return;
		 }
		 else {
			 
			 count_na[hisID][action] += 1.0;
		 }
	 }
	 
}
