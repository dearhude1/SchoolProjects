package linearGridWorld;


public class GameAction
{

    public static final int NUM_ACTIONS = 2;
    
    /**
    * define action ids
    */
    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final String[] ACTIONS_STRING = { "L", "R" };
    
    
    private int[] jointAction;
    
    public GameAction()
    {
	jointAction = new int[LinearGridWorld.NUM_AGENTS];
	
	for( int agent = 0; agent < LinearGridWorld.NUM_AGENTS; agent++ )
	    jointAction[agent] = LEFT;
    }
    
    public GameAction( int[] actions )
    {
	jointAction = new int[LinearGridWorld.NUM_AGENTS];
	
	if( actions == null || 
	    actions.length != LinearGridWorld.NUM_AGENTS ) {
		 
	    for( int agent = 0; agent < LinearGridWorld.NUM_AGENTS; agent++ )    
		jointAction[agent] = LEFT;
	}
	else {
	    
	    for( int agent = 0; agent < LinearGridWorld.NUM_AGENTS; agent++ )    
		jointAction[agent] = actions[agent];
	}
    }
    
    public int getAction( int agent )
    {
	if( agent < 0 || agent >= LinearGridWorld.NUM_AGENTS ) {
	    
	    System.out.println( "GameAction->setAction: No Such Agent!" );
	    return -1;
	}
	else
	    return jointAction[agent];
    }
    
    public void setAction( int agent, int action ) 
    {
	if( agent < 0 || agent >= LinearGridWorld.NUM_AGENTS ) {
	    
	    System.out.println( "@GameAction->setAction: No Such Agent!" );
	    return;
	}
	else if( action < 0 || action >= NUM_ACTIONS ) {
	    
	    System.out.println( "GameAction->setAction: No Such Action!" );
	}
	
	jointAction[agent] = action; 
    }
    
    public boolean equals( Object obj )
    {
	if( obj == null )
	    return false;
	else if( !(obj instanceof GameAction) )
	    return false;
	else {
	    
	    GameAction action = (GameAction) obj;
	    
	    for( int agent = 0; agent < LinearGridWorld.NUM_AGENTS; agent++ ) {
		
		if( jointAction[agent] != action.getAction(agent) )
		    return false;
	    }
	    
	    return true;
	}
    }
    
    public int hashCode()
    {
	int hCode = 0;
	for( int agent = 0; agent < LinearGridWorld.NUM_AGENTS; agent++ ) {
	 
	    int act = jointAction[agent];
	    
	    hCode += act * (21-agent);
	}
	
	return hCode;
    }
    
    public void printAction()
    {
	System.out.print( "(" );
	for( int agent = 0; agent < LinearGridWorld.NUM_AGENTS-1; agent++ ) {
	    
	    System.out.print( ACTIONS_STRING[jointAction[agent]]+"," );
	}
	System.out.print( ACTIONS_STRING[jointAction[LinearGridWorld.NUM_AGENTS-1]] + ")" );
	System.out.println();
    }
    
    public static String getActionString( int action ) 
    {
	if( action < 0 || action >= NUM_ACTIONS ) {
	    
	    System.out.println("GameAction->getActiongString: Wrong Parameter!");
	    return null;
	}
	else
	    return ACTIONS_STRING[action];
    }
}
