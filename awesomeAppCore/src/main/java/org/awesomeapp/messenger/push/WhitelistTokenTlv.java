package org.awesomeapp.messenger.push;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.java.otr4j.session.TLV;

import java.io.UnsupportedEncodingException;

/**
 * A class representing the JSON payload of the ChatSecure-Push Whitelist Token Exchange OTR TLV
 * scheme. This scheme facilitates the exchange of push Whitelist tokens over OTR sessions.
 * Created by dbro on 9/28/15.
 */
public class WhitelistTokenTlv {

    /**
     * ChatSecure-Push TLV type
     * This corresponds to the ChatSecure-Push Whitelist Token Exchange date format.
     * See <a href="https://github.com/ChatSecure/ChatSecure-Push-Server/wiki/Chat-Client-Implementation-Notes#json-whitelist-token-exchange">JSON Whitelist Token Exchange</a>
     */
    public static final int TLV_WHITELIST_TOKEN = 0x01A4;

    /**
     * @return a {@link Gson} instance configured to de/serialize {@link WhitelistTokenTlv}
     * objects received or to-be-sent over the ChatSecure-Push Whitelist Token Exchange TLV scheme.
     */
    public static Gson createGson() {
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    }

    public static WhitelistTokenTlv parseTlv(@NonNull TLV tlv) throws UnsupportedEncodingException {
        return createGson().fromJson(new String(tlv.getValue(), "UTF-8"), WhitelistTokenTlv.class);
    }

    public final String endpoint;
    public final String[] tokens;
    public final String extraData;

    public WhitelistTokenTlv(@NonNull String endpoint,
                             @NonNull String[] tokens,
                             @Nullable String extraData) {
        this.endpoint = endpoint;
        this.tokens = tokens;
        this.extraData = extraData;
    }


    @Override
    public String toString() {
        return createGson().toJson(this);
    }
}
