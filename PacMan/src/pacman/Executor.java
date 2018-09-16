package pacman;

import ilog.concert.cppimpl.ostream;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Random;
import pacman.controllers.Controller;
import pacman.controllers.HumanController;
import pacman.controllers.KeyBoardInput;
import pacman.controllers.examples.AggressiveGhosts;
import pacman.controllers.examples.Legacy;
import pacman.controllers.examples.Legacy2TheReckoning;
import pacman.controllers.examples.NearestPillPacMan;
import pacman.controllers.examples.NearestPillPacManVS;
import pacman.controllers.examples.RandomGhosts;
import pacman.controllers.examples.RandomNonRevPacMan;
import pacman.controllers.examples.RandomPacMan;
import pacman.controllers.examples.StarterGhosts;
import pacman.controllers.examples.StarterPacMan;
import pacman.controllers.learners.marl.ECEQLambda;
import pacman.controllers.learners.marl.GreedyGNegoQ;
import pacman.controllers.learners.marl.GreedyGNegoQVFT;
import pacman.controllers.learners.marl.GreedyGUCEQ;
import pacman.controllers.learners.marl.GreedyGUCEQVFT;
import pacman.controllers.learners.marl.NegoQLambda;
import pacman.controllers.learners.marl.NegoQLambdaVFT;
import pacman.controllers.learners.marl.PCEQLambda;
import pacman.controllers.learners.marl.UCEQLambda;
import pacman.controllers.learners.marl.UCEQLambdaVFT;
import pacman.controllers.learners.sarl.GreedyGQ;
import pacman.controllers.learners.sarl.LSTDQ;
import pacman.controllers.learners.sarl.NQLambda;
import pacman.controllers.learners.sarl.QLambda;
import pacman.controllers.learners.sarl.RLSTDQ;
import pacman.controllers.learners.sarl.SingleLearnerQLambda;
import pacman.controllers.learners.sarl.SingleLearnerRLSTDQ;
import pacman.game.Game;
import pacman.game.GameView;
import pacman.game.Constants.GHOST;

import static pacman.game.Constants.*;

/**
 * This class may be used to execute the game in timed or un-timed modes, with or without
 * visuals. Competitors should implement their controllers in game.entries.ghosts and 
 * game.entries.pacman respectively. The skeleton classes are already provided. The package
 * structure should not be changed (although you may create sub-packages in these packages).
 */
@SuppressWarnings("unused")
public class Executor
{	
	/**
	 * The main method. Several options are listed - simply remove comments to use the option you want.
	 *
	 * @param args the command line arguments
	 */
	public static void main(String[] args)
	{
		Executor exec=new Executor();

		/*
		 * 
		 */
		//run multiple games in batch mode - good for testing.
		int numTrials=1;
		exec.runExperiment(new RandomNonRevPacMan(), null, numTrials);
		 
		
		/*
		//run a game in synchronous mode: game waits until controllers respond.
		int delay=5;
		boolean visual= false;//true;
		exec.runGame(new RandomPacMan(),new RandomGhosts(),visual,delay);
  		*/ 
		
		///*
		//run the game in asynchronous mode.
		//boolean visual= true;
//		exec.runGameTimed(new NearestPillPacMan(),new AggressiveGhosts(),visual);
		//exec.runGameTimed(new RandomNonRevPacMan(), null, true);
//		exec.runGameTimed(new HumanController(new KeyBoardInput()),new StarterGhosts(),visual);	
		//*/
		
		/*
		//run the game in asynchronous mode but advance as soon as both controllers are ready  - this is the mode of the competition.
		//time limit of DELAY ms still applies.
		boolean visual=true;
		boolean fixedTime=false;
		exec.runGameTimedSpeedOptimised(new RandomPacMan(),new RandomGhosts(),fixedTime,visual);
		*/
		
		/*
		//run game in asynchronous mode and record it to file for replay at a later stage.
		boolean visual=true;
		String fileName="replay.txt";
		exec.runGameTimedRecorded(new HumanController(new KeyBoardInput()),new RandomGhosts(),visual,fileName);
		//exec.replayGame(fileName,visual);
		 */
	}
	
