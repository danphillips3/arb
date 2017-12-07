package org.arb;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

import org.knowm.xchange.currency.*;

public class CalcPartition {
	private CurrencyPair m_currencyPair;
	
	//key: feed, value: latest price
	private Map<String, OrderBookInfo> m_orderBookMap = new ConcurrentHashMap<String, OrderBookInfo>();
	
	public CalcPartition(CurrencyPair currencyPair) {
		m_currencyPair = currencyPair;
	}
	
	public boolean onOrderBookUpdate(OrderBookInfo orderBookInfo) {
		System.out.println(orderBookInfo.toString());
		if (orderBookInfo.getBestAsk().equals(BigDecimal.ZERO) || orderBookInfo.getBestBid().equals(BigDecimal.ZERO)) {
			System.out.println("Skipping bad update");
			return false;
		}
		
		m_orderBookMap.put(orderBookInfo.getExchange(), orderBookInfo);
		return true;
	}
	
	public CurrencyPair getCurrencyPair() {
		return m_currencyPair;
	}
	
	public Map<String, OrderBookInfo> getOrderBookMap() {
		return m_orderBookMap;
	}
}
