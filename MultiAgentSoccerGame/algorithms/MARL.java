package algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import soccerGame.GameAction;
import soccerGame.GameState;
import soccerGame.SoccerGame;
import soccerGame.StateActionPair;



/**
 * the base class of all MARL algorithms
 * @author dearhude1
 *
 */
public class MARL
{

    public static final int FFQ = 0;
    public static final int FFQ_TRANS = 1;	
    public static final int RANDOM = 2;
    
    public static final String[] ALG_STRINGS = { "FFQ", "FFQTrans", "Random" };
    
    /**
     * fundamental parameter of MARL algorithms
     */
    //we are now conducting preliminary experiments
    protected double ALPHA = 0.9;//0.9;
    protected double GAMMA = 0.9;//0.7;
    protected double EPSILON = 0.1;//0.8;//0.01;//0.1;//0.025;
    //0.01 for HAMMQ against random opponent
    //0.8 for minimaxQ opponent for all algorithms
    //0.1 for minimaxQ and minimaxQTrans against random opponents
    
    double dynEpsilon = 1.0;
    protected double explorationTime = 0.0;
    
    /**
     * for random use
     */
    protected Random random;
    
    /**
     * Q-tables for all agents
     * each entry store each agent's Q-value for a state-action pair
     * 
     * note the each entry contains an array of all team members' values
     * its length is the number of agents in the team
     */
    protected HashMap<StateActionPair, Double> Qs;

    protected HashMap<StateActionPair, Integer> vstNum;
    
    /**
     * this algorithm should decide for all agents in the team
     */
    protected int teamIndex;
    
    //right
    public MARL( int index )
    {
	teamIndex = index;
	random = new Random();
	
	 
	
	/**
	 * get all states and all joint actions from LinearGridWorld class
	 */
	ArrayList<GameState> allStates = SoccerGame.allStates;
	ArrayList<GameAction> allActions = SoccerGame.allJointActions;
	
	/**
	 * init the Q-table
	 */	
	Qs = new HashMap<StateActionPair, Double>();
	vstNum = new HashMap<StateActionPair, Integer>();
	
	 System.out.println("Here we go");
	 System.out.println("All States: "+allStates.size());
	 System.out.println("All Actions: "+allActions.size());
	
	for( int stateIndex = 0; stateIndex < allStates.size(); stateIndex++ ) {
	    
	    GameState gameState = allStates.get( stateIndex );
	    
	   
	    
	    for( int actionIndex = 0; actionIndex < allActions.size(); actionIndex++ ) {
		
		GameAction jntAction = allActions.get( actionIndex );
		StateActionPair saPair = new StateActionPair( gameState, jntAction );
		
		
		if( !Qs.containsKey( saPair ) ) {
		    
		    Qs.put( saPair, random.nextDouble() );
		    vstNum.put( saPair, 0 );
		}
		else {
		    
		    saPair = null;
		}
	    }
	}
	
	
    }
    
    //right
    public MARL( int index, double alpha, double gamma, double epsilon )
    {
	teamIndex = index;
	random = new Random();
	
	ALPHA = alpha;
	GAMMA = gamma;
	EPSILON = epsilon;
	
	/**
	 * get all states and all joint actions from LinearGridWorld class
	 */
	ArrayList<GameState> allStates = SoccerGame.allStates;
	ArrayList<GameAction> allActions = SoccerGame.allJointActions;
	
	/**
	 * init the Q-table
	 */	
	/**
	 * init the Q-table
	 */	
	Qs = new HashMap<StateActionPair, Double>();
	vstNum = new HashMap<StateActionPair, Integer>();
	
	for( int stateIndex = 0; stateIndex < allStates.size(); stateIndex++ ) {
	    
	    GameState gameState = allStates.get( stateIndex );
	    
	    for( int actionIndex = 0; actionIndex < allActions.size(); actionIndex++ ) {
		
		GameAction jntAction = allActions.get( actionIndex );
		StateActionPair saPair = new StateActionPair( gameState, jntAction );
		
		
		if( !Qs.containsKey( saPair ) ) {
		    
		    Qs.put( saPair, random.nextDouble() );
		    vstNum.put( saPair, 0 );
		}
		else {
		    
		    saPair = null;
		}
	    }
	}
    }
    
