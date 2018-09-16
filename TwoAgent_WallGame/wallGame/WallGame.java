package wallGame;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Random;


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
public class WallGame
{
    /**
     * important parameters of the grid-world game
     */
    public static final int NUM_AGENTS = 2;
    public static final int NUM_LOCATIONS = 9;
    public static final int WORLD_WIDTH = 3;
    public static final int WORLD_HEIGHT = 3;
    
    public static final int ATTACKER_AGENT = 0;
    public static final int DEFENDER_AGENT = 1;
    

    
    /**
     * the location ID of each agent's goal
     */
    //private int[] agentGoals;
    private int[] agentInitLocs;
    
    /**
     * the number of episodes in one game
     */
    public static final int episodesNum = 100000;
    
    public static final int LOOP = 1;
    
    
    public WallGame()
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
     * if the attacker reaches the right column of the world, 
     * the game then is over
     */
    public boolean gameOver( GameState gameState ) 
    {
	int[] agentLocationIDs = gameState.getLocationIDs();
	
	if( agentLocationIDs == null || 
		agentLocationIDs.length != NUM_AGENTS ) {
	    
	    System.out.println("WallGame->gameOver: Game Over With Error!");
	    return false;
	}	
	
	//the condition of reaching the right column
	if( agentLocationIDs[ATTACKER_AGENT] % WORLD_WIDTH == 2) {
	    
	    return true;
	}
	else
	    return false;
    }
    
    
    public GridWorldLocation nextLocation( GridWorldLocation location, int action )
    {
	if( location == null ) {
	    
	    System.out.println( "@GridWorld->nextLocation: Parameter location is NULL!");
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
	    
	    System.out.println( "@GridWorld->nextLocation: Parameter location is NULL!");
	    return null;
	}
	
	int locationRow = agentLocationID / WORLD_WIDTH;
	int locationCol = agentLocationID - locationRow * WORLD_WIDTH;
	
	return nextLocation( new GridWorldLocation( locationRow, locationCol ), action );
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
	    
	    System.out.println( "@GameState->doAction: Something Wrong in Parameters!" );
	    return null;
	}
	
	GameState nextState = new GameState(gameState.getLocationIDs());
	
	GridWorldLocation[] agentNextLocs = new GridWorldLocation[NUM_AGENTS];
	boolean bShouldStay[] = new boolean[NUM_AGENTS];
	int[] curLocationID = new int[NUM_AGENTS];
	
	/**
	 * first loop, we check whether the agents:
	 * are out of boundary
	 * or reach their goals
	 */
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    int agentLocationID = gameState.getLocationID(agent);
	    curLocationID[agent] = agentLocationID;
	    
	    int currentRow = agentLocationID / WORLD_WIDTH;
	    int currentCol = agentLocationID - WORLD_WIDTH * currentRow;
	    
	    bShouldStay[agent] = false;
	    
