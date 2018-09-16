package multiGridWorld;

public class GridWorldLocation
{

    public static final int INVALID_LOCATION_ID = -1;
    
    private int locationRow;
    private int locationCol;
    
    public GridWorldLocation()
    {
	locationRow = 0;
	locationCol = 0;
    }
    
    public GridWorldLocation( int row, int col )
    {
	locationRow = row;
	locationCol = col;
    }
    
    
    public int getRow() { return locationRow; }
    
    public int getCol() { return locationCol; }
    
    public void setRow( int row )
    {
	locationRow = row;
    }
    
    public void setCol( int col )
    {
	locationCol = col;
    }
    
    public void setLocation( int locID )
    {
	if( locID < 0 || locID >= MultiGridWorld.NUM_LOCATIONS ) {
	    
	    return;
	}
	
	locationRow = locID / MultiGridWorld.WORLD_WIDTH;
	locationCol = locID - MultiGridWorld.WORLD_WIDTH * locationRow;
    }
    
    public int getLocationID()
    {
	/**
	 * only valid location has an ID
	 */
	if( locationRow >= 0 && locationCol >= 0 && 
	    locationRow < MultiGridWorld.WORLD_HEIGHT && 
	    	locationCol < MultiGridWorld.WORLD_WIDTH ) {
	    
	    return locationRow * MultiGridWorld.WORLD_WIDTH + locationCol;
	}
	else {
	    
	    System.out.println( "@GridWorldLocation->getLocationID: This Location is Invalid!" );
	    return INVALID_LOCATION_ID;
	}
    }
}
