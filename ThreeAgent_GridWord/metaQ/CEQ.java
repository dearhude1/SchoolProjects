package metaQ;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import drasys.or.mp.*;
import drasys.or.mp.lp.DenseSimplex;
import drasys.or.mp.lp.LinearProgrammingI;
import drasys.or.matrix.VectorI;
import tools.Pair;
import tools.Tools;

/**
 * 
 */
public class CEQ implements GridGame
{

    public static final NumberFormat numberFormat = NumberFormat
	    .getNumberInstance();

    static
    {
	numberFormat.setMaximumFractionDigits(4);
    }

    /**
     * right 0,up 1,left 2,down 3
     */
    public static final int UP = 1;

    public static final int DOWN = 3;

    public static final int LEFT = 2;

    public static final int RIGHT = 0;

    /**
     * the number of states
     */
    public static final int STATE_NUM = 16;

    public static final String[] ACTIONS_DES = { "R", "U", "L", "D" };

    
    public static final int LOOP = 1;

    public static final int GOAL_A = 14;//13;//14;//10;//14;

    public static final int GOAL_B = 15;//13;//15;//15;

    public static final int GOAL_C = 12;//13;//12;//4;//12;

    public static final int episodesNum = 50000;

    public static final double epsilonValue = 0.01; //0.3;

    
    protected double[][][][][][] Q1;

    protected double[][][][][][] Q2;

    protected double[][][][][][] Q3;
    
    
    /**
     * add by Huyujing
     * when each agent is independent
     */
    protected double[][][][][][] Q11;
    protected double[][][][][][] Q12;
    protected double[][][][][][] Q13;
    
    protected double[][][][][][] Q21;
    protected double[][][][][][] Q22;
    protected double[][][][][][] Q23;
    
    protected double[][][][][][] Q31;
    protected double[][][][][][] Q32;
    protected double[][][][][][] Q33;
    

    protected int[] loc;

    protected int[] preLoc;

    protected int[] action;

    protected double[] reward;

    protected int[] averageStepsToTerminal;

    protected int[] averageStepsToTerminal1;

    protected int[] averageStepsToTerminal2;

    protected int[] averageStepsToTerminal3;

    protected long[] averageEpisodeTime;

    protected long[] averageEpisodeTime1;

    protected long[] averageEpisodeTime2;

    protected long[] averageEpisodeTime3;

    protected double epsilon;

    protected Random randomEpsilon;

    protected Random random;

    protected boolean gotA;

    protected boolean gotB;

    protected boolean gotC;
    
    protected int stepA = 0;
    protected int stepB = 0;
    protected int stepC = 0;

    
    /**
     * 用于存放每一步计算每个agent的CE平衡解
     */
    private double[] policies1;
    private double[] policies2;
    private double[] policies3;
    
    public CEQ()
    {
	selfIntro();
	Q1 = new double[STATE_NUM][STATE_NUM][STATE_NUM][4][4][4];
	Q2 = new double[STATE_NUM][STATE_NUM][STATE_NUM][4][4][4];
	Q3 = new double[STATE_NUM][STATE_NUM][STATE_NUM][4][4][4];
	loc = new int[3];
	preLoc = new int[3];
	action = new int[3];
	reward = new double[3];
	averageStepsToTerminal = new int[episodesNum];
	averageStepsToTerminal1 = new int[episodesNum];
	averageStepsToTerminal2 = new int[episodesNum];
	averageStepsToTerminal3 = new int[episodesNum];
	averageEpisodeTime = new long[episodesNum];
	averageEpisodeTime1 = new long[episodesNum];
	averageEpisodeTime2 = new long[episodesNum];
	averageEpisodeTime3 = new long[episodesNum];
	
	
	epsilon = epsilonValue;
	random = new Random();
	randomEpsilon = new Random();
	
	
	/**
	 * added by Huyujing
	 */
	Q11 = new double[STATE_NUM][STATE_NUM][STATE_NUM][4][4][4];
	Q12 = new double[STATE_NUM][STATE_NUM][STATE_NUM][4][4][4];
	Q13 = new double[STATE_NUM][STATE_NUM][STATE_NUM][4][4][4];
	Q21 = new double[STATE_NUM][STATE_NUM][STATE_NUM][4][4][4];
	Q22 = new double[STATE_NUM][STATE_NUM][STATE_NUM][4][4][4];
	Q23 = new double[STATE_NUM][STATE_NUM][STATE_NUM][4][4][4];
	Q31 = new double[STATE_NUM][STATE_NUM][STATE_NUM][4][4][4];
	Q32 = new double[STATE_NUM][STATE_NUM][STATE_NUM][4][4][4];
	Q33 = new double[STATE_NUM][STATE_NUM][STATE_NUM][4][4][4];
	
	/**
	 * 将三维数组放到一维上
	 */
	policies1 = new double[64];
	policies2 = new double[64];
	policies3 = new double[64];
	for( int i = 0; i < 64; i++ ) {
	    
	    policies1[i] = policies2[i] = policies3[i] = 0.0;  
	}
    }
    
    public int[] getEquilibriumAction(int s1,int s2, int s3)
    {
	return null;
    }

