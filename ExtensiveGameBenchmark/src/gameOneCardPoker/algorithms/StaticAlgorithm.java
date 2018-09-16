package gameOneCardPoker.algorithms;

import gameOneCardPoker.Action;
import gameOneCardPoker.Card;
import gameOneCardPoker.GameState;
import gameOneCardPoker.OneCardPoker;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

public class StaticAlgorithm extends Algorithm {

	
	private double[][][] myPolicy;
	
	public StaticAlgorithm()
	{
		super();
		
		myPolicy = new double[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		
		//read policy from file
		readPolicy();
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
			pro[0] = myPolicy[playerRank][hisID][0];
			for( int a = 1; a < Action.NUM_ACTION_TYPES; a++ ) {
				
				pro[a] = pro[a-1] + myPolicy[playerRank][hisID][a];
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
	
	public void readPolicy()
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
				
				myPolicy[rank][his][a] = Double.parseDouble(line);
				
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
	
	public void readPolicy( String fileName )
	{
		
		try {
			
			BufferedReader bufReader = new BufferedReader(new FileReader(fileName));	
			
			String line = "";
			int rank = 0;
			int his = 0;
			int a = 0;
			while( (line = bufReader.readLine()) != null ) {
				
				if( line.length() == 0 )
					continue;
				
				myPolicy[rank][his][a] = Double.parseDouble(line);
				
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
