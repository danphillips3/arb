package org.arb;

import java.util.*;

import org.knowm.xchange.*;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.service.marketdata.MarketDataService;

import si.mazi.rescu.HttpStatusExceptionSupport;

public class MarketDataFeed {
	private final int updateDelayMillis = 3000;
	
	private String m_feedName;
	private MarketDataService m_marketDataService;
	private Calc m_calc;
	private ArrayList<CurrencyPair> m_currencyPairs = new ArrayList<CurrencyPair>();
	
	public MarketDataFeed(String name, ArrayList<CurrencyPair> currencyPairs, MarketDataService service, Calc calc) {
		m_feedName = name;
		m_marketDataService = service;
		m_calc = calc;
		m_currencyPairs.addAll(currencyPairs);
	}
	
	public void start() {
		for (CurrencyPair currencyPair : m_currencyPairs) {
			TaskPool.addTask(() -> update(currencyPair));
		}
	}
	
	private void update(CurrencyPair currencyPair) {
		try {
			OrderBook orderBook = m_marketDataService.getOrderBook(currencyPair);
			OrderBookInfo orderBookInfo = new OrderBookInfo(m_feedName, currencyPair, orderBook);
			m_calc.onOrderBookUpdate(orderBookInfo);
			
			Thread.sleep(updateDelayMillis);
		} catch (HttpStatusExceptionSupport httpException) {
			System.out.println(m_feedName + ": Failed to update " + currencyPair.toString() + " - HTTP exception: " + httpException.getHttpStatusCode());
			if (httpException.getHttpStatusCode() == 404) {
				System.out.println(m_feedName + ": Currency pair " + currencyPair.toString() + " not available");
				return;
			}
		} catch (Exception e) {
			System.out.println(m_feedName + ": Failed to update " + currencyPair.toString() + " - " + e.getMessage());
		}
		
		TaskPool.addTask(() -> update(currencyPair));
	}
}
