package fr.ortolang.teicorpo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
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
	TreeMap<String, ArrayList<Annot>> tiers;
	TreeMap<String, NewTier> newTiers;
	// index for creating new ids
	int idIncr = 1;

	// timeline information
	TeiTimeline timeline;

	Document teiDoc;
	XPath teiXPath;
	public TierParams optionsOutput;

	TeiToPartition(XPath xpath, Document tei, TierParams optionsTei) {
		optionsOutput = optionsTei;
		teiDoc = tei;
		teiXPath = xpath;
		timeline = new TeiTimeline();
		// récupérer la timeline
		timeline.buildTimeline(teiDoc);
		// info tiers et types
		getTierInfo();
		// contenu des tiers
		getTiers();
	}

	public static String getLgqConstraint(ArrayList<TierInfo> ti, String type) {
		for (TierInfo tie : ti) {
			if (tie.tier_id.equals(type))
				return tie.type.constraint;
		}
		return "";
	}

	public static TierInfo getTierInfoElement(ArrayList<TierInfo> ti, String type) {
		for (TierInfo tie : ti) {
			if (tie.tier_id.equals(type))
				return tie;
		}
		return null;
	}

	// Construction d'une structure intermédiaire contenant les annotations de
	// la transcription
	// Structure = map
	// Clé = Nom du tier
	// Valeur = liste des annotations de ce type
	public void getTiers() {
		tiers = new TreeMap<String, ArrayList<Annot>>();
		newTiers = new TreeMap<String, NewTier>();
		NodeList annotationGrps = null;
		try {
			annotationGrps = Utils.getAllAnnotationBloc(this.teiXPath, this.teiDoc);
		} catch (XPathExpressionException e) {
			System.out.println("Erreur de traitement de xpath annotationBlock");
			e.printStackTrace();
			System.exit(1);
		}
		for (int i = 0; i < annotationGrps.getLength(); i++) {
			Element annotGrp = (Element) annotationGrps.item(i);
			AnnotatedUtterance au = new AnnotatedUtterance();
			au.process(annotGrp, this.timeline, null, optionsOutput, false);
			NodeList annotGrpElmts = annotGrp.getChildNodes();
			String name = au.speakerCode;
			if (!Utils.isNotEmptyOrNull(name))
				continue; // this is a note and not an utterance
			if (optionsOutput != null) {
				if (optionsOutput.isDontDisplay(name))
					continue;
				if (!optionsOutput.isDoDisplay(name))
					continue;
			}
//			String start = Utils.refID(annotGrp.getAttribute("start"));
//			System.err.println("Start1: " + start);
//			start = timeline.getTimeValue(start);
//			System.err.println("Start2: " + start);
//			String end = Utils.refID(annotGrp.getAttribute("end"));
//			end = timeline.getTimeValue(end);
			String start = au.start;
			String end = au.end;
			String id = au.id;
			// String timeSG = (start.isEmpty() || end.isEmpty()) ? "ref" : "time"; // ref is impossible on u tags in ELAN
			// si le nombre d'annotation de au.speeches > 1 il faudrait changer le statut des spans qui en dépendent
			for (Annot a: au.speeches) {
				/*
				Annot annot = new Annot();
				annot.content = annotElmt.getTextContent().trim();
				annot.start = start;
				annot.end = end;
				annot.id = id;
				annot.name = name;
				*/
				a.timereftype = "time";
				String lgqt = "";
				for (TierInfo ti : tierInfos) {
					if (ti.tier_id.equals(name))
						lgqt = ti.type.lgq_type_id == null ? "-" : ti.type.lgq_type_id;
				}
				addElementToMap(tiers, name, a, lgqt, "-");
			}
			
			for (int j = 0; j < annotGrpElmts.getLength(); j++) {
				if (Utils.isElement(annotGrpElmts.item(j))) {
					Element annotElmt = (Element) annotGrpElmts.item(j);
					if (annotElmt.getNodeName().equals("spanGrp")) {
						String at = annotElmt.getAttribute("type");
						if (at != null && at.equals("TurnInformation"))
							continue;
						spanGrpCase(tiers, annotElmt, id, name, "time", start, end);
					}
				}
			}
		}
	}

	String getText(Element e) {
		String s = "";
		NodeList nle = e.getChildNodes();
		for (int i = 0; i < nle.getLength(); i++) {
			// System.out.printf("-- %d %s %n", i, nle.item(i));
			Node ei = nle.item(i);
			if (Utils.isText(ei)) {
				s += ei.getTextContent();
			}
		}
		return s;
	}

	// Traitement des spanGrp pour ajout dans la structure Map
	public void spanGrpCase(TreeMap<String, ArrayList<Annot>> tiers, Element spanGrp, String id, String name,
			String timeref, String start, String end) {
		String typeSG = spanGrp.getAttribute("type");
		if (optionsOutput != null) {
			if (optionsOutput.level == 1)
				return;
			if (optionsOutput.isDontDisplay(typeSG))
				return;
			if (!optionsOutput.isDoDisplay(typeSG))
				return;
		}
		NodeList spans = spanGrp.getChildNodes();
		String previousId = "";
		if (spans == null)
			return;
		Double timelength = -1.0;
		try {
			timelength = (Double.parseDouble(end) - Double.parseDouble(start)) / spans.getLength();
		} catch (Exception e) {
			timelength = -1.0;
		}
		// System.out.printf("XX: %s %s %f %n", start, end, timelength);
		for (int z = 0; z < spans.getLength(); z++) {
			Node nodespan = spans.item(z);
			// System.out.printf("%d %s %d %n", z, nodespan.getNodeName(),
			// nodespan.getNodeType());
			if (!nodespan.getNodeName().equals("span"))
				continue;
			Element span = (Element) nodespan;
			Annot annot = new Annot();
			annot.content = getText(span).trim();
			String spid = span.getAttribute("xml:id");
			if (!spid.isEmpty())
				annot.id = spid;
			else
				annot.id = "x" + idIncr++;
			// System.out.printf("%d %d %s %s %s %s {%s} %s ", z,
			// span.getNodeType(), typeSG, id, name, span.getTagName(),
			// annot.content, annot.id);
			// if (span.hasAttribute("target")){
			if (!LgqType.isTimeType(getLgqConstraint(tierInfos, typeSG))) {
				annot.timereftype = "ref";
				String tg = span.getAttribute("target");
				if (!tg.isEmpty())
					annot.link = tg.substring(1);
				else
					annot.link = id;
				if (!previousId.isEmpty())
					annot.previous = previousId;
				previousId = annot.id;
				/*
				 * add equivalence in time in case it is necessary
				 */
				// System.out.printf("++ %d %s %n", z, annot);
				if (timelength > 0.0) {
					Double refstart = z * timelength + Double.parseDouble(start);
					Double refend = (z + 1) * timelength + Double.parseDouble(start);
					// System.out.printf("-- %d %f %f %n", z, refstart, refend);
					annot.start = Double.toString(refstart);
					annot.end = Double.toString(refend);
				}
				// System.out.printf("ref %s (%s) %n", annot.link,
				// annot.previous);
			} else {
				annot.timereftype = "time";
				String tstart = span.getAttribute("from");
				String tend = span.getAttribute("to");
				// System.out.printf("YY: %s %s %n", tstart, tend);
				if (Utils.isNotEmptyOrNull(tstart)) {
					annot.start = timeline.getTimeValue(Utils.refID(tstart));
				}
				if (Utils.isNotEmptyOrNull(tend)) {
					annot.end = timeline.getTimeValue(Utils.refID(tend));
				}
				// System.out.printf("time %s %s %n", annot.start, annot.end);
			}
			String lgqt = "";
			for (TierInfo ti : tierInfos) {
				if (ti.tier_id.equals(typeSG))
					lgqt = ti.type.lgq_type_id == null ? "-" : ti.type.lgq_type_id;
			}
			addElementToMap(tiers, typeSG, annot, lgqt, name);
			NodeList spanGrps = span.getChildNodes();
			for (int l = 0; l < spanGrps.getLength(); l++) {
				Node nodeSpanGrp = spanGrps.item(l);
				if (!nodeSpanGrp.getNodeName().equals("spanGrp"))
					continue;
				Element subSpanGrp = (Element) nodeSpanGrp;
				if (annot.timereftype == "ref")
					spanGrpCase(tiers, subSpanGrp, annot.id, name, annot.timereftype, start, end);
				else
					spanGrpCase(tiers, subSpanGrp, annot.id, name, annot.timereftype, annot.start, annot.end);
			}
		}
	}

	void addElementToMap(TreeMap<String, ArrayList<Annot>> map, String type, Annot annot, String lingType,
			String topparent) {
		// Créer le nouveau nom du tier
		// stocker la référence du type ling de ce nom.
		String truename;
		if (!topparent.isEmpty() && !topparent.equals("-")) {
			truename = topparent + "-" + type;
			NewTier nt = new NewTier(truename, type, lingType, topparent);
			// System.out.printf("-: %s %s %s %s %n", truename, type, lingType,
			// topparent);
			newTiers.put(truename, nt);
		} else
			truename = type;
		if (map.containsKey(truename)) {
			map.get(truename).add(annot);
		} else {
			ArrayList<Annot> newAnnotList = new ArrayList<Annot>();
			newAnnotList.add(annot);
			map.put(truename, newAnnotList);
		}
	}

	// Elements linguistic_type
	void getTierInfo() {
		tierInfos = new ArrayList<TierInfo>();
		Element teiHeader = (Element) this.teiDoc.getElementsByTagName("teiHeader").item(0);
		NodeList notes = teiHeader.getElementsByTagName("note");
		for (int j = 0; j < notes.getLength(); j++) {
			Element note = (Element) notes.item(j);
			if (note.getAttribute("type").equals("TEMPLATE_DESC")) {
				// System.out.println("trouvé les notes TEMPLATE");
				NodeList templateChildren = note.getChildNodes();
				for (int y = 0; y < templateChildren.getLength(); y++) {
					TierInfo ti = new TierInfo();
					Node nd = templateChildren.item(y);
					if (!nd.getNodeName().equals("note"))
						continue;
					NodeList templateNote = nd.getChildNodes();
					for (int z = 0; z < templateNote.getLength(); z++) {
						Node nd2 = templateNote.item(z);
						if (!nd2.getNodeName().equals("note"))
							continue;
						Element elt = (Element) nd2;
						if (elt.getAttribute("type").equals("code")) {
							ti.tier_id = elt.getTextContent();
						} else if (elt.getAttribute("type").equals("graphicref")) {
							ti.type.graphic_ref = elt.getTextContent();
						} else if (elt.getAttribute("type").equals("parent")) {
							ti.parent = elt.getTextContent();
						} else if (elt.getAttribute("type").equals("type")) {
							ti.type.constraint = elt.getTextContent();
						} else if (elt.getAttribute("type").equals("subtype")) {
							ti.type.lgq_type_id = elt.getTextContent();
						} else if (elt.getAttribute("type").equals("scribe")) {
							ti.annotator = elt.getTextContent();
						} else if (elt.getAttribute("type").equals("lang")) {
							ti.lang = elt.getTextContent();
						} else if (elt.getAttribute("type").equals("langref")) {
							ti.lang_ref = elt.getTextContent();
						} else if (elt.getAttribute("type").equals("cv")) {
							ti.type.cv_ref = elt.getTextContent();
						}
					}
					if (!Utils.isNotEmptyOrNull(ti.type.lgq_type_id))
						ti.type.lgq_type_id = ti.tier_id; // no ling type, the
															// ling are named as
															// the tiers are.
					if (ti.type.constraint == null)
						ti.type.constraint = ""; // no ling type, the ling are
													// named as the tiers are.
					// System.out.println(ti.toString());
					// System.out.println(lgqType.getAttribute("LINGUISTIC_TYPE_REF"));
					tierInfos.add(ti);
					// annot_doc.appendChild(lgqType);
				}
			}
		}
		getParticipantNames();
	}

	// Récupération des noms des participants
	void getParticipantNames() {
		NodeList participantsInfo = this.teiDoc.getElementsByTagName("person");
		for (int i = 0; i < participantsInfo.getLength(); i++) {
			Element person = (Element) participantsInfo.item(i);
			NodeList cn = person.getChildNodes();
			for (int j = 0; j < cn.getLength(); j++) {
				if (Utils.isElement(cn.item(j))) {
					Element child = (Element) cn.item(j);
					if (child.getNodeName().equals("altGrp")) {
						NodeList nnl = child.getElementsByTagName("alt");
						for (int z = 0; z < nnl.getLength(); z++) {
							Element alt = (Element)nnl.item(z);
							if (alt.hasAttribute("type")) {
								for (TierInfo ti : tierInfos) {
									// NodeList pn =
									// person.getElementsByTagName("persName");
									// System.err.println(ti.toString());
									if (ti.tier_id.equals(alt.getAttribute("type"))) {
										if (person.getElementsByTagName("persName").getLength() > 0) {
											ti.participant = person.getElementsByTagName("persName").item(0)
													.getTextContent();
										} else {
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
