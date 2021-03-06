package algorithms;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import drasys.or.matrix.VectorI;
import drasys.or.mp.Constraint;
import drasys.or.mp.Problem;
import drasys.or.mp.SizableProblemI;
import drasys.or.mp.lp.DenseSimplex;
import drasys.or.mp.lp.LinearProgrammingI;

import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import gamePursuitGame.GameAction;
import gamePursuitGame.GameState;
import gamePursuitGame.PursuitGame;


public class MiniMaxQ extends MARL
{
    
    /**
     * the polic of minimaxQ
     * not joint action 
     * only the action of this agent
     */
    protected double[][][] pi;
    
    /**
     * the value of each state
     */
    protected double[][] V;
    
    double alphaDecay = 0.99999;
    
    public MiniMaxQ( int index )
    {
	super(index);
	
	/**
	 * init policy and tables
	 */
	int locNum = PursuitGame.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	
	pi = new double[locNum][locNum][actionNum];
	V = new double[locNum][locNum];
	for( int s1 = 0; s1 < locNum; s1++ )
	    for( int s2 = 0; s2 < locNum; s2++ ) {
		
		V[s1][s2] = 1.0;//(Math.random() - 0.5) / 10.0; //1.0;
		
		for( int a = 0; a < GameAction.NUM_ACTIONS; a++ ) {
			
		    pi[s1][s2][a] = 1.0 / GameAction.NUM_ACTIONS;
		    
		    for( int o = 0; o < GameAction.NUM_ACTIONS; o++ ) {
			
			Qs[agentIndex][s1][s2][a][o] = 1.0;//(Math.random() - 0.5) / 10.0; //1.0; 
		    }
		}
	    }
	
	/**
	 * init alpha
	 */
	ALPHA = 1.0;
    }
    
    public MiniMaxQ( int index, double alpha, double gamma, double epsilon )
    {
	
	super(index, alpha, gamma, epsilon);
	
	/**
	 * init policy and tables
	 */
	int locNum = PursuitGame.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	
	pi = new double[locNum][locNum][actionNum];
	V = new double[locNum][locNum];
	for( int s1 = 0; s1 < locNum; s1++ )
	    for( int s2 = 0; s2 < locNum; s2++ ) {
		
		V[s1][s2] = (Math.random() - 0.5) / 10.0; //1.0;
		
		for( int a = 0; a < GameAction.NUM_ACTIONS; a++ ) {
			
		    pi[s1][s2][a] = 1.0 / GameAction.NUM_ACTIONS;
		    
		    for( int o = 0; o < GameAction.NUM_ACTIONS; o++ ) {
			
			Qs[agentIndex][s1][s2][a][o] = (Math.random() - 0.5) / 10.0; //1.0; 
		    }
		}
	    }
	
	/**
	 * init alpha
	 */
	ALPHA = 1.0;
    }
    
    
    
