package algorithms;

import drasys.or.mp.Constraint;
import drasys.or.mp.SizableProblemI;
import gameGridWorld.GameAction;
import gameGridWorld.GameState;
import gameGridWorld.GridWorld;

/**
 * centralized CE-Q algorithm
 * @author dearhude1
 *
 */
public class CenCEQ extends MARL
{

    
    public CenCEQ()
    {
	/**
	 * index is no use for centralized CE-Q
	 */
	super( 0 );
	
	/**
	 * init the Q-table
	 * for centralized algorithms 
	 * the Q-tables can be initialized randomly
	 */
	int locNum = GridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;

	/**
	 * init Q-tables again
	 */
	for( int agentIndex = 0; agentIndex < GridWorld.NUM_AGENTS; agentIndex++ )
	    for( int s1 = 0; s1 < locNum; s1++ )
		for( int s2 = 0; s2 < locNum; s2++ )
		    for(int s3 = 0; s3 < locNum; s3++ )
			for( int a1 = 0; a1 < actionNum; a1++ )
			    for( int a2 = 0; a2 < actionNum; a2++ )
				for(int a3 = 0; a3 < actionNum; a3++ ) {
				    
				    Qs[agentIndex][s1][s2][s3][a1][a2][a3] = (Math.random() - 0.5) / 10.0;
				}
    }
    
    
    public CenCEQ( double alpha, double gamma, double epsilon )
    {
	super( 0, alpha, gamma, epsilon);
	
	/**
	 * init the Q-table
	 * for centralized algorithms 
	 * the Q-tables can be initialized randomly
	 */
	int locNum = GridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	
	/**
	 * init Q-tables again
	 */
	for( int agentIndex = 0; agentIndex < GridWorld.NUM_AGENTS; agentIndex++ )
	    for( int s1 = 0; s1 < locNum; s1++ )
		for( int s2 = 0; s2 < locNum; s2++ )
		    for(int s3 = 0; s3 < locNum; s3++ )
			for( int a1 = 0; a1 < actionNum; a1++ )
			    for( int a2 = 0; a2 < actionNum; a2++ )
				for(int a3 = 0; a3 < actionNum; a3++ ) {
				    
				    Qs[agentIndex][s1][s2][s3][a1][a2][a3] = (Math.random() - 0.5) / 10.0;
				}
    }
    
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
	    
	    System.out.println("@CenCEQ->updateQ: NULL nextState!");
	    
