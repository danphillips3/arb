package org.arb;

import org.arb.strategy.*;

import java.util.ArrayList;
import java.util.HashMap;

import org.knowm.xchange.anx.v2.ANXExchange;
import org.knowm.xchange.bitstamp.BitstampExchange;
import org.knowm.xchange.bittrex.BittrexExchange;
import org.knowm.xchange.coinbase.CoinbaseExchange;
import org.knowm.xchange.gdax.GDAXExchange;
import org.knowm.xchange.gemini.v1.GeminiExchange;
import org.knowm.xchange.itbit.v1.ItBitExchange;
import org.knowm.xchange.bitbay.BitbayExchange;
import org.knowm.xchange.btcc.BTCCExchange;
import org.knowm.xchange.btce.v3.BTCEExchange;
import org.knowm.xchange.kraken.KrakenExchange;
import org.knowm.xchange.okcoin.OkCoinExchange;
import org.knowm.xchange.quadrigacx.QuadrigaCxExchange;
import org.knowm.xchange.taurus.TaurusExchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dsx.DSXExchange;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class Main {
	public static boolean isRecording = false;
	public static boolean isReplay = false;
	public static ArrayList<CurrencyPair> currencyPairs = new ArrayList<CurrencyPair>();
	public static HashMap<String, String> exchangeNameTypeMap = new HashMap<String, String>();
	public static Strategy strategy;
	
	private static HashMap<String, String> exchangeLookupMap;
	
	static {
		exchangeLookupMap = new HashMap<String, String>();
		exchangeLookupMap.put("anx", ANXExchange.class.getName());
		exchangeLookupMap.put("bitbay", BitbayExchange.class.getName());
		exchangeLookupMap.put("bitstamp", BitstampExchange.class.getName());
		exchangeLookupMap.put("btce", BTCEExchange.class.getName());
		exchangeLookupMap.put("dsx", DSXExchange.class.getName());
		exchangeLookupMap.put("gdax", GDAXExchange.class.getName());
		exchangeLookupMap.put("gemini", GeminiExchange.class.getName());
		exchangeLookupMap.put("kraken", KrakenExchange.class.getName());
		exchangeLookupMap.put("taurus", TaurusExchange.class.getName());
		exchangeLookupMap.put("quadrigacx", QuadrigaCxExchange.class.getName());
		
		//disabled -- not working
		//exchangeLookupMap.put("btcc", BTCCExchange.class.getName());
		//exchangeLookupMap.put("coinbase", CoinbaseExchange.class.getName());
		//exchangeLookupMap.put("itbit", ItBitExchange.class.getName());
		//exchangeLookupMap.put("okcoin", OkCoinExchange.class.getName());
		
		//disabled -- no BTC/USD
		//exchangeLookupMap.put("bittrex", BittrexExchange.class.getName());
	}
	
	public static void main(String[] args) {
		Logger root = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		root.setLevel(Level.OFF);
		
		for (int i = 0; i < args.length; i+=2) {
			processArg(args[i], args[i+1]);
		}
		
		if (exchangeNameTypeMap.isEmpty()) {
			System.out.println("No exchanges specified - using all");
			exchangeNameTypeMap = exchangeLookupMap;
		}
		
		Arb arb = new Arb(currencyPairs, exchangeNameTypeMap, strategy);
		
		if (isReplay) {
			arb.startReplay("");
		} else {
			if (isRecording) {
				arb.initRecording();
			}
			arb.startLive();
		}
	}
	
	public static void processArg(String key, String val) {
		System.out.println("Processing arg: " + key + " " + val);
		if (key.equals("--replay")) {
			isReplay = true;
		} else if (key.equals("--record")) {
			isRecording = true;
		} else if (key.equals("--currencies")) {
			String[] split = val.split(",");
			for (String currencyPairRaw : split) {
				String[] currencySplit = currencyPairRaw.split("/");
				currencyPairs.add(new CurrencyPair(new Currency(currencySplit[0]), new Currency(currencySplit[1])));
			}
		} else if (key.equals("--exchanges")) {
			String[] split = val.split(",");
			for (String exchange : split) {
				String exchangeLookupResult = exchangeLookupMap.get(exchange);
				if (exchangeLookupResult == null) {
					exitWithError("unknown exchange " + exchange);
				}
				exchangeNameTypeMap.put(exchange, exchangeLookupResult);
			}
		} else if (key.equals("--strategy")) {
			strategy = getStrategy(val);
		} else {
			exitWithError("unknown arg " + key);
		}
	}
	
	public static void exitWithError(String err) {
		System.out.println("Fatal ERROR: " + err);
		System.exit(1);
	}
	
	public static Strategy getStrategy(String strategyName) {
		if (strategyName.equals("SimpleStrategy")) {
			return new SimpleStrategy();
		} else if (strategyName.equals("SwingingThreshold")) {
			return new SwingingThreshold();
		} else if (strategyName.equals("SpreadStrategy")) {
			return new SpreadStrategy();
		} else {
			System.out.println("Unknown strategy: " + strategyName);
			return null;
		}
	}
}
