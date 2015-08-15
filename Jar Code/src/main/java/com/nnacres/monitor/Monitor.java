package com.nnacres.monitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import redis.clients.jedis.Jedis;

class Monitor {
	private String query = "SHOW SLAVE STATUS";
	private String[] params = { "Seconds_Behind_Master", "Exec_Master_Log_Pos",
			"Slave_IO_Running", "Slave_SQL_Running" };
	private long LIST_SIZE = 100;
	private SimpleDateFormat formatter = new SimpleDateFormat(
			"HH:mm:ss yyyy-MM-dd");

	@SuppressWarnings("unchecked")
	void perform() throws ParseException, ClassNotFoundException {
		try {
			JSONParser parser = new JSONParser();
			InputStream in = Thread.currentThread().getContextClassLoader()
					.getResourceAsStream("resources/database.json");
			Reader reader = new BufferedReader(new InputStreamReader(in));
			Object object = parser.parse(reader);
			JSONArray jsonArray = (JSONArray) object;
			Iterator<?> i = jsonArray.iterator();
			while (i.hasNext()) {
				JSONObject current = (JSONObject) i.next();
				JSONObject obj = sql((String) current.get("username"),
						(String) current.get("password"),
						(String) current.get("ip"));
				redis(obj, (String) current.get("alias"));
			}
		} catch (IOException e) {
			e.printStackTrace();
			JSONObject obj = new JSONObject();
			obj.put("error_message", e.getMessage());
			obj.put("error", true);
			redis(obj, null);
		}

	}

	@SuppressWarnings("unchecked")
	private JSONObject sql(String username, String password, String ip)
			throws ClassNotFoundException {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection conn = DriverManager.getConnection("jdbc:mysql://" + ip,
					username, password);
			Statement myst = conn.createStatement();
			ResultSet resultSet = myst.executeQuery(this.query);
			return convert(resultSet);
		} catch (SQLException e) {
			// e.printStackTrace();
			JSONObject obj = new JSONObject();
			obj.put("error", true);
			obj.put("error_message", e.getMessage());
			obj.put("error_number", e.getErrorCode());
			return obj;
		}
	}

	@SuppressWarnings("unchecked")
	private JSONObject convert(ResultSet resultSet) throws SQLException {
		JSONObject obj = new JSONObject();
		if (resultSet.first()) {
			for (int i = 0; i < params.length; i++) {
				String insert = resultSet.getString(params[i]);
				if (insert != null)
					obj.put(params[i], insert);
				// else
				// obj.put(params[i], "NULL");
			}
		}
		obj.put("error", false);
		return obj;
	}

	@SuppressWarnings({ "unchecked", "unused" })
	private void redis(JSONObject obj, String alias) {
		try {
			alias=alias+";monitor";
			Date newDate = new Date();
			obj.put("time", formatter.format(newDate));
			Jedis jedis = new Jedis("localhost");
			List<String> checker = null;
			if ((boolean) obj.get("error")) {
				if (alias != null) {
					jedis.lpush(alias, obj.toString());
					jedis.ltrim(alias, 0, LIST_SIZE);
					checker = jedis.lrange(alias, 0, 9);
					continuousFailureCheck(checker, alias);
				} else
					System.out.print(obj.get("error_message"));
			} else {
				obj.put("error", false);
				jedis.lpush(alias, obj.toString());
				jedis.ltrim(alias, 0, LIST_SIZE);
			}
			jedis.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("rawtypes")
	private void continuousFailureCheck(List<String> checker, String alias) {
		Iterator it = checker.iterator();
		int counter = 0;
		while (it.hasNext()) {
			String string = (String) (it.next());
			Object obj = JSONValue.parse(string);
			JSONObject json = (JSONObject) obj;
			if ((boolean) json.get("error"))
				counter++;
		}
		if (counter >= 10) {
			System.out.println(alias);
			sendErrorEmail(alias);
		}
	}

	private void sendErrorEmail(String alias) {
		// TODO implement this method to send an email with subject 'Monitoring
		// tool: 'alias' failure
	}
}