    /**
     * For running multiple games without visuals. This is useful to get a good idea of how well a controller plays
     * against a chosen opponent: the random nature of the game means that performance can vary from game to game. 
     * Running many games and looking at the average score (and standard deviation/error) helps to get a better
     * idea of how well the controller is likely to do in the competition.
     *
     * @param pacManController The Pac-Man controller
     * @param ghostController The Ghosts controller
     * @param trials The number of trials to be executed
     */
    public void runExperiment(Controller<MOVE> pacManController,Controller<EnumMap<GHOST,MOVE>> ghostController,int trials)
    {
    	double avgScore=0;
    	Random rnd=new Random(0);
		Game game;
		
		//for( GHOST ghostType : GHOST.values() ) {
			
			for(int i=0;i<trials;i++)
			{			
				game=new Game(rnd.nextLong());
				
				/**
				 * create the ghost controller here
				 */
				//ghostController = new QLGhosts( game, 0.1, 0.9, 0.02, true );
				//ghostController = new QLambda( game, 0.01, 0.9,//0.9, 
						//0.0, 0.1, false );
				//ghostController = new NQLambda( game, 0.01, 0.9,//0.9, 
						//0.1, 0.1, false );
				//ghostController = new RLSTDQ( game, 0.01, 0.9, 0.0, 0.1, false );
				//ghostController = new LSTDQ( game, 0.01, 0.9, 
						//0.1, 0.1, true );
				//ghostController = new PCEQLambdaGhostsWithFA( game, 0.01, 
						//0.9, 0.1, 0.1, false );
				//ghostController = new NegoQLambda( game, 0.01, 
						//0.9, 0.1, 0.1, true );
				//ghostController = new SingleLearnerGhosts (game, GHOST.BLINKY, 
						//0.01, 0.9, 0, 0.1, false );
				//ghostController = new SingleLearnerRLSTDQ(game, GHOST.SUE, 
						//0.01, 0.9, 0.0, 0.1, false);
				//ghostController = new NegoQLambdaVFT(game, 0.01, 0.9, 0.05, 0.05, false);
				//ghostController = new GreedyGQ(game, 0.01, 0.9, 0.0, 0.03, false);
				
				//ghostController = new GreedyGNegoQ(game, 0.01, 0.9, 0.04, false);
				
				//ghostController = new GreedyGUCEQ(game, 0.01, 0.9, 0.05, false);
				//ghostController = new GreedyGUCEQVFT(game, 0.01, 0.9, 0.04, false);
				
				//ghostController = new UCEQLambdaVFT(game, 0.01, 0.9, 0.1, 0.1, true);
				
				ghostController = new GreedyGNegoQVFT(game, 0.01, 0.9, 0.04, false);
				
				//ghostController = new Legacy2TheReckoning();
				while(!game.gameOver())
				{
			        /**
			         * update the value function for the ghosts
			         */
			        ghostController.update( game.copy(), System.currentTimeMillis()+DELAY );
			        
			        MOVE pacmanMove = pacManController.getMove(game.copy(),System.currentTimeMillis()+DELAY);
			        EnumMap<GHOST, MOVE> ghostMoves = ghostController.getMove(game.copy(),System.currentTimeMillis()+DELAY);
			        game.advanceGame( pacmanMove, ghostMoves );
			        
				}
				
				ghostController.update( game.copy(), System.currentTimeMillis()+DELAY );
				
							
				/**
				 * store the score in each episode 
				 * also compute the standard deviation
				 *
				double[] scorePerEP = game.getScorePerEP();
				double[] scores = new double[Game.EPISODE_NUM];
				for( int ep = Game.EPISODE_NUM - 1; ep > 0; ep-- ) {
					
					scores[ep] = scorePerEP[ep] - scorePerEP[ep-1];
				}
				scores[0] = scorePerEP[0];
				
				avgScore = scorePerEP[Game.EPISODE_NUM-1] / Game.EPISODE_NUM;
				System.out.println(i+"\t average score "+avgScore);
				
				double squaredSum = 0;
				for( int ep = 0; ep < Game.EPISODE_NUM; ep++ ) {
					
					double diff = scores[ep] - avgScore;
					squaredSum += diff * diff;
				}
				squaredSum /= Game.EPISODE_NUM;
				double sigma = Math.sqrt(squaredSum);
				System.out.println("Standard Deviation "+sigma);
				
				try {
					
					BufferedWriter bufWriter = new BufferedWriter(new 
							FileWriter("./scores_"+".csv"));
					for( int ep = 0; ep < Game.EPISODE_NUM; ep++ ) {
						
						double score = scores[ep];
						bufWriter.write(score+", ");
					}
					bufWriter.close();
					
					bufWriter = new BufferedWriter(new 
							FileWriter("./data_"+".txt"));
					bufWriter.write("Average Score "+avgScore);
					bufWriter.newLine();
					bufWriter.write("Standard Deviation "+sigma);
					bufWriter.close();
				} 
				catch (Exception e) {
					// TODO: handle exception
				}
				*/
				
				avgScore = game.getScore() / ((double)Game.EPISODE_NUM);
				System.out.println(i+"\t average score "+avgScore);
			}
			
		//}
		

			
    }
	
