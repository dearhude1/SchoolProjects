package pacman.controllers.learners.marl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;

import pacman.game.Constants;
import pacman.game.Game;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;

/**
 * NegoQ(lambda) algorithm with policy transfer
 * @author dell
 *
 */
public class NegoQLambdaPT extends NegoQLambda {

	/**
	 * single-agent value function parametrs of each ghost
	 */
	protected HashMap<GHOST, double[][]> singleAgentThetaMap = 	
			new HashMap<Constants.GHOST, double[][]>();
	
			
	private static final int POLICY_TRANSFER_EPISODE_NUM = 2000;
	
	public NegoQLambdaPT( Game game, double lambda, boolean bLearn )
	{
		
		super(game, lambda, bLearn);
		
		
		valueFunctionFileName = "./valueFunction_NegoQLambdaVFT_";
		if( !doesLearn ) {
			
			readValueFunction( valueFunctionFileName );
		}

		else {
			
			/**
			 * initialize the single agent theta map
			 */
			for( GHOST ghostType : GHOST.values() ) {
				
				if( !singleAgentThetaMap.containsKey(ghostType) ) {
					
					double[][] singleAgentTheta = 
							new double[Constants.NUM_GHOST_STRATEGIES_SINGLE_AGENT]
									[Constants.NUM_FEATURES_SINGLE_AGENT];
					
					for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES_SINGLE_AGENT; strategy++ ) 
						for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_SINGLE_AGENT; fIndex++ ) {
						
							singleAgentTheta[strategy][fIndex] = 0.0;
					}
					
					singleAgentThetaMap.put( ghostType, singleAgentTheta );
				}
			}
			
