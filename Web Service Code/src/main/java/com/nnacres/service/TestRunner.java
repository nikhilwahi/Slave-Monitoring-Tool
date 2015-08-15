package com.nnacres.service;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

public class TestRunner {
	public static void main(String[] args) {
		Result result = JUnitCore.runClasses(ServiceTest.class);
		int check = 5;
		for (Failure failure : result.getFailures()) {
			if (failure.toString().indexOf("Redis") != -1)
				System.out.println("Redis is running");
			else {
				System.out.println(failure.toString());
				check--;
			}
		}
		System.out.println((check == 5));
	}
}