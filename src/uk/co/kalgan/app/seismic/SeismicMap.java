package uk.co.kalgan.app.seismic;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;

public class SeismicMap extends MapActivity {
	
	Cursor earthquakeCursor;
	SeismicReceiver receiver;
	NotificationManager notificationManager;

	@Override
	public void onCreate(Bundle savedInstanceState) { 
		super.onCreate(savedInstanceState);
		setContentView(R.layout.earthquake_map);
		
		Uri earthquakeURI = SeismicProvider.CONTENT_URI;
		earthquakeCursor = getContentResolver().query(earthquakeURI, null, null, null, null);
		
		MapView earthquakeMap = (MapView)findViewById(R.id.map_view);
		SeismicOverlay so = new SeismicOverlay(earthquakeCursor);
		earthquakeMap.getOverlays().add(so);
		
		String svcName = Context.NOTIFICATION_SERVICE;
		notificationManager = (NotificationManager)getSystemService(svcName);
	}
	
	@Override
	public void onResume() {
		notificationManager.cancel(SeismicService.NOTIFICATION_ID);
		
		earthquakeCursor.requery();
		
		IntentFilter filter;
		filter = new IntentFilter(SeismicService.NEW_EARTHQUAKE_FOUND);
		receiver = new SeismicReceiver();
		registerReceiver(receiver, filter);
				
		super.onResume();
	}
	
	@Override
	public void onPause() {
		earthquakeCursor.deactivate();
		unregisterReceiver(receiver);
		super.onPause();
	}
	
	@Override
	public void onDestroy() {
		earthquakeCursor.close();
		super.onDestroy();
	}
	
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	public class SeismicReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context _context, Intent _intent) {
			notificationManager.cancel(SeismicService.NOTIFICATION_ID);
			
			earthquakeCursor.requery();
			MapView earthquakeMap = (MapView)findViewById(R.id.map_view);
			earthquakeMap.invalidate();
		}
	}
}
