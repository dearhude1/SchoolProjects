/**
 * Sprite.java
 * @author ��ԣ��
 * March 20th 2011
 * 
 * Sprite����ͼ������Ϸ�е�һ������,������ʵ���ƶ�ͼ��,���Ŷ����ȹ���
 * ���׳�"�ӻ���"����"����"
 * Sprite����˾���ͼ������֮�⻹����:����λ��,���,�߶�,�ٶ�,֡��������
 * λ�����Ծ���Sprite��ͼ������Ļ�ϻ��Ƶķ�λ
 * ��Ⱥ͸߶Ⱦ���Sprite������ڻ����ϵĳ����С
 * �ٶ����Կ��Կ���ͼ����ƶ�,ͼ��֡����ص����Կ��Կ��ƶ����Ĳ���
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
	 * �ӻ����͸�
	 */
	protected int spriteWidth;
	protected int spriteHeight;

	/**
	 * �ӻ�������Ļ�ϵ�����
	 */
	protected int spritePosX;
	protected int spritePosY;

	/**
	 * �ӻ����ٶ�,���ű���
	 */
	protected int spriteVelocityX;
	protected int spriteVelocityY;

	/**
	 * �ӻ����ͼ��
	 */
	protected Bitmap spriteImage;

	/**
	 * ����Ϸ��ͼ�������������
	 */
	protected ImageLoader imageLoader;

	/**
	 * ��Ϸ��Ļ��͸�
	 */
	protected int screenWidth;
	protected int screenHeight;

	/**
	 * ��ʾ�ӻ����Ƿ��� "���"����ʾ���� "�����"�ͽ�������
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
	 * ���Ƹ��ӻ����ͼ�� �����������о���ʵ�� ����ʵ������������������
	 * 
	 * @param canvas
	 *            :��������
	 */
	public void onDraw(Canvas canvas) {

	}

	/**
	 * ���¸��ӻ��� �����������о���ʵ�� ����ʵ������������������
	 */
	public void update() {

	}

	/**
	 * ���ڸ��ӻ����ϵĴ����¼�������Ӧ �����������о���ʵ�� ����ʵ������������������
	 * 
	 * @param event
	 *            :�����¼�
	 */
	public void onTouchEvent(MotionEvent event) {

	}

	/**
	 * ���ø��ӻ����ͼ��spriteImage �����������о���ʵ�� ����ʵ������������������
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
