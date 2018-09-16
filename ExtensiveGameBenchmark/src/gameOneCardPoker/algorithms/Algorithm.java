package gameOneCardPoker.algorithms;

import java.util.Random;

import gameOneCardPoker.Action;
import gameOneCardPoker.GameState;
import gameOneCardPoker.OneCardPoker;

/**
 * Algorithm
 * The base class of all algorithms for solving extensive games
 * @author dearhude1
 *
 */
public class Algorithm {

	/**
	 * Algorithm Type Definition
	 */
	public static final int ALG_DEFAULT = 0;
	public static final int ALG_OFFLINE_CFR = 1;
	public static final int ALG_OFFLINE_MCCFR = 2;
	public static final int ALG_OFFLINE_LCFR = 3;
	public static final int ALG_ONLINE_onLCFR = 4;
	public static final int ALG_ONLINE_MCCFROS = 5;
	public static final int ALG_ONLINE_NASH = 6;
	public static final int ALG_ONLINE_RANDOM = 7;
	public static final int ALG_ONLINE_SOPHISSTATIC = 8;
	public static final int ALG_ONLINE_DYNAMIC = 9;
	public static final int ALG_ONLINE_BESTNASH = 10;
	public static final int ALG_ONLINE_BESTRESPONSE = 11;
	public static final int ALG_ONLINE_DBBR = 12;
	public static final int ALG_ONLINE_BEFEWP = 13;
	public static final int ALG_ONLINE_BEFFE = 14;
	
	public static final int ALG_REGRET_LEARNING = 15;
	
	public static final int ALG_OFFLINE_CFRRL = 16;
	public static final int ALG_ONLINE_onCFRRL = 17;
	
	public static final int ALG_ACTORCRITIC = 18;
	
	public static final int ALG_QLEARNING = 19;
	public static final int ALG_SARSA = 20;
	
	public static final String[] ALG_STRINGS = {"default", "CFR", "MCCFR", "LCFR", "onLCFR", "onMCCFROS", 
		"NE", "Random", "Sophi", "Dynamic", "BestNash", "BestResponse", "DBBR", "BEFEWP", "BEFFE", "RegretLearning",
		"CFRRL", "onCFRRL", "ActorCritic", "Qlearning", "Sarsa" };
	
	protected int seat = 0;

	
	public Algorithm()
	{

	}
	
	public void selfPlay()
	{
		
	}
	
	public void initOnlinePlay()
	{
		
	}
	
	public void onlinePlayer_GameStarts( GameState gameState, long T )
	{
		
	}
	
	public int onlinePlay_ChooseAction( GameState gameState, long T )
	{
		OneCardPoker.checkOver( gameState );
		if( gameState.isGameOver() ) {
			
			return Action.ACTION_INVALID;
		}
		else {
			
			if( new Random().nextDouble() < 0.5 )
				return Action.ACTION_BET_ZERO;
			else
				return Action.ACTION_BET_ONE;
		}
	}
	
	public void onlinePlay_GameOver( GameState gameState, long T )
	{
		
	}
	
	public void onlinePlay_ObserveOpponent( GameState gameState, long T, int action )
	{
		
	}
	
	
	public void displayPolicy()
	{
		
	}
	
	public int getSeat() { return seat; }
	
	public void setSeat( int s )
	{ 
		if( s < 0 || s > 1 ) {
			
			return;
		}
		
		seat = s;
	}
}