    /**
     * right 0,up 1,left 2,down 3
     */
    public void train(double alpha, double beta)
    {
	NumberFormat nf = NumberFormat.getNumberInstance();
	nf.setMaximumFractionDigits(2);
	nf.setMinimumFractionDigits(2);
	nf.setMaximumIntegerDigits(3);
	nf.setMinimumIntegerDigits(3);
	for (int i = 0; i < LOOP; i++)
	{
	    System.out.println("**********The " + (i + 1) + "th times to run "
		    + episodesNum + " episodes***************");
	    resetGame();
	    for (int episode = 1; episode <= episodesNum; episode++)
	    {

		loc[0] = 0;
		loc[1] = 2;
		loc[2] = 3;
		int steps = 0;
		int steps1 = 0;
		int steps2 = 0;
		int steps3 = 0;
		long time = 0;
		long time1 = 0;
		long time2 = 0;
		long time3 = 0;
		gotA = gotB = gotC = false;
		stepA = stepB = stepC = 0;
		
		long start = System.nanoTime();
		
		
		/**
		 * initial state s = (0,2,3)
		 */
		int s1 = preLoc[0] = loc[0] = 0;
		int s2 = preLoc[1] = loc[1] = 2;
		int s3 = preLoc[2] = loc[2] = 3;
		
		/**
		 * we compute the equilibrium action for the initial state
		 */
		double[] CE = computeCE(0, s1, s2, s3);
		int[] eqJA_CE = getAction_CE(CE);
		
		//double[] CE1 = computeCE(0, s1, s2, s3);
		//double[] CE2 = computeCE(1, s1, s2, s3);
		//double[] CE3 = computeCE(2, s1, s2, s3);
		//int[] eqJA_CE1 = getAction_CE(CE1);
		//int[] eqJA_CE2 = getAction_CE(CE2);
		//int[] eqJA_CE3 = getAction_CE(CE3); 
		
		while (!gameover())
		{

		    
		    /**
		     * epsilon-greedy也在CEQ中使用? 
		     */
		    int a1 = eqJA_CE[0];
		    int a2 = eqJA_CE[1];
		    int a3 = eqJA_CE[2];
		    //int a1 = eqJA_CE1[0];
		    //int a2 = eqJA_CE2[1];
		    //int a3 = eqJA_CE3[2];
		    
		    if(randomEpsilon.nextDouble() < epsilon)
		    {
			a1 = random.nextInt(4);
			a2 = random.nextInt(4);
			a3 = random.nextInt(4);
		    }
		    
		    /**
		     * take actions
		     * in fact, here we just set the action to be taken
		     * actions are taken in function setReward()
		     */
		    action[0] = a1;
		    action[1] = a2;
		    action[2] = a3;
		    
		    /**
		     * count the steps
		     */
		    steps++;
		    if (!gotA)
		    {
			steps1++;
			time1 = System.nanoTime() - start;
			
			stepA++;
		    }
		    if (!gotB)
		    {
			steps2++;
			time2 = System.nanoTime() - start;
			
			stepB++;
		    }
		    if (!gotC)
		    {
			steps3++;
			time3 = System.nanoTime() - start;
			
			stepC++;
		    }
		    
		    /**
		     * set rewards for actions
		     * action are taken here
		     */
		    setReward();
		    //Tools.DEBUG("After setReward");
		    
		    /**
		     * then we observe the next state s' = (sp1,sp2,sp3)
		     * and compute the equilibrium action in s' 
		     */
		    int sp1 = loc[0];
		    int sp2 = loc[1];
		    int sp3 = loc[2];
		    
		    CE = computeCE(0, sp1, sp2, sp3);
		    eqJA_CE = getAction_CE(CE);
		    //CE1 = computeCE(0, sp1, sp2, sp3);
		    //CE2 = computeCE(1, sp1, sp2, sp3);
		    //CE3 = computeCE(2, sp1, sp2, sp3);
		    //eqJA_CE1 = getAction_CE(CE1);
		    //eqJA_CE2 = getAction_CE(CE2);
		    //eqJA_CE3 = getAction_CE(CE3);
		    double value1 = getValue_CE(0, sp1, sp2, sp3, CE);
		    double value2 = getValue_CE(1, sp1, sp2, sp3, CE);
		    double value3 = getValue_CE(2, sp1, sp2, sp3, CE);
		    
		
		    Q1[s1][s2][s3][a1][a2][a3] = (1 - alpha) * Q1[s1][s2][s3][a1][a2][a3]
		           + alpha * (reward[0] + beta * value1 );
		    Q2[s1][s2][s3][a1][a2][a3] = (1 - alpha) * Q2[s1][s2][s3][a1][a2][a3]
		           + alpha * (reward[1] + beta * value2 );
		    Q3[s1][s2][s3][a1][a2][a3] = (1 - alpha) * Q3[s1][s2][s3][a1][a2][a3]
		           + alpha * (reward[2] + beta * value3 );
		    
		    /**
		     * update Q-values
		     *
		    Q11[s1][s2][s3][a1][a2][a3] = (1 - alpha) * Q11[s1][s2][s3][a1][a2][a3]
			    + alpha * (reward[0] + beta * Q11[sp1][sp2][sp3][eqJA_ME1[0]][eqJA_ME1[1]][eqJA_ME1[2]]);
		    Q12[s1][s2][s3][a1][a2][a3] = (1 - alpha) * Q12[s1][s2][s3][a1][a2][a3]
		            + alpha * (reward[1] + beta * Q12[sp1][sp2][sp3][eqJA_ME1[0]][eqJA_ME1[1]][eqJA_ME1[2]]);
		    Q13[s1][s2][s3][a1][a2][a3] = (1 - alpha) * Q13[s1][s2][s3][a1][a2][a3]
		            + alpha * (reward[2] + beta * Q13[sp1][sp2][sp3][eqJA_ME1[0]][eqJA_ME1[1]][eqJA_ME1[2]]);
		    
		    
		    Q21[s1][s2][s3][a1][a2][a3] = (1 - alpha) * Q21[s1][s2][s3][a1][a2][a3]
		            + alpha * (reward[0] + beta * Q21[sp1][sp2][sp3][eqJA_ME2[0]][eqJA_ME2[1]][eqJA_ME2[2]]);
		    Q22[s1][s2][s3][a1][a2][a3] = (1 - alpha) * Q22[s1][s2][s3][a1][a2][a3]
		            + alpha * (reward[1] + beta * Q22[sp1][sp2][sp3][eqJA_ME2[0]][eqJA_ME2[1]][eqJA_ME2[2]]);
		    Q23[s1][s2][s3][a1][a2][a3] = (1 - alpha) * Q23[s1][s2][s3][a1][a2][a3]
		            + alpha * (reward[2] + beta * Q23[sp1][sp2][sp3][eqJA_ME2[0]][eqJA_ME2[1]][eqJA_ME2[2]]);
		    
		    Q31[s1][s2][s3][a1][a2][a3] = (1 - alpha) * Q31[s1][s2][s3][a1][a2][a3]
		            + alpha * (reward[0] + beta * Q31[sp1][sp2][sp3][eqJA_NE3[0]][eqJA_NE3[1]][eqJA_NE3[2]]);
		    Q32[s1][s2][s3][a1][a2][a3] = (1 - alpha) * Q32[s1][s2][s3][a1][a2][a3]
		            + alpha * (reward[1] + beta * Q32[sp1][sp2][sp3][eqJA_NE3[0]][eqJA_NE3[1]][eqJA_NE3[2]]);
		    Q33[s1][s2][s3][a1][a2][a3] = (1 - alpha) * Q33[s1][s2][s3][a1][a2][a3]
		            + alpha * (reward[2] + beta * Q33[sp1][sp2][sp3][eqJA_NE3[0]][eqJA_NE3[1]][eqJA_NE3[2]]);
		    */
		    
		    
		    /**
		     * s <- s'
		     */
		    s1 = preLoc[0] = sp1;
		    s2 = preLoc[1] = sp2;
		    s3 = preLoc[2] = sp3;

		}
		long end = System.nanoTime();
		time = end - start;
		// get a time here, caculate the time used
		// 多次测量，取平均值
		averageStepsToTerminal[episode - 1] += (steps - averageStepsToTerminal[episode - 1])
			/ (i + 1);
		averageStepsToTerminal1[episode - 1] += (steps1 - averageStepsToTerminal1[episode - 1])
			/ (i + 1);
		averageStepsToTerminal2[episode - 1] += (steps2 - averageStepsToTerminal2[episode - 1])
			/ (i + 1);
		averageStepsToTerminal3[episode - 1] += (steps3 - averageStepsToTerminal3[episode - 1])
			/ (i + 1);
		averageEpisodeTime[episode - 1] += (time - averageEpisodeTime[episode - 1])
			/ (i + 1);
		averageEpisodeTime1[episode - 1] += (time1 - averageEpisodeTime1[episode - 1])
			/ (i + 1);
		averageEpisodeTime2[episode - 1] += (time2 - averageEpisodeTime2[episode - 1])
			/ (i + 1);
		averageEpisodeTime3[episode - 1] += (time3 - averageEpisodeTime3[episode - 1])
			/ (i + 1);
		if (episode == 1)
		    System.out.println("Progress:"
			    + nf.format(100 * (double) episode / episodesNum)
			    + "%");
		else if (episode % 100 == 0)
		{
		    System.out.println("Progress:"
			    + nf.format(100 * (double) episode / episodesNum)
			    + "%");
		}
		
	    }

	}
	try
	{
	    // 写时间文件
	    BufferedWriter timewriter = new BufferedWriter(new FileWriter("./"
		    + this.getClass().getSimpleName() + "time.csv"));
	    BufferedWriter timewriter1 = new BufferedWriter(new FileWriter("./"
		    + this.getClass().getSimpleName() + "time_agent1.csv"));
	    BufferedWriter timewriter2 = new BufferedWriter(new FileWriter("./"
		    + this.getClass().getSimpleName() + "time_agent2.csv"));
	    BufferedWriter timewriter3 = new BufferedWriter(new FileWriter("./"
		    + this.getClass().getSimpleName() + "time_agent3.csv"));

	    for (int i = 0; i < averageEpisodeTime.length; i++)
	    {
		timewriter.write(averageEpisodeTime[i] + ", ");
		timewriter1.write(averageEpisodeTime1[i] + ", ");
		timewriter2.write(averageEpisodeTime2[i] + ", ");
		timewriter3.write(averageEpisodeTime3[i] + ", ");
	    }
	    timewriter.close();
	    timewriter1.close();
	    timewriter2.close();
	    timewriter3.close();

	    // 写步数文件
	    BufferedWriter stepswriter = new BufferedWriter(new FileWriter("./"
		    + this.getClass().getSimpleName() + ".csv"));
	    BufferedWriter stepswriter1 = new BufferedWriter(new FileWriter(
		    "./" + this.getClass().getSimpleName() + "_agent1.csv"));
	    BufferedWriter stepswriter2 = new BufferedWriter(new FileWriter(
		    "./" + this.getClass().getSimpleName() + "_agent2.csv"));
	    BufferedWriter stepswriter3 = new BufferedWriter(new FileWriter(
		    "./" + this.getClass().getSimpleName() + "_agent3.csv"));
	    for (int i = 0; i < averageStepsToTerminal.length; i++)
	    {
		stepswriter.write(averageStepsToTerminal[i] + ", ");
		stepswriter1.write(averageStepsToTerminal1[i] + ", ");
		stepswriter2.write(averageStepsToTerminal2[i] + ", ");
		stepswriter3.write(averageStepsToTerminal3[i] + ", ");
	    }
	    stepswriter.close();
	    stepswriter1.close();
	    stepswriter2.close();
	    stepswriter3.close();
	    
	    
	}
	catch (IOException e)
	{
	    e.printStackTrace();
	}
	System.out.println();
    }

