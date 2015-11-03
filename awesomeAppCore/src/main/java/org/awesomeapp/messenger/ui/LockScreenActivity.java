package org.awesomeapp.messenger.ui;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.widget.ViewFlipper;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import info.guardianproject.cacheword.PassphraseSecrets;
import info.guardianproject.otr.app.im.R;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.RouterActivity;
import org.awesomeapp.messenger.ui.legacy.ThemeableActivity;
import org.awesomeapp.messenger.util.SecureMediaStore;

import info.guardianproject.util.Languages;

public class LockScreenActivity extends ThemeableActivity implements ICacheWordSubscriber {
    private static final String TAG = "LockScreenActivity";

    private final static int MIN_PASS_LENGTH = 4;
    // private final static int MAX_PASS_ATTEMPTS = 3;
    // private final static int PASS_RETRY_WAIT_TIMEOUT = 30000;

    private EditText mEnterPassphrase;
    private EditText mNewPassphrase;
    private EditText mConfirmNewPassphrase;
    private View mViewCreatePassphrase;
    private View mViewEnterPassphrase;

    private CacheWordHandler mCacheWord;
    private String mPasswordError;
    private TwoViewSlider mSlider;

    private ImApp mApp;
    private Button mBtnCreate;
    private Button mBtnSkip;

    public static final String ACTION_CHANGE_PASSPHRASE = "cp";

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mApp = (ImApp)getApplication();

        getSupportActionBar().hide();

        setContentView(R.layout.activity_lock_screen);

        mCacheWord = new CacheWordHandler(mApp, (ICacheWordSubscriber)this);
        mCacheWord.connectToService();

        mViewCreatePassphrase = findViewById(R.id.llCreatePassphrase);
        mViewEnterPassphrase = findViewById(R.id.llEnterPassphrase);

        mEnterPassphrase = (EditText) findViewById(R.id.editEnterPassphrase);

        mNewPassphrase = (EditText) findViewById(R.id.editNewPassphrase);
        mConfirmNewPassphrase = (EditText) findViewById(R.id.editConfirmNewPassphrase);
        ViewFlipper vf = (ViewFlipper) findViewById(R.id.viewFlipper1);
        LinearLayout flipView1 = (LinearLayout) findViewById(R.id.flipView1);
        LinearLayout flipView2 = (LinearLayout) findViewById(R.id.flipView2);

        mSlider = new TwoViewSlider(vf, flipView1, flipView2, mNewPassphrase, mConfirmNewPassphrase);



