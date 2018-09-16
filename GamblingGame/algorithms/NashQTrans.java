package algorithms;

import gamGam.GameAction;
import gamGam.GameState;
import gamGam.GamblingGame;
import gamGam.StateActionPair;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * New algorithm:
 * NashQ with direct equilibrium transfer
 * @author dearhude1
 *
 */

public class NashQTrans extends NashQ
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
    private double tau = 0.1;//0.01;
    
    
    /**
     * for error bound analysis
     *
    private int timeStepLimit = 3000;
    private double[][][] errorBounds;
    private int[][] transTimes;
    */
    
    public NashQTrans()
    {
	super();
	
	int actionNum = GameAction.NUM_ACTIONS;
	
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
	    visited.put( gameState, new Boolean(false) );
	    
	    //init equilibrium
	    double[] equil = new double[GamblingGame.NUM_AGENTS * actionNum];    
	    for( int act = 0; act < GamblingGame.NUM_AGENTS * actionNum; act++ ) {
		
		equil[act] = 0.0; 
	    }
	    lastEquilibrium.put( gameState, equil );
	    
		
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
	
	//for error bound analysis
	/**
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
    
    public NashQTrans( double alpha, double gamma, double epsilon )
    {
	super(alpha, gamma, epsilon);
	
	int actionNum = GameAction.NUM_ACTIONS;
	
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
	    visited.put( gameState, new Boolean(false) );
	    
	    //init equilibrium
	    double[] equil = new double[GamblingGame.NUM_AGENTS * actionNum];    
	    for( int act = 0; act < GamblingGame.NUM_AGENTS * actionNum; act++ ) {
		
		equil[act] = 0.0; 
	    }
	    lastEquilibrium.put( gameState, equil );
	    
		
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
	
	if( nextState == null ) {
	    
	    System.out.println("@NashQTrans->updateQ: NULL nextState!");
	    
	    return null;
	}
	else {
	    
	    /**
	     * first we should determine whether to compute an NE
	     */
	    double[] boundValue = null;
	    boundValue = shouldComputeNE( nextState );
	    boolean bTrans = true;
	    if( boundValue != null ) {
		
		for( int agent = 0; agent < GamblingGame.NUM_AGENTS; agent++ ) {
			
		    if( boundValue[agent] > tau ) {
			    
			bTrans = false;
			break;
		    }
		}
	    }
	    else
		bTrans = false;

	    /**
	     * compute the Nash equilibrium in the next state
	     */
	    double[] nashEquil = null;
	    if( bTrans ) {
		
		nashEquil = lastEquilibrium.get( nextState );
		
		//record the error bound///////////////////////////
		/**
		if( transTimes[loc0][loc1] < timeStepLimit ) {
		    
		    int time = transTimes[loc0][loc1];
		    if( boundValue[0] > boundValue[1] ) 
			errorBounds[loc0][loc1][time] += boundValue[0];
		    else
			errorBounds[loc0][loc1][time] += boundValue[1];
		    
		    //System.out.println(errorBounds[loc0][loc1][time]);
		    
		    transTimes[loc0][loc1] += 1;
		}
		*/
		//for error bound analysis//////////////////////////
	    }
	    else {
		
		//if( boundValue != null )
		   // System.out.println(boundValue[0]);
		
		nashEquil = computeNE( agentIndex, nextState );
		
		/**
		 * store the current game
		 */
		storeGame( nextState, nashEquil );
	    }
	    
	    /**
	     * get a joint action according to the correlated equilibrium
	     */
	    GameAction nextAction = getJointAction_NE( nashEquil );
	    
	    
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
		double[] nashQValues = getNashQValues( nextState, nashEquil );
		    
		for( int agent = 0; agent < GamblingGame.NUM_AGENTS; agent++ ) {
		    
		    /**
		     * get the Q-value
		     */
		    double Qsa = getQValue( agent, curState, jointAction );
		    
		    /**
		     * updating rule for NashQTrans
		     */
		    if( !bTrans )
			Qsa = (1 - ALPHA) * Qsa + ALPHA * (rewards[agent] + GAMMA * nashQValues[agent]);
		    else 
			Qsa = (1 - ALPHA) * Qsa + ALPHA * (rewards[agent] + GAMMA * (nashQValues[agent]));//boundValue[agent]));
		   
		    
		    /**
		     * variable learning rate
		     *
		    double alpha = getVariableAlpha( curState, jointAction );
		    if( !bTrans )
			Qsa = (1 - alpha) * Qsa + alpha * (rewards[agent] + GAMMA * nashQValues[agent]);
		    else 
			Qsa = (1 - alpha) * Qsa + alpha * (rewards[agent] + GAMMA * (nashQValues[agent]+boundValue[agent]));
		    */
		    
		    /**
		     * write back to the tables
		     */
		    setQValue( agent, curState, jointAction, Qsa );
		}
		
		/**
		 * maybe we can release some memories
		 */
		nashQValues = null;
	    }
	    
	    /**
	     * maybe we can release some memories
	     */
	    nashEquil = null;
	    
	    return nextAction;
	}
    }
    
    //think about it
    /**
     * determine whether we should compute an NE in a state
     */
    private double[] shouldComputeNE( GameState gameState )
    {
	if( gameState == null ) {
	    
	    System.out.println("NashQTrans->shouldComputeNE: Parameter error");
	    return null;
	}
	
	Boolean b = visited.get( gameState );
	if( b == null ) {
	    
	    System.out.println("I am NULLLLLL");
	}
	
	if( !visited.get( gameState ) ) {
	    
	    visited.put( gameState, new Boolean(true) );
	    return null;
	}
	else {
	    
	    
	    double[] retValues = new double[GamblingGame.NUM_AGENTS]; 
	    int actionNum = GameAction.NUM_ACTIONS;
	    
	    ArrayList<GameAction> allActions = GamblingGame.allJointActions;
	    
	    double[] deltaWeightedSums = new double[GamblingGame.NUM_AGENTS];
	    for( int agent = 0; agent < GamblingGame.NUM_AGENTS; agent++ ) 
		deltaWeightedSums[agent] = 0;
	    
	    for( int actionIndex = 0; actionIndex < allActions.size(); actionIndex++ ) {
		
		GameAction jntAction = allActions.get( actionIndex );
		StateActionPair saPair = new StateActionPair( gameState, jntAction );
		
		for( int agent = 0; agent < GamblingGame.NUM_AGENTS; agent++ ) {
		    
		    double deltaValue = getQValue( agent, gameState, jntAction) - 
			    lastGame.get( saPair )[agent];
		    
		    double proInEquilibrium = 1.0;
		    double[] equil = lastEquilibrium.get( gameState );
		    for( int agent_p = 0; agent_p < GamblingGame.NUM_AGENTS; agent_p++ ) {
			
			int compIndex = agent_p * GameAction.NUM_ACTIONS + jntAction.getAction(agent_p);
			proInEquilibrium *= equil[compIndex];
		    }
		    
		    deltaWeightedSums[agent] += deltaValue * proInEquilibrium;// += not =
		}
	    }
	    
	    
	    /**
	     * compute the error bound for each agent
	     */
	    
	    for( int agent = 0; agent < GamblingGame.NUM_AGENTS; agent++ ) {
		
		double deltaPlusSum_Max = Double.NEGATIVE_INFINITY;
		for( int act = 0; act < actionNum; act++ ) {
		    		
		    double deltaPlusSum = 0.0;
		    		
		    //all other joint actions
		    ArrayList<GameAction> jntOtherActions = generateOtherJntActions( agent );
		    for( int listIndex = 0; listIndex < jntOtherActions.size(); listIndex++ ) {
			
			GameAction jntAction = jntOtherActions.get( listIndex );
			jntAction.setAction( agent, act );
			StateActionPair saPair = new StateActionPair( gameState, jntAction );
			
			double deltaValue = getQValue( agent, gameState, jntAction ) - 
				    lastGame.get( saPair )[agent];
			
			
			if( deltaValue > 0 )
			    deltaPlusSum += deltaValue;
			
			
			//release memory?
			saPair = null;
			
		    }
		    		
		    if( deltaPlusSum > deltaPlusSum_Max )
			deltaPlusSum_Max = deltaPlusSum;
		}
		    	
		retValues[agent] = deltaPlusSum_Max - deltaWeightedSums[agent];
	    }

	    
	    
	    return retValues;
	    
	    /**
	    if( bound0 < tau && bound1 < tau ) {
		
		//if( bound0 > 0.0 )
		   // System.out.println("bound 0: "+bound0);
		//if( bound1 > 0.0 )
		    //System.out.println("bound 1: "+bound1);
		
		return false;
	    }
	    else {
		
		//System.out.println("bound 0: "+bound0);
		//System.out.println("bound 1: "+bound1);
		return true;
	    }
	    */
	}
    }
    
    //right
    /**
     * store the last game where an NE is computed
     * store the computed NE at the same time
     */
    private void storeGame( GameState gameState, double[] nashEquil )
    {
	if( gameState == null || nashEquil == null ) {
	    
	    System.out.println("NashQTrans->storeGame: Parameter error");
	    return;
	}
	
	int actionNum = GameAction.NUM_ACTIONS;
	
	ArrayList<GameAction> allActions = GamblingGame.allJointActions;
	for( int actionIndex = 0; actionIndex < allActions.size(); actionIndex++ ) {
	    
	    GameAction jntAction = allActions.get( actionIndex );
	    
	    StateActionPair saPair = new StateActionPair( gameState, jntAction);
	    
	    double[] payoffs = lastGame.get( saPair );
	    for( int agent = 0; agent < GamblingGame.NUM_AGENTS; agent++ ) {
		
		payoffs[agent] = getQValue( agent, gameState, jntAction );
	    }
	    lastGame.put( saPair, payoffs );
	}
	
	    
	double[] equilibrium = lastEquilibrium.get( gameState );
	for( int act = 0; act < GamblingGame.NUM_AGENTS * actionNum; act++ ) {
	    
	    equilibrium[act] = nashEquil[act];
	}
	lastEquilibrium.put( gameState, equilibrium );
    }
    
}
