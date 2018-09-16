package algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import gameGridWorld.GameAction;
import gameGridWorld.GameState;
import gameGridWorld.SparseGridWorld;
import gameGridWorld.StateActionPair;

/**
 * the base class of all MARL algorithms
 * @author dearhude1
 *
 */
public class MARL
{

    public static final int NASHQ = 0;
    public static final int uCEQ = 1;
    public static final int eCEQ = 2;
    public static final int pCEQ = 3;
    public static final int dCEQ = 4;
    public static final int NEGOQ = 5;
    public static final int WOLFPHC = 6;
    public static final int NashQ_TransLocQ = 7;
    public static final int uCEQ_TransLocQ = 8;
    public static final int eCEQ_TransLocQ = 9;
    public static final int pCEQ_TransLocQ = 10;
    public static final int dCEQ_TransLocQ = 11;
    public static final int NEGOQ_TransLocQ = 12;
    public static final int NashQ_TransPolicy = 13;
    public static final int uCEQ_TransPolicy = 14;
    public static final int eCEQ_TransPolicy = 15;
    public static final int pCEQ_TransPolicy = 16;
    public static final int dCEQ_TransPolicy = 17;
    public static final int CoordinateLearning = 18;
    public static final int Qlearning = 19;
    public static final int uCEQ_TransModel = 20;
    public static final int eCEQ_TransModel = 21;
    public static final int pCEQ_TransModel = 22;
    public static final int dCEQ_TransModel = 23;
    public static final int NashQ_TransModel = 24;
    public static final int NEGOQ_TransModel = 25;
    public static final int uCEQ_AbsGame = 26;
    public static final int eCEQ_AbsGame = 27;
    public static final int pCEQ_AbsGame = 28;
    public static final int dCEQ_AbsGame = 29;
    public static final int NashQ_AbsGame = 30;
    public static final int NEGOQ_AbsGame = 31;
    public static final int NEGOQ_TransNSR = 32;
    public static final int NEGOQ_AbsGame_NSR = 33;
    public static final int uCEQ_TransNSR = 34;
    public static final int uCEQ_AbsGame_NSR = 35;
    public static final int uCEQ_AbsGame_NSR2 = 36;
    public static final int eCEQ_TransNSR = 37;
    public static final int eCEQ_AbsGame_NSR = 38;
    public static final int pCEQ_TransNSR = 39;
    public static final int pCEQ_AbsGame_NSR = 40;
    public static final int dCEQ_TransNSR = 41;
    public static final int dCEQ_AbsGame_NSR = 42;
    public static final int NashQ_TransNSR = 43;
    public static final int NashQ_AbsGame_NSR = 44;
    
    public static final String[] ALG_STRINGS = { 
	"NashQ", "uCEQ", "eCEQ", "pCEQ", "dCEQ", "NegoQ", "WoLF-PHC",  
	"NashQTransLocQ", "uCEQTransLocQ","eCEQTransLocQ","pCEQTransLocQ","dCEQTransLocQ", "NegoQTransLocQ",
	"NashQTransPolicy", "uCEQTransPolicy", "eCEQTransPolicy", "pCEQTransPolicy", "dCEQTransPolicy",
	"CoordinateLearning",
	"Qlearning",
	"uCEQTransModel", "eCEQTransModel", "pCEQTransModel", "dCEQTransModel",
	"NashQTransModel", "NegoQTransModel",
	"uCEQAbsGame", "eCEQAbsGame", "pCEQAbsGame", "dCEQAbsGame", 
	"NashQAbsGame", "NegoQAbsGame",
	"NegoQTransNSR", "NegoQAbsGameNSR",
	"uCEQTransNSR","uCEQAbsGameNSR","uCEQAbsGameNSR2",
	"eCEQTransNSR","eCEQAbsGameNSR",
	"pCEQTransNSR","pCEQAbsGameNSR",
	"dCEQTransNSR","dCEQAbsGameNSR",
	"NashQTransNSR","NashQAbsGameNSR"};
    
    /**
     * fundamental parameter of MARL algorithms
     */
    //we are now conducting preliminary experiments
    /**
     * alpha = 0.9, gamma = 0.7, ep = 0.03 for all algorithm except for NegoQ
     * 
     */
    //last version of parameters, alpha = 0.9, gamma = 0.95, ep = 0.05
    protected double ALPHA = 0.99;//0.99;
    protected double GAMMA = 0.9;//0.95
    protected double EPSILON = 0.01;//0.05;
    
