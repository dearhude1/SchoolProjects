package pacman.controllers.learners.marl;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;

import drasys.or.mp.Constraint;
import drasys.or.mp.SizableProblemI;

import pacman.game.Constants;
import pacman.game.Game;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;

/**
 * the implementation of Q(lambda) with function approximation
 *
 */
public class CEQLambda extends MARLWithFA {

	public String valueFunctionFileName = "./valueFunction_CEQ_";
	
	
	/**
	 * parameters for eligibility trace
	 */
	protected double LAMBDA = 0.9;
	
	/**
	 * for eligibility trace
	 */
	private HashMap<GHOST, double[][]> ghostEligMap = 
			new HashMap<Constants.GHOST, double[][]>();
			
	/**
	 * this member means that whether 
	 * the Q-learning ghosts learn in the game 
	 * or act greedily according to a learnt value function
	 */
	protected boolean doesLearn = true;
	
	/**
	 * the last correlated equilibrium for each ghost
	 */
	protected EnumMap<GHOST, double[]> lastCE = 
			new EnumMap<Constants.GHOST, double[]>(GHOST.class);
	
	
	public CEQLambda( Game game, double lambda, boolean bLearn )
	{
		
		super(game);
		
		LAMBDA = lambda;
		doesLearn = bLearn;
		
		for( GHOST ghostType : GHOST.values() ) {
			
			if( !ghostEligMap.containsKey(ghostType) ) {
				
				double[][] ghostElig = new double[Constants.NUM_JOINT_STRATEGIES]
						[Constants.NUM_FEATURES_MULTI_AGENT];
				for( int jntStrategy = 0; jntStrategy < Constants.NUM_JOINT_STRATEGIES; jntStrategy++ )	
					for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
					
						ghostElig[jntStrategy][fIndex] = 0.0;
				}
				
				ghostEligMap.put(ghostType, ghostElig);	
			}
		}
	}
	
	public CEQLambda( Game game, double alpha, 
			double gamma, double epsilon, double lambda, 
			boolean bLearn )
	{
		super(game, alpha, gamma, epsilon);
		
		LAMBDA = lambda;
		doesLearn = bLearn;
		
		for( GHOST ghostType : GHOST.values() ) {
			
			if( !ghostEligMap.containsKey(ghostType) ) {
				
				double[][] ghostElig = new double[Constants.NUM_JOINT_STRATEGIES]
						[Constants.NUM_FEATURES_MULTI_AGENT];
				for( int jntStrategy = 0; jntStrategy < Constants.NUM_JOINT_STRATEGIES; jntStrategy++ )	
					for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
					
						ghostElig[jntStrategy][fIndex] = 0.0;
				}
				
				ghostEligMap.put(ghostType, ghostElig);	
			}
		}
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
				
				/**
				 * reset the eligibility traces to zeros
				 */
				for( int jntStrategy = 0; jntStrategy < Constants.NUM_JOINT_STRATEGIES; jntStrategy++ ) {
					
					for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
						
						for( GHOST ghostType : GHOST.values() ) {
							
							ghostEligMap.get(ghostType)[jntStrategy][fIndex] = 0.0;
						}
					}
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
	
		/**
		 * if there is no need to take an action
		 */
		boolean noNeed = true;
		for(GHOST ghostType : GHOST.values()) {
			
			if( game.doesGhostRequireAction(ghostType) ) {
				
				noNeed = false;
				break;
			}
		}
		if( noNeed )
			return curMoves;
		
		
    	/**
    	 * get the ghosts which can take an action currently
    	 * only these ghosts can play a game
    	 */
    	int numGamingGhosts = 0;
    	ArrayList<GHOST> gamingGhosts = new ArrayList<Constants.GHOST>();
    	for( GHOST ghostType : GHOST.values() ) {
    		
    		if( game.doesGhostRequireAction(ghostType) ) {
    			
    			gamingGhosts.add(ghostType);
    			numGamingGhosts++;
    		}
    	}
    	
    	/**
    	 * construct a game, compute an equilibrium 
		 * and choose an action
    	 * 
    	 * maybe we can generate the joint strategies 
    	 * of the gaming ghosts
    	 */
    	ArrayList<int[]> gamingJntStrategies = generateJointStrategies(game, 
    			gamingGhosts);
		//long t1 = System.nanoTime();
    	
    	System.out.println("Gaming Jnt Strategy Size "+gamingJntStrategies.size());
		
		double[] correlEquil = computeCE( game, gamingGhosts, gamingJntStrategies );
		
		//long t2 = System.nanoTime();
		
		//System.out.println("Time "+(t2-t1)/1000000);
	
    	
		//modify the correlated equilibrium?
		if( correlEquil == null ) {
			
			correlEquil = new double[gamingJntStrategies.size()];
			for( int listIndex = 0; listIndex < gamingJntStrategies.size(); listIndex++ ) {
				
				correlEquil[listIndex] = 1.0 / gamingJntStrategies.size();
			}
		}
		else {
			//System.out.println("Have CE");
		}
			
		int sampledJntAction = sampleCorrelEquil( correlEquil, gamingJntStrategies );
		int[] chosenStrategies = null;
		
		//if the correlated equilibrium is invalid
		//assign a random strategy to each ghost
		if( sampledJntAction == -1 ){
			
			chosenStrategies = new int[Constants.NUM_GHOSTS];
			for(GHOST ghostType : GHOST.values()) {
				
				int ghostIndex = queryGhostIndex(ghostType);
				if( !game.doesGhostRequireAction(ghostType) ) {
					
					chosenStrategies[ghostIndex] = Constants.GHOST_STRATEGY_BUSY;
				}
				else {
					
					ArrayList<Integer> possStrategies = 
							getPossibleStrategies( game, ghostType ); 
					int numPossStrategies = possStrategies.size();
					chosenStrategies[ghostIndex] = 
							possStrategies.get(random.nextInt(numPossStrategies));
				}
			}
		}
		else
			chosenStrategies = jntStrategy2Strategies(sampledJntAction);
		
		/**
		 * should be careful here
		 * this is not the actual joint strategy chosen
		 * the chosen joint strategy is obtain when all ghosts execute epsilon-greedy
		 */
		//int chosenJntStrategy = strategies2JntStrategy(strategies);
		
		for(GHOST ghostType : GHOST.values()) {
			
			if( !game.doesGhostRequireAction(ghostType) ) {

				continue;
			}
			
			int ghostIndex = queryGhostIndex(ghostType);
			
			/**
			 * do epsilon-greedy
			 */
			int computedStrategy = chosenStrategies[ghostIndex];
			int chosenStrategy = epsilonGreedy(game, ghostType, computedStrategy);
			
			//we should record the chosen strategy for the ghost
			chosenStrategies[ghostIndex] = chosenStrategy;
			
			/**
			 * choose the real move according to the strategy
			 */
			MOVE chosenMove = getMoveAccordingToStrategy(game, ghostType, chosenStrategy);
			
			
			/**
			 * set the move of the ghost
			 */
			curMoves.remove( ghostType );
			curMoves.put( ghostType, chosenMove );
			
			/**
			 * then update the value function??
			 * why not also use the current choosen strategy 
			 * to update the value function???
			 */
			if( doesLearn ) {
				
				updateValueFunctionCE( game, ghostType, 
						correlEquil, gamingJntStrategies );
			}
			
			/**
			 * store the last game state
			 */
			lastGames.put( ghostType, game );
			
			/**
			 * store the last CE
			 */
			if( lastCE.containsKey(ghostType) )
				lastCE.remove(ghostType);
			lastCE.put(ghostType, correlEquil);

		}
		

		/**
		 * record the executed joint strategy
		 */
		int chosenJntStrategy = strategies2JntStrategy(chosenStrategies);
		for(GHOST ghostType : GHOST.values()) {
			
			if( !game.doesGhostRequireAction(ghostType) )
				continue;

			if( lastJntStrategies.containsKey(ghostType) )
				lastJntStrategies.remove(ghostType);
			lastJntStrategies.put( ghostType, chosenJntStrategy );
			
		}
		
		////////////////////////////////////
		//////////Added by dearhude1////////
		////////////////////////////////////
		hasComputed = true;
		lastMove = curMoves;
		
		return curMoves;
	}

	
	//double check
	protected void updateValueFunctionCE( Game curGame, GHOST ghostType, 
			double[] curCorrelEquil, ArrayList<int[]> curGamingJntStrategies  )
	{

		//the last correlated equilibrium???
		if( lastMove == null || 
				!lastMove.containsKey(ghostType) || 
				!lastJntStrategies.containsKey(ghostType) || 
				!lastGames.containsKey(ghostType) )
			return;		
		
		/**
		 * get the last action of this ghost
		 */
		MOVE ghostLastMov = lastMove.get( ghostType );
		
		if( curGame.getGhostCurrentNodeIndex(ghostType) != 
				curGame.getGhostInitialNodeIndex() && 
				ghostLastMov != MOVE.NEUTRAL ) {
			
			/**
			 * get the last game state
			 */
			Game lastGame = lastGames.get( ghostType );

			
			/**
			 * get the last correlated equilibrium
			 */
			double[] lastCorrelEquil = lastCE.get(ghostType);
			
			
			/**
			 * get the last chosen joint strategy
			 */
			int lastJntStrategy = lastJntStrategies.get( ghostType );
			
			/**
			 * compute the reward in the last step
			 * compute the reward with respect to pills and power pills
			 * 
			 * we should reset the reward in the reward map to zero
			 */
			double rwd = ghostRewardMap.get(ghostType);
			double timeDuration = curGame.getCurrentLevelTime() - 
					lastGame.getCurrentLevelTime();
			rwd /= timeDuration;
			ghostRewardMap.put( ghostType, 0.0 );
			
			double[] featureValues = computeFeatureValues( lastGame );
			
			/**
			 * update the eligibilities 
			 * if the probability of the chosen joint strategy is positive in the correlated 
			 * equilibrium, then the eligibility can be traced
			 */
			
			//the index of the correlated equilibrium is not joint strategy, but list index
	    	ArrayList<GHOST> lastGamingGhosts = new ArrayList<Constants.GHOST>();
	    	for( GHOST ghType : GHOST.values() ) {
	    		
	    		if( lastGame.doesGhostRequireAction(ghType) ) {
	    			
	    			lastGamingGhosts.add(ghType);
	    		}
	    	}
			ArrayList<int[]> lastGamingJntStrategies = 
					generateJointStrategies(lastGame, lastGamingGhosts);
			boolean isSupported = true;
			int listIndex = -1;
			for( int lsIndex = 0; lsIndex < lastGamingJntStrategies.size(); lsIndex++ ) {
				
				int[] strategies = lastGamingJntStrategies.get(lsIndex);
				int jntStrategy = strategies2JntStrategy(strategies);
				if( jntStrategy == lastJntStrategy ) {
					
					listIndex = lsIndex;
					break;
				}
			}
			if( lastCorrelEquil == null || 
					!(lastCorrelEquil[listIndex] > 0) ) {
				
				isSupported = false;
			}
			
			for( int jntStrategy = 0; jntStrategy < Constants.NUM_JOINT_STRATEGIES; jntStrategy++ ) {
				
				if( isSupported ) {
					
					for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
						
						ghostEligMap.get(ghostType)[jntStrategy][fIndex] = 
								ghostEligMap.get(ghostType)[jntStrategy][fIndex] * GAMMA * LAMBDA;
						
						if( jntStrategy == lastJntStrategy ) {
							
							ghostEligMap.get(ghostType)[jntStrategy][fIndex] += 
									featureValues[fIndex];
						}
					}
				}
				else {
					
					for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) 
						ghostEligMap.get(ghostType)[jntStrategy][fIndex] = 0.0;
				}
			}
			
			/**
			 * then update the value function
			 * compute the TD error: r + maxQ(s',a') - Q(s,a)
			 */
			double Qsa = getJntQValue(lastGame, ghostType, lastJntStrategy);
			double CEQ = computeCEQValue(curGame, curCorrelEquil, 
					ghostType, curGamingJntStrategies);
			double delta = rwd + GAMMA * CEQ - Qsa;
			
			for( int jntStrategy = 0; jntStrategy < Constants.NUM_JOINT_STRATEGIES; jntStrategy++ ) {
				for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
				
					ghostThetaMap.get(ghostType)[jntStrategy][fIndex] += 
							ALPHA * delta * ghostEligMap.get(ghostType)[jntStrategy][fIndex];
				}
			}
			
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
	
	//double check
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
					!lastJntStrategies.containsKey(ghostType) || 
					!lastGames.containsKey(ghostType) )
				continue;
			
			/**
			 * get the last action of this ghost
			 */
			MOVE ghostLastMov = lastMove.get( ghostType );
			
			if( ghostLastMov != MOVE.NEUTRAL ) {
				
				/**
				 * get the last game state
				 */
				Game lastGame = lastGames.get( ghostType );

				/**
				 * get the last correlated equilibrium
				 */
				double[] lastCorrelEquil = lastCE.get(ghostType);
				
				/**
				 * get the last chosen joint strategy
				 */
				int lastJntStrategy = lastJntStrategies.get( ghostType );
				
				
				/**
				 * update the eligibilities 
				 * if the probability of the chosen joint strategy is positive in the correlated 
				 * equilibrium, then the eligibility can be traced
				 */
				double[] featureValues = computeFeatureValues( lastGame );
				
				//the index of the correlated equilibrium is not joint strategy, but list index
		    	ArrayList<GHOST> lastGamingGhosts = new ArrayList<Constants.GHOST>();
		    	for( GHOST ghType : GHOST.values() ) {
		    		
		    		if( lastGame.doesGhostRequireAction(ghType) ) {
		    			
		    			lastGamingGhosts.add(ghType);
		    		}
		    	}
				ArrayList<int[]> lastGamingJntStrategies = generateJointStrategies(lastGame, 
						lastGamingGhosts);
				boolean isSupported = true;
				int listIndex = -1;
				for( int lsIndex = 0; lsIndex < lastGamingJntStrategies.size(); lsIndex++ ) {
					
					int[] strategies = lastGamingJntStrategies.get(lsIndex);
					int jntStrategy = strategies2JntStrategy(strategies);
					if( jntStrategy == lastJntStrategy ) {
						
						listIndex = lsIndex;
						break;
					}
				}
				if( lastCorrelEquil == null || 
						!(lastCorrelEquil[listIndex] > 0) ) {
					
					isSupported = false;
				}
				
				for( int jntStrategy = 0; jntStrategy < Constants.NUM_JOINT_STRATEGIES; jntStrategy++ ) {
					
					if( isSupported ) {
						
						for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
							
							ghostEligMap.get(ghostType)[jntStrategy][fIndex] = 
									ghostEligMap.get(ghostType)[jntStrategy][fIndex] * GAMMA * LAMBDA;
							
							if( jntStrategy == lastJntStrategy ) {
								
								ghostEligMap.get(ghostType)[jntStrategy][fIndex] += 
										featureValues[fIndex];
							}
						}
					}
					else {
						
						for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) 
							ghostEligMap.get(ghostType)[jntStrategy][fIndex] = 0.0;
					}
				}
				
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
				double Qsa = getJntQValue(lastGame, ghostType, lastJntStrategy);
				double Qsap = 0.0;
				double delta = rwd + GAMMA * Qsap - Qsa;

				for( int jntStrategy = 0; jntStrategy < Constants.NUM_JOINT_STRATEGIES; jntStrategy++ ) {
					for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
					
						ghostThetaMap.get(ghostType)[jntStrategy][fIndex] += 
								ALPHA * delta * ghostEligMap.get(ghostType)[jntStrategy][fIndex];
					}
				}
				
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
	
	
	protected double[] computeCE( Game game, ArrayList<GHOST> gamingGhosts, 
			ArrayList<int[]> gamingJointStrategies )
	{
		
		return null;
	}
	
	/**
	 * set the constraint of correlated equilibrium
	 */
	protected void setConstraintCE_Cplex( IloCplex ceLP, 
			 IloNumVar[] p, Game game, ArrayList<GHOST> gamingGhosts, 
			 ArrayList<int[]> gamingJointStrategies ) throws Exception
	{
		
		int numGameGhosts = gamingGhosts.size();
		int varNum = gamingJointStrategies.size();
		//int varNum = Constants.NUM_JOINT_STRATEGIES;
		
		/**
		 * the constraints for the impossible joint strategies
		 * 
		 * generate joint strategies of all gaming ghosts
		 */
		//ArrayList<int[]> allJntStrategies = generateJointStrategies(game, gamingGhosts);
		//int numJntStrategies = gamingJointStrategies.size();
		
		//System.out.println("VarNum: "+varNum);
		
		/**
		 * for impossible joint strategies
		 */
		for( int listIndex = 0; listIndex < varNum; listIndex++ ) {
			
			int[] strategies = gamingJointStrategies.get(listIndex);
			int jntStrategy = strategies2JntStrategy(strategies);
			
			if( !isJntStrategPossible(game, jntStrategy) ) {
				
				double[] coeff = new double[varNum];
				for( int lsIndex = 0; lsIndex < varNum; lsIndex++ ) {
					
					if( lsIndex == listIndex )
						coeff[lsIndex] = 1.0;
					else
						coeff[lsIndex] = 0.0;
				}
				IloNumExpr expr = ceLP.scalProd( coeff, p );
				ceLP.addEq( expr, 0.0 );
			}
		}
		
		/**
		 * for the inequality of correlated equilibrium
		 */
		for( GHOST ghostType : GHOST.values() ) {
			
			if( !game.doesGhostRequireAction(ghostType) )
				continue;
			
			int ghostIndex = queryGhostIndex(ghostType);
			
			//loop for strategy s
			for( int s = 0; s < Constants.NUM_GHOST_STRATEGIES_MULTI_AGENT - 1; s++ ) {
				
				//if strateg s is not possible for the ghost
				//then all corresponding joint strategies have a 0 probability
				//which has been set above
				if( !isStrategyPossible(game, ghostType, s) )
					continue;
				
				//loop for strategy s'
				for( int sp = 0; sp < Constants.NUM_GHOST_STRATEGIES_MULTI_AGENT - 1; sp++ ) {
					
					if( s == sp || 
							!isStrategyPossible(game, ghostType, sp) )
						continue;
					
				    //the inequality expression
				    IloNumExpr ineqExpr = ceLP.constant(0.0);
				    
				    //set the coefficient
				    ArrayList<int[]> otherJntStrategies = 
				    		generateOtherJointStrategies(game, ghostType, gamingGhosts);
				    
				    for( int listIndex = 0; listIndex < otherJntStrategies.size(); listIndex++ ) {
				    	
				    	/**
				    	 * note that if the ghost cannot take an action in the current state 
				    	 * then its Qvalue is 0
				    	 */
				    	int[] strategies = otherJntStrategies.get( listIndex );
				    	strategies[ghostIndex] = s;
				    	int jntStrategy_s = strategies2JntStrategy(strategies);
				    	double Qsa = getJntQValue(game, ghostType, jntStrategy_s);

				    	
				    	strategies[ghostIndex] = sp;
				    	int jntStrategy_sp = strategies2JntStrategy(strategies);
				    	double Qsap = getJntQValue(game, ghostType, jntStrategy_sp);
				    	double coeff = Qsa - Qsap;
				    	
				    	/**
				    	 * here watch out 
				    	 * the index of the variable
				    	 */
				    	int varIndex = -1;
				    	
				    	for( int lsIndex = 0; lsIndex < varNum; lsIndex++ ) {
				    		
				    		int jntS = strategies2JntStrategy(gamingJointStrategies.get(lsIndex));
				    		if( jntStrategy_s == jntS ) {
				    			
				    			varIndex = lsIndex;
				    			break;
				    		}
				    	}
				    	
				    	if( varIndex == -1 ) {
				    		
				    		System.out.println("VarIndex -1");
				    		System.out.println("Gaming Ghosts: "+gamingGhosts.size());
					    	System.out.println("VarNum: "+varNum+" ArraySize: "+gamingJointStrategies.size());
				    		for( int lsIndex = 0; lsIndex < varNum; lsIndex++ ) {
				    			
				    			int[] ss =gamingJointStrategies.get(lsIndex);
				    			for( int i = 0; i < Constants.NUM_GHOSTS; i++ )
				    				System.out.print(" "+ss[i]);
				    			System.out.println("------");
				    			
				    			//int js = strategies2JntStrategy(ss);
				    			//System.out.print(" "+js);
				    		}
				    		System.out.println("========");
				    		for( int lsIndex = 0; lsIndex < otherJntStrategies.size(); lsIndex++ ) {
				    			
				    			int[] ss = otherJntStrategies.get(lsIndex);
				    			ss[ghostIndex] = s;
				    			for( int i = 0; i < Constants.NUM_GHOSTS; i++ )
				    				System.out.print(" "+ss[i]);
				    			System.out.println("------");
				    			
				    			//int js = strategies2JntStrategy(ss);
				    			//System.out.print(" "+js);
				    		}
				    		System.out.println("========");
				    		
				    	}
				    	
				    	IloNumExpr itemExpr = ceLP.prod( coeff, p[varIndex] );
				    	ineqExpr = ceLP.sum( ineqExpr, itemExpr );
				    }
				    
				    ceLP.addGe( ineqExpr, 0.0 );
				}
			}
		}
		
		
		//System.out.println("Why not here??");
		
		/**
		 * the constraint that the sum of 
		 * all probabilities of the joint strategies is 1
		 */
		IloNumExpr sumExpr = ceLP.constant(0.0);
		for( int lsIndex = 0; lsIndex < varNum; lsIndex++ ) {
			
			sumExpr = ceLP.sum( sumExpr, p[lsIndex] );
		}
		ceLP.addEq( sumExpr, 1.0 );
	}
	
	
	
	/**
	 * compute the value of a correlated equilibrium for all ghosts
	 */
	//check
	protected double computeCEQValue( Game game, double[] correlEquil, 
			GHOST ghostType, ArrayList<int[]> gamingJntStrategies )
	{
		if( correlEquil == null ) {
			
			//System.out.println("CEQGhostsWithFA->computeCEQValues: NULL CE");
			return 0;
		}
		/**
		else if( correlEquil.length != Constants.NUM_JOINT_STRATEGIES ) {
			
			System.out.println("CEQGhostsWithFA->computeCEQValues: Wrong Length");
			return 0;
		}
		*/
		
		double retValue = 0.0;;
    	
    	
		if( correlEquil.length != gamingJntStrategies.size() ) {
			
			System.out.println("CEQGhostsWithFA->computeCEQValues: Wrong Length "+ 
					correlEquil.length+" "+gamingJntStrategies.size() );
			return 0;
		}
    	
		for( int listIndex = 0; listIndex < gamingJntStrategies.size(); listIndex++ ) {
			
			int[] strategies = gamingJntStrategies.get(listIndex);
			int jntStrategy = strategies2JntStrategy(strategies);
			double value = getJntQValue(game, ghostType, jntStrategy);
			retValue += value * correlEquil[listIndex];
		}
		
		return retValue;
	}
	
	/**
	 * sample a joint strategy according to a correlated equilibrium
	 */
	protected int sampleCorrelEquil( double[] correlEquil, 
			ArrayList<int[]> gamingJntStrategies )
	{

		if( correlEquil == null ) {
			
			return -1;
		}
		else if( gamingJntStrategies == null || 
				gamingJntStrategies.size() <= 0 ) {
			
			return -1;
		}
		else if( correlEquil.length != gamingJntStrategies.size() ) {
			
			System.out.println("CEQGhostsWithFA->sampleCorrelEquil: Wrong Length");
			
			//return random.nextInt( numJntStrategies );
			
			return -1;
		}
		
		int retJntStrategy = -1;
	    double proSum = 0.0;
	    double samplePro = random.nextDouble();
	    
	    for( int listIndex = 0; listIndex < gamingJntStrategies.size(); listIndex++ ) {
			
	    	proSum += correlEquil[listIndex];
		
	    	if( samplePro <= proSum ) {
		    
	    		int[] strategies = gamingJntStrategies.get(listIndex);
	    		int jntStrategy = strategies2JntStrategy(strategies);
	    		retJntStrategy = jntStrategy;
	    		break;
	    	}
	    }
	    
	    return retJntStrategy;
	}
	
}
