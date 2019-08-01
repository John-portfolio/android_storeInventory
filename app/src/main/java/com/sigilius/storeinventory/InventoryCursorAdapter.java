package com.sigilius.storeinventory;


import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.NumberFormat;
import java.util.Locale;

import com.sigilius.storeinventory.data.InventoryContract.*;

/**
 * {@link InventoryCursorAdapter} is an adapter for a list or grid view
 * that uses a {@link Cursor} of inventory data as its data source. This adapter knows
 * how to create list items for each row of inventory data in the {@link Cursor}.
 */
public class InventoryCursorAdapter extends CursorAdapter {

    /**
     * Constructs a new {@link InventoryCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public InventoryCursorAdapter(Context context, Cursor c) {

        super(context, c, 0 /* flags */);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the pet data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current pet can be set on the name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override

    public void bindView(View view, final Context context, final Cursor cursor) {
        // Find the individual views that we want to modify in the list item layout
        TextView nameTextView = (TextView) view.findViewById(R.id.name);
        final TextView quantityTextView = (TextView) view.findViewById(R.id.quantity);
        TextView priceTextView = (TextView) view.findViewById(R.id.price);
        final Button sellButton = (Button) view.findViewById(R.id.btnSell);
        RelativeLayout parentView = (RelativeLayout) view.findViewById(R.id.list_item_layout);

        // Find the columns of inventory attributes that we're interested in
        int idColumnIndex = cursor.getColumnIndex(InventoryEntry._ID);
        int nameColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_NAME);
        int quantityColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_QUANTITY);
        int priceColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_PRICE);

        // Read the inventory attributes from the cursor for the current item
        final int rowId = cursor.getInt(idColumnIndex);
        String inventoryName = cursor.getString(nameColumnIndex);
        String inventoryQuantity = cursor.getString(quantityColumnIndex);
        int inventoryPrice = Integer.parseInt(cursor.getString(priceColumnIndex)) / 100;
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
        String priceString = formatter.format(inventoryPrice);

        // update the TextViews with the attributes
        nameTextView.setText(inventoryName);
        quantityTextView.setText(inventoryQuantity);
        priceTextView.setText(priceString);


        parentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, EditorActivity.class);

                Uri currentInventoryUri = ContentUris.withAppendedId(InventoryEntry.CONTENT_URI, rowId);

                intent.setData(currentInventoryUri);

                context.startActivity(intent);
            }
        });

        sellButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int quantity = Integer.parseInt(quantityTextView.getText().toString());
                if ( quantity > 0 ) {
                    quantity--;

                    String quantityString = Integer.toString(quantity);

                    ContentValues values = new ContentValues();
                    values.put(InventoryEntry.COLUMN_INVENTORY_QUANTITY, quantityString);

                    Uri uri = ContentUris.withAppendedId(InventoryEntry.CONTENT_URI, rowId);

                    int rowsAffected = context.getContentResolver().update(uri, values, null, null);

                    if (rowsAffected != 0 ) {
                        quantityTextView.setText(quantityString);
                    }

                }
            }
        });
    }
}

