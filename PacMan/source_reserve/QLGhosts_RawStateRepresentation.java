package pacman.controllers.learners;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;

import pacman.game.Constants;
import pacman.game.Game;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;

public class QLGhosts extends RLGhosts {

	
	/**
	 * this member means that whether 
	 * the Q-learning ghosts learn in the game 
	 * or act greedily according to a learnt value function
	 */
	private boolean doesLearn = true;
	
	public QLGhosts( Game game, boolean bLearn )
	{
		super(game);
		
		doesLearn = bLearn;
		
		/**
		 * if learning is not required 
		 * then read the value function in the file
		 */
		if( !doesLearn )
			readValueFunction( game );
	}
	
	public QLGhosts( Game game, double alpha, double gamma, double epsilon, 
			boolean bLearn )
	{
		super( game, alpha, gamma, epsilon );
		
		doesLearn = bLearn;
		
		/**
		 * if learning is not required 
		 * then read the value function in the file
		 */
		if( !doesLearn )
			readValueFunction( game );
	}
	
	/**
	 * override the method getMove, 
	 * which returns the move of the ghosts
	 */
	public EnumMap<GHOST,MOVE> getMove(Game game, long timeDue)
	{	
		
		/**
		 * note that a ghost can compute a move 
		 * only after one update is conducted
		 */
		if( hasComputed() )
			return curMoves;
		
		
		curMoves.clear();
		for(GHOST ghostType : GHOST.values())
			if(game.doesGhostRequireAction(ghostType)) {
				
				/**
				 * choose the max move according to the value function
				 */
				MOVE maxMove = getMaxMove( game, ghostType );
				
				/**
				 * do epsilon-greedy
				 */
				MOVE chosenMove = epsilonGreedy( game, ghostType, maxMove );
				
				/**
				 * set the move of the ghost
				 */
				curMoves.put( ghostType, chosenMove );
				
				
				/**
				 * store the last game state
				 */
				lastGames.put( ghostType, game );
			}
		
		return curMoves;
	}
	
	
	/**
	 * Updates the game state: a copy of the game is passed to this method 
	 * and the class variable is updated accordingly.
	 * Also, value function is updated in this method.
	 *
	 * @param game A copy of the current game
	 * @param timeDue The time the next move is due
	 */
	public void update( Game curGame, long timeDue)
	{
		synchronized(this)
		{
			this.game = curGame;
			this.timeDue = timeDue;
			wasSignalled = true;
			hasComputed = false;
			
			
			/**
			 * if learning is required, 
			 * then call the method to update the value function
			 */
			if( doesLearn )
				updateValueFunction( curGame );
			
			notify();
		}
	}
	
	private void updateValueFunction( Game curGame )
	{
		if( lastMove == null )
			return;
		
		/**
		 * update the value function if 
		 * currently the ghost is not in the initial position
		 * 
		 * the member lastGame is not null
		 */
		for(GHOST ghostType : GHOST.values()) {
		
			/**
			 * get the last action of this ghost
			 */
			MOVE ghostLastMov = lastMove.get( ghostType );
			
			if( curGame.getGhostCurrentNodeIndex(ghostType) != curGame.getGhostInitialNodeIndex() && 
					ghostLastMov != MOVE.NEUTRAL ) {
				
				/**
				 * get the last game state
				 */
				Game lastGame = lastGames.get( ghostType );
				
				
				/**
				 * compute the reward in the last step
				 */
				double rwd = computeReward( lastGame, curGame, ghostType, ghostLastMov );
				
				
				/**
				 * use Q-learning rule to update the value function
				 */
				double Qsa = getQValue( lastGame, ghostType, ghostLastMov );
				double maxQp = getMaxQValue( curGame, ghostType );
				Qsa = ( 1 - ALPHA ) * Qsa + ALPHA * ( rwd + GAMMA * maxQp );
				setQValue( lastGame, ghostType, ghostLastMov, Qsa );
			}
		}
		
		
		/**
		 * if the game is over 
		 * then store the value function
		 */
		if( curGame.gameOver() ) {
			
			storeValueFunction( curGame );
		}
	}
	
