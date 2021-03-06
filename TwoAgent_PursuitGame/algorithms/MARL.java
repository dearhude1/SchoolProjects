package algorithms;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Random;

import gamePursuitGame.GameAction;
import gamePursuitGame.GameState;
import gamePursuitGame.PursuitGame;

/**
 * the base class of all MARL algorithms
 * @author dearhude1
 *
 */
public class MARL
{

    public static final int MINIMAXQ = 0;
    public static final int NASHQ = 1;
    public static final int FFQ = 2;
    public static final int DENCE_CEQ = 3;
    public static final int CEN_CEQ = 4;
    public static final int uCEQ = 5;
    public static final int eCEQ = 6;
    public static final int pCEQ = 7;
    public static final int dCEQ = 8;
    public static final int NEGOQ = 9;
    public static final int WOLFPHC = 10;
    public static final int RANDOM = 11;
    public static final int MINIMAXQ_TRANS = 12;
    
    
    public static final String[] ALG_STRINGS = { "minimaxQ", "NashQ", "FFQ", "decCEQ", "cenCEQ", 
	"uCEQ", "eCEQ", "pCEQ", "dCEQ", "NegoQ", "WoLF-PHC", "Random", "minimaxQTrans" };
    
    /**
     * fundamental parameter of MARL algorithms
     */
    protected double ALPHA = 0.9;
    protected double GAMMA = 0.9;
    protected double EPSILON = 0.02;
    
    /**
     * for random use
     */
    protected Random random;
    
    /**
     * Q-table of the corresponding agent
     * 3 dimensions for state
     * 3 dimensions for joint action
     */
    protected double[][][][][] Qs;
    
    /**
     * visit number of each state-action pair
     */
    protected double[][][][] vstNum;
    
    /**
     * an algorithm can also represent an agent
     * agent index begins with 0
     */
    protected int agentIndex;
    
    
    public MARL( int index )
    {
	agentIndex = index;
	random = new Random();
	
	/**
	 * init the Q-table
	 */
	int locNum = PursuitGame.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	Qs = new double[PursuitGame.NUM_AGENTS][locNum][locNum][actionNum][actionNum];
	vstNum = new double[locNum][locNum][actionNum][actionNum];
	for( int agentIndex = 0; agentIndex < PursuitGame.NUM_AGENTS; agentIndex++ )
	    for( int s1 = 0; s1 < locNum; s1++ )
		for( int s2 = 0; s2 < locNum; s2++ )
		    for( int a1 = 0; a1 < actionNum; a1++ )
			for( int a2 = 0; a2 < actionNum; a2++ ) {
				    
			    Qs[agentIndex][s1][s2][a1][a2] = 0.0;
				    
			    vstNum[s1][s2][a1][a2] = 0.0;
			}
    }
    
    public MARL( int index, double alpha, double gamma, double epsilon )
    {
	agentIndex = index;
	random = new Random();
	
	ALPHA = alpha;
	GAMMA = gamma;
	EPSILON = epsilon;
	
	/**
	 * init the Q-table
	 */
	int locNum = PursuitGame.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	Qs = new double[PursuitGame.NUM_AGENTS][locNum][locNum][actionNum][actionNum];
	vstNum = new double[locNum][locNum][actionNum][actionNum];
	for( int agentIndex = 0; agentIndex < PursuitGame.NUM_AGENTS; agentIndex++ )
	    for( int s1 = 0; s1 < locNum; s1++ )
		for( int s2 = 0; s2 < locNum; s2++ )
		    for( int a1 = 0; a1 < actionNum; a1++ )
			for( int a2 = 0; a2 < actionNum; a2++ ) {
				    
			    Qs[agentIndex][s1][s2][a1][a2] = 0.0;
				    
			    vstNum[s1][s2][a1][a2] = 0.0;
			}
    }
    
    /**
     * Compute the action for the current state
     * this is also a core method for MARL algorithm in Pursuit Game
     * @return
     */
    public GameAction getAction( GameState gameState )
    {
	/**
	 * return a random action
	 */
	GameAction jntAction = new GameAction();
	
	jntAction.setAction( 0, random.nextInt(GameAction.NUM_ACTIONS) );
	jntAction.setAction(1, random.nextInt(GameAction.NUM_ACTIONS) );
	
	return jntAction;
    }
    
    /**
     * epsilon-greedy for one agent
     * @param action
     * @return
     */
    public int epsilonGreedy( int action )
    {
	if( random.nextDouble() < EPSILON ) 
	    return random.nextInt(GameAction.NUM_ACTIONS);
	else
	    return action;
    }
    