			/**
			 * read the single agent value function from files
			 */
			readSingleAgentParameters();
			
		}
	}
	
	public NegoQLambdaPT( Game game, double alpha, 
			double gamma, double epsilon, double lambda, 
			boolean bLearn )
	{
		super(game, alpha, gamma, epsilon, lambda, bLearn);
		
		valueFunctionFileName = "./valueFunction_NegoQLambdaVFT_";
		if( !doesLearn ) {
			
			readValueFunction( valueFunctionFileName );
		}

		else {
			
			/**
			 * initialize the single agent theta map
			 */
			for( GHOST ghostType : GHOST.values() ) {
				
				if( !singleAgentThetaMap.containsKey(ghostType) ) {
					
					double[][] singleAgentTheta = 
							new double[Constants.NUM_GHOST_STRATEGIES_SINGLE_AGENT]
									[Constants.NUM_FEATURES_SINGLE_AGENT];
					
					for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES_SINGLE_AGENT; strategy++ ) 
						for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_SINGLE_AGENT; fIndex++ ) {
						
							singleAgentTheta[strategy][fIndex] = 0.0;
					}
					
					singleAgentThetaMap.put( ghostType, singleAgentTheta );
				}
			}
			
			/**
			 * read the single agent value function from files
			 */
			readSingleAgentParameters();
			
		}
	}
	
	private void readSingleAgentParameters()
	{
		
		try {
			
			System.out.println("Load Single-Agent Value Function Parameters");
			
			String fileNamePrefix = "./valueFunction_RLSTDQMax_";
			for( GHOST ghostType : GHOST.values() ) {
				
				String dir = fileNamePrefix + ghostType.toString() +".txt";
				BufferedReader qReader = new BufferedReader(new FileReader(dir));
				
				int strNumSingleAgent = Constants.NUM_GHOST_STRATEGIES_SINGLE_AGENT;
				int ghostStrategy = 0;
				int fIndex = 0;
				
				String line = "";
				while( (line = qReader.readLine()) != null) {
					
				    if( line.isEmpty() )
						continue;
				    
				    double qValue = Double.parseDouble( line );
				    singleAgentThetaMap.get(ghostType)[ghostStrategy][fIndex] = qValue;
				    
			    	fIndex++;
			    	if( fIndex >= Constants.NUM_FEATURES_SINGLE_AGENT ) {
			    		
			    		fIndex = 0;
			    		ghostStrategy++;
			    		if( ghostStrategy >= strNumSingleAgent ) {
			    		
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
		
		/**
		 * the equilibrium strategy when using policy transfer
		 * that is, we use the transferred policy as the equilibrium 
		 * strategy in this state
		 */
		int[] equilStrategiesPT = new int[Constants.NUM_GHOSTS];
		for( int ghostIndex = 0; ghostIndex < Constants.NUM_GHOSTS; ghostIndex++ ) {
		
			equilStrategiesPT[ghostIndex] = chosenStrategies[ghostIndex];
		}
		
		for(GHOST ghostType : GHOST.values()) {
			
			if( !game.doesGhostRequireAction(ghostType) ) {
				
				continue;
			}
			
			int ghostIndex = queryGhostIndex(ghostType);
			
			/**
			 * if we are in policy transfer episodes
			 */
			int chosenStrategy = -1;
			if( game.getCurrentEpisode() <= POLICY_TRANSFER_EPISODE_NUM ) {
				
				int maxStrategy = getSingleAgentMaxStrategy(game, ghostType);
				chosenStrategies[ghostIndex] = maxStrategy;
				equilStrategiesPT[ghostIndex] = maxStrategy;
				chosenStrategy = maxStrategy;
			}
			else {
				
				/**
				 * do epsilon-greedy
				 */
				int computedStrategy = chosenStrategies[ghostIndex];
				chosenStrategy = epsilonGreedy(game, ghostType, computedStrategy);
				
				//we should record the chosen strategy for the ghost
				chosenStrategies[ghostIndex] = chosenStrategy;
			}
			

			/**
			 * choose the real move according to the strategy
			 */
			MOVE chosenMove = getMoveAccordingToStrategy(game, ghostType, chosenStrategy);
			
			/**
			 * set the move of the ghost
			 */
			curMoves.remove( ghostType );
			curMoves.put( ghostType, chosenMove );
			
		}
		
		
		/**
		 * record the executed joint strategy
		 * and set the equilibrium strategy
		 */
		int chosenJntStrategy = strategies2JntStrategy(chosenStrategies);
		int equilStrategy = -1;
		if( game.getCurrentEpisode() <= POLICY_TRANSFER_EPISODE_NUM ) {
			
			equilStrategy = strategies2JntStrategy(equilStrategiesPT);
		}
		else {
			
			equilStrategy = optJntStrategy;
		}
		
		for(GHOST ghostType : GHOST.values()) {
			
			if( !game.doesGhostRequireAction(ghostType) )
				continue;
			
			/**
			 * then update the value function
			 */
			if( doesLearn ) {
				
				updateValueFunction( game, ghostType, equilStrategy );
			}
			
			/**
			 * store the last game state
			 */
			lastGames.put( ghostType, game );
			
			/**
			 * store the equilibrium of this state
			 */
			if( lastEquilibria.containsKey(ghostType) )
				lastEquilibria.remove(ghostType);
			lastEquilibria.put(ghostType, equilStrategy);
			

			/**
			 * store the chosen joint strategy in this state
			 */
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
	
	
	/**
	 * compute the value of each eight features
	 * called by the method computeFeatureValueMatrix
	 */
	protected double[] computeSingleAgentFeatureValues( Game game, GHOST ghostType )
	{
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);
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
	
	
	protected double getSingleAgentQValue( double[] realFeatureValues, GHOST ghostType, 
			int ghostStrategy )
	{
		if( realFeatureValues == null ) {
			
			System.out.println("NegoQLambdaPT->getSingleAgentQValue: NULL Array");
			return 0.0;
		}
		else if( realFeatureValues.length != Constants.NUM_FEATURES_SINGLE_AGENT ) {
			
			System.out.println("NegoQLambdaPT->getSingleAgentQValue: Wrong Array Length "+realFeatureValues.length);
			return 0.0;
		}
		else if( !ghostThetaMap.containsKey(ghostType) ) {
			
			System.out.println("NegoQLambdaPT->getSingleAgentQValue: No Such Ghost");
			return 0.0;
		}
		
		double retValue = 0.0;
		double[][] ghostTheta = singleAgentThetaMap.get(ghostType);
		
		for( int fIndex = 0; fIndex < Constants.NUM_FEATURES_SINGLE_AGENT; fIndex++ ) {
			
			retValue += ghostTheta[ghostStrategy][fIndex] * 
					realFeatureValues[fIndex];
		}
		
		return retValue;
	}
	
	
	protected int getSingleAgentMaxStrategy( Game game, GHOST ghostType )
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
			
			System.out.println("NegoQLambdaPT->getSingleAgentMaxStrategy: Ghost "+ghostType+
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
			double[] phi = computeSingleAgentFeatureValues(game, ghostType);
			double qValue = getSingleAgentQValue(phi, ghostType, strategy);
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
			
			double[] phi = computeSingleAgentFeatureValues(game, ghostType);
			double qValue = getSingleAgentQValue(phi, ghostType, strategy);
			
			if( Math.abs(qValue-maxQ) < 0.00001 ) {
				
				maxStrategyList.add( strategy );
			}
		}
		
		int listSize = maxStrategyList.size();
		//System.out.println("maxQ: "+maxQ);
		return maxStrategyList.get(random.nextInt(listSize));
	}
	
	


}
