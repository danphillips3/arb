package org.arb;

import java.math.*;

import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;

public class TradeDetails {
	private CurrencyPair m_currencyPair;
	private String m_buyExchange;
	private String m_sellExchange;
	private BigDecimal m_bidPrice;
	private BigDecimal m_askPrice;
	private BigDecimal m_qty;
	private BigDecimal m_buyFee;
	private BigDecimal m_sellFee;
	private BigDecimal m_estimatedProfit;
	
	public TradeDetails(CurrencyPair currencyPair, String buyExchange, String sellExchange, 
			BigDecimal bidPrice, BigDecimal askPrice, BigDecimal qty, 
			BigDecimal buyFee, BigDecimal sellFee, BigDecimal estimatedProfit) {
		m_currencyPair = currencyPair;
		m_buyExchange = buyExchange;
		m_sellExchange = sellExchange;
		m_bidPrice = bidPrice;
		m_askPrice = askPrice;
		m_qty = qty;
		m_buyFee = buyFee;
		m_sellFee = sellFee;
		m_estimatedProfit = estimatedProfit;
	}
	
	public String getBuyExchange() {
		return m_buyExchange;
	}
	
	public String getSellExchange() {
		return m_sellExchange;
	}
	
	public MoneyAmount getBuyCredit() {
		return new MoneyAmount(m_currencyPair.base, m_qty);
	}
	
	public MoneyAmount getSellCredit() {
		return new MoneyAmount(m_currencyPair.counter, m_qty.multiply(m_bidPrice).subtract(m_sellFee));
	}
	
	public MoneyAmount getBuyDebit() {
		return new MoneyAmount(m_currencyPair.counter, m_qty.multiply(m_askPrice).add(m_buyFee));
	}
	
	public MoneyAmount getSellDebit() {
		return new MoneyAmount(m_currencyPair.base, m_qty);
	}
	
	public BigDecimal getEstimatedProfit() {
		return m_estimatedProfit;
	}
	
	public String toString() {
		return m_currencyPair.toString() + 
				" BUY " + m_buyExchange + " @ " + m_askPrice + " / " +
				" SELL " + m_sellExchange + " @ " + m_bidPrice + " " +
				", Qty = " + m_qty.setScale(5, BigDecimal.ROUND_DOWN) + 
				", Profit = " + m_estimatedProfit.setScale(5, BigDecimal.ROUND_DOWN);
	}
}
