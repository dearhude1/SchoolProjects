package metaQ;

import java.util.HashSet;
import java.util.Set;

import tools.Pair;

public class MetaQGridGameTwoTest
{

    public static void main(String args[])
    {

	test();
    }

    public static void test()
    {
	double[][][] Q1 =
	{
	{
	{ 1.5, 0.5 },
	{ 0.5, 4 } },
	{
	{ 2, 1 },
	{ 1, 0 } }

	};
	double[][][] Q2 =
	{
	{
	{ 1.5, 0.5 },
	{ 2, 1 } },
	{
	{ 0.5, -0.5 },
	{ 1, 0 } }

	};
	double[][][] Q3 =
	{
	{
	{ 1.5, 2 },
	{ 0.5, 1 } },
	{
	{ 0.5, 1 },
	{ -0.5, 0 } }

	};
	System.out.println("Nash Equlibrium:");
	findNashE(Q1, Q2, Q3);

	for (Pair<Integer, Integer, Integer> p : findNashE(Q1, Q2, Q3))
	{
	    System.out.println(p.toString());
	}

	// for(int i =0;i<arr.length;i++){
	// for(int j=0;j<arr[0].length;j++){
	// for(int k=0;k<arr[0][0].length;k++){
	// System.out.println(arr[i][j][k]);
	// }
	// }
	// }
	// minminmax(Q1,1);
	// for (Pair<Integer, Integer, Integer> p:minminmax(Q1, 1)) {
	// System.out.println(p.toString());
	// }
	//
	// System.out.println("Q1-----------------------------------------");
	// for (Pair<Integer, Integer, Integer> p:minminmax(Q2, 2)) {
	// System.out.println(p.toString());
	// }
	// minminmax(Q2,2);
	// System.out.println("Q2-----------------------------------------");
	// for (Pair<Integer, Integer, Integer> p:minminmax(Q3, 3)) {
	// System.out.println(p.toString());
	// }
	// minminmax(Q3,3);
	// System.out.println("Q3-----------------------------------------");
	// Set<Pair<Integer, Integer, Integer>> temp = intersect(minminmax(
	// Q1, 1), minminmax(Q2,
	// 2));
	// Set<Pair<Integer, Integer, Integer>> sym = intersect(minminmax(
	// Q3, 3), temp);
	// for (Pair<Integer, Integer, Integer> p:sym) {
	// System.out.println(p.toString());
	// }
    }

