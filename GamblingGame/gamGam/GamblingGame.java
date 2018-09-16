package gamGam;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Random;


import algorithms.CenCEQ;
import algorithms.DCEQTrans;
import algorithms.ECEQTrans;
import algorithms.NashQ;
import algorithms.DCEQ;
import algorithms.DecenCEQ;
import algorithms.ECEQ;
import algorithms.MARL;
import algorithms.NashQTrans;
import algorithms.PCEQ;
import algorithms.PCEQTrans;
import algorithms.UCEQ;
import algorithms.UCEQTrans;


/**
 * The definition of a grid world game
 * 
 * this is a 2-agent grid world game 
 * with a 3x3 world
 * 
 * allowing stochastic transitions
 */
public class GamblingGame
{
    /**
     * important parameters of the grid-world game
     */
    public static final int NUM_AGENTS = 10;//2;//5;
    
    
    /**
     * the number of episodes in one game
     */
    public static final int episodesNum = 5000;//20000;//5000;//10000;//50000;//50000;//5000;//100000;
    //500 for similarity 
    //50000 for error
    
    /**
     * all possible states and all possible joint actions
     */
    public static ArrayList<GameState> allStates;
    public static ArrayList<GameAction> allJointActions;
    
    
    public static final int LOOP = 10;
    
    
    public GamblingGame()
    {
	
	/**
	 * generate all possible state-action pairs
	 */
	generateAllStates();
	generateAllJointActions();
	
    }
    
    
    //right
    public boolean gameOver( GameState gameState ) 
    {

	return gameState.isTerminalState();
    }
    
    
    
    /**
     * 
     * do actions and set the reward
     */
    public GameState doAction( GameState gameState, GameAction jntAction, double[] rewards )
    {
	if( gameState == null || jntAction == null ||
	    rewards == null || rewards.length != NUM_AGENTS ) {
	    
	    System.out.println( "GamblingGame->doAction: Something Wrong in Parameters!" );
	    return null;
	}
	
	//if the game is over
	if( gameState.isTerminalState() ) {
	    
	    System.out.println("GamblingGame->doAction: The current state is a terminal state!");
	    return null;
	}
	
	//get the next state
	int next_pot = gameState.getPot()- GameState.DCREASE_SIZE;
	int next_bigsmall = new Random().nextInt(2);
	
	//get the number of agents that bet small and bet big
	int agentNum_BetSml = 0;
	for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
	    
	    if( jntAction.getAction( agentIndex ) == GameAction.BET_SMALL ) {
		
		agentNum_BetSml += 1;
	    }
	}
	int agentNum_BetBig = NUM_AGENTS - agentNum_BetSml;
	
