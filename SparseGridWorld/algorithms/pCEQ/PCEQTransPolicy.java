package algorithms.pCEQ;

import gameGridWorld.GameAction;
import gameGridWorld.GameState;
import gameGridWorld.SparseGridWorld;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class PCEQTransPolicy extends PCEQ
{


    /**
     * new parameter for controlling the use of local policy 
     * eta dreases with time
     */
    double ETA = 0.5;//1.0;
    
    double ETA_decay = 0.999;
    
    /**
     * local Q-table for policy transfer
     */
    double[][][] locQs;
    
    
    double[] eta = null;
    double[] eta_decay = null;
    
    public PCEQTransPolicy()
    {
	/**
	 * index is no use for centralized CE-Q
	 */
	super();
	
	/**
	 * read local Q-tables for policy transfer
	 */
	readLocalQ();
	
	eta = new double[SparseGridWorld.NUM_AGENTS];
	eta_decay = new double[SparseGridWorld.NUM_AGENTS];
	eta[0] = ETA;
	eta[1] = 0.5;//1.0;
	eta_decay[0] = ETA_decay;
	eta_decay[1] = 0.9;
	
    }
    
    
    public PCEQTransPolicy( double alpha, double gamma, double epsilon )
    {
	
	super( alpha, gamma, epsilon);
	
	/**
	 * read local Q-tables for policy transfer
	 */
	readLocalQ();
	
	eta = new double[SparseGridWorld.NUM_AGENTS];
	eta_decay = new double[SparseGridWorld.NUM_AGENTS];
	eta[0] = ETA;
	eta[1] = 0.5;//1.0;
	eta_decay[0] = ETA_decay;
	eta_decay[1] = 0.9;
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
	    GameAction nextAction = getJointAction_XX( correlEquil, nextState );
	    
	    
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
    
    protected GameAction getJointAction_XX( double[] correlatedE, GameState gameState )
    {	
	GameAction retAction = new GameAction();
	
	if( gameState == null ) {
	    
	    System.out.println("uCEQ_TransPolicy->getJointAction: Wrong Paramters!");
	    
	    retAction.setAction(0, random.nextInt(GameAction.NUM_ACTIONS));
	    retAction.setAction(1, random.nextInt(GameAction.NUM_ACTIONS));
		
	    return retAction;
	}
	
	for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
	    
	    
	    if( random.nextDouble() < eta[agentIndex] ) {
		
		int localMaxAction = getLocalAction( agentIndex, gameState );
		retAction.setAction( agentIndex, localMaxAction );
		
		eta[agentIndex] *= eta_decay[agentIndex];//ETA_decay;
	    }
	    else if( correlatedE == null ) {
			    
		retAction.setAction(agentIndex, random.nextInt(GameAction.NUM_ACTIONS));
	    }
	    else {
			
		double[] probabilities = new double[16];
			
		probabilities[0] = correlatedE[0];
		for( int i = 1; i < 16; i++ ) {
		    probabilities[i] =  probabilities[i-1] + correlatedE[i];
		}
				
		double d = random.nextDouble();
		int actionIndex = 0;
		for( int i = 0; i < 16; i++ ) {
		    
		    if( d < probabilities[i] ) {
			actionIndex = i;
			break;
		    }
		}
			    
		int jointAction0 = actionIndex / GameAction.NUM_ACTIONS;
		
		if( agentIndex == 0 )
		    retAction.setAction(0, jointAction0);
		else 
		    retAction.setAction(1, actionIndex % GameAction.NUM_ACTIONS);    
	    }
	}

	
	return retAction;
    }
    
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
	    //System.out.println("ETA: "+ETA);
	}
	/**
	 * return the action according to the computed CE
	 */
	else if( correlatedE == null ) {
	 
	    retAction.setAction(0, random.nextInt(GameAction.NUM_ACTIONS));
	    retAction.setAction(1, random.nextInt(GameAction.NUM_ACTIONS));
	}
	else {
	
	    double[] probabilities = new double[16];
		
	    probabilities[0] = correlatedE[0];
	    for( int i = 1; i < 16; i++ )
	    {
		probabilities[i] =  probabilities[i-1] + correlatedE[i];
	    }
			
	    double d = random.nextDouble();
	    int actionIndex = 0;
	    for( int i = 0; i < 16; i++ )
	    {
		if( d < probabilities[i] )
		{
		    actionIndex = i;
		    break;
		}
	    }
		    
	    int jointAction0 = actionIndex / GameAction.NUM_ACTIONS;
	    retAction.setAction(0, jointAction0);
	    retAction.setAction(1, actionIndex % GameAction.NUM_ACTIONS);    
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
	for( int action = 0; action < GameAction.NUM_ACTIONS; action++ ) {
	    
	    double locQ = locQs[agentIndex][loc][action];
	    if( maxQ < locQ ) {
		
		maxQ = locQ;
		maxAction = action;
	    }
	}
	
	
	/**
	 * how about Boltzman distribution??
	 */
	
	return maxAction;
	
    }

}
