package algorithms;


import multiGridWorld.GameAction;
import multiGridWorld.GameState;
import multiGridWorld.MultiGridWorld;


/**
 * New algorithm:
 * NashQ with direct equilibrium transfer
 * @author dearhude1
 *
 */

public class NashQTrans extends NashQ
{

    /**
     * the last game 
     */
    private double[][][][][][] lastGame;
    
    /**
     * the last equilibrium in each state
     */
    private double[][][][] lastEquilibrium;
    
    private boolean[][][] visited;
    
    /**
     * we use tau to denote the threshold of the error bound
     */
    private double tau = 0.01;//0.01;
    
    
    /**
     * for error bound analysis
     *
    private int timeStepLimit = 3000;
    private double[][][][] errorBounds;
    private int[][][] transTimes;
    */
    
    public NashQTrans()
    {
	super();
	
	int locNum = MultiGridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	int worldNum = MultiGridWorld.WORLD_NUM;
	
	//init new variables
	lastGame = new double[MultiGridWorld.NUM_AGENTS][worldNum][locNum][locNum][actionNum][actionNum];
	lastEquilibrium = new double[worldNum][locNum][locNum][MultiGridWorld.NUM_AGENTS * actionNum];
	visited = new boolean[worldNum][locNum][locNum];
	
	for( int wldIndex = 0; wldIndex < worldNum; wldIndex++ )
	    for( int s1 = 0; s1 < locNum; s1++ )
		for( int s2 = 0; s2 < locNum; s2++ ) {
		    
		    visited[wldIndex][s1][s2] = false;
		
		    for( int a1 = 0; a1 < actionNum; a1++ )
			for( int a2 = 0; a2 < actionNum; a2++ ) 
			    for( int agentIndex = 0; agentIndex < MultiGridWorld.NUM_AGENTS; agentIndex++ ) {
				    
				lastGame[agentIndex][wldIndex][s1][s2][a1][a2] = 0.0;
			    }
		    
		    for( int act = 0; act < MultiGridWorld.NUM_AGENTS * actionNum; act++ )
			lastEquilibrium[wldIndex][s1][s2][act] = 0.0;
	    }
	
	/**
	errorBounds = new double[locNum][locNum][timeStepLimit];
	transTimes = new int[locNum][locNum];
	 
	for( int s1 = 0; s1 < locNum; s1++ )
	    for( int s2 = 0; s2 < locNum; s2++ ) {
		
		transTimes[s1][s2] = 0;
		
		for( int t = 0; t < timeStepLimit; t++ ) 
		    errorBounds[s1][s2][t] = 0.0;
	    }
	*/
    }
    
    public NashQTrans( double alpha, double gamma, double epsilon )
    {
	super(alpha, gamma, epsilon);
	
	int locNum = MultiGridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	int worldNum = MultiGridWorld.WORLD_NUM;
	
	//init new variables
	lastGame = new double[MultiGridWorld.NUM_AGENTS][worldNum][locNum][locNum][actionNum][actionNum];
	lastEquilibrium = new double[worldNum][locNum][locNum][MultiGridWorld.NUM_AGENTS * actionNum];
	visited = new boolean[worldNum][locNum][locNum];
	
	for( int wldIndex = 0; wldIndex < worldNum; wldIndex++ )
	    for( int s1 = 0; s1 < locNum; s1++ )
		for( int s2 = 0; s2 < locNum; s2++ ) {
		    
		    visited[wldIndex][s1][s2] = false;
		
		    for( int a1 = 0; a1 < actionNum; a1++ )
			for( int a2 = 0; a2 < actionNum; a2++ ) 
			    for( int agentIndex = 0; agentIndex < MultiGridWorld.NUM_AGENTS; agentIndex++ ) {
				    
				lastGame[agentIndex][wldIndex][s1][s2][a1][a2] = 0.0;
			    }
		    
		    for( int act = 0; act < MultiGridWorld.NUM_AGENTS * actionNum; act++ )
			lastEquilibrium[wldIndex][s1][s2][act] = 0.0;
	    }
	
	/**
	errorBounds = new double[locNum][locNum][timeStepLimit];
	transTimes = new int[locNum][locNum];
	 
	for( int s1 = 0; s1 < locNum; s1++ )
	    for( int s2 = 0; s2 < locNum; s2++ ) {
		
		transTimes[s1][s2] = 0;
		
		for( int t = 0; t < timeStepLimit; t++ ) 
		    errorBounds[s1][s2][t] = 0.0;
	    }
	*/
	
    }
    
