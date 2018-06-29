package org.awesomeapp.messenger.ui.widgets;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.ImUrlActivity;
import org.awesomeapp.messenger.nearby.NearbyShareActivity;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.util.SecureMediaStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import im.zom.messenger.R;

public class ImageViewActivity extends AppCompatActivity implements PZSImageView.PSZImageViewImageMatrixListener {

    public static final String URIS = "uris";
    public static final String MIME_TYPES = "mime_types";
    public static final String MESSAGE_IDS = "message_ids";
    public static final String CURRENT_INDEX = "current_index";

    private ConditionallyEnabledViewPager viewPagerPhotos;
    private RectF tempRect = new RectF();

    private ArrayList<Uri> uris;
    private ArrayList<String> mimeTypes;
    private ArrayList<String> messagePacketIds;

    private boolean mShowResend = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        supportRequestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        //getSupportActionBar().setElevation(0);

        mShowResend = getIntent().getBooleanExtra("showResend",false);

        viewPagerPhotos = new ConditionallyEnabledViewPager(this);
        viewPagerPhotos.setBackgroundColor(0x33333333);
        setContentView(viewPagerPhotos);
        //setContentView(R.layout.image_view_activity);
        getSupportActionBar().show();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setTitle("");

        //viewPagerPhotos = (ViewPager) findViewById(R.id.viewPagerPhotos);
        viewPagerPhotos.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                updateTitle();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        viewPagerPhotos.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if ((right - left) > 0 && (bottom - top) > 0 && viewPagerPhotos.getAdapter() == null) {
                    uris = getIntent().getParcelableArrayListExtra(URIS);
                    mimeTypes = getIntent().getStringArrayListExtra(MIME_TYPES);
                    messagePacketIds = getIntent().getStringArrayListExtra(MESSAGE_IDS);

                    if (uris != null && mimeTypes != null && uris.size() > 0 && uris.size() == mimeTypes.size()) {
                        viewPagerPhotos.setAdapter(new PhotosPagerAdapter(ImageViewActivity.this, uris));
                        int currentIndex = getIntent().getIntExtra(CURRENT_INDEX, 0);
                        viewPagerPhotos.setCurrentItem(currentIndex);
                        updateTitle();
                    }
                }
            }
        });

        /*viewPagerPhotos.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!viewPagerPhotosEnabled) {
                    return true;
                }
                return false;
            }
        });*/
       // getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        /**
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);*/
    }

    private void updateTitle() {
        if (viewPagerPhotos.getAdapter() != null && viewPagerPhotos.getAdapter().getCount() > 0) {
            String title = getString(R.string.photo_x_of_y, viewPagerPhotos.getCurrentItem() + 1, viewPagerPhotos.getAdapter().getCount());
            setTitle(title);
        } else {
            setTitle("");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_message_context, menu);

        menu.findItem(R.id.menu_message_copy).setVisible(false);
        menu.findItem(R.id.menu_message_resend).setVisible(mShowResend);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_message_forward:
                forwardMediaFile();
                return true;
            case R.id.menu_message_share:
                exportMediaFile();
                return true;
                /**
            case R.id.menu_message_copy:
                exportMediaFile();
                return true;**/

            case R.id.menu_message_resend:
                resendMediaFile();
                return true;

            case R.id.menu_message_delete:
                deleteMediaFile();
                return true;

            case R.id.menu_message_nearby:
                sendNearby();
                return true;

            default:
        }
        return super.onOptionsItemSelected(item);
    }

    public void sendNearby ()
    {
        int currentItem = viewPagerPhotos.getCurrentItem();
        if (currentItem >= 0 && currentItem < uris.size()) {
            String resharePath = uris.get(currentItem).toString();
            Intent shareIntent = new Intent(this, NearbyShareActivity.class);
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.setDataAndType(Uri.parse(resharePath), mimeTypes.get(currentItem));
            startActivity(shareIntent);
        }

    }


    public void exportMediaFile ()
    {
        int currentItem = viewPagerPhotos.getCurrentItem();
        if (currentItem >= 0 && currentItem < uris.size()) {
            java.io.File exportPath = SecureMediaStore.exportPath(mimeTypes.get(currentItem), uris.get(currentItem));
            exportMediaFile(mimeTypes.get(currentItem), uris.get(currentItem), exportPath);
        }
    };

    private void exportMediaFile (String mimeType, Uri mediaUri, java.io.File exportPath)
    {
        try {

            SecureMediaStore.exportContent(mimeType, mediaUri, exportPath);
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(exportPath));
            shareIntent.setType(mimeType);
            startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.export_media)));
        } catch (IOException e) {
            Toast.makeText(this, "Export Failed " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void forwardMediaFile ()
    {
        int currentItem = viewPagerPhotos.getCurrentItem();
        if (currentItem >= 0 && currentItem < uris.size()) {
            String resharePath = uris.get(currentItem).toString();
            Intent shareIntent = new Intent(this, ImUrlActivity.class);
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.setDataAndType(Uri.parse(resharePath), mimeTypes.get(currentItem));
            startActivity(shareIntent);
        }
    }

    private void resendMediaFile ()
    {
        int currentItem = viewPagerPhotos.getCurrentItem();
        if (currentItem >= 0 && currentItem < uris.size()) {
            String resharePath = uris.get(currentItem).toString();
            String mimeType = mimeTypes.get(currentItem).toString();

            Intent intentResult = new Intent();
            intentResult.putExtra("resendImageUri",resharePath);
            intentResult.putExtra("resendImageMimeType",mimeType);

            setResult(RESULT_OK,intentResult);
            finish();
        }
    }

    private void deleteMediaFile () {
        if (messagePacketIds != null) {
            int currentItem = viewPagerPhotos.getCurrentItem();
            if (currentItem >= 0 && currentItem < uris.size()) {

                Uri deleteUri = uris.get(currentItem);
                if (deleteUri.getScheme() != null && deleteUri.getScheme().equals("vfs"))
                {
                    info.guardianproject.iocipher.File fileMedia = new info.guardianproject.iocipher.File(deleteUri.getPath());
                    fileMedia.delete();
                }

                String messagePacketId = messagePacketIds.get(currentItem);
                Imps.deleteMessageInDb(getContentResolver(), messagePacketId);
                setResult(RESULT_OK);
                finish();
            }
        }
    }

    public class PhotosPagerAdapter extends PagerAdapter {
        private final RequestOptions imageRequestOptions;

        Context context;
        List<Uri> photos;

        public PhotosPagerAdapter(Context context, List<Uri> photos)
        {
            super();
            this.context = context;
            this.photos = photos;
            imageRequestOptions = new RequestOptions().centerInside().diskCacheStrategy(DiskCacheStrategy.NONE).error(R.drawable.broken_image_large);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            PZSImageView imageView = new PZSImageView(context);
            imageView.setBackgroundColor(0xff333333);
            imageView.setId(position);
            container.addView(imageView);

            Uri mediaUri = photos.get(position);

            try {
                imageView.setMatrixListener(ImageViewActivity.this);
                if (SecureMediaStore.isVfsUri(mediaUri)) {

                    info.guardianproject.iocipher.File fileMedia = new info.guardianproject.iocipher.File(mediaUri.getPath());

                    if (fileMedia.exists()) {
                        Glide.with(context)
                                .asBitmap()
                                .apply(imageRequestOptions)
                                .load(new info.guardianproject.iocipher.FileInputStream(fileMedia))
                                .into(imageView);
                    }
                    else
                    {
                        Glide.with(context)
                                .asBitmap()
                                .apply(imageRequestOptions)
                                .load(R.drawable.broken_image_large)
                                .into(imageView);
                    }
                } else {
                    Glide.with(context)
                            .asBitmap()
                            .apply(imageRequestOptions)
                            .load(mediaUri)
                            .into(imageView);
                }
            }
            catch (Throwable t) { // may run Out Of Memory
                Log.w(ImApp.LOG_TAG, "unable to load thumbnail: " + t);
            }
            return imageView;
        }

        @Override
        public int getCount() {
            return photos.size();
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }

        @Override
        public void destroyItem(ViewGroup collection, int position, Object arg2) {
            collection.removeView((View) arg2);
        }
    }

    @Override
    public void onImageMatrixSet(PZSImageView view, int imageWidth, int imageHeight, Matrix imageMatrix) {
        if (view.getId() != viewPagerPhotos.getCurrentItem()) {
            return;
        }
        if (imageMatrix != null) {
            tempRect.set(0, 0, imageWidth, imageHeight);
            imageMatrix.mapRect(tempRect);
            int width = view.getWidth() - view.getPaddingLeft() - view.getPaddingRight();
            int height = view.getHeight() - view.getPaddingTop() - view.getPaddingBottom();
            if (tempRect.width() > width || tempRect.height() > height) {
                viewPagerPhotos.enableSwiping = false;
                return;
            }
        }
        viewPagerPhotos.enableSwiping = true;
    }

    class ConditionallyEnabledViewPager extends ViewPager {
        public boolean enableSwiping = true;
        private final GestureDetector gestureDetector;
        private final SwipeToCloseListener gestureDetectorListener;
        private final VelocityTracker velocityTracker;
        private boolean inSwipeToCloseGesture = false;
        private boolean isClosing = false;
        private float startingY = 0;

        public ConditionallyEnabledViewPager(Context context) {
            super(context);
            gestureDetectorListener = new SwipeToCloseListener(context);
            gestureDetector = new GestureDetector(context, gestureDetectorListener);
            velocityTracker = VelocityTracker.obtain();
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
            if (inSwipeToCloseGesture) {
                if (!isClosing) {
                    velocityTracker.addMovement(ev);
                    if (ev.getAction() == MotionEvent.ACTION_CANCEL || ev.getAction() == MotionEvent.ACTION_UP) {
                        velocityTracker.computeCurrentVelocity(1000); // Pixels per second
                        float velocityY = velocityTracker.getYVelocity();
                        float dy = ev.getY() - startingY;
                        ViewConfiguration vc = ViewConfiguration.get(getContext());
                        if (Math.abs(dy) > vc.getScaledTouchSlop() && Math.abs(velocityY) > vc.getScaledMinimumFlingVelocity()) {
                            closeByFling(dy, Math.abs(viewPagerPhotos.getHeight() / velocityY));
                        } else {
                            // Reset all children. Lazy approach, instead of keeping count of "current photo" which
                            // might have changed during the motion event.
                            for (int i = 0; i < viewPagerPhotos.getChildCount(); i++) {
                                View child = viewPagerPhotos.getChildAt(i);
                                if (child.getTranslationY() != 0) {
                                    child.animate().translationY(0).alpha(1.0f).rotation(0).start();
                                }
                            }
                        }
                        inSwipeToCloseGesture = false;
                    } else {
                        gestureDetector.onTouchEvent(ev);
                    }
                }
                return true;
            } else if (enableSwiping && gestureDetector.onTouchEvent(ev)) {
                inSwipeToCloseGesture = true;
                velocityTracker.clear();
                velocityTracker.addMovement(ev);
                return true;
            }
            if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
                startingY = ev.getY();
                gestureDetectorListener.setDisabled(false);
            } else if (ev.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
                gestureDetectorListener.setDisabled(true); // More than one finger, disable swipe to close
            }
            return super.dispatchTouchEvent(ev);
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            if (!enableSwiping) {
                return false;
            }
            return super.onInterceptTouchEvent(ev);
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            if (!enableSwiping) {
                return true;
            }
            return super.onTouchEvent(ev);
        }

        void closeByFling(float dy, float seconds) {

            seconds = Math.min(seconds, 0.7f); // Upper limit on animation time!

            isClosing = true; // No further touches
            View currentPhoto = viewPagerPhotos.findViewById(viewPagerPhotos.getCurrentItem());
            if (currentPhoto != null) {
                currentPhoto.setPivotX(0.8f * currentPhoto.getWidth());
                currentPhoto.setTranslationY(dy);
                currentPhoto.setAlpha(Math.max(0, 1 - Math.abs(dy) / (viewPagerPhotos.getHeight() / 2)));
                currentPhoto.setRotation(30 * (dy / (viewPagerPhotos.getHeight() / 2)));
                currentPhoto.animate().rotation(Math.signum(dy) * 30).translationY(Math.signum(dy) * currentPhoto.getHeight()).alpha(0).setDuration((long)(1000 * seconds)).setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        finish();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                }).start();
            } else {
                // Hm, no animation, just close
                finish();
            }
        }

        @Override
        public boolean performClick() {
            return !enableSwiping || super.performClick();
        }

        private class SwipeToCloseListener extends GestureDetector.SimpleOnGestureListener {
            private final float minDistance;
            private boolean disabled;
            private boolean inGesture;

            public SwipeToCloseListener(Context context) {
                super();
                minDistance = ViewConfiguration.get(context).getScaledTouchSlop();
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                float dy = e2.getY() - e1.getY();
                float dx = e2.getX() - e1.getX();
                if (Math.abs(dy) > minDistance && !disabled) {
                    View currentPhoto = viewPagerPhotos.findViewById(viewPagerPhotos.getCurrentItem());
                    if (currentPhoto != null) {
                        currentPhoto.setPivotX(0.8f * currentPhoto.getWidth());
                        currentPhoto.setTranslationY(dy);
                        currentPhoto.setAlpha(Math.max(0, 1 - Math.abs(dy) / (viewPagerPhotos.getHeight() / 2)));
                        currentPhoto.setRotation(30 * (dy / (viewPagerPhotos.getHeight() / 2)));
                    }
                    inGesture = true;
                    return true;
                } else if (Math.abs(dx) > minDistance && !inGesture) {
                    disabled = true; // Looks like we have a horizontal movement, disable "swipe-to-close"
                }
                return false;
            }

            public void setDisabled(boolean disabled) {
                this.disabled = disabled;
                inGesture = false;
            }
        }
    };

}