	/**
	 * Run a game in asynchronous mode: the game waits until a move is returned. In order to slow thing down in case
	 * the controllers return very quickly, a time limit can be used. If fasted gameplay is required, this delay
	 * should be put as 0.
	 *
	 * @param pacManController The Pac-Man controller
	 * @param ghostController The Ghosts controller
	 * @param visual Indicates whether or not to use visuals
	 * @param delay The delay between time-steps
	 */
	public void runGame(Controller<MOVE> pacManController,Controller<EnumMap<GHOST,MOVE>> ghostController,boolean visual,int delay)
	{
		Game game=new Game(0);
		
		GameView gv=null;
		
		if(visual)
			gv=new GameView(game).showGame();
		
		while(!game.gameOver())
		{
	        game.advanceGame(pacManController.getMove(game.copy(),-1),ghostController.getMove(game.copy(),-1));
	        
	        try{Thread.sleep(delay);}catch(Exception e){}
	        
	        if(visual)
	        	gv.repaint();
		}
	}
	
	/**
     * Run the game with time limit (asynchronous mode). This is how it will be done in the competition. 
     * Can be played with and without visual display of game states.
     *
     * @param pacManController The Pac-Man controller
     * @param ghostController The Ghosts controller
	 * @param visual Indicates whether or not to use visuals
     */
    public void runGameTimed(Controller<MOVE> pacManController,Controller<EnumMap<GHOST,MOVE>> ghostController,boolean visual)
	{
    	
    	double avgScore = 0;
    	
		Game game=new Game(0);
		
		GameView gv=null;
		
		if(visual)
			gv=new GameView(game).showGame();
		
		if(pacManController instanceof HumanController)
			gv.getFrame().addKeyListener(((HumanController)pacManController).getKeyboardInput());
				
		/**
		 * create the ghost controller here
		 */
		//ghostController = new QLGhosts( game, 0.9, 0.99999, 0.01, false );
		//ghostController = new StarterGhosts();
		//ghostController = new QLambdaGhostsWithFA( game, 0.1, 0.9, 
				//0.01, 0.1, false );
		//ghostController = new NQLambdaGhostsWithFA( game, 0.1, 0.9, 
				//0.01, 0.1, false );
		//ghostController = new RLSTDQ( game, 0.1, 0.9,//0.9, 
				//0.01, 0.1, false );
		//ghostController = new NegoQLambdaGhostsWithFA( game, 0.01, 
				//0.9, 0, 0.1, false );
		//ghostController = new SingleLearnerQLambda (game, GHOST.BLINKY, 
				//0.01, 0.9, 0, 0.1, false );
		//ghostController = new SingleLearnerRLSTDQ(game, GHOST.BLINKY, 
				//0.01, 0.9, 0, 0.1, false);
		
		ghostController = new GreedyGQ(game, 0.01, 0.9, 0, 0.1, false);
		
		//ghostController = new StarterGhosts();
		
		new Thread(pacManController).start();
		new Thread(ghostController).start();
		
		while(!game.gameOver())
		{
			pacManController.update(game.copy(),System.currentTimeMillis()+DELAY);
			ghostController.update(game.copy(),System.currentTimeMillis()+DELAY);

			try
			{
				Thread.sleep(DELAY);
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}

	        game.advanceGame(pacManController.getMove(),ghostController.getMove());	   
	        
	        if(visual)
	        	gv.repaint();
	        
	        
	        /**
	         * if the game is over
	         */
	        if( game.gameOver() ) {
	        	
	        	System.out.println("Update right after the game is over");
				pacManController.update(game.copy(),System.currentTimeMillis()+DELAY);
				ghostController.update(game.copy(),System.currentTimeMillis()+DELAY);
	        }
		}
		
		avgScore+=game.getScore();
		System.out.println("Total Score"+game.getScore());
		
		pacManController.terminate();
		ghostController.terminate();
	}
	