    /**
     * Core method for MARL algorithms
     * update the Q-table and return the action for the next state
     * @param curState: the current state
     * @param jointAction: the joint action taken in the current state
     * @param rewards: the reward obtained from the current state to the next state
     * @param nextState: the next state
     * @return GameAction: the action expected to be chosen in the next state
     */
    public GameAction updateQ( GameState curState, GameAction jointAction, 
	    double[] rewards, GameState nextState )
    {
	
	if( nextState == null ) {
	    
	    System.out.println("@NashQTrans->updateQ: NULL nextState!");
	    
	    return null;
	}
	else {
	    
	    /**
	     * first we should determine whether to compute an NE
	     */
	    double[] boundValue = null;
	    boundValue = shouldComputeNE( nextState );
	    boolean bTrans = (boundValue != null && boundValue[0] <= tau && boundValue[1] <= tau);
	    /**
	     * compute the Nash equilibrium in the next state
	     */
	    double[] nashEquil = null;
	    if( bTrans ) {
		
		int loc0 = nextState.getLocationID(0);
		int loc1 = nextState.getLocationID(1);
		int worldIndex = nextState.getWorldIndex();
		nashEquil = lastEquilibrium[worldIndex][loc0][loc1];
		
		//record the error bound///////////////////////////
		/**
		if( transTimes[loc0][loc1] < timeStepLimit ) {
		    
		    int time = transTimes[loc0][loc1];
		    if( boundValue[0] > boundValue[1] ) 
			errorBounds[loc0][loc1][time] += boundValue[0];
		    else
			errorBounds[loc0][loc1][time] += boundValue[1];
		    
		    //System.out.println(errorBounds[loc0][loc1][time]);
		    
		    transTimes[loc0][loc1] += 1;
		}
		*/
		//for error bound analysis//////////////////////////
	    }
	    else {
		
		//if( boundValue != null )
		   // System.out.println(boundValue[0]);
		
		nashEquil = computeNE_2agent( agentIndex, nextState );
		
		/**
		 * store the current game
		 */
		storeGame( nextState, nashEquil );
	    }
	    
	    /**
	     * get a joint action according to the correlated equilibrium
	     */
	    GameAction nextAction = getJointAction_NE( nashEquil );
	    
	    
	    /**
	     * update the Q-tables
	     * but if this is the initial state of the game
	     * just return the action
	     */
	    if( curState != null && jointAction != null 
		&& rewards != null ) {
		
		/**
		 * mark a visit
		 */
		visit( curState, jointAction );
		
		/**
		 * compute the correspoding Q-values
		 */
		double[] nashQValues = getNashQValues( nextState, nashEquil );
		    
		for( int agent = 0; agent < MultiGridWorld.NUM_AGENTS; agent++ ) {
		    
		    /**
		     * get the Q-value
		     */
		    double Qsa = getQValue( agent, curState, jointAction );
		    
		    /**
		     * updating rule for NashQTrans
		     */
		    if( !bTrans )
			Qsa = (1 - ALPHA) * Qsa + ALPHA * (rewards[agent] + GAMMA * nashQValues[agent]);
		    else 
			Qsa = (1 - ALPHA) * Qsa + ALPHA * (rewards[agent] + GAMMA * (nashQValues[agent]));//boundValue[agent]));
		   
		    
		    /**
		     * variable learning rate
		     *
		    double alpha = getVariableAlpha( curState, jointAction );
		    if( !bTrans )
			Qsa = (1 - alpha) * Qsa + alpha * (rewards[agent] + GAMMA * nashQValues[agent]);
		    else 
			Qsa = (1 - alpha) * Qsa + alpha * (rewards[agent] + GAMMA * (nashQValues[agent]));//+boundValue[agent]));
		    */
		    
		    /**
		     * write back to the tables
		     */
		    setQValue( agent, curState, jointAction, Qsa );
		}
		
		/**
		 * maybe we can release some memories
		 */
		nashQValues = null;
	    }
	    
	    /**
	     * maybe we can release some memories
	     */
	    nashEquil = null;
	    
	    return nextAction;
	}
    }
    
