package com.ziwei.ratedisplay;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class RefreshReceiver extends BroadcastReceiver {
	public static final String ACTION_REFRESH = "com.ziwei.ratedisplay.REFRESH";
	public static final String ACTION_AUTO_REFRESH = "com.ziwei.ratedisplay.AUTO_REFRESH";

	@Override
	public void onReceive( final Context context, final Intent intent ) {
		final PendingResult pendingResult        = goAsync();
		final Runnable      finishPendingResult  = new Runnable() {
			@Override
			public void run() {
				pendingResult.finish();
			}
		};
		final String  action             = intent == null ? null : intent.getAction();
		final Context applicationContext = context.getApplicationContext();
		if ( ACTION_AUTO_REFRESH.equals( action ) ) {
			RateWidgetProvider.refreshAllWidgets( applicationContext, finishPendingResult );
			return;
		}
		if ( ACTION_REFRESH.equals( action ) ) {
			final int widgetId = intent.getIntExtra( MainActivity.EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID );
			if ( widgetId != AppWidgetManager.INVALID_APPWIDGET_ID ) {
				RateWidgetProvider.refreshWidget( applicationContext, widgetId, finishPendingResult );
				return;
			}
		}
		finishPendingResult.run();
	}
}
