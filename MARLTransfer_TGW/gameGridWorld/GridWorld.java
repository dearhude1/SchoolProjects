package gameGridWorld;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Random;


import algorithms.CenCEQ;
import algorithms.DCEQTransEqui;
import algorithms.DCEQTransLocQ;
import algorithms.ECEQTransEqui;
import algorithms.ECEQTransLocQ;
import algorithms.ECEQTransPolicy;
import algorithms.NashQ;
import algorithms.DCEQ;
import algorithms.DecenCEQ;
import algorithms.ECEQ;
import algorithms.MARL;
import algorithms.NashQSuppTrans;
import algorithms.NashQTransEqui;
import algorithms.NashQTransLocQ;
import algorithms.NashQTransPolicy;
import algorithms.NegoQ;
import algorithms.NegoQ_TransLocQ;
import algorithms.PCEQ;
import algorithms.PCEQTransEqui;
import algorithms.PCEQTransLocQ;
import algorithms.PCEQTransPolicy;
import algorithms.QLearning;
import algorithms.TeamQ;
import algorithms.UCEQ;
import algorithms.UCEQTransEqui;
import algorithms.UCEQTransLocQ;
import algorithms.WoLFPHC;
import algorithms.UCEQTransPolicy;


/**
 * The definition of a grid world game
 * 
 * this is a 2-agent grid world game 
 * with a 3x3 world
 * 
 * allowing stochastic transitions
 */
public class GridWorld
{
    /**
     * important parameters of the grid-world game
     */
    public static final int NUM_AGENTS = 2;
    public static final int NUM_LOCATIONS = 9;
    public static final int WORLD_WIDTH = 3;
    public static final int WORLD_HEIGHT = 3;
    
    /**
     * the location ID of each agent's goal
     */
    private int[] agentGoals;
    private int[] agentInitLocs;
    
    /**
     * the number of episodes in one game
     */
    public static final int episodesNum = 2000;//10000;//2000;//5000;//100000;
    
