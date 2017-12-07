package org.arb;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;

import org.arb.TradeDetails.TradeType;
import org.arb.strategy.Strategy;
import org.knowm.xchange.*;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.service.account.AccountService;
import org.knowm.xchange.service.marketdata.MarketDataService;

enum RunMode { Unknown, Live, Record, Replay };

public class Arb {
	private RunMode m_runMode;
	private Calc m_calc;
	private ArrayList<Exchange> m_exchanges = new ArrayList<Exchange>();
	private ArrayList<MarketDataFeed> m_marketDataFeeds = new ArrayList<MarketDataFeed>();
	private ArrayList<CurrencyPair> m_currencyPairs = new ArrayList<CurrencyPair>();
	private static ArrayList<String> m_tradeLog = new ArrayList<String>();
	private static Map<String, AccountInfo> m_accountMap = new HashMap<String, AccountInfo>();
	
	private boolean m_pauseEnabled;
	private Date m_start;
	private Date m_end;
	
	public Arb(
			RunMode runMode, 
			ArrayList<CurrencyPair> currencyPairs, 
			Map<String, String> exchangeNameTypeMap, 
			Strategy strategy, 
			Date start, 
			Date end,
			boolean pauseEnabled) {
		
		m_runMode = runMode;
		m_calc = new Calc(this, strategy);
		m_start = start;
		m_end = end;
		m_pauseEnabled = pauseEnabled;
		
		addCurrencyPairs(currencyPairs);
		addExchanges(exchangeNameTypeMap);
	}
	
	public void start() {
		if (m_runMode == RunMode.Live) {
			startLive();
		} else if (m_runMode == RunMode.Record) {
			startRecord();
		} else if (m_runMode == RunMode.Replay) {
			startReplay();
		} else {
			Main.exitWithError("Unknown run mode");
		}
	}
	
	private void startLive() {
		System.out.println("Starting Arb - Live");
		
		for (MarketDataFeed feed : m_marketDataFeeds) {
			feed.start();
		}
		
		System.out.println("--Initial Wallet Value--");
		System.out.println(walletToString());
	}
	
	private void startRecord() {
		System.out.println("Starting Arb - Recording");
		
		for (MarketDataFeed feed : m_marketDataFeeds) {
			feed.start();
		}
	}
	
	private void startReplay() {
		System.out.println("Starting Arb - Replaying from " + m_start.toString() + " - " + m_end.toString());
		
		ArrayList<OrderBookInfo> updates = DBManager.loadOrderBookUpdates(getExchanges(), m_start, m_end);
		System.out.println("Loaded " + updates.size() + " updates");
		
		Strategy strat = m_calc.getStrategy();
		if (strat != null) {
			m_calc.getStrategy().setStartingReplayBalance(m_accountMap.values());
		}
		
		System.out.println("--Initial Wallet Value--");
		System.out.println(walletToString());
		pauseIfEnabled();
		
		for (OrderBookInfo orderBookInfo : updates) {
			m_calc.onOrderBookUpdate(orderBookInfo);
		}
		
		printTrades();
		
		System.out.println("--Final Wallet Value--");
		System.out.println(walletToString());
	}
	
	public void executeTrade(TradeDetails trade) {
		AccountInfo account = getAccountInfo(trade.getExchange());
		if (m_runMode == RunMode.Replay) {
			trade.applyToAccount(account);
			
			if (m_calc.getStrategy() != null) {
				m_calc.getStrategy().onFill(trade.getExchange(), trade.getTradeType(), trade.getQty(), trade.getPrice());
			}
		} else {
			Main.exitWithError("ExecuteTrade not implemented for this run mode");
		}
	}
	
	public static AccountInfo getAccountInfo(String name) {
		return m_accountMap.get(name);
	}

	public String walletToString() {
		String walletString = "";
		
		Map<Currency, BigDecimal> wallet = new HashMap<Currency, BigDecimal>();
		for (AccountInfo account : m_accountMap.values()) {
			for (Currency currency : account.getCurrencies()) {
				if (!wallet.containsKey(currency)) {
					wallet.put(currency, account.getBalance(currency));
				} else {
					BigDecimal currentBalance = wallet.get(currency);
					wallet.put(currency, currentBalance.add(account.getBalance(currency)));
				}
			}
		}
		
		for (Currency currency : wallet.keySet()) {
			BigDecimal value = wallet.get(currency);
			walletString += currency.toString() + " = " + value.setScale(4, BigDecimal.ROUND_HALF_EVEN) + " ";
		}
		
		return walletString;
	}
	
	public void printTrades() {
		System.out.println("");
		System.out.println("--Trade Log--");
		for (String tradeLogLine : m_tradeLog) {
			System.out.println(tradeLogLine);
		}
	}
	
	public void addExchange(String name, String exchangeType) {
		System.out.println("Adding exchange - " + name);
		
		Exchange exchange = null;
		AccountService accountService = null;
		MarketDataService marketDataService = null;
		if (m_runMode != RunMode.Replay) {
			exchange = ExchangeFactory.INSTANCE.createExchange(exchangeType);
			accountService = exchange.getAccountService();
			marketDataService = exchange.getMarketDataService();
			
			m_exchanges.add(exchange);
		}
		
		AccountInfo account = new AccountInfo(name, accountService);
		m_accountMap.put(name, account);
		
		MarketDataFeed marketDataFeed = new MarketDataFeed(name, m_currencyPairs, marketDataService, m_calc);
		m_marketDataFeeds.add(marketDataFeed);
	}
	
	public void addExchanges(Map<String, String> exchangeNameTypeMap) {
		for (Entry<String, String> exchangeNameType : exchangeNameTypeMap.entrySet()) {
			addExchange(exchangeNameType.getKey(), exchangeNameType.getValue());
		}
	}
	
	public void addCurrencyPair(CurrencyPair currencyPair) {
		System.out.println("Adding currency pair - " + currencyPair.toString());
		m_currencyPairs.add(currencyPair);
	}
	
	public void addCurrencyPairs(ArrayList<CurrencyPair> pairs) {
		for (CurrencyPair pair : pairs) {
			addCurrencyPair(pair);
		}
	}
	
	public RunMode getRunMode() {
		return m_runMode;
	}
	
	public void recordOrderBookInfo(OrderBookInfo orderBookInfo) {
		DBManager.insert(orderBookInfo);
	}
	
	public ArrayList<String> getExchanges() {
		return new ArrayList<String>(m_accountMap.keySet());
	}
	
	public void pauseIfEnabled() {
		if (m_pauseEnabled) {
			Util.pause();
		}
	}
	
	public static void addTradeLogLine(String trade) {
		System.out.println(trade);
		m_tradeLog.add(trade);
	}
}
