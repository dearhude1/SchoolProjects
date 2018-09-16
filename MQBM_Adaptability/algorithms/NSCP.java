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

public class NSCP extends MARL
{
    /**
     * Q-table of NSCP
     */
    protected double[][][][] Q;
    
    /**
     * the polic of NSCP
     * not joint action 
     * only the action of this agent
     */
    protected double[][][] pi;

    /**
     * the estimated policy of the opponent
     */
    protected double[][][] oppModel;
    
    protected int[][][] countSelf;
    protected int[][][] countOpp;
    
    public NSCP( int index )
    {
	super(index);
	
	int locNum = GridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	
	Q = new double[locNum][locNum][actionNum][actionNum];
	pi = new double[locNum][locNum][actionNum];
	countSelf = new int[locNum][locNum][actionNum];
	countOpp = new int[locNum][locNum][actionNum];
	oppModel = new double[locNum][locNum][actionNum];
	for( int s1 = 0; s1 < locNum; s1++ )
	    for( int s2 = 0; s2 < locNum; s2++ ) {
		    
		    for( int a = 0; a < GameAction.NUM_ACTIONS; a++ ) {
			
			pi[s1][s2][a] = 1.0 / GameAction.NUM_ACTIONS;
			oppModel[s1][s2][a] = 1.0 / GameAction.NUM_ACTIONS;
			
			countSelf[s1][s2][a] = 0;
			countOpp[s1][s2][a] = 0;
			
			for( int o = 0; o < GameAction.NUM_ACTIONS; o++ )
			    Q[s1][s2][a][o] = 0.0;
		    }
		}
    }
    
    public NSCP( int index, double alpha, double gamma, double epsilon )
    {
	super(index, alpha, gamma, epsilon);
	
	int locNum = GridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	
	Q = new double[locNum][locNum][actionNum][actionNum];
	pi = new double[locNum][locNum][actionNum];
	countSelf = new int[locNum][locNum][actionNum];
	countOpp = new int[locNum][locNum][actionNum];
	for( int s1 = 0; s1 < locNum; s1++ )
	    for( int s2 = 0; s2 < locNum; s2++ ) {
		    
		    for( int a = 0; a < GameAction.NUM_ACTIONS; a++ ) {
			
			pi[s1][s2][a] = 1.0 / GameAction.NUM_ACTIONS;
			oppModel[s1][s2][a] = 1.0 / GameAction.NUM_ACTIONS;
			
			countSelf[s1][s2][a] = 0;
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
	    
	    System.out.println("@NSCP->updateQ: NULL Next State!");
	    
	    return null;
	}
	
	/**
	 * select action in the next state according to pi(s)
	 */
	GameAction nextAction = getAction_Pi( nextState );
	
	/**
	 * update the Q-tables
	 * but if this is the initial state of the game
	 * just return the action
	 */
	if( curState != null && jointAction != null 
		&& rewards != null ) {
	    
	    /**
	     * mark a visit
	     * and update opponent model
	     */
	    visit( curState, jointAction );
	    
	    /**
	     * update Q-value and policy
	     */
	    double Qsa = getQValue( curState, jointAction );
	    
	    //get the value in the next state
	    double equilValue = getEquilibriumValue( nextState );
	    
	    Qsa = ( 1 - ALPHA ) * Qsa + ALPHA * ( rewards[agentIndex] + GAMMA * equilValue );
	    
	    setQValue( curState, jointAction, Qsa );
	}
	
	return nextAction;
    }
    
    private double getEquilibriumValue( GameState gameState)
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
			getCoefficient(i, s1, s2));
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
    
    public double getCoefficient( int a, int s1, int s2 )
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
    
    protected void visit( GameState curState, GameAction curAction )
    {
	if( curState == null || curAction == null ) {
	    
	    System.out.println("@NSCP->visit: Wrong Parameters!");
	    return;
	}
	
	int loc0 = curState.getLocationID(0);
	int loc1 = curState.getLocationID(1);
	
	countSelf[loc0][loc1][curAction.getAction(agentIndex)] += 1;
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
    
    private double getQValue( GameState gameState, GameAction gameAction )
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
    
    private void setQValue( GameState gameState, GameAction action, 
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
}