    public static final int LOOP = 1;
    
    
    public GridWorld()
    {
	agentGoals = new int[NUM_AGENTS];
	agentInitLocs = new int[NUM_AGENTS];
	
	/**
	 * read each agent's goal
	 */
	 try {
	     
	     BufferedReader bufReader = new BufferedReader(new FileReader("./goals.txt")); 
	     String line = "";
	     
	     int agent = 0;
	     while( (line = bufReader.readLine()) != null ) {
		 
		 if( line.length() == 0 )	 
		     continue;
		 
		 agentGoals[agent] = Integer.parseInt(line);
		 agent++;
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
    
    public boolean gameOver( GameState gameState ) 
    {
	int[] agentLocationIDs = gameState.getLocationIDs();
	
	if( agentLocationIDs == null || 
		agentLocationIDs.length != NUM_AGENTS ) {
	    
	    System.out.println("GridWorld->gameOver: Game Over With Error!");
	    return false;
	}
	
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    if( agentLocationIDs[agent] != agentGoals[agent] )
		return false;
	}
	
	return true;
    }
    
    public boolean isGoal( int agent, int locationID )
    {
	if( agent < 0 || agent > NUM_AGENTS ||
	    locationID < 0 || locationID > NUM_LOCATIONS ) {
	    
	    System.out.println( "@GridWorld->isGoal: Wrong Parameters!");
	    return false;
	}
	
	if( locationID == agentGoals[agent] )
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
    
    
    public GameState doAction( GameState gameState, GameAction jntAction, double[] rewards, 
	    int[] steps ) 
    {
	
	if( agentGoals[0] == 8 && agentGoals[1] == 6 )
	    return doAction_Normal(gameState, jntAction, rewards, steps);
	else if( agentGoals[0] == 7 && agentGoals[1] == 7 )
	    return doAction_Stochastic(gameState, jntAction, rewards, steps);
	else {
	    
	    System.out.println("@GridWorld->doAction: unavailable goals!");
	    return null;
	}
    }
    
    /**
     * normal version of doAction
     * 
     * check again
     * do actions and set the reward
     */
    public GameState doAction_Normal( GameState gameState, GameAction jntAction, double[] rewards, 
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
	
	/**
	 * first loop, we check whether the agents:
	 * are out of boundary
	 * or reach their goals
	 */
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    int agentLocationID = gameState.getLocationID(agent);
	    int currentRow = agentLocationID / WORLD_WIDTH;
	    int currentCol = agentLocationID - WORLD_WIDTH * currentRow;
	    
	    bShouldStay[agent] = false;
	    
	    /**
	     * if the agent has already reached the goal
	     */
	    if( isGoal( agent, agentLocationID ) ) {
		
		rewards[agent] = 0.0;
		bShouldStay[agent] = true; 
		continue;
	    }
	    
	    /**
	     * let the agent walk one step further 
	     * and get the next location
	     */
	    agentNextLocs[agent] = nextLocation( agentLocationID, jntAction.getAction(agent) );
	    if( outOfBoundary( agentNextLocs[agent] ) ) {
		
		bShouldStay[agent] = true;
		//rewards[agent] = -50.0;
		rewards[agent] = -10.0;
		
		/**
		 * then set back
		 */
		agentNextLocs[agent].setRow( currentRow );
		agentNextLocs[agent].setCol( currentCol );
	    }
	    else if( isGoal(agent, agentNextLocs[agent].getLocationID()) ) {
		
		rewards[agent] = 100.0;// / ((double) steps[agent]);
		//rewards[agent] = 100.0 / ((double) steps[agent]);
	    }
	}
	
	/**
	 * second loop for testing collision
	 * 
	 * dependence!!!
	 */
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    /**
	     * no collision will happen is the agent reached the goal 
	     * or reaches the goal in this step
	     */
	    if( isGoal( agent, gameState.getLocationID(agent) ) ||
		isGoal( agent, agentNextLocs[agent].getLocationID() ) )
		continue;
	    
	    for( int agent_p = agent+1; agent_p < NUM_AGENTS; agent_p++ ) {
		
		if( isGoal( agent_p, gameState.getLocationID(agent_p) ) ||
		    isGoal( agent_p, agentNextLocs[agent_p].getLocationID() ) )
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
			rewards[agent] = -10.0;    
			rewards[agent_p] = -10.0;
		    }
		    /**
		     * if agent moves in this step
		     */
		    else if( gameState.getLocationID(agent) !=  agentNextLocs[agent].getLocationID() ) {
			
			bShouldStay[agent] = true;
			rewards[agent] = -10.0;
		    }
		    /**
		     * if agent_p moves in this step
		     */
		    else {
			
			bShouldStay[agent_p] = true;
			rewards[agent_p] = -10.0; 
		    }
		}
	    }
	}
	
