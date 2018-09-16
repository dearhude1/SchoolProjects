package algorithms.nashQ;

import gameGridWorld.GameAction;
import gameGridWorld.GameState;
import gameGridWorld.SparseGridWorld;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import drasys.or.mp.Constraint;
import drasys.or.mp.Problem;
import drasys.or.mp.SizableProblemI;
import drasys.or.mp.lp.DenseSimplex;
import drasys.or.mp.lp.LinearProgrammingI;

public class NashQTransModel extends NashQ
{

    /**
     * local Q-table for transfer value function
     */
    private double[][][] locQs;

    /**
     * the model of the reward function
     * 
     * note that this member stores the reward sum!!!
     */
    private double[][][] transRmodel;
    
    /**
     * the model of the transition function
     */
    private double[][][][] transTmodel;
    
    /**
     * the following three arrays 
     * are used for constructing the real local 
     * model in the MAS
     */
    private double[][][] Rmodel;
    private int[][][] Nsa;
    private double[][][][] Tmodel;
    
    /**
     * for iteratively computing the state similarity
     */
    private double[][] stateSimilarity;
    
    /**
     * the distance between each state 
     * in the current model
     */
    private double[][][] similarityModel;
    
    /**
     * the distance between each state in 
     * the transfered model
     */
    private double[][][] similarityTransModel;
    
    /**
     * the number of exploration episodes 
     * for construting the local model
     */
    private int numExpEpisodes = 100;
    
    private boolean bLearning = false;
    
    
    private double transferConditionValue = 15;//50; //25;//0.05;
    
    private ArrayList<Integer>[] validCellsLists = null;
    
    
    private double[] transCndValues = null;
    
    
    public NashQTransModel()
    {
	/**
	 * index is no use for centralized CE-Q
	 */
	super();
	
	/**
	 * read local Q-tables for value function transfer
	 */
	readLocalQ();
	
	/**
	 * read the model
	 */
	readLocalModel();
	
	int agentNum = SparseGridWorld.NUM_AGENTS;
	int locNum = SparseGridWorld.NUM_CELLS;
	int actionNum = GameAction.NUM_ACTIONS;
	
	transCndValues = new double[agentNum];
	transCndValues[0] = 1.5;
	transCndValues[1] = 3.0;
	
	Nsa = new int[agentNum][locNum][actionNum];
	Rmodel = new double[agentNum][locNum][actionNum];
	Tmodel = new double[agentNum][locNum][actionNum][locNum];
	stateSimilarity = new double[agentNum][locNum];
	
	similarityModel = new double[agentNum][locNum][locNum];
	similarityTransModel = new double[agentNum][locNum][locNum];
	
	for( int ag = 0; ag < SparseGridWorld.NUM_AGENTS; ag++ )
	    for( int s = 0; s < locNum; s++ ) {
		
		stateSimilarity[ag][s] = 0.0;
		
		for( int a = 0; a < actionNum; a++ ) {
				    
		    Nsa[ag][s][a] = 0;
		    Rmodel[ag][s][a] = 0.0;    
		    
		    for( int sp = 0; sp < locNum; sp++ ) {
		    
			Tmodel[ag][s][a][sp] = 0;
			
			
			
			similarityModel[ag][s][sp] = 0.0;
			similarityTransModel[ag][s][sp] = 0.0;
		    }
		}
	    }
    }
    
    public NashQTransModel( double alpha, double gamma, double epsilon )
    {
	super( alpha, gamma, epsilon);
	
	/**
	 * read local Q-tables for value function transfer
	 */
	readLocalQ();
	
	/**
	 * read the model
	 */
	readLocalModel();
	
	int agentNum = SparseGridWorld.NUM_AGENTS;
	int locNum = SparseGridWorld.NUM_CELLS;
	int actionNum = GameAction.NUM_ACTIONS;
	
	transCndValues = new double[agentNum];
	transCndValues[0] = 13;//1.5;
	transCndValues[1] = 13;//3.0;
	
	Nsa = new int[agentNum][locNum][actionNum];
	Rmodel = new double[agentNum][locNum][actionNum];
	Tmodel = new double[agentNum][locNum][actionNum][locNum];
	stateSimilarity = new double[agentNum][locNum];
	
	similarityModel = new double[agentNum][locNum][locNum];
	similarityTransModel = new double[agentNum][locNum][locNum];
	
	for( int ag = 0; ag < SparseGridWorld.NUM_AGENTS; ag++ )
	    for( int s = 0; s < locNum; s++ ) {
		
		stateSimilarity[ag][s] = 0.0;
		
		for( int a = 0; a < actionNum; a++ ) {
				    
		    Nsa[ag][s][a] = 0;
		    Rmodel[ag][s][a] = 0.0;
		
		    for( int sp = 0; sp < locNum; sp++ ) {
		    
			Tmodel[ag][s][a][sp] = 0;	
			similarityModel[ag][s][sp] = 0.0;
			similarityTransModel[ag][s][sp] = 0.0;
		    }
		}
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
	    
	    System.out.println("@NashQTransModel->updateQ: NULL nextState!");
	    
	    return null;
	}
	else if( !bLearning ) {
	    
	    
	    /**
	     * choose a random action
	     */
	    GameAction nextAction = new GameAction();
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		
		nextAction.setAction( agent, random.nextInt(GameAction.NUM_ACTIONS) );
	    }
	    
	    if( curState != null && jointAction != null 
		&& rewards != null ) {
		
		/**
		 * update the statistics
		 */
		for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		    
		    int curLocalState = curState.getLocationID( agent );
		    int nextLocalState = nextState.getLocationID( agent );
		    int locAction = jointAction.getAction( agent );
		    
		    Rmodel[agent][curLocalState][locAction] += rewards[agent];
		    Nsa[agent][curLocalState][locAction] += 1;
		    Tmodel[agent][curLocalState][locAction][nextLocalState] += 1;	
		}
	    }
	    
