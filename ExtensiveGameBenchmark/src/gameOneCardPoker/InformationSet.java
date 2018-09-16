package gameOneCardPoker;

public class InformationSet {

	private int cardRank;
	
	private int historyID;
	
	public InformationSet()
	{
		cardRank = 0;
		historyID = 0;
	}
	
	public InformationSet( int rank, int hisID )
	{
		if( rank < 0 || rank >= Card.NUM_RANKS )
			rank = 0;
		if( hisID < 0 || hisID >= OneCardPoker.NUM_HISTORY_IDS )
			hisID = 0;
		
		cardRank = rank;
		historyID = hisID;
	}
	
	public int getCardRank() { return cardRank; }
	
	public int getHistoryID() { return historyID; }
}
