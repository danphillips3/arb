package org.arb.strategy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;

import org.arb.AccountInfo;
import org.arb.Arb;
import org.arb.CalcPartition;
import org.arb.OrderBookInfo;
import org.arb.TradeDetails;
import org.arb.TradeDetails.TradeType;
import org.arb.Util;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;

/*
 * SpreadStrategy
 * 
 */

public class SpreadStrategy extends Strategy {
	//strategy control params
	private BigDecimal m_spreadEntry = new BigDecimal(0.005);
	private BigDecimal m_spreadTarget = new BigDecimal(0.005);
	private BigDecimal m_liquidityAvailabilityMultiplier = new BigDecimal(1.2);
	private BigDecimal m_balanceOverflowPadding = new BigDecimal(0.85);
	private BigDecimal m_startingUsd = new BigDecimal(1000);
	
	//strategy state members
	private static int m_nextPositionId = 1;
	private ArrayList<SpreadPosition> m_openPositions = new ArrayList<SpreadPosition>();
	
	//cached values
	private CurrencyPair m_currencyPair;
	
	public ArrayList<TradeDetails> getTrades(CalcPartition partition, OrderBookInfo lastUpdatedOrderBook) {
		ArrayList<TradeDetails> trades = new ArrayList<TradeDetails>();
		m_currencyPair = partition.getCurrencyPair();
		
		for (OrderBookInfo orderBookInfo : partition.getOrderBookMap().values()) {
			if (lastUpdatedOrderBook.getExchange().equals(orderBookInfo.getExchange())) {
				continue;
			}
			
			try {
				if (!hasOpenPositionOnExchange(lastUpdatedOrderBook.getExchange()) && !hasOpenPositionOnExchange(orderBookInfo.getExchange())) {
					trades.addAll(processUpdateNoPosition(lastUpdatedOrderBook, orderBookInfo));
				} else {
					SpreadPosition openPosition = getOpenPosition(lastUpdatedOrderBook.getExchange(), orderBookInfo.getExchange());
					if (openPosition != null && openPosition.isFullyFilled()) {
						trades.addAll(processUpdateWithPosition(openPosition, lastUpdatedOrderBook, orderBookInfo));
					}
				}
			} catch (Exception e) {
				System.out.println("Exception processing update: " + e);
			}
		}
		
		return trades;
	}
	
	public void onFill(String exchange, TradeType tradeType, BigDecimal qty, BigDecimal price) {
		SpreadPosition position = getOpenPosition(exchange);
		if (position == null) {
			return;
		}
		
		if (position.m_shortExchange.equals(exchange)) {
			position.m_shortFillQtyRemaining = position.m_shortFillQtyRemaining.subtract(qty);
		} else if (position.m_longExchange.equals(exchange)) {
			position.m_longFillQtyRemaining = position.m_longFillQtyRemaining.subtract(qty);
		}
	}
	
	private boolean hasOpenPositionOnExchange(String exchange) {
		for (SpreadPosition position : m_openPositions) {
			if (position.m_longExchange.equals(exchange) || position.m_shortExchange.equals(exchange)) {
				return true;
			}
		}
		return false;
	}
	
	private SpreadPosition getOpenPosition(String exchange) {
		for (SpreadPosition position : m_openPositions) {
			if (position.m_longExchange.equals(exchange) || position.m_shortExchange.equals(exchange)) {
				return position;
			}
		}
		return null;
	}
	
	private SpreadPosition getOpenPosition(String exchange1, String exchange2) {
		for (SpreadPosition position : m_openPositions) { 
			if (position.m_longExchange.equals(exchange1) && position.m_shortExchange.equals(exchange2)) {
				return position;
			}
			if (position.m_longExchange.equals(exchange2) && position.m_shortExchange.equals(exchange1)) {
				return position;
			}
		}
		return null;
	}
	
	private SpreadPosition openNewPosition(BigDecimal qty) {
		SpreadPosition newPosition = new SpreadPosition(m_nextPositionId++, qty);
		m_openPositions.add(newPosition);
		return newPosition;
	}
	
