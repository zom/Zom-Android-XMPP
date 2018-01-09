package org.awesomeapp.messenger.ui;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import org.awesomeapp.messenger.Preferences;
import org.ironrabbit.type.CustomTypefaceManager;

import im.zom.messenger.R;

/**
 * Created by n8fr8 on 5/7/16.
 */
public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        if (Preferences.doBlockScreenshots())
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE);

        //not set color
        int themeColorHeader = settings.getInt("themeColor",-1);
        int themeColorBg = settings.getInt("themeColorBg",-1);

        if (themeColorHeader != -1) {
            if (Build.VERSION.SDK_INT >= 21) {
                getWindow().setNavigationBarColor(themeColorHeader);
                getWindow().setStatusBarColor(themeColorHeader);
            }

            if (getSupportActionBar() != null)
                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(themeColorHeader));
        }


        if (themeColorBg != -1)
        {
            getWindow().getDecorView().setBackgroundColor(themeColorBg);
        }
    }


    public void applyStyleForToolbar() {

        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        int themeColorHeader = settings.getInt("themeColor",-1);
        int themeColorText = settings.getInt("themeColorText",-1);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        //first set font
        Typeface typeface = CustomTypefaceManager.getCurrentTypeface(this);

        if (typeface != null) {
            for (int i = 0; i < toolbar.getChildCount(); i++) {
                View view = toolbar.getChildAt(i);
                if (view instanceof TextView) {
                    TextView tv = (TextView) view;

                    tv.setTypeface(typeface);
                    break;
                }
            }
        }

        if (themeColorHeader != -1) {
            toolbar.setBackgroundColor(themeColorHeader);
            toolbar.setTitleTextColor(themeColorText);
        }

    }



}
