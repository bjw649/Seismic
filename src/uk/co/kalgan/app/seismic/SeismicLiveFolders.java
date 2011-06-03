package uk.co.kalgan.app.seismic;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.LiveFolders;

public class SeismicLiveFolders extends Activity {
	public static class SeismicLiveFolder extends Activity {
		private static Intent createLiveFolderIntent(Context _context) {
			Intent intent = new Intent();
			intent.setData(SeismicProvider.LIVE_FOLDER_URI);
			intent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_BASE_INTENT,
					new Intent(Intent.ACTION_VIEW,
							SeismicProvider.CONTENT_URI));
			intent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_DISPLAY_MODE,
					LiveFolders.DISPLAY_MODE_LIST);
			intent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_ICON,
					Intent.ShortcutIconResource.fromContext(_context, R.drawable.icon));
			intent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_NAME, "Earthquakes");
			return intent;
		}
		
		@Override
		public void onCreate(Bundle _savedInstanceState) {
			super.onCreate(_savedInstanceState);
			
			String action = getIntent().getAction();
			if (LiveFolders.ACTION_CREATE_LIVE_FOLDER.equals(action)) 
				setResult(RESULT_OK, createLiveFolderIntent(this));
			else
				setResult(RESULT_CANCELED);
			finish();
		}
	}

}
