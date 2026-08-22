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
import android.view.View;
import android.widget.RemoteViews;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RateWidgetProvider extends AppWidgetProvider {
	private static final String ACTION_BOOT_COMPLETED             = "android.intent.action.BOOT_COMPLETED";
	private static final String BASE_API_URL                      = "https://open.er-api.com/v6/latest/";
	private static final long REFRESH_INTERVAL_MILLIS             = 30L * 60L * 1000L;
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
					final String requestUrl                              = BASE_API_URL + requested.getBaseCurrency();
					final String body                                    = fetchBody( requestUrl );
					final String updated                                 = new SimpleDateFormat( "HH:mm, dd MMM", Locale.getDefault() ).format( new Date() );
					final String[] rates                                 = new String[ PreferencesStore.MAX_TARGETS ];
					for ( int index = 0; index < PreferencesStore.MAX_TARGETS; index++ ) {
						final String target = requested.getTarget( index );
						rates[ index ] = target.length() == 0 ? "" : extractRate( body, target );
					}

					final PreferencesStore.WidgetConfiguration current = PreferencesStore.loadConfiguration( applicationContext, widgetId );
					if ( !requested.isSameSelection( current ) ) {
						return;
					}

					for ( int index = 0; index < PreferencesStore.MAX_TARGETS; index++ ) {
						final String target = requested.getTarget( index );
						if ( target.length() > 0 && rates[ index ] != null ) {
							PreferencesStore.saveCachedRate(
								applicationContext,
								widgetId,
								requested.getBaseCurrency(),
								target,
								rates[ index ],
								updated
							);
						}
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
		final PreferencesStore.WidgetConfiguration configuration   = PreferencesStore.loadConfiguration( context, widgetId );
		final LayoutSelection                         selection    = selectLayout( manager, widgetId, configuration );
		final boolean                                 isRefreshing = isRefreshInProgress( widgetId );
		final RemoteViews                              views       = new RemoteViews( context.getPackageName(), selection.layoutResource );
		views.setTextViewText( R.id.widget_title, "1 " + CurrencyCatalog.find( configuration.getBaseCurrency() ).getName() );
		views.setTextViewText( R.id.widget_updated, "Tap a row to configure" );

		final int[] rowResources = {
			R.id.rate_row_0, R.id.rate_row_1, R.id.rate_row_2, R.id.rate_row_3, R.id.rate_row_4
		};
		final int[] codeResources = {
			R.id.target_code_0, R.id.target_code_1, R.id.target_code_2, R.id.target_code_3, R.id.target_code_4
		};
		final int[] valueResources = {
			R.id.target_value_0, R.id.target_value_1, R.id.target_value_2, R.id.target_value_3, R.id.target_value_4
		};

		String latestUpdated = null;
		int visibleRows      = 0;
		int hiddenRows       = 0;
		for ( int index = 0; index < PreferencesStore.MAX_TARGETS; index++ ) {
			final String  target     = configuration.getTarget( index );
			final boolean hasTarget  = target.length() > 0;
			final boolean visible    = hasTarget && visibleRows < selection.maxRows;
			views.setViewVisibility( rowResources[ index ], visible ? View.VISIBLE : View.GONE );
			if ( visible ) {
				final String cached        = PreferencesStore.getCachedRate( context, widgetId, target );
				final String cachedUpdated = PreferencesStore.getCachedUpdated( context, widgetId, target );
				final int    separator     = cached == null ? -1 : cached.indexOf( "|" );
				final String cachedBase    = separator > 0 ? cached.substring( 0, separator ) : null;
				final String rawRate       = cachedBase == null || !configuration.getBaseCurrency().equals( cachedBase )
					? null
					: cached.substring( separator + 1 );
				final CurrencyCatalog.CurrencyInfo currency = CurrencyCatalog.find( target );
				views.setTextViewText( codeResources[ index ], currency.getName() );
				views.setTextViewText( valueResources[ index ], isRefreshing ? "…" : rawRate == null ? "—" : formatRate( currency, rawRate ) );
				if ( rawRate != null && cachedUpdated != null ) {
					latestUpdated = cachedUpdated;
				}
				visibleRows++;
			} else if ( hasTarget ) {
				hiddenRows++;
			}
		}
		if ( isRefreshing ) {
			views.setTextViewText( R.id.widget_updated, "Refreshing…" );
		} else if ( visibleRows > 0 && latestUpdated != null ) {
			final String overflow = hiddenRows > 0 ? " · +" + hiddenRows + " more" : "";
			views.setTextViewText( R.id.widget_updated, "Updated " + latestUpdated + overflow + " · mid-market" );
		} else if ( hiddenRows > 0 ) {
			views.setTextViewText( R.id.widget_updated, "+" + hiddenRows + " more · tap to configure" );
		}

		final Intent configure = new Intent( context, MainActivity.class );
		configure.putExtra( MainActivity.EXTRA_WIDGET_ID, widgetId );
		configure.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP );
		final PendingIntent configurePending = PendingIntent.getActivity(
			context,
			100000 + widgetId,
			configure,
			PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
		);
		views.setOnClickPendingIntent( R.id.widget_root, configurePending );
		views.setOnClickPendingIntent( R.id.widget_config, configurePending );

		final Intent refresh = new Intent( context, RefreshReceiver.class ).setAction( RefreshReceiver.ACTION_REFRESH );
		refresh.putExtra( MainActivity.EXTRA_WIDGET_ID, widgetId );
		final PendingIntent refreshPending = PendingIntent.getBroadcast(
			context,
			widgetId,
			refresh,
			PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
		);
		views.setOnClickPendingIntent( R.id.widget_refresh, refreshPending );
		manager.updateAppWidget( widgetId, views );
	}

	private static boolean isRefreshInProgress( final int widgetId ) {
		synchronized ( REFRESH_IN_PROGRESS_WIDGETS ) {
			return REFRESH_IN_PROGRESS_WIDGETS.contains( widgetId );
		}
	}

	private static LayoutSelection selectLayout(
		final AppWidgetManager manager,
		final int widgetId,
		final PreferencesStore.WidgetConfiguration configuration
	) {
		final Bundle options = manager.getAppWidgetOptions( widgetId );
		final int minimumHeight = options == null
			? 0
			: options.getInt( AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0 );
		int targetCount = 0;
		for ( final String target : configuration.getTargets() ) {
			if ( target.length() > 0 ) {
				targetCount++;
			}
		}

		if ( targetCount == 1 ) {
			return new LayoutSelection( R.layout.widget_rate_single, 1 );
		}
		if ( minimumHeight > 0 && minimumHeight < 170 ) {
			return new LayoutSelection( R.layout.widget_rate_compact, 2 );
		}
		if ( targetCount <= 2 || minimumHeight >= 260 ) {
			return new LayoutSelection( R.layout.widget_rate_expanded, PreferencesStore.MAX_TARGETS );
		}
		if ( minimumHeight >= 210 ) {
			return new LayoutSelection( R.layout.widget_rate, PreferencesStore.MAX_TARGETS );
		}
		return new LayoutSelection( R.layout.widget_rate, 3 );
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

	private static final class LayoutSelection {
		private final int layoutResource;
		private final int maxRows;

		LayoutSelection( final int layoutResource, final int maxRows ) {
			this.layoutResource = layoutResource;
			this.maxRows = maxRows;
		}
	}
}
