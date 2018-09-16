package gameOneCardPoker.algorithms;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

import gameOneCardPoker.Action;
import gameOneCardPoker.Card;
import gameOneCardPoker.GameState;
import gameOneCardPoker.Helper;
import gameOneCardPoker.OneCardPoker;

public class BEFEWP extends DBBR {

	/**
	 * we should have several Nash equilibria
	 * we precompute 10 nash equilibria and store them into files
	 */
	protected double[][][][] Nash_policies;
	
	/**
	 * best response to the variable: bestResponse 
	 * with a little modification
	 */
	protected double[][][] taoPolicy;
	
	/**
	 * for computing nemesis value
	 */
	protected double[][][] nemesisPolicy;
	
	/**
	 * the index of the best Nash equilibrium
	 * range from 0 to 9
	 */
	protected int bestNash_Index = 0;
	
	/**
	 * the flag which determines to use a Nash equilibrium 
	 * or a full best response
	 */
	protected boolean usingNash = true;
	
	/**
	 * accumulative epsilon safe values for each player
	 * updated on each iteration
	 */
	protected double[] accumEpsilons;
	
	/**
	 * immediate epsilon safe value of the current iteration
	 * for each player
	 */
	protected double[] immEpsilons;
	
	/**
	 * record the opponent's action in the current episode
	 */
	protected int[] opponentActions;
	
	public BEFEWP()
	{
		super();
		
		/**
		 * read nash equilibria
		 */
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
		
		taoPolicy = new double[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		nemesisPolicy = new double[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
			
			for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
				
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					taoPolicy[rank][hisID][a] = 0.0;
					nemesisPolicy[rank][hisID][a] = 0.0;
				}
			}
		}
		
		opponentActions = new int[OneCardPoker.NUM_HISTORY_IDS];
		for( int h = 0; h < OneCardPoker.NUM_HISTORY_IDS; h++ )
			opponentActions[h] = Action.ACTION_INVALID; 
		
		readNashEquilibria();
		
		accumEpsilons = new double[2];
		immEpsilons = new double[2];
		accumEpsilons[0] = accumEpsilons[1] = 0.0;
		immEpsilons[0] = immEpsilons[1] = 0.0;
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
			 
			 if( usingNash ) {
				 
				 pro[0] = Nash_policies[bestNash_Index][playerRank][hisID][0];
				 for( int a = 1; a < Action.NUM_ACTION_TYPES; a++ ) {
						
					 pro[a] = pro[a-1] + Nash_policies[bestNash_Index][playerRank][hisID][a];
				 }
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
		 if( immEpsilons[seat] <= accumEpsilons[seat] ) {
			
			 //System.out.println("seat: "+seat+" immE: "+immEpsilons[seat]+" accE: "+accumEpsilons[seat]+" Safe!");
			 usingNash = false;
		 }
		 else {
			 
			 //System.out.println("seat: "+seat+" immE: "+immEpsilons[seat]+" accE: "+accumEpsilons[seat]+" Not Safe!");
			 usingNash = true;
			 
			 //use the best nash equilibrium
			 findBestNashEquilibrium();
		 }
	}
	 
	 public void onlinePlay_GameOver( GameState gameState, long T )
	 {
		 
		 if((T == T_equil) || 
			(T > T_equil && (T % k_updateModel == 0)) ) {
			 
				/**
				 * update the opponent model on each iteration!!
				 * and modify our strategy
				 */
				 //constructOpponentModel();
		 }
		 
		/**
		 * update the opponent model on each iteration!!
		 * and modify our strategy
		 */
		 constructOpponentModel();
		 
		 /**
		  * compute the best response to the modeled opponent strategy
		  * this strategy is stored in variable: bestReponse
		  */
		 Helper.computeBestResponse( opponentStrategy, bestResponse );
		 
		 
		 /**
		  * update the accumulative epsilon safe value
		  * first, compute the best response to the best response
		  * then modify it with the opponent's action in the latest game
		  */
		 Helper.computeBestResponse( bestResponse, taoPolicy );
		 
		 int hisID = OneCardPoker.historyID( gameState );
		 if( hisID == 3 || hisID == 6 || hisID == 8 ) {
			 
			 int oppRank = Card.rankOfCard( gameState.getPlayerCard(1-seat) );
			 
			 for( int h = 0; h < OneCardPoker.NUM_HISTORY_IDS; h++ ) {
				 
				 int oppAction = opponentActions[h];
				 
				 if( oppAction != Action.ACTION_INVALID ) {
					 
					 taoPolicy[oppRank][hisID][oppAction] = 1.0;
					 taoPolicy[oppRank][hisID][1-oppAction] = 0.0;
				 }
				 
				 //we should clean opponentActions for next use
				 opponentActions[h] = Action.ACTION_INVALID;
			 }
		 }
		 
		 GameState gs = new GameState();
		 double[] p1 = new double[Card.NUM_RANKS];
		 double[] p2 = new double[Card.NUM_RANKS];
		 
		 for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
				 
			 p1[rank] = 1.0;
			 p2[rank] = 1.0;
		 }
		 double u1 = Helper.walkTrees_ComputeStrategyValue( 1, 1, gs, p1, p2, bestResponse, taoPolicy );
		 double u2 = Helper.walkTrees_ComputeStrategyValue( 2, 1, gs, p1, p2, taoPolicy, bestResponse );
		 
		 accumEpsilons[0] += u1 - gameValues[0];
		 accumEpsilons[1] += u2 - gameValues[1];
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
			 
			 opponentActions[hisID] = action;
		 }
	 }
	
	
	 protected void findBestNashEquilibrium()
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
