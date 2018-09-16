package metaQ;

import gameGridWorld.GameAction;

import java.util.ArrayList;
import java.util.Random;

public class MatrixGame
{

    /**
     * utility matrix for each agent
     */
    private double[][] M1;
    private double[][] M2;
    
    private int ACTION_NUM_AGENT1 = 5;
    private int ACTION_NUM_AGENT2 = 5;
    
    public MatrixGame()
    {
	M1 = new double[ACTION_NUM_AGENT1][ACTION_NUM_AGENT2];
	M2 = new double[ACTION_NUM_AGENT1][ACTION_NUM_AGENT2];
	
	Random random = new Random();
	
	for( int a = 0; a < ACTION_NUM_AGENT1; a++ ) 
	    for( int b = 0; b < ACTION_NUM_AGENT2; b++ ) {
		
		M1[a][b] = random.nextDouble()*random.nextInt(50);
		M2[a][b] = random.nextDouble()*random.nextInt(50);
	    }
	
    }
    
    
    public void regenerateMatrix()
    {
	Random random = new Random();
	
	for( int a = 0; a < ACTION_NUM_AGENT1; a++ ) 
	    for( int b = 0; b < ACTION_NUM_AGENT2; b++ ) {
		
		M1[a][b] = random.nextDouble()*random.nextInt(50);
		M2[a][b] = random.nextDouble()*random.nextInt(50);
	    }
    }
    

    public void displayMatrix()
    {
	
	System.out.println( "Matrix M1================= ========================" );
	for( int a = 0; a < ACTION_NUM_AGENT1; a++ ) {
	    
	    for( int b = 0; b < ACTION_NUM_AGENT2; b++ ) {
		
		System.out.print( M1[a][b] + "    " );
	    }
	    System.out.println();
	}
	System.out.println( "Matrix M1=========================================" );
	
	
	System.out.println( "Matrix M2=========================================" );
	for( int a = 0; a < ACTION_NUM_AGENT1; a++ ) {
	    
	    for( int b = 0; b < ACTION_NUM_AGENT2; b++ ) {
		
		System.out.print( M2[a][b] + "    " );
	    }
	    System.out.println();
	}
	System.out.println( "Matrix M2=========================================" );
    }
    
