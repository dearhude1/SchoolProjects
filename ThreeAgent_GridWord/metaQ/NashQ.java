package metaQ;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import tools.Pair;
import tools.Tools;

public class NashQ implements GridGame
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

    public static final String[] ACTIONS_DES =
    { "R", "U", "L", "D" };

    public static final int LOOP = 1;//1;
    
    /**
     * add by Huyujing
     * for test the percentage of optimal convergence
     */
    public int optCovgCount = 0;
    public int testStepA = 0;
    public int testStepB = 0;
    public int testStepC = 0;

    public static final int GOAL_A = 14;//13;//14;

    public static final int GOAL_B = 15;//13;//15;

    public static final int GOAL_C = 12;//13;//12;

    public static final int episodesNum = 50000;

    public static final double epsilonValue = 0.01;//0.3;
    
    /**
     * add by Huyujing
     * two epsilon values for dynamic adaptability
     */
    public static final double epsilonHigh = 0.3;
    public static final double epsilonLow = 0.1;

    protected double[][][][][][] Q1;

    protected double[][][][][][] Q2;

    protected double[][][][][][] Q3;

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
    
    /**
     * add by Huyujing
     * the steps used for A,B,C to reach their goals
     */
    protected int stepA = 0;
    protected int stepB = 0;
    protected int stepC = 0;

    public NashQ()
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

		
		int steps = 0;
		int steps1 =0;
		int steps2 =0;
		int steps3 =0;
		long time = 0;
		long time1 = 0;
		long time2 = 0;
		long time3 = 0;
		gotA = gotB = gotC = false;
		long start = System.nanoTime();
		
		stepA = stepB = stepC = 0;
		
		/**
		 * dynamic epsilon?
		 *
		if(episode <= episodesNum/2)
		    epsilon = epsilonHigh;
		else
		    epsilon = epsilonLow;
		*/
		
		/**
		 * initial state s = (0,2,3)
		 */
		int s1 = preLoc[0] = loc[0] = 0;
		int s2 = preLoc[1] = loc[1] = 2;
		int s3 = preLoc[2] = loc[2] = 3;
		
		/**
		 * we compute the equilibrium action for the initial state
		 */
		int[] eqJA = getEqJointAction(s1, s2, s3);
		
		
		// get a time here
		while (!gameover())
		{

		    
		    /**
		     * epsilon-greedy for choosing the actions to be taken 
		     */
		    int a1 = eqJA[0];
		    int a2 = eqJA[1];
		    int a3 = eqJA[2];
		    /**
		    if(randomEpsilon.nextDouble() < epsilon)
			a1 = random.nextInt(4);
		    if(randomEpsilon.nextDouble() < epsilon)
			a2 = random.nextInt(4);
		    if(randomEpsilon.nextDouble() < epsilon)
			a3 = random.nextInt(4);
		    */
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
		    if(!gotA)
		    {
			steps1++;
			time1=System.nanoTime()-start;
			stepA++;
		    }
		    if(!gotB)
		    {
			steps2++;
			time2=System.nanoTime()-start;
			stepB++;
		    }
		    if(!gotC)
		    {
			steps3++;
			time3=System.nanoTime()-start;
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
		    eqJA = getEqJointAction(sp1,sp2,sp3);

		    /**
		     * update Q-values
		     */
		    Q1[s1][s2][s3][a1][a2][a3] = (1 - alpha) * Q1[s1][s2][s3][a1][a2][a3]
			    + alpha * (reward[0] + beta * Q1[sp1][sp2][sp3][eqJA[0]][eqJA[1]][eqJA[2]]);
		    
		    Q2[s1][s2][s3][a1][a2][a3] = (1 - alpha) * Q2[s1][s2][s3][a1][a2][a3]
		            + alpha * (reward[1] + beta * Q2[sp1][sp2][sp3][eqJA[0]][eqJA[1]][eqJA[2]]);
		    
		    Q3[s1][s2][s3][a1][a2][a3] = (1 - alpha) * Q3[s1][s2][s3][a1][a2][a3]
		            + alpha * (reward[2] + beta * Q3[sp1][sp2][sp3][eqJA[0]][eqJA[1]][eqJA[2]]);
		    
		    
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
		
		/**
		 * show the progress of training
		 */
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
	    
	    
	    testTrainingResult();

	}
	
	System.out.println("Optimal Converge:"+optCovgCount);
	
	try
	{
	    //写时间文件
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
	    
	    //写步数文件
	    BufferedWriter stepswriter = new BufferedWriter(new FileWriter("./"
		    + this.getClass().getSimpleName() + ".csv"));
	    BufferedWriter stepswriter1 = new BufferedWriter(new FileWriter("./"
		    + this.getClass().getSimpleName() + "_agent1.csv"));
	    BufferedWriter stepswriter2 = new BufferedWriter(new FileWriter("./"
		    + this.getClass().getSimpleName() + "_agent2.csv"));
	    BufferedWriter stepswriter3 = new BufferedWriter(new FileWriter("./"
		    + this.getClass().getSimpleName() + "_agent3.csv"));
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

	int[] a = getEquilibriumAction(0, 2, 3);
	System.out.println("Equilibrium: (" + ACTIONS_DES[a[0]] + ","
		+ ACTIONS_DES[a[1]] + "," + ACTIONS_DES[a[2]] + ")");

	Tools.enableDebug(false);
	System.out.println("\n\n***********Training Finished,Test Learning Performance********************");

	loc[0] = 0;
	loc[1] = 2;
	loc[2] = 3;
	gotA = gotB = gotC = false;
	
	testStepA = testStepB = testStepC = 0;
	while (!gameover()) 
	{
	    preLoc[0] = loc[0];
	    preLoc[1] = loc[1];
		preLoc[2] = loc[2];
		getAction();
		setReward();
		String s = "";
		    
		if (isGoal(0, preLoc[0]))		   
		{			
		    s += "Agent1 got the Goal";		    
		}		    
		else		   
		{			
		    testStepA++;
		    
		    s += "Agent1:" + preLoc[0] + "->" + getDirection(action[0]);		    
		}   
		if (isGoal(1, preLoc[1]))   	    
		{		
		    s += ",  Agent2 got the Goal, ";
		}   
		else   
		{
		    testStepB++;
		    
		    s += ",  Agent2:" + preLoc[1] + "->" + getDirection(action[1]);    
		}		   
		if (isGoal(2, preLoc[2]))		   
		{			
		    s += ",  Agent3 got the Goal, ";		    
		}		   
		else		    
		{
		    testStepC++;
		    
		    s += ",  Agent3:" + preLoc[2] + "->" + getDirection(action[2]);		   
		}		    
		System.out.println(s);		
	    }
	
	if( testStepA == 4 && testStepB == 4 && testStepC == 4)
	    optCovgCount++;

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
				Q1[i][j][k][m][p][q] = Q2[i][j][k][m][p][q] = Q3[i][j][k][m][p][q] = (Math
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
		reward[0] = 100 / ((double)stepA);
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
		reward[1] = 100 / ((double)stepB);
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
		reward[2] = 100 / ((double)stepC);
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
	System.out.println("#####GridGame 1 Using NashQ#####");
    }

    public static void main(String args[])
    {
	GridGame gg = new NashQ();
	Tools.timerBegin();
	gg.train(0.1, 0.2);
	Tools.timerStop();
	gg.testTrainingResult();
    }

    @Override
    public void getAction()
    {
	int s1 = loc[0];
	int s2 = loc[1];
	int s3 = loc[2];
	// three agent's nash 平衡集
	Set<Pair<Integer, Integer, Integer>> NashES = findNashE(Q1[s1][s2][s3],
		Q2[s1][s2][s3], Q3[s1][s2][s3]);

	if (NashES.size() == 0)
	{
	    // 没有纯策略nash均衡，就选择一个最大和最大的
	    Pair<Integer, Integer, Integer> NashE = chooseBestE( s1, s2, s3);
	    action[0] = NashE.v1;
	    action[1] = NashE.v2;
	    action[2] = NashE.v3;
	}
	else
	{
	    // 找一个和最大的Nash平衡

	    Pair<Integer, Integer, Integer> NashE = chooseBestE(NashES, s1, s2,
		    s3);

	    action[0] = NashE.v1;
	    action[1] = NashE.v2;
	    action[2] = NashE.v3;
	}
	// epsilon-greedy 算法
	if (randomEpsilon.nextDouble() < epsilon)
	{
	    action[0] = random.nextInt(4);
	    action[1] = random.nextInt(4);
	    action[2] = random.nextInt(4);
	}

    }

    /**
     * get a equilibrium joint action in  a state
     */
    public int[] getEqJointAction(int s1, int s2, int s3)
    {	
	int[] retActions = new int[3];
	
	/**
	 * compute pure strategy Nash equilibria
	 */
	Set<Pair<Integer, Integer, Integer>> NashES = findNashE(Q1[s1][s2][s3],
		Q2[s1][s2][s3], Q3[s1][s2][s3]);

	/**
	 * if there's no pure strategy Nash equilibria
	 * choose the action which maximizes the sum of all
	 * agents' utility
	 */
	if (NashES.size() == 0)
	{
	    Pair<Integer, Integer, Integer> NashE = chooseBestE(s1, s2, s3);
	    retActions[0] = NashE.v1;
	    retActions[1] = NashE.v2;
	    retActions[2] = NashE.v3;
	}
	else
	{
	    Pair<Integer, Integer, Integer> NashE = chooseBestE(NashES, s1, s2, s3);
	    retActions[0] = NashE.v1;
	    retActions[1] = NashE.v2;
	    retActions[2] = NashE.v3;
	}
	
	return retActions;
    }
    
    @Override
    public double getEquilibrium(int agent, int s1, int s2, int s3)
    {
	int a1 = random.nextInt(4);
	int a2 = random.nextInt(4);
	int a3 = random.nextInt(4);

	Set<Pair<Integer, Integer, Integer>> NashES = findNashE(Q1[s1][s2][s3],
		Q2[s1][s2][s3], Q3[s1][s2][s3]);

	if (NashES.size() == 0)
	{
	 // 没有纯策略nash均衡，就选择一个最大和最大的
	    Pair<Integer, Integer, Integer> NashE = chooseBestE( s1, s2,
		    s3);
	    a1 = NashE.v1;
	    a2 = NashE.v2;
	    a3 = NashE.v3;
	}
	else
	{
	    // 找一个和最大的平衡

	    Pair<Integer, Integer, Integer> NashE = chooseBestE(NashES, s1, s2,
		    s3);

	   a1 = NashE.v1;
	   a2 = NashE.v2;
	   a3 = NashE.v3;
	}

	if (agent == 0)
	{
	    return Q1[s1][s2][s3][a1][a2][a3];
	}
	else if (agent == 1)
	{
	    return Q2[s1][s2][s3][a1][a2][a3];
	}
	else if (agent == 2)
	{
	    return Q3[s1][s2][s3][a1][a2][a3];
	}
	return 0;
    }

    /**
     * 最后输出结果时用到，没有epsilon-greedy
     */
    @Override
    public int[] getEquilibriumAction(int s1, int s2, int s3)
    {
	int[] ret = new int[3];
	int a1 = random.nextInt(4), a2 = random.nextInt(4), a3 = random
		.nextInt(4);
	Set<Pair<Integer, Integer, Integer>> NashES = findNashE(Q1[s1][s2][s3],
		Q2[s1][s2][s3], Q3[s1][s2][s3]);
	if (NashES.size() == 0)
	{
	 // 没有纯策略nash均衡，就选择一个最大和最大的
	    Pair<Integer, Integer, Integer> NashE = chooseBestE( s1, s2,
		    s3);
	    a1 = NashE.v1;
	    a2 = NashE.v2;
	    a3 = NashE.v3;
	}
	else
	{
	    Pair<Integer, Integer, Integer> NashE = chooseBestE(NashES, s1, s2,
		    s3);
	    a1 = NashE.v1;
	    a2 = NashE.v2;
	    a3 = NashE.v3;
	}
	ret[0] = a1;
	ret[1] = a2;
	ret[2] = a3;
	return ret;
    }

    /**
     * 找纯策略nash平衡
     * 
     * @param Q1
     * @param Q2
     * @param Q3
     * @return
     */
    private Set<Pair<Integer, Integer, Integer>> findNashE(double[][][] Q1,
	    double[][][] Q2, double[][][] Q3)
    {
	Set<Pair<Integer, Integer, Integer>> NashES = new HashSet<Pair<Integer, Integer, Integer>>();
	int row = Q1.length;
	int column = Q1[0].length;
	int high = Q1[0][0].length;
	boolean[][][] MaxQ1, MaxQ2, MaxQ3;
	MaxQ1 = new boolean[row][column][high];
	MaxQ2 = new boolean[row][column][high];
	MaxQ3 = new boolean[row][column][high];
	double MaxValue1 = Q1[0][0][0];
	for (int j = 0; j < column; j++)
	{
	    for (int k = 0; k < high; k++)
	    {
		MaxValue1 = 0;
		// 必须先找到最大值，再把和最大值相等的数都标记出来
		for (int i = 0; i < row; i++)
		{
		    if (Q1[i][j][k] >= MaxValue1)
		    {
			MaxValue1 = Q1[i][j][k];
		    }
		}
		for (int i = 0; i < row; i++)
		{
		    if (Q1[i][j][k] >= MaxValue1)
		    {
			MaxQ1[i][j][k] = true;
		    }
		}

	    }
	}
	double MaxValue2 = Q2[0][0][0];
	for (int i = 0; i < row; i++)
	{
	    for (int k = 0; k < high; k++)
	    {
		MaxValue2 = 0;
		for (int j = 0; j < column; j++)
		{
		    if (Q2[i][j][k] >= MaxValue2)
		    {
			MaxValue2 = Q2[i][j][k];
		    }
		}
		for (int j = 0; j < column; j++)
		{
		    if (Q2[i][j][k] >= MaxValue2)
		    {
			MaxQ2[i][j][k] = true;
		    }
		}
	    }
	}
	double MaxValue3 = Q3[0][0][0];
	for (int i = 0; i < row; i++)
	{
	    for (int j = 0; j < column; j++)
	    {
		MaxValue3 = 0;
		for (int k = 0; k < high; k++)
		{
		    if (Q3[i][j][k] >= MaxValue3)
		    {
			MaxValue3 = Q3[i][j][k];
		    }
		}
		for (int k = 0; k < high; k++)
		{
		    if (Q3[i][j][k] >= MaxValue3)
		    {
			MaxQ3[i][j][k] = true;
		    }
		}
	    }
	}

	for (int i = 0; i < row; i++)
	{
	    for (int j = 0; j < column; j++)
	    {
		for (int k = 0; k < high; k++)
		{
		    if (MaxQ1[i][j][k] && MaxQ2[i][j][k] && MaxQ3[i][j][k])
		    {
			NashES
				.add(new Pair<Integer, Integer, Integer>(i, j,
					k));
		    }
		}
	    }
	}

	return NashES;
    }

    protected Pair<Integer, Integer, Integer> chooseBestE(
	    Set<Pair<Integer, Integer, Integer>> sym, int s1, int s2, int s3)
    {
	Pair<Integer, Integer, Integer> metaE = (Pair<Integer, Integer, Integer>) (sym
		.toArray()[random.nextInt(sym.size())]);
	double tempsum = Q1[s1][s2][s3][metaE.v1][metaE.v2][metaE.v3]
		+ Q2[s1][s2][s3][metaE.v1][metaE.v2][metaE.v3]
		+ Q3[s1][s2][s3][metaE.v1][metaE.v2][metaE.v3];
	for (Pair<Integer, Integer, Integer> p : sym)
	{
	    double sum = Q1[s1][s2][s3][p.v1][p.v2][p.v3]
		    + Q2[s1][s2][s3][p.v1][p.v2][p.v3]
		    + Q3[s1][s2][s3][p.v1][p.v2][p.v3];
	    if (sum > tempsum)
	    {
		tempsum = sum;
		metaE = p;
	    }
	}
	return metaE;
    }

    /**
     * 在没有平衡的情况下找所有收益组合中综合最大的一对
     * @param s1
     * @param s2
     * @param s3
     * @return
     */
    protected Pair<Integer, Integer, Integer> chooseBestE(int s1, int s2, int s3)
    {
	Set<Pair<Integer, Integer, Integer>> AllSet = new HashSet<Pair<Integer, Integer, Integer>>();
	for (int a1 = 0; a1 < 4; a1++)
	{
	    for (int a2 = 0; a2 < 4; a2++)
	    {
		for (int a3 = 0; a3 < 4; a3++)
		{
		    Pair<Integer, Integer, Integer> p = new Pair<Integer, Integer, Integer>(
			    a1, a2, a3);
		    AllSet.add(p);
		}
	    }
	}

	Pair<Integer, Integer, Integer> metaE = (Pair<Integer, Integer, Integer>) (AllSet
		.toArray()[random.nextInt(AllSet.size())]);
	double tempsum = Q1[s1][s2][s3][metaE.v1][metaE.v2][metaE.v3]
		+ Q2[s1][s2][s3][metaE.v1][metaE.v2][metaE.v3]
		+ Q3[s1][s2][s3][metaE.v1][metaE.v2][metaE.v3];
	for (Pair<Integer, Integer, Integer> p : AllSet)
	{
	    double sum = Q1[s1][s2][s3][p.v1][p.v2][p.v3]
		    + Q2[s1][s2][s3][p.v1][p.v2][p.v3]
		    + Q3[s1][s2][s3][p.v1][p.v2][p.v3];
	    if (sum > tempsum)
	    {
		tempsum = sum;
		metaE = p;
	    }
	}
	return metaE;
    }

}
