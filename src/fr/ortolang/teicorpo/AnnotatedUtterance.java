/**
 * @author Myriam Majdoub
 * Représentations des informations d'un énoncé en format texte à partir d'un format TEI.
 */

package fr.ortolang.teicorpo;

import java.util.ArrayList;
import java.util.HashSet;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Représentation des objets Utterance
 */
public class AnnotatedUtterance {
	// Element annotatedU
	public Element annotU;
	// Identifiant
	public String id;
	// Temps de début(chaîne de caractère, unité = secondes)
	public String start;
	//// Temps de fin(chaîne de caractère, unité = secondes)
	public String end;
	// Code utilisé pour le locuteur
	public String speakerCode;
	// Nom du locuteur
	public String speakerName;
	// énoncé (avec bruits, pauses...)
	public String speech;
	// Enoncé pur
	public String cleanedSpeech;
	// Marque de div: si l'utterance marque le début d'un div, le type de
	// div est spécifié dans ce champ
	// Servira à repérer les divisions dans la transcription
	public String type;
	// Commentaires additionnels hors tiers
	public ArrayList<String> coms = new ArrayList<String>();
	// Liste d'énoncés
	public ArrayList<String> speeches = new ArrayList<String>();
	// Liste de tiers
	public ArrayList<Annot> tiers = new ArrayList<Annot>();
	// Liste des types de tiers
	public HashSet<String> tierTypes = new HashSet<String>();

	public String shortPause;
	public String longPause;
	public String veryLongPause;
	
	public TeiTimeline teiTimeline;

	/**
	 * Impression des utterances : liste des énoncés
	 */
	public void print() {
		System.out.println(
				id + "-" + type + "-" + start + "-" + end + " :-: " + this.speakerName + "   ->   " + speech);
		for (Annot tier : tiers) {
			System.out.println(" :: " + tier.tierToString());
		}
	}

	public AnnotatedUtterance(Element annotatedU, TeiTimeline teiTimeline, TransInfo transInfo, TierParams options) {
		this.teiTimeline = teiTimeline;
		// Initialisation des variables d'instances
		shortPause = Utils.shortPause;
		longPause = Utils.longPause;
		veryLongPause = Utils.veryLongPause;
		annotU = annotatedU;
		id = Utils.getAttrAnnotationBloc(annotatedU, "xml:id");
		start = teiTimeline.getTimeValue(Utils.getAttrAnnotationBloc(annotatedU, "start"));
		end = teiTimeline.getTimeValue(Utils.getAttrAnnotationBloc(annotatedU, "end"));
		speakerCode = Utils.getAttrAnnotationBloc(annotatedU, "who");
		if (options != null) {
			if (options.isDontDisplay(speakerCode))
				return;
			if (!options.isDoDisplay(speakerCode))
				return;
		}
		if (transInfo != null)
			speakerName = transInfo.getParticipantName(speakerCode);
		else
			speakerName = "";
		NodeList annotUElements = annotatedU.getChildNodes();
		cleanedSpeech = "";
		speech = "";
		// Parcours des éléments contenus dans u et construction des
		// variables speech, cleanedSpeech et speeches en fonction.
		for (int i = 0; i < annotUElements.getLength(); i++) {
			if (Utils.isElement(annotUElements.item(i))) {
				Element annotUEl = (Element) annotUElements.item(i);
				String nodeName = annotUEl.getNodeName();
				/*
				 * Cas élément "u": dans ce cas, on concatène tous les u
				 * pour former cleanedSpeech, de même pour speech mais en
				 * incluant les éventuels bruits/incidents qui ont lieu lors
				 * de l'énoncé (sous la forme d'une chaîne de caractère
				 * prédéfinie). On ajoute tous les énoncés dans speeches.
				 */
				if (nodeName.equals("u")) {
					NodeList us = annotUEl.getChildNodes();
					processSeg(us);
				}
				// Ajout des tiers
				else if (nodeName.equals("spanGrp")) {
					String type = annotUEl.getAttribute("type");
					if (options != null) {
						if (options.level == 1)
							return;
						if (options.isDontDisplay(type))
							return;
						if (!options.isDoDisplay(type))
							return;
					}
					NodeList spans = annotUEl.getElementsByTagName("span");
					for (int y = 0; y < spans.getLength(); y++) {
						Element span = (Element) spans.item(y);

						tiers.add(new Annot(type, span.getTextContent()));

						tierTypes.add(type);//
						if (!type.equals("pho") && !type.equals("act") && !type.equals("sit") && !type.equals("com")
								&& !type.equals("morpho")) {
							tierTypes.add("other");
						}
					}
				}
			}
		}
		speech = Utils.cleanString(speech);
		cleanedSpeech = Utils.cleanString(cleanedSpeech);
	}

