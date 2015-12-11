package org.awesomeapp.messenger.ui.widgets;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.awesomeapp.messenger.ImUrlActivity;
import org.awesomeapp.messenger.util.SecureMediaStore;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileInputStream;
import info.guardianproject.otr.app.im.R;

import java.io.IOException;

public class ImageViewActivity extends AppCompatActivity {

    public static final String URI = "uri";
    public static final String MIMETYPE = "mimetype";

    private Uri mediaUri;
    private String mimeType;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_view_activity);
        getSupportActionBar().show();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        ;
        setTitle("");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_message_context, menu);

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
            case R.id.menu_message_delete:
                deleteMediaFile();
                return true;

            default:
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mediaUri = Uri.parse(getIntent().getStringExtra(URI));
        mimeType = getIntent().getStringExtra(MIMETYPE);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                display(mediaUri.getPath());
            }
        });
    }

    private void display( String filename ) {
        try {
            Bitmap bitmap = fitToScreen(filename);
            PZSImageView imageView = (PZSImageView) findViewById(R.id.pzs_image_view);
            imageView.setImageBitmap(bitmap);
        } catch (Throwable t) { // may run Out Of Memory
            findViewById(R.id.pzs_image_view).setVisibility(View.INVISIBLE);
            findViewById(R.id.pzs_broken_image_view).setVisibility(View.VISIBLE);
        }
    }

    private Bitmap fitToScreen( String filename ) throws IOException {
        // read in dimensions only
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inInputShareable = true;
        options.inPurgeable = true;

        FileInputStream fis = new FileInputStream(new File(filename));
        BitmapFactory.decodeStream(fis, null, options);
        fis.close();

        if ((options.outWidth <= 0) || (options.outHeight <= 0))
            throw new IOException( "Image dimensions unknown");

        // calculate down sampling ratio to fit screen
        int imageWidth = options.outWidth;
        int imageHeight = options.outHeight;

        options = new BitmapFactory.Options();
        Point screenDimensions = getScreenDimensions();
        if (imageHeight > imageWidth) {
            options.inSampleSize = imageHeight / screenDimensions.y;
        } else {
            options.inSampleSize = imageWidth / screenDimensions.x;
        }

        // read in downsampled image
        fis = new FileInputStream(new File(filename));
        Bitmap scaledBitmap = BitmapFactory.decodeStream(fis, null, options);
        fis.close();
        return scaledBitmap;
    }

    @SuppressLint("NewApi")
    private Point getScreenDimensions() {
        Display display = getWindowManager().getDefaultDisplay();
        if (Build.VERSION.SDK_INT >= 13) {
            Point size = new Point();
            display.getSize(size);
            return size;
        }
        return new Point( display.getWidth(), display.getHeight());
    }

    public void exportMediaFile ()
    {
        java.io.File exportPath = SecureMediaStore.exportPath(mimeType, mediaUri);
        exportMediaFile(mimeType, mediaUri, exportPath);

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

        String resharePath = mediaUri.toString();
        Intent shareIntent = new Intent(this, ImUrlActivity.class);
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setDataAndType(Uri.parse(resharePath), mimeType);
        startActivity(shareIntent);


    }

    private void deleteMediaFile ()
    {
        Toast.makeText(this,"Feature not quite ready yet!",Toast.LENGTH_SHORT).show();
    }
}
