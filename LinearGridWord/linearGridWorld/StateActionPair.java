package linearGridWorld;

public class StateActionPair
{

    private GameState gameState;
    private GameAction gameAction;
    
    public StateActionPair( GameState state, GameAction jntAction ) 
    {
	if( state == null || jntAction == null ) {
	    
	    System.out.println("StateActionPair->Constructor: Null Parameter");
	    
	    gameState = new GameState();
	    gameAction = new GameAction();
	}
	
	else {
	    
	    gameState = new GameState( state.getLocationIDs() );
	    gameAction = new GameAction();
	    
	    for( int agent = 0; agent < LinearGridWorld.NUM_AGENTS; agent++ ) {
		 
		gameAction.setAction( agent, jntAction.getAction(agent) );
	    }
	}
    }
    
    public StateActionPair( int[] locationIDs, int[] agentActions )
    {
	if( locationIDs == null || agentActions == null ) {
	    
	    System.out.println("StateActionPair->Constructor: Null Parameter");
	    
	    gameState = new GameState();
	    gameAction = new GameAction();
	}
	
	else {
	    
	    gameState = new GameState( locationIDs );
	    gameAction = new GameAction( agentActions );
	}
    }
    
    public GameState getGameState() 
    {
	return gameState;
    }
    
    public GameAction getGameAction()
    {
	return gameAction;
    }
    
    
    public boolean equals( Object obj )
    {
	if( obj == null )
	    return false;
	else if( !(obj instanceof StateActionPair) )
	    return false;
	else {
	    
	    StateActionPair saPair = (StateActionPair) obj;
	    
	    if( gameState.equals( saPair.getGameState() ) && 
		    gameAction.equals( saPair.getGameAction() ) )
		return true;
	    else 
		return false;
	}
    }
    
    public int hashCode()
    {
	
	int stateHCode = gameState.hashCode();
	int actionHCode = gameAction.hashCode();
	
	return (5*stateHCode + actionHCode);
    }

}
