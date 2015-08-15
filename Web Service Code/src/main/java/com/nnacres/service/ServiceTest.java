package com.nnacres.service;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.json.JSONObject;

import junit.framework.TestCase;

public class ServiceTest extends TestCase {
	private static final UriInfo URIInfo = null;

	public void testRedis() {
		Service s1 = new Service();
		try {
			Response a = s1.perform("catch", "");
			JSONObject t = new JSONObject(a.getEntity().toString());
			if ((boolean) (t.get("error"))) {
				assertEquals(104, t.get("error_number"));
				if (104 == t.getInt("error_number")) {
					System.out
							.println("redis connection has failed. Further tests won't be performed");
					System.out.println(t);
					System.exit(0);
				}
			} else {
				System.out.println("redis connection hasn't failed");
			}
		} catch (Exception e) {
			System.out.println("redis connection hasfailed");

			e.printStackTrace();
		}
	}

	public void testIncorrectSeverName() {
		Service s1 = new Service();
		try {
			Response a = s1.get("wrong name", URIInfo);
			JSONObject t= new JSONObject(a.getEntity().toString());
			assertEquals(404,
					t.getInt("error_number"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void testNotNull() {
		Service s1 = new Service();
		try {
			assertNotNull("response is given", s1.get("", URIInfo));
			assertNotNull("response is given", s1.get("test", URIInfo));
			assertNotNull("response is given", s1.get("vm2", URIInfo));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void testIncorrectArgs() {
		Service s1 = new Service();
		try {
			Response a = s1.perform("vm2", "values=wrong&wrong&wrong");
			JSONObject t = new JSONObject(a.getEntity().toString());
			if (t.getBoolean("error")) {
				if (t.getInt("error_number") != 102) {
					assertEquals(105, t.getInt("error_number"));
				} else
					System.out.println("sql connection failed at cron");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void testOld() {
		Service s1 = new Service();
		try {
			Response a = s1.perform("vm2", "");
			JSONObject t = new JSONObject(a.getEntity().toString());
			if ((boolean) t.get("error")) {
				if (t.getInt("error_number") != 102) {
					assertEquals(105, t.getInt("error_number"));
				} else
					System.out.println("sql connection failed at cron");
			} else {
				System.out.println("the data was not old");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void testSqlError() {
		Service s1 = new Service();
		try {
			Response a = s1.perform("06", "");
			JSONObject t = new JSONObject(a.getEntity().toString());
			System.out.println(t);
			if ((boolean) t.get("error")) {
				assertEquals(102, (int) t.get("error_number"));
				
			} else {
				System.out.println("sql connection hasn't failed");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}