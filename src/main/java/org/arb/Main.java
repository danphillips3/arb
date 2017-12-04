package org.arb;

import org.arb.strategy.*;
import java.util.HashMap;

import org.knowm.xchange.bitstamp.BitstampExchange;
import org.knowm.xchange.coinbase.CoinbaseExchange;
import org.knowm.xchange.gdax.GDAXExchange;
import org.knowm.xchange.bitbay.BitbayExchange;
import org.knowm.xchange.kraken.KrakenExchange;
import org.knowm.xchange.currency.CurrencyPair;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class Main {
	public static void main(String[] args) {
		boolean isRecording = false;
		boolean isReplay = true;
		String path = "/Users/daniel/Documents/xchange-record/record-test.txt";
		
		CurrencyPair[] currencyPairs = {
			CurrencyPair.BTC_USD
		};
		
		HashMap<String, String> exchangeNameTypeMap = new HashMap<String, String>();
		exchangeNameTypeMap.put("Bitstamp", BitbayExchange.class.getName());
		exchangeNameTypeMap.put("GDAX", GDAXExchange.class.getName());
		exchangeNameTypeMap.put("Bitbay", BitbayExchange.class.getName());
		exchangeNameTypeMap.put("Kraken", KrakenExchange.class.getName());
		
		Strategy strategy = new SimpleStrategy();
		
		Logger root = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		root.setLevel(Level.OFF);
		
		Arb arb = new Arb(currencyPairs, exchangeNameTypeMap, strategy);
		
		if (isReplay) {
			arb.startReplay(path);
		} else {
			if (isRecording) {
				arb.initRecording(path);
			}
			arb.startLive();
		}
	}

}
