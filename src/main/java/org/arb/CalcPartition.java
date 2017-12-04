package org.arb;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

import org.knowm.xchange.currency.*;
import org.knowm.xchange.dto.marketdata.OrderBook;

public class CalcPartition {
	private CurrencyPair m_currencyPair;
	
	//key: feed, value: latest price
	private Map<String, OrderBookInfo> m_orderBookMap = new ConcurrentHashMap<String, OrderBookInfo>();
	
	public CalcPartition(CurrencyPair currencyPair) {
		m_currencyPair = currencyPair;
	}
	
	public void onOrderBookUpdate(OrderBookInfo orderBookInfo) {
		System.out.println(orderBookInfo.toString());
		m_orderBookMap.put(orderBookInfo.getExchange(), orderBookInfo);
	}
	
	public CurrencyPair getCurrencyPair() {
		return m_currencyPair;
	}
	
	public Map<String, OrderBookInfo> getOrderBookMap() {
		return m_orderBookMap;
	}
}
