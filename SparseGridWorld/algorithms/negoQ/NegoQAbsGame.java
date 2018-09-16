package algorithms.negoQ;

import gameGridWorld.GameAction;
import gameGridWorld.GameState;
import gameGridWorld.SparseGridWorld;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import drasys.or.matrix.VectorI;
import drasys.or.mp.Constraint;
import drasys.or.mp.Problem;
import drasys.or.mp.SizableProblemI;
import drasys.or.mp.lp.DenseSimplex;
import drasys.or.mp.lp.LinearProgrammingI;

public class NegoQAbsGame extends NegoQ
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
    
    public NegoQAbsGame( int agIndex )
    {
	super( agIndex );
	
	int agentNum = SparseGridWorld.NUM_AGENTS;
	int locNum = SparseGridWorld.NUM_CELLS;
	int actionNum = GameAction.NUM_ACTIONS;
	
	transCndValues = new double[agentNum];
	transCndValues[0] = 6.0;//2.0;
	transCndValues[1] = 6.0;//2.0;
	
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
    
    public NegoQAbsGame( int agIndex, double alpha, double gamma, double epsilon )
    {
	super( agIndex, alpha, gamma, epsilon);
	
	int agentNum = SparseGridWorld.NUM_AGENTS;
	int locNum = SparseGridWorld.NUM_CELLS;
	int actionNum = GameAction.NUM_ACTIONS;
	
	transCndValues = new double[agentNum];
	transCndValues[0] = 11;//12;//7.0;//2.0;
	transCndValues[1] = 11;//11;//9.0;//2.0;
	
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
	    
	    System.out.println("ECEQAbsGame->isRelated: Wrong Parameters");
	    return false;
	}
	
	if( relatedMap.containsKey( gameState ) ) {
	    
	    boolean[] relArray = relatedMap.get( gameState );
	    return relArray[agent];
	}
	else {
	    
	    System.out.println("ECEQAbsGame->isRelated: No such State Key");
	    
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
    public void updateQ_NegoQ( GameState curState, GameAction jointAction, 
	    double[] rewards, GameState nextState, GameAction nextEquilAction )
    {
	
	if( nextState == null ) {
	    
	    System.out.println("@NegoQAbsGame->updateQ: NULL nextState!");
	}
	else if( !bLearning ) {
	    
	    /**
	     * choose a random action
	     */
	    GameAction nextAction = new GameAction();
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		
		nextAction.setAction( agent, random.nextInt(GameAction.NUM_ACTIONS) );
	    }
	    
	}
	else {


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
		GameAction updatedCurAction = new GameAction();
		GameAction updatedNextAction = new GameAction();
		for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		   
		    if( isRelated( curState, agent ) ) 
			updatedCurAction.setAction( agent, jointAction.getAction(agent) );
		    else 		
			updatedCurAction.setAction( agent, 0 );
			

		    if( isRelated( nextState, agent ) ) 
			updatedNextAction.setAction( agent, nextEquilAction.getAction(agent) );
		    else
			updatedNextAction.setAction( agent, 0 );
		}
		
		/**
		 * mark a visit
		 */
		visit( curState, updatedCurAction );
		

		
		//double alpha = getVariableAlpha( curState, updatedCurAction );
		
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
			
			double Qsa = getQValue( agent, curState, updatedCurAction );
			
			if( isRelated( nextState, agent ) ) {
			    
			    double equilValue = getQValue( agentIndex, nextState, updatedNextAction );
			    Qsa = (1 - ALPHA) * Qsa + ALPHA * (rewards[agent] + GAMMA * equilValue );
			}
			else {
			    
			    double qmax = getMaxQvalue( agent, nextLocState );
			    Qsa = (1 - ALPHA) * Qsa + ALPHA * (rewards[agent] + GAMMA * qmax );
			}
			
			setQValue( agent, curState, updatedCurAction, Qsa );
		    }
		    else {
			
			double qsa = locQs[agent][curLocState][curAction];
			
			/**
			 * use the next correlated equilibirum value to 
			 * back up the current local Q-value
			 */
			if( isRelated( nextState, agent ) ) {
			    
			    double equilValue = getQValue( agentIndex, nextState, updatedNextAction );
			    qsa = (1 - ALPHA) * qsa + ALPHA * (rewards[agent] + GAMMA * equilValue );
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
		
	    }
	}
    }
    
    
    public static GameAction negotiation( NegoQAbsGame agent_i, NegoQAbsGame agent_j, 
	    GameState gameState )
    {
	
	if( gameState == null || agent_i == null || 
		agent_j == null ) {
	    
	    System.out.println("@NegoQTransModel->negotiation: NULL Parameters!");
	    return null;
	}
	
	
	if( !agent_i.isLearning() ||  !agent_j.isLearning() ) {
	    
	    Random rnd = new Random();
	    GameAction retAction = new GameAction();
	    retAction.setAction( agent_i.getAgentIndex(), rnd.nextInt(GameAction.NUM_ACTIONS));
	    retAction.setAction( agent_j.getAgentIndex(), rnd.nextInt(GameAction.NUM_ACTIONS));
	    
	    return retAction;
	}
	
	
	if( !agent_i.isRelated( gameState, agent_i.getAgentIndex() ) || 
		!agent_j.isRelated( gameState, agent_j.getAgentIndex() ) ) {
	  
	    GameAction retAction = new GameAction();
	    
	    int agentIndex_i = agent_i.getAgentIndex();
	    int agentIndex_j = agent_j.getAgentIndex();
	    int maxAction_i = agent_i.getMaxAction( agentIndex_i, 
		    gameState.getLocationID(agentIndex_i));
	    int maxAction_j = agent_j.getMaxAction( agentIndex_j, 
		    gameState.getLocationID(agentIndex_j));
	    
	    retAction.setAction( agentIndex_i, maxAction_i );
	    retAction.setAction( agentIndex_j, maxAction_j );
	    
	    return retAction;
	}
	
	//no need to modify since this is 2-agent task
	/**
	 * negotiation for pure strategy Nash equilibria
	 */
	ArrayList<GameAction> maxSet_i = agent_i.getMaxSet(gameState);
	ArrayList<GameAction> maxSet_j = agent_j.getMaxSet(gameState);
	agent_i.findNEs(maxSet_i, maxSet_j);
	agent_j.findNEs(maxSet_i, maxSet_j);
	
	
	/**
	 * if there exist Nash equilibira 
	 * then find NSEDAs,
	 * or find meta equilibria
	 */
	if( agent_i.existsNE() ) {
	    
	    
	    ArrayList<GameAction> partDmSet_i = agent_i.getPartiallyDominatingSet(gameState);
	    ArrayList<GameAction> partDmSet_j = agent_j.getPartiallyDominatingSet(gameState);
	    agent_i.findNSEDAs(partDmSet_i, partDmSet_j);
	    agent_j.findNSEDAs(partDmSet_i, partDmSet_j);
	    
	    /**
	     * find EDAs if needed
	     */
	    
	}
	else {
	    
	    /**
	     * for 2-agent grid-world game
	     * we first find symmetric meta equilibria
	     */	    
	    ArrayList<GameAction> possSymmSet_i = agent_i.getPossibleSymmEquilSet(gameState);
	    ArrayList<GameAction> possSymmSet_j = agent_j.getPossibleSymmEquilSet(gameState);
	    agent_i.findSymmEquils(possSymmSet_i, possSymmSet_j);
	    agent_j.findSymmEquils(possSymmSet_i, possSymmSet_j);
	    
	    if( !agent_i.existsSymmMetaEquil() ) {
		
		/**
		 * choose one complete game first
		 */
		ArrayList<String> indices = new ArrayList<String>();
		indices.add("0"); indices.add("1");
		String[] prefix = new String[SparseGridWorld.NUM_AGENTS];
		Random rnd = new Random();
		for( int index = 0; index < SparseGridWorld.NUM_AGENTS; index++ ) {
			
		    prefix[index] = indices.remove(rnd.nextInt(indices.size()));
		}
		    
		/**
		 * then find the set of actions which may be a meta equilibrium 
		 * and find the intersection
		 */
		ArrayList<GameAction> possMetaSet_i = agent_i.getPossibleMetaEquil(gameState, prefix);
		ArrayList<GameAction> possMetaSet_j = agent_j.getPossibleMetaEquil(gameState, prefix);
		agent_i.findMetaEquils(possMetaSet_i, possMetaSet_j);
		agent_j.findMetaEquils(possMetaSet_i, possMetaSet_j);
	    }
	}
	
	/**
	 * then choose one optimal action
	 */
	GameAction[] favorActions = new GameAction[SparseGridWorld.NUM_AGENTS];
	favorActions[agent_i.getAgentIndex()] = agent_i.myFavoriteAction(gameState);
	favorActions[agent_j.getAgentIndex()] = agent_j.myFavoriteAction(gameState);
	
	Random rnd = new Random(); 
	GameAction selectedAction = favorActions[rnd.nextInt(SparseGridWorld.NUM_AGENTS)];
	
	return selectedAction;
    }
    
    
    
    private ArrayList<Integer> getInvolveAgents( GameState gameState )
    {
	if( gameState == null ) {
	    
	    System.out.println("ECEQAbsGame->getInvolveAgents: Null State");
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
     * generate the joint action list of the specified agents
     * @param agentList
     * @return
     */
    private ArrayList<GameAction> generateJointActions( ArrayList<Integer> agentList )
    {
	if( agentList == null ) {
	 
	    System.out.println("ECEQAbsGame->generateJointActions: Null List");
	    return null;
	}
	else if( agentList.size() <= 1 ) {
	    
	    System.out.println("ECEQAbsGame->generateJointActions: List Size Wrong");
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
	    
	    System.out.println("ECEQAbsGame->generateOtherJntActions: Wrong Agent Index");
	    return null;
	}
	else if( agentList == null ) {
		 
	    System.out.println("ECEQAbsGame->generateOtherJntActions: Null List");
	    return null;
	}
	else if( agentList.size() <= 1 ) {
	    
	    System.out.println("ECEQAbsGame->generateOtherJntActions: List Size Wrong");
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
    private int getMaxAction( int agent, int locState ) 
    {
	
	if( agent < 0 || agent >= SparseGridWorld.NUM_AGENTS ) {
	    
	    System.out.println("@ECEQAbsGame->getMaxAction: Wrong Agent Index!");
	    return -1;
	}
	if( locState < 0 || 
		locState >= SparseGridWorld.NUM_CELLS ) {
	    
	    System.out.println("@ECEQAbsGame->getMaxAction: Wrong Local State!");
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
	    
	    System.out.println("@ECEQAbsGame->getMaxQvalue: Wrong Agent Index!");
	    return -1;
	}
	if( locState < 0 || 
		locState >= SparseGridWorld.NUM_CELLS ) {
	    
	    System.out.println("@ECEQAbsGame->getMaxQvalue: Wrong Local State!");
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
    

    public boolean isLearning()
    {
	return bLearning;
    }
    
}
