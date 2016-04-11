package org.awesomeapp.messenger.ui;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.encode.Contents;
import com.google.zxing.encode.QRCodeEncoder;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.MainActivity;
import org.awesomeapp.messenger.model.ImConnection;
import org.awesomeapp.messenger.plugin.xmpp.XmppAddress;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.service.IImConnection;
import org.awesomeapp.messenger.ui.legacy.DatabaseUtils;
import org.awesomeapp.messenger.ui.legacy.SignInHelper;
import org.awesomeapp.messenger.ui.onboarding.OnboardingManager;
import org.awesomeapp.messenger.ui.qr.QrGenAsyncTask;
import org.awesomeapp.messenger.ui.qr.QrShareAsyncTask;
import org.awesomeapp.messenger.ui.widgets.ImageViewActivity;
import org.awesomeapp.messenger.ui.widgets.RoundedAvatarDrawable;
import org.awesomeapp.messenger.util.SecureMediaStore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import im.zom.messenger.R;


public class AccountFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    ImageView mIvAvatar;
    CropImageView mCropImageView;
    TextView mTvPassword;
    ImApp mApp;
    Handler mHandler = new Handler();
    ImageView ivScan;
    View mView;

    long mProviderId;
    long mAccountId;
    String mUserAddress;
    String mUserKey;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment GalleryFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AccountFragment newInstance(String param1, String param2) {
        AccountFragment fragment = new AccountFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public AccountFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        mApp = ((ImApp) getActivity().getApplication());
        mProviderId = mApp.getDefaultProviderId();
        mAccountId = mApp.getDefaultAccountId();
        mUserAddress = mApp.getDefaultUsername();
        mUserKey = mApp.getDefaultOtrKey();

       mView = inflater.inflate(R.layout.awesome_fragment_account, container, false);

        if (!TextUtils.isEmpty(mUserAddress)) {
            XmppAddress xAddress = new XmppAddress(mUserAddress);

            TextView tvNickname = (TextView) mView.findViewById(R.id.tvNickname);

            TextView tvUsername = (TextView) mView.findViewById(R.id.edtName);
            mTvPassword = (TextView) mView.findViewById(R.id.edtPass);
            View btnShowPassword = mView.findViewById(R.id.btnShowPass);
            btnShowPassword.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    mTvPassword.setText(getAccountPassword(mProviderId));
                }
            });

            TextView tvFingerprint = (TextView) mView.findViewById(R.id.tvFingerprint);

            ivScan = (ImageView) mView.findViewById(R.id.qrcode);

            ivScan.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                    String inviteString;
                    try {
                        inviteString = OnboardingManager.generateInviteLink(getActivity(), mUserAddress, mUserKey);
                        OnboardingManager.inviteScan(getActivity(), inviteString);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }

            });

            mIvAvatar = (ImageView) mView.findViewById(R.id.imageAvatar);
            mIvAvatar.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {

                    startAvatarTaker();

                }
            });

            ImageView btnQrShare = (ImageView) mView.findViewById(R.id.qrshare);
            btnQrShare.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    try {
                        String inviteLink = OnboardingManager.generateInviteLink(getActivity(), mUserAddress, mUserKey);
                        new QrShareAsyncTask(getActivity()).execute(inviteLink);
                    } catch (IOException ioe) {
                        Log.e(ImApp.LOG_TAG, "couldn't generate QR code", ioe);
                    }
                }
            });


            Switch switchOnline = (Switch) mView.findViewById(R.id.switchOnline);
            switchOnline.setChecked(checkConnection());

            switchOnline.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        signIn();
                    } else {
                        // The toggle is disabled
                        signOut();
                    }
                }
            });

            try {

                Drawable avatar = DatabaseUtils.getAvatarFromAddress(mApp.getContentResolver(), mUserAddress, ImApp.DEFAULT_AVATAR_WIDTH, ImApp.DEFAULT_AVATAR_HEIGHT, false);

                if (avatar != null)
                    mIvAvatar.setImageDrawable(avatar);
            } catch (Exception e) {
                Log.w(ImApp.LOG_TAG, "error getting avatar", e);
            }

            tvUsername.setText(mUserAddress);
            tvNickname.setText(xAddress.getUser());

            if (mUserKey != null) {
                tvFingerprint.setText(prettyPrintFingerprint(mUserKey));

                /**
                 try {
                 String inviteLink = OnboardingManager.generateInviteLink(getActivity(), fullUserName, mApp.getDefaultOtrKey());
                 new QrGenAsyncTask(getActivity(), ivScan,ImApp.DEFAULT_AVATAR_WIDTH).execute(inviteLink);
                 } catch (IOException ioe) {
                 Log.e(ImApp.LOG_TAG, "couldn't generate QR code", ioe);
                 }*/
            }

            /**
            Button btnLock = (Button) mView.findViewById(R.id.btnLock);
            btnLock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((MainActivity) getActivity()).handleLock();
                    }
            });*/
        }


        return mView;
    }

    private boolean checkConnection() {
        try {
            IImConnection conn = mApp.getConnection(mProviderId, mAccountId);

            if (conn.getState() == ImConnection.DISCONNECTED)
                return false;

            return true;
        } catch (Exception e) {
            return false;
        }

    }

    private String getAccountPassword(long providerId) {

        String result = "";

        Cursor c = getActivity().getContentResolver().query(Imps.Provider.CONTENT_URI_WITH_ACCOUNT,
                new String[]{Imps.Provider.ACTIVE_ACCOUNT_PW}, Imps.Provider.CATEGORY + "=? AND providers." + Imps.Provider._ID + "=?" /* selection */,
                new String[]{ImApp.IMPS_CATEGORY, providerId + ""} /* selection args */,
                Imps.Provider.DEFAULT_SORT_ORDER);

        if (c != null) {
            c.moveToFirst();
            result = c.getString(0);
            c.close();
        }

        return result;

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == 200) {

            Uri imageUri = getPickImageResultUri(data);

            if (imageUri == null)
                return;

            mCropImageView = new CropImageView(getActivity());// (CropImageView)view.findViewById(R.id.CropImageView);
            mCropImageView.setAspectRatio(1, 1);
            mCropImageView.setFixedAspectRatio(true);
            mCropImageView.setGuidelines(1);

            try {
                Bitmap bmpThumbnail = SecureMediaStore.getThumbnailFile(getActivity(), imageUri, 512);
                mCropImageView.setImageBitmap(bmpThumbnail);

                // Use the Builder class for convenient dialog construction
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setView(mCropImageView)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                setAvatar(mCropImageView.getCroppedImage());
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                            }
                        });
                // Create the AlertDialog object and return it
                AlertDialog dialog = builder.create();
                dialog.show();
                ;
            } catch (IOException ioe) {
                Log.e(ImApp.LOG_TAG, "couldn't load avatar", ioe);
            }

        }


    }

    private void setAvatar(Bitmap bmp) {

        BitmapDrawable avatar = new BitmapDrawable(bmp);
        mIvAvatar.setImageDrawable(avatar);

        final ImApp app = ((ImApp) getActivity().getApplication());

        try {

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, stream);

            byte[] avatarBytesCompressed = stream.toByteArray();
            String avatarHash = "nohash";
            DatabaseUtils.insertAvatarBlob(getActivity().getContentResolver(), Imps.Avatars.CONTENT_URI, mProviderId, mAccountId, avatarBytesCompressed, avatarHash, mUserAddress);
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


    /**
     * Create a chooser intent to select the source to get image from.<br/>
     * The source can be camera's (ACTION_IMAGE_CAPTURE) or gallery's (ACTION_GET_CONTENT).<br/>
     * All possible sources are added to the intent chooser.
     */
    public Intent getPickImageChooserIntent() {

        // Determine Uri of camera image to save.
        Uri outputFileUri = getCaptureImageOutputUri();

        List<Intent> allIntents = new ArrayList<>();
        PackageManager packageManager = getActivity().getPackageManager();

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
        File getImage = getActivity().getExternalCacheDir();
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


    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private String prettyPrintFingerprint(String fingerprint) {
        StringBuffer spacedFingerprint = new StringBuffer();

        for (int i = 0; i + 8 <= fingerprint.length(); i += 8) {
            spacedFingerprint.append(fingerprint.subSequence(i, i + 8));
            spacedFingerprint.append(' ');
        }

        return spacedFingerprint.toString();
    }

    void signIn () {
        // The toggle is enabled
        SignInHelper helper = new SignInHelper(getActivity(), mHandler, new SignInHelper.SignInListener() {
            @Override
            public void connectedToService() {

            }

            @Override
            public void stateChanged(int state, long accountId) {

            }
        });

        helper.signIn(getAccountPassword(mProviderId), mProviderId, mAccountId,true);

    }

    void signOut() {
        //if you are signing out, then we will deactive "auto" sign in
        ContentValues values = new ContentValues();
        values.put(Imps.AccountColumns.KEEP_SIGNED_IN, 0);
        getActivity().getContentResolver().update(ContentUris.withAppendedId(Imps.Account.CONTENT_URI, mAccountId), values, null, null);
        signOut(mProviderId, mAccountId);
        ;
    }

    void signOut(long providerId, long accountId) {

        try {

            IImConnection conn = mApp.getConnection(mProviderId, mAccountId);
            if (conn != null) {
                conn.logout();
            } else {
                // Normally, we can always get the connection when user chose to
                // sign out. However, if the application crash unexpectedly, the
                // status will never be updated. Clear the status in this case
                // to make it recoverable from the crash.
                ContentValues values = new ContentValues(2);
                values.put(Imps.AccountStatusColumns.PRESENCE_STATUS, Imps.CommonPresenceColumns.OFFLINE);
                values.put(Imps.AccountStatusColumns.CONNECTION_STATUS, Imps.ConnectionStatus.OFFLINE);
                String where = Imps.AccountStatusColumns.ACCOUNT + "=?";
                getActivity().getContentResolver().update(Imps.AccountStatus.CONTENT_URI, values, where,
                        new String[]{Long.toString(accountId)});
            }
        } catch (RemoteException ex) {
            Log.e(ImApp.LOG_TAG, "signout: caught ", ex);
        } finally {

        }

    }

    private final static int MY_PERMISSIONS_REQUEST_CAMERA = 1;

    void startAvatarTaker() {
        int permissionCheck = ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.CAMERA);

        if (permissionCheck ==PackageManager.PERMISSION_DENIED)
        {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    Manifest.permission.CAMERA)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Snackbar.make(mView, R.string.grant_perms, Snackbar.LENGTH_LONG).show();
            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.CAMERA},
                        MY_PERMISSIONS_REQUEST_CAMERA);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
        else {

            startActivityForResult(getPickImageChooserIntent(), 200);
        }
    }


}
