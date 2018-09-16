package algorithms;

import drasys.or.matrix.VectorI;
import drasys.or.mp.Problem;
import drasys.or.mp.SizableProblemI;
import drasys.or.mp.lp.DenseSimplex;
import drasys.or.mp.lp.LinearProgrammingI;
import gameGridWorld.GameAction;
import gameGridWorld.GameState;
import gameGridWorld.GridWorld;

public class UCEQTransEqui extends CEQTransEqui
{

    public UCEQTransEqui()
    {
	/**
	 * index is no use for centralized CE-Q
	 */
	super();
	
    }
    
    
    public UCEQTransEqui( double alpha, double gamma, double epsilon )
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
	
	return computeCE_utilitarian( q1, q2 );
    }
    
    private double[] computeCE_utilitarian( double[][] q1, double[][] q2 )
    {
	
	double[] ce = new double[16];
	for( int i = 0; i < 16; i++ )
	    ce[i] = 0.0;
	
	/**
	 * 16 variables (16 joint actions)
	 * at least 41 constraints
	 */
	SizableProblemI problem = new Problem(41, 16);
	problem.getMetadata().put("lp.isMaximize", "true");
	
	
	try 
	{
	    /**
	     * set the objective function
	     * maximize the sum of all joint actions' utilities
	     */
	    for( int i = 0; i < GameAction.NUM_ACTIONS; i++ )
	    {
		for( int j = 0; j < GameAction.NUM_ACTIONS; j++ )
		{
		    String a = GameAction.getActionString(i);
		    String b = GameAction.getActionString(j);
		    /**
		     * set the coefficient of the joint action
		     * the name of the joint action (i,j,k) is "a:b:c"
		     */
		    problem.newVariable(a+":"+b).setObjectiveCoefficient( q1[i][j]+q2[i][j] );
		}
	    }
	    
	    /**
	     * set the constraints of the problem
	     */
	    setConstraints_CE( problem, q1, q2 );
	    
	    /**
	     * set up the linear programming 
	     * and solve it
	     */
	    LinearProgrammingI iLP;
	    iLP = new DenseSimplex(problem);
	    iLP.solve();
	    VectorI v = iLP.getSolution();
	    ce = v.getArray();
	    
	    
	    return ce;
	}
	catch(Exception e)
	{
	    //e.printStackTrace();
	    return null;
	}
    }
    
}
