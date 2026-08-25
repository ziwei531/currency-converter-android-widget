package com.ziwei.ratedisplay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.appwidget.AppWidgetManager;
import android.net.Uri;

public class WidgetRowReceiver extends BroadcastReceiver {
	@Override
	public void onReceive( final Context context, final Intent intent ) {
		final String action = intent.getAction();
		final String rowAction = intent.getStringExtra( RateWidgetProvider.EXTRA_ROW_ACTION );
		if ( RateWidgetProvider.ACTION_ROW_CLICK.equals( action ) && RateWidgetProvider.ROW_ACTION_GRAPH.equals( rowAction ) ) {
			openGoogleGraph( context, intent.getStringExtra( RateWidgetProvider.EXTRA_BASE_CURRENCY ), intent.getStringExtra( RateWidgetProvider.EXTRA_TARGET_CURRENCY ) );
			return;
		}
		if ( RateWidgetProvider.ACTION_ROW_CLICK.equals( action ) && RateWidgetProvider.ROW_ACTION_EDIT.equals( rowAction ) ) {
			final Intent edit = new Intent( context, MainActivity.class );
			edit.putExtra( MainActivity.EXTRA_WIDGET_ID, intent.getIntExtra( RateWidgetProvider.EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID ) );
			edit.putExtra( MainActivity.EXTRA_PAIR_INDEX, intent.getIntExtra( MainActivity.EXTRA_PAIR_INDEX, -1 ) );
			edit.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP );
			context.startActivity( edit );
		}
	}

	private static void openGoogleGraph( final Context context, final String baseCurrency, final String targetCurrency ) {
		if ( baseCurrency == null || targetCurrency == null ) {
			return;
		}
		final String query = Uri.encode( baseCurrency + " " + targetCurrency );
		final Intent browser = new Intent( Intent.ACTION_VIEW, Uri.parse( "https://www.google.com/search?q=" + query ) );
		browser.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
		context.startActivity( browser );
	}
}
