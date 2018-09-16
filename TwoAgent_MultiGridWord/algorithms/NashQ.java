package algorithms;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.cppimpl.IloEnv;
import ilog.cp.IloCP;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplexModeler;

import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLClientInfoException;
import java.util.ArrayList;
import java.util.PriorityQueue;

import multiGridWorld.GameAction;
import multiGridWorld.GameState;
import multiGridWorld.MultiGridWorld;

import drasys.or.Pair;

import help.Support;
import help.XVector;

public class NashQ extends MARL
{
    //The three members below are used for counting how many games are similar during learning
    //for counting the games in each state
    private double[][][] gameCounters;
    
    //for counting similar games in each state
    private double[][][] simiGameCounters;
    
    //the last Nash equilibrium in each state
    private double[][][][] lastEquilibrium;
    ////////////////////////////////////////////////////////////////////////////////////////////
    
    public NashQ()
    {
	super( 0 );
	
	/**
	 * init the Q-table
	 * for centralized algorithms 
	 * the Q-tables can be initialized randomly
	 */
	int locNum = MultiGridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	int worldNum = MultiGridWorld.WORLD_NUM;
	
	for( int agentIndex = 0; agentIndex < MultiGridWorld.NUM_AGENTS; agentIndex++ )
	    for( int wldIndex = 0; wldIndex < worldNum; wldIndex++ )
		for( int s1 = 0; s1 < locNum; s1++ )
		    for( int s2 = 0; s2 < locNum; s2++ )
			for( int a1 = 0; a1 < actionNum; a1++ )
			    for( int a2 = 0; a2 < actionNum; a2++ ) {
				    
				Qs[agentIndex][wldIndex][s1][s2][a1][a2] = random.nextDouble();//(Math.random() - 0.5) / 10.0;
			    }
	
	//init the three testing members
	gameCounters = new double[worldNum][locNum][locNum];
	simiGameCounters = new double[worldNum][locNum][locNum];
	lastEquilibrium = new double[worldNum][locNum][locNum][MultiGridWorld.NUM_AGENTS * actionNum];
	for( int wldIndex = 0;  wldIndex < worldNum; wldIndex++ )
	    for( int s1 = 0; s1 < locNum; s1++ )
		for( int s2 = 0; s2 < locNum; s2++ ) {
		
		    gameCounters[wldIndex][s1][s2] = simiGameCounters[wldIndex][s1][s2] = 0.0;
		
		    for( int act = 0; act < MultiGridWorld.NUM_AGENTS * actionNum; act++ )
			lastEquilibrium[wldIndex][s1][s2][act] = 0.0;
	    }
    }
    
