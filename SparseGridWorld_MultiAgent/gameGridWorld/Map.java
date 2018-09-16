package gameGridWorld;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class Map
{


    /**
     * Wall cells are also considered 
     * for map width and map height
     */
    private int mapWidth = 0;
    private int mapHeight = 0;
    
    private int[][] mapCells = null;
    private int numCells = 0;
    
    /**
     * map information
     */
    private int numAgents = 0;
    private int[] initCellIndices = null;
    private int[] goalCellIndices = null;
    

    /**
     * the indices of cells which are not wall
     */
    private int[] validCellIndices = null;
    private int validCellNum = 0;
    
    public Map( String mapFileName, String mapInfoFileName )
    {
	/**
	 * load the map and corresponding information
	 */
	loadMap( mapFileName );
	loadMapInfo( mapInfoFileName );
	
	/**
	 * count for all valid cells
	 */
	findValidCells();
    }
    
    public int getMapWidth()
    {
	return mapWidth;
    }
    
    public int getMapHeight()
    {
	return mapHeight;
    }
    
    public int getNumAgents()
    {
	return numAgents;
    }
    
    public int getInitCellIndex( int agentIndex ) 
    {
	if( agentIndex < 0 || agentIndex >= numAgents ) {
	    
	    System.out.println("Map->getInitCellIndex: Wrong Agent Index!");
	    return -1;
	}
	else if( initCellIndices == null ) {
	    
	    System.out.println("Map->getInitCellIndex: Array initCellIndices Null!");
	    return -1;
	}
	
	else
	    return initCellIndices[agentIndex];
    }
    
    
    public int getGoalCellIndex( int agentIndex )
    {
	if( agentIndex < 0 || agentIndex >= numAgents ) {
	    
	    System.out.println("Map->getGoalCellIndex: Wrong Agent Index!");
	    return -1;
	}
	else if( goalCellIndices == null ) {
	    
	    System.out.println("Map->getGoalCellIndex: Array initCellIndices Null!");
	    return -1;
	}
	
	else
	    return goalCellIndices[agentIndex];
    }
    
    /**
     * from left to right
     * from top to bottom
     */
    public int getCellType( int row, int col )
    {
	if( col < 0 || col >= mapWidth || 
		row < 0 || row >= mapHeight ) {
	    
	    System.out.println("Map->getCellType: Wrong Position!");
	    return MapCellType.INVALID_CELL;
	}
	
	else
	    return mapCells[row][col];
    }
    
    
    public int getCellType( int cellIndex )
    {
	if( cellIndex < 0 || cellIndex >= mapHeight * mapWidth ) {
	    
	    System.out.println("Map->getCellCoordination: Wrong Index!");
	    return MapCellType.INVALID_CELL;
	}
	else {
	    
	    int row = cellIndex / mapWidth;
	    int col = cellIndex - row * mapWidth;
	    
	    return mapCells[row][col];
	}
    }
    
    /**
     * get the linear index of a cell
     * 
     * index arrangement
     * from left to right
     * from top to bottom
     * 
     */
    public int getCellIndex( int row, int col )
    {
	if( col < 0 || col >= mapWidth || 
		row < 0 || row >= mapHeight ) {
	    
	    System.out.println("Map->getCellIndex: Wrong Position!");
	    return -1;
	}
	else {
	    
	    int index = col + row * mapWidth;
	    return index;
	}
    }
    
    public int getCellIndex( GridWorldLocation gridLocation )
    {
	if( gridLocation == null ) {
	    
	    System.out.println("Map->getCellIndex: NULL GridWorldLocation!");
	    return -1;
	}
	
	int row = gridLocation.getRow();
	int col = gridLocation.getCol();
	
	return getCellIndex(row, col);
    }
    
    
    /**
     * return a length 2 array {row, col}
     * 
     */
    public int[] getCellCoordination( int cellIndex )
    {
	if( cellIndex < 0 || cellIndex >= mapHeight * mapWidth ) {
	    
	    System.out.println("Map->getCellCoordination: Wrong Index!");
	    return null;
	}
	else {
	    
	    int row = cellIndex / mapWidth;
	    int col = cellIndex - row * mapWidth;
	    
	    return new int[]{row,col};
	}
    }
    
    public int getCellRow( int cellIndex )
    {
	if( cellIndex < 0 || cellIndex >= mapHeight * mapWidth ) {
	    
	    System.out.println("Map->getCellRow: Wrong Index!");
	    return -1;
	}
	else {
	 
	    int row = cellIndex / mapWidth;
	    return row;
	}
    }
    
    
    public int getCellCol( int cellIndex ) 
    {
	if( cellIndex < 0 || cellIndex >= mapHeight * mapWidth ) {
	    
	    System.out.println("Map->getCellCol: Wrong Index!");
	    return -1;
	}
	else {
	    
	    int row = cellIndex / mapWidth;
	    int col = cellIndex - row * mapWidth;
	    
	    return col;
	}
    }
    
    public int getNumCells()
    {
	return numCells;
    }
    
    public int getValidCellNum()
    {
	return validCellNum;
    }
    
    
    private void loadMap( String mapFileName )
    {
	try {
	    
	    BufferedReader mapReader = new BufferedReader(new FileReader(mapFileName));
	    
	    String line = "";
	    


	    int rowCount = 0;
	    while( (line = mapReader.readLine()) != null ) {
		
		if( line.startsWith("%") || line.length() == 0 )
		    continue;
		
		/**
		 * read map width and height
		 * and construct the map matrix
		 */
		else if( line.startsWith("Map Width and Height:") ) {
		    
		    StringTokenizer strToken = new StringTokenizer( line, ":" );
		    strToken.nextToken();
		    
		    String whString = strToken.nextToken();
		    StringTokenizer whToken = new StringTokenizer( whString, "," );
		    mapWidth = Integer.parseInt( whToken.nextToken() );
		    mapHeight = Integer.parseInt( whToken.nextToken()  );
		    
		    mapCells = new int[mapHeight][mapWidth];
		    numCells = mapWidth * mapHeight;
		}
		
		/**
		 * read each cell
		 */
		else {
		    
		    StringTokenizer strToken = new StringTokenizer( line, ",");
		    int colCount = 0;
			
		    while( strToken.hasMoreElements()) {
			    
			mapCells[rowCount][colCount] = Integer.parseInt(strToken.nextToken());
			    
			colCount++;
		    }
			
		    rowCount++;		    
		}
	    }
	    
	    mapReader.close();
	}
	catch (Exception e) {
	    // TODO: handle exception
	    
	    e.printStackTrace();
	}
	
	/**
	 * print for test
	 */
	for( int row = 0; row < mapHeight; row++ ) {
	    
	    
	    for( int col = 0; col < mapWidth; col++ ) {
		
		if( col < mapWidth-1 )
		    System.out.print(mapCells[row][col]+", ");
		else
		    System.out.print(mapCells[row][col]);
	    }
	    System.out.println();
	}
	
    }
    
    
    /**
     * load map information
     */
    private void loadMapInfo( String mapInfoFileName )
    {
	try {
	    
	    BufferedReader mapInfoReader = 
		    new BufferedReader(new FileReader(mapInfoFileName));
	    
	    String line = "";
	    
	    /**
	     * file format
	     * 
	     * row-1 "Agent Number:" n
	     * row-2 to row-(n+1)
	     * initCell,goalCell
	     */
	    int agentIndex = 0;
	    while( (line = mapInfoReader.readLine()) != null ) {
		
		//the line started with a '%' is a comment line
		if( line.startsWith("%") || line.length() == 0 )
		    continue;
		else if( line.startsWith("Agent Num:") ) {
		    
		    StringTokenizer strToken = new StringTokenizer( line, ":");
		    strToken.nextToken();
		    numAgents = Integer.parseInt( strToken.nextToken() );
		    
		    initCellIndices = new int[numAgents];
		    goalCellIndices = new int[numAgents];
		}
		else {
		    
		    StringTokenizer strToken = new StringTokenizer( line, ",");
			
		    /**
		     * there must be only two tokens
		     */
		    //init cell
		    initCellIndices[agentIndex] = Integer.parseInt( strToken.nextToken() );
			
		    //goal cell
		    goalCellIndices[agentIndex] = Integer.parseInt( strToken.nextToken() );
			
		    agentIndex++;
		}
	    }
	    
	    
	    mapInfoReader.close();
	    
	}
	catch( IOException ioe ) {
	    
	    ioe.printStackTrace();
	}
	
	/**
	 * print for test
	 */
	System.out.println("Number of Agents: "+numAgents);
	for( int agent = 0; agent < numAgents; agent++ ) {
	    
	    System.out.println("Agent "+agent+": init cell "+
	    initCellIndices[agent]+", goal cell "+goalCellIndices[agent]);
	}
    }
    
    

    private void findValidCells()
    {
	
	ArrayList<Integer> cellIndicesList = new ArrayList<Integer>();
	
	for( int cellIndex = 0; cellIndex < numCells; cellIndex++ ) {
	    
	    int cellType = getCellType( cellIndex );
	    
	    if( cellType == MapCellType.WIDE_CELL || 
		    cellType == MapCellType.NARROW_CELL ) {
		
		cellIndicesList.add( cellIndex );
	    }
	}
	
	validCellNum = cellIndicesList.size();
	validCellIndices = new int[validCellNum];
	
	for( int listIndex = 0; listIndex < validCellNum; listIndex++ ) {
	    
	    validCellIndices[listIndex] = cellIndicesList.get( listIndex );
	}
	
	//release the memory
	cellIndicesList = null;
    }
    
    
    
}
