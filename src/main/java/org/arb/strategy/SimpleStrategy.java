package org.arb.strategy;

import java.math.BigDecimal;
import java.util.ArrayList;

import org.arb.AccountInfo;
import org.arb.Arb;
import org.arb.ArbControls;
import org.arb.CalcPartition;
import org.arb.OrderBookInfo;
import org.arb.StaticData;
import org.arb.TradeDetails;
import org.knowm.xchange.currency.CurrencyPair;

public class SimpleStrategy implements Strategy {
	public ArrayList<TradeDetails> getTrades(CalcPartition partition) {
		ArrayList<TradeDetails> potentialTrades = new ArrayList<TradeDetails>();
		for (OrderBookInfo orderBookInfo : partition.getOrderBookMap().values()) {
			for (OrderBookInfo compareOrderBookInfo : partition.getOrderBookMap().values()) {
				if (compareOrderBookInfo.getExchange() != orderBookInfo.getExchange()) {
					potentialTrades.add(getPotentialTrade(partition.getCurrencyPair(), orderBookInfo, compareOrderBookInfo));
				}
			}
		}
		
		TradeDetails bestTrade = null;
		for (TradeDetails potentialTrade : potentialTrades) {
			System.out.print("Potential trade: " + potentialTrade.toString());
			
			BigDecimal potentialProfit = potentialTrade.getEstimatedProfit();
			if (potentialProfit.compareTo(ArbControls.getProfitThreshold()) < 0) {
				System.out.println(" -> Below Profit Threshold");
			} else if (bestTrade == null || potentialProfit.compareTo(bestTrade.getEstimatedProfit()) > 0) {
				System.out.println(" -> New best");
				bestTrade = potentialTrade;
			} else {
				System.out.println(" -> Worse");
			}
		}
		
		ArrayList<TradeDetails> trades = new ArrayList<TradeDetails>();
		if (bestTrade == null) {
			System.out.println("No viable trades");
		} else {
			trades.add(bestTrade);
		}
		return trades;
	}
	
	private TradeDetails getPotentialTrade(CurrencyPair currencyPair, OrderBookInfo orderBook, OrderBookInfo compareOrderBook) {
		OrderBookInfo buySideOrderBook, sellSideOrderBook;
		if (orderBook.getBestBid().compareTo(compareOrderBook.getBestBid()) <= 0) {
			buySideOrderBook = orderBook;
			sellSideOrderBook = compareOrderBook;
		} else {
			buySideOrderBook = compareOrderBook;
			sellSideOrderBook = orderBook;
		}
		
		AccountInfo buySideAccount = Arb.getAccountInfo(buySideOrderBook.getExchange());
		AccountInfo sellSideAccount = Arb.getAccountInfo(sellSideOrderBook.getExchange());
		
		BigDecimal buyFunds = buySideAccount.getBalance(buySideOrderBook.getCurrencyPair().counter);
		BigDecimal sellFunds = sellSideAccount.getBalance(sellSideOrderBook.getCurrencyPair().base);
		
		BigDecimal maxBuyQty = buyFunds.divide(buySideOrderBook.getBestAsk(), 8, BigDecimal.ROUND_DOWN);
		BigDecimal maxSellQty = sellFunds; //sell funds is the same as sell qty
		BigDecimal maxFundsQty = maxBuyQty.min(maxSellQty);
		
		BigDecimal availableQty = buySideOrderBook.getBestBidQty().min(sellSideOrderBook.getBestAskQty());
		BigDecimal qty = availableQty.min(maxFundsQty);
		
		BigDecimal buyDollarAmount = buySideOrderBook.getBestAsk().multiply(qty);
		BigDecimal sellDollarAmount = sellSideOrderBook.getBestBid().multiply(qty);
		BigDecimal diffDollarAmount = sellDollarAmount.subtract(buyDollarAmount);
		
		BigDecimal buyTransactionFee = buyDollarAmount.multiply(StaticData.getTransactionRate());
		BigDecimal sellTransactionFee = sellDollarAmount.multiply(StaticData.getTransactionRate());
		BigDecimal totalTransactionFee = buyTransactionFee.add(sellTransactionFee);
		
		BigDecimal profit = diffDollarAmount.subtract(totalTransactionFee);
		
		return new TradeDetails(
				currencyPair, 
				buySideOrderBook.getExchange(), sellSideOrderBook.getExchange(),
				sellSideOrderBook.getBestBid(), buySideOrderBook.getBestAsk(),
				qty, buyTransactionFee, sellTransactionFee, profit);
	}
}
