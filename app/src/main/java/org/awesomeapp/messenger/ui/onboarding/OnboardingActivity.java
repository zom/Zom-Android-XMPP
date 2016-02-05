package org.awesomeapp.messenger.ui.onboarding;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.ViewFlipper;

import com.theartofdev.edmodo.cropper.CropImageView;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.MainActivity;
import org.awesomeapp.messenger.Preferences;
import org.awesomeapp.messenger.crypto.OtrAndroidKeyManagerImpl;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.tasks.AddContactAsyncTask;
import org.awesomeapp.messenger.ui.legacy.DatabaseUtils;
import org.awesomeapp.messenger.ui.legacy.SignInHelper;
import org.awesomeapp.messenger.ui.legacy.SimpleAlertHandler;
import org.awesomeapp.messenger.ui.legacy.ThemeableActivity;
import org.awesomeapp.messenger.ui.widgets.InstantAutoCompleteTextView;
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

import im.zom.messenger.R;
import org.awesomeapp.messenger.util.Languages;
import org.ironrabbit.type.CustomTypefaceManager;

public class OnboardingActivity extends ThemeableActivity {

    private ViewFlipper mViewFlipper;
    private EditText mEditUsername;
    private View mSetupProgress;
    private View mSetupButton;
    private ImageView mImageAvatar;

    private InstantAutoCompleteTextView mSpinnerDomains;

    private String mRequestedUserName;
    private String mFullUserName;
    private String mFingerprint;

    private SimpleAlertHandler mHandler;

    private static final String USERNAME_ONLY_ALPHANUM = "[^A-Za-z0-9]";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkCustomFont();

        setContentView(R.layout.awesome_onboarding);        
        getSupportActionBar().hide();
        getSupportActionBar().setTitle("");

        mHandler = new SimpleAlertHandler(this);

        View viewSplash = findViewById(R.id.flipViewMain);
        View viewRegister =  findViewById(R.id.flipViewRegister);
        View viewCreate = findViewById(R.id.flipViewCreateNew);
        View viewLogin = findViewById(R.id.flipViewLogin);
        View viewInvite = findViewById(R.id.flipViewInviteFriends);
        View viewAdvanced  = findViewById(R.id.flipViewAdvanced);

        mViewFlipper = (ViewFlipper) findViewById(R.id.viewFlipper1);

