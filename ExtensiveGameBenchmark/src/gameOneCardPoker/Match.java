package gameOneCardPoker;

import gameOneCardPoker.algorithms.Algorithm;
import gameOneCardPoker.algorithms.Dynamic;
import gameOneCardPoker.algorithms.Nash;
import gameOneCardPoker.algorithms.RandomAlgorithm;
import gameOneCardPoker.algorithms.SophisStatic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

/**
 * For executing match between agents
 * @author dearhude1
 *
 */
public class Match {

	/**
	 * match num is used for matches between state-of-the-art agents
	 */
	public static final double MATCHES_NUM = 1.0;//100.0;//1.0;
	
	public static long GAMES_NUM = 20000;//1000;//1000;
	
	public static final int OPPONENT_NUM = 2;//1000; //1;
	
	private ArrayList<Integer> allCards_Player1;
	private ArrayList<Integer> allCards_Player2;
	
	private ArrayList<Double> randomPlayerParameters;
	private ArrayList<Double> sophiPlayerParameters; 
	private ArrayList<Double> dynamicPlayerParameters;

	/**
	 * for learning curve
	 */
	private double[] curvePoints;
	
	public Match()
	{
		allCards_Player1 = new ArrayList<Integer>();
		allCards_Player2 = new ArrayList<Integer>();
		
		randomPlayerParameters = new ArrayList<Double>();
		sophiPlayerParameters = new ArrayList<Double>();
		dynamicPlayerParameters = new ArrayList<Double>();
		
		/**
		 * for learning curve
		 */
		curvePoints = new double[30000];
	}
	
	public void generateRandomList()
	{
		/**
		 * the process of generating random probabilities in files
		 *
		try {
			
			BufferedWriter bufWriter = new BufferedWriter(new FileWriter("./randomPros.txt"));
			
			Random random = new Random();
			
			for( int i = 1; i <= 2000; i++ ) {
				
				double pro = random.nextDouble();
				
				bufWriter.write( String.valueOf(pro) );
				bufWriter.newLine();
			}
			
			bufWriter.close();
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
		*/
		
		
		
		/**
		 * read from files
		 */
		 try {
			 
			 //random players
			 BufferedReader bufReader = new BufferedReader(new FileReader("./parameters/randomPros.txt"));
			 
			 String line = "";
			 while( (line = bufReader.readLine()) != null ) {
					
				 if( line.length() == 0 )
					 continue;
				
				 randomPlayerParameters.add( Double.parseDouble(line) );
			 }
				
			 bufReader.close();
			 
			 
			 //sophi players
			 bufReader = new BufferedReader(new FileReader("./parameters/sophiPros.txt"));
			 
			 line = "";
			 while( (line = bufReader.readLine()) != null ) {
					
				 if( line.length() == 0 )
					 continue;
				
				 sophiPlayerParameters.add( Double.parseDouble(line) );
			 }
				
			 bufReader.close();
			 
			 //dynamic players
			 bufReader = new BufferedReader(new FileReader("./parameters/dynamicPros.txt"));
			 
			 line = "";
			 while( (line = bufReader.readLine()) != null ) {
					
				 if( line.length() == 0 )
					 continue;
				
				 dynamicPlayerParameters.add( Double.parseDouble(line) );
			 }
				
			 bufReader.close();
		 }
		 catch(IOException ioe)
		 {
			 ioe.printStackTrace();
		 }
		 
	}
	