	public void processSeg(NodeList us) {
		String utt = "";
		for (int z = 0; z < us.getLength(); z++) {
			Node segChild = us.item(z);
			String segChildName = segChild.getNodeName();
			// Ajout des pauses: syntaxe = # pour les pauses courtes, sinon
			// ### pour les pauses longues
			if (Utils.isElement(segChild)) {
				Element segChildEl = (Element) segChild;
				if (segChildName.equals("pause")) {
					if (segChildEl.getAttribute("type").equals("short")) {
						cleanedSpeech += shortPause;
						speech += shortPause;
						utt += shortPause;
					} else if (segChildEl.getAttribute("type").equals("long")) {
						cleanedSpeech += longPause;
						speech += longPause;
						utt += longPause;
					} else if (segChildEl.getAttribute("type").equals("verylong")) {
						cleanedSpeech += veryLongPause;
						speech += veryLongPause;
						utt += veryLongPause;
					} else if (segChildEl.getAttribute("type").equals("chrono")) {
						String chronoPause = " " + Utils.specificPause + segChildEl.getAttribute("dur") + " ";
						cleanedSpeech += chronoPause;
						speech += chronoPause;
						utt += chronoPause;
					}
				}

				// Ajout des évènement (éléments incident & vocal):
				// syntaxe = * type|subtype|desc1 desc2 ... descN /*
				else if (segChildName.equals("incident")) {
					String st = segChildEl.getAttribute("subtype");
					String val = getIncidentDesc(segChildEl);
					if (segChildEl.getAttribute("type").equals("pronounce")) {
						/*
						String[] speechSplit = speech.split("\\s");
						String lastW = "";
						try {
							lastW = speechSplit[speechSplit.length - 1];
						} catch (Exception e) {
						}
						if (lastW.contains("]")) {
							lastW = "";
						}
						String v = joinString(speechSplit, 0, speechSplit.length);
						speech += v;
						utt += v;
						String pron = "[" + lastW + ", " + getIncidentDesc(segChildEl, "desc") + "] ";
						if (!Utils.isNotEmptyOrNull(speech.trim())) {
							speech += pron;
							utt += pron;
						}
						*/
						String ann = Utils.leftEvent + val;
						if (Utils.isNotEmptyOrNull(st)) 
							ann += " /" + st + "/PHO" + Utils.rightEvent + " ";
						else
							ann += Utils.rightEvent + " ";
						speech += ann;
						utt += ann;
					} else if (segChildEl.getAttribute("type").equals("language")) {
						/*
						String[] speechSplit = speech.split("\\s");
						// String lastW = speechSplit[speechSplit.length-1];
						String v = joinString(speechSplit, 0, speechSplit.length) + " $ lang="
								+ getIncidentDesc(segChildEl, "desc") + " X/$ ";
						speech += v;
						utt += v;
						*/
						String ann = Utils.leftCode;
						if (Utils.isNotEmptyOrNull(st)) ann += "/" + st;
						if (Utils.isNotEmptyOrNull(val)) 
							ann += "/LG:" + val + Utils.rightCode + " ";
						else
							ann += "/LG" + Utils.rightCode + " ";
						speech += ann;
						utt += ann;
					} else if (segChildEl.getAttribute("type").equals("noise")) {
						String ann = Utils.leftEvent + val;
						ann += " /";
						if (Utils.isNotEmptyOrNull(st)) ann += st + "/";
						ann += "N" + Utils.rightEvent + " ";
						speech += ann;
						utt += ann;
					} else if (segChildEl.getAttribute("type").equals("comment")) {
						String ann = Utils.leftEvent + val;
						ann += " /";
						if (Utils.isNotEmptyOrNull(st)) ann += st + "/";
						ann += "COM" + Utils.rightEvent + " ";
						speech += ann;
						utt += ann;
					} else if (segChildEl.getAttribute("type").equals("background")) {
						String ann = Utils.leftEvent + val;
						String tm = getIncidentDescAttr(segChildEl, "time");
						if (tm != null) tm = teiTimeline.getTimeValue(tm);
						String lv = getIncidentDescAttr(segChildEl, "level");
						ann += " /";
						if (Utils.isNotEmptyOrNull(st)) ann += st;
						ann += "/"; 
						if (Utils.isNotEmptyOrNull(tm)) ann += tm;
						ann += "/"; 
						if (Utils.isNotEmptyOrNull(lv)) ann += lv;
						ann += "/B" + Utils.rightEvent + " ";
						speech += ann;
						utt += ann;
					} else {
						String ann = Utils.leftCode + val;
						if (Utils.isNotEmptyOrNull(st)) 
							ann += " /" + st;
						ann += "/";
						switch(segChildEl.getAttribute("type")) {
						case "lexical":
							ann += "LX";
							break;
						case "entities":
							ann += "NE";
							break;
						}
						ann += Utils.rightCode + " ";
						speech += ann;
						utt += ann;
					}
				} else if (segChildName.equals("vocal")) {
					String vocal = Utils.leftCode;
					try {
						vocal += segChildEl.getElementsByTagName("desc").item(0).getTextContent();
					} finally {
						speech += vocal + "/VOC " + Utils.rightCode;
						utt += vocal + "/VOC " + Utils.rightCode;
					}
				} else if (segChildName.equals("seg")) {
					processSeg(segChildEl.getChildNodes());
				} else if (segChildName.equals("anchor") && !segChildEl.getAttribute("synch").startsWith("#au")) {
					String sync = teiTimeline.getTimeValue(segChildEl.getAttribute("synch"));
					// creer une ligne avec speech, cleanedSpeech, addspeech
					speeches.add(start + ";" + sync + "__" + utt);
					start = sync;
					// System.out.printf("anchor: %s %s %s%n", start, sync,
					// utt);
					utt = "";
				}
				// Tiers de type "morpho"
				else if (segChildName.equals("morpho")) {
					String tierName = "morpho";
					String tierMorpho = "";
					NodeList cN = ((Element) segChild).getElementsByTagName("w");
					for (int j = 0; j < cN.getLength(); j++) {
						Node w = cN.item(j);
						tierMorpho += w.getTextContent();
						NamedNodeMap attrs = w.getAttributes();
						for (int jz = 0; jz < attrs.getLength(); jz++) {
							Node att = attrs.item(jz);
							tierMorpho += "|" + att.getNodeName() + ":" + att.getNodeValue();
						}
						tierMorpho = " ";
					}
					tiers.add(new Annot(tierName, tierMorpho));
					tierTypes.add("morpho");
				}
			} else if (segChild.getNodeName().equals("#text")) {
				String content = segChild.getTextContent() + " ";
				if (Utils.isNotEmptyOrNull(content.trim())) {
					speech += content;
					cleanedSpeech += content;
					// Dans speeches, pour chaque énoncé (seg), il peut y
					// avoir des marques temporelles
					// On ajoute donc chaque énoncé sous cette forme
					// start;end__speech
					utt += content;
				}
			}
		}
		speeches.add(start + ";" + end + "__" + utt);
	}

