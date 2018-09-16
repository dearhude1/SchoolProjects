package pacman.controllers.learners.marl;

import java.util.ArrayList;

import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import drasys.or.matrix.VectorI;
import drasys.or.mp.Problem;
import drasys.or.mp.SizableProblemI;
import drasys.or.mp.lp.DenseSimplex;
import drasys.or.mp.lp.LinearProgrammingI;
import pacman.game.Constants;
import pacman.game.Game;
import pacman.game.Constants.GHOST;

public class UCEQLambda extends CEQLambda {
	
	public UCEQLambda( Game game, double lambda, boolean bLearn )
	{
		
		super(game, lambda, bLearn);
		
		valueFunctionFileName = "./valueFunction_uCEQLambda_";
		
		/**
		 * read the value function
		 */
		if( !doesLearn )
			readValueFunction( valueFunctionFileName );
	}
	
	public UCEQLambda( Game game, double alpha, 
			double gamma, double epsilon, double lambda, 
			boolean bLearn )
	{
		super(game, alpha, gamma, epsilon, lambda, bLearn);
		
		valueFunctionFileName = "./valueFunction_uCEQLambda_";
		
		/**
		 * read the value function
		 */
		//if( !doesLearn )
			//readValueFunction( valueFunctionFileName );
	}
	
	
	protected double[] computeCE( Game game, ArrayList<GHOST> gamingGhosts, 
			ArrayList<int[]> gamingJointStrategies )
	{
		//return computeCE_utilitarian(game);
		
		return computeCE_utilitarian_Cplex(game, gamingGhosts, gamingJointStrategies);
	}
	
	/**
	private double[] computeCE_utilitarian( Game game ) 
	{
		
		
		int numJntStrategies = Constants.NUM_JOINT_STRATEGIES;
		SizableProblemI problem = new Problem(300, numJntStrategies);
		problem.getMetadata().put("lp.isMaximize", "true");
		
		double[] ce = new double[numJntStrategies];
		for( int i = 0; i < numJntStrategies; i++ )
		    ce[i] = 0.0;
		
		
		try {
			
		    for( int jntStrategy = 0; jntStrategy < numJntStrategies; jntStrategy++ ) {
		    	
		    	double coeff = 0.0;
		    	for(  GHOST ghostType : GHOST.values() ) {
		    		
		    		coeff += getJntQValue(game, ghostType, jntStrategy);
		    	}
			
		    	problem.newVariable(""+jntStrategy).
		    		setObjectiveCoefficient( coeff );
		    }
		    
		    setConstraintCE(problem, game);

		    LinearProgrammingI iLP;
		    iLP = new DenseSimplex(problem);
		    double x = iLP.solve();
		    //System.out.println("XXX "+x);
		    VectorI v = iLP.getSolution();
		    ce = v.getArray();
		    
		    
		    return ce;
		}
		catch(Exception e) {
			
			//e.printStackTrace();
			return null;
		}
	}
	*/
	
	private double[] computeCE_utilitarian_Cplex( Game game, 
			ArrayList<GHOST> gamingGhosts, 
			ArrayList<int[]> gamingJointStrategies ) 
	{
			
		IloCplex ceLP = null;
		try {
			
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
		    	coeff[listIndex] = 0.0;
		    	
		    	for(  GHOST ghostType : GHOST.values() ) {
		    		
		    		if( game.doesGhostRequireAction(ghostType) )
		    			coeff[listIndex] += getJntQValue(game, ghostType, jntStrategy);
		    	}
		    	
		    	//System.out.print(" "+coeff[listIndex]);
		    }
		    //System.out.println();
		    ceLP.add(ceLP.maximize(ceLP.scalProd(coeff, p)));
		    
		    /**
		     * set up the linear programming 
		     * and solve it
		     */
		    double[] solution = null;
		    if( ceLP.solve() ) {
			
			
		    	solution = ceLP.getValues( p );
			
		    }
		    ceLP.end();
		    
		    //release the memory??
		    ceLP = null;
		    
		    return solution;
		}
		catch(Exception e) {
			
			//e.printStackTrace();
			ceLP.end();
			return null;
		}
	}
	
}
