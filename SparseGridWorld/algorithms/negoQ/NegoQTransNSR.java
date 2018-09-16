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
import java.util.StringTokenizer;
/**
 * NegoQ with N-step Return (NSR) transfer for selective value function transfer
 * @author dearhude1
 *
 */

public class NegoQTransNSR extends NegoQ
{
    
    
    /**
     * N-step Return model for each agent
     * defined over each agents' local state space
     */
    private double[] nsrMeanTrans;
    private double[] nsrVarianceTrans;
    
    
    /**
     * the N-step Return data of this MARL algorithm
     */
    private HashMap<GameState, double[]> dataNSR;
    
    /**
     * the visit number of each joint state
     */
    private HashMap<GameState, Integer> stateVstNum;
    
    /**
     * the N-step Return model in the multi-agent system
     * defined over the joint state space
     * 
     * these two maps only needs to be used once
     */
    //private HashMap<GameState, double[]> nsrMeanModel;
    //private HashMap<GameState, double[]> nsrVarianceModel;

    /**
     * the rewards, joint state sampled during learning,
     * the max length of these lists is N
     */
    private ArrayList<Double> sampledRewards;
    private ArrayList<GameState> sampledGameStates;
    
    /**
     * the maximum number of NSR data point of each state-action pair
     */
    public static final int NSR_MAX_DATA_NUM = 500;
    
    /**
     * the number of episodes for learning N-step Return model
     */
    public static final int NUM_EPISODE_NSR_MODEL_LEARNING = 100;
    
    /**
     * the number of steps to look forward
     */
    private int numNStep = 3;//15;//3;//5;
    
    /**
     * whether the algorithm should learn now
     */
    private boolean bLearning = false;
    
    /**
     * a boolean map which indicates whether 
     * each agents are related in each game state
     * 
     * relatedMap is computed by comparing the single-agent NSR model
     * and the NSR model of the multi-agent system
     */
    private HashMap<GameState, Boolean> relatedMap;
    
    /**
     * local Q-table for value function transfer
     */
    private double[][][] locQs;
    
    public double THRESHOLD_VALUE = 0.1;
    
    public NegoQTransNSR( int agIndex ) 
    {
	super(agIndex);
	
	int numAgents = SparseGridWorld.NUM_AGENTS;
	int numGrids = SparseGridWorld.NUM_CELLS;
	
	nsrMeanTrans = new double[numGrids];
	nsrVarianceTrans = new double[numGrids];
	
	/**
	 * init the hashmap of the NSR data 
	 * and the state visit number
	 */
	dataNSR = new HashMap<GameState, double[]>();
	stateVstNum = new HashMap<GameState, Integer>();
	ArrayList<GameState> allStates = SparseGridWorld.getAllValidStates();
	for( int stateIndex = 0; stateIndex < allStates.size(); stateIndex++ ) {
	    
	    GameState gameState = allStates.get( stateIndex );
	    
	    double[] nsrDataArray = new double[NSR_MAX_DATA_NUM];    
	    for( int index = 0; index < NSR_MAX_DATA_NUM; index++ ) {
		
		nsrDataArray[index] = 0;
	    }
		
	    if( !dataNSR.containsKey( gameState ) ) {
		    
		dataNSR.put( gameState, nsrDataArray );
	    }
	    else
		nsrDataArray = null;
	    
	    if( !stateVstNum.containsKey( gameState ) ) {
		
		stateVstNum.put( gameState, 0 );
	    }
	}
	
	/**
	 * init the list of sampled rewards and sampled states
	 */
	sampledRewards = new ArrayList<Double>();
	sampledGameStates = new ArrayList<GameState>();
	
	/**
	 * transfer the N-step return model 
	 */
	transferNSR();
    }
    
