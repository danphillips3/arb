package org.arb;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;

import org.arb.strategy.Strategy;
import org.knowm.xchange.*;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;

public class Arb {
	private boolean m_isRecording = false;
	private String m_recordingPath = "";
	private PrintWriter m_recordWriter;
	private ArrayList<Exchange> m_exchanges = new ArrayList<Exchange>();
	private ArrayList<MarketDataFeed> m_marketDataFeeds = new ArrayList<MarketDataFeed>();
	private ArrayList<CurrencyPair> m_currencyPairs = new ArrayList<CurrencyPair>();
	private ArrayList<String> tradeList = new ArrayList<String>();
	private Calc m_calc;
	
	private static Map<String, AccountInfo> m_accountMap = new HashMap<String, AccountInfo>();
	
	public Arb(CurrencyPair[] currencyPairs, Map<String, String> exchangeNameTypeMap, Strategy strategy) {
		m_calc = new Calc(this, strategy);
		addCurrencyPairs(currencyPairs);
		addExchanges(exchangeNameTypeMap);
	}
	
	public void initRecording(String recordingPath) {
		m_isRecording = true;
		m_recordingPath = recordingPath;
		
		try {
			FileWriter fw = new FileWriter(m_recordingPath, false);
			BufferedWriter bw = new BufferedWriter(fw);
			m_recordWriter = new PrintWriter(bw);
		} catch (Exception e) {
			System.out.println("Error initializing recording: " + e.getMessage());
		}
	}
	
	public void startLive() {
		System.out.println("Starting Arb - Live");
		for (MarketDataFeed feed : m_marketDataFeeds) {
			feed.start();
		}
		
		System.out.println("--Initial Wallet Value--");
		printFullWalletValue();
	}
	
	public void startReplay(String replayPath) {
		System.out.println("Starting Arb - Replaying from " + replayPath);
		
		ArrayList<OrderBookInfo> updates = new ArrayList<OrderBookInfo>();
		try {
			FileReader fileReader = new FileReader(replayPath);
			BufferedReader reader = new BufferedReader(fileReader);
			String line;
			while ((line = reader.readLine()) != null) {
				OrderBookInfo orderBookInfo = new OrderBookInfo(line);
				updates.add(orderBookInfo);
			}
			reader.close();
		} catch (Exception e) {
			System.out.println("Error reading replay from " + replayPath + ": " + e.getMessage());
		}
		
		for (OrderBookInfo orderBookInfo : updates) {
			m_calc.onOrderBookUpdate(orderBookInfo);
		}
	}
	
	public void executeTrade(TradeDetails trade) {
		System.out.println("Executing trade: " + trade.toString());
		tradeList.add(trade.toString());
		
		AccountInfo buyAccount = getAccountInfo(trade.getBuyExchange());
		AccountInfo sellAccount = getAccountInfo(trade.getSellExchange());
		
		buyAccount.applyCredit(trade.getBuyCredit());
		buyAccount.applyDebit(trade.getBuyDebit());
		sellAccount.applyCredit(trade.getSellCredit());
		sellAccount.applyDebit(trade.getSellDebit());
	}
	
	public static AccountInfo getAccountInfo(String name) {
		return m_accountMap.get(name);
	}
	
	public void printFullWalletValue() {
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
		
		System.out.print("Wallet: ");
		
		BigDecimal totalUsdValue = BigDecimal.ZERO;
		for (Currency currency : wallet.keySet()) {
			BigDecimal value = wallet.get(currency);
			System.out.print(currency.toString() + " = " + value.setScale(4, BigDecimal.ROUND_HALF_EVEN) + " ");
			
			BigDecimal usdRate = ArbControls.getUsdRate(currency);
			BigDecimal usdValue = value.multiply(usdRate);
			totalUsdValue = totalUsdValue.add(usdValue);
		}
		System.out.print(" -> Total USD Value: " + totalUsdValue.setScale(2, BigDecimal.ROUND_DOWN));
		System.out.println("--Trades--");
		for (String tradeStr : tradeList) {
			System.out.println(tradeStr);
		}
	}
	
	public void addExchange(String name, String exchangeType) {
		System.out.println("Adding exchange - " + name);
		
		Exchange exchange = ExchangeFactory.INSTANCE.createExchange(exchangeType);
		m_exchanges.add(exchange);
		
		AccountInfo account = new AccountInfo(name, exchange.getAccountService());
		m_accountMap.put(name, account);
		
		MarketDataFeed marketDataFeed = new MarketDataFeed(name, m_currencyPairs, exchange.getMarketDataService(), m_calc);
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
	
	public void addCurrencyPairs(CurrencyPair[] pairs) {
		for (CurrencyPair pair : pairs) {
			addCurrencyPair(pair);
		}
	}
	
	public boolean isRecording() {
		return m_isRecording;
	}
	
	public void recordOrderBookInfo(OrderBookInfo orderBookInfo) {
		m_recordWriter.println(orderBookInfo.serialize());
		m_recordWriter.flush();
	}
}
