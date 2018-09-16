package wallGame;

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
	    locationRow < WallGame.WORLD_HEIGHT && 
	    	locationCol < WallGame.WORLD_WIDTH ) {
	    
	    return locationRow * WallGame.WORLD_WIDTH + locationCol;
	}
	else {
	    
	    System.out.println( "@GridWorldLocation->getLocationID: This Location is Invalid!" );
	    System.out.println(locationRow+" "+locationCol);
	    
	    return INVALID_LOCATION_ID;
	}
    }
}
