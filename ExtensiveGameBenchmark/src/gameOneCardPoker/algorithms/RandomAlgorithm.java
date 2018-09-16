package gameOneCardPoker.algorithms;

import gameOneCardPoker.Action;
import gameOneCardPoker.GameState;
import gameOneCardPoker.OneCardPoker;

import java.util.Random;

public class RandomAlgorithm extends Algorithm {

	private double randomPro = 0.5;
	
	public int onlinePlay_ChooseAction( GameState gameState, long T )
	{
		
		OneCardPoker.checkOver( gameState );
		if( gameState.isGameOver() ) {
			
			return Action.ACTION_INVALID;
		}
		else {
			
			int retAction = 0;
			
			double[] pro = new double[Action.NUM_ACTION_TYPES];
			
			/**
			pro[0] = 1.0 / Action.NUM_ACTION_TYPES;
			for( int a = 1; a < Action.NUM_ACTION_TYPES; a++ ) {
				
				pro[a] = pro[a-1] + 1.0 / Action.NUM_ACTION_TYPES;
			}
			*/
			
			pro[0] = randomPro;
			pro[1] = 1.0;
			
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
