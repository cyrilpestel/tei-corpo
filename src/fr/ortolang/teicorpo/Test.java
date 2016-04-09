package fr.ortolang.teicorpo;

import java.util.ArrayList;
import java.util.List;

class Internal {
	public String s;
	public String toString() {
		return s;
	}
}

public class Test {
	static String mod(String s) {
		s += "-modif";
		return s;
	}
	static void modlist(List<String> l) {
		l.add("modif");
	}
	static void modint(Internal i) {
		i.s += "-modif";
	}
	
	public static void main(String args[]) {
		String a = "abcdef";
		System.out.println(a);
		String b = mod(a);
		System.out.println(a);
		System.out.println(b);
		List<String> la = new ArrayList<String>();
		la.add("abcdef");
		System.out.println(la);
		modlist(la);
		System.out.println(la);
		Internal ia = new Internal();
		ia.s = "abcdef";
		System.out.println(ia);
		modint(ia);
		System.out.println(ia);
	}
}
