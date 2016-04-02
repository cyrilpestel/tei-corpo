package fr.ortolang.teicorpo;

import java.util.Comparator;

import org.w3c.dom.Element;

public class CompareAnnotU implements Comparator<Element>{

	HierarchicTrans ht;

	CompareAnnotU(HierarchicTrans h){
		ht = h;
	}

	@Override
	public int compare(Element o1, Element o2) {
		// TODO Auto-generated method stub
		try{
			String v1 = Utils.getAttrAnnotationBloc(o1, "start");
			String v2 = Utils.getAttrAnnotationBloc(o2, "start");
			Double start1 = Double.parseDouble(ht.getTimeValue(v1));
			Double start2 = Double.parseDouble(ht.getTimeValue(v2));
			if(start1>start2){
				return 1;
			}
			else if(start1<start2){
				return -1;
			}
			else{
				return 0;
			}
		}
		catch(Exception e){
			return 0;
		}
	}


}
