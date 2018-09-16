package algorithms;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cp.IloCP;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.PriorityQueue;
import gameGridWorld.GameAction;
import gameGridWorld.GameState;
import gameGridWorld.SparseGridWorld;
import gameGridWorld.StateActionPair;
import help.Support;
import help.XVector;

public class NashQ extends MARL
{

    
    public NashQ()
    {
	super( 0 );
	
    }
    
    public NashQ( double alpha, double gamma, double epsilon )
    {
	super( 0, alpha, gamma, epsilon);
	
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
	    
	    System.out.println("@NashQ->updateQ: NULL nextState!");
	    
	    return null;
	}
	else {
	    
	    /**
	     * compute the Nash equilibrium in the next state
	     */
	    double[] nashEquil = computeNE( agentIndex, nextState );
	    
	    
	    /**
	     * get a joint action according to the Nash equilibrium
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
		    
		for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		    
		    /**
		     * get the Q-value
		     */
		    double Qsa = getQValue( agent, curState, jointAction );
		    
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
		}
		
		/**
		 * for tranfer algorithm, we need decay
		 * for non-transfer algorithm, no need
		 */
		ALPHA *= 0.99988;//0.9958;//0.998;//0.99958;//0.99958;
		
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
    
    protected double[] computeNE( int agent, GameState gameState )
    {
	if( gameState == null ) {
	    
	    System.out.println("@NashQ->computeNE: NULL gameState!");
	    return null;
	}
	else if( agent < 0 || agent >= SparseGridWorld.NUM_AGENTS ) {
	    
	    System.out.println("NashQ->computeNE: Wrong agent Parameter!");
	    return null;
	}
	
	
	/**
	 * get all possible support size vector, sorted
	 * increasing order
	 * first order: the sum of the support size
	 * second order: the max difference of the support size
	 */
	ArrayList<XVector> xList = generateXVectors();
	for( int i = 0; i < xList.size(); i++ ) {
	    
	    XVector xVector = xList.get(i);
	    
	    /**
	     * initialize support and domain according to 
	     * the current xVector
	     */
	    ArrayList<ArrayList<Support>> domainProfile = new ArrayList<ArrayList<Support>>();
	    for( int j = 0; j < SparseGridWorld.NUM_AGENTS; j++ ) {
		
		domainProfile.add( generateSupportDomain(xVector.getX(j)) );
	    }
	    
	    Support[] supportProfile = new Support[SparseGridWorld.NUM_AGENTS];
	    for( int j = 0; j < SparseGridWorld.NUM_AGENTS; j++ ) {
		
		supportProfile[j] = null;
	    }
	    
	    
	    /**
	     * call the method recursiveBackTracking
	     */
	    double[] nashEquil = recursiveBackTracking( supportProfile, 
		    domainProfile, 0, gameState );
	    if( nashEquil != null )
		return nashEquil;
	    
	    //release memory
	    domainProfile = null;
	    supportProfile = null;
	}
	
	
	return null;
    }
    

    

    //whether supp1 is a sub support of supp2
    protected boolean subSupport( Support supp1, Support supp2 )
    {
	//parameters	
	for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
	    
	    
	    if( supp1.supported( act ) )
		if( !supp2.supported( act ) )
		    return false;
	}
	
