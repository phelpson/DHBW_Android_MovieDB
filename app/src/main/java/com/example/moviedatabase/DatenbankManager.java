package com.example.moviedatabase;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;


public class DatenbankManager extends SQLiteOpenHelper {

    protected SQLiteStatement _statementInsertNeuAbk = null;
    SQLiteDatabase db;

    /**
     * Prepared SQL-Statement, um zu einer bereits in der DB eingetragenen
     * Abkürzung die erste oder eine weitere Bedeutung einzutragen.
     */
    protected SQLiteStatement _statementInsertBedeutung = null;

    public DatenbankManager(Context context) {

        super(context,
                "movie.db",  // Name der DB
                null,          // Default-CursorFactory verwenden
                1);            // Versions-Nummer

        //get database
        db = getReadableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d("databaseStatus", "database oncreate reached");
        try {
            //create table moviefav to store favourite list
            db.execSQL("CREATE TABLE movieFav ( " +
                    "movfav_id     INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "movietitle TEXT NOT NULL ); "
            );

        } catch (Exception e) {

        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void saveTitle(String title) {

        //insert new title into movie fav table in database
        db.execSQL("INSERT INTO movieFav (movietitle) VALUES('" + title + "');");
    }

    public Boolean checkTitle(String title) {

        //replace url specific characters
        title = title.replace("+", " ");
        title = title.replace("%3A", ":");

        //get cursor from db raw query
        Cursor cursor = db.rawQuery("SELECT * FROM movieFav WHERE movietitle = '" + title + "'", null);

        // *** step through results with cursor ***
        int resultRows = cursor.getCount();
        //check if there are any results
        if (resultRows == 0) {
            //if there are no results return false
            return false;
        } else {
            //if there is >0 results return true
            return true;
        }

    }

    //delete selected title from database
    public void deleteTitle(String title) {
        //create ID
        db.execSQL("DELETE FROM movieFav WHERE movietitle = '" + title + "'");
    }

    //returns list with all favourised movies from db
    public String[] getFavMovieList() {
        //test
        Cursor cursor = db.rawQuery("SELECT movietitle " +
                        "  FROM movieFav",
                null);

        // *** check cursor count if there are any favourite movies ***
        int resultRows = cursor.getCount();

        //if result is 0, return empty string list
        if (resultRows == 0) {
            return new String[]{};
        }

        String[] resultStrings = new String[resultRows];
        //set counter to 0
        int counter = 0;
        //iterate through favourite movies with cursor
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            resultStrings[counter] = cursor.getString(0);
            counter++;
        }
        //return results
        return resultStrings;
    }
}
