package gamGam;



public class GameState
{
    
    /**
     * the upper and lower bounds for the pot
     * 
     * a pot of 0 means the game is over
     */
    public static final int UPPER_BOUND_POT = 50;
    public static final int LOWER_BOUND_POT = 0;
    
    
    public static final int DCREASE_SIZE = 5;
    public static final double REWARD_SIZE = 5.0;
    
    /**
     * two constants corresponding to the variable bigsmall
     */
    public static final int SMALL_WIN = 0;
    public static final int BIG_WIN = 1;
    
    
 
    /**
     * the current pot of the game 
     */
    private int pot = UPPER_BOUND_POT;
    
    /**
     * this variable determines which team (the team bets the bigger size or 
     * that bets the smaller size) wins the reward in the last state.
     * 
     * pot + bigsmall = current state
     * note that for the initial state (i.e., pot = 10),
     * this variable is meaningless
     */
    private int big_small = 0;
    
    
    public GameState( int pt, int bg_sml )
    {
	
	if( pt > UPPER_BOUND_POT || pt < LOWER_BOUND_POT ||
		bg_sml < SMALL_WIN || bg_sml > BIG_WIN ) {
	    
	    System.out.println("GameState->constructor: Parameter Errors!");
	    
	    pot = UPPER_BOUND_POT;
	    big_small = SMALL_WIN;
	}
	else {
	 
	    pot = pt;
	    
	    //to guarantee that there is only one initial state
	    if( pot == UPPER_BOUND_POT )
		big_small = SMALL_WIN;
	    else
		big_small = bg_sml;
	}
    }
    
    
    public int getPot() 
    {
	return pot;
    }
    
    public boolean isTerminalState()
    {
	if( pot == LOWER_BOUND_POT )
	    return true;
	else
	    return false;
    }
    
    public boolean isInitialState()
    {
	if( pot == UPPER_BOUND_POT )
	    return true;
	else
	    return false;
    }
    
    public int getBigSmall()
    {
	
	return big_small;
    }
    
    public void setPot( int pt )
    {
	if( pt > UPPER_BOUND_POT || pt < LOWER_BOUND_POT ) {
	    
	    System.out.println("GameState->setPot: Parameter Errors!");
	    return;
	}
	else {
	 
	    pot = pt;
	}
    }
    
    public void setBigSmall( int bg_sml )
    {
	if( bg_sml < SMALL_WIN || bg_sml > BIG_WIN ) {
	    
	    System.out.println("GameState->setBigSmall: Parameter Errors!");
	    return;
	}
	else {
	
	    big_small = bg_sml;
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
	    
	    if( state.getPot() == UPPER_BOUND_POT && 
		    pot == UPPER_BOUND_POT )
		return true;
	    else if( state.getPot() == pot && 
		    state.getBigSmall() == big_small ) 		
		return true;
	    else
		return false;
	}
    }
    
    //change with the upper bound of pot?
    public int hashCode()
    {
	int hCode = 0;
	if( pot == UPPER_BOUND_POT )
	    hCode = pot * 10;
	else
	    hCode = pot * 10 + big_small;
	
	return hCode;
    }
    
}
