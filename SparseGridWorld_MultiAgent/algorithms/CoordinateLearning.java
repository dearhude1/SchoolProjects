package algorithms;

import java.util.ArrayList;
import gameGridWorld.GameAction;
import gameGridWorld.GameState;
import gameGridWorld.SparseGridWorld;

public class CoordinateLearning extends MARL
{

    /**
     * redefine the actions for this algorithm
     */
    private static final int LOCAL_ACTION_UP = 1;
    private static final int lOCAL_ACTION_DOWN = 3;
    private static final int lOCAL_ACTION_LEFT = 2;
    private static final int lOCAL_ACTION_RIGHT = 0;
    private static final int LOCAL_ACTION_COORDINATE = 4;
    private static final int NUM_LOCAL_ACTIONS = 5;
    
    private static int mapWidth = 0;
    private static int mapHeight = 0;
    
    /**
     * the hashmap in MARL can be directly used as 
     * the Q-table of the joint action 
     * 
     *  we also need a local Q-table, 
     *  note that an additional action "Coordinate" should be 
     *  taken into account
     */
    double[][] locQ = null;
    
    
    /**
     * two variables to indicate whether 
     * agents coordinate in the last state
     */
    private int currentAction = -1;
    private boolean currentActivePercept = false;
    
    public CoordinateLearning( int agentIndex )
    {
	super( agentIndex );
	
	/**
	 * init the locQ table
	 */
	int locStateNum = SparseGridWorld.NUM_CELLS;
	locQ = new double[locStateNum][NUM_LOCAL_ACTIONS];
	
	for( int s = 0; s < locStateNum; s++ ) 
	    for( int a = 0; a < NUM_LOCAL_ACTIONS; a++ ) {
		
		locQ[s][a] = random.nextDouble();
	    }
    }
    
    public CoordinateLearning( int agentIndex, double alpha, 
	    double gamma, double epsilon )
    {
	super(agentIndex, alpha, gamma, epsilon);
	
	/**
	 * init the locQ table
	 */
	int locStateNum = SparseGridWorld.NUM_CELLS;
	locQ = new double[locStateNum][NUM_LOCAL_ACTIONS];
	
	for( int s = 0; s < locStateNum; s++ ) 
	    for( int a = 0; a < NUM_LOCAL_ACTIONS; a++ ) {
		
		locQ[s][a] = random.nextDouble();
	    }
    }
    
    
    /**
     * for CoordinateLearning 
     * epsilon greedy is excuted in the method updateQ 
     */
    public int epsilonGreedy( int action )
    {
	
	return action;
    }
    
    public GameAction epsilonGreedy( GameAction gameAction )
    {
	
	return gameAction;
    }
    
    public GameAction updateQ( GameState curState, GameAction jointAction, 
	    double[] rewards, GameState nextState )
    {
	
	if( nextState == null ) {
	    
	    System.out.println("@CoordinateLearning->updateQ: NULL State!");
	    
	    return null;
	}
	else {
	    
	    /**
	     * choose the an action according to 
	     * the local table (epsilon greedy) 
	     */
	    int nextAction = 0;
	    int maxAction = getLocalAction_Uncoordinate( nextState );
	    int epGreedyAction = maxAction;
	    
	    if( random.nextDouble() < EPSILON )
		epGreedyAction = random.nextInt( NUM_LOCAL_ACTIONS );
	    
	    /**
	     * the next joint action
	     */
	    GameAction maxJointAction = null;
	    GameAction nextJointAction = new GameAction();
	    
	    /**
	     * if current action is coordinate
	     */
	    boolean activePercept = false;
	    if( epGreedyAction == LOCAL_ACTION_COORDINATE ) {
		
		/**
		 * if the agent can perceive the other agents
		 * 
		 * in our case, if the Manhattan distance between two agents 
		 * are smaller than 3, then perception is succeeded
		 */
		activePercept = isActivePercept( nextState, agentIndex );
		
		/**
		 * if active, choose the next action 
		 * according to the joint learning table with ep-greedy
		 */
		if( activePercept ) {
		    
		    maxJointAction = getLocalAction_Coordinate( nextState );
		    
		    nextAction = maxJointAction.getAction( agentIndex );
		    nextJointAction.setAction( agentIndex, nextAction );
		}
		else {
		    
		    //the max action except the Coordination??
		    nextAction = getLocalExecutingAction( nextState );
		    
		    nextJointAction.setAction( agentIndex, nextAction );
		}
	    }
	    else {
		
		nextJointAction.setAction( agentIndex, epGreedyAction );
	    }
		
	    
	    
	    /**
	     * update the Q-tables
	     */
	    if( curState != null && jointAction != null 
			&& rewards != null ) {
		
		/**
		 * update the local function
		 */
		int locState = curState.getLocationID( agentIndex );
		double qValue = locQ[locState][currentAction];
		double qMax = locQ[nextState.getLocationID(agentIndex)][maxAction];
		qValue = (1 - ALPHA) * qValue + ALPHA * (rewards[agentIndex] + GAMMA * qMax);
		locQ[locState][currentAction] = qValue;
		
		/**
		 * if in the last state, the agent choose Coordinate 
		 * and can perceive the other agents, then we should 
		 * update the global Q-function
		 */
		if( currentAction == LOCAL_ACTION_COORDINATE && 
			currentActivePercept ) {
		    
		    /**
		     * update the joint-action table using local-action table
		     */
		    
		    double Qsa = getQValue( agentIndex, curState, jointAction );
		    Qsa = (1 - ALPHA) * Qsa + ALPHA * (rewards[agentIndex] + GAMMA * qMax );
		    setQValue( agentIndex, curState, jointAction, Qsa );
		    
		}
		
		
		/**
		 * update alpha??
		 */
		ALPHA *= 0.9991;//0.99985;//0.99958;//0.99958;//58;
	    }
	    
	    
	    /**
	     * record the current local action and ...
	     */
	    currentAction = epGreedyAction;//maxAction;
	    currentActivePercept = activePercept;
	    
	    
	    /**
	     * return the next joint action
	     */
	    return nextJointAction;
	    
	}
    }

