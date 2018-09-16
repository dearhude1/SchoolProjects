package gameOneCardPoker.algorithms;

import gameOneCardPoker.GameState;
import gameOneCardPoker.Helper;

public class BEFFE extends BEFEWP {

	private int maxMatchNum = 0;
	
	public BEFFE( int gameNum )
	{
		super();
		
		maxMatchNum = gameNum;
	}
	
	public void onlinePlayer_GameStarts( GameState gameState, long T )
	{
		if( T == 0 ) {
			
			usingNash = true;
			return;
		}
		
		/**
		  * compute the exploitability of the variable: best response 
		  * according to the current seat
		  */
		 Helper.computeNemesisStrategy( bestResponse, nemesisPolicy, 1 );
		 Helper.computeNemesisStrategy( bestResponse, nemesisPolicy, 2 );
		 
		 double nemesisValue1 = Helper.computeStrategyValue( 1, bestResponse, nemesisPolicy );
		 double nemesisValue2 = Helper.computeStrategyValue( 2, nemesisPolicy, bestResponse );
		 immEpsilons[0] = gameValues[0] - nemesisValue1;
		 immEpsilons[1] = gameValues[1] - nemesisValue2;
		 
		 /**
		  * then compare the immediate epsilon safe value with 
		  * the accumulative epsilon safe value 
		  * and decide to use which strategy
		  */
		 if( ( maxMatchNum - T ) * immEpsilons[seat] <= accumEpsilons[seat] ) {
			
			 usingNash = false;
		 }
		 else {
			 
			 usingNash = true;
			 
			 //use the best nash equilibrium
			 findBestNashEquilibrium();
		 }
	}
}
