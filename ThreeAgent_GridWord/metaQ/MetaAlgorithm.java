package metaQ;

import java.util.HashSet;
import java.util.Set;

import tools.Pair;

public class MetaAlgorithm
{
    /**
     * find the element that both exist in Set s1 and Set s2
     * @param s1
     * @param s2
     * @return s1 union s2
     */
        static Set<Pair<Integer, Integer, Integer>> intersect(
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
    /**
     * 从三个方向上找meta理性。常规上来说，对agent1 ，应该是从row上找，对agent2，从column上找，对agent3，从high上找
     * @param Q
     * @param direction 1--Row 	2--Column	3--High
     * @return  the meta equilibrium set
     */
        static Set<Pair<Integer, Integer, Integer>> minminmax(
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

        private static  double findminminmaxRow(double[][][] Q)
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

        private static  double findminminmaxColumn(double[][][] Q)
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

        private static  double findminminmaxHigh(double[][][] Q)
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
}