	    //being out of boundary is impossible
	    /**
	     * let the agent walk one step further 
	     * and get the next location
	     */
	    agentNextLocs[agent] = nextLocation( agentLocationID, jntAction.getAction(agent) );
	}
	
	/**
	 * check the next locations and whether they should stay unchanged
	 */
	//if the same grid
	if( agentNextLocs[ATTACKER_AGENT] == agentNextLocs[DEFENDER_AGENT] ) {
	    
	    //if not diagonally
	    int curAttRow = curLocationID[ATTACKER_AGENT] / WORLD_WIDTH;
	    int curAttCol = curLocationID[ATTACKER_AGENT] - WORLD_WIDTH * curAttRow;
	    int curDefRow = curLocationID[DEFENDER_AGENT] / WORLD_WIDTH;
	    int curDefCol = curLocationID[DEFENDER_AGENT] - WORLD_WIDTH * curDefRow;
	    
	    if( Math.abs(curAttRow-curDefRow) == 1 && 
		    Math.abs(curAttCol-curDefCol) == 1 ) {
		
		bShouldStay[ATTACKER_AGENT] = false;
		bShouldStay[DEFENDER_AGENT] = true;
	    }
	    else {
		
		bShouldStay[ATTACKER_AGENT] = true;
		bShouldStay[DEFENDER_AGENT] = true;
	    }
	}
	//if exchange grid
	else if( agentNextLocs[ATTACKER_AGENT].getLocationID() == curLocationID[DEFENDER_AGENT] && 
		agentNextLocs[DEFENDER_AGENT].getLocationID() == curLocationID[ATTACKER_AGENT] ) {
	    
	    bShouldStay[ATTACKER_AGENT] = true;
	    bShouldStay[DEFENDER_AGENT] = true;
	}
	else {
	    
	    bShouldStay[ATTACKER_AGENT] = false;
	    bShouldStay[DEFENDER_AGENT] = false;
	}	    
	
	/**
	 * then execute the action and set the next state
	 */
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    if( bShouldStay[agent] )
		nextState.setLocationID( agent, curLocationID[agent] );
	    else
		nextState.setLocationID( agent, agentNextLocs[agent].getLocationID() );
	}
	
	/**
	 * then set the reward
	 */
	if( gameOver( nextState ) ) {
	    
	    rewards[ATTACKER_AGENT] = 40;
	    rewards[DEFENDER_AGENT] = 0;
	}
	else {
	    
	    rewards[ATTACKER_AGENT] = 15;
	    rewards[DEFENDER_AGENT] = 25;
	}
	
	return nextState;
    }
    
    
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
    
    public static boolean[] actionSet( int loc )
    {
	if( loc < 0 || loc >= NUM_LOCATIONS ) {
	    
	    System.out.println("WallGame->actionSet: Wrong Parameters!");
	    return null;
	}
	
	int curRow = loc / WORLD_WIDTH;
	int curCol = loc - WORLD_WIDTH * curRow;
	
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
	int[] algTypes = new int[]{ MARL.MINIMAXQ_TRANS };

	
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
	
	int LOOP = 10;//10;
	
	/**
	 * choose the algorithm
	 */
	String algStr = MARL.ALG_STRINGS[algType];
	
	
	/**
	 * for recording steps
	 */
	double[][] stepNums = new double[NUM_AGENTS][episodesNum];
	double[][] allRewards = new double[NUM_AGENTS][episodesNum];
	for( int agent = 0; agent < WallGame.NUM_AGENTS; agent++ )
	    for( int ep = 0; ep < episodesNum; ep++ ) {
		
		stepNums[agent][ep] = 0; 
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
	    MARL agent0 = createMARL( algType, 0 );
	    MARL agent1 = createMARL( algType, 1 );
	    
	    
	    /**/
	    if( isCentral )
		agent.gameStarted(loop);
	    else {
		
		agent0.gameStarted(loop);
		//agent1.gameStarted(loop);
	    }
	    
	    
	    double[][][] retArray = new double[episodesNum][2][WallGame.NUM_AGENTS];
	    for( int ep = 0; ep < episodesNum; ep++ ) {
		    
		long startTime = System.nanoTime();
		    
		
		if( isCentral )
		    retArray[ep] = oneEpisodeCentral( agent );
		else
		    retArray[ep] = oneEpisode( agent0, agent1 );
		   
		long endTime = System.nanoTime();
		durTimes[ep] = endTime - startTime;
		   
		for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
		    
		    stepNums[agentIndex][ep] += retArray[ep][0][agentIndex];
		    allRewards[agentIndex][ep] += retArray[ep][1][agentIndex];
		}
		      
		/**
		 * display the progress
		 */
		displayProgress( ep+1 );   
	    }
	    
	    
	    //release?
	    retArray = null;
	    
	    //one loop finished
	    /**/
	    if( isCentral )
		agent.gameFinished(loop);
	    else {
		
		agent0.gameFinished(loop);
		//agent1.gameFinished(loop);
	    }
	    
	    
	}
	
	long overTime = System.nanoTime();
	
	/**
	 * write the steps and times into files
	 */
	try
	{
	    
	    //write times
	    /**
	    BufferedWriter timeWriter = new BufferedWriter(new FileWriter("./" + algStr+ "_time.csv"));
	    for( int ep = 0; ep < episodesNum; ep++ ) {
		
		timeWriter.write( durTimes[ep] + ", ");
	    }
	    timeWriter.close();
	    */
	    
	    BufferedWriter timeWriter = new BufferedWriter(new FileWriter("./" + algStr+ "_allTime.txt"));
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
	    /**
	    for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
		
		BufferedWriter rewardsWriter = new BufferedWriter(new FileWriter("./rewd_" + algStr+"_agent" + agentIndex + ".csv"));
		for( int ep = 0; ep < episodesNum; ep++ ) {
		    
		    rewardsWriter.write( allRewards[agentIndex][ep] / LOOP + ", ");
		}
		rewardsWriter.close();
	    }
	    */
	    
	    
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
    public double[][] oneEpisode( MARL agent0, MARL agent1 )
    {
	/**
	 * dimension 0: steps
	 * dimension 1: rewards
	 */
	double[][] retArray = new double[2][NUM_AGENTS];
	
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    retArray[0][agent] = 0.0;
	    retArray[1][agent] = 0.0;
	}
	
	
	int[] stepNums = new int[NUM_AGENTS];
	for( int agent = 0; agent < NUM_AGENTS; agent++ )
	    stepNums[agent] = 0;
	
	
	/**
	 * init the states
	 */
	GameState gameState = new GameState( agentInitLocs );
	
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
	
	while( !gameOver( gameState ) ) {
	    
	    /**
	     * epsilon-greedy and get the action to be taken actually
	     */
	    
	    jntAction.setAction( 0, agent0.epsilonGreedy(gameState, jntAction0.getAction(0)) );
	    jntAction.setAction( 1, agent1.epsilonGreedy(gameState, jntAction1.getAction(1)) );
	    
	    //record the steps
	    for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
		    
		stepNums[agent] = stepNums[agent] + 1; 
		retArray[0][agent] = retArray[0][agent] + 1;
	    }
	    
	    /**
	     * observe the next state and get the rewards
	     */
	    GameState nextState = doAction( gameState, jntAction, rewards, stepNums );
	    
	    //record the rewards
	    for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
		
		retArray[1][agent] = retArray[1][agent] + rewards[agent];  
	    }
	    
	    /**
	     * update Q-values
	     */
	    jntAction0 = agent0.updateQ( gameState, jntAction, rewards, nextState );
	    jntAction1 = agent1.updateQ( gameState, jntAction, rewards, nextState );
	    
	    /**
	     * transfer to the next state
	     */
	    gameState = null; //??
	    gameState = nextState;
	    
	}
	
	//compute the average reward
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    if( retArray[0][agent] < 0.00000001 )
		retArray[1][agent] = 0.0;
	    
	    else {
		retArray[1][agent] = retArray[1][agent] / retArray[0][agent]; 
	    }
	}
	
	return retArray;
    }
    
    /**
     * 
     * @return two arrays, one for steps, one for rewards
     */
    public double[][] oneEpisodeCentral( MARL agent )
    {
	/**
	 * dimension 0: steps
	 * dimension 1: rewards
	 */
	double[][] retArray = new double[2][NUM_AGENTS];
	
	for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
	    
	    retArray[0][agentIndex] = 0.0;
	    retArray[1][agentIndex] = 0.0;
	}
	
	int[] stepNums = new int[NUM_AGENTS];
	for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ )
	    stepNums[agentIndex] = 0;
	
	/**
	 * init the states
	 */
	GameState gameState = new GameState( agentInitLocs );
	
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
	    jntAction = agent.epsilonGreedy(gameState, jntAction);
	    
	    for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
				    
		stepNums[agentIndex] = stepNums[agentIndex] + 1;
		retArray[0][agentIndex] = retArray[0][agentIndex] + 1;
	    }
	    
	    /**
	     * observe the next state and get the rewards
	     */
	    GameState nextState = doAction( gameState, jntAction, rewards, stepNums );
	    
	    //record the rewards
	    for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
		
		retArray[1][agentIndex] = retArray[1][agentIndex] + rewards[agentIndex];  
	    }
	    
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
	
	//compute the average reward
	for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
	    
	    if( retArray[0][agentIndex] < 0.00000001 )
		retArray[1][agentIndex] = 0.0;
	    
	    else {
		retArray[1][agentIndex] = retArray[1][agentIndex] / retArray[0][agentIndex]; 
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
	case MARL.MINIMAXQ:
	    return new MiniMaxQ( agent );
	case MARL.MINIMAXQ_TRANS:
	    return new MiniMaxQTrans( agent );
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
	
	WallGame gridWorld = new WallGame();
	
	gridWorld.oneTest();

    }
}