        mBtnSkip = (Button)findViewById(R.id.btnSkip);
        mBtnSkip.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                mHandler.post(new Runnable ()
                {
                   public void run ()
                   {
                       LockScreenActivity.this.finish();
                   }
                });

            }
        });

        if (getIntent() != null && getIntent().getAction() != null)
            if (getIntent().getAction().equals(ACTION_CHANGE_PASSPHRASE))
            {
                changePassphrase();
            }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCacheWord.detach();

    }

    @Override
    protected void onResume() {
        super.onResume();
        mCacheWord.reattach();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCacheWord.disconnectFromService();
    }

    private boolean newEqualsConfirmation() {
        return mNewPassphrase.getText().toString()
                .equals(mConfirmNewPassphrase.getText().toString());
    }

    private void showValidationError() {
        Toast.makeText(LockScreenActivity.this, mPasswordError, Toast.LENGTH_LONG).show();
        mNewPassphrase.requestFocus();
    }

    private void showInequalityError() {
        Toast.makeText(LockScreenActivity.this,
                getString(R.string.lock_screen_passphrases_not_matching),
                Toast.LENGTH_SHORT).show();
        clearNewFields();
    }

    private void clearNewFields() {
        mNewPassphrase.getEditableText().clear();
        mConfirmNewPassphrase.getEditableText().clear();
    }

    private boolean isPasswordValid() {
        return validatePassword(mNewPassphrase.getText().toString().toCharArray());
    }

    private boolean isPasswordFieldEmpty() {
        return mNewPassphrase.getText().toString().length() == 0;
    }

    private boolean isConfirmationFieldEmpty() {
        return mConfirmNewPassphrase.getText().toString().length() == 0;
    }

    private void initializeWithPassphrase() {
        try {
            String passphrase = mNewPassphrase.getText().toString();
            if (!passphrase.isEmpty()) {

                PassphraseSecrets p = (PassphraseSecrets)mCacheWord.getCachedSecrets();
                mCacheWord.changePassphrase(p,passphrase.toCharArray());

                //now remove the temp passphrase if it exists
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
                if (settings.contains(ImApp.PREFERENCE_KEY_TEMP_PASS))
                    settings.edit().remove(ImApp.PREFERENCE_KEY_TEMP_PASS).commit();

                finish();

            }

        } catch (Exception e) {
            // TODO initialization failed
            Log.e(TAG, "Cacheword pass initialization failed: " + e.getMessage());
        }
    }

    private void changePassphrase() {
        // Passphrase is not set, so allow the user to create one

        View viewCreatePassphrase = findViewById(R.id.llCreatePassphrase);
        viewCreatePassphrase.setVisibility(View.VISIBLE);
        mViewEnterPassphrase.setVisibility(View.GONE);

        mNewPassphrase.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
            {
                if (actionId == EditorInfo.IME_NULL || actionId == EditorInfo.IME_ACTION_DONE)
                {
                    if (!isPasswordValid())
                        showValidationError();
                    else
                        mSlider.showConfirmationField();
                }
                return false;
            }
        });

        mConfirmNewPassphrase.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_NULL || actionId == EditorInfo.IME_ACTION_DONE) {
                    if (!newEqualsConfirmation()) {
                        showInequalityError();
                        mSlider.showNewPasswordField();
                    }
                }
                return false;
            }
        });

        mBtnCreate = (Button) findViewById(R.id.btnCreate);
        mBtnCreate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // validate
                if (!isPasswordValid()) {
                    showValidationError();
                    mSlider.showNewPasswordField();
                } else if (isConfirmationFieldEmpty() && !isPasswordFieldEmpty()) {
                    mBtnSkip.setVisibility(View.GONE);
                    mSlider.showConfirmationField();
                    mBtnCreate.setText(R.string.lock_screen_confirm_passphrase);
                } else if (!newEqualsConfirmation()) {
                    showInequalityError();
                } else if (!isConfirmationFieldEmpty() && !isPasswordFieldEmpty()) {
                    initializeWithPassphrase();
                }
            }
        });


    }

    private void promptPassphrase() {
        mViewCreatePassphrase.setVisibility(View.GONE);
        mViewEnterPassphrase.setVisibility(View.VISIBLE);

        mEnterPassphrase.setOnEditorActionListener(new OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
            {
                if (actionId == EditorInfo.IME_NULL || actionId == EditorInfo.IME_ACTION_GO)
                {
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    Handler threadHandler = new Handler();
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0, new ResultReceiver(
                            threadHandler)
                    {
                        @Override
                        protected void onReceiveResult(int resultCode, Bundle resultData)
                        {
                            super.onReceiveResult(resultCode, resultData);

                            if (mEnterPassphrase.getText().toString().length() == 0)
                                return;
                            // Check passphrase
                            try {
                                char[] passphrase = mEnterPassphrase.getText().toString().toCharArray();

                                mCacheWord.setPassphrase(passphrase);
                            } catch (Exception e) {
                                mEnterPassphrase.setText("");
                                // TODO implement try again and wipe if fail
                                Log.e(TAG, "Cacheword pass verification failed: " + e.getMessage());
                                return;
                            }

                        }
                    });
                    return true;
                }
                return false;
            }
        });
    }

    private boolean validatePassword(char[] pass)
    {

        if (pass.length < MIN_PASS_LENGTH && pass.length != 0)
        {
            // should we support some user string message here?
            mPasswordError = getString(R.string.pass_err_length);
            return false;
        }

        return true;
    }

    public class TwoViewSlider {

        private boolean firstIsShown = true;
        private ViewFlipper flipper;
        private LinearLayout container1;
        private LinearLayout container2;
        private View firstView;
        private View secondView;
        private Animation pushRightIn;
        private Animation pushRightOut;
        private Animation pushLeftIn;
        private Animation pushLeftOut;

        public TwoViewSlider(ViewFlipper flipper, LinearLayout container1, LinearLayout container2,
                View view1, View view2) {
            this.flipper = flipper;
            this.container1 = container1;
            this.container2 = container2;
            this.firstView = view1;
            this.secondView = view2;

            pushRightIn = AnimationUtils.loadAnimation(LockScreenActivity.this, R.anim.push_right_in);
            pushRightOut = AnimationUtils.loadAnimation(LockScreenActivity.this, R.anim.push_right_out);
            pushLeftIn = AnimationUtils.loadAnimation(LockScreenActivity.this, R.anim.push_left_in);
            pushLeftOut = AnimationUtils.loadAnimation(LockScreenActivity.this, R.anim.push_left_out);

        }

        public void showNewPasswordField() {
            if (firstIsShown)
                return;

            flipper.setInAnimation(pushRightIn);
            flipper.setOutAnimation(pushRightOut);
            flip();
        }

        public void showConfirmationField() {
            if (!firstIsShown)
                return;

            flipper.setInAnimation(pushLeftIn);
            flipper.setOutAnimation(pushLeftOut);
            flip();
        }

        private void flip() {
            if (firstIsShown) {
                firstIsShown = false;
                container2.removeAllViews();
                container2.addView(secondView);
            } else {
                firstIsShown = true;
                container1.removeAllViews();
                container1.addView(firstView);
            }
            flipper.showNext();
        }
    }

    @Override
    public void onCacheWordUninitialized() {

       //this should never happen

    }

    @Override
    public void onCacheWordLocked() {
        promptPassphrase();
    }

    @Override
    public void onCacheWordOpened() {
        mCacheWord.setTimeout(0);
        setResult(RESULT_OK);
        finish();

    }


}