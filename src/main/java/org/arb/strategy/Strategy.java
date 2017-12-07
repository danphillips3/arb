package org.arb.strategy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;

import org.arb.AccountInfo;
import org.arb.CalcPartition;
import org.arb.OrderBookInfo;
import org.arb.TradeDetails;
import org.knowm.xchange.currency.Currency;

public abstract class Strategy {
	public abstract ArrayList<TradeDetails> getTrades(CalcPartition partition, OrderBookInfo lastUpdated);
	
	public void setStartingReplayBalance(Collection<AccountInfo> accounts) {
		for (AccountInfo account : accounts) {
			account.updateBalance(Currency.USD, new BigDecimal(1000));
			account.updateBalance(Currency.BTC, new BigDecimal(0.1));
		}
	}
}
