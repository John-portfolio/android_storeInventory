package com.sigilius.storeinventory;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.os.ParcelFileDescriptor;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.sigilius.storeinventory.data.InventoryContract.*;
import com.sigilius.storeinventory.data.InventoryDbHelper;

import org.w3c.dom.Text;

import java.io.FileDescriptor;
import java.io.IOException;

import static com.sigilius.storeinventory.data.InventoryProvider.LOG_TAG;

public class EditorActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>{

    /** Identifier for the pet data loader */
    private static final int EXISTING_INVENTORY_LOADER = 0;

    /** Content URI for the existing item (null if it's a new item) */
    private Uri mCurrentInventoryUri;

    /** Uri for tracking the image location */
    private Uri mUri;

    /** EditText field to enter inventory name */
    private EditText mNameEditText;

    /** EditText field to enter inventory quantity */
    private EditText mQuantityEditText;

    /** EditText field to enter inventory price */
    private EditText mPriceEditText;

    /** TextView for Supplier emai */
    private TextView mSupplierEmailText;

    /** ImageView to enter inventory image */
    private ImageView mImageEntry;

    /** Boolean to track image request */
    private static final int PICK_IMAGE_REQUEST = 0;

    /** EditText field to enter the inventory supplier */
    private Spinner mSupplierSpinner;

    /** Button to delete all Inventory in this view */
    private Button mDeleteInventoryButton;

    /** Button to fire intent for order email*/
    private Button mOrderButton;

    /** Button to update item quantity */
    private Button mChangeQuantityButton;

    /** Button to increase item quantity */
    private Button mAddButton;

    /** Button to lower item quantity */
    private Button mMinusButton;

    /** TextView for quantoty change */
    private TextView mModifiedQuantity;

    /** Default supplier */
    private int mSupplier = InventoryEntry.ACME;

