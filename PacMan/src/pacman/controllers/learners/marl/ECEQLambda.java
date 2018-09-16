package pacman.controllers.learners.marl;

import java.util.ArrayList;

import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import pacman.game.Game;
import pacman.game.Constants.GHOST;

public class ECEQLambda extends CEQLambda {
	
	public ECEQLambda( Game game, double lambda, boolean bLearn )
	{
		
		super(game, lambda, bLearn);
		
		valueFunctionFileName = "./valueFunction_eCEQLambda_";
		
		/**
		 * read the value function
		 */
		if( !doesLearn )
			readValueFunction( valueFunctionFileName );
	}
	
	public ECEQLambda( Game game, double alpha, 
			double gamma, double epsilon, double lambda, 
			boolean bLearn )
	{
		super(game, alpha, gamma, epsilon, lambda, bLearn);
		
		valueFunctionFileName = "./valueFunction_eCEQLambda_";
		
		/**
		 * read the value function
		 */
		if( !doesLearn )
			readValueFunction( valueFunctionFileName );
	}
	
	
	protected double[] computeCE( Game game, ArrayList<GHOST> gamingGhosts, 
			ArrayList<int[]> gamingJointStrategies )
	{
		
		return computeCE_egalitarian_Cplex(game, gamingGhosts, gamingJointStrategies);
	}
	
	private double[] computeCE_egalitarian_Cplex( Game game, 
			ArrayList<GHOST> gamingGhosts, 
			ArrayList<int[]> gamingJointStrategies ) 
	{
		IloCplex ceLP = null;
		try {
			
		    double maxValue = Double.NEGATIVE_INFINITY;
		    double[] solution = null;
		    for( GHOST ghostType : GHOST.values() ) {
		    	
			    /**
			     * 1. firstly, create the model
			     * IloCplex is used to create Mathmatical Programming Models
			     * such as:
			     * Linear Programming
			     * Quadratic Programming
			     * Quadratically Constrained Program...
			     */
			    ceLP = new IloCplex();
			    
			    /**
			     * 2. secondly, create the variables:
			     * stores each agent's probability of taking each action
			     * agent i's j-th action probability: index = i * Num_Actions
			     */
			    int numGamingGhosts = gamingGhosts.size();
			    int varNum = gamingJointStrategies.size();
			    double[] lowBounds = new double[varNum];
			    double[] uppBounds = new double[varNum];
			    for( int varIndex = 0; varIndex < varNum; varIndex++ ) {
				
			    	lowBounds[varIndex] = 0.0;
			    	uppBounds[varIndex] = 1.0;
			    }
			    IloNumVar[] p = ceLP.numVarArray(varNum, lowBounds, uppBounds);
			    
			    /**
			     * 3. then create the constraints:
			     */
			    setConstraintCE_Cplex(ceLP, p, game, 
			    		gamingGhosts, gamingJointStrategies);

			    
			    /**
			     * set the objective function
			     * maximize the sum of all joint actions' utilities
			     */
		    	double[] coeff = new double[varNum];
		    	for( int listIndex = 0; listIndex < varNum; listIndex++ ) {
		    		
		    		int[] strategies = gamingJointStrategies.get(listIndex);
		    		int jntStrategy = strategies2JntStrategy(strategies);
		    		coeff[listIndex] = 0.0 - getJntQValue(game, ghostType, jntStrategy);
		    	}
			    IloNumExpr objExpr = ceLP.scalProd(coeff, p);
			    ceLP.add(ceLP.maximize(objExpr));
			    
			    if( ceLP.solve() ) {
					
					double objValue = ceLP.getObjValue();
					if( objValue > maxValue ) {
						
						solution = ceLP.getValues( p );
						maxValue = objValue;
					}
			    }
			    
			    ceLP.clearModel();
			    ceLP.end();
			    
			    //release the memory??
			    //ceLP = null;	
		    }
		    
		    return solution;
		}
		catch(Exception e) {
			
			//e.printStackTrace();
			ceLP.end();
			return null;
		}
	}
	
}
