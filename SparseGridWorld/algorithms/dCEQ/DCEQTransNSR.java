package algorithms.dCEQ;

import gameGridWorld.GameAction;
import gameGridWorld.GameState;
import gameGridWorld.SparseGridWorld;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

public class DCEQTransNSR extends DCEQ
{
    
    /**
     * N-step Return model for each agent
     * defined over each agents' local state space
     */
    private double[][] nsrMeanTrans;
    private double[][] nsrVarianceTrans;
    
    
    /**
     * the N-step Return data of this MARL algorithm
     */
    private HashMap<GameState, double[][]> dataNSR;
    
    /**
     * the visit number of each joint state
     */
    private HashMap<GameState, Integer> stateVstNum;
    

    /**
     * the rewards, joint state sampled during learning,
     * the max length of these lists is N
     */
    private ArrayList<double[]> sampledRewards;
    private ArrayList<GameState> sampledGameStates;
    
    /**
     * the maximum number of NSR data point of each state-action pair
     */
    public static final int NSR_MAX_DATA_NUM = 500;
    
    /**
     * the number of episodes for learning N-step Return model
     */
    public static final int NUM_EPISODE_NSR_MODEL_LEARNING = 100;//400;
    
    /**
     * the number of steps to look forward
     */
    private int numNStep = 5;
    
    /**
     * whether the algorithm should learn now
     */
    //private boolean bLearning = false;
    
    /**
     * a boolean map which indicates whether 
     * each agents are related in each game state
     * 
     * relatedMap is computed by comparing the single-agent NSR model
     * and the NSR model of the multi-agent system
     */
    //private HashMap<GameState, boolean[]> relatedMap;
    
    /**
     * local Q-table for value function transfer
     */
    private double[][][] locQs;
    
    /**
     * whether the algorithm should learn now
     */
    private boolean bLearning = false;
    
    protected HashMap<GameState, boolean[]> relatedMap;
    
