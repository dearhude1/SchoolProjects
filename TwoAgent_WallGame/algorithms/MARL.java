package algorithms;

import java.util.Random;

import wallGame.GameAction;
import wallGame.GameState;
import wallGame.WallGame;

import com.sun.corba.se.impl.presentation.rmi.DynamicAccessPermission;



/**
 * the base class of all MARL algorithms
 * @author dearhude1
 *
 */
public class MARL
{

    public static final int MINIMAXQ = 0;
    public static final int MINIMAXQ_TRANS = 1;	
    
    public static final String[] ALG_STRINGS = { "minimaxQ", "minimaxQTrans" };
    
    /**
     * fundamental parameter of MARL algorithms
     */
    //we are now conducting preliminary experiments
    protected double ALPHA = 0.9;//0.9;
    protected double GAMMA = 0.9;//0.7;
    protected double EPSILON = 0.025;//0.1;//0.025;
    //for errors, ep =0.05
    
    double dynEpsilon = 1.0;
    protected double explorationTime = 0.0;
    
    /**
     * for random use
     */
    protected Random random;
    
    /**
     * Q-table of the corresponding agent
     * 2 dimensions for state
     * 2 dimensions for joint action
     */
    protected double[][][][][] Qs;
    
    /**
     * visit number of each state-action pair
     */
    protected double[][][][] vstNum;
    
    /**
     * an algorithm can also represent an agent
     * agent index begins with 0
     */
    protected int agentIndex;
    
    
    public MARL( int index )
    {
	agentIndex = index;
	random = new Random();
	
	/**
	 * init the Q-table
	 */
	int locNum = WallGame.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	Qs = new double[WallGame.NUM_AGENTS][locNum][locNum][actionNum][actionNum];
	vstNum = new double[locNum][locNum][actionNum][actionNum];
	for( int agentIndex = 0; agentIndex < WallGame.NUM_AGENTS; agentIndex++ )
	    for( int s1 = 0; s1 < locNum; s1++ )
		for( int s2 = 0; s2 < locNum; s2++ )
		    for( int a1 = 0; a1 < actionNum; a1++ )
			for( int a2 = 0; a2 < actionNum; a2++ ) {
				    
			    Qs[agentIndex][s1][s2][a1][a2] = 0.0;
				    
			    vstNum[s1][s2][a1][a2] = 0.0;
			}
    }
    
    public MARL( int index, double alpha, double gamma, double epsilon )
    {
	agentIndex = index;
	random = new Random();
	
	ALPHA = alpha;
	GAMMA = gamma;
	EPSILON = epsilon;
	
	/**
	 * init the Q-table
	 */
	int locNum = WallGame.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	Qs = new double[WallGame.NUM_AGENTS][locNum][locNum][actionNum][actionNum];
	vstNum = new double[locNum][locNum][actionNum][actionNum];
	for( int agentIndex = 0; agentIndex < WallGame.NUM_AGENTS; agentIndex++ )
	    for( int s1 = 0; s1 < locNum; s1++ )
		for( int s2 = 0; s2 < locNum; s2++ )
		    for( int a1 = 0; a1 < actionNum; a1++ )
			for( int a2 = 0; a2 < actionNum; a2++ ) {
				    
			    Qs[agentIndex][s1][s2][a1][a2] = 0.0;
				    
			    vstNum[s1][s2][a1][a2] = 0.0;
			}
    }
    
    /**
     * Compute the action for the current state
     * @return
     */
    public int getAction( GameState gameState )
    {
	return GameAction.UP;
    }
    
