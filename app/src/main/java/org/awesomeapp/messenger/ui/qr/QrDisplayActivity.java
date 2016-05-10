package org.awesomeapp.messenger.ui.qr;

import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.google.zxing.Result;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.ui.BaseActivity;
import org.awesomeapp.messenger.ui.onboarding.OnboardingManager;

import java.util.ArrayList;

import im.zom.messenger.R;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_NOSENSOR;
import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;
import static android.view.Gravity.CENTER;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.widget.ImageView.ScaleType.FIT_CENTER;
import static android.widget.LinearLayout.HORIZONTAL;
import static android.widget.LinearLayout.VERTICAL;

@SuppressWarnings("deprecation")
public class QrDisplayActivity extends BaseActivity {

	private static String TAG = QrDisplayActivity.class.getPackage().getName();


	private LinearLayout layoutMain = null;

	private boolean gotResult = false;

	private Intent dataResult = new Intent();
	ArrayList<String> resultStrings = new ArrayList<String>();

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);

		setRequestedOrientation(SCREEN_ORIENTATION_NOSENSOR);

		getSupportActionBar().hide();

		String qrData = getIntent().getStringExtra(Intent.EXTRA_TEXT);

		ImageView qrCodeView = new ImageView(this);

		qrCodeView.setScaleType(FIT_CENTER);
		qrCodeView.setBackgroundColor(WHITE);
		qrCodeView.setLayoutParams(new LayoutParams(MATCH_PARENT,
				MATCH_PARENT, 1f));

		Display display = getWindowManager().getDefaultDisplay();
		boolean portrait = display.getWidth() < display.getHeight();
		layoutMain = new LinearLayout(this);
		if(portrait) layoutMain.setOrientation(VERTICAL);
		else layoutMain.setOrientation(HORIZONTAL);
		layoutMain.setWeightSum(1);
		layoutMain.addView(qrCodeView);
		setContentView(layoutMain);

		new QrGenAsyncTask(this, qrCodeView, display.getWidth()).executeOnExecutor(ImApp.sThreadPoolExecutor,qrData);
	}





}