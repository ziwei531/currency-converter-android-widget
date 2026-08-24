package com.ziwei.ratedisplay;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
	private final List<CurrencyCatalog.CurrencyInfo> currencies = CurrencyCatalog.getCurrencies();
	private final List<String> targetCodes = new ArrayList<>();
	private int widgetId = INVALID_WIDGET_ID;
	private String baseCode;
	private LinearLayout baseSelector;
	private LinearLayout targetList;
	private TextView targetCount;
	private TextView addCurrency;
	private int primaryTextColor;
	private int secondaryTextColor;
	private int surfaceColor;
	private int fieldStrokeColor;
	private int backgroundColor;
	private int accentColor;

	@Override
	protected void onCreate( final Bundle savedInstanceState ) {
		final boolean isNightMode = ( getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK ) == Configuration.UI_MODE_NIGHT_YES;
		setTheme( isNightMode ? R.style.AppThemeDark : R.style.AppTheme );
		super.onCreate( savedInstanceState );
		widgetId = getIntent().getIntExtra( EXTRA_WIDGET_ID, INVALID_WIDGET_ID );
		if ( widgetId != INVALID_WIDGET_ID ) {
			setResult( RESULT_CANCELED );
		}
		primaryTextColor = getColor( isNightMode ? R.color.config_dark_text : R.color.config_light_text );
		secondaryTextColor = getColor( isNightMode ? R.color.config_dark_secondary : R.color.config_light_secondary );
		surfaceColor = getColor( isNightMode ? R.color.config_dark_surface : R.color.config_light_surface );
		fieldStrokeColor = getColor( isNightMode ? R.color.config_dark_stroke : R.color.config_light_stroke );
		backgroundColor = getColor( isNightMode ? R.color.config_dark_background : R.color.config_light_background );
		accentColor = getColor( isNightMode ? R.color.config_dark_accent : R.color.config_light_accent );
		getWindow().setBackgroundDrawable( new ColorDrawable( backgroundColor ) );
		setTitle( "Currency Converter Widget" );
		buildConfigurationScreen();
	}

	private void buildConfigurationScreen() {
		final PreferencesStore.WidgetConfiguration configuration = widgetId == INVALID_WIDGET_ID
			? PreferencesStore.loadDefaultConfiguration( this )
			: PreferencesStore.loadConfiguration( this, widgetId );
		baseCode = configuration.getBaseCurrency();
		targetCodes.clear();
		for ( final String target : configuration.getTargets() ) {
			if ( target != null && target.length() > 0 && !targetCodes.contains( target ) ) {
				targetCodes.add( target );
			}
		}

		final ScrollView scroll = new ScrollView( this );
		scroll.setFillViewport( true );
		scroll.setBackgroundColor( backgroundColor );
		final LinearLayout content = createColumn();
		scroll.addView( content );

		final LinearLayout backRow = new LinearLayout( this );
		backRow.setGravity( Gravity.LEFT | Gravity.CENTER_VERTICAL );
		final ImageButton back = new ImageButton( this );
		back.setImageResource( R.drawable.ic_back );
		back.setColorFilter( primaryTextColor, PorterDuff.Mode.SRC_IN );
		back.setScaleType( ImageButton.ScaleType.CENTER );
		back.setPadding( 0, 0, 0, 0 );
		back.setMinimumHeight( dp( 40 ) );
		back.setBackground( createBackButtonBackground() );
		back.setContentDescription( "Back" );
		back.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick( final View view ) {
				finish();
			}
		} );
		backRow.addView( back, new LinearLayout.LayoutParams( dp( 52 ), dp( 40 ) ) );
		content.addView( backRow );

		final TextView title = createText( "Currency Converter Widget", 20, primaryTextColor );
		title.setGravity( Gravity.CENTER );
		title.setSingleLine( true );
		content.addView( title, new LinearLayout.LayoutParams( -1, dp( 40 ) ) );

		final TextView description = createText(
			"Choose a base currency and up to five target currencies to show on your widget.",
			14,
			secondaryTextColor
		);
		description.setGravity( Gravity.CENTER );
		description.setPadding( dp( 16 ), dp( 4 ), dp( 16 ), dp( 24 ) );
		content.addView( description );

		content.addView( createSectionLabel( "BASE CURRENCY" ) );
		baseSelector = createCurrencyRow( baseCode, false );
		baseSelector.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick( final View view ) {
				showCurrencyPicker( true, -1 );
			}
		} );
		content.addView( baseSelector, withBottomMargin( dp( 24 ) ) );

		final LinearLayout targetHeader = new LinearLayout( this );
		targetHeader.setGravity( Gravity.CENTER_VERTICAL );
		targetHeader.addView( createSectionLabel( "TARGET CURRENCIES" ), new LinearLayout.LayoutParams( 0, -2, 1 ) );
		targetCount = createCountBadge();
		targetHeader.addView( targetCount );
		content.addView( targetHeader );

		targetList = new LinearLayout( this );
		targetList.setOrientation( LinearLayout.VERTICAL );
		content.addView( targetList );

		addCurrency = createAddCurrencyControl();
		content.addView( addCurrency, withTopBottomMargin( dp( 12 ), dp( 24 ) ) );

		final Button saveButton = createSaveButton();
		content.addView( saveButton );
		final TextView helper = createText( "↻  Widget will refresh after saving.", 13, secondaryTextColor );
		helper.setGravity( Gravity.CENTER );
		helper.setPadding( 0, dp( 8 ), 0, 0 );
		content.addView( helper );

		setContentView( scroll );
		renderTargets();
	}

	private LinearLayout createCurrencyRow( final String code, final boolean isTarget ) {
		final CurrencyCatalog.CurrencyInfo currency = CurrencyCatalog.find( code );
		final LinearLayout row = new LinearLayout( this );
		row.setGravity( Gravity.CENTER_VERTICAL );
		row.setPadding( dp( 16 ), dp( 10 ), dp( 12 ), dp( 10 ) );
		row.setMinimumHeight( dp( 64 ) );
		row.setBackground( createFieldBackground() );

		if ( isTarget ) {
			final TextView handle = createText( "≡", 24, secondaryTextColor );
			handle.setGravity( Gravity.CENTER );
			handle.setContentDescription( "Target order" );
			row.addView( handle, new LinearLayout.LayoutParams( dp( 32 ), -1 ) );
		}

		final LinearLayout text = new LinearLayout( this );
		text.setOrientation( LinearLayout.VERTICAL );
		text.setGravity( Gravity.CENTER_VERTICAL );
		final TextView name = createText( currency.getName(), 16, primaryTextColor );
		name.setSingleLine( false );
		name.setEllipsize( android.text.TextUtils.TruncateAt.END );
		final TextView codeView = createText( currency.getCode(), 13, secondaryTextColor );
		text.addView( name );
		text.addView( codeView );
		row.addView( text, new LinearLayout.LayoutParams( 0, -2, 1 ) );

		if ( isTarget ) {
			final TextView remove = createText( "×", 28, secondaryTextColor );
			remove.setGravity( Gravity.CENTER );
			remove.setContentDescription( "Remove " + currency.getName() );
			row.addView( remove, new LinearLayout.LayoutParams( dp( 44 ), dp( 48 ) ) );
		}

		return row;
	}

	private void renderTargets() {
		targetList.removeAllViews();
		for ( int index = 0; index < targetCodes.size(); index++ ) {
			final int targetIndex = index;
			final LinearLayout row = createCurrencyRow( targetCodes.get( index ), true );
			row.setOnClickListener( new View.OnClickListener() {
				@Override
				public void onClick( final View view ) {
					showCurrencyPicker( false, targetIndex );
				}
			} );
			final TextView reorder = ( TextView ) row.getChildAt( 0 );
			reorder.setContentDescription( "Long-press and drag to reorder" );
			reorder.setOnLongClickListener( new View.OnLongClickListener() {
			@Override
			public boolean onLongClick( final View view ) {
				final ClipData data = ClipData.newPlainText( "target-index", String.valueOf( targetIndex ) );
				final View.DragShadowBuilder shadow = new View.DragShadowBuilder( row ) {
					@Override
					public void onProvideShadowMetrics( final Point shadowSize, final Point shadowTouchPoint ) {
						shadowSize.set( row.getMeasuredWidth(), row.getMeasuredHeight() );
						shadowTouchPoint.set( reorder.getMeasuredWidth() / 2, row.getMeasuredHeight() / 2 );
					}
				};
				return view.startDragAndDrop( data, shadow, null, 0 );
			}
			} );
			row.setOnDragListener( new View.OnDragListener() {
				@Override
				public boolean onDrag( final View view, final DragEvent event ) {
					switch ( event.getAction() ) {
						case DragEvent.ACTION_DRAG_STARTED:
							return event.getClipDescription() != null
								&& event.getClipDescription().hasMimeType( "text/plain" );
						case DragEvent.ACTION_DRAG_ENTERED:
							row.setAlpha( 0.65f );
							return true;
						case DragEvent.ACTION_DRAG_EXITED:
							row.setAlpha( 1.0f );
							return true;
						case DragEvent.ACTION_DROP: {
							row.setAlpha( 1.0f );
							if ( event.getClipData() == null || event.getClipData().getItemCount() == 0 ) {
								return false;
							}
							try {
								final CharSequence payload = event.getClipData().getItemAt( 0 ).getText();
								if ( payload == null ) {
									return false;
								}
								final int sourceIndex = Integer.parseInt( payload.toString() );
								moveTarget( sourceIndex, targetIndex );
								return true;
							} catch ( final NumberFormatException ignored ) {
								return false;
							}
						}
						case DragEvent.ACTION_DRAG_ENDED:
							row.setAlpha( 1.0f );
							return true;
						default:
							return true;
					}
				}
			} );
			final TextView remove = ( TextView ) ( ( ViewGroup ) row ).getChildAt( row.getChildCount() - 1 );
			remove.setOnClickListener( new View.OnClickListener() {
				@Override
				public void onClick( final View view ) {
					targetCodes.remove( targetIndex );
					renderTargets();
				}
			} );
			targetList.addView( row, withTopMargin( index == 0 ? dp( 8 ) : dp( 6 ) ) );
		}
		targetCount.setText( targetCodes.size() + " / " + PreferencesStore.MAX_TARGETS );
		final boolean canAdd = targetCodes.size() < PreferencesStore.MAX_TARGETS;
		addCurrency.setVisibility( canAdd ? View.VISIBLE : View.GONE );
	}

	private void moveTarget( final int sourceIndex, final int targetIndex ) {
		if ( sourceIndex < 0 || sourceIndex >= targetCodes.size() || sourceIndex == targetIndex ) {
			return;
		}
		final String movedTarget = targetCodes.remove( sourceIndex );
		final int insertionIndex = Math.min( targetIndex, targetCodes.size() );
		targetCodes.add( insertionIndex, movedTarget );
		renderTargets();
	}

	private TextView createAddCurrencyControl() {
		final TextView add = createText( "+  Add currency", 16, primaryTextColor );
		add.setGravity( Gravity.CENTER );
		add.setMinHeight( dp( 56 ) );
		add.setBackground( createDashedLikeBackground() );
		add.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick( final View view ) {
				if ( targetCodes.size() < PreferencesStore.MAX_TARGETS ) {
					showCurrencyPicker( false, -1 );
				}
			}
		} );
		return add;
	}

	private Button createSaveButton() {
		final Button save = new Button( this );
		save.setText( "Save changes" );
		save.setTextColor( isLightColor( accentColor ) ? 0xFF241B35 : 0xFFFFFFFF );
		save.setTextSize( 16 );
		save.setAllCaps( false );
		save.setMinHeight( dp( 52 ) );
		save.setBackground( createAccentBackground() );
		save.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick( final View view ) {
				saveConfiguration();
			}
		} );
		return save;
	}

	private TextView createCountBadge() {
		final TextView badge = createText( "0 / " + PreferencesStore.MAX_TARGETS, 13, primaryTextColor );
		badge.setGravity( Gravity.CENTER );
		badge.setPadding( dp( 10 ), dp( 5 ), dp( 10 ), dp( 5 ) );
		badge.setBackground( createBadgeBackground() );
		return badge;
	}

	private void showCurrencyPicker( final boolean selectingBase, final int targetIndex ) {
		final LinearLayout content = new LinearLayout( this );
		content.setOrientation( LinearLayout.VERTICAL );
		content.setPadding( dp( 20 ), 0, dp( 20 ), 0 );
		content.setMinimumHeight( dp( 480 ) );

		final EditText search = new EditText( this );
		search.setHint( "Search by name or code..." );
		search.setSingleLine( true );
		search.setInputType( InputType.TYPE_CLASS_TEXT );
		search.setGravity( Gravity.CENTER_VERTICAL );
		search.setMinHeight( dp( 56 ) );
		search.setPadding( dp( 16 ), 0, dp( 16 ), 0 );
		search.setBackground( createSearchBackground() );
		content.addView( search, withTopBottomMargin( dp( 12 ), dp( 12 ) ) );

		final List<CurrencyCatalog.CurrencyInfo> visibleCurrencies = new ArrayList<>();
		final BaseAdapter adapter = new BaseAdapter() {
			@Override
			public int getCount() {
				return visibleCurrencies.size();
			}

			@Override
			public CurrencyCatalog.CurrencyInfo getItem( final int position ) {
				return visibleCurrencies.get( position );
			}

			@Override
			public long getItemId( final int position ) {
				return position;
			}

			@Override
			public View getView( final int position, final View convertView, final ViewGroup parent ) {
				final CurrencyCatalog.CurrencyInfo currency = getItem( position );
				final LinearLayout row = new LinearLayout( MainActivity.this );
				row.setGravity( Gravity.CENTER_VERTICAL );
				row.setPadding( dp( 8 ), dp( 10 ), dp( 8 ), dp( 10 ) );
				final TextView name = createText( currency.getName(), 16, primaryTextColor );
				name.setSingleLine( false );
				name.setEllipsize( android.text.TextUtils.TruncateAt.END );
				row.addView( name, new LinearLayout.LayoutParams( 0, -2, 1 ) );
				final TextView code = createText( currency.getCode() + "  +", 14, secondaryTextColor );
				code.setGravity( Gravity.CENTER );
				row.addView( code, new LinearLayout.LayoutParams( dp( 64 ), -2 ) );
				return row;
			}
		};

		final ListView list = new ListView( this );
		list.setAdapter( adapter );
		content.addView( list, new LinearLayout.LayoutParams( -1, 0, 1 ) );
		final AlertDialog dialog = new AlertDialog.Builder( this )
			.setTitle( selectingBase ? "Choose base currency" : "Add currency" )
			.setView( content )
			.setNegativeButton( "Cancel", null )
			.create();

		final Runnable refreshList = new Runnable() {
			@Override
			public void run() {
				final String query = search.getText().toString().trim().toLowerCase();
				visibleCurrencies.clear();
				for ( final CurrencyCatalog.CurrencyInfo currency : currencies ) {
					final String currencyCode = currency.getCode();
					if ( selectingBase ) {
						if ( targetCodes.contains( currencyCode ) ) {
							continue;
						}
					} else {
						if ( baseCode.equals( currencyCode ) ) {
							continue;
						}
						if ( targetCodes.contains( currencyCode )
							&& ( targetIndex < 0 || !currencyCode.equals( targetCodes.get( targetIndex ) ) ) ) {
							continue;
						}
					}
					if ( query.length() == 0 || currencyCode.toLowerCase().contains( query ) || currency.getName().toLowerCase().contains( query ) ) {
						visibleCurrencies.add( currency );
					}
				}
				adapter.notifyDataSetChanged();
			}
		};
		refreshList.run();
		search.addTextChangedListener( new TextWatcher() {
			@Override
			public void beforeTextChanged( final CharSequence text, final int start, final int count, final int after ) {
			}

			@Override
			public void onTextChanged( final CharSequence text, final int start, final int before, final int count ) {
				refreshList.run();
			}

			@Override
			public void afterTextChanged( final Editable text ) {
			}
		} );
		list.setOnItemClickListener( new android.widget.AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick( final android.widget.AdapterView<?> parent, final View view, final int position, final long id ) {
				final CurrencyCatalog.CurrencyInfo chosenCurrency = ( CurrencyCatalog.CurrencyInfo ) adapter.getItem( position );
				final String chosen = chosenCurrency.getCode();
				if ( selectingBase ) {
					baseCode = chosen;
					updateCurrencyRow( baseSelector, chosen );
				} else if ( targetIndex >= 0 ) {
					targetCodes.set( targetIndex, chosen );
				} else if ( targetCodes.size() < PreferencesStore.MAX_TARGETS && !targetCodes.contains( chosen ) ) {
					targetCodes.add( chosen );
				}
				renderTargets();
				dialog.dismiss();
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

	private void saveConfiguration() {
		final String baseCurrency = baseCode;
		final List<String> targets = new ArrayList<>();
		final Set<String> selectedTargets = new HashSet<>();
		for ( final String target : targetCodes ) {
			if ( target == null || target.length() == 0 ) {
				continue;
			}
			if ( baseCurrency.equals( target ) ) {
				showMessage( "A target must be different from the base currency." );
				return;
			}
			if ( !selectedTargets.add( target ) ) {
				showMessage( "Each target currency can only be selected once." );
				return;
			}
			targets.add( target );
		}
		if ( targets.isEmpty() ) {
			showMessage( "Select at least one target currency." );
			return;
		}
		final List<String> storedTargets = new ArrayList<>( targets );
		while ( storedTargets.size() < PreferencesStore.MAX_TARGETS ) {
			storedTargets.add( "" );
		}
		final PreferencesStore.WidgetConfiguration configuration = new PreferencesStore.WidgetConfiguration( widgetId, baseCurrency, storedTargets );
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
		content.setPadding( dp( 24 ), dp( 20 ), dp( 24 ), dp( 28 ) );
		content.setBackgroundColor( backgroundColor );
		return content;
	}

	private TextView createSectionLabel( final String text ) {
		final TextView label = createText( text, 12, secondaryTextColor );
		label.setLetterSpacing( 0.08f );
		label.setPadding( 0, 0, 0, dp( 8 ) );
		return label;
	}

	private TextView createText( final String text, final int size, final int color ) {
		final TextView view = new TextView( this );
		view.setText( text );
		view.setTextSize( size );
		view.setTextColor( color );
		return view;
	}

	private GradientDrawable createFieldBackground() {
		final GradientDrawable background = new GradientDrawable();
		background.setColor( surfaceColor );
		background.setCornerRadius( dp( 12 ) );
		background.setStroke( dp( 1 ), fieldStrokeColor );
		return background;
	}

	private GradientDrawable createDashedLikeBackground() {
		final GradientDrawable background = createFieldBackground();
		background.setStroke( dp( 1 ), fieldStrokeColor, dp( 5 ), dp( 4 ) );
		return background;
	}

	private GradientDrawable createBadgeBackground() {
		final GradientDrawable background = new GradientDrawable();
		background.setColor( surfaceColor );
		background.setCornerRadius( dp( 8 ) );
		background.setStroke( dp( 1 ), fieldStrokeColor );
		return background;
	}

	private GradientDrawable createAccentBackground() {
		final GradientDrawable background = new GradientDrawable();
		background.setColor( accentColor );
		background.setCornerRadius( dp( 10 ) );
		return background;
	}

	private GradientDrawable createBackButtonBackground() {
		final GradientDrawable background = new GradientDrawable();
		background.setColor( surfaceColor );
		background.setCornerRadius( dp( 20 ) );
		background.setStroke( dp( 1 ), fieldStrokeColor );
		return background;
	}

	private GradientDrawable createSearchBackground() {
		final GradientDrawable background = new GradientDrawable();
		background.setColor( surfaceColor );
		background.setCornerRadius( dp( 10 ) );
		background.setStroke( dp( 1 ), fieldStrokeColor );
		return background;
	}

	private void updateCurrencyRow( final LinearLayout row, final String code ) {
		final CurrencyCatalog.CurrencyInfo currency = CurrencyCatalog.find( code );
		final int textIndex = row.getChildCount() == 3 ? 1 : 0;
		final LinearLayout text = ( LinearLayout ) row.getChildAt( textIndex );
		final TextView name = ( TextView ) text.getChildAt( 0 );
		final TextView codeView = ( TextView ) text.getChildAt( 1 );
		name.setText( currency.getName() );
		codeView.setText( currency.getCode() );
	}

	private LinearLayout.LayoutParams withTopMargin( final int margin ) {
		final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams( -1, -2 );
		params.topMargin = margin;
		return params;
	}

	private LinearLayout.LayoutParams withBottomMargin( final int margin ) {
		final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams( -1, -2 );
		params.bottomMargin = margin;
		return params;
	}

	private LinearLayout.LayoutParams withTopBottomMargin( final int top, final int bottom ) {
		final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams( -1, -2 );
		params.topMargin = top;
		params.bottomMargin = bottom;
		return params;
	}

	private int dp( final int value ) {
		return Math.round( value * getResources().getDisplayMetrics().density );
	}

	private boolean isLightColor( final int color ) {
		final double luminance = ( 0.299 * android.graphics.Color.red( color ) )
			+ ( 0.587 * android.graphics.Color.green( color ) )
			+ ( 0.114 * android.graphics.Color.blue( color ) );
		return luminance > 170;
	}

	private void showMessage( final String message ) {
		Toast.makeText( this, message, Toast.LENGTH_SHORT ).show();
	}
}