    /**
     * Compute the action for the current state
     * @return
     *
    public int getAction( GameState gameState )
    {
	return random.nextInt( GameAction.NUM_ACTIONS );
    }
    */
    
    //check again
    /**
     * epsilon-greedy for one agent
     * @param action
     * @return
     */
    public int[] epsilonGreedy( GameState gameState, int[] teamActions )
    {
	/**/
	double epsilon = 0.0;
	if( explorationTime < 0.01 )
	    epsilon = dynEpsilon;
	else {
	    
	    epsilon = Math.pow(1/explorationTime, 0.100001);
	    if( epsilon < dynEpsilon )
		epsilon = dynEpsilon;
	}
	
	
	if( random.nextDouble() < EPSILON ) { 
		
	    //System.out.println(dynEpsilon);
	    
	    /**
	     * exploration one time
	     */
	    dynEpsilon *= 0.99999;//0.99999;
	    explorationTime += 1.0;
	    
	    /**
	     * for each agent in this team
	     */
	    int[] newActions = new int[SoccerGame.NUM_TEAM_AGENTS];
	    for( int agentNoInTeam = 0; agentNoInTeam < SoccerGame.NUM_TEAM_AGENTS; agentNoInTeam++ ) {
		
		int agentIndex = teamIndex * SoccerGame.NUM_TEAM_AGENTS + agentNoInTeam;
		
		int availNum = 0;
		boolean[] actAvail = SoccerGame.actionSet( agentIndex, gameState );
		    
		for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
			    
		    if(actAvail[act])
			availNum++;
		}
		    
		double pro = random.nextDouble();
		double proSum = 0.0;
		for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
		
		    if( !actAvail[act] )
			continue;
			   
		    proSum += 1.0 / availNum;
		    if( pro <= proSum ) {
				
			newActions[agentNoInTeam] = act;
			break;
		    }
		}
	    }

