package gameOneCardPoker;

public class Card {

	public static final int NUM_SUITS = 4;
	public static final int NUM_RANKS = 13;
	
	public static final char[] SUIT_CHARS = {'c','d','h','s'};
	public static final char[] RANK_CHARS = {'2','3','4','5','6','7','8','9','T','J','Q','K','A'};
	
	public static final int FIRST_CARD_INDEX = 0;
	public static final int LAST_CARD_INDEX = 51;
	public static final int INVALID_CARD = -1;
	
	public static int suitOfCard( int card )
	{
		if( card < 0 || card > 51 )
			return -1;
		
		return card % NUM_SUITS;
	}
	
	public static int rankOfCard( int card )
	{
		if( card < 0 || card > 51 )
			return -1;
		
		return card / NUM_SUITS;
	}
	
	public static boolean higher( int card1, int card2 )
	{
		int rank1 = rankOfCard( card1 );
		int rank2 = rankOfCard( card2 );
		
		if( rank1 > rank2 )
			return true;
		else
			return false;
	}
	
	public static boolean equal( int card1, int card2 )
	{
		int rank1 = rankOfCard( card1 );
		int rank2 = rankOfCard( card2 );
		
		if( rank1 == rank2 )
			return true;
		else
			return false;
	}
}
