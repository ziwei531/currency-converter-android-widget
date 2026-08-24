package com.ziwei.ratedisplay;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PreferencesStore {
	public static final int MAX_PAIRS          = 15;
	private static final String PREFS          = "rate_state";
	private static final String SCHEMA_VERSION = "schema_version";
	private static final String DEFAULT_BASE   = "default_base";
	private static final String DEFAULT_TARGET = "default_target_";
	private static final String BASE_PREFIX    = "widget_base_";
	private static final String TARGET_PREFIX  = "widget_target_";
	private static final String PAIR_BASE      = "pair_base_";
	private static final String PAIR_TARGET    = "pair_target_";
	private static final String RATE_PREFIX    = "widget_rate_";
	private static final String UPDATED_PREFIX = "widget_updated_";
	private static final String LEGACY_DIRECTION = "direction";
	private static final String USD_TO_MYR     = "USD_TO_MYR";

	private PreferencesStore() {
	}

	public static WidgetConfiguration loadConfiguration( final Context context, final int widgetId ) {
		final SharedPreferences preferences = getPreferences( context );
		migrateLegacyDefaults( preferences );
		migrateWidgetConfiguration( preferences, widgetId );
		if ( !preferences.contains( getPairBaseKey( widgetId, 0 ) ) ) {
			final WidgetConfiguration defaults = loadDefaultConfiguration( context );
			return new WidgetConfiguration( widgetId, defaults.getPairs() );
		}
		return new WidgetConfiguration( widgetId, readPairs( preferences, widgetId ) );
	}

	public static WidgetConfiguration loadDefaultConfiguration( final Context context ) {
		final SharedPreferences preferences = getPreferences( context );
		migrateLegacyDefaults( preferences );
		return new WidgetConfiguration( -1, readDefaultPairs( preferences ) );
	}

	public static void saveConfiguration( final Context context, final WidgetConfiguration configuration ) {
		final SharedPreferences.Editor editor = getPreferences( context ).edit();
		editor.putInt( SCHEMA_VERSION, 3 );
		writePairs( editor, configuration.getWidgetId(), configuration );
		editor.apply();
	}

	public static void saveDefaultConfiguration( final Context context, final WidgetConfiguration configuration ) {
		final SharedPreferences.Editor editor = getPreferences( context ).edit();
		editor.putInt( SCHEMA_VERSION, 3 );
		for ( int index = 0; index < MAX_PAIRS; index++ ) {
			final ConversionPair pair = configuration.getPair( index );
			editor.putString( DEFAULT_BASE + index, pair == null ? "" : pair.getBaseCurrency() );
			editor.putString( DEFAULT_TARGET + index, pair == null ? "" : pair.getTargetCurrency() );
		}
		editor.apply();
	}

	public static String getCachedRate( final Context context, final int widgetId, final String base, final String target ) {
		return getPreferences( context ).getString( getRateKey( widgetId, base, target ), null );
	}

	public static String getCachedUpdated( final Context context, final int widgetId, final String base, final String target ) {
		return getPreferences( context ).getString( getUpdatedKey( widgetId, base, target ), null );
	}

	public static void saveCachedRate(
		final Context context,
		final int widgetId,
		final String base,
		final String target,
		final String rate,
		final String updated
	) {
		final SharedPreferences.Editor editor = getPreferences( context ).edit();
		editor.putString( getRateKey( widgetId, base, target ), rate );
		editor.putString( getUpdatedKey( widgetId, base, target ), updated );
		editor.apply();
	}

	public static void deleteWidget( final Context context, final int widgetId ) {
		final SharedPreferences.Editor editor = getPreferences( context ).edit();
		editor.remove( getBaseKey( widgetId ) );
		for ( int index = 0; index < MAX_PAIRS; index++ ) {
			editor.remove( getTargetKey( widgetId, index ) );
			editor.remove( getPairBaseKey( widgetId, index ) );
			editor.remove( getPairTargetKey( widgetId, index ) );
		}
		for ( final CurrencyCatalog.CurrencyInfo currency : CurrencyCatalog.getCurrencies() ) {
			for ( final CurrencyCatalog.CurrencyInfo target : CurrencyCatalog.getCurrencies() ) {
				editor.remove( getRateKey( widgetId, currency.getCode(), target.getCode() ) );
				editor.remove( getUpdatedKey( widgetId, currency.getCode(), target.getCode() ) );
			}
		}
		editor.apply();
	}

	private static void migrateLegacyDefaults( final SharedPreferences preferences ) {
		if ( preferences.getInt( SCHEMA_VERSION, 0 ) >= 3 ) {
			return;
		}
		final boolean isUsdToMyr = USD_TO_MYR.equals( preferences.getString( LEGACY_DIRECTION, USD_TO_MYR ) );
		final String legacyBase = preferences.getString( DEFAULT_BASE, isUsdToMyr ? "USD" : "MYR" );
		final SharedPreferences.Editor editor = preferences.edit();
		editor.putInt( SCHEMA_VERSION, 3 );
		editor.putString( DEFAULT_BASE + "0", legacyBase );
		editor.putString( DEFAULT_TARGET + "0", preferences.getString( DEFAULT_TARGET + "0", isUsdToMyr ? "MYR" : "USD" ) );
		for ( int index = 1; index < MAX_PAIRS; index++ ) {
			editor.putString( DEFAULT_BASE + index, legacyBase.length() == 0 ? "" : preferences.getString( DEFAULT_TARGET + index, "" ).length() == 0 ? "" : legacyBase );
			editor.putString( DEFAULT_TARGET + index, preferences.getString( DEFAULT_TARGET + index, "" ) );
		}
		editor.apply();
	}

	private static void migrateWidgetConfiguration( final SharedPreferences preferences, final int widgetId ) {
		if ( preferences.contains( getPairBaseKey( widgetId, 0 ) ) ) {
			return;
		}
		final String legacyBase = preferences.getString( getBaseKey( widgetId ), null );
		if ( legacyBase == null ) {
			return;
		}
		final SharedPreferences.Editor editor = preferences.edit();
		for ( int index = 0; index < MAX_PAIRS; index++ ) {
			final String target = preferences.getString( getTargetKey( widgetId, index ), "" );
			editor.putString( getPairBaseKey( widgetId, index ), target.length() == 0 ? "" : legacyBase );
			editor.putString( getPairTargetKey( widgetId, index ), target );
		}
		editor.putInt( SCHEMA_VERSION, 3 );
		editor.apply();
	}

	private static List<ConversionPair> readPairs( final SharedPreferences preferences, final int widgetId ) {
		final List<ConversionPair> pairs = new ArrayList<>();
		for ( int index = 0; index < MAX_PAIRS; index++ ) {
			final String base = preferences.getString( getPairBaseKey( widgetId, index ), "" );
			final String target = preferences.getString( getPairTargetKey( widgetId, index ), "" );
			if ( base.length() > 0 && target.length() > 0 ) {
				pairs.add( new ConversionPair( base, target ) );
			}
		}
		return pairs;
	}

	private static List<ConversionPair> readDefaultPairs( final SharedPreferences preferences ) {
		final List<ConversionPair> pairs = new ArrayList<>();
		for ( int index = 0; index < MAX_PAIRS; index++ ) {
			final String base = preferences.getString( DEFAULT_BASE + index, "" );
			final String target = preferences.getString( DEFAULT_TARGET + index, "" );
			if ( base.length() > 0 && target.length() > 0 ) {
				pairs.add( new ConversionPair( base, target ) );
			}
		}
		return pairs;
	}

	private static void writePairs(
		final SharedPreferences.Editor editor,
		final int widgetId,
		final WidgetConfiguration configuration
	) {
		for ( int index = 0; index < MAX_PAIRS; index++ ) {
			final ConversionPair pair = configuration.getPair( index );
			editor.putString( getPairBaseKey( widgetId, index ), pair == null ? "" : pair.getBaseCurrency() );
			editor.putString( getPairTargetKey( widgetId, index ), pair == null ? "" : pair.getTargetCurrency() );
		}
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

	private static String getPairBaseKey( final int widgetId, final int index ) {
		return PAIR_BASE + widgetId + "_" + index;
	}

	private static String getPairTargetKey( final int widgetId, final int index ) {
		return PAIR_TARGET + widgetId + "_" + index;
	}

	private static String getRateKey( final int widgetId, final String base, final String target ) {
		return RATE_PREFIX + widgetId + "_" + base + "_" + target;
	}

	private static String getUpdatedKey( final int widgetId, final String base, final String target ) {
		return UPDATED_PREFIX + widgetId + "_" + base + "_" + target;
	}

	public static final class ConversionPair {
		private final String baseCurrency;
		private final String targetCurrency;

		public ConversionPair( final String baseCurrency, final String targetCurrency ) {
			this.baseCurrency = baseCurrency;
			this.targetCurrency = targetCurrency;
		}

		public String getBaseCurrency() {
			return baseCurrency;
		}

		public String getTargetCurrency() {
			return targetCurrency;
		}

		@Override
		public boolean equals( final Object object ) {
			if ( !( object instanceof ConversionPair ) ) {
				return false;
			}
			final ConversionPair other = ( ConversionPair ) object;
			return baseCurrency.equals( other.baseCurrency ) && targetCurrency.equals( other.targetCurrency );
		}

		@Override
		public int hashCode() {
			return 31 * baseCurrency.hashCode() + targetCurrency.hashCode();
		}
	}

	public static final class WidgetConfiguration {
		private final int widgetId;
		private final List<ConversionPair> pairs;

		public WidgetConfiguration( final int widgetId, final List<ConversionPair> pairs ) {
			this.widgetId = widgetId;
			this.pairs = Collections.unmodifiableList( new ArrayList<>( pairs ) );
		}

		public int getWidgetId() {
			return widgetId;
		}

		public ConversionPair getPair( final int index ) {
			return index < pairs.size() ? pairs.get( index ) : null;
		}

		public List<ConversionPair> getPairs() {
			return pairs;
		}

		public boolean isSameSelection( final WidgetConfiguration other ) {
			return other != null && pairs.equals( other.pairs );
		}
	}
}