	private ArrayList<TradeDetails> processUpdateWithPosition(SpreadPosition position, OrderBookInfo orderBook, OrderBookInfo orderBookCompare) { 
		ArrayList<TradeDetails> trades = new ArrayList<TradeDetails>();
		
		OrderBookInfo shortOrderBook = null;
		if (orderBook.getExchange().equals(position.m_shortExchange)) {
			shortOrderBook = orderBook;
		} else if (orderBookCompare.getExchange().equals(position.m_shortExchange)) {
			shortOrderBook = orderBookCompare;
		}
		
		OrderBookInfo longOrderBook = null;
		if (orderBook.getExchange().equals(position.m_longExchange)) {
			longOrderBook = orderBook;
		} else if (orderBookCompare.getExchange().equals(position.m_longExchange)) {
			longOrderBook = orderBookCompare;
		}
		
		if (shortOrderBook != null && longOrderBook != null) {
			AccountInfo shortAccount = Arb.getAccountInfo(position.m_shortExchange);
			BigDecimal desiredLiquidity = BigDecimal.ZERO.subtract(shortAccount.getBalance(m_currencyPair.base));
			
			if (shouldExitTrade(position, longOrderBook, shortOrderBook, desiredLiquidity)) {
				//generate trade instructions
				TradeDetails buyBackTrade = new TradeDetails(m_currencyPair, position.m_shortExchange,
					TradeType.BuyBack, shortOrderBook.getBestAsk(), desiredLiquidity, position.m_shortExchangeFee);
				TradeDetails sellTrade = new TradeDetails(m_currencyPair, position.m_longExchange, 
					TradeType.Sell, longOrderBook.getBestBid(), desiredLiquidity, position.m_longExchangeFee);
				
				trades.add(buyBackTrade);
				trades.add(sellTrade);	
				
				//close out position
				m_openPositions.remove(position);
				
				Arb.addTradeLogLine("Close position #" + position.m_id + " EXIT @ " + 
						Util.format(getSpread(shortOrderBook.getBestAsk(), longOrderBook.getBestBid()), 5) + 
						" (" + m_openPositions.size() + " open positions remain)");
			}
		}
		
		return trades;
	}
	
	private boolean shouldExitTrade(SpreadPosition position, OrderBookInfo longOrderBook, OrderBookInfo shortOrderBook, BigDecimal desiredLiquidity) {
		//check spread
		if (getSpread(shortOrderBook.getBestAsk(), longOrderBook.getBestBid()).compareTo(position.m_exitSpread) > 0) {
			return false;
		}
		
		//check liquidity
		if (desiredLiquidity.compareTo(shortOrderBook.getBestAskQty()) < 0) {
			return false;
		}
		
		if (desiredLiquidity.compareTo(longOrderBook.getBestBidQty()) < 0) {
			return false;
		}
		
		return true;
	}
	
	private ArrayList<TradeDetails> processUpdateNoPosition(OrderBookInfo orderBook, OrderBookInfo orderBookCompare) {
		ArrayList<TradeDetails> trades = new ArrayList<TradeDetails>();
		
		OrderBookInfo shortOrderBook = null;
		OrderBookInfo longOrderBook = null;
		boolean shouldEnterTrade = false;
		
		if (shouldEnterTrade(orderBook, orderBookCompare)) {
			shouldEnterTrade = true;
			shortOrderBook = orderBook;
			longOrderBook = orderBookCompare;
		} else if (shouldEnterTrade(orderBookCompare, orderBook)) {
			shouldEnterTrade = true;
			shortOrderBook = orderBookCompare;
			longOrderBook = orderBook;
		}
		
		if (shouldEnterTrade) {
			BigDecimal tradeQty = getDesiredLiquidity(longOrderBook);
			
			SpreadPosition position = openNewPosition(tradeQty);
			position.m_shortExchange = shortOrderBook.getExchange();
			position.m_longExchange = longOrderBook.getExchange();
			position.m_enterSpread = getSpread(shortOrderBook.getBestBid(), longOrderBook.getBestAsk());
			
			//fees
			position.m_shortExchangeFee = Arb.getAccountInfo(position.m_shortExchange).getTransactionRate();
			position.m_longExchangeFee = Arb.getAccountInfo(position.m_longExchange).getTransactionRate(); 
			
			//need to pay each fee twice
			//short fee - once on shortsell, once on buyback
			//long fee - once on buy, once on sell
			BigDecimal fullShortFee = position.m_shortExchangeFee.multiply(new BigDecimal(2));
			BigDecimal fullLongFee = position.m_longExchangeFee.multiply(new BigDecimal(2));
			
			//calculate spread to exit at
			BigDecimal spreadMinusFees = position.m_enterSpread.subtract(fullShortFee).subtract(fullLongFee);
			position.m_exitSpread = spreadMinusFees.subtract(m_spreadTarget);
			
			//generate trade instructions
			TradeDetails longTrade = new TradeDetails(m_currencyPair, position.m_longExchange, 
				TradeType.Buy, longOrderBook.getBestAsk(), tradeQty, position.m_longExchangeFee);
			TradeDetails shortTrade = new TradeDetails(m_currencyPair, position.m_shortExchange,
				TradeType.ShortSell, shortOrderBook.getBestBid(), tradeQty, position.m_shortExchangeFee);
			
			trades.add(longTrade);
			trades.add(shortTrade);
			
			Arb.addTradeLogLine("Open position #" + position.m_id + " ENTER @ " + Util.format(position.m_enterSpread, 5) + " -> EXIT TARGET: " + Util.format(position.m_exitSpread, 5) + " (" + m_openPositions.size() + " open positions)");
		}
		
		return trades;
	}
	
