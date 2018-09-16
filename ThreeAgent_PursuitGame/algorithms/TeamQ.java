package algorithms;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import gamePursuitGame.GameAction;
import gamePursuitGame.GameState;
import gamePursuitGame.PursuitGame;


public class TeamQ extends MARL
{

    /**
     * Q-table of teamQ agent
     * only for this agent
     * Q(s,a)
     * 
     * 3 dimensions for state
     * 2 dimensions for action
     */
    private double[][][][][] teamQs;
    
    
    public TeamQ( int agIndex )
    {
	super( agIndex );
	
	/**
	 * init the Q-table
	 * for centralized algorithms 
	 * the Q-tables can be initialized randomly
	 */
	int locNum = PursuitGame.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	
	/**
	 * init Q-tables again
	 */
	teamQs = new double[locNum][locNum][locNum][actionNum][actionNum];
	for( int s1 = 0; s1 < locNum; s1++ )
	    for( int s2 = 0; s2 < locNum; s2++ )
		for( int s3 = 0; s3 < locNum; s3++ )
		    for( int a1 = 0; a1 < actionNum; a1++ )
			for( int a2 = 0; a2 < actionNum; a2++ ) {
				
			    teamQs[s1][s2][s3][a1][a2] = 0;//(Math.random() - 0.5) / 10.0;
			}
	
	
    }
    
    
    public TeamQ( int agIndex, double alpha, double gamma, double epsilon )
    {
	super( agIndex, alpha, gamma, epsilon );
	
	/**
	 * init the Q-table
	 * for centralized algorithms 
	 * the Q-tables can be initialized randomly
	 */
	int locNum = PursuitGame.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	
	/**
	 * init Q-tables again
	 */
	teamQs = new double[locNum][locNum][locNum][actionNum][actionNum];
	for( int s1 = 0; s1 < locNum; s1++ )
	    for( int s2 = 0; s2 < locNum; s2++ )
		for( int s3 = 0; s3 < locNum; s3++ )
		    for( int a1 = 0; a1 < actionNum; a1++ )
			for( int a2 = 0; a2 < actionNum; a2++ ) {
				
			    teamQs[s1][s2][s3][a1][a2] = 0;//(Math.random() - 0.5) / 10.0;
			}
    }
    
    
    /**
     * Compute the action for the current state
     * this is also a core method for MARL algorithm in Pursuit Game
     * @return
     */
    public GameAction getAction( GameState gameState )
    {
	/**
	 * return the action with the maximal value
	 */
	GameAction maxAction = null;
	double maxValue = -Double.MAX_VALUE;
	for( int a1 = 0; a1 < GameAction.NUM_ACTIONS; a1++ )
	    for( int a2 = 0; a2 < GameAction.NUM_ACTIONS; a2++ ) {
		
		GameAction jntAction = new GameAction(new int[]{0,a1,a2});
		//agentIndex is not useful here, only for the need of parameter
		double value = getQValue(agentIndex, gameState, jntAction);
		
		if( value > maxValue ) {
		    
		    maxValue = value;
		    maxAction = jntAction;
		}
		else
		    jntAction = null;
	    }
	
	return maxAction;
    }
    
    
    /**
     * Core method for MARL algorithms
     * update the Q-table and return the action for the next state
     * @param curState: the current state
     * @param jointAction: the joint action taken in the current state
     * @param rewards: the reward obtained from the current state to the next state
     * @param nextState: the next state
     * @return GameAction: the action expected to be chosen in the next state
     */
    public GameAction updateQ( GameState curState, GameAction jointAction, 
	    double[] rewards, GameState nextState )
    {
	
	if( nextState == null ) {
	    
	    System.out.println("@TeamQ->updateQ: NULL nextState!");
	    
	    return null;
	}
	else {
	    
	    
	    /**
	     * get a joint action according to the correlated equilibrium
	     */
	    GameAction nextAction = getAction( nextState );
	    
	    
	    /**
	     * update the Q-tables
	     * but if this is the initial state of the game
	     * just return the action
	     */
	    if( curState != null && jointAction != null 
		&& rewards != null ) {
		
		/**
		 * mark a visit
		 */
		visit( curState, jointAction );
		
		/**
		 * learning rule:
		 * Q(s,a) <- (1-alpha)Q(s,a) + alpha * (reward + gamma * V(s'))
		 */
		double Qsa = getQValue( agentIndex, curState, jointAction );
		
		//Val = max Q(s,a1,a2)
		double Val = getQValue( agentIndex, nextState, nextAction );
		
		Qsa = (1 - ALPHA) * Qsa + ALPHA * ( rewards[agentIndex] + GAMMA * Val );
		setQValue( agentIndex, curState, jointAction, Qsa );
		
	    }
	    
	    
	    return nextAction;
	}
    }
    
    
    protected double getQValue( int agent, GameState gameState, 
	    GameAction gameAction )
    {
	if( gameAction == null || 
		gameState == null ) {
	    
	    System.out.println("@CenCEQ->getQValue: Wrong Parameters!");
	    return 0.0;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int loc2 = gameState.getLocationID(2);
	int a1 = gameAction.getAction(1);
	int a2 = gameAction.getAction(2);
	
	return teamQs[loc0][loc1][loc2][a1][a2];
    }
    
    protected void setQValue( int agent, GameState gameState, 
	    GameAction gameAction, double value )
    {
	if( gameAction == null || 
		gameState == null ) {
	    
	    System.out.println("@CenCEQ->setQValue: Wrong Parameters!");
	    return;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int loc2 = gameState.getLocationID(2);
	int a1 = gameAction.getAction(1);
	int a2 = gameAction.getAction(2);
	
	teamQs[loc0][loc1][loc2][a1][a2] = value;
    }
    
    /**
     * store the Q-values learnt
     */
    public void storeQValues( String algName )
    {
	try {
	    
	    /**
	     * write all values in member Qs
	     * 
	     * only the table of this agent
	     */
	    BufferedWriter writer = new BufferedWriter(new FileWriter("./"+algName+"_values_agent"+agentIndex+".txt"));
	    int locNum = PursuitGame.NUM_LOCATIONS;
	    int actionNum = GameAction.NUM_ACTIONS;
	    for( int s1 = 0; s1 < locNum; s1++ )
		for( int s2 = 0; s2 < locNum; s2++ )
		    for( int s3 = 0; s3 < locNum; s3++ )
			for( int a1 = 0; a1 < actionNum; a1++ )
			    for( int a2 = 0; a2 < actionNum; a2++ ) {
					    
			    	    writer.write(""+teamQs[s1][s2][s3][a1][a2]);
			    	    writer.newLine();
			    	}
	    writer.close();
	}
	catch (Exception e) {
	    e.printStackTrace();
	}
    }
    
    /**
     * read Q-values of a specific agent from file
     */
    public void readQValues( String algName, int agent )
    {
	if( agent == PursuitGame.PREY_AGENT_INDEX )
	    return;
	
	try {
	    
	    /**
	     * write all values in member Qs
	     */
	    BufferedReader reader = new BufferedReader(new FileReader("./"+algName+"_values_agent"+agentIndex+".txt"));
	    int locNum = PursuitGame.NUM_LOCATIONS;
	    int actionNum = GameAction.NUM_ACTIONS;
	    
	    String line = "";
	    int s1 = 0;
	    int s2 = 0;
	    int s3 = 0;
	    int a1 = 0;
	    int a2 = 0;
	    
	    while( (line = reader.readLine()) != null ) {
		
		if( line.length() == 0 )
		    continue;
		
		teamQs[s1][s2][s3][a1][a2] = Double.parseDouble( line );
		
		a2++;
		if( a2 >= actionNum ) {
			    
		    a2 = 0;
		    a1++;
		    if( a1 >= actionNum ) {
				
			a1 = 0;
			s3++;
			if( s3 >= locNum ) {
				
			    s3 = 0;
			    s2++;
			    if( s2 >= locNum ) {
					    
				s2 = 0;
				s1++;
			    }	
			}
		    }
		}
	    }
	    reader.close();
	}
	catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
