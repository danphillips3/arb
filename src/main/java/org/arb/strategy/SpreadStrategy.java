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
	private boolean m_hasEnteredTrade = false;
	private String m_shortExchange;
	private String m_longExchange;
	private BigDecimal m_enterSpread;
	private BigDecimal m_exitSpread;
	
	//cached values
	private CurrencyPair m_currencyPair;
	private BigDecimal m_shortExchangeFee;
	private BigDecimal m_longExchangeFee;
	
	public ArrayList<TradeDetails> getTrades(CalcPartition partition, OrderBookInfo lastUpdatedOrderBook) {
		ArrayList<TradeDetails> trades = new ArrayList<TradeDetails>();
		m_currencyPair = partition.getCurrencyPair();
		
		for (OrderBookInfo orderBookInfo : partition.getOrderBookMap().values()) {
			if (trades.isEmpty() && lastUpdatedOrderBook.getExchange() != orderBookInfo.getExchange()) {
				try {
					if (!m_hasEnteredTrade) {
						trades.addAll(processUpdateNoPosition(lastUpdatedOrderBook, orderBookInfo));
					} else {
						trades.addAll(processUpdateWithPosition(lastUpdatedOrderBook, orderBookInfo));
					}
				} catch (Exception e) {
					System.out.println("Exception processing update: " + e);
				}
			}
		}
		
		return trades;
	}
	
	private ArrayList<TradeDetails> processUpdateWithPosition(OrderBookInfo orderBook, OrderBookInfo orderBookCompare) { 
		ArrayList<TradeDetails> trades = new ArrayList<TradeDetails>();
		
		OrderBookInfo shortOrderBook = null;
		if (orderBook.getExchange().equals(m_shortExchange)) {
			shortOrderBook = orderBook;
		} else if (orderBookCompare.getExchange().equals(m_shortExchange)) {
			shortOrderBook = orderBookCompare;
		}
		
		OrderBookInfo longOrderBook = null;
		if (orderBook.getExchange().equals(m_longExchange)) {
			longOrderBook = orderBook;
		} else if (orderBookCompare.getExchange().equals(m_longExchange)) {
			longOrderBook = orderBookCompare;
		}
		
		if (shortOrderBook != null && longOrderBook != null) {
			AccountInfo shortAccount = Arb.getAccountInfo(m_shortExchange);
			BigDecimal desiredLiquidity = BigDecimal.ZERO.subtract(shortAccount.getBalance(m_currencyPair.base));
			
			if (shouldExitTrade(longOrderBook, shortOrderBook, desiredLiquidity)) {
				//generate trade instructions
				TradeDetails buyBackTrade = new TradeDetails(m_currencyPair, m_shortExchange,
					TradeType.BuyBack, shortOrderBook.getBestAsk(), desiredLiquidity, m_shortExchangeFee);
				TradeDetails sellTrade = new TradeDetails(m_currencyPair, m_longExchange, 
					TradeType.Sell, longOrderBook.getBestBid(), desiredLiquidity, m_longExchangeFee);
				
				trades.add(buyBackTrade);
				trades.add(sellTrade);
				
				Arb.addTradeLogLine("EXITING TRADE @ " + Util.format(getSpread(shortOrderBook.getBestAsk(), longOrderBook.getBestBid()), 5));	
				resetState();
			}
		}
		
		return trades;
	}
	
	private boolean shouldExitTrade(OrderBookInfo longOrderBook, OrderBookInfo shortOrderBook, BigDecimal desiredLiquidity) {
		//check spread
		if (getSpread(shortOrderBook.getBestAsk(), longOrderBook.getBestBid()).compareTo(m_exitSpread) > 0) {
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
			m_shortExchange = shortOrderBook.getExchange();
			m_longExchange = longOrderBook.getExchange();
			m_enterSpread = getSpread(shortOrderBook.getBestBid(), longOrderBook.getBestAsk());
			m_hasEnteredTrade = true;
			
			//fees
			m_shortExchangeFee = Arb.getAccountInfo(m_shortExchange).getTransactionRate();
			m_longExchangeFee = Arb.getAccountInfo(m_longExchange).getTransactionRate(); 
			
			//need to pay each fee twice
			//short fee - once on shortsell, once on buyback
			//long fee - once on buy, once on sell
			BigDecimal fullShortFee = m_shortExchangeFee.multiply(new BigDecimal(2));
			BigDecimal fullLongFee = m_longExchangeFee.multiply(new BigDecimal(2));
			
			//calculate spread to exit at
			BigDecimal spreadMinusFees = m_enterSpread.subtract(fullShortFee).subtract(fullLongFee);
			m_exitSpread = spreadMinusFees.subtract(m_spreadTarget);
			
			//generate trade instructions
			BigDecimal tradeQty = getDesiredLiquidity(longOrderBook);
			
			TradeDetails longTrade = new TradeDetails(m_currencyPair, m_longExchange, 
				TradeType.Buy, longOrderBook.getBestAsk(), tradeQty, m_longExchangeFee);
			TradeDetails shortTrade = new TradeDetails(m_currencyPair, m_shortExchange,
				TradeType.ShortSell, shortOrderBook.getBestBid(), tradeQty, m_shortExchangeFee);
			
			trades.add(longTrade);
			trades.add(shortTrade);
			
			Arb.addTradeLogLine("ENTERING TRADE @ " + Util.format(m_enterSpread, 5) + " -> EXIT TARGET: " + Util.format(m_exitSpread, 5));
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
	
	private void resetState() {
		m_hasEnteredTrade = false;
		m_shortExchange = null;
		m_longExchange = null;
		m_enterSpread = null;
		m_exitSpread = null;
		m_currencyPair = null;
		m_shortExchangeFee = null;
		m_longExchangeFee = null;
	}
	
	public void setStartingReplayBalance(Collection<AccountInfo> accounts) {
		for (AccountInfo account : accounts) {
			account.updateBalance(Currency.USD, m_startingUsd);
			account.updateBalance(Currency.BTC, new BigDecimal(0));
		}
	}
}
