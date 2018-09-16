package gamePursuitGame;

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
    
    public int getLocationID()
    {
	/**
	 * only valid location has an ID
	 */
	if( locationRow >= 0 && locationCol >= 0 && 
	    locationRow < PursuitGame.WORLD_HEIGHT && 
	    	locationCol < PursuitGame.WORLD_WIDTH ) {
	    
	    return locationRow * PursuitGame.WORLD_WIDTH + locationCol;
	}
	else {
	    
	    System.out.println( "@GridWorldLocation->getLocationID: This Location is Invalid!" );
	    return INVALID_LOCATION_ID;
	}
    }
    
    public int getDistance( GridWorldLocation gwLoc )
    {
	if( gwLoc == null ) {
	    
	    return 0;
	}
	
	int disX = Math.abs(locationCol - gwLoc.getCol() );
	int disY = Math.abs( locationRow - gwLoc.getRow() );
	
	return (disX+disY);
	
    }
}
