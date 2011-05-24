package uk.co.kalgan.app.seismic;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.RemoteViews;

public class SeismicWidget extends AppWidgetProvider {
	
	@Override
	public void onUpdate(Context _context,
						 AppWidgetManager _appWidgetManager,
						 int[] _appWidgetIds) {
		updateQuake(_context, _appWidgetManager, _appWidgetIds);
	}
	
	@Override
	public void onReceive(Context _context, Intent _intent) {
		super.onReceive(_context, _intent);
		
		if (_intent.getAction().equals(SeismicService.QUAKES_REFRESHED))
			updateQuake(_context);
	}
	
	public void updateQuake(Context _context) {
		ComponentName thisWidget = new ComponentName(_context, SeismicWidget.class);
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(_context);
		int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
		
		updateQuake(_context, appWidgetManager, appWidgetIds);
	}
	
	public void updateQuake(Context _context,
							AppWidgetManager _appWidgetManager,
							int[] _appWidgetIds) {
		Cursor lastEarthquake;
		ContentResolver cr = _context.getContentResolver();
		lastEarthquake = cr.query(SeismicProvider.CONTENT_URI, null, null, null, null);
		
		String magnitude = "--";
		String details = "-- None --";
		
		if (lastEarthquake != null) {
			try {
				if (lastEarthquake.moveToFirst()) {
					magnitude = lastEarthquake.getString(SeismicProvider.MAGNITUDE_COLUMN);
					details = lastEarthquake.getString(SeismicProvider.DETAILS_COLUMN);
				}
			} finally {
				lastEarthquake.close();
			}
		}
		
		final int N = _appWidgetIds.length;
		for(int i =0; i < N; i++) {
			int appWidgetsId = _appWidgetIds[i];
			RemoteViews views = new RemoteViews(_context.getPackageName(), R.layout.seismic_widget);
			views.setTextViewText(R.id.widget_magnatude, magnitude);
			views.setTextViewText(R.id.widget_details, details);
			_appWidgetManager.updateAppWidget(appWidgetsId, views);
		}
	}
}
