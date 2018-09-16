package pacman.controllers.learners.marl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import pacman.game.Constants;
import pacman.game.Game;
import pacman.game.Constants.GHOST;

public class UCEQLambdaVFT extends UCEQLambda {

	/**
	 * single-agent value function parametrs of each ghost
	 */
	protected HashMap<GHOST, double[][]> singleAgentThetaMap = new HashMap<Constants.GHOST, double[][]>();
			
	public UCEQLambdaVFT( Game game, double lambda, boolean bLearn )
	{
				
		super(game, lambda, bLearn);
				
				
		valueFunctionFileName = "./valueFunction_UCEQLambdaVFT_";
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
			readSingleAgentValueFunction();
			
			/**
			 * transfer the single-agent value function parameters
			 */
			transfer();
		}
	}

	public UCEQLambdaVFT( Game game, double alpha, 
			double gamma, double epsilon, double lambda, 
			boolean bLearn )
	{
		super(game, alpha, gamma, epsilon, lambda, bLearn);
		
		valueFunctionFileName = "./valueFunction_UCEQLambdaVFT_";
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
			readSingleAgentValueFunction();
			
			/**
			 * transfer the single-agent value function parameters
			 */
			transfer();
		}
	}
	
	private void readSingleAgentValueFunction()
	{
		
		try {
			
			System.out.println("Load Single-Agent Value Function Parameters");
			
			String fileNamePrefix = "./valueFunction_GreedyGQ_";
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

	
	private void transfer()
	{
		
		for( GHOST ghostType : GHOST.values() ) {
			
			int ghostIndex = queryGhostIndex(ghostType);
			
			/**
			 * the first dimension of the single-agent value function is strategy index
			 * the second dimension of the single-agent value function is feature index
			 */
			for( int strategy = 0; strategy < Constants.NUM_GHOST_STRATEGIES_SINGLE_AGENT; strategy++ ) {
				
				/**
				 * for each joint strategy of which the strategy of the 
				 * iterated ghost is the current strategy
				 */
				for( int jntStrategy = 0; jntStrategy < Constants.NUM_JOINT_STRATEGIES; jntStrategy++ ) {
					
					int[] strategies = jntStrategy2Strategies(jntStrategy);
					if( strategies[ghostIndex] != strategy )
						continue;
					
					/**
					 * for each single-agent feature, find the 
					 * corresponding feature index in the multi-agent features
					 * and directly transfer the theta value
					 */
					for( int saFIndex = 0; saFIndex < Constants.NUM_FEATURES_SINGLE_AGENT; saFIndex++ ) {
						
						int maFIndex = featureMap2( ghostType, saFIndex );
						if( maFIndex == -1 )
							continue;
						else 
							ghostThetaMap.get(ghostType)[jntStrategy][maFIndex] = 
								singleAgentThetaMap.get(ghostType)[strategy][saFIndex];
					}
				}
			}
		}
		
		System.out.println("Successfully transferred");
		
	}
	
	private int featureMap( GHOST ghostType, int saFIndex )
	{
		int maFIndex = -1;
		
		switch( saFIndex ) {
		
		//single-agent feature 0 is the distance from the ghost to pacman
		case 0:{
			
			if( ghostType == GHOST.BLINKY )
				maFIndex = 5;
			else if( ghostType == GHOST.PINKY )
				maFIndex = 6;
			else if( ghostType == GHOST.INKY )
				maFIndex = 7;
			else 
				maFIndex = 8;
			break;
		}
		//single-agent feature 1 is the distance from pacman to the ghost
		case 1: {
			
			if( ghostType == GHOST.BLINKY )
				maFIndex = 1;
			else if( ghostType == GHOST.PINKY )
				maFIndex = 2;
			else if( ghostType == GHOST.INKY )
				maFIndex = 3;
			else 
				maFIndex = 4;
			break;
		}
		//single-agent feature 2 is the euclidean distance between ghost and pacman
		case 2: {
			
			if( ghostType == GHOST.BLINKY )
				maFIndex = 9;
			else if( ghostType == GHOST.PINKY )
				maFIndex = 10;
			else if( ghostType == GHOST.INKY )
				maFIndex = 11;
			else 
				maFIndex = 12;
			break;
		}
		//single-agent feature 3 is the manhattan distance between ghost and pacman
		case 3: {
			
			if( ghostType == GHOST.BLINKY )
				maFIndex = 13;
			else if( ghostType == GHOST.PINKY )
				maFIndex = 14;
			else if( ghostType == GHOST.INKY )
				maFIndex = 15;
			else 
				maFIndex = 16;
			break;
		}
		//single-agent feature 4 is the distance from ghost to power pill
		case 4: {
			
			if( ghostType == GHOST.BLINKY )
				maFIndex = 17;
			else if( ghostType == GHOST.PINKY )
				maFIndex = 18;
			else if( ghostType == GHOST.INKY )
				maFIndex = 19;
			else 
				maFIndex = 20;
			break;
		}
		//single-agent feature 5 is the distance from pacman to power pill
		case 5: {
			
			maFIndex = 0;
			break;
		}
		//single-agent feature 6 is the edible time of the ghost
		case 6: {
			
			if( ghostType == GHOST.BLINKY )
				maFIndex = 21;
			else if( ghostType == GHOST.PINKY )
				maFIndex = 22;
			else if( ghostType == GHOST.INKY )
				maFIndex = 23;
			else 
				maFIndex = 24;
			break;
		}
		//single-agent feature 7 is the level time
		case 7: {
			
			 maFIndex = 25;
			break;
		}
		//single-agent feature 8 is the power pill rate
		case 8: {
			
			maFIndex = 26;
			break;
		}
		//single-agent feature 9 is the pill rate
		case 9: {
			
			maFIndex = 27;
			break;
		}
		//single-agent feature 10 is the pacman life rate
		case 10: {
			
			maFIndex = 28;
			break;
		}
		/**
		 * single-agent features 11-14 are the distances from 
		 * the ghost to the other ghosts including itself
		 * 11: distance to Blinky
		 * 12: distance to Pinky
		 * 13: distance to Inky
		 * 14: distance to Sue
		 */
		case 11: {
			
			if( ghostType == GHOST.BLINKY )
				maFIndex = -1;
			else if( ghostType == GHOST.PINKY )
				maFIndex = 32;
			else if( ghostType == GHOST.INKY )
				maFIndex = 35;
			else 
				maFIndex = 38;
			break;
		}
		case 12: {
			
			if( ghostType == GHOST.BLINKY )
				maFIndex = 29;
			else if( ghostType == GHOST.PINKY )
				maFIndex = -1;
			else if( ghostType == GHOST.INKY )
				maFIndex = 36;
			else 
				maFIndex = 39;
			break;
		}
		case 13: {
			
			if( ghostType == GHOST.BLINKY )
				maFIndex = 30;
			else if( ghostType == GHOST.PINKY )
				maFIndex = 33;
			else if( ghostType == GHOST.INKY )
				maFIndex = -1;
			else 
				maFIndex = 40;
			break;
		}
		case 14: {
			
			if( ghostType == GHOST.BLINKY )
				maFIndex = 31;
			else if( ghostType == GHOST.PINKY )
				maFIndex = 34;
			else if( ghostType == GHOST.INKY )
				maFIndex = 37;
			else 
				maFIndex = -1;
			break;
		}
		//other single-agent features do not appear in multi-agent features
		default: {
			
			break;
		}
		}
		
		return maFIndex;
	}

	
	
	private int featureMap2( GHOST ghostType, int saFIndex )
	{
		int maFIndex = -1;
		
		switch( saFIndex ) {
		
		//single-agent feature 0 is the distance from the ghost to pacman
		case 0:{
			
			if( ghostType == GHOST.BLINKY )
				maFIndex = 5;
			else if( ghostType == GHOST.PINKY )
				maFIndex = 6;
			else if( ghostType == GHOST.INKY )
				maFIndex = 7;
			else 
				maFIndex = 8;
			break;
		}
		//single-agent feature 1 is the distance from pacman to the ghost
		case 1: {
			
			if( ghostType == GHOST.BLINKY )
				maFIndex = 1;
			else if( ghostType == GHOST.PINKY )
				maFIndex = 2;
			else if( ghostType == GHOST.INKY )
				maFIndex = 3;
			else 
				maFIndex = 4;
			break;
		}
		//single-agent feature 2 is the euclidean distance between ghost and pacman
		case 2: {
			
			if( ghostType == GHOST.BLINKY )
				maFIndex = 9;
			else if( ghostType == GHOST.PINKY )
				maFIndex = 10;
			else if( ghostType == GHOST.INKY )
				maFIndex = 11;
			else 
				maFIndex = 12;
			break;
		}
		//single-agent feature 3 is the manhattan distance between ghost and pacman
		case 3: {
			
			if( ghostType == GHOST.BLINKY )
				maFIndex = 13;
			else if( ghostType == GHOST.PINKY )
				maFIndex = 14;
			else if( ghostType == GHOST.INKY )
				maFIndex = 15;
			else 
				maFIndex = 16;
			break;
		}
		//single-agent feature 4 is the distance from ghost to power pill
		case 4: {
			
			if( ghostType == GHOST.BLINKY )
				maFIndex = 17;
			else if( ghostType == GHOST.PINKY )
				maFIndex = 18;
			else if( ghostType == GHOST.INKY )
				maFIndex = 19;
			else 
				maFIndex = 20;
			break;
		}
		//single-agent feature 5 is the distance from pacman to power pill
		case 5: {
			
			maFIndex = 0;
			break;
		}
		//single-agent feature 6 is the edible time of the ghost
		case 6: {
			
			if( ghostType == GHOST.BLINKY )
				maFIndex = 21;
			else if( ghostType == GHOST.PINKY )
				maFIndex = 22;
			else if( ghostType == GHOST.INKY )
				maFIndex = 23;
			else 
				maFIndex = 24;
			break;
		}
		//single-agent feature 7 is the level time
		case 7: {
			
			 maFIndex = 25;
			break;
		}
		//single-agent feature 8 is the power pill rate
		case 8: {
			
			maFIndex = 26;
			break;
		}
		//single-agent feature 9 is the pill rate
		case 9: {
			
			maFIndex = 27;
			break;
		}
		//single-agent feature 10 is the pacman life rate
		case 10: {
			
			maFIndex = 28;
			break;
		}
		/**
		 * single-agent features 11-14 are the distances from 
		 * the ghost to the other ghosts including itself
		 * 11: distance to Blinky
		 * 12: distance to Pinky
		 * 13: distance to Inky
		 * 14: distance to Sue
		 */
		case 11: {
			
			if( ghostType == GHOST.BLINKY )
				maFIndex = -1;
			else if( ghostType == GHOST.PINKY )
				maFIndex = 32;
			else if( ghostType == GHOST.INKY )
				maFIndex = 35;
			else 
				maFIndex = 38;
			break;
		}
		case 12: {
			
			if( ghostType == GHOST.BLINKY )
				maFIndex = 29;
			else if( ghostType == GHOST.PINKY )
				maFIndex = -1;
			else if( ghostType == GHOST.INKY )
				maFIndex = 36;
			else 
				maFIndex = 39;
			break;
		}
		case 13: {
			
			if( ghostType == GHOST.BLINKY )
				maFIndex = 30;
			else if( ghostType == GHOST.PINKY )
				maFIndex = 33;
			else if( ghostType == GHOST.INKY )
				maFIndex = -1;
			else 
				maFIndex = 40;
			break;
		}
		case 14: {
			
			if( ghostType == GHOST.BLINKY )
				maFIndex = 31;
			else if( ghostType == GHOST.PINKY )
				maFIndex = 34;
			else if( ghostType == GHOST.INKY )
				maFIndex = 37;
			else 
				maFIndex = -1;
			break;
		}
		/**
		 * feature 15: the combined feature of distance from pacman to ghost 
		 * and the edible time of the ghost
		 */
		case 15: {
			if( ghostType == GHOST.BLINKY ) 
				maFIndex = 41;
			else if( ghostType == GHOST.PINKY )
				maFIndex = 42;
			else if( ghostType == GHOST.INKY )
				maFIndex = 43;
			else 
				maFIndex = 44;
			
			break;
		}
		/**
		 * feature 16: the combined feature of distance from pacman to ghost 
		 * and the minimal distance from pacman to power pill
		 */
		case 16: {
			
			if( ghostType == GHOST.BLINKY )
				maFIndex = 45;
			else if( ghostType == GHOST.PINKY )
				maFIndex = 46;
			else if( ghostType == GHOST.INKY )
				maFIndex = 47;
			else
				maFIndex = 48;
			
			break;
		}
		//other single-agent features do not appear in multi-agent features
		default: {
			
			break;
		}
		}
		
		return maFIndex;
	}
}
