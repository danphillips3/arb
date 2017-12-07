package org.arb;

import java.math.*;

import org.knowm.xchange.currency.CurrencyPair;

public class TradeDetails {
	public enum TradeType { Buy, Sell, ShortSell, BuyBack }
	
	private CurrencyPair m_currencyPair;
	private String m_exchange;
	private TradeType m_tradeType;
	private BigDecimal m_price;
	private BigDecimal m_qty;
	private BigDecimal m_fee;
	
	public TradeDetails(CurrencyPair currencyPair, String exchange, TradeType tradeType, 
			BigDecimal price, BigDecimal qty, BigDecimal fee) {
		m_currencyPair = currencyPair;
		m_exchange = exchange;
		m_tradeType = tradeType;
		m_price = price;
		m_qty = qty;
		m_fee = fee;
	}
	
	public void applyToAccount(AccountInfo account) {
		MoneyAmount baseCurrencyTradeVal = new MoneyAmount(m_currencyPair.base, m_qty);
		MoneyAmount counterCurrencyTradeVal = new MoneyAmount(m_currencyPair.counter, m_price.multiply(m_qty));
		MoneyAmount fee = new MoneyAmount(m_currencyPair.counter, m_price.multiply(m_qty).multiply(m_fee));
		
		if (m_tradeType == TradeType.Buy) {
			account.applyCredit(baseCurrencyTradeVal);
			account.applyDebit(counterCurrencyTradeVal);
		} else if (m_tradeType == TradeType.Sell) {
			account.applyCredit(counterCurrencyTradeVal);
			account.applyDebit(baseCurrencyTradeVal);
		} else if (m_tradeType == TradeType.ShortSell) {
			account.applyCredit(counterCurrencyTradeVal);
			account.applyDebit(baseCurrencyTradeVal);
		} else if (m_tradeType == TradeType.BuyBack) {
			account.applyCredit(baseCurrencyTradeVal);
			account.applyDebit(counterCurrencyTradeVal);
		}
		
		account.applyDebit(fee);
	}
	
	public String getExchange() {
		return m_exchange;
	}
	
	public BigDecimal getQty() {
		return m_qty;
	}
	
	public BigDecimal getPrice() {
		return m_price;
	}
	
	public TradeType getTradeType() {
		return m_tradeType;
	}
	
	public String toString() {
		String direction = m_tradeType == TradeType.Buy || m_tradeType == TradeType.BuyBack ? "-" : "+";
		BigDecimal dollarVal = m_price.multiply(m_qty);
		BigDecimal fee = dollarVal.multiply(m_fee);
		
		return m_tradeType.toString() + " " + m_currencyPair.toString() + 
			" from " + m_exchange + ": " + m_qty + " @ " + Util.format(m_price) + 
			" (" + direction + " $" + Util.format(dollarVal) + ") " + 
			" (- $" + Util.format(fee) + ")";
	}
}
