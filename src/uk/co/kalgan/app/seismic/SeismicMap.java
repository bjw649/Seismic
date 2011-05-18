package uk.co.kalgan.app.seismic;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;

public class SeismicMap extends MapActivity {
	
	Cursor earthquakeCursor;

	@Override
	public void onCreate(Bundle savedInstanceState) { 
		super.onCreate(savedInstanceState);
		setContentView(R.layout.earthquake_map);
		
		Uri earthquakeURI = SeismicProvider.CONTENT_URI;
		earthquakeCursor = getContentResolver().query(earthquakeURI, null, null, null, null);
		
		MapView earthquakeMap = (MapView)findViewById(R.id.map_view);
		SeismicOverlay so = new SeismicOverlay(earthquakeCursor);
		earthquakeMap.getOverlays().add(so);
	}
	
	@Override
	public void onResume() {
		earthquakeCursor.requery();
		super.onResume();
	}
	
	@Override
	public void onPause() {
		earthquakeCursor.deactivate();
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

}
