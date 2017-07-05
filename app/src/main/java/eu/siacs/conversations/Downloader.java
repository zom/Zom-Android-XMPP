package eu.siacs.conversations;

import android.webkit.URLUtil;

import org.apache.commons.io.FilenameUtils;
import org.awesomeapp.messenger.util.SecureMediaStore;
import org.awesomeapp.messenger.util.SystemServices;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.io.CipherOutputStream;
import org.bouncycastle.crypto.io.CipherInputStream;
import org.bouncycastle.crypto.modes.AEADBlockCipher;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import info.guardianproject.iocipher.File;

public class Downloader {

    static {
        URL.setURLStreamHandlerFactory(new AesGcmURLStreamHandlerFactory());
    }

    private String mMimeType = null;

    public Downloader()
    {}

    public boolean get (String urlString, OutputStream storageStream) throws IOException
    {
        try {
            if (urlString.startsWith("aesgcm"))
                urlString = urlString.replace("aesgcm","https");

            final URL url = new URL(urlString);
            OutputStream os = setupOutputStream(storageStream, url.getRef());
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            InputStream inputStream = connection.getInputStream();
            connection.connect();
            mMimeType = connection.getContentType();

            byte[] buffer = new byte[4096];
            int count;
            while ((count = inputStream.read(buffer)) != -1) {
                os.write(buffer, 0, count);
            }
            os.flush();
            os.close();
            return true;
        } catch (Exception e) {

            return false;
        }
    }

    public String getMimeType ()
    {
        return mMimeType;
    }

    public static InputStream setupInputStream(InputStream is, byte[] keyAndIv) {
        if (keyAndIv != null && keyAndIv.length == 48) {
    //        byte[] keyAndIv = hexToBytes(reference);
            byte[] key = new byte[32];
            byte[] iv = new byte[16];
            System.arraycopy(keyAndIv, 0, iv, 0, 16);
            System.arraycopy(keyAndIv, 16, key, 0, 32);
            AEADBlockCipher cipher = new GCMBlockCipher(new AESEngine());
            cipher.init(false, new AEADParameters(new KeyParameter(key), 128, iv));
            return new CipherInputStream(is, cipher);
        } else {
            return is;
        }
    }
    public static OutputStream setupOutputStream(OutputStream os, String reference) {
        if (reference != null && reference.length() == 96) {
            byte[] keyAndIv = hexToBytes(reference);
            byte[] key = new byte[32];
            byte[] iv = new byte[16];
            System.arraycopy(keyAndIv, 0, iv, 0, 16);
            System.arraycopy(keyAndIv, 16, key, 0, 32);
            AEADBlockCipher cipher = new GCMBlockCipher(new AESEngine());
            cipher.init(false, new AEADParameters(new KeyParameter(key), 128, iv));
            return new CipherOutputStream(os, cipher);
        } else {
            return os;
        }
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] array = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            array[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
        }
        return array;
    }

    public File openSecureStorageFile(String sessionId, String url) throws FileNotFoundException {
//        debug( "openFile: url " + url) ;

        String filename = getFilenameFromUrl(url);
        String localFilename = SecureMediaStore.getDownloadFilename(sessionId, filename);
      //  debug( "openFile: localFilename " + localFilename) ;
        info.guardianproject.iocipher.File fileNew = new info.guardianproject.iocipher.File(localFilename);
        fileNew.getParentFile().mkdirs();

        return fileNew;
    }

    private String getFilenameFromUrl(String urlString) {
        String fileName = URLUtil.guessFileName(urlString, null, null);

        if (fileName.contains("#"))
            return fileName.split("#")[0];
        else if (fileName.contains("?"))
            return fileName.split("\\?")[0];
        else
            return fileName;

    }
}
