package com.nnacres.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
//import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import org.json.JSONException;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;

/**
 * Root resource (exposed at "monitor" path)
 */
@Path("monitor")
public class Service {
	// private int cacheTime = 10;
	// private CacheControl control = new CacheControl() {
	// {
	// }
	// };
	@SuppressWarnings("serial")
	private Map<String, Integer> errorList = new HashMap<String, Integer>() {
		{
			put("invalid server name", 404);
			put("sql error", 102);
			put("old data", 103);
			put("redis error", 104);
			put("invalid arguments", 105);
		}
	};

	/**
	 * Method handling HTTP GET requests. The returned object will be sent to
	 * the client as "text/plain" media type.
	 * 
	 * @return String that will be returned as an application/json response.
	 * @throws JSONException
	 * @throws ParseException
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{alias}")
	public Response get(@PathParam("alias") String alias,
			@Context UriInfo uriInfo) throws JSONException, ParseException {
		String query = null;
		if (uriInfo != null)
			query = uriInfo.getRequestUri().getQuery();
		// control.setMaxAge(cacheTime);
		return perform(alias, query);
	}

	Response perform(String alias, String query) throws JSONException,
			ParseException {
		try {
			Jedis jedis = new Jedis("localhost");
			String value = jedis.lindex(alias+";monitor", 0);
			jedis.close();

			if (value == null) {
				JSONObject json = new JSONObject(
						"{\"error\":true,\"error_message\":\"invalid server name\"}");
				json.put("error_number", errorList.get("invalid server name"));
				return Response.status(Status.NOT_FOUND)
						.entity(json.toString()).build();// .cacheControl(control).build();
				// return json;
			}

			JSONObject json = new JSONObject(value);
			if ((boolean) json.get("error")) {
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(json.toString()).build();// .cacheControl(control).build();
			}

			boolean flag = true;
			if (!checkTimestamp(json.getString("time")))
				flag = false;

			if (query == null || query.length() < 8) {
				if (flag) {
					return Response.status(Status.OK).entity(json.toString())
							.build();// .cacheControl(control).build();
				}
				json.put("error_message", "data is old");
				json.put("error_number", errorList.get("old data"));
				json.put("error", true);
				return Response.status(Status.PARTIAL_CONTENT)
						.entity(json.toString()).build();// .cacheControl(control).build();
			}

			String[] params = getParams(query);
			return getOutput(params, json, flag);
		} catch (Exception e) {
			JSONObject json = new JSONObject();
			json.put("error_message", e.getMessage() + " redis error");
			json.put("error_number", errorList.get("redis error"));
			json.put("error", true);
			return Response.status(Status.SERVICE_UNAVAILABLE)
					.entity(json.toString()).build();// .cacheControl(control).build();
		}
	}

	private Response getOutput(String[] params, JSONObject json, boolean newData)
			throws JSONException {
		Iterator<?> keys = json.keys();
		JSONObject result = new JSONObject();
		int flag = 0;
		while (keys.hasNext()) {
			String key = (String) keys.next();
			for (int j = 0; j < params.length; j++) {
				String param = params[j];
				if (key.toLowerCase().equals(param.toLowerCase())) {
					result.put(key, json.get(key));
					flag++;
				}
			}
		}
		if (flag == 0) {
			result.put("error", true);
			result.put("error_message", "all arguments are invalid");
			result.put("error_number", errorList.get("invalid arguments"));
			return Response.status(Status.BAD_REQUEST)
					.entity(result.toString()).build();// .cacheControl(control).build();
		}
		result.put("time", json.get("time"));
		if (!newData) {
			result.put("error", true);
			result.put("error_number", errorList.get("old data"));
			result.put("error_message", "data is old");
			return Response.status(Status.PARTIAL_CONTENT)
					.entity(result.toString()).build();// .cacheControl(control).build();
		}

		result.put("error", false);
		return Response.status(Status.OK).entity(result.toString()).build();// .cacheControl(control).build();
	}

	private boolean checkTimestamp(String checkTime) throws ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss yyyy-MM-dd");
		Date redisDate = formatter.parse(checkTime);
		Date newDate = new Date();
		long check = (newDate.getTime() - redisDate.getTime()) / 1000;
		if (check > 300)
			return false;
		return true;
	}

	private String[] getParams(String query) {
		query = query.substring(7);
		String[] params = query.split("&");
		return params;
	}
}