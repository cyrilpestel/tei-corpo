package fr.ortolang.teicorpo;

import java.util.Comparator;

import org.w3c.dom.Element;

public class CompareAnnotU implements Comparator<Element> {

	HierarchicTrans ht;

	CompareAnnotU(HierarchicTrans h) {
		ht = h;
	}

	@Override
	public int compare(Element o1, Element o2) {
		// TODO Auto-generated method stub
		try {
			String v1 = Utils.getAttrAnnotationBloc(o1, "start");
			String v2 = Utils.getAttrAnnotationBloc(o2, "start");
			if (!Utils.isNotEmptyOrNull(v1) && !Utils.isNotEmptyOrNull(v2))
				return 0;
			if (!Utils.isNotEmptyOrNull(v1))
				return -1;
			if (!Utils.isNotEmptyOrNull(v2))
				return 1;
			String h1 = ht.getTimeValue(v1);
			String h2 = ht.getTimeValue(v2);
			if (!Utils.isNotEmptyOrNull(h1) && !Utils.isNotEmptyOrNull(h2))
				return 0;
			if (!Utils.isNotEmptyOrNull(h1))
				return -1;
			if (!Utils.isNotEmptyOrNull(h2))
				return 1;
			Double start1 = Double.parseDouble(h1);
			Double start2 = Double.parseDouble(h2);
			if (start1 > start2) {
				return 1;
			} else if (start1 < start2) {
				return -1;
			} else {
				return 0;
			}
		} catch (Exception e) {
			System.err.println("Cannot convert double: " + e.toString());
			return 0;
		}
	}

}