    /**
     * determine whether we should compute an NE in a state
     */
    private double[] shouldComputeNE( GameState gameState )
    {
	if( gameState == null ) {
	    
	    System.out.println("NashQTrans->shouldComputeNE: Parameter error");
	    return null;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int worldIndex = gameState.getWorldIndex();
	
	
	if( !visited[worldIndex][loc0][loc1] ) {
	    
	    visited[worldIndex][loc0][loc1] = true;
	    return null;
	}
	else {
	    
	    int actionNum = GameAction.NUM_ACTIONS;
	    
	    /**
	     * compute the difference of two games
	     */
	    double[][][] deltaMatrix = new double[MultiGridWorld.NUM_AGENTS][actionNum][actionNum];
	    for( int agentIndex = 0; agentIndex < MultiGridWorld.NUM_AGENTS; agentIndex++ )
		for( int act0 = 0; act0 < actionNum; act0++ )
		    for( int act1 = 0; act1 < actionNum; act1++ ) {
			
			deltaMatrix[agentIndex][act0][act1] = Qs[agentIndex][worldIndex][loc0][loc1][act0][act1] - 
				lastGame[agentIndex][worldIndex][loc0][loc1][act0][act1];
		    }
	    
	    /**
	     * compute delta weighted sum
	     */
	    double[] deltaWeightedSums = new double[MultiGridWorld.NUM_AGENTS];
	    
	    
	    for( int agentIndex = 0; agentIndex < MultiGridWorld.NUM_AGENTS; agentIndex++ ) {
	    
		deltaWeightedSums[agentIndex] = 0.0;
		for( int act0 = 0; act0 < actionNum; act0++ ) 
	    	    for( int act1 = 0; act1 < actionNum; act1++ ) {
		
	    		//big error here!!! += not =
	    		deltaWeightedSums[agentIndex] += lastEquilibrium[worldIndex][loc0][loc1][act0] * 
	    			lastEquilibrium[worldIndex][loc0][loc1][actionNum+act1] * deltaMatrix[agentIndex][act0][act1];
	    	    }
	    }
	    
	    
	    /**
	     * compute the error bound for agent0
	     */
	    double deltaPlusSum_Max = Double.NEGATIVE_INFINITY;
	    for( int act0 = 0; act0 < actionNum; act0++ ) {
	    		
		double deltaPlusSum = 0.0;
	    		
		for( int act1 = 0; act1 < actionNum; act1++ ) {
	    			
		    if( deltaMatrix[0][act0][act1] > 0 )
			deltaPlusSum += deltaMatrix[0][act0][act1];
		}
	    		
		if( deltaPlusSum > deltaPlusSum_Max )
		    deltaPlusSum_Max = deltaPlusSum;
	    }
	    	
	    double bound0 = deltaPlusSum_Max - deltaWeightedSums[0];
	    
	    
	    /**
	     * compute the error bound for agent1
	     */
	    deltaPlusSum_Max = Double.NEGATIVE_INFINITY;
	    for( int act1 = 0; act1 < actionNum; act1++ ) {
	    		
		double deltaPlusSum = 0.0;
	    		
		for( int act0 = 0; act0 < actionNum; act0++ ) {
	    			
		    if( deltaMatrix[1][act0][act1] > 0 )
			deltaPlusSum += deltaMatrix[1][act0][act1];
		}
	    		
		if( deltaPlusSum > deltaPlusSum_Max )
		    deltaPlusSum_Max = deltaPlusSum;
	    }
	    double bound1 = deltaPlusSum_Max - deltaWeightedSums[1];
	    
	    
	    return new double[]{bound0,bound1};
	    
	    /**
	    if( bound0 < tau && bound1 < tau ) {
		
		//if( bound0 > 0.0 )
		   // System.out.println("bound 0: "+bound0);
		//if( bound1 > 0.0 )
		    //System.out.println("bound 1: "+bound1);
		
		return false;
	    }
	    else {
		
		//System.out.println("bound 0: "+bound0);
		//System.out.println("bound 1: "+bound1);
		return true;
	    }
	    */
	}
    }
    
    /**
     * store the last game where an NE is computed
     * store the computed NE at the same time
     */
    private void storeGame( GameState gameState, double[] nashEquil )
    {
	if( gameState == null ) {
	    
	    System.out.println("NashQTrans->storeGame: Parameter error");
	    return;
	}
	
	
	int actionNum = GameAction.NUM_ACTIONS;
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int worldIndex = gameState.getWorldIndex();
	
	for( int a1 = 0; a1 < actionNum; a1++ )
	    for( int a2 = 0; a2 < actionNum; a2++ ) 
		for( int agentIndex = 0; agentIndex < MultiGridWorld.NUM_AGENTS; agentIndex++ ) {
			    
		    lastGame[agentIndex][worldIndex][loc0][loc1][a1][a2] = 
			    Qs[agentIndex][worldIndex][loc0][loc1][a1][a2];
		}
	    
	for( int act = 0; act < MultiGridWorld.NUM_AGENTS * actionNum; act++ )
	    lastEquilibrium[worldIndex][loc0][loc1][act] = nashEquil[act];
    }
}
