package gameOneCardPoker;

public class Action {

	public static final int NUM_ACTION_TYPES = 2;
	
	public static final int ACTION_BET_ZERO = 0;
	public static final int ACTION_BET_ONE = 1;
	public static final int ACTION_INVALID = 2;
	
	public static final char[] ACTION_CHARS = {'z','o','x'};
	
	public static int actionSize( int action )
	{
		if( action < 0 || action > 1 )
			return -1;
		else
			return action;
	}
}
