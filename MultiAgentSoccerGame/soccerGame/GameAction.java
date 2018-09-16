package soccerGame;


public class GameAction
{

    public static final int NUM_ACTIONS = 3;
    
    /**
    * define action ids
    *
    public static final int UP = 1;
    public static final int DOWN = 3;
    public static final int LEFT = 2;
    public static final int RIGHT = 0;
    public static final int STAY = 4;
    public static final int PASS = 5;
    public static final String[] ACTIONS_STRING = { "R", "U", "L", "D", "S", "P" };
    */
    
    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int PASS = 2;
    public static final String[] ACTIONS_STRING = { "L", "R", "P" };
    
    //check all functions since the agent number is different
    
    private int[] jointAction;
    
    public GameAction()
    {
	jointAction = new int[SoccerGame.NUM_TEAM_AGENTS * SoccerGame.NUM_TEAMS];
	
	for( int agent = 0; agent < SoccerGame.NUM_TEAM_AGENTS * SoccerGame.NUM_TEAMS; agent++ )
	    jointAction[agent] = 0;
    }
    
    public GameAction( int[] actions )
    {
	jointAction = new int[SoccerGame.NUM_TEAM_AGENTS * SoccerGame.NUM_TEAMS];
	
	if( actions == null || 
	    actions.length != SoccerGame.NUM_TEAM_AGENTS * SoccerGame.NUM_TEAMS ) {
		 
	    for( int agent = 0; agent < SoccerGame.NUM_TEAM_AGENTS * SoccerGame.NUM_TEAMS; agent++ )    
		jointAction[agent] = 0;
	}
	else {
	    
	    for( int agent = 0; agent < SoccerGame.NUM_TEAM_AGENTS * SoccerGame.NUM_TEAMS; agent++ )    
		jointAction[agent] = actions[agent];
	}
    }
    
    public int getAction( int agent )
    {
	if( agent < 0 || agent >= SoccerGame.NUM_TEAM_AGENTS * SoccerGame.NUM_TEAMS ) {
	    
	    System.out.println( "@GameAction->setAction: No Such Agent!" );
	    return -1;
	}
	else
	    return jointAction[agent];
    }
    
    public void setAction( int agent, int action ) 
    {
	if( agent < 0 || agent >= SoccerGame.NUM_TEAM_AGENTS * SoccerGame.NUM_TEAMS ) {
	    
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
	    
	    for( int agent = 0; agent < SoccerGame.NUM_TEAM_AGENTS * SoccerGame.NUM_TEAMS; agent++ ) {
		
		if( jointAction[agent] != action.getAction(agent) )
		    return false;
	    }
	    
	    return true;
	}
    }
    
    public int hashCode()
    {
	int hCode = 0;
	for( int agent = 0; agent < SoccerGame.NUM_TEAM_AGENTS * SoccerGame.NUM_TEAMS; agent++ ) {
	 
	    int act = jointAction[agent];
	    
	    hCode += act * (21-agent);
	}
	
	return hCode;
    }
    
    public void printAction()
    {
	System.out.print( "(" );
	for( int agent = 0; agent < SoccerGame.NUM_TEAM_AGENTS * SoccerGame.NUM_TEAMS-1; agent++ ) {
	    
	    System.out.print( ACTIONS_STRING[jointAction[agent]]+"," );
	}
	System.out.print( ACTIONS_STRING[jointAction[SoccerGame.NUM_TEAM_AGENTS * SoccerGame.NUM_TEAMS-1]] + ")" );
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
