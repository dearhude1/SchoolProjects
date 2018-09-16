package soccerGame;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Random;


import algorithms.HAMMQ;
import algorithms.MARL;
import algorithms.MiniMaxQ;
import algorithms.MiniMaxQTrans;



/**
 * The definition of a grid world game
 * 
 * this is a 2-agent grid world game 
 * with a 3x3 world
 * 
 * allowing stochastic transitions
 */
public class SoccerGame
{
    /**
     * important parameters of the grid-world game
     */
    public static final int NUM_AGENTS = 2;
    public static final int NUM_LOCATIONS = 20;
    public static final int WORLD_WIDTH = 5;
    public static final int WORLD_HEIGHT = 4;
    
    /**
     * agent A is from left to right
     * agent B is from right to left
     */
    public static final int AGENT_A = 0;
    public static final int AGENT_B = 1;
    
    /**
     * goal possession 
     */
    public static final int BALL_ON_A = 0;
    public static final int BALL_ON_B = 1;
    
    /**
     * the location ID of each agent's goal
     */
    //private int[] agentGoals;
    private int[] agentInitLocs;
    
    /**
     * the number of episodes in one game
     */
    public static final int matchNum = 500;
    
    public static final int LOOP = 1;
    
    private int actionOrder = 0;
    
    public SoccerGame()
    {
	agentInitLocs = new int[NUM_AGENTS];
	 
	 /**
	  * read the initial locations
	  */
	 try {
	     
	     BufferedReader bufReader = new BufferedReader(new FileReader("./initLocations.txt")); 
	     String line = "";
	     
	     int agent = 0;
	     while( (line = bufReader.readLine()) != null ) {
		 
		 if( line.length() == 0 )	 
		     continue;
		 
		 agentInitLocs[agent] = Integer.parseInt(line);
		 agent++;
	     }
	     bufReader.close();
	 }
	 catch(IOException ioe){
	     
	     ioe.printStackTrace();
	 }
    }
    
    public int[] getInitLocationIDs()
    {
	return agentInitLocs;
    }
    
    /**
     * if the agent which has the goal reaches the goal line 
     * the game then is over
     */
    public boolean gameOver( GameState gameState ) 
    {
	int[] agentLocationIDs = gameState.getLocationIDs();
	
	if( agentLocationIDs == null || 
		agentLocationIDs.length != NUM_AGENTS ) {
	    
	    System.out.println("SoccerGame->gameOver: Game Over With Error!");
	    return false;
	}
	
	int goalPoss = gameState.getBallPossession();
	if( goalPoss == BALL_ON_A ) {
	    
	    int loc_A = gameState.getLocationID( AGENT_A );
	    if( reachGoal( loc_A, AGENT_A) )
		return true;
	    else
		return false;
	}
	else {
	    
	    int loc_B = gameState.getLocationID( AGENT_B );
	    if( reachGoal( loc_B, AGENT_B ) )
		return true;
	    else
		return false;
	}
	
    }
    
    
    public GridWorldLocation nextLocation( GridWorldLocation location, int action )
    {
	if( location == null ) {
	    
	    System.out.println( "@SoccerGame->nextLocation: Parameter location is NULL!");
	    return null;
	}
	
	int locationRow = location.getRow();
	int locationCol = location.getCol();
	
	/**
	 * be careful
	 * we do not test collision and agent out of boundary
	 */
	if( action == GameAction.RIGHT )
	    locationCol += 1;
	else if( action == GameAction.UP )
	    locationRow += 1;
	else if( action == GameAction.LEFT )
	    locationCol -= 1;
	else if( action == GameAction.DOWN )
	    locationRow -= 1;
	
	GridWorldLocation nextLocation = new GridWorldLocation( locationRow, locationCol );
	
	return nextLocation;
    }
    
    public GridWorldLocation nextLocation( int agentLocationID, int action )
    {
	if( agentLocationID < 0 || agentLocationID >= NUM_LOCATIONS ) {
	    
	    System.out.println( "@SoccerGame->nextLocation: Parameter location is NULL!");
	    return null;
	}
	
	int locationRow = agentLocationID / WORLD_WIDTH;
	int locationCol = agentLocationID - locationRow * WORLD_WIDTH;
	
	return nextLocation( new GridWorldLocation( locationRow, locationCol ), action );
    }
    