    /**
     * for random use
     */
    protected Random random;
    
    
    
    /**
     * Q-tables for all agents
     * each entry store each agent's Q-value for a state-action pair
     */
    protected HashMap<StateActionPair, double[]> Qs;

    protected HashMap<StateActionPair, Integer> vstNum;
    
    
    /**
     * an algorithm can also represent an agent
     * agent index begins with 0
     */
    protected int agentIndex;
    
    double dynEpsilon = 1.0;
    protected double explorationTime = 0.0;

    
    public MARL( int index )
    {
	agentIndex = index;
	random = new Random();
	
	
	/**
	 * create the Q-table
	 */	
	Qs = new HashMap<StateActionPair,double[]>();
	vstNum = new HashMap<StateActionPair, Integer>();
	
	
	/**
	 * get all states and all joint actions from LinearGridWorld class
	 */
	ArrayList<GameState> allStates = SparseGridWorld.getAllValidStates();
	ArrayList<GameAction> allActions = SparseGridWorld.getAllJointActions();
	
	/**
	 * init the table
	 */
	for( int stateIndex = 0; stateIndex < allStates.size(); stateIndex++ ) {
	    
	    GameState gameState = allStates.get( stateIndex );
	    
	    for( int actionIndex = 0; actionIndex < allActions.size(); actionIndex++ ) {
		
		GameAction jntAction = allActions.get( actionIndex );
		
		StateActionPair saPair = new StateActionPair( gameState, jntAction );
		
		double[] qs = new double[SparseGridWorld.NUM_AGENTS];
		for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		    
		    qs[agent] = 0.0;
		}
		
		if( !Qs.containsKey( saPair ) ) {
		    
		    Qs.put( saPair, qs );
		    
		    vstNum.put( saPair, 0 );
		}
		else {
		    
		    saPair = null;
		    qs = null;
		}
		    
	    }
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
	 * create the Q-table
	 */	
	Qs = new HashMap<StateActionPair,double[]>();
	vstNum = new HashMap<StateActionPair, Integer>();
	
	
	/**
	 * get all states and all joint actions from LinearGridWorld class
	 */
	ArrayList<GameState> allStates = SparseGridWorld.getAllValidStates();
	ArrayList<GameAction> allActions = SparseGridWorld.getAllJointActions();
	
	/**
	 * init the table
	 */
	for( int stateIndex = 0; stateIndex < allStates.size(); stateIndex++ ) {
	    
	    GameState gameState = allStates.get( stateIndex );
	    
	    for( int actionIndex = 0; actionIndex < allActions.size(); actionIndex++ ) {
		
		GameAction jntAction = allActions.get( actionIndex );
		
		StateActionPair saPair = new StateActionPair( gameState, jntAction );
		
		double[] qs = new double[SparseGridWorld.NUM_AGENTS];
		for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		    
		    qs[agent] = random.nextDouble();
		}
		
		if( !Qs.containsKey( saPair ) ) {
		    
		    Qs.put( saPair, qs );
		    
		    vstNum.put( saPair, 0 );
		}
		else {
		    
		    saPair = null;
		    qs = null;
		}
		    
	    }
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
	    
	    epsilon = Math.pow(1/explorationTime, 0.20001);
	    if( epsilon < dynEpsilon )
		epsilon = dynEpsilon;
	}
	*/
	
