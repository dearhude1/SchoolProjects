package algorithms;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import drasys.or.mp.Problem;
import drasys.or.mp.SizableProblemI;
import drasys.or.mp.lp.DenseSimplex;
import drasys.or.mp.lp.LinearProgrammingI;
import gameGridWorld.GameAction;
import gameGridWorld.GameState;
import gameGridWorld.GridWorld;

public class DCEQ extends DecenCEQ
{

    public DCEQ( int index )
    {
	super( index );
    }
    
    
    public DCEQ( int index, double alpha, double gamma, double epsilon )
    {
	super( index, alpha, gamma, epsilon);
    }
    
    /**
     * compute a correlated equilibrium
     * @param agent
     * @param gameState
     * @return
     */
    protected double[] computeCE( int agent, GameState gameState )
    {
	
	if( gameState == null ) {
	    
	    System.out.println("@DCEQ->computeCE: NULL gameState!");
	    return null;
	}
	else if( agent < 0 || agent >= GridWorld.NUM_AGENTS ) {
	    
	    System.out.println();
	    return null;
	}
	
	double[][] q1, q2;
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	q1 = Qs[0][loc0][loc1];
	q2 = Qs[1][loc0][loc1];
	
	return computeCE_dictatorial( agent, q1, q2 );
    }
    
    
    private double[] computeCE_dictatorial( int agent, double[][] q1, double[][] q2 )
    {
	
	double[] ce = new double[16];
	for( int i = 0; i < 16; i++ )
	    ce[i] = 0.0;
	
	SizableProblemI problem = new Problem(41, 16);
	problem.getMetadata().put("lp.isMaximize", "true");
	
	
	try 
	{

	    for( int i = 0; i < GameAction.NUM_ACTIONS; i++ ) {
		for( int j = 0; j < GameAction.NUM_ACTIONS; j++ ) {
		    
		    String a = GameAction.getActionString(i);
		    String b = GameAction.getActionString(j);
		    if( agent == 0 ) {
			problem.newVariable(a+":"+b).setObjectiveCoefficient( q1[i][j] );
		    }
		    else {
			problem.newVariable(a+":"+b).setObjectiveCoefficient( q2[i][j] );
		    }
		}
	    }
	    
	    setConstraints_CE( problem, q1, q2 );
	    
	    LinearProgrammingI iLP;
	    iLP = new DenseSimplex(problem);
	    iLP.solve();
	    ce = iLP.getSolution().getArray();
	    
	    return ce;
	}
	catch(Exception e)
	{
	    //e.printStackTrace();
	    return null;
	}
    }
    
    //record some data for analysis
    /**
     * 
    public void gameFinished()
    {
	try {
	    
	    BufferedWriter writer = new BufferedWriter(new FileWriter("./dCEQ_analysis_"+agentIndex+".txt"));
	    
	    int locNum = GridWorld.NUM_LOCATIONS;
	    for( int s0 = 0; s0 < locNum; s0++ )
		for( int s1 = 0; s1 < locNum; s1++ ) {
		    
		    String line = "("+s0+","+s1+"): ";
		    line += "simiGame: "+String.valueOf((int) simiGameCounters[s0][s1]);
		    line += " Game: "+ String.valueOf((int) gameCounters[s0][s1]);
		    
		    if( !(gameCounters[s0][s1] < 0.01) ) {
			
			double rate = simiGameCounters[s0][s1] / gameCounters[s0][s1];
			
			line += " Rate: "+rate;
		    }
		    
		    writer.write( line );
		    writer.newLine();
		}
	    
	    writer.close();
	}
	catch(IOException e) {
	    
	    e.printStackTrace();
	}
    }
    */
    
    public void gameStarted( int loop )
    {
	//read the two files
	try {
	    
	    BufferedReader gameCountReader = new BufferedReader(new FileReader("./dCEQ_gameCount.csv"));
	    
	    int locNum = GridWorld.NUM_LOCATIONS;
	    
	    String[] pickedStates = {"(0,1)","(1,4)","(2,5)","(3,7)","(4,1)","(4,2)","(5,3)","(6,7)","(7,1)","(7,5)"};
	    
	    String line = "";
	    int index = 0;
	    while( (line = gameCountReader.readLine()) != null) {
		
		if( line.isEmpty() )
		    continue;
		
		int s0 = Integer.parseInt(pickedStates[index].substring(1,2));
		int s1 = Integer.parseInt(pickedStates[index].substring(3,4));
		index++;
		
		int pos = line.indexOf(',');
		double gameCount = Double.parseDouble(line.substring(0, pos));
		
		gameCounters[s0][s1] = gameCount * (loop-1);
	    }
	    gameCountReader.close();
	    
	    BufferedReader rateReader = new BufferedReader(new FileReader("./dCEQ_rate.csv"));
	    index = 0;
	    while( (line = rateReader.readLine()) != null) {
		
		if( line.isEmpty() )
		    continue;
		
		int s0 = Integer.parseInt(pickedStates[index].substring(1,2));
		int s1 = Integer.parseInt(pickedStates[index].substring(3,4));
		index++;
		
		int pos = line.indexOf(',');
		double rate = Double.parseDouble(line.substring(0, pos));
		
		simiGameCounters[s0][s1] = gameCounters[s0][s1] * rate;
	    }
	    rateReader.close();
	}
	catch (Exception e) {
	    // TODO: handle exception
	}
    }
    
    public void gameFinished( int loop )
    {
	try {
	    
	    BufferedWriter rateWriter = new BufferedWriter(new FileWriter("./dCEQ_rate.csv"));
	    BufferedWriter gameCountWriter = new BufferedWriter(new FileWriter("./dCEQ_gameCount.csv"));
	    
	    int locNum = GridWorld.NUM_LOCATIONS;
	    
	    String[] pickedStates = {"(0,1)","(1,4)","(2,5)","(3,7)","(4,1)","(4,2)","(5,3)","(6,7)","(7,1)","(7,5)"};
	    
	    for( int s0 = 0; s0 < locNum; s0++ )
		for( int s1 = 0; s1 < locNum; s1++ ) {
		    
		    
		    String line = "("+s0+","+s1+")";
		    boolean bFind = false;
		    for( int index = 0; index < pickedStates.length; index++ ) {
			
			if( line.equals( pickedStates[index]) ) {
			    
			    bFind = true;
			    break;
			}
		    }
		    
		    //write the data
		    if( bFind ){
			
			String gameCount_line = String.valueOf((int) gameCounters[s0][s1]/loop)+", ";
			
			String rate_line = "";
			if( !(gameCounters[s0][s1] < 0.01) ) {
				
			    double rate = simiGameCounters[s0][s1] / gameCounters[s0][s1];
			    
			    rate_line += rate;
			}
			else 
			    rate_line += 0.0;
			rate_line += ", ";
			
			gameCountWriter.write( gameCount_line );
			rateWriter.write( rate_line );
			gameCountWriter.newLine();
			rateWriter.newLine();
		    }
		}
	    
	    gameCountWriter.close();
	    rateWriter.close();
	}
	catch(IOException e) {
	    
	    e.printStackTrace();
	}
    }
}
