package guandan.game;

import guandan.audio.AudioManager;
import guandan.player.Player;
import guandan.player.UserPlayer;
import guandan.sprite.AnimationSprite;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import guandan.constants.GameState;
import guandan.game.GameActivity;

import guandan.constants.*;
import guandan.helper.*;

import guandan.constants.AIType;
import guandan.constants.PlayerWarning;
import guandan.player.Easy_AI_Player;
import guandan.player.Normal_AI_Player;

import guandan.audio.SoundsName;
import guandan.constants.Constants;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.view.View.OnTouchListener;

public class GameView extends SurfaceView implements SurfaceHolder.Callback,
		OnGestureListener, OnTouchListener {

	/**
	 * �ԶԾֽ��漸��״̬�Ķ���
	 */
	private static final int STATE_IN_BATTLE = 0;
	private static final int STATE_INNING_END = 1;
	private static final int STATE_ROUND_END = 2;
	private static final int STATE_PLAYER_PAY = 3;
	private static final int STATE_PLAYER_BACK = 4;

	/**
	 * ��������:�ȴ��Ծֿ�ʼ״̬ ���״̬����ҿ��Զ��ƽ��й۲� ��������ʾ"��ʼ"��ť ��ҵ��"��ʼ"��ť��,һ�ֲſ�ʼ
	 */
	private static final int STATE_WAITING_INNING_BEGIN = 5;

	private static final String ACTIVITY_TAG = "======== bingo debug =========";

	private int viewState;

	public int getViewState() {
		return viewState;
	}

	public void setViewState(int viewState) {
		this.viewState = viewState;
	}

	private static final int USER_PLAYER_TURN = 3;
	private int currentTurn;

	/**
	 * ѡ�е�NPCͷ��ͼƬ��û����PhotoSprite��ֱ����ͼ
	 */
	private Bitmap[] npcPhotoBitmaps;

	/**
	 * NPCͷ��ͼƬ��λ������
	 */
	Point[] npcPhotoPositions;

	// ������^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
	/**
	 * �ƾ��е�108����
	 */
	private ArrayList<Poker> allPokers;

	public ArrayList<Poker> getAllPokers() {
		return allPokers;
	}

	public void setAllPokers(ArrayList<Poker> allPokers) {
		this.allPokers = allPokers;
	}

	/**
	 * ����������һȦ�������
	 */
	private ArrayList<Poker>[] lastPokers;

	public ArrayList<Poker>[] getLastPokers() {
		return lastPokers;
	}

	public void setLastPokers(ArrayList<Poker>[] lastPokers) {
		this.lastPokers = lastPokers;
	}

	/**
	 * ��ǰ��Ҫ���µ��˿���
	 */
	private ArrayList<Poker> currentPokers;

	public void setCurrentPokers(ArrayList<Poker> currentPokers) {
		this.currentPokers = currentPokers;
	}

	/**
	 * ��ǰ��Ҫ���˽��µ��Ǹ���� ��currentPokers��Ӧ
	 */
	private int currentWinner;

	/**
	 * һ��֮�и�����ҵ�����
	 */
	private int[] orderRecord;

	public void setOrderRecord(int[] orderRecord) {
		this.orderRecord = orderRecord;
	}

	/**
	 * һ��֮��������¼���ڼ�����
	 */
	private int recordIndex;

	/**
	 * �ƾ��е�����ͼ��
	 */
	private Bitmap desktopImage;

	/**
	 * �ƾ��еĶ����ӻ��� ����ͬ����
	 */
	private ArrayList<AnimationSprite> animationSpriteList;
	private ReentrantLock animationSpriteListLock;

	/**
	 * ��Ļ�ĸ߶ȺͿ���
	 */
	private int screenWidth;
	private int screenHeight;

	private ImageLoader imageLoader;

	/**
	 * add by ������
	 */
	private AudioManager audioManager;

	/**
	 * ����ƾ��е��ĸ���Ҷ��� ������Ԫ�ظ���Ϊ4 �������һ��һ����UserPlayer����,�����û�����
	 */
	private Player[] players;
	private String[] playerNames;
	public String[] getPlayerNames() {
		return playerNames;
	}

	public void setPlayerNames(String[] playerNames) {
		this.playerNames = playerNames;
	}

	private UserPlayer userPlayer;

	/**
	 * �ƾ��߼���س�Ա ��ǰ�ƾֵ��Ƽ� ���һ�����Ƽ� ��Ҷ���һ�����Ƽ�
	 */
	private int currentGrade;
	private int playerGrade;
	private int opponentGrade;

	/**
	 * add by ��ԣ�� �ƾ�һ�ֵĽ��
	 */
	private int roundValue;

	/**
	 * add by ��ԣ��
	 */
	private Poker[] tributePokers;

	public Poker[] getTributePokers() {
		return tributePokers;
	}

	public void setTributePokers(Poker[] tributePokers) {
		this.tributePokers = tributePokers;
	}

	/**
	 * add by ��ԣ��
	 */
	private boolean roundFinished;

	private GestureDetector gd;

	/**
	 * ������Ա**************************************** ��ĩ����Ի��� ��ĩ����Ի���
	 */
	private InningEndDialog inningEndDialog;

	private static final int MESS_INNING_END = 0;
	private static final int MESS_ROUND_END = 1;
	private static final int MESS_BEFORE_INNING_BEGIN = 2;
	private static final int MESS_NEXT_TURN = 3;
	private Handler mHandler = new Handler() {

		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESS_INNING_END:
				inningEnding();
				break;
			case MESS_ROUND_END:
				roundEnding(roundValue);
				break;
			case MESS_BEFORE_INNING_BEGIN: {
				inningEndDialog.cancel();
				inningEndDialog = null;

				beforeInningBegin();
				break;
			}
			case MESS_NEXT_TURN: {
				nextTurn();
				break;
			}
			default:
				break;
			}
		}
	};

	private SurfaceHolder sfh;
	private Paint paint;
	private GameThread gameThread;

	private SharedPreferences setting;

	private GameActivity gameActivity;

	public GameView(Context context, AttributeSet attrs) {
		super(context, attrs);
		gameActivity = (GameActivity) context;
		audioManager = AudioManager.getInstance();
		sfh = this.getHolder();
		sfh.addCallback(this);
		paint = new Paint();
		paint.setAntiAlias(true);
		setFocusable(true);
		setFocusableInTouchMode(true);
		setting = context.getSharedPreferences("Guandan_Setting",
				Context.MODE_PRIVATE);
		this.setOnTouchListener(this);// ������󶨴���������
		gd = new GestureDetector(this);
		init();

	}

	private void init() {
		imageLoader = ImageLoader.getInstance();
		screenWidth = imageLoader.getScreenWidth();
		screenHeight = imageLoader.getScreenHeight();
		players = new Player[4];

		animationSpriteList = new ArrayList<AnimationSprite>();
		animationSpriteListLock = new ReentrantLock(true);

		initPhotoes();
		
		

	}

	private void initPhotoes() {
		/**
		 * NPCͷ��ͼƬ��ʼ��
		 */
		npcPhotoBitmaps = new Bitmap[3];
		playerNames = new String[4];

		/**
		 * Ϊͷ��������ʾ������ ע�����ʵ����Ļ��С����
		 */
		Point player0_Pos = new Point(700, 100);// (630,100);
		Point player1_Pos = new Point(120, 25);// (150,25);
		Point player2_Pos = new Point();

		player0_Pos.x = (int) (((float) screenWidth * 700) / ((float) Constants.SCREEN_WIDTH_DEFINED));
		player0_Pos.y = (int) (((float) screenHeight * 100) / ((float) Constants.SCREEN_HEIGHT_DEFINED));
		player1_Pos.x = (int) (((float) screenWidth * 140) / ((float) Constants.SCREEN_WIDTH_DEFINED));
		player1_Pos.y = (int) (((float) screenHeight * 30) / ((float) Constants.SCREEN_HEIGHT_DEFINED));
		player2_Pos.x = (int) (((float) screenWidth * 30) / ((float) Constants.SCREEN_WIDTH_DEFINED));
		player2_Pos.y = (int) (((float) screenHeight * 185) / ((float) Constants.SCREEN_HEIGHT_DEFINED));

		npcPhotoPositions = new Point[3];
		npcPhotoPositions[0] = new Point(player0_Pos.x, player0_Pos.y);
		npcPhotoPositions[1] = new Point(player1_Pos.x, player1_Pos.y);
		npcPhotoPositions[2] = new Point(player2_Pos.x, player2_Pos.y);

	}

	public void onDraw(Canvas canvas) {
		/**
		 * ������������: 1. ��ײ�������ͼ�� 2. ����NPCͷ��,����,ʣ������ 3. ���Ƹ�����Ҵ������ 4. ������ҵ��ƶ�
		 */
		if (canvas == null)
			return;

		/**
		 * ���Ȼ�������ͼ��
		 */
		if (desktopImage != null) {
			canvas.drawBitmap(
					desktopImage,
					new Rect(0, 0, desktopImage.getWidth(), desktopImage
							.getHeight()), new Rect(0, 0, screenWidth,
							screenHeight), null);
		}

		/**
		 * ����ͷ��
		 */
		for (int i = 0; i < 3; ++i)
			canvas.drawBitmap(
					npcPhotoBitmaps[i],
					new Rect(0, 0, npcPhotoBitmaps[i].getWidth(),
							npcPhotoBitmaps[i].getHeight()),
					new Rect(npcPhotoPositions[i].x, npcPhotoPositions[i].y,
							npcPhotoPositions[i].x
									+ npcPhotoBitmaps[i].getWidth(),
							npcPhotoPositions[i].y
									+ npcPhotoBitmaps[i].getHeight()), null);

		/**
		 * ����ʣ������
		 */
		for (int i = 0; i < 3; ++i)
			canvas.drawText("��ʣ: " + players[i].getPokerNum() + "��",
					npcPhotoPositions[i].x - 15, npcPhotoPositions[i].y + 55,
					new Paint());
		/**
		 * ����NPC�������
		 */
		for (int i = 0; i < 3; ++i)
			canvas.drawText(imageLoader.getSelectedOpponentList().get(i),
					npcPhotoPositions[i].x + 5, npcPhotoPositions[i].y - 5,
					new Paint());

		for (int i = 0; i < players.length; i++)
			players[i].drawPokers(canvas);

		/**
		 * ���ƶ���
		 */
		drawAnimations(canvas);
	}

	private void drawAnimations(Canvas canvas) {
		animationSpriteListLock.lock();

		for (int i = 0; i < animationSpriteList.size(); i++) {
			AnimationSprite aniSprite = animationSpriteList.get(i);

			if (aniSprite.isActive())
				aniSprite.onDraw(canvas);
		}

		animationSpriteListLock.unlock();
	}

	public void onPatternClick() {
		userPlayer.sortPokers(UserPlayer.SORT_POKERS_AS_PATTERN);
	}

	public void onPointClick() {
		userPlayer.sortPokers(UserPlayer.SORT_POKERS_AS_POINTS);
	}

	public void onTypeClick() {
		userPlayer.sortPokers(UserPlayer.SORT_POKERS_AS_TYPE);
	}

	public void onBeginClick() {
		gameActivity.setViews_INVISIBLE(R.id.begin_button);
		/**
		 * ��ʼʱ�Ž��Ʒ��ŵ���������
		 */
		assignTribute();

		/**
		 * ����ƾ��߼���ر���
		 */
		for (int i = 0; i < orderRecord.length; i++)
			orderRecord[i] = 0;
		recordIndex = 0;

		currentPokers = new ArrayList<Poker>();
		currentWinner = currentTurn;
		lastPokers[0] = null;
		lastPokers[1] = null;
		lastPokers[2] = null;
		lastPokers[3] = null;

		/**
		 * ֪ͨ�������һ�ֿ�ʼ��
		 */
		for (int i = 0; i < 4; i++) {
			players[i].inningBegin();
		}

		/**
		 * �ı�viewStateΪ"�Ծ���"
		 * 
		 * ���ƾֿ�ʼ���ֵ���ҳ��� ����Ҫ��ذ�ť״̬
		 */
		viewState = STATE_IN_BATTLE;

		if (currentTurn == USER_PLAYER_TURN) {
			gameActivity.setViews_VISIBLE(R.id.lead_button);
			gameActivity.setViews_VISIBLE(R.id.reset_button);
		}
	}

	private void assignTribute() {
		/**
		 * ���ȼ���ǲ��Ǵ��ڽ����ع�
		 */
		if (tributePokers == null)
			return;

		boolean allNull = true;
		for (int i = 0; i < 4; i++) {
			if (tributePokers[i] != null) {
				allNull = false;
				break;
			}
		}
		if (allNull)
			return;

		int firstPlayerTurn = orderRecord[0];
		int secondPlayerTurn = orderRecord[1];
		int thirdPlayerTurn = orderRecord[2];
		int lastPlayerTurn = orderRecord[3];

		/**
		 * ˫�� ��Ҫ�Ƚ����Ź��Ĵ�С ͷ���ô�Ĺ�
		 */
		if (Math.abs(thirdPlayerTurn - lastPlayerTurn) == 2) {
			if (Comparator.comparePoker(tributePokers[lastPlayerTurn],
					tributePokers[thirdPlayerTurn], currentGrade) == 1) {
				players[firstPlayerTurn]
						.receivePay(tributePokers[lastPlayerTurn]);
				players[secondPlayerTurn]
						.receivePay(tributePokers[thirdPlayerTurn]);
				players[thirdPlayerTurn]
						.receivePay(tributePokers[secondPlayerTurn]);
				players[lastPlayerTurn]
						.receivePay(tributePokers[firstPlayerTurn]);
			} else {
				players[firstPlayerTurn]
						.receivePay(tributePokers[thirdPlayerTurn]);
				players[secondPlayerTurn]
						.receivePay(tributePokers[lastPlayerTurn]);
				players[thirdPlayerTurn]
						.receivePay(tributePokers[firstPlayerTurn]);
				players[lastPlayerTurn]
						.receivePay(tributePokers[secondPlayerTurn]);
			}
		}
		/**
		 * ����
		 */
		else {
			players[firstPlayerTurn].receivePay(tributePokers[lastPlayerTurn]);
			players[lastPlayerTurn].receivePay(tributePokers[firstPlayerTurn]);
		}

		/**
		 * �ַ���֮��
		 */
		tributePokers = null;
	}

	public void onLeadClick() {
		/**
		 * ���� �й��ƾ��߼�,�Ƚϸ��� ��������: ��pkSpriteManager����õ�ǰ�������� ���鵯�������Ƿ���ϳ���Ҫ��
		 * Ҫ�ȵ�ǰ���ƴ�(һ��Ҫ�ɱ�,����Ҫ��)
		 * 
		 * ���������userPlayer����lead���ƴ�� Ȼ����ó�Ա����someoneLead��֪ͨ������ҵȲ��� ��󽻳�����Ȩ
		 * 
		 */
		int compareResult = 0;

		ArrayList<Poker> poppedPokers = userPlayer.getPoppedPokers();
		int pokerType = Parser.getPokerType(poppedPokers, currentGrade)[0];

		Log.i("heiheihei poker type", " " + pokerType);

		if (poppedPokers == null || poppedPokers.size() == 0) {
			// ������ʾ,���ܳ�����
			tips(PlayerWarning.NULL_POKER_WARNING);
		} else if (pokerType == PokerType.INVALID_TYPE) {
			// ������ʾ,��Ч����
			tips(PlayerWarning.INVALID_POKER_TYPE_WARNING);
		} else if (currentWinner == USER_PLAYER_TURN) {

			/*
			 * 
			 * ��ǰ���ֵ���ҷ���
			 */
			ArrayList<Poker> playerPokers = userPlayer.lead();

			someoneLead(playerPokers, USER_PLAYER_TURN);

			setRightButtons_INVISIBLE();

			/**
			 * �Ƴ����������recordOrder
			 */
			if (userPlayer.checkFinished())
				recordOrder(USER_PLAYER_TURN);

			nextTurn();

		}

		else if ((compareResult = Comparator.comparePokers(poppedPokers,
				currentPokers, currentGrade)) != 1) {
			// ������ʾ,�������ȵ�ǰ�����

			if (compareResult == -1) {
				tips(PlayerWarning.MATCH_WARNING);
			} else {
				tips(PlayerWarning.TOO_SMALL_WARNING);
			}

		} else {
			ArrayList<Poker> playerPokers = userPlayer.lead();

			someoneLead(playerPokers, USER_PLAYER_TURN);

			setRightButtons_INVISIBLE();

			/**
			 * �Ƴ����������recordOrder
			 */
			if (userPlayer.checkFinished())
				recordOrder(USER_PLAYER_TURN);

			nextTurn();

		}
	}

	private void setRightButtons_INVISIBLE() {
		gameActivity.setViews_INVISIBLE(R.id.lead_button);
		gameActivity.setViews_INVISIBLE(R.id.pass_button);
		gameActivity.setViews_INVISIBLE(R.id.reset_button);
	}

	private void setRightButtons_VISIBLE() {
		gameActivity.setViews_VISIBLE(R.id.lead_button);
		gameActivity.setViews_VISIBLE(R.id.pass_button);
		gameActivity.setViews_VISIBLE(R.id.reset_button);
	}

	private void setLeftButtons_INVISIBLE() {
		gameActivity.setViews_INVISIBLE(R.id.pattern_button);
		gameActivity.setViews_INVISIBLE(R.id.point_button);
		gameActivity.setViews_INVISIBLE(R.id.type_button);
	}

	private void setLeftButtons_VISIBLE() {
		gameActivity.setViews_VISIBLE(R.id.pattern_button);
		gameActivity.setViews_VISIBLE(R.id.point_button);
		gameActivity.setViews_VISIBLE(R.id.type_button);
	}

	public void onPassClick() {
		/**
		 * ������ �������ƿ��Խ��䵯�� ����someoneLead ����������Ȩ
		 */
		userPlayer.resetPokers();

		someoneLead(null, USER_PLAYER_TURN);

		setRightButtons_INVISIBLE();

		nextTurn();
	}

	public void onPayBackClick() {
		ArrayList<Poker> poppedPoker = userPlayer.getPoppedPokers();

		if (reasonablePayBack(poppedPoker)) {
			Poker paidPokers = userPlayer.payBack();

			playerPaid_copy(paidPokers);

			gameActivity.setViews_INVISIBLE(R.id.payback_button);
			viewState = STATE_WAITING_INNING_BEGIN;
			gameActivity.setViews_VISIBLE(R.id.begin_button);

		} else {
			/**
			 * ����������ʾ??
			 */
		}
	}

	public void onPayClick() {
		ArrayList<Poker> poppedPoker = userPlayer.getPoppedPokers();
		if (reasonablePay(poppedPoker)) {
			Poker paidPokers = userPlayer.payTribute();

			playerPaid_copy(paidPokers);

			gameActivity.setViews_INVISIBLE(R.id.pay_button);
			viewState = STATE_WAITING_INNING_BEGIN;
			gameActivity.setViews_VISIBLE(R.id.begin_button);
		} else {
			/**
			 * ����������ʾ??
			 */
		}
	}

	private void playerPaid_copy(Poker paidPoker) {
		// TODO Auto-generated method stub
		/**
		 * ֻ��ʾ��������
		 */

		if (tributePokers == null)
			tributePokers = new Poker[4];

		tributePokers[USER_PLAYER_TURN] = paidPoker;
		userPlayer.Ipaid(paidPoker);
	}

	public void onResetClick() {
		/**
		 * ���ƶ����ü���
		 */
		userPlayer.resetPokers();
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return gd.onTouchEvent(event);
	}

	public boolean onFling(MotionEvent event1, MotionEvent event2,
			float velocityX, float velocityY) {
		userPlayer.onFling(event1, event2, velocityX, velocityY);
		return true;

	}

	public boolean onTouchEvent(MotionEvent event) {
		userPlayer.onTouchEvent(event);
		return true;
	}

	private void update() {
		/**
		 * ���¶�������
		 */

		animationSpriteListLock.lock();

		/**
		 * ɾ���Ѿ������Ķ���
		 */
		for (int i = 0; i < animationSpriteList.size(); i++) {
			AnimationSprite aniSprite = animationSpriteList.get(i);
			if (!aniSprite.isActive()) {
				animationSpriteList.remove(i);
				i--;
			}
		}

		for (int i = 0; i < animationSpriteList.size(); i++) {
			animationSpriteList.get(i).update();
		}

		animationSpriteListLock.unlock();
	}

	/**
	 * �Է��ؼ��Ĵ���
	 * 
	 * Home��,��Դ���Ƿ�ý���GameActivity����??
	 * 
	 * @param keyCode
	 * @param event
	 * @return
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return false;
	}

	protected void nextTurn() {
		/**
		 * 1. һ���Ѿ�����ʱ�����˳�
		 */
		if (isInningEnd())
			return;

		/**
		 * 2. Ѱ�ҵ�ǰ�ĳ�����
		 */
		boolean bLoop = true;
		int turn = currentTurn;
		while (bLoop) {
			turn = (turn + 1) % 4;

			// ������һ��??
			lastPokers[turn] = null;
			players[turn].cleanMyDesk();

			if (!players[turn].isFinished()) {
				bLoop = false;

			}

			/**
			 * ���Ѿ��������� ��鵱ǰcurrentWinner ��currentWinner���Լ�
			 * �������Լ������ƺ�,��û�н����Լ�����,�����ýӷ�
			 */
			else if (currentWinner == turn) {
				currentPokers = null;

				/**
				 * �ԼҵĴ���ż���(currentTurn+2)%4 �Լ�δ������,����Ȩ�ֵ��Լ� �����Ѿ�˫��,���ᵽ������
				 */
				if (!players[(turn + 2) % 4].isFinished()) {
					currentWinner = (turn + 2) % 4;
					turn = (turn + 2) % 4;

					bLoop = false;
				}
			} else {
				Log.e("next turn", "������ѭ��");
			}
		}

		/**
		 * 3. ����Ƿ��Ƶ�����
		 */
		if (turn == currentWinner) {
			currentPokers = null;
		}

		/**
		 * 4. �ֵ���ҳ����򽫳��ƵȰ�ť��ʾ����; �ֵ�AI��ҳ����������itsMyTurn��������֪ͨ
		 */
		currentTurn = turn;
		// players[currentTurn].itsMyTurn(); //�Զ����Ƽӵ�

		/**
		 * �����Զ�����ȥ����
		 */
		if (turn == USER_PLAYER_TURN) {
			if (currentWinner == USER_PLAYER_TURN) {
				// leadButton.setActive(true);
				// resetButton.setActive(true);
				gameActivity.setViews_VISIBLE(R.id.lead_button);
				gameActivity.setViews_VISIBLE(R.id.reset_button);
			} else {
				// leadButton.setActive(true);
				// passButton.setActive(true);
				// resetButton.setActive(true);
				
				setRightButtons_VISIBLE();
			}

			/**
			 * ���ܷ񵽴����
			 */
			Log.i("next", "wanjia jiefeng");
		} else {
			players[currentTurn].itsMyTurn();
		}
	}

	private boolean isInningEnd() {
		for (int i = 0; i < 4; i++) {
			if (!players[i].isFinished())
				return false;
		}

		return true;
	}

	private boolean reasonablePay(ArrayList<Poker> pokers) {
		/**
		 * ���������ǳ����ƺ���������һ����
		 */
		if (pokers == null || pokers.size() != 1)
			return false;
		else {
			Poker pk = pokers.get(0);
			if (Helper.isMasterCard(pk, currentGrade))
				return false;
			else {
				Poker maxPoker = userPlayer.maxPoker_ExceptMasterCard();
				if (Comparator.comparePoker(pk, maxPoker, currentGrade) == 0)
					return true;
				else
					return false;
			}
		}
	}

	private boolean reasonablePayBack(ArrayList<Poker> pokers) {
		/**
		 * �ع��ĵ�������С��10
		 */
		if (pokers == null || pokers.size() != 1)
			return false;
		else
			return true;
	}

	protected void beforeInningBegin() {
		/**
		 * ϴ��
		 */
		shuffle();

		/**
		 * ����
		 */
		assign();

		/**
		 * ���н����ͻع�����
		 */
		int payState = pay_and_payBack_Copy();

		/**
		 * ���ó��ƴ���
		 */
		currentTurn = whoFirst();

		/**
		 * ����payState��ֵ���趨����״̬
		 */
		
		if (payState == 0) {
			gameActivity.setViews_VISIBLE(R.id.begin_button);
			viewState = STATE_WAITING_INNING_BEGIN;
		} else if (payState == 1) {
			gameActivity.setViews_VISIBLE(R.id.pay_button);
			viewState = STATE_PLAYER_PAY;
		} else {
			gameActivity.setViews_VISIBLE(R.id.payback_button);
			viewState = STATE_PLAYER_BACK;
		}
		gameActivity.refreshViews(playerGrade, opponentGrade, currentGrade);
		setLeftButtons_VISIBLE();
	}

	private int whoFirst() {
		int turn = 0;

		/**
		 * ��һ�����ȷ�����ƴ���
		 */
		if (orderRecord[0] == -1) {
			Random random = new Random();
			turn = random.nextInt(4);

		}

		/**
		 * ���������һ�ֽ����ȷ��
		 */
		else {
			int firstPlayerTurn = orderRecord[0];
			int secondPlayerTurn = orderRecord[1];
			int thirdPlayerTurn = orderRecord[2];
			int lastPlayerTurn = orderRecord[3];

			/**
			 * �ֵ��º�˫��ͳ�� ĩ�εĴ����
			 */
			int oldJokerNum = 0;
			if (Math.abs(thirdPlayerTurn - lastPlayerTurn) == 2) {
				oldJokerNum = players[lastPlayerTurn].getPokerNum(
						PokerPattern.JOKER, PokerPoints.OLD_JOKER);
				oldJokerNum += players[thirdPlayerTurn].getPokerNum(
						PokerPattern.JOKER, PokerPoints.OLD_JOKER);
			} else {
				oldJokerNum = players[lastPlayerTurn].getPokerNum(
						PokerPattern.JOKER, PokerPoints.OLD_JOKER);
			}

			/**
			 * ������,��ͷ���ȳ� ����ĩ���ȳ�
			 */
			if (oldJokerNum == 2) {
				turn = firstPlayerTurn;
			} else
				turn = lastPlayerTurn;
		}

		currentWinner = turn;

		return turn;
	}

	private int pay_and_payBack_Copy() {
		/**
		 * �������ֻ��Ҫ���Լ��Ĺ��Ƹ��� �ȴ������� ���Ƚ����Է�
		 */
		if (orderRecord == null || orderRecord[0] == -1)
			return 0;

		int firstPlayerTurn = orderRecord[0];
		int secondPlayerTurn = orderRecord[1];
		int thirdPlayerTurn = orderRecord[2];
		int lastPlayerTurn = orderRecord[3];

		int retValue;

		tributePokers = null;
		tributePokers = new Poker[4];

		if (Math.abs(thirdPlayerTurn - lastPlayerTurn) == 2) {
			int oldJokerNum = players[lastPlayerTurn].getPokerNum(
					PokerPattern.JOKER, PokerPoints.OLD_JOKER);
			oldJokerNum += players[thirdPlayerTurn].getPokerNum(
					PokerPattern.JOKER, PokerPoints.OLD_JOKER);

			/**
			 * ���ǿ���
			 */
			if (oldJokerNum == 2) {
				tips(PlayerWarning.NO_TRIBUTE_WARNNING);
				retValue = 0;
			} else {
				if (firstPlayerTurn == USER_PLAYER_TURN) {

					tributePokers[lastPlayerTurn] = players[lastPlayerTurn]
							.payTribute();
					tributePokers[thirdPlayerTurn] = players[thirdPlayerTurn]
							.payTribute();
					tributePokers[secondPlayerTurn] = players[secondPlayerTurn]
							.payBack();

					retValue = -1;

				} else if (secondPlayerTurn == USER_PLAYER_TURN) {

					tributePokers[lastPlayerTurn] = players[lastPlayerTurn]
							.payTribute();
					tributePokers[thirdPlayerTurn] = players[thirdPlayerTurn]
							.payTribute();
					tributePokers[firstPlayerTurn] = players[firstPlayerTurn]
							.payBack();

					retValue = -1;
				} else if (thirdPlayerTurn == USER_PLAYER_TURN) {
					tributePokers[lastPlayerTurn] = players[lastPlayerTurn]
							.payTribute();
					tributePokers[secondPlayerTurn] = players[secondPlayerTurn]
							.payBack();
					tributePokers[firstPlayerTurn] = players[firstPlayerTurn]
							.payBack();

					retValue = 1;
				} else {
					tributePokers[thirdPlayerTurn] = players[thirdPlayerTurn]
							.payTribute();
					tributePokers[secondPlayerTurn] = players[secondPlayerTurn]
							.payBack();
					tributePokers[firstPlayerTurn] = players[firstPlayerTurn]
							.payBack();

					retValue = 1;
				}
			}
		}

		/**
		 * ����ǵ���
		 */
		else {
			int oldJokerNum = players[lastPlayerTurn].getPokerNum(
					PokerPattern.JOKER, PokerPoints.OLD_JOKER);

			/**
			 * ���ǿ���
			 */
			if (oldJokerNum == 2) {
				tips(PlayerWarning.NO_TRIBUTE_WARNNING);
				retValue = 0;
			} else {
				if (firstPlayerTurn == USER_PLAYER_TURN) {
					tributePokers[lastPlayerTurn] = players[lastPlayerTurn]
							.payTribute();

					retValue = -1;
				} else if (lastPlayerTurn == USER_PLAYER_TURN) {
					tributePokers[firstPlayerTurn] = players[firstPlayerTurn]
							.payBack();

					retValue = 1;
				} else {
					tributePokers[lastPlayerTurn] = players[lastPlayerTurn]
							.payTribute();
					tributePokers[firstPlayerTurn] = players[firstPlayerTurn]
							.payBack();

					retValue = 0;
				}
			}
		}

		for (int i = 0; i < 3; i++) {
			if (tributePokers[i] != null)
				players[i].Ipaid(tributePokers[i]);
		}

		return retValue;
	}

	private void assign() {

		/**
		 * ����˳�򷢼���
		 */
		ArrayList<Poker>[] assignedPokers = new ArrayList[4];
		for (int i = 0; i < 4; i++)
			assignedPokers[i] = new ArrayList<Poker>();

		for (int i = 0; i < allPokers.size(); i++) {
			assignedPokers[i % 4].add(allPokers.get(i));
		}
		allPokers.clear();

		/**
		 * ����Player���pokerAssigned���� ���Ʒ��������������
		 */
		for (int i = 0; i < 4; i++)
			players[i].pokerAssigned(assignedPokers[i]);
	}

	private void shuffle() {
		/**
		 * �����ԱallPokers����108���ƵĻ� ˵���м���������� ֻ�����»�һ������
		 */
		if (allPokers == null || allPokers.size() != 108) {
			newPokers();
		}

		/**
		 * ϴ���㷨: ���Ʒ�Ϊ���� ������������ȡ�Ʋ���,ÿ�����ȡ1��4��
		 * 
		 * �����ϴ1-5��
		 */
		Random random = new Random();
		int times = random.nextInt(6);
		if (times == 0)
			times = 1;

		for (int i = 1; i <= times; i++) {
			ArrayList<Poker> pokerList1 = new ArrayList<Poker>();
			ArrayList<Poker> pokerList2 = new ArrayList<Poker>();
			for (int j = 0; j < allPokers.size(); j++) {
				if (j <= 53)
					pokerList1.add(allPokers.get(j));
				else
					pokerList2.add(allPokers.get(j));
			}
			allPokers.clear();

			boolean finished = false;
			while (!finished) {
				if (pokerList1.size() == 0 && pokerList2.size() == 0) {
					finished = true;
				} else if (pokerList1.size() == 0) {
					finished = true;
					allPokers.addAll(pokerList2);
				} else if (pokerList2.size() == 0) {
					finished = true;
					allPokers.addAll(pokerList1);
				} else {
					int num1 = random.nextInt(5);
					int num2 = random.nextInt(5);
					if (num1 == 0)
						num1 = 1;
					if (num2 == 0)
						num2 = 1;

					while (num1 > 0 && pokerList1.size() > 0) {
						allPokers.add(pokerList1.remove(0));
						num1--;
					}
					while (num2 > 0 && pokerList2.size() > 0) {
						allPokers.add(pokerList2.remove(0));
						num2--;
					}
				}
			}
		}
	}

	private void newPokers() {
		allPokers = new ArrayList<Poker>();

		for (int pattern = PokerPattern.CLUB; pattern <= PokerPattern.SPADE; pattern++) {
			for (int points = PokerPoints.ACE; points <= PokerPoints.KING; points++) {
				allPokers.add(new Poker(pattern, points));
				allPokers.add(new Poker(pattern, points));
			}
		}

		/**
		 * �ټ������
		 */
		allPokers.add(new Poker(PokerPattern.JOKER, PokerPoints.LITTLE_JOKER));
		allPokers.add(new Poker(PokerPattern.JOKER, PokerPoints.LITTLE_JOKER));
		allPokers.add(new Poker(PokerPattern.JOKER, PokerPoints.OLD_JOKER));
		allPokers.add(new Poker(PokerPattern.JOKER, PokerPoints.OLD_JOKER));

	}

	protected void roundEnding(int endValue) {
		// TODO Auto-generated method stub
		roundFinished = true;

		roundValue = endValue;

		AlertDialog.Builder builder = new AlertDialog.Builder(gameActivity);

		if (endValue == 1)
			builder.setTitle("��ϲ��,��һ�ֻ�ʤ��");
		else
			builder.setTitle("���ź�,���������һ��");
		builder.setCancelable(true);
		builder.setPositiveButton("����һ��",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();

						newRound();
					}
				});
		builder.setNegativeButton("�˳��ƾ�",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
						gameActivity.finish();
					}
				});
		builder.show();

		viewState = STATE_ROUND_END;
	}

	protected void inningEnding() {
		// TODO Auto-generated method stub
		setRightButtons_INVISIBLE();
		setLeftButtons_INVISIBLE();

		for (int i = 0; i < 4; i++)
			players[i].inningEnd();

		/**
		 * ���ж��Ƿ���ĩ
		 * 
		 * Ȼ��ʼ����
		 */
		int roundValue = isRoundEnd();
		if (roundValue != 0) {
			roundEnding(roundValue);
		} else {
			gradeUp();
			inningEndDialog = new InningEndDialog(gameActivity);

			viewState = STATE_INNING_END;
		}
	}

	private void gradeUp() {
		// TODO Auto-generated method stub
		int firstPlayerTurn = orderRecord[0];
		int secondPlayerTurn = orderRecord[1];
		int thirdPlayerTurn = orderRecord[2];
		int lastPlayerTurn = orderRecord[3];

		/**
		 * ����ܹ�����gradeUp ˵��һ�ֻ�û�н���
		 * 
		 * ������������A����ȻҪ�˻�A
		 */
		/**
		 * ˫�����ļ� �����ҶԼ�Ϊ��ĩβ��һ�� �����ҶԼҲ�Ϊ��ĩβ������
		 */
		if (Math.abs(firstPlayerTurn - secondPlayerTurn) == 2) {
			if (firstPlayerTurn == USER_PLAYER_TURN || firstPlayerTurn == 1)
				playerGrade += 4;
			else
				opponentGrade += 4;
		} else if (Math.abs(firstPlayerTurn - lastPlayerTurn) == 2) {
			if (firstPlayerTurn == USER_PLAYER_TURN || firstPlayerTurn == 1)
				playerGrade += 1;
			else
				opponentGrade += 1;
		} else {
			if (firstPlayerTurn == USER_PLAYER_TURN || firstPlayerTurn == 1)
				playerGrade += 2;
			else
				opponentGrade += 2;
		}

		/**
		 * ��������A���˻ص�A
		 */
		if (playerGrade > PokerGrade.ACE)
			playerGrade = PokerGrade.ACE;
		if (opponentGrade > PokerGrade.ACE)
			opponentGrade = PokerGrade.ACE;

		if (orderRecord[0] == USER_PLAYER_TURN || orderRecord[0] == 1)
			currentGrade = playerGrade;
		else
			currentGrade = opponentGrade;
	}

	private int isRoundEnd() {
		// TODO Auto-generated method stub
		/**
		 * return 0 δ���� 1 ����,�һ�ʤ ��1 ����,��ʧ��
		 */
		/**
		 * ���Լ�һ����Aʱ ����˫�²��ܹ���
		 */
		int retValue = 0;
		int firstPlayerTurn = orderRecord[0];
		int secondPlayerTurn = orderRecord[1];
		int thirdPlayerTurn = orderRecord[2];
		int lastPlayerTurn = orderRecord[3];

		if (playerGrade == PokerGrade.ACE && opponentGrade == PokerGrade.ACE) {
			if ((firstPlayerTurn + secondPlayerTurn) == 4)
				retValue = 1;
			else if ((thirdPlayerTurn + lastPlayerTurn) == 4)
				retValue = -1;
			else
				retValue = 0;
		} else if (playerGrade == PokerGrade.ACE
				&& currentGrade == PokerGrade.ACE) {
			if ((firstPlayerTurn + secondPlayerTurn) == 4)
				retValue = 1;
			else
				retValue = 0;
		} else if (opponentGrade == PokerGrade.ACE
				&& currentGrade == PokerGrade.ACE) {
			if ((thirdPlayerTurn + lastPlayerTurn) == 4)
				retValue = -1;
			else
				retValue = 0;
		} else
			retValue = 0;

		return retValue;
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		// TODO Auto-generated method stub
		/**
		 * ��ȡ����
		 */
		int desktopID = imageLoader.getDesktopImage(setting
				.getInt("desktop", 0));
		desktopImage = imageLoader.getBitmapById(desktopID);

		/**
		 * ��ȡNPCͷ��ͼƬ
		 */
		for (int i = 0; i < 3; ++i) {
			playerNames[i] = imageLoader.getSelectedOpponentList().get(i);
			int resourceID = imageLoader.getPhotoImage(playerNames[i]);
			npcPhotoBitmaps[i] = imageLoader.getBitmapById(resourceID);
		}
		playerNames[3] = "��";

		/**
		 * ��ʼ���ƾֵ���Ҷ���
		 */
		for (int i = 0; i < 4; i++)
			players[i] = null;

		/**
		 * ��ȡ�Ѷ� �߼�AI��ûʵ��
		 */
		int gameLevel = setting.getInt("AILevel", AIType.easy_AI);
		if (gameLevel == 2)
			gameLevel = 1;
		for (int i = 0; i < 3; i++) {
			if (gameLevel == 0)
				players[i] = new Easy_AI_Player(this, i, AIType.easy_AI);
			else if (gameLevel == 1)
				players[i] = new Normal_AI_Player(this, i, AIType.normal_AI);
		}

		players[USER_PLAYER_TURN] = new UserPlayer(this, USER_PLAYER_TURN);
		userPlayer = (UserPlayer) players[USER_PLAYER_TURN];

		/**
		 * ΪһЩ���ƾ��߼���صĳ�Ա���ٿռ�
		 */
		lastPokers = new ArrayList[4];
		orderRecord = new int[4];

		/**
		 * ���ó�Ա����newRound
		 */

		newRound();

		gameThread = new GameThread();
		gameThread.setRunning(true);
		gameThread.start();
	}

	private void newRound() {
		roundFinished = false;

		/**
		 * һЩ�й��ƾ��߼���׼������
		 */
		playerGrade = PokerGrade.TWO;
		opponentGrade = PokerGrade.TWO;
		currentGrade = PokerGrade.TWO;
		for (int i = 0; i < orderRecord.length; i++)
			orderRecord[i] = -1;

		/**
		 * ���µ�һ�ֿ�ʼ�µ�һ��
		 */
		beforeInningBegin();

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		// TODO Auto-generated method stub

	}

	public void pause() {
		AudioManager.stopMusic();
		gameThread.suspend();
	}

	public void resume() {
		AudioManager.startMusic(SoundsName.BATTLEBACK_MUSIC);
		for(int i=0;i<4;i++){
			players[i].notify();
		}
		gameThread.start();
	}

	public void stop() throws InterruptedException {
		AudioManager.stopMusic();
		for(int i=0;i<4;i++){
			players[i].wait();
		}
		gameThread.stop();
	}

	public int getCurrentTurn() {
		return currentTurn;
	}

	public void setCurrentTurn(int grade) {
		currentTurn = grade;
	}

	public ArrayList<Poker> getCurrentPokers() {
		return currentPokers;
	}

	public int getCurrentWinner() {
		return currentWinner;
	}

	public void setCurrentWinner(int grade) {
		currentWinner = grade;
	}

	public int getCurrentGrade() {
		return currentGrade;
	}

	public void setCurrentGrade(int grade) {
		currentGrade = grade;
	}

	public int getPlayerGrade() {
		return playerGrade;
	}

	public void setPlayerGrade(int grade) {
		playerGrade = grade;
	}

	public int getOpponentGrade() {
		return opponentGrade;
	}

	public void setOpponentGrade(int grade) {
		opponentGrade = grade;
	}

	public int[] getOrderRecord() {
		return orderRecord;
	}

	public void someoneLead(ArrayList<Poker> pokerList, int index) {
		/**
		 * add by ��ԣ�� ��������AI���ڴ���ʱ�˳��ı���״��
		 */
		if (roundFinished)
			return;

		if (pokerList == null || pokerList.size() == 0) {
			/**
			 * ����someonePass
			 */
			someonePass(index);
		} else {

			/**
			 * ��pokerList�Ÿ���
			 * 
			 * �ٴ���currentPokers
			 */
			int currentType = Parser.getPokerType(pokerList, currentGrade)[0];

			/**
			 * add by ������
			 */
			makeSoundByType(currentType);

			pokerList = Sorter.sortPokers_reverse(pokerList, currentType,
					currentGrade);

			currentPokers = pokerList;

			currentWinner = index;

			/**
			 * ���մ��������
			 */
			allPokers.addAll(pokerList);
		}

		for (int i = 0; i < 4; i++) {
			players[i].someoneLead(pokerList, index);
		}

		lastPokers[index] = pokerList;
	}
	
	/**
	 * add by ������
	 * @param type
	 */
	private void makeSoundByType(int type){
		switch (type) {
		case PokerType.BOMB:
			audioManager.startSound(SoundsName.BOMB_SOUND);
			break;
		case PokerType.DOUBLE_TRIPLE_STRAIGHT:
			audioManager.startSound(SoundsName.GANGBAN_SOUND);
			break;
		case PokerType.FOUR_JOKER:
			audioManager.startSound(SoundsName.WANGZHA_SOUND);
			break;
		case PokerType.STRAIGHT:
			audioManager.startSound(SoundsName.SHUNZI_SOUND);
			break;
		case PokerType.STRAIGHT_FLUSH:
			audioManager.startSound(SoundsName.SHUNZI_SOUND);
			break;
		case PokerType.TRIPLE:
			audioManager.startSound(SoundsName.SANGE_SOUND);
			break;
		case PokerType.TRIPLE_DOUBLE_STRAIGHT:
			audioManager.startSound(SoundsName.LIANDUI_SOUND);
			break;
		case PokerType.TRIPLE_WITH_DOUBLE:
			audioManager
					.startSound(SoundsName.SANDAIYIDUI_SOUND);
			break;
		default:
			return;
		}
	}

	private void someonePass(int playerIndex) {
		/**
		 * ����"����"������AnimationSprite ������playerIndex������Ӧ��ʾ����
		 */
		AnimationSprite passAnSprite = new AnimationSprite("animation_pass",
				false, 15);

		/**
		 * add by ������
		 */
		audioManager.startSound(SoundsName.MAN_BUYAO_SOUND);

		Point spritePos = new Point();
		if (playerIndex == 0) {
			spritePos.x = 540;
			spritePos.y = 102;
		} else if (playerIndex == 1) {
			spritePos.x = 360;
			spritePos.y = 54;
		} else if (playerIndex == 2) {
			spritePos.x = 200;
			spritePos.y = 150;
		} else {
			spritePos.x = 400;// 360;
			spritePos.y = 190;// 150;
		}

		int realX = (int) (((float) spritePos.x * screenWidth) / ((float) Constants.SCREEN_WIDTH_DEFINED));
		int realY = (int) (((float) spritePos.y * screenHeight) / ((float) Constants.SCREEN_HEIGHT_DEFINED));
		spritePos.x = realX - passAnSprite.getWidth() / 2;
		spritePos.y = realY;
		passAnSprite.setPos(spritePos);

		/**
		 * ���뵽������
		 */
		animationSpriteListLock.lock();

		animationSpriteList.add(passAnSprite);

		animationSpriteListLock.unlock();
	}

	public void recordOrder(int playerIndex) {
		/**
		 * add by ��ԣ�� ��������AI���ڴ���ʱ�˳��ı���״��
		 */
		if (roundFinished || orderRecord == null)
			return;

		if (playerIndex == USER_PLAYER_TURN) {
			setLeftButtons_INVISIBLE();
		}

		orderRecord[recordIndex] = playerIndex;

		recordIndex++;

		/**
		 * recordIndexΪ3 ����recordIndexΪ2��˫��ʱ �ƾ־�������
		 * 
		 * ����δ���� ���������Ѿ����굫�ǻ�δ����,ѯ������Ƿ�����ۿ�
		 */
		if (recordIndex == 3) {
			for (int i = 0; i < 4; i++) {
				if (!players[i].isFinished()) {
					orderRecord[recordIndex] = i;
					allPokers.addAll(players[i].IamLast());
					break;
				}
			}

			for (int i = 0; i < 4; i++) {
				if (players[i].isFinished())
					Log.i("Finished", "i");
			}

			/*
			 * ��Hanlder������Ϣ ������һ��
			 */
			Message msg = new Message();
			msg.what = MESS_INNING_END;
			mHandler.sendMessage(msg);
		} else if (recordIndex == 2
				&& (Math.abs(orderRecord[0] - orderRecord[1]) == 2)) {
			int record[] = new int[2];
			int pokerNum[] = new int[2];
			int count = 0;
			for (int i = 0; i < 4; i++) {
				if (!players[i].isFinished()) {
					record[count] = i;
					ArrayList<Poker> pokersLeft = players[i].IamLast();
					pokerNum[count] = pokersLeft.size();
					count++;
					allPokers.addAll(pokersLeft);
				}
			}

			if (pokerNum[0] > pokerNum[1]) {
				orderRecord[recordIndex] = record[1];
				recordIndex++;
				orderRecord[recordIndex] = record[0];
			} else {
				orderRecord[recordIndex] = record[0];
				recordIndex++;
				orderRecord[recordIndex] = record[1];
			}

			// debug
			for (int i = 0; i < 4; i++) {
				if (players[i].isFinished())
					Log.i("Finished", "i");
			}

			Message msg = new Message();
			msg.what = MESS_INNING_END;
			mHandler.sendMessage(msg);
		} else {
			/**
			 * ����Ѿ������� ���ƾֻ�û�н���ʱ
			 * 
			 * ѯ������Ƿ�Ҫ����?
			 */
		}
	}

	public void receiveMessage_NextTurn() {
		// TODO Auto-generated method stub
		/**
		 * add by ��ԣ�� ��������AI���ڴ���ʱ�˳��ı���״��
		 */
		if (roundFinished)
			return;

		Message msg = new Message();
		msg.what = MESS_NEXT_TURN;
		mHandler.sendMessage(msg);
	}

	public int[] getPlayerPokerNum() {
		// TODO Auto-generated method stub
		int[] playerPokerNums = new int[4];

		for (int i = 0; i < 4; i++) {
			playerPokerNums[i] = players[i].getPokerNum();
		}

		return playerPokerNums;
	}

	/**
	 * ��Ҷ��Ʋ�������ʱ ������ʾ
	 * 
	 * @param type
	 *            0:����Ч���͸�����ʾ 1���Գ����Ƹ�����ʾ 2:�����Ͳ���������ʾ 3:����̫С������ʾ
	 */
	private void tips(int type) {
		AnimationSprite warningSprite = null;

		switch (type) {
		case PlayerWarning.INVALID_POKER_TYPE_WARNING:
			warningSprite = new AnimationSprite("animation_invalidpokertype",
					false, 15);
			break;
		case PlayerWarning.NULL_POKER_WARNING:
			warningSprite = new AnimationSprite("animation_nullpokerwarning",
					false, 15);
			break;
		case PlayerWarning.MATCH_WARNING:
			warningSprite = new AnimationSprite("animation_matchwarning",
					false, 15);
			break;
		case PlayerWarning.TOO_SMALL_WARNING:
			warningSprite = new AnimationSprite("animation_toosmallwarning",
					false, 15);
			break;
		case PlayerWarning.NO_TRIBUTE_WARNNING:
			warningSprite = new AnimationSprite("animation_notribute", false,
					15);
			break;

		default:
			break;
		}

		if (warningSprite == null)
			return;

		int posX = (int) (((float) 400 * screenWidth) / ((float) Constants.SCREEN_WIDTH_DEFINED));
		int posY = (int) (((float) 190 * screenHeight) / ((float) Constants.SCREEN_HEIGHT_DEFINED));
		posX = posX - warningSprite.getWidth() / 2;

		/**
		 * ������ʾ������м�
		 */
		if (type == PlayerWarning.NO_TRIBUTE_WARNNING) {
			posY = (int) (((float) 240 * screenHeight) / ((float) Constants.SCREEN_HEIGHT_DEFINED));
			posY -= warningSprite.getHeight() / 2;
		}

		warningSprite.setPos(posX, posY);

		animationSpriteListLock.lock();

		animationSpriteList.add(warningSprite);

		animationSpriteListLock.unlock();

	}

	public void gameRender() {
		Canvas canvas;
		canvas = null;
		try {
			/**
			 * �ȴ�gameView�л�ȡ��������canvas Ȼ���ڻ��������ϻ��ƻ���
			 */
			canvas = sfh.lockCanvas(null);
			synchronized (sfh) {
				onDraw(canvas);
			}
		} finally {
			if (canvas != null)
				sfh.unlockCanvasAndPost(canvas);
		}
	}

	private class InningEndDialog extends Dialog {

		public InningEndDialog(Context context) {
			super(context);

			/**
			 * add by ������
			 */
			AudioManager.musicOff();

			this.setTitle("��ĩ����");
			TextView textView = new TextView(context);
			String resultString = new String("");
			resultString += "ͷ��:" + playerNames[orderRecord[0]] + "\n";
			resultString += "����:" + playerNames[orderRecord[1]] + "\n";
			resultString += "����:" + playerNames[orderRecord[2]] + "\n";
			resultString += "ĩ��:" + playerNames[orderRecord[3]];
			textView.setText(resultString);

			/**
			 * add by ������
			 */
			if (playerNames[orderRecord[0]] == "��")
				audioManager.startSound(SoundsName.WIN_SOUND);
			else if (playerNames[orderRecord[3]] == "��")
				audioManager.startSound(SoundsName.LOSE_SOUND);

			this.setContentView(textView);
			this.show();
		}

		public boolean onTouchEvent(MotionEvent event) {

			int eventType = event.getAction();

			/**
			 * ���������ͷ��¼�(��ζ�űض�������) ��ʼ�µ�һ��
			 * 
			 * ���㴰����ʧ
			 */
			if (eventType == MotionEvent.ACTION_UP) {
				Message msg = new Message();
				msg.what = MESS_BEFORE_INNING_BEGIN;
				mHandler.sendMessage(msg);
			}

			/**
			 * add by ������
			 */
			AudioManager.musicOn();
			return true;
		}
	}

	private class GameThread extends Thread {

		private boolean running = false;

		public void setRunning(boolean run) {
			running = run;
		}

		public void run() {
			while (running) {

				update();
				gameRender();

				/**
				 * ÿˢ��һ֡����20ms �����ƻ���ʱ��,1s�ڿ���ˢ��50֡
				 */
				try {
					Thread.sleep(20);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public boolean onDown(MotionEvent arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onLongPress(MotionEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2,
			float arg3) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onShowPress(MotionEvent arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean onSingleTapUp(MotionEvent arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	public LinkedList<Poker> getPlayerPokers(int index) {
		if (index < 0 || index > 3)
			return null;
		else
			return players[index].getPokers();

	}
	public GameActivity getGameActivity()
	{
		return gameActivity;
	}
}