        mEditUsername = (EditText)viewCreate.findViewById(R.id.edtNewName);
        mSpinnerDomains = (InstantAutoCompleteTextView)viewAdvanced.findViewById(R.id.spinnerDomains);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, OnboardingManager.getServers(this));
        mSpinnerDomains.setAdapter(adapter);

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

        View btnStartOnboarding = viewSplash.findViewById(R.id.buttonStartOnboarding);
        btnStartOnboarding.setOnClickListener(new OnClickListener() {
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
                        checkCustomFont ();
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

        View btnCreateAdvanced = viewAdvanced.findViewById(R.id.btnNewRegister);
        btnCreateAdvanced.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

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

        View btnInviteSkip = viewInvite.findViewById(R.id.btnInviteSkip);
        btnInviteSkip.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                showMainScreen();

            }

        });

        mSetupButton = viewCreate.findViewById(R.id.btnRegister);
        mSetupButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                showInviteScreen();

            }

        });

        View btnSignIn = viewLogin.findViewById(R.id.btnSignIn);
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
                        
            showPrevious();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Back button should bring us to the previous screen, unless we're on the first screen
        if (mViewFlipper.getCurrentView().getId()==R.id.flipViewMain)
        {
            super.onBackPressed();
        } else {
            showPrevious();
        }
    }

    private void showPrevious()
    {
        if (mViewFlipper.getCurrentView().getId()==R.id.flipView2)
        {
            setAnimRight();
            showSplashScreen();
        }
        else if (mViewFlipper.getCurrentView().getId()==R.id.flipViewCreateNew)
        {
            setAnimRight();
            mViewFlipper.showPrevious();
            getSupportActionBar().setTitle("");
        }
        else if (mViewFlipper.getCurrentView().getId()==R.id.flipViewLogin)
        {
            showSplashScreen();
        }
        else if (mViewFlipper.getCurrentView().getId()==R.id.flipViewAdvanced)
        {
            setAnimRight();
            showLoginScreen();
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
        mViewFlipper.showNext();
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

        mRequestedUserName = ((EditText)findViewById(R.id.edtNameAdvanced)).getText().toString();
        String jabberUserId = mRequestedUserName.replaceAll(USERNAME_ONLY_ALPHANUM, "").toLowerCase();

        mEditUsername.setText(jabberUserId);

        String domain = ((InstantAutoCompleteTextView)findViewById(R.id.spinnerDomains)).getText().toString();

        String password = ((EditText)findViewById(R.id.edtNewPass)).getText().toString();

        mViewFlipper.setDisplayedChild(2);

        showSetupProgress();

        new FindServerTask ().execute(mRequestedUserName, jabberUserId, domain, password);

    }
    
    private void startAccountSetup()
    {
        setAnimLeft();

        showSetupProgress ();

        String jabberUserId = mRequestedUserName.replaceAll(USERNAME_ONLY_ALPHANUM, "").toLowerCase();

        new FindServerTask ().execute(mRequestedUserName,jabberUserId);
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

                String domain = null;
                String password = null;

                if (setupValues.length > 2)
                    domain = setupValues[2]; //user can specify the domain they want to be on for a new account

                if (setupValues.length > 3)
                    password = setupValues[3];

                OtrAndroidKeyManagerImpl keyMan = OtrAndroidKeyManagerImpl.getInstance(OnboardingActivity.this);
                KeyPair keyPair = keyMan.generateLocalKeyPair();
                mFingerprint = keyMan.getFingerprint(keyPair.getPublic());

                String nickname = setupValues[0];
                String username = setupValues[1] + '.' + mFingerprint.substring(mFingerprint.length()-8,mFingerprint.length()).toLowerCase();

                OnboardingAccount result = OnboardingManager.registerAccount(OnboardingActivity.this, mHandler, nickname, username, null, domain, 5222);

                if (result != null) {
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

            View viewCreate = findViewById(R.id.flipViewCreateNew);
            viewCreate.findViewById(R.id.progressImage).setVisibility(View.GONE);

            if (account != null) {
                mFullUserName = account.username + '@' + account.domain;

                StringBuffer sb = new StringBuffer();
                sb.append(getString(R.string.account_congrats)).append("\n\n");
                sb.append(getString(R.string.save_account_info));
                TextView statusSuccess = (TextView)viewCreate.findViewById(R.id.statusSuccess);
                statusSuccess.setText(sb.toString());

                viewCreate.findViewById(R.id.viewProgress).setVisibility(View.GONE);

                viewCreate.findViewById(R.id.viewSuccess).setVisibility(View.VISIBLE);

                SignInHelper signInHelper = new SignInHelper(OnboardingActivity.this, mHandler);
                signInHelper.activateAccount(account.providerId, account.accountId);
                signInHelper.signIn(account.password, account.providerId, account.accountId, true);
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
        getSupportActionBar().setTitle(R.string.invite_action);

        TextView tv = (TextView)findViewById(R.id.statusInviteFriends);
        tv.setText(R.string.invite_friends);
    }

    private void doInviteSMS()
    {
        String inviteString = OnboardingManager.generateInviteMessage(this, mRequestedUserName,mFullUserName, mFingerprint);
        OnboardingManager.inviteSMSContact(this, null, inviteString);
    }

    private void doInviteShare()
    {

        String inviteString = OnboardingManager.generateInviteMessage(this, mRequestedUserName,mFullUserName, mFingerprint);
        OnboardingManager.inviteShare(this, inviteString);
    }
 
    private void doInviteScan ()
    {
        String inviteString;
        try {
            inviteString = OnboardingManager.generateInviteLink(this, mFullUserName, mFingerprint);
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

    private void doExistingAccountRegister ()
    {
        String username = ((TextView)findViewById(R.id.edtName)).getText().toString();
        String password = ((TextView)findViewById(R.id.edtPass)).getText().toString();

        findViewById(R.id.progressExistingUser).setVisibility(View.VISIBLE);
        findViewById(R.id.progressExistingImage).setVisibility(View.VISIBLE);

        new ExistingAccountTask().execute(username, password);

    }

    private class ExistingAccountTask extends AsyncTask<String, Void, OnboardingAccount> {
        @Override
        protected OnboardingAccount doInBackground(String... account) {
            try {

                OtrAndroidKeyManagerImpl keyMan = OtrAndroidKeyManagerImpl.getInstance(OnboardingActivity.this);
                KeyPair keyPair = keyMan.generateLocalKeyPair();
                mFingerprint = keyMan.getFingerprint(keyPair.getPublic());

                OnboardingAccount result = OnboardingManager.addExistingAccount(OnboardingActivity.this, mHandler, account[0], account[0], account[1]);

                if (result != null) {
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

            mFullUserName = account.username + '@' + account.domain;

            SignInHelper signInHelper = new SignInHelper(OnboardingActivity.this, mHandler);
            signInHelper.activateAccount(account.providerId,account.accountId);
            signInHelper.signIn(account.password, account.providerId, account.accountId, true);

            showInviteScreen();
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
            else if (requestCode == OnboardingManager.REQUEST_CHOOSE_AVATAR)
            {
                Uri imageUri = getPickImageResultUri(data);

                if (imageUri == null)
                    return;

                mCropImageView = new CropImageView(OnboardingActivity.this);// (CropImageView)view.findViewById(R.id.CropImageView);
                mCropImageView.setAspectRatio(1, 1);
                mCropImageView.setFixedAspectRatio(true);
                mCropImageView.setGuidelines(1);

                try {
                    Bitmap bmpThumbnail = SecureMediaStore.getThumbnailFile(OnboardingActivity.this, imageUri, 512);
                    mCropImageView.setImageBitmap(bmpThumbnail);

                    // Use the Builder class for convenient dialog construction
                    android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(OnboardingActivity.this);
                    builder.setView(mCropImageView)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    setAvatar(mCropImageView.getCroppedImage());
                                    showInviteScreen();
                                }
                            })
                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // User cancelled the dialog
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


    private void setAvatar(Bitmap bmp) {

        RoundedAvatarDrawable avatar = new RoundedAvatarDrawable(bmp);
        mImageAvatar.setImageDrawable(avatar);

        final ImApp app = ((ImApp)getApplication());

        try {

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, stream);

            long providerId = app.getDefaultProviderId();
            long accountId = app.getDefaultAccountId();
            byte[] avatarBytesCompressed = stream.toByteArray();
            String avatarHash = "nohash";
            String userAddress = app.getDefaultUsername();

            DatabaseUtils.insertAvatarBlob(getContentResolver(), Imps.Avatars.CONTENT_URI, providerId, accountId, avatarBytesCompressed, avatarHash, userAddress);
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

    /**
     * Get URI to image received from capture by camera.
     */
    private Uri getCaptureImageOutputUri() {
        Uri outputFileUri = null;
        File getImage = getExternalCacheDir();
        if (getImage != null) {
            outputFileUri = Uri.fromFile(new File(getImage.getPath(), "pickImageResult.jpg"));
        }
        return outputFileUri;
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
            String action = data.getAction();
            isCamera = action != null && action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
        }
        return isCamera ? getCaptureImageOutputUri() : data.getData();
    }

    private final static int MY_PERMISSIONS_REQUEST_CAMERA = 707070;

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

    private void checkCustomFont ()
    {
        if (Preferences.isLanguageTibetan())
        {
            CustomTypefaceManager.loadFromAssets(this);

        }
        else
        {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            List<InputMethodInfo> mInputMethodProperties = imm.getEnabledInputMethodList();

            final int N = mInputMethodProperties.size();

            for (int i = 0; i < N; i++) {

                InputMethodInfo imi = mInputMethodProperties.get(i);

                //imi contains the information about the keyboard you are using
                if (imi.getPackageName().equals("org.ironrabbit.bhoboard")) {
                    //                    CustomTypefaceManager.loadFromKeyboard(this);
                    CustomTypefaceManager.loadFromAssets(this);

                    break;
                }

            }
        }



    }


}
