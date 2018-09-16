package algorithms;

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
	transCndValues[0] = 2.0;
	transCndValues[1] = 2.0;
	transCndValues[2] = 2.0;
	
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
	transCndValues[0] = 10;//12;//12;//1.5;//5;
	transCndValues[1] = 10;//12;//3.0;//3.0;//5;
	transCndValues[2] = 10;//12;//3.0;//3.0;//5;
	transCndValues[3] = 9;
		
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
	if( bLearning ) {

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
		int curLocState = curState.getLocationID( agentIndex );
		int curAction = jointAction.getAction( agentIndex );
		int nextLocState = nextState.getLocationID( agentIndex );
		    
		if( isRelated( curState, agentIndex ) ) {
			
		    double Qsa = getQValue( agentIndex, curState, updatedCurAction );
			
		    if( isRelated( nextState, agentIndex ) ) {
			    
			double equilValue = getQValue( agentIndex, nextState, updatedNextAction );
			Qsa = (1 - ALPHA) * Qsa + ALPHA * (rewards[agentIndex] + GAMMA * equilValue );
		    }
		    else {
			    
			double qmax = getMaxQvalue( agentIndex, nextLocState );
			Qsa = (1 - ALPHA) * Qsa + ALPHA * (rewards[agentIndex] + GAMMA * qmax );
		    }
			
		    setQValue( agentIndex, curState, updatedCurAction, Qsa );
		}
		else {
			
		    double qsa = locQs[agentIndex][curLocState][curAction];
		    
		    /**
		     * use the next correlated equilibirum value to 
		     * back up the current local Q-value
		     */
		    if( isRelated( nextState, agentIndex ) ) {
			    
			double equilValue = getQValue( agentIndex, nextState, updatedNextAction );
			qsa = (1 - ALPHA) * qsa + ALPHA * (rewards[agentIndex] + GAMMA * equilValue );
		    }
		    /**
		     * use the next local Q-value to back up the current local Q-value
		     */
		    else {
			    
			double qmax = getMaxQvalue( agentIndex, nextLocState );
			qsa = (1 - ALPHA) * qsa + ALPHA * (rewards[agentIndex] + GAMMA * qmax );
		    }	
			
		    locQs[agentIndex][curLocState][curAction] = qsa;
		}
		
		//Alpha *= 0.99988;
		ALPHA *= 0.99985;//0.9991;//985;//988;//58;//0.9975;//0.99958;
		
	    }
	}
    }
    
    
    public static GameAction negotiation( ArrayList<NegoQAbsGame> agents, GameState gameState, 
	    boolean absGame )
    {
	if( gameState == null || agents == null ) {
	    
	    System.out.println("@NegoQ->negotiation: NULL Parameters!");
	    return null;
	}
	
	
	if( !agents.get(0).isLearning() ) {
	    
	    /**
	     * choose a random action
	     */
	    GameAction nextAction = new GameAction();
	    Random rnd = new Random();
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		
		nextAction.setAction( agent, rnd.nextInt(GameAction.NUM_ACTIONS) );
	    }
	    
	    rnd = null;
	    return nextAction;
	}
	
	//abstract the game
	/**
	 * not every agent are involved in the game 
	 */
	ArrayList<Integer> involvedAgents = agents.get(0).getInvolveAgents(gameState);
	int numInvAgents = involvedAgents.size();
	
	if( numInvAgents == 0 ) {
	    
	    GameAction retAction = new GameAction();
	    for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
		
		int locState = gameState.getLocationID( agentIndex );
		int maxAction = agents.get( agentIndex ).getMaxAction( agentIndex, locState );
		retAction.setAction( agentIndex, maxAction );
	    }
	    return retAction;
	}
	
	
	/**
	 * negotiation for pure strategy Nash equilibria
	 */
	ArrayList<GameAction>[] maxSets = new ArrayList[numInvAgents];
	for( int agentListIndex = 0; agentListIndex < numInvAgents; agentListIndex++ ) {
	    
	    int agentIndex = involvedAgents.get( agentListIndex );
	    maxSets[agentListIndex] = agents.get( agentIndex ).getMaxSet( gameState, involvedAgents );
	}
	
	/**
	 * we can let only one agent compute NE
	 * and then tell the others
	 */
	ArrayList<GameAction> neActions = findNEs( maxSets, involvedAgents );

	if( neActions != null ) {
	    
	    for( int agentListIndex = 0; agentListIndex < numInvAgents; agentListIndex++ ) {
		    
		int agentIndex = involvedAgents.get( agentListIndex );
		agents.get( agentIndex ).setNEs(neActions);
	    }
	}
	
	/**
	 * if there exist Nash equilibira 
	 * then find NSEDAs,
	 * or find meta equilibria
	 */
	if( neActions.size() > 0 ) {
	    
	    ArrayList<GameAction>[] partDmSets = new ArrayList[numInvAgents];
	    for( int agentListIndex = 0; agentListIndex < numInvAgents; agentListIndex++ ) {
		
		int agentIndex = involvedAgents.get( agentListIndex );
		partDmSets[agentListIndex] = agents.get( agentIndex ).
			getPartiallyDominatingSet( gameState, involvedAgents );
	    }
	    ArrayList<GameAction> nsedaActions = findNSEDAs( partDmSets, involvedAgents );
	    if( nsedaActions != null ) {
		
		for( int agentListIndex = 0; agentListIndex < numInvAgents; agentListIndex++ ) {
			    
		    int agentIndex = involvedAgents.get( agentListIndex );
		    agents.get( agentIndex ).setNSEDAs(nsedaActions);
		}
	    }
	}
	else {
	    
	    /**
	     * for 2-agent grid-world game
	     * we first find symmetric meta equilibria
	     */
	    ArrayList<GameAction>[] possSymmSets = new ArrayList[numInvAgents];
	    for( int agentListIndex = 0; agentListIndex < numInvAgents; agentListIndex++ ) {
	    
		int agentIndex = involvedAgents.get( agentListIndex );
		possSymmSets[agentListIndex] = agents.get(agentIndex).
			getPossibleSymmEquilSet( gameState, involvedAgents );
	    }
	    
	    ArrayList<GameAction> symmEqActions = findSymmEquils( possSymmSets, involvedAgents );
	    if( symmEqActions != null ) {
		
		for( int agentListIndex = 0; agentListIndex < numInvAgents; agentListIndex++ ) {
		    
		    int agentIndex = involvedAgents.get( agentListIndex );
		    agents.get( agentIndex ).setSymmEqs(symmEqActions);
		}
	    }
	    
	    
	    /**
	     * find a meta equilibrium
	     */
	    if( symmEqActions.size() == 0 ) {
		
		/**
		 * we only find meta equilibrium from complete games
		 */
		ArrayList<String> indices = new ArrayList<String>();
		for( int agentListIndex = 0; agentListIndex < numInvAgents; agentListIndex++ ) {
			    
		    int agentIndex = involvedAgents.get( agentListIndex );
		    indices.add(String.valueOf(agentIndex));
		}
		String[] prefix = new String[numInvAgents];
		Random rnd = new Random();
		for( int index = 0; index < numInvAgents; index++ ) {
			    
		    prefix[index] = indices.remove( rnd.nextInt(indices.size()) );
		}
		    
		/**
		 * then find the set of actions which may be a meta equilibrium 
		 * and find the intersection
		 */
		ArrayList<GameAction>[] possMetaSets = new ArrayList[numInvAgents];
		for( int agentListIndex = 0; agentListIndex < numInvAgents; agentListIndex++ ) {
			
		    int agentIndex = involvedAgents.get( agentListIndex );
		    possMetaSets[agentListIndex] = agents.get( agentIndex ).
			    getPossibleMetaEquil(gameState, prefix, involvedAgents);
		}
		ArrayList<GameAction> metaActions = findMetaEquils(possMetaSets, involvedAgents);
		if( metaActions != null ) {
		  
		    for( int agentListIndex = 0; agentListIndex < numInvAgents; agentListIndex++ ) {
			
			int agentIndex = involvedAgents.get( agentListIndex );
			agents.get( agentIndex ).setMetaEqs( metaActions );
		    }
		}
	    }
	}
	
	/**
	 * then choose one optimal action
	 */
	GameAction[] favorActions = new GameAction[numInvAgents];
	for( int agentListIndex = 0; agentListIndex < numInvAgents; agentListIndex++ ) {
	    
	    int agentIndex = involvedAgents.get(agentListIndex );
	    favorActions[agentListIndex] = agents.get( agentIndex ).myFavoriteAction( gameState );
	}
	
	Random rnd = new Random(); 
	GameAction selectedAction = favorActions[rnd.nextInt(numInvAgents)];
	
	/**
	 * for unrelated agents?
	 */
	if( numInvAgents != SparseGridWorld.NUM_AGENTS ) {
	    
	    //System.out.println("Partial Game");
	    
	    for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
		
		if( involvedAgents.contains( agentIndex ) )
		    continue;
		
		int locState = gameState.getLocationID( agentIndex );
		int maxAction = agents.get( agentIndex ).getMaxAction( agentIndex, locState );
		selectedAction.setAction( agentIndex, maxAction );
		
		//System.out.println("I'v got an action");
	    }
	}
	
	return selectedAction;
	
    }
    
    
    
    public static ArrayList<GameAction> findNEs( ArrayList<GameAction>[] maxSets, 
	    ArrayList<Integer> involvedAgents ) 
    {
	if( maxSets == null || involvedAgents == null || 
		involvedAgents.size() < 2 || 
		maxSets.length != involvedAgents.size() ) {
	    
	    System.out.println("@NegoQAbsGame->findNEs: Wrong Parameter");
	    return null;
	}

	ArrayList<GameAction> retList = new ArrayList<GameAction>();
	
	ArrayList<GameAction> maxSet = maxSets[0];
	int numInvAgents = involvedAgents.size();
	
	for( int jntActionIndex = 0; jntActionIndex < maxSet.size(); jntActionIndex++ ) {
	    
	    GameAction jntAction = maxSet.get( jntActionIndex );
	    
	    boolean isNE = true;
	    for( int other = 1; other < numInvAgents; other++ ) {
		
		if( !maxSets[other].contains(jntAction) ) {
		    
		    isNE = false;
		    break;
		}
	    }
	    
	    if( isNE ) {
		
		//set the actions of all unrelated agents to 0???
		
		if( !retList.contains( jntAction ) ) {
		    retList.add( jntAction );
		}
	    }
	}
	
	
	return retList;
    }
    
    
    public static ArrayList<GameAction> findNSEDAs( ArrayList<GameAction>[] partDmSets, 
	    ArrayList<Integer> involvedAgents )
    {
	if( partDmSets == null || involvedAgents == null || 
		involvedAgents.size() < 2 || 
		partDmSets.length != involvedAgents.size() ) {
	    
	    System.out.println("@NegoQAbsGame->findNSEDAs: Wrong Parameter");
	    return null;
	}

	ArrayList<GameAction> retList = new ArrayList<GameAction>();
	
	ArrayList<GameAction> partDmSet = partDmSets[0];
	int numInvAgents = involvedAgents.size();
	
	for( int jntActionIndex = 0; jntActionIndex < partDmSet.size(); jntActionIndex++ ) {
	    
	    GameAction jntAction = partDmSet.get( jntActionIndex );
	    
	    boolean isNSEDA = true;
	    for( int other = 1; other < numInvAgents; other++ ) {
		
		if( !partDmSets[other].contains(jntAction) ) {
		    
		    isNSEDA = false;
		    break;
		}
	    }
	    
	    if( isNSEDA ) {
		
		//set the actions of all unrelated agents to 0???
		
		if( !retList.contains( jntAction ) ) {
		    retList.add( jntAction );
		}
	    }
	}
	
	return retList;
    }
    
    public static ArrayList<GameAction> findSymmEquils( ArrayList<GameAction>[]  possSymmSets, 
	    ArrayList<Integer> involvedAgents )
    {
	if( possSymmSets == null || involvedAgents == null || 
		involvedAgents.size() < 2 || 
		possSymmSets.length != involvedAgents.size() ) {
	    
	    System.out.println("@NegoQAbsGame->findSymmEquils: Wrong Parameter");
	    return null;
	}

	ArrayList<GameAction> retList = new ArrayList<GameAction>();
	
	ArrayList<GameAction> possSymmSet = possSymmSets[0];
	int numInvAgents = involvedAgents.size();
	
	for( int jntActionIndex = 0; jntActionIndex < possSymmSet.size(); jntActionIndex++ ) {
	    
	    GameAction jntAction = possSymmSet.get( jntActionIndex );
	    
	    boolean isSymm = true;
	    for( int other = 1; other < numInvAgents; other++ ) {
		
		if( !possSymmSets[other].contains(jntAction) ) {
		    
		    isSymm = false;
		    break;
		}
	    }
	    
	    if( isSymm ) {
		
		//set the actions of all unrelated agents to 0???
		
		if( !retList.contains( jntAction ) ) {
		    retList.add( jntAction );
		}
	    }
	}
	
	return retList;
    }
    
    
    public static ArrayList<GameAction> findMetaEquils( ArrayList<GameAction>[] possMetaSets, 
	    ArrayList<Integer> involvedAgents )
    {
	if( possMetaSets == null || involvedAgents == null || 
		involvedAgents.size() < 2 || 
		possMetaSets.length != involvedAgents.size() ) {
	    
	    System.out.println("@NegoQAbsGame->findMetaEquils: Wrong Parameter");
	    return null;
	}

	ArrayList<GameAction> retList = new ArrayList<GameAction>();
	
	ArrayList<GameAction> possMetaSet = possMetaSets[0];
	int numInvAgents = involvedAgents.size();
	
	for( int jntActionIndex = 0; jntActionIndex < possMetaSet.size(); jntActionIndex++ ) {
	    
	    GameAction jntAction = possMetaSet.get( jntActionIndex );
	    
	    boolean isMeta = true;
	    for( int other = 1; other < numInvAgents; other++ ) {
		
		if( !possMetaSets[other].contains(jntAction) ) {
		    
		    isMeta = false;
		    break;
		}
	    }
	    
	    if( isMeta ) {
		
		//set the actions of all unrelated agents to 0???
		
		if( !retList.contains( jntAction ) ) {
		    retList.add( jntAction );
		}
	    }
	}
	
	return retList;
    }
    
    /**
     * compute the max set in a state and return 
     * negotiation for pure strategy Nash equilibria
     */
    public ArrayList<GameAction> getMaxSet( GameState gameState, 
	    ArrayList<Integer> involvedAgents )
    {
	if( gameState == null || involvedAgents == null ) {
	    
	    System.out.println("@NegoQAbsGame->getMaxSet: NULL gameState!");
	    return null;
	}
	else if( !involvedAgents.contains( agentIndex ) ) {
	    
	    System.out.println("NegoQAbsGame->getMaxSet: Not Involved");
	    return null;
	}
	
	int numInvAgents = involvedAgents.size();
	ArrayList<GameAction> retList = new ArrayList<GameAction>();
	
	/**
	 * generate the joint actions of the other involved agents
	 */
	ArrayList<Integer> otherAgents = new ArrayList<Integer>();
	otherAgents.addAll( involvedAgents );
	for( int agentListIndex = 0; agentListIndex < numInvAgents; agentListIndex++ ) {
	    
	    int agIndex = otherAgents.get( agentListIndex );
	    if( agIndex == agentIndex ) {
		
		otherAgents.remove( agentListIndex );
		break;
	    }
	}
	ArrayList<GameAction> othJntActions = generateJointActions( otherAgents );
	
	/**
	 * find max action according to the current Q-table
	 */
	for( int listIndex = 0; listIndex < othJntActions.size(); listIndex++ ) {
	    
	    GameAction jntAction = othJntActions.get( listIndex );
	    
	    jntAction.setAction( agentIndex, 0);
	    double maxValue = getQValue( agentIndex, gameState, jntAction );
	    double maxAct = 0;
	    
	    //find the max value
	    for( int act = 1; act < GameAction.NUM_ACTIONS; act++ ) {
		
		jntAction.setAction( agentIndex, act );
		double value = getQValue( agentIndex, gameState, jntAction );
		
		if( value > maxValue ) {
		    
		    maxValue = value;
		    maxAct = act;
		}
	    }
	    
	    //find all max actions
	    for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
		
		GameAction gameAction = new GameAction();
		for( int agIndex = 0; agIndex < SparseGridWorld.NUM_AGENTS; agIndex++ ) {
		    
		    if( agIndex == agentIndex )
			gameAction.setAction( agIndex, act );
		    else if( involvedAgents.contains( agIndex ) )
			gameAction.setAction( agIndex, jntAction.getAction(agIndex) );
		    else
			gameAction.setAction( agIndex, 0 );
		}
		
		double value = getQValue( agentIndex, gameState, gameAction );
		if( Math.abs(value-maxValue) < 0.00001 ) {
		    
		    if( !retList.contains( gameAction ) )
			retList.add( gameAction );
		}
	    }
	}
	
	return retList;
    }
    
    
    /**
     * compute the partially dominating set for finding NSEDAs
     */
    public ArrayList<GameAction> getPartiallyDominatingSet( GameState gameState,
	    ArrayList<Integer> involvedAgents )
    {
	if( gameState == null || 
		involvedAgents == null ) {
	    
	    System.out.println("@NegoQAbsGame->getPartiallyDominatingSet: NULL gameState!");
	    return null;
	}
	else if( !involvedAgents.contains( agentIndex ) ) {
	    
	    System.out.println("NegoQAbsGame->getPartiallyDominatingSet: Not Involved");
	    return null;
	}
	
	ArrayList<GameAction> retList = new ArrayList<GameAction>();
	
	/**
	 * no nash equilibria
	 */
	if( nashEquilActions.size() == 0 ) {
	    
	    System.out.println("@NegoQAbsGame->getPartiallyDominatingSet: No Nash Equilibria!");
	    return retList;
	}
	
	ArrayList<GameAction> allJntActions = generateJointActions( involvedAgents );
	for( int listIndex = 0; listIndex < allJntActions.size(); listIndex++ ) {
	    
	    GameAction jntAction = allJntActions.get( listIndex );
	    
	    if( nashEquilActions.contains( jntAction ) )
		continue;
	    
	    double jntActionValue = getQValue( agentIndex, gameState, jntAction );
	    for( int neIndex = 0; neIndex < nashEquilActions.size(); neIndex++ ) {
			
		GameAction neAction = nashEquilActions.get(neIndex);
		double neValue = getQValue( agentIndex, gameState, neAction );
			
		if( jntActionValue >= neValue ) {
			    
		    if( !retList.contains( jntAction ) )
			retList.add( jntAction );
			    
		    break;
		}
	    }
	    
	}
	
	return retList;
    }
    
    
    /**
     * compute the set of possible meta equilibria 
     * according to this agent's Q-table
     */
    public ArrayList<GameAction> getPossibleMetaEquil( GameState gameState, String[] prefix, 
	    ArrayList<Integer> involvedAgents )
    {
	if( gameState == null || prefix == null || 
		involvedAgents == null ) {
	    
	    System.out.println("@NegoQAbsGame->getPossibleMetaEquil: NULL Parameters!");
	    return null;
	}
	else if( !involvedAgents.contains( agentIndex ) ) {
	    
	    System.out.println("NegoQAbsGame->getPossibleMetaEquil: Not Involved");
	    return null;
	}
	else if( prefix.length != involvedAgents.size() ) {
	    
	    System.out.println("@NegoQAbsGame->getPossibleMetaEquil: Wrong Prefix!");
	    return null;
	}
	
	ArrayList<GameAction> retList = new ArrayList<GameAction>();
	
	
	ArrayList<Integer> leftAgents = new ArrayList<Integer>();
	ArrayList<Integer> rightAgents =  new ArrayList<Integer>();
	for( int stringIndex = 0; stringIndex < prefix.length; stringIndex++ ) {
	    
	    String curString = prefix[stringIndex];
	    leftAgents.add( Integer.parseInt( curString ) );
	}
	
	ArrayList<GameAction> outcomeActions = null;
	while( !leftAgents.isEmpty() ) {
	    
	    int curAgent = leftAgents.remove( 0 );
	    
	    if( curAgent == agentIndex ) {
		
		outcomeActions = findMax( curAgent, outcomeActions, 
			leftAgents, rightAgents, gameState, involvedAgents );
	    }
	    else {
		
		outcomeActions = findMin( curAgent, outcomeActions, 
			leftAgents, rightAgents, gameState, involvedAgents );
	    }
	    
	    rightAgents.add( curAgent );
	}
	
	if( outcomeActions.size() == 1 ) {
	    
	    GameAction threAction = outcomeActions.remove( 0 );
	    double threValue = getQValue( agentIndex, gameState, threAction );
	    
	    /**
	     * not joint actions of all agents 
	     * but of the involved agents
	     */
	    ArrayList<GameAction> allJointActions = generateJointActions( involvedAgents );
	    for( int listIndex = 0; listIndex < allJointActions.size(); listIndex++ ) {
		
		GameAction jntAction = allJointActions.get( listIndex );
		double value = getQValue( agentIndex, gameState, jntAction );
		
		if( value >= threValue ) {
		    
		    if( !retList.contains( jntAction ) ) 
			retList.add( jntAction );
		}
	    }
	}
	else {
	    
	    System.out.println("NegoQAbsGame->getPossibleMetaEquil: Cannot find a threhold value");
	}
	
	return retList;
    }
    
    public ArrayList<GameAction> getPossibleSymmEquilSet( GameState gameState, 
	    ArrayList<Integer> involvedAgents )
    {
	if( gameState == null ) {
	    
	    System.out.println("@NegoQAbsGame->getPossibleSymmEquilSet: NULL Parameters!");
	    return null;
	}
	else if( !involvedAgents.contains( agentIndex ) ) {
	    
	    System.out.println("NegoQAbsGame->getPossibleSymmEquilSet: Not Involved");
	    return null;
	}
	
	int numInvAgents = involvedAgents.size();
	String[] prefix = new String[numInvAgents];
	prefix[0] = String.valueOf(agentIndex);
	int arrayIndex = 1;
	for( int agentListIndex = 0; agentListIndex < numInvAgents; agentListIndex++ ) {
	    
	    int agIndex = involvedAgents.get(agentListIndex);
	    if( agIndex == agentIndex ) 
		continue;
	    
	    prefix[arrayIndex] = String.valueOf( agIndex );
	    arrayIndex++;
	}
	
	
	return getPossibleMetaEquil(gameState, prefix, involvedAgents);
    }
    
    
    //check
    protected ArrayList<GameAction> findMax( int currentAgent, ArrayList<GameAction> gameActions, 
	    ArrayList<Integer> leftAgents, ArrayList<Integer> rightAgents, GameState gameState, 
	    ArrayList<Integer> involvedAgents )
    {
	
	
	ArrayList<GameAction> retList = new ArrayList<GameAction>();
	
	if( leftAgents.size() == 0 ) {
	    
	    double maxValue = Double.NEGATIVE_INFINITY;
	    GameAction maxAction = new GameAction();
	    GameAction curAction = new GameAction();
	    for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
		
		/**
		 * set the action of the current agent 
		 */
		curAction.setAction( currentAgent, act );
		
		/**
		 * set the actions of the right agents
		 * gameActions must not be null!!
		 */
		for( int index = 0; index < gameActions.size(); index++ ) {
			
		    GameAction gameAction = gameActions.get( index );
			
		    //find the joint action that equals the joint action of all non-right agents
		    if( act == gameAction.getAction(currentAgent) ) {
			
			for( int rightAgentListIndex = 0; rightAgentListIndex < rightAgents.size(); rightAgentListIndex++ ) {
				
			    int rightAgentIndex = rightAgents.get( rightAgentListIndex );
			    curAction.setAction( rightAgentIndex, gameAction.getAction(rightAgentIndex) );
			}
			    
			break;
		    }
		}
		    
		/**
		 * get the value of the action
		 */
		double value = getQValue( agentIndex, gameState, curAction );
		if( value > maxValue ) {
		    
		    maxValue = value;
		    for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
			
			if( involvedAgents.contains( agentIndex ) )
			    maxAction.setAction( agentIndex, curAction.getAction(agentIndex) );
			else
			    maxAction.setAction( agentIndex, 0 );
		    }
		}
	    }
	    
	    retList.add( maxAction );
	    return retList;
	}
	
	/**
	 * the joint action list must contain all joint actions 
	 * of the left agents!!!
	 */
	ArrayList<GameAction> othJntActions = generateJointActions( leftAgents );
	
	for( int listIndex = 0; listIndex < othJntActions.size(); listIndex++ ) {
	    
	    GameAction curAction = othJntActions.get( listIndex );
	    

	    double maxValue = Double.NEGATIVE_INFINITY;
	    int maxAct = -1;
	    GameAction maxAction = new GameAction();
	    for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
		
		/**
		 * set the action of the current agent 
		 */
		curAction.setAction( currentAgent, act );
		
		/**
		 * set the actions of the right agents
		 */
		if( gameActions != null ) {
		    
		    for( int index = 0; index < gameActions.size(); index++ ) {
			
			GameAction gameAction = gameActions.get( index );
			
			//find the joint action that equals the joint action of all non-right agents
			if( act != gameAction.getAction(currentAgent) )
			    continue;
			boolean equal = true;
			for( int leftAgentListIndex = 0; leftAgentListIndex < leftAgents.size(); leftAgentListIndex++ ) {
			   
			    int leftAgentIndex = leftAgents.get( leftAgentListIndex );
			    if( curAction.getAction(leftAgentIndex) != gameAction.getAction( leftAgentIndex ) ) {
				
				equal = false;
				break;
			    }
			}
			
			//set the right actions 
			if( equal ) {
			    
			    for( int rightAgentListIndex = 0; rightAgentListIndex < rightAgents.size(); rightAgentListIndex++ ) {
				
				int rightAgentIndex = rightAgents.get( rightAgentListIndex );
				curAction.setAction( rightAgentIndex, gameAction.getAction(rightAgentIndex) );
			    }
			    
			    break;
			}
		    }
		}
		
		/**
		 * get the value of the action
		 */
		double value = getQValue( agentIndex, gameState, curAction );
		if( value > maxValue ) {
		    
		    maxValue = value;
		    for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
			
			if( involvedAgents.contains( agentIndex ) )
			    maxAction.setAction( agentIndex, curAction.getAction(agentIndex) );
			else
			    maxAction.setAction( agentIndex, 0 );
		    }
		}
	    }
	    
	    //add this max action to the ret list
	    retList.add( maxAction );
	}
	
	return retList;
    }
    
    //check
    protected ArrayList<GameAction> findMin( int currentAgent, ArrayList<GameAction> gameActions, 
	    ArrayList<Integer> leftAgents, ArrayList<Integer> rightAgents, GameState gameState, 
	    ArrayList<Integer> involvedAgents )
    {
	
	ArrayList<GameAction> retList = new ArrayList<GameAction>();
	
	if( leftAgents.size() == 0 ) {
	    
	    double minValue = Double.POSITIVE_INFINITY;
	    GameAction minAction = new GameAction();
	    GameAction curAction = new GameAction();
	    for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
		
		/**
		 * set the action of the current agent 
		 */
		curAction.setAction( currentAgent, act );
		
		/**
		 * set the actions of the right agents
		 * gameActions must not be null!!
		 */
		for( int index = 0; index < gameActions.size(); index++ ) {
			
		    GameAction gameAction = gameActions.get( index );
			
		    //find the joint action that equals the joint action of all non-right agents
		    if( act == gameAction.getAction(currentAgent) ) {
			
			for( int rightAgentListIndex = 0; rightAgentListIndex < rightAgents.size(); rightAgentListIndex++ ) {
				
			    int rightAgentIndex = rightAgents.get( rightAgentListIndex );
			    curAction.setAction( rightAgentIndex, gameAction.getAction(rightAgentIndex) );
			}
			    
			break;
		    }
		}
		    
		/**
		 * get the value of the action
		 */
		double value = getQValue( agentIndex, gameState, curAction );
		if( value < minValue ) {
		    
		    minValue = value;
		    for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
			
			if( involvedAgents.contains( agentIndex ) )
			    minAction.setAction( agentIndex, curAction.getAction(agentIndex) );
			else
			    minAction.setAction( agentIndex, 0 );
		    }
		}
	    }
	    
	    retList.add( minAction );
	    return retList;
	}
	
	
	/**
	 * the joint action list must contain all joint actions 
	 * of the left agents!!!
	 */
	ArrayList<GameAction> othJntActions = generateJointActions( leftAgents );
	
	for( int listIndex = 0; listIndex < othJntActions.size(); listIndex++ ) {
	    
	    GameAction curAction = othJntActions.get( listIndex );
	    

	    double minValue = Double.POSITIVE_INFINITY;
	    GameAction minAction = new GameAction();
	    for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
		
		/**
		 * set the action of the current agent 
		 */
		curAction.setAction( currentAgent, act );
		
		/**
		 * set the actions of the right agents
		 */
		if( gameActions != null ) {
		    
		    for( int index = 0; index < gameActions.size(); index++ ) {
			
			GameAction gameAction = gameActions.get( index );
			
			//find the joint action that equals the joint action of all non-right agents
			if( act != gameAction.getAction(currentAgent ) )
			    continue;
			boolean equal = true;
			for( int leftAgentListIndex = 0; leftAgentListIndex < leftAgents.size(); leftAgentListIndex++ ) {
			   
			    int leftAgentIndex = leftAgents.get( leftAgentListIndex );
			    if( curAction.getAction(leftAgentIndex) != gameAction.getAction( leftAgentIndex ) ) {
				
				equal = false;
				break;
			    }
			}
			
			//set the right actions 
			if( equal ) {
			    
			    for( int rightAgentListIndex = 0; rightAgentListIndex < rightAgents.size(); rightAgentListIndex++ ) {
				
				int rightAgentIndex = rightAgents.get( rightAgentListIndex );
				curAction.setAction( rightAgentIndex, gameAction.getAction(rightAgentIndex) );
			    }
			    
			    break;
			}
		    }
		}
		
		/**
		 * get the value of the action
		 */
		double value = getQValue( agentIndex, gameState, curAction );
		if( value < minValue ) {
		    
		    minValue = value;
		    for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
			
			if( involvedAgents.contains( agentIndex ) )
			    minAction.setAction( agentIndex, curAction.getAction(agentIndex) );
			else
			    minAction.setAction( agentIndex, 0 );
		    }
		}
	    }
	    
	    //add this max action to the ret list
	    retList.add( minAction );
	}
	
	return retList;
    }
    
    
    private ArrayList<Integer> getInvolveAgents( GameState gameState )
    {
	if( gameState == null ) {
	    
	    System.out.println("ECEQAbsGame->getInvolveAgents: Null State");
	    return null;
	}
	
	ArrayList<Integer> involvedAgents = new ArrayList<Integer>();
	for( int ag = 0; ag < SparseGridWorld.NUM_AGENTS; ag++ ) {
	    
	    if( isRelated( gameState, ag ) )
		involvedAgents.add( ag );
	}
	
	return involvedAgents;
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