	public void dealCards()
	{
	
		Random random = new Random();
		
		
		/**
		 * the process of generating cards in files
		for( int game = 0; game < GAMES_NUM; game++ ) {
			
			ArrayList<Integer> deck = new ArrayList<Integer>();
			for( int card = Card.FIRST_CARD_INDEX; card <= Card.LAST_CARD_INDEX; card++ )
				deck.add( card );
			int card1 = deck.remove( random.nextInt( deck.size() ) );
			int card2 = deck.remove( random.nextInt( deck.size() ) );

			allCards_Player1.add(card1);
			allCards_Player2.add(card2);
			
			deck.clear();
			deck = null;
		}
		try {
			
			BufferedWriter bufWriter = new BufferedWriter(new FileWriter("./cardsPlayer1.txt"));
			
			for( int i = 0; i < allCards_Player1.size(); i++ ) {
				
				bufWriter.write( String.valueOf( allCards_Player1.get(i) ) );
				bufWriter.newLine();
			}
			bufWriter.close();
			
			bufWriter = new BufferedWriter(new FileWriter("./cardsPlayer2.txt"));
			
			for( int i = 0; i < allCards_Player2.size(); i++ ) {
				
				bufWriter.write( String.valueOf( allCards_Player2.get(i) ) );
				bufWriter.newLine();
			}
			bufWriter.close();
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
		*/
		
		/**
		 * read from files
		 */
		 try {
			 
			 BufferedReader bufReader = new BufferedReader(new FileReader("./parameters/cardsPlayer1.txt"));
			 
			 String line = "";
			 while( (line = bufReader.readLine()) != null ) {
					
				 if( line.length() == 0 )
					 continue;
				
				 allCards_Player1.add( Integer.parseInt(line) );
			 }
				
			 bufReader.close();
			 
			 bufReader = new BufferedReader(new FileReader("./parameters/cardsPlayer2.txt"));
			 
			 line = "";
			 while( (line = bufReader.readLine()) != null ) {
					
				 if( line.length() == 0 )
					 continue;
				
				 allCards_Player2.add( Integer.parseInt(line) );
			 }
				
			 bufReader.close();
		 }
		 catch(IOException ioe)
		 {
			 ioe.printStackTrace();
		 }
			
	}
	
	public void cleanDeck()
	{
		
		allCards_Player1.clear();
		allCards_Player2.clear();
	}
	