    /**
     * whether an agent has reached its goal line
     * @param locID
     * @return
     */
    public boolean reachGoal( int locID, int agent )
    {
	if( agent == AGENT_A && (locID == GridWorldLocation.GOAL_A_1 || 
		locID == GridWorldLocation.GOAL_A_2) )
	    return true;
	else if( agent == AGENT_B && (locID == GridWorldLocation.GOAL_B_1 ||
		locID == GridWorldLocation.GOAL_B_2) )
	    return true;
	else 
	    return false;
    }
    
    
    /**
     * normal version of doAction
     * 
     * check again
     * do actions and set the reward
     */
    public GameState doAction( GameState gameState, GameAction jntAction, double[] rewards, 
	    int[] steps )
    {
	if( gameState == null || jntAction == null ||
	    rewards == null || rewards.length != NUM_AGENTS ) {
	    
	    System.out.println( "@SoccerGame->doAction: Something Wrong in Parameters!" );
	    return null;
	}
	
	GameState nextState = new GameState(gameState.getLocationIDs(), 
		gameState.getBallPossession());
	
	GridWorldLocation[] agentNextLocs = new GridWorldLocation[NUM_AGENTS];
	
	int[] curLocationID = new int[NUM_AGENTS];
	int[] nextLocationID = new int[NUM_AGENTS];
	curLocationID[AGENT_A] = gameState.getLocationID( AGENT_A );
	curLocationID[AGENT_B] = gameState.getLocationID( AGENT_B );
	
	/**
	 * first execut the actions in random order
	 */
	int firstAgent = new Random().nextInt(2);
	int secondAgent = (firstAgent+1) % NUM_AGENTS;
	
	//int firstAgent = actionOrder;
	//int secondAgent = (firstAgent+1) % NUM_AGENTS;
	//actionOrder = secondAgent;
	
	/**
	 * action execution for the first agent
	 */
	boolean over = false;
	agentNextLocs[firstAgent] = nextLocation( gameState.getLocationID(firstAgent), 
		jntAction.getAction(firstAgent) );
	nextLocationID[firstAgent] = agentNextLocs[firstAgent].getLocationID();
	
	//if goes into the loction of the second agent
	if( nextLocationID[firstAgent] == curLocationID[secondAgent] ) {
	    
	    //then the second agent owns the ball
	    nextState.setBallPossession( secondAgent );
	    nextState.setLocationID( firstAgent, curLocationID[firstAgent] );
	    nextLocationID[firstAgent] = curLocationID[firstAgent];
	    rewards[firstAgent] = 0;
	}
	//if reach the goal line
	else if( nextState.getBallPossession() == firstAgent && 
		reachGoal( nextLocationID[firstAgent], firstAgent) ) {
	    
	    nextState.setLocationID( firstAgent, nextLocationID[firstAgent] );
	    nextState.setLocationID( secondAgent, curLocationID[secondAgent] );
	    
	    rewards[firstAgent] = 10000.0;//10??
	    rewards[secondAgent] = -10000.0;
	    
	    over = true;
	}
	//if out of the boundary
	else if( outOfBoundary(agentNextLocs[firstAgent]) ) {
	    
	    nextState.setLocationID( firstAgent, curLocationID[firstAgent] );
	    nextLocationID[firstAgent] = curLocationID[firstAgent];
	    rewards[firstAgent] = 0;
	}
	//in other cases, just move
	else {
	    
	    nextState.setLocationID( firstAgent, nextLocationID[firstAgent] );
	    rewards[firstAgent] = 0;
	}
	
	
	/**
	 * action execution for the second agent
	 */
	agentNextLocs[secondAgent] = nextLocation( gameState.getLocationID(secondAgent), 
		jntAction.getAction(secondAgent) );
	nextLocationID[secondAgent] = agentNextLocs[secondAgent].getLocationID();
	if( over ) {
	    
	    return nextState;
	}
	//if goes into the next location of the first agent
	else if( nextLocationID[secondAgent] == nextLocationID[firstAgent] ) {
	    
	    //then the first agent owns the ball
	    nextState.setBallPossession( firstAgent );
	    nextState.setLocationID( secondAgent, curLocationID[secondAgent] );
	    nextLocationID[secondAgent] = curLocationID[secondAgent];
	    rewards[secondAgent] = 0;
	}
	//if reach the goal line
	else if ( nextState.getBallPossession() == secondAgent && 
		reachGoal( nextLocationID[secondAgent], secondAgent) ) {
	    
	    nextState.setLocationID( secondAgent, nextLocationID[secondAgent] );
	    
	    rewards[firstAgent] = -10000.0;
	    rewards[secondAgent] = 10000.0;
	    
	    over = true;
	}
	//if out of the boundary
	else if( outOfBoundary(agentNextLocs[secondAgent]) ) {
	    
	    nextState.setLocationID( secondAgent, curLocationID[secondAgent] );
	    nextLocationID[secondAgent] = curLocationID[secondAgent];
	    rewards[secondAgent] = 0;
	}
	//in other cases, just move
	else {
	    
	    nextState.setLocationID( secondAgent, nextLocationID[secondAgent] );
	    rewards[secondAgent] = 0;
	}
	
	return nextState;
    }
    
