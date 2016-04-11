package fr.ortolang.teicorpo;

import java.util.HashSet;

class TierParams {
	String input;
	String output;
	HashSet<String> doDisplay;
	HashSet<String> dontDisplay;
	int level;
	TierParams() {
		input = null;
		output = null;
		doDisplay = new HashSet<String>();
		dontDisplay = new HashSet<String>();
		level = 0; // all levels
	}
	void addDoDisplay(String s) {
		doDisplay.add(s.toLowerCase());
	}
	void addDontDisplay(String s) {
		dontDisplay.add(s.toLowerCase());
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