	    return nextAction;

	}
	else {
	    
	    /**
	     * compute the Nash equilibrium in the next state
	     */
	    double[] nashEquil = computeNE( agentIndex, nextState );
	    
	    /**
	     * get a joint action according to the Nash equilibrium
	     */
	    GameAction nextAction = getJointAction_NE( nashEquil );
	    
	    
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
		double[] nashQValues = getNashQValues( nextState, nashEquil );
		    
		for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		    
		    /**
		     * get the Q-value
		     */
		    double Qsa = getQValue( agent, curState, jointAction );
		    
		    /**
		     * updating rule
		     */
		    Qsa = (1 - ALPHA) * Qsa + ALPHA * (rewards[agent] + GAMMA * nashQValues[agent]);
		    
		    
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
		
		ALPHA *= 0.99988;//0.99988;//58;//0.9975;//0.99958;
		
		/**
		 * maybe we can release some memories
		 */
		nashQValues = null;
	    }
	    
	    /**
	     * maybe we can release some memories
	     */
	    nashEquil = null;
	    
	    return nextAction;
	}
    }
    
    private void readLocalQ()
    {
	
	/**
	 * init member locQs
	 */
	locQs = new double[SparseGridWorld.NUM_AGENTS][SparseGridWorld.NUM_CELLS][GameAction.NUM_ACTIONS];
	
	try {
	    
	    for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
		
		String fileName = "./Rmax_agent"+agentIndex+".txt";
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
    
    private void readLocalModel()
    {
	/**
	 * init member reward model and transition model
	 */
	transRmodel = new double[SparseGridWorld.NUM_AGENTS][SparseGridWorld.NUM_CELLS][GameAction.NUM_ACTIONS];
	transTmodel = new double[SparseGridWorld.NUM_AGENTS][SparseGridWorld.NUM_CELLS][GameAction.NUM_ACTIONS][SparseGridWorld.NUM_CELLS];
	
	try {
	    

	    for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
		
		/**
		 * read the reward model
		 */
		String fileName = "./RewardModel_agent"+agentIndex+".txt";
		BufferedReader rmReader = new BufferedReader(new FileReader(fileName));
		
		int locState = 0;
		int locAct = 0;
		
		String line = "";
		while( (line = rmReader.readLine()) != null) {
			
		    if( line.isEmpty() )
			continue;
			
		    double reward = Double.parseDouble( line );
		    transRmodel[agentIndex][locState][locAct] = reward;
		    
		    locAct++;
		    if( locAct >= GameAction.NUM_ACTIONS ) {
			
			locAct = 0;
			locState++;
			
			if( locState >= SparseGridWorld.NUM_CELLS ) {
			    
			    break;
			}
		    }
		}
		rmReader.close();
		rmReader = null;
		
		
		/**
		 * read the transition model
		 */
		fileName = "./TransitionModel_agent"+agentIndex+".txt";
		BufferedReader tmReader = new BufferedReader(new FileReader(fileName));
		
		int s = 0; 
		int a = 0; 
		int sp = 0;
		line = "";
		while( (line = tmReader.readLine()) != null ) {
		    
		    if( line.isEmpty() )
			continue;
		    
		    double transPro = Double.parseDouble( line );
		    transTmodel[agentIndex][s][a][sp] = transPro;
		    
		    //move to the next item
		    sp++;
		    if( sp >= SparseGridWorld.NUM_CELLS ) {
			
			sp = 0;
			a++;
			
			if( a >= GameAction.NUM_ACTIONS ) {
			    
			    a = 0;
			    s++;
			    
			    if( s >= SparseGridWorld.NUM_CELLS ) {
				
				break;
			    }
			}
		    }
		}
		tmReader.close();
		tmReader = null;
	    }	
	    
	}
	catch(IOException ioe) {
	    
	    ioe.printStackTrace();
	}
    }
    
    
    private void readSimilarity()
    {
	
	try {
	    
	    /**/
	    for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
		
		String fileName = "./similarity_agent"+agentIndex+".txt";
		BufferedReader simReader = new BufferedReader(new FileReader(fileName));
		
		int s = 0;
		String line = "";
		
		while( (line = simReader.readLine()) != null ) {
		    
		    if( line.isEmpty() )
			continue;
		    
		    double similarity = Double.parseDouble( line );
		    stateSimilarity[agentIndex][s] = similarity;
		    
		    s++;
		    if( s >= SparseGridWorld.NUM_CELLS ) {
			
			break;
		    }
		}
		
		simReader.close();
		simReader = null;
		
	    }
	    
	}
	catch (Exception e) {
	    // TODO: handle exception
	}
    }
    
    //should be called by the game executor
    public void currentEpisode( int ep )
    {
	if( bLearning || ep < numExpEpisodes )
	    return;
	else {
	    
	    bLearning = true;
	    
	    /**
	     * compute the model
	     */
	    computeModel();
	    
	    /**
	     * compute valid cells
	     */
	    getValidCells();
	    
	    readSimilarity();
	    
	    /**
	     * compute state similarity for all states
	     */
	    //computeStateSimilarity();
	    
	    
	    /**
	     * start to transfer the model and value function
	     */
	    transfer();
	}
    }
    
    private void computeModel()
    {
	
	for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
	    
	    for( int s = 0; s < SparseGridWorld.NUM_CELLS; s++ ) {
		
		if( invalidLocalState( s, agent ) )
		    continue;
		
		for( int a = 0; a < GameAction.NUM_ACTIONS; a++ ) {
		    
		    Rmodel[agent][s][a] = Rmodel[agent][s][a] / Nsa[agent][s][a];
		    
		    for( int sp = 0; sp < SparseGridWorld.NUM_CELLS; sp++ ) {
			
			Tmodel[agent][s][a][sp] = Tmodel[agent][s][a][sp] / Nsa[agent][s][a];
		    }
		}
		
	    }
	    
	}
    }
    
    private void transfer()
    {
	
	
	/**
	 * then transfer the value function according to the similarity
	 */
	ArrayList<GameState> allStates = SparseGridWorld.getAllValidStates();
	ArrayList<GameAction> jointActions = SparseGridWorld.getAllJointActions();
	
	
	int[] transferCount = new int[2];
	transferCount[0] = 0;
	transferCount[1] = 0;
	
	for( int stateIndex = 0; stateIndex < allStates.size(); stateIndex ++ ) {
		
	    GameState state = allStates.get( stateIndex );
	   
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		
		int locState = state.getLocationID( agent );
		/**
		 * if the distance between the same states 
		 * in two different models is small (which means they are similar)
		 * then we transfer the value functions
		 */
		if( stateSimilarity[agent][locState] < transCndValues[agent] ) {
		    
		    transferCount[agent] += 1;
		    
		    /**
		     * for all joint actions 
		     */
		    for( int jntActIndex = 0; jntActIndex < jointActions.size(); jntActIndex++ ) {
			    
			GameAction jntAction = jointActions.get( jntActIndex );
			
			int locAction = jntAction.getAction( agent );
			    
			double locQValue = locQs[agent][locState][locAction];
			   
			setQValue( agent, state, jntAction, locQValue );  
			
		    }
		}
		else {
		    
		    for( int jntActIndex = 0; jntActIndex < jointActions.size(); jntActIndex++ ) {
			    
			GameAction jntAction = jointActions.get( jntActIndex );
			   
			setQValue( agent, state, jntAction, 20.0 );  
			
		    } 
		}
		
	    }
		
	}
	
	System.out.println("Transfer Count: "+transferCount[0]+", "+transferCount[1]);
	

    }
    
    
    private boolean invalidLocalState( int locState, int agent ) 
    {
	if( locState < 0 || 
		locState >= SparseGridWorld.NUM_CELLS ) {
	    
	    return true;
	}
	
	/**
	 * if the number of visits to each state-action pair is zero
	 * then we treat this state as invalid state
	 */
	for( int action = 0; action < GameAction.NUM_ACTIONS; action++ ) {
		    
	    if( Nsa[agent][locState][action] > 0 )
		return false;
	}
	
	return true;
    }
    
    private void getValidCells( )
    {
	
	validCellsLists = new ArrayList[2];
	for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) 
	    validCellsLists[agent] = new ArrayList<Integer>();
	
	for( int locState = 0; locState < SparseGridWorld.NUM_CELLS; locState++ ) {
	    
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		
		if( !invalidLocalState( locState, agent ) )
		    validCellsLists[agent].add( locState );
	    }

	}
    }

}
