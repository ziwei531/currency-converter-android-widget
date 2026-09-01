package com.ziwei.ratedisplay;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class SecureApiKeyStore {
	private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
	private static final String KEY_ALIAS          = "fxratesapi-key-encryption";
	private static final String PREFS             = "rate_secrets";
	private static final String CIPHERTEXT        = "fxrates_api_key";
	private static final String VALUE_SEPARATOR    = ":";
	private static final String FORMAT_PREFIX       = "v1";
	private static final String AAD                 = "com.ziwei.ratedisplay/fxrates-api-key";
	private static final int AUTH_TAG_LENGTH_BITS = 128;

	private SecureApiKeyStore() {
	}

	static String get( final Context context ) {
		final String stored = getPreferences( context ).getString( CIPHERTEXT, null );
		if ( stored == null || stored.length() == 0 ) {
			return null;
		}
		try {
			final String[] parts = stored.split( VALUE_SEPARATOR, -1 );
			if ( parts.length != 3 || !FORMAT_PREFIX.equals( parts[ 0 ] ) ) {
				return null;
			}
			final byte[] iv = Base64.decode( parts[ 1 ], Base64.NO_WRAP );
			final byte[] ciphertext = Base64.decode( parts[ 2 ], Base64.NO_WRAP );
			final Cipher cipher = Cipher.getInstance( "AES/GCM/NoPadding" );
			cipher.init( Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec( AUTH_TAG_LENGTH_BITS, iv ) );
			cipher.updateAAD( AAD.getBytes( StandardCharsets.UTF_8 ) );
			return new String( cipher.doFinal( ciphertext ), StandardCharsets.UTF_8 );
		} catch ( final KeyPermanentlyInvalidatedException error ) {
			clear( context );
			deleteKey();
			return null;
		} catch ( final GeneralSecurityException | IOException | IllegalArgumentException error ) {
			return null;
		}
	}

	static boolean save( final Context context, final String apiKey ) {
		final String trimmedKey = apiKey == null ? "" : apiKey.trim();
		if ( trimmedKey.length() == 0 ) {
			clear( context );
			return true;
		}
		if ( trimmedKey.length() > 512 ) {
			return false;
		}
		for ( int index = 0; index < trimmedKey.length(); index++ ) {
			if ( Character.isISOControl( trimmedKey.charAt( index ) ) ) {
				return false;
			}
		}
		try {
			final Cipher cipher = Cipher.getInstance( "AES/GCM/NoPadding" );
			cipher.init( Cipher.ENCRYPT_MODE, getOrCreateKey() );
			final byte[] iv = cipher.getIV();
			cipher.updateAAD( AAD.getBytes( StandardCharsets.UTF_8 ) );
			final String stored = FORMAT_PREFIX
				+ VALUE_SEPARATOR
				+ Base64.encodeToString( iv, Base64.NO_WRAP )
				+ VALUE_SEPARATOR
				+ Base64.encodeToString( cipher.doFinal( trimmedKey.getBytes( StandardCharsets.UTF_8 ) ), Base64.NO_WRAP );
			return getPreferences( context ).edit().putString( CIPHERTEXT, stored ).commit();
		} catch ( final KeyPermanentlyInvalidatedException error ) {
			clear( context );
			deleteKey();
			return false;
		} catch ( final GeneralSecurityException | IOException error ) {
			return false;
		}
	}

	static void clear( final Context context ) {
		getPreferences( context ).edit().remove( CIPHERTEXT ).apply();
	}

	private static SecretKey getOrCreateKey() throws GeneralSecurityException, IOException {
		final KeyStore keyStore = KeyStore.getInstance( KEYSTORE_PROVIDER );
		keyStore.load( null );
		if ( keyStore.containsAlias( KEY_ALIAS ) ) {
			return ( SecretKey ) keyStore.getKey( KEY_ALIAS, null );
		}
		final KeyGenerator generator = KeyGenerator.getInstance( KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER );
		generator.init( new KeyGenParameterSpec.Builder(
			KEY_ALIAS,
			KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
		)
			.setKeySize( 256 )
			.setBlockModes( KeyProperties.BLOCK_MODE_GCM )
			.setEncryptionPaddings( KeyProperties.ENCRYPTION_PADDING_NONE )
			.setRandomizedEncryptionRequired( true )
			.build() );
		return generator.generateKey();
	}

	private static void deleteKey() {
		try {
			final KeyStore keyStore = KeyStore.getInstance( KEYSTORE_PROVIDER );
			keyStore.load( null );
			if ( keyStore.containsAlias( KEY_ALIAS ) ) {
				keyStore.deleteEntry( KEY_ALIAS );
			}
		} catch ( final GeneralSecurityException | IOException ignored ) {
		}
	}

	private static SharedPreferences getPreferences( final Context context ) {
		return context.getSharedPreferences( PREFS, Context.MODE_PRIVATE );
	}
}
