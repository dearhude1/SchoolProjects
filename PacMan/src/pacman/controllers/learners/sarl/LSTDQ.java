package pacman.controllers.learners.sarl;

import java.util.ArrayList;
import java.util.EnumMap;

import Jama.Matrix;

import pacman.game.Constants;
import pacman.game.Game;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;

/**
 * the implementation of Q(lambda) with function approximation
 *
 */
public class LSTDQ extends RLWithFA {

	private static final String valueFunctionFileName = "./valueFunction_LSTDQ_";
	
	/**
	 * for eligibility trace in Q(lambda)
	 *
	private HashMap<GHOST, double[][]> ghostEligMap = 
			new HashMap<Constants.GHOST, double[][]>();
			*/
	
	/**
	 * parameters for eligibility trace
	 */
	protected double LAMBDA = 0.9;
	
	/**
	 * this member means that whether 
	 * the Q-learning ghosts learn in the game 
	 * or act greedily according to a learnt value function
	 */
	private boolean doesLearn = true;
	
	/**
	 * each ghost keeps an array of the experienced state-action-reward pairs
	 * after each episode ends, we update each ghost's value function
	 * 
	 * from the updating rules, we can only store the reward and the 
	 * experienced state features
	 */
	private EnumMap<GHOST, ArrayList<Double>> rewardSequenceMap = 
			new EnumMap<Constants.GHOST, ArrayList<Double>>(GHOST.class);
	private EnumMap<GHOST, ArrayList<Integer>> strategySequenceMap = 
			new EnumMap<Constants.GHOST, ArrayList<Integer>>(GHOST.class);
	private EnumMap<GHOST, ArrayList<double[]>> featureSequenceMap = 
			new EnumMap<Constants.GHOST, ArrayList<double[]>>(GHOST.class);
	
	public LSTDQ( Game game, double lambda, boolean bLearn )
	{
		
		super(game);
		
		LAMBDA = lambda;
		doesLearn = bLearn;
		
		for( GHOST ghostType : GHOST.values() ) {
			
			//the eligilibity map
			/**
			if( !ghostEligMap.containsKey(ghostType) ) {
				
				double[][] ghostElig = new double[Constants.NUM_GHOST_STRATEGIES][NUM_FEATURES];
				for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES; strategy++ )	
					for( int fIndex = 0; fIndex < NUM_FEATURES; fIndex++ ) {
					
						ghostElig[strategy][fIndex] = 0.0;
				}
				
				ghostEligMap.put(ghostType, ghostElig);	
			}
			*/
			
			//the three sequence maps
			if( !rewardSequenceMap.containsKey(ghostType) ) {
				
				ArrayList<Double> rwdSeq = new ArrayList<Double>();
				rewardSequenceMap.put(ghostType, rwdSeq);
			}
			if( !strategySequenceMap.containsKey(ghostType) ) {
				
				ArrayList<Integer> strSeq = new ArrayList<Integer>();
				strategySequenceMap.put(ghostType, strSeq);
			}
			if( !featureSequenceMap.containsKey(ghostType) ) {
				
				ArrayList<double[]> feaSeq = new ArrayList<double[]>();
				featureSequenceMap.put(ghostType, feaSeq);
			}
		}
		
		/**
		 * read the value function
		 */
		if( !doesLearn )
			readValueFunction( valueFunctionFileName );
	}
	
