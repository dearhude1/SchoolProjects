/**
 * AI_Player.java
 * @author 孟庆锴
 * March 17th 2011
 * 
 * 实现AI功能
 * 简单AI
 * 普通AI
 * 高级AI
 * 
 */

package guandan.player;

import guandan.helper.Parser;
import guandan.helper.Comparator;
import guandan.constants.PokerPattern;
import guandan.constants.PokerPoints;
import guandan.game.*;

import java.util.*;

import android.util.Log;

public class AI_Player extends Player implements Runnable {
	// Attribute/////////
	/**
	 * 定义保存各种牌数量的数组的长度 数值15，A,2,3~k和大王小王，共计15种大小
	 */
	protected final int length = 15;

	/**
	 * 此AI的属性 0为简单AI 1为普通AI 2为高级AI
	 */
	protected int AI = 0;

	/**
	 * 当前牌级 范围A,2,3~K
	 */
	protected int currentGrade = 2;

	/**
	 * 当前手牌数 范围0~27
	 */
	protected int pokerNum = -1;

	/**
	 * 保存各种大小的牌的张数 如对应numList[1] = 1,代表有A一张 numList[0] = 1,代表有小王一张 numList[14] =
	 * 1,代表有大王一张
	 */
	protected int numList[] = new int[length];
	/**
	 * 保存牌局已经打出的牌的张数 如对应outNumList[2] = 2，代表已打出2两张
	 */
	protected int outNumList[] = new int[length];
	/**
	 * 保存对家需要的牌型
	 */
	protected int friendNeed[];
	/**
	 * 保存对手需要的牌型
	 */
	protected int enemyNeed[];
	/**
	 * 保存各种花色的各牌张数 如pokerList0[0][1] = 1,代表有梅花A一张 如pokerList0[1][1] = 1,代表有方片A一张
	 * 如pokerList0[2][1] = 1,代表有红桃A一张 如pokerList0[3][1] = 1,代表有黑桃A一张
	 */
	protected int pokerList0[][] = new int[4][length];
	/**
	 * 手牌剩余的最小手数
	 */
	protected double leftHands = 100;

	protected Thread playingThread;

	/**
	 * AI_Player的构造方法
	 * 
	 * @param battleView
	 *            创建此AI的BattleView对象
	 * @param myTurn
	 *            此AI在牌局中的轮次
	 * @param AI
	 *            此AI的难易度
	 * @param grade
	 *            当前牌级
	 */
	public AI_Player(GameView gv, int turn, int AI) {
		super(gv, turn);
		this.AI = AI;
		this.isFinished = false;
		initial();
	}

	private static final String ACTIVITY_TAG = "======== bingo debug =========";

