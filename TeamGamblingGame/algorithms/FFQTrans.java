package algorithms;



import java.util.ArrayList;

import teamGamGam.GamblingGame;
import teamGamGam.GameAction;
import teamGamGam.GameState;
import teamGamGam.TeamAction;


public class FFQTrans extends FFQ
{
    
    /**
     * we use tau to denote the threshold of the error bound
     */
    private double tau = 0.1;//0.05;
    //0.05 for minimaxQ opponent
    //0.05 for random opponent time
    
    public FFQTrans( int index )
    {
	super(index);
	
    }
    
    public FFQTrans( int index, double alpha, double gamma, double epsilon )
    {
	
	super(index, alpha, gamma, epsilon);
	
    }
    
    
    public GameAction updateQ( GameState curState, GameAction jointAction, 
	    double[] rewards, GameState nextState )
    {
	if( nextState == null ) {
	    
	    System.out.println("@MiniMaxQ->updateQ: NULL nextState!");
	    
	    return null;
	}
	else {
	    
	    
	    /**
	     * select action in the next state according to pi(s)
	     */
	    GameAction nextAction = sampleAction( nextState );
	    
	    /**
	     * update the Q-tables
	     * but if this is the initial state of the game
	     * just return the action
	     */
	    if( curState != null && jointAction != null 
		&& rewards != null )  {
		
		
		/**
		 * mark a visit
		 */
		visit( curState, jointAction );
		
		
		/**
		 * learning rule:
		 * Q(s,a) <- (1-alpha)Q(s,a) + alpha * (reward + gamma * V(s'))
		 */
		double Qsa = getQValue( curState, jointAction );
			
		//no need to query the state if the gameis over!!
		double Vsp = getV( nextState );
		
		//double alpha = getVariableAlpha(curState, jointAction);
		//Qsa = (1 - alpha) * Qsa + alpha * ( rewards[agentIndex] + GAMMA * Vsp );
			
		//reward for this team!!!!
		Qsa = (1 - ALPHA) * Qsa + ALPHA * ( rewards[teamIndex] + GAMMA * Vsp );
			
		setQValue( curState, jointAction, Qsa );
		
		
		/**
		 * whether to update policy
		 */
		//long startTime = System.nanoTime();
		
		boolean bCompute = shouldCompute( curState );
		
		//long endTime = System.nanoTime();
		
		//long duration = (endTime-startTime)/1000000L;
		//System.out.println("Microseconds: "+ duration);
		
		if( bCompute ) {
		    
		    /**
		     * linear programming to update the policy in curState
		     */
		    double minimaxV = updatePolicy( curState );
		    setV( curState,  minimaxV );
		}
		else {
		    
		    double minimaxV = getPolicyMinValue( curState, pi.get( curState ), false, -1 );
		    setV( curState,  minimaxV );
		}
		
		ALPHA *= alphaDecay;
	    }
	    
	    return nextAction;
	}
    }
    
    //check again
    private boolean shouldCompute( GameState gameState )
    {
	if( gameState == null ) {
	    
	    System.out.println("FFQTrans->shouldCompute: Parameter error");
	    return true;
	}
	if( !pi.containsKey( gameState ) ) {
	    
	    System.out.println("FFQTrans->shouldCompute: No such game state in pi");
	    return true;
	}
	
	/**
	 * the last policy
	 */
	double[] lastPolicy = pi.get( gameState ); 
	double lastPolicyValue = getPolicyMinValue(gameState, lastPolicy, false, -1);
	
	/**
	 * check the last policy
	 */
	double max_error = Double.NEGATIVE_INFINITY;
	ArrayList<TeamAction> teamActions = GamblingGame.allTeamActions;
	int teamActNum = teamActions.size();
	
	for( int teamActIndex = 0; teamActIndex < teamActNum; teamActIndex++ ) {
	    
	    
	    double[] purePolicy = new double[teamActNum];
	    purePolicy[teamActIndex] = 1.0;
	    
	    for( int teamActIndex_p = 0; teamActIndex_p < teamActNum; teamActIndex_p++ ) {
		
		if( teamActIndex_p != teamActIndex )
		    purePolicy[teamActIndex_p] = 0.0;
	    }
	    
	    
	    double minValue = getPolicyMinValue(gameState, purePolicy, true, teamActIndex);
	    
	    double error = minValue - lastPolicyValue;
	    if( error > max_error ) 
		max_error = error;
	}
	
	if( max_error <= tau ) {
	  
	    //System.out.println("Alpha: "+ ALPHA);
	    return false;
	}
	else {
	    
	    //System.out.println("No Transfer!");
	    return true; 
	}
    }

    //check again
    private double getPolicyMinValue( GameState gameState, double[] policy, boolean isPure, 
	    int supportIndex )
    {
	if( gameState == null || policy == null ) {
	    
	    System.out.println("FFQTrans->getPolicyValue: Parameter error");
	    return 0;
	}
	
	ArrayList<TeamAction> teamActions = GamblingGame.allTeamActions;
	int teamActNum = teamActions.size();
	
	if( policy.length != teamActNum ) {
	    
	    System.out.println("FFQTrans->getPolicyMinValue: policy length wrong");
	    return 0;
	}
	
	int oppTeamIndex = (teamIndex+1) % GamblingGame.NUM_TEAMS;
	ArrayList<TeamAction> oppTeamActions = GamblingGame.allTeamActions;
	int oppTeamActNum = oppTeamActions.size();
	double minValue = Double.POSITIVE_INFINITY;
	
	//opponent action loop
	for( int oppTeamActIndex = 0; oppTeamActIndex < oppTeamActNum; oppTeamActIndex++ ) {
	    
	    TeamAction oppTeamAction = oppTeamActions.get( oppTeamActIndex );
	    GameAction gameAction = new GameAction();
	    for( int oppNoInTeam = 0; oppNoInTeam < GamblingGame.NUM_TEAM_AGENTS; oppNoInTeam++ ) {
		
		int oppIndex = oppTeamIndex * GamblingGame.NUM_TEAM_AGENTS + oppNoInTeam;
		gameAction.setAction( oppIndex, oppTeamAction.getAction(oppNoInTeam) );
	    }
	    double value = 0.0;
	    
	    //this team action loop
	    if( isPure ) {
		
		TeamAction teamAction = teamActions.get( supportIndex );
		for( int agentNoInTeam = 0; agentNoInTeam < GamblingGame.NUM_TEAM_AGENTS; agentNoInTeam++ ) {
			
		    int agentIndex = teamIndex * GamblingGame.NUM_TEAM_AGENTS + agentNoInTeam;
		    gameAction.setAction( agentIndex, teamAction.getAction(agentNoInTeam) );
		}
		value += getQValue(gameState, gameAction);
	    }
	    else {
		
		for( int teamActIndex = 0; teamActIndex < teamActNum; teamActIndex++ ) {
			
		    TeamAction teamAction = teamActions.get( teamActIndex );
		    for( int agentNoInTeam = 0; agentNoInTeam < GamblingGame.NUM_TEAM_AGENTS; agentNoInTeam++ ) {
				
			int agentIndex = teamIndex * GamblingGame.NUM_TEAM_AGENTS + agentNoInTeam;
			gameAction.setAction( agentIndex, teamAction.getAction(agentNoInTeam) );
		    }
			
		    value += getQValue(gameState, gameAction) * policy[teamActIndex];
		}
	    }

	    
	    if( value < minValue ) {
		
		minValue = value;
	    }  
	}
	
	return minValue;
    }
    
}
