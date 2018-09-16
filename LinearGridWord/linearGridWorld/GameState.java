package linearGridWorld;

import linearGridWorld.LinearGridWorld;


public class GameState
{
    
    /**
     * the current location of each agent 
     * at this state
     */
    private int[] agentLocationIDs;
    
    
    public GameState()
    {
	agentLocationIDs = new int[LinearGridWorld.NUM_AGENTS];
	
	for( int agent = 0; agent < LinearGridWorld.NUM_AGENTS; agent++ )
	    agentLocationIDs[agent] = LinearGridWorld.NUM_LOCATIONS-1-agent;
    }
    
    public GameState( int locationIDs[] )
    {
	agentLocationIDs = new int[LinearGridWorld.NUM_AGENTS];
	if( locationIDs == null || 
		locationIDs.length != LinearGridWorld.NUM_AGENTS ) {
	    
	    for( int agent = 0; agent < LinearGridWorld.NUM_AGENTS; agent++ )
		agentLocationIDs[agent] = LinearGridWorld.NUM_LOCATIONS-1-agent;
	}
	else {
	    
	    for( int agent = 0; agent < LinearGridWorld.NUM_AGENTS; agent++ )
		agentLocationIDs[agent] = locationIDs[agent];
	}
    }
    
    public boolean equals( Object obj )
    {
	if( obj == null )
	    return false;
	else if( !(obj instanceof GameState) )
	    return false;
	else {
	    
	    GameState state = (GameState) obj;
	    
	    for( int agent = 0; agent < LinearGridWorld.NUM_AGENTS; agent++ ) {
		
		if( agentLocationIDs[agent] != state.getLocationID(agent) ) {
		       
		    return false;
		}
	    }
	    
	    
	    return true;
	}
    }
    
    public int hashCode()
    {
	int hCode = 0;
	for( int agent = 0; agent < LinearGridWorld.NUM_AGENTS; agent++ ) {
	 
	    int loc = agentLocationIDs[agent];
	    
	    hCode += loc * (31-agent);
	}
	
	return hCode;
    }
    
    public int getLocationID( int agent ) 
    {
	if( agent >= 0 && agent < LinearGridWorld.NUM_AGENTS )
	    return agentLocationIDs[agent];
	else {
	    
	    System.out.println("@GameState->getLocationID: Invalid Agent ID!");
	    return -1;
	}
    }
    
    public int[] getLocationIDs()
    {
	return agentLocationIDs;
    }
    
    public void setLocationID( int agent, int locationID )
    {
	if( agent >= 0 && agent < LinearGridWorld.NUM_AGENTS && 
		locationID >= 0 && locationID < LinearGridWorld.NUM_LOCATIONS ) {
	    
	    agentLocationIDs[agent] = locationID; 
	}
	else {
	    
	    System.out.println( "@GameState->setLocationID: Something Wrong in Parameters!" );
	}
    }
    
}
