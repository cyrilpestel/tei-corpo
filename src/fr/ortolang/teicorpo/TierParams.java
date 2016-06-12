package fr.ortolang.teicorpo;

import java.util.HashSet;
import java.util.TreeMap;

class TierParams {
	boolean forceEmpty;
	boolean cleanLine;
	boolean sectionDisplay;
	String input;
	String output;
	String mediaName;
	String encoding;
	String options;
	boolean nospreadtime;
	boolean detectEncoding;
	HashSet<String> commands;  // for -c parameter
	HashSet<String> doDisplay;
	HashSet<String> dontDisplay;
	TreeMap<String, String> tv;
	int level;
	public boolean raw;
	public boolean noHeader;
	TierParams() {
		input = null;
		output = null;
		mediaName = null;
		encoding = null;
		detectEncoding = true;
		commands = new HashSet<String>();
		doDisplay = new HashSet<String>();
		dontDisplay = new HashSet<String>();
		level = 0; // all levels
		forceEmpty = true;
		cleanLine = false;
		noHeader = false;
		raw = false;
		sectionDisplay = false;
		options = "";
		nospreadtime = false;
		tv = new TreeMap<String, String>();
	}
	void addCommand(String s) {
		commands.add(s);
	}
	void addDoDisplay(String s) {
		doDisplay.add(s.toLowerCase());
	}
	void addDontDisplay(String s) {
		dontDisplay.add(s.toLowerCase());
	}
	void addTv(String info) {
		int p = info.indexOf(":");
		if (p<1 || p >= info.length()) {
			System.err.println("error: txm information ignored (missing :) => " + info);
		} else {
			tv.put(info.substring(0, p), info.substring(p+1));
		}
	}
	void setLevel(int l) {
		level = l;
	}
	boolean isLevel(int t) {
		return (level == 0 | t <= level) ? true : false; // si level == 0 all else if 't' is <= level
	}
	private static boolean test(String s, HashSet<String> dd) {
		s = s.toLowerCase();
		for (String f: dd) {
			if (f.startsWith("*") && f.endsWith("*")) {
				if (s.indexOf(f.substring(1, f.length()-2)) >= 0) return true;
			} else if (f.startsWith("*")) {
				if (s.indexOf(f.substring(1)) >= 0) return true;
			} else if (f.endsWith("*")) {
				if (s.indexOf(f.substring(0, f.length()-1)) >= 0) return true;
			} else {
				if (s.equals(f)) return true;
			}
		}
		return false;
	}
	boolean isDoDisplay(String s) {
		if (doDisplay.size() < 1) return true;
		return TierParams.test(s, doDisplay);
	}
	boolean isDontDisplay(String s) {
		if (dontDisplay.size() < 1) return false;
		return TierParams.test(s, dontDisplay);
	}
}
