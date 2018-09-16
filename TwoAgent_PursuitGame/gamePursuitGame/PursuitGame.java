package gamePursuitGame;

import gamePursuitGame.GridWorldLocation;

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
import algorithms.NegoQ;


/**
 * The definition of a grid world game
 * 
 * this is a 2-agent grid world game 
 * with a 3x3 world
 * 
 * allowing stochastic transitions
 */
public class PursuitGame
{
    /**
     * important parameters of the pursuit game
     */
    public static final int NUM_AGENTS = 2;
    public static final int NUM_LOCATIONS = 25;
    public static final int WORLD_WIDTH = 5;
    public static final int WORLD_HEIGHT = 5;
    
    /**
     * the index of prey agent
     */
    public static final int PREY_AGENT_INDEX = 0;
    
    
    /**
     * the location ID of each agent's goal
     */
    //private int[] agentGoals;
    private int[] agentInitLocs;
    
    /**
     * the number of training episodes in one game
     */
    public static final int trainEpisNum = 20000;
    
    /**
     * the number of test episodes in one game
     */
    public static final int testEpiNum = 100;
    
    public static final int LOOP = 20;
    
    
    public PursuitGame()
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
    
    public boolean gameOver( GameState gameState ) 
    {
	int[] agentLocationIDs = gameState.getLocationIDs();
	
	if( agentLocationIDs == null || 
		agentLocationIDs.length != NUM_AGENTS ) {
	    
	    System.out.println("PursuitGame->gameOver: Game Over With Error!");
	    return false;
	}
	
	/**
	 * if the prey is captured by one of the hunter agents
	 */
	int preyLocationID = agentLocationIDs[PREY_AGENT_INDEX];
	for( int agent = 1; agent < NUM_AGENTS; agent++ ) {
	    
	    if( agentLocationIDs[agent] == preyLocationID )
		return true;
	}
	
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
    public GameState doAction( GameState gameState, GameAction jntAction, double[] rewards )
    {
	if( gameState == null || jntAction == null ||
	    rewards == null || rewards.length != NUM_AGENTS ) {
	    
	    System.out.println( "@PursuitGame->doAction: Something Wrong in Parameters!" );
	    return null;
	}
	
	GameState nextState = new GameState(gameState.getLocationIDs());
	
	GridWorldLocation[] agentNextLocs = new GridWorldLocation[NUM_AGENTS];
	boolean bShouldStay[] = new boolean[NUM_AGENTS];
	
	/**
	 * if the game is already over
	 */
	if( gameOver(gameState) ) {
	    
	    System.out.println("PursuitGame->doAction: The game is over!");
	    return gameState;
	}
	
	/**
	 * first loop, init rewards
	 */
	for( int agent = 0; agent < PursuitGame.NUM_AGENTS; agent++ ) {
		
	    rewards[agent] = 0.0; 
	}
	
	/**
	 * we check whether the agents are out of boundary
	 */
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    int agentLocationID = gameState.getLocationID(agent);
	    int currentRow = agentLocationID / WORLD_WIDTH;
	    int currentCol = agentLocationID - WORLD_WIDTH * currentRow;
	    
	    bShouldStay[agent] = false;
	    
	    /**
	     * let the agent walk one step further 
	     * and get the next location
	     * 
	     * for prey agent, it has 0.5 probability to stay at the current grid
	     */
	    if( agent == 0 ) {
		
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
	     * if the next location is out of boundary 
	     * then we should set the agent back to its current position
	     * with a little penalty
	     */
	    if( outOfBoundary( agentNextLocs[agent] ) ) {
		
		bShouldStay[agent] = true;
		//rewards[agent] = -50.0;
		rewards[agent] = -5.0; //for zero-sum, no penalty??
		
		/**
		 * then set back
		 */
		agentNextLocs[agent].setRow( currentRow );
		agentNextLocs[agent].setCol( currentCol );
	    }
	}
	
	/**
	 * check the prey agent has been captured
	 */
	boolean gameOver = false;
	for( int agent = 1; agent < PursuitGame.NUM_AGENTS; agent++ ) {
	    
	    if( agentNextLocs[PREY_AGENT_INDEX].getLocationID() == 
		agentNextLocs[agent].getLocationID() ) {
		
		gameOver = true;
		break;
	    }
	}
	if( gameOver ) {
	    
	    rewards[PREY_AGENT_INDEX] = -200;
	    for( int agent = 0; agent < PursuitGame.NUM_AGENTS; agent++ )
		rewards[agent] = 200.0 / (PursuitGame.NUM_AGENTS-1);
	}
	
	/**
	 * if the game is not over, test collision
	 */
	else {
	    
	    for( int agent = 1; agent < NUM_AGENTS; agent++ ) {
		for( int agent_p = agent+1; agent_p < NUM_AGENTS; agent_p++ ) {
			

		    if( agentNextLocs[agent].getLocationID() == 
			agentNextLocs[agent_p].getLocationID() ) {
			    
			/**
			 * both agent and agent_p came to the same grid
			 */
			if( gameState.getLocationID(agent) !=  agentNextLocs[agent].getLocationID() &&
				gameState.getLocationID(agent_p) != agentNextLocs[agent_p].getLocationID() ) {
				
			    bShouldStay[agent] = true;    
			    bShouldStay[agent_p] = true;    
			    rewards[agent] = -5.0;    
			    rewards[agent_p] = -5.0;
			}
			/**
			 * agent moves and agent_p stays in this step
			 */
			else if( gameState.getLocationID(agent) !=  agentNextLocs[agent].getLocationID() ) {
				
			    bShouldStay[agent] = true;
			    rewards[agent] = -5.0;
			}
			/**
			 * agent_p moves and agent stays in this step
			 */
			else {
			    
			    bShouldStay[agent_p] = true;
			    rewards[agent_p] = -5.0; 
			}
		    }
		}
	    }	    
	}

	/**
	 * then we check distance
	 */
	int preyLocID = gameState.getLocationID( PREY_AGENT_INDEX );
	int preyRow = preyLocID / WORLD_WIDTH;
	int preyCol = preyLocID - WORLD_WIDTH * preyRow;
	for( int agent = 1; agent < PursuitGame.NUM_AGENTS; agent++ ) {
	    
	    int agentLocID = gameState.getLocationID( agent );
	    int currentRow = agentLocID / WORLD_WIDTH;
	    int currentCol = agentLocID - WORLD_WIDTH * currentRow;
	    
	    int preDis = Math.abs(currentCol-preyCol) + Math.abs(currentRow-preyRow);
	    int nextDis = agentNextLocs[agent].getDistance( agentNextLocs[PREY_AGENT_INDEX]);
	    
	    //get closer to the prey
	    if( preDis > nextDis ) {
		
		rewards[PREY_AGENT_INDEX] -= 10.0;
		rewards[agent] += 10.0;
	    }
	    //get farther to the prey
	    else if( preDis < nextDis ) {
		
		rewards[PREY_AGENT_INDEX] += 10.0;
		rewards[agent] -= 10.0;
	    }
	}
	
	
	/**
	 * then set the next state
	 */
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    if( !bShouldStay[agent] ) {
		
		nextState.setLocationID( agent, agentNextLocs[agent].getLocationID() );
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
    public void trainAlgorithms()
    {
	int[] algTypes = new int[]{ MARL.MINIMAXQ_TRANS };
	
	for( int algIndex = 0; algIndex < algTypes.length; algIndex++ ) {
	    
	    int algType = algTypes[algIndex];
	    train( algType, false );
	}
    }
    
    public void testAlgorithms()
    {
	//int[] algTypes = new int[]{ MARL.NEGOQ, MARL.uCEQ, MARL.eCEQ, MARL.pCEQ, MARL.dCEQ };
	
	int[] preyTypes = new int[]{ MARL.NEGOQ };
	int[] algTypes = new int[]{ MARL.NEGOQ };
	
	for( int preyIndex = 0; preyIndex < preyTypes.length; preyIndex++ ) {
	    
	    int preyType = preyTypes[preyIndex];
	    
	    for( int algIndex = 0; algIndex < algTypes.length; algIndex++ ) {
		    
		    
		int algType = algTypes[algIndex];
		    
		test(algType, preyType);
	    }
	}

    }
    
    /**
     * one run contains one algorithm's several episodes 
     */
    public void train( int algType, boolean isCentral )
    {
	
	int LOOP = 20;
	
	/**
	 * choose the algorithm
	 */
	String algStr = MARL.ALG_STRINGS[algType];
	
	/**
	 * for recording steps
	 */
	double[][] stepNums = new double[NUM_AGENTS][trainEpisNum];
	double[][] allRewards = new double[NUM_AGENTS][trainEpisNum];
	for( int agent = 0; agent < PursuitGame.NUM_AGENTS; agent++ )
	    for( int ep = 0; ep < trainEpisNum; ep++ ) {
		
		stepNums[agent][ep] = 0; 
		allRewards[agent][ep] = 0;
	    }
	
	
	/**
	 * for recording time duration
	 */
	long[] durTimes = new long[trainEpisNum];
	    
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
	    
	    
	    //the process of training
	    for( int ep = 0; ep < trainEpisNum; ep++ ) {
		    
		long startTime = System.nanoTime();
		    
		//record the episode steps
		//int steps;
		double[][] retArray = null;
		if( isCentral )
		    retArray = trainingEpisodeCentral( agent );
		else if( algType == MARL.NEGOQ )
		    retArray = trainingEpisodeNego( (NegoQ)agent0, (NegoQ)agent1 );
		else
		    retArray = trainingEpisode( agent0, agent1 );
		   
		long endTime = System.nanoTime();
		durTimes[ep] = endTime - startTime;
		   
		//stepNums[ep] += steps;
		
		for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
		    
		    stepNums[agentIndex][ep] += retArray[0][agentIndex];
		    allRewards[agentIndex][ep] += retArray[1][agentIndex];
		}
		   
		//release memories?
		retArray = null;
		
		/**
		 * display the progress
		 */
		displayProgress( ep+1 );
	    }
	    //then store the Q-tables in file
	    /**/
	    if( isCentral ) {
		
		//agent.storeQValues( algStr );
		//agent.storePolicy();
	    }
	    else {
		
		//agent0.storeQValues( algStr );
		//agent1.storeQValues( algStr );
		//agent0.storePolicy();
		//agent1.storePolicy();
	    }
	    
	}
	long overTime = System.nanoTime();
	/**
	 * write the steps and times into files
	 */
	try
	{
	    /**
	     * write training times
	     */
	    BufferedWriter timeWriter = new BufferedWriter(new FileWriter("./" + algStr+ "_time.csv"));
	    for( int ep = 0; ep < trainEpisNum; ep++ ) {
		
		timeWriter.write( durTimes[ep] + ", ");
	    }
	    timeWriter.close();
	    
	    timeWriter = new BufferedWriter(new FileWriter("./" + algStr+ "_allTime.txt"));
	    timeWriter.write(""+((overTime-beginTime)/1000000000.0));
	    timeWriter.close();
	    
	    /**
	     * write training steps
	     */
	    for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
		
		BufferedWriter stepsWriter = new BufferedWriter(new FileWriter("./" + algStr+"_agent" + agentIndex + ".csv"));
		for( int ep = 0; ep < trainEpisNum; ep++ ) {
		    
		    stepsWriter.write( (int)((double) stepNums[agentIndex][ep] / LOOP) + ", ");
		}
		stepsWriter.close();
	    }
	    
	    //write rewards
	    for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
		
		BufferedWriter rewardsWriter = new BufferedWriter(new FileWriter("./rewd_" + algStr+"_agent" + agentIndex + ".csv"));
		for( int ep = 0; ep < trainEpisNum; ep++ ) {
		    
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
    
    
    public double[][] trainingEpisode( MARL agent0, MARL agent1 )
    {
	
	//int stepNums = 0;
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
		
		stepNums[agent] = stepNums[agent] + 1; 
		retArray[0][agent] = retArray[0][agent] + 1;
	    }
	    
	    /**
	     * observe the next state and get the rewards
	     */
	    GameState nextState = doAction( gameState, jntAction, rewards );
	    
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
    
    
    public double[][] trainingEpisodeCentral( MARL agent )
    {
	//int stepNums = 0;
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
	    
	    //record the steps
	    for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
		
		stepNums[agentIndex] = stepNums[agentIndex] + 1; 
		retArray[0][agentIndex] = retArray[0][agentIndex] + 1;
	    }
	    
	    /**
	     * observe the next state and get the rewards
	     */
	    GameState nextState = doAction( gameState, jntAction, rewards );
	    
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
    public double[][] trainingEpisodeNego( NegoQ agent0, NegoQ agent1 )
    {
	//int stepNums = 0;
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
	 * through negotiation
	 */
	GameAction optAction = NegoQ.negotiation( agent0, agent1, gameState );
	
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
	    
	    //record the steps
	    for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
		
		stepNums[agent] = stepNums[agent] + 1; 
		retArray[0][agent] = retArray[0][agent] + 1;
	    }
	    
	    /**
	     * epsilon-greedy and get the action to be taken actually
	     */
	    jntAction.setAction( 0, agent0.epsilonGreedy(optAction.getAction(0)) );
	    jntAction.setAction( 1, agent1.epsilonGreedy(optAction.getAction(1)) );
	    
	    /**
	     * observe the next state and get the rewards
	     */
	    GameState nextState = doAction( gameState, jntAction, rewards );
	    
	    //record the rewards
	    for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
		
		retArray[1][agentIndex] = retArray[1][agentIndex] + rewards[agentIndex];  
	    }
	    
	    /**
	     * get the optimal action int the next state 
	     * and update Q-values
	     */
	    optAction = NegoQ.negotiation( agent0, agent1, nextState );
	    agent0.updateQ_NegoQ( gameState, jntAction, rewards, nextState, optAction );
	    agent1.updateQ_NegoQ( gameState, jntAction, rewards, nextState, optAction );
	    
	    /**
	     * transfer to the next state
	     */
	    gameState = null; //??
	    gameState = nextState;
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
    
    
    
    public void test( int algType, int preyType )
    {
	
	/**
	 * first create the agent
	 * only two agents, one prey and one hunter
	 * 
	 * no central algorithms
	 */
	MARL agent0 = createMARL( preyType, 0 );
	MARL agent1 = createMARL( algType, 1 );
	
	String preyStr = MARL.ALG_STRINGS[preyType];
	String algStr = MARL.ALG_STRINGS[algType];
	
	
	/**
	 * read the Q-table from file
	 */
	agent0.readQValues( preyStr, 0 );
	agent0.readQValues( preyStr, 1 );
	agent1.readQValues( algStr, 0 );
	agent1.readQValues( algStr, 1 );
	
	/**
	 * read policy from file
	 */
	agent0.readPolicy();
	agent1.readPolicy();
	
	/**
	 * for recording test steps
	 */
	int[] testSteps = new int[testEpiNum];
	
	//the process of test
	for( int test = 0; test < testEpiNum; test++ ) {
			   
	    testSteps[test] = testEpisode( agent0, agent1 );
	}
	
	/**
	 * write the test results to file
	 */
	try
	{
	    /**
	     * write test steps
	     */
	    double sumTestSteps = 0.0;
	    double squareSum = 0.0;
	    for( int test = 0; test < testEpiNum; test++ )
		sumTestSteps += testSteps[test];
	    double averTestSteps = sumTestSteps / testEpiNum;
	    for( int test = 0; test < testEpiNum; test++ ) {
		
		double diff = testSteps[test] - averTestSteps;
		squareSum += diff * diff;
	    }
	    double standardError = Math.sqrt( squareSum / testEpiNum );
	    BufferedWriter testWriter = new BufferedWriter(new FileWriter("./"+algStr+" vs "+preyStr+".txt"));
	    testWriter.write("averStep: "+averTestSteps);
	    testWriter.newLine();
	    testWriter.write("standardError: "+standardError);
	    testWriter.close();
	}
	catch (Exception e) {
	    e.printStackTrace();
	}
    }
    
    
    public int testEpisode( MARL preyAgent, MARL hunterAgent )
    {
	int stepNums = 0;
	
	/**
	 * init the states
	 */
	GameState gameState = new GameState( agentInitLocs );
	
	/**
	 * get the action expected taken by each agent
	 */
	GameAction jntAction0 = preyAgent.getAction( gameState );
	GameAction jntAction1 = hunterAgent.getAction( gameState );
	
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
	    
	    //increase the step number
	    stepNums++;
	    
	    //System.out.println("Step: "+stepNums);
	    
	    /**
	     * no epsilon-greedy since this is test
	     */
	    jntAction.setAction( 0, jntAction0.getAction(0) );
	    jntAction.setAction( 1, jntAction1.getAction(1) );
	    GameState nextState = doAction( gameState, jntAction, rewards );
	    
	    System.out.println("Prey: "+GameAction.getActionString(jntAction.getAction(0))+ 
		    " Hunter: "+GameAction.getActionString(jntAction.getAction(1)) );
	    
	    
	    /**
	     * update Q-values
	     */
	    jntAction0 = preyAgent.getAction( nextState );
	    jntAction1 = hunterAgent.getAction( nextState );
	    
	    
	    /**
	     * transfer to the next state
	     */
	    gameState = null; //??
	    gameState = nextState;
	    
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
	    System.out.println("Progress:" + numberFormat.format(100 * (double) ep / trainEpisNum) + "%");
	else if( ep % 100 == 0)
	{
	    System.out.println("Progress:" + numberFormat.format(100 * (double) ep / trainEpisNum) + "%");
	}
    }
    
    private MARL createMARL( int alg, int agent )
    {
	switch( alg )
	{
	case MARL.NEGOQ:
	    return new NegoQ( agent );
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
	
	PursuitGame purstuiGame = new PursuitGame();
	
	//first train
	purstuiGame.trainAlgorithms();
	
	
	//then test
	//purstuiGame.testAlgorithms();
    }
}
