package org.arb;

import java.sql.*;

public class DBManager {
	private static Connection m_connection;
	
	static {
		try {
			System.out.println("Connecting to DB");
			Class.forName("com.mysql.jdbc.Driver");  
			
			m_connection = DriverManager.getConnection("jdbc:mysql://ec2-18-216-220-109.us-east-2.compute.amazonaws.com:3306/arb","dphillips","test123");  
			System.out.println("Successfully connected to DB");
		} catch (Exception e) {
			System.out.println(e);
		}
	}
	
	public static void insert(OrderBookInfo orderBook) {
		String sql = 
			"INSERT INTO quotes " + 
			"(currency_base, currency_counter, exchange, bid_px, ask_px, bid_qty, ask_qty, ts) VALUES (" +
			"'" + orderBook.getCurrencyPair().base.toString() + "'," +
			"'" + orderBook.getCurrencyPair().counter.toString() + "'," +
			"'" + orderBook.getExchange() + "'," +
			orderBook.getBestBid().toString() + "," +
			orderBook.getBestAsk().toString() + "," +
			orderBook.getBestBidQty().toString() + "," +
			orderBook.getBestAskQty().toString() + "," + 
			"now());";
		
		executeUpdateSql(sql);
	}
	
	private static void executeUpdateSql(String sql) {
		System.out.println("Executing SQL: " + sql);
		try {
			Statement stmt = m_connection.createStatement();  
			stmt.executeUpdate(sql);
		} catch (Exception e) {
			System.out.println("SQL update failed: " + e);
		}
	}
}
