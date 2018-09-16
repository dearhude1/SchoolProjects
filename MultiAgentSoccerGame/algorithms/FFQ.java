package algorithms;

import java.util.ArrayList;
import java.util.HashMap;


import soccerGame.GameAction;
import soccerGame.GameState;
import soccerGame.SoccerGame;
import soccerGame.TeamAction;
import drasys.or.matrix.VectorI;
import drasys.or.mp.Constraint;
import drasys.or.mp.Problem;
import drasys.or.mp.SizableProblemI;
import drasys.or.mp.lp.DenseSimplex;
import drasys.or.mp.lp.LinearProgrammingI;



public class FFQ extends MARL
{
    
    
    /**
     * policy for each agent of this team
     * joint poilcy for each state
     */
    protected HashMap<GameState, double[]> pi;
    
    /**
     * the value of each state 
     * for each agent in this team
     */
    protected HashMap<GameState, Double> V;
    
    double alphaDecay = 0.9999954;//0.99999;
    
    
    //check again
    public FFQ( int index )
    {
	super(index);
	
	
	/**
	 * get all states and all joint actions from LinearGridWorld class
	 */
	ArrayList<GameState> allStates = SoccerGame.allStates;
	
	/**
	 * init policy and tables
	 */
	pi = new HashMap<GameState, double[]>();
	V = new HashMap<GameState, Double>();
	
	for( int stateIndex = 0; stateIndex < allStates.size(); stateIndex++ ) {
	    
	    GameState gameState = allStates.get( stateIndex );
	    
	    /**
	     * for policy
	     */
	    ArrayList<TeamAction> availTeamActions = SoccerGame.generateAvailTeamActions(gameState, teamIndex);
	    int availNum = availTeamActions.size();
	    double[] policies = new double[availNum];
	    for( int teamActionIndex = 0; teamActionIndex < availTeamActions.size(); teamActionIndex++ ) {
		    
		    policies[teamActionIndex] = 1.0 / availNum;
	    } 
	    if( !pi.containsKey( gameState ) ) {
		
		pi.put( gameState, policies);
	    }
	    else {
		
		policies = null;
	    }
	    //release memory??
	    availTeamActions = null;
	    
	    /**
	     * for value
	     */

	    if( !V.containsKey( gameState ) ) {
		
		V.put( gameState, random.nextDouble() );
	    }
	}
	
	/**
	 * init alpha
	 */
	ALPHA = 1.0;
    }
    
    public FFQ( int index, double alpha, double gamma, double epsilon )
    {
	
	super(index, alpha, gamma, epsilon);
	
	/**
	 * get all states and all joint actions from LinearGridWorld class
	 */
	ArrayList<GameState> allStates = SoccerGame.allStates;
	
	/**
	 * init policy and tables
	 */
	pi = new HashMap<GameState, double[]>();
	V = new HashMap<GameState, Double>();
	
	for( int stateIndex = 0; stateIndex < allStates.size(); stateIndex++ ) {
	    
	    GameState gameState = allStates.get( stateIndex );
	    
	    /**
	     * for policy
	     */
	    ArrayList<TeamAction> availTeamActions = SoccerGame.generateAvailTeamActions(gameState, teamIndex);
	    int availNum = availTeamActions.size();
	    double[] policies = new double[availNum];
	    for( int teamActionIndex = 0; teamActionIndex < availTeamActions.size(); teamActionIndex++ ) {
		    
		    policies[teamActionIndex] = 1.0 / availNum;
	    } 
	    if( !pi.containsKey( gameState ) ) {
		
		pi.put( gameState, policies);
	    }
	    else {
		
		policies = null;
	    }
	    //release memory??
	    availTeamActions = null;
	    
	    /**
	     * for value
	     */
	    if( !V.containsKey( gameState ) ) {
		
		V.put( gameState, random.nextDouble() );
	    }
	}
	
	/**
	 * init alpha
	 */
	ALPHA = 1.0;
    }
    
    
    //check again and again
    public GameAction sampleAction( GameState gameState )
    {
	/**
	 * get an action according to pi
	 */
	if( gameState == null ) {
	    
	    System.out.println("@MiniMaxQ->sampleAction: NULL Parameter!");
	    return null;
	}
	
	GameAction retAction = new GameAction();
	
	double proSum = 0.0;
	double d = random.nextDouble();
	    
	ArrayList<TeamAction> teamActions = SoccerGame.generateAvailTeamActions(gameState, teamIndex);
	for( int teamActionIndex = 0; teamActionIndex < teamActions.size(); teamActionIndex++ ) {
	    
	    TeamAction teamAction = teamActions.get( teamActionIndex );
	    
	    proSum += getPi( gameState, teamAction );
		    
	    if( d < proSum ) {
	
		for( int agentNoInTeam = 0; agentNoInTeam < SoccerGame.NUM_TEAM_AGENTS; agentNoInTeam++ ) {
		  
		    int agentIndex = teamIndex * SoccerGame.NUM_TEAM_AGENTS + agentNoInTeam;
		    retAction.setAction( agentIndex, teamAction.getAction(agentNoInTeam) );
		}
		
		break;
	    }
	}
	
	//release memory?
	teamActions = null;
	
	return retAction;
    }
    
