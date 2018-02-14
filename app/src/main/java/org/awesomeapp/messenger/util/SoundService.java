package org.awesomeapp.messenger.util;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

/**
 * Created by n8fr8 on 2/13/18.
 */

public class SoundService extends Service {

    MediaPlayer mMediaPlayer;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        // check action
        String action = intent.getAction();
        switch (action) {
            case "ACTION_START_PLAYBACK":
                startSound(intent.getStringExtra("SOUND_URI"));
                break;
            case "ACTION_STOP_PLAYBACK":
                stopSound();
                break;
        }

        // service will not be recreated if abnormally terminated
        return START_NOT_STICKY;
    }

    private void startSound(String uriString) {

        // parse sound
        Uri soundUri;
        try {
            soundUri = Uri.parse(uriString);
        } catch (Exception e) {
            cleanup();
            return;
        }

        // play ringer sound
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                }
            });
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    cleanup();
                }
            });
        }
        try {
            mMediaPlayer.setDataSource(this, soundUri);
            mMediaPlayer.prepareAsync();
        } catch (Exception e) {
            cleanup();
        }

    }

    private void stopSound() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        cleanup();
    }

    private void cleanup() {
        stopSelf();
    }

}
