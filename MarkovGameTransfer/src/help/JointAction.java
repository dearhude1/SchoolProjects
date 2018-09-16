package help;

public class JointAction {

	/**
	 * action of each agent
	 * from agent0 to agent(n-1)
	 */
	private int[] actions;
	
	public JointAction( int[] acts )
	{
		int agentNum = acts.length;
		
		actions = new int[agentNum];
		
		for( int agent = 0; agent < agentNum; agent++ )
			actions[agent] = acts[agent];
	}
	
	
	/**
	 * get the joint action in memory from all agents' actions
	 *
	public int getJointAction( )
	{
		int agentNum = actions.length;
		
		int jntAct = 0;
		for( int agent = 0; agent < agentNum; agent++ ) {
			
			int actLoc = actions[agent];
			for( int agent_p = agent+1; agent_p < agentNum; agent_p++ ) {
				
				actLoc *= actionNums[agent_p];
			}
			
			jntAct += actLoc;
		}
		
		return jntAct;
	}
	*/
	
	public int getAction( int agent )
	{
		if( agent < 0 || agent >= actions.length ) {
			
			System.out.println("JointAction@getAction: Wrong Parameter agent!");
			return -1;
		}
		
		return actions[agent];
	}
	
	public int[] getActions()
	{
		return actions;
	}
	
	public void setAction( int agent, int act )
	{
		if( agent < 0 || agent >= actions.length ) {
			
			System.out.println("JointAction@setAction: Wrong Parameter agent!");
			return;
		}
		
		actions[agent] = act;
	}
}
