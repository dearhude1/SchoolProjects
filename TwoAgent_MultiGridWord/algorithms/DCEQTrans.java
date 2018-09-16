package algorithms;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import multiGridWorld.GameAction;
import multiGridWorld.GameState;
import multiGridWorld.MultiGridWorld;

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
	else if( agent < 0 || agent >= MultiGridWorld.NUM_AGENTS ) {
	    
	    System.out.println();
	    return null;
	}
	
	double[][] q1, q2;
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int worldIndex = gameState.getWorldIndex();
	
	q1 = Qs[0][worldIndex][loc0][loc1];
	q2 = Qs[1][worldIndex][loc0][loc1];
	
	return computeCE_dictatorial( agent, q1, q2 );
    }
    
    
    private double[] computeCE_dictatorial( int agent, double[][] q1, double[][] q2 )
    {
	
	double[] ce = new double[16];
	for( int i = 0; i < 16; i++ )
	    ce[i] = 0.0;
	
	SizableProblemI problem = new Problem(41, 16);
	problem.getMetadata().put("lp.isMaximize", "true");
	
	
	try 
	{

	    for( int i = 0; i < GameAction.NUM_ACTIONS; i++ ) {
		for( int j = 0; j < GameAction.NUM_ACTIONS; j++ ) {
		    
		    String a = GameAction.getActionString(i);
		    String b = GameAction.getActionString(j);
		    if( agent == 0 ) {
			problem.newVariable(a+":"+b).setObjectiveCoefficient( q1[i][j] );
		    }
		    else {
			problem.newVariable(a+":"+b).setObjectiveCoefficient( q2[i][j] );
		    }
		}
	    }
	    
	    setConstraints_CE( problem, q1, q2 );
	    
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
