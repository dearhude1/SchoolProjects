package gameGridWorld;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Random;

import algorithms.CoordinateLearning;
import algorithms.DCEQAbsGame;
import algorithms.DCEQTransLocQ;
import algorithms.DCEQTransModel;
import algorithms.DCEQTransPolicy;
import algorithms.ECEQAbsGame;
import algorithms.ECEQTransLocQ;
import algorithms.ECEQTransModel;
import algorithms.ECEQTransPolicy;
import algorithms.NashQ;
import algorithms.DCEQ;
import algorithms.ECEQ;
import algorithms.MARL;
import algorithms.NashQAbsGame;
import algorithms.NashQTransLocQ;
import algorithms.NashQTransModel;
import algorithms.NashQTransPolicy;
import algorithms.NegoQ;
import algorithms.NegoQAbsGame;
import algorithms.NegoQTransLocQ;
import algorithms.NegoQTransModel;
import algorithms.PCEQ;
import algorithms.PCEQAbsGame;
import algorithms.PCEQTransLocQ;
import algorithms.PCEQTransModel;
import algorithms.PCEQTransPolicy;
import algorithms.QL;
import algorithms.QLearning;
import algorithms.Rmax;
import algorithms.UCEQ;
import algorithms.UCEQAbsGame;
import algorithms.UCEQTransLocQ;
import algorithms.UCEQTransModel;
import algorithms.UCEQTransPolicy;


/**
 * The definition of a grid world game
 * 
 * this is a 2-agent grid world game 
 * with a 3x3 world
 * 
 * allowing stochastic transitions
 */
public class SparseGridWorld
{
    /**
     * important parameters of the grid-world game
     */
    //public static final int NUM_AGENTS = 2;
    //public static final int NUM_LOCATIONS = 9;
    //public static final int WORLD_WIDTH = 3;
    //public static final int WORLD_HEIGHT = 3;
    
    /**
     * the location ID of each agent's goal
     */
    //private int[] agentGoals;
    //private int[] agentInitLocs;
    
    /**
     * the number of episodes in one game
     */
    public static final int episodesNum = 2000;//10000;//2000;//5000;//100000;
    
    public static final int LOOP = 1;
    
    /**
     * the number of agents
     *  
     * should be set from the class Map
     */
    public static int NUM_AGENTS = 0;
    
    /**
     * the number of cells in this map
     * including wall cells
     */
    public static int NUM_CELLS = 0;
    
    /**
     * the number of all valid cells
     */
    public static int NUM_VALID_CELLS = 0;
    
    /**
     * map width and height
     */
    private int mapWidth = 0;
    private int mapHeight = 0;
    
    /**
     * game map
     */
    private Map gameMap = null;
    
    
    /**
     * all valid states of the map
     */
    private static ArrayList<GameState> allStates = null;
    
    /**
     * all joint actions
     */
    private static ArrayList<GameAction> allJointActions = null;
    
    /**
     * for generating probabilities and random numbers
     */
    Random random = null;
    
    /**
     * max step of one episode
     */
    private int maxStep = 250;
    
    public SparseGridWorld( String mapFileName, String mapInfoFileName )
    {

	/**
	 * create the map
	 */
	gameMap = new Map(mapFileName, mapInfoFileName);
	
	/**
	 * set the number of agents in this game
	 */
	NUM_AGENTS = gameMap.getNumAgents();
	
	/**
	 * set the number of cells and valid cells
	 */
	NUM_CELLS = gameMap.getNumCells();
	NUM_VALID_CELLS = gameMap.getValidCellNum();
	
	/**
	 * set map width and height
	 */
	mapWidth = gameMap.getMapWidth();
	mapHeight = gameMap.getMapHeight();
	
	/**
	 * tell class GameState how many agents there are in this map 
	 */
	GameState.setNumAgents( NUM_AGENTS );
	
	/**
	 * tell class GameAction how many agents there are in this map 
	 */
	GameAction.setNumAgents( NUM_AGENTS );
	
	/**
	 * tell class GameAction how many agents there are in this map 
	 */
	StateActionPair.setNumAgents( NUM_AGENTS );
	
	/**
	 * tell class CoordinateLearning the map height and map width
	 */
	CoordinateLearning.setMapWidthHeight( mapWidth,  mapHeight );
	
	/**
	 * generate all valid states
	 */
	generateValidStates();
	
	/**
	 * generate all joint actions
	 */
	generateAllJointActions();
	
	/**
	 * create a random class
	 */
	random = new Random();
    }
    
    
    
    
    public boolean gameOver( GameState gameState ) 
    {
	int[] agentLocationIDs = gameState.getLocationIDs();
	
	if( agentLocationIDs == null || 
		agentLocationIDs.length != NUM_AGENTS ) {
	    
	    System.out.println("SparseGridWorld->gameOver: Game Over With Error!");
	    return false;
	}
	
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    if( agentLocationIDs[agent] != gameMap.getGoalCellIndex(agent) )
		return false;
	}
	
	//System.out.println("Game is over!! "+agentLocationIDs[0]+", "+agentLocationIDs[1]);
	
