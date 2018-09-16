package gameOneCardPoker;

import gameOneCardPoker.algorithms.*;




public class Agent {

	private int seat;
	
	
	//for recording all results obtained from the beginning of the game
	double allResults = 0.0;
	
	double resultsPlayer1 = 0.0;
	double resultsPlayer2 = 0.0;
	double player1Count = 0.0;
	double player2Count = 0.0;
	
	/**
	 * algorithm used for this agent
	 */
	private int algorithmType = 0;
	private Algorithm algorithm;
	
	public Agent()
	{
		
		algorithm = new Algorithm();
		algorithmType = Algorithm.ALG_DEFAULT;
	}
	
	/**
	public Agent( int algType )
	{
		switch( algType ) {
		
		case Algorithm.ALG_OFFLINE_CFR:
			algorithm = new CFR();
			break;
		case Algorithm.ALG_OFFLINE_MCCFR:
			algorithm = new MCCFR();
			break;
		case Algorithm.ALG_OFFLINE_LCFR:
			algorithm = new LCFR();
			break;
		case Algorithm.ALG_ONLINE_MCCFROS:
			algorithm = new onMCCFRos();
			break;
		case Algorithm.ALG_ONLINE_onLCFR:
			algorithm = new onLCFR();
			break;
		case Algorithm.ALG_ONLINE_STATIC:
			algorithm = new StaticAlgorithm();
			break;
		case Algorithm.ALG_ONLINE_RANDOM:
			algorithm = new RandomAlgorithm();
			break;
		case Algorithm.ALG_ONLINE_SOPHISSTATIC:
			algorithm = new SophisStatic();
			break;
		case Algorithm.ALG_ONLINE_DYNAMIC:
			algorithm = new Dynamic();
			break;
		case Algorithm.ALG_ONLINE_BESTNASH:
			algorithm = new BestNash();
			break;
		case Algorithm.ALG_ONLINE_DBBR:
			algorithm = new DBBR();
			break;
		case Algorithm.ALG_ONLINE_BEFEWP:
			algorithm = new BEFEWP();
			break;
		case Algorithm.ALG_ONLINE_BEFFE:
			algorithm = new BEFFE(30000);
			break;
		case Algorithm.ALG_REGRET_LEARNING:
			algorithm = new RegretLearning();
			break;
		default:
			algorithm = new Algorithm();
			break;
		}
		
		algorithmType = algType;
	}
	*/
	
	public Agent( int algType, int gameNum )
	{
		switch( algType ) {
		
		case Algorithm.ALG_OFFLINE_CFR:
			algorithm = new CFR();
			break;
		case Algorithm.ALG_OFFLINE_MCCFR:
			algorithm = new MCCFR();
			break;
		case Algorithm.ALG_OFFLINE_LCFR:
			algorithm = new LCFR();
			break;
		case Algorithm.ALG_ONLINE_MCCFROS:
			algorithm = new onMCCFRos();
			break;
		case Algorithm.ALG_ONLINE_onLCFR:
			algorithm = new onLCFR();
			break;
		case Algorithm.ALG_ONLINE_NASH:
			algorithm = new Nash();
			break;
		case Algorithm.ALG_ONLINE_RANDOM:
			algorithm = new RandomAlgorithm();
			break;
		case Algorithm.ALG_ONLINE_SOPHISSTATIC:
			algorithm = new SophisStatic();
			break;
		case Algorithm.ALG_ONLINE_DYNAMIC:
			algorithm = new Dynamic( gameNum );
			break;
		case Algorithm.ALG_ONLINE_BESTNASH:
			algorithm = new BestNash( gameNum );
			break;
		case Algorithm.ALG_ONLINE_DBBR:
			algorithm = new DBBR( gameNum );
			break;
		case Algorithm.ALG_ONLINE_BEFEWP:
			algorithm = new BEFEWP();
			break;
		case Algorithm.ALG_ONLINE_BEFFE:
			algorithm = new BEFFE( gameNum );
			break;
		case Algorithm.ALG_REGRET_LEARNING:
			algorithm = new RegretLearning();
			break;
		case Algorithm.ALG_OFFLINE_CFRRL:
			algorithm = new CFRRL();
			break;
		case Algorithm.ALG_ONLINE_onCFRRL:
			algorithm = new onCFRRL();
			break;
		case Algorithm.ALG_ACTORCRITIC:
			algorithm = new RL_ActorCritic();
			break;
		case Algorithm.ALG_QLEARNING:
			algorithm = new RL_Qlearning();
			break;
		case Algorithm.ALG_SARSA:
			algorithm = new RL_Sarsa();
			break;
		default:
			algorithm = new Algorithm();
			break;
		}
		
		algorithmType = algType;
	}
	
	//Gets and Sets///////////////////////////////////////////////
	
	public int getSeat() { return seat; }
	
	public void setSeat( int s )
	{ 
		if( s < 0 || s > 1 ) {
			
			return;
		}
		
		seat = s;
		
		algorithm.setSeat(seat);
		

	}
	
	public double getAllResults() { return allResults; }
	
	
	public double getWinRate( int seat )
	{
		if( seat == 0 ) {
			
			if( player1Count <= 0.0 )
				return 0;
			else
				return resultsPlayer1 / player1Count;
		}
		else if( seat == 1 ) {
			
			if( player2Count <= 0.0 )
				return 0;
			else
				return resultsPlayer2 / player2Count;
		}
		else {
			
			if( player1Count + player2Count <= 0.0 )
				return 0;
			else
				return (resultsPlayer1+resultsPlayer2) / (player1Count+player2Count);
		}
	}
	
	public void resultAdded(double rlt)
	{
		allResults += rlt;
		
		if( seat == 0 )
			resultsPlayer1 += rlt;
		else
			resultsPlayer2 += rlt;
		
		if( seat == 0 )
			player1Count += 1.0;
		else
			player2Count += 1.0;
	}
	
	public void resetAllResults() 
	{
		allResults = 0.0;
		resultsPlayer1 = 0.0;
		resultsPlayer2 = 0.0;
	}
	
	public int getAlgorithmType() { return algorithmType; }
	
	public Algorithm getAlgorithm() { return algorithm; }
	
	//Gets and Sets///////////////////////////////////////////////

	
	public void selfPlay()
	{
		algorithm.selfPlay();
	}
	
	public void initOnlinePlay()
	{
		algorithm.initOnlinePlay();
	}
	
	public void onlinePlay_GameStarts( GameState gameState, long T )
	{
		algorithm.onlinePlayer_GameStarts( gameState, T );
	}
	
	public int onlinePlay_ChooseAction( GameState gameState, long T )
	{
		return algorithm.onlinePlay_ChooseAction( gameState, T );
	}
	
	public void onlinePlay_GameOver( GameState gameState, long T )
	{
		algorithm.onlinePlay_GameOver( gameState, T );
	}
	
	public void onlinePlay_ObserveOpponent( GameState gameState, long T, int action )
	{
		algorithm.onlinePlay_ObserveOpponent( gameState, T, action );
	}
	
	public void displayPolicy()
	{
		algorithm.displayPolicy();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		Agent agent = new Agent( Algorithm.ALG_ACTORCRITIC, 2000 );
		agent.selfPlay();
		
	}

}
