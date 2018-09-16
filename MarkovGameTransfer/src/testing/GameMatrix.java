package testing;


import help.JointAction;
import help.Support;
import help.XVector;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cp.IloCP;
import ilog.cplex.IloCplex;

import java.awt.Container;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Random;

public class GameMatrix {

	/**
	 * the number of agents in the game
	 */
	private int agentNum = 2;
	
	/**
	 * the number of available actions for each agent
	 */
	private int[] actionNums = null;
	
	
	private int jointActionNums = 4;
	
	/**
	 * game matrix
	 * the first dim is agent index
	 * the second dim is the utilities of joint actions
	 */
	private double[][] gamePayOffs;
	
	
	private Random random = null;
	
	/**
	public GameMatrix()
	{
		agentNum = 2;
		actionNums = new int[agentNum];
		
		
		for( int agentIndex = 0; agentIndex < agentNum; agentIndex++ ) {
			
			actionNums[agentIndex] = 2;
		}
		jointActionNums = 4;
		
		random = new Random();
		gamePayOffs = new double[agentNum][jointActionNums];
		for( int agent = 0; agent < agentNum; agent++ ) {
			
			for( int jntAct = 0; jntAct < jointActionNums; jntAct++ ) {
				
				gamePayOffs[agent][jntAct] = random.nextDouble();
			}
		}
	}
	*/
	
	public GameMatrix()
	{
		random = new Random();
		
		agentNum = 2;
		if( agentNum < 2 )
			agentNum = 2;
		actionNums = new int[agentNum];
		
		
		jointActionNums = 1;
		for( int agentIndex = 0; agentIndex < agentNum; agentIndex++ ) {
			
			actionNums[agentIndex] = 20;//random.nextInt(5);
			if( actionNums[agentIndex] < 1 )
				actionNums[agentIndex] = 1;
			
			jointActionNums *= actionNums[agentIndex];
		}
		
		/**
		 * construct game matrix
		 */
		
		gamePayOffs = new double[agentNum][jointActionNums];
		for( int agent = 0; agent < agentNum; agent++ ) {
			
			for( int jntAct = 0; jntAct < jointActionNums; jntAct++ ) {
				
				gamePayOffs[agent][jntAct] = random.nextDouble();
			}
		}
	}
	
	/**
	 * @param payoffMtx: each agent's payoffs should be arranged as follows.
	 * the most outside loop: for agent 1
	 * then agent 2
	 * then agent 3
	 * ...
	 */
	public GameMatrix( int agNum, int[] actNums, double[][] payoffMtx )
	{
		agentNum = agNum;
		actionNums = new int[agentNum];
		jointActionNums = 1;
		for( int agent = 0; agent < agentNum; agent++ ) {
			
			actionNums[agent] = actNums[agent];
			if( actionNums[agent] <= 0 )
				actionNums[agent] = 1;
			
			jointActionNums *= actionNums[agent];
		}
		
		
		/**
		 * construct the payoff matrix
		 */
		gamePayOffs = new double[agentNum][jointActionNums];
		for( int agent = 0; agent < agentNum; agent++ ) {
			
			for( int jntAct = 0; jntAct < jointActionNums; jntAct++ ) {
				
				gamePayOffs[agent][jntAct] = payoffMtx[agent][jntAct];
			}
		}
	}
	
	
	/**
	 * get the joint action in memory from all agents' actions
	 */
	private int getJointAction( int[] agentActions )
	{
		if( agentActions == null || agentActions.length != agentNum ) {
			
			System.out.println("GameMatrix@getJointAction: Parameter Wrong!");
			return 1;
		}
		
		int jntAct = 0;
		for( int agent = 0; agent < agentNum; agent++ ) {
			
			/**
			 * for agent i's, its action loc is:
			 * act_i * actNum[i+1] * actNum[i+2] *...*actNum[n]
			 */
			int actLoc = agentActions[agent];
			for( int agent_p = agent+1; agent_p < agentNum; agent_p++ ) {
				
				actLoc *= actionNums[agent_p];
			}
			
			jntAct += actLoc;
		}
		
		
		return jntAct;
	}
	
