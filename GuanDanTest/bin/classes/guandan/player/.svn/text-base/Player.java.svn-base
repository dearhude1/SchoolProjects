/**
 * Player.java
 * @author 胡裕靖
 * March 23rd 2011
 * 
 * Player类,各类玩家的父类
 * 其子类目前有UserPlayer,AI_Player
 * 可能以后会增加NetworkPlayer等
 */
package guandan.player;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

import guandan.constants.Constants;
import guandan.game.GameView;
import guandan.game.ImageLoader;
import guandan.game.Poker;
import guandan.sprite.PokerSprite;

import android.graphics.Canvas;
import android.graphics.Point;
import android.view.MotionEvent;

public class Player {

	// Fields//////////////////////////////////////////

	protected int myGrade;

	protected int myTurn;

	protected GameView gameView;

	protected LinkedList<Poker> pokerList;

	protected boolean isFinished;

	/**
	 * 绘制功能相关成员
	 */
	protected ArrayList<PokerSprite> pokerSprites_OnDesk;
	protected PokerSprite tributePokerSprite;

	protected ImageLoader imageLoader;
	protected int screenWidth;
	protected int screenHeight;
	private Point outAreaPoint;
	protected int displayWidth_ForPoker;

	protected ReentrantLock spritesLock;

	// Methods/////////////////////////////////////////

	/**
	 * public Player() {
	 * 
	 * }
	 */

	public Player(GameView gv, int turn) {
		gameView = gv;
		imageLoader = ImageLoader.getInstance();
		screenWidth = imageLoader.getScreenWidth();
		screenHeight = imageLoader.getScreenHeight();

		pokerList = new LinkedList<Poker>();
		spritesLock = new ReentrantLock(true);

		pokerSprites_OnDesk = new ArrayList<PokerSprite>();

		myTurn = turn;

		/**
		 * 根据myTurn设置出牌位置
		 */
		setOutArea();
	}

	private void setOutArea() {
		/**
		 * 可以从文件读入
		 * 
		 * 仅知道一个中线坐标点 以及每张牌显示宽度即可
		 */

		/**
		 * 另外从文件中读取每张牌的显示宽度 经过换算后赋值给displayWidth
		 */

		/**
		 * 急于测试 这里就在代码中直接赋值了 然后再换算
		 */
		outAreaPoint = new Point(0, 0);
		if (myTurn == 0) {
			outAreaPoint.x = 580;// 500;
			outAreaPoint.y = 100;// 80;
		} else if (myTurn == 1) {
			outAreaPoint.x = 360;// 360;
			outAreaPoint.y = 5;
		} else if (myTurn == 2) {
			outAreaPoint.x = 230;
			outAreaPoint.y = 120; // 160;
		} else {
			outAreaPoint.x = 400;
			outAreaPoint.y = 160; // 185;
		}

		int realX = (int) (((float) outAreaPoint.x * screenWidth) / ((float) Constants.SCREEN_WIDTH_DEFINED));
		int realY = (int) (((float) outAreaPoint.y * screenHeight) / ((float) Constants.SCREEN_HEIGHT_DEFINED));
		outAreaPoint.x = realX;
		outAreaPoint.y = realY;

		displayWidth_ForPoker = (int) (((float) screenWidth * Constants.DISPLAY_WIDTH_FOR_POKER_DEFINED) / ((float) Constants.SCREEN_WIDTH_DEFINED));

	}

	/**
	 * 以下是需要各个子类重载的函数: onDraw onTouchEvent lead someoneLead payTribute payBack
	 * receivePay pokerAssigned
	 */

	public void drawPokers(Canvas canvas) {
		/**
		 * 绘制打在桌面的牌堆即可 没有就不绘制
		 * 
		 * 涉及到多线程对链表pokerSprites_OnDesk的访问 需要加上同步锁
		 */

		spritesLock.lock();

		for (int i = 0; i < pokerSprites_OnDesk.size(); i++) {
			PokerSprite pkSprite = pokerSprites_OnDesk.get(i);
			if (pkSprite.isActive())
				pkSprite.onDraw(canvas);
		}

		if (tributePokerSprite != null)
			tributePokerSprite.onDraw(canvas);

		spritesLock.unlock();
	}

	public void onTouchEvent(MotionEvent event) {
		/**
		 * Player类的onTouchEvent不进行任何操作
		 */
	}

