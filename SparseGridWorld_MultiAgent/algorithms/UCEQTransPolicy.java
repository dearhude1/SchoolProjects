package algorithms;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import gameGridWorld.GameAction;
import gameGridWorld.GameState;
import gameGridWorld.SparseGridWorld;

public class UCEQTransPolicy extends UCEQ
{

    /**
     * new parameter for controlling the use of local policy 
     * eta dreases with time
     */
    double ETA = 1.0;//0.9;//0.5;
    
    /**
     * 0.99 for GW1
     * 0.99 for GW2
     */
    double ETA_decay = 0.9;//0.5;//0.99;
    
    /**
     * local Q-table for policy transfer
     */
    double[][][] locQs;
    
    public UCEQTransPolicy()
    {
	/**
	 * index is no use for centralized CE-Q
	 */
	super();
	
	/**
	 * read local Q-tables for policy transfer
	 */
	readLocalQ();
    }
    
    
    public UCEQTransPolicy( double alpha, double gamma, double epsilon )
    {
	
	super( alpha, gamma, epsilon);
	
	/**
	 * read local Q-tables for policy transfer
	 */
	readLocalQ();
    }
    
    private void readLocalQ()
    {
	
	/**
	 * init member locQs
	 */
	locQs = new double[SparseGridWorld.NUM_AGENTS][SparseGridWorld.NUM_CELLS][GameAction.NUM_ACTIONS];
	
	try {
	    
	    for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
		
		String fileName = "./Qs_agent"+agentIndex+".txt";
		BufferedReader qReader = new BufferedReader(new FileReader(fileName));
		
		int locState = 0;
		int locAct = 0;
		
		String line = "";
		while( (line = qReader.readLine()) != null) {
			
		    if( line.isEmpty() )
			continue;
			
		    double qValue = Double.parseDouble( line );
		    
		    locQs[agentIndex][locState][locAct] = qValue;
		    
		    locAct++;
		    if( locAct >= GameAction.NUM_ACTIONS ) {
			
			locAct = 0;
			locState++;
			
			if( locState >= SparseGridWorld.NUM_CELLS ) {
			    
			    break;
			}
		    }
		}
		qReader.close();
	    }	
	    
	}
	catch(IOException ioe) {
	    
	    ioe.printStackTrace();
	}
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
	    
	    System.out.println("@uCEQ_TransPolicy->updateQ: NULL nextState!");
	    
	    return null;
	}
	else {
	    
	    /**
	     * compute the correlated equilibrium in the next state
	     */
	    double[] correlEquil = computeCE( agentIndex, nextState );
	    
	    /**
	     * get a joint action according to the correlated equilibrium
	     */
	    GameAction nextAction = getJointAction( correlEquil, nextState );
	    
	    
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
		 * compute the correspoding Q-values
		 */
		double[] correlValues = getCEQValues( nextState, correlEquil );
		    
		for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		    
		    /**
		     * get the Q-value
		     */
		    double Qsa = getQValue( agent, curState, jointAction );
		    
		    /**
		     * updating rule
		     */
		    Qsa = (1 - ALPHA) * Qsa + ALPHA * (rewards[agent] + GAMMA * correlValues[agent]);
		    
		    
		    /**
		     * variable learning rate
		     */
		    //double alpha = getVariableAlpha( curState, jointAction );
		    //Qsa = (1 - alpha) * Qsa + alpha * (rewards[agent] + GAMMA * correlValues[agent]);
		    
		    /**
		     * write back to the tables
		     */
		    setQValue( agent, curState, jointAction, Qsa );
		    
		    
		    /**
		     * improve the locQ value tables???
		     *
		    int agentLoc = curState.getLocationID(agent);
		    int agentAct = jointAction.getAction(agent);
		    double locQv= locQs[agent][agentLoc][agentAct];
		    locQv = (1 - ALPHA) * locQv + ALPHA * (rewards[agent] + GAMMA * correlValues[agent] );
		    locQs[agent][agentLoc][agentAct] = locQv;
		    */
		}
		
		/**
		 * maybe we can release some memories
		 */
		correlValues = null;
	    }
	    
	    /**
	     * maybe we can release some memories
	     */
	    correlEquil = null;
	    
	    return nextAction;
	}
    }
    
    /**
     * override the method of class CenCEQ
     * get the joint action according to a correlated equilibrium
     */
    protected GameAction getJointAction( double[] correlatedE, GameState gameState )
    {	
	GameAction retAction = new GameAction();
	
	if( gameState == null ) {
	    
	    System.out.println("uCEQ_TransPolicy->getJointAction: Wrong Paramters!");
	    
	    retAction.setAction(0, random.nextInt(GameAction.NUM_ACTIONS));
	    retAction.setAction(1, random.nextInt(GameAction.NUM_ACTIONS));
		
	    return retAction;
	}
	
	
	/**
	 * use the local policy
	 */
	if( random.nextDouble() < ETA ) {
	    
	    
	    for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
		
		int localMaxAction = getLocalAction( agentIndex, gameState );
		retAction.setAction( agentIndex, localMaxAction );
	    }
	    
	    ETA *= ETA_decay;
	    
	}
	/**
	 * return the action according to the computed CE
	 */
	else if( correlatedE == null ) {
	 
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++) {
		
		retAction.setAction(agent, random.nextInt(GameAction.NUM_ACTIONS));
	    }	    
	}
	else {
	
	    retAction = getJointAction_CE(correlatedE);
 
	}
	
	return retAction;
    }
    
    private int getLocalAction( int agentIndex, GameState gameState )
    {
	
	if( agentIndex <  0 || agentIndex >= SparseGridWorld.NUM_AGENTS ||
		gameState == null ) {
	    
	    System.out.println("uCEQ_TransPolicy->getLocalMaxAction: Wrong Paramters!");
	    return random.nextInt( GameAction.NUM_ACTIONS );
	}
	
	int loc = gameState.getLocationID( agentIndex );
	
	/**
	 * select the one with the max Q-value
	 */
	double maxQ = Double.NEGATIVE_INFINITY;
	int maxAction = 0;
	
	/**
	 * maxAction for GW2
	 */
	for( int action = 0; action < GameAction.NUM_ACTIONS; action++ ) {
	    
	    double locQ = locQs[agentIndex][loc][action];
	    if( maxQ < locQ ) {
		
		maxQ = locQ;
		maxAction = action;
	    }
	}
	
	
	/**
	 * Boltzman distribution for GW1
	 *
	double[] expValue = new double[GameAction.NUM_ACTIONS];
	double expSum = 0;
	double Temp = 10;
	for( int action = 0; action < GameAction.NUM_ACTIONS; action++ ) {
	    
	    expValue[action] = Math.exp( locQs[agentIndex][loc][action] / Temp );
	    
	    expSum += expValue[action];
	}
	double samplePro = random.nextDouble();
	double proSum = 0;
	for( int action = 0; action < GameAction.NUM_ACTIONS; action++ ) {
	    
	    proSum += expValue[action] / expSum;
	    
	    if( samplePro <= proSum ) {
		
		maxAction = action;
		break;
	    }
	}
	*/
	
	return maxAction;
	
    }

}
