/**
 * UserPlayer.java
 * @author 胡裕靖
 * March 23rd 2011
 * 
 * UserPlayer是代表用户玩家
 * 就是手机的持有者,继承自Player类
 * 完成如下功能:
 * 对用户玩家牌堆和打出的牌的绘制;
 * 对牌堆上的触屏事件响应;
 * 对牌堆的各种排序操作;
 */
package guandan.player;

import java.util.ArrayList;

import guandan.constants.Constants;
import guandan.constants.PokerPattern;
import guandan.constants.PokerType;
import guandan.game.GameView;
import guandan.game.Poker;
import guandan.game.R;
import guandan.helper.Comparator;
import guandan.helper.Helper;
import guandan.helper.Parser;
import guandan.sprite.PokerSprite;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;

public class UserPlayer extends Player {

	// Fields//////////////////////////////////////////

	/**
	 * 玩家的牌堆分为两层,防止空间不够以及玩家的误操作 上面一层被下面一层盖住一部分(也可以不盖,取决于实际有多少空间)
	 */
	private ArrayList<PokerSprite> pokerSprites_Up;
	private ArrayList<PokerSprite> pokerSprites_Down;

	private int pokerHeapX;
	private int pokerHeapY_Up;
	private int pokerHeapY_Down;
	private int pokerHeapY_Up_ZoomIn;
	private int pokerHeapY_Down_ZoomIn;
	private int pokerHeapY_Up_ZoomOut;
	private int pokerHeapY_Down_ZoomOut;

	public static final int SORT_POKERS_AS_PATTERN = 0;
	public static final int SORT_POKERS_AS_POINTS = 1;
	public static final int SORT_POKERS_AS_TYPE = 2;
	private int sortWay = SORT_POKERS_AS_PATTERN;

	/**
	 * 为牌堆放大而增加的成员
	 */
	private boolean isZoomOut = false;
	private Bitmap pokerPanelBitmap;
	private Rect pokerPanelPos;

	// Methods/////////////////////////////////////////

	public UserPlayer(GameView gv, int turn) {
		super(gv, turn);

		pokerSprites_Up = new ArrayList<PokerSprite>();
		pokerSprites_Down = new ArrayList<PokerSprite>();

		/**
		 * 设置玩家牌堆的位置
		 */
		setPokerHeapArea();

		setPokerPanel();
	}

	private void setPokerHeapArea() {
		/**
		 * 读取文件对 pokerHeapX pokerHeapY_Up pokerHeapY_Down 进行计算即可
		 */

		/**
		 * 急于测试 直接在代码中赋值 再计算
		 */

		pokerHeapX = 400;// 360;
		pokerHeapY_Up_ZoomIn = 270;// 280;//315;
		pokerHeapY_Down_ZoomIn = 360;// 375;
		pokerHeapY_Up_ZoomOut = 100;
		pokerHeapY_Down_ZoomOut = 200;

		pokerHeapX = (int) (((float) 400 * screenWidth) / ((float) Constants.SCREEN_WIDTH_DEFINED));
		pokerHeapY_Up_ZoomIn = (int) (((float) 270 * screenHeight) / ((float) Constants.SCREEN_HEIGHT_DEFINED));
		pokerHeapY_Down_ZoomIn = (int) (((float) 360 * screenHeight) / ((float) Constants.SCREEN_HEIGHT_DEFINED));
		pokerHeapY_Up_ZoomOut = (int) (((float) 40 * screenHeight) / ((float) Constants.SCREEN_HEIGHT_DEFINED));
		pokerHeapY_Down_ZoomOut = (int) (((float) 200 * screenHeight) / ((float) Constants.SCREEN_HEIGHT_DEFINED));

		if (isZoomOut) {
			pokerHeapY_Down = pokerHeapY_Down_ZoomOut;
			pokerHeapY_Up = pokerHeapY_Up_ZoomOut;
		} else {
			pokerHeapY_Down = pokerHeapY_Down_ZoomIn;
			pokerHeapY_Up = pokerHeapY_Up_ZoomIn;
		}
	}

	private void setPokerPanel() {
		pokerPanelBitmap = BitmapFactory.decodeResource(
				gameView.getResources(), R.drawable.pokerpanel);

		int leftX = (int) (((float) 20 * screenWidth) / ((float) Constants.SCREEN_WIDTH_DEFINED));
		int rightX = (int) (((float) 700 * screenWidth) / ((float) Constants.SCREEN_WIDTH_DEFINED));
		int topY = (int) (((float) 60 * screenHeight) / ((float) Constants.SCREEN_HEIGHT_DEFINED));
		int bottomY = (int) (((float) 460 * screenHeight) / ((float) Constants.SCREEN_HEIGHT_DEFINED));

		pokerPanelPos = new Rect(leftX, topY, rightX, bottomY);
	}

