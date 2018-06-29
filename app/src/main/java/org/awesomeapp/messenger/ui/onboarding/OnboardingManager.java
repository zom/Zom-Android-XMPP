package org.awesomeapp.messenger.ui.onboarding;

import org.awesomeapp.messenger.nearby.NearbyAddContactActivity;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.ui.qr.QrScanActivity;
import org.json.JSONException;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.ui.legacy.ImPluginHelper;
import org.awesomeapp.messenger.plugin.xmpp.XmppConnection;
import org.awesomeapp.messenger.util.LogCleaner;

import java.io.IOException;
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
    public final static int REQUEST_CHOOSE_AVATAR = REQUEST_SCAN+1;

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

    public static void inviteNearby(Activity context, String message)
    {
        Intent intent = new Intent(context, NearbyAddContactActivity.class);
        intent.putExtra(Intent.EXTRA_TEXT, message);
        intent.setType("text/plain");
        context.startActivity(intent);
    }

    public static void inviteShareToPackage (Activity context, String message, String packageName)
    {
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, message);
        intent.setType("text/plain");
        intent.setPackage(packageName);
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
            
            resp.append(generateInviteLink(context,username,fingerprint,nickname));
            
            return resp.toString();
        } catch (Exception e)
        { 
            Log.d(ImApp.LOG_TAG,"error with link",e);
            return null;
        }
    }

    public static DecodedInviteLink decodeInviteLink (String link)
    {
        DecodedInviteLink diLink = null;

        Uri inviteLink = Uri.parse(link);
        String[] code = inviteLink.toString().split("#");

        if (code.length == 1
                && inviteLink.getScheme() != null
                && inviteLink.getScheme().toLowerCase().equals("xmpp")) {

            diLink = new DecodedInviteLink();

            String parseLink = link.substring(5);

            int idx = -1;

            if ((idx = parseLink.indexOf("?"))!=-1)
                parseLink = parseLink.substring(0,idx);

            diLink.username = parseLink;
            diLink.isMigration = false;

            if ((idx = link.indexOf("otr-fingerprint"))!=-1)
                diLink.fingerprint = link.substring(idx+16);

            diLink.nickname = null;

        }
        else if (code[0].contains("/i/")){

            //this is an invite link
            try {
                String out = new String(Base64.decode(code[1], Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING));

//                String[] parts = out.split("\\?otr=");
                String[] partsTemp = out.split("\\?");

                if (partsTemp == null)
                {
                    partsTemp = new String[1];
                    partsTemp[0] = out;
                    diLink = new DecodedInviteLink();
                    diLink.username = out;
                }
                else {

                    diLink = new DecodedInviteLink();
                    diLink.username = partsTemp[0];

                    if (partsTemp.length > 1)
                    {
                        String[] parts = partsTemp[1].split("&");


                        for (String part : parts) {

                            String[] keyValue = part.split("=");

                            if (keyValue[0].equals("otr"))
                                diLink.fingerprint = keyValue[1];
                            else if (keyValue[0].equals("m"))
                                diLink.isMigration = true;
                            else if (keyValue[0].equals("nickname"))
                                diLink.nickname = keyValue[1];

;
                        }



                    }

                }

            }
            catch (IllegalArgumentException iae)
            {
             Log.e(ImApp.LOG_TAG,"bad link decode",iae);
            }
        }

        return diLink;

    }

    public static String generateXmppLink (String username, String fingerprint) throws IOException
    {
        StringBuffer inviteUrl = new StringBuffer();
        inviteUrl.append("xmpp:");
        inviteUrl.append(username);
        inviteUrl.append("?subscribe");
        inviteUrl.append("&otr-fingerprint=").append(fingerprint);

        return inviteUrl.toString();
    }

    public static String generateInviteLink (Context context, String username, String fingerprint, String nickname) throws IOException
    {
        return generateInviteLink(context, username, fingerprint, nickname, false);
    }

    public static String generateInviteLink (Context context, String username, String fingerprint, String nickname, boolean isMigrateLink) throws IOException
    {
        StringBuffer inviteUrl = new StringBuffer();
        inviteUrl.append(BASE_INVITE_URL);
        
        StringBuffer code = new StringBuffer();        
        code.append(username);
        code.append("?otr=").append(fingerprint);

        if (nickname != null)
            code.append("&nickname=").append(nickname);

        if (isMigrateLink)
            code.append("&m=1");

        inviteUrl.append(Base64.encodeToString(code.toString().getBytes(), Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING));
        return inviteUrl.toString();
    }

    private final static String PASSWORD_LETTERS = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789+@!#";
    private final static int PASSWORD_LENGTH = 12;

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



    public static boolean changePassword (Activity context, long providerId, long accountId, String oldPassword, String newPassword)
    {
        try {
            XmppConnection xmppConn = new XmppConnection(context);
            xmppConn.initUser(providerId, accountId);
            boolean success = xmppConn.changeServerPassword(providerId, accountId, oldPassword, newPassword);

            return success;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public static OnboardingAccount registerAccount (Context context, String nickname, String username, String password, String domain, String server, int port) throws JSONException {

        if (password == null)
            password = generatePassword();

        ContentResolver cr = context.getContentResolver();
        ImPluginHelper helper = ImPluginHelper.getInstance(context);
        long providerId = helper.createAdditionalProvider(helper.getProviderNames().get(0)); //xmpp FIXME

        long accountId = ImApp.insertOrUpdateAccount(cr, providerId, -1, nickname, username, password);

        Uri accountUri = ContentUris.withAppendedId(Imps.Account.CONTENT_URI, accountId);

        Cursor pCursor = cr.query(Imps.ProviderSettings.CONTENT_URI, new String[]{Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE}, Imps.ProviderSettings.PROVIDER + "=?", new String[]{Long.toString(providerId)}, null);

        Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
                pCursor, cr, providerId, false /* don't keep updated */, null /* no handler */);

        settings.setRequireTls(true);
        settings.setTlsCertVerify(true);
        settings.setAllowPlainAuth(false);

        try
        {
            settings.setDomain(domain);
            settings.setPort(port);

            if (server != null) {
                settings.setServer(server); //if we have a host, then we should use it
                settings.setDoDnsSrv(false);

            }
            else
            {
                settings.setServer(null);
                settings.setDoDnsSrv(true);

            }

            settings.requery();

            HashMap<String, String> aParams = new HashMap<String, String>();

            XmppConnection xmppConn = new XmppConnection(context);
            xmppConn.initUser(providerId, accountId);

            boolean success = xmppConn.registerAccount(settings, username, password, aParams);

            if (success) {

                OnboardingAccount result = new OnboardingAccount();
                result.username = username;
                result.domain = domain;
                result.password = password;
                result.providerId = providerId;
                result.accountId = accountId;
                result.nickname = nickname;

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

        ImApp.deleteAccount(context.getContentResolver(),accountId, providerId);

        settings.close();
        return null;

    }



    public static OnboardingAccount addExistingAccount (Activity context, Handler handler, String nickname, String jabberId, String password) {

        OnboardingAccount result = null;

        String[] jabberParts = jabberId.split("@");
        String username = jabberParts[0];
        String domain = jabberParts[1];
        int port = 5222;

        ContentResolver cr = context.getContentResolver();
        ImPluginHelper helper = ImPluginHelper.getInstance(context);
        long providerId = helper.createAdditionalProvider(helper.getProviderNames().get(0)); //xmpp FIXME

        long accountId = ImApp.insertOrUpdateAccount(cr, providerId, -1, nickname, username, password);

        if (accountId == -1)
            return null;

        Uri accountUri = ContentUris.withAppendedId(Imps.Account.CONTENT_URI, accountId);

        Cursor pCursor = cr.query(Imps.ProviderSettings.CONTENT_URI, new String[]{Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE}, Imps.ProviderSettings.PROVIDER + "=?", new String[]{Long.toString(providerId)}, null);

        Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
                pCursor, cr, providerId, false /* don't keep updated */, null /* no handler */);

        //should check to see if Orbot is installed and running
        boolean doDnsSrvLookup = true;

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


    public static class DecodedInviteLink {
        public String username;
        public boolean isMigration = false;
        public String fingerprint;
        public String nickname;
    }

}
