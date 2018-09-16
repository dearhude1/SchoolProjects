package algorithms;

import gameGridWorld.GameAction;
import gameGridWorld.GameState;
import gameGridWorld.SparseGridWorld;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import drasys.or.mp.Constraint;
import drasys.or.mp.Problem;
import drasys.or.mp.SizableProblemI;
import drasys.or.mp.lp.DenseSimplex;
import drasys.or.mp.lp.LinearProgrammingI;

public class NegoQTransModel extends NegoQ
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
    private double[][] transRmodel;
    
    /**
     * the model of the transition function
     */
    private double[][][] transTmodel;
    
    /**
     * the following three arrays 
     * are used for constructing the real local 
     * model in the MAS
     */
    private double[][] Rmodel;
    private int[][] Nsa;
    private double[][][] Tmodel;
    
    /**
     * for iteratively computing the state similarity
     */
    private double[] stateSimilarity;
    
    /**
     * the distance between each state 
     * in the current model
     */
    private double[][] similarityModel;
    
    /**
     * the distance between each state in 
     * the transfered model
     */
    private double[][] similarityTransModel;
    
    /**
     * the number of exploration episodes 
     * for construting the local model
     */
    private int numExpEpisodes = 100;//100;
    
    private boolean bLearning = false;
    
    
    private double transferConditionValue = 15;//20;//50; //25;//0.05;
    
    private ArrayList<Integer> validCellsList = null;
    
    
    private double[] transCndValues = null;
    
    public NegoQTransModel( int agIndex )
    {
	/**
	 * index is no use for centralized CE-Q
	 */
	super( agIndex );
	
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
	
	Nsa = new int[locNum][actionNum];
	Rmodel = new double[locNum][actionNum];
	Tmodel = new double[locNum][actionNum][locNum];
	stateSimilarity = new double[locNum];
	
	similarityModel = new double[locNum][locNum];
	similarityTransModel = new double[locNum][locNum];
	
	for( int s = 0; s < locNum; s++ ) {
		
	    stateSimilarity[s] = 0.0;
	    
	    for( int a = 0; a < actionNum; a++ ) {
				    
		Nsa[s][a] = 0;
		Rmodel[s][a] = 0.0;    
		    
		for( int sp = 0; sp < locNum; sp++ ) {
		    
		    Tmodel[s][a][sp] = 0;
				
		    similarityModel[s][sp] = 0.0;
		    similarityTransModel[s][sp] = 0.0;
		}
	    }
	}

    }
    
    public NegoQTransModel( int agIndex, double alpha, double gamma, double epsilon )
    {
	super( agIndex, alpha, gamma, epsilon);
	
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
	transCndValues[0] = 16;//9;//1.5;
	transCndValues[1] = 16;//3.0;
	transCndValues[2] = 16;//9;//3.0;
	transCndValues[3] = 16;//3.0;
	
	Nsa = new int[locNum][actionNum];
	Rmodel = new double[locNum][actionNum];
	Tmodel = new double[locNum][actionNum][locNum];
	stateSimilarity = new double[locNum];
	
	similarityModel = new double[locNum][locNum];
	similarityTransModel = new double[locNum][locNum];
	
	for( int s = 0; s < locNum; s++ ) {
		
	    stateSimilarity[s] = 0.0;
	    
	    for( int a = 0; a < actionNum; a++ ) {
				    
		Nsa[s][a] = 0;
		Rmodel[s][a] = 0.0;    
		    
		for( int sp = 0; sp < locNum; sp++ ) {
		    
		    Tmodel[s][a][sp] = 0;
				
		    similarityModel[s][sp] = 0.0;
		    similarityTransModel[s][sp] = 0.0;
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
    public void updateQ_NegoQ( GameState curState, GameAction jointAction, 
	    double[] rewards, GameState nextState, GameAction optAction )
    {
	
	if( nextState == null ) {
	    
	    System.out.println("@NegoQTransModel->updateQ: NULL nextState!");
	    
	    return;
	}
	else if( !bLearning ) {
	    
	    /**
	     * mark a visit
	     */
	    visit( curState, jointAction );
	    
	    /**
	     * choose a random action
	     */
	    GameAction nextAction = new GameAction();
	    nextAction.setAction( agentIndex, random.nextInt(GameAction.NUM_ACTIONS) );
	    
	    if( curState != null && jointAction != null 
		&& rewards != null ) {
		
		/**
		 * update the statistics
		 */
		int curLocalState = curState.getLocationID( agentIndex );
		int nextLocalState = nextState.getLocationID( agentIndex );
		int locAction = jointAction.getAction( agentIndex );
		    
		Rmodel[curLocalState][locAction] += rewards[agentIndex];
		Nsa[curLocalState][locAction] += 1;
		Tmodel[curLocalState][locAction][nextLocalState] += 1;
	    }

	}
	else {
	    
	    /**
	     * mark a visit
	     */
	    visit( curState, jointAction );
		
	    /**
	     * only update the Q-table of this agent
	     */
	    double Qsa = getQValue( agentIndex, curState, jointAction );
	    double equilValue = getQValue( agentIndex, nextState, optAction );
		
	    //Qsa = ( 1 - ALPHA ) * Qsa + ALPHA * ( rewards[agentIndex] + GAMMA * equilValue );
		
	    /**
	     * variable learning rate
	     */
	    double alpha = getVariableAlpha( curState, jointAction );
	    Qsa = (1 - alpha) * Qsa + alpha * ( rewards[agentIndex] + GAMMA * equilValue );
	    
	    
	    setQValue( agentIndex, curState, jointAction, Qsa );
		
		
	    /**
	     * alpha decay??
	     */
	    //ALPHA *= 0.9958;//0.9958;//0.99958;//
	}
    }
   
    
    
    public static GameAction negotiation( ArrayList<NegoQTransModel> agents, GameState gameState, 
	    boolean transModel )
    {
	if( gameState == null || agents == null ) {
	    
	    System.out.println("@NegoQ->negotiation: NULL Parameters!");
	    return null;
	}
	
	if( !agents.get(0).isLearning() ) {
	    
	    GameAction retAction = new GameAction();
	    Random rnd = new Random();
	    for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		
		retAction.setAction( agent, rnd.nextInt(GameAction.NUM_ACTIONS) );
	    }
	    
	    return retAction;
	}
	
	/**
	 * negotiation for pure strategy Nash equilibria
	 */
	ArrayList<GameAction>[] maxSets = new ArrayList[SparseGridWorld.NUM_AGENTS];
	for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
	    
	    maxSets[agentIndex] = agents.get( agentIndex ).getMaxSet( gameState );
	}
	/**
	 * we can let only one agent compute NE
	 * and then tell the others
	 */
	agents.get( 0 ).findNEs( maxSets );
	ArrayList<GameAction> neActions = agents.get( 0 ).nashEquilActions;
	for( int neIndex = 0; neIndex < neActions.size(); neIndex++ ) {
	    
	    GameAction neAction = neActions.get( neIndex );
	    for( int agentIndex = 1; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
		    
		agents.get( agentIndex ).messageNE( neAction );
	    }
	}
	
	/**
	 * if there exist Nash equilibira 
	 * then find NSEDAs,
	 * or find meta equilibria
	 */
	if( agents.get(0).existsNE() ) {
	    
	    ArrayList<GameAction>[] partDmSets = new ArrayList[SparseGridWorld.NUM_AGENTS];
	    for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
		
		partDmSets[agentIndex] = agents.get( agentIndex ).getPartiallyDominatingSet( gameState );
	    }
	    agents.get( 0 ).findNSEDAs( partDmSets );
	    ArrayList<GameAction> nsedaActions = agents.get(0).nsedaActions;
	    for( int jntActIndex = 0; jntActIndex < nsedaActions.size(); jntActIndex++ ) {
		
		GameAction nsdeaAction = nsedaActions.get( jntActIndex );
		for( int agentIndex = 1; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
			
		    agents.get( agentIndex ).messageNSEDA(nsdeaAction);
		}
	    }
	}
	else {
	    
	    /**
	     * for 2-agent grid-world game
	     * we first find symmetric meta equilibria
	     */
	    ArrayList<GameAction>[] possSymmSets = new ArrayList[SparseGridWorld.NUM_AGENTS];
	    for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
	    
		possSymmSets[agentIndex] = agents.get(agentIndex).getPossibleSymmEquilSet( gameState );
	    }
	    agents.get( 0 ).findSymmEquils( possSymmSets );
	    ArrayList<GameAction> symmEqActions = agents.get(0).symmMetaEquilActions;
	    for( int jntActIndex = 0; jntActIndex < symmEqActions.size(); jntActIndex++ ) {
		
		GameAction symmEq = symmEqActions.get( jntActIndex );
		for( int agentIndex = 1; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
			
		    agents.get( agentIndex ).messageSymmEq(symmEq);
		}
	    }

	    /**
	     * find a meta equilibrium
	     */
	    if( !agents.get(0).existsSymmMetaEquil() ) {
		
		/**
		 * we only find meta equilibrium from complete games
		 */
		ArrayList<String> indices = new ArrayList<String>();
		for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
			    
		    indices.add(String.valueOf(agentIndex));
		}
		String[] prefix = new String[SparseGridWorld.NUM_AGENTS];
		Random rnd = new Random();
		for( int index = 0; index < SparseGridWorld.NUM_AGENTS; index++ ) {
			    
		    prefix[index] = indices.remove( rnd.nextInt(indices.size()) );
		}
		    
		/**
		 * then find the set of actions which may be a meta equilibrium 
		 * and find the intersection
		 */
		ArrayList<GameAction>[] possMetaSets = new ArrayList[SparseGridWorld.NUM_AGENTS];
		for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
			
		    possMetaSets[agentIndex] = agents.get( agentIndex ).getPossibleMetaEquil(gameState, prefix);
		}
		agents.get( 0 ).findMetaEquils(possMetaSets);
		ArrayList<GameAction> metaActions = agents.get(0).metaEquilActions; 
		for( int jntActIndex = 0; jntActIndex < metaActions.size(); jntActIndex++ ) {
			
		    GameAction metaAction = metaActions.get( jntActIndex );
		    for( int agentIndex = 1; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
				
			agents.get( agentIndex ).messageMeta(metaAction);
		    }
		}
	    }

	}
	
	/**
	 * then choose one optimal action
	 */
	GameAction[] favorActions = new GameAction[SparseGridWorld.NUM_AGENTS];
	for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
	    
	    favorActions[agentIndex] = agents.get( agentIndex ).myFavoriteAction( gameState );
	}
	
	Random rnd = new Random(); 
	GameAction selectedAction = favorActions[rnd.nextInt(SparseGridWorld.NUM_AGENTS)];
	
	return selectedAction;
	
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
	transRmodel = new double[SparseGridWorld.NUM_CELLS][GameAction.NUM_ACTIONS];
	transTmodel = new double[SparseGridWorld.NUM_CELLS][GameAction.NUM_ACTIONS][SparseGridWorld.NUM_CELLS];
	
	try {
	    
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
		transRmodel[locState][locAct] = reward;
		    
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
		transTmodel[s][a][sp] = transPro;
		    
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
	catch(IOException ioe) {
	    
	    ioe.printStackTrace();
	}
    }
    
    
    private void readSimilarity()
    {
	
	try {
	    
	    String fileName = "./similarity_agent"+agentIndex+".txt";
	    BufferedReader simReader = new BufferedReader(new FileReader(fileName));
		
	    int s = 0;
	    String line = "";
		
	    while( (line = simReader.readLine()) != null ) {
		    
		if( line.isEmpty() )
		    continue;
		    
		double similarity = Double.parseDouble( line );
		stateSimilarity[s] = similarity;
		    
		s++;
		if( s >= SparseGridWorld.NUM_CELLS ) {
			
		    break;
		}
	    }
		
	    simReader.close();
	    simReader = null;
	    
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
	for( int s = 0; s < SparseGridWorld.NUM_CELLS; s++ ) {
	 
	    
	    if( invalidLocalState(s) )
		continue;
	    
	    for( int a = 0; a < GameAction.NUM_ACTIONS; a++ ) {
		
		Rmodel[s][a] = Rmodel[s][a] / Nsa[s][a];
		
		for( int sp = 0; sp < SparseGridWorld.NUM_CELLS; sp++ ) {
		    
		    Tmodel[s][a][sp] = Tmodel[s][a][sp] / Nsa[s][a];
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
	
	int transferCount = 0;
	for( int stateIndex = 0; stateIndex < allStates.size(); stateIndex ++ ) {
		
	    GameState state = allStates.get( stateIndex );
	   
	    int locState = state.getLocationID( agentIndex );
	    /**
	     * if the distance between the same states 
	     * in two different models is small (which means they are similar)
	     * then we transfer the value functions
	     */
	    if( stateSimilarity[locState] < transCndValues[agentIndex] ) {
		    
		
		transferCount++;
		
		/**
		 * for all joint actions 
		 */
		for( int jntActIndex = 0; jntActIndex < jointActions.size(); jntActIndex++ ) {
		    
		    GameAction jntAction = jointActions.get( jntActIndex );
		    
		    int locAction = jntAction.getAction( agentIndex );
			    
		    double locQValue = locQs[agentIndex][locState][locAction];
			   
		    setQValue( agentIndex, stateIndex, jntActIndex, locQValue );    
		}
	    }
	    else {
		    
		    for( int jntActIndex = 0; jntActIndex < jointActions.size(); jntActIndex++ ) {
			    
			GameAction jntAction = jointActions.get( jntActIndex );
			   
			setQValue( agentIndex, stateIndex, jntActIndex, 20.0 );  
			
		    } 
		}
	}
	
	System.out.println("TransferCount: "+transferCount);
	

    }
    
    
    private boolean invalidLocalState( int locState ) 
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
		    
	    if( Nsa[locState][action] > 0 )
		return false;
	}
	
	return true;
    }
    
    private void getValidCells()
    {
	
	validCellsList = new ArrayList<Integer>();
	
	for( int locState = 0; locState < SparseGridWorld.NUM_CELLS; locState++ ) {
	    
	    if( !invalidLocalState(locState) )
		validCellsList.add( locState );
	}
    }

    public boolean isLearning()
    {
	return bLearning;
    }
    
    
}
