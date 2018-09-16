package algorithms;


import java.util.ArrayList;
import java.util.PriorityQueue;

import gameGridWorld.GameAction;
import gameGridWorld.GameState;
import gameGridWorld.GridWorld;
import help.Support;
import help.XVector;

/**
 * new algorithm:
 * NashQ with support profile transfer
 * @author dearhude1
 *
 */

public class NashQSuppTrans extends NashQ
{
    
    /**
     * dim 1-2: states
     * dim 3: agent
     */
    private Support[][][] supportProfiles;
    
    
    /**
     * the last executed joint action in each state
     */
    private GameAction[][] lastActions;
    
    /**
     * the last equilibrium in each state
     */
    private double[][][] lastEquilibrium;
    
    /**
     * the changes in Q-table
     */
    private double[][][][][] deltaQs;
    
    public NashQSuppTrans()
    {
	super();
	
	
	/**
	 * init the Q-table
	 * for centralized algorithms 
	 * the Q-tables can be initialized randomly
	 */
	int locNum = GridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;

	
	/**/
	supportProfiles = new Support[locNum][locNum][GridWorld.NUM_AGENTS];
	
	lastActions = new GameAction[locNum][locNum];
	lastEquilibrium = new double[locNum][locNum][GridWorld.NUM_AGENTS * GameAction.NUM_ACTIONS];
	deltaQs = new double[GridWorld.NUM_AGENTS][locNum][locNum][actionNum][actionNum];
	
	for( int s0 = 0; s0 < locNum; s0++ )
	    for( int s1 = 0; s1 < locNum; s1++ ) {
		    
		lastActions[s0][s1] = null;
		
		for( int agent = 0; agent < GridWorld.NUM_AGENTS; agent++ ) 
		    supportProfiles[s0][s1][agent] = null;
		    
		for( int act = 0; act < GridWorld.NUM_AGENTS * GameAction.NUM_ACTIONS; act++ )
		    lastEquilibrium[s0][s1][act] = 0.0;
		
		for( int a0 = 0; a0 < actionNum; a0++ )
		    for( int a1 = 0; a1 < actionNum; a1++ ) {
			
			deltaQs[0][s0][s1][a0][a1] = deltaQs[1][s0][s1][a0][a1] = 0.0;
		    }
	    }
	
	
    }
    
    
    public NashQSuppTrans( double alpha, double gamma, double epsilon )
    {
	super( alpha, gamma, epsilon);
	
	/**
	 * init the Q-table
	 * for centralized algorithms 
	 * the Q-tables can be initialized randomly
	 */
	int locNum = GridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	
	
	/**/
	supportProfiles = new Support[locNum][locNum][GridWorld.NUM_AGENTS];
	
	lastActions = new GameAction[locNum][locNum];
	lastEquilibrium = new double[locNum][locNum][GridWorld.NUM_AGENTS * GameAction.NUM_ACTIONS];
	deltaQs = new double[GridWorld.NUM_AGENTS][locNum][locNum][actionNum][actionNum];
	
	for( int s0 = 0; s0 < locNum; s0++ )
	    for( int s1 = 0; s1 < locNum; s1++ ) {
		    
		lastActions[s0][s1] = null;
		
		for( int agent = 0; agent < GridWorld.NUM_AGENTS; agent++ ) 
		    supportProfiles[s0][s1][agent] = null;
		    
		for( int act = 0; act < GridWorld.NUM_AGENTS * GameAction.NUM_ACTIONS; act++ )
		    lastEquilibrium[s0][s1][act] = 0.0;
		
		for( int a0 = 0; a0 < actionNum; a0++ )
		    for( int a1 = 0; a1 < actionNum; a1++ ) {
			
			deltaQs[0][s0][s1][a0][a1] = deltaQs[1][s0][s1][a0][a1] = 0.0;
		    }
	    }
	
    }
    
