package algorithms;

import multiGridWorld.GameAction;
import multiGridWorld.GameState;
import multiGridWorld.MultiGridWorld;
import drasys.or.mp.Constraint;
import drasys.or.mp.SizableProblemI;

/**
 * decentralized CE-Q algorithm
 * @author dearhude1
 *
 */
public class DecenCEQ extends MARL
{

    //The three members below are used for counting how many games are similar during learning
    //for counting the games in each state
    private double[][][] gameCounters;
    
    //for counting similar games in each state
    private double[][][] simiGameCounters;
    
    //the last Nash equilibrium in each state
    protected double[][][][] lastEquilibrium;
    ////////////////////////////////////////////////////////////////////////////////////////////
    
    public DecenCEQ( int index )
    {
	super(index);
	
	/**
	 * init the Q-table
	 * for centralized algorithms 
	 * the Q-tables can be initialized randomly
	 */
	int locNum = MultiGridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	int worldNum = MultiGridWorld.WORLD_NUM;
	
	/**
	 * init Q-tables again
	 */
	for( int agentIndex = 0; agentIndex < MultiGridWorld.NUM_AGENTS; agentIndex++ )
	    for( int wldIndex = 0; wldIndex < worldNum; wldIndex++ )
		for( int s1 = 0; s1 < locNum; s1++ )
		    for( int s2 = 0; s2 < locNum; s2++ )
			for( int a1 = 0; a1 < actionNum; a1++ )
			    for( int a2 = 0; a2 < actionNum; a2++ ) {
				    
				Qs[agentIndex][wldIndex][s1][s2][a1][a2] = (Math.random() - 0.5) / 10.0;
			    }
	
	//init the three testing members
	gameCounters = new double[worldNum][locNum][locNum];
	simiGameCounters = new double[worldNum][locNum][locNum];
	lastEquilibrium = new double[worldNum][locNum][locNum][GameAction.NUM_ACTIONS * GameAction.NUM_ACTIONS];
	for( int wldIndex = 0;  wldIndex < worldNum; wldIndex++ )
	    for( int s1 = 0; s1 < locNum; s1++ )
		for( int s2 = 0; s2 < locNum; s2++ ) {
		
		    gameCounters[wldIndex][s1][s2] = simiGameCounters[wldIndex][s1][s2] = 0.0;
		
		    for( int jntAct = 0; jntAct < GameAction.NUM_ACTIONS * GameAction.NUM_ACTIONS; jntAct++ )
			lastEquilibrium[wldIndex][s1][s2][jntAct] = 0.0;
	    }
    }
    
