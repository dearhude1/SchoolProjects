package algorithms.uCEQ;

import gameGridWorld.GameAction;
import gameGridWorld.GameState;
import gameGridWorld.SparseGridWorld;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import sun.security.krb5.internal.LocalSeqNumber;

public class UCEQAbsGameNSR2 extends UCEQAbsGame
{
    
    /**
     * N-step Return model for each agent
     * defined over each agents' local state space
     */
    private double[][] nsrMeanTrans;
    private double[][] nsrVarianceTrans;
    
    
    /**
     * the N-step Return data of this MARL algorithm
     * local state
     */
    private HashMap<Integer, double[][]> dataNSR;
    
    /**
     * the visit number of each local state 
     * for each agent
     */
    private HashMap<Integer, int[]> stateVstNum;
    

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
     * local Q-table for single-agent learning
     * note that this algorithm does not transfer value 
     * functions from Q-learning or R-max learning
     */
    //private double[][][] locQs;
    
    
    public UCEQAbsGameNSR2( ) 
    {
	super( );
	
	int numAgents = SparseGridWorld.NUM_AGENTS;
	int numGrids = SparseGridWorld.NUM_CELLS;
	
	nsrMeanTrans = new double[numAgents][numGrids];
	nsrVarianceTrans = new double[numAgents][numGrids];
	
	/**
	 * init the hashmap of the NSR data 
	 * and the state visit number
	 */
	dataNSR = new HashMap<Integer, double[][]>();
	stateVstNum = new HashMap<Integer, int[]>();
	ArrayList<GameState> allStates = SparseGridWorld.getAllValidStates();
	for( int locState = 0; locState < SparseGridWorld.NUM_CELLS; locState++ ) {
	 
	    double[][] nsrDataArray = new double[numAgents][NSR_MAX_DATA_NUM];
	    int[] vstNumArray = new int[SparseGridWorld.NUM_AGENTS]; 
	    for( int agIndex = 0; agIndex < numAgents; agIndex++ ) {
		
		vstNumArray[agIndex] = 0;
		for( int index = 0; index < NSR_MAX_DATA_NUM; index++ ) {
			
		    nsrDataArray[agIndex][index] = 0;
		}
	    }
		
	    if( !dataNSR.containsKey( locState ) ) {
		    
		dataNSR.put( locState, nsrDataArray );
	    }
	    else
		nsrDataArray = null;
	    
	    if( !stateVstNum.containsKey( locState ) ) {
		
		stateVstNum.put( locState, vstNumArray );
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
    
    public UCEQAbsGameNSR2( double alpha, double gamma, double epsilon )
    {
	super(alpha, gamma, epsilon);
	
	int numAgents = SparseGridWorld.NUM_AGENTS;
	int numGrids = SparseGridWorld.NUM_CELLS;
	
	nsrMeanTrans = new double[numAgents][numGrids];
	nsrVarianceTrans = new double[numAgents][numGrids];
	
	/**
	 * init the hashmap of the NSR data 
	 * and the state visit number
	 */
	dataNSR = new HashMap<Integer, double[][]>();
	stateVstNum = new HashMap<Integer, int[]>();
	ArrayList<GameState> allStates = SparseGridWorld.getAllValidStates();
	for( int locState = 0; locState < SparseGridWorld.NUM_CELLS; locState++ ) {
	 
	    double[][] nsrDataArray = new double[numAgents][NSR_MAX_DATA_NUM];
	    int[] vstNumArray = new int[SparseGridWorld.NUM_AGENTS]; 
	    for( int agIndex = 0; agIndex < numAgents; agIndex++ ) {
		
		vstNumArray[agIndex] = 0;
		for( int index = 0; index < NSR_MAX_DATA_NUM; index++ ) {
			
		    nsrDataArray[agIndex][index] = 0;
		}
	    }
		
	    if( !dataNSR.containsKey( locState ) ) {
		    
		dataNSR.put( locState, nsrDataArray );
	    }
	    else
		nsrDataArray = null;
	    
	    if( !stateVstNum.containsKey( locState ) ) {
		
		stateVstNum.put( locState, vstNumArray );
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
		 * make a visit to the each local state
		 */
		for( int agIndex = 0; agIndex < SparseGridWorld.NUM_AGENTS; agIndex++ ) {
		    
		    stateVstNum.get(curState.getLocationID(agIndex))[agIndex] += 1;
		}

		
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
		    
		    if( !isRelated( agent, nextState ) ) {
			
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
		    
		    if( isRelated( agent, nextState ) ) {
			
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
		    if( isRelated( agent, curState ) )
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
		    
		    if( isRelated( agent, curState ) ) {
			
			double Qsa = getQValue( agent, curState, updatedJntAction );
			
			if( isRelated( agent, nextState ) ) {
			    
			    double CEQ = correlValues[agent];
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
			if( isRelated( agent, nextState ) ) {
			    
			    double CEQ = correlValues[agent];
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
		//ALPHA *= 0.9991;//0.99999;//985;//988;//58;//0.9975;//0.99958;
		
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
	    for( int agIndex = 0; agIndex < SparseGridWorld.NUM_AGENTS; agIndex++ ) {
		 
		int locState = firstGameState.getLocationID(agIndex);
		int ns = stateVstNum.get(locState)[agIndex];
		if( ns < NSR_MAX_DATA_NUM ) {
			
		    double nsr = 0.0;
		    double discount = 1.0;
		    for( int index = 0; index < sampledRewards.size(); index++ ) {
					
			nsr += sampledRewards.get(index)[agIndex] * discount;
			discount *= GAMMA;
		    }
		    dataNSR.get(locState)[agIndex][ns] = nsr;
		}
		else {
			
		    //System.out.println("State has been explored for "+NSR_MAX_DATA_NUM+" times");
		}
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

	
	for( int locState = 0; locState < SparseGridWorld.NUM_CELLS; locState++ ) {
	    
	    double[] klDisArray = new double[SparseGridWorld.NUM_AGENTS];
	    for( int agIndex = 0; agIndex < SparseGridWorld.NUM_AGENTS; agIndex++ ) {
		
		/**
		 * compute the NSR distribution in this joint state
		 */
		double[] nsrDistr = computeNSRDistribution( agIndex, locState );
		double averNSR = nsrDistr[0];
		double varNSR = nsrDistr[1];
			
		/**
		 * find the transferred NSR distribution of the local state
		 */
		double averNSR_Trans = nsrMeanTrans[agIndex][locState];
		double varNSR_Trans = nsrVarianceTrans[agIndex][locState];
			
		/**
		 * compute the KL-distance of the two Gaussian distribution
		 */
		double klDis = KLDistance(averNSR_Trans, varNSR_Trans, 
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
		
		int locState = gameState.getLocationID(agIndex);
		double klDis = klDisList.get(locState)[agIndex];
		related[agIndex] = false;
		if( klDis >= maxKLDisArray[agIndex] / 10 ) {
			    
		    count++;
		    related[agIndex] = true;
		}
		
		//System.out.println("Max KL: "+maxKLDisArray[agIndex]+" KL"+klDis);
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
    private double[] computeNSRDistribution( int agIndex, int locState )
    {
	
	double[] nsrArray = dataNSR.get(locState)[agIndex];
	int ns = stateVstNum.get(locState)[agIndex];
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
    
    
}