    public ArrayList<GameAction> negotiation()
    {
        //agent 1
        ArrayList<GameAction> J1 = new ArrayList<GameAction>();
        for( int a = 0; a < ACTION_NUM_AGENT1; a++ ) {
            for( int b = 0; b < ACTION_NUM_AGENT2; b++ ) {
        	
        	/**
        	 * find agent1's max action against action b
        	 */
        	int maxAction_a = a;
    	    	for( int a_p = 0; a_p < ACTION_NUM_AGENT1; a_p++ ) {
    		
    	    	    if( M1[a_p][b] > 
    	    	    	M1[maxAction_a][b] ) {
    		    
    	    		maxAction_a = a_p;
    	    	    }
    	    	}
    	    
    	    	/**
    	    	 * if action a is the max action
    	    	 */
    	    	if( maxAction_a == a ) {
    		
    	    	    //record the action (a,b) for agent 1
    	    	    GameAction jointAction = new GameAction( new int[] {a,b} );
    	    	    if( !J1.contains(jointAction) )
    		   	J1.add(jointAction);
    	    	}
    	    	/**
    	    	 * then ask agent2 whether (maxAction,b) is a Nash equilibrium
    	    	 */
    	    	else {
    		
    	    	    int maxAction_b = b;
    	    	    for( int b_p = 0; b_p < ACTION_NUM_AGENT2; b_p++ ) {
    		    
    	    		if( M2[maxAction_a][b_p] > 
    	    		    M2[maxAction_a][maxAction_b] ) {
    			
    	    		    maxAction_b = b_p;
    	    		}
    	    	    }
    		
    	    	    if( maxAction_b == b ) {
    		    
    	    		//record the action (maxAction_a,b) for agent 1
    	    		GameAction jointAction = new GameAction( new int[] {maxAction_a,b} );
    	    		if( !J1.contains(jointAction) )    
    	    		    J1.add(jointAction);
    	    	    }
    	    	    /**
    	    	     * else we judge whether (a,b) dominate (maxAction_a,maxAction_b)
    	    	     */
    	    	    else if( M1[a][b] > M1[maxAction_a][maxAction_b] ){
    		    
    	    		//record (a,b) for agent1
    	    		GameAction jointAction = new GameAction( new int[] {a,b} );
    	    		if( !J1.contains(jointAction) )    
    	    		    J1.add(jointAction);
    	    	    }
    	    	}
            }
        }
        
        
        //agent 2
        ArrayList<GameAction> J2 = new ArrayList<GameAction>();
        for( int b = 0; b < ACTION_NUM_AGENT2; b++ ) {
            
            for( int a = 0; a < ACTION_NUM_AGENT1; a++ ) {
    	    
    	    	/**
    	    	 * find agent2's max action against action a
    	    	 */
    	    	int maxAction_b = b;
    	    	for( int b_p = 0; b_p < ACTION_NUM_AGENT2; b_p++ ) {
    		
    	    	    if( M2[a][b_p] > M2[a][maxAction_b] ) {
    		    
    	    		maxAction_b = b_p;
    	    	    }
    	    	}
    	    
    	    	/**
    	    	 * if action b is the max action
    	    	 */
    	    	if( maxAction_b == b ) {
    		
    	    	    //record the action (a,b) for agent 2
    	    	    GameAction jointAction = new GameAction( new int[] {a,b} );
    	    	    if( !J2.contains(jointAction) )
    	    		J2.add(jointAction);
    	    	}
    	    	/**
    	    	 * then ask agent1 whether (a, maxAction_b) is a Nash equilibrium
    	    	 */
    	    	else {
    		
    	    	    int maxAction_a = a;
    	    	    for( int a_p = 0; a_p < ACTION_NUM_AGENT1; a_p++ ) {
    		    
    	    		if( M1[a_p][maxAction_b] > M1[maxAction_a][maxAction_b] ) {
    			
    	    		    maxAction_a = a_p;
    	    		}
    	    	    }
    		
    	    	    if( maxAction_a == a ) {
    		    
    	    		//record the action (a,maxAction_b) for agent 2
    	    		GameAction jointAction = new GameAction( new int[] {a, maxAction_b} );
    	    		if( !J2.contains(jointAction) )    
    	    		    J2.add(jointAction);
    	    	    }
    	    	    /**
    	    	     * else we judge whether (a,b) dominate (maxAction_a,maxAction)
    	    	     */
    	    	    else if( M2[a][b] > M2[maxAction_a][maxAction_b] ){
    		    
    	    		//record (a,b) for agent2
    	    		GameAction jointAction = new GameAction( new int[] {a,b} );
    	    		if( !J2.contains(jointAction) )    
    	    		    J2.add(jointAction);
    	    	    }
    	    	}
            }
        }
        
        ArrayList<GameAction> intersectionSet = new ArrayList<GameAction>();
        for( int i = 0; i < J1.size(); i++ ) {
            
            GameAction gameAction = J1.get(i);
    	
            if( J2.contains(gameAction) )
    	    	intersectionSet.add(gameAction);
        }
        
        
        
        System.out.println( "Size Nego: "+ intersectionSet.size() );
        
        for( int i = 0; i < intersectionSet.size(); i++ ) {
            
            GameAction gameAction = intersectionSet.get(i);
            
            System.out.println( M1[gameAction.getAction(0)][gameAction.getAction(1)] +
        	    "," + M2[gameAction.getAction(0)][gameAction.getAction(1)] );
        }
        
        
        return intersectionSet;
    }
    
