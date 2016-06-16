    package org.awesomeapp.messenger.bho;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import net.sqlcipher.DatabaseUtils;

import java.io.File;
import java.util.ArrayList;

import im.zom.messenger.R;

    /**
 * Created by n8fr8 on 6/2/16.
 */
public class DictionarySearch {


    //	public final static String DEFAULT_FONT = "Jomolhari-alpha3c-0605331.ttf";
    public final static String DEFAULT_FONT = "monlamuniouchan2.ttf";
    //"Monlam Uni Sans Serif.ttf";//"monlamuniouchan3.ttf";//"monlambodyig.ttf";


    public final static String COLUMN_ID = "_id";
    public final static String COLUMN_WORD = "word";
    public final static String COLUMN_MEANING = "definition";

    public final static int QUERY_LIMIT = 5;

    private final static String DB_FOLDER_NAME = "monlam";
    private DictionaryAdapter database;

        private int MY_PERMISSIONS_REQUEST_FILE = 1;

    public DictionarySearch (Activity context) {


        int permissionCheck = ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permissionCheck == PackageManager.PERMISSION_DENIED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(context,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {


            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(context,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_FILE);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }

            return;
        }

        File dbPath = new File(Environment.getExternalStorageDirectory(), DB_FOLDER_NAME);

        if (dbPath.exists())
            database = new DictionaryAdapter(context, dbPath, "tbtotb","tbtotb");
    }

    public void close ()
    {
        if (database != null) {
            database.close();
            database = null;
        }
    }

    public boolean exists ()
    {
        return database != null;
    }

    public ArrayList<String> getMatchingWords (String queryString)
    {
        if (database == null)
            return null;

        ArrayList<String> results = null;

        String queryText = COLUMN_WORD + " LIKE " + DatabaseUtils.sqlEscapeString(queryString + "%");

        //OR " + COLUMN_MEANING + " LIKE '%" + queryString + "%'";

        Cursor cursor = database.getAllEntries(new String[] {COLUMN_WORD}, queryText, null, null, null, COLUMN_WORD, " ASC LIMIT " + QUERY_LIMIT);

        if (cursor != null) {
            if (cursor.getCount() > 0) {

                results = new ArrayList<String>();

                while (cursor.moveToNext()) {
                    results.add(cursor.getString(0));

                }

            }

            cursor.close();
        }

        return results;
    }


}
