package edu.nyu.scps.stroker;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class Helper extends SQLiteOpenHelper {
    String tableName;

    public Helper(Context context, String tableName) {
        super(context, "strokes.db", null, 1);
        this.tableName = tableName;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //an SQLite statement.
        String statement = "CREATE TABLE " + tableName + " (\n"
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + "stroke INTEGER,\n"
                + "x REAL,\n"
                + "y REAL\n"
                + ");";

        db.execSQL(statement);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
