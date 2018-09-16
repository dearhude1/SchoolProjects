package algorithms;


import soccerGame.GameAction;
import soccerGame.GameState;
import soccerGame.SoccerGame;

import drasys.or.matrix.VectorI;
import drasys.or.mp.Constraint;
import drasys.or.mp.Problem;
import drasys.or.mp.SizableProblemI;
import drasys.or.mp.lp.DenseSimplex;
import drasys.or.mp.lp.LinearProgrammingI;



public class HAMMQ extends MiniMaxQ
{
    
    /**
     * heuristic policy
     * not joint action 
     * only the action of this agent
     */
    protected int[] pi_H;
    
    
    /**
     * H-table of the corresponding agent
     * 3 dimensions for state (joing location and ball possession)
     * 2 dimensions for joint action
     */
    protected double[][][][][] H;
    
    
    private double ETA = 1.0;//1.0;
    private double PSI = 1.0;//1.0;
    
    public HAMMQ( int index )
    {
	super(index);
	
	/**
	 * init policy and tables
	 */
	int locNum = SoccerGame.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	int agentNum = SoccerGame.NUM_AGENTS;
	
	H = new double[locNum][locNum][agentNum][actionNum][actionNum];
	
	/**
	 * the suggested action in each location when holding the ball, 
	 * independent of the opponent's location
	 */
	pi_H = new int[locNum];
	
	for( int s1 = 0; s1 < locNum; s1++ )
	    for( int s2 = 0; s2 < locNum; s2++ ) 
	    	for( int ballPoss = 0; ballPoss < agentNum; ballPoss++ ) {
		
	    	    for( int act = 0; act < actionNum; act++ ) {
	    		
	    		for( int o_act = 0; o_act < actionNum; o_act++ ) {
	    		    
	    		    H[s1][s2][ballPoss][act][o_act] = 0.0;
	    		}
	    	    }
	    	    
	    	}
	
	initHeuristicPolicy();
	
    }
    
    private void initHeuristicPolicy()
    {
	int locNum = SoccerGame.NUM_LOCATIONS;
	
	for( int s1 = 0; s1 < locNum; s1++ ) {
		
	    pi_H[s1] = GameAction.RIGHT;
	}

	pi_H[SoccerGame.NUM_LOCATIONS-1]= GameAction.DOWN;
	pi_H[4] = GameAction.UP;
    }
    
    public HAMMQ( int index, double alpha, double gamma, double epsilon )
    {
	
	super(index, alpha, gamma, epsilon);
	
	/**
	 * init policy and tables
	 */
	int locNum = SoccerGame.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	int agentNum = SoccerGame.NUM_AGENTS;
	
	H = new double[locNum][locNum][agentNum][actionNum][actionNum];
	
	/**
	 * the suggested action in each location when holding the ball, 
	 * independent of the opponent's location
	 */
	pi_H = new int[locNum];
	
	for( int s1 = 0; s1 < locNum; s1++ )
	    for( int s2 = 0; s2 < locNum; s2++ ) 
	    	for( int ballPoss = 0; ballPoss < agentNum; ballPoss++ ) {
		
	    	    for( int act = 0; act < actionNum; act++ ) {
	    		
	    		for( int o_act = 0; o_act < actionNum; o_act++ ) {
	    		    
	    		    H[s1][s2][ballPoss][act][o_act] = 0.0;
	    		}
	    	    }
	    	    
	    	}
	
	initHeuristicPolicy();
    }
    
    
    
    
    public GameAction updateQ( GameState curState, GameAction jointAction, 
	    double[] rewards, GameState nextState )
    {
	if( nextState == null ) {
	    
	    System.out.println("@HAMMQ->updateQ: NULL nextState!");
	    
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
		
		
		/**
		 * update the value H(s,a,o)
		 */
		int loc0 = curState.getLocationID(0);
		int loc1 = curState.getLocationID(1);
		int ballPoss = curState.getBallPossession();
		int act = jointAction.getAction( agentIndex );
		double H_sao = getHeuristicValue( curState, jointAction );
		
		if( ballPoss == agentIndex && 
			act == pi_H[loc0] ) {
		    
		    double maxQvalue = getMaxQValue( curState, 
			    jointAction.getAction((agentIndex+1)%SoccerGame.NUM_AGENTS));
		    
		    H_sao = maxQvalue - Qsa + ETA;
		}
		else 
		    H_sao = 0.0;
		
		setHeuristicValue( curState, jointAction, H_sao );
		
		/**
		 * update Q-values
		 */
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
		    
		    coeffs[a] = getQValue( agentIndex, gameState, jntAction ) + 
			    PSI * getHeuristicValue( gameState, jntAction );
		    
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
    
    
    private double getHeuristicValue( GameState gameState, GameAction jntAction )
    {
	if( gameState == null || jntAction == null ) {
	    
	    System.out.println("@HAMMQ->getHeuristicValue: NULL Parameters!");
	    return 0.0;
	}
	
	
	int loc0 = gameState.getLocationID( 0 );
	int loc1 = gameState.getLocationID( 1 );
	int ballPoss = gameState.getBallPossession();
	int act = jntAction.getAction( agentIndex );
	int o_act = jntAction.getAction( (agentIndex+1) % SoccerGame.NUM_AGENTS );
	
	return H[loc0][loc1][ballPoss][act][o_act];
    }
    
    private void setHeuristicValue( GameState gameState, GameAction jntAction, double value )
    {
	if( gameState == null || jntAction == null ) {
	    
	    System.out.println("@HAMMQ->setHeuristicValue: NULL Parameters!");
	    return;
	}
	
	
	int loc0 = gameState.getLocationID( 0 );
	int loc1 = gameState.getLocationID( 1 );
	int ballPoss = gameState.getBallPossession();
	int act = jntAction.getAction( agentIndex );
	int o_act = jntAction.getAction( (agentIndex+1) % SoccerGame.NUM_AGENTS );
	
	H[loc0][loc1][ballPoss][act][o_act] = value;
    }
    
    private double getMaxQValue( GameState gameState, int act_o )
    {
	
	if( gameState == null || act_o < 0 ||
		act_o >= GameAction.NUM_ACTIONS ) {
	    
	    System.out.println("@HAMMQ->getMaxQValue: Wrong Parameters!");
	    return 0.0;
	}
	
	int oppIndex = (agentIndex+1) % SoccerGame.NUM_AGENTS;
	
	GameAction jntAction = new GameAction();
	jntAction.setAction( oppIndex, act_o );
	
	double maxQ = Double.NEGATIVE_INFINITY;
	for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
	    
	    jntAction.setAction( agentIndex, act );
	    double Q = getQValue( agentIndex, gameState, jntAction );
	    
	    if( maxQ < Q )
		maxQ = Q;
	}
	
	return maxQ;
    }
    
}
