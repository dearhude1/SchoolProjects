package algorithms;

import gameGridWorld.GameAction;
import gameGridWorld.GridWorld;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class QLearning
{

    /**
     * fundamental parameter of Q-learning algorithm
     */
    //we are now conducting preliminary experiments
    protected double ALPHA = 0.2;
    protected double GAMMA = 0.9;//0.7;
    protected double EPSILON = 0.02;//0.02;
    
    
    /**
     * for random use
     */
    protected Random random;
    
    /**
     * Q-table of the corresponding agent
     * 1 dimensions for local state
     * 1 dimensions for local action
     */
    protected double[][] Qs;
    
    
    /**
     * visit number of each state-action pair
     */
    protected double[][] vstNum;
    
    /**
     * an algorithm can also represent an agent
     * agent index begins with 0
     */
    protected int agentIndex;
    
    public QLearning( int index )
    {
	agentIndex = index;
	random = new Random();
	
	/**
	 * init the Q-table
	 */
	int locNum = GridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	Qs = new double[locNum][actionNum];
	vstNum = new double[locNum][actionNum];
	
	for( int s = 0; s < locNum; s++ )
	    for( int a = 0; a < actionNum; a++ ) {
				    
		Qs[s][a] = 0.0;    
		vstNum[s][a] = 0.0;
	    }
    }
    
    public QLearning( int index, double alpha, double gamma, double epsilon )
    {
	agentIndex = index;
	random = new Random();
	
	ALPHA = alpha;
	GAMMA = gamma;
	EPSILON = epsilon;
	
	/**
	 * init the Q-table
	 */
	int locNum = GridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	Qs = new double[locNum][actionNum];
	vstNum = new double[locNum][actionNum];
	
	for( int s = 0; s < locNum; s++ )
	    for( int a = 0; a < actionNum; a++ ) {
				    
		Qs[s][a] = 0.0;    
		vstNum[s][a] = 0.0;
	    }
    }
    
    
    /**
     * epsilon-greedy for one agent
     * @param action
     * @return
     */
    public int epsilonGreedy( int max_action )
    {
	if( random.nextDouble() < EPSILON ) 
	    return random.nextInt(GameAction.NUM_ACTIONS);
	else
	    return max_action;
    }
    
    /**
     * @return: return the next action
     */
    public int updateQ( int locState, int locAction, 
	    double reward, int  nextLocState )
    {
	
	if( nextLocState < 0 || 
		nextLocState >= GridWorld.NUM_LOCATIONS ) {
	    
	    System.out.println("@QLearning->updateQ: Wrong nextState!");
	    
	    return -1;
	}	
	else {
	 
	    /**
	     * first get the next max action according to the Q-function
	     */
	    int nextAction = getMaxAction( nextLocState );
	    
	    /**
	     * if it is not the initial state, 
	     * then update the Q-function
	     */
	    if( locState != -1 ) {
		
		/**
		 * make a visit?
		 */
		
		
		double Qsa = getQValue( locState, locAction );
		double maxQp = getQValue( nextLocState, nextAction );
		Qsa = (1 - ALPHA) * Qsa + ALPHA * (reward + GAMMA * maxQp);
		
		setQValue( locState, locAction, Qsa );
	    }
	    
	    return nextAction;
	}

    }
    
    private int getMaxAction( int locState ) {
	
	if( locState < 0 || 
		locState >= GridWorld.NUM_LOCATIONS ) {
	    
	    System.out.println("@QLearning->getMaxAction: Wrong Parameters!");
	    return -1;
	}
	
	double maxQ = Double.NEGATIVE_INFINITY;
	int maxAction = 0;
	
	for( int action = 0; action < GameAction.NUM_ACTIONS; action++ ) {
	    
	    if( Qs[locState][action] > maxQ ) {
		
		maxQ = Qs[locState][action];
		maxAction = action;
	    }
	}
	
	return maxAction;
    }
    
    protected double getQValue( int locState, int locAction )
    {
	if( locState < 0 || locState >= GridWorld.NUM_LOCATIONS ||
		locAction < 0 || locAction >= GameAction.NUM_ACTIONS ) {
	    
	    System.out.println("@QLearning->getQValue: Wrong Parameters!");
	    return 0.0;
	}
	
	return Qs[locState][locAction];
    }
    
    protected void setQValue( int locState, int locAction, double value )
    {
	if( locState < 0 || locState >= GridWorld.NUM_LOCATIONS ||
		locAction < 0 || locAction >= GameAction.NUM_ACTIONS ) {
	    
	    System.out.println("@QLearning->setQValue: Wrong Parameters!");
	    return;
	}
	
	Qs[locState][locAction] = value;
    }
    
    public int getAgentIndex() 
    {
	return agentIndex;
    }
    

    
    //just for data
    public void gameStarted( int loop )
    {
	
    }
    
    public void gameFinished( int loop ) 
    {
	
	/**
	 * store the Q-table
	 */

	try {
	    
	    BufferedWriter qWriter = new BufferedWriter(new FileWriter("./Qs" +"_agent" + agentIndex + ".txt"));
		
	    for( int locState = 0; locState < GridWorld.NUM_LOCATIONS; locState++ ) 
		for( int locAct = 0; locAct < GameAction.NUM_ACTIONS; locAct++ ) {
			
		    double Qvalue = getQValue( locState, locAct );
		    qWriter.write( String.valueOf(Qvalue) );
		    qWriter.newLine();
		}
		qWriter.close();	    
	    
	}
	catch(IOException ioe) {
	    
	    ioe.printStackTrace();
	}
    }
}
