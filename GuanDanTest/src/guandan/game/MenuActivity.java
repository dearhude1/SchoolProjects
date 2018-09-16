package guandan.game;

import guandan.audio.AudioManager;
import guandan.audio.SoundsName;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;
import android.widget.ImageView;

public class MenuActivity extends Activity implements OnClickListener {

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			AudioManager.stopMusic();
		}
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * menuView中的控件Button
	 */
	private Button quickButton;
	private Button helpButton;
	private Button settingButton;
	private Button quitButton;

	private ImageView iv;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/**
		 * 设置全屏
		 */
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		DisplayMetrics disMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(disMetrics);

		iv = new ImageView(this);
		iv.setBackgroundResource(R.drawable.logo);
		setContentView(iv);

		Animation mAlphaAnimation = new AlphaAnimation(0.1f, 1.0f);
		mAlphaAnimation.setDuration(2000);

		mAlphaAnimation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation arg0) {
				// TODO Auto-generated method stub
				setContentView(R.layout.menu);
				initButtons();
			}

			@Override
			public void onAnimationRepeat(Animation arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationStart(Animation arg0) {
				// TODO Auto-generated method stub

			}

		});

		iv.startAnimation(mAlphaAnimation);
		ImageLoader.createInstance(this, disMetrics.widthPixels,
				disMetrics.heightPixels);

		AudioManager.initInstance(this);
		AudioManager.startMusic(SoundsName.MENU_MUSIC);

	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		
	}

	@Override
	protected void onStop() {
		super.onStop();

	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	private void initButtons() {

		quickButton = (Button) findViewById(R.id.quickgame_button);
		quickButton.setOnClickListener(this);

		helpButton = (Button) findViewById(R.id.help_button);
		helpButton.setOnClickListener(this);

		settingButton = (Button) findViewById(R.id.setting_button);
		settingButton.setOnClickListener(this);

		quitButton = (Button) findViewById(R.id.quit_button);
		quitButton.setOnClickListener(this);

	}

	@Override
	public void onClick(View view) {
		AudioManager.getInstance().startSound(SoundsName.CLICK_BUTTON_SOUND);
		if (view == quickButton) {
			Intent intent = new Intent(MenuActivity.this, GameActivity.class);
			startActivity(intent);
		} else if (view == helpButton) {
			Intent intent = new Intent(MenuActivity.this, HelpActivity.class);
			startActivity(intent);
		} else if (view == settingButton) {
			Intent intent = new Intent(MenuActivity.this, SettingActivity.class);
			startActivity(intent);
		} else if (view == quitButton) {
			System.exit(0);
		}
	}

}