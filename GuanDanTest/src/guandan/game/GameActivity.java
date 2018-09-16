package guandan.game;

import guandan.audio.AudioManager;
import guandan.audio.SoundsName;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class GameActivity extends Activity implements OnClickListener {

	

	private GameView gameView;

	private Button pattern_button;
	private Button point_button;
	private Button type_button;
	private Button begin_button;
	private Button lead_button;
	private Button pass_button;
	private Button payback_button;
	private Button pay_button;
	private Button reset_button;

	private TextView playerGrade_txtView;
	private TextView opponentGrade_txtView;
	private TextView currentGrade_txtView;
	
	private AudioManager am;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		am=AudioManager.getInstance();
		setContentView(R.layout.game);
		initViews();
		AudioManager.startMusic(SoundsName.BATTLEBACK_MUSIC);
	}

	private void initViews() {
		gameView = (GameView) findViewById(R.id.gameView);
		pattern_button = (Button) findViewById(R.id.pattern_button);
		pattern_button.setOnClickListener(this);

		point_button = (Button) findViewById(R.id.point_button);
		point_button.setOnClickListener(this);

		type_button = (Button) findViewById(R.id.type_button);
		type_button.setOnClickListener(this);

		begin_button = (Button) findViewById(R.id.begin_button);
		begin_button.setOnClickListener(this);

		lead_button = (Button) findViewById(R.id.lead_button);
		lead_button.setOnClickListener(this);

		pass_button = (Button) findViewById(R.id.pass_button);
		pass_button.setOnClickListener(this);

		payback_button = (Button) findViewById(R.id.payback_button);
		payback_button.setOnClickListener(this);

		pay_button = (Button) findViewById(R.id.pay_button);
		pay_button.setOnClickListener(this);

		reset_button = (Button) findViewById(R.id.reset_button);
		reset_button.setOnClickListener(this);

		playerGrade_txtView = (TextView) findViewById(R.id.playerGrade_txtView);
		opponentGrade_txtView = (TextView) findViewById(R.id.opponentGrade_txtView);
		currentGrade_txtView = (TextView) findViewById(R.id.currentGrade_txtView);

	}

	public void setViews_VISIBLE(int id) {
		View view = findViewById(id);
		view.setVisibility(View.VISIBLE);
	}

	public void setViews_INVISIBLE(int id) {
		View view = findViewById(id);
		view.setVisibility(View.INVISIBLE);
	}


	public void refreshViews(int playGrade,int opeGrade,int currentGrade) {
		String text = String.format(this.getString(R.string.playerGrade_txt), playGrade); 
		playerGrade_txtView.setText(text);
		
		text = String.format(this.getString(R.string.opponentGrade_txt), opeGrade); 
		opponentGrade_txtView.setText(text);
		
		text = String.format(this.getString(R.string.currentGrade_txt), currentGrade); 
		currentGrade_txtView.setText(text);
	}
	
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
//		gameView.pause();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
//		gameView.resume();
	}

	@Override
	public void onClick(View view) {
		// TODO Auto-generated method stub
		am.startSound(SoundsName.CLICK_BUTTON_SOUND);
		if (view == pattern_button) {
			gameView.onPatternClick();
		} else if (view == point_button) {
			gameView.onPointClick();
		} else if (view == type_button) {
			gameView.onTypeClick();
		} else if (view == begin_button) {
			gameView.onBeginClick();
		} else if (view == lead_button) {
			gameView.onLeadClick();
		} else if (view == pass_button) {
			gameView.onPassClick();
		} else if (view == payback_button) {
			gameView.onPayBackClick();
		} else if (view == pay_button) {
			gameView.onPayClick();
		} else if (view == reset_button) {
			gameView.onResetClick();
		}
	}

	public boolean onFling(MotionEvent event1, MotionEvent event2,
			float velocityX, float velocityY) {
		Log.i("Gesture Support", "Fling");

		if (event1.getAction() == MotionEvent.ACTION_DOWN)
			Log.i("Gesture Support", "Down");
		if (event2.getAction() == MotionEvent.ACTION_UP)
			Log.i("Gesture Support", "Up");

		gameView.onFling(event1, event2, velocityX, velocityY);

		return true;
	}

}
