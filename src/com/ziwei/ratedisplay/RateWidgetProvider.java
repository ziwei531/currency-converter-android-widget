package com.ziwei.ratedisplay;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.RemoteViews;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RateWidgetProvider extends AppWidgetProvider {
	public static final String EXTRA_WIDGET_ID = "app_widget_id";
	public static final String ACTION_ROW_CLICK        = "com.ziwei.ratedisplay.ROW_CLICK";
	public static final String EXTRA_ROW_ACTION        = "rowAction";
	public static final String ROW_ACTION_EDIT         = "edit";
	public static final String ROW_ACTION_GRAPH        = "graph";
	public static final String EXTRA_BASE_CURRENCY = "baseCurrency";
	public static final String EXTRA_TARGET_CURRENCY = "targetCurrency";
	private static final String ACTION_BOOT_COMPLETED             = "android.intent.action.BOOT_COMPLETED";
	private static final String BASE_API_URL                      = "https://open.er-api.com/v6/latest/";
	private static final String FX_RATES_API_URL                  = "https://api.fxratesapi.com/latest";
	private static final long REFRESH_INTERVAL_MILLIS             = 60L * 60L * 1000L;
	private static final int BODY_LIMIT_BYTES                     = 262144;
	private static final ExecutorService REFRESH_EXECUTOR         = Executors.newSingleThreadExecutor();
	private static final Set<Integer> REFRESH_IN_PROGRESS_WIDGETS = new HashSet<>();
	private static final Set<Integer> LIVE_WIDGET_IDS             = Collections.synchronizedSet( new HashSet<>() );

	@Override
	public void onUpdate( final Context context, final AppWidgetManager manager, final int[] ids ) {
		scheduleAutoRefresh( context );
		for ( final int id : ids ) {
			LIVE_WIDGET_IDS.add( id );
			renderWidget( context, manager, id );
			fetchRatesForWidget( context, id );
		}
	}

	@Override
	public void onEnabled( final Context context ) {
		super.onEnabled( context );
		scheduleAutoRefresh( context );
	}

	@Override
	public void onDeleted( final Context context, final int[] ids ) {
		for ( final int id : ids ) {
			LIVE_WIDGET_IDS.remove( id );
			PreferencesStore.deleteWidget( context, id );
		}
		super.onDeleted( context, ids );
	}

	@Override
	public void onAppWidgetOptionsChanged(
		final Context context,
		final AppWidgetManager manager,
		final int widgetId,
		final Bundle newOptions
	) {
		super.onAppWidgetOptionsChanged( context, manager, widgetId, newOptions );
		renderWidget( context, manager, widgetId );
	}

	@Override
	public void onDisabled( final Context context ) {
		cancelAutoRefresh( context );
		super.onDisabled( context );
	}

	@Override
	public void onReceive( final Context context, final Intent intent ) {
		super.onReceive( context, intent );
		final String action = intent == null ? null : intent.getAction();
		if ( ACTION_BOOT_COMPLETED.equals( action ) ) {
			scheduleAutoRefreshIfWidgetExists( context );
		}
	}

	public static void refreshAllWidgets( final Context context ) {
		refreshAllWidgets( context, null );
	}

	public static void refreshAllWidgets( final Context context, final Runnable completion ) {
		final AppWidgetManager manager = AppWidgetManager.getInstance( context );
		final int[] ids = manager.getAppWidgetIds( new ComponentName( context, RateWidgetProvider.class ) );
		if ( ids.length == 0 ) {
			completeRefresh( completion );
			return;
		}

		final AtomicInteger remainingWidgets = new AtomicInteger( ids.length );
		for ( final int id : ids ) {
			refreshWidget( context, id, new Runnable() {
				@Override
				public void run() {
					if ( remainingWidgets.decrementAndGet() == 0 ) {
						completeRefresh( completion );
					}
				}
			} );
		}
	}

	public static void refreshWidget( final Context context, final int widgetId ) {
		refreshWidget( context, widgetId, null );
	}

	public static void refreshWidget( final Context context, final int widgetId, final Runnable completion ) {
		final AppWidgetManager manager = AppWidgetManager.getInstance( context );
		if ( !isManagedWidget( manager, context, widgetId ) ) {
			completeRefresh( completion );
			return;
		}
		LIVE_WIDGET_IDS.add( widgetId );
		fetchRatesForWidget( context, widgetId, completion );
	}

	private static void fetchRatesForWidget( final Context context, final int widgetId ) {
		fetchRatesForWidget( context, widgetId, null );
	}

	private static void fetchRatesForWidget( final Context context, final int widgetId, final Runnable completion ) {
		final Context applicationContext = context.getApplicationContext();
		final AppWidgetManager manager   = AppWidgetManager.getInstance( applicationContext );
		if ( !isManagedWidget( manager, applicationContext, widgetId ) ) {
			completeRefresh( completion );
			return;
		}
		synchronized ( REFRESH_IN_PROGRESS_WIDGETS ) {
			if ( !REFRESH_IN_PROGRESS_WIDGETS.add( widgetId ) ) {
				completeRefresh( completion );
				return;
			}
		}
		renderWidget( applicationContext, manager, widgetId );
		REFRESH_EXECUTOR.execute( new Runnable() {
			@Override
			public void run() {
				try {
					final PreferencesStore.WidgetConfiguration requested = PreferencesStore.loadConfiguration( applicationContext, widgetId );
					final String provider = PreferencesStore.getRateProvider( applicationContext );
					final String apiKey = PreferencesStore.getFxRatesApiKey( applicationContext );
					if ( PreferencesStore.PROVIDER_FX_RATES_API.equals( provider ) && apiKey == null ) {
						return;
					}
					final Map<String, String> responseBodies = new HashMap<>();
					for ( final PreferencesStore.ConversionPair pair : requested.getPairs() ) {
						if ( !responseBodies.containsKey( pair.getBaseCurrency() ) ) {
							try {
								responseBodies.put( pair.getBaseCurrency(), fetchBody( buildRequestUrl( provider, apiKey, pair.getBaseCurrency() ) ) );
							} catch ( final Exception ignored ) {
								responseBodies.put( pair.getBaseCurrency(), null );
							}
						}
					}
					final String updated = new SimpleDateFormat( "HH:mm, dd MMM", Locale.getDefault() ).format( new Date() );
					boolean didSaveRate = false;
					final PreferencesStore.WidgetConfiguration current = PreferencesStore.loadConfiguration( applicationContext, widgetId );
					final String currentProvider = PreferencesStore.getRateProvider( applicationContext );
					final String currentApiKey = PreferencesStore.getFxRatesApiKey( applicationContext );
					if ( !requested.isSameSelection( current ) || !provider.equals( currentProvider ) || !areEqual( apiKey, currentApiKey ) ) {
						return;
					}
					for ( final PreferencesStore.ConversionPair pair : requested.getPairs() ) {
						final String rate = extractRate( responseBodies.get( pair.getBaseCurrency() ), pair.getTargetCurrency() );
						if ( rate != null ) {
							PreferencesStore.saveCachedRate( applicationContext, widgetId, provider, pair.getBaseCurrency(), pair.getTargetCurrency(), rate, updated );
							didSaveRate = true;
						}
					}
					if ( didSaveRate ) {
						PreferencesStore.saveLastRefreshed( applicationContext, widgetId, updated );
					}
				} catch ( final Exception ignored ) {
				} finally {
					synchronized ( REFRESH_IN_PROGRESS_WIDGETS ) {
						REFRESH_IN_PROGRESS_WIDGETS.remove( widgetId );
					}
					if ( LIVE_WIDGET_IDS.contains( widgetId ) ) {
						renderWidget( applicationContext, AppWidgetManager.getInstance( applicationContext ), widgetId );
					}
					completeRefresh( completion );
				}
			}
		} );
	}

	private static boolean areEqual( final String first, final String second ) {
		return first == null ? second == null : first.equals( second );
	}

	private static void completeRefresh( final Runnable completion ) {
		if ( completion != null ) {
			completion.run();
		}
	}

	private static boolean isManagedWidget( final AppWidgetManager manager, final Context context, final int widgetId ) {
		final int[] widgetIds = manager.getAppWidgetIds( new ComponentName( context, RateWidgetProvider.class ) );
		for ( final int managedId : widgetIds ) {
			if ( managedId == widgetId ) {
				return true;
			}
		}
		return false;
	}

	private static String buildRequestUrl( final String provider, final String apiKey, final String baseCurrency ) throws Exception {
		if ( !PreferencesStore.PROVIDER_FX_RATES_API.equals( provider ) ) {
			return BASE_API_URL + URLEncoder.encode( baseCurrency, "UTF-8" );
		}
		return FX_RATES_API_URL
			+ "?base=" + URLEncoder.encode( baseCurrency, "UTF-8" )
			+ "&api_key=" + URLEncoder.encode( apiKey, "UTF-8" );
	}

	private static String fetchBody( final String requestUrl ) throws Exception {
		HttpURLConnection connection = null;
		try {
			connection = ( HttpURLConnection ) new URL( requestUrl ).openConnection();
			connection.setConnectTimeout( 10000 );
			connection.setReadTimeout( 10000 );
			connection.setRequestMethod( "GET" );
			connection.setRequestProperty( "Accept", "application/json" );
			final int responseCode = connection.getResponseCode();
			if ( responseCode != HttpURLConnection.HTTP_OK ) {
				throw new Exception( "HTTP " + responseCode );
			}

			final InputStream input = connection.getInputStream();
			final BufferedReader reader = new BufferedReader( new InputStreamReader( input, "UTF-8" ) );
			final StringBuilder body = new StringBuilder();
			int byteCount = 0;
			String line;
			while ( ( line = reader.readLine() ) != null ) {
				byteCount += line.length();
				if ( byteCount > BODY_LIMIT_BYTES ) {
					throw new Exception( "Response too large" );
				}
				body.append( line );
			}
			reader.close();
			return body.toString();
		} finally {
			if ( connection != null ) {
				connection.disconnect();
			}
		}
	}

	private static String extractRate( final String body, final String target ) {
		if ( body == null || target == null ) {
			return null;
		}
		final Pattern pattern = Pattern.compile(
			"\"" + Pattern.quote( target ) + "\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)"
		);
		final Matcher matcher = pattern.matcher( body );
		if ( !matcher.find() ) {
			return null;
		}
		try {
			final BigDecimal rate = new BigDecimal( matcher.group( 1 ) );
			if ( rate.signum() <= 0 || rate.scale() > 12 ) {
				return null;
			}
			return rate.toPlainString();
		} catch ( final NumberFormatException error ) {
			return null;
		}
	}

	private static void renderWidget( final Context context, final AppWidgetManager manager, final int widgetId ) {
		final PreferencesStore.WidgetConfiguration configuration = PreferencesStore.loadConfiguration( context, widgetId );
		final boolean isRefreshing = isRefreshInProgress( widgetId );
		final RemoteViews views = new RemoteViews( context.getPackageName(), R.layout.widget_rate );
		views.setTextViewText( R.id.widget_title, "Currency conversions" );
		final Intent service = new Intent( context, RateWidgetService.class );
		service.putExtra( EXTRA_WIDGET_ID, widgetId );
		service.setData( android.net.Uri.parse( service.toUri( Intent.URI_INTENT_SCHEME ) ) );
		views.setRemoteAdapter( R.id.rate_list, service );
		views.setEmptyView( R.id.rate_list, R.id.widget_empty );
		if ( isRefreshing ) {
			views.setTextViewText( R.id.widget_updated, "Refreshing…" );
		} else if ( configuration.getPairs().isEmpty() ) {
			views.setTextViewText( R.id.widget_updated, "Tap to configure conversion pairs" );
		} else {
			final String lastRefreshed = PreferencesStore.getLastRefreshed( context, widgetId );
			views.setTextViewText( R.id.widget_updated, lastRefreshed == null
				? PreferencesStore.PROVIDER_FX_RATES_API.equals( PreferencesStore.getRateProvider( context ) )
					? "fxRatesAPI feed · tap refresh to check"
					: "Rates update daily · tap refresh to check"
				: "Last refreshed: " + lastRefreshed );
		}

		final Intent configure = new Intent( context, MainActivity.class );
		configure.putExtra( MainActivity.EXTRA_WIDGET_ID, widgetId );
		configure.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP );
		final PendingIntent configurePending = PendingIntent.getActivity( context, 100000 + widgetId, configure, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE );
		views.setOnClickPendingIntent( R.id.widget_title, configurePending );
		views.setOnClickPendingIntent( R.id.widget_updated, configurePending );
		views.setOnClickPendingIntent( R.id.widget_empty, configurePending );
		final Intent rowAction = new Intent( context, WidgetRowReceiver.class );
		rowAction.setAction( ACTION_ROW_CLICK );
		rowAction.putExtra( EXTRA_WIDGET_ID, widgetId );
		final PendingIntent rowActionPending = PendingIntent.getBroadcast( context, 200000 + widgetId, rowAction, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE );
		views.setPendingIntentTemplate( R.id.rate_list, rowActionPending );
		final Intent refresh = new Intent( context, RefreshReceiver.class ).setAction( RefreshReceiver.ACTION_REFRESH );
		refresh.putExtra( MainActivity.EXTRA_WIDGET_ID, widgetId );
		final PendingIntent refreshPending = PendingIntent.getBroadcast( context, widgetId, refresh, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE );
		views.setOnClickPendingIntent( R.id.widget_refresh, refreshPending );
		manager.updateAppWidget( widgetId, views );
		manager.notifyAppWidgetViewDataChanged( widgetId, R.id.rate_list );
	}

	static boolean isRefreshInProgressForService( final int widgetId ) {
		return isRefreshInProgress( widgetId );
	}

	private static boolean isRefreshInProgress( final int widgetId ) {
		synchronized ( REFRESH_IN_PROGRESS_WIDGETS ) {
			return REFRESH_IN_PROGRESS_WIDGETS.contains( widgetId );
		}
	}



	private static String formatRate( final CurrencyCatalog.CurrencyInfo currency, final String rawRate ) {
		try {
			final int decimals = currency.getFractionDigits() <= 0 ? 2 : Math.min( 4, currency.getFractionDigits() + 1 );
			final BigDecimal value = new BigDecimal( rawRate ).setScale( decimals, RoundingMode.HALF_UP );
			return currency.getSymbol() + " " + value.toPlainString();
		} catch ( final NumberFormatException error ) {
			return currency.getSymbol() + " —";
		}
	}

	private static void scheduleAutoRefresh( final Context context ) {
		final AlarmManager alarmManager = ( AlarmManager ) context.getSystemService( Context.ALARM_SERVICE );
		if ( alarmManager == null ) {
			return;
		}
		alarmManager.setInexactRepeating(
			AlarmManager.ELAPSED_REALTIME,
			SystemClock.elapsedRealtime() + REFRESH_INTERVAL_MILLIS,
			REFRESH_INTERVAL_MILLIS,
			getAutoRefreshIntent( context )
		);
	}

	private static void scheduleAutoRefreshIfWidgetExists( final Context context ) {
		final AppWidgetManager manager = AppWidgetManager.getInstance( context );
		final int[] ids = manager.getAppWidgetIds( new ComponentName( context, RateWidgetProvider.class ) );
		if ( ids.length > 0 ) {
			scheduleAutoRefresh( context );
		}
	}

	private static void cancelAutoRefresh( final Context context ) {
		final AlarmManager alarmManager = ( AlarmManager ) context.getSystemService( Context.ALARM_SERVICE );
		if ( alarmManager != null ) {
			alarmManager.cancel( getAutoRefreshIntent( context ) );
		}
	}

	private static PendingIntent getAutoRefreshIntent( final Context context ) {
		final Intent refresh = new Intent( context, RefreshReceiver.class ).setAction( RefreshReceiver.ACTION_AUTO_REFRESH );
		return PendingIntent.getBroadcast(
			context,
			0,
			refresh,
			PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
		);
	}

}
