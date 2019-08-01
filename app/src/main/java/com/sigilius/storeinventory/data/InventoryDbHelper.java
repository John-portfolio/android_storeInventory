package com.sigilius.storeinventory.data;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static com.sigilius.storeinventory.data.InventoryContract.*;

public class InventoryDbHelper extends SQLiteOpenHelper {

    private static final String LOG_TAG = InventoryDbHelper.class.getName();
    private static final String DB_NAME = "store.db";
    private static final int DB_VERSION = 3;

    /** needed default constructor */
    public InventoryDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + InventoryEntry.TABLE_NAME + "( " +
                    InventoryEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    InventoryEntry.COLUMN_INVENTORY_NAME +  " TEXT NOT NULL, " +
                    InventoryEntry.COLUMN_INVENTORY_QUANTITY + " INTEGER NOT NULL DEFAULT 0, " +
                    InventoryEntry.COLUMN_INVENTORY_PRICE + " INTEGER NOT NULL, " +
                    InventoryEntry.COLUMN_INVENTORY_SUPPLIER +  " INTEGER NOT NULL, " +
                    InventoryEntry.COLUMN_INVENTORY_EMAIL +  " TEXT NOT NULL, " +
                    InventoryEntry.COLUMN_INVENTORY_IMAGE + " TEXT );";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + InventoryEntry.TABLE_NAME;

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

}