    public NegoQTransNSR( int agIndex, double alpha, double gamma, double epsilon )
    {
	super(agIndex, alpha, gamma, epsilon);
	
	int numAgents = SparseGridWorld.NUM_AGENTS;
	int numGrids = SparseGridWorld.NUM_CELLS;
	
	nsrMeanTrans = new double[numGrids];
	nsrVarianceTrans = new double[numGrids];
	
	/**
	 * init the hashmap of the NSR data 
	 * and the state visit number
	 */
	dataNSR = new HashMap<GameState, double[]>();
	stateVstNum = new HashMap<GameState, Integer>();
	ArrayList<GameState> allStates = SparseGridWorld.getAllValidStates();
	for( int stateIndex = 0; stateIndex < allStates.size(); stateIndex++ ) {
	    
	    GameState gameState = allStates.get( stateIndex );
	    
	    double[] nsrDataArray = new double[NSR_MAX_DATA_NUM];    
	    for( int index = 0; index < NSR_MAX_DATA_NUM; index++ ) {
	    
		nsrDataArray[index] = 0;
	    }
		
	    if( !dataNSR.containsKey( gameState ) ) {
		    
		dataNSR.put( gameState, nsrDataArray );
	    }
	    else
		nsrDataArray = null;
	    
	    if( !stateVstNum.containsKey( gameState ) ) {
		
		stateVstNum.put( gameState, 0 );
	    }
	}
	
	
	/**
	 * init the list of sampled rewards and sampled states
	 */
	sampledRewards = new ArrayList<Double>();
	sampledGameStates = new ArrayList<GameState>();
	
	/**
	 * transfer the N-step return model 
	 */
	transferNSR();
	
    }
    
    
    public static GameAction negotiation( NegoQTransNSR agent_i, 
	    NegoQTransNSR agent_j, GameState gameState )
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
	//if we are exploring the multi-agent system
	else if( !bLearning ) {
	    
	    /**
	     * choose a random action
	     *
	    GameAction nextAction = new GameAction();
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		
		nextAction.setAction( agent, random.nextInt(GameAction.NUM_ACTIONS) );
	    }
	    */
	    
	    /**
	     * record the NSR data
	     */
	    if( curState != null && jointAction != null 
		&& rewards != null ) {
		
		/**
		 * make a visit to the current state
		 */
		int visitNum = stateVstNum.get(curState);
		visitNum += 1;
		stateVstNum.remove(curState);
		stateVstNum.put(curState, visitNum);
		
		/**
		 * directly call a method
		 */
		updateNSRData( rewards );

		
		/**
		 * add the current rewards and game state to 
		 * the sampled list
		 */
		sampledRewards.add(rewards[agentIndex]);
		sampledGameStates.add(curState);
	    }
	}
	//if we are learning the value function
	else {

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
		 * only update the Q-table of this agent
		 */
		double Qsa = getQValue( agentIndex, curState, jointAction );
		double equilValue = getQValue( agentIndex, nextState, nextEquilAction );
			
		//Qsa = ( 1 - ALPHA ) * Qsa + ALPHA * ( rewards[agentIndex] + GAMMA * equilValue );
			
		/**
		 * variable learning rate
		 */
		double alpha = getVariableAlpha( curState, jointAction );
		Qsa = (1 - alpha) * Qsa + alpha * ( rewards[agentIndex] + GAMMA * equilValue );
			
		setQValue( agentIndex, curState, jointAction, Qsa );
			
			
		/**
		 * alpha decay??
		 */
		//ALPHA *= 0.9958;//0.9958;//0.99958;//	
	    }
	}
    }
    
    /**
     * transfer the single-agent NSR model
     */
    private void transferNSR()
    {
	
	/**
	 * read the NSR model from files
	 */
	try {
	
	    String nsrFileName = "./NSR" +"_agent" + agentIndex + ".txt";
	    BufferedReader reader = new BufferedReader(new FileReader(nsrFileName));
			
	    int locState = 0;
	    String line = "";
	    while( (line = reader.readLine()) != null ) {
		    
		if( line.isEmpty() )
		    continue;
		    
		StringTokenizer token = new StringTokenizer(line, ",");
		double nsrMean = Double.parseDouble(token.nextToken());
		double nsrVar = Double.parseDouble(token.nextToken());
		nsrMeanTrans[locState] = nsrMean;
		nsrVarianceTrans[locState] = nsrVar;
		    
		locState++;
		if( locState >= SparseGridWorld.NUM_CELLS ) {
			
		    break;
		}
	    }
	    reader.close();
	}
	catch (Exception e)
	{
	    // TODO: handle exception
	    e.printStackTrace();
	}
    }
    
    
    private void transferValueFunction()
    {
	
	/**
	 * then transfer the value function according to the similarity
	 */
	ArrayList<GameState> allStates = SparseGridWorld.getAllValidStates();
	ArrayList<GameAction> jointActions = SparseGridWorld.getAllJointActions();
	
	int transferCount = 0;
	for( int stateIndex = 0; stateIndex < allStates.size(); stateIndex ++ ) {
		
	    GameState state = allStates.get( stateIndex );
	   
	    int locState = state.getLocationID( agentIndex );
	    /**
	     * if the distance between the same states 
	     * in two different models is small (which means they are similar)
	     * then we transfer the value functions
	     */
	    if( !isRelated( state ) ) {
		    
		transferCount++;
		
		/**
		 * for all joint actions 
		 */
		for( int jntActIndex = 0; jntActIndex < jointActions.size(); jntActIndex++ ) {
		    
		    GameAction jntAction = jointActions.get( jntActIndex );
		    int locAction = jntAction.getAction( agentIndex );
		    double locQValue = locQs[agentIndex][locState][locAction];
			   
		    setQValue( agentIndex, state, jntAction, locQValue );    
		}
	    }
	    else {
		    
		    for( int jntActIndex = 0; jntActIndex < jointActions.size(); jntActIndex++ ) {
			    
			GameAction jntAction = jointActions.get( jntActIndex );
			setQValue( agentIndex, state, jntAction, 0.0 );  //?
		    } 
		}
	}
	
	System.out.println("All States: "+allStates.size());
	System.out.println("TransferCount: "+transferCount);
	

    }
    
    /**
     * according to the NSR model, 
     * determine in each joint state that whether 
     * each agent is related to the other agents
     */
    private void generateRelatedMap()
    {
	relatedMap = new HashMap<GameState, Boolean>();
	
	/**
	 * get all joint states
	 */
	ArrayList<GameState> allStates = SparseGridWorld.getAllValidStates();
	ArrayList<Double> klDisList = new ArrayList<Double>(); 
	double maxKLDis = Double.NEGATIVE_INFINITY;

	
	for( int stateIndex = 0; stateIndex < allStates.size(); stateIndex++ ) {
	    
	    GameState gameState = allStates.get( stateIndex );
	    
	    /**
	     * compute the NSR distribution in this joint state
	     */
	    double[] nsrDistr = computeNSRDistribution(gameState);
	    double averNSR = nsrDistr[0];
	    double varNSR = nsrDistr[1];
		
	    /**
	     * find the NSR distribution of the local state
	     */
	    int localState = gameState.getLocationID(agentIndex);
	    double averNSR_Local = nsrMeanTrans[localState];
	    double varNSR_Local = nsrVarianceTrans[localState];
		
	    /**
	     * compute the KL-distance of the two Gaussian distribution
	     */
	    double klDis = KLDistance(averNSR_Local, varNSR_Local, 
		    averNSR, varNSR);
	    //System.out.println("KL distance "+klDis);
	    
	    klDisList.add(klDis);
	    
	    if( klDis > maxKLDis ) {
		
		maxKLDis = klDis;
	    }
	}
	
	
	System.out.println("Max KL Dis: "+maxKLDis);
	
	for( int stateIndex = 0; stateIndex < allStates.size(); stateIndex++ ) {
	    
	    GameState gameState = allStates.get( stateIndex );
	    double klDis = klDisList.get(stateIndex);
	    
	    boolean related = false;
	    if( klDis > maxKLDis / 10 ) { //100
		    
		related = true;
	    }
	    
	    if( !relatedMap.containsKey(gameState) )
		relatedMap.put( gameState, related );
	}
	
	/**
	 * it seems that we can release the space 
	 * of many hashmaps
	 */
	nsrMeanTrans = null;
	nsrVarianceTrans = null;
	dataNSR.clear();
	dataNSR = null;
	sampledGameStates.clear();
	sampledGameStates = null;
	sampledRewards.clear();
	sampledRewards = null;
    }
    
    /**
     * compute the NSR distribution of an agent 
     * in a given joint state
     */
    private double[] computeNSRDistribution( GameState gameState )
    {

	
	double[] nsrArray = dataNSR.get(gameState);
	int ns = stateVstNum.get(gameState);
	if( ns > NSR_MAX_DATA_NUM )
	    ns = NSR_MAX_DATA_NUM;
	
	
	double averNSR = 0.0;
	double varNSR = 0.0;
	for( int i = 0; i < ns; i++ ) {
		
	    averNSR += nsrArray[i];
	}
	if( ns > 0 )
	    averNSR /= ns;
	for( int i = 0; i < ns; i++ )  {
		
	    double diff = nsrArray[i] - averNSR;
	    varNSR += diff * diff;
	}
	if( ns > 0 )
	    varNSR /= ns;
	
	double[] retArray = new double[2];
	retArray[0] = averNSR;
	retArray[1] = varNSR;
	return retArray;
    }
    
    //should be called by the game executor
    public void currentEpisode( int ep )
    {
	if( bLearning || ep < NUM_EPISODE_NSR_MODEL_LEARNING )
	    return;
	else {
	    
	    bLearning = true;
	    
	    /**
	     * compute the related map 
	     * according to the NSR data 
	     * and the single-agent NSR models 
	     */
	    generateRelatedMap();


	    /**
	     * transfer value function
	     */
	    readLocalQ();
	    transferValueFunction();
	}
    }
    
    
    private void updateNSRData( double[] rewards )
    {
	/**
	 * if N-step has been reached 
	 * record the N-step Return for the game state 
	 * in the first position of the list
	 */
	if( sampledRewards.size() == numNStep && 
		sampledGameStates.size() == numNStep ) {
	    
	    GameState firstGameState = sampledGameStates.remove(0);
	    int ns = stateVstNum.get( firstGameState );
	    
	    if( ns < NSR_MAX_DATA_NUM ) {
			
		double nsr = 0.0;
		double discount = 1.0;
		for( int index = 0; index < sampledRewards.size(); index++ ) {
			
		    nsr += sampledRewards.get(index) * discount;
		    discount *= GAMMA;
		}
		dataNSR.get(firstGameState)[ns] = nsr;
	    }
	    else {
		
		//System.out.println("State has been explored for "+NSR_MAX_DATA_NUM+" times");
	    }
	    
	    /**
	     * also remove the first element in the reward list
	     */
	    sampledRewards.remove(0);
	}
	else if( sampledRewards.size() != sampledGameStates.size() ) {
	    
	    System.out.println("updateNSRData: The two sample lists are not with the same size");
	}

    }
    
    /**
     * update the NSR data when an episode ends
     * called by the game executor
     */
    public void updateNSRDataWhenGameOver( )
    {
	
	if( bLearning )
	    return;
	
	else if( sampledRewards.size() !=  
		sampledGameStates.size() ) {
		
	    System.out.println("Sampled List Size Not Match!");
	    return;
	}
	
	while( sampledRewards.size() > 0 ) {
	    
	    GameState firstGameState = sampledGameStates.remove(0);
	    int ns = stateVstNum.get( firstGameState );
	    
	    if( ns < NSR_MAX_DATA_NUM ) {
	
		double nsr = 0.0;
		double discount = 1.0;
		for( int index = 0; index < sampledRewards.size(); index++ ) {
			
		    nsr += sampledRewards.get(index) * discount;
		    discount *= GAMMA;
		}
		dataNSR.get(firstGameState)[ns] = nsr;
	    }
	    
	    /**
	     * also remove the first element in the reward list
	     */
	    sampledRewards.remove(0);
	}
    }
    
    
    /**
     * compute the KL-distance between two Gaussian distribution
     * N1(u1, sqrdSigma1), N2(u2, sqrdSigma2)
     * compute the distance D(N1||N2)
     */
    private double KLDistance( double u1, double sqrdSigma1, 
	    double u2, double sqrdSigma2 )
    {
	
	if( sqrdSigma1 < 0.000001 || 
		sqrdSigma2 < 0.000001 )
	    return 0.0;
	
	double sigma1 = Math.sqrt(sqrdSigma1);
	double sigma2 = Math.sqrt(sqrdSigma2);
	double u1_minus_u2 = u1 - u2;
	
	double x = 0.5 * u1_minus_u2 * u1_minus_u2 / sqrdSigma2;
	double y = 0.5 * (sqrdSigma1 - sqrdSigma2) / sqrdSigma2;
	double z = Math.log(sigma2/sigma1);
	
	return (x+y+z);
    }
 
    
    private boolean isRelated( GameState gameState )
    {
	if( relatedMap == null || gameState == null ) {
	    
	    System.out.println("isRelated: Wrong Parameters");
	    return false;
	}
	
	if( relatedMap.containsKey( gameState ) ) {
	    
	    return relatedMap.get( gameState );
	}
	else {
	    
	    System.out.println("isRelated: No such State Key");
	    
	    for( int ag = 0; ag < SparseGridWorld.NUM_AGENTS; ag++ ) {
		
		System.out.print("Agent "+ag+": "+gameState.getLocationID(ag));
	    }
	    System.out.println();
	    
	    return false;
	}
    }
    
    /**
     * whether this algorithm is learning
     * @return
     */
    public boolean isLearning()
    {
	return bLearning;
    }
    
    
    
    
    /**
     * read the local-Q function of each agent
     */
    private void readLocalQ()
    {
	
	/**
	 * init member locQs
	 */
	locQs = new double[SparseGridWorld.NUM_AGENTS]
		[SparseGridWorld.NUM_CELLS][GameAction.NUM_ACTIONS];
	
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
    
}
