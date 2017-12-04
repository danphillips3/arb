package org.arb;

import java.math.BigDecimal;
import org.knowm.xchange.currency.Currency;

public class MoneyAmount {
	private Currency m_currency;
	private BigDecimal m_amount;
	
	public MoneyAmount(Currency currency, BigDecimal amount) {
		m_currency = currency;
		m_amount = amount;
	}
	
	public Currency getCurrency() {
		return m_currency;
	}
	
	public BigDecimal getAmount() { 
		return m_amount;
	}
}
