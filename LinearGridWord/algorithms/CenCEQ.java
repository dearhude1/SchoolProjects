package algorithms;

import java.util.ArrayList;
import linearGridWorld.GameAction;
import linearGridWorld.GameState;
import linearGridWorld.LinearGridWorld;

import drasys.or.mp.Constraint;
import drasys.or.mp.SizableProblemI;

/**
 * centralized CE-Q algorithm
 * @author dearhude1
 *
 */
public class CenCEQ extends MARL
{

    /**
    //The three members below are used for counting how many games are similar during learning
    //for counting the games in each state
    protected double[][] gameCounters;
    
    //for counting similar games in each state
    protected double[][] simiGameCounters;
    
    //the last Nash equilibrium in each state
    protected double[][][] lastEquilibrium;
    ////////////////////////////////////////////////////////////////////////////////////////////
    */
    
    public CenCEQ()
    {
	/**
	 * index is no use for centralized CE-Q
	 */
	super( 0 );
	
	/**
	 * init the Q-table
	 * for centralized algorithms 
	 * the Q-tables can be initialized randomly
	 *
	int locNum = LinearGridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;

	for( int agentIndex = 0; agentIndex < LinearGridWorld.NUM_AGENTS; agentIndex++ )
	    for( int s1 = 0; s1 < locNum; s1++ )
		for( int s2 = 0; s2 < locNum; s2++ )
		    for( int a1 = 0; a1 < actionNum; a1++ )
			for( int a2 = 0; a2 < actionNum; a2++ ) {
				    
			    Qs[agentIndex][s1][s2][a1][a2] = (Math.random() - 0.5) / 10.0;
			}
	
	//init the three testing members
	gameCounters = new double[locNum][locNum];
	simiGameCounters = new double[locNum][locNum];
	lastEquilibrium = new double[locNum][locNum][GameAction.NUM_ACTIONS * GameAction.NUM_ACTIONS];
	for( int s1 = 0; s1 < locNum; s1++ )
	    for( int s2 = 0; s2 < locNum; s2++ ) {
		
		gameCounters[s1][s2] = simiGameCounters[s1][s2] = 0.0;
		
		for( int jntAct = 0; jntAct < GameAction.NUM_ACTIONS * GameAction.NUM_ACTIONS; jntAct++ )
		    lastEquilibrium[s1][s2][jntAct] = 0.0;
	    }
	*/
    }
    
    
    public CenCEQ( double alpha, double gamma, double epsilon )
    {
	super( 0, alpha, gamma, epsilon);
	
	/**
	 * init the Q-table
	 * for centralized algorithms 
	 * the Q-tables can be initialized randomly
	 *
	int locNum = LinearGridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	
	for( int agentIndex = 0; agentIndex < LinearGridWorld.NUM_AGENTS; agentIndex++ )
	    for( int s1 = 0; s1 < locNum; s1++ )
		for( int s2 = 0; s2 < locNum; s2++ )
		    for( int a1 = 0; a1 < actionNum; a1++ )
			for( int a2 = 0; a2 < actionNum; a2++ ) {
				    
			    Qs[agentIndex][s1][s2][a1][a2] = (Math.random() - 0.5) / 10.0;
			}
	
	//init the three testing members
	gameCounters = new double[locNum][locNum];
	simiGameCounters = new double[locNum][locNum];
	lastEquilibrium = new double[locNum][locNum][GameAction.NUM_ACTIONS * GameAction.NUM_ACTIONS];
	for( int s1 = 0; s1 < locNum; s1++ )
	    for( int s2 = 0; s2 < locNum; s2++ ) {
		
		gameCounters[s1][s2] = simiGameCounters[s1][s2] = 0.0;
		
		for( int jntAct = 0; jntAct < GameAction.NUM_ACTIONS * GameAction.NUM_ACTIONS; jntAct++ )
		    lastEquilibrium[s1][s2][jntAct] = 0.0;
	    }
	*/
    }
    
