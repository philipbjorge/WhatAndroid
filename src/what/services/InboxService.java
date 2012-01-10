package what.services;

import what.inbox.InboxActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import api.inbox.inbox.Inbox;

public class InboxService extends Service {
	public static final String BROADCAST_ACTION = "";
	private final Handler handler = new Handler();
	private Intent intent;
	private NotificationManager myNotificationManager;
	private SharedPreferences sharedPreferences;
	public static Inbox inbox;
	public static int ID = 0;
	private static boolean isRunning = false;

	@Override
	public void onCreate() {
		super.onCreate();
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		intent = new Intent();
		setRunning(true);
	}

	@Override
	public void onStart(Intent intent, int startId) {
		handler.removeCallbacks(sendUpdatesToUI);
		handler.postDelayed(sendUpdatesToUI, 0); // initial delay
	}

	private Runnable sendUpdatesToUI = new Runnable() {
		@Override
		public void run() {
			try {
				updateStatusBar();
			} catch (Exception e) {
				e.printStackTrace();
			}
			sendBroadcast(intent);
			handler.postDelayed(this, loadRefreshRate()); // delay
		}
	};

	private void updateStatusBar() {
		new LoadInbox().execute();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		// handler.removeCallbacks(sendUpdatesToUI);
		setRunning(false);
		super.onDestroy();
	}

	private void generateNotification(CharSequence ticket, CharSequence title, CharSequence content) {
		myNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		long when = System.currentTimeMillis();
		Notification notification = new Notification(what.gui.R.drawable.icon, ticket, when);
		Context context = getApplicationContext();

		Intent notificationIntent = new Intent(this, InboxActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		notification.setLatestEventInfo(context, title, content, contentIntent);

		myNotificationManager.notify(ID, notification);
	}

	public static boolean isRunning() {
		return isRunning;
	}

	public static void setRunning(boolean isRunning) {
		InboxService.isRunning = isRunning;
	}

	private long loadRefreshRate() {
		try {
			return Long.parseLong(sharedPreferences.getString("inboxService_interval", "180")) * 60000;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 3600000;
		}
	}

	private class LoadInbox extends AsyncTask<Void, Void, Boolean> {
		@Override
		protected void onPreExecute() {
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			inbox = Inbox.inboxFromPage(1);
			return inbox.getStatus();
		}

		@Override
		protected void onPostExecute(Boolean status) {
			if (status == true) {
				generateNotification("Private Message", inbox.getResponse().getUnread() + " unread messages", "");
			}
		}
	}
}