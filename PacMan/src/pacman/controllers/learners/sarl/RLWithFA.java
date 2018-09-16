package pacman.controllers.learners.sarl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Random;

import org.omg.PortableServer.ID_ASSIGNMENT_POLICY_ID;

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
public class RLWithFA extends Controller<EnumMap<GHOST,MOVE>> {

	public String valueFunctionFileName = "./valueFunction_RL";
	
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
	 * the number of state features
	 * feature 1: the path distance from the ghost to pacman
	 * feature 2: the path distance from pacman to the ghost
	 * feature 3: the Euclidean distance between the ghost and pacman
	 * feature 4: the Manhattan distance between the ghost and pacman
	 * feature 5: the path distance from the ghost to the nearest power pill
	 * feature 6: the path distance from pacman to the nearest power pill
	 * feature 7: the edible time of the ghost
	 * feature 8: the current time of the level
	 * 
	 * add three more features (the global state):
	 * feature 9: remaining power pills rate
	 * feature 10: remaining pill rate
	 * feature 11: remaining pacman lives rate
	 * 
	 * add more features
	 * the path distance to ghost BLINKY
	 * the path distance to ghost PINKY
	 * the path distance to ghost INKY
	 * the path distance to ghost SUE
	 * the combined feature of the path distance from pacman to the ghost and the edible time
	 * the combined feature of the path distance from pacman to the ghost and the path distance 
	 * from pacman to the nearest power pill
	 * 
	 * all the features are continuous 
	 */
	
	/**
	 * the value function parameter to be learnt
	 * since we use RL control algorithms, so each 
	 * action should have corresponding parameters
	 * 
	 * currently we have only two strategies
	 */
	//protected double[][] theta = new double[Constants.NUM_GHOST_STRATEGIES][NUM_FEATURES];
	
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
	protected EnumMap<GHOST, Integer> curStrategies = 
			new EnumMap<Constants.GHOST, Integer>(GHOST.class);
	
	/**
	 * the strategies of the ghosts in the last step
	 */
	protected EnumMap<GHOST, Integer> lastStrategies = 
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
	
	public RLWithFA( Game game )
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
	
