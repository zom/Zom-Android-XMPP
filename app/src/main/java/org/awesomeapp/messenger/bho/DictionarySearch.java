    package org.awesomeapp.messenger.bho;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;

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

    public DictionarySearch (Context context) {
        File dbPath = new File(Environment.getExternalStorageDirectory(), DB_FOLDER_NAME);

        if (dbPath.exists())
            database = new DictionaryAdapter(context, dbPath, "tbtotb","tbtotb");
    }

    public boolean exists ()
    {
        return database != null;
    }

    public ArrayList<String> getMatchingWords (String queryString)
    {
        ArrayList<String> results = null;

        String queryText = COLUMN_WORD + " LIKE '" + queryString + "%'";

        //OR " + COLUMN_MEANING + " LIKE '%" + queryString + "%'";

        Cursor cursor = database.getAllEntries(new String[] {COLUMN_WORD}, queryText, null, null, null, COLUMN_WORD, " ASC LIMIT " + QUERY_LIMIT);

        if (cursor != null) {

            results = new ArrayList<String>();

            while (!cursor.isLast())
            {
                results.add(cursor.getString(0));
                cursor.moveToNext();
            }

            cursor.close();
        }

        return results;
    }


}
