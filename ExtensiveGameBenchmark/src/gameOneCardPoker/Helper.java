package gameOneCardPoker;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class Helper {

	public static void computeAverageStrategy( double[][][] acc_policy, double[][][] average_policy )
	{
		for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
			for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
				
				double sum1 = 0.0;
				for( int action = 0; action < Action.NUM_ACTION_TYPES; action++ ) {
					
					sum1 += acc_policy[rank][hisID][action];
				}
				
				for( int action = 0; action < Action.NUM_ACTION_TYPES; action++ ) {
				
					if( sum1 < 0.00001 )
						average_policy[rank][hisID][action] = 0;
					else {
						
						average_policy[rank][hisID][action] = acc_policy[rank][hisID][action] / sum1;
					}
				}
				//check
				for( int action = 0; action < Action.NUM_ACTION_TYPES; action++ ) {
					
					if( average_policy[rank][hisID][action] < 0.0001 ) {
						
						average_policy[rank][hisID][action] = 0.0;
						average_policy[rank][hisID][1-action] = 1.0;
					}
				}
			}
		}
	}
	
	/**
	 * compute the exploitability value of current average strateublicgy
	 * maybe we should store the value
	 */
	public static double computeExploitability( double[][][] pl )
	{
		
		/**
		 * exploitability value for player1
		 * so the viewer is player2
		 * the opponent is player1
		 */
		double epsilon1 = 0;
		for( int rank2 = 0; rank2 < Card.NUM_RANKS; rank2++ ) {
			
			/**
			 * chance probability for player1's each rank
			 */
			double[] reachingPro = new double[Card.NUM_RANKS];
			for( int rank1 = 0; rank1 < Card.NUM_RANKS; rank1++ ) {
				
				reachingPro[rank1] = 1.0 / 13.0;  
			}
			
			GameState gameState = new GameState();
			int cardPlayer2 = rank2 * Card.NUM_SUITS;
			gameState.receiveCard( 1, cardPlayer2 );
			
			epsilon1 += walkTrees_BRValue( 2, 1, gameState, reachingPro, pl );
		}
		
		
		/**
		 * exploitability value for player2
		 * so the viewer is player1
		 * the opponent is player2
		 */
		double epsilon2 = 0;
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
				
				epsilon2 += walkTrees_BRValue( 1, 1, gameState, reachingPro, pl );
		}
		
		
		double epsilon = (epsilon1 + epsilon2) / 2.0;
		
		System.out.println( "Exploitability: "+epsilon+" "+epsilon1+" "+epsilon2 );
		
		return epsilon;
	}
	
	private static double walkTrees_BRValue( int viewer, int currentPlayer, GameState state, 
			double[] reachingPro, double[][][] pl )
	{
		
		int viewerSeat = 0;
		int oppSeat = 1;
		if( viewer == 2 ) {
			
			viewerSeat = 1;
			oppSeat = 0;
		}
		
		double retValue = 0.0;
		
		/**
		 * if the current node is terminal node
		 * then enumerate all possible cards for opponent
		 * return the expected utility for the viewer
		 */
		OneCardPoker.checkOver( state );
		if( state.isGameOver() ) {
			
			for( int oppRank = 0; oppRank < Card.NUM_RANKS; oppRank++ ) {
			
				state.receiveCard( oppSeat, oppRank * Card.NUM_SUITS );
				retValue += OneCardPoker.getUtility( state, viewerSeat ) * reachingPro[oppRank];
			}
		}
		
		else {
			
			int historyID = OneCardPoker.historyID( state );
			
			/**
			 * The viewer is acting at this node
			 */
			if( viewer == currentPlayer ) {
				
				double[] v_Ia = new double[Action.NUM_ACTION_TYPES];
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					GameState nextState = new GameState( state );
					OneCardPoker.doAction( nextState, a );
					
					int nextPlayer = 1;
					if( viewer == 1 )
						nextPlayer = 2;
					v_Ia[a] = walkTrees_BRValue( viewer, nextPlayer, nextState, reachingPro, pl );
					
					nextState = null;
				}
				retValue = v_Ia[0];
				for( int a = 1; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					if( v_Ia[a] > retValue )
						retValue = v_Ia[a];
				}
			}
			
			/**
			 * The opponent is acting at this node
			 */
			else {
				
				for( int oppAction = 0; oppAction < Action.NUM_ACTION_TYPES; oppAction++ ) {
					
					GameState nextState = new GameState( state );
					OneCardPoker.doAction( nextState, oppAction );
				
					double[] rp = new double[Card.NUM_RANKS];
					for( int oppRank = 0; oppRank < Card.NUM_RANKS; oppRank++ ) {
						
						rp[oppRank] = reachingPro[oppRank] * pl[oppRank][historyID][oppAction];
						
						//rp[oppRank] = reachingPro[oppRank] * policy[oppRank][historyID][oppAction];
					}
					
					int nextPlayer = viewer;
					double v_Ia = walkTrees_BRValue( viewer, nextPlayer, nextState, rp, pl );
					retValue += v_Ia;
					
					rp = null;
				}
			}

		}
		
		return retValue;
	}

	
	public static double computeStrategyValue( int viewer, double[][][] policy1, double[][][] policy2 )
	{
		 double[] p1 = new double[Card.NUM_RANKS];
		 double[] p2 = new double[Card.NUM_RANKS];
		 
		 for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
			 
			 p1[rank] = 1.0;
			 p2[rank] = 1.0;
		 }
		 
		 GameState gameState = new GameState();
		 
		 return Helper.walkTrees_ComputeStrategyValue( viewer, 1, gameState, p1, p2, policy1, policy2 );
	}
	
	public static double walkTrees_ComputeStrategyValue( int viewer, int currentPlayer, GameState gameState, 
			double[] p1, double[] p2, double[][][] policy1, double[][][] policy2 )
	{
		double retValue = 0.0;
		
		 /**
		  * in fact, we are walking in the public information set tree
		  * it should be noted that p1 and p2 do not include the chance probabilities
		  */
		 
		 int hisID = OneCardPoker.historyID( gameState );
		 
		 int viewerSeat = 0;
		 int oppSeat = 1;
		 if( viewer == 2 ) {
			 
			 viewerSeat = 1;
			 oppSeat = 0;
		 }
		 
		 OneCardPoker.checkOver( gameState );
		 if( gameState.isGameOver() ) {
				
			 /**
			  * Consider all possible terminal histories
			  * At a leaf node in the public information set tree
			  * two ranks determine a terminal history
			  */
			 for( int rank1 = 0; rank1 < Card.NUM_RANKS; rank1++ ) {
				 
				 gameState.receiveCard( 0, rank1 * Card.NUM_SUITS );
				 double chancePro1 = 1.0 / 13.0;
				 
				 for( int rank2 = 0; rank2 < Card.NUM_RANKS; rank2++ ) {
					 
					 gameState.receiveCard( 1, rank2 * Card.NUM_SUITS );
					 double chancePro2 = 4.0 / 51.0;
					 if( rank2 == rank1 )
						 chancePro2 = 3.0 / 51.0;
					 
					 retValue += OneCardPoker.getUtility( gameState, viewerSeat ) * 
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
						 
						 p1_p[rank1] = p1[rank1] * policy1[rank1][hisID][a]; 
					 }
					 
					 retValue += walkTrees_ComputeStrategyValue(viewer, nextPlayer, nextState, p1_p, p2, policy1, policy2);
						
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
						 
						 p2_p[rank2] = p2[rank2] * policy2[rank2][hisID][a]; 
					 }
					 retValue += walkTrees_ComputeStrategyValue(viewer, nextPlayer, nextState, p1, p2_p, policy1, policy2);
						
					 nextState = null;
					 p2_p = null;
				 }
			 }
		 }
		 
		 return retValue;
	}
	
	
	public static void computeNemesisStrategy( double[][][] srcPolicy, double[][][] tarPolicy, int viewer )
	{
		int viewerSeat = 0;
		int oppSeat = 1;
		if( viewer == 2 ) {
			
			viewerSeat = 1;
			oppSeat = 0;
		}
		
		for( int oppRank = 0; oppRank < Card.NUM_RANKS; oppRank++ ) {
			
			
			GameState gameState = new GameState();
			gameState.receiveCard( oppSeat, oppRank * Card.NUM_SUITS );
			
			double[] reachingPro = new double[Card.NUM_RANKS];
			for( int viewerRank = 0; viewerRank < Card.NUM_RANKS; viewerRank++ ) {
				
				if( viewer == 1 )
					reachingPro[viewerRank] = 1.0 / 13.0;
				if( viewer == 2 ) {
					
					reachingPro[viewerRank] = 4.0 / 51.0;
					if( viewerRank == oppRank )
						reachingPro[viewerRank] = 3.0 / 51.0;
				}
			}
			
			walkTrees_ComputeNemesisStrategy( viewer, 1, gameState, reachingPro, srcPolicy, tarPolicy );
		}
	}
	
	public static double walkTrees_ComputeNemesisStrategy( int viewer, int currentPlayer, GameState gameState, 
			double[] reachingPro, double[][][] srcPolicy, double[][][] tarPolicy )
	{
		int viewerSeat = 0;
		int oppSeat = 1;
		if( viewer == 2 ) {
			
			viewerSeat = 1;
			oppSeat = 0;
		}
		
		int oppRank = Card.rankOfCard(gameState.getPlayerCard(oppSeat));
		double retValue = 0.0;
		
		OneCardPoker.checkOver( gameState );
		if( gameState.isGameOver() ) {
			
			for( int viewerRank = 0; viewerRank < Card.NUM_RANKS; viewerRank++ ) {
			
				gameState.receiveCard( viewerSeat, viewerRank * Card.NUM_SUITS );
				
				if( viewer == 1 ) {
					
					if( oppRank == viewerRank )
						retValue += OneCardPoker.getUtility( gameState, viewerSeat ) * reachingPro[viewerRank] * 3.0 / 51.0;
					else
						retValue += OneCardPoker.getUtility( gameState, viewerSeat ) * reachingPro[viewerRank] * 4.0 / 51.0;
				}
				else {
					
					retValue += OneCardPoker.getUtility( gameState, viewerSeat ) * reachingPro[viewerRank] / 13.0;
				}
			}
		}
		else {
			
			int historyID = OneCardPoker.historyID( gameState );
			
			/**
			 * the viewer's decision node
			 * just sum up the value of all actions
			 */
			if( currentPlayer == viewer ) {
				
				for( int viewerAction = 0; viewerAction < Action.NUM_ACTION_TYPES; viewerAction++ ) {
					
					GameState nextState = new GameState( gameState );
					OneCardPoker.doAction( nextState, viewerAction );
					
					double[] rp = new double[Card.NUM_RANKS];
					for( int viewerRank = 0; viewerRank < Card.NUM_RANKS; viewerRank++ ) {
						
						rp[viewerRank] = reachingPro[viewerRank] * srcPolicy[viewerRank][historyID][viewerAction];
					}
					
					int nextPlayer = 1;
					if( viewer == 1 )
						nextPlayer = 2;
					retValue += walkTrees_ComputeNemesisStrategy( viewer, nextPlayer, nextState, 
							rp, srcPolicy, tarPolicy );
					
					nextState = null;
					rp = null;
				}

			}
			else {
				
				double[] v_Ia = new double[Action.NUM_ACTION_TYPES];
				for( int oppAction = 0; oppAction < Action.NUM_ACTION_TYPES; oppAction++ ) {
					
					GameState nextState = new GameState( gameState );
					OneCardPoker.doAction( nextState, oppAction );
					
					int nextPlayer = viewer;
					v_Ia[oppAction] = walkTrees_ComputeNemesisStrategy( viewer, nextPlayer, nextState, 
							reachingPro, srcPolicy, tarPolicy );					
				}
				retValue = v_Ia[0];
				int minAction = Action.ACTION_BET_ZERO;
				for( int a = 1; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					if( v_Ia[a] < retValue ) {
						retValue = v_Ia[a];
						minAction = a;
					}
				}
				tarPolicy[oppRank][historyID][minAction] = 1.0;
				tarPolicy[oppRank][historyID][1-minAction] = 0.0;
			}
		}
		
		return retValue;
	}
	
	public static void computeBestResponse( double[][][] srcPolicy, double[][][] tarPolicy )
	{
		
		/**
		 * compute the best response to the variable: srcPolicy
		 * and store it in the variable: tarPolicy
		 */
		
		 /**
		  * the opponent is player 1
		  * so the strategy is computed for player 2
		  */
		 for( int rank2 = 0; rank2 < Card.NUM_RANKS; rank2++ ) {
		 
			 /**
			  * chance probability for player1's each rank
			  */
			 double[] reachingPro = new double[Card.NUM_RANKS];
			 for( int rank1 = 0; rank1 < Card.NUM_RANKS; rank1++ ) {
					
				 reachingPro[rank1] = 1.0 / 13.0;  
			 }
				
			 GameState gameState = new GameState();
			 int cardPlayer2 = rank2 * Card.NUM_SUITS;
			 gameState.receiveCard( 1, cardPlayer2 );
			 walkTrees_ComputeBestResponse( 2, 1, gameState, reachingPro, srcPolicy, tarPolicy );
		 }
		 
		 /**
		  * the opponent is player 2
		  * so the strategy is computed for player 1
		  */
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
			 walkTrees_ComputeBestResponse( 1, 1, gameState, reachingPro, srcPolicy, tarPolicy );
		}
	}
	
	public static double walkTrees_ComputeBestResponse( int viewer, int currentPlayer, GameState state,
			double[] reachingPro, double[][][] srcPolicy, double[][][] tarPolicy )
	{
		 int viewerSeat = 0;
		 int oppSeat = 1;
		 if( viewer == 2 ) {
				
			 viewerSeat = 1;
			 oppSeat = 0;
		 }
		 int viewerRank = Card.rankOfCard(state.getPlayerCard(viewerSeat));
			
		 double retValue = 0.0;
			
		 /**
		  * if the current node is terminal node
		  * then enumerate all possible cards for opponent
		  * return the expected utility for the viewer
		  */
		 OneCardPoker.checkOver( state );
		 if( state.isGameOver() ) {
				
			 for( int oppRank = 0; oppRank < Card.NUM_RANKS; oppRank++ ) {
				
				 state.receiveCard( oppSeat, oppRank * Card.NUM_SUITS );
				 
				 if( viewer == 1 )
					 retValue += OneCardPoker.getUtility( state, viewerSeat ) * reachingPro[oppRank] / 13.0;
				 else {
					 
					 if( oppRank == viewerRank )
						 retValue += OneCardPoker.getUtility( state, viewerSeat ) * reachingPro[oppRank] * 3 / 51.0;
					 else
						 retValue += OneCardPoker.getUtility( state, viewerSeat ) * reachingPro[oppRank] * 4 / 51.0;
				 }
			 }
		 }
		 
		 else {
				
			 int historyID = OneCardPoker.historyID( state );
			 
			 /**
			  * The viewer is acting at this node
			  */
			 if( viewer == currentPlayer ) {
					
				 double[] v_Ia = new double[Action.NUM_ACTION_TYPES];
				 for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
						
					 GameState nextState = new GameState( state );
					 OneCardPoker.doAction( nextState, a );
						
					 int nextPlayer = 1;
					 if( viewer == 1 )
						 nextPlayer = 2;
					 v_Ia[a] = walkTrees_ComputeBestResponse( viewer, nextPlayer, nextState, reachingPro, 
							 srcPolicy, tarPolicy );
						
					 nextState = null;
				 }
				 
				 /**
				  * then we can find the best response at this node
				  */
				 int maxAction = Action.ACTION_BET_ZERO;
				 retValue = v_Ia[0];
				 for( int a = 1; a < Action.NUM_ACTION_TYPES; a++ ) {
						
					 if( v_Ia[a] > retValue ) {
						 
						 maxAction = a;
						 retValue = v_Ia[a];
					 }
				 }
				 tarPolicy[viewerRank][historyID][maxAction] = 1.0;
				 tarPolicy[viewerRank][historyID][1-maxAction] = 0.0;
			 }
				
			 /**
			  * The opponent is acting at this node
			  */
			 else {
					
				 for( int oppAction = 0; oppAction < Action.NUM_ACTION_TYPES; oppAction++ ) {
						
					 GameState nextState = new GameState( state );
					 OneCardPoker.doAction( nextState, oppAction );
					
					 double[] rp = new double[Card.NUM_RANKS];
					 for( int oppRank = 0; oppRank < Card.NUM_RANKS; oppRank++ ) {
							
						 rp[oppRank] = reachingPro[oppRank] * srcPolicy[oppRank][historyID][oppAction];
					 }
					 
					 int nextPlayer = viewer;
					 double v_Ia = walkTrees_ComputeBestResponse( viewer, nextPlayer, nextState, rp, 
							 srcPolicy, tarPolicy );
					 retValue += v_Ia;
					 
					 rp = null;
				 }
			 }

		 }
			
		 return retValue;
	}
	
	public static void displayPolicy( double[][][] pl )
	{
		/**
		 * display policy1 and policy2 for card 28
		 */
		for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
			for( int historyID = 0; historyID <= 4; historyID++ ) {
				
				//System.out.print( "History " + historyID + ": " );
				
				String string = "Bet0: ";
				string += pl[rank][historyID][0];
				string += "  Bet1: ";
				string += pl[rank][historyID][1];
				System.out.print( string );
				System.out.println();
			}
		}
		
		//System.out.println(average_policy[1][4][0]+" "+average_policy[1][4][1]);
		
	}

	
	public static void storePolicy( double[][][] pl )
	{
		try {
			
			BufferedWriter bufWriter = new BufferedWriter(new FileWriter("./policy.txt"));
			
			for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
				
				for( int his = 0; his < OneCardPoker.NUM_HISTORY_IDS; his++ ) {
					
					for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
						
						if( his == 3 || his == 5 || his == 6 || his == 7 || his == 8 )
							bufWriter.write(String.valueOf("-1.0\n"));
						else
							bufWriter.write(String.valueOf( pl[rank][his][a] )+"\n");
							//bufWriter.write(String.valueOf(0.5));
						bufWriter.newLine();
					}                                                
				}
			}
			
			bufWriter.close();
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
	}
	
	public static void storePolicy( double[][][] pl, String fileName )
	{
		try {
			
			BufferedWriter bufWriter = new BufferedWriter(new FileWriter( fileName ));
			
			for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
				
				for( int his = 0; his < OneCardPoker.NUM_HISTORY_IDS; his++ ) {
					
					for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
						
						if( his == 3 || his == 5 || his == 6 || his == 7 || his == 8 )
							bufWriter.write(String.valueOf("-1.0\n"));
						else
							bufWriter.write(String.valueOf( pl[rank][his][a] )+"\n");
							//bufWriter.write(String.valueOf(0.5));
						bufWriter.newLine();
					}                                                
				}
			}
			
			bufWriter.close();
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
	}
}
