package gameGridWorld;


public class GameState
{
    
    /**
     * determined by the map of the game
     */
    private static int NUM_AGENTS = 0;
    
    /**
     * the current location of each agent 
     * at this state
     */
    private int[] agentCellIndices;
    
    private int stateIndex = 0;
    
    public GameState()
    {
	agentCellIndices = new int[NUM_AGENTS];
	
	for( int agent = 0; agent < NUM_AGENTS; agent++ )
	    agentCellIndices[agent] = GridWorldLocation.INVALID_LOCATION_ID;
    }
    
    public GameState( int cellIndices[] )
    {
	agentCellIndices = new int[NUM_AGENTS];
	if( cellIndices == null || 
		cellIndices.length != NUM_AGENTS ) {
	    
	    for( int agent = 0; agent < NUM_AGENTS; agent++ )
		agentCellIndices[agent] = GridWorldLocation.INVALID_LOCATION_ID;
	}
	else {
	    
	    for( int agent = 0; agent < NUM_AGENTS; agent++ )
		agentCellIndices[agent] = cellIndices[agent];
	}
    }
    
    public int getLocationID( int agent ) 
    {
	if( agent >= 0 && agent < NUM_AGENTS )
	    return agentCellIndices[agent];
	else {
	    
	    System.out.println("@GameState->getLocationID: Invalid Agent ID!");
	    return GridWorldLocation.INVALID_LOCATION_ID;
	}
    }
    
    public int[] getLocationIDs()
    {
	return agentCellIndices;
    }
    
    public void setLocationID( int agent, int locationID )
    {
	if( agent >= 0 && agent < NUM_AGENTS && 
		locationID >= 0 && locationID < SparseGridWorld.NUM_CELLS ) {
	    
	    agentCellIndices[agent] = locationID; 
	}
	else {
	    
	    System.out.println( "@GameState->setLocationID: Something Wrong in Parameters!" );
	}
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
    
    public boolean equals( Object obj )
    {
	
	if( obj == null )
	    return false;
	else if( !(obj instanceof GameState) )
	    return false;
	else {
	    
	    GameState state = (GameState) obj;
	    
	    for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
		
		if( agentCellIndices[agent] != state.getLocationID(agent) ) {
		       
		    return false;
		}
	    }
	    
	    return true;
	}
    }
    
    public int hashCode()
    {
	int hCode = 0;
	for( int agent = 0; agent < NUM_AGENTS; agent++ ) {
	 
	    int loc = agentCellIndices[agent];
	    
	    hCode += loc * (31-agent);
	}
	
	return hCode;
    }
    
    public static int memorySize()
    {
	
	return Integer.SIZE * NUM_AGENTS;
    }
    
    public void setIndex( int index ) 
    {
	stateIndex = index;
    }
    
    public int getIndex()
    {
	
	return stateIndex;
    }
    
}
