package algorithms;

import java.util.ArrayList;

import linearGridWorld.GameAction;
import linearGridWorld.GameState;
import linearGridWorld.LinearGridWorld;

import drasys.or.mp.Problem;
import drasys.or.mp.SizableProblemI;
import drasys.or.mp.lp.DenseSimplex;
import drasys.or.mp.lp.LinearProgrammingI;


public class ECEQTrans extends CEQTrans
{
    public ECEQTrans()
    {
	/**
	 * index is no use for centralized CE-Q
	 */
	super();
	
    }
    
    
    public ECEQTrans( double alpha, double gamma, double epsilon )
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
	else if( agent < 0 || agent >= LinearGridWorld.NUM_AGENTS ) {
	    
	    System.out.println();
	    return null;
	}
	
	
	return computeCE_egalitarian( gameState );
    }
    
    //check again
    private double[] computeCE_egalitarian( GameState gameState )
    {
	
	int jointActionNum = LinearGridWorld.allJointActions.size();
	SizableProblemI problem = new Problem(300, jointActionNum);
	problem.getMetadata().put("lp.isMaximize", "true");
	
	double[] ce = new double[jointActionNum];
	for( int i = 0; i < jointActionNum; i++ )
	    ce[i] = 0.0;
	
	try 
	{
	    
	    double maxAns = Double.NEGATIVE_INFINITY;
	    
	    ArrayList<GameAction> jntActions = LinearGridWorld.allJointActions;	    
	    for( int agent = 0; agent < LinearGridWorld.NUM_AGENTS; agent++ ) {
		  
		for( int actionIndex = 0; actionIndex < jntActions.size(); actionIndex++ )  {
			
		    GameAction jntAction = jntActions.get( actionIndex );
		    double coeff = -getQValue( agent, gameState, jntAction );
		    
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
	    
	    /**
	    if( ce != null ) {
		
		for( int actionIndex = 0; actionIndex < jointActionNum; actionIndex++ ) {
		    
		    System.out.print(ce[actionIndex]+", ");
		}
		System.out.println();
	    }
	    else {
		
		System.out.println("No CE");
	    }
	    */
	    
	    return ce;
	}
	catch(Exception e)
	{
	    e.printStackTrace();
	    return null;
	}
    }
    

}