	if( random.nextDouble() < EPSILON ) {
	    
	    /**
	     * exploration one time
	     *
	    dynEpsilon *= 0.99989;//0.99999;
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
	    
	    epsilon = Math.pow(1/explorationTime, 0.20001);
	    if( epsilon < dynEpsilon )
		epsilon = dynEpsilon;
	}
	*/
	
	if( random.nextDouble() < EPSILON ) {
	    
	    /**
	     * exploration one time
	     *
	    dynEpsilon *= 0.999989;//0.99999;
	    explorationTime += 1.0;
	    */
	    
	    for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
		
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
	
	StateActionPair saPair = new StateActionPair( curState, curAction );
	
	if( vstNum.containsKey( saPair ) ) {
	    
	    int count = vstNum.get( saPair );
	    count += 1;
	    vstNum.put( saPair, count );
	}
	
	saPair = null;
    }
    
    protected double getVariableAlpha( GameState gameState, GameAction gameAction )
    {
	if( gameState == null || gameAction == null ) {
	    
	    System.out.println("@MARL->getVariableAlpha: Wrong Parameters!");
	    return 0.0;
	}
	
	StateActionPair saPair = new StateActionPair( gameState, gameAction );
	
	double vAlpha = 1.0;
	
	if( vstNum.containsKey( saPair ) ) {
	    
	    int count = vstNum.get( saPair );
	    
	    if( count <= 0 ) 
		vAlpha = 1.0;
	    else 
		vAlpha = 1.0 / ((double) count);
	}
	else
	    vAlpha = 1.0;
	
	saPair = null;
	return vAlpha;
	
    }
    
    protected double getQValue( int agent, GameState gameState, 
	    GameAction gameAction )
    {
	if( gameAction == null || gameState == null || 
		agent < 0 || agent >= SparseGridWorld.NUM_AGENTS ) {
	    
	    System.out.println("@MARL->getQValue: Wrong Parameters!");
	    return 0.0;
	}
	
	StateActionPair saPair = new StateActionPair( gameState, gameAction );
	double qValue = 0.0;
	
	if( Qs.containsKey( saPair ) ) {
	    
	    double[] qEntries = Qs.get( saPair ); 
	    qValue = qEntries[agent];
	}
	
	//release the memory
	saPair = null;
	
	return qValue;
    }
    
    protected void setQValue( int agent, GameState gameState, 
	    GameAction gameAction, double value )
    {
	if( gameAction == null || 
		gameState == null ) {
	    
	    System.out.println("@CenCEQ->setQValue: Wrong Parameters!");
	    return;
	}
	
	StateActionPair saPair = new StateActionPair( gameState, gameAction );
	
	if( Qs.containsKey( saPair ) ) {
	    
	    double[] qEntries = Qs.get( saPair ); 
	    qEntries[agent] = value;
	    Qs.put( saPair, qEntries );
	}
	
	saPair = null;
    }
    
    
    protected ArrayList<GameAction> generateOtherJntActions( int agent_i )
    {
    	
    	ArrayList<GameAction> retJntActions = new ArrayList<GameAction>();
    	
    	
    	//agents' actions for iteration
    	int agentNum = SparseGridWorld.NUM_AGENTS;
    	int[] agentActionsIter = new int[agentNum];
    	for( int agent = 0; agent < agentNum; agent++ )
    	    agentActionsIter[agent] = 0;
    	
    	boolean cont = true;
    	while( cont ) {
			
    	    /**
    	     * the current partial joint action
    	     */
    	    GameAction jntAction = new GameAction( agentActionsIter );
    	    retJntActions.add( jntAction );
			
    	    /**
    	     * compute the next joint action
    	     */
    	    int last_agent = agentNum-1;
    	    int first_agent = 0;
    	    if( agent_i == 0 )
    		first_agent = 1;
    	    if( agent_i == agentNum-1 )
    		last_agent = agentNum-2;
			
    	    for( int agent_p = last_agent; agent_p >= first_agent; agent_p-- ) {
				
    		if( agent_p == agent_i )
    		    continue;
				
    		/**
    		 * the action index of the current agent increase
    		 */
    		agentActionsIter[agent_p] += 1;
				
    		if( agent_p > first_agent && agentActionsIter[agent_p] == GameAction.NUM_ACTIONS ) {
					
    		    agentActionsIter[agent_p] = 0;
					
    		    //then the next agent action should also increase
    		}
    		else
    		    break;
    	    }
			
    	    /**
    	     * whether to continue
    	     */
    	    if( agentActionsIter[first_agent] == GameAction.NUM_ACTIONS )
    		break;
    	}
		
    	return retJntActions;
    }
    
    //just for data
    public void gameFinished( int loop ) 
    {
	
    }
    
    //just for data
    public void gameStarted( int loop )
    {
	
    }
    
    /**
     * tell the agent the current episode number
     * can be called by the game executor
     */
    public void currentEpisode( int ep )
    {
	
    }
}
