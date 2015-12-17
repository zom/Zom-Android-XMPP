package org.awesomeapp.messenger.ui.onboarding.xmpp;

/**
 * Created by n8fr8 on 5/30/15.
 */
public class Server {

    public String mHostname;
    public int mPort;
    public String mCertFingerprint;
    public String mCountryCode;

    public String getHostname() {
        return mHostname;
    }

    public void setHostname(String mHostname) {
        this.mHostname = mHostname;
    }
}