    public GameAction getAction( GameState gameState )
    {
	/**
	 * get an action according to pi
	 */
	if( gameState == null ) {
	    
	    System.out.println("@MiniMaxQ->getAction: NULL Parameter!");
	    return null;
	}
	
	double[] probabilities = new double[GameAction.NUM_ACTIONS];
		
	
	probabilities[0] = getPi(gameState, 0);
	for( int act = 1; act < GameAction.NUM_ACTIONS; act++ )
	{
	    probabilities[act] =  probabilities[act-1] + getPi(gameState, act);
	}
		
	double d = random.nextDouble();
	int actionIndex = 0;
	for( int act = 0; act < GameAction.NUM_ACTIONS; act++ )
	{
	    if( d < probabilities[act] )
	    {
		actionIndex = act;
		break;
	    }
	}
	
	if( random.nextDouble() < 0.001 ) {
	    
	    GameAction randAction = new GameAction();
	    randAction.setAction(0, random.nextInt(GameAction.NUM_ACTIONS));
	    randAction.setAction(1, random.nextInt(GameAction.NUM_ACTIONS));
	    return randAction;
	}
	
	return new GameAction(new int[]{actionIndex,actionIndex});
	
    }
    
    
    public GameAction updateQ( GameState curState, GameAction jointAction, 
	    double[] rewards, GameState nextState )
    {
	if( nextState == null ) {
	    
	    System.out.println("@MiniMaxQ->updateQ: NULL nextState!");
	    
	    return null;
	}
	else {
	    
	    
	    /**
	     * select action in the next state according to pi(s)
	     */
	    GameAction nextAction = getAction( nextState );
	    
	    /**
	     * update the Q-tables
	     * but if this is the initial state of the game
	     * just return the action
	     */
	    if( curState != null && jointAction != null 
		&& rewards != null )  {
		
		
		/**
		 * mark a visit
		 */
		visit( curState, jointAction );
		
		
		/**
		 * learning rule:
		 * Q(s,a) <- (1-alpha)Q(s,a) + alpha * (reward + gamma * V(s'))
		 */
		double Qsa = getQValue( agentIndex, curState, jointAction );
		double Vsp = getV( nextState );
		Qsa = (1 - ALPHA) * Qsa + ALPHA * ( rewards[agentIndex] + GAMMA * Vsp );
		setQValue( agentIndex, curState, jointAction, Qsa );
		
		
		/**
		 * linear programming to update the policy in curState
		 */
		double minimaxV = updatePolicy( curState );
		setV( curState,  minimaxV );
		
		ALPHA *= alphaDecay;
	    }
	    
	    return nextAction;
	}
    }
    
    
    /**
    private double updatePolicy( GameState gameState )
    {
	
	if( gameState == null ) {
	    
	    System.out.println("@MiniMaxQ->updatePolicy: NULL Parameter!");
	    return 0.0;
	}
	
	

	double[][] policies = new double[GameAction.NUM_ACTIONS][GameAction.NUM_ACTIONS];
	

	int maxIndex = -1;
	double maxValue = -Double.MAX_VALUE;
	for( int o = 0; o < GameAction.NUM_ACTIONS; o++ ) {
	    
	    try {
		
		IloCplex minimaxLCP = new IloCplex();
		
		double[] lowerBounds = new double[GameAction.NUM_ACTIONS];
		double[] uppBounds = new double[GameAction.NUM_ACTIONS];
		for( int varIndex = 0; varIndex < GameAction.NUM_ACTIONS; varIndex++ ) {
			
		    lowerBounds[varIndex] = 0.0;
		    uppBounds[varIndex] = 1.0;
		}
		
		IloNumVar[] var = minimaxLCP.numVarArray( GameAction.NUM_ACTIONS, lowerBounds, uppBounds);
		
		IloNumExpr sumExpr = minimaxLCP.scalProd(new double[]{ 1.0,1.0,1.0,1.0 }, var);
		minimaxLCP.addEq( sumExpr, 1.0 );
		
		double[] coeffs = new double[GameAction.NUM_ACTIONS];
		for( int a = 0; a < GameAction.NUM_ACTIONS; a++ ) {
		    
		    GameAction jntAction = new GameAction();
		    jntAction.setAction( agentIndex, a );
		    jntAction.setAction( (agentIndex+1)%PursuitGame.NUM_AGENTS, o);
		    
		    coeffs[a] = getQValue( agentIndex, gameState, jntAction );
		}
		minimaxLCP.addMinimize( minimaxLCP.scalProd(coeffs, var) );
		
		
		if( minimaxLCP.solve() ) {
		    
		    policies[o] = minimaxLCP.getValues( var ); 
		    
		    double value = minimaxLCP.getValue( minimaxLCP.scalProd(coeffs, var) );
		    if( value > maxValue ) {
			
			maxValue = value;
			maxIndex = o;
		    }
		}
		else{
		    //no solution
		}
		minimaxLCP.end();
	    }
	    catch (Exception e) { e.printStackTrace(); }
	    
	}
	
	
	for( int a = 0; a < GameAction.NUM_ACTIONS; a++ ) {
	    
	    setPi( gameState, a, policies[maxIndex][a] );
	}
	
	return maxValue;
    }
    */
    
