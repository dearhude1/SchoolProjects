package pacman.controllers.learners;

import java.util.EnumMap;
import java.util.Random;

import pacman.controllers.Controller;
import pacman.game.Constants;
import pacman.game.Game;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;

/**
 * single-agent reinforcement learning controllers for ghosts
 * 
 * since in the original game, the ghosts do not collapse, 
 * so we can use one single-agent RL controller to control 
 * all ghosts actions, which is similar to parallel learning??
 */
public class RLGhosts extends Controller<EnumMap<GHOST,MOVE>> {

	
	protected static final int GHOST_NOT_EDIBLE = 0;
	protected static final int GHOST_EDIBLE = 1;
	
	/**
	 * the moves of the ghosts in the current step
	 */
	protected EnumMap<GHOST,MOVE> curMoves = new EnumMap<GHOST,MOVE>(GHOST.class);
	
	/**
	 * the moves of the ghosts in the last step
	 */
	//protected EnumMap<GHOST,MOVE> lastMoves = new EnumMap<GHOST,MOVE>(GHOST.class);
	
	/**
	 * the last game state corresponding to each ghost
	 */
	protected EnumMap<GHOST,Game> lastGames = new EnumMap<GHOST,Game>(GHOST.class);
	
	private MOVE[] allMoves = MOVE.values();
	
	
	/**
	 * learning parameters
	 */
    protected double ALPHA = 0.99;//0.99;
    protected double GAMMA = 0.9;//0.95
    protected double EPSILON = 0.01;//0.05;
    
    
	/**
	 * for random numbers
	 */
	protected Random random;
	
	
	/**
	 * Q-value function
	 * dimensions:
	 * dim 1: whether the ghost is edible
	 * dim 2: the current node index of pacman
	 * dim 3: the current node index of the ghost
	 * dim 4: the action of the ghost
	 * 
	 */
	protected double[][][][] qValues;
	
	
	public RLGhosts( Game game )
	{
		
		super();
		
		random = new Random();
		
		//lastMove = new EnumMap<GHOST,MOVE>(GHOST.class);
		
		/**
		 * initialize the value function
		 */
		initializeValueFunction( game );
		
		/**
		 * initialize the last game state
		 */
		for( GHOST ghost : GHOST.values() ) {
			
			lastGames.put( ghost, game );
		}
	}
	
	
	public RLGhosts( Game game, double alpha, double gamma, double epsilon )
	{
		
		super();
		
		random = new Random();
		
		//lastMove = new EnumMap<GHOST,MOVE>(GHOST.class);
		
		ALPHA = alpha;
		GAMMA = gamma;
		EPSILON = epsilon;
		
		/**
		 * initialize the value function
		 */
		initializeValueFunction( game );
		
		/**
		 * initialize the last game state
		 */
		for( GHOST ghost : GHOST.values() ) {
			
			lastGames.put( ghost, game );
		}
	}
	
	/**
	 * implement the method getMove, 
	 * which returns the move of the ghosts
	 */
	public EnumMap<GHOST,MOVE> getMove(Game game, long timeDue)
	{	
		curMoves.clear();
		
		for(GHOST ghostType : GHOST.values())
			if(game.doesGhostRequireAction(ghostType))
				curMoves.put(ghostType, allMoves[random.nextInt(allMoves.length)]);
		
		return curMoves;
	}

	
	/**
	 * Updates the game state: a copy of the game is passed to this method and the class variable is
	 * updated accordingly.
	 *
	 * @param game A copy of the current game
	 * @param timeDue The time the next move is due
	 */
	public void update(Game game, long timeDue)
	{
		super.update(game, timeDue);
		
	}
	
