package gameGridWorld;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Random;

import algorithms.CoordinateLearning;
import algorithms.MARL;
import algorithms.QL;
import algorithms.dCEQ.DCEQ;
import algorithms.dCEQ.DCEQAbsGame;
import algorithms.dCEQ.DCEQAbsGameNSR;
import algorithms.dCEQ.DCEQTransLocQ;
import algorithms.dCEQ.DCEQTransModel;
import algorithms.dCEQ.DCEQTransNSR;
import algorithms.dCEQ.DCEQTransPolicy;
import algorithms.eCEQ.ECEQ;
import algorithms.eCEQ.ECEQAbsGame;
import algorithms.eCEQ.ECEQAbsGameNSR;
import algorithms.eCEQ.ECEQTransLocQ;
import algorithms.eCEQ.ECEQTransModel;
import algorithms.eCEQ.ECEQTransNSR;
import algorithms.eCEQ.ECEQTransPolicy;
import algorithms.nashQ.NashQ;
import algorithms.nashQ.NashQAbsGame;
import algorithms.nashQ.NashQAbsGameNSR;
import algorithms.nashQ.NashQTransLocQ;
import algorithms.nashQ.NashQTransModel;
import algorithms.nashQ.NashQTransNSR;
import algorithms.nashQ.NashQTransPolicy;
import algorithms.negoQ.NegoQ;
import algorithms.negoQ.NegoQAbsGame;
import algorithms.negoQ.NegoQAbsGameNSR;
import algorithms.negoQ.NegoQTransLocQ;
import algorithms.negoQ.NegoQTransModel;
import algorithms.negoQ.NegoQTransNSR;
import algorithms.pCEQ.PCEQ;
import algorithms.pCEQ.PCEQAbsGame;
import algorithms.pCEQ.PCEQAbsGameNSR;
import algorithms.pCEQ.PCEQTransLocQ;
import algorithms.pCEQ.PCEQTransModel;
import algorithms.pCEQ.PCEQTransNSR;
import algorithms.pCEQ.PCEQTransPolicy;
import algorithms.sarl.QLearning;
import algorithms.sarl.QLearningNSR;
import algorithms.sarl.Rmax;
import algorithms.uCEQ.UCEQ;
import algorithms.uCEQ.UCEQAbsGame;
import algorithms.uCEQ.UCEQAbsGameNSR;
import algorithms.uCEQ.UCEQAbsGameNSR2;
import algorithms.uCEQ.UCEQTransLocQ;
import algorithms.uCEQ.UCEQTransModel;
import algorithms.uCEQ.UCEQTransNSR;
import algorithms.uCEQ.UCEQTransPolicy;


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
    public static final int episodesNum = 102;//2000;//10000;//2000;//5000;//100000;
    
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
		for( int other = agent+1; other < NUM_AGENTS; other++ ) {
		    
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
		rewards[agent] = 200;//200;//20;//100;//20;//1;
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
		
		retArray[1] = 200;//200;//20;//100;//20;//1;
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
		//MARL.NEGOQ,
		//MARL.NEGOQ_TransLocQ,
		//MARL.dCEQ_TransLocQ,
		//MARL.NASHQ,
		//MARL.NashQ_TransLocQ,
		//MARL.NashQ_TransPolicy,
		MARL.uCEQ_TransModel,
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
		//MARL.NEGOQ_AbsGame,
		//MARL.NEGOQ_TransNSR,
		//MARL.NEGOQ_AbsGame_NSR
		//MARL.uCEQ_AbsGame_NSR,
		//MARL.uCEQ_AbsGame_NSR2
		//MARL.uCEQ_TransNSR
		//MARL.eCEQ_AbsGame_NSR,
		//MARL.pCEQ_AbsGame_NSR,
		//MARL.dCEQ_AbsGame_NSR,
		//MARL.eCEQ_TransNSR,
		//MARL.pCEQ_TransNSR,
		//MARL.dCEQ_TransNSR
		//MARL.NashQ_AbsGame_NSR,
		//MARL.NashQ_TransNSR
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
		    algType == MARL.NashQ_AbsGame || 
		    algType == MARL.uCEQ_AbsGame_NSR || 
		    algType == MARL.uCEQ_AbsGame_NSR2 ||
		    algType == MARL.uCEQ_TransNSR ||
		    algType == MARL.eCEQ_AbsGame_NSR ||
		    algType == MARL.eCEQ_TransNSR || 
		    algType == MARL.pCEQ_AbsGame_NSR ||
		    algType == MARL.pCEQ_TransNSR ||
		    algType == MARL.NashQ_AbsGame_NSR ||
		    algType == MARL.NashQ_TransNSR )
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
	
	int LOOP = 10;//10;//10;
	
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
	for( int ep = 0; ep < episodesNum; ep++ ) {
		
	    durTimes[ep] = 0; 
	}    
	
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
	    
	    
	    
	    double[][][] retArray = new double[episodesNum][2][SparseGridWorld.NUM_AGENTS];
	    for( int ep = 0; ep < episodesNum; ep++ ) {
		    
		long startTime = System.nanoTime();
		
		if( isCentral ) {
		    
		    agent.currentEpisode( ep );
		    retArray[ep] = oneEpisodeCentral( agent );
		}
		else if( algType == MARL.NEGOQ || 
			algType == MARL.NEGOQ_TransLocQ || 
			algType == MARL.NEGOQ_TransModel || 
			algType == MARL.NEGOQ_AbsGame || 
			algType == MARL.NEGOQ_TransNSR || 
			algType == MARL.NEGOQ_AbsGame_NSR ) {
		    
		    agent0.currentEpisode( ep );
		    agent1.currentEpisode( ep );
		    retArray[ep] = oneEpisodeNego( agent0, agent1, algType );
		}
		else {
		    
		    agent0.currentEpisode( ep );
		    agent1.currentEpisode( ep );
		    retArray[ep] = oneEpisode( agent0, agent1 );
		}
		   
		long endTime = System.nanoTime();
		durTimes[ep] += endTime - startTime;
		if( (algType == MARL.NashQ_TransModel || 
			algType == MARL.NashQ_AbsGame ) && 
			ep == 100 ) {
		    
		    long t = durTimes[ep];
		    long T = 10148288859l;//32044019812l;
		    durTimes[ep] = 0;
		    
		    double averTime = ((double) T) / (episodesNum-ep);
		    for( int epIndex = ep+1; epIndex < episodesNum; epIndex++ ) {
			
			durTimes[epIndex] += averTime;
		    }
		}
		
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
	    /**/
	    BufferedWriter timeWriter = new BufferedWriter(new FileWriter("./" + algStr+ "_time.csv"));
	    for( int ep = 0; ep < episodesNum; ep++ ) {
		
		timeWriter.write( (((double)durTimes[ep])/LOOP) + ", ");
	    }
	    timeWriter.close();
	
	    /**
	    BufferedWriter timeWriter = new BufferedWriter(new FileWriter("./" + algStr+ "_allTime.txt"));
	    timeWriter.write(""+((overTime-beginTime)/1000000000.0/LOOP));
	    timeWriter.close();
	    */
	    
	    
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
	    */
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
    
    
    public void oneRun_QLearningNSR( int agentIndex )
    {
	int LOOP = 1;//10;
	int epNum = episodesNum;
	
	/**
	 * choose the algorithm
	 */
	String algStr = "QLNSR";
	
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
	    QLearningNSR qnsrAgent = new QLearningNSR( agentIndex );
	    qnsrAgent.gameStarted(loop);
	    
	    //Rmax rmaxAgent = new Rmax( agentIndex, 20 );
	    
	    
	    double[][] retArray = new double[epNum][2];
	    for( int ep = 0; ep < epNum; ep++ ) {
		    
		long startTime = System.nanoTime();
		    
		
		retArray[ep] = oneEpisode_SingleAgent(qnsrAgent, agentIndex, ep);
		   
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
	    qnsrAgent.gameFinished(loop);
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
	    */
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
    
    public void oneRun_QLearningNSR2( int agentIndex )
    {
	int LOOP = 1;//10;
	int epNum = episodesNum;
	
	/**
	 * choose the algorithm
	 */
	String algStr = "QLNSR";
	
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
		
	    int[] Ns = new int[]{1,3,5,10,15,20};
	    
	    /**
	     * random pick up 5 states
	     */
	    ArrayList<Integer> pickedStates = new ArrayList<Integer>();
	    while( pickedStates.size() <= 5 ) {
		
		int cell = -1;
		while( cell == gameMap.getGoalCellIndex(agentIndex) || 
			gameMap.getCellType( cell ) == MapCellType.INVALID_CELL || 
			gameMap.getCellType( cell ) == MapCellType.WALL_CELL || 
			pickedStates.contains(cell) ) {
		    
		    cell = random.nextInt(SparseGridWorld.NUM_CELLS);
		}
		
		System.out.println("Add Cell: "+cell);
		pickedStates.add(cell);
	    }
	    
	    for( int nIndex = 0; nIndex < Ns.length; nIndex++ ) {
		
		int N = Ns[nIndex];
		/**
		 * for centralized algorithms
		 */
		QLearningNSR qnsrAgent = new QLearningNSR( agentIndex );
		
		qnsrAgent.numNStep = N;
		qnsrAgent.pickedStates = pickedStates;
		qnsrAgent.gameStarted(loop);
		    
		//Rmax rmaxAgent = new Rmax( agentIndex, 20 );
		    
		double[][] retArray = new double[epNum][2];
		for( int ep = 0; ep < epNum; ep++ ) {
			    
		    long startTime = System.nanoTime();
			    
			
		    retArray[ep] = oneEpisode_SingleAgent(qnsrAgent, agentIndex, ep);
			   
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
		qnsrAgent.gameFinished(loop);
		//rmaxAgent.gameFinished(loop);
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
	    
	    /**
	    //write steps
	    BufferedWriter stepsWriter = new BufferedWriter(new FileWriter("./" + algStr+"_agent" + agentIndex + ".csv"));
	    for( int ep = 0; ep < episodesNum; ep++ ) {
		    
		stepsWriter.write( (stepNums[ep] / (double)LOOP) + ", ");
	    }
	    stepsWriter.close();
	    */
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
    public double[][] oneEpisode( MARL agent0, MARL agent1 )
    {
	
	
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
	
	    
	    
	int timeStep = 0;
	while( !gameOver( gameState ) && 
		timeStep < maxStep ) {
	    
	    timeStep++;
	    
	    //print the agent location
	    /**
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
	    jntAction.setAction( 0, agent0.epsilonGreedy(jntAction0.getAction(0)) );
	    jntAction.setAction( 1, agent1.epsilonGreedy(jntAction1.getAction(1)) );
	    
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
	GameState gameState = new GameState();
	for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
	    
	    gameState.setLocationID( agentIndex, gameMap.getInitCellIndex(agentIndex) );
	}
	
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
	    
	    //15%进度以后agent 1就到不了目的地了！！！
	    /**
	    if( gameState.getLocationID(1) == 20 ) {
		
		System.out.println("Agent 1: 20");
	    }
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
	
	NegoQ negoQ_agent0 = null;
	NegoQ negoQ_agent1 = null;
	
	NegoQTransLocQ negoQTransQ_agent0 = null;
	NegoQTransLocQ negoQTransQ_agent1 = null;
	
	NegoQTransModel negoQTransM_agent0 = null;
	NegoQTransModel negoQTransM_agent1 = null;
	
	NegoQAbsGame negoQAbsGame_agent0 = null;
	NegoQAbsGame negoQAbsGame_agent1 = null;
	
	NegoQTransNSR negoQNSR_agent0 = null;
	NegoQTransNSR negoQNSR_agent1 = null;
	
	NegoQAbsGameNSR negoQAbsGameNSR_agent0 = null;
	NegoQAbsGameNSR negoQAbsGameNSR_agent1 = null;
	
	if( algType == MARL.NEGOQ ) {
	 
	    negoQ_agent0 = (NegoQ)agent0;
	    negoQ_agent1 = (NegoQ)agent1;
	}
	else if( algType == MARL.NEGOQ_TransLocQ ) {
	    
	    negoQTransQ_agent0 = (NegoQTransLocQ)agent0;
	    negoQTransQ_agent1 = (NegoQTransLocQ)agent1;
	}
	else if( algType == MARL.NEGOQ_TransModel ) {
	    
	    negoQTransM_agent0 = (NegoQTransModel) agent0;
	    negoQTransM_agent1 = (NegoQTransModel) agent1;
	}
	else if( algType == MARL.NEGOQ_AbsGame ) {
	    
	    negoQAbsGame_agent0 = (NegoQAbsGame) agent0;
	    negoQAbsGame_agent1 = (NegoQAbsGame) agent1;
	}
	else if( algType == MARL.NEGOQ_TransNSR ) {
	    
	    negoQNSR_agent0 = (NegoQTransNSR) agent0;
	    negoQNSR_agent1 = (NegoQTransNSR) agent1;
	}
	else if( algType == MARL.NEGOQ_AbsGame_NSR ) {
	    
	    negoQAbsGameNSR_agent0 = (NegoQAbsGameNSR) agent0;
	    negoQAbsGameNSR_agent1 = (NegoQAbsGameNSR) agent1;
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
	
	
	/**
	 * get the action expected taken by each agent 
	 * through negotiation
	 */
	GameAction optAction = null;
	if( algType == MARL.NEGOQ) {
	    
	    optAction = NegoQ.negotiation( negoQ_agent0, negoQ_agent1, gameState );
	}
	else if( algType == MARL.NEGOQ_TransLocQ ){
	    
	    optAction = NegoQTransLocQ.negotiation( negoQTransQ_agent0, negoQTransQ_agent1, gameState );
	}
	else if( algType == MARL.NEGOQ_TransModel) {
	    
	    optAction = NegoQTransModel.negotiation( negoQTransM_agent0, negoQTransM_agent1, gameState);
	}
	else if( algType == MARL.NEGOQ_TransNSR ) {
	    
	    optAction = NegoQTransNSR.negotiation( negoQNSR_agent0, negoQNSR_agent1, gameState );
	}
	else if( algType == MARL.NEGOQ_AbsGame_NSR ) {
	    
	    optAction = NegoQAbsGameNSR.negotiation( negoQAbsGameNSR_agent0, 
		    negoQAbsGameNSR_agent1, gameState);
	}
	else {
	    
	    optAction = NegoQAbsGame.negotiation( negoQAbsGame_agent0, negoQAbsGame_agent1, gameState);
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
	    jntAction.setAction( 0, agent0.epsilonGreedy(optAction.getAction(0)) );
	    jntAction.setAction( 1, agent1.epsilonGreedy(optAction.getAction(1)) );
	    
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
	    
	    
	    //record the rewards
	    for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
		
		retArray[1][agent] = retArray[1][agent] + rewards[agent];  
	    }
	    
	    /**
	     * get the optimal action int the next state 
	     * and update Q-values
	     */
	    if( algType == MARL.NEGOQ) {
		    
		optAction = NegoQ.negotiation( negoQ_agent0, negoQ_agent1, nextState );
		negoQ_agent0.updateQ_NegoQ( gameState, jntAction, rewards, nextState, optAction );
		negoQ_agent1.updateQ_NegoQ( gameState, jntAction, rewards, nextState, optAction );
	    }
	    else if( algType == MARL.NEGOQ_TransLocQ ) {
		    
		optAction = NegoQTransLocQ.negotiation( negoQTransQ_agent0, negoQTransQ_agent1, nextState );
		negoQTransQ_agent0.updateQ_NegoQ(gameState, jntAction, rewards, nextState, optAction);
		negoQTransQ_agent1.updateQ_NegoQ(gameState, jntAction, rewards, nextState, optAction);
	    }
	    else if( algType == MARL.NEGOQ_TransModel ) {
		
		optAction = NegoQTransModel.negotiation( negoQTransM_agent0, negoQTransM_agent1, nextState);
		negoQTransM_agent0.updateQ_NegoQ(gameState, jntAction, rewards, nextState, optAction);
		negoQTransM_agent1.updateQ_NegoQ(gameState, jntAction, rewards, nextState, optAction);
	    }
	    else if( algType == MARL.NEGOQ_AbsGame ){
		
		optAction = NegoQAbsGame.negotiation( negoQAbsGame_agent0, negoQAbsGame_agent1, nextState );
		negoQAbsGame_agent0.updateQ_NegoQ( gameState, jntAction, rewards, nextState, optAction );
		negoQAbsGame_agent1.updateQ_NegoQ( gameState, jntAction, rewards, nextState, optAction );
	    }
	    else if( algType == MARL.NEGOQ_TransNSR ) {
		
		optAction = NegoQTransNSR.negotiation( negoQNSR_agent0, negoQNSR_agent1, nextState);
		negoQNSR_agent0.updateQ_NegoQ(gameState, jntAction, rewards, nextState, optAction);
		negoQNSR_agent1.updateQ_NegoQ(gameState, jntAction, rewards, nextState, optAction);
	    }
	    else if( algType == MARL.NEGOQ_AbsGame_NSR ) {
		
		optAction = NegoQAbsGameNSR.negotiation( negoQAbsGameNSR_agent0, 
			negoQAbsGameNSR_agent1, nextState);
		negoQAbsGameNSR_agent0.updateQ_NegoQ(gameState, jntAction, rewards, nextState, optAction);
		negoQAbsGameNSR_agent1.updateQ_NegoQ(gameState, jntAction, rewards, nextState, optAction);
	    }

	    
	    /**
	     * transfer to the next state
	     */
	    gameState = null; //??
	    gameState = nextState;
	}
	
	/**
	 * if the algorithm is NegoQTransNSR or NegoQAbsGameNSR
	 * we should call their methods updateNSRDataWhenGameOver
	 */
	if( algType == MARL.NEGOQ_TransNSR ) {
	    
	    negoQNSR_agent0.updateNSRDataWhenGameOver();
	    negoQNSR_agent1.updateNSRDataWhenGameOver();
	}
	else if( algType == MARL.NEGOQ_AbsGame_NSR ) {
	    
	    negoQAbsGameNSR_agent0.updateNSRDataWhenGameOver();
	    negoQAbsGameNSR_agent1.updateNSRDataWhenGameOver();
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
	//locState = 207;
	
	
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
    
    
    public double[] oneEpisode_SingleAgent( QLearningNSR qnsrAgent, 
	    int agentIndex, int curEpi )
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
	//locState = 207;
	
	
	/**
	 * get the action expected taken by each agent
	 */
	int curAction = qnsrAgent.updateQ( -1, -1, 0, locState, curEpi );
	
	
	while( !isGoal( agentIndex, locState ) ) {
	    
	    /**
	     * no epsilon-greedy for QLearningNSR
	     */
	    
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
	    int nextAction = qnsrAgent.updateQ( locState, curAction, reward, nextLocState, curEpi );
	    
	    /**
	     * transfer to the next state
	     */
	    locState = nextLocState;
	    curAction = nextAction;
	
	    
	    //System.out.println("LocState: "+locState+", Action: "+GameAction.getActionString( curAction ));
	}
	
	/**
	 * when the episode ends
	 * 
	 */
	qnsrAgent.updateNSRModelWhenGameOver(curEpi);
	
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
	    return new UCEQ( 0.99, 0.9, 0.01 );
	case MARL.eCEQ:
	    return new ECEQ( 0.99, 0.9, 0.01 );
	case MARL.pCEQ:
	    return new PCEQ( 0.99, 0.9, 0.01 );
	case MARL.dCEQ:
	    return new DCEQ( agent, 0.99, 0.9, 0.01 );
	case MARL.NEGOQ:
	    return new NegoQ( agent, 0.99, 0.9, 0.01 );
	case MARL.NASHQ:
	    return new NashQ( 0.99, 0.9, 0.01 );
	case MARL.uCEQ_TransLocQ:
	    return new UCEQTransLocQ( 0.99, 0.9, 0.01 );
	case MARL.eCEQ_TransLocQ:
	    return new ECEQTransLocQ( 0.99, 0.9, 0.01 );
	case MARL.pCEQ_TransLocQ:
	    return new PCEQTransLocQ( 0.99, 0.9, 0.01 );
	case MARL.dCEQ_TransLocQ:
	    return new DCEQTransLocQ( agent, 0.99, 0.9, 0.01 );
	case MARL.NashQ_TransLocQ:
	    return new NashQTransLocQ( 0.99, 0.9, 0.01 );
	case MARL.NEGOQ_TransLocQ:
	    return new NegoQTransLocQ( agent, 0.99, 0.9, 0.01 );
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
	    return new CoordinateLearning( agent, 0.99, 0.9, 0.01 );
	    //return new CoordinateLearning( agent, 0.1, 0.9, 0.01 );
	case MARL.Qlearning:
	    return new QL( agent, 0.99, 0.9, 0.01 );
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
	    return new UCEQAbsGame( 0.99, 0.9, 0.01 );
	case MARL.eCEQ_AbsGame:
	    return new ECEQAbsGame( 0.99, 0.9, 0.01 );
	case MARL.pCEQ_AbsGame:
	    return new PCEQAbsGame( 0.99, 0.9, 0.01 );
	case MARL.dCEQ_AbsGame:
	    return new DCEQAbsGame( agent, 0.99, 0.9, 0.01 );
	case MARL.NashQ_AbsGame:
	    return new NashQAbsGame( 0.99, 0.9, 0.01 );
	case MARL.NEGOQ_AbsGame:
	    return new NegoQAbsGame( agent, 0.99, 0.9, 0.01 );
	case MARL.NEGOQ_TransNSR:
	    return new NegoQTransNSR( agent, 0.99, 0.9, 0.01);
	case MARL.NEGOQ_AbsGame_NSR:
	    return new NegoQAbsGameNSR( agent, 0.99, 0.9, 0.01);
	case MARL.uCEQ_AbsGame_NSR:
	    return new UCEQAbsGameNSR( 0.99, 0.9, 0.01 );
	case MARL.uCEQ_AbsGame_NSR2:
	    return new UCEQAbsGameNSR2(0.99, 0.9, 0.01);
	case MARL.uCEQ_TransNSR:
	    return new UCEQTransNSR(0.99, 0.9, 0.01);
	case MARL.eCEQ_AbsGame_NSR:
	    return new ECEQAbsGameNSR(0.99, 0.9, 0.01);
	case MARL.eCEQ_TransNSR:
	    return new ECEQTransNSR(0.99, 0.9, 0.01);
	case MARL.pCEQ_AbsGame_NSR:
	    return new PCEQAbsGameNSR(0.99, 0.9, 0.01);
	case MARL.pCEQ_TransNSR:
	    return new PCEQTransNSR(0.99, 0.9, 0.01);
	case MARL.dCEQ_AbsGame_NSR:
	    return new DCEQAbsGameNSR(agent, 0.99, 0.9, 0.01);
	case MARL.dCEQ_TransNSR:
	    return new DCEQTransNSR(agent, 0.99, 0.9, 0.01);
	case MARL.NashQ_AbsGame_NSR:
	    return new NashQAbsGameNSR(0.99, 0.9, 0.01);
	case MARL.NashQ_TransNSR:
	    return new NashQTransNSR(0.99, 0.9, 0.01);
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
	
	SparseGridWorld sgw = new SparseGridWorld("./maps/PENTAGON.txt","./maps/PENTAGON_Info.txt");
	
	//multi-agent test
	sgw.oneTest();
	
	//single-agent test
	//sgw.oneRun_QLearningNSR(1);
	//sgw.oneRun_Rmax(1);
	//sgw.oneRun_SingleAgent(1);

    }
}
