package algorithms.sarl;

import gameGridWorld.GameAction;
import gameGridWorld.SparseGridWorld;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class Rmax
{
    
    /**
     * fundamental parameter of Q-learning algorithm
     */
    //we are now conducting preliminary experiments
    protected double ALPHA = 0.99;//0.2;
    protected double GAMMA = 0.9;//0.9;//0.7;
    
    //not exploration factor, but error bound
    protected double EPSILON = 0.05;
    
    
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
     * the model of the reward function
     * 
     * note that this member stores the reward sum!!!
     */
    protected double[][] Rmodel;
    
    /**
     * visit number of each state-action pair
     */
    protected int[][] Nsa;
    
    /**
     * the times from s to s' by action a
     * for computing Tmodel
     */
    protected int[][][] Nsas;
    
    /**
     * the model of the transition function
     */
    //protected double[][][] Tmodel;
    
    /**
     * learning paramter
     */
    //private double epsilon1 = 0.0;
    
    private double delta = 0.1;
    
    private int m = 0;
    
    /**
     * the rounds for value iteration 
     * when a state becomes KNOWN
     */
    private int iterationRound = 0;
    
    private int maxR = 0;
    
    
    /**
     * an algorithm can also represent an agent
     * agent index begins with 0
     */
    protected int agentIndex;
    
    public Rmax( int index, int maxRwd )
    {
	agentIndex = index;
	random = new Random();
	
	maxR = maxRwd;
	
	/**
	 * init the Q-table
	 */
	int locNum = SparseGridWorld.NUM_CELLS;
	int actionNum = GameAction.NUM_ACTIONS;
	Qs = new double[locNum][actionNum];
	Nsa = new int[locNum][actionNum];
	Rmodel = new double[locNum][actionNum];
	//Tmodel = new double[locNum][actionNum][locNum];
	Nsas = new int[locNum][actionNum][locNum];
	
	for( int s = 0; s < locNum; s++ )
	    for( int a = 0; a < actionNum; a++ ) {
				    
		Qs[s][a] = 20;//200;//maxRwd / (1-GAMMA);    
		Nsa[s][a] = 0;
		Rmodel[s][a] = 0.0;
		
		for( int sp = 0; sp < locNum; sp++ ) {
		    
		    //Tmodel[s][a][sp] = 0.0;
		    Nsas[s][a][sp] = 0;
		}
	    }
	
	computeIterationRound();
	computeSampleNum();
    }
    
    public Rmax( int index, double alpha, double gamma, double epsilon, 
	    int maxRwd )
    {
	agentIndex = index;
	random = new Random();
	
	ALPHA = alpha;
	GAMMA = gamma;
	EPSILON = epsilon;
	
	maxR = maxRwd;
	
	/**
	 * init the Q-table
	 */
	int locNum = SparseGridWorld.NUM_CELLS;
	int actionNum = GameAction.NUM_ACTIONS;
	Qs = new double[locNum][actionNum];
	Nsa = new int[locNum][actionNum];
	Rmodel = new double[locNum][actionNum];
	//Tmodel = new double[locNum][actionNum][locNum];
	Nsas = new int[locNum][actionNum][locNum];
	
	for( int s = 0; s < locNum; s++ )
	    for( int a = 0; a < actionNum; a++ ) {
				    
		Qs[s][a] = 20;//200;//maxRwd / (1-GAMMA);    
		Nsa[s][a] = 0;
		Rmodel[s][a] = 0.0;
		
		for( int sp = 0; sp < locNum; sp++ ) {
		    
		    //Tmodel[s][a][sp] = 0.0;
		    Nsas[s][a][sp] = 0;
		}
	    }
	
	computeIterationRound();
	computeSampleNum();
    }
    
    
    private void computeIterationRound()
    {
	
	double denominator = 1 - GAMMA;
	
	double numerator = Math.log( 1 / (EPSILON * (1-GAMMA)) );
	
	iterationRound = (int) (numerator / denominator);
    }
    
    
    private void computeSampleNum()
    {
	
	int S = SparseGridWorld.NUM_CELLS; //the number of states
	int A = GameAction.NUM_ACTIONS;
	double Vmax = maxR / (1-GAMMA);
	
	double tempM = 0;
	tempM = S + Math.log(S*A/delta);
	
	tempM *= Vmax * Vmax;
	tempM /= EPSILON * EPSILON;
	tempM /= (1-GAMMA) * (1-GAMMA);
	
	//tempM *= 5;//a constant???
	System.out.println("TempM: "+tempM);
	m = (int) (tempM / 10000000000L);
	
	
	System.out.println("Sample Number: "+m);
    }
    
    /**
     * @return: return the next action
     */
    public int updateQ( int locState, int locAction, 
	    double reward, int  nextLocState )
    {
	
	if( nextLocState < 0 || 
		nextLocState >= SparseGridWorld.NUM_CELLS ) {
	    
	    System.out.println("@Rmax->updateQ: Wrong nextState!");
	    
	    return -1;
	}	
	else {
	 
	    /**
	     * whether choose the max action???
	     */
	    int nextAction = getMaxAction( nextLocState );
	    
	    /**
	     * if it is not the initial state, 
	     * then update the Q-function
	     */
	    if( locState != -1 ) {
		
		/**
		 * make a visit
		 */
		if( Nsa[locState][locAction] < m ) {
		    
		    /**
		     * update the statistics
		     */
		    Nsa[locState][locAction] += 1;
		    Rmodel[locState][locAction] += reward;
		    Nsas[locState][locAction][nextLocState] += 1;
		    
		    /**
		     * then if sufficient visits have been made
		     * update the value function
		     */
		    if( Nsa[locState][locAction] == m ) {
			
			for( int round = 1; round <= iterationRound; round++ ) {
			    
			    valueIteration();
			}
		    }
		}
		
	    }
	    
	    return nextAction;
	}

    }
    
    private int getMaxAction( int locState ) {
	
	if( locState < 0 || 
		locState >= SparseGridWorld.NUM_CELLS ) {
	    
	    System.out.println("@Rmax->getMaxAction: Wrong Parameters!");
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
	
	/**
	 * if there are several max actions
	 */
	ArrayList<Integer> maxActionList = new ArrayList<Integer>();
	maxActionList.add( maxAction );
	for( int action = 0; action < GameAction.NUM_ACTIONS; action++ ) {
	    
	    if( action == maxAction ) 
		continue;
	    
	    double qValue = Qs[locState][action];
	    if( Math.abs( qValue - maxQ ) < 0.0001 ) {
		
		maxActionList.add( action );
	    }
	}
	
	int chosenIndex = random.nextInt( maxActionList.size() );
	int retAction = maxActionList.get( chosenIndex );
	
	return retAction;
    }
    
    private double getMaxQvalue( int locState )
    {
	if( locState < 0 || 
		locState >= SparseGridWorld.NUM_CELLS ) {
	    
	    System.out.println("@Rmax->getMaxQvalue: Wrong Parameters!");
	    return -1;
	}
	
	double maxQ = Double.NEGATIVE_INFINITY;
	
	for( int action = 0; action < GameAction.NUM_ACTIONS; action++ ) {
	    
	    if( Qs[locState][action] > maxQ ) {
		
		maxQ = Qs[locState][action];
	    }
	}
	
	return maxQ;
    }
    
    protected double getQValue( int locState, int locAction )
    {
	if( locState < 0 || locState >= SparseGridWorld.NUM_CELLS ||
		locAction < 0 || locAction >= GameAction.NUM_ACTIONS ) {
	    
	    System.out.println("@Rmax->getQValue: Wrong Parameters!");
	    return 0.0;
	}
	
	return Qs[locState][locAction];
    }
    
    protected void setQValue( int locState, int locAction, double value )
    {
	if( locState < 0 || locState >= SparseGridWorld.NUM_CELLS ||
		locAction < 0 || locAction >= GameAction.NUM_ACTIONS ) {
	    
	    System.out.println("@Rmax->setQValue: Wrong Parameters!");
	    return;
	}
	
	Qs[locState][locAction] = value;
    }

    private void valueIteration()
    {
	
	//System.out.println("Value Iteration");
	
	int locNum = SparseGridWorld.NUM_CELLS;
	int actionNum = GameAction.NUM_ACTIONS;
	
	for( int s = 0; s < locNum; s++ )
	    for( int a = 0; a < actionNum; a++ ) {
		
		if( Nsa[s][a] >= m ) {
		    
		    
		    /**
		     * compute the reward
		     */
		    double Qsa = 0;
		    double R = Rmodel[s][a] / Nsa[s][a];
		    
		    Qsa = R;
		    
		    /**
		     * for all next states
		     */
		    for( int sp = 0; sp < locNum; sp++ ) {
			
			/**
			 * compute the transition model
			 */
			double Tsasp = ((double)Nsas[s][a][sp]) / ((double)Nsa[s][a]);
			
			/**
			 * the max Qvalue of the next state
			 */
			double maxQsp = 0.0;
			if( Tsasp > 0.00001 )
			    maxQsp = getMaxQvalue( sp );
			    
			
			Qsa += GAMMA * Tsasp * maxQsp;
		    }
		    
		    setQValue( s, a, Qsa );
		}
	    }
    }
    
    public boolean stopRunning()
    {
	
	return false;
    }
    
    public void gameFinished( int loop ) 
    {
	
	/**
	 * store the Q-table
	 */

	try {
	    
	    BufferedWriter qWriter = new BufferedWriter(new FileWriter("./Rmax" +"_agent" + agentIndex + ".txt"));
		
	    for( int locState = 0; locState < SparseGridWorld.NUM_CELLS; locState++ ) 
		for( int locAct = 0; locAct < GameAction.NUM_ACTIONS; locAct++ ) {
			
		    double Qvalue = getQValue( locState, locAct );
		    qWriter.write( String.valueOf(Qvalue) );
		    qWriter.newLine();
		}
	    qWriter.close();	    
	    
	    /**
	     * write the reward model
	     */
	    BufferedWriter rmWriter = 
		    new BufferedWriter(new FileWriter("./RewardModel"+"_agent"+agentIndex+".txt"));
	    for( int locState = 0; locState < SparseGridWorld.NUM_CELLS; locState++ )
		for( int locAct = 0; locAct < GameAction.NUM_ACTIONS; locAct++ ) {
		    
		    double reward = 0.0;
		    if( Nsa[locState][locAct] > 0 )
			reward = Rmodel[locState][locAct] / Nsa[locState][locAct];
		    
		    rmWriter.write( String.valueOf(reward) );
		    rmWriter.newLine();
		}
	    rmWriter.close();
	    
	    /**
	     * write the transition model
	     */
	    BufferedWriter tmWriter = 
		    new BufferedWriter(new FileWriter("./TransitionModel"+"_agent"+agentIndex+".txt"));
	    for( int s = 0; s < SparseGridWorld.NUM_CELLS; s++ ) 
		for( int a = 0; a < GameAction.NUM_ACTIONS; a++ )
		    for( int sp = 0; sp < SparseGridWorld.NUM_CELLS; sp++ ) {
			
			double transPro = 0.0;
			if( Nsa[s][a] > 0 )
			    transPro = ((double)Nsas[s][a][sp]) / ((double)Nsa[s][a]); // Tmodel[s][a][sp];
			
			tmWriter.write( String.valueOf(transPro) );
			tmWriter.newLine();
		    }
	    tmWriter.close();
	    
	}
	catch(IOException ioe) {
	    
	    ioe.printStackTrace();
	}
	
    }
    
}