    /**
    public int[] actionSet( GameState gameState, int agent )
    {
	
	if( gameState == null || agent < 0 ||
		agent >= NUM_AGENTS ) {
	    
	    System.out.println("WallGame->actionSet: Wrong Parameters!");
	    return null;
	}
	
	int curLocID = gameState.getLocationID( agent );
	int curRow = curLocID / WORLD_WIDTH;
	int curCol = curLocID - WORLD_WIDTH * curRow;
	
	boolean[] actAvail = new boolean[GameAction.NUM_ACTIONS];
	for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
	    
	    actAvail[act] = true; 
	}
	
	int availNum = GameAction.NUM_ACTIONS;
	if( curRow == 0 ) {
	    
	    actAvail[GameAction.DOWN] = false;
	    availNum -= 1;
	}
	if( curRow == WORLD_HEIGHT-1 ) {
	    
	    actAvail[GameAction.UP] = false;
	    availNum -= 1;
	}
	if( curCol == 0 ) {
	    
	    actAvail[GameAction.LEFT] = false;
	    availNum -= 1;
	}
	if( curCol == WORLD_WIDTH-1 ) {
	    
	    actAvail[GameAction.RIGHT] = false;
	    availNum -= 1;
	}
	
	int[] actSet = new int[availNum];
	int index = 0;
	for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
	    
	    if( actAvail[act] ){
		
		actSet[index] = act;
		index++;
	    }	    
	}
	
	return actSet;
    }
    */
    
    /**/
    public static boolean[] actionSet( int agent, int loc, int ballPoss )
    {
	if( loc < 0 || loc >= NUM_LOCATIONS ) {
	    
	    System.out.println("WallGame->actionSet: Wrong Parameters!");
	    return null;
	}
	
	/**/
	int curRow = loc / WORLD_WIDTH;
	int curCol = loc - WORLD_WIDTH * curRow;
	
	boolean[] actAvail = new boolean[GameAction.NUM_ACTIONS];
	for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
	    
	    actAvail[act] = true; 
	}
	
	//int availNum = GameAction.NUM_ACTIONS;
	if( curRow == 0 ) {
	    
	    actAvail[GameAction.DOWN] = false;
	    //availNum -= 1;
	}
	if( curRow == WORLD_HEIGHT-1 ) {
	    
	    actAvail[GameAction.UP] = false;
	    //availNum -= 1;
	}
	
	if( agent == AGENT_A ) {
	    
	    if( curCol == 0 ) {
		    
		actAvail[GameAction.LEFT] = false;
		//availNum -= 1;
	    }
	    if( curCol == WORLD_WIDTH-1 ) {
		    
		if( ballPoss == agent && (curRow == GridWorldLocation.GOAL_ROW_1 ||
			curRow == GridWorldLocation.GOAL_ROW_2) ) {
		    
		    actAvail[GameAction.RIGHT] = true;
		}
		else {
		    
		    actAvail[GameAction.RIGHT] = false;
		    //availNum -= 1;
		}
	    }
	}
	//agent B
	else {
	    
	    if( curCol == WORLD_WIDTH-1 ) {
		
		actAvail[GameAction.RIGHT] = false;
		//availNum -= 1;
	    }
	    if( curCol == 0 ) {
		
		if( ballPoss == agent && (curRow == GridWorldLocation.GOAL_ROW_1 ||
			curRow == GridWorldLocation.GOAL_ROW_2) ) {
		    
		    actAvail[GameAction.LEFT] = true;
		}
		else {
		    
		    actAvail[GameAction.LEFT] = false;
		    //availNum -= 1;
		}
	    }
	}

