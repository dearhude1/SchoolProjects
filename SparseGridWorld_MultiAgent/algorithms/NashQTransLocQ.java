package algorithms;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import gameGridWorld.GameAction;
import gameGridWorld.GameState;
import gameGridWorld.SparseGridWorld;

public class NashQTransLocQ extends NashQ
{

    public NashQTransLocQ()
    {
	/**
	 * index is no use for centralized CE-Q
	 */
	super();
	
	
	/**
	 * using local value function to init the Q-tables
	 */
	double[][][] locQs = readLocalQ();

	ArrayList<GameState> allStates = SparseGridWorld.getAllValidStates();
	ArrayList<GameAction> jointActions = SparseGridWorld.getAllJointActions();
	
	for( int stateIndex = 0; stateIndex < allStates.size(); stateIndex ++ ) {
		
	    GameState state = allStates.get( stateIndex );
	   
	    /**
	     * for all joint actions 
	     */
	    for( int jntActIndex = 0; jntActIndex < jointActions.size(); jntActIndex++ ) {
		    
		GameAction jntAction = jointActions.get( jntActIndex );
		
		/**
		 * init the value of the state-action pair for each agent
		 * using local Q-value function
		 * 
		 * note that the Q-tables are initialized in the MARL class
		 * so we need to change the value using setQValue method
		 */
		for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
		    
		    int locState = state.getLocationID( agentIndex );
		    int locAction = jntAction.getAction( agentIndex );
		    
		   double locQValue = locQs[agentIndex][locState][locAction];
		   
		   setQValue( agentIndex, stateIndex, jntActIndex, locQValue );  
		}
		
	    }	
	}
    }
    
    public NashQTransLocQ( double alpha, double gamma, double epsilon )
    {
	
	super( alpha, gamma, epsilon);
	
	/**
	 * using local value function to init the Q-tables
	 */
	double[][][] locQs = readLocalQ();

	ArrayList<GameState> allStates = SparseGridWorld.getAllValidStates();
	ArrayList<GameAction> jointActions = SparseGridWorld.getAllJointActions();
	
	for( int stateIndex = 0; stateIndex < allStates.size(); stateIndex ++ ) {
		
	    GameState state = allStates.get( stateIndex );
	   
	    /**
	     * for all joint actions 
	     */
	    for( int jntActIndex = 0; jntActIndex < jointActions.size(); jntActIndex++ ) {
		    
		GameAction jntAction = jointActions.get( jntActIndex );
		
		/**
		 * init the value of the state-action pair for each agent
		 * using local Q-value function
		 * 
		 * note that the Q-tables are initialized in the MARL class
		 * so we need to change the value using setQValue method
		 */
		for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
			    
		    int locState = state.getLocationID( agentIndex );
		    int locAction = jntAction.getAction( agentIndex );
			    
		    double locQValue = locQs[agentIndex][locState][locAction];
			   
		    setQValue( agentIndex, stateIndex, jntActIndex, locQValue );  
		}
		
	    }	
	}
    }
    
    private double[][][] readLocalQ()
    {
	double[][][] locQs = new double[SparseGridWorld.NUM_AGENTS][SparseGridWorld.NUM_CELLS][GameAction.NUM_ACTIONS];
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
	    return null;
	}
	
	return locQs;
    }

}
