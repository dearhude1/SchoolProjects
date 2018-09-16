package soccerGame;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Random;
import algorithms.MARL;
import algorithms.FFQ;
import algorithms.FFQTrans;



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

    public static final int NUM_TEAMS = 2;
    
    /**
     * the number of agents in each team
     */
    public static final int NUM_TEAM_AGENTS = 3;//1;
    
    /**
     * each agent is located in an area
     */
    public static final int AREA_WIDTH = 4;//5;
    public static final int AREA_HEIGHT = 1;//2;
    public static final int AREA_LOCATIONS = 4;//5;//10;
    
    
    /**
     * Two teams
     */
    public static final int TEAM_A = 0;
    public static final int TEAM_B = 1;
    
    public static final int INIT_LOCATION_TEAM_A = 1;
    public static final int INIT_LOCATION_TEAM_B = 2;//8;
    
    /**
     * goal possession 
     */
    public static final int BALL_WITH_A = 0;
    public static final int BALL_WITH_B = 1;
    
    public static final int GOAL_LOC_A = 101;
    public static final int GOAL_LOC_B = 99;
    public static final int INVALID_LOCATION_ID = -10;
    
    /**
     * the location ID of each agent's goal
     */
    private int[] agentInitLocs;
    
    /**
     * the number of episodes in one game
     */
    public static final int matchNum = 500;
    
    public static final int LOOP = 1;
    
    //private int actionOrder = 0;
    
    /**
     * all possible states and all possible joint actions
     */
    public static ArrayList<GameState> allStates;
    public static ArrayList<GameAction> allJointActions;
    
    public SoccerGame()
    {
	agentInitLocs = new int[NUM_TEAM_AGENTS * NUM_TEAMS];
	 
	
	for( int agent = 0; agent < NUM_TEAM_AGENTS * NUM_TEAMS; agent++ ) {
	    
	    if( agent < NUM_TEAM_AGENTS )
		agentInitLocs[agent] = INIT_LOCATION_TEAM_A;
	    else
		agentInitLocs[agent] = INIT_LOCATION_TEAM_B;
	}
	
	/**
	 * generate all possible state-action pairs
	 */
	generateAllStates();
	generateAllJointActions();
	
    }
    
    public int[] getInitLocationIDs()
    {
	return agentInitLocs;
    }
    
    //right
    /**
     * if the agent which has the goal reaches the goal line 
     * the game then is over
     */
    public boolean gameOver( GameState gameState ) 
    {
	int[] agentLocationIDs = gameState.getLocationIDs();
	

	
	if( agentLocationIDs == null || 
		agentLocationIDs.length != NUM_TEAM_AGENTS * NUM_TEAMS ) {
	    
	    System.out.println("SoccerGame->gameOver: Game Over With Error!");
	    return false;
	}
	
	int possAgent = gameState.getBallPossession();
	int agentLocID = gameState.getLocationID( possAgent );
	
	if( reachGoal( agentLocID, possAgent ) ) {
	    
	    return true;
	}
	else
	    return false;
	
    }
    
    //right
    //note that there is no verification of action availability
    public int nextLocation( int location, int action )
    {
	//why goal location can enter here???
	if( location < 0 || location >= AREA_LOCATIONS ) {
	    
	    
	    if( location == GOAL_LOC_A || location == GOAL_LOC_B ) {
		
		System.out.println("@SoccerGame->nextLocation: Why goal line come here!!");
	    }
	    
	    System.out.println( "@SoccerGame->nextLocation: Parameter location Wrong! "+location);
	    return -1;
	}
	
	int locID = location;
	
	/**
	 * be careful
	 * we do not test collision and agent out of boundary
	 */
	if( action == GameAction.RIGHT )
	    locID += 1;
	else if( action == GameAction.LEFT )
	    locID -= 1;
	
	/**
	 * note that all action are permitted
	 * so deal with the goal line
	 */
	if( locID == AREA_LOCATIONS ) {
	    
	    //System.out.println("Dealing with this");
	    locID = GOAL_LOC_A;
	}
	if( locID == -1 ) {
	    
	    //System.out.println("Dealing with this");
	    locID = GOAL_LOC_B; 
	}
	
	
	return locID;
    }
    

    
    //right
    /**
     * whether an agent has reached its goal line
     * @param locID
     * @return
     */
    public boolean reachGoal( int locID, int agentWithBall )
    {
	int team = getAgentTeam( agentWithBall );
	
	if( team == TEAM_A && locID == GOAL_LOC_A ) {
	    
	    return true;
	}
	else if( team == TEAM_B && locID == GOAL_LOC_B ) {
	    
	    return true;
	}
	else 
	    return false;
    }
    
    //seems right
    /**
     * normal version of doAction
     * 
     * check again
     * do actions and set the reward
     */
    //the assumption is the all actions should be available 
    public GameState doAction( GameState gameState, GameAction jntAction, double[] rewards, 
	    int[] steps )
    {
	if( gameState == null || jntAction == null ||
	    rewards == null || rewards.length != NUM_TEAMS ) {
	    
	    System.out.println( "@SoccerGame->doAction: Something Wrong in Parameters!" );
	    return null;
	}
	
	
	/**
	 * the returned state
	 */
	//directly operate on the current state??
	//GameState nextState = new GameState(gameState.getLocationIDs(), gameState.getBallPossession());
	
	/**
	 * first execute the actions in random order
	 */
	int firstTeam = new Random().nextInt( NUM_TEAMS );
	int secondTeam = (firstTeam+1) % NUM_TEAMS;
	int curBallAgent = gameState.getBallPossession();
	
	/**
	 * executing action for each agent 
	 * first loop for the agent has ball
	 * second loop for agents without ball
	 */
	for( int agentNoInTeam = 0; agentNoInTeam < NUM_TEAM_AGENTS; agentNoInTeam++ ) {
	    
	    int agentFirstTeam = agentNoInTeam + firstTeam * NUM_TEAM_AGENTS;
	    int agentSecondTeam = agentNoInTeam + secondTeam * NUM_TEAM_AGENTS;
	    
	    if( agentFirstTeam != curBallAgent && 
		    agentSecondTeam != curBallAgent )
		continue;
	    
	   int curLocID_FirstAgent = gameState.getLocationID( agentFirstTeam );
	   int curLocID_SecondAgent = gameState.getLocationID( agentSecondTeam );
	   
	   //first agent has the ball
	   if( agentFirstTeam == curBallAgent ) {
	       
	       /**
	        * first agent takes its action
	        */
	       //if the action is pass the ball
	       if( jntAction.getAction( agentFirstTeam ) == GameAction.PASS ) {
		       
		   int nextBallPossAgent = passBall( agentFirstTeam, gameState );
		       
		   //directly operate on the current state
		   gameState.setBallPossession( nextBallPossAgent );
		   gameState.setLocationID( agentFirstTeam, curLocID_FirstAgent );
		   
	       }
	       //else just move the first agent
	       else { 
		   
		   int nextLocID_FirstAgent = nextLocation( curLocID_FirstAgent, 
			   jntAction.getAction( agentFirstTeam ) );
		       
		   /**
		    * if reach the goal line
		    */
		   if( reachGoal( nextLocID_FirstAgent, agentFirstTeam ) ) {
			   
		       gameState.setLocationID( agentFirstTeam, nextLocID_FirstAgent );
			   
		       //set reward
			   
		       //return??
		   }
		       
		   /**
		    * else enter the opponent's grid
		    * then with 40%, the opponent is passed
		    * with 60%, the ball is chased by the opponent, and first agent remain still
		    * then the ball will belong to the second agent
		    */
		   else if( nextLocID_FirstAgent == curLocID_SecondAgent ) {
			   
		       double rndPro = new Random().nextDouble();
		       
		       //the opponent is passed
		       //but it can catch up in its move
		       //since both agents should move 
		       //there will not be situations in which the two agents overlap
		       if( rndPro <= 0.4 ) {
			   
			   gameState.setLocationID( agentFirstTeam, nextLocID_FirstAgent );
		       }
		       //remain still
		       else {
			   
			   gameState.setLocationID( agentFirstTeam, curLocID_FirstAgent );
			   gameState.setBallPossession( agentSecondTeam );
		       }
		   }
		   else {
			   
		       gameState.setLocationID( agentFirstTeam, nextLocID_FirstAgent );
		   }
	       }
		   
	       /**
	        * move the second agent
	        */
	       int nextLocID_SecondAgent = nextLocation( curLocID_SecondAgent, 
		       jntAction.getAction( agentSecondTeam ) );
	       
	       
	       /**
	        * currently, even if the second agent chases the ball
	        * it can not pass the goal line since its available action set 
	        * does contain such action
	        */
	       //if the second agent goes to the next loction of the first agent
	       if( nextLocID_SecondAgent == gameState.getLocationID( agentFirstTeam ) ) {
		   
		   /**
		    * if the second agent has chased the ball, then it will be back to the first agent,
		    * also, it cannot move
		    */
		   if( gameState.getBallPossession() == agentSecondTeam ) {
		       
		       gameState.setBallPossession( agentFirstTeam );
		   }
		   gameState.setLocationID( agentSecondTeam, curLocID_SecondAgent );
		   
	       }
	       //else just move the second agent
	       else {
		   
		   gameState.setLocationID( agentSecondTeam, nextLocID_SecondAgent );
		   
		   //here!!!
		   //System.out.println("next "+agentSecondTeam+" "+nextLocID_SecondAgent);
		   //System.out.println("current "+agentSecondTeam+" "+curLocID_SecondAgent);
		   //System.out.println("action: "+jntAction.getAction(agentSecondTeam));
	       }
	   }
	   //second agent has the ball
	   else {
	       
	       /**
	        * firstly move the first agent
	        */
	       int nextLocID_FirstAgent = nextLocation( curLocID_FirstAgent, 
		       jntAction.getAction( agentFirstTeam ) );
	       
	       //if collapse with the second agent, remain still
	       if( nextLocID_FirstAgent == curLocID_SecondAgent ) {
		   
		   gameState.setLocationID( agentFirstTeam, curLocID_FirstAgent );
	       }
	       //else just move
	       else 
		   gameState.setLocationID( agentFirstTeam, nextLocID_FirstAgent );
	       
	       /**
	        * take actions for the second agent
	        */
	       //if the second agent pass the ball
	       if( jntAction.getAction( agentSecondTeam ) == GameAction.PASS ) {
		   
		   int nextBallPossAgent = passBall( agentSecondTeam, gameState );
		       
		   //directly operate on the current state
		   gameState.setBallPossession( nextBallPossAgent );
		   gameState.setLocationID( agentSecondTeam, curLocID_SecondAgent );
	       }
	       //else just move the second agent
	       else {
		   
		   int nextLocID_SecondAgent = nextLocation( curLocID_SecondAgent, 
			   jntAction.getAction( agentSecondTeam ) );
		   
		   
		   /**
		    * if the second agent pass the goal line
		    */
		   if( reachGoal( nextLocID_SecondAgent, agentSecondTeam ) ) {
		       
		       gameState.setLocationID( agentSecondTeam, nextLocID_SecondAgent );
			   
		       //set reward
			   
		       //return??
		   }
		   /**
		    * else if the second agent goes into the next grid of the first agent
		    * then with 40%, the second passes
		    * with 60%, the ball is chased by the first agent, and second remains
		    * then the ball will belong to the second agent
		    */
		   else if( nextLocID_SecondAgent == gameState.getLocationID( agentFirstTeam ) ) {
		       
		       double rndPro = new Random().nextDouble();
		       
		       //they can exchange locations
		       if( rndPro <= 0.4 ) {
			   
			   gameState.setLocationID( agentSecondTeam, nextLocID_SecondAgent );
			   gameState.setLocationID( agentFirstTeam, curLocID_SecondAgent );
		       }
		       //the ball is chased by the first agent
		       else {
			   
			   gameState.setBallPossession( agentFirstTeam );
			   gameState.setLocationID( agentSecondTeam, curLocID_SecondAgent );
		       }

		   }
		   /**
		    * else just move
		    */
		   else {
		       
		       gameState.setLocationID( agentSecondTeam, nextLocID_SecondAgent );
		   }   
	       }
	   }
	}//first loop//////////////////////////////////////////
	
	
	//second loop for agents previously without the ball
	for( int agentNoInTeam = 0; agentNoInTeam < NUM_TEAM_AGENTS; agentNoInTeam++ ) {
	    
	    int agentFirstTeam = agentNoInTeam + firstTeam * NUM_TEAM_AGENTS;
	    int agentSecondTeam = agentNoInTeam + secondTeam * NUM_TEAM_AGENTS;
	    
	    if( agentFirstTeam == curBallAgent || 
		    agentSecondTeam == curBallAgent )
		continue;
	    
	    int curLocID_FirstAgent = gameState.getLocationID( agentFirstTeam );
	    int curLocID_SecondAgent = gameState.getLocationID( agentSecondTeam );
		   
	    /**
	     * first executing the action of the first agent
	     * since previously it cannot choose pass action 
	     * so just move this agent
	     * also it cannot pass the goal line since this action is not available
	     */
	    int nextLocID_FirstAgent  = nextLocation( curLocID_FirstAgent, 
		    jntAction.getAction( agentFirstTeam ) );
	    
	    /**
	     * if this agent enters the current loc of the second agent
	     * 
	     * in this situation, with 30% the second agent is passed 
	     * with 70% the ball is chased by the second agent
	     */
	    if( nextLocID_FirstAgent == curLocID_SecondAgent ) {
			   
		if( gameState.getBallPossession() == agentFirstTeam ) {
		    
		    double rndPro = new Random().nextDouble();
		    //the second agent is passed
		    if( rndPro <= 0.3 ) {
			
			gameState.setLocationID( agentFirstTeam, nextLocID_FirstAgent );
		    }
		    //the ball is chased by the second agent
		    else {
			
			gameState.setBallPossession( agentSecondTeam );
			gameState.setLocationID( agentFirstTeam, curLocID_FirstAgent );
		    }
		}
		//else just remain still
		else
		    gameState.setLocationID( agentFirstTeam, curLocID_FirstAgent );
	    }
	    /**
	     * else just move this agent
	     */
	    else {
		
		gameState.setLocationID( agentFirstTeam, nextLocID_FirstAgent );
	    }
	    
	    /**
	     * then executing the action of the second agent
	     */
	    int nextLocID_SecondAgent = nextLocation( curLocID_SecondAgent, 
		    jntAction.getAction( agentSecondTeam ) );
	    
	    /**
	     * if the second agent enters the next location of the first agent
	     * 
	     */
	    if( nextLocID_SecondAgent == gameState.getLocationID( agentFirstTeam ) ) {
		
		/**
		 * if it has the ball,
		 * then with 30%, the first agent is passed, and they change locations
		 * with 70%, the ball is chased by the first agent, the second agent remain
		 */
		if( gameState.getBallPossession() == agentSecondTeam ) {
		    
		    double rndPro = new Random().nextDouble();
		    //the first agent is passed
		    if( rndPro <= 0.3 ) {
			
			gameState.setLocationID( agentSecondTeam, nextLocID_SecondAgent );
			gameState.setLocationID( agentFirstTeam, curLocID_SecondAgent );
		    }
		    else {
			
			gameState.setBallPossession( agentFirstTeam );
			gameState.setLocationID( agentSecondTeam, curLocID_SecondAgent );
		    }
		}
		else
		    gameState.setLocationID( agentSecondTeam, curLocID_SecondAgent );
	    }
	    /**
	     * else, just move
	     */
	    else {
		
		gameState.setLocationID( agentSecondTeam, nextLocID_SecondAgent );
	    }
	}//second loop////////////////////////////////////////////////////////////////////
	
	/**
	 * set the reward for each agent
	 */
	if( gameOver( gameState ) ) {
	    
	    
	    //only two goal states
	    //Team A or Team B
	    
	    int possTeam = gameState.getBallPossTeam();
	    for( int team = 0; team < NUM_TEAMS; team++ ) {
		
		if( possTeam == team )
		    rewards[team] = 1000;
		else
		    rewards[team] = -1000;
	    }
	}
	else {
	    
	    for( int team = 0; team < NUM_TEAMS; team++ )
		rewards[team] = 0;
	}

	//return gameState ok??
	return gameState;
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
    
    //check again
    public static boolean[] actionSet( int agent, GameState gameState )
    {
	if( gameState == null || agent < 0 || 
		agent >= NUM_TEAM_AGENTS * NUM_TEAMS ) {
	    
	    System.out.println("SoccerGame->actionSet: Wrong Parameters!");
	    return null;
	}
	
	/**
	 * we need these variables
	 */
	int agentLoc = gameState.getLocationID( agent );
	int agentTeam = getAgentTeam( agent );
	int possAgent = gameState.getBallPossession();
	
	boolean[] actAvail = new boolean[GameAction.NUM_ACTIONS];
	for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
	    
	    actAvail[act] = true; 
	}
	
	//int availNum = GameAction.NUM_ACTIONS;
	
	
	/**
	 * the action for changing columns
	 */
	if( agentTeam == TEAM_A ) {
	    
	    if( agentLoc == 0 ) {
		    
		actAvail[GameAction.LEFT] = false;
		//availNum -= 1;
	    }
	    if( agentLoc == AREA_WIDTH-1 ) {
		    
		if( possAgent == agent ) {
		    
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
	    
	    if( agentLoc == AREA_WIDTH-1 ) {
		
		actAvail[GameAction.RIGHT] = false;
		//availNum -= 1;
	    }
	    if( agentLoc == 0 ) {
		
		if( possAgent == agent ) {
		    
		    actAvail[GameAction.LEFT] = true;
		}
		else {
		    
		    actAvail[GameAction.LEFT] = false;
		    //availNum -= 1;
		}
	    }
	}

	
	/**
	 * the action for passing ball
	 */
	if( agent == possAgent ) {
	    
	    //whether a teammate can receive the ball
	    boolean canPass = false;
	    for( int teammate = agentTeam * NUM_TEAM_AGENTS; teammate < (agentTeam+1) * NUM_TEAM_AGENTS; teammate++ ) {
		
		/**
		 * only neighour agents can pass the ball
		 */
		if( teammate == agent || 
			Math.abs(teammate-agent) != 1 ) 
		    continue;
		
		int teammateLoc = gameState.getLocationID( teammate );
		
		if( Math.abs(teammateLoc-agentLoc) == 1 ) {
		    
		    canPass = true;
		    break;
		}
	    }
	    actAvail[GameAction.PASS] = canPass;
	}
	else 
	    actAvail[GameAction.PASS] = false;
	
	return actAvail;
    }
    
    
    //right
    //no verification for goal line
    public boolean outOfBoundary( int location )
    {
	if( location < 0 || location >= AREA_LOCATIONS )
	    return true;
	
	else 
	    return false;
    }
    
    //right
    private int passBall( int curPossAgent, GameState gameState )
    {
	if( gameState == null || curPossAgent < 0 ||
		curPossAgent >= NUM_TEAMS * NUM_TEAM_AGENTS ) {
	    
	    System.out.println("passBall: Wrong Parameters!");
	    return -1;
	}
	if( curPossAgent != gameState.getBallPossession() ) {
	    
	    System.out.println("passBall: This agent is not having a ball!");
	    return -1;
	}
	
	int team = getAgentTeam( curPossAgent );
	int locPossAgent = gameState.getLocationID( curPossAgent );
	
	for( int teammate = team * NUM_TEAM_AGENTS; teammate < (team+1) * NUM_TEAM_AGENTS; teammate++ ) {
	    
	    if( teammate == curPossAgent || 
		    Math.abs(teammate-curPossAgent) != 1)
		continue;
	    
	    int locTmm = gameState.getLocationID( teammate );
	    
	    if( Math.abs(locTmm-locPossAgent) == 1  ) {
		
		return teammate;
	    }
	}
	
	System.out.println("The ball can be passed to no agent!!");
	return -1;
	
    }
    
    /**
     * one test contains several algorithms run
     */
    public void oneTest()
    {
	int[] algTypes = new int[]{ MARL.FFQ, 
		MARL.FFQ_TRANS  
		};

	
	for( int algIndex = 0; algIndex < algTypes.length; algIndex++ ) {
	    
	    int algType = algTypes[algIndex];
	    oneRun( algType );
	}
    }
    
    /**
     * one run contains one algorithm's several episodes 
     * 
     * no centralized version
     */
    public void oneRun( int algType )
    {
	
	int LOOP = 5;
	
	/**
	 * choose the algorithm
	 */
	String algStr = MARL.ALG_STRINGS[algType];
	
	
	/**
	 * for recording steps
	 */
	double[][] allGoals = new double[NUM_TEAMS][matchNum];
	for( int team = 0; team < SoccerGame.NUM_TEAMS; team++ )
	    for( int ep = 0; ep < matchNum; ep++ ) {
		
		allGoals[team][ep] = 0;
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
	     * for decentralized algorithms
	     */
	    MARL teamA = createMARL( algType, TEAM_A );
	    MARL teamB = createMARL( MARL.RANDOM, TEAM_B );
	    
	    
	    
	    /**
	    if( isCentral )
		agent.gameStarted(loop);
	    else {
		
		agent0.gameStarted(loop);
		//agent1.gameStarted(loop);
	    }
	    */
	    
	    double[][] retArray = new double[matchNum][SoccerGame.NUM_TEAMS];
	    for( int ep = 0; ep < matchNum; ep++ ) {
		    
		long startTime = System.nanoTime();
		    
		retArray[ep] = oneMatch( teamA, teamB );
		   
		long endTime = System.nanoTime();
		durTimes[ep] = endTime - startTime;
		   
		for( int team = 0; team < NUM_TEAMS; team++ ) {
		    
		    allGoals[team][ep] += retArray[ep][team];
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
	    for( int team = 0; team < NUM_TEAMS; team++ ) {
		
		BufferedWriter rewardsWriter = new BufferedWriter(new FileWriter("./goals_" + algStr+"_team" + team + ".csv"));
		for( int ep = 0; ep < matchNum; ep++ ) {
		    
		    rewardsWriter.write( allGoals[team][ep] / LOOP + ", ");
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
    public double[] oneMatch( MARL teamA, MARL teamB )
    {
	/**
	 * for recording goals
	 */
	double[] retArray = new double[NUM_TEAMS];
	
	for( int team = 0; team < NUM_TEAMS; team++ ) {
	    
	    retArray[team] = 0.0;
	}
	
	
	for( int gameCount = 1; gameCount <= 10; gameCount++ ) {
	    
	    
	    
	    /**
	     * init the states
	     */
	    int gameStep = 0;
	    GameState gameState = new GameState( agentInitLocs, 
		    new Random().nextInt(NUM_TEAM_AGENTS * NUM_TEAMS) );
	    
	    /**
	     * compute the available action set for each agent
	     * get the action expected taken by each agent
	     */
	    GameAction jntAction_TeamA = teamA.updateQ( null, null, null, gameState );
	    GameAction jntAction_TeamB = teamA.updateQ( null, null, null, gameState );
		
	    /**
	     * the action actually taken
	     */
	    GameAction jntAction = new GameAction();
		
	    /**
	     * the reward for each agent in each transfer
	     */
	    double[] rewards = new double[NUM_TEAMS];
	    for( int team = 0; team < NUM_TEAMS; team++ )
		rewards[team] = 0.0;
		
	    while( !gameOver( gameState ) && gameStep < 50 ) {
		    
		gameStep++;
		
		/**
		 * epsilon-greedy and get the action to be taken actually
		 * 
		 * set the action for each
		 */
		jntAction_TeamA = teamA.epsilonGreedy(gameState, jntAction_TeamA);
		jntAction_TeamB = teamB.epsilonGreedy(gameState, jntAction_TeamB);
		for( int agentNoInTeam = 0; agentNoInTeam < NUM_TEAM_AGENTS; agentNoInTeam++ ) {
		    
		    int agentIndex_A = TEAM_A * NUM_TEAM_AGENTS + agentNoInTeam;
		    int agentIndex_B = TEAM_B * NUM_TEAM_AGENTS + agentNoInTeam;
		    
		    jntAction.setAction(agentIndex_A, jntAction_TeamA.getAction(agentIndex_A));
		    jntAction.setAction(agentIndex_B, jntAction_TeamB.getAction(agentIndex_B));
		}
		    
		/**
		 * observe the next state and get the rewards
		 */
		GameState nextState = doAction( gameState, jntAction, rewards, null );
		
		if( !gameOver( nextState ) ) {
		    
		    /**
		     * update Q-values
		     */
		    jntAction_TeamA = teamA.updateQ( gameState, jntAction, rewards, nextState );
		    jntAction_TeamB = teamB.updateQ( gameState, jntAction, rewards, nextState );
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
		retArray[getAgentTeam(gameState.getBallPossession())] += 1.0;
	    }
	}
	
	return retArray;
    }
    
    /**
     * 
     * @return two arrays, one for steps, one for rewards
     *
    public double[] oneMatchCentral( MARL agent )
    {
	double[] retArray = new double[NUM_AGENTS];
	
	for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
	    
	    retArray[agentIndex] = 0.0;
	}
	
	for( int gameCount = 1; gameCount <= 10; gameCount++ ) {
	    
	    int gameStep = 0;
	    GameState gameState = new GameState( agentInitLocs, new Random().nextInt(NUM_AGENTS) );
		

	    GameAction jntAction = agent.updateQ( null, null, null, gameState );
		

	    double[] rewards = new double[NUM_AGENTS];
	    for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ )
		rewards[agentIndex] = 0.0;
		
	    while( !gameOver( gameState ) && gameStep < 50 ) {
		    
		gameStep++;

		jntAction = agent.epsilonGreedy(gameState, jntAction);
		    
		    

		GameState nextState = doAction( gameState, jntAction, rewards, null );
		    
		    

		GameAction nextAction = agent.updateQ( gameState, jntAction, rewards, nextState );
		    

		gameState = null; //??
		gameState = nextState;
		jntAction = null; //??
		jntAction = nextAction;
		
	    }
	    

	    if( gameOver( gameState ) ) {
		    
		//the one has the ball goals
		retArray[gameState.getBallPossession()] += 1.0;
	    }
	}
	
	return retArray;
    }
    */
    
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
	case MARL.FFQ:
	    return new FFQ( agent );
	case MARL.FFQ_TRANS:
	    return new FFQTrans( agent );
	default:
	    return new MARL( agent );
	}
    }
    
    
    public static int getAgentTeam( int agent ) 
    {
	if( agent < 0 || agent >= NUM_TEAM_AGENTS * NUM_TEAMS ) {
	 
	    System.out.println("getAgentTeam: Wrong Parameter!");
	    return -1;
	}
	else {
	    
	    int team = agent / NUM_TEAM_AGENTS;
	    return team;
	}
    }
    
    //check again
    private void generateAllStates()
    {
	
	allStates = new ArrayList<GameState>();
	
	int[] stateIterator = new int[NUM_TEAM_AGENTS * NUM_TEAMS];
	int ballPossAgent = 0;
	
	for( int agent = 0; agent < NUM_TEAM_AGENTS * NUM_TEAMS; agent++ ) {
	    
	    stateIterator[agent] = 0; //loc 0
	}
	
	/**
	 * for each agent 
	 */
	
	while( true ) {
	    
	    /**
	     * check the current location
	     */
	    if( availableState( stateIterator, ballPossAgent ) ) {
		
		
		GameState gameState = new GameState( stateIterator, ballPossAgent );
		
		if( !allStates.contains( gameState ) ) {
		    
		    allStates.add( gameState );
		}
		else {
		    
		    
		    gameState = null;
		}
	    }
	    
	    
	    /**
	     * move to the next state
	     * first ballPossAgent increases
	     * if it is larger than agent number, then the state increases
	     */
	    ballPossAgent++;
	    if( ballPossAgent >= NUM_TEAM_AGENTS * NUM_TEAMS ) {
		
		ballPossAgent = 0;
		
		for( int agent = NUM_TEAM_AGENTS * NUM_TEAMS-1; agent >= 0; agent-- ) {
			
		    stateIterator[agent] += 1;
			
		    if( agent > 0 && stateIterator[agent] >= AREA_LOCATIONS ) {
			    
			stateIterator[agent] = 0;
		    }
		    else
			break;
		}
		/**
		 * check the stop condition
		 */
		if( stateIterator[0] >= AREA_LOCATIONS ) {
			
		    break;
		}
	    }
	}
	
	
	/**
	 * note that the goal states are not contained in this list
	 */
    }
    
    //check again
    private boolean availableState( int[] locationIDs, int ballPossAgent ) 
    {
	
	if( locationIDs == null || 
		locationIDs.length != NUM_TEAM_AGENTS * NUM_TEAMS || 
		ballPossAgent < 0 || ballPossAgent >= NUM_TEAM_AGENTS * NUM_TEAMS ) {
	    
	    System.out.println("availableState: Wrong Parameter!");
	    return false;
	}
	
	
	for( int agentNoInTeam = 0; agentNoInTeam < NUM_TEAM_AGENTS; agentNoInTeam++ ) {
	    
	    int agentTeamA = TEAM_A * NUM_TEAM_AGENTS + agentNoInTeam;
	    int agentTeamB = TEAM_B * NUM_TEAM_AGENTS + agentNoInTeam;
	    int locA = locationIDs[agentTeamA];
	    int locB = locationIDs[agentTeamB];
	    
	    //invalid locations
	    if( locA == INVALID_LOCATION_ID || 
		    locB == INVALID_LOCATION_ID )
		return false;
	    
	  //whether they collapse
	    if( locA >= 0 && locA < AREA_LOCATIONS && 
		    locB >= 0 && locB < AREA_LOCATIONS ) {
		
		if( locA == locB )
		    return false;
	    }
	}
	
	return true;
    }
    
    //needs done
    private void generateAllJointActions()
    {
	allJointActions = new ArrayList<GameAction>();
	
	int[] actionIterator = new int[NUM_TEAM_AGENTS * NUM_TEAMS];
	
	for( int agent = 0; agent < NUM_TEAM_AGENTS * NUM_TEAMS; agent++ ) {
	    
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
	     * move to the next location
	     */
	    for( int agent = NUM_TEAM_AGENTS * NUM_TEAMS-1; agent >= 0; agent-- ) {
		
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
    
    //check again and again
    //generate all available joint actions of a team
    //but the return type is the joint action of all agents
    public static ArrayList<TeamAction> generateAvailTeamActions( GameState gameState, int teamIndex )
    {
	if( gameState == null || 
		teamIndex < 0 || 
		teamIndex >= NUM_TEAMS ) {
	    
	    System.out.println("SoccerGame->generateAvailTeamActions: Wrong Parameters");
	    return null;
	}
	
	
	ArrayList<TeamAction> retList = new ArrayList<TeamAction>();
	
	//only agent in this team!!!
	int[] actionIterator = new int[NUM_TEAM_AGENTS];
	for( int agentNoInTeam = 0; agentNoInTeam < NUM_TEAM_AGENTS; agentNoInTeam++ ) {
	    
	    actionIterator[agentNoInTeam] = 0;
	}
	
	while( true ) {
	    
	    TeamAction teamAction = new TeamAction( actionIterator );
	    
	    /**
	     * check the teamAction availability
	     */
	    boolean bAvail = true;
	    for( int agentNoInTeam = 0; agentNoInTeam < NUM_TEAM_AGENTS; agentNoInTeam++ ) {
		
		int agentIndex = teamIndex * NUM_TEAM_AGENTS + agentNoInTeam;
		
		int action = teamAction.getAction( agentNoInTeam );
		
		if( !actionAvail( gameState, agentIndex, action ) ) {
		    
		    bAvail = false;
		    break;
		}
	    }
	    
	    /**
	     * if available, then add this team action
	     */
	    if( bAvail ) {
		
		if( !retList.contains( teamAction ) )
		    retList.add( teamAction );
	    }
	    else
		teamAction = null;
	    
	    /**
	     * move to the next action
	     */
	    for( int agentNoInTeam = NUM_TEAM_AGENTS-1; agentNoInTeam >= 0; agentNoInTeam-- ) {
		
		actionIterator[agentNoInTeam] += 1;
		
		if( agentNoInTeam > 0 && actionIterator[agentNoInTeam] >= GameAction.NUM_ACTIONS ) {
		    
		    actionIterator[agentNoInTeam] = 0;
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
	
	
	
	return retList;
    }
    
    //check again and again
    public static int queryTeamActionIndex( GameState gameState, int teamIndex, TeamAction teamAction )
    {
	
	ArrayList<TeamAction> availTeamActions = generateAvailTeamActions(gameState, teamIndex);
	
	for( int index = 0; index < availTeamActions.size(); index++ ) {
	    
	    TeamAction tAction = availTeamActions.get( index );
	    
	    if( tAction.equals( teamAction ) ) {
		
		//release the memory
		availTeamActions = null;
		
		return index;
	    }
	}
	
	//release the memory
	availTeamActions = null;
	
	return -1;
    }
    
    public static boolean actionAvail( GameState gameState, int agentIndex, int action ) 
    {
	
	int ballPossAgent = gameState.getBallPossession();
	
	int team = getAgentTeam( agentIndex );
	int agentLoc = gameState.getLocationID( agentIndex );
	
	//go left
	if( action == GameAction.LEFT ) {
	    
	    if( agentLoc == 0 ) {
		
		if( team == TEAM_B && agentIndex == ballPossAgent )
		    return true;
		else
		    return false;
	    }
	    else 
		return true;
	}
	//go right
	else if( action == GameAction.RIGHT ) {
	    
	    if( agentLoc == AREA_WIDTH-1 ) {
		
		if( team == TEAM_A && agentIndex == ballPossAgent )
		    return true;
		else
		    return false;
	    }
	    else 
		return true;
	}
	//pass the ball
	else if( action == GameAction.PASS ) {
	    
	    if( agentIndex != ballPossAgent ) 
		return false;
	    else
		return canPassBall( agentIndex, gameState );
	}
	//other actions??
	else
	    return false;
    }
    
    //check again
    public static boolean canPassBall( int curPossAgent, GameState gameState )
    {
	if( gameState == null || curPossAgent < 0 ||
		curPossAgent >= NUM_TEAMS * NUM_TEAM_AGENTS ) {
	    
	    System.out.println("canPassBall: Wrong Parameters!");
	    return false;
	}
	if( curPossAgent != gameState.getBallPossession() ) {
	    
	    System.out.println("canPassBall: This agent is not having a ball!");
	    return false;
	}
	
	int team = getAgentTeam( curPossAgent );
	int locPossAgent = gameState.getLocationID( curPossAgent );
	
	for( int teammate = team * NUM_TEAM_AGENTS; teammate < (team+1) * NUM_TEAM_AGENTS; teammate++ ) {
	    
	    if( teammate == curPossAgent || 
		    Math.abs(teammate-curPossAgent) != 1)
		continue;
	    
	    int locTmm = gameState.getLocationID( teammate );
	    
	    if( Math.abs(locTmm-locPossAgent) == 1 ) {
		
		return true;
	    }
	}

	return false;
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
