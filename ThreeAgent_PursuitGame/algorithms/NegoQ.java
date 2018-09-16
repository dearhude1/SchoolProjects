package algorithms;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Random;

import gamePursuitGame.GameAction;
import gamePursuitGame.GameState;
import gamePursuitGame.PursuitGame;

public class NegoQ extends MARL
{
    /**
     * store all pure strategy Nash equilibria 
     * in a certain state
     */
    private ArrayList<GameAction> nashEquilActions;
    
    /**
     * store all NSEDAs in a certain state
     */
    private ArrayList<GameAction> nsedaActions;
    
    private ArrayList<GameAction> symmMetaEquilActions;
    
    /**
     * store some meta equilibria in a certain state
     */
    private ArrayList<GameAction> metaEquilActions;
    
    /**
     * we do not consider the action of the prey
     * in this cooperative task
     * 
     * thus, 0 means agent1
     * 1 means agent2
     * 
     * member agentIndex remains the same
     * we should use (agentIndex-1) in NegoQ
     */
    private double[][][][][][] negoQs;
    
    public NegoQ( int agIndex )
    {
	super( agIndex );
	
	/**
	 * init the Q-table
	 * for centralized algorithms 
	 * the Q-tables can be initialized randomly
	 */
	int locNum = PursuitGame.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	
	/**
	 * init Q-tables again
	 */
	negoQs = new double[PursuitGame.NUM_AGENTS-1][locNum][locNum][locNum][actionNum][actionNum];
	for( int agentIndex = 0; agentIndex < PursuitGame.NUM_AGENTS-1; agentIndex++ )
	    for( int s1 = 0; s1 < locNum; s1++ )
		for( int s2 = 0; s2 < locNum; s2++ )
		    for( int s3 = 0; s3 < locNum; s3++ )
			for( int a1 = 0; a1 < actionNum; a1++ )
			    for( int a2 = 0; a2 < actionNum; a2++ ) {
				
				negoQs[agentIndex][s1][s2][s3][a1][a2] = (Math.random() - 0.5) / 10.0;
			    }
	
	nashEquilActions = new ArrayList<GameAction>();
	nsedaActions = new ArrayList<GameAction>();
	symmMetaEquilActions = new ArrayList<GameAction>();
	metaEquilActions = new ArrayList<GameAction>();
	
    }
    
    
    public NegoQ( int agIndex, double alpha, double gamma, double epsilon )
    {
	super( agIndex, alpha, gamma, epsilon );
	
	/**
	 * init the Q-table
	 * for centralized algorithms 
	 * the Q-tables can be initialized randomly
	 */
	int locNum = PursuitGame.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	
	/**
	 * init Q-tables again
	 */
	negoQs = new double[PursuitGame.NUM_AGENTS-1][locNum][locNum][locNum][actionNum][actionNum];
	for( int agentIndex = 0; agentIndex < PursuitGame.NUM_AGENTS-1; agentIndex++ )
	    for( int s1 = 0; s1 < locNum; s1++ )
		for( int s2 = 0; s2 < locNum; s2++ )
		    for( int s3 = 0; s3 < locNum; s3++ )
			for( int a1 = 0; a1 < actionNum; a1++ )
			    for( int a2 = 0; a2 < actionNum; a2++ ) {
				
				negoQs[agentIndex][s1][s2][s3][a1][a2] = (Math.random() - 0.5) / 10.0;
			    }
	
	nashEquilActions = new ArrayList<GameAction>();
	nsedaActions = new ArrayList<GameAction>();
	symmMetaEquilActions = new ArrayList<GameAction>();
	metaEquilActions = new ArrayList<GameAction>();
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
	if( curState == null || nextState == null || 
		jointAction == null || optAction == null ||
		rewards == null ) {
	    
	    System.out.println("@NegoQ->updateQ_NegoQ: NULL Parameters!");
	    
	    return;
	}
	
	/**
	 * mark a visit
	 */
	visit( curState, jointAction );
	
	/**
	 * only update the Q-table of this agent
	 */
	double Qsa = getQValue( agentIndex, curState, jointAction );
	double equilValue = getQValue( agentIndex, nextState, optAction );
	
	Qsa = ( 1 - ALPHA ) * Qsa + ALPHA * ( rewards[agentIndex] + GAMMA * equilValue );
	
	/**
	 * variable learning rate
	 */
	//double alpha = getVariableAlpha( curState, jointAction );
	//Qsa = (1 - alpha) * Qsa + alpha * ( rewards[agentIndex] + GAMMA * equilValue );
	
	setQValue( agentIndex, curState, jointAction, Qsa );
    }
    
    /**
     * compute the optimal action is a state
     * using the learnt table, not through negotiation
     */
    public GameAction getAction( GameState gameState )
    {
	/**
	 * negotiation for pure strategy Nash equilibria
	 */
	ArrayList<GameAction> maxSet_0 = getMaxSet(gameState, 0);
	ArrayList<GameAction> maxSet_1 = getMaxSet(gameState, 1);
	findNEs(maxSet_0, maxSet_1);
	
	if( existsNE() ) {
	    
	    ArrayList<GameAction> partDmSet_0 = getPartiallyDominatingSet(gameState, 0);
	    ArrayList<GameAction> partDmSet_1 = getPartiallyDominatingSet(gameState, 1);
	    findNSEDAs(partDmSet_0, partDmSet_1);
	}
	else {
	    
	    /**
	     * for 2-agent pursuit game
	     * we first find symmetric meta equilibria
	     */	    
	    ArrayList<GameAction> possSymmSet_0 = getPossibleSymmEquilSet(gameState,0);
	    ArrayList<GameAction> possSymmSet_1 = getPossibleSymmEquilSet(gameState,1);
	    findSymmEquils(possSymmSet_0, possSymmSet_1);
	    
	    if( !existsSymmMetaEquil() ) {
		
		/**
		 * choose one complete game first
		 */
		ArrayList<String> indices = new ArrayList<String>();
		indices.add("0"); indices.add("1");
		String[] prefix = new String[PursuitGame.NUM_AGENTS];
		Random rnd = new Random();
		for( int index = 0; index < PursuitGame.NUM_AGENTS; index++ ) {
			
		    prefix[index] = indices.remove(rnd.nextInt(indices.size()));
		}
		    
		/**
		 * then find the set of actions which may be a meta equilibrium 
		 * and find the intersection
		 */
		ArrayList<GameAction> possMetaSet_0 = getPossibleMetaEquil(gameState, prefix,0);
		ArrayList<GameAction> possMetaSet_1 = getPossibleMetaEquil(gameState, prefix,1);
		findMetaEquils(possMetaSet_0, possMetaSet_1);
	    }
	}
	
	/**
	 * then choose one optimal action
	 */
	GameAction[] favorActions = new GameAction[PursuitGame.NUM_AGENTS];
	favorActions[0] = myFavoriteAction(gameState,0);
	favorActions[1] = myFavoriteAction(gameState,1);
	
	Random rnd = new Random(); 
	GameAction selectedAction = favorActions[rnd.nextInt(PursuitGame.NUM_AGENTS)];
	
	
	nashEquilActions.clear();
	nsedaActions.clear();
	symmMetaEquilActions.clear();
	metaEquilActions.clear();
	
	
	return selectedAction;
    }
    