	/**
	 * then set the next state
	 */
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    if( !bShouldStay[agent] ) {
		
		nextState.setLocationID( agent, agentNextLocs[agent].getLocationID() );
		
		/**
		 * if the next location is the goal?!
		 */
		if( !isGoal(agent, agentNextLocs[agent].getLocationID()) )
			rewards[agent] = -1.0;
	    }
	}
	
	return nextState;
    }
    
    
    /**
     * @return: return the next state and corresponding reward
     */
    public int[] doAction_SingleAgent( int agentIndex, int locState, int locAction, int step )
    {
	
	/**
	 * the parameters?
	 */
	
	/**
	 * 2-element array:
	 * the first element is the next local state
	 * the second element is the reward
	 */
	int[] retArray = new int[2];
	
	
	int nextLocState = locState;
	GridWorldLocation agentNextLoc = new GridWorldLocation();
	boolean bShouldStay = false;
	
	/**
	 * first loop, we check whether the agent is out of boundary
	 * or reach its goal
	 */
	int currentRow = locState / WORLD_WIDTH;
	int currentCol = locState - WORLD_WIDTH * currentRow;
	    
	/**
	 * if the agent has already reached the goal
	 */
	if( isGoal( agentIndex, locState ) ) {
		
	    //the reward
	    retArray[1] = 0;
	    bShouldStay = true; 
	}
	else {
	    
	    /**
	     * let the agent walk one step further 
	     * and get the next location
	     */
	    agentNextLoc = nextLocation( locState, locAction );
	    if( outOfBoundary( agentNextLoc ) ) {
		
		bShouldStay = true;
		//the reward
		retArray[1] = -10;
		
		/**
		 * then set back
		 */
		agentNextLoc.setRow( currentRow );
		agentNextLoc.setCol( currentCol );
	    }
	    else if( isGoal( agentIndex, agentNextLoc.getLocationID()) ) {
		
		retArray[1] = 100;// / ((double) steps[agent]);
	    } 
	}

	/**
	 * then set the next state
	 */
	if( !bShouldStay ) {
		
	    nextLocState = agentNextLoc.getLocationID();
		
	    /**
	     * if the next location is the goal?!
	     */
	    if( !isGoal( agentIndex, agentNextLoc.getLocationID()) )
		retArray[1] = -1;
	}
	retArray[0] = nextLocState;
	
	return retArray;
    }
    
    public GameState doAction_Stochastic( GameState gameState, GameAction jntAction, double[] rewards, 
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
	
	/**
	 * first loop, we check whether the agents:
	 * are out of boundary
	 * or reach their goals
	 */
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    int agentLocationID = gameState.getLocationID(agent);
	    int currentRow = agentLocationID / WORLD_WIDTH;
	    int currentCol = agentLocationID - WORLD_WIDTH * currentRow;
	    
	    bShouldStay[agent] = false;
	    
	    /**
	     * if the agent has already reached the goal
	     */
	    if( isGoal( agent, agentLocationID ) ) {
		
		rewards[agent] = 0.0;
		bShouldStay[agent] = true; 
		continue;
	    }
	    
	    /**
	     * let the agent walk one step further and get the next location
	     */
	    //there is a barrier between grid0 and grid3
	    if( (agentLocationID == 0 && jntAction.getAction(0) == GameAction.UP) || 
		    (agentLocationID == 3 && jntAction.getAction(0) == GameAction.DOWN) ) {
		    
		if( new Random().nextDouble() < 0.5 ) {
			
		    bShouldStay[agent] = true;
		    agentNextLocs[agent] = new GridWorldLocation( currentRow, currentCol );
		}
		else
		    agentNextLocs[agent] = nextLocation( agentLocationID, jntAction.getAction(agent) );
	    }
	    //there is a barrier between grid2 and grid5
	    else if( (agentLocationID == 2 && jntAction.getAction(1) == GameAction.UP) || 
		    (agentLocationID == 5 && jntAction.getAction(1) == GameAction.DOWN) ) {
		    
		if( new Random().nextDouble() < 0.5 ) {
			
		    bShouldStay[agent] = true;
		    agentNextLocs[agent] = new GridWorldLocation( currentRow, currentCol );
		}
		else
		    agentNextLocs[agent] = nextLocation( agentLocationID, jntAction.getAction(agent) );
	    }
	    else
		agentNextLocs[agent] = nextLocation( agentLocationID, jntAction.getAction(agent) );
		
	    
	    /**
	     * check the availability of the next locs
	     */
	    if( outOfBoundary( agentNextLocs[agent] ) ) {
		
		bShouldStay[agent] = true;
		rewards[agent] = -10.0;
		
		/**
		 * then set back
		 */
		agentNextLocs[agent].setRow( currentRow );
		agentNextLocs[agent].setCol( currentCol );
	    }
	    else if( isGoal(agent, agentNextLocs[agent].getLocationID()) ) {
		
		rewards[agent] = 100.0;
		//rewards[agent] = 100.0 / ((double) steps[agent]);
	    }
	}
	
	/**
	 * second loop for testing collision
	 * 
	 * dependence!!!
	 */
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    /**
	     * no collision will happen is the agent reached the goal 
	     * or reaches the goal in this step
	     */
	    if( isGoal( agent, gameState.getLocationID(agent) ) ||
		isGoal( agent, agentNextLocs[agent].getLocationID() ) )
		continue;
	    
	    for( int agent_p = agent+1; agent_p < NUM_AGENTS; agent_p++ ) {
		
		if( isGoal( agent_p, gameState.getLocationID(agent_p) ) ||
		    isGoal( agent_p, agentNextLocs[agent_p].getLocationID() ) )
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
			rewards[agent] = -10.0;    
			rewards[agent_p] = -10.0;
		    }
		    /**
		     * if agent moves in this step
		     */
		    else if( gameState.getLocationID(agent) !=  agentNextLocs[agent].getLocationID() ) {
			
			bShouldStay[agent] = true;
			rewards[agent] = -10.0;
		    }
		    /**
		     * if agent_p moves in this step
		     */
		    else {
			
			bShouldStay[agent_p] = true;
			rewards[agent_p] = -10.0; 
		    }
		}
	    }
	}
	
	/**
	 * then set the next state
	 */
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    if( !bShouldStay[agent] ) {
		
		nextState.setLocationID( agent, agentNextLocs[agent].getLocationID() );
		
		/**
		 * if the next location is the goal?!
		 */
		if( !isGoal(agent, agentNextLocs[agent].getLocationID()) )
			rewards[agent] = -1.0;
	    }
	}
	
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
	int[] algTypes = new int[]{ 
		//MARL.uCEQ, 
		//MARL.uCEQ_TransLocQ, 
		//MARL.eCEQ, 
		//MARL.eCEQ_TransLocQ, 
		//MARL.pCEQ, 
		//MARL.pCEQ_TransLocQ, 
		//MARL.dCEQ, 
		//MARL.dCEQ_TransLocQ,
		//MARL.NASHQ,
		//MARL.NashQ_TransLocQ,
		MARL.NEGOQ,
		MARL.NEGOQ_TransLocQ,
		//MARL.uCEQ_TransPolicy,
		//MARL.eCEQ_TransPolicy,
		//MARL.pCEQ_TransPolicy
		
		//MARL.NashQ_TransPolicy
		};

	
	//int[] algTypes = new int[]{ MARL.dCEQ };
	
	for( int algIndex = 0; algIndex < algTypes.length; algIndex++ ) {
	    
	    
	    int algType = algTypes[algIndex];
	    
	    if( algType == MARL.uCEQ || algType == MARL.eCEQ ||
		    algType == MARL.pCEQ || algType == MARL.NASHQ ||
		    algType == MARL.NASHQ_TransEqui || algType == MARL.NASHQ_SUPP_TRANS || 
		    algType == MARL.uCEQ_TransEqui || algType == MARL.eCEQ_TransEqui ||
		    algType == MARL.pCEQ_TransEqui || 
		    algType == MARL.TeamQ || algType == MARL.NashQ_TransLocQ ||
		    algType == MARL.uCEQ_TransLocQ || 
		    algType == MARL.eCEQ_TransLocQ || 
		    algType == MARL.pCEQ_TransLocQ || 
		    algType == MARL.NashQ_TransPolicy ||
		    algType == MARL.uCEQ_TransPolicy || 
		    algType == MARL.eCEQ_TransPolicy ||
		    algType == MARL.pCEQ_TransPolicy )
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
	
	int LOOP = 500;
	
	/**
	 * choose the algorithm
	 */
	String algStr = MARL.ALG_STRINGS[algType];
	
	
	/**
	 * for recording steps
	 */
	double[][] stepNums = new double[NUM_AGENTS][episodesNum];
	double[][] allRewards = new double[NUM_AGENTS][episodesNum];
	for( int agent = 0; agent < GridWorld.NUM_AGENTS; agent++ )
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
	    
	    
	    
	    double[][][] retArray = new double[episodesNum][2][GridWorld.NUM_AGENTS];
	    for( int ep = 0; ep < episodesNum; ep++ ) {
		    
		long startTime = System.nanoTime();
		    
		
		if( isCentral )
		    retArray[ep] = oneEpisodeCentral( agent );
		else if( algType == MARL.NEGOQ || 
			algType == MARL.NEGOQ_TransLocQ )
		    retArray[ep] = oneEpisodeNego( agent0, agent1, algType );
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
    
    
    public void oneRun_SingleAgent( int agentIndex )
    {
	int LOOP = 50;
	
	/**
	 * choose the algorithm
	 */
	String algStr = "Q-learning";
	
	
	/**
	 * for recording steps
	 */
	double[] stepNums = new double[episodesNum];
	double[] allRewards = new double[episodesNum];
	for( int ep = 0; ep < episodesNum; ep++ ) {
		
	    stepNums[ep] = 0; 
	    allRewards[ep] = 0;
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
	    QLearning qAgent = new QLearning( agentIndex );
		
	    
	   //qAgent.gameStarted(loop);
	    
	    
	    double[][] retArray = new double[episodesNum][2];
	    for( int ep = 0; ep < episodesNum; ep++ ) {
		    
		long startTime = System.nanoTime();
		    
		
		retArray[ep] = oneEpisode_SingleAgent( qAgent, agentIndex );
		   
		long endTime = System.nanoTime();
		
		//recording the data
		durTimes[ep] = endTime - startTime;
		stepNums[ep] += retArray[ep][0];
		allRewards[ep] += retArray[ep][1];
		      
		/**
		 * display the progress
		 */
		displayProgress( ep+1 );   
	    }
	    
	    //release?
	    retArray = null;
	    
	    //one loop finished
	    qAgent.gameFinished(loop);
	    
	}
	
	long overTime = System.nanoTime();
	
	/**
	 * write the steps and times into files
	 */
	try
	{
	    
	    //write times
	    BufferedWriter timeWriter = new BufferedWriter(new FileWriter("./" + algStr+ "_time.csv"));
	    for( int ep = 0; ep < episodesNum; ep++ ) {
		
		timeWriter.write( durTimes[ep] + ", ");
	    }
	    timeWriter.close();
	    
	    timeWriter = new BufferedWriter(new FileWriter("./" + algStr+ "_allTime.txt"));
	    timeWriter.write(""+((overTime-beginTime)/1000000000.0/LOOP));
	    timeWriter.close();
	    
	    
	    //write steps
	    BufferedWriter stepsWriter = new BufferedWriter(new FileWriter("./" + algStr+"_agent" + agentIndex + ".csv"));
	    for( int ep = 0; ep < episodesNum; ep++ ) {
		    
		stepsWriter.write( (stepNums[ep] / (double)LOOP) + ", ");
	    }
	    stepsWriter.close();
	    
	    //write rewards
	    BufferedWriter rewardsWriter = new BufferedWriter(new FileWriter("./rewd_" + algStr+"_agent" + agentIndex + ".csv"));
	    for( int ep = 0; ep < episodesNum; ep++ ) {
		    
		rewardsWriter.write( allRewards[ep] / LOOP + ", ");
	    }
	    rewardsWriter.close();
	}
	catch (IOException e)
	{
	    e.printStackTrace();
	}
	
    }
    

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
	    for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
		
		if( gameState.getLocationID(agent) != agentGoals[agent] ) {
		    
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
	    jntAction = agent.epsilonGreedy(jntAction);
	    
	    for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
		
		if( gameState.getLocationID(agentIndex) != agentGoals[agentIndex] ) {
		    
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
    
    /**
     * this is only for NegoQ learning algorithm
     */
    public double[][] oneEpisodeNego( MARL agent0, MARL agent1, int algType )
    {
	
	NegoQ negoQ_agent0 = (NegoQ)agent0;//null;
	NegoQ negoQ_agent1 = (NegoQ)agent1;//null;
	
	/**
	NegoQ_TransLocQ negoQTransQ_agent0 = null;
	NegoQ_TransLocQ negoQTransQ_agent1 = null;
	
	if( algType == MARL.NEGOQ ) {
	 
	    negoQ_agent0 = (NegoQ)agent0;
	    negoQ_agent1 = (NegoQ)agent1;
	}
	else if( algType == MARL.NEGOQ_TransLocQ ) {
	    
	    negoQTransQ_agent0 = (NegoQ_TransLocQ)agent0;
	    negoQTransQ_agent1 = (NegoQ_TransLocQ)agent1;
	}
	else {
	    
	    System.out.println("GridWorld->oneEpisodeNego: Parameter Wrong!");
	    return null;
	}
	*/
	
	
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
	 * get the action expected taken by each agent 
	 * through negotiation
	 */
	GameAction optAction = NegoQ.negotiation( negoQ_agent0, negoQ_agent1, gameState );
	/**
	if( algType == MARL.NEGOQ) {
	    
	    optAction = NegoQ.negotiation( negoQ_agent0, negoQ_agent1, gameState );
	}
	else {
	    
	    optAction = NegoQ.negotiation( negoQ_agent0, negoQ_agent1, gameState );
	}
	*/
	
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
	    jntAction.setAction( 0, agent0.epsilonGreedy(optAction.getAction(0)) );
	    jntAction.setAction( 1, agent1.epsilonGreedy(optAction.getAction(1)) );
	    
	    for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
		
		if( gameState.getLocationID(agent) != agentGoals[agent] ) {
		    
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
	     * get the optimal action int the next state 
	     * and update Q-values
	     */
	    optAction = NegoQ.negotiation( negoQ_agent0, negoQ_agent1, nextState );
	    negoQ_agent0.updateQ_NegoQ( gameState, jntAction, rewards, nextState, optAction );
	    negoQ_agent1.updateQ_NegoQ( gameState, jntAction, rewards, nextState, optAction );
	    
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
    
    
    public double[] oneEpisode_SingleAgent( QLearning qAgent, int agentIndex )
    {
	/**
	 * dimension 0: steps
	 * dimension 1: rewards
	 */
	double[] retArray = new double[2];	
	retArray[0] = 0.0;
	retArray[1] = 0.0;
	
	int stepNum = 0;
	
	
	/**
	 * init the states
	 * 
	 * random initial state?
	 */
	//int locState = agentInitLocs[agentIndex];
	Random rnd = new Random();
	int locState = agentGoals[agentIndex];
	while( locState == agentGoals[agentIndex] ) {
	    
	    locState = rnd.nextInt(GridWorld.NUM_LOCATIONS);
	}
	
	
	
	/**
	 * get the action expected taken by each agent
	 */
	int curAction = qAgent.updateQ( -1, -1, 0, locState );
	
	
	while( !isGoal( agentIndex, locState ) ) {
	    
	    /**
	     * epsilon-greedy and get the action to be taken actually
	     */
	    curAction = qAgent.epsilonGreedy(curAction);
	    
	    /**
	     * update the data
	     */
	    stepNum = stepNum + 1;
	    retArray[0] = retArray[0] + 1;
	    
	    
	    /**
	     * observe the next state and get the rewards
	     */
	    int[] array = doAction_SingleAgent( agentIndex, locState, curAction, stepNum );
	    int nextLocState = array[0];
	    int reward = array[1];
	    
	    //record the rewards
	    retArray[1] = retArray[1] + reward;
	    
	    /**
	     * update Q-values
	     */
	    int nextAction = qAgent.updateQ( locState, curAction, reward, nextLocState );
	    
	    /**
	     * transfer to the next state
	     */
	    locState = nextLocState;
	    curAction = nextAction;
	}
	
	//compute the average reward
	if( retArray[0]< 0.00000001 )
	    retArray[1] = 0.0;
	    
	else {
	    retArray[1] = retArray[1] / retArray[0]; 
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
	case MARL.NEGOQ:
	    return new NegoQ( agent );
	case MARL.WOLFPHC:
	    return new WoLFPHC( agent );
	case MARL.NASHQ:
	    return new NashQ();
	case MARL.NASHQ_TransEqui:
	    return new NashQTransEqui();
	case MARL.NASHQ_SUPP_TRANS:
	    return new NashQSuppTrans();
	case MARL.uCEQ_TransEqui:
	    return new UCEQTransEqui();
	case MARL.eCEQ_TransEqui:
	    return new ECEQTransEqui();
	case MARL.pCEQ_TransEqui:
	    return new PCEQTransEqui();
	case MARL.dCEQ_TransEqui:
	    return new DCEQTransEqui( agent );
	case MARL.TeamQ:
	    return new TeamQ();
	case MARL.uCEQ_TransLocQ:
	    return new UCEQTransLocQ();
	case MARL.eCEQ_TransLocQ:
	    return new ECEQTransLocQ();
	case MARL.pCEQ_TransLocQ:
	    return new PCEQTransLocQ();
	case MARL.dCEQ_TransLocQ:
	    return new DCEQTransLocQ( agent );
	case MARL.NashQ_TransLocQ:
	    return new NashQTransLocQ();
	case MARL.NEGOQ_TransLocQ:
	    return new NegoQ( agent );
	case MARL.uCEQ_TransPolicy:
	    return new UCEQTransPolicy();
	case MARL.eCEQ_TransPolicy:
	    return new ECEQTransPolicy();
	case MARL.pCEQ_TransPolicy:
	    return new PCEQTransPolicy();
	case MARL.NashQ_TransPolicy:
	    return new NashQTransPolicy();
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
	
	GridWorld gridWorld = new GridWorld();
	
	//multi-agent test
	gridWorld.oneTest();
	
	//single-agent test
	//gridWorld.oneRun_SingleAgent( 0 );

    }
}