    protected double updatePolicy( GameState gameState )
    {
	if( gameState == null ) {
	    
	    System.out.println("@MiniMaxQ->updatePolicy: NULL Parameter!");
	    return 0.0;
	}
	
	
	/**
	 * pair (o,a)
	 * which stands for that there is a policy for each of the opponent's actions
	 */
	double[][] policies = new double[GameAction.NUM_ACTIONS][GameAction.NUM_ACTIONS];
	
	/**
	 * for each of the opponent's action 'o', find a policy 
	 * which minimize \Sum_{a} pi(s,a)*Q(s,a,o)
	 */
	//may be I implement the wrong version: max_o min_pi \Sum_{a} pi(s,a)*Q(s,a,o)
	//maximize the min_o, so we should maximize for all o
	int minIndex = -1;
	double minValue = Double.POSITIVE_INFINITY;
	for( int o = 0; o < GameAction.NUM_ACTIONS; o++ ) {
	    
	    try {
		
		/**
		 * use drays package
		 */
		SizableProblemI problem = new Problem(9, 4);
		problem.getMetadata().put("lp.isMaximize", "true");
		
		/**
		 * set variables
		 */
		double[] coeffs = new double[GameAction.NUM_ACTIONS];
		for( int a = 0; a < GameAction.NUM_ACTIONS; a++ ) {
		    
		    GameAction jntAction = new GameAction();
		    jntAction.setAction( agentIndex, a );
		    jntAction.setAction( (agentIndex+1)%PursuitGame.NUM_AGENTS, o);
		    
		    String varName = GameAction.getActionString(o)+":"+
			    GameAction.getActionString(a);
		    
		    coeffs[a] = getQValue( agentIndex, gameState, jntAction );
		    
		    problem.newVariable(varName).setObjectiveCoefficient(coeffs[a]);
		}
		
		
		/**
		 * set constraints
		 */
		String sumConsStr = "SumOne";
		problem.newConstraint(sumConsStr).setType(Constraint.EQUAL).setRightHandSide(1.0);
		for( int a = 0; a < GameAction.NUM_ACTIONS; a++ ) {
		    
		    String varName = GameAction.getActionString(o)+":"+
			    GameAction.getActionString(a);
		    problem.setCoefficientAt( sumConsStr, varName, 1.0 );
		}
		
		for( int a = 0; a < GameAction.NUM_ACTIONS; a++ ) {
		    
		    String varName = GameAction.getActionString(o)+":"+
			    GameAction.getActionString(a);
		    
		    String geZeroCons = "GeZero-"+varName;
		    String leOneCons = "LeOne-"+varName;
		    
		    problem.newConstraint(geZeroCons).setType(Constraint.GREATER).setRightHandSide(0.0);
		    problem.setCoefficientAt( geZeroCons, varName, 1.0);
		    
		    problem.newConstraint(leOneCons).setType(Constraint.LESS).setRightHandSide(1.0);
		    problem.setCoefficientAt( leOneCons, varName, 1.0 );
		}
		
		
		/**
		 * solve the problem
		 */
		LinearProgrammingI iLP;
		iLP = new DenseSimplex(problem);
		iLP.solve();
		VectorI v = iLP.getSolution();
		policies[o] = v.getArray();
		
		/**
		 * compute the value
		 */
		double value = 0.0;
		for( int a = 0; a < GameAction.NUM_ACTIONS; a++ ) 
		    value += policies[o][a] * coeffs[a];
		
		if( value < minValue ) {
		    
		    minIndex = o;
		    minValue = value;
		}
		
	    }
	    catch (Exception e) { e.printStackTrace(); }
	    
	}
	
	
	/**
	 * update the policy
	 */
	for( int a = 0; a < GameAction.NUM_ACTIONS; a++ ) {
	    
	    setPi( gameState, a, policies[minIndex][a] );
	}
	
	/**
	 * at last return the corresponding 
	 * value of the policy
	 */
	return minValue;
    }
    
    protected double getPi( GameState gameState, int action )
    {
	if( gameState == null ) {
	    
	    System.out.println("@MiniMaxQ->getPi: NULL Parameter!");
	    return 0.0;
	}
	else if( action < 0 || action >= GameAction.NUM_ACTIONS ) {
	    
	    System.out.println("@MiniMaxQ->getPi: Unavailable Action!");
	    return 0.0;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	
	return pi[loc0][loc1][action];
    }
    
    protected void setPi( GameState gameState, int action, double pro )
    {
	if( gameState == null ) {
	    
	    System.out.println("@MiniMaxQ->setPi: NULL Parameter!");
	    return;
	}
	else if( action < 0 || action >= GameAction.NUM_ACTIONS ) {
	    
	    System.out.println("@MiniMaxQ->setPi: Unavailable Action!");
	    return;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	
	pi[loc0][loc1][action] = pro;
    }
    
    protected double getV( GameState gameState )
    {
	if( gameState == null ) {
	    
	    System.out.println("@MiniMaxQ->getV: NULL Parameter!");
	    return 0.0;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	
	return V[loc0][loc1];
    }
    
    protected void setV( GameState gameState, double value )
    {
	if( gameState == null ) {
	    
	    System.out.println("@MiniMaxQ->setV: NULL Parameter!");
	    return;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	
	V[loc0][loc1] = value; 
    }
    
    public void storePolicy()
    {
	
	try {
	    
	    /**
	     * write all values in member Qs
	     * 
	     * only the table of this agent
	     */
	    String fileName = "./minimaxQPolicy_agent"+agentIndex+".txt";
	    BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
	    int locNum = PursuitGame.NUM_LOCATIONS;
	    int actionNum = GameAction.NUM_ACTIONS;
	    for( int s1 = 0; s1 < locNum; s1++ )
		for( int s2 = 0; s2 < locNum; s2++ )
		    for( int a = 0; a < actionNum; a++ ) {
					    
			    writer.write(""+pi[s1][s2][a]);
			    writer.newLine();
		    }
	    writer.close();
	}
	catch (Exception e) {
	    e.printStackTrace();
	}
    }
    
    public void readPolicy()
    {
	
	try {
	    
	    String fileName = "./minimaxQPolicy_agent"+agentIndex+".txt";
	    BufferedReader reader = new BufferedReader(new FileReader(fileName));
	    int locNum = PursuitGame.NUM_LOCATIONS;
	    int actionNum = GameAction.NUM_ACTIONS;
	    
	    String line = "";
	    int s1 = 0;
	    int s2 = 0;
	    int a = 0;
	    
	    while( (line = reader.readLine()) != null ) {
		
		if( line.length() == 0 )
		    continue;
		
		pi[s1][s2][a] = Double.parseDouble( line );
		
		a++;
		if( a >= actionNum ) {
		    
		    a = 0;
		    s2++;
		    if( s2 >= locNum ) {
			
			s2 = 0;
			s1++;
		    }
		}
	    }
	    reader.close();
	}
	catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