    /**
     * epsilon-greedy for one agent
     * @param action
     * @return
     */
    public int epsilonGreedy( GameState gameState, int action )
    {
	/**/
	double epsilon = 0.0;
	if( explorationTime < 0.01 )
	    epsilon = dynEpsilon;
	else {
	    
	    epsilon = Math.pow(1/explorationTime, 0.100001);
	    if( epsilon < dynEpsilon )
		epsilon = dynEpsilon;
	}
	
	
	if( random.nextDouble() < EPSILON ) { 
		
	    //System.out.println(dynEpsilon);
	    
	    /**
	     * exploration one time
	     */
	    dynEpsilon *= 0.99999;//0.99999;
	    explorationTime += 1.0;
	    
	    int availNum = 0;
	    boolean[] actAvail = WallGame.actionSet( gameState.getLocationID(agentIndex) );
	    for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
		    
		if(actAvail[act])
		    availNum++;
	    }
	    double pro = random.nextDouble();
	    double proSum = 0.0;
	    for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
		
		if( !actAvail[act] )
		    continue;
		    
		proSum += 1.0 / availNum;
		if( pro < proSum ) {
			
		    return act;
		}
	    }
	    return action;
	}
	else
	    return action;
    }
    
    public GameAction epsilonGreedy( GameState gameState, GameAction gameAction )
    {
	double epsilon = 0.0;
	if( explorationTime < 0.01 )
	    epsilon = dynEpsilon;
	else {
	    
	    epsilon = Math.pow(1/explorationTime, 0.100001);
	    if( epsilon < dynEpsilon )
		epsilon = dynEpsilon;
	}
	
	if( random.nextDouble() < EPSILON ) {
	    
	    //System.out.println(dynEpsilon);
	    /**
	     * exploration one time
	     */
	    dynEpsilon *= 0.99999;//0.99999;
	    explorationTime += 1.0;
	    
	    for( int agentIndex = 0; agentIndex < WallGame.NUM_AGENTS; agentIndex++ ) {
		
		int availNum = 0;
		boolean[] actAvail = WallGame.actionSet( gameState.getLocationID(agentIndex) );
		for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
		    
		    if(actAvail[act])
			availNum++;
		}
		double pro = random.nextDouble();
		double proSum = 0.0;
		for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
		    
		    if( !actAvail[act] )
			continue;
		    
		    proSum += 1.0 / availNum;
		    
		    if( pro < proSum ) {
			
			gameAction.setAction( agentIndex, act );
			break;
		    }
		}
	    }   
	}
	
	return gameAction;
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
	
	return null;
    }
    
    
    public int getAgentIndex() 
    {
	return agentIndex;
    }
    
    protected void visit( GameState curState, GameAction curAction )
    {
	if( curState == null || curAction == null ) {
	    
	    System.out.println("@MARL->visit: Wrong Parameters!");
	    return;
	}
	
	int loc0 = curState.getLocationID(0);
	int loc1 = curState.getLocationID(1);
	int a0 = curAction.getAction(0);
	int a1 = curAction.getAction(1);
	
	vstNum[loc0][loc1][a0][a1] += 1.0;
    }
    
    protected double getVariableAlpha( GameState gameState, GameAction gameAction )
    {
	if( gameState == null || gameAction == null ) {
	    
	    System.out.println("@MARL->getVariableAlpha: Wrong Parameters!");
	    return 0.0;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int a0 = gameAction.getAction(0);
	int a1 = gameAction.getAction(1);
	
	if( vstNum[loc0][loc1][a0][a1] <= 0.0 )
	    return 1.0;
	else
	    return 1.0 / vstNum[loc0][loc1][a0][a1];
	
    }
    
    protected double getQValue( int agent, GameState gameState, 
	    GameAction gameAction )
    {
	if( gameAction == null || 
		gameState == null ) {
	    
	    System.out.println("@MARL->getQValue: Wrong Parameters!");
	    return 0.0;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int a0 = gameAction.getAction(0);
	int a1 = gameAction.getAction(1);
	
	return Qs[agent][loc0][loc1][a0][a1];
    }
    
    protected void setQValue( int agent, GameState gameState, 
	    GameAction gameAction, double value )
    {
	if( gameAction == null || 
		gameState == null ) {
	    
	    System.out.println("@CenCEQ->setQValue: Wrong Parameters!");
	    return;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int a0 = gameAction.getAction(0);
	int a1 = gameAction.getAction(1);
	
	Qs[agent][loc0][loc1][a0][a1] = value;
    }
    
    
    //just for data
    public void gameFinished( int loop ) 
    {
	
    }
    
    //just for data
    public void gameStarted( int loop )
    {
	
    }
}
