package linearGridWorld;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Random;
import algorithms.CenCEQ;
import algorithms.DCEQTrans;
import algorithms.ECEQTrans;
import algorithms.NashQ;
import algorithms.DCEQ;
import algorithms.DecenCEQ;
import algorithms.ECEQ;
import algorithms.MARL;
import algorithms.NashQTrans;
import algorithms.PCEQ;
import algorithms.PCEQTrans;
import algorithms.UCEQ;
import algorithms.UCEQTrans;


/**
 * The definition of a grid world game
 * 
 * this is a 2-agent grid world game 
 * with a 3x3 world
 * 
 * allowing stochastic transitions
 */
public class LinearGridWorld
{
    /**
     * important parameters of the grid-world game
     */
    public static final int NUM_AGENTS = 5;//5;
    public static final int NUM_LOCATIONS = 6;
    public static final int WORLD_WIDTH = 6;
    public static final int WORLD_HEIGHT = 1;
    
    /**
     * the location ID of each agent's goal
     */
    private int[] agentGoals;
    private int[] agentInitLocs;
    
    /**
     * the number of episodes in one game
     */
    public static final int episodesNum = 3;//20000;//5000;//10000;//50000;//50000;//5000;//100000;
    //500 for similarity 
    //50000 for error
    
    /**
     * all possible states and all possible joint actions
     */
    public static ArrayList<GameState> allStates;
    public static ArrayList<GameAction> allJointActions;
    
    
    public static final int LOOP = 1;
    
    
    public LinearGridWorld()
    {
	agentGoals = new int[NUM_AGENTS];
	agentInitLocs = new int[NUM_AGENTS];

	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    agentGoals[agent] = agent;
	    agentInitLocs[agent] = (NUM_LOCATIONS-1) - agent;
	}
	
	
	/**
	 * generate all possible state-action pairs
	 */
	generateAllStates();
	generateAllJointActions();
	
    }
    
    public int[] getInitLocationIDs()
    {
	return agentInitLocs;
    }
    
    //right
    public boolean gameOver( GameState gameState ) 
    {
	int[] agentLocationIDs = gameState.getLocationIDs();
	
	if( agentLocationIDs == null || 
		agentLocationIDs.length != NUM_AGENTS ) {
	    
	    System.out.println("LinearGridWorld->gameOver: Game Over With Error!");
	    return false;
	}
	
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    if( agentLocationIDs[agent] != agentGoals[agent] )
		return false;
	}
	
	return true;
    }
    
    //right
    //whether an agent reaches its goal
    public boolean isGoal( int agent, int locationID )
    {
	if( agent < 0 || agent > NUM_AGENTS ||
	    locationID < 0 || locationID > NUM_LOCATIONS ) {
	    
	    System.out.println( "LinearGridWorld->isGoal: Wrong Parameters!");
	    return false;
	}
	
	if( locationID == agentGoals[agent] )
	    return true;
	else
	    return false;
    }
    
    //right
    public int nextLocation( int locationID, int action )
    {
	if( locationID < 0 || locationID >= NUM_LOCATIONS || 
		action < 0 || action >= GameAction.NUM_ACTIONS ) {
	    
	    System.out.println( "LinearGridWorld->nextLocation: Parameter Wrong!");
	    return -1;
	}
	
	
	/**
	 * be careful
	 * we do not test collision and agent out of boundary
	 */
	int retID = locationID;
	if( action == GameAction.RIGHT )
	    retID += 1;
	else if( action == GameAction.LEFT )
	    retID -= 1;
	
	//deal with out of boundary here
	if( retID < 0 )
	    retID = 0;
	else if( retID >= NUM_LOCATIONS )
	    retID = NUM_LOCATIONS-1;
	
	return retID;
    }
    
    
    //check again and again and again
    /**
     * 
     * do actions and set the reward
     */
    public GameState doAction( GameState gameState, GameAction jntAction, double[] rewards, 
	    int[] steps )
    {
	if( gameState == null || jntAction == null ||
	    rewards == null || rewards.length != NUM_AGENTS ) {
	    
	    System.out.println( "LinearGridWorld->doAction: Something Wrong in Parameters!" );
	    return null;
	}
	
	GameState nextState = new GameState(gameState.getLocationIDs());

	
	//the agent in each grid
	int[] agentInGrid = new int[NUM_LOCATIONS];
	for( int loc = 0; loc < NUM_LOCATIONS; loc++ ) {
	    
	    boolean occupied = false;
	    for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
		
		if( gameState.getLocationID(agent) == loc ) {
		    
		    agentInGrid[loc] = agent;
		    occupied = true;
		    break;
		}
	    }
	    
	    //if no agents in this grid
	    if( !occupied ) 
		agentInGrid[loc] = -1; 
	}
	
	
	//for each grid, whether it is determined to 
	//belong to which agent in the next state
	boolean[] determined = new boolean[NUM_LOCATIONS];
	for( int loc = 0; loc < NUM_LOCATIONS; loc++ ) {
	    
	    determined[loc] = false;
	}
	

	/**
	 * first obtain the target location of each agent
	 */
	int[] targetLocation = new int[NUM_AGENTS];
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    targetLocation[agent] = nextLocation( gameState.getLocationID(agent), 
		    				jntAction.getAction(agent) );
	}
	
	/**
	 * the determined next location of each agent
	 */
	int[] nextLocation = new int[NUM_AGENTS];
	
	/**
	 * from left to right
	 * decide for each grid on by one
	 */
	int gridIndex = 0;
	while( gridIndex < NUM_LOCATIONS ) {
	    
	    //find corresponding agent
	    int agentHere = agentInGrid[gridIndex];
	    
	    /**
	     * no agent here
	     * move to the next grid
	     * and the current grid is not determined
	     */
	    if( agentHere == -1 ) {
		
		//determined[gridIndex] = false;
		gridIndex++;
	    }
	    /**
	     * if there is agent in this grid
	     */
	    else {
		
		//if the target location is the current grid
		if( targetLocation[agentHere] == gridIndex ) {
		    
		    nextLocation[agentHere] = gridIndex;
		    determined[gridIndex] = true;
		    gridIndex++;
		}
		//else if the agent wants to move to left
		else if( targetLocation[agentHere] == gridIndex-1 ) {
		    
		    /**
		     * if the target location is determined
		     * then the agent should remain still
		     */
		    if( determined[targetLocation[agentHere]] ) {
			
			nextLocation[agentHere] = gridIndex;
			determined[gridIndex] = true;
			gridIndex++;
		    }
		    /**
		     * else the agent can move to that location
		     */
		    else {
			
			nextLocation[agentHere] = targetLocation[agentHere];
			determined[targetLocation[agentHere]] = true;
			gridIndex++;
		    }
		}
		//else if the agent wants to move to right
		else {
		    
		    int nextGrid = gridIndex+1;
		    int nextNextGrid = gridIndex+2;
		    
		    if( nextNextGrid < NUM_LOCATIONS ) {
			
			int agentNextNextGrid = agentInGrid[nextNextGrid];
			int agentNextGrid = agentInGrid[nextGrid];
			
			/**
			 * if there is an agent in the nextNextGrid
			 */
			if( agentNextNextGrid != -1 ) {
			    
			    /**
			     * the nextNext agent wants to move to the same 
			     * grid as the current agent, then we should 
			     * determine for the 3 agents in the current grid, 
			     * the next grid and the nextNext grid
			     */
			    if( targetLocation[agentNextNextGrid] == 
				    targetLocation[agentHere] ) {
				
				Random random = new Random();
				double stocNum = random.nextDouble();
				
				//the current agent moves
				if( stocNum < 0.3333333 ) {
				    
				    /**
				     * the nextNext agent remains still
				     */
				    nextLocation[agentNextNextGrid] = nextNextGrid;
				    determined[nextNextGrid] = true;
				    
				    /**
				     * whether the current agent can move 
				     * is up to the agent in the next grid
				     */
				    if( agentNextGrid != -1 ) {
					
					/**
					 * the next agent wants to move to the nextNext grid,
					 * but the nextNext agent remains still, 
					 * so the three agents should all remain still
					 */
					if( targetLocation[agentNextGrid] == nextNextGrid) {
					    
					    nextLocation[agentHere] = gridIndex;
					    nextLocation[agentNextGrid] = nextGrid;
					    determined[gridIndex] = true;
					    determined[nextGrid] = true;
					}
					else {
					    
					    nextLocation[agentHere] = nextGrid;
					    nextLocation[agentNextGrid] = gridIndex;
					    determined[gridIndex] = true;
					    determined[nextGrid] = true;
					}
				    }
				    /**
				     * there is no agent in the next grid
				     * just move
				     */
				    else {
					
					nextLocation[agentHere] = nextGrid;
					determined[nextGrid] = true;
				    }
				    
				    gridIndex += 3;
				}
				//the nextNext agent moves
				else if( stocNum < 0.666666667 ) {
				    
				    /**
				     * the current agent remains still
				     */
				    nextLocation[agentHere] = gridIndex;
				    determined[gridIndex] = true;
				  
				    /**
				     * whether the nextNext agent can move 
				     * is up to the agent in the next grid
				     */
				    if( agentNextGrid != -1 ) {
					
					/**
					 * if the next agent wants to move to the curernt grid,
					 * but the current agent remains still, 
					 * so the three agents should all remain still
					 */
					if( targetLocation[agentNextGrid] == gridIndex ) {
					    
					    nextLocation[agentNextNextGrid] = nextNextGrid;
					    nextLocation[agentNextGrid] = nextGrid;
					    determined[nextGrid] = true;
					    determined[nextNextGrid] = true;
					}
					else {
					    
					    nextLocation[agentNextNextGrid] = nextGrid;
					    nextLocation[agentNextGrid] = nextNextGrid;
					    determined[nextGrid] = true;
					    determined[nextNextGrid] = true;
					}
				    }
				    /**
				     * there is no agent in the next grid 
				     * just move
				     */
				    else {
					
					nextLocation[agentNextNextGrid] = nextGrid;
					determined[nextGrid] = true;
				    }
				    
				    gridIndex += 3;
				}
				//the three agents should remain still
				else {
				    
				    nextLocation[agentHere] = gridIndex;
				    nextLocation[agentNextNextGrid] = nextNextGrid;
				    determined[gridIndex] = true;
				    determined[nextNextGrid] = true;
				    if( agentNextGrid != -1 ) {
					
					nextLocation[agentNextGrid] = nextGrid;
					determined[nextGrid] = true;
				    }
				    gridIndex += 3;
				}
			    }
			    /**
			     * else, if the nextNext agent wants to move right 
			     * there is one case should be considered:
			     * if the nextNext Grid is the last grid and the 
			     * nextNext agent wants to move right, in which 
			     * case it will remain still 
			     */
			    else {
				
				/**
				 * first, if there is no agent in the next grid
				 * then directly move to the next grid
				 */
				if( agentNextGrid == -1 ) {
				    
				    nextLocation[agentHere] = nextGrid;
				    determined[nextGrid] = true;
				    gridIndex++;
				}
				/**
				 * if there is an agent in the next grid,
				 * but its target is gridIndex,
				 * then exchange the two agents locations
				 */
				else if( targetLocation[agentNextGrid] == gridIndex ) {
				    
				    nextLocation[agentHere] = nextGrid;
				    nextLocation[agentNextGrid] = gridIndex;
				    determined[gridIndex] = true;
				    determined[nextGrid] = true;
				    gridIndex += 2;
				}
				/**
				 * the last case is the most complex,
				 * the agent in the next grid also wants to move right,
				 * the agent in the nextNext grid also wants to move right
				 * so check all grids one by one from current grid to right, 
				 * until one empty grid or an agent which wants to move to 
				 * left is found!
				 */
				else {
				    
				    int gridIterator = nextNextGrid+1;
				    
				    /**
				     * if the nextNext grid is already the last grid
				     */
				    if( gridIterator == NUM_LOCATIONS ) {
					
					for( int grid = gridIndex; grid < gridIterator; grid++ ) {
					    
					    nextLocation[agentInGrid[grid]] = grid;
					    determined[grid] = true; 
					}
					gridIndex = gridIterator;
				    }
				    
				    while( gridIterator < NUM_LOCATIONS ) {
					
					int agentIterator = agentInGrid[gridIterator];
					
					/**
					 * if no agent in the current grid
					 */
					if( agentIterator == -1 ) {
					    
					    int gridNextIterator = gridIterator+1;
					    
					    /**
					     * if gridIterator is already the last grid
					     * then all agents from gridIndex to 
					     * gridIterator-1 can move right
					     */
					    if( gridNextIterator == NUM_LOCATIONS ) {
						
						for( int grid = gridIndex; grid < gridIterator; grid++ ) {
						    
						    nextLocation[agentInGrid[grid]] = grid+1;
						    determined[grid+1] = true;
						}
						gridIndex = gridIterator;
						
						//jump out of the loop
						break;
					    }

					    else {
						
						int agentNextIterator = agentInGrid[gridNextIterator];
						
						/**
						 * if there is no agent in the nextIterator 
						 * or this agent wants to move right
						 * all agents from gridIndex to gridIterator-1 can move 
						 * to right
						 */
						if( agentNextIterator == -1 || 
							targetLocation[agentNextIterator] != gridIterator ) {
						    
						    for( int grid = gridIndex; grid < gridIterator; grid++ ) {
							
							nextLocation[agentInGrid[grid]] = grid+1;
							determined[grid+1] = true;
						    }
						    gridIndex = gridIterator;
						    //jump out of the loop
						    break;
						}
						/**
						 * else, the agent in the nextIterator wants to move to left
						 * so from gridIndex to gridIterator-2, 
						 * all agents should remain
						 */
						else {
						    
						    double stocNum = new Random().nextDouble();
						    
						    /**
						     * if the agent in gridIterator is sampled to move right,
						     * then all agents from gridIndex to gridIterator-1 move to right,
						     * agent in gridNextIterator should remain still
						     */
						    if( stocNum < 0.333333333 ) {
							
							for( int grid = gridIndex; grid < gridIterator; grid++ ) {
							    
							    nextLocation[agentInGrid[grid]] = grid+1;
							    determined[grid+1] = true;
							}
							nextLocation[agentNextIterator] = gridNextIterator;
							determined[gridNextIterator] = true;
						    }
						    /**
						     * all agents from gridIndex to gridIterator-1 remain still
						     * the agent in gridNextIterator is sampled to move to left
						     */
						    else if( stocNum < 0.66666667 ) {
							
							for( int grid = gridIndex; grid < gridIterator; grid++ ) {
							    
							    nextLocation[agentInGrid[grid]] = grid;
							    determined[grid] = true; 
							}
							nextLocation[agentNextIterator] = gridIterator;
							determined[gridIterator] = true;
						    }
						    /**
						     * all agents are sampled to remain still
						     */
						    else {
							
							for( int grid = gridIndex; grid < gridIterator; grid++ ) {
							    
							    nextLocation[agentInGrid[grid]] = grid;
							    determined[grid] = true; 
							}
							nextLocation[agentNextIterator] = gridNextIterator;
							determined[gridNextIterator] = true;
						    }
						    
						    /**
						     * all agents from gridIndex to gridNextIterator 
						     * are determined
						     */
						    gridIndex = gridNextIterator+1;
						    
						    //jump out of the loop
						    break;
						}
					    }
					}
					/**
					 * else if this agent wants to move to left, 
					 * then from gridIterator-3 to gridIndex, 
					 * all agents should remain still
					 */
					else if( targetLocation[agentIterator] == gridIterator-1 ) {
					    
					    for( int grid = gridIterator-3; grid >= gridIndex; grid-- ) {
						
						nextLocation[agentInGrid[grid]] = grid;
						determined[grid] = true; 
					    }
					    
					    gridIndex = gridIterator-2;
					    break;
					    //jump out of the loop
					}
					/**
					 * else, continue moving to the right grid
					 */
					else {
					    
					    //if gridIterator is the last grid
					    if( gridIterator == (NUM_LOCATIONS-1) ) {
						
						for( int grid = gridIndex; grid <= gridIterator; grid++ ) {
						    
						    nextLocation[agentInGrid[grid]] = grid;
						    determined[grid] = true; 
						}
						gridIndex = gridIterator+1;
						break;
					    }
					    else
						gridIterator++;
					}
				    }   
				}
			    }
			}
			/**
			 * no agent in the next next grid
			 * directly move to the next grid
			 */
			else {
			    
			    /**
			     * if there is no agent in the next grid
			     * or the next agent wants to move left
			     * then they can move
			     */
			    if( agentNextGrid == -1 ) {
				
				nextLocation[agentHere] = nextGrid;
				determined[nextGrid] = true;
				gridIndex++;
			    }
			    else if( targetLocation[agentNextGrid] == gridIndex ) {
				
				nextLocation[agentHere] = nextGrid;
				nextLocation[agentNextGrid] = gridIndex;
				determined[nextGrid] = true;
				determined[gridIndex] = true;
				gridIndex += 2;
			    }
			    /**
			     * if the next agent wants to move right
			     * then it is complex
			     */
			    else {
				
				//////////////////////////////////////////////////////////
				int gridIterator = nextGrid+1;
				    
				/**
				 * if the nextNext grid is already the last grid
				 */
				if( gridIterator == NUM_LOCATIONS ) {
					
				    for( int grid = gridIndex; grid < gridIterator; grid++ ) {
					
					nextLocation[agentInGrid[grid]] = grid;
					determined[grid] = true; 
				    }
				    gridIndex = gridIterator;
				}
				    
				while( gridIterator < NUM_LOCATIONS ) {
					
				    int agentIterator = agentInGrid[gridIterator];
					
				    /**
				     * if no agent in the current grid
				     */
				    if( agentIterator == -1 ) {
					    
					int gridNextIterator = gridIterator+1;
					    
					/**
					 * if gridIterator is already the last grid
					 * then all agents from gridIndex to 
					 * gridIterator-1 can move right
					 */
					if( gridNextIterator == NUM_LOCATIONS ) {
						
					    for( int grid = gridIndex; grid < gridIterator; grid++ ) {
						    
						nextLocation[agentInGrid[grid]] = grid+1;
						determined[grid+1] = true;
					    }
					    gridIndex = gridIterator;
						
					    //jump out of the loop
					    break;
					}

					else {
						
					    int agentNextIterator = agentInGrid[gridNextIterator];
						
					    /**
					     * if there is no agent in the nextIterator 
					     * or this agent wants to move right
					     * all agents from gridIndex to gridIterator-1 can move 
					     * to right
					     */
					    if( agentNextIterator == -1 || 
						    targetLocation[agentNextIterator] != gridIterator ) {
						    
						for( int grid = gridIndex; grid < gridIterator; grid++ ) {
							
						    nextLocation[agentInGrid[grid]] = grid+1;
						    determined[grid+1] = true;
						}
						gridIndex = gridIterator;
						//jump out of the loop
						break;
					    }
					    /**
					     * else, the agent in the nextIterator wants to move to left
					     */
					    else {
						    
						double stocNum = new Random().nextDouble();
						    
						/**
						 * if the agent in gridIterator is sampled to move right,
						 * then all agents from gridIndex to gridIterator-1 move to right,
						 * agent in gridNextIterator should remain still
						 */
						if( stocNum < 0.333333333 ) {
							
						    for( int grid = gridIndex; grid < gridIterator; grid++ ) {
							    
							nextLocation[agentInGrid[grid]] = grid+1;
							determined[grid+1] = true;
						    }
						    nextLocation[agentNextIterator] = gridNextIterator;
						    determined[gridNextIterator] = true;
						}
						/**
						 * all agents from gridIndex to gridIterator-1 remain still
						 * the agent in gridNextIterator is sampled to move to left
						 */
						else if( stocNum < 0.66666667 ) {
							
						    for( int grid = gridIndex; grid < gridIterator; grid++ ) {
							    
							nextLocation[agentInGrid[grid]] = grid;
							determined[grid] = true; 
						    }
						    nextLocation[agentNextIterator] = gridIterator;
						    determined[gridIterator] = true;
						}
						/**
						 * all agents are sampled to remain still
						 */
						else {
							
						    for( int grid = gridIndex; grid < gridIterator; grid++ ) {
							    
							nextLocation[agentInGrid[grid]] = grid;
							determined[grid] = true; 
						    }
						    nextLocation[agentNextIterator] = gridNextIterator;
						    determined[gridNextIterator] = true;
						}
						
						/**
						 * all agents from gridIndex to gridNextIterator 
						 * are determined
						 */
						gridIndex = gridNextIterator+1;
						    
						//jump out of the loop
						break;
					    }
					}
				    }
				    /**
				     * else if this agent wants to move to left, 
				     * then from gridIterator-3 to gridIndex, 
				     * all agents should remain still
				     */
				    else if( targetLocation[agentIterator] == gridIterator-1 ) {
					    
					for( int grid = gridIterator-3; grid >= gridIndex; grid-- ) {
						
					    nextLocation[agentInGrid[grid]] = grid;
					    determined[grid] = true; 
					}
					
					gridIndex = gridIterator-2;
					break;
					//jump out of the loop
				    }
				    /**
				     * else, continue moving to the right grid
				     */
				    else {
					    
					//if gridIterator is the last grid
					if( gridIterator == (NUM_LOCATIONS-1) ) {
						
					    for( int grid = gridIndex; grid <= gridIterator; grid++ ) {
						    
						nextLocation[agentInGrid[grid]] = grid;
						determined[grid] = true; 
					    }
					    gridIndex = gridIterator+1;
					    break;
					}
					else
					    gridIterator++;
				    }
				}  
				/////////////////////////////////////////////////////////////////
			    }
			}
		    }
		    else if( nextGrid < NUM_LOCATIONS ) {
			
			/**
			 * if there is an agent in the next grid
			 */
			if( agentInGrid[nextGrid] != -1 ) {
			    
			    int agentNextGrid = agentInGrid[nextGrid];
			    /**
			     * if the agent in the next grid (also the last grid)
			     * wants to move left
			     * then exchange the grids of the two agents
			     */
			    if( targetLocation[agentNextGrid] == gridIndex ) {
				
				nextLocation[agentHere] = nextGrid;
				nextLocation[agentNextGrid] = gridIndex;
				determined[nextGrid] = true;
				determined[gridIndex] = true;
				gridIndex += 2;
			    }
			    
			    /**
			     * if the agent in the next grid wants to move right, 
			     * in which case it will remain still, 
			     * then both agents should remain still
			     */
			    else {
				
				nextLocation[agentHere] = gridIndex;
				nextLocation[agentNextGrid] = nextGrid;
				determined[nextGrid] = true;
				determined[gridIndex] = true;
				gridIndex += 2;
			    }
			}
			/**
			 * no agent there, directly move to that grid
			 */
			else {
			    
			    nextLocation[agentHere] = targetLocation[agentHere];
			    determined[targetLocation[agentHere]] = true;
			    gridIndex++;
			}
		    }
		    //this is the last grid, remain still
		    else {
			
			nextLocation[agentHere] = gridIndex;
			determined[gridIndex] = true;
			gridIndex++;
		    }
		}
		
	    }
		
	}
	
	
	/**
	 * then set the next state
	 */
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    nextState.setLocationID(agent, nextLocation[agent]);
	}

	
	//debug
	if( !availableState(nextState.getLocationIDs()) ) {
	    
	    System.out.print("NextState: ");
	    for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
		
		System.out.print(nextState.getLocationID(agent)+" ");
	    }
	    System.out.println();
	    
	    System.out.print("LastState: ");
	    for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
		
		System.out.print(gameState.getLocationID(agent)+" ");
	    }
	    System.out.println();
	    
	    System.out.print("JointAction: ");
	    for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
		
		System.out.print(GameAction.getActionString(jntAction.getAction(agent))+" ");
	    }
	    System.out.println();
	}
	
	/**
	 * set the reward
	 */
	if( gameOver( nextState ) ) {
	    
	    for( int agent = 0; agent < NUM_AGENTS; agent++ )
		rewards[agent] = 500;
	}
	else {
	    
	    int count = 0;
	    for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
		
		if( nextLocation[agent] == agentGoals[agent] ) {
		    
		    count++;
		}
	    }
	    for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
		
		if( count == 0 )
		    rewards[agent] = -5;//-1 for nashQ
		else
		    rewards[agent] = count; 
	    }
	}
	
	return nextState;
    }
    
    //right
    public boolean outOfBoundary( int locationID )
    {
	if( locationID < 0 || locationID >= NUM_LOCATIONS ) {
	    
	    return true;
	}
	else 
	    return false;
    }
    
    
    /**
     * one test contains several algorithms run
     */
    public void oneTest()
    {
	int[] algTypes = new int[]{ //
		
		//MARL.uCEQ_TRANS,
		//MARL.dCEQ_TRANS, 
		//MARL.eCEQ_TRANS, MARL.pCEQ_TRANS,		
		//MARL.uCEQ,
		//MARL.eCEQ, 
		//MARL.pCEQ,
		//MARL.dCEQ
		MARL.NASHQ_TRANS
		//MARL.NASHQ
		};

	
	//int[] algTypes = new int[]{ MARL.dCEQ };
	
	for( int algIndex = 0; algIndex < algTypes.length; algIndex++ ) {
	    
	    
	    int algType = algTypes[algIndex];
	    
	    if( algType == MARL.uCEQ || algType == MARL.eCEQ ||
		    algType == MARL.pCEQ || algType == MARL.NASHQ ||
		    algType == MARL.NASHQ_TRANS || algType == MARL.NASHQ_SUPP_TRANS || 
		    algType == MARL.uCEQ_TRANS || algType == MARL.eCEQ_TRANS ||
		    algType == MARL.pCEQ_TRANS )
		oneRun( algType, true );
	    else
		oneRun( algType, false );
	}
    }
    
    /**
     * one run contains one algorithm's several episodes 
     */
    public void oneRun( int algType, boolean isCentral )
    {
	
	int LOOP = 1;
	
	/**
	 * choose the algorithm
	 */
	String algStr = MARL.ALG_STRINGS[algType];
	
	
	/**
	 * for recording steps
	 */
	double[][] stepNums = new double[NUM_AGENTS][episodesNum];
	double[][] allRewards = new double[NUM_AGENTS][episodesNum];
	for( int agent = 0; agent < LinearGridWorld.NUM_AGENTS; agent++ )
	    for( int ep = 0; ep < episodesNum; ep++ ) {
		
		stepNums[agent][ep] = 0; 
		allRewards[agent][ep] = 0;
	    }
	
	/**
	 * for recording time duration
	 */
	long[] durTimes = new long[episodesNum];
	    
	//for the time of the hole process
	long beginTime = System.nanoTime();
		
	for( int loop = 1; loop <= LOOP; loop++ ) {
	
	    System.out.println("Algorithm: "+algStr+" the "+loop+"-th loop===========");
		
	    /**
	     * for centralized algorithms
	     */
	    MARL agent = createMARL( algType, 0 );
		
	    /**
	     * for decentralized algorithms
	     */
	    MARL[] agents = new MARL[LinearGridWorld.NUM_AGENTS];
	    for( int agentIndex = 0; agentIndex < LinearGridWorld.NUM_AGENTS; agentIndex++ ) {
		
		agents[agentIndex] = createMARL( algType, agentIndex );
	    }
	    
	    
	    /**
	    if( isCentral )
		agent.gameStarted(loop);
	    else {
		
		agent0.gameStarted(loop);
		//agent1.gameStarted(loop);
	    }
	    */
	    
	    
	    double[][][] retArray = new double[episodesNum][2][LinearGridWorld.NUM_AGENTS];
	    for( int ep = 0; ep < episodesNum; ep++ ) {
		    
		long startTime = System.nanoTime();
		
		
		if( isCentral )
		    retArray[ep] = oneEpisodeCentral( agent );
		else {
		    
		    retArray[ep] = oneEpisode( agents );
		   
		}
		long endTime = System.nanoTime();
		durTimes[ep] = endTime - startTime;
		   
		for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
		    
		    stepNums[agentIndex][ep] += retArray[ep][0][agentIndex];
		    allRewards[agentIndex][ep] += retArray[ep][1][agentIndex];
		}
		      
		/**
		 * display the progress
		 */
		displayProgress( ep+1 );   
	    }
	    
	    
	    //release?
	    retArray = null;
	    
	    //one loop finished
	    /**
	    if( isCentral )
		agent.gameFinished(loop);
	    else {
		
		agent0.gameFinished(loop);
		//agent1.gameFinished(loop);
	    }
	    */
	    
	}
	
	long overTime = System.nanoTime();
	
	/**
	 * write the steps and times into files
	 */
	try
	{
	    
	    //write times
	    /**/
	    BufferedWriter timeWriter = new BufferedWriter(new FileWriter("./" + algStr+ "_time.csv"));
	    for( int ep = 0; ep < episodesNum; ep++ ) {
		
		timeWriter.write( durTimes[ep] + ", ");
	    }
	    timeWriter.close();
	    
	    
	    timeWriter = new BufferedWriter(new FileWriter("./" + algStr+ "_allTime.txt"));
	    timeWriter.write(""+((overTime-beginTime)/1000000000.0/LOOP));
	    timeWriter.close();
	    
	    
	    //write steps
	    /**/
	    for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
		
		BufferedWriter stepsWriter = new BufferedWriter(new FileWriter("./" + algStr+"_agent" + agentIndex + ".csv"));
		for( int ep = 0; ep < episodesNum; ep++ ) {
		    
		    stepsWriter.write( (stepNums[agentIndex][ep] / (double)LOOP) + ", ");
		}
		stepsWriter.close();
	    }
	    
	    
	    //write rewards
	    /**/
	    for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
		
		BufferedWriter rewardsWriter = new BufferedWriter(new FileWriter("./rewd_" + algStr+"_agent" + agentIndex + ".csv"));
		for( int ep = 0; ep < episodesNum; ep++ ) {
		    
		    rewardsWriter.write( allRewards[agentIndex][ep] / LOOP + ", ");
		}
		rewardsWriter.close();
	    }
	    
	    
	}
	catch (IOException e)
	{
	    e.printStackTrace();
	}
	
    }
    
    /**
     * 
     * @return two arrays, one for steps, one for rewards
     */
    public double[][] oneEpisode( MARL[] agents )
    {
	if( agents == null || 
		agents.length != LinearGridWorld.NUM_AGENTS ) {
	    
	    return null;
	}
	
	/**
	 * dimension 0: steps
	 * dimension 1: rewards
	 */
	double[][] retArray = new double[2][NUM_AGENTS];
	
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    retArray[0][agent] = 0.0;
	    retArray[1][agent] = 0.0;
	}
	
	
	int[] stepNums = new int[NUM_AGENTS];
	for( int agent = 0; agent < NUM_AGENTS; agent++ )
	    stepNums[agent] = 0;
	
	
	/**
	 * init the states
	 */
	GameState gameState = new GameState( agentInitLocs );
	
	/**
	 * get the action expected taken by each agent
	 */
	GameAction[] jntActions = new GameAction[LinearGridWorld.NUM_AGENTS];
	for( int agentIndex = 0; agentIndex < LinearGridWorld.NUM_AGENTS; agentIndex++ ) {
	    
	    jntActions[agentIndex] = agents[agentIndex].updateQ( null, null, null, gameState);
	}
	
	/**
	 * the action actually taken
	 */
	GameAction jntAction = new GameAction();
	
	/**
	 * the reward for each agent in each transfer
	 */
	double[] rewards = new double[NUM_AGENTS];
	    for( int agent = 0; agent < NUM_AGENTS; agent++ )
		rewards[agent] = 0.0;
	
	while( !gameOver( gameState ) ) {
	    
	    /**
	     * epsilon-greedy and get the action to be taken actually
	     */
	    for( int agentIndex = 0; agentIndex < LinearGridWorld.NUM_AGENTS; agentIndex++ ) {
		    
		int agentAction = jntActions[agentIndex].getAction(agentIndex);
		jntAction.setAction( agentIndex, agents[agentIndex].epsilonGreedy( agentAction) );
	    }
	    
	    //record the steps
	    for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
		stepNums[agent] = stepNums[agent] + 1; 
		retArray[0][agent] = retArray[0][agent] + 1;
	    }
	    
	    /**
	     * observe the next state and get the rewards
	     */
	    GameState nextState = doAction( gameState, jntAction, rewards, stepNums );
	    
	    //record the rewards
	    for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
		
		retArray[1][agent] = retArray[1][agent] + rewards[agent];  
	    }
	    
	    /**
	     * update Q-values
	     */
	    for( int agentIndex = 0; agentIndex < LinearGridWorld.NUM_AGENTS; agentIndex++ ) {
		    
		jntActions[agentIndex] = agents[agentIndex].updateQ( gameState, jntAction, rewards, nextState);
	    }
	    
	    /**
	     * transfer to the next state
	     */
	    gameState = null; //??
	    gameState = nextState;
	    
	}
	
	//compute the average reward
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    if( retArray[0][agent] < 0.00000001 )
		retArray[1][agent] = 0.0;
	    
	    else {
		retArray[1][agent] = retArray[1][agent] / retArray[0][agent]; 
	    }
	}
	
	return retArray;
    }
    
    /**
     * 
     * @return two arrays, one for steps, one for rewards
     */
    public double[][] oneEpisodeCentral( MARL agent )
    {
	/**
	 * dimension 0: steps
	 * dimension 1: rewards
	 */
	double[][] retArray = new double[2][NUM_AGENTS];
	
	for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
	    
	    retArray[0][agentIndex] = 0.0;
	    retArray[1][agentIndex] = 0.0;
	}
	
	int[] stepNums = new int[NUM_AGENTS];
	for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ )
	    stepNums[agentIndex] = 0;
	
	/**
	 * init the states
	 */
	GameState gameState = new GameState( agentInitLocs );
	
	/**
	 * get the action expected taken by each agent
	 */
	GameAction jntAction = agent.updateQ( null, null, null, gameState );
	
	/**
	 * the reward for each agent in each transfer
	 */
	double[] rewards = new double[NUM_AGENTS];
	    for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ )
		rewards[agentIndex] = 0.0;
	
	while( !gameOver( gameState ) ) {
	    
	    /**
	     * epsilon-greedy and get the action to be taken actually
	     */
	    jntAction = agent.epsilonGreedy(jntAction);
	    
	    for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
		    
		stepNums[agentIndex] = stepNums[agentIndex] + 1;
		retArray[0][agentIndex] = retArray[0][agentIndex] + 1;
	    }
	    
	    /**
	     * observe the next state and get the rewards
	     */
	    GameState nextState = doAction( gameState, jntAction, rewards, stepNums );
	    
	    //record the rewards
	    for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
		
		retArray[1][agentIndex] = retArray[1][agentIndex] + rewards[agentIndex];  
	    }
	    
	    /**
	     * update Q-values
	     */
	    GameAction nextAction = agent.updateQ( gameState, jntAction, rewards, nextState );
	    
	    /**
	     * transfer to the next state
	     */
	    gameState = null; //??
	    gameState = nextState;
	    jntAction = null; //??
	    jntAction = nextAction;
	    
	}
	
	//compute the average reward
	for( int agentIndex = 0; agentIndex < NUM_AGENTS; agentIndex++ ) {
	    
	    if( retArray[0][agentIndex] < 0.00000001 )
		retArray[1][agentIndex] = 0.0;
	    
	    else {
		retArray[1][agentIndex] = retArray[1][agentIndex] / retArray[0][agentIndex]; 
	    }
	}
	
	return retArray;
    }
    
    
    
    private void displayProgress( int ep )
    {
	
	NumberFormat numberFormat = NumberFormat.getNumberInstance();
	numberFormat.setMaximumFractionDigits(2);
	numberFormat.setMinimumFractionDigits(2);
	numberFormat.setMaximumIntegerDigits(3);
	numberFormat.setMinimumIntegerDigits(3);
	
	
	if( ep == 1)
	    System.out.println("Progress:" + numberFormat.format(100 * (double) ep / episodesNum) + "%");
	else if( ep % 100 == 0)
	{
	    System.out.println("Progress:" + numberFormat.format(100 * (double) ep / episodesNum) + "%");
	}
    }
    
    private MARL createMARL( int alg, int agent )
    {
	switch( alg )
	{
	case MARL.DENCE_CEQ:
	    return new DecenCEQ( agent );
	case MARL.CEN_CEQ:
	    return new CenCEQ();
	case MARL.uCEQ:
	    return new UCEQ();
	case MARL.eCEQ:
	    return new ECEQ();
	case MARL.pCEQ:
	    return new PCEQ();
	case MARL.dCEQ:
	    return new DCEQ( agent );
	case MARL.NASHQ:
	    return new NashQ();
	case MARL.NASHQ_TRANS:
	    return new NashQTrans();
	case MARL.uCEQ_TRANS:
	    return new UCEQTrans();
	case MARL.eCEQ_TRANS:
	    return new ECEQTrans();
	case MARL.pCEQ_TRANS:
	    return new PCEQTrans();
	case MARL.dCEQ_TRANS:
	    return new DCEQTrans( agent );
	default:
	    return new MARL( agent );
	}
    }
    
	
    //right
    private void generateAllStates()
    {
	
	allStates = new ArrayList<GameState>();
	
	int[] stateIterator = new int[NUM_AGENTS];
	
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    stateIterator[agent] = 0; //loc 0
	}
	
	/**
	 * for each agent 
	 */
	
	while( true ) {
	    
	    /**
	     * check the current location
	     */
	    if( availableState( stateIterator ) ) {
		
		
		GameState gameState = new GameState( stateIterator );
		
		if( !allStates.contains( gameState ) ) {
		    
		    allStates.add( gameState );
		}
		else {
		    
		    
		    gameState = null;
		}
	    }
	    
	    
	    /**
	     * move to the next location
	     */
	    for( int agent = NUM_AGENTS-1; agent >= 0; agent-- ) {
		
		stateIterator[agent] += 1;
		
		if( agent > 0 && stateIterator[agent] >= NUM_LOCATIONS ) {
		    
		    stateIterator[agent] = 0;
		}
		else
		    break;
	    }
	    
	    
	    /**
	     * check the stop condition
	     */
	    if( stateIterator[0] >= NUM_LOCATIONS ) {
		
		break;
	    }
	}
	
	
    }
	
    public static int queryJointActionIndex( GameAction jntAction )
    {
	if( jntAction == null ) {
	    
	    return -1;
	}
	else if( allJointActions == null ) {
	    
	    return -1;
	}
	
	for( int listIndex = 0; listIndex < allJointActions.size(); listIndex++ ) {
	    
	    GameAction gameAction = allJointActions.get( listIndex );
	    
	    if( gameAction.equals( jntAction ) ) {
		
		return listIndex;
	    }
	}
	
	return -1;
    }
    
    //right
    private void generateAllJointActions()
    {
	allJointActions = new ArrayList<GameAction>();
	
	int[] actionIterator = new int[NUM_AGENTS];
	
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    actionIterator[agent] = GameAction.LEFT;
	}
	
	while( true ) {
	    
	    GameAction gameAction = new GameAction( actionIterator );
	    
	    if( !allJointActions.contains( gameAction ) ) {
		
		allJointActions.add( gameAction );
	    }
	    else
		gameAction = null;
	    
	    /**
	     * move to the next location
	     */
	    for( int agent = NUM_AGENTS-1; agent >= 0; agent-- ) {
		
		actionIterator[agent] += 1;
		
		if( agent > 0 && actionIterator[agent] >= GameAction.NUM_ACTIONS ) {
		    
		    actionIterator[agent] = 0;
		}
		else
		    break;
	    }
	    
	    
	    /**
	     * check the stop condition
	     */
	    if( actionIterator[0] >= GameAction.NUM_ACTIONS ) {
		
		break;
	    }
	}
    }
    
    //right
    private boolean availableState( int[] locationIDs ) 
    {
	
	if( locationIDs == null || 
		locationIDs.length != NUM_AGENTS ) {
	    
	    System.out.println("availableState: Wrong Parameter!");
	    return false;
	}
	
	
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	    
	    int loc = locationIDs[agent];
	    
	    for( int agent_p = agent+1; agent_p < NUM_AGENTS; agent_p++ ) {
		
		int loc_p = locationIDs[agent_p];
		
		if( loc == loc_p )
		    return false;
	    }
	}
	
	return true;
    }
    
    /**
     * @param args
     */
    public static void main(String[] args)
    {
	// TODO Auto-generated method stub
	
	LinearGridWorld gridWorld = new LinearGridWorld();
	
	gridWorld.oneTest();

    }
}
