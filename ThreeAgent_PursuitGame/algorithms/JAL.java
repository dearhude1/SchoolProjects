package algorithms;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import gamePursuitGame.GameAction;
import gamePursuitGame.GameState;
import gamePursuitGame.PursuitGame;

public class JAL extends MARL
{
    /**
     * Q-table for JAL
     * 3 dimensions for state
     * 2 dimensions for joint action
     */
    private double[][][][][] jalQs;
    
    /**
     * opponent model of the other agents' strategy
     * 
     * including prey action and another hunter's action
     * 
     * dimension: 
     * 1 for agent index
     * 3 for state
     * 1 for agent action
     */
    private double[][][][][] oppModel;
    
    
    private double decay = 0.9999;
    
    public JAL( int agIndex )
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
	jalQs = new double[locNum][locNum][locNum][actionNum][actionNum];
	oppModel = new double[PursuitGame.NUM_AGENTS-1][locNum][locNum][locNum][actionNum];
	for( int s1 = 0; s1 < locNum; s1++ )
	    for( int s2 = 0; s2 < locNum; s2++ )
		for( int s3 = 0; s3 < locNum; s3++ )
		    for( int a1 = 0; a1 < actionNum; a1++ ) {
			
			for( int agent = 0; agent < PursuitGame.NUM_AGENTS-1; agent++ )
			    oppModel[agent][s1][s2][s3][a1] = 0.0;
			
			for( int a2 = 0; a2 < actionNum; a2++ ) {
			
			    jalQs[s1][s2][s3][a1][a2] = 0;//(Math.random() - 0.5) / 10.0;
			    
			}
		    }
	