	public LSTDQ( Game game, double alpha, 
			double gamma, double epsilon, double lambda, 
			boolean bLearn )
	{
		super(game, alpha, gamma, epsilon);
		
		LAMBDA = lambda;
		doesLearn = bLearn;
		
		for( GHOST ghostType : GHOST.values() ) {
			
			/**
			if( !ghostEligMap.containsKey(ghostType) ) {
				
				double[][] ghostElig = new double[Constants.NUM_GHOST_STRATEGIES][NUM_FEATURES];
				for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES; strategy++ )	
					for( int fIndex = 0; fIndex < NUM_FEATURES; fIndex++ ) {
					
						ghostElig[strategy][fIndex] = 0.0;
				}
				
				ghostEligMap.put(ghostType, ghostElig);	
			}
			*/
			
			//the three sequence maps
			if( !rewardSequenceMap.containsKey(ghostType) ) {
				
				ArrayList<Double> rwdSeq = new ArrayList<Double>();
				rewardSequenceMap.put(ghostType, rwdSeq);
			}
			if( !strategySequenceMap.containsKey(ghostType) ) {
				
				ArrayList<Integer> strSeq = new ArrayList<Integer>();
				strategySequenceMap.put(ghostType, strSeq);
			}
			if( !featureSequenceMap.containsKey(ghostType) ) {
				
				ArrayList<double[]> feaSeq = new ArrayList<double[]>();
				featureSequenceMap.put(ghostType, feaSeq);
			}
		}
		
		/**
		 * read the value function
		 */
		if( !doesLearn )
			readValueFunction( valueFunctionFileName );
	}
	
	
	/**
	 * Updates the game state: a copy of the game is passed to this method 
	 * and the class variable is updated accordingly.
	 * Also, value function is updated in this method.
	 *
	 * @param game A copy of the current game
	 * @param timeDue The time the next move is due
	 */
	public void update( Game curGame, long timeDue )
	{
		synchronized(this)
		{
			this.game = curGame;
			this.timeDue = timeDue;
			wasSignalled = true;
			hasComputed = false;
			
			/**
			 * update the reward for each ghost 
			 * when a pill or a power pill was eaten
			 * or when a ghost was eaten 
			 * or the pacman was eaten
			 * 
			 * when update the value function 
			 * we should also compute the reward in respect to pills and power pills
			 */
			for( GHOST ghostType : GHOST.values() ) {
				
				if( curGame.wasGhostEaten( ghostType ) ) {
					
					double rwd = ghostRewardMap.get(ghostType);
					rwd -= Constants.GHOST_EAT_SCORE * curGame.getGhostEatMultiplier() / 2;
					ghostRewardMap.put(ghostType, rwd);
				}
			}
			if( curGame.wasPacManEaten() ) {
				
				for( GHOST ghostType : GHOST.values() ) {
					
					if( curGame.ghostEatsPacman == ghostType ) {
						
						double rwd = ghostRewardMap.get(ghostType);
						rwd += Constants.AWARD_LIFE_LEFT;
						ghostRewardMap.put(ghostType, rwd);
						break;
					}
				}
			}
			
			/**
			 * now we update only when an episode ends 
			 * if we are not in a decision state
			 */			
			if( doesLearn && curGame.wasEpisodeEnded() ) {
				
				updateValueFunction_EpiEnded( curGame );
			}
			
			
			/**
			 * if an episode ends
			 * then a new episode begin, 
			 * therefore we should compute the ARPS in the last episode
			 */
			if( curGame.wasEpisodeEnded() ) {
				
				int currentEp = curGame.getCurrentEpisode();
				
				for( int ghostIndex = 0; ghostIndex < 4; ghostIndex++ ) {
					
					if( gameSteps[ghostIndex] > 0 ) 
						arpsPerEpisode[ghostIndex][currentEp] /= gameSteps[ghostIndex];
					else 
						arpsPerEpisode[ghostIndex][currentEp] = 0.0;
					
					gameSteps[ghostIndex] = 0;	
				}
			}
			
			if( game.gameOver() && doesLearn ) {
				
				storeValueFunction( valueFunctionFileName );
				
				/**
				 * write the ARPS into files
				 */
				storeARPS();
			}
			
			notify();
		}
	}
	
	/**
	 * override the method getMove, 
	 * which returns the move of the ghosts
	 */
	public EnumMap<GHOST,MOVE> getMove(Game game, long timeDue)
	{	
		
		/**
		 * note that a ghost can compute a move 
		 * only after one update is conducted
		 */
		if( hasComputed() )
			return curMoves;
	
		
		//curMoves.clear();
		for(GHOST ghostType : GHOST.values())
			if(game.doesGhostRequireAction(ghostType)) {
			
				/**
				 * choose the max strategy according to the value function
				 */
				int maxStrategy = getMaxStrategy( game, ghostType );
				
				
				/**
				 * do epsilon-greedy
				 */
				int chosenStrategy = epsilonGreedy( game, ghostType, maxStrategy );
				
				/**
				 * chose the real action (namely direction) according 
				 * to the strategy
				 */
				MOVE chosenMove = getMoveAccordingToStrategy( game, 
						ghostType, chosenStrategy );
				
				/**
				 * set the move of the ghost
				 */
				if( curStrategies.containsKey(ghostType) )
					curStrategies.remove(ghostType);
				curStrategies.put( ghostType, chosenStrategy );
				curMoves.remove( ghostType );
				curMoves.put( ghostType, chosenMove );
				
				
				/**
				 * then update the value function??
				 */
				if( doesLearn ) {
					
					updateValueFunction_New( game, ghostType );
				}
				
				/**
				 * store the last game state
				 */
				lastGames.put( ghostType, game );
				
				lastStrategies.put( ghostType, chosenStrategy );
			}
		
		////////////////////////////////////
		//////////Added by dearhude1////////
		////////////////////////////////////
		hasComputed = true;
		lastMove = curMoves;
		
		return curMoves;
	}

	
	/**
	 * for LSTDQ, we only record the experienced state-action-reward 
	 * pairs in the function
	 */
	private void updateValueFunction_New( Game curGame, GHOST ghostType )
	{

		if( lastMove == null || 
				!lastMove.containsKey(ghostType) || 
				!lastStrategies.containsKey(ghostType) || 
				!lastGames.containsKey(ghostType) )
			return;
		
		/**
		 * get the last action of this ghost
		 */
		MOVE ghostLastMov = lastMove.get( ghostType );
		int ghostLastStrategy = lastStrategies.get( ghostType );
		if( curGame.getGhostCurrentNodeIndex(ghostType) != 
				curGame.getGhostInitialNodeIndex() && 
				ghostLastMov != MOVE.NEUTRAL ) {
			
			/**
			 * get the last game state
			 */
			Game lastGame = lastGames.get( ghostType );

			/**
			 * compute the reward in the last step
			 * compute the reward with respect to pills and power pills
			 * 
			 * we should reset the reward in the reward map to zero
			 */
			//double pillRwd = curGame.getRemainingPillScore() - lastGame.getRemainingPillScore();
			//double powerPillRwd = curGame.getRemainingPowerPillScore() - 
					//lastGame.getRemainingPowerPillScore();
			//double rwd = pillRwd + powerPillRwd + ghostRewardMap.get(ghostType);
			double rwd = ghostRewardMap.get(ghostType);
			double timeDuration = curGame.getCurrentLevelTime() - lastGame.getCurrentLevelTime();
			rwd /= timeDuration;
			ghostRewardMap.put( ghostType, 0.0 );
			
			//store the reward
			rewardSequenceMap.get(ghostType).add(rwd);
			
			//store the strategy
			strategySequenceMap.get(ghostType).add(ghostLastStrategy);
			
			//store the experienced features
			double[][] featureValues = computeFeatureValueMatrix( lastGame, 
					ghostType, ghostLastStrategy );
			double[] features = new double[Constants.NUM_FEATURES_SINGLE_AGENT];
			for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_SINGLE_AGENT; fIndex++ ) {
				
				features[fIndex] = featureValues[ghostLastStrategy][fIndex];
			}
			featureSequenceMap.get(ghostType).add(features);
			
			
			/**
			 * record the reward for the current step 
			 * and increase the game step
			 */
			int ghostIndex = queryGhostIndex( ghostType );
			int episode = game.getCurrentEpisode();
			arpsPerEpisode[ghostIndex][episode] += rwd;
			gameSteps[ghostIndex] += 1;
			
			/**
			 * after updating 
			 * clear the EnumMap
			 */
			/**
			if( lastStrategies.containsKey(ghostType) )
				lastStrategies.remove(ghostType);
			*/
			if( lastGames.containsKey(ghostType))
				lastGames.remove(ghostType);
		}
	}
	
	/**
	 * for LSTDQ, we find a least square match of the sampled data
	 * @param curGame
	 */
	private void updateValueFunction_EpiEnded( Game curGame )
	{
		
		if( !curGame.wasEpisodeEnded() ) {
			
			System.out.println("Cannot Update since episode not ended");
			return;
		}
		if( lastMove == null ) {
			
			System.out.println("Last Move is NULL");
			return;
		}
		
		if( curGame.gameOver() )
			System.out.println("Update When Game Over");
		
		
		/**
		 * compute the final reward of the episode
		 */
		double fnlReward = 0;
		int levelTime = curGame.getLevelTimeWhenEpEnded();
		double lostScore = curGame.getLostScoreWhenEpEnded();
		double remainingScore = curGame.getRemainingScoreWhenEpEnded();
		if( levelTime > 0 ) {
			
			/**
			//fnlReward = (remainingScore - lostScore) / levelTime;
			//fnlReward = remainingScore - lostScore;
			//fnlReward = 1.0 / lostScore;
			//fnlReward = remainingScore / lostScore;
			fnlReward = (remainingScore+1.0) / (lostScore+1.0) - 
					(lostScore+1.0) / (remainingScore+1.0);
			fnlReward /= levelTime;
			*/
			
			fnlReward = remainingScore / levelTime;

			System.out.println("final reward "+fnlReward);
		}
		else {
			
			System.out.println("Level Time is 0!!!");
		}
		
		/**
		 * update the value function if 
		 * currently the ghost is not in the initial position
		 * 
		 * the member lastGame is not null
		 */
		for(GHOST ghostType : GHOST.values()) {
			
			if( !lastMove.containsKey(ghostType) || 
					!lastStrategies.containsKey(ghostType) || 
					!lastGames.containsKey(ghostType) )
				continue;
			
			/**
			 * get the last action of this ghost
			 */
			MOVE ghostLastMov = lastMove.get( ghostType );
			int ghostLastStrategy = lastStrategies.get( ghostType );
			
			if( ghostLastMov != MOVE.NEUTRAL ) {
				
				/**
				 * get the last game state
				 */
				Game lastGame = lastGames.get( ghostType );

				/**
				 * compute the reward for the (T-1) step
				 */
				double timeDuration = levelTime - 
						lastGame.getCurrentLevelTime();
				double rwd = fnlReward + ghostRewardMap.get(ghostType) / timeDuration;
				ghostRewardMap.put( ghostType,  0.0 );
				
				/**
				 * store the data for (T-1) step
				 */
				double[][] featureValues = computeFeatureValueMatrix( lastGame, 
						ghostType, ghostLastStrategy );
				rewardSequenceMap.get(ghostType).add(rwd);
				strategySequenceMap.get(ghostType).add(ghostLastStrategy);
				double[] feature = new double[Constants.NUM_FEATURES_SINGLE_AGENT];
				for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_SINGLE_AGENT; fIndex++ ) {
					
					feature[fIndex] = featureValues[ghostLastStrategy][fIndex];
				}
				featureSequenceMap.get(ghostType).add(feature);
				
				
				/**
				 * then find the least square match of the data
				 * from time step 1 to time step (T-1)
				 */
				int T = rewardSequenceMap.get(ghostType).size();
				int matSize = Constants.NUM_FEATURES_SINGLE_AGENT * Constants.NUM_GHOST_STRATEGIES_SINGLE_AGENT;
				
				//we can compute the left sum and the right sum at the same time
				Matrix leftSumMat = new Matrix( matSize, matSize );
				Matrix rightSumMat = new Matrix( matSize, 1 );
				for( int row = 0; row < matSize; row++ ) {
					
					rightSumMat.set( row, 0, 0.0 );
					for( int col = 0; col < matSize; col++ )
						leftSumMat.set( row, col, 0.0 );
				}
				
				Matrix phi_t = new Matrix(matSize, 1);
				Matrix phi_tPlusOne = new Matrix(matSize, 1);
				for( int t = 0; t < T; t++ ) {
					
					/**
					 * read the recorded data
					 */
					double[] feature_t = featureSequenceMap.get(ghostType).get(t);
					double[] feature_tPlusOne = null;
					if( t < (T-1) )
						feature_tPlusOne = featureSequenceMap.get(ghostType).get(t+1);
					
					int strategy_t = strategySequenceMap.get(ghostType).get(t);
					int strategy_tPlusOne = -1;
					if( t < (T-1) )
						strategy_tPlusOne = strategySequenceMap.get(ghostType).get(t+1);
					
					double reward_t = rewardSequenceMap.get(ghostType).get(t);
					
					/**
					 * set the real feature vector
					 */
					int realFeatureIndex = 0;
					for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES_SINGLE_AGENT; strategy++ ) {
						
						for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_SINGLE_AGENT; fIndex++ ) {
							
							//phi_t
							if( strategy == strategy_t )
								phi_t.set( realFeatureIndex, 0, feature_t[fIndex] );
							else
								phi_t.set( realFeatureIndex, 0, 0 );
				
							//phi_tPlusOne
							if( strategy == strategy_tPlusOne && 
									feature_tPlusOne != null )
								phi_tPlusOne.set( realFeatureIndex, 0, feature_tPlusOne[fIndex] );
							else
								phi_tPlusOne.set( realFeatureIndex, 0, 0 );
							
							realFeatureIndex++;
						}
					}
					
					/**
					 * compute phi_t * (phi_t - gamma * phi_tPlusOne)
					 */
					Matrix diffMatrix = phi_t.minus(phi_tPlusOne.times(GAMMA));
					//Matrix prodMatrix = phi_t.times( diffMatrix.transpose() );
					//leftSumMat.plusEquals(prodMatrix);
					
					/**
					 * compute phi_t * reward_t
					 */
					//Matrix prodMatrix_p = phi_t.times(reward_t);
					//rightSumMat.plusEquals(prodMatrix_p);
					
					
					Matrix prodMatrix_bellResidual_left = diffMatrix.times(diffMatrix.transpose());
					Matrix prodMatrix_bellResidual_right = diffMatrix.times( reward_t );
					leftSumMat.plusEquals( prodMatrix_bellResidual_left );
					rightSumMat.plusEquals( prodMatrix_bellResidual_right );
				}
				leftSumMat.timesEquals(1.0/T);
				rightSumMat.timesEquals(1.0/T);
				
				/**
				 * then we compute the final vector according to 
				 * leftSumMat^(-1) * rightSumMat
				 */
				Matrix resultMat = null;//leftSumMat.inverse().times(rightSumMat);
				
				while( Math.abs(leftSumMat.det()) < 0.0001 ) {
					
					for( int i = 0; i < leftSumMat.getRowDimension(); i++ ) {
						
						double value = leftSumMat.get(i, i);
						leftSumMat.set(i, i, value + 0.0001);
					}
				}
				
				if( leftSumMat.det() != 0.0 ) {
					
					resultMat = leftSumMat.inverse().times(rightSumMat);
					System.out.println("Have Inverse Matrix "+leftSumMat.det());
				}
				else {
					
					System.out.println("No Inverse Matrix this time");
				}
				
				/**
				 * then we update the parameter theta
				 */
				int realFeatureIndex = 0;
				for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES_SINGLE_AGENT; strategy++ ) {
					for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_SINGLE_AGENT; fIndex++ ) {
					
						if( resultMat != null ) 
							ghostThetaMap.get(ghostType)[strategy][fIndex] = 
								resultMat.get( realFeatureIndex, 0 );
					}
				}
				
				/**
				 * after updating the parameter theta, 
				 * we should clean the data sequence
				 */
				rewardSequenceMap.get(ghostType).clear();
				strategySequenceMap.get(ghostType).clear();
				featureSequenceMap.get(ghostType).clear();

				
				//record the reward for the current step and increase the game step
				int ghostIndex = queryGhostIndex( ghostType );
				int episode = game.getCurrentEpisode();
				arpsPerEpisode[ghostIndex][episode] += fnlReward;
				gameSteps[ghostIndex] += 1;
				
				/**
				 * after updating 
				 * clear the EnumMap
				 */
				/**
				if( lastStrategies.containsKey(ghostType) )
					lastStrategies.remove(ghostType);
				*/
				if( lastGames.containsKey(ghostType)) {
					
					lastGames.remove(ghostType);
				}
			}
		}
	}
	
	
}