    public NashQ( double alpha, double gamma, double epsilon )
    {
	super( 0, alpha, gamma, epsilon);
	
	/**
	 * init the Q-table
	 * for centralized algorithms 
	 * the Q-tables can be initialized randomly
	 */
	int locNum = MultiGridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	int worldNum = MultiGridWorld.WORLD_NUM;
	
	for( int agentIndex = 0; agentIndex < MultiGridWorld.NUM_AGENTS; agentIndex++ )
	    for( int wldIndex = 0; wldIndex < worldNum; wldIndex++ )
		for( int s1 = 0; s1 < locNum; s1++ )
		    for( int s2 = 0; s2 < locNum; s2++ )
			for( int a1 = 0; a1 < actionNum; a1++ )
			    for( int a2 = 0; a2 < actionNum; a2++ ) {
				    
				Qs[agentIndex][wldIndex][s1][s2][a1][a2] = random.nextDouble();//(Math.random() - 0.5) / 10.0;
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
	    
	    System.out.println("@CenNashQ->updateQ: NULL nextState!");
	    
	    return null;
	}
	else {
	    
	    /**
	     * compute the Nash equilibrium in the next state
	     */
	    double[] nashEquil = computeNE_2agent( agentIndex, nextState );//computeNE( agentIndex, nextState );
	    
	    /**
	    //the following code is for testing similar games during learning
	    //then compare the current equilibrium with the last one
	    int loc0 = nextState.getLocationID(0);
	    int loc1 = nextState.getLocationID(1);
	    int worldIndex = nextState.getWorldIndex();
	    if( gameCounters[worldIndex][loc0][loc1] > 0.01 ) {
		
		//distance of two Nash equilibrium
		double dis = 0.0;
		for( int act = 0; act < MultiGridWorld.NUM_AGENTS * GameAction.NUM_ACTIONS; act++ ) {
		    
		    double diff = lastEquilibrium[worldIndex][loc0][loc1][act] - nashEquil[act];
		    dis += diff * diff;
		    
		}
		dis = Math.sqrt(dis);
		
		double x = 0.01;//0.0000001;
		if( dis < x ) {   
		    simiGameCounters[worldIndex][loc0][loc1] += 1.0;
		}
	    }
	    gameCounters[worldIndex][loc0][loc1] += 1.0;
	    //store the current equilibrium
	    for( int act = 0; act < MultiGridWorld.NUM_AGENTS * GameAction.NUM_ACTIONS; act++ ) {
		    
		lastEquilibrium[worldIndex][loc0][loc1][act] = nashEquil[act];
	    }
	    */
	    //////////////////////////////////////////////////////////////////////
	    
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
		    
		for( int agent = 0; agent < MultiGridWorld.NUM_AGENTS; agent++ ) {
		    
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
	    
	    System.out.println("@CenNashQ->computeNE: NULL gameState!");
	    return null;
	}
	else if( agent < 0 || agent >= MultiGridWorld.NUM_AGENTS ) {
	    
	    System.out.println("CenNashQ->computeNE: Wrong agent Parameter!");
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
	    for( int j = 0; j < MultiGridWorld.NUM_AGENTS; j++ ) {
		
		domainProfile.add( generateSupportDomain(xVector.getX(j)) );
	    }
	    
	    Support[] supportProfile = new Support[MultiGridWorld.NUM_AGENTS];
	    for( int j = 0; j < MultiGridWorld.NUM_AGENTS; j++ ) {
		
		supportProfile[j] = null;
	    }
	    
	    
	    /**
	     * call the method recursiveBackTracking
	     */
	    double[] nashEquil = recursiveBackTracking( supportProfile, domainProfile, 0, gameState );
	    if( nashEquil != null )
		return nashEquil;
	}
	
	
	return null;
    }
    
    //check
    protected double[] computeNE_2agent( int agent, GameState gameState )
    {
	if( gameState == null ) {
	    
	    System.out.println("@NashQ->computeNE: NULL gameState!");
	    return null;
	}
	else if( agent < 0 || agent >= MultiGridWorld.NUM_AGENTS ) {
	    
	    System.out.println("NashQ->computeNE: Wrong agent Parameter!");
	    return null;
	}
	
	
	/**
	 * get all possible support size vector, sorted
	 * increasing order
	 * first order: the sum of the support size
	 * second order: the max difference of the support size
	 */
	ArrayList<XVector> xList = generateXVectors_2agent();
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
			    if( nashEquil != null )
				return nashEquil;
			}
		    }
		}
		
	    }
	}
	
	
	return null;
    }
    
    
    //whether the action act_i of agent_i is conditionally dominated 
    //given a support of the other agent agent_j
    protected boolean conditionallyDominated( int agent_i, int act_i, Support support_j, 
	    GameState gameState )
    {
	//parameter
	
	
	int agent_j = (agent_i+1) % MultiGridWorld.NUM_AGENTS;
	
	
	for( int act_ip = 0; act_ip < GameAction.NUM_ACTIONS; act_ip++ ) {
		
	    if( act_i == act_ip )
		continue;
		
	    //whether act_i is conditionally dominated by act_ip
	    boolean dominated = true;
	    for( int act_j = 0; act_j < GameAction.NUM_ACTIONS; act_j++ ) {
		    
		if( !support_j.supported( act_j ) )
		    continue;
		    
		GameAction jntAct_i = new GameAction();
		jntAct_i.setAction( agent_i, act_i );
		jntAct_i.setAction( agent_j, act_j );
		    
		GameAction jntAct_ip = new GameAction();
		jntAct_ip.setAction( agent_i,  act_ip );
		jntAct_ip.setAction( agent_j, act_j );
		    
		double valueAct_i = getQValue( agent_i, gameState, jntAct_i );
		double valueAct_ip = getQValue( agent_i, gameState, jntAct_ip );
		    
		if( valueAct_i >= valueAct_ip ) {
			
		    dominated = false;
		    break;
		}
	    }
		
	    if( dominated ) {
		    
		return true;
	    }
	}
		
	return false;
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
     * get the Q-values of a Nash equilibrium
     */
    protected double[] getNashQValues( GameState gameState, double[] nashE )
    {
	double[] values = new double[MultiGridWorld.NUM_AGENTS];
	for( int agent = 0; agent < MultiGridWorld.NUM_AGENTS; agent++ )
	    values[agent] = 0.0;
	
	if( nashE == null ) {
	    
	    //System.out.println("@CenNashQ->getNashQValues: NULL Nash Equilibrium!");
	    return values;
	}
	
	
	double[][][] qs = new double[MultiGridWorld.NUM_AGENTS][][];
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int worldIndex = gameState.getWorldIndex();
	
	qs[0] = Qs[0][worldIndex][loc0][loc1];
	qs[1] = Qs[1][worldIndex][loc0][loc1];
	
	for( int agent = 0; agent < MultiGridWorld.NUM_AGENTS; agent++ )
	    for( int i = 0; i < GameAction.NUM_ACTIONS; i++ ) {
		
		int agent0 = 0;
		double p_i = nashE[agent0*GameAction.NUM_ACTIONS+i];
		
		for( int j = 0; j < GameAction.NUM_ACTIONS; j++ ) {
		    
		    int agent1 = 1;
		    double p_j = nashE[agent1*GameAction.NUM_ACTIONS+j];
		    
		    double jntPro = p_i * p_j;
		    values[agent] += jntPro * qs[agent][i][j];
		}
	    }
	
	return values;
    }
    
    
    
    //Not used
    protected double[] recursiveBackTracking( Support[] suppProfile, 
	    ArrayList<ArrayList<Support>> domProfile, int index, GameState gameState )
    {
	
	/**
	 * check parameters
	 */
	
	if( index == MultiGridWorld.NUM_AGENTS ) {
	    
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
		for( int i = index+1; i < MultiGridWorld.NUM_AGENTS; i++ ) {
		    
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
    
    
    //Not used
    private boolean IRSDS( ArrayList<ArrayList<Support>> domPro, GameState gameState )
    {
	/**
	 * check the parameter
	 */
	if( domPro == null || gameState == null ||
		domPro.size() != MultiGridWorld.NUM_AGENTS ) {
	    
	    System.out.println("@CenNashQ->IRSDS: Wrong Parameters!");
	    
	    return false;
	}
	
	boolean changed = true;
	while( changed ) {
	    
	    changed = false;
	    
	    /**
	     * loop for all agent index
	     */
	    for( int agent_i = 0; agent_i < MultiGridWorld.NUM_AGENTS; agent_i++ ) {
		
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
			Support[] otherCups = new Support[MultiGridWorld.NUM_AGENTS];
			for( int j = 0; j < MultiGridWorld.NUM_AGENTS; j++ ) {
			    
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
			ArrayList<GameAction> jntActions_a = new ArrayList<GameAction>();
			ArrayList<GameAction> jntActions_ap = new ArrayList<GameAction>();
			for( int a1 = 0; a1 < GameAction.NUM_ACTIONS; a1++ ) 
			    for( int a2 = 0; a2 < GameAction.NUM_ACTIONS; a2++ ) {
				
				//assign the actions, quite troublesome
				int[] jA_a = new int[MultiGridWorld.NUM_AGENTS];
				int[] jA_ap = new int[MultiGridWorld.NUM_AGENTS];
				int agent_j = (agent_i+1) % MultiGridWorld.NUM_AGENTS;
				int agent_k = (agent_i+2) % MultiGridWorld.NUM_AGENTS;
				jA_a[agent_i] = a_i;
				jA_a[agent_j] = a1;
				jA_a[agent_k] = a2; 
				jA_ap[agent_i] = ap_i;
				jA_ap[agent_j] = a1;
				jA_ap[agent_k] = a2;
				////////////////////////////////////////////
				
				boolean unSupported = false;
				for( int agentIndex = 0; agentIndex < MultiGridWorld.NUM_AGENTS; agentIndex++ ) {
				    
				    if( agentIndex == agent_i )
					continue;
				    if( !otherCups[agentIndex].supported( jA_a[agentIndex] ) ) {
					
					unSupported = true;
					break;
				    }
				}
				if( unSupported ) {
				    
				    jA_a = null;
				    jA_ap = null;
				}
				else {
				    
				    GameAction aAction = new GameAction( jA_a );
				    GameAction apAction = new GameAction( jA_ap );
				    jntActions_a.add( aAction );
				    jntActions_ap.add( apAction );
				}
			    }
			
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
			for( int jntIndex = 0; jntIndex < jntActions_a.size(); jntIndex++ ) {
			    
			    GameAction aAction = jntActions_a.get( jntIndex );
			    GameAction apAction = jntActions_ap.get( jntIndex );
			    
			    double Qi_a = getQValue( agent_i, gameState, aAction );
			    double Qi_ap = getQValue( agent_i, gameState, apAction );
			    
			    if( Qi_a >= Qi_ap ) {
				
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
    
    //Check again
    protected double[] feasibilityProgram( Support[] suppProfile, GameState gameState )
    {
	if( suppProfile == null || 
		suppProfile.length != MultiGridWorld.NUM_AGENTS ) {
	    
	    System.out.println("@CenNashQ->feasibilityProgram: Wrong Parameter!");
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
	    IloCplex  nashLCP = new IloCplex();
	    //IloCP nashLCP = new IloCP();
	    
	    //IloEnv env = new IloEnv();
	    
	    /**
	     * 2. secondly, create the variables:
	     * stores each agent's probability of taking each action
	     * agent i's j-th action probability: index = i * Num_Actions
	     */
	    double[] lowBounds = new double[MultiGridWorld.NUM_AGENTS * GameAction.NUM_ACTIONS];
	    double[] uppBounds = new double[MultiGridWorld.NUM_AGENTS * GameAction.NUM_ACTIONS];
	    for( int varIndex = 0; varIndex < MultiGridWorld.NUM_AGENTS * GameAction.NUM_ACTIONS; varIndex++ ) {
		
		lowBounds[varIndex] = 0.0;
		uppBounds[varIndex] = 1.0;
	    }
	    IloNumVar[] p = nashLCP.numVarArray(MultiGridWorld.NUM_AGENTS*GameAction.NUM_ACTIONS, lowBounds, uppBounds);
	    
	    
	    /**
	     * 3. then create the constraints:
	     */
	    
	    //3.1 for the sum of each agent's action probability
	    //\Sum_{a_i \in S_i} p_i(a_i) = 1
	    for( int agent_i = 0; agent_i < MultiGridWorld.NUM_AGENTS; agent_i++ ) {
		
		Support supp_i = suppProfile[agent_i];
		double[] coeff = new double[MultiGridWorld.NUM_AGENTS * GameAction.NUM_ACTIONS];
		for( int coeffIndex = 0; coeffIndex < MultiGridWorld.NUM_AGENTS*GameAction.NUM_ACTIONS; coeffIndex++ ) {
		    
		    int agent_j = coeffIndex / GameAction.NUM_ACTIONS;
		    int act = coeffIndex % GameAction.NUM_ACTIONS;
		    
		    if( agent_j != agent_i )
			coeff[coeffIndex] = 0.0;
		    else if( !supp_i.supported(act) )
			coeff[coeffIndex] = 0.0;
		    else
			coeff[coeffIndex] = 1.0;
		}
		
		IloNumExpr sumExpr = nashLCP.scalProd(coeff, p);
		nashLCP.addEq( sumExpr, 1.0 );
	    }
	    
	    //3.2 for each agent's unsupported action
	    //for any a_i \not\in S_i p_i(a_i) = 0
	    for( int agent_i = 0; agent_i < MultiGridWorld.NUM_AGENTS; agent_i++ ) {
		
		Support supp_i = suppProfile[agent_i];
		if( supp_i.supportSize() == GameAction.NUM_ACTIONS )
		    continue;
		
		for( int act = 0;  act < GameAction.NUM_ACTIONS; act++ ) {
		    
		    //should be !supported?
		    if( !supp_i.supported( act ) ) {
			
			double[] coeff = new double[MultiGridWorld.NUM_AGENTS * GameAction.NUM_ACTIONS];
			for( int coeffIndex = 0; coeffIndex < MultiGridWorld.NUM_AGENTS*GameAction.NUM_ACTIONS; coeffIndex++ ) {
			    
			    if( coeffIndex == (agent_i * GameAction.NUM_ACTIONS + act) )
				coeff[coeffIndex] = 1.0;
			    else
				coeff[coeffIndex] = 0.0;
			}
			
			IloNumExpr eqzeroExpr = nashLCP.scalProd( coeff, p );
			nashLCP.addEq( eqzeroExpr, 0.0 );
		    }
		}
	    }
	    
	    //3.3 for the nash condition inequality
	    //for any i \in N, any a_i \in S_i and any a_i' \not\in S_i
	    //\Sum_{a_{-i} \in S_{-i}} p(a_{-i}[u_i(a_i,a_{-i})-u_i(a_i',a_{-i})] >= 0
	    for( int agent_i = 0; agent_i < MultiGridWorld.NUM_AGENTS; agent_i++ ) {
		    
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
			IloNumExpr ineqExpr = nashLCP.constant(0);
			for( int act_j = 0; act_j < GameAction.NUM_ACTIONS; act_j++ ) {
				
			    int agent_j = (agent_i+1) % MultiGridWorld.NUM_AGENTS;

			    if( !suppProfile[agent_j].supported( act_j ) )
				continue;
			    
			    //locate the variable index
			    int varIndex_j = agent_j * GameAction.NUM_ACTIONS + act_j;
				
			    //obtain the coefficients
			    GameAction jntAction_ai = new GameAction();
			    jntAction_ai.setAction(agent_i, a_i);
			    jntAction_ai.setAction(agent_j, act_j);
				
			    GameAction jntAction_api = new GameAction();
			    jntAction_api.setAction(agent_i, ap_i);
			    jntAction_api.setAction(agent_j, act_j);
				
			    double coeff = getQValue(agent_i, gameState, jntAction_ai) - 
			    	getQValue(agent_i, gameState, jntAction_api);
				
			    IloNumExpr itemExpr = nashLCP.prod( coeff, p[varIndex_j] );
				
			    //add the item to the inequality expression
			    ineqExpr = nashLCP.sum( ineqExpr, itemExpr );
			}
			/**
			 * create the inequality constraint
			 * 
			 * be careful! should be greater than 0 
			 * not less than 0
			 */
			nashLCP.addGe( ineqExpr, 0.0 );
		    }
		}
		
		
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
				
		    IloNumExpr eqExpr = nashLCP.constant(0);
		    for( int act_j = 0; act_j < GameAction.NUM_ACTIONS; act_j++ ) {
					
			int agent_j = (agent_i+1) % MultiGridWorld.NUM_AGENTS;

			if( !suppProfile[agent_j].supported( act_j ) )
			    continue;
			
			//locate the variable index
			int varIndex_j = agent_j * GameAction.NUM_ACTIONS + act_j;
			
			//obtain the coefficients
			GameAction jntAction_ai = new GameAction();
			jntAction_ai.setAction(agent_i, a_i);
			jntAction_ai.setAction(agent_j, act_j);
			
			GameAction jntAction_afs = new GameAction();
			jntAction_afs.setAction(agent_i, firstSuppAction);
			jntAction_afs.setAction(agent_j, act_j);
			
			double coeff = getQValue(agent_i, gameState, jntAction_ai) - 
					getQValue(agent_i, gameState, jntAction_afs);
			
			IloNumExpr itemExpr = nashLCP.prod( coeff, p[varIndex_j] );
		
			//add the item to the inequality expression
			eqExpr = nashLCP.sum( eqExpr, itemExpr );
		    }
		    nashLCP.addEq( eqExpr, 0.0 );
		}
	    }
	    
	    
	    /**
	     * 4. set the objective function
	     * since there is not an objective function in this feasibility program 
	     * set a null objective function??
	     */
	    
	    
	    /**
	     * all constraints have been set
	     * then we should solve this QCP
	     */
	    
	    if( nashLCP.solve() ) {
		
		//solution = new double[GridWorld.NUM_AGENTS * GameAction.NUM_ACTIONS];
		//nashLCP.getValues( p, solution );
		
		solution = nashLCP.getValues( p );
		
		/**
		for( int index = 0; index < GridWorld.NUM_AGENTS * GameAction.NUM_ACTIONS; index++ ) {
		    
		    System.out.println("Solution "+index+": "+solution[index]);
		}
		*/
	    }
	    nashLCP.end();
	}
	
	catch( IloException iloE ) {
	    
	    System.err.println("Concert exception '" + iloE + "' caught");
	}

	
	return solution;
    }
    
    /**
     * compute the union support of two supports
     */
    private Support supportCup( Support supp1, Support supp2 )
    {
	/**
	 * check the parameter
	 */
	if( supp1 == null || supp2 == null ) {
	    
	    System.out.println("@CenNashQ->supportCup: NULL Parameters!");
	    return null;
	}
	
	boolean[] suppArray = new boolean[GameAction.NUM_ACTIONS];
	for( int action = 0; action < GameAction.NUM_ACTIONS; action++ ) {
	    
	    if( supp1.supported(action) || supp2.supported(action) )
		suppArray[action] = true;
	    else
		suppArray[action] = false; 
	}
	
	return new Support( suppArray );
    }
    
    /**
     * compute the union support of several supports
     */
    private Support supportCup( ArrayList<Support> dom  )
    {
	/**
	 * check the parameter
	 */
	if( dom == null ) {
	    
	    System.out.println("@CenNashQ->supportCup: NULL Parameters!");
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
	int minSumX = 2;
	int maxSumX = 8;
	
	for( int sumX = minSumX; sumX <= maxSumX; sumX++ ) {
	    
	    PriorityQueue<XVector> queue = generateXVectors( sumX );
	    
	    while( !queue.isEmpty()) {
		
		retList.add( queue.poll() );
	    }
	}
	
	for( int i = 0; i < retList.size(); i++ ) {
	    
	    XVector xVector = retList.get(i);
	    
	    //System.out.println("Sum "+xVector.sum()+" maxDiff "+xVector.maxDiff());
	}
	
	return retList;
    }
    
    public ArrayList<XVector> generateXVectors_2agent()
    {
	
	ArrayList<XVector> retList = new ArrayList<XVector>();
	
	
	/**
	 * first according to difference of x
	 * 2 agents, each agent has 4 actions
	 * for each agent, at least one actions has a probability larger than 0
	 */
	int minDiffX = 0;
	int maxDiffX = 3;
	
	for( int diffX = maxDiffX; diffX >= minDiffX; diffX-- ) {
	//for( int diffX = minDiffX; diffX <= maxDiffX; diffX++ ) {
	    
	    PriorityQueue<XVector> queue = generateXVectors_2agent( diffX );
	    
	    while( !queue.isEmpty()) {
		
		retList.add( queue.poll() );
	    }
	}
	
	/**
	for( int i = 0; i < retList.size(); i++ ) {
	    
	    XVector xVector = retList.get(i);
	    
	    //System.out.println("Sum "+xVector.sum()+" maxDiff "+xVector.maxDiff());
	}
	*/
	
	return retList;
    }
    
    protected PriorityQueue<XVector> generateXVectors_2agent( int diff ) 
    {
	/**
	 * note that the head of a PriorityQueue is the minimum element
	 */
	PriorityQueue<XVector> retQueue = new PriorityQueue<XVector>();
	
	/**
	 * generate all possible support size vectors
	 */
	for( int x1 = 1; x1 <= GameAction.NUM_ACTIONS; x1++ ) {
	    

	    for( int x2 = 1; x2 <= GameAction.NUM_ACTIONS; x2++ ) {
		
		if( (x1-x2 == diff) || (x2-x1 == diff) ) {
			
		    XVector xVector = new XVector( new int[]{x1, x2} );
		    if( !retQueue.contains( xVector ) )
			retQueue.add( xVector );
		    else
			xVector = null;
			
		    break;
		}
	    }
	}
	
	
	return retQueue;
    }
    
    private PriorityQueue<XVector> generateXVectors( int sum ) 
    {
	/**
	 * note that the head of a PriorityQueue is the minimum element
	 */
	PriorityQueue<XVector> retQueue = new PriorityQueue<XVector>();
	
	/**
	 * generate all possible support size vectors
	 */
	for( int x1 = 1; x1 <= GameAction.NUM_ACTIONS; x1++ ) {
	    
	    if( x1 > sum )
		break;

	    for( int x2 = 1; x2 <= GameAction.NUM_ACTIONS; x2++ ) {
		
		if( x1+x2 > sum )
		    break;

		if( x1+x2 == sum ) {
			
		    XVector xVector = new XVector( new int[]{x1, x2} );
		    if( !retQueue.contains( xVector ) )
			retQueue.add( xVector );
		    else
			xVector = null;
			
		    break;
		}
	    }
	}
	
	
	return retQueue;
    }
    
    
    protected ArrayList<Support> generateSupportDomain( int supportSize )
    {
	ArrayList<Support> retList = new ArrayList<Support>();
	
	if( supportSize < 1 || supportSize > GameAction.NUM_ACTIONS ) {
	    
	    System.out.println("@CenNashQ->generateSupportDomain: Wrong Support Size!");
	}
	
	
	for( int b1 = 0; b1 <= 1; b1++ ) {
	    
	    for( int b2 = 0; b2 <= 1; b2++ ) {
		
		for( int b3 = 0; b3 <= 1; b3++ ) {
		    
		    for( int b4 = 0; b4 <= 1; b4++ ) {
			
			if( (b1+b2+b3+b4) == supportSize ) {
			    
			    Support support = new Support( new int[]{b1,b2,b3,b4});
			    
			    if( !retList.contains(support) )
				retList.add(support);
			    else
				support = null;
			}
		    }
		}
	    }
	}
	
	return retList;
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
	    
	    retAction.setAction(0, random.nextInt(GameAction.NUM_ACTIONS));
	    retAction.setAction(1, random.nextInt(GameAction.NUM_ACTIONS));
	}
	
	else {
	    
	    double[] probabilities = new double[GameAction.NUM_ACTIONS];
	    
	    /**
	     * choose agent0's action
	     */
	    int agent0 = 0;
	    probabilities[0] = nashE[agent0*GameAction.NUM_ACTIONS+0];
	    for( int a0 = 1; a0 < GameAction.NUM_ACTIONS; a0++ ) {
		
		probabilities[a0] = probabilities[a0-1] + nashE[agent0*GameAction.NUM_ACTIONS+a0]; 
	    }
	    double d = random.nextDouble();
	    int action0 = 0;
	    for( int a0 = 0; a0 < GameAction.NUM_ACTIONS; a0++ ) {
		
		if( d < probabilities[a0] ) {
		    
		    action0 = a0;
		    break;
		}
	    }
	    
	    /**
	     * choose agent1's action
	     */
	    int agent1 = 1;
	    probabilities[0] = nashE[agent1*GameAction.NUM_ACTIONS+0];
	    for( int a1 = 1; a1 < GameAction.NUM_ACTIONS; a1++ ) {
		
		probabilities[a1] = probabilities[a1-1] + nashE[agent1*GameAction.NUM_ACTIONS+a1]; 
	    }
	    d = random.nextDouble();
	    int action1 = 0;
	    for( int a1 = 0; a1 < GameAction.NUM_ACTIONS; a1++ ) {
		
		if( d < probabilities[a1] ) {
		    
		    action1 = a1;
		    break;
		}
	    }
	    
	    retAction.setAction(0, action0);
	    retAction.setAction(1, action1);
	}
	
	return retAction;
    }
    
    
    //record some data for analysis
    /**
     * 
    public void gameFinished()
    {
	try {
	    
	    BufferedWriter writer = new BufferedWriter(new FileWriter("./NashQ_analysis.txt"));
	    
	    int locNum = GridWorld.NUM_LOCATIONS;
	    for( int s0 = 0; s0 < locNum; s0++ )
		for( int s1 = 0; s1 < locNum; s1++ ) {
		    
		    String line = "("+s0+","+s1+"): ";
		    line += "simiGame: "+String.valueOf((int) simiGameCounters[s0][s1]);
		    line += " Game: "+ String.valueOf((int) gameCounters[s0][s1]);
		    
		    if( !(gameCounters[s0][s1] < 0.01) ) {
			
			double rate = simiGameCounters[s0][s1] / gameCounters[s0][s1];
			
			line += " Rate: "+rate;
		    }
		    
		    writer.write( line );
		    writer.newLine();
		}
	    
	    writer.close();
	}
	catch(IOException e) {
	    
	    e.printStackTrace();
	}
    }
    */
    
}
