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
import java.util.Random;

public class MCCFR extends CFR_Series {

	
	
	/**
	 * for each information set
	 * record the last time it was visited
	 */
	private long[][] visitMarkers;
	
	
	
	//for recording all results obtained from the beginning of the game
	double allResults = 0.0;
	
	
	public MCCFR()
	{
		super();
		
		visitMarkers = new long[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS];
		
		for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
			
			for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
				
				visitMarkers[rank][hisID] = 0; 
			}
		}
	}
	
	/**
	 * Self play of Monte Carlo Counterfactual Regret Minimization (MCCFR)
	 */
	public void selfPlay()
	{
		
		long startTime = System.nanoTime();
		long lastTime = startTime;
		long currentTime = startTime;
		ArrayList<Double> epsilonList = new ArrayList<Double>();
		
		int T = 1;
		while( (currentTime - startTime) < 10000000000L ) {
			 
			/**
			 * initialize game state
			 */
			GameState gameState = new GameState();
			
			/**
			 * sample chance node
			 * deal cards for each player
			 */
			Random random = new Random();
			ArrayList<Integer> deck = new ArrayList<Integer>();
			for( int card = Card.FIRST_CARD_INDEX; card <= Card.LAST_CARD_INDEX; card++ )
				deck.add( card );
			int card1 = deck.remove( random.nextInt( deck.size() ) );
			int card2 = deck.remove( random.nextInt( deck.size() ) );
			
			gameState.receiveCard( 0, card1 );
			gameState.receiveCard( 1, card2 );
			
			double chanceProSeat0 = 1.0/13.0;
			double chanceProSeat1 = 0.0;
			if( Card.rankOfCard(card2) == Card.rankOfCard(card1) )
				chanceProSeat1 = 3.0/51.0;
			else
				chanceProSeat1 = 4.0/51.0;
			
			double q_z = chanceProSeat0 * chanceProSeat1;
			
			/**
			 * then begin to walk tree
			 */
			
			//walkTrees_ChanceSampled_CFR( 1, 1, gameState, 1, 1, T, q_z );
			//walkTrees_ChanceSampled_CFR( 2, 2, gameState, 1, 1, T, q_z );
			
			//walkTrees_ExternalSampling_CFR( 1, 1, gameState, 1, T );
			//walkTrees_ExternalSampling_CFR( 2, 1, gameState, 1, T );
			
			walkTrees_OutcomeSampling_CFR(1, gameState, 1, 1, 1, T );
			
			
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
	}
	
	/**
	 * chance-sampled CFR
	 * @param rootPlayer
	 * @param currentPlayer
	 * @param state
	 * @param p1
	 * @param p2
	 * @param T
	 * @param q_z: chance probability
	 * @return
	 */
	private double[] walkTrees_ChanceSampled_CFR( int rootPlayer, int currentPlayer, GameState state, 
			double p1, double p2, int T, double q_z )
	{
		//p1 for player1 , p2 for player2
		
		/**
		 * p1: player1's reaching probability
		 * p2: player2's reaching probability
		 * not including chance probability
		 */
		/**
		 * return values
		 * stands for the utility value of current node when using current policy
		 */
		double[] values = new double[2];
		double v1_I = 0.0;
		double v2_I = 0.0;
		
		/**
		 * if current node is a terminal node
		 * currentPlayer is no use in terminal node
		 * 
		 * note that the if a terminal node is reached by taking action a in I
		 * the value of v(sigma,I,a) is not u(z), because v(sigma,I,a) stands for
		 * the value under policy sigma. The reaching probability of the opponent should
		 * be considered. And since we are sampling, we should consider the probability of q(z)
		 */
		/**
		 * the reaching probability of the other players includes the distribution of chance player!
		 * so
		 * v(sigma,z) = u(z)*pi(sigma_-i,z)*q_z/q_z
		 */
		OneCardPoker.checkOver( state );
		if( state.isGameOver() ) {
			
			double utilitySeat0 = OneCardPoker.getUtility( state, 0 );
			double utilitySeat1 = OneCardPoker.getUtility( state, 1 );
			if( rootPlayer == 1 ) {
				
				v1_I = utilitySeat0 * p2;
				v2_I = utilitySeat1 * p1;
				
			}
			else {
				
				v1_I = utilitySeat1 * p2;
				v2_I = utilitySeat0 * p1;
			}
		}
		
		/**
		 * if current node is a decision node
		 */
		else {
			
			int seatPlayer1 = 0;
			int seatPlayer2 = 1;
			if( rootPlayer == 2 ) {
				
				seatPlayer1 = 1;
				seatPlayer2 = 0;
			}
			
			int historyID = OneCardPoker.historyID( state );
			InformationSet infoSet_Player1 = new InformationSet( Card.rankOfCard(state.getPlayerCard(seatPlayer1)), 
					historyID );
			InformationSet infoSet_Player2 = new InformationSet( Card.rankOfCard(state.getPlayerCard(seatPlayer2)),
					historyID );
			
			/**
			 * for acting player
			 */
			if( currentPlayer == 1 ) {
				
				/**
				 * update current policy by using regret matching
				 */
				double sumPositiveRegret = 0.0;
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					double posRegret = regret[infoSet_Player1.getCardRank()][infoSet_Player1.getHistoryID()][a];
					if( posRegret < 0.00001 )
						posRegret = 0;
					sumPositiveRegret += posRegret;
				}
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					double posRegret = regret[infoSet_Player1.getCardRank()][infoSet_Player1.getHistoryID()][a];
					if( posRegret < 0.00001 )
						posRegret = 0;
					if( sumPositiveRegret < 0.00001 )
						policy[infoSet_Player1.getCardRank()][infoSet_Player1.getHistoryID()][a] = 1.0 / Action.NUM_ACTION_TYPES;
					else
						policy[infoSet_Player1.getCardRank()][infoSet_Player1.getHistoryID()][a] = posRegret / sumPositiveRegret;
				}
				
				/**
				 * try each action in current node
				 */
				double[] v1_Ia = new double[Action.NUM_ACTION_TYPES];
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					GameState nextState = new GameState( state );
					OneCardPoker.doAction( nextState, a );
					
					//recursive
					int nextPlayer = 2;
					double p1_next = p1 * policy[infoSet_Player1.getCardRank()][infoSet_Player1.getHistoryID()][a];
					double[] v12_Ia = walkTrees_ChanceSampled_CFR( rootPlayer, nextPlayer, nextState, 
							p1_next, p2, T, q_z );
					
					//v1(I) = sum{u1(I,a) * p1(I,a)}
					//v2(I) = sum{u2(I,a)}, because the current player is player1
					v1_Ia[a] = v12_Ia[0]; 
					v1_I += v12_Ia[0] * policy[infoSet_Player1.getCardRank()][infoSet_Player1.getHistoryID()][a];
					v2_I += v12_Ia[1];
					
					nextState = null;
				}
				
				/**
				 * update regret for current player
				 */
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					double reg = regret[infoSet_Player1.getCardRank()][infoSet_Player1.getHistoryID()][a];
					//double R = ( T - 1 ) * regret + ( v1_Ia[a] - v1_I );
					double R = reg + ( v1_Ia[a] - v1_I );
					//R = R / T;
					regret[infoSet_Player1.getCardRank()][infoSet_Player1.getHistoryID()][a] = R;
					
					acc_policy[infoSet_Player1.getCardRank()][infoSet_Player1.getHistoryID()][a] += 
						p1 * policy[infoSet_Player1.getCardRank()][infoSet_Player1.getHistoryID()][a];
					
				}
			}
			/**
			 * acting player is player2
			 */
			else {
				
				/**
				 * regret matching
				 */
				double sumPositiveRegret = 0.0;
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					double posRegret = regret[infoSet_Player2.getCardRank()][infoSet_Player2.getHistoryID()][a];
					if( posRegret < 0.00001 )
						posRegret = 0;
					sumPositiveRegret += posRegret;
				}
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					double posRegret = regret[infoSet_Player2.getCardRank()][infoSet_Player2.getHistoryID()][a];
					if( posRegret < 0.00001 )
						posRegret = 0;
					if( sumPositiveRegret < 0.00001 )
						policy[infoSet_Player2.getCardRank()][infoSet_Player2.getHistoryID()][a] = 1.0 / Action.NUM_ACTION_TYPES;
					else
						policy[infoSet_Player2.getCardRank()][infoSet_Player2.getHistoryID()][a] = posRegret / sumPositiveRegret;
						
				}
				
				/**
				 * try each action in current node
				 */
				double[] v2_Ia = new double[Action.NUM_ACTION_TYPES];
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					GameState nextState = new GameState( state );
					OneCardPoker.doAction( nextState, a );
					
					//recursive
					int nextPlayer = 1;
					double p2_next = p2 * policy[infoSet_Player2.getCardRank()][infoSet_Player2.getHistoryID()][a];
					double[] v12_Ia = walkTrees_ChanceSampled_CFR( rootPlayer, nextPlayer, nextState, 
							p1, p2_next, T, q_z );
					
					//u1(I) = sum{u1(I,a)}, because the current player is player2
					//u2(I) = sum{u2(I,a) * p2(I,a)}
					v2_Ia[a] = v12_Ia[1]; 
					v1_I += v12_Ia[0];
					v2_I += v12_Ia[1] * policy[infoSet_Player2.getCardRank()][infoSet_Player2.getHistoryID()][a];
					
					nextState = null;
				}
				
				/**
				 * update regret for current player
				 */
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					double reg = regret[infoSet_Player2.getCardRank()][infoSet_Player2.getHistoryID()][a];
					//double R = ( T - 1 ) * regret + ( v2_Ia[a] - v2_I );
					double R = reg + ( v2_Ia[a] - v2_I );
					//R = R / T;
					regret[infoSet_Player2.getCardRank()][infoSet_Player2.getHistoryID()][a] = R;
					
					acc_policy[infoSet_Player2.getCardRank()][infoSet_Player2.getHistoryID()][a] += 
						p2 * policy[infoSet_Player2.getCardRank()][infoSet_Player2.getHistoryID()][a];
				}
			}
		}
		
		values[0] = v1_I;
		values[1] = v2_I;
		
		return values;
	}

	
	/**
	 * MCCFR with outcome sampling
	 * @param currentPlayer
	 * @param state
	 * @param p1
	 * @param p2
	 * @param samprofile
	 * @param T
	 * @return
	 */
	private double[] walkTrees_OutcomeSampling_CFR( int currentPlayer, GameState state, double p1, 
			double p2, double samprofile, int T )
	{
		double[] retValues = new double[2];
		double v1_I = 0.0;
		double v2_I = 0.0;
		
		/**
		 * In outcome sampling
		 * It seems hard to update all players' strategies and regret values in parallel
		 * so we just walk tree one by one
		 */
		OneCardPoker.checkOver( state );
		if( state.isGameOver() ) {
			
			double utilitySeat0 = OneCardPoker.getUtility( state, 0 );
			double utilitySeat1 = OneCardPoker.getUtility( state, 1 );
			
			v1_I = utilitySeat0 * p2 / samprofile;
			v2_I = utilitySeat1 * p1 / samprofile;
			
			
			//System.out.println( "p1: "+p1+" p2: "+p2 );
			
		}
		
		else {
			
			int historyID = OneCardPoker.historyID( state );
			InformationSet infoSet_Player1 = new InformationSet( Card.rankOfCard(state.getPlayerCard(0)), 
					historyID );
			
			InformationSet infoSet_Player2 = new InformationSet( Card.rankOfCard(state.getPlayerCard(1)),
					historyID );
			
			
			double epsilon = 0.02;
			
			/**
			 * acting player is player1
			 */
			if( currentPlayer == 1 ) {
				
				/**
				 * regret matching for the acting player
				 */
				double sumPositiveRegret = 0.0;
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					double posRegret = regret[infoSet_Player1.getCardRank()][infoSet_Player1.getHistoryID()][a];
					if( posRegret < 0.00001 )
						posRegret = 0;
					sumPositiveRegret += posRegret;
				}
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					double posRegret = regret[infoSet_Player1.getCardRank()][infoSet_Player1.getHistoryID()][a];
					if( posRegret < 0.00001 )
						posRegret = 0;
					if( sumPositiveRegret < 0.00001 )
						policy[infoSet_Player1.getCardRank()][infoSet_Player1.getHistoryID()][a] = 1.0 / Action.NUM_ACTION_TYPES;
					else
						policy[infoSet_Player1.getCardRank()][infoSet_Player1.getHistoryID()][a] = posRegret / sumPositiveRegret;
				}
				
				
				/**
				 * sample action using epsilon-greedy
				 */
				int samplingAction = 0;
				
				double greedyPro = new Random().nextDouble();
				double playerPro = new Random().nextDouble();
				double[] pro = new double[Action.NUM_ACTION_TYPES];
				if( greedyPro < epsilon ) {
					
					pro[0] = 1.0 / Action.NUM_ACTION_TYPES;
					for( int a = 1; a < Action.NUM_ACTION_TYPES; a++ ) {
						
						pro[a] = pro[a-1] + 1.0 / Action.NUM_ACTION_TYPES;
					}
				}
				else {
					
					pro[0] = policy[infoSet_Player1.getCardRank()][infoSet_Player1.getHistoryID()][0];
					for( int a = 1; a < Action.NUM_ACTION_TYPES; a++ ) {
						
						pro[a] = pro[a-1] + policy[infoSet_Player1.getCardRank()][infoSet_Player1.getHistoryID()][a];
					}
				}
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					if( playerPro < pro[a] ) {
						
						samplingAction = a;
						break;
					}
				}
				double sp = epsilon * 1.0/Action.NUM_ACTION_TYPES + 
						(1-epsilon) * policy[infoSet_Player1.getCardRank()][infoSet_Player1.getHistoryID()][samplingAction];
				
				
				
				double[] v1_Ia = new double[Action.NUM_ACTION_TYPES];
				GameState nextState = new GameState( state );
				OneCardPoker.doAction( nextState, samplingAction );
				int nextPlayer = 2;
				
				double p1_next = p1 * policy[infoSet_Player1.getCardRank()][infoSet_Player1.getHistoryID()][samplingAction];
				double[] v12_Ia = walkTrees_OutcomeSampling_CFR( nextPlayer, nextState, p1_next, p2, samprofile * sp, T );
				nextState = null;
				
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					if( a != samplingAction )
						v1_Ia[a] = 0.0;
					else
						v1_Ia[a] = v12_Ia[0];
				}
				
				//for player1
				v1_I = v12_Ia[0] * policy[infoSet_Player1.getCardRank()][infoSet_Player1.getHistoryID()][samplingAction];
				//for player2
				v2_I = v12_Ia[1];
				
				
				/**
				 * update regret for player1
				 */
				long lastTime = visitMarkers[infoSet_Player1.getCardRank()][infoSet_Player1.getHistoryID()];
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					double reg = regret[infoSet_Player1.getCardRank()][infoSet_Player1.getHistoryID()][a];
					double R = reg + ( v1_Ia[a] - v1_I );
					regret[infoSet_Player1.getCardRank()][infoSet_Player1.getHistoryID()][a] = R;
					
					acc_policy[infoSet_Player1.getCardRank()][infoSet_Player1.getHistoryID()][a] += 
						(T-lastTime) * p1 * policy[infoSet_Player1.getCardRank()][infoSet_Player1.getHistoryID()][a];
				}
				
				visitMarkers[infoSet_Player1.getCardRank()][infoSet_Player1.getHistoryID()] = T;
			}
			
			/**
			 * player2 is acting player
			 */
			else {
				
				/**
				 * regret matching for the acting player
				 */
				double sumPositiveRegret = 0.0;
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					double posRegret = regret[infoSet_Player2.getCardRank()][infoSet_Player2.getHistoryID()][a];
					if( posRegret < 0.00001 )
						posRegret = 0;
					sumPositiveRegret += posRegret;
				}
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					double posRegret = regret[infoSet_Player2.getCardRank()][infoSet_Player2.getHistoryID()][a];
					if( posRegret < 0.00001 )
						posRegret = 0;
					if( sumPositiveRegret < 0.00001 )
						policy[infoSet_Player2.getCardRank()][infoSet_Player2.getHistoryID()][a] = 1.0 / Action.NUM_ACTION_TYPES;
					else
						policy[infoSet_Player2.getCardRank()][infoSet_Player2.getHistoryID()][a] = posRegret / sumPositiveRegret;
				}
				
				/**
				 * sample action using epsilon-greedy
				 */
				int samplingAction = 0;
				double greedyPro = new Random().nextDouble();
				double playerPro = new Random().nextDouble();
				double[] pro = new double[Action.NUM_ACTION_TYPES];
				if( greedyPro < epsilon ) {
					
					pro[0] = 1.0 / Action.NUM_ACTION_TYPES;
					for( int a = 1; a < Action.NUM_ACTION_TYPES; a++ ) {
						
						pro[a] = pro[a-1] + 1.0 / Action.NUM_ACTION_TYPES;
					}
				}
				else {
					pro[0] = policy[infoSet_Player2.getCardRank()][infoSet_Player2.getHistoryID()][0];
					for( int a = 1; a < Action.NUM_ACTION_TYPES; a++ ) {
						
						pro[a] = pro[a-1] + policy[infoSet_Player2.getCardRank()][infoSet_Player2.getHistoryID()][a];
					}
				}
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					if( playerPro < pro[a] ) {
						
						samplingAction = a;
						break;
					}
				}
				double sp = epsilon * 1.0/Action.NUM_ACTION_TYPES + 
						(1-epsilon)*policy[infoSet_Player2.getCardRank()][infoSet_Player2.getHistoryID()][samplingAction];
				
				double[] v2_Ia = new double[Action.NUM_ACTION_TYPES];
				GameState nextState = new GameState( state );
				OneCardPoker.doAction( nextState, samplingAction );
				int nextPlayer = 1;
				
				double p2_next = p2 * policy[infoSet_Player2.getCardRank()][infoSet_Player2.getHistoryID()][samplingAction];
				double[] v12_Ia = walkTrees_OutcomeSampling_CFR( nextPlayer, nextState, p1, p2_next, samprofile * sp, T );
				nextState = null;
				
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					if( a != samplingAction )
						v2_Ia[a] = 0.0;
					else
						v2_Ia[a] = v12_Ia[1];
				}
				
				//for player1
				v1_I = v12_Ia[0];
				//for player2
				v2_I = v12_Ia[1] * policy[infoSet_Player2.getCardRank()][infoSet_Player2.getHistoryID()][samplingAction];
				
				
				/**
				 * update regret for player2
				 */
				long lastTime = visitMarkers[infoSet_Player2.getCardRank()][infoSet_Player2.getHistoryID()];
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					double reg = regret[infoSet_Player2.getCardRank()][infoSet_Player2.getHistoryID()][a];
					double R = reg + ( v2_Ia[a] - v2_I );
					regret[infoSet_Player2.getCardRank()][infoSet_Player2.getHistoryID()][a] = R;
					
					acc_policy[infoSet_Player2.getCardRank()][infoSet_Player2.getHistoryID()][a] += 
						(T-lastTime) * p2 * policy[infoSet_Player2.getCardRank()][infoSet_Player2.getHistoryID()][a];
				}
				
				visitMarkers[infoSet_Player2.getCardRank()][infoSet_Player2.getHistoryID()] = T;
			}
		}
		
		retValues[0] = v1_I;
		retValues[1] = v2_I;
		
		return retValues;
	}

	/**
	 * MCCFR with external sampling
	 * @param player: the player whose regret value and strategy need to be updated
	 * @param currentPlayer: the acting player in current node
	 * @param state: game state
	 * @param p_i: the reaching probability of player i 
	 * @param T
	 * @return: the value of current node for player i
	 */
	private double walkTrees_ExternalSampling_CFR( int player, int currentPlayer, GameState state, double p_i, int T )
	{
	
		/**
		 * In external sampling
		 * It seems hard to update all players' strategies and regret values in parallel
		 * so we just walk tree one by one
		 */
		
		double vi_I = 0;
		OneCardPoker.checkOver( state );
		if( state.isGameOver() ) {
			
			double utilitySeat0 = OneCardPoker.getUtility( state, 0 );
			double utilitySeat1 = OneCardPoker.getUtility( state, 1 );
			
			/**
			 * External Sampling:
			 * vi(sigma,I) = sum{ui(z)*pi_-i(sigma,I)*pi(sigma,I,z)/q(z)}
			 * since q(z) = pi_-i(sigma,z)
			 * vi(sigma,I) = sum{ui(z)*pi_i(sigma,I,z)}
			 * So for the terminal node, we just return the outcome of the game
			 */
			//The player's seat number is 0
			if( player == 1 ) {
				
				vi_I = utilitySeat0;
			}
			//The player's seat number is 1
			else {
				
				vi_I = utilitySeat1;
			}
		}
		
		else {

			int playerSeat = 0;
			if( player == 2 ) {
				
				playerSeat = 1;
			}
			
			int historyID = OneCardPoker.historyID( state );
			InformationSet infoSet_Player = new InformationSet( Card.rankOfCard(state.getPlayerCard(playerSeat)), 
					historyID );
			
			InformationSet infoSet_Opp = new InformationSet( Card.rankOfCard(state.getPlayerCard(1-playerSeat)),
					historyID );
			
			/**
			 * we are the acting player now
			 */
			if( currentPlayer == player ) {
				
				/**
				 * update current policy by using regret matching
				 */
				double sumPositiveRegret = 0.0;
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					double posRegret = regret[infoSet_Player.getCardRank()][infoSet_Player.getHistoryID()][a];
					if( posRegret < 0.00001 )
						posRegret = 0;
					sumPositiveRegret += posRegret;
				}
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					double posRegret = regret[infoSet_Player.getCardRank()][infoSet_Player.getHistoryID()][a];
					if( posRegret < 0.00001 )
						posRegret = 0;
					if( sumPositiveRegret < 0.00001 )
						policy[infoSet_Player.getCardRank()][infoSet_Player.getHistoryID()][a] = 1.0 / Action.NUM_ACTION_TYPES;
					else
						policy[infoSet_Player.getCardRank()][infoSet_Player.getHistoryID()][a] = posRegret / sumPositiveRegret;
				}
				
				/**
				 * try each action in current node
				 */
				double[] vi_Ia = new double[Action.NUM_ACTION_TYPES];
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					GameState nextState = new GameState( state );
					OneCardPoker.doAction( nextState, a );
					
					//recursive
					int nextPlayer = 1;
					if( player == 1 )
						nextPlayer = 2;
					
					double p1_next = p_i * policy[infoSet_Player.getCardRank()][infoSet_Player.getHistoryID()][a];
					vi_Ia[a] = walkTrees_ExternalSampling_CFR( player, nextPlayer, nextState, p1_next, T );
					
					vi_I += vi_Ia[a] * policy[infoSet_Player.getCardRank()][infoSet_Player.getHistoryID()][a];
					
					nextState = null;
				}
				
				/**
				 * update regret for current player
				 */
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					double reg = regret[infoSet_Player.getCardRank()][infoSet_Player.getHistoryID()][a];
					double R = reg + ( vi_Ia[a] - vi_I );
					regret[infoSet_Player.getCardRank()][infoSet_Player.getHistoryID()][a] = R;
					
					acc_policy[infoSet_Player.getCardRank()][infoSet_Player.getHistoryID()][a] += 
						p_i * policy[infoSet_Player.getCardRank()][infoSet_Player.getHistoryID()][a];
				}
			}
			
			/**
			 * we are not acting currently
			 */
			else {
				
				/**
				 * we sample the opponent action according to the strategy
				 * 
				 * with some exploration??
				 *
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					if( policy1[infoSet_Opp.getCardRank()][infoSet_Opp.getHistoryID()][a] < 0.00001 ) {

						System.out.println("Exploration Happened");
						policy1[infoSet_Opp.getCardRank()][infoSet_Opp.getHistoryID()][a] += 0.1;
						policy1[infoSet_Opp.getCardRank()][infoSet_Opp.getHistoryID()][1-a] -= 0.1;
						break;
					}
				}
				*/
				int oppAction = 0;
				double oppPro = new Random().nextDouble();
				double[] pro = new double[Action.NUM_ACTION_TYPES];
				pro[0] = policy[infoSet_Opp.getCardRank()][infoSet_Opp.getHistoryID()][0];
				for( int a = 1; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					pro[a] = pro[a-1] + policy[infoSet_Opp.getCardRank()][infoSet_Opp.getHistoryID()][a];
				}
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					if( oppPro < pro[a] ) {
						
						oppAction = a;
						break;
					}
				}
				
				/**
				 * Get to the next inforamtion set
				 * and walk trees recursively
				 */
				GameState nextState = new GameState( state );
				OneCardPoker.doAction( nextState, oppAction );
				
				int nextPlayer = player;

				/**
				 * just let vi(I,a) = vi(I) if we are not acting currently?
				 * because the opponent's action is sampled according to some scheme
				 */
				double vi_Ia = walkTrees_ExternalSampling_CFR( player, nextPlayer, nextState, p_i, T );
				
				vi_I = vi_Ia;
				
				nextState = null;
			}
		}
		
		return vi_I;
	}
}