    /**
     * Core method for MARL algorithms
     * update the Q-table and return the action for the next state
     * @param curState: the current state
     * @param jointAction: the joint action taken in the current state
     * @param rewards: the reward obtained from the current state to the next state
     * @param nextState: the next state
     * @return GameAction: the action expected to be chosen in the next state
     */
    public GameAction updateQ( GameState curState, GameAction jointAction, 
	    double[] rewards, GameState nextState )
    {
	
	if( nextState == null ) {
	    
	    System.out.println("@CenCEQ->updateQ: NULL nextState!");
	    
	    return null;
	}
	else {
	    
	    /**
	     * compute the correlated equilibrium in the next state
	     */
	    double[] correlEquil = computeCE( agentIndex, nextState );
	    
	    
	    /**
	    //the following code is for testing similar games during learning
	    //then compare the current equilibrium with the last one
	    int loc0 = nextState.getLocationID(0);
	    int loc1 = nextState.getLocationID(1);
	    if( correlEquil != null && gameCounters[loc0][loc1] > 0.01 ) {
		
		//distance of two Nash equilibrium
		double dis = 0.0;
		for( int jntAct = 0; jntAct < GameAction.NUM_ACTIONS * GameAction.NUM_ACTIONS; jntAct++ ) {
		    
		    double diff = lastEquilibrium[loc0][loc1][jntAct] - correlEquil[jntAct];
		    dis += diff * diff;
		    
		}
		dis = Math.sqrt(dis);
		
		double x = 0.01;//0.0000001;
		if( dis < x ) {   
		    simiGameCounters[loc0][loc1] += 1.0;
		}
	    }
	    gameCounters[loc0][loc1] += 1.0;
	    //store the current equilibrium
	    for( int jntAct = 0; jntAct < GameAction.NUM_ACTIONS * GameAction.NUM_ACTIONS; jntAct++ ) {
		    
		if( correlEquil != null )
		    lastEquilibrium[loc0][loc1][jntAct] = correlEquil[jntAct];
		else
		    lastEquilibrium[loc0][loc1][jntAct] = 0.0;
	    }
	    //////////////////////////////////////////////////////////////////////
	    */
	    
	    /**
	     * get a joint action according to the correlated equilibrium
	     */
	    GameAction nextAction = getJointAction_CE( correlEquil );
	    
	    
	    /**
	     * update the Q-tables
	     * but if this is the initial state of the game
	     * just return the action
	     */
	    if( curState != null && jointAction != null 
		&& rewards != null ) {
		
		/**
		 * mark a visit
		 */
		visit( curState, jointAction );
		
		/**
		 * compute the correspoding Q-values
		 */
		double[] correlValues = getCEQValues( nextState, correlEquil );
		    
		for( int agent = 0; agent < LinearGridWorld.NUM_AGENTS; agent++ ) {
		    
		    /**
		     * get the Q-value
		     */
		    double Qsa = getQValue( agent, curState, jointAction );
		    
		    /**
		     * updating rule
		     */
		    //Qsa = (1 - ALPHA) * Qsa + ALPHA * (rewards[agent] + GAMMA * correlValues[agent]);
		    
		    
		    /**
		     * variable learning rate
		     */
		    double alpha = getVariableAlpha( curState, jointAction );
		    Qsa = (1 - alpha) * Qsa + alpha * (rewards[agent] + GAMMA * correlValues[agent]);
		    
		    /**
		     * write back to the tables
		     */
		    setQValue( agent, curState, jointAction, Qsa );
		}
		
		/**
		 * maybe we can release some memories
		 */
		correlValues = null;
	    }
	    
	    /**
	     * maybe we can release some memories
	     */
	    correlEquil = null;
	    
	    return nextAction;
	}
    }
    
    
    /**
     * compute a correlated equilibrium
     * can be overrided by sub classes
     * @param agent
     * @param gameState
     * @return
     */
    protected double[] computeCE( int agent, GameState gameState )
    {
	
	return null;
    }
    
    
    