	return true;
    }
    
    public boolean isGoal( int agent, int locationID )
    {
	if( agent < 0 || agent > NUM_AGENTS ||
	    locationID < 0 || locationID > NUM_CELLS ) {
	    
	    System.out.println( "@SparseGridWorld->isGoal: Wrong Parameters!");
	    return false;
	}
	
	if( locationID == gameMap.getGoalCellIndex( agent ) )
	    return true;
	else
	    return false;
    }
    
    
    public boolean isGoal( int agent, GridWorldLocation gridLoc )
    {
	if( agent < 0 || agent > NUM_AGENTS || 
		gridLoc == null) {
		    
	    System.out.println( "@SparseGridWorld->isGoal: Wrong Parameters!");
	    return false;
	}
	
	int row = gridLoc.getRow();
	int col = gridLoc.getCol();
	
	return isGoal( agent, gameMap.getCellIndex(row, col) );
    }	
    
    /**
     * now the cell is arranged from left to right
     * and from top to bottom
     * 
     * therefore, we have:
     * UP: row_next = row - 1
     * DOWN: row_next = row + 1
     * LEFT: col_next = col - 1
     * RIGHT: col_next = col + 1
     */
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
	    locationRow -= 1;
	else if( action == GameAction.LEFT )
	    locationCol -= 1;
	else if( action == GameAction.DOWN )
	    locationRow += 1;
	
	GridWorldLocation nextLocation = new GridWorldLocation( locationRow, locationCol );
	
	return nextLocation;
    }
    
    public GridWorldLocation nextLocation( int agentLocationID, int action )
    {
	if( agentLocationID < 0 || agentLocationID >= NUM_CELLS ) {
	    
	    System.out.println( "@GridWorld->nextLocation: Parameter location is NULL!");
	    return null;
	}
	
	int row = gameMap.getCellRow( agentLocationID );
	int col = gameMap.getCellCol( agentLocationID );
	
	return nextLocation( new GridWorldLocation( row, col ), action );
    }
    
    
    
    //check again!!
    public GameState doAction( GameState gameState, GameAction jntAction, 
	    double[] rewards, int[] steps )
    {
	if( gameState == null || jntAction == null ||
	    rewards == null || rewards.length != NUM_AGENTS ) {
	    
	    System.out.println( "@GameState->doAction: Something Wrong in Parameters!" );
	    return null;
	}
	
	GameState nextState = new GameState(gameState.getLocationIDs());
	
	GridWorldLocation[] agentNextLocs = new GridWorldLocation[NUM_AGENTS];
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    agentNextLocs[agent] = new GridWorldLocation( );
	}
	
	//whether each agent should stay
	boolean bShouldStay[] = new boolean[NUM_AGENTS];
	boolean nowInGoal[] = new boolean[NUM_AGENTS];
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    bShouldStay[agent] = false;
	    nowInGoal[agent] = false;
	}
	
	/**
	 * first loop, we check whether the agents 
	 * have already reached their goals
	 */
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    int currentLocation = gameState.getLocationID(agent);
	    
	    if( isGoal( agent, currentLocation ) ) {
		
		bShouldStay[agent] = true; 
		agentNextLocs[agent].setRow( gameMap.getCellRow(currentLocation) );
		agentNextLocs[agent].setCol( gameMap.getCellCol(currentLocation) );
		
		//then set the agent to its initial cell
		nowInGoal[agent] = true;
		
		//agentNextLocs[agent].setRow( gameMap.getInitCellIndex(agent) );
		//agentNextLocs[agent].setCol( gameMap.getInitCellIndex(agent) );
		
		//set the reward??
		rewards[agent] = 0.0;
	    }
	}
	
	/**
	 * second step, take one step 
	 * 
	 * with 0.8 probability success
	 */
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    //the agent has already reached its goal
	    if( bShouldStay[agent] )//|| nowInGoal[agent] )
		continue;
	    
	    int agentLocationID = gameState.getLocationID(agent);
	    agentNextLocs[agent] = nextLocation( agentLocationID, jntAction.getAction(agent) );
	    
	    if( random.nextDouble() < 0.2 ) {
		
		bShouldStay[agent] = true;
		agentNextLocs[agent].setRow( gameMap.getCellRow(agentLocationID) );
		agentNextLocs[agent].setCol( gameMap.getCellCol(agentLocationID) );
		
		//set the reward??
		rewards[agent] = -0.5;
	    }
	}
	
	/**
	 * third step, whether agents are 
	 * out of boundary in the next location
	 */
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    /**
	     * the agent has already reached its goal
	     * or the action fails
	     */
	    if( bShouldStay[agent] )//|| nowInGoal[agent] )
		continue;
	    
	    if( outOfBoundary( agentNextLocs[agent] ) ) {
		
		bShouldStay[agent] = true;
		int agentLocationID = gameState.getLocationID(agent);
		agentNextLocs[agent].setRow( gameMap.getCellRow(agentLocationID) );
		agentNextLocs[agent].setCol( gameMap.getCellCol(agentLocationID) );
		
		//set the reward??
		rewards[agent] = -10;
	    }
	}
	
	/**
	 * the fourth step, whether agents are in the same narrow cell
	 */
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    /**
	     * the agent has already reached its goal
	     * or the action fails
	     */
	    if( nowInGoal[agent] )
		continue;
	    
	    /**
	     * note that a goal cell must not be a narrow cell
	     * so if an agent stay in its goal state, it will not be punished
	     */
	    GridWorldLocation nxtLocation = agentNextLocs[agent];
	    int nxtCellType = gameMap.getCellType( nxtLocation.getRow(), 
		    nxtLocation.getCol());
	    
	    /**
	     * check whether collapse with other agents 
	     * in a narrow cell
	     */
	    if( nxtCellType == MapCellType.NARROW_CELL ) {
		
		boolean punish = false;
		for( int other = 0; other < NUM_AGENTS; other++ ) {
		    
		    if( other == agent ) 
			continue;
		    
		    /**
		     * if the other agent is in its goal state???
		     */
		    if( nowInGoal[other] ) 
			continue;
		    
		    GridWorldLocation othNxtLocation = agentNextLocs[other];
		    
		    if( othNxtLocation.getRow() == nxtLocation.getRow() && 
			    othNxtLocation.getCol() == nxtLocation.getCol() ) {
			
			punish = true;
			
			/**
			 * punish all agents in a narrow cell 
			 * though an agent is forced to stay in it
			 */
			int othCurrentLocation = gameState.getLocationID( other );
			bShouldStay[other] = true;
			agentNextLocs[other].setRow( gameMap.getCellRow( othCurrentLocation ) );
			agentNextLocs[other].setCol( gameMap.getCellCol( othCurrentLocation ) );
			
			//set the reward
			rewards[other] = -10;
		    }
		}
		
		//punish this agent if it should be
		if( punish ) {
		    
		    int currentLocation = gameState.getLocationID( agent );
		    bShouldStay[agent] = true;
		    agentNextLocs[agent].setRow( gameMap.getCellRow( currentLocation ) );
		    agentNextLocs[agent].setCol( gameMap.getCellCol( currentLocation ) );
		    
		    //set the reward
		    rewards[agent] = -10;
		}
	    }
	    
	    /**
	     * if reach the goal in this step
	     * 
	     * not else, since a goal cell may be a narrow cell
	     */
	    if ( isGoal(agent, agentNextLocs[agent] ) && 
		    !bShouldStay[agent] ) {
		
		//set higher rewards
		rewards[agent] = 20;//200;//20;//100;//20;//1;
		//System.out.println("Reach the goal, agent "+agent);
		
	    }
	    /**
	     * if not reach the goal but move in this step
	     */
	    else if( !bShouldStay[agent] ) { //&& !nowInGoal[agent] ) {
		
		rewards[agent] = -1;
	    }
	}
	
	
	/**
	 * the fifth step, then set the next state
	 */
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    if( !bShouldStay[agent] ) { //|| nowInGoal[agent] ) {
		
		nextState.setLocationID( agent, gameMap.getCellIndex(agentNextLocs[agent]) );
		
	    }
	    else
		nextState.setLocationID( agent, gameState.getLocationID(agent) );
	}
	
	return nextState;
    }
    
    
    /**
     * @return: return the next state and corresponding reward
     */
    public int[] doAction_SingleAgent( int agentIndex, int curCell, 
	    int locAction, int step )
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
	
	
	int nextCell = curCell;
	GridWorldLocation agentNextLoc = new GridWorldLocation();
	boolean bShouldStay = false;
	
	/**
	 * first loop, we check whether the agent is out of boundary
	 * or reach its goal
	 */
	int currentRow = gameMap.getCellRow( curCell );
	int currentCol = gameMap.getCellCol( curCell );
	    
	/**
	 * if the agent has already reached the goal
	 */
	if( isGoal( agentIndex, curCell ) ) {
		
	    //the reward
	    retArray[1] = 0;
	    bShouldStay = true; 
	}
	else if( random.nextDouble() < 0.2 ) {
	    
	    agentNextLoc.setRow( currentRow );
	    agentNextLoc.setCol( currentCol );
	    
	    retArray[1] = -1;
	    bShouldStay = true;
	}
	else {
	    
	    /**
	     * let the agent walk one step further 
	     * and get the next location
	     */
	    agentNextLoc = nextLocation( curCell, locAction );
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
	    else if( isGoal( agentIndex, agentNextLoc ) ) {
		
		retArray[1] = 20;//200;//20;//100;//20;//1;
	    } 
	}

	/**
	 * then set the next state
	 */
	if( !bShouldStay ) {
		
	    nextCell = gameMap.getCellIndex( agentNextLoc );
		
	    /**
	     * if the next location is the goal?!
	     */
	    if( !isGoal( agentIndex, agentNextLoc ) )
		retArray[1] = -1;
	}
	retArray[0] = nextCell;
	
	return retArray;
    }
    
    
    public boolean outOfBoundary( int row, int col )
    {
	if( row < 0 || col < 0 || 
	    row >= mapHeight || 	
		col >= mapWidth ) {
	    
	    return true;
	}
	else {
	    
	    int cellType = gameMap.getCellType( row, col );
	    
	    if( cellType == MapCellType.INVALID_CELL ) {
		
		System.out.println("SparseGridWorld->outOfBoundary: Why Invalid Cell??");
		return true;
	    }
	    else if( cellType == MapCellType.WALL_CELL ) {
		
		return true;
	    }
	    else { 
		
		return false;
	    }
	}
	
	
    }
    
    public boolean outOfBoundary( GridWorldLocation location )
    {
	if( location == null )
	    return true;
	
	int row = location.getRow();
	int col = location.getCol();
	
	return outOfBoundary( row,  col );
    }
    
    
    /**
     * one test contains several algorithms run
     */
    public void oneTest()
    {
	int[] algTypes = new int[]{ 
		
		//MARL.CoordinateLearning,
		//MARL.Qlearning,
		//MARL.uCEQ, 
		//MARL.uCEQ_TransLocQ, 
		//MARL.uCEQ_TransPolicy,
		//MARL.eCEQ, 
		//MARL.eCEQ_TransLocQ, 
		//MARL.eCEQ_TransPolicy,
		//MARL.pCEQ, 
		//MARL.pCEQ_TransLocQ,
		//MARL.pCEQ_TransPolicy,
		//MARL.dCEQ, 
		
		//MARL.dCEQ_TransPolicy,
		MARL.NEGOQ_TransLocQ,
		//MARL.NEGOQ,
		//MARL.dCEQ_TransLocQ,
		//MARL.NASHQ,
		//MARL.NashQ_TransLocQ,
		//MARL.NashQ_TransPolicy,
		//MARL.uCEQ_TransModel,
		//MARL.eCEQ_TransModel,
		//MARL.pCEQ_TransModel,
		//MARL.dCEQ_TransModel,
		//MARL.NashQ_TransModel,
		//MARL.NEGOQ_TransModel,
		//MARL.uCEQ_AbsGame,
		//MARL.eCEQ_AbsGame,
		//MARL.pCEQ_AbsGame,
		//MARL.dCEQ_AbsGame,
		//MARL.NashQ_AbsGame,
		//MARL.NEGOQ_AbsGame
		//MARL.RANDOM
		};

	
	//int[] algTypes = new int[]{ MARL.dCEQ };
	
	for( int algIndex = 0; algIndex < algTypes.length; algIndex++ ) {
	    
	    
	    int algType = algTypes[algIndex];
	    
	    if( algType == MARL.uCEQ || algType == MARL.eCEQ ||
		    algType == MARL.pCEQ || algType == MARL.NASHQ || 
		    algType == MARL.NashQ_TransLocQ ||
		    algType == MARL.uCEQ_TransLocQ || 
		    algType == MARL.eCEQ_TransLocQ || 
		    algType == MARL.pCEQ_TransLocQ || 
		    algType == MARL.NashQ_TransPolicy ||
		    algType == MARL.uCEQ_TransPolicy || 
		    algType == MARL.eCEQ_TransPolicy ||
		    algType == MARL.pCEQ_TransPolicy || 
		    algType == MARL.uCEQ_TransModel || 
		    algType == MARL.eCEQ_TransModel || 
		    algType == MARL.pCEQ_TransModel || 
		    algType == MARL.NashQ_TransModel ||
		    algType == MARL.uCEQ_AbsGame ||
		    algType == MARL.eCEQ_AbsGame || 
		    algType == MARL.pCEQ_AbsGame || 
		    algType == MARL.NashQ_AbsGame )
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
	
	int LOOP = 10;//5;//10;
	
	/**
	 * choose the algorithm
	 */
	String algStr = MARL.ALG_STRINGS[algType];
	
	
	/**
	 * for recording steps
	 */
	double[][] stepNums = new double[NUM_AGENTS][episodesNum];
	double[][] allRewards = new double[NUM_AGENTS][episodesNum];
	for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ )
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
	    MARL agent = null; 
	    ArrayList<MARL> agents = null;
	    
		
	    if( isCentral )
		agent = createMARL( algType, 0 );
	    else {
		
		agents = new ArrayList<MARL>();
		for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
			
		    MARL marlAgent = createMARL( algType, agentIndex );
		    agents.add( marlAgent );
		}
	    }
	    
	    /**/
	    if( isCentral )
		agent.gameStarted(loop);
	    else {
		
		
		for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
		    
		    agents.get( agentIndex ).gameStarted( loop );
		}
	    }
	    
	    
	    
	    double[][][] retArray = new double[episodesNum][2][SparseGridWorld.NUM_AGENTS];
	    for( int ep = 0; ep < episodesNum; ep++ ) {
		    
		//System.out.println("Current ep: "+ep);
		
		long startTime = System.nanoTime();
		
		if( isCentral ) {
		    
		    agent.currentEpisode( ep );
		    retArray[ep] = oneEpisodeCentral( agent );
		}
		else if( algType == MARL.NEGOQ || 
			algType == MARL.NEGOQ_TransLocQ || 
			algType == MARL.NEGOQ_TransModel || 
			algType == MARL.NEGOQ_AbsGame ) {
		    
		    for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
			
			agents.get( agentIndex ).currentEpisode( ep );
		    }
		    retArray[ep] = oneEpisodeNego( agents, algType );
		}
		else {
		    
		    for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
			
			agents.get( agentIndex ).currentEpisode( ep );
		    }
		    retArray[ep] = oneEpisode( agents );
		}
		   
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
		
		for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
		    
		    agents.get( agentIndex ).gameFinished( loop );
		}
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
	    /**/
	    for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
		
		BufferedWriter stepsWriter = new BufferedWriter(new FileWriter("./" + algStr+"_agent" + agentIndex + ".csv"));
		for( int ep = 0; ep < episodesNum; ep++ ) {
		    
		    stepsWriter.write( (stepNums[agentIndex][ep] / (double)LOOP) + ", ");
		}
		stepsWriter.close();
	    }
	    
	    
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
	int LOOP = 1;//10;
	int epNum = 10000;
	
	/**
	 * choose the algorithm
	 */
	String algStr = "Q-learning";
	
	//String algStr = "Rmax";
	
	/**
	 * for recording steps
	 */
	double[] stepNums = new double[epNum];
	double[] allRewards = new double[epNum];
	for( int ep = 0; ep < epNum; ep++ ) {
		
	    stepNums[ep] = 0; 
	    allRewards[ep] = 0;
	}
	
	/**
	 * for recording time duration
	 */
	long[] durTimes = new long[epNum];
	
	//for the time of the hole process
	long beginTime = System.nanoTime();
		
	for( int loop = 1; loop <= LOOP; loop++ ) {
	
	    System.out.println("Algorithm: "+algStr+" the "+loop+"-th loop===========");
		
	    /**
	     * for centralized algorithms
	     */
	    QLearning qAgent = new QLearning( agentIndex );
	    qAgent.gameStarted(loop);
	    
	    //Rmax rmaxAgent = new Rmax( agentIndex, 20 );
	    
	    
	    double[][] retArray = new double[epNum][2];
	    for( int ep = 0; ep < epNum; ep++ ) {
		    
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
	    //rmaxAgent.gameFinished(loop);
	    
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
	    
	    /**
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
	    */
	}
	catch (IOException e)
	{
	    e.printStackTrace();
	}
	
    }
    
    
    public void oneRun_Rmax( int agentIndex )
    {
	int LOOP = 1;//10;
	int epNum = 1000000;//10000000;
	
	/**
	 * choose the algorithm
	 */	
	String algStr = "Rmax";
	
	
	/**
	 * for recording time duration
	 */
	long[] durTimes = new long[epNum];
	
	//for the time of the hole process
	long beginTime = System.nanoTime();
		
	for( int loop = 1; loop <= LOOP; loop++ ) {
	
	    System.out.println("Algorithm: "+algStr+" the "+loop+"-th loop===========");
		
	    /**
	     * for centralized algorithms
	     */
	    Rmax rmaxAgent = new Rmax( agentIndex, 200 );
	    
	    int episode = 0;
	    while( episode < epNum ) {
		
		    
		episode++;
		
		oneEpisode_SingleAgent( rmaxAgent, agentIndex );
		   
		displayProgress( episode );
		
		/**
		if( rmaxAgent.stopRunning() )
		    break;
		*/
		
	    }
	    
	    //one loop finished
	    rmaxAgent.gameFinished(loop);
	    
	}
	
	long overTime = System.nanoTime();
	
	
    }
    

    //should be changed
    public double[][] oneEpisode( ArrayList<MARL> agents )
    {
	
	if( agents == null || 
		agents.size() != SparseGridWorld.NUM_AGENTS ) {
	    
	    System.out.println("SparseGridWorld->oneEpisode: Wrong Parameters");
	}
	
	/**
	 * guarantee that the map has been loaded 
	 * 
	 * and all information of this map are right
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
	GameState gameState = new GameState();
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    gameState.setLocationID( agent, gameMap.getInitCellIndex(agent) );
	}
	gameState.setIndex( allStates.indexOf( gameState ) );
	
	/**
	 * get the action expected taken by each agent
	 */
	GameAction[] jntActions = new GameAction[SparseGridWorld.NUM_AGENTS];
	for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
	    
	    jntActions[agent] = agents.get( agent ).updateQ( null, null, null, gameState );
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
	
	    
	    
	int timeStep = 0;
	while( !gameOver( gameState ) && 
		timeStep < maxStep ) {
	   
	    
	    timeStep++;
	    
	    /**
	    System.out.println("Time Step: "+timeStep);
	    for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
		
		System.out.print(""+gameState.getLocationID( agentIndex )+",");
		
		if( isGoal( agentIndex, gameState.getLocationID( agentIndex ) ) )
		    System.out.println("I am in my goal!!!");
	    }
	    System.out.println();
	    */
	    
	    /**
	     * epsilon-greedy and get the action to be taken actually
	     */
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		    
		jntAction.setAction( agent, 
			agents.get(agent).epsilonGreedy(jntActions[agent].getAction(agent)));
	    }
	    
	    
	    //record the steps
	    for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
		
		if( !isGoal( agent, gameState.getLocationID(agent) ) ) {
		    
		    stepNums[agent] = stepNums[agent] + 1; 
		    retArray[0][agent] = retArray[0][agent] + 1;
		}
	    }
	    
	    /**
	     * observe the next state and get the rewards
	     */
	    GameState nextState = doAction( gameState, jntAction, rewards, stepNums );
	    nextState.setIndex( allStates.indexOf( nextState ) );
	    
	    //record the rewards
	    for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
		
		retArray[1][agent] = retArray[1][agent] + rewards[agent];  
	    }
	    
	    /**
	     * update Q-values
	     */
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		    
		jntActions[agent] = agents.get( agent ).
			updateQ( gameState, jntAction, rewards, nextState );
	    }
	    
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
	GameState gameState = new GameState();
	for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
	    
	    gameState.setLocationID( agentIndex, gameMap.getInitCellIndex(agentIndex) );
	}
	gameState.setIndex( allStates.indexOf( gameState ) );
	
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
	
	int timeStep = 0;
	while( !gameOver( gameState ) && 
		timeStep < maxStep ) {
	    
	    timeStep++;
	    
	    //System.out.println("The game is not over");
	    
	    //print the agent location
	    /**
	    for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
		
		System.out.print(""+gameState.getLocationID( agentIndex )+",");
	    }
	    System.out.println();
	    */
	    
	    /**
	     * epsilon-greedy and get the action to be taken actually
	     */
	    jntAction = agent.epsilonGreedy(jntAction);
	    
	    for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
		
		if( !isGoal( agentIndex, gameState.getLocationID(agentIndex) )  ) {
		    
		    stepNums[agentIndex] = stepNums[agentIndex] + 1;
		    retArray[0][agentIndex] = retArray[0][agentIndex] + 1;
		}
	    }
	    
	    /**
	    for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
		
		System.out.print(""+GameAction.getActionString(jntAction.getAction( agentIndex )));
	    }
	    System.out.println();
	    */
	    
	    /**
	     * observe the next state and get the rewards
	     */
	    GameState nextState = doAction( gameState, jntAction, rewards, stepNums );
	    nextState.setIndex( allStates.indexOf( nextState ) );
	    
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
    public double[][] oneEpisodeNego( ArrayList<MARL> agents, int algType )
    {
	
	ArrayList specificAgents = null;
	
	if( algType == MARL.NEGOQ ) {
	 
	    specificAgents = new ArrayList<NegoQ>();
	    for( int agIndex = 0; agIndex < NUM_AGENTS; agIndex++ ) {
		
		NegoQ negoQAgent = (NegoQ) agents.get( agIndex ); 
		specificAgents.add( negoQAgent );
	    }
	}
	else if( algType == MARL.NEGOQ_TransLocQ ) {
	    
	    specificAgents = new ArrayList<NegoQTransLocQ>();
	    for( int agIndex = 0; agIndex < NUM_AGENTS; agIndex++ ) {
		
		NegoQTransLocQ negoTransQAgent = (NegoQTransLocQ) agents.get( agIndex ); 
		specificAgents.add( negoTransQAgent );
	    }
	}
	else if( algType == MARL.NEGOQ_TransModel ) {
	    
	    specificAgents = new ArrayList<NegoQTransModel>();
	    for( int agIndex = 0; agIndex < NUM_AGENTS; agIndex++ ) {
		
		NegoQTransModel negoQTMAgent = (NegoQTransModel) agents.get( agIndex ); 
		specificAgents.add( negoQTMAgent );
	    }
	}
	else if( algType == MARL.NEGOQ_AbsGame ) {
	    
	    specificAgents = new ArrayList<NegoQAbsGame>();
	    for( int agIndex = 0; agIndex < NUM_AGENTS; agIndex++ ) {
		
		NegoQAbsGame negoQAGAgent = (NegoQAbsGame) agents.get( agIndex ); 
		specificAgents.add( negoQAGAgent );
	    }
	}
	else {
	    
	    System.out.println("GridWorld->oneEpisodeNego: Parameter Wrong!");
	    return null;
	}
	
	
	
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
	GameState gameState = new GameState();
	for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
	    
	    gameState.setLocationID( agentIndex, gameMap.getInitCellIndex(agentIndex) );
	}
	gameState.setIndex( allStates.indexOf( gameState ) );
	
	/**
	 * get the action expected taken by each agent 
	 * through negotiation
	 */
	
	/**/
	GameAction optAction = null;
	if( algType == MARL.NEGOQ) {
	    
	    optAction = NegoQ.negotiation( specificAgents, gameState );
	}
	else if( algType == MARL.NEGOQ_TransLocQ ){
	    
	    optAction = NegoQTransLocQ.negotiation( specificAgents, gameState );
	}
	else if( algType == MARL.NEGOQ_TransModel) {
	    
	    optAction = NegoQTransModel.negotiation( specificAgents, gameState, true);
	}
	else {
	    
	    optAction = NegoQAbsGame.negotiation( specificAgents, gameState, true );
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
	
	
	int timeStep = 0;
	while( !gameOver( gameState ) && 
		timeStep < maxStep ) {
	    
	    timeStep++;
	    
	    /**
	     * epsilon-greedy and get the action to be taken actually
	     */
	    for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
		
		jntAction.setAction( agent, 
			agents.get(agent).epsilonGreedy( optAction.getAction(agent) ) );
	    }
	    
	    for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
		
		if( !isGoal( agent, gameState.getLocationID(agent) ) ) {
		    
		    stepNums[agent] = stepNums[agent] + 1;
		    retArray[0][agent] = retArray[0][agent] + 1;
		}
	    }
	    
	    /**
	     * observe the next state and get the rewards
	     */
	    GameState nextState = doAction( gameState, jntAction, rewards, stepNums );
	    nextState.setIndex( allStates.indexOf( nextState ) );
	    
	    //record the rewards
	    for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
		
		retArray[1][agent] = retArray[1][agent] + rewards[agent];  
	    }
	    
	    /**
	     * get the optimal action int the next state 
	     * and update Q-values
	     */
	    if( algType == MARL.NEGOQ) {
		    
		optAction = NegoQ.negotiation( specificAgents, nextState );
		
		for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
			    
		    NegoQ negoQ = (NegoQ) agents.get(agent); 
		    negoQ.updateQ_NegoQ(gameState, jntAction, rewards, nextState, optAction);
		}
	    }
	    else if( algType == MARL.NEGOQ_TransLocQ ) {
		    
		optAction = NegoQTransLocQ.negotiation( specificAgents, nextState );
		for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		    
		    NegoQTransLocQ negoTransQ = (NegoQTransLocQ) agents.get(agent); 
		    negoTransQ.updateQ_NegoQ(gameState, jntAction, rewards, nextState, optAction);
		}
	    }
	    else if( algType == MARL.NEGOQ_TransModel ) {
		
		optAction = NegoQTransModel.negotiation( specificAgents, nextState, true );
		for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		    
		    NegoQTransModel negoQTM = (NegoQTransModel) agents.get(agent); 
		    negoQTM.updateQ_NegoQ(gameState, jntAction, rewards, nextState, optAction);
		}
	    }
	    else {
		
		optAction = NegoQAbsGame.negotiation( specificAgents, nextState, true );
		for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		    
		    NegoQAbsGame negoQABG = (NegoQAbsGame) agents.get(agent); 
		    negoQABG.updateQ_NegoQ(gameState, jntAction, rewards, nextState, optAction);
		}
	    }
	    
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
	Random rnd = new Random();
	int locState = 0;
	while( locState == gameMap.getGoalCellIndex(agentIndex) || 
		gameMap.getCellType( locState ) == MapCellType.INVALID_CELL || 
		gameMap.getCellType( locState ) == MapCellType.WALL_CELL ) {
	    
	    locState = rnd.nextInt(SparseGridWorld.NUM_CELLS);
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
	
	    
	    //System.out.println("LocState: "+locState+", Action: "+GameAction.getActionString( curAction ));
	}
	
	//compute the average reward
	if( retArray[0]< 0.00000001 )
	    retArray[1] = 0.0;
	    
	else {
	    retArray[1] = retArray[1] / retArray[0]; 
	}
	
	return retArray;
    }
    
    
    public double[] oneEpisode_SingleAgent( Rmax rmaxAgent, int agentIndex )
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
	Random rnd = new Random();
	int locState = 0;
	while( locState == gameMap.getGoalCellIndex(agentIndex) || 
		gameMap.getCellType( locState ) == MapCellType.INVALID_CELL || 
		gameMap.getCellType( locState ) == MapCellType.WALL_CELL ) {
	    
	    locState = rnd.nextInt(SparseGridWorld.NUM_CELLS);
	}
	
	
	
	/**
	 * get the action expected taken by each agent
	 */
	int curAction = rmaxAgent.updateQ( -1, -1, 0, locState );
	
	
	while( !isGoal( agentIndex, locState ) ) {
	    
	    /**
	     * no epsilon-greedy for Rmax agents
	     */
	    //curAction = qAgent.epsilonGreedy(curAction);
	    
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
	    int nextAction = rmaxAgent.updateQ( locState, curAction, reward, nextLocState );
	    
	    /**
	     * transfer to the next state
	     */
	    locState = nextLocState;
	    curAction = nextAction;
	
	    
	    //System.out.println("LocState: "+locState+", Action: "+GameAction.getActionString( curAction ));
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
	case MARL.uCEQ:
	    return new UCEQ( 0.7, 0.9, 0.01 );
	case MARL.eCEQ:
	    return new ECEQ( 0.7, 0.9, 0.01 );
	case MARL.pCEQ:
	    return new PCEQ( 0.7, 0.9, 0.01 );
	case MARL.dCEQ:
	    return new DCEQ( agent, 0.99, 0.9, 0.01 );
	case MARL.NEGOQ:
	    return new NegoQ( agent, 0.99, 0.9, 0.01 );
	case MARL.NASHQ:
	    return new NashQ( 0.99, 0.9, 0.01 );
	case MARL.uCEQ_TransLocQ:
	    return new UCEQTransLocQ( 0.1, 0.9, 0.01 );
	case MARL.eCEQ_TransLocQ:
	    return new ECEQTransLocQ( 0.1, 0.9, 0.01 );
	case MARL.pCEQ_TransLocQ:
	    return new PCEQTransLocQ( 0.1, 0.9, 0.01 );
	case MARL.dCEQ_TransLocQ:
	    return new DCEQTransLocQ( agent, 0.99, 0.9, 0.01 );
	case MARL.NashQ_TransLocQ:
	    return new NashQTransLocQ( 0.7, 0.9, 0.01 );
	case MARL.NEGOQ_TransLocQ:
	    return new NegoQTransLocQ( agent, 0.1, 0.9, 0.01 );
	case MARL.uCEQ_TransPolicy:
	    return new UCEQTransPolicy();
	case MARL.eCEQ_TransPolicy:
	    return new ECEQTransPolicy( 0.99, 0.9, 0.01 );
	case MARL.pCEQ_TransPolicy:
	    return new PCEQTransPolicy( 0.99, 0.9, 0.01 );
	case MARL.dCEQ_TransPolicy:
	    return new DCEQTransPolicy( agent );
	case MARL.NashQ_TransPolicy:
	    return new NashQTransPolicy();
	case MARL.CoordinateLearning:
	    return new CoordinateLearning( agent, 0.1, 0.9, 0.01 );
	case MARL.Qlearning:
	    return new QL( agent, 0.99, 0.9, 0.03 );
	case MARL.uCEQ_TransModel:
	    return new UCEQTransModel( 0.99, 0.9, 0.01 );
	case MARL.eCEQ_TransModel:
	    return new ECEQTransModel( 0.99, 0.9, 0.01 );
	case MARL.pCEQ_TransModel:
	    return new PCEQTransModel( 0.99, 0.9, 0.01 );
	case MARL.dCEQ_TransModel:
	    return new DCEQTransModel( agent, 0.99, 0.9, 0.01 );
	case MARL.NashQ_TransModel:
	    return new NashQTransModel( 0.99, 0.9, 0.01 );
	case MARL.NEGOQ_TransModel:
	    return new NegoQTransModel( agent, 0.99, 0.9, 0.01 );
	case MARL.uCEQ_AbsGame:
	    return new UCEQAbsGame( 0.7, 0.9, 0.01 );
	case MARL.eCEQ_AbsGame:
	    return new ECEQAbsGame( 0.7, 0.9, 0.01 );
	case MARL.pCEQ_AbsGame:
	    return new PCEQAbsGame( 0.7, 0.9, 0.01 );
	case MARL.dCEQ_AbsGame:
	    return new DCEQAbsGame( agent, 0.99, 0.9, 0.01 );
	case MARL.NashQ_AbsGame:
	    return new NashQAbsGame( 0.99, 0.9, 0.01 );
	case MARL.NEGOQ_AbsGame:
	    return new NegoQAbsGame( agent, 0.1, 0.9, 0.01 );
	default:
	    return new MARL( agent );
	}
    }
    
    
    /**
     * generate all valid states according to the map of the game
     */
    private void generateValidStates()
    {
	
	
	allStates = new ArrayList<GameState>();
	
	int numCells = gameMap.getNumCells();
	
	/**
	 * guarantee that the map has been loaded correctly
	 */
	int[] stateIterator = new int[NUM_AGENTS]; 
	
	for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
	    
	    stateIterator[agentIndex] = 0;
	}
	
	
	while( true ) {
	    
	    /**
	     * check whether current state is valid
	     */
	    if( availableState( stateIterator ) ) {
		
		GameState state = new GameState( stateIterator );
		
		if( !allStates.contains( state ) ) {
		    
		    allStates.add( state );
		}
		else {
		    
		    state = null;
		}
	    }
	    
	    /**
	     * add all states to the list
	     *
	    GameState state = new GameState( stateIterator );
	    if( !allStates.contains( state ) ) {
		    
		allStates.add( state );
	    }
	    else {
		    
		state = null;
	    }
	    */
	    
	    /**
	     * move to the next state
	     */
	    for( int agent = NUM_AGENTS-1; agent >= 0; agent-- ) {
		
		stateIterator[agent] += 1;
		
		if( agent > 0 && stateIterator[agent] >= numCells ) {
		    
		    stateIterator[agent] = 0;
		}
		else
		    break;
	    }
	    
	    /**
	     * stop condition
	     */
	    if( stateIterator[0] >= numCells ) {
		
		break;
	    }
	    
	}
	
	System.out.println("Size: "+allStates.size());
    }
    
    /**
     * whether a collection of cell indices are valid state
     */
    private boolean availableState( int[] stateIterator )
    {
	if( stateIterator == null || 
		stateIterator.length != NUM_AGENTS ) {
	    
	    System.out.println("Map->availableState: Wrong Parameter!");
	    return false;
	}
	
	
	for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
	    
	    int cellIndex = stateIterator[agentIndex];
	    
	    int cellType = gameMap.getCellType( cellIndex );
	    
	    /**
	     * test whether it has wall cells
	     */
	    if( cellType == MapCellType.INVALID_CELL ) {
		
		System.out.println("Map->availableState: INVALID CELL TYPE!");
		return false;
	    }
	    else if( cellType == MapCellType.WALL_CELL ) {
		
		return false;
	    }
	    
	    /**
	     * test whether this agent and some other agent 
	     * are in the same narrow cell
	     */
	    for( int otherAgent = 0; otherAgent < NUM_AGENTS; otherAgent++ ) {
		
		if( otherAgent == agentIndex )
		    continue;
		
		int cellIndexOther = stateIterator[otherAgent];
		
		if( cellIndex == cellIndexOther && 
			cellType == MapCellType.NARROW_CELL && 
			cellIndex != gameMap.getGoalCellIndex(agentIndex) && 
			cellIndex != gameMap.getGoalCellIndex(otherAgent) ) {
		    
		    return false;
		}
	    }
	}
	
	return true;
    }

    /**
     * generate all possible joint actions
     */
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
     * query the index of a joint action in the joint action list
     */
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
    
    public static int queryStateIndex( GameState gameState )
    {
	
	if( gameState == null ) {
	    
	    System.out.println("Null Game State");
	    return -1;
	}
	else if( allStates == null ) {
	    
	    System.out.println("Null List");
	    return -1;
	}
	
	/**/
	int index = allStates.indexOf( gameState );
	return index;
	
	
	/**
	 * find a start index
	 *
	int mapCellNum = NUM_CELLS;
	int endIndex = 0;
	for( int agent = SparseGridWorld.NUM_AGENTS-1; agent >= 0; agent-- ) {
	    
	    int power = SparseGridWorld.NUM_AGENTS-1 - agent;
	    int prod = (int) Math.pow( ((double) mapCellNum), power );
	    
	    endIndex += prod * gameState.getLocationID( agent );
	}
	
	return endIndex;
	*/
	
	/**
	for( int listIndex = 0; listIndex < allStates.size(); listIndex++ ) {
	    
	    GameState state = allStates.get( listIndex );
	    
	    if( gameState.equals( state ) ) {
		
		return listIndex;
	    }
	}
	
	for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
	    
	    System.out.print(gameState.getLocationID(agentIndex)+",");
	}
	System.out.println();
	
	return -1;
	*/
    }
    
    public static ArrayList<GameState> getAllValidStates()
    {
	
	return allStates;
    }
    
    
    public static ArrayList<GameAction> getAllJointActions()
    {
	
	return allJointActions;
    }
    
   
    
    /**
     * @param args
     */
    public static void main(String[] args)
    {
	// TODO Auto-generated method stub
	
	SparseGridWorld sgw = new SparseGridWorld("./maps/GearMap2.txt","./maps/GearMap2_Info.txt");
	
	//multi-agent test
	sgw.oneTest();
	
	//single-agent test
	//sgw.oneRun_Rmax(3);
	//sgw.oneRun_SingleAgent(2);

    }
}
