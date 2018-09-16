package algorithms.eCEQ;

import java.util.ArrayList;

import algorithms.CenCEQ;
import drasys.or.mp.Problem;
import drasys.or.mp.SizableProblemI;
import drasys.or.mp.lp.DenseSimplex;
import drasys.or.mp.lp.LinearProgrammingI;
import gameGridWorld.GameAction;
import gameGridWorld.GameState;
import gameGridWorld.SparseGridWorld;

public class ECEQ extends CenCEQ
{

    public ECEQ()
    {
	/**
	 * index is no use for centralized CE-Q
	 */
	super();
	
    }
    
    
    public ECEQ( double alpha, double gamma, double epsilon )
    {
	
	super( alpha, gamma, epsilon);
	
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
	    
	    System.out.println("@ECEQ->computeCE: NULL gameState!");
	    return null;
	}
	else if( agent < 0 || agent >= SparseGridWorld.NUM_AGENTS ) {
	    
	    System.out.println();
	    return null;
	}
	
	
	return computeCE_egalitarian( gameState );
    }
    
    private double[] computeCE_egalitarian( GameState gameState )
    {
	
	int jointActionNum = SparseGridWorld.getAllJointActions().size();
	SizableProblemI problem = new Problem(300, jointActionNum);
	problem.getMetadata().put("lp.isMaximize", "true");
	
	double[] ce = new double[jointActionNum];
	for( int i = 0; i < jointActionNum; i++ )
	    ce[i] = 0.0;
	
	try 
	{
	    
	    double maxAns = Double.NEGATIVE_INFINITY;
	    
	    ArrayList<GameAction> jntActions = SparseGridWorld.getAllJointActions();	    
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		  
		for( int actionIndex = 0; actionIndex < jntActions.size(); actionIndex++ )  {
			
		    GameAction jntAction = jntActions.get( actionIndex );
		    double coeff = -getQValue( agent, gameState, jntAction );
		    
		    //set coeff
		    if( agent == 0 )
			problem.newVariable(""+actionIndex).setObjectiveCoefficient( coeff );
		    else
			problem.getVariable(""+actionIndex).setObjectiveCoefficient( coeff );
		}
		
		if( agent == 0 )
		    setConstraints_CE( problem, gameState );
		
		LinearProgrammingI iLP;
		iLP = new DenseSimplex(problem);
		double ans = iLP.solve();
		ce = iLP.getSolution().getArray();
		
		if( ans > maxAns ) {
		    
		    maxAns = ans;
		    ce = iLP.getSolution().getArray();
		}
	    }
	    
	    
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
	    
	    BufferedWriter writer = new BufferedWriter(new FileWriter("./eCEQ_analysis.txt"));
	    
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
	/**
	//read the two files
	try {
	    
	    BufferedReader gameCountReader = new BufferedReader(new FileReader("./eCEQ_gameCount.csv"));
	    
	    //int locNum = GridWorld.NUM_LOCATIONS;
	    
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
	    
	    BufferedReader rateReader = new BufferedReader(new FileReader("./eCEQ_rate.csv"));
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
	*/
    }
    
    public void gameFinished( int loop )
    {
	/**
	try {
	    
	    BufferedWriter rateWriter = new BufferedWriter(new FileWriter("./eCEQ_rate.csv"));
	    BufferedWriter gameCountWriter = new BufferedWriter(new FileWriter("./eCEQ_gameCount.csv"));
	    
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
	
	*/
    }
}