	ALPHA = 1.0;
    }
    
    
    public JAL( int agIndex, double alpha, double gamma, double epsilon )
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
	jalQs = new double[locNum][locNum][locNum][actionNum][actionNum];
	oppModel = new double[PursuitGame.NUM_AGENTS-1][locNum][locNum][locNum][actionNum];
	for( int s1 = 0; s1 < locNum; s1++ )
	    for( int s2 = 0; s2 < locNum; s2++ )
		for( int s3 = 0; s3 < locNum; s3++ )
		    for( int a1 = 0; a1 < actionNum; a1++ ) {
			
			for( int agent = 0; agent < PursuitGame.NUM_AGENTS-1; agent++ )
			    oppModel[agent][s1][s2][s3][a1] = 0.0;
			
			for( int a2 = 0; a2 < actionNum; a2++ ) {
			
			    jalQs[s1][s2][s3][a1][a2] = 0;//(Math.random() - 0.5) / 10.0;
			    
			}
		    }
	
	ALPHA = 1.0;
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
	int maxIndex = 0;
	double maxValue = -Double.MAX_VALUE;
	for( int a_i = 0; a_i < GameAction.NUM_ACTIONS; a_i++ ) {
	    
	    double value = getExpValue(gameState, a_i);
	    
	    if( value > maxValue ) {
		    
		maxValue = value;
		maxIndex = a_i;
	    }
	}
	
	//we only care about this agent's part
	GameAction maxAction = new GameAction(new int[]{0,maxIndex,maxIndex});
	
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
		double expVal = getExpValue(nextState, nextAction.getAction(agentIndex));
		
		
		//Qsa = (1 - ALPHA) * Qsa + ALPHA * ( rewards[agentIndex] + GAMMA * expVal );
		
		//variant learning rate
		double alpha = getVariableAlpha(curState, jointAction);
		Qsa = (1 - alpha) * Qsa + alpha * ( rewards[agentIndex] + GAMMA * expVal );
		
		setQValue( agentIndex, curState, jointAction, Qsa );
		
		
		ALPHA *= decay;
	    }
	    
	    return nextAction;
	}
    }
    
    
    private double getOppActionPro( GameState gameState, int agent_j, int a_j )
    {
	if( gameState == null ) {
	    
	    return 0.0;
	}
	if( a_j < 0 || a_j >= GameAction.NUM_ACTIONS ) {
	    
	    return 0.0;
	}
	if( agent_j == agentIndex ) {
	    
	    return 0.0;
	}
	
	if( agent_j == 2 )
	    agent_j -= 1;
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int loc2 = gameState.getLocationID(2);
	
	double allTimes = 0.0;
	for( int act_j = 0; act_j < PursuitGame.NUM_AGENTS; act_j++ ) {
	    
	    allTimes += oppModel[agent_j][loc0][loc1][loc2][act_j];
	}
	
	if( allTimes <= 0.0 )
	    return 0.0;
	
	return oppModel[agent_j][loc0][loc1][loc2][a_j] / allTimes;
    }
    
    private double getExpValue( GameState gameState, int act_i )
    {
	
	if( gameState == null ) {
	    
	    return 0.0;
	}
	if( act_i < 0 || act_i >= GameAction.NUM_ACTIONS ) {
	    
	    return 0.0;
	}
	
	double value = 0;
	
	int agent_j = 1;
	if( agentIndex == 1 )
	    agent_j = 2;
	
	for( int preyAction = 0; preyAction < GameAction.NUM_ACTIONS; preyAction++ ) {
	    
	    double oppActionPro_Prey = getOppActionPro(gameState, PursuitGame.PREY_AGENT_INDEX, preyAction);
	    
	    for( int a_j = 0; a_j < GameAction.NUM_ACTIONS; a_j++ ) {
		
		GameAction jntAction = new GameAction();
		jntAction.setAction(PursuitGame.PREY_AGENT_INDEX, preyAction);
		jntAction.setAction(agentIndex, act_i);
		jntAction.setAction(agent_j, a_j);
		
		//agentIndex is not useful here, only for the need of parameter
		double qVal = getQValue(agentIndex, gameState, jntAction);
		double oppActionPro_j = getOppActionPro(gameState, agent_j, a_j);
		value += qVal * oppActionPro_Prey * oppActionPro_j;
	    }
	}
	
	return value;
    }
    
    protected void visit( GameState curState, GameAction curAction )
    {
	if( curState == null || curAction == null ) {
	    
	    System.out.println("@JAL->visit: Wrong Parameters!");
	    return;
	}
	
	int loc0 = curState.getLocationID(0);
	int loc1 = curState.getLocationID(1);
	int loc2 = curState.getLocationID(2);
	int a0 = curAction.getAction(0);
	int a1 = curAction.getAction(1);
	int a2 = curAction.getAction(2);
	
	vstNum[loc0][loc1][loc2][a0][a1][a2] += 1.0;
	
	oppModel[0][loc0][loc1][loc2][a0] += 1.0;
	if( agentIndex == 1 )
	    oppModel[1][loc0][loc1][loc2][a2] += 1.0;
	else
	    oppModel[1][loc0][loc1][loc2][a1] += 1.0;
    }
    
    protected double getQValue( int agent, GameState gameState, 
	    GameAction gameAction )
    {
	if( gameAction == null || 
		gameState == null ) {
	    
	    System.out.println("@JAL->getQValue: Wrong Parameters!");
	    return 0.0;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int loc2 = gameState.getLocationID(2);
	int a1 = gameAction.getAction(1);
	int a2 = gameAction.getAction(2);
	
	return jalQs[loc0][loc1][loc2][a1][a2];
    }
    
    protected void setQValue( int agent, GameState gameState, 
	    GameAction gameAction, double value )
    {
	if( gameAction == null || 
		gameState == null ) {
	    
	    System.out.println("@JAL->setQValue: Wrong Parameters!");
	    return;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int loc2 = gameState.getLocationID(2);
	int a1 = gameAction.getAction(1);
	int a2 = gameAction.getAction(2);
	
	jalQs[loc0][loc1][loc2][a1][a2] = value;
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
					    
			    	    writer.write(""+jalQs[s1][s2][s3][a1][a2]);
			    	    writer.newLine();
			    	}
	    writer.close();
	    
	    
	    /**
	     * write opponent model
	     */
	    writer = new BufferedWriter(new FileWriter("./"+algName+"_oppModel_agent"+agentIndex+".txt"));
	    for( int agIndex = 0; agIndex < PursuitGame.NUM_AGENTS-1; agIndex++ )
		for( int s1 = 0; s1 < locNum; s1++ )
		    for( int s2 = 0; s2 < locNum; s2++ )
			for( int s3 = 0; s3 < locNum; s3++ )
			    for( int a = 0; a < actionNum; a++ ) {
					    
				writer.write(""+oppModel[agIndex][s1][s2][s3][a]);
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
	     * read all values in member Qs
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
		
		jalQs[s1][s2][s3][a1][a2] = Double.parseDouble( line );
		
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
	    
	    /**
	     * read opponent model
	     */
	    int agIndex = 0;
	    s1 = 0;
	    s2 = 0;
	    s3 = 0;
	    a1 = 0;
	    reader = new BufferedReader(new FileReader("./"+algName+"_oppModel_agent"+agentIndex+".txt"));
	    while( (line = reader.readLine()) != null ) {
		
		if( line.length() == 0 )
		    continue;
		
		oppModel[agIndex][s1][s2][s3][a1] = Double.parseDouble( line );
		
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
			    if( s1 >= locNum ) {
				
				s1 = 0;
				agIndex++;
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
