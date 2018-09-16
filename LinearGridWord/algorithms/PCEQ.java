package algorithms;

import java.util.ArrayList;
import linearGridWorld.GameAction;
import linearGridWorld.GameState;
import linearGridWorld.LinearGridWorld;

import drasys.or.mp.Problem;
import drasys.or.mp.SizableProblemI;
import drasys.or.mp.lp.DenseSimplex;
import drasys.or.mp.lp.LinearProgrammingI;

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
	    
	    System.out.println("pCEQ->computeCE: NULL gameState!");
	    return null;
	}
	else if( agent < 0 || agent >= LinearGridWorld.NUM_AGENTS ) {
	    
	    System.out.println();
	    return null;
	}
	
	
	return computeCE_plutocratic( gameState );
    }
    
    private double[] computeCE_plutocratic( GameState gameState )
    {
	
	int jointActionNum = LinearGridWorld.allJointActions.size();
	SizableProblemI problem = new Problem(300, jointActionNum);
	problem.getMetadata().put("lp.isMaximize", "true");
	
	double[] ce = new double[jointActionNum];
	for( int i = 0; i < jointActionNum; i++ ) {
	    ce[i] = 1.0 / ((double) jointActionNum);
	}
	
	
	try 
	{
	    
	    double maxAns = Double.NEGATIVE_INFINITY;
	    
	    ArrayList<GameAction> jntActions = LinearGridWorld.allJointActions;	    
	    for( int agent = 0; agent < LinearGridWorld.NUM_AGENTS; agent++ ) {
		
		for( int actionIndex = 0; actionIndex < jntActions.size(); actionIndex++ )  {
			
		    GameAction jntAction = jntActions.get( actionIndex );
		    double coeff = getQValue( agent, gameState, jntAction );
		    
		    //set coeff
		    if( agent == 0 )
			problem.newVariable(""+actionIndex).setObjectiveCoefficient( coeff );
		    else
			problem.getVariable(""+actionIndex).setObjectiveCoefficient( coeff );
		    
		}
		if( agent == 0 )
		    setConstraints_CE( problem, gameState );
		
		LinearProgrammingI iLP;
		iLP = new DenseSimplex(problem);
		double ans = iLP.solve();
		ce = iLP.getSolution().getArray();
		
		if( ans > maxAns ) {
		    
		    maxAns = ans;
		    ce = iLP.getSolution().getArray();
		}
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