	/**
	 * 以下是重写父类Player的函数: onDraw onTouchEvent lead someoneLead payTribute payBack
	 * receivePay pokerAssigned
	 */

	public void drawPokers(Canvas canvas) {

		/**
		 * 访问各个链表之前先加锁 因为涉及到多个线程对链表的访问
		 */
		spritesLock.lock();

		/**
		 * 画当前打出的牌
		 */
		for (int i = 0; i < pokerSprites_OnDesk.size(); i++) {
			PokerSprite pkSprite = pokerSprites_OnDesk.get(i);
			if (pkSprite.isActive())
				pkSprite.onDraw(canvas);
		}
		if (tributePokerSprite != null)
			tributePokerSprite.onDraw(canvas);

		/**
		 * 现在分放大和非放大情况
		 */
		if (isZoomOut) {
			canvas.drawBitmap(pokerPanelBitmap,
					new Rect(0, 0, pokerPanelBitmap.getWidth(),
							pokerPanelBitmap.getHeight()), pokerPanelPos, null);
		}

		/**
		 * 画玩家牌堆 画牌堆时先上后下,先左后右
		 */
		for (int i = 0; i < pokerSprites_Up.size(); i++) {
			PokerSprite pkSprite = pokerSprites_Up.get(i);
			if (pkSprite.isActive())
				pkSprite.onDraw(canvas);
		}
		for (int i = 0; i < pokerSprites_Down.size(); i++) {
			PokerSprite pkSprite = pokerSprites_Down.get(i);
			if (pkSprite.isActive())
				pkSprite.onDraw(canvas);
		}

		/**
		 * 最后解锁
		 */
		spritesLock.unlock();
	}

	public void onTouchEvent(MotionEvent event) {
		int eventType = event.getAction();
		int eventPosX = (int) event.getX();
		int eventPosY = (int) event.getY();

		if (eventType == MotionEvent.ACTION_DOWN
				|| eventType == MotionEvent.ACTION_MOVE
				|| eventType == MotionEvent.ACTION_UP) {
			/**
			 * 触屏响应的时候先下后上,先右后左
			 * 
			 * 关键的地方是: 找到手指触碰到的那张牌 因为手指触碰的地方很可能是多张牌都重叠在那儿 所以应该找到重叠的牌中最上面那一张
			 * 
			 * 根据牌堆下盖上,右盖左的前提,如下处理即可
			 */
			PokerSprite rightPokerSprite = null;
			for (int i = pokerSprites_Down.size() - 1; i >= 0; i--) {
				PokerSprite pkSprite = pokerSprites_Down.get(i);
				if (pkSprite.isActive()) {
					if (pkSprite.isInside(eventPosX, eventPosY)) {
						if (rightPokerSprite == null)
							rightPokerSprite = pkSprite;
						else
							pkSprite.setSelected(false);
					} else
						pkSprite.setSelected(false);
				}
			}
			for (int i = pokerSprites_Up.size() - 1; i >= 0; i--) {
				PokerSprite pkSprite = pokerSprites_Up.get(i);
				if (pkSprite.isActive()) {
					if (pkSprite.isInside(eventPosX, eventPosY)) {
						if (rightPokerSprite == null)
							rightPokerSprite = pkSprite;
						else
							pkSprite.setSelected(false);
					} else
						pkSprite.setSelected(false);
				}
			}
			/**
			 * 对手指选中的那张牌再进行具体处理
			 */
			if (rightPokerSprite != null) {
				/**
				 * 考虑到方便用户操作,加入根据当前牌型来弹出牌的功能
				 * 
				 * 只处理: 对子,三张,炸弹(保留) 顺子,同花顺,三代二,三连对,钢板等牌型不考虑(一来复杂,二来这样对玩家来说有点无趣)
				 * 同时不考虑配牌
				 * 
				 * 这种功能触发的条件是: 当前牌堆中并没有其他牌弹出; 并且对当前选中的牌的操作是令其弹出的操作;
				 * 弹出的牌能够比当前牌要大;
				 */
				boolean popped_BeforeTouch = rightPokerSprite.isPopped();

				rightPokerSprite.onTouchEvent(event);

				boolean popped_AfterTouch = rightPokerSprite.isPopped();

				if (!popped_BeforeTouch && popped_AfterTouch
						&& getPoppedPokers().size() == 1) {
					helpPlayerPop(rightPokerSprite.getPoker());
				}

			}
		}
	}

