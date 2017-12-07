package org.arb;

import java.util.*;
import java.util.concurrent.*;

import org.arb.strategy.Strategy;
import org.knowm.xchange.currency.*;
import org.knowm.xchange.dto.marketdata.OrderBook;

public class Calc {
	private Arb m_arb;
	private Strategy m_strategy;
	
	//key: currency pair, val: calc partition for currency pair
	private Map<CurrencyPair, CalcPartition> m_partitionMap = new ConcurrentHashMap<CurrencyPair, CalcPartition>();
	
	public Calc(Arb arb, Strategy strategy) {
		m_arb = arb;
		m_strategy = strategy;
	}
	
	public void onOrderBookUpdate(OrderBookInfo orderBookInfo) {
		if (m_arb.getRunMode() == RunMode.Record) {
			m_arb.recordOrderBookInfo(orderBookInfo);
		} else {
			CalcPartition calcPartition = getPartition(orderBookInfo.getCurrencyPair());
			boolean updateSuccess = calcPartition.onOrderBookUpdate(orderBookInfo);
			
			if (m_strategy != null && updateSuccess) {
				ArrayList<TradeDetails> trades = m_strategy.getTrades(calcPartition, orderBookInfo);
				for (TradeDetails trade : trades) {
					m_arb.executeTrade(trade);
					
					String tradeLogLine = trade.toString();
					if (trade == trades.get(trades.size()-1)) {
						tradeLogLine += " -> " + m_arb.walletToString();
					}
					Arb.addTradeLogLine(tradeLogLine);
				}
				
				if (!trades.isEmpty()) {
					m_arb.pauseIfEnabled();
				}
			}
		}
	}
	
	private CalcPartition getPartition(CurrencyPair currencyPair) {
		CalcPartition calcPartition = m_partitionMap.get(currencyPair);
		if (calcPartition == null) {
			calcPartition = new CalcPartition(currencyPair);
			m_partitionMap.put(currencyPair, calcPartition);
		}
		return calcPartition;
	}
	
	public Strategy getStrategy() {
		return m_strategy;
	}
}
