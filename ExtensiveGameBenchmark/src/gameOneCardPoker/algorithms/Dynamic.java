package gameOneCardPoker.algorithms;

import gameOneCardPoker.Action;
import gameOneCardPoker.Card;
import gameOneCardPoker.GameState;
import gameOneCardPoker.OneCardPoker;

import java.util.Random;

public class Dynamic extends DBBR {

	private int deceptionRoundNum = 100;
	
	private double randomPro = 0.5;
	
	public Dynamic()
	{
		super();
	}
	
	public Dynamic( int gameNum ) 
	{
		super();
		
		/**
		 * one fifth of the match number
		 */
		deceptionRoundNum = gameNum / 5;
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
			 
			 if( T <= deceptionRoundNum ) {
				 
				 /**
				 pro[0] = 1.0 / Action.NUM_ACTION_TYPES;
				 for( int a = 1; a < Action.NUM_ACTION_TYPES; a++ ) {
						
					 pro[a] = pro[a-1] + 1.0 / Action.NUM_ACTION_TYPES;
				 }
				 */
				 
				 pro[0] = randomPro;
				 pro[1] = 1.0;
			 }
			 else {
				 
				 pro[0] = bestResponse[playerRank][hisID][0];
				 for( int a = 1; a < Action.NUM_ACTION_TYPES; a++ ) {
						
					 pro[a] = pro[a-1] + bestResponse[playerRank][hisID][a];
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
	 
		public void setRandomPro( double rndPro ) 
		{
			if( rndPro < 0 || rndPro > 1 )
				rndPro = 0.5;
			
			randomPro = rndPro;
		}
}
