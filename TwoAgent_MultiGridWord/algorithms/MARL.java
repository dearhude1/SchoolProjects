package algorithms;

import java.util.Random;

import multiGridWorld.GameAction;
import multiGridWorld.GameState;
import multiGridWorld.MultiGridWorld;


/**
 * the base class of all MARL algorithms
 * @author dearhude1
 *
 */
public class MARL
{

    public static final int MINIMAXQ = 0;
    public static final int NASHQ = 1;
    public static final int FFQ = 2;
    public static final int DENCE_CEQ = 3;
    public static final int CEN_CEQ = 4;
    public static final int uCEQ = 5;
    public static final int eCEQ = 6;
    public static final int pCEQ = 7;
    public static final int dCEQ = 8;
    public static final int NEGOQ = 9;
    public static final int WOLFPHC = 10;
    public static final int NASHQ_TRANS = 11;
    public static final int NASHQ_SUPP_TRANS = 12;
    public static final int uCEQ_TRANS = 13;
    public static final int eCEQ_TRANS = 14;
    public static final int pCEQ_TRANS = 15;
    public static final int dCEQ_TRANS = 16;
    
    public static final String[] ALG_STRINGS = { "minimaxQ", "NashQ", "FFQ", "decCEQ", "cenCEQ", 
	"uCEQ", "eCEQ", "pCEQ", "dCEQ", "NegoQ", "WoLF-PHC", "NashQTrans", "NashQSuppTrans",
	"uCEQTrans", "eCEQTrans", "pCEQTrans", "dCEQTrans" };
    
    /**
     * fundamental parameter of MARL algorithms
     */
    //we are now conducting preliminary experiments
    protected double ALPHA = 0.99;//0.99;
    protected double GAMMA = 0.7;//0.7;
    protected double EPSILON = 0.05;//0.02;//1.0??
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
    protected double[][][][][][] Qs;
    
    /**
     * visit number of each state-action pair
     */
    protected double[][][][][] vstNum;
    
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
	int locNum = MultiGridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	int worldNum = MultiGridWorld.WORLD_NUM;
	
	Qs = new double[MultiGridWorld.NUM_AGENTS][worldNum][locNum][locNum][actionNum][actionNum];
	vstNum = new double[worldNum][locNum][locNum][actionNum][actionNum];
	for( int agentIndex = 0; agentIndex < MultiGridWorld.NUM_AGENTS; agentIndex++ )
	    for( int wldIndex = 0; wldIndex < worldNum; wldIndex++ )
		for( int s1 = 0; s1 < locNum; s1++ )
		    for( int s2 = 0; s2 < locNum; s2++ )
			for( int a1 = 0; a1 < actionNum; a1++ )
			    for( int a2 = 0; a2 < actionNum; a2++ ) {
				    
				Qs[agentIndex][wldIndex][s1][s2][a1][a2] = random.nextDouble();//0.0;
				    
				vstNum[wldIndex][s1][s2][a1][a2] = 0.0;
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
	int locNum = MultiGridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	int worldNum = MultiGridWorld.WORLD_NUM;
	
	Qs = new double[MultiGridWorld.NUM_AGENTS][worldNum][locNum][locNum][actionNum][actionNum];
	vstNum = new double[worldNum][locNum][locNum][actionNum][actionNum];
	for( int agentIndex = 0; agentIndex < MultiGridWorld.NUM_AGENTS; agentIndex++ )
	    for( int wldIndex = 0; wldIndex < worldNum; wldIndex++ )
		for( int s1 = 0; s1 < locNum; s1++ )
		    for( int s2 = 0; s2 < locNum; s2++ )
			for( int a1 = 0; a1 < actionNum; a1++ )
			    for( int a2 = 0; a2 < actionNum; a2++ ) {
				    
				Qs[agentIndex][wldIndex][s1][s2][a1][a2] = random.nextDouble();//0.0;
				    
				vstNum[wldIndex][s1][s2][a1][a2] = 0.0;
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
    public int epsilonGreedy( int action )
    {
	/**
	double epsilon = 0.0;
	if( explorationTime < 0.01 )
	    epsilon = dynEpsilon;
	else {
	    
	    epsilon = Math.pow(1/explorationTime, 0.10001);
	    if( epsilon < dynEpsilon )
		epsilon = dynEpsilon;
	}
	*/
	
	if( random.nextDouble() < EPSILON ) {
	    
	    /**
	     * exploration one time
	     *
	    dynEpsilon *= 0.9995396;//0.99999;
	    explorationTime += 1.0;
	    */
	    
	    return random.nextInt(GameAction.NUM_ACTIONS);
	}
	else
	    return action;
    }
    
    public GameAction epsilonGreedy( GameAction gameAction )
    {
	/**
	double epsilon = 0.0;
	if( explorationTime < 0.01 )
	    epsilon = dynEpsilon;
	else {
	    
	    epsilon = Math.pow(1/explorationTime, 0.10001);
	    if( epsilon < dynEpsilon )
		epsilon = dynEpsilon;
	}
	*/
	
	if( random.nextDouble() < EPSILON ) {
	    
	    /**
	     * exploration one time
	     *
	    dynEpsilon *= 0.9995396;//0.99999;
	    explorationTime += 1.0;
	    */
	    
	    for( int agentIndex = 0; agentIndex < MultiGridWorld.NUM_AGENTS; agentIndex++ ) {
		
		gameAction.setAction( agentIndex, random.nextInt(GameAction.NUM_ACTIONS) );
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
	int worldIndex = curState.getWorldIndex();
	int a0 = curAction.getAction(0);
	int a1 = curAction.getAction(1);
	
	vstNum[worldIndex][loc0][loc1][a0][a1] += 1.0;
    }
    
    protected double getVariableAlpha( GameState gameState, GameAction gameAction )
    {
	if( gameState == null || gameAction == null ) {
	    
	    System.out.println("@MARL->getVariableAlpha: Wrong Parameters!");
	    return 0.0;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int worldIndex = gameState.getWorldIndex();
	int a0 = gameAction.getAction(0);
	int a1 = gameAction.getAction(1);
	
	if( vstNum[worldIndex][loc0][loc1][a0][a1] <= 0.0 )
	    return 1.0;
	else
	    return 1.0 / vstNum[worldIndex][loc0][loc1][a0][a1];
	
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
	int worldIndex = gameState.getWorldIndex();
	int a0 = gameAction.getAction(0);
	int a1 = gameAction.getAction(1);
	
	return Qs[agent][worldIndex][loc0][loc1][a0][a1];
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
	int worldIndex = gameState.getWorldIndex();
	int a0 = gameAction.getAction(0);
	int a1 = gameAction.getAction(1);
	
	Qs[agent][worldIndex][loc0][loc1][a0][a1] = value;
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