    /**
     * set the constraints according to the conditions of correlated equilibrium
     */
    //right
    protected void setConstraints_CE( SizableProblemI problem, GameState gameState ) throws Exception
    {
	int jointActionNum = LinearGridWorld.allJointActions.size();
	
	/**
	 * 2x4x3 = 24 constraints
	 * for the inequalities of correlated equilibrium
	 */
	for( int agent = 0; agent < LinearGridWorld.NUM_AGENTS; agent++ )
	{

	    /**
	     * loop for action ai
	     */
	    for( int ai = 0; ai < GameAction.NUM_ACTIONS; ai++ )
	    {
		/**
		 * loop for action ai'
		 */
		for( int aip = 0; aip < GameAction.NUM_ACTIONS; aip++ )
		{
		    if( aip == ai )
			continue;
		    
		    /**
		     * the name of the constraint:
		     * agentID(ai-ai')
		     */
		    String aiString = GameAction.getActionString(ai);
		    String aipString = GameAction.getActionString(aip);
		    String conString = agent+"("+aiString+"-"+aipString+")";
		    int consIndex = problem.newConstraint(conString).
			    setType(Constraint.GREATER).setRightHandSide(0.0).getRowIndex();
		    
		    /**
		     * set the coefficient
		     */
		    
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
			double coeff = Q_sa - Q_sap;
			
			int variableIndex = LinearGridWorld.queryJointActionIndex( jntAction_a );
			problem.setCoefficientAt( consIndex, variableIndex, coeff );
		    }
		}
	    }
	}
	
	/**
	 * 1 constraint
	 * the constraint that the sum of all 
	 * joint actions' probabilities is 1
	 */
	String eqCon = "equalConstraint";
	int index = problem.newConstraint(eqCon).
		setType(Constraint.EQUAL).setRightHandSide(1.0).getRowIndex();
	for( int i = 0; i < jointActionNum; i++ )
	{
	    problem.setCoefficientAt(index, i, 1.0);
	}
	
	/**
	 * 16 constraints
	 * the constraint of each joint action 
	 * that its probability is larger than 0
	 */
	for( int i = 0; i < jointActionNum; i++ )
	{
	    String zeroCon = "aboveZero" + (i+1);
	    index = problem.newConstraint(zeroCon).
	    	setType(Constraint.GREATER).setRightHandSide(0.0).getRowIndex();
	    problem.setCoefficientAt(index, i, 1.0);
	}
	
	
	/**
	 * maybe we lose one important condition
	 */
    }
    
    
    /**
     * get the joint action according to a correlated equilibrium
     */
    
    //right
    protected GameAction getJointAction_CE( double[] correlatedE )
    {	
	GameAction retAction = new GameAction();
	
	if( correlatedE == null )
	{
	    for( int agent = 0; agent < LinearGridWorld.NUM_AGENTS; agent++ ) {
		
		retAction.setAction( agent, random.nextInt(GameAction.NUM_ACTIONS) );
	    }
	}
	
	else {
	    
	    ArrayList<GameAction> allActions = LinearGridWorld.allJointActions;

	    double probability = 0.0;
	    double rndPro = random.nextDouble();
	    
	    for( int actionIndex = 0; actionIndex < allActions.size(); actionIndex++ ) {
		
		probability += correlatedE[actionIndex];
		
		if( rndPro >= probability ) {
		    
		    GameAction jntAction = allActions.get( actionIndex );
		    
		    for( int agent = 0; agent < LinearGridWorld.NUM_AGENTS; agent++ ) {
			
			retAction.setAction( agent, jntAction.getAction(agent) );
		    }
		    
		    break;
		}
	    }
	}
	
	return retAction;
    }
    
    /**
     * get the Q-values of a correlated equilibrium
     */
    //right
    protected double[] getCEQValues( GameState gameState, double[] correlatedE )
    {
	double[] values = new double[LinearGridWorld.NUM_AGENTS];
	for( int i = 0; i < LinearGridWorld.NUM_AGENTS; i++ )
	    values[i] = 0.0;
	
	/**
	 * if null equilibrium, return the value of random policy
	 * cannot return a value of 0
	 */
	if( correlatedE == null ) {
	    
	    int jntActionNum = LinearGridWorld.allJointActions.size();
	    correlatedE = new double[jntActionNum];
	    
	    for( int actionIndex = 0; actionIndex < jntActionNum; actionIndex++ )
		correlatedE[actionIndex] = 1.0 / ((double) jntActionNum );
	}
	
	ArrayList<GameAction> allActions = LinearGridWorld.allJointActions;
	for( int actionIndex = 0; actionIndex < allActions.size(); actionIndex++ ) {
	    
	    GameAction jntAction = allActions.get( actionIndex );
	    
	    for( int agent = 0; agent < LinearGridWorld.NUM_AGENTS; agent++ ) {
		
		double Q_sa = getQValue( agent, gameState, jntAction );
		
		values[agent] += correlatedE[actionIndex] * Q_sa;
		
	    }
	}
	
	return values;
    }
    
    
    
}
