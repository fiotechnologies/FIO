package com.fio.hospitalfinder;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DbAdapter {

    private static final String DB_PATH = "/data/data/com.fio.hospitalfinder/databases/";
    private static final String DB_NAME = "HospitalfinderlocationDatabase";
    private static final String TABLE_ITEMS = "hospital_finderdetails";
    public static final String HOSPITAL_ID = "_id";
    public static final String HOSPITAL_NAME = "hospital_name";
    public static final String HOSPITAL_SPECIALIST = "hospital_specialist";
    public static final String HOSPITAL_ADDRESS = "hospital_address";
    public static final String HOSPITAL_PHONE2 = "hospital_phone2";
    public static final String LOCATION = "location";
    public static final String ZIPCODE = "zipcode";
    private static final int DB_VERSION = 1;
    private final Context mCtx;
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        private Context ctx = null;

        DatabaseHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
            this.ctx = context;
            createDatabase();
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }

        private void createDatabase() {
            boolean dbExists = isDatabaseAlreadyAvailable();
            if (!dbExists) {
                this.getWritableDatabase();
                copyDatabase();
            }
        }

        private boolean isDatabaseAlreadyAvailable() {
            SQLiteDatabase db;
            try {
                db = SQLiteDatabase.openDatabase(DB_PATH + DB_NAME, null, SQLiteDatabase.OPEN_READONLY);
            } catch (Exception e) {
                db = null;
            }
            if (db != null) {
                db.close();
                return true;
            } else {
                return false;
            }
        }

        private void copyDatabase() {
            InputStream inputStream = null;;
            String outFileName = null;;
            OutputStream outStream = null;
            try {
                inputStream = ctx.getAssets().open(DB_NAME);
                outFileName = DB_PATH + DB_NAME;
                outStream = new FileOutputStream(outFileName);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputStream.read(buffer)) > 0) {
                    outStream.write(buffer, 0, len);
                }
            } catch (IOException ex) {
                Logger.getLogger(DbAdapter.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    outStream.flush();
                    outStream.close();
                    inputStream.close();
                } catch (IOException ex) {
                    Logger.getLogger(DbAdapter.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     *
     * @param ctx the Context within which to work
     */
    public DbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    public DbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getReadableDatabase();
        return this;
    }

    public void close() {
        mDbHelper.close();
    }

    public Cursor fetchListItems(String zipCode,String locality,String specialist) {
        String where = null;
        String[] whereArgs = null;
        if(zipCode != null && !zipCode.trim().equals("") 
                && locality != null &&  !locality.trim().equals("")){
            where = ZIPCODE+"=? or upper("+LOCATION+") like upper(?)";
            whereArgs = new String[]{zipCode,"%"+locality+"%"};
        }else  if(zipCode != null && !zipCode.trim().equals("") 
                && specialist != null &&  !specialist.trim().equals("")){
            where = ZIPCODE+"=? and upper("+HOSPITAL_SPECIALIST+") like upper(?)";
            whereArgs = new String[]{zipCode,"%"+specialist+"%"};
        }else  if(locality != null && !locality.trim().equals("") 
                && specialist != null &&  !specialist.trim().equals("")){
            where = "upper("+LOCATION+") like upper(?) and upper("+HOSPITAL_SPECIALIST+") like upper(?)";
            whereArgs = new String[]{"%"+locality+"%","%"+specialist+"%"};
        }else if(zipCode != null && !zipCode.trim().equals("")){
            where = ZIPCODE+"=?";
            whereArgs = new String[]{zipCode};
         
        }else if(locality != null && !locality.trim().equals("")){
            where = "upper("+LOCATION+") like upper(?)";
            whereArgs = new String[]{"%"+locality+"%"};
        
        }else if(specialist != null && !specialist.trim().equals("")){
            where = "upper("+HOSPITAL_SPECIALIST+") like upper(?)";
            whereArgs = new String[]{"%"+specialist+"%"};
        
        
        }else{
            where = ZIPCODE+"=?";
            whereArgs = new String[]{""};
        }
        Cursor cursor =
                mDb.query(TABLE_ITEMS, new String[]{HOSPITAL_ID, HOSPITAL_NAME,HOSPITAL_SPECIALIST, HOSPITAL_ADDRESS, HOSPITAL_PHONE2, LOCATION, ZIPCODE},
                where, whereArgs, null, null, HOSPITAL_NAME+" asc");

        if (cursor != null) {
            cursor.moveToFirst();            
        }
        return cursor;
    }
}

