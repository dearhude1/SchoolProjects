package algorithms.sarl;

import gameGridWorld.GameAction;
import gameGridWorld.SparseGridWorld;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Q-learning with N-step Return (NSR) model
 * @author dearhude1
 *
 */
public class QLearningNSR extends QLearning
{

    /**
     * the number of steps to look forward
     */
    public int numNStep = 2;//5;//3;
    
    /**
     * N-step Return data of each single-agent state-action pair
     * 0 dim: local state
     * 1 dim: local action
     * 2 dim: the NSR data point
     */
    //protected double[][][] NSR;
    
    //the NSR model should be defined only in the state space
    protected double[][] dataNSR;
    
    /**
     * visit number of states
     */
    protected double[] stateVstNum;
    
    /**
     * the maximum number of NSR data point of each state-action pair
     */
    public static final int NSR_MAX_DATA_NUM = 500;
    
    /**
     * the number of episodes for learning N-step Return model
     */
    public static final int NUM_EPISODE_NSR_MODEL_LEARNING = 400;
    
    /**
     * the rewards, local states, and local actions
     * sampled during learning,
     * the max length of these lists is N
     */
    ArrayList<Double> sampledRewards;
    ArrayList<Integer> sampledLocalState;
    //ArrayList<Integer> sampledLocalAction;
    
    public ArrayList<Integer> pickedStates; 
    
    public QLearningNSR( int index )
    {
	super(index);
	
	int locNum = SparseGridWorld.NUM_CELLS;
	/**
	int actionNum = GameAction.NUM_ACTIONS;
	NSR = new double[locNum][actionNum][NSR_MAX_DATA_NUM];
	for( int s = 0; s < locNum; s++ )
	    for( int a = 0; a < actionNum; a++ ) 
	    	for( int nsrIndex = 0; nsrIndex < NSR_MAX_DATA_NUM; nsrIndex++ ) {
	    	    
	    	    NSR[s][a][nsrIndex] = 0.0;
	    	}
	*/
	
	dataNSR = new double[locNum][NSR_MAX_DATA_NUM];
	stateVstNum = new double[locNum];
	for( int s = 0; s < locNum; s++ ) {
	  
	    stateVstNum[s] = 0.0;
	    for( int nsrIndex = 0; nsrIndex < NSR_MAX_DATA_NUM; nsrIndex++ ) {
	    	    
		dataNSR[s][nsrIndex] = 0.0;
	    }
	}
	
	sampledRewards = new ArrayList<Double>();
	sampledLocalState = new ArrayList<Integer>();
	//sampledLocalAction = new ArrayList<Integer>();
    }
    
