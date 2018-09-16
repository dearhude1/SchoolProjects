package pacman.controllers.learners.marl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Random;

import pacman.controllers.Controller;
import pacman.game.Constants;
import pacman.game.Game;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;

/**
 * Reinforcement learning ghosts with function approximation
 * for addressing the curse of dimensionality
 * 
 * We use linear function approximation
 */
public class MARLWithFA extends Controller<EnumMap<GHOST,MOVE>> {

	/**
	 * learning parameters
	 */
    protected double ALPHA = 0.99;//0.99;
    protected double GAMMA = 0.9;//0.95
    protected double EPSILON = 0.01;//0.05;
	
	/**
	 * edible status
	 * 0: when the ghost is not edible and the pacman is not near to a power pill
	 * 1: when the ghost is edible
	 * 2: when the ghost is not edbiel but the pacman is near to a power pill
	 */
	protected static final int GHOST_NOT_EDIBLE = 0;
	protected static final int GHOST_EDIBLE = 1;
	protected static final int GHOST_NEAR_EDIBLE = 2;
	
	/**
	 * GLOBAL FEATURES:
	 * 
	 * 1: the path distance from pacman to the nearest power pill
	 * 
	 * The distance from pacman to all ghosts
	 * 2: the path distance from pacman to BLINKY
	 * 3: the path distance from pacman to PINKY
	 * 4: the path distance from pacman to INKY
	 * 5: the path distance from pacman to SUE
	 * 
	 * The distance from all ghosts to pacman
	 * 6: the path distance from BLINKY to pacman
	 * 7: the path distance from PINKY to pacman
	 * 8: the path distance from INKY to pacman
	 * 9: the path distance from SUE to pacman
	 * 
	 * The Euclidean distance between the ghosts and pacman
	 * 10: the Euclidean distance between BLINKY to pacman
	 * 11: the Euclidean distance between PINKY to pacman
	 * 12: the Euclidean distance between INKY to pacman
	 * 13: the Euclidean distance between SUE to pacman
	 * 
	 * The Manhattan distance between the ghosts and pacman
	 * 14: the Manhattan distance between BLINKY to pacman
	 * 15: the Manhattan distance between PINKY to pacman
	 * 16: the Manhattan distance between INKY to pacman
	 * 17: the Manhattan distance between SUE to pacman
	 * 
	 * The path distance from the ghost to the nearest power pill
	 * 18: the distance from BLINKY to PP
	 * 19: the distance from PINKY to PP
	 * 20: the distance from INKY to PP
	 * 21: the distance from SUE to PP
	 * 
	 * The edible time of all ghosts
	 * 22: the edible time of BLINKY
	 * 23: the edible time of PINKY
	 * 24: the edible time of INKY
	 * 25: the edible time of SUE
	 * 
	 * 26: the current time of the level
	 * 27: remaining power pills rate
	 * 28: remaining pill rate
	 * 29: remaining pacman lives rate
	 * 
	 * The path distance between the ghosts
	 * 30: distance from BLINKY to PINKY
	 * 31: distance from BLINKY to INKY
	 * 32: distance from BLINKY to SUE
	 * 33: distance from PINKY to BLINKY
	 * 34: distance from PINKY to INKY
	 * 35: distance from PINKY to SUE
	 * 36: distance from INKY to BLINKY 
	 * 37: distance from INKY to PINKY
	 * 38: distance from INKY to SUE
	 * 39: distance from SUE to BLINKY
	 * 40: distance from SUE to PINKY
	 * 41: distance from SUE to INKY
	 * 
	 * all the features are continuous 
	 * 
	 * CANDIDATE FEATURES 
	 * the combined feature of the path distance from pacman to the ghost and the edible time
	 * the combined feature of the path distance from pacman to the ghost and the path distance 
	 * from pacman to the nearest power pill
	 */
	//protected static final int NUM_FEATURES = 41;//11;
	
	
	/**
	 * one Q-value function for each ghost
	 */
	protected HashMap<GHOST, double[][]> ghostThetaMap = 
			new HashMap<Constants.GHOST, double[][]>();
	
	/**
	 * the moves of the ghosts in the current step
	 */
	protected EnumMap<GHOST,MOVE> curMoves = new EnumMap<GHOST,MOVE>(GHOST.class);
	
	/**
	 * the strategies of the ghosts in the current step
	 */
	//protected EnumMap<GHOST, Integer> curJntStrategies = 
			//new EnumMap<Constants.GHOST, Integer>(GHOST.class);
	
	/**
	 * the strategies of the ghosts in the last step
	 */
	protected EnumMap<GHOST, Integer> lastJntStrategies = 
			new EnumMap<Constants.GHOST, Integer>(GHOST.class);
	
	/**
	 * the last game state corresponding to each ghost
	 */
	protected EnumMap<GHOST,Game> lastGames = new EnumMap<GHOST,Game>(GHOST.class);
	
	/**
	 * the target power pill of the ghosts if they decide to protect the pill
	 */
	protected EnumMap<GHOST, Integer> targetPowerPillIndices = 
			new EnumMap<Constants.GHOST, Integer>(GHOST.class);
	
	/**
	 * the target edible ghost of the ghosts if they decide to cover it
	 */
	protected EnumMap<GHOST,GHOST> ghostsNeedCover = 
			new EnumMap<Constants.GHOST, Constants.GHOST>(GHOST.class);
	
	/**
	 * the ghosts which provide cover for each ghost
	 */
	protected EnumMap<GHOST,GHOST> ghostsProvideCover = 
			new EnumMap<Constants.GHOST, Constants.GHOST>(GHOST.class);
	
	/**
	 * for random numbers
	 */
	protected Random random;
	
	
	/**
	 * average reward per step (ARPS) in each episode
	 */
	//protected double[][] arpsPerEpisode = null;
	
	/**
	 * the running steps in the current game
	 */
	//protected int[] gameSteps = null;
	
	/**
	 * the reward computed for each ghost in each step
	 */
	protected EnumMap<GHOST, Double> ghostRewardMap = 
			new EnumMap<GHOST, Double>(GHOST.class);
	
	public MARLWithFA( Game game )
	{
		super();
		
		random = new Random();
		
		//lastMove = new EnumMap<GHOST,MOVE>(GHOST.class);
		
		/**
		 * initialize the value function
		 */
		initializeLearningParameters();
		
		/**
		 * initialize the last game state
		 */
		for( GHOST ghost : GHOST.values() ) {
			
			lastGames.put( ghost, game );
			
			targetPowerPillIndices.put( ghost, -1 );
		}
		
		/**
		 * initialize the reward
		 */
		for( GHOST ghost : GHOST.values() ) {
			
			ghostRewardMap.put( ghost, 0.0 );
		}
		
		/**
		 * create the array of arps and steps
		 *
		int episodeNum = Game.EPISODE_NUM;
		arpsPerEpisode = new double[4][episodeNum];
		gameSteps = new int[4];
		for( int ghostIndex = 0; ghostIndex < 4; ghostIndex++ ) {
			
			gameSteps[ghostIndex] = 0;
			for( int ep = 0; ep < episodeNum; ep++ ) {
				
				arpsPerEpisode[ghostIndex][ep] = 0.0;
			}
		}
		*/
		

	}
	
	public MARLWithFA( Game game, double alpha, 
			double gamma, double epsilon )
	{
		super();
		
		random = new Random();
		ALPHA = alpha;
		GAMMA = gamma;
		EPSILON = epsilon;
		
		//lastMove = new EnumMap<GHOST,MOVE>(GHOST.class);
		
		/**
		 * initialize the value function
		 */
		initializeLearningParameters();
		
		/**
		 * initialize the last game state
		 */
		for( GHOST ghost : GHOST.values() ) {
			
			lastGames.put( ghost, game );
			
			targetPowerPillIndices.put( ghost, -1 );
		}
		
		/**
		 * initialize the reward
		 */
		for( GHOST ghost : GHOST.values() ) {
			
			ghostRewardMap.put( ghost, 0.0 );
		}
		
		/**
		 * create the array of arps and steps
		 *
		int episodeNum = Game.EPISODE_NUM;
		arpsPerEpisode = new double[4][episodeNum];
		gameSteps = new int[4];
		for( int ghostIndex = 0; ghostIndex < 4; ghostIndex++ ) {
			
			gameSteps[ghostIndex] = 0;
			for( int ep = 0; ep < episodeNum; ep++ ) {
				
				arpsPerEpisode[ghostIndex][ep] = 0.0;
			}
		}
		*/
	}
	