    public ArrayList<GameAction> findOptimalSet()
    {
	//find the set of Nash equilibrium
	ArrayList<GameAction> retSet = new ArrayList<GameAction>();
	ArrayList<GameAction> nashSet = new ArrayList<GameAction>();
	for( int a = 0; a < ACTION_NUM_AGENT1; a++ ) {
	    
	    for( int b = 0; b < ACTION_NUM_AGENT2; b++ ) {
		
		if( isMax_Agent1(a, b) && isMax_Agent2(a, b) ) {
		    
		    GameAction gameAction = new GameAction(new int[] {a,b});
		    retSet.add(gameAction);
		    nashSet.add(gameAction);
		}
	    }
	}
	
	/**
	System.out.println( "Size: "+ retSet.size() );
	
        for( int i = 0; i < retSet.size(); i++ ) {
            
            GameAction gameAction = retSet.get(i);
            
            System.out.println( M1[gameAction.getAction(0)][gameAction.getAction(1)] +
        	    "," + M2[gameAction.getAction(0)][gameAction.getAction(1)] );
        }
	*/
        
	//find the action dominate nash equilibrium
	for( int i = 0; i < nashSet.size(); i++ ) {
	    
	    GameAction nashAction = nashSet.get(i);
	    
	    for( int a = 0; a < ACTION_NUM_AGENT1; a++ ) 
		for( int b = 0; b < ACTION_NUM_AGENT2; b++ ) {
		    
		    if( a == nashAction.getAction(0) && b == nashAction.getAction(1) )
			continue;
		    
		    if( M1[a][b] >= M1[nashAction.getAction(0)][nashAction.getAction(1)] && 
			    M2[a][b] >= M2[nashAction.getAction(0)][nashAction.getAction(1)]) {
			
			GameAction jointAction = new GameAction( new int[] {a,b} );
			
			if( !retSet.contains( jointAction ) ) {
			    
			    //System.out.println( M1[jointAction.getAction(0)][jointAction.getAction(1)] +
		        	    //"," + M2[jointAction.getAction(0)][jointAction.getAction(1)] );
			    
			    retSet.add( jointAction );
			}
			else
			    jointAction = null;
		    }
		}
	}
	

	
	return retSet;
    }
    
