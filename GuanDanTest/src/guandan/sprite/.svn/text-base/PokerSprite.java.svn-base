/**
 * PokerSprite.java
 * @author 胡裕靖
 * March 20th 2011
 * 
 * PokerSprite扩展Sprite类
 * 实现能呈现到屏幕上的扑克牌
 * 它不但能够将其代表的扑克牌绘制到屏幕上
 * 还能够响应玩家在扑克牌上的操作
 */
package guandan.sprite;

import guandan.game.Poker;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;

public class PokerSprite extends Sprite {

	// Fields/////////////////////////////////////////////

	/**
	 * PokerSprite对应的扑克牌 依靠这个成员获取其图像
	 */
	private Poker poker;

	/**
	 * 表示扑克牌是否从牌堆中弹出
	 */
	private boolean isPopped = false;

	/**
	 * 表示扑克牌是否被选中
	 */
	private boolean isSelected = false;

	private boolean isZoomOut = false;

	// Methods////////////////////////////////////////////

	public PokerSprite(Poker pk) {
		super();

		/**
		 * 注意这一步重要 而不是直接赋值
		 */
		poker = new Poker(pk.pattern, pk.points);

		/**
		 * 调用setImage方法 设置扑克牌的图像
		 */
		setImage();

	}

	/**
	 * 将扑克牌图像绘制出来
	 * 
	 * @param canvas
	 *            :画布对象
	 */
	public void onDraw(Canvas canvas) {

		/**
		 * 先画扑克牌图像
		 */
		canvas.drawBitmap(spriteImage, new Rect(0, 0, spriteImage.getWidth(),
				spriteImage.getHeight()), new Rect(spritePosX, spritePosY,
				spritePosX + spriteWidth, spritePosY + spriteHeight), null);

		/**
		 * 如果被选中 还需绘制加亮显示条
		 * 
		 * 暂定为两个像素的蓝色半透明矩形框 先画在牌内部
		 */
		if (isSelected) {
			Paint paint = new Paint();
			paint.setColor(Color.argb(128, Color.red(Color.BLUE),
					Color.green(Color.BLUE), Color.blue(Color.BLUE)));
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeWidth(2.0f);
			canvas.drawRect(new Rect(spritePosX, spritePosY, spritePosX
					+ spriteWidth, spritePosY + spriteHeight), paint);
		}

	}

	/**
	 * 处理在扑克牌上的触屏事件
	 * 
	 * @param event
	 *            :触屏事件对象
	 */
	public void onTouchEvent(MotionEvent event) {
		int eventType = event.getAction();
		int eventPosX = (int) event.getX();
		int eventPosY = (int) event.getY();

		if (eventType == MotionEvent.ACTION_DOWN
				|| eventType == MotionEvent.ACTION_MOVE) {
			if (isInside(eventPosX, eventPosY))
				isSelected = true;
		} else if (eventType == MotionEvent.ACTION_UP) {
			if (isInside(eventPosX, eventPosY)) {
				if (isPopped)
					retract();
				else
					pop();

				isSelected = false;
			}
		}
	}

	/**
	 * 将扑克牌从牌堆中弹出
	 */
	public void pop() {
		isPopped = true;
		setPosY(spritePosY - 10);

		Log.i("PokerSprite", "pop");
	}

	/**
	 * 将扑克牌缩回牌堆
	 */
	public void retract() {
		isPopped = false;
		setPosY(spritePosY + 10);

		Log.i("PokerSprite", "restract");
	}

	/**
	 * 设置扑克牌的图像 在PokerSprite初始化时调用
	 */
	protected void setImage() {
		if (!poker.isInvalid()) {
			/**
			 * 子画面大小就是图像的大小 是否妥当???
			 */

			if (isZoomOut)
				zoomOut();

			int resourceID = imageLoader.getPokerImage(poker.pattern,
					poker.points);
			spriteImage = imageLoader.parseResourceID(resourceID);

			/**
			 * 换算子画面大小
			 */
			spriteWidth = spriteImage.getWidth();
			spriteHeight = spriteImage.getHeight();

		}
	}

	public boolean isPopped() {
		return isPopped;
	}

	public Poker getPoker() {
		return poker;
	}

	public void setPoker(Poker pk) {
		if (pk == null || pk.isInvalid())
			return;

		poker.pattern = pk.pattern;
		poker.points = pk.points;

		setImage();
	}

	public void setSelected(boolean bSel) {
		isSelected = bSel;
	}

	/**
	 * 将扑克牌放大 长宽各位原来的2倍
	 */
	public void zoomOut() {

		isZoomOut = true;
		spriteWidth *= 2;
		spriteHeight *= 2;

	}

	/**
	 * 将扑克牌缩小 长宽各位原来的1/2
	 */
	public void zoomIn() {
		isZoomOut = false;
		spriteWidth /= 2;
		spriteHeight /= 2;

	}
}
