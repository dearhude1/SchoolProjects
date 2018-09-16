package algorithms;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cp.IloCP;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.PriorityQueue;

import drasys.or.mp.Problem;

import gameGridWorld.GameAction;
import gameGridWorld.GameState;
import gameGridWorld.GridWorld;
import help.Support;
import help.XVector;

public class NashQ extends MARL
{
    
    
    public NashQ()
    {
	super( 0 );
	
	/**
	 * init the Q-table
	 * for centralized algorithms 
	 * the Q-tables can be initialized randomly
	 */
	int locNum = GridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;

	for( int agentIndex = 0; agentIndex < GridWorld.NUM_AGENTS; agentIndex++ )
	    for( int s1 = 0; s1 < locNum; s1++ )
		for( int s2 = 0; s2 < locNum; s2++ )
		    for(int s3 = 0; s3 < locNum; s3++ )
			for( int a1 = 0; a1 < actionNum; a1++ )
			    for( int a2 = 0; a2 < actionNum; a2++ )
				for(int a3 = 0; a3 < actionNum; a3++ ) {
				    
				    Qs[agentIndex][s1][s2][s3][a1][a2][a3] = (Math.random() - 0.5) / 10.0;
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
	int locNum = GridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;

	for( int agentIndex = 0; agentIndex < GridWorld.NUM_AGENTS; agentIndex++ )
	    for( int s1 = 0; s1 < locNum; s1++ )
		for( int s2 = 0; s2 < locNum; s2++ )
		    for(int s3 = 0; s3 < locNum; s3++ )
			for( int a1 = 0; a1 < actionNum; a1++ )
			    for( int a2 = 0; a2 < actionNum; a2++ )
				for(int a3 = 0; a3 < actionNum; a3++ ) {
				    
				    Qs[agentIndex][s1][s2][s3][a1][a2][a3] = (Math.random() - 0.5) / 10.0;
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
	     * compute the correlated equilibrium in the next state
	     */
	    double[] nashEquil = computeNE( agentIndex, nextState );
	    
	    
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
		    
		    /**
		     * updating rule
		     */
		    Qsa = (1 - ALPHA) * Qsa + ALPHA * (rewards[agent] + GAMMA * nashQValues[agent]);
		    
		    
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
	else if( agent < 0 || agent >= GridWorld.NUM_AGENTS ) {
	    
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
	    for( int j = 0; j < GridWorld.NUM_AGENTS; j++ ) {
		
		domainProfile.add( generateSupportDomain(xVector.getX(j)) );
	    }
	    
	    Support[] supportProfile = new Support[GridWorld.NUM_AGENTS];
	    for( int j = 0; j < GridWorld.NUM_AGENTS; j++ ) {
		
		supportProfile[j] = null;
	    }
	    
	    
	    /**
	     * call the method recursiveBackTracking
	     */
	    double[] nashEquil = recursiveBackTracking( supportProfile, domainProfile, 0, gameState );
	    if( nashEquil != null )
		return nashEquil;
	    
	    //release memory
	    domainProfile = null;
	    supportProfile = null;
	}
	
	
	return null;
    }
    
    
    /**
     * get the Q-values of a Nash equilibrium
     */
    protected double[] getNashQValues( GameState gameState, double[] nashE )
    {
	double[] values = new double[GridWorld.NUM_AGENTS * GameAction.NUM_ACTIONS];
	for( int i = 0; i < GridWorld.NUM_AGENTS * GameAction.NUM_ACTIONS; i++ )
	    values[i] = 0.0;
	
	if( nashE == null ) {
	    
	    //System.out.println("@CenNashQ->getNashQValues: NULL Nash Equilibrium!");
	    return values;
	}
	
	
	double[][][][] qs = new double[GridWorld.NUM_AGENTS][][][];
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int loc2 = gameState.getLocationID(2);
	
	qs[0] = Qs[0][loc0][loc1][loc2];
	qs[1] = Qs[1][loc0][loc1][loc2];
	qs[2] = Qs[2][loc0][loc1][loc2];
	
	for( int agent = 0; agent < GridWorld.NUM_AGENTS; agent++ )
	    for( int i = 0; i < GameAction.NUM_ACTIONS; i++ ) {
		
		int agent0 = 0;
		double p_i = nashE[agent0*GameAction.NUM_ACTIONS+i];
		
		for( int j = 0; j < GameAction.NUM_ACTIONS; j++ ) {
		    
		    int agent1 = 1;
		    double p_j = nashE[agent1*GameAction.NUM_ACTIONS+j];
		    
		    for( int k = 0; k < GameAction.NUM_ACTIONS; k++ )
		    {
			int agent2 = 2;
			double p_k = nashE[agent2*GameAction.NUM_ACTIONS+k];
			
			double jntPro = p_i * p_j * p_k;
			values[agent] += jntPro * qs[agent][i][j][k];
		    }
		}
	    }
	
	return values;
    }
    
    
    protected double[] recursiveBackTracking( Support[] suppProfile, 
	    ArrayList<ArrayList<Support>> domProfile, int index, GameState gameState )
    {
	
	/**
	 * check parameters
	 */
	
	if( index == GridWorld.NUM_AGENTS ) {
	    
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
		for( int i = index+1; i < GridWorld.NUM_AGENTS; i++ ) {
		    
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
    
    
    //Check again
    private boolean IRSDS( ArrayList<ArrayList<Support>> domPro, GameState gameState )
    {
	/**
	 * check the parameter
	 */
	if( domPro == null || gameState == null ||
		domPro.size() != GridWorld.NUM_AGENTS ) {
	    
	    System.out.println("@CenNashQ->IRSDS: Wrong Parameters!");
	    
	    return false;
	}
	
	boolean changed = true;
	while( changed ) {
	    
	    changed = false;
	    
	    /**
	     * loop for all agent index
	     */
	    for( int agent_i = 0; agent_i < GridWorld.NUM_AGENTS; agent_i++ ) {
		
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
			Support[] otherCups = new Support[GridWorld.NUM_AGENTS];
			for( int j = 0; j < GridWorld.NUM_AGENTS; j++ ) {
			    
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
				int[] jA_a = new int[GridWorld.NUM_AGENTS];
				int[] jA_ap = new int[GridWorld.NUM_AGENTS];
				int agent_j = (agent_i+1) % GridWorld.NUM_AGENTS;
				int agent_k = (agent_i+2) % GridWorld.NUM_AGENTS;
				jA_a[agent_i] = a_i;
				jA_a[agent_j] = a1;
				jA_a[agent_k] = a2; 
				jA_ap[agent_i] = ap_i;
				jA_ap[agent_j] = a1;
				jA_ap[agent_k] = a2;
				////////////////////////////////////////////
				
				boolean unSupported = false;
				for( int agentIndex = 0; agentIndex < GridWorld.NUM_AGENTS; agentIndex++ ) {
				    
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
    private double[] feasibilityProgram( Support[] suppProfile, GameState gameState )
    {
	if( suppProfile == null || 
		suppProfile.length != GridWorld.NUM_AGENTS ) {
	    
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
	    //IloCplex  nashQCP = new IloCplex();
	    IloCP nashQCP = new IloCP();

	    
	    /**
	     * 2. secondly, create the variables:
	     * stores each agent's probability of taking each action
	     * agent i's j-th action probability: index = i * Num_Actions
	     */
	    double[] lowBounds = new double[GridWorld.NUM_AGENTS * GameAction.NUM_ACTIONS];
	    double[] uppBounds = new double[GridWorld.NUM_AGENTS * GameAction.NUM_ACTIONS];
	    for( int varIndex = 0; varIndex < GridWorld.NUM_AGENTS * GameAction.NUM_ACTIONS; varIndex++ ) {
		
		lowBounds[varIndex] = 0.0;
		uppBounds[varIndex] = 1.0;
	    }
	    IloNumVar[] p = nashQCP.numVarArray(GridWorld.NUM_AGENTS*GameAction.NUM_ACTIONS, lowBounds, uppBounds);
	    
	    
	    /**
	     * 3. then create the constraints:
	     * IloRange class for range constraints
	     */
	    
	    //3.1 for the sum of each agent's action probability
	    //\Sum_{a_i \in S_i} p_i(a_i) = 1
	    for( int agent_i = 0; agent_i < GridWorld.NUM_AGENTS; agent_i++ ) {
		
		Support supp_i = suppProfile[agent_i];
		double[] coeff = new double[GridWorld.NUM_AGENTS * GameAction.NUM_ACTIONS];
		for( int coeffIndex = 0; coeffIndex < GridWorld.NUM_AGENTS*GameAction.NUM_ACTIONS; coeffIndex++ ) {
		    
		    int agent_j = coeffIndex / GameAction.NUM_ACTIONS;
		    int act = coeffIndex % GameAction.NUM_ACTIONS;
		    
		    if( agent_j != agent_i )
			coeff[coeffIndex] = 0.0;
		    else if( !supp_i.supported(act) )
			coeff[coeffIndex] = 0.0;
		    else
			coeff[coeffIndex] = 1.0;
		}
		
		IloNumExpr sumExpr = nashQCP.scalProd(coeff, p);
		nashQCP.addEq( sumExpr, 1.0 );
	    }
	    
	    //3.2 for each agent's unsupported action
	    //for any a_i \not\in S_i p_i(a_i) = 0
	    for( int agent_i = 0; agent_i < GridWorld.NUM_AGENTS; agent_i++ ) {
		
		Support supp_i = suppProfile[agent_i];
		if( supp_i.supportSize() == GameAction.NUM_ACTIONS )
		    continue;
		
		for( int act = 0;  act < GameAction.NUM_ACTIONS; act++ ) {
		    
		    if( !supp_i.supported( act ) ) {
			
			double[] coeff = new double[GridWorld.NUM_AGENTS * GameAction.NUM_ACTIONS];
			for( int coeffIndex = 0; coeffIndex < GridWorld.NUM_AGENTS*GameAction.NUM_ACTIONS; coeffIndex++ ) {
			    
			    if( coeffIndex == (agent_i * GameAction.NUM_ACTIONS + act) )
				coeff[coeffIndex] = 1.0;
			    else
				coeff[coeffIndex] = 0.0;
			}
			
			IloNumExpr eqzeroExpr = nashQCP.scalProd( coeff, p );
			nashQCP.addEq( eqzeroExpr, 0.0 );
		    }
		}
	    }
	    
	    //3.3 for the nash condition inequality
	    //for any i \in N, any a_i \in S_i and any a_i' \not\in S_i
	    //\Sum_{a_{-i} \in S_{-i}} p(a_{-i}[u_i(a_i,a_{-i})-u_i(a_i',a_{-i})] <= 0
	    for( int agent_i = 0; agent_i < GridWorld.NUM_AGENTS; agent_i++ ) {
		    
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
			 * loop for the joint actions of the other agents
			 */
			IloNumExpr ineqExpr = nashQCP.constant(0);
			for( int act_j = 0; act_j < GameAction.NUM_ACTIONS; act_j++ ) {
			    
			    int agent_j = (agent_i+1) % GridWorld.NUM_AGENTS;
			    int varIndex_j = agent_j * GameAction.NUM_ACTIONS + act_j;
			    if( !suppProfile[agent_j].supported( act_j ) )
				continue;
			    
			    for( int act_k = 0; act_k < GameAction.NUM_ACTIONS; act_k++ ) {
				
				
				int agent_k = (agent_i+2) % GridWorld.NUM_AGENTS;
				int varIndex_k = agent_k * GameAction.NUM_ACTIONS + act_k;
				if( !suppProfile[agent_k].supported( act_k ) )
				    continue;
				
				//obtain the coefficients
				GameAction jntAction_ai = new GameAction();
				jntAction_ai.setAction(agent_i, a_i);
				jntAction_ai.setAction(agent_j, act_j);
				jntAction_ai.setAction(agent_k, act_k);
				GameAction jntAction_api = new GameAction();
				jntAction_api.setAction(agent_i, ap_i);
				jntAction_api.setAction(agent_j, act_j);
				jntAction_api.setAction(agent_k, act_k);
				double coeff = getQValue(agent_i, gameState, jntAction_ai) - 
						getQValue(agent_i, gameState, jntAction_api);
				
				IloNumExpr itemExpr = nashQCP.prod( coeff, p[varIndex_j], p[varIndex_k] );
				
				//add the item to the inequality expression
				ineqExpr = nashQCP.sum( ineqExpr, itemExpr );
			    }
			}
			/**
			 * create the inequality constraint
			 */
			nashQCP.addGe( ineqExpr, 0 );
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
				
		    IloNumExpr eqExpr = nashQCP.constant(0);
		    for( int act_j = 0; act_j < GameAction.NUM_ACTIONS; act_j++ ) {
			
			int agent_j = (agent_i+1) % GridWorld.NUM_AGENTS;
			int varIndex_j = agent_j * GameAction.NUM_ACTIONS + act_j;
			if( !suppProfile[agent_j].supported( act_j ) )
			    continue;
			
		    	for( int act_k = 0; act_k < GameAction.NUM_ACTIONS; act_k++ ) {
					
		    	    
		    	    int agent_k = (agent_i+2) % GridWorld.NUM_AGENTS;
		    	    int varIndex_k = agent_k * GameAction.NUM_ACTIONS + act_k;
		    	    if( !suppProfile[agent_k].supported( act_k ) )
		    		continue;
		    	    
		    	    //obtain the coefficients
		    	    GameAction jntAction_ai = new GameAction();
		    	    jntAction_ai.setAction(agent_i, a_i);
		    	    jntAction_ai.setAction(agent_j, act_j);
		    	    jntAction_ai.setAction(agent_k, act_k);
			
		    	    GameAction jntAction_afs = new GameAction();
		    	    jntAction_afs.setAction(agent_i, firstSuppAction);
		    	    jntAction_afs.setAction(agent_j, act_j);
		    	    jntAction_afs.setAction(agent_k, act_k);
			
		    	    double coeff = getQValue(agent_i, gameState, jntAction_ai) - 
		    	    	getQValue(agent_i, gameState, jntAction_afs);
			
		    	    IloNumExpr itemExpr = nashQCP.prod( coeff, p[varIndex_j], p[varIndex_k] );
		
		    	    //add the item to the inequality expression
		    	    eqExpr = nashQCP.sum( eqExpr, itemExpr );
		    	}
		    }
		    nashQCP.addEq( eqExpr, 0.0 );
		}
	    }
	    
	    
	    /**
	     * 4. set the objective function
	     * since there is not an objective function in this feasibility program 
	     * set a null objective function??
	     *
	    double[] coeffs = new double[GridWorld.NUM_AGENTS*GameAction.NUM_ACTIONS];
	    for( int coeffIndex = 0; coeffIndex < GridWorld.NUM_AGENTS * GameAction.NUM_ACTIONS; coeffIndex++ )
		coeffs[coeffIndex] = 1.0;
	    nashQCP.add( nashQCP.maximize( nashQCP.scalProd( coeffs, p) ) );
	    */
	    
	    /**
	     * all constraints have been set
	     * then we should solve this QCP
	     */
	    if( nashQCP.solve() ) {
		
		solution = new double[GridWorld.NUM_AGENTS * GameAction.NUM_ACTIONS];
		nashQCP.getValues( p, solution );
		
		System.out.println("Solution Length"+solution.length);
		
		for( int neIndex = 0; neIndex < GridWorld.NUM_AGENTS*GameAction.NUM_ACTIONS; neIndex++ ) {
		    
		    System.out.println("NE"+neIndex+": "+solution[neIndex]);
		}
	    }
	    else
		System.out.println("No Solution");
	    nashQCP.end();
	}
	
	catch( IloException iloE ) {
	    
	    //System.err.println("Concert exception '" + iloE + "' caught");
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
	 * 
	 * empty domain?
	 */
	if( dom == null ) {
	    
	    System.out.println("@CenNashQ->supportCup: NULL Parameters!");
	    return null;
	}
	
	//debug
	int size = 0;
	
	boolean[] suppArray = new boolean[GameAction.NUM_ACTIONS];
	for( int action = 0; action < GameAction.NUM_ACTIONS; action++ ) {
	    
	    suppArray[action] = false;
	    
	    for( int i = 0; i < dom.size(); i++ ) {
		
		Support supp = dom.get(i);
		if( supp.supported(action) ) {
		    
		    
		    //debug
		    size++;
		    
		    suppArray[action] = true;
		    break;
		}
	    }   
	}
	
	
	Support cupSupport = new Support( suppArray );
	if( !cupSupport.isValid() ) {
	    
	    System.out.println("DomainSize: "+dom.size());
	    
	    for( int i = 0; i < dom.size(); i++ ) {
		
		Support supp = dom.get(i);
		
		System.out.println("SupportSize: "+supp.supportSize());
	    }
	}
	
	
	return cupSupport;
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
	int minSumX = 3;
	int maxSumX = 12;
	
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
		
		for( int x3 = 1; x3 <= GameAction.NUM_ACTIONS; x3++ ) {
		    
		    if( x1+x2+x3 == sum ) {
			
			XVector xVector = new XVector( new int[]{x1, x2, x3} );
			if( !retQueue.contains( xVector ) )
			    retQueue.add( xVector );
			else
			    xVector = null;
			
			break;
		    }
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
	    
	    return null;
	}
	
	
	for( int b1 = 0; b1 <= 1; b1++ ) {
	    
	    for( int b2 = 0; b2 <= 1; b2++ ) {
		
		for( int b3 = 0; b3 <= 1; b3++ ) {
		    
		    for( int b4 = 0; b4 <= 1; b4++ ) {
			
			if( (b1+b2+b3+b4) == supportSize ) {
			    
			    Support support = new Support( new int[]{b1,b2,b3,b4} );
			    
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
	    retAction.setAction(2, random.nextInt(GameAction.NUM_ACTIONS));
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
	    
	    /**
	     * choose agent2's action
	     */
	    int agent2 = 2;
	    probabilities[0] = nashE[agent2*GameAction.NUM_ACTIONS+0];
	    for( int a2 = 1; a2 < GameAction.NUM_ACTIONS; a2++ ) {
		
		probabilities[a2] = probabilities[a2-1] + nashE[agent2*GameAction.NUM_ACTIONS+a2]; 
	    }
	    d = random.nextDouble();
	    int action2 = 0;
	    for( int a2 = 0; a2 < GameAction.NUM_ACTIONS; a2++ ) {
		
		if( d < probabilities[a2] ) {
		    
		    action2 = a2;
		    break;
		}
	    }
	    
	    retAction.setAction(0, action0);
	    retAction.setAction(1, action1);
	    retAction.setAction(2, action2);
	}
	
	return retAction;
    }
    
    
    public static void main(String[] args)
    {
	// TODO Auto-generated method stub
	
	new NashQ().generateXVectors();

    }
}
