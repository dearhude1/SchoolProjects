package gameOneCardPoker;

public class GameState {
	
	private boolean gameOver = false;
	
	private int[] playerCards;
	
	private int[] actionSequence;
	
	
	/**
	 * number of actions which have been taken in the current game
	 */
	private int numActions;
	
	/**
	 * current player who is going to take action
	 */
	private int actingPlayer;
	
	private double pot;
	
	private double[] bet;
	
	//Constructor//////////////////////////////////////////////////////////
	public GameState()
	{
		gameOver = false;
		numActions = 0;
		playerCards = new int[OneCardPoker.MAX_PLAYERS];
		actionSequence = new int[OneCardPoker.MAX_NUM_ACTIONS];
		
		/**
		 * action sequence
		 */
		for( int i = 0; i < OneCardPoker.MAX_NUM_ACTIONS; i++ ) {
			
			actionSequence[i] = Action.ACTION_INVALID; 
		}
		
		/**
		 * cards
		 */
		for( int p = 0; p <= 1; p++ ) {
			
			playerCards[p] = Card.INVALID_CARD;
		}
		
		/**
		 * the first acting player is the one whose seat is 0
		 */
		actingPlayer = 0;
		
		/**
		 * each player puts $1 ante at the beginning of a game
		 */
		pot = 2.0;
		
		/**
		 * the money bet by each player
		 */
		bet = new double[2];
		for( int p = 0; p <= 1; p++ ) {
			
			bet[p] = 1.0; 
		}
	}
	
	public GameState( GameState state )
	{
		gameOver = state.isGameOver();
		numActions = state.getNumActions();
		playerCards = new int[OneCardPoker.MAX_PLAYERS];
		actionSequence = new int[OneCardPoker.MAX_NUM_ACTIONS];
		
		/**
		 * action sequence
		 */
		for( int i = 0; i < OneCardPoker.MAX_NUM_ACTIONS; i++ ) {
			
			actionSequence[i] = state.getAction(i);
		}
		
		/**
		 * cards
		 */
		for( int p = 0; p <= 1; p++ ) {
			
			playerCards[p] = state.getPlayerCard(p);
		}
		
		/**
		 * the first acting player is the one whose seat is 0
		 */
		actingPlayer = state.getActingPlayer();
		
		/**
		 * pot
		 */
		pot = state.getPot();
		
		/**
		 * the money bet by each player
		 */
		bet = new double[2];
		for( int p = 0; p <= 1; p++ ) {
			
			bet[p] = state.getBet(p); 
		}
	}
	//Constructor//////////////////////////////////////////////////////////

	//Gets and Sets//////////////////////////////////////////////////////////
	public boolean isGameOver() { return gameOver; }
	
	public void setGameOver( boolean over ) { gameOver = over; }
	
	public int getNumActions() { return numActions; }
	
	public int getPlayerCard( int player ) 
	{ 
		
		if( player == 0 || player == 1 )
			return playerCards[player]; 
		else
			return Card.INVALID_CARD;
	}
	
	public int getAction( int index )
	{ 
		if( index >= 0 && index < OneCardPoker.MAX_NUM_ACTIONS )
			return actionSequence[index];
		else {
			
			return Action.ACTION_INVALID;
		}
	}
	
	public double getPot() { return pot; }
	
	public double getBet( int player )
	{
		if( player != 0 && player != 1 ) {
			
			System.out.println("@GameState::getBet: Invalid player!" );
			return 0;
		}
		else
			return bet[player];
	}
	
	public int getActingPlayer() { return actingPlayer; }
	
	/**
	 * compute current history according to the action sequence
	 * @return: a String
	 */

	public String getCurrentHistory()
	{
		String history = new String();
		
		for( int i = 0; i < numActions; i++ ) {
			
			history += Action.ACTION_CHARS[actionSequence[i]];
		}
		
		return history;
	}
	//Gets and Sets//////////////////////////////////////////////////////////
	
	//Help Methods///////////////////////////////////////////////////////////
	public void receiveCard( int player, int card )
	{
		if( player < 0 || player > 1 ) {
			
			System.out.println( "@GameState:receiveCard: Invalid player!" );
			return;
		}
		else if( card < Card.FIRST_CARD_INDEX || card > Card.LAST_CARD_INDEX ) {
			
			System.out.println( "@GameState::receiveCard: Invalid card!" );
			return;
		}
		
		playerCards[player] = card; 
	}

	
	public void receiveAction( int action )
	{
		if( gameOver ) {
			
			System.out.println( "@GameState::receiveAction: Game is over!" );
			return;
		}
		else if( numActions >= OneCardPoker.MAX_NUM_ACTIONS ) {
			
			System.out.println( "@GameState::receiveAction: No more actions can be taken. Three actions have been taken!" );
			System.out.println( getCurrentHistory() );
			return;
		}
		else if( action != Action.ACTION_BET_ONE && 
				action != Action.ACTION_BET_ZERO ) {
			
			System.out.println("@GameState::receiveAction: Invalid Action!");
			return;
		}
		
		/**
		 * record the current action
		 */
		actionSequence[numActions] = action;
		
		/**
		 * then the money
		 */
		pot += Action.actionSize( action );
		bet[actingPlayer] += Action.actionSize( action );
		
		numActions++;
	}
	
	public void nextPlayer()
	{
		if( !gameOver )
			actingPlayer = 1 - actingPlayer;
	}
	
	public void resetState()
	{
		gameOver = false;
		numActions = 0;
		
		/**
		 * action sequence
		 */
		for( int i = 0; i < OneCardPoker.MAX_NUM_ACTIONS; i++ ) {
			
			actionSequence[i] = Action.ACTION_INVALID; 
		}
		
		/**
		 * cards
		 */
		for( int p = 0; p <= 1; p++ ) {
			
			playerCards[p] = Card.INVALID_CARD;
		}
		
		/**
		 * the first acting player is the one whose seat is 0
		 */
		actingPlayer = 0;
		
		/**
		 * each player puts $1 ante at the beginning of a game
		 */
		pot = 2.0;
		
		/**
		 * the money bet by each player
		 */
		bet = new double[2];
		for( int p = 0; p <= 1; p++ ) {
			
			bet[p] = 1.0; 
		}
	}
	//Help Methods///////////////////////////////////////////////////////////
	
}
