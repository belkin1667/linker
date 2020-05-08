package com.belkin.linker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

class DataBase {
    final static String CLASS_LOG_TAG = "DataBase";

    static private DBHelper dbHelper;
    static private SQLiteDatabase db;
    static private List<Link> links;
    static private LinkToListItemAdapter adapter;
    static private Context context;

    private DataBase() {}

    static void setContext(Context mContext) {
        Log.i(CLASS_LOG_TAG, "setContext(Context) method call");
        context = mContext;
        if(links == null)
            links = new ArrayList<>();
    }

    static void setAdapter(LinkToListItemAdapter mAdapter) {
        adapter = mAdapter;
    }

    static void readAllDataBase() {
        Log.i(CLASS_LOG_TAG, "readAllDataBase() method call");
        if (openDatabase()) {
            db = dbHelper.getWritableDatabase();
            Cursor c = db.query(DBHelper.TABLE_NAME, null, null, null, null, null, null);
            if (c.moveToFirst()) {
                // определяем номера столбцов по имени в выборке
                int idIndex = c.getColumnIndex("id");
                int urlIndex = c.getColumnIndex("url");
                int hostIndex = c.getColumnIndex("host");
                int headerIndex = c.getColumnIndex("header");
                int datetimeIndex = c.getColumnIndex("datetime");
                int imageUrlIndex = c.getColumnIndex("imageUrl");
                do {
                    // получаем значения по номерам столбцов и пишем все в лог
                    int id = c.getInt(idIndex);
                    String url = c.getString(urlIndex);
                    String host = c.getString(hostIndex);
                    String header = c.getString(headerIndex);
                    String datetime = c.getString(datetimeIndex);
                    String imageUrl = c.getString(imageUrlIndex);
                    Link link = new Link(id, url, host, header, imageUrl, datetime);
                    links.add(link);
                    Log.i(CLASS_LOG_TAG, "Read from database " + DBHelper.DB_NAME + " from table " + DBHelper.TABLE_NAME + ": " + link.toString());
                } while (c.moveToNext());
            }
            dbHelper.close();
        }
    }

    static void writeToDataBase(List<Link> links) {
        Log.i(CLASS_LOG_TAG, "writeToDataBase(List<Link>) method call");
        if (openDatabase()) {
            db = dbHelper.getWritableDatabase();
            for (Link link : links) {
                dbWrite(link);
            }
            db.close();
        }
    }

    static void writeToDataBase(Link link) {
        Log.i(CLASS_LOG_TAG, "writeToDataBase(Link) method call");
        if (openDatabase()) {
            db = dbHelper.getWritableDatabase();
            dbWrite(link);
            db.close();
        }
    }

    private static long dbWrite(Link link) {
        Log.i(CLASS_LOG_TAG, "insertToDataBase(Link) method call");
        if (openDatabase()) {
            ContentValues cv = new ContentValues();
            cv.put("url", link.getUrl());
            cv.put("host", link.getHost());
            cv.put("header", link.getHeader());
            cv.put("datetime", link.getDatetime());
            cv.put("imageUrl", link.getImageUrl());
            link.setId(db.insert(DBHelper.TABLE_NAME, null, cv));

            Log.i(CLASS_LOG_TAG, "Write to database " + DBHelper.DB_NAME + " to table " + DBHelper.TABLE_NAME + ": " + link.toString());

            return link.getId();
        }
        return -1;
    }

    static void deleteFromDataBase(List<Link> links) {
        Log.i(CLASS_LOG_TAG, "deleteFromDataBase(List<Link>) method call");
        if (openDatabase()) {
            for (Link link : links) {
                deleteFromDataBase(link.getId());
            }
        }
    }

    static void deleteFromDataBase(Link link) {
        Log.i(CLASS_LOG_TAG, "deleteFromDataBase(Link) method call");
        if (openDatabase()) {
            deleteFromDataBase(link.getId());
        }
    }

    static private void deleteFromDataBase(long id) {
        Log.i(CLASS_LOG_TAG, "deleteFromDataBase(long) method call");
        if (openDatabase()) {
            int delCount = db.delete(DBHelper.TABLE_NAME, "id=?", new String[]{String.valueOf(id)});
            if (delCount == 1) {
                Log.i(CLASS_LOG_TAG, "Delete from database " + DBHelper.DB_NAME + " from table " + DBHelper.TABLE_NAME + ": row id = " + id);
            }
            else {
                Log.i(CLASS_LOG_TAG, "Cannot delete from database " + DBHelper.DB_NAME + " from table " + DBHelper.TABLE_NAME + ": row id = " + id + " not found");
            }

            try {

            }
            catch (Exception e) {
                Log.d("DEBUG", e.getMessage());
            }

        }
    }

    private static boolean openDatabase() {
        Log.i(CLASS_LOG_TAG, "isDbHelperSet() method call");
        if (dbHelper == null) {
            Log.i(CLASS_LOG_TAG, "DBHelper is not set");
            Log.i(CLASS_LOG_TAG, "Trying to get it from context");
            Log.e(CLASS_LOG_TAG, "DBHelper is not set. Context is not provided. Please, provide context with setContext() method");

            if (context == null) {
                Log.i(CLASS_LOG_TAG, "Context is not set");
                Log.e(CLASS_LOG_TAG, "Context is not provided. Please, provide context with setContext() method");
                return false;
            }
            else {
                dbHelper = new DBHelper(context);
                db = dbHelper.getWritableDatabase();
                Log.i(CLASS_LOG_TAG, "DBHelper is set");
                Log.i(CLASS_LOG_TAG, "Database is set");
                return true;
            }
        }
        else {
            db = dbHelper.getWritableDatabase();
            Log.i(CLASS_LOG_TAG, "Database is set");
            return true;
        }
    }

    static List<Link> getData() {
        Log.i(CLASS_LOG_TAG, "getData() method call");
        return links;
    }


    static void addNewLink(List<String> urls) {
        Log.i(CLASS_LOG_TAG, "addNewLink(List<String>) method call");
        for (String url : urls) {
            addNewLink(url);
        }
    }

    static void addNewLink(String url) {
        Log.i(CLASS_LOG_TAG, "addNewLink(String) method call");
        Link link = new Link(url, adapter, links.size());
        links.add(link);
        adapter.notifyDataSetChanged();

    }

    static private class DBHelper extends SQLiteOpenHelper  {
        final private static String CLASS_LOG_TAG = DataBase.CLASS_LOG_TAG + ".DBHelper";

        final static String TABLE_NAME = "mytable";
        final static String DB_NAME = "myDB";

        DBHelper(Context context) {
            super(context, DB_NAME, null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.i(CLASS_LOG_TAG, "onCreate(SQLiteDatabase) method call");
            db.execSQL("CREATE TABLE " + TABLE_NAME + " " +
                    "(" +
                    "id integer primary key autoincrement," +
                    "url text," +
                    "host text," +
                    "header text," +
                    "datetime text," +
                    "imageUrl text" +
                    ")");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.i(CLASS_LOG_TAG, "onUpgrade(SQLiteDatabase, int, int) method call");
        }
    }
}
