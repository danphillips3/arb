package org.arb;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.service.account.AccountService;

public class AccountInfo {
	private String m_name;
	private AccountService m_accountService;
	private Map<Currency, BigDecimal> m_currencyMap = new ConcurrentHashMap<Currency, BigDecimal>();
	
	public AccountInfo(String name, AccountService accountService) {
		m_name = name;
		m_accountService = accountService;
	}
	
	public void updateBalance(Currency currency, BigDecimal val) {
		m_currencyMap.put(currency, val);
	}
	
	public BigDecimal getBalance(Currency currency) {
		BigDecimal balance = m_currencyMap.get(currency);
		if (balance == null) {
			return BigDecimal.ZERO;
		}
		return balance;
	}
	
	public BigDecimal getTransactionRate() {
		return new BigDecimal(0.0025);
	}
	
	public boolean isMarginTradingAllowed() { 
		return true;
	}
	
	public Set<Currency> getCurrencies() {
		return m_currencyMap.keySet();
	}
	
	public void applyCredit(MoneyAmount amount) {
		BigDecimal origBalance = getBalance(amount.getCurrency());
		BigDecimal newBalance = origBalance.add(amount.getAmount());
		updateBalance(amount.getCurrency(), newBalance);
	}
	
	public void applyDebit(MoneyAmount amount) {
		BigDecimal origBalance = getBalance(amount.getCurrency());
		BigDecimal newBalance = origBalance.subtract(amount.getAmount());
		updateBalance(amount.getCurrency(), newBalance);
	}
}
