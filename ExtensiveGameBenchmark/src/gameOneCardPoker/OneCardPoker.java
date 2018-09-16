package gameOneCardPoker;


/**
 * The class of one-card poker game
 * @author dearhude1
 */
public class OneCardPoker {

	/**
	 * There are only two players in one-card poker game.
	 */
	public static final int MAX_PLAYERS = 2;
	
	/**
	 * All available action sequences are:
	 * zz, zoz, zoo, oz, oo
	 */
	public static final int MAX_NUM_ACTIONS = 3;
	
	public static final int NUM_HISTORY_IDS = 9;
	
	public static final String[] terminalHistories = {"zz","zoz","zoo","oz","oo"};
	
	
	public static GameState doAction( GameState state, int action )
	{
		if( state == null ) {
			
			System.out.println( "@OneCardPoker::doAction: Null game state!" );
			return null;
		}
		else if( action != Action.ACTION_BET_ONE && 
				action != Action.ACTION_BET_ZERO ) {
			
			System.out.println( "@OneCardPoker::doAction: Invalid action!" );
			return null;
		}
		else if( state.isGameOver() ) {
			
			System.out.println( "@OneCardPoker::doAction: The game is over!" );
			return state;
		}

		
		/**
		 * do the action
		 */
		state.receiveAction( action );
		
		/**
		 * check if the game is over
		 */
		checkOver( state );
		
		/**
		 * change the player
		 */
		if( !state.isGameOver() )
			state.nextPlayer();
		
		return state;
	}
	
	public static void checkOver( GameState state )
	{
		if( state == null ) {
			
			System.out.println( "@OneCardPoker::checkOver: Null game state!" );
			return;
		}
		else if( state.isGameOver() ) {
		
			//System.out.println( "@OneCardPoker::checkOver: The game is over!" );
			return;
		}
		
		String curHis = state.getCurrentHistory();
		if( curHis == null ) {
			
			System.out.println( "@OneCardPoker::checkOver: Null current history!" );
			return;
		}
		for( int i = 0; i < terminalHistories.length; i++ ) {
			
			if( curHis.equals(terminalHistories[i]) ) {
				
				state.setGameOver( true );
				break;
			}
		}
	}
	
	/**
	 * utility = return - 
	 * @param terminalHis
	 * @param playerSeat
	 * @return
	 */
	public static double getUtility( GameState state, int playerSeat )
	{
		if( state == null ) {
			
			System.out.println( "@OneCardPoker::getUtility: Null game state!" );
			return 0;
		}
		else if( !state.isGameOver() ) {
			
			System.out.println( "@OneCardPoker::getUtility: The game is not over!" );
			return 0;
		}
		else if( playerSeat != 0 && playerSeat != 1 ) {
			
			System.out.println( "@OneCardPoker::getUtility: Invalid player set!" );
			return 0;
		}
		
		String terminalHis = state.getCurrentHistory();
		
		/**
		 * -1 for loss, 1 for win, 0 for tie
		 */
		int result = 0;
		
		/**
		 * player 0 fold
		 */
		if( terminalHis.equals("zoz") ) {
			
			if( playerSeat == 0 )
				result = -1;
			else
				result = 1;
		}
		/**
		 * player 1 fold
		 */
		else if( terminalHis.equals("oz") ) {
			
			if( playerSeat == 0 )
				result = 1;
			else
				result = -1;
		}
		/**
		 * We need to compare player cards for the following histories
		 */
		else if( terminalHis.equals("zz") ||
				terminalHis.equals("zoo") ||
				terminalHis.equals("oo") ) {
			
			int playerCard = state.getPlayerCard( playerSeat );
			int oppCard = state.getPlayerCard( 1 - playerSeat );
			
			if( Card.higher(playerCard, oppCard) )
				result = 1;
			else if( Card.equal(playerCard, oppCard) )
				result = 0;
			else
				result = -1;
		}
		else {
			return 0;
		}
		
		double pot = state.getPot();
		double bet = state.getBet( playerSeat );
		
		if( result == 1 )
			return ( pot - bet );
		else if( result == 0 )
			return 0;
		else
			return ( -bet );
	}
	