	private void initializeLearningParameters()
	{
		
		for( GHOST ghostType : GHOST.values() ) {
			
			if( !ghostThetaMap.containsKey(ghostType) ) {
				
				double[][] ghostTheta = 
						new double[Constants.NUM_JOINT_STRATEGIES]
								[Constants.NUM_FEATURES_MULTI_AGENT];
				
				for( int jntStrategy = 0; jntStrategy < Constants.NUM_JOINT_STRATEGIES; jntStrategy++ ) 
					for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
					
						ghostTheta[jntStrategy][fIndex] = random.nextDouble();//0.0;
				}
				
				ghostThetaMap.put( ghostType, ghostTheta );
			}
		}
	}
	
	/**
	 * implement the method getMove, 
	 * which returns the move of the ghosts
	 */
	public EnumMap<GHOST,MOVE> getMove(Game game, long timeDue)
	{	
		curMoves.clear();
		
		for(GHOST ghostType : GHOST.values())
			if(game.doesGhostRequireAction(ghostType)) {
				
				MOVE[] possibleMoves=game.getPossibleMoves(
						game.getGhostCurrentNodeIndex(ghostType), 
						game.getGhostLastMoveMade(ghostType));
				
				curMoves.put( ghostType, 
						possibleMoves[random.nextInt(possibleMoves.length)] );
			}
		
		return curMoves;
	}
	
	/**
	 * note that if the ghost cannot take an action in the current game
	 * the returned value will be 0 for this ghost
	 */
	protected double getJntQValue( Game game, GHOST ghostType, int jntStrategy )
	{
		
		/**
		 * note that we can only choose available moves
		 */
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);

		if( ghostNodeIndex == game.getCurrentMaze().lairNodeIndex ) {
			
			//System.out.println("RLGhostsWithFA->epsilonGreedy: Ghost "+ghostType+" is in Lair");
			return 0.0;
		}
		/**
		 * if the ghost cannot take an action
		 * the returned value is 0
		 */
		else if( !game.doesGhostRequireAction(ghostType) ) {
			
			return 0.0;
		}
		
		
		/**
		 * whether the strateg is available
		 */
		if( !isJntStrategPossible(game, jntStrategy) ) {
			
			//System.out.println("Joint Strategy Invalid!");
			return 0.0;
		}
		
		
		/**
		 * compute the features of the current state
		 */
		double[] phi = computeFeatureValues( game );
		
		/**
		 * then get the Q-value
		 */
		double qValue = getJntQValue( phi, ghostType, jntStrategy );
		
		//System.out.println("jntQ Value: "+qValue);
		/**
		int[] strategies = jntStrategy2Strategies(jntStrategy);
		for( int i = 0; i < strategies.length; i++ ) 
			System.out.print(" "+strategies[i]);
		System.out.println();
		*/
		
		return qValue;
	}
	
	/**
	 * computeFeatureValues for eight-strategy version
	 *
	protected double[][] computeFeatureValueMatrix( Game game, GHOST ghostType, 
			int ghostStrategy )
	{
		
		double[][] retValues = new double[Constants.NUM_GHOST_STRATEGIES][NUM_FEATURES];
		
		double[] featureValues = computeFeatureValues( game, 
				ghostType, ghostStrategy );
		
		
		for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES; strategy++ ) {
			
			if( strategy == ghostStrategy && 
					isStrategyPossible( game, ghostType, ghostStrategy ) ) {
				
				for( int fIndex = 0; fIndex < NUM_FEATURES; fIndex++ )
					retValues[strategy][fIndex] = featureValues[fIndex];
			}
			else {
				
				for( int fIndex = 0; fIndex < NUM_FEATURES; fIndex++ )
					retValues[strategy][fIndex] = 0.0;
			}
		}
		
		return retValues;
	}
	*/
	
	/**
	 * compute the value of each eight features
	 * called by the method computeFeatureValueMatrix
	 */
	protected double[] computeFeatureValues2( Game game )
	{	
		
		//int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();
		double maxNodeDis = game.getCurrentMaxNodeDis();
		
		int blinkyNodeIndex = game.getGhostCurrentNodeIndex(GHOST.BLINKY);
		int pinkyNodeIndex = game.getGhostCurrentNodeIndex(GHOST.PINKY);
		int inkyNodeIndex = game.getGhostCurrentNodeIndex(GHOST.INKY);
		int sueNodeIndex = game.getGhostCurrentNodeIndex(GHOST.SUE);
		MOVE blinkyLastMov = game.getGhostLastMoveMade(GHOST.BLINKY);
		MOVE pinkyLastMov = game.getGhostLastMoveMade(GHOST.PINKY);
		MOVE inkyLastMov = game.getGhostLastMoveMade(GHOST.INKY);
		MOVE sueLastMov = game.getGhostLastMoveMade(GHOST.SUE);
		
		//the ret values
		double[] featureValues = new double[Constants.NUM_FEATURES_MULTI_AGENT];
		
		/**
		 * 1: the path distance from pacman to the nearest power pill
		 */
		double minDisPacmanToPP = computeDisPacmanToPowerPill(game);
		featureValues[0] = minDisPacmanToPP / maxNodeDis;
		
		/**
		 * The distance from pacman to all ghosts
		 * 2: the path distance from pacman to BLINKY
		 * 3: the path distance from pacman to PINKY
		 * 4: the path distance from pacman to INKY
		 * 5: the path distance from pacman to SUE
		 */
		double disPacmanToBlinky = computeDisPacmanToGhost(game, GHOST.BLINKY);
		featureValues[1] = disPacmanToBlinky / maxNodeDis;
		double disPacmanToPinky = computeDisPacmanToGhost(game, GHOST.PINKY);
		featureValues[2] = disPacmanToPinky / maxNodeDis;
		double disPacmanToInky = computeDisPacmanToGhost(game, GHOST.INKY);
		featureValues[3] = disPacmanToInky / maxNodeDis;
		double disPacmanToSue = computeDisPacmanToGhost(game, GHOST.SUE);
		featureValues[4] = disPacmanToSue / maxNodeDis;
		
		
		/**
		 * The distance from all ghosts to pacman
		 * 6: the path distance from BLINKY to pacman
		 * 7: the path distance from PINKY to pacman
		 * 8: the path distance from INKY to pacman
		 * 9: the path distance from SUE to pacman
		 */
		double disBlinkyToPacman = computeDisGhostToPacman(game, GHOST.BLINKY);
		double disPinkyToPacman = computeDisGhostToPacman(game, GHOST.PINKY);
		double disInkyToPacman = computeDisGhostToPacman(game, GHOST.INKY);
		double disSueToPacman = computeDisGhostToPacman(game, GHOST.SUE);
		featureValues[5] = disBlinkyToPacman / maxNodeDis;
		featureValues[6] = disPinkyToPacman / maxNodeDis;
		featureValues[7] = disInkyToPacman / maxNodeDis;
		featureValues[8] = disSueToPacman / maxNodeDis;
		
				
		/**	 
		 * The Euclidean distance between the ghosts and pacman
		 * 10: the Euclidean distance between BLINKY to pacman
		 * 11: the Euclidean distance between PINKY to pacman
		 * 12: the Euclidean distance between INKY to pacman
		 * 13: the Euclidean distance between SUE to pacman
		 */
		double eucDisBlinkyToPacman = computeEucDisGhostToPacman(game, GHOST.BLINKY);
		double eucDisPinkyToPacman = computeEucDisGhostToPacman(game, GHOST.PINKY);
		double eucDisInkyToPacman = computeEucDisGhostToPacman(game, GHOST.INKY);
		double eucDisSueToPacman = computeEucDisGhostToPacman(game, GHOST.SUE);
		featureValues[9] = eucDisBlinkyToPacman / maxNodeDis;
		featureValues[10] = eucDisPinkyToPacman / maxNodeDis;
		featureValues[11] = eucDisInkyToPacman / maxNodeDis;
		featureValues[12] = eucDisSueToPacman / maxNodeDis;
		
		
		/**
		 * The Manhattan distance between the ghosts and pacman
		 * 14: the Manhattan distance between BLINKY to pacman
		 * 15: the Manhattan distance between PINKY to pacman
		 * 16: the Manhattan distance between INKY to pacman
		 * 17: the Manhattan distance between SUE to pacman
		 */
		double manDisBlinkyToPacman = computeManhDisGhostToPacman(game, GHOST.BLINKY);
		double manDisPinkyToPacman = computeManhDisGhostToPacman(game, GHOST.PINKY);
		double manDisInkyToPacman = computeManhDisGhostToPacman(game, GHOST.INKY);
		double manDisSueToPacman = computeManhDisGhostToPacman(game, GHOST.SUE);
		featureValues[13] = manDisBlinkyToPacman / maxNodeDis;
		featureValues[14] = manDisPinkyToPacman / maxNodeDis;
		featureValues[15] = manDisInkyToPacman / maxNodeDis;
		featureValues[16] = manDisSueToPacman / maxNodeDis;
		
			
		/**
		 * The path distance from the ghost to the nearest power pill
		 * 18: the distance from BLINKY to PP
		 * 19: the distance from PINKY to PP
		 * 20: the distance from INKY to PP
		 * 21: the distance from SUE to PP
		 */
		boolean ppAvail = false;
		double minDisBlinkyToPP = Double.POSITIVE_INFINITY;
		double minDisPinkyToPP = Double.POSITIVE_INFINITY;
		double minDisInkyToPP = Double.POSITIVE_INFINITY;
		double minDisSueToPP = Double.POSITIVE_INFINITY;
		int[] powerPillNodeIndices = game.getPowerPillIndices();
		for( int ppIndex = 0; ppIndex < powerPillNodeIndices.length; ppIndex++ ) {
			
			if( game.isPowerPillStillAvailable(ppIndex) ) {
				
				ppAvail = true;
				
				//for the ghost
				double disBlinkyToPowerPill = game.getShortestPathDistance( 
						blinkyNodeIndex, powerPillNodeIndices[ppIndex], blinkyLastMov );
				double disPinkyToPowerPill = game.getShortestPathDistance(
						pinkyNodeIndex, powerPillNodeIndices[ppIndex], pinkyLastMov);
				double disInkyToPowerPill = game.getShortestPathDistance(
						inkyNodeIndex, powerPillNodeIndices[ppIndex], inkyLastMov);
				double disSueToPowerPill = game.getShortestPathDistance(
						sueNodeIndex, powerPillNodeIndices[ppIndex], sueLastMov);
				if( disBlinkyToPowerPill < minDisBlinkyToPP ) {
					
					minDisBlinkyToPP = disBlinkyToPowerPill;
				}
				if( disPinkyToPowerPill < minDisPinkyToPP ) {
					
					minDisPinkyToPP = disPinkyToPowerPill;
				}
				if( disInkyToPowerPill < minDisInkyToPP ) {
					
					minDisInkyToPP = disInkyToPowerPill;
				}
				if( disSueToPowerPill < minDisSueToPP ) {
					
					minDisSueToPP = disSueToPowerPill;
				}
			}
		}
		if( !ppAvail ) {
			
			minDisBlinkyToPP = maxNodeDis - 1.0;
			minDisPinkyToPP = maxNodeDis - 1.0;
			minDisInkyToPP = maxNodeDis - 1.0;
			minDisSueToPP = maxNodeDis - 1.0;
		}
		
		featureValues[17] = minDisBlinkyToPP / maxNodeDis;
		featureValues[18] = minDisPinkyToPP / maxNodeDis;
		featureValues[19] = minDisInkyToPP / maxNodeDis;
		featureValues[20] = minDisSueToPP / maxNodeDis;
		  
		
		/**
		 * The edible time of all ghosts
		 * 22: the edible time of BLINKY
		 * 23: the edible time of PINKY
		 * 24: the edible time of INKY
		 * 25: the edible time of SUE
		 */
		featureValues[21] = game.getGhostEdibleTime(GHOST.BLINKY) / 
				Constants.EDIBLE_TIME;
		featureValues[22] = game.getGhostEdibleTime(GHOST.PINKY) / 
				Constants.EDIBLE_TIME;
		featureValues[23] = game.getGhostEdibleTime(GHOST.INKY) / 
				Constants.EDIBLE_TIME;
		featureValues[24] = game.getGhostEdibleTime(GHOST.SUE) / 
				Constants.EDIBLE_TIME;
		
		/**
		 * 26: the current time of the level
		 * 27: remaining power pills rate
		 * 28: remaining pill rate
		 * 29: remaining pacman lives rate
		 */
		double currentLevelTime = game.getCurrentLevelTime();
		featureValues[25] = currentLevelTime / ((double) Constants.LEVEL_LIMIT);
		featureValues[26] = game.getRemainingPowerPillRate();
		featureValues[27] = game.getRemainingPillRate();
		featureValues[28] = game.getPacmanRemainingLifeRate();
		
		
		/**
		 * The path distance between the ghosts
		 * 30: distance from BLINKY to PINKY
		 * 31: distance from BLINKY to INKY
		 * 32: distance from BLINKY to SUE
		 * 33: distance from PINKY to BLINKY
		 * 34: distance from PINKY to INKY
		 * 35: distance from PINKY to SUE
		 * 36: distance from INKY to BLINKY 
		 * 37: distance from INKY to PINKY
		 * 38: distance from INKY to SUE
		 * 39: distance from SUE to BLINKY
		 * 40: distance from SUE to PINKY
		 * 41: distance from SUE to INKY
		 */
		double disBlinkyToPinky = computeDistanceBetweenGhosts(
				GHOST.BLINKY, GHOST.PINKY, game);
		double disBlinkyToInky = computeDistanceBetweenGhosts(
				GHOST.BLINKY, GHOST.INKY, game);
		double disBlinkyToSue = computeDistanceBetweenGhosts(
				GHOST.BLINKY, GHOST.SUE, game);
		featureValues[29] = disBlinkyToPinky / maxNodeDis;
		featureValues[30] = disBlinkyToInky / maxNodeDis;
		featureValues[31] = disBlinkyToSue / maxNodeDis;
		
		double disPinkyToBlinky = computeDistanceBetweenGhosts(
				GHOST.PINKY, GHOST.BLINKY, game);
		double disPinkyToInky = computeDistanceBetweenGhosts(
				GHOST.PINKY, GHOST.INKY, game);
		double disPinkyToSue = computeDistanceBetweenGhosts(
				GHOST.PINKY, GHOST.SUE, game);
		featureValues[32] = disPinkyToBlinky / maxNodeDis;
		featureValues[33] = disPinkyToInky / maxNodeDis;
		featureValues[34] = disPinkyToSue / maxNodeDis;
		
		double disInkyToBlinky = computeDistanceBetweenGhosts(
				GHOST.INKY, GHOST.BLINKY, game);
		double disInkyToPinky = computeDistanceBetweenGhosts(
				GHOST.INKY, GHOST.PINKY, game);
		double disInkyToSue = computeDistanceBetweenGhosts(
				GHOST.INKY, GHOST.SUE, game);
		featureValues[35] = disInkyToBlinky / maxNodeDis;
		featureValues[36] = disInkyToPinky / maxNodeDis;
		featureValues[37] = disInkyToSue / maxNodeDis;
		
		double disSueToBlinky = computeDistanceBetweenGhosts(
				GHOST.SUE, GHOST.BLINKY, game);
		double disSueToPinky = computeDistanceBetweenGhosts(
				GHOST.SUE, GHOST.PINKY, game);
		double disSueToInky = computeDistanceBetweenGhosts(
				GHOST.SUE, GHOST.INKY, game);
		featureValues[38] = disSueToBlinky / maxNodeDis;
		featureValues[39] = disSueToPinky / maxNodeDis;
		featureValues[40] = disSueToInky / maxNodeDis;
		
		return featureValues;
	}
	
	
	/**
	 * compute the value of each eight features
	 * called by the method computeFeatureValueMatrix
	 */
	protected double[] computeFeatureValues( Game game )
	{	
		
		//int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();
		double maxNodeDis = game.getCurrentMaxNodeDis();
		
		int blinkyNodeIndex = game.getGhostCurrentNodeIndex(GHOST.BLINKY);
		int pinkyNodeIndex = game.getGhostCurrentNodeIndex(GHOST.PINKY);
		int inkyNodeIndex = game.getGhostCurrentNodeIndex(GHOST.INKY);
		int sueNodeIndex = game.getGhostCurrentNodeIndex(GHOST.SUE);
		MOVE blinkyLastMov = game.getGhostLastMoveMade(GHOST.BLINKY);
		MOVE pinkyLastMov = game.getGhostLastMoveMade(GHOST.PINKY);
		MOVE inkyLastMov = game.getGhostLastMoveMade(GHOST.INKY);
		MOVE sueLastMov = game.getGhostLastMoveMade(GHOST.SUE);
		
		//the ret values
		double[] featureValues = new double[Constants.NUM_FEATURES_MULTI_AGENT];
		
		/**
		 * 1: the path distance from pacman to the nearest power pill
		 */
		double minDisPacmanToPP = computeDisPacmanToPowerPill(game);
		featureValues[0] = minDisPacmanToPP / maxNodeDis;
		
		/**
		 * The distance from pacman to all ghosts
		 * 2: the path distance from pacman to BLINKY
		 * 3: the path distance from pacman to PINKY
		 * 4: the path distance from pacman to INKY
		 * 5: the path distance from pacman to SUE
		 */
		double disPacmanToBlinky = computeDisPacmanToGhost(game, GHOST.BLINKY);
		featureValues[1] = disPacmanToBlinky / maxNodeDis;
		double disPacmanToPinky = computeDisPacmanToGhost(game, GHOST.PINKY);
		featureValues[2] = disPacmanToPinky / maxNodeDis;
		double disPacmanToInky = computeDisPacmanToGhost(game, GHOST.INKY);
		featureValues[3] = disPacmanToInky / maxNodeDis;
		double disPacmanToSue = computeDisPacmanToGhost(game, GHOST.SUE);
		featureValues[4] = disPacmanToSue / maxNodeDis;
		
		
		/**
		 * The distance from all ghosts to pacman
		 * 6: the path distance from BLINKY to pacman
		 * 7: the path distance from PINKY to pacman
		 * 8: the path distance from INKY to pacman
		 * 9: the path distance from SUE to pacman
		 */
		double disBlinkyToPacman = computeDisGhostToPacman(game, GHOST.BLINKY);
		double disPinkyToPacman = computeDisGhostToPacman(game, GHOST.PINKY);
		double disInkyToPacman = computeDisGhostToPacman(game, GHOST.INKY);
		double disSueToPacman = computeDisGhostToPacman(game, GHOST.SUE);
		featureValues[5] = disBlinkyToPacman / maxNodeDis;
		featureValues[6] = disPinkyToPacman / maxNodeDis;
		featureValues[7] = disInkyToPacman / maxNodeDis;
		featureValues[8] = disSueToPacman / maxNodeDis;
		
				
		/**	 
		 * The Euclidean distance between the ghosts and pacman
		 * 10: the Euclidean distance between BLINKY to pacman
		 * 11: the Euclidean distance between PINKY to pacman
		 * 12: the Euclidean distance between INKY to pacman
		 * 13: the Euclidean distance between SUE to pacman
		 */
		double eucDisBlinkyToPacman = computeEucDisGhostToPacman(game, GHOST.BLINKY);
		double eucDisPinkyToPacman = computeEucDisGhostToPacman(game, GHOST.PINKY);
		double eucDisInkyToPacman = computeEucDisGhostToPacman(game, GHOST.INKY);
		double eucDisSueToPacman = computeEucDisGhostToPacman(game, GHOST.SUE);
		featureValues[9] = eucDisBlinkyToPacman / maxNodeDis;
		featureValues[10] = eucDisPinkyToPacman / maxNodeDis;
		featureValues[11] = eucDisInkyToPacman / maxNodeDis;
		featureValues[12] = eucDisSueToPacman / maxNodeDis;
		
		
		/**
		 * The Manhattan distance between the ghosts and pacman
		 * 14: the Manhattan distance between BLINKY to pacman
		 * 15: the Manhattan distance between PINKY to pacman
		 * 16: the Manhattan distance between INKY to pacman
		 * 17: the Manhattan distance between SUE to pacman
		 */
		double manDisBlinkyToPacman = computeManhDisGhostToPacman(game, GHOST.BLINKY);
		double manDisPinkyToPacman = computeManhDisGhostToPacman(game, GHOST.PINKY);
		double manDisInkyToPacman = computeManhDisGhostToPacman(game, GHOST.INKY);
		double manDisSueToPacman = computeManhDisGhostToPacman(game, GHOST.SUE);
		featureValues[13] = manDisBlinkyToPacman / maxNodeDis;
		featureValues[14] = manDisPinkyToPacman / maxNodeDis;
		featureValues[15] = manDisInkyToPacman / maxNodeDis;
		featureValues[16] = manDisSueToPacman / maxNodeDis;
		
			
		/**
		 * The path distance from the ghost to the nearest power pill
		 * 18: the distance from BLINKY to PP
		 * 19: the distance from PINKY to PP
		 * 20: the distance from INKY to PP
		 * 21: the distance from SUE to PP
		 */
		boolean ppAvail = false;
		double minDisBlinkyToPP = Double.POSITIVE_INFINITY;
		double minDisPinkyToPP = Double.POSITIVE_INFINITY;
		double minDisInkyToPP = Double.POSITIVE_INFINITY;
		double minDisSueToPP = Double.POSITIVE_INFINITY;
		int[] powerPillNodeIndices = game.getPowerPillIndices();
		for( int ppIndex = 0; ppIndex < powerPillNodeIndices.length; ppIndex++ ) {
			
			if( game.isPowerPillStillAvailable(ppIndex) ) {
				
				ppAvail = true;
				
				//for the ghost
				double disBlinkyToPowerPill = game.getShortestPathDistance( 
						blinkyNodeIndex, powerPillNodeIndices[ppIndex], blinkyLastMov );
				double disPinkyToPowerPill = game.getShortestPathDistance(
						pinkyNodeIndex, powerPillNodeIndices[ppIndex], pinkyLastMov);
				double disInkyToPowerPill = game.getShortestPathDistance(
						inkyNodeIndex, powerPillNodeIndices[ppIndex], inkyLastMov);
				double disSueToPowerPill = game.getShortestPathDistance(
						sueNodeIndex, powerPillNodeIndices[ppIndex], sueLastMov);
				if( disBlinkyToPowerPill < minDisBlinkyToPP ) {
					
					minDisBlinkyToPP = disBlinkyToPowerPill;
				}
				if( disPinkyToPowerPill < minDisPinkyToPP ) {
					
					minDisPinkyToPP = disPinkyToPowerPill;
				}
				if( disInkyToPowerPill < minDisInkyToPP ) {
					
					minDisInkyToPP = disInkyToPowerPill;
				}
				if( disSueToPowerPill < minDisSueToPP ) {
					
					minDisSueToPP = disSueToPowerPill;
				}
			}
		}
		if( !ppAvail ) {
			
			minDisBlinkyToPP = maxNodeDis - 1.0;
			minDisPinkyToPP = maxNodeDis - 1.0;
			minDisInkyToPP = maxNodeDis - 1.0;
			minDisSueToPP = maxNodeDis - 1.0;
		}
		
		featureValues[17] = minDisBlinkyToPP / maxNodeDis;
		featureValues[18] = minDisPinkyToPP / maxNodeDis;
		featureValues[19] = minDisInkyToPP / maxNodeDis;
		featureValues[20] = minDisSueToPP / maxNodeDis;
		  
		
		/**
		 * The edible time of all ghosts
		 * 22: the edible time of BLINKY
		 * 23: the edible time of PINKY
		 * 24: the edible time of INKY
		 * 25: the edible time of SUE
		 */
		featureValues[21] = game.getGhostEdibleTime(GHOST.BLINKY) / 
				Constants.EDIBLE_TIME;
		featureValues[22] = game.getGhostEdibleTime(GHOST.PINKY) / 
				Constants.EDIBLE_TIME;
		featureValues[23] = game.getGhostEdibleTime(GHOST.INKY) / 
				Constants.EDIBLE_TIME;
		featureValues[24] = game.getGhostEdibleTime(GHOST.SUE) / 
				Constants.EDIBLE_TIME;
		
		/**
		 * 26: the current time of the level
		 * 27: remaining power pills rate
		 * 28: remaining pill rate
		 * 29: remaining pacman lives rate
		 */
		double currentLevelTime = game.getCurrentLevelTime();
		featureValues[25] = currentLevelTime / ((double) Constants.LEVEL_LIMIT);
		featureValues[26] = game.getRemainingPowerPillRate();
		featureValues[27] = game.getRemainingPillRate();
		featureValues[28] = game.getPacmanRemainingLifeRate();
		
		
		/**
		 * The path distance between the ghosts
		 * 30: distance from BLINKY to PINKY
		 * 31: distance from BLINKY to INKY
		 * 32: distance from BLINKY to SUE
		 * 33: distance from PINKY to BLINKY
		 * 34: distance from PINKY to INKY
		 * 35: distance from PINKY to SUE
		 * 36: distance from INKY to BLINKY 
		 * 37: distance from INKY to PINKY
		 * 38: distance from INKY to SUE
		 * 39: distance from SUE to BLINKY
		 * 40: distance from SUE to PINKY
		 * 41: distance from SUE to INKY
		 */
		double disBlinkyToPinky = computeDistanceBetweenGhosts(
				GHOST.BLINKY, GHOST.PINKY, game);
		double disBlinkyToInky = computeDistanceBetweenGhosts(
				GHOST.BLINKY, GHOST.INKY, game);
		double disBlinkyToSue = computeDistanceBetweenGhosts(
				GHOST.BLINKY, GHOST.SUE, game);
		featureValues[29] = disBlinkyToPinky / maxNodeDis;
		featureValues[30] = disBlinkyToInky / maxNodeDis;
		featureValues[31] = disBlinkyToSue / maxNodeDis;
		
		double disPinkyToBlinky = computeDistanceBetweenGhosts(
				GHOST.PINKY, GHOST.BLINKY, game);
		double disPinkyToInky = computeDistanceBetweenGhosts(
				GHOST.PINKY, GHOST.INKY, game);
		double disPinkyToSue = computeDistanceBetweenGhosts(
				GHOST.PINKY, GHOST.SUE, game);
		featureValues[32] = disPinkyToBlinky / maxNodeDis;
		featureValues[33] = disPinkyToInky / maxNodeDis;
		featureValues[34] = disPinkyToSue / maxNodeDis;
		
		double disInkyToBlinky = computeDistanceBetweenGhosts(
				GHOST.INKY, GHOST.BLINKY, game);
		double disInkyToPinky = computeDistanceBetweenGhosts(
				GHOST.INKY, GHOST.PINKY, game);
		double disInkyToSue = computeDistanceBetweenGhosts(
				GHOST.INKY, GHOST.SUE, game);
		featureValues[35] = disInkyToBlinky / maxNodeDis;
		featureValues[36] = disInkyToPinky / maxNodeDis;
		featureValues[37] = disInkyToSue / maxNodeDis;
		
		double disSueToBlinky = computeDistanceBetweenGhosts(
				GHOST.SUE, GHOST.BLINKY, game);
		double disSueToPinky = computeDistanceBetweenGhosts(
				GHOST.SUE, GHOST.PINKY, game);
		double disSueToInky = computeDistanceBetweenGhosts(
				GHOST.SUE, GHOST.INKY, game);
		featureValues[38] = disSueToBlinky / maxNodeDis;
		featureValues[39] = disSueToPinky / maxNodeDis;
		featureValues[40] = disSueToInky / maxNodeDis;
		
		/**
		 * added features
		 * the combined feature of distance from pacman to ghost 
		 * and the edible time
		 */
		featureValues[41] = Math.pow(disPacmanToBlinky/maxNodeDis, 
				game.getGhostEdibleTime(GHOST.BLINKY)/Constants.EDIBLE_TIME);
		featureValues[42] = Math.pow(disPacmanToPinky/maxNodeDis, 
				game.getGhostEdibleTime(GHOST.PINKY)/Constants.EDIBLE_TIME);
		featureValues[43] = Math.pow(disPacmanToInky/maxNodeDis, 
				game.getGhostEdibleTime(GHOST.INKY)/Constants.EDIBLE_TIME);
		featureValues[44] = Math.pow(disPacmanToSue/maxNodeDis, 
				game.getGhostEdibleTime(GHOST.SUE)/Constants.EDIBLE_TIME);
		
		/**
		 * added features
		 * the combined feature of the path distance from pacman to the 
		 * ghost and the path distance from pacman to the nearest power pill
		 */
		featureValues[45] = Math.pow(disPacmanToBlinky/maxNodeDis, minDisPacmanToPP/maxNodeDis);
		featureValues[46] = Math.pow(disPacmanToPinky/maxNodeDis, minDisPacmanToPP/maxNodeDis);
		featureValues[47] = Math.pow(disPacmanToInky/maxNodeDis, minDisPacmanToPP/maxNodeDis);
		featureValues[48] = Math.pow(disPacmanToSue/maxNodeDis, minDisPacmanToPP/maxNodeDis);
		
		return featureValues;
	}
	
	/**
	protected double getQValue( double[][] featureValues, GHOST ghostType )
	{
		if( featureValues == null ) {
			
			System.out.println("RLGhostsWithFA->getQValue: NULL Array");
			return 0.0;
		}
		else if( featureValues[0].length != NUM_FEATURES ) {
			
			System.out.println("RLGhostsWithFA->getQValue: Wrong Array Length "+featureValues.length);
			return 0.0;
		}
		else if( !ghostThetaMap.containsKey(ghostType) ) {
			
			System.out.println("RLGhostsWithFA->getQValue: No Such Ghost");
			return 0.0;
		}
		
		double retValue = 0.0;
		double[][] ghostTheta = ghostThetaMap.get(ghostType);
		for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES; strategy++ )
			for( int fIndex = 0; fIndex < NUM_FEATURES; fIndex++ ) {
			
				retValue += ghostTheta[strategy][fIndex] * 
						featureValues[strategy][fIndex];
		}
		
		return retValue;
	}
	*/
	
	protected double getJntQValue( double[] realFeatureValues, GHOST ghostType, 
			int jntStrategy )
	{
		if( realFeatureValues == null ) {
			
			System.out.println("MARLGhostsWithFA->getJntQValue: NULL Array");
			return 0.0;
		}
		else if( realFeatureValues.length != Constants.NUM_FEATURES_MULTI_AGENT ) {
			
			System.out.println("MARLGhostsWithFA->getJntQValue: Wrong Array Length "+realFeatureValues.length);
			return 0.0;
		}
		else if( !ghostThetaMap.containsKey(ghostType) ) {
			
			System.out.println("MARLGhostsWithFA->getJntQValue: No Such Ghost");
			return 0.0;
		}
		else if( jntStrategy < 0 || 
				jntStrategy >= Constants.NUM_JOINT_STRATEGIES ) {
			
			System.out.println("MARLGhostsWithFA->getJntQValue: Wrong Jnt Strategy");
			return 0.0;
		}
		
		double retValue = 0.0;
		double[][] ghostTheta = ghostThetaMap.get(ghostType);
		
		for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
			
			retValue += ghostTheta[jntStrategy][fIndex] * 
					realFeatureValues[fIndex];
		}
		
		return retValue;
	}
	
	/**
	 * get the maximal Q-value of joint strategies in a game state
	 */
	protected double getMaxJntQValue( Game game, GHOST ghostType )
	{
		/**
		 * note that we can only choose available moves
		 */
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);

		if( ghostNodeIndex == game.getCurrentMaze().lairNodeIndex ) {
			
			//System.out.println("RLGhostsWithFA->getMaxQValue: Ghost "+ghostType+" is in Lair");
			return 0.0;
		}
		else if( !game.doesGhostRequireAction(ghostType) ) {
			
			return 0.0;
		}
		
		/**
		 * compute the features of the current state
		 */
		double[] phi = computeFeatureValues( game );
		double maxQ = Double.NEGATIVE_INFINITY;
		for( int jntStrategy = 0; jntStrategy < Constants.NUM_JOINT_STRATEGIES; jntStrategy++ ) {
			
			/**
			 * we should skip impossible strategies
			 */
			if( !isJntStrategPossible(game, jntStrategy) )
				continue;
			
			
			double qValue = getJntQValue( phi, ghostType, jntStrategy );
			
			if( qValue > maxQ )
				maxQ = qValue;
		}
		
		return maxQ;
	}
	
	/**
	 * get the joint strategy which has the maximal Qvalue 
	 * in a game state
	 */
	protected int getMaxJntStrategy( Game game, GHOST ghostType )
	{
		
		/**
		 * note that we can only choose available moves
		 */
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);

		if( ghostNodeIndex == game.getCurrentMaze().lairNodeIndex ) {
			
			//System.out.println("RLGhostsWithFA->getMaxJntStrategy: Ghost "+ghostType+" is in Lair");
			return 0;
		}
		else if( !game.doesGhostRequireAction(ghostType) ) {
			
			System.out.println("MARLGhostsWithFA->getMaxJntStrategy: Ghost "+ghostType+
					"cannot take an action");
			return 0;
		}
		
		
		/**
		 * first find the maxQ value
		 */
		double maxQ = Double.NEGATIVE_INFINITY;
		for( int jntStrategy = 0; jntStrategy < Constants.NUM_JOINT_STRATEGIES; jntStrategy++ ) {
			
			/**
			 * we should skip impossible strategies
			 */
			if( !isJntStrategPossible(game, jntStrategy) )
				continue;
			
			
			/**
			 * compute the features of the current state
			 */
			double[] phi = computeFeatureValues( game );
			//double qValue = getQValue( phi );
			double qValue = getJntQValue( phi, ghostType, jntStrategy );
			if( qValue > maxQ )
				maxQ = qValue;
		}
		
		/**
		 * then find all strategies which have a value close to maxQ
		 */
		ArrayList<Integer> maxStrategyList = new ArrayList<Integer>();
		for( int jntStrategy = 0; jntStrategy < Constants.NUM_JOINT_STRATEGIES; jntStrategy++ ) {
			
			/**
			 * we should skip impossible strategies
			 */
			if( !isJntStrategPossible(game, jntStrategy) )
				continue;
			
			double[] phi = computeFeatureValues( game );
			double qValue = getJntQValue( phi, ghostType, jntStrategy );
			
			if( Math.abs(qValue-maxQ) < 0.00001 ) {
				
				maxStrategyList.add( jntStrategy );
			}
		}
		
		int listSize = maxStrategyList.size();
		//System.out.println("maxQ: "+maxQ);
		return maxStrategyList.get(random.nextInt(listSize));
	}
	
	/**
	 * for epsilon greedy of the learning algorithm
	 * note that this is not for joint strategy
	 */
	protected int epsilonGreedy( Game game, GHOST ghostType, 
			int chosenStrategy )
	{
		/**
		 * note that we can only choose available moves
		 */
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);

		if( ghostNodeIndex == game.getCurrentMaze().lairNodeIndex ) {
			
			//System.out.println("MARLGhostsWithFA->epsilonGreedy: Ghost "+ghostType+" is in Lair");
			return Constants.GHOST_STRATEGY_EVADE;
		}
		else if( !game.doesGhostRequireAction(ghostType) ) {
			
			System.out.println("MARLGhostsWithFA->epsilonGreedy: Ghost "+ghostType+
					"cannot take an action");
			return Constants.GHOST_STRATEGY_EVADE;
		}
		
		if( random.nextDouble() < EPSILON ) {
			
			/**
			 * find all possible strategies 
			 * do not include strategy busy
			 */
			ArrayList<Integer> possStrategies = getPossibleStrategies( game, 
					ghostType ); 
			int numPossStrategies = possStrategies.size();
			
			return possStrategies.get( random.nextInt(numPossStrategies) );
			
			
			//return random.nextInt(Constants.NUM_GHOST_STRATEGIES);
		}
		else {
			
			//System.out.println("Non Random Selection");
			return chosenStrategy;
		}
	}
	
	/**
	 * actually, we set the values of the learning parameters
	 * it seems that we do not have to use this method
	 * since we directly update learning parameters in the update function
	 *
	protected void setQValue( double[][] parameters, int ghostStrategy )
	{
		if( parameters == null ) {
			
			System.out.println("RLGhostsWithFA->setQValue: NULL Array");
			return;
		}
		else if( parameters.length != NUM_FEATURES ) {
			
			System.out.println("RLGhostsWithFA->setQValue: Wrong Array Length");
			return;
		}
		else if( ghostStrategy < 0 || ghostStrategy > Constants.NUM_GHOST_STRATEGIES ) {
			
			System.out.println("RLGhostsWithFA->setQValue: Wrong Ghost Strategt");
			return;
		}
		
		for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES; strategy++ )
			for( int fIndex = 0; fIndex < NUM_FEATURES; fIndex++ ) {
			
				theta[strategy][fIndex] = parameters[strategy][fIndex];
		}	
	}
	*/
	
	/**
	protected void setQValue( double[][] parameters, int ghostStrategy, 
			GHOST ghostType )
	{
		if( parameters == null ) {
			
			System.out.println("RLGhostsWithFA->setQValue: NULL Array");
			return;
		}
		else if( parameters.length != NUM_FEATURES ) {
			
			System.out.println("RLGhostsWithFA->setQValue: Wrong Array Length");
			return;
		}
		else if( ghostStrategy < 0 || ghostStrategy > Constants.NUM_GHOST_STRATEGIES ) {
			
			System.out.println("RLGhostsWithFA->setQValue: Wrong Ghost Strategt");
			return;
		}
		else if( !ghostThetaMap.containsKey(ghostType) ) {
			
			System.out.println("RLGhostsWithFA->setQValue: No Such Ghost");
			return;
		}
		
		for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES; strategy++ )
			for( int fIndex = 0; fIndex < NUM_FEATURES; fIndex++ ) {
			
				ghostThetaMap.get(ghostType)[strategy][fIndex] = 
						parameters[strategy][fIndex];
		}	
	}
	*/
	
	
	protected MOVE getMoveAccordingToStrategy( Game game, GHOST ghostType, 
			int strategy )
	{
		int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);
		MOVE ghostLastMove = game.getGhostLastMoveMade( ghostType );
		
		if( strategy == Constants.GHOST_STRATEGY_ATTACK ) {
			
			//directly target to Ms. Pac-man
			return game.getApproximateNextMoveTowardsTarget( ghostNodeIndex, 
					pacmanNodeIndex, ghostLastMove, DM.PATH );
		}
		else if( strategy == Constants.GHOST_STRATEGY_EVADE ) {
			
			//directly run away from Ms. Pac-Man
			return getMoveWhenEvade( game, ghostType );
		}
		else if( strategy == Constants.GHOST_STRATEGY_POWERPILL ) {
			
			return getMoveWhenPowerPill( game, ghostType );
		}
		else if( strategy == Constants.GHOST_STRATEGY_SHIELD ) {
			
			return getMoveWhenShield( game, ghostType );
		}
		/**
		else if( strategy == Constants.GHOST_STRATEGY_PROTECT ) {
			
			return getMoveWhenProtect( game, ghostType );
		}
		else if( strategy == Constants.GHOST_STRATEGY_DEFEND ) {
			
			return getMoveWhenDefend(game, ghostType);
		}
		else if( strategy == Constants.GHOST_STRATEGY_CUT ) {
			
			return getMoveWhenCut(game, ghostType);
		}
		else if( strategy == Constants.GHOST_STRATEGY_BLOCK ) {
			
			return getMoveWhenBlock(game, ghostType);
		}
		else if( strategy == Constants.GHOST_STRATEGY_COVER ) {
			
			return getMoveWhenCover(game, ghostType);
		}
		else if( strategy == Constants.GHOST_STRATEGY_FINDCOVER ) {
			
			return getMoveWhenFindCover(game, ghostType);
		}
		*/
		else {
			
			System.out.println("RLGhostsWithFA->getMoveAccordingToStrategy: Bad Strategy!");
			return MOVE.UP;
		}
	}
	
	private MOVE getMoveWhenEvade( Game game, GHOST ghostType )
	{
		int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);
		MOVE ghostLastMove = game.getGhostLastMoveMade( ghostType );
		
		/**
		 * if the ghost is edible, move away from Pacman
		 */
		if( game.isGhostEdible(ghostType) ) {
			
			return game.getApproximateNextMoveAwayFromTarget( 
					ghostNodeIndex, pacmanNodeIndex, ghostLastMove, 
					DM.PATH );
		}
		/**
		 * if the ghost is not edible but Pacman is near 
		 * to a power pill, then move away from the power pill
		 */
		else {
			
			boolean ppAvail = false;
			int targetPowerPillIndex = -1;
			double minDisToPP = Double.POSITIVE_INFINITY;
			int[] powerPillNodeIndices = game.getPowerPillIndices();
			for( int ppIndex = 0; ppIndex < powerPillNodeIndices.length; ppIndex++ ) {
				
				if( game.isPowerPillStillAvailable(ppIndex) ) {
					
					ppAvail = true;
					double disToPP = game.getShortestPathDistance( pacmanNodeIndex, 
							powerPillNodeIndices[ppIndex] );
					if( disToPP < minDisToPP ) {
						
						minDisToPP = disToPP;
						targetPowerPillIndex = ppIndex;
					}
				}
			}
			
			if( ppAvail && minDisToPP < 50 ) { //35 ) {
				
				/**/
				return game.getApproximateNextMoveAwayFromTarget( 
						ghostNodeIndex, powerPillNodeIndices[targetPowerPillIndex], 
						ghostLastMove, DM.PATH );
				//*/
				/**
				return game.getApproximateNextMoveAwayFromTarget( 
						ghostNodeIndex, pacmanNodeIndex, 
						ghostLastMove, DM.PATH );
				*/
				
			}
			else {
				
				System.out.println("RLGhostsWithFA->getMoveWhenEvade: Evade is not Possible!");
				return MOVE.NEUTRAL;
				
			}
		}
		
	}
	
	private MOVE getMoveWhenPowerPill( Game game, GHOST ghostType )
	{
		/**
		 * find the nearest power pill which can be saved 
		 * and the path from pacman to which does not contain the ghost
		 */
		int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);
		MOVE ghostLastMove = game.getGhostLastMoveMade( ghostType );
		
		//target to the nearest Power Pill to Ms. Pac-Man
		boolean ppAvail = false;
		int targetPowerPillIndex = -1;
		double minDisGhostToPP = Double.POSITIVE_INFINITY;
		int[] powerPillNodeIndices = game.getPowerPillIndices();
		for( int ppIndex = 0; ppIndex < powerPillNodeIndices.length; ppIndex++ ) {
			
			//if the ghost is edible, then it cannot protect a power pill
			if( game.isGhostEdible(ghostType) )
				break;
			
			if( game.isPowerPillStillAvailable(ppIndex) ) {
				
				ppAvail = true;
				
				double disPacmanToPP = game.getShortestPathDistance( pacmanNodeIndex, 
						powerPillNodeIndices[ppIndex] );
				
				double disGhostToPP = game.getShortestPathDistance( ghostNodeIndex, 
						powerPillNodeIndices[ppIndex], ghostLastMove );
				
				//also the power pill should not be far away from the pacman
				if( disPacmanToPP < disGhostToPP || 
						disPacmanToPP >= 60 )
					continue;
				
				boolean onPath = false;
				int[] pathPacmanToPP = game.getShortestPath( pacmanNodeIndex, 
						powerPillNodeIndices[ppIndex] );
				for( int i = 0; i < pathPacmanToPP.length; i++ ) {
					
					if( pathPacmanToPP[i] == ghostNodeIndex ) {
						
						onPath = true;
						break;
					}
				}
				if( onPath ) 
					continue;
				
				if( disGhostToPP < minDisGhostToPP ) {
					
					minDisGhostToPP = disGhostToPP;
					targetPowerPillIndex = ppIndex;
				}
			}
		}
		if( targetPowerPillIndex != -1 ) {
			
			targetPowerPillIndices.remove( ghostType );
			targetPowerPillIndices.put( ghostType, targetPowerPillIndex );
			
			return game.getApproximateNextMoveTowardsTarget( ghostNodeIndex, 
					powerPillNodeIndices[targetPowerPillIndex], 
					ghostLastMove, DM.PATH );
		}
		//we should guarantee that there is power pill and 
		//the ghost is not edible!!
		//else target to or runaway from Ms. Pac-Man
		else {
			
			targetPowerPillIndices.remove( ghostType );
			targetPowerPillIndices.put( ghostType, -1 );
			
			System.out.println("Cannot Protect, No Power Pill Available!");
			if( game.isGhostEdible(ghostType) )
				return game.getApproximateNextMoveAwayFromTarget( ghostNodeIndex, 
						pacmanNodeIndex, ghostLastMove, DM.PATH );
			else
				return game.getApproximateNextMoveTowardsTarget( ghostNodeIndex, 
						pacmanNodeIndex, ghostLastMove, DM.PATH );
		}
	}
	
	private MOVE getMoveWhenShield( Game game, GHOST ghostType )
	{
		int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);
		MOVE ghostLastMove = game.getGhostLastMoveMade( ghostType );
		MOVE pacmanLastMove = game.getPacmanLastMoveMade();
		
		
		/**
		 * edible ghost should find a cover
		 */
		if( game.isGhostEdible(ghostType) ) {
			
			/**
			 * find a way to a non-edible ghost to which the path 
			 * does not contain the current position of Ms. Pac-Man
			 */
			GHOST providerGhost = findCoverProviderGhost( game, 
					ghostType, pacmanNodeIndex, ghostNodeIndex, 
					pacmanLastMove, ghostLastMove );
			
			//directly run away from Ms. Pac-Man if we cannot find a cover
			if( providerGhost == null ) {
				
				//System.out.println("No Ghosts can Cover ME!!!");
				
				if( ghostsProvideCover.containsKey(ghostType) )
					ghostsProvideCover.remove( ghostType );
				
				return game.getApproximateNextMoveAwayFromTarget( ghostNodeIndex, 
						pacmanNodeIndex, ghostLastMove, DM.PATH );
			}
			else {
				
				if( ghostsProvideCover.containsKey(ghostType) )
					ghostsProvideCover.remove( ghostType );
				ghostsProvideCover.put( ghostType, providerGhost );
				
				//find the NPP node where the provider ghost is moving
				int pvdGhostNodeIndex = game.getGhostCurrentNodeIndex( providerGhost );
				MOVE pvdGhostMove = game.getGhostLastMoveMade(providerGhost);
				int tgtNPPNodeIndex = game.getNextNPPNodeIndex(pvdGhostNodeIndex, 
						pvdGhostMove);
				
				return game.getApproximateNextMoveTowardsTarget( ghostNodeIndex,
						tgtNPPNodeIndex, ghostLastMove, DM.PATH ); 
			}
		}
		/**
		 * non-edible ghost should cover an edible ghost
		 */
		else {
			
			//cover the edible ghosts
			GHOST targetGhost = findCoverTargetGhost( game, 
					ghostType, pacmanNodeIndex, ghostNodeIndex, 
					pacmanLastMove, ghostLastMove );
			
			if( targetGhost == null ) {
				
				//System.out.println("No Edible Ghosts");
				
				if( ghostsNeedCover.containsKey(ghostType) )
					ghostsNeedCover.remove(ghostType);
				
				return game.getApproximateNextMoveTowardsTarget( ghostNodeIndex, 
						pacmanNodeIndex, ghostLastMove, DM.PATH );
			}
			else {
				
				if( ghostsNeedCover.containsKey(ghostType) )
					ghostsNeedCover.remove(ghostType);
				ghostsNeedCover.put(ghostType, targetGhost);
				
				//find the NPP node where the target ghost is moving
				int tgtGhostNodeIndex = game.getGhostCurrentNodeIndex( targetGhost );
				MOVE tgtGhostMove = game.getGhostLastMoveMade(targetGhost);
				int tgtNPPNodeIndex = game.getNextNPPNodeIndex(tgtGhostNodeIndex, 
						tgtGhostMove);
				
				return game.getApproximateNextMoveTowardsTarget( ghostNodeIndex,
						tgtNPPNodeIndex, ghostLastMove, DM.PATH );
			}
		}

	}
	
	/**
	 * get the direction when the ghost chooses STRATEGY PROTECT
	 *
	private MOVE getMoveWhenProtect( Game game, GHOST ghostType )
	{
		int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);
		MOVE ghostLastMove = game.getGhostLastMoveMade( ghostType );
		
		//target to the nearest Power Pill to Ms. Pac-Man
		boolean ppAvail = false;
		int targetPowerPillIndex = -1;
		double minDisToPP = Double.POSITIVE_INFINITY;
		int[] powerPillNodeIndices = game.getPowerPillIndices();
		for( int ppIndex = 0; ppIndex < powerPillNodeIndices.length; ppIndex++ ) {
			
			if( game.isPowerPillStillAvailable(ppIndex) ) {
				
				ppAvail = true;
				double disToPP = game.getShortestPathDistance( pacmanNodeIndex, 
						powerPillNodeIndices[ppIndex] );
				if( disToPP < minDisToPP ) {
					
					minDisToPP = disToPP;
					targetPowerPillIndex = ppIndex;
				}
			}
		}
		//if there are still available power pills 
		//and the ghost is not edible
		if( ppAvail && !game.isGhostEdible(ghostType) ) {
			
			targetPowerPillIndices.remove( ghostType );
			targetPowerPillIndices.put( ghostType, targetPowerPillIndex );
			
			return game.getApproximateNextMoveTowardsTarget( ghostNodeIndex, 
					powerPillNodeIndices[targetPowerPillIndex], 
					ghostLastMove, DM.PATH );
		}
		//we should guarantee that there is power pill and 
		//the ghost is not edible!!
		//else target to or runaway from Ms. Pac-Man
		else {
			
			targetPowerPillIndices.remove( ghostType );
			targetPowerPillIndices.put( ghostType, -1 );
			
			System.out.println("Cannot Protect, No Power Pill Available!");
			if( game.isGhostEdible(ghostType) )
				return game.getApproximateNextMoveAwayFromTarget( ghostNodeIndex, 
						pacmanNodeIndex, ghostLastMove, DM.PATH );
			else
				return game.getApproximateNextMoveTowardsTarget( ghostNodeIndex, 
						pacmanNodeIndex, ghostLastMove, DM.PATH );
		}
	}
	*/

	
	/**
	 * get the direction when the ghost chooses STRATEGY DEFEND
	 *
	private MOVE getMoveWhenDefend( Game game, GHOST ghostType )
	{
		int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);
		MOVE ghostLastMove = game.getGhostLastMoveMade( ghostType );
		
		//target to the nearest Power Pill to this ghost
		boolean ppAvail = false;
		int targetPowerPillIndex = -1;
		double minDisToPP = Double.POSITIVE_INFINITY;
		int[] powerPillNodeIndices = game.getPowerPillIndices();
		for( int ppIndex = 0; ppIndex < powerPillNodeIndices.length; ppIndex++ ) {
			
			if( game.isPowerPillStillAvailable(ppIndex) ) {
				
				ppAvail = true;
				double disToPP = game.getShortestPathDistance( ghostNodeIndex, 
						powerPillNodeIndices[ppIndex], ghostLastMove );
				if( disToPP < minDisToPP ) {
					
					minDisToPP = disToPP;
					targetPowerPillIndex = ppIndex;
				}
			}
		}
		//if there are still available power pills 
		//and the ghost is not edible
		if( ppAvail && !game.isGhostEdible(ghostType) ) {
			
			targetPowerPillIndices.remove( ghostType );
			targetPowerPillIndices.put( ghostType, targetPowerPillIndex );
			
			return game.getApproximateNextMoveTowardsTarget( ghostNodeIndex, 
					powerPillNodeIndices[targetPowerPillIndex], 
					ghostLastMove, DM.PATH );
		}
		//we should guarantee that there is power pill and 
		//the ghost is not edible!!
		//else target to or runaway from Ms. Pac-Man
		else {
			
			targetPowerPillIndices.remove( ghostType );
			targetPowerPillIndices.put( ghostType, -1 );
			
			System.out.println("Cannot Defend, No Power Pill Available!");
			if( game.isGhostEdible(ghostType) )
				return game.getApproximateNextMoveAwayFromTarget( ghostNodeIndex, 
						pacmanNodeIndex, ghostLastMove, DM.PATH );
			else
				return game.getApproximateNextMoveTowardsTarget( ghostNodeIndex, 
						pacmanNodeIndex, ghostLastMove, DM.PATH );
		}
	}
	*/
	
	/**
	 * get the direction when the ghost chooses STRATEGY CUT
	 *
	private MOVE getMoveWhenCut( Game game, GHOST ghostType )
	{
		int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);
		MOVE ghostLastMove = game.getGhostLastMoveMade( ghostType );
		MOVE pacmanLastMove = game.getPacmanLastMoveMade();
		
		//if the ghost is edible, then run away
		if( game.isGhostEdible(ghostType) ) {
			
			System.out.println("Cannot cut, Edible Ghost!");
			return game.getApproximateNextMoveAwayFromTarget( ghostNodeIndex, 
					pacmanNodeIndex, ghostLastMove, DM.PATH );
		}
		else {
			
			//find the cut node
			int cutNodeIndex =  getNPPNodeCutPacman( game, 
					pacmanNodeIndex, ghostNodeIndex, 
					pacmanLastMove, ghostLastMove );
			
			return game.getApproximateNextMoveTowardsTarget( ghostNodeIndex, 
					cutNodeIndex, ghostLastMove, DM.PATH );
		}
	}
	*/
	
	/**
	 * get the direction when the ghost chooses STRATEGY BLOCK
	 *
	private MOVE getMoveWhenBlock( Game game, GHOST ghostType )
	{		
		int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);
		MOVE ghostLastMove = game.getGhostLastMoveMade( ghostType );
		MOVE pacmanLastMove = game.getPacmanLastMoveMade();
	
		//if the ghost is edible, then run away
		if( game.isGhostEdible(ghostType) ) {
			
			System.out.println("Cannot block, Edible Ghost!");
			return game.getApproximateNextMoveAwayFromTarget( ghostNodeIndex, 
					pacmanNodeIndex, ghostLastMove, DM.PATH );
		}
		else {
			
			//find the block node
			int blockNodeIndex = getNPPNodeBlockPacman( game, 
					pacmanNodeIndex, ghostNodeIndex, 
					pacmanLastMove, ghostLastMove );
			
			return game.getApproximateNextMoveTowardsTarget( ghostNodeIndex, 
					blockNodeIndex, ghostLastMove, DM.PATH );
		}
	}
	*/
	
	/**
	 * get the direction when the ghost chooses STRATEGY COVER
	 *
	private MOVE getMoveWhenCover( Game game, GHOST ghostType )
	{
		int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);
		MOVE ghostLastMove = game.getGhostLastMoveMade( ghostType );
		MOVE pacmanLastMove = game.getPacmanLastMoveMade();
		
		if( game.isGhostEdible(ghostType) ) {
			
			System.out.println("Cannot Cover, Edible Ghost");
			
			if( ghostsNeedCover.containsKey(ghostType) )
				ghostsNeedCover.remove(ghostType);
			
			return game.getApproximateNextMoveAwayFromTarget( ghostNodeIndex, 
					pacmanNodeIndex, ghostLastMove, DM.PATH );
		}
		
		//cover the edible ghosts
		GHOST targetGhost = findCoverTargetGhost( game, 
				ghostType, pacmanNodeIndex, ghostNodeIndex, 
				pacmanLastMove, ghostLastMove );
		
		if( targetGhost == null ) {
			
			//System.out.println("No Edible Ghosts");
			
			if( ghostsNeedCover.containsKey(ghostType) )
				ghostsNeedCover.remove(ghostType);
			
			return game.getApproximateNextMoveTowardsTarget( ghostNodeIndex, 
					pacmanNodeIndex, ghostLastMove, DM.PATH );
		}
		else {
			
			if( ghostsNeedCover.containsKey(ghostType) )
				ghostsNeedCover.remove(ghostType);
			ghostsNeedCover.put(ghostType, targetGhost);
			
			//find the NPP node where the target ghost is moving
			int tgtGhostNodeIndex = game.getGhostCurrentNodeIndex( targetGhost );
			MOVE tgtGhostMove = game.getGhostLastMoveMade(targetGhost);
			int tgtNPPNodeIndex = game.getNextNPPNodeIndex(tgtGhostNodeIndex, tgtGhostMove);
			
			return game.getApproximateNextMoveTowardsTarget( ghostNodeIndex,
					tgtNPPNodeIndex, ghostLastMove, DM.PATH );
		}
	}
	*/
	
	/**
	 * get the direction when the ghost chooses STRATEGY FIND_COVER
	 *
	private MOVE getMoveWhenFindCover( Game game, GHOST ghostType )
	{
		int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);
		MOVE ghostLastMove = game.getGhostLastMoveMade( ghostType );
		MOVE pacmanLastMove = game.getPacmanLastMoveMade();
		
		if( !game.isGhostEdible(ghostType) ) {
			
			System.out.println("Do not have to find cover, non-edible");
			
			if( ghostsProvideCover.containsKey(ghostType) )
				ghostsProvideCover.remove( ghostType );
			
			return 	game.getApproximateNextMoveTowardsTarget( ghostNodeIndex,
					pacmanNodeIndex, ghostLastMove, DM.PATH );
		}
		
		GHOST providerGhost = findCoverProviderGhost( game, 
				ghostType, pacmanNodeIndex, ghostNodeIndex, 
				pacmanLastMove, ghostLastMove );
		
		//directly run away from Ms. Pac-Man if we cannot find a cover
		if( providerGhost == null ) {
			
			//System.out.println("No Ghosts can Cover ME!!!");
			
			if( ghostsProvideCover.containsKey(ghostType) )
				ghostsProvideCover.remove( ghostType );
			
			return game.getApproximateNextMoveAwayFromTarget( ghostNodeIndex, 
					pacmanNodeIndex, ghostLastMove, DM.PATH );
		}
		else {
			
			if( ghostsProvideCover.containsKey(ghostType) )
				ghostsProvideCover.remove( ghostType );
			ghostsProvideCover.put( ghostType, providerGhost );
			
			//find the NPP node where the provider ghost is moving
			int pvdGhostNodeIndex = game.getGhostCurrentNodeIndex( providerGhost );
			MOVE pvdGhostMove = game.getGhostLastMoveMade(providerGhost);
			int tgtNPPNodeIndex = game.getNextNPPNodeIndex(pvdGhostNodeIndex, pvdGhostMove);
			
			return game.getApproximateNextMoveTowardsTarget( ghostNodeIndex,
					tgtNPPNodeIndex, ghostLastMove, DM.PATH ); 
		}
	}
	*/
	
	protected ArrayList<Integer> getPossibleStrategies( Game game, GHOST ghostType )
	{
		
		ArrayList<Integer> possStrategyList = new ArrayList<Integer>();
		for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES_MULTI_AGENT; strategy++ ) {
			
			//be careful with STRATEGY BUSY
			if( strategy == Constants.GHOST_STRATEGY_BUSY ) 
				continue;
			
			if( isStrategyPossible( game, ghostType, strategy ) ) {
				
				possStrategyList.add( strategy );
			}
		}
		
		return possStrategyList;
	}
	
	protected boolean isStrategyPossible( Game game, GHOST ghostType, int strategy )
	{
		
		boolean retValue = false;
		
		switch ( strategy ) {
		
		case Constants.GHOST_STRATEGY_ATTACK: {
			retValue = true;
			break;
		}
		case Constants.GHOST_STRATEGY_EVADE: {
			retValue = isStrategyEvadePossible(game, ghostType);
			break;
		}
		case Constants.GHOST_STRATEGY_POWERPILL: {
			
			if( !isThereAvailPowerPill(game) )
				retValue = false;
			else if( game.isGhostEdible(ghostType) )
				retValue = false;
			else
				retValue = isStrategyPowerPillPossible( game, ghostType );
			break;
		}
		case Constants.GHOST_STRATEGY_SHIELD: {
			
			//edible ghosts can find a cover
			if( game.isGhostEdible(ghostType) ) {
				
				if( isThereNonEdibleGhosts(game) )
					retValue = true;
				else
					retValue = false;
			}
			//non-edible ghosts can cover edible ghosts
			else {
				
				if( isThereEdibleGhosts(game) )
					retValue = true;
				else
					retValue = false;
			}
			break;
		}
		/**
		case Constants.GHOST_STRATEGY_PROTECT: {
			
			if( isThereAvailPowerPill(game) && 
					!game.isGhostEdible(ghostType) )
				retValue = true;
			else
				retValue = false;
			break;
		}
		case Constants.GHOST_STRATEGY_DEFEND: {
			
			if( isThereAvailPowerPill(game) && 
					!game.isGhostEdible(ghostType) )
				retValue = true;
			else
				retValue = false;
			break;
		}
		*/
		/**
		case Constants.GHOST_STRATEGY_CUT: {
			
			retValue = isStrategyCutPossible(game, ghostType);
			break;
		}
		case Constants.GHOST_STRATEGY_BLOCK: {
			
			retValue = isStrategyBlockPossible( game, ghostType );
			break;
		}
		*/
		/**
		case Constants.GHOST_STRATEGY_COVER: {
			
			if( isThereEdibleGhosts(game) && 
					!game.isGhostEdible(ghostType) )
				retValue = true;
			else
				retValue = false;
			break;
		}
		case Constants.GHOST_STRATEGY_FINDCOVER: {
			
			if( isThereNonEdibleGhosts(game) && 
					game.isGhostEdible(ghostType) )
				retValue = true;
			else
				retValue = false;
			break;
		}
		*/
		//be careful with STRATEGY BUSY
		case Constants.GHOST_STRATEGY_BUSY: {
			
			retValue = true;
			break;
		}
		default: {
			
			retValue = false;
			break;
		}
		}
		
		return retValue;
	}
	
	private boolean isThereEdibleGhosts( Game game )
	{
		boolean thereIs = false;
		
		for( GHOST ghostType : GHOST.values() ) {
			
			if( game.isGhostEdible(ghostType) ) {
				
				thereIs = true;
				break;
			}
		}
		
		return thereIs;
	}
	
	private boolean isThereNonEdibleGhosts( Game game )
	{
		boolean thereIs = false;
		
		for( GHOST ghostType : GHOST.values() ) {
			
			if( !game.isGhostEdible(ghostType) && 
					game.getGhostLairTime(ghostType) == 0 ) {
				
				thereIs = true;
				break;
			}
		}
		
		return thereIs;
	}
	
	private boolean isThereAvailPowerPill( Game game )
	{
		boolean ppAvail = false;
		int[] powerPillNodeIndices = game.getPowerPillIndices();
		for( int ppIndex = 0; ppIndex < powerPillNodeIndices.length; ppIndex++ ) {
			
			if( game.isPowerPillStillAvailable(ppIndex) ) {
				
				ppAvail = true;
				break;
			}
		}
		
		return ppAvail;
	}
	
	
	private boolean isStrategyEvadePossible( Game game, GHOST ghostType ) 
	{
		
		int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();
		
		/**
		 * if the ghost is edible, move away from Pacman
		 */
		if( game.isGhostEdible(ghostType) ) {
			
			return true;
		}
		/**
		 * if the ghost is not edible but Pacman is near 
		 * to a power pill, then move away from the power pill
		 */
		else {
			
			boolean ppAvail = false;
			double minDisToPP = Double.POSITIVE_INFINITY;
			int[] powerPillNodeIndices = game.getPowerPillIndices();
			for( int ppIndex = 0; ppIndex < powerPillNodeIndices.length; ppIndex++ ) {
				
				if( game.isPowerPillStillAvailable(ppIndex) ) {
					
					ppAvail = true;
					double disToPP = game.getShortestPathDistance( pacmanNodeIndex, 
							powerPillNodeIndices[ppIndex] );
					if( disToPP < minDisToPP ) {
						
						minDisToPP = disToPP;
					}
				}
			}
			
			//a little prior knowledge
			if( ppAvail && minDisToPP < 50 ) { //35 ) {
				
				return true;
			}
			else {
				
				return false;
			}
		}
	}
	
	
	private boolean isStrategyPowerPillPossible( Game game, 
			GHOST ghostType )
	{
		if( game.isGhostEdible(ghostType) ) 
			return false;
		
		/**
		 * find the nearest power pill which can be saved 
		 * and the path from pacman to which does not contain the ghost
		 */
		int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);
		MOVE ghostLastMove = game.getGhostLastMoveMade( ghostType );
		
		//target to the nearest Power Pill to Ms. Pac-Man
		boolean ppAvail = false;
		int targetPowerPillIndex = -1;
		double minDisGhostToPP = Double.POSITIVE_INFINITY;
		int[] powerPillNodeIndices = game.getPowerPillIndices();
		for( int ppIndex = 0; ppIndex < powerPillNodeIndices.length; ppIndex++ ) {
			
			if( game.isPowerPillStillAvailable(ppIndex) ) {
				
				ppAvail = true;
				
				double disPacmanToPP = game.getShortestPathDistance( pacmanNodeIndex, 
						powerPillNodeIndices[ppIndex] );
				
				double disGhostToPP = game.getShortestPathDistance( ghostNodeIndex, 
						powerPillNodeIndices[ppIndex], ghostLastMove );
				
				//also the power pill should not be far away from the pacman
				if( disPacmanToPP < disGhostToPP || 
						disPacmanToPP >= 60 )
					continue;
				
				boolean onPath = false;
				int[] pathPacmanToPP = game.getShortestPath( pacmanNodeIndex, 
						powerPillNodeIndices[ppIndex] );
				for( int i = 0; i < pathPacmanToPP.length; i++ ) {
					
					if( pathPacmanToPP[i] == ghostNodeIndex ) {
						
						onPath = true;
						break;
					}
				}
				if( onPath ) 
					continue;
				
				else
					return true;
			}
		}
		
		return false;
	}
	
	/**
	private boolean isStrategyCutPossible( Game game, 
			GHOST ghostType )
	{
		if( game.isGhostEdible(ghostType) )
				return false;
		
		int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);
		MOVE ghostLastMove = game.getGhostLastMoveMade( ghostType );
		MOVE pacmanLastMove = game.getPacmanLastMoveMade();
		
		//find the cut node, if cannot find, then not possible
		int towardNPPNodeIndex = getNPPNodeCutPacman( game, 
				pacmanNodeIndex, ghostNodeIndex, 
				pacmanLastMove, ghostLastMove );
		
		if( towardNPPNodeIndex == -1 )
			return false;
		else {
			
			//can we store the toward node??
			return true;
		}
	}
	*/
	
	/**
	private boolean isStrategyBlockPossible( Game game, GHOST ghostType )
	{
		if( game.isGhostEdible(ghostType) )
			return false;
	
		int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);
		MOVE ghostLastMove = game.getGhostLastMoveMade( ghostType );
		MOVE pacmanLastMove = game.getPacmanLastMoveMade();
	
		//find the cut node, if cannot find, then not possible
		int backwardNPPNodeIndex = getNPPNodeBlockPacman( game, 
				pacmanNodeIndex, ghostNodeIndex, 
				pacmanLastMove, ghostLastMove );
	
		if( backwardNPPNodeIndex == -1 )
			return false;
		else {
		
			//can we store the toward node??
			return true;
		}
	}
	*/
	
	/**
	 * query the index of the ghost 
	 * according to the type of the ghost
	 */
	protected int queryGhostIndex( GHOST ghostType ) 
	{
		
		int ghostIndex = 0;
		for( GHOST ghost : GHOST.values() ) {
			
			if( ghost == ghostType ) 
				return ghostIndex;
			
			ghostIndex++;
		}
		
		System.out.println("RLGhostsWithFA->queryGhostIndex: Cannot find the ghost index");
		return ghostIndex;
	}
	
	/**
	 * query the index of the ghost 
	 * according to the type of the ghost
	 */
	protected GHOST queryGhostType( int ghostIndex ) 
	{
		
		int ghIndex = 0;
		for( GHOST ghost : GHOST.values() ) {
			
			if( ghIndex == ghostIndex ) 
				return ghost;
			
			ghIndex++;
		}
		
		System.out.println("RLGhostsWithFA->queryGhostIndex: Cannot find the ghost type "+ghostIndex);
		return null;
	}
	
	protected int queryEdibleState( Game game, GHOST ghostType )
	{
		
		if( game.isGhostEdible(ghostType) ) {
			
			return GHOST_EDIBLE;
		}
		else {
			
			/**
			 * if the Pacman is close to a power pill
			 */
			int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();
			int[] powerPillIndices = game.getPowerPillIndices();
			
			for( int ppIndex = 0; ppIndex < powerPillIndices.length; ppIndex++ ) {
				
				if( game.isPowerPillStillAvailable( ppIndex ) ) {
					
					int ppNodeIndex = powerPillIndices[ppIndex];
					
					/**
					 * DO NOT consider the moving direction of Pacman
					 */
					int distance = game.getShortestPathDistance( pacmanNodeIndex, 
							ppNodeIndex );
					
					//it seems that 50 is better than 35
					if( distance < 40 ) {
						
						return GHOST_NEAR_EDIBLE;
					}
				}
			}
			
			return GHOST_NOT_EDIBLE;
		}
	}
	
	
	/**
	 * the reversal direction can be chosen 
	 * since this is possible for Ms. Pac-Man
	 */
	protected int getNearestNPPIndexForPacman( Game game, 
			int pacmanNodeIndex, MOVE direction	)
	{
		
		/**
		 * if currently the object is in an NPP node
		 */
		if( !game.isPipe(pacmanNodeIndex) ) {
			
			/**
			 * for each possible direction
			 */
			MOVE[] possMovs = game.getPossibleMoves( pacmanNodeIndex );
			int minDis = Integer.MAX_VALUE;
			int minNPP = -1;
			for( int movIndex = 0; movIndex < possMovs.length; movIndex++ ) {
				
				int neighborNodeIndex = game.getNeighbour( pacmanNodeIndex, 
						possMovs[movIndex] );
				while( game.isPipe(neighborNodeIndex) ) {
					
					neighborNodeIndex = game.getNeighbour( neighborNodeIndex, 
							possMovs[movIndex] );
				}
				
				int dis = game.getShortestPathDistance( pacmanNodeIndex, 
						neighborNodeIndex );
				if( dis < minDis ) {
					
					minDis = dis;
					minNPP = neighborNodeIndex;
				}
			}
			
			return minNPP;
		}
		/**
		 * else the object is in a corridor
		 */
		else {
			
			/**
			 * move along the direction and find the 
			 * first NPP
			 */
			int neighborNodeIndex = game.getNeighbour( pacmanNodeIndex, direction );
			while( game.isPipe(neighborNodeIndex) ) {
				
				neighborNodeIndex = game.getNeighbour( neighborNodeIndex, 
						direction );
			}
			
			return neighborNodeIndex;
		}
	}
	
	
	/**
	 * note that the path from the ghost node 
	 * to the cut node cannot contain the pacman node
	 */
	protected int getNPPNodeCutPacman( Game game, int pacmanNodeIndex, 
			int ghostNodeIndex, MOVE pacmanLastMov, 
			MOVE ghostLastMov )
	{

		/**
		 * the NPP node currently Ms. Pac-Man is moving toward
		 */
		int towardNPPNodeIndex = -1;
		
		/**
		 * the cut node is an NPP node near to Ms. Pac-Man
		 * and requires that Ms. Pac-Man is not on the path 
		 * from the ghost to it
		 */
		if( !game.isPipe(pacmanNodeIndex) ) {
			
			towardNPPNodeIndex = pacmanNodeIndex;
		}
		/**
		 * else if Ms. Pac-Man is in a corridor
		 */
		else {
			
			/**
			 * first find the NPP node that Ms. Pac-Man 
			 * are moving toward
			 */
			int neighborNodeIndex = game.getNeighbour( pacmanNodeIndex, 
					pacmanLastMov );
			while( game.isPipe(neighborNodeIndex) ) {
				
				neighborNodeIndex = game.getNeighbour( neighborNodeIndex, 
						pacmanLastMov );
			}
			towardNPPNodeIndex = neighborNodeIndex;
		}
		
		/**
		 * whether Ms. Pac-Man is on the path from 
		 * the ghost to this NPP node
		 */
		int[] path = game.getShortestPath( ghostNodeIndex, 
				towardNPPNodeIndex, ghostLastMov );
		boolean onPath = false;
		for( int i = 0; i < path.length; i++ ) {
			
			if( path[i] == pacmanNodeIndex ) {
				
				onPath = true;
				break;
			}
		}
		
		if( towardNPPNodeIndex == pacmanNodeIndex || 
				onPath )
			return -1;
		else {
			
			/**
			 * if not on the path and the ghost can 
			 * reach the node earlier, 
			 * then we have found a cut node
			 */
			int disPacmanToNode = game.getShortestPathDistance( 
					pacmanNodeIndex, towardNPPNodeIndex );
			
			if( disPacmanToNode > path.length ) {
				
				//System.out.println("Find cut node");
				return towardNPPNodeIndex;
			}
			else
				return -1;
		}
	}
	
	
	
	/**
	 * note that direction is the current direction of pacman 
	 * not the reversal direction
	 */
	protected int getNPPNodeBlockPacman( Game game, 
			int pacmanNodeIndex, int ghostNodeIndex, 
			MOVE pacmanLastMov, MOVE ghostLastMov )
	{
		/**
		 * the NPP node backward to Ms. Pac-Man currently
		 */
		int backwardNPPNodeIndex = -1;
		MOVE reverDirection = pacmanLastMov.opposite();
		
		/**
		 * the block node is the nearest NPP node 
		 * to which Ms. Pac-Man is not on the path 
		 * from the ghost
		 */
		if( !game.isPipe(pacmanNodeIndex) ) {
			
			backwardNPPNodeIndex = pacmanNodeIndex;
		}
		/**
		 * else Ms. Pac-Man is in a corridor
		 */
		else {
			
			/**
			 * first find the NPP node which is back to 
			 * that Ms. Pac-Man are moving toward
			 */
			int neighborNodeIndex = game.getNeighbour( pacmanNodeIndex, 
					reverDirection );
			while( game.isPipe(neighborNodeIndex) ) {
				
				neighborNodeIndex = game.getNeighbour( neighborNodeIndex, 
						reverDirection );
			}
			backwardNPPNodeIndex = neighborNodeIndex;
		}
		
		/**
		 * whether Ms. Pac-Man is on the path from 
		 * the ghost to this NPP node
		 */
		int[] path = game.getShortestPath( ghostNodeIndex, 
				backwardNPPNodeIndex, ghostLastMov );
		boolean onPath = false;
		for( int i = 0; i < path.length; i++ ) {
			
			if( path[i] == pacmanNodeIndex ) {
				
				onPath = true;
				break;
			}
		}
		
		if( backwardNPPNodeIndex == pacmanNodeIndex || 
				onPath )
			return -1;
		/**
		 * if not on the path, then we find cut node
		 */
		else {
			
			int disPacmanToNode = game.getShortestPathDistance( 
					pacmanNodeIndex, backwardNPPNodeIndex );
			
			if( disPacmanToNode > path.length ) {
				
				//System.out.println("Find block node");
				return backwardNPPNodeIndex;
			}
			else
				return -1;
		}
	}
	
	/**
	 * this method should be called by an edible ghost 
	 * it is for finding a non-edible ghost which can 
	 * cover this ghost
	 * @return: the node index of the non-edible ghost
	 */
	protected GHOST findCoverProviderGhost( Game game, GHOST ghostType, 
			int pacmanNodeIndex, int ghostNodeIndex, 
			MOVE pacmanLastMov, MOVE ghostLastMov )
	{
		
		if( !game.isGhostEdible(ghostType) ) {
			
			System.out.println("MARLGhostsWithFA->findCoverProviderGhost: The ghost is not edible");
			return null;
		}
		
		//the ghosts that are non-edible and the path does not contain Ms. Pac-Man
		ArrayList<GHOST> rightGhosts = new ArrayList<Constants.GHOST>(); 
		EnumMap<GHOST, Integer> towardNPPNodeIndices = new 
				EnumMap<Constants.GHOST, Integer>(GHOST.class);
		
		/**
		 * find all non-edible ghosts
		 */
		for( GHOST gstType : GHOST.values() ) {
			
			if( gstType == ghostType || 
					game.isGhostEdible( gstType ) || 
					game.getGhostLairTime(gstType) > 0 )
				continue;
			
			//if the ghost is in an NPP node, then its direction is undecidable
			int gstNodeIndex = game.getGhostCurrentNodeIndex( gstType );
			if( !game.isPipe(gstNodeIndex) )
				continue;
			
			//find the NPP node the iterated ghost is moving towards
			//we should move to the node where the cover provider is moving
			//not the current position of the cover provider!!!
			MOVE gstMov = game.getGhostLastMoveMade(gstType);
			
			if( gstMov == MOVE.NEUTRAL )
				continue;
			
			int towardNPPNodeIndex = game.getNextNPPNodeIndex(gstNodeIndex, gstMov);
			
			/**
			 * whether Ms. Pac-Man is on the Path 
			 * from the edible ghost to the moving target of the iterated ghost
			 */
			int[] path = game.getShortestPath( ghostNodeIndex, 
					towardNPPNodeIndex, ghostLastMov );
			boolean onPath = false;
			for( int i = 0; i < path.length; i++ ) {
				
				if( path[i] == pacmanNodeIndex ) {
					
					onPath = true;
					break;
				}
			}
			if( !onPath ) {
				
				rightGhosts.add( gstType );
				towardNPPNodeIndices.put( gstType, towardNPPNodeIndex );
			}
		}
		
		/**
		 * if there is right ghost 
		 * then find the nearest one (or just random select?)
		 */
		if( rightGhosts.size() > 0 ) {
			
			int minDis = Integer.MAX_VALUE;
			int minIndex = -1;
			for( int listIndex = 0; listIndex < rightGhosts.size(); listIndex++ ) {
				
				GHOST gstType = rightGhosts.get( listIndex );
				int towardNPPNodeIndex = towardNPPNodeIndices.get(gstType);
				int dis = game.getShortestPathDistance( ghostNodeIndex, 
						towardNPPNodeIndex, ghostLastMov );
				
				if( dis < minDis ) {
					
					minDis = dis;
					minIndex = listIndex;
				}
			}
			
			GHOST rightGhost = rightGhosts.get( minIndex );
			return rightGhost;
		}
		/**
		 * else we should run away from Ms. Pac-Man
		 */
		else {
			
			return null;
		}
		
	}
	
	/**
	 * this method should be called by non-edible ghost
	 * it is for finding an edible ghost
	 */
	protected GHOST findCoverTargetGhost( Game game, GHOST ghostType, 
			int pacmanNodeIndex, int ghostNodeIndex, 
			MOVE pacmanLastMov, MOVE ghostLastMov	)
	{
		
		if( game.isGhostEdible(ghostType) ) {
			
			System.out.println("MARLGhostsWithFA->findCoverTargetNode: The ghost is edible");
			return null;
		}
		
		//the ghosts that are edible and the path does not contain Ms. Pac-Man
		ArrayList<GHOST> rightGhosts = new ArrayList<Constants.GHOST>(); 
		EnumMap<GHOST, Integer> towardNPPNodeIndices = new 
				EnumMap<Constants.GHOST, Integer>(GHOST.class);
		
		for( GHOST gstType : GHOST.values() ) {
			
			if( gstType == ghostType || 
					!game.isGhostEdible( gstType ) )
				continue;
			
			int gstNodeIndex = game.getGhostCurrentNodeIndex( gstType );
			if( !game.isPipe(gstNodeIndex) )
				continue;
			
			//find the NPP node the iterated ghost is moving towards
			//we should move to the node where the cover provider is moving
			//not the current position of the cover provider!!!
			MOVE gstMov = game.getGhostLastMoveMade(gstType);
			int towardNPPNodeIndex = game.getNextNPPNodeIndex(gstNodeIndex, gstMov);

			
			/**
			 * whether Ms. Pac-Man is on the Path
			 */
			int[] path = game.getShortestPath( ghostNodeIndex, 
					towardNPPNodeIndex, ghostLastMov );
			boolean onPath = false;
			for( int i = 0; i < path.length; i++ ) {
				
				if( path[i] == pacmanNodeIndex ) {
					
					onPath = true;
					break;
				}
			}
			if( !onPath ) {
				
				rightGhosts.add( gstType );
				towardNPPNodeIndices.put( gstType, towardNPPNodeIndex );
			}
		}
		
		/**
		 * if there is right ghost 
		 * then find the nearest one (or just random select?)
		 */
		if( rightGhosts.size() > 0 ) {
			
			int minDis = Integer.MAX_VALUE;
			int minIndex = -1;
			for( int listIndex = 0; listIndex < rightGhosts.size(); listIndex++ ) {
				
				GHOST gstType = rightGhosts.get( listIndex );
				int towardNPPNodeIndex = towardNPPNodeIndices.get(gstType);
				int dis = game.getShortestPathDistance( ghostNodeIndex, 
						towardNPPNodeIndex, ghostLastMov );
				
				if( dis < minDis ) {
					
					minDis = dis;
					minIndex = listIndex;
				}
			}
			
			GHOST rightGhost = rightGhosts.get( minIndex );
			return rightGhost;
		}
		/**
		 * else directly move toward Ms. Pac-Man
		 */
		else {
			
			return null;
		}	
	}
	
	/**
	 * transform a joint strategy index to the strategies of the ghosts
	 */
	protected int[] jntStrategy2Strategies( int jntStrategy )
	{
		if( jntStrategy < 0 || 
				jntStrategy >= Constants.NUM_JOINT_STRATEGIES ) {
			
			System.out.println("MARLGhostsWithFA->jntStrategy2Strategies: Wrong Parameter "+jntStrategy);
			return null;
		}
		
		int x = jntStrategy;
		int[] strategies = new int[Constants.NUM_GHOSTS];
		for( int ghostIndex = 0; ghostIndex < Constants.NUM_GHOSTS; ghostIndex++ ) {
			
			int y = x / ((int) Math.pow(Constants.NUM_GHOST_STRATEGIES_MULTI_AGENT, 
					Constants.NUM_GHOSTS-1-ghostIndex));
			strategies[ghostIndex] = y;
			
			x = x - y * ((int) Math.pow(Constants.NUM_GHOST_STRATEGIES_MULTI_AGENT, 
					Constants.NUM_GHOSTS-1-ghostIndex));
		}
		
		return strategies;
	}
	
	/**
	 * transform the strategies of the ghosts to a joint strategy index
	 */
	protected int strategies2JntStrategy( int[] strategies )
	{
		
		if( strategies == null || 
				strategies.length != Constants.NUM_GHOSTS ) {
			
			System.out.println("MARLGhostsWithFA->strategiesToJntStrategy: Wrong Array");
			return -1;
		}
		
		int jntStrategyIndex = 0;
		for( int ghostIndex = 0; ghostIndex < strategies.length; ghostIndex++ ) {
			
			int sIndex = strategies[ghostIndex];
			jntStrategyIndex += sIndex * Math.pow(Constants.NUM_GHOST_STRATEGIES_MULTI_AGENT, 
					Constants.NUM_GHOSTS-ghostIndex-1);
		}
		
		return jntStrategyIndex;
	}
	
	/**
	 * determine whether a joint strategy is possible
	 */
	protected boolean isJntStrategPossible( Game game, int jntStrategy )
	{
		if( jntStrategy < 0 || 
				jntStrategy >= Constants.NUM_JOINT_STRATEGIES ) {
			
			return false;
		}
		else if( game == null ) {
			
			return false;
		}
		
		int[] strategies = jntStrategy2Strategies(jntStrategy);
		if( strategies == null || 
				strategies.length != Constants.NUM_GHOSTS ) {
			
			return false;
		}
		
		for( GHOST ghostType : GHOST.values() ) {
			
			int ghostIndex = queryGhostIndex(ghostType);
			int strategy = strategies[ghostIndex];
			
			//be careful with STRATEGY BUSY
			if( !isStrategyPossible(game, ghostType, strategy) )
				return false;
		}
		
		return true;
	}
	
	/**
	 * given a ghost, generate all possible other joint actions
	 */
	//should be modified
	/**
	protected ArrayList<int[]> generateOtherJointStrategies( Game game, 
			GHOST ghostType )
	{
		int ghostIndex = queryGhostIndex(ghostType);
		ArrayList<int[]> retList = new ArrayList<int[]>();
		
		int numGhosts = Constants.NUM_GHOSTS;
		int[] strategyIterator = new int[numGhosts];
		
		for( int ghIndex = 0; ghIndex < numGhosts; ghIndex++ ) 
			strategyIterator[ghIndex] = 0;
		
	    int lastGhostIndex = numGhosts - 1;
	    int firstGhostIndex = 0;
	    if( ghostIndex == 0 )
	    	firstGhostIndex = 1;
	    if( ghostIndex == numGhosts - 1 )
	    	lastGhostIndex = numGhosts - 2;
	    
		while( true ) {
			
			int[] strategies = new int[numGhosts];
			boolean possible = true;
			for( GHOST othGhost : GHOST.values() ) {
			
				
				int othGhostIndex = queryGhostIndex(othGhost);
				if( othGhostIndex == ghostIndex )
					continue;
				
				if( !isStrategyPossible(game, 
						othGhost, strategyIterator[othGhostIndex]) ) {
					
					possible = false;
					break;
				}
				
				strategies[othGhostIndex] = strategyIterator[othGhostIndex];
			}
			if( possible )
				retList.add(strategies);
			
			
			for( int ghIndex = lastGhostIndex; ghIndex >= firstGhostIndex; ghIndex-- ) {
				
				if( ghIndex == ghostIndex )
					continue;
				
				strategyIterator[ghIndex] += 1;
				if( ghIndex > firstGhostIndex && 
						strategyIterator[ghIndex] == Constants.NUM_GHOST_STRATEGIES ) 
					strategyIterator[ghIndex] = 0;
				else
					break;
			}
			
			if( strategyIterator[firstGhostIndex] == 
					Constants.NUM_GHOST_STRATEGIES ) {
				
				break;
			}
		}
		
		return retList;
	}
	*/
	
	protected ArrayList<int[]> generateOtherJointStrategies( Game game, 
			GHOST ghostType, ArrayList<GHOST> gamingGhosts )
	{
		
		ArrayList<int[]> retList = new ArrayList<int[]>();
		
		/**
		 * get the gaming ghosts except this ghost
		 */
		ArrayList<GHOST> othGamingGhosts = new ArrayList<Constants.GHOST>();
		for( int i = 0; i < gamingGhosts.size(); i++ ) {
			
			GHOST ghType = gamingGhosts.get(i);
			if( ghostType != ghType )
				othGamingGhosts.add(ghType);
		}
		

		if( othGamingGhosts.size() == 0 ) {
			
			int[] strategies = new int[Constants.NUM_GHOSTS];
			for( GHOST ghType : GHOST.values() ) {
				
				int ghIndex = queryGhostIndex(ghType);
				
				if( game.doesGhostRequireAction(ghType) )
					strategies[ghIndex] = 0;
				else
					strategies[ghIndex] = Constants.GHOST_STRATEGY_BUSY;
			}
			retList.add(strategies);
			return retList;
		}
		
		
		/**
		 * the strategy iterator
		 */
		int numGhosts = Constants.NUM_GHOSTS;
		int[] strategyIterator = new int[numGhosts];
		for( GHOST ghType : GHOST.values() ) {
			
			int ghIndex = queryGhostIndex(ghType);
			
			if( game.doesGhostRequireAction(ghType) )
				strategyIterator[ghIndex] = 0;
			else
				strategyIterator[ghIndex] = Constants.GHOST_STRATEGY_BUSY;
		}
		
		/**
	    int lastGhostIndex = numGhosts - 1;
	    int firstGhostIndex = 0;
	    if( ghostIndex == 0 )
	    	firstGhostIndex = 1;
	    if( ghostIndex == numGhosts - 1 )
	    	lastGhostIndex = numGhosts - 2;
	    */
	    
		while( true ) {
			
			/**
			 * add the current joint strategy to the list
			 * note that we only add possible joint strategy
			 */
			int[] strategies = new int[numGhosts];
			boolean possible = true;
			for( int index = 0; index < othGamingGhosts.size(); index++ ) {
			
				GHOST othGhost = othGamingGhosts.get(index);
				int othGhostIndex = queryGhostIndex(othGhost);
				
				if( !isStrategyPossible(game, 
						othGhost, strategyIterator[othGhostIndex]) ) {
					
					possible = false;
					break;
				}
				
				strategies[othGhostIndex] = strategyIterator[othGhostIndex];
			}
			if( possible ) {
				
				for( GHOST ghType : GHOST.values() ) {
					
					if( !game.doesGhostRequireAction(ghType) ) {
						
						int ghIndex = queryGhostIndex(ghType);
						strategies[ghIndex] = Constants.GHOST_STRATEGY_BUSY;
					}
				}
				
				retList.add(strategies);
			}
			
			/**
			 * move to the next joint strategy
			 */
			for( int index = othGamingGhosts.size() - 1; index >= 0; index-- ) {
				
				GHOST othGhost = othGamingGhosts.get(index);
				int othGhostIndex = queryGhostIndex(othGhost);
				
				/**
				 * for acting ghosts, STRATEGY BUSY cannot be chosen
				 */
				strategyIterator[othGhostIndex] += 1;
				if( index > 0 && 
						strategyIterator[othGhostIndex] == Constants.NUM_GHOST_STRATEGIES_MULTI_AGENT-1 ) {
					
					strategyIterator[othGhostIndex] = 0;
				}
				else 
					break;
			}
			
			/**
			 * whether to continue the while loop
			 * for acting ghosts, STRATEGY BUSY cannot be chosen
			 */
			if( strategyIterator[queryGhostIndex(othGamingGhosts.get(0))] == 
					Constants.NUM_GHOST_STRATEGIES_MULTI_AGENT-1 ) {
				
				break;
			}
		}
		
		return retList;
	}
	
	/**
	 * generate the joint strategies of all ghosts which play a game
	 */
	protected ArrayList<int[]> generateJointStrategies( Game game, 
			ArrayList<GHOST> gamingGhosts )
	{
		
		ArrayList<int[]> retList = new ArrayList<int[]>();
		
		/**
		 * the strategy iterator
		 */
		int numGhosts = Constants.NUM_GHOSTS;
		int[] strategyIterator = new int[numGhosts];
		for( GHOST ghType : GHOST.values() ) {
			
			int ghIndex = queryGhostIndex(ghType);
			
			if( game.doesGhostRequireAction(ghType) )
				strategyIterator[ghIndex] = 0;
			else
				strategyIterator[ghIndex] = Constants.GHOST_STRATEGY_BUSY;
		}
		
	    
		while( true ) {
			
			/**
			 * add the current joint strategy to the list
			 * note that we only add possible joint strategy
			 */
			int[] strategies = new int[numGhosts];
			boolean possible = true;
			for( int index = 0; index < gamingGhosts.size(); index++ ) {
			
				GHOST gamingGhost = gamingGhosts.get(index);
				int gamingGhostIndex = queryGhostIndex(gamingGhost);
				
				if( !isStrategyPossible(game, 
						gamingGhost, strategyIterator[gamingGhostIndex]) ) {
					
					possible = false;
					break;
				}
				
				strategies[gamingGhostIndex] = strategyIterator[gamingGhostIndex];
			}
			if( possible ) {
				
				//for other ghosts not playing the game
				for( GHOST ghType : GHOST.values() ) {
					
					if( !game.doesGhostRequireAction(ghType) ) {
						
						int ghIndex = queryGhostIndex(ghType);
						strategies[ghIndex] = Constants.GHOST_STRATEGY_BUSY;
					}
				}
				
				retList.add(strategies);
			}
			
			/**
			 * move to the next joint strategy
			 */
			for( int index = gamingGhosts.size() - 1; index >= 0; index-- ) {
				
				GHOST gamingGhost = gamingGhosts.get(index);
				int gamingGhostIndex = queryGhostIndex(gamingGhost);
				
				/**
				 * for acting ghosts, STRATEGY BUSY cannot be chosen
				 */
				strategyIterator[gamingGhostIndex] += 1;
				if( index > 0 && 
						strategyIterator[gamingGhostIndex] == Constants.NUM_GHOST_STRATEGIES_MULTI_AGENT-1 ) {
					
					strategyIterator[gamingGhostIndex] = 0;
				}
				else 
					break;
			}
			
			/**
			 * whether to continue the while loop
			 * for acting ghosts, STRATEGY BUSY cannot be chosen
			 */
			if( strategyIterator[queryGhostIndex(gamingGhosts.get(0))] == 
					Constants.NUM_GHOST_STRATEGIES_MULTI_AGENT-1 ) {
				
				break;
			}
		}
		
		
		return retList;
	}
	
	///////////////////////////////////////////////////////
	///////////Read or Write Files/////////////////////////
	///////////////////////////////////////////////////////
	/**
	 * write the value function into files
	 * actually we write the learning parameters theta for each strategy
	 * @param game
	 */
	protected void storeValueFunction( String fileName )
	{
		
		try {
			
			for( GHOST ghostType : GHOST.values() ) {
				
				String dir = fileName + ghostType.toString() +".txt";
				BufferedWriter qWriter = new BufferedWriter(new FileWriter(dir));
				
				//int numStrategies = Constants.NUM_GHOST_STRATEGIES;
				for( int jntStrategy = 0; jntStrategy < Constants.NUM_JOINT_STRATEGIES; jntStrategy++ )
					for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_MULTI_AGENT; fIndex++ ) {
					
					
						double parameter = ghostThetaMap.get(ghostType)[jntStrategy][fIndex];
						qWriter.write( String.valueOf(parameter) );
						qWriter.newLine();
				}
				
				qWriter.close();			
			}

		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	/**
	 * read the value functions from file
	 */
	protected void readValueFunction( String fileName )
	{
		
		try {
			/**
			BufferedReader qReader = new BufferedReader(new FileReader(fileName));
			
			int numStrategies = Constants.NUM_GHOST_STRATEGIES;
			int ghostStrategy = 0;
			int fIndex = 0;
			
			String line = "";
			while( (line = qReader.readLine()) != null) {
				
			    if( line.isEmpty() )
					continue;
			    
			    double qValue = Double.parseDouble( line );
			    theta[ghostStrategy][fIndex] = qValue;
			    
		    	fIndex++;
		    	if( fIndex >= NUM_FEATURES ) {
		    		
		    		fIndex = 0;
		    		ghostStrategy++;
		    		if( ghostStrategy >= numStrategies ) {
		    		
		    			break;
		    		}
		    	}
			}
			
			qReader.close();
			*/
			
			
			for( GHOST ghostType : GHOST.values() ) {
				
				String dir = fileName + ghostType.toString() +".txt";
				BufferedReader qReader = new BufferedReader(new FileReader(dir));
				
				int numStrategies = Constants.NUM_JOINT_STRATEGIES;
				int jntStrategy = 0;
				int fIndex = 0;
				
				String line = "";
				while( (line = qReader.readLine()) != null) {
					
				    if( line.isEmpty() )
						continue;
				    
				    double qValue = Double.parseDouble( line );
				    ghostThetaMap.get(ghostType)[jntStrategy][fIndex] = qValue;
				    
			    	fIndex++;
			    	if( fIndex >= Constants.NUM_FEATURES_MULTI_AGENT ) {
			    		
			    		fIndex = 0;
			    		jntStrategy++;
			    		if( jntStrategy >= numStrategies ) {
			    		
			    			break;
			    		}
			    	}
				}
				
				qReader.close();
			}
			
		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	protected void storeARPS()
	{
		/**
		try {
			
			for( int ghostIndex = 0; ghostIndex < 4; ghostIndex++ ) {
				
				BufferedWriter bufWriter = new BufferedWriter(new 
						FileWriter("./ARPS_"+ghostIndex+".csv"));
				
				for( int ep = 0; ep < Game.EPISODE_NUM; ep++ ) {
					
					double arps = arpsPerEpisode[ghostIndex][ep];
					bufWriter.write(arps+", ");
				}
				bufWriter.close();
			}
			
			BufferedWriter averWriter = new BufferedWriter(new 
					FileWriter("./ARPS_average.csv"));
			for( int ep = 0; ep < Game.EPISODE_NUM; ep++ ) {
				
				double arps = 0;
				for( int ghostIndex = 0; ghostIndex < 4; ghostIndex++ ) {
					
					arps += arpsPerEpisode[ghostIndex][ep];
				}
				arps /= 4;
				
				averWriter.write(arps+", ");
			}
			averWriter.close();
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		*/
		
	}
	
	
	private double computeDistanceBetweenGhosts( GHOST fromGhost, 
			GHOST toGhost, Game game )
	{
		
		if( fromGhost == toGhost )
			return 0.0;
		
		if( game.getGhostLairTime(toGhost) > 0 || 
				game.getGhostLairTime(fromGhost) > 0) {
			
			return game.getCurrentMaxNodeDis() - 1.0;
		}
		else if( game.getGhostLastMoveMade(fromGhost) == MOVE.NEUTRAL ) {
			
			return game.getShortestPathDistance(
					game.getGhostCurrentNodeIndex(fromGhost), 
					game.getGhostCurrentNodeIndex(toGhost));
		}
		else {
			
			return game.getShortestPathDistance(
					game.getGhostCurrentNodeIndex(fromGhost), 
					game.getGhostCurrentNodeIndex(toGhost), 
					game.getGhostLastMoveMade(fromGhost));
		}
	}
	
	protected double computeDisGhostToPacman( Game game, GHOST ghostType )
	{
		double maxNodeDis = game.getCurrentMaxNodeDis();
		
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);
		int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();
		
		if( game.getGhostLairTime(ghostType) > 0 )
			return maxNodeDis-1;
		else if( game.getGhostLastMoveMade(ghostType) == MOVE.NEUTRAL ) {
			
			return game.getShortestPathDistance(ghostNodeIndex, pacmanNodeIndex);
		}
		else {
			return game.getShortestPathDistance(ghostNodeIndex, 
					pacmanNodeIndex, game.getGhostLastMoveMade(ghostType));
		}
	}
	
	protected double computeDisPacmanToGhost( Game game, GHOST ghostType )
	{
		double maxNodeDis = game.getCurrentMaxNodeDis();
		
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);
		int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();
		
		if( game.getGhostLairTime(ghostType) > 0 )
			return maxNodeDis-1;
		else {
			return game.getShortestPathDistance( pacmanNodeIndex, 
					ghostNodeIndex );
		}
	}
	
	protected double computeManhDisGhostToPacman( Game game, GHOST ghostType )
	{
		double maxNodeDis = game.getCurrentMaxNodeDis();
		
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);
		int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();
		
		if( game.getGhostLairTime(ghostType) > 0 )
			return maxNodeDis-1;
		else {
			return game.getManhattanDistance( pacmanNodeIndex, 
					ghostNodeIndex );
		}
	}
	
	protected double computeEucDisGhostToPacman( Game game, GHOST ghostType )
	{
		double maxNodeDis = game.getCurrentMaxNodeDis();
		
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);
		int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();
		
		if( game.getGhostLairTime(ghostType) > 0 )
			return maxNodeDis-1;
		else {
			return game.getEuclideanDistance( pacmanNodeIndex, 
					ghostNodeIndex );
		}
	}
	
	
	protected double computeDisPacmanToPowerPill( Game game )
	{
		double maxNodeDis = game.getCurrentMaxNodeDis();
		
		int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();
		
		boolean ppAvail = false;
		double minDisPacmanToPP = Double.POSITIVE_INFINITY;
		int[] powerPillNodeIndices = game.getPowerPillIndices();
		for( int ppIndex = 0; ppIndex < powerPillNodeIndices.length; ppIndex++ ) {
			
			if( game.isPowerPillStillAvailable(ppIndex) ) {
				
				ppAvail = true;
				int ppNodeIndex = powerPillNodeIndices[ppIndex];
				//for the pacman
				double realDis = game.getShortestPathDistance( 
						pacmanNodeIndex, ppNodeIndex );
				double disPacmanToPP = realDis;
				
				//if the path contains a ghost and the ghost is moving near
				//then we recompute the distance
				if( realDis < 50 ) {
					
					boolean recompute = false;
					int[] path = game.getShortestPath(pacmanNodeIndex, ppNodeIndex);
					for( GHOST ghostType : GHOST.values() ) {
						
						boolean ghostOnPath = false;
						boolean ppOnPath = false;
						int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);
						for( int i = 0; i < path.length; i++ ) {
							
							if( path[i] == ghostNodeIndex ) {
								
								ghostOnPath = true;
								break;
							}
						}
						
						if( !ghostOnPath )
							continue;
						
						int[] ghostPath = game.getShortestPath( ghostNodeIndex, 
								pacmanNodeIndex, game.getGhostLastMoveMade(ghostType) ); 
						for( int i = 0; i < ghostPath.length; i++ ) {
							
							if( ghostPath[i] == ppNodeIndex ) {
								
								ppOnPath = true;
								break;
							}
						}
						
						if( !ppOnPath ) {
							
							recompute = true;
							break;
						}
					}
					
					if( recompute ) {
						
						double maxDis = Double.NEGATIVE_INFINITY;
						MOVE[] possMoves = game.getPossibleMoves(pacmanNodeIndex);
						for( int i = 0; i < possMoves.length; i++ ) {
							
							MOVE mov = possMoves[i];
							double dis = game.getShortestPathDistance(
									pacmanNodeIndex, ppNodeIndex, mov.opposite() );
							
							if( dis > maxDis )
								maxDis = dis;
						}
						
						disPacmanToPP = maxDis;
						
						//System.out.println("RealDis: "+realDis);
						//System.out.println("ModifiedDis: "+maxDis);
					}
				}

				
				if( disPacmanToPP < minDisPacmanToPP ) {
					
					minDisPacmanToPP = disPacmanToPP;
				}
			}
		}
		if( !ppAvail ) {
			
			minDisPacmanToPP = maxNodeDis - 1.0;
		}
		
		return minDisPacmanToPP;
	}
	
	///////////////////////////////////////////////////////
	//////////////////Compute Reward///////////////////////
	///////////////////////////////////////////////////////
	
	
	protected double computeReward( Game lastGame, Game curGame, 
			GHOST ghostType, int lastStrategy )
	{
		double reward = 0.0;
		

		/**
		 * if the game is over
		 */
		if( curGame.gameOver() ) {
			
			//if the game finishes since the pacman is eaten and
			//has no life
			if( curGame.wasPacManEaten() ) {
				
				return computeRewardWhenPacmanEaten( lastGame, curGame, 
						ghostType, lastStrategy );
			}
			
			//if the time is over, the reward is 0
			else
				reward = 0.0;
		}
		/**
		 * if the game is not over but the pacman is eaten
		 * that is, one episode is over
		 */
		else if( curGame.wasPacManEaten() ) {
			
			if( !lastGame.wasPacManEaten() ) {
				
				return computeRewardWhenPacmanEaten( lastGame, curGame, 
						ghostType, lastStrategy );
			}
			else
				reward = 0.0;
		}
		/**
		 * if there is ghost was eaten
		 */
		else if( curGame.wasGhostEaten(ghostType) ){
			
			return computeRewardWhenGhostEaten( lastGame, curGame, 
						ghostType, lastStrategy );
		}
		else {
			
			/**
			 * if other ghost was eaten
			 */
			for( GHOST gstType : GHOST.values() ) {
				
				if( ghostType == gstType )
					continue;
				
				if( curGame.wasGhostEaten(gstType) )
					reward += computeRewardWhenOtherGhostEaten( 
							lastGame, curGame, ghostType, gstType, lastStrategy );
			}
			
			/**
			 * if a power pill was eaten
			 */
			if( curGame.wasPowerPillEaten() ) {
				
				reward += computeRewardWhenPowerPillEaten( 
						lastGame, curGame, ghostType, lastStrategy );
			}
		}
		
		return reward;
	}
	
	/**
	 * compute the reward for a ghost 
	 * when the pacman is eaten
	 */
	private double computeRewardWhenPacmanEaten( Game lastGame, Game curGame, 
			GHOST ghostType, int lastStrategy )
	{
		double reward = 0.0;
		
		/**
		 * the information of the pacman and ghosts in the current 
		 * game should be obtained from the copy of curGame 
		 * since when the pacman is eaten, the level is reset
		 */
		Game copyCurGame = curGame.gameCopyWhenPacmanEaten;
		int ghostNodeIndexWhenEaten = copyCurGame.getGhostCurrentNodeIndex(ghostType);
		int pacmanNodeIndexWhenEaten = copyCurGame.getPacmanCurrentNodeIndex();
		MOVE ghostMovWhenEaten = copyCurGame.getGhostLastMoveMade(ghostType);
		
		/**
		 * if the pacman is eaten by this ghost
		 */
		if( curGame.ghostEatsPacman == ghostType ) {
			
			//System.out.println("Pacman Eaten, Big Reward "+lastStrategy);
			reward = 200;//1.0;//200;
		}
		/**
		 * if the ghost is edible at that time or 
		 * it is in the lair
		 */
		else if( copyCurGame.isGhostEdible(ghostType) || 
				copyCurGame.getGhostLairTime(ghostType) > 0 )
			reward = 0.0;
		/**
		 * if the ghost contribute to the death of pacman 
		 * that is the path from this ghost to the ghost 
		 * which eats pacman contains the position of pacman
		 * and the distance from this ghost to pacman is 
		 * small
		 */
		else {
			
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
			if( disGhostToPacman < 50 && onPath ) {
				
				//System.out.println("Help to eat pacman "+lastStrategy);
				reward = 50;//0.25;//50.0;
			}
			else
				reward = 0.0;
		}
		
		return reward;
	}
	
	/**
	 * compute the reward for a ghost when it is eaten
	 */
	private double computeRewardWhenGhostEaten( Game lastGame, Game curGame, 
			GHOST ghostType, int lastStrategy )
	{
		
		double reward = 0.0;
		
		/**
		 * if the directions of the ghost and 
		 * pacman are the same, this indicates that 
		 * the ghosts are running away, so even if it 
		 * was eaten
		 */
		MOVE pacmanCurMov = curGame.getPacmanLastMoveMade(); //????
		MOVE ghostMovWhenEaten = curGame.ghostsMovWhenEaten.get(ghostType);
		
		/**
		if( pacmanCurMov == ghostMovWhenEaten ) {
			
			if( ghostMovWhenEaten == MOVE.NEUTRAL ) {
				
				System.out.println("computeRewardWhenGhostEaten: NEUTRAL MOVE??");
				reward = 0.0;
			}
			else {
				
				System.out.println("Ghost Eaten when run away "+lastStrategy);
				reward = 10;//0.05;//10;
			}
		}
		else {
			
			//System.out.println("Ghost Eaten when getting closer "+lastStrategy);
			reward = 0;//-10;//-0.05;//-10;
		}
		*/
		
		reward = 0;//-1;//
		
		return reward;
	}
	
	/**
	 * compute the reward for a ghost when other ghost is eaten
	 */
	private double computeRewardWhenOtherGhostEaten( Game lastGame, Game curGame, 
			GHOST ghostType, GHOST eatenGhostType, int lastStrategy )
	{
		
		double reward = 0.0;
		
		/**
		 * if the eaten ghost is not edible 
		 * in the last state, then we know that 
		 * this ghost did not know
		 * 
		 * if in the last game the ghost is edible
		 * then it cannot help
		 */
		if( !lastGame.isGhostEdible(eatenGhostType) ||
				lastGame.isGhostEdible(ghostType) )
			reward = 0.0;
		/**
		 * if the ghost self is edible 
		 * then it cannot help
		 */
		else if( curGame.isGhostEdible(ghostType) ||
				curGame.getGhostLairTime(ghostType) > 0 )
			reward = 0.0;
		/**
		 * a negative reward should be given 
		 * if in the last state this ghost could help 
		 * the eaten ghost but it did not 
		 * (so how to model this situation?)
		 */
		else {
			
			/**
			 * if the distance between the last position of the 
			 * ghost and the position where the eaten ghost was eaten 
			 * is smaller than the distance between the last position 
			 * of pacman and the position where the eaten ghost was eaten, 
			 * then we can infer that this ghost can help but it did not
			 */
			int ghostLastNodeIndex = lastGame.getGhostCurrentNodeIndex(ghostType);
			int pacmanLastNodeIndex = lastGame.getPacmanCurrentNodeIndex();
			MOVE ghostLastMov = lastGame.getGhostLastMoveMade(ghostType);
			int eventNodeIndex = curGame.ghostsPositionWhenEaten.get(eatenGhostType);
			
			int disGhostToEventPos = lastGame.getShortestPathDistance(
					ghostLastNodeIndex, eventNodeIndex, ghostLastMov );
			int disPacmanToEventPos = lastGame.getShortestPathDistance(
					pacmanLastNodeIndex, eventNodeIndex );
			
			if( disGhostToEventPos < disPacmanToEventPos ) {
				
				//System.out.println("Punish for not helping "+lastStrategy);
				reward = -10;//-0.05;//-10;
			}
			else {
				
				reward = 0;
			}
		}
		
		return reward;
	}
	
	/**
	 * compute the reward for a ghost when a power pill is eaten
	 */
	private double computeRewardWhenPowerPillEaten( Game lastGame, Game curGame, 
			GHOST ghostType, int lastStrategy )
	{
		double reward = 0;
		
		
		/**
		 * if the ghost is edible in the last state 
		 * then it cannot protect the pill
		 */
		if( lastGame.isGhostEdible(ghostType) )
			reward = 0.0;
		else {
			
			/**
			 * if the ghost can protect the power pill 
			 * but it did not, then it will be punished 
			 * that is, the distance between the ghost 
			 * and the power pill in the last state is 
			 * smaller than the distance between pacman 
			 * and the power pill in the last state
			 */
			int ghostLastNodeIndex = lastGame.getGhostCurrentNodeIndex(ghostType);
			int pacmanLastNodeIndex = lastGame.getPacmanCurrentNodeIndex();
			MOVE ghostLastMov = lastGame.getGhostLastMoveMade(ghostType);
			
			int disGhostToPowerPill = lastGame.getShortestPathDistance( 
					ghostLastNodeIndex, curGame.nodeIndexPowerPillEaten, ghostLastMov );
			int disPacmanToPowerPill = lastGame.getShortestPathDistance( 
					pacmanLastNodeIndex, curGame.nodeIndexPowerPillEaten );
			if( disGhostToPowerPill < disPacmanToPowerPill ) {
				
				//System.out.println("Punish when not protecting power pill "+lastStrategy);
				reward = -5;//-0.025;//-5;
			}
			else
				reward = 0.0;
		}
		
		return reward;
	}
	

	
}
