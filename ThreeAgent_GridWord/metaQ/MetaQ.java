package metaQ;

import java.util.HashSet;
import java.util.Set;

import tools.Pair;
import tools.Tools;

public class MetaQ extends NashQ
{

    @SuppressWarnings("unchecked")
    public void getAction()
    {
	int s1 = loc[0];
	int s2 = loc[1];
	int s3 = loc[2];
	
	// three agent's meta 平衡集
	Set<Pair<Integer, Integer, Integer>> temp = intersect(minminmax(
		Q1[s1][s2][s3], 1), minminmax(Q2[s1][s2][s3],
		2));
	Set<Pair<Integer, Integer, Integer>> sym = intersect(minminmax(
		Q3[s1][s2][s3], 3), temp);
	if (sym.size() == 0)
	{
	    Pair<Integer, Integer, Integer> MetaE = chooseBestE(s1, s2, s3);
	   
		action[0] = MetaE.v1;
	    
		action[1] = MetaE.v2;
	    
		action[2] = MetaE.v3;
	   
	}
	else
	{
	    // 找一个和最大的meta平衡

	    Pair<Integer, Integer, Integer> metaE = chooseBestE(sym, s1, s2, s3);

	    action[0] = metaE.v1;
	    action[1] = metaE.v2;
	    action[2] = metaE.v3;
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
 * find the element that both exist in Set s1 and Set s2
 * @param s1
 * @param s2
 * @return s1 union s2
 */
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

    /**
     * 从三个方向上找meta理性。常规上来说，对agent1 ，应该是从row上找，对agent2，从column上找，对agent3，从high上找
     * @param Q
     * @param direction 1--Row 	2--Column 3--High
     * @return  the meta equilibrium set
     */
    private  Set<Pair<Integer, Integer, Integer>> minminmax(
	    double[][][] Q, int direction)
    {
	Set<Pair<Integer, Integer, Integer>> set = new HashSet<Pair<Integer, Integer, Integer>>();
	int M = Q.length;
	int N = Q[0].length;
	int K = Q[0][0].length;

	double threshold = 0;
	double t1 = 0;
	double t2 = 0;
	if (direction == 1)
	{
	    t1 = findminminmaxRow_jki(Q);
	    t2 = findminminmaxRow_kji(Q);
	}
	else if (direction == 2)
	{
	    t1 = findminminmaxColumn_ikj(Q);
	    t2 = findminminmaxColumn_kij(Q);
	}
	else if (direction == 3)
	{
	    t1 = findminminmaxHigh_ijk(Q);
	    t2 = findminminmaxHigh_jik(Q);
	}
	
	if(Math.abs(t1-t2) < 0.00001)
	{
	    
	}
	else
	{
	    //System.out.println("minminmax is different to minminmax!");
	}
	
	if( t1 > t2 )
	    threshold = t2;
	else
	    threshold = t1;

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

    private double findminminmaxRow_kji(double[][][] Q)
    {
	int M = Q.length;
	int N = Q[0].length;
	int K = Q[0][0].length;

	int p = 0, q = 0, r = 0;
	
	/**
	 * search in M direction for max values
	 */
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

	/**
	 * search in N direction for minmax values
	 */
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

	/**
	 * search in K direction for minminmax values
	 */
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
    
    
    private double findminminmaxRow_jki(double[][][] Q)
    {
	int M = Q.length;
	int N = Q[0].length;
	int K = Q[0][0].length;

	
	int p = 0, q = 0, r = 0;
	
	/**
	 * search in M direction for max values
	 */
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

	/**
	 * search in K direction for minmax values
	 */
	temp = 0;
	int[] Ks = new int[N];
	for( int n = 0; n < N; n++ )
	{
	    for( int k = 0; k < K; k++ )
	    {
		if(Q[Ms[n][k]][n][k] < Q[Ms[n][k]][n][temp])
		{
		    temp = k;
		}
	    }
	    Ks[n] = temp;
	}


	/**
	 * search in N direction for minminmax values
	 */
	temp = 0;
	for( int n = 0; n < N; n++ )
	{
	    int k = Ks[n];
	    int m = Ms[n][k];
	    
	    if(Q[m][n][k] < Q[m][temp][k])
	    {
		temp = n;
	    }
	}
	q = temp;
	r = Ks[q];
	p = Ms[q][r];
	
	return Q[p][q][r];
    }

    private double findminminmaxColumn_ikj(double[][][] Q)
    {
	int M = Q.length;
	int N = Q[0].length;
	int K = Q[0][0].length;

	
	int p = 0, q = 0, r = 0;
	
	/**
	 * search in N direction for max values
	 */
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

	/**
	 * search in K direction for minmax values
	 */
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

	/**
	 * search in M direction for minminmax values
	 */
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

    private double findminminmaxColumn_kij(double[][][] Q)
    {
	int M = Q.length;
	int N = Q[0].length;
	int K = Q[0][0].length;

	
	int p = 0, q = 0, r = 0;
	
	/**
	 * search in N direction for max values
	 */
	int[][] Ns = new int[K][M];
	int temp = 0;
	for( int k = 0; k < K; k++ )
	{
	    for( int m = 0; m < M; m++ )
	    {
		for( int n = 0; n < N; n++ )
		{
		    if(Q[m][n][k] > Q[m][temp][k])
		    {
			temp = n;
		    }
		}
		Ns[k][m] = temp;
	    }
	}
	
	/**
	 * search in M direction for minmax values
	 */
	temp = 0;
	int[] Ms = new int[K];
	for( int k = 0; k < K; k++ )
	{
	    for( int m = 0; m < M; m++ )
	    {
		if(Q[m][Ns[k][m]][k] < Q[temp][Ns[k][m]][k])
		{
		    temp = m;
		}
	    }
	    Ms[k] = temp;
	}
	
	/**
	 * search in K direction for minminmax values
	 */
	temp = 0;
	for( int k = 0; k < K; k++ )
	{
	    int m = Ms[k];
	    int n = Ns[k][m];
	    if(Q[m][n][k] < Q[m][n][temp])
	    {
		temp = k;
	    }
	}
	r = temp;
	p = Ms[r];
	q = Ns[r][p];
	
	return Q[p][q][r];
    }
    
    private double findminminmaxHigh_ijk(double[][][] Q)
    {
	int M = Q.length;
	int N = Q[0].length;
	int K = Q[0][0].length;

	
	int p = 0, q = 0, r = 0;
	
	/**
	 * search in K direction for max values
	 */
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

	/**
	 * search in N direction for minmax values
	 */
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

	/**
	 * search in M direction for minminmax values
	 */
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

    private double findminminmaxHigh_jik(double[][][] Q)
    {
	int M = Q.length;
	int N = Q[0].length;
	int K = Q[0][0].length;

	
	int p = 0, q = 0, r = 0;
	
	/**
	 * search in K direction for max values
	 */
	int[][] Ks = new int[N][M];
	int temp = 0;
	for (int n = 0; n < N; n++)
	{
	    for (int m = 0; m < M; m++)
	    {
		for (int k = 0; k < K; k++)
		{
		    if (Q[m][n][k] > Q[m][n][temp])
		    {
			temp = k;
		    }
		}
		Ks[n][m] = temp;
	    }
	}

	/**
	 * search in M direction for minmax values
	 */
	temp = 0;
	int[] Ms = new int[N];
	for( int n = 0; n < N; n++ )
	{
	    for( int m = 0; m < M; m++ )
	    {
		if(Q[m][n][Ks[n][m]] < Q[temp][n][Ks[n][m]])
		{
		    temp = m;
		}
	    }
	    Ms[n] = temp;
	}


	/**
	 * search in N direction for minminmax values
	 */
	temp = 0;
	for( int n = 0; n < N; n++ )
	{
	    int m = Ms[n];
	    int k = Ks[n][m];
	    
	    if(Q[m][n][k] < Q[m][temp][k])
	    {
		temp = n;
	    }
	}
	q = temp;
	p = Ms[q];
	r = Ks[q][p];
	
	return Q[p][q][r];
    }
    
    /**
     * The following seven methods are for finding the actions
     * which can guarantee conservative expected values 
     * 
     * add by Huyujing
     */
    
    private Set<Pair<Integer, Integer, Integer>> maxminmin(double[][][] Q, 
	    int direction)
    {
	Set<Pair<Integer, Integer, Integer>> set = new HashSet<Pair<Integer, Integer, Integer>>();
	int M = Q.length;
	int N = Q[0].length;
	int K = Q[0][0].length;

	double threshold = 0;
	double t1 = 0;
	double t2 = 0;
	if (direction == 1)
	{
	    t1 = findmaxminminRow_ijk(Q);
	    t2 = findmaxminminRow_ikj(Q);
	}
	else if (direction == 2)
	{
	    t1 = findmaxminminColumn_jik(Q);
	    t2 = findmaxminminColumn_jki(Q);
	}
	else if (direction == 3)
	{
	    t1 = findmaxminminHigh_kij(Q);
	    t2 = findmaxminminHigh_kji(Q);
	}
	
	
	if( t1 > t2 )
	    threshold = t2;
	else
	    threshold = t1;

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
    
    private double findmaxminminRow_ijk(double[][][] Q)
    {
	int M = Q.length;
	int N = Q[0].length;
	int K = Q[0][0].length;

	/**
	 * for recording the indices of threshold
	 */
	int p = 0, q = 0, r = 0;
	
	/**
	 * search in K direction for the min values
	 */
	int[][] Ks = new int[M][N];
	int temp = 0;
	for (int m = 0; m < M; m++)
	{
	    for (int n = 0; n < N; n++)
	    {
		for (int k = 0; k < K; k++)
		{
		    if (Q[m][n][k] < Q[m][n][temp])
		    {
			temp = k;
		    }
		}
		Ks[m][n] = temp;
	    }
	}
	
	/**
	 * search in N direction for the minmin values
	 */
	temp = 0;
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
	
	/**
	 * search in M direction for the maxminmin values
	 */
	temp = 0;
	for (int m = 0; m < M; m++)
	{
	    int n = Ns[m];
	    int k = Ks[m][n];
	    if (Q[m][n][k] > Q[temp][n][k])
	    {
		temp = m;
	    }
	}
	p = temp;
	q = Ns[p];
	r = Ks[p][q];
	
	return Q[p][q][r];

    }
    
    private double findmaxminminRow_ikj(double[][][] Q)
    {
	int M = Q.length;
	int N = Q[0].length;
	int K = Q[0][0].length;

	/**
	 * for recording the indices of threshold
	 */
	int p = 0, q = 0, r = 0;
	
	/**
	 * search in N direction for the min values
	 */
	int[][] Ns = new int[M][K];
	int temp = 0;
	for (int m = 0; m < M; m++ )
	{
	    for (int k = 0; k < K; k++ )
	    {
		for (int n = 0; n < N; n++ )
		{
		    if (Q[m][n][k] < Q[m][temp][k])
		    {
			temp = n;
		    }
		}
		Ns[m][k] = temp; 
	    }
	}
	
	/**
	 * search in K direction for the minmin values
	 */
	temp = 0;
	int[] Ks = new int[M];
	for( int m = 0; m < M; m++ )
	{
	    for( int k = 0; k < K; k++ )
	    {
		if( Q[m][Ns[m][k]][k] < Q[m][Ns[m][k]][temp] )
		{
		    temp = k;
		}
	    }
	    Ks[m] = temp; 
	}
	
	/**
	 * search in M direction for the maxminmin values
	 */
	temp = 0;
	for (int m = 0; m < M; m++)
	{
	    int k = Ks[m];
	    int n = Ns[m][k];
	    if (Q[m][n][k] > Q[temp][n][k])
	    {
		temp = m;
	    }
	}
	p = temp;
	r = Ks[p];
	q = Ns[p][r];
	
	return Q[p][q][r];
    }
    
    private double findmaxminminColumn_jki(double[][][] Q)
    {
	int M = Q.length;
	int N = Q[0].length;
	int K = Q[0][0].length;

	
	int p = 0, q = 0, r = 0;
	
	/**
	 * search in M direction for min values
	 */
	int[][] Ms = new int[N][K];
	int temp = 0;
	for (int n = 0; n < N; n++)
	{
	    for (int k = 0; k < K; k++)
	    {
		for (int m = 0; m < M; m++)
		{
		    if (Q[m][n][k] < Q[temp][n][k])
		    {
			temp = m;
		    }
		}
		Ms[n][k] = temp;
	    }
	}

	/**
	 * search in K direction for minmin values
	 */
	temp = 0;
	int[] Ks = new int[N];
	for( int n = 0; n < N; n++ )
	{
	    for( int k = 0; k < K; k++ )
	    {
		if(Q[Ms[n][k]][n][k] < Q[Ms[n][k]][n][temp])
		{
		    temp = k;
		}
	    }
	    Ks[n] = temp;
	}


	/**
	 * search in N direction for maxminmin values
	 */
	temp = 0;
	for( int n = 0; n < N; n++ )
	{
	    int k = Ks[n];
	    int m = Ms[n][k];
	    
	    if(Q[m][n][k] > Q[m][temp][k])
	    {
		temp = n;
	    }
	}
	q = temp;
	r = Ks[q];
	p = Ms[q][r];
	
	return Q[p][q][r];
    }
 
    private double findmaxminminColumn_jik(double[][][] Q)
    {
	int M = Q.length;
	int N = Q[0].length;
	int K = Q[0][0].length;

	
	int p = 0, q = 0, r = 0;
	
	/**
	 * search in K direction for min values
	 */
	int[][] Ks = new int[N][M];
	int temp = 0;
	for (int n = 0; n < N; n++)
	{
	    for (int m = 0; m < M; m++)
	    {
		for (int k = 0; k < K; k++)
		{
		    if (Q[m][n][k] < Q[m][n][temp])
		    {
			temp = k;
		    }
		}
		Ks[n][m] = temp;
	    }
	}

	/**
	 * search in M direction for minmin values
	 */
	temp = 0;
	int[] Ms = new int[N];
	for( int n = 0; n < N; n++ )
	{
	    for( int m = 0; m < M; m++ )
	    {
		if(Q[m][n][Ks[n][m]] < Q[temp][n][Ks[n][m]])
		{
		    temp = m;
		}
	    }
	    Ms[n] = temp;
	}


	/**
	 * search in N direction for maxminmin values
	 */
	temp = 0;
	for( int n = 0; n < N; n++ )
	{
	    int m = Ms[n];
	    int k = Ks[n][m];
	    
	    if(Q[m][n][k] > Q[m][temp][k])
	    {
		temp = n;
	    }
	}
	q = temp;
	p = Ms[q];
	r = Ks[q][p];
	
	return Q[p][q][r];
    }   
 
    private double findmaxminminHigh_kij(double[][][] Q)
    {
	int M = Q.length;
	int N = Q[0].length;
	int K = Q[0][0].length;

	
	int p = 0, q = 0, r = 0;
	
	/**
	 * search in N direction for min values
	 */
	int[][] Ns = new int[K][M];
	int temp = 0;
	for( int k = 0; k < K; k++ )
	{
	    for( int m = 0; m < M; m++ )
	    {
		for( int n = 0; n < N; n++ )
		{
		    if(Q[m][n][k] < Q[m][temp][k])
		    {
			temp = n;
		    }
		}
		Ns[k][m] = temp;
	    }
	}
	
	/**
	 * search in M direction for minmin values
	 */
	temp = 0;
	int[] Ms = new int[K];
	for( int k = 0; k < K; k++ )
	{
	    for( int m = 0; m < M; m++ )
	    {
		if(Q[m][Ns[k][m]][k] < Q[temp][Ns[k][m]][k])
		{
		    temp = m;
		}
	    }
	    Ms[k] = temp;
	}
	
	/**
	 * search in K direction for maxminmin values
	 */
	temp = 0;
	for( int k = 0; k < K; k++ )
	{
	    int m = Ms[k];
	    int n = Ns[k][m];
	    if(Q[m][n][k] > Q[m][n][temp])
	    {
		temp = k;
	    }
	}
	r = temp;
	p = Ms[r];
	q = Ns[r][p];
	
	return Q[p][q][r];
    }
    
    private double findmaxminminHigh_kji(double[][][] Q)
    {
	int M = Q.length;
	int N = Q[0].length;
	int K = Q[0][0].length;

	
	int p = 0, q = 0, r = 0;
	
	/**
	 * search in M direction for min values
	 */
	int[][] Ms = new int[K][N];
	int temp = 0;
	for( int k = 0; k < K; k++ )
	{
	    for( int n = 0; n < N; n++ )
	    {
		for( int m = 0; m < M; m++ )
		{
		    if(Q[m][n][k] < Q[temp][n][k])
		    {
			temp = m;
		    }
		}
		Ms[k][n] = temp; 
	    }
	}
	
	/**
	 * search in N direction for minmin values
	 */
	temp = 0;
	int[] Ns = new int[K];
	for( int k = 0; k < K; k++ )
	{
	    for( int n = 0; n < N; n++ )
	    {
		if(Q[Ms[k][n]][n][k] < Q[Ms[k][n]][temp][k])
		{
		    temp = n;
		}
	    }
	    Ns[k] = temp; 
	}
	
	/**
	 * search in K direction for maxminmin values
	 */
	temp = 0;
	for( int k = 0; k < K; k++ )
	{
	    int n = Ns[k];
	    int m = Ms[k][n];
	    if(Q[m][n][k] > Q[m][n][temp])
	    {
		temp = k;
	    }
	}
	r = temp;
	q = Ns[r];
	p = Ms[r][q];
	
	return Q[p][q][r];
    }
    
    
    /**
     * The following seven methods are for finding the actions
     * which are the second optimal for the agent.
     * That means, find the minmaxmin actions
     * 
     * add by Huyujing
     */
    private Set<Pair<Integer, Integer, Integer>> minmaxmin(double[][][] Q, int direction)
    {
	Set<Pair<Integer, Integer, Integer>> set = new HashSet<Pair<Integer, Integer, Integer>>();
	int M = Q.length;
	int N = Q[0].length;
	int K = Q[0][0].length;

	double threshold = 0;
	double t1 = 0;
	double t2 = 0;
	if (direction == 1)
	{
	    t1 = findminmaxminRow_jik(Q);
	    t2 = findminmaxminRow_kij(Q);
	}
	else if (direction == 2)
	{
	    t1 = findminmaxminColumn_ijk(Q);
	    t2 = findminmaxminColumn_kji(Q);
	}
	else if (direction == 3)
	{
	    t1 = findminmaxminHigh_ikj(Q);
	    t2 = findminmaxminHigh_jki(Q);
	}
	
	
	if( t1 > t2 )
	    threshold = t2;
	else
	    threshold = t1;

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
    
    
    private double findminmaxminRow_jik(double[][][] Q)
    {
	int M = Q.length;
	int N = Q[0].length;
	int K = Q[0][0].length;

	
	int p = 0, q = 0, r = 0;
	
	/**
	 * search in K direction for min values
	 */
	int[][] Ks = new int[N][M];
	int temp = 0;
	for (int n = 0; n < N; n++)
	{
	    for (int m = 0; m < M; m++)
	    {
		for (int k = 0; k < K; k++)
		{
		    if (Q[m][n][k] < Q[m][n][temp])
		    {
			temp = k;
		    }
		}
		Ks[n][m] = temp;
	    }
	}

	/**
	 * search in M direction for maxmin values
	 */
	temp = 0;
	int[] Ms = new int[N];
	for( int n = 0; n < N; n++ )
	{
	    for( int m = 0; m < M; m++ )
	    {
		if(Q[m][n][Ks[n][m]] > Q[temp][n][Ks[n][m]])
		{
		    temp = m;
		}
	    }
	    Ms[n] = temp;
	}


	/**
	 * search in N direction for minmaxmin values
	 */
	temp = 0;
	for( int n = 0; n < N; n++ )
	{
	    int m = Ms[n];
	    int k = Ks[n][m];
	    
	    if(Q[m][n][k] < Q[m][temp][k])
	    {
		temp = n;
	    }
	}
	q = temp;
	p = Ms[q];
	r = Ks[q][p];
	
	return Q[p][q][r];
    }
    
    
    private double findminmaxminRow_kij(double[][][] Q)
    {
	int M = Q.length;
	int N = Q[0].length;
	int K = Q[0][0].length;

	
	int p = 0, q = 0, r = 0;
	
	/**
	 * search in N direction for min values
	 */
	int[][] Ns = new int[K][M];
	int temp = 0;
	for( int k = 0; k < K; k++ )
	{
	    for( int m = 0; m < M; m++ )
	    {
		for( int n = 0; n < N; n++ )
		{
		    if(Q[m][n][k] < Q[m][temp][k])
		    {
			temp = n;
		    }
		}
		Ns[k][m] = temp;
	    }
	}
	
	/**
	 * search in M direction for maxmin values
	 */
	temp = 0;
	int[] Ms = new int[K];
	for( int k = 0; k < K; k++ )
	{
	    for( int m = 0; m < M; m++ )
	    {
		if(Q[m][Ns[k][m]][k] > Q[temp][Ns[k][m]][k])
		{
		    temp = m;
		}
	    }
	    Ms[k] = temp;
	}
	
	/**
	 * search in K direction for minmaxmin values
	 */
	temp = 0;
	for( int k = 0; k < K; k++ )
	{
	    int m = Ms[k];
	    int n = Ns[k][m];
	    if(Q[m][n][k] < Q[m][n][temp])
	    {
		temp = k;
	    }
	}
	r = temp;
	p = Ms[r];
	q = Ns[r][p];
	
	return Q[p][q][r];
    }
    
    
    private double findminmaxminColumn_ijk(double[][][] Q)
    {
	int M = Q.length;
	int N = Q[0].length;
	int K = Q[0][0].length;

	/**
	 * for recording the indices of threshold
	 */
	int p = 0, q = 0, r = 0;
	
	/**
	 * search in K direction for the min values
	 */
	int[][] Ks = new int[M][N];
	int temp = 0;
	for (int m = 0; m < M; m++)
	{
	    for (int n = 0; n < N; n++)
	    {
		for (int k = 0; k < K; k++)
		{
		    if (Q[m][n][k] < Q[m][n][temp])
		    {
			temp = k;
		    }
		}
		Ks[m][n] = temp;
	    }
	}
	
	/**
	 * search in N direction for the maxmin values
	 */
	temp = 0;
	int[] Ns = new int[M];
	for (int m = 0; m < M; m++)
	{
	    for (int n = 0; n < N; n++)
	    {
		if (Q[m][n][Ks[m][n]] > Q[m][temp][Ks[m][n]])
		{
		    temp = n;
		}
	    }
	    Ns[m] = temp;
	}
	
	/**
	 * search in M direction for the minmaxmin values
	 */
	temp = 0;
	for (int m = 0; m < M; m++)
	{
	    int n = Ns[m];
	    int k = Ks[m][n];
	    if (Q[m][n][k] < Q[temp][n][k])
	    {
		temp = m;
	    }
	}
	p = temp;
	q = Ns[p];
	r = Ks[p][q];
	
	return Q[p][q][r];
    }
    
    
    private double findminmaxminColumn_kji(double[][][] Q)
    {
	int M = Q.length;
	int N = Q[0].length;
	int K = Q[0][0].length;

	
	int p = 0, q = 0, r = 0;
	
	/**
	 * search in M direction for min values
	 */
	int[][] Ms = new int[K][N];
	int temp = 0;
	for( int k = 0; k < K; k++ )
	{
	    for( int n = 0; n < N; n++ )
	    {
		for( int m = 0; m < M; m++ )
		{
		    if(Q[m][n][k] < Q[temp][n][k])
		    {
			temp = m;
		    }
		}
		Ms[k][n] = temp; 
	    }
	}
	
	/**
	 * search in N direction for maxmin values
	 */
	temp = 0;
	int[] Ns = new int[K];
	for( int k = 0; k < K; k++ )
	{
	    for( int n = 0; n < N; n++ )
	    {
		if(Q[Ms[k][n]][n][k] > Q[Ms[k][n]][temp][k])
		{
		    temp = n;
		}
	    }
	    Ns[k] = temp; 
	}
	
	/**
	 * search in K direction for minmaxmin values
	 */
	temp = 0;
	for( int k = 0; k < K; k++ )
	{
	    int n = Ns[k];
	    int m = Ms[k][n];
	    if(Q[m][n][k] < Q[m][n][temp])
	    {
		temp = k;
	    }
	}
	r = temp;
	q = Ns[r];
	p = Ms[r][q];
	
	return Q[p][q][r];
    }
    
    private double findminmaxminHigh_ikj(double[][][] Q)
    {
	int M = Q.length;
	int N = Q[0].length;
	int K = Q[0][0].length;

	/**
	 * for recording the indices of threshold
	 */
	int p = 0, q = 0, r = 0;
	
	/**
	 * search in N direction for the min values
	 */
	int[][] Ns = new int[M][K];
	int temp = 0;
	for (int m = 0; m < M; m++ )
	{
	    for (int k = 0; k < K; k++ )
	    {
		for (int n = 0; n < N; n++ )
		{
		    if (Q[m][n][k] < Q[m][temp][k])
		    {
			temp = n;
		    }
		}
		Ns[m][k] = temp; 
	    }
	}
	
	/**
	 * search in K direction for the maxmin values
	 */
	temp = 0;
	int[] Ks = new int[M];
	for( int m = 0; m < M; m++ )
	{
	    for( int k = 0; k < K; k++ )
	    {
		if( Q[m][Ns[m][k]][k] > Q[m][Ns[m][k]][temp] )
		{
		    temp = k;
		}
	    }
	    Ks[m] = temp; 
	}
	
	/**
	 * search in M direction for the minmaxmin values
	 */
	temp = 0;
	for (int m = 0; m < M; m++)
	{
	    int k = Ks[m];
	    int n = Ns[m][k];
	    if (Q[m][n][k] < Q[temp][n][k])
	    {
		temp = m;
	    }
	}
	p = temp;
	r = Ks[p];
	q = Ns[p][r];
	
	return Q[p][q][r];
    }
    
    
    private double findminmaxminHigh_jki(double[][][] Q)
    {
	int M = Q.length;
	int N = Q[0].length;
	int K = Q[0][0].length;

	
	int p = 0, q = 0, r = 0;
	
	/**
	 * search in M direction for min values
	 */
	int[][] Ms = new int[N][K];
	int temp = 0;
	for (int n = 0; n < N; n++)
	{
	    for (int k = 0; k < K; k++)
	    {
		for (int m = 0; m < M; m++)
		{
		    if (Q[m][n][k] < Q[temp][n][k])
		    {
			temp = m;
		    }
		}
		Ms[n][k] = temp;
	    }
	}

	/**
	 * search in K direction for maxmin values
	 */
	temp = 0;
	int[] Ks = new int[N];
	for( int n = 0; n < N; n++ )
	{
	    for( int k = 0; k < K; k++ )
	    {
		if(Q[Ms[n][k]][n][k] > Q[Ms[n][k]][n][temp])
		{
		    temp = k;
		}
	    }
	    Ks[n] = temp;
	}


	/**
	 * search in N direction for minmaxmin values
	 */
	temp = 0;
	for( int n = 0; n < N; n++ )
	{
	    int k = Ks[n];
	    int m = Ms[n][k];
	    
	    if(Q[m][n][k] < Q[m][temp][k])
	    {
		temp = n;
	    }
	}
	q = temp;
	r = Ks[q];
	p = Ms[q][r];
	
	return Q[p][q][r];
    }
    
    
    
    public double getEquilibrium(int agent, int s1, int s2, int s3)
    {
	return getMetaQ(agent, s1, s2, s3);
    }

    @SuppressWarnings("unchecked")
    private double getMetaQ(int agent, int s1, int s2, int s3)
    {
	int a1 = random.nextInt(4);
	int a2 = random.nextInt(4);
	int a3 = random.nextInt(4);
	
	Set<Pair<Integer, Integer, Integer>> finalSet = null;
	
	/**
	 * get the set of symmetric meta equilibria
	 */
	Set<Pair<Integer, Integer, Integer>> symP1 =  minminmax(Q1[s1][s2][s3], 1);
	Set<Pair<Integer, Integer, Integer>> symP2 =  minminmax(Q2[s1][s2][s3], 2);
	Set<Pair<Integer, Integer, Integer>> symP3 =  minminmax(Q3[s1][s2][s3], 3);
	Set<Pair<Integer, Integer, Integer>> temp = intersect(symP1, symP2);
	
	Set<Pair<Integer, Integer, Integer>> symSet = intersect(symP3, temp);
	
	if (symSet.size() == 0)
	{
	    /**
	     * there is no symmetric meta equilibrium for many times
	     * we have to get the set of general meta equilibria
	     */
	    
	    Set<Pair<Integer, Integer, Integer>> mP1 = maxminmin(Q1[s1][s2][s3], 1);
	    Set<Pair<Integer, Integer, Integer>> mP2 = maxminmin(Q2[s1][s2][s3], 2);
	    Set<Pair<Integer, Integer, Integer>> mP3 = maxminmin(Q3[s1][s2][s3], 3);
	    
	    Set<Pair<Integer, Integer, Integer>> interSet12 = intersect(mP1, mP2);
	    Set<Pair<Integer, Integer, Integer>> interSet13 = intersect(mP1, mP3);
	    Set<Pair<Integer, Integer, Integer>> interSet23 = intersect(mP2, mP3);
	    Set<Pair<Integer, Integer, Integer>> interSet123 = intersect(mP3, interSet12);
	    
	    
	    if( interSet123.size() == 0)
	    {
		if( interSet12.size() == 0 && interSet13.size() == 0 && interSet23.size() == 0)
		{
		    System.out.println("All intersection empty!");
		}
		
		//System.out.println("No general equilibria");
	    }
	    else
	    {
		finalSet = interSet123;
	    }
	}
	else
	{  
	    finalSet = symSet;
	}
	
	if(finalSet != null && finalSet.size() > 0)
	{
	    Pair<Integer, Integer, Integer> metaE = chooseBestE(finalSet, s1, s2, s3);	    
	    a1 = metaE.v1;  
	    a2 = metaE.v2;
	    a3 = metaE.v3;
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
    
    
    public int[] getEqJointAction(int s1, int s2, int s3)
    {
	int[] retActions = new int[3];
	
	
	Set<Pair<Integer, Integer, Integer>> finalSet = null;
	
	/**
	 * get the set of symmetric meta equilibria
	 */
	Set<Pair<Integer, Integer, Integer>> symP1 =  minminmax(Q1[s1][s2][s3], 1);
	Set<Pair<Integer, Integer, Integer>> symP2 =  minminmax(Q2[s1][s2][s3], 2);
	Set<Pair<Integer, Integer, Integer>> symP3 =  minminmax(Q3[s1][s2][s3], 3);
	Set<Pair<Integer, Integer, Integer>> temp = intersect(symP1, symP2);
	
	Set<Pair<Integer, Integer, Integer>> symSet = intersect(symP3, temp);
	
	if (symSet.size() == 0)
	{
	    /**
	     * there is no symmetric meta equilibrium for many times
	     * we have to get the set of general meta equilibria
	     */
	    
	    Set<Pair<Integer, Integer, Integer>> mP1 = maxminmin(Q1[s1][s2][s3], 1);
	    Set<Pair<Integer, Integer, Integer>> mP2 = maxminmin(Q2[s1][s2][s3], 2);
	    Set<Pair<Integer, Integer, Integer>> mP3 = maxminmin(Q3[s1][s2][s3], 3);
	    
	    Set<Pair<Integer, Integer, Integer>> interSet12 = intersect(mP1, mP2);
	    Set<Pair<Integer, Integer, Integer>> interSet13 = intersect(mP1, mP3);
	    Set<Pair<Integer, Integer, Integer>> interSet23 = intersect(mP2, mP3);
	    Set<Pair<Integer, Integer, Integer>> interSet123 = intersect(mP3, interSet12);
	    
	    
	    if( interSet123.size() == 0)
	    {
		if( interSet12.size() == 0 && interSet13.size() == 0 && interSet23.size() == 0)
		{
		    System.out.println("All intersection empty!");
		}
		
		//System.out.println("No general equilibria");
	    }
	    else
	    {
		finalSet = interSet123;
	    }
	}
	else
	{  
	    finalSet = symSet;
	}
	
	if(finalSet != null && finalSet.size() > 0)
	{
	    Pair<Integer, Integer, Integer> metaE = chooseBestE(finalSet, s1, s2, s3);	    
	    retActions[0] = metaE.v1;  
	    retActions[1] = metaE.v2;
	    retActions[2] = metaE.v3;
	}
	else
	{
	    Pair<Integer, Integer, Integer> jointAction = chooseBestE(s1, s2, s3);
	    retActions[0] = jointAction.v1;
	    retActions[1] = jointAction.v2;
	    retActions[2] = jointAction.v3;
	}
	
	return retActions;
    }
    

    public void selfIntro()
    {
	System.out.println("#####GridGame 1 Using MetaQ#####");
    }
    

    /**
     * 
     * 最后得到的结果
     */
    @SuppressWarnings("unchecked")
    public int[] getEquilibriumAction(int s1, int s2, int s3)
    {
	int[] ret = new int[3];
	int a1 = random.nextInt(4), a2 = random.nextInt(4), a3 = random
		.nextInt(4);
	Set<Pair<Integer, Integer, Integer>> temp = intersect(minminmax(
		Q1[s1][s2][s3], 1), minminmax(Q2[s1][s2][s3],
		2));
	Set<Pair<Integer, Integer, Integer>> sym = intersect(minminmax(
		Q3[s1][s2][s3], 3), temp);
	if (sym.size() == 0)
	{
	    
	}
	else
	{
	    Pair<Integer, Integer, Integer> metaE = chooseBestE(sym, s1, s2, s3);
	    a1 = metaE.v1;
	    a2 = metaE.v2;
	    a3 = metaE.v3;
	}
	ret[0] = a1;
	ret[1] = a2;
	ret[2] = a3;
	return ret;
    }
    

    public static void main(String args[])
    {
	GridGame gg = new MetaQ();
	Tools.timerBegin();
	gg.train(0.1, 0.9);
	Tools.timerStop();
	//gg.testTrainingResult();
    }

}
