package com.ziwei.ratedisplay;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.RemoteViews;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RateWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_REFRESH = "com.ziwei.ratedisplay.REFRESH";
    private static final String ACTION_SWAP = "com.ziwei.ratedisplay.SWAP";
    private static final String ACTION_AUTO_REFRESH = "com.ziwei.ratedisplay.AUTO_REFRESH";
    private static final String ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
    private static final String BASE_API_URL = "https://api.frankfurter.app/latest";
    private static final String PREFS = "rate_state";
    private static final String DIRECTION = "direction";
    private static final String USD_TO_MYR = "USD_TO_MYR";
    private static final String MYR_TO_USD = "MYR_TO_USD";
    private static final long REFRESH_INTERVAL_MILLIS = 30L * 60L * 1000L;
    private static final AtomicBoolean REFRESH_IN_PROGRESS = new AtomicBoolean( false );

    @Override
    public void onUpdate( final Context context, final AppWidgetManager manager, final int[] ids ) {
        scheduleAutoRefresh( context );
        for ( final int id : ids ) {
            updateFromCache( context, manager, id );
        }
        fetchAndRefresh( context );
    }

    @Override
    public void onEnabled( final Context context ) {
        super.onEnabled( context );
        scheduleAutoRefresh( context );
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

        if ( ACTION_SWAP.equals( action ) ) {
            swapDirection( context );
            return;
        }

        if ( ACTION_REFRESH.equals( action ) ) {
            setAll( context, "Fetching…", "Updating mid-market rate" );
            fetchAndRefresh( context );
            return;
        }

        if ( ACTION_AUTO_REFRESH.equals( action ) ) {
            fetchAndRefresh( context );
            return;
        }

        if ( ACTION_BOOT_COMPLETED.equals( action ) ) {
            scheduleAutoRefreshIfWidgetExists( context );
        }
    }

    private void fetchAndRefresh( final Context context ) {
        if ( !REFRESH_IN_PROGRESS.compareAndSet( false, true ) ) {
            return;
        }

        new Thread( new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    final String requestUrl = BASE_API_URL
                        + "?from=" + getBaseCurrency( context )
                        + "&to=" + getTargetCurrency( context );
                    connection = (HttpURLConnection) new URL( requestUrl ).openConnection();
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
                    String line;
                    while ( ( line = reader.readLine() ) != null ) {
                        body.append( line );
                    }
                    reader.close();

                    final String targetCurrency = getTargetCurrency( context );
                    final Pattern ratePattern = Pattern.compile(
                        "\"" + targetCurrency + "\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)"
                    );
                    final Matcher match = ratePattern.matcher( body.toString() );
                    if ( !match.find() ) {
                        throw new Exception( "MYR rate missing" );
                    }

                    final String rate = match.group( 1 );
                    final String updated = new SimpleDateFormat( "HH:mm, dd MMM", Locale.getDefault() ).format( new Date() );
                    context.getSharedPreferences( PREFS, Context.MODE_PRIVATE ).edit()
                        .putString( "rate", rate )
                        .putString( "updated", updated )
                        .apply();
                    setAll( context, formatRate( context, rate ), "Updated " + updated + " · mid-market" );
                } catch ( final Exception error ) {
                    showCachedOrUnavailable( context );
                } finally {
                    if ( connection != null ) {
                        connection.disconnect();
                    }
                    REFRESH_IN_PROGRESS.set( false );
                }
            }
        } ).start();
    }

    private static void showCachedOrUnavailable( final Context context ) {
        final String cached = context.getSharedPreferences( PREFS, Context.MODE_PRIVATE ).getString( "rate", null );
        final String updated = context.getSharedPreferences( PREFS, Context.MODE_PRIVATE ).getString( "updated", null );
        if ( cached != null ) {
            setAll( context, formatRate( context, cached ), "Cached · " + updated + " · offline" );
        } else {
            setAll( context, "Unavailable", "Tap ↻ to retry" );
        }
    }

    private static void scheduleAutoRefresh( final Context context ) {
        final AlarmManager alarmManager = (AlarmManager) context.getSystemService( Context.ALARM_SERVICE );
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

    private void swapDirection( final Context context ) {
        final String currentDirection = getDirection( context );
        final String nextDirection = USD_TO_MYR.equals( currentDirection ) ? MYR_TO_USD : USD_TO_MYR;
        context.getSharedPreferences( PREFS, Context.MODE_PRIVATE ).edit()
            .putString( DIRECTION, nextDirection )
            .apply();
        setAll( context, "Fetching…", "Updating mid-market rate" );
        fetchAndRefresh( context );
    }

    private static void scheduleAutoRefreshIfWidgetExists( final Context context ) {
        final AppWidgetManager manager = AppWidgetManager.getInstance( context );
        final int[] ids = manager.getAppWidgetIds( new ComponentName( context, RateWidgetProvider.class ) );
        if ( ids.length > 0 ) {
            scheduleAutoRefresh( context );
        }
    }

    private static void cancelAutoRefresh( final Context context ) {
        final AlarmManager alarmManager = (AlarmManager) context.getSystemService( Context.ALARM_SERVICE );
        if ( alarmManager != null ) {
            alarmManager.cancel( getAutoRefreshIntent( context ) );
        }
    }

    private static PendingIntent getAutoRefreshIntent( final Context context ) {
        final Intent refresh = new Intent( context, RateWidgetProvider.class ).setAction( ACTION_AUTO_REFRESH );
        return PendingIntent.getBroadcast(
            context,
            0,
            refresh,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static void setAll( final Context context, final String rate, final String subtitle ) {
        final AppWidgetManager manager = AppWidgetManager.getInstance( context );
        final int[] ids = manager.getAppWidgetIds( new ComponentName( context, RateWidgetProvider.class ) );
        for ( final int id : ids ) {
            updateWidget( context, manager, id, rate, subtitle );
        }
    }

    private static void updateFromCache( final Context context, final AppWidgetManager manager, final int id ) {
        final String rate = context.getSharedPreferences( PREFS, Context.MODE_PRIVATE ).getString( "rate", "Loading…" );
        final String updated = context.getSharedPreferences( PREFS, Context.MODE_PRIVATE ).getString( "updated", "Fetching mid-market rate" );
        final String displayRate = rate.equals( "Loading…" ) ? rate : "RM " + rate;
        updateWidget( context, manager, id, displayRate, updated );
    }

    private static String getDirection( final Context context ) {
        return context.getSharedPreferences( PREFS, Context.MODE_PRIVATE ).getString( DIRECTION, USD_TO_MYR );
    }

    private static String getBaseCurrency( final Context context ) {
        return USD_TO_MYR.equals( getDirection( context ) ) ? "USD" : "MYR";
    }

    private static String getTargetCurrency( final Context context ) {
        return USD_TO_MYR.equals( getDirection( context ) ) ? "MYR" : "USD";
    }

    private static String getDirectionTitle( final Context context ) {
        return getBaseCurrency( context ) + " → " + getTargetCurrency( context );
    }

    private static String formatRate( final Context context, final String rate ) {
        return "MYR".equals( getTargetCurrency( context ) ) ? "RM " + rate : "$ " + rate;
    }

    private static void updateWidget( final Context context, final AppWidgetManager manager, final int id, final String rate, final String subtitle ) {
        final RemoteViews views = new RemoteViews( context.getPackageName(), R.layout.widget_rate );
        views.setTextViewText( R.id.widget_title, getDirectionTitle( context ) );
        views.setTextViewText( R.id.widget_rate, rate );
        views.setTextViewText( R.id.widget_updated, subtitle );

        final Intent swap = new Intent( context, RateWidgetProvider.class ).setAction( ACTION_SWAP );
        final PendingIntent swapPending = PendingIntent.getBroadcast(
            context,
            id + 100000,
            swap,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent( R.id.widget_swap, swapPending );

        final Intent refresh = new Intent( context, RateWidgetProvider.class ).setAction( ACTION_REFRESH );
        final PendingIntent pending = PendingIntent.getBroadcast(
            context,
            id,
            refresh,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent( R.id.widget_refresh, pending );
        manager.updateAppWidget( id, views );
    }
}
