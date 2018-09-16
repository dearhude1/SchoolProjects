package algorithms;

import gameGridWorld.GameAction;
import gameGridWorld.GameState;
import gameGridWorld.SparseGridWorld;
import gameGridWorld.StateActionPair;
import help.Support;
import help.XVector;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;

import sun.management.resources.agent;

import drasys.or.matrix.VectorI;
import drasys.or.mp.Constraint;
import drasys.or.mp.Problem;
import drasys.or.mp.SizableProblemI;
import drasys.or.mp.lp.DenseSimplex;
import drasys.or.mp.lp.LinearProgrammingI;

/**
 * algorithm uCEQ with game abstraction
 * the one-shot game in each state 
 * is abstracted by model transfer and measuring state similarity
 * 
 * @author dearhude1
 */
public class NashQAbsGame extends NashQ
{
    
    /**
     * local Q-table for single-agent learning
     * note that this algorithm does not transfer value 
     * functions from Q-learning or R-max learning
     */
    private double[][][] locQs;
    
    /**
     * marking whether an agent 
     * is related to the other agents 
     * in its local state
     */
    private boolean[][] isRelated;
    
    
    /**
     * the state similarity
     */
    private double[][] stateSimilarity;
    
    /**
     * transfer threshold values for each agent
     */
    private double[] transCndValues = null;
    
    /**
     * the number of exploration episodes 
     * for construting the local model
     */
    private int numExpEpisodes = 100;
    
    private boolean bLearning = false;
    
    private HashMap<GameState, boolean[]> relatedMap;
    
    private double Alpha = 0.99;
    
    public NashQAbsGame()
    {
	super();
	
	int agentNum = SparseGridWorld.NUM_AGENTS;
	int locNum = SparseGridWorld.NUM_CELLS;
	int actionNum = GameAction.NUM_ACTIONS;
	
	transCndValues = new double[agentNum];
	transCndValues[0] = 2.0;
	transCndValues[1] = 2.0;
	
	locQs = new double[agentNum][locNum][actionNum];
	isRelated = new boolean[agentNum][locNum];
	stateSimilarity = new double[agentNum][locNum];
	
	relatedMap = new HashMap<GameState,boolean[]>();
	
	for( int ag = 0; ag < SparseGridWorld.NUM_AGENTS; ag++ )
	    for( int s = 0; s < locNum; s++ ) {
		
		//a large value for initialization
		stateSimilarity[ag][s] = 1000.0;
		isRelated[ag][s] = true;
		
		for( int a = 0; a < actionNum; a++ ) {
		    
		    locQs[ag][s][a] = random.nextDouble();
		}
	    }
    }
    
