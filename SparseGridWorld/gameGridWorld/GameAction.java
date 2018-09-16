package gameGridWorld;



public class GameAction
{

    public static final int NUM_ACTIONS = 4;
    
    /**
    * define action ids
    */
    public static final int UP = 1;
    public static final int DOWN = 3;
    public static final int LEFT = 2;
    public static final int RIGHT = 0;
    public static final String[] ACTIONS_STRING = { "R", "U", "L", "D" };
    
    /**
     * determined by the map of the game
     */
    private static int NUM_AGENTS = 0;
    
    private int[] jointAction;
    
    
    
    public GameAction()
    {
	jointAction = new int[NUM_AGENTS];
	
	for( int agent = 0; agent < NUM_AGENTS; agent++ )
	    jointAction[agent] = 0;
    }
    
    public GameAction( int[] actions )
    {
	jointAction = new int[NUM_AGENTS];
	
	if( actions == null || 
	    actions.length != NUM_AGENTS ) {
		 
	    for( int agent = 0; agent < NUM_AGENTS; agent++ )    
		jointAction[agent] = 0;
	}
	else {
	    
	    for( int agent = 0; agent < NUM_AGENTS; agent++ )    
		jointAction[agent] = actions[agent];
	}
    }
    
    public int getAction( int agent )
    {
	if( agent < 0 || agent >= NUM_AGENTS ) {
	    
	    System.out.println( "@GameAction->setAction: No Such Agent!" );
	    return -1;
	}
	else
	    return jointAction[agent];
    }
    
    public void setAction( int agent, int action ) 
    {
	if( agent < 0 || agent >= NUM_AGENTS ) {
	    
	    System.out.println( "@GameAction->setAction: No Such Agent!" );
	    return;
	}
	else if( action < 0 || action >= NUM_ACTIONS ) {
	    
	    System.out.println( "@GameAction->setAction: No Such Action!" );
	}
	
	jointAction[agent] = action; 
    }
    
    
    public void printAction()
    {
	System.out.print( "(" );
	for( int agent = 0; agent < NUM_AGENTS-1; agent++ ) {
	    
	    System.out.print( ACTIONS_STRING[jointAction[agent]]+"," );
	}
	System.out.print( ACTIONS_STRING[jointAction[NUM_AGENTS-1]] + ")" );
	System.out.println();
    }
    
    public static String getActionString( int action ) 
    {
	if( action < 0 || action >= NUM_ACTIONS ) {
	    
	    System.out.println("@GameAction->getActiongString: Wrong Parameter!");
	    return null;
	}
	else
	    return ACTIONS_STRING[action];
    }
    
    
    public boolean equals( Object obj )
    {
	if( obj == null )
	    return false;
	else if( !(obj instanceof GameAction) )
	    return false;
	else {
	    
	    GameAction action = (GameAction) obj;
	    
	    for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
		
		if( jointAction[agent] != action.getAction(agent) )
		    return false;
	    }
	    
	    return true;
	}
    }
    
    public int hashCode()
    {
	int hCode = 0;
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	 
	    int act = jointAction[agent];
	    
	    hCode += act * (21-agent);
	}
	
	return hCode;
    }
    
    /**
     * called when the map has been loaded
     * just before the call of the method "generateValidStates" of the class Map
     * @param agentNum
     */
    public static void setNumAgents( int agentNum ) 
    {
	if( agentNum < 0 )
	    NUM_AGENTS = 2;
	else	
	    NUM_AGENTS = agentNum;
    }
}
