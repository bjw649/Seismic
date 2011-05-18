package uk.co.kalgan.app.seismic;

import java.util.ArrayList;

import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public class SeismicOverlay extends Overlay {
	
	Cursor earthquakes;
	
	public SeismicOverlay(Cursor _cursor) {
		super();
		earthquakes = _cursor;
		
		quakeLocations = new ArrayList<GeoPoint>();
		refreshQuakeLocations();
		earthquakes.registerDataSetObserver(new DataSetObserver() {
			@Override
			public void onChanged() {
				refreshQuakeLocations();
			}
		});
	}
	
	int rad = 5;
	
	@Override
	public void draw(Canvas _canvas, MapView _mapView, boolean _shadow) {
		Projection projection = _mapView.getProjection();
		
		// Setup paint brush
		Paint paint = new Paint();
		paint.setARGB(250, 255, 0, 0);
		paint.setAntiAlias(true);
		paint.setFakeBoldText(true);
		
		if (_shadow == false) {
			for (GeoPoint point : quakeLocations) {
				Point myPoint = new Point();
				projection.toPixels(point, myPoint);
				
				RectF oval = new RectF(myPoint.x-rad, myPoint.y-rad,
									   myPoint.x+rad, myPoint.y+rad);
				_canvas.drawOval(oval, paint);
			}
		}
	}
	
	ArrayList<GeoPoint> quakeLocations;
	
	private void refreshQuakeLocations() {
		if (earthquakes.moveToFirst()) {
			do {
				Double lat = earthquakes.getFloat(SeismicProvider.LATITUDE_COLUMN)*1E6;
				Double lon = earthquakes.getFloat(SeismicProvider.LONGITUDE_COLUMN)*1E6;
				
				GeoPoint geoPoint = new GeoPoint(lat.intValue(), lon.intValue());
				quakeLocations.add(geoPoint);
			} while(earthquakes.moveToNext());
		}
	}
}
