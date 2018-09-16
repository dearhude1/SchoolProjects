package pacman.controllers.examples;

import java.util.Random;
import pacman.game.Game;
import pacman.game.Constants.MOVE;
import pacman.controllers.Controller;

/*
 * The Class RandomPacMan.
 */
public final class RandomPacMan extends Controller<MOVE>
{
	private Random rnd=new Random();
	private MOVE[] allMoves=MOVE.values();
	
	/* (non-Javadoc)
	 * @see pacman.controllers.Controller#getMove(pacman.game.Game, long)
	 */
	public MOVE getMove(Game game,long timeDue)
	{
		if( game.doesPacmanRequireAction() ) {
			
			int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();
			MOVE[] availMoves = game.getPossibleMoves( pacmanNodeIndex );
			
			return availMoves[rnd.nextInt(availMoves.length)];
		}
		else
			return game.getPacmanLastMoveMade();
	}
}