package com.ziwei.ratedisplay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

public final class CurrencyCatalog {
	private static final String[] SUPPORTED_CODES = {
		"AED", "AFN", "ALL", "AMD", "ANG", "AOA", "ARS", "AUD", "AWG", "AZN",
		"BAM", "BBD", "BDT", "BGN", "BHD", "BIF", "BMD", "BND", "BOB", "BRL",
		"BSD", "BTN", "BWP", "BYN", "BZD", "CAD", "CDF", "CHF", "CLF", "CLP",
		"CNH", "CNY", "COP", "CRC", "CUP", "CVE", "CZK", "DJF", "DKK", "DOP",
		"DZD", "EGP", "ERN", "ETB", "EUR", "FJD", "FKP", "FOK", "GBP", "GEL",
		"GGP", "GHS", "GIP", "GMD", "GNF", "GTQ", "GYD", "HKD", "HNL", "HRK",
		"HTG", "HUF", "IDR", "ILS", "IMP", "INR", "IQD", "IRR", "ISK", "JEP",
		"JMD", "JOD", "JPY", "KES", "KGS", "KHR", "KID", "KMF", "KRW", "KWD",
		"KYD", "KZT", "LAK", "LBP", "LKR", "LRD", "LSL", "LYD", "MAD", "MDL",
		"MGA", "MKD", "MMK", "MNT", "MOP", "MRU", "MUR", "MVR", "MWK", "MXN",
		"MYR", "MZN", "NAD", "NGN", "NIO", "NOK", "NPR", "NZD", "OMR", "PAB",
		"PEN", "PGK", "PHP", "PKR", "PLN", "PYG", "QAR", "RON", "RSD", "RUB",
		"RWF", "SAR", "SBD", "SCR", "SDG", "SEK", "SGD", "SHP", "SLE", "SLL",
		"SOS", "SRD", "SSP", "STN", "SYP", "SZL", "THB", "TJS", "TMT", "TND",
		"TOP", "TRY", "TTD", "TVD", "TWD", "TZS", "UAH", "UGX", "USD", "UYU",
		"UZS", "VES", "VND", "VUV", "WST", "XAF", "XCD", "XCG", "XDR", "XOF",
		"XPF", "YER", "ZAR", "ZMW", "ZWG", "ZWL"
	};

	private CurrencyCatalog() {
	}

	public static List<CurrencyInfo> getCurrencies() {
		final List<CurrencyInfo> currencies = new ArrayList<>();
		for ( final String code : SUPPORTED_CODES ) {
			currencies.add( new CurrencyInfo( code ) );
		}
		return Collections.unmodifiableList( currencies );
	}

	public static CurrencyInfo find( final String code ) {
		for ( final CurrencyInfo currency : getCurrencies() ) {
			if ( currency.getCode().equals( code ) ) {
				return currency;
			}
		}
		return new CurrencyInfo( code );
	}

	public static final class CurrencyInfo {
		private final String code;
		private final String name;
		private final String symbol;
		private final int fractionDigits;

		CurrencyInfo( final String code ) {
			this.code = code;
			Currency currency = null;
			try {
				currency = Currency.getInstance( code );
			} catch ( final IllegalArgumentException ignored ) {
				// Some provider-supported regional codes are not in Android's ISO catalogue.
			}

			if ( currency == null ) {
				this.name           = code + " (provider currency)";
				this.symbol         = code;
				this.fractionDigits = 2;
			} else {
				this.name           = currency.getDisplayName( Locale.ENGLISH );
				this.symbol         = currency.getSymbol( Locale.US );
				this.fractionDigits = currency.getDefaultFractionDigits();
			}
		}

		public String getCode() {
			return code;
		}

		public String getName() {
			return name;
		}

		public String getSymbol() {
			return symbol;
		}

		public int getFractionDigits() {
			return fractionDigits;
		}

		public String getLabel() {
			return name + " (" + code + ")";
		}
	}
}
