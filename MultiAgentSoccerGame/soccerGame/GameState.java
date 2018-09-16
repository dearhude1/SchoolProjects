package soccerGame;

import java.util.Random;



public class GameState
{
    
    /**
     * the current location of each agent 
     * at this state
     */
    private int[] agentLocationIDs;
    
    /**
     * goal possession
     * indicated by the ID of agent which has the ball
     */
    private int ballPossAgent = 0;
    
    //check all functions since the agent number is different
    
    
    public GameState( int ballPoss )
    {
	agentLocationIDs = new int[SoccerGame.NUM_TEAM_AGENTS * SoccerGame.NUM_TEAMS];
	
	for( int agent = 0; agent < SoccerGame.NUM_TEAM_AGENTS * SoccerGame.NUM_TEAMS; agent++ ) {
	    
	    int team = agent / SoccerGame.NUM_TEAM_AGENTS;
	    
	    if( team == SoccerGame.TEAM_A )
		agentLocationIDs[agent] = SoccerGame.INIT_LOCATION_TEAM_A;
	    else if( team == SoccerGame.TEAM_B )
		agentLocationIDs[agent] = SoccerGame.INIT_LOCATION_TEAM_B;
	    else
		agentLocationIDs[agent] = SoccerGame.INVALID_LOCATION_ID;
	}
	/**
	 * init the goal possesion randomly
	 */
	ballPossAgent = ballPoss;
    }
    
    public GameState( int locationIDs[], int ballPoss )
    {
	agentLocationIDs = new int[SoccerGame.NUM_TEAM_AGENTS * SoccerGame.NUM_TEAMS];
	if( locationIDs == null || 
		locationIDs.length != SoccerGame.NUM_TEAM_AGENTS * SoccerGame.NUM_TEAMS ) {
	    
	    for( int agent = 0; agent < SoccerGame.NUM_TEAM_AGENTS * SoccerGame.NUM_TEAMS; agent++ )
		agentLocationIDs[agent] = SoccerGame.INVALID_LOCATION_ID;
	}
	else {
	    
	    for( int agent = 0; agent < SoccerGame.NUM_TEAM_AGENTS * SoccerGame.NUM_TEAMS; agent++ )
		agentLocationIDs[agent] = locationIDs[agent];
	}
	
	/**
	 * init the goal possesion randomly
	 */
	ballPossAgent = ballPoss;
    }
    
    public int getLocationID( int agent ) 
    {
	if( agent >= 0 && agent < SoccerGame.NUM_TEAM_AGENTS * SoccerGame.NUM_TEAMS )
	    return agentLocationIDs[agent];
	else {
	    
	    System.out.println("@GameState->getLocationID: Invalid Agent ID!");
	    return SoccerGame.INVALID_LOCATION_ID;
	}
    }
    
    public int[] getLocationIDs()
    {
	return agentLocationIDs;
    }
    
    public void setLocationID( int agent, int locationID )
    {
	if( agent >= 0 && agent < SoccerGame.NUM_TEAM_AGENTS * SoccerGame.NUM_TEAMS && 
		locationID >= 0 && locationID < SoccerGame.AREA_LOCATIONS ) {
	    
	    agentLocationIDs[agent] = locationID; 
	}
	else {
	    
	    int team = SoccerGame.getAgentTeam( agent );
	    
	    if( team == SoccerGame.TEAM_A && locationID == SoccerGame.GOAL_LOC_A ) {
		    agentLocationIDs[agent] = locationID; 
	    }
	    else if( team == SoccerGame.TEAM_B && locationID == SoccerGame.GOAL_LOC_B ) {
		agentLocationIDs[agent] = locationID; 
	    }
	    else {
		
		System.out.println( "@GameState->setLocationID: Something Wrong in Parameters!" );
		
		System.out.println("agent "+agent+" loc: "+locationID);
	    }
	}
    }
    
    public int getBallPossession()
    {
	return ballPossAgent;
    }
    
    public int getBallPossTeam()
    {
	int team = SoccerGame.getAgentTeam( ballPossAgent );
	return team;
    }
    
    public void setBallPossession( int agent )
    {
	if( agent < 0 || agent >= SoccerGame.NUM_TEAM_AGENTS * SoccerGame.NUM_TEAMS ) {
	    
	    System.out.println("GameState->setBallPossession: Wrong Parameter!");
	    ballPossAgent = new Random().nextInt(SoccerGame.NUM_TEAM_AGENTS * SoccerGame.NUM_TEAMS);
	}
	else
	    ballPossAgent = agent;
    }
    
    public boolean equals( Object obj )
    {
	if( obj == null )
	    return false;
	else if( !(obj instanceof GameState) )
	    return false;
	else {
	    
	    GameState state = (GameState) obj;
	    
	    for( int agent = 0; agent < SoccerGame.NUM_TEAM_AGENTS * SoccerGame.NUM_TEAMS; agent++ ) {
		
		if( agentLocationIDs[agent] != state.getLocationID(agent) ) {
		       
		    return false;
		}
	    }
	    
	    if( ballPossAgent != state.getBallPossession() )
		return false;
	    else
		return true;
	}
    }
    
    public int hashCode()
    {
	int hCode = 0;
	for( int agent = 0; agent < SoccerGame.NUM_TEAM_AGENTS * SoccerGame.NUM_TEAMS; agent++ ) {
	 
	    int loc = agentLocationIDs[agent];
	    
	    hCode += loc * (31-agent);
	}
	
	return hCode;
    }
    
}
