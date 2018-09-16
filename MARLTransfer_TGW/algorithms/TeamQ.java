package algorithms;


import gameGridWorld.GameAction;
import gameGridWorld.GameState;
import gameGridWorld.GridWorld;


/**
 * TeamQ is a centralized controller
 * only one Q-function for all state-joint-action pairs
 * @author dearhude1
 *
 */
public class TeamQ extends MARL
{

    
    /**
     * Q-table of the corresponding agent
     * 2 dimensions for state
     * 2 dimensions for joint action
     */
    protected double[][][][] Qfunc;
    
    public TeamQ( )
    {
	
	super( 0 );
	
	/**
	 * init the Q-table
	 */
	int locNum = GridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	Qfunc = new double[locNum][locNum][actionNum][actionNum];

	for( int s1 = 0; s1 < locNum; s1++ )
	    for( int s2 = 0; s2 < locNum; s2++ )
		for( int a1 = 0; a1 < actionNum; a1++ )
		    for( int a2 = 0; a2 < actionNum; a2++ ) {
				    
			Qfunc[s1][s2][a1][a2] = 0.0;
				    
		    }
    }
    
    public TeamQ( double alpha, double gamma, double epsilon )
    {
	
	super( 0, alpha, gamma, epsilon);
	
	
	/**
	 * init the Q-table
	 */
	int locNum = GridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	Qfunc = new double[locNum][locNum][actionNum][actionNum];

	for( int s1 = 0; s1 < locNum; s1++ )
	    for( int s2 = 0; s2 < locNum; s2++ )
		for( int a1 = 0; a1 < actionNum; a1++ )
		    for( int a2 = 0; a2 < actionNum; a2++ ) {
				    
			Qfunc[s1][s2][a1][a2] = 0.0;
				    
		    }
    }
    
    
    protected GameAction getMaxAction( GameState gameState )
    {
	/**
	 * return the action with the maximal value
	 */
	GameAction maxAction = null;
	double maxValue = Double.NEGATIVE_INFINITY;
	for( int a1 = 0; a1 < GameAction.NUM_ACTIONS; a1++ )
	    for( int a2 = 0; a2 < GameAction.NUM_ACTIONS; a2++ ) {
		
		GameAction jntAction = new GameAction(new int[]{a1,a2});
		//agentIndex is not useful here, only for the need of parameter
		double value = getQValue(agentIndex, gameState, jntAction);
		
		if( value > maxValue ) {
		    
		    maxValue = value;
		    maxAction = jntAction;
		}
		else
		    jntAction = null;
	    }
	
	return maxAction;
    }
    
    public GameAction updateQ( GameState curState, GameAction jointAction, 
	    double[] rewards, GameState nextState )
    {
	if( nextState == null ) {
	    
	    System.out.println("@TeamQ->updateQ: NULL nextState!");
	    
	    return null;
	}
	
	else {
	    
	    /**
	     * get the max action in the next state
	     */
	    GameAction nextAction = getMaxAction( nextState );
	    
	    /**
	     * update the Q-function
	     * but if this is the initial state of the game
	     * just return the action
	     */
	    if( curState != null && jointAction != null 
		&& rewards != null ) {
		
		/**
		 * mark a visit
		 */
		//visit( curState, jointAction );
		
		/**
		 * learning rule:
		 * Q(s,a) <- (1-alpha)Q(s,a) + alpha * (reward + gamma * V(s'))
		 */
		double Qsa = getQValue( agentIndex, curState, jointAction );
		
		//Val = max Q(s,a1,a2)
		double Val = getQValue( agentIndex, nextState, nextAction );
		
		Qsa = (1 - ALPHA) * Qsa + ALPHA * ( rewards[agentIndex] + GAMMA * Val );
		setQValue( agentIndex, curState, jointAction, Qsa );
	    }
	    
	    return nextAction;
	}
    }
    
    
    /**
     * agentIndex is not used
     */
    protected double getQValue( int agent, GameState gameState, 
	    GameAction gameAction )
    {
	if( gameAction == null || 
		gameState == null ) {
	    
	    System.out.println("@TeamQ->getQValue: Wrong Parameters!");
	    return 0.0;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int a0 = gameAction.getAction(0);
	int a1 = gameAction.getAction(1);
	
	return Qfunc[loc0][loc1][a0][a1];
    }
    
    /**
     * agentIndex is not used
     */
    protected void setQValue( int agent, GameState gameState, 
	    GameAction gameAction, double value )
    {
	if( gameAction == null || 
		gameState == null ) {
	    
	    System.out.println("@TeamQ->setQValue: Wrong Parameters!");
	    return;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int a0 = gameAction.getAction(0);
	int a1 = gameAction.getAction(1);
	
	Qfunc[loc0][loc1][a0][a1] = value;
    }
    
    
}
