package wallGame;

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
    
    
    private int[] jointAction;
    
    public GameAction()
    {
	jointAction = new int[WallGame.NUM_AGENTS];
	
	for( int agent = 0; agent < WallGame.NUM_AGENTS; agent++ )
	    jointAction[agent] = 0;
    }
    
    public GameAction( int[] actions )
    {
	jointAction = new int[WallGame.NUM_AGENTS];
	
	if( actions == null || 
	    actions.length != WallGame.NUM_AGENTS ) {
		 
	    for( int agent = 0; agent < WallGame.NUM_AGENTS; agent++ )    
		jointAction[agent] = 0;
	}
	else {
	    
	    for( int agent = 0; agent < WallGame.NUM_AGENTS; agent++ )    
		jointAction[agent] = actions[agent];
	}
    }
    
    public int getAction( int agent )
    {
	if( agent < 0 || agent >= WallGame.NUM_AGENTS ) {
	    
	    System.out.println( "@GameAction->setAction: No Such Agent!" );
	    return -1;
	}
	else
	    return jointAction[agent];
    }
    
    public void setAction( int agent, int action ) 
    {
	if( agent < 0 || agent >= WallGame.NUM_AGENTS ) {
	    
	    System.out.println( "@GameAction->setAction: No Such Agent!" );
	    return;
	}
	else if( action < 0 || action >= NUM_ACTIONS ) {
	    
	    System.out.println( "@GameAction->setAction: No Such Action!" );
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
	    
	    for( int agent = 0; agent < WallGame.NUM_AGENTS; agent++ ) {
		
		if( jointAction[agent] != action.getAction(agent) )
		    return false;
	    }
	    
	    return true;
	}
    }
    
    public void printAction()
    {
	System.out.print( "(" );
	for( int agent = 0; agent < WallGame.NUM_AGENTS-1; agent++ ) {
	    
	    System.out.print( ACTIONS_STRING[jointAction[agent]]+"," );
	}
	System.out.print( ACTIONS_STRING[jointAction[WallGame.NUM_AGENTS-1]] + ")" );
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
}
