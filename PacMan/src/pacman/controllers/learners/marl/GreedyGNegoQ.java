package pacman.controllers.learners.marl;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Random;

import pacman.game.Constants;
import pacman.game.Game;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;

public class GreedyGNegoQ extends NegoQLambda {


	protected HashMap<GHOST, double[][]> ghostWeightMap = 
			new HashMap<Constants.GHOST, double[][]>();
	
	/**
	 * all the equilibrium actions in one state		
	 */
	protected ArrayList<Integer> optJntStrategies = 
			new ArrayList<Integer>();
	
	/**
	 * learning rate for weight vector
	 */
	public double BETA = 0.1;
	
	public GreedyGNegoQ( Game game, boolean bLearn )
	{
		
		super(game, 0, bLearn);
		
		
		for( GHOST ghostType : GHOST.values() ) {
			
			if( !ghostWeightMap.containsKey(ghostType) ) {
				
				double[][] ghostWeight = new double[Constants.NUM_JOINT_STRATEGIES]
						[Constants.NUM_FEATURES_MULTI_AGENT];
				for( int jntStrategy = 0; jntStrategy < Constants.NUM_JOINT_STRATEGIES; jntStrategy++ )	
					for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
					
						ghostWeight[jntStrategy][fIndex] = 0.0;
				}
				
				ghostWeightMap.put(ghostType, ghostWeight);	
			}
		}

		
		/**
		 * read the value function
		 */
		valueFunctionFileName = "./valueFunction_GreedyGNegoQ_";
		if( !doesLearn )
			readValueFunction( valueFunctionFileName );
	}
	
	public GreedyGNegoQ( Game game, double alpha, 
			double gamma, double beta, 
			boolean bLearn )
	{
		//epsilon is 0 in GreedyGNegoQ
		//lambda is useless, so it is also 0
		super(game, alpha, gamma, 0.0, 0, bLearn);
		
		BETA = beta;
		
		for( GHOST ghostType : GHOST.values() ) {
			
			if( !ghostWeightMap.containsKey(ghostType) ) {
				
				double[][] ghostWeight = new double[Constants.NUM_JOINT_STRATEGIES]
						[Constants.NUM_FEATURES_MULTI_AGENT];
				for( int jntStrategy = 0; jntStrategy < Constants.NUM_JOINT_STRATEGIES; jntStrategy++ )	
					for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
					
						ghostWeight[jntStrategy][fIndex] = 0.0;
				}
				
				ghostWeightMap.put(ghostType, ghostWeight);	
			}
		}
		
		/**
		 * read the value function
		 */
		valueFunctionFileName = "./valueFunction_GreedyGNegoQ_";
		//if( !doesLearn )
			//readValueFunction( valueFunctionFileName );
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
			 * and the last chosen joint strategy
			 * note that in GreedyGNegoQ, the last equilibrium 
			 * is just the last joint strategy
			 */
			int lastEquil = lastEquilibria.get(ghostType);
			int lastJntStrategy = lastJntStrategies.get( ghostType );
			
			/**
			if( lastEquil != lastJntStrategy ) {
				
				System.out.println("What's the hell");
			}
			*/
			
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
			
			
			

			/**
			 * compute the TD-error delta_(t+1)
			 * note that if we choose pi(|theta) as a greedy policy 
			 * then the expected value of the next state V_{t+1}(\theta)
			 * is the maxQ value of the next state, 
			 * with NegoQ learner, this gradient may be the equilibrium value?
			 */
			double Qsa = getJntQValue(lastGame, ghostType, lastJntStrategy);
			//double Qequil = getJntQValue(curGame, ghostType, curOptJntStrategy);
			//double V_tp1 = Qequil;
			/**
			 * the value of the next state is the average value 
			 * of those of all equilibria in the next state
			 */
			double Qequil = 0;
			for( int i = 0; i < optJntStrategies.size(); i++ ) {
				
				int jntEquil = optJntStrategies.get(i);
				Qequil += getJntQValue(curGame, ghostType, jntEquil);
			}
			double V_tp1 = Qequil / optJntStrategies.size();
			
			double delta_tp1 = rwd + GAMMA * V_tp1 - Qsa;
			
			/**
			 * the feature vector of the current state phi_t
			 */                                              
			double[] feature_t = computeFeatureValues(lastGame); 
			/**
			double[][] phi_t = new double[Constants.NUM_JOINT_STRATEGIES]
					[Constants.NUM_FEATURES_MULTI_AGENT];
			for( int jStrategy = 0; jStrategy < Constants.NUM_JOINT_STRATEGIES; jStrategy++ ) {
				
				for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
					
					if( jStrategy == lastJntStrategy )
						phi_t[jStrategy][fIndex] = feature_t[fIndex];
					else
						phi_t[jStrategy][fIndex] = 0.0;
				}
			}
			*/
			
			/**
			 * if the greedy class is used, \hat{\phi}_{t+1}, the subgradient of V_{t+1}(\theta)
			 * is \phi(s_{t+1}, a_{t+1}), where a_{t+1} is the max action in the next state
			 */
			double[] feature_tp1 = computeFeatureValues(curGame);
			/**
			double[][] hat_phi_tp1 = new double[Constants.NUM_JOINT_STRATEGIES]
					[Constants.NUM_FEATURES_MULTI_AGENT]; 
			for( int jStrategy = 0; jStrategy < Constants.NUM_JOINT_STRATEGIES; jStrategy++ ) {
				
				for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
					
					if( jStrategy == curOptJntStrategy )
						hat_phi_tp1[jStrategy][fIndex] = feature_tp1[fIndex];
					else
						hat_phi_tp1[jStrategy][fIndex] = 0.0;
				}
			}
			*/
			
			/** a third version
			double[][] hat_phi_tp1 = new double[Constants.NUM_JOINT_STRATEGIES]
					[Constants.NUM_FEATURES_MULTI_AGENT]; 
			for( int jStrategy = 0; jStrategy < Constants.NUM_JOINT_STRATEGIES; jStrategy++ ) {
				
				for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
					
					hat_phi_tp1[jStrategy][fIndex] = 0.0;
				}
			}
			for( int i = 0; i < optJntStrategies.size(); i++ ) {
				
				int jntEquil = optJntStrategies.get(i);
				for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
					
					hat_phi_tp1[jntEquil][fIndex] = feature_tp1[fIndex] / optJntStrategies.size();
				}
			}
			*/
			
			/**
			 * compute the inner product of w_t and phi_t
			 */
			double innpro_w_phi_t = 0;
			/**
			for( int jStrategy = 0; jStrategy < Constants.NUM_JOINT_STRATEGIES; jStrategy++ ) {
				for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
					
					
					innpro_w_phi_t += ghostWeightMap.get(ghostType)[jStrategy][fIndex] * 
							phi_t[jStrategy][fIndex]; 
				}
			}
			*/
			//this loop optimizes the above double-loop
			for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
				
				innpro_w_phi_t += ghostWeightMap.get(ghostType)[lastJntStrategy][fIndex] * 
						feature_t[fIndex];
			}
			
			/**
			 * then we can update the learning paramter theta 
			 * and the weight w
			 */
			/**
			for( int jStrategy = 0; jStrategy < Constants.NUM_JOINT_STRATEGIES; jStrategy++ ) {
				for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
				
					ghostThetaMap.get(ghostType)[jStrategy][fIndex] += 
							ALPHA * (delta_tp1 * phi_t[jStrategy][fIndex] - 
									GAMMA * innpro_w_phi_t * hat_phi_tp1[jStrategy][fIndex]);
					
					ghostWeightMap.get(ghostType)[jStrategy][fIndex] += 
							BETA * (delta_tp1 - innpro_w_phi_t) * phi_t[jStrategy][fIndex];
				}
			}
			*/
			
			/**
			//a third version
			boolean isEquil = false;
			for( int i = 0; i < optJntStrategies.size(); i++ ) {
				
				if( lastJntStrategy == optJntStrategies.get(i) ) {
					
					isEquil = true;
					break;
				}
			}
			for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
				
				if( isEquil ) {
					
					for( int i = 0; i < optJntStrategies.size(); i++ ) {
						
						if( lastJntStrategy == optJntStrategies.get(i) ) {
							
							ghostThetaMap.get(ghostType)[lastJntStrategy][fIndex] += 
									ALPHA * (delta_tp1 * feature_t[fIndex] - 
											GAMMA * innpro_w_phi_t * hat_phi_tp1[lastJntStrategy][fIndex]);
						}
						else {
						
							int jntEquil = optJntStrategies.get(i);
							ghostThetaMap.get(ghostType)[jntEquil][fIndex] += 
									ALPHA * (0 - 
											GAMMA * innpro_w_phi_t * hat_phi_tp1[jntEquil][fIndex]);
						}
					}
					
					ghostWeightMap.get(ghostType)[lastJntStrategy][fIndex] += 
							BETA * (delta_tp1 - innpro_w_phi_t) * feature_t[fIndex];
				}
				else {
					
					ghostThetaMap.get(ghostType)[lastJntStrategy][fIndex] += 
							ALPHA * (delta_tp1 * feature_t[fIndex] - 0);
					for(int i = 0; i < optJntStrategies.size(); i++ ) {
						
						int jntEquil = optJntStrategies.get(i);
						ghostThetaMap.get(ghostType)[jntEquil][fIndex] += 
								ALPHA * (0 - 
										GAMMA * innpro_w_phi_t * hat_phi_tp1[jntEquil][fIndex]);
					}
					
					ghostWeightMap.get(ghostType)[lastJntStrategy][fIndex] += 
							BETA * (delta_tp1 - innpro_w_phi_t) * feature_t[fIndex];
				}
			}
			*/
			
			//an optimized version of the above double-loop
			/**
			 * phi_t[jStrategy][fIndex] != 0 only when jStrategy is lastJntStrategy
			 * hat_phi_tp1[jStrategy][fIndex] != only when jStrategy is curOptJntStrategy
			 */
			/**
			 * hat_phi_tp1[jStrategy][fIndex] != 0 only when jStrategy is curOptJntStrategy
			 * now lastJntStrategy is not curOptJntStrategy
			 * so GAMMA * innpro_w_phi_t * hat_phi_tp1[lastJntStrategy][fIndex] is 0
			 */
			/**
			 * phi_t[jStrategy][fIndex] != 0 only when jStrategy is lastJntStrategy
			 * now curOptJntStrategy is not lastJntStrategy
			 * so delta_tp1 * phi_t[curOptJntStrategy][fIndex] is 0
			 */
			/**
			 * for other joint strategies, they are neight lastJntStrategy 
			 * nor curOptJntStrategy, 
			 * so delta_tp1 * phi_t[jStrategy][fIndex] is 0 
			 * and GAMMA * innpro_w_phi_t * hat_phi_tp1[jStrategy][fIndex] is 0
			 * there is no need to update the corresponding learning parameters
			 */
			/**
			 * phi_t[jStrategy][fIndex] != 0 only when jStrategy is lastJntStrategy
			 */
			/**/
			for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
				
				if( lastJntStrategy == curOptJntStrategy ) {
					

					ghostThetaMap.get(ghostType)[lastJntStrategy][fIndex] += 
							ALPHA * (delta_tp1 * feature_t[fIndex] - 
									GAMMA * innpro_w_phi_t * feature_tp1[fIndex]);
				}
				else {
					
					ghostThetaMap.get(ghostType)[lastJntStrategy][fIndex] += 
							ALPHA * (delta_tp1 * feature_t[fIndex] - 0);
					

					ghostThetaMap.get(ghostType)[curOptJntStrategy][fIndex] += 
							ALPHA * (0 - GAMMA * innpro_w_phi_t * feature_tp1[fIndex]);
				}

				ghostWeightMap.get(ghostType)[lastJntStrategy][fIndex] += 
						BETA * (delta_tp1 - innpro_w_phi_t) * feature_t[fIndex];
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
				 * note that in GreedyGNegoQ, the last equilibrium 
				 * is just the last joint strategy
				 */
				int lastEquil = lastEquilibria.get(ghostType);
				int lastJntStrategy = lastJntStrategies.get( ghostType );
				
				
				/**
				 * compute the reward for the (T-1) step
				 */
				double timeDuration = levelTime - 
						lastGame.getCurrentLevelTime();
				double rwd = fnlReward + ghostRewardMap.get(ghostType) / timeDuration;
				ghostRewardMap.put( ghostType,  0.0 );
				
				/**
				 * compute the TD-error delta_(t+1)
				 * note that if we choose pi(|theta) as a greedy policy 
				 * then the expected value of the next state V_{t+1}(\theta)
				 * is the maxQ value of the next state, 
				 * with NegoQ learner, this gradient may be the equilibrium value?
				 * 
				 * in the terminal state, the value of the next state is 0
				 */
				double Qsa = getJntQValue(lastGame, ghostType, lastJntStrategy);
				double Qequil = 0;
				double V_tp1 = Qequil;
				double delta_tp1 = rwd + GAMMA * V_tp1 - Qsa;
				
				
				/**
				 * the feature vector of the current state phi_t
				 */
				double[] feature_t = computeFeatureValues(lastGame); 
				/**
				double[][] phi_t = new double[Constants.NUM_JOINT_STRATEGIES]
						[Constants.NUM_FEATURES_MULTI_AGENT];
				for( int jStrategy = 0; jStrategy < Constants.NUM_JOINT_STRATEGIES; jStrategy++ ) {
					
					for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
						
						if( jStrategy == lastJntStrategy )
							phi_t[jStrategy][fIndex] = feature_t[fIndex];
						else
							phi_t[jStrategy][fIndex] = 0.0;
					}
				}
				*/
				
				/**
				 * if the greedy class is used, \hat{\phi}_{t+1}, the subgradient of V_{t+1}(\theta)
				 * is \phi(s_{t+1}, a_{t+1}), where a_{t+1} is the max action in the next state
				 */
				//for terminal state, its feature values are zeros
				double[] feature_tp1 = computeFeatureValues(curGame);
				/**
				double[][] hat_phi_tp1 = new double[Constants.NUM_JOINT_STRATEGIES]
						[Constants.NUM_FEATURES_MULTI_AGENT]; 
				for( int jStrategy = 0; jStrategy < Constants.NUM_JOINT_STRATEGIES; jStrategy++ ) {
					
					for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
						
						if( jStrategy == curOptJntStrategy )
							hat_phi_tp1[jStrategy][fIndex] = feature_tp1[fIndex];
						else
							hat_phi_tp1[jStrategy][fIndex] = 0.0;
					}
				}
				*/
				
				/**
				 * compute the inner product of w_t and phi_t
				 */
				double innpro_w_phi_t = 0;
				/**
				for( int jStrategy = 0; jStrategy < Constants.NUM_JOINT_STRATEGIES; jStrategy++ ) {
					for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
						
						
						innpro_w_phi_t += ghostWeightMap.get(ghostType)[jStrategy][fIndex] * 
								phi_t[jStrategy][fIndex]; 
					}
				}
				*/
				//this loop optimizes the above double-loop
				for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
					
					innpro_w_phi_t += ghostWeightMap.get(ghostType)[lastJntStrategy][fIndex] * 
							feature_t[fIndex];
				}
				
				/**
				 * then we can update the learning paramter theta 
				 * and the weight w
				 */
				/**
				for( int jStrategy = 0; jStrategy < Constants.NUM_JOINT_STRATEGIES; jStrategy++ ) {
					for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
					
						ghostThetaMap.get(ghostType)[jStrategy][fIndex] += 
								ALPHA * (delta_tp1 * phi_t[jStrategy][fIndex] - 
										GAMMA * innpro_w_phi_t * hat_phi_tp1[jStrategy][fIndex]);
						
						ghostWeightMap.get(ghostType)[jStrategy][fIndex] += 
								BETA * (delta_tp1 - innpro_w_phi_t) * phi_t[jStrategy][fIndex];
					}
				}
				*/
				//an optimized version of the above double-loop
				for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
					
					/**
					 * phi_t[jStrategy][fIndex] != 0 only when jStrategy is lastJntStrategy
					 * hat_phi_tp1[jStrategy][fIndex] != only when jStrategy is curOptJntStrategy
					 * 
					 * for terminal state, we set its feature values to 0
					 */
					ghostThetaMap.get(ghostType)[lastJntStrategy][fIndex] += 
							ALPHA * (delta_tp1 * feature_t[fIndex] - 0.0);
					
					
					/**
					 * phi_t[jStrategy][fIndex] != 0 only when jStrategy is lastJntStrategy
					 */
					ghostWeightMap.get(ghostType)[lastJntStrategy][fIndex] += 
							BETA * (delta_tp1 - innpro_w_phi_t) * feature_t[fIndex];
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

	
	/**
	 * override the method negotiation of the father class
	 */
    public int negotiation( Game game )
    {
		
    	//System.out.println("Negotiation in the GreedyGNegoQ class");
    	
    	optJntStrategies.clear();
    	
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
	
    	/**
    	 * store the equilibrium actions in the current state
    	 */
    	for( int i = 0; i < favorJntStrategies.length; i++ ) 
    		optJntStrategies.add(favorJntStrategies[i]);
    	
    	
    	Random rnd = new Random(); 
    	int chosenJntStrategy = favorJntStrategies[rnd.nextInt(Constants.NUM_GHOSTS)];
	
    	return chosenJntStrategy;
	
    }
	
}
