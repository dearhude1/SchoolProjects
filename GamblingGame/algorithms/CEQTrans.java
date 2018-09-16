package algorithms;

import gamGam.GameAction;
import gamGam.GameState;
import gamGam.GamblingGame;
import gamGam.StateActionPair;

import java.util.ArrayList;
import java.util.HashMap;



public class CEQTrans extends CenCEQ
{

    /**
     * the last game 
     */
    private HashMap<StateActionPair, double[]> lastGame;
    
    /**
     * the last equilibrium in each state
     */
    private HashMap<GameState, double[]> lastEquilibrium;
    
    private HashMap<GameState, Boolean> visited;
    
    /**
     * we use tau to denote the threshold of the error bound
     */
    private double tau = 0.01;
    //0.01 for transfer
    //0.1 for error analysis
    
    /**
     * for error bound analysis
     *
    protected int timeStepLimit = 3000;
    protected double[][][] errorBounds;
    protected int[][] transTimes;
    */
    
    public CEQTrans()
    {
	super();

	
	//init new variables
	lastGame = new HashMap<StateActionPair, double[]>(); 
	lastEquilibrium = new HashMap<GameState, double[]>();
	visited = new HashMap<GameState, Boolean>();
	
	/**
	 * get all states and all joint actions from LinearGridWorld class
	 */
	ArrayList<GameState> allStates = GamblingGame.allStates;
	ArrayList<GameAction> allActions = GamblingGame.allJointActions;
	
	for( int stateIndex = 0; stateIndex < allStates.size(); stateIndex++ ) {
	    
	    GameState gameState = allStates.get( stateIndex );
	    
	    //init visited
	    visited.put( gameState, false );
	    
	    //init equilibrium
	    double[] corrEquil = new double[allActions.size()];    
	    for( int jntAct = 0; jntAct < allActions.size(); jntAct++ ) {
		
		corrEquil[jntAct] = 0.0; 
	    }
	    if( !lastEquilibrium.containsKey( gameState ) ) {
	    
		lastEquilibrium.put( gameState, corrEquil );
	    }
	    else {
		
		 corrEquil = null;
	    }
	    
	    //init last game
	    for( int actionIndex = 0; actionIndex < allActions.size(); actionIndex++ ) {
		
		GameAction jntAction = allActions.get( actionIndex );
		StateActionPair saPair = new StateActionPair( gameState, jntAction );
		double[] payoffs = new double[GamblingGame.NUM_AGENTS];
		
		for( int agent = 0; agent < GamblingGame.NUM_AGENTS; agent++ ) {
		    
		    payoffs[agent] = 0.0;
		}
		lastGame.put( saPair, payoffs );
	    }
	}
	
	/**
	//for error bound analysis
	errorBounds = new double[locNum][locNum][timeStepLimit];
	transTimes = new int[locNum][locNum];
	 
	for( int s1 = 0; s1 < locNum; s1++ )
	    for( int s2 = 0; s2 < locNum; s2++ ) {
		
		transTimes[s1][s2] = 0;
		
		for( int t = 0; t < timeStepLimit; t++ ) 
		    errorBounds[s1][s2][t] = 0.0;
	    }
	*/
    }
    
    public CEQTrans( double alpha, double gamma, double epsilon )
    {
	super(alpha, gamma, epsilon);
	
	//init new variables
	lastGame = new HashMap<StateActionPair, double[]>(); 
	lastEquilibrium = new HashMap<GameState, double[]>();
	visited = new HashMap<GameState, Boolean>();
	
	/**
	 * get all states and all joint actions from LinearGridWorld class
	 */
	ArrayList<GameState> allStates = GamblingGame.allStates;
	ArrayList<GameAction> allActions = GamblingGame.allJointActions;
	
	for( int stateIndex = 0; stateIndex < allStates.size(); stateIndex++ ) {
	    
	    GameState gameState = allStates.get( stateIndex );
	    
	    //init visited
	    visited.put( gameState, false );
	    
	    //init equilibrium
	    double[] corrEquil = new double[allActions.size()];    
	    for( int jntAct = 0; jntAct < allActions.size(); jntAct++ ) {
		
		corrEquil[jntAct] = 0.0; 
	    }
	    lastEquilibrium.put( gameState, corrEquil );
	    
	    //init last game
	    for( int actionIndex = 0; actionIndex < allActions.size(); actionIndex++ ) {
		
		GameAction jntAction = allActions.get( actionIndex );
		StateActionPair saPair = new StateActionPair( gameState, jntAction );
		double[] payoffs = new double[GamblingGame.NUM_AGENTS];
		
		for( int agent = 0; agent < GamblingGame.NUM_AGENTS; agent++ ) {
		    
		    payoffs[agent] = 0.0;
		}
		lastGame.put( saPair, payoffs );
	    }
	}
	
    }
    
