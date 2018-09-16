package teamGamGam;

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
 * The definition of a gambling game
 * the advantage of this type of game is that 
 * the game state is independent of the agents 
 * so that the exponential growth in state space can be 
 * avoided.
 */
public class GamblingGame
{

    public static final int NUM_TEAMS = 2;
    
    /**
     * the number of agents in each team
     */
    public static final int NUM_TEAM_AGENTS = 7;//2;
    
    
    /**
     * Two teams
     */
    public static final int TEAM_A = 0;
    public static final int TEAM_B = 1;
    
    /**
     * the number of episodes in one game
     */
    public static final int matchNum = 5000;
    
    public static final int LOOP = 1;
    
    //private int actionOrder = 0;
    
    /**
     * all possible states and all possible joint actions
     */
    public static ArrayList<GameState> allStates;
    public static ArrayList<GameAction> allJointActions;
    
    /**
     * all possible joint actions of agents in a team
     */
    public static ArrayList<TeamAction> allTeamActions;
    
    public GamblingGame()
    {
	
	/**
	 * generate all possible state-action pairs
	 */
	generateAllStates();
	generateAllJointActions();
	generateTeamActions();
    }
    
    
    /**
     * the game is over when the pot size is 0
     */
    public boolean gameOver( GameState gameState ) 
    {
	
	return gameState.isTerminalState();	
    }
    
    
    public GameState doAction( GameState gameState, GameAction jntAction, double[] rewards )
    {
	if( gameState == null || jntAction == null ||
	    rewards == null || rewards.length != NUM_TEAMS ) {
	    
	    System.out.println( "@GamblingGame->doAction: Something Wrong in Parameters!" );
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
	
	
	//compare the bet size of the two teams
	int[] betSizes = new int[2]; 
	betSizes[0] = betSizes[1] = 0;
	for( int agentNoInTeam = 0; agentNoInTeam < NUM_TEAM_AGENTS; agentNoInTeam++ ) {
	    
	    int agentFirstTeam = agentNoInTeam;
	    int agentSecondTeam = agentNoInTeam + NUM_TEAM_AGENTS;
	    
	    betSizes[0] += jntAction.getAction( agentFirstTeam );
	    betSizes[1] += jntAction.getAction( agentSecondTeam );
	}
	
	//set the rewards
	if( betSizes[TEAM_A] == betSizes[TEAM_B]) {
	 
	    rewards[TEAM_A] = rewards[TEAM_B] = GameState.REWARD_SIZE / 2.0; 
	}
	else {
	    
	    int biggerTeam = TEAM_A;
	    if( betSizes[TEAM_A] < betSizes[TEAM_B] )
		biggerTeam = TEAM_B;
	    int smallerTeam = (biggerTeam+1) % NUM_TEAMS;
	    
	    if( next_bigsmall == GameState.BIG_WIN ) {
		
		rewards[biggerTeam] = GameState.REWARD_SIZE;
		rewards[smallerTeam] = 0.0;
	    }
	    else {
		
		rewards[smallerTeam] = GameState.REWARD_SIZE;
		rewards[biggerTeam] = 0.0;    
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
	int[] algTypes = new int[]{ //MARL.FFQ, 
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
	
	int LOOP = 1;
	
	/**
	 * choose the algorithm
	 */
	String algStr = MARL.ALG_STRINGS[algType];
	
	
	/**
	 * for recording steps
	 */
	double[][] allGoals = new double[NUM_TEAMS][matchNum];
	for( int team = 0; team < GamblingGame.NUM_TEAMS; team++ )
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
	    
	    double[][] retArray = new double[matchNum][GamblingGame.NUM_TEAMS];
	    for( int ep = 0; ep < matchNum; ep++ ) {
		    
		long startTime = System.nanoTime();
		    
		retArray[ep] = oneGame( teamA, teamB );
		   
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
     * @return the rewards in a game
     */
    public double[] oneGame( MARL teamA, MARL teamB )
    {
	/**
	 * for recording goals
	 */
	double[] retArray = new double[NUM_TEAMS];
	
	for( int team = 0; team < NUM_TEAMS; team++ ) {
	    
	    retArray[team] = 0.0;
	}
	
	
	for( int gameCount = 1; gameCount <= 1; gameCount++ ) {
	    
	    /**
	     * init the states
	     */
	    GameState gameState = new GameState( GameState.UPPER_BOUND_POT, 
		    GameState.SMALL_WIN );
	    
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
		
	    while( !gameOver( gameState ) ) {
		    
		
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
		GameState nextState = doAction( gameState, jntAction, rewards );
		
		//record the rewards
		for( int teamIndex = 0; teamIndex < NUM_TEAMS; teamIndex++ ) {
		    
		    retArray[teamIndex] += rewards[teamIndex];
		}
		
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
		gameState = nextState;
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
	     * move to the next action
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
    

    private void generateTeamActions( )
    {
	allTeamActions = new ArrayList<TeamAction>();
	
	//only agent in this team!!!
	int[] actionIterator = new int[NUM_TEAM_AGENTS];
	for( int agentNoInTeam = 0; agentNoInTeam < NUM_TEAM_AGENTS; agentNoInTeam++ ) {
	    
	    actionIterator[agentNoInTeam] = GameAction.BET_SMALL;
	}
	
	while( true ) {
	    
	    TeamAction teamAction = new TeamAction( actionIterator );
	    
	    if( !allTeamActions.contains( teamAction ) )
		allTeamActions.add( teamAction );
	    else
		teamAction = null;
	    
	    /**
	     * move to the next action
	     */
	    for( int agentNoInTeam = NUM_TEAM_AGENTS-1; agentNoInTeam >= 0; agentNoInTeam-- ) {
		
		actionIterator[agentNoInTeam] += 1;
		
		if( agentNoInTeam > 0 && actionIterator[agentNoInTeam] >= GameAction.NUM_ACTIONS ) {
		    
		    actionIterator[agentNoInTeam] = GameAction.BET_SMALL;
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
    
    public static int queryTeamActionIndex( GameState gameState, int teamIndex, 
	    TeamAction teamAction )
    {
	
	for( int index = 0; index < allTeamActions.size(); index++ ) {
	    
	    TeamAction tAction = allTeamActions.get( index );
	    
	    if( tAction.equals( teamAction ) ) {
		
		return index;
	    }
	}
	
	return -1;
    }
    
    
    
    /**
     * @param args
     */
    public static void main(String[] args)
    {
	// TODO Auto-generated method stub
	
	GamblingGame soccerGame = new GamblingGame();
	
	soccerGame.oneTest();

    }
}
