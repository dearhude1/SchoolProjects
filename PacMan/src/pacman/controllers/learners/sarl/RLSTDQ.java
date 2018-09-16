package pacman.controllers.learners.sarl;
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
public class RLSTDQ extends RLWithFA {
	
	
	/**
	 * parameters for eligibility trace
	 */
	protected double LAMBDA = 0.9;
	
	/**
	 * this member means that whether 
	 * the Q-learning ghosts learn in the game 
	 * or act greedily according to a learnt value function
	 */
	protected boolean doesLearn = true;
	
	protected EnumMap<GHOST, Matrix> cMatrixMap = new 
			EnumMap<Constants.GHOST, Matrix>(GHOST.class);
	
	public RLSTDQ( Game game, double lambda, boolean bLearn )
	{
		
		super(game);
		
		LAMBDA = lambda;
		doesLearn = bLearn;
		
		for( GHOST ghostType : GHOST.values() ) {
			
			//the Matrix C for value function updating
			int matSize = Constants.NUM_GHOST_STRATEGIES_SINGLE_AGENT * Constants.NUM_FEATURES_SINGLE_AGENT;
			if( !cMatrixMap.containsKey(ghostType) ) {
				
				Matrix c = new Matrix( matSize, matSize );
				for( int i = 0; i < matSize; i++ ) 
					for( int j = 0; j < matSize; j++ ) {
						
						if( i == j )
							c.set( i, j, 10 );
						else
							c.set( i, j, 0 );
					}
				cMatrixMap.put(ghostType, c);
			}
		}
		
		
		/**
		 * read the value function
		 */
		valueFunctionFileName = "./valueFunction_RLSTDQMax_";
		if( !doesLearn )
			readValueFunction( valueFunctionFileName );
	}
	
