package fr.ortolang.teicorpo;

import java.util.HashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class TeiTimeline {
	// Map pour représenter la timeline
	public HashMap<String, String> timeline;
	double xmaxTime;
	TeiTimeline() {
		timeline = new HashMap<String, String>();
		xmaxTime = 0.0;
	}
	public void buildTimeline(Document teiDoc) {
		timeline.put("T0", "0");
		Element tl = (Element) teiDoc.getElementsByTagName("timeline").item(0);
		String unit = tl.getAttribute("unit");
		double ratio = 1.0;
		if (unit.equals("ms"))
			ratio = 1.0 / 1000.0;
		else if (unit.equals("s"))
			ratio = 1.0;
		else {
			System.out.println("Unité inconnue pour timeline: " + unit);
			System.out.println("Pas de conversion réalisée.");
			ratio = 1.0;
		}
		NodeList whens = tl.getElementsByTagName("when");
		for (int i = 0; i < whens.getLength(); i++) {
			Element when = (Element) whens.item(i);
			if (when.hasAttribute("interval")) {
				String tms = when.getAttribute("interval");
				double vald = Double.parseDouble(tms);
				vald *= ratio;
				if (vald > xmaxTime)
					xmaxTime = vald;
				tms = Utils.printDouble(vald, 10);
//				System.out.println(tms + " --> " + when.getAttribute("xml:id"));
				timeline.put(when.getAttribute("xml:id"), tms);
			} else if (when.hasAttribute("absolute")) {
				String tms = when.getAttribute("absolute");
				double vald = Double.parseDouble(tms);
				vald *= ratio;
				tms = Utils.printDouble(vald, 10);
//				System.out.println(tms + " (abs)--> " + when.getAttribute("xml:id"));
				timeline.put(when.getAttribute("xml:id"), tms);
			}
		}
	}

	public String getTimeValue(String timeId) {
		if (Utils.isNotEmptyOrNull(timeId)) {
			String spl = Utils.refID(timeId);
			return timeline.get(spl);
		}
		return "";
	}

}