    //solution B
    public ArrayList<GameAction> newNegotiation()
    {
        //agent 1
        ArrayList<GameAction> J1_Nash = new ArrayList<GameAction>();
        ArrayList<GameAction> J1_Dom = new ArrayList<GameAction>();
        for( int a = 0; a < ACTION_NUM_AGENT1; a++ ) {
            for( int b = 0; b < ACTION_NUM_AGENT2; b++ ) {
        	
        	/**
        	 * find agent1's max action against action b
        	 */
        	int maxAction_a = a;
    	    	for( int a_p = 0; a_p < ACTION_NUM_AGENT1; a_p++ ) {
    		
    	    	    if( M1[a_p][b] > 
    	    	    	M1[maxAction_a][b] ) {
    		    
    	    		maxAction_a = a_p;
    	    	    }
    	    	}
    	    
    	    	/**
    	    	 * if action a is the max action
    	    	 */
    	    	if( maxAction_a == a ) {
    		
    	    	    //record the action (a,b) for agent 1
    	    	    GameAction jointAction = new GameAction( new int[] {a,b} );
    	    	    if( !J1_Nash.contains(jointAction) )
    	    		J1_Nash.add(jointAction);
    	    	}
    	    	/**
    	    	 * then ask agent2 whether (maxAction,b) is a Nash equilibrium
    	    	 */
    	    	else {
    		
    	    	    int maxAction_b = b;
    	    	    for( int b_p = 0; b_p < ACTION_NUM_AGENT2; b_p++ ) {
    		    
    	    		if( M2[maxAction_a][b_p] > 
    	    		    M2[maxAction_a][maxAction_b] ) {
    			
    	    		    maxAction_b = b_p;
    	    		}
    	    	    }
    		
    	    	    if( maxAction_b == b ) {
    		    
    	    		//record the action (maxAction_a,b) for agent 1
    	    		GameAction jointAction = new GameAction( new int[] {maxAction_a,b} );
    	    		if( !J1_Nash.contains(jointAction) ) {
    	    		    
    	    		    
    	    		    J1_Nash.add(jointAction);
    	    		    
    	    		    //find the dominating set
    	    		    for( int dom_a = 0; dom_a < ACTION_NUM_AGENT1; dom_a++ ) 
    	    			for( int dom_b = 0; dom_b < ACTION_NUM_AGENT2; dom_b++ ) {
    	    			    
    	    			    if( M1[dom_a][dom_b] >= M1[maxAction_a][b] ) {
    	    				
    	    				GameAction domAction = new GameAction( new int[] {dom_a,dom_b});
    	    				if( !J1_Dom.contains( domAction ) )
    	    				    J1_Dom.add( domAction );
    	    				else
    	    				    domAction = null;
    	    			    }
    	    			}
    	    		}
    	    	    }
    	    	}
            }
        }
        
        
        //agent 2
        ArrayList<GameAction> J2_Nash = new ArrayList<GameAction>();
        ArrayList<GameAction> J2_Dom = new ArrayList<GameAction>();
        for( int b = 0; b < ACTION_NUM_AGENT2; b++ ) {
            
            for( int a = 0; a < ACTION_NUM_AGENT1; a++ ) {
    	    
    	    	/**
    	    	 * find agent2's max action against action a
    	    	 */
    	    	int maxAction_b = b;
    	    	for( int b_p = 0; b_p < ACTION_NUM_AGENT2; b_p++ ) {
    		
    	    	    if( M2[a][b_p] > M2[a][maxAction_b] ) {
    		    
    	    		maxAction_b = b_p;
    	    	    }
    	    	}
    	    
    	    	/**
    	    	 * if action b is the max action
    	    	 */
    	    	if( maxAction_b == b ) {
    		
    	    	    //record the action (a,b) for agent 2
    	    	    GameAction jointAction = new GameAction( new int[] {a,b} );
    	    	    if( !J2_Nash.contains(jointAction) )
    	    		J2_Nash.add(jointAction);
    	    	}
    	    	/**
    	    	 * then ask agent1 whether (a, maxAction_b) is a Nash equilibrium
    	    	 */
    	    	else {
    		
    	    	    int maxAction_a = a;
    	    	    for( int a_p = 0; a_p < ACTION_NUM_AGENT1; a_p++ ) {
    		    
    	    		if( M1[a_p][maxAction_b] > M1[maxAction_a][maxAction_b] ) {
    			
    	    		    maxAction_a = a_p;
    	    		}
    	    	    }
    		
    	    	    if( maxAction_a == a ) {
    		    
    	    		//record the action (a,maxAction_b) for agent 2
    	    		GameAction jointAction = new GameAction( new int[] {a, maxAction_b} );
    	    		if( !J2_Nash.contains(jointAction) ) {
    	    		    
    	    			J2_Nash.add(jointAction);
    	    		    
    	    		    //find the dominating set
    	    		    for( int dom_a = 0; dom_a < ACTION_NUM_AGENT1; dom_a++ ) 
    	    			for( int dom_b = 0; dom_b < ACTION_NUM_AGENT2; dom_b++ ) {
    	    			    
    	    			    if( M2[dom_a][dom_b] >= M2[a][maxAction_b] ) {
    	    				
    	    				GameAction domAction = new GameAction( new int[] {dom_a,dom_b});
    	    				if( !J2_Dom.contains( domAction ) )
    	    				    J2_Dom.add( domAction );
    	    				else
    	    				    domAction = null;
    	    			    }
    	    			}
    	    		}
    	    	    }
    	    	}
            }
        }
        
        ArrayList<GameAction> intersectionSet = new ArrayList<GameAction>();
        for( int i = 0; i < J1_Nash.size(); i++ ) {
            
            GameAction gameAction = J1_Nash.get(i);
    	
            if( J2_Nash.contains(gameAction) )
    	    	intersectionSet.add(gameAction);
        }
        for( int i = 0; i < J1_Dom.size(); i++ ) {
            
            GameAction gameAction = J1_Dom.get(i);
    	
            if( J2_Dom.contains(gameAction) && !intersectionSet.contains(gameAction) )
    	    	intersectionSet.add(gameAction);
        }
        
        /**
        System.out.println( "Size Nego: "+ intersectionSet.size() );
        
        for( int i = 0; i < intersectionSet.size(); i++ ) {
            
            GameAction gameAction = intersectionSet.get(i);
            
            System.out.println( M1[gameAction.getAction(0)][gameAction.getAction(1)] +
        	    "," + M2[gameAction.getAction(0)][gameAction.getAction(1)] );
        }
        */
        
        return intersectionSet;
    }
    
    
    //sulotion C
    public ArrayList<GameAction> negotiation_SolutionC()
    {
        //agent 1
        ArrayList<GameAction> J1_Nash = new ArrayList<GameAction>();
        ArrayList<GameAction> J1_Dom = new ArrayList<GameAction>();
        for( int a = 0; a < ACTION_NUM_AGENT1; a++ ) {
            for( int b = 0; b < ACTION_NUM_AGENT2; b++ ) {
        	
        	/**
        	 * find agent1's max action against action b
        	 */
        	int maxAction_a = a;
    	    	for( int a_p = 0; a_p < ACTION_NUM_AGENT1; a_p++ ) {
    		
    	    	    if( M1[a_p][b] > 
    	    	    	M1[maxAction_a][b] ) {
    		    
    	    		maxAction_a = a_p;
    	    	    }
    	    	}
    	    
    	    	/**
    	    	 * if action a is the max action
    	    	 */
    	    	if( maxAction_a == a ) {
    		
    	    	    
    	    	    int maxAction_b = b;
    	    	    for( int b_p = 0; b_p < ACTION_NUM_AGENT2; b_p++ ) {
    		    
    	    		if( M2[maxAction_a][b_p] > 
    	    		    M2[maxAction_a][maxAction_b] ) {
    			
    	    		    maxAction_b = b_p;
    	    		}
    	    	    }
    	    	    
    	    	    if( maxAction_b == b ) {
    	    		
    	    		//then we find a Nash equilibrium 
    	    		GameAction jointAction = new GameAction( new int[] {a,b} );    
    	    		if( !J1_Nash.contains(jointAction) ) {
    	    		    
    	    		    J1_Nash.add(jointAction);
    	    		    
    	    		    //find the dominating set
    	    		    for( int dom_a = 0; dom_a < ACTION_NUM_AGENT1; dom_a++ ) 
    	    			for( int dom_b = 0; dom_b < ACTION_NUM_AGENT2; dom_b++ ) {
    	    			    
    	    			    if( M1[dom_a][dom_b] >= M1[maxAction_a][b] ) {
    	    				
    	    				GameAction domAction = new GameAction( new int[] {dom_a,dom_b});
    	    				if( !J1_Dom.contains( domAction ) )
    	    				    J1_Dom.add( domAction );
    	    				else
    	    				    domAction = null;
    	    			    }
    	    			}
    	    		}
    	    	    }
    	    	}
            }
        }
        
        
        //agent 2
        ArrayList<GameAction> J2_Nash = new ArrayList<GameAction>();
        ArrayList<GameAction> J2_Dom = new ArrayList<GameAction>();
        for( int b = 0; b < ACTION_NUM_AGENT2; b++ ) {
            
            for( int a = 0; a < ACTION_NUM_AGENT1; a++ ) {
    	    
    	    	/**
    	    	 * find agent2's max action against action a
    	    	 */
    	    	int maxAction_b = b;
    	    	for( int b_p = 0; b_p < ACTION_NUM_AGENT2; b_p++ ) {
    		
    	    	    if( M2[a][b_p] > M2[a][maxAction_b] ) {
    		    
    	    		maxAction_b = b_p;
    	    	    }
    	    	}
    	    
    	    	/**
    	    	 * if action b is the max action
    	    	 */
    	    	if( maxAction_b == b ) {
    	    	    
    	    	    int maxAction_a = a;
    	    	    for( int a_p = 0; a_p < ACTION_NUM_AGENT1; a_p++ ) {
    		    
    	    		if( M1[a_p][maxAction_b] > M1[maxAction_a][maxAction_b] ) {
    			
    	    		    maxAction_a = a_p;
    	    		}
    	    	    }
    	    	    
    	    	    if( maxAction_a == a ) {
    	    		
    	    		//then we find a Nash equilibrium   
    	    		GameAction jointAction = new GameAction( new int[] {a,b} );
    	    		    
    	    		if( !J2_Nash.contains(jointAction) ) {
        	    		
    	    		    J2_Nash.add(jointAction);
    	    		    
    	    		    //find the dominating set
    	    		    for( int dom_a = 0; dom_a < ACTION_NUM_AGENT1; dom_a++ ) 	 
    	    			for( int dom_b = 0; dom_b < ACTION_NUM_AGENT2; dom_b++ ) {	    			    
    	    			            	    			
    	    			    if( M2[dom_a][dom_b] >= M2[a][maxAction_b] ) {	
    	    			    			    
    	    				GameAction domAction = new GameAction( new int[] {dom_a,dom_b}); 	    			    
    	    				if( !J2_Dom.contains( domAction ) )	    				    
    	    				    J2_Dom.add( domAction );    				
    	    				else	    				    
    	    				    domAction = null;    			    
    	    			    }	
    	    			}    	    	    
    	    		}    	    		   	    	    
    	    	    }
    	    	}
            }
        }
        
        ArrayList<GameAction> intersectionSet = new ArrayList<GameAction>();
        for( int i = 0; i < J1_Nash.size(); i++ ) {
            
            GameAction gameAction = J1_Nash.get(i);
    	
            if( J2_Nash.contains(gameAction) )
    	    	intersectionSet.add(gameAction);
        }
        //System.out.println("Nash Count "+intersectionSet.size());
        for( int i = 0; i < J1_Dom.size(); i++ ) {
            
            GameAction gameAction = J1_Dom.get(i);
    	
            if( J2_Dom.contains(gameAction) && !intersectionSet.contains(gameAction) )
    	    	intersectionSet.add(gameAction);
        }
        
        /**
        System.out.println( "Size Nego: "+ intersectionSet.size() );
        
        for( int i = 0; i < intersectionSet.size(); i++ ) {
            
            GameAction gameAction = intersectionSet.get(i);
            
            System.out.println( M1[gameAction.getAction(0)][gameAction.getAction(1)] +
        	    "," + M2[gameAction.getAction(0)][gameAction.getAction(1)] );
        }
        */
        
        return intersectionSet;
    }
    
    
    public ArrayList<GameAction> negotiation_SolutionE()
    {
        //agent 1
        ArrayList<GameAction> J1_Nash = new ArrayList<GameAction>();
        ArrayList<GameAction> J1_Dom = new ArrayList<GameAction>();
        for( int a = 0; a < ACTION_NUM_AGENT1; a++ ) {
            for( int b = 0; b < ACTION_NUM_AGENT2; b++ ) {
        	
        	/**
        	 * find agent1's max action against action b
        	 */
        	int maxAction_a = a;
    	    	for( int a_p = 0; a_p < ACTION_NUM_AGENT1; a_p++ ) {
    		
    	    	    if( M1[a_p][b] > 
    	    	    	M1[maxAction_a][b] ) {
    		    
    	    		maxAction_a = a_p;
    	    	    }
    	    	}
    	    
    	    	/**
    	    	 * if action a is the max action
    	    	 */
    	    	if( maxAction_a == a ) {
    		
    	    	    
    	    	    int maxAction_b = b;
    	    	    for( int b_p = 0; b_p < ACTION_NUM_AGENT2; b_p++ ) {
    		    
    	    		if( M2[maxAction_a][b_p] > 
    	    		    M2[maxAction_a][maxAction_b] ) {
    			
    	    		    maxAction_b = b_p;
    	    		}
    	    	    }
    	    	    
    	    	    if( maxAction_b == b ) {
    	    		
    	    		//then we find a Nash equilibrium 
    	    		GameAction jointAction = new GameAction( new int[] {a,b} );    
    	    		if( !J1_Nash.contains(jointAction) ) {
    	    		    
    	    		    J1_Nash.add(jointAction);
    	    		    
    	    		    //find the dominating set
    	    		    for( int dom_a = 0; dom_a < ACTION_NUM_AGENT1; dom_a++ ) 
    	    			for( int dom_b = 0; dom_b < ACTION_NUM_AGENT2; dom_b++ ) {
    	    			    
    	    			    if( M1[dom_a][dom_b] >= M1[maxAction_a][b] ) {
    	    				
    	    				GameAction domAction = new GameAction( new int[] {dom_a,dom_b});
    	    				if( !J1_Dom.contains( domAction ) )
    	    				    J1_Dom.add( domAction );
    	    				else
    	    				    domAction = null;
    	    			    }
    	    			}
    	    		}
    	    	    }
    	    	}
            }
        }
        
        
        //agent 2
        ArrayList<GameAction> J2_Nash = new ArrayList<GameAction>();
        ArrayList<GameAction> J2_Dom = new ArrayList<GameAction>();
        for( int b = 0; b < ACTION_NUM_AGENT2; b++ ) {
            
            for( int a = 0; a < ACTION_NUM_AGENT1; a++ ) {
    	    
    	    	/**
    	    	 * find agent2's max action against action a
    	    	 */
    	    	int maxAction_b = b;
    	    	for( int b_p = 0; b_p < ACTION_NUM_AGENT2; b_p++ ) {
    		
    	    	    if( M2[a][b_p] > M2[a][maxAction_b] ) {
    		    
    	    		maxAction_b = b_p;
    	    	    }
    	    	}
    	    
    	    	/**
    	    	 * if action b is the max action
    	    	 */
    	    	if( maxAction_b == b ) {
    	    	    
    	    	    int maxAction_a = a;
    	    	    for( int a_p = 0; a_p < ACTION_NUM_AGENT1; a_p++ ) {
    		    
    	    		if( M1[a_p][maxAction_b] > M1[maxAction_a][maxAction_b] ) {
    			
    	    		    maxAction_a = a_p;
    	    		}
    	    	    }
    	    	    
    	    	    if( maxAction_a == a ) {
    	    		
    	    		//then we find a Nash equilibrium   
    	    		GameAction jointAction = new GameAction( new int[] {a,b} );
    	    		    
    	    		if( !J2_Nash.contains(jointAction) ) {
        	    		
    	    		    J2_Nash.add(jointAction);
    	    		    
    	    		    //find the dominating set
    	    		    for( int dom_a = 0; dom_a < ACTION_NUM_AGENT1; dom_a++ ) 	 
    	    			for( int dom_b = 0; dom_b < ACTION_NUM_AGENT2; dom_b++ ) {	    			    
    	    			            	    			
    	    			    if( M2[dom_a][dom_b] >= M2[a][maxAction_b] ) {	
    	    			    			    
    	    				GameAction domAction = new GameAction( new int[] {dom_a,dom_b}); 	    			    
    	    				if( !J2_Dom.contains( domAction ) )	    				    
    	    				    J2_Dom.add( domAction );    				
    	    				else	    				    
    	    				    domAction = null;    			    
    	    			    }	
    	    			}    	    	    
    	    		}    	    		   	    	    
    	    	    }
    	    	}
            }
        }
        
        ArrayList<GameAction> intersectionSet = new ArrayList<GameAction>();
        for( int i = 0; i < J1_Nash.size(); i++ ) {
            
            GameAction gameAction = J1_Nash.get(i);
    	
            if( J2_Nash.contains(gameAction) )
    	    	intersectionSet.add(gameAction);
        }
        //System.out.println("Nash Count "+intersectionSet.size());
        for( int i = 0; i < J1_Dom.size(); i++ ) {
            
            GameAction gameAction = J1_Dom.get(i);
    	
            if( J2_Dom.contains(gameAction) && !intersectionSet.contains(gameAction) )
    	    	intersectionSet.add(gameAction);
        }
        
        /**
        System.out.println( "Size Nego: "+ intersectionSet.size() );
        
        for( int i = 0; i < intersectionSet.size(); i++ ) {
            
            GameAction gameAction = intersectionSet.get(i);
            
            System.out.println( M1[gameAction.getAction(0)][gameAction.getAction(1)] +
        	    "," + M2[gameAction.getAction(0)][gameAction.getAction(1)] );
        }
        */
        
        return intersectionSet;
    }
    
    private boolean isMax_Agent1( int a, int b )
    {
	
	for( int a_p = 0; a_p < ACTION_NUM_AGENT1; a_p++ ) {
	 
	    if( M1[a][b] < M1[a_p][b] )
		return false;
	}
	return true;
    }
    
    private boolean isMax_Agent2( int a, int b )
    {
	
	for( int b_p = 0; b_p < ACTION_NUM_AGENT2; b_p++ ) {
	 
	    if( M2[a][b] < M2[a][b_p] )
		return false;
	}
	return true;
    }
    
    /**
     * @param args
     */
    public static void main(String[] args)
    {
	// TODO Auto-generated method stub
	
	MatrixGame mtrGame = new MatrixGame();
	
	mtrGame.displayMatrix();
	
	
	//mtrGame.negotiation_SolutionC();
	//mtrGame.findOptimalSet();
	
	
	
	/**/
	for( int i = 0; i < 100000; i++ ) {
	    
	    mtrGame.regenerateMatrix();
	    
	    if( mtrGame.negotiation_SolutionC().size() != mtrGame.findOptimalSet().size() )
		System.out.println( "Find Other Actions" );   
	}
	System.out.println( "Over" );
	
    }

}
