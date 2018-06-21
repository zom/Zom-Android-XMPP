package org.awesomeapp.messenger.model;

import android.content.Context;
import android.util.Pair;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

public class Server {

    private static Server[] servers;

    private static Server parseJSONObject (JSONObject jsonObject) throws JSONException {
    //    Server server = new Server();
     //   server.domain = jsonObject.getString("domain");
        Server server = new Gson().fromJson(jsonObject.toString(), Server.class);
        return server;
    }

    public static void reload (Context context)
    {
        servers = getServers(context);
    }

    public static String[] getServersText (Context context)
    {
        if (servers == null)
            reload(context);

        String[] serverDomains = new String[servers.length];
        int i = 0;

        for (Server server : servers)
            serverDomains[i++] = server.domain;

        return serverDomains;
    }

    public static Server getServer (Context context, String hostname)
    {
        if (servers == null)
            reload(context);

        for (Server server : servers)
        {
            if (server.domain.equals(hostname))
                return server;
            else if (server.server.equals(hostname))
                return server;
        }

        return null;
    }

    public static Server[] getServers (Context context)
    {
        if (servers == null)
        {
            try {
                JSONObject obj = new JSONObject(loadServersJSON(context));
                JSONArray jsonServers = obj.getJSONArray("servers");
                servers = new Server[jsonServers.length()];

                for (int i = 0; i < jsonServers.length(); i++) {

                    JSONObject server = jsonServers.getJSONObject(i);
                    servers[i] = parseJSONObject(server);

                }

            } catch (Exception e) {
                return null;
            }
        }

        return servers;
    }

    private static String loadServersJSON(Context context) {
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

    public String name;
    public String description;
    public String website;
    public String twitter;
    public String privacy_policy;
    public String logo;
    public String country_code;
    public String domain;
    public String server;
    public String ip;
    public int port = 5222; //default value
    public boolean requires_otr = false;
    public boolean captcha = false;
    public String[] extensions;
    public String certificate;

}

/**
 "name": "Zom",
 "description": "Zom's home servers, setup just right, keeping all your messages safe and sound!",
 "website": "https://zom.im/",
 "twitter": "https://twitter.com/zomapp",
 "privacy_policy": "https://zom.im/privacy/",
 "logo": "images/zom.png",
 "country_code": "US",
 "domain": "home.zom.im",
 "server": "home.zom.im",
 "ip": "37.218.242.128",
 "port": 5222,
 "requires_otr": true,
 "captcha": false,
 "extensions": ["XEP-0357", "XEP-0363"],
 "certificate": "MIIFEjCCA/qgAwIBAgISA8+uQF/fEfKp67HU1UqTt9e0MA0GCSqGSIb3DQEBCwUAMEoxCzAJBgNVBAYTAlVTMRYwFAYDVQQKEw1MZXQncyBFbmNyeXB0MSMwIQYDVQQDExpMZXQncyBFbmNyeXB0IEF1dGhvcml0eSBYMzAeFw0xNzEwMDgyMzMyMzJaFw0xODAxMDYyMzMyMzJaMBwxGjAYBgNVBAMTEWNvbmZlcmVuY2Uuem9tLmltMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvCFdJsySjlM19gUcr/RMC2EBiXBJ/pfasZ5USDXsEc86w6AwmJeqmmH+azFQNiF9n4Cy4JFM4qR2G0h2lrZdhDRdXvjo6PQa1E8Kw0zeFwDd9PGfojmyl3VomyF9yyoAdOwslz/SsNmXRBmVxUEHSeIX6dGP+E6R6RqFxfIrVnxPjMHIOvIvCLSKa2D2KQNUmkk5Oopf82KCrdqj247x0wGAgUGciXBgIcWTZK4PLxzqv0bgUIdI5NkbqkeB3/5u7mIXdzBlAKPW5iwRdDTxXRBONhfMFILadkyt9LJeD1wdKolZjXgO/l8gIeonEvGv3NSqusAo5hvBUsfokpW8bwIDAQABo4ICHjCCAhowDgYDVR0PAQH/BAQDAgWgMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjAMBgNVHRMBAf8EAjAAMB0GA1UdDgQWBBTPVrxzVCWMxZtomDtYsmCDD6C76jAfBgNVHSMEGDAWgBSoSmpjBH3duubRObemRWXv86jsoTBvBggrBgEFBQcBAQRjMGEwLgYIKwYBBQUHMAGGImh0dHA6Ly9vY3NwLmludC14My5sZXRzZW5jcnlwdC5vcmcwLwYIKwYBBQUHMAKGI2h0dHA6Ly9jZXJ0LmludC14My5sZXRzZW5jcnlwdC5vcmcvMCkGA1UdEQQiMCCCEWNvbmZlcmVuY2Uuem9tLmltggtob21lLnpvbS5pbTCB/gYDVR0gBIH2MIHzMAgGBmeBDAECATCB5gYLKwYBBAGC3xMBAQEwgdYwJgYIKwYBBQUHAgEWGmh0dHA6Ly9jcHMubGV0c2VuY3J5cHQub3JnMIGrBggrBgEFBQcCAjCBngyBm1RoaXMgQ2VydGlmaWNhdGUgbWF5IG9ubHkgYmUgcmVsaWVkIHVwb24gYnkgUmVseWluZyBQYXJ0aWVzIGFuZCBvbmx5IGluIGFjY29yZGFuY2Ugd2l0aCB0aGUgQ2VydGlmaWNhdGUgUG9saWN5IGZvdW5kIGF0IGh0dHBzOi8vbGV0c2VuY3J5cHQub3JnL3JlcG9zaXRvcnkvMA0GCSqGSIb3DQEBCwUAA4IBAQCPCrACCwknNIwpeaWvaZ6L18d2nMgIfbFtHD9x4oSlsWV9/Rv9w372LjGnzySvYCTKTwX7xcdgNXpwKXrVErtKeN9yFq+JhmwecxG1xjgfAsWiQ0m8bh+nWmVQvCXrSm/jVBUblq+JRIvdHkWt4Jbud8qJuqTsMkVs1a0cGplcFmwEyYRRmL24iwLoW4YdP9/8gyDxLqK+Lef/kcQzv6y/fLwMLlKrExuisoby3mDwRWMZHuw2icYDS5Mn4Oo3B7VkQ1DSipfHSGJD8rbfnvFNsQw1dMWLHHxuLsRWC7lK+zM2rAjmDk+x2NKTjomF9u/SIRvaYkwoxe9agAlOTaJI"
  **/