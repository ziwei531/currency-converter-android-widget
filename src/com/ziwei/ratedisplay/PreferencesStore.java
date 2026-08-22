package com.ziwei.ratedisplay;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PreferencesStore {
    public static final int MAX_TARGETS = 5;
    public static final int DEFAULT_WIDGET_ID = 0;
    private static final String PREFS = "rate_state";
    private static final String SCHEMA_VERSION = "schema_version";
    private static final String BASE_PREFIX = "widget_base_";
    private static final String TARGET_PREFIX = "widget_target_";
    private static final String RATE_PREFIX = "widget_rate_";
    private static final String UPDATED_PREFIX = "widget_updated_";
    private static final String LEGACY_DIRECTION = "direction";
    private static final String USD_TO_MYR = "USD_TO_MYR";

    private PreferencesStore() {
    }

    public static WidgetConfiguration loadConfiguration( final Context context, final int widgetId ) {
        final SharedPreferences preferences = getPreferences( context );
        migrateLegacyConfiguration( preferences, widgetId );
        final String base = preferences.getString( getBaseKey( widgetId ), "USD" );
        final List<String> targets = new ArrayList<>();
        for ( int index = 0; index < MAX_TARGETS; index++ ) {
            final String target = preferences.getString( getTargetKey( widgetId, index ), index == 0 ? "MYR" : "" );
            targets.add( target );
        }
        return new WidgetConfiguration( widgetId, base, targets );
    }

    public static void saveConfiguration( final Context context, final WidgetConfiguration configuration ) {
        final SharedPreferences.Editor editor = getPreferences( context ).edit();
        editor.putInt( SCHEMA_VERSION, 2 );
        editor.putString( getBaseKey( configuration.getWidgetId() ), configuration.getBaseCurrency() );
        for ( int index = 0; index < MAX_TARGETS; index++ ) {
            editor.putString( getTargetKey( configuration.getWidgetId(), index ), configuration.getTarget( index ) );
        }
        editor.apply();
    }

    public static String getCachedRate( final Context context, final int widgetId, final String target ) {
        return getPreferences( context ).getString( getRateKey( widgetId, target ), null );
    }

    public static String getCachedUpdated( final Context context, final int widgetId, final String target ) {
        return getPreferences( context ).getString( getUpdatedKey( widgetId, target ), null );
    }

    public static void saveCachedRate( final Context context, final int widgetId, final String base, final String target, final String rate, final String updated ) {
        final SharedPreferences.Editor editor = getPreferences( context ).edit();
        editor.putString( getRateKey( widgetId, target ), base + "|" + rate );
        editor.putString( getUpdatedKey( widgetId, target ), updated );
        editor.apply();
    }

    public static void deleteWidget( final Context context, final int widgetId ) {
        final SharedPreferences.Editor editor = getPreferences( context ).edit();
        editor.remove( getBaseKey( widgetId ) );
        for ( int index = 0; index < MAX_TARGETS; index++ ) {
            editor.remove( getTargetKey( widgetId, index ) );
        }
        for ( final CurrencyCatalog.CurrencyInfo currency : CurrencyCatalog.getCurrencies() ) {
            editor.remove( getRateKey( widgetId, currency.getCode() ) );
            editor.remove( getUpdatedKey( widgetId, currency.getCode() ) );
        }
        editor.apply();
    }

    private static void migrateLegacyConfiguration( final SharedPreferences preferences, final int widgetId ) {
        final String baseKey = getBaseKey( widgetId );
        if ( preferences.contains( baseKey ) ) {
            return;
        }

        final String legacyDirection = preferences.getString( LEGACY_DIRECTION, USD_TO_MYR );
        final boolean legacyUsdToMyr = USD_TO_MYR.equals( legacyDirection );
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putInt( SCHEMA_VERSION, 2 );
        editor.putString( baseKey, legacyUsdToMyr ? "USD" : "MYR" );
        editor.putString( getTargetKey( widgetId, 0 ), legacyUsdToMyr ? "MYR" : "USD" );
        for ( int index = 1; index < MAX_TARGETS; index++ ) {
            editor.putString( getTargetKey( widgetId, index ), "" );
        }
        editor.apply();
    }

    private static SharedPreferences getPreferences( final Context context ) {
        return context.getSharedPreferences( PREFS, Context.MODE_PRIVATE );
    }

    private static String getBaseKey( final int widgetId ) {
        return BASE_PREFIX + widgetId;
    }

    private static String getTargetKey( final int widgetId, final int index ) {
        return TARGET_PREFIX + widgetId + "_" + index;
    }

    private static String getRateKey( final int widgetId, final String target ) {
        return RATE_PREFIX + widgetId + "_" + target;
    }

    private static String getUpdatedKey( final int widgetId, final String target ) {
        return UPDATED_PREFIX + widgetId + "_" + target;
    }

    public static final class WidgetConfiguration {
        private final int widgetId;
        private final String baseCurrency;
        private final List<String> targets;

        public WidgetConfiguration( final int widgetId, final String baseCurrency, final List<String> targets ) {
            this.widgetId = widgetId;
            this.baseCurrency = baseCurrency;
            this.targets = Collections.unmodifiableList( new ArrayList<>( targets ) );
        }

        public int getWidgetId() {
            return widgetId;
        }

        public String getBaseCurrency() {
            return baseCurrency;
        }

        public String getTarget( final int index ) {
            return targets.get( index );
        }

        public List<String> getTargets() {
            return targets;
        }

        public boolean hasTarget( final String target ) {
            return targets.contains( target );
        }

        public boolean isSameSelection( final WidgetConfiguration other ) {
            return other != null
                && baseCurrency.equals( other.baseCurrency )
                && targets.equals( other.targets );
        }
    }
}