    public static GameAction negotiation( NegoQ agent_i, NegoQ agent_j, 
	    GameState gameState )
    {
	
	if( gameState == null || agent_i == null || 
		agent_j == null ) {
	    
	    System.out.println("@NegoQ->negotiation: NULL Parameters!");
	    return null;
	}
	
	
	/**
	 * negotiation for pure strategy Nash equilibria
	 */
	ArrayList<GameAction> maxSet_i = agent_i.getMaxSet(gameState);
	ArrayList<GameAction> maxSet_j = agent_j.getMaxSet(gameState);
	agent_i.findNEs(maxSet_i, maxSet_j);
	agent_j.findNEs(maxSet_i, maxSet_j);
	
	
	/**
	 * if there exist Nash equilibira 
	 * then find NSEDAs,
	 * or find meta equilibria
	 */
	if( agent_i.existsNE() ) {
	    
	    
	    ArrayList<GameAction> partDmSet_i = agent_i.getPartiallyDominatingSet(gameState);
	    ArrayList<GameAction> partDmSet_j = agent_j.getPartiallyDominatingSet(gameState);
	    agent_i.findNSEDAs(partDmSet_i, partDmSet_j);
	    agent_j.findNSEDAs(partDmSet_i, partDmSet_j);
	    
	    /**
	     * find EDAs if needed
	     */
	    
	}
	else {
	    
	    
	    /**
	     * for 2-agent grid-world game
	     * we first find symmetric meta equilibria
	     */	    
	    ArrayList<GameAction> possSymmSet_i = agent_i.getPossibleSymmEquilSet(gameState);
	    ArrayList<GameAction> possSymmSet_j = agent_j.getPossibleSymmEquilSet(gameState);
	    agent_i.findSymmEquils(possSymmSet_i, possSymmSet_j);
	    agent_j.findSymmEquils(possSymmSet_i, possSymmSet_j);
	    
	    if( !agent_i.existsSymmMetaEquil() ) {
		
		/**
		 * choose one complete game first
		 */
		ArrayList<String> indices = new ArrayList<String>();
		indices.add("1"); indices.add("2");
		String[] prefix = new String[PursuitGame.NUM_AGENTS-1];
		Random rnd = new Random();
		for( int index = 0; index < PursuitGame.NUM_AGENTS-1; index++ ) {
			
		    prefix[index] = indices.remove(rnd.nextInt(indices.size()));
		}
		    
		/**
		 * then find the set of actions which may be a meta equilibrium 
		 * and find the intersection
		 */
		ArrayList<GameAction> possMetaSet_i = agent_i.getPossibleMetaEquil(gameState, prefix);
		ArrayList<GameAction> possMetaSet_j = agent_j.getPossibleMetaEquil(gameState, prefix);
		agent_i.findMetaEquils(possMetaSet_i, possMetaSet_j);
		agent_j.findMetaEquils(possMetaSet_i, possMetaSet_j);
	    }
	}
	
	/**
	 * then choose one optimal action
	 */
	GameAction[] favorActions = new GameAction[PursuitGame.NUM_AGENTS-1];
	favorActions[agent_i.getAgentIndex()-1] = agent_i.myFavoriteAction(gameState);
	favorActions[agent_j.getAgentIndex()-1] = agent_j.myFavoriteAction(gameState);
	
	Random rnd = new Random(); 
	GameAction selectedAction = favorActions[rnd.nextInt(PursuitGame.NUM_AGENTS-1)];
	
	return selectedAction;
    }
    
    public void findNEs( ArrayList<GameAction> maxSet_i, ArrayList<GameAction> maxSet_j )
    {
	if( maxSet_i == null || maxSet_j == null ) {
	    
	    System.out.println("@NegoQ->findNE: NULL Parameters!");
	    return;
	}
	
	/**
	 * clean the list of pure strategy NEs 
	 * for the current state
	 */
	nashEquilActions.clear();
	
	for( int jntActionIndex = 0; jntActionIndex < maxSet_i.size(); jntActionIndex++ ) {
	    
	    GameAction jntAction = maxSet_i.get( jntActionIndex );
	    
	    if( maxSet_j.contains(jntAction) ) {
		
		if( !nashEquilActions.contains(jntAction) )
		    nashEquilActions.add( jntAction );
	    }
	}
	
    }
    
    public void findNSEDAs( ArrayList<GameAction> partDmSet_i, ArrayList<GameAction> partDmSet_j )
    {
	if( partDmSet_i == null || partDmSet_j == null ) {
	    
	    System.out.println("@NegoQ->findNSEDAs: NULL Parameters!");
	    return;
	}
	
	/**
	 * clean the list of NSEDAs
	 * for the current state
	 */
	nsedaActions.clear();
	
	for( int jntActionIndex = 0; jntActionIndex < partDmSet_i.size(); jntActionIndex++ ) {
	    
	    GameAction jntAction = partDmSet_i.get( jntActionIndex );
	    
	    if( partDmSet_j.contains( jntAction ) ) {
		
		if( !nsedaActions.contains( jntAction ) )
		    nsedaActions.add( jntAction );
	    }
	}
    }
    
    
    public void findSymmEquils( ArrayList<GameAction> possSymmSet_i, ArrayList<GameAction> possSymmSet_j )
    {
	if( possSymmSet_i == null || possSymmSet_j == null ) {
	    
	    
	    return;
	}
	
	symmMetaEquilActions.clear();
	
	for( int jntActionIndex = 0; jntActionIndex < possSymmSet_i.size(); jntActionIndex++ ) {
		
	    GameAction jntAction = possSymmSet_i.get( jntActionIndex );
		
	    if( possSymmSet_j.contains( jntAction ) )
		if( !symmMetaEquilActions.contains( jntAction ) )
		    symmMetaEquilActions.add( jntAction );
	}
    }
    
