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
	 * dim 2: the current MazeGraph Connection index of pacman
	 * dim 3: the moving direction of pacman
	 * dim 4: the current NonPipe Node index of the ghost
	 * dim 5: the action of the ghost
	 * 
	 * Note that we use an abstracted state representation
	 * Ghosts and Pacman can take actions only in NonPipeNode
	 * 
	 */
	protected double[][][][][] qValues;
	
	
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
		int numNPP = game.getCurrentMazeGraph().getNumNPP();
		int numConn = game.getCurrentMazeGraph().getNumConnections();
		//int numNodes = game.getNumberOfNodes();
		int numActions = Constants.NUM_REAL_MOVES;
		
		qValues = new double[numEdible][numConn][numActions][numNPP][numActions];
		
		for( int edible = 0; edible < numEdible; edible++ ) 
			for( int connIndex = 0; connIndex < numConn; connIndex++ )
				for( int pacmanAct = 0; pacmanAct < numActions; pacmanAct++ )
					for( int nppIndex = 0; nppIndex < numNPP; nppIndex++ )
						for( int ghostAct = 0; ghostAct < numActions; ghostAct++ ) {
							
						qValues[edible][connIndex][pacmanAct][nppIndex][ghostAct] = 0.0;//random.nextDouble();
					}
	}
	
	
    /**
     * epsilon-greedy for one ghost, return INT type
     */
    protected int epsilonGreedy( Game game, GHOST ghostType, int maxAction )
    {
    	
		/**
		 * note that we can only choose available moves
		 */
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);
		if( ghostNodeIndex == game.getGhostInitialNodeIndex() ) {
			
			//System.out.println("RLGhosts->epsilonGreedy: Ghost "+ghostType+" is in initial place");
			return MOVE.parseMoves( MOVE.UP );
		}
		else if( ghostNodeIndex == game.getCurrentMaze().lairNodeIndex ) {
			
			//System.out.println("RLGhosts->epsilonGreedy: Ghost "+ghostType+" is in Lair");
			return MOVE.parseMoves( MOVE.UP );
		}
		else if( game.isPipe( ghostNodeIndex ) ) {
			
			System.out.println("RLGhosts->epsilonGreedy: Ghost "+ghostType+" is not in an NPP node");
			return MOVE.parseMoves( MOVE.UP );
		}
    	
		else if( random.nextDouble() < EPSILON ) {
	    
    		/**
    		 * note that we can only choose available moves
    		 */
    		int nodeIndex = game.getGhostCurrentNodeIndex(ghostType);
    		MOVE[] availMoves = game.getPossibleMoves( nodeIndex );
    		int movIndex = random.nextInt( availMoves.length );
    		
    		return MOVE.parseMoves( availMoves[movIndex] );
    	}
    	else
    		return maxAction;
    }
    
    /**
     * epsilon-greedy for one ghost, return MOVE type
     */
    protected MOVE epsilonGreedy( Game game, GHOST ghostType, MOVE maxMove )
    {
		/**
		 * note that we can only choose available moves
		 */
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);
		
		if( ghostNodeIndex == game.getGhostInitialNodeIndex() ) {
			
			//System.out.println("RLGhosts->epsilonGreedy: Ghost "+ghostType+" is in initial place");
			return MOVE.UP;
		}
		else if( ghostNodeIndex == game.getCurrentMaze().lairNodeIndex ) {
			
			//System.out.println("RLGhosts->epsilonGreedy: Ghost "+ghostType+" is in Lair");
			return MOVE.UP;
		}
		else if( game.isPipe( ghostNodeIndex ) ) {
			
			System.out.println("RLGhosts->epsilonGreedy: Ghost "+ghostType+" is not in an NPP node");
			return MOVE.UP;
		}
		
		else if( random.nextDouble() < EPSILON ) {
	    
    		/**
    		 * note that we can only choose available moves
    		 */
    		int nodeIndex = game.getGhostCurrentNodeIndex(ghostType);
    		MOVE[] availMoves = game.getPossibleMoves( nodeIndex );
    		int movIndex = random.nextInt( availMoves.length );
    		
    		return availMoves[movIndex];
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
		 * if the ghost is not in an NPP node, return 0
		 */
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);
		if( ghostNodeIndex == game.getGhostInitialNodeIndex() ) {
			
			//System.out.println("RLGhosts->getQValue: Ghost "+ghostType+" is in initial place");
			return 0;
		}
		else if( ghostNodeIndex == game.getCurrentMaze().lairNodeIndex ) {
			
			//System.out.println("RLGhosts->getQValue: Ghost "+ghostType+" is in Lair");
			return 0;
		}
		else if( game.isPipe( ghostNodeIndex ) ) {
			
			System.out.println("RLGhosts->getQValue: Ghost "+ghostType+" is not in an NPP node");
			return 0;
		}
		
		/**
		 * get the state of the game, namely:
		 * whether the ghost is edible
		 * the connection index of pacman
		 * the move direction of pacman
		 * the NPP index of the ghost
		 */
		
		//query the connection index of pacman
		int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();
		MOVE pacmanMov = game.getPacmanLastMoveMade(); //last move?
		
		int pacmanConnIndex = game.getCurrentMazeGraph().queryConnectionIndex( game, 
				pacmanNodeIndex, pacmanMov );
		
		//the action of the pacman		
		int pacmanAct = MOVE.parseMoves( pacmanMov );
		
		//whether the ghost is edible
		int ghostEdible = GHOST_NOT_EDIBLE;
		if( game.isGhostEdible(ghostType) ) 
			ghostEdible = GHOST_EDIBLE;
		
		//the NPP index of ghost
		int ghostNPPIndex = game.getCurrentMazeGraph().queryNPPIndex( game, 
				ghostNodeIndex );
		
		return qValues[ghostEdible][pacmanConnIndex][pacmanAct][ghostNPPIndex][ghostAct];
		
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
		 * if the ghost is not in an NPP node, return 0
		 */
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);
		
		if( ghostNodeIndex == game.getGhostInitialNodeIndex() ) {
			
			//System.out.println("RLGhosts->setQValue: Ghost "+ghostType+" is in initial place");
			return;
		}
		else if( ghostNodeIndex == game.getCurrentMaze().lairNodeIndex ) {
			
			//System.out.println("RLGhosts->setQValue: Ghost "+ghostType+" is in Lair");
			return;
		}
		else if( game.isPipe( ghostNodeIndex ) ) {
			
			System.out.println("RLGhosts->setQValue: Ghost "+ghostType+" is not in an NPP node");
			return;
		}
		
		/**
		 * get the state of the game, namely:
		 * whether the ghost is edible
		 * the connection index of pacman
		 * the move direction of pacman
		 * the NPP index of the ghost
		 */
		
		//query the connection index of pacman
		int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();
		MOVE pacmanMov = game.getPacmanLastMoveMade(); //last move?
		int pacmanConnIndex = game.getCurrentMazeGraph().queryConnectionIndex( game, 
				pacmanNodeIndex, pacmanMov );
		
		//the action of the pacman		
		int pacmanAct = MOVE.parseMoves( pacmanMov );
		
		//whether the ghost is edible
		int ghostEdible = GHOST_NOT_EDIBLE;
		if( game.isGhostEdible(ghostType) ) 
			ghostEdible = GHOST_EDIBLE;
		
		//the NPP index of ghost
		int ghostNPPIndex = game.getCurrentMazeGraph().queryNPPIndex( game, 
				ghostNodeIndex );
		
		qValues[ghostEdible][pacmanConnIndex][pacmanAct][ghostNPPIndex][ghostAct] = value;
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
