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
		
		updateBalance(Currency.USD, new BigDecimal(1000));
		updateBalance(Currency.BTC, new BigDecimal(0.1));
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
		
		if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
			System.out.println("WARNING: " + m_name + " Balance < 0 (" + newBalance + ") " + amount.getCurrency().toString());
		}
	}
}
