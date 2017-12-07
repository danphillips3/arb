package org.arb;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DBManager {
	private static Connection m_connection;
	
	static {
		try {
			System.out.println("Connecting to DB");
			Class.forName("com.mysql.jdbc.Driver");  
			
			m_connection = DriverManager.getConnection("jdbc:mysql://ec2-18-216-220-109.us-east-2.compute.amazonaws.com:3306/arb","dphillips","test123");  
			System.out.println("Successfully connected to DB");
		} catch (Exception e) {
			Main.exitWithError("Could not connect to DB: " + e);
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
	
	public static ArrayList<OrderBookInfo> loadOrderBookUpdates(List<String> exchanges, java.util.Date start, java.util.Date end) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		String sql = "SELECT currency_base, currency_counter, exchange, bid_px, ask_px, bid_qty, ask_qty, ts " +
			"FROM quotes where ts > '" + format.format(start) + "' AND ts < '" + format.format(end) + "' " + 
			"AND exchange in (" + toSqlList(exchanges) + ")";
		ResultSet result = executeQuery(sql);
		
		ArrayList<OrderBookInfo> updates = new ArrayList<OrderBookInfo>();
		try {
			while (result.next()) {
				updates.add(new OrderBookInfo(result));
			}
		} catch (Exception e) {
			System.out.println("Error parsing order book info: " + e);
		}
		return updates;
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
	
	private static ResultSet executeQuery(String sql) {
		System.out.println("Executing SQL: " + sql);
		
		ResultSet result = null;
		try {
			Statement stmt = m_connection.createStatement();  
			result = stmt.executeQuery(sql);
		} catch (Exception e) {
			System.out.println("SQL query failed: " + e);
		}
		return result;
	}
	
	private static String toSqlList(List<?> list) {
		String sqlStr = String.join(",", list.stream().map((e) -> "'" + e.toString() + "'").collect(Collectors.toList()));
		return sqlStr;
	}
}
