package org.arb;

import org.arb.strategy.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.anx.v2.ANXExchange;
import org.knowm.xchange.bitstamp.BitstampExchange;
import org.knowm.xchange.bittrex.BittrexExchange;
import org.knowm.xchange.cexio.CexIOExchange;
import org.knowm.xchange.coinbase.CoinbaseExchange;
import org.knowm.xchange.gdax.GDAXExchange;
import org.knowm.xchange.gemini.v1.GeminiExchange;
import org.knowm.xchange.itbit.v1.ItBitExchange;
import org.knowm.xchange.bitbay.BitbayExchange;
import org.knowm.xchange.bitfinex.v1.BitfinexExchange;
import org.knowm.xchange.btcc.BTCCExchange;
import org.knowm.xchange.btce.v3.BTCEExchange;
import org.knowm.xchange.kraken.KrakenExchange;
import org.knowm.xchange.okcoin.OkCoinExchange;
import org.knowm.xchange.poloniex.PoloniexExchange;
import org.knowm.xchange.quadrigacx.QuadrigaCxExchange;
import org.knowm.xchange.taurus.TaurusExchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dsx.DSXExchange;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class Main {
	private static RunMode runMode = RunMode.Unknown;
	private static ArrayList<CurrencyPair> currencyPairs = new ArrayList<CurrencyPair>();
	private static HashMap<String, ExchangeSpecification> exchangeNameTypeMap = new HashMap<String, ExchangeSpecification>();
	private static Strategy strategy;
	private static Date start = Date.from(Instant.now());
	private static int hours = 0;
	private static Date end = Date.from(Instant.now().plusSeconds(12 * 60 * 60));
	private static boolean pauseEnabled = true;
	
	private static HashMap<String, ExchangeSpecification> exchangeLookupMap;
	
	static {
		exchangeLookupMap = new HashMap<String, ExchangeSpecification>();
		exchangeLookupMap.put("anx", new ExchangeSpecification(ANXExchange.class.getName()));
		exchangeLookupMap.put("bitbay", new ExchangeSpecification(BitbayExchange.class.getName()));
		exchangeLookupMap.put("bitstamp", new ExchangeSpecification(BitstampExchange.class.getName()));
		exchangeLookupMap.put("cexio", new ExchangeSpecification(CexIOExchange.class.getName()));
		exchangeLookupMap.put("dsx", new ExchangeSpecification(DSXExchange.class.getName()));
		exchangeLookupMap.put("gdax", new ExchangeSpecification(GDAXExchange.class.getName()));
		exchangeLookupMap.put("gemini", new ExchangeSpecification(GeminiExchange.class.getName()));
		exchangeLookupMap.put("kraken", new ExchangeSpecification(KrakenExchange.class.getName()));
		exchangeLookupMap.put("poloniex", new ExchangeSpecification(PoloniexExchange.class.getName()));
		exchangeLookupMap.put("okcoin", new ExchangeSpecification(OkCoinExchange.class.getName()));
		
		//exchange specific params
		exchangeLookupMap.get("okcoin").setExchangeSpecificParametersItem("Use_Intl", true);
		
		//disabled -- not working
		//exchangeLookupMap.put("btcc", BTCCExchange.class.getName());
		//exchangeLookupMap.put("coinbase", CoinbaseExchange.class.getName());
		//exchangeLookupMap.put("itbit", ItBitExchange.class.getName());
		
		//disabled -- not using
		//exchangeLookupMap.put("taurus", new ExchangeSpecification(TaurusExchange.class.getName()));
		//exchangeLookupMap.put("quadrigacx", new ExchangeSpecification(QuadrigaCxExchange.class.getName()));
		//exchangeLookupMap.put("btce", new ExchangeSpecification(BTCEExchange.class.getName()));
		//exchangeLookupMap.put("bitfinex", new ExchangeSpecification(BitfinexExchange.class.getName()));
		
		//disabled -- no BTC/USD
		//exchangeLookupMap.put("bittrex", BittrexExchange.class.getName());
	}
	
	public static void main(String[] args) {
		Logger root = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		root.setLevel(Level.OFF);
		
		try {
			for (int i = 0; i < args.length; i+=2) {
				processArg(args[i], args[i+1]);
			}	
		} catch (Exception e) {
			exitWithError("Could not process argument: " + e);
		}
		
		if (exchangeNameTypeMap.isEmpty()) {
			System.out.println("No exchanges specified - using all");
			exchangeNameTypeMap = exchangeLookupMap;
		}
		
		if (hours != 0) {
			end = Date.from(start.toInstant().plusSeconds(hours * 60 * 60));
		}
		
		Arb arb = new Arb(runMode, currencyPairs, exchangeNameTypeMap, strategy, start, end, pauseEnabled);
		arb.start();
	}
	
	public static void processArg(String key, String val) throws Exception {
		System.out.println("Processing arg: " + key + " " + val);
		if (key.equals("--mode")) {
			if (val.equalsIgnoreCase(RunMode.Live.toString())) {
				runMode = RunMode.Live;
			} else if (val.equalsIgnoreCase(RunMode.Record.toString())) {
				runMode = RunMode.Record;
			} else if (val.equalsIgnoreCase(RunMode.Replay.toString())) {
				runMode = RunMode.Replay;
			} else {
				runMode = RunMode.Unknown;
			}
		} else if (key.equals("--currencies")) {
			String[] split = val.split(",");
			for (String currencyPairRaw : split) {
				String[] currencySplit = currencyPairRaw.split("/");
				currencyPairs.add(new CurrencyPair(new Currency(currencySplit[0]), new Currency(currencySplit[1])));
			}
		} else if (key.equals("--exchanges")) {
			String[] split = val.split(",");
			for (String exchange : split) {
				ExchangeSpecification exchangeLookupResult = exchangeLookupMap.get(exchange);
				if (exchangeLookupResult == null) {
					exitWithError("unknown exchange " + exchange);
				}
				exchangeNameTypeMap.put(exchange, exchangeLookupResult);
			}
		} else if (key.equals("--strategy")) {
			strategy = getStrategy(val);
		} else if (key.equals("--start")) {
			start = new SimpleDateFormat("yyyy-MM-dd").parse(val);
		} else if (key.equals("--end")) {
			end = new SimpleDateFormat("yyyy-MM-dd").parse(val);
		} else if (key.equals("--hours")) {
			hours = Integer.parseInt(val);
		} else if (key.equals("--pause")) {
			pauseEnabled = Boolean.parseBoolean(val);
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
