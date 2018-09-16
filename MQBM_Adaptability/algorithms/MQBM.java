package algorithms;

import drasys.or.matrix.VectorI;
import drasys.or.mp.Constraint;
import drasys.or.mp.Problem;
import drasys.or.mp.SizableProblemI;
import drasys.or.mp.lp.DenseSimplex;
import drasys.or.mp.lp.LinearProgrammingI;
import gameGridWorld.GameAction;
import gameGridWorld.GameState;
import gameGridWorld.GridWorld;

public class MQBM extends NegoQ
{
    
    /**
     * Q-table of NSCP
     */
    protected double[][][][] Q;
    
    /**
     * the policy of NSCP
     * not joint action 
     * only the action of this agent
     */
    protected double[][][] pi;

    /**
     * the estimated policy of the opponent in NSCP
     */
    protected double[][][] oppModel;
    
    //protected int[][][] countSelf;
    protected int[][][] countOpp;
    
    /**
     * belief in each state
     */
    protected double[][] belief;
    
    /**
     * for updating belief
     */
    protected double delta = 0.01;
    
    public MQBM( int index )
    {
	
	super( index );
	
	int locNum = GridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	Q = new double[locNum][locNum][actionNum][actionNum];
	pi = new double[locNum][locNum][actionNum];
	//countSelf = new int[locNum][locNum][actionNum];
	countOpp = new int[locNum][locNum][actionNum];
	oppModel = new double[locNum][locNum][actionNum];
	belief = new double[locNum][locNum];
	for( int s1 = 0; s1 < locNum; s1++ )
	    for( int s2 = 0; s2 < locNum; s2++ ) {
		    
		belief[s1][s2] = 0.5; 
		for( int a = 0; a < GameAction.NUM_ACTIONS; a++ ) {
			
		    pi[s1][s2][a] = 1.0 / GameAction.NUM_ACTIONS;
		    oppModel[s1][s2][a] = 1.0 / GameAction.NUM_ACTIONS;
			
		    //countSelf[s1][s2][a] = 0;
		    countOpp[s1][s2][a] = 0;
			
		    for( int o = 0; o < GameAction.NUM_ACTIONS; o++ ) {
			
			Q[s1][s2][a][o] = 0.0;
			Qs[0][s1][s2][a][o] = 0.0;
			Qs[1][s1][s2][a][o] = 0.0;
		    }
		}
	    }
    }
    
