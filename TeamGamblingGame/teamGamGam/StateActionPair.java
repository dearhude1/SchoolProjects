package teamGamGam;

public class StateActionPair
{

    private GameState gameState;
    private GameAction gameAction;
    
    public StateActionPair( GameState state, GameAction jntAction ) 
    {
	if( state == null || jntAction == null ) {
	    
	    System.out.println("StateActionPair->Constructor: Null Parameter");
	    
	    gameState = new GameState( GameState.UPPER_BOUND_POT, GameState.SMALL_WIN );
	    gameAction = new GameAction();
	}
	
	else {
	    
	    gameState = new GameState( state.getPot(), state.getBigSmall() );
	    gameAction = new GameAction();
	    
	    for( int agent = 0; agent < GamblingGame.NUM_TEAM_AGENTS * GamblingGame.NUM_TEAMS; agent++ ) {
		 
		gameAction.setAction( agent, jntAction.getAction(agent) );
	    }
	}
    }
    
    public StateActionPair( int pot, int big_small, int[] agentActions )
    {
	if( agentActions == null || pot > GameState.UPPER_BOUND_POT || 
		pot < GameState.LOWER_BOUND_POT || big_small > GameState.BIG_WIN ||
		big_small < GameState.SMALL_WIN ) {
	    
	    System.out.println("StateActionPair->constructor: Parameter Errors!");
	    
	    gameState = new GameState( GameState.UPPER_BOUND_POT, GameState.SMALL_WIN );
	    gameAction = new GameAction();
	}
	
	else {
	    
	    gameState = new GameState( pot, big_small );
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
