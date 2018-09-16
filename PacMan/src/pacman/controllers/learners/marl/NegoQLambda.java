package pacman.controllers.learners.marl;

import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Random;
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
public class NegoQLambda extends MARLWithFA {

	public String valueFunctionFileName = "./valueFunction_NegoQLambda_";
	
	
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
	 * the last pure equilibrium for each ghost
	 */
	protected EnumMap<GHOST, Integer> lastEquilibria = 
			new EnumMap<Constants.GHOST, Integer>(GHOST.class);
	
	
	/**
	 * store all pure strategy Nash equilibria 
	 * in a certain state
	 */
	public ArrayList<Integer> nashEquilActions;
		    
	/**
	 * store all NSEDAs in a certain state
	 */	   
	public ArrayList<Integer> nsedaActions;
		    
	public ArrayList<Integer> symmMetaEquilActions;
		    
	/**
	 * store some meta equilibria in a certain state
	 */
	public ArrayList<Integer> metaEquilActions;
			
			
	public NegoQLambda( Game game, double lambda, boolean bLearn )
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
		
		nashEquilActions = new ArrayList<Integer>();
		nsedaActions = new ArrayList<Integer>();
		symmMetaEquilActions = new ArrayList<Integer>();
		metaEquilActions = new ArrayList<Integer>();
		
