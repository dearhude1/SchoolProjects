package gameOneCardPoker.algorithms;

import gameOneCardPoker.Action;
import gameOneCardPoker.Card;
import gameOneCardPoker.GameState;
import gameOneCardPoker.Helper;
import gameOneCardPoker.InformationSet;
import gameOneCardPoker.OneCardPoker;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class CFR extends CFR_Series {
	
	
	//for recording all results obtained from the beginning of the game
	double allResults = 0.0;
	
	public CFR()
	{
		super();
	}
	
	/**
	 * Self play of Counterfactual Regret Minimization (CFR)
	 */
	public void selfPlay()
	{
		
		long startTime = System.nanoTime();
		long lastTime = startTime;
		long currentTime = startTime;
		ArrayList<Double> epsilonList = new ArrayList<Double>();
		
		int T = 1;
		while( (currentTime - startTime) < 10000000000L ) { //
			//for( int T = 1; T <= 10000000; T++ ) {
			 
			/**
			 * initialize game state
			 */
			GameState gameState = new GameState();
			
			for( int r = 0; r < Card.NUM_RANKS; r++ ) {
				
				gameState.receiveCard( 0, r * Card.NUM_SUITS );
				gameState.receiveCard( 1, r * Card.NUM_SUITS );
				
				
				double[] pp = new double[Card.NUM_RANKS];
				for( int r2 = 0; r2 < Card.NUM_RANKS; r2++ ) {
					
					pp[r2] = 1.0 / 13.0; 
					if( r2 == r )
						pp[r2] *= 3.0 / 51.0;
					else
						pp[r2] *= 4.0 / 51.0; 
				}
				walkTrees_CFR( 1, 1, gameState, 1, pp, T);
				walkTrees_CFR( 2, 1, gameState, 1, pp, T);
			}
			
			
			Helper.computeAverageStrategy( acc_policy, average_policy );
			T++;
			
			currentTime = System.nanoTime();
			if( currentTime - lastTime > 10000000L ) {
				
				long count = (currentTime - lastTime) / 10000000L;
				double ep = Helper.computeExploitability( average_policy );
				for( int i = 0; i < count; i++ )
					epsilonList.add(ep);
				
				lastTime = currentTime;
			}
		}
		
		/**
		 * write to file
		 */
		try {
			
			BufferedWriter bufWriter = new BufferedWriter(new FileWriter("./epsilons.csv"));
			System.out.println(epsilonList.size());
			while( epsilonList.size() > 0 ) {
				
				bufWriter.write( epsilonList.remove(0)+", " );
			}
			
			bufWriter.close();
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
		
		Helper.computeAverageStrategy( acc_policy, average_policy );
		Helper.displayPolicy( average_policy );
		Helper.storePolicy( average_policy );
	}
	
	private double walkTrees_CFR( int viewer, int currentPlayer, 
			GameState state, double p_i, double[] pp_i, int T )
	{
		double v_I = 0.0;
		
		int viewerSeat = 0;
		int oppSeat = 1;
		if( viewer == 2 ) {
			viewerSeat = 1;
			oppSeat = 0;
		}
		
		int hisID = OneCardPoker.historyID( state );
		InformationSet infoSet_Viewer = new InformationSet( Card.rankOfCard(state.getPlayerCard(viewerSeat)), hisID );
		
		OneCardPoker.checkOver( state );
		if( state.isGameOver() ) {
			
			
			//utility for player 0
			for( int r = 0; r < Card.NUM_RANKS; r++ ) {
				
				GameState virState = new GameState(state);
				virState.receiveCard( oppSeat, r * Card.NUM_SUITS);
				
				v_I += OneCardPoker.getUtility( virState, viewerSeat ) * pp_i[r];
				virState = null;
			}
		}
		else {
			
			if( currentPlayer == viewer ) {
				
				/**
				 * regret matching
				 */
				double sumPositiveRegret = 0.0;
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					double posRegret = regret[infoSet_Viewer.getCardRank()][hisID][a];
					if( posRegret < 0.00001 )
						posRegret = 0;
					//sumPositiveRegret += posRegret;
					
					//using square
					sumPositiveRegret += posRegret * posRegret;
					
					//using cube
					//sumPositiveRegret += posRegret * posRegret * posRegret;
				}
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					double posRegret = regret[infoSet_Viewer.getCardRank()][hisID][a];
					if( posRegret < 0.00001 )
						posRegret = 0;
					if( sumPositiveRegret < 0.00001 )
						policy[infoSet_Viewer.getCardRank()][hisID][a] = 1.0 / Action.NUM_ACTION_TYPES;
					else
						//policy[infoSet_Viewer.getCardRank()][hisID][a] = posRegret / sumPositiveRegret;
					
						//using square
						policy[infoSet_Viewer.getCardRank()][hisID][a] = posRegret*posRegret / sumPositiveRegret;
						
						//using cube
						//policy[infoSet_Viewer.getCardRank()][hisID][a] = posRegret*posRegret*posRegret / sumPositiveRegret;
				}
				
				/**
				 * try each action
				 */
				double[] v_Ia = new double[Action.NUM_ACTION_TYPES];
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					GameState nextState = new GameState( state );
					OneCardPoker.doAction( nextState, a );
					
					//recursive
					int nextPlayer = 2;
					if( viewer == 2 )
						nextPlayer = 1;
					
					double p_i_next = p_i * policy[infoSet_Viewer.getCardRank()][hisID][a];
					v_Ia[a] = walkTrees_CFR( viewer, nextPlayer, nextState, p_i_next, pp_i, T);
					v_I += v_Ia[a] * policy[infoSet_Viewer.getCardRank()][hisID][a];
					
					nextState = null;
				}
				
				/**
				 * update regret and strategy
				 */
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					double R = regret[infoSet_Viewer.getCardRank()][hisID][a];
					R = R + ( v_Ia[a] - v_I );
					
					regret[infoSet_Viewer.getCardRank()][hisID][a] = R;
					
					acc_policy[infoSet_Viewer.getCardRank()][hisID][a] += 
						p_i * policy[infoSet_Viewer.getCardRank()][hisID][a];
				}
			}
			else {
				
				
				/**
				 * try each action
				 */
				double[] v_Ia = new double[Action.NUM_ACTION_TYPES];
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					GameState nextState = new GameState( state );
					OneCardPoker.doAction( nextState, a );
					
					//recursive
					int nextPlayer = viewer;
					
					double[] pp_i_next = new double[Card.NUM_RANKS];
					for( int r = 0; r < Card.NUM_RANKS; r++ ) {
						
						pp_i_next[r] = pp_i[r] * policy[r][hisID][a];
					}
					
					v_Ia[a] = walkTrees_CFR( viewer, nextPlayer, nextState, p_i, pp_i_next, T);
					v_I += v_Ia[a];
					
					nextState = null;
				}
			}
		}
		
		return v_I;
	}
}
