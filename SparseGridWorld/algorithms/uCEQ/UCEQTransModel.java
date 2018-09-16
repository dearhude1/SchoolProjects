package algorithms.uCEQ;

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

public class UCEQTransModel extends UCEQ
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
    
    
    private double transferConditionValue = 2.0;//10;//10;//10;//50; //25;//0.05;
    private double[] transCndValues = null;
    
    private ArrayList<Integer>[] validCellsLists = null;
    
    public UCEQTransModel()
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
    
    public UCEQTransModel( double alpha, double gamma, double epsilon )
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
	transCndValues[0] = 13.0;//13.0;//1.5;//9.0;//4.3;
	transCndValues[1] = 13.0;//13.0;//3.0;//3.0;//9.0;//5.0;
	
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
	    
	    System.out.println("@uCEQ_TransPolicy->updateQ: NULL nextState!");
	    
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
	     * compute the correlated equilibrium in the next state
	     */
	    double[] correlEquil = computeCE( agentIndex, nextState );
	    
	    /**
	     * get a joint action according to the correlated equilibrium
	     */
	    GameAction nextAction = getJointAction_CE( correlEquil );
	    
	    
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
		
		ALPHA *= 0.9991;//0.9991;//0.998;//0.99;//0.995;//0.99985;//0.99988;//58;//0.9975;//0.99958;
		
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
	    
	    /**
	    for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
		
		String fileName = "./simiModel_agent"+agentIndex+".txt";
		BufferedReader simReader = new BufferedReader(new FileReader(fileName));
		
		int s = 0;
		int sp = 0;
		String line = "";
		
		while( (line = simReader.readLine()) != null ) {
		    
		    if( line.isEmpty() )
			continue;
		    
		    double similarity = Double.parseDouble( line );
		    similarityModel[agentIndex][s][sp] = similarity;
		    
		    sp++;
		    if( sp >= SparseGridWorld.NUM_CELLS ) {
			
			sp = 0; 
			s++;
			
			if( s >= SparseGridWorld.NUM_CELLS )
			    break;
		    }
		}
		simReader.close();
	    }
	    
	    
	    for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
		
		String fileName = "./simiTransModel_agent"+agentIndex+".txt";
		BufferedReader simReader = new BufferedReader(new FileReader(fileName));
		
		int s = 0;
		int sp = 0;
		String line = "";
		
		while( (line = simReader.readLine()) != null ) {
		    
		    if( line.isEmpty() )
			continue;
		    
		    double similarity = Double.parseDouble( line );
		    similarityTransModel[agentIndex][s][sp] = similarity;
		    
		    sp++;
		    if( sp >= SparseGridWorld.NUM_CELLS ) {
			
			sp = 0; 
			s++;
			
			if( s >= SparseGridWorld.NUM_CELLS )
			    break;
		    }
		}
		simReader.close();
	    }
	    */
	    
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
	    
	    long startTime = System.nanoTime();
	    /**
	     * compute state similarity for all states
	     */
	    //computeStateSimilarity();
	    
	    computeStateSimilarityNSR();
	    
	    //long endTime = System.nanoTime();
	    //System.out.println("Time: "+(endTime-startTime)+" ");
	    
	    readSimilarity();
	    
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
			
			//max value??
			setQValue( agent, state, jntAction, 20.0 );  
			
		    }
		}
		
	    }
		
	}
	
	//System.out.println("Transfer Count: "+transferCount[0]+", "+transferCount[1]);
	

    }
    
    private void computeStateSimilarityNSR()
    {
	
	long startTime = System.nanoTime();
	
	int N = 15;
	for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
	    
	    int validCellNum = validCellsLists[agent].size();
	
	    for( int n = 0; n < N; n++ ) {
		
		for( int cellIndex = 0; cellIndex < validCellNum; cellIndex++ ) {
			    
		    int s = validCellsLists[agent].get( cellIndex );
		    double Rs = 0;
			    
		    for( int cellIndex_p = 0; cellIndex_p < validCellNum; cellIndex_p++ ) {
				
			int sp = validCellsLists[agent].get( cellIndex_p );
			double Rsp = random.nextDouble();
			double Rspp = random.nextDouble();
			for( int action = 0; action < GameAction.NUM_ACTIONS; action++ ) {
			    
				Rs += GAMMA * Tmodel[agent][s][action][sp] * (Rsp - Rspp);
			}
		    }
		}	
	    }
	}
	
	long endTime = System.nanoTime();
	System.out.println("During Time: "+(endTime-startTime)/1000);
	
    }
    
    private void computeStateSimilarity()
    {
	
	/**
	 * compute the state similarity in the current model
	 */
	while ( true ) {
	   
	    System.out.println("Computing Similarity in Current Model");
	    
	    double maxDelta = Double.NEGATIVE_INFINITY;
	    double delta = 0.0;
	    
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		
		int validCellNum = validCellsLists[agent].size();
		
		for( int cellIndex = 0; cellIndex < validCellNum; cellIndex++ ) {
		    
		    int s = validCellsLists[agent].get( cellIndex );
		    
		    for( int cellIndex_p = 0; cellIndex_p < validCellNum; cellIndex_p++ ) {
			
			int sp = validCellsLists[agent].get( cellIndex_p );
			
	    		double similarity = computeStateSimilarity( agent, s, sp, 
	    			similarityModel, Rmodel, Tmodel );
	    		
	    		delta = Math.abs( similarity - similarityModel[agent][s][sp] );
	    		similarityModel[agent][s][sp] = similarity;
	    		
	    		if( delta > maxDelta ) 
	    		    maxDelta = delta;
		    }
		}
	    }
	    
	    //avoid repeated computation
	    /**
	    for( int cellIndex = validCellNum-1; cellIndex >= 0; cellIndex-- ) {
		
		int s = validCellsList.get( cellIndex );
		
		for( int cellIndex_p = cellIndex-1; cellIndex_p >= 0; cellIndex_p-- ) {
		    
		    int sp = validCellsList.get( cellIndex_p );
		    
		    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
			
			similarityModel[agent][s][sp] = similarityModel[agent][sp][s];
		    }
			
		}
	    }
	    */
	    
	    System.out.println("Similarity: "+similarityModel[0][0][0]);
	    System.out.println("Max Delta: "+ maxDelta);
	    
	    if( maxDelta < 10.0 ) 
		break;
	}
	
	
	/**
	 * compute the state similarity in the transfered model
	 */
	while ( true ) {
	   
	    System.out.println("Computing Similarity in Transfered Model");
	    
	    double maxDelta = Double.NEGATIVE_INFINITY;
	    double delta = 0.0;
	    
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		
		int validCellNum = validCellsLists[agent].size();
		
		for( int cellIndex = 0; cellIndex < validCellNum; cellIndex++ ) {
		    
		    int s = validCellsLists[agent].get( cellIndex );
		    
		    for( int cellIndex_p = 0; cellIndex_p < validCellNum; cellIndex_p++ ) {
			
			int sp = validCellsLists[agent].get( cellIndex_p );
			
	    		double similarity = computeStateSimilarity( agent, s, sp, 
	    			similarityTransModel, transRmodel, transTmodel );
	    		
	    		delta = Math.abs( similarity - similarityTransModel[agent][s][sp] );
	    		similarityTransModel[agent][s][sp] = similarity;
	    		
	    		if( delta > maxDelta ) 
	    		    maxDelta = delta;
		    }
		}
	    }
	    
	    //avoid repeated computation
	    /**
	    for( int cellIndex = validCellNum-1; cellIndex >= 0; cellIndex-- ) {
		
		int s = validCellsList.get( cellIndex );
		
		for( int cellIndex_p = cellIndex-1; cellIndex_p >= 0; cellIndex_p-- ) {
		    
		    int sp = validCellsList.get( cellIndex_p );
		    
		    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
			
			similarityTransModel[agent][s][sp] = similarityTransModel[agent][sp][s];
		    }
			
		}
	    }
	    */
	    
	    System.out.println("Similarity: "+similarityTransModel[0][0][0]);
	    System.out.println("Max Delta: "+ maxDelta);
	    
	    if( maxDelta < 10.0 ) 
		break;
	}
	
	
	/**
	 * compute the similarity of each state
	 */
	
	
	for( int s = 0; s < SparseGridWorld.NUM_CELLS; s++ ) {
	    
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		
		if( invalidLocalState( s, agent ) )
		    continue;
		
		double simi = 0.0;
		
		int validNum = validCellsLists[agent].size();
		for( int cellIndex = 0; cellIndex < validNum; cellIndex++ ) {
		    
		    int sp = validCellsLists[agent].get( cellIndex );
		    
		    double modelSimi = similarityModel[agent][s][sp];
		    double transModelSimi = similarityTransModel[agent][s][sp];
		    double deltaSimi = modelSimi - transModelSimi;
		    
		    simi += deltaSimi * deltaSimi;
		}
		
		if( validNum == 0 )
		    simi = 10000;
		else
		    simi = Math.sqrt( simi ) / validNum;
		
		stateSimilarity[agent][s] = simi;
	    }
	    
	}
	
	
	/**
	 * write the similarity to files
	 *
	try {
	    
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		
		String fileName = "./simiModel_agent"+agent+".txt";
		BufferedWriter simWriter = new BufferedWriter(new FileWriter(fileName));
		
		for( int s = 0; s < SparseGridWorld.NUM_CELLS; s++ ) 
		    for( int sp = 0; sp < SparseGridWorld.NUM_CELLS; sp++ ) {
			
			String line = String.valueOf(similarityModel[agent][s][sp]);
			simWriter.write( line );
			simWriter.newLine();
		    }
	    
		simWriter.close();
	    }
	    
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		
		String fileName = "./simiTransModel_agent"+agent+".txt";
		BufferedWriter simWriter = new BufferedWriter(new FileWriter(fileName));
		
		for( int s = 0; s < SparseGridWorld.NUM_CELLS; s++ ) 
		    for( int sp = 0; sp < SparseGridWorld.NUM_CELLS; sp++ ) {
			
			String line = String.valueOf(similarityTransModel[agent][s][sp]);
			simWriter.write( line );
			simWriter.newLine();
		    }
	    
		simWriter.close();
	    }
	    
	    
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		
		String fileName = "./similarity_agent"+agent+".txt";
		BufferedWriter simWriter = new BufferedWriter(new FileWriter(fileName));
		
		for( int s = 0; s < SparseGridWorld.NUM_CELLS; s++ ) {
		 
		    String line = String.valueOf( stateSimilarity[agent][s] );
		    simWriter.write( line );
		    simWriter.newLine();
		}
	    
		simWriter.close();
	    }
	}
	catch (Exception e) {
	    // TODO: handle exception
	    e.printStackTrace();
	}
	*/
    }
    
    /**
    private boolean invalidLocalState( int locState ) 
    {
	if( locState < 0 || 
		locState >= SparseGridWorld.NUM_CELLS ) {
	    
	    return true;
	}
	
	for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
	    for( int action = 0; action < GameAction.NUM_ACTIONS; action++ ) {
	    
		if( Nsa[agent][locState][action] > 0 )
		    return false;
	    }
	}
	
	return true;
    }
    */
    
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
    
    /**
     * 
     * @param agent
     * @param modeledState: state in the modeled state set
     * @param transferState: state in the state set of the transfered model
     * @return
     */
    private double computeStateSimilarity( int agent, int s, int sp, double[][][] simiMatrix, 
	    double[][][] rewardModel, double[][][][] transitionModel ) 
    {
	if( s < 0 || s >= SparseGridWorld.NUM_CELLS ||
		sp < 0 || sp >= SparseGridWorld.NUM_CELLS ) {
	    
	    System.out.println("UCEQTransModel->computeStateSimilarity: Wrong State");
	    return 0;
	}
	
	if( s == sp ) 
	    return 0.0;
	
	double similarity = 0;
	double maxSim = Double.NEGATIVE_INFINITY;
	
	boolean change = false;
	double[] ktd = new double[GameAction.NUM_ACTIONS];
	double[] rwds = new double[GameAction.NUM_ACTIONS]; 
	for( int action = 0; action < GameAction.NUM_ACTIONS; action++ ) {
	    
	    //the reward distance
	    double reward_s = rewardModel[agent][s][action];
	    double reward_sp = rewardModel[agent][sp][action];
	    double rwdAbs = Math.abs( reward_s - reward_sp );
	    
	    //Kantorovich distance
	    double kantoroDis = kantorovichDistance( agent, s, sp, action, 
		    simiMatrix, rewardModel, transitionModel );
	    
	    similarity = rwdAbs + GAMMA * kantoroDis;
	    
	    ktd[action] = kantoroDis;
	    rwds[action] = rwdAbs;

	    
	    if( maxSim < similarity ) {
		
		maxSim = similarity;
		
		change = true;
	    }
	}
	
	/**/
	if( !change ) {
	    
	    System.out.println("Infinity ret value: "+maxSim);
	    
	    for( int action = 0; action < GameAction.NUM_ACTIONS; action++ ) {
		
		System.out.print(" "+ktd[action]+","+rwds[action]+" ");
		
		if( ktd[action] == Double.NaN ) {
			    
		    System.out.println("Not a number, fuck!");
		}
	    }
	    System.out.println();
	    
	}
	
	//System.out.println("Similarity: "+maxSim);
	
	return maxSim;
    }
    
    private double kantorovichDistance( int agent, int s, 
	    int sp, int action, double[][][] simiMatrix, 
	    double[][][] rewardModel, double[][][][] transitionModel )
    {
	/**
	 * the distance between the same state is 0
	 */
	if( s == sp )
	    return 0.0;
	
	double retValue = 0;
	
	/**
	 * find the reachable states from state s and sp
	 * this can dramatically simplify the linear programming
	 */
	ArrayList<Integer> reachable_s = new ArrayList<Integer>();
	ArrayList<Integer> reachable_sp = new ArrayList<Integer>(); 
	
	for( int locState = 0; locState < SparseGridWorld.NUM_CELLS; locState++ ) {
	    
	    if( transitionModel[agent][s][action][locState] > 0 )
		reachable_s.add( locState );
	}
	for( int locState = 0; locState < SparseGridWorld.NUM_CELLS; locState++ ) {
	    
	    if( transitionModel[agent][sp][action][locState] > 0 )
		reachable_sp.add( locState );
	}
	
	int varNum = reachable_s.size() * reachable_sp.size();
	int consNum = varNum + reachable_s.size() + reachable_sp.size();
	int num_sk = reachable_s.size();
	int num_st = reachable_sp.size();
	
	/*
	int validCellNum = validCellsList.size();
	int varNum = validCellNum * validCellNum;
	int consNum = varNum + 2 * validCellNum;
	*/
	
	SizableProblemI problem = new Problem( consNum, varNum );
	problem.getMetadata().put("lp.isMinimize", "true");
	
	double[] solutions = new double[varNum];
	
	try {
	    
	    
	    /**
	     * 1. create the variable and objective
	     *
	    for( int cellIndex = 0; cellIndex < validCellNum; cellIndex++ ) {
		
		int sk = validCellsList.get( cellIndex );
		
		for( int cellIndex_p = 0; cellIndex_p < validCellNum; cellIndex_p++ ) {
		    
		    int st = validCellsList.get( cellIndex_p );
		    
		    String variableName = ""+sk+":"+st;
		    double similarity = simiMatrix[agent][sk][st];
		    
		    problem.newVariable(variableName).setObjectiveCoefficient( similarity );
		}
	    }
	    */
	    for( int cellIndex = 0; cellIndex < num_sk; cellIndex++ ) {
		
		int sk = reachable_s.get( cellIndex );
		
		for( int cellIndex_p = 0; cellIndex_p < num_st; cellIndex_p++ ) {
		    
		    int st = reachable_sp.get( cellIndex_p );
		    
		    String variableName = ""+sk+":"+st;
		    double similarity = simiMatrix[agent][sk][st];
		    
		    problem.newVariable(variableName).setObjectiveCoefficient( similarity );
		}
	    }
	    
	    /**
	     * 2. the lower bounds of each variable
	     */
	    for( int varIndex = 0; varIndex < varNum; varIndex++ ) {
		
		String consName = "aboveZero"+varIndex;
		int consIndex = problem.newConstraint(consName).
		    	setType(Constraint.GREATER).setRightHandSide(0.0).getRowIndex();
		problem.setCoefficientAt(consIndex, varIndex, 1.0);
	    }
	    
	    /**
	     * 3. other constraints
	     * a. for each sk
	     * b. for each st
	     *
	    for( int cellIndex = 0; cellIndex < validCellNum; cellIndex++ ) {
		
		int sk = validCellsList.get( cellIndex );
		double rightHandSide = transitionModel[agent][s][action][sk];
		
		
		String consName = "sk"+sk;
		problem.newConstraint(consName).
			setType(Constraint.EQUAL).setRightHandSide(rightHandSide);
		
		for( int cellIndex_p = 0; cellIndex_p < validCellNum; cellIndex_p++ ) {
		    
		    int st = validCellsList.get( cellIndex_p );
		    
		    String variableName = ""+sk+":"+st;
		    problem.setCoefficientAt(consName, variableName, 1.0);
		}
	    }
	    
	    for( int cellIndex = 0; cellIndex < validCellNum; cellIndex++ ) {
		
		int st = validCellsList.get( cellIndex );
		double rightHandSide = transitionModel[agent][sp][action][st];
		
		
		String consName = "st"+st;
		problem.newConstraint(consName).
			setType(Constraint.EQUAL).setRightHandSide(rightHandSide);
		
		for( int cellIndex_p = 0; cellIndex_p < validCellNum; cellIndex_p++ ) {
		    
		    int sk = validCellsList.get( cellIndex_p );
		    
		    String variableName = ""+sk+":"+st;
		    problem.setCoefficientAt(consName, variableName, 1.0);
		}
	    }
	    */
	    
	    for( int cellIndex = 0; cellIndex < num_sk; cellIndex++ ) {
		
		int sk = reachable_s.get( cellIndex );
		double rightHandSide = transitionModel[agent][s][action][sk];
		
		
		String consName = "sk"+sk;
		problem.newConstraint(consName).
			setType(Constraint.EQUAL).setRightHandSide(rightHandSide);
		
		for( int cellIndex_p = 0; cellIndex_p < num_st; cellIndex_p++ ) {
		    
		    int st = reachable_sp.get( cellIndex_p );
		    
		    String variableName = ""+sk+":"+st;
		    problem.setCoefficientAt(consName, variableName, 1.0);
		}
	    }
	    
	    for( int cellIndex = 0; cellIndex < num_st; cellIndex++ ) {
		
		int st = reachable_sp.get( cellIndex );
		double rightHandSide = transitionModel[agent][sp][action][st];
		
		
		String consName = "st"+st;
		problem.newConstraint(consName).
			setType(Constraint.EQUAL).setRightHandSide(rightHandSide);
		
		for( int cellIndex_p = 0; cellIndex_p < num_sk; cellIndex_p++ ) {
		    
		    int sk = reachable_s.get( cellIndex_p );
		    
		    String variableName = ""+sk+":"+st;
		    problem.setCoefficientAt(consName, variableName, 1.0);
		}
	    }
	    
	    /**
	     * 4. solve this linear programming
	     */
	    LinearProgrammingI iLP;
	    iLP = new DenseSimplex(problem);
	    double ans = iLP.solve();
	    
	    
	    if( ans > Double.NEGATIVE_INFINITY ) {
		
		//solutions = iLP.getSolution().getArray();
		retValue = ans;
	    }
	    
	}
	catch (Exception e) {
	    // TODO: handle exception
	    
	   //e.printStackTrace();  
	}

	
	return retValue;
	
    }
    
    /**
     * check again and again!!!
     * 
     * compute the Kantorovich distance between two transition distribution
     *
    private double kantorovichDistance( int agent, int modeledState, 
	    int transferState, int action )
    {
	double retValue = -1;
	
	try {
	    
	    IloCplex kantoDisCplex = new IloCplex();
	    
	    kantoDisCplex.setParam(IloCplex.Param.WorkMem, 2000);
	    
	    int varNum = SparseGridWorld.NUM_CELLS * SparseGridWorld.NUM_CELLS;
	    double[] lowBounds = new double[varNum];
	    double[] uppBounds = new double[varNum];
	    for( int varIndex = 0; varIndex < varNum; varIndex++ ) {
		
		lowBounds[varIndex] = 0.0;
		uppBounds[varIndex] = Double.POSITIVE_INFINITY;
	    }
	    IloNumVar[] p = kantoDisCplex.numVarArray(varNum, lowBounds, uppBounds);
	    
	    
	    for( int sk = 0; sk < SparseGridWorld.NUM_CELLS; sk++ ) {
		
		double transPro = Tmodel[agent][modeledState][action][sk];
		double[] coeff = new double[varNum];
		for( int coeffIndex = 0; coeffIndex < varNum; coeffIndex++ ) {
		    
		    //check it
		    if( (coeffIndex / SparseGridWorld.NUM_CELLS) == sk )
			coeff[coeffIndex] = 1.0;
		    else
			coeff[coeffIndex] = 0.0;
		}

		IloNumExpr sumExpr = kantoDisCplex.scalProd(coeff, p);
		kantoDisCplex.addEq( sumExpr, transPro );
	    }
	    
	    for( int st = 0; st < SparseGridWorld.NUM_CELLS; st++ ) {
		
		double transPro = transTmodel[agent][transferState][action][st];
		double[] coeff = new double[varNum];
		for( int coeffIndex = 0; coeffIndex < varNum; coeffIndex++ ) {
		    
		    if( (coeffIndex % SparseGridWorld.NUM_CELLS) == st )
			coeff[coeffIndex] = 1.0;
		    else
			coeff[coeffIndex] = 0.0;
		}
		IloNumExpr sumExpr = kantoDisCplex.scalProd(coeff, p);
		kantoDisCplex.addEq( sumExpr, transPro );
	    }
	    
	    IloNumExpr objExpr = kantoDisCplex.constant(0);
	    for( int sk = 0; sk < SparseGridWorld.NUM_CELLS; sk++ ) {
		
		//ignore the invalid states?
		if( invalidLocalState( sk ) )
		    continue;
		
		for( int st = 0; st < SparseGridWorld.NUM_CELLS; st++ ) {
		    
		  //ignore the invalid states?
		    if( invalidLocalState( st ) )
			continue;
		    
		    double similarity = stateSimilarity[agent][sk][st];
		    int varIndex = sk * SparseGridWorld.NUM_CELLS + st;
		    objExpr = kantoDisCplex.sum( objExpr, kantoDisCplex.prod(similarity, p[varIndex]) );
		}
	    }
	    kantoDisCplex.add(kantoDisCplex.minimize( objExpr ) );
	    
	    if( kantoDisCplex.solve() ) {
		
		//solution = new double[varNum]; 
		//nashCP.getValues( p, solution );
		
		retValue = kantoDisCplex.getValue(objExpr);
		System.out.println("Return value: "+retValue);
	    }
	    kantoDisCplex.end();
	    
	    //release the memory??
	    kantoDisCplex = null;
	    
	}
	catch (IloException iloE) {
	    // TODO: handle exception
	    System.err.println("Concert exception '" + iloE + "' caught");
	}
	
	return retValue;

    }
    */
}