		/**
		 * read the value function
		 */
		//if( !doesLearn )
			//readValueFunction( valueFunctionFileName );
	}
	
	public NegoQLambda( Game game, double alpha, 
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
		
		nashEquilActions = new ArrayList<Integer>();
		nsedaActions = new ArrayList<Integer>();
		symmMetaEquilActions = new ArrayList<Integer>();
		metaEquilActions = new ArrayList<Integer>();
		
		/**
		 * read the value function
		 */
		//if( !doesLearn )
			//readValueFunction( valueFunctionFileName );
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
				
				/**
				for( int ghostIndex = 0; ghostIndex < 4; ghostIndex++ ) {
					
					if( gameSteps[ghostIndex] > 0 ) 
						arpsPerEpisode[ghostIndex][currentEp] /= gameSteps[ghostIndex];
					else 
						arpsPerEpisode[ghostIndex][currentEp] = 0.0;
					
					gameSteps[ghostIndex] = 0;	
				}
				*/
				
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
		 * construct a game, compute an equilibrium 
		 * according to NegoQ algorithm
		 */	
		int optJntStrategy = negotiation( game );

		int[] chosenStrategies = jntStrategy2Strategies( optJntStrategy );
		
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
				
				updateValueFunction( game, ghostType, optJntStrategy );
			}
			
			/**
			 * store the last game state
			 */
			lastGames.put( ghostType, game );
			
			/**
			 * store the last CE
			 */
			if( lastEquilibria.containsKey(ghostType) )
				lastEquilibria.remove(ghostType);
			lastEquilibria.put(ghostType, optJntStrategy);
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
	protected void updateValueFunction( Game curGame, GHOST ghostType, 
			int curOptJntStrategy )
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
			int lastEquil = lastEquilibria.get(ghostType);
			
			
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
			for( int jntStrategy = 0; jntStrategy < Constants.NUM_JOINT_STRATEGIES; jntStrategy++ ) {
				
				if( lastJntStrategy == lastEquil ) {
					
					for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
						
						ghostEligMap.get(ghostType)[jntStrategy][fIndex] = 
								ghostEligMap.get(ghostType)[jntStrategy][fIndex] * GAMMA * LAMBDA;
						
						if( jntStrategy == lastJntStrategy ) {
							
							ghostEligMap.get(ghostType)[jntStrategy][fIndex] += 
									featureValues[fIndex];
						}
					}
					
					//System.out.println("Eglibility Trace!!!");
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
			double Qequil = getJntQValue(curGame, ghostType, curOptJntStrategy);
			double delta = rwd + GAMMA * Qequil - Qsa;
			
			for( int jntStrategy = 0; jntStrategy < Constants.NUM_JOINT_STRATEGIES; jntStrategy++ ) {
				for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
				
					ghostThetaMap.get(ghostType)[jntStrategy][fIndex] += 
							ALPHA * delta * ghostEligMap.get(ghostType)[jntStrategy][fIndex];
				}
			}
			
			/**
			 * record the reward for the current step 
			 * and increase the game step
			 *
			int ghostIndex = queryGhostIndex( ghostType );
			int episode = game.getCurrentEpisode();
			arpsPerEpisode[ghostIndex][episode] += rwd;
			gameSteps[ghostIndex] += 1;
			*/
			
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
				int lastEquil = lastEquilibria.get(ghostType);
				
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
				for( int jntStrategy = 0; jntStrategy < Constants.NUM_JOINT_STRATEGIES; jntStrategy++ ) {
					
					if( lastJntStrategy == lastEquil ) {
						
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
				/**
				int ghostIndex = queryGhostIndex( ghostType );
				int episode = game.getCurrentEpisode();
				arpsPerEpisode[ghostIndex][episode] += fnlReward;
				gameSteps[ghostIndex] += 1;
				*/
				
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
	
	
	//double check
    public int negotiation( Game game )
    {
		
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
    	 * negotiation for pure strategy Nash equilibria
    	 */
    	EnumMap<GHOST, ArrayList<Integer>> maxSets = 
    			new EnumMap<Constants.GHOST, ArrayList<Integer>>(GHOST.class);
    	for( GHOST ghostType : GHOST.values() ) {
	    
    		if( !game.doesGhostRequireAction(ghostType) )
    			continue;
    		
    		ArrayList<Integer> maxStrategies = getMaxSet( game, ghostType, 
    				gamingGhosts );

    		maxSets.put( ghostType, maxStrategies );
    	}
    	/**
    	 * we can let only one agent compute NE
    	 * and then tell the others
    	 */
    	findNEs( maxSets );

	
    	/**
    	 * if there exist Nash equilibira 
    	 * then find NSEDAs,
    	 * or find meta equilibria
    	 */
    	if( nashEquilActions.size() > 0 ) {
    		
    		EnumMap<GHOST, ArrayList<Integer>> partDmSets = 
    				new EnumMap<Constants.GHOST, ArrayList<Integer>>(GHOST.class);
    		
    		for( GHOST ghostType : GHOST.values() ) {
    			
        		if( !game.doesGhostRequireAction(ghostType) )
        			continue;
        		
        		ArrayList<Integer> partDmSet = 
        				getPartiallyDominatingSet( game, ghostType, gamingGhosts );
        		
        		partDmSets.put( ghostType, partDmSet );
    		}

    		findNSEDAs( partDmSets );
    	}
    	else {
	    
    		/**
    		 * first find symmetric meta equilibria
    		 */
    		EnumMap<GHOST, ArrayList<Integer>> possSymmSets = 
    				new EnumMap<Constants.GHOST, ArrayList<Integer>>(GHOST.class);
    		for( GHOST ghostType : GHOST.values() ) {
	    
        		if( !game.doesGhostRequireAction(ghostType) )
        			continue;
        		
        		ArrayList<Integer> possSymmSet = 
        				getPossibleSymmEquilSet( game, ghostType, gamingGhosts );
        		
        		possSymmSets.put(ghostType, possSymmSet);
    		}
    		findSymmEquils( possSymmSets );

    		/**
    		 * find a meta equilibrium
    		 */
    		if( symmMetaEquilActions.size() == 0 ) {
		
    			/**
    			 * we only find meta equilibrium from complete games
    			 */
    			ArrayList<String> indices = new ArrayList<String>();
    			for( int index = 0; index < gamingGhosts.size(); index++ ) {
    				
    				GHOST gamingGhost = gamingGhosts.get(index);
    				int gamingGhostIndex = queryGhostIndex(gamingGhost);
    				indices.add(String.valueOf(gamingGhostIndex));
    			}
    			String[] prefix = new String[numGamingGhosts];
    			Random rnd = new Random();
    			for( int index = 0; index < numGamingGhosts; index++ ) {
			    
    				prefix[index] = indices.remove( rnd.nextInt(indices.size()) );
    			}
		    
    			
    			/**
    			 * then find the set of actions which may be a meta equilibrium 
    			 * and find the intersection
    			 */
    			EnumMap<GHOST, ArrayList<Integer>> possMetaSets = 
    					new EnumMap<Constants.GHOST, ArrayList<Integer>>(GHOST.class);
    			for( GHOST ghostType : GHOST.values() ) {
    			    
    				if( !game.doesGhostRequireAction(ghostType) ) 
    					continue;
    				
        			ArrayList<Integer> possMetaSet = 
        					getPossibleMetaEquil(game, prefix, ghostType, gamingGhosts);
        			
        			possMetaSets.put(ghostType, possMetaSet);
    			}
    			findMetaEquils( possMetaSets );
    		}
    	}
	
    	/**
    	 * then choose one optimal action
    	 */
    	int[] favorJntStrategies = findFavorJntStrategies(game);
    	if( favorJntStrategies == null ) 
    		System.out.println("Gaming ghost "+numGamingGhosts);
	
    	Random rnd = new Random(); 
    	int chosenJntStrategy = favorJntStrategies[rnd.nextInt(Constants.NUM_GHOSTS)];
	
    	return chosenJntStrategy;
	
    }
    
    
    /**
     * compute the max set in a state and return 
     * negotiation for pure strategy Nash equilibria
     */
    public ArrayList<Integer> getMaxSet( Game game, GHOST ghostType, 
    		ArrayList<GHOST> gamingGhosts )
    {

    	ArrayList<Integer> retList = new ArrayList<Integer>();
	
    	/**
    	 * find max action according to the current Q-table
    	 */
    	int ghostIndex = queryGhostIndex(ghostType);
    	
    	/**
    	 * note that all other joint actions should be possible
    	 */
    	ArrayList<int[]> othJntActions = generateOtherJointStrategies( game, 
    			ghostType, gamingGhosts );
    	
    	
    	for( int listIndex = 0; listIndex < othJntActions.size(); listIndex++ ) {
	    
    		int[] strategies = othJntActions.get(listIndex); 
    		double maxValue = Double.NEGATIVE_INFINITY;
	    
    		//find the max value
    		for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES_MULTI_AGENT; strategy++ ) {
		
    			// we should not consider STRATEGY BUSY for acting ghosts
    			if( !isStrategyPossible(game, ghostType, strategy) ||
    					strategy == Constants.GHOST_STRATEGY_BUSY )
    				continue;
    			
    			strategies[ghostIndex] = strategy;
    			int jntStrategy = strategies2JntStrategy(strategies);
    			double jntQ = getJntQValue(game, ghostType, jntStrategy);
    			
    			if( jntQ > maxValue ) {
			    
    				maxValue = jntQ;
    			}
    		}
	    
    		//find all max actions
    		for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES_MULTI_AGENT; strategy++ ) {
		
    			if( !isStrategyPossible(game, ghostType, strategy) || 
    					strategy == Constants.GHOST_STRATEGY_BUSY )
    				continue;
    			
    			strategies[ghostIndex] = strategy;
    			int jntStrategy = strategies2JntStrategy(strategies);
    			double value = getJntQValue(game, ghostType, jntStrategy);
    			if( Math.abs(value-maxValue) < 0.00001 ) {
		    
    				if( !retList.contains( jntStrategy ) )
    					retList.add( jntStrategy );
    			}
    		}
    	}
    	
    	//for debug
    	/**
    	if( gamingGhosts.size() == 1 ) {
    		
    		for( int index = 0; index < othJntActions.size(); index++ ) {
    			
    			int[] strategies = othJntActions.get(index);
    			for( int gIndex = 0; gIndex < Constants.NUM_GHOSTS; gIndex++ ) {
    				
    				System.out.print(" "+strategies[gIndex]);
    			}
    			System.out.println();
    		}
    	}
    	*/
    	
    	if( gamingGhosts.size() == 1 && retList.size() == 0 ) {
    	
        	for( int listIndex = 0; listIndex < othJntActions.size(); listIndex++ ) {
        	    
        		int[] strategies = othJntActions.get(listIndex); 
        		double maxValue = Double.NEGATIVE_INFINITY;
    	    
        		//find the max value
        		for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES_MULTI_AGENT; strategy++ ) {
    		
        			// we should not consider STRATEGY BUSY for acting ghosts
        			if( !isStrategyPossible(game, ghostType, strategy) ||
        					strategy == Constants.GHOST_STRATEGY_BUSY )
        				continue;
        			
        			strategies[ghostIndex] = strategy;
        			int jntStrategy = strategies2JntStrategy(strategies);
        			double jntQ = getJntQValue(game, ghostType, jntStrategy);
        			
        			System.out.print("("+jntStrategy+","+jntQ+") ");
        		}
        		System.out.println();
        	}
    	}
    	
    	return retList;
    }
    
    
    public void findNEs( EnumMap<GHOST,ArrayList<Integer>> maxSets ) 
    {
    	if( maxSets == null ) {
	    
    		System.out.println("@NegoQLambdaGhostsWithFA->findNEs: NULL Parameters!");
    		return;
    	}
	
    	GHOST firstGhost = null;
    	ArrayList<Integer> firstMaxSet = null; 
    	for( GHOST ghostType : GHOST.values() ) {
    		
    		if( maxSets.containsKey(ghostType) ) {
    			
    			firstGhost = ghostType;
    			firstMaxSet = maxSets.get(ghostType);
    			break;
    		}
    	}
    	
		for( int listIndex = 0; listIndex < firstMaxSet.size(); listIndex++ ) {
	    	
			int jntStrategy = firstMaxSet.get(listIndex);
			
    		/*
    		 * if it is already known that it is NE
    		 */
    		if( nashEquilActions.contains( jntStrategy ) )
    			continue;
    		
    		boolean isNE = true;
			for( GHOST ghostType : GHOST.values() ) {
	        	
	    		if( ghostType == firstGhost || 
	    				!maxSets.containsKey(ghostType) )
	    			continue;


	    		ArrayList<Integer> othMaxSet = maxSets.get(ghostType);
	    		if( !othMaxSet.contains(jntStrategy) ) {
	    			
	    			isNE = false;
	    			break;
	    		}
	    	}
			
    		if( isNE ) {
    			
    			if( !nashEquilActions.contains( jntStrategy ) )
    				nashEquilActions.add( jntStrategy );
    		}
		}
		
		if(maxSets.size() == 1 && nashEquilActions.size() == 0) {
			
			System.out.println("ONE GHOST BUT DO NOT FIND THE NE "+firstMaxSet.size());
			for( int i = 0; i < firstMaxSet.size(); i++ ) {
				
				System.out.print(firstMaxSet.get(i)+" ");
				
			}
			System.out.println();
			
		}
		/**
		else if( maxSets.size() == 1 ) {
			
			System.out.println("CAN FIND MAX ACTION: ");
			for( int i = 0; i < nashEquilActions.size(); i++ ) {
				
				System.out.print(nashEquilActions.get(i)+" ");
			}
			System.out.println();
		}
		*/
    }
	
    /**
     * compute the partially dominating set for finding NSEDAs
     */
    public ArrayList<Integer> getPartiallyDominatingSet( Game game, GHOST ghostType, 
    		ArrayList<GHOST> gamingGhosts )
    {
	
    	int ghostIndex = queryGhostIndex(ghostType);
    	ArrayList<Integer> retList = new ArrayList<Integer>();
	
    	/**
    	 * no nash equilibria
    	 */
    	if( nashEquilActions.size() == 0 ) {
	    
    		System.out.println("getPartiallyDominatingSet: No Nash Equilibria!");
    		return retList;
    	}
	
    	/**
    	 * generate all possible joint actions
    	 */
    	for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES_MULTI_AGENT; strategy++ ) {
    		
    		if( !isStrategyPossible(game, ghostType, strategy) || 
    				strategy == Constants.GHOST_STRATEGY_BUSY )
    			continue;
    		
        	ArrayList<int[]> othJntStrategies = generateOtherJointStrategies( game, 
        			ghostType, gamingGhosts );
        	for( int index = 0; index < othJntStrategies.size(); index++ ) {
        		
        		int[] strategies = othJntStrategies.get(index);
        		strategies[ghostIndex] = strategy;
        		int jntStrategy = strategies2JntStrategy(strategies);
        		
        		if( nashEquilActions.contains( jntStrategy ) )
        			continue;
        		
        		double jntQValue = getJntQValue(game, ghostType, jntStrategy);
        		for( int neIndex = 0; neIndex < nashEquilActions.size(); neIndex++ ) {
        	    	
        			int pne = nashEquilActions.get(neIndex);
        			double neQvalue = getJntQValue(game, ghostType, pne);
    			
        			if( jntQValue >= neQvalue ) {
    			    
        				if( !retList.contains( jntStrategy ) )
        					retList.add( jntStrategy );
    			    
        				break;
        			}
        		}
        	}
    	}
    		
    	return retList;
    }
    
    public void findNSEDAs( EnumMap<GHOST, ArrayList<Integer>> partDmSets )
    {
    	if( partDmSets == null ) {
	 
    		System.out.println("@NegoQ->findNSEDAs: NULL Parameters!");
    		return;
    	}
	
    	GHOST firstGhost = null;
    	ArrayList<Integer> firstPartDmSet = null; 
    	for( GHOST ghostType : GHOST.values() ) {
    		
    		if( partDmSets.containsKey(ghostType) ) {
    			
    			firstGhost = ghostType;
    			firstPartDmSet = partDmSets.get(ghostType);
    			break;
    		}
    	}
    	
    	for( int listIndex = 0; listIndex < firstPartDmSet.size(); listIndex++ ) {
    		
    		int jntStrategy = firstPartDmSet.get(listIndex);
    		
    		if( nsedaActions.contains( jntStrategy ) )
    			continue;
    		    		
    		boolean isNSEDA = true;
			for( GHOST ghostType : GHOST.values() ) {
	        	
	    		if( ghostType == firstGhost || 
	    				!partDmSets.containsKey(ghostType) )
	    			continue;


	    		ArrayList<Integer> othPartDmSet = partDmSets.get(ghostType);
	    		if( !othPartDmSet.contains(jntStrategy) ) {
	    			
	    			isNSEDA = false;
	    			break;
	    		}
	    	}
					    
    		if( isNSEDA ) {
		
    			if( !nsedaActions.contains( jntStrategy ) )
    				nsedaActions.add( jntStrategy );
    		}
    	}
    }
    
    public ArrayList<Integer> getPossibleSymmEquilSet( Game game, GHOST ghostType, 
    		ArrayList<GHOST> gamingGhosts )
    {
    	
    	int numGamingGhosts = gamingGhosts.size();
    	String[] prefix = new String[numGamingGhosts];
    	int ghostIndex = queryGhostIndex(ghostType);
    	int arrayIndex = 0;
    	for( int index = 0; index < numGamingGhosts; index++ ) {
    		
    		GHOST ghost = gamingGhosts.get(index);
    		if( ghost == ghostType )
    			continue;
    		
    		int ghIndex = queryGhostIndex(ghost);
    		prefix[arrayIndex] = String.valueOf(ghIndex);
    		arrayIndex++;
    	}
    	prefix[numGamingGhosts-1] = String.valueOf(ghostIndex);
	
    	return getPossibleMetaEquil( game, prefix, ghostType, gamingGhosts );
    }
    
    
    public void findSymmEquils( EnumMap<GHOST, ArrayList<Integer>>  possSymmSets )
    {
    	if( possSymmSets == null ) {
	    
    		System.out.println("@NegoQ->findSymmEquils: NULL Parameters!");
    		return;
    	}
	
    	GHOST firstGhost = null;
    	ArrayList<Integer> firstSymmSet = null; 
    	for( GHOST ghostType : GHOST.values() ) {
    		
    		if( possSymmSets.containsKey(ghostType) ) {
    			
    			firstGhost = ghostType;
    			firstSymmSet = possSymmSets.get(ghostType);
    			break;
    		}
    	}
    	    	
    	for( int listIndex = 0; listIndex < firstSymmSet.size(); listIndex++ ) {
    		
    		int jntStrategy = firstSymmSet.get(listIndex);
    		
       		/*
    		 * if it is already known that it is symmetric equilibrium
    		 */
    		if( symmMetaEquilActions.contains( jntStrategy ) )
    			continue;
    		
    		boolean isSymmEq = true;
    		for( GHOST ghostType : GHOST.values() ) {
    			
	    		if( ghostType == firstGhost || 
	    				!possSymmSets.containsKey(ghostType) )
	    			continue;

	    		ArrayList<Integer> othPossSymmSet = possSymmSets.get(ghostType);
	    		if( !othPossSymmSet.contains(jntStrategy) ) {
	    			
	    			isSymmEq = false;
	    			break;
	    		}
    		}
	    
    		if( isSymmEq ) {
		
    			if( !symmMetaEquilActions.contains( jntStrategy ) )
    				symmMetaEquilActions.add( jntStrategy );
    		}
    	}
    }
    
    
    /**
     * compute the set of possible meta equilibria 
     * according to this agent's Q-table
     */
    public ArrayList<Integer> getPossibleMetaEquil( Game game, 
    		String[] prefix, GHOST ghostType, ArrayList<GHOST> gamingGhosts )
    {
    	if( prefix == null ) {
	    
    		System.out.println("NegoQLambdaGhostsWithFA->getPossibleMetaEquil: NULL Parameters!");
    		return null;
    	}
    	else if( prefix.length != gamingGhosts.size() ) {
    		
    		System.out.println("NegoQLambdaGhostsWithFA->getPossibleMetaEquil: Wrong Prefix!");
    		return null;
    	}
    	else if( gamingGhosts == null || 
    			gamingGhosts.size() == 0 ) {
    		
    		System.out.println("NegoQLambdaGhostsWithFA->getPossibleMetaEquil: No Gaming Ghosts!");
    		return null;
    	}
	
    	
    	int ghostIndex = queryGhostIndex(ghostType);
    	ArrayList<Integer> retList = new ArrayList<Integer>();
	
	
    	ArrayList<Integer> leftGhosts = new ArrayList<Integer>();
    	ArrayList<Integer> rightGhosts =  new ArrayList<Integer>();
    	for( int stringIndex = 0; stringIndex < prefix.length; stringIndex++ ) {
	    
    		String curString = prefix[stringIndex];
    		leftGhosts.add( Integer.parseInt( curString ) );
    	}
	
    	ArrayList<Integer> outcomeJntStrategies = null;
    	while( !leftGhosts.isEmpty() ) {
	    
    		int curGhostIndex = leftGhosts.remove( leftGhosts.size() - 1 );
	    
    		if( curGhostIndex == ghostIndex ) {
		
    			outcomeJntStrategies = findMax( game, ghostType, 
    					outcomeJntStrategies, leftGhosts, rightGhosts, 
    					curGhostIndex );
    		}
    		else {
		
    			outcomeJntStrategies = findMin( game, ghostType, 
    					outcomeJntStrategies, leftGhosts, rightGhosts, 
    					curGhostIndex );
    		}
	    
    		rightGhosts.add( curGhostIndex );
    	}
	
    	if( outcomeJntStrategies.size() == 1 ) {
	    
    		int threJntStrategy = outcomeJntStrategies.remove( 0 );
    		double threValue = getJntQValue(game, ghostType, threJntStrategy);
	    
    		/**
    		 * all joint actions of the gaming ghosts
    		 */
        	for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES_MULTI_AGENT; strategy++ ) {
        		
        		if( !isStrategyPossible(game, ghostType, strategy) || 
        				strategy == Constants.GHOST_STRATEGY_BUSY )
        			continue;
        		
            	ArrayList<int[]> othJntStrategies = generateOtherJointStrategies( game, 
            			ghostType, gamingGhosts );
            	for( int index = 0; index < othJntStrategies.size(); index++ ) {
            		
            		int[] strategies = othJntStrategies.get(index);
            		strategies[ghostIndex] = strategy;
            		int jntStrategy = strategies2JntStrategy(strategies);
            		
        			double jntQvalue = getJntQValue(game, ghostType, jntStrategy);
        			
        			if( jntQvalue >= threValue ) {
    		    
        				if( !retList.contains( jntStrategy ) ) {
        				
        					retList.add( jntStrategy );
        				}
        			}
            	}
        	}
    		
    		//System.out.println("Threshold Value: "+threValue);
    	}
    	else {
	    
    		System.out.println("NegoQLambdaGhostsWithFA->getPossibleMetaEquil: Cannot find a threhold value");
    	}
	
    	return retList;
    }
    
    
    public void findMetaEquils( EnumMap<GHOST, ArrayList<Integer>> possMetaSets )
    {
    	if( possMetaSets == null ) {
	    
    		System.out.println("@NegoQ->findMetaEquils: NULL Parameters!");
    		return;
    	}
	
    	GHOST firstGhost = null;
    	ArrayList<Integer> firstPossMetaSet = null; 
    	for( GHOST ghostType : GHOST.values() ) {
    		
    		if( possMetaSets.containsKey(ghostType) ) {
    			
    			firstGhost = ghostType;
    			firstPossMetaSet = possMetaSets.get(ghostType);
    			break;
    		}
    	}
	
    	for( int listIndex = 0; listIndex < firstPossMetaSet.size(); listIndex++ ) {
	    
    		int jntStrategy = firstPossMetaSet.get(listIndex);
	    
    		/*
    		 * if it is already known that it is Meta equilibrium
    		 */
    		if( metaEquilActions.contains( jntStrategy ) )
    			continue;
	    
    		boolean isMeta = true;
    		for( GHOST ghostType : GHOST.values() ) {
    			
	    		if( ghostType == firstGhost || 
	    				!possMetaSets.containsKey(ghostType) )
	    			continue;

	    		ArrayList<Integer> othPossMetaSet = possMetaSets.get(ghostType);
	    		if( !othPossMetaSet.contains(jntStrategy) ) {
	    			
	    			isMeta = false;
	    			break;
	    		}
    		}
    		if( isMeta ) {
		
    			if( !metaEquilActions.contains( jntStrategy ) )
    				metaEquilActions.add( jntStrategy );
    		}
    	}
    }
    
    //check!!!
    protected ArrayList<Integer> findMax( Game game, GHOST ghostType, 
    		ArrayList<Integer> candtJntStrategies, ArrayList<Integer> leftGhosts, 
    		ArrayList<Integer> rightGhosts, int curGhostIndex ) 
	{
    	
    	//int ghostIndex = queryGhostIndex(ghostType);
    	GHOST curGhostType = queryGhostType(curGhostIndex);
    	ArrayList<Integer> retList = new ArrayList<Integer>();
		
    	/**
    	 * no left ghosts means that we should find the final max action
    	 */
    	if( leftGhosts.size() == 0 ) {
	    
    		double maxValue = Double.NEGATIVE_INFINITY;
    		int maxJntStrategy = -1;
    		int[] curStrategies = new int[Constants.NUM_GHOSTS];
    		for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES_MULTI_AGENT; strategy++ ) {
	    	
    			//do not include STRATEGY BUSY
    			if( !isStrategyPossible(game, curGhostType, strategy) || 
    					strategy == Constants.GHOST_STRATEGY_BUSY )
    				continue;
    			
    			/**
    			 * set the strategy of the current ghost
    			 */
    			curStrategies[curGhostIndex] = strategy;
    			
    			/**
    			 * set the strategies of the right agents
    			 */
    			if( candtJntStrategies == null ) {
    				
    				System.out.println("Cand Joint Strategies is NULL, Why");
    				System.out.println("Right Ghost: "+rightGhosts.size());
    			}
    			for( int index = 0; index < candtJntStrategies.size(); index++ ) {
			
    				int candtJntStrategy = candtJntStrategies.get(index);
    				int[] candtStrategies = jntStrategy2Strategies(candtJntStrategy);
				
    				//find the joint action that equals the joint action of all non-right agents
    				if( strategy == candtStrategies[curGhostIndex] ) {
			
    					/**
    					for( int rightGhostListIndex = 0; rightGhostListIndex < rightGhosts.size(); rightGhostListIndex++ ) {
				
    						int rightGhostIndex = rightGhosts.get( rightGhostListIndex );
    						curStrategies[rightGhostIndex] = candtStrategies[rightGhostIndex];
    					}	
    					*/
    					
    					for( int ghostIndex = 0; ghostIndex < Constants.NUM_GHOSTS; ghostIndex++ ) {
    						
    						curStrategies[ghostIndex] = candtStrategies[ghostIndex];  
    					}
    					break;
    				}
    			}
		    
    			/**
    			 * get the value of the action
    			 */
    			int curJntStrategy = strategies2JntStrategy(curStrategies);
    			double value = getJntQValue( game, ghostType, curJntStrategy );
    			if( value > maxValue ) {
		    
    				maxValue = value;
    				maxJntStrategy = curJntStrategy;
    			}
    		}
	    
    		retList.add( maxJntStrategy );
    		return retList;
    	}
	
    	/**
    	 * if there are still left ghosts 
    	 * we should find all inter-media results for next max or min operation
    	 * note that all other joint strategies should be possible
    	 */
    	//generate the joint actions for the left ghosts
    	ArrayList<int[]> othJntStrategies = generateReducedJntStrategies( game, leftGhosts );
	
    	for( int listIndex = 0; listIndex < othJntStrategies.size(); listIndex++ ) {
	    
    		int[] strategies = othJntStrategies.get(listIndex);
	    

    		double maxValue = Double.NEGATIVE_INFINITY;
    		int maxJntStrategy = -1;
    		for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES_MULTI_AGENT; strategy++ ) {
		
    			//do not include STRATEGY BUSY
    			if( !isStrategyPossible(game, curGhostType, strategy) || 
    					strategy == Constants.GHOST_STRATEGY_BUSY ) {
    				
    				continue;
    			}
    			
    			/**
    			 * set the action of the current ghost
    			 */
    			strategies[curGhostIndex] = strategy;
		
    			/**
    			 * set the actions of the right agents
    			 */
    			if( candtJntStrategies != null ) {
		    
    				for( int index = 0; index < candtJntStrategies.size(); index++ ) {
			
    					int candtJntStrategy = candtJntStrategies.get(index);
    					int[] candtStrategies = jntStrategy2Strategies(candtJntStrategy); 
			
    					//find the joint action that equals the joint action of all non-right agents
    					if( strategy != candtStrategies[curGhostIndex] )
    						continue;
    					
    					boolean equal = true;
    					for( int leftGhostListIndex = 0; leftGhostListIndex < leftGhosts.size(); leftGhostListIndex++ ) {
			   
    						int leftGhostIndex = leftGhosts.get( leftGhostListIndex );
    						if( strategies[leftGhostIndex] != 
    								candtStrategies[leftGhostIndex] ) {
				
    							equal = false;
    							break;
    						}
    					}
			
    					//set the right actions 
    					if( equal ) {
    						
    						for( int rightGhostListIndex = 0; rightGhostListIndex < rightGhosts.size(); rightGhostListIndex++ ) {
				
    							int rightGhostIndex = rightGhosts.get( rightGhostListIndex );
    							strategies[rightGhostIndex] = candtStrategies[rightGhostIndex];
    						}
    						break;
    					}
    				}
    			}
		
    			/**
    			 * get the value of the action
    			 */
    			int jntStrategy = strategies2JntStrategy(strategies);
    			double jntQvalue = getJntQValue(game, ghostType, jntStrategy);
    			//System.out.println("Generated JntStrategy: "+jntStrategy+" "+jntQvalue);
    			if( jntQvalue > maxValue ) {
		    
    				maxValue = jntQvalue;
    				maxJntStrategy = jntStrategy;
    			}
    		}
	    
    		//add this max action to the ret list
    		retList.add( maxJntStrategy );
    	}
	
    	return retList;
    }
    
    //double check
    protected ArrayList<Integer> findMin( Game game, GHOST ghostType, 
    		ArrayList<Integer> candtJntStrategies, ArrayList<Integer> leftGhosts, 
    		ArrayList<Integer> rightGhosts, int curGhostIndex )
    {
      	
    	//int ghostIndex = queryGhostIndex(ghostType);
    	GHOST curGhostType = queryGhostType(curGhostIndex);
    	ArrayList<Integer> retList = new ArrayList<Integer>();
		
    	/**
    	 * no left ghosts means that we should find the final max action
    	 */
    	if( leftGhosts.size() == 0 ) {
	    
    		double minValue = Double.POSITIVE_INFINITY;
    		int minJntStrategy = -1;
    		int[] curStrategies = new int[Constants.NUM_GHOSTS];
    		for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES_MULTI_AGENT; strategy++ ) {
	    	
    			//do not include STRATEGY BUSY
    			if( !isStrategyPossible(game, curGhostType, strategy) || 
    					strategy == Constants.GHOST_STRATEGY_BUSY )
    				continue;
    			
    			/**
    			 * set the strategy of the current ghost
    			 */
    			curStrategies[curGhostIndex] = strategy;
    			
    			/**
    			 * set the strategies of the right agents
    			 */
    			for( int index = 0; index < candtJntStrategies.size(); index++ ) {
			
    				int candtJntStrategy = candtJntStrategies.get(index);
    				int[] candtStrategies = jntStrategy2Strategies(candtJntStrategy);
				
    				//find the joint action that equals the joint action of all non-right agents
    				if( strategy == candtStrategies[curGhostIndex] ) {
			
    					/**
    					for( int rightGhostListIndex = 0; rightGhostListIndex < rightGhosts.size(); rightGhostListIndex++ ) {
				
    						int rightGhostIndex = rightGhosts.get( rightGhostListIndex );
    						curStrategies[rightGhostIndex] = candtStrategies[rightGhostIndex];
    					}	
    					*/
    					
    					for( int ghostIndex = 0; ghostIndex < Constants.NUM_GHOSTS; ghostIndex++ ) {
    						
    						curStrategies[ghostIndex] = candtStrategies[ghostIndex];  
    					}
			    
    					break;
    				}
    			}
		    
    			/**
    			 * get the value of the action
    			 */
    			int curJntStrategy = strategies2JntStrategy(curStrategies);
    			double value = getJntQValue( game, ghostType, curJntStrategy );
    			if( value < minValue ) {
		    
    				minValue = value;
    				minJntStrategy = curJntStrategy;
    			}
    		}
	    
    		retList.add( minJntStrategy );
    		return retList;
    	}
	
    	/**
    	 * if there are still left ghosts 
    	 * we should find all inter-media results for next max or min operation
    	 * note that all other joint strategies should be possible
    	 */
    	//generate the joint actions for the left ghosts
    	ArrayList<int[]> othJntStrategies = generateReducedJntStrategies( game, leftGhosts );
	
    	for( int listIndex = 0; listIndex < othJntStrategies.size(); listIndex++ ) {
	    
    		int[] strategies = othJntStrategies.get(listIndex);
	    

    		double minValue = Double.POSITIVE_INFINITY;
    		int minJntStrategy = -1;
    		for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES_MULTI_AGENT; strategy++ ) {
		
    			if( !isStrategyPossible(game, curGhostType, strategy) || 
    					strategy == Constants.GHOST_STRATEGY_BUSY ) {
    				
    				//System.out.println("Not Possible");
    				continue;
    			}
    			
    			/**
    			 * set the action of the current ghost
    			 */
    			strategies[curGhostIndex] = strategy;
		
    			/**
    			 * set the actions of the right agents
    			 */
    			if( candtJntStrategies != null ) {
		    
    				for( int index = 0; index < candtJntStrategies.size(); index++ ) {
			
    					int candtJntStrategy = candtJntStrategies.get(index);
    					
    					int[] candtStrategies = jntStrategy2Strategies(candtJntStrategy); 
			
    					//find the joint action that equals the joint action of all non-right agents
    					if( strategy != candtStrategies[curGhostIndex] )
    						continue;
    					
    					boolean equal = true;
    					for( int leftGhostListIndex = 0; leftGhostListIndex < leftGhosts.size(); leftGhostListIndex++ ) {
			   
    						int leftGhostIndex = leftGhosts.get( leftGhostListIndex );
    						if( strategies[leftGhostIndex] != 
    								candtStrategies[leftGhostIndex] ) {
				
    							equal = false;
    							break;
    						}
    					}
			
    					//set the right actions 
    					if( equal ) {
    						
    						for( int rightGhostListIndex = 0; rightGhostListIndex < rightGhosts.size(); rightGhostListIndex++ ) {
				
    							int rightGhostIndex = rightGhosts.get( rightGhostListIndex );
    							strategies[rightGhostIndex] = candtStrategies[rightGhostIndex];
    						}
    						break;
    					}
    				}
    			}
		
    			/**
    			 * get the value of the action
    			 */
    			int jntStrategy = strategies2JntStrategy(strategies);
    			//System.out.println("Generated JntStrategy: "+jntStrategy);
    			double jntQvalue = getJntQValue(game, ghostType, jntStrategy);
    			if( jntQvalue < minValue ) {
		    
    				minValue = jntQvalue;
    				minJntStrategy = jntStrategy;
    			}
    		}
	    
    		//add this max action to the ret list
    		retList.add( minJntStrategy );
    	}
	
    	return retList;
    }
    
    /**
     * generate reduced joint strategies for given ghosts
     * @param agentList
     * @return
     */
    //double check!!
    protected ArrayList<int[]> generateReducedJntStrategies( Game game, 
    		ArrayList<Integer> ghostIndexList )
    {
    	if( ghostIndexList == null ) {
	 
    		System.out.println("generateReducedJntStrategies: Null List");
    		return null;
    	}
    	else if( ghostIndexList.size() < 1 ) {
	    
    		System.out.println("generateReducedJntStrategies: List Size Wrong");
    		return null;
    	}
	
    	ArrayList<int[]> retList = new ArrayList<int[]>();
	
    	/**
    	 * the strategy iterator
    	 */
    	int[] strategyIterator = new int[Constants.NUM_GHOSTS];
    	for( GHOST ghostType : GHOST.values() ) {
    		
    		int ghostIndex = queryGhostIndex(ghostType);
    		if( !game.doesGhostRequireAction(ghostType) )
    			strategyIterator[ghostIndex] = Constants.GHOST_STRATEGY_BUSY;
    		else
    			strategyIterator[ghostIndex] = Constants.GHOST_STRATEGY_ATTACK;
    	}
	
    	while( true ) {
	    
    		/**
    		 * the reduced joint action should also be possible
    		 */
    		boolean possible = true;
    		int[] strategies = new int[Constants.NUM_GHOSTS];
    		for( int listIndex = 0; listIndex < ghostIndexList.size(); listIndex++ ) {
		
    			int ghostIndex = ghostIndexList.get( listIndex );
    			GHOST ghostType = queryGhostType( ghostIndex );
    			
    			if( !isStrategyPossible(game, ghostType, strategyIterator[ghostIndex]) || 
    					strategyIterator[ghostIndex] == Constants.GHOST_STRATEGY_BUSY ) {
    				
    				possible = false;
    				break;
    			}
    			
    			strategies[ghostIndex] = strategyIterator[ghostIndex];
    		}
	    
    		if( possible ) {
    			
				//for other ghosts not playing the game
				for( GHOST ghType : GHOST.values() ) {
					
					if( !game.doesGhostRequireAction(ghType) ) {
						
						int ghIndex = queryGhostIndex(ghType);
						strategies[ghIndex] = Constants.GHOST_STRATEGY_BUSY;
					}
				}
				
    			retList.add( strategies );
    		}
    		/**
    		 * move to the next action
    		 */
    		for( int listIndex = ghostIndexList.size() - 1; listIndex >= 0; listIndex-- ) {
		
    			int ghostIndex = ghostIndexList.get(listIndex);
    			strategyIterator[ghostIndex] += 1;
		
    			if( listIndex > 0 && 
    					strategyIterator[ghostIndex] >= Constants.NUM_GHOST_STRATEGIES_MULTI_AGENT - 1 ) {
		    
    				strategyIterator[ghostIndex] = 0;
    			}
    			else
    				break;
    		}
	    
    		/**
    		 * check the stop condition
    		 */
    		if( strategyIterator[ghostIndexList.get(0)] >= 
    				Constants.NUM_GHOST_STRATEGIES_MULTI_AGENT - 1 ) {
		
    			break;
    		}
    	}
	
    	return retList;
    }
    
    
    /**
     * find the action with the highest utility from 
     * Nash equilibria, NSEDAs and meta equilibria
     */
    public int[] findFavorJntStrategies( Game game )
    {
    	if( nashEquilActions.size() == 0 && 
    			symmMetaEquilActions.size() == 0 && 
    			metaEquilActions.size() == 0 ) {
	    
    		System.out.println("@NegoQ->myFavoriteAction: Wrong!No Optimal Action!");
    		
    		for( GHOST ghostType : GHOST.values() ) {
    			
    			System.out.print("I am in "+game.getGhostCurrentNodeIndex(ghostType)+
    					" "+game.getGhostLairTime(ghostType)+" "+game.getGhostEdibleTime(ghostType)+ 
    					" "+game.getGhostLastMoveMade(ghostType));
    			
    			if( game.doesGhostRequireAction(ghostType) )
    				System.out.print(" Require Action");
    			else
    				System.out.print(" Donot Require");
    			
    			System.out.println();
    		}
    		return null;
    	}
	
    	ArrayList<Integer> optActions = new ArrayList<Integer>();
	
    	optActions.addAll(nashEquilActions);
    	optActions.addAll(nsedaActions);
    	optActions.addAll(symmMetaEquilActions);
    	optActions.addAll(metaEquilActions);
	
    	int[] retArray = new int[Constants.NUM_GHOSTS];
    	for( GHOST ghostType : GHOST.values() ) {
    		
    		int ghostIndex = queryGhostIndex(ghostType);
    		
    		int maxJntStrategy = -1;
    		double maxValue = Double.NEGATIVE_INFINITY;
    		
    		for( int listIndex = 0; listIndex < optActions.size(); listIndex++ ) {
    			
    			int jntStrategy = optActions.get(listIndex);
    			double jntQvalue = getJntQValue(game, ghostType, jntStrategy);
    			
    			if( jntQvalue > maxValue ) {
    				
    				maxValue = jntQvalue;
    				maxJntStrategy = jntStrategy;
    			}
    		}
    		
    		retArray[ghostIndex] = maxJntStrategy;
    	}
	
    	nashEquilActions.clear();
    	nsedaActions.clear();
    	symmMetaEquilActions.clear();
    	metaEquilActions.clear();
	
	
    	return retArray;
    }
    
}
