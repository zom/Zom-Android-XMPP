package org.awesomeapp.messenger.plugin.xmpp;

import java.util.Locale;

import org.lantern.mobilesdk.Lantern;
import org.lantern.mobilesdk.StartResult;
import org.lantern.mobilesdk.LanternNotRunningException;

import android.Session;

public class LanternSession implements Session {
    private StartResult startResult;

    public void setStartResult(StartResult result) {
        this.startResult = result;
    }

    public boolean lanternDidStart() {
        if (startResult == null) {
            return false;
        }
        return true;
    }

    public String getHTTPAddr() {
        if (startResult == null) {
            return "";
        }
        return startResult.getHTTPAddr();
    }

    public String getSOCKS5Addr() {
        if (startResult == null) {
            return "";
        }
        return startResult.getSOCKS5Addr();
    }

    private void newUser() {
    }

    public boolean usePaymentWall() {
        return false;
    }

    public boolean isChineseUser() {
        final Locale locale = Locale.getDefault();

        return locale.equals(new Locale("zh", "CN")) ||
            locale.equals(new Locale("zh", "TW"));
    }

    private boolean isDeviceLinked() {
        return false;
    }

    public boolean isReferralApplied() {
        return false;
    }

    public int getNumFreeMonths() {
        return 0;
    }

    public boolean isProUser() {
        return false;
    }

    public void setError(String command, String errorMsg) {
    }

    public void setErrorId(String command, String errorId) {
    }

    public String getLastError(String command) {
        return "";
    }

    public String getLastErrorId(String command) {
        return "";
    }

    public String currency() {
        return "";
    }

    public void addDevice(String id, String name) {
    }

    public void removeDevice(String id) {
    }

    public void setStripePubKey(String key) {
    }

    public String stripeApiKey() {
        return "";
    }

    public void addPlan(String id, String description, String currency,
            boolean bestValue, long numYears, long price) {
    }

    public boolean deviceLinked() {
        return false;
    }

    public void setVerifyCode(String code) {
    }

    public String verifyCode() {
        return "";
    }

    public boolean proxyAll() {
        return true;
    }

    public void setProxyAll(boolean proxyAll) {
    }

    public void setDeviceCode(String code, long expiration) {
    }

    public String deviceCode() {
        return "";
    }

    public String getCountry() {
      return "";
    }

    public Long getDeviceExp() {
        return 0l;
    }

    public void userData(boolean active, long expiration,
            String subscription, String email) {
    }

    private void setExpiration(long expiration) {
    }

    public String getExpiration() {
        return "";
    }

    public void proUserStatus(String status) {
    }

    public void setProPlan(String plan) {
    }

    public void setProUser(String email, String token) {
    }

    public void setIsProUser(boolean isProUser) {
    }

    public void setStripeToken(String token) {
    }

    public void setEmail(String email) {
    }

    public void setResellerCode(String code) {
    }

    public void setProvider(String provider) {
    }

    public void setAccountId(String accountId) {
    }

    public String accountId() {
      return "";
    }

    public void setCode(String referral) {
    }

    public void setToken(String token) {
    }

    public String stripeToken() {
      return "";
    }

    public String email() {
        return "";
    }

    public String resellerCode() {
        return "";
    }

    public String provider() {
        return "";
    }

    public void setUserId(long userId) {
    }

    private void setDeviceId(String deviceId) {
    }

    public String deviceId() {
        return "12345678";
    }

    public String deviceName() {
        return android.os.Build.MODEL;
    }

    public String code() {
        return "";
    }

    public long getUserID() {
        return 0;
    }

    public String getToken() {
        return "";
    }

    public String getPlan() {
        return "";
    }

    public void setReferral(String referralCode) {
    }

    public String referral() {
        return "";
    }

    public String plan() {
        return "";
    }

    public String locale() {
        return Locale.getDefault().toString();
    }

    public void setReferralApplied() {
    }

    public void bandwidthUpdate(long quota, long remaining) {
    }

    public void showSurvey(String url) {
    }

    @Override
    public void updateStats(String s, String s1, String s2, long l, long l1) {
    }

    public void setSurveyTaken(String url) {
    }

    public void setStaging(boolean staging) {
    }

    public boolean useStaging() {
        return false;
    }

    public void setCountry(String country) {
    }

    public void afterStart() {
    }
  }
