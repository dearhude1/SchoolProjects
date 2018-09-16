package testing;

import help.Support;
import help.XVector;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Random;

import javax.lang.model.element.Element;

public class TwoDimMatrix {

	
	private int[] actionNums = null;
	
	private double[][][] gamePayoffs = null;
	
	private Random random = null;
	
	
	private double biasFactor = 0.1;
	
	
	private Support support[];
	

	public TwoDimMatrix()
	{
		
		actionNums = new int[2];
		actionNums[0] = actionNums[1] = 1;
		
		random = new Random();
		
		gamePayoffs = new double[2][actionNums[0]][actionNums[1]];
		for( int agentIndex = 0; agentIndex < 2; agentIndex++ ) {
			
			for( int act0 = 0; act0 < actionNums[0]; act0++ ) {
				
				for( int act1 = 0; act1 < actionNums[1]; act1++ ) {
					
					double payoff = 50 * random.nextDouble();
					String pStr = Double.toString( payoff );
					String subStr = pStr.substring(0, pStr.lastIndexOf(".")+3);
					payoff = Double.parseDouble( subStr );
					
					gamePayoffs[agentIndex][act0][act1] = payoff;
				}
			}
		}
		
		
		support = new Support[2];
	}
	
	public TwoDimMatrix( TwoDimMatrix mat )
	{
		random = new Random();
		
		if( mat == null ) {
			
			actionNums = new int[2];
			actionNums[0] = actionNums[1] = 1;
			
			gamePayoffs = new double[2][actionNums[0]][actionNums[1]];
			for( int agentIndex = 0; agentIndex < 2; agentIndex++ ) {
				
				for( int act0 = 0; act0 < actionNums[0]; act0++ ) {
					
					for( int act1 = 0; act1 < actionNums[1]; act1++ ) {
						
						double payoff = 50 * random.nextDouble();
						String pStr = Double.toString( payoff );
						String subStr = pStr.substring(0, pStr.lastIndexOf(".")+3);
						payoff = Double.parseDouble( subStr );
						
						gamePayoffs[agentIndex][act0][act1] = payoff;
					}
				}
			}
		}
		else {
			
			actionNums = new int[2];
			actionNums[0] = mat.getActionNums(0);
			actionNums[1] = mat.getActionNums(1);
			
			gamePayoffs = new double[2][actionNums[0]][actionNums[1]];
			for( int agentIndex = 0; agentIndex < 2; agentIndex++ ) {
				
				for( int act0 = 0; act0 < actionNums[0]; act0++ ) {
					
					for( int act1 = 0; act1 < actionNums[1]; act1++ ) {
						
						gamePayoffs[agentIndex][act0][act1] = mat.getPayoff(agentIndex, act0, act1);
					}
				}
			}
		}
		
		support = new Support[2];
	}
	
	
	public TwoDimMatrix( int actNum0, int actNum1 ) 
	{
		actionNums = new int[2];
		actionNums[0] = actNum0;
		actionNums[1] = actNum1;
		
		random = new Random();
		
		gamePayoffs = new double[2][actionNums[0]][actionNums[1]];
		for( int agentIndex = 0; agentIndex < 2; agentIndex++ ) {
			
			for( int act0 = 0; act0 < actionNums[0]; act0++ ) {
				
				for( int act1 = 0; act1 < actionNums[1]; act1++ ) {
					
					double payoff = 50 * random.nextDouble();
					String pStr = Double.toString( payoff );
					String subStr = pStr.substring(0, pStr.lastIndexOf(".")+3);
					payoff = Double.parseDouble( subStr );
					
					gamePayoffs[agentIndex][act0][act1] = payoff;
				}
			}
		}
		
		support = new Support[2];
	}
	
	
	public double getPayoff( int agentIndex, int act0, int act1 )
	{
		return gamePayoffs[agentIndex][act0][act1];
	}
	
