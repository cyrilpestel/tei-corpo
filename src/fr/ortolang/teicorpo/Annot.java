package fr.ortolang.teicorpo;

import java.util.ArrayList;

public class Annot{
	String id;
	String name;
	String start;
	String end;
	ArrayList<Annot> dependantAnnotations;
	String content;
	String link;
	String previous;
	String timereftype; // time ou ref

	public Annot (){
		id="";
		name="";
		start="";
		end="";
		dependantAnnotations=new ArrayList<Annot>();
		content = "";
		link="";
		timereftype="";
	}

	public Annot(String tierName, String value){
		id="";
		name = tierName;
		start = "";
		end = "";
		dependantAnnotations=null;
		content = value;
		link="";
		timereftype="";
	}

	public void setTime(long beginTime, long endTime){
        /*
         * correct values if end time before begin time
         */
        if (endTime < beginTime && endTime >= 0) {
        	long sw = endTime;
            endTime = beginTime;
            beginTime = sw;
        }
		start = Long.toString(beginTime, 10);
		end = Long.toString(endTime, 10);
	}

	public Annot(String tierName, String value, long beginTime, long endTime){
		id="";
		name = tierName;
        /*
         * correct values if end time before begin time
         */
        if (endTime < beginTime && endTime >= 0) {
        	long sw = endTime;
            endTime = beginTime;
            beginTime = sw;
        }
		start = Long.toString(beginTime, 10);
		end = Long.toString(endTime, 10);
		dependantAnnotations=new ArrayList<Annot>();
		content = value;
		link="";
		timereftype="time";
	}

	public Annot(String tierName, String value, String beginTime, String endTime){
		id="";
		name = tierName;
        /*
         * correct values if end time before begin time
         */
		float beginTimeFloat = Float.parseFloat(beginTime);
		float endTimeFloat = Float.parseFloat(endTime);
        if (endTimeFloat<beginTimeFloat  && endTime != "-1") {
        	String sw = endTime;
            endTime = beginTime;
            beginTime = sw;
        }
        
		start = beginTime;
		end = endTime;
		dependantAnnotations=new ArrayList<Annot>();
		content = value;
		link="";
		timereftype="time";
	}

	public String toString(){
		String s = "NAME = " + name + "; ID = " + id ; 
		if(timereftype.equals("ref")){
			s += "; LINK = " + link;
		}
		else {
			s += "; START = " + start + "; END = " + end;
		}
		s += "; CONTENT = " + content;
		int i = 0;
		while(i < dependantAnnotations.size()){				
			s += "\n\t" + dependantAnnotations.get(i).toString();
			i++;
			if (i>=10) {
				s += "\n\t...";
				break;
			}
		}
		return s;
	}

	public String tierToString(){
		String s = "\tTIER: " + name + " "; 
		if(timereftype.equals("ref")){
			s += "; LINK: " + link;
		}
		else {
			s += "; START: " + start + "; END: " + end;
		}
		s += "; CONTENT: " + content;
		return s;
	}

	public String AnnotToString(String s){
		s += "NAME = " + name + "; ID = " + id ; 
		if(timereftype.equals("ref")){
			s += "; LINK = " + link;
		}
		else {
			s += "; START = " + start + "; END = " + end;
		}
		s += "; CONTENT = " + content;
		int i = 0;
		while(i<dependantAnnotations.size()){				
			s += "\n\t" + dependantAnnotations.get(i).AnnotToString(s);
			i++;
		}
		return s;
	}
}