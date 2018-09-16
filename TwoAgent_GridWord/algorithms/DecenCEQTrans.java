package algorithms;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import gameGridWorld.GameAction;
import gameGridWorld.GameState;
import gameGridWorld.GridWorld;

public class DecenCEQTrans extends DecenCEQ
{
    /**
     * the last game 
     */
    private double[][][][][] lastGame;
    
    private boolean[][] visited;
    
    /**
     * we use tau to denote the threshold of the error bound
     */
    private double tau = 0.01;
    //0.01 for transfer
    //0.1 for error analysis
    
    /**
     * for error bound analysis
     */
    protected int timeStepLimit = 3000;
    protected double[][][] errorBounds;
    protected int[][] transTimes;
    
    public DecenCEQTrans( int index )
    {
	super(index);
	
	int locNum = GridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	
	//init new variables
	lastGame = new double[GridWorld.NUM_AGENTS][locNum][locNum][actionNum][actionNum];
	visited = new boolean[locNum][locNum];
	
	for( int s1 = 0; s1 < locNum; s1++ )
	    for( int s2 = 0; s2 < locNum; s2++ ) {
		    
		visited[s1][s2] = false;
		
		for( int a1 = 0; a1 < actionNum; a1++ )
		    for( int a2 = 0; a2 < actionNum; a2++ ) 
			for( int agentIndex = 0; agentIndex < GridWorld.NUM_AGENTS; agentIndex++ ) {
				    
			    lastGame[agentIndex][s1][s2][a1][a2] = 0.0;
			}
	    }
	
	//for error bound analysis
	errorBounds = new double[locNum][locNum][timeStepLimit];
	transTimes = new int[locNum][locNum];
	 
	for( int s1 = 0; s1 < locNum; s1++ )
	    for( int s2 = 0; s2 < locNum; s2++ ) {
		
		transTimes[s1][s2] = 0;
		
		for( int t = 0; t < timeStepLimit; t++ ) 
		    errorBounds[s1][s2][t] = 0.0;
	    }
    }
    
    public DecenCEQTrans( int index, double alpha, double gamma, double epsilon  )
    {
	super(index, alpha, gamma, epsilon);
	
	int locNum = GridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	
	lastGame = new double[GridWorld.NUM_AGENTS][locNum][locNum][actionNum][actionNum];
	visited = new boolean[locNum][locNum];
	
	for( int s1 = 0; s1 < locNum; s1++ )
	    for( int s2 = 0; s2 < locNum; s2++ ) {
		    
		visited[s1][s2] = false;
		
		for( int a1 = 0; a1 < actionNum; a1++ )
		    for( int a2 = 0; a2 < actionNum; a2++ ) 
			for( int agentIndex = 0; agentIndex < GridWorld.NUM_AGENTS; agentIndex++ ) {
				    
			    lastGame[agentIndex][s1][s2][a1][a2] = 0.0;
			}
	    }
	
	//for error bound analysis
	errorBounds = new double[locNum][locNum][timeStepLimit];
	transTimes = new int[locNum][locNum];
	 
	for( int s1 = 0; s1 < locNum; s1++ )
	    for( int s2 = 0; s2 < locNum; s2++ ) {
		
		transTimes[s1][s2] = 0;
		
		for( int t = 0; t < timeStepLimit; t++ ) 
		    errorBounds[s1][s2][t] = 0.0;
	    }
    }
    