	public ArrayList<Poker> lead() {
		/**
		 * Player类的lead只返回null即可
		 * 
		 * 具体工作交由其子类来做
		 */
		return null;
	}

	/**
	 * 牌局恢复时还原桌上打出的牌
	 * 
	 * @param pkList
	 */
	public void restorePokerOndesk(ArrayList<Poker> pkList) {

		spritesLock.lock();

		if (pkList == null || pkList.size() == 0) {
			/**
			 * 显示不出字样 建立一个TextSprite或者MessageBox
			 */

		} else {
			/**
			 * 首先对pokerList进行排序 排序后成为便于观察符合习惯的组牌顺序
			 * 
			 * 这里调用了Helper的两个函数: 先调用Helper.getPokerType获取其牌型
			 * 然后根据牌型调用Helper.sortPokers将其排序
			 */
			/*
			 * 
			 * 不用排序了 现在在上层添加了对函数参数pkList的排序 也就是说参数传进来的时候已经排序好了 int grade =
			 * battleView.getCurrentGrade(); int pokerType =
			 * Helper.getPokerType(pkList, grade);
			 * 
			 * pkList = Helper.sortPokers_reverse(pkList, pokerType, grade);
			 */
			/**
			 * 建立相应的PokerSprite
			 */
			pokerSprites_OnDesk.clear();
			for (int i = 0; i < pkList.size(); i++) {
				PokerSprite pkSprite = new PokerSprite(pkList.get(i));
				pokerSprites_OnDesk.add(pkSprite);
			}

			/**
			 * 设置每张牌在牌局中的位置 根据outAreaCenter的值 以及displayWidth_ForPoker的值来计算
			 * 
			 * 注意: 10张牌的显示总宽度是: 9个displayWidth_ForPoker+1张牌的宽度
			 */
			int pokerNum = pkList.size();
			int pokerWidth = pokerSprites_OnDesk.get(0).getWidth();
			int allWidth = displayWidth_ForPoker * (pokerNum - 1) + pokerWidth;
			int posX = outAreaPoint.x - (allWidth / 2);
			int posY = outAreaPoint.y;
			for (int i = 0; i < pokerNum; i++) {
				PokerSprite pkSprite = pokerSprites_OnDesk.get(i);
				pkSprite.setPos(posX, posY);
				posX += displayWidth_ForPoker;
			}
		}

		/**
		 * 解锁
		 */
		spritesLock.unlock();

	}

	/**
	 * 牌局中每当玩家打出牌时的处理
	 * 
	 * @param pkList
	 * @param index
	 */
	public void someoneLead(ArrayList<Poker> pkList, int index) {
		/**
		 * 如果出牌的那个是自己 将牌显示到自己对应的出牌区
		 */
		if (index == myTurn) {
			/**
			 * 加锁
			 */
			spritesLock.lock();

			if (pkList == null || pkList.size() == 0) {
				/**
				 * 显示不出字样 建立一个TextSprite或者MessageBox
				 */
			} else {
				/**
				 * 首先对pokerList进行排序 排序后成为便于观察符合习惯的组牌顺序
				 * 
				 * 这里调用了Helper的两个函数: 先调用Helper.getPokerType获取其牌型
				 * 然后根据牌型调用Helper.sortPokers将其排序
				 */

				/**
				 * 建立相应的PokerSprite
				 */
				pokerSprites_OnDesk.clear();
				for (int i = 0; i < pkList.size(); i++) {
					PokerSprite pkSprite = new PokerSprite(pkList.get(i));
					pokerSprites_OnDesk.add(pkSprite);
				}

				/**
				 * 设置每张牌在牌局中的位置 根据outAreaCenter的值 以及displayWidth_ForPoker的值来计算
				 * 
				 * 注意: 10张牌的显示总宽度是: 9个displayWidth_ForPoker+1张牌的宽度
				 */
				int pokerNum = pkList.size();
				int pokerWidth = pokerSprites_OnDesk.get(0).getWidth();
				int allWidth = displayWidth_ForPoker * (pokerNum - 1)
						+ pokerWidth;
				int posX = outAreaPoint.x - (allWidth / 2);
				int posY = outAreaPoint.y;
				for (int i = 0; i < pokerNum; i++) {
					PokerSprite pkSprite = pokerSprites_OnDesk.get(i);
					pkSprite.setPos(posX, posY);
					posX += displayWidth_ForPoker;
				}
			}

			/**
			 * 解锁
			 */
			spritesLock.unlock();
		}
	}