    /**
     * Run the game in asynchronous mode but proceed as soon as both controllers replied. The time limit still applies so 
     * so the game will proceed after 40ms regardless of whether the controllers managed to calculate a turn.
     *     
     * @param pacManController The Pac-Man controller
     * @param ghostController The Ghosts controller
     * @param fixedTime Whether or not to wait until 40ms are up even if both controllers already responded
	 * @param visual Indicates whether or not to use visuals
     */
    public void runGameTimedSpeedOptimised(Controller<MOVE> pacManController, 
    		Controller<EnumMap<GHOST,MOVE>> ghostController, 
    		boolean fixedTime, boolean visual)
 	{
 		Game game=new Game(0);
 		
 		GameView gv=null;
 		
 		if(visual)
 			gv=new GameView(game).showGame();
 		
 		if(pacManController instanceof HumanController)
 			gv.getFrame().addKeyListener(((HumanController)pacManController).getKeyboardInput());
 				
 		new Thread(pacManController).start();
 		new Thread(ghostController).start();
 		
 		while(!game.gameOver())
 		{
 			pacManController.update(game.copy(),System.currentTimeMillis()+DELAY);
 			ghostController.update(game.copy(),System.currentTimeMillis()+DELAY);

 			try
			{
				int waited=DELAY/INTERVAL_WAIT;
				
				for(int j=0;j<DELAY/INTERVAL_WAIT;j++)
				{
					Thread.sleep(INTERVAL_WAIT);
					
					if(pacManController.hasComputed() && ghostController.hasComputed())
					{
						waited=j;
						break;
					}
				}
				
				if(fixedTime)
					Thread.sleep(((DELAY/INTERVAL_WAIT)-waited)*INTERVAL_WAIT);
				
				game.advanceGame(pacManController.getMove(),ghostController.getMove());	
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
 	        
 	        if(visual)
 	        	gv.repaint();
 		}
 		
 		pacManController.terminate();
 		ghostController.terminate();
 	}
    
	/**
	 * Run a game in asynchronous mode and recorded.
	 *
     * @param pacManController The Pac-Man controller
     * @param ghostController The Ghosts controller
     * @param visual Whether to run the game with visuals
	 * @param fileName The file name of the file that saves the replay
	 */
	public void runGameTimedRecorded(Controller<MOVE> pacManController,Controller<EnumMap<GHOST,MOVE>> ghostController,boolean visual,String fileName)
	{
		StringBuilder replay=new StringBuilder();
		
		Game game=new Game(0);
		
		GameView gv=null;
		
		if(visual)
		{
			gv=new GameView(game).showGame();
			
			if(pacManController instanceof HumanController)
				gv.getFrame().addKeyListener(((HumanController)pacManController).getKeyboardInput());
		}		
		
		new Thread(pacManController).start();
		new Thread(ghostController).start();
		
		while(!game.gameOver())
		{
			pacManController.update(game.copy(),System.currentTimeMillis()+DELAY);
			ghostController.update(game.copy(),System.currentTimeMillis()+DELAY);

			try
			{
				Thread.sleep(DELAY);
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}

	        game.advanceGame(pacManController.getMove(),ghostController.getMove());	        
	        
	        if(visual)
	        	gv.repaint();
	        
	        replay.append(game.getGameState()+"\n");
		}
		
		pacManController.terminate();
		ghostController.terminate();
		
		saveToFile(replay.toString(),fileName,false);
	}
	
	/**
	 * Replay a previously saved game.
	 *
	 * @param fileName The file name of the game to be played
	 * @param visual Indicates whether or not to use visuals
	 */
	public void replayGame(String fileName,boolean visual)
	{
		ArrayList<String> timeSteps=loadReplay(fileName);
		
		Game game=new Game(0);
		
		GameView gv=null;
		
		if(visual)
			gv=new GameView(game).showGame();
		
		for(int j=0;j<timeSteps.size();j++)
		{			
			game.setGameState(timeSteps.get(j));

			try
			{
				Thread.sleep(DELAY);
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
	        if(visual)
	        	gv.repaint();
		}
	}
	
	//save file for replays
    public static void saveToFile(String data,String name,boolean append)
    {
        try 
        {
            FileOutputStream outS=new FileOutputStream(name,append);
            PrintWriter pw=new PrintWriter(outS);

            pw.println(data);
            pw.flush();
            outS.close();

        } 
        catch (IOException e)
        {
            System.out.println("Could not save data!");	
        }
    }  

    //load a replay
    private static ArrayList<String> loadReplay(String fileName)
	{
    	ArrayList<String> replay=new ArrayList<String>();
		
        try
        {         	
        	BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));	 
            String input=br.readLine();		
            
            while(input!=null)
            {
            	if(!input.equals(""))
            		replay.add(input);

            	input=br.readLine();	
            }
            br.close();
        }
        catch(IOException ioe)
        {
            ioe.printStackTrace();
        }
        
        return replay;
	}
}