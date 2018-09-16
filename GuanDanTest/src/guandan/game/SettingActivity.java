package guandan.game;

/**   
 * @保存方式：SharedPreference   
 * @注意：SharedPreference 可以跨程序包使用。
 * @操作模式: Context.MODE_PRIVATE：新内容覆盖原内容   
 *            Context.MODE_APPEND：新内容追加到原内容后   
 *            Context.MODE_WORLD_READABLE：允许其他应用程序读取   
 *            Context.MODE_WORLD_WRITEABLE：允许其他应用程序写入，会覆盖原数据。   
 */

import guandan.audio.AudioManager;
import guandan.audio.SoundsName;
import guandan.constants.AIType;

import java.util.ArrayList;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateInterpolator;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ToggleButton;
import android.widget.CompoundButton;

public class SettingActivity extends Activity implements OnClickListener {
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		onSave();
		super.onStop();
	}

	private ViewGroup mContainer;
	private ViewGroup settingViewsGroup;

	private Button lastDesktop_Button;
	private Button nextDesktop_Button;
	private ImageView desktop_ImageView;
	private ImageView preview_ImageView;

	private Button back_Button;
	private ToggleButton music_ToggleButton;
	private ToggleButton sound_ToggleButton;

	private RadioGroup AILevel_RadioGroup;
	private RadioButton[] AI_RadioButtons;


	private ArrayList<Integer> desktopImageList;
	private int currentDesktop;
	private int AILevel;
	private boolean music_on;
	private boolean sound_on;

	private SharedPreferences setting;
	
	private AudioManager am;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.setting);
		am=AudioManager.getInstance();
		desktopImageList = ImageLoader.getInstance().getDesktopImageList();

		setting = getSharedPreferences("Guandan_Setting", MODE_PRIVATE);

		currentDesktop = setting.getInt("desktop", 0);
		music_on=setting.getBoolean("music", true);
		sound_on=setting.getBoolean("sound", true);
		AILevel=setting.getInt("AILevel", AIType.easy_AI);

		initViews();
	}

	private void initViews() {
		mContainer = (ViewGroup) findViewById(R.id.container);
		// Since we are caching large views, we want to keep their cache
		// between each animation
		mContainer
				.setPersistentDrawingCache(ViewGroup.PERSISTENT_ANIMATION_CACHE);
		settingViewsGroup = (ViewGroup) findViewById(R.id.settingViewsGroup);

		AILevel_RadioGroup = (RadioGroup) findViewById(R.id.AILevel_Group);
		AILevel_RadioGroup
				.setOnCheckedChangeListener(new SettingCheckedChangeListener());
		AI_RadioButtons=new RadioButton[AILevel_RadioGroup.getChildCount()];
		
		AI_RadioButtons[0] = (RadioButton) findViewById(R.id.easy);
		AI_RadioButtons[1] = (RadioButton) findViewById(R.id.normal);
		AI_RadioButtons[2] = (RadioButton) findViewById(R.id.hard);
		
		for(int i=0;i<AI_RadioButtons.length;i++){
			if(i==AILevel)
				AI_RadioButtons[i].setChecked(true);
		}
			
		lastDesktop_Button = (Button) findViewById(R.id.lastdesktop_button);
		lastDesktop_Button.setOnClickListener(this);

		nextDesktop_Button = (Button) findViewById(R.id.nextdesktop_button);
		nextDesktop_Button.setOnClickListener(this);

		back_Button = (Button) findViewById(R.id.cmback_button);
		back_Button.setOnClickListener(this);

		music_ToggleButton = (ToggleButton) findViewById(R.id.music_button);
		music_ToggleButton
				.setOnCheckedChangeListener(new SettingCheckedChangeListener());
		if(music_on)
			music_ToggleButton.setChecked(true);

		sound_ToggleButton = (ToggleButton) findViewById(R.id.sound_button);
		sound_ToggleButton
				.setOnCheckedChangeListener(new SettingCheckedChangeListener());
		if(sound_on)
			sound_ToggleButton.setChecked(true);

		desktop_ImageView = (ImageView) findViewById(R.id.desktop_iv);
		preview_ImageView = (ImageView) findViewById(R.id.desktop_preview);
		// Prepare the ImageView
		desktop_ImageView.setClickable(true);
		desktop_ImageView.setOnClickListener(this);
		desktop_ImageView.setBackgroundResource(desktopImageList.get(currentDesktop));

		preview_ImageView.setClickable(true);
		preview_ImageView.setFocusable(true);
		preview_ImageView.setOnClickListener(this);

	}

	@Override
	public void onClick(View view) {
		// TODO Auto-generated method stub
		am.startSound(SoundsName.CLICK_BUTTON_SOUND);
		if (view == desktop_ImageView) {
			preview_ImageView.setBackgroundResource(desktopImageList
					.get(currentDesktop));
			applyRotation(currentDesktop, 0, 90);
		} else if (view == preview_ImageView) {
			applyRotation(-1, 180, 90);
		} else if (view == lastDesktop_Button) {
			if (currentDesktop > 0)
				currentDesktop--;
			else
				currentDesktop = desktopImageList.size() - 1;
			desktop_ImageView.setBackgroundResource(desktopImageList
					.get(currentDesktop));
		} else if (view == nextDesktop_Button) {
			if (currentDesktop < desktopImageList.size() - 1)
				currentDesktop++;
			else
				currentDesktop = 0;
			desktop_ImageView.setBackgroundResource(desktopImageList
					.get(currentDesktop));
		} else if (view == back_Button) {
			SettingActivity.this.finish();
		}
	}

	class SettingCheckedChangeListener implements
			CompoundButton.OnCheckedChangeListener,
			RadioGroup.OnCheckedChangeListener {

		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			// TODO Auto-generated method stub
			am.startSound(SoundsName.CLICK_BUTTON_SOUND);
			if (buttonView == music_ToggleButton) {
				if (isChecked) {
					
					System.out.println("Music On");
					AudioManager.musicOn();
				} else {
					
					System.out.println("Music Off");
					AudioManager.musicOff();
				}

			} else if (buttonView == sound_ToggleButton) {
				if (isChecked) {
					AudioManager.soundOn();
				} else
					AudioManager.soundOff();
			}

		}

		// 处理选择RadioButton的CheckedChanged事件
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			am.startSound(SoundsName.CLICK_BUTTON_SOUND);
			for(int i=0;i<AI_RadioButtons.length;i++){
				if(checkedId==AI_RadioButtons[i].getId()){
					AI_RadioButtons[i].setChecked(true);
					AILevel=i;
				}
			}
		}
	}

	/**
	 * Setup a new 3D rotation on the container view.
	 * 
	 * @param position
	 *            the item that was clicked to show a picture, or -1 to show the
	 *            list
	 * @param start
	 *            the start angle at which the rotation must begin
	 * @param end
	 *            the end angle of the rotation
	 */
	private void applyRotation(int position, float start, float end) {
		// Find the center of the container
		final float centerX = mContainer.getWidth() / 2.0f;
		final float centerY = mContainer.getHeight() / 2.0f;

		// Create a new 3D rotation with the supplied parameter
		// The animation listener is used to trigger the next animation
		final Rotate3dAnimation rotation = new Rotate3dAnimation(start, end,
				centerX, centerY, 310.0f, true);
		rotation.setDuration(500);
		rotation.setFillAfter(true);
		rotation.setInterpolator(new AccelerateInterpolator());
		rotation.setAnimationListener(new DisplayNextView(position));

		mContainer.startAnimation(rotation);
	}


	private void onSave() {
		setting.edit().putInt("AILevel", AILevel)
				.putInt("desktop", currentDesktop)
				.putBoolean("music", music_ToggleButton.isChecked())
				.putBoolean("sound", sound_ToggleButton.isChecked())
				.commit();
		
		System.out.println(AILevel);
	}

	/**
	 * This class listens for the end of the first half of the animation. It
	 * then posts a new action that effectively swaps the views when the
	 * container is rotated 90 degrees and thus invisible.
	 */
	private final class DisplayNextView implements Animation.AnimationListener {
		private final int mPosition;

		private DisplayNextView(int position) {
			mPosition = position;
		}

		public void onAnimationStart(Animation animation) {
		}

		public void onAnimationEnd(Animation animation) {
			mContainer.post(new SwapViews(mPosition));
		}

		public void onAnimationRepeat(Animation animation) {
		}
	}

	/**
	 * An animation that rotates the view on the Y axis between two specified
	 * angles. This animation also adds a translation on the Z axis (depth) to
	 * improve the effect.
	 */
	public class Rotate3dAnimation extends Animation {
		private final float mFromDegrees;
		private final float mToDegrees;
		private final float mCenterX;
		private final float mCenterY;
		private final float mDepthZ;
		private final boolean mReverse;
		private Camera mCamera;

		/**
		 * Creates a new 3D rotation on the Y axis. The rotation is defined by
		 * its start angle and its end angle. Both angles are in degrees. The
		 * rotation is performed around a center point on the 2D space, definied
		 * by a pair of X and Y coordinates, called centerX and centerY. When
		 * the animation starts, a translation on the Z axis (depth) is
		 * performed. The length of the translation can be specified, as well as
		 * whether the translation should be reversed in time.
		 * 
		 * @param fromDegrees
		 *            the start angle of the 3D rotation
		 * @param toDegrees
		 *            the end angle of the 3D rotation
		 * @param centerX
		 *            the X center of the 3D rotation
		 * @param centerY
		 *            the Y center of the 3D rotation
		 * @param reverse
		 *            true if the translation should be reversed, false
		 *            otherwise
		 */
		public Rotate3dAnimation(float fromDegrees, float toDegrees,
				float centerX, float centerY, float depthZ, boolean reverse) {
			mFromDegrees = fromDegrees;
			mToDegrees = toDegrees;
			mCenterX = centerX;
			mCenterY = centerY;
			mDepthZ = depthZ;
			mReverse = reverse;
		}

		@Override
		public void initialize(int width, int height, int parentWidth,
				int parentHeight) {
			super.initialize(width, height, parentWidth, parentHeight);
			mCamera = new Camera();
		}

		@Override
		protected void applyTransformation(float interpolatedTime,
				Transformation t) {
			final float fromDegrees = mFromDegrees;
			float degrees = fromDegrees
					+ ((mToDegrees - fromDegrees) * interpolatedTime);

			final float centerX = mCenterX;
			final float centerY = mCenterY;
			final Camera camera = mCamera;

			final Matrix matrix = t.getMatrix();

			camera.save();
			if (mReverse) {
				camera.translate(0.0f, 0.0f, mDepthZ * interpolatedTime);
			} else {
				camera.translate(0.0f, 0.0f, mDepthZ
						* (1.0f - interpolatedTime));
			}
			camera.rotateY(degrees);
			camera.getMatrix(matrix);
			camera.restore();

			matrix.preTranslate(-centerX, -centerY);
			matrix.postTranslate(centerX, centerY);
		}
	}

	/**
	 * This class is responsible for swapping the views and start the second
	 * half of the animation.
	 */
	private final class SwapViews implements Runnable {
		private final int mPosition;

		public SwapViews(int position) {
			mPosition = position;
		}

		public void run() {
			final float centerX = mContainer.getWidth() / 2.0f;
			final float centerY = mContainer.getHeight() / 2.0f;
			Rotate3dAnimation rotation;

			if (mPosition > -1) {
				settingViewsGroup.setVisibility(View.GONE);
				preview_ImageView.setVisibility(View.VISIBLE);
				preview_ImageView.requestFocus();

				rotation = new Rotate3dAnimation(90, 180, centerX, centerY,
						310.0f, false);
			} else {
				preview_ImageView.setVisibility(View.GONE);
				settingViewsGroup.setVisibility(View.VISIBLE);
				settingViewsGroup.requestFocus();

				rotation = new Rotate3dAnimation(90, 0, centerX, centerY,
						310.0f, false);
			}

			rotation.setDuration(500);
			rotation.setFillAfter(true);
			rotation.setInterpolator(new DecelerateInterpolator());

			mContainer.startAnimation(rotation);
		}
	}
}
