package soccerGame;

public class StateActionPair
{

    private GameState gameState;
    private GameAction gameAction;
    
    public StateActionPair( GameState state, GameAction jntAction ) 
    {
	if( state == null || jntAction == null ) {
	    
	    System.out.println("StateActionPair->Constructor: Null Parameter");
	    
	    gameState = new GameState(0);
	    gameAction = new GameAction();
	}
	
	else {
	    
	    gameState = new GameState( state.getLocationIDs(), state.getBallPossession() );
	    gameAction = new GameAction();
	    
	    for( int agent = 0; agent < SoccerGame.NUM_TEAM_AGENTS * SoccerGame.NUM_TEAMS; agent++ ) {
		 
		gameAction.setAction( agent, jntAction.getAction(agent) );
	    }
	}
    }
    
    public StateActionPair( int[] locationIDs, int[] agentActions, int ballPossAgent )
    {
	if( locationIDs == null || agentActions == null ) {
	    
	    System.out.println("StateActionPair->Constructor: Null Parameter");
	    
	    gameState = new GameState( ballPossAgent );
	    gameAction = new GameAction();
	}
	
	else {
	    
	    gameState = new GameState( locationIDs, ballPossAgent );
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