    public DCEQTransNSR( int agIndex ) 
    {
	super( agIndex );
	
	int numAgents = SparseGridWorld.NUM_AGENTS;
	int numGrids = SparseGridWorld.NUM_CELLS;
	
	nsrMeanTrans = new double[numAgents][numGrids];
	nsrVarianceTrans = new double[numAgents][numGrids];
	
	/**
	 * init the hashmap of the NSR data 
	 * and the state visit number
	 */
	dataNSR = new HashMap<GameState, double[][]>();
	stateVstNum = new HashMap<GameState, Integer>();
	ArrayList<GameState> allStates = SparseGridWorld.getAllValidStates();
	for( int stateIndex = 0; stateIndex < allStates.size(); stateIndex++ ) {
	    
	    GameState gameState = allStates.get( stateIndex );
	    double[][] nsrDataArray = new double[numAgents][NSR_MAX_DATA_NUM]; 
	    for( int agent = 0; agent < numAgents; agent++ ) {
		
		for( int index = 0; index < NSR_MAX_DATA_NUM; index++ ) {
			
		    nsrDataArray[agent][index] = 0;
		}
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
	sampledRewards = new ArrayList<double[]>();
	sampledGameStates = new ArrayList<GameState>();
	
	/**
	 * transfer the N-step return model 
	 */
	transferNSR();
    }
    
    public DCEQTransNSR( int agIndex, double alpha, 
	    double gamma, double epsilon )
    {
	super( agIndex, alpha, gamma, epsilon);
	
	int numAgents = SparseGridWorld.NUM_AGENTS;
	int numGrids = SparseGridWorld.NUM_CELLS;
	
	nsrMeanTrans = new double[numAgents][numGrids];
	nsrVarianceTrans = new double[numAgents][numGrids];
	
	/**
	 * init the hashmap of the NSR data 
	 * and the state visit number
	 */
	dataNSR = new HashMap<GameState, double[][]>();
	stateVstNum = new HashMap<GameState, Integer>();
	ArrayList<GameState> allStates = SparseGridWorld.getAllValidStates();
	for( int stateIndex = 0; stateIndex < allStates.size(); stateIndex++ ) {
	    
	    GameState gameState = allStates.get( stateIndex );
	    double[][] nsrDataArray = new double[numAgents][NSR_MAX_DATA_NUM]; 
	    for( int agent = 0; agent < numAgents; agent++ ) {
		
		for( int index = 0; index < NSR_MAX_DATA_NUM; index++ ) {
			
		    nsrDataArray[agent][index] = 0;
		}
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
	sampledRewards = new ArrayList<double[]>();
	sampledGameStates = new ArrayList<GameState>();
	
	/**
	 * transfer the N-step return model 
	 */
	transferNSR();
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
	    
	    for( int agIndex = 0; agIndex < SparseGridWorld.NUM_AGENTS; agIndex++ ) {
	
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
		    nsrMeanTrans[agIndex][locState] = nsrMean;
		    nsrVarianceTrans[agIndex][locState] = nsrVar;
			    
		    locState++;
		    if( locState >= SparseGridWorld.NUM_CELLS ) {
				
			break;
		    }
		}
		reader.close();	
	    }
	}
	catch (Exception e)
	{
	    // TODO: handle exception
	    e.printStackTrace();
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
	    
	    System.out.println("@NegoQAbsGame->updateQ: NULL nextState!");
	    return null;
	}
	//if we are exploring the multi-agent system
	else if( !bLearning ) {
	    
	    /**
	     * choose a random action
	     */
	    GameAction nextAction = new GameAction();
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		
		nextAction.setAction( agent, random.nextInt(GameAction.NUM_ACTIONS) );
	    }
	    
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
		updateNSRData();
		
		/**
		 * add the current rewards and game state to 
		 * the sampled list
		 */
		double[] rwds = new double[SparseGridWorld.NUM_AGENTS];
		for( int agIndex = 0; agIndex < SparseGridWorld.NUM_AGENTS; agIndex++ ) {
		    
		    rwds[agIndex] = rewards[agIndex];
		}
		sampledRewards.add(rwds);
		sampledGameStates.add(curState);
	    }
	    
	    return nextAction;
	}
	//if we are learning the value function
	else {

	    /**
	     * compute the correlated equilibrium in the next state
	     */
	    double[] correlEquil = computeCE( agentIndex, nextState );
	    
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
		    
		for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		    
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
		
		ALPHA *= 0.9991;//0.998;//0.9991;//0.998;//0.99;//0.995;//0.99985;//0.99988;//58;//0.9975;//0.99958;
		
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
	     * then transfer the single-agent value function
	     * or conduct game abstraction
	     */
	    
	    /**
	     * or initialize the local-value function
	     */
	    int agentNum = SparseGridWorld.NUM_AGENTS;
	    int locNum = SparseGridWorld.NUM_CELLS;
	    int actionNum = GameAction.NUM_ACTIONS;	
	    locQs = new double[agentNum][locNum][actionNum];
	    for( int ag = 0; ag < SparseGridWorld.NUM_AGENTS; ag++ )
		for( int s = 0; s < locNum; s++ ) {
		    for( int a = 0; a < actionNum; a++ ) {
			    
			locQs[ag][s][a] = random.nextDouble();
		    }
		}
	    
	    //transfer the local value function
	    /**
	     * transfer value function
	     */
	    readLocalQ();
	    transferValueFunction();
	}
    }
    
    private void updateNSRData( )
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
		
