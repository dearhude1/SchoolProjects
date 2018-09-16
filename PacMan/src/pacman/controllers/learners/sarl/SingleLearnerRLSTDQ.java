package pacman.controllers.learners.sarl;

import java.util.EnumMap;

import Jama.Matrix;

import pacman.game.Constants;
import pacman.game.Game;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;

/**
 * Single-agent Learner ghosts with RLSTDQ algorithm:
 * only one ghost is equipped with RL algorithm
 * the other three ghosts are Starter Ghosts, namely rule-based ghosts
 */
public class SingleLearnerRLSTDQ extends RLSTDQ {

	//attack Ms Pac-Man with this probability
	private final static float CONSISTENCY=0.9f;
	
	//if Ms Pac-Man is this close to a power pill, back away
	private final static int PILL_PROXIMITY=35;		
	
	/**
	 * the ghost which is equipped with RL algorithms
	 */
	private GHOST rlGhost;
	
	public SingleLearnerRLSTDQ( Game game, GHOST learningGhost, 
			double lambda, boolean bLearn )
	{
		super( game, lambda, bLearn );
		
		rlGhost = learningGhost;
		
		
		/**
		 * read the value function
		 */
		valueFunctionFileName = "./valueFunction_SingleLearnerRLSTDQ_"+rlGhost.toString()+"_";
		if( !doesLearn )
			readValueFunction( valueFunctionFileName );
	}
	
	public SingleLearnerRLSTDQ( Game game, GHOST learningGhost, 
			double alpha, double gamma, double epsilon, 
			double lambda, boolean bLearn )
	{
		super( game, alpha, gamma, epsilon, lambda, bLearn );
		
		rlGhost = learningGhost;
		
		/**
		 * read the value function
		 */
		valueFunctionFileName = "./valueFunction_SingleLearnerRLSTDQ_"+rlGhost.toString()+"_";
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
			*/
			//old version of rewards////////////////////////////////////////////
			
			//new version of rewards////////////////////////////////////////////
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
				 * the reinforcement learning ghost
				 */
				if( ghostType == rlGhost ) {
				
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
					 * then update the value function
					 * call the updating method of father class RLSTDQGhosts
					 */
					if( doesLearn ) {
						
						updateValueFunction( game, ghostType,chosenStrategy );
					}
					
					/**
					 * store the last game state
					 */
					lastGames.put( ghostType, game );
					
					lastStrategies.put( ghostType, chosenStrategy );
					
				}
				/**
				 * the rule-based ghosts
				 */
				else {
					
					int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);
					MOVE ghostLastMov = game.getGhostLastMoveMade(ghostType);
					
					//retreat from Ms Pac-Man if edible or if Ms Pac-Man is close to power pill
					if(game.getGhostEdibleTime(ghostType)>0 || 
							closeToPower(game)) {	
					
						MOVE mov = game.getApproximateNextMoveAwayFromTarget(
								ghostNodeIndex, game.getPacmanCurrentNodeIndex(), 
								ghostLastMov,DM.PATH );
						curMoves.remove(ghostType);
						curMoves.put(ghostType, mov);
					
						//System.out.println("Now Escape!");
					}
					else 
					{
						//attack Ms Pac-Man otherwise (with certain probability)
						if( random.nextFloat() < CONSISTENCY )	{		
							
							MOVE mov = game.getApproximateNextMoveTowardsTarget(
									ghostNodeIndex, game.getPacmanCurrentNodeIndex(), 
									ghostLastMov, DM.PATH );
							curMoves.remove(ghostType);
							curMoves.put( ghostType, mov );
						}
						//else take a random legal action (to be less predictable)
						else									
						{					
							MOVE[] possibleMoves = game.getPossibleMoves(ghostNodeIndex, 
									ghostLastMov);
							curMoves.remove(ghostType);
							curMoves.put(ghostType, 
									possibleMoves[random.nextInt(possibleMoves.length)]);
						}
					}
				}
			}
		
		////////////////////////////////////
		//////////Added by dearhude1////////
		////////////////////////////////////
		hasComputed = true;
		lastMove = curMoves;
		
		return curMoves;
	}
	
	
	protected void updateValueFunction_EpiEnded( Game curGame )
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
		//double lostScore = curGame.getLostScoreWhenEpEnded();
		double remainingScore = curGame.getRemainingScoreWhenEpEnded();
		if( levelTime > 0 ) {
			
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
		if( !lastMove.containsKey(rlGhost) || 
				!lastStrategies.containsKey(rlGhost) || 
				!lastGames.containsKey(rlGhost) )
			return;
		
		/**
		 * get the last action of this ghost
		 */
		MOVE ghostLastMov = lastMove.get( rlGhost );
		int ghostLastStrategy = lastStrategies.get( rlGhost );
		
		if( ghostLastMov != MOVE.NEUTRAL ) {
			
			/**
			 * get the last game state
			 */
			Game lastGame = lastGames.get( rlGhost );

			/**
			 * compute the reward for the (T-1) step
			 */
			double timeDuration = levelTime - 
					lastGame.getCurrentLevelTime();
			double rwd = fnlReward + ghostRewardMap.get(rlGhost) / timeDuration;
			ghostRewardMap.put( rlGhost,  0.0 );
			
			/**
			 * compute the TD error: r + maxQ(s',a') - Q(s,a)
			 */
			double Qsa = getQValue(lastGame, rlGhost, ghostLastStrategy);
			double Qsap = 0.0;
			double delta_t = rwd + GAMMA * Qsap - Qsa;
			
			/**
			 * get the feature matrix phi_t and phi_(t+1)
			 */
			int matSize = Constants.NUM_FEATURES_SINGLE_AGENT * Constants.NUM_GHOST_STRATEGIES_SINGLE_AGENT;
			Matrix phi_t = new Matrix(matSize, 1);
			Matrix phi_tPlusOne = new Matrix(matSize, 1);
			double[][] featureMatrix_t = computeFeatureValueMatrix(lastGame, 
					rlGhost, ghostLastStrategy);
			
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
			Matrix C_t = cMatrixMap.get(rlGhost);
			Matrix diffMatrix = phi_t.minus(phi_tPlusOne.times(GAMMA));
			double denominator = 1+diffMatrix.transpose().times(C_t).times(phi_t).get(0,0);
			Matrix thetaInc = C_t.times( phi_t ).times( delta_t / denominator );

			realFeatureIndex = 0;
			for( int s = 0; s < Constants.NUM_GHOST_STRATEGIES_SINGLE_AGENT; s++ ) {
				
				for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_SINGLE_AGENT; fIndex++ ) {
					
					if( s == ghostLastStrategy ) {
						
						ghostThetaMap.get(rlGhost)[s][fIndex] += 
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
			cMatrixMap.remove(rlGhost);
			cMatrixMap.put(rlGhost, C_t);

			
			//record the reward for the current step and increase the game step
			int ghostIndex = queryGhostIndex( rlGhost );
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
			if( lastGames.containsKey(rlGhost)) {
				
				lastGames.remove(rlGhost);
			}
		}
	}
	
    //This helper function checks if Ms Pac-Man is close to an available power pill
	private boolean closeToPower(Game game)
    {
    	int[] powerPills=game.getPowerPillIndices();
    	
    	for(int i=0;i<powerPills.length;i++)
    		if(game.isPowerPillStillAvailable(i) && game.getShortestPathDistance(powerPills[i],game.getPacmanCurrentNodeIndex())<PILL_PROXIMITY)
    			return true;

        return false;
    }
	
}