	// Récupération d'un type de tier donné (passé en paramètre)

	public ArrayList<Annot> getTier(String typeTier) {
		ArrayList<Annot> t = new ArrayList<Annot>();
		if (typeTier.equals("pho") || typeTier.equals("act") || typeTier.equals("sit") || typeTier.equals("com")
				|| typeTier.equals("morpho")) {
			for (Annot tier : this.tiers) {
				if (typeTier.equals(tier.name)) {
					t.add(tier);
				}
			}
		} else {
			for (Annot tier : this.tiers) {
				String tpT = tier.name;
				if (!tpT.equals("pho") && !tpT.equals("act") && !tpT.equals("sit") && !tpT.equals("com")
						&& !tpT.equals("morpho")) {
					t.add(tier);
				}
			}
		}
		return t;
	}

	public String joinString(String[] stringSplit, int begin, int end) {
		String sentence = "";
		for (int i = begin; i < end; i++) {
			sentence += stringSplit[i] + " ";
		}
		return sentence;
	}

	public String getIncidentDescAttr(Element incident, String attName) {
		NodeList descs = incident.getElementsByTagName("desc");
		if (attName.isEmpty()) {
			for (int i = 0; i < descs.getLength(); i++) {
				Element desc = (Element) descs.item(i);
				if (desc.getAttribute("type") == null) {
					return desc.getTextContent();
				}
			}
			return "";
		} else {
			for (int i = 0; i < descs.getLength(); i++) {
				Element desc = (Element) descs.item(i);
				if (desc.getAttribute("type").equals(attName)) {
					return desc.getTextContent();
				}
			}
			return "";
		}
	}

	public String getIncidentDesc(Element incident) {
		NodeList descs = incident.getElementsByTagName("desc");
		String v = "";
		for (int i = 0; i < descs.getLength(); i++) {
			Element desc = (Element) descs.item(i);
			v += desc.getTextContent();
		}
		return v;
	}
}
