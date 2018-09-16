package algorithms;

import gamGam.GameAction;
import gamGam.GameState;
import gamGam.GamblingGame;

import java.util.ArrayList;

import drasys.or.mp.Problem;
import drasys.or.mp.SizableProblemI;
import drasys.or.mp.lp.DenseSimplex;
import drasys.or.mp.lp.LinearProgrammingI;

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
	else if( agent < 0 || agent >= GamblingGame.NUM_AGENTS ) {
	    
	    System.out.println();
	    return null;
	}
	
	
	return computeCE_dictatorial( agent, gameState );
    }
    
    //check again
    private double[] computeCE_dictatorial( int agent, GameState gameState )
    {
	
	int jointActionNum = GamblingGame.allJointActions.size();
	SizableProblemI problem = new Problem(300, jointActionNum);
	problem.getMetadata().put("lp.isMaximize", "true");
	
	double[] ce = new double[jointActionNum];
	for( int i = 0; i < jointActionNum; i++ )
	    ce[i] = 0.0;
	
	
	try 
	{
	    ArrayList<GameAction> jntActions = GamblingGame.allJointActions;	
	    
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
