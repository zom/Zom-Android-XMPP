package org.awesomeapp.messenger.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaDataSource;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.ui.widgets.VisualizerView;
import org.awesomeapp.messenger.util.HttpMediaStreamer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.RandomAccessFile;

public class AudioPlayer {
    private static final String TAG = "AudioPlayer";

    private Context mContext;
    private String mFileName;
    private String mMimeType;

    private static MediaPlayer mediaPlayer;
    private HttpMediaStreamer streamer;

    private Visualizer mVisualizer;
    private VisualizerView mVisualizerView;
    private TextView mInfoView;

    private int mDuration = -1;
    private boolean mPrepared = false;
    private boolean mPlayOnPrepare = false;

    public AudioPlayer(Context context, String fileName, String mimeType, VisualizerView visualizerView, TextView infoView) throws Exception {
        mContext = context.getApplicationContext();
        mFileName = fileName;
        mMimeType = mimeType;
        mVisualizerView = visualizerView;
        mInfoView = infoView;

    }

    public int getDuration ()
    {
        return mDuration;
    }

    public void play() {

        if (mPrepared) {

            int permissionCheck = ContextCompat.checkSelfPermission(mContext,
                    Manifest.permission.RECORD_AUDIO);

            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                if (mVisualizer == null)
                    setupVisualizerFxAndUI();

                if (!mVisualizer.getEnabled())
                    mVisualizer.setEnabled(true);
            }

            mediaPlayer.start();
        }
        else
        {
            mPlayOnPrepare = true;
            new AsyncTask<String, Void, Boolean>() {

                @Override
                protected Boolean doInBackground(String... params) {

                    try {
                        initPlayer();
                        return true;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                }

                @Override
                protected void onPostExecute(Boolean playerInit) {


                }

            }.execute();
        }
    }

    public void pause() {
        mediaPlayer.pause();
    }

    public boolean isPlaying () {
        if (mediaPlayer != null)
            return mediaPlayer.isPlaying();
        else
            return false;
    }

    public boolean isPaused () {
        if (mediaPlayer != null)
            return (!mediaPlayer.isPlaying()) && (mediaPlayer.getCurrentPosition() > 0);
        else
            return false;
    }

    public void stop() {
        killPlayer();
    }

    private void killPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if (streamer != null) {
            streamer.destroy();
            streamer = null;
        }
    }

    public void initPlayer() throws Exception {

        final File fileStream = new File(mFileName);

        if (mediaPlayer != null)
        {
            if (mediaPlayer.isPlaying())
                mediaPlayer.stop();

            mediaPlayer.release();
            mediaPlayer = null;
        }

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        if (fileStream.exists()) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mediaPlayer.setDataSource(new MediaDataSource() {

                    info.guardianproject.iocipher.RandomAccessFile fis;

                    @Override
                    public int readAt(long position, byte[] buffer, int offset, int size) throws IOException {
                        if (fis == null)
                            fis = new info.guardianproject.iocipher.RandomAccessFile(fileStream,"r");

                        if (position > getSize())
                            return -1;

                        fis.seek(position);
                        byte[] outBuffer = new byte[size];
                        int readSize = fis.read(outBuffer,0,size);
                        System.arraycopy(outBuffer,0,buffer,offset,size);
                        return readSize;
                    }

                    @Override
                    public long getSize() throws IOException {
                        return fileStream.length();
                    }

                    @Override
                    public void close() throws IOException {
                        if (fis != null)
                            fis.close();
                    }
                });
            }
            else
            {
                streamer = new HttpMediaStreamer(fileStream, mMimeType);
                Uri uri = streamer.getUri();
                mediaPlayer.setDataSource(mContext, uri);
            }
        }
        else
        {
            mediaPlayer.setDataSource(mFileName);
        }

        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {

                Log.d("AudioPlayer", "there was an error loading music: " + i + " " + i1);
                return true;
            }

        });
        mediaPlayer.setOnPreparedListener(new OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer mp) {

                mPrepared = true;
                mDuration = mediaPlayer.getDuration();
                mInfoView.setText((getDuration()/1000) + "secs");

                if (mPlayOnPrepare)
                    play();

            }
        });
        mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                //killPlayer();

                if (mVisualizer != null)
                 mVisualizer.setEnabled(false);



            }
        });


      //  mediaPlayer.prepareAsync();

    }


    private void setupVisualizerFxAndUI() {

        try {

            // Create the Visualizer object and attach it to our media player.
            mVisualizer = new Visualizer(mediaPlayer.getAudioSessionId());
            mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
            mVisualizer.setDataCaptureListener(
                    new Visualizer.OnDataCaptureListener() {
                        public void onWaveFormDataCapture(Visualizer visualizer,
                                                          byte[] bytes, int samplingRate) {
                            mVisualizerView.updateVisualizer(bytes);
                        }

                        public void onFftDataCapture(Visualizer visualizer,
                                                     byte[] bytes, int samplingRate) {
                        }
                    }, Visualizer.getMaxCaptureRate() / 2, true, false);

        }
        catch (RuntimeException re)
        {
            Log.w(ImApp.LOG_TAG, "unable to init audio player visualizaer",re);
        }
    }



}
