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

    public final static String TRANSPORT_SS2 = "ss2";

    public final static String DEFAULT_PROXY_TYPE = "SOCKS5";
    public final static String DEFAULT_SERVER = "172.104.48.102";
    public final static int DEFAULT_PORT = 80;

    public final static String DEFAULT_HTTP_PROXY_SERVER = "52.68.246.231";
    public final static int DEFAULT_HTTP_PORT = 80;

    public boolean installTransport (Context context, String assetKey)
    {
        BinaryInstaller bi = new BinaryInstaller(context,context.getFilesDir());
        try {
            mFileTransport = bi.installResource("transports", assetKey, true);
            return mFileTransport.exists();
        }
        catch (Exception ioe)
        {
            debug("Couldn't install transport: " + ioe);
        }

        return false;
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
                String serverPort = "80";
                String serverPassword = "zomzom123";
                String serverCipher = "AEAD_CHACHA20_POLY1305";//"aes-128-cfb";
                String localAddress = "127.0.0.1";
                String localPort = "31059";

                StringBuffer cmd = new StringBuffer();
                cmd.append(mFileTransport.getCanonicalPath()).append(' ');

                 /**
                cmd.append("-s ").append(serverAddress).append(' ');
                cmd.append("-p ").append(serverPort).append(' ');
                cmd.append("-k ").append(serverPassword).append(' ');
                cmd.append("-m ").append(serverCipher).append(' ');
                cmd.append("-b ").append(localAddress).append(' ');
                cmd.append("-l ").append(localPort).append(' ');
                 **/

                 cmd.append(" -c ").append("'ss://").append(serverCipher).append(":");
                 cmd.append(serverPassword).append("@");
                 cmd.append(serverAddress).append(":").append(serverPort).append("'");
                 cmd.append(" -socks :").append(localPort);

                 //disable for now
               // exec(cmd.toString(), false);

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