    public void findMetaEquils( ArrayList<GameAction> possMetaSet_i, ArrayList<GameAction> possMetaSet_j )
    {
	if( possMetaSet_i == null || possMetaSet_j == null ) {
	    
	    System.out.println("@NegoQ->findMetaEquils: NULL Parameters!");
	    return;
	}
	
	/**
	 * clean the list of meta equlibria
	 * for the current state
	 */
	metaEquilActions.clear();
	
	for( int jntActionIndex = 0; jntActionIndex < possMetaSet_i.size(); jntActionIndex++ ) {
	    
	    GameAction jntAction = possMetaSet_i.get( jntActionIndex );
	    
	    if( possMetaSet_j.contains( jntAction ) ) {
		
		if( !metaEquilActions.contains( jntAction ) ) 
		    metaEquilActions.add( jntAction );
	    }
	}
    }
    
    /**
     * compute the max set in a state and return 
     * negotiation for pure strategy Nash equilibria
     */
    public ArrayList<GameAction> getMaxSet( GameState gameState )
    {
	if( gameState == null ) {
	    
	    System.out.println("@NegoQ->getMaxSet: NULL gameState!");
	    return null;
	}
	
	ArrayList<GameAction> retList = new ArrayList<GameAction>();
	
	/**
	 * find max action according to the current Q-table
	 */
	for( int act_j = 0; act_j < GameAction.NUM_ACTIONS; act_j++ ) {
	    
	    int agent_j = 1;
	    if( agentIndex == 1 )
		agent_j = 2;
	    
	    /**
	     * find the max value first 
	     * and then find all max action
	     */
	    GameAction maxAction = new GameAction();
	    maxAction.setAction( PursuitGame.PREY_AGENT_INDEX, 0);
	    maxAction.setAction(agentIndex, 0);
	    maxAction.setAction(agent_j, act_j);
	    double maxValue = getQValue( agentIndex, gameState, maxAction );
	    
	    for( int act_i = 1; act_i < GameAction.NUM_ACTIONS; act_i++ ) {
		    
		GameAction gmAction = new GameAction();
		gmAction.setAction(PursuitGame.PREY_AGENT_INDEX, 0);
		gmAction.setAction(agentIndex, act_i);
		gmAction.setAction(agent_j, act_j);
		
		double value = getQValue(agentIndex, gameState, gmAction);
		if( value > maxValue )
			maxValue = value;
	    }
	    for( int act_i = 0; act_i < GameAction.NUM_ACTIONS; act_i++ ) {
		    
		GameAction gmAction = new GameAction();
		gmAction.setAction(PursuitGame.PREY_AGENT_INDEX, 0);
		gmAction.setAction(agentIndex, act_i);
		gmAction.setAction(agent_j, act_j);
		    
		double value = getQValue(agentIndex, gameState, gmAction);
		if( value >= maxValue ) {
			
		    if( !retList.contains( gmAction ) )
			retList.add( gmAction );
		}
	    }
	}
	
	return retList;
    }
    
    /**
     * get the max set of a specific agent
     */
    public ArrayList<GameAction> getMaxSet( GameState gameState, int agent_i )
    {
	if( gameState == null ) {
	    
	    System.out.println("@NegoQ->getMaxSet: NULL gameState!");
	    return null;
	}
	
	ArrayList<GameAction> retList = new ArrayList<GameAction>();
	
	/**
	 * find max action according to the current Q-table
	 */
	for( int act_j = 0; act_j < GameAction.NUM_ACTIONS; act_j++ ) {
	    
	    int agent_j = 1;
	    if( agent_i == 1 )
		agent_j = 2;
	    
	    /**
	     * find the max value first 
	     * and then find all max action
	     */
	    GameAction maxAction = new GameAction();
	    maxAction.setAction(PursuitGame.PREY_AGENT_INDEX, 0);
	    maxAction.setAction(agent_i, 0);
	    maxAction.setAction(agent_j, act_j);
	    double maxValue = getQValue( agent_i, gameState, maxAction );
	    
	    for( int act_i = 1; act_i < GameAction.NUM_ACTIONS; act_i++ ) {
		    
		GameAction gmAction = new GameAction();
		gmAction.setAction(PursuitGame.PREY_AGENT_INDEX, 0);
		gmAction.setAction(agent_i, act_i);
		gmAction.setAction(agent_j, act_j);
		
		double value = getQValue(agent_i, gameState, gmAction);
		if( value > maxValue )
			maxValue = value;
	    }
	    for( int act_i = 0; act_i < GameAction.NUM_ACTIONS; act_i++ ) {
		    
		GameAction gmAction = new GameAction();
		gmAction.setAction(PursuitGame.PREY_AGENT_INDEX, 0);
		gmAction.setAction(agent_i, act_i);
		gmAction.setAction(agent_j, act_j);
		    
		double value = getQValue(agent_i, gameState, gmAction);
		if( value >= maxValue ) {
			
		    if( !retList.contains( gmAction ) )
			retList.add( gmAction );
		}
	    }
	}
	
	return retList;
    }
    
