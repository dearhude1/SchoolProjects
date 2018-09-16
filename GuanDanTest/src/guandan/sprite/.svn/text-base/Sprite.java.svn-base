/**
 * Sprite.java
 * @author 胡裕靖
 * March 20th 2011
 * 
 * Sprite类是图像在游戏中的一个载体,它可以实现移动图像,播放动画等功能
 * 它俗称"子画面"或者"精灵"
 * Sprite类除了具有图像属性之外还具有:坐标位置,宽度,高度,速度,帧数等属性
 * 位置属性决定Sprite的图像在屏幕上绘制的方位
 * 宽度和高度决定Sprite类呈现在画面上的长宽大小
 * 速度属性可以控制图像的移动,图像帧数相关的属性可以控制动画的播放
 */
package guandan.sprite;

import guandan.game.ImageLoader;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.MotionEvent;

public class Sprite {

	// Fields///////////////////////////////////////////////

	/**
	 * 子画面宽和高
	 */
	protected int spriteWidth;
	protected int spriteHeight;

	/**
	 * 子画面在屏幕上的坐标
	 */
	protected int spritePosX;
	protected int spritePosY;

	/**
	 * 子画面速度,留着备用
	 */
	protected int spriteVelocityX;
	protected int spriteVelocityY;

	/**
	 * 子画面的图像
	 */
	protected Bitmap spriteImage;

	/**
	 * 对游戏的图像加载器的引用
	 */
	protected ImageLoader imageLoader;

	/**
	 * 游戏屏幕宽和高
	 */
	protected int screenWidth;
	protected int screenHeight;

	/**
	 * 表示子画面是否存活 "存活"就显示出来 "不存活"就将被销毁
	 */
	private boolean isActive;

	// Methods//////////////////////////////////////////////

	public Sprite() {
		imageLoader = ImageLoader.getInstance();
		screenWidth = imageLoader.getScreenWidth();
		screenHeight = imageLoader.getScreenHeight();

		isActive = true;
	}

	/**
	 * 绘制该子画面的图像 本函数不进行具体实现 具体实现由其各个子类来完成
	 * 
	 * @param canvas
	 *            :画布对象
	 */
	public void onDraw(Canvas canvas) {

	}

	/**
	 * 更新该子画面 本函数不进行具体实现 具体实现由其各个子类来完成
	 */
	public void update() {

	}

	/**
	 * 对在该子画面上的触屏事件进行响应 本函数不进行具体实现 具体实现由其各个子类来完成
	 * 
	 * @param event
	 *            :触屏事件
	 */
	public void onTouchEvent(MotionEvent event) {

	}

	/**
	 * 设置该子画面的图像spriteImage 本函数不进行具体实现 具体实现由其各个子类来完成
	 */
	protected void setImage() {

	}

	public int getWidth() {
		return spriteWidth;
	}

	public int getHeight() {
		return spriteHeight;
	}

	public int getPosX() {
		return spritePosX;
	}

	public int getPosY() {
		return spritePosY;
	}

	public void setPosX(int posX) {
		if (posX < 0)
			posX = 0;
		else if (posX + spriteWidth >= screenWidth)
			posX = screenWidth - spriteWidth - 1;

		spritePosX = posX;
	}

	public void setPosY(int posY) {
		if (posY < 0)
			posY = 0;
		else if (posY + spriteHeight >= screenHeight)
			posY = screenHeight - spriteHeight - 1;

		spritePosY = posY;
	}

	public void setPos(int posX, int posY) {
		setPosX(posX);
		setPosY(posY);
	}

	public void setPos(Point pos) {
		setPosX(pos.x);
		setPosY(pos.y);
	}

	public Rect getPosRect() {
		return new Rect(spritePosX, spritePosY, spritePosX + spriteWidth,
				spritePosY + spriteHeight);
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean bActive) {
		isActive = bActive;
	}

	public boolean isInside(int posX, int posY) {
		Rect posRect = getPosRect();

		return posRect.contains(posX, posY);
	}

	public boolean isInside(Point pos) {
		return isInside(pos.x, pos.y);
	}

}
