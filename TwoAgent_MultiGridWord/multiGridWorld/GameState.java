package multiGridWorld;


public class GameState
{
    
    /**
     * the current location of each agent 
     * at this state
     */
    private int[] agentLocationIDs;
    
    private int worldIndex;
    
    public GameState()
    {
	agentLocationIDs = new int[MultiGridWorld.NUM_AGENTS];
	
	for( int agent = 0; agent < MultiGridWorld.NUM_AGENTS; agent++ )
	    agentLocationIDs[agent] = GridWorldLocation.INVALID_LOCATION_ID;
    }
    
    public GameState( int wldIndex, int locationIDs[] )
    {
	agentLocationIDs = new int[MultiGridWorld.NUM_AGENTS];
	if( locationIDs == null || locationIDs.length != MultiGridWorld.NUM_AGENTS ||
		wldIndex < 0 || wldIndex >= MultiGridWorld.WORLD_NUM ) {
	    
	    worldIndex = 1;
	    for( int agent = 0; agent < MultiGridWorld.NUM_AGENTS; agent++ )
		agentLocationIDs[agent] = GridWorldLocation.INVALID_LOCATION_ID;
	}
	else {
	    
	    worldIndex = wldIndex;
	    for( int agent = 0; agent < MultiGridWorld.NUM_AGENTS; agent++ )
		agentLocationIDs[agent] = locationIDs[agent];
	}
    }
    

    
    public int getLocationID( int agent ) 
    {
	if( agent >= 0 && agent < MultiGridWorld.NUM_AGENTS )
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
	if( agent >= 0 && agent < MultiGridWorld.NUM_AGENTS && 
		locationID >= 0 && locationID < MultiGridWorld.NUM_LOCATIONS ) {
	    
	    agentLocationIDs[agent] = locationID; 
	}
	else {
	    
	    System.out.println( "@GameState->setLocationID: Something Wrong in Parameters!" );
	}
    }
    
    public int getWorldIndex()
    {
	return worldIndex;
    }
    
    public void setWolrdIndex( int wldIndex )
    {
	if( wldIndex < 0 || wldIndex >= MultiGridWorld.WORLD_NUM ) {
	    
	    System.out.println("GameState->setWorldIndex: Wrong Parameter!");
	    return;
	}
	
	worldIndex = wldIndex;
    }
}