    /**
     * compute the partially dominating set for finding NSEDAs
     */
    public ArrayList<GameAction> getPartiallyDominatingSet( GameState gameState )
    {
	if( gameState == null ) {
	    
	    System.out.println("@NegoQ->getPartiallyDominatingSet: NULL gameState!");
	    return null;
	}

	
	ArrayList<GameAction> retList = new ArrayList<GameAction>();
	
	/**
	 * no nash equilibria
	 */
	if( nashEquilActions.size() == 0 ) {
	    
	    System.out.println("@NegoQ->getPartiallyDominatingSet: No Nash Equilibria!");
	    return retList;
	}
	
	for( int a1 = 0; a1 < GameAction.NUM_ACTIONS; a1++) 
	    for( int a2 = 0; a2 < GameAction.NUM_ACTIONS; a2++ ) {
		    
		GameAction jntAction = new GameAction( new int[]{0,a1,a2} );
		    
		/**
		 * because NSEDA must be a non-equilibrium action
		 */
		if( nashEquilActions.contains( jntAction ) )
		    continue;
		
		double jntActionValue = getQValue( agentIndex, gameState, jntAction );
		for( int neIndex = 0; neIndex < nashEquilActions.size(); neIndex++ ) {
			
		    GameAction neAction = nashEquilActions.get(neIndex);
		    double neValue = getQValue( agentIndex, gameState, neAction );
			
		    if( jntActionValue >= neValue ) {
			    
			if( !retList.contains( jntAction ) )
			    retList.add( jntAction );
			    
			break;
		    }
		}
	    }
	
	return retList;
    }
    
    
    /**
     * get the partially dominating set for finding NSEDAs for a specific agent
     */
    public ArrayList<GameAction> getPartiallyDominatingSet( GameState gameState, int agent_i )
    {
	if( gameState == null ) {
	    
	    System.out.println("@NegoQ->getPartiallyDominatingSet: NULL gameState!");
	    return null;
	}

	
	ArrayList<GameAction> retList = new ArrayList<GameAction>();
	
	/**
	 * no nash equilibria
	 */
	if( nashEquilActions.size() == 0 ) {
	    
	    System.out.println("@NegoQ->getPartiallyDominatingSet: No Nash Equilibria!");
	    return retList;
	}
	
	for( int a1 = 0; a1 < GameAction.NUM_ACTIONS; a1++) 
	    for( int a2 = 0; a2 < GameAction.NUM_ACTIONS; a2++ ) {
		    
		GameAction jntAction = new GameAction( new int[]{0,a1,a2} );
		    
		/**
		 * because NSEDA must be a non-equilibrium action
		 */
		if( nashEquilActions.contains( jntAction ) )
		    continue;
		
		double jntActionValue = getQValue( agent_i, gameState, jntAction );
		for( int neIndex = 0; neIndex < nashEquilActions.size(); neIndex++ ) {
			
		    GameAction neAction = nashEquilActions.get(neIndex);
		    double neValue = getQValue( agent_i, gameState, neAction );
			
		    if( jntActionValue >= neValue ) {
			    
			if( !retList.contains( jntAction ) )
			    retList.add( jntAction );
			    
			break;
		    }
		}
	    }
	
	return retList;
    }
    
    /**
     * compute the set of possible meta equilibria 
     * according to this agent's Q-table
     */
    public ArrayList<GameAction> getPossibleMetaEquil( GameState gameState, String[] prefix )
    {
	if( gameState == null || prefix == null ) {
	    
	    System.out.println("@NegoQ->getPossibleMetaEquil: NULL Parameters!");
	    return null;
	}
	else if( prefix.length != PursuitGame.NUM_AGENTS-1 ) {
	    
	    System.out.println("@NegoQ->getPossibleMetaEquil: Wrong Prefix!");
	    return null;
	}
	
	ArrayList<GameAction> retList = new ArrayList<GameAction>();
	
	/**
	 * get the agent index in each position
	 * agent0 and agent1 should belong to {1,2}
	 */
	int agent0 = Integer.parseInt( prefix[0] );
	int agent1 = Integer.parseInt( prefix[1] );
	
	double threValue = 0.0;
	int[] indicesArray = null;
	int index = 0;
	
	/**
	 * max_min situation
	 */
	if( agentIndex == agent0 ) {
	    
	    //find the min actions in agent1's dimension
	    indicesArray = findIndicesArray_Min( agent0, agent1, gameState);
	    
	    //find the max action in agent0's dimension
	    index = findIndexAction_Max( indicesArray, agent0, agent1, gameState);
	}
	/**
	 * min_max situation
	 */
	else if( agentIndex == agent1 ) {
	    
	    //find the max actions in agent1's dimension
	    indicesArray = findIndicesArray_Max( agent0, agent1, gameState);
	    
	    //find the min action in agent0's dimension
	    index = findIndexAction_Min( indicesArray, agent0, agent1, gameState);
	}
	
	else {
	    
	    System.out.println("@NegoQ->getPossibleMetaEquil: Cannot find my Index in prefix!");
	    return null;
	}
	
	/**
	 * get the threshold value
	 */
	GameAction threAction = new GameAction();
	threAction.setAction( PursuitGame.PREY_AGENT_INDEX, 0);
	threAction.setAction( agent0, index );
	threAction.setAction( agent1, indicesArray[index] );
	threValue = getQValue( agentIndex, gameState, threAction );
	
	
	/**
	 * then find all joint actions which are larger than 
	 * threshold value
	 */
	for( int a0 = 0; a0 < GameAction.NUM_ACTIONS; a0++ )
	    for( int a1 = 0; a1 < GameAction.NUM_ACTIONS; a1++ ) {
		    
		    GameAction jntAction = new GameAction( new int[]{0,a0,a1} );
		    double value = getQValue( agentIndex, gameState, jntAction );
		    
		    if( value >= threValue ) {
			
			if( !retList.contains( jntAction ) )
			    retList.add( jntAction );
		    }
		}
	
	return retList;
    }

    
    /**
     * compute the set of possible meta equilibria for a specific agent
     */
    public ArrayList<GameAction> getPossibleMetaEquil( GameState gameState, String[] prefix, 
	    int agent_i )
    {
	if( gameState == null || prefix == null ) {
	    
	    System.out.println("@NegoQ->getPossibleMetaEquil: NULL Parameters!");
	    return null;
	}
	else if( prefix.length != PursuitGame.NUM_AGENTS-1 ) {
	    
	    System.out.println("@NegoQ->getPossibleMetaEquil: Wrong Prefix!");
	    return null;
	}
	if( agent_i < 1 || agent_i >= PursuitGame.NUM_AGENTS ) {
	    
	    System.out.println("@NegoQ->NULL Agents!");
	    return null;
	}
	
	ArrayList<GameAction> retList = new ArrayList<GameAction>();
	
	/**
	 * get the agent index in each position
	 */
	int agent0 = Integer.parseInt( prefix[0] );
	int agent1 = Integer.parseInt( prefix[1] );
	
	double threValue = 0.0;
	int[] indicesArray = null;
	int index = 0;
	
	/**
	 * max_min situation
	 */
	if( agent_i == agent0 ) {
	    
	    //find the min actions in agent1's dimension
	    indicesArray = findIndicesArray_Min( agent0, agent1, gameState, agent_i );
	    
	    //find the max action in agent0's dimension
	    index = findIndexAction_Max( indicesArray, agent0, agent1, gameState, agent_i );
	}
	/**
	 * min_max situation
	 */
	else if( agent_i == agent1 ) {
	    
	    //find the max actions in agent1's dimension
	    indicesArray = findIndicesArray_Max( agent0, agent1, gameState, agent_i );
	    
	    //find the min action in agent0's dimension
	    index = findIndexAction_Min( indicesArray, agent0, agent1, gameState, agent_i );
	}
	
	else {
	    
	    System.out.println("@NegoQ->getPossibleMetaEquil: Cannot find my Index in prefix!");
	    return null;
	}
	
	/**
	 * get the threshold value
	 */
	GameAction threAction = new GameAction();
	threAction.setAction(PursuitGame.PREY_AGENT_INDEX, 0);
	threAction.setAction( agent0, index );
	threAction.setAction( agent1, indicesArray[index] );
	threValue = getQValue( agent_i, gameState, threAction );
	
	
	/**
	 * then find all joint actions which are larger than 
	 * threshold value
	 */
	for( int a0 = 0; a0 < GameAction.NUM_ACTIONS; a0++ )
	    for( int a1 = 0; a1 < GameAction.NUM_ACTIONS; a1++ ) {
		    
		    GameAction jntAction = new GameAction( new int[]{0,a0,a1} );
		    double value = getQValue( agent_i, gameState, jntAction );
		    
		    if( value >= threValue ) {
			
			if( !retList.contains( jntAction ) )
			    retList.add( jntAction );
		    }
		}
	
	return retList;
    }
    
