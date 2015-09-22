package org.awesomeapp.messenger.ui.onboarding;

import org.awesomeapp.messenger.plugin.xmpp.XmppAddress;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.ui.qr.QrScanActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.ui.legacy.ImPluginHelper;
import org.awesomeapp.messenger.plugin.xmpp.XmppConnection;
import org.awesomeapp.messenger.util.LogCleaner;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Telephony;
import android.util.Base64;
import android.util.Log;

public class OnboardingManager {

    public final static int REQUEST_SCAN = 1111;
    public final static String BASE_INVITE_URL = "https://zom.im/i/#";

    public static void inviteSMSContact (Activity context, String phoneNumber, String message)
    {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) // At least KitKat
        {
            String defaultSmsPackageName = Telephony.Sms.getDefaultSmsPackage(context); // Need to change the build to API 19

            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("text/plain");
            sendIntent.putExtra(Intent.EXTRA_TEXT, message);

            if (defaultSmsPackageName != null)// Can be null in case that there is no default, then the user would be able to choose
            // any app that support this intent.
            {
                sendIntent.setPackage(defaultSmsPackageName);
            }
            context.startActivity(sendIntent);

        }
        else // For early versions, do what worked for you before.
        {
            Intent smsIntent = new Intent(android.content.Intent.ACTION_VIEW);
            smsIntent.setType("vnd.android-dir/mms-sms");

            if (phoneNumber != null)
            smsIntent.putExtra("address",phoneNumber);
            smsIntent.putExtra("sms_body",message);

            context.startActivity(smsIntent);
        }
    }
    
    public static void inviteShare (Activity context, String message)
    {
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);        
        intent.putExtra(Intent.EXTRA_TEXT, message);
        intent.setType("text/plain");
        context.startActivity(intent);

    }
    
    public static void inviteScan (Activity context, String message)
    {
        Intent intent = new Intent(context, QrScanActivity.class);
        intent.putExtra(Intent.EXTRA_TEXT,message);
        intent.setType("text/plain");
        context.startActivityForResult(intent, REQUEST_SCAN);

    }
    
    public static String generateInviteMessage (Context context, String nickname, String username, String fingerprint)
    {
        try
        {
            StringBuffer resp = new StringBuffer();

            resp.append(nickname).append(" is inviting you to Zom: ");
            
            resp.append(generateInviteLink(context,username,fingerprint));
            
            return resp.toString();
        } catch (Exception e)
        { 
            Log.d(ImApp.LOG_TAG,"error with link",e);
            return null;
        }
    }

    public static String[] decodeInviteLink (String link)
    {
        Uri inviteLink = Uri.parse(link);
        String[] code = inviteLink.toString().split("#");

        if (code.length > 1) {

            try {
                String out = new String(Base64.decode(code[1], Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING));

                String[] parts = out.split("\\?otr=");

                return parts;
            }
            catch (IllegalArgumentException iae)
            {
             Log.e(ImApp.LOG_TAG,"bad link decode",iae);
            }
        }

        return null;

    }

    public static String generateInviteLink (Context context, String username, String fingerprint) throws IOException
    {
        StringBuffer inviteUrl = new StringBuffer();
        inviteUrl.append(BASE_INVITE_URL);
        
        StringBuffer code = new StringBuffer();        
        code.append(username);
        code.append("?otr=").append(fingerprint);
        
        inviteUrl.append(Base64.encodeToString(code.toString().getBytes(), Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING));
        return inviteUrl.toString();
    }

    private final static String PASSWORD_LETTERS = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789+@!#";
    private final static int PASSWORD_LENGTH = 10;

    public static String generatePassword()
    {
        // Pick from some letters that won't be easily mistaken for each
        // other. So, for example, omit o O and 0, 1 l and L.
        SecureRandom random = new SecureRandom();

        StringBuffer pw = new StringBuffer();
        for (int i=0; i<PASSWORD_LENGTH; i++)
        {
            int index = (int)(random.nextDouble()*PASSWORD_LETTERS.length());
            pw.append(PASSWORD_LETTERS.substring(index, index+1));
        }
        return pw.toString();
    }

    public static OnboardingAccount registerAccount (Activity context, Handler handler, String username, String domain, int port) throws JSONException {
        String password = generatePassword();

        ContentResolver cr = context.getContentResolver();
        ImPluginHelper helper = ImPluginHelper.getInstance(context);
        long providerId = helper.createAdditionalProvider(helper.getProviderNames().get(0)); //xmpp FIXME

        long accountId = ImApp.insertOrUpdateAccount(cr, providerId, -1, username, password);

        Uri accountUri = ContentUris.withAppendedId(Imps.Account.CONTENT_URI, accountId);

        Cursor pCursor = cr.query(Imps.ProviderSettings.CONTENT_URI, new String[]{Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE}, Imps.ProviderSettings.PROVIDER + "=?", new String[]{Long.toString(providerId)}, null);

        Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
                pCursor, cr, providerId, false /* don't keep updated */, null /* no handler */);

        //should check to see if Orbot is installed and running
        boolean useTor = false;
        boolean doDnsSrvLookup = true;

        JSONObject obj = new JSONObject(loadServersJSON(context));
        JSONArray servers = obj.getJSONArray("servers");


        settings.setUseTor(useTor);
        settings.setRequireTls(true);
        settings.setTlsCertVerify(true);
        settings.setAllowPlainAuth(false);

        settings.setDoDnsSrv(doDnsSrvLookup);

        int maxAttempts = 5;

        if (domain == null) {
            int nameIdx = 0;

            for (int i = 0; i < servers.length(); i++) {

                JSONObject server = servers.getJSONObject(i);

                for (int n = 0; n < maxAttempts; n++) {

                    try {

                        domain = server.getString("domain");
                        String host = server.getString("server");

                        if (host != null) {
                            settings.setServer(host); //set the google connect server
                            settings.setDoDnsSrv(false);
                        }
                        else
                        {
                            settings.setServer(null);
                            settings.setDoDnsSrv(true);
                        }

                        settings.setDomain(domain);
                        settings.setPort(server.getInt("port"));
                        settings.requery();

                        HashMap<String, String> aParams = new HashMap<String, String>();

                        XmppConnection xmppConn = new XmppConnection(context);
                        xmppConn.initUser(providerId, accountId);

                        boolean success = xmppConn.registerAccount(settings, username, password, aParams);

                        if (success) {
                            OnboardingAccount result = null;

                            result = new OnboardingAccount();
                            result.username = username;
                            result.domain = domain;
                            result.password = password;
                            result.providerId = providerId;
                            result.accountId = accountId;

                            //now keep this account signed-in
                            ContentValues values = new ContentValues();
                            values.put(Imps.AccountColumns.KEEP_SIGNED_IN, 1);
                            cr.update(accountUri, values, null, null);
                            settings.close();
                            return result;
                        }


                    } catch (Exception e) {
                        LogCleaner.error(ImApp.LOG_TAG, "error registering new account", e);


                    }

                    username = username + (i+1); //add a number to the end of the username
                    ImApp.insertOrUpdateAccount(cr, providerId, accountId, username, password);
                    settings.requery();

                }


            }


        }
        else
        {
            try
            {
                settings.setDomain(domain);
                settings.setPort(port);
                settings.requery();

                HashMap<String, String> aParams = new HashMap<String, String>();

                XmppConnection xmppConn = new XmppConnection(context);
                xmppConn.initUser(providerId, accountId);

                boolean success = xmppConn.registerAccount(settings, username, password, aParams);

                if (success) {
                    OnboardingAccount result = null;

                    result = new OnboardingAccount();
                    result.username = username;
                    result.domain = domain;
                    result.password = password;
                    result.providerId = providerId;
                    result.accountId = accountId;

                    //now keep this account signed-in
                    ContentValues values = new ContentValues();
                    values.put(Imps.AccountColumns.KEEP_SIGNED_IN, 1);
                    cr.update(accountUri, values, null, null);

                    settings.close();

                    return result;
                }
            } catch (Exception e) {
                LogCleaner.error(ImApp.LOG_TAG, "error registering new account", e);


            }
        }

        settings.close();
        return null;

    }

    public static OnboardingAccount addExistingAccount (Activity context, Handler handler, String jabberId, String password) {

        OnboardingAccount result = null;

        String[] jabberParts = jabberId.split("@");
        String username = jabberParts[0];
        String domain = jabberParts[1];
        int port = 5222;

        ContentResolver cr = context.getContentResolver();
        ImPluginHelper helper = ImPluginHelper.getInstance(context);
        long providerId = helper.createAdditionalProvider(helper.getProviderNames().get(0)); //xmpp FIXME

        long accountId = ImApp.insertOrUpdateAccount(cr, providerId, -1, username, password);

        Uri accountUri = ContentUris.withAppendedId(Imps.Account.CONTENT_URI, accountId);

        Cursor pCursor = cr.query(Imps.ProviderSettings.CONTENT_URI, new String[]{Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE}, Imps.ProviderSettings.PROVIDER + "=?", new String[]{Long.toString(providerId)}, null);

        Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
                pCursor, cr, providerId, false /* don't keep updated */, null /* no handler */);

        //should check to see if Orbot is installed and running
        boolean useTor = false;
        boolean doDnsSrvLookup = true;

        settings.setUseTor(useTor);
        settings.setRequireTls(true);
        settings.setTlsCertVerify(true);
        settings.setAllowPlainAuth(false);

        settings.setDoDnsSrv(doDnsSrvLookup);

        try {

            settings.setDomain(domain);
            settings.setPort(port);
            settings.requery();

            result = new OnboardingAccount();
            result.username = username;
            result.domain = domain;
            result.password = password;
            result.providerId = providerId;
            result.accountId = accountId;

            //now keep this account signed-in
            ContentValues values = new ContentValues();
            values.put(Imps.AccountColumns.KEEP_SIGNED_IN, 1);
            cr.update(accountUri, values, null, null);

            // settings closed in registerAccount
        } catch (Exception e) {
            LogCleaner.error(ImApp.LOG_TAG, "error registering new account", e);


        }


        settings.close();
        return result;

    }


    public static String loadServersJSON(Context context) {
        String json = null;
        try {

            InputStream is = context.getAssets().open("servers.json");

            int size = is.available();

            byte[] buffer = new byte[size];

            is.read(buffer);

            is.close();

            json = new String(buffer, "UTF-8");


        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;

    }


}
