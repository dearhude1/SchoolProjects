package algorithms;

import gameGridWorld.GameAction;
import gameGridWorld.GameState;
import gameGridWorld.SparseGridWorld;

public class QL extends MARL
{

    /**
     * Q-table of the corresponding agent
     * 1 dimensions for local state
     * 1 dimensions for local action
     */
    protected double[][] Qs;
    
    
    /**
     * visit number of each state-action pair
     */
    protected double[][] vstNum;

    public QL( int agent )
    {
	
	super( agent );
	
	/**
	 * init the Q-table
	 */
	int locNum = SparseGridWorld.NUM_CELLS;
	int actionNum = GameAction.NUM_ACTIONS;
	Qs = new double[locNum][actionNum];
	vstNum = new double[locNum][actionNum];
	
	for( int s = 0; s < locNum; s++ )
	    for( int a = 0; a < actionNum; a++ ) {
				    
		Qs[s][a] = random.nextDouble();//0.0;    
		vstNum[s][a] = 0.0;
	    }
    }
    
    public QL( int agent, double alpha, double gamma, double epsilon )
    {
	
	super( agent, alpha, gamma, epsilon);
	
	/**
	 * init the Q-table
	 */
	int locNum = SparseGridWorld.NUM_CELLS;
	int actionNum = GameAction.NUM_ACTIONS;
	Qs = new double[locNum][actionNum];
	vstNum = new double[locNum][actionNum];
	
	for( int s = 0; s < locNum; s++ )
	    for( int a = 0; a < actionNum; a++ ) {
				    
		Qs[s][a] = random.nextDouble();//0.0;    
		vstNum[s][a] = 0.0;
	    }
    }

    public GameAction updateQ( GameState curState, GameAction jointAction, 
	    double[] rewards, GameState nextState )
    {
	
	if( nextState == null ) {
	    
	    System.out.println("@QL->updateQ: NULL State!");
	    
	    return null;
	}	
	else {
	 
	    /**
	     * first get the next max action according to the Q-function
	     */
	    int nextLoc = nextState.getLocationID( agentIndex );
	    int nextAction = getMaxAction( nextLoc );
	    
	    /**
	     * if it is not the initial state, 
	     * then update the Q-function
	     */
	    if( curState != null && jointAction != null 
		    && rewards != null ) {
		
		/**
		 * make a visit?
		 */
		
		int curLoc = curState.getLocationID( agentIndex );
		int curAction = jointAction.getAction( agentIndex );
		double Qsa = getQValue( curLoc, curAction );
		double maxQp = getQValue( nextLoc, nextAction );
		Qsa = (1 - ALPHA) * Qsa + ALPHA * (rewards[agentIndex] + GAMMA * maxQp);
		
		setQValue( curLoc, curAction, Qsa );
		
		/**
		 * alpha decay??
		 */
		ALPHA *= 0.9991;//0.999998;//0.9958;//0.99958;
		
	    }
	    
	    GameAction retAction = new GameAction();
	    retAction.setAction( agentIndex, nextAction );
	    return retAction;
	}

    }
    
    private int getMaxAction( int locState ) {
	
	if( locState < 0 || 
		locState >= SparseGridWorld.NUM_CELLS ) {
	    
	    System.out.println("@QL->getMaxAction: Wrong Parameters!");
	    return -1;
	}
	
	double maxQ = Double.NEGATIVE_INFINITY;
	int maxAction = 0;
	
	for( int action = 0; action < GameAction.NUM_ACTIONS; action++ ) {
	    
	    if( Qs[locState][action] > maxQ ) {
		
		maxQ = Qs[locState][action];
		maxAction = action;
	    }
	}
	
	return maxAction;
    }
    
    protected double getQValue( int locState, int locAction )
    {
	if( locState < 0 || locState >= SparseGridWorld.NUM_CELLS ||
		locAction < 0 || locAction >= GameAction.NUM_ACTIONS ) {
	    
	    System.out.println("@QL->getQValue: Wrong Parameters!");
	    return 0.0;
	}
	
	return Qs[locState][locAction];
    }
    
    protected void setQValue( int locState, int locAction, double value )
    {
	if( locState < 0 || locState >= SparseGridWorld.NUM_CELLS ||
		locAction < 0 || locAction >= GameAction.NUM_ACTIONS ) {
	    
	    System.out.println("@QL->setQValue: Wrong Parameters!");
	    return;
	}
	
	Qs[locState][locAction] = value;
    }
    
}
