package algorithms;


import gamePursuitGame.GameAction;
import gamePursuitGame.GameState;
import gamePursuitGame.PursuitGame;

public class MiniMaxQTrans extends MiniMaxQ
{
    
    /**
     * we use tau to denote the threshold of the error bound
     */
    private double tau = 0.5;
    
    public MiniMaxQTrans( int index )
    {
	super(index);
	
    }
    
    public MiniMaxQTrans( int index, double alpha, double gamma, double epsilon )
    {
	
	super(index, alpha, gamma, epsilon);
	
    }
    
    
    public GameAction updateQ( GameState curState, GameAction jointAction, 
	    double[] rewards, GameState nextState )
    {
	if( nextState == null ) {
	    
	    System.out.println("@MiniMaxQ->updateQ: NULL nextState!");
	    
	    return null;
	}
	else {
	    
	    
	    /**
	     * select action in the next state according to pi(s)
	     */
	    GameAction nextAction = getAction( nextState );
	    
	    /**
	     * update the Q-tables
	     * but if this is the initial state of the game
	     * just return the action
	     */
	    if( curState != null && jointAction != null 
		&& rewards != null )  {
		
		
		/**
		 * mark a visit
		 */
		visit( curState, jointAction );
		
		
		/**
		 * learning rule:
		 * Q(s,a) <- (1-alpha)Q(s,a) + alpha * (reward + gamma * V(s'))
		 */
		double Qsa = getQValue( agentIndex, curState, jointAction );
		double Vsp = getV( nextState );
		Qsa = (1 - ALPHA) * Qsa + ALPHA * ( rewards[agentIndex] + GAMMA * Vsp );
		setQValue( agentIndex, curState, jointAction, Qsa );
		
		
		/**
		 * whether to update policy
		 */
		boolean bCompute = shouldCompute( curState );
		if( bCompute ) {
		    
		    /**
		     * linear programming to update the policy in curState
		     */
		    double minimaxV = updatePolicy( curState );
		    setV( curState,  minimaxV );
		}
		else {
		    
		    int loc0 = curState.getLocationID(0);
		    int loc1 = curState.getLocationID(1);
		    double minimaxV = getPolicyMinValue( curState, pi[loc0][loc1]);
		    setV( curState,  minimaxV );
		}
		

		
		ALPHA *= alphaDecay;
	    }
	    
	    return nextAction;
	}
    }
    
    private boolean shouldCompute( GameState gameState )
    {
	if( gameState == null ) {
	    
	    System.out.println("MiniMaxQTrans->shouldComputeNE: Parameter error");
	    return true;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	
	/**
	 * the last policy
	 */
	double[] lastPolicy = pi[loc0][loc1]; 
	double lastPolicyValue = getPolicyMinValue(gameState, lastPolicy);
	
	
	/**
	 * check the last policy
	 */
	double max_error = Double.NEGATIVE_INFINITY;
	for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
	    
	    /**
	     * a new pure policy
	     */
	    double[] purePolicy = new double[GameAction.NUM_ACTIONS];
	    for( int act_p = 0; act_p < GameAction.NUM_ACTIONS; act_p++ ) {
		
		if( act_p == act )
		    purePolicy[act_p] = 1.0;
		else
		    purePolicy[act_p] = 0.0; 
	    }
	    double minValue = getPolicyMinValue(gameState, purePolicy);
	    
	    /**
	     * error(a,pi) = min_o Q(s,a,o) - min_o \Sum_{a'} pi(a')Q(s,a',o)
	     */
	    double error = minValue - lastPolicyValue;
	    if( error > max_error ) 
		max_error = error;
	}
	
	if( max_error <= tau )
	    return false;
	else {
	    return true;
	}
	
    }

    private double getPolicyMinValue( GameState gameState, double[] policy )
    {
	if( gameState == null || policy == null || 
		policy.length != GameAction.NUM_ACTIONS ) {
	    
	    System.out.println("MiniMaxQTrans->getPolicyValue: Parameter error");
	    return 0;
	}
	
	double minValue = Double.POSITIVE_INFINITY;
	for( int act_o = 0; act_o < GameAction.NUM_ACTIONS; act_o++ ) {
	    
	    double value = 0.0;
	    for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
		
		GameAction jntAction = new GameAction();
		jntAction.setAction( agentIndex, act );
		jntAction.setAction( (agentIndex+1)%PursuitGame.NUM_AGENTS, act_o);
		value += getQValue(agentIndex, gameState, jntAction) * policy[act];
	    }
	    if( value < minValue ) {
		
		minValue = value;
	    }   
	}
	
	return minValue;
    }
    
}
