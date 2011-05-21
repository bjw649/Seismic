package uk.co.kalgan.app.seismic;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class Seismic extends Activity {
    
	static final private int QUAKE_DIALOG = 1;
    ListView seismicListView;
	ArrayList<Quake> earthquakes = new ArrayList<Quake>();
	ArrayAdapter<Quake> aa;
	Quake selectedQuake;
	SeismicReceiver receiver;
	NotificationManager notificationManager;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        seismicListView = (ListView)this.findViewById(R.id.seismicListView);
        
        seismicListView.setOnItemClickListener(new OnItemClickListener() {
        	@Override
        	public void onItemClick(AdapterView<?> _av, View _v, int _index, long arg3) {
        		selectedQuake = earthquakes.get(_index);
        		showDialog(QUAKE_DIALOG);
        	}
        });
        
        int layoutID = android.R.layout.simple_list_item_1;
        aa = new ArrayAdapter<Quake>(this, layoutID, earthquakes);
        seismicListView.setAdapter(aa);
        
        loadQuakesfromProvider();
        
        updateFromPreferences();
        refreshEarthquakes();
        
        String svcName = Context.NOTIFICATION_SERVICE;
        notificationManager = (NotificationManager)getSystemService(svcName);
    }
	
	@Override
	public void onResume() {
		notificationManager.cancel(SeismicService.NOTIFICATION_ID);
		
		IntentFilter filter;
		filter = new IntentFilter(SeismicService.NEW_EARTHQUAKE_FOUND);
		receiver = new SeismicReceiver();
		registerReceiver(receiver, filter);
		
		loadQuakesfromProvider();
		super.onResume();
	}
	
	@Override
	public void onPause() {
		unregisterReceiver(receiver);
		super.onPause();
	}
	
	static final private int MENU_UPDATE = Menu.FIRST;
	static final private int MENU_PREFERENCES = Menu.FIRST+1;
	static final private int MENU_EARTHQUAKE_MAP = Menu.FIRST+2;
		
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		menu.add(0, MENU_UPDATE, Menu.NONE, R.string.menu_update);
		menu.add(0, MENU_PREFERENCES, Menu.NONE, R.string.menu_preferences);
		
		Intent startMap = new Intent(this, SeismicMap.class);
		menu.add(0, MENU_EARTHQUAKE_MAP, Menu.NONE,
				R.string.menu_earthquake_map).setIntent(startMap);
				
		return true;
	}
	
	private static final int SHOW_PREFERENCES = 1;
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		
		switch (item.getItemId()) {
		case (MENU_UPDATE): {
			updateFromPreferences();
			refreshEarthquakes();
			return true;
		}
		case (MENU_PREFERENCES): {
			Intent i = new Intent(this, UserPreferences.class);
			startActivityForResult(i, SHOW_PREFERENCES);
			return true;
		}
		}
		return false;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == SHOW_PREFERENCES) {
			if (resultCode == Activity.RESULT_OK) {
				updateFromPreferences();
				refreshEarthquakes();
			}
		}
	}
	
	@Override
	public Dialog onCreateDialog(int id) {
		switch(id) {
		case (QUAKE_DIALOG): {
			LayoutInflater li = LayoutInflater.from(this);
			View quakeDetailsView = li.inflate(R.layout.quake_details, null);
			
			AlertDialog.Builder quakeDialog = new AlertDialog.Builder(this);
			quakeDialog.setTitle("Quake Time");
			quakeDialog.setView(quakeDetailsView);
			return quakeDialog.create();
		}
		}
		return null;
	}
	
	@Override
	public void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case (QUAKE_DIALOG): {
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
			String dateString = sdf.format(selectedQuake.getDate());
			String quakeText = "Magnitude " +selectedQuake.getMagnitude() +
								"\n" + selectedQuake.getDetails() + "\n" +
								selectedQuake.getLink();
			
			AlertDialog quakeDialog = (AlertDialog)dialog;
			quakeDialog.setTitle(dateString);
			TextView tv = (TextView)quakeDialog.findViewById(R.id.quakeDetailsTextView);
			tv.setText(quakeText);
			
			break;
		}
		}
	}
	
	private void refreshEarthquakes() {
		startService(new Intent(this, SeismicService.class));
	}
	
	private void addQuakeToArray(Quake _quake) {
		if (_quake.getMagnitude() >= minimumMagnitude) {
			earthquakes.add(_quake);
			aa.notifyDataSetChanged();
		}
	}
	
	private void loadQuakesfromProvider() {
		earthquakes.clear();
		
		ContentResolver cr = getContentResolver();
		
		// Return all saved quakes
		Cursor c = cr.query(SeismicProvider.CONTENT_URI, null, null, null, null);
		if (c.moveToFirst()) {
			do {
				// Extract details
				Long datems = c.getLong(SeismicProvider.DATE_COLUMN);
				String details = c.getString(SeismicProvider.DETAILS_COLUMN);
				Float lat = c.getFloat(SeismicProvider.LATITUDE_COLUMN);
				Float lon = c.getFloat(SeismicProvider.LONGITUDE_COLUMN);
				Double mag = c.getDouble(SeismicProvider.MAGNITUDE_COLUMN);
				String link = c.getString(SeismicProvider.LINK_COLUMN);
				
				Location location = new Location("dummy");
				location.setLatitude(lat);
				location.setLongitude(lon);
				
				Date date = new Date(datems);
				
				Quake q = new Quake(date, details, location, mag, link);
				addQuakeToArray(q);
			} while (c.moveToNext());
		}
	}
	
	int minimumMagnitude = 0;
	boolean autoUpdate = false;
	int updateFreq = 0;
	
	private void updateFromPreferences() {
		Context context = getApplicationContext();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		minimumMagnitude = Integer.parseInt(prefs.getString(UserPreferences.PREF_MIN_MAG, "0"));
				
		updateFreq = Integer.parseInt(prefs.getString(UserPreferences.PREF_UPDATE_FREQ, "0"));
		
		autoUpdate = prefs.getBoolean(UserPreferences.PREF_AUTO_UPDATE, false);
	}
	
	public class SeismicReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context _context, Intent _intent) {
			loadQuakesfromProvider();
			
			notificationManager.cancel(SeismicService.NOTIFICATION_ID);
		}
	}
}