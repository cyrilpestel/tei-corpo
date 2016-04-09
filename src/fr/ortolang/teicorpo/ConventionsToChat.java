package fr.ortolang.teicorpo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConventionsToChat {
	
	public static String noise(String s){
		Pattern p1 = Pattern.compile("(\\s|^\\s*)(\\*) ([^/\\*]*) (B/\\*)");
		Matcher m = p1.matcher(s);
		while (m.find()){
			if(s.startsWith(m.group())){
				s = s.replace(m.group(), " 0 [=! " + m.group(3) + "]");
			}
			else{
				s = s.replace(m.group(), " [=! " + m.group(3) + "]");
			}			
		}
		return s;
	}
	
	public static String pauses(String s){
		//Pause non chrono
		Pattern p1 = Pattern.compile("(\\s|^)(#+)(\\s|$)");
		Matcher m1 = p1.matcher(s);
		while(m1.find()){
			String rep = m1.group(2).replaceAll("#", ".");
			s = s.replace(m1.group(), " (" + rep + ") ");
		}		
		//Pause chrono
		Pattern p2 = Pattern.compile("(\\s|^)(#)(\\d+\\.\\d*)(\\s|$)");
		Matcher m2 = p2.matcher(s);
		while(m2.find()){
			s = s.replace(m2.group(), " (" + m2.group(3) + ") ");
		}
		return s;
	}
	
	public static String term(String s){
		String patternStr = "#(\\+\\.\\.\\.|\\+/\\.|\\+!\\?|\\+//\\.|\\+/\\?|\\+\"/\\.|\\+\"\\.|\\+//\\?|\\+\\.\\.\\?|\\+\\.|\\.|\\?|!\\s*$)";
		Pattern pattern = Pattern.compile(patternStr);
		Matcher matcher = pattern.matcher(s);
		if (matcher.find()) {
			s  = s.replace(matcher.group(), matcher.group(1));
		}
		return s;
	}
	
	
	public static String setConv(String s){
		s = pauses(s);		
		s = noise(s);
		s = term(s);
		return Utils.cleanString(s);
	}
	
	public static void main(String [] args){
		
		System.out.println(setConv(" tr * Event|noise|desc:pi extent:previous B/* quand on #2. s'en re-souvient c'est c'est amélioré #.!"));
	}
}