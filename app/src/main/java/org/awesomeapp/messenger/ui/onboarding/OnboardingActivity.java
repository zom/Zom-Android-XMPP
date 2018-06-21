package org.awesomeapp.messenger.ui.onboarding;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.ListPopupWindow;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.theartofdev.edmodo.cropper.CropImageView;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.MainActivity;
import org.awesomeapp.messenger.crypto.otr.OtrAndroidKeyManagerImpl;
import org.awesomeapp.messenger.model.Server;
import org.awesomeapp.messenger.plugin.xmpp.XmppAddress;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.tasks.AddContactAsyncTask;
import org.awesomeapp.messenger.ui.BaseActivity;
import org.awesomeapp.messenger.ui.legacy.DatabaseUtils;
import org.awesomeapp.messenger.ui.legacy.SignInHelper;
import org.awesomeapp.messenger.ui.legacy.SimpleAlertHandler;
import org.awesomeapp.messenger.ui.widgets.RoundedAvatarDrawable;
import org.awesomeapp.messenger.util.SecureMediaStore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import im.zom.messenger.BuildConfig;
import im.zom.messenger.R;
import org.awesomeapp.messenger.util.Languages;

public class OnboardingActivity extends BaseActivity {

    private ViewFlipper mViewFlipper;
    private EditText mEditUsername;
    private View mSetupProgress;
    //private View mSetupButton;
    private ImageView mImageAvatar;

    private MenuItem mItemSkip = null;

    private EditText mSpinnerDomains;

    private String mNickname;
    private String mUsername;
    private String mFingerprint;
    private OnboardingAccount mNewAccount;

    private SimpleAlertHandler mHandler;

    private static final String USERNAME_ONLY_ALPHANUM = "[^A-Za-z0-9]";

    private boolean mShowSplash = true;
    private ListPopupWindow mDomainList;

    private FindServerTask mCurrentFindServerTask;
    private ExistingAccountTask mExistingAccountTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mShowSplash = getIntent().getBooleanExtra("showSplash",true);

        setContentView(R.layout.awesome_onboarding);

        if (mShowSplash) {
            getSupportActionBar().hide();

        }
        else
        {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        getSupportActionBar().setTitle("");

        mHandler = new SimpleAlertHandler(this);

        View viewSplash = findViewById(R.id.flipViewMain);
        View viewRegister =  findViewById(R.id.flipViewRegister);
        View viewCreate = findViewById(R.id.flipViewCreateNew);
        View viewLogin = findViewById(R.id.flipViewLogin);
        View viewInvite = findViewById(R.id.flipViewInviteFriends);
        View viewAdvanced  = findViewById(R.id.flipViewAdvanced);


        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        int themeColorHeader = settings.getInt("themeColor",-1);

        if (themeColorHeader != -1)
            viewInvite.setBackgroundColor(themeColorHeader);

        mViewFlipper = (ViewFlipper) findViewById(R.id.viewFlipper1);

        mEditUsername = (EditText)viewCreate.findViewById(R.id.edtNewName);
        mSpinnerDomains = (EditText)viewAdvanced.findViewById(R.id.spinnerDomains);
     //   ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
       //         android.R.layout.simple_dropdown_item_1line, OnboardingManager.getServers(this));
        // mSpinnerDomains.setAdapter(adapter);

        mDomainList = new ListPopupWindow(this);
        mDomainList.setAdapter(new ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line, Server.getServersText(this)));
        mDomainList.setAnchorView(mSpinnerDomains);
        mDomainList.setWidth(600);
        mDomainList.setHeight(400);

