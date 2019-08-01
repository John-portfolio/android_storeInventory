package com.sigilius.storeinventory.data;


import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import static com.sigilius.storeinventory.data.InventoryContract.*;

/**
 * {@link ContentProvider} for Inventory app
 */
public class InventoryProvider extends ContentProvider{

    public static final String LOG_TAG = InventoryProvider.class.getSimpleName();

    /** URI matcher code for the content URI for the store table */
    private static final int INVENTORY = 100;
    /** URI matcher code for the content URI for single inventory in the store table */
    private static final int INVENTORY_ID = 101;

    /**
     * UriMatcher object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_INVENTORY,INVENTORY);
        sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_INVENTORY + "/#", INVENTORY_ID);
    }

    /** Database helper object */
    private InventoryDbHelper mDbHelper;


    @Override
    public boolean onCreate() {
        mDbHelper = new InventoryDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        Cursor cursor;

        int match = sUriMatcher.match(uri);

        switch (match) {
            case INVENTORY:
                cursor = database.query(InventoryEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case INVENTORY_ID:
                selection = InventoryEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };

                cursor = database.query(InventoryEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                return InventoryEntry.CONTENT_LIST_TYPE;
            case INVENTORY_ID:
                return InventoryEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                return insertInventory(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Insert an item into the database with the given content values. Return the new content URI
     * for that specific row in the database.
     */
    private Uri insertInventory(Uri uri, ContentValues values) {
        String name = values.getAsString(InventoryEntry.COLUMN_INVENTORY_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Inventory item requires a name");
        }

        Integer price = values.getAsInteger(InventoryEntry.COLUMN_INVENTORY_PRICE);
        if (price != null && price < 0) {
            throw new IllegalArgumentException("Inventory item requires a valid price");
        }

        Integer supplier = values.getAsInteger(InventoryEntry.COLUMN_INVENTORY_SUPPLIER);
        if (supplier == null || !InventoryEntry.isValidSupplier(supplier)) {
            throw new IllegalArgumentException("Inventory requires valid supplier");
        }

        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        long id = database.insert(InventoryEntry.TABLE_NAME, null, values);

        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return ContentUris.withAppendedId(uri, id);
    }


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int rowsDeleted;

        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                // Delete all rows that match the selection and selection args
                rowsDeleted = database.delete(InventoryEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case INVENTORY_ID:
                // Delete a single row given by the ID in the URI
                selection = InventoryEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                rowsDeleted = database.delete(InventoryEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                return updateInventory(uri, contentValues, selection, selectionArgs);
            case INVENTORY_ID:
                selection = InventoryEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                return updateInventory(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    /**
     * Update inventory in the database with the given content values. Apply the changes to the rows
     * specified in the selection and selection arguments (which could be 0 or 1 or more inventory items).
     * Return the number of rows that were successfully updated.
     */
    private int updateInventory(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        /**
         * If the {@link InventoryEntry#COLUMN_INVENTORY_IMAGE} key is present,
         * check that the name value is not null.
         */
        if (values.containsKey(InventoryEntry.COLUMN_INVENTORY_NAME)) {
            String name = values.getAsString(InventoryEntry.COLUMN_INVENTORY_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Inventory requires a name");
            }
        }

        /**
         * If the {@link InventoryEntry#COLUMN_INVENTORY_SUPPLIER} key is present,
         * check that the gender value is valid.
         */
        if (values.containsKey(InventoryEntry.COLUMN_INVENTORY_SUPPLIER)) {
            Integer supplier = values.getAsInteger(InventoryEntry.COLUMN_INVENTORY_SUPPLIER);
            if (supplier == null || !InventoryEntry.isValidSupplier(supplier)) {
                throw new IllegalArgumentException("Pet requires valid gender");
            }
        }

        /**
         * If the {@link InventoryEntry#COLUMN_INVENTORY_QUANTITY} key is present,
         * check that the weight value is valid.
         */
        if (values.containsKey(InventoryEntry.COLUMN_INVENTORY_PRICE)) {
            Integer price = values.getAsInteger(InventoryEntry.COLUMN_INVENTORY_PRICE);
            if (price != null && price < 0) {
                throw new IllegalArgumentException("Inventory requires valid price");
            }
        }

        if (values.size() == 0) {
            return 0;
        }

        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        int rowsUpdated = database.update(InventoryEntry.TABLE_NAME, values, selection, selectionArgs);

        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsUpdated;
    }

}
