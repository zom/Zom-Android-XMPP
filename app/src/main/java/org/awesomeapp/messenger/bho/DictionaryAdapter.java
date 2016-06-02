package org.awesomeapp.messenger.bho;


import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by n8fr8 on 6/2/16.
 */
public class DictionaryAdapter {


        private static final int DATABASE_VERSION = 1;

        // Index Key column
        public static final String KEY_ID = "_id";

        private Context mContext = null;

        private String mDbName = null;

        private String mDefaultTable = null;
        // Variable to hold database instant
        private SQLiteDatabase db;
        private File mDbPath;
        private File mDbFile;

        /**
         * Open the database if it exists or create it if it doesn't. Additionally checks if the
         * table exists and creates it if it doesn't.
         * @param context Context passed by the parent.
         */
        @SuppressWarnings("unchecked")
        public DictionaryAdapter(Context context, File dbPath, String dbName, String defaultTable){
            // Start initializing all of the variables

            mContext = context;
            mDbName = dbName;
            mDefaultTable = defaultTable;
            mDbPath = dbPath;
            mDbFile = new File(dbPath, mDbName);

            if (mDbFile.exists())
            {
                open();
            }
        }



        /**
         * Open the connection to the database.
         * @return Returns a DBAdapter.
         * @throws SQLException
         */
        private DictionaryAdapter open() throws SQLException {
            db = SQLiteDatabase.openOrCreateDatabase(mDbFile, null);

            return this;
        }

        /**
         * Close the connection to the database.
         */
        public void close() {
            db.close();
        }

        /**
         * Insert a row into the database.
         * @param key ArrayList of Keys (column headers).
         * @param value ArrayList of Key values.
         * @return Returns the number of the row added.
         */
        public long insertEntry(ArrayList<String> key, ArrayList<String> value) {
            ContentValues contentValues = new ContentValues();
            for(int i = 0; key.size() > i; i++){
                contentValues.put(key.get(i), value.get(i));
            }
            Log.v("Database Add", contentValues.toString());
            return db.insert(mDefaultTable, null, contentValues);
        }

        /**
         * Remove a row from the database.
         * @param rowIndex Number of the row to remove.
         * @return Returns TRUE if it was deleted, FALSE if failed.
         */
        public boolean removeEntry(long rowIndex) {
            return db.delete(mDefaultTable, KEY_ID + "=" + rowIndex, null) > 0;
        }

        /**
         * Get all entries in the database sorted by the given value.
         * @param columns List of columns to include in the result.
         * @param selection Return rows with the following string only. Null returns all rows.
         * @param selectionArgs Arguments of the selection.
         * @param groupBy Group results by.
         * @param having A filter declare which row groups to include in the cursor.
         * @param sortBy Column to sort elements by.
         * @param sortOption ASC for ascending, DESC for descending.
         * @return Returns a cursor through the results.
         */
        public Cursor getAllEntries(String[] columns, String selection, String[] selectionArgs,
                                    String groupBy, String having, String sortBy, String sortOption) {
            return db.query(mDefaultTable, columns, selection, selectionArgs, groupBy,
                    having, sortBy + " " + sortOption);
        }

        /**
         * Does the SQL UPDATE function on the table with given SQL string
         * @param sqlQuery an SQL Query starting at SET
         */
        public void update(String sqlQuery) {
            db.rawQuery("UPDATE " + mDefaultTable + sqlQuery, null);
        }

        /**
         * Get all entries in the database sorted by the given value.
         * @param columns List of columns to include in the result.
         * @param selection Return rows with the following string only. Null returns all rows.
         * @param selectionArgs Arguments of the selection.
         * @param groupBy Group results by.
         * @param having A filter declare which row groups to include in the cursor.
         * @param sortBy Column to sort elements by.
         * @param sortOption ASC for ascending, DESC for descending.
         * @param limit limiting number of records to return
         * @return Returns a cursor through the results.
         */
        public Cursor getAllEntries(String[] columns, String selection, String[] selectionArgs,
                                    String groupBy, String having, String sortBy, String sortOption, String limit) {
            return db.query(mDefaultTable, columns, selection, selectionArgs, groupBy,
                    having, sortBy + " " + sortOption, limit);
        }


        /**
         * This is a function that should only be used if you know what you're doing.
         * It is only here to clear the appended test data. This clears out all data within
         * the table specified when the database connection was opened.
         * @return Returns TRUE if successful. FALSE if not.
         */
        public boolean clearTable() {
            return db.delete(mDefaultTable, null, null) > 0;
        }

        /**
         * Update the selected row of the open table.
         * @param rowIndex Number of the row to update.
         * @param key ArrayList of Keys (column headers).
         * @param value ArrayList of Key values.
         * @return Returns an integer.
         */
        public int updateEntry(long rowIndex, ArrayList<String> key, ArrayList<String> value) {
            String where = KEY_ID + "=" + rowIndex;
            ContentValues contentValues = new ContentValues();
            for(int i = 0; key.size() > i; i++){
                contentValues.put(key.get(i), value.get(i));
            }
            return db.update(mDefaultTable, contentValues, where, null);
        }

}
