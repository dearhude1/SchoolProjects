package gameOneCardPoker.algorithms;

import gameOneCardPoker.Action;
import gameOneCardPoker.Card;
import gameOneCardPoker.GameState;
import gameOneCardPoker.Helper;
import gameOneCardPoker.OneCardPoker;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

public class BestNash extends DBBR {

	/**
	 * we should have several Nash equilibria
	 * we precompute 10 nash equilibria and store them into files
	 */
	private double[][][][] Nash_policies;
	
	/**
	 * the index of the best Nash equilibrium
	 * range from 0 to 9
	 */
	private int bestNash_Index = 0;
	
	public BestNash()
	{
		super();
		
		Nash_policies = new double[10][Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		for( int i = 0; i < 10; i++ ) {
			
			for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
				
				for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
					
					for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
						
						Nash_policies[i][rank][hisID][a] = 0.0;
					}
				}
			}
		}
		
		readNashEquilibria();
	}
	
	public BestNash( int gameNum )
	{
		super(gameNum);
		
		Nash_policies = new double[10][Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		for( int i = 0; i < 10; i++ ) {
			
			for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
				
				for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
					
					for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
						
						Nash_policies[i][rank][hisID][a] = 0.0;
					}
				}
			}
		}
		
		readNashEquilibria();
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
			 pro[0] = Nash_policies[bestNash_Index][playerRank][hisID][0];
			 for( int a = 1; a < Action.NUM_ACTION_TYPES; a++ ) {
					
				 pro[a] = pro[a-1] + Nash_policies[bestNash_Index][playerRank][hisID][a];
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
			 
			 findBestNashEquilibrium();
		 }
	 }
	
	private void findBestNashEquilibrium()
	{
		
		/**
		 * find the best equilibrium of the current player
		 * namely, according to the seat
		 */
		if( seat == 0 ) {
			
			GameState gameState = new GameState();
			double[] p1 = new double[Card.NUM_RANKS];
			double[] p2 = new double[Card.NUM_RANKS];
			 
			for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
				 
				p1[rank] = 1.0;
				p2[rank] = 1.0;
			}
			
			bestNash_Index = 0;
			double bestValue = Helper.walkTrees_ComputeStrategyValue((seat+1), 1, gameState, p1, p2, 
					Nash_policies[bestNash_Index], opponentStrategy );
			for( int index = 1; index < 10; index++ ) {
				
				double value = Helper.walkTrees_ComputeStrategyValue((seat+1), 1, gameState, p1, p2, 
						Nash_policies[index], opponentStrategy );
				
				if( value > bestValue ) {
					
					bestValue = value;
					bestNash_Index = index;
				}
			}
			
			p1 = null;
			p2 = null;
			gameState = null;
		}
		else {
			
			GameState gameState = new GameState();
			double[] p1 = new double[Card.NUM_RANKS];
			double[] p2 = new double[Card.NUM_RANKS];
			 
			for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
				 
				p1[rank] = 1.0;
				p2[rank] = 1.0;
			}
			
			bestNash_Index = 0;
			double bestValue = Helper.walkTrees_ComputeStrategyValue((seat+1), 1, gameState, p1, p2, 
					opponentStrategy, Nash_policies[bestNash_Index] );
			for( int index = 1; index < 10; index++ ) {
				
				double value = Helper.walkTrees_ComputeStrategyValue((seat+1), 1, gameState, p1, p2, 
						opponentStrategy, Nash_policies[index] );
				
				if( value > bestValue ) {
					
					bestValue = value;
					bestNash_Index = index;
				}
			}
			
			p1 = null;
			p2 = null;
			gameState = null;
		}
	}
	
	
	private void readNashEquilibria()
	{
		for( int i = 0; i <= 9; i++ ) {
			
			 try {
				 
				 String fileName = "./nash equilibria/Nash"+String.valueOf(i+1)+".txt";
				 BufferedReader bufReader = new BufferedReader(new FileReader(fileName));
						
				 String line = "";
				 int rank = 0;
				 int his = 0;
				 int a = 0;
				 while( (line = bufReader.readLine()) != null ) {
						
					 if( line.length() == 0 )
						 continue;
						
					 Nash_policies[i][rank][his][a] = Double.parseDouble(line);
					 
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
}
