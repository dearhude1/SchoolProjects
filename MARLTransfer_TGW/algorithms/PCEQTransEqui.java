package algorithms;

import gameGridWorld.GameAction;
import gameGridWorld.GameState;
import gameGridWorld.GridWorld;
import drasys.or.mp.Problem;
import drasys.or.mp.SizableProblemI;
import drasys.or.mp.lp.DenseSimplex;
import drasys.or.mp.lp.LinearProgrammingI;

public class PCEQTransEqui extends CEQTransEqui
{

    public PCEQTransEqui()
    {
	/**
	 * index is no use for centralized CE-Q
	 */
	super();
	
    }
    
    
    public PCEQTransEqui( double alpha, double gamma, double epsilon )
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
	
	double[][] q1, q2;
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	q1 = Qs[0][loc0][loc1];
	q2 = Qs[1][loc0][loc1];
	
	return computeCE_plutocratic( q1, q2 );
    }
    
    private double[] computeCE_plutocratic( double[][] q1, double[][] q2 )
    {
	
	double[] ce = new double[16];
	for( int i = 0; i < 16; i++ )
	    ce[i] = 0.0;
	
	SizableProblemI problem = new Problem(41, 16);
	problem.getMetadata().put("lp.isMaximize", "true");
	
	
	try 
	{
	    /**
	     * set agent 1's expectation as 
	     * the objective function
	     */
	    for( int i = 0; i < GameAction.NUM_ACTIONS; i++ )
	    {
		for( int j = 0; j < GameAction.NUM_ACTIONS; j++ )
		{
		    String a = GameAction.getActionString(i);
		    String b = GameAction.getActionString(j);
		    problem.newVariable(a+":"+b).setObjectiveCoefficient( q1[i][j] );
		}
	    }
	    
	    setConstraints_CE( problem, q1, q2 );
	    
	    LinearProgrammingI iLP;
	    iLP = new DenseSimplex(problem);
	    double ans = iLP.solve();
	    ce = iLP.getSolution().getArray();
	    
	    /**
	     * agent 2's expectation
	     */
	    for( int i = 0; i < GameAction.NUM_ACTIONS; i++ )
	    {
		for( int j = 0; j < GameAction.NUM_ACTIONS; j++ )
		{
		    
		    String a = GameAction.getActionString(i);
		    String b = GameAction.getActionString(j);
		    problem.getVariable(a+":"+b).setObjectiveCoefficient( q2[i][j] );
		}
	    }
	    iLP.setProblem(problem);
	    double ans2 = iLP.solve();
	    if( ans < ans2 )
	    {
		ans = ans2;
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
