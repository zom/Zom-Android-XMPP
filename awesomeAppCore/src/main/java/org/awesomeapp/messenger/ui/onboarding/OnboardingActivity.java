package org.awesomeapp.messenger.ui.onboarding;

import org.awesomeapp.messenger.crypto.OtrAndroidKeyManagerImpl;
import info.guardianproject.otr.app.im.R;
import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.tasks.AddContactAsyncTask;
import org.awesomeapp.messenger.ui.legacy.SignInHelper;
import org.awesomeapp.messenger.ui.legacy.SimpleAlertHandler;
import org.awesomeapp.messenger.ui.legacy.ThemeableActivity;
import org.awesomeapp.messenger.util.Languages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
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
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.ViewFlipper;

import org.awesomeapp.messenger.MainActivity;

public class OnboardingActivity extends ThemeableActivity {

    private ViewFlipper mViewFlipper;
    private EditText mEditUsername;
    private View mSetupProgress;
    private TextView mSetupStatus;
    private Button mSetupButton;

    private String mRequestedUserName;
    private String mFingerprint;

    private SimpleAlertHandler mHandler;

    private static final String USERNAME_ONLY_ALPHANUM = "[^A-Za-z0-9]";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.awesome_onboarding);        
        getSupportActionBar().hide();
        getSupportActionBar().setTitle("");

        mHandler = new SimpleAlertHandler(this);

        mViewFlipper = (ViewFlipper) findViewById(R.id.viewFlipper1);
        mEditUsername = (EditText)findViewById(R.id.edtNewName);
        
        setAnimLeft();
        
        Button btnShowCreate = (Button) findViewById(R.id.btnShowRegister);
        btnShowCreate.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                setAnimLeft();
                showSetupScreen();
            }

        });

        Button btnShowLogin = (Button) findViewById(R.id.btnShowLogin);
        btnShowLogin.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                setAnimLeft();
                showLoginScreen();
            }

        });
        
        // set up language chooser button
        ImageButton languageButton = (ImageButton) findViewById(R.id.languageButton);
        languageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity activity = OnboardingActivity.this;
                final Languages languages = Languages.get(activity);
                final ArrayAdapter<String> languagesAdapter = new ArrayAdapter<String>(activity,
                        android.R.layout.simple_list_item_single_choice, languages.getAllNames());
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setIcon(R.drawable.ic_settings_language);
                builder.setTitle(R.string.KEY_PREF_LANGUAGE_TITLE);
                builder.setAdapter(languagesAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int position) {
                        String[] languageCodes = languages.getSupportedLocales();
                        resetLanguage(languageCodes[position]);
                        dialog.dismiss();
                    }
                });
                builder.show();
            }
        });
        
        mEditUsername.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_NULL || actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    Handler threadHandler = new Handler();
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0, new ResultReceiver(
                            threadHandler) {
                        @Override
                        protected void onReceiveResult(int resultCode, Bundle resultData) {
                            super.onReceiveResult(resultCode, resultData);

                            mRequestedUserName = mEditUsername.getText().toString();

                            if (mRequestedUserName.length() > 0) {
                                startAccountSetup();
                            }



                        }
                    });
                    return true;
                }

                return false;
            }
        });
        
        Button btnInviteSms = (Button)findViewById(R.id.btnInviteSMS);
        btnInviteSms.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                doInviteSMS();

            }

        });

        Button btnInviteShare = (Button)findViewById(R.id.btnInviteShare);
        btnInviteShare.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v) {
               
                doInviteShare();
                
            }
            
        });

        Button btnInviteQR = (Button)findViewById(R.id.btnInviteScan);
        btnInviteQR.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v) {
               
                doInviteScan();
                
            }
            
        });

        Button btnInviteSkip = (Button)findViewById(R.id.btnInviteSkip);
        btnInviteSkip.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                showMainScreen();

            }

        });

        mSetupButton = (Button)findViewById(R.id.btnRegister);
        mSetupButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                doAccountRegister();

            }

        });

        Button btnSignIn = (Button)findViewById(R.id.btnSignIn);
        btnSignIn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

               doExistingAccountRegister();

            }

        });

    }

    private void setAnimLeft ()
    {
        Animation animIn = AnimationUtils.loadAnimation(this, R.anim.push_left_in);
        Animation animOut = AnimationUtils.loadAnimation(this, R.anim.push_left_out);
        mViewFlipper.setInAnimation(animIn);
        mViewFlipper.setOutAnimation(animOut);
    }
    
    private void setAnimRight ()
    {
        Animation animIn = AnimationUtils.loadAnimation(OnboardingActivity.this, R.anim.push_right_in);
        Animation animOut = AnimationUtils.loadAnimation(OnboardingActivity.this, R.anim.push_right_out);
        mViewFlipper.setInAnimation(animIn);
        mViewFlipper.setOutAnimation(animOut);
    }    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
        case android.R.id.home:
                        
            if (mViewFlipper.getCurrentView().getId()==R.id.flipView2)
            { 
                setAnimRight ();
                showSplashScreen();
            }
            else if (mViewFlipper.getCurrentView().getId()==R.id.flipView3)
            { 
                setAnimRight ();
                mViewFlipper.showPrevious();
                getSupportActionBar().setTitle("");                
            }
            
            
            
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSplashScreen ()
    {
        setAnimRight ();
        getSupportActionBar().hide();
        getSupportActionBar().setTitle("");
        mViewFlipper.showPrevious();
    }
    
    private void showSetupScreen ()
    {
        
        mViewFlipper.showNext();
        
        getSupportActionBar().show();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    private void showLoginScreen ()
    {

        mViewFlipper.setDisplayedChild(3);

        getSupportActionBar().show();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }
    
    private void startAccountSetup()
    {
        setAnimLeft();
        mSetupProgress = findViewById(R.id.progressNewUser);
        mSetupProgress.setVisibility(View.VISIBLE);
        
        mSetupStatus = (TextView)findViewById(R.id.statusNewUser);
        mSetupStatus.setVisibility(View.VISIBLE);

        findViewById(R.id.progressImage).setVisibility(View.VISIBLE);

        mRequestedUserName = mRequestedUserName.replaceAll(USERNAME_ONLY_ALPHANUM, "").toLowerCase(Locale.ENGLISH);

        new FindServerTask ().execute(mRequestedUserName);
    }

    private void resetLanguage(String language) {
        ((ImApp) getApplication()).setNewLocale(this, language);
        Intent intent = getIntent();
        intent.setClass(this, OnboardingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();
        overridePendingTransition(0, 0);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }
    
    private class FindServerTask extends AsyncTask<String, Void, OnboardingAccount> {
        @Override
        protected OnboardingAccount doInBackground(String... username) {
            try {
                OnboardingAccount result = OnboardingManager.registerAccount(OnboardingActivity.this, mHandler, username[0]);

                String jabberId = result.username + '@' + result.domain;

                OtrAndroidKeyManagerImpl keyMan = OtrAndroidKeyManagerImpl.getInstance(OnboardingActivity.this);
                keyMan.generateLocalKeyPair(jabberId);
                mFingerprint = keyMan.getLocalFingerprint(jabberId);

                return result;
            }
            catch (Exception e)
            {
                Log.e(ImApp.LOG_TAG, "auto onboarding fail", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(OnboardingAccount account) {

            mRequestedUserName = account.username + '@' + account.domain;

            findViewById(R.id.progressImage).setVisibility(View.GONE);

            mSetupProgress.setVisibility(View.GONE);
            mSetupStatus.setText("Success. You are now ready to Zom!");
            mSetupButton.setVisibility(View.VISIBLE);

            SignInHelper signInHelper = new SignInHelper(OnboardingActivity.this, mHandler);
            signInHelper.activateAccount(account.providerId,account.accountId);
            signInHelper.signIn(account.password,account.providerId,account.accountId,true);
        }
      }

    private void doAccountRegister()
    {
        showInviteScreen ();
    }

    private void showInviteScreen ()
    {
        mViewFlipper.setDisplayedChild(2);
        getSupportActionBar().setTitle("Invite");

        TextView tv = (TextView)findViewById(R.id.statusInviteFriends);
        tv.setText("Invite your friends to connect on Zom....");
    }

    private void doInviteSMS()
    {
        String inviteString = OnboardingManager.generateInviteMessage(this, mRequestedUserName, mFingerprint);
        OnboardingManager.inviteSMSContact(this, null, inviteString);
    }
    
    private void doInviteShare()
    {

        String inviteString = OnboardingManager.generateInviteMessage(this, mRequestedUserName, mFingerprint);
        OnboardingManager.inviteShare(this, inviteString);
    }
 
    private void doInviteScan ()
    {
        String inviteString;
        try {
            inviteString = OnboardingManager.generateInviteLink(this, mRequestedUserName, mFingerprint);
            OnboardingManager.inviteScan(this, inviteString);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }

    private void showMainScreen ()
    {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void doExistingAccountRegister ()
    {
        String username = ((TextView)findViewById(R.id.edtName)).getText().toString();
        String password = ((TextView)findViewById(R.id.edtPass)).getText().toString();

        new ExistingAccountTask().execute(username, password);
    }

    private class ExistingAccountTask extends AsyncTask<String, Void, OnboardingAccount> {
        @Override
        protected OnboardingAccount doInBackground(String... account) {
            try {
                OnboardingAccount result = OnboardingManager.addExistingAccount(OnboardingActivity.this, mHandler, account[0], account[1]);

                OtrAndroidKeyManagerImpl keyMan = OtrAndroidKeyManagerImpl.getInstance(OnboardingActivity.this);
                keyMan.generateLocalKeyPair(account[0]);
                mFingerprint = keyMan.getLocalFingerprint(account[0]);

                return result;
            }
            catch (Exception e)
            {
                Log.e(ImApp.LOG_TAG, "auto onboarding fail", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(OnboardingAccount account) {

            mRequestedUserName = account.username + '@' + account.domain;

            SignInHelper signInHelper = new SignInHelper(OnboardingActivity.this, mHandler);
            signInHelper.activateAccount(account.providerId,account.accountId);
            signInHelper.signIn(account.password, account.providerId, account.accountId, true);

            showInviteScreen();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        showInviteScreen();

        ImApp mApp = (ImApp)getApplication();
        mApp.initAccountInfo();

        if (resultCode == RESULT_OK) {
            if (requestCode == OnboardingManager.REQUEST_SCAN) {

                ArrayList<String> resultScans = data.getStringArrayListExtra("result");
                for (String resultScan : resultScans)
                {

                    try {
                        //parse each string and if they are for a new user then add the user
                        String[] parts = OnboardingManager.decodeInviteLink(resultScan);

                        new AddContactAsyncTask(mApp.getDefaultProviderId(),mApp.getDefaultAccountId(), mApp).execute(parts[0],parts[1]);

                        //if they are for a group chat, then add the group
                    }
                    catch (Exception e)
                    {
                        Log.w(ImApp.LOG_TAG, "error parsing QR invite link", e);
                    }
                }

                if (resultScans.size() > 0)
                {
                    showMainScreen ();
                }
            }

        }
    }

}
