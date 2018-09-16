package algorithms;

import drasys.or.mp.Problem;
import drasys.or.mp.SizableProblemI;
import drasys.or.mp.lp.DenseSimplex;
import drasys.or.mp.lp.LinearProgrammingI;
import gameGridWorld.GameAction;
import gameGridWorld.GameState;
import gameGridWorld.GridWorld;

public class PCEQ extends CenCEQ
{
    public PCEQ()
    {
	/**
	 * index is no use for centralized CE-Q
	 */
	super();
	
    }
    
    
    public PCEQ( double alpha, double gamma, double epsilon )
    {
	
	super( alpha, gamma, epsilon);
	
    }
    
    /**
     * compute a correlated equilibrium
     * @param agent
     * @param gameState
     * @return
     */
    protected double[] computeCE( int agent, GameState gameState )
    {
	
	if( gameState == null ) {
	    
	    System.out.println("@CenCEQ->computeCE: NULL gameState!");
	    return null;
	}
	else if( agent < 0 || agent >= GridWorld.NUM_AGENTS ) {
	    
	    System.out.println();
	    return null;
	}
	
	double[][][] q1, q2, q3;
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int loc2 = gameState.getLocationID(2);
	q1 = Qs[0][loc0][loc1][loc2];
	q2 = Qs[1][loc0][loc1][loc2];
	q3 = Qs[2][loc0][loc1][loc2]; 
	
	return computeCE_plutocratic( q1, q2, q3);
    }
    
    private double[] computeCE_plutocratic( double[][][] q1, double[][][] q2, 
	    double[][][] q3 )
    {
	
	double[] ce = new double[64];
	for( int i = 0; i < 64; i++ )
	    ce[i] = 0.0;
	
	SizableProblemI problem = new Problem(36, 64);
	problem.getMetadata().put("lp.isMaximize", "true");
	
	
	try 
	{
	    /**
	     * set agent 1's expectation as 
	     * the objective function
	     */
	    for( int i = 0; i < 4; i++ )
	    {
		for( int j = 0; j < 4; j++ )
		{
		    for( int k = 0; k < 4; k++ )
		    {
			String a = GameAction.getActionString(i);
			String b = GameAction.getActionString(j);
			String c = GameAction.getActionString(k);
			
			problem.newVariable(a+":"+b+":"+c).setObjectiveCoefficient(
				q1[i][j][k]);
		    }
		}
	    }
	    
	    setConstraints_CE( problem, q1, q2, q3 );
	    
	    LinearProgrammingI iLP;
	    iLP = new DenseSimplex(problem);
	    double ans = iLP.solve();
	    ce = iLP.getSolution().getArray();
	    
	    /**
	     * agent 2's expectation
	     */
	    for( int i = 0; i < 4; i++ )
	    {
		for( int j = 0; j < 4; j++ )
		{
		    for( int k = 0; k < 4; k++ )
		    {
			String a = GameAction.getActionString(i);
			String b = GameAction.getActionString(j);
			String c = GameAction.getActionString(k);
			
			problem.getVariable(a+":"+b+":"+c).setObjectiveCoefficient(
				q2[i][j][k]);
		    }
		}
	    }
	    iLP.setProblem(problem);
	    double ans2 = iLP.solve();
	    if( ans < ans2 )
	    {
		ans = ans2;
		ce = iLP.getSolution().getArray();
	    }
	   
	    
	    /**
	     * agent 3's expectation
	     */
	    for( int i = 0; i < 4; i++ )
	    {
		for( int j = 0; j < 4; j++ )
		{
		    for( int k = 0; k < 4; k++ )
		    {
			String a = GameAction.getActionString(i);
			String b = GameAction.getActionString(j);
			String c = GameAction.getActionString(k);
			
			problem.getVariable(a+":"+b+":"+c).setObjectiveCoefficient(
				q3[i][j][k]);
		    }
		}
	    }
	    iLP.setProblem(problem);
	    double ans3 = iLP.solve();
	    if( ans < ans3 )
	    {
		ans = ans3;
		ce = iLP.getSolution().getArray();
	    }
	    
	    return ce;
	}
	catch(Exception e)
	{
	    //e.printStackTrace();
	    return null;
	}

    }
}
