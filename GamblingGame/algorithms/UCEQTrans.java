package algorithms;

import gamGam.GameAction;
import gamGam.GameState;
import gamGam.GamblingGame;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.util.ArrayList;


import drasys.or.matrix.VectorI;
import drasys.or.mp.Problem;
import drasys.or.mp.SizableProblemI;
import drasys.or.mp.lp.DenseSimplex;
import drasys.or.mp.lp.LinearProgrammingI;

public class UCEQTrans extends CEQTrans
{

    public UCEQTrans()
    {
	/**
	 * index is no use for centralized CE-Q
	 */
	super();
	
    }
    
    
    public UCEQTrans( double alpha, double gamma, double epsilon )
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
	    
	    System.out.println("uCEQ->computeCE: NULL gameState!");
	    return null;
	}
	else if( agent < 0 || agent >= GamblingGame.NUM_AGENTS ) {
	    
	    System.out.println();
	    return null;
	}
	
	
	return computeCE_utilitarian( gameState );
	//return computeCE_utilitarianCplex( gameState );
    }
    
    
    private double[] computeCE_utilitarian( GameState gameState )
    {
	
	
	int jointActionNum = GamblingGame.allJointActions.size();
	SizableProblemI problem = new Problem(300, jointActionNum);
	problem.getMetadata().put("lp.isMaximize", "true");
	
	double[] ce = new double[jointActionNum];
	for( int i = 0; i < jointActionNum; i++ )
	    ce[i] = 0.0;
	
	try 
	{
	    /**
	     * set the objective function
	     * maximize the sum of all joint actions' utilities
	     */
	    //remember the variable indices
	    ArrayList<GameAction> jntActions = GamblingGame.allJointActions;
	    for( int actionIndex = 0; actionIndex < jntActions.size(); actionIndex++ ) {
		
		GameAction jntAction = jntActions.get( actionIndex );
		double coeff = 0.0;
		for(  int agent = 0; agent < GamblingGame.NUM_AGENTS; agent++ ) {
		    
		    coeff += getQValue( agent, gameState, jntAction );
		}
		
		problem.newVariable(""+actionIndex).setObjectiveCoefficient( coeff );
	    }
	    
	    /**
	     * set the constraints of the problem
	     */
	    setConstraints_CE( problem, gameState );
	    
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
    
    
    /**
     * use cplex to solve the linear program
     */
    private double[] computeCE_utilitarianCplex( GameState gameState ) 
    {
	/**
	 * the number of variables
	 */
	int varNum = GamblingGame.allJointActions.size();
	
	double[] solution = null;
	
	try {
	    
	    IloCplex linearProgram = new IloCplex();
	    
	    /**
	     * 1. first create variables
	     */
	    double[] lowBounds = new double[varNum];
	    double[] uppBounds = new double[varNum];
	    for( int varIndex = 0; varIndex < varNum; varIndex++ ) {
		
		lowBounds[varIndex] = 0.0;
		uppBounds[varIndex] = 1.0;
	    }
	    IloNumVar[] p = linearProgram.numVarArray( varNum, lowBounds, uppBounds);
	    
	    
	    /**
	     * 2. set the constraints of correlated equilibrium
	     */
	    for( int agent = 0; agent < GamblingGame.NUM_AGENTS; agent++ ) {

		/**
		 * loop for action ai
		 */
		for( int ai = 0; ai < GameAction.NUM_ACTIONS; ai++ ) {
		    /**
		     * loop for action ai'
		     */
		    for( int aip = 0; aip < GameAction.NUM_ACTIONS; aip++ ) {
			    
			if( aip == ai )
			    continue;
			
			/**
			 * set the coefficient
			 */
			double[] coeffs = new double[varNum];
			for( int varIndex = 0; varIndex < varNum; varIndex++ )
			    coeffs[varIndex] = 0.0;
			
			//generate all other joint actions
			ArrayList<GameAction> othJntActList_a = generateOtherJntActions( agent );
			ArrayList<GameAction> othJntActList_ap = generateOtherJntActions( agent );
			for( int listIndex = 0; listIndex < othJntActList_a.size(); listIndex++ ) {
				
			    GameAction jntAction_a = othJntActList_a.get(listIndex);
			    GameAction jntAction_ap = othJntActList_ap.get(listIndex);
			    jntAction_a.setAction( agent, ai );
			    jntAction_ap.setAction( agent, aip );
				
			    double Q_sa = getQValue( agent, gameState, jntAction_a );
			    double Q_sap = getQValue( agent, gameState, jntAction_ap );
			    
			    int variableIndex = GamblingGame.queryJointActionIndex( jntAction_a );
			    coeffs[variableIndex] = Q_sa - Q_sap;
			}
			
			//the constraint of inequality
			IloNumExpr ineqExpr = linearProgram.scalProd( coeffs, p );
			linearProgram.addGe( ineqExpr, 0.0 );
		    }
		}
	    }
	    
	    /**
	     * 3. the constraint that the sum of all 
	     * joint actions' probabilities is 1
	     */
	    
	    IloNumExpr sumExpr = linearProgram.constant(0);
	    for( int varIndex = 0; varIndex < varNum; varIndex++ ) {
		
		sumExpr = linearProgram.sum( sumExpr, p[varIndex] );
	    }
	    linearProgram.addEq( sumExpr, 1.0 );
	    	
	    /**
	     * 4. set the objective
	     */
	    double[] coeffs = new double[varNum];
	    ArrayList<GameAction> jntActions = GamblingGame.allJointActions;
	    
	    for( int actionIndex = 0; actionIndex < jntActions.size(); actionIndex++ ) {
		
		GameAction jntAction = jntActions.get( actionIndex );
		coeffs[actionIndex] = 0.0;
		
		for(  int agent = 0; agent < GamblingGame.NUM_AGENTS; agent++ ) {
		    
		    coeffs[actionIndex] += getQValue( agent, gameState, jntAction );
		}
	    }
	    linearProgram.add(linearProgram.maximize(linearProgram.scalProd( coeffs, p )));
	    
	    /**
	     * 5. solve the linear program
	     */
	    if( linearProgram.solve() ) {
		
		//solution = new double[varNum]; 
		//nashCP.getValues( p, solution );
		
		solution = linearProgram.getValues( p );
		
		/**
		for( int neIndex = 0; neIndex < varNum; neIndex++ ) {
		    
		    System.out.println("NE"+neIndex+": "+solution[neIndex]);
		}
		*/
	    }
	    linearProgram.end();
	    
	    //release the memory?
	    linearProgram = null;
	    
	}
	catch( IloException iloE ) {
	    
	    System.err.println("Concert exception '" + iloE + "' caught");
	}
	
	return solution;
    }
    
}