    public GameAction updateQ( GameState curState, GameAction jointAction, 
	    double[] rewards, GameState nextState )
    {
	
	if( nextState == null ) {
	    
	    System.out.println("@CenCEQ->updateQ: NULL nextState!");
	    
	    return null;
	}
	else {
	    
	    /**
	     * first we should determine whether to compute a CE
	     */
	    double[] lossValues = null;
	    lossValues = shouldComputeCE( nextState );
	    
	    //here
	    boolean bTrans = true;
	    if( lossValues != null ) {
		
		for( int agent = 0; agent < GamblingGame.NUM_AGENTS; agent++ ) {
		    
		    if( lossValues[agent] >= tau ) {
			
			bTrans = false;
			break;
		    }
		}
	    }
	    else
		bTrans = false;
	    
	    /**
	     * compute the correlated equilibrium in the next state
	     */
	    double[] correlEquil = null;
	    if( bTrans ) {
		
		correlEquil = lastEquilibrium.get( nextState );
	    }
	    else {
		
		correlEquil = computeCE( agentIndex, nextState );
		
		/**
		 * store the current game
		 */
		storeGame( nextState, correlEquil );
	    }
	    
	    
	    /**
	     * get a joint action according to the correlated equilibrium
	     */
	    GameAction nextAction = getJointAction_CE( correlEquil );
	    
	    
	    /**
	     * update the Q-tables
	     * but if this is the initial state of the game
	     * just return the action
	     */
	    if( curState != null && jointAction != null 
		&& rewards != null ) {
		
		/**
		 * mark a visit
		 */
		visit( curState, jointAction );
		
		/**
		 * compute the correspoding Q-values
		 */
		double[] correlValues = getCEQValues( nextState, correlEquil );
		    
		for( int agent = 0; agent < GamblingGame.NUM_AGENTS; agent++ ) {
		    
		    /**
		     * get the Q-value
		     */
		    double Qsa = getQValue( agent, curState, jointAction );
		    
		    /**
		     * updating rule
		     */
		    //Qsa = (1 - ALPHA) * Qsa + ALPHA * (rewards[agent] + GAMMA * correlValues[agent]);
		    
		    
		    /**
		     * variable learning rate
		     */
		    double alpha = getVariableAlpha( curState, jointAction );
		    Qsa = (1 - alpha) * Qsa + alpha * (rewards[agent] + GAMMA * correlValues[agent]);
		    
		    /**
		     * write back to the tables
		     */
		    setQValue( agent, curState, jointAction, Qsa );
		}
		
		/**
		 * maybe we can release some memories
		 */
		correlValues = null;
	    }
	    
	    /**
	     * maybe we can release some memories
	     */
	    correlEquil = null;
	    
	    return nextAction;
	}
    }
    
    
    /**
     * determine whether we should compute an NE in a state
     */
    private double[] shouldComputeCE( GameState gameState )
    {
	if( gameState == null ) {
	    
	    System.out.println("CEQTrans->shouldComputeCE: Parameter error");
	    return null;
	}
	
	
	ArrayList<GameAction> allActions = GamblingGame.allJointActions;
	
	/**
	 * first check whether the last equilibrium is available
	 */
	double proSum = 0.0;
	for( int jntAct = 0; jntAct < allActions.size(); jntAct++ ) {
	    
	    proSum += lastEquilibrium.get( gameState )[jntAct];
	}
	if( !(Math.abs(proSum-1.0) < 0.1) ) {
	   
	    return null;
	}
	
	
	if( !visited.get( gameState ) ) {
	    
	    visited.put( gameState, true );
	    return null;
	}
	else {
	    
	    double[] retValues = new double[GamblingGame.NUM_AGENTS];
	    
	    /**
	     * compute the max loss for each agent
	     */
	    for( int agent = 0; agent < GamblingGame.NUM_AGENTS; agent++ ) {
		
		retValues[agent] = computeCELoss( agent, gameState );
	    }
	    
	    return retValues;
	}
    }
    
