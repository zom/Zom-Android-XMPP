package org.awesomeapp.messenger.service;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import static android.net.ConnectivityManager.CONNECTIVITY_ACTION;
import static org.awesomeapp.messenger.service.HeartbeatService.NETWORK_INFO_CONNECTED;
import static org.awesomeapp.messenger.service.HeartbeatService.NETWORK_INFO_EXTRA;
import static org.awesomeapp.messenger.service.HeartbeatService.NETWORK_STATE_ACTION;
import static org.awesomeapp.messenger.service.HeartbeatService.NETWORK_STATE_EXTRA;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class NetworkSchedulerService extends JobService implements
        ConnectivityReceiver.ConnectivityReceiverListener {

    private static final String TAG = NetworkSchedulerService.class.getSimpleName();

    private ConnectivityReceiver mConnectivityReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Service created");
        mConnectivityReceiver = new ConnectivityReceiver(this);
    }



    /**
     * When the app's NetworkConnectionActivity is created, it starts this service. This is so that the
     * activity and this service can communicate back and forth. See "setUiCallback()"
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        return START_NOT_STICKY;
    }


    @Override
    public boolean onStartJob(JobParameters params) {
        Log.i(TAG, "onStartJob" + mConnectivityReceiver);
        registerReceiver(mConnectivityReceiver, new IntentFilter(CONNECTIVITY_ACTION));
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.i(TAG, "onStopJob");
        unregisterReceiver(mConnectivityReceiver);
        return true;
    }

    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        Intent intent = new Intent(NETWORK_STATE_ACTION, null, this, RemoteImService.class);
        intent.putExtra(NETWORK_INFO_CONNECTED, isConnected);

        NetworkConnectivityReceiver.State stateExtra = isConnected ? NetworkConnectivityReceiver.State.CONNECTED : NetworkConnectivityReceiver.State.NOT_CONNECTED;
        intent.putExtra(NETWORK_STATE_EXTRA,stateExtra.ordinal());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        }
        else
        {
            startService(intent);
        }
    }
}