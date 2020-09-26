package com.example.clientserver;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class Database extends SQLiteOpenHelper {
    private String TABLE_NAME;
    private String col1 = "RID", col2 = "NEW_MESSAGES";

    Database(@Nullable Context context, @Nullable String dataBasename, int version, String table_name) {
        super(context, dataBasename, null, version);
        this.TABLE_NAME = table_name;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME +
                "(RID CHAR(25) PRIMARY KEY,NEW_MESSAGES INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean insertData(String id, int new_messages) {
        long result;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(col1, id);
        cv.put(col2, new_messages);
        result = db.insert(TABLE_NAME, null, cv);
        return result != -1;
    }

    public int getData(String rid) {
        SQLiteDatabase db = this.getWritableDatabase();
        int value;
        String query = "SELECT NEW_MESSAGES FROM " + TABLE_NAME + " WHERE RID = '" + rid + "'";
        Cursor c = db.rawQuery(query, null);
        if (c.moveToFirst()) {
            value = c.getInt(0);
        } else value = -1;
        c.close();
        return value;
    }

    Integer delete(String rid) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_NAME, "RID = '" + rid + "'", null/*new String[]{rid}*/);
    }

    void updateData(String rid, int new_value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(col1, rid);
        cv.put(col2, new_value);
        db.update(TABLE_NAME, cv, "RID = '" + rid + "'", null);
    }
}