    private static Set<Pair<Integer, Integer, Integer>> intersect(
	    Set<Pair<Integer, Integer, Integer>> s1,
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

    private static Set<Pair<Integer, Integer, Integer>> minminmax(
	    double[][][] Q, int direction)
    {
	Set<Pair<Integer, Integer, Integer>> set = new HashSet<Pair<Integer, Integer, Integer>>();
	int M = Q.length;
	int N = Q[0].length;
	int K = Q[0][0].length;

	double threshold = 0;
	if (direction == 1)
	{
	    threshold = findminminmaxRow(Q);
	}
	else if (direction == 2)
	{
	    threshold = findminminmaxColumn(Q);
	}
	else if (direction == 3)
	{
	    threshold = findminminmaxHigh(Q);
	}

	for (int i = 0; i < M; i++)
	{
	    for (int j = 0; j < N; j++)
	    {
		for (int k = 0; k < K; k++)
		{
		    if (Q[i][j][k] >= threshold)
		    {
			set.add(new Pair<Integer, Integer, Integer>(i, j, k));
		    }
		}
	    }
	}
	return set;

    }

    private static double findminminmaxRow(double[][][] Q)
    {
	int M = Q.length;
	int N = Q[0].length;
	int K = Q[0][0].length;

	// 表示阈值的那个数的下标
	int p = 0, q = 0, r = 0;
	// 找M方向上最大,存在一个二维数组中
	int[][] Ms = new int[N][K];
	int temp = 0;
	for (int n = 0; n < N; n++)
	{
	    for (int k = 0; k < K; k++)
	    {
		for (int m = 0; m < M; m++)
		{
		    if (Q[m][n][k] > Q[temp][n][k])
		    {
			temp = m;
		    }
		}
		Ms[n][k] = temp;
	    }
	}

	// 找N方向上最小,存于一个一维数组中
	int[] Ns = new int[K];
	for (int k = 0; k < K; k++)
	{
	    for (int n = 0; n < N; n++)
	    {
		if (Q[(Ms[n][k])][n][k] < Q[(Ms[n][k])][temp][k])
		{
		    temp = n;
		}
	    }
	    Ns[k] = temp;
	}

	// 找K方向上最小，存在r中
	for (int k = 0; k < K; k++)
	{
	    int n = Ns[k];
	    int m = Ms[n][k];
	    if (Q[m][n][k] < Q[m][n][temp])
	    {
		temp = k;
	    }
	}
	r = temp;

	q = Ns[r];
	p = Ms[q][r];
	return Q[p][q][r];
    }

    private static double findminminmaxColumn(double[][][] Q)
    {
	int M = Q.length;
	int N = Q[0].length;
	int K = Q[0][0].length;

	// 表示阈值的那个数的下标
	int p = 0, q = 0, r = 0;
	// 找N方向上最大,存在一个二维数组中
	int[][] Ns = new int[M][K];
	int temp = 0;
	for (int m = 0; m < M; m++)
	{
	    for (int k = 0; k < K; k++)
	    {
		for (int n = 0; n < N; n++)
		{
		    if (Q[m][n][k] > Q[m][temp][k])
		    {
			temp = n;
		    }
		}
		Ns[m][k] = temp;
	    }
	}

	// 找K方向上最小,存于一个一维数组中
	int[] Ks = new int[M];
	for (int m = 0; m < M; m++)
	{
	    for (int k = 0; k < K; k++)
	    {
		if (Q[m][(Ns[m][k])][k] < Q[m][(Ns[m][k])][temp])
		{
		    temp = k;
		}
	    }
	    Ks[m] = temp;
	}

	// 找M方向上最小，存在r中
	for (int m = 0; m < M; m++)
	{
	    int k = Ks[m];
	    int n = Ns[m][k];
	    if (Q[m][n][k] < Q[temp][n][k])
	    {
		temp = k;
	    }
	}
	p = temp;

	r = Ks[p];
	q = Ns[p][r];
	return Q[p][q][r];
    }

    private static double findminminmaxHigh(double[][][] Q)
    {
	int M = Q.length;
	int N = Q[0].length;
	int K = Q[0][0].length;

	// 表示阈值的那个数的下标
	int p = 0, q = 0, r = 0;
	// 找K方向上最大,存在一个二维数组中
	int[][] Ks = new int[M][N];
	int temp = 0;
	for (int m = 0; m < M; m++)
	{
	    for (int n = 0; n < N; n++)
	    {
		for (int k = 0; k < K; k++)
		{
		    if (Q[m][n][k] > Q[m][n][temp])
		    {
			temp = k;
		    }
		}
		Ks[m][n] = temp;
	    }
	}

	// 找N方向上最小,存于一个一维数组中
	int[] Ns = new int[M];
	for (int m = 0; m < M; m++)
	{
	    for (int n = 0; n < N; n++)
	    {
		if (Q[m][n][Ks[m][n]] < Q[m][temp][Ks[m][n]])
		{
		    temp = n;
		}
	    }
	    Ns[m] = temp;
	}

	// 找M方向上最小，存在r中
	for (int m = 0; m < M; m++)
	{
	    int n = Ns[m];
	    int k = Ks[m][n];
	    if (Q[m][n][k] < Q[temp][n][k])
	    {
		temp = k;
	    }
	}
	p = temp;

	q = Ns[p];
	r = Ks[p][q];
	return Q[p][q][r];
    }

    private static Set<Pair<Integer, Integer, Integer>> findNashE(
	    double[][][] Q1, double[][][] Q2, double[][][] Q3)
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
			NashES.add(new Pair<Integer, Integer, Integer>(i, j, k));
		    }
		}
	    }
	}

	return NashES;
    }
}