	/**
	 * 对滑屏操作的支持 当用户快速横向滑动时,弹出或者缩回其手指经过的扑克牌
	 * 
	 * @param event1
	 *            :类型为ACTION_DOWN的MotionEvent
	 * @param event2
	 *            :类型为ACTION_UP的MotionEvent
	 * @param velocityX
	 *            :滑动时的X方向的速度
	 * @param velocityY
	 *            :滑动时的Y方向的速度
	 */
	public void onFling(MotionEvent event1, MotionEvent event2,
			float velocityX, float velocityY) {
		/**
		 * X方向上滑动太慢 或者滑动前后竖直方向上偏差太大 将不作处理
		 */
		int posX1 = (int) event1.getX();
		int posY1 = (int) event1.getY();
		int posX2 = (int) event2.getX();
		int posY2 = (int) event2.getY();
		int minX = posX1;
		int maxX = posX2;
		int meanY = (posY1 + posY2) / 2;
		if (posX1 > posX2) {
			minX = posX2;
			maxX = posX1;
		}

		ArrayList<PokerSprite> pkSprites_Slide = new ArrayList<PokerSprite>();
		PokerSprite pkSprite_OnRelease = null;

		if (Math.abs(velocityX) < 1 || Math.abs(posY1 - posY2) >= 30) {
			Log.i("Gesture Support", "Invalid Gesture");
		} else {
			/**
			 * 首先遍历pokerSprites_Down和pokerSprites_Up 找到手指滑过手机屏幕时所经过的那些扑克牌
			 */
			for (int i = pokerSprites_Down.size() - 1; i >= 0; i--) {
				PokerSprite pkSprite = pokerSprites_Down.get(i);
				Rect posRect = pkSprite.getPosRect();
				int centerX = posRect.centerX();
				if (pkSprite.isActive()) {
					if (posRect.right > minX && posRect.left < maxX
							&& posRect.top < meanY && posRect.bottom > meanY
							&& centerX >= minX) {
						pkSprites_Slide.add(pkSprite);
					}

					if (pkSprite.isInside(posX2, posY2)
							&& pkSprite_OnRelease == null) {
						pkSprite_OnRelease = pkSprite;
					}
				}
			}
			if (pkSprites_Slide.size() == 0) {
				for (int i = pokerSprites_Up.size() - 1; i >= 0; i--) {
					PokerSprite pkSprite = pokerSprites_Up.get(i);
					Rect posRect = pkSprite.getPosRect();
					int centerX = posRect.centerX();
					if (pkSprite.isActive()) {
						if (posRect.right > minX && posRect.left < maxX
								&& posRect.top < meanY
								&& posRect.bottom > meanY && centerX >= minX) {
							pkSprites_Slide.add(pkSprite);
						}

						if (pkSprite.isInside(posX2, posY2)
								&& pkSprite_OnRelease == null) {
							pkSprite_OnRelease = pkSprite;
						}
					}
				}
			}

			/**
			 * 改变那些被手指滑过的扑克牌的状态,即弹起或者缩回 注意对指尖释放位置上的那张扑克牌做特殊处理
			 */
			if (pkSprites_Slide.size() > 0) {
				for (int i = 0; i < pkSprites_Slide.size(); i++) {
					PokerSprite pkSprite = pkSprites_Slide.get(i);
					if (pkSprite.isPopped()) {
						pkSprite.retract();

						Log.i("Gesture Test", "Retract");
					} else {
						pkSprite.pop();

						Log.i("Gesture Test", "Pop");
					}
				}

				if (pkSprite_OnRelease != null) {
					if (pkSprite_OnRelease.isPopped())
						pkSprite_OnRelease.retract();
					else
						pkSprite_OnRelease.pop();
				}
			}
		}

	}

