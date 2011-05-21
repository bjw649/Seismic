package uk.co.kalgan.app.seismic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SeismicAlarmReceiver extends BroadcastReceiver {

	public static final String ACTION_REFRESH_EARTHQUAKE_ALARM = 
		"uk.co.kalgan.seismic.ACTION_REFRESH_EARTHQUAKE_ALARM";
	
	@Override
	public void onReceive(Context _context, Intent _intent) {
		Intent startIntent = new Intent(_context, SeismicService.class);
		_context.startActivity(startIntent);
	}

}