	private boolean shouldEnterTrade(OrderBookInfo shortOrderBook, OrderBookInfo longOrderBook) {
		//check that margin trading is allowed
		if (!Arb.getAccountInfo(shortOrderBook.getExchange()).isMarginTradingAllowed()) {
			return false;
		}
		
		//check spread
		if (getSpread(shortOrderBook.getBestBid(), longOrderBook.getBestAsk()).compareTo(m_spreadEntry) <= 0) {
			return false;
		}
		
		//check liquidity
		BigDecimal qty = getDesiredLiquidity(longOrderBook);
		
		if (qty.compareTo(longOrderBook.getBestAskQty().multiply(m_liquidityAvailabilityMultiplier)) > 0) {
			return false;
		}
		
		if (qty.compareTo(shortOrderBook.getBestBidQty().multiply(m_liquidityAvailabilityMultiplier)) > 0) {
			return false;
		}
		
		return true;
	}
	
	private BigDecimal getDesiredLiquidity(OrderBookInfo longOrderBook) {
		AccountInfo longAccount = Arb.getAccountInfo(longOrderBook.getExchange());
		BigDecimal availableBuyFunds = longAccount.getBalance(m_currencyPair.counter).multiply(m_balanceOverflowPadding);
		return availableBuyFunds.divide(longOrderBook.getBestAsk(), 8, BigDecimal.ROUND_DOWN);
	}
	
	private BigDecimal getSpread(BigDecimal price1, BigDecimal price2) {
		BigDecimal diff = price1.subtract(price2);
		if (diff.equals(BigDecimal.ZERO)) {
			return null;
		}
		BigDecimal spread = diff.divide(price1, 8, BigDecimal.ROUND_DOWN);		
		return spread;
	}
	
	public void setStartingReplayBalance(Collection<AccountInfo> accounts) {
		for (AccountInfo account : accounts) {
			account.updateBalance(Currency.USD, m_startingUsd);
			account.updateBalance(Currency.BTC, new BigDecimal(0));
		}
	}
	
	private class SpreadPosition {
		public int m_id;
		public String m_shortExchange;
		public String m_longExchange;
		public BigDecimal m_shortExchangeFee;
		public BigDecimal m_longExchangeFee;
		public BigDecimal m_enterSpread;
		public BigDecimal m_exitSpread;
		public BigDecimal m_shortFillQtyRemaining = BigDecimal.ZERO;
		public BigDecimal m_longFillQtyRemaining = BigDecimal.ZERO;
		
		public SpreadPosition(int id, BigDecimal qty) {
			m_id = id;
			m_shortFillQtyRemaining = qty;
			m_longFillQtyRemaining = qty;
		}
		
		public boolean isFullyFilled() {
			return m_shortFillQtyRemaining.compareTo(BigDecimal.ZERO) == 0 && 
					m_longFillQtyRemaining.compareTo(BigDecimal.ZERO) == 0;
		}
		
		public int hashCode() {
			return m_id;
		}
		
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			return m_id == ((SpreadPosition)obj).m_id;
		}
	}
}
