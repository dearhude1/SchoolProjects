package help;


/**
 * the support of a mixed strategy action
 * for computing a Nash equilibrium
 * 
 * The definition of a support can be 
 * found in paper "Simple search methods for finding a Nash equilibrium"
 */
public class Support
{
	private boolean[] actionSupport;
	private boolean valid;
	private int size = 0;
	
	private int actionNum = 0;
	
	public Support( boolean[] supp, int actNum )
	{
		actionNum = actNum;
		
	    if( supp == null || supp.length != actionNum ) {
		
	    	System.out.println("@Support->constructor: Wrong Array!");
		
	    	actionSupport = new boolean[actionNum];
	    	for( int action = 0; action < actionNum; action++ )
	    		actionSupport[action] = false;
		
	    	valid = false;
	    }
	    else {
		
		
	    	actionSupport = new boolean[actionNum];
	    	for( int action = 0; action < actionNum; action++ ) {
		   
	    		actionSupport[action] = supp[action];
	    		if( actionSupport[action] )
	    			size++;
	    	}
	    	if( size == 0 ) {
		    
	    		System.out.println("@Support->constructor: No Support!");
	    		valid = false;
	    	}
	    	else
	    		valid = true;
	    }
	}
	
	public Support( int[] b, int actNum )
	{
		actionNum = actNum;
		
	    if( b == null || b.length != actionNum ) {
		
	    	System.out.println("@Support->constructor: Wrong Array!");
		
	    	actionSupport = new boolean[actionNum];
	    	for( int action = 0; action < actionNum; action++ )
	    		actionSupport[action] = false;
		
	    	valid = false;
	    }
	    else {
		
	    	actionSupport = new boolean[actionNum];
	    	for( int action = 0; action < actionNum; action++ ) {
		    
	    		if( b[action] == 0 )
	    			actionSupport[action] = false;
	    		else if( b[action] == 1 ) {
			
	    			actionSupport[action] = true;
	    			size++;
	    		}
	    		else {
			
	    			actionSupport[action] = false;
	    			valid = false;
	    			size = 0;
	    			break;
	    		}
		}
		
		if( size == 0 ) {
		    
		    System.out.println("@Support->constructor: No Support!");
		    valid = false;
		}
		else
		    valid = true;
	    }
	}
	
	public boolean isValid() { return valid; }
	
	public int supportSize() { return size; }
	
	public boolean equals( Object obj )
	{
	    if( obj == null )
	    	return false;
	    else if( !(obj instanceof Support) )
	    	return false;
	    else {
		
	    	Support supp = (Support) obj;
	    	if( supp.supportSize() != size )
	    		return false;
		
	    	for( int action = 0; action < actionNum; action++ ) {
		    
	    		if( actionSupport[action] != supp.supported(action) )
	    			return false;
	    	}
		
	    	return true;
	    }
	}
	
	
	public boolean supported( int action )
	{
	    if( action < 0 || action >= actionNum ) {
		
	    	System.out.println("@Support->supported: Wrong Parameter!");
	    	return false;
	    }
	    else 
	    	return actionSupport[action];
	}
	
	
	public void display()
	{
		for( int act = 0; act < actionNum; act++ ) {
			
			if( actionSupport[act] )
				System.out.print("1 ");
			else
				System.out.print("0 ");
		}
		System.out.println();
	}
}