    public void printQ()
    {

	double[][][] arr1 = Q1[0][2][3];
	double[][][] arr2 = Q2[0][2][3];
	double[][][] arr3 = Q3[0][2][3];
	for (int k = 0; k < 4; k++)
	{
	    System.out.println("k choose " + ACTIONS_DES[k]);
	    for (int i1 = 0; i1 < 4; i1++)
		System.out.print("        " + ACTIONS_DES[i1] + "   \t\t");
	    System.out.println();
	    for (int i = 0; i < 4; i++)
	    {
		System.out.print(ACTIONS_DES[i] + ": ");
		for (int j = 0; j < 4; j++)
		{
		    System.out.print("(" + numberFormat.format(arr1[i][j][k])
			    + "," + numberFormat.format(arr2[i][j][k]) + ","
			    + numberFormat.format(arr3[i][j][k]) + ")  ");
		}
		System.out.println();
	    }
	    System.out.println();
	}
    }

    public void testTrainingResult()
    {
	epsilon = 0.0;
	loc[0] = 0;
	loc[1] = 2;
	loc[2] = 3;

	printQ();

	//int[] a = getEquilibriumAction(0, 2, 3);
	//System.out.println("Equilibrium: (" + ACTIONS_DES[a[0]] + ","
	//	+ ACTIONS_DES[a[1]] + "," + ACTIONS_DES[a[2]] + ")");

	Tools.enableDebug(false);
	System.out
		.println("\n\n***********Training Finished,Test Learning Performance********************");

	loc[0] = 0;
	loc[1] = 2;
	loc[2] = 3;
	gotA = gotB = gotC = false;
	while (!gameover())
	{
	    int s1 = preLoc[0] = loc[0];
	    int s2 = preLoc[1] = loc[1];
	    int s3 = preLoc[2] = loc[2];
	    
	    double[] CE = computeCE(0, s1, s2, s3);
	    int[] eqJA_CE = getAction_CE(CE);
	    //double[] CE1 = computeCE(0, s1, s2, s3);
	    //double[] CE2 = computeCE(1, s1, s2, s3);
	    //double[] CE3 = computeCE(2, s1, s2, s3);
	    //int[] eqJA_CE1 = getAction_CE(CE1);
	    //int[] eqJA_CE2 = getAction_CE(CE2);
	    //int[] eqJA_CE3 = getAction_CE(CE3); 
		
	    action[0] = eqJA_CE[0];
	    action[1] = eqJA_CE[1];
	    action[2] = eqJA_CE[2];
	    
	    //getAction();
	    setReward();
	    String s = "";

	    if (isGoal(0, preLoc[0]))
	    {
		s += "Agent1 got the Goal";
	    }
	    else
	    {
		s += "Agent1:" + preLoc[0] + "->" + getDirection(action[0]);
	    }
	    if (isGoal(1, preLoc[1]))
	    {
		s += ",  Agent2 got the Goal, ";
	    }
	    else
	    {
		s += ",  Agent2:" + preLoc[1] + "->" + getDirection(action[1]);
	    }

	    if (isGoal(2, preLoc[2]))
	    {
		s += ",  Agent3 got the Goal, ";
	    }
	    else
	    {
		s += ",  Agent3:" + preLoc[2] + "->" + getDirection(action[2]);
	    }

	    System.out.println(s);
	}
	epsilon = epsilonValue;
    }

