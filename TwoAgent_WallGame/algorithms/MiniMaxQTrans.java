package algorithms;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import wallGame.GameAction;
import wallGame.GameState;
import wallGame.WallGame;

public class MiniMaxQTrans extends MiniMaxQ
{
    
    /**
     * for error bound analysis
     */
    private int timeStepLimit = 3000;
    private double[][][] errorBounds;
    private int[][] transTimes;
    
    /**
     * we use tau to denote the threshold of the error bound
     */
    private double tau = 0.1;//5.0;//0.1;
    
    public MiniMaxQTrans( int index )
    {
	super(index);
	
	//for error bound analysis
	int locNum = WallGame.NUM_LOCATIONS;
	errorBounds = new double[locNum][locNum][timeStepLimit];
	transTimes = new int[locNum][locNum];
	 
	for( int s1 = 0; s1 < locNum; s1++ )
	    for( int s2 = 0; s2 < locNum; s2++ ) {
		
		transTimes[s1][s2] = 0;
		
		for( int t = 0; t < timeStepLimit; t++ ) 
		    errorBounds[s1][s2][t] = 0.0;
	    }
	
    }
    
    public MiniMaxQTrans( int index, double alpha, double gamma, double epsilon )
    {
	
	super(index, alpha, gamma, epsilon);
	
	//for error bound analysis
	int locNum = WallGame.NUM_LOCATIONS;
	errorBounds = new double[locNum][locNum][timeStepLimit];
	transTimes = new int[locNum][locNum];
	 
	for( int s1 = 0; s1 < locNum; s1++ )
	    for( int s2 = 0; s2 < locNum; s2++ ) {
		
		transTimes[s1][s2] = 0;
		
		for( int t = 0; t < timeStepLimit; t++ ) 
		    errorBounds[s1][s2][t] = 0.0;
	    }
    }
    
    
    public GameAction updateQ( GameState curState, GameAction jointAction, 
	    double[] rewards, GameState nextState )
    {
	if( nextState == null ) {
	    
	    System.out.println("@MiniMaxQ->updateQ: NULL nextState!");
	    
	    return null;
	}
	else {
	    
	    
	    /**
	     * select action in the next state according to pi(s)
	     */
	    GameAction nextAction = sampleAction( nextState );
	    
	    /**
	     * update the Q-tables
	     * but if this is the initial state of the game
	     * just return the action
	     */
	    if( curState != null && jointAction != null 
		&& rewards != null )  {
		
		
		/**
		 * mark a visit
		 */
		visit( curState, jointAction );
		
		
		/**
		 * learning rule:
		 * Q(s,a) <- (1-alpha)Q(s,a) + alpha * (reward + gamma * V(s'))
		 */
		double Qsa = getQValue( agentIndex, curState, jointAction );
		double Vsp = getV( nextState );
		
		double alpha = getVariableAlpha(curState, jointAction);
		Qsa = (1 - alpha) * Qsa + alpha * ( rewards[agentIndex] + GAMMA * Vsp );
		
		//Qsa = (1 - ALPHA) * Qsa + ALPHA * ( rewards[agentIndex] + GAMMA * Vsp );
		
		
		setQValue( agentIndex, curState, jointAction, Qsa );
		
		
		/**
		 * whether to update policy
		 */
		boolean bCompute = shouldCompute( curState );
		if( bCompute ) {
		    
		    
		    /**
		     * linear programming to update the policy in curState
		     */
		    double minimaxV = updatePolicy( curState );
		    setV( curState,  minimaxV );
		}
		else {
		    
		    int loc0 = curState.getLocationID(0);
		    int loc1 = curState.getLocationID(1);
		    double minimaxV = getPolicyMinValue( curState, pi[loc0][loc1]);
		    setV( curState,  minimaxV );
		    
	
		}
		

		
		ALPHA *= alphaDecay;
	    }
	    
	    return nextAction;
	}
    }
    
    private boolean shouldCompute( GameState gameState )
    {
	if( gameState == null ) {
	    
	    System.out.println("MiniMaxQTrans->shouldComputeNE: Parameter error");
	    return true;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	
	/**
	 * the last policy
	 */
	double[] lastPolicy = pi[loc0][loc1]; 
	double lastPolicyValue = getPolicyMinValue(gameState, lastPolicy);
	
	
	/**
	 * check the last policy
	 */
	boolean[] actAvail = WallGame.actionSet( gameState.getLocationID(agentIndex) );
	
	double max_error = Double.NEGATIVE_INFINITY;
	for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
	    
	    if( !actAvail[act] )
		continue;
	   
	    /**
	     * a new pure policy
	     */
	    double[] purePolicy = new double[GameAction.NUM_ACTIONS];
	    for( int act_p = 0; act_p < GameAction.NUM_ACTIONS; act_p++ ) {
		
		if( act_p == act )
		    purePolicy[act_p] = 1.0;
		else
		    purePolicy[act_p] = 0.0; 
	    }
	    double minValue = getPolicyMinValue(gameState, purePolicy);
	    
	    /**
	     * error(a,pi) = min_o Q(s,a,o) - min_o \Sum_{a'} pi(a')Q(s,a',o)
	     */
	    double error = minValue - lastPolicyValue;
	    if( error > max_error ) 
		max_error = error;
	}
	
	if( max_error <= tau ) {
	    
	    //if( max_error > 0 )
		//System.out.println("Bigger than 0"+ max_error);
		    
	    
	    //record the error bound///////////////////////////
	    if( transTimes[loc0][loc1] < timeStepLimit ) {
		    
		int time = transTimes[loc0][loc1];
		errorBounds[loc0][loc1][time] += max_error;// * Math.pow(10, 14);
		    
		if( agentIndex == 0 && max_error > 0)
		    System.out.println(loc0+","+loc1+","+errorBounds[loc0][loc1][time]);
		    
		transTimes[loc0][loc1] += 1;
	    }
	    //for error bound analysis//////////////////////////
	    
	    return false;
	}
	else {
	    return true;
	}
	
    }

    private double getPolicyMinValue( GameState gameState, double[] policy )
    {
	if( gameState == null || policy == null || 
		policy.length != GameAction.NUM_ACTIONS ) {
	    
	    System.out.println("MiniMaxQTrans->getPolicyValue: Parameter error");
	    return 0;
	}
	
	double minValue = Double.POSITIVE_INFINITY;
	for( int act_o = 0; act_o < GameAction.NUM_ACTIONS; act_o++ ) {
	    
	    double value = 0.0;
	    for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
		
		GameAction jntAction = new GameAction();
		jntAction.setAction( agentIndex, act );
		jntAction.setAction( (agentIndex+1) % WallGame.NUM_AGENTS, act_o);
		value += getQValue(agentIndex, gameState, jntAction) * policy[act];
	    }
	    if( value < minValue ) {
		
		minValue = value;
	    }   
	}
	
	return minValue;
    }
    
    public void gameStarted( int loop )
    {
	if( loop == 1 )
	    return;
	
	int locNum = WallGame.NUM_LOCATIONS;
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
			    
			String fileName = "./MinimaxQTrans_"+agentIndex+"_"+line+".csv";
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
	
	int locNum = WallGame.NUM_LOCATIONS;
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
			    
			String fileName = "./MinimaxQTrans_"+agentIndex+"_"+line+".csv";
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
