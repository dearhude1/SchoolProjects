package algorithms.pCEQ;

import gameGridWorld.GameAction;
import gameGridWorld.GameState;
import gameGridWorld.SparseGridWorld;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import drasys.or.matrix.VectorI;
import drasys.or.mp.Constraint;
import drasys.or.mp.Problem;
import drasys.or.mp.SizableProblemI;
import drasys.or.mp.lp.DenseSimplex;
import drasys.or.mp.lp.LinearProgrammingI;

public class PCEQAbsGame extends PCEQ
{

    /**
     * local Q-table for single-agent learning
     * note that this algorithm does not transfer value 
     * functions from Q-learning or R-max learning
     */
    protected double[][][] locQs;
    
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
    
    protected boolean bLearning = false;
    
    protected HashMap<GameState, boolean[]> relatedMap;
    
    private double Alpha = 0.99;
    
    public PCEQAbsGame()
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
    
    public PCEQAbsGame( double alpha, double gamma, double epsilon )
    {
	super(alpha, gamma, epsilon);
	
	int agentNum = SparseGridWorld.NUM_AGENTS;
	int locNum = SparseGridWorld.NUM_CELLS;
	int actionNum = GameAction.NUM_ACTIONS;
	
	transCndValues = new double[agentNum];
	transCndValues[0] = 11;//1.0;//1.5;//5;
	transCndValues[1] = 11;//2.0;//3.0;//5;
	
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
	    
	    System.out.println("PCEQAbsGame->isRelated: Wrong Parameters");
	    return false;
	}
	