    public String getDirection(int action)
    {
	return ACTIONS_DES[action];
    }

    public boolean gameover()
    {
	if (loc[0] == GOAL_A && loc[1] == GOAL_B && loc[2] == GOAL_C)
	    return true;
	return false;
    }

    /**
     * <code>
     * 	6 7 8
     * 	3 4 5
     * 	0 1 2
     * </code>
     */
    public void resetGame()
    {
	loc[0] = 0;
	loc[1] = 2;
	loc[2] = 3;
	action[0] = action[1] = action[2] = 0;
	for (int i = 0; i < Q1.length; i++)
	{
	    for (int j = 0; j < Q1[0].length; j++)
	    {
		for (int k = 0; k < Q1[0][0].length; k++)
		{
		    for (int m = 0; m < Q1[0][0][0].length; m++)
		    {
			for (int p = 0; p < Q1[0][0][0][0].length; p++)
			{
			    for (int q = 0; q < Q1[0][0][0][0][0].length; q++)
			    {
				Q1[i][j][k][m][p][q] = Q2[i][j][k][m][p][q] = Q3[i][j][k][m][p][q] = 0.0;
				
				//(Math.random() - 0.5) / 10;
				Q11[i][j][k][m][p][q] = Q12[i][j][k][m][p][q] = Q13[i][j][k][m][p][q] = (Math
					.random() - 0.5) / 10;
				Q21[i][j][k][m][p][q] = Q22[i][j][k][m][p][q] = Q23[i][j][k][m][p][q] = (Math
					.random() - 0.5) / 10;
				Q31[i][j][k][m][p][q] = Q32[i][j][k][m][p][q] = Q33[i][j][k][m][p][q] = (Math
					.random() - 0.5) / 10;
			    }
			}
		    }
		}
	    }
	}
    }

