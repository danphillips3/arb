package org.arb;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TaskPool {
	private static ExecutorService m_executor = Executors.newFixedThreadPool(5);
	
	public static Future<?> addTask(Runnable task) {
		try {
			return m_executor.submit(task);
		} catch (Exception e) {
			System.out.println("Exception adding task: " + e.getMessage());
		}
		return null;
	}
	
	public static ArrayList<Future<?>> addTasks(ArrayList<Runnable> tasks) {
		ArrayList<Future<?>> taskResults = new ArrayList<Future<?>>();
		for (Runnable task : tasks) {
			try {
				taskResults.add(m_executor.submit(task));
			} catch (Exception e) {
				System.out.println("Exception adding task: " + e.getMessage());
			}
		}	
		return taskResults;
	}
}