    //check again
    public GameAction updateQ( GameState curState, GameAction jointAction, 
	    double[] rewards, GameState nextState )
    {
	if( nextState == null ) {
	    
	    System.out.println("@FFQ->updateQ: NULL nextState!");
	    
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
		double Qsa = getQValue( curState, jointAction );
			
		//no need to query the state if the gameis over!!
		double Vsp = getV( nextState );
			
		//double alpha = getVariableAlpha(curState, jointAction);
		//Qsa = (1 - alpha) * Qsa + alpha * ( rewards[agentIndex] + GAMMA * Vsp );
			
		//reward for this team!!!!
		
		Qsa = (1 - ALPHA) * Qsa + ALPHA * ( rewards[teamIndex] + GAMMA * Vsp );
			
		setQValue( curState, jointAction, Qsa );
		
		
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
    
    
    //check again and again and again
    protected double updatePolicy( GameState gameState )
    {
	if( gameState == null ) {
	    
	    System.out.println("@FFQ->updatePolicy: NULL Parameter!");
	    return 0.0;
	}
	
	
	//find all available actions of the opponent's team
	int oppTeamIndex = (teamIndex+1) % SoccerGame.NUM_TEAMS;
	ArrayList<TeamAction> oppTeamActions = SoccerGame.generateAvailTeamActions(gameState, oppTeamIndex);
	ArrayList<TeamAction> teamActions = SoccerGame.generateAvailTeamActions(gameState, teamIndex);
	int oppTeamActNum = oppTeamActions.size();
	int teamActNum = teamActions.size();
	
	/**
	 * joint policy for each of the opponents' joint actions
	 * 
	 * 1-dim: the joint action of agents in the opponent team
	 * 2-dim: the joint action of agents in this team (must be joint action for linear program)
	 */
	double[][] jntPolicies = new double[oppTeamActNum][teamActNum];
	
	//a large number of time for linear programming???
	int minIndex = -1;
	double minValue = Double.POSITIVE_INFINITY;
	for( int oppTeamActIndex = 0; oppTeamActIndex < oppTeamActNum; oppTeamActIndex++ ) {
	    
	    TeamAction oppTeamAction = oppTeamActions.get( oppTeamActIndex );
	    
	    //real joint action
	    GameAction gameAction = new GameAction();
	    for( int oppNoInTeam = 0; oppNoInTeam < SoccerGame.NUM_TEAM_AGENTS; oppNoInTeam++ ) {
		
		int oppIndex = oppTeamIndex * SoccerGame.NUM_TEAM_AGENTS + oppNoInTeam;
		gameAction.setAction( oppIndex, oppTeamAction.getAction(oppNoInTeam) );
	    }
	    
	    try {
		
		/**
		 * use drays package
		 */
		SizableProblemI problem = new Problem( 300, teamActNum );
		problem.getMetadata().put("lp.isMaximize", "true");
		
		
		/**
		 * set variables
		 */
		double[] coeffs = new double[teamActNum];
		for( int teamActIndex = 0; teamActIndex < teamActNum; teamActIndex++ ) {
		
		    TeamAction teamAction = teamActions.get( teamActIndex );
		    for( int agentNoInTeam = 0; agentNoInTeam < SoccerGame.NUM_TEAM_AGENTS; agentNoInTeam++ ) {
			
			int agentIndex = teamIndex * SoccerGame.NUM_TEAM_AGENTS + agentNoInTeam;
			gameAction.setAction( agentIndex, teamAction.getAction(agentNoInTeam) );
		    }
			
		    String varName = "O"+oppTeamActIndex+"M"+teamActIndex;
		    coeffs[teamActIndex] = getQValue(gameState, gameAction);
		    
		    problem.newVariable(varName).setObjectiveCoefficient(coeffs[teamActIndex]);
		}
		
		
		/**
		 * all actions are available 
		 * so directly set the constraint
		 */
		//sum one contraint
		String sumConsStr = "SumOne";
		problem.newConstraint(sumConsStr).setType(Constraint.EQUAL).setRightHandSide(1.0);
		for( int teamActIndex = 0; teamActIndex < teamActNum; teamActIndex++ ) {
		    
		    String varName = "O"+oppTeamActIndex+"M"+teamActIndex;
		    problem.setCoefficientAt( sumConsStr, varName, 1.0 );
		}
		//bound constraint
		for( int teamActIndex = 0; teamActIndex < teamActNum; teamActIndex++ ) {
		    
		    String varName = "O"+oppTeamActIndex+"M"+teamActIndex;
		    
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
		jntPolicies[oppTeamActIndex] = v.getArray();
		
		/**
		 * compute the value
		 */
		double value = 0.0;
		for( int teamActIndex = 0; teamActIndex < teamActNum; teamActIndex++ ) 
		    value += jntPolicies[oppTeamActIndex][teamActIndex] * coeffs[teamActIndex];
		
		if( value < minValue ) {
		    
		    minIndex = oppTeamActIndex;
		    minValue = value;
		}
	    }
	    catch(Exception e) {
		
	    }
	}
	
	
	/**
	 * update the policy
	 */
	for( int teamActIndex = 0; teamActIndex < teamActNum; teamActIndex++ ) {
	    
	    TeamAction teamAction = teamActions.get( teamActIndex );
	    setPi( gameState, teamAction, jntPolicies[minIndex][teamActIndex] );
	}
	
	/**
	 * at last return the corresponding 
	 * value of the policy
	 */
	return minValue;
    }
    
    
    
    
    //check again
    protected double getPi( GameState gameState, TeamAction teamAction )
    {
	if( gameState == null ) {
	    
	    System.out.println("@FFQ->getPi: NULL Parameter!");
	    return 0.0;
	}
	else if( teamAction == null ) {
	    
	    System.out.println("@FFQ->getPi: NULL Team Action!");
	    return 0.0;
	}
	
	if( !pi.containsKey( gameState ) ) {
	    
	    System.out.println("getPi: No such game state in policy???");
	    return 0.0;
	}
	else {
	    
	    /**
	     * find the team action of gameAction
	     *
	    TeamAction teamAction = new TeamAction();
	    for( int agentNoInTeam = 0; agentNoInTeam < SoccerGame.NUM_TEAM_AGENTS; agentNoInTeam++ ) {
		
		int agentIndex = teamIndex * SoccerGame.NUM_TEAM_AGENTS + agentNoInTeam;
		teamAction.setAction(agentNoInTeam, gameAction.getAction(agentIndex));
	    }
	    */
	    
	    /**
	     * query the team action index
	     */
	    int teamActionIndex = SoccerGame.queryTeamActionIndex(gameState, teamIndex, teamAction);
	    if( teamActionIndex != -1 ) {
		
		double[] policies = pi.get( gameState );
		return policies[teamActionIndex];
	    }
	    else
		return 0.0;
	}
    }
    
    //check again and again
    protected void setPi( GameState gameState, TeamAction teamAction, double pro )
    {
	if( gameState == null ) {
	    
	    System.out.println("@FFQ->setPi: NULL Parameter!");
	    return;
	}
	else if( teamAction == null ) {
	    
	    System.out.println("@FFQ->setPi: NULL team Action!");
	    return;
	}
	else if( pro < 0 || pro > 1.0 ) {
	    
	    System.out.println("FFQ->setPi: Probability out of range!");
	    return;
	}
	
	if( !pi.containsKey( gameState ) ) {
	    
	    System.out.println("setPi: No such game state in policy???");
	    return;
	}
	else {
	    
	    /**
	     * find the team action of gameAction
	     *
	    TeamAction teamAction = new TeamAction();
	    for( int agentNoInTeam = 0; agentNoInTeam < SoccerGame.NUM_TEAM_AGENTS; agentNoInTeam++ ) {
		
		int agentIndex = teamIndex * SoccerGame.NUM_TEAM_AGENTS + agentNoInTeam;
		teamAction.setAction(agentNoInTeam, gameAction.getAction(agentIndex));
	    }
	    */
	    
	    /**
	     * query the team action index
	     */
	    int teamActionIndex = SoccerGame.queryTeamActionIndex(gameState, teamIndex, teamAction);
	    if( teamActionIndex != -1 ) {
		
		double[] policies = pi.get( gameState );
		policies[teamActionIndex] = pro;
		pi.put( gameState, policies );
	    }
	}
    }
    
    //check again
    protected double getV( GameState gameState )
    {
	if( gameState == null ) {
	    
	    System.out.println("@FFQ->getV: NULL Parameter!");
	    return 0.0;
	}
	
	//here already deals with game over state??
	if( !V.containsKey( gameState ) ) {
	    
	    System.out.println("FFQ->getV: no such game state in V??");
	    
	    /**
	    for( int agent = 0; agent < SoccerGame.NUM_TEAM_AGENTS * SoccerGame.NUM_TEAMS; agent++ ) {
		
		System.out.print(""+gameState.getLocationID(agent)+" ");
	    }
	    System.out.println();
	    */
	    
	    return 0.0;
	}
	else {
	    
	    return V.get( gameState );
	}
    }
    
    //check again
    protected void setV( GameState gameState, double value )
    {
	if( gameState == null ) {
	    
	    System.out.println("@MiniMaxQ->setV: NULL Parameter!");
	    return;
	}
	
	if( !V.containsKey( gameState ) ) {
	    
	    System.out.println("FFQ->getV: no such game state in V??");
	    return;
	}
	else {
	    
	    V.put( gameState, value );
	}
    }
    
}