		for( int agIndex = 0; agIndex < SparseGridWorld.NUM_AGENTS; agIndex++ ) {
		    
		    double nsr = 0.0;
		    double discount = 1.0;
		    for( int index = 0; index < sampledRewards.size(); index++ ) {
				
			nsr += sampledRewards.get(index)[agIndex] * discount;
			discount *= GAMMA;
		    }
		    dataNSR.get(firstGameState)[agIndex][ns] = nsr;
		}
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
     * according to the NSR model, 
     * determine in each joint state that whether 
     * each agent is related to the other agents
     */
    private void generateRelatedMap()
    {
	relatedMap = new HashMap<GameState, boolean[]>();
	
	/**
	 * get all joint states
	 */
	ArrayList<GameState> allStates = SparseGridWorld.getAllValidStates();
	ArrayList<double[]> klDisList = new ArrayList<double[]>(); 
	
	double[] maxKLDisArray =  new double[SparseGridWorld.NUM_AGENTS];
	for( int agIndex = 0; agIndex < SparseGridWorld.NUM_AGENTS; agIndex++ ) {
	    
	    maxKLDisArray[agIndex] = Double.NEGATIVE_INFINITY;
	}

	
	for( int stateIndex = 0; stateIndex < allStates.size(); stateIndex++ ) {
	    
	    GameState gameState = allStates.get( stateIndex );
	
	    double[] klDisArray = new double[SparseGridWorld.NUM_AGENTS];
	    for( int agIndex = 0; agIndex < SparseGridWorld.NUM_AGENTS; agIndex++ ) {
		
		/**
		 * compute the NSR distribution in this joint state
		 */
		double[] nsrDistr = computeNSRDistribution( agIndex, gameState);
		double averNSR = nsrDistr[0];
		double varNSR = nsrDistr[1];
			
		/**
		 * find the NSR distribution of the local state
		 */
		int localState = gameState.getLocationID(agentIndex);
		double averNSR_Local = nsrMeanTrans[agIndex][localState];
		double varNSR_Local = nsrVarianceTrans[agIndex][localState];
			
		/**
		 * compute the KL-distance of the two Gaussian distribution
		 */
		double klDis = KLDistance(averNSR_Local, varNSR_Local, 
			averNSR, varNSR);
		
		klDisArray[agIndex] = klDis;
		    
		if( klDis > maxKLDisArray[agIndex] ) {
			
		    maxKLDisArray[agIndex] = klDis;
		}
	    }
	    klDisList.add(klDisArray);
	}
	
	int gameCount = 0;
	for( int stateIndex = 0; stateIndex < allStates.size(); stateIndex++ ) {
	    
	    GameState gameState = allStates.get( stateIndex );
	    
	    boolean[] related = new boolean[SparseGridWorld.NUM_AGENTS]; 
	    int count = 0;
	    for( int agIndex = 0; agIndex < SparseGridWorld.NUM_AGENTS; agIndex++ ) {
		
		double klDis = klDisList.get(stateIndex)[agIndex];
		related[agIndex] = false;
		if( klDis > maxKLDisArray[agIndex] / 50 ) {
			    
		    related[agIndex] = true;
		    count++;
		}
	    }
	    
	    for( int agIndex = 0; agIndex < SparseGridWorld.NUM_AGENTS; agIndex++ ) {
		
		if( count < 2 && related[agIndex] ) {
		   
		    related[agIndex] = false;
		}
	    }
	    if( count >= 2 )
		gameCount++;
	    
	    if( !relatedMap.containsKey(gameState) )
		relatedMap.put( gameState, related );
	}
	
	
	System.out.println(allStates.size()+"States, Game Count "+gameCount);
	
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
    private double[] computeNSRDistribution( int agIndex, GameState gameState )
    {
	
	double[] nsrArray = dataNSR.get(gameState)[agIndex];
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
	double z = 0 - Math.log(sigma1/sigma2);
	
	return (x+y+z);
    }
    
    protected boolean isRelated( int agIndex, GameState gameState )
    {
	if( relatedMap == null || gameState == null ||
		agIndex < 0 || agIndex >= SparseGridWorld.NUM_AGENTS ) {
	    
	    System.out.println("isRelated: Wrong Parameters");
	    return false;
	}
	
	if( relatedMap.containsKey( gameState ) ) {
	    
	    return relatedMap.get(gameState)[agIndex];
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
		
		String fileName = "./Rmax_agent"+agentIndex+".txt";
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
	   
	    for( int agIndex = 0; agIndex < SparseGridWorld.NUM_AGENTS; agIndex++ ) {
		
		int locState = state.getLocationID( agIndex );
		/**
		 * if the distance between the same states 
		 * in two different models is small (which means they are similar)
		 * then we transfer the value functions
		 */
		if( !isRelated( agIndex, state ) ) {
			    
		    transferCount++;
			
		    /**
		     * for all joint actions 
		     */
		    for( int jntActIndex = 0; jntActIndex < jointActions.size(); jntActIndex++ ) {
			    
			GameAction jntAction = jointActions.get( jntActIndex );
			int locAction = jntAction.getAction( agIndex );
			double locQValue = locQs[agIndex][locState][locAction];
				   
			setQValue( agIndex, state, jntAction, locQValue );    
		    }
		}
		else {
			    
		    for( int jntActIndex = 0; jntActIndex < jointActions.size(); jntActIndex++ ) {
				    
			GameAction jntAction = jointActions.get( jntActIndex );
			setQValue( agIndex, state, jntAction, 0.0 );  //?
		    } 
		}
	    }
	}
	
	System.out.println("All States: "+allStates.size());
	System.out.println("TransferCount: "+transferCount);
	

    }
}

