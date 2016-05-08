package org.awesomeapp.messenger.ui;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import org.awesomeapp.messenger.ui.legacy.SettingActivity;
import org.awesomeapp.messenger.ui.onboarding.OnboardingActivity;

import im.zom.messenger.R;

public class AccountsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.awesome_activity_accounts);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_accounts, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;


            case R.id.menu_add_account:
                Intent i = new Intent(this, OnboardingActivity.class);
                i.putExtra("showSplash",false);
                startActivity(i);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
