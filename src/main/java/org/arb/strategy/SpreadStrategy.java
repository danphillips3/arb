package org.arb.strategy;

import java.math.BigDecimal;
import java.util.ArrayList;

import org.arb.AccountInfo;
import org.arb.Arb;
import org.arb.CalcPartition;
import org.arb.OrderBookInfo;
import org.arb.StaticData;
import org.arb.TradeDetails;
import org.knowm.xchange.currency.CurrencyPair;

/*
 * SpreadStrategy
 * 
 */

public class SpreadStrategy implements Strategy {
	
	
	public ArrayList<TradeDetails> getTrades(CalcPartition partition, OrderBookInfo lastUpdatedOrderBook) {
		
		for (OrderBookInfo orderBookInfo : partition.getOrderBookMap().values()) {
			if (lastUpdatedOrderBook.getExchange() != orderBookInfo.getExchange()) {
				System.out.println(getSpread(lastUpdatedOrderBook, orderBookInfo));
				System.out.println(getSpread(orderBookInfo, lastUpdatedOrderBook));
			}
		}
		
		ArrayList<TradeDetails> trades = new ArrayList<TradeDetails>();
		return trades;
	}
	
	private BigDecimal getSpread(OrderBookInfo orderBook, OrderBookInfo orderBookCompare) {
		BigDecimal diff = orderBook.getBestBid().subtract(orderBookCompare.getBestAsk());
		if (diff.equals(BigDecimal.ZERO)) {
			return null;
		}
		BigDecimal spread = diff.divide(orderBook.getBestBid(), 8, BigDecimal.ROUND_DOWN);
		return spread;
	}
	
	
}
