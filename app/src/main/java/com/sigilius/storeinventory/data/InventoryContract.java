package com.sigilius.storeinventory.data;


import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public final class InventoryContract {

    private InventoryContract() {}

    /**
     * The "Content authority" is a name for the entire content provider, similar to the
     * relationship between a domain name and its website.  A convenient string to use for the
     * content authority is the package name for the app, which is guaranteed to be unique on the
     * device.
     */
    public static final String CONTENT_AUTHORITY = "com.sigilius.storeinventory";

    /**
     * Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
     * the content provider.
     */
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    /**
     * Possible path (appended to base content URI for possible URI's)
     * For instance, content://com.example.android.pets/pets/ is a valid path for
     * looking at pet data. content://com.example.android.pets/staff/ will fail,
     * as the ContentProvider hasn't been given any information on what to do with "staff".
     */
    public static final String PATH_INVENTORY = "inventory";

    public static final class InventoryEntry implements BaseColumns {

        /**
         * The content URI to access the inventory data in the provider
         */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_INVENTORY);

        /**
         * The MIME type of the {@link #CONTENT_URI} for a list of inventory.
         */
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_INVENTORY;

        /**
         * The MIME type of the {@link #CONTENT_URI} for a single item.
         */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_INVENTORY;

        public static final String TABLE_NAME = "inventory";
        public static final String _ID = BaseColumns._ID;
        public static final String COLUMN_INVENTORY_NAME = "name";
        public static final String COLUMN_INVENTORY_QUANTITY = "quantity";
        public static final String COLUMN_INVENTORY_PRICE = "price";
        public static final String COLUMN_INVENTORY_SUPPLIER = "supplier";
        public static final String COLUMN_INVENTORY_EMAIL = "email";
        public static final String COLUMN_INVENTORY_IMAGE = "image";

        /**
         * Possible values for the inventory supplier
         */
        public static final int ACME = 0;
        public static final String ACME_EMAIL = "orders@acme.com";
        public static final int OOW_ELECTRONICS = 1;
        public static final String OOW_EMAIL = "orders@oowelect.com";
        public static final int GEMCO = 2;
        public static final String GEMCO_EMAIL = "orders@gemco.com";
        public static final int POKE_GO = 3;
        public static final String POKE_GO_EMAIL = "orders@pokego.org";

        /**
         * Returns whether or not the given supplier is {@link #ACME}, {@link #OOW_ELECTRONICS},
         * or {@link #GEMCO} or {@link #POKE_GO}.
         */
        public static boolean isValidSupplier(int supplier) {
            if (supplier == ACME || supplier == OOW_ELECTRONICS || supplier == GEMCO || supplier == POKE_GO) {
                return true;
            }
            return false;
        }
    }
}
