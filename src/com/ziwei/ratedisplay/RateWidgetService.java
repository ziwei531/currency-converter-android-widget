package com.ziwei.ratedisplay;

import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class RateWidgetService extends RemoteViewsService {
	@Override
	public RemoteViewsFactory onGetViewFactory( final Intent intent ) {
		return new RateRemoteViewsFactory( getApplicationContext(), intent.getIntExtra( RateWidgetProvider.EXTRA_WIDGET_ID, -1 ) );
	}

	private static final class RateRemoteViewsFactory implements RemoteViewsFactory {
		private final android.content.Context context;
		private final int widgetId;
		private List<PreferencesStore.ConversionPair> pairs;
		private boolean isRefreshing;

		RateRemoteViewsFactory( final android.content.Context context, final int widgetId ) {
			this.context = context;
			this.widgetId = widgetId;
		}

		@Override
		public void onCreate() {
			loadConfiguration();
		}

		@Override
		public void onDataSetChanged() {
			loadConfiguration();
		}

		@Override
		public void onDestroy() {
			pairs = null;
		}

		@Override
		public int getCount() {
			return pairs == null ? 0 : pairs.size();
		}

		@Override
		public RemoteViews getViewAt( final int position ) {
			final RemoteViews row = new RemoteViews( context.getPackageName(), R.layout.widget_rate_item );
			if ( pairs == null || position < 0 || position >= pairs.size() ) {
				return row;
			}
			final PreferencesStore.ConversionPair pair = pairs.get( position );
			final CurrencyCatalog.CurrencyInfo target = CurrencyCatalog.find( pair.getTargetCurrency() );
			final String cached = PreferencesStore.getCachedRate(
				context,
				widgetId,
				pair.getBaseCurrency(),
				pair.getTargetCurrency()
			);
			row.setTextViewText( R.id.target_code, pair.getBaseCurrency() + " → " + pair.getTargetCurrency() );
			row.setTextViewText( R.id.target_value, isRefreshing ? "…" : cached == null ? "—" : formatRate( target, cached ) );
			final Intent fillIn = new Intent();
			fillIn.putExtra( MainActivity.EXTRA_PAIR_INDEX, position );
			row.setOnClickFillInIntent( R.id.widget_rate_item_root, fillIn );
			return row;
		}

		@Override
		public RemoteViews getLoadingView() {
			return null;
		}

		@Override
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public long getItemId( final int position ) {
			return position;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		private void loadConfiguration() {
			final PreferencesStore.WidgetConfiguration configuration = PreferencesStore.loadConfiguration( context, widgetId );
			pairs = configuration.getPairs();
			isRefreshing = RateWidgetProvider.isRefreshInProgressForService( widgetId );
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
	}
}