    public DecenCEQ( int index, double alpha, double gamma, double epsilon )
    {
	super(index, alpha, gamma, epsilon);
	
	/**
	 * init the Q-table
	 * for centralized algorithms 
	 * the Q-tables can be initialized randomly
	 */
	int locNum = MultiGridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	int worldNum = MultiGridWorld.WORLD_NUM;
	
	/**
	 * init Q-tables again
	 */
	for( int agentIndex = 0; agentIndex < MultiGridWorld.NUM_AGENTS; agentIndex++ )
	    for( int wldIndex = 0; wldIndex < worldNum; wldIndex++ )
		for( int s1 = 0; s1 < locNum; s1++ )
		    for( int s2 = 0; s2 < locNum; s2++ )
			for( int a1 = 0; a1 < actionNum; a1++ )
			    for( int a2 = 0; a2 < actionNum; a2++ ) {
				    
				Qs[agentIndex][wldIndex][s1][s2][a1][a2] = (Math.random() - 0.5) / 10.0;
			    }
	
	//init the three testing members
	gameCounters = new double[worldNum][locNum][locNum];
	simiGameCounters = new double[worldNum][locNum][locNum];
	lastEquilibrium = new double[worldNum][locNum][locNum][GameAction.NUM_ACTIONS * GameAction.NUM_ACTIONS];
	for( int wldIndex = 0;  wldIndex < worldNum; wldIndex++ )
	    for( int s1 = 0; s1 < locNum; s1++ )
		for( int s2 = 0; s2 < locNum; s2++ ) {
		
		    gameCounters[wldIndex][s1][s2] = simiGameCounters[wldIndex][s1][s2] = 0.0;
		
		    for( int jntAct = 0; jntAct < GameAction.NUM_ACTIONS * GameAction.NUM_ACTIONS; jntAct++ )
			lastEquilibrium[wldIndex][s1][s2][jntAct] = 0.0;
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
	    
	    System.out.println("@DecenCEQ->updateQ: NULL nextState!");
	    
	    return null;
	}
	else {
	    
	    /**
	     * compute the correlated equilibrium in the next state
	     */
	    double[] correlEquil = computeCE( agentIndex, nextState );
	    
	    //the following code is for testing similar games during learning
	    //then compare the current equilibrium with the last one
	    int loc0 = nextState.getLocationID(0);
	    int loc1 = nextState.getLocationID(1);
	    int worldIndex = nextState.getWorldIndex();
	    
	    /**
	    if( correlEquil != null && gameCounters[worldIndex][loc0][loc1] > 0.01 ) {
		
		//distance of two Nash equilibrium
		double dis = 0.0;
		for( int jntAct = 0; jntAct < GameAction.NUM_ACTIONS * GameAction.NUM_ACTIONS; jntAct++ ) {
		    
		    double diff = lastEquilibrium[worldIndex][loc0][loc1][jntAct] - correlEquil[jntAct];
		    dis += diff * diff;
		    
		}
		dis = Math.sqrt(dis);
		
		double x = 0.01;//0.0000001;
		if( dis < x ) {   
		    simiGameCounters[worldIndex][loc0][loc1] += 1.0;
		}
	    }
	    gameCounters[worldIndex][loc0][loc1] += 1.0;
	    //store the current equilibrium
	    for( int jntAct = 0; jntAct < GameAction.NUM_ACTIONS * GameAction.NUM_ACTIONS; jntAct++ ) {
		    
		if( correlEquil != null )
		    lastEquilibrium[worldIndex][loc0][loc1][jntAct] = correlEquil[jntAct];
		else
		    lastEquilibrium[worldIndex][loc0][loc1][jntAct] = 0.0;
	    }
	    */
	    //////////////////////////////////////////////////////////////////////
	    
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
		    
		for( int agent = 0; agent < MultiGridWorld.NUM_AGENTS; agent++ ) {
		    
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
		    //double alpha = getVariableAlpha( curState, jointAction );
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
    protected void setConstraints_CE( SizableProblemI problem, double[][] q1,
	    double[][] q2 ) throws Exception
    {
	/**
	 * 2x4x3 = 24 constraints
	 * for the inequalities of correlated equilibrium
	 */
	for( int agent = 0; agent < MultiGridWorld.NUM_AGENTS; agent++ )
	{
	    double[][] q = null;
	    if( agent == 0 )
		q = q1;
	    else
		q = q2;
	    
	    /**
	     * 4x3 = 12 constraints
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
		    for( int j = 0; j < GameAction.NUM_ACTIONS; j++ ) {
			
			String bString = GameAction.getActionString(j);
			
			if( agent == 0 )
			    problem.setCoefficientAt( conString, aiString+":"+bString, 
				    q[ai][j]-q[aip][j]);
			else
			    problem.setCoefficientAt( conString, bString+":"+aiString, 
				    q[j][ai]-q[j][aip]);
		    }
		}
	    }
	}
	
	/**
	 * 1 constraint
	 * the constraint that the sum of all 
	 * joint actions' probabilities is 1
	 */
	String eqCon = "equalConstraint";
	int index = problem.newConstraint(eqCon).
		setType(Constraint.EQUAL).setRightHandSide(1.0).getRowIndex();
	for( int i = 0; i < 16; i++ )
	{
	    problem.setCoefficientAt(index, i, 1.0);
	}
	
	/**
	 * 16 constraints
	 * the constraint of each joint action 
	 * that its probability is larger than 0
	 */
	for( int i = 0; i < 16; i++ )
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
    protected GameAction getJointAction_CE( double[] correlatedE )
    {	
	GameAction retAction = new GameAction();
	
	if( correlatedE == null )
	{
	    retAction.setAction(0, random.nextInt(GameAction.NUM_ACTIONS));
	    retAction.setAction(1, random.nextInt(GameAction.NUM_ACTIONS));
	}
	
	else {
	    
	    double[] probabilities = new double[16];
		
	    probabilities[0] = correlatedE[0];
	    for( int i = 1; i < 16; i++ )
	    {
		probabilities[i] =  probabilities[i-1] + correlatedE[i];
	    }
		
	    double d = random.nextDouble();
	    int actionIndex = 0;
	    for( int i = 0; i < 16; i++ )
	    {
		if( d < probabilities[i] )
		{
		    actionIndex = i;
		    break;
		}
	    }
	    
	    int jointAction0 = actionIndex / GameAction.NUM_ACTIONS;
	    retAction.setAction(0, jointAction0);
	    retAction.setAction(1, actionIndex % GameAction.NUM_ACTIONS);
	}
	
	return retAction;
    }
    
    /**
     * get the Q-values of a correlated equilibrium
     */
    protected double[] getCEQValues( GameState gameState, double[] correlatedE )
    {
	double[] values = new double[MultiGridWorld.NUM_AGENTS];
	for( int i = 0; i < MultiGridWorld.NUM_AGENTS; i++ )
	    values[i] = 0.0;
	
	if( correlatedE == null ) {
	    
	    
	    return values;
	}
	
	
	double[][][] qs = new double[MultiGridWorld.NUM_AGENTS][][];
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int worldIndex = gameState.getWorldIndex();
	
	qs[0] = Qs[0][worldIndex][loc0][loc1];
	qs[1] = Qs[1][worldIndex][loc0][loc1];
	
	for( int agent = 0; agent < MultiGridWorld.NUM_AGENTS; agent++ )
	    for( int i = 0; i < GameAction.NUM_ACTIONS; i++ )
		for( int j = 0; j < GameAction.NUM_ACTIONS; j++ ) {
		    
		    int index = i * GameAction.NUM_ACTIONS + j;
		    values[agent] += correlatedE[index] * qs[agent][i][j]; 
		}
	
	return values;
    }
    
    
}
