package gameOneCardPoker.algorithms;

import gameOneCardPoker.Action;
import gameOneCardPoker.Card;
import gameOneCardPoker.OneCardPoker;

public class CFR_Series extends Algorithm {

	/**
	 * two policies
	 */
	protected double[][][] policy;
	protected double[][][] average_policy;
	
	/**
	 * for computing average strategy
	 */
	protected double[][][] acc_policy;
	
	/**
	 * regret table
	 */
	protected double[][][] regret;
	
	public CFR_Series()
	{
		super();
		
		policy = new double[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		average_policy = new double[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		acc_policy = new double[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		regret = new double[Card.NUM_RANKS][OneCardPoker.NUM_HISTORY_IDS][Action.NUM_ACTION_TYPES];
		
		for( int rank = 0; rank < Card.NUM_RANKS; rank++ ) {
			
			for( int hisID = 0; hisID < OneCardPoker.NUM_HISTORY_IDS; hisID++ ) {
				
				for( int a = 0; a < Action.NUM_ACTION_TYPES; a++ ) {
					
					policy[rank][hisID][a] = 0.5;
					average_policy[rank][hisID][a] = 0.0;
					acc_policy[rank][hisID][a] = 0.0;
					regret[rank][hisID][a] = 0.0;
				}
			}
		}
	}
}