    private int getLocalAction_Uncoordinate( GameState curState )
    {
	if( curState == null ) {
	    
	    System.out.println("CoordinateLearning->getLocalAction_Uncoordinate: NULL State");
	    return -1;
	}
	
	int locState = curState.getLocationID( agentIndex );
	
	int maxAct = 0; 
	double maxQ = locQ[locState][maxAct];
	for( int locAct = 1; locAct < NUM_LOCAL_ACTIONS; locAct++ ) {
	    
	    double qValue = locQ[locState][locAct];
	    
	    if( maxQ < qValue ) {
		
		maxQ = qValue;
		maxAct = locAct;
	    }
	}
	
	return maxAct;
    }
    
    private int getLocalExecutingAction( GameState curState )
    {
	if( curState == null ) {
	    
	    System.out.println("CoordinateLearning->getLocalExecutingAction: NULL State");
	    return -1;
	}
	
	int locState = curState.getLocationID( agentIndex );
	
	int maxAct = 0; 
	double maxQ = locQ[locState][maxAct];
	for( int locAct = 1; locAct < GameAction.NUM_ACTIONS; locAct++ ) {
	    
	    double qValue = locQ[locState][locAct];
	    
	    if( maxQ < qValue ) {
		
		maxQ = qValue;
		maxAct = locAct;
	    }
	}
	
	return maxAct;
    }
    
    private GameAction getLocalAction_Coordinate( GameState curState )
    {
	if( curState == null ) {
	    
	    System.out.println("CoordinateLearning->getLocalAction_Coordinate: NULL State");
	    return null;
	}
	
	ArrayList<GameAction> jntActList = SparseGridWorld.getAllJointActions();
	
	double maxQ = Double.NEGATIVE_INFINITY;
	int maxJntActIndex = 0;
	
	for( int jntActIndex = 0; jntActIndex < jntActList.size(); jntActIndex++ ) {
	    
	    GameAction jntAction = jntActList.get( jntActIndex );
	    double jntQvalue = getQValue( agentIndex, curState, jntAction );

	    
	    if( maxQ < jntQvalue ) {
		
		maxQ = jntQvalue;
		maxJntActIndex = jntActIndex;
	    }
	}
	
	GameAction retAction = jntActList.get( maxJntActIndex );
	return retAction;
    }
    
    private int getMahanttanDistance( GameState state, int agent_i, 
	    int agent_j )
    {
	if( state == null ) {
	    
	    return Integer.MAX_VALUE;
	}
	
	int stateIndex_i = state.getLocationID( agent_i );
	int stateIndex_j = state.getLocationID( agent_j );
	
	int row_i = stateIndex_i / mapWidth;
	int row_j = stateIndex_j / mapWidth;
	int col_i = stateIndex_i - row_i * mapWidth;
	int col_j = stateIndex_j - row_j * mapWidth;
	
	return (Math.abs(row_i-row_j)+Math.abs(col_i-col_j));
    }
    
    private boolean isActivePercept( GameState state, int agent )
    {
	if( state == null || agent < 0 || 
		agent >= SparseGridWorld.NUM_AGENTS ) {
	    
	    System.out.println("CoordinateLearning->isActivePercept: Wrong Parameter");
	    return false;
	}
	
	for( int ag = 0; ag < SparseGridWorld.NUM_AGENTS; ag++ ) {
	    
	    if( ag == agent )
		continue;
	    
	    int mhttDis = getMahanttanDistance(state, agent, ag);
	    if( mhttDis <= 3 )
		return true;
	}
	
	return false;
    }
    
    public static void setMapWidthHeight( int width, int height ) 
    {
	mapWidth = width;
	mapHeight = height;
    }
    
}