    // location 0-15,action 0-3
    // right 0,up 1,left 2,down3
    public void setReward()
    {
	int locationNext1 = -1;
	int locationNext2 = -1;
	int locationNext3 = -1;

	locationNext1 = getLocationNext(loc[0], action[0]);
	locationNext2 = getLocationNext(loc[1], action[1]);
	locationNext3 = getLocationNext(loc[2], action[2]);
	reward[0] = reward[1] = reward[2] = 0;
	// set reward1
	// 到达目的地，r=100
	if (gotA == true)
	{
	    locationNext1 = loc[0];
	    reward[0] = 0;
	}
	else
	{
	    if (isGoal(0, locationNext1) == true)
	    {
		reward[0] = 100;// / ((double)stepA);
		loc[0] = locationNext1;
	    }
	    // 1与2或者1与3相撞，r=-10
	    else if (isGoal(0, locationNext1) == false
		    && ((locationNext1 == locationNext2) || locationNext1 == locationNext3))
	    {
		reward[0] = -10;
	    }
	    else if (loc[0] == locationNext1)
	    {// 撞墙上了
		reward[0] = -10;
	    }
	    else
	    {
		reward[0] = -1;
		loc[0] = locationNext1;
	    }
	}
	// set reward2
	if (gotB == true)
	{
	    locationNext2 = loc[1];
	    reward[1] = 0;
	}
	else
	{
	    if (isGoal(1, locationNext2) == true)
	    {
		reward[1] = 100;// / ((double)stepB);
		loc[1] = locationNext2;
	    }
	    else if (isGoal(1, locationNext2) == false
		    && ((locationNext1 == locationNext2))
		    || locationNext2 == locationNext3)
	    {
		reward[1] = -10;
	    }
	    else if (loc[1] == locationNext2)
	    {
		reward[1] = -10;
	    }
	    else
	    {
		reward[1] = -1;
		loc[1] = locationNext2;
	    }
	}
	// set reward3
	if (gotC == true)
	{
	    locationNext3 = loc[2];
	    reward[2] = 0;
	}
	else
	{
	    if (isGoal(2, locationNext3) == true)
	    {
		reward[2] = 100;// / ((double)stepC);
		loc[2] = locationNext3;
	    }
	    else if (isGoal(2, locationNext3) == false
		    && ((locationNext3 == locationNext1))
		    || locationNext3 == locationNext2)
	    {
		reward[2] = -10;
	    }
	    else if (loc[2] == locationNext3)
	    {
		reward[2] = -10;
	    }
	    else
	    {
		reward[2] = -1;
		loc[2] = locationNext3;
	    }
	}
    }

    public int getLocationNext(int location, int action)
    {
	int l = -1;
	int a = location / 4;
	int b = location - a * 4;
	// System.out.println("location:"+location+",action:"+ACTIONS_DES[action]);
	// right 0,up 1,left 2,down3
	if (action == RIGHT)
	{
	    if (b < 3)
		b = b + 1;
	}
	if (action == UP)
	{
	    if (a < 3)
		a = a + 1;
	}
	if (action == LEFT)
	{
	    if (b > 0)
		b = b - 1;
	}
	if (action == DOWN)
	{
	    if (a > 0)
		a = a - 1;
	}
	l = a * 4 + b;
	// System.out.println("next location:"+l);
	return l;
    }

    public boolean isGoal(int agent, int loc)
    {
	boolean f = false;
	if (agent == 0 && loc == GOAL_A)
	{
	    f = true;
	    gotA = true;
	}
	if (agent == 1 && loc == GOAL_B)
	{
	    f = true;
	    gotB = true;
	}
	if (agent == 2 && loc == GOAL_C)
	{
	    f = true;
	    gotC = true;
	}
	return f;
    }

    public void selfIntro()
    {
	System.out.println("#####GridGame 1 Using Meta vs Meta vs Meta#####");
    }



    @Override
    public void getAction()
    {

    }

    

    public double getEquilibrium_Q(int agent, int s1, int s2, int s3)
    {
	return 0;
    }
   




    @Override
    public double getEquilibrium(int agent, int s1, int s2, int s3)
    {
	// TODO Auto-generated method stub
	return 0;
    }

    /**
     * The followings are added by Huyujing
     */
    