	if( relatedMap.containsKey( gameState ) ) {
	    
	    boolean[] relArray = relatedMap.get( gameState );
	    return relArray[agent];
	}
	else {
	    
	    System.out.println("PCEQAbsGame->isRelated: No such State Key");
	    
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
	    double[] correlEquil = computeCE( agentIndex, nextState );
	    
	    /**
	     * then choose an action for each agent
	     * the computed CE may be null
	     * if the CE is null, then all agents should act according to their own tables
	     * else the involved agents take actions according to the CE 
	     * and other agents act according to their own tables
	     */
	    GameAction nextAction = new GameAction();
	    if( correlEquil == null ) {
		
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
		GameAction ceAction = getJointAction_CE( correlEquil, involvedAgents );
		for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		    
		    int locState = nextState.getLocationID( agent );
		    
		    if( isRelated( nextState, agent ) ) {
			
			nextAction.setAction( agent, ceAction.getAction(agent) );
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
		double[] correlValues = getCEQValues( nextState, correlEquil );
		

		
		double alpha = getVariableAlpha( curState, updatedJntAction );
		
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
			    
			    double CEQ = correlValues[agent];
			    Qsa = (1 - ALPHA) * Qsa + ALPHA * (rewards[agent] + GAMMA * CEQ );
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
			    
			    double CEQ = correlValues[agent];
			    qsa = (1 - ALPHA) * qsa + ALPHA * (rewards[agent] + GAMMA * CEQ );
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
		ALPHA *= 0.9991;//985;//988;//58;//0.9975;//0.99958;
		
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
    
    
    protected double[] computeCE( int agent, GameState gameState )
    {
	if( gameState == null ) {
	    
	    System.out.println("PCEQAbsGame->getJointAction: Null State");
	    return null;
	}
	else if( agent < 0 || agent >= SparseGridWorld.NUM_AGENTS ) {
	    
	    System.out.println("PCEQAbsGame->computeCE: Wrong agent index");
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
	else
	    return computeCE_plutocratic( gameState, involvedAgents );
    }
    
    
    
    private double[] computeCE_plutocratic( GameState gameState, 
	    ArrayList<Integer> involvedAgents )
    {
	if( gameState == null ) {
	    
	    System.out.println("PCEQAbsGame->computeCE_utilitarian: Null State");
	    return null;
	}
	else if( involvedAgents == null ) {
	    
	    System.out.println("PCEQAbsGame->computeCE_utilitarian: Null Agent List");
	    return null;
	}
	else if( involvedAgents.size() == 1 ) {
	    
	    System.out.println("PCEQAbsGame->computeCE_utilitarian: Only one agent involved");
	    return null;
	}
	
	//System.out.println("In a Game");
	
	int varNum = 1;
	for( int listIndex = 0; listIndex < involvedAgents.size(); listIndex++ ) 
	    varNum *= GameAction.NUM_ACTIONS;
	
	//an estimation of the number of constraints
	int consNum = 300;
	
	SizableProblemI problem = new Problem( consNum, varNum );
	problem.getMetadata().put("lp.isMaximize", "true");
	
	double[] ce = new double[varNum];
	for( int i = 0; i < varNum; i++ )
	    ce[i] = 0.0;
	
	try {
	    
	    int count = 0;
	    double maxAns = Double.NEGATIVE_INFINITY;
	    
	    ArrayList<GameAction> subJntActionList = generateJointActions( involvedAgents );
	    for( int agentListIndex = 0; agentListIndex < involvedAgents.size(); agentListIndex++ ) {
			
		int agentIndex = involvedAgents.get( agentListIndex );
		
		/**
		 * set the objective function
		 * maximize the sum of all joint actions' utilities
		 */
		for( int actionIndex = 0; actionIndex < subJntActionList.size(); actionIndex++ ) {
			
		    GameAction jntAction = subJntActionList.get( actionIndex );
			
		    /**
		     * set the action of all unrelated agents to 0
		     */
		    for( int ag = 0; ag < SparseGridWorld.NUM_AGENTS; ag++ ) {
			    
			if( !isRelated( gameState, ag ) )
			    jntAction.setAction( ag, 0 );
		    }
			
		    /**
		     * all joint actions corresponding to the agent's action 
		     * should be updated!!!
		     */
		    double coeff = getQValue( agentIndex, gameState, jntAction );
			
		    if( count == 0 )
			problem.newVariable(""+actionIndex).setObjectiveCoefficient( coeff );
		    else
			problem.getVariable(""+actionIndex).setObjectiveCoefficient( coeff );
		}
		
		/**
		 * set the constraints of the problem
		 */
		if( count == 0 ) {
		 
		    count++;
		    setConstraints_CE( problem, gameState, involvedAgents );
		}
		else
		    count++;
		
		
		/**
		 * set up the linear programming 
		 * and solve it
		 */
		LinearProgrammingI iLP;
		iLP = new DenseSimplex(problem);
		double ans = iLP.solve();
		    
		if( ans > maxAns ) {
				
		    maxAns = ans;
		    VectorI v = iLP.getSolution();
		    ce = v.getArray();
		}
	    }
	    
	    return ce;
	}
	catch (Exception e) {
	    // TODO: handle exception
	    return null;
	}
    }
    
    
    protected ArrayList<Integer> getInvolveAgents( GameState gameState )
    {
	if( gameState == null ) {
	    
	    System.out.println("PCEQAbsGame->getInvolveAgents: Null State");
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
     * set the constraints according to the conditions of correlated equilibrium
     */
    protected void setConstraints_CE( SizableProblemI problem, GameState gameState, 
	    ArrayList<Integer> agentList ) throws Exception
    {
	
	if( gameState == null ) {
	    
	    System.out.println("PCEQAbsGame->setConstraints_CE: Null State");
	    return;
	}
	else if( agentList == null ) {
	    
	    System.out.println("PCEQAbsGame->setConstraints_CE: Null Agent List");
	    return;
	}
	else if( agentList.size() == 1 ) {
	    
	    System.out.println("PCEQAbsGame->setConstraints_CE: Only one agent involved");
	    return;
	}
	
	int varNum = 1;
	for( int listIndex = 0; listIndex < agentList.size(); listIndex++ ) 
	    varNum *= GameAction.NUM_ACTIONS;
	
	/**
	 * the constraint of correlated equilibrium condition
	 */
	for( int agentListIndex = 0; agentListIndex < agentList.size(); agentListIndex++ ) {
	    
	    int agentIndex = agentList.get( agentListIndex );
	    
	    /**
	     * loop for action ai
	     */
	    for( int ai = 0; ai < GameAction.NUM_ACTIONS; ai++ ) {
	    
		/**
		 * loop for action ai'
		 */
		for( int aip = 0; aip < GameAction.NUM_ACTIONS; aip++ ) {
		    
		    if( aip == ai )
			continue;
		    
		    /**
		     * the name of the constraint:
		     * agentIndex(ai-ai')
		     */
		    String aiString = GameAction.getActionString(ai);
		    String aipString = GameAction.getActionString(aip);
		    String conString = agentIndex+"("+aiString+"-"+aipString+")";
		    
		    problem.newConstraint(conString).setType(Constraint.GREATER).
		    	setRightHandSide(0.0).getRowIndex();
		    
		    /**
		     * set the coefficient
		     */
		    //generate all other joint actions
		    ArrayList<GameAction> othJntActList_a = generateOtherJntActions( agentIndex, agentList );
		    ArrayList<GameAction> othJntActList_ap = generateOtherJntActions( agentIndex, agentList );
		    for( int othJntActionIndex = 0; othJntActionIndex < othJntActList_a.size(); othJntActionIndex++ ) {
			
			GameAction jntAction_a = othJntActList_a.get( othJntActionIndex );
			GameAction jntAction_ap = othJntActList_ap.get( othJntActionIndex );
			jntAction_a.setAction( agentIndex, ai );
			jntAction_ap.setAction( agentIndex, aip );
			
			/**
			 * set the action of all unrelated agents to 0
			 */
			for( int ag = 0; ag < SparseGridWorld.NUM_AGENTS; ag++ ) {
			    
			    
			    if( !isRelated( gameState, ag ) ) {
				
				jntAction_a.setAction( ag, 0 );
				jntAction_ap.setAction( ag, 0 );
			    }
			}
			
			double Q_sa = getQValue( agentIndex, gameState, jntAction_a );
			double Q_sap = getQValue( agentIndex, gameState, jntAction_ap );
			double coeff = Q_sa - Q_sap;
			
			/**
			 * variable index??
			 */
			String variableName = getVariableName( jntAction_a, agentList );
			problem.setCoefficientAt( conString, variableName, coeff );
		    }
		}
	    
	    }
	}
	
	/**
	 * the constraint that the sum of all 
	 * joint actions' probabilities is 1
	 */
	String eqCon = "equalConstraint";
	int index = problem.newConstraint(eqCon).
		setType(Constraint.EQUAL).setRightHandSide(1.0).getRowIndex();
	for( int i = 0; i < varNum; i++ )
	{
	    problem.setCoefficientAt( index, i, 1.0);
	}
	
	/**
	 * the constraint of each joint action 
	 * that its probability is larger than 0
	 */
	for( int i = 0; i < varNum; i++ )
	{
	    String zeroCon = "aboveZero" + (i+1);
	    index = problem.newConstraint(zeroCon).
	    	setType(Constraint.GREATER).setRightHandSide(0.0).getRowIndex();
	    problem.setCoefficientAt( index, i, 1.0);
	}
	
    }
    
    /**
     * generate the joint action list of the specified agents
     * @param agentList
     * @return
     */
    private ArrayList<GameAction> generateJointActions( ArrayList<Integer> agentList )
    {
	if( agentList == null ) {
	 
	    System.out.println("PCEQAbsGame->generateJointActions: Null List");
	    return null;
	}
	else if( agentList.size() <= 1 ) {
	    
	    System.out.println("PCEQAbsGame->generateJointActions: List Size Wrong");
	    return null;
	}
	
	ArrayList<GameAction> retList = new ArrayList<GameAction>();
	
	int agentNum = agentList.size();
	int[] actionIterator = new int[agentNum];
	
	for( int agent = 0; agent < agentNum; agent++ ) {
	    
	    actionIterator[agent] = 0;
	}
	
	while( true ) {
	    
	    GameAction gameAction = new GameAction();
	    
	    for( int listIndex = 0; listIndex < agentList.size(); listIndex++ ) {
		
		int agentIndex = agentList.get( listIndex );
		gameAction.setAction( agentIndex, actionIterator[listIndex] );
	    }
	    
	    if( !retList.contains(gameAction) )
		retList.add( gameAction );
	    else 
		gameAction = null;
	    
	    /**
	     * move to the next action
	     */
	    for( int agent = agentNum-1; agent >= 0; agent-- ) {
		
		actionIterator[agent] += 1;
		
		if( agent > 0 && actionIterator[agent] >= GameAction.NUM_ACTIONS ) {
		    
		    actionIterator[agent] = 0;
		}
		else
		    break;
	    }
	    
	    /**
	     * check the stop condition
	     */
	    if( actionIterator[0] >= GameAction.NUM_ACTIONS ) {
		
		break;
	    }
	}
	
	return retList;
    }
    
    private ArrayList<GameAction> generateOtherJntActions( int agentIndex, 
	    ArrayList<Integer> agentList )
    {
	
	if( agentIndex < 0 || 
		agentIndex >= SparseGridWorld.NUM_AGENTS ) {
	    
	    System.out.println("PCEQAbsGame->generateOtherJntActions: Wrong Agent Index");
	    return null;
	}
	else if( agentList == null ) {
		 
	    System.out.println("PCEQAbsGame->generateOtherJntActions: Null List");
	    return null;
	}
	else if( agentList.size() <= 1 ) {
	    
	    System.out.println("PCEQAbsGame->generateOtherJntActions: List Size Wrong");
	    return null;
	}
	
    	ArrayList<GameAction> retJntActions = new ArrayList<GameAction>();
    	
    	//agents' actions for iteration
    	int agentNum = agentList.size();
    	int[] actionIterator = new int[agentNum-1];
    	for( int agent = 0; agent < agentNum-1; agent++ )
    	    actionIterator[agent] = 0;
    	
    	
    	while( true ) {
    	    
    	    GameAction gameAction = new GameAction();
    	    
    	    /**
    	     * generate one joint action
    	     */
    	    int iteratorIndex = 0;
    	    for( int agentListIndex = 0; agentListIndex < agentList.size(); agentListIndex++ ) {
    		
    		int agIndex = agentList.get( agentListIndex );
    		
    		if( agIndex == agentIndex )
    		    continue;
    		else {
    		    
    		    gameAction.setAction( agIndex, actionIterator[iteratorIndex]);
    		    iteratorIndex++;
    		}
    	    }
    	    if( !retJntActions.contains( gameAction ) )
    		retJntActions.add( gameAction );
    	    else
    		gameAction = null;
    	    
    	    /**
    	     * move to the next action
    	     */
    	    for( int agent = agentNum-2; agent >= 0; agent-- ) {
    		
    		actionIterator[agent] += 1;
		if( agent > 0 && actionIterator[agent] >= GameAction.NUM_ACTIONS ) {
		    
		    actionIterator[agent] = 0;
		}
		else
		    break;
    	    }
    	    
    	    /**
    	     * check the stop condition
    	     */
	    if( actionIterator[0] >= GameAction.NUM_ACTIONS ) {
		
		break;
	    }
    	}
    	
    	return retJntActions;
    }
    
    /**
     * get the variable name of a joint action 
     * according to the involved agents
     */
    private String getVariableName( GameAction gameAction, ArrayList<Integer> agentList )
    {
	
	ArrayList<GameAction> subJntActionList = generateJointActions( agentList );
	
	for( int actionIndex = 0; actionIndex < subJntActionList.size(); actionIndex++ ) {
	    
	    GameAction jntAction = subJntActionList.get( actionIndex );
	    
	    /**
	     * only check the actions of the involved agents
	     */
	    boolean bEqual = true;
	    for( int agentListIndex = 0; agentListIndex < agentList.size(); agentListIndex++ ) {
		
		int agentIndex = agentList.get( agentListIndex );
		
		if( jntAction.getAction( agentIndex ) != 
			gameAction.getAction( agentIndex ) ) {
		    
		    bEqual = false;
		    break;
		}
	    }
	    
	    if( bEqual ) {
		
		String retStr = ""+actionIndex;
		return retStr;
	    }
	}
	
	/**
	 * return an empty string if we cannot find
	 */
	String emptyStr = "";
	return emptyStr;
    }

    /**
     * get the max action according to an agent's own table
     */
    protected int getMaxAction( int agent, int locState ) 
    {
	
	if( agent < 0 || agent >= SparseGridWorld.NUM_AGENTS ) {
	    
	    System.out.println("@PCEQAbsGame->getMaxAction: Wrong Agent Index!");
	    return -1;
	}
	if( locState < 0 || 
		locState >= SparseGridWorld.NUM_CELLS ) {
	    
	    System.out.println("@PCEQAbsGame->getMaxAction: Wrong Local State!");
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
    
    protected double getMaxQvalue( int agent, int locState )
    {
	if( agent < 0 || agent >= SparseGridWorld.NUM_AGENTS ) {
	    
	    System.out.println("@PCEQAbsGame->getMaxQvalue: Wrong Agent Index!");
	    return -1;
	}
	if( locState < 0 || 
		locState >= SparseGridWorld.NUM_CELLS ) {
	    
	    System.out.println("@PCEQAbsGame->getMaxQvalue: Wrong Local State!");
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
    
    protected GameAction getJointAction_CE( double[] correlEquil, ArrayList<Integer> involvedAgents )
    {
	GameAction retAction = null;
	if( correlEquil == null || 
		involvedAgents == null ) {
	    
	    System.out.println("PCEQAbsGame->getJointAction_CE: Null Parameters");
	}
	else if( involvedAgents.size() <= 1 ) {
	    
	    System.out.println("PCEQAbsGame->getJointAction_CE: Only one agent involved");
	}
	
	ArrayList<GameAction> subJointActions = generateJointActions( involvedAgents );
	
	/**
	 * the size of this list and the correlated equilibrium should be the same
	 */
	if( correlEquil.length != subJointActions.size() ) {
	    
	    System.out.println("Very Strange!");
	}
	else {
	    
	    retAction = new GameAction();
		
	    double proSum = 0.0;
	    double samplePro = random.nextDouble();
		
	    for( int actionIndex = 0; actionIndex < subJointActions.size(); actionIndex++ ) {
		    
		proSum += correlEquil[actionIndex];
		    
		if( samplePro <= proSum ) {
			
		    GameAction jntAction = subJointActions.get( actionIndex );
		    for( int agentListIndex = 0; agentListIndex < involvedAgents.size(); agentListIndex++ ) {
			    
			int agentIndex = involvedAgents.get( agentListIndex );
			retAction.setAction( agentIndex, jntAction.getAction(agentIndex) );
		    }
		    break;
		}
	    }
	}

	if( retAction == null ) {
	    
	    retAction = new GameAction();
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		
		retAction.setAction( agent, random.nextInt(GameAction.NUM_ACTIONS) );
	    }
	}
	
	return retAction;
    }
    
    
    protected double[] getCEQValues( GameState gameState, double[] correlatedE )
    {
	
	/**
	 * the CE value of an unrelated agent is 0
	 */
	double[] values = new double[SparseGridWorld.NUM_AGENTS];
	for( int i = 0; i < SparseGridWorld.NUM_AGENTS; i++ )
	    values[i] = 0.0;
	
	/**
	 * if null equilibrium, return the value of random policy
	 * cannot return a value of 0
	 */
	if( correlatedE == null ) {
	    
	    return values;
	}
	
	ArrayList<Integer> involvedAgents = getInvolveAgents( gameState );
	ArrayList<GameAction> subJointActions = generateJointActions( involvedAgents );
	for( int actionIndex = 0; actionIndex < subJointActions.size(); actionIndex++ ) {
	    
	    /**
	     * if the probability of this joint action is zero 
	     * then move to the next joint action
	     */
	    if( correlatedE[actionIndex] < 0.00000001 )
		continue;
	    	    
	    GameAction jntAction = subJointActions.get( actionIndex );
	    
	    /**
	     * set all other unrelated agents' action to 0!!!
	     */
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		
		if( !isRelated( gameState, agent) )
		    jntAction.setAction( agent, 0 );
	    }
	    
	    for( int agentListIndex = 0; agentListIndex < involvedAgents.size(); agentListIndex++ ) {
		
		int agent = involvedAgents.get( agentListIndex );
		double Q_sa = getQValue( agent, gameState, jntAction );
		
		values[agent] += correlatedE[actionIndex] * Q_sa;
		
	    }
	}
	
	return values;
    }

}
