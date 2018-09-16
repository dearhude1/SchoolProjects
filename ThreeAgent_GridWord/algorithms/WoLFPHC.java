package algorithms;

import gameGridWorld.GameAction;
import gameGridWorld.GameState;
import gameGridWorld.GridWorld;

/**
 * the algorithm of WoLF-PHC
 * for details please read the 
 * paper "Multiagent Learning Using Variable Learning Rate"
 *
 */
public class WoLFPHC extends MARL
{

    /**
     * the learning rates of policy
     */
    double DELTA_LOSE = 0.1;
    double DELTA_WIN = 0.05;
    
    /**
     * WoLF-PHC only maintain the 
     * Q-table of its own action
     * not the joint action
     */
    protected double[][][][] Q;
    
    /**
     * the polic of WoLFPHC
     * not joint action 
     * only the action of this agent
     */
    protected double[][][][] pi;
    
    /**
     * average policy
     * for determining win or lose
     */
    protected double[][][][] pi_bar;
    
    /**
     * record the number of visits of each state
     */
    protected int[][][] stateCounts;
    
    public WoLFPHC( int index )
    {
	super(index);

	
	/**
	 * init the policy
	 */
	int locNum = GridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	
	Q = new double[locNum][locNum][locNum][actionNum];
	pi = new double[locNum][locNum][locNum][actionNum];
	pi_bar = new double[locNum][locNum][locNum][actionNum];
	stateCounts = new int[locNum][locNum][locNum];
	for( int s1 = 0; s1 < locNum; s1++ )
	    for( int s2 = 0; s2 < locNum; s2++ )
		for( int s3 = 0; s3 < locNum; s3++ ) {
		    
		    stateCounts[s1][s2][s3] = 0;
		    for( int a = 0; a < GameAction.NUM_ACTIONS; a++ ) {
			
			Q[s1][s2][s3][a] = 0.0;
			pi[s1][s2][s3][a] = 1.0 / GameAction.NUM_ACTIONS;
			pi_bar[s1][s2][s3][a] = 1.0 / GameAction.NUM_ACTIONS;
		    }
		}
    }
    
    public WoLFPHC( int index, double deltaL, double deltaW )
    {
	super(index);

	
	DELTA_LOSE = deltaL;
	DELTA_WIN = deltaW;
	
	/**
	 * init the policy
	 */
	int locNum = GridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	
	Q = new double[locNum][locNum][locNum][actionNum];
	pi = new double[locNum][locNum][locNum][actionNum];
	stateCounts = new int[locNum][locNum][locNum];
	pi_bar = new double[locNum][locNum][locNum][actionNum];
	for( int s1 = 0; s1 < locNum; s1++ )
	    for( int s2 = 0; s2 < locNum; s2++ )
		for( int s3 = 0; s3 < locNum; s3++ ) {
		    
		    stateCounts[s1][s2][s3] = 0;
		    for( int a = 0; a < GameAction.NUM_ACTIONS; a++ ) {
			
			Q[s1][s2][s3][a] = 0.0;
			pi[s1][s2][s3][a] = 1.0 / GameAction.NUM_ACTIONS;
			pi_bar[s1][s2][s3][a] = 1.0 / GameAction.NUM_ACTIONS;
		    }
		}
    }
    
    public WoLFPHC( int index, double alpha, double gamma, double epsilon )
    {
	super(index, alpha, gamma, epsilon);
	
	/**
	 * init the poilcy
	 */
	int locNum = GridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	
	Q = new double[locNum][locNum][locNum][actionNum];
	pi = new double[locNum][locNum][locNum][actionNum];
	pi_bar = new double[locNum][locNum][locNum][actionNum];
	stateCounts = new int[locNum][locNum][locNum];
	for( int s1 = 0; s1 < locNum; s1++ )
	    for( int s2 = 0; s2 < locNum; s2++ )
		for( int s3 = 0; s3 < locNum; s3++ ) {
		    
		    stateCounts[s1][s2][s3] = 0;
		    for( int a = 0; a < GameAction.NUM_ACTIONS; a++ ) {
			
			Q[s1][s2][s3][a] = 0.0;
			pi[s1][s2][s3][a] = 1.0 / GameAction.NUM_ACTIONS;
			pi_bar[s1][s2][s3][a] = 1.0 / GameAction.NUM_ACTIONS;
		    }
		}
    }
    