    /** Boolean flag that keeps track of whether the item has been edited (true) or not (false) */
    private boolean mInventoryHasChanged = false;

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mInventoryHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new item or editing an existing item
        Intent intent = getIntent();
        mCurrentInventoryUri = intent.getData();

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_inventory_name);
        mQuantityEditText = (EditText) findViewById(R.id.edit_inventory_quantity);
        mPriceEditText = (EditText) findViewById(R.id.edit_inventory_price);
        mImageEntry = (ImageView) findViewById(R.id.edit_inventory_image);
        mSupplierSpinner = (Spinner) findViewById(R.id.spinner_supplier);
        mSupplierEmailText = (TextView) findViewById(R.id.email);

        mDeleteInventoryButton = (Button) findViewById(R.id.delete_all);
        mChangeQuantityButton = (Button) findViewById(R.id.change_quantity);
        mOrderButton = (Button) findViewById(R.id.order_inventory);
        mAddButton = (Button) findViewById(R.id.plus);
        mMinusButton = (Button) findViewById(R.id.minus);
        mModifiedQuantity = (TextView) findViewById(R.id.buy_sell);

        mNameEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mImageEntry.setOnTouchListener(mTouchListener);
        mSupplierSpinner.setOnTouchListener(mTouchListener);
        mModifiedQuantity.setOnTouchListener(mTouchListener);

        // If the intent does not contain a item content URI, then we are
        // creating a new inventory item
        if (mCurrentInventoryUri == null) {
            setTitle(getString(R.string.editor_activity_title_new_inventory));
            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete inventory that hasn't been created yet.)
            invalidateOptionsMenu();
            // Hide things not needed here
            mAddButton.setVisibility(View.GONE);
            mMinusButton.setVisibility(View.GONE);
            mModifiedQuantity.setVisibility(View.GONE);
            mOrderButton.setVisibility(View.GONE);
            mChangeQuantityButton.setVisibility(View.GONE);
            mDeleteInventoryButton.setVisibility(View.GONE);

        } else {
            setTitle(getString(R.string.editor_activity_title_edit_inventory));
            getLoaderManager().initLoader(EXISTING_INVENTORY_LOADER, null, this);
        }


        mImageEntry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent;

                if (Build.VERSION.SDK_INT < 19) {
                    intent = new Intent(Intent.ACTION_GET_CONTENT);
                } else {
                    intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                }

                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
            }

        });

        mDeleteInventoryButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                showDeleteConfirmationDialog();
            }
        });

        mChangeQuantityButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                int existingQuantity = Integer.parseInt(mQuantityEditText.getText().toString());
                int changeQuantity = Integer.parseInt(mModifiedQuantity.getText().toString());
                if ((existingQuantity += changeQuantity) < 0) {
                    existingQuantity = 0;
                }
                mQuantityEditText.setText(Integer.toString(existingQuantity));
                mModifiedQuantity.setText("0");
            }
        });

        mOrderButton.setOnClickListener(new View.OnClickListener() {
            String emailAddress;
            String subject = "New Order";

            public void onClick(View view) {
                emailAddress = mSupplierEmailText.getText().toString().trim();
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:" + emailAddress));
                intent.putExtra(Intent.EXTRA_SUBJECT, subject);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });

        mAddButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                int changeQuantity = Integer.parseInt(mModifiedQuantity.getText().toString());
                changeQuantity++;
                mModifiedQuantity.setText(Integer.toString(changeQuantity));
            }
        });

        mMinusButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                int changeQuantity = Integer.parseInt(mModifiedQuantity.getText().toString());
                changeQuantity--;
                mModifiedQuantity.setText(Integer.toString(changeQuantity));
            }
        });

        setupSpinner();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {

            if (resultData != null) {
                mUri = resultData.getData();
                Log.i(LOG_TAG, "Uri: " + mUri.toString());

                mImageEntry.setImageBitmap(getBitmapFromUri(mUri));
            }
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) {
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            parcelFileDescriptor.close();
            return image;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to load image.", e);
            return null;
        } finally {
            try {
                if (parcelFileDescriptor != null) {
                    parcelFileDescriptor.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "Error closing ParcelFile Descriptor");
            }
        }
    }


    /**
     * Setup the dropdown spinner that allows the user to select the gender of the pet.
     */
    private void setupSpinner() {
        ArrayAdapter genderSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_supplier_options, android.R.layout.simple_spinner_item);

        genderSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        // Apply the adapter to the spinner
        mSupplierSpinner.setAdapter(genderSpinnerAdapter);

        // Set the integer mSelected to the constant values
        mSupplierSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String) parent.getItemAtPosition(position);
                if (!TextUtils.isEmpty(selection)) {
                    switch (position) {
                        case 0:
                            mSupplier = InventoryEntry.ACME;
                            mSupplierEmailText.setText(InventoryEntry.ACME_EMAIL);
                            break;
                        case 1:
                            mSupplier = InventoryEntry.OOW_ELECTRONICS;
                            mSupplierEmailText.setText(InventoryEntry.OOW_EMAIL);
                            break;
                        case 2:
                            mSupplier = InventoryEntry.GEMCO;
                            mSupplierEmailText.setText(InventoryEntry.GEMCO_EMAIL);
                            break;
                        case 3:
                            mSupplier = InventoryEntry.POKE_GO;
                            mSupplierEmailText.setText(InventoryEntry.POKE_GO_EMAIL);
                            break;
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mSupplier = InventoryEntry.ACME;
            }
        });
    }


    /**
     * Get user input from editor and save new inventory into database
     */
    private void saveInventory() {
        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        String nameString = mNameEditText.getText().toString().trim();
        String quantityString = mQuantityEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();
        String emailString = mSupplierEmailText.getText().toString().trim();

        if ((mCurrentInventoryUri == null) && (TextUtils.isEmpty(nameString) ||
                TextUtils.isEmpty(priceString) || mUri == null )) {
             showIncompleteFormDialog();
             return;
        }

        if ((mCurrentInventoryUri != null) && (TextUtils.isEmpty(nameString) ||
                TextUtils.isEmpty(priceString))) {
            showIncompleteFormDialog();
            return;
        }

        ContentValues values = new ContentValues();
        values.put(InventoryEntry.COLUMN_INVENTORY_NAME, nameString);
        values.put(InventoryEntry.COLUMN_INVENTORY_QUANTITY, quantityString);
        values.put(InventoryEntry.COLUMN_INVENTORY_PRICE, priceString);
        values.put(InventoryEntry.COLUMN_INVENTORY_SUPPLIER, mSupplier);
        values.put(InventoryEntry.COLUMN_INVENTORY_EMAIL, emailString);
        if (mUri != null) {
            values.put(InventoryEntry.COLUMN_INVENTORY_IMAGE, mUri.toString());
        }

        // If the quantity is not provided by the user, don't try to parse the string into an
        // integer value. Use 0 by default.
        int quantity = 0;
        if (!TextUtils.isEmpty(quantityString)) {
            quantity = Integer.parseInt(quantityString);
        }
        values.put(InventoryEntry.COLUMN_INVENTORY_QUANTITY, quantity);


        if (mCurrentInventoryUri == null ) {
            Uri newUri = getContentResolver().insert(InventoryEntry.CONTENT_URI, values);

            if (newUri == null) {
                Toast.makeText(this, getString(R.string.editor_insert_inventory_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.editor_insert_inventory_successful),
                        Toast.LENGTH_SHORT).show();
            }

        } else {

            int rowsAffected = getContentResolver().update(mCurrentInventoryUri, values, null, null);

            if (rowsAffected == 0) {
                Toast.makeText(this, getString(R.string.editor_update_inventory_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.editor_update_inventory_successful),
                        Toast.LENGTH_SHORT).show();
            }

        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    /**
     * This method is called after invalidateOptionsMenu(), so that the
     * menu can be updated (some menu items can be hidden or made visible).
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                saveInventory();
                finish();
                return true;
            case android.R.id.home:
                if (!mInventoryHasChanged) {
                    NavUtils.navigateUpFromSameTask(this);
                    return true;
                }

                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method is called when the back button is pressed.
     */
    @Override
    public void onBackPressed() {
        if (!mInventoryHasChanged) {
            super.onBackPressed();
            return;
        }

        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                };

        showUnsavedChangesDialog(discardButtonClickListener);
    }

    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                deleteInventory();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showIncompleteFormDialog() {
        Toast.makeText(this, getString(R.string.bad_data_entered),
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Perform the deletion of the pet in the database.
     */
    private void deleteInventory() {

        if (mCurrentInventoryUri != null) {
            int rowsDeleted = getContentResolver().delete(mCurrentInventoryUri, null, null);

            if (rowsDeleted == 0) {
                // If the new content URI is null, then there was an error with the update.
                Toast.makeText(this, getString(R.string.editor_delete_inventory_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the update was successful
                Toast.makeText(this, getString(R.string.editor_delete_inventory_successful),
                        Toast.LENGTH_SHORT).show();
            }

            finish();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mNameEditText.setText("");
        mPriceEditText.setText("");
        mSupplierSpinner.setSelection(0);
        mQuantityEditText.setText("");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        if (cursor.moveToFirst()) {
            int nameColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_NAME);
            int quantityColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_QUANTITY);
            int priceColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_PRICE);
            int supplierColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_SUPPLIER);
            int imageColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_IMAGE);

            String name = cursor.getString(nameColumnIndex);
            String quantity = cursor.getString(quantityColumnIndex);
            int price = cursor.getInt(priceColumnIndex);
            int supplier = cursor.getInt(supplierColumnIndex);
            Uri photoUri = Uri.parse(cursor.getString(imageColumnIndex));

            mNameEditText.setText(name);
            mQuantityEditText.setText(quantity);
            mPriceEditText.setText(Integer.toString(price));
            mImageEntry.setImageBitmap(getBitmapFromUri(photoUri));

            switch (supplier) {
                case InventoryEntry.OOW_ELECTRONICS:
                    mSupplierSpinner.setSelection(1);
                    break;
                case InventoryEntry.GEMCO:
                    mSupplierSpinner.setSelection(2);
                    break;
                case InventoryEntry.POKE_GO:
                    mSupplierSpinner.setSelection(3);
                    break;
                default:
                    mSupplierSpinner.setSelection(0);
                    break;
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        String[] projection = {
                InventoryEntry._ID,
                InventoryEntry.COLUMN_INVENTORY_NAME,
                InventoryEntry.COLUMN_INVENTORY_QUANTITY,
                InventoryEntry.COLUMN_INVENTORY_PRICE,
                InventoryEntry.COLUMN_INVENTORY_SUPPLIER,
                InventoryEntry.COLUMN_INVENTORY_IMAGE };

        return new CursorLoader(this,
                mCurrentInventoryUri,
                projection,
                null,
                null,
                null);
    }

}
