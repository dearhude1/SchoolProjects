package multiGridWorld;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;


import algorithms.CenCEQ;
import algorithms.DCEQTrans;
import algorithms.ECEQTrans;
import algorithms.NashQ;
import algorithms.DCEQ;
import algorithms.DecenCEQ;
import algorithms.ECEQ;
import algorithms.MARL;
import algorithms.NashQSuppTrans;
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
public class MultiGridWorld
{
    /**
     * important parameters of the grid-world game
     */
    public static final int NUM_AGENTS = 2;
    public static final int NUM_LOCATIONS = 9;
    public static final int WORLD_WIDTH = 3;
    public static final int WORLD_HEIGHT = 3;
    public static final int WORLD_NUM = 2;
    
    /**
     * the location ID of each agent's goal
     */
    private int[][] agentGoals;
    private int[][] agentInitLocs;
    
    /**
     * the number of episodes in one game
     */
    public static final int episodesNum = 20000;//50000;//50000;//5000;//100000;
    //500 for similarity 
    //50000 for error
    
    public static final int LOOP = 1;
    
    
    public MultiGridWorld()
    {
	agentGoals = new int[WORLD_NUM][NUM_AGENTS];
	agentInitLocs = new int[WORLD_NUM][NUM_AGENTS];
	
	/**
	 * read each agent's goal
	 */
	 try {
	     
	     BufferedReader bufReader = new BufferedReader(new FileReader("./goals.txt")); 
	     String line = "";
	     
	     int worldIndex = 0;
	     while( (line = bufReader.readLine()) != null ) {
		 
		 if( line.length() == 0 )	 
		     continue;
		 
		 int commaIndex = line.indexOf(",");
		 agentGoals[worldIndex][0] = Integer.parseInt(line.substring(0, commaIndex));
		 agentGoals[worldIndex][1] = Integer.parseInt(line.substring(commaIndex+1));
		 
		 
		 worldIndex++;
	     }
	     bufReader.close();
	 }
	 catch(IOException ioe){
	     
	     ioe.printStackTrace();
	 }
	 
	 /**
	  * read the initial locations
	  */
	 try {
	     
	     BufferedReader bufReader = new BufferedReader(new FileReader("./initLocations.txt")); 
	     String line = "";
	     
	     int worldIndex = 0;
	     while( (line = bufReader.readLine()) != null ) {
		 
		 if( line.length() == 0 )	 
		     continue;
		 
		 int commaIndex = line.indexOf(",");
		 agentInitLocs[worldIndex][0] = Integer.parseInt(line.substring(0, commaIndex));
		 agentInitLocs[worldIndex][1] = Integer.parseInt(line.substring(commaIndex+1));
		 
		 worldIndex++;
	     }
	     bufReader.close();
	 }
	 catch(IOException ioe){
	     
	     ioe.printStackTrace();
	 }
    }
    
    public int[] getInitLocationIDs( int wldIndex )
    {
	if( wldIndex < 0 || wldIndex >= WORLD_NUM ) {
	    
	    System.out.println("getInitLocationIDs: Wrong Parameter!");
	    return null;
	}
	
	return agentInitLocs[wldIndex];
    }
    
    public boolean gameOver( GameState gameState ) 
    {
	
	int worldIndex = gameState.getWorldIndex();
	 
	if( worldIndex != WORLD_NUM-1 )
	    return false;
	
	int[] agentLocationIDs = gameState.getLocationIDs();
	
	if( agentLocationIDs == null || 
		agentLocationIDs.length != NUM_AGENTS ) {
	    
	    System.out.println("GridWorld->gameOver: Game Over With Error!");
	    return false;
	}
	
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    if( agentLocationIDs[agent] != agentGoals[worldIndex][agent] )
		return false;
	}
	