    public GameAction updateQ( GameState curState, GameAction jointAction, 
	    double[] rewards, GameState nextState )
    {
	
	if( nextState == null ) {
	    
	    System.out.println("@CenCEQ->updateQ: NULL nextState!");
	    
	    return null;
	}
	else {
	    
	    /**
	     * first we should determine whether to compute a CE
	     */
	    double[] lossValues = null;
	    lossValues = shouldComputeCE( nextState );
	    boolean bTrans = (lossValues != null && lossValues[0] < tau && lossValues[1] < tau);
	    
	    /**
	     * compute the correlated equilibrium in the next state
	     */
	    double[] correlEquil = null;
	    if( bTrans ) {
		
		correlEquil = lastEquilibrium[nextState.getLocationID(0)][nextState.getLocationID(1)];
		
		int loc0 = nextState.getLocationID(0);
		int loc1 = nextState.getLocationID(1);
		
		//record the error bound///////////////////////////
		/**
		if( transTimes[loc0][loc1] < timeStepLimit ) {
		    
		    int time = transTimes[loc0][loc1];
		    if( lossValues[0] > lossValues[1] ) 
			errorBounds[loc0][loc1][time] += lossValues[0];
		    else
			errorBounds[loc0][loc1][time] += lossValues[1];
		    
		    //System.out.println(errorBounds[loc0][loc1][time]);
		    
		    transTimes[loc0][loc1] += 1;
		}
		*/
		//for error bound analysis//////////////////////////
	    }
	    else {
		
		correlEquil = computeCE( agentIndex, nextState );
		
		/**
		 * store the current game
		 */
		storeGame( nextState, correlEquil );
	    }
	    
	    
	    
	    //the following code is for testing similar games during learning
	    //then compare the current equilibrium with the last one
	    /**
	    int loc0 = nextState.getLocationID(0);
	    int loc1 = nextState.getLocationID(1);
	    if( correlEquil != null && gameCounters[loc0][loc1] > 0.01 ) {
		
		//distance of two Nash equilibrium
		double dis = 0.0;
		for( int jntAct = 0; jntAct < GameAction.NUM_ACTIONS * GameAction.NUM_ACTIONS; jntAct++ ) {
		    
		    double diff = lastEquilibrium[loc0][loc1][jntAct] - correlEquil[jntAct];
		    dis += diff * diff;
		    
		}
		dis = Math.sqrt(dis);
		
		if( dis < 0.01 ) {   
		    simiGameCounters[loc0][loc1] += 1.0;
		}
	    }
	    gameCounters[loc0][loc1] += 1.0;
	    //store the current equilibrium
	    for( int jntAct = 0; jntAct < GameAction.NUM_ACTIONS * GameAction.NUM_ACTIONS; jntAct++ ) {
		    
		if( correlEquil != null )
		    lastEquilibrium[loc0][loc1][jntAct] = correlEquil[jntAct];
		else
		    lastEquilibrium[loc0][loc1][jntAct] = 0.0;
	    }
	    */
	    //////////////////////////////////////////////////////////////////////
	    
	    /**
	     * get a joint action according to the correlated equilibrium
	     */
	    GameAction nextAction = getJointAction_CE( correlEquil );
	    
	    
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
		double[] correlValues = getCEQValues( nextState, correlEquil );
		    
		for( int agent = 0; agent < GridWorld.NUM_AGENTS; agent++ ) {
		    
		    /**
		     * get the Q-value
		     */
		    double Qsa = getQValue( agent, curState, jointAction );
		    
		    /**
		     * updating rule
		     */
		    Qsa = (1 - ALPHA) * Qsa + ALPHA * (rewards[agent] + GAMMA * correlValues[agent]);
		    
		    
		    /**
		     * variable learning rate
		     */
		    //double alpha = getVariableAlpha( curState, jointAction );
		    //Qsa = (1 - alpha) * Qsa + alpha * (rewards[agent] + GAMMA * correlValues[agent]);
		    
		    /**
		     * write back to the tables
		     */
		    setQValue( agent, curState, jointAction, Qsa );
		}
		
		/**
		 * maybe we can release some memories
		 */
		correlValues = null;
	    }
	    
	    /**
	     * maybe we can release some memories
	     */
	    correlEquil = null;
	    
	    return nextAction;
	}
    }
    
    
    /**
     * determine whether we should compute an NE in a state
     */
    private double[] shouldComputeCE( GameState gameState )
    {
	if( gameState == null ) {
	    
	    System.out.println("CEQTrans->shouldComputeCE: Parameter error");
	    return null;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	
	
	/**
	 * first check whether the last equilibrium is available
	 */
	double proSum = 0.0;
	for( int jntAct = 0; jntAct < GameAction.NUM_ACTIONS*GameAction.NUM_ACTIONS; jntAct++ ) {
	    
	    proSum += lastEquilibrium[loc0][loc1][jntAct];
	}
	if( !(Math.abs(proSum-1.0) < 0.1) )
	    return null;
	
	
	if( !visited[loc0][loc1] ) {
	    
	    visited[loc0][loc1] = true;
	    return null;
	}
	else {
	    
	    
	    double[] retValues = new double[GridWorld.NUM_AGENTS];
	    
	    /**
	     * compute the max loss for each agent
	     */
	    for( int agent = 0; agent < GridWorld.NUM_AGENTS; agent++ ) {
		
		retValues[agent] = computeCELoss( agent, gameState );
	    }
	    
	    
	    return retValues;
	}
    }
    
    protected double computeCELoss( int agent, GameState gameState )
    {
	if( gameState == null || agent < 0 || 
		agent >= GridWorld.NUM_AGENTS ) {
	    
	    System.out.println("CEQTrans->computeCELoss: Parameter error");
	    return Double.MAX_VALUE;
	}
	
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	
	int agent_o = (agent+1) % GridWorld.NUM_AGENTS;
	
	/**
	 * compute loss for each action
	 */
	double maxLoss = Double.NEGATIVE_INFINITY;
	for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
	    
	    
	    /**
	     * for all the other actions of the agent
	     */
	    double maxLoss_p = Double.NEGATIVE_INFINITY;
	    for( int act_p = 0; act_p < GameAction.NUM_ACTIONS; act_p++ ) {
		
		if( act_p == act )
		    continue;
		
		/**
		 * for the other agents' actions
		 */
		double loss = 0.0;
		for( int act_o = 0; act_o < GameAction.NUM_ACTIONS; act_o++ ) {
		    
		    GameAction gameAction = new GameAction(new int[]{0,0});
		    GameAction gameAction_p = new GameAction(new int[]{0,0});
		    gameAction.setAction( agent, act );
		    gameAction.setAction(agent_o, act_o);
		    gameAction_p.setAction(agent, act_p);
		    gameAction_p.setAction(agent_o, act_o);
		    
		    double deltaUti = getQValue(agent, gameState, gameAction_p) - 
			    getQValue(agent, gameState, gameAction);
		    
		    int jntActIndex = gameAction.getAction(0)*GameAction.NUM_ACTIONS + gameAction.getAction(1);
		    loss += lastEquilibrium[loc0][loc1][jntActIndex] * deltaUti;
		}
		
		/**
		 * compare loss to the current maxLoss
		 */
		if( loss > maxLoss_p )
		    maxLoss_p = loss;
	    }
	    
	    if( maxLoss_p > maxLoss )
		maxLoss = maxLoss_p;
	}
	
	return maxLoss;
    }
    
    /**
     * store the last game where an NE is computed
     * store the computed NE at the same time
     */
    private void storeGame( GameState gameState, double[] correlEquil )
    {
	if( gameState == null ) {
	    
	    System.out.println("CEQTrans->storeGame: Parameter error");
	    return;
	}
	
	
	int actionNum = GameAction.NUM_ACTIONS;
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	
	for( int a1 = 0; a1 < actionNum; a1++ )
	    for( int a2 = 0; a2 < actionNum; a2++ ) 
		for( int agentIndex = 0; agentIndex < GridWorld.NUM_AGENTS; agentIndex++ ) {
			    
		    lastGame[agentIndex][loc0][loc1][a1][a2] = Qs[agentIndex][loc0][loc1][a1][a2];
		}
	
	for( int jntAct = 0; jntAct < GameAction.NUM_ACTIONS * GameAction.NUM_ACTIONS; jntAct++ ) {
		
	    if( correlEquil == null )
		lastEquilibrium[loc0][loc1][jntAct] = 0.0;
	    else
		lastEquilibrium[loc0][loc1][jntAct] = correlEquil[jntAct];
	}
    }
    

}
