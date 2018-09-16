package pacman.game.internal;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;

import pacman.game.Game;
import pacman.game.Constants.MOVE;

/**
 * added by dearhude1,
 * a compact representation of a maze
 * 
 * In such kind of graph, a node is a non-pipe node in the maze
 * the edge between each vertex denotes the connection between two non-pipe nodes
 * 
 * NPP is short for non-pipe
 */
public class MazeGraph {

	/**
	 * the indices of all non-pipe nodes
	 * note that the indices is the index of nodes in the maze
	 */
	private int[] nonPipeNodeIndices;

	/**
	 * the hash map which maps the non-pipe node index to 
	 * the class of NonPipeNode
	 */
	private HashMap<Integer, NonPipeNode> nodeIndexToNPP;
	
	/**
	 * the array of connections between each two NPP nodes
	 */
	private Connection[] connections;
	
	/**
	 * the number of nodes in this graph
	 */
	private int numNPP;
	
	/**
	 * the number of connections in this graph
	 */
	private int numConnections;
	
	/**
	 * inner class NonPipeNode
	 */
	public class NonPipeNode {
		
		/**
		 * the index only in the MazeGraph
		 */
		private int nonPipeIndex;
		
		/**
		 * the index of corresponding node in the maze
		 */
		private int nodeIndex;
		
		/**
		 * the node indices of all neighbor NPP nodes
		 */
		private EnumMap<MOVE, Integer> neighborNPPNodeIndices = 
				new EnumMap<MOVE, Integer>(MOVE.class);
				
		public NonPipeNode( int nodeIdx, int nppIndex )
		{
			nodeIndex = nodeIdx;
			nonPipeIndex = nppIndex;
		}
		
		public int getNPPIndex() 
		{
			return nonPipeIndex;
		}
		
		public int getNodeIndex()
		{
			return nodeIndex;
		}
				
		public void setNeighborNPPNodeIndex( MOVE direction, int neighborNodeIndex )
		{
			if( direction == MOVE.NEUTRAL ) {
				
				/**
				 * do not put this direction into the map
				 */
				System.out.println("NO NEUTRAL direction!");
				//neighborNPPNodeIndices.put( MOVE.NEUTRAL, nodeIndex );
			}
			else {
				
				/**
				 * it is possible that the neighbor NPP is this NPP
				 */
				if( !neighborNPPNodeIndices.containsKey( direction ) ) {
					
					neighborNPPNodeIndices.put( direction, neighborNodeIndex );
				}
				
			}
		}
		
		public int getNeighborNPPNodeIndex( MOVE direction )
		{

			if( neighborNPPNodeIndices.containsKey( direction ) ) {
				
				return neighborNPPNodeIndices.get( direction );
			}
			else {
				
				System.out.println("Do not have this direction key");
				return -1;
			}
		}
	}
	
	
	/**
	 * inner class Connection
	 * the edge bewteen each connected NonPipeNode
	 * @author dell
	 *
	 */
	public class Connection {
		
		/**
		 * the index of connection
		 */
		private int connectionIndex;
		
		/**
		 * the node indices of the two connected NPP
		 */
		private int[] connectedNPPNodeIndices;
		
		/**
		 * the NPP index of the two connected NPP
		 */
		private int[] connectedNPPIndices;
		
		public Connection( int connIndex, NonPipeNode npp1, 
				NonPipeNode npp2 )
		{
			
			connectionIndex = connIndex;
			
			connectedNPPIndices = new int[2];
			connectedNPPNodeIndices = new int[2];
			
			connectedNPPNodeIndices[0] = npp1.getNodeIndex();
			connectedNPPIndices[0] = npp1.getNPPIndex();
			connectedNPPNodeIndices[1] = npp2.getNodeIndex();
			connectedNPPIndices[1] = npp2.getNPPIndex();
		}
		
		public int getConnectionIndex()
		{
			return connectionIndex;
		}
		
		public int[] getNPPNodeIndices()
		{
			return connectedNPPNodeIndices;
		}
		
		public boolean hasNPP_NodeIndex( int nppNodeIndex )
		{
			if( connectedNPPNodeIndices[0] == nppNodeIndex || 
					connectedNPPNodeIndices[1] == nppNodeIndex )
				return true;
			else
				return false;
		}
		
