package org.awesomeapp.messenger.ui.camera;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.witness.proofmode.crypto.HashUtils;
import org.witness.proofmode.crypto.PgpUtils;
import org.witness.proofmode.util.DeviceInfo;
import org.witness.proofmode.util.GPSTracker;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.security.MessageDigest;
import java.security.Security;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileInputStream;
import info.guardianproject.iocipher.FileOutputStream;
import timber.log.Timber;

public class ProofMode {

    public final static String PROOF_FILE_TAG = ".proof.csv";
    public final static String OPENPGP_FILE_TAG = ".asc";
    private final static String PROOF_BASE_FOLDER = "/proofmode";
    public final static String PROOF_MIME_TYPE = "text/csv";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }


    public static boolean generateProof (final Context context, Uri uriMedia) throws FileNotFoundException {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String mediaPathTmp = uriMedia.getPath();

        if (!new File(mediaPathTmp).exists())
        {
            //what now?
           return false;
        }

        final String mediaPath = mediaPathTmp;

        final boolean showDeviceIds = prefs.getBoolean("trackDeviceId",true);
        final boolean showLocation = prefs.getBoolean("trackLocation",true);
        final boolean autoNotarize = prefs.getBoolean("autoNotarize", true);
        final boolean showMobileNetwork = prefs.getBoolean("trackMobileNetwork",false);

        final String mediaHash = getSHA256FromFileContent(new FileInputStream(uriMedia.getPath()));

        if (mediaHash != null) {

            Timber.d("Writing proof for hash %s for path %s",mediaHash, mediaPath);

            //write immediate proof, w/o safety check result
            writeProof(context, mediaPath, mediaHash, showDeviceIds, showLocation, showMobileNetwork, null, false, false, -1, null);

            return true;
        }
        else
        {
            Timber.d("Unable to access media files, no proof generated");

        }



        return false;
    }


    private static void writeProof (Context context, String mediaPath, String hash, boolean showDeviceIds, boolean showLocation, boolean showMobileNetwork, String safetyCheckResult, boolean isBasicIntegrity, boolean isCtsMatch, long notarizeTimestamp, String notes)
    {

        File fileMedia = new File(mediaPath);

        File fileMediaSig = new File(mediaPath + OPENPGP_FILE_TAG);
        File fileMediaProof = new File(mediaPath + PROOF_FILE_TAG);
        File fileMediaProofSig = new File(mediaPath + PROOF_FILE_TAG + OPENPGP_FILE_TAG);

        try {

            //sign the media file
            if (!fileMediaSig.exists())
                PgpUtils.getInstance(context).createDetachedSignature(new FileInputStream(fileMedia), new FileOutputStream(fileMediaSig), PgpUtils.DEFAULT_PASSWORD);

            //add data to proof csv and sign again
            boolean writeHeaders = !fileMediaProof.exists();
            writeTextToFile(context, fileMediaProof, buildProof(context, mediaPath, writeHeaders, showDeviceIds, showLocation, showMobileNetwork, safetyCheckResult, isBasicIntegrity, isCtsMatch, notarizeTimestamp, notes));

            if (fileMediaProof.exists()) {
                //sign the proof file again
                PgpUtils.getInstance(context).createDetachedSignature(new FileInputStream(fileMediaProof), new FileOutputStream(fileMediaProofSig), PgpUtils.DEFAULT_PASSWORD);
            }

        } catch (Exception e) {
            Log.e("MediaWatcher", "Error signing media or proof", e);
        }

    }

    private static String buildProof (Context context, String mediaPath, boolean writeHeaders, boolean showDeviceIds, boolean showLocation, boolean showMobileNetwork, String safetyCheckResult, boolean isBasicIntegrity, boolean isCtsMatch, long notarizeTimestamp, String notes)
    {
        File fileMedia = new File (mediaPath);
        String hash = getSHA256FromFileContent(mediaPath);

        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.FULL,DateFormat.FULL);

        HashMap<String, String> hmProof = new HashMap<>();

        hmProof.put("File Path",mediaPath);
        hmProof.put("File Hash SHA256",hash);
        hmProof.put("File Modified",df.format(new Date(fileMedia.lastModified())));
        hmProof.put("Proof Generated",df.format(new Date()));

        if (showDeviceIds) {
            hmProof.put("DeviceID", DeviceInfo.getDeviceId(context));
            hmProof.put("Wifi MAC", DeviceInfo.getWifiMacAddr());
        }

        hmProof.put("IPv4",DeviceInfo.getDeviceInfo(context, DeviceInfo.Device.DEVICE_IP_ADDRESS_IPV4));
        hmProof.put("IPv6",DeviceInfo.getDeviceInfo(context, DeviceInfo.Device.DEVICE_IP_ADDRESS_IPV6));

        hmProof.put("DataType",DeviceInfo.getDataType(context));
        hmProof.put("Network",DeviceInfo.getDeviceInfo(context, DeviceInfo.Device.DEVICE_NETWORK));

        hmProof.put("NetworkType",DeviceInfo.getNetworkType(context));
        hmProof.put("Hardware",DeviceInfo.getDeviceInfo(context, DeviceInfo.Device.DEVICE_HARDWARE_MODEL));
        hmProof.put("Manufacturer",DeviceInfo.getDeviceInfo(context, DeviceInfo.Device.DEVICE_MANUFACTURE));
        hmProof.put("ScreenSize",DeviceInfo.getDeviceInch(context));

        hmProof.put("Language",DeviceInfo.getDeviceInfo(context, DeviceInfo.Device.DEVICE_LANGUAGE));
        hmProof.put("Locale",DeviceInfo.getDeviceInfo(context, DeviceInfo.Device.DEVICE_LOCALE));

        GPSTracker gpsTracker = new GPSTracker(context);

        if (showLocation
                && gpsTracker.canGetLocation())
        {
            Location loc = gpsTracker.getLocation();
            int waitIdx = 0;
            while (loc == null && waitIdx < 3)
            {
                waitIdx++;
                try { Thread.sleep (500); }
                catch (Exception e){}
                loc = gpsTracker.getLocation();
            }

            if (loc != null) {
                hmProof.put("Location.Latitude",loc.getLatitude()+"");
                hmProof.put("Location.Longitude",loc.getLongitude()+"");
                hmProof.put("Location.Provider",loc.getProvider());
                hmProof.put("Location.Accuracy",loc.getAccuracy()+"");
                hmProof.put("Location.Altitude",loc.getAltitude()+"");
                hmProof.put("Location.Bearing",loc.getBearing()+"");
                hmProof.put("Location.Speed",loc.getSpeed()+"");
                hmProof.put("Location.Time",loc.getTime()+"");
            }

        }

        if (!TextUtils.isEmpty(safetyCheckResult)) {
            hmProof.put("SafetyCheck", safetyCheckResult);
            hmProof.put("SafetyCheckBasicIntegrity", isBasicIntegrity+"");
            hmProof.put("SafetyCheckCtsMatch", isCtsMatch+"");
            hmProof.put("SafetyCheckTimestamp", df.format(new Date(notarizeTimestamp)));
        }
        else
        {
            hmProof.put("SafetyCheck", "");
            hmProof.put("SafetyCheckBasicIntegrity", "");
            hmProof.put("SafetyCheckCtsMatch", "");
            hmProof.put("SafetyCheckTimestamp", "");
        }

        if (!TextUtils.isEmpty(notes))
            hmProof.put("Notes",notes);
        else
            hmProof.put("Notes","");

        if (showMobileNetwork)
            hmProof.put("CellInfo",DeviceInfo.getCellInfo(context));

        StringBuffer sb = new StringBuffer();

        if (writeHeaders) {
            for (String key : hmProof.keySet()) {
                sb.append(key).append(",");
            }

            sb.append("\n");
        }

        for (String key : hmProof.keySet())
        {
            String value = hmProof.get(key);
            value = value.replace(',',' '); //remove commas from CSV file
            sb.append(value).append(",");
        }

        sb.append("\n");

        return sb.toString();

    }

    private static void writeTextToFile (Context context, File fileOut, String text)
    {
        try {
            PrintStream ps = new PrintStream(new FileOutputStream(fileOut,true));
            ps.println(text);
            ps.flush();
            ps.close();
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
        }

    }

    private static String getSHA256FromFileContent(String filename)
    {

        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[65536]; //created at start.
            InputStream fis = new FileInputStream(filename);
            int n = 0;
            while (n != -1)
            {
                n = fis.read(buffer);
                if (n > 0)
                {
                    digest.update(buffer, 0, n);
                }
            }
            byte[] digestResult = digest.digest();
            return asHex(digestResult);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private static String getSHA256FromFileContent(InputStream fis)
    {

        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[65536]; //created at start.
            int n = 0;
            while (n != -1)
            {
                n = fis.read(buffer);
                if (n > 0)
                {
                    digest.update(buffer, 0, n);
                }
            }
            byte[] digestResult = digest.digest();
            return asHex(digestResult);
        }
        catch (Exception e)
        {
            return null;
        }
    }


    private static String asHex(byte[] arrayBytes) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < arrayBytes.length; i++) {
            stringBuffer.append(Integer.toString((arrayBytes[i] & 0xff) + 0x100, 16)
                    .substring(1));
        }
        return stringBuffer.toString();
    }
}