	public int getActionNums( int agentIndex )
	{
		if( agentIndex < 0 || agentIndex > 1 )
			return 0;
		
		return actionNums[agentIndex];
	}
	
	
	public void biasMatrix()
	{
		
		/**
		 * determine one game element
		 */
		int act0_bias = random.nextInt( actionNums[0] );
		int act1_bias = random.nextInt( actionNums[1] );
		while( !(support[0].supported(act0_bias) && support[1].supported(act1_bias)) ) {
			
			act0_bias = random.nextInt( actionNums[0] );
			act1_bias = random.nextInt( actionNums[1] );
		}
		
		
		/**
		 * bias agent0's payoff
		 */
		int sign = 1;
		if( random.nextInt(2) == 0 )
			sign = -1;
		
		double biasValue0 = gamePayoffs[0][act0_bias][act1_bias] * sign * biasFactor;
		gamePayoffs[0][act0_bias][act1_bias] += biasValue0;
		
		/**
		 * bias agent1's payoff
		 */
		sign = 1;
		if( random.nextInt(2) == 0 )
			sign = -1;
		
		double biasValue1 = gamePayoffs[1][act0_bias][act1_bias] * sign * biasFactor;
		gamePayoffs[1][act0_bias][act1_bias] += biasValue1;
		
		
		System.out.println("Bias action: (a"+act0_bias+" , b"+act1_bias+")");
	}
	
	public void showGameMatrix()
	{
		/**
		 * the first row
		 * print all actions of agent 1
		 */
		System.out.print("========");
		for( int act1 = 0; act1 < actionNums[1]; act1++ ) {
			
			System.out.print("   b"+act1+"   ");
		}
		System.out.println();
		
		
		/**
		 * then print each row
		 * begin with agent0's action
		 */
		for( int act0 = 0; act0 < actionNums[0]; act0++ )
		{
			
			System.out.print("   a"+act0+"   ");
			for( int act1 = 0; act1 < actionNums[1]; act1++ ) {
				
				double payoff0 = gamePayoffs[0][act0][act1];
				double payoff1 = gamePayoffs[1][act0][act1];
				
				
				System.out.print( "("+payoff0+","+payoff1+")" );
			}
			System.out.println();
		}
	}
	
	

    private double[] computeNE()
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
	    
	    
    		//for all S0
    		int x0 = xVector.getX(0);
    		ArrayList<Support> D0 = generateSupportDomain( x0, 0 );
    		