    public MQBM( int index, double alpha, double gamma, double epsilon )
    {
	super(index, alpha, gamma, epsilon);
	
	int locNum = GridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	Q = new double[locNum][locNum][actionNum][actionNum];
	pi = new double[locNum][locNum][actionNum];
	//countSelf = new int[locNum][locNum][actionNum];
	countOpp = new int[locNum][locNum][actionNum];
	oppModel = new double[locNum][locNum][actionNum];
	belief = new double[locNum][locNum];
	for( int s1 = 0; s1 < locNum; s1++ )
	    for( int s2 = 0; s2 < locNum; s2++ ) {
		    
		belief[s1][s2] = 0.5; 
		for( int a = 0; a < GameAction.NUM_ACTIONS; a++ ) {
			
		    pi[s1][s2][a] = 1.0 / GameAction.NUM_ACTIONS;
		    oppModel[s1][s2][a] = 1.0 / GameAction.NUM_ACTIONS;
			
		    //countSelf[s1][s2][a] = 0;
		    countOpp[s1][s2][a] = 0;
			
		    for( int o = 0; o < GameAction.NUM_ACTIONS; o++ )
			Q[s1][s2][a][o] = 0.0;
		}
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
	    
	    System.out.println("@MQBM->updateQ: NULL Next State!");
	    
	    return null;
	}
	
	/**
	 * find a meta equilibrium according to NegoQ
	 */
	GameAction nextAction_NegoQ = computeAction( nextState );
	
	/**
	 * find a best response action according to NSCP
	 */
	GameAction nextAction_NSCP = getAction_Pi( nextState );
	

	/**
	 * return the action according to belief
	 */
	int nextLoc0 = nextState.getLocationID(0);
	int nextLoc1 = nextState.getLocationID(1);
	GameAction nextAction = null;
	if( random.nextDouble() < belief[nextLoc0][nextLoc1] )
	    nextAction = nextAction_NegoQ;
	else
	    nextAction = nextAction_NSCP;
	
	
	/**
	 * update the Q-tables
	 * but if this is the initial state of the game
	 * just return the action
	 */
	if( curState != null && jointAction != null 
		&& rewards != null ) {
	    
	    /**
	     * update belief??
	     */
	    GameAction metaEq = computeAction( curState );
	    int oppIndex = (agentIndex+1)%2;
	    int loc0 = curState.getLocationID(0);
	    int loc1 = curState.getLocationID(1);
	    if( metaEq.getAction(oppIndex) == 
		    jointAction.getAction(oppIndex) )
		belief[loc0][loc1] += delta * (1-EPSILON);
	    else
		belief[loc0][loc1] -= delta * (1-EPSILON); 
	    if( belief[loc0][loc1] > 1.0 )
		belief[loc0][loc1] = 1.0;
	    if( belief[loc0][loc1] < 0.0 )
		belief[loc0][loc1] = 0.0;
		
	    /**
	     * investigate the belief value
	     */
	    //System.out.println("Agent "+agentIndex+" belief in ("+loc0+","+loc1+"): "+belief[loc0][loc1]);
	    
	    /**
	     * mark a visit
	     */
	    visit( curState, jointAction );
		
	    /**
	     * update Q-tables for NegoQ
	     */
	    for( int agent = 0; agent < GridWorld.NUM_AGENTS; agent++ ) {
		    
		/**
		 * only update the Q-table of this agent
		 */
		double Qsa = getQValue( agent, curState, jointAction );
		double equilValue = getQValue( agent, nextState, nextAction_NegoQ );
			
		Qsa = ( 1 - ALPHA ) * Qsa + ALPHA * ( rewards[agent] + GAMMA * equilValue );
			
		/**
		 * variable learning rate
		 */
		//double alpha = getVariableAlpha( curState, jointAction );
		//Qsa = (1 - alpha) * Qsa + alpha * ( rewards[agent] + GAMMA * equilValue );
		
		setQValue( agent, curState, jointAction, Qsa );
	    }
	    
	    /**
	     * update Q-table for NSCP
	     */
	    double Qsa_NSCP = getQValue_NSCP( curState, jointAction );
	    double equilValue = getEquilibriumValue_NSCP( nextState );
	    Qsa_NSCP = ( 1 - ALPHA ) * Qsa_NSCP + ALPHA * ( rewards[agentIndex] + GAMMA * equilValue );
	    
	    setQValue_NSCP( curState, jointAction, Qsa_NSCP );
	}
	
	return nextAction;
    }
    
    
    protected void visit( GameState curState, GameAction curAction )
    {
	if( curState == null || curAction == null ) {
	    
	    System.out.println("@MQBM->visit: Wrong Parameters!");
	    return;
	}
	
	int loc0 = curState.getLocationID(0);
	int loc1 = curState.getLocationID(1);
	int a0 = curAction.getAction(0);
	int a1 = curAction.getAction(1);
	vstNum[loc0][loc1][a0][a1] += 1.0;
	
	//countSelf[loc0][loc1][curAction.getAction(agentIndex)] += 1;
	countOpp[loc0][loc1][curAction.getAction((agentIndex+1)%2)] += 1;
	
	/**
	 * update opponent model
	 */
	double allCount = 0.0;
	for( int o = 0; o < GameAction.NUM_ACTIONS; o++ ) {
	    
	    allCount += countOpp[loc0][loc1][o];
	}
	for( int o = 0; o < GameAction.NUM_ACTIONS; o++ ) {
	    
	    oppModel[loc0][loc1][o] = countOpp[loc0][loc1][o] / allCount; 
	}
    }