	/**
	 * 玩家弹出某张牌后 根据牌局当前牌的牌型 帮助玩家弹出其他若干张牌,凑成对应牌型
	 * 
	 * @param poker
	 *            :被玩家弹出的那张牌
	 */
	private void helpPlayerPop(Poker poker) {
		ArrayList<Poker> currentPokers = gameView.getCurrentPokers();
		int currentWinner = gameView.getCurrentWinner();
		int currentGrade = gameView.getCurrentGrade();

		if (currentWinner == 3 || currentPokers == null
				|| currentPokers.size() == 1)
			return;

		int currentType = Parser.getPokerType(currentPokers, currentGrade)[0];
		ArrayList<Poker> samePokers = new ArrayList<Poker>();
		ArrayList<Poker> otherPokers = new ArrayList<Poker>();
		ArrayList<PokerSprite> pokerSpritesHeap = new ArrayList<PokerSprite>();
		pokerSpritesHeap.addAll(pokerSprites_Up);
		pokerSpritesHeap.addAll(pokerSprites_Down);

		int result = Comparator.comparePoker(poker, currentPokers.get(0),
				currentGrade);

		if (result != 1
				&& (currentType == PokerType.DOUBLE || currentType == PokerType.TRIPLE))
			return;

		for (int i = 0; i < pokerSpritesHeap.size(); i++) {
			PokerSprite pkSprite = pokerSpritesHeap.get(i);
			Poker pk = pkSprite.getPoker();
			if (!pkSprite.isPopped()
					&& Comparator.comparePoker(poker, pk, currentGrade) == 0) {
				samePokers.add(pk);
			}
		}

		if (currentType == PokerType.DOUBLE) {
			if (samePokers.size() < 1)
				return;
			else
				otherPokers.add(samePokers.get(0));
		} else if (currentType == PokerType.TRIPLE) {
			if (samePokers.size() < 2)
				return;
			else {
				otherPokers.add(samePokers.get(0));
				otherPokers.add(samePokers.get(1));
			}
		} else if (currentType == PokerType.BOMB) {
			if (samePokers.size() < currentPokers.size() - 1)
				return;
			else {
				if (result == 1) {
					for (int i = 0; i < currentPokers.size() - 1; i++)
						otherPokers.add(samePokers.get(i));
				} else if (samePokers.size() == currentPokers.size() - 1) {
					return;
				} else {
					for (int i = 0; i < currentPokers.size(); i++)
						otherPokers.add(samePokers.get(i));
				}
			}
		} else
			return;

		/**
		 * 相应的扑克牌弹起
		 */
		for (int i = 0; i < pokerSpritesHeap.size(); i++) {
			PokerSprite pkSprite = pokerSpritesHeap.get(i);

			if (pkSprite.isPopped())
				continue;

			Poker pk = pkSprite.getPoker();

			for (int j = 0; j < otherPokers.size(); j++) {
				if (pk.pattern == otherPokers.get(j).pattern
						&& pk.points == otherPokers.get(j).points) {
					pkSprite.pop();
					otherPokers.remove(j);
					break;
				}
			}
		}
	}

	/**
	 * 将当前弹出的牌打出 重写父类Player的函数lead
	 */
	public ArrayList<Poker> lead() {

		/**
		 * 需要对链表加锁 涉及到多线程同步问题
		 */
		spritesLock.lock();

		ArrayList<Poker> poppedPokers = new ArrayList<Poker>();

		for (int i = 0; i < pokerSprites_Up.size(); i++) {
			PokerSprite pkSprite = pokerSprites_Up.get(i);
			if (pkSprite.isActive() && pkSprite.isPopped()) {
				poppedPokers.add(pkSprite.getPoker());
				pkSprite.setActive(false);

				pokerSprites_Up.remove(i);
				i--;
			}
		}
		for (int i = 0; i < pokerSprites_Down.size(); i++) {
			PokerSprite pkSprite = pokerSprites_Down.get(i);
			if (pkSprite.isActive() && pkSprite.isPopped()) {
				poppedPokers.add(pkSprite.getPoker());
				pkSprite.setActive(false);

				pokerSprites_Down.remove(i);
				i--;
			}
		}

		/**
		 * 解锁
		 */
		spritesLock.unlock();

		/**
		 * 对应的pokerList中的对象也要删除
		 */
		for (int i = 0; i < poppedPokers.size(); i++) {
			Poker poppedPoker = poppedPokers.get(i);

			for (int j = 0; j < pokerList.size(); j++) {
				Poker pk = pokerList.get(j);

				if (poppedPoker.pattern == pk.pattern
						&& poppedPoker.points == pk.points) {
					pokerList.remove(j);
					break;
				}
			}
		}

		/**
		 * 最后重新理一下牌堆 包括: pokerList 各个PokerSprite对象
		 */
		sortPokers(sortWay);

		return poppedPokers;
	}

	public void someoneLead(ArrayList<Poker> pokerList, int index) {
		/**
		 * UserPlayer直接调用父类的someoneLead即可
		 */
		super.someoneLead(pokerList, index);
	}