    /**
     * compute the set of possible symmetric meta equilibria
     */
    public ArrayList<GameAction> getPossibleSymmEquilSet( GameState gameState )
    {
	if( gameState == null ) {
	    
	    System.out.println("@NegoQ->getPossibleSymmEquilSet: NULL Parameters!");
	    return null;
	}
	
	String[] prefix1 = {"1","2"};
	String[] prefix2 = {"2","1"};
	ArrayList<GameAction> possSymmSet1 = getPossibleMetaEquil(gameState, prefix1);
	ArrayList<GameAction> possSymmSet2 = getPossibleMetaEquil(gameState, prefix2);
	
	ArrayList<GameAction> possSymmSet = new ArrayList<GameAction>();
	
	for( int jntActionIndex = 0; jntActionIndex < possSymmSet1.size(); jntActionIndex++ ) {
	    
	    GameAction jntAction = possSymmSet1.get( jntActionIndex );
	    
	    if( possSymmSet2.contains( jntAction ) ) {
		
		if( !possSymmSet.contains( jntAction ) )
		    possSymmSet.add( jntAction );
	    }
	}
	
	return possSymmSet;
    }
    
    /**
     * compute the set of possible symmetric meta equilibria 
     * for a specific agent
     */
    public ArrayList<GameAction> getPossibleSymmEquilSet( GameState gameState, int agent_i )
    {
	if( gameState == null ) {
	    
	    System.out.println("@NegoQ->getPossibleSymmEquilSet: NULL Parameters!");
	    return null;
	}
	
	String[] prefix1 = {"1","2"};
	String[] prefix2 = {"2","1"};
	ArrayList<GameAction> possSymmSet1 = getPossibleMetaEquil( gameState, prefix1, agent_i );
	ArrayList<GameAction> possSymmSet2 = getPossibleMetaEquil( gameState, prefix2, agent_i );
	
	ArrayList<GameAction> possSymmSet = new ArrayList<GameAction>();
	
	for( int jntActionIndex = 0; jntActionIndex < possSymmSet1.size(); jntActionIndex++ ) {
	    
	    GameAction jntAction = possSymmSet1.get( jntActionIndex );
	    
	    if( possSymmSet2.contains( jntAction ) ) {
		
		if( !possSymmSet.contains( jntAction ) )
		    possSymmSet.add( jntAction );
	    }
	}
	
	return possSymmSet;
    }
    
    /**
     * find the action with the highest utility from 
     * Nash equilibria, NSEDAs and meta equilibria
     */
    public GameAction myFavoriteAction( GameState gameState )
    {
	if( nashEquilActions.size() == 0 && 
		symmMetaEquilActions.size() == 0 && 
		metaEquilActions.size() == 0 ) {
	    
	    System.out.println("@NegoQ->myFavoriteAction: Wrong!No Optimal Action!");
	    
	    GameAction randAction = new GameAction();
	    randAction.setAction(PursuitGame.PREY_AGENT_INDEX,0);
	    randAction.setAction(1, random.nextInt(GameAction.NUM_ACTIONS) );
	    randAction.setAction(2, random.nextInt(GameAction.NUM_ACTIONS) );
	    return randAction;
	}
	
	ArrayList<GameAction> optActions = new ArrayList<GameAction>();
	
	optActions.addAll(nashEquilActions);
	optActions.addAll(nsedaActions);
	optActions.addAll(symmMetaEquilActions);
	optActions.addAll(metaEquilActions);
	
	GameAction maxAction = optActions.get(0);
	double maxValue = getQValue( agentIndex, gameState, maxAction );
	for( int jntActionIndex = 1; jntActionIndex < optActions.size(); jntActionIndex++ ) {
	    
	    GameAction jntAction = optActions.get( jntActionIndex );
	    double value = getQValue( agentIndex, gameState, jntAction );
	    
	    if( value > maxValue ) {
		
		maxAction = jntAction;
		maxValue = value;
	    }
	}
	
	nashEquilActions.clear();
	nsedaActions.clear();
	symmMetaEquilActions.clear();
	metaEquilActions.clear();
	
	
	return maxAction;
    }
    
