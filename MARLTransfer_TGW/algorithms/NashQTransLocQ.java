package algorithms;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import gameGridWorld.GameAction;
import gameGridWorld.GridWorld;

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
	int locNum = GridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	for( int agentIndex = 0; agentIndex < GridWorld.NUM_AGENTS; agentIndex++ )
	    for( int s1 = 0; s1 < locNum; s1++ )
		for( int s2 = 0; s2 < locNum; s2++ )
		    for( int a1 = 0; a1 < actionNum; a1++ )
			for( int a2 = 0; a2 < actionNum; a2++ ) {
				    
			    Qs[0][s1][s2][a1][a2] = locQs[0][s1][a1];
			    Qs[1][s1][s2][a1][a2] = locQs[1][s2][a2];
			}
    }
    
    public NashQTransLocQ( double alpha, double gamma, double epsilon )
    {
	
	super( alpha, gamma, epsilon);
	
	/**
	 * using local value function to init the Q-tables
	 */
	double[][][] locQs = readLocalQ();
	int locNum = GridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	for( int agentIndex = 0; agentIndex < GridWorld.NUM_AGENTS; agentIndex++ )
	    for( int s1 = 0; s1 < locNum; s1++ )
		for( int s2 = 0; s2 < locNum; s2++ )
		    for( int a1 = 0; a1 < actionNum; a1++ )
			for( int a2 = 0; a2 < actionNum; a2++ ) {
				    
			    Qs[0][s1][s2][a1][a2] = locQs[0][s1][a1];
			    Qs[1][s1][s2][a1][a2] = locQs[1][s2][a2];
			}
    }
    
    private double[][][] readLocalQ()
    {
	double[][][] locQs = new double[GridWorld.NUM_AGENTS][GridWorld.NUM_LOCATIONS][GameAction.NUM_ACTIONS];
	try {
	    
	    for( int agentIndex = 0; agentIndex < GridWorld.NUM_AGENTS; agentIndex++ ) {
		
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
			
			if( locState >= GridWorld.NUM_LOCATIONS ) {
			    
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
