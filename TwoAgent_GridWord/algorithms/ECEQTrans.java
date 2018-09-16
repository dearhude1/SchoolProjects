package algorithms;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import gameGridWorld.GameAction;
import gameGridWorld.GameState;
import gameGridWorld.GridWorld;
import drasys.or.mp.Problem;
import drasys.or.mp.SizableProblemI;
import drasys.or.mp.lp.DenseSimplex;
import drasys.or.mp.lp.LinearProgrammingI;


public class ECEQTrans extends CEQTrans
{
    public ECEQTrans()
    {
	/**
	 * index is no use for centralized CE-Q
	 */
	super();
	
    }
    
    
    public ECEQTrans( double alpha, double gamma, double epsilon )
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
	    
	    System.out.println("@CenCEQ->computeCE: NULL gameState!");
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
	
	return computeCE_egalitarian( q1, q2 );
    }
    
    private double[] computeCE_egalitarian( double[][] q1, double[][] q2 )
    {
	
	double[] ce = new double[16];
	for( int i = 0; i < 16; i++ )
	    ce[i] = 0.0;
	
	SizableProblemI problem = new Problem(41, 16);
	problem.getMetadata().put("lp.isMaximize", "true");
	
	
	try 
	{
	    /**
	     * firstly, set agent 1's expectation 
	     * as the objective function
	     */
	    for( int i = 0; i < GameAction.NUM_ACTIONS; i++ )
	    {
		for( int j = 0; j < GameAction.NUM_ACTIONS; j++ )
		{
		    String a = GameAction.getActionString(i);
		    String b = GameAction.getActionString(j);
		    problem.newVariable(a+":"+b).setObjectiveCoefficient( -q1[i][j] );
		}
	    }
	    
	    setConstraints_CE( problem, q1, q2 );
	    
	    LinearProgrammingI iLP;
	    iLP = new DenseSimplex(problem);
	    double ans = iLP.solve();
	    ce = iLP.getSolution().getArray();
	    
	    /**
	     * then change the objective function 
	     * to agent 2's expectation
	     */
	    for( int i = 0; i < GameAction.NUM_ACTIONS; i++ )
	    {
		for( int j = 0; j < GameAction.NUM_ACTIONS; j++ )
		{
		    String a = GameAction.getActionString(i);
		    String b = GameAction.getActionString(j);
		    problem.getVariable(a+":"+b).setObjectiveCoefficient( -q2[i][j] );			
		}
	    }
	    iLP.setProblem(problem);
	    double ans2 = iLP.solve();
	    if( ans < ans2 )
	    {
		ans = ans2;
		ce = iLP.getSolution().getArray();
	    }
	   
	    
	    return ce;
	}
	catch(Exception e)
	{
	    //e.printStackTrace();
	    return null;
	}
    }
    
    public void gameStarted( int loop )
    {
	if( loop == 1 )
	    return;
	
	int locNum = GridWorld.NUM_LOCATIONS;
	String[] pickedStates = {"(0,1)","(1,4)","(2,5)","(3,7)","(4,1)","(4,2)","(5,3)","(6,7)","(7,1)","(7,5)"};
	    
	for( int s0 = 0; s0 < locNum; s0++ )
	    for( int s1 = 0; s1 < locNum; s1++ ) {
		    
		    
		String line = "("+s0+","+s1+")";
		boolean bFind = true;
		if( s0 == s1 )
		    bFind = false;
		
		/**
		for( int index = 0; index < pickedStates.length; index++ ) {
			
		    if( line.equals( pickedStates[index]) ) {
			    
			bFind = true;
			break;
		    }
		}
		*/
		
		//write the data
		if( bFind ){
		
		    try {
			    
			String fileName = "./eCEQTrans_"+line+".csv";
			BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
			    
			String fileLine = "";
			int time = 0;
			while( (fileLine = fileReader.readLine()) != null ) {
			    
			    if( fileLine.isEmpty() )
				continue;
			    
			    int pos = fileLine.indexOf(",");
			    double error = Double.parseDouble(fileLine.substring(0,pos));
			    errorBounds[s0][s1][time] = error * (loop-1);
			    
			    time++;
			}
			fileReader.close();
		    }
		    catch(IOException e) {
			    
			e.printStackTrace();
		    }
		}
	    }
    }
    
    public void gameFinished( int loop )
    {
	
	int locNum = GridWorld.NUM_LOCATIONS;
	String[] pickedStates = {"(0,1)","(1,4)","(2,5)","(3,7)","(4,1)","(4,2)","(5,3)","(6,7)","(7,1)","(7,5)"};
	    
	for( int s0 = 0; s0 < locNum; s0++ )
	    for( int s1 = 0; s1 < locNum; s1++ ) {
		    
		    
		String line = "("+s0+","+s1+")";
		
		boolean bFind = true;
		if( s0 == s1 )
		    bFind = false;
		
		/**
		for( int index = 0; index < pickedStates.length; index++ ) {
			
		    if( line.equals( pickedStates[index]) ) {
			    
			bFind = true;
			break;
		    }
		}
		*/
		
		//write the data
		if( bFind ){
		
		    try {
			    
			String fileName = "./eCEQTrans_"+line+".csv";
			BufferedWriter fileWriter = new BufferedWriter(new FileWriter(fileName));
			    
			for( int t = 0; t < timeStepLimit; t++ ) {
			    
			    double error = errorBounds[s0][s1][t] / loop;
			    fileWriter.write(error+",");
			    fileWriter.newLine();
			}
			fileWriter.close();
		    }
		    catch(IOException e) {
			    
			e.printStackTrace();
		    }
		}
	    }
    }

}