	public void run() {
		/**
		 * 运行完一次即退出
		 */
		Log.e(ACTIVITY_TAG, "【BattleView--loadState】=====  当前轮次是（myTurn）：   "
				+ myTurn);
		Log.e(ACTIVITY_TAG,
				"【BattleView--loadState】=====  当前轮次是（getCurrentTurn）：   "
						+ gameView.getCurrentTurn());
		if (myTurn == gameView.getCurrentTurn()) {
			Random random = new Random();
			int time = random.nextInt(1500);
			if (time < 500)
				time = 500;
			try {
				Thread.sleep(time);
			} catch (Exception e) {
				e.printStackTrace();
			}

			ArrayList<Poker> leadPokers = lead();
			int k = 0;
			for (int i = 0; i < 15; i++) {
				k += numList[i];
			}
			Log.i("test num" + this.myTurn, k + " ");
			if (k != this.pokerList.size()) {
				try {
					throw new Exception("");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			gameView.someoneLead(leadPokers, myTurn);

			if (pokerList.size() == 0) {
				gameView.recordOrder(myTurn);

				isFinished = true;
			}

			/**
			 * 出牌之后通知battleView 将出牌权转让到下一个
			 */
			gameView.receiveMessage_NextTurn();
		}
	}

	public void itsMyTurn() {
		/**
		 * 启动线程进行一次出牌
		 */
		if (myTurn == gameView.getCurrentTurn()) {
			playingThread = null;
			playingThread = new Thread(this);
			playingThread.start();
		}
	}

	public void inningBegin() {
		/**
		 * 对进回贡的牌做处理
		 */
		super.inningBegin();

		isFinished = false;

		if (myTurn == gameView.getCurrentTurn()) {
			playingThread = null;
			playingThread = new Thread(this);
			playingThread.start();
		}
	}

	/**
	 * 设置该AI等级
	 * 
	 * @param AI
	 *            0为简单AI;1为普通AI;2为高级AI
	 */
	public void setAI(int AI) {
		this.AI = AI;
	}

	/**
	 * 设置手牌打完
	 */
	public void setIsFinished() {
		this.isFinished = true;
	}

	/**
	 * 设置牌级
	 * 
	 * @param grade
	 */
	public void setCurrentGrade(int grade) {
		this.currentGrade = grade;
	}

	// public void setFinished(boolean bFinished)
	// {
	// isFinished = bFinished;
	//
	// if(isFinished)
	// {
	// if(playingThread.getState() == Thread.State.RUNNABLE)
	// {
	// playingThread.y
	// }
	// }
	// }

	/**
	 * 设置手牌 发牌结束后设置AI初始手牌
	 * 
	 * @param pokerList
	 */
	public void pokerAssigned(ArrayList<Poker> pokerList) {
		this.pokerList = new LinkedList<Poker>(pokerList);
		// this.currentGrade = this.battleView.getCurrentGrade();
		this.currentGrade = this.gameView.getCurrentGrade() == 14 ? 1
				: this.gameView.getCurrentGrade();
		initial();
		ListIterator<Poker> listIterator = this.pokerList.listIterator();
		/**
		 * 根据手牌，将对应的数组赋值
		 */
		while (listIterator.hasNext()) {
			Poker poker = listIterator.next();
			switch (poker.pattern) {
			case PokerPattern.JOKER:
				if (poker.points == PokerPoints.LITTLE_JOKER) {
					numList[0]++;
				} else
					numList[14]++;
				break;
			case 1:
				numList[poker.points]++;
				pokerList0[0][poker.points]++;
				break;
			case 2:
				numList[poker.points]++;
				pokerList0[1][poker.points]++;
				break;
			case 3:
				numList[poker.points]++;
				pokerList0[2][poker.points]++;
				break;
			case 4:
				numList[poker.points]++;
				pokerList0[3][poker.points]++;
				break;
			}
		}
	}

	/**
	 * 根据AI难易度，设置记住了哪些牌
	 * 
	 * @param currentPoker
	 *            任意角色新打出的牌
	 */
	public void setOutNumList(ArrayList<Poker> currentPoker) {
		if (currentPoker == null || currentPoker.size() == 0) {
			return;
		}
		switch (AI) {
		case 1:
			for (ListIterator<Poker> listIterator = currentPoker.listIterator(); listIterator
					.hasNext();) {
				Poker poker = listIterator.next();
				if (poker.pattern == PokerPattern.JOKER) {
					if (poker.points == PokerPoints.LITTLE_JOKER) {
						outNumList[0]++;
					} else
						outNumList[14]++;
				} else if (poker.points == this.currentGrade) {
					if (Math.random() * 100 < 80) {
						outNumList[poker.points]++;
					}
				} else if (poker.points == PokerPoints.ACE) {
					if (Math.random() * 100 < 50) {
						outNumList[poker.points]++;
					}
				} else if (poker.points == PokerPoints.KING) {
					if (Math.random() * 100 < 30) {
						outNumList[poker.points]++;
					}
				}
			}
			break;
		case 2:
			for (ListIterator<Poker> listIterator = currentPoker.listIterator(); listIterator
					.hasNext();) {
				Poker poker = listIterator.next();
				if (poker.pattern == PokerPattern.JOKER) {
					if (poker.points == PokerPoints.LITTLE_JOKER) {
						outNumList[0]++;
					} else
						outNumList[14]++;
				} else if (poker.points == this.currentGrade) {
					outNumList[poker.points]++;
				} else if (poker.points == PokerPoints.ACE
						|| poker.points > PokerPoints.TEN) {
					if (Math.random() * 100 < 80) {
						outNumList[poker.points]++;
					}
				} else {
					if (Math.random() * 100 < 40) {
						outNumList[poker.points]++;
					}
				}
			}
			break;
		}
	}

	/**
	 * 在进贡还贡阶段返回大王数 以来判定是否要进行进贡还贡
	 * 
	 * @return 大王数
	 */
	public int checkPay() {
		return numList[14];//
	}

	/**
	 * 进贡
	 * 
	 * @return 手中除红桃主牌的最大牌
	 */
	public Poker payTribute() {
		ArrayList<Poker> result = new ArrayList<Poker>();
		if (numList[14] == 1) {
			result = selectPokers(1, PokerPattern.JOKER, PokerPoints.OLD_JOKER,
					result);
			return result.get(0);
		} else if (numList[0] != 0) {
			result = selectPokers(1, PokerPattern.JOKER,
					PokerPoints.LITTLE_JOKER, result);
			return result.get(0);
		} else if (numList[this.currentGrade] != 0
				&& numList[this.currentGrade] != pokerList0[2][this.currentGrade]) {
			result = selectPokers(1, -1, this.currentGrade, result);
			return result.get(0);
		} else {
			if (this.currentGrade != 1) {
				if (numList[1] != 0) {
					result = selectPokers(1, -1, PokerPoints.ACE, result);
					return result.get(0);
				} else {
					for (int i = 13; i > 1; i--) {
						if (this.currentGrade != i && numList[i] != 0) {
							result = selectPokers(1, -1, i, result);
							return result.get(0);
						}
					}
				}
			} else {
				for (int i = 13; i > 1; i--) {
					if (this.currentGrade != i && numList[i] != 0) {
						result = selectPokers(1, -1, i, result);
						return result.get(0);
					}
				}
			}
		}
		return null;
	}

	/**
	 * 将被进贡或被回贡得到的牌加入手牌
	 * 
	 * @param poker
	 *            新的到的牌
	 */
	public void receivePay(Poker poker) {

		pokerList.add(new Poker(poker.pattern, poker.points));

		if (poker.pattern == PokerPattern.JOKER) {
			if (poker.points == PokerPoints.LITTLE_JOKER) {
				numList[0]++;
			} else
				numList[14]++;
		} else {
			numList[poker.points]++;
			pokerList0[poker.pattern - 1][poker.points]++;
		}
	}

	/**
	 * 回贡
	 * 
	 * @return 回贡的牌
	 */
	public Poker payBack() {
		return null;
	}

	/**
	 * 返回牌型类型和大虾
	 * 
	 * @param currentPoker
	 *            当前牌型
	 * @return int result[2] result[0]
	 *         数值代表牌型，0单牌，1对子，2三张，3连队，4钢板，5三带两，6杂花顺，7同花顺，8炸弹，9天王炸
	 *         参见pokerTypr.java result[1] 代表大小0为小王，14为大王
	 */
	protected int[] pokerType(ArrayList<Poker> currentPoker) {
		return Parser.getPokerType(currentPoker, this.currentGrade);
	}

	/**
	 * 比较两张扑克的大小
	 * 
	 * @param poker0
	 * @param poker1
	 * @return 如果poker0>poker1返回true，否则返回false
	 */
	protected boolean comparePoker(Poker poker0, Poker poker1) {
		switch (Comparator.comparePoker(poker0, poker1, this.currentGrade)) {
		case 1:
			return true;
		default:
			return false;
		}
		// if(poker0.pattern == 0 && poker1.pattern ==0){
		// if(poker0.points == 1 && poker1.points ==0){
		// return true;
		// }
		// else return false;
		// }
		// else if(poker0.pattern ==0 && poker1.pattern != 0){
		// return true;
		// }
		// else if(poker0.pattern !=0 && poker1.pattern == 0){
		// return false;
		// }
		// else{
		// if(poker0.points == poker1.points){
		// return false;
		// }
		// else if (poker0.points == this.currentGrade){
		// return true;
		// }
		// else if (poker1.points == this.currentGrade){
		// return false;
		// }
		// else if (poker0.points == 1){
		// return true;
		// }
		// else if (poker1.points ==1){
		// return false;
		// }
		// else if(poker0.points > poker1.points){
		// return true;
		// }
		// }
		// return false;
	}

	/**
	 * 出炸的策略
	 * 
	 * @return
	 */
	protected ArrayList<Poker> payFire() {
		return new ArrayList<Poker>();
	}

	/**
	 * 根据即将打出的牌，对相应的数组做出修改
	 * 
	 * @param number
	 *            即将打出的数量
	 * @param pattern
	 *            删除的类型 0，1，2，3，4 为各自对应的牌型
	 * @param points
	 *            删除的牌的点数
	 * @param result
	 *            返回结果
	 * @return
	 */
	protected ArrayList<Poker> selectPokers(int number, int pattern,
			int points, ArrayList<Poker> result) {
		int count = 0;
		ListIterator<Poker> listIterator;
		if (pattern == 0) {
			while ((number--) > 0) {
				listIterator = this.pokerList.listIterator(count);
				while (listIterator.hasNext()) {
					Poker poker = listIterator.next();
					if (poker.pattern == 0 && poker.points == points) {
						break;
					}
					count++;
				}
				Poker poker = (Poker) this.pokerList.get(count);
				this.pokerList.remove(count);
				result.add(poker);
				if (poker.points == PokerPoints.LITTLE_JOKER) {
					numList[0]--;
				} else {
					numList[14]--;
				}
			}
		} else if (pattern == -1) {
			while ((number--) > 0) {
				listIterator = this.pokerList.listIterator(count);
				while (listIterator.hasNext()) {
					Poker poker = listIterator.next();
					if (poker.pattern != 0
							&& poker.points == points
							&& !(poker.pattern == 3 && poker.points == this.currentGrade)) {
						/**
						 * 为了防止同花顺被保留
						 */
						if (pokerList0[poker.pattern - 1][poker.points] > 0) {
							break;
						}

					}
					count++;
				}
				Poker poker = (Poker) this.pokerList.get(count);
				pokerList0[poker.pattern - 1][poker.points]--;
				this.pokerList.remove(count);
				result.add(poker);
				numList[poker.points]--;
			}
		} else {
			while ((number--) > 0) {
				listIterator = this.pokerList.listIterator(count);
				while (listIterator.hasNext()) {
					Poker poker = listIterator.next();
					if (poker.pattern == pattern && poker.points == points) {
						break;
					}
					count++;
				}
				Poker poker = (Poker) this.pokerList.get(count);
				pokerList0[poker.pattern - 1][poker.points]--;
				this.pokerList.remove(count);
				result.add(poker);
				numList[poker.points]--;
			}
		}
		return result;
	}

	/**
	 * 出牌，根据不同策略返回应该打出的牌
	 * 
	 * @param currentPoker
	 *            现在牌面最大的牌
	 * @param currentWinner
	 *            该牌面的出牌者
	 * @return 应该打出的牌
	 * 
	 * 
	 * 
	 */
	public ArrayList<Poker> lead() {

		ArrayList<Poker> currentPoker = gameView.getCurrentPokers();
		int currentWinner = gameView.getCurrentWinner();

		switch (AI) {
		case 0:
			return easy_AI(currentPoker, currentWinner);
		case 1:
			return normal_AI(currentPoker, currentWinner);
		case 2:
			return hard_AI(currentPoker, currentWinner);
		}
		return null;
	}

	/**
	 * 简单AI策略
	 * 
	 * @return 简单AI应该出什么牌
	 */
	protected ArrayList<Poker> easy_AI(ArrayList<Poker> currentPoker,
			int currentWinner) {
		return new ArrayList<Poker>();
	}

	/**
	 * 普通AI策略
	 * 
	 * @return 普通AI应该出什么牌
	 */
	protected ArrayList<Poker> normal_AI(ArrayList<Poker> currentPoker,
			int currentWinner) {
		return new ArrayList<Poker>();
	}

	/**
	 * 高级AI策略
	 * 
	 * @return 高级AI应该出什么牌
	 */
	protected ArrayList<Poker> hard_AI(ArrayList<Poker> currentPoker,
			int currentWinner) {
		return new ArrayList<Poker>();
	}

	/**
	 * 设置对家需要什么牌
	 * 
	 * @param type
	 *            牌型
	 * @param need
	 *            0为不需要；1为需要
	 */
	protected void setFriendNeed(int type, int need) {
	}

	/**
	 * 设置对手需要什么牌
	 * 
	 * @param type
	 *            牌型
	 * @param need
	 *            是否需要
	 */
	protected void setEnemyNeed(int type, int need) {

	}

	/**
	 * 有出牌情况，调用此方法做相应记录工作 先调用上层父类的方法
	 * 
	 * @param currentPokers
	 * @param turn
	 */
	public void someoneLead(ArrayList<Poker> currentPokers, int turn) {
		super.someoneLead(currentPokers, turn);
	}

	/**
	 * 对相关数组初始化
	 */
	protected void initial() {
		this.isFinished = false;
		for (int i = 0; i < length; i++) {
			numList[i] = 0;
			outNumList[i] = 0;
		}
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < this.pokerList0[0].length; j++) {
				pokerList0[i][j] = 0;
			}
		}
	}
}
