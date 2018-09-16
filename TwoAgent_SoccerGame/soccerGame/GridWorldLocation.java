package soccerGame;

public class GridWorldLocation
{

    public static final int INVALID_LOCATION_ID = -1;
    
    private int locationRow;
    private int locationCol;
    
    public static final int GOAL_ROW_1 = 1;
    public static final int GOAL_ROW_2 = 2;
    public static final int GOAL_COL_A = 5;
    public static final int GOAL_COL_B = -1;
    
    public static final int GOAL_A_1 = 110;
    public static final int GOAL_A_2 = 115;
    public static final int GOAL_B_1 = 104;
    public static final int GOAL_B_2 = 109;
    
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
	    locationRow < SoccerGame.WORLD_HEIGHT && 
	    	locationCol < SoccerGame.WORLD_WIDTH ) {
	    
	    return locationRow * SoccerGame.WORLD_WIDTH + locationCol;
	}
	/**
	 * if the location is a goal line
	 */
	else if( isGoal() ) {
	    
	    return 100 + locationRow * SoccerGame.WORLD_WIDTH + locationCol;
	}
	else {
	    
	    //System.out.println( "@GridWorldLocation->getLocationID: This Location is Invalid!" );
	    //System.out.println(locationRow+" "+locationCol);
	    
	    return INVALID_LOCATION_ID;
	}
    }
    
    private boolean isGoal()
    {
	if( (locationRow == GOAL_ROW_1 && locationCol == GOAL_COL_A) ||
		(locationRow == GOAL_ROW_2 && locationCol == GOAL_COL_A) || 
		(locationRow == GOAL_ROW_1 && locationCol == GOAL_COL_B) ||
		(locationRow == GOAL_ROW_2 && locationCol == GOAL_COL_B) )
	    return true;
	else
	    return false;
    }
    

}