	public static int historyID( GameState state )
	{
		
		String curHis = state.getCurrentHistory();
		
		if( curHis == null ) {
			
			System.out.println( "@OneCardPoker::historyID: Null current history!" );
			return -1;
		}
		
		if( curHis.isEmpty() )
			return 0;
		else if( curHis.equals("z") )
			return 1;
		else if( curHis.equals("o") )
			return 2;
		else if( curHis.equals("zz") )
			return 3;
		else if( curHis.equals("zo") )
			return 4;
		else if( curHis.equals("oz") )
			return 5;
		else if( curHis.equals("oo") )
			return 6;
		else if( curHis.equals("zoz") )
			return 7;
		else if( curHis.equals("zoo") )
			return 8;
		else
			return -1;
	}

	public static boolean isTerminalHistory( int hisID )
	{
		if( hisID == 3 || hisID == 5 ||
				hisID == 6 || hisID == 7 ||
					hisID == 8 )
			return true;
		else
			return false;
	}
	
	public static int getParentHisID( int hisID ) {
		
		if( hisID == 1 || hisID == 2 )
			return 0;
		else if( hisID == 3 || hisID == 4 )
			return 1;
		else if( hisID == 5 || hisID == 6 )
			return 2;
		else if( hisID == 7 || hisID == 8 )
			return 4;
		else
			return -1;
	}
	
	public static int[] getChildHisIDs( int hisID )
	{
		int[] childIDs = new int[2];
		
		if( isTerminalHistory(hisID) )
			return null;
		else if( hisID == 0 ) {
			
			childIDs[0] = 1;
			childIDs[1] = 2;
		}
		else if( hisID == 1 ) {
			
			childIDs[0] = 3;
			childIDs[1] = 4;
		}
		else if( hisID == 2 ) {
			
			childIDs[0] = 5;
			childIDs[1] = 6;
		}
		else {
			
			childIDs[0] = 7;
			childIDs[1] = 8;
		}
		
		return childIDs;
	}
	
	
	public static int getActionFromParent( int hisID, int parentHisID )
	{
		if( parentHisID == 0 ) {
			
			if( hisID == 1 )
				return Action.ACTION_BET_ZERO;
			else if( hisID == 2 )
				return Action.ACTION_BET_ONE;
			else
				return Action.ACTION_INVALID;
		}
		else if( parentHisID == 1 ) {
			
			if( hisID == 3 )
				return Action.ACTION_BET_ZERO;
			else if( hisID == 4 )
				return Action.ACTION_BET_ONE;
			else
				return Action.ACTION_INVALID;
		}
		else if( parentHisID == 2 ) {
			
			if( hisID == 5 )
				return Action.ACTION_BET_ZERO;
			else if( hisID == 6 )
				return Action.ACTION_BET_ONE;
			else
				return Action.ACTION_INVALID;
		}
		else if( parentHisID == 4 ) {
			
			if( hisID == 7 )
				return Action.ACTION_BET_ZERO;
			else if( hisID == 8 )
				return Action.ACTION_BET_ONE;
			else
				return Action.ACTION_INVALID;
		}
		else
			return Action.ACTION_INVALID;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		GameState gameState = new GameState();
		
		/**
		 * step1: get current acting player from state
		 * step2: get the action from the acting player
		 * step3: do the action and change the state
		 * step4: check if the game is over
		 * step5: if not over, turn to next player
		 */
		for( int i = 0; i < 1000; i++ ) {
			
			while( !gameState.isGameOver() ) {
				
				/**
				 * get curret player
				 */
				
				/**
				 * do action
				 */
				
				/**
				 * change the current state
				 */
			}
		}
	}

}