		public boolean hasNPP_NPPIndex( int nppIndex )
		{
			if( connectedNPPIndices[0] == nppIndex || 
					connectedNPPIndices[1] == nppIndex )
				return true;
			else
				return false;
		}
	}
	
	
	//Maze graph is created only for the current maze in the game
	public MazeGraph( Game game ) 
	{
		
		/**
		 * get the number of nodes in the current maze
		 */
		int numNodes = game.getNumberOfNodes();
		
		/**
		 * find all non-pipe nodes and 
		 * create the array nonPipeNodeIndices
		 */
		ArrayList<Integer> nonPipeNodeList = new ArrayList<Integer>();
		for( int nodeIdx = 0; nodeIdx < numNodes; nodeIdx++ ) {
			
			if( !game.isPipe( nodeIdx ) ) {
				
				nonPipeNodeList.add( nodeIdx );
			}
		}
		numNPP = nonPipeNodeList.size();
		nonPipeNodeIndices = new int[numNPP];
		for( int nonPipeIndex = 0; nonPipeIndex < numNPP; nonPipeIndex++ ) {
			
			nonPipeNodeIndices[nonPipeIndex] = nonPipeNodeList.get( nonPipeIndex );
		}
		nonPipeNodeList.clear();
		nonPipeNodeList = null;
		
		/**
		 * create the HashMap and each NonPipeNode class instance
		 */
		createNonPipeNodes();
		
		/**
		 * create the connections between NonPipeNodes 
		 * and assign the neighbors to each NonPipeNode
		 */
		createConnections( game );
	}
	
	
	private void createNonPipeNodes()
	{
		if( nonPipeNodeIndices == null ) {
			
			System.out.println("MazeGraph->createNonPipeNodes: NULL Array");
			return;
		}
		
		nodeIndexToNPP = new HashMap<Integer, MazeGraph.NonPipeNode>();
		for( int nppIndex = 0; nppIndex < numNPP; nppIndex++ ) {
			
			int nppNodeIndex = nonPipeNodeIndices[nppIndex];
			NonPipeNode nppNode = new NonPipeNode(nppNodeIndex, nppIndex);
			
			if( !nodeIndexToNPP.containsKey( nppNodeIndex ) ) {
				
				nodeIndexToNPP.put( nppNodeIndex, nppNode );
			}
			else {
				
				System.out.println("MazeGraph->createNonPipeNodes: node "+nppNodeIndex+" is already in");
				nppNode = null;
			}
		}
	}
	
	private void createConnections( Game game )
	{
		
		ArrayList<Connection> connectionList = new ArrayList<Connection>();
		int connIndex = 0;
		
		/**
		 * for each NPP node 
		 * move along an available direction until find another NPP node 
		 */
		for( int nppIndex = 0; nppIndex < numNPP; nppIndex++ ) {
			
			int nppNodeIndex = nonPipeNodeIndices[nppIndex];
			
			/**
			 * for each possible direction
			 */
			MOVE[] availMoves = game.getPossibleMoves( nppNodeIndex );
			for( int movIndex = 0; movIndex < availMoves.length; movIndex++ ) {
				
				/**
				 * move along the direction until another NPP node is found 
				 * Note that the another NPP can be the current NPP itself
				 */
				MOVE move = availMoves[movIndex];
				int neighborNodeIndex = game.getNeighbour( nppNodeIndex, move );
				while( game.isPipe( neighborNodeIndex ) ) {
					
					/**
					 * check whether the neighbor also has this move
					 * if not, something is wrong
					 */
					MOVE[] neighMoves = game.getPossibleMoves( neighborNodeIndex );
					boolean found = false;
					for( int movIndex_p = 0; movIndex_p < neighMoves.length; movIndex_p++ ) {
						
						MOVE mov_p = neighMoves[movIndex_p];
						if( mov_p == move ) {
							
							found = true;
							break;
						}
					}
					if( found ) {
						
						neighborNodeIndex = game.getNeighbour(neighborNodeIndex, move);
					}
					else {
						
						System.out.println("MazeGraph->createConnections: The neighbour does not have this direction");
					}
				}
				
				/**
				 * set the neighbors
				 */
				NonPipeNode nppNode = nodeIndexToNPP.get( nppNodeIndex );
				nppNode.setNeighborNPPNodeIndex( move, neighborNodeIndex );
				
				/**
				 * we create a connection by calling the function: createConnection
				 * however, if the connection between the two nodes has been added 
				 * then do not create the connection
				 */
				boolean connFound = false;
				for( int connListIndex = 0; connListIndex < connectionList.size(); connListIndex++ ) {
					
					Connection conn = connectionList.get( connListIndex );
					
					if( conn.hasNPP_NodeIndex( nppNodeIndex ) && 
							conn.hasNPP_NodeIndex( neighborNodeIndex ) ) {
						
						connFound = true;
						break;
					}
				}
				if( !connFound ){
					
					Connection connection = createConnection( connIndex, 
							nppNodeIndex, neighborNodeIndex );
					
					connectionList.add( connection );
					connIndex++;
				}
				
			}//Here we have added all neighbours of the current NPP
		}
		
		
		/**
		 * transform the connections
		 */
		numConnections = connectionList.size();
		connections = new Connection[numConnections];
		for( int index = 0; index < numConnections; index++ ) {
			
			Connection conn = connectionList.get( index );
			connections[index] = conn;
		}
		connectionList.clear();
		connectionList = null;
	}
	
