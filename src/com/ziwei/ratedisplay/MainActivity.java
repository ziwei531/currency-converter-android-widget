package com.ziwei.ratedisplay;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {
	public static final String EXTRA_WIDGET_ID = "appWidgetId";
	private static final int INVALID_WIDGET_ID = AppWidgetManager.INVALID_APPWIDGET_ID;
	private final List<CurrencyCatalog.CurrencyInfo> currencies      = CurrencyCatalog.getCurrencies();
	private final List<TextView> targetSelectors                     = new ArrayList<>();
	private final List<String> targetCodes                           = new ArrayList<>();
	private int                  widgetId                            = INVALID_WIDGET_ID;
	private String               baseCode;
	private TextView             baseSelector;
	private int                  primaryTextColor;
	private int                  secondaryTextColor;
	private int                  surfaceColor;
	private int                  fieldStrokeColor;

	@Override
	protected void onCreate( final Bundle savedInstanceState ) {
		setTheme( R.style.AppThemeDark );
		super.onCreate( savedInstanceState );
		widgetId = getIntent().getIntExtra( EXTRA_WIDGET_ID, INVALID_WIDGET_ID );
		if ( widgetId != INVALID_WIDGET_ID ) {
			setResult( RESULT_CANCELED );
		}
		primaryTextColor    = getColor( R.color.config_dark_text );
		secondaryTextColor = getColor( R.color.config_dark_secondary );
		surfaceColor       = getColor( R.color.config_dark_surface );
		fieldStrokeColor   = getColor( R.color.config_dark_stroke );
		setTitle( "Configure Currency Converter Widget" );
		buildConfigurationScreen();
	}

	private void buildConfigurationScreen() {
		final PreferencesStore.WidgetConfiguration configuration = widgetId == INVALID_WIDGET_ID
			? PreferencesStore.loadDefaultConfiguration( this )
			: PreferencesStore.loadConfiguration( this, widgetId );
		baseCode = configuration.getBaseCurrency();
		for ( int index = 0; index < PreferencesStore.MAX_TARGETS; index++ ) {
			targetCodes.add( configuration.getTarget( index ) );
		}

		final ScrollView scroll = new ScrollView( this );
		final LinearLayout content = createColumn();
		scroll.addView( content );

		final TextView title = createText( "Currency Converter Widget", 28, primaryTextColor );
		title.setGravity( Gravity.CENTER );
		content.addView( title );

		final TextView description = createText(
			"Choose a base currency and up to five targets. Search by code or name instead of scrolling through a long list.",
			16,
			secondaryTextColor
		);
		description.setPadding( 0, 16, 0, 24 );
		content.addView( description );

		content.addView( createLabel( "Base currency" ) );
		baseSelector = createCurrencySelector( baseCode, false, new CurrencySelectionListener() {
			@Override
			public void onCurrencySelected( final CurrencyCatalog.CurrencyInfo currency ) {
				baseCode = currency.getCode();
				updateSelectorText( baseSelector, currency );
			}
		} );
		content.addView( baseSelector );

		content.addView( createLabel( "Target currencies" ) );
		for ( int index = 0; index < PreferencesStore.MAX_TARGETS; index++ ) {
			final int targetIndex = index;
			final TextView targetLabel = createText( "Target " + ( index + 1 ), 14, secondaryTextColor );
			targetLabel.setPadding( 0, 12, 0, 0 );
			content.addView( targetLabel );
			final TextView targetSelector = createCurrencySelector( targetCodes.get( index ), true, new CurrencySelectionListener() {
				@Override
				public void onCurrencySelected( final CurrencyCatalog.CurrencyInfo currency ) {
					targetCodes.set( targetIndex, currency.getCode() );
					updateSelectorText( targetSelectors.get( targetIndex ), currency );
				}
			} );
			targetSelectors.add( targetSelector );
			content.addView( targetSelector );
		}

		final Button saveButton = new Button( this );
		saveButton.setText( widgetId == INVALID_WIDGET_ID ? "Save defaults for new widgets" : "Save and refresh widget" );
		saveButton.setTextColor( primaryTextColor );
		saveButton.setAllCaps( false );
		saveButton.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick( final View view ) {
				saveConfiguration();
			}
		} );
		content.addView( saveButton );
		setContentView( scroll );
	}

	private TextView createCurrencySelector(
		final String code,
		final boolean allowEmpty,
		final CurrencySelectionListener listener
	) {
		final TextView selector = createText( "", 16, primaryTextColor );
		selector.setGravity( Gravity.CENTER_VERTICAL );
		selector.setPadding( 18, 0, 18, 0 );
		selector.setMinHeight( 54 );
		selector.setBackground( createFieldBackground() );
		selector.setCompoundDrawablesWithIntrinsicBounds( 0, 0, android.R.drawable.arrow_down_float, 0 );
		selector.setCompoundDrawablePadding( 12 );
		selector.setFocusable( true );
		selector.setClickable( true );
		selector.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick( final View view ) {
				showCurrencyDialog( selector, allowEmpty, listener );
			}
		} );
		if ( code == null || code.length() == 0 ) {
			selector.setText( "Not selected" );
		} else {
			updateSelectorText( selector, CurrencyCatalog.find( code ) );
		}
		return selector;
	}

	private void showCurrencyDialog(
		final TextView selector,
		final boolean allowEmpty,
		final CurrencySelectionListener listener
	) {
		final LinearLayout content = new LinearLayout( this );
		content.setOrientation( LinearLayout.VERTICAL );
		content.setPadding( 24, 0, 24, 0 );
		final EditText search = new EditText( this );
		search.setHint( "Search code or currency name" );
		search.setSingleLine( true );
		search.setInputType( InputType.TYPE_CLASS_TEXT );
		content.addView( search, new LinearLayout.LayoutParams( -1, -2 ) );
		final List<CurrencyCatalog.CurrencyInfo> filtered = new ArrayList<>();
		if ( allowEmpty ) {
			filtered.add( new CurrencyCatalog.CurrencyInfo( "" ) );
		}
		filtered.addAll( currencies );
		final ArrayAdapter<CurrencyCatalog.CurrencyInfo> adapter = new ArrayAdapter<CurrencyCatalog.CurrencyInfo>( this, android.R.layout.simple_list_item_1, filtered ) {
			@Override
			public View getView( final int position, final View convertView, final android.view.ViewGroup parent ) {
				final TextView view = ( TextView ) super.getView( position, convertView, parent );
				final CurrencyCatalog.CurrencyInfo currency = getItem( position );
				view.setText( currency.getCode().length() == 0 ? "Not selected" : currency.getLabel() );
				view.setTextSize( 16 );
				view.setTextColor( primaryTextColor );
				view.setPadding( 18, 18, 18, 18 );
				return view;
			}
		};
		final ListView list = new ListView( this );
		list.setAdapter( adapter );
		content.addView( list, new LinearLayout.LayoutParams( -1, 0, 1 ) );
		final AlertDialog dialog = new AlertDialog.Builder( this )
			.setTitle( "Choose currency" )
			.setView( content )
			.setNegativeButton( "Cancel", null )
			.create();
		list.setOnItemClickListener( ( parent, view, position, id ) -> {
			final CurrencyCatalog.CurrencyInfo chosen = adapter.getItem( position );
			if ( chosen != null ) {
				if ( chosen.getCode().length() == 0 ) {
					selector.setText( "Not selected" );
				} else {
					listener.onCurrencySelected( chosen );
				}
				if ( chosen.getCode().length() == 0 ) {
					listener.onCurrencySelected( chosen );
				}
			}
			dialog.dismiss();
		} );
		search.addTextChangedListener( new TextWatcher() {
			@Override
			public void beforeTextChanged( final CharSequence text, final int start, final int count, final int after ) {
			}
			@Override
			public void onTextChanged( final CharSequence text, final int start, final int before, final int count ) {
				final String query = text.toString().trim().toLowerCase();
				adapter.clear();
				if ( allowEmpty && query.length() == 0 ) {
					adapter.add( new CurrencyCatalog.CurrencyInfo( "" ) );
				}
				for ( final CurrencyCatalog.CurrencyInfo currency : currencies ) {
					if ( currency.getCode().toLowerCase().contains( query ) || currency.getName().toLowerCase().contains( query ) ) {
						adapter.add( currency );
					}
				}
			}
			@Override
			public void afterTextChanged( final Editable text ) {
			}
		} );
		dialog.setOnShowListener( new DialogInterface.OnShowListener() {
			@Override
			public void onShow( final DialogInterface ignored ) {
				search.requestFocus();
				final Window window = dialog.getWindow();
				if ( window != null ) {
					window.setSoftInputMode( android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE );
				}
			}
		} );
		dialog.show();
	}

	private void updateSelectorText( final TextView selector, final CurrencyCatalog.CurrencyInfo currency ) {
		if ( currency.getCode().length() == 0 ) {
			selector.setText( "Not selected" );
			return;
		}
		selector.setText( currency.getLabel() );
	}

	private GradientDrawable createFieldBackground() {
		final GradientDrawable background = new GradientDrawable();
		background.setColor( surfaceColor );
		background.setCornerRadius( 16 );
		background.setStroke( 2, fieldStrokeColor );
		return background;
	}

	private void saveConfiguration() {
		final String      baseCurrency     = baseCode;
		final List<String> targets         = new ArrayList<>();
		final Set<String> selectedTargets  = new HashSet<>();
		for ( final String target : targetCodes ) {
			if ( target.length() > 0 ) {
				if ( baseCurrency.equals( target ) ) {
					showMessage( "A target must be different from the base currency." );
					return;
				}
				if ( !selectedTargets.add( target ) ) {
					showMessage( "Each target currency can only be selected once." );
					return;
				}
			}
			targets.add( target );
		}
		if ( selectedTargets.isEmpty() ) {
			showMessage( "Select at least one target currency." );
			return;
		}
		final PreferencesStore.WidgetConfiguration configuration = new PreferencesStore.WidgetConfiguration( widgetId, baseCurrency, targets );
		if ( widgetId == INVALID_WIDGET_ID ) {
			PreferencesStore.saveDefaultConfiguration( this, configuration );
			Toast.makeText( this, "Defaults saved for new widgets", Toast.LENGTH_SHORT ).show();
		} else {
			PreferencesStore.saveConfiguration( this, configuration );
			RateWidgetProvider.refreshWidget( this, widgetId );
			setResult( RESULT_OK, getResultIntent() );
			Toast.makeText( this, "Currency widget updated", Toast.LENGTH_SHORT ).show();
		}
		finish();
	}

	private Intent getResultIntent() {
		final Intent result = new Intent();
		result.putExtra( EXTRA_WIDGET_ID, widgetId );
		return result;
	}

	private LinearLayout createColumn() {
		final LinearLayout content = new LinearLayout( this );
		content.setOrientation( LinearLayout.VERTICAL );
		content.setPadding( 32, 32, 32, 32 );
		content.setBackgroundColor( getColor( R.color.config_dark_background ) );
		return content;
	}

	private TextView createLabel( final String text ) {
		return createText( text, 18, primaryTextColor );
	}

	private TextView createText( final String text, final int size, final int color ) {
		final TextView view = new TextView( this );
		view.setText( text );
		view.setTextSize( size );
		view.setTextColor( color );
		return view;
	}

	private void showMessage( final String message ) {
		Toast.makeText( this, message, Toast.LENGTH_SHORT ).show();
	}

	private interface CurrencySelectionListener {
		void onCurrencySelected( CurrencyCatalog.CurrencyInfo currency );
	}
}
