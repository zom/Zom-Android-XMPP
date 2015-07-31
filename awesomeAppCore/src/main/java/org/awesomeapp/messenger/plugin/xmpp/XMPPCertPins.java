package org.awesomeapp.messenger.plugin.xmpp;

import java.util.ArrayList;
import java.util.HashSet;


public class XMPPCertPins
{

    // Use the following rules
    // https//wiki.mozilla.org/Security/Server_Side_TLS
    // AEADs over everything else
    // PFS over non-PFS
    // AES-128 over AES-256 ( https//www.schneier.com/blog/archives/2009/07/another_new_aes.html )
    // Avoid SHA-1
    // Remove RC4, MD5, DES
    public final static String[] SSL_IDEAL_CIPHER_SUITES_API_20 = {
     "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
     "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
     "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
     "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
     "TLS_DHE_RSA_WITH_AES128_GCM_SHA256",
     "TLS_DHE_RSA_WITH_AES256_GCM_SHA384",

     "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
     "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
     "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
     "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
     "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
     "TLS_DHE_RSA_WITH_AES_256_CBC_SHA384",

     "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
     "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
     "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
     "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
     "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
     "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
     "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
     "TLS_DHE_DSS_WITH_AES_256_CBC_SHA",

     "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA",
     "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
     "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA",
     "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA",
     "TLS_RSA_WITH_AES_128_CBC_SHA256",
     "TLS_RSA_WITH_AES_256_CBC_SHA256",
     "TLS_RSA_WITH_AES_128_CBC_SHA",
     "TLS_RSA_WITH_AES_256_CBC_SHA"
    };

    // Follow above rules but as closely as possible but if we have to use RC4, use it last
    public final static String[] SSL_IDEAL_CIPHER_SUITES = {
    "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
    "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
    "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
    "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
    "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
    "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
    "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
    "TLS_DHE_DSS_WITH_AES_256_CBC_SHA",

    "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA",
    "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA",
    "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
    "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA",

    "TLS_RSA_WITH_AES_128_CBC_SHA",
    "TLS_RSA_WITH_AES_256_CBC_SHA",

    // UNCOMMENT THIS BLOCK ONLY IF ABSOLUTELY NECESSARY
    /*
    "TLS_ECDHE_RSA_WITH_RC4_128_SHA",
    "TLS_ECDHE_ECDSA_WITH_RC4_128_SHA",
    "TLS_ECDH_RSA_WITH_RC4_128_SHA",
    "TLS_ECDH_ECDSA_WITH_RC4_128_SHA",
    */
    };

    public static ArrayList<String> PINLIST = null;

    /**
     * These are currently all pins of the CA's signing keys for the CAs used by
     * servers that we trust. AndroidPinning always validates using the normal
     * CA method, so there is no use to include cacert.org, similar CAs, or
     * self-signed certificates here. AndroidPinning will fail anyway when it
     * runs its built-in check against the system's trust manager.
     *
     * @return
     */
    public static String[] getPinList() {
        if (PINLIST == null) {
            PINLIST = new ArrayList<String>();
            // generated using http//gitlab.doeg.gy/cpu/jabberpinfetch

            /* guardianproject.info/hyper.to self-signed
            SubjectDN CN=hyper.to, O=Chaos Inc., L=San Francisco, ST=California, C=US
            IssuerDN CN=hyper.to, O=Chaos Inc., L=San Francisco, ST=California, C=US
            Fingerprint 1064712E64D1AE7F4FDC2DEFDE7F19B1CEEB82B8
            SPKI Pin 2B1292D6CD084EC90B5DBD398AEA15B853337971
            */
            PINLIST.add("2B1292D6CD084EC90B5DBD398AEA15B853337971");

            //otr.im
            PINLIST.add("C9DD0915DD25FE69651C2D814746A1999473FA1D31310931FE692C871F94E230");
            PINLIST.add("AC12DAC450327E8F57E21EAA3ABF65C50E08CBED");
            
            // double check there are no duplicates by mistake
            if (PINLIST.size() != new HashSet<String>(PINLIST).size())
                throw new SecurityException("PINLIST has duplicate entries!");
        }

        return PINLIST.toArray(new String[PINLIST.size()]);

    }
}