	/**
	 * initialize the vlaue function: qValues
	 * @param game: the copy of the game instance 
	 */
	private void initializeValueFunction( Game game )
	{
		
		/**
		 * check the game instance
		 */
		if( game == null ) {
			
			System.out.println("RLGhosts->initializeValueFunction: NULL Game Instance");
			return;
		}
		
		
		int numEdible = 2;
		int numNodes = game.getNumberOfNodes();
		int numGhostActions = Constants.NUM_GHOST_MOVES;
		
		qValues = new double[numEdible][numNodes][numNodes][numGhostActions];
		
		for( int edible = 0; edible < numEdible; edible++ ) 
			for( int pacmanNode = 0; pacmanNode < numNodes; pacmanNode++ )
				for( int ghostNode = 0; ghostNode < numNodes; ghostNode++ )
					for( int ghostAct = 0; ghostAct < numGhostActions; ghostAct++ ) {
							
						qValues[edible][pacmanNode][ghostNode][ghostAct] = 0.0;//random.nextDouble();
					}
	}
	
	
    /**
     * epsilon-greedy for one ghost, return INT type
     */
    protected int epsilonGreedy( Game game, GHOST ghostType, int maxAction )
    {
    	if( random.nextDouble() < EPSILON ) {
	    
    		return random.nextInt(Constants.NUM_GHOST_MOVES);
    	}
    	else
    		return maxAction;
    }
    
    /**
     * epsilon-greedy for one ghost, return MOVE type
     */
    protected MOVE epsilonGreedy( Game game, GHOST ghostType, MOVE maxMove )
    {
    	if( random.nextDouble() < EPSILON ) {
	    
    		/**
    		 * get all possible moves of the ghost in the current state
    		 */
    		int act = random.nextInt(Constants.NUM_GHOST_MOVES);
    		return MOVE.parseInt( act );
    	}
    	else
    		return maxMove;
    }
	
    /**
     * get the Q-value of a state-action pair for a specified ghost
     * @param game: for querying the game state
     * @param ghostType: the specified ghost
     * @param ghostAct: the action of the ghost
     */
	protected double getQValue( Game game, GHOST ghostType, int ghostAct )
	{
		/**
		 * get the state of the game, namely:
		 * whether the ghost is edible
		 * the node index of the ghost
		 * the node index of pacman
		 * the last move of pacman
		 */
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);
		int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();
		//int pacmanAct = MOVE.parseMoves( game.getPacmanLastMoveMade() );
		int ghostEdible = GHOST_NOT_EDIBLE;
		if( game.isGhostEdible(ghostType) ) 
			ghostEdible = GHOST_EDIBLE;
		
		return qValues[ghostEdible][pacmanNodeIndex][ghostNodeIndex][ghostAct];
		
	}
	
	/**
	 ** get the Q-value of a state-action pair for a specified ghost
     * @param game: for querying the game state
     * @param ghostType: the specified ghost
     * @param ghostMove: the action of the ghost, MOVE type
	 * @return
	 */
	protected double getQValue( Game game, GHOST ghostType, MOVE ghostMove )
	{
		int ghostAct = MOVE.parseMoves( ghostMove );
		return getQValue( game, ghostType, ghostAct );
	}
	
    /**
     * set the Q-value of a state-action pair for a specified ghost
     * @param game: for querying the game state
     * @param ghostType: the specified ghost
     * @param ghostAct: the action of the ghost
     * @param value: the value to be set
     */
	protected void setQValue( Game game, GHOST ghostType, int ghostAct, double value )
	{
		/**
		 * get the state of the game, namely:
		 * whether the ghost is edible
		 * the node index of the ghost
		 * the node index of pacman
		 * the last move of pacman
		 */
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);
		int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();
		//int pacmanAct = MOVE.parseMoves( game.getPacmanLastMoveMade() );
		int ghostEdible = GHOST_NOT_EDIBLE;
		if( game.isGhostEdible(ghostType) ) 
			ghostEdible = GHOST_EDIBLE;
		
		qValues[ghostEdible][pacmanNodeIndex][ghostNodeIndex][ghostAct] = value;
	}
	
    /**
     * set the Q-value of a state-action pair for a specified ghost
     * @param game: for querying the game state
     * @param ghostType: the specified ghost
     * @param ghostMove: the action of the ghost, MOVE type
     * @param value: the value to be set
     */
	protected void setQValue( Game game, GHOST ghostType, MOVE ghostMove, double value ) 
	{
		int ghostAct = MOVE.parseMoves( ghostMove );
		setQValue( game, ghostType, ghostAct, value );
	}
	
	
	
}
