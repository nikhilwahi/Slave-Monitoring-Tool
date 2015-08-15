package com.nnacres.monitor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.json.simple.parser.ParseException;

public class Caller {
	public static void main(String[] args) throws ParseException,
			ClassNotFoundException {
		long start=System.currentTimeMillis();
		multipleRunning();
		Monitor test = new Monitor();
		test.perform();
		long end=System.currentTimeMillis();
		System.out.print(end-start+" milliseconds");
	}

	private static void multipleRunning() {
		Process p;
		try {
			String[] cmd = { "/bin/sh", "-c", "ps xau | grep -c 99AcresMonitorTool" };
			p = Runtime.getRuntime().exec(cmd);
			BufferedReader br = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			String toConvert = br.readLine();
			int test = 0;
			if (toConvert != null)
				test = Integer.parseInt(toConvert);
			System.out.println(test);
			if (test > 3) {
				String[] cmd2 = { "/bin/sh", "-c",
						"ps aux | grep monitor| awk '{ print $2 }'" };
				p = Runtime.getRuntime().exec(cmd2);
				br = new BufferedReader(new InputStreamReader(
						p.getInputStream()));
				String s;
				List<String> list = new ArrayList<String>();
				while ((s = br.readLine()) != null) {
					list.add(s);
				}
				list.remove(list.size() - 2);
				Iterator<?> it = list.iterator();
				while (it.hasNext()) {
					String[] cmd3 = { "/bin/sh", "-c", "kill -9 " + it.next() };
					p = Runtime.getRuntime().exec(cmd3);
				}
			}
			p.destroy();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}