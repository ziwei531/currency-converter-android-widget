package com.ziwei.ratedisplay;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
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
    private final List<Spinner> targetSpinners = new ArrayList<>();
    private Spinner baseSpinner;
    private int widgetId = INVALID_WIDGET_ID;

    @Override
    protected void onCreate( final Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        widgetId = getIntent().getIntExtra( EXTRA_WIDGET_ID, INVALID_WIDGET_ID );
        setTitle( "Configure Rate Nori" );
        buildConfigurationScreen();
    }

    private void buildConfigurationScreen() {
        final PreferencesStore.WidgetConfiguration configuration = widgetId == INVALID_WIDGET_ID
            ? PreferencesStore.loadDefaultConfiguration( this )
            : PreferencesStore.loadConfiguration( this, widgetId );

        final ScrollView scroll = new ScrollView( this );
        final LinearLayout content = createColumn();
        scroll.addView( content );

        final TextView title = createText( "Rate Nori", 28, Color.rgb( 36, 27, 53 ) );
        title.setGravity( Gravity.CENTER );
        content.addView( title );

        final TextView description = createText(
            "Choose one base currency and up to five target currencies. The widget will show how much one base unit is worth.",
            16,
            Color.rgb( 111, 98, 128 )
        );
        description.setPadding( 0, 16, 0, 24 );
        content.addView( description );

        content.addView( createLabel( "Base currency" ) );
        baseSpinner = createCurrencySpinner( false );
        baseSpinner.setSelection( findCurrencyPosition( configuration.getBaseCurrency(), false ) );
        content.addView( baseSpinner );

        content.addView( createLabel( "Target currencies" ) );
        for ( int index = 0; index < PreferencesStore.MAX_TARGETS; index++ ) {
            final TextView targetLabel = createText( "Target " + ( index + 1 ), 14, Color.rgb( 111, 98, 128 ) );
            targetLabel.setPadding( 0, 12, 0, 0 );
            content.addView( targetLabel );

            final Spinner targetSpinner = createCurrencySpinner( true );
            targetSpinner.setSelection( findCurrencyPosition( configuration.getTarget( index ), true ) );
            targetSpinners.add( targetSpinner );
            content.addView( targetSpinner );
        }

        final Button saveButton = new Button( this );
        saveButton.setText( widgetId == INVALID_WIDGET_ID ? "Save defaults for new widgets" : "Save and refresh widget" );
        saveButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( final View view ) {
                saveConfiguration();
            }
        } );
        content.addView( saveButton );

        setContentView( scroll );
    }

    private Spinner createCurrencySpinner( final boolean allowEmpty ) {
        final List<String> labels = new ArrayList<>();
        if ( allowEmpty ) {
            labels.add( "Not selected" );
        }
        for ( final CurrencyCatalog.CurrencyInfo currency : currencies ) {
            labels.add( currency.getLabel() );
        }

        final Spinner spinner = new Spinner( this );
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            labels
        );
        adapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
        spinner.setAdapter( adapter );
        return spinner;
    }

    private int findCurrencyPosition( final String code, final boolean allowEmpty ) {
        if ( code == null || code.length() == 0 ) {
            return 0;
        }
        for ( int index = 0; index < currencies.size(); index++ ) {
            if ( currencies.get( index ).getCode().equals( code ) ) {
                return allowEmpty ? index + 1 : index;
            }
        }
        return 0;
    }

    private void saveConfiguration() {
        final int basePosition = baseSpinner.getSelectedItemPosition();
        final String baseCurrency = currencies.get( basePosition ).getCode();
        final List<String> targets = new ArrayList<>();
        final Set<String> selectedTargets = new HashSet<>();

        for ( final Spinner spinner : targetSpinners ) {
            final int position = spinner.getSelectedItemPosition();
            final String target = position == 0 ? "" : currencies.get( position - 1 ).getCode();
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

        final PreferencesStore.WidgetConfiguration configuration = new PreferencesStore.WidgetConfiguration(
            widgetId,
            baseCurrency,
            targets
        );

        if ( widgetId == INVALID_WIDGET_ID ) {
            PreferencesStore.saveDefaultConfiguration( this, configuration );
            Toast.makeText( this, "Defaults saved for new widgets", Toast.LENGTH_SHORT ).show();
        } else {
            PreferencesStore.saveConfiguration( this, configuration );
            RateWidgetProvider.refreshWidget( this, widgetId );
            Toast.makeText( this, "Currency widget updated", Toast.LENGTH_SHORT ).show();
        }
        finish();
    }

    private LinearLayout createColumn() {
        final LinearLayout content = new LinearLayout( this );
        content.setOrientation( LinearLayout.VERTICAL );
        content.setPadding( 32, 32, 32, 32 );
        content.setBackgroundColor( Color.rgb( 237, 231, 246 ) );
        return content;
    }

    private TextView createLabel( final String text ) {
        return createText( text, 18, Color.rgb( 36, 27, 53 ) );
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
}