    /**
     * find the action with the highest utility from 
     * Nash equilibria, NSEDAs and meta equilibria
     */
    public GameAction myFavoriteAction( GameState gameState, int agent_i )
    {
	if( nashEquilActions.size() == 0 && 
		symmMetaEquilActions.size() == 0 && 
		metaEquilActions.size() == 0 ) {
	    
	    System.out.println("@NegoQ->myFavoriteAction: Wrong!No Optimal Action!");
	    
	    GameAction randAction = new GameAction();
	    randAction.setAction(PursuitGame.PREY_AGENT_INDEX,0);
	    randAction.setAction(1, random.nextInt(GameAction.NUM_ACTIONS) );
	    randAction.setAction(2, random.nextInt(GameAction.NUM_ACTIONS) );
	    return randAction;
	}
	
	ArrayList<GameAction> optActions = new ArrayList<GameAction>();
	
	optActions.addAll(nashEquilActions);
	optActions.addAll(nsedaActions);
	optActions.addAll(symmMetaEquilActions);
	optActions.addAll(metaEquilActions);
	
	GameAction maxAction = optActions.get(0);
	double maxValue = getQValue( agent_i, gameState, maxAction );
	for( int jntActionIndex = 1; jntActionIndex < optActions.size(); jntActionIndex++ ) {
	    
	    GameAction jntAction = optActions.get( jntActionIndex );
	    double value = getQValue( agent_i, gameState, jntAction );
	    
	    if( value > maxValue ) {
		
		maxAction = jntAction;
		maxValue = value;
	    }
	}
	
	/**
	 * remove these operations
	 *
	nashEquilActions.clear();
	nsedaActions.clear();
	symmMetaEquilActions.clear();
	metaEquilActions.clear();
	*/
	
	
	return maxAction;
    }
    
    public boolean existsNE()
    {
	if( nashEquilActions.size() == 0 )
	    return false;
	else
	    return true;
    }
    
    public boolean existsSymmMetaEquil()
    {
	if( symmMetaEquilActions.size() == 0 )
	    return false;
	else
	    return true;
    }
    
    
    /**
     * find action indices in agent1's dimension which satisfy: 
     * max_min
     */
    private int[] findIndicesArray_Min( int agent0, int agent1, GameState gameState )
    {
	
	if( gameState == null ) {
	    
	    System.out.println("@NegoQ->findIndicesArray_Min: NULL Parameters!");
	    return null;
	}
	if( agent0 < 1 || agent0 >= PursuitGame.NUM_AGENTS ||
		agent1 < 1 || agent1 >= PursuitGame.NUM_AGENTS ) {
	    
	    System.out.println("@NegoQ->findIndicesArray_Min: Wrong Agents!");
	    return null;
	}
	
	int[] indicesArray = new int[GameAction.NUM_ACTIONS];
	for( int act0 = 0; act0 < GameAction.NUM_ACTIONS; act0++ ) {
	
	    GameAction minAction = new GameAction();
	    minAction.setAction( PursuitGame.PREY_AGENT_INDEX, 0);
	    minAction.setAction( agent0, act0 );
	    minAction.setAction( agent1, 0 );
	    double minValue = getQValue( agentIndex, gameState, minAction );
	    int minIndex = 0;
	    
	    for( int act1 = 1; act1 < GameAction.NUM_ACTIONS; act1++ ) {
		
		GameAction jntAction = new GameAction();
		jntAction.setAction( PursuitGame.PREY_AGENT_INDEX, 0 );
		jntAction.setAction( agent0, act0 );
		jntAction.setAction( agent1, act1 );
		
		double value = getQValue( agentIndex, gameState, jntAction );
		if( value < minValue ) {
		    
		    minValue = value;
		    minIndex = act1;
		}
	    }
	    
	    indicesArray[act0] = minIndex;
	}
	
	return indicesArray;
    }

    /**
     * find action indices in agent1's dimension which satisfy: 
     * max_min
     */
    private int[] findIndicesArray_Min( int agent0, int agent1, GameState gameState, 
	    int agent_i )
    {
	
	if( gameState == null ) {
	    
	    System.out.println("@NegoQ->findIndicesArray_Min: NULL Parameters!");
	    return null;
	}
	if( agent0 < 1 || agent0 >= PursuitGame.NUM_AGENTS ||
		agent1 < 1 || agent1 >= PursuitGame.NUM_AGENTS || 
		agent_i < 1 || agent_i >= PursuitGame.NUM_AGENTS ) {
	    
	    System.out.println("@NegoQ->findIndicesArray_Min: Wrong Agents!");
	    return null;
	}
	
	int[] indicesArray = new int[GameAction.NUM_ACTIONS];
	for( int act0 = 0; act0 < GameAction.NUM_ACTIONS; act0++ ) {
	
	    GameAction minAction = new GameAction();
	    minAction.setAction( PursuitGame.PREY_AGENT_INDEX, 0);
	    minAction.setAction( agent0, act0 );
	    minAction.setAction( agent1, 0 );
	    double minValue = getQValue( agent_i, gameState, minAction );
	    int minIndex = 0;
	    
	    for( int act1 = 1; act1 < GameAction.NUM_ACTIONS; act1++ ) {
		
		GameAction jntAction = new GameAction();
		jntAction.setAction( PursuitGame.PREY_AGENT_INDEX, 0 );
		jntAction.setAction( agent0, act0 );
		jntAction.setAction( agent1, act1 );
		
		double value = getQValue( agent_i, gameState, jntAction );
		if( value < minValue ) {
		    
		    minValue = value;
		    minIndex = act1;
		}
	    }
	    
	    indicesArray[act0] = minIndex;
	}
	
	return indicesArray;
    }
    
