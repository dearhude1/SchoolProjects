package pacman.controllers.learners.marl;

import java.util.ArrayList;
import java.util.HashMap;

import pacman.game.Constants;
import pacman.game.Game;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;

public class GreedyGUCEQ extends UCEQLambda {

	
	protected HashMap<GHOST, double[][]> ghostWeightMap = 
			new HashMap<Constants.GHOST, double[][]>();
	
	/**
	 * learning rate for weight vector
	 */
	public double BETA = 0.1;
	
	public GreedyGUCEQ( Game game, boolean bLearn )
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
		valueFunctionFileName = "./valueFunction_GreedyGUCEQ_";
		if( !doesLearn )
			readValueFunction( valueFunctionFileName );
	}
	
	public GreedyGUCEQ( Game game, double alpha, 
			double gamma, double beta, boolean bLearn )
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
		valueFunctionFileName = "./valueFunction_GreedyGUCEQ_";
		//if( !doesLearn )
			//readValueFunction( valueFunctionFileName );
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
			double timeDuration = curGame.getCurrentLevelTime() - lastGame.getCurrentLevelTime();
			rwd /= timeDuration;
			ghostRewardMap.put( ghostType, 0.0 );

			
			/**
			 * compute the TD-error delta_(t+1)
			 * note that if we choose pi(|theta) as a greedy policy 
			 * then the expected value of the next state V_{t+1}(\theta)
			 * is the maxQ value of the next state, 
			 * with CEQ learner, this gradient may be the equilibrium value?
			 */
			double Qsa = getJntQValue(lastGame, ghostType, lastJntStrategy);
			double CEQ = computeCEQValue(curGame, curCorrelEquil, 
					ghostType, curGamingJntStrategies);
			double delta_tp1 = rwd + GAMMA * CEQ - Qsa;
			
			
			/**
			 * the feature vector of the current state phi_t
			 */                                              
			double[] feature_t = computeFeatureValues(lastGame); 
			
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
					
					if( curCorrelEquil[jStrategy] > 0 ) {
						
						hat_phi_tp1[jStrategy][fIndex] = feature_tp1[fIndex] * curCorrelEquil[jStrategy];
					}
					else
						hat_phi_tp1[jStrategy][fIndex] = 0.0;
				}
			}
			*/
			
			/**
			 * compute the inner product of w_t and phi_t
			 */
			double innpro_w_phi_t = 0;
			for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
				
				innpro_w_phi_t += ghostWeightMap.get(ghostType)[lastJntStrategy][fIndex] * 
						feature_t[fIndex];
			}
			
			/**
			 * then we can update the learning paramter theta 
			 * and the weight w
			 */
			boolean isSupported = false;
			for( int listIndex = 0; listIndex < curGamingJntStrategies.size(); listIndex++ ) {
				
				int[] strategies = curGamingJntStrategies.get(listIndex);
				int jntStrategy = strategies2JntStrategy(strategies);
				
				for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
				
					if( jntStrategy == lastJntStrategy ) {
						
						ghostThetaMap.get(ghostType)[jntStrategy][fIndex] += 
								ALPHA * (delta_tp1 * feature_t[fIndex] - 
								GAMMA * innpro_w_phi_t * feature_tp1[fIndex] * curCorrelEquil[listIndex]);
						
						isSupported = true;
					}
					else {
						
						ghostThetaMap.get(ghostType)[jntStrategy][fIndex] += 
								ALPHA * (0 - GAMMA * innpro_w_phi_t * 
								feature_tp1[fIndex] * curCorrelEquil[listIndex]);
					}
				}
			}
			for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
				
				if( !isSupported ) {
					
					ghostThetaMap.get(ghostType)[lastJntStrategy][fIndex] += 
							ALPHA * (delta_tp1 * feature_t[fIndex] - 0);
				}
				
				ghostWeightMap.get(ghostType)[lastJntStrategy][fIndex] += 
						BETA * (delta_tp1 - innpro_w_phi_t) * feature_t[fIndex];
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
				 * with CEQ learner, this gradient may be the equilibrium value?
				 */
				double Qsa = getJntQValue(lastGame, ghostType, lastJntStrategy);
				double CEQ = 0.0;
				double delta_tp1 = rwd + GAMMA * CEQ - Qsa;
				
				
				/**
				 * the feature vector of the current state phi_t
				 */                                              
				double[] feature_t = computeFeatureValues(lastGame); 
				
				/**
				 * if the greedy class is used, \hat{\phi}_{t+1}, the subgradient of V_{t+1}(\theta)
				 * is \phi(s_{t+1}, a_{t+1}), where a_{t+1} is the max action in the next state
				 */
				double[] feature_tp1 = computeFeatureValues(curGame);


				/**
				 * compute the inner product of w_t and phi_t
				 */
				double innpro_w_phi_t = 0;
				for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
					
					innpro_w_phi_t += ghostWeightMap.get(ghostType)[lastJntStrategy][fIndex] * 
							feature_t[fIndex];
				}
				
				
				/**
				 * then we can update the learning paramter theta 
				 * and the weight w
				 */
				for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
					
					ghostThetaMap.get(ghostType)[lastJntStrategy][fIndex] += 
							ALPHA * (delta_tp1 * feature_t[fIndex] - 0);
					
					ghostWeightMap.get(ghostType)[lastJntStrategy][fIndex] += 
							BETA * (delta_tp1 - innpro_w_phi_t) * feature_t[fIndex];
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
	
}
