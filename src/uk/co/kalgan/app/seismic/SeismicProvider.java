package uk.co.kalgan.app.seismic;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class SeismicProvider extends ContentProvider {

	public static final Uri CONTENT_URI = Uri.parse("content://uk.co.kalgan.provider.seismic/earthquakes");
	
	// Create constants to differentiate between different URI requests
	private static final int QUAKES = 1;
	private static final int QUAKE_ID = 2;
	
	private static final UriMatcher uriMatcher;
	// Allocate UriMatcher with trailing 'earthquakes' for all quakes and
	// a trailing 'earthquakes/[rowID]' for just one 
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI("uk.co.kalgan.provider.Seismic", "earthquakes", QUAKES);
		uriMatcher.addURI("uk.co.kalgan.provider.Seismic", "earthquakes/#", QUAKE_ID);
	}
	
	private SQLiteDatabase earthquakeDB;
	
	private static final String TAG = "SeismicProvider";
	private static final String DATABASE_NAME = "earthquakes.db";
	private static final int DATABASE_VERSION = 1;
	private static final String EARTHQUAKE_TABLE = "earthquakes";
	
	// Column Names
	public static final String KEY_ID = "_id";
	public static final String KEY_DATE = "date";
	public static final String KEY_DETAILS = "details";
	public static final String KEY_LOCATION_LAT = "latitude";
	public static final String KEY_LOCATION_LON	= "longitude";
	public static final String KEY_MAGNITUDE = "magnitude";
	public static final String KEY_LINK = "link";
	
	// Column indexes
	public static final int DATE_COLUMN = 1;
	public static final int DETAILS_COLUMN = 2;
	public static final int LONGITUDE_COLUMN = 3;
	public static final int LATITUDE_COLUMN = 4;
	public static final int MAGNITUDE_COLUMN = 5;
	public static final int LINK_COLUMN = 6;
	
	// Helper class for database open, creation and updating
	private static class seismicDatabaseHelper extends SQLiteOpenHelper {
		private static final String DATABASE_CREATE =
			"create table " + EARTHQUAKE_TABLE + " ("
			+ KEY_ID + " integer primary key autoincrement, "
			+ KEY_DATE + " INTEGER, "
			+ KEY_DETAILS + " TEXT, "
			+ KEY_LOCATION_LAT + " FLOAT, "
			+ KEY_LOCATION_LON + " FLOAT, "
			+ KEY_MAGNITUDE + " FLOAT, "
			+ KEY_LINK + " TEXT);";
		
		public seismicDatabaseHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, name, factory, version);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading Database from version " + oldVersion +
					" to version " + newVersion + ", will destroy old data!");
			db.execSQL("DROP TABLE IF EXISTS " + EARTHQUAKE_TABLE);
			onCreate(db);
		}
	}
	
	@Override
	public boolean onCreate() {
		Context context = getContext();
		
		seismicDatabaseHelper dbHelper = new seismicDatabaseHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
		earthquakeDB = dbHelper.getWritableDatabase();
		
		return (earthquakeDB == null) ? false : true;
	}
	
	@Override
	public String getType(Uri _uri) {
		switch ( uriMatcher.match(_uri)) {
		case QUAKES: return "vnd.android.cursor.dir/vnd.kalgan.earthquake";
		case QUAKE_ID: return "vnd.android.cursor.item/vnd.kalgan.earthquake";
		default: throw new IllegalArgumentException("Unsupported URI: " + _uri);
		}
	}

	@Override
	public Uri insert(Uri _uri, ContentValues _values) {
		// Insert the new row and return its row number if
		long rowID = earthquakeDB.insert(EARTHQUAKE_TABLE, "quake", _values);
		
		// return URI to the newly inserted row on success
		if (rowID > 0) {
			Uri uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
			getContext().getContentResolver().notifyChange(uri, null);
			return uri;
		}
		throw new SQLiteException("Failed to insert row into " + _uri);
	}

	@Override
	public int delete(Uri _uri, String _where, String[] _whereArgs) {
		int count;
		
		switch (uriMatcher.match(_uri)) {
		case QUAKES: {
			count = earthquakeDB.delete(EARTHQUAKE_TABLE, _where, _whereArgs);
			break;
		}
		case QUAKE_ID: {
			String segment = _uri.getPathSegments().get(1);
			count = earthquakeDB.delete(EARTHQUAKE_TABLE,
					KEY_ID + "=" + segment
					+ (!TextUtils.isEmpty(_where) ? "AND (" + _where + ')' : ""),
					_whereArgs);
			break;
		}
		default: throw new IllegalArgumentException("Unsupported URI: " + _uri);
		}
		getContext().getContentResolver().notifyChange(_uri, null);
		return count;
	}

	
	@Override
	public Cursor query(Uri _uri, String[] _projection, String _selection,
			String[] _selectionArgs, String _sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		
		qb.setTables(EARTHQUAKE_TABLE);
		
		// If it is a row query, limit the results to that row
		switch (uriMatcher.match(_uri)) {
		case QUAKE_ID: {
			qb.appendWhere(KEY_ID + "=" + _uri.getPathSegments().get(1));
			break;
		}
		default:break;
		}
		
		// If no sort order
		String orderBy;
		if (TextUtils.isEmpty(_sortOrder)) {
			orderBy = KEY_DATE;
		} else {
			orderBy = _sortOrder;
		}
		
		// Apply query to the database
		Cursor c =qb.query(earthquakeDB, _projection, 
				_selection, _selectionArgs, null, null, orderBy);
		
		// Register the contexts ContentResolver to be notified
		// if the cursor result set changes
		c.setNotificationUri(getContext().getContentResolver(), _uri);
		
		return null;
	}

	@Override
	public int update(Uri _uri, ContentValues _values, String _where,
			String[] _whereArgs) {
		int count;
		switch (uriMatcher.match(_uri)) {
		case QUAKES: {
			count = earthquakeDB.update(EARTHQUAKE_TABLE, _values, _where, _whereArgs);
			break;
		}
		case QUAKE_ID: {
			String segment = _uri.getPathSegments().get(1);
			count = earthquakeDB.update(EARTHQUAKE_TABLE, _values,
					KEY_ID + "=" + segment
					+ (!TextUtils.isEmpty(_where) ? "AND (" + _where + ')' : ""),
					_whereArgs);
		}
		default: throw new IllegalArgumentException("Unsupported URI: " + _uri);
		}
		
		getContext().getContentResolver().notifyChange(_uri, null);
		return count;
	}

}