	    return newActions;
	}
	else
	    return teamActions;
    }
    
    //check again
    public GameAction epsilonGreedy( GameState gameState, GameAction gameAction )
    {
	double epsilon = 0.0;
	if( explorationTime < 0.01 )
	    epsilon = dynEpsilon;
	else {
	    
	    epsilon = Math.pow(1/explorationTime, 0.100001);
	    if( epsilon < dynEpsilon )
		epsilon = dynEpsilon;
	}
	
	if( random.nextDouble() < EPSILON ) {
	    
	    //System.out.println(dynEpsilon);
	    /**
	     * exploration one time
	     */
	    dynEpsilon *= 0.99999;//0.99999;
	    explorationTime += 1.0;
	    
	    /**
	     * for each agent in this team
	     */
	    for( int agentNoInTeam = 0; agentNoInTeam < SoccerGame.NUM_TEAM_AGENTS; agentNoInTeam++ ) {
		
		int agentIndex = teamIndex * SoccerGame.NUM_TEAM_AGENTS + agentNoInTeam;
		
		int availNum = 0;
		boolean[] actAvail = SoccerGame.actionSet( agentIndex, gameState );
		    
		for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
			    
		    if(actAvail[act])
			availNum++;
		}
		    
		double pro = random.nextDouble();
		double proSum = 0.0;
		for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
		
		    if( !actAvail[act] )
			continue;
			   
		    proSum += 1.0 / availNum;
		    if( pro <= proSum ) {
				
			gameAction.setAction( agentIndex, act);
			break;
		    }
		}
	    } 
	}
	
	return gameAction;
    }
    
    //right
    /**
     * Core method for MARL algorithms
     * update the Q-table and return the action for the next state
     * @param curState: the current state
     * @param jointAction: the joint action taken in the current state
     * @param rewards: the reward obtained from the current state to the next state
     * @param nextState: the next state
     * @return GameAction: the action expected to be chosen in the next state
     */
    public GameAction updateQ( GameState curState, GameAction jointAction, 
	    double[] rewards, GameState nextState )
    {
	
	/**
	 * for each agent in this team
	 */
	GameAction retAction = new GameAction();
	for( int agentNoInTeam = 0; agentNoInTeam < SoccerGame.NUM_TEAM_AGENTS; agentNoInTeam++ ) {
		
	    int agentIndex = teamIndex * SoccerGame.NUM_TEAM_AGENTS + agentNoInTeam;
		
	    int availNum = 0;
	    boolean[] actAvail = SoccerGame.actionSet( agentIndex, nextState );
		    
	    for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
			    
		if(actAvail[act])
		    availNum++;
	    }
		    
	    double pro = random.nextDouble();
	    double proSum = 0.0;
	    for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
			    
		if( !actAvail[act] )
		    continue;
			   
		proSum += 1.0 / availNum;
		if( pro < proSum ) {
				
		    retAction.setAction( agentIndex, act );
		    break;
		}
	    }
	}
	
	
	return retAction;
	
    }
    
    
    public int getTeamIndex() 
    {
	return teamIndex;
    }
    
    //check again
    protected void visit( GameState curState, GameAction curAction )
    {
	if( curState == null || curAction == null ) {
	    
	    System.out.println("@MARL->visit: Wrong Parameters!");
	    return;
	}
	
	
	StateActionPair saPair = new StateActionPair( curState, curAction );
	
	if( vstNum.containsKey( saPair ) ) {
	    
	    int count = vstNum.get( saPair );
	    count += 1;
	    vstNum.put( saPair, count );
	}
	
	saPair = null;
    }
    
  //check again
    protected double getVariableAlpha( GameState gameState, GameAction gameAction )
    {
	if( gameState == null || gameAction == null ) {
	    
	    System.out.println("@MARL->getVariableAlpha: Wrong Parameters!");
	    return 0.0;
	}
	
	StateActionPair saPair = new StateActionPair( gameState, gameAction );
	double vAlpha = 1.0;
	
	if( vstNum.containsKey( saPair ) ) {
	    
	    int count = vstNum.get( saPair );
	    
	    if( count <= 0 ) 
		vAlpha = 1.0;
	    else 
		vAlpha = 1.0 / ((double) count);
	}
	else
	    vAlpha = 1.0;
	
	saPair = null;
	return vAlpha;
    }
    
    //be careful with the parameter agentNoInTeam
    //it is not the agentIndex in the whole game
    protected double getQValue( GameState gameState, GameAction gameAction )
    {
	if( gameAction == null || 
		gameState == null ) {
	    
	    System.out.println("@MARL->getQValue: Wrong Parameters!");
	    return 0.0;
	}
	
	StateActionPair saPair = new StateActionPair( gameState, gameAction );
	double qValue = 0.0;
	
	if( Qs.containsKey( saPair ) ) {
	    
	    qValue = Qs.get( saPair );
	}
	
	//release the memory
	saPair = null;
	
	return qValue;
    }
    
    //be careful with the parameter agentNoInTeam
    //it is not the agentIndex in the whole game
    protected void setQValue( GameState gameState, 
	    GameAction gameAction, double value )
    {
	if( gameAction == null || 
		gameState == null ) {
	    
	    System.out.println("@CenCEQ->setQValue: Wrong Parameters!");
	    return;
	}
	
	StateActionPair saPair = new StateActionPair( gameState, gameAction );
	
	if( Qs.containsKey( saPair ) ) {
	    
	    Qs.put( saPair, value );
	}
	
	saPair = null;
    }
    
    
    //just for data
    public void gameFinished( int loop ) 
    {
	
    }
    
    //just for data
    public void gameStarted( int loop )
    {
	
    }
}
