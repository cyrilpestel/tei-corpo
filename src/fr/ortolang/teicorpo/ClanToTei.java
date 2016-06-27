/**
 * ClanToTei permet de convertir des fichiers au format Chat en un format TEI pour la représentation de données orales.
 * @author Myriam Majdoub
 *
 */

package fr.ortolang.teicorpo;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ClanToTei {
	static String EXT = ".cha";

	// Variables d'instance
	/** Structure de donnée contenant les informations du fichier Chat. */
	ChatFile cf;
	/** Fichier Chat. */
	File chatFile;
	/** Nouveau document TEI. */
	Document docTEI;
	/** Racine du document TEI. */
	Element rootTEI;
	// acces Xpath
	public XPathFactory xPathfactory;
	public XPath xpath;
	/** Element timeline */
	// Element timeline;
	/** Liste des types de tiers présents dans le fichier */
	HashSet<String> tiersNames;
	ArrayList<String> times = new ArrayList<String>();
	ArrayList<Element> timeElements;
	Double maxTime = 0.0;
	TierParams tparams;

	/**
	 * Identifiant des éléments <strong>desc</strong> de <strong>text</strong>.
	 */
	static int descID;
	/** Identifiant des éléments <strong>u</strong> de <strong>text</strong>. */
	static int utteranceId;
	static int whenId;

	/**
	 * Construction de l'objet ChatFile à partir du fichier CHAT.
	 * 
	 * @param chatFileName
	 *            Nom du fichier CHAT à convertir.
	 * @throws Exception
	 */
	// Constructeur: initialise le ChatFile et le docTEI
	public ClanToTei(String chatFileName, TierParams tp) throws Exception {
//		System.err.printf("ClanToTei %s -- %s %n", chatFileName, tp);
		if (tp == null) tp = new TierParams();
		descID = 0;
		utteranceId = 0;
		whenId = 0;
		tparams = (tp != null) ? tp : new TierParams();
		tiersNames = new HashSet<String>();
		cf = new ChatFile();
		chatFile = new File(chatFileName);
		cf.load(chatFileName);
		cf.findInfo(false);
		// ajouter paramètre
		if (!tp.nospreadtime) cf.cleantime_inmemory(1);
		timeElements = new ArrayList<Element>();
		DocumentBuilderFactory factory = null;

		try {
			factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Utils.setDTDvalidation(factory, true);
			docTEI = builder.newDocument();
			this.xPathfactory = XPathFactory.newInstance();
			this.xpath = xPathfactory.newXPath();
			this.xpath.setNamespaceContext(new NamespaceContext() {
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
			rootTEI = docTEI.createElement("TEI");
			rootTEI.setAttribute("version", Utils.versionTEI);
			this.rootTEI.setAttribute("xmlns", "http://www.tei-c.org/ns/1.0");
			docTEI.appendChild(rootTEI);
		} catch (Exception e) {
			e.printStackTrace();
		}
		conversion(tp.options);
	}

	/**
	 * Conversion du fichier CHAT: création d'un TEI vide, puis mise à jour des
	 * éléments <strong>teiHeader</strong> et <strong>text</strong>.
	 * 
	 * @throws IOException
	 * @throws DOMException
	 */
	// public void conversion(String extension) throws DOMException,
	// IOException{
	public void conversion(String extension) throws DOMException, IOException {
		this.buildEmptyTEI();
		this.buildHeader();
		this.buildText(extension);
		setDurDate();
		setDivTimes();
		addTemplateDesc();
		addTimeline();
	}

	/**
	 * Création d'un Document TEI minimal.
	 */
	public void buildEmptyTEI() {
		Element teiHeader = this.docTEI.createElement("teiHeader");
		this.rootTEI.appendChild(teiHeader);
		Element fileDesc = this.docTEI.createElement("fileDesc");
		teiHeader.appendChild(fileDesc);

		Element titleStmt = this.docTEI.createElement("titleStmt");
		fileDesc.appendChild(titleStmt);
		Element publicationStmt = docTEI.createElement("publicationStmt");
		fileDesc.appendChild(publicationStmt);

		// Ajout publicationStmt
		Element distributor = docTEI.createElement("distributor");
		distributor.setTextContent("tei_corpo");
		publicationStmt.appendChild(distributor);

		Element notesStmt = docTEI.createElement("notesStmt");
		fileDesc.appendChild(notesStmt);
		Element sourceDesc = this.docTEI.createElement("sourceDesc");
		fileDesc.appendChild(sourceDesc);

		Element profileDesc = this.docTEI.createElement("profileDesc");
		teiHeader.appendChild(profileDesc);
		Element encodingDesc = this.docTEI.createElement("encodingDesc");
		teiHeader.appendChild(encodingDesc);
		Element revisionDesc = this.docTEI.createElement("revisionDesc");
		teiHeader.appendChild(revisionDesc);
		Utils.setRevisionInfo(this.docTEI, revisionDesc, chatFile.getAbsolutePath(), null);

		Element text = this.docTEI.createElement("text");
		this.rootTEI.appendChild(text);
		Element timeline = this.docTEI.createElement("timeline");
		text.appendChild(timeline);
		Element body = docTEI.createElement("body");
		text.appendChild(body);
		Element when = this.docTEI.createElement("when");
		when.setAttribute("absolute", "0");
		when.setAttribute("xml:id", "T" + whenId);
		// timeline.appendChild(when);
		timeElements.add(when);
		timeline.setAttribute("unit", "s");
		times.add("0.0");
	}

	/**
	 * Remplissage du header.
	 * 
	 * @throws IOException
	 * @throws DOMException
	 */
	public void buildHeader() throws DOMException, IOException {
		////// Mise à jour de l'élémentFileDesc: titre + info enregistrement+
		////// date + lieu+ éventuelles notes
		setFileDesc();
		setProfileDesc();
		setEncodingDesc();
	}

	/**
	 * Mise à jour de l'élémént encodingDesc: informations sur le logiciel qui a
	 * généré le document d'origine (ici Clan)
	 */
	public void setEncodingDesc() {
		Element encodingDesc = (Element) docTEI.getElementsByTagName("encodingDesc").item(0);
		Element appInfo = this.docTEI.createElement("appInfo");
		encodingDesc.appendChild(appInfo);
		Element application = this.docTEI.createElement("application");
		application.setAttribute("ident", "Clan");
		application.setAttribute("version", "Chat");
		appInfo.appendChild(application);
		Element desc = this.docTEI.createElement("desc");
		application.appendChild(desc);
		desc.setTextContent("Transcription created with CLAN/CHAT and converted to TEI_CORPO - Soft: " + Utils.versionSoft);
	}

	/**
	 * Mise à jour de l'élément <strong>fileDesc</strong>, correspondant à la
	 * description formelle du fichier.<br>
	 * Contient les informations sur le fichiers et sur l'enregistrement dont
	 * provient la transcription.
	 * 
	 * @throws IOException
	 * @throws DOMException
	 */
	public void setFileDesc() throws DOMException, IOException {
		// titleStmt
		Element titleStmt = (Element) this.docTEI.getElementsByTagName("titleStmt").item(0);
		Element title = docTEI.createElement("title");
		titleStmt.appendChild(title);
		title.setTextContent("Fichier TEI obtenu à partir du fichier CLAN " + new File(cf.filename()).getName());

		// sourceDesc
		Element sourceDesc = (Element) this.docTEI.getElementsByTagName("sourceDesc").item(0);
		Element recordingStmt = docTEI.createElement("recordingStmt");
		sourceDesc.appendChild(recordingStmt);
		Element recording = docTEI.createElement("recording");
		recordingStmt.appendChild(recording);
		// Element media
		Element media = docTEI.createElement("media");
		recording.appendChild(media);
		if (tparams.mediaName != null) {
			media.setAttribute("mimeType", Utils.findMediaType(tparams.mediaName));
			media.setAttribute("url", tparams.mediaName);
		} else if (cf.mediaFilename != null) {
			String url = Utils.findClosestMedia(chatFile.getParent(), cf.mediaFilename.toUpperCase(), cf.mediaType);
			media.setAttribute("mimeType", Utils.findMediaType(url));
			media.setAttribute("url", url);
		}

		if (cf.timeDuration != null) {
			ChatLine cl = new ChatLine(cf.timeDuration);
			recording.setAttribute("dur", cl.tail);
		}

		Element notesStmt = (Element) this.docTEI.getElementsByTagName("notesStmt").item(0);
		Element addNotes = docTEI.createElement("note");
		addNotes.setAttribute("type", "COMMENTS_DESC");
		notesStmt.appendChild(addNotes);

		if (cf.birth != null) {
			Element note = docTEI.createElement("note");
			addNotes.appendChild(note);
			ChatLine cl = new ChatLine(cf.birth);
			note.setAttribute("type", cl.head);
			note.setTextContent(cl.tail);
		}

		if (!cf.comments.isEmpty() || !cf.otherInfo.isEmpty() || cf.transcriber != null) {
			if (cf.transcriber != null) {
				Element note = docTEI.createElement("note");
				note.setAttribute("type", "scribe");
				ChatLine cl = new ChatLine(cf.transcriber);
				note.setTextContent(cl.tail);
				addNotes.appendChild(note);
			}
			for (String com : cf.comments) {
				Element note = docTEI.createElement("note");
				ChatLine cl = new ChatLine(com);
				note.setTextContent(cl.tail);
				note.setAttribute("type", "comment");
				addNotes.appendChild(note);
			}

			for (String info : cf.otherInfo) {
				Element note = docTEI.createElement("note");
				ChatLine cl = new ChatLine(info);
				note.setAttribute("type", cl.head);
				note.setTextContent(cl.tail);
				addNotes.appendChild(note);
			}
		}
	}

	// Construction de l'élément profileDesc : settingDesc + Stmt
	/**
	 * Mise à jour de l'élément <strong>profileDesc</strong>, correspondant à la
	 * description sémantique du fichier.<br>
	 * Contient les informations sur les s et les situations de
	 * l'enregistrement.
	 */
	public void setProfileDesc() {
		Element profileDesc = (Element) docTEI.getElementsByTagName("profileDesc").item(0);
		setSettingDesc(profileDesc);
		setStmt(profileDesc);
	}

	/**
	 * Mise à jour de l'élément <strong>settingDesc</strong>: contient les
	 * situations qui ont eu lieues lors de l'enregistrement.
	 * 
	 * @param profileDesc
	 *            L'élément <strong>profileDesc</strong> auqel est rattaché le
	 *            <strong>settingDesc</strong>.
	 */
	public void setSettingDesc(Element profileDesc) {
		Element settingDesc = docTEI.createElement("settingDesc");
		profileDesc.appendChild(settingDesc);

		if (cf.lang != null) {
			ChatLine cl = new ChatLine(cf.lang);
			rootTEI.setAttribute("xml:lang", cl.tail.trim());
		}

		if (cf.location != null) {
			Element place = docTEI.createElement("place");
			Element placeName = docTEI.createElement("placeName");
			place.appendChild(placeName);
			ChatLine cl = new ChatLine(cf.location);
			placeName.setTextContent(cl.tail);
			settingDesc.appendChild(place);
		}
	}

	/**
	 * creation du premier div
	 */
	public Element setFirstDiv() {
		Element settingDesc = (Element) docTEI.getElementsByTagName("settingDesc").item(0);
		Element setting = docTEI.createElement("setting");
		Element activity = docTEI.createElement("activity");
		setting.appendChild(activity);
		setting.setAttribute("xml:id", "d" + descID);
		Element body = (Element) docTEI.getElementsByTagName("body").item(0);
		Element div = Utils.createDivHead(docTEI);
		body.appendChild(div);
		div.setAttribute("subtype", "d" + descID);
		if (cf.situation != null) {
			ChatLine cl = new ChatLine(cf.situation);
			activity.setTextContent(cl.tail);
			div.setAttribute("type", "Situation");
		}
		settingDesc.appendChild(setting);
		return div;
	}

	/**
	 * Création d'un nouvel élément <strong>div</strong>.
	 * 
	 * @param parent
	 *            L'élément <strong>div</strong> auquel est rattaché la nouvelle
	 *            section.
	 * @param subj
	 *            Sujet de la nouvelle section.
	 */
	public Element addNewDiv(Element parent, String type, String subj) {
		Element setting = docTEI.createElement("setting");
		Element activity = docTEI.createElement("activity");
		setting.appendChild(activity);
		activity.setTextContent(subj);
		descID++;
		setting.setAttribute("xml:id", "d" + descID);
		Element settingDesc = (Element) docTEI.getElementsByTagName("settingDesc").item(0);
		settingDesc.appendChild(setting);
		Element div = Utils.createDivHead(docTEI);
		div.setAttribute("subtype", "d" + descID);
		div.setAttribute("type", type);
		parent.appendChild(div);
		return div;
	}

	/**
	 * Mise à jour des éléments <strong>person</strong> et de leurs attributs.
	 * 
	 * @param profileDesc
	 */
	public void setStmt(Element profileDesc) {
		Element particDesc = docTEI.createElement("particDesc");
		profileDesc.appendChild(particDesc);
		Element listPerson = docTEI.createElement("listPerson");
		particDesc.appendChild(listPerson);

		for (ChatFile.ID part : cf.ids) {
			Element person = docTEI.createElement("person");
			listPerson.appendChild(person);
			if (Utils.isNotEmptyOrNull(part.name)) {
				Element name = docTEI.createElement("persName");
				name.setTextContent(part.name);
				person.appendChild(name);
			}
			if (Utils.isNotEmptyOrNull(part.corpus)) {
				person.setAttribute("source", part.corpus);
			}
			if (Utils.isNotEmptyOrNull(part.code)) {
				Element altGrp = docTEI.createElement("altGrp");
				Element alt = docTEI.createElement("alt");
				alt.setAttribute("type", part.code);
				person.appendChild(altGrp);
				altGrp.appendChild(alt);
			}
			if (Utils.isNotEmptyOrNull(part.language)) {
				Element langKnowledge = docTEI.createElement("langKnowledge");
				Element langKnown = docTEI.createElement("langKnown");
				if (part.language == "fra" || part.language == "fr")
					langKnown.setTextContent("français");
				else if (part.language == "eng" || part.language == "en")
					langKnown.setTextContent("english");
				else
					langKnown.setTextContent(part.language);
				langKnown.setAttribute("tag", part.language);
				langKnowledge.appendChild(langKnown);
				person.appendChild(langKnowledge);
			}
			if (Utils.isNotEmptyOrNull(part.age)) {
				person.setAttribute("age", part.age);
			}
			if (Utils.isNotEmptyOrNull(part.sex)) {
				if (part.sex.toLowerCase().substring(0, 1).equals("m")) {
					person.setAttribute("sex", "1");
				} else if (part.sex.toLowerCase().substring(0, 1).equals("f")) {
					person.setAttribute("sex", "2");
				} else {
					person.setAttribute("sex", "9");
				}
			}
			if (Utils.isNotEmptyOrNull(part.role)) {
				person.setAttribute("role", part.role);
			}
			if (Utils.isNotEmptyOrNull(part.SES)) {
				Element socecStatus = docTEI.createElement("socecStatus");
				socecStatus.setTextContent(part.SES);
				person.appendChild(socecStatus);
			}
			if (Utils.isNotEmptyOrNull(part.education)) {
				Element educ = docTEI.createElement("education");
				educ.setTextContent(part.education);
				person.appendChild(educ);
			}
			if (Utils.isNotEmptyOrNull(part.group)) {
				Element n = docTEI.createElement("note");
				n.setTextContent(part.group);
				n.setAttribute("type", "group");
				person.appendChild(n);
			}
			if (Utils.isNotEmptyOrNull(part.customfield)) {
				Element n = docTEI.createElement("note");
				n.setTextContent(part.customfield);
				n.setAttribute("type", "customField");
				person.appendChild(n);
			}
		}
	}

	/**
	 * Construction de l'élément <strong>text</strong>: contient la
	 * transcription.
	 * 
	 * @param extension
	 *            (information for tiers mor, etc.)
	 */
	public void buildText(String extension) {
		Element div = setFirstDiv(); // initializes the initial div
		int size = cf.nbMainLines(), i;
		// skip initial header
		for (i = 0; i < size; i++) {
			if (cf.ml(i).startsWith("*") || cf.ml(i).toLowerCase().startsWith("@g") || cf.ml(i).toLowerCase().startsWith("@bg")) {
				break;
			}
		}
		buildTextDiv(div, i, extension, 0); // starts at first line
	}

	/**
	 * Construction de l'élément <strong>text</strong>: contient la
	 * transcription.
	 * 
	 * @param div
	 *            - the div currently growing
	 * @param ptr
	 *            - pointer to the original mainlines tab (cf.ml)
	 * @param extension
	 *            - (information for tiers mor, etc.)
	 * @param inGem
	 *            - 0 not gen active, 1 @g active, 2 @bg active (change the
	 *            action when closing a gem)
	 */
	public int buildTextDiv(Element div, int ptr, String extension, int inGem) {
		int size = cf.nbMainLines(), i = ptr;
		try {
			while (i < size) {
				ChatLine cl = new ChatLine(cf.ml(i));
				String start = Integer.toString(cf.startMl(i));
				String end = Integer.toString(cf.endMl(i));
				if (cl.head.startsWith("@")) {
					if (cl.head.toLowerCase().startsWith("@g")) {
						if (inGem == 1) {
							// was in @G
							// close the current div and continue
							// with inG true we are always with a sub call
							// ends function
							return i;
							// this finishes the div and process again a
							// beginning of div
						} else if (inGem == 2) {
							// was in @BG
							// do not close the current div
							// but creates a new one
							// starts a div
							Element newdiv = addNewDiv(div, "G", cl.tail);
							i = buildTextDiv(newdiv, i + 1, extension, 1); // starts
																			// a
																			// new
																			// process
																			// in
																			// a
																			// gem
						} else {
							// starts a div
							Element newdiv = addNewDiv(div, "G", cl.tail);
							i = buildTextDiv(newdiv, i + 1, extension, 1); // starts
																			// a
																			// new
																			// process
																			// in
																			// a
																			// gem
						}
					} else if (cl.head.toLowerCase().startsWith("@bg")) {
						if (inGem == 1) {
							// was in @G
							// close the current div and continue
							// with inG true we are always with a sub call
							// ends function
							return i;
							// this finishes the div and process again a
							// beginning of div
						} else if (inGem == 2) {
							// was in @BG
							// do not close the current div
							// but creates a new one
							// starts a div
							Element newdiv = addNewDiv(div, "BG", cl.tail);
							i = buildTextDiv(newdiv, i + 1, extension, 2); // starts
																			// a
																			// new
																			// process
																			// in
																			// a
																			// gem
						} else {
							// starts a div
							Element newdiv = addNewDiv(div, "BG", cl.tail);
							i = buildTextDiv(newdiv, i + 1, extension, 2); // starts
																			// a
																			// new
																			// process
																			// in
																			// a
																			// gem
						}
					} else if (cl.head.toLowerCase().startsWith("@eg")) {
						if (inGem == 1) {
							// close first the @G
							return i;
						} else {
							// close the current div and continue
							// with inG true we are always with a sub call
							// ends function
							return i + 1;
						}
					} else if (cl.head.toLowerCase().startsWith("@end")) {
						i++; // could stop process if we wanted to - normal end
								// of file
					} else {
						// this should not happen
						// this is not within the chat format
						System.err.println("unknown format at " + cl.head + " " + cl.tail);
						Element annotatedU = build_comment(start, end, cl);
						div.appendChild(annotatedU);
						i++;
					}
				} else {
					// add to the current div
					int tierSize = cf.nbTiers(i);
					String[] tiers = new String[tierSize];
					for (int j = 0; j < tierSize; j++) {
						tiers[j] = cf.t(i, j);
					}
					Element annotatedU = build_u_element(start, end, cl, tiers, extension);
					set_AnnotU_element(tiers, annotatedU);
					div.appendChild(annotatedU);
					i++;
				}
			}
			// end close all remaining @G and @BG
			if (inGem != 0)
				return i;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return i;
	}

	/**
	 * Construction et mise à jour des élément <strong>u</strong> à partir d'une
	 * ligne de ChatFile.
	 * 
	 * @param utterance
	 * @return
	 */
	public Element build_comment(String startTime, String endTime, ChatLine cl) {
		Element incident = this.docTEI.createElement("incident");
		Element desc = this.docTEI.createElement("desc");
		desc.appendChild(incident);
		desc.setAttribute("xml:id", "au" + utteranceId);
		utteranceId++;
		desc.setTextContent(cl.tail);
		if (!startTime.isEmpty())
			desc.setAttribute("start", startTime);
		if (!endTime.isEmpty())
			desc.setAttribute("end", endTime);
		if (!cl.head.isEmpty())
			desc.setAttribute("type", cl.head);
		return incident;
	}

	/**
	 * Construction et mise à jour des élément <strong>u</strong> à partir d'une
	 * ligne de ChatFile.
	 * 
	 * @param utterance
	 * @return
	 */
	public Element build_u_element(String startTime, String endTime, ChatLine cl, String[] tiers, String extension) {
		Element annotatedU = Utils.createAnnotationBloc(docTEI);
		Utils.setAttrAnnotationBloc(docTEI, annotatedU, "xml:id", "au" + utteranceId);
		utteranceId++;
		// tiersNames.add(Utils.ANNOTATIONBLOC);
		if (extension.equals("xmor") || extension.equals("mor"))
			splitWcontent(cl.tail.replaceAll("\\s+", " "), annotatedU, tiers, extension);
		else if (extension.equals("xmorext") || extension.equals("morext"))
			splitWcontentWithRepetition(cl.tail.replaceAll("\\s+", " "), annotatedU, tiers, extension);
		Utils.setAttrAnnotationBloc(docTEI, annotatedU, "who", cl.head.substring(1));
		if (!startTime.equals("-1")) {
			String startId = addTimeToTimeline(toSeconds(startTime));
			Utils.setAttrAnnotationBloc(docTEI, annotatedU, "start", startId);
		}
		if (!endTime.equals("-1")) {
			String endId = addTimeToTimeline(toSeconds(endTime));
			Utils.setAttrAnnotationBloc(docTEI, annotatedU, "end", endId);
		}
		splitUContent(cl.tail, annotatedU);
		return annotatedU;
	}

	/**
	 * Ajout des tiers dans les éléments <strong>u</strong>.
	 * 
	 * @param tiers
	 * @param u
	 */
	public void set_AnnotU_element(String[] tiers, Element u) {
		if (tiers.length > 0) {
			for (String tier : tiers) {
				Element spanGrp = docTEI.createElement("spanGrp");
				u.appendChild(spanGrp);
				ChatLine cl = new ChatLine(tier);
				if (cl.head.equals("tim")) {
					Element time = docTEI.createElement("time");
					time.setAttribute("when", cl.tail);
					u.appendChild(time);
				} else {
					this.tiersNames.add(cl.head.substring(1));
					Element span = docTEI.createElement("span");
					spanGrp.setAttribute("type", cl.head.substring(1));
					// Element seg = docTEI.createElement("seg");
					// span.setAttribute("type", tierType);
					span.setTextContent(cl.tail.replaceAll("\\s+", " "));
					spanGrp.appendChild(span);
					// span.appendChild(seg);
				}
			}
		}
	}

	public void addTimeline() {
		Utils.sortTimeline(timeElements);
		Element timeline = (Element) docTEI.getElementsByTagName("timeline").item(0);
		for (Element when : timeElements) {
			timeline.appendChild(when);
		}
	}

	/**
	 * Epure et split les tiers selon l'extension donnée en input.
	 * 
	 * @param tiers
	 *            L'étiquetage morpho-syntaxique à splitter.
	 * @param extension
	 *            L'option -f renseignée en input par l'utilisateur.
	 */
	public static String[] tierExtension(String[] tiers, String extension) {
		for (String tier : tiers) {
			ChatLine cl = new ChatLine(tier);
			cl.tail = cl.tail.replaceAll(".[0-9]+_[0-9]+.", ""); // vire le
																	// caractère
																	// &#21 si
																	// restant;
			cl.tail = cl.tail.replaceAll("\\p{C}", "?");
			if (cl.head.equals(extension))
				return cl.tail.split("\\s+");
			else
				return cl.tail.split("\\s+");
		}
		return tiers;
	}

	public static String getSubj(String[] altElement) {
		String subj = "";
		for (int i = 3; i < altElement.length; i++) {
			subj += altElement[i] + " ";
		}
		return subj;
	}

	/**
	 * Epure et split la phrase.
	 * 
	 * @param source
	 *            La phrase à nettoyer et splitter.
	 */
	public static String[] epureSource(String source) {

		source = source.replaceAll("<.+\\[\\/\\/?\\] ?", ""); // vire les
																// u2ments
																// répétés
																// balisés
		source = source.replaceAll(",,", ""); // vire les double virgules
		source = source.replaceAll(",", ""); // vire les virgules
		source = source.replaceAll("\\[.\\/?\\]", ""); // vire les crochets
														// contenant 1 caract.
		source = source.replaceAll("\\[[^\\]|\\/]+\\]", ""); // vire les
																// crochets
																// contenant
																// +sieurs
																// caract.
		source = source.replaceAll("\\(\\.\\.?\\)", ""); // vire les pauses
		source = source.replaceAll("@.", ""); // vire les indications de type @
		String[] s = source.split("\\s+|\\-'|\\_"); // multiple delimiters with
													// regex |\\
		return s;
	}

	/**
	 * Epure et split la copie de la phrase.
	 * 
	 * @param source_bis
	 *            La copie de la phrase, dont on vire les répétitions.
	 */
	public static String[] vireRepet(String source_bis) {

		source_bis = source_bis.replaceAll("<[^>]+> \\[\\/\\/?\\] ", ""); // vire
																			// les
																			// éléments
																			// approximatifs
																			// répétés
																			// balisés
		// source_bis = source_bis.replaceAll("\\b[^\\s]+\\b \\[.\\] ", ""); //
		// vire les mots approximatifs répétés
		source_bis = source_bis.replaceAll("\\[.\\/?\\] ", ""); // vire les
																// marqueurs
																// contenant 1
																// caract.
		// source_bis = source_bis.replaceAll("xxx ", ""); // vire les mots
		// incompris
		// source_bis = source_bis.replaceAll("www ", ""); // vire les mots non
		// retrancscrits
		source_bis = source_bis.replaceAll("0 ", ""); // vire les bruits
		String[] s_bis = epureSource(source_bis);
		return s_bis;
	}

	/**
	 * Repère les énoncés répétés dans la phrase mais pas étiquettés
	 * morpho-syntaxiquement et stocke dans l'élément <strong>w<strong>.
	 * 
	 * @param source
	 *            La phrase à nettoyer.
	 * @param morpho
	 *            L'élément <strong>morpho<strong> auquel se rattachent les
	 *            mots.
	 */
	public int matchRepetContent(String source, Element morpho) {

		try {
			// Matche les u2ments/mots approximatifs répétés balisés suivis de
			// [//] ou [/]
			String regex = "(<[^>]+>) \\[\\/\\/?\\]";
			Pattern p = Pattern.compile(regex);
			Matcher m = p.matcher(source);

			int j = 0;
			while (m.find()) {
				j++;
				String[] repet = m.group(1).split("\\s+|\\-'|\\_");
				for (int i = 0; i < repet.length; i++) {

					Element r = docTEI.createElement("w");
					repet[i] = epureFinal(repet[i]);
					r.setTextContent(repet[i]);
					morpho.appendChild(r);
					r.setAttribute("repet", "" + (j));
				}
				return m.start();
			}
		} catch (PatternSyntaxException pse) {
			pse.printStackTrace();
		}
		return -1;
	}

	/**
	 * Permet de décomposer une phrase en plusieurs éléments mots.<br>
	 * La phrase est alors décomposée en éléments <strong>w</strong> auxquels on
	 * attribue des attributs <strong>pos</strong> et <strong>lemm</strong>.
	 * 
	 * @param source
	 *            La phrase à splitter.
	 * @param u
	 *            L'élément <strong>u<strong> auquel se rattache la phrase.
	 * @param tiers
	 *            L'étiquetage morpho-syntaxique à redéployer sur les mots de la
	 *            phrase.
	 * @param extension
	 *            L'option -f renseignée en input par l'utilisateur.
	 */
	public void splitWcontent(String source, Element annotatedU, String[] tiers, String extension) {

		Element morpho = docTEI.createElement("morpho");
		String source_bis = source;
		String source_ter = source;
		matchRepetContent(source, morpho); // repère les u2ments répétés
											// approximatifs non étiquetés
		String[] s = epureSource(source_ter); // épure et splitte la copie de
												// l'énoncé
		String[] t = tierExtension(tiers, extension); // splitte le contenu de
														// la tier selon son
														// extension
		String[] s_bis = vireRepet(source_bis); // épure et splitte la copie de
												// l'énoncé de ses répétitions

		// Si l'énoncé épuré de ses répétitions et la tier étiquetée n'ont pas
		// la même longueur
		if (s_bis.length != t.length) {
			for (int i = 0; i < s.length; i++) {
				Element w = docTEI.createElement("w");

				// On redéploie l'énoncé brut, sans attribut
				try {
					s[i] = epureFinal(s[i]);
					w.setTextContent(s[i]);
					morpho.appendChild(w);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			// Si l'énoncé épuré de ses répétitions et la tier étiquetée ont la
			// même longueur
		} else {
			for (int i = 0; i < t.length; i++) {
				Element w = docTEI.createElement("w");

				try {
					// Si tout va bien
					if (t[i].indexOf('|') != -1) {

						String pos = t[i].substring(0, t[i].indexOf('|'));
						String lemme = t[i].substring(t[i].indexOf('|') + 1, t[i].length());

						// Teste la présence d'un autre pipe -> pos
						if (lemme.contains("|")) {
							pos += "," + lemme.substring(0, lemme.indexOf("|"));
							lemme = lemme.replace(lemme.substring(0, lemme.indexOf("|") + 1), "");
						}
						// Teste la présence d'un suffixe, qu'on découpe
						if (lemme.contains("&")) {
							String suffixe = lemme.substring(lemme.indexOf("&") + 1, lemme.length());
							lemme = lemme.replace("&" + suffixe, "");
							w.setAttribute("suf", suffixe);
						}
						if (lemme.contains("-")) {
							String suffixe = lemme.substring(lemme.indexOf("-") + 1, lemme.length());
							lemme = lemme.replace("-" + suffixe, "");
							w.setAttribute("suf", suffixe);
						}

						w.setAttribute("pos", pos);
						w.setAttribute("lemma", lemme);

					}
					// Si l'énoncé et la tier étiquetée contiennent la même
					// ponctuation
					else if ((t[i].indexOf('|') == -1) && (s_bis[i].equals(t[i]))) {
						w.setAttribute("punct", t[i]);
					}
					// Si le mot de l'énoncé commence par '&' -> bruit
					else if (Utils.isNotEmptyOrNull(s_bis[i]) && s_bis[i].charAt(0) == '&') {
						w.setAttribute("bruit", s_bis[i].substring(1, s_bis[i].length()));
					} else {
						w.setAttribute("warning", "");
					}
				} catch (Exception e) {
					System.out.print("Warning : ");
					e.printStackTrace();
					w.setAttribute("warning", "");
				}

				s_bis[i] = epureFinal(s_bis[i]);

				w.setTextContent(s_bis[i]);
				morpho.appendChild(w);
			}
		}
		annotatedU.appendChild(morpho);
	}

	/**
	 * Permet de décomposer une phrase en plusieurs éléments mots.<br>
	 * Avec tentative de récupération des répétitions La phrase est alors
	 * décomposée en éléments <strong>w</strong> auxquels on attribue des
	 * attributs <strong>pos</strong> et <strong>lemm</strong>.
	 * 
	 * @param source
	 *            La phrase à splitter.
	 * @param u
	 *            L'élément <strong>u<strong> auquel se rattache la phrase.
	 * @param tiers
	 *            L'étiquetage morpho-syntaxique à redéployer sur les mots de la
	 *            phrase.
	 * @param extension
	 *            L'option -f renseignée en input par l'utilisateur.
	 */
	public void splitWcontentWithRepetition(String source, Element annotatedU, String[] tiers, String extension) {

		Element morpho = docTEI.createElement("morpho");
		matchRepetContent(source, morpho); // repère les u2ments répétés
											// approximatifs non étiquettés
		String[] s = epureSource(source); // épure l'énoncé
		String[] t = tierExtension(tiers, extension); // récupère le contenu de
														// la tier selon son
														// extension

		boolean triplx = false; // présence de mots non retranscrits
		boolean repetMot = false; // présence de mots répétés
		boolean repetu2 = false; // présence de u2ments répétés
		int indice = 0; // indice de début de la répétition
		int pass = 0; // indice du nombre de mots non retrancrits dans un meme
						// énoncé
		int top = 0; // indice du nombre de mots/u2ment répétés dans un meme
						// énoncé

		for (int i = 0; i < s.length; i++) {

			Element w = docTEI.createElement("w");

			// Calcule l'indice de début d'une répétition
			if (s[i].indexOf("<") != -1)
				indice = i;

			try {
				// Si l'énoncé contient des mots non retranscrits
				if ((s[i].equals("xxx")) || (s[i].equals("www")) || (s[i].equals("0")) || (s[i].indexOf('0') == 0)
						|| (s[i].indexOf('&') == 0)) {

					w.setAttribute("warning", s[i]);
					triplx = true;
					pass++;
				}

				// Si l'énoncé contient des mots ou u2ments répétés
				else if ((s[i].equals("[/]")) || (s[i].equals("[//]"))) {

					if (s[i - 1].indexOf(">") != -1) {
						// s[i-1] = s[i-1].substring(0, s[i-1].length()-1);
						w.setAttribute("warning", s[i]);
						repetu2 = true;
					} else {
						w.setAttribute("warning", s[i]);
						repetMot = true;
					}
					top++;
				}

				// Si la boucle a déjà rencontré un mot répété dans l'énoncé,
				// non étiqueté
				else if (repetMot == true) {

					// Redécale selon le nombre de passages
					if (top > 1) {

						if (t[i - (2 + top)].indexOf('|') != -1) {

							String pos = t[i - (2 + top)].substring(0, t[i - (2 + top)].indexOf('|'));
							String lemme = t[i - (2 + top)].substring(t[i - (2 + top)].indexOf('|') + 1,
									t[i - (2 + top)].length());

							// Teste la présence d'un autre pipe -> pos
							if (lemme.contains("|")) {
								pos += "," + lemme.substring(0, lemme.indexOf("|"));
								lemme = lemme.replace(lemme.substring(0, lemme.indexOf("|") + 1), "");
							}
							// Teste la présence d'un suffixe, qu'on découpe
							if (lemme.contains("&")) {
								String suffixe = lemme.substring(lemme.indexOf("&") + 1, lemme.length());
								lemme = lemme.replace("&" + suffixe, "");
								w.setAttribute("suf", suffixe);
							}
							if (lemme.contains("-")) {
								String suffixe = lemme.substring(lemme.indexOf("-") + 1, lemme.length());
								lemme = lemme.replace("-" + suffixe, "");
								w.setAttribute("suf", suffixe);
							}
							w.setAttribute("pos", pos);
							w.setAttribute("lemma", lemme);
						} else if ((t[i - (2 + top)].indexOf('|') == -1) && (s[i].equals(t[i - (2 + top)]))) {
							w.setAttribute("punct", t[i - (2 + top)]);
						} else {
							w.setAttribute("unkn", t[i - (2 + top)]);
							repetMot = false;
						}
					} else {
						if (t[i - 2].indexOf('|') != -1) {

							String pos = t[i - 2].substring(0, t[i - 2].indexOf('|'));
							String lemme = t[i - 2].substring(t[i - 2].indexOf('|') + 1, t[i - 2].length());

							// Teste la présence d'un autre pipe -> pos
							if (lemme.contains("|")) {
								pos += "," + lemme.substring(0, lemme.indexOf("|"));
								lemme = lemme.replace(lemme.substring(0, lemme.indexOf("|") + 1), "");
							}
							// Teste la présence d'un suffixe, qu'on découpe
							if (lemme.contains("&")) {
								String suffixe = lemme.substring(lemme.indexOf("&") + 1, lemme.length());
								lemme = lemme.replace("&" + suffixe, "");
								w.setAttribute("suf", suffixe);
							}
							if (lemme.contains("-")) {
								String suffixe = lemme.substring(lemme.indexOf("-") + 1, lemme.length());
								lemme = lemme.replace("-" + suffixe, "");
								w.setAttribute("suf", suffixe);
							}
							w.setAttribute("pos", pos);
							w.setAttribute("lemma", lemme);
						} else if ((t[i - 2].indexOf('|') == -1) && (s[i].equals(t[i - 2]))) {
							w.setAttribute("punct", t[i - 2]);
						} else {
							w.setAttribute("unkn", t[i - 2]);
							repetMot = false;
						}
					}
				}

				// Si la boucle a déjà rencontré un u2ment répété dans l'énoncé,
				// non étiqueté
				else if (repetu2 == true) {
					if (t[indice].indexOf('|') != -1) {
						String pos = t[indice].substring(0, t[indice].indexOf('|'));
						String lemme = t[indice].substring(t[indice].indexOf('|') + 1, t[indice].length());

						// Teste la présence d'un autre pipe -> pos
						if (lemme.contains("|")) {
							pos += "," + lemme.substring(0, lemme.indexOf("|"));
							lemme = lemme.replace(lemme.substring(0, lemme.indexOf("|") + 1), "");
						}
						// Teste la présence d'un suffixe, qu'on découpe
						if (lemme.contains("&")) {
							String suffixe = lemme.substring(lemme.indexOf("&") + 1, lemme.length());
							lemme = lemme.replace("&" + suffixe, "");
							w.setAttribute("suf", suffixe);
						}
						if (lemme.contains("-")) {
							String suffixe = lemme.substring(lemme.indexOf("-") + 1, lemme.length());
							lemme = lemme.replace("-" + suffixe, "");
							w.setAttribute("suf", suffixe);
						}
						w.setAttribute("pos", pos);
						w.setAttribute("lemma", lemme);
					} else if ((t[indice].indexOf('|') == -1) && (s[i].equals(t[indice]))) {
						w.setAttribute("punct", t[indice]);
					} else {
						w.setAttribute("unkn", t[indice]);
						repetu2 = false;
					}
					indice++;
				}

				// Si la boucle a déjà rencontré un mot présent dans l'énoncé,
				// absent dans l'étiquetage
				else if (triplx == true) {

					// Redécale selon le nombre de passages
					if (pass > 1) {

						if (t[i - 1].indexOf('|') != -1) {
							String pos = t[i - pass].substring(0, t[i - pass].indexOf('|'));
							String lemme = t[i - pass].substring(t[i - pass].indexOf('|') + 1, t[i - pass].length());

							// Teste la présence d'un autre pipe -> pos
							if (lemme.contains("|")) {
								pos += "," + lemme.substring(0, lemme.indexOf("|"));
								lemme = lemme.replace(lemme.substring(0, lemme.indexOf("|") + 1), "");
							}
							// Teste la présence d'un suffixe, qu'on découpe
							if (lemme.contains("&")) {
								String suffixe = lemme.substring(lemme.indexOf("&") + 1, lemme.length());
								lemme = lemme.replace("&" + suffixe, "");
								w.setAttribute("suf", suffixe);
							}
							if (lemme.contains("-")) {
								String suffixe = lemme.substring(lemme.indexOf("-") + 1, lemme.length());
								lemme = lemme.replace("-" + suffixe, "");
								w.setAttribute("suf", suffixe);
							}
							w.setAttribute("pos", pos);
							w.setAttribute("lemma", lemme);
						} else if ((t[i - pass].indexOf('|') == -1) && (s[i].equals(t[i - pass]))) {
							w.setAttribute("punct", t[i - pass]);
						} else {
							w.setAttribute("unkn", t[i - pass]);
						}
					} else {
						if (t[i - 1].indexOf('|') != -1) {
							String pos = t[i - 1].substring(0, t[i - 1].indexOf('|'));
							String lemme = t[i - 1].substring(t[i - 1].indexOf('|') + 1, t[i - 1].length());

							// Teste la présence d'un autre pipe -> pos
							if (lemme.contains("|")) {
								pos += "," + lemme.substring(0, lemme.indexOf("|"));
								lemme = lemme.replace(lemme.substring(0, lemme.indexOf("|") + 1), "");
							}
							// Teste la présence d'un suffixe, qu'on découpe
							if (lemme.contains("&")) {
								String suffixe = lemme.substring(lemme.indexOf("&") + 1, lemme.length());
								lemme = lemme.replace("&" + suffixe, "");
								w.setAttribute("suf", suffixe);
							}
							if (lemme.contains("-")) {
								String suffixe = lemme.substring(lemme.indexOf("-") + 1, lemme.length());
								lemme = lemme.replace("-" + suffixe, "");
								w.setAttribute("suf", suffixe);
							}
							w.setAttribute("pos", pos);
							w.setAttribute("lemma", lemme);
						} else if ((t[i - 1].indexOf('|') == -1) && (s[i].equals(t[i - 1]))) {
							w.setAttribute("punct", t[i - 1]);
						} else {
							w.setAttribute("unkn", t[i - 1]);
							triplx = false;
						}
					}
				} else if (t[i].indexOf('|') != -1) {
					String pos = t[i].substring(0, t[i].indexOf('|'));
					String lemme = t[i].substring(t[i].indexOf('|') + 1, t[i].length());
					// Teste la présence d'un autre pipe -> pos
					if (lemme.contains("|")) {
						pos += "," + lemme.substring(0, lemme.indexOf("|"));
						lemme = lemme.replace(lemme.substring(0, lemme.indexOf("|") + 1), "");
					}
					// Teste la présence d'un suffixe, qu'on découpe
					if (lemme.contains("&")) {
						String suffixe = lemme.substring(lemme.indexOf("&") + 1, lemme.length());
						lemme = lemme.replace("&" + suffixe, "");
						w.setAttribute("suf", suffixe);
					}
					if (lemme.contains("-")) {
						String suffixe = lemme.substring(lemme.indexOf("-") + 1, lemme.length());
						lemme = lemme.replace("-" + suffixe, "");
						w.setAttribute("suf", suffixe);
					}
					w.setAttribute("pos", pos);
					w.setAttribute("lemma", lemme);
				}

				// Si l'énoncé et la tier étiquetée contiennent la meme
				// ponctuation
				else if ((t[i].indexOf('|') == -1) && (s[i].equals(t[i]))) {
					w.setAttribute("punct", t[i]);
				}
				// Si le mot de l'énoncé commence par '&' -> bruit
				else if (s[i].charAt(0) == '&') {
					w.setAttribute("bruit", s[i].substring(1, s[i].length()));
				} else {
					w.setAttribute("warning", "");
				}
			} catch (Exception e) {
				System.out.print("Warning : ");
				e.printStackTrace();
				w.setAttribute("warning", "");

			}

			s[i] = epureFinal(s[i]);
			w.setTextContent(s[i]);
			morpho.appendChild(w);
		}
		annotatedU.appendChild(morpho);
	}

	/**
	 * Permet d'épurer le mot.
	 * 
	 * @param w
	 *            L'élément <strong>w<strong> contenant le mot.
	 */
	public String epureFinal(String w) {

		w = w.replaceAll(">", "");
		w = w.replaceAll("<", "");
		return w;
	}

	/**
	 * Permet de decomposer une phrase en plusieurs éléments si celle ci est
	 * entrecoupee de pauses.<br>
	 * La phrase est alors decomposee en éléments <strong>u </strong> et
	 * <strong>pause</strong>.
	 * 
	 * @param source
	 *            La phrase à splitter.
	 * @param annotatedU
	 *            L'élément <strong>u<strong> à laquelle se rattache la phrase.
	 */
	public void splitUContent(String source, Element annotatedU) {
		String[] s = source.split(" ");
		String uContent = "";
		Element u = docTEI.createElement("u");
		Element pause = docTEI.createElement("pause");
		Element seg = docTEI.createElement("seg");

		for (String el : s) {
			boolean addPause = false;
			if (el.startsWith("(")) {
				if (el.equals("(.)") || el.toLowerCase().startsWith("(pause)")) {
					addPause = addPause(seg, pause, "short", "");
				} else if (el.equals("(..)")) {
					addPause = addPause(seg, pause, "long", "");
				} else if (el.equals("(...)")) {
					addPause = addPause(seg, pause, "verylong", "");
				} else if (el.matches("\\(\\d+\\.\\d+\\)")) {
					addPause = addPause(seg, pause, "chrono", el.substring(1, el.length() - 1));
				}
				if (addPause) {
					// uContent = convTerm(uContent);// deleteControlChars(convTerm(uContent));
					Node content = docTEI.createTextNode(uContent);
					seg.appendChild(content);
					annotatedU.appendChild(u);
					u.appendChild(seg);
					seg.appendChild(pause);
					uContent = "";
				}
			}
			if (addPause == false) {
				uContent += el += " ";
			}
		}
		// uContent = convTerm(uContent).replaceAll("\\s+", " ");// deleteControlChars(convTerm(uContent));
		uContent = uContent.replaceAll("\\s+", " "); // deleteControlChars(convTerm(uContent));
		Node content = docTEI.createTextNode(uContent);
		seg.appendChild(content);
		seg.appendChild(content);
		u.appendChild(seg);
		annotatedU.appendChild(u);
	}

	public boolean addPause(Element u, Element pause, String pauseType, String pauseDur) {
		pause.setAttribute("type", pauseType);
		if (pauseType.equals("chrono")) {
			pause.setAttribute("dur", pauseDur);
		}
		u.appendChild(pause);
		return true;
	}

	/**
	 * Crée le fichier de sortie contenant le nouveau Document TEI.
	 * 
	 * @param outputFileName
	 *            Le nom du fichier de sortie. public void createFile(String
	 *            outputFileName) {
	 * 
	 *            Source source = new DOMSource(this.docTEI); Result resultat =
	 *            new StreamResult(outputFileName);
	 * 
	 *            try { // Configuration du transformer TransformerFactory
	 *            fabrique2 = TransformerFactory.newInstance(); Transformer
	 *            transformer = fabrique2.newTransformer();
	 *            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	 *            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
	 *            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,
	 *            Utils.teiCorpoDtd()); // Transformation
	 *            transformer.transform(source, resultat); } catch (Exception e)
	 *            { e.printStackTrace(); } }
	 */

	public void setDivTimes() {
		NodeList divs = null;
		try {
			divs = Utils.getAllDivs(this.xpath, this.docTEI);
		} catch (XPathExpressionException e1) {
			e1.printStackTrace();
		}
		// System.out.println("nb divs: " + divs.getLength());
		for (int i = 0; i < divs.getLength(); i++) {
			Element div = (Element) divs.item(i);
			// System.out.println(i + " " + div.getAttribute("type"));
			NodeList annotUElmts = null;
			try {
				annotUElmts = Utils.getSomeAnnotationBloc(this.xpath, div);
			} catch (XPathExpressionException e) {
				e.printStackTrace();
			}
			// System.out.println(i + " :- " + annotUElmts.getLength());
			if (annotUElmts.getLength() != 0) {
				String start = "";
				String end = "";
				for (int j = 0; j < annotUElmts.getLength(); j++) {
					Element currentU = (Element) annotUElmts.item(j);
					// System.out.println(j + " " +
					// currentU.getAttribute("type"));
					String time = Utils.getAttrAnnotationBloc(currentU, "start");
					// System.out.println("start: "+time);
					if (Utils.isNotEmptyOrNull(time) && start == "") {
						start = time;
					}
					time = Utils.getAttrAnnotationBloc(currentU, "end");
					// System.out.println("end: "+time);
					if (Utils.isNotEmptyOrNull(time)) {
						end = time;
					}
				}
				String startId = addTimeToTimeline(getTimeValue(start));
				String endId = addTimeToTimeline(getTimeValue(end));
				Utils.setDivHeadAttr(this.docTEI, div, "start", startId);
				Utils.setDivHeadAttr(this.docTEI, div, "end", endId);
			}
		}
	}

	/**
	 * Ajoute au document l'information de la durée de l'enregistrement.
	 */
	public void setDurDate() {
		Element recording = (Element) this.docTEI.getElementsByTagName("recording").item(0);
		Element date = this.docTEI.createElement("date");
		recording.appendChild(date);
		ChatLine cl = new ChatLine(cf.date);
		if (Utils.isNotEmptyOrNull(cl.tail)) {
			date.setTextContent(cl.tail);
		}
		date.setAttribute("dur", String.valueOf(maxTime));
	}

	public String getTimeValue(String timeId) {
		if (Utils.isNotEmptyOrNull(timeId)) {
			return times.get(Integer.parseInt(timeId.split("#T")[1]));
		}
		return "";
	}

	public String addTimeToTimeline(String time) {
		if (Utils.isNotEmptyOrNull(time)) {
			Double t = Double.parseDouble(time);
			if (t > maxTime)
				maxTime = t;
			String id = "";
			if (times.contains(time)) {
				id = "T" + times.indexOf(time);
			} else {
				times.add(time);
				Element when = docTEI.createElement("when");
				when.setAttribute("interval", time);
				whenId++;
				id = "T" + whenId;
				when.setAttribute("xml:id", id);
				when.setAttribute("since", "#T0");
				// timeline.appendChild(when);
				timeElements.add(when);
			}
			return "#" + id;
		} else {
			return "";
		}
	}

	/*
	 * String deleteControlChars(String line){ StringBuffer buff = new
	 * StringBuffer(); char c; for (int pos = 0; pos < line.length(); pos++) { c
	 * = line.charAt(pos); if (Character.isISOControl(c)) { buff.append(' '); }
	 * else { buff.append(c); } } return buff.toString().replaceAll("\\x21",
	 * ""); }
	 */

	public void removeNote(Element add) {
		try {
			Element notesStmt = (Element) docTEI.getElementsByTagName("notesStmt").item(0);
			NodeList notes = notesStmt.getElementsByTagName("note");
			for (int i = 0; i < notes.getLength(); i++) {
				Element note = (Element) notes.item(i);
				if (note.getTextContent().equals(add.getTextContent())) {
					notesStmt.removeChild(note);
				}
			}
		} catch (Exception e) {
		}
	}

	public void addTemplateDesc() {
		Element fileDesc = (Element) this.docTEI.getElementsByTagName("fileDesc").item(0);
		Element notesStmt = (Element) fileDesc.getElementsByTagName("notesStmt").item(0);
		Element templateNote = docTEI.createElement("note");
		templateNote.setAttribute("type", "TEMPLATE_DESC");
		notesStmt.appendChild(templateNote);

		// Ajout des locuteurs dans les templates
		Element particDesc = (Element) this.docTEI.getElementsByTagName("particDesc").item(0);
		NodeList persons = particDesc.getElementsByTagName("person");
		for (int i = 0; i < persons.getLength(); i++) {
			Element person = (Element) persons.item(i);
			Element note = docTEI.createElement("note");

			Element noteType = docTEI.createElement("note");
			noteType.setAttribute("type", "type");
			noteType.setTextContent("-");
			note.appendChild(noteType);

			Element noteParent = docTEI.createElement("note");
			noteParent.setAttribute("type", "parent");
			noteParent.setTextContent("-");
			note.appendChild(noteParent);
			if (person.getElementsByTagName("alt").getLength() > 0) {
				Element alt = (Element) person.getElementsByTagName("alt").item(0);

				Element noteCode = docTEI.createElement("note");
				noteCode.setAttribute("type", "code");
				noteCode.setTextContent(alt.getAttribute("type"));
				note.appendChild(noteCode);

			}
			templateNote.appendChild(note);
		}
		for (String tierName : this.tiersNames) {
			Element note = docTEI.createElement("note");

			Element noteCode = docTEI.createElement("note");
			noteCode.setAttribute("type", "code");
			noteCode.setTextContent(tierName);
			note.appendChild(noteCode);

			Element noteType = docTEI.createElement("note");
			noteType.setAttribute("type", "type");
			noteType.setTextContent(LgqType.SYMB_ASSOC);
			note.appendChild(noteType);

			String parent = Utils.ANNOTATIONBLOC;
			if (tierName.toLowerCase().equals(Utils.ANNOTATIONBLOC)) {
				parent = "-";
			}
			Element noteParent = docTEI.createElement("note");
			noteParent.setAttribute("type", "parent");
			noteParent.setTextContent(parent);
			note.appendChild(noteParent);

			templateNote.appendChild(note);
		}
	}

	public static String toSeconds(String time_milliseconds) {
		return Float.toString(Float.parseFloat(time_milliseconds) / 1000);
	}

	public static String convTerm(String s) {
		String patternStr = "([^#])(\\+\\.\\.\\.|\\+/\\.|\\+!\\?|\\+//\\.|\\+/\\?|\\+\"/\\.|\\+\"\\.|\\+//\\?|\\+\\.\\.\\?|\\+\\.|\\.|\\?|!\\s*$)";
		Pattern pattern = Pattern.compile(patternStr);
		Matcher matcher = pattern.matcher(s);
		if (matcher.find()) {
			s = s.replace(matcher.group(), matcher.group(1) + " {" + matcher.group(2)) + " /T}";
		}
		return s;
	}

	public static void usage() {
		System.err.println("Description: ClanToTei convertit un fichier au format Chat en un fichier au format TEI");
		System.err.println("Usage: ClanToTei [-options] file" + EXT);
		System.err.println(
				"	     :-i nom du fichier ou repertoire où se trouvent les fichiers Chat/Clan à convertir (les fichiers ont pour extension "
						+ EXT);
		System.err.println("	     :-o nom du fichier de sortie au format TEI (.xml) ou du repertoire de résultats");
		System.err.println(
				"	     	si cette option n'est pas spécifié, le fichier de sortie aura le même nom que le fichier d'entrée, avec l'extension .xml;");
		System.err.println(
				"	     	si on donne un repertoire comme input et que cette option n'est pas spécifiée, les résultats seront stockées dans le même dossier que l'entrée.");
		// System.err.println(" :-f mor lecture et traitement des champs %mor");
		// System.err.println(" :-f xmor lecture et traitement des champs
		// %xmor");
		// System.err.println(" :-f morext lecture et traitement des champs %mor
		// avec extension des répétitions (attention: résultats à contrôler)");
		// System.err.println(" :-f xmorext lecture et traitement des champs
		// %xmor avec extension des répétitions (attention: résultats à
		// contrôler)");
		System.err.println("	     :-m media - nom du fichier média associé");
		System.err.println("	     :--pure - do not use special tags but only classic tei tags");
		System.err.println("	     :--nospreadtime - do not try to ajust time for unaligned utterances");
		System.err.println("	     :-usage ou -help = affichage de ce message");
		System.exit(1);
	}

	public static void main(String[] args) throws Exception {
		Utils.printVersionMessage();
		submain(args);
	}

	public static void submain(String[] args) throws Exception {
		TierParams tp = new TierParams();
		tp.input = null;
		tp.output = null;
		tp.mediaName = null;
		tp.options = "";
		boolean nospreadtime = false;
		// parcours des arguments
		if (args.length == 0) {
			System.err.println("Vous n'avez spécifié aucun argument.\n");
			usage();
		} else {
			for (int i = 0; i < args.length; i++) {
				try {
					if (args[i].equals("-i")) {
						i++;
						if (i >= args.length)
							usage();
						tp.input = args[i];
					} else if (args[i].equals("-o")) {
						i++;
						if (i >= args.length)
							usage();
						tp.output = args[i];
					} else if (args[i].equals("-m")) {
						i++;
						if (i >= args.length)
							usage();
						tp.mediaName = args[i];
					} else if (args[i].equals("--nospreadtime")) {
						nospreadtime = true;
					} else if (args[i].equals("--pure")) {
						Utils.teiStylePure = true;
					} else if (args[i].equals("-f")) {
						i++;
						if (i >= args.length)
							usage();
						if (args[i].equals("mor")) {
							tp.options = "mor";
						} else if (args[i].equals("xmor")) {
							tp.options = "xmor";
						} else if (args[i].equals("morext")) {
							tp.options = "morext";
						} else if (args[i].equals("xmorext")) {
							tp.options = "xmorext";
						} else {
							usage();
						}
					} else {
						usage();
					}
				} catch (Exception e) {
					usage();
				}
			}
		}

		File f = new File(tp.input);

		tp.input = f.getCanonicalPath();

		if (f.isDirectory()) {
			File[] chatFiles = f.listFiles();

			if (tp.output == null) {
				if (tp.input.endsWith("/")) {
					tp.output = tp.input.substring(0, tp.input.length() - 1);
				} else {
					tp.output = tp.input;
				}
			}

			File outFile = new File(tp.output);
			if (outFile.exists()) {
				if (!outFile.isDirectory()) {
					System.out.println("\n Erreur :" + tp.output
							+ " est un fichier, vous devez spécifier un nom de dossier pour le stockage des résultats. \n");
					usage();
					System.exit(1);
				}
			}

			if (!tp.output.endsWith("/")) {
				tp.output += "/";
			}
			new File(tp.output).mkdir();

			for (File file : chatFiles) {
				if (file.getName().toLowerCase().endsWith(Utils.EXT_PUBLISH + EXT)) {
					System.out.printf("-- ignoré: %s%n", file.getName());
				} else if (file.getName().endsWith(EXT)) {
					ClanToTei tr = new ClanToTei(file.getAbsolutePath(), tp);
					String outputFileName = Utils.basename(file) + Utils.EXT;
					System.out.println(tp.output + outputFileName);
					Utils.setDocumentName(tr.docTEI, Utils.lastname(outputFileName));
					Utils.createFile(tp.output + outputFileName, tr.docTEI);
				} else if (file.isDirectory()) {
					args[0] = "-i";
					args[1] = file.getAbsolutePath();
					submain(args);
				}
			}
		} else {
			if (tp.output == null) {
				tp.output = Utils.fullbasename(tp.input) + Utils.EXT;
			} else if (new File(tp.output).isDirectory()) {
				if (tp.output.endsWith("/")) {
					tp.output = tp.output + Utils.basename(tp.input) + Utils.EXT;
				} else {
					tp.output = tp.output + "/" + Utils.basename(tp.input) + Utils.EXT;
				}
			}

			if (!(Utils.validFileFormat(tp.input, EXT))) {
				System.err.println("Le fichier d'entrée du programme doit avoir l'extension " + EXT);
				usage();
			}

			System.out.println("Lecture de " + tp.input);
			ClanToTei tr = new ClanToTei(tp.input, tp);
			Utils.setDocumentName(tr.docTEI, Utils.lastname(tp.output));
			Utils.createFile(tp.output, tr.docTEI);
			System.out.println("New file TEI created: " + tp.output);
		}
	}
}