    		for( int s0Index = 0; s0Index < D0.size(); s0Index++ ) {
		
    			Support supp0 = D0.get( s0Index );
		
    			//given s0, find the action of a1 which are not conditionally dominated
    			boolean[] a1_supported = new boolean[actionNums[1]];
    			for( int a1 = 0; a1 < actionNums[1]; a1++ )	    
    				a1_supported[a1] = !conditionallyDominated( 1, a1, supp0 );
    			
    			Support supDominated1 = new Support( a1_supported, actionNums[1] );
    			if( !supDominated1.isValid() ) {
    				supDominated1 = null;
    				continue;
    			}
		
    			//whether there exists action a0 \in S0 which is conditionally dominated
    			//given supDominated1
    			boolean exists_dominated_a0 = false;
    			for( int a0 = 0; a0 < actionNums[0]; a0++ ) {
		    
    				if( !supp0.supported(a0) )
    					continue;
		    
    				if( conditionallyDominated( 0, a0,  supDominated1 ) ) {
			
    					exists_dominated_a0 = true;
    					break;
    				}
    			}
		
    			if( !exists_dominated_a0 ) {
		    
    				//for all s1
    				int x1 = xVector.getX(1);
    				ArrayList<Support> D1 = generateSupportDomain( x1, 1 );
		    
    				for( int s1Index = 0; s1Index < D1.size(); s1Index++ ) {
			
    					Support supp1 = D1.get(s1Index);
			
    					if( !subSupport( 1, supp1, supDominated1) )
    						continue;
			
    					//whether there exists action a0 \in S0 which is conditionally dominated 
    					//given S1
    					boolean exists_dominated_s0s1 = false;
    					for( int a0 = 0; a0 < actionNums[0]; a0++ ) {
			    
    						if( !supp0.supported(a0) )
    							continue;
			    
    						if( conditionallyDominated( 0, a0,  supp1 ) ) {
				
    							exists_dominated_s0s1 = true;
    							break;
    						}
    					}
			
    					if( !exists_dominated_s0s1 ) {
			    
    						double[] nashEquil = feasibilityProgram( new Support[]{supp0,supp1} );
    						if( nashEquil != null ) {
    							
    							
    							support[0] = supp0;
    							support[1] = supp1;
    							
    							/**
    							 * display the support
    							 */
    							System.out.print("agent 1: ");
    							for( int a0 = 0; a0 < actionNums[0]; a0++ ) {
    								
    								if( nashEquil[a0] > 0 )
    									System.out.print("1 ");
    								else 
    									System.out.print("0 ");
    							}
    							System.out.println();
    							
    							System.out.print("agent 2: ");
    							for( int a1 = 0; a1 < actionNums[1]; a1++ ) {
    								
    								if( nashEquil[actionNums[0]+a1] > 0 )
    									System.out.print("1 ");
    								else 
    									System.out.print("0 ");
    							}
    							System.out.println();
    							
    							return nashEquil;
    						}
    					}
    				}
    			}
		
    		}
    	}
    	
    	
    	return null;
    }
    
    public ArrayList<XVector> generateXVectors()
    {
	
    	ArrayList<XVector> retList = new ArrayList<XVector>();
	
	
    	/**
    	 * first according to difference of x
    	 * for each agent, at least one actions has a probability larger than 0
    	 */
    	int minDiffX = 0;
    	int maxDiffX = actionNums[0] - 1;
    	if( actionNums[0] < actionNums[1] )
    		maxDiffX = actionNums[1] - 1;
	
    	for( int diffX = maxDiffX; diffX >= minDiffX; diffX-- ) {
    	//for( int diffX = minDiffX; diffX <= maxDiffX; diffX++ ) {
	    
    		PriorityQueue<XVector> queue = generateXVectors( diffX );
	    
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
	
    private PriorityQueue<XVector> generateXVectors( int diff ) 
    {
    	/**
    	 * note that the head of a PriorityQueue is the minimum element
    	 */
    	PriorityQueue<XVector> retQueue = new PriorityQueue<XVector>();
	
    	/**
    	 * generate all possible support size vectors
    	 */
    	for( int x1 = 1; x1 <= actionNums[0]; x1++ ) {
	    
    		for( int x2 = 1; x2 <= actionNums[1]; x2++ ) {
		
    			if( (x1-x2 == diff) || (x2-x1 == diff) ) {
			
    				XVector xVector = new XVector( new int[]{x1, x2}, actionNums, 2 );
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
    
    
    private ArrayList<Support> generateSupportDomain( int supportSize, int agent )
    {
    	ArrayList<Support> retList = new ArrayList<Support>();
	
    	if( supportSize < 1 || supportSize > actionNums[agent] ) {
	    
    		System.out.println("@generateSupportDomain: Wrong Support Size!");
    	}
	
	
    	int[] bArray = new int[actionNums[agent]];
    	for( int act = 0; act < actionNums[agent]; act++ )
    		bArray[act] = 0;
    	
    	supportDomain( bArray, supportSize, retList, agent, 0, 0 );
    	
    	/**
    	 * debug?
    	 *
    	for( int i = 0; i < retList.size(); i++ ) {
    		
    		Support sup = retList.get(i);
    		sup.display();
    	}
    	*/
	
    	return retList;
    }
    
    private void supportDomain( int[] bArray, int supportSize, 
    		ArrayList<Support> retList, int agent, int currentPoint, int supportSum )
    {
    	
    	if( currentPoint >= actionNums[agent] ) {
    		
    		return;
    	}
    	
    	/**
    	 * set the current point
    	 */
    	bArray[currentPoint] = 1;
    	supportSum += 1;
    	
    	//add to the retList
    	if( supportSum == supportSize ) {
    		
    		Support support = new Support( bArray, actionNums[agent]);
    		if( !retList.contains( support ) )
    			retList.add( support );
    		else
    			support = null;
    		
    		
    		//then move to the next depth
    		bArray[currentPoint] = 0;
    		supportSum -= 1;
    		supportDomain(bArray, supportSize, retList, agent, currentPoint+1, supportSum);
    		
    	}
    	//move to the next depth
    	else if( supportSum < supportSize ) {
    		
    		supportDomain(bArray, supportSize, retList, agent, currentPoint+1, supportSum);
    		
    		bArray[currentPoint] = 0;
    		supportSum -= 1;
    		supportDomain(bArray, supportSize, retList, agent, currentPoint+1, supportSum);
    		
    	}
    }
    
    
    /**
     * whether the action act_i of agent_i is conditionally dominated
     * given a support of the other agent agent_j
     */
    private boolean conditionallyDominated( int agent_i, int act_i, Support support_j )
    {
    	//parameter
	
    	int agent_j = (agent_i+1) % 2;
	
    	for( int act_ip = 0; act_ip < actionNums[agent_i]; act_ip++ ) {
		
    		if( act_i == act_ip )
    			continue;
		
    		//whether act_i is conditionally dominated by act_ip
    		boolean dominated = true;
    		for( int act_j = 0; act_j < actionNums[agent_j]; act_j++ ) {
		    
    			if( !support_j.supported( act_j ) )
    				continue;
		    
    			double valueAct_i = 0;
    			double valueAct_ip = 0;
    			if( agent_i == 0 ) {
    				
    				valueAct_i = gamePayoffs[agent_i][act_i][act_j];
        			valueAct_ip = gamePayoffs[agent_i][act_ip][act_j];
    			}
    			else {
    				
    				valueAct_i = gamePayoffs[agent_i][act_j][act_i];
        			valueAct_ip = gamePayoffs[agent_i][act_j][act_ip];
    			}
    			
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
    
    
    /**
     * whether supp1 is a sub support of supp2
     */
    private boolean subSupport( int agent, Support supp1, Support supp2 )
    {
    	//parameters
    	
    	for( int act = 0; act < actionNums[agent]; act++ ) {
	    
    		if( supp1.supported( act ) )
    			if( !supp2.supported( act ) )
    				return false;
    	}
	
    	return true;
    }
    
    //here
    private double[] feasibilityProgram( Support[] suppProfile )
    {
    	if( suppProfile == null || suppProfile.length != 2 ) {
	    
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
    		IloCplex  nashLCP = new IloCplex();
	    
    		//IloEnv env = new IloEnv();
    		nashLCP.setOut(null);
	    
    		/**
    		 * 2. secondly, create the variables:
    		 * stores each agent's probability of taking each action
    		 * agent i's j-th action probability: index = i * Num_Actions
    		 */
    		double[] lowBounds = new double[actionNums[0] + actionNums[1]];
    		double[] uppBounds = new double[actionNums[0] + actionNums[1]];
    		for( int varIndex = 0; varIndex < actionNums[0] + actionNums[1]; varIndex++ ) {
		
    			lowBounds[varIndex] = 0.0;
    			uppBounds[varIndex] = 1.0;
    		}
    		IloNumVar[] p = nashLCP.numVarArray( actionNums[0] + actionNums[1], lowBounds, uppBounds);
	    
	    
    		/**
    		 * 3. then create the constraints:
    		 */
	    
    		//3.1 for the sum of each agent's action probability
    		//\Sum_{a_i \in S_i} p_i(a_i) = 1
    		for( int agent_i = 0; agent_i < 2; agent_i++ ) {
		
    			Support supp_i = suppProfile[agent_i];
    			double[] coeff = new double[actionNums[0] + actionNums[1]];
    			for( int coeffIndex = 0; coeffIndex < actionNums[0] + actionNums[1]; coeffIndex++ ) {
		    
    				int agent_j = coeffIndex / actionNums[0];
    				
    				int act = coeffIndex - actionNums[0];
    				if( coeffIndex < actionNums[0] )
    					act = coeffIndex;
    					
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
    		for( int agent_i = 0; agent_i < 2; agent_i++ ) {
		
    			Support supp_i = suppProfile[agent_i];
    			if( supp_i.supportSize() == actionNums[agent_i] )
    				continue;
		
    			for( int act = 0;  act < actionNums[agent_i]; act++ ) {
		    
    				//should be !supported?
    				if( !supp_i.supported( act ) ) {
			
    					double[] coeff = new double[actionNums[0] + actionNums[1]];
    					for( int coeffIndex = 0; coeffIndex < (actionNums[0] + actionNums[1]); coeffIndex++ ) {
			    
    						if( coeffIndex == (agent_i * actionNums[0] + act) )
    							coeff[coeffIndex] = 1.0;
    						else
    							coeff[coeffIndex] = 0.0;
    					}
			
    					IloNumExpr eqzeroExpr = nashLCP.scalProd( coeff, p );
    					nashLCP.addEq( eqzeroExpr, 0.0 );
    				}
    			}
    		}
    		
    		/**
    		for( int agent_i = 0; agent_i < 2; agent_i++ ) {
    			
    			Support supp_i = suppProfile[agent_i];
    			
    			for( int act = 0; act < actionNums[agent_i]; act++ ) {
    				
    				if( supp_i.supported( act ) ) {
    					
    					double[] coeff = new double[actionNums[0] + actionNums[1]];
    					
    					for( int coeffIndex = 0; coeffIndex < (actionNums[0]+actionNums[1]);coeffIndex++ ) {
    						
    						if( coeffIndex == (agent_i * actionNums[0] + act) )
    							coeff[coeffIndex] = 1.0;
    						else
    							coeff[coeffIndex] = 0.0;
    					}
    					
    					IloNumExpr lgzeroExpr = nashLCP.scalProd( coeff, p );
    					nashLCP.addGe( lgzeroExpr, 0.0 );
    				}
    			}
    		}
    		*/
	    
    		//3.3 for the nash condition inequality
    		//for any i \in N, any a_i \in S_i and any a_i' \not\in S_i
    		//\Sum_{a_{-i} \in S_{-i}} p(a_{-i}[u_i(a_i,a_{-i})-u_i(a_i',a_{-i})] >= 0
    		for( int agent_i = 0; agent_i < 2; agent_i++ ) {
		    
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
    					 */
    					IloNumExpr ineqExpr = nashLCP.constant(0);
    					for( int act_j = 0; act_j < actionNums[1-agent_i]; act_j++ ) {
				
    						int agent_j = (agent_i+1) % 2;

    						//locate the variable index
    						int varIndex_j = agent_j * actionNums[0] + act_j;
				
    						//obtain the coefficients
				
    						double coeff = 0.0;
    						if( agent_i == 0 ) 
    							coeff = gamePayoffs[agent_i][a_i][act_j] - gamePayoffs[agent_i][ap_i][act_j];
    						else
    							coeff = gamePayoffs[agent_i][act_j][a_i] - gamePayoffs[agent_i][act_j][ap_i];
    						
    						
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
    			for( int a_i = 0; a_i < actionNums[agent_i]; a_i++ ) {
    				
    				if( support_i.supported(a_i) ) {
    					
    					firstSuppAction = a_i;
    					break;
    				}
    			}
    			for( int a_i = 0; a_i < actionNums[agent_i]; a_i++ ) {
    				
    				if( !support_i.supported(a_i) || a_i == firstSuppAction )
    					continue;
    				
    				IloNumExpr eqExpr = nashLCP.constant(0);
    				for( int act_j = 0; act_j < actionNums[1-agent_i]; act_j++ ) {
    					
						int agent_j = (agent_i+1) % 2;

						//locate the variable index
						int varIndex_j = agent_j * actionNums[0] + act_j;
			
						//obtain the coefficients
			
						double coeff = 0.0;
						if( agent_i == 0 ) 
							coeff = gamePayoffs[agent_i][a_i][act_j] - gamePayoffs[agent_i][firstSuppAction][act_j];
						else
							coeff = gamePayoffs[agent_i][act_j][a_i] - gamePayoffs[agent_i][act_j][firstSuppAction];
						
						
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
		
    			solution = nashLCP.getValues(p);
		
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
     * compute the difference between two game matrix
     * return: this class - mat
     */
    private double[][][] deltaMatrix( TwoDimMatrix mat )
    {
    	if( mat == null ) {
    		
    		return null;
    	}
    	else if( mat.getActionNums(0) != actionNums[0] || 
    			mat.getActionNums(1) != actionNums[1] ) {
    		
    		return null;
    	}
    	
    	double[][][] deltaMat = new double[2][actionNums[0]][actionNums[1]];
    	
    	for( int agentIndex = 0; agentIndex < 2; agentIndex++ ) {
			
			for( int act0 = 0; act0 < actionNums[0]; act0++ ) {
				
				for( int act1 = 0; act1 < actionNums[1]; act1++ ) {
					
					
					deltaMat[agentIndex][act0][act1] = gamePayoffs[agentIndex][act0][act1] - 
						mat.getPayoff(agentIndex, act0, act1);
				}
			}
		}
    	
    	return deltaMat;
    }
    
    public double boundDirectTransfer( TwoDimMatrix mat, double[] nashEquil )
    {
    	if( mat == null || nashEquil == null ) {
    		
    		return -1;
    	}
    	else if( mat.getActionNums(0) != actionNums[0] || 
    			mat.getActionNums(1) != actionNums[1] ) {
    		
    		return -1;
    	}
    	
    	
    	double[][][] deltaMat = deltaMatrix( mat );
    	
    	//compute \Sigma_p*(a)delta(a)
    	double deltaSum_0 = 0.0;
    	double deltaSum_1 = 0.0;
    	for( int act0 = 0; act0 < actionNums[0]; act0++ )
    		for( int act1 = 0; act1 < actionNums[1]; act1++ ) {
    			
    			deltaSum_0 += nashEquil[act0] * nashEquil[actionNums[0]+act1] * deltaMat[0][act0][act1];
    			deltaSum_1 += nashEquil[act0] * nashEquil[actionNums[0]+act1] * deltaMat[1][act0][act1];
    		}
    	
    	//bound for agent 0
    	double deltaPlusSum_Max = Double.NEGATIVE_INFINITY;
    	for( int act0 = 0; act0 < actionNums[0]; act0++ ) {
    		
    		double deltaPlusSum = 0.0;
    		
    		for( int act1 = 0; act1 < actionNums[1]; act1++ ) {
    			
    			if( deltaMat[0][act0][act1] > 0 )
    				deltaPlusSum += deltaMat[0][act0][act1];
    		}
    		
    		if( deltaPlusSum > deltaPlusSum_Max )
    			deltaPlusSum_Max = deltaPlusSum;
    	}
    	
    	double bound0 = deltaPlusSum_Max - deltaSum_0;
    	
    	
    	//bound for agent 1
    	deltaPlusSum_Max = Double.NEGATIVE_INFINITY;
    	for( int act1 = 0; act1 < actionNums[1]; act1++ ) {
    		
    		double deltaPlusSum = 0.0;
    		
    		for( int act0 = 0; act0 < actionNums[0]; act0++ ) {
    			
    			if( deltaMat[1][act0][act1] > 0 )
    				deltaPlusSum += deltaMat[1][act0][act1];
    		}
    		
    		if( deltaPlusSum > deltaPlusSum_Max )
    			deltaPlusSum_Max = deltaPlusSum;
    	}
    	double bound1 = deltaPlusSum_Max - deltaSum_1;
    	
    	
    	System.out.println("bound 0: "+bound0);
    	System.out.println("bound 1: "+bound1);
    	
    	if( bound0 > bound1 ) 
    		return bound0;
    	else
    		return bound1;
    	
    }
    
	public static void main(String[] args)
	{
		TwoDimMatrix mat = new TwoDimMatrix(4,4);
		
		//direct transfer///////////////////////////
		/**
		TwoDimMatrix matCopy = new TwoDimMatrix(mat);
		
		matCopy.showGameMatrix();
		
		
		double[] nashEquil = matCopy.computeNE();
		
		for( int i = 0; i < nashEquil.length; i++ ) 
			System.out.println( nashEquil[i] );
		
		
		int biasTime = 1;
		for( int i = 0; i < biasTime; i++ )
			mat.biasMatrix();

		mat.showGameMatrix();
		
		
		
		mat.boundDirectTransfer(matCopy, nashEquil);
		*/
		
		mat.showGameMatrix();
		double[] nashEquil = mat.computeNE();
		
		for( int i = 0; i < nashEquil.length; i++ ) 
			System.out.println( nashEquil[i] );
		
		mat.biasMatrix();
		
		mat.showGameMatrix();
		nashEquil = mat.computeNE();
		
		for( int i = 0; i < nashEquil.length; i++ ) 
			System.out.println( nashEquil[i] );
	
	}
	
}
