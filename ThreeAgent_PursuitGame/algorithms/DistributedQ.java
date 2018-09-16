package algorithms;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import gamePursuitGame.GameAction;
import gamePursuitGame.GameState;
import gamePursuitGame.PursuitGame;

/**
 * distributedQ for deterministic MAMDPs
 * 
 * deterministic version is enough for 
 * solving Pursuit Games with random prey
 *
 */
public class DistributedQ extends MARL
{
    /**
     * the q-table of distributedQ
     * 3 dimensions for state
     * 1 dimension for action of this agent
     */
    private double[][][][] q_table;
    
    /**
     * the policy of this agent
     * it is a deterministic policy
     */
    private int[][][] pi;
    
    public DistributedQ( int agIndex )
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
	q_table = new double[locNum][locNum][locNum][actionNum];
	pi = new int[locNum][locNum][locNum];
	for( int s1 = 0; s1 < locNum; s1++ )
	    for( int s2 = 0; s2 < locNum; s2++ )
		for( int s3 = 0; s3 < locNum; s3++ ) {
		    
		    pi[s1][s2][s3] = random.nextInt(GameAction.NUM_ACTIONS);
		    
		    for( int a = 0; a < actionNum; a++ ) {
				
			    q_table[s1][s2][s3][a] = 0;//(Math.random() - 0.5) / 10.0;
			}
		}
	
	
    }
    
    
    public DistributedQ( int agIndex, double alpha, double gamma, double epsilon )
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
	q_table = new double[locNum][locNum][locNum][actionNum];
	pi = new int[locNum][locNum][locNum];
	for( int s1 = 0; s1 < locNum; s1++ )
	    for( int s2 = 0; s2 < locNum; s2++ )
		for( int s3 = 0; s3 < locNum; s3++ ) {
		    
		    pi[s1][s2][s3] = random.nextInt(GameAction.NUM_ACTIONS);
		    
		    for( int a = 0; a < actionNum; a++ ) {
				
			    q_table[s1][s2][s3][a] = 0;//(Math.random() - 0.5) / 10.0;
			}
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
	 * return the action according to pi
	 */
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int loc2 = gameState.getLocationID(2);
	int act_i = pi[loc0][loc1][loc2];
	
	/**
	 * we only care about the action of this agent
	 */
	return new GameAction(new int[]{act_i,act_i,act_i});
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
		double q_sa = getQValue( agentIndex, curState, jointAction );
		
		double max_qsa = getMaxQValue( curState );
		double max_qspap = getMaxQValue( nextState );
		
		double backup_qsa = rewards[agentIndex] + GAMMA * max_qspap;
		
		/**
		 * update the q-table if there is an increase of the value
		 * 
		 * q_{t+1}(s,a) = max( q_t(s,a), r + gamma*max q_t(s',a') )
		 */
		if( q_sa < backup_qsa )
		    setQValue( agentIndex, curState, jointAction, backup_qsa );
		
		
		/**
		 * update policy in curState
		 * 
		 * pi remains the same if max q_t(s,a) = max q_{t+1}(s,a)
		 */
		double max_qsa_t1 = getMaxQValue( curState );
		if(  Math.abs(max_qsa-max_qsa_t1) >= 0.0001 ) 
		    setPi( curState, jointAction.getAction(agentIndex) );
		
	    }
	    
	    return nextAction;
	}
    }
    
    
    private double getMaxQValue( GameState gameState )
    {
	if( gameState == null ) {
	    
	    System.out.println("@DistributedQ->getMaxQValue: NULL Parameter!");
	    return 0.0;
	}
	
	double maxValue = -Double.MAX_VALUE;
	for( int a = 0; a < GameAction.NUM_ACTIONS; a++ ) {
	    
	    GameAction jntAction = new GameAction(new int[]{a,a,a});
	    double value = getQValue(agentIndex, gameState, jntAction);
	    
	    if( value > maxValue )
		maxValue = value;
	    
	    jntAction = null;
	}
	
	return maxValue;
    }
    
    protected double getQValue( int agent, GameState gameState, 
	    GameAction gameAction )
    {
	if( gameAction == null || 
		gameState == null ) {
	    
	    System.out.println("@DistributedQ->getQValue: Wrong Parameters!");
	    return 0.0;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int loc2 = gameState.getLocationID(2);
	int a = gameAction.getAction(agentIndex);
	
	return q_table[loc0][loc1][loc2][a];
    }
    
    protected void setQValue( int agent, GameState gameState, 
	    GameAction gameAction, double value )
    {
	if( gameAction == null || 
		gameState == null ) {
	    
	    System.out.println("@DistributedQ->setQValue: Wrong Parameters!");
	    return;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int loc2 = gameState.getLocationID(2);
	int a = gameAction.getAction(agentIndex);
	
	q_table[loc0][loc1][loc2][a] = value;
    }

    private void setPi( GameState gameState, int action )
    {
	if( gameState == null ) {
	 
	    System.out.println("@DistributedQ->setPi: NULL Parameter!");
	    return;
	}
	if( action < 0 || action >= GameAction.NUM_ACTIONS ) {
	    
	    System.out.println("@DistributedQ->setPi: Wrong Action!");
	    return;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int loc2 = gameState.getLocationID(2);
	
	pi[loc0][loc1][loc2] = action;
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
			for( int a = 0; a < actionNum; a++ ) {
					    
			    	    writer.write(""+q_table[s1][s2][s3][a]);
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
	    int a = 0;
	    
	    while( (line = reader.readLine()) != null ) {
		
		if( line.length() == 0 )
		    continue;
		
		q_table[s1][s2][s3][a] = Double.parseDouble( line );
		
		a++;
		if( a >= actionNum ) {
				
		    a = 0;
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
	    reader.close();
	}
	catch (Exception e) {
	    e.printStackTrace();
	}
    }
    
    public void storePolicy()
    {
	
	try {
	    
	    /**
	     * write all values in member Qs
	     * 
	     * only the table of this agent
	     */
	    String fileName = "./disQPolicy_agent"+agentIndex+".txt";
	    BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
	    int locNum = PursuitGame.NUM_LOCATIONS;
	    
	    for( int s1 = 0; s1 < locNum; s1++ )
		for( int s2 = 0; s2 < locNum; s2++ )
		    for( int s3 = 0; s3 < locNum; s3++ ) {
					    
			    writer.write(""+pi[s1][s2][s3]);
			    writer.newLine();
			}
	    writer.close();
	}
	catch (Exception e) {
	    e.printStackTrace();
	}
    }
    
    public void readPolicy()
    {
	
	try {
	    
	    String fileName = "./disQPolicy_agent"+agentIndex+".txt";
	    BufferedReader reader = new BufferedReader(new FileReader(fileName));
	    int locNum = PursuitGame.NUM_LOCATIONS;
	    
	    String line = "";
	    int s1 = 0;
	    int s2 = 0;
	    int s3 = 0;
	    
	    while( (line = reader.readLine()) != null ) {
		
		if( line.length() == 0 )
		    continue;
		
		pi[s1][s2][s3] = Integer.parseInt( line );
		
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
	    reader.close();
	}
	catch (Exception e) {
	    e.printStackTrace();
	}
    }
    
}
