package gameOneCardPoker.algorithms;

import gameOneCardPoker.Action;
import gameOneCardPoker.Card;
import gameOneCardPoker.GameState;
import gameOneCardPoker.OneCardPoker;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

public class SophisStatic extends Algorithm {

	private double[][][] Nash_Policy;
	
	private double randomPro = 0.5;
	
	public SophisStatic()
	{
		super();
		
		Nash_Policy = new double[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		
		//read policy from file
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
			
			int retAction = 0;
			
			double[] pro = new double[Action.NUM_ACTION_TYPES];
			if( new Random().nextDouble() < 0.8 ) {
				
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
				
				pro[0] = Nash_Policy[playerRank][hisID][0];
				for( int a = 1; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					pro[a] = pro[a-1] + Nash_Policy[playerRank][hisID][a];
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
	
	public void readNash()
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
				
				Nash_Policy[rank][his][a] = Double.parseDouble(line);
				
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
