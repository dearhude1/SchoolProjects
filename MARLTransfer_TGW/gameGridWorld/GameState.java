package gameGridWorld;


public class GameState
{
    
    /**
     * the current location of each agent 
     * at this state
     */
    private int[] agentLocationIDs;
    
    
    public GameState()
    {
	agentLocationIDs = new int[GridWorld.NUM_AGENTS];
	
	for( int agent = 0; agent < GridWorld.NUM_AGENTS; agent++ )
	    agentLocationIDs[agent] = GridWorldLocation.INVALID_LOCATION_ID;
    }
    
    public GameState( int locationIDs[] )
    {
	agentLocationIDs = new int[GridWorld.NUM_AGENTS];
	if( locationIDs == null || 
		locationIDs.length != GridWorld.NUM_AGENTS ) {
	    
	    for( int agent = 0; agent < GridWorld.NUM_AGENTS; agent++ )
		agentLocationIDs[agent] = GridWorldLocation.INVALID_LOCATION_ID;
	}
	else {
	    
	    for( int agent = 0; agent < GridWorld.NUM_AGENTS; agent++ )
		agentLocationIDs[agent] = locationIDs[agent];
	}
    }
    
    public int getLocationID( int agent ) 
    {
	if( agent >= 0 && agent < GridWorld.NUM_AGENTS )
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
	if( agent >= 0 && agent < GridWorld.NUM_AGENTS && 
		locationID >= 0 && locationID < GridWorld.NUM_LOCATIONS ) {
	    
	    agentLocationIDs[agent] = locationID; 
	}
	else {
	    
	    System.out.println( "@GameState->setLocationID: Something Wrong in Parameters!" );
	}
    }
    
}