    public NashQAbsGame( double alpha, double gamma, double epsilon )
    {
	super(alpha, gamma, epsilon);
	
	int agentNum = SparseGridWorld.NUM_AGENTS;
	int locNum = SparseGridWorld.NUM_CELLS;
	int actionNum = GameAction.NUM_ACTIONS;
	
	transCndValues = new double[agentNum];
	transCndValues[0] = 12;//1.0;//1.5;//5;
	transCndValues[1] = 10;//2.0;//3.0;//5;
	
	locQs = new double[agentNum][locNum][actionNum];
	isRelated = new boolean[agentNum][locNum];
	stateSimilarity = new double[agentNum][locNum];
	
	relatedMap = new HashMap<GameState,boolean[]>();
	
	for( int ag = 0; ag < SparseGridWorld.NUM_AGENTS; ag++ )
	    for( int s = 0; s < locNum; s++ ) {
		
		//a large value for initialization
		stateSimilarity[ag][s] = 1000.0;
		isRelated[ag][s] = true;
		
		for( int a = 0; a < actionNum; a++ ) {
		    
		    locQs[ag][s][a] = random.nextDouble();//0.0;
		}
	    }
    }

    
    /**
     * this algorithm only needs to read similarity 
     * from files 
     * 
     * this just means that we transfer the models
     */
    private void readSimilarity()
    {
	
	try {
	    
	    /**/
	    for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
		
		String fileName = "./similarity_agent"+agentIndex+".txt";
		BufferedReader simReader = new BufferedReader(new FileReader(fileName));
		
		int s = 0;
		String line = "";
		
		while( (line = simReader.readLine()) != null ) {
		    
		    if( line.isEmpty() )
			continue;
		    
		    double similarity = Double.parseDouble( line );
		    stateSimilarity[agentIndex][s] = similarity;
		    
		    s++;
		    if( s >= SparseGridWorld.NUM_CELLS ) {
			
			break;
		    }
		}
		
		simReader.close();
		simReader = null;
		
	    }
	    
	}
	catch (Exception e) {
	    // TODO: handle exception
	}
    }
    
    private void readLocalQ()
    {
	
	/**
	 * init member locQs
	 */
	//locQs = new double[SparseGridWorld.NUM_AGENTS][SparseGridWorld.NUM_CELLS][GameAction.NUM_ACTIONS];
	
	try {
	    
	    for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
		
		String fileName = "./Qs_agent"+agentIndex+".txt";
		BufferedReader qReader = new BufferedReader(new FileReader(fileName));
		
		int locState = 0;
		int locAct = 0;
		
		String line = "";
		while( (line = qReader.readLine()) != null) {
			
		    if( line.isEmpty() )
			continue;
			
		    double qValue = Double.parseDouble( line );
		    
		    locQs[agentIndex][locState][locAct] = qValue;
		    
		    locAct++;
		    if( locAct >= GameAction.NUM_ACTIONS ) {
			
			locAct = 0;
			locState++;
			
			if( locState >= SparseGridWorld.NUM_CELLS ) {
			    
			    break;
			}
		    }
		}
		qReader.close();
	    }	
	    
	}
	catch(IOException ioe) {
	    
	    ioe.printStackTrace();
	}
    }
    
    
    private void transfer()
    {
	
	/**
	readLocalQ();
	
	ArrayList<GameState> allStates = SparseGridWorld.getAllValidStates();
	ArrayList<GameAction> jointActions = SparseGridWorld.getAllJointActions();
	
	for( int stateIndex = 0; stateIndex < allStates.size(); stateIndex ++ ) {
		
	    GameState state = allStates.get( stateIndex );
	   
	    for( int jntActIndex = 0; jntActIndex < jointActions.size(); jntActIndex++ ) {
		    
		GameAction jntAction = jointActions.get( jntActIndex );
		
		for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
		    
		    int locState = state.getLocationID( agentIndex );
		    int locAction = jntAction.getAction( agentIndex );
		    
		   double locQValue = locQs[agentIndex][locState][locAction];
		   
		   setQValue( agentIndex, state, jntAction, locQValue );  
		}
	    }
	}
	*/
	
	for( int locState = 0; locState < SparseGridWorld.NUM_CELLS; locState++ ) {
		
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		
		/**
		 * if the distance between the same states 
		 * in two different models is small (which means they are similar)
		 * then we transfer the value functions
		 */
		if( stateSimilarity[agent][locState] < transCndValues[agent] ) {
		    
		    isRelated[agent][locState] = false;
		}
		else {
		    
		    isRelated[agent][locState] = true;
		}
		
	    }
	}
	
	/**
	 * check all joint state
	 */
	int gameCount = 0;
	ArrayList<GameState> allStateList = SparseGridWorld.getAllValidStates();
	for( int stateIndex = 0; stateIndex < allStateList.size(); stateIndex++ ) {
	    
	    GameState gameState = allStateList.get( stateIndex );
	    
	    int relatedCount = 0;
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		
		int locState = gameState.getLocationID( agent );
		if( isRelated[agent][locState] )
		    relatedCount++;
	    }
	    
	    boolean[] relArray = new boolean[SparseGridWorld.NUM_AGENTS];
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		
		/**/
		int locState = gameState.getLocationID( agent );
		if( relatedCount >= 2 && isRelated[agent][locState] )
		    relArray[agent] = true;
		else
		    relArray[agent] = false;
		//*/
		
		/**
		int locState = gameState.getLocationID( agent );
		if( relatedCount >= 1 )
		    relArray[agent] = true;
		else
		    relArray[agent] = false;
		*/
	    }
	    
	    if( relatedCount >= 1 ) 
		gameCount++;
	    
	    if( !relatedMap.containsKey( gameState ) ) 
		relatedMap.put( gameState, relArray );
	    else
		relArray = null;
	}
	
	System.out.println("Game Count: "+gameCount);
    }
    
    private boolean isRelated( GameState gameState, int agent )
    {
	if( relatedMap == null || gameState == null || 
		agent < 0 || agent >= SparseGridWorld.NUM_AGENTS ) {
	    
	    System.out.println("UCEQAbsGame->isRelated: Wrong Parameters");
	    return false;
	}
	
	if( relatedMap.containsKey( gameState ) ) {
	    
	    boolean[] relArray = relatedMap.get( gameState );
	    return relArray[agent];
	}
	else {
	    
	    System.out.println("UCEQAbsGame->isRelated: No such State Key");
	    
	    for( int ag = 0; ag < SparseGridWorld.NUM_AGENTS; ag++ ) {
		
		System.out.print("Agent "+ag+": "+gameState.getLocationID(ag));
	    }
	    System.out.println();
	    
	    return false;
	}
    }
    
    
    //should be called by the game executor
    public void currentEpisode( int ep )
    {
	if( bLearning || ep < numExpEpisodes )
	    return;
	else {
	    
	    bLearning = true;
	    
	    readSimilarity();
	    
	    /**
	     * start to transfer the model and value function
	     */
	    transfer();
	}
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
	    
	    System.out.println("@uCEQ_TransPolicy->updateQ: NULL nextState!");
	    
	    return null;
	}
	else if( !bLearning ) {
	    
	    /**
	     * choose a random action
	     */
	    GameAction nextAction = new GameAction();
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		
		nextAction.setAction( agent, random.nextInt(GameAction.NUM_ACTIONS) );
	    }
	    
	    return nextAction;
	}
	else {
	    
	    /**
	     * compute the correlated equilibrium in the next state
	     */
	    double[] nashEquil = computeNE( agentIndex, nextState );
	    
	    /**
	     * then choose an action for each agent
	     * the computed CE may be null
	     * if the CE is null, then all agents should act according to their own tables
	     * else the involved agents take actions according to the CE 
	     * and other agents act according to their own tables
	     */
	    GameAction nextAction = new GameAction();
	    if( nashEquil == null ) {
		
		for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		    
		    int locState = nextState.getLocationID( agent );
		    int maxAction = getMaxAction( agent, locState );
		    nextAction.setAction( agent, maxAction );
		}
	    }
	    else {
		
		/**
		 * for unrelated agents
		 */
		for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		    
		    int locState = nextState.getLocationID( agent );
		    
		    if( !isRelated( nextState, agent ) ) {
			
			int maxAction = getMaxAction( agent, locState );
			nextAction.setAction( agent, maxAction );
		    }
		}
		/**
		 * for related agents, choose action 
		 * according to the correlated equilibrium
		 */
		ArrayList<Integer> involvedAgents = getInvolveAgents( nextState );
		GameAction neAction = getJointAction_NE( nashEquil, involvedAgents );
		for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		    
		    int locState = nextState.getLocationID( agent );
		    
		    if( isRelated( nextState, agent ) ) {
			
			nextAction.setAction( agent, neAction.getAction(agent) );
		    }
		}
	    }


	    /**
	     * update the Q-tables
	     * but if this is the initial state of the game
	     * just return the action
	     */
	    if( curState != null && jointAction != null 
		&& rewards != null ) {
		
		
		/**
		 * for the updated joint action 
		 * set the actions of the unrelated agents to 0
		 */
		GameAction updatedJntAction = new GameAction();
		for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		    
		    int curLocState = curState.getLocationID( agent );
		    if( isRelated( curState, agent ) )
			updatedJntAction.setAction( agent, jointAction.getAction(agent) );
		    else 
			updatedJntAction.setAction( agent, 0 );
		}
		
		/**
		 * mark a visit
		 */
		visit( curState, updatedJntAction );
	
		
		/**
		 * compute the value of the computed CE
		 */
		double[] nashValues = getNashQValues( nextState, nashEquil );
		
		/**
		 * there are four situations for each agent:
		 * unrelated in curState, unrelated in nextState,
		 * unrelated in curState, related in nextState,
		 * related in curState, unrelated in nextState,
		 * related in curState, related in nextState
		 */
		for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		    
		    int curLocState = curState.getLocationID( agent );
		    int curAction = jointAction.getAction( agent );
		    int nextLocState = nextState.getLocationID( agent );
		    
		    if( isRelated( curState, agent ) ) {
			
			double Qsa = getQValue( agent, curState, updatedJntAction );
			
			if( isRelated( nextState, agent ) ) {
			    
			    double CEQ = nashValues[agent];
			    Qsa = (1 - ALPHA) * Qsa + ALPHA * (rewards[agent] + GAMMA * CEQ );
			    
			    //double qmax = getMaxQvalue( agent, nextLocState );
			    //Qsa = (1 - ALPHA) * Qsa + ALPHA * (rewards[agent] + GAMMA * qmax );
			}
			else {
			    
			    double qmax = getMaxQvalue( agent, nextLocState );
			    Qsa = (1 - ALPHA) * Qsa + ALPHA * (rewards[agent] + GAMMA * qmax );
			}
			
			setQValue( agent, curState, updatedJntAction, Qsa );
		    }
		    else {
			
			double qsa = locQs[agent][curLocState][curAction];
			
			/**
			 * use the next correlated equilibirum value to 
			 * back up the current local Q-value
			 */
			if( isRelated( nextState, agent ) ) {
			    
			    double CEQ = nashValues[agent];
			    qsa = (1 - ALPHA) * qsa + ALPHA * (rewards[agent] + GAMMA * CEQ );
			    
			    //double qmax = getMaxQvalue( agent, nextLocState );
			    //qsa = (1 - ALPHA) * qsa + ALPHA * (rewards[agent] + GAMMA * qmax );
			}
			/**
			 * use the next local Q-value to back up the current local Q-value
			 */
			else {
			    
			    double qmax = getMaxQvalue( agent, nextLocState );
			    qsa = (1 - ALPHA) * qsa + ALPHA * (rewards[agent] + GAMMA * qmax );
			}	
			
			locQs[agent][curLocState][curAction] = qsa;
		    }
		    
		}
		
		//Alpha *= 0.99988;
		ALPHA *= 0.99988;//0.99985;//0.99999;//985;//988;//58;//0.9975;//0.99958;
		
		/**
		 * maybe we can release some memories
		 */
		nashValues = null;
	    }
	    
	    /**
	     * maybe we can release some memories
	     */
	    nashEquil = null;
	    
	    return nextAction;
	}
    }
    
    
    protected double[] computeNE( int agent, GameState gameState )
    {
	if( gameState == null ) {
	    
	    System.out.println("UCEQAbsGame->getJointAction: Null State");
	    return null;
	}
	else if( agent < 0 || agent >= SparseGridWorld.NUM_AGENTS ) {
	    
	    System.out.println("UCEQAbsGame->computeCE: Wrong agent index");
	    return null;
	}
	
	/**
	 * not every agent are involved in the game 
	 */
	ArrayList<Integer> involvedAgents = getInvolveAgents(gameState);
	
	if( involvedAgents.size() == 0 ) 
	    return null;
	/**
	 * just return the correlated equilibrium 
	 * for the involved agents
	 */
	else {
	    
	    int involvedAgentNum = involvedAgents.size();
	    
	    /**
	     * get all possible support size vector, sorted
	     * increasing order
	     * first order: the sum of the support size
	     * second order: the max difference of the support size
	     */
	    ArrayList<XVector> xList = generateXVectors( involvedAgentNum );
	    
	    for( int i = 0; i < xList.size(); i++ ) {
		    
		XVector xVector = xList.get(i);
		    
		/**
		 * initialize support and domain according to 
		 * the current xVector
		 */
		ArrayList<ArrayList<Support>> domainProfile = new ArrayList<ArrayList<Support>>();
		
		//arranged according to the order in the involved agent list!!
		for( int j = 0; j < involvedAgents.size(); j++ ) {
			
		    //the support domain of the j-th agent in the involved agent list!!
		    /**
		     * no need to modify the method generateSupportDomain since it is unrelated 
		     * to the number of agents
		     */
		    domainProfile.add( generateSupportDomain(xVector.getX(j)) );
		}
		    
		Support[] supportProfile = new Support[involvedAgentNum];
		for( int j = 0; j < involvedAgentNum; j++ ) {
			
		    supportProfile[j] = null;
		}
		    
		    
		/**
		 * call the method recursiveBackTracking
		 */
		double[] nashEquil = recursiveBackTracking( supportProfile, 
			domainProfile, 0, gameState, involvedAgents );
		if( nashEquil != null )
		    return nashEquil;
		    
		//release memory
		domainProfile = null;
		supportProfile = null;
	    }
	    
	    return null;
	}
    }
    
    
    //ok
    public ArrayList<XVector> generateXVectors( int involvedAgentNum )
    {
	
	ArrayList<XVector> retList = new ArrayList<XVector>();
	
	/**
	 * first according to sum of x
	 * 3 agents, each agent has 4 actions
	 * for each agent, at least one actions has a probability larger than 0
	 */
	int minSumX = involvedAgentNum;
	int maxSumX = GameAction.NUM_ACTIONS * involvedAgentNum;
	
	for( int sumX = minSumX; sumX <= maxSumX; sumX++ ) {
	    
	    PriorityQueue<XVector> queue = generateXVectors( sumX, involvedAgentNum );
	    
	    while( !queue.isEmpty()) {
		
		retList.add( queue.poll() );
	    }
	}
	
	
	return retList;
    }
    
    //ok
    private PriorityQueue<XVector> generateXVectors( int sum, int involvedAgentNum ) 
    {
    	/**
    	 * note that the head of a PriorityQueue is the minimum element
    	 */
    	PriorityQueue<XVector> retQueue = new PriorityQueue<XVector>();
    	
    	/**
    	 * one for each agent's support size
    	 */
    	int agentNum = involvedAgentNum;
    	int[] xs = new int[agentNum];
    	for( int agent = 0; agent < agentNum; agent++ ) {
    		
    		xs[agent] = 1;
    	}
    	
    	/**
    	 * generate all possible support size vectors
    	 */
    	while( true ){
    		
    	    /**
    	     * check the current support size vector
    	     */
    	    int sizeSum = 0;
    	    for( int agent = 0; agent < agentNum; agent++ )
    		sizeSum += xs[agent];
    		
    	    //add the current support size vector to the queue
    	    if( sizeSum == sum ) {
    			
    		XVector xVector = new XVector( xs, agentNum );
    			
    		if( !retQueue.contains( xVector ) )
    		    retQueue.add( xVector );
    		else
    		    xVector = null;
    	    }
    		
    	    /**
    	     * compute the next support size vector
    	     */
    	    for( int agent = agentNum-1; agent >= 0; agent-- ) {
				
    		/**
    		 * the action index of the current agent increase
    		 */
    		xs[agent] += 1;
				
    		if( agent > 0 && xs[agent] == GameAction.NUM_ACTIONS+1 ) {
					
    		    xs[agent] = 1;
					
    		    //then the next agent action should also increase
    		}
    		else
    		    break;
    	    }
			
    	    /**
    	     * whether to continue
    	     */
    	    if( xs[0] == GameAction.NUM_ACTIONS+1 )
    		break;
    	}
    	
    	return retQueue;
    }
    
    
    //ok
    protected double[] recursiveBackTracking( Support[] suppProfile, 
	    ArrayList<ArrayList<Support>> domProfile, int indexInList, GameState gameState, 
	    ArrayList<Integer> involvedAgents )
    {
	
	/**
	 * check parameters
	 */
	
	if( indexInList == involvedAgents.size() ) {
	    
	    //feasibility program
	    return feasibilityProgram( suppProfile, gameState );
	    
	}
	else {
	    
	    /**
	     * initialized the support of the current agent
	     */
	    ArrayList<Support> Di = domProfile.get( indexInList );
	    
	    while( !Di.isEmpty() ) {
		
		suppProfile[indexInList] = Di.remove(0);
		
		/**
		 * generate a new domain profile for IRSDS
		 * for each agent whose support has been instantiated, the domain 
		 * contains only that support
		 * 
		 * for each other agent, the domain contains all support that were 
		 * not eliminated in a previous call
		 */
		ArrayList<ArrayList<Support>> domPro_IRSDS = new ArrayList<ArrayList<Support>>();
		for( int i = 0; i <= indexInList; i++ ) {
		    
		    ArrayList<Support> dom = new ArrayList<Support>();
		    dom.add( suppProfile[i] );
		    
		    domPro_IRSDS.add( dom );
		}
		for( int i = indexInList+1; i < involvedAgents.size(); i++ ) {
		    
		    //from D_{i+1} to D_n, we use a new list
		    /**/
		    ArrayList<Support> dom = new ArrayList<Support>();
	    		    
		    for( int domListIndex = 0; domListIndex < domProfile.get(i).size(); domListIndex++ )
			dom.add( domProfile.get(i).get(domListIndex) );
    		    	
    		    
		    domPro_IRSDS.add( dom );
		}
		
		
		//if IRSDS succeeds, update domProfile using domPro
		//else, domProfile remains unchanged
		if( IRSDS( domPro_IRSDS, gameState, involvedAgents ) ) {
		    
		    //use an updated domain profile
		    double[] nashEquil = recursiveBackTracking( suppProfile, domPro_IRSDS, 
			    indexInList+1, gameState, involvedAgents );
		  
		    if( nashEquil != null )
			return nashEquil;
		}
	    }
	}
	
	
	return null;
    }
    
    //ok
    private boolean IRSDS( ArrayList<ArrayList<Support>> domPro, GameState gameState,
	    ArrayList<Integer> involvedAgents )
    {
	/**
	 * check the parameter
	 */
	if( domPro == null || gameState == null ||
		involvedAgents == null ||
		involvedAgents.size() < 2 ||
		domPro.size() != involvedAgents.size() ) {
	    
	    System.out.println("@NashQAbsGame->IRSDS: Wrong Parameters!");
	    
	    return false;
	}
	
	int numAgents = involvedAgents.size();
	boolean changed = true;
	while( changed ) {
	    
	    changed = false;
	    
	    /**
	     * loop for all agent index in the involved agents list
	     */
	    for( int agentListIndex_i = 0; agentListIndex_i < numAgents; agentListIndex_i++ ) {
		
		int agentIndex_i = involvedAgents.get( agentListIndex_i );
		
		/**
		 * get the domain of agent i's support Di
		 * and compute the union support of all elements in Di
		 */
		ArrayList<Support> Di = domPro.get( agentListIndex_i );
		
		//if Di is empty already
		if( Di.isEmpty() )
		    return false;
		
		//no need to modify this method
		Support cupSupp = supportCup( Di );
		
		/**
		 * loop for actions supported by cupSupp
		 */
		for( int a_i = 0; a_i < GameAction.NUM_ACTIONS; a_i++ ) {
		    
		    if( !cupSupp.supported(a_i) )
			continue;
		    
		    /**
		     * loop for other actions in Ai
		     */
		    for( int ap_i = 0; ap_i < GameAction.NUM_ACTIONS; ap_i++ ) {
			
			if( ap_i == a_i )
			    continue;
			
			/**
			 * check whether a is conditionally dominated by ap
			 */
			Support[] otherCups = new Support[numAgents];
			for( int j = 0; j < numAgents; j++ ) {
			    
			    if( j == agentListIndex_i ) {
				
				otherCups[j] = null; 
				continue;
			    }
			    
			    ArrayList<Support> Dj = domPro.get(j);
			    
			    //if Dj is empty already
			    if( Dj.isEmpty() )
				return false;
				
			    otherCups[j] = supportCup( Dj );
			}
			
			//generate all possible joint actions
			ArrayList<GameAction> othJntActList_a = generateOtherJntActions( agentListIndex_i, otherCups, involvedAgents );
			ArrayList<GameAction> othJntActList_ap = generateOtherJntActions( agentListIndex_i, otherCups, involvedAgents );
			
			
			//determine whether a is conditionally dominated by ap
			/**
			 * conditionally dominated:
			 * given a profile set of available actions R_{-i} \subset A_{-i}
			 * for an action a_i and another action a_ip
			 * if for all a_{-i} \in R__{-i}, there holds:
			 * u_i(a_i,a_{-i}) < u_i(a_ip,a_{-i})
			 * then a_i is conditionally dominated by a_ip
			 */
			boolean condDominated = true;
			for( int listIndex = 0; listIndex < othJntActList_a.size(); listIndex++ ) {
	    
			    GameAction jntAction_a = othJntActList_a.get(listIndex);
			    GameAction jntAction_ap = othJntActList_ap.get(listIndex);
					
			    jntAction_a.setAction( agentIndex_i, a_i );
			    jntAction_ap.setAction( agentIndex_i, ap_i );
					
			    double Q_sa = getQValue( agentIndex_i, gameState, jntAction_a );
			    double Q_sap = getQValue( agentIndex_i, gameState, jntAction_ap );
	    
			    if( Q_sa >= Q_sap ) {
					    
				condDominated = false;
				break;
			    }
			}
			
			/**
			 * if a is conditionally dominated by ap 
			 * then remove the supports which support a in Di 
			 */
			if( condDominated ) {
			    
			    for( int suppIndex = 0; suppIndex < Di.size(); suppIndex++ ) {
				
				Support supp = Di.get( suppIndex );
				if( supp.supported( a_i ) ) {
				    
				    Di.remove( suppIndex );
				    suppIndex--;
				}
			    }
			    
			    changed = true;
			    
			    /**
			     * return failure when Di is empty
			     */
			    if( Di.isEmpty() )
				return false;
			}
		    }
		}
		
	    }
	    
	}
	
	return true;
    }
    
    //ok
    private ArrayList<GameAction> generateOtherJntActions( int agentListIndex_i, 
	    Support[] suppProfile, ArrayList<Integer> involvedAgents )
    {
    	
    	if( suppProfile == null || 
    		involvedAgents == null ||
    		involvedAgents.size() < 2 ||
    		suppProfile.length != involvedAgents.size() ) {
	    
    		System.out.println("@generateOtherJntActions: Wrong Parameter!");
    		return null;
    	}
    	
    	int agentNum = involvedAgents.size();
    	ArrayList<GameAction> retJntActions = new ArrayList<GameAction>();
    	
    	
    	//agents' actions for iteration
    	int[] agentActionsIter = new int[agentNum];
    	for( int agent = 0; agent < agentNum; agent++ )
    	    agentActionsIter[agent] = 0;
		
    	boolean cont = true;
    	while( cont ) {
			
    	    /**
    	     * set the current partial joint action
    	     */
    	    GameAction jntAction = new GameAction();
    	    for( int agentListIndex = 0; agentListIndex < involvedAgents.size(); agentListIndex++ ) {
    		
    		int agentIndex = involvedAgents.get( agentListIndex );
    		jntAction.setAction( agentIndex, agentActionsIter[agentListIndex] );
    	    }
    	    
    	    
    	    /**
    	     * add to the list according to the support profile
    	     */
    	    boolean bAdd = true;
    	    for( int agentListIndex = 0; agentListIndex < agentNum; agentListIndex++ ) {
				
    		if( agentListIndex == agentListIndex_i )
    		    continue;
    		
    		int agentIndex = involvedAgents.get( agentListIndex );
    		if( !suppProfile[agentListIndex].supported( jntAction.getAction(agentIndex)) ) {
					
    		    bAdd = false;
    		    break;
    		}
    	    }
    	    if( bAdd )
    		retJntActions.add( jntAction );
			
    	    /**
    	     * compute the next joint action
    	     */
    	    int agentListIndex_Last = agentNum-1;
    	    int agentListIndex_First = 0;
    	    if( agentListIndex_i == 0 )
    		agentListIndex_First = 1;
    	    if( agentListIndex_i == agentNum-1 )
    		agentListIndex_Last = agentNum-2;
    	    
    	    for( int agentListIndex_p = agentListIndex_Last; agentListIndex_p >= agentListIndex_First; agentListIndex_p-- ) {
				
    		if( agentListIndex_p == agentListIndex_i )
    		    continue;
				
    		/**
    		 * the action index of the current agent increase
    		 */
    		agentActionsIter[agentListIndex_p] += 1;
				
    		if( agentListIndex_p > agentListIndex_First && 
    			agentActionsIter[agentListIndex_p] == GameAction.NUM_ACTIONS ) {
					
    		    agentActionsIter[agentListIndex_p] = 0;
					
    		    //then the next agent action should also increase
    		}
    		else
    		    break;
    	    }
			
    	    /**
    	     * whether to continue
    	     */
    	    if( agentActionsIter[agentListIndex_First] == GameAction.NUM_ACTIONS )
    		break;
    	}
		
    	return retJntActions;
    }
    
    //ok
    protected double[] feasibilityProgram( Support[] suppProfile, GameState gameState, 
	    ArrayList<Integer> involvedAgents )
    {
	if( suppProfile == null || 
		involvedAgents == null || 
		involvedAgents.size() < 2 ||
		suppProfile.length != involvedAgents.size() ) {
	    
	    System.out.println("@NashQ->feasibilityProgram: Wrong Parameter!");
	    return null;
	}
	
	double[] solution = null;
	
	try {
	    
	    /**
	     * 1. firstly, create the model
	     * IloCplex is used to create Mathmatical Programming Models
	     * such as:
	     * Linear Programming
	     * Quadratic Programming
	     * Quadratically Constrained Program...
	     */
	    //IloCP nashCP = new IloCP();
	    
	    IloCplex nashCP = new IloCplex();
	    
	    nashCP.setParam(IloCplex.Param.WorkMem, 2000);
	   // nashCP.setParameter(IloCP.ParameterValues.,2);
	    
	    /**
	     * 2. secondly, create the variables:
	     * stores each agent's probability of taking each action
	     * agent i's j-th action probability: index = i * Num_Actions
	     */
	    int numAgents = involvedAgents.size();
	    int varNum = numAgents * numAgents;
	    double[] lowBounds = new double[numAgents * GameAction.NUM_ACTIONS];
	    double[] uppBounds = new double[numAgents * GameAction.NUM_ACTIONS];
	    for( int varIndex = 0; varIndex < numAgents * GameAction.NUM_ACTIONS; varIndex++ ) {
		
		lowBounds[varIndex] = 0.0;
		uppBounds[varIndex] = 1.0;
	    }
	    IloNumVar[] p = nashCP.numVarArray(numAgents*GameAction.NUM_ACTIONS, lowBounds, uppBounds);
	    
	    
	    /**
	     * 3. then create the constraints:
	     */
	    
	    //3.1 for the sum of each agent's action probability
	    //\Sum_{a_i \in S_i} p_i(a_i) = 1
	    for( int agentListIndex_i = 0; agentListIndex_i < numAgents; agentListIndex_i++ ) {
		
		Support supp_i = suppProfile[agentListIndex_i];
		double[] coeff = new double[numAgents * GameAction.NUM_ACTIONS];
		for( int coeffIndex = 0; coeffIndex < numAgents*GameAction.NUM_ACTIONS; coeffIndex++ ) {
		    
		    int agentListIndex_j = coeffIndex / GameAction.NUM_ACTIONS;
		    int act = coeffIndex % GameAction.NUM_ACTIONS;
		    
		    if( agentListIndex_j != agentListIndex_i )
			coeff[coeffIndex] = 0.0;
		    else if( !supp_i.supported(act) )
			coeff[coeffIndex] = 0.0;
		    else
			coeff[coeffIndex] = 1.0;
		}
		
		IloNumExpr sumExpr = nashCP.scalProd(coeff, p);
		nashCP.addEq( sumExpr, 1.0 );
	    }
	    
	    //3.2 for each agent's unsupported action
	    //for any a_i \not\in S_i p_i(a_i) = 0
	    for( int agentListIndex_i = 0; agentListIndex_i < numAgents; agentListIndex_i++ ) {
		
		Support supp_i = suppProfile[agentListIndex_i];
		if( supp_i.supportSize() == GameAction.NUM_ACTIONS )
		    continue;
		
		for( int act = 0;  act < GameAction.NUM_ACTIONS; act++ ) {
		    
		    //should be !supported?
		    if( !supp_i.supported( act ) ) {
			
			double[] coeff = new double[numAgents * GameAction.NUM_ACTIONS];
			for( int coeffIndex = 0; coeffIndex < numAgents*GameAction.NUM_ACTIONS; coeffIndex++ ) {
			    
			    if( coeffIndex == (agentListIndex_i * GameAction.NUM_ACTIONS + act) )
				coeff[coeffIndex] = 1.0;
			    else
				coeff[coeffIndex] = 0.0;
			}
			
			IloNumExpr eqzeroExpr = nashCP.scalProd( coeff, p );
			nashCP.addEq( eqzeroExpr, 0.0 );
		    }
		}
	    }
	    
	    //3.3 for the nash condition inequality
	    //for any i \in N, any a_i \in S_i and any a_i' \not\in S_i
	    //\Sum_{a_{-i} \in S_{-i}} p(a_{-i}[u_i(a_i,a_{-i})-u_i(a_i',a_{-i})] >= 0
	    for( int agentListIndex_i = 0; agentListIndex_i < numAgents; agentListIndex_i++ ) {
		    
		int agentIndex_i = involvedAgents.get( agentListIndex_i );
		
		Support support_i = suppProfile[agentListIndex_i];
		    
		/**
		 * loop for all supported actions
		 */
		for( int a_i = 0; a_i < GameAction.NUM_ACTIONS; a_i++ ) {
			
		    if( !support_i.supported( a_i ) )
			continue;
			
		    /**
		     * loop for all actions that are not supported
		     */
		    for( int ap_i = 0; ap_i < GameAction.NUM_ACTIONS; ap_i++ ) {
			    
			if( support_i.supported( ap_i ) )
			    continue;
			    
			    
			/**
			 * loop for the joint actions of the other agent's supported!! actions
			 */
			ArrayList<GameAction> othJntActList_a = generateOtherJntActions( agentListIndex_i, suppProfile, involvedAgents );
			ArrayList<GameAction> othJntActList_ap = generateOtherJntActions( agentListIndex_i, suppProfile, involvedAgents );
			
			IloNumExpr ineqExpr = nashCP.constant(0);
			for( int listIndex = 0; listIndex < othJntActList_a.size(); listIndex++ ) {
					
			    GameAction jntAction_a = othJntActList_a.get(listIndex);
			    GameAction jntAction_ap = othJntActList_ap.get(listIndex);
			    jntAction_a.setAction( agentIndex_i, a_i);
			    jntAction_ap.setAction( agentIndex_i, ap_i);
					
			    double Q_sa = getQValue( agentIndex_i, gameState, jntAction_a );
			    double Q_sap = getQValue( agentIndex_i, gameState, jntAction_ap );
			    double coeff = Q_sa - Q_sap;
					
			    //variable indices
			    IloNumExpr itemExpr = nashCP.constant(coeff);
			    for( int agentListIndex_j = 0; agentListIndex_j < numAgents; agentListIndex_j++ ) {
						
				if( agentListIndex_j == agentListIndex_i )
				    continue;
					
				int agentIndex_j = involvedAgents.get( agentListIndex_j );
				int act_j = jntAction_a.getAction( agentIndex_j );
				int varIndex_j = agentListIndex_j * GameAction.NUM_ACTIONS + act_j;
						
				itemExpr = nashCP.prod( itemExpr, p[varIndex_j] );
			    }
			    //add the item to the inequality expression
			    ineqExpr = nashCP.sum( ineqExpr, itemExpr );
			}
			/**
			 * create the inequality constraint
			 * 
			 * be careful! should be greater than 0 
			 * not less than 0
			 */
			nashCP.addGe( ineqExpr, 0 );
		    }//loop for unsupported actions of agent_i
		}//loop for supported actions of agent_i
		
		
		/**
		 * all supported actions has equal values
		 */
		int firstSuppAction = 0;
		for( int a_i = 0; a_i < GameAction.NUM_ACTIONS; a_i++ ) {
				
		    if( support_i.supported(a_i) ) {
			
			firstSuppAction = a_i;
			break;
		    }
		}
		for( int a_i = 0; a_i < GameAction.NUM_ACTIONS; a_i++ ) {
				
		    if( !support_i.supported(a_i) || a_i == firstSuppAction )
			continue;
				
		    IloNumExpr eqExpr = nashCP.constant(0);
		    
		    //the joint actions of the other agents
		    //should also be supported
		    ArrayList<GameAction> othJntActList_afs = generateOtherJntActions( agentListIndex_i, suppProfile, involvedAgents );
		    ArrayList<GameAction> othJntActList_a = generateOtherJntActions( agentListIndex_i, suppProfile, involvedAgents );
		    
		    for( int listIndex = 0; listIndex < othJntActList_a.size(); listIndex++ ) {
			
			GameAction jntAction_a = othJntActList_a.get(listIndex);
			GameAction jntAction_afs = othJntActList_afs.get(listIndex);
			jntAction_a.setAction( agentIndex_i, a_i);
			jntAction_afs.setAction( agentIndex_i, firstSuppAction);
					
			double Q_sa = getQValue( agentIndex_i, gameState, jntAction_a );
			double Q_sap = getQValue( agentIndex_i, gameState, jntAction_afs );
			double coeff = Q_sa - Q_sap;
					
			//variable indices
			IloNumExpr itemExpr = nashCP.constant(coeff);
			for( int agentListIndex_j = 0; agentListIndex_j < numAgents; agentListIndex_j++ ) {
						
			    if( agentListIndex_j == agentListIndex_i )
				continue;
					
			    int agentIndex_j = involvedAgents.get( agentListIndex_j );
			    int act_j = jntAction_a.getAction( agentIndex_j );
			    int varIndex_j = agentListIndex_j * GameAction.NUM_ACTIONS + act_j;
				
			    itemExpr = nashCP.prod( itemExpr, p[varIndex_j] );
			}
			eqExpr = nashCP.sum( eqExpr, itemExpr );
		    }
		    nashCP.addEq( eqExpr, 0.0 );
		}//loop for the supported actions of agent_i, except the firstSuppAction
	    }//loop for agent_i
	    
	    
	    
	    /**
	     * 5. all constraints have been set
	     * then we should solve this QCP
	     */
	    if( nashCP.solve() ) {
		
		//solution = new double[varNum]; 
		//nashCP.getValues( p, solution );
		
		solution = nashCP.getValues( p );
		
		/**
		for( int index = 0; index < GridWorld.NUM_AGENTS * GameAction.NUM_ACTIONS; index++ ) {
		    
		    System.out.println("Solution "+index+": "+solution[index]);
		}
		*/
	    }
	    nashCP.end();
	    
	    //release the memory??
	    nashCP = null;
	}
	
	catch( IloException iloE ) {
	    
	    System.err.println("Concert exception '" + iloE + "' caught");
	}

	
	return solution;
    }
    
    
    
    private ArrayList<Integer> getInvolveAgents( GameState gameState )
    {
	if( gameState == null ) {
	    
	    System.out.println("UCEQAbsGame->getInvolveAgents: Null State");
	    return null;
	}
	
	ArrayList<Integer> involvedAgents = new ArrayList<Integer>();
	for( int ag = 0; ag < SparseGridWorld.NUM_AGENTS; ag++ ) {
	    
	    int locState = gameState.getLocationID( ag );
	    if( isRelated( gameState, ag ) )
		involvedAgents.add( ag );
	}
	
	return involvedAgents;
    }
    


    /**
     * get the max action according to an agent's own table
     */
    private int getMaxAction( int agent, int locState ) 
    {
	
	if( agent < 0 || agent >= SparseGridWorld.NUM_AGENTS ) {
	    
	    System.out.println("@UCEQAbsGame->getMaxAction: Wrong Agent Index!");
	    return -1;
	}
	if( locState < 0 || 
		locState >= SparseGridWorld.NUM_CELLS ) {
	    
	    System.out.println("@UCEQAbsGame->getMaxAction: Wrong Local State!");
	    return -1;
	}
	
	double maxQ = Double.NEGATIVE_INFINITY;
	int maxAction = 0;
	
	for( int action = 0; action < GameAction.NUM_ACTIONS; action++ ) {
	    
	    if( locQs[agent][locState][action] > maxQ ) {
		
		maxQ = locQs[agent][locState][action];
		maxAction = action;
	    }
	}
	
	/**
	 * if there are several max actions
	 */
	ArrayList<Integer> maxActionList = new ArrayList<Integer>();
	maxActionList.add( maxAction );
	for( int action = 0; action < GameAction.NUM_ACTIONS; action++ ) {
	    
	    if( action == maxAction ) 
		continue;
	    
	    double qValue = locQs[agent][locState][action];
	    if( Math.abs( qValue - maxQ ) < 0.0001 ) {
		
		maxActionList.add( action );
	    }
	}
	
	int chosenIndex = random.nextInt( maxActionList.size() );
	int retAction = maxActionList.get( chosenIndex );
	
	return retAction;
    }
    
    private double getMaxQvalue( int agent, int locState )
    {
	if( agent < 0 || agent >= SparseGridWorld.NUM_AGENTS ) {
	    
	    System.out.println("@UCEQAbsGame->getMaxQvalue: Wrong Agent Index!");
	    return -1;
	}
	if( locState < 0 || 
		locState >= SparseGridWorld.NUM_CELLS ) {
	    
	    System.out.println("@UCEQAbsGame->getMaxQvalue: Wrong Local State!");
	    return -1;
	}
	
	double maxQ = Double.NEGATIVE_INFINITY;
	int maxAction = 0;
	
	for( int action = 0; action < GameAction.NUM_ACTIONS; action++ ) {
	    
	    if( locQs[agent][locState][action] > maxQ ) {
		
		maxQ = locQs[agent][locState][action];
		maxAction = action;
	    }
	}
	
	return maxQ;
    }
    
    //ok
    private GameAction getJointAction_NE( double[] nashEquil, ArrayList<Integer> involvedAgents )
    {
	GameAction retAction = new GameAction();
	
	if( nashEquil == null || 
		involvedAgents == null ) {
	    
	    System.out.println("NashQAbsGame->getJointAction_NE: Null Parameters");
	}
	else if( involvedAgents.size() <= 1 ) {
	    
	    System.out.println("NashQAbsGame->getJointAction_NE: Only one agent involved");
	}
	
	if( nashEquil == null )
	{
	    //System.out.println("@CenNashQ->getJointAction_NE: NULL Nash Equilibrium!");
	    
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		
		retAction.setAction( agent, random.nextInt(GameAction.NUM_ACTIONS));
	    }
	}
	else {
	    
	    for( int agentListIndex = 0; agentListIndex < involvedAgents.size(); agentListIndex++ ) {
		
		int agentIndex = involvedAgents.get( agentListIndex );
		
		double probability = 0.0;
		double randPro = random.nextDouble();
		
		for( int action = 0; action < GameAction.NUM_ACTIONS; action++ ) {
		    
		    int compIndex = agentListIndex * GameAction.NUM_ACTIONS + action;
		    probability += nashEquil[compIndex];
		    
		    if( randPro < probability ) {
			
			retAction.setAction( agentIndex, action );
			break;
		    }
		}
		
	    }
	}
	
	return retAction;
    }
    
    
    //ok
    protected double[] getNashQValues( GameState gameState, double[] nashE )
    {
	double[] values = new double[SparseGridWorld.NUM_AGENTS];
	for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ )
	    values[agent] = 0.0;
	
	if( nashE == null ) {
	    
	    return values;
	}
	
	int stateIndex = SparseGridWorld.queryStateIndex( gameState );
	ArrayList<Integer> involvedAgents = getInvolveAgents( gameState );
	ArrayList<GameAction> subJointActions = generateJointActions( involvedAgents );
	int numAgents = involvedAgents.size();
	
	for( int actionIndex = 0; actionIndex < subJointActions.size(); actionIndex++ ) {
	    
	    GameAction jntAction = subJointActions.get( actionIndex );
	    StateActionPair saPair = new StateActionPair( gameState, jntAction );
	    
	    /**
	     * set all other unrelated agents' action to 0!!!
	     */
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		
		if( !isRelated( gameState, agent) )
		    jntAction.setAction( agent, 0 );
	    }
	    
	    //get the component of each agent in Nash equilibrium
	    double jointPro = 1.0;
	    for( int agentListIndex = 0; agentListIndex < numAgents; agentListIndex++ ) {
		
		int agentIndex = involvedAgents.get( agentListIndex );
		int compIndex = agentListIndex * GameAction.NUM_ACTIONS + jntAction.getAction(agentIndex);
		jointPro *= nashE[compIndex];
	    }
	    
	    //for robust
	    /**
	    if( !Qs.containsKey( saPair ) ) {
		
		saPair = null;
		return values;
	    }
	    
	    float[] qEntry = Qs.get( saPair );
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		
		values[agent] += qEntry[agent] * jointPro;
	    }
	    */
	    
	    /**/
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		
		values[agent] += valueFunc[agent][stateIndex][actionIndex] * jointPro;
	    }
	}
	
	return values;
    }
    
    

}
