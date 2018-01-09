package org.awesomeapp.messenger.service;

import android.content.Context;

import com.jrummyapps.android.shell.CommandResult;
import com.jrummyapps.android.shell.Shell;

import org.awesomeapp.messenger.util.BinaryInstaller;
import org.awesomeapp.messenger.util.LogCleaner;

import java.io.File;

/**
 * Created by n8fr8 on 11/17/17.
 */

public class AdvancedNetworking {

    private File mFileTransport = null;
    private Thread mTransportThread = null;

    public void installTransport (Context context, String assetKey)
    {
        BinaryInstaller bi = new BinaryInstaller(context,context.getFilesDir());
        try {
            mFileTransport = bi.installResource("transports", assetKey, false);
        }
        catch (Exception ioe)
        {
            debug("Couldn't install transport: " + ioe);
        }
    }

    public void startTransport ()
    {
        if (mFileTransport != null) {
            debug("Transport installed:  " + mFileTransport.getAbsolutePath());
            mTransportThread = new Thread () {
                public void run ()
                {
                    startTransportSync();
                }
            };
            mTransportThread.start();
        }
    }

    public void stopTransport ()
    {
        if (mTransportThread != null && mTransportThread.isAlive())
            mTransportThread.interrupt();
    }

    private void startTransportSync ()
    {
        try {
            if (mFileTransport != null)
            {

                String serverAddress = "172.104.48.102";
                String serverPort = "443";
                String serverPassword = "zomzom123";
                String serverCipher = "aes-128-cfb";
                String localAddress = "127.0.0.1";
                String localPort = "31059";

                StringBuffer cmd = new StringBuffer();
                cmd.append(mFileTransport.getCanonicalPath()).append(' ');
                cmd.append("-s ").append(serverAddress).append(' ');
                cmd.append("-p ").append(serverPort).append(' ');
                cmd.append("-k ").append(serverPassword).append(' ');
                cmd.append("-m ").append(serverCipher).append(' ');
                cmd.append("-b ").append(localAddress).append(' ');
                cmd.append("-l ").append(localPort).append(' ');

                exec(cmd.toString(), false);
            }
        }
        catch (Exception ioe)
        {
            debug("Couldn't install transport: " + ioe);
        }
    }


    private int exec (String cmd, boolean wait) throws Exception
    {
        CommandResult shellResult = Shell.run(cmd);
        debug("CMD: " + cmd + "; SUCCESS=" + shellResult.isSuccessful());

        if (!shellResult.isSuccessful()) {
            throw new Exception("Error: " + shellResult.exitCode + " ERR=" + shellResult.getStderr() + " OUT=" + shellResult.getStdout());
        }

        return shellResult.exitCode;
    }

    public void debug(String msg) {
        LogCleaner.debug(getClass().getName(), msg);
        // Log.d(TAG, msg);

    }

}