	public RLSTDQ( Game game, double alpha, 
			double gamma, double epsilon, double lambda, 
			boolean bLearn )
	{
		super(game, alpha, gamma, epsilon);
		
		LAMBDA = lambda;
		doesLearn = bLearn;
		
		for( GHOST ghostType : GHOST.values() ) {
			
			//the Matrix C for value function updating
			int matSize = Constants.NUM_GHOST_STRATEGIES_SINGLE_AGENT * Constants.NUM_FEATURES_SINGLE_AGENT;
			if( !cMatrixMap.containsKey(ghostType) ) {
				
				Matrix c = new Matrix( matSize, matSize );
				for( int i = 0; i < matSize; i++ ) 
					for( int j = 0; j < matSize; j++ ) {
						
						if( i == j )
							c.set( i, j, 10000 );
						else
							c.set( i, j, 0 );
					}
				cMatrixMap.put(ghostType, c);
			}
		}
		
		/**
		 * read the value function
		 */
		valueFunctionFileName = "./valueFunction_RLSTDQMax_";
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
			
			//old version of rewards////////////////////////////////////////////
			/**
			 * update the reward for each ghost 
			 * when a pill or a power pill was eaten
			 * or when a ghost was eaten 
			 * or the pacman was eaten
			 * 
			 * when update the value function 
			 * we should also compute the reward in respect to pills and power pills
			 *
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
			*/
			//old version of rewards////////////////////////////////////////////
			
			
			//new version of rewards////////////////////////////////////////////
			for( GHOST ghostType : GHOST.values() ) {
				
				if( curGame.wasGhostEaten( ghostType ) ) {
					
					//find out which ghosts can have a reward
					double rwdNum = 0;
					EnumMap<GHOST, Boolean> rwdedGhostMap = 
							new EnumMap<Constants.GHOST, Boolean>(GHOST.class);
					
					for( GHOST otherGhost : GHOST.values() ) {
						
						if( otherGhost == ghostType ) {
							
							rwdNum += 1.0;
							rwdedGhostMap.put( otherGhost, true );
							continue;
						}
						
						Game othGhostLastGame = lastGames.get(otherGhost);
						if( othGhostLastGame == null || 
								!othGhostLastGame.isGhostEdible(ghostType) || 
								othGhostLastGame.isGhostEdible(otherGhost) || 
								curGame.isGhostEdible(otherGhost) || 
								curGame.getGhostLairTime(otherGhost) > 0) {
							
							rwdedGhostMap.put(otherGhost, false);
							continue;
						}
						
						int othGhostNodeIndex = othGhostLastGame.getGhostCurrentNodeIndex(otherGhost);
						int pacmanLastNodeIndex = othGhostLastGame.getPacmanCurrentNodeIndex();
						MOVE othGhostLastMov = othGhostLastGame.getGhostLastMoveMade(otherGhost);
						//int eventNodeIndex = curGame.ghostsPositionWhenEaten.get(ghostType);
						int eatenGhostNodeIndex = othGhostLastGame.getGhostCurrentNodeIndex(ghostType);
						
						int disGhostToEatenGhost = othGhostLastGame.getShortestPathDistance(
								othGhostNodeIndex, eatenGhostNodeIndex, othGhostLastMov );
						int disPacmanToEatenGhost = othGhostLastGame.getShortestPathDistance(
								pacmanLastNodeIndex, eatenGhostNodeIndex );
						
						if( disGhostToEatenGhost > disPacmanToEatenGhost ) {
							
							rwdedGhostMap.put(otherGhost, false);
							continue;
						}
						/**
						 * whether the eaten ghost is on 
						 * both the path between Pacman to the ghost 
						 * and the path between the ghost to Pacman
						 */
						boolean eatenGhostOnPath = false;
						int[] path = othGhostLastGame.getShortestPath(pacmanLastNodeIndex, 
								othGhostNodeIndex);
						for( int i = 0; i < path.length; i++ ) {
							
							if( path[i] == eatenGhostNodeIndex ) {
								
								eatenGhostOnPath = true;
								break;
							}
						}
						
						if( !eatenGhostOnPath ) {
							
							rwdedGhostMap.put(otherGhost, false);
							continue;
						}
						
						eatenGhostOnPath = false;
						path = othGhostLastGame.getShortestPath(othGhostNodeIndex, 
								pacmanLastNodeIndex, othGhostLastMov);
						for( int i = 0; i < path.length; i++ ) {
							
							if( path[i] == eatenGhostNodeIndex ) {
								
								eatenGhostOnPath = true;
								break;
							}
						}
						
						if( eatenGhostOnPath ) {
							
							rwdNum += 1.0;
							rwdedGhostMap.put(otherGhost, true);
						}
						else 
							rwdedGhostMap.put(otherGhost, false);
					}
					
					//assign the reward
					for( GHOST otherGhost : GHOST.values() ) {
						
						if( rwdedGhostMap.get(otherGhost) ) {
							
							double rwd = ghostRewardMap.get(ghostType);
							rwd -= Constants.GHOST_EAT_SCORE * 
									curGame.getGhostEatMultiplier() / 2 / rwdNum;
							ghostRewardMap.put(ghostType, rwd);
						}
					}
				}
			}
			
			if( curGame.wasPacManEaten() ) {
				
				double rwdNum = 0;
				EnumMap<GHOST, Boolean> rwdedGhostMap = 
						new EnumMap<Constants.GHOST, Boolean>(GHOST.class);
				for( GHOST ghostType : GHOST.values() ) {
					
					//for the eating ghost
					if( curGame.ghostEatsPacman == ghostType ) {
						
						rwdNum += 1.0;
						rwdedGhostMap.put(ghostType, true);
					}
					//for the ghosts making this eating event
					else {
						
						Game copyCurGame = curGame.gameCopyWhenPacmanEaten;
						
						int ghostNodeIndexWhenEaten = copyCurGame.getGhostCurrentNodeIndex(ghostType);
						int pacmanNodeIndexWhenEaten = copyCurGame.getPacmanCurrentNodeIndex();
						MOVE ghostMovWhenEaten = copyCurGame.getGhostLastMoveMade(ghostType);
						
						int disGhostToPacman = copyCurGame.getShortestPathDistance( 
								ghostNodeIndexWhenEaten, pacmanNodeIndexWhenEaten, 
								ghostMovWhenEaten );
						//the current node index of the ghost which eats pacman
						int ghostEatPacmanCurNodeIndex = 
								copyCurGame.getGhostCurrentNodeIndex(curGame.ghostEatsPacman);
						int[] path = copyCurGame.getShortestPath( ghostNodeIndexWhenEaten, 
								ghostEatPacmanCurNodeIndex, ghostMovWhenEaten );
						boolean onPath = false;
						for( int i = 0; i < path.length; i++ ) {
							
							if( path[i] == pacmanNodeIndexWhenEaten ) {
								
								onPath = true;
								break;
							}
						}
						
						if( !copyCurGame.isGhostEdible(ghostType) && 
								copyCurGame.getGhostLairTime(ghostType) == 0 && 
								disGhostToPacman <= 20 && onPath ) {
							
							rwdNum += 1.0;
							rwdedGhostMap.put(ghostType, true);
						}
						else
							rwdedGhostMap.put(ghostType, false);
					}
				}
				//all related ghosts share the total reward
				for( GHOST ghostType : GHOST.values() ) { 
					
					if( rwdedGhostMap.get(ghostType) ) {
						
						double rwd = ghostRewardMap.get(ghostType);
						rwd += Constants.AWARD_LIFE_LEFT / rwdNum;
						ghostRewardMap.put(ghostType, rwd);
					}
				}
			}
			//new version of rewards////////////////////////////////////////////
			
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
				 * why not also use the current choosen strategy 
				 * to update the value function???
				 */
				if( doesLearn ) {
					
					updateValueFunction( game, ghostType, chosenStrategy );
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
	protected void updateValueFunction( Game curGame, GHOST ghostType, 
			int chosenStrategy )
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
			
			
			//store the experienced features
			double[][] featureValues = computeFeatureValueMatrix( lastGame, 
					ghostType, ghostLastStrategy );
			double[] features = new double[Constants.NUM_FEATURES_SINGLE_AGENT];
			for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_SINGLE_AGENT; fIndex++ ) {
				
				features[fIndex] = featureValues[ghostLastStrategy][fIndex];
			}
						
			
			/**
			 * compute the TD error: r + maxQ(s',a') - Q(s,a)
			 */
			double Qsa = getQValue(lastGame, ghostType, ghostLastStrategy);
			double Qsap = getQValue(curGame, ghostType, chosenStrategy);
			double delta_t = rwd + GAMMA * Qsap - Qsa;
			
			/**
			 * get the feature matrix phi_t and phi_(t+1)
			 */
			int matSize = Constants.NUM_FEATURES_SINGLE_AGENT * Constants.NUM_GHOST_STRATEGIES_SINGLE_AGENT;
			Matrix phi_t = new Matrix(matSize, 1);
			Matrix phi_tPlusOne = new Matrix(matSize, 1);
			double[][] featureMatrix_t = computeFeatureValueMatrix(lastGame, 
					ghostType, ghostLastStrategy);
			double[][] featureMatrix_tpo = computeFeatureValueMatrix(curGame, 
					ghostType, chosenStrategy);
			
			int realFeatureIndex = 0;
			for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES_SINGLE_AGENT; strategy++ ) {
				
				for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_SINGLE_AGENT; fIndex++ ) {
					
					phi_t.set( realFeatureIndex, 0, featureMatrix_t[strategy][fIndex] );
					phi_tPlusOne.set( realFeatureIndex, 0, featureMatrix_tpo[strategy][fIndex]);
					realFeatureIndex++;
				}
			}
			
			/**
			 * update the learning parameter
			 */
			Matrix C_t = cMatrixMap.get(ghostType);
			Matrix diffMatrix = phi_t.minus(phi_tPlusOne.times(GAMMA));
			double denominator = 1+diffMatrix.transpose().times(C_t).times(phi_t).get(0,0);
			Matrix thetaInc = C_t.times( phi_t ).times( delta_t / denominator );

			realFeatureIndex = 0;
			for( int s = 0; s < Constants.NUM_GHOST_STRATEGIES_SINGLE_AGENT; s++ ) {
				
				for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_SINGLE_AGENT; fIndex++ ) {
					
				
					if( s == ghostLastStrategy ) {
						
						ghostThetaMap.get(ghostType)[s][fIndex] += 
								thetaInc.get(realFeatureIndex, 0);
					}
					realFeatureIndex++;	
				}
			}
			//System.out.println("============= "+denominator);
			
			/**
			 * update the matrix C
			 */
			Matrix numeratorMat = C_t.times(phi_t).times(diffMatrix.transpose()).times(C_t);
			C_t = C_t.minus( numeratorMat.times( 1.0 / denominator) );
			cMatrixMap.remove(ghostType);
			cMatrixMap.put(ghostType, C_t);
			
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
				 * compute the TD error: r + maxQ(s',a') - Q(s,a)
				 */
				double Qsa = getQValue(lastGame, ghostType, ghostLastStrategy);
				double Qsap = 0.0;
				double delta_t = rwd + GAMMA * Qsap - Qsa;
				
				/**
				 * get the feature matrix phi_t and phi_(t+1)
				 */
				int matSize = Constants.NUM_FEATURES_SINGLE_AGENT * Constants.NUM_GHOST_STRATEGIES_SINGLE_AGENT;
				Matrix phi_t = new Matrix(matSize, 1);
				Matrix phi_tPlusOne = new Matrix(matSize, 1);
				double[][] featureMatrix_t = computeFeatureValueMatrix(lastGame, 
						ghostType, ghostLastStrategy);
				
				int realFeatureIndex = 0;
				for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES_SINGLE_AGENT; strategy++ ) {
					
					for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_SINGLE_AGENT; fIndex++ ) {
						
						phi_t.set( realFeatureIndex, 0, featureMatrix_t[strategy][fIndex] );
						phi_tPlusOne.set( realFeatureIndex, 0, 0);
						realFeatureIndex++;
					}
				}
				
				/**
				 * update the learning parameter
				 */
				Matrix C_t = cMatrixMap.get(ghostType);
				Matrix diffMatrix = phi_t.minus(phi_tPlusOne.times(GAMMA));
				double denominator = 1+diffMatrix.transpose().times(C_t).times(phi_t).get(0,0);
				Matrix thetaInc = C_t.times( phi_t ).times( delta_t / denominator );

				realFeatureIndex = 0;
				for( int s = 0; s < Constants.NUM_GHOST_STRATEGIES_SINGLE_AGENT; s++ ) {
					
					for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_SINGLE_AGENT; fIndex++ ) {
						
					
						if( s == ghostLastStrategy ) {
							
							ghostThetaMap.get(ghostType)[s][fIndex] += 
									thetaInc.get(realFeatureIndex, 0);
						}
						realFeatureIndex++;	
					}
				}
				//System.out.println("============= "+denominator);
				
				/**
				 * update the matrix C
				 */
				Matrix numeratorMat = C_t.times(phi_t).times(diffMatrix.transpose()).times(C_t);
				C_t = C_t.minus( numeratorMat.times( 1.0 / denominator) );
				cMatrixMap.remove(ghostType);
				cMatrixMap.put(ghostType, C_t);

				
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