	public Poker payTribute() {

		/**
		 * 涉及到对链表的修改操作 要加锁
		 */
		spritesLock.lock();

		Poker returnPoker = null;
		for (int i = 0; i < pokerSprites_Up.size(); i++) {
			PokerSprite pkSprite = pokerSprites_Up.get(i);
			if (pkSprite.isActive() && pkSprite.isPopped()) {
				returnPoker = pkSprite.getPoker();
				pkSprite.setActive(false);

				/**
				 * 删掉那张牌
				 */
				pokerSprites_Up.remove(i);
				i--;

				break;
			}
		}
		if (returnPoker == null) {
			for (int i = 0; i < pokerSprites_Down.size(); i++) {
				PokerSprite pkSprite = pokerSprites_Down.get(i);
				if (pkSprite.isActive() && pkSprite.isPopped()) {
					returnPoker = pkSprite.getPoker();
					pkSprite.setActive(false);

					/**
					 * 删掉那张牌
					 */
					pokerSprites_Down.remove(i);
					i--;

					break;
				}
			}
		}

		/**
		 * 解锁
		 */
		spritesLock.unlock();

		/**
		 * 对应的pokerList中的对象也要删除
		 */
		for (int i = 0; i < pokerList.size(); i++) {
			Poker pk = pokerList.get(i);

			if (returnPoker.pattern == pk.pattern
					&& returnPoker.points == pk.points) {
				pokerList.remove(i);
				break;
			}
		}

		/**
		 * 重新理一下牌堆
		 */
		sortPokers(sortWay);

		return returnPoker;
	}

	public Poker payBack() {
		return payTribute();
	}

	public void receivePay(Poker poker) {
		/**
		 * 先调用父类Player的receivePay 修改pokerList链表
		 */
		super.receivePay(poker);

		/**
		 * 涉及对链表的修改 需要加锁
		 */
		spritesLock.lock();

		PokerSprite pkSprite = new PokerSprite(poker);

		/**
		 * 怎么来让玩家知道这是一张进贡或回贡过来的牌呢
		 */
		pokerSprites_Down.add(pkSprite);

		/**
		 * 解锁
		 */
		spritesLock.unlock();

		/**
		 * 对牌堆重新排序
		 */
		sortPokers(sortWay);

		/**
		 * 弹出那张牌 表示这张牌是别人进贡或回贡来的
		 */
		boolean go_on = true;
		for (int i = 0; i < pokerSprites_Up.size(); i++) {
			PokerSprite pkSp = pokerSprites_Up.get(i);
			Poker pk = pkSp.getPoker();

			if (poker.pattern == pk.pattern && poker.points == pk.points) {
				pkSp.pop();
				go_on = false;
				break;
			}
		}
		if (go_on) {
			for (int i = 0; i < pokerSprites_Down.size(); i++) {
				PokerSprite pkSp = pokerSprites_Down.get(i);
				Poker pk = pkSp.getPoker();

				if (poker.pattern == pk.pattern && poker.points == pk.points) {
					pkSp.pop();
					break;
				}
			}
		}

	}

	public void pokerAssigned(ArrayList<Poker> pkList) {
		/**
		 * 先调用父类pokerAssigned 设置成员变量pokerList
		 */
		super.pokerAssigned(pkList);

		/**
		 * 建立相应的PokerSprite对象
		 */
		spritesLock.lock();

		pokerSprites_Down.clear();
		pokerSprites_Up.clear();

		for (int i = 0; i < pokerList.size(); i++) {
			Poker pk = pokerList.get(i);
			PokerSprite pkSprite = new PokerSprite(pk);
			pokerSprites_Up.add(pkSprite);
		}

		spritesLock.unlock();

		/**
		 * 排序并显示到相应区域
		 */
		sortPokers(SORT_POKERS_AS_POINTS);
	}

	/**
	 * 获取从牌堆弹出的那些牌
	 * 
	 * @return:Poker对象的链表
	 */
	public ArrayList<Poker> getPoppedPokers() {
		ArrayList<Poker> returnList = new ArrayList<Poker>();

		for (int i = 0; i < pokerSprites_Up.size(); i++) {
			PokerSprite pkSprite = pokerSprites_Up.get(i);
			if (pkSprite.isActive() && pkSprite.isPopped()) {
				returnList.add(pkSprite.getPoker());
			}
		}
		for (int i = 0; i < pokerSprites_Down.size(); i++) {
			PokerSprite pkSprite = pokerSprites_Down.get(i);
			if (pkSprite.isActive() && pkSprite.isPopped()) {
				returnList.add(pkSprite.getPoker());
			}
		}

		return returnList;
	}