	return true;
    }
    
    /**
     * get the value of a Nash equilibrium
     */
    protected double[] getNashQValues( GameState gameState, double[] nashE )
    {
	double[] values = new double[SparseGridWorld.NUM_AGENTS];
	for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ )
	    values[agent] = 0.0;
	
	if( nashE == null ) {
	    
	    //System.out.println("@CenNashQ->getNashQValues: NULL Nash Equilibrium!");
	    return values;
	}
	
	
	int stateIndex = SparseGridWorld.queryStateIndex( gameState );
	
	ArrayList<GameAction> allActions = SparseGridWorld.getAllJointActions();
	
	for( int actionIndex = 0; actionIndex < allActions.size(); actionIndex++ ) {
	    
	    GameAction jntAction = allActions.get( actionIndex );
	    StateActionPair saPair = new StateActionPair( gameState, jntAction );
	    
	    //get the component of each agent in Nash equilibrium
	    double jointPro = 1.0;
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		
		int compIndex = agent * GameAction.NUM_ACTIONS + jntAction.getAction(agent);
		jointPro *= nashE[compIndex];
	    }
	    
	    //for robust
	    /**
	    if( !Qs.containsKey( saPair ) ) {
		
		saPair = null;
		return values;
	    }
	    
	    float[] qEntry = Qs.get( saPair );
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		
		values[agent] += qEntry[agent] * jointPro;
	    }
	    */
	    
	    /**/
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		
		values[agent] += valueFunc[agent][stateIndex][actionIndex] * jointPro;
	    }
	    
	}
	
	
	return values;
    }
    
    
    
    //done
    protected double[] recursiveBackTracking( Support[] suppProfile, 
	    ArrayList<ArrayList<Support>> domProfile, int index, GameState gameState )
    {
	
	/**
	 * check parameters
	 */
	
	if( index == SparseGridWorld.NUM_AGENTS ) {
	    
	    //feasibility program
	    return feasibilityProgram( suppProfile, gameState );
	    
	}
	else {
	    
	    /**
	     * initialized the support of the current agent
	     */
	    ArrayList<Support> Di = domProfile.get( index );
	    
	    while( !Di.isEmpty() ) {
		
		suppProfile[index] = Di.remove(0);
		
		/**
		 * generate a new domain profile for IRSDS
		 * for each agent whose support has been instantiated, the domain 
		 * contains only that support
		 * 
		 * for each other agent, the domain contains all support that were 
		 * not eliminated in a previous call
		 */
		ArrayList<ArrayList<Support>> domPro_IRSDS = new ArrayList<ArrayList<Support>>();
		for( int i = 0; i <= index; i++ ) {
		    
		    ArrayList<Support> dom = new ArrayList<Support>();
		    dom.add( suppProfile[i] );
		    
		    domPro_IRSDS.add( dom );
		}
		for( int i = index+1; i < SparseGridWorld.NUM_AGENTS; i++ ) {
		    
		    //from D_{i+1} to D_n, we use a new list
		    /**/
		    ArrayList<Support> dom = new ArrayList<Support>();
	    		    
		    for( int listIndex = 0; listIndex < domProfile.get(i).size(); listIndex++ )
			dom.add( domProfile.get(i).get(listIndex) );
    		    	
    		    
		    domPro_IRSDS.add( dom );
		}
		
		
		//if IRSDS succeeds, update domProfile using domPro
		//else, domProfile remains unchanged
		if( IRSDS( domPro_IRSDS, gameState ) ) {
		    
		    //use an updated domain profile
		    double[] nashEquil = recursiveBackTracking( suppProfile, domPro_IRSDS, 
			    index+1, gameState );
		  
		    if( nashEquil != null )
			return nashEquil;
		}
	    }
	}
	
	
	return null;
    }
    
    
    //done
    private boolean IRSDS( ArrayList<ArrayList<Support>> domPro, GameState gameState )
    {
	/**
	 * check the parameter
	 */
	if( domPro == null || gameState == null ||
		domPro.size() != SparseGridWorld.NUM_AGENTS ) {
	    
	    System.out.println("@NashQ->IRSDS: Wrong Parameters!");
	    
	    return false;
	}
	
	boolean changed = true;
	while( changed ) {
	    
	    changed = false;
	    
	    /**
	     * loop for all agent index
	     */
	    for( int agent_i = 0; agent_i < SparseGridWorld.NUM_AGENTS; agent_i++ ) {
		
		/**
		 * get the domain of agent i's support Di
		 * and compute the union support of all elements in Di
		 */
		ArrayList<Support> Di = domPro.get(agent_i);
		
		//if Di is empty already
		if( Di.isEmpty() )
		    return false;
		
		Support cupSupp = supportCup( Di );
		
		/**
		 * loop for actions supported by cupSupp
		 */
		for( int a_i = 0; a_i < GameAction.NUM_ACTIONS; a_i++ ) {
		    
		    if( !cupSupp.supported(a_i) )
			continue;
		    
		    /**
		     * loop for other actions in Ai
		     */
		    for( int ap_i = 0; ap_i < GameAction.NUM_ACTIONS; ap_i++ ) {
			
			if( ap_i == a_i )
			    continue;
			
			/**
			 * check whether a is conditionally dominated by ap
			 */
			Support[] otherCups = new Support[SparseGridWorld.NUM_AGENTS];
			for( int j = 0; j < SparseGridWorld.NUM_AGENTS; j++ ) {
			    
			    if( j == agent_i ) {
				
				otherCups[j] = null; 
				continue;
			    }
			    
			    ArrayList<Support> Dj = domPro.get(j);
			    
			    //if Dj is empty already
			    if( Dj.isEmpty() )
				return false;
				
			    otherCups[j] = supportCup( Dj );
			}
			
			//generate all possible joint actions
			ArrayList<GameAction> othJntActList_a = generateOtherJntActions(agent_i, otherCups);
			ArrayList<GameAction> othJntActList_ap = generateOtherJntActions(agent_i, otherCups);
			
			
			//determine whether a is conditionally dominated by ap
			/**
			 * conditionally dominated:
			 * given a profile set of available actions R_{-i} \subset A_{-i}
			 * for an action a_i and another action a_ip
			 * if for all a_{-i} \in R__{-i}, there holds:
			 * u_i(a_i,a_{-i}) < u_i(a_ip,a_{-i})
			 * then a_i is conditionally dominated by a_ip
			 */
			boolean condDominated = true;
			for( int listIndex = 0; listIndex < othJntActList_a.size(); listIndex++ ) {
	    
			    GameAction jntAction_a = othJntActList_a.get(listIndex);
			    GameAction jntAction_ap = othJntActList_ap.get(listIndex);
					
			    jntAction_a.setAction( agent_i, a_i );
			    jntAction_ap.setAction( agent_i, ap_i );
					
			    double Q_sa = getQValue(agent_i, gameState, jntAction_a);
			    double Q_sap = getQValue(agent_i, gameState, jntAction_ap);
	    
			    if( Q_sa >= Q_sap ) {
					    
				condDominated = false;
				break;
			    }
			}
			
			/**
			 * if a is conditionally dominated by ap 
			 * then remove the supports which support a in Di 
			 */
			if( condDominated ) {
			    
			    for( int suppIndex = 0; suppIndex < Di.size(); suppIndex++ ) {
				
				Support supp = Di.get( suppIndex );
				if( supp.supported( a_i ) ) {
				    
				    Di.remove( suppIndex );
				    suppIndex--;
				}
			    }
			    
			    changed = true;
			    
			    /**
			     * return failure when Di is empty
			     */
			    if( Di.isEmpty() )
				return false;
			}
		    }
		}
		
	    }
	    
	}
	
	return true;
    }
    
    //done
    protected double[] feasibilityProgram( Support[] suppProfile, GameState gameState )
    {
	if( suppProfile == null || 
		suppProfile.length != SparseGridWorld.NUM_AGENTS ) {
	    
	    System.out.println("@NashQ->feasibilityProgram: Wrong Parameter!");
	    return null;
	}
	
	double[] solution = null;
	
	try {
	    
	    /**
	     * 1. firstly, create the model
	     * IloCplex is used to create Mathmatical Programming Models
	     * such as:
	     * Linear Programming
	     * Quadratic Programming
	     * Quadratically Constrained Program...
	     */
	    IloCP nashCP = new IloCP();
	    
	    //IloCplex nashCP = new IloCplex();
	    
	    //nashCP.setParam(IloCplex.Param.WorkMem, 2000);
	   // nashCP.setParameter(IloCP.ParameterValues.,2);
	    
	    /**
	     * 2. secondly, create the variables:
	     * stores each agent's probability of taking each action
	     * agent i's j-th action probability: index = i * Num_Actions
	     */
	    int varNum = SparseGridWorld.NUM_AGENTS * GameAction.NUM_ACTIONS;
	    double[] lowBounds = new double[SparseGridWorld.NUM_AGENTS * GameAction.NUM_ACTIONS];
	    double[] uppBounds = new double[SparseGridWorld.NUM_AGENTS * GameAction.NUM_ACTIONS];
	    for( int varIndex = 0; varIndex < SparseGridWorld.NUM_AGENTS * GameAction.NUM_ACTIONS; varIndex++ ) {
		
		lowBounds[varIndex] = 0.0;
		uppBounds[varIndex] = 1.0;
	    }
	    IloNumVar[] p = nashCP.numVarArray(SparseGridWorld.NUM_AGENTS*GameAction.NUM_ACTIONS, lowBounds, uppBounds);
	    
	    
	    /**
	     * 3. then create the constraints:
	     */
	    
	    //3.1 for the sum of each agent's action probability
	    //\Sum_{a_i \in S_i} p_i(a_i) = 1
	    for( int agent_i = 0; agent_i < SparseGridWorld.NUM_AGENTS; agent_i++ ) {
		
		Support supp_i = suppProfile[agent_i];
		double[] coeff = new double[SparseGridWorld.NUM_AGENTS * GameAction.NUM_ACTIONS];
		for( int coeffIndex = 0; coeffIndex < SparseGridWorld.NUM_AGENTS*GameAction.NUM_ACTIONS; coeffIndex++ ) {
		    
		    int agent_j = coeffIndex / GameAction.NUM_ACTIONS;
		    int act = coeffIndex % GameAction.NUM_ACTIONS;
		    
		    if( agent_j != agent_i )
			coeff[coeffIndex] = 0.0;
		    else if( !supp_i.supported(act) )
			coeff[coeffIndex] = 0.0;
		    else
			coeff[coeffIndex] = 1.0;
		}
		
		IloNumExpr sumExpr = nashCP.scalProd(coeff, p);
		nashCP.addEq( sumExpr, 1.0 );
	    }
	    
	    //3.2 for each agent's unsupported action
	    //for any a_i \not\in S_i p_i(a_i) = 0
	    for( int agent_i = 0; agent_i < SparseGridWorld.NUM_AGENTS; agent_i++ ) {
		
		Support supp_i = suppProfile[agent_i];
		if( supp_i.supportSize() == GameAction.NUM_ACTIONS )
		    continue;
		
		for( int act = 0;  act < GameAction.NUM_ACTIONS; act++ ) {
		    
		    //should be !supported?
		    if( !supp_i.supported( act ) ) {
			
			double[] coeff = new double[SparseGridWorld.NUM_AGENTS * GameAction.NUM_ACTIONS];
			for( int coeffIndex = 0; coeffIndex < SparseGridWorld.NUM_AGENTS*GameAction.NUM_ACTIONS; coeffIndex++ ) {
			    
			    if( coeffIndex == (agent_i * GameAction.NUM_ACTIONS + act) )
				coeff[coeffIndex] = 1.0;
			    else
				coeff[coeffIndex] = 0.0;
			}
			
			IloNumExpr eqzeroExpr = nashCP.scalProd( coeff, p );
			nashCP.addEq( eqzeroExpr, 0.0 );
		    }
		}
	    }
	    
	    //3.3 for the nash condition inequality
	    //for any i \in N, any a_i \in S_i and any a_i' \not\in S_i
	    //\Sum_{a_{-i} \in S_{-i}} p(a_{-i}[u_i(a_i,a_{-i})-u_i(a_i',a_{-i})] >= 0
	    for( int agent_i = 0; agent_i < SparseGridWorld.NUM_AGENTS; agent_i++ ) {
		    
		Support support_i = suppProfile[agent_i];
		    
		/**
		 * loop for all supported actions
		 */
		for( int a_i = 0; a_i < GameAction.NUM_ACTIONS; a_i++ ) {
			
		    if( !support_i.supported( a_i ) )
			continue;
			
		    /**
		     * loop for all actions that are not supported
		     */
		    for( int ap_i = 0; ap_i < GameAction.NUM_ACTIONS; ap_i++ ) {
			    
			if( support_i.supported( ap_i ) )
			    continue;
			    
			    
			/**
			 * loop for the joint actions of the other agent's supported!! actions
			 */
			ArrayList<GameAction> othJntActList_a = generateOtherJntActions(agent_i, suppProfile);
			ArrayList<GameAction> othJntActList_ap = generateOtherJntActions(agent_i, suppProfile);
			
			IloNumExpr ineqExpr = nashCP.constant(0);
			for( int listIndex = 0; listIndex < othJntActList_a.size(); listIndex++ ) {
					
			    GameAction jntAction_a = othJntActList_a.get(listIndex);
			    GameAction jntAction_ap = othJntActList_ap.get(listIndex);
			    jntAction_a.setAction(agent_i, a_i);
			    jntAction_ap.setAction(agent_i, ap_i);
					
			    double Q_sa = getQValue( agent_i, gameState, jntAction_a );
			    double Q_sap = getQValue( agent_i, gameState, jntAction_ap );
			    double coeff = Q_sa - Q_sap;
					
			    //variable indices
			    IloNumExpr itemExpr = nashCP.constant(coeff);
			    for( int agent_j = 0; agent_j < SparseGridWorld.NUM_AGENTS; agent_j++ ) {
						
				if( agent_j == agent_i )
				    continue;
						
				int act_j = jntAction_a.getAction( agent_j );
				int varIndex_j = agent_j * GameAction.NUM_ACTIONS + act_j;
						
				itemExpr = nashCP.prod( itemExpr, p[varIndex_j] );
			    }
			    //add the item to the inequality expression
			    ineqExpr = nashCP.sum( ineqExpr, itemExpr );
			}
			/**
			 * create the inequality constraint
			 * 
			 * be careful! should be greater than 0 
			 * not less than 0
			 */
			nashCP.addGe( ineqExpr, 0 );
		    }//loop for unsupported actions of agent_i
		}//loop for supported actions of agent_i
		
		
		/**
		 * all supported actions has equal values
		 */
		int firstSuppAction = 0;
		for( int a_i = 0; a_i < GameAction.NUM_ACTIONS; a_i++ ) {
				
		    if( support_i.supported(a_i) ) {
			
			firstSuppAction = a_i;
			break;
		    }
		}
		for( int a_i = 0; a_i < GameAction.NUM_ACTIONS; a_i++ ) {
				
		    if( !support_i.supported(a_i) || a_i == firstSuppAction )
			continue;
				
		    IloNumExpr eqExpr = nashCP.constant(0);
		    
		    //the joint actions of the other agents
		    //should also be supported
		    ArrayList<GameAction> othJntActList_afs = generateOtherJntActions(agent_i, suppProfile);
		    ArrayList<GameAction> othJntActList_a = generateOtherJntActions(agent_i, suppProfile);
		    
		    for( int listIndex = 0; listIndex < othJntActList_a.size(); listIndex++ ) {
			
			GameAction jntAction_a = othJntActList_a.get(listIndex);
			GameAction jntAction_afs = othJntActList_afs.get(listIndex);
			jntAction_a.setAction(agent_i, a_i);
			jntAction_afs.setAction(agent_i, firstSuppAction);
					
			double Q_sa = getQValue( agent_i, gameState, jntAction_a );
			double Q_sap = getQValue( agent_i, gameState, jntAction_afs );
			double coeff = Q_sa - Q_sap;
					
			//variable indices
			IloNumExpr itemExpr = nashCP.constant(coeff);
			for( int agent_j = 0; agent_j < SparseGridWorld.NUM_AGENTS; agent_j++ ) {
						
			    if( agent_j == agent_i )
				continue;
						
			    int act_j = jntAction_a.getAction( agent_j );
			    int varIndex_j = agent_j * GameAction.NUM_ACTIONS + act_j;
				
			    itemExpr = nashCP.prod( itemExpr, p[varIndex_j] );
			}
			eqExpr = nashCP.sum( eqExpr, itemExpr );
		    }
		    nashCP.addEq( eqExpr, 0.0 );
		}//loop for the supported actions of agent_i, except the firstSuppAction
	    }//loop for agent_i
	    
	    
	    
	    /**
	     * 5. all constraints have been set
	     * then we should solve this QCP
	     */
	    if( nashCP.solve() ) {
		
		solution = new double[varNum]; 
		nashCP.getValues( p, solution );
		
		//solution = nashCP.getValues( p );
		
		/**
		for( int index = 0; index < GridWorld.NUM_AGENTS * GameAction.NUM_ACTIONS; index++ ) {
		    
		    System.out.println("Solution "+index+": "+solution[index]);
		}
		*/
	    }
	    nashCP.end();
	    
	    //release the memory??
	    nashCP = null;
	}
	
	catch( IloException iloE ) {
	    
	    System.err.println("Concert exception '" + iloE + "' caught");
	}

	
	return solution;
    }
    
    
    /**
     * compute the union support of several supports
     */
    protected Support supportCup( ArrayList<Support> dom  )
    {
	/**
	 * check the parameter
	 */
	if( dom == null ) {
	    
	    System.out.println("@NashQ->supportCup: NULL Parameters!");
	    return null;
	}
	
	boolean[] suppArray = new boolean[GameAction.NUM_ACTIONS];
	for( int action = 0; action < GameAction.NUM_ACTIONS; action++ ) {
	    
	    suppArray[action] = false;
	    for( int i = 0; i < dom.size(); i++ ) {
		
		Support supp = dom.get(i);
		if( supp.supported(action) ) {
		    
		    suppArray[action] = true;
		    break;
		}
	    }   
	}
	
	return new Support( suppArray );
    }
    
    /**
     * sort the support size vector according to 
     * Algorithm 2 in paper "Simple search methods for finding a Nash equilibrium"
     * @return
     */
    public ArrayList<XVector> generateXVectors()
    {
	
	ArrayList<XVector> retList = new ArrayList<XVector>();
	
	/**
	 * first according to sum of x
	 * 3 agents, each agent has 4 actions
	 * for each agent, at least one actions has a probability larger than 0
	 */
	int minSumX = SparseGridWorld.NUM_AGENTS;
	int maxSumX = GameAction.NUM_ACTIONS * SparseGridWorld.NUM_AGENTS;
	
	for( int sumX = minSumX; sumX <= maxSumX; sumX++ ) {
	    
	    PriorityQueue<XVector> queue = generateXVectors( sumX );
	    
	    while( !queue.isEmpty()) {
		
		retList.add( queue.poll() );
	    }
	}
	
	
	return retList;
    }
    
    
    //done
    private PriorityQueue<XVector> generateXVectors( int sum ) 
    {
    	/**
    	 * note that the head of a PriorityQueue is the minimum element
    	 */
    	PriorityQueue<XVector> retQueue = new PriorityQueue<XVector>();
	
    	
    	/**
    	 * one for each agent's support size
    	 */
    	int agentNum = SparseGridWorld.NUM_AGENTS;
    	int[] xs = new int[agentNum];
    	for( int agent = 0; agent < agentNum; agent++ ) {
    		
    		xs[agent] = 1;
    	}
    	
    	/**
    	 * generate all possible support size vectors
    	 */
    	while( true ){
    		
    	    /**
    	     * check the current support size vector
    	     */
    	    int sizeSum = 0;
    	    for( int agent = 0; agent < agentNum; agent++ )
    		sizeSum += xs[agent];
    		
    	    //add the current support size vector to the queue
    	    if( sizeSum == sum ) {
    			
    		XVector xVector = new XVector( xs );
    			
    		if( !retQueue.contains( xVector ) )
    		    retQueue.add( xVector );
    		else
    		    xVector = null;
    	    }
    		
    	    /**
    	     * compute the next support size vector
    	     */
    	    for( int agent = agentNum-1; agent >= 0; agent-- ) {
				
    		/**
    		 * the action index of the current agent increase
    		 */
    		xs[agent] += 1;
				
    		if( agent > 0 && xs[agent] == GameAction.NUM_ACTIONS+1 ) {
					
    		    xs[agent] = 1;
					
    		    //then the next agent action should also increase
    		}
    		else
    		    break;
    	    }
			
    	    /**
    	     * whether to continue
    	     */
    	    if( xs[0] == GameAction.NUM_ACTIONS+1 )
    		break;
    	}
    	
    	return retQueue;
    }
    
    
    //done
    protected ArrayList<Support> generateSupportDomain( int supportSize )
    {
    	ArrayList<Support> retList = new ArrayList<Support>();
	
    	if( supportSize < 1 || supportSize > GameAction.NUM_ACTIONS ) {
	    
    	    System.out.println("@generateSupportDomain: Wrong Support Size!"); 
    	    return null;
    	}
	
    	
    	int[] flags = new int[GameAction.NUM_ACTIONS];
    	for( int act = 0; act < GameAction.NUM_ACTIONS; act++ )
    	    flags[act] = 0;
    	
    	
    	//Double Check
    	while( true ) {
    		
    	    /**
    	     * check the current support
    	     */
    	    int flagSum = 0;
    	    for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) 
    		flagSum += flags[act];
    		
    	    if( flagSum == supportSize ) {
    			
    		Support support = new Support( flags );
    		if( !retList.contains(support) )
    		    retList.add(support);
    		else
    		    support = null;
    	    }
    		
    	    /**
    	     * compute the next support
    	     */
    	    for( int act = GameAction.NUM_ACTIONS-1; act >= 0; act-- ) {
    			
    		flags[act] += 1;
    			
    		if( act > 0 && flags[act] > 1 ) {
    				
    		    flags[act] = 0; 
    		}
    		else
    		    break;
    	    }
    		
    		
    	    /**
    	     * whether to continue
    	     */
    	    if( flags[0] > 1 )
    		break;
    	}
	
    	return retList;
    }
    
    
    /**
     * generate the joint actions of the agents other than agent_i
     */
    private ArrayList<GameAction> generateOtherJntActions( int agent_i, 
	    Support[] suppProfile )
    {
    	
    	if( suppProfile == null || 
    		suppProfile.length != SparseGridWorld.NUM_AGENTS ) {
	    
    		System.out.println("@generateOtherJntActions: Wrong Parameter!");
    		return null;
    	}
    	
    	int agentNum = SparseGridWorld.NUM_AGENTS;
    	ArrayList<GameAction> retJntActions = new ArrayList<GameAction>();
    	
    	
    	//agents' actions for iteration
    	int[] agentActionsIter = new int[agentNum];
    	for( int agent = 0; agent < agentNum; agent++ )
    	    agentActionsIter[agent] = 0;
		
    	boolean cont = true;
    	while( cont ) {
			
    	    /**
    	     * the current partial joint action
    	     */
    	    GameAction jntAction = new GameAction( agentActionsIter );
			
    	    /**
    	     * add to the list according to the support profile
    	     */
    	    boolean bAdd = true;
    	    for( int agent = 0; agent < agentNum; agent++ ) {
				
    		if( agent == agent_i )
    		    continue;
    		if( !suppProfile[agent].supported( jntAction.getAction(agent)) ) {
					
    		    bAdd = false;
    		    break;
    		}
    	    }
    	    if( bAdd )
    		retJntActions.add( jntAction );
			
    	    /**
    	     * compute the next joint action
    	     */
    	    int last_agent = agentNum-1;
    	    int first_agent = 0;
    	    if( agent_i == 0 )
    		first_agent = 1;
    	    if( agent_i == agentNum-1 )
    		last_agent = agentNum-2;
    	    
    	    for( int agent_p = last_agent; agent_p >= first_agent; agent_p-- ) {
				
    		if( agent_p == agent_i )
    		    continue;
				
    		/**
    		 * the action index of the current agent increase
    		 */
    		agentActionsIter[agent_p] += 1;
				
    		if( agent_p > first_agent && agentActionsIter[agent_p] == GameAction.NUM_ACTIONS ) {
					
    		    agentActionsIter[agent_p] = 0;
					
    		    //then the next agent action should also increase
    		}
    		else
    		    break;
    	    }
			
    	    /**
    	     * whether to continue
    	     */
    	    if( agentActionsIter[first_agent] == GameAction.NUM_ACTIONS )
    		break;
    	}
		
    	return retJntActions;
    }
    
    /**
     * get the joint action according to a Nash equilibrium
     */
    protected GameAction getJointAction_NE( double[] nashE )
    {	
	
	GameAction retAction = new GameAction();
	
	if( nashE == null )
	{
	    //System.out.println("@CenNashQ->getJointAction_NE: NULL Nash Equilibrium!");
	    
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		
		retAction.setAction( agent, random.nextInt(GameAction.NUM_ACTIONS));
	    }
	}
	
	else {
	    
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		
		double probability = 0.0;
		
		double randPro = random.nextDouble();
		
		for( int action = 0; action < GameAction.NUM_ACTIONS; action++ ) {
		    
		    int compIndex = agent * GameAction.NUM_ACTIONS + action;
		    probability += nashE[compIndex];
		    
		    if( randPro < probability ) {
			
			retAction.setAction( agent, action );
			break;
		    }
		}
		
	    }
	}
	
	return retAction;
    }
    
}