    /**
     * get an action, NSCP part
     */
    private GameAction getAction_Pi( GameState gameState )
    {
	if( gameState == null ) {
	    
	    System.out.println("@NSCP->getAction_Pi: NULL Parameter!");
	    return null;
	}
	
	double[] probabilities = new double[GameAction.NUM_ACTIONS];
		
	
	probabilities[0] = getPi(gameState, 0);
	for( int act = 1; act < GameAction.NUM_ACTIONS; act++ )
	{
	    probabilities[act] =  probabilities[act-1] + getPi(gameState, act);
	}
		
	double d = random.nextDouble();
	int actionIndex = 0;
	for( int act = 0; act < GameAction.NUM_ACTIONS; act++ )
	{
	    if( d < probabilities[act] )
	    {
		actionIndex = act;
		break;
	    }
	}
	
	return new GameAction(new int[]{actionIndex,actionIndex});
    }
    
    /**
     * get the probability of an action in a state, NSCP part
     */
    private double getPi( GameState gameState, int action )
    {
	if( gameState == null ) {
	    
	    System.out.println("@NSCP->getPi: NULL Parameter!");
	    return 0.0;
	}
	else if( action < 0 || action >= GameAction.NUM_ACTIONS ) {
	    
	    System.out.println("@NSCP->getPi: Unavailable Action!");
	    return 0.0;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	
	return pi[loc0][loc1][action];
    }
    
    
    private double getEquilibriumValue_NSCP( GameState gameState)
    {
	
	int s1 = gameState.getLocationID(0);
	int s2 = gameState.getLocationID(1);
	
	SizableProblemI prob = new Problem(5, 4);
	prob.getMetadata().put("lp.isMaximize", "true");

	try
	{
	    for (int i = 0; i < 4; i++)
	    {
		String x = "x" + (i + 1);
		prob.newVariable(x).setObjectiveCoefficient(
			getCoefficient_NSCP(i, s1, s2));
	    }

	    prob.newConstraint("con").setType(Constraint.EQUAL)
		    .setRightHandSide(1.0);
	    for (int i = 0; i < 4; i++)
	    {
		prob.setCoefficientAt("con", "x" + (i + 1), 1.0);
	    }
	    for (int i = 0; i < 4; i++)
	    {
		String con = "aboveZeroConstraint" + (i + 1);
		int index = prob.newConstraint(con).setType(Constraint.GREATER)
			.setRightHandSide(0.0).getRowIndex();
		prob.setCoefficientAt(index, i, 1.0);
	    }
	    LinearProgrammingI lp;
	    lp = new DenseSimplex(prob);
	    double ans = lp.solve();
	    VectorI v = lp.getSolution();
	    
	    pi[s1][s2] = v.getArray(); 
	    
	    return ans;
	}
	catch (Exception e) {
	    return 0.0;
	}
    }
    
    public double getCoefficient_NSCP( int a, int s1, int s2 )
    {
	double ret = 0.0;
	for (int i = 0; i < 4; i++)
	{
	    if( agentIndex == 0 )
		ret += oppModel[s1][s2][i] * Q[s1][s2][a][i];
	    else
		ret += oppModel[s1][s2][i] * Q[s1][s2][i][a]; 
	}
	return ret;
    }
    
    private double getQValue_NSCP( GameState gameState, GameAction gameAction )
    {
	if( gameAction == null || 
		gameState == null ) {
	    
	    System.out.println("@NSCP->getQValue: Wrong Parameters!");
	    return 0.0;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int a0 = gameAction.getAction( 0 );
	int a1 = gameAction.getAction( 1 );
	
	return Q[loc0][loc1][a0][a1];
    }
    
    private void setQValue_NSCP( GameState gameState, GameAction action, 
	    double value )
    {
	if( gameState == null || action == null ) {
	    
	    System.out.println("@NSCP->setQValue: Wrong Parameters!");
	    return;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int a0 = action.getAction(0);
	int a1 = action.getAction(1);
	
	Q[loc0][loc1][a0][a1] = value;
    }
    
    
    public double getBelief( int s1, int s2 )
    {
	if( s1 < 0 || s1 >= GridWorld.NUM_LOCATIONS ||
		s2 < 0 || s2 >= GridWorld.NUM_LOCATIONS ) {
	    
	    System.out.println("@MQBM->getBelief: Wrong States!");
	    return 0.0;
	}
	
	return belief[s1][s2];
    }
    
    public double[][] getBeliefValues()
    {
	return belief;
    }
}
