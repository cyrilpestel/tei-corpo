package fr.ortolang.teicorpo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class TeiToPartition {

	// Liste des informations sur les tiers
	ArrayList<TierInfo> tierInfos;
	// Liste des types linguistiques
	ArrayList<Element> lgqTypes;
	// Liste des tiers
	HashMap<String, ArrayList<Annot>> tiers;
	HashMap<String, String> newTiers;
	// index for creating new ids
	int idIncr = 1;

	Document teiDoc;
	XPath teiXPath;
	
	TeiToPartition(XPath xpath, Document tei) {
		teiDoc = tei;
		teiXPath = xpath;
		getTierInfo();
		getTiers();
	}

	public static String getLgqConstraint(ArrayList<TierInfo> ti, String type) {
		for (TierInfo tie: ti) {
			if (tie.tier_id.equals(type))
				return tie.type.constraint;
		}
		return "";
	}
	
	//Construction d'une structure intermédiaire contenant les annotations de la transcription
	//Structure = map
	//Clé = Nom du tier
	//Valeur = liste des annotations de ce type
	public void getTiers() {
		tiers = new HashMap<String, ArrayList<Annot>>();
		newTiers = new HashMap<String, String>();
		NodeList annotationGrps = null;
		try {
			annotationGrps = Utils.getAllAnnotationBloc(this.teiXPath, this.teiDoc);
		} catch (XPathExpressionException e) {
			System.out.println("Erreur de traitement de xpath annotationBloc");
			e.printStackTrace();
			System.exit(1);
		}
		for(int i = 0; i<annotationGrps.getLength(); i++) {
			Element annotGrp = (Element) annotationGrps.item(i);
			NodeList annotGrpElmts = annotGrp.getChildNodes();
			String name = annotGrp.getAttribute("who");
			String start = annotGrp.getAttribute("start").substring(1);
			String end = annotGrp.getAttribute("end").substring(1);
			String id = annotGrp.getAttribute("xml:id");
			String timeSG = (start.isEmpty() || end.isEmpty()) ? "ref" : "time";
			for(int j = 0; j<annotGrpElmts.getLength(); j++){
				if(Utils.isElement(annotGrpElmts.item(j))){
					Element annotElmt = (Element)annotGrpElmts.item(j);
					if(annotElmt.getNodeName().equals("u")){
						Annot annot = new Annot();
						annot.content = annotElmt.getTextContent().trim();
						annot.start = start;
						annot.end = end;
						annot.id = id;
						annot.name = name;
						annot.type = timeSG;
						addElementToMap(tiers, name, annot, "");
					}
					else if(annotElmt.getNodeName().equals("spanGrp")){
						spanGrpCase(tiers, annotElmt, id, name, timeSG, start, end);
					}
				}
			}
		}
	}

	String getText(Element e) {
		String s = "";
		NodeList nle = e.getChildNodes();
		for (int i=0; i<nle.getLength(); i++) {
			// System.out.printf("-- %d %s %n", i, nle.item(i));
			Node ei = nle.item(i);
			if (Utils.isText(ei)) {
				s += ei.getTextContent();
			}
		}
		return s;
	}
	
	//Traitement des spanGrp pour ajout dans la structure Map
	public void spanGrpCase(HashMap<String, ArrayList<Annot>> tiers, Element spanGrp, String id, String name, String timeref, String start, String end) {
		String typeSG = spanGrp.getAttribute("type");
		NodeList spans = spanGrp.getChildNodes();
		String previousId = "";
		if (spans == null) return;
		for(int z=0; z<spans.getLength(); z++) {
			Node nodespan = spans.item(z);
			// System.out.printf("%d %s %d %n", z, nodespan.getNodeName(), nodespan.getNodeType());
			if (!nodespan.getNodeName().equals("span")) continue;
			Element span = (Element)nodespan;
			Annot annot = new Annot();
			annot.content = getText(span).trim();
			String spid = span.getAttribute("xml:id");
			if (!spid.isEmpty())
				annot.id = spid;
			else
				annot.id = "x" + idIncr++;
			// System.out.printf("%d %d %s %s %s %s {%s} %s ", z, span.getNodeType(), typeSG, id, name, span.getTagName(), annot.content, annot.id);
			// if (span.hasAttribute("target")){
			if (!LgqType.isTimeType(getLgqConstraint(tierInfos, typeSG))){
				annot.type = "ref";
				String tg = span.getAttribute("target");
				if (!tg.isEmpty())
					annot.link = tg.substring(1);
				else
					annot.link = id;
				if (!previousId.isEmpty())
					annot.previous = previousId;
				previousId = annot.id;
				// System.out.printf("ref %s (%s) %n", annot.link, annot.previous);
			} else {
				annot.type = "time";
				annot.start = span.getAttribute("from");
				if(Utils.isNotEmptyOrNull(annot.start)){
					annot.start = annot.start.substring(1);
				}
				annot.end = span.getAttribute("to");
				if(Utils.isNotEmptyOrNull(annot.end)){
					annot.end = annot.end.substring(1);
				}
				// System.out.printf("time %s %s %n", annot.start, annot.end);
			}
			addElementToMap(tiers, /* name + "-" + */ typeSG, annot, typeSG);
			NodeList spanGrps = span.getChildNodes();
			for(int l = 0; l<spanGrps.getLength(); l++){
				Node nodeSpanGrp = spanGrps.item(l);
				if (!nodeSpanGrp.getNodeName().equals("spanGrp")) continue;
				Element subSpanGrp = (Element)nodeSpanGrp;
				if (annot.type == "ref")
					spanGrpCase(tiers, subSpanGrp, annot.id, name, annot.type, start, end);
				else
					spanGrpCase(tiers, subSpanGrp, annot.id, name, annot.type, annot.start, annot.end);
			}
		}
	}

	void addElementToMap(HashMap<String, ArrayList<Annot>> map, String type, Annot annot, String lingType){
		if(map.containsKey(type)){
			map.get(type).add(annot);
		}
		else{
			ArrayList<Annot> newAnnotList = new ArrayList<Annot>();
			newAnnotList.add(annot);
			map.put(type, newAnnotList);
			if (!lingType.isEmpty()) newTiers.put(type, lingType);
		}
	}

	//Elements linguistic_type
	void getTierInfo(){
		tierInfos= new ArrayList<TierInfo>();
		Element teiHeader = (Element) this.teiDoc.getElementsByTagName("teiHeader").item(0);
		NodeList notes = teiHeader.getElementsByTagName("note");
		for(int j = 0; j<notes.getLength(); j++){
			Element note = (Element) notes.item(j);
			if(note.getAttribute("type").equals("TEMPLATE_DESC")){
				//System.out.println("trouvé les notes TEMPLATE");
				NodeList templateChildren = note.getChildNodes();
				for(int y = 0; y<templateChildren.getLength(); y++){
					TierInfo ti = new TierInfo();
					Node nd = templateChildren.item(y);
					if (!nd.getNodeName().equals("note")) continue;
					NodeList templateNote  = nd.getChildNodes();
					for(int z = 0; z < templateNote.getLength(); z++){
						Node nd2  = templateNote.item(z);
						if (!nd2.getNodeName().equals("note")) continue;
						Element elt = (Element)nd2;
						if(elt.getAttribute("type").equals("code")){
							ti.tier_id = elt.getTextContent();
						}
						else if(elt.getAttribute("type").equals("graphicref")){
							ti.type.graphic_ref = elt.getTextContent();
						}
						else if(elt.getAttribute("type").equals("parent")){
							ti.parent = elt.getTextContent();
						}
						else if(elt.getAttribute("type").equals("type")){
							ti.type.constraint = elt.getTextContent();
						}
						else if(elt.getAttribute("type").equals("subtype")){
							ti.type.lgq_type_id = elt.getTextContent();
						}
						else if(elt.getAttribute("type").equals("scribe")){
							ti.annotator = elt.getTextContent();
						}
						else if(elt.getAttribute("type").equals("lang")){
							ti.lang = elt.getTextContent();
						}
						else if(elt.getAttribute("type").equals("langref")){
							ti.lang_ref = elt.getTextContent();
						}
						else if(elt.getAttribute("type").equals("cv")){
							ti.type.cv_ref =  elt.getTextContent();
						}
					}
//					System.out.println(ti.toString());
//					System.out.println(lgqType.getAttribute("LINGUISTIC_TYPE_REF"));
					tierInfos.add(ti);
					//annot_doc.appendChild(lgqType);
				}
			}
		}
		getParticipantNames();
	}

	//Récupération des noms des participants
	void getParticipantNames (){
		NodeList participantsInfo = this.teiDoc.getElementsByTagName("person");
		for(int i = 0; i<participantsInfo.getLength(); i++){
			Element person = (Element)participantsInfo.item(i);
			NodeList cn = person.getChildNodes();
			for(int j = 0; j<cn.getLength(); j++){
				if(Utils.isElement(cn.item(j))){
					Element child = (Element)cn.item(j);
					if(child.getNodeName().equals("altGrp")){
						for(int z = 0; z<child.getElementsByTagName("alt").getLength(); z++){
							Element alt = (Element) child.getElementsByTagName("alt").item(z);
							if(alt.hasAttribute("type")){
								for(TierInfo ti : tierInfos){
									NodeList pn = person.getElementsByTagName("persName");
									if(ti.tier_id.equals(alt.getAttribute("type"))){
										if(person.getElementsByTagName("persName").getLength()>0){
											ti.participant = person.getElementsByTagName("persName").item(0).getTextContent();
										}
										else{
											ti.participant = "";
										}
									}
								}
							}
						}
					}
				}
			}
		}

	}

}