    /**
     * find action indices in agent1's dimeion which satisfy:
     * min_max
     */
    private int[] findIndicesArray_Max( int agent0, int agent1, GameState gameState )
    {
	if( gameState == null ) {
	    
	    System.out.println("@NegoQ->findIndicesArray_Max: NULL Parameters!");
	    return null;
	}
	if( agent0 < 1 || agent0 >= PursuitGame.NUM_AGENTS ||
		agent1 < 1 || agent1 >= PursuitGame.NUM_AGENTS ) {
	    
	    System.out.println("@NegoQ->findIndicesArray_Max: Wrong Agents!");
	    return null;
	}
	
	int[] indicesArray = new int[GameAction.NUM_ACTIONS];
	for( int act0 = 0; act0 < GameAction.NUM_ACTIONS; act0++ ) {
	
	    GameAction maxAction = new GameAction();
	    maxAction.setAction(PursuitGame.PREY_AGENT_INDEX, 0);
	    maxAction.setAction( agent0, act0 );
	    maxAction.setAction( agent1, 0 );
	    double maxValue = getQValue( agentIndex, gameState, maxAction );
	    int maxIndex = 0;
	    
	    for( int act1 = 1; act1 < GameAction.NUM_ACTIONS; act1++ ) {
		
		GameAction jntAction = new GameAction();
		jntAction.setAction(PursuitGame.PREY_AGENT_INDEX, 0);
		jntAction.setAction( agent0, act0 );
		jntAction.setAction( agent1, act1 );
		
		double value = getQValue( agentIndex, gameState, jntAction );
		if( value > maxValue ) {
		    
		    maxValue = value;
		    maxIndex = act1;
		}
	    }
	    
	    indicesArray[act0] = maxIndex;
	}
	
	return indicesArray;
    }
    
    
    private int[] findIndicesArray_Max( int agent0, int agent1, GameState gameState, 
	    int agent_i )
    {
	if( gameState == null ) {
	    
	    System.out.println("@NegoQ->findIndicesArray_Max: NULL Parameters!");
	    return null;
	}
	if( agent0 < 1 || agent0 >= PursuitGame.NUM_AGENTS ||
		agent1 < 1 || agent1 >= PursuitGame.NUM_AGENTS || 
		agent_i < 1 || agent_i >= PursuitGame.NUM_AGENTS ) {
	    
	    System.out.println("@NegoQ->findIndicesArray_Max: Wrong Agents!");
	    return null;
	}
	
	int[] indicesArray = new int[GameAction.NUM_ACTIONS];
	for( int act0 = 0; act0 < GameAction.NUM_ACTIONS; act0++ ) {
	
	    GameAction maxAction = new GameAction();
	    maxAction.setAction(PursuitGame.PREY_AGENT_INDEX, 0);
	    maxAction.setAction( agent0, act0 );
	    maxAction.setAction( agent1, 0 );
	    double maxValue = getQValue( agent_i, gameState, maxAction );
	    int maxIndex = 0;
	    
	    for( int act1 = 1; act1 < GameAction.NUM_ACTIONS; act1++ ) {
		
		GameAction jntAction = new GameAction();
		jntAction.setAction(PursuitGame.PREY_AGENT_INDEX, 0);
		jntAction.setAction( agent0, act0 );
		jntAction.setAction( agent1, act1 );
		
		double value = getQValue( agent_i, gameState, jntAction );
		if( value > maxValue ) {
		    
		    maxValue = value;
		    maxIndex = act1;
		}
	    }
	    
	    indicesArray[act0] = maxIndex;
	}
	
	return indicesArray;
    }
    
    /**
     * find action index in agent0's dimension which satisfies:
     * min_max
     */
    private int findIndexAction_Min( int[] indicesArray, int agent0, 
	    int agent1, GameState gameState )
    {
	if( indicesArray == null || gameState == null ) {
	    
	    System.out.println("@NegoQ->findIndexAction_Min: NULL Parameters!");
	    return -1;
	}
	if( agent0 < 1 || agent0 >= PursuitGame.NUM_AGENTS ||
		agent1 < 1 || agent1 >= PursuitGame.NUM_AGENTS ) {
	    
	    System.out.println("@NegoQ->findIndexAction_Min: Wrong Agents!");
	    return 0;
	}
	
	GameAction minAction = new GameAction();
	minAction.setAction(PursuitGame.PREY_AGENT_INDEX, 0);
	minAction.setAction( agent0, 0 );
	minAction.setAction( agent1, indicesArray[0] );
	double minValue = getQValue( agentIndex, gameState, minAction );
	int minIndex = 0;
	
	for( int act0 = 1; act0 < GameAction.NUM_ACTIONS; act0++ ) {
	    
	    GameAction jntAction = new GameAction();
	    jntAction.setAction(PursuitGame.PREY_AGENT_INDEX, 0);
	    jntAction.setAction( agent0, act0 );
	    jntAction.setAction( agent1, indicesArray[act0] );
	    
	    double value = getQValue( agentIndex, gameState, jntAction );
	    if( value < minValue ) {
		
		minValue = value;
		minIndex = act0;
	    }
	}
	
	return minIndex;
    } 
    
    private int findIndexAction_Min( int[] indicesArray, int agent0, 
	    int agent1, GameState gameState, int agent_i )
    {
	if( indicesArray == null || gameState == null ) {
	    
	    System.out.println("@NegoQ->findIndexAction_Min: NULL Parameters!");
	    return -1;
	}
	if( agent0 < 1 || agent0 >= PursuitGame.NUM_AGENTS ||
		agent1 < 1 || agent1 >= PursuitGame.NUM_AGENTS || 
		agent_i < 1 || agent_i >= PursuitGame.NUM_AGENTS ) {
	    
	    System.out.println("@NegoQ->findIndexAction_Min: Wrong Agents!");
	    return 0;
	}
	
	GameAction minAction = new GameAction();
	minAction.setAction(PursuitGame.PREY_AGENT_INDEX, 0);
	minAction.setAction( agent0, 0 );
	minAction.setAction( agent1, indicesArray[0] );
	double minValue = getQValue( agent_i, gameState, minAction );
	int minIndex = 0;
	
	for( int act0 = 1; act0 < GameAction.NUM_ACTIONS; act0++ ) {
	    
	    GameAction jntAction = new GameAction();
	    jntAction.setAction(PursuitGame.PREY_AGENT_INDEX, 0);
	    jntAction.setAction( agent0, act0 );
	    jntAction.setAction( agent1, indicesArray[act0] );
	    
	    double value = getQValue( agent_i, gameState, jntAction );
	    if( value < minValue ) {
		
		minValue = value;
		minIndex = act0;
	    }
	}
	
	return minIndex;
    }
    
