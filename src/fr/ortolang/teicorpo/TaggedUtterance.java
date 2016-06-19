package fr.ortolang.teicorpo;

import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class TaggedUtterance {
	ArrayList<TaggedWord> twL;
	
	TaggedUtterance() {
		twL = new ArrayList<TaggedWord>();
	}
	
	void reset() {
		twL = new ArrayList<TaggedWord>();
	}
	
	void add(String[] wcl) {
		TaggedWord tw = new TaggedWord(wcl);
		twL.add(tw);
	}

	public Element createSpan(Document teiDoc) {
		Element span = teiDoc.createElement("span");
		if (twL == null) return span;
		for (int i=0; i < twL.size(); i++) {
			TaggedWord c = twL.get(i);
			Element w = teiDoc.createElement("w");
			w.setTextContent(c.word);
			w.setAttribute("type", c.pos);
			w.setAttribute("lemma", c.lemma);
			span.appendChild(w);
		}
		return span;
	}

	public String toString() {
		String s ="";
		if (twL == null) return s;
		for (int i=0; i < twL.size(); i++) {
			TaggedWord c = twL.get(i);
			s += c.toString() + "\n";
		}
		return s;
	}

}