	/**
	 * 当牌局"重置"按钮按下时被调用
	 */
	public void resetPokers() {
		/**
		 * 不涉及对链表的修改 不加锁
		 */

		for (int i = 0; i < pokerSprites_Up.size(); i++) {
			PokerSprite pkSprite = pokerSprites_Up.get(i);
			if (pkSprite.isActive() && pkSprite.isPopped())
				pkSprite.retract();
		}
		for (int i = 0; i < pokerSprites_Down.size(); i++) {
			PokerSprite pkSprite = pokerSprites_Down.get(i);
			if (pkSprite.isActive() && pkSprite.isPopped())
				pkSprite.retract();
		}
	}

	/**
	 * 当牌局中某个牌堆整理按钮被按下时调用
	 * 
	 * @param way
	 *            :整理牌堆的方式,分"花色","点数","牌型"三种
	 */
	public void sortPokers(int way) {
		sortWay = way;

		/**
		 * 排序只排poker对象 牌号pokerList中的poker对象之后 调整牌堆的位置
		 */
		switch (sortWay) {
		case SORT_POKERS_AS_PATTERN:
			sortPokers_AsPattern();
			break;
		case SORT_POKERS_AS_POINTS:
			sortPokers_AsPoints();
			break;
		case SORT_POKERS_AS_TYPE:
			sortPokers_AsType();
			break;
		default:
			break;
		}

		/**
		 * 调用setPokerHeapLayout
		 */
		setPokerHeapLayout();
	}

	private void sortPokers_AsPattern() {
		/**
		 * 按照花色排序 排序后在pokerList中从左到右是 鬼牌,黑桃,红桃,方块,梅花
		 * 
		 * 而每种花色内部又按照大小排序
		 * 
		 * 鬼牌始终放最前面 考虑牌级
		 */
		ArrayList<Poker> spadePokerList = new ArrayList<Poker>();
		ArrayList<Poker> heartPokerList = new ArrayList<Poker>();
		ArrayList<Poker> squarePokerList = new ArrayList<Poker>();
		ArrayList<Poker> clubPokerList = new ArrayList<Poker>();
		ArrayList<Poker> jokerList = new ArrayList<Poker>();

		/**
		 * 先按点数排序
		 */
		sortPokers_AsPoints();

		/**
		 * 再分花色处理
		 */
		for (int i = 0; i < pokerList.size(); i++) {
			Poker pk = pokerList.get(i);

			if (pk.pattern == PokerPattern.CLUB)
				clubPokerList.add(pk);
			else if (pk.pattern == PokerPattern.SQUARE)
				squarePokerList.add(pk);
			else if (pk.pattern == PokerPattern.HEART)
				heartPokerList.add(pk);
			else if (pk.pattern == PokerPattern.SPADE)
				spadePokerList.add(pk);
			else
				jokerList.add(pk);
		}

		pokerList.clear();
		pokerList.addAll(jokerList);
		pokerList.addAll(spadePokerList);
		pokerList.addAll(heartPokerList);
		pokerList.addAll(squarePokerList);
		pokerList.addAll(clubPokerList);
	}

	private void sortPokers_AsPoints() {
		/**
		 * 从前到后按点数排序即可 可调用Helper.comparePoker函数
		 * 
		 * 鬼牌以及主牌都会放到pokerList最前面
		 * 
		 */

		int currentGrade = gameView.getCurrentGrade();
		for (int i = 0; i < pokerList.size(); i++) {
			int maxIndex = i;
			for (int j = i + 1; j < pokerList.size(); j++) {
				if (Comparator.comparePoker(pokerList.get(j),
						pokerList.get(maxIndex), currentGrade) == 1) {
					maxIndex = j;
				}
			}
			if (maxIndex != i) {
				Poker poker_i = pokerList.get(i);
				Poker poker_max = pokerList.get(maxIndex);
				Poker tempPoker = new Poker(poker_i.pattern, poker_i.points);
				poker_i.pattern = poker_max.pattern;
				poker_i.points = poker_max.points;
				poker_max.pattern = tempPoker.pattern;
				poker_max.points = tempPoker.points;
			}
		}
	}

