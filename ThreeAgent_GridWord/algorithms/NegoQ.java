package algorithms;

import java.util.ArrayList;
import java.util.Random;
import gameGridWorld.GameAction;
import gameGridWorld.GameState;
import gameGridWorld.GridWorld;

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
    
    /**
     * store some meta equilibria in a certain state
     */
    private ArrayList<GameAction> metaEquilActions;
    
    
    public NegoQ( int agIndex )
    {
	super( agIndex );
	
	/**
	 * init the Q-table
	 * for centralized algorithms 
	 * the Q-tables can be initialized randomly
	 */
	int locNum = GridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	
	/**
	 * init Q-tables again
	 */
	for( int agentIndex = 0; agentIndex < GridWorld.NUM_AGENTS; agentIndex++ )
	    for( int s1 = 0; s1 < locNum; s1++ )
		for( int s2 = 0; s2 < locNum; s2++ )
		    for(int s3 = 0; s3 < locNum; s3++ )
			for( int a1 = 0; a1 < actionNum; a1++ )
			    for( int a2 = 0; a2 < actionNum; a2++ )
				for(int a3 = 0; a3 < actionNum; a3++ ) {
				    
				    Qs[agentIndex][s1][s2][s3][a1][a2][a3] = (Math.random() - 0.5) / 10.0;
				}
	
	nashEquilActions = new ArrayList<GameAction>();
	nsedaActions = new ArrayList<GameAction>();
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
	int locNum = GridWorld.NUM_LOCATIONS;
	int actionNum = GameAction.NUM_ACTIONS;
	
	/**
	 * init Q-tables again
	 */
	for( int agentIndex = 0; agentIndex < GridWorld.NUM_AGENTS; agentIndex++ )
	    for( int s1 = 0; s1 < locNum; s1++ )
		for( int s2 = 0; s2 < locNum; s2++ )
		    for(int s3 = 0; s3 < locNum; s3++ )
			for( int a1 = 0; a1 < actionNum; a1++ )
			    for( int a2 = 0; a2 < actionNum; a2++ )
				for(int a3 = 0; a3 < actionNum; a3++ ) {
				    
				    Qs[agentIndex][s1][s2][s3][a1][a2][a3] = (Math.random() - 0.5) / 10.0;
				}
	
	nashEquilActions = new ArrayList<GameAction>();
	nsedaActions = new ArrayList<GameAction>();
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
    
    
    public static GameAction negotiation( NegoQ agent_i, NegoQ agent_j, NegoQ agent_k, 
	    GameState gameState )
    {
	
	if( gameState == null || agent_i == null || 
		agent_j == null || agent_k == null ) {
	    
	    System.out.println("@NegoQ->negotiation: NULL Parameters!");
	    return null;
	}
	
	
	/**
	 * negotiation for pure strategy Nash equilibria
	 */
	ArrayList<GameAction> maxSet_i = agent_i.getMaxSet(gameState);
	ArrayList<GameAction> maxSet_j = agent_j.getMaxSet(gameState);
	ArrayList<GameAction> maxSet_k = agent_k.getMaxSet(gameState);
	agent_i.findNEs(maxSet_i, maxSet_j, maxSet_k);
	agent_j.findNEs(maxSet_i, maxSet_j, maxSet_k);
	agent_k.findNEs(maxSet_i, maxSet_j, maxSet_k);
	
	
	/**
	 * if there exist Nash equilibira 
	 * then find NSEDAs,
	 * or find meta equilibria
	 */
	if( agent_i.existsNE() ) {
	    
	    
	    ArrayList<GameAction> partDmSet_i = agent_i.getPartiallyDominatingSet(gameState);
	    ArrayList<GameAction> partDmSet_j = agent_j.getPartiallyDominatingSet(gameState);
	    ArrayList<GameAction> partDmSet_k = agent_k.getPartiallyDominatingSet(gameState);
	    agent_i.findNSEDAs(partDmSet_i, partDmSet_j, partDmSet_k);
	    agent_j.findNSEDAs(partDmSet_i, partDmSet_j, partDmSet_k);
	    agent_k.findNSEDAs(partDmSet_i, partDmSet_j, partDmSet_k);
	    
	    /**
	     * find EDAs if needed
	     */
	    
	}
	else {
	    
	    /**
	     * choose one complete game first
	     */
	    ArrayList<String> indices = new ArrayList<String>();
	    indices.add("0"); indices.add("1"); indices.add("2");
	    String[] prefix = new String[GridWorld.NUM_AGENTS];
	    Random rnd = new Random();
	    for( int index = 0; index < GridWorld.NUM_AGENTS; index++ ) {
		
		prefix[index] = indices.remove(rnd.nextInt(indices.size()));
	    }
	    
	    /**
	     * then find the set of actions which may be a meta equilibrium 
	     * and find the intersection
	     */
	    ArrayList<GameAction> possMetaSet_i = agent_i.getPossibleMetaEquil(gameState, prefix);
	    ArrayList<GameAction> possMetaSet_j = agent_j.getPossibleMetaEquil(gameState, prefix);
	    ArrayList<GameAction> possMetaSet_k = agent_k.getPossibleMetaEquil(gameState, prefix);
	    agent_i.findMetaEquils(possMetaSet_i, possMetaSet_j, possMetaSet_k);
	    agent_j.findMetaEquils(possMetaSet_i, possMetaSet_j, possMetaSet_k);
	    agent_k.findMetaEquils(possMetaSet_i, possMetaSet_j, possMetaSet_k);
	}
	
	/**
	 * then choose one optimal action
	 */
	GameAction[] favorActions = new GameAction[GridWorld.NUM_AGENTS];
	favorActions[agent_i.getAgentIndex()] = agent_i.myFavoriteAction(gameState);
	favorActions[agent_j.getAgentIndex()] = agent_j.myFavoriteAction(gameState);
	favorActions[agent_k.getAgentIndex()] = agent_k.myFavoriteAction(gameState);
	
	Random rnd = new Random(); 
	GameAction selectedAction = favorActions[rnd.nextInt(GridWorld.NUM_AGENTS)];
	
	return selectedAction;
    }
    
    public void findNEs( ArrayList<GameAction> maxSet_i, ArrayList<GameAction> maxSet_j, 
	    ArrayList<GameAction> maxSet_k )
    {
	if( maxSet_i == null || maxSet_j == null ||
		maxSet_k == null ) {
	    
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
	    
	    if( maxSet_j.contains(jntAction) && maxSet_k.contains(jntAction) ) {
		
		if( !nashEquilActions.contains(jntAction) )
		    nashEquilActions.add( jntAction );
	    }
	}
	
    }
    
    public void findNSEDAs( ArrayList<GameAction> partDmSet_i, ArrayList<GameAction> partDmSet_j, 
	    ArrayList<GameAction> partDmSet_k )
    {
	if( partDmSet_i == null || partDmSet_j == null ||
		partDmSet_k == null ) {
	    
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
	    
	    if( partDmSet_j.contains( jntAction ) && partDmSet_k.contains( jntAction ) ) {
		
		if( !nsedaActions.contains( jntAction ) )
		    nsedaActions.add( jntAction );
	    }
	}
    }
    
    
    public void findMetaEquils( ArrayList<GameAction> possMetaSet_i, ArrayList<GameAction> possMetaSet_j, 
	    ArrayList<GameAction> possMetaSet_k )
    {
	if( possMetaSet_i == null || possMetaSet_j == null || 
		possMetaSet_k == null ) {
	    
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
	    
	    if( possMetaSet_j.contains( jntAction ) && possMetaSet_k.contains( jntAction ) ) {
		
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
	    
	    for( int act_k = 0; act_k < GameAction.NUM_ACTIONS; act_k++ ) {
		
		int agent_j = (agentIndex+1) % GridWorld.NUM_AGENTS;
		int agent_k = (agentIndex+2) % GridWorld.NUM_AGENTS;
		
		/**
		 * find the max value first 
		 * and then find all max action
		 */
		GameAction maxAction = new GameAction();
		maxAction.setAction(agentIndex, 0);
		maxAction.setAction(agent_j, act_j);
		maxAction.setAction(agent_k, act_k);
		double maxValue = getQValue( agentIndex, gameState, maxAction );
		for( int act_i = 1; act_i < GameAction.NUM_ACTIONS; act_i++ ) {
		    
		    GameAction gmAction = new GameAction();
		    gmAction.setAction(agentIndex, act_i);
		    gmAction.setAction(agent_j, act_j);
		    gmAction.setAction(agent_k, act_k);
		    
		    double value = getQValue(agentIndex, gameState, gmAction);
		    if( value > maxValue )
			maxValue = value;
		}
		for( int act_i = 0; act_i < GameAction.NUM_ACTIONS; act_i++ ) {
		    
		    GameAction gmAction = new GameAction();
		    gmAction.setAction(agentIndex, act_i);
		    gmAction.setAction(agent_j, act_j);
		    gmAction.setAction(agent_k, act_k);
		    
		    double value = getQValue(agentIndex, gameState, gmAction);
		    if( value >= maxValue ) {
			
			if( !retList.contains( gmAction ) )
			    retList.add( gmAction );
		    }
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
	    for( int a2 = 0; a2 < GameAction.NUM_ACTIONS; a2++ )
		for( int a3 = 0; a3 < GameAction.NUM_ACTIONS; a3++ ) {
		    
		    GameAction jntAction = new GameAction( new int[]{a1,a2,a3} );
		    
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
     * compute the set of possible meta equilibria 
     * according to this agent's Q-table
     */
    public ArrayList<GameAction> getPossibleMetaEquil( GameState gameState, String[] prefix )
    {
	if( gameState == null || prefix == null ) {
	    
	    System.out.println("@NegoQ->getPossibleMetaEquil: NULL Parameters!");
	    return null;
	}
	else if( prefix.length != GridWorld.NUM_AGENTS ) {
	    
	    System.out.println("@NegoQ->getPossibleMetaEquil: Wrong Prefix!");
	    return null;
	}
	
	ArrayList<GameAction> retList = new ArrayList<GameAction>();
	
	/**
	 * get the agent index in each position
	 */
	int agent0 = Integer.parseInt( prefix[0] );
	int agent1 = Integer.parseInt( prefix[1] );
	int agent2 = Integer.parseInt( prefix[2] );
	
	double threValue = 0.0;
	int[][] indicesMat = null;
	int[] indicesArray = null;
	int index = 0;
	
	/**
	 * max_min_min situation
	 */
	if( agentIndex == agent0 ) {
	    
	    //find the min actions in agent2's dimension
	    indicesMat = findIndicesMat_Min(agent0, agent1, agent2, gameState);
	    
	    //find the min actions in agent1's dimension
	    indicesArray = findIndicesArray_Min(indicesMat, agent0, agent1, agent2, gameState);
	    
	    //find the max action in agent0's dimension
	    index = findIndexAction_Max(indicesMat, indicesArray, agent0, agent1, agent2, gameState);
	}
	/**
	 * min_max_min situation
	 */
	else if( agentIndex == agent1 ) {
	    
	    //find the min actions in agent2's dimension
	    indicesMat = findIndicesMat_Min(agent0, agent1, agent2, gameState);
	    
	    //find the max actions in agent1's dimension
	    indicesArray = findIndicesArray_Max(indicesMat, agent0, agent1, agent2, gameState);
	    
	    //find the min action in agent0's dimension
	    index = findIndexAction_Min(indicesMat, indicesArray, agent0, agent1, agent2, gameState);
	}
	
	/**
	 * min_min_max situation
	 */
	else if( agentIndex == agent2 ) {
	    
	    //find the max actions in agent2's dimension
	    indicesMat = findIndicesMat_Max(agent0, agent1, agent2, gameState);
	    
	    //find the min actions in agent1's dimension
	    indicesArray = findIndicesArray_Min(indicesMat, agent0, agent1, agent2, gameState);
	    
	    //find the min action in agent0's dimension
	    index = findIndexAction_Min(indicesMat, indicesArray, agent0, agent1, agent2, gameState);
	}
	else {
	    
	    System.out.println("@NegoQ->getPossibleMetaEquil: Cannot find my Index in prefix!");
	    return null;
	}
	
	/**
	 * get the threshold value
	 */
	GameAction threAction = new GameAction();
	threAction.setAction( agent0, index );
	threAction.setAction( agent1, indicesArray[index] );
	threAction.setAction( agent2, indicesMat[index][indicesArray[index]] );
	threValue = getQValue( agentIndex, gameState, threAction );
	
	
	/**
	 * then find all joint actions which are larger than 
	 * threshold value
	 */
	for( int a0 = 0; a0 < GameAction.NUM_ACTIONS; a0++ )
	    for( int a1 = 0; a1 < GameAction.NUM_ACTIONS; a1++ )
		for( int a2 = 0; a2 < GameAction.NUM_ACTIONS; a2++ ) {
		    
		    GameAction jntAction = new GameAction( new int[]{a0,a1,a2} );
		    double value = getQValue( agentIndex, gameState, jntAction );
		    
		    if( value >= threValue ) {
			
			if( !retList.contains( jntAction ) )
			    retList.add( jntAction );
		    }
		}
	
	
	return retList;
    }
    
    
    /**
     * find the action with the highest utility from 
     * Nash equilibria, NSEDAs and meta equilibria
     */
    public GameAction myFavoriteAction( GameState gameState )
    {
	if( nashEquilActions.size() == 0 && 
		metaEquilActions.size() == 0 ) {
	    
	    System.out.println("@NegoQ->myFavoriteAction: Wrong!No Optimal Action!");
	    return null;
	}
	
	ArrayList<GameAction> optActions = new ArrayList<GameAction>();
	
	optActions.addAll(nashEquilActions);
	optActions.addAll(nsedaActions);
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
	
	
	/**
	 * 
	 *
	if( nashEquilActions.contains( maxAction ) ) {
	    
	    System.out.println("A Nash Equlibrium");
	}
	else if( nsedaActions.contains( maxAction ) ) {
	    
	    System.out.println("An NSEDA");
	}
	else {
	    
	    System.out.println("A Meta Equilibrium");
	}
	*/
	
	/**
	 * then clean all list?
	 */
	nashEquilActions.clear();
	nsedaActions.clear();
	metaEquilActions.clear();
	
	
	return maxAction;
    }
    
    public boolean existsNE()
    {
	if( nashEquilActions.size() == 0 )
	    return false;
	else
	    return true;
    }
    
    
    /**
     * find action indices in agent2's dimension which satisfy:
     * max_min_min or min_max_min 
     */
    private int[][] findIndicesMat_Min( int agent0, int agent1, int agent2, 
	    GameState gameState ) 
    {
	//parameters
	
	
	int[][] indicesMat = new int[GameAction.NUM_ACTIONS][GameAction.NUM_ACTIONS];
	
	for( int act0 = 0; act0 < GameAction.NUM_ACTIONS; act0++ )
	    for( int act1 = 0; act1 < GameAction.NUM_ACTIONS; act1++ ) {
		
		GameAction minAction = new GameAction();
		minAction.setAction( agent0, act0 );
		minAction.setAction( agent1, act1 );
		minAction.setAction( agent2, 0 );
		double minValue = getQValue( agentIndex, gameState, minAction );
		int minIndex = 0;
		for( int act2 = 1; act2 < GameAction.NUM_ACTIONS; act2++ ) {
			
		    GameAction jntAction = new GameAction();
		    jntAction.setAction( agent0, act0 );
		    jntAction.setAction( agent1, act1 );
		    jntAction.setAction( agent2, act2 );
			
		    double value = getQValue( agentIndex, gameState, jntAction );
		    if( value < minValue ) {
			    
			minIndex = act2;
			minValue = value;
		    }
		}
		indicesMat[act0][act1] = minIndex;
	    }
	
	return indicesMat;
    }
    
    /**
     * find action indices in agent2's dimension which satisfy:
     * min_min_max 
     */
    private int[][] findIndicesMat_Max( int agent0, int agent1, int agent2, 
	    GameState gameState )
    {
	
	//parameters
	
	
	int[][] indicesMat = new int[GameAction.NUM_ACTIONS][GameAction.NUM_ACTIONS];
	
	for( int act0 = 0; act0 < GameAction.NUM_ACTIONS; act0++ )
	    for( int act1 = 0; act1 < GameAction.NUM_ACTIONS; act1++ ) {
		
		GameAction maxAction = new GameAction();
		maxAction.setAction( agent0, act0 );
		maxAction.setAction( agent1, act1 );
		maxAction.setAction( agent2, 0 );
		double maxValue = getQValue( agentIndex, gameState, maxAction );
		int maxIndex = 0;
		for( int act2 = 1; act2 < GameAction.NUM_ACTIONS; act2++ ) {
			
		    GameAction jntAction = new GameAction();
		    jntAction.setAction( agent0, act0 );
		    jntAction.setAction( agent1, act1 );
		    jntAction.setAction( agent2, act2 );
			
		    double value = getQValue( agentIndex, gameState, jntAction );
		    if( value > maxValue ) {
			    
			maxIndex = act2;
			maxValue = value;
		    }
		}
		indicesMat[act0][act1] = maxIndex;
	    }
	
	return indicesMat;	
    }
    
    /**
     * find action indices in agent1's dimension which satisfy: 
     * min_min_max or max_min_min
     */
    private int[] findIndicesArray_Min( int[][] indicesMat, int agent0, 
	    int agent1, int agent2, GameState gameState )
    {
	
	if( indicesMat == null || gameState == null ) {
	    
	    System.out.println("@NegoQ->findIndicesArray_Min: NULL Parameters!");
	    return null;
	}
	
	int[] indicesArray = new int[GameAction.NUM_ACTIONS];
	for( int act0 = 0; act0 < GameAction.NUM_ACTIONS; act0++ ) {
	
	    GameAction minAction = new GameAction();
	    minAction.setAction( agent0, act0 );
	    minAction.setAction( agent1, 0 );
	    minAction.setAction( agent2, indicesMat[act0][0] );
	    double minValue = getQValue( agentIndex, gameState, minAction );
	    int minIndex = 0;
	    for( int act1 = 1; act1 < GameAction.NUM_ACTIONS; act1++ ) {
		
		GameAction jntAction = new GameAction();
		jntAction.setAction( agent0, act0 );
		jntAction.setAction( agent1, act1 );
		jntAction.setAction( agent2, indicesMat[act0][act1] );
		
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
     * find action indices in agent1's dimeion which satisfy:
     * min_max_min
     */
    private int[] findIndicesArray_Max( int[][] indicesMat, int agent0, 
	    int agent1, int agent2, GameState gameState )
    {
	if( indicesMat == null || gameState == null ) {
	    
	    System.out.println("@NegoQ->findIndicesArray_Max: NULL Parameters!");
	    return null;
	}
	
	int[] indicesArray = new int[GameAction.NUM_ACTIONS];
	for( int act0 = 0; act0 < GameAction.NUM_ACTIONS; act0++ ) {
	
	    GameAction maxAction = new GameAction();
	    maxAction.setAction( agent0, act0 );
	    maxAction.setAction( agent1, 0 );
	    maxAction.setAction( agent2, indicesMat[act0][0] );
	    double maxValue = getQValue( agentIndex, gameState, maxAction );
	    int maxIndex = 0;
	    for( int act1 = 1; act1 < GameAction.NUM_ACTIONS; act1++ ) {
		
		GameAction jntAction = new GameAction();
		jntAction.setAction( agent0, act0 );
		jntAction.setAction( agent1, act1 );
		jntAction.setAction( agent2, indicesMat[act0][act1] );
		
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
    
    /**
     * find action index in agent0's dimension which satisfies:
     * min_max_min or min_min_max
     */
    private int findIndexAction_Min( int[][] indicesMat, int[] indicesArray, 
	    int agent0, int agent1, int agent2, GameState gameState )
    {
	if( indicesArray == null || indicesMat == null || 
		gameState == null ) {
	    
	    System.out.println("@NegoQ->findIndexAction_Min: NULL Parameters!");
	    return -1;
	}
	
	GameAction minAction = new GameAction();
	minAction.setAction( agent0, 0 );
	minAction.setAction( agent1, indicesArray[0] );
	minAction.setAction( agent2, indicesMat[0][indicesArray[0]]);
	double minValue = getQValue( agentIndex, gameState, minAction );
	int minIndex = 0;
	for( int act0 = 1; act0 < GameAction.NUM_ACTIONS; act0++ ) {
	    
	    GameAction jntAction = new GameAction();
	    jntAction.setAction( agent0, act0 );
	    jntAction.setAction( agent1, indicesArray[act0] );
	    jntAction.setAction( agent2, indicesMat[act0][indicesArray[act0]] );
	    
	    double value = getQValue( agentIndex, gameState, jntAction );
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
    private int findIndexAction_Max( int[][] indicesMat, int[] indicesArray, 
	    int agent0, int agent1, int agent2, GameState gameState )
    {
	if( indicesArray == null || indicesMat == null || 
		gameState == null ) {
	    
	    System.out.println("@NegoQ->findIndexAction_Max: NULL Parameters!");
	    return -1;
	}
	
	GameAction maxAction = new GameAction();
	maxAction.setAction( agent0, 0 );
	maxAction.setAction( agent1, indicesArray[0] );
	maxAction.setAction( agent2, indicesMat[0][indicesArray[0]]);
	double maxValue = getQValue( agentIndex, gameState, maxAction );
	int maxIndex = 0;
	for( int act0 = 1; act0 < GameAction.NUM_ACTIONS; act0++ ) {
	    
	    GameAction jntAction = new GameAction();
	    jntAction.setAction( agent0, act0 );
	    jntAction.setAction( agent1, indicesArray[act0] );
	    jntAction.setAction( agent2, indicesMat[act0][indicesArray[act0]] );
	    
	    double value = getQValue( agentIndex, gameState, jntAction );
	    if( value > maxValue ) {
		
		maxValue = value;
		maxIndex = act0;
	    }
	}
	
	return maxIndex;
    }
}
