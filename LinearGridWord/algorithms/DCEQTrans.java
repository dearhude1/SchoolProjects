package algorithms;

import java.util.ArrayList;

import linearGridWorld.GameAction;
import linearGridWorld.GameState;
import linearGridWorld.LinearGridWorld;

import drasys.or.mp.Problem;
import drasys.or.mp.SizableProblemI;
import drasys.or.mp.lp.DenseSimplex;
import drasys.or.mp.lp.LinearProgrammingI;

public class DCEQTrans extends DecenCEQTrans
{

    public DCEQTrans( int index )
    {
	/**
	 * index is no use for centralized CE-Q
	 */
	super( index );
	
    }
    
    
    public DCEQTrans( int index, double alpha, double gamma, double epsilon )
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
	else if( agent < 0 || agent >= LinearGridWorld.NUM_AGENTS ) {
	    
	    System.out.println();
	    return null;
	}
	
	
	return computeCE_dictatorial( agent, gameState );
    }
    
    //check again
    private double[] computeCE_dictatorial( int agent, GameState gameState )
    {
	
	int jointActionNum = LinearGridWorld.allJointActions.size();
	SizableProblemI problem = new Problem(300, jointActionNum);
	problem.getMetadata().put("lp.isMaximize", "true");
	
	double[] ce = new double[jointActionNum];
	for( int i = 0; i < jointActionNum; i++ )
	    ce[i] = 0.0;
	
	
	try 
	{
	    ArrayList<GameAction> jntActions = LinearGridWorld.allJointActions;	
	    
	    for( int actionIndex = 0; actionIndex < jntActions.size(); actionIndex++ )  {
		
		GameAction jntAction = jntActions.get( actionIndex );
		double coeff = getQValue( agent, gameState, jntAction );
		
		problem.newVariable(""+actionIndex).setObjectiveCoefficient( coeff );
	    }
	    setConstraints_CE( problem, gameState );
	    
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