	private Connection createConnection( int connIndex, int nppNodeIndex1, 
			int nppNodeIndex2 )
	{
		/**
		 * check the parameters
		 */
		if( !nodeIndexToNPP.containsKey(nppNodeIndex1) || 
				!nodeIndexToNPP.containsKey(nppNodeIndex2) ) {
			
			System.out.println("MazeGraph->createConnection: No Such NPP in HashMap");
			return null;
		}
		
		
		NonPipeNode npp1 = nodeIndexToNPP.get( nppNodeIndex1 );
		NonPipeNode npp2 = nodeIndexToNPP.get( nppNodeIndex2 );
		
		return new Connection( connIndex, npp1, npp2 );
	}
	
	
	public int getNumNPP()
	{
		return numNPP;
	}
	
	public int getNumConnections()
	{
		return numConnections;
	}
	
	public int[] getAllNPPNodeIndices()
	{
		return nonPipeNodeIndices;
	}
	
	public Connection[] getAllConnections()
	{
		return connections;
	}
	
	//check again
	public int queryConnectionIndex( Game game, int nodeIndex, 
			MOVE direction )
	{
		
		/**
		 * cannot be NEUTRAL direction
		 */
		if( direction == MOVE.NEUTRAL ) {
			
			System.out.println("MazeGraph->queryConnectionIndex: NEUTRAL direction");
			return -1;
		}
		
		/**
		 * find the two NPP nodes related to the connection
		 */
		int nppNodeIndex_1 = -1;
		int nppNodeIndex_2 = -1;
		
		//if the current node is also an NPP
		if( !game.isPipe( nodeIndex ) ) {
			
			nppNodeIndex_1 = nodeIndex;
			
			/**
			 * first move along the direction to find another NPP node
			 */
			int neighborNodeIndex = game.getNeighbour(nodeIndex, direction);
			
			/**
			 * but we should check whether this direction is available in 
			 * the current NPP node since
			 * 
			 * if available, move along the direction
			 * else, move along the opposite direction
			 */
			if( neighborNodeIndex != -1 ) {
				
				while( game.isPipe(neighborNodeIndex) ) {
					
					neighborNodeIndex = game.getNeighbour(neighborNodeIndex, direction);
				}
				nppNodeIndex_2 = neighborNodeIndex;
			}
			else {
				
				MOVE oppDirection = direction.opposite();
				neighborNodeIndex = game.getNeighbour( nodeIndex, oppDirection );
				while( game.isPipe(neighborNodeIndex) ) {
					
					neighborNodeIndex = game.getNeighbour(neighborNodeIndex, oppDirection );
				}
				nppNodeIndex_2 = neighborNodeIndex;
			}
		}
		//if the current node is a pipe node
		else {
			
			/**
			 * first move along the direction
			 */
			int neighborNodeIndex = game.getNeighbour(nodeIndex, direction);
			while( game.isPipe(neighborNodeIndex) ) {
				
				neighborNodeIndex = game.getNeighbour(neighborNodeIndex, direction);
			}
			nppNodeIndex_1 = neighborNodeIndex;
			
			/**
			 * then move along the opposite direction
			 */
			MOVE oppDirection = direction.opposite();
			neighborNodeIndex = game.getNeighbour( nodeIndex, oppDirection);
			while( game.isPipe(neighborNodeIndex) ) {
				
				neighborNodeIndex = game.getNeighbour(neighborNodeIndex, oppDirection);
			}
			nppNodeIndex_2 = neighborNodeIndex;
		}
		
		
		/**
		 * find the corresponding connection
		 */
		for( int connIndex = 0; connIndex < connections.length; connIndex++ ) {
			
			Connection connection = connections[connIndex];
			
			if( connection.hasNPP_NodeIndex( nppNodeIndex_1 ) && 
				connection.hasNPP_NodeIndex( nppNodeIndex_2 ) )
				return connIndex;
		}
		
		/**
		 * cannot find the connection
		 */
		System.out.println("MazeGraph->queryConnection: Cannot find the connection "+nodeIndex+" "+direction);
		return -1;	
	}
	
	public int queryNPPIndex( Game game, int nodeIndex )
	{
		
		if( game.isPipe( nodeIndex ) ) {
			
			System.out.println("MazeGraph->quearyNPPIndex: Not an NPP");
			return -1;
		}
		
		else {
			
			NonPipeNode nppNode = nodeIndexToNPP.get( nodeIndex );
			return nppNode.getNPPIndex();
		}
	}
	
}
