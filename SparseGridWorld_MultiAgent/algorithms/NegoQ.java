package algorithms;

import java.util.ArrayList;
import java.util.Random;

import sun.java2d.pipe.AlphaColorPipe;
import gameGridWorld.GameAction;
import gameGridWorld.GameState;
import gameGridWorld.SparseGridWorld;

public class NegoQ extends MARL
{
    /**
     * store all pure strategy Nash equilibria 
     * in a certain state
     */
    public ArrayList<GameAction> nashEquilActions;
    
    /**
     * store all NSEDAs in a certain state
     */
    public ArrayList<GameAction> nsedaActions;
    
    public ArrayList<GameAction> symmMetaEquilActions;
    
    /**
     * store some meta equilibria in a certain state
     */
    public ArrayList<GameAction> metaEquilActions;
    
    
    public NegoQ( int agIndex )
    {
	super( agIndex );
	
	ArrayList<GameState> allStates = SparseGridWorld.getAllValidStates();
	ArrayList<GameAction> allActions = SparseGridWorld.getAllJointActions();
	vstNums = new int[allStates.size()][allActions.size()];
	for( int stateIndex = 0; stateIndex < allStates.size(); stateIndex++ ) {
	    
	    for( int actionIndex = 0; actionIndex < allActions.size(); actionIndex++ ) {
			
			
		vstNums[stateIndex][actionIndex] = 0;
	    }
	}
	
	nashEquilActions = new ArrayList<GameAction>();
	nsedaActions = new ArrayList<GameAction>();
	symmMetaEquilActions = new ArrayList<GameAction>();
	metaEquilActions = new ArrayList<GameAction>();
	
    }
    
    
    public NegoQ( int agIndex, double alpha, double gamma, double epsilon )
    {
	super( agIndex, alpha, gamma, epsilon );
	
	ArrayList<GameState> allStates = SparseGridWorld.getAllValidStates();
	ArrayList<GameAction> allActions = SparseGridWorld.getAllJointActions();
	vstNums = new int[allStates.size()][allActions.size()];
	for( int stateIndex = 0; stateIndex < allStates.size(); stateIndex++ ) {
	    
	    for( int actionIndex = 0; actionIndex < allActions.size(); actionIndex++ ) {
			
			
		vstNums[stateIndex][actionIndex] = 0;
	    }
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
	//ALPHA *= 0.99958;//0.9958;//0.99958;//
    }
    
    
    public static GameAction negotiation( ArrayList<NegoQ> agents, GameState gameState )
    {
	if( gameState == null || agents == null ) {
	    
	    System.out.println("@NegoQ->negotiation: NULL Parameters!");
	    return null;
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
    

    public void findNEs( ArrayList<GameAction>[] maxSets ) 
    {
	if( maxSets == null || maxSets.length != SparseGridWorld.NUM_AGENTS ) {
	    
	    System.out.println("@NegoQ->findNE: NULL Parameters!");
	    return;
	}
	
	ArrayList<GameAction> maxSet = maxSets[agentIndex];
	
	for( int jntActionIndex = 0; jntActionIndex < maxSet.size(); jntActionIndex++ ) {
	    
	    GameAction jntAction = maxSet.get( jntActionIndex );
	    
	    /*
	     * if it is already known that it is NE
	     */
	    if( nashEquilActions.contains( jntAction ) )
		continue;
	    
	    boolean isNE = true;
	    for( int other = 0; other < SparseGridWorld.NUM_AGENTS; other++ ) {
		
		if( other == agentIndex ) 
		    continue;
		
		if( !maxSets[other].contains(jntAction) ) {
		    
		    isNE = false;
		    break;
		}
	    }
	    
	    if( isNE ) {
		
		if( !nashEquilActions.contains( jntAction ) )
		    nashEquilActions.add( jntAction );
	    }
	}
    }
    

    /**
     * other agents tell this agent an action is an NE
     */
    public void messageNE( GameAction neAction )
    {
	
	if( neAction == null ) 
	    return;
	
	if( !nashEquilActions.contains( neAction ) )
	    nashEquilActions.add( neAction );
    }
    
    public void findNSEDAs( ArrayList<GameAction>[] partDmSets )
    {
	if( partDmSets == null ) {
	 
	    System.out.println("@NegoQ->findNSEDAs: NULL Parameters!");
	    return;
	}
	
	ArrayList<GameAction> partDmSet = partDmSets[agentIndex];
	
	for( int jntActionIndex = 0; jntActionIndex < partDmSet.size(); jntActionIndex++ ) {
	    
	    GameAction jntAction = partDmSet.get( jntActionIndex );
	    
	    /*
	     * if it is already known that it is NSEDA
	     */
	    if( nsedaActions.contains( jntAction ) )
		continue;
	    
	    boolean isNSEDA = true;
	    for( int other = 0; other < SparseGridWorld.NUM_AGENTS; other++ ) {
		
		if( other == agentIndex ) 
		    continue;
		
		if( !partDmSets[other].contains(jntAction) ) {
		    
		    isNSEDA = false;
		    break;
		}
	    }
	    
	    if( isNSEDA ) {
		
		if( !nsedaActions.contains( jntAction ) )
		    nsedaActions.add( jntAction );
	    }
	}
    }
    
    /**
     * other agents tell this agent an action is an NSEDA
     */
    public void messageNSEDA( GameAction nsdeaAction )
    {
	
	if( nsdeaAction == null ) 
	    return;
	
	if( !nsedaActions.contains( nsdeaAction ) )
	    nsedaActions.add( nsdeaAction );
    }
    
    public void findSymmEquils( ArrayList<GameAction>[]  possSymmSets )
    {
	if( possSymmSets == null || 
		possSymmSets.length != SparseGridWorld.NUM_AGENTS ) {
	    
	    System.out.println("@NegoQ->findSymmEquils: NULL Parameters!");
	    return;
	}
	
	ArrayList<GameAction> possSymmSet = possSymmSets[agentIndex];
	
	for( int jntActionIndex = 0; jntActionIndex < possSymmSet.size(); jntActionIndex++ ) {
	    
	    GameAction jntAction = possSymmSet.get( jntActionIndex );
	    
	    /*
	     * if it is already known that it is symmetric equilibrium
	     */
	    if( symmMetaEquilActions.contains( jntAction ) )
		continue;
	    
	    boolean isSymmEq = true;
	    for( int other = 0; other < SparseGridWorld.NUM_AGENTS; other++ ) {
		
		if( other == agentIndex ) 
		    continue;
		
		if( !possSymmSets[other].contains(jntAction) ) {
		    
		    isSymmEq = false;
		    break;
		}
	    }
	    
	    if( isSymmEq ) {
		
		if( !symmMetaEquilActions.contains( jntAction ) )
		    symmMetaEquilActions.add( jntAction );
	    }
	}
    }
    
    /**
     * other agents tell this agent an action is an NSEDA
     */
    public void messageSymmEq( GameAction symmEq )
    {
	
	if( symmEq == null ) 
	    return;
	
	if( !symmMetaEquilActions.contains( symmEq ) )
	    symmMetaEquilActions.add( symmEq );
    }
    
    public void findMetaEquils( ArrayList<GameAction>[] possMetaSets )
    {
	if( possMetaSets == null || possMetaSets.length != SparseGridWorld.NUM_AGENTS ) {
	    
	    System.out.println("@NegoQ->findMetaEquils: NULL Parameters!");
	    return;
	}
	
	ArrayList<GameAction> possMetaSet = possMetaSets[agentIndex];
	
	for( int jntActionIndex = 0; jntActionIndex < possMetaSet.size(); jntActionIndex++ ) {
	    
	    GameAction jntAction = possMetaSet.get( jntActionIndex );
	    
	    /*
	     * if it is already known that it is Meta equilibrium
	     */
	    if( metaEquilActions.contains( jntAction ) )
		continue;
	    
	    boolean isMeta = true;
	    for( int other = 0; other < SparseGridWorld.NUM_AGENTS; other++ ) {
		
		if( other == agentIndex ) 
		    continue;
		
		if( !possMetaSets[other].contains(jntAction) ) {
		    
		    isMeta = false;
		    break;
		}
	    }
	    
	    if( isMeta ) {
		
		if( !metaEquilActions.contains( jntAction ) )
		    metaEquilActions.add( jntAction );
	    }
	}
    }
    
    
    /**
     * other agents tell this agent an action is an NE
     */
    public void messageMeta( GameAction metaAction )
    {
	
	if( metaAction == null ) 
	    return;
	
	if( !metaEquilActions.contains( metaAction ) )
	    metaEquilActions.add( metaAction );
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
	ArrayList<GameAction> othJntActions = generateOtherJntActions( agentIndex );
	for( int listIndex = 0; listIndex < othJntActions.size(); listIndex++ ) {
	    
	    GameAction jntAction = othJntActions.get( listIndex );
	    
	    jntAction.setAction( agentIndex, 0);
	    double maxValue = getQValue( agentIndex, gameState, jntAction );
	    double maxAct = 0;
	    
	    //find the max value
	    for( int act = 1; act < GameAction.NUM_ACTIONS; act++ ) {
		
		jntAction.setAction( agentIndex, act );
		double value = getQValue( agentIndex, gameState, jntAction );
		
		if( value > maxValue ) {
		    
		    maxValue = value;
		    maxAct = act;
		}
	    }
	    
	    //find all max actions
	    for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
		
		GameAction gameAction = new GameAction();
		for( int agIndex = 0; agIndex < SparseGridWorld.NUM_AGENTS; agIndex++ ) {
		    
		    if( agIndex == agentIndex )
			gameAction.setAction( agIndex, act );
		    else
			gameAction.setAction( agIndex, jntAction.getAction(agIndex) );
		}
		
		double value = getQValue( agentIndex, gameState, gameAction );
		if( Math.abs(value-maxValue) < 0.00001 ) {
		    
		    if( !retList.contains( gameAction ) )
			retList.add( gameAction );
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
	
	ArrayList<GameAction> allJntActions = SparseGridWorld.getAllJointActions();
	for( int listIndex = 0; listIndex < allJntActions.size(); listIndex++ ) {
	    
	    GameAction jntAction = allJntActions.get( listIndex );
	    
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
	else if( prefix.length != SparseGridWorld.NUM_AGENTS ) {
	    
	    System.out.println("@NegoQ->getPossibleMetaEquil: Wrong Prefix!");
	    return null;
	}
	
	ArrayList<GameAction> retList = new ArrayList<GameAction>();
	
	
	ArrayList<Integer> leftAgents = new ArrayList<Integer>();
	ArrayList<Integer> rightAgents =  new ArrayList<Integer>();
	for( int stringIndex = 0; stringIndex < prefix.length; stringIndex++ ) {
	    
	    String curString = prefix[stringIndex];
	    leftAgents.add( Integer.parseInt( curString ) );
	}
	
	ArrayList<GameAction> outcomeActions = null;
	while( !leftAgents.isEmpty() ) {
	    
	    int curAgent = leftAgents.remove( 0 );
	    
	    if( curAgent == agentIndex ) {
		
		outcomeActions = findMax( curAgent, outcomeActions, leftAgents, rightAgents, gameState );
	    }
	    else {
		
		outcomeActions = findMin( curAgent, outcomeActions, leftAgents, rightAgents, gameState );
	    }
	    
	    rightAgents.add( curAgent );
	}
	
	if( outcomeActions.size() == 1 ) {
	    
	    GameAction threAction = outcomeActions.remove( 0 );
	    double threValue = getQValue( agentIndex, gameState, threAction );
	    
	    ArrayList<GameAction> allJointActions = SparseGridWorld.getAllJointActions();
	    for( int listIndex = 0; listIndex < allJointActions.size(); listIndex++ ) {
		
		GameAction jntAction = allJointActions.get( listIndex );
		double value = getQValue( agentIndex, gameState, jntAction );
		
		if( value >= threValue ) {
		    
		    if( !retList.contains( jntAction ) ) 
			retList.add( jntAction );
		}
	    }
	}
	else {
	    
	    System.out.println("NegoQ->getPossibleMetaEquil: Cannot find a threhold value");
	}
	
	return retList;
    }
    
    //check
    protected ArrayList<GameAction> findMax( int currentAgent, ArrayList<GameAction> gameActions, 
	    ArrayList<Integer> leftAgents, ArrayList<Integer> rightAgents, GameState gameState )
    {
	
	
	ArrayList<GameAction> retList = new ArrayList<GameAction>();
	
	if( leftAgents.size() == 0 ) {
	    
	    double maxValue = Double.NEGATIVE_INFINITY;
	    GameAction maxAction = new GameAction();
	    GameAction curAction = new GameAction();
	    for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
		
		/**
		 * set the action of the current agent 
		 */
		curAction.setAction( currentAgent, act );
		
		/**
		 * set the actions of the right agents
		 * gameActions must not be null!!
		 */
		for( int index = 0; index < gameActions.size(); index++ ) {
			
		    GameAction gameAction = gameActions.get( index );
			
		    //find the joint action that equals the joint action of all non-right agents
		    if( act == gameAction.getAction(currentAgent) ) {
			
			for( int rightAgentListIndex = 0; rightAgentListIndex < rightAgents.size(); rightAgentListIndex++ ) {
				
			    int rightAgentIndex = rightAgents.get( rightAgentListIndex );
			    curAction.setAction( rightAgentIndex, gameAction.getAction(rightAgentIndex) );
			}
			    
			break;
		    }
		}
		    
		/**
		 * get the value of the action
		 */
		double value = getQValue( agentIndex, gameState, curAction );
		if( value > maxValue ) {
		    
		    maxValue = value;
		    for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
			
			maxAction.setAction( agentIndex, curAction.getAction(agentIndex) );
		    }
		}
	    }
	    
	    retList.add( maxAction );
	    return retList;
	}
	
	/**
	 * the joint action list must contain all joint actions 
	 * of the left agents!!!
	 */
	ArrayList<GameAction> othJntActions = generateJointActions( leftAgents );
	
	for( int listIndex = 0; listIndex < othJntActions.size(); listIndex++ ) {
	    
	    GameAction curAction = othJntActions.get( listIndex );
	    

	    double maxValue = Double.NEGATIVE_INFINITY;
	    int maxAct = -1;
	    GameAction maxAction = new GameAction();
	    for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
		
		/**
		 * set the action of the current agent 
		 */
		curAction.setAction( currentAgent, act );
		
		/**
		 * set the actions of the right agents
		 */
		if( gameActions != null ) {
		    
		    for( int index = 0; index < gameActions.size(); index++ ) {
			
			GameAction gameAction = gameActions.get( index );
			
			//find the joint action that equals the joint action of all non-right agents
			if( act != gameAction.getAction(currentAgent) )
			    continue;
			boolean equal = true;
			for( int leftAgentListIndex = 0; leftAgentListIndex < leftAgents.size(); leftAgentListIndex++ ) {
			   
			    int leftAgentIndex = leftAgents.get( leftAgentListIndex );
			    if( curAction.getAction(leftAgentIndex) != gameAction.getAction( leftAgentIndex ) ) {
				
				equal = false;
				break;
			    }
			}
			
			//set the right actions 
			if( equal ) {
			    
			    for( int rightAgentListIndex = 0; rightAgentListIndex < rightAgents.size(); rightAgentListIndex++ ) {
				
				int rightAgentIndex = rightAgents.get( rightAgentListIndex );
				curAction.setAction( rightAgentIndex, gameAction.getAction(rightAgentIndex) );
			    }
			    
			    break;
			}
		    }
		}
		
		/**
		 * get the value of the action
		 */
		double value = getQValue( agentIndex, gameState, curAction );
		if( value > maxValue ) {
		    
		    maxValue = value;
		    for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
			
			maxAction.setAction( agentIndex, curAction.getAction(agentIndex) );
		    }
		}
	    }
	    
	    //add this max action to the ret list
	    retList.add( maxAction );
	}
	
	return retList;
    }
    
    //check
    protected ArrayList<GameAction> findMin( int currentAgent, ArrayList<GameAction> gameActions, 
	    ArrayList<Integer> leftAgents, ArrayList<Integer> rightAgents, GameState gameState )
    {
	
	ArrayList<GameAction> retList = new ArrayList<GameAction>();
	
	if( leftAgents.size() == 0 ) {
	    
	    double minValue = Double.POSITIVE_INFINITY;
	    GameAction minAction = new GameAction();
	    GameAction curAction = new GameAction();
	    for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
		
		/**
		 * set the action of the current agent 
		 */
		curAction.setAction( currentAgent, act );
		
		/**
		 * set the actions of the right agents
		 * gameActions must not be null!!
		 */
		for( int index = 0; index < gameActions.size(); index++ ) {
			
		    GameAction gameAction = gameActions.get( index );
			
		    //find the joint action that equals the joint action of all non-right agents
		    if( act == gameAction.getAction(currentAgent) ) {
			
			for( int rightAgentListIndex = 0; rightAgentListIndex < rightAgents.size(); rightAgentListIndex++ ) {
				
			    int rightAgentIndex = rightAgents.get( rightAgentListIndex );
			    curAction.setAction( rightAgentIndex, gameAction.getAction(rightAgentIndex) );
			}
			    
			break;
		    }
		}
		    
		/**
		 * get the value of the action
		 */
		double value = getQValue( agentIndex, gameState, curAction );
		if( value < minValue ) {
		    
		    minValue = value;
		    for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
			
			minAction.setAction( agentIndex, curAction.getAction(agentIndex) );
		    }
		}
	    }
	    
	    retList.add( minAction );
	    return retList;
	}
	
	
	/**
	 * the joint action list must contain all joint actions 
	 * of the left agents!!!
	 */
	ArrayList<GameAction> othJntActions = generateJointActions( leftAgents );
	
	for( int listIndex = 0; listIndex < othJntActions.size(); listIndex++ ) {
	    
	    GameAction curAction = othJntActions.get( listIndex );
	    

	    double minValue = Double.POSITIVE_INFINITY;
	    GameAction minAction = new GameAction();
	    for( int act = 0; act < GameAction.NUM_ACTIONS; act++ ) {
		
		/**
		 * set the action of the current agent 
		 */
		curAction.setAction( currentAgent, act );
		
		/**
		 * set the actions of the right agents
		 */
		if( gameActions != null ) {
		    
		    for( int index = 0; index < gameActions.size(); index++ ) {
			
			GameAction gameAction = gameActions.get( index );
			
			//find the joint action that equals the joint action of all non-right agents
			if( act != gameAction.getAction(currentAgent ) )
			    continue;
			boolean equal = true;
			for( int leftAgentListIndex = 0; leftAgentListIndex < leftAgents.size(); leftAgentListIndex++ ) {
			   
			    int leftAgentIndex = leftAgents.get( leftAgentListIndex );
			    if( curAction.getAction(leftAgentIndex) != gameAction.getAction( leftAgentIndex ) ) {
				
				equal = false;
				break;
			    }
			}
			
			//set the right actions 
			if( equal ) {
			    
			    for( int rightAgentListIndex = 0; rightAgentListIndex < rightAgents.size(); rightAgentListIndex++ ) {
				
				int rightAgentIndex = rightAgents.get( rightAgentListIndex );
				curAction.setAction( rightAgentIndex, gameAction.getAction(rightAgentIndex) );
			    }
			    
			    break;
			}
		    }
		}
		
		/**
		 * get the value of the action
		 */
		double value = getQValue( agentIndex, gameState, curAction );
		if( value < minValue ) {
		    
		    minValue = value;
		    for( int agentIndex = 0; agentIndex < SparseGridWorld.NUM_AGENTS; agentIndex++ ) {
			
			minAction.setAction( agentIndex, curAction.getAction(agentIndex) );
		    }
		}
	    }
	    
	    //add this max action to the ret list
	    retList.add( minAction );
	}
	
	return retList;
    }
    
    
    public ArrayList<GameAction> getPossibleSymmEquilSet( GameState gameState )
    {
	if( gameState == null ) {
	    
	    System.out.println("@NegoQ->getPossibleSymmEquilSet: NULL Parameters!");
	    return null;
	}
	
	String[] prefix = new String[SparseGridWorld.NUM_AGENTS];
	prefix[0] = String.valueOf(agentIndex);
	int arrayIndex = 1;
	for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
	    
	    if( agent == agentIndex ) 
		continue;
	    
	    prefix[arrayIndex] = String.valueOf( agent );
	    arrayIndex++;
	}
	
	
	return getPossibleMetaEquil(gameState, prefix);
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
	    return null;
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
    
    
    protected void visit( GameState curState, GameAction curAction )
    {
	if( curState == null || curAction == null ) {
	    
	    System.out.println("@MARL->visit: Wrong Parameters!");
	    return;
	}
	
	/**/
	int stateIndex = curState.getIndex();//SparseGridWorld.queryStateIndex( gameState );
	int actionIndex = SparseGridWorld.queryJointActionIndex( curAction );
	
	vstNums[stateIndex][actionIndex] += 1;
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
	
	int[] indicesArray = new int[GameAction.NUM_ACTIONS];
	for( int act0 = 0; act0 < GameAction.NUM_ACTIONS; act0++ ) {
	
	    GameAction minAction = new GameAction();
	    minAction.setAction( agent0, act0 );
	    minAction.setAction( agent1, 0 );
	    double minValue = getQValue( agentIndex, gameState, minAction );
	    int minIndex = 0;
	    for( int act1 = 1; act1 < GameAction.NUM_ACTIONS; act1++ ) {
		
		GameAction jntAction = new GameAction();
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
     * find action indices in agent1's dimeion which satisfy:
     * min_max
     */
    private int[] findIndicesArray_Max( int agent0, int agent1, GameState gameState )
    {
	if( gameState == null ) {
	    
	    System.out.println("@NegoQ->findIndicesArray_Max: NULL Parameters!");
	    return null;
	}
	
	int[] indicesArray = new int[GameAction.NUM_ACTIONS];
	for( int act0 = 0; act0 < GameAction.NUM_ACTIONS; act0++ ) {
	
	    GameAction maxAction = new GameAction();
	    maxAction.setAction( agent0, act0 );
	    maxAction.setAction( agent1, 0 );
	    double maxValue = getQValue( agentIndex, gameState, maxAction );
	    int maxIndex = 0;
	    for( int act1 = 1; act1 < GameAction.NUM_ACTIONS; act1++ ) {
		
		GameAction jntAction = new GameAction();
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
	
	GameAction minAction = new GameAction();
	minAction.setAction( agent0, 0 );
	minAction.setAction( agent1, indicesArray[0] );
	double minValue = getQValue( agentIndex, gameState, minAction );
	int minIndex = 0;
	for( int act0 = 1; act0 < GameAction.NUM_ACTIONS; act0++ ) {
	    
	    GameAction jntAction = new GameAction();
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
	
	GameAction maxAction = new GameAction();
	maxAction.setAction( agent0, 0 );
	maxAction.setAction( agent1, indicesArray[0] );
	double maxValue = getQValue( agentIndex, gameState, maxAction );
	int maxIndex = 0;
	for( int act0 = 1; act0 < GameAction.NUM_ACTIONS; act0++ ) {
	    
	    GameAction jntAction = new GameAction();
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
    
    public ArrayList<GameAction> getNEs()
    {
	
	return nashEquilActions;
    }
    
    public void setNEs( ArrayList<GameAction> neActions )
    {
	if( neActions == null ) {
	    
	    return;
	}
	
	nashEquilActions.clear();
	nashEquilActions.addAll( neActions );
    }
    
    public ArrayList<GameAction> getNSEDAs()
    {
	return nsedaActions;
    }
    
    public void setNSEDAs( ArrayList<GameAction> nsedas )
    {
	if( nsedas == null ) {
	    
	    return;
	}
	
	nsedaActions.clear();
	nsedaActions.addAll( nsedas );
    }
    
    public ArrayList<GameAction> getSymmEqs()
    {
	return symmMetaEquilActions;
    }
    
    public void setSymmEqs( ArrayList<GameAction> symmEqs )
    {
	
	if( symmEqs == null ) {
	    
	    return;
	}
	
	symmMetaEquilActions.clear();
	symmMetaEquilActions.addAll( symmEqs );
    }
    
    public ArrayList<GameAction> getMetaEqs()
    {
	return metaEquilActions;
    }
    
    public void setMetaEqs( ArrayList<GameAction> metaEqs )
    {
	if( metaEqs == null ) {
	    
	    return;
	}
	
	metaEquilActions.clear();
	metaEquilActions.addAll( metaEqs );
    }
    
    
    
    
}
