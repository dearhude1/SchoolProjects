package algorithms;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import soccerGame.GameAction;
import soccerGame.GameState;
import soccerGame.SoccerGame;
import sun.management.resources.agent;

import drasys.or.matrix.VectorI;
import drasys.or.mp.Constraint;
import drasys.or.mp.Problem;
import drasys.or.mp.SizableProblemI;
import drasys.or.mp.lp.DenseSimplex;
import drasys.or.mp.lp.LinearProgrammingI;

import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;


public class MiniMaxQ extends MARL
{
    
    /**
     * the polic of minimaxQ
     * not joint action 
     * only the action of this agent
     */
    protected double[][][][] pi;
    
    /**
     * the value of each state
     */
    protected double[][][] V;
    
    double alphaDecay = 0.9999954;//0.99999;
    
    public MiniMaxQ( int index )
    {
	super(index);
	
	/**
	 * init policy and tables
	 */
	int locNum = SoccerGame.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	int agentNum = SoccerGame.NUM_AGENTS;
	
	pi = new double[locNum][locNum][agentNum][actionNum];
	V = new double[locNum][locNum][agentNum];
	for( int s1 = 0; s1 < locNum; s1++ )
	    for( int s2 = 0; s2 < locNum; s2++ ) 
	    	for( int ballPoss = 0; ballPoss < agentNum; ballPoss++ ) {
		
	    	    V[s1][s2][ballPoss] = random.nextDouble();
		
	    	    int loc = s1;
	    	    if( index == 1 )
	    		loc = s2;
		
	    	    /**/
	    	    boolean[] availActs = SoccerGame.actionSet( index, loc, ballPoss );
	    	    int availNum = 0;
	    	    for( int a = 0; a < GameAction.NUM_ACTIONS; a++ ) {
		    
	    		if( availActs[a] )
	    		    availNum += 1;
	    	    }
		
	    	    for( int a = 0; a < GameAction.NUM_ACTIONS; a++ ) {
			
	    		/**/
	    		if( availActs[a] ) 
	    		    pi[s1][s2][ballPoss][a] = 1.0 / availNum;
	    		else 
	    		    pi[s1][s2][ballPoss][a] = 0.0;
		    
	    		for( int o = 0; o < GameAction.NUM_ACTIONS; o++ ) {
			
	    		    Qs[agentIndex][s1][s2][ballPoss][a][o] = 
	    			    random.nextDouble();
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
	int locNum = SoccerGame.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	int agentNum = SoccerGame.NUM_AGENTS;
	
	pi = new double[locNum][locNum][agentNum][actionNum];
	V = new double[locNum][locNum][agentNum];
	for( int s1 = 0; s1 < locNum; s1++ )
	    for( int s2 = 0; s2 < locNum; s2++ ) 
	    	for( int ballPoss = 0; ballPoss < agentNum; ballPoss++ ) {
		
	    	    V[s1][s2][ballPoss] = random.nextDouble();
		
	    	    int loc = s1;
	    	    if( index == 1 )
	    		loc = s2;
		
	    	    /**/
	    	    boolean[] availActs = SoccerGame.actionSet( index, loc, ballPoss );
	    	    int availNum = 0;
	    	    for( int a = 0; a < GameAction.NUM_ACTIONS; a++ ) {
		    
	    		if( availActs[a] )
	    		    availNum += 1;
	    	    }
		
	    	    for( int a = 0; a < GameAction.NUM_ACTIONS; a++ ) {
			
	    		/**/
	    		if( availActs[a] ) 
	    		    pi[s1][s2][ballPoss][a] = 1.0 / availNum;
	    		else 
	    		    pi[s1][s2][ballPoss][a] = 0.0;
		    
	    		for( int o = 0; o < GameAction.NUM_ACTIONS; o++ ) {
			
	    		    Qs[agentIndex][s1][s2][ballPoss][a][o] = 
	    			    random.nextDouble();
	    		}
	    	    }
	    	}
	
	/**
	 * init alpha
	 */
	ALPHA = 1.0;
    }
    
    
    
    public GameAction sampleAction( GameState gameState)
    {
	/**
	 * get an action according to pi
	 */
	if( gameState == null ) {
	    
	    System.out.println("@MiniMaxQ->sampleAction: NULL Parameter!");
	    return null;
	}
	
	boolean[] actAvail = SoccerGame.actionSet( agentIndex, 
		gameState.getLocationID(agentIndex), gameState.getBallPossession() );
	
	double proSum = 0.0;
	double d = random.nextDouble();
	int actionIndex = 0;
	for( int act = 0; act < GameAction.NUM_ACTIONS; act++ )
	{
	    if( !actAvail[act] )
		continue;
	    
	    proSum += getPi(gameState, act);
	    
	    if( d < proSum ) {
		
		actionIndex = act;
		break;
	    }
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
	    GameAction nextAction = sampleAction( nextState );
	    
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
		
		//double alpha = getVariableAlpha(curState, jointAction);
		//Qsa = (1 - alpha) * Qsa + alpha * ( rewards[agentIndex] + GAMMA * Vsp );
		
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
		    jntAction.setAction( (agentIndex+1)%WallGame.NUM_AGENTS, o);
		    
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
	
	int oppIndex = (agentIndex+1) % SoccerGame.NUM_AGENTS;
	
	/**/
	boolean[] actAvail_opp =  SoccerGame.actionSet( oppIndex, 
		gameState.getLocationID(oppIndex), gameState.getBallPossession() );
	boolean[] actAvail = SoccerGame.actionSet( agentIndex, 
		gameState.getLocationID(agentIndex), gameState.getBallPossession() );
	
	
	for( int o = 0; o < GameAction.NUM_ACTIONS; o++ ) {
	    
	    /**/
	    if( !actAvail_opp[o] )
		continue;
	    
	    
	    try {
		
		/**
		 * use drays package
		 */
		SizableProblemI problem = new Problem(14, 5);
		problem.getMetadata().put("lp.isMaximize", "true");
		
		/**
		 * set variables
		 */
		double[] coeffs = new double[GameAction.NUM_ACTIONS];
		for( int a = 0; a < GameAction.NUM_ACTIONS; a++ ) {
		    
		    GameAction jntAction = new GameAction();
		    jntAction.setAction( agentIndex, a );
		    jntAction.setAction( (agentIndex+1)%SoccerGame.NUM_AGENTS, o);
		    
		    String varName = GameAction.getActionString(o)+":"+
			    GameAction.getActionString(a);
		    
		    coeffs[a] = getQValue( agentIndex, gameState, jntAction );
		    
		    problem.newVariable(varName).setObjectiveCoefficient(coeffs[a]);
		}
		
		/**
		 * set for unavailable actions
		 */
		for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
		    
		    String avaiConsStr = "Available"+act;
		    
		    if( !actAvail[act] ) {
			
			problem.newConstraint(avaiConsStr).setType(
				Constraint.EQUAL).setRightHandSide(0.0);
			
			for( int act_p = 0; act_p < GameAction.NUM_ACTIONS; act_p++ ) {
			    
			    String varName = GameAction.getActionString(o)+":"+
				    	GameAction.getActionString(act_p);
			    
			    if( act_p == act )
				problem.setCoefficientAt(avaiConsStr, varName, 1.0);
			    else
				problem.setCoefficientAt(avaiConsStr, varName, 0.0);
			}
		    }
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
	int ballPoss = gameState.getBallPossession();
	
	return pi[loc0][loc1][ballPoss][action];
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
	int ballPoss = gameState.getBallPossession();
	
	pi[loc0][loc1][ballPoss][action] = pro;
    }
    
    protected double getV( GameState gameState )
    {
	if( gameState == null ) {
	    
	    System.out.println("@MiniMaxQ->getV: NULL Parameter!");
	    return 0.0;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int ballPoss = gameState.getBallPossession();
	
	return V[loc0][loc1][ballPoss];
    }
    
    protected void setV( GameState gameState, double value )
    {
	if( gameState == null ) {
	    
	    System.out.println("@MiniMaxQ->setV: NULL Parameter!");
	    return;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int ballPoss = gameState.getBallPossession();
	
	V[loc0][loc1][ballPoss] = value; 
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
	    int locNum = SoccerGame.NUM_LOCATIONS;
	    int actionNum = GameAction.NUM_ACTIONS;
	    for( int s1 = 0; s1 < locNum; s1++ )
		for( int s2 = 0; s2 < locNum; s2++ )
		    for( int ballPoss = 0; ballPoss < SoccerGame.NUM_AGENTS; ballPoss++ )
			for( int a = 0; a < actionNum; a++ ) {
					    
			    writer.write(""+pi[s1][s2][ballPoss][a]);
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
	    int locNum = SoccerGame.NUM_LOCATIONS;
	    int actionNum = GameAction.NUM_ACTIONS;
	    int agentNum = SoccerGame.NUM_AGENTS;
	    
	    String line = "";
	    int s1 = 0;
	    int s2 = 0;
	    int ballPoss = 0;
	    int a = 0;
	    
	    while( (line = reader.readLine()) != null ) {
		
		if( line.length() == 0 )
		    continue;
		
		pi[s1][s2][ballPoss][a] = Double.parseDouble( line );
		
		a++;
		if( a >= actionNum ) {
		    
		    a = 0;
		    
		    ballPoss++;
		    if( ballPoss >= agentNum ) {
			s2++;
			if( s2 >= locNum ) {
				
			    s2 = 0;
			    s1++;
			}
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
