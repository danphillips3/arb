package org.arb;

import java.math.BigDecimal;
import java.util.Scanner;

public class Util {
	private static Scanner reader = new Scanner(System.in);
	
	public static String format(BigDecimal n) {
		return format(n, 2);
	}
	
	public static String format(BigDecimal n, int scale) {
		return n.setScale(scale, BigDecimal.ROUND_HALF_EVEN).toString();
	}
	
	public static void pause() {
		System.out.println("Press enter to continue..");
		reader.nextLine();
	}
}