    public QLearningNSR( int index, double alpha, double gamma, double epsilon )
    {
	super(index, alpha, gamma, epsilon);
	
	int locNum = SparseGridWorld.NUM_CELLS;
	
	/**
	int actionNum = GameAction.NUM_ACTIONS;
	NSR = new double[locNum][actionNum][NSR_MAX_DATA_NUM];
	for( int s = 0; s < locNum; s++ )
	    for( int a = 0; a < actionNum; a++ ) 
	    	for( int nsrIndex = 0; nsrIndex < NSR_MAX_DATA_NUM; nsrIndex++ ) {
	    	    
	    	    NSR[s][a][nsrIndex] = 0.0;
	    	}
	*/
	
	dataNSR = new double[locNum][NSR_MAX_DATA_NUM];
	stateVstNum = new double[locNum];
	for( int s = 0; s < locNum; s++ ) {
	  
	    stateVstNum[s] = 0.0;
	    for( int nsrIndex = 0; nsrIndex < NSR_MAX_DATA_NUM; nsrIndex++ ) {
	    	    
		dataNSR[s][nsrIndex] = 0.0;
	    }
	}
	
	sampledRewards = new ArrayList<Double>();
	sampledLocalState = new ArrayList<Integer>();
	//sampledLocalAction = new ArrayList<Integer>();
    }
    
    
    /**
     * @return: return the next action
     */
    public int updateQ( int locState, int locAction, 
	    double reward, int  nextLocState, int curEpi )
    {
	
	if( nextLocState < 0 || 
		nextLocState >= SparseGridWorld.NUM_CELLS ) {
	    
	    System.out.println("@QLearningNSR->updateQ: Wrong nextState!");
	    
	    return -1;
	}	
	else {
	 
	    /**
	     * if we are still in NSR model learning
	     */
	    if( curEpi < NUM_EPISODE_NSR_MODEL_LEARNING ) {
		
		int nextAction = getRandomAction(nextLocState);
		
		
		/**
		 * if it is not the initial state, 
		 * then update the NSR model
		 */
		if( locState != -1 ) {
		    
		    /**
		     * make a visit?
		     */
		    //vstNum[locState][locAction] += 1.0;
		    stateVstNum[locState] += 1.0;
		    
		    if( sampledRewards.size() == numNStep && 
			    sampledLocalState.size() == numNStep ) { //&& sampledLocalAction.size() == numNStep ) {
			
			//find the state-action pair to be updated
			int s0 = sampledLocalState.remove(0);
			int ns0 = (int) stateVstNum[s0];
			//int a0 = sampledLocalAction.remove(0);
			//int ns0a0 = (int) vstNum[s0][a0];
			
			
			if( ns0 < NSR_MAX_DATA_NUM ) {
			
			    double nsr = 0.0;
			    double discount = 1.0;
			    for( int index = 0; index < sampledRewards.size(); index++ ) {
				    
				nsr += sampledRewards.get(index) * discount;
				discount *= GAMMA;
			    }
			    
			    dataNSR[s0][ns0] = nsr;
			}
			else {
			    
			    //System.out.println("("+s0+","+a0+") has been explored for "+NSR_MAX_DATA_NUM+" times");
			}

			//also remove the first element in reward list
			sampledRewards.remove(0);
		    }
		    /**
		     * add the current state-action pair and reward into the sampled lists
		     */
		    sampledRewards.add(reward);
		    sampledLocalState.add(locState);
		    //sampledLocalAction.add(locAction);
		}
		
		return nextAction;
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
			

			
		    double Qsa = getQValue( locState, locAction );
		    double maxQp = getQValue( nextLocState, nextAction );
		    Qsa = (1 - ALPHA) * Qsa + ALPHA * (reward + GAMMA * maxQp);
			
		    setQValue( locState, locAction, Qsa );
		}
		 return nextAction;
	    }
	}
    }
    
    
    public void updateNSRModelWhenGameOver( int curEpi )
    {
	
	if( curEpi >= NUM_EPISODE_NSR_MODEL_LEARNING ) {
	    
	    System.out.println("No Need to Update NSR Model Now");
	    return;
	}
	else if( sampledRewards.size() !=  sampledLocalState.size() ) { // || sampledRewards.size() != sampledLocalAction.size() ) {
	
	    System.out.println("Sampled List Size Not Match!");
	    return;
	}
	
	while( sampledRewards.size() > 0 ) {
	    
	    //find the state-action pair to be updated
	    int s = sampledLocalState.remove(0);
	    int ns = (int) stateVstNum[s];
	    //int a = sampledLocalAction.remove(0);
	    //int nsa = (int) vstNum[s][a];
		
	    if( ns < NSR_MAX_DATA_NUM ) {
		
		double nsr = 0.0;
		double discount = 1.0;
		for( int index = 0; index < sampledRewards.size(); index++ ) {
			    
		    nsr += sampledRewards.get(index) * discount;
		    discount *= GAMMA;
		}
		    
		dataNSR[s][ns] = nsr;
	    }
	    else {
		    
		//System.out.println("("+s+","+a+") has been explored for "+NSR_MAX_DATA_NUM+" times");
	    }
	    
	    //also remove the first element in reward list
	    sampledRewards.remove(0);
	}
    }
    
    private int getRandomAction( int locState )
    {
	if( locState < 0 || 
		locState >= SparseGridWorld.NUM_CELLS ) {
	    
	    System.out.println("@QLearning->getRandomAction: Wrong Parameters!");
	    return -1;
	}
	
	return random.nextInt(GameAction.NUM_ACTIONS);
    }
    
    
    public void gameFinished( int loop ) 
    {
	
	try {
	    
	    /**
	     * Write the NSR model
	     */
	    BufferedWriter nsrWriter = new BufferedWriter(new FileWriter("./NSR" +"_agent" + agentIndex + ".txt"));
	    for( int locState = 0; locState < SparseGridWorld.NUM_CELLS; locState++ ) {
	
		/**
		 * compute the mean value and variance 
		 * of the N-step returns of the state-action pair
		 */
		double[] nsrArray = dataNSR[locState];
		int ns = (int) stateVstNum[locState];
		if( ns > NSR_MAX_DATA_NUM )
		    ns = NSR_MAX_DATA_NUM;
		    
		double averNSR = 0.0;
		double varNSR = 0.0;
		for( int i = 0; i < ns; i++ ) {
			
		    averNSR += nsrArray[i];
		}
		if( ns > 0 )
		    averNSR /= ns;
		for( int i = 0; i < ns; i++ )  {
			
		    double diff = nsrArray[i] - averNSR;
		    varNSR += diff * diff;
		}
		if( ns > 0 )
		    varNSR /= ns;
		
		//nsrWriter.write( String.valueOf(averNSR)+","+String.valueOf(varNSR)+","+String.valueOf(ns) );
		nsrWriter.write( String.valueOf(averNSR)+","+String.valueOf(varNSR) );
		nsrWriter.newLine();
		
		/**
		if( pickedStates.contains(locState ) ) {
		    
		    locStateList.add(locState);
		    meanList.add(averNSR);
		    varList.add(varNSR);
		}
		*/
	    }
	    nsrWriter.close();
	    
	    /**
	     * Write the Q-table
	     */
	    BufferedWriter qWriter = new BufferedWriter(new FileWriter("./Qs" +"_agent" + agentIndex + ".txt"));
		
	    for( int locState = 0; locState < SparseGridWorld.NUM_CELLS; locState++ ) {
		for( int locAct = 0; locAct < GameAction.NUM_ACTIONS; locAct++ ) {
			
		    double Qvalue = getQValue( locState, locAct );
		    qWriter.write( String.valueOf(Qvalue) );
		    qWriter.newLine();
		}
	    }
	    qWriter.close();
	    
	    /**
	     * write the NSR distribution of the selected states
	     *
	    for( int index = 0; index < locStateList.size(); index++ ) {
		
		int locState = locStateList.get(index);
		
		nsrWriter = new BufferedWriter(new FileWriter("./state_"+locState+"_mean" + ".csv",true));
		
		double averNSR = meanList.get(index);
		double varNSR = varList.get(index);
		nsrWriter.append( String.valueOf(averNSR)+"," );
		nsrWriter.flush();
		nsrWriter.close();
		
		nsrWriter = new BufferedWriter(new FileWriter("./state_"+locState+"_var" + ".csv",true));
		nsrWriter.append( String.valueOf(varNSR)+"," );
		nsrWriter.close();
	    }
	    */
	    
	}
	catch(IOException ioe) {
	    
	    ioe.printStackTrace();
	}
    }
    
}
