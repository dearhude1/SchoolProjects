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
     */
    private int ballPossession = 0;
    
    public GameState( int ballPoss )
    {
	agentLocationIDs = new int[SoccerGame.NUM_AGENTS];
	
	for( int agent = 0; agent < SoccerGame.NUM_AGENTS; agent++ )
	    agentLocationIDs[agent] = GridWorldLocation.INVALID_LOCATION_ID;
	
	/**
	 * init the goal possesion randomly
	 */
	ballPossession = ballPoss;
    }
    
    public GameState( int locationIDs[], int ballPoss )
    {
	agentLocationIDs = new int[SoccerGame.NUM_AGENTS];
	if( locationIDs == null || 
		locationIDs.length != SoccerGame.NUM_AGENTS ) {
	    
	    for( int agent = 0; agent < SoccerGame.NUM_AGENTS; agent++ )
		agentLocationIDs[agent] = GridWorldLocation.INVALID_LOCATION_ID;
	}
	else {
	    
	    for( int agent = 0; agent < SoccerGame.NUM_AGENTS; agent++ )
		agentLocationIDs[agent] = locationIDs[agent];
	}
	
	/**
	 * init the goal possesion randomly
	 */
	ballPossession = ballPoss;
    }
    
    public int getLocationID( int agent ) 
    {
	if( agent >= 0 && agent < SoccerGame.NUM_AGENTS )
	    return agentLocationIDs[agent];
	else {
	    
	    System.out.println("@GameState->getLocationID: Invalid Agent ID!");
	    return GridWorldLocation.INVALID_LOCATION_ID;
	}
    }
    
    public int[] getLocationIDs()
    {
	return agentLocationIDs;
    }
    
    public void setLocationID( int agent, int locationID )
    {
	if( agent >= 0 && agent < SoccerGame.NUM_AGENTS && 
		locationID >= 0 && locationID < SoccerGame.NUM_LOCATIONS ) {
	    
	    agentLocationIDs[agent] = locationID; 
	}
	else if( agent == SoccerGame.AGENT_A && 
		(locationID == GridWorldLocation.GOAL_A_1 || 
		locationID == GridWorldLocation.GOAL_A_2)) {
	    agentLocationIDs[agent] = locationID; 
	}
	else if( agent == SoccerGame.AGENT_B && 
		(locationID == GridWorldLocation.GOAL_B_1 || 
		locationID == GridWorldLocation.GOAL_B_2) ) {
	    agentLocationIDs[agent] = locationID; 
	}
	else {
	    
	    System.out.println( "@GameState->setLocationID: Something Wrong in Parameters!" );
	    
	    System.out.println("loc: "+locationID);
	}
    }
    
    public int getBallPossession()
    {
	return ballPossession;
    }
    
    public void setBallPossession( int agent )
    {
	if( agent < 0 || agent >= SoccerGame.NUM_AGENTS ) {
	    
	    System.out.println("GameState->setBallPossession: Wrong Parameter!");
	    ballPossession = new Random().nextInt(SoccerGame.NUM_AGENTS);
	}
	else
	    ballPossession = agent;
    }
}