	private void sortPokers_AsType() {
		/**
		 * 按照每种点数的牌的张数来分
		 * 
		 * 排好序之后从前到后显示顺序是: 炸弹,三张,两张,单张(最多8张一样的) 而每种牌型内部又按照点数排序
		 * 
		 * 不考虑顺子,钢板,三带二,三连对等 因为这些牌型涉及到对其他牌型的拆分 排出来反而更乱
		 */
		int currentGrade = gameView.getCurrentGrade();
		ArrayList<Poker>[] pokerLists_AsNum = new ArrayList[8];
		for (int i = 0; i < 8; i++)
			pokerLists_AsNum[i] = new ArrayList<Poker>();

		/**
		 * 先按照点数排序
		 */
		sortPokers_AsPoints();

		/**
		 * 然后将每一种点数的牌抽出来 放到对应的类别中
		 */
		int current;
		int next;
		for (current = 0; current < pokerList.size(); current = next) {
			ArrayList<Poker> currentList = new ArrayList<Poker>();
			Poker currentPoker = pokerList.get(current);
			currentList.add(currentPoker);

			next = current + 1;
			for (; next < pokerList.size(); next++) {
				Poker nextPoker = pokerList.get(next);

				if (Comparator.comparePoker(currentPoker, nextPoker,
						currentGrade) == 0)
					currentList.add(nextPoker);
				else
					break;
			}

			/**
			 * 此时currentList中存放的是点数和currentPoker
			 * 相同的若干张牌,可以根据其长度加入到pokerLists_AsNum 对应的位置
			 */
			int listSize = currentList.size();
			pokerLists_AsNum[listSize - 1].addAll(currentList);
		}

		/**
		 * 最后修改pokerList链表
		 */
		pokerList.clear();
		for (int i = 7; i >= 0; i--)
			pokerList.addAll(pokerLists_AsNum[i]);
	}

	private void setPokerHeapLayout() {
		/**
		 * 没有牌了则无需进行下去
		 */
		if (pokerList == null || pokerList.size() == 0)
			return;

		/**
		 * 有个这样的想法 pokerSprites_Up中的最后一张牌 和pokerSprites_Down中的最后一张牌
		 * 在排好序的pokerList中是相连的
		 * 
		 * 这样在视觉上一排和下一排是连续的
		 */

		/**
		 * 加锁
		 */
		spritesLock.lock();

		ArrayList<PokerSprite> pokerSpritesHeap = new ArrayList<PokerSprite>();
		ArrayList<Poker> poppedPokers = new ArrayList<Poker>();

		pokerSpritesHeap.addAll(pokerSprites_Up);
		pokerSpritesHeap.addAll(pokerSprites_Down);
		pokerSprites_Up.clear();
		pokerSprites_Down.clear();

		for (int i = 0; i < pokerSpritesHeap.size(); i++) {
			PokerSprite pkSprite = pokerSpritesHeap.get(i);
			if (pkSprite.isPopped()) {
				pkSprite.retract(); // 这一步是否必要

				Poker pk = pkSprite.getPoker();
				Poker poppedPoker = new Poker(pk.pattern, pk.points);
				poppedPokers.add(poppedPoker);
			}
		}

		for (int i = 0; i < pokerList.size(); i++) {
			Poker pk = pokerList.get(i);
			pokerSpritesHeap.get(i).setPoker(pk);
		}

		/**
		 * 然后分上下堆
		 * 
		 * 超过14则分2堆 未超过14可以就成1堆
		 */
		int pokerNum = pokerList.size();
		if (pokerNum > 14) {
			int downSize = pokerNum / 2;
			int upSize = pokerNum - downSize;

			for (int i = 0; i < upSize; i++)
				pokerSprites_Up.add(pokerSpritesHeap.get(i));
			for (int i = pokerNum - 1; i >= upSize; i--)
				pokerSprites_Down.add(pokerSpritesHeap.get(i));
		} else
			pokerSprites_Up.addAll(pokerSpritesHeap);

		/**
		 * 设置牌堆中每张牌的位置
		 */
		if (isZoomOut) {
			pokerHeapY_Down = pokerHeapY_Down_ZoomOut;
			pokerHeapY_Up = pokerHeapY_Up_ZoomOut;
		} else {
			pokerHeapY_Down = pokerHeapY_Down_ZoomIn;
			pokerHeapY_Up = pokerHeapY_Up_ZoomIn;
		}

		int pokerWidth = pokerSpritesHeap.get(0).getWidth();
		int allWidth_Up = displayWidth_ForPoker * (pokerSprites_Up.size() - 1)
				+ pokerWidth;
		int allWidth_Down = displayWidth_ForPoker
				* (pokerSprites_Down.size() - 1) + pokerWidth;
		int posX_Up = pokerHeapX - (allWidth_Up / 2);
		int posX_Down = pokerHeapX - (allWidth_Down / 2);
		for (int i = 0; i < pokerSprites_Up.size(); i++) {
			PokerSprite pkSprite = pokerSprites_Up.get(i);
			pkSprite.setPos(posX_Up, pokerHeapY_Up);
			posX_Up += displayWidth_ForPoker;

		}
		for (int i = 0; i < pokerSprites_Down.size(); i++) {
			PokerSprite pkSprite = pokerSprites_Down.get(i);
			pkSprite.setPos(posX_Down, pokerHeapY_Down);
			posX_Down += displayWidth_ForPoker;

		}

		/**
		 * 原本弹出的牌还设置为弹出
		 */
		for (int i = 0; i < pokerSpritesHeap.size(); i++) {
			PokerSprite pkSprite = pokerSpritesHeap.get(i);
			Poker pk = pkSprite.getPoker();

			for (int j = 0; j < poppedPokers.size(); j++) {
				Poker poppedPoker = poppedPokers.get(j);
				if (pk.pattern == poppedPoker.pattern
						&& pk.points == poppedPoker.points) {
					pkSprite.pop();
					poppedPokers.remove(j);
					break;
				}
			}
		}

		/**
		 * 解锁
		 */
		spritesLock.unlock();
	}

