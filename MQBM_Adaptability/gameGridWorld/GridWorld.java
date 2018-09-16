package gameGridWorld;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Random;


import algorithms.CenCEQ;
import algorithms.CenNashQ;
import algorithms.DCEQ;
import algorithms.DecenCEQ;
import algorithms.ECEQ;
import algorithms.MARL;
import algorithms.MQBM;
import algorithms.NSCP;
import algorithms.NegoQ;
import algorithms.PCEQ;
import algorithms.UCEQ;
import algorithms.WoLFPHC;


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
    public static final int episodesNum = 10000;
    
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
		
		rewards[agent] = 100.0 / ((double) steps[agent]);
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
		
		rewards[agent] = 100.0 / ((double) steps[agent]);
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
	int[] algTypes_0 = new int[]{ MARL.MQBM };
	
	int[] algTypes_1 = new int[]{ MARL.NASHQ };
	//int[] algTypes_1 = new int[]{ MARL.NEGOQ, MARL.MQBM, MARL.NSCP, MARL.WOLFPHC, MARL.pCEQ, MARL.NASHQ };
	
	for( int alg_0 = 0; alg_0 < algTypes_0.length; alg_0++ ) {
	    for( int alg_1 = 0; alg_1 < algTypes_1.length; alg_1++ ) {
		
		int algType_0 = algTypes_0[alg_0];
		int algType_1 = algTypes_1[alg_1];
		
		if( algType_0 == MARL.NASHQ && 
			algType_1 == MARL.NASHQ )
		    continue;
		
		oneRun( algType_0, algType_1 );
	    }   
	}
    }
    
    /**
     * one run contains one algorithm's several episodes 
     */
    public void oneRun( int algType, boolean isCentral )
    {
	
	int LOOP = 1;//50;
	
	/**
	 * choose the algorithm
	 */
	String algStr = MARL.ALG_STRINGS[algType];
	
	
	/**
	 * for recording steps
	 */
	int[][] stepNums = new int[NUM_AGENTS][episodesNum];
	for( int agent = 0; agent < GridWorld.NUM_AGENTS; agent++ )
	    for( int ep = 0; ep < episodesNum; ep++ )
		stepNums[agent][ep] = 0; 
	
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
	    
	    for( int ep = 0; ep < episodesNum; ep++ ) {
		    
		long startTime = System.nanoTime();
		    
		int[] steps = null;
		   
		if( isCentral )
		    steps = oneEpisodeCentral( agent );
		//else if( algType == MARL.NEGOQ )
		    //steps = oneEpisodeNego( (NegoQ)agent0, (NegoQ)agent1 );
		else
		    steps = oneEpisode( agent0, agent1 );
		   
		long endTime = System.nanoTime();
		durTimes[ep] = endTime - startTime;
		   
		stepNums[0][ep] += steps[0];
		stepNums[1][ep] += steps[1];
		   
		//release memories?
		steps = null;
		   
		/**
		 * display the progress
		 */
		//displayProgress( ep+1 );
		   
	    }
	}
	
	long overTime = System.nanoTime();
	
	/**
	 * write the steps and times into files
	 */
	try
	{
	    /**
	     * write times
	     */
	    BufferedWriter timeWriter = new BufferedWriter(new FileWriter("./" + algStr+ "_time.csv"));
	    for( int ep = 0; ep < episodesNum; ep++ ) {
		
		timeWriter.write( durTimes[ep] + ", ");
	    }
	    timeWriter.close();
	    
	    timeWriter = new BufferedWriter(new FileWriter("./" + algStr+ "_allTime.txt"));
	    timeWriter.write(""+((overTime-beginTime)/1000000000.0));
	    timeWriter.close();
	    
	    /**
	     * write steps
	     */
	    for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
		
		BufferedWriter stepsWriter = new BufferedWriter(new FileWriter("./" + algStr+"_agent" + agentIndex + ".csv"));
		for( int ep = 0; ep < episodesNum; ep++ ) {
		    
		    stepsWriter.write( (int)((double) stepNums[agentIndex][ep] / LOOP) + ", ");
		}
		stepsWriter.close();

	    }
	}
	catch (IOException e)
	{
	    e.printStackTrace();
	}
    }
    
    /**
     * for different types of agents
     */
    public void oneRun( int algType_0, int algType_1 )
    {
	
	int LOOP = 1;
	
	/**
	 * choose the algorithm
	 */
	String algStr_0 = MARL.ALG_STRINGS[algType_0];
	String algStr_1 = MARL.ALG_STRINGS[algType_1];
	
	
	/**
	 * for recording steps
	 */
	int[][] stepNums = new int[NUM_AGENTS][episodesNum];
	for( int agent = 0; agent < GridWorld.NUM_AGENTS; agent++ )
	    for( int ep = 0; ep < episodesNum; ep++ )
		stepNums[agent][ep] = 0; 
	
	/**
	 * we also need to record the belief of MQBM agents
	 */
	double[][][] belief_0 = new double[episodesNum][NUM_LOCATIONS][NUM_LOCATIONS]; 
	double[][][] belief_1 = new double[episodesNum][NUM_LOCATIONS][NUM_LOCATIONS];
	for( int ep = 0; ep < episodesNum; ep++ )
	    for( int s1 = 0; s1 < NUM_LOCATIONS; s1++ )
		for( int s2 = 0; s2 < NUM_LOCATIONS; s2++ ) {
		    
		    belief_0[ep][s1][s2] = 0;
		    belief_1[ep][s1][s2] = 0; 
		}
	
	/**
	 * for recording time duration
	 */
	long[] durTimes = new long[episodesNum];
	    
	//for the time of the hole process
	long beginTime = System.nanoTime();
		
	for( int loop = 1; loop <= LOOP; loop++ ) {
	
	    System.out.println("Algorithm: "+algStr_0+" vs "+algStr_1+" the "+loop+"-th loop===========");
	    
		
	    /**
	     * for decentralized algorithms
	     */
	    MARL agent0 = createMARL( algType_0, 0 );
	    MARL agent1 = createMARL( algType_1, 1 );
	    
	    for( int ep = 0; ep < episodesNum; ep++ ) {
		    
		long startTime = System.nanoTime();
		    
		int[] steps = null;
		   
		steps = oneEpisode( agent0, agent1 );
		   
		long endTime = System.nanoTime();
		durTimes[ep] = endTime - startTime;
		   
		stepNums[0][ep] += steps[0];
		stepNums[1][ep] += steps[1];
		   
		//release memories?
		steps = null;
		
		
		/**
		 * record belief
		 */
		if( algType_0 == MARL.MQBM ) {
		    
		    MQBM mqbm0 = (MQBM) agent0;
		    double[][] b0 = mqbm0.getBeliefValues();
		    for( int s1 = 0; s1 < NUM_LOCATIONS; s1++ )
			for( int s2 = 0; s2 < NUM_LOCATIONS; s2++ )
			    belief_0[ep][s1][s2] += b0[s1][s2];
		}
		if( algType_1 == MARL.MQBM ) {
		    
		    MQBM mqbm1 = (MQBM) agent1;
		    double[][] b1 = mqbm1.getBeliefValues();
		    for( int s1 = 0; s1 < NUM_LOCATIONS; s1++ )
			for( int s2 = 0; s2 < NUM_LOCATIONS; s2++ )
			    belief_1[ep][s1][s2] += b1[s1][s2];
		}
		   
		/**
		 * display the progress
		 */
		displayProgress( ep+1 );
		   
	    }
	}
	
	long overTime = System.nanoTime();
	
	/**
	 * write the steps and times into files
	 */
	try
	{
	    /**
	     * write times
	     *
	    BufferedWriter timeWriter = new BufferedWriter(new FileWriter("./" + algStr+ "_time.csv"));
	    for( int ep = 0; ep < episodesNum; ep++ ) {
		
		timeWriter.write( durTimes[ep] + ", ");
	    }
	    timeWriter.close();
	    
	    timeWriter = new BufferedWriter(new FileWriter("./" + algStr+ "_allTime.txt"));
	    timeWriter.write(""+((overTime-beginTime)/1000000000.0));
	    timeWriter.close();
	    */
	    
	    /**
	     * write steps
	     */
	    BufferedWriter stepsWriter = new BufferedWriter(
		    new FileWriter("./" + algStr_0+"_"+algStr_1+"_0"+".csv"));
	    for( int ep = 0; ep < episodesNum; ep++ ) {
		    
		stepsWriter.write( (int)((double) stepNums[0][ep] / LOOP) + ", ");
	    }
	    stepsWriter.close();
	    
	    stepsWriter = new BufferedWriter(
		    new FileWriter("./" + algStr_0+"_"+algStr_1+"_1"+".csv"));
	    for( int ep = 0; ep < episodesNum; ep++ ) {
		    
		stepsWriter.write( (int)((double) stepNums[1][ep] / LOOP) + ", ");
	    }
	    stepsWriter.close();
	    
	    if( algType_0 == MARL.MQBM ) {
		
		for( int s1 = 0; s1 < NUM_LOCATIONS; s1++ )
		    for( int s2 = 0; s2 < NUM_LOCATIONS; s2++ ) {
			
			BufferedWriter beliefWriter = new BufferedWriter(
				new FileWriter("./belief"+algStr_0+"_"+algStr_1+"_"+s1+s2+"_0.csv"));
		    
			for( int ep = 0; ep < episodesNum; ep++ ) {
			    
			    beliefWriter.write(belief_0[ep][s1][s2] / LOOP+",");
			}
			
			beliefWriter.close();
		    }
		
		
	    }
	    if( algType_1 == MARL.MQBM ) {
		
		for( int s1 = 0; s1 < NUM_LOCATIONS; s1++ )
		    for( int s2 = 0; s2 < NUM_LOCATIONS; s2++ ) {
			
			BufferedWriter beliefWriter = new BufferedWriter(
				new FileWriter("./belief"+algStr_0+"_"+algStr_1+"_"+s1+s2+"_1.csv"));
		    
			for( int ep = 0; ep < episodesNum; ep++ ) {
			    
			    beliefWriter.write(belief_1[ep][s1][s2] / LOOP+",");
			}
			beliefWriter.close();
		    }
	    }
	    
	}
	catch (IOException e)
	{
	    e.printStackTrace();
	}
    }
    
    public int[] oneEpisode( MARL agent0, MARL agent1 )
    {
	
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
	    
	    for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
		
		if( gameState.getLocationID(agent) != agentGoals[agent] )
		    stepNums[agent] = stepNums[agent] + 1;
	    }
	    
	    /**
	     * observe the next state and get the rewards
	     */
	    GameState nextState = doAction( gameState, jntAction, rewards, stepNums );
	    
	    
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
	
	return stepNums;
    }
    
    
    public int[] oneEpisodeCentral( MARL agent )
    {
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
		
		if( gameState.getLocationID(agentIndex) != agentGoals[agentIndex] )
		    stepNums[agentIndex] = stepNums[agentIndex] + 1;
	    }
	    
	    /**
	     * observe the next state and get the rewards
	     */
	    GameState nextState = doAction( gameState, jntAction, rewards, stepNums );
	    
	    
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
	
	return stepNums;
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
	    return new CenNashQ();
	case MARL.NSCP:
	    return new NSCP( agent );
	case MARL.MQBM:
	    return new MQBM( agent );
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
	
	gridWorld.oneTest();

    }
}
