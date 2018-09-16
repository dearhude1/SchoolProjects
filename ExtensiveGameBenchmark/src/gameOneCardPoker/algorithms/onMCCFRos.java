package gameOneCardPoker.algorithms;

import gameOneCardPoker.Action;
import gameOneCardPoker.Card;
import gameOneCardPoker.GameState;
import gameOneCardPoker.Helper;
import gameOneCardPoker.OneCardPoker;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class onMCCFRos extends CFR_Series {

	
	
	/**
	 * for each information set
	 * record the last time it was visited
	 */
	private long[][] visitMarkers;
	
	private double EPSILON = 0.01;
	
	
	//MCCFRos online
	private ArrayList<GameState> gameStates;
	private ArrayList<Integer> actionList;
	
	//for recording all results obtained from the beginning of the game
	double allResults = 0.0;
	
	public onMCCFRos()
	{
		super();
		
		visitMarkers = new long[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS];
		
		for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
			
			for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
				
				visitMarkers[rank][hisID] = 0; 
			}
		}
		
		gameStates = new ArrayList<GameState>();
		actionList = new ArrayList<Integer>();
		
	}
	
	public void initOnlinePlay()
	{
		//readNash();
	}
	
	public int onlinePlay_ChooseAction( GameState state, long T )
	{
		int hisID = OneCardPoker.historyID( state );
		int playerRank = Card.rankOfCard( state.getPlayerCard(seat) );
		
		OneCardPoker.checkOver( state );
		if( state.isGameOver() ) {
			
			return Action.ACTION_INVALID;
		}
		else {
			
			
			//record the state and actions
			gameStates.add( new GameState(state) );
			
			if( state.getActingPlayer() != seat ){
				
				actionList.add( Action.ACTION_INVALID );
				
				return Action.ACTION_INVALID;
			}
			
			//choose an action and return
			else {
				
				int retAction = 0;
				double[] pro = new double[Action.NUM_ACTION_TYPES];
				if( new Random().nextDouble() < EPSILON ) {
					
					pro[0] = 1.0 / Action.NUM_ACTION_TYPES;
					for( int a = 1; a < Action.NUM_ACTION_TYPES; a++ ) {
						
						pro[a] = pro[a-1] + 1.0 / Action.NUM_ACTION_TYPES;
					}
				}
				else {
					
					pro[0] = policy[playerRank][hisID][0];
					for( int a = 1; a < Action.NUM_ACTION_TYPES; a++ ) {
						
						pro[a] = pro[a-1] + policy[playerRank][hisID][a];
					}	
				}
				
				double playerPro = new Random().nextDouble();
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					if( playerPro < pro[a] ) {
						
						retAction = a;
						break;
					}
				}
				
				actionList.add( retAction );
				
				return retAction;
			}
		}
	}
	
	
	public void onlinePlay_GameOver( GameState state, long T )
	{
		
		OneCardPoker.checkOver( state );
		if( !state.isGameOver() ) {
			
			return;
		}
		
		//update average strategy
		double p_i = 1.0;
		for( int index = 0; index < gameStates.size(); index++ ) {
			
			GameState gs = gameStates.get(index);
			int a = actionList.get(index);
			int h = OneCardPoker.historyID( gs );
			int rank = Card.rankOfCard( gs.getPlayerCard(seat) );
			
			if( gs.getActingPlayer() == seat ) {
				
				long lastTime = visitMarkers[rank][h];
				for( int ap = 0; ap < Action.NUM_ACTION_TYPES; ap++ ) {
					
					acc_policy[rank][h][ap] += (T-lastTime) * p_i * policy[rank][h][ap];
				}
				visitMarkers[rank][h] = T; 
				p_i *= policy[rank][h][a];
			}
		}
		
		//for obtaining sampling prob
		double sampro = 1.0;
		for( int index = 0; index < gameStates.size(); index++ ) {
			
			GameState gs = gameStates.get(index);
			if( gs.getActingPlayer() == seat ) {
				
				int h = OneCardPoker.historyID( gs );
				int rank = Card.rankOfCard( gs.getPlayerCard(seat) );
				int a = actionList.get(index);
				
				double ep = (1-EPSILON) * policy[rank][h][a] + EPSILON / ((double) Action.NUM_ACTION_TYPES);
				sampro *= ep;
			}
		}
		
		//for obtaining value and regret
		double result = OneCardPoker.getUtility( state, seat );
		double v_Ip = result / sampro;
		double v_I = 0.0;
		for( int index = gameStates.size()-1; index >= 0; index-- ) {
			
			GameState gs = gameStates.get(index);
			int a = actionList.get(index);
			int h = OneCardPoker.historyID( gs );
			int rank = Card.rankOfCard( gs.getPlayerCard(seat) );
			
			if( gs.getActingPlayer() != seat ) {
				
				v_I = v_Ip;
				v_Ip = v_I;
			}
			else {
				
				//update value and regret
				v_I = v_Ip * policy[rank][h][a];
				for( int ap = 0; ap < Action.NUM_ACTION_TYPES; ap++ ) {
					
					double r = regret[rank][h][ap];
					
					if( ap == a )
						r = r + (v_Ip - v_I);
					else
						r = r + (0 - v_I);
					
					regret[rank][h][ap] = r;
				}
				v_Ip = v_I;
				
				//update policy
				double sumPositiveRegret = 0.0;
				for( int ap = 0; ap < Action.NUM_ACTION_TYPES; ap++ ) {
					
					double posRegret = regret[rank][h][ap];
					if( posRegret < 0.00001 )
						posRegret = 0;
					sumPositiveRegret += posRegret;
				}
				for( int ap = 0; ap < Action.NUM_ACTION_TYPES; ap++ ) {
					
					double posRegret = regret[rank][h][ap];
					if( posRegret < 0.00001 )
						posRegret = 0;
					if( sumPositiveRegret < 0.00001 )
						policy[rank][h][ap] = 1.0 / Action.NUM_ACTION_TYPES;
					else
						policy[rank][h][ap] = posRegret / sumPositiveRegret;
				}
			}
		}
		
		
		//clean the list
		gameStates.clear();
		actionList.clear();
		
		Helper.computeAverageStrategy( acc_policy, average_policy );
		//computeExploitability();
	}
	
	private void readNash()
	{
		try {
			
			BufferedReader bufReader = new BufferedReader(new FileReader("./Nash.txt"));
			
			
			String line = "";
			int rank = 0;
			int his = 0;
			int a = 0;
			while( (line = bufReader.readLine()) != null ) {
				
				if( line.length() == 0 )
					continue;
				
				//Nash_policy[rank][his][a] = Double.parseDouble(line);
				
				policy[rank][his][a] = Double.parseDouble(line);
				
				a++;
				if( a >= Action.NUM_ACTION_TYPES ) {
					
					a = 0;
					his++;
					if( his >= OneCardPoker.NUM_HISTORY_IDS ) {
						
						his = 0;
						rank++;
					}
				}
			}
			
			bufReader.close();
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
	}

}