    /**
     * find action index in agent0's dimension which satisfies:
     * max_min_min
     */
    private int findIndexAction_Max( int[] indicesArray, int agent0, 
	    int agent1, GameState gameState )
    {
	if( indicesArray == null || gameState == null ) {
	    
	    System.out.println("@NegoQ->findIndexAction_Max: NULL Parameters!");
	    return -1;
	}
	if( agent0 < 1 || agent0 >= PursuitGame.NUM_AGENTS ||
		agent1 < 1 || agent1 >= PursuitGame.NUM_AGENTS ) {
	    
	    System.out.println("@NegoQ->findIndexAction_Max: Wrong Agents!");
	    return 0;
	}
	
	GameAction maxAction = new GameAction();
	maxAction.setAction(PursuitGame.PREY_AGENT_INDEX, 0);
	maxAction.setAction( agent0, 0 );
	maxAction.setAction( agent1, indicesArray[0] );
	double maxValue = getQValue( agentIndex, gameState, maxAction );
	int maxIndex = 0;
	
	for( int act0 = 1; act0 < GameAction.NUM_ACTIONS; act0++ ) {
	    
	    GameAction jntAction = new GameAction();
	    jntAction.setAction(PursuitGame.PREY_AGENT_INDEX, 0);
	    jntAction.setAction( agent0, act0 );
	    jntAction.setAction( agent1, indicesArray[act0] );
	    
	    double value = getQValue( agentIndex, gameState, jntAction );
	    if( value > maxValue ) {
		
		maxValue = value;
		maxIndex = act0;
	    }
	}
	
	return maxIndex;
    }
    
    
    private int findIndexAction_Max( int[] indicesArray, int agent0, 
	    int agent1, GameState gameState, int agent_i )
    {
	if( indicesArray == null || gameState == null ) {
	    
	    System.out.println("@NegoQ->findIndexAction_Max: NULL Parameters!");
	    return -1;
	}
	if( agent0 < 1 || agent0 >= PursuitGame.NUM_AGENTS ||
		agent1 < 1 || agent1 >= PursuitGame.NUM_AGENTS ||
		agent_i < 1 || agent_i >= PursuitGame.NUM_AGENTS ) {
	    
	    System.out.println("@NegoQ->findIndexAction_Max: Wrong Agents!");
	    return 0;
	}
	
	GameAction maxAction = new GameAction();
	maxAction.setAction(PursuitGame.PREY_AGENT_INDEX, 0);
	maxAction.setAction( agent0, 0 );
	maxAction.setAction( agent1, indicesArray[0] );
	double maxValue = getQValue( agent_i, gameState, maxAction );
	int maxIndex = 0;
	
	for( int act0 = 1; act0 < GameAction.NUM_ACTIONS; act0++ ) {
	    
	    GameAction jntAction = new GameAction();
	    jntAction.setAction(PursuitGame.PREY_AGENT_INDEX, 0);
	    jntAction.setAction( agent0, act0 );
	    jntAction.setAction( agent1, indicesArray[act0] );
	    
	    double value = getQValue( agent_i, gameState, jntAction );
	    if( value > maxValue ) {
		
		maxValue = value;
		maxIndex = act0;
	    }
	}
	
	return maxIndex;
    }
    
    protected double getQValue( int agent, GameState gameState, 
	    GameAction gameAction )
    {
	if( gameAction == null || 
		gameState == null ) {
	    
	    System.out.println("@NegoQ->getQValue: Wrong Parameters!");
	    return 0.0;
	}
	if( agent < 1 || agent >= PursuitGame.NUM_AGENTS ) {
	    
	    System.out.println("@NegoQ->getQvalue: Wrong Agents!");
	    return 0.0;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int loc2 = gameState.getLocationID(2);
	//int a0 = gameAction.getAction(0);
	int a1 = gameAction.getAction(1);
	int a2 = gameAction.getAction(2);
	
	return negoQs[agent-1][loc0][loc1][loc2][a1][a2];
    }
    
    protected void setQValue( int agent, GameState gameState, 
	    GameAction gameAction, double value )
    {
	if( gameAction == null || 
		gameState == null ) {
	    
	    System.out.println("@NegoQ->setQValue: Wrong Parameters!");
	    return;
	}
	if( agent < 1 || agent >= PursuitGame.NUM_AGENTS ) {
	    
	    System.out.println("@NegoQ->setQValue: Wrong Agents!");
	    return;
	}
	
	int loc0 = gameState.getLocationID(0);
	int loc1 = gameState.getLocationID(1);
	int loc2 = gameState.getLocationID(2);
	//int a0 = gameAction.getAction(0);
	int a1 = gameAction.getAction(1);
	int a2 = gameAction.getAction(2);
	
	negoQs[agent-1][loc0][loc1][loc2][a1][a2] = value;
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
		    for( int s3 = 0; s3 < locNum; s3++ )
			for( int a1 = 0; a1 < actionNum; a1++ )
			    for( int a2 = 0; a2 < actionNum; a2++ ) {
					    
			    	    writer.write(""+negoQs[agentIndex-1][s1][s2][s3][a1][a2]);
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
	
	if( agent == PursuitGame.PREY_AGENT_INDEX )
	    return;
	
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
	    int s3 = 0;
	    int a1 = 0;
	    int a2 = 0;
	    
	    while( (line = reader.readLine()) != null ) {
		
		if( line.length() == 0 )
		    continue;
		
		negoQs[agent-1][s1][s2][s3][a1][a2] = Double.parseDouble( line );
		
		a2++;
		if( a2 >= actionNum ) {
			    
		    a2 = 0;
		    a1++;
		    if( a1 >= actionNum ) {
				
			a1 = 0;
			s3++;
			if( s3 >= locNum ) {
				
			    s3 = 0;
			    s2++;
			    if( s2 >= locNum ) {
					    
				s2 = 0;
				s1++;
			    }
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
}
