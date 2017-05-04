package org.awesomeapp.messenger.tasks;

import android.os.AsyncTask;

/**
 * Created by n8fr8 on 5/1/17.
 */

public class MigrantAccountTask extends AsyncTask<String, Void, Integer> {

    @Override
    protected Integer doInBackground(String... strings) {

        //get existing account username

        //find or use provided new server/domain

        //register account on new domain with same password

        //send migration message to existing contacts

        //add existing contacts on new server

        //archive existing conversations and contacts

        //login and set new default account

        return null;
    }

    @Override
    protected void onPostExecute(Integer integer) {
        super.onPostExecute(integer);
    }
}
