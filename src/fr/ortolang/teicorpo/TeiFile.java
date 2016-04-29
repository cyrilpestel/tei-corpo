/**
 * @author Myriam Majdoub
 * Représentations des informations d'un document TEI.
 */

package fr.ortolang.teicorpo;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class TeiFile {

	//Document TEI à lire
	public Document teiDoc;
	// acces Xpath
	public XPathFactory xPathfactory;
	public XPath xpath;
	//Informations sur la transcription
	public TransInfo transInfo;
	//Transcription
	public Trans trans;
	//Langage du discours
	public String language;
	//Map pour représenter la timeline
	public HashMap<String, String> timeline = new HashMap<String, String>();
	//Extension des fichiers TEI

	public TierParams optionsOutput;
	//Lignes principales de la transcriptions (liste d'utterances)
	ArrayList<AnnotatedUtterance> mainLines = new ArrayList<AnnotatedUtterance>();

	//Validation du document Tei par la dtd
	boolean validation = false;

	public TeiFile(File teiFile, TierParams options){
		optionsOutput = options;
		DocumentBuilderFactory factory = null;
		Element root = null;
		try{
			factory = DocumentBuilderFactory.newInstance();
			Utils.setDTDvalidation(factory, validation);
			DocumentBuilder builder = factory.newDocumentBuilder();
			teiDoc = builder.parse(teiFile);
			root = teiDoc.getDocumentElement();
			xPathfactory = XPathFactory.newInstance();
			xpath = xPathfactory.newXPath();
			xpath.setNamespaceContext(new NamespaceContext() {
			    public String getNamespaceURI(String prefix) {
		        	System.out.println("prefix called " + prefix);
			        if (prefix == null) {
			            throw new IllegalArgumentException("No prefix provided!");
			        } else if (prefix.equals(XMLConstants.DEFAULT_NS_PREFIX)) {
			        	System.out.println("default prefix called");
			            return "http://www.tei-c.org/ns/1.0";
			        } else if (prefix.equals("tei")) {
			        	System.out.println("tei prefix called");
			            return "http://www.tei-c.org/ns/1.0";
			        } else if (prefix.equals("xsi")) {
			            return "http://www.w3.org/2001/XMLSchema-instance";
			        } else {
			            return XMLConstants.NULL_NS_URI;
			        }
			    }

			    public Iterator<?> getPrefixes(String val) {
			        return null;
			    }

			    public String getPrefix(String uri) {
			        return null;
			    }
			});
		}catch(Exception e){
			e.printStackTrace();
		}
		buildTimeline();
		transInfo = new TransInfo((Element) root.getElementsByTagName("teiHeader").item(0));
		trans = new Trans((Element) root.getElementsByTagName("text").item(0), this);
		transInfo.fileLocation = teiFile.getAbsolutePath();
		language = root.getAttribute("xml:lang");
	}

	public void buildTimeline(){
		timeline.put("T0", "0");
		Element tl = (Element)this.teiDoc.getElementsByTagName("timeline").item(0);
		String unit = tl.getAttribute("unit");
		double ratio = 1.0;
		if (unit.equals("ms"))
			ratio = 1.0/1000.0;
		else if (unit.equals("s"))
			ratio = 1.0;
		else {
			System.out.println("Unité inconnue pour timeline: " + unit);
			System.out.println("Pas de conversion réalisée.");
			ratio = 1.0;
		}
		NodeList whens = tl.getElementsByTagName("when");
		for (int i = 0; i<whens.getLength(); i++){
			Element when = (Element) whens.item(i);
			if (when.hasAttribute("interval")) {
				String tms = when.getAttribute("interval");
				double vald = Double.parseDouble(tms);
				vald *= ratio;
				tms = Utils.printDouble(vald, 10);
				//System.out.println(tms + " --> " + ntms);
				timeline.put(when.getAttribute("xml:id"), tms);
			} else if (when.hasAttribute("absolute")) {
				String tms = when.getAttribute("absolute");
				double vald = Double.parseDouble(tms);
				vald *= ratio;
				tms = Utils.printDouble(vald, 10);
				timeline.put(when.getAttribute("xml:id"), tms);
			}
		}
	}

	public int mainLinesSize(){
		return mainLines.size();
	}

	public String getTimeValue(String timeId){
		if(Utils.isNotEmptyOrNull(timeId)){
			String[] spl = timeId.split("#");
			if (spl.length>1)
				return timeline.get(spl[1]);
			else
				return "";
		}
		return "";
	}

	/**
	 * Contient les informations sur la transcription: lieu, date, locuteurs, situation, enregistrement...
	 */
	public class TransInfo{

		//Titre du document
		public String title;
		//Format original du document
		public String format;
		//Nom de l'enregistrement
		public String medianame;
		//Adresse du fichier
		public String fileLocation;
		//Nom de l'enregistrement si différent du nom de la transription
		public String recordName;
		//Type de l'enregistrement (video ou audio)
		public String mediatype;
		//Adresse de l'enregistrement
		//public String medialocation;
		//Date de l'enregistement
		public String date;
		//Lieu où a eu lieu l'enregistrement
		public String place;
		//Nom du transcripteur
		public String transcriber;
		//Version de la transcription
		public String version;
		//Durée de la transcription
		public String timeDuration;
		//Heure du début de la transcription
		public String startTime;

		//Liste des situations ayant eu lieu lors de l'enregistrement
		public Map <String, String> situations = new HashMap<String, String>();
		//Liste des locuteurs ayant participé à l'enregistrement
		public ArrayList<Participant> participants = new ArrayList<Participant>();
		//Liste des notes supplémentaires concernant la transcription
		public ArrayList<String> notes = new ArrayList<String>();
		//Liste ordonnée des situations
		public ArrayList<String> situationList = new ArrayList<String>();

		public TransInfo(Element docTeiHeader){
			//Récupération des informations relatives au fichier
			Element fileDesc = (Element) docTeiHeader.getElementsByTagName("fileDesc").item(0);
			getFileDescInfo(fileDesc);
			//Récupération de informations relatives à l'enregistrement
			Element profileDesc = (Element) docTeiHeader.getElementsByTagName("profileDesc").item(0);
			getProfileDescInfo(profileDesc);
			//Récupération des information relatives aux versions de l'enregistrement
			//Element revisionDesc = (Element) docTeiHeader.getElementsByTagName("revisionDesc").item(0);			

			//Récupération des informations relatives au logiciel d'origine de la transcription
			Element application = (Element) docTeiHeader.getElementsByTagName("application").item(0);
			if(application != null){
				format = application.getAttribute("ident");
				version = application.getAttribute("version");
			}
		}

		/**Récupération des informations relatives au fichier
		 * @param fileDesc	Element fileDesc
		 */
		public void getFileDescInfo(Element fileDesc){

			try{
				title = fileDesc.getElementsByTagName("title").item(0).getTextContent();
			}
			catch(Exception e){}

			try{
				Element recording = (Element) fileDesc.getElementsByTagName("recording").item(0);
				Element media = (Element) recording.getElementsByTagName("media").item(0);
				medianame = (new File(media.getAttribute("url"))).getName();
				mediatype = media.getAttribute("mimeType");
				Element date = (Element) recording.getElementsByTagName("date").item(0);
				timeDuration = date.getAttribute("dur");
				// recordName = (new File(media.getAttribute("url"))).getName();
			}
			catch(Exception e){}

			NodeList recordingDate = fileDesc.getElementsByTagName("date");
			if (recordingDate.getLength()!=0){
				date = recordingDate.item(0).getTextContent();
			}

			NodeList recordingStartTime = fileDesc.getElementsByTagName("time");
			if (recordingStartTime.getLength()!=0){
				Element time = (Element)recordingStartTime.item(0);
				startTime = time.getAttribute("when");
			}

			try{
				Element notesStmt = (Element) fileDesc.getElementsByTagName("notesStmt").item(0);
				Element addNotes = (Element) notesStmt.getElementsByTagName("note").item(0);
				NodeList allNotes = addNotes.getElementsByTagName("note");
				for(int i = 0; i<allNotes.getLength(); i++){
					if(Utils.isElement(allNotes.item(i))){
						Element n = (Element)allNotes.item(i);
						if(n.getAttribute("type").equals("scribe")){
							if(n.getTextContent().toLowerCase().contains("coder -")){
								transcriber = n.getTextContent().split("oder -")[1];
							}
							else if(n.getTextContent().toLowerCase().contains("coder-")){
								transcriber = n.getTextContent().split("oder-")[1];
							}
							else{
								transcriber = n.getTextContent();
							}
						}
						else{
							if(Utils.isNotEmptyOrNull(n.getTextContent())){
								//System.out.println("[" + n.getAttribute("type")+ "] " + n.getTextContent());
								notes.add("[" + n.getAttribute("type")+ "] " + n.getTextContent());
							}						
						}
					}
				}
			}
			catch(Exception e){}
		}

		/**Récupération de informations relatives à l'enregistrement
		 * @param profileDesc Element profileDesc
		 */
		public void getProfileDescInfo(Element profileDesc){
			try{
				NodeList recordingPlace = profileDesc.getElementsByTagName("placeName");
				if (recordingPlace.getLength()!=0){
					place = recordingPlace.item(0).getTextContent();
				}
				NodeList participantsStmt = profileDesc.getElementsByTagName("particDesc");
				if (participantsStmt.getLength()!=0) {
					NodeList participantsList = ((Element)participantsStmt.item(0)).getElementsByTagName("person");
					for (int i = 0; i<participantsList.getLength(); i++){
						Node pNode = participantsList.item(i);
						Element p = (Element) pNode;
						participants.add(new Participant (p));
					}
				}
				NodeList settingDesc = profileDesc.getElementsByTagName("settingDesc");
				if (settingDesc.getLength()!=0) {
					NodeList settings = ((Element)settingDesc.item(0)).getElementsByTagName("setting");
					for (int i = 0; i<settings.getLength(); i++){
						Element d = (Element) settings.item(i);
						Element activity = (Element) d.getElementsByTagName("activity").item(0);
						String c = activity.getTextContent().trim();
						// System.out.printf("Theme: %s %s %n", d.getAttribute("xml:id"), c);
						situations.put(d.getAttribute("xml:id"), c);
						situationList.add(c);
					}
				}
			} catch(Exception e){
				e.printStackTrace();
				System.out.println("erreur dans le traitement des settings");
			}
		}

		/**
		 * Impression de TransInfo
		 */
		public void print(){
			System.out.println("medianame\t" + medianame);
			System.out.println("version\t" + version);
			System.out.println("place\t" + place);
			System.out.println("date\t" + date);
			System.out.println("transcriber\t" + transcriber);	

			for(Participant p: participants){
				p.print();
			}

			for (Map.Entry<String , String> entry : situations.entrySet()){
				System.out.println("desc\t" + entry.getKey() + "\t" + entry.getValue());
			}

			for(String note : notes){
				System.out.println("Note: " + note);
			}
		}
	}

	/**
	 * Représentation d'un locuteur.
	 */
	public class Participant{
		//Code donné au locuteur
		public String id;
		//Nom du locuteur
		public String name;
		//Rôle du locuteur
		public String role;
		//Sexe du locuteur
		public String sex;
		//Langue parlée par le locuteur
		public String language;
		//Corpus
		public String corpus;
		//Age du locuteur
		public String age;
		//Informations supplémentaires concernant le locuteur
		public Map<String, String> adds = new HashMap<String, String>();

		public Participant(Element participant){
			NamedNodeMap attrs = participant.getAttributes();
			for (int i = 0; i<attrs.getLength(); i++){
				Node att = attrs.item(i);
				String attName = att.getNodeName();
				String attValue = att.getNodeValue();
				/*if (attName.equals("code")){
					id = attValue;
				}
				else*/ 
				/*if(attName.equals("name")){
					name = attValue;
				}
				else */
				if(attName.equals("role")){
					role = attValue;
				}
				else if(attName.equals("sex")){
					if(attValue.equals("1")){
						sex = "male";
					}
					else if(attValue.equals("2")){
						sex = "female";
					}
					else{
						sex = "Unknown";
					}
				}
				/*else if(attName.equals("xml:lang")){
					language = attValue;
				}*/
				else if(attName.equals("source")){
					corpus = attValue;
				}
				else if(attName.equals("age")){
					age = attValue;
				}
				else{
					adds.put(attName, attValue);
				}
			}
			NodeList partEl = participant.getChildNodes();
			for (int i = 0; i<partEl.getLength(); i++){
				if (Utils.isElement(partEl.item(i))){
					Element el = (Element) partEl.item(i);
					if (el.getNodeName().equals("note")){
						// System.out.printf("adds: %s %s %n", el.getAttribute("type"), el.getTextContent());
						adds.put(el.getAttribute("type"), el.getTextContent());
					}
					else if (el.getNodeName().equals("altGrp")){
						NodeList alts = el.getElementsByTagName("alt");
						String codes = "";
						for(int r = 0; r<alts.getLength(); r++){
							Element alt = (Element)alts.item(r);
							codes += alt.getAttribute("type");
							if(r+1<alts.getLength()){
								codes += ";";
							}
						}
						id = codes;
					}
					else if(el.getNodeName().equals("langKnowledge")){
						language = el.getTextContent();
					}
					else if(el.getNodeName().equals("persName")){
						name = el.getTextContent();
					}
					else{
						adds.put(el.getNodeName(), el.getTextContent());
					}
				}
			}
		}

		/**
		 * Impression de Participant.
		 */
		public void print(){
			System.out.print("Participant");
			System.out.print("\tid " + id + "\tname " + name + "\trole " + role + "\tsex " +  sex + "\tlang " + language + "\t");
			for (Map.Entry<String, String> e : adds.entrySet() ){
				System.out.print(e.getKey() + " " + e.getValue() + "\t");
			}
			System.out.println();
		}
	}

	/**
	 * Représentation de la transcription: situations + énoncés + tiers
	 */
	public class Trans {
		//Liste des div
		public ArrayList<Div> divs = new ArrayList<Div>();
		//Liste des types de tier présents dans la transcription
		public HashSet<String> tierTypes = new HashSet<String>();
		//Situation principale
		public String sit;
		//Autres info
		// public ArrayList<String> infos = new ArrayList<String> ();

		public Trans(Element text, TeiFile tf) {
			//Liste d'éléments contenus dans la transcription (élément text)
			Element body = (Element)text.getElementsByTagName("body").item(0);
			NodeList divList = body.getChildNodes();

			//Div principal
			try {
				for(int i = 0; i<divList.getLength(); i++) {
					if(Utils.isElement(divList.item(i))) {
						Element elmt = (Element)divList.item(i);
						if(elmt.getTagName().equals("div")) {
							String attr = Utils.getDivHeadAttr(elmt, "subtype");
							sit = tf.transInfo.situations.get(attr);
							break;
						}
					}
				}
			} catch(Exception e) {
				sit = "";
			}
/*
			//Thème principal
			if(getDivOcc(divList) == 1) {
				NodeList subList = divList.item(0).getChildNodes();
				if (getDivOcc(subList) != 0)
					divList = subList;
			}
*/
			for (int i = 0; i<divList.getLength(); i++) {
				if(Utils.isElement(divList.item(i))){
					Element elmt = (Element)divList.item(i);
					if(elmt.getTagName().equals("div")){
						String id = Utils.getDivHeadAttr(elmt, "subtype");
						String theme = tf.transInfo.situations.get(id);
						// System.out.printf("BeforeDiv: %s %s %n", id, theme);
						Div d = new Div(tf, elmt, id, theme);
						divs.add(d);
						for(AnnotatedUtterance u : d.utterances){
							tierTypes.addAll(u.tierTypes);
						}
					}
				}
			}
/*
			NodeList coms = text.getElementsByTagName("note");
			for(int i = 0; i<coms.getLength(); i++){
				Element info = (Element) coms.item(i);
				infos.add(info.getAttribute("type")+ "\t" + info.getTextContent());
			}
*/
		}
	}

	/**
	 * Représentation des objets Utterance
	 */
	public class AnnotatedUtterance{
		//Element annotatedU
		public Element annotU;
		//Identifiant
		public String id;
		//Temps de début(chaîne de caractère, unité = secondes) 
		public String start;
		////Temps de fin(chaîne de caractère, unité = secondes) 
		public String end;
		//Code utilisé pour le locuteur
		public String speakerCode;
		//Nom du locuteur
		public String speakerName;
		//énoncé (avec bruits, pauses...)
		public String speech;
		//Enoncé pur
		public String cleanedSpeech;
		//Marque de div: si l'utterance marque le début d'un div, le type de div est spécifié dans ce champ
		//Servira à repérer les divisions dans la transcription
		public String type;
		//Commentaires additionnels hors tiers
		public ArrayList<String> coms = new ArrayList<String>();
		//Liste d'énoncés
		public ArrayList<String> speeches = new ArrayList<String>();
		//Liste de tiers
		public ArrayList<Annot> tiers = new ArrayList<Annot>();
		//Liste des types de tiers
		public HashSet<String> tierTypes = new HashSet<String>();

		public String shortPause = " # ";
		public String longPause = " ## ";
		public String veryLongPause = " ### ";

		/**
		 * Impression des utterances : liste des énoncés
		 */
		public void print(){
			System.out.println(id + "-" + type + "-" + start + "-" + end + " :-: " + this.speakerName + "   ->   " + speech);
			for (Annot tier : tiers){
				System.out.println(" :: " + tier.tierToString());
			}
		}

		public AnnotatedUtterance(Element annotatedU){
			//Initialisation des variables d'instances
			shortPause = " # ";
			longPause = " ## ";
			veryLongPause = " ### ";
			annotU = annotatedU;
			id = Utils.getAttrAnnotationBloc(annotatedU, "xml:id");
			start = getTimeValue(Utils.getAttrAnnotationBloc(annotatedU, "start"));
			end = getTimeValue(Utils.getAttrAnnotationBloc(annotatedU, "end"));
			speakerCode = Utils.getAttrAnnotationBloc(annotatedU, "who");
			if (optionsOutput != null) {
				if (optionsOutput.isDontDisplay(speakerCode)) return;
				if (!optionsOutput.isDoDisplay(speakerCode)) return;
			}
			speakerName = getParticipantName(speakerCode);
			NodeList annotUElements = annotatedU.getChildNodes();
			cleanedSpeech = "";
			speech = "";
			//Parcours des éléments contenus dans u et construction des variables speech, cleanedSpeech et speeches en fonction.
			for(int i = 0; i<annotUElements.getLength(); i++){
				if (Utils.isElement(annotUElements.item(i))){
					Element annotUEl = (Element)annotUElements.item(i);
					String nodeName = annotUEl.getNodeName();
					/*Cas élément "u":
					 * dans ce cas, on concatène tous les u pour former cleanedSpeech, de même pour speech mais en incluant les éventuels bruits/incidents qui ont lieu lors de l'énoncé
					 * (sous la forme d'une chaîne de caractère prédéfinie).
					 * On ajoute tous les énoncés dans speeches.
					 */
					if(nodeName.equals("u")) {
						NodeList us = annotUEl.getChildNodes();
						processSeg(us);
					}
					//Ajout des tiers
					else if(nodeName.equals("spanGrp")){
						String type = annotUEl.getAttribute("type");
						if (optionsOutput != null) {
							if (optionsOutput.level == 1) return;
							if (optionsOutput.isDontDisplay(type)) return;
							if (!optionsOutput.isDoDisplay(type)) return;
						}
						NodeList spans = annotUEl.getElementsByTagName("span");
						for (int y = 0 ; y<spans.getLength(); y++){
							Element span = (Element)spans.item(y);
							
							tiers.add(new Annot(type, span.getTextContent()));

							tierTypes.add(type);//
							if (!type.equals("pho") && !type.equals("act") && !type.equals("sit") && !type.equals("com") && !type.equals("morpho")){
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
			for(int z = 0; z<us.getLength(); z++){
				Node segChild = us.item(z);
				String segChildName = segChild.getNodeName();
				//Ajout des pauses: syntaxe = # pour les pauses courtes, sinon ### pour les pauses longues
				if(Utils.isElement(segChild)){
					Element segChildEl = (Element) segChild;
					if(segChildName.equals("pause")){
						if(segChildEl.getAttribute("type").equals("short")){
							cleanedSpeech += shortPause;
							speech += shortPause;
							utt += shortPause;
						}
						else if(segChildEl.getAttribute("type").equals("long")){
							cleanedSpeech += longPause;
							speech += longPause;
							utt += longPause;
						}
						else if(segChildEl.getAttribute("type").equals("verylong")){
							cleanedSpeech += veryLongPause;
							speech += veryLongPause;
							utt += veryLongPause;
						}
						else if(segChildEl.getAttribute("type").equals("chrono")){
							String chronoPause = "#" + segChildEl.getAttribute("dur") + " ";
							cleanedSpeech += chronoPause;
							speech += chronoPause;
							utt += chronoPause;
						}
					}

					//Ajout des évènement (éléments incident & vocal):
					//syntaxe = * type|subtype|desc1 desc2 ... descN /*
					else if(segChildName.equals("incident")){
						if(segChildEl.getAttribute("subtype").equals("pronounce")){													
							String [] speechSplit = speech.split("\\s");
							String lastW = "";
							try{
								lastW = speechSplit[speechSplit.length-1];
							}
							catch(Exception e){}
							if(lastW.contains("]")){
								lastW = "";
							}
							String v = joinString(speechSplit, 0, speechSplit.length);
							speech += v;
							utt += v;
							String pron = "[" + lastW + ", " + getIncidentDesc(segChildEl, "desc") + "] ";
							if(!Utils.isNotEmptyOrNull(speech.trim())){
								speech += pron;
								utt += pron;
							}
						}
						else if(segChildEl.getAttribute("subtype").equals("language")){
							String [] speechSplit = speech.split("\\s");
							//String lastW = speechSplit[speechSplit.length-1];
							String v = joinString(speechSplit, 0, speechSplit.length) + " $ lang=" + getIncidentDesc(segChildEl, "desc") + " X/$ ";
							speech += v;
							utt += v;
						}
						else{
							String event = "* " + segChildEl.getAttribute("subtype");
							try{
								NodeList descs = segChildEl.getElementsByTagName("desc");
								if(descs.getLength()!=0){ event+= "|";}
								for(int q = 0; q < descs.getLength(); q++){
									Element desc = (Element) descs.item(q);
									event += desc.getAttribute("type");
									event += ":" + desc.getTextContent() + " ";
								}
							}
							finally{
								speech += event + "B/* ";
								utt += event + "B/* ";
							}
						}
					}
					else if (segChildName.equals("vocal")){
						String vocal = "* vocal|";
						try{ 
							vocal += "|" + segChildEl.getElementsByTagName("desc").item(0).getTextContent();
						}
						finally{
							speech += vocal + "/* ";
							utt += vocal + "/* ";
						}
					}
					else if(segChildName.equals("seg")){
						processSeg(segChildEl.getChildNodes());
					}
					else if(segChildName.equals("anchor") && !segChildEl.getAttribute("synch").startsWith("#au")){
						String sync = getTimeValue(segChildEl.getAttribute("synch"));
						// creer une ligne avec speech, cleanedSpeech, addspeech
						speeches.add(start + ";" + sync + "__" + utt);
						start = sync;
						// System.out.printf("anchor: %s %s %s%n", start, sync, utt);
						utt = "";
					}
					//Tiers de type "morpho"
					else if(segChildName.equals("morpho")){
						String tierName="morpho";
						String tierMorpho="";
						NodeList cN = ((Element)segChild).getElementsByTagName("w");
						for (int j=0; j<cN.getLength(); j++){
							Node w = cN.item(j);
							tierMorpho += w.getTextContent();
							NamedNodeMap attrs = w.getAttributes();
							for(int jz=0; jz<attrs.getLength(); jz++){
								Node att = attrs.item(jz);
								tierMorpho += "|" + att.getNodeName() + ":" + att.getNodeValue();
							}
							tierMorpho =" ";
						}
						tiers.add(new Annot(tierName, tierMorpho));
						tierTypes.add("morpho");
					}
				}
				else if(segChild.getNodeName().equals("#text")){
					String content = segChild.getTextContent() + " ";
					if (Utils.isNotEmptyOrNull(content.trim())){
						speech += content;
						cleanedSpeech += content;
						//Dans speeches, pour chaque énoncé (seg), il peut y avoir des marques temporelles
						//On ajoute donc chaque énoncé sous cette forme
						//start;end__speech
						utt += content;
					}
				}
			}
			speeches.add(start + ";" + end + "__" + utt);
		}

		//Récupération d'un type de tier donné (passé en paramètre)

		public ArrayList<Annot> getTier(String typeTier){
			ArrayList<Annot> t = new ArrayList<Annot>();
			if (typeTier.equals("pho")|| typeTier.equals("act")  || typeTier.equals("sit")  || typeTier.equals("com")  || typeTier.equals("morpho")) {
				for (Annot tier : this.tiers){
					if (typeTier.equals(tier.name)){
						t.add(tier);
					}
				}
			}
			else{
				for (Annot tier : this.tiers){
					String tpT = tier.name;
					if (!tpT.equals("pho") && !tpT.equals("act") && !tpT.equals("sit") && !tpT.equals("com") && !tpT.equals("morpho")){
						t.add(tier);
					}
				}
			}
			return t;
		}

		public String joinString(String [] stringSplit, int begin, int end){
			String sentence = "";
			for(int i = begin; i<end; i++){
				sentence += stringSplit[i] + " ";
			}
			return sentence;
		}

		public String getIncidentDesc(Element incident, String attName){
			String attValue = "";
			NodeList descs = incident.getElementsByTagName("desc");
			for(int i = 0; i<descs.getLength(); i++){
				Element desc = (Element) descs.item(i);
				if(desc.getAttribute("type").equals(attName)){
					return desc.getTextContent();
				}
			}
			return attValue;
		}
	}

	/**
	 * Représentation des Div
	 */
	public class Div{
		//Thème/sujet du div
		public String theme;
		//Identifiant du thème
		public String themeId;
		//Temps de début du div (en secondes)
		public String start;
		//Temps de fin du div (en secondes)
		public String end;
		//Type du div
		public String type;
		//liste des Utterances contenues dans le Div
		public ArrayList<AnnotatedUtterance> utterances = new ArrayList<AnnotatedUtterance>();
		//Element div
		public Element divElement;

		public Div(TeiFile tf, Element div, String id, String theme){
			//initialisation des variables d'instances
			this.theme = theme;
			this.themeId = id;
			this.type = Utils.getDivHeadAttr(div, "type");
			this.start = getTimeValue(Utils.getDivHeadAttr(div, "start"));
			this.end = getTimeValue(Utils.getDivHeadAttr(div, "end"));
			divElement = div;
			//Noeuds contenus dans le div
			NodeList ch = div.getChildNodes();
			//Fin de sous-div
			boolean eg = false;
			boolean first = true;
			//Parcours des éléments contenus dans le div (au niveau 1)
			for (int i = 0; i<ch.getLength(); i++){
				if(Utils.isElement(ch.item(i))){
					Element el = (Element) ch.item(i);
					if(Utils.isAnnotatedBloc(el)){
						if(getNote(el)!=null){
							Element note = getNote(el);
							AnnotatedUtterance lastU = getLastAnnotU();
							if(lastU != null){
								lastU.coms.add(note.getAttribute("type") + "\t" + note.getTextContent());
							}
						}
						else{
							AnnotatedUtterance utt = new AnnotatedUtterance(el);
							//Si c'est le premier u, on lui ajoute le type du div
							if (first == true) {
								utt.type = this.type + "\t" + this.themeId;
								if(this.type.toLowerCase().startsWith("bg")){
									//Si le type est Bg, on indique qu'il s'agit d'un sous-div
									eg = true;
								}
								first = false;
							}
							//Ajout de l'utterance: dans la liste d'utterances du div, et dans la liste d'utterance principale de la transcription
							this.utterances.add(utt);
							mainLines.add(utt);
						}
					}
					//Cas sous-div
					else if(el.getNodeName().equals("div")){
						String elid = Utils.getDivHeadAttr(el, "subtype");
						String eltheme = tf.transInfo.situations.get(elid);
						Div subdiv = new Div(tf, el, elid, eltheme);
						this.utterances.addAll(subdiv.utterances);
					}
				}
			}
			if(eg){
				AnnotatedUtterance lastU = getLastAnnotU();
				lastU.type = "Eg\t" + this.themeId;
			}
		}

		public AnnotatedUtterance getLastAnnotU(){
			AnnotatedUtterance lastAnnotU = null;
			try{
				lastAnnotU = this.utterances.get(utterances.size()-1);
			}
			catch(Exception e){
				if(!this.utterances.isEmpty()){
					lastAnnotU = this.utterances.get(0);
				}
			}
			return lastAnnotU;
		}
	}

	//Récupération du nom d'un participant à partir de son identifiant (code) 
	public String getParticipantName(String participantID){
		for (Participant p : this.transInfo.participants){
			if (participantID.equals(p.id) && p.name != null && ! p.name.isEmpty()){
				return p.name;
			}
		}
		return participantID;
	}

	public static Element getNote(Element e){
		NodeList nl = e.getChildNodes();
		for(int i = 0; i<nl.getLength(); i++){
			if(nl.item(i).getNodeName().equals("note")){
				return (Element) nl.item(i);
			}
		}
		return null;
	}

	//Vérifie si l'élément contient au moins un div
	public static boolean containsDiv(Element div){
		NodeList cn = div.getChildNodes();
		int l = cn.getLength();
		for (int i = 0; i < l ; i++){
			if (cn.item(i).getNodeName().equals("div")){
				return true;
			}
		}
		return false;
	}

	//Récupération d'un type de note
	public static Element getNote(String type, Element notesStmt){
		Element addNotes = (Element) notesStmt.getElementsByTagName("note").item(0);
		NodeList notes = addNotes.getElementsByTagName("note");
		for (int i = 0; i< notes.getLength(); i++){
			if(Utils.isElement(notes.item(i))){
				Element note = (Element) notes.item(i);
				if (note.getAttribute("type").equals(type)){
					return note;
				}
			}
		}
		return null;
	}

	public static int getDivOcc(NodeList nl){
		int divOcc = 0;
		for(int i = 0; i<nl.getLength(); i++){
			if(nl.item(i).getNodeName().equals("div")){
				divOcc++;
			}
		}
		return divOcc;
	}

	//Main
	public static void main (String [] args) {
		TeiFile tf = new TeiFile(new File(args[0]), null);
		for (Div d : tf.trans.divs){
			for (AnnotatedUtterance u : d.utterances){
				u.print();
			}
		}
	}
}
