package algorithms;

import java.util.ArrayList;

import drasys.or.mp.Constraint;
import drasys.or.mp.SizableProblemI;
import gameGridWorld.GameAction;
import gameGridWorld.GameState;
import gameGridWorld.SparseGridWorld;

/**
 * decentralized CE-Q algorithm
 * @author dearhude1
 *
 */
public class DecenCEQ extends MARL
{

    
    public DecenCEQ( int index )
    {
	super( index );
    }
    
    public DecenCEQ( int index, double alpha, double gamma, double epsilon )
    {
	super(index, alpha, gamma, epsilon);
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
	    
	    System.out.println("@DecenCEQ->updateQ: NULL nextState!");
	    
	    return null;
	}
	else {
	    
	    /**
	     * compute the correlated equilibrium in the next state
	     */
	    double[] correlEquil = computeCE( agentIndex, nextState );
	    
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
		    
		for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		    
		    /**
		     * get the Q-value
		     */
		    double Qsa = getQValue( agent, curState, jointAction );
		    
		    /**
		     * updating rule
		     */
		    Qsa = (1 - ALPHA) * Qsa + ALPHA * (rewards[agent] + GAMMA * correlValues[agent]);
		    
		    /**
		     * variable learning rate
		     */
		    //double alpha = getVariableAlpha( curState, jointAction );
		    //Qsa = (1 - alpha) * Qsa + alpha * (rewards[agent] + GAMMA * correlValues[agent]);
		    
		    /**
		     * write back to the tables
		     */
		    setQValue( agent, curState, jointAction, Qsa );
		}
		
		/**
		 * for tranfer algorithm, we need decay
		 * for non-transfer algorithm, no need
		 */
		ALPHA *= 0.9991;//0.998;//0.998;//0.9991;//0.99958;//0.992;//0.9958;//0.99958
		
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
    protected void setConstraints_CE( SizableProblemI problem, 
	    GameState gameState ) throws Exception
    {
	
	int jointActionNum = SparseGridWorld.getAllJointActions().size();
	
	/**
	 * 2x4x3 = 24 constraints
	 * for the inequalities of correlated equilibrium
	 */
	for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ )
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
			
			int variableIndex = SparseGridWorld.queryJointActionIndex( jntAction_a );
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
    protected GameAction getJointAction_CE( double[] correlatedE )
    {	
	GameAction retAction = new GameAction();
	
	if( correlatedE == null )
	{
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		
		retAction.setAction( agent, random.nextInt(GameAction.NUM_ACTIONS) );
	    }
	}
	
	else {
	    
	    ArrayList<GameAction> allActions = SparseGridWorld.getAllJointActions();

	    double proSum = 0.0;
	    double samplePro = random.nextDouble();
	    
	    for( int actionIndex = 0; actionIndex < allActions.size(); actionIndex++ ) {
		
		proSum += correlatedE[actionIndex];
		
		if( samplePro <= proSum ) {
		    
		    GameAction jntAction = allActions.get( actionIndex );
		    
		    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
			
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
    protected double[] getCEQValues( GameState gameState, double[] correlatedE )
    {
	
	double[] values = new double[SparseGridWorld.NUM_AGENTS];
	for( int i = 0; i < SparseGridWorld.NUM_AGENTS; i++ )
	    values[i] = 0.0;
	
	/**
	 * if null equilibrium, return the value of random policy
	 * cannot return a value of 0
	 */
	if( correlatedE == null ) {
	    
	    int jntActionNum = SparseGridWorld.getAllJointActions().size();
	    correlatedE = new double[jntActionNum];
	    
	    for( int actionIndex = 0; actionIndex < jntActionNum; actionIndex++ )
		correlatedE[actionIndex] = 1.0 / ((double) jntActionNum );
	}
	
	ArrayList<GameAction> allActions = SparseGridWorld.getAllJointActions();
	for( int actionIndex = 0; actionIndex < allActions.size(); actionIndex++ ) {
	    
	    GameAction jntAction = allActions.get( actionIndex );
	    
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		
		double Q_sa = getQValue( agent, gameState, jntAction );
		
		values[agent] += correlatedE[actionIndex] * Q_sa;
		
	    }
	}
	
	return values;
    }
    
    
}