    public GameAction epsilonGreedy( GameAction gameAction )
    {
	if( random.nextDouble() < EPSILON ) {
	    
	    for( int agentIndex = 0; agentIndex < PursuitGame.NUM_AGENTS; agentIndex++ ) {
		
		gameAction.setAction( agentIndex, random.nextInt(GameAction.NUM_ACTIONS) );
	    }   
	}
	
	return gameAction;
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
	
	return null;
    }
    
    public int getAgentIndex() 
    {
	return agentIndex;
    }
    
    protected void visit( GameState curState, GameAction curAction )
    {
	if( curState == null || curAction == null ) {
	    
	    System.out.println("@MARL->visit: Wrong Parameters!");
	    return;
	}
	
	int loc0 = curState.getLocationID(0);
	int loc1 = curState.getLocationID(1);
	int a0 = curAction.getAction(0);
	int a1 = curAction.getAction(1);
	
	vstNum[loc0][loc1][a0][a1] += 1.0;
    }
    
    protected double getVariableAlpha( GameState gameState, GameAction gameAction )
    {
	if( gameState == null || gameAction == null ) {
	    
	    System.out.println("@MARL->getVariableAlpha: Wrong Parameters!");
	    return 0.0;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int a0 = gameAction.getAction(0);
	int a1 = gameAction.getAction(1);
	
	if( vstNum[loc0][loc1][a0][a1] <= 0.0 )
	    return 1.0;
	else
	    return 1.0 / vstNum[loc0][loc1][a0][a1];
	
    }
    
    protected double getQValue( int agent, GameState gameState, 
	    GameAction gameAction )
    {
	if( gameAction == null || 
		gameState == null ) {
	    
	    System.out.println("@CenCEQ->getQValue: Wrong Parameters!");
	    return 0.0;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int a0 = gameAction.getAction(0);
	int a1 = gameAction.getAction(1);
	
	return Qs[agent][loc0][loc1][a0][a1];
    }
    
    protected void setQValue( int agent, GameState gameState, 
	    GameAction gameAction, double value )
    {
	if( gameAction == null || 
		gameState == null ) {
	    
	    System.out.println("@CenCEQ->setQValue: Wrong Parameters!");
	    return;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int a0 = gameAction.getAction(0);
	int a1 = gameAction.getAction(1);
	
	Qs[agent][loc0][loc1][a0][a1] = value;
    }
    
    /**
     * store the Q-values learnt
     */
    public void storeQValues( String algName )
    {
	try {
	    
	    /**
	     * write all values in member Qs
	     * 
	     * only the table of this agent
	     */
	    BufferedWriter writer = new BufferedWriter(new FileWriter("./"+algName+"_values_agent"+agentIndex+".txt"));
	    int locNum = PursuitGame.NUM_LOCATIONS;
	    int actionNum = GameAction.NUM_ACTIONS;
	    for( int s1 = 0; s1 < locNum; s1++ )
		for( int s2 = 0; s2 < locNum; s2++ )
		    for( int a1 = 0; a1 < actionNum; a1++ )
			for( int a2 = 0; a2 < actionNum; a2++ ) {
					    
			    writer.write(""+Qs[agentIndex][s1][s2][a1][a2]);
			    writer.newLine();
			}
	    writer.close();
	}
	catch (Exception e) {
	    e.printStackTrace();
	}
    }
    
    /**
     * read Q-values of a specific agent from file
     */
    public void readQValues( String algName, int agent )
    {
	
	try {
	    
	    /**
	     * write all values in member Qs
	     */
	    BufferedReader reader = new BufferedReader(new FileReader("./"+algName+"_values_agent"+agent+".txt"));
	    int locNum = PursuitGame.NUM_LOCATIONS;
	    int actionNum = GameAction.NUM_ACTIONS;
	    
	    String line = "";
	    int s1 = 0;
	    int s2 = 0;
	    int a1 = 0;
	    int a2 = 0;
	    
	    while( (line = reader.readLine()) != null ) {
		
		if( line.length() == 0 )
		    continue;
		
		Qs[agent][s1][s2][a1][a2] = Double.parseDouble( line );
		
		a2++;
		if( a2 >= actionNum ) {
		    
		    a2 = 0;
		    a1++;
		    if( a1 >= actionNum ) {
			
			a1 = 0;
			s2++;
			if( s2 >= locNum ) {
			    
			    s2 = 0;
			    s1++;
			}
		    }
		}
	    }
	    reader.close();
	}
	catch (Exception e) {
	    e.printStackTrace();
	}
    }
    
    public void storePolicy()
    {
	
    }
    
    public void readPolicy()
    {
	
    }
}
