package algorithms;

import drasys.or.mp.Problem;
import drasys.or.mp.SizableProblemI;
import drasys.or.mp.lp.DenseSimplex;
import drasys.or.mp.lp.LinearProgrammingI;
import gameGridWorld.GameAction;
import gameGridWorld.GameState;
import gameGridWorld.GridWorld;

public class DCEQ extends DecenCEQ
{

    public DCEQ( int index )
    {
	super( index );
    }
    
    
    public DCEQ( int index, double alpha, double gamma, double epsilon )
    {
	super( index, alpha, gamma, epsilon);
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
	    
	    System.out.println("@DCEQ->computeCE: NULL gameState!");
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
	
	return computeCE_dictatorial(agent, q1, q2, q3);
    }
    
    
    private double[] computeCE_dictatorial( int agent, double[][][] q1, double[][][] q2, 
	    double[][][] q3 )
    {
	
	double[] ce = new double[64];
	for( int i = 0; i < 64; i++ )
	    ce[i] = 0.0;
	
	SizableProblemI problem = new Problem(36, 64);
	problem.getMetadata().put("lp.isMaximize", "true");
	
	
	try 
	{

	    for( int i = 0; i < 4; i++ )
	    {
		for( int j = 0; j < 4; j++ )
		{
		    for( int k = 0; k < 4; k++ )
		    {
			String a = GameAction.getActionString(i);
			String b = GameAction.getActionString(j);
			String c = GameAction.getActionString(k);
			
			if( agent == 0 )
			{
			    problem.newVariable(a+":"+b+":"+c).setObjectiveCoefficient(
					q1[i][j][k]);
			}
			else if( agent == 1 )
			{
			    problem.newVariable(a+":"+b+":"+c).setObjectiveCoefficient(
					q2[i][j][k]);
			}
			else
			{
			    problem.newVariable(a+":"+b+":"+c).setObjectiveCoefficient(
					q3[i][j][k]);
			}
		    }
		}
	    }
	    
	    setConstraints_CE( problem, q1, q2, q3 );
	    
	    LinearProgrammingI iLP;
	    iLP = new DenseSimplex(problem);
	    iLP.solve();
	    ce = iLP.getSolution().getArray();
	    
	    return ce;
	}
	catch(Exception e)
	{
	    //e.printStackTrace();
	    return null;
	}
    }
}