	public RLWithFA( Game game, double alpha, 
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
		/**
		for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES; strategy++ ) 
			for( int fIndex = 0; fIndex < NUM_FEATURES; fIndex++ ) {
			
				theta[strategy][fIndex] = random.nextDouble();//0.0;
		}
		*/
		
		for( GHOST ghostType : GHOST.values() ) {
			
			if( !ghostThetaMap.containsKey(ghostType) ) {
				
				double[][] ghostTheta = 
						new double[Constants.NUM_GHOST_STRATEGIES_SINGLE_AGENT]
								[Constants.NUM_FEATURES_SINGLE_AGENT];
				
				for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES_SINGLE_AGENT; strategy++ ) 
					for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_SINGLE_AGENT; fIndex++ ) {
					
						ghostTheta[strategy][fIndex] = random.nextDouble();//0.0;
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
	
	protected double getQValue( Game game, GHOST ghostType, int ghostStrategy )
	{
		
		/**
		 * note that we can only choose available moves
		 */
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);

		if( ghostNodeIndex == game.getCurrentMaze().lairNodeIndex ) {
			
			//System.out.println("RLGhostsWithFA->epsilonGreedy: Ghost "+ghostType+" is in Lair");
			return 0.0;
		}
		//Q-value in initial node cannot be zero!
		/**
		if( ghostNodeIndex == game.getGhostInitialNodeIndex() ) {
			
			//System.out.println("RLGhostsWithFA->epsilonGreedy: Ghost "+ghostType+" is in initial place");
			return 0.0;
		}
		*/
		//even if the ghost cannot make a decision in this state
		//but it also has Q-values in this state
		/**
		else if( game.isPipe( ghostNodeIndex ) ) {
			
			System.out.println("RLGhostsWithFA->epsilonGreedy: Ghost "+ghostType+" is not in an NPP node");
			return 0.0;
		}
		*/
		
		/**
		 * whether the strateg is available
		 */
		if( !isStrategyPossible(game, ghostType, ghostStrategy) ) {
			
			System.out.println("Strategy Invalid!");
			return 0.0;
		}
		
		
		/**
		 * compute the features of the current state
		 */
		double[][] phi = computeFeatureValueMatrix( game, ghostType, ghostStrategy );
		
		/**
		 * then get the Q-value
		 */
		//double qValue = getQValue( phi );
		double qValue = getQValue( phi, ghostType );
		
		return qValue;
	}
	
	/**
	 * computeFeatureValues for eight-strategy version
	 */
	protected double[][] computeFeatureValueMatrix( Game game, GHOST ghostType, 
			int ghostStrategy )
	{
		
		/**
		 * each strateg has corresponding features
		 */
		double[][] retValues = new double[Constants.NUM_GHOST_STRATEGIES_SINGLE_AGENT]
				[Constants.NUM_FEATURES_SINGLE_AGENT];
		
		/**
		 * compute the feature value
		 */
		double[] featureValues = computeFeatureValues( game, 
				ghostType );
		
		
		/**
		 * note that only ghostStrategy has non-zero feature values
		 * also, ghostStrategy must be available in the current state
		 */
		for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES_SINGLE_AGENT; strategy++ ) {
			
			if( strategy == ghostStrategy && 
					isStrategyPossible( game, ghostType, ghostStrategy ) ) {
				
				for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_SINGLE_AGENT; fIndex++ )
					retValues[strategy][fIndex] = featureValues[fIndex];
			}
			else {
				
				for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_SINGLE_AGENT; fIndex++ )
					retValues[strategy][fIndex] = 0.0;
			}
		}
		
		return retValues;
	}
	
	/**
	 * compute the value of each eight features
	 * called by the method computeFeatureValueMatrix
	 */
	protected double[] computeFeatureValues( Game game, GHOST ghostType )
	{
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);
		int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();
		MOVE ghostLastMov = game.getGhostLastMoveMade(ghostType);
		double maxNodeDis = game.getCurrentMaxNodeDis();
		
		//the ret values
		double[] featureValues = new double[Constants.NUM_FEATURES_SINGLE_AGENT];
		
		/**
		 * feature 1: 
		 * the distance from the ghost to the pacman
		 */
		double disGhostToPacman = computeDisGhostToPacman(game, ghostType);
		featureValues[0] = disGhostToPacman / maxNodeDis;
		
		/**
		 * feature 2:
		 * the distance from the pacman to the ghost
		 */
		double disPacmanToGhost = computeDisPacmanToGhost(game, ghostType);
		featureValues[1] = disPacmanToGhost / maxNodeDis;
		
		
		/**
		 * feature 3:
		 * the Euclidean distance between the ghost and pacman
		 */
		double eucDisGhostPacman = computeEucDisGhostToPacman(game, ghostType);
		featureValues[2] = eucDisGhostPacman / maxNodeDis;
	
		
		/**
		 * feature 4:
		 * the Manhattan distance between the ghost and pacman
		 */
		double mhttDisGhostPacman = computeManhDisGhostToPacman(game, ghostType);
		featureValues[3] = mhttDisGhostPacman / maxNodeDis;
		
		/**
		 * feature 5:
		 * the distance from the ghost to the nearest power pill
		 * feature 6:
		 * the distance from the pacman to the nearest power pill
		 */
		boolean ppAvail = false;
		double minDisGhostToPP = Double.POSITIVE_INFINITY;
		double minDisPacmanToPP = computeDisPacmanToPowerPill(game);//Double.POSITIVE_INFINITY;
		int[] powerPillNodeIndices = game.getPowerPillIndices();
		for( int ppIndex = 0; ppIndex < powerPillNodeIndices.length; ppIndex++ ) {
			
			if( game.isPowerPillStillAvailable(ppIndex) ) {
				
				ppAvail = true;
				
				//for the ghost
				double disGhostToPowerPill = game.getShortestPathDistance( 
						ghostNodeIndex, powerPillNodeIndices[ppIndex], ghostLastMov );
				if( disGhostToPowerPill < minDisGhostToPP ) {
					
					minDisGhostToPP = disGhostToPowerPill;
				}
				
			}
		}
		if( !ppAvail ) {
			
			minDisGhostToPP = maxNodeDis - 1.0;
		}
		
		featureValues[4] = minDisGhostToPP / maxNodeDis;
		featureValues[5] = minDisPacmanToPP / maxNodeDis;
		         
		
		/**
		 * feature 7:
		 * the edible time of the ghost
		 */
		double edibleTime = game.getGhostEdibleTime( ghostType );
		featureValues[6] = edibleTime / Constants.EDIBLE_TIME;
		
		/**
		 * feature 8:
		 * the current time of the level
		 */
		double currentLevelTime = game.getCurrentLevelTime();
		featureValues[7] = currentLevelTime / ((double) Constants.LEVEL_LIMIT);
		
		
		/**
		 * feature 9:
		 * the remaining power pill rates
		 */
		featureValues[8] = game.getRemainingPowerPillRate();
		
		/**
		 * feature 10:
		 * the remaining pill rates
		 */
		featureValues[9] = game.getRemainingPillRate();
		
		/**
		 * feature 11:
		 * the pacman remaining live rates
		 */
		featureValues[10] = game.getPacmanRemainingLifeRate();
		
		/**
		 * features 12- 15:
		 * the distances to all ghosts
		 */
		double disToBLINKY = 0;
		if( ghostType != GHOST.BLINKY ) {
			
			if( game.getGhostLairTime(GHOST.BLINKY) > 0 )
				disToBLINKY = maxNodeDis - 1;
			else if( ghostLastMov == MOVE.NEUTRAL )
				disToBLINKY = game.getShortestPathDistance( ghostNodeIndex, 
						game.getGhostCurrentNodeIndex(GHOST.BLINKY) );
			else {
				disToBLINKY = game.getShortestPathDistance( ghostNodeIndex, 
						game.getGhostCurrentNodeIndex(GHOST.BLINKY), ghostLastMov );
			}
		}
		
		double disToPINKY = 0;
		if( ghostType != GHOST.PINKY ) {
			
			if( game.getGhostLairTime(GHOST.PINKY) > 0 )
				disToPINKY = maxNodeDis - 1;
			else if( ghostLastMov == MOVE.NEUTRAL )
				disToPINKY = game.getShortestPathDistance( ghostNodeIndex, 
						game.getGhostCurrentNodeIndex(GHOST.PINKY) );
			else {
				disToPINKY = game.getShortestPathDistance( ghostNodeIndex, 
						game.getGhostCurrentNodeIndex(GHOST.PINKY), ghostLastMov );
			}
		}
			
		double disToINKY = 0;
		if( ghostType != GHOST.INKY ) {
			
			if( game.getGhostLairTime(GHOST.INKY) > 0 )
				disToINKY = maxNodeDis - 1;
			else if( ghostLastMov == MOVE.NEUTRAL )
				disToINKY = game.getShortestPathDistance( ghostNodeIndex, 
						game.getGhostCurrentNodeIndex(GHOST.INKY) );
			else {
				disToINKY = game.getShortestPathDistance( ghostNodeIndex, 
						game.getGhostCurrentNodeIndex(GHOST.INKY), ghostLastMov );
			}
		}
			
		double disToSUE = 0;
		if( ghostType != GHOST.SUE ) {
			
			if( game.getGhostLairTime(GHOST.SUE) > 0 )
				disToSUE = maxNodeDis - 1;
			else if( ghostLastMov == MOVE.NEUTRAL )
				disToSUE = game.getShortestPathDistance( ghostNodeIndex, 
						game.getGhostCurrentNodeIndex(GHOST.SUE) );
			else {
				disToSUE = game.getShortestPathDistance( ghostNodeIndex, 
						game.getGhostCurrentNodeIndex(GHOST.SUE), ghostLastMov );
			}
		}
			
		featureValues[11] = disToBLINKY / maxNodeDis;
		featureValues[12] = disToPINKY / maxNodeDis;
		featureValues[13] = disToINKY / maxNodeDis;
		featureValues[14] = disToSUE / maxNodeDis;
		
		/**
		 * feature 16:
		 * the combined feature of the distance from pacman to the ghost 
		 * and the edible time
		 */
		featureValues[15] = //edibleTime*disPacmanToGhost / (Constants.EDIBLE_TIME*maxNodeDis);
				//Math.pow(edibleTime/Constants.EDIBLE_TIME, disPacmanToGhost/maxNodeDis);
				Math.pow(disPacmanToGhost/maxNodeDis, edibleTime/Constants.EDIBLE_TIME);
		
		/**
		 * the combined feature of the path distance from pacman to the 
		 * ghost and the path distance from pacman to the nearest power pill
		 */
		featureValues[16] = disPacmanToGhost * minDisPacmanToPP / 
				(maxNodeDis*maxNodeDis);
				//Math.pow(maxNodeDis/(maxNodeDis+disPacmanToGhost), minDisPacmanToPP/maxNodeDis);
		
		//System.out.println(" "+featureValues[15]+" "+featureValues[16]);
		
		return featureValues;
	}
	
	/**
	private double getQValue( double[][] featureValues )
	{
		if( featureValues == null ) {
			
			System.out.println("RLGhostsWithFA->getQValue: NULL Array");
			return 0.0;
		}
		else if( featureValues[0].length != NUM_FEATURES ) {
			
			System.out.println("RLGhostsWithFA->getQValue: Wrong Array Length "+featureValues.length);
			return 0.0;
		}
		
		double retValue = 0.0;
		for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES; strategy++ )
			for( int fIndex = 0; fIndex < NUM_FEATURES; fIndex++ ) {
			
				retValue += theta[strategy][fIndex] * featureValues[strategy][fIndex];
		}
		
		return retValue;
	}
	*/
	
	protected double getQValue( double[][] featureValues, GHOST ghostType )
	{
		if( featureValues == null ) {
			
			System.out.println("RLGhostsWithFA->getQValue: NULL Array");
			return 0.0;
		}
		else if( featureValues[0].length != Constants.NUM_FEATURES_SINGLE_AGENT ) {
			
			System.out.println("RLGhostsWithFA->getQValue: Wrong Array Length "+featureValues.length);
			return 0.0;
		}
		else if( !ghostThetaMap.containsKey(ghostType) ) {
			
			System.out.println("RLGhostsWithFA->getQValue: No Such Ghost");
			return 0.0;
		}
		
		double retValue = 0.0;
		double[][] ghostTheta = ghostThetaMap.get(ghostType);
		for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES_SINGLE_AGENT; strategy++ )
			for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_SINGLE_AGENT; fIndex++ ) {
			
				retValue += ghostTheta[strategy][fIndex] * 
						featureValues[strategy][fIndex];
		}
		
		return retValue;
	}
	
	protected double getQValue( double[] realFeatureValues, GHOST ghostType, 
			int ghostStrategy )
	{
		if( realFeatureValues == null ) {
			
			System.out.println("RLGhostsWithFA->getQValue: NULL Array");
			return 0.0;
		}
		else if( realFeatureValues.length != Constants.NUM_FEATURES_SINGLE_AGENT ) {
			
			System.out.println("RLGhostsWithFA->getQValue: Wrong Array Length "+realFeatureValues.length);
			return 0.0;
		}
		else if( !ghostThetaMap.containsKey(ghostType) ) {
			
			System.out.println("RLGhostsWithFA->getQValue: No Such Ghost");
			return 0.0;
		}
		
		double retValue = 0.0;
		double[][] ghostTheta = ghostThetaMap.get(ghostType);
		
		for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_SINGLE_AGENT; fIndex++ ) {
			
			retValue += ghostTheta[ghostStrategy][fIndex] * 
					realFeatureValues[fIndex];
		}
		
		return retValue;
	}
	
	/**
	 * get the maximal Q-value in a game state
	 */
	protected double getMaxQValue( Game game, GHOST ghostType )
	{
		/**
		 * note that we can only choose available moves
		 */
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);

		if( ghostNodeIndex == game.getCurrentMaze().lairNodeIndex ) {
			
			//System.out.println("RLGhostsWithFA->getMaxQValue: Ghost "+ghostType+" is in Lair");
			return 0.0;
		}
		//Q-value in initial node cannot be zero!
		/**
		if( ghostNodeIndex == game.getGhostInitialNodeIndex() ) {
			
			//System.out.println("RLGhostsWithFA->getMaxQValue: Ghost "+ghostType+" is in initial place");
			return 0.0;
		}
		*/
		//even if the ghost cannot make a decision in this state
		//but it also has Q-values in this state
		/**
		else if( game.isPipe( ghostNodeIndex ) ) {
			
			if( game.wasGhostEaten( ghostType ) )
				System.out.println("RLGhostsWithFA->getMaxQValue: Ghost "+ghostType+" was eaten");
			else {
				
				//System.out.println("QLGhosts->getMaxQValue: Ghost "+ghostType+" is not in an NPP node "+ghostNodeIndex);
			}
			return 0.0;
		}
		*/
		
		double maxQ = Double.NEGATIVE_INFINITY;
		for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES_SINGLE_AGENT; strategy++ ) {
			
			/**
			 * we should skip impossible strategies
			 */
			if( !isStrategyPossible(game, ghostType, strategy) )
				continue;
			
			
			/**
			 * compute the features of the current state
			 */
			double[][] phi = computeFeatureValueMatrix( game, ghostType, strategy );
			//double qValue = getQValue( phi );
			double qValue = getQValue( phi, ghostType );
			if( qValue > maxQ )
				maxQ = qValue;
		}
		
		return maxQ;
	}
	
	/**
	 * get the ghost strategy which has the maximal Qvalue 
	 * in a game state
	 */
	protected int getMaxStrategy( Game game, GHOST ghostType )
	{
		
		/**
		 * note that we can only choose available moves
		 */
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);

		if( ghostNodeIndex == game.getCurrentMaze().lairNodeIndex ) {
			
			//System.out.println("RLGhostsWithFA->getMaxStrategy: Ghost "+ghostType+" is in Lair");
			return Constants.GHOST_STRATEGY_EVADE;
		}
		else if( !game.doesGhostRequireAction(ghostType) ) {
			
			System.out.println("RLGhostsWithFA->getMaxStrategy: Ghost "+ghostType+
					"cannot take an action");
			return Constants.GHOST_STRATEGY_EVADE;
		}
		
		
		/**
		 * first find the maxQ value
		 */
		double maxQ = Double.NEGATIVE_INFINITY;
		for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES_SINGLE_AGENT; strategy++ ) {
			
			/**
			 * we should skip impossible strategies
			 */
			if( !isStrategyPossible(game, ghostType, strategy) )
				continue;
			
			
			/**
			 * compute the features of the current state
			 */
			double[][] phi = computeFeatureValueMatrix( game, ghostType, strategy );
			//double qValue = getQValue( phi );
			double qValue = getQValue( phi, ghostType );
			if( qValue > maxQ )
				maxQ = qValue;
		}
		
		/**
		for( int fIndex = 0; fIndex < NUM_FEATURES; fIndex++ ) {
			
			System.out.print(" "+theta[fIndex]);
		}
		System.out.println();
		*/

		/**
		if( maxQ == Double.NEGATIVE_INFINITY || 
				maxQ == Double.POSITIVE_INFINITY ) {
			
			for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES; strategy++ ) {
				
				double qValue = getQValue( phi, strategy );
				System.out.println(" "+qValue);
			}
		}
		*/
		
		/**
		 * then find all strategies which have a value close to maxQ
		 */
		ArrayList<Integer> maxStrategyList = new ArrayList<Integer>();
		for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES_SINGLE_AGENT; strategy++ ) {
			
			/**
			 * we should skip impossible strategies
			 */
			if( !isStrategyPossible(game, ghostType, strategy) )
				continue;
			
			double[][] phi = computeFeatureValueMatrix( game, ghostType, strategy );
			double qValue = getQValue( phi, ghostType );
			
			if( Math.abs(qValue-maxQ) < 0.00001 ) {
				
				maxStrategyList.add( strategy );
			}
		}
		
		int listSize = maxStrategyList.size();
		//System.out.println("maxQ: "+maxQ);
		return maxStrategyList.get(random.nextInt(listSize));
	}
	
	/**
	 * for epsilon greedy of the learning algorithm
	 */
	protected int epsilonGreedy( Game game, GHOST ghostType, 
			int maxStrategy )
	{
		/**
		 * note that we can only choose available moves
		 */
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);

		if( ghostNodeIndex == game.getCurrentMaze().lairNodeIndex ) {
			
			//System.out.println("RLGhostsWithFA->epsilonGreedy: Ghost "+ghostType+" is in Lair");
			return Constants.GHOST_STRATEGY_EVADE;
		}
		else if( !game.doesGhostRequireAction(ghostType) ) {
			
			System.out.println("RLGhostsWithFA->epsilonGreedy: Ghost "+ghostType+
					"cannot take an action");
			return Constants.GHOST_STRATEGY_EVADE;
		}
		
		if( random.nextDouble() < EPSILON ) {
			
			/**
			 * find all possible strategies
			 */
			ArrayList<Integer> possStrategies = getPossibleStrategies( game, 
					ghostType ); 
			int numPossStrategies = possStrategies.size();
			
			
			return possStrategies.get( random.nextInt(numPossStrategies) );
			
			
			//return random.nextInt(Constants.NUM_GHOST_STRATEGIES);
		}
		else {
			
			return maxStrategy;
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
	
	protected void setQValue( double[][] parameters, int ghostStrategy, 
			GHOST ghostType )
	{
		if( parameters == null ) {
			
			System.out.println("RLGhostsWithFA->setQValue: NULL Array");
			return;
		}
		else if( parameters.length != Constants.NUM_FEATURES_SINGLE_AGENT ) {
			
			System.out.println("RLGhostsWithFA->setQValue: Wrong Array Length");
			return;
		}
		else if( ghostStrategy < 0 || ghostStrategy > Constants.NUM_GHOST_STRATEGIES_SINGLE_AGENT ) {
			
			System.out.println("RLGhostsWithFA->setQValue: Wrong Ghost Strategt");
			return;
		}
		else if( !ghostThetaMap.containsKey(ghostType) ) {
			
			System.out.println("RLGhostsWithFA->setQValue: No Such Ghost");
			return;
		}
		
		for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES_SINGLE_AGENT; strategy++ )
			for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_SINGLE_AGENT; fIndex++ ) {
			
				ghostThetaMap.get(ghostType)[strategy][fIndex] = 
						parameters[strategy][fIndex];
		}	
	}
	
	/**
	 * getMoveAccordingToStrategy for two-strategy version
	 *
	protected MOVE getMoveAccordingToStrategy( Game game, GHOST ghostType, 
			int strategy )
	{
		int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);
		MOVE ghostLastMove = game.getGhostLastMoveMade( ghostType );
		
		if( strategy == Constants.GHOST_STRATEGY_MOVE_AWAY ) {
			
			
			//the to node is the current position of Pacman 
			//or the position to which the pacman will go???
			return game.getApproximateNextMoveAwayFromTarget( ghostNodeIndex, 
					pacmanNodeIndex, ghostLastMove, DM.PATH );
			
		}
		else if( strategy == Constants.GHOST_STRATEGY_GET_CLOSER ) {
			
			//the to node is the current position of pacman 
			//or the position to which the pacman will go???
			return game.getApproximateNextMoveTowardsTarget( ghostNodeIndex, 
					pacmanNodeIndex, ghostLastMove, DM.PATH );
		}
		else {
			
			System.out.println("RLGhostsWithFA->getMoveAccordingToStrategy: Bad Strategy!");
			return MOVE.UP;
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
						ghostNodeIndex, pacmanNodeIndex, ghostLastMove, 
						DM.PATH );
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
		for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES_SINGLE_AGENT; strategy++ ) {
			
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
		case Constants.GHOST_STRATEGY_CUT: {
			
			retValue = isStrategyCutPossible(game, ghostType);
			break;
		}
		case Constants.GHOST_STRATEGY_BLOCK: {
			
			retValue = isStrategyBlockPossible( game, ghostType );
			break;
		}
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
			
			if( !game.isGhostEdible(ghostType) ) {
				
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
	
	private double computeDisGhostToPacman( Game game, GHOST ghostType )
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
	
	private double computeDisPacmanToGhost( Game game, GHOST ghostType )
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
	
	private double computeManhDisGhostToPacman( Game game, GHOST ghostType )
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
	
	private double computeEucDisGhostToPacman( Game game, GHOST ghostType )
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
			
			System.out.println("RLGhostsWithFA->findCoverProviderGhost: The ghost is not edible");
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
			
			System.out.println("RLGhostsWithFA->findCoverTargetNode: The ghost is edible");
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
			
			/**
			BufferedWriter qWriter = new BufferedWriter(new FileWriter(fileName));
			
			//int numStrategies = Constants.NUM_GHOST_STRATEGIES;
			for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES; strategy++ )
				for( int fIndex = 0; fIndex < NUM_FEATURES; fIndex++ ) {
				
				
					double parameter = theta[strategy][fIndex];
					qWriter.write( String.valueOf(parameter) );
					qWriter.newLine();
			}
			
			qWriter.close();
			*/
			for( GHOST ghostType : GHOST.values() ) {
				
				String dir = fileName + ghostType.toString() +".txt";
				BufferedWriter qWriter = new BufferedWriter(new FileWriter(dir));
				
				//int numStrategies = Constants.NUM_GHOST_STRATEGIES;
				for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES_SINGLE_AGENT; strategy++ )
					for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_SINGLE_AGENT; fIndex++ ) {
					
					
						double parameter = ghostThetaMap.get(ghostType)[strategy][fIndex];
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
			
			System.out.println("Read "+fileName);
			
			for( GHOST ghostType : GHOST.values() ) {
				
				String dir = fileName + ghostType.toString() +".txt";
				BufferedReader qReader = new BufferedReader(new FileReader(dir));
				
				int numStrategies = Constants.NUM_GHOST_STRATEGIES_SINGLE_AGENT;
				int ghostStrategy = 0;
				int fIndex = 0;
				
				String line = "";
				while( (line = qReader.readLine()) != null) {
					
				    if( line.isEmpty() )
						continue;
				    
				    double qValue = Double.parseDouble( line );
				    ghostThetaMap.get(ghostType)[ghostStrategy][fIndex] = qValue;
				    
			    	fIndex++;
			    	if( fIndex >= Constants.NUM_FEATURES_SINGLE_AGENT ) {
			    		
			    		fIndex = 0;
			    		ghostStrategy++;
			    		if( ghostStrategy >= numStrategies ) {
			    		
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
	
	
	/**
	 * compute the reward when the ghost choose to 
	 * protect a power pill in the last state
	 */
	private double computeRewardWhenProtectPP( Game lastGame, Game curGame, 
			GHOST ghostType )
	{
		double reward = 0.0;
		
		//the target power pill
		int targetPPIndex = targetPowerPillIndices.get( ghostType );
		if( targetPPIndex == -1 )
			reward = 0.0;
		//the power pill has been eaten or the ghost is edible now
		//both cases are treated as failure for power pill protection
		else if( !curGame.isPowerPillStillAvailable(targetPPIndex) || 
				curGame.isGhostEdible(ghostType) )
			reward = -0.5;
		else {
			
			int targetPPNodeIndex = curGame.getPowerPillIndices()[targetPPIndex];
			int pacmanNodeIndex = curGame.getPacmanCurrentNodeIndex();
			int ghostNodeIndex = curGame.getGhostCurrentNodeIndex(ghostType);
			int disPacmanToPP = curGame.getShortestPathDistance( pacmanNodeIndex, 
					targetPPNodeIndex );
			MOVE ghostLastMove = curGame.getGhostLastMoveMade(ghostType);
			int disGhostToPP = curGame.getShortestPathDistance( ghostNodeIndex, 
					targetPPNodeIndex, ghostLastMove );
			
			//if the ghost is closer to the power pill than Ms. Pac-Man
			//then we say protection succeeds, otherwise it fails
			if( disGhostToPP <= disPacmanToPP )
				reward = 0.5;
			else
				reward = -0.5;
		}
		
		return reward;
	}
	
	//double check
	/**
	 * compute the reward when the ghost choose to 
	 * protect another ghost in the last state
	 */
	private double computeRewardWhenCoveringGhost( Game lastGame, 
			Game curGame, GHOST ghostType )
	{
		double reward = 0.0;
		
		if( !ghostsNeedCover.containsKey(ghostType) )
			reward = 0.0;
		//if now this ghost becomes edible
		if( curGame.isGhostEdible(ghostType) ) {
			reward = -0.5;
		}
		else {
			
			GHOST targetGhost = ghostsNeedCover.get(ghostType);
			int targetNodeIndex = lastGame.getNextNPPNodeIndex( 
					lastGame.getGhostCurrentNodeIndex(targetGhost), 
					lastGame.getGhostLastMoveMade(targetGhost) );
			
			//we do not encourage protection from far away ghosts
			int disGhToTgtLast = lastGame.getShortestPathDistance( 
					lastGame.getGhostCurrentNodeIndex(ghostType), 
					targetNodeIndex, 
					lastGame.getGhostLastMoveMade(ghostType) );
			if( disGhToTgtLast > 70 ) 
				reward = 0.0;//or -0.1?
			//if the ghost has been eaten since last decision, then failed
			else if( curGame.wasGhostEaten( targetGhost ) || 
					curGame.getGhostLairTime( targetGhost ) > 0 )
				reward = -0.5;
			//else the target ghost is still edible or becomes non-edible
			else {
				
				int disGhToGhCur = curGame.getShortestPathDistance( 
						curGame.getGhostCurrentNodeIndex(ghostType), 
						curGame.getGhostCurrentNodeIndex(targetGhost), 
						curGame.getGhostLastMoveMade(ghostType) );//whether consider the direction??
				//if currently the two ghosts are still too far away
				if( disGhToGhCur > 70 )
					reward = 0.0;//or -0.1?
				//if the target ghost is edible
				else if( curGame.isGhostEdible(targetGhost) ) {
					
					int disPacmanToTargetGhost = curGame.getShortestPathDistance(
							curGame.getPacmanCurrentNodeIndex(), 
							curGame.getGhostCurrentNodeIndex(targetGhost) );
					
					//this means that this cover will succeed
					if( disGhToGhCur <= disPacmanToTargetGhost )
						reward = 0.1;
					else
						reward = -0.5;
				}
				//if the target ghost becomes non-edible
				else 
					reward = 0.5;//0.1 or 0.5 / dis???
			}
		}
		
		return reward;
	}
	
	//double check
	/**
	 * compute the reward when the ghost choose to 
	 * find a cover in the last state
	 */
	private double computeRewardWhenFindingCover( Game lastGame, 
			Game curGame, GHOST ghostType )
	{
		double reward = 0.0;
		
		if( !ghostsProvideCover.containsKey(ghostType) )
			reward = 0.0;
		else {
			
			GHOST coverProviderGhost = ghostsProvideCover.get(ghostType);
			int targetNodeIndex = lastGame.getNextNPPNodeIndex( 
					lastGame.getGhostCurrentNodeIndex(coverProviderGhost), 
					lastGame.getGhostLastMoveMade(coverProviderGhost));
			
			int disGhToTgtLast = lastGame.getShortestPathDistance( 
					lastGame.getGhostCurrentNodeIndex(ghostType), 
					targetNodeIndex, 
					lastGame.getGhostLastMoveMade(ghostType) );
			int disPacmanToGhLast = lastGame.getShortestPathDistance( 
					lastGame.getPacmanCurrentNodeIndex(), 
					lastGame.getGhostCurrentNodeIndex(ghostType) );
			int disGhToGhCur = curGame.getShortestPathDistance( 
					curGame.getGhostCurrentNodeIndex(ghostType), 
					curGame.getGhostCurrentNodeIndex(coverProviderGhost), 
					curGame.getGhostLastMoveMade(ghostType) );
			int disPacmanToGhCur = curGame.getShortestPathDistance( 
					curGame.getPacmanCurrentNodeIndex(), 
					curGame.getGhostCurrentNodeIndex(ghostType) );
			
			//we do not encourage protection from far away ghosts
			if( disGhToTgtLast > 70 )
				reward = 0.0;//or -0.1
			//if the ghost has been eaten???
			else if( curGame.wasGhostEaten(ghostType) || 
					curGame.getGhostLairTime(ghostType) > 0 ) {
				
				System.out.println("Get Reward when ghost finds cover but was eaten");
				
				//if being eaten is inevitable
				if( disPacmanToGhLast / 2 < disGhToTgtLast ) //the speed of pacman?? 
					reward = 0.0;
				else
					reward = -0.5;
			}
			//if the ghost is still edible
			else if( curGame.isGhostEdible(ghostType) ) {
				
				//if now the provide becomes edible
				if( curGame.isGhostEdible(coverProviderGhost) )
					reward = -0.5;
				//if this cover can be in time
				else if( disPacmanToGhCur / 2 > disGhToGhCur )
					reward = 0.5;
				//if this cover cannot help
				else
					reward = 0.5;
			}
			//else the ghost becomes non-edible
			else {
				
				//if the pacman is too far away in the last state
				//then finding a cover is not so important
				if( disPacmanToGhLast > 120 ) 
					reward = 0.0;
				//if now the two ghosts are too far away, then valid cover 
				//did not form
				else if( disGhToGhCur > 50 )
					reward = 0.0;
				//else we treat it as a successful cover
				else
					reward = 0.5;
			}
		}
		
		return reward;
	}
	
	private double computeDisPacmanToPowerPill( Game game )
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
	
}
