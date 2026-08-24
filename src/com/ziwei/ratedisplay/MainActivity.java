package com.ziwei.ratedisplay;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ClipData;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.List;

public class MainActivity extends Activity {
	public static final String EXTRA_WIDGET_ID = "appWidgetId";
	public static final String EXTRA_PAIR_INDEX = "pairIndex";
	private static final int INVALID_WIDGET_ID = AppWidgetManager.INVALID_APPWIDGET_ID;
	private final List<CurrencyCatalog.CurrencyInfo> currencies = CurrencyCatalog.getCurrencies();
	private final List<PreferencesStore.ConversionPair> pairs = new ArrayList<>();
	private int widgetId = INVALID_WIDGET_ID;
	private LinearLayout pairList;
	private TextView pairCount;
	private TextView addConversion;
	private int primaryTextColor;
	private int secondaryTextColor;
	private int surfaceColor;
	private int fieldStrokeColor;
	private int backgroundColor;
	private int accentColor;
	private String formBaseCode;
	private String formTargetCode;
	private int editingPairIndex = -1;
	private boolean isConversionFormVisible;
	private boolean hasUnsavedPairListChanges;

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
		handlePairEditIntent( getIntent() );
	}

	@Override
	protected void onStop() {
		super.onStop();
		if ( !isConversionFormVisible && hasUnsavedPairListChanges ) {
			autoSavePairList();
		}
	}

	@Override
	protected void onNewIntent( final Intent intent ) {
		super.onNewIntent( intent );
		setIntent( intent );
		handlePairEditIntent( intent );
	}

	private void handlePairEditIntent( final Intent intent ) {
		final int requestedPairIndex = intent == null ? -1 : intent.getIntExtra( EXTRA_PAIR_INDEX, -1 );
		if ( widgetId != INVALID_WIDGET_ID && requestedPairIndex >= 0 ) {
			showEditConversionScreen( requestedPairIndex );
		}
	}

	private void buildConfigurationScreen() {
		isConversionFormVisible = false;
		final PreferencesStore.WidgetConfiguration configuration = widgetId == INVALID_WIDGET_ID
			? PreferencesStore.loadDefaultConfiguration( this )
			: PreferencesStore.loadConfiguration( this, widgetId );
		pairs.clear();
		pairs.addAll( configuration.getPairs() );

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
		final TextView description = createText( "Add up to fifteen independent currency conversion pairs to this widget.", 14, secondaryTextColor );
		description.setGravity( Gravity.CENTER );
		description.setPadding( dp( 16 ), dp( 4 ), dp( 16 ), dp( 24 ) );
		content.addView( description );

		final LinearLayout header = new LinearLayout( this );
		header.setGravity( Gravity.CENTER_VERTICAL );
		header.addView( createSectionLabel( "CONVERSION PAIRS" ), new LinearLayout.LayoutParams( 0, -2, 1 ) );
		pairCount = createCountBadge();
		header.addView( pairCount );
		content.addView( header );
		pairList = new LinearLayout( this );
		pairList.setOrientation( LinearLayout.VERTICAL );
		content.addView( pairList );
		addConversion = createAddConversionControl();
		content.addView( addConversion, withTopBottomMargin( dp( 12 ), dp( 24 ) ) );
		content.addView( createSaveButton() );
		final TextView helper = createText( "↻  Widget will refresh after saving.", 13, secondaryTextColor );
		helper.setGravity( Gravity.CENTER );
		helper.setPadding( 0, dp( 8 ), 0, 0 );
		content.addView( helper );
		setContentView( scroll );
		renderPairs();
	}

	private void renderPairs() {
		pairList.removeAllViews();
		for ( int index = 0; index < pairs.size(); index++ ) {
			final int pairIndex = index;
			final PreferencesStore.ConversionPair pair = pairs.get( index );
			final LinearLayout row = createPairRow( pair, index );
			final TextView handle = ( TextView ) row.getChildAt( 0 );
			handle.setOnLongClickListener( new View.OnLongClickListener() {
				@Override
				public boolean onLongClick( final View view ) {
					final ClipData data = ClipData.newPlainText( "pair-index", String.valueOf( pairIndex ) );
					final View.DragShadowBuilder shadow = new View.DragShadowBuilder( row ) {
						@Override
						public void onProvideShadowMetrics( final Point shadowSize, final Point shadowTouchPoint ) {
							shadowSize.set( row.getMeasuredWidth(), row.getMeasuredHeight() );
							shadowTouchPoint.set( view.getMeasuredWidth() / 2, row.getMeasuredHeight() / 2 );
						}
					};
					return view.startDragAndDrop( data, shadow, null, 0 );
				}
			} );
			row.setOnDragListener( new View.OnDragListener() {
				@Override
				public boolean onDrag( final View view, final DragEvent event ) {
					if ( event.getAction() == DragEvent.ACTION_DROP && event.getClipData() != null && event.getClipData().getItemCount() > 0 ) {
						try {
							movePair( Integer.parseInt( event.getClipData().getItemAt( 0 ).getText().toString() ), pairIndex );
							return true;
						} catch ( final NumberFormatException ignored ) {
							return false;
						}
					}
					return event.getAction() == DragEvent.ACTION_DRAG_STARTED || event.getAction() == DragEvent.ACTION_DRAG_ENDED;
				}
			} );
			row.setOnClickListener( new View.OnClickListener() {
				@Override
				public void onClick( final View view ) {
					showEditConversionScreen( pairIndex );
				}
			} );
			final TextView edit = ( TextView ) row.getChildAt( row.getChildCount() - 2 );
			edit.setOnClickListener( new View.OnClickListener() {
				@Override
				public void onClick( final View view ) {
					showEditConversionScreen( pairIndex );
				}
			} );
			final TextView remove = ( TextView ) row.getChildAt( row.getChildCount() - 1 );
			remove.setOnClickListener( new View.OnClickListener() {
				@Override
				public void onClick( final View view ) {
					pairs.remove( pairIndex );
					hasUnsavedPairListChanges = true;
					renderPairs();
				}
			} );
			pairList.addView( row, withTopMargin( index == 0 ? dp( 8 ) : dp( 6 ) ) );
		}
		pairCount.setText( pairs.size() + " / " + PreferencesStore.MAX_PAIRS );
		addConversion.setVisibility( pairs.size() < PreferencesStore.MAX_PAIRS ? View.VISIBLE : View.GONE );
	}

	private LinearLayout createPairRow( final PreferencesStore.ConversionPair pair, final int index ) {
		final LinearLayout row = new LinearLayout( this );
		row.setGravity( Gravity.CENTER_VERTICAL );
		row.setPadding( dp( 12 ), dp( 10 ), dp( 8 ), dp( 10 ) );
		row.setMinimumHeight( dp( 68 ) );
		row.setBackground( createFieldBackground() );
		final TextView handle = createText( "≡", 24, secondaryTextColor );
		handle.setGravity( Gravity.CENTER );
		handle.setContentDescription( "Long-press and drag to reorder conversion" );
		row.addView( handle, new LinearLayout.LayoutParams( dp( 36 ), -1 ) );
		final LinearLayout text = new LinearLayout( this );
		text.setOrientation( LinearLayout.VERTICAL );
		text.setGravity( Gravity.CENTER_VERTICAL );
		final CurrencyCatalog.CurrencyInfo base = CurrencyCatalog.find( pair.getBaseCurrency() );
		final CurrencyCatalog.CurrencyInfo target = CurrencyCatalog.find( pair.getTargetCurrency() );
		text.addView( createText( base.getCode() + "  →  " + target.getCode(), 16, primaryTextColor ) );
		final String cachedRate = widgetId == INVALID_WIDGET_ID ? null : PreferencesStore.getCachedRate( this, widgetId, pair.getBaseCurrency(), pair.getTargetCurrency() );
		text.addView( createText( "1 " + base.getCode() + " = " + ( cachedRate == null ? "—" : cachedRate ) + " " + target.getCode(), 13, secondaryTextColor ) );
		row.addView( text, new LinearLayout.LayoutParams( 0, -2, 1 ) );
		final TextView edit = createText( "✎", 23, secondaryTextColor );
		edit.setGravity( Gravity.CENTER );
		edit.setContentDescription( "Edit conversion " + ( index + 1 ) );
		row.addView( edit, new LinearLayout.LayoutParams( dp( 40 ), dp( 48 ) ) );
		final TextView remove = createText( "×", 28, secondaryTextColor );
		remove.setGravity( Gravity.CENTER );
		remove.setContentDescription( "Remove conversion " + ( index + 1 ) );
		row.addView( remove, new LinearLayout.LayoutParams( dp( 40 ), dp( 48 ) ) );
		return row;
	}

	private void movePair( final int sourceIndex, final int targetIndex ) {
		if ( sourceIndex < 0 || sourceIndex >= pairs.size() || sourceIndex == targetIndex ) {
			return;
		}
		final PreferencesStore.ConversionPair moved = pairs.remove( sourceIndex );
		pairs.add( Math.min( targetIndex, pairs.size() ), moved );
		hasUnsavedPairListChanges = true;
		renderPairs();
	}

	private TextView createAddConversionControl() {
		final TextView add = createText( "+  Add conversion", 16, primaryTextColor );
		add.setGravity( Gravity.CENTER );
		add.setMinHeight( dp( 56 ) );
		add.setBackground( createDashedLikeBackground() );
		add.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick( final View view ) {
				if ( pairs.size() < PreferencesStore.MAX_PAIRS ) {
					showAddConversionScreen();
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
		final TextView badge = createText( "0 / " + PreferencesStore.MAX_PAIRS, 13, primaryTextColor );
		badge.setGravity( Gravity.CENTER );
		badge.setPadding( dp( 10 ), dp( 5 ), dp( 10 ), dp( 5 ) );
		badge.setBackground( createBadgeBackground() );
		return badge;
	}

	private void showAddConversionScreen() {
		editingPairIndex = -1;
		formBaseCode = currencies.get( 0 ).getCode();
		formTargetCode = currencies.get( 1 ).getCode();
		buildConversionFormScreen( false );
	}

	private void showEditConversionScreen( final int pairIndex ) {
		if ( pairIndex < 0 || pairIndex >= pairs.size() ) {
			return;
		}
		editingPairIndex = pairIndex;
		final PreferencesStore.ConversionPair pair = pairs.get( pairIndex );
		formBaseCode = pair.getBaseCurrency();
		formTargetCode = pair.getTargetCurrency();
		buildConversionFormScreen( true );
	}

	private void buildConversionFormScreen( final boolean editing ) {
		isConversionFormVisible = true;
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
		back.setPadding( 0, 0, 0, 0 );
		back.setBackground( createBackButtonBackground() );
		back.setContentDescription( "Back" );
		back.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick( final View view ) {
				buildConfigurationScreen();
			}
		} );
		backRow.addView( back, new LinearLayout.LayoutParams( dp( 52 ), dp( 40 ) ) );
		content.addView( backRow );

		final TextView title = createText( editing ? "Edit conversion" : "Add conversion", 22, primaryTextColor );
		title.setGravity( Gravity.CENTER );
		content.addView( title, new LinearLayout.LayoutParams( -1, dp( 54 ) ) );
		final TextView description = createText( "Choose the currencies for this conversion.", 14, secondaryTextColor );
		description.setGravity( Gravity.CENTER );
		description.setPadding( 0, 0, 0, dp( 28 ) );
		content.addView( description );

		final LinearLayout baseField = createCurrencyField( "BASE CURRENCY", formBaseCode, true );
		content.addView( baseField, withTopBottomMargin( 0, dp( 12 ) ) );
		final ImageButton swap = new ImageButton( this );
		swap.setImageResource( R.drawable.ic_swap );
		swap.setColorFilter( accentColor, PorterDuff.Mode.SRC_IN );
		swap.setBackground( createBackButtonBackground() );
		swap.setContentDescription( "Swap base and target currencies" );
		swap.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick( final View view ) {
				final String oldBase = formBaseCode;
				formBaseCode = formTargetCode;
				formTargetCode = oldBase;
				buildConversionFormScreen( editing );
			}
		} );
		final LinearLayout.LayoutParams swapParams = new LinearLayout.LayoutParams( dp( 48 ), dp( 48 ) );
		swapParams.gravity = Gravity.CENTER_HORIZONTAL;
		content.addView( swap, swapParams );
		final LinearLayout targetField = createCurrencyField( "TARGET CURRENCY", formTargetCode, false );
		content.addView( targetField, withTopBottomMargin( dp( 12 ), dp( 36 ) ) );

		final LinearLayout actions = new LinearLayout( this );
		actions.setGravity( Gravity.CENTER_VERTICAL );
		final Button cancel = new Button( this );
		cancel.setText( "Cancel" );
		cancel.setAllCaps( false );
		cancel.setTextSize( 16 );
		cancel.setTextColor( primaryTextColor );
		cancel.setMinHeight( dp( 52 ) );
		cancel.setBackground( createSecondaryButtonBackground() );
		cancel.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick( final View view ) {
				buildConfigurationScreen();
			}
		} );
		final LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams( 0, dp( 52 ), 1 );
		cancelParams.rightMargin = dp( 4 );
		actions.addView( cancel, cancelParams );
		final Button commit = new Button( this );
		commit.setText( editing ? "Save changes" : "Add conversion" );
		commit.setAllCaps( false );
		commit.setTextSize( 16 );
		commit.setTextColor( isLightColor( accentColor ) ? 0xFF241B35 : 0xFFFFFFFF );
		commit.setBackground( createAccentBackground() );
		commit.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick( final View view ) {
				commitConversionForm();
			}
		} );
		final LinearLayout.LayoutParams commitParams = new LinearLayout.LayoutParams( 0, dp( 52 ), 1 );
		commitParams.leftMargin = dp( 4 );
		actions.addView( commit, commitParams );
		content.addView( actions );
		setContentView( scroll );
	}

	private LinearLayout createCurrencyField( final String label, final String code, final boolean selectingBase ) {
		final LinearLayout field = new LinearLayout( this );
		field.setGravity( Gravity.CENTER_VERTICAL );
		field.setPadding( dp( 16 ), dp( 8 ), dp( 12 ), dp( 8 ) );
		field.setBackground( createFieldBackground() );
		final LinearLayout text = new LinearLayout( this );
		text.setOrientation( LinearLayout.VERTICAL );
		text.addView( createText( label, 11, secondaryTextColor ) );
		final CurrencyCatalog.CurrencyInfo currency = CurrencyCatalog.find( code );
		text.addView( createText( currency.getName() + "  (" + code + ")", 16, primaryTextColor ) );
		field.addView( text, new LinearLayout.LayoutParams( 0, -2, 1 ) );
		final TextView arrow = createText( "›", 28, secondaryTextColor );
		arrow.setGravity( Gravity.CENTER );
		field.addView( arrow, new LinearLayout.LayoutParams( dp( 32 ), -1 ) );
		field.setContentDescription( "Choose " + ( selectingBase ? "base" : "target" ) + " currency" );
		field.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick( final View view ) {
				showCurrencyPicker( selectingBase );
			}
		} );
		return field;
	}

	private void showCurrencyPicker( final boolean selectingBase ) {
		final LinearLayout content = new LinearLayout( this );
		content.setOrientation( LinearLayout.VERTICAL );
		content.setPadding( dp( 20 ), 0, dp( 20 ), 0 );
		final EditText search = new EditText( this );
		search.setHint( "Search by name or code..." );
		search.setSingleLine( true );
		search.setInputType( InputType.TYPE_CLASS_TEXT );
		content.addView( search, withTopBottomMargin( dp( 12 ), dp( 12 ) ) );
		final List<CurrencyCatalog.CurrencyInfo> visibleCurrencies = new ArrayList<>();
		final BaseAdapter adapter = new BaseAdapter() {
			@Override public int getCount() { return visibleCurrencies.size(); }
			@Override public CurrencyCatalog.CurrencyInfo getItem( final int position ) { return visibleCurrencies.get( position ); }
			@Override public long getItemId( final int position ) { return position; }
			@Override public View getView( final int position, final View convertView, final ViewGroup parent ) {
				final CurrencyCatalog.CurrencyInfo currency = getItem( position );
				final LinearLayout row = new LinearLayout( MainActivity.this );
				row.setGravity( Gravity.CENTER_VERTICAL );
				row.setPadding( dp( 8 ), dp( 10 ), dp( 8 ), dp( 10 ) );
				row.addView( createText( currency.getName(), 16, primaryTextColor ), new LinearLayout.LayoutParams( 0, -2, 1 ) );
				row.addView( createText( currency.getCode(), 14, secondaryTextColor ), new LinearLayout.LayoutParams( dp( 64 ), -2 ) );
				return row;
			}
		};
		final ListView list = new ListView( this );
		list.setAdapter( adapter );
		content.addView( list, new LinearLayout.LayoutParams( -1, 0, 1 ) );
		final AlertDialog dialog = new AlertDialog.Builder( this ).setTitle( selectingBase ? "Choose base currency" : "Choose target currency" ).setView( content ).setNegativeButton( "Cancel", null ).create();
		final Runnable refreshList = new Runnable() {
			@Override public void run() {
				final String query = search.getText().toString().trim().toLowerCase();
				visibleCurrencies.clear();
				for ( final CurrencyCatalog.CurrencyInfo currency : currencies ) {
					if ( query.length() == 0 || currency.getCode().toLowerCase().contains( query ) || currency.getName().toLowerCase().contains( query ) ) {
						visibleCurrencies.add( currency );
					}
				}
				adapter.notifyDataSetChanged();
			}
		};
		refreshList.run();
		search.addTextChangedListener( new TextWatcher() {
			@Override public void beforeTextChanged( final CharSequence text, final int start, final int count, final int after ) { }
			@Override public void onTextChanged( final CharSequence text, final int start, final int before, final int count ) { refreshList.run(); }
			@Override public void afterTextChanged( final Editable text ) { }
		} );
		list.setOnItemClickListener( new android.widget.AdapterView.OnItemClickListener() {
			@Override public void onItemClick( final android.widget.AdapterView<?> parent, final View view, final int position, final long id ) {
				final String chosen = ( ( CurrencyCatalog.CurrencyInfo ) adapter.getItem( position ) ).getCode();
				if ( selectingBase ) { formBaseCode = chosen; } else { formTargetCode = chosen; }
				dialog.dismiss();
				buildConversionFormScreen( editingPairIndex >= 0 );
			}
		} );
		dialog.show();
	}

	private void commitConversionForm() {
		if ( formBaseCode.equals( formTargetCode ) ) {
			showMessage( "A target must be different from its base currency." );
			return;
		}
		final PreferencesStore.ConversionPair selected = new PreferencesStore.ConversionPair( formBaseCode, formTargetCode );
		for ( int index = 0; index < pairs.size(); index++ ) {
			if ( index != editingPairIndex && selected.equals( pairs.get( index ) ) ) {
				showMessage( "Each conversion pair can only be selected once." );
				return;
			}
		}
		if ( editingPairIndex >= 0 ) {
			pairs.set( editingPairIndex, selected );
		} else {
			pairs.add( selected );
		}
		final PreferencesStore.WidgetConfiguration configuration = new PreferencesStore.WidgetConfiguration( widgetId, pairs );
		if ( widgetId == INVALID_WIDGET_ID ) {
			PreferencesStore.saveDefaultConfiguration( this, configuration );
		} else {
			PreferencesStore.saveConfiguration( this, configuration );
			RateWidgetProvider.refreshWidget( this, widgetId );
		}
		hasUnsavedPairListChanges = false;
		buildConfigurationScreen();
	}

	private void saveConfiguration() {
		if ( !isPairListValid( true ) ) {
			return;
		}
		persistPairList();
		if ( widgetId == INVALID_WIDGET_ID ) {
			Toast.makeText( this, "Defaults saved for new widgets", Toast.LENGTH_SHORT ).show();
		} else {
			setResult( RESULT_OK, getResultIntent() );
			Toast.makeText( this, "Currency widget updated", Toast.LENGTH_SHORT ).show();
		}
		finish();
	}

	private void autoSavePairList() {
		if ( !isPairListValid( false ) ) {
			return;
		}
		persistPairList();
	}

	private boolean isPairListValid( final boolean showErrors ) {
		if ( pairs.isEmpty() ) {
			if ( showErrors ) {
				showMessage( "Add at least one conversion pair." );
			}
			return false;
		}
		for ( int index = 0; index < pairs.size(); index++ ) {
			final PreferencesStore.ConversionPair pair = pairs.get( index );
			if ( pair.getBaseCurrency().equals( pair.getTargetCurrency() ) ) {
				if ( showErrors ) {
					showMessage( "A target must be different from its base currency." );
				}
				return false;
			}
			for ( int other = index + 1; other < pairs.size(); other++ ) {
				if ( pair.equals( pairs.get( other ) ) ) {
					if ( showErrors ) {
						showMessage( "Each conversion pair can only be selected once." );
					}
					return false;
				}
			}
		}
		return true;
	}

	private void persistPairList() {
		final PreferencesStore.WidgetConfiguration configuration = new PreferencesStore.WidgetConfiguration( widgetId, pairs );
		if ( widgetId == INVALID_WIDGET_ID ) {
			PreferencesStore.saveDefaultConfiguration( this, configuration );
		} else {
			PreferencesStore.saveConfiguration( this, configuration );
			RateWidgetProvider.refreshWidget( this, widgetId );
		}
		hasUnsavedPairListChanges = false;
	}

	private Intent getResultIntent() { final Intent result = new Intent(); result.putExtra( EXTRA_WIDGET_ID, widgetId ); return result; }
	private LinearLayout createColumn() { final LinearLayout content = new LinearLayout( this ); content.setOrientation( LinearLayout.VERTICAL ); content.setPadding( dp( 24 ), dp( 20 ), dp( 24 ), dp( 28 ) ); content.setBackgroundColor( backgroundColor ); return content; }
	private TextView createSectionLabel( final String text ) { final TextView label = createText( text, 12, secondaryTextColor ); label.setLetterSpacing( 0.08f ); label.setPadding( 0, 0, 0, dp( 8 ) ); return label; }
	private TextView createText( final String text, final int size, final int color ) { final TextView view = new TextView( this ); view.setText( text ); view.setTextSize( size ); view.setTextColor( color ); return view; }
	private GradientDrawable createFieldBackground() { final GradientDrawable background = new GradientDrawable(); background.setColor( surfaceColor ); background.setCornerRadius( dp( 12 ) ); background.setStroke( dp( 1 ), fieldStrokeColor ); return background; }
	private GradientDrawable createDashedLikeBackground() { final GradientDrawable background = createFieldBackground(); background.setStroke( dp( 1 ), fieldStrokeColor, dp( 5 ), dp( 4 ) ); return background; }
	private GradientDrawable createBadgeBackground() { final GradientDrawable background = new GradientDrawable(); background.setColor( surfaceColor ); background.setCornerRadius( dp( 8 ) ); background.setStroke( dp( 1 ), fieldStrokeColor ); return background; }
	private GradientDrawable createAccentBackground() { final GradientDrawable background = new GradientDrawable(); background.setColor( accentColor ); background.setCornerRadius( dp( 10 ) ); return background; }
	private GradientDrawable createSecondaryButtonBackground() { final GradientDrawable background = new GradientDrawable(); background.setColor( surfaceColor ); background.setCornerRadius( dp( 10 ) ); background.setStroke( dp( 1 ), fieldStrokeColor ); return background; }
	private GradientDrawable createBackButtonBackground() { final GradientDrawable background = new GradientDrawable(); background.setColor( surfaceColor ); background.setCornerRadius( dp( 20 ) ); background.setStroke( dp( 1 ), fieldStrokeColor ); return background; }
	private GradientDrawable createSearchBackground() { final GradientDrawable background = new GradientDrawable(); background.setColor( surfaceColor ); background.setCornerRadius( dp( 10 ) ); background.setStroke( dp( 1 ), fieldStrokeColor ); return background; }
	private LinearLayout.LayoutParams withTopMargin( final int margin ) { final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams( -1, -2 ); params.topMargin = margin; return params; }
	private LinearLayout.LayoutParams withTopBottomMargin( final int top, final int bottom ) { final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams( -1, -2 ); params.topMargin = top; params.bottomMargin = bottom; return params; }
	private int dp( final int value ) { return Math.round( value * getResources().getDisplayMetrics().density ); }
	private boolean isLightColor( final int color ) { final double luminance = ( 0.299 * android.graphics.Color.red( color ) ) + ( 0.587 * android.graphics.Color.green( color ) ) + ( 0.114 * android.graphics.Color.blue( color ) ); return luminance > 170; }
	private void showMessage( final String message ) { Toast.makeText( this, message, Toast.LENGTH_SHORT ).show(); }
}
