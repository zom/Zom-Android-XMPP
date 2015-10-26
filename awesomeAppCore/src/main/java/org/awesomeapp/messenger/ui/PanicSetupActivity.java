package org.awesomeapp.messenger.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import java.util.ArrayList;

import info.guardianproject.otr.app.im.R;
import info.guardianproject.panic.PanicReceiver;

public class PanicSetupActivity extends AppCompatActivity {

    public static final String PREF_KEY_LOCK_APP = "lock_app";
    public static final String PREF_KEY_CLEAR_APP_DATA = "clear_app_data";
    public static final String PREF_KEY_UNINSTALL_APP = "uninstall_app";

    private final static int REQUEST_CHOOSE_CONTACT = 9782;

    private SharedPreferences prefs;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (PanicReceiver.checkForDisconnectIntent(this)) {
            finish();
            return;
        }

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_panic_setup);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setLogo(R.mipmap.ic_launcher);
        toolbar.setTitle(R.string.panic_setup);
        setSupportActionBar(toolbar);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        CheckBox lockApp = (CheckBox) findViewById(R.id.lock_app);
        lockApp.setChecked(prefs.getBoolean(PREF_KEY_LOCK_APP, true));
        lockApp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(PREF_KEY_LOCK_APP, isChecked).apply();
            }
        });

        CheckBox clearAppData = (CheckBox) findViewById(R.id.clear_app_data);
        clearAppData.setChecked(prefs.getBoolean(PREF_KEY_CLEAR_APP_DATA, false));
        clearAppData.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(PREF_KEY_CLEAR_APP_DATA, isChecked).apply();
            }
        });

        CheckBox uninstallApp = (CheckBox) findViewById(R.id.uninstall_app);
        uninstallApp.setChecked(prefs.getBoolean(PREF_KEY_UNINSTALL_APP, false));
        uninstallApp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(PREF_KEY_UNINSTALL_APP, isChecked).apply();
            }
        });

        Button chooseFriend = (Button) findViewById(R.id.choose_friend);
        chooseFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PanicSetupActivity.this, ContactsPickerActivity.class);
                startActivityForResult(intent, REQUEST_CHOOSE_CONTACT);
            }
        });

        intent = getIntent();
        if (intent == null) {
            // started from the Zom GUI, e.g. from a button/menu
        } else {
            // started by an incoming Intent from another app
            String action = intent.getAction();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.panic_setup, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_done:
                PanicReceiver.setTriggerPackageName(this);
                setResult(Activity.RESULT_OK, intent);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHOOSE_CONTACT && resultCode == Activity.RESULT_OK) {
            String username = data.getStringExtra(ContactsPickerActivity.EXTRA_RESULT_USERNAME);

            if (username != null) {
            } else {
                ArrayList<String> users = data.getStringArrayListExtra(ContactsPickerActivity.EXTRA_RESULT_USERNAMES);
                if (users != null) {
                    //int[] providers = data.getIntArrayExtra(ContactsPickerActivity.EXTRA_RESULT_PROVIDER);
                    //int[] accounts = data.getIntArrayExtra(ContactsPickerActivity.EXTRA_RESULT_ACCOUNT);

                    //start group and do invite here
                }
            }
        }
    }
}
