package gameOneCardPoker.algorithms;

import gameOneCardPoker.Action;
import gameOneCardPoker.Card;
import gameOneCardPoker.GameState;
import gameOneCardPoker.OneCardPoker;

import java.util.Random;

import drasys.or.mp.Constraint;
import drasys.or.mp.Problem;
import drasys.or.mp.SizableProblemI;

public class RWYWE extends DBBR {

	
	double[] epsilonSafeValues;
	
	public RWYWE()
	{
		super();
		
		/**
		 * compute the game value
		 */
		computeGameValues();
		
		epsilonSafeValues = new double[Card.NUM_RANKS];
		for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) 
			epsilonSafeValues[rank] = 0.0; 
	}
	
	public RWYWE( double N, int T, int k )
	{
		super(N, T, k);
		
		/**
		 * compute the game value
		 */
		computeGameValues();
		
		epsilonSafeValues = new double[Card.NUM_RANKS];
		for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) 
			epsilonSafeValues[rank] = 0.0; 
	}
	
	
	private void computeSafeBestResponse( )
	{
		/**
		 * only compute for the player that the agent being
		 * according to the current seat
		 */
		if( seat == 0 ) {
			
			/**
			 * if we compute the epsilon-safe best response overall information set
			 * the corresponding linear programming is so big (so many variables, over 6^14 constraints!!)
			 * 
			 * our idea is that compute the epsilon-safe best response in each private information set
			 * each private information set maintains a independent value of epsilon
			 */

			 try {
				 
				 double[] e = {1,0,0,0,0,0,0};
				 double[] f = {1,0,0,0,0,0,0};
				 
				 double[][] E = generateMatrixE();
				 double[][] F = generateMatrixF();
				 
				 for( int rank1 = 0; rank1 < Card.NUM_RANKS; rank1++ ) {
					 
					 SizableProblemI problem = new Problem( 25, OneCardPoker.NUM_HISTORY_IDS );
					 problem.getMetadata().put("lp.isMaximize", "true");
					 
					 /**
					  * set variables: the part of sigma_i
					  */
					 double[] Ay = new double[OneCardPoker.NUM_HISTORY_IDS];
					 for( int h = 0; h < OneCardPoker.NUM_HISTORY_IDS; h++ )
						 Ay[h] = 0;
					 
					 GameState gameState = new GameState();
					 int cardPlayer1 = rank1 * Card.NUM_SUITS;
					 gameState.receiveCard( 0, cardPlayer1 );
					 
					 double[] oppReachPro = new double[Card.NUM_RANKS];
					 for( int oppRank = 0; oppRank < Card.NUM_RANKS; oppRank++ ) {
						 
						 if( oppRank == rank1 )
							 oppReachPro[oppRank] = 3.0 / 51.0;
						 else
							 oppReachPro[oppRank] = 4.0 / 51.0;
						 
					 }
					 walkTrees_PayOffs_Ay( 1, gameState, Ay, oppReachPro );
					 
					 for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
						 
						 String variableName = "Rank"+String.valueOf(rank1)+"x"+String.valueOf(hisID); 
						 problem.newVariable(variableName).setObjectiveCoefficient(Ay[hisID]);
					 }
					 
					 /**
					  * set basic constraints
					  */
					 int firstRow = 0;
					 String firstRow_ConsString = "Rank"+String.valueOf(rank1)+"EqualToOne"+String.valueOf(firstRow);
					 problem.newConstraint(firstRow_ConsString).setType(Constraint.EQUAL).setRightHandSide(1.0);
					 problem.setCoefficientAt(firstRow_ConsString, 
							 "Rank"+String.valueOf(rank1)+"x"+String.valueOf(firstRow), 1.0);
					 
					 //set the constraints: E x = e 
					 for( int count = 0; count < e.length; count++ ) {
						 
						 String consString = "Rank"+String.valueOf(rank1)+"Equal"+String.valueOf(count);
						 problem.newConstraint(consString).setType(Constraint.EQUAL).setRightHandSide(e[count]);
						 
						 for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
							 
							 problem.setCoefficientAt(consString, 
									 "Rank"+String.valueOf(rank1)+"x"+String.valueOf(hisID), E[count][hisID]);
						 }
					 }
					 
					 //set the constraints: x >= 0 (from history 1)
					 for( int hisID = 1; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
						 
						 String consString = "Rank"+String.valueOf(rank1)+"LargerThanZero"+String.valueOf(hisID);
						 problem.newConstraint(consString).setType(Constraint.GREATER).setRightHandSide(0.0);
						 problem.setCoefficientAt(consString, 
								 "Rank"+String.valueOf(rank1)+"x"+String.valueOf(hisID), 1.0);
					 }
					 
					 //set the constraints: x <= 1 (from history 1)
					 for( int hisID = 1; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
						 
						 String consString = "Rank"+String.valueOf(rank1)+"SmallerThanOne"+String.valueOf(hisID);
						 problem.newConstraint(consString).setType(Constraint.LESS).setRightHandSide(1.0);
						 
						 problem.setCoefficientAt(consString, 
								 "Rank"+String.valueOf(rank1)+"x"+String.valueOf(hisID), 1.0);
					 }
					 
					 /**
					  * set constraints for safety:
					  * the exploitability of sigma_i should be smaller than (v*_1-epsilon)
					  * the problem is that we should compute the best response of sigma_i first
					  * this is impossible since sigma_i has not been determined yet
					  * according to our experience, the best response of a mixed strategy is pure!
					  * so we just enumerate the possible condition of sigma_-i (so many!!!....)
					  */
				 }
				 
			 }
			 catch(Exception ep) {
				 
			 }

		}
		else {
			
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
		/**
		 * update the opponent model on each iteration!!
		 * and modify our strategy
		 */
		 constructOpponentModel();
		 
		 /**
		  * from the player's perspective
		  * compute epsilon-safe best response 
		  * to the modeled opponent strategy
		  */
		 computeSafeBestResponse();
		 
		 /**
		  * update the epsilon value
		  */
		 
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