	//return new boolean[]{true,true,true,true,true};
	
	return actAvail;
    }
    
    
    public boolean outOfBoundary( int row, int col )
    {
	if( row < 0 || col < 0 || 
	    row >= WORLD_HEIGHT || 	
		col >= WORLD_WIDTH ) {
	    
	    return true;
	}
	else 
	    return false;
    }
    
    public boolean outOfBoundary( GridWorldLocation location )
    {
	if( location == null )
	    return true;
	
	int row = location.getRow();
	int col = location.getCol();
	
	if( row < 0 || col < 0 || 
	    row >= WORLD_HEIGHT || 
	    	col >= WORLD_WIDTH ) {
	    return true;	
	}
	else 
	    return false;
    }
    
    
    /**
     * one test contains several algorithms run
     */
    public void oneTest()
    {
	int[] algTypes = new int[]{ //MARL.MINIMAXQ, 
		//MARL.MINIMAXQ_TRANS, 
		MARL.HAMMQ };

	
	for( int algIndex = 0; algIndex < algTypes.length; algIndex++ ) {
	    
	    
	    int algType = algTypes[algIndex];
	    
	    oneRun( algType, false );
	}
    }
    
    /**
     * one run contains one algorithm's several episodes 
     */
    public void oneRun( int algType, boolean isCentral )
    {
	
	int LOOP = 10;
	
	/**
	 * choose the algorithm
	 */
	String algStr = MARL.ALG_STRINGS[algType];
	
	
	/**
	 * for recording steps
	 */
	double[][] allGoals = new double[NUM_AGENTS][matchNum];
	for( int agent = 0; agent < SoccerGame.NUM_AGENTS; agent++ )
	    for( int ep = 0; ep < matchNum; ep++ ) {
		
		allGoals[agent][ep] = 0;
	    }
	
	/**
	 * for recording time duration
	 */
	long[] durTimes = new long[matchNum];
	    
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
	    MARL agent0 = createMARL( algType, 0 );
	    //MARL agent1 = createMARL( algType, 1 );
	    MARL agent1 = createMARL( MARL.MINIMAXQ, 1);
	    
	    
	    /**/
	    if( isCentral )
		agent.gameStarted(loop);
	    else {
		
		agent0.gameStarted(loop);
		//agent1.gameStarted(loop);
	    }
	    
	    
	    double[][] retArray = new double[matchNum][SoccerGame.NUM_AGENTS];
	    for( int ep = 0; ep < matchNum; ep++ ) {
		    
		long startTime = System.nanoTime();
		    
		
		if( isCentral )
		    retArray[ep] = oneMatchCentral( agent );
		else
		    retArray[ep] = oneMatch( agent0, agent1 );
		   
		long endTime = System.nanoTime();
		durTimes[ep] = endTime - startTime;
		   
		for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
		    
		    allGoals[agentIndex][ep] += retArray[ep][agentIndex];
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
	    for( int ep = 0; ep < matchNum; ep++ ) {
		
		timeWriter.write( durTimes[ep] + ", ");
	    }
	    timeWriter.close();
	    
	    
	    timeWriter = new BufferedWriter(new FileWriter("./" + algStr+ "_allTime.txt"));
	    timeWriter.write(""+((overTime-beginTime)/1000000000.0/LOOP));
	    timeWriter.close();
	    
	    
	    //write steps
	    /**
	    for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
		
		BufferedWriter stepsWriter = new BufferedWriter(new FileWriter("./" + algStr+"_agent" + agentIndex + ".csv"));
		for( int ep = 0; ep < episodesNum; ep++ ) {
		    
		    stepsWriter.write( (stepNums[agentIndex][ep] / (double)LOOP) + ", ");
		}
		stepsWriter.close();
	    }
	    */
	    
	    
	    //write rewards
	    /**/
	    for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
		
		BufferedWriter rewardsWriter = new BufferedWriter(new FileWriter("./goals_" + algStr+"_agent" + agentIndex + ".csv"));
		for( int ep = 0; ep < matchNum; ep++ ) {
		    
		    rewardsWriter.write( allGoals[agentIndex][ep] / LOOP + ", ");
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
    public double[] oneMatch( MARL agent0, MARL agent1 )
    {
	/**
	 * for recording goals
	 */
	double[] retArray = new double[NUM_AGENTS];
	
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    retArray[agent] = 0.0;
	}
	
	
	for( int gameCount = 1; gameCount <= 10; gameCount++ ) {
	    
	    /**
	     * init the states
	     */
	    int gameStep = 0;
	    GameState gameState = new GameState( agentInitLocs, new Random().nextInt(NUM_AGENTS) );
		
	    /**
	     * compute the available action set for each agent
	     * get the action expected taken by each agent
	     */
	    GameAction jntAction0 = agent0.updateQ( null, null, null, gameState );
	    GameAction jntAction1 = agent1.updateQ( null, null, null, gameState );
		
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
		
	    while( !gameOver( gameState ) && gameStep < 50 ) {
		    
		gameStep++;
		
		/**
		 * epsilon-greedy and get the action to be taken actually
		 */
		    
		jntAction.setAction( 0, agent0.epsilonGreedy(gameState, jntAction0.getAction(0)) );
		jntAction.setAction( 1, agent1.epsilonGreedy(gameState, jntAction1.getAction(1)) );
		//jntAction.setAction( 1, jntAction1.getAction(1) );
		    
		    
		/**
		 * observe the next state and get the rewards
		 */
		GameState nextState = doAction( gameState, jntAction, rewards, null );
		
		    
		if( !gameOver( nextState ) ) {
		    
		    /**
		     * update Q-values
		     */
		    jntAction0 = agent0.updateQ( gameState, jntAction, rewards, nextState );
		    jntAction1 = agent1.updateQ( gameState, jntAction, rewards, nextState );
		}
		
		    
		/**
		 * transfer to the next state
		 */
		gameState = null; //??
		gameState = nextState;
		
	    }
	    
	    /**
	     * if an agent goals in the current game
	     */
	    if( gameOver( gameState ) ) {
		    
		//the one has the ball goals
		retArray[gameState.getBallPossession()] += 1.0;
	    }
	}
	
	return retArray;
    }
    
    /**
     * 
     * @return two arrays, one for steps, one for rewards
     */
    public double[] oneMatchCentral( MARL agent )
    {
	/**
	 * for recording goals
	 */
	double[] retArray = new double[NUM_AGENTS];
	
	for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
	    
	    retArray[agentIndex] = 0.0;
	}
	
	for( int gameCount = 1; gameCount <= 10; gameCount++ ) {
	    
	    /**
	     * init the states
	     */
	    int gameStep = 0;
	    GameState gameState = new GameState( agentInitLocs, new Random().nextInt(NUM_AGENTS) );
		
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
		
	    while( !gameOver( gameState ) && gameStep < 50 ) {
		    
		gameStep++;
		
		/**
		 * epsilon-greedy and get the action to be taken actually
		 */
		jntAction = agent.epsilonGreedy(gameState, jntAction);
		    
		    
		/**
		 * observe the next state and get the rewards
		 */
		GameState nextState = doAction( gameState, jntAction, rewards, null );
		    
		    
		/**
		 * update Q-values
		 */
		GameAction nextAction = agent.updateQ( gameState, jntAction, rewards, nextState );
		    
		/**
		 * transfer to the next state
		 */
		gameState = null; //??
		gameState = nextState;
		jntAction = null; //??
		jntAction = nextAction;
		
	    }
	    
	    /**
	     * if an agent goals in the current game
	     */
	    if( gameOver( gameState ) ) {
		    
		//the one has the ball goals
		retArray[gameState.getBallPossession()] += 1.0;
	    }
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
	    System.out.println("Progress:" + numberFormat.format(100 * (double) ep / matchNum) + "%");
	else if( ep % 100 == 0)
	{
	    System.out.println("Progress:" + numberFormat.format(100 * (double) ep / matchNum) + "%");
	}
    }
    
    private MARL createMARL( int alg, int agent )
    {
	switch( alg )
	{
	case MARL.MINIMAXQ:
	    return new MiniMaxQ( agent );
	case MARL.MINIMAXQ_TRANS:
	    return new MiniMaxQTrans( agent );
	case MARL.HAMMQ:
	    return new HAMMQ( agent );
	default:
	    return new MARL( agent );
	}
    }
    
    
    /**
     * @param args
     */
    public static void main(String[] args)
    {
	// TODO Auto-generated method stub
	
	SoccerGame soccerGame = new SoccerGame();
	
	soccerGame.oneTest();

    }
}