    /**
     * find the element that both exist in Set s1 and Set s2
     * @param s1
     * @param s2
     * @return s1 union s2
     */
    private static Set<Pair<Integer, Integer, Integer>> intersect(Set<Pair<Integer, Integer, Integer>> s1, 
	    Set<Pair<Integer, Integer, Integer>> s2)
   {
	 Set<Pair<Integer, Integer, Integer>> s = new HashSet<Pair<Integer, Integer, Integer>>();
	 for (Pair<Integer, Integer, Integer> p : s1)
	 {
    	    if (s2.contains(p))
    		s.add(p);
	 }
	 return s; 
     }
    
    
    
    
    /**
     * get action from correlated equilibria
     * @param agent
     * @param s1
     * @param s2
     * @param s3
     * @return: 返回CE平衡,一个长度为64的double数组,每个元素代表对应联合动作的概率
     */
    public double[] computeCE(int agent, int s1, int s2, int s3)
    {
	
	double[][][] q1, q2, q3;
	
	/**
	if(agent == 0)
	{
	   q1 = Q11[s1][s2][s3];
	   q2 = Q12[s1][s2][s3];
	   q3 = Q13[s1][s2][s3];
	}
	else if(agent == 1)
	{
	    q1 = Q21[s1][s2][s3];
	    q2 = Q22[s1][s2][s3];
	    q3 = Q23[s1][s2][s3];
	}
	else
	{
	    q1 = Q31[s1][s2][s3];
	    q2 = Q32[s1][s2][s3];
	    q3 = Q33[s1][s2][s3];
	}
	*/
	
	q1 = Q1[s1][s2][s3];
	q2 = Q2[s1][s2][s3];
	q3 = Q3[s1][s2][s3]; 
	
	//return computeCE_utilitarian(agent, q1, q2, q3);
	return computeCE_egalitarian(agent, q1, q2, q3);
	//return computeCE_plutocratic(agent, q1, q2, q3);
	//return computeCE_dictatorial(agent, q1, q2, q3);
    }
    
    public double[] computeCE_utilitarian(int agent, double[][][] q1, double[][][] q2, double[][][] q3 )
    {
	
	double[] ce = new double[64];
	for( int i = 0; i < 64; i++ )
	    ce[i] = 0.0;
	
	/**
	 * 一共64个变量(64个联合动作)
	 * 至少36个约束条件
	 */
	SizableProblemI problem = new Problem(36, 64);
	problem.getMetadata().put("lp.isMaximize", "true");
	
	
	try 
	{
	    /**
	     * 设置目标函数
	     * 最大化所有联合动作的总效用
	     */
	    for( int i = 0; i < 4; i++ )
	    {
		for( int j = 0; j < 4; j++ )
		{
		    for( int k = 0; k < 4; k++ )
		    {
			String a = getDirection(i);
			String b = getDirection(j);
			String c = getDirection(k);
			
			//联合动作(a,b,c)对应的变量即是其执行概率,该变量命名为"a:b:c"
			//其系数显然是对应的Q值的和
			problem.newVariable(a+":"+b+":"+c).setObjectiveCoefficient(
				q1[i][j][k]+q2[i][j][k]+q3[i][j][k]);
		    }
		}
	    }
	    
	    /**
	     * 设置问题的约束条件,即是
	     */
	    setConstraints_CE( problem, q1, q2, q3 );
	    
	    /**
	     * 建立线性规划并求解
	     */
	    LinearProgrammingI iLP;
	    iLP = new DenseSimplex(problem);
	    iLP.solve();
	    VectorI v = iLP.getSolution();
	    ce = v.getArray();
	    
	    
	    return ce;
	}
	catch(Exception e)
	{
	    //e.printStackTrace();
	    return null;
	}

    }
    
    public double[] computeCE_egalitarian(int agent, double[][][] q1, double[][][] q2, double[][][] q3 )
    {
	
	double[] ce = new double[64];
	for( int i = 0; i < 64; i++ )
	    ce[i] = 0.0;
	
	/**
	 * 一共64个变量(64个联合动作)
	 * 至少36个约束条件
	 */
	SizableProblemI problem = new Problem(36, 64);
	problem.getMetadata().put("lp.isMaximize", "true");
	
	
	try 
	{
	    /**
	     * 设置目标函数为agent 1的期望值
	     */
	    for( int i = 0; i < 4; i++ )
	    {
		for( int j = 0; j < 4; j++ )
		{
		    for( int k = 0; k < 4; k++ )
		    {
			String a = getDirection(i);
			String b = getDirection(j);
			String c = getDirection(k);
			
			//联合动作(a,b,c)对应的变量即是其执行概率,该变量命名为"a:b:c"
			//其系数显然是对应的Q值的和
			problem.newVariable(a+":"+b+":"+c).setObjectiveCoefficient(
				-q1[i][j][k]);
		    }
		}
	    }
	    
	    /**
	     * 设置问题的约束条件,即是
	     */
	    setConstraints_CE( problem, q1, q2, q3 );
	    
	    /**
	     * 建立线性规划并求解
	     */
	    LinearProgrammingI iLP;
	    iLP = new DenseSimplex(problem);
	    double ans = iLP.solve();
	    ce = iLP.getSolution().getArray();
	    
	    /**
	     * 目标函数换成agent 2的期望值
	     */
	    for( int i = 0; i < 4; i++ )
	    {
		for( int j = 0; j < 4; j++ )
		{
		    for( int k = 0; k < 4; k++ )
		    {
			String a = getDirection(i);
			String b = getDirection(j);
			String c = getDirection(k);
			
			//联合动作(a,b,c)对应的变量即是其执行概率,该变量命名为"a:b:c"
			//其系数显然是对应的Q值的和
			problem.getVariable(a+":"+b+":"+c).setObjectiveCoefficient(
				-q2[i][j][k]);
		    }
		}
	    }
	    iLP.setProblem(problem);
	    double ans2 = iLP.solve();
	    if( ans < ans2 )
	    {
		ans = ans2;
		ce = iLP.getSolution().getArray();
	    }
	   
	    
	    /**
	     * 再把目标函数换成agent 3的期望值
	     */
	    for( int i = 0; i < 4; i++ )
	    {
		for( int j = 0; j < 4; j++ )
		{
		    for( int k = 0; k < 4; k++ )
		    {
			String a = getDirection(i);
			String b = getDirection(j);
			String c = getDirection(k);
			
			//联合动作(a,b,c)对应的变量即是其执行概率,该变量命名为"a:b:c"
			//其系数显然是对应的Q值的和
			problem.getVariable(a+":"+b+":"+c).setObjectiveCoefficient(
				-q3[i][j][k]);
		    }
		}
	    }
	    iLP.setProblem(problem);
	    double ans3 = iLP.solve();
	    if( ans < ans3 )
	    {
		ans = ans3;
		ce = iLP.getSolution().getArray();
	    }
	    
	    return ce;
	}
	catch(Exception e)
	{
	    //e.printStackTrace();
	    return null;
	}

    }   
    
