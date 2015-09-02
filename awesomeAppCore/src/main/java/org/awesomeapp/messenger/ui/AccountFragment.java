package org.awesomeapp.messenger.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.theartofdev.edmodo.cropper.CropImageView;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.plugin.xmpp.XmppAddress;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.ui.legacy.DatabaseUtils;
import org.awesomeapp.messenger.ui.onboarding.OnboardingManager;
import org.awesomeapp.messenger.ui.widgets.ImageViewActivity;
import org.awesomeapp.messenger.ui.widgets.RoundedAvatarDrawable;
import org.awesomeapp.messenger.util.SecureMediaStore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import info.guardianproject.otr.app.im.R;

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

        final ImApp app = ((ImApp)getActivity().getApplication());

        View view = inflater.inflate(R.layout.awesome_fragment_account, container, false);

        String fullUserName = app.getDefaultUsername();

        XmppAddress xAddress = new XmppAddress(fullUserName);

        TextView tvNickname = (TextView)view.findViewById(R.id.tvNickname);
        TextView tvUsername = (TextView)view.findViewById(R.id.edtName);
        TextView tvFingerprint = (TextView)view.findViewById(R.id.tvFingerprint);

        ImageView ivScan = (ImageView)view.findViewById(R.id.buttonScan);

        ivScan.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                String inviteString;
                try {
                    inviteString = OnboardingManager.generateInviteLink(getActivity(), app.getDefaultUsername(), app.getDefaultOtrKey());
                    OnboardingManager.inviteScan(getActivity(), inviteString);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }

        });

        mIvAvatar = (ImageView)view.findViewById(R.id.imageAvatar);
        mIvAvatar.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                startActivityForResult(getPickImageChooserIntent(), 200);

            }
        });


        try {

            RoundedAvatarDrawable avatar = DatabaseUtils.getAvatarFromAddress(app.getContentResolver(), fullUserName, 128, 128);

            if (avatar != null)
                mIvAvatar.setImageDrawable(avatar);
        }
        catch (Exception e)
        {
            Log.w(ImApp.LOG_TAG, "error getting avagtar", e);
        }

        tvUsername.setText(fullUserName);
        tvNickname.setText(xAddress.getUser());

        if (app.getDefaultOtrKey() != null)
            tvFingerprint.setText(prettyPrintFingerprint(app.getDefaultOtrKey()));


        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == 200) {

            Uri imageUri = getPickImageResultUri(data);

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
            }
            catch (IOException ioe)
            {
                Log.e(ImApp.LOG_TAG,"couldn't load avatar",ioe);
            }

        }


    }

    private void setAvatar (Bitmap bmp)
    {

         RoundedAvatarDrawable avatar = new RoundedAvatarDrawable(bmp);
         mIvAvatar.setImageDrawable(avatar);

         final ImApp app = ((ImApp)getActivity().getApplication());

         try {

             ByteArrayOutputStream stream = new ByteArrayOutputStream();
             bmp.compress(Bitmap.CompressFormat.JPEG, 90, stream);

             long providerId = app.getDefaultProviderId();
             long accountId = app.getDefaultAccountId();
             byte[] avatarBytesCompressed = stream.toByteArray();
             String avatarHash = "nohash";
             String userAddress = app.getDefaultUsername();

             DatabaseUtils.insertAvatarBlob(getActivity().getContentResolver(), Imps.Avatars.CONTENT_URI, providerId, accountId, avatarBytesCompressed, avatarHash, userAddress);
         }
         catch (Exception e)
         {
         Log.w(ImApp.LOG_TAG,"error loading image bytes",e);
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

    private String prettyPrintFingerprint (String fingerprint)
    {
        StringBuffer spacedFingerprint = new StringBuffer();

        for (int i = 0; i + 8 <= fingerprint.length(); i+=8)
        {
            spacedFingerprint.append(fingerprint.subSequence(i,i+8));
            spacedFingerprint.append(' ');
        }

        return spacedFingerprint.toString();
    }
}