	public Poker payTribute() {
		return null;
	}

	public Poker payBack() {
		return null;
	}

	public void Ipaid(Poker myTributePoker) {
		/**
		 * 将这张扑克牌显示到自己的桌面上
		 */
		spritesLock.lock();

		tributePokerSprite = new PokerSprite(myTributePoker);

		int pokerWidth = tributePokerSprite.getWidth();
		int posX = outAreaPoint.x - (pokerWidth / 2);
		int posY = outAreaPoint.y;
		tributePokerSprite.setPos(posX, posY);

		spritesLock.unlock();
	}

	public void receivePay(Poker poker) {
		/**
		 * 接到这张牌时 先不忙把它加入链表 而是先显示到桌面上
		 */
		pokerList.add(new Poker(poker.pattern, poker.points));

		/**
		 * 将这张扑克牌显示到自己的桌面上
		 */
		// spritesLock.lock();

		// paidPokerSprite = new PokerSprite(imageLoader,
		// screenWidth, screenHeight, poker);
		// //pokerSprites_OnDesk.add(pkSprite);
		// int pokerWidth = paidPokerSprite.getWidth();
		// int posX = outAreaPoint.x-(pokerWidth/2);
		// int posY = outAreaPoint.y;
		// paidPokerSprite.setPos(posX, posY);
		//
		// spritesLock.unlock();

	}

	public void pokerAssigned(ArrayList<Poker> pkList) {
		if (pokerList == null)
			pokerList = new LinkedList<Poker>();

		pokerList.clear();
		pokerList.addAll(pkList);
	}

	/**
	 * 
	 * 将上一次的出的牌在显示区域清空
	 */
	public void cleanMyDesk() {

		/**
		 * 加锁
		 */
		spritesLock.lock();

		/**
		 * 清空之前的显示区域
		 */
		if (pokerSprites_OnDesk.size() > 0) {
			for (int i = 0; i < pokerSprites_OnDesk.size(); i++) {
				PokerSprite pkSprite = pokerSprites_OnDesk.get(i);
				pkSprite.setActive(false);
			}
			pokerSprites_OnDesk.clear();
		}

		/**
		 * 解锁
		 */
		spritesLock.unlock();
	}

	/**
	 * 轮到自己出牌时调用
	 */
	public void itsMyTurn() {

	}

	public boolean isFinished() {
		return isFinished;
	}

	public boolean checkFinished() {
		if (pokerList == null)
			return true;
		else {
			if (pokerList.size() == 0) {
				isFinished = true;
			} else
				isFinished = false;

			return isFinished;
		}
	}

	public int getPokerNum(int pattern, int points) {
		int count = 0;

		for (int i = 0; i < pokerList.size(); i++) {
			Poker poker = pokerList.get(i);
			if (poker.pattern == pattern && poker.points == points)
				count++;
		}

		return count;
	}

	public ArrayList<Poker> IamLast() {
		isFinished = true;

		ArrayList<Poker> pokersLeft = new ArrayList<Poker>();

		while (pokerList.size() > 0)
			pokersLeft.add(pokerList.remove());

		return pokersLeft;
	}

	public int getGrade() {
		return myGrade;
	}

	public void setGrade(int grade) {
		myGrade = grade;
	}

	public int getTurn() {
		return myTurn;
	}

	public void setTurn(int turn) {
		myTurn = turn;
	}

	public void setFinished(boolean bFinished) {
		isFinished = bFinished;
	}

	public void inningEnd() {
		isFinished = true;

		spritesLock.lock();

		for (int i = 0; i < pokerSprites_OnDesk.size(); i++)
			pokerSprites_OnDesk.get(i).setActive(false);

		pokerSprites_OnDesk.clear();

		spritesLock.unlock();
	}

	public void inningBegin() {
		/**
		 * 该玩家给出的贡牌消失
		 */
		spritesLock.lock();

		if (tributePokerSprite != null) {
			tributePokerSprite.setActive(false);
			tributePokerSprite = null;
		}

		spritesLock.unlock();

		isFinished = false;
	}

	public int getPokerNum() {
		if (pokerList == null)
			return 0;
		else
			return pokerList.size();
	}

	public LinkedList<Poker> getPokers() {
		LinkedList<Poker> retList = new LinkedList<Poker>();
		retList.addAll(pokerList);

		return retList;
	}

}