    public double[] computeCE_plutocratic(int agent, double[][][] q1, double[][][] q2, double[][][] q3 )
    {
	
	double[] ce = new double[64];
	for( int i = 0; i < 64; i++ )
	    ce[i] = 0.0;
	
	/**
	 * 一共64个变量(64个联合动作)
	 * 至少36个约束条件
	 */
	SizableProblemI problem = new Problem(36, 64);
	problem.getMetadata().put("lp.isMaximize", "true");
	
	
	try 
	{
	    /**
	     * 设置目标函数为agent 1的期望值
	     */
	    for( int i = 0; i < 4; i++ )
	    {
		for( int j = 0; j < 4; j++ )
		{
		    for( int k = 0; k < 4; k++ )
		    {
			String a = getDirection(i);
			String b = getDirection(j);
			String c = getDirection(k);
			
			//联合动作(a,b,c)对应的变量即是其执行概率,该变量命名为"a:b:c"
			//其系数显然是对应的Q值的和
			problem.newVariable(a+":"+b+":"+c).setObjectiveCoefficient(
				q1[i][j][k]);
		    }
		}
	    }
	    
	    /**
	     * 设置问题的约束条件,即是
	     */
	    setConstraints_CE( problem, q1, q2, q3 );
	    
	    /**
	     * 建立线性规划并求解
	     */
	    LinearProgrammingI iLP;
	    iLP = new DenseSimplex(problem);
	    double ans = iLP.solve();
	    ce = iLP.getSolution().getArray();
	    
	    /**
	     * 目标函数换成agent 2的期望值
	     */
	    for( int i = 0; i < 4; i++ )
	    {
		for( int j = 0; j < 4; j++ )
		{
		    for( int k = 0; k < 4; k++ )
		    {
			String a = getDirection(i);
			String b = getDirection(j);
			String c = getDirection(k);
			
			//联合动作(a,b,c)对应的变量即是其执行概率,该变量命名为"a:b:c"
			//其系数显然是对应的Q值的和
			problem.getVariable(a+":"+b+":"+c).setObjectiveCoefficient(
				q2[i][j][k]);
		    }
		}
	    }
	    iLP.setProblem(problem);
	    double ans2 = iLP.solve();
	    if( ans < ans2 )
	    {
		ans = ans2;
		ce = iLP.getSolution().getArray();
	    }
	   
	    
	    /**
	     * 再把目标函数换成agent 3的期望值
	     */
	    for( int i = 0; i < 4; i++ )
	    {
		for( int j = 0; j < 4; j++ )
		{
		    for( int k = 0; k < 4; k++ )
		    {
			String a = getDirection(i);
			String b = getDirection(j);
			String c = getDirection(k);
			
			//联合动作(a,b,c)对应的变量即是其执行概率,该变量命名为"a:b:c"
			//其系数显然是对应的Q值的和
			problem.getVariable(a+":"+b+":"+c).setObjectiveCoefficient(
				q3[i][j][k]);
		    }
		}
	    }
	    iLP.setProblem(problem);
	    double ans3 = iLP.solve();
	    if( ans < ans3 )
	    {
		ans = ans3;
		ce = iLP.getSolution().getArray();
	    }
	    
	    return ce;
	}
	catch(Exception e)
	{
	    //e.printStackTrace();
	    return null;
	}

    }
    
    /**
     * dictatorial是非集中式的算法
     * 最大化特定agent的总收益,所以每个agent分开算
     */
    public double[] computeCE_dictatorial(int agent, double[][][] q1, double[][][] q2, double[][][] q3 )
    {
	
	double[] ce = new double[64];
	for( int i = 0; i < 64; i++ )
	    ce[i] = 0.0;
	
	/**
	 * 一共64个变量(64个联合动作)
	 * 至少36个约束条件
	 */
	SizableProblemI problem = new Problem(36, 64);
	problem.getMetadata().put("lp.isMaximize", "true");
	
	
	try 
	{
	    /**
	     * 设置目标函数为agent 1的期望值
	     */
	    for( int i = 0; i < 4; i++ )
	    {
		for( int j = 0; j < 4; j++ )
		{
		    for( int k = 0; k < 4; k++ )
		    {
			String a = getDirection(i);
			String b = getDirection(j);
			String c = getDirection(k);
			
			//联合动作(a,b,c)对应的变量即是其执行概率,该变量命名为"a:b:c"
			//其系数显然是对应的Q值的和
			if( agent == 0 )
			{
			    problem.newVariable(a+":"+b+":"+c).setObjectiveCoefficient(
					q1[i][j][k]);
			}
			else if( agent == 1 )
			{
			    problem.newVariable(a+":"+b+":"+c).setObjectiveCoefficient(
					q2[i][j][k]);
			}
			else
			{
			    problem.newVariable(a+":"+b+":"+c).setObjectiveCoefficient(
					q3[i][j][k]);
			}
		    }
		}
	    }
	    
	    /**
	     * 设置问题的约束条件,即是
	     */
	    setConstraints_CE( problem, q1, q2, q3 );
	    
	    /**
	     * 建立线性规划并求解
	     */
	    LinearProgrammingI iLP;
	    iLP = new DenseSimplex(problem);
	    iLP.solve();
	    ce = iLP.getSolution().getArray();
	    
	    return ce;
	}
	catch(Exception e)
	{
	    //e.printStackTrace();
	    return null;
	}
    }
    
