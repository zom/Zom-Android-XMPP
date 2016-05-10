package org.awesomeapp.messenger.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import org.awesomeapp.messenger.Preferences;

import java.util.ArrayList;

import im.zom.messenger.R;
import info.guardianproject.panic.PanicResponder;

public class PanicSetupActivity extends BaseActivity {

    private final static int REQUEST_CHOOSE_CONTACT = 9782;

    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (PanicResponder.checkForDisconnectIntent(this)) {
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

        /* These preferences are setup to represent what will happen to the user,
         * they do not represent what the app must do.  For example, uninstalling
         * the app will by definition delete all the app's data, so when the
         * "Uninstall" checkbox is on, the "Clear Data" checkbox must also be on.
         * But the app does not need to clear the data if it is going to be
         * uninstalled anyway. */
        final CheckBox lockApp = (CheckBox) findViewById(R.id.lock_app);
        final CheckBox clearAppData = (CheckBox) findViewById(R.id.clear_app_data);
        final CheckBox uninstallApp = (CheckBox) findViewById(R.id.uninstall_app);

        lockApp.setChecked(Preferences.lockApp());
        lockApp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Preferences.setLockApp(isChecked);
            }
        });

        clearAppData.setChecked(Preferences.clearAppData());
        clearAppData.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Preferences.setClearAppData(isChecked);
                if (!isChecked) {
                    uninstallApp.setChecked(false);
                    Preferences.setUninstallApp(false);
                }
            }
        });

        uninstallApp.setChecked(Preferences.uninstallApp());
        uninstallApp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Preferences.setUninstallApp(isChecked);
                if (isChecked) {
                    clearAppData.setChecked(true);
                    Preferences.setClearAppData(true);
                }
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
                PanicResponder.setTriggerPackageName(this);
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
