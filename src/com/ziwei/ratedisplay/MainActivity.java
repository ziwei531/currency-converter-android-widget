package com.ziwei.ratedisplay;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate( final Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setTitle( "USD MYR Currency Converter" );

        final LinearLayout content = new LinearLayout( this );
        content.setOrientation( LinearLayout.VERTICAL );
        content.setGravity( Gravity.CENTER );
        content.setPadding( 48, 48, 48, 48 );
        content.setBackgroundColor( Color.rgb( 237, 231, 246 ) );

        final TextView title = new TextView( this );
        title.setText( "USD → MYR" );
        title.setTextColor( Color.rgb( 36, 27, 53 ) );
        title.setTextSize( 28 );
        title.setGravity( Gravity.CENTER );
        content.addView( title );

        final TextView instructions = new TextView( this );
        instructions.setText( "Add the USD MYR rate widget from your home-screen widget picker." );
        instructions.setTextColor( Color.rgb( 111, 98, 128 ) );
        instructions.setTextSize( 16 );
        instructions.setGravity( Gravity.CENTER );
        instructions.setPadding( 0, 24, 0, 0 );
        content.addView( instructions );

        setContentView( content );
    }
}