	public double oneMatch( int playerType, int opponentType, int matchNum )
	{
		
		//System.out.println("The "+String.valueOf(matchNum+1)+"-th match begins===================");
		
		//now test DBBR algorithm against a Nash agent
		Agent agentA = new Agent( playerType, (int) GAMES_NUM );
		Agent agentB = new Agent( opponentType, (int) GAMES_NUM );
		
		/**
		 * set agent seats
		 */
		int seat = 0;
		agentA.setSeat(seat);
		agentB.setSeat(1-seat);
		
		int seatChangeCount = 0;
		
		/**
		 * for recording time
		 */
		long startTime = System.nanoTime();
		 
		agentA.initOnlinePlay();
		agentB.initOnlinePlay();
		for( long T = 0; T < GAMES_NUM; T++ ) { //30000L; T++ ) {

			/**
			 * init game state
			 */
			GameState gameState = new GameState();
			
			/**
			 * deal cards
			 */
			gameState.receiveCard( 0, allCards_Player1.get((int) T));
			gameState.receiveCard( 1, allCards_Player2.get((int) T));
			
			/**
			 * play the game
			 */
			agentA.onlinePlay_GameStarts( gameState, T );
			agentB.onlinePlay_GameStarts( gameState, T );
			while( !gameState.isGameOver() ) {
				
				int actionA = agentA.onlinePlay_ChooseAction( gameState, T );
				int actionB = agentB.onlinePlay_ChooseAction( gameState, T );
				
				/**
				 * observe the opponent's action in public information set
				 */
				if( gameState.getActingPlayer() == agentA.getSeat() ) {
					
					agentB.onlinePlay_ObserveOpponent( gameState, T, actionA );
					OneCardPoker.doAction( gameState, actionA );
				}
				else {
					
					agentA.onlinePlay_ObserveOpponent( gameState, T, actionB );
					OneCardPoker.doAction( gameState, actionB );
				}
				
				if( gameState.isGameOver() ) {
					
					
					agentA.onlinePlay_GameOver( gameState, T );
					agentB.onlinePlay_GameOver( gameState, T );
					
					
					double rlt = OneCardPoker.getUtility( gameState, agentA.getSeat() );
					agentA.resultAdded(rlt);
					
					rlt = OneCardPoker.getUtility( gameState, agentB.getSeat() );
					agentB.resultAdded(rlt);
				}	
			}
			
			
			/**
			 * exchange the seat
			 */
			seatChangeCount++;
			if( seatChangeCount >= 1 ) {
				
				seat = 1- seat;
				agentA.setSeat(seat);
				agentB.setSeat(1-seat);
				
				seatChangeCount = 0;
			}
			
			
			
			/**
			double aver = agentA.getAllResults() / ((double) T);
			double averPlayer1 = agentA.getAverageWinRate_Player1();
			double averPlayer2 = agentA.getAverageWinRate_Player2();
			System.out.println("Agent A-Overall WinRate: "+aver+" Player1: "+averPlayer1+" Player2: "+averPlayer2);
			aver = agentB.getAllResults() / ((double) T);
			averPlayer1 = agentB.getAverageWinRate_Player1();
			averPlayer2 = agentB.getAverageWinRate_Player2();
			System.out.println("Agent B-Overall WinRate: "+aver+" Player1: "+averPlayer1+" Player2: "+averPlayer2);
			System.out.println("======================================================");
			*/
		}
		
		//return agentA.getWinRate( 0 );
		//return agentA.getWinRate( 1 );
		return agentA.getWinRate( -1 );
	}
	
	
	public void playWithRandom( int playerType )
	{
		System.out.println(" Matches Between Algorithm:"+playerType+" and Algorithm: Random ===============");
		
		double[] winRates_Player = new double[OPPONENT_NUM];
		double sum = 0.0;
		
		for( int oppNum = 0; oppNum < OPPONENT_NUM; oppNum++ ) {
			
			winRates_Player[oppNum] = oneMatch_Random( playerType, randomPlayerParameters.get(oppNum) );
			
			sum += winRates_Player[oppNum];
		}
		
		//then compute average win rate and standard deviation
		double averageWinrate = sum / ((double) OPPONENT_NUM);
		double squareSum = 0.0;
		for( int oppNum = 0; oppNum < OPPONENT_NUM; oppNum++ ) {
			
			double difference = winRates_Player[oppNum] - averageWinrate;
			squareSum += difference * difference;
		}
		double standardDeviation = Math.sqrt( squareSum / ((double)OPPONENT_NUM) );
		
		System.out.println("Average Winrate: "+averageWinrate+" Standard Deviation: "+standardDeviation);
		
		//then write to file
		//write to file
		try {
			
			
			/**
			 * write the final results
			 */
			String fileName = "./"+Algorithm.ALG_STRINGS[playerType]+" vs "+Algorithm.ALG_STRINGS[Algorithm.ALG_ONLINE_RANDOM]+".txt";
			
			BufferedWriter bufWriter = new BufferedWriter(new FileWriter(fileName));
			
			for( int oppNum = 0; oppNum < OPPONENT_NUM; oppNum++ ) {
			
				bufWriter.write(String.valueOf(winRates_Player[oppNum]));
				bufWriter.newLine();
			}
			bufWriter.write("Average Winrate: "+averageWinrate);
			bufWriter.newLine();
			bufWriter.write("Standard Deviation: "+standardDeviation);
			bufWriter.newLine();
			
			bufWriter.close();
			
			
			/**
			 * write the learning curve points
			 */
			fileName = "./"+Algorithm.ALG_STRINGS[Algorithm.ALG_ONLINE_RANDOM]+"-"+Algorithm.ALG_STRINGS[playerType]+".csv";
			bufWriter = new BufferedWriter(new FileWriter(fileName));
			
			for( int i = 0; i < GAMES_NUM; i++ ) {
				
				bufWriter.write( curvePoints[i] / OPPONENT_NUM +", " );
				
				curvePoints[i] = 0.0; 
			}
			bufWriter.close();
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
	}
	
	public double oneMatch_Random( int playerType, double rndPro )
	{
		
		Agent agentA = new Agent( playerType, (int) GAMES_NUM );
		
		/**
		 * set the opponent and its parameter
		 */
		Agent agentB = new Agent( Algorithm.ALG_ONLINE_RANDOM, (int) GAMES_NUM );
		((RandomAlgorithm) agentB.getAlgorithm()).setRandomPro(rndPro);
		
		/**
		 * set agent seats
		 */
		int seat = 1;
		agentA.setSeat(seat);
		agentB.setSeat(1-seat);
		
		agentA.initOnlinePlay();
		agentB.initOnlinePlay();
		for( long T = 0; T < GAMES_NUM; T++ ) { //30000L; T++ ) {

			/**
			 * init game state
			 */
			GameState gameState = new GameState();
			
			/**
			 * deal cards
			 */
			int size = allCards_Player1.size();
			gameState.receiveCard( 0, allCards_Player1.get((int) T%size));
			gameState.receiveCard( 1, allCards_Player2.get((int) T%size));
			
			/**
			 * play the game
			 */
			agentA.onlinePlay_GameStarts( gameState, T );
			agentB.onlinePlay_GameStarts( gameState, T );
			while( !gameState.isGameOver() ) {
				
				int actionA = agentA.onlinePlay_ChooseAction( gameState, T );
				int actionB = agentB.onlinePlay_ChooseAction( gameState, T );
				
				/**
				 * observe the opponent's action in public information set
				 */
				if( gameState.getActingPlayer() == agentA.getSeat() ) {
					
					agentB.onlinePlay_ObserveOpponent( gameState, T, actionA );
					OneCardPoker.doAction( gameState, actionA );
				}
				else {
					
					agentA.onlinePlay_ObserveOpponent( gameState, T, actionB );
					OneCardPoker.doAction( gameState, actionB );
				}
				
				if( gameState.isGameOver() ) {
					
					
					agentA.onlinePlay_GameOver( gameState, T );
					agentB.onlinePlay_GameOver( gameState, T );
					
					
					double rlt = OneCardPoker.getUtility( gameState, agentA.getSeat() );
					agentA.resultAdded(rlt);
					
					rlt = OneCardPoker.getUtility( gameState, agentB.getSeat() );
					agentB.resultAdded(rlt);
					
					/**
					 * so after each game
					 * record the current win rates
					 */
					curvePoints[(int)T] += agentA.getWinRate( 1 );
				}
			}
			
			/**
			 * exchange the seat
			 *
			seatChangeCount++;
			if( seatChangeCount >= 1 ) {
				
				seat = 1- seat;
				agentA.setSeat(seat);
				agentB.setSeat(1-seat);
				
				seatChangeCount = 0;
			}
			*/
			
			
		}
		
		return agentA.getWinRate( 1 );
		//return agentA.getWinRate( 1 );
		//return agentA.getWinRate( -1 );
	}
	
	public void playWithSophi( int playerType )
	{
		System.out.println(" Matches Between Algorithm:"+playerType+" and Algorithm: Sophi ===============");
		
		double[] winRates_Player = new double[OPPONENT_NUM];
		double sum = 0.0;
		
		for( int oppNum = 0; oppNum < OPPONENT_NUM; oppNum++ ) {
			
			winRates_Player[oppNum] = oneMatch_Sophi( playerType, sophiPlayerParameters.get(oppNum) );
			
			sum += winRates_Player[oppNum];
		}
		
		//then compute average win rate and standard deviation
		double averageWinrate = sum / ((double) OPPONENT_NUM);
		double squareSum = 0.0;
		for( int oppNum = 0; oppNum < OPPONENT_NUM; oppNum++ ) {
			
			double difference = winRates_Player[oppNum] - averageWinrate;
			squareSum += difference * difference;
		}
		double standardDeviation = Math.sqrt( squareSum / ((double)OPPONENT_NUM) );
		
		System.out.println("Average Winrate: "+averageWinrate+" Standard Deviation: "+standardDeviation);
		
		//then write to file
		//write to file
		try {
			
			String fileName = "./"+Algorithm.ALG_STRINGS[playerType]+" vs "+Algorithm.ALG_STRINGS[Algorithm.ALG_ONLINE_SOPHISSTATIC]+".txt";
			
			BufferedWriter bufWriter = new BufferedWriter(new FileWriter(fileName));
			
			for( int oppNum = 0; oppNum < OPPONENT_NUM; oppNum++ ) {
			
				bufWriter.write(String.valueOf(winRates_Player[oppNum]));
				bufWriter.newLine();
			}
			bufWriter.write("Average Winrate: "+averageWinrate);
			bufWriter.newLine();
			bufWriter.write("Standard Deviation: "+standardDeviation);
			bufWriter.newLine();
			
			bufWriter.close();
			
			/**
			 * write the learning curve points
			 */
			fileName = "./"+Algorithm.ALG_STRINGS[Algorithm.ALG_ONLINE_SOPHISSTATIC]+"-"+Algorithm.ALG_STRINGS[playerType]+".csv";
			bufWriter = new BufferedWriter(new FileWriter(fileName));
			
			for( int i = 0; i < GAMES_NUM; i++ ) {
				
				bufWriter.write( curvePoints[i] / OPPONENT_NUM +", " );
				
				curvePoints[i] = 0.0; 
			}
			bufWriter.close();
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
	}
	
	public double oneMatch_Sophi( int playerType, double rndPro )
	{
		
		Agent agentA = new Agent( playerType, (int) GAMES_NUM );
		
		/**
		 * set the opponent and its parameter
		 */
		Agent agentB = new Agent( Algorithm.ALG_ONLINE_SOPHISSTATIC, (int) GAMES_NUM );
		((SophisStatic) agentB.getAlgorithm()).setRandomPro(rndPro);
		
		/**
		 * set agent seats
		 */
		int seat = 1;
		agentA.setSeat(seat);
		agentB.setSeat(1-seat);
		
		
		agentA.initOnlinePlay();
		agentB.initOnlinePlay();
		for( long T = 0; T < GAMES_NUM; T++ ) { //30000L; T++ ) {

			/**
			 * init game state
			 */
			GameState gameState = new GameState();
			
			/**
			 * deal cards
			 */
			int size = allCards_Player1.size();
			gameState.receiveCard( 0, allCards_Player1.get((int) T%size));
			gameState.receiveCard( 1, allCards_Player2.get((int) T%size));
			
			/**
			 * play the game
			 */
			agentA.onlinePlay_GameStarts( gameState, T );
			agentB.onlinePlay_GameStarts( gameState, T );
			while( !gameState.isGameOver() ) {
				
				int actionA = agentA.onlinePlay_ChooseAction( gameState, T );
				int actionB = agentB.onlinePlay_ChooseAction( gameState, T );
				
				/**
				 * observe the opponent's action in public information set
				 */
				if( gameState.getActingPlayer() == agentA.getSeat() ) {
					
					agentB.onlinePlay_ObserveOpponent( gameState, T, actionA );
					OneCardPoker.doAction( gameState, actionA );
				}
				else {
					
					agentA.onlinePlay_ObserveOpponent( gameState, T, actionB );
					OneCardPoker.doAction( gameState, actionB );
				}
				
				if( gameState.isGameOver() ) {
					
					
					agentA.onlinePlay_GameOver( gameState, T );
					agentB.onlinePlay_GameOver( gameState, T );
					
					
					double rlt = OneCardPoker.getUtility( gameState, agentA.getSeat() );
					agentA.resultAdded(rlt);
					
					rlt = OneCardPoker.getUtility( gameState, agentB.getSeat() );
					agentB.resultAdded(rlt);
					
					/**
					 * so after each game
					 * record the current win rates
					 */
					curvePoints[(int)T] += agentA.getWinRate( 1 );
				}	
			}
			
			
			/**
			 * exchange the seat
			 *
			seatChangeCount++;
			if( seatChangeCount >= 1 ) {
				
				seat = 1- seat;
				agentA.setSeat(seat);
				agentB.setSeat(1-seat);
				
				seatChangeCount = 0;
			}
			*/
			
		}
		
		return agentA.getWinRate( 1 );
		//return agentA.getWinRate( 1 );
		//return agentA.getWinRate( -1 );
	}
	
	
	public void playWithDynamic( int playerType )
	{
		System.out.println(" Matches Between Algorithm:"+playerType+" and Algorithm: Dynamic ===============");
		
		double[] winRates_Player = new double[OPPONENT_NUM];
		double sum = 0.0;
		
		for( int oppNum = 0; oppNum < OPPONENT_NUM; oppNum++ ) {
			
			winRates_Player[oppNum] = oneMatch_Dynamic( playerType, dynamicPlayerParameters.get(oppNum) );
			
			sum += winRates_Player[oppNum];
		}
		
		//then compute average win rate and standard deviation
		double averageWinrate = sum / ((double) OPPONENT_NUM);
		double squareSum = 0.0;
		for( int oppNum = 0; oppNum < OPPONENT_NUM; oppNum++ ) {
			
			double difference = winRates_Player[oppNum] - averageWinrate;
			squareSum += difference * difference;
		}
		double standardDeviation = Math.sqrt( squareSum / ((double)OPPONENT_NUM) );
		
		System.out.println("Average Winrate: "+averageWinrate+" Standard Deviation: "+standardDeviation);
		
		//then write to file
		//write to file
		try {
			
			String fileName = "./"+Algorithm.ALG_STRINGS[playerType]+" vs "+Algorithm.ALG_STRINGS[Algorithm.ALG_ONLINE_DYNAMIC]+".txt";
			
			BufferedWriter bufWriter = new BufferedWriter(new FileWriter(fileName));
			
			for( int oppNum = 0; oppNum < OPPONENT_NUM; oppNum++ ) {
			
				bufWriter.write(String.valueOf(winRates_Player[oppNum]));
				bufWriter.newLine();
			}
			bufWriter.write("Average Winrate: "+averageWinrate);
			bufWriter.newLine();
			bufWriter.write("Standard Deviation: "+standardDeviation);
			bufWriter.newLine();
			
			bufWriter.close();
			
			/**
			 * write the learning curve points
			 */
			fileName = "./"+Algorithm.ALG_STRINGS[Algorithm.ALG_ONLINE_DYNAMIC]+"-"+Algorithm.ALG_STRINGS[playerType]+".csv";
			bufWriter = new BufferedWriter(new FileWriter(fileName));
			
			for( int i = 0; i < GAMES_NUM; i++ ) {
				
				bufWriter.write( curvePoints[i] / OPPONENT_NUM +", " );
				
				curvePoints[i] = 0.0; 
			}
			bufWriter.close();
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
	}
	
	
	public double oneMatch_Dynamic( int playerType, double rndPro )
	{
		
		Agent agentA = new Agent( playerType, (int) GAMES_NUM );
		
		/**
		 * set the opponent and its parameter
		 */
		Agent agentB = new Agent( Algorithm.ALG_ONLINE_DYNAMIC, (int) GAMES_NUM );
		((Dynamic) agentB.getAlgorithm()).setRandomPro(rndPro);
		
		/**
		 * set agent seats
		 */
		int seat = 1;
		agentA.setSeat(seat);
		agentB.setSeat(1-seat);
		
		agentA.initOnlinePlay();
		agentB.initOnlinePlay();
		for( long T = 0; T < GAMES_NUM; T++ ) {

			/**
			 * init game state
			 */
			GameState gameState = new GameState();
			
			/**
			 * deal cards
			 */
			int size = allCards_Player1.size();
			gameState.receiveCard( 0, allCards_Player1.get((int) T%size));
			gameState.receiveCard( 1, allCards_Player2.get((int) T%size));
			
			/**
			 * play the game
			 */
			agentA.onlinePlay_GameStarts( gameState, T );
			agentB.onlinePlay_GameStarts( gameState, T );
			while( !gameState.isGameOver() ) {
				
				int actionA = agentA.onlinePlay_ChooseAction( gameState, T );
				int actionB = agentB.onlinePlay_ChooseAction( gameState, T );
				
				/**
				 * observe the opponent's action in public information set
				 */
				if( gameState.getActingPlayer() == agentA.getSeat() ) {
					
					agentB.onlinePlay_ObserveOpponent( gameState, T, actionA );
					OneCardPoker.doAction( gameState, actionA );
				}
				else {
					
					agentA.onlinePlay_ObserveOpponent( gameState, T, actionB );
					OneCardPoker.doAction( gameState, actionB );
				}
				
				if( gameState.isGameOver() ) {
					
					
					agentA.onlinePlay_GameOver( gameState, T );
					agentB.onlinePlay_GameOver( gameState, T );
					
					
					double rlt = OneCardPoker.getUtility( gameState, agentA.getSeat() );
					agentA.resultAdded(rlt);
					
					rlt = OneCardPoker.getUtility( gameState, agentB.getSeat() );
					agentB.resultAdded(rlt);
					
					/**
					 * so after each game
					 * record the current win rates
					 */
					curvePoints[(int)T] += agentA.getWinRate( 1 );
				}	
			}
			
			
			/**
			 * exchange the seat
			 *
			seatChangeCount++;
			if( seatChangeCount >= 1 ) {
				
				seat = 1- seat;
				agentA.setSeat(seat);
				agentB.setSeat(1-seat);
				
				seatChangeCount = 0;
			}
			*/
			
		}
		
		return agentA.getWinRate( 1 );
		//return agentA.getWinRate( 1 );
		//return agentA.getWinRate( -1 );
	}
	
	
	public void playWithNE( int playerType )
	{
		System.out.println(" Matches Between Algorithm:"+playerType+" and Algorithm: Nash ===============");
		
		double[] winRates_Player = new double[OPPONENT_NUM];
		double sum = 0.0;
		
		for( int oppNum = 0; oppNum < OPPONENT_NUM; oppNum++ ) {
			
			winRates_Player[oppNum] = oneMatch_NE( playerType, oppNum+1 );
			
			sum += winRates_Player[oppNum];
		}
		
		//then compute average win rate and standard deviation
		double averageWinrate = sum / ((double) OPPONENT_NUM);
		double squareSum = 0.0;
		for( int oppNum = 0; oppNum < OPPONENT_NUM; oppNum++ ) {
			
			double difference = winRates_Player[oppNum] - averageWinrate;
			squareSum += difference * difference;
		}
		double standardDeviation = Math.sqrt( squareSum / ((double)OPPONENT_NUM) );
		
		System.out.println("Average Winrate: "+averageWinrate+" Standard Deviation: "+standardDeviation);
		
		//then write to file
		//write to file
		try {
			
			String fileName = "./"+Algorithm.ALG_STRINGS[playerType]+" vs "+Algorithm.ALG_STRINGS[Algorithm.ALG_ONLINE_NASH]+".txt";
			
			BufferedWriter bufWriter = new BufferedWriter(new FileWriter(fileName));
			
			for( int oppNum = 0; oppNum < OPPONENT_NUM; oppNum++ ) {
			
				bufWriter.write(String.valueOf(winRates_Player[oppNum]));
				bufWriter.newLine();
			}
			bufWriter.write("Average Winrate: "+averageWinrate);
			bufWriter.newLine();
			bufWriter.write("Standard Deviation: "+standardDeviation);
			bufWriter.newLine();
			
			bufWriter.close();
			
			/**
			 * write the learning curve points
			 */
			fileName = "./"+Algorithm.ALG_STRINGS[Algorithm.ALG_ONLINE_NASH]+"-"+Algorithm.ALG_STRINGS[playerType]+".csv";
			bufWriter = new BufferedWriter(new FileWriter(fileName));
			
			for( int i = 0; i < GAMES_NUM; i++ ) {
				
				bufWriter.write( curvePoints[i] / OPPONENT_NUM +", " );
				
				curvePoints[i] = 0.0; 
			}
			bufWriter.close();
			
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
	}

	public double oneMatch_NE( int playerType, int nashNum )
	{
		
		Agent agentA = new Agent( playerType, (int) GAMES_NUM );
		
		/**
		 * set the opponent and its parameter
		 */
		Agent agentB = new Agent( Algorithm.ALG_ONLINE_NASH, (int) GAMES_NUM );
		((Nash) agentB.getAlgorithm()).setPolicyNum( nashNum );
		
		/**
		 * set agent seats
		 */
		int seat = 1;
		agentA.setSeat(seat);
		agentB.setSeat(1-seat);
		
		
		agentA.initOnlinePlay();
		agentB.initOnlinePlay();
		for( long T = 0; T < GAMES_NUM; T++ ) {

			/**
			 * init game state
			 */
			GameState gameState = new GameState();
			
			/**
			 * deal cards
			 */
			gameState.receiveCard( 0, allCards_Player1.get((int) T));
			gameState.receiveCard( 1, allCards_Player2.get((int) T));
			
			/**
			 * play the game
			 */
			agentA.onlinePlay_GameStarts( gameState, T );
			agentB.onlinePlay_GameStarts( gameState, T );
			while( !gameState.isGameOver() ) {
				
				int actionA = agentA.onlinePlay_ChooseAction( gameState, T );
				int actionB = agentB.onlinePlay_ChooseAction( gameState, T );
				
				/**
				 * observe the opponent's action in public information set
				 */
				if( gameState.getActingPlayer() == agentA.getSeat() ) {
					
					agentB.onlinePlay_ObserveOpponent( gameState, T, actionA );
					OneCardPoker.doAction( gameState, actionA );
				}
				else {
					
					agentA.onlinePlay_ObserveOpponent( gameState, T, actionB );
					OneCardPoker.doAction( gameState, actionB );
				}
				
				if( gameState.isGameOver() ) {
					
					
					agentA.onlinePlay_GameOver( gameState, T );
					agentB.onlinePlay_GameOver( gameState, T );
					
					
					double rlt = OneCardPoker.getUtility( gameState, agentA.getSeat() );
					agentA.resultAdded(rlt);
					
					rlt = OneCardPoker.getUtility( gameState, agentB.getSeat() );
					agentB.resultAdded(rlt);
					
					/**
					 * so after each game
					 * record the current win rates
					 */
					curvePoints[(int)T] += agentA.getWinRate( 1 );
				}	
			}
			
			
			/**
			 * exchange the seat
			 *
			seatChangeCount++;
			if( seatChangeCount >= 1 ) {
				
				seat = 1- seat;
				agentA.setSeat(seat);
				agentB.setSeat(1-seat);
				
				seatChangeCount = 0;
			}
			*/
			
		}
		
		return agentA.getWinRate( 1 );
		//return agentA.getWinRate( 1 );
		//return agentA.getWinRate( -1 );
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		Match match = new Match();
		
		match.generateRandomList();
		match.dealCards();
		
		//the last one actually is nash
		//int opponentTypes[] = { Algorithm.ALG_ONLINE_DBBR, Algorithm.ALG_ONLINE_MCCFROS,
				//Algorithm.ALG_ACTORCRITIC, Algorithm.ALG_QLEARNING };
		int opponentTypes[] = { Algorithm.ALG_SARSA };
		
		//int playerTypes[] = { Algorithm.ALG_ONLINE_DBBR, Algorithm.ALG_ONLINE_MCCFROS,
				//Algorithm.ALG_ACTORCRITIC, Algorithm.ALG_QLEARNING, Algorithm.ALG_SARSA,
				//Algorithm.ALG_ONLINE_onLCFR, Algorithm.ALG_ONLINE_onCFRRL };
		int playerTypes[] = { Algorithm.ALG_ONLINE_BEFEWP, Algorithm.ALG_ONLINE_BEFFE };
		
		
		for( int indexPT = 0; indexPT < playerTypes.length; indexPT++ ) {
			
			int playerType = playerTypes[indexPT];
			
			/**
			 * match against naive opponents
			 */
			//play with random player
			match.playWithRandom( playerType );
			
			//play with sophi player
			match.playWithSophi( playerType );
			
			//play with dynamic player
			GAMES_NUM = 30000;
			match.playWithDynamic( playerType );
			
			GAMES_NUM = 20000;
			//play with NE player
			match.playWithNE( playerType );
			
			
			
			//this is for match between state-of-the-art agents
			/**
			for( int indexOT = 0; indexOT < opponentTypes.length; indexOT++ ) {
				
				int opponentType = opponentTypes[indexOT];
				
				if( playerType == opponentType )
					continue;
				
				System.out.println(" Matches Between Algorithm:"+playerType+" and Algorithm:"+opponentType+" ===============");
				
				double[] winRates_Player1 = new double[(int)MATCHES_NUM];
				double sum = 0.0;
				for( int matchIndex = 0; matchIndex < MATCHES_NUM; matchIndex++ ) {
					
					winRates_Player1[matchIndex] = match.oneMatch( playerType, opponentType, matchIndex );
					sum += winRates_Player1[matchIndex];
				}
				
				//then compute average win rate and standard deviation
				double averageWinrate = sum / MATCHES_NUM;
				double squareSum = 0.0;
				for( int matchIndex = 0; matchIndex < MATCHES_NUM; matchIndex++ ) {
					
					double difference = winRates_Player1[matchIndex] - averageWinrate;
					squareSum += difference * difference;
				}
				double standardDeviation = Math.sqrt( squareSum / MATCHES_NUM );
				
				System.out.println("Average Winrate: "+averageWinrate+" Standard Deviation: "+standardDeviation);
				
				//then write to file
				//write to file
				try {
					
					String fileName = "./"+Algorithm.ALG_STRINGS[playerType]+" vs "+Algorithm.ALG_STRINGS[opponentType]+".txt";
					
					BufferedWriter bufWriter = new BufferedWriter(new FileWriter(fileName));
					
					for( int matchIndex = 0; matchIndex < MATCHES_NUM; matchIndex++ ) {
					
						bufWriter.write("Match"+matchIndex+": "+String.valueOf(winRates_Player1[matchIndex]));
						bufWriter.newLine();
					}
					bufWriter.write("Average Winrate: "+averageWinrate);
					bufWriter.newLine();
					bufWriter.write("Standard Deviation: "+standardDeviation);
					bufWriter.newLine();
					
					bufWriter.close();
					
				}
				catch(IOException ioe)
				{
					ioe.printStackTrace();
				}
			}
			*/
			
			
		}
		
		match.cleanDeck();
	}

}
