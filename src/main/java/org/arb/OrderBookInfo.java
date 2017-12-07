package org.arb;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.trade.LimitOrder;

public class OrderBookInfo {
	private String m_exchange;
	private CurrencyPair m_currencyPair;
	private OrderBook m_orderBook;
	private BigDecimal m_bestBid;
	private BigDecimal m_bestAsk;
	private BigDecimal m_bestBidQty;
	private BigDecimal m_bestAskQty;
	
	public OrderBookInfo(String exchange, CurrencyPair currencyPair, OrderBook orderBook) {
		m_exchange = exchange;
		m_currencyPair = currencyPair;
		m_orderBook = orderBook;
		
		List<LimitOrder> bids = m_orderBook.getBids();
		List<LimitOrder> asks = m_orderBook.getAsks();
		
		if (!bids.isEmpty()) {
			m_bestBid = bids.get(0).getLimitPrice();
		} else {
			m_bestBid = BigDecimal.ZERO;
		}
		
		if (!asks.isEmpty()) {
			m_bestAsk = asks.get(0).getLimitPrice();
		} else {
			m_bestAsk = BigDecimal.ZERO;
		}
		
		m_bestBidQty = BigDecimal.ZERO;
		for (LimitOrder bid : bids) {
			if (bid.getLimitPrice().equals(m_bestBid)) {
				m_bestBidQty = m_bestBidQty.add(bid.getRemainingAmount());
			} else {
				break;
			}
		}
		
		m_bestAskQty = BigDecimal.ZERO;
		for (LimitOrder ask : asks) {
			if (ask.getLimitPrice().equals(m_bestAsk)) {
				m_bestAskQty = m_bestAskQty.add(ask.getRemainingAmount());
			} else {
				break;
			}
		}
	}
	
	public OrderBookInfo(ResultSet row) throws SQLException {
		m_exchange = row.getString("exchange");
		m_bestBid = row.getBigDecimal("bid_px");
		m_bestAsk = row.getBigDecimal("ask_px");
		m_bestBidQty = row.getBigDecimal("bid_qty");
		m_bestAskQty = row.getBigDecimal("ask_qty");
		
		Currency base = new Currency(row.getString("currency_base"));
		Currency counter = new Currency(row.getString("currency_counter"));
		m_currencyPair = new CurrencyPair(base, counter);
	}
	
	public OrderBookInfo(String serialized) {
		deserialize(serialized);
	}
	
	public String getExchange() { return m_exchange; }
	public CurrencyPair getCurrencyPair() { return m_currencyPair; }
	public OrderBook getOrderBook() { return m_orderBook; }
	public BigDecimal getBestBid() { return m_bestBid; }
	public BigDecimal getBestAsk() { return m_bestAsk; }
	public BigDecimal getBestBidQty() { return m_bestBidQty; }
	public BigDecimal getBestAskQty() { return m_bestAskQty; }
	
	public String toString() {
		return m_currencyPair.toString() + " " + m_exchange +
				": bid=" + m_bestBid + ", ask=" + m_bestAsk + 
				", bidQty=" + m_bestBidQty + ", askQty=" + m_bestAskQty; 
	}
	
	public String serialize() {
		return "OrderBookInfo," + m_exchange + "," + m_currencyPair.toString() + "," + 
				m_bestBid + "," + m_bestAsk + "," + m_bestBidQty + "," + m_bestAskQty;
	}
	
	public void deserialize(String raw) {
		String[] split = raw.split(",");
		m_exchange = split[1];
		
		String[] currencyPairSplit = split[2].split("/");
		Currency base = new Currency(currencyPairSplit[0]);
		Currency counter = new Currency(currencyPairSplit[1]);
		m_currencyPair = new CurrencyPair(base, counter);
		
		m_bestBid = new BigDecimal(split[3]);
		m_bestAsk = new BigDecimal(split[4]);
		m_bestBidQty = new BigDecimal(split[5]);
		m_bestAskQty = new BigDecimal(split[6]);
	}
}
