/**
 * @author Myriam Majdoub
 * TeiToTranscriber: convertit un fichier teiml en un fichier au format Transcriber, conformément à la dtd trans-14.dtd.
 */

package fr.ortolang.teicorpo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import fr.ortolang.teicorpo.TeiFile.Participant;

public class TeiToTranscriber extends TeiConverter {
	// Document transcriber
	Document trsDoc;
	// Element Trans: racine du document Transcriber
	Element trans;
	// Element Episode: contient la transcription de l'enregistrement
	Element episode;
	// temporary variables used to construct the sections and turns
	String speakers;
	Element section;
	ArrayList<TranscriberTurn> turns;
	String oldEndTime;
	boolean sectionStartSet;
	boolean sectionEndSet;
	boolean shiftNextStart;
	boolean noComments;

	// Extension du fichier de sortie: .trs
	final static String EXT = ".trs";

	public TeiToTranscriber(String inputName, String outputName, TierParams optionsTei) {
		super(inputName, outputName, optionsTei);
		if (this.tf == null) return;
		speakers = null; // name of current speaker for turns
		section = null; // is a section opened ?
		oldEndTime = ""; // last end time of a turn
		if (optionsTei.level == 1)
			noComments = true;
		sectionStartSet = false;
		sectionEndSet = false;
		shiftNextStart = false;
		outputWriter();
		conversion();
	}