	//set the rewards
	if( next_bigsmall == GameState.BIG_WIN ) {
		
	    for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
		
		if( jntAction.getAction( agentIndex ) == GameAction.BET_BIG ) {
			
		    rewards[agentIndex] = GameState.REWARD_SIZE / agentNum_BetBig;
		}
		else 
		    rewards[agentIndex] = 0.0;
	    }
	}
	else {
	
	    for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
		
		if( jntAction.getAction( agentIndex ) == GameAction.BET_SMALL ) {
			
		    rewards[agentIndex] = GameState.REWARD_SIZE / agentNum_BetSml;
		}
		else 
		    rewards[agentIndex] = 0.0;
	    }
	}
	
	//set the next state
	gameState.setPot( next_pot );
	gameState.setBigSmall( next_bigsmall );
	
	return gameState;
    }
    
    
    
    /**
     * one test contains several algorithms run
     */
    public void oneTest()
    {
	int[] algTypes = new int[]{ //
		
		//MARL.uCEQ_TRANS,
		//MARL.dCEQ_TRANS, 
		//MARL.eCEQ_TRANS, MARL.pCEQ_TRANS		
		//MARL.uCEQ
		//MARL.eCEQ, 
		//MARL.pCEQ,
		//MARL.dCEQ
		MARL.NASHQ_TRANS
		//MARL.NASHQ
		};

	
	//int[] algTypes = new int[]{ MARL.dCEQ };
	
	for( int algIndex = 0; algIndex < algTypes.length; algIndex++ ) {
	    
	    
	    int algType = algTypes[algIndex];
	    
	    if( algType == MARL.uCEQ || algType == MARL.eCEQ ||
		    algType == MARL.pCEQ || algType == MARL.NASHQ ||
		    algType == MARL.NASHQ_TRANS || algType == MARL.NASHQ_SUPP_TRANS || 
		    algType == MARL.uCEQ_TRANS || algType == MARL.eCEQ_TRANS ||
		    algType == MARL.pCEQ_TRANS )
		oneRun( algType, true );
	    else
		oneRun( algType, false );
	}
    }
    
    /**
     * one run contains one algorithm's several episodes 
     */
    public void oneRun( int algType, boolean isCentral )
    {
	
	int LOOP = 1;
	
	/**
	 * choose the algorithm
	 */
	String algStr = MARL.ALG_STRINGS[algType];
	
	
	/**
	 * for recording steps
	 */
	double[][] allRewards = new double[NUM_AGENTS][episodesNum];
	for( int agent = 0; agent < GamblingGame.NUM_AGENTS; agent++ )
	    for( int ep = 0; ep < episodesNum; ep++ ) {
		
		allRewards[agent][ep] = 0;
	    }
	
	/**
	 * for recording time duration
	 */
	long[] durTimes = new long[episodesNum];
	    
	//for the time of the hole process
	long beginTime = System.nanoTime();
		
	for( int loop = 1; loop <= LOOP; loop++ ) {
	
	    System.out.println("Algorithm: "+algStr+" the "+loop+"-th loop===========");
		
	    /**
	     * for centralized algorithms
	     */
	    MARL agent = createMARL( algType, 0 );
		
	    /**
	     * for decentralized algorithms
	     */
	    MARL[] agents = new MARL[GamblingGame.NUM_AGENTS];
	    for( int agentIndex = 0; agentIndex < GamblingGame.NUM_AGENTS; agentIndex++ ) {
		
		agents[agentIndex] = createMARL( algType, agentIndex );
	    }
	    
	    
	    /**
	    if( isCentral )
		agent.gameStarted(loop);
	    else {
		
		agent0.gameStarted(loop);
		//agent1.gameStarted(loop);
	    }
	    */
	    
	    
	    double[][] retArray = new double[episodesNum][GamblingGame.NUM_AGENTS];
	    for( int ep = 0; ep < episodesNum; ep++ ) {
		    
		long startTime = System.nanoTime();
		
		
		if( isCentral )
		    retArray[ep] = oneEpisodeCentral( agent );
		else {
		    
		    retArray[ep] = oneEpisode( agents );
		   
		}
		long endTime = System.nanoTime();
		durTimes[ep] = endTime - startTime;
		   
		for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
		    
		    allRewards[agentIndex][ep] += retArray[ep][agentIndex];
		}
		      
		/**
		 * display the progress
		 */
		displayProgress( ep+1 );   
	    }
	    
	    
	    //release?
	    retArray = null;
	    
	    //one loop finished
	    /**
	    if( isCentral )
		agent.gameFinished(loop);
	    else {
		
		agent0.gameFinished(loop);
		//agent1.gameFinished(loop);
	    }
	    */
	    
	}
	
	long overTime = System.nanoTime();
	
	/**
	 * write the steps and times into files
	 */
	try
	{
	    
	    //write times
	    /**/
	    BufferedWriter timeWriter = new BufferedWriter(new FileWriter("./" + algStr+ "_time.csv"));
	    for( int ep = 0; ep < episodesNum; ep++ ) {
		
		timeWriter.write( durTimes[ep] + ", ");
	    }
	    timeWriter.close();
	    
	    
	    timeWriter = new BufferedWriter(new FileWriter("./" + algStr+ "_allTime.txt"));
	    timeWriter.write(""+((overTime-beginTime)/1000000000.0/LOOP));
	    timeWriter.close();
	    
	    
	    //write rewards
	    /**/
	    for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
		
		BufferedWriter rewardsWriter = new BufferedWriter(new FileWriter("./rewd_" + algStr+"_agent" + agentIndex + ".csv"));
		for( int ep = 0; ep < episodesNum; ep++ ) {
		    
		    rewardsWriter.write( allRewards[agentIndex][ep] / LOOP + ", ");
		}
		rewardsWriter.close();
	    }
	    
	    
	}
	catch (IOException e)
	{
	    e.printStackTrace();
	}
	
    }
    
    /**
     * 
     * @return two arrays, one for steps, one for rewards
     */
    public double[] oneEpisode( MARL[] agents )
    {
	if( agents == null || 
		agents.length != GamblingGame.NUM_AGENTS ) {
	    
	    return null;
	}
	
	/**
	 * dimension 0: steps
	 * dimension 1: rewards
	 */
	double[] retArray = new double[NUM_AGENTS];
	
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    retArray[agent] = 0.0;
	}
	
	/**
	int[] stepNums = new int[NUM_AGENTS];
	for( int agent = 0; agent < NUM_AGENTS; agent++ )
	    stepNums[agent] = 0;
	*/
	
	/**
	 * init the states
	 */
	GameState gameState = new GameState( GameState.UPPER_BOUND_POT, 
		    GameState.SMALL_WIN );
	
	/**
	 * get the action expected taken by each agent
	 */
	GameAction[] jntActions = new GameAction[GamblingGame.NUM_AGENTS];
	for( int agentIndex = 0; agentIndex < GamblingGame.NUM_AGENTS; agentIndex++ ) {
	    
	    jntActions[agentIndex] = agents[agentIndex].updateQ( null, null, null, gameState);
	}
	
	/**
	 * the action actually taken
	 */
	GameAction jntAction = new GameAction();
	
	/**
	 * the reward for each agent in each transfer
	 */
	double[] rewards = new double[NUM_AGENTS];
	    for( int agent = 0; agent < NUM_AGENTS; agent++ )
		rewards[agent] = 0.0;
	
	while( !gameOver( gameState ) ) {
	    
	    /**
	     * epsilon-greedy and get the action to be taken actually
	     */
	    for( int agentIndex = 0; agentIndex < GamblingGame.NUM_AGENTS; agentIndex++ ) {
		    
		int agentAction = jntActions[agentIndex].getAction(agentIndex);
		jntAction.setAction( agentIndex, agents[agentIndex].epsilonGreedy( agentAction) );
	    }
	    
	    
	    /**
	     * observe the next state and get the rewards
	     */
	    GameState nextState = doAction( gameState, jntAction, rewards );
	    
	    //record the rewards
	    for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
		
		retArray[agent] += rewards[agent];  
	    }
	    
	    /**
	     * update Q-values
	     */
	    for( int agentIndex = 0; agentIndex < GamblingGame.NUM_AGENTS; agentIndex++ ) {
		    
		jntActions[agentIndex] = agents[agentIndex].updateQ( gameState, jntAction, rewards, nextState);
	    }
	    
	    /**
	     * transfer to the next state
	     */
	    gameState = nextState;
	    
	}
	
	return retArray;
    }
    
    /**
     * 
     * @return two arrays, one for steps, one for rewards
     */
    public double[] oneEpisodeCentral( MARL agent )
    {
	/**
	 * dimension 0: steps
	 * dimension 1: rewards
	 */
	double[] retArray = new double[NUM_AGENTS];
	
	for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
	    
	    retArray[agentIndex] = 0.0;
	}
	
	
	/**
	 * init the states
	 */
	GameState gameState = new GameState( GameState.UPPER_BOUND_POT, 
		    GameState.SMALL_WIN );
	
	/**
	 * get the action expected taken by each agent
	 */
	GameAction jntAction = agent.updateQ( null, null, null, gameState );
	
	/**
	 * the reward for each agent in each transfer
	 */
	double[] rewards = new double[NUM_AGENTS];
	    for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ )
		rewards[agentIndex] = 0.0;
	
	while( !gameOver( gameState ) ) {
	    
	    /**
	     * epsilon-greedy and get the action to be taken actually
	     */
	    jntAction = agent.epsilonGreedy(jntAction);
	    
	    
	    /**
	     * observe the next state and get the rewards
	     */
	    GameState nextState = doAction( gameState, jntAction, rewards );
	    
	    //record the rewards
	    for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
		
		retArray[agentIndex] += rewards[agentIndex];  
	    }
	    
	    /**
	     * update Q-values
	     */
	    GameAction nextAction = agent.updateQ( gameState, jntAction, rewards, nextState );
	    
	    /**
	     * transfer to the next state
	     */
	    gameState = nextState;
	    jntAction = null; //??
	    jntAction = nextAction;
	    
	}
	
	return retArray;
    }
    
    
    
    private void displayProgress( int ep )
    {
	
	NumberFormat numberFormat = NumberFormat.getNumberInstance();
	numberFormat.setMaximumFractionDigits(2);
	numberFormat.setMinimumFractionDigits(2);
	numberFormat.setMaximumIntegerDigits(3);
	numberFormat.setMinimumIntegerDigits(3);
	
	
	if( ep == 1)
	    System.out.println("Progress:" + numberFormat.format(100 * (double) ep / episodesNum) + "%");
	else if( ep % 100 == 0)
	{
	    System.out.println("Progress:" + numberFormat.format(100 * (double) ep / episodesNum) + "%");
	}
    }
    
    private MARL createMARL( int alg, int agent )
    {
	switch( alg )
	{
	case MARL.DENCE_CEQ:
	    return new DecenCEQ( agent );
	case MARL.CEN_CEQ:
	    return new CenCEQ();
	case MARL.uCEQ:
	    return new UCEQ();
	case MARL.eCEQ:
	    return new ECEQ();
	case MARL.pCEQ:
	    return new PCEQ();
	case MARL.dCEQ:
	    return new DCEQ( agent );
	case MARL.NASHQ:
	    return new NashQ();
	case MARL.NASHQ_TRANS:
	    return new NashQTrans();
	case MARL.uCEQ_TRANS:
	    return new UCEQTrans();
	case MARL.eCEQ_TRANS:
	    return new ECEQTrans();
	case MARL.pCEQ_TRANS:
	    return new PCEQTrans();
	case MARL.dCEQ_TRANS:
	    return new DCEQTrans( agent );
	default:
	    return new MARL( agent );
	}
    }
    
	
    //right
    private void generateAllStates()
    {
	
	allStates = new ArrayList<GameState>();
	
	int pot = GameState.UPPER_BOUND_POT;
	int bigsmall = GameState.SMALL_WIN;
	
	
	/**
	 * for each agent 
	 */
	
	while( true ) {
	    
	    //generate the new state
	    GameState gameState = new GameState( pot, bigsmall );
		
	    if( !allStates.contains( gameState ) ) {
		    
		allStates.add( gameState );
	    }
	    else {
		    
		gameState = null;
	    }
	    
	    
	    /**
	     * move to the next state
	     * first change the bigsmall variable
	     * then change the pot size
	     */
	    bigsmall++;
	    if( bigsmall > GameState.BIG_WIN ) {
		
		bigsmall = GameState.SMALL_WIN;
		
		//get the next pot size
		pot -= GameState.DCREASE_SIZE;
		
		/**
		 * check the stop condition
		 */
		if( pot < GameState.LOWER_BOUND_POT ) {
			
		    break;
		}
	    }
	}
	
    }
	
    public static int queryJointActionIndex( GameAction jntAction )
    {
	if( jntAction == null ) {
	    
	    return -1;
	}
	else if( allJointActions == null ) {
	    
	    return -1;
	}
	
	for( int listIndex = 0; listIndex < allJointActions.size(); listIndex++ ) {
	    
	    GameAction gameAction = allJointActions.get( listIndex );
	    
	    if( gameAction.equals( jntAction ) ) {
		
		return listIndex;
	    }
	}
	
	return -1;
    }
    
    //right
    private void generateAllJointActions()
    {
	allJointActions = new ArrayList<GameAction>();
	
	int[] actionIterator = new int[NUM_AGENTS];
	
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    actionIterator[agent] = 0;
	}
	
	while( true ) {
	    
	    GameAction gameAction = new GameAction( actionIterator );
	    
	    if( !allJointActions.contains( gameAction ) ) {
		
		allJointActions.add( gameAction );
	    }
	    else
		gameAction = null;
	    
	    /**
	     * move to the next action
	     */
	    for( int agent = NUM_AGENTS-1; agent >= 0; agent-- ) {
		
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
    }
    
 
    
    /**
     * @param args
     */
    public static void main(String[] args)
    {
	// TODO Auto-generated method stub
	
	GamblingGame gridWorld = new GamblingGame();
	
	gridWorld.oneTest();

    }
}