	/**
	 * compute the reward for a specified ghost
	 */
	private double computeReward( Game lastGame, Game curGame, 
			GHOST ghostType, MOVE lastMove )
	{
		double reward = 0.0;
		
		/**
		 * if the game is over
		 */
		if( curGame.gameOver() ) {
			
			/**
			 * if the game finishes since the pacman is eaten and
			 * has no life
			 */
			if( curGame.wasPacManEaten() ) {
				
				/**
				 * if the pacman is eaten by this ghost
				 */
				if( curGame.ghostEatsPacman == ghostType )
					reward = 20;
				else
					reward = 0.0;
			}
			
			/**
			 * if the time is over, the reward is 0
			 */
			else
				reward = 0.0;
		}
		/**
		 * if the pacman is eaten
		 */
		else if( curGame.wasPacManEaten() ) {
			
			
			if( !lastGame.wasPacManEaten() ) {
				
				/**
				 * if the pacman is eaten by this ghost
				 */
				if( curGame.ghostEatsPacman == ghostType )
					reward = 20;
				else
					reward = 0.0;
			}
			else
				reward = 0.0;
		}
		/**
		 * if the ghost has been eaten
		 */
		else if( curGame.wasGhostEaten( ghostType ) ) {
			
			/**
			 * if the ghost is eaten in the last step
			 */
			if( !lastGame.wasGhostEaten( ghostType ) )
				reward = -10;
			/**
			 * if the ghost had been eaten before
			 */
			else
				reward = 0;
		}
		else {
			
			int ghostLastNodeIndex = lastGame.getGhostCurrentNodeIndex( ghostType );
			int pacmanLastNodeIndex = lastGame.getPacmanCurrentNodeIndex();
			int ghostCurNodeIndex = curGame.getGhostCurrentNodeIndex( ghostType );
			int pacmanCurNodeIndex = curGame.getPacmanCurrentNodeIndex();
			int lastDis = lastGame.getShortestPathDistance( ghostLastNodeIndex, pacmanLastNodeIndex );
			int curDis = curGame.getShortestPathDistance( ghostCurNodeIndex, pacmanCurNodeIndex );
			
			
			/**
			 * we put some knowledge into the reward function.
			 * That is, when the ghost is edible, the ghost 
			 * must go away from the pacman. Otherwise, it 
			 * should move towards the pacman
			 */
			if( lastGame.isGhostEdible(ghostType) ) {
				
				if( curDis < lastDis ) 
					reward = -1;
				else if( curDis > lastDis )
					reward = 1;
				else
					reward = 0;
			}
			else {
				
				if( curDis < lastDis ) 
					reward = 1;
				else if( curDis > lastDis )
					reward = -1;
				else
					reward = 0;
			}
		}
		
		
		return reward;
	}
	
	
	/**
	 * get the max action for a specified ghost in the current game state
	 */
	private int getMaxAction( Game game, GHOST ghostType )
	{
		
		/**
		 * if game is over,
		 * directly return an action
		 */
		if( game.gameOver() )
			return 0;
		
		int numActions = Constants.NUM_GHOST_MOVES;
		
		double maxValue = Double.NEGATIVE_INFINITY;
		for( int act = 0; act < numActions; act++ ) {
			
			double value = getQValue( game, ghostType, act );
			if( value > maxValue ) {
				
				maxValue = value;
			}
		}
		
		/**
		 * if there are multiple max action
		 */
		ArrayList<Integer> maxActions = new ArrayList<Integer>();
		for( int act = 0; act < numActions; act++ ) {
			
			double value = getQValue( game, ghostType, act );
			if( Math.abs(value-maxValue) < 0.00001 ) {
				
				maxActions.add( act );
			}
		}
		
		
		return maxActions.get(random.nextInt(maxActions.size()));
	}
	
	/**
	 * get the max move for a specified ghost in the current game state, 
	 * we can transform the max action to the corresponding MOVE type
	 */
	private MOVE getMaxMove( Game game, GHOST ghostType )
	{
		return MOVE.parseInt( getMaxAction(game, ghostType) );
	}
	
	/**
	 * get the max Q-value for a specified ghost in the current game state
	 */
	private double getMaxQValue( Game game, GHOST ghostType )
	{
		
		/**
		 * if game is over,
		 * directly return 0
		 */
		if( game.gameOver() )
			return 0.0;
		
		int numActions = Constants.NUM_GHOST_MOVES;
		
		double maxValue = Double.NEGATIVE_INFINITY;
		for( int act = 0; act < numActions; act++ ) {
			
			double value = getQValue( game, ghostType, act );
			if( value > maxValue ) {
				
				maxValue = value;
			}
		}
		
		return maxValue;
	}
	
	/**
	 * store the learnt value function into file
	 */
	private void storeValueFunction( Game curGame )
	{
		
		try {
			
			BufferedWriter qWriter = new BufferedWriter(new FileWriter("./valueFunction_QL"+".txt"));
			
			int numEdible = 2;
			int numNodes = curGame.getNumberOfNodes();
			int numGhostActions = Constants.NUM_GHOST_MOVES;
			
			for( int edible = 0; edible < numEdible; edible++ ) 
				for( int pacmanNode = 0; pacmanNode < numNodes; pacmanNode++ )
					for( int ghostNode = 0; ghostNode < numNodes; ghostNode++ )
						for( int ghostAct = 0; ghostAct < numGhostActions; ghostAct++ ) {
								
							double Qvalue = qValues[edible][pacmanNode][ghostNode][ghostAct];
							qWriter.write( String.valueOf(Qvalue) );
							qWriter.newLine();
						}
			
			qWriter.close();
			
		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	private void readValueFunction( Game curGame )
	{
		
		try {
			
			String fileName = "./valueFunction_QL"+".txt";
			BufferedReader qReader = new BufferedReader(new FileReader(fileName));
			
			int numEdible = 2;
			int numNodes = curGame.getNumberOfNodes();
			int numGhostActions = Constants.NUM_GHOST_MOVES;
			int edible = 0;
			int pacmanNode = 0;
			int ghostNode = 0;
			int ghostAct = 0;
			
			String line = "";
			while( (line = qReader.readLine()) != null) {
				
			    if( line.isEmpty() )
					continue;
			    
			    double qValue = Double.parseDouble( line );
			    qValues[edible][pacmanNode][ghostNode][ghostAct] = qValue;
			    
			    /**
			     * move to the next state
			     */
			    ghostAct++;
			    if( ghostAct >= numGhostActions ) {
			    	
			    	ghostAct = 0;
			    	ghostNode++;
			    	if( ghostNode >= numNodes ) {
			    		
			    		ghostNode = 0;
			    		pacmanNode++;
			    		if( pacmanNode >= numNodes ) {
			    			
			    			pacmanNode = 0;
			    			edible++;
			    			if( edible >= numEdible ) {
			    				
			    				break;
			    			}
			    		}
			    	}
			    }
			}
			
			
			qReader.close();
			
		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
}