    //check again
    protected double computeCELoss( int agent, GameState gameState )
    {
	if( gameState == null || agent < 0 || 
		agent >= GamblingGame.NUM_AGENTS ) {
	    
	    System.out.println("CEQTrans->computeCELoss: Parameter error");
	    return Double.MAX_VALUE;
	}
	
	/**
	 * compute loss for each action
	 */
	double maxLoss = Double.NEGATIVE_INFINITY;
	for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
	    
	    
	    /**
	     * for all the other actions of the agent
	     */
	    double maxLoss_p = Double.NEGATIVE_INFINITY;
	    for( int act_p = 0; act_p < GameAction.NUM_ACTIONS; act_p++ ) {
		
		if( act_p == act )
		    continue;
		
		/**
		 * for the other agents' actions
		 */
		double loss = 0.0;
		ArrayList<GameAction> othJntActions_a = generateOtherJntActions( agent );
		ArrayList<GameAction> othJntActions_ap = generateOtherJntActions( agent );
		for( int listIndex = 0; listIndex < othJntActions_a.size(); listIndex++ ) {
		    
		    GameAction jntAction_a = othJntActions_a.get( listIndex );
		    GameAction jntAction_ap = othJntActions_ap.get( listIndex );
		    
		    jntAction_a.setAction( agent, act );
		    jntAction_ap.setAction( agent, act_p );
		    
		    double deltaUti = getQValue(agent, gameState, jntAction_ap)-
			    getQValue(agent, gameState, jntAction_a);
		    
		    int variableIndex = GamblingGame.queryJointActionIndex( jntAction_a );
		    loss += lastEquilibrium.get( gameState )[variableIndex] * deltaUti;
		    
		}
		
		/**
		 * compare loss to the current maxLoss
		 */
		if( loss > maxLoss_p )
		    maxLoss_p = loss;
	    }
	    
	    if( maxLoss_p > maxLoss )
		maxLoss = maxLoss_p;
	}
	
	//if( maxLoss < 0)
	    //System.out.println("Oh My God!!!");
	
	return maxLoss;
    }
    
    /**
     * store the last game where an NE is computed
     * store the computed NE at the same time
     */
    private void storeGame( GameState gameState, double[] correlEquil )
    {
	if( gameState == null ) {
	    
	    System.out.println("CEQTrans->storeGame: Parameter error");
	    return;
	}
	
	
	ArrayList<GameAction> allActions = GamblingGame.allJointActions;
	double[] lastEquil = lastEquilibrium.get( gameState ); 
	
	for( int actionIndex = 0; actionIndex < allActions.size(); actionIndex++ ) {
	    
	    GameAction jntAction = allActions.get( actionIndex );
	    StateActionPair saPair = new StateActionPair( gameState, jntAction );
	    
	    double[] payoffs = lastGame.get( saPair );
	    for( int agent = 0; agent < GamblingGame.NUM_AGENTS; agent++ )
		payoffs[agent] = getQValue( agent, gameState, jntAction);
	    
	    lastGame.put( saPair, payoffs );
	    
	    //release memory
	    saPair = null;
	    
	    if( correlEquil == null )
		lastEquil[actionIndex] = 0.0;
	    else
		lastEquil[actionIndex] = correlEquil[actionIndex];   
	}
	
	lastEquilibrium.put( gameState, lastEquil );
	
    }
    

    
}