	private int getJointAction( JointAction jntAction )
	{
		if( jntAction == null ) {
			
			System.out.println("GameMatrix@getJointAction: Parameter Wrong!");
			return 1;
		}
		
		return getJointAction( jntAction.getActions() );
	}
	
	/**
	 * get agents' corresponding actions in a joint action
	 */
	private int[] getActions( int jntAction )
	{
		
		if( jntAction < 0 || jntAction >= jointActionNums ) {
			
			System.out.println("GameMatrix@getActions: Parameter jntAction Wrong!");
			return null;
		}
		
		int[] retActions = new int[agentNum];
		
		int sum = jntAction;
		for( int agent = 0; agent < agentNum; agent++ ) {
			
			/**
			 * for agent i's, its action loc is:
			 * act_i * actNum[i+1] * actNum[i+2] *...*actNum[n]
			 */
			int factor = 1;
			for( int agent_p = agent+1; agent_p < agentNum; agent_p++ ) {
				
				factor *= actionNums[agent_p];
			}
			//double check
			retActions[agent] = sum / factor;
			sum -= factor * retActions[agent];
		}
		
		return retActions;
	}
	
	public void showGameMatrix()
	{
		
		for( int agent = 0; agent < agentNum; agent++ ) {
			
			/**
			 * the first row
			 */
			System.out.println("====================================");
			System.out.println("Agent "+agent+"'s payoffs");
			
			/**
			 * note that each row is a joint action of the other agents and one action of the current agent
			 * so first print the action row of the current agent
			 */
			for( int act = 0; act < actionNums[agent]; act++ ) {
				
				System.out.print("        A"+agent+"_"+act+"     ");
			}
			System.out.println();
			
			
			/**
			 * then for each other's joint actions
			 * each row should begin with the joint action
			 */
			//joint actions for iteration
			ArrayList<JointAction> othJointActions = generateOtherJntActions( agent );
			for( int listIndex = 0; listIndex < othJointActions.size(); listIndex++ ) {
				
				JointAction jointAction = othJointActions.get( listIndex );
				
				//print the partial joint action
				String partJntActStr = "(";
				for( int agent_p = 0; agent_p < agentNum; agent_p++ ) {
					
					if( agent_p == agent ) 
						partJntActStr += "_";
					else
						partJntActStr += jointAction.getAction( agent_p );
					
					if( agent_p < agentNum-1 )
						partJntActStr += ",";
					else
						partJntActStr += ")";
				}
				System.out.print( partJntActStr );
				
				//for each action of agent
				for( int act = 0; act < actionNums[agent]; act++ ) {
					
					jointAction.setAction( agent, act );
					System.out.print(" "+gamePayOffs[agent][getJointAction(jointAction)]+" ");
				}
				
				System.out.println();
			}
			
		}
		
	}
	
	
	/**
	 * the returned value is stored for each agent's action probabilities, not the joint actions
	 * first agent 1
	 * second agent 2
	 * ...
	 */
	public double[] computeNE()
	{
			
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
		    for( int agent = 0; agent < agentNum; agent++ ) {
			
		    	domainProfile.add( generateSupportDomain( xVector.getX(agent), agent ) );
		    }
		    
		    /**
		     * init the support profile to null
		     */
		    Support[] supportProfile = new Support[agentNum];
		    for( int agent = 0; agent < agentNum; agent++ ) {
			
		    	supportProfile[agent] = null;
		    }
		    
		    
		    /**
		     * call the method recursiveBackTracking
		     */
		    double[] nashEquil = recursiveBackTracking( supportProfile, domainProfile, 0 );
		    if( nashEquil != null )
		    	return nashEquil;
		    
		    //release memory
		    domainProfile = null;
		    supportProfile = null;
		}
		
		
		return null;	
	}
	
    protected double[] recursiveBackTracking( Support[] suppProfile, 
    	    ArrayList<ArrayList<Support>> domProfile, int index )
    {
    	
    	/**
    	 * check parameters
    	 */
    	
    	if( index == agentNum ) {
    		
    	    //feasibility program
    	    return feasibilityProgram( suppProfile );
    	    
    	}
    	else {
    	      		
    		/**
    	     * initialized the support of the current agent
    	     */
    	    ArrayList<Support> Di = domProfile.get( index );
    	    
    	    while( !Di.isEmpty() ) {
    		
    	    	suppProfile[index] = Di.remove(0); //no need to recover Di here
    		
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
    	    	for( int i = index+1; i < agentNum; i++ ) {
    		    
    	    		//from D_{i+1} to D_n, we use a new list
    	    		/**/
    	    		ArrayList<Support> dom = new ArrayList<Support>();
    		    
    	    		for( int listIndex = 0; listIndex < domProfile.get(i).size(); listIndex++ )
    	    			dom.add( domProfile.get(i).get(listIndex) );
    		    	
    		    
    	    		domPro_IRSDS.add( dom );
    	    	}
    		
    		
    	    	//if IRSDS succeeds, use domPro_IRSDS for recurse
    	    	//else, domProfile remains unchanged
    	    	if( IRSDS( domPro_IRSDS ) ) {
    		    
    	    		//use an updated domain profile
    	    		/**
    				ArrayList<ArrayList<Support>> newList = new ArrayList<ArrayList<Support>>();
    		    	for( int i = 0; i <= index; i++ )//why use the first i parts??
    		    	newList.add( domProfile.get(i) );
    			   	for( int i = index+1; i < agentNum; i++ )
    		    	newList.add( domPro.get(i) );
    		    
    		    	domProfile.clear();
    		    	domProfile.addAll( newList );
    		    
    		    	double[] nashEquil = recursiveBackTracking( suppProfile, domProfile, index+1 );
    	    		 */
    			
    	    		//why not directly use domPro_IRSDS
    	    		double[] nashEquil = recursiveBackTracking( suppProfile, domPro_IRSDS, index+1 );
    		  
    	    		if( nashEquil != null )
    	    			return nashEquil;
    	    	}
    	    }
    	}
    	
    	return null;
    }
	
    //Double Check
    private boolean IRSDS( ArrayList<ArrayList<Support>> domPro_IRSDS )
    {
    	/**
    	 * check the parameter
    	 */
    	if( domPro_IRSDS == null || domPro_IRSDS.size() != agentNum ) {
	    
    		System.out.println("@IRSDS: Wrong Parameters!");
	    
    		return false;
    	}
    	
    	boolean changed = true;
    	while( changed ) {
	    
    		changed = false;
	    
    		/**
    		 * loop for all agent index
    		 */
    		for( int agent_i = 0; agent_i < agentNum; agent_i++ ) {
		
    			/**
    			 * get the domain of agent i's support Di
    			 * and compute the union support of all elements in Di
    			 */
    			ArrayList<Support> Di = domPro_IRSDS.get(agent_i);
		
    			//if Di is empty already
    			//why Di is already empty?
    			if( Di.isEmpty() )
    				return false;
		
    			Support cupSupp = supportCup( Di, agent_i );
		
    			/**
    			 * loop for actions supported by cupSupp
    			 */
    			for( int a_i = 0; a_i < actionNums[agent_i]; a_i++ ) {
		    
    				if( !cupSupp.supported(a_i) )
    					continue;
		    
    				/**
    				 * loop for other actions in Ai
    				 */
    				for( int ap_i = 0; ap_i < actionNums[agent_i]; ap_i++ ) {
			
    					if( ap_i == a_i )
    						continue;
			
    					/**
    					 * check whether a is conditionally dominated by ap
    					 */
    					Support[] otherCups = new Support[agentNum];
    					for( int j = 0; j < agentNum; j++ ) {
			    
    						if( j == agent_i ) {
				
    							otherCups[j] = null;//new Support(b, actNum); 
    							continue;
    						}
			    
    						ArrayList<Support> Dj = domPro_IRSDS.get(j);
			    
    						//if Dj is empty already
    						//why here
    						if( Dj.isEmpty() )
    							return false;
				
    						otherCups[j] = supportCup( Dj, j );
    					}
    					
    					//generate all possible joint actions
    					ArrayList<JointAction> othJntActList_a = generateOtherJntActions(agent_i, otherCups);
    					ArrayList<JointAction> othJntActList_ap = generateOtherJntActions(agent_i, otherCups);
    					                                                                         
    					
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
			    
    						JointAction jntAction_a = othJntActList_a.get(listIndex);
    						JointAction jntAction_ap = othJntActList_ap.get(listIndex);
    						
    						jntAction_a.setAction( agent_i, a_i );
    						jntAction_ap.setAction( agent_i, ap_i );
    						
    						int memIndex_a = getJointAction( jntAction_a );
    						int memIndex_ap = getJointAction( jntAction_ap );
			    
    						double Ui_a = gamePayOffs[agent_i][memIndex_a];
    						double Ui_ap = gamePayOffs[agent_i][memIndex_ap];
    						if( Ui_a >= Ui_ap ) {
				
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
	
    
    /**
     * compute the union support of several supports
     */
    private Support supportCup( ArrayList<Support> dom, int agent  )
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
    	
    	int size = 0;
	
    	boolean[] suppArray = new boolean[actionNums[agent]];
    	for( int action = 0; action < actionNums[agent]; action++ ) {
	    
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
	
    	Support cupSupport = new Support( suppArray, actionNums[agent] );
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
    	int minSumX = agentNum;
    	int maxSumX = 0;
    	for( int agent = 0; agent < agentNum; agent++ ) {
    		
    		maxSumX += actionNums[agent];
    	}
	
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
    
    //Double Check
    private PriorityQueue<XVector> generateXVectors( int sum ) 
    {
    	/**
    	 * note that the head of a PriorityQueue is the minimum element
    	 */
    	PriorityQueue<XVector> retQueue = new PriorityQueue<XVector>();
	
    	
    	
    	/**
    	 * one for each agent's support size
    	 */
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
    			
    			XVector xVector = new XVector( xs, actionNums, agentNum );
    			
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
				
				if( agent > 0 && xs[agent] == actionNums[agent]+1 ) {
					
					xs[agent] = 1;
					
					//then the next agent action should also increase
				}
				else
					break;
			}
			
			/**
			 * whether to continue
			 */
			if( xs[0] == actionNums[0]+1 )
				break;
    	}
    	
    	
    	return retQueue;
    }
    
    protected ArrayList<Support> generateSupportDomain( int supportSize, int agent )
    {
    	ArrayList<Support> retList = new ArrayList<Support>();
	
    	if( supportSize < 1 || supportSize > actionNums[agent] ) {
	    
    		System.out.println("@generateSupportDomain: Wrong Support Size!");
	    
    		return null;
    	}
	
    	
    	int[] flags = new int[actionNums[agent]];
    	for( int act = 0; act < actionNums[agent]; act++ )
    		flags[act] = 0;
    	
    	
    	//Double Check
    	while( true ) {
    		
    		
    		/**
    		 * check the current support
    		 */
    		int flagSum = 0;
    		for( int act = 0; act < actionNums[agent]; act++ ) 
    			flagSum += flags[act];
    		
    		if( flagSum == supportSize ) {
    			
    			Support support = new Support( flags, actionNums[agent]);
				if( !retList.contains(support) )
					retList.add(support);
				else
					support = null;
    		}
    		
    		/**
    		 * compute the next support
    		 */
    		for( int act = actionNums[agent]-1; act >= 0; act-- ) {
    			
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
    
    
    //Check again
    private double[] feasibilityProgram( Support[] suppProfile )
    {
    	if( suppProfile == null || 
    			suppProfile.length != agentNum ) {
	    
    		System.out.println("@feasibilityProgram: Wrong Parameter!");
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
    		//IloCplex nashQCP = new IloCplex();
    		
    		IloCP nashQCP = new IloCP();
    		
    		
    		/**
    		 * 2. secondly, create the variables:
    		 * stores each agent's probability of taking each action
    		 * agent i's j-th action probability: index = i * Num_Actions
    		 */
    		int varNum = 0;
    		for( int agent = 0; agent < agentNum; agent++ )
    			varNum += actionNums[agent];
    		
    		double[] lowBounds = new double[varNum];
    		double[] uppBounds = new double[varNum];
    		for( int varIndex = 0; varIndex < varNum; varIndex++ ) {
		
    			lowBounds[varIndex] = 0.0;
    			uppBounds[varIndex] = 1.0;
    		}
    		IloNumVar[] p = nashQCP.numVarArray( varNum, lowBounds, uppBounds );
	    
	    
    		/**
    		 * 3. then create the constraints:
    		 * IloRange class for range constraints
    		 */
	    
    		//3.1 for the sum of each agent's action probability
    		//\Sum_{a_i \in S_i} p_i(a_i) = 1
    		int actIndexRange_Low = 0;    //include
    		int actIndexRange_High = actionNums[0]; //exclude
    		for( int agent_i = 0; agent_i < agentNum; agent_i++ ) {
		 
    			Support supp_i = suppProfile[agent_i];
    			double[] coeff = new double[varNum];
    			for( int coeffIndex = 0; coeffIndex < varNum; coeffIndex++ ) {
		    
    				//not the action range of the current agent
    				if( coeffIndex < actIndexRange_Low || coeffIndex >= actIndexRange_High )
    					coeff[coeffIndex] = 0.0;
    				else {
    					
    					int act = coeffIndex - actIndexRange_Low;
    					if( !supp_i.supported(act) )
        					coeff[coeffIndex] = 0.0;
        				else
        					coeff[coeffIndex] = 1.0;
    				}
    			}
		
    			IloNumExpr sumExpr = nashQCP.scalProd(coeff, p);
    			nashQCP.addEq( sumExpr, 1.0 );
    			
    			//the action range for the next agent
    			if( agent_i < agentNum-1 ) {
    			
    				actIndexRange_Low = actIndexRange_High;
    				actIndexRange_High += actionNums[agent_i+1];
    			}
    		}
	    
    		//3.2 for each agent's unsupported action
    		//for any a_i \not\in S_i p_i(a_i) = 0
    		for( int agent_i = 0; agent_i < agentNum; agent_i++ ) {
		
    			Support supp_i = suppProfile[agent_i];
    			if( supp_i.supportSize() == actionNums[agent_i] )
    				continue;
		
    			for( int act = 0;  act < actionNums[agent_i]; act++ ) {
		    
    				//the variable index of this action
    				int varIndex = getVariableIndex( agent_i, act );
    				
    				if( !supp_i.supported( act ) ) {
			
    					double[] coeff = new double[varNum];
    					for( int coeffIndex = 0; coeffIndex < varNum; coeffIndex++ ) {
			    
    						if( coeffIndex == varIndex )
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
    		for( int agent_i = 0; agent_i < agentNum; agent_i++ ) {
		    
    			Support support_i = suppProfile[agent_i];
		    
    			/**
    			 * loop for all supported actions
    			 */
    			for( int a_i = 0; a_i < actionNums[agent_i]; a_i++ ) {
			
    				if( !support_i.supported( a_i ) )
    					continue;
			
    				/**
    				 * loop for all actions that are not supported
    				 */
    				for( int ap_i = 0; ap_i < actionNums[agent_i]; ap_i++ ) {
			    
    					if( support_i.supported( ap_i ) )
    						continue;
			    
			    
    					/**
    					 * loop for the joint actions of the other agents
    					 * Note that each other joint actions should be supported!!!
    					 */
    					ArrayList<JointAction> othJntActList_a = generateOtherJntActions(agent_i, suppProfile);
    					ArrayList<JointAction> othJntActList_ap = generateOtherJntActions(agent_i, suppProfile);
    					
    					IloNumExpr ineqExpr = nashQCP.constant(0);
    					for( int listIndex = 0; listIndex < othJntActList_a.size(); listIndex++ ) {
    						
    						JointAction jntAction_a = othJntActList_a.get(listIndex);
    						JointAction jntAction_ap = othJntActList_ap.get(listIndex);
    						jntAction_a.setAction(agent_i, a_i);
    						jntAction_ap.setAction(agent_i, ap_i);
    						
    						int memIndex_a = getJointAction( jntAction_a );
    						int memIndex_ap = getJointAction( jntAction_ap );
    						
    						double coeff = gamePayOffs[agent_i][memIndex_a] - gamePayOffs[agent_i][memIndex_ap];
    						
    						//variable indices
    						IloNumExpr itemExpr = nashQCP.constant(coeff);
    						for( int agent_j = 0; agent_j < agentNum; agent_j++ ) {
    							
    							if( agent_j == agent_i )
    								continue;
    							
    							int act_j = jntAction_a.getAction( agent_j );
    							int varIndex_j = getVariableIndex( agent_j, act_j );
    							
    							itemExpr = nashQCP.prod( itemExpr, p[varIndex_j] );
    						}
    						//add the item to the inequality expression
							ineqExpr = nashQCP.sum( ineqExpr, itemExpr );
    					}
    					/**
						 * create the inequality constraint
						 */
    					nashQCP.addGe( ineqExpr, 0 );
    				}//loop for unsupported actions of agent_i
    			}//loop for supported actions of agent_i
		
    			/**
    			 * all supported actions has equal values
    			 */
    			int firstSuppAction = 0;
    			for( int a_i = 0; a_i < actionNums[agent_i]; a_i++ ) {
				
    				if( support_i.supported(a_i) ) {
			
    					firstSuppAction = a_i;
    					break;
    				}
    			}
    			for( int a_i = firstSuppAction+1; a_i < actionNums[agent_i]; a_i++ ) {
				
    				if( !support_i.supported(a_i) || a_i == firstSuppAction )
    					continue;
				
    				IloNumExpr eqExpr = nashQCP.constant(0);
    				
    				//the joint actions of the other agents
    				//should also be supported
					ArrayList<JointAction> othJntActList_afs = generateOtherJntActions(agent_i, suppProfile);
					ArrayList<JointAction> othJntActList_a = generateOtherJntActions(agent_i, suppProfile);
					for( int listIndex = 0; listIndex < othJntActList_a.size(); listIndex++ ) {
						
						JointAction jntAction_a = othJntActList_a.get(listIndex);
						JointAction jntAction_afs = othJntActList_afs.get(listIndex);
						jntAction_a.setAction(agent_i, a_i);
						jntAction_afs.setAction(agent_i, firstSuppAction);
						
						int memIndex_a = getJointAction( jntAction_a );
						int memIndex_afs = getJointAction( jntAction_afs );
						
						double coeff = gamePayOffs[agent_i][memIndex_a] - gamePayOffs[agent_i][memIndex_afs];
						
						//variable indices
						IloNumExpr itemExpr = nashQCP.constant(coeff);
						for( int agent_j = 0; agent_j < agentNum; agent_j++ ) {
							
							if( agent_j == agent_i )
								continue;
							
							int act_j = jntAction_a.getAction( agent_j );
							int varIndex_j = getVariableIndex( agent_j, act_j );
							
							itemExpr = nashQCP.prod( itemExpr, p[varIndex_j] );
						}
						eqExpr = nashQCP.sum( eqExpr, itemExpr );
					}
					nashQCP.addEq( eqExpr, 0.0 );
    			}//loop for the supported actions of agent_i, except the firstSuppAction
    		}//loop for agent_i
	    
	    
    		/**
    		 * 4. set the objective function
    		 * since there is not an objective function in this feasibility program 
    		 * set a null objective function??
    		 *
	    	double[] coeffs = new double[agentNum*GameAction.NUM_ACTIONS];
	    	for( int coeffIndex = 0; coeffIndex < agentNum * GameAction.NUM_ACTIONS; coeffIndex++ )
			coeffs[coeffIndex] = 1.0;
	    	nashQCP.add( nashQCP.maximize( nashQCP.scalProd( coeffs, p) ) );
    		 */
    		
    		//nashQCP.add( nashQCP.maximize( nashQCP.constant(0.0) ) );
	    
    		/**
    		 * all constraints have been set
    		 * then we should solve this QCP
    		 */
    		if( nashQCP.solve() ) {
		
    			solution = new double[varNum]; 
    			nashQCP.getValues( p, solution );
			
    			System.out.println("Solution Length"+solution.length);
		
    			for( int neIndex = 0; neIndex < varNum; neIndex++ ) {
		    
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
    
    
    //Double Check
    /**
     * generate the joint actions of the agents other than agent_i
     */
    private ArrayList<JointAction> generateOtherJntActions( int agent_i, Support[] suppProfile )
    {
    	
    	if( suppProfile == null || suppProfile.length != agentNum ) {
	    
    		System.out.println("@generateOtherJntActions: Wrong Parameter!");
    		return null;
    	}
    	
    	
    	ArrayList<JointAction> retJntActions = new ArrayList<JointAction>();
    	
    	
		//agents' actions for iteration
		int[] agentActionsIter = new int[agentNum];
		for( int agent = 0; agent < agentNum; agent++ )
			agentActionsIter[agent] = 0;
		
		boolean cont = true;
		while( cont ) {
			
			/**
			 * the current partial joint action
			 */
			JointAction jntAction = new JointAction( agentActionsIter );
			
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
				
				if( agent_p > first_agent && agentActionsIter[agent_p] == actionNums[agent_p] ) {
					
					agentActionsIter[agent_p] = 0;
					
					//then the next agent action should also increase
				}
				else
					break;
			}
			
			/**
			 * whether to continue
			 */
			if( agentActionsIter[first_agent] == actionNums[first_agent] )
				break;
		}
		
		return retJntActions;
    }
    
    
    /**
     * generate the joint actions of the agents other than agent_i 
     * according to the support profile
     * 
     */
    private ArrayList<JointAction> generateOtherJntActions( int agent_i )
    {
    	
    	ArrayList<JointAction> retJntActions = new ArrayList<JointAction>();
    	
    	
    	
		//agents' actions for iteration
		int[] agentActionsIter = new int[agentNum];
		for( int agent = 0; agent < agentNum; agent++ )
			agentActionsIter[agent] = 0;
		
		boolean cont = true;
		while( cont ) {
			
			/**
			 * the current partial joint action
			 */
			JointAction jntAction = new JointAction( agentActionsIter );
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
				
				if( agent_p > first_agent && agentActionsIter[agent_p] == actionNums[agent_p] ) {
					
					agentActionsIter[agent_p] = 0;
					
					//then the next agent action should also increase
				}
				else
					break;
			}
			
			/**
			 * whether to continue
			 */
			if( agentActionsIter[first_agent] == actionNums[first_agent] )
				break;
		}
		
		return retJntActions;
    }
    
    private int getVariableIndex( int agent, int act )
    {
    	//if agent
    	//if act
    	
		int varIndex = 0;
		for( int agent_k = 0; agent_k < agent; agent_k++ )
			varIndex += actionNums[agent_k];
		
		varIndex += act;
		
		return varIndex;	
    }
    
    public boolean checkNE( double[] nashEquil )
    {
    	if( nashEquil == null ) {
    		
    		System.out.println("@checkNE: NULL Nash equilibrium!");
    		return false;
    	}
    	
		int varNum = 0;
		for( int agent = 0; agent < agentNum; agent++ )
			varNum += actionNums[agent];
		
		if( nashEquil.length != varNum ) {
			
			System.out.println("@checnNE: Wrong Size of Nash equilibrium");
			return false;
		}
		
		
		/**
		 * for each agent
		 */
		for( int agent = 0; agent < agentNum; agent++ ) {
			
			double[] expUtility = new double[actionNums[agent]];
			
			
			//compute the expected utility of each action
			for( int act = 0; act < actionNums[agent]; act++ ) {
				
				expUtility[act] = 0.0;
				
				/**
				 * the other joint action
				 */
				ArrayList<JointAction> othJointActions = generateOtherJntActions( agent );
				for( int listIndex = 0; listIndex < othJointActions.size(); listIndex++ ) {
					
					JointAction jntAction = othJointActions.get( listIndex );
					jntAction.setAction( agent, act );
					
					double pro = 1.0;
					for( int agent_p = 0; agent_p < agentNum; agent_p++ ) {
						
						if( agent_p != agent ) {
							
							int act_p = jntAction.getAction( agent_p );
							int varIndex = getVariableIndex(agent_p, act_p);
							pro *= nashEquil[varIndex];
						}
					}
					expUtility[act] += pro * gamePayOffs[agent][getJointAction(jntAction)]; 
				}
			}
			
			//check the value for each action
			double nashValue = 0.0;
			for( int act = 0; act < actionNums[agent]; act++ ) {
				
				int varIndex = getVariableIndex( agent, act );
				if( nashEquil[varIndex] > 0.0 )
					nashValue += expUtility[act] * nashEquil[varIndex]; 
			}
			for( int act = 0; act < actionNums[agent]; act++ ) {
				
				int varIndex = getVariableIndex( agent, act );
				
				//for unsupported action
				if( nashEquil[varIndex] <= 0.0000001 ) {
					
					if( expUtility[act] >= nashValue ) {
						
						System.out.println("Agent "+agent+", NashValue: "+nashValue+", unspp action utility "+act+":"+expUtility[act]);		
						return false;
					}
				}
				else {
					
					if( Math.abs(expUtility[act]-nashValue)  > 0.000001 ) {
						
						System.out.println("Agent "+agent+", NashValue: "+nashValue+", spp action utility "+act+":"+expUtility[act]);
						return false;
					}
				}
			}
			
			System.out.println("Nash value for agent "+agent+": "+nashValue);
		}
		
		
		System.out.println("This is a Nash equilibrium!");
		return true;
    	
    }
    
	public static void main(String[] args)
	{
			
		double[] payoff_agent1 = {10,-20,-20,-20,-20,5,-20,5,-20, -20,-20,5,-20,10,-20,5,-20,-20, -20,5,-20,5,-20,-20,-20,-20,10};
		double[] payoff_agent2 = {10,-20,-20,-20,-20,5,-20,5,-20, -20,-20,5,-20,10,-20,5,-20,-20, -20,5,-20,5,-20,-20,-20,-20,10};
		double[] payoff_agent3 = {10,-20,-20,-20,-20,5,-20,5,-20, -20,-20,5,-20,10,-20,5,-20,-20, -20,5,-20,5,-20,-20,-20,-20,10}; 
		double[][] payoffMat = new double[3][];
		payoffMat[0] = payoff_agent1;
		payoffMat[1] = payoff_agent2;
		payoffMat[2] = payoff_agent3;
		//GameMatrix mat = new GameMatrix( 3, new int[]{3,3,3}, payoffMat );

		
		
		//for( int i = 0; i < nashEquil.length; i++ ) 
			//System.out.println( nashEquil[i] );
		
		int count = 0;
		for( int round = 1; round <= 10; round++ ) {
			
			GameMatrix mat = new GameMatrix( );
			
			//mat.showGameMatrix();
			double[] nashEquil = mat.computeNE();
			
			mat.showGameMatrix();
			
			if( mat.checkNE(nashEquil) )
				count++;
		}
		
		if( count == 100 )
			System.out.println("All Nash equilibrium");
		else
			System.out.println("Only "+count);
	}
    
}