    /**
     * 为线性规划问题problem设置CE平衡的约束条件
     */
    private void setConstraints_CE(SizableProblemI problem, double[][][] q1,
	    double[][][] q2, double[][][] q3 ) throws Exception
    {
	//对于CE平衡的不等式,一共36个约束条件
	for( int agent = 0; agent < 3; agent++ )
	{
	    double[][][] q = null;
	    if( agent == 0 )
		q = q1;
	    else if( agent == 1 )
		q = q2;
	    else
		q = q3;
	    
	    /**
	     * loop for action ai
	     */
	    for( int ai = 0; ai < 4; ai++ )
	    {
		/**
		 * loop for action ai'
		 */
		for( int aip = 0; aip < 4; aip++ )
		{
		    if( aip == ai )
			continue;
		    
		    /**
		     * 为ai建立约束条件
		     * 此约束条件的名称为"agentID(ai-ai')"
		     */
		    String aiString = getDirection(ai);
		    String aipString = getDirection(aip);
		    String conString = agent+"("+aiString+"-"+aipString+")";
		    problem.newConstraint(conString).setType(Constraint.GREATER).setRightHandSide(0.0);
		    //需要遍历另外两个agent的联合动作
		    for( int j = 0; j < 4; j++ )
		    {
			for( int k = 0; k < 4; k++ )
			{
			    String bString = getDirection(j);
			    String cString = getDirection(k);
			    
			    if( agent == 0 )
				problem.setCoefficientAt(conString, aiString+":"+bString+":"+cString, 
					q[ai][j][k]-q[aip][j][k] );
			    else if( agent == 1 )
				problem.setCoefficientAt(conString, bString+":"+aiString+":"+cString, 
					q[j][ai][k]-q[j][aip][k] );
			    else
				problem.setCoefficientAt(conString, bString+":"+cString+":"+aiString, 
					q[j][k][ai]-q[j][k][aip] );
				
			}
		    }
		}
	    }
	}
	
	/**
	 * 所有联合动作的概率之和为1的约束条件
	 */
	String eqCon = "equalConstraint";
	int index = problem.newConstraint(eqCon).
		setType(Constraint.EQUAL).setRightHandSide(1.0).getRowIndex();
	for( int i = 0; i < 64; i++ )
	{
	    problem.setCoefficientAt(index, i, 1.0);
	}
	
	/**
	 * 每个联合动作的概率之和大等于0的约束条件
	 */
	for( int i = 0; i < 64; i++ )
	{
	    String zeroCon = "aboveZero" + (i+1);
	    index = problem.newConstraint(zeroCon).
	    	setType(Constraint.GREATER).setRightHandSide(0.0).getRowIndex();
	    problem.setCoefficientAt(index, i, 1.0);
	}
	
    }
    
    /**
     * 根据计算出的CE平衡来得到执行的联合动作
     */
    public int[] getAction_CE( double[] correlatedE )
    {
	int[] retAction = new int[3];
	
	if( correlatedE == null )
	{
	    retAction[0] = random.nextInt(4);
	    retAction[1] = random.nextInt(4);
	    retAction[2] = random.nextInt(4);
	    
	    return retAction;
	}
	
	double[] probabilities = new double[64];
	
	probabilities[0] = correlatedE[0];
	for( int i = 1; i < 64; i++ )
	{
	    probabilities[i] =  probabilities[i-1] + correlatedE[i];
	}
	
	double d = random.nextDouble();
	int actionIndex = 0;
	for( int i = 0; i < 64; i++ )
	{
	    if( d < probabilities[i] )
	    {
		actionIndex = i;
		break;
	    }
	}
	retAction[0] = actionIndex/16;
	retAction[1] = (actionIndex - retAction[0]*16)/4;
	retAction[2] = actionIndex%4;
	
	return retAction;
    }
    
    /**
     * 根据计算出的CE平衡得到其对应的Q-value期望值
     */
    public double getValue_CE( int agent, int s1, int s2, int s3, 
	    double[] correlatedE )
    {
	/**
	 * 还是会有计算不出来的情况
	 */
	if( correlatedE == null )
	    return 0.0;
	
	double value = 0.0;
	
	double[][][] q;
	if( agent == 0 )
	    q = Q1[s1][s2][s3];
	else if( agent == 1 )
	    q = Q2[s1][s2][s3];
	else
	    q = Q3[s1][s2][s3];
	
	for( int i = 0; i < 4; i++ )
	    for( int j = 0; j < 4; j++ )
		for( int k = 0; k < 4; k++ )
		{
		    int index = i*16 + j*4 + k;
		    value += correlatedE[index] * q[i][j][k];
		}
	
	return value;
    }
    
        

    
    
    public static void main(String args[])
    {
	GridGame gg = new CEQ();
	Tools.timerBegin();
	gg.train(0.1, 0.2);
	Tools.timerStop();
	gg.testTrainingResult();
    }
}
