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
				 * then update the value function??
				 */
				if( doesLearn ) {
					
					updateValueFunction( game, ghostType );
				}
				
				/**
				 * store the last game state
				 */
				lastGames.put( ghostType, game );
			}
		
		////////////////////////////////////
		//////////Added by dearhude1////////
		////////////////////////////////////
		hasComputed = true;
		lastMove = curMoves;
		
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
			 * update the value function 
			 * when the pacman is eaten or a ghost is eaten?
			 */
			boolean ghostEaten = false;
			for(GHOST ghostType : GHOST.values() ) {
				
				if( curGame.wasGhostEaten( ghostType ) ) {
					
					ghostEaten = true;
					break;
				}
			}
			if( doesLearn && (curGame.wasPacManEaten() || ghostEaten) ) {
				
				//System.out.println("Update Value Function");
				updateValueFunction( curGame );
			}
			
			
			if( game.gameOver() ) {
				
				storeValueFunction( curGame );
			}
			
			notify();
		}
	}
	
	
	
	private void updateValueFunction( Game curGame )
	{
		if( lastMove == null ) {
			
			System.out.println("Last Move is NULL");
			return;
		}
		
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
	
	
	private void updateValueFunction( Game curGame, GHOST ghostType )
	{
		if( lastMove == null || 
				!lastMove.containsKey(ghostType) )
			return;
		
		/**
		 * get the last action of this ghost
		 */
		MOVE ghostLastMov = lastMove.get( ghostType );
		
		if( curGame.getGhostCurrentNodeIndex(ghostType) != 
				curGame.getGhostInitialNodeIndex() && 
				ghostLastMov != MOVE.NEUTRAL ) {
			
			/**
			 * get the last game state
			 */
			Game lastGame = lastGames.get( ghostType );
			
			
			/**
			 * compute the reward in the last step
			 */
			double rwd = computeReward( lastGame, curGame, 
					ghostType, ghostLastMov );
			
			
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
				if( curGame.ghostEatsPacman == ghostType ) {
					
					//System.out.println("A Big Reward");
					reward = 200;
				}
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
				if( curGame.ghostEatsPacman == ghostType ) {
					
					//System.out.println("A Big Reward");
					reward = 200;
				}
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
			if( !lastGame.wasGhostEaten( ghostType ) ) {
				
				//System.out.println("A Big Negative Reward");
				reward = -200;
			}
			/**
			 * if the ghost had been eaten before
			 */
			else
				reward = 0;
		}
		else {
			
			/**
			 * we put some knowledge into the reward function.
			 * That is, when the ghost is edible, the ghost 
			 * must go away from the pacman. Otherwise, it 
			 * should move towards the pacman
			 */
			int ghostLastNodeIndex = lastGame.getGhostCurrentNodeIndex( ghostType );
			int pacmanLastNodeIndex = lastGame.getPacmanCurrentNodeIndex();
			int ghostCurNodeIndex = curGame.getGhostCurrentNodeIndex( ghostType );
			int pacmanCurNodeIndex = curGame.getPacmanCurrentNodeIndex();
			int lastDis = lastGame.getShortestPathDistance( ghostLastNodeIndex, pacmanLastNodeIndex );
			int curDis = curGame.getShortestPathDistance( ghostCurNodeIndex, pacmanCurNodeIndex );
			
			if( lastGame.isGhostEdible(ghostType) ) {
				
				if( curDis < lastDis ) 
					reward = -0.1;
				else if( curDis > lastDis )
					reward = 0.1;
				else
					reward = 0;
			}
			else {
				
				if( curDis < lastDis ) 
					reward = 0.1;
				else if( curDis > lastDis )
					reward = -0.1;
				else
					reward = 0;
			}
			
			//reward = 0;
		}
		
		
		return reward;
	}
	
	
	/**
	 * get the max MOVE for a specified ghost in the current game state
	 */
	private MOVE getMaxMove( Game game, GHOST ghostType )
	{
		
		/**
		 * if game is over,
		 * directly return an action
		 */
		if( game.gameOver() )
			return MOVE.UP;
		
		
		/**
		 * note that we can only choose available moves
		 */
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);
		if( ghostNodeIndex == game.getGhostInitialNodeIndex() ) {
			
			//System.out.println("QLGhosts->getMaxMove: Ghost "+ghostType+" is in initial place");
			return MOVE.UP;
		}
		else if( ghostNodeIndex == game.getCurrentMaze().lairNodeIndex ) {
			
			//System.out.println("QLGhosts->getMaxMove: Ghost "+ghostType+" is in Lair");
			return MOVE.UP;
		}
		else if( game.isPipe( ghostNodeIndex ) ) {
			
			System.out.println("QLGhosts->getMaxMove: Ghost "+ghostType+" is not in an NPP node");
			return MOVE.UP;
		}
		
		MOVE[] availMoves = game.getPossibleMoves( ghostNodeIndex );
		double maxValue = Double.NEGATIVE_INFINITY;
		for( int movIndex = 0; movIndex < availMoves.length; movIndex++ ) {
			
			double value = getQValue(game, ghostType, availMoves[movIndex]);
			if( value > maxValue ) {
				
				maxValue = value;
			}
		}

		
		/**
		 * if there are multiple max action
		 */
		ArrayList<MOVE> maxMoves = new ArrayList<MOVE>();
		for( int movIndex = 0; movIndex < availMoves.length; movIndex++ ) {
			
			double value = getQValue(game, ghostType, availMoves[movIndex]);
			if( Math.abs(value-maxValue) < 0.00001 ) {
				
				maxMoves.add( availMoves[movIndex] );
			}
		}
				
		
		return maxMoves.get( random.nextInt(maxMoves.size()) );

	}
	
	/**
	 * get the max move for a specified ghost in the current game state, 
	 * we can transform the max MOVE to the corresponding int type
	 */
	private int getMaxAction( Game game, GHOST ghostType )
	{
		return MOVE.parseMoves( getMaxMove(game, ghostType));
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
		
		
		/**
		 * note that we can only choose available moves
		 */
		int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghostType);
		if( ghostNodeIndex == game.getGhostInitialNodeIndex() ) {
			
			//System.out.println("QLGhosts->getMaxQValue: Ghost "+ghostType+" is in initial place");
			return 0.0;
		}
		else if( ghostNodeIndex == game.getCurrentMaze().lairNodeIndex ) {
			
			//System.out.println("QLGhosts->getMaxQValue: Ghost "+ghostType+" is in Lair");
			return 0.0;
		}
		else if( game.isPipe( ghostNodeIndex ) ) {
			
			System.out.println("QLGhosts->getMaxQValue: Ghost "+ghostType+" is not in an NPP node");
			return 0.0;
		}
		
		MOVE[] availMoves = game.getPossibleMoves( ghostNodeIndex );
		double maxValue = Double.NEGATIVE_INFINITY;
		for( int movIndex = 0; movIndex < availMoves.length; movIndex++ ) {
			
			double value = getQValue(game, ghostType, availMoves[movIndex]);
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
			int numNPP = curGame.getCurrentMazeGraph().getNumNPP();
			int numConn = curGame.getCurrentMazeGraph().getNumConnections();
			int numActions = Constants.NUM_REAL_MOVES;
			
			for( int edible = 0; edible < numEdible; edible++ ) 
				for( int connIndex = 0; connIndex < numConn; connIndex++ )
					for( int pacmanAct = 0; pacmanAct < numActions; pacmanAct++ )
						for( int nppIndex = 0; nppIndex < numNPP; nppIndex++ )
							for( int ghostAct = 0; ghostAct < numActions; ghostAct++ ) {
								
								double Qvalue = qValues[edible][connIndex][pacmanAct][nppIndex][ghostAct];
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
			int numNPP = curGame.getCurrentMazeGraph().getNumNPP();
			int numConn = curGame.getCurrentMazeGraph().getNumConnections();
			int numActions = Constants.NUM_REAL_MOVES;
			
			int edible = 0;
			int connIndex = 0;
			int pacmanAct = 0;
			int ghostNPPIndex = 0;
			int ghostAct = 0;
			
			String line = "";
			while( (line = qReader.readLine()) != null) {
				
			    if( line.isEmpty() )
					continue;
			    
			    double qValue = Double.parseDouble( line );
			    qValues[edible][connIndex][pacmanAct][ghostNPPIndex][ghostAct] = qValue;
			    
			    /**
			     * move to the next state
			     */
			    ghostAct++;
			    if( ghostAct >= numActions ) {
			    	
			    	ghostAct = 0;
			    	ghostNPPIndex++;
			    	if( ghostNPPIndex >= numNPP ) {
			    		
			    		ghostNPPIndex = 0;
			    		pacmanAct++;
			    		if( pacmanAct >= numActions ) {
			    			
			    			pacmanAct = 0;
			    			connIndex++;
			    			if( connIndex >= numConn ) {
			    				
			    				connIndex = 0;
				    			edible++;
				    			if( edible >= numEdible ) {
				    				
				    				break;
				    			}
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