        mDomainList.setModal(false);
        mDomainList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mSpinnerDomains.setText(Server.getServersText(OnboardingActivity.this)[position]);
                mDomainList.dismiss();
            }
        });

        mSpinnerDomains.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mDomainList.show();
            }
        });
        mSpinnerDomains.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    mDomainList.show();
            }
        });

        mImageAvatar = (ImageView) viewCreate.findViewById(R.id.imageAvatar);
        mImageAvatar.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {


               startAvatarTaker();

            }
        });

        setAnimLeft();

        ImageView imageLogo = (ImageView)viewSplash.findViewById(R.id.imageLogo);
        imageLogo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setAnimLeft();
                showOnboarding();
            }
        });

        View btnStartOnboardingNext = viewSplash.findViewById(R.id.nextButton);
        btnStartOnboardingNext.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setAnimLeft();
                showOnboarding();
            }
        });



        View btnShowCreate = viewRegister.findViewById(R.id.btnShowRegister);
        btnShowCreate.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                setAnimLeft();
                showSetupScreen();
            }

        });

        View btnShowLogin = viewRegister.findViewById(R.id.btnShowLogin);
        btnShowLogin.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                setAnimLeft();
                showLoginScreen();
            }

        });

        View btnShowAdvanced = viewCreate.findViewById(R.id.btnAdvanced);
        btnShowAdvanced.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                setAnimLeft();
                showAdvancedScreen();
            }

        });

        // set up language chooser button
        View languageButton = viewSplash.findViewById(R.id.languageButton);
        languageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final Activity activity = OnboardingActivity.this;
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
                        ImApp.resetLanguage(activity, languageCodes[position]);
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

                            mNickname = mEditUsername.getText().toString();

                            if (mNickname.length() > 0) {
                                startAccountSetup();
                            }


                        }
                    });
                    return true;
                }

                return false;
            }
        });

        View btnCreateAdvanced = viewAdvanced.findViewById(R.id.btnNewRegister);
        btnCreateAdvanced.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                View viewEdit = findViewById(R.id.edtNameAdvanced);
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(viewEdit.getWindowToken(), 0);
                startAdvancedSetup();
            }
        });

        View btnInviteSms = viewInvite.findViewById(R.id.btnInviteSMS);
        btnInviteSms.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                doInviteSMS();

            }

        });

        View btnInviteShare = viewInvite.findViewById(R.id.btnInviteShare);
        btnInviteShare.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v) {
               
                doInviteShare();
                
            }
            
        });

        View btnInviteQR = viewInvite.findViewById(R.id.btnInviteScan);
        btnInviteQR.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v) {
               
                doInviteScan();
                
            }
            
        });


        View btnSignIn = viewLogin.findViewById(R.id.btnSignIn);
        btnSignIn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

               doExistingAccountRegister();

            }

        });

        if (!mShowSplash)
        {
            setAnimLeft();
            showOnboarding();
        }
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
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_onboarding, menu);

        mItemSkip = menu.findItem(R.id.menu_skip);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
        case android.R.id.home:
                        
            showPrevious();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {

        if (mDomainList != null && mDomainList.isShowing())
            mDomainList.dismiss();
        else
            showPrevious();
    }

    // Back button should bring us to the previous screen, unless we're on the first screen
    private void showPrevious()
    {
        setAnimRight();
        getSupportActionBar().setTitle("");

        if (mCurrentFindServerTask != null)
            mCurrentFindServerTask.cancel(true);

        if (mViewFlipper.getCurrentView().getId()==R.id.flipViewMain)
        {
            finish();
        }
        else if (mViewFlipper.getCurrentView().getId()==R.id.flipViewRegister)
        {
            if (mShowSplash)
                showSplashScreen();
            else
                finish();
        }
        else if (mViewFlipper.getCurrentView().getId()==R.id.flipViewCreateNew)
        {
            showOnboarding();
        }
        else if (mViewFlipper.getCurrentView().getId()==R.id.flipViewLogin)
        {
            showOnboarding();
        }
        else if (mViewFlipper.getCurrentView().getId()==R.id.flipViewAdvanced)
        {

            showOnboarding();

        }
    }

    private void showSplashScreen ()
    {
        setAnimRight();
        getSupportActionBar().hide();
        getSupportActionBar().setTitle("");
        mViewFlipper.setDisplayedChild(0);
    }

    private void showOnboarding ()
    {

        mViewFlipper.setDisplayedChild(1);

    }


    private void showSetupScreen ()
    {

        mViewFlipper.setDisplayedChild(2);
        getSupportActionBar().show();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    private void showLoginScreen ()
    {

        mViewFlipper.setDisplayedChild(3);
        findViewById(R.id.progressExistingUser).setVisibility(View.GONE);
        findViewById(R.id.progressExistingImage).setVisibility(View.GONE);

        getSupportActionBar().show();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    private void showAdvancedScreen ()
    {
        mViewFlipper.setDisplayedChild(4);

        getSupportActionBar().show();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    private void startAdvancedSetup ()
    {
        mSetupProgress = findViewById(R.id.progressNewUser);
        mSetupProgress.setVisibility(View.VISIBLE);

        mNickname = ((EditText)findViewById(R.id.edtNameAdvanced)).getText().toString();
        String username = mNickname.replaceAll(USERNAME_ONLY_ALPHANUM, "").toLowerCase();

        if (TextUtils.isEmpty(username))
            username = "zomuser"; //if there are no alphanum then just use "zomuser"

      //  mEditUsername.setText(username);

        String domain = ((EditText)findViewById(R.id.spinnerDomains)).getText().toString();

        String password = ((EditText)findViewById(R.id.edtNewPass)).getText().toString();

        mViewFlipper.setDisplayedChild(2);

        showSetupProgress();

        if (mCurrentFindServerTask != null)
            mCurrentFindServerTask.cancel(true);

        mCurrentFindServerTask = new FindServerTask ();
        mCurrentFindServerTask.execute(mNickname, username, domain, password);

    }
    
    private void startAccountSetup()
    {
        setAnimLeft();

        showSetupProgress ();

        String username = mNickname.replaceAll(USERNAME_ONLY_ALPHANUM, "").toLowerCase();

        if (TextUtils.isEmpty(username))
            username = "zomuser"; //if there are no alphanum then just use "zomuser"

        if (mCurrentFindServerTask != null)
            mCurrentFindServerTask.cancel(true);

        mCurrentFindServerTask = new FindServerTask ();
        mCurrentFindServerTask.execute(mNickname,username);
    }

    private void showSetupForm ()
    {
        View viewCreate = findViewById(R.id.flipViewCreateNew);
        viewCreate.findViewById(R.id.viewProgress).setVisibility(View.GONE);
        viewCreate.findViewById(R.id.viewCreate).setVisibility(View.VISIBLE);
        viewCreate.findViewById(R.id.btnAdvanced).setVisibility(View.VISIBLE);

    }

    private void showSetupProgress ()
    {
        View viewCreate = findViewById(R.id.flipViewCreateNew);
        viewCreate.findViewById(R.id.viewProgress).setVisibility(View.VISIBLE);
        viewCreate.findViewById(R.id.viewCreate).setVisibility(View.GONE);
        viewCreate.findViewById(R.id.btnAdvanced).setVisibility(View.GONE);

    }

    private class FindServerTask extends AsyncTask<String, Void, OnboardingAccount> {
        @Override
        protected OnboardingAccount doInBackground(String... setupValues) {
            try {

                Server[] servers = Server.getServers(OnboardingActivity.this);

                Server myServer = new Server();
                String password = null;

                if (setupValues.length > 2)
                    myServer.domain = setupValues[2]; //user can specify the domain they want to be on for a new account

                if (setupValues.length > 3)
                    password = setupValues[3];

                if (setupValues.length > 4)
                    myServer.server = setupValues[4];

                if (myServer.domain == null)
                {
                    myServer = servers[0];
                }

                OtrAndroidKeyManagerImpl keyMan = OtrAndroidKeyManagerImpl.getInstance(OnboardingActivity.this);
                KeyPair keyPair = keyMan.generateLocalKeyPair();
                mFingerprint = keyMan.getFingerprint(keyPair.getPublic());

                String nickname = setupValues[0];
                String username = setupValues[1];

                OnboardingAccount result = null;

                while (result == null) {

                    result = OnboardingManager.registerAccount(OnboardingActivity.this, nickname, username, password, myServer.domain, myServer.domain, myServer.port);

                    if (result == null)
                    {
                        //wait one second
                        try
                        {Thread.sleep(1000);}
                        catch (Exception e){}

                        //append key to username
                        username = setupValues[1] + '.' + mFingerprint.substring(mFingerprint.length()-8,mFingerprint.length()).toLowerCase();
                    }
                }

                String jid = result.username + '@' + result.domain;
                keyMan.storeKeyPair(jid,keyPair);

                return result;
            }
            catch (Exception e)
            {
                Log.e(ImApp.LOG_TAG, "auto onboarding fail", e);
                return null;
            }
        }

        @Override
        protected void onCancelled(OnboardingAccount onboardingAccount) {
            super.onCancelled(onboardingAccount);

            showSetupForm ();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();

            showSetupForm ();
        }

        @Override
        protected void onPostExecute(OnboardingAccount account) {

            View viewCreate = findViewById(R.id.flipViewCreateNew);
            viewCreate.findViewById(R.id.progressImage).setVisibility(View.GONE);

            if (account != null) {
                mUsername = account.username + '@' + account.domain;
                mNewAccount = account;


                viewCreate.findViewById(R.id.viewProgress).setVisibility(View.GONE);
                viewCreate.findViewById(R.id.viewSuccess).setVisibility(View.VISIBLE);

                ImApp mApp = (ImApp)getApplication();
                mApp.setDefaultAccount(account.providerId,account.accountId);

                SignInHelper signInHelper = new SignInHelper(OnboardingActivity.this, mHandler);
                signInHelper.activateAccount(account.providerId, account.accountId);
                signInHelper.signIn(account.password, account.providerId, account.accountId, true);

                mItemSkip.setVisible(true);
                mItemSkip.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {

                        showInviteScreen();
                        return false;
                    }
                });
            }
            else
            {
                viewCreate.findViewById(R.id.viewProgress).setVisibility(View.GONE);
                viewCreate.findViewById(R.id.viewCreate).setVisibility(View.VISIBLE);
                viewCreate.findViewById(R.id.btnAdvanced).setVisibility(View.VISIBLE);

                StringBuffer sb = new StringBuffer();
                sb.append(getString(R.string.account_setup_error_server));
                TextView status = (TextView)viewCreate.findViewById(R.id.statusError);
                status.setText(sb.toString());


                //need to try again somehow
            }
        }
      }

    private void showInviteScreen ()
    {
        mViewFlipper.setDisplayedChild(5);

        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(false);
        TextView tv = (TextView)findViewById(R.id.statusInviteFriends);
        tv.setText(R.string.invite_friends);

        mItemSkip.setVisible(true);
        mItemSkip.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                showMainScreen();
                mItemSkip.setVisible(false);
                return false;
            }
        });
    }

    private void doInviteSMS()
    {
        String inviteString = OnboardingManager.generateInviteMessage(this, mNickname,mUsername, mFingerprint);
        OnboardingManager.inviteSMSContact(this, null, inviteString);
    }

    private void doInviteShare()
    {

        String inviteString = OnboardingManager.generateInviteMessage(this, mNickname,mUsername, mFingerprint);
        OnboardingManager.inviteShare(this, inviteString);
    }
 
    private void doInviteScan ()
    {
        String inviteString;
        try {
            inviteString = OnboardingManager.generateInviteLink(this, mUsername, mFingerprint, mNickname);
            OnboardingManager.inviteScan(this, inviteString);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }

    private void showMainScreen ()
    {
        finish();

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);

    }

    private synchronized boolean doExistingAccountRegister ()
    {
        String username = ((TextView)findViewById(R.id.edtName)).getText().toString();
        String password = ((TextView)findViewById(R.id.edtPass)).getText().toString();

        if (verifyJabberID(username)) {

            if (mExistingAccountTask == null) {
                findViewById(R.id.progressExistingUser).setVisibility(View.VISIBLE);
                findViewById(R.id.progressExistingImage).setVisibility(View.VISIBLE);

                mExistingAccountTask = new ExistingAccountTask();
                mExistingAccountTask.execute(username, password);

                return true;
            }
        }
        else
        {
            Toast.makeText(this,getString(R.string.account_setup_example_email_address),Toast.LENGTH_SHORT).show();
        }

        return false;
    }

    //make sure it is valid, and that people aren't including unsupported domains
    public static boolean verifyJabberID(String jid) {
        if (jid != null) {
            Pattern p = Pattern
                    .compile("(?i)[a-z0-9\\-_\\.]++@[a-z0-9\\-_]++(\\.[a-z0-9\\-_]++)++");
            Matcher m = p.matcher(jid);

            if (!m.matches()) {
                return false;
            }
            else if (jid.contains("gmail.com")||jid.contains("google.com"))
            {
                return false;
            }
            else if (jid.contains("facebook.com"))
            {
                return false;
            }
        } else {
            return false;
        }

        return true;
    }


    private class ExistingAccountTask extends AsyncTask<String, Void, OnboardingAccount> {
        @Override
        protected OnboardingAccount doInBackground(String... account) {
            try {

                OtrAndroidKeyManagerImpl keyMan = OtrAndroidKeyManagerImpl.getInstance(OnboardingActivity.this);
                KeyPair keyPair = keyMan.generateLocalKeyPair();
                mFingerprint = keyMan.getFingerprint(keyPair.getPublic());

                String nickname = new XmppAddress(account[0]).getUser();
                OnboardingAccount result = OnboardingManager.addExistingAccount(OnboardingActivity.this, mHandler, nickname, account[0], account[1]);

                if (result != null && result.accountId != -1 && (!TextUtils.isEmpty(result.username))) {
                    String jabberId = result.username + '@' + result.domain;
                    keyMan.storeKeyPair(jabberId,keyPair);
                }

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

            mUsername = account.username + '@' + account.domain;

            ImApp mApp = (ImApp)getApplication();
            mApp.setDefaultAccount(account.providerId,account.accountId);

            SignInHelper signInHelper = new SignInHelper(OnboardingActivity.this, mHandler);
            signInHelper.activateAccount(account.providerId,account.accountId);
            signInHelper.signIn(account.password, account.providerId, account.accountId, true);

            showInviteScreen();

            mExistingAccountTask = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        ImApp mApp = (ImApp)getApplication();
        mApp.initAccountInfo();

        if (resultCode == RESULT_OK) {
            if (requestCode == OnboardingManager.REQUEST_SCAN) {

                showInviteScreen();

                ArrayList<String> resultScans = data.getStringArrayListExtra("result");
                for (String resultScan : resultScans)
                {

                    try {
                        //parse each string and if they are for a new user then add the user
                        OnboardingManager.DecodedInviteLink diLink = OnboardingManager.decodeInviteLink(resultScan);

                        new AddContactAsyncTask(mNewAccount.providerId, mNewAccount.accountId, mApp).execute(diLink.username, diLink.fingerprint, diLink.nickname);

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
            else if (requestCode == OnboardingManager.REQUEST_CHOOSE_AVATAR)
            {
                Uri imageUri = getPickImageResultUri(data);

                if (imageUri == null)
                    return;

                mCropImageView = new CropImageView(OnboardingActivity.this);// (CropImageView)view.findViewById(R.id.CropImageView);
                mCropImageView.setAspectRatio(1, 1);
                mCropImageView.setFixedAspectRatio(true);
                mCropImageView.setCropShape(CropImageView.CropShape.OVAL);
              //  mCropImageView.setGuidelines(1);

                try {
                    Bitmap bmpThumbnail = SecureMediaStore.getThumbnailFile(OnboardingActivity.this, imageUri, 512);
                    mCropImageView.setImageBitmap(bmpThumbnail);

                    // Use the Builder class for convenient dialog construction
                    android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(OnboardingActivity.this);
                    builder.setView(mCropImageView)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    setAvatar(mCropImageView.getCroppedImage(), mNewAccount);
                                    showInviteScreen();

                                    delete(mOutputFileUri);
                                }
                            })
                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // User cancelled the dialog

                                    delete(mOutputFileUri);

                                }
                            });
                    // Create the AlertDialog object and return it
                    android.support.v7.app.AlertDialog dialog = builder.create();
                    dialog.show();


                    ;
                } catch (IOException ioe) {
                    Log.e(ImApp.LOG_TAG, "couldn't load avatar", ioe);
                }
            }

        }
    }


    private void setAvatar(Bitmap bmp, OnboardingAccount account) {

        RoundedAvatarDrawable avatar = new RoundedAvatarDrawable(bmp);
        mImageAvatar.setImageDrawable(avatar);

        final ImApp app = ((ImApp)getApplication());

        try {

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, stream);

            byte[] avatarBytesCompressed = stream.toByteArray();
            String avatarHash = "nohash";

            DatabaseUtils.insertAvatarBlob(getContentResolver(), Imps.Avatars.CONTENT_URI, account.providerId, account.accountId, avatarBytesCompressed, avatarHash, account.username + '@' + account.domain);
        } catch (Exception e) {
            Log.w(ImApp.LOG_TAG, "error loading image bytes", e);
        }
    }

    public byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    CropImageView mCropImageView;

    /**
     * Create a chooser intent to select the source to get image from.<br/>
     * The source can be camera's (ACTION_IMAGE_CAPTURE) or gallery's (ACTION_GET_CONTENT).<br/>
     * All possible sources are added to the intent chooser.
     */
    public Intent getPickImageChooserIntent() {

        // Determine Uri of camera image to save.
        Uri outputFileUri = getCaptureImageOutputUri();

        List<Intent> allIntents = new ArrayList<>();
        PackageManager packageManager = getPackageManager();

        // collect all camera intents
        Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for (ResolveInfo res : listCam) {
            Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            if (outputFileUri != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            }
            allIntents.add(intent);
        }

        // collect all gallery intents
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        List<ResolveInfo> listGallery = packageManager.queryIntentActivities(galleryIntent, 0);
        for (ResolveInfo res : listGallery) {
            Intent intent = new Intent(galleryIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            allIntents.add(intent);
        }

        // the main intent is the last in the list (fucking android) so pickup the useless one
        Intent mainIntent = allIntents.get(allIntents.size() - 1);
        for (Intent intent : allIntents) {
            if (intent.getComponent().getClassName().equals("com.android.documentsui.DocumentsActivity")) {
                mainIntent = intent;
                break;
            }
        }
        allIntents.remove(mainIntent);

        // Create a chooser from the main intent
        Intent chooserIntent = Intent.createChooser(mainIntent, getString(R.string.choose_photos));

        // Add all other intents
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, allIntents.toArray(new Parcelable[allIntents.size()]));

        return chooserIntent;
    }

    Uri mOutputFileUri = null;

    /**
     * Get URI to image received from capture by camera.
     */
    private synchronized Uri getCaptureImageOutputUri() {

        if (mOutputFileUri == null) {
            File photo = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "zomavatar.jpg");
            mOutputFileUri = FileProvider.getUriForFile(this,
                    BuildConfig.APPLICATION_ID + ".provider",
                    photo);

        }

        return mOutputFileUri;
    }


    private boolean delete(Uri uri) {
        if (uri.getScheme().equals("content")) {
            int deleted = getContentResolver().delete(uri,null,null);
            return deleted == 1;
        }
        if (uri.getScheme().equals("file")) {
            java.io.File file = new java.io.File(uri.toString().substring(5));

            if (file.exists())
                return file.delete();
        }
        return false;
    }


    /**
     * Get the URI of the selected image from {@link #getPickImageChooserIntent()}.<br/>
     * Will return the correct URI for camera and gallery image.
     *
     * @param data the returned data of the activity result
     */
    public Uri getPickImageResultUri(Intent data) {
        boolean isCamera = true;
        if (data != null) {

            if (data.getData() == null)
                return getCaptureImageOutputUri();
            else {
                String action = data.getAction();
                isCamera = action != null && action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
                return isCamera ? getCaptureImageOutputUri() : data.getData();
            }

        }
        else
            return getCaptureImageOutputUri();
    }

    private final static int MY_PERMISSIONS_REQUEST_CAMERA = 1;

    void startAvatarTaker() {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA);

        if (permissionCheck ==PackageManager.PERMISSION_DENIED)
        {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Snackbar.make(mViewFlipper, R.string.grant_perms, Snackbar.LENGTH_LONG).show();
            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        MY_PERMISSIONS_REQUEST_CAMERA);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
        else {
            startActivityForResult(getPickImageChooserIntent(), OnboardingManager.REQUEST_CHOOSE_AVATAR);
        }
    }






}
