package teamGamGam;


public class TeamAction
{
    
    //check all functions since the agent number is different
    
    private int[] teamAction;
    
    public TeamAction()
    {
	teamAction = new int[GamblingGame.NUM_TEAM_AGENTS];
	
	for( int agent = 0; agent < GamblingGame.NUM_TEAM_AGENTS; agent++ )
	    teamAction[agent] = GameAction.BET_SMALL;
    }
    
    public TeamAction( int[] actions )
    {
	teamAction = new int[GamblingGame.NUM_TEAM_AGENTS];
	
	if( actions == null || 
	    actions.length != GamblingGame.NUM_TEAM_AGENTS  ) {
		 
	    for( int agent = 0; agent < GamblingGame.NUM_TEAM_AGENTS; agent++ )    
		teamAction[agent] = GameAction.BET_SMALL;
	}
	else {
	    
	    for( int agent = 0; agent < GamblingGame.NUM_TEAM_AGENTS; agent++ )    
		teamAction[agent] = actions[agent];
	}
    }
    
    public int getAction( int agentNoInTeam )
    {
	if( agentNoInTeam < 0 || agentNoInTeam >= GamblingGame.NUM_TEAM_AGENTS ) {
	    
	    System.out.println( "@TeamAction->setAction: No Such Agent No!" );
	    return -1;
	}
	else
	    return teamAction[agentNoInTeam];
    }
    
    public void setAction( int agentNoInTeam, int action ) 
    {
	if( agentNoInTeam < 0 || agentNoInTeam >= GamblingGame.NUM_TEAM_AGENTS ) {
	    
	    System.out.println( "@TeamAction->setAction: No Such Agent!" );
	    return;
	}
	else if( action < 0 || action >= GameAction.NUM_ACTIONS ) {
	    
	    System.out.println( "@TeamAction->setAction: No Such Action!" );
	}
	
	teamAction[agentNoInTeam] = action; 
    }
    
    public boolean equals( Object obj )
    {
	if( obj == null )
	    return false;
	else if( !(obj instanceof TeamAction) )
	    return false;
	else {
	    
	    TeamAction action = (TeamAction) obj;
	    
	    for( int agentNoInTeam = 0; agentNoInTeam < GamblingGame.NUM_TEAM_AGENTS; agentNoInTeam++ ) {
		
		if( teamAction[agentNoInTeam] != action.getAction(agentNoInTeam) )
		    return false;
	    }
	    
	    return true;
	}
    }
    
    public int hashCode()
    {
	int hCode = 0;
	for( int agentNoInTeam = 0; agentNoInTeam < GamblingGame.NUM_TEAM_AGENTS; agentNoInTeam++ ) {
	 
	    int act = teamAction[agentNoInTeam];
	    
	    hCode += act * (11-agentNoInTeam);
	}
	
	return hCode;
    }
}