    public WoLFPHC( int index, double alpha, double gamma, double epsilon, 
	    double deltaL, double deltaW )
    {
	super(index, alpha, gamma, epsilon);
	
	DELTA_LOSE = deltaL;
	DELTA_WIN = deltaW;
	
	/**
	 * init the poilcy
	 */
	int locNum = GridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	
	Q = new double[locNum][locNum][locNum][actionNum];
	pi = new double[locNum][locNum][locNum][actionNum];
	pi_bar = new double[locNum][locNum][locNum][actionNum];
	stateCounts = new int[locNum][locNum][locNum];
	for( int s1 = 0; s1 < locNum; s1++ )
	    for( int s2 = 0; s2 < locNum; s2++ )
		for( int s3 = 0; s3 < locNum; s3++ ) {
		    
		    stateCounts[s1][s2][s3] = 0;
		    for( int a = 0; a < GameAction.NUM_ACTIONS; a++ ) {
			
			Q[s1][s2][s3][a] = 0.0;
			pi[s1][s2][s3][a] = 1.0 / GameAction.NUM_ACTIONS;
			pi_bar[s1][s2][s3][a] = 1.0 / GameAction.NUM_ACTIONS;
		    }
		}
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
	    
	    System.out.println("@WoLFPHC->updateQ: NULL nextState!");
	    
	    return null;
	}
	else {
	    
	    /**
	     * select action in the next state according to pi(s)
	     */
	    GameAction nextAction = getAction_Pi( nextState );
	    
	    
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
		 * get the value maxQ(s',a')
		 * and update Q-value
		 */
		double maxQValue = getMaxQValue(nextState);
		
		double Qsa = getQValue( curState, jointAction.getAction( agentIndex ) );
		
		
		boolean policyInc = true;
		if( Qsa - getMaxQValue(curState) < 0 )
		    policyInc = false;
		
		
		/**
		 * updating rule
		 */
		Qsa = (1 - ALPHA) * Qsa + ALPHA * ( rewards[agentIndex] + GAMMA * maxQValue );
		/**
		 * variable learning rate
		 */
		//alpha = getVariableAlpha( curState, jointAction );
		//Qsa = (1 - alpha) * Qsa + alpha * (rewards[agent] + GAMMA * correlValues[agent]);
		
		setQValue( curState, jointAction.getAction( agentIndex ), Qsa );
		
		/**
		 * update average policy
		 * 
		 * C(s) = C(s) + 1 has been done in function visit
		 */
		for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
		    
		    double pi_sa = getPi( curState, act );
		    double pi_bar_sa = getPiBar( curState, act );
		    double Cs = (double) getStateCount( curState );
		    
		    pi_bar_sa += ( pi_sa - pi_bar_sa ) / Cs;
		    setPiBar( curState, act, pi_bar_sa );
		}
		
		
		/**
		 * then step pi closer to the optimal policy
		 * remember also update policy of other actions
		 */
		double Delta = 0.0;
		if( !policyInc ) {
		    
		    Delta -= delta_sa( curState, jointAction.getAction( agentIndex ) );
		}
		else {
		    
		    for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
			
			if( act == jointAction.getAction( agentIndex ) )
			    continue;
			
			Delta += delta_sa( curState, act );
		    }
		}
		double pi_sa = getPi( curState, jointAction.getAction( agentIndex ) );
		pi_sa += Delta;
		setPi( curState, jointAction.getAction( agentIndex ), pi_sa );
		
		//update policy for other actions
		for( int othAction = 0; othAction < GameAction.NUM_ACTIONS; othAction++ ) {
		    
		    if( othAction == jointAction.getAction( agentIndex ) ) 
			continue;
		    
		    double pi_soa = getPi( curState, othAction );
		    pi_soa -= Delta / (GameAction.NUM_ACTIONS-1);
		    setPi( curState,  othAction,  pi_soa );
		}
	    }
	    
	    
	    return nextAction;
	}
    }
    
    protected void visit( GameState curState, GameAction curAction )
    {
	if( curState == null || curAction == null ) {
	    
	    System.out.println("@MARL->visit: Wrong Parameters!");
	    return;
	}
	
	int loc0 = curState.getLocationID(0);
	int loc1 = curState.getLocationID(1);
	int loc2 = curState.getLocationID(2);
	int a0 = curAction.getAction(0);
	int a1 = curAction.getAction(1);
	int a2 = curAction.getAction(2);
	
	vstNum[loc0][loc1][loc2][a0][a1][a2] += 1.0;
	
	stateCounts[loc0][loc1][loc2] += 1;
    }
    
    private int getStateCount( GameState gameState )
    {
	if( gameState == null ) {
	    
	    System.out.println("@WoLFPHC->getStateCount: NULL Parameter!");
	    return -1;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int loc2 = gameState.getLocationID(2);
	
	return stateCounts[loc0][loc1][loc2];
    }
    
    private GameAction getAction_Pi( GameState gameState )
    {
	if( gameState == null ) {
	    
	    System.out.println("@WoLFPHC->getAction_Pi: NULL Parameter!");
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
	
	return new GameAction(new int[]{actionIndex,actionIndex,actionIndex});
    }
    
    private double getMaxQValue( GameState gameState )
    {
	if( gameState == null ) {
	    
	    System.out.println("@WoLFPHC->getMaxQValue: NULL Parameter!");
	    return 0.0;
	}
	
	double maxValue = getQValue( gameState, 0 );
	for( int act = 1; act < GameAction.NUM_ACTIONS; act++ ) {
	    
	    double value = getQValue(gameState, act);
	    
	    if( value > maxValue )
		maxValue = value;
	}
	
	return maxValue;
    }
    
    private double getPi( GameState gameState, int action )
    {
	if( gameState == null ) {
	    
	    System.out.println("@WoLFPHC->getPi: NULL Parameter!");
	    return 0.0;
	}
	else if( action < 0 || action >= GameAction.NUM_ACTIONS ) {
	    
	    System.out.println("@WoLFPHC->getPi: Unavailable Action!");
	    return 0.0;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int loc2 = gameState.getLocationID(2);
	
	return pi[loc0][loc1][loc2][action];
    }
    
    
    private double getPiBar( GameState gameState, int action )
    {
	if( gameState == null ) {
	    
	    System.out.println("@WoLFPHC->getPiBar: NULL Parameter!");
	    return 0.0;
	}
	else if( action < 0 || action >= GameAction.NUM_ACTIONS ) {
	    
	    System.out.println("@WoLFPHC->getPiBar: Unavailable Action!");
	    return 0.0;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int loc2 = gameState.getLocationID(2);
	
	return pi[loc0][loc1][loc2][action];
    }
    
    
    private void setPi( GameState gameState, int action, double piv )
    {
	if( gameState == null ) {
	    
	    System.out.println("@WoLFPHC->setPi: NULL Parameter!");
	    return;
	}
	else if( action < 0 || action >= GameAction.NUM_ACTIONS ) {
	    
	    System.out.println("@WoLFPHC->setPi: Unavailable Action!");
	    return;
	}
	else if( piv < 0 || piv > 1 ) {
	    
	    //System.out.println("@WoLFPHC->setPi: Unavailable Probability! "+piv);
	    
	    if( piv < 0 )
		piv = 0;
	    if( piv > 1 )
		piv = 1;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int loc2 = gameState.getLocationID(2);
	
	pi[loc0][loc1][loc2][action] = piv;
    }
    
    private void setPiBar( GameState gameState, int action, double pib )
    {
	if( gameState == null ) {
	    
	    System.out.println("@WoLFPHC->setPiBar: NULL Parameter!");
	    return;
	}
	else if( action < 0 || action >= GameAction.NUM_ACTIONS ) {
	    
	    System.out.println("@WoLFPHC->setPiBar: Unavailable Action!");
	    return;
	}
	else if( pib < 0 || pib > 1 ) {
	    
	    //System.out.println("@WoLFPHC->setPiBar: Unavailable Probability!");
	    if( pib < 0 )
		pib = 0;
	    if( pib > 1 )
		pib = 1;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int loc2 = gameState.getLocationID(2);
	
	pi_bar[loc0][loc1][loc2][action] = pib;
    }
    
    protected double getQValue( int agent, GameState gameState, 
	    GameAction gameAction )
    {
	if( gameAction == null || 
		gameState == null ) {
	    
	    System.out.println("@WoLFPHC->getQValue: Wrong Parameters!");
	    return 0.0;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int loc2 = gameState.getLocationID(2);
	int a = gameAction.getAction( agentIndex );
	
	return Q[loc0][loc1][loc2][a];
    }
    
    private double getQValue( GameState gameState, int action )
    {
	if( gameState == null ) {
	    
	    System.out.println("@WoLFPHC->getQValue: Wrong Parameters!");
	    return 0.0;
	}
	if( action < 0 || action >= GameAction.NUM_ACTIONS ) {
	    
	    System.out.println("WoLFPHC->getQValue: Unavailable Action!");
	    return 0.0;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int loc2 = gameState.getLocationID(2);
	
	return Q[loc0][loc1][loc2][action];
    }
    
    protected void setQValue( int agent, GameState gameState, 
	    GameAction gameAction, double value )
    {
	if( gameAction == null || 
		gameState == null ) {
	    
	    System.out.println("@WoLFPHC->setQValue: Wrong Parameters!");
	    return;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int loc2 = gameState.getLocationID(2);
	int a = gameAction.getAction( agentIndex );
	
	Q[loc0][loc1][loc2][a] = value;
    }
    
    private void setQValue( GameState gameState, int action, 
	    double value )
    {
	if( gameState == null ) {
	    
	    System.out.println("@WoLFPHC->getQValue: Wrong Parameters!");
	    return;
	}
	if( action < 0 || action >= GameAction.NUM_ACTIONS ) {
	    
	    System.out.println("WoLFPHC->getQValue: Unavailable Action! ");
	    return;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int loc2 = gameState.getLocationID(2);
	
	Q[loc0][loc1][loc2][action] = value;
    }
    
    private double delta_sa( GameState gameState, int action )
    {
	if( gameState == null ) {
	    
	    System.out.println("@WoLFPHC->delta_sa: Wrong Parameters!");
	    return 0.0;
	}
	if( action < 0 || action >= GameAction.NUM_ACTIONS ) {
	    
	    System.out.println("WoLFPHC->delta_sa: Unavailable Action!");
	    return 0.0;
	}
	
	double sumQ_pi = 0.0;
	double sumQ_pibar = 0.0;
	for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
		    
	    double qValue = getQValue( gameState, act );
	    double pi_sa = getPi(gameState, act);
	    double pi_bar_sa = getPiBar(gameState, act);
		    
	    sumQ_pi  += qValue * pi_sa;
	    sumQ_pibar += qValue * pi_bar_sa;
	    
	}
	double delta = DELTA_LOSE;
	if( sumQ_pi > sumQ_pibar )
	    delta = DELTA_WIN;
		
	double delta_sa = getPi( gameState, action );
	double divDelta = delta / (GameAction.NUM_ACTIONS-1);
	if( delta_sa > divDelta )
	    delta_sa = divDelta;
	
	return delta_sa;
    }
}