	// Création du document trs
	public void outputWriter() {
		this.trsDoc = null;
		DocumentBuilderFactory factory = null;
		try {
			factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			this.trsDoc = builder.newDocument();
			this.trans = this.trsDoc.createElement("Trans");
			this.trsDoc.appendChild(trans);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Conversion du fichier teiml
	public void conversion() {
		buildHeader();
		buildText();
	}

	// Ajout des informations relatives à l'enregistrement et à la situation
	// d'énonciation
	public void buildHeader() {
		setTransAttributes();
		buildSpks();
		buildTopics();
	}

	// Mise à jour des attributs de l'élément Trans
	public void setTransAttributes() {
		// Attributs trans : audio_filename, scribe, xml:lang, version,
		// version_date, elapsed_time
		if (Utils.isNotEmptyOrNull(tf.transInfo.medianame))
			setAttr(trans, "audio_filename", Utils.basename(tf.transInfo.medianame), false);
		else
			setAttr(trans, "audio_filename", "", false);
		setAttr(trans, "scribe", tf.transInfo.transcriber, false);
		setAttr(trans, "xml:lang", tf.language, false);
		setAttr(trans, "version", tf.transInfo.version, false);
		setAttr(trans, "version_date", tf.transInfo.date, false);
		setAttr(trans, "elapsed_time", tf.transInfo.timeDuration, false);
	}

	// Construction de l'élément Speakers
	public void buildSpks() {
		if (!tf.transInfo.participants.isEmpty()) {
			Element spks = trsDoc.createElement("Speakers");
			trans.appendChild(spks);
			for (Participant p : tf.transInfo.participants) {
				addSpk(spks, p);
			}
		}
	}

	// Ajout d'un speaker
	public void addSpk(Element spks, Participant p) {
		// Attributs speaker: id, name, check, type, dialect, accent, scope
		// Problème: transcriber ne prend que les informations citées ci-dessus,
		// pas de possibilité de commentaires dans
		// un élément Speaker, donc si autres infos, sont perdues ...
		Element spk = trsDoc.createElement("Speaker");
		// Les attributs id et name sont obligatoires
		setAttr(spk, "id", cleanId(p.id), true);
		if (Utils.isNotEmptyOrNull(p.name)) {
			setAttr(spk, "name", p.name, true);
		} else {
			setAttr(spk, "name", p.id, true);
		}

		// Attribut check : 2 valeurs possibles "yes" ou "no"
		String val = p.adds.get("check");
		if (Utils.isNotEmptyOrNull(val)) {
			if (val.equals("yes"))
				setAttr(spk, "check", "yes", false);
			else if (val.equals("no"))
				setAttr(spk, "check", "no", false);
		}

		// Si rôle = child ou children, va dans l'attribut type
		if (Utils.isNotEmptyOrNull(p.role)) {
			if (p.role.equals("child") || p.role.equals("children")) {
				setAttr(spk, "type", "child", false);
			}
		}

		// Attribut dialect: liste prédéfinies de valeurs: native | nonnative
		val = p.adds.get("dialect");
		if (Utils.isNotEmptyOrNull(val)) {
			if (val.equals("native"))
				setAttr(spk, "dialect", "native", false);
			else if (val.equals("nonative"))
				setAttr(spk, "dialect", "nonative", false);
		}

		// Attribut sex de TEI va dans role, liste prédéfinie de valeurs
		if (Utils.isNotEmptyOrNull(p.sex)) {
			setAttr(spk, "type", convertSex(p.sex), false);
		}
		setAttr(spk, "accent", p.adds.get("accent"), false);

		// Attribut scope: valeurs possibles : "local" ou "global"
		val = p.adds.get("scope");
		if (Utils.isNotEmptyOrNull(val)) {
			if (val.equals("local"))
				setAttr(spk, "scope", "local", false);
			else if (val.equals("global"))
				setAttr(spk, "scope", "global", false);
		}
		spks.appendChild(spk);
	}

	private String cleanId(String id) {
		if (id.isEmpty()) return "";
		id = id.replaceAll(" ", "-");
		String initial = id.substring(0,1);
		if (initial.matches("[0-9]")) id = "x" + id;
		return id;
	}

	// Construction de l'élément Topics
	public void buildTopics() {
		if (!tf.transInfo.situations.isEmpty()) {
			Element topics = trsDoc.createElement("Topics");
			trans.appendChild(topics);
			Element settingDesc = (Element) tf.teiDoc.getElementsByTagName("settingDesc").item(0);
			NodeList settings = settingDesc.getElementsByTagName("setting");
			for (int i = 0; i < settings.getLength(); i++) {
				Element setting = (Element) settings.item(i);
				addTopic(topics, setting);
			}
		}
	}

	// Ajout d'un élément Topic
	public void addTopic(Element topics, Element setting) {
		// Attributs topic : "id", "desc"
		Element topic = trsDoc.createElement("Topic");
		String id = setting.getAttribute("xml:id");
		if (!Utils.isNotEmptyOrNull(id))
			id = "xx";
		setAttr(topic, "id", id, true);
		setAttr(topic, "desc", setting.getTextContent().replaceAll("\\s+", " ").trim(), true);
		topics.appendChild(topic);
	}

	// Construction de l'élément Episode (contient la transcription)
	public void buildText() {
		// Mise à jour des attributs de l'élément Episode
		// Episode : attributs "program" et "air_date"
		episode = trsDoc.createElement("Episode");
		trans.appendChild(episode);
		/*
		 * the episode is the first div or it does not exists
		 */
		Element body = (Element) (tf.teiDoc.getElementsByTagName("body").item(0));
		// TEST if there is only one div
		NodeList bodyChildren = body.getChildNodes();
		int first = -1;
		for (int i = 0; i < bodyChildren.getLength(); i++) {
			Node n = bodyChildren.item(i);
			if (Utils.isElement(n)) {
				if (n.getNodeName().equals("div")) {
					if (first != -1) {
						// more than one div: no episode
						first = -2;
						break;
					}
					first = i;
				} else {
					// no only divs
					first = -2;
					break;
				}
			}
		}

		if (first >= 0) {
			Element ep = (Element) bodyChildren.item(first);
			// only one div so it's the episode
			// un seul div c'est l'épisode
			// on cherche les infos program et air_date
			// sinon on met type + substype dans program

			String type = Utils.getDivHeadAttr(ep, "type");
			String subtype = Utils.getDivHeadAttr(ep, "subtype");
			String program = Utils.getDivHeadAttr(ep, "program");
			String air_date = Utils.getDivHeadAttr(ep, "air_date");
			if (Utils.isNotEmptyOrNull(program))
				episode.setAttribute("program", program);
			else if (Utils.isNotEmptyOrNull(type))
				episode.setAttribute("program", type);
			if (Utils.isNotEmptyOrNull(air_date))
				episode.setAttribute("air_date", air_date);
			else if (Utils.isNotEmptyOrNull(subtype))
				episode.setAttribute("air_date", subtype);
			bodyChildren = ep.getChildNodes();
		}
		processDivAndAnnotation(bodyChildren, true);
	}

	// Mise à jour des attributs de l'élément Section
	// Lancement de la création des turns
	public void processDivAndAnnotation(NodeList elts, boolean realdivs) {
		/*
		 * il faut le diviser en bout de annotationBloc et en div
		 */
		for (int ptr = 0; ptr < elts.getLength(); ptr++) {
			Node nd = elts.item(ptr);
			// System.out.printf("%d %d %s%n", ptr, nd.getNodeType(), nd);
			if (!Utils.isElement(nd))
				continue;
			Element d = (Element) nd;
			if (d.getTagName().equals("div")) {
				// clore la section en cours si elle existe
				if (realdivs) {
					if (section != null) {
						addTurnsToSection(section, turns);
						section = null;
						turns = null;
					}
					// creer une nouvelle section
					section = trsDoc.createElement("Section");
					episode.appendChild(section);
					turns = new ArrayList<TranscriberTurn>();
					String startTime = timeSimplification(tf.getTimeValue(Utils.getDivHeadAttr(d, "start")));
					if (shiftNextStart && !startTime.isEmpty())
						startTime = Utils.printDouble(Double.parseDouble(startTime) + 0.001, 4);
					String endTime = timeSimplification(tf.getTimeValue(Utils.getDivHeadAttr(d, "end")));
					setAttr(section, "startTime", startTime, true);
					setAttr(section, "endTime", endTime, true);
					sectionStartSet = true;
					sectionEndSet = true;
					if (d.getAttribute("type") == "report" || d.getAttribute("type") == "nontrans"
							|| Utils.getDivHeadAttr(d, "type") == "filler") {
						section.setAttribute("type", Utils.getDivHeadAttr(d, "type"));
					} else {
						section.setAttribute("type", "report");
					}
					setAttr(section, "topic", Utils.getDivHeadAttr(d, "subtype"), false);
					NodeList dChilds = d.getChildNodes();
					processDivAndAnnotation(dChilds, false);
				} else {
					// this div is not at the first level so it cannot be
					// converted to a section element of transcriber
					// convert it to a note
					// this an annotatedU
					if (section == null) {
						section = trsDoc.createElement("Section");
						section.setAttribute("type", "report");
						sectionStartSet = false;
						sectionEndSet = false;
						episode.appendChild(section);
						turns = new ArrayList<TranscriberTurn>();
					}
					String startTime = timeSimplification(tf.getTimeValue(Utils.getDivHeadAttr(d, "start")));
					String endTime = timeSimplification(tf.getTimeValue(Utils.getDivHeadAttr(d, "end")));
					if (shiftNextStart && !startTime.isEmpty())
						startTime = Utils.printDouble(Double.parseDouble(startTime) + 0.001, 4);
					if (sectionStartSet == false) {
						setAttr(section, "startTime", startTime, true);
						sectionStartSet = true;
					}
					if (sectionEndSet == false)
						setAttr(section, "endTime", endTime, true);
					String typediv = Utils.getDivHeadAttr(d, "type");
					String subtypediv = Utils.getDivHeadAttr(d, "type");
					String tc = "";
					if (Utils.isNotEmptyOrNull(startTime))
						tc += "START: " + startTime + " ";
					if (Utils.isNotEmptyOrNull(endTime))
						tc += "END: " + endTime + " ";
					if (Utils.isNotEmptyOrNull(typediv))
						tc += "TYPE: " + typediv + " ";
					if (Utils.isNotEmptyOrNull(subtypediv))
						tc += "SUBTYPE: " + subtypediv + " ";
					TranscriberTurn tt = new TranscriberTurn(startTime, endTime, "subsection");
					tt.add(TranscriberTurn.Comment, tc);
					turns.add(tt);
				}
			} else if (d.getTagName().equals(Utils.ANNOTATIONBLOC)) {
				// this an annotatedU
				if (section == null) {
					section = trsDoc.createElement("Section");
					sectionStartSet = false;
					sectionEndSet = false;
					section.setAttribute("type", "report");
					episode.appendChild(section);
					turns = new ArrayList<TranscriberTurn>();
				}
				buildTurn(d);
			}
		}
		if (section != null)
			addTurnsToSection(section, turns);
		turns = null;
		section = null;
	}

	private void addTurnsToSection(Element sec, ArrayList<TranscriberTurn> turns) {
		if (turns.size() < 1)
			return;
		ArrayList<TranscriberTurn> overlaps = new ArrayList<TranscriberTurn>();
		overlaps.add(turns.get(0));
		int ioverlap = 0, iturns = 1;
		while (iturns < turns.size()) {
			TranscriberTurn tprev = overlaps.get(ioverlap), tnext = turns.get(iturns);
			if (time1InfTime2(tnext.startTime, tprev.endTime)) {
				// si le suivant commence avant la fin du précédent
				if (tprev.speaker.size() <= 1) {
					// le précédent n'était pas déjà un overlap
					// donc il faut insérer un champ who
					String spkprev = tprev.speakersToString();
					String spknext = tnext.speakersToString();
					if (spkprev.equals(spknext)) {
						// this is a real overlap but an error.
						// update a single speaker turn
						tprev.endTime = tnext.endTime;
						tprev.add(TranscriberTurn.Sync, tnext.startTime);
						tprev.copyFrom(tnext, 1);
					} else {
						TranscriberTurn over1 = new TranscriberTurn(tprev.startTime, tnext.endTime, spkprev);
						over1.add(TranscriberTurn.Sync, tprev.startTime);
						over1.add(TranscriberTurn.Who, cleanId(spkprev));
						over1.copyFrom(tprev, 1);
						over1.add(TranscriberTurn.Sync, tnext.startTime);
						over1.add(TranscriberTurn.Who, cleanId(spknext));
						over1.addSpeaker(spknext);
						over1.copyFrom(tnext, 1);
						overlaps.remove(ioverlap);
						overlaps.add(over1);
					}
				} else {
					// ajout du tnext
					tprev.endTime = tnext.endTime;
					tprev.add(TranscriberTurn.Sync, tnext.startTime);
					String spk = tnext.speakersToString();
					tprev.addSpeaker(spk);
					tprev.add(TranscriberTurn.Who, cleanId(spk));
					tprev.copyFrom(tnext, 1);
				}
				iturns++;
			} else {
				overlaps.add(tnext);
				ioverlap++;
				iturns++;
			}
		}
		// second pass to group the same speakers together
		int current = 0, next = 1;
		while (next < overlaps.size()) {
			TranscriberTurn tcurrent = overlaps.get(current), tnext = overlaps.get(next);
			String spkcurrent = tcurrent.speakersToString();
			String spknext = tnext.speakersToString();
			if (spkcurrent.equals(spknext)) {
				// si le suivant est ajouté au courant
				tcurrent.endTime = tnext.endTime;
				tcurrent.copyFrom(tnext, 0);
				overlaps.remove(next);
			} else {
				current++;
				next++;
			}
		}
		for (TranscriberTurn t : overlaps) {
			Element e = t.toElement(trsDoc);
			sec.appendChild(e);
		}
	}

	// Construction d'un élément turn pour la première fois
	// Les turns seront modifiés et compactés dans addTurnsToSectin
	public void buildTurn(Element elt) {
		String spk = cleanId(Utils.getAttrAnnotationBloc(elt, "who"));
		String startTime = timeSimplification(tf.getTimeValue(Utils.getAttrAnnotationBloc(elt, "start")));
		if (shiftNextStart && !startTime.isEmpty()) {
			// System.err.println(startTime);
			startTime = Utils.printDouble(Double.parseDouble(startTime) + 0.001, 4);
			shiftNextStart = false;
		}
		String endTime = timeSimplification(tf.getTimeValue(Utils.getAttrAnnotationBloc(elt, "end")));
		if (startTime.isEmpty() || endTime.isEmpty()) {
			if (oldEndTime.isEmpty())
				return;
			startTime = oldEndTime;
			if (shiftNextStart) {
				// System.err.println(startTime);
				startTime = Utils.printDouble(Double.parseDouble(startTime) + 0.001, 4);
				endTime = Utils.printDouble(Double.parseDouble(oldEndTime) + 0.001, 4);
				oldEndTime = endTime;
			} else {
				endTime = Utils.printDouble(Double.parseDouble(oldEndTime) + 0.001, 4);
				oldEndTime = endTime;
			}
			shiftNextStart = true;
		}
		if (startTime.equals(endTime)) {
			endTime = Utils.printDouble(Double.parseDouble(endTime) + 0.001, 4);
			oldEndTime = endTime;
			shiftNextStart = true;
		}
		if (sectionStartSet == false) {
			setAttr(section, "startTime", startTime, true);
			sectionStartSet = true;
		}
		if (sectionEndSet == false)
			setAttr(section, "endTime", endTime, true);
		TranscriberTurn tt = new TranscriberTurn(startTime, endTime, spk);
		turns.add(tt);
		setTurnAttributes(tt, elt);
		oldEndTime = endTime;
		NodeList uChildNodes = elt.getChildNodes();
		tt.add(TranscriberTurn.Sync, startTime);
		setTurn(tt, uChildNodes);
	}

	public String timeSimplification(String firstVal) {
		try {
			String[] valTimeSplit = firstVal.split("\\.");
			String startApresVirgule = valTimeSplit[1];
			if (startApresVirgule.length() > 3) {
				firstVal = valTimeSplit[0] + "." + valTimeSplit[1].substring(0, 3);
			}
		} catch (Exception e) {
		}
		return firstVal;
	}

	public boolean time1InfTime2(String time1, String time2) {
		try {
			return Float.parseFloat(time1) < Float.parseFloat(time2);
		} catch (Exception e) {
			return false;
		}
	}

	// Mise à jour des attributs de Turn
	public void setTurnAttributes(TranscriberTurn turn, Element annotU) {
		String e = Utils.getAttrAnnotationBloc(annotU, "mode");
		if (Utils.isNotEmptyOrNull(e) && (e.equals("spontaneous") || e.equals("planned"))) {
			turn.mode = e;
		} else {
			// Add comment
		}
		e = Utils.getAttrAnnotationBloc(annotU, "fidelity");
		if (Utils.isNotEmptyOrNull(e) && (e.equals("high") || e.equals("medium") || e.equals("low"))) {
			turn.fidelity = e;
		} else {
			// Add comment
		}
		e = Utils.getAttrAnnotationBloc(annotU, "channel");
		if (Utils.isNotEmptyOrNull(e) && (e.equals("telephone") || e.equals("studio"))) {
			turn.channel = e;
		} else {
			// Add comment
		}
	}

	// Mise à jour de l'attribut speaker de turn
	public void setSpkAttribute(Element turn, Element annotU) {
		String wh = cleanId(annotU.getAttribute("who"));
		String speaker = turn.getAttribute("speaker");
		if (Utils.isNotEmptyOrNull(speaker) && Utils.isNotEmptyOrNull(wh) && !speaker.contains(wh + " ")
				&& !speaker.endsWith(wh)) {
			turn.setAttribute("speaker", speaker + " " + wh);
			// System.out.println(" /// " + annotU.getAttribute("xml:id") + "
			// /// " + wh + " /// "+ speaker);
		} else {
			setAttr(turn, "speaker", wh, false);
		}
	}

	// Mise à jour de l'élément Turn: ajout des tiers en tant que commentaires
	public void setTurn(TranscriberTurn turn, NodeList uChildNodes) {
		for (int j = 0; j < uChildNodes.getLength(); j++) {
			if (Utils.isElement(uChildNodes.item(j))) {
				Element annotUChild = (Element) uChildNodes.item(j);
				String nodeName = annotUChild.getNodeName();
				// Traitement des noeuds qui peuvent être dans annotatedU: u
				// (contient noeuds g et incident), add, phon, morpho ou link
				if (nodeName.equals("u")) {
					// Traitement des noeuds contenus dans: g, incident, vocal
					// ou comment
					addU(turn, annotUChild);
				} else if (nodeName.equals("spanGrp")) {
					if (noComments)
						continue;
					NodeList spans = annotUChild.getElementsByTagName("span");
					for (int h = 0; h < spans.getLength(); h++) {
						Element span = (Element) spans.item(h);
						addComment(turn, annotUChild.getAttribute("type"), span.getTextContent());
					}
				}
			}
		}
	}

	/*
	 * //Ajout des éléments Who dans Turn public void addWhoElement(Element
	 * turn, int nb){ Element who = trsDoc.createElement("Who");
	 * who.setAttribute("nb", Integer.toString(nb)); turn.appendChild(who); }
	 */
	// Traitement de l'élément u provenant du fichier teiml:
	// les éléments seg deviennent des éléments texte, leur temps de début et de
	// fin correspondent aux éléments Sync dans Transcriber
	// Les éléments incident deviennent des éléments Event ou des Background
	// selon leur type
	// Les éléments comment restent des éléments comment, idem dans Transcriber
	// Les éléments vocal restent des éléments vocal, idem dans Transcriber
	public void addU(TranscriberTurn turn, Element u) {
		NodeList uChildNodes = u.getChildNodes();
		for (int t = 0; t < uChildNodes.getLength(); t++) {
			if (Utils.isElement(uChildNodes.item(t))) {
				Element uChild = (Element) uChildNodes.item(t);
				String uChildName = uChild.getNodeName();
				String uChildContent = uChild.getTextContent().replaceAll("\\s+", " ");
				if (uChildName.equals("seg")) {
					NodeList segChildNodes = uChild.getChildNodes();
					for (int d = 0; d < segChildNodes.getLength(); d++) {
						Node segChild = segChildNodes.item(d);
						if (Utils.isElement(segChild)) {
							Element segChildEl = (Element) segChild;
							String segChildElName = segChildEl.getNodeName();
							if (segChildElName.equals("pause")) {
								addPause(turn, segChildEl);
							} else if (segChildElName.equals("incident")) {
								addIncident(turn, segChildEl);
							} else if (segChildElName.equals("vocal")) {
								turn.add(TranscriberTurn.Vocal,
										segChildEl.getTextContent().replaceAll("\\s+", " ").trim());
							}
						} else if (segChild.getNodeName().equals("#text")) {
							turn.addText(segChild.getTextContent().replaceAll("\\s+", " "));
						}
					}
				} else if (uChildName.equals("anchor") && uChild.getNodeValue() != null) {
					// System.out.println(uChild.getNodeValue());
					if (!tf.getTimeValue(uChild.getAttribute("synch")).equals(turn.startTime)) {
						turn.add(TranscriberTurn.Sync,
								timeSimplification(tf.getTimeValue(uChild.getAttribute("synch"))));
					}
				} else if (uChildName.equals("comment")) {
					turn.add(TranscriberTurn.Comment, uChildContent);
				}
			}
		}
	}

	// Conversion d'un élément incident provenant du fichier teiml, devient un
	// élément Event ou Background
	public void addIncident(TranscriberTurn turn, Element incident) {
		// L'élément incident a pour attributs type et subtype, contient des
		// noeuds desc (type + textContent)
		String type = incident.getAttribute("type");
		if (type.equals("Event")) {
			createEventElement(turn, incident);
		} else if (type.equals("Background")) {
			createBackgroundElement(turn, incident);
		}
	}

	public void addPause(TranscriberTurn turn, Element pause) {
		if (pause.getAttribute("type").equals("verylong")) {
			turn.addText(" /// ");
		} else if (pause.getAttribute("type").equals("long")) {
			turn.addText(" ++ ");
		} else {
			turn.addText(" + ");
		}
	}

	// Ajout d'un commentaire correspondant à un tier dans teiml => syntaxe =
	// On utilise la syntaxe suivante: nomDuTier:\tDescriptionDuTier
	public void addComment(TranscriberTurn turn, String type, String commentContent) {
		turn.add(TranscriberTurn.Comment, type + ":\t" + commentContent);
	}

	// Création d'un élément Event et mise à jour de ses attributs à partir d'un
	// élément incident
	public void createEventElement(TranscriberTurn turn, Element incident) {
		String econtent = "";
		String etype = "";
		String eextent = "";
		Element event = trsDoc.createElement("Event");
		if (Utils.isNotEmptyOrNull(incident.getAttribute("subtype"))) {
			etype = incident.getAttribute("subtype");
		} else {
			etype = "noise";
		}
		NodeList descs = incident.getElementsByTagName("desc");
		for (int l = 0; l < descs.getLength(); l++) {
			Element desc = (Element) descs.item(l);
			String type = desc.getAttribute("type");
			String content = desc.getTextContent().replaceAll("\\s+", " ");
			if (type.equals("type") || type.equals("extent") || type.equals("desc")) {
				if (type.equals("type")) {
					if (content.equals("noise") || content.equals("lexical") || content.equals("pronounce")
							|| content.equals("language") || content.equals("entities")) {
						etype = content;
					} else {
						etype = "noise";
					}
				} else if (type.equals("extent")) {
					if (content.equals("begin") || content.equals("end") || content.equals("previous")
							|| content.equals("next")) {
						eextent = content;
					} else {
						eextent = "instantaneous";
					}
				} else {
					econtent = content;
				}
			} else {
				econtent = event.getAttribute("desc") + ", " + type + ": " + content;
			}
		}
		turn.addEvent(econtent, etype, eextent);
	}

	// Création d'un élément Background et mise à jour de ses attributs à partir
	// d'un élément incident
	public void createBackgroundElement(TranscriberTurn turn, Element incident) {
		String type = incident.getAttribute("subtype");
		if (type == null)
			type = "";
		String time = "";
		String level = "";
		NodeList descs = incident.getElementsByTagName("desc");
		for (int l = 0; l < descs.getLength(); l++) {
			Element desc = (Element) descs.item(l);
			String what = desc.getAttribute("type");
			if (what.equals("type"))
				type = desc.getTextContent().replaceAll("\\s+", " ");
			if (what.equals("time"))
				time = desc.getTextContent().replaceAll("\\s+", " ");
			if (what.equals("level"))
				level = desc.getTextContent().replaceAll("\\s+", " ");
		}
		turn.addBackground(time, type, level);
	}

	// Mise à jour de l'attribut d'un élément: si la valeur de l'attribut est
	// nulle, l'attribut n'est pas ajouté.
	public static void setAttr(Element el, String attName, String attValue, boolean required) {
		if (Utils.isNotEmptyOrNull(attValue)) {
			el.setAttribute(attName, attValue);
		} else {
			if (required) {
				el.setAttribute(attName, "");
			}
		}
	}

	// Conversion de la valeur sex selon les conventions de Transcriber, qui ne
	// prend que les valeur male, female ou unknown (dans l'attribut de type de
	// speaker)
	public static String convertSex(String sex) {
		if (sex.equals("1")) {
			return "male";
		} else if (sex.equals("2")) {
			return "female";
		} else {
			return "unknown";
		}
	}

	// Création du fichier de sortie
	public void createOutput() {
		Source source = new DOMSource(trsDoc);
		Result resultat = new StreamResult(outputName);

		try {
			TransformerFactory fabrique2 = TransformerFactory.newInstance();
			Transformer transformer = fabrique2.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "trans-14.dtd");
			transformer.transform(source, resultat);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * //Conversion date au format chat (JJ-MMM-AAAA)-> date au formt
	 * transcriber (JJMMAA) public static String chatDateToTrsDate(String date){
	 * try{ String [] months = new
	 * DateFormatSymbols(Locale.FRANCE).getShortMonths(); String patternStr =
	 * "(\\d\\d)-(.*)-(\\d\\d\\d\\d)"; Pattern pattern =
	 * Pattern.compile(patternStr); Matcher matcher = pattern.matcher(date); if
	 * (matcher.find()) { date = matcher.group(1) + "" +
	 * months[Integer.parseInt(matcher.group(2))-1].toUpperCase() + "" +
	 * convertYear(matcher.group(3)); } else{ System.out.println(); } }
	 * catch(Exception e){ return date; } return date; }
	 */

	// Programme principal
	public static void main(String args[]) throws IOException {
		Utils.printVersionMessage();

		String usageString = "Description: TeiToTranscriber convertit un fichier au format Tei en un fichier au format Transcriber.%nUsage: TeiToTranscriber [-options] <file"
				+ Utils.EXT + ">%n";
		TierParams options = new TierParams();
		// Parcours des arguments
		if (!Utils.processArgs(args, options, usageString, Utils.EXT, EXT))
			System.exit(1);
		String input = options.input;
		String output = options.output;

		File f = new File(input);
		// Permet d'avoir le nom complet du fichier (chemin absolu, sans signes
		// spéciaux(. et .. par ex))
		input = f.getCanonicalPath();

		if (f.isDirectory()) {
			File[] teiFiles = f.listFiles();

			String outputDir = "";
			if (output == null) {
				if (input.endsWith("/")) {
					outputDir = input.substring(0, input.length() - 1);
				} else {
					outputDir = input + "/";
				}
			} else {
				outputDir = output;
				if (!outputDir.endsWith("/")) {
					outputDir = output + "/";
				}
			}

			File outFile = new File(outputDir);
			if (outFile.exists()) {
				if (!outFile.isDirectory()) {
					System.out.println("\n Erreur :" + output
							+ " est un fichier, vous devez spécifier un nom de dossier pour le stockage des résultats. \n");
					System.exit(1);
				}
			}

			new File(outputDir).mkdir();

			for (File file : teiFiles) {
				String name = file.getName();
				if (file.isFile() && (name.endsWith(Utils.EXT))) {
					String outputFileName = file.getName().split("\\.")[0] + Utils.EXT_PUBLISH + EXT;
					TeiToTranscriber ttt = new TeiToTranscriber(file.getAbsolutePath(), outputDir + outputFileName,
							options);
					System.out.println(outputDir + outputFileName);
					ttt.createOutput();
				} else if (file.isDirectory()) {
					args[0] = "-i";
					args[1] = file.getAbsolutePath();
					main(args);
				}
			}
		}

		else {
			if (output == null) {
				output = input.split("\\.")[0] + Utils.EXT_PUBLISH + EXT;
			} else if (new File(output).isDirectory()) {
				if (output.endsWith("/")) {
					output = output + input.split("\\.")[0] + Utils.EXT_PUBLISH + EXT;
				} else {
					output = output + "/" + input.split("\\.")[0] + Utils.EXT_PUBLISH + EXT;
				}
			}

			if (!(Utils.validFileFormat(input, Utils.EXT))) {
				System.err.println("Le fichier d'entrée du programme doit avoir l'extension " + Utils.EXT);
			}
			TeiToTranscriber ttt = new TeiToTranscriber(new File(input).getAbsolutePath(), output, options);
			System.out.println("Reading " + input);
			ttt.createOutput();
			System.out.println("New file created " + output);
		}
	}
}