	public void inningEnd() {
		isFinished = true;

		spritesLock.lock();

		for (int i = 0; i < pokerSprites_OnDesk.size(); i++)
			pokerSprites_OnDesk.get(i).setActive(false);
		for (int i = 0; i < pokerSprites_Down.size(); i++)
			pokerSprites_Down.get(i).setActive(false);
		for (int i = 0; i < pokerSprites_Up.size(); i++)
			pokerSprites_Up.get(i).setActive(false);

		pokerSprites_OnDesk.clear();
		pokerSprites_Up.clear();
		pokerSprites_Down.clear();

		spritesLock.unlock();
	}

	// public void inningBegin()
	// {
	//
	// boolean receivedPay = false;
	// spritesLock.lock();
	//
	//
	// if(paidPokerSprite != null)
	// {
	// receivedPay = true;
	//
	// //问题就在这里
	// //pokerList中的对象不能和pokerSprites中的对象指向一样
	// Poker pk = paidPokerSprite.getPoker();
	// pokerList.add(new Poker(pk.pattern, pk.points));
	// //pokerList.add(paidPokerSprite.getPoker());
	//
	// pokerSprites_Down.add(paidPokerSprite);
	//
	//
	// paidPokerSprite = null;
	// }
	//
	// spritesLock.unlock();
	//
	// if(receivedPay)
	// sortPokers(sortWay);
	//
	//
	// isFinished = false;
	// }

	/**
	 * 获得除主牌红桃外的最大的一张牌
	 */
	public Poker maxPoker_ExceptMasterCard() {
		int currentGrade = gameView.getCurrentGrade();
		int maxIndex = 0;

		while (maxIndex < pokerList.size()) {
			Poker maxPoker = pokerList.get(maxIndex);
			if (!Helper.isMasterCard(maxPoker, currentGrade)) {
				break;
			}
			maxIndex++;
		}
		for (int i = 1; i < pokerList.size(); i++) {

			Poker pk = pokerList.get(i);

			if (Helper.isMasterCard(pk, currentGrade))
				continue;
			else {
				Poker maxPoker = pokerList.get(maxIndex);

				if (Comparator.comparePoker(pk, maxPoker, currentGrade) == 1)
					maxIndex = i;
			}
		}

		return new Poker(pokerList.get(maxIndex).pattern,
				pokerList.get(maxIndex).points);
	}

	public boolean isZoomOut() {
		return isZoomOut;
	}

	public void zoomOut() {
		isZoomOut = true;

		/**
		 * 先将每张扑克牌放大
		 */
		spritesLock.lock();
		displayWidth_ForPoker = (int) (((float) screenWidth * 30) / ((float) Constants.SCREEN_WIDTH_DEFINED));
		for (int i = 0; i < pokerSprites_Down.size(); i++) {
			pokerSprites_Down.get(i).zoomOut();
		}
		for (int i = 0; i < pokerSprites_Up.size(); i++) {
			pokerSprites_Up.get(i).zoomOut();
		}
		spritesLock.unlock();

		/**
		 * 再重新设置牌堆位置
		 */
		setPokerHeapLayout();
	}

	public void zoomIn() {
		isZoomOut = false;

		/**
		 * 先将每张扑克牌缩小
		 */
		spritesLock.lock();
		displayWidth_ForPoker = (int) (((float) screenWidth * 25) / ((float) Constants.SCREEN_WIDTH_DEFINED));
		for (int i = 0; i < pokerSprites_Down.size(); i++) {
			pokerSprites_Down.get(i).zoomIn();
		}
		for (int i = 0; i < pokerSprites_Up.size(); i++) {
			pokerSprites_Up.get(i).zoomIn();
		}
		spritesLock.unlock();

		/**
		 * 再重新设置牌堆位置
		 */
		setPokerHeapLayout();
	}
}