    public GameAction updateQ( GameState curState, GameAction jointAction, 
	    double[] rewards, GameState nextState )
    {
	
	if( nextState == null ) {
	    
	    System.out.println("@CenNashQ->updateQ: NULL nextState!");
	    
	    return null;
	}
	else {
	    
	    /**
	     * incorporate three rules
	     *
	    boolean bCompute = shouldComputeNE( nextState );
	    
	    double[] nashEquil = null;
	    if( bCompute ) {
		
		nashEquil = computeNE_2agent( agentIndex, nextState );
	    }
	    else {
		
		nashEquil = lastEquilibrium[nextState.getLocationID(0)][nextState.getLocationID(1)];
	    }
	    */
	    
	    double[] nashEquil = computeNE_2agent(agentIndex, nextState);
	    
	    /**
	     * get a joint action according to the correlated equilibrium
	     */
	    GameAction nextAction = getJointAction_NE( nashEquil );
	    
	    
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
		double[] nashQValues = getNashQValues( nextState, nashEquil );
		    
		for( int agent = 0; agent < GridWorld.NUM_AGENTS; agent++ ) {
		    
		    /**
		     * get the Q-value
		     */
		    double Qsa = getQValue( agent, curState, jointAction );
		    double lastQsa = Qsa;
		    
		    /**
		     * updating rule
		     */
		    Qsa = (1 - ALPHA) * Qsa + ALPHA * (rewards[agent] + GAMMA * nashQValues[agent]);
		    
		    
		    /**
		     * variable learning rate
		     */
		    //double alpha = getVariableAlpha( curState, jointAction );
		    //Qsa = (1 - alpha) * Qsa + alpha * (rewards[agent] + GAMMA * nashQValues[agent]);
		    
		    /**
		     * write back to the tables
		     */
		    setQValue( agent, curState, jointAction, Qsa );
		    
		    /**
		     * record the change
		     *
		    int curLoc0 = curState.getLocationID(0);
		    int curLoc1 = curState.getLocationID(1);
		    int curAct0 = jointAction.getAction(0);
		    int curAct1 = jointAction.getAction(1);
		    deltaQs[agent][curLoc0][curLoc1][curAct0][curAct1] = Qsa - lastQsa;
		    */
		}
		
		/**
		 * maybe we can release some memories
		 */
		nashQValues = null;
	    }
	    
	    /**
	     * maybe we can release some memories
	     */
	    nashEquil = null;
	    
	    return nextAction;
	}
    }

    /**/
    protected double[] computeNE_2agent( int agent, GameState gameState )
    {
	if( gameState == null ) {
	    
	    System.out.println("@NashQSuppTrans->computeNE: NULL gameState!");
	    return null;
	}
	else if( agent < 0 || agent >= GridWorld.NUM_AGENTS ) {
	    
	    System.out.println("NashQSuppTrans->computeNE: Wrong agent Parameter!");
	    return null;
	}
	
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	
	//return compute( gameState );
	
	
	/**/
	boolean consistent = true;
	
	if( supportProfiles[loc0][loc1][0] != null &&
		supportProfiles[loc0][loc1][1] != null ) {
	    
	    for( int a0 = 0; a0 < GameAction.NUM_ACTIONS; a0++ ) {
		    
		if( !supportProfiles[loc0][loc1][0].supported(a0) )
		    continue;
		
		if( conditionallyDominated(0, a0, supportProfiles[loc0][loc1][1], gameState) ) {
		    
		    consistent = false;
		    break;
		}
	    }
	    
	    if( consistent ) {
		
		for( int a1 = 0; a1 < GameAction.NUM_ACTIONS; a1++ ) {
		    
		    if( !supportProfiles[loc0][loc1][1].supported(a1) )
			    continue;
		    
		    if( conditionallyDominated(1, a1, supportProfiles[loc0][loc1][0], gameState) ) {
			    
			consistent = false;
			break;
		    }
		}
	    }
	    
	}
	else
	    consistent = false;
	
	
	if( consistent ) {
	    
	    
	    double[] nashEquil = lastEquilibrium[loc0][loc1];
	    return nashEquil;
	    
	    /**
	    double[] nashEquil = feasibilityProgram( supportProfiles[loc0][loc1], gameState);
	    
	    if( nashEquil != null ) {
		
		System.out.println("Reduce Time!");
		
		return nashEquil;
	    }
	    else
		return compute( gameState );
	    */
	}
	else {
	    
	    return compute( gameState );
	}
	
	
	
    }
    
    
    private double[] compute( GameState gameState )
    {
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	
	/**
	 * get all possible support size vector, sorted
	 * increasing order
	 * first order: the sum of the support size
	 * second order: the max difference of the support size
	 */
	//ArrayList<XVector> xList = generateXVectors_2agent();
	
	//new vector generating methods
	ArrayList<XVector> xList = generateXVectors( gameState );
	
	for( int i = 0; i < xList.size(); i++ ) {
	    
	    XVector xVector = xList.get(i);
	    
	    
	    //for all S0
	    int x0 = xVector.getX(0);
	    ArrayList<Support> D0 = generateSupportDomain(x0);
	    for( int s0Index = 0; s0Index < D0.size(); s0Index++ ) {
		
		Support supp0 = D0.get( s0Index );
		
		//given s0, find the action of a1 which are not conditionally dominated
		boolean[] a1_supported = new boolean[GameAction.NUM_ACTIONS];
		for( int a1 = 0; a1 < GameAction.NUM_ACTIONS; a1++ )	    
		    a1_supported[a1] = !conditionallyDominated( 1, a1, supp0, gameState );
		Support supDominated1 = new Support( a1_supported );
		if( !supDominated1.isValid() ) {
		    supDominated1 = null;
		    continue;
	    	}
		
		//whether there exists action a0 \in S0 which is conditionally dominated
		//given supDominated1
		boolean exists_dominated_a0 = false;
		for( int a0 = 0; a0 < GameAction.NUM_ACTIONS; a0++ ) {
		    
		    if( !supp0.supported(a0) )
			continue;
		    
		    if( conditionallyDominated( 0, a0,  supDominated1, gameState ) ) {
			
			exists_dominated_a0 = true;
			break;
		    }
		}
		
		if( !exists_dominated_a0 ) {
		    
		    //for all s1
		    int x1 = xVector.getX(1);
		    ArrayList<Support> D1 = generateSupportDomain(x1);
		    
		    for( int s1Index = 0; s1Index < D1.size(); s1Index++ ) {
			
			Support supp1 = D1.get(s1Index);
			
			if( !subSupport(supp1, supDominated1) )
			    continue;
			
			//whether there exists action a0 \in S0 which is conditionally dominated 
			//given S1
			boolean exists_dominated_s0s1 = false;
			for( int a0 = 0; a0 < GameAction.NUM_ACTIONS; a0++ ) {
			    
			    if( !supp0.supported(a0) )
				continue;
			    
			    if( conditionallyDominated( 0, a0,  supp1, gameState ) ) {
				
				exists_dominated_s0s1 = true;
				break;
			    }
			}
			
			if( !exists_dominated_s0s1 ) {
			    
			    double[] nashEquil = feasibilityProgram(new Support[]{supp0,supp1}, gameState);
			    
			    if( nashEquil != null ) {
				
				/**
				 * record the support profile
				 */
				supportProfiles[loc0][loc1][0] = supp0;
				supportProfiles[loc0][loc1][1] = supp1; 
				lastEquilibrium[loc0][loc1] = nashEquil;
				
				return nashEquil;
			    }
			}
		    }
		}
	    }
	}
	
	return null;
    }
    
    /**/
    private ArrayList<XVector> generateXVectors( GameState gameState )
    {
	
	if( gameState == null ) {
	    
	    System.out.println("@NashQSuppTrans->generateXVectors: NULL Parameter!");
	    return null;
	}
	
	ArrayList<XVector> retList = new ArrayList<XVector>();
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	if( supportProfiles[loc0][loc1][0] == null || 
		supportProfiles[loc0][loc1][1] == null ) {
	    
	    return generateXVectors_2agent();
	}
	
	int suppSize0 = supportProfiles[loc0][loc1][0].supportSize();
	int suppSize1 = supportProfiles[loc0][loc1][1].supportSize();
	
	for( int diff0 = 0; diff0 <= 3; diff0++ ) {
	    for( int diff1 = 0; diff1 <= 3; diff1++ ) {
		
		int x0 = suppSize0 - diff0;
		int x1 = suppSize1 - diff1;
		XVector xVector = new XVector( new int[]{x0, x1} );
		if( xVector.isAvailable() || retList.contains(xVector) )
		    xVector = null;
		else
		    retList.add( xVector );
		
		x0 = suppSize0 + diff0;
		x1 = suppSize1 - diff1;
		xVector = new XVector( new int[]{x0, x1} );
		if( xVector.isAvailable() || retList.contains(xVector) )
		    xVector = null;
		else
		    retList.add( xVector );
		
		x0 = suppSize0 - diff0;
		x1 = suppSize1 + diff1;
		xVector = new XVector( new int[]{x0, x1} );
		if( xVector.isAvailable() || retList.contains(xVector) )
		    xVector = null;
		else
		    retList.add( xVector );
		
		x0 = suppSize0 + diff0;
		x1 = suppSize1 + diff1;
		xVector = new XVector( new int[]{x0, x1} );
		if( xVector.isAvailable() || retList.contains(xVector) )
		    xVector = null;
		else
		    retList.add( xVector );
	    }
	}
	
	
	return retList;
    }
    
    
    /**/
    private boolean shouldComputeNE( GameState gameState ) 
    {
	if( gameState == null ) {
	    
	    System.out.println("NashQSuppTrans->shouldComputeNE: Parameter error");
	    return true;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int actionNum = GameAction.NUM_ACTIONS;
	
	if( lastActions[loc0][loc1] == null ) {
	    
	    return true;
	}
	

	int act0 = lastActions[loc0][loc1].getAction(0);
	int act1 = lastActions[loc0][loc1].getAction(1);
	
	if( lastEquilibrium[loc0][loc1][act0]*lastEquilibrium[loc0][loc1][actionNum+act1] > 0.0 ) {
	    
	    return true;
	}
	else {
	    

	    if( (!(lastEquilibrium[loc0][loc1][act0] > 0.0)) && 
		    (!(lastEquilibrium[loc0][loc1][actionNum+act1] > 0.0)) ) {
		
		return false;
	    }

	    else if( lastEquilibrium[loc0][loc1][act0] > 0.0 ) {
		
		if( deltaQs[1][loc0][loc1][act0][act1] < 0.0 )
		    return false;
		else
		    return true;
	    }
	    else if( lastEquilibrium[loc0][loc1][actionNum+act1] > 0.0 ) {
		
		if( deltaQs[0][loc0][loc1][act0][act1] < 0.0 )
		    return false;
		else
		    return true;
	    }
	    else {
		
		System.out.println("Error  error error");
		return true;
	    } 
	}
    }
    
    
}