	return true;
    }
    
    public boolean isGoal( int agent, int locationID, int worldIndex )
    {
	if( agent < 0 || agent > NUM_AGENTS ||
	    locationID < 0 || locationID > NUM_LOCATIONS || 
	    worldIndex < 0 || worldIndex >= WORLD_NUM ) {
	    
	    System.out.println( "@MultiGridWorld->isGoal: Wrong Parameters!");
	    return false;
	}
	
	if( locationID == agentGoals[worldIndex][agent] )
	    return true;
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
     * check again
     * do actions and set the reward
     */
    public GameState doAction( GameState gameState, GameAction jntAction, 
	    double[] rewards, int[] steps )
    {
	if( gameState == null || jntAction == null ||
	    rewards == null || rewards.length != NUM_AGENTS ) {
	    
	    System.out.println( "@GameState->doAction: Something Wrong in Parameters!" );
	    return null;
	}
	
	int curWorldIndex = gameState.getWorldIndex();
	int nextWorldIndex = curWorldIndex;
	
	/**
	 * the next state
	 * which should be returned
	 */
	GameState nextState = new GameState();
	GridWorldLocation[] agentNextLocs = new GridWorldLocation[NUM_AGENTS];
	
	boolean bShouldStay[] = new boolean[NUM_AGENTS];
	for( int agent = 0; agent < NUM_AGENTS; agent++ )
	    bShouldStay[agent] = false; 
	
	/**
	 * one step further
	 * care about:
	 * reach the last goal or
	 * out of boundary
	 */
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    int agentLocationID = gameState.getLocationID(agent);
	    int currentRow = agentLocationID / WORLD_WIDTH;
	    int currentCol = agentLocationID - WORLD_WIDTH * currentRow;
	    
	    /**
	     * if agent has already reached its goal of the last world
	     */
	    if( curWorldIndex == WORLD_NUM-1 && 
		    isGoal( agent, agentLocationID, curWorldIndex ) ) {
		
		agentNextLocs[agent] = new GridWorldLocation( currentRow, currentCol );
		
		bShouldStay[agent] = true;
		rewards[agent] = 0.0;
	    }
	    else {
		
		agentNextLocs[agent] = nextLocation( agentLocationID, jntAction.getAction(agent) );
		
		if( outOfBoundary( agentNextLocs[agent] )) {
		    
		    agentNextLocs[agent].setRow( currentRow );
		    agentNextLocs[agent].setCol( currentCol );
		    bShouldStay[agent] = true;
		    rewards[agent] = -10;
		}
		else if( curWorldIndex == WORLD_NUM-1 && 
			isGoal(agent, agentNextLocs[agent].getLocationID(), curWorldIndex) ) {
			
		    rewards[agent] = 1000.0;
		}
	    }
	}
	
	/**
	 * if game is over in the next state 
	 * or the game transfers to the next world 
	 */
	if( curWorldIndex != WORLD_NUM-1 ) {
	    
	    boolean transfer = true;
	    
	    for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
		
		if( !isGoal(agent,  agentNextLocs[agent].getLocationID(), curWorldIndex) ) {
		    
		    transfer = false;
		    break;
		}
	    }
	    
	    if( transfer ) {
		
		nextWorldIndex = curWorldIndex + 1;
		
		for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
		    
		    int locID_NextWorld = agentInitLocs[nextWorldIndex][agent];
		    agentNextLocs[agent].setLocation( locID_NextWorld );
		    
		    nextState.setLocationID( agent, locID_NextWorld);
		    rewards[agent] = 100;
		}
		
		nextState.setWolrdIndex( nextWorldIndex );
		return nextState;
	    }
	}
	else {
	    
	    boolean gameOver = true;
	    
	    for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
		
		if( !isGoal(agent, agentNextLocs[agent].getLocationID(), curWorldIndex) ) {
		    
		    gameOver = false;
		    break;
		}
	    }
	    
	    if( gameOver ) {
		
		for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
		    
		    nextState.setLocationID(agent, agentNextLocs[agent].getLocationID());
		}
		nextState.setWolrdIndex( nextWorldIndex );
		return nextState;
	    }
	}
	
	
	/**
	 * here are normal moves
	 * collision happens?
	 */
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    /**
	     * no collision will happen is the agent reached the goal 
	     * or reaches the goal in this step
	     */
	    if( curWorldIndex == WORLD_NUM-1 && 
		    (isGoal( agent, gameState.getLocationID(agent), curWorldIndex ) ||
		isGoal( agent, agentNextLocs[agent].getLocationID(), curWorldIndex) ) )
		continue;
	    
	    for( int agent_p = agent+1; agent_p < NUM_AGENTS; agent_p++ ) {
		
		if( curWorldIndex == WORLD_NUM-1 && 
			(isGoal( agent_p, gameState.getLocationID(agent_p), curWorldIndex ) ||
		    isGoal( agent_p, agentNextLocs[agent_p].getLocationID(), curWorldIndex)) )
		    continue;

		if( agentNextLocs[agent].getLocationID() == 
		    	agentNextLocs[agent_p].getLocationID() ) {
		    
		    /**
		     * both agent and agent_p came to the same grid
		     */
		    if( gameState.getLocationID(agent) !=  agentNextLocs[agent].getLocationID() &&
			gameState.getLocationID(agent_p) != agentNextLocs[agent_p].getLocationID() ) {
			
			bShouldStay[agent] = true;    
			bShouldStay[agent_p] = true;    
			
			agentNextLocs[agent].setLocation(gameState.getLocationID(agent));
			agentNextLocs[agent_p].setLocation(gameState.getLocationID(agent_p));
			
			rewards[agent] = -10.0;    
			rewards[agent_p] = -10.0;
		    }
		    /**
		     * if agent moves in this step
		     */
		    else if( gameState.getLocationID(agent) !=  agentNextLocs[agent].getLocationID() ) {
			
			bShouldStay[agent] = true;
			
			agentNextLocs[agent].setLocation(gameState.getLocationID(agent));
			
			rewards[agent] = -10.0;
		    }
		    /**
		     * if agent_p moves in this step
		     */
		    else {
			
			bShouldStay[agent_p] = true;
			
			agentNextLocs[agent_p].setLocation(gameState.getLocationID(agent_p));
			
			rewards[agent_p] = -10.0; 
		    }
		}
	    }
	}
	
	/**
	 * then set the next state
	 */
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    nextState.setLocationID( agent, agentNextLocs[agent].getLocationID() );
	    
	    if( !bShouldStay[agent] ) {
		
		//nextState.setLocationID( agent, agentNextLocs[agent].getLocationID() );
		
		/**
		 * if the next location is the goal?!
		 */
		if( !(curWorldIndex == WORLD_NUM-1 && 
			isGoal(agent, agentNextLocs[agent].getLocationID(), curWorldIndex)) )
		    rewards[agent] = -1;
	    }
	}
	nextState.setWolrdIndex( nextWorldIndex );
	
	return nextState;
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
	int[] algTypes = new int[]{ //MARL.uCEQ, MARL.eCEQ, MARL.pCEQ, 
		//MARL.uCEQ_TRANS, MARL.eCEQ_TRANS, MARL.pCEQ_TRANS, MARL.dCEQ_TRANS
		MARL.NASHQ_TRANS//,
		//MARL.NASHQ
		//MARL.dCEQ
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
	double[][] stepNums = new double[NUM_AGENTS][episodesNum];
	double[][] allRewards = new double[NUM_AGENTS][episodesNum];
	for( int agent = 0; agent < MultiGridWorld.NUM_AGENTS; agent++ )
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
	    
	    
	    
	    double[][][] retArray = new double[episodesNum][2][MultiGridWorld.NUM_AGENTS];
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
	    
	    //check optimality of this loop
	    /**
	    boolean badLoop = false;
	    for( int ep = 99990; ep < episodesNum; ep++ ) {
		if( retArray[ep][0][0]+retArray[ep][0][1] > 8 ) {
		    badLoop = true;
		    break;
		}
	    }
	    if( badLoop ) 
	    {
		for( int ep = 0; ep < episodesNum; ep++ ) {
		    for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
		    
			stepNums[agentIndex][ep] -= retArray[ep][0][agentIndex];
			allRewards[agentIndex][ep] -= retArray[ep][1][agentIndex];
		    }
		}
		
		loop--;
	    }
	    */
	    
	    
	    
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
	    
	    
	    //write steps
	    /**/
	    for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
		
		BufferedWriter stepsWriter = new BufferedWriter(new FileWriter("./" + algStr+"_agent" + agentIndex + ".csv"));
		for( int ep = 0; ep < episodesNum; ep++ ) {
		    
		    stepsWriter.write( (stepNums[agentIndex][ep] / (double)LOOP) + ", ");
		}
		stepsWriter.close();
	    }
	    
	    
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
	GameState gameState = new GameState( 0, agentInitLocs[0] );
	
	/**
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
	    
	    jntAction.setAction( 0, agent0.epsilonGreedy(jntAction0.getAction(0)) );
	    jntAction.setAction( 1, agent1.epsilonGreedy(jntAction1.getAction(1)) );
	    
	    //record the steps
	    int curWorldIndex = gameState.getWorldIndex();
	    for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
		
		if( ! (curWorldIndex == WORLD_NUM && 
			gameState.getLocationID(agent) != agentGoals[curWorldIndex][agent]) ) {
		    
		    stepNums[agent] = stepNums[agent] + 1; 
		    retArray[0][agent] = retArray[0][agent] + 1;
		}
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
	GameState gameState = new GameState( 0, agentInitLocs[0] );
	
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
	    
	    int curWorldIndex = gameState.getWorldIndex();
	    for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
		
		if( ! (curWorldIndex == WORLD_NUM && 
			gameState.getLocationID(agentIndex) != agentGoals[curWorldIndex][agentIndex]) ) {
		    
		    stepNums[agentIndex] = stepNums[agentIndex] + 1; 
		    retArray[0][agentIndex] = retArray[0][agentIndex] + 1;
		}
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
	case MARL.NASHQ_SUPP_TRANS:
	    return new NashQSuppTrans();
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
    
    
    /**
     * @param args
     */
    public static void main(String[] args)
    {
	// TODO Auto-generated method stub
	
	MultiGridWorld gridWorld = new MultiGridWorld();
	
	gridWorld.oneTest();

    }
}