	    return null;
	}
	else {
	    
	    /**
	     * compute the correlated equilibrium in the next state
	     */
	    double[] correlEquil = computeCE( agentIndex, nextState );
	    
	    
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
		    
		for( int agent = 0; agent < GridWorld.NUM_AGENTS; agent++ ) {
		    
		    /**
		     * get the Q-value
		     */
		    double Qsa = getQValue( agent, curState, jointAction );
		    
		    /**
		     * updating rule
		     */
		    Qsa = (1 - ALPHA) * Qsa + ALPHA * (rewards[agent] + GAMMA * correlValues[agent]);
		    
		    
		    /**
		     * variable learning rate
		     */
		    //alpha = getVariableAlpha( curState, jointAction );
		    //Qsa = (1 - alpha) * Qsa + alpha * (rewards[agent] + GAMMA * correlValues[agent]);
		    
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
     * compute a correlated equilibrium
     * can be overrided by sub classes
     * @param agent
     * @param gameState
     * @return
     */
    protected double[] computeCE( int agent, GameState gameState )
    {
	
	return null;
    }
    
    
    
    /**
     * set the constraints according to the conditions of correlated equilibrium
     */
    protected void setConstraints_CE( SizableProblemI problem, double[][][] q1,
	    double[][][] q2, double[][][] q3 ) throws Exception
    {
	/**
	 * 36 constraints for the inequalities of correlated equilibrium
	 */
	for( int agent = 0; agent < GridWorld.NUM_AGENTS; agent++ )
	{
	    double[][][] q = null;
	    if( agent == 0 )
		q = q1;
	    else if( agent == 1 )
		q = q2;
	    else
		q = q3;
	    
	    /**
	     * loop for action ai
	     */
	    for( int ai = 0; ai < GameAction.NUM_ACTIONS; ai++ )
	    {
		/**
		 * loop for action ai'
		 */
		for( int aip = 0; aip < GameAction.NUM_ACTIONS; aip++ )
		{
		    if( aip == ai )
			continue;
		    
		    /**
		     * the name of the constraint:
		     * agentID(ai-ai')
		     */
		    String aiString = GameAction.getActionString(ai);
		    String aipString = GameAction.getActionString(aip);
		    String conString = agent+"("+aiString+"-"+aipString+")";
		    problem.newConstraint(conString).setType(Constraint.GREATER).setRightHandSide(0.0);
		    
		    /**
		     * set the coefficient
		     */
		    for( int j = 0; j < GameAction.NUM_ACTIONS; j++ )
		    {
			for( int k = 0; k < GameAction.NUM_ACTIONS; k++ )
			{
			    String bString = GameAction.getActionString(j);
			    String cString = GameAction.getActionString(k);
			    
			    if( agent == 0 )
				problem.setCoefficientAt(conString, aiString+":"+bString+":"+cString, 
					q[ai][j][k]-q[aip][j][k] );
			    else if( agent == 1 )
				problem.setCoefficientAt(conString, bString+":"+aiString+":"+cString, 
					q[j][ai][k]-q[j][aip][k] );
			    else
				problem.setCoefficientAt(conString, bString+":"+cString+":"+aiString, 
					q[j][k][ai]-q[j][k][aip] );
				
			}
		    }
		}
	    }
	}
	
	/**
	 * the constraint that the sum of all 
	 * joint actions' probabilities is 1
	 */
	String eqCon = "equalConstraint";
	int index = problem.newConstraint(eqCon).
		setType(Constraint.EQUAL).setRightHandSide(1.0).getRowIndex();
	for( int i = 0; i < 64; i++ )
	{
	    problem.setCoefficientAt(index, i, 1.0);
	}
	
	/**
	 * the constraint of each joint action 
	 * that its probability is larger than 0
	 */
	for( int i = 0; i < 64; i++ )
	{
	    String zeroCon = "aboveZero" + (i+1);
	    index = problem.newConstraint(zeroCon).
	    	setType(Constraint.GREATER).setRightHandSide(0.0).getRowIndex();
	    problem.setCoefficientAt(index, i, 1.0);
	}
	
	
	/**
	 * maybe we lose one important condition
	 */
    }
    
    
    /**
     * get the joint action according to a correlated equilibrium
     */
    private GameAction getJointAction_CE( double[] correlatedE )
    {	
	GameAction retAction = new GameAction();
	
	if( correlatedE == null )
	{
	    retAction.setAction(0, random.nextInt(GameAction.NUM_ACTIONS));
	    retAction.setAction(1, random.nextInt(GameAction.NUM_ACTIONS));
	    retAction.setAction(2, random.nextInt(GameAction.NUM_ACTIONS));
	}
	
	else {
	    
	    double[] probabilities = new double[64];
		
	    probabilities[0] = correlatedE[0];
	    for( int i = 1; i < 64; i++ )
	    {
		probabilities[i] =  probabilities[i-1] + correlatedE[i];
	    }
		
	    double d = random.nextDouble();
	    int actionIndex = 0;
	    for( int i = 0; i < 64; i++ )
	    {
		if( d < probabilities[i] )
		{
		    actionIndex = i;
		    break;
		}
	    }
	    
	    
	    int jointAction0 = actionIndex / 16;
	    retAction.setAction(0, jointAction0);
	    retAction.setAction(1, (actionIndex - jointAction0*16)/4);
	    retAction.setAction(2, actionIndex % 4);
	}
	
	return retAction;
    }
    
    /**
     * get the Q-values of a correlated equilibrium
     */
    private double[] getCEQValues( GameState gameState, double[] correlatedE )
    {
	double[] values = new double[GridWorld.NUM_AGENTS];
	for( int i = 0; i < GridWorld.NUM_AGENTS; i++ )
	    values[i] = 0.0;
	
	if( correlatedE == null ) {
	    
	    
	    return values;
	}
	
	
	double[][][][] qs = new double[GridWorld.NUM_AGENTS][][][];
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int loc2 = gameState.getLocationID(2);
	
	qs[0] = Qs[0][loc0][loc1][loc2];
	qs[1] = Qs[1][loc0][loc1][loc2];
	qs[2] = Qs[2][loc0][loc1][loc2];
	
	for( int agent = 0; agent < GridWorld.NUM_AGENTS; agent++ )
	    for( int i = 0; i < GameAction.NUM_ACTIONS; i++ )
		for( int j = 0; j < GameAction.NUM_ACTIONS; j++ )
		    for( int k = 0; k < GameAction.NUM_ACTIONS; k++ )
		    {
			int index = i * 16 + j * 4 + k;
			values[agent] += correlatedE[index] * qs[agent][i][j][k];
		    }
	
	return values;
    }
    
    
    

    
}
