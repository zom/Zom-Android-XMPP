package org.awesomeapp.messenger.service;

import java.io.File;

import android.content.Context;

public interface ImService {
    public void showToast(CharSequence text, int duration);
    public Context getApplicationContext();
}
