package guandan.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class StoreManager {
	private SharedPreferences storage;
	private GameView gameView;

	public StoreManager(GameView gv, Context context) {
		gameView = gv;
		storage = context.getSharedPreferences("Guandan_store",
				Context.MODE_PRIVATE);
	}

	public void saveState() {
		Editor editor = storage.edit();

		editor.commit();
	}

	private void savePlayerNames(Editor editor) {
		for (int i = 0; i < 4; i++) {
			String key = "playername" + String.valueOf(i);
			editor.putString(key, gameView.getPlayerNames()[i]);
		}
	}

	private void loadPlayerNames() {
		String[] playerNames = new String[4];

		for (int i = 0; i < 4; i++) {
			String key = "playername" + String.valueOf(i);
			playerNames[i] = storage.getString(key, "");
		}
		gameView.setPlayerNames(playerNames);
	}
	
	private void saveTributePokers(Editor editor)
	{
		Poker[]	tributePokers = gameView.getTributePokers();
		if(tributePokers == null)
			tributePokers = new Poker[4];
		
		for(int i = 0; i < 4; i++)
		{
			String keyNum = "tributenum"+String.valueOf(i);
			String keyPoker = "tributepoker"+String.valueOf(i);
			
			if(tributePokers[i] == null)
				editor.putInt(keyNum, 0);
			else
			{
				editor.putInt(keyNum, 1);
				editor.putInt(keyPoker+"pa", tributePokers[i].pattern);
				editor.putInt(keyPoker+"po", tributePokers[i].points);
			}
		}
		
	}
	
	private void loadTributePokers()
	{
		Poker[] tributePokers = new Poker[4];
		
		for(int i = 0; i < 4; i++)
		{
			String keyNum = "tributenum"+String.valueOf(i);
			String keyPoker = "tributepoker"+String.valueOf(i);
			
			/**
			 * 载入并进行恢复
			 */
			int pokerNum = storage.getInt(keyNum,0);
			if(pokerNum == 0)
				continue;
			else
			{
				int pattern = storage.getInt(keyPoker+"pa",0);
				int points = storage.getInt(keyPoker+"po",0);
				
				tributePokers[i] = new Poker(pattern, points);
//				players[i].Ipaid(tributePokers[i]);
			}
		}
		gameView.setTributePokers(tributePokers);
	}
}
