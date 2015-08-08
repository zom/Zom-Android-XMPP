package org.awesomeapp.messenger.util;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.widget.Toast;

import org.awesomeapp.messenger.ui.widgets.VisualizerView;

public class AudioPlayer {
    private static final String TAG = "AudioPlayer";

    private Context mContext;
    private String mFileName;
    private String mMimeType;

    private MediaPlayer mediaPlayer;
    private HttpMediaStreamer streamer;

    private Visualizer mVisualizer;
    private VisualizerView mVisualizerView;

    private int mDuration = -1;

    public AudioPlayer(Context context, String fileName, String mimeType, VisualizerView visualizerView) throws Exception {
        mContext = context.getApplicationContext();
        mFileName = fileName;
        mMimeType = mimeType;
        mVisualizerView = visualizerView;

        initPlayer();

    }

    public int getDuration ()
    {
        return mDuration;
    }

    public void play() {
        mediaPlayer.start();
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
        streamer = new HttpMediaStreamer(mFileName, mMimeType);
        Uri uri = streamer.getUri();

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnPreparedListener(new OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer mp) {

                mDuration = mediaPlayer.getDuration();
                setupVisualizerFxAndUI();
                mVisualizer.setEnabled(true);


            }
        });
        mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                //killPlayer();

            }
        });

        mediaPlayer.setDataSource(mContext, uri);
        mediaPlayer.prepareAsync();



    }


    private void setupVisualizerFxAndUI() {

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
}
