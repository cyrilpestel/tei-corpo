package fr.ortolang.teicorpo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

class Internal {
	public String s;
	public String toString() {
		return s;
	}
}

public class Test {
	public static void main(String args[]) {
		URL url = null;
		try {
			url = new URL(args[0]);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    try {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"))) {
					for (String line; (line = reader.readLine()) != null;) {
					    System.out.println(line);
					}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
