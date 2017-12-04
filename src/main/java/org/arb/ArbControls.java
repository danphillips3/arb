package org.arb;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.knowm.xchange.currency.Currency;

public class ArbControls {
	private static BigDecimal m_profitThreshold;
	private static Map<Currency, BigDecimal> m_usdRates = new HashMap<Currency, BigDecimal>();
	
	static {
		m_profitThreshold = new BigDecimal(2);
		
		m_usdRates.put(Currency.USD, new BigDecimal(1));
		m_usdRates.put(Currency.BTC, new BigDecimal(10000));
	}
	
	public static BigDecimal getProfitThreshold() {
		return m_profitThreshold;
	}
	
	public static BigDecimal getUsdRate(Currency currency) {
		BigDecimal rate = m_usdRates.get(currency);
		if (rate == null) {
			System.out.println("WARNING: usd rate not found -> " + currency.toString());
			return BigDecimal.ZERO;
		}
		return rate;
	}
}
