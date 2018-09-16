package help;

import gameGridWorld.GameAction;
import gameGridWorld.SparseGridWorld;

/**
 * the class of support size vector
 * for computing a Nash equilibrium
 * 
 * more information of support size vector (usually denoted by x)
 * can be found in paper "Simple search methods for finding a Nash equilibrium"
 */
public class XVector implements Comparable<XVector>
{
	private int[] x;
	
	private boolean available = true;
	
	private int agentNum = SparseGridWorld.NUM_AGENTS;
	
	public XVector( int[] y )
	{
	    
	    if( y == null || y.length != SparseGridWorld.NUM_AGENTS ) {
		
		System.out.println("@XVector->constructor: Wrong Parameter!");
		
		x = new int[SparseGridWorld.NUM_AGENTS];
		for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		    
		    x[agent] = 1;
		}
	    }
	    else {
		
		x = new int[SparseGridWorld.NUM_AGENTS];
		for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		    
		    if( y[agent] < 1 || y[agent] > GameAction.NUM_ACTIONS ) {
			
			//System.out.println("@XVector->constructor: Wrong Parameter!");
			y[agent] = 1;
			
			available = false;
		    }
		    x[agent] = y[agent];
		}
	    }
	}
	
	public XVector( int[] y, int agNum )
	{
	    if( agNum < 2 ) {
	    
		System.out.println("@XVector->constructor: Wrong Agent Number!");
		
		agentNum = SparseGridWorld.NUM_AGENTS;
		x = new int[SparseGridWorld.NUM_AGENTS];
		for( int agent = 0; agent < SparseGridWorld.NUM_AGENTS; agent++ ) {
		    
		    x[agent] = 1;
		}
	    }
	    else if( y == null || y.length != agNum ) {
		
		System.out.println("@XVector->constructor: Wrong Parameter!");
		
		agentNum = agNum;
		x = new int[agentNum];
		for( int agent = 0; agent < agentNum; agent++ ) {
		    
		    x[agent] = 1;
		}
	    }
	    else {
		
		agentNum = agNum;
		x = new int[agentNum];
		for( int agent = 0; agent < agentNum; agent++ ) {
		    
		    if( y[agent] < 1 || y[agent] > GameAction.NUM_ACTIONS ) {
			
			//System.out.println("@XVector->constructor: Wrong Parameter!");
			y[agent] = 1;
			
			available = false;
		    }
		    x[agent] = y[agent];
		}
	    }
	}
	
	public boolean isAvailable()
	{
	    return available;
	}
	
	public boolean equals( Object object ) 
	{
	    
	    if( object == null )
		return false;
	    else if( !(object instanceof XVector) )
		return false;
	    else {
		
		XVector xv = (XVector) object;
		
		if( agentNum != xv.getAgentNum() ) {
		    
		    return false;
		}
		
		for( int agent = 0; agent < agentNum; agent++ ) {
		    
		    if( x[agent] != xv.getX(agent) )
			return false;
		}
		return true;
	    }
	}
	
	public int compareTo( XVector xVector )
	{
	    if( xVector == null )
		return 1;
	    
	    int mDff = maxDiff();
	    int mDffp = xVector.maxDiff();
	    
	    if( mDff > mDffp )
		return 1;
	    else if( mDff == mDffp )
		return 0;
	    else
		return -1;
	}
	
	public int getX( int agent )
	{
	    if( agent < 0 || agent >= agentNum ) {
		
		System.out.println("@XVector->getX: Wrong Parameter!");
		return 0;
	    }
	    
	    return x[agent];
	}
	
	public int getAgentNum()
	{
	    return agentNum;
	}
	
	public int sum()
	{
	    int sum = 0;
	    
	    for( int agent = 0; agent < agentNum; agent++ )
		sum += x[agent];
	    
	    return sum;
	}
	
	public int maxDiff()
	{
	    /**
	     * find the max
	     */
	    int max = x[0];
	    for( int agent = 1; agent < agentNum; agent++ ) {
		
		if( max < x[agent]) {
		    
		    max = x[agent];
		}
	    }
	    
	    /**
	     * find the min
	     */
	    int min = x[0];
	    for( int agent = 1; agent < agentNum; agent++ ) {
		
		if( min > x[agent] ) {
		    
		    min = x[agent];
		}
	    }
	    
	    return ( max - min );
	}
};