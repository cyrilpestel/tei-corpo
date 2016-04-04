/**
 * @author Myriam Majdoub
 * TeiToTranscriber: convertit un fichier teiml en un fichier au format Transcriber, conform�ment � la dtd trans-14.dtd.
 */

package fr.ortolang.teicorpo;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;

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
	//Document transcriber
	Document trsDoc;
	//Element Trans: racine du document Transcriber
	Element trans;
	//Element Episode: contient la transcription de l'enregistrement
	Element episode;

	//Extension du fichier de sortie: .trs
	final static String EXT = ".trs";

	public TeiToTranscriber(String inputName, String outputName) {
		super(inputName, outputName);

	}

	//Création du document trs
	public void outputWriter(){
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

	//Ajout des informations relatives à l'enregistrement et à la situation d'énonciation
	public void buildHeader(){
		setTransAttributes();
		buildSpks();
		buildTopics();
	}

	//Mise à jour des attributs de l'élément Trans
	public void setTransAttributes(){
		//Attributs trans : audio_filename, scribe, xml:lang, version, version_date, elapsed_time
		setAttr(trans, "audio_filename", tf.transInfo.medianame, false);
		setAttr(trans, "scribe", tf.transInfo.transcriber, false);
		setAttr(trans, "xml:lang", tf.language, false);
		setAttr(trans, "version", tf.transInfo.version, false);
		setAttr(trans, "version_date", tf.transInfo.date, false);
		setAttr(trans, "elapsed_time", tf.transInfo.timeDuration, false);		
	}

	//Construction de l'élément Speakers
	public void buildSpks(){
		if(!tf.transInfo.participants.isEmpty()){
			Element spks = trsDoc.createElement("Speakers");
			trans.appendChild(spks);
			for(Participant p : tf.transInfo.participants){
				addSpk(spks, p);
			}
		}
	}

	//Ajout d'un speaker
	public void addSpk(Element spks, Participant p){
		//Attributs speaker: id, name, check, type, dialect, accent, scope		
		//Problème: transcriber ne prend que les informations citées ci-dessus, pas de possibilité de commentaires dans
		//un élément Speaker, donc si autres infos, sont perdues ...
		Element spk = trsDoc.createElement("Speaker");
		//Les attributs id et name sont obligatoires
		setAttr(spk, "id", p.id, true);
		if(Utils.isNotEmptyOrNull(p.name)){
			setAttr(spk, "name", p.name, true);
		}
		else{
			setAttr(spk, "name", p.id, true);
		}
		
		//Attribut check : 2 valeurs possibles "yes" ou "no"
		String val = p.adds.get("check");
		if (Utils.isNotEmptyOrNull(val)) {
			if (val.equals("yes"))
				setAttr(spk, "check", "yes", false);
			else if (val.equals("no"))
				setAttr(spk, "check", "no", false);
		}

		//Si rôle = child ou children, va dans l'attribut type
		if(p.role.equals("child") || p.role.equals("children")){
			setAttr(spk, "type", "child", false);
		}
		
		//Attribut dialect: liste prédéfinies de valeurs: native | nonnative
		val = p.adds.get("dialect");
		if (Utils.isNotEmptyOrNull(val)) {
			if (val.equals("native"))
				setAttr(spk, "dialect", "native", false);
			else if (val.equals("nonative"))
				setAttr(spk, "dialect", "nonative", false);
		}

		//Attribut sex de TEI va dans role, liste prédéfinie de valeurs
		if(Utils.isNotEmptyOrNull(p.sex)){
			setAttr(spk,"type", convertSex(p.sex), false);
		}
		setAttr(spk, "accent", p.adds.get("accent"), false);

		//Attribut scope: valeurs possibles : "local" ou "global"
		val = p.adds.get("scope");
		if (Utils.isNotEmptyOrNull(val)) {
			if (val.equals("local"))
				setAttr(spk, "scope", "local", false);
			else if (val.equals("global"))
				setAttr(spk, "scope", "global", false);
		}
		spks.appendChild(spk);
	}

	//Construction de l'élément Topics
	public void buildTopics(){
		if(!tf.transInfo.situations.isEmpty()){
			Element topics = trsDoc.createElement("Topics");
			trans.appendChild(topics);
			Element settingDesc = (Element) tf.teiDoc.getElementsByTagName("settingDesc").item(0);
			NodeList settings = settingDesc.getElementsByTagName("setting");
			for(int i = 0; i<settings.getLength(); i++){
				Element setting = (Element)settings.item(i);
				addTopic(topics, setting);
			}
		}
	}

	//Ajout d'un élément Topic
	public void addTopic(Element topics, Element setting){
		//Attributs topic : "id", "desc"
		Element topic = trsDoc.createElement("Topic");
		setAttr(topic, "id", setting.getAttribute("xml:id"), true);
		setAttr(topic, "desc", setting.getTextContent().trim(), true);
		topics.appendChild(topic);
	}

	//Construction de l'élément Episode (contient la transcription)
	public void buildText(){
		//Mise à jour des attributs de l'élément Episode
		setEpisodeAttr();
		//Sections:attributs
		//type, topic, startTime, endTime
		NodeList divs = tf.teiDoc.getElementsByTagName("div");
		for (int i = 0; i<divs.getLength(); i++){
			Element d = (Element)divs.item(i);
			Element section = trsDoc.createElement("Section");
			setSectionAttr(d, section);
			episode.appendChild(section);
			buildSection(d, section);
		}
		//Turns: speaker, startTime, endTime, mode, fidelity, channel. Contient texte + sync (attribut time (secondes))
	}

	//Mise à jour des attributs de l'élément Episode
	public void setEpisodeAttr(){
		//Episode : attributs "program" et "air_date"
		episode = trsDoc.createElement("Episode");
		for(String info : tf.trans.infos){
			String infoType = Utils.getInfoType(info);
			String infoContent = Utils.getInfo(info);
			if(info.startsWith("program") || info.startsWith("air_date")){
				episode.setAttribute(infoType, infoContent);
			}
		}
		trans.appendChild(episode);
	}

	//Mise à jour des attributs de l'élément Section
	public void setSectionAttr(Element d, Element section){
		String startTime = timeSimplification(tf.getTimeValue(d.getAttribute("start")));
		String endTime = timeSimplification(tf.getTimeValue(d.getAttribute("end")));
		setAttr(section, "startTime", startTime, true);
		setAttr(section, "endTime", endTime, true);
		if(d.getAttribute("type")=="report" || d.getAttribute("type")=="nontrans" || d.getAttribute("type")=="filler"){
			section.setAttribute("type", d.getAttribute("type"));
		}
		else{
			section.setAttribute("type", "report");
		}
		setAttr(section, "topic", d.getAttribute("subtype"), false);
	}

	//Construction d'un élément section
	public void buildSection(Element d, Element section){
		NodeList annotUtterances = d.getElementsByTagName(Utils.ANNOTATIONBLOC);
		String currentSpk = "";
		Element firstAu = (Element)d.getElementsByTagName(Utils.ANNOTATIONBLOC).item(0);
		if(d.getElementsByTagName(Utils.ANNOTATIONBLOC).getLength()>0){ currentSpk = firstAu.getAttribute("who");}
		Element turn = trsDoc.createElement("Turn");
		//Parcours utterance
		for(int i = 0; i<annotUtterances.getLength(); i++){
			Element annotU = (Element) annotUtterances.item(i);
			//Element link = getLinkElement(annotU);
			String spk = ((Element)annotUtterances.item(i)).getAttribute("who");
			//if (!spk.equals(currentSpk)){
			turn = trsDoc.createElement("Turn");
			//currentSpk = spk;
			//}
			setTurnAttributes(turn, annotU);
			turn.setAttribute("speaker", annotU.getAttribute("who"));
			NodeList uChildNodes = annotU.getChildNodes();
			boolean syncAnnots = false;
			if(i<d.getElementsByTagName(Utils.ANNOTATIONBLOC).getLength()-1){
				syncAnnots = syncAnnots(annotU,(Element)d.getElementsByTagName(Utils.ANNOTATIONBLOC).item(i+1));
			}
			//Traitement des links
			/*if(link != null){
				turn = trsDoc.createElement("Turn");
				setTurnAttributes(turn, annotU);
				int nbAtt = 1;
				setSpkAttribute(turn, annotU);
				addWhoElement(turn, nbAtt);
				setTurn(turn, uChildNodes);				
				int nbLinks = link.getAttribute("target").split(" ").length;
				for(int lk = 0; lk<nbLinks; lk++){
					if(nbAtt>turn.getAttribute("speaker").split(" ").length){
						nbAtt = 1;
					}
					else{
						nbAtt++;
					}
					i++;
					annotU = (Element)annotUtterances.item(i);
					addWhoElement(turn, nbAtt);
					setTurn(turn, annotU.getChildNodes());
					setSpkAttribute(turn, annotU);
				}
			}else*/
			if(syncAnnots){
				//System.out.println(annotU.getTextContent());
				turn = trsDoc.createElement("Turn");
				setTurnAttributes(turn, annotU);
				Element sync = trsDoc.createElement("Sync");
				int nbAtt = 1;
				setSpkAttribute(turn, annotU);
				addWhoElement(turn, nbAtt);
				setTurn(turn, uChildNodes);

				while(syncAnnots){
					i++;
					annotU = (Element) annotUtterances.item(i);
					setSpkAttribute(turn, annotU);
					sync = trsDoc.createElement("Sync");
					sync.setAttribute("time", timeSimplification(tf.getTimeValue(annotU.getAttribute("start"))));
					turn.appendChild(sync);
					if(nbAtt>turn.getAttribute("speaker").split(" ").length){
						nbAtt = 1;
					}
					else{
						nbAtt++;
					}
					addWhoElement(turn, nbAtt);
					setTurn(turn, annotU.getChildNodes());
					syncAnnots = syncAnnots(annotU,(Element)d.getElementsByTagName(Utils.ANNOTATIONBLOC).item(i+1));						
				}
				///*****///
				//System.out.println(turn.getAttribute("startTime") + " - " + turn.getAttribute("endTime") + " -> " + turn.getTextContent());
				turn.setAttribute("endTime", timeSimplification(tf.getTimeValue(annotU.getAttribute("end"))));
				///*****///
			}
			else{
				setTurn(turn, uChildNodes);
				//Traitement des commentaires supplémentaires:
				//=tiers
				Element au = (Element) annotUtterances.item(i);
				NodeList coms = au.getElementsByTagName("span");
				for(int y = 0; y<coms.getLength(); y++){
					Element com = (Element)coms.item(y);
					String name = com.getAttribute("type");
					String value =com.getTextContent();
					Element comment = trsDoc.createElement("Comment");
					comment.setAttribute("desc", "@" + name + "\t" + value);
					turn.appendChild(comment);
				}
			}
			section.appendChild(turn);
		}
	}
	
	public String timeSimplification (String firstVal){
		try{
			String [] valTimeSplit = firstVal.split("\\.");
			String startApresVirgule = valTimeSplit[1];
			if(startApresVirgule.length()>3){
				firstVal = valTimeSplit[0] + "." + valTimeSplit[1].substring(0,3);
			}
		}
		catch(Exception e){}
		return firstVal;
	}

	public boolean syncAnnots(Element annot1, Element annot2){
		try{		
			return Float.parseFloat(tf.getTimeValue(annot2.getAttribute("start"))) < Float.parseFloat(tf.getTimeValue(annot1.getAttribute("end")));
		}
		catch(Exception e){
			return false;
		}		
	}

	//Mise à jour des attributs de Turn 
	public void setTurnAttributes(Element turn, Element annotU){
		//System.out.println(tf.getTimeValue(annotU.getAttribute("start")) + " :::: " + annotU.getAttribute("xml:id") + "  -->  " + annotU.getTextContent());
		String start = timeSimplification(tf.getTimeValue(annotU.getAttribute("start")));
		String end = timeSimplification(tf.getTimeValue(annotU.getAttribute("end")));
		setAttr(turn, "startTime", start, true);
		setAttr(turn, "endTime", end, true);
		Element sync = trsDoc.createElement("Sync");
		sync.setAttribute("time", start);
		turn.appendChild(sync);
		if(annotU.getAttribute("mode")=="spontaneous" ||  annotU.getAttribute("mode")=="planned"){
			setAttr(turn, "mode", annotU.getAttribute("mode"), false);
		}
		else{
			//Add comment
		}
		if(annotU.getAttribute("fidelity")=="high" ||  annotU.getAttribute("fidelity")=="medium" || annotU.getAttribute("fidelity")=="low"){
			setAttr(turn, "fidelity", annotU.getAttribute("fidelity"), false);
		}
		else{
			//Add comment
		}
		if(annotU.getAttribute("channel")=="telephone" ||  annotU.getAttribute("channel")=="studio"){
			setAttr(turn, "channel", annotU.getAttribute("channel"), false);
		}
		else{
			//Add comment
		}
	}

	//Mise à jour de l'attribut speaker de turn
	public void setSpkAttribute(Element turn, Element annotU){
		String wh = annotU.getAttribute("who");
		String speaker = turn.getAttribute("speaker");
		if(Utils.isNotEmptyOrNull(speaker) && Utils.isNotEmptyOrNull(wh) && !speaker.contains(wh+" ") && !speaker.endsWith(wh)){
			turn.setAttribute("speaker", speaker + " " + wh);
			//System.out.println(" /// " + annotU.getAttribute("xml:id") + " /// " + wh + " /// "+ speaker);
		}
		else{
			setAttr(turn, "speaker", wh, false);
		}
	}

	//Mise à jour de l'élément Turn: ajout des tiers en tant que commentaires
	public void setTurn(Element turn, NodeList uChildNodes){
		for(int j = 0; j<uChildNodes.getLength(); j++){
			if(Utils.isElement(uChildNodes.item(j))){
				Element annotUChild = (Element)uChildNodes.item(j);
				String nodeName = annotUChild.getNodeName();
				//Traitement des noeuds qui peuvent être dans annotatedU: u (contient noeuds g et incident), add, phon, morpho ou link
				if(nodeName.equals("u")){
					//Traitement des noeuds contenus dans: g, incident, vocal ou comment
					addU(turn, annotUChild);
				}
				/*else if (nodeName.equals("spanGrp")){
					NodeList spans = annotUChild.getElementsByTagName("span");
					for (int h = 0; h<spans.getLength(); h++){
						Element span = (Element) spans.item(h);
						addComment(turn, annotUChild.getAttribute("type"), span.getTextContent());
					}
				}*/
			}
		}
	}

	//Ajout des éléments Who dans Turn
	public void addWhoElement(Element turn, int nb){
		Element who = trsDoc.createElement("Who");
		who.setAttribute("nb", Integer.toString(nb));
		turn.appendChild(who);
	}

	//Traitement de l'élément u provenant du fichier teiml:
	//les éléments seg deviennent des éléments texte, leur temps de début et de fin correspondent aux éléments Sync dans Transcriber
	//Les éléments incident deviennent des éléments Event ou des Background selon leur type
	//Les éléments comment restent des éléments comment, idem dans Transcriber
	//Les éléments vocal restent des éléments vocal, idem dans Transcriber
	public void addU(Element turn, Element u){
		NodeList uChildNodes = u.getChildNodes();
		for(int t=0; t<uChildNodes.getLength(); t++){
			if(Utils.isElement(uChildNodes.item(t))){
				Element uChild = (Element)uChildNodes.item(t);
				String uChildName = uChild.getNodeName();
				String uChildContent = uChild.getTextContent();
				if(uChildName.equals("seg")){					
					NodeList segChildNodes = uChild.getChildNodes();
					for (int d = 0 ; d<segChildNodes.getLength(); d++){
						Node segChild = segChildNodes.item(d);
						if (Utils.isElement(segChild)){
							Element segChildEl = (Element)segChild;
							String segChildElName = segChildEl.getNodeName();
							if(segChildElName.equals("pause")){
								addPause(turn, segChildEl); 
							}
							else if(segChildElName.equals("incident")){
								addIncident(turn, segChildEl);
							}
							else if(segChildElName.equals("vocal")){
								Element vocal = trsDoc.createElement("Vocal");
								vocal.setAttribute("desc", segChildEl.getTextContent().trim());
								turn.appendChild(vocal);
							}
						}
						else if (segChild.getNodeName().equals("#text")){
							turn.appendChild(trsDoc.createTextNode(segChild.getTextContent()));
						}
					}
				}
				else if(uChildName.equals("anchor") && uChild.getNodeValue() != null){
					//System.out.println(uChild.getNodeValue());
					if(!tf.getTimeValue(uChild.getAttribute("synch")).equals(turn.getAttribute("startTime"))){
						Element sync = trsDoc.createElement("Sync");
						turn.appendChild(sync);
						sync.setAttribute("time", timeSimplification(tf.getTimeValue(uChild.getAttribute("synch"))));
					}
				}
				else if(uChildName.equals("comment")){
					Element comment = trsDoc.createElement("comment");
					comment.setAttribute("desc", uChildContent);
					turn.appendChild(comment);
				}
			}
		}
	}

	//Conversion d'un élément incident provenant du fichier teiml, devient un élément Event ou Background
	public void addIncident(Element turn, Element incident){
		//L'élément incident a pour attributs type et subtype, contient des noeuds desc (type + textContent)
		String type = incident.getAttribute("type");
		if(type.equals("Event")){
			Element event = createEventElement(incident);
			turn.appendChild(event);
		}
		else if(type.equals("Background")){
			Element background = createBackgroundElement(incident);
			turn.appendChild(background);
		}
	}

	public void addPause(Element turn, Element pause){
		if(pause.getAttribute("type").equals("verylong")){
			turn.appendChild(trsDoc.createTextNode(" /// "));
		}
		else if(pause.getAttribute("type").equals("long")){
			turn.appendChild(trsDoc.createTextNode(" ++ "));
		}
		else{
			turn.appendChild(trsDoc.createTextNode(" + "));
		}
	}

	//Ajout d'un commentaire correspondant à un tier dans teiml => syntaxe =
	//On utilise la syntaxe suivante: nomDuTier:\tDescriptionDuTier
	public void addComment(Element turn, String type, String commentContent){
		Element comment = trsDoc.createElement("Comment");
		comment.setAttribute("desc", type + ":\t" + commentContent);
		turn.appendChild(comment);
	}

	//Création d'un élément Event et mise à jour de ses attributs à partir d'un élément incident
	public Element createEventElement(Element incident){
		Element event = trsDoc.createElement("Event");
		if(Utils.isNotEmptyOrNull(incident.getAttribute("subtype"))){
			setAttr(event, "type",incident.getAttribute("subtype"), false);
		}
		else{
			event.setAttribute("type", "noise");
		}		
		NodeList descs = incident.getElementsByTagName("desc");
		for(int l=0; l<descs.getLength(); l++){
			Element desc = (Element)descs.item(l);
			String type = desc.getAttribute("type");
			String content = desc.getTextContent();
			if(type.equals("type") || type.equals("extent") || type.equals("desc")){
				if(type.equals("type")){
					if(content.equals("noise") || content.equals("lexical") || content.equals("pronounce") || content.equals("language") || content.equals("entities")){
						event.setAttribute("type", content);
					}
					else{
						event.setAttribute("type", "noise");
					}
				}
				else if(type.equals("extent")){
					if(content.equals("begin") || content.equals("end") || content.equals("previous") || content.equals("next")){
						event.setAttribute("extent", content);
					}
					else{
						event.setAttribute("extent", "instantaneous");
					}
				}
				else{
					event.setAttribute("desc", content);
				}
			}
			else{
				event.setAttribute("desc", event.getAttribute("desc") + ", " + type + ": " + content);
			}
		}
		return event;
	}

	//Création d'un élément Background et mise à jour de ses attributs à partir d'un élément incident
	public Element createBackgroundElement(Element incident){
		Element background = trsDoc.createElement("Background");
		setAttr(background, "type", incident.getAttribute("subtype"), false);
		NodeList descs = incident.getElementsByTagName("desc");
		for(int l=0; l<descs.getLength(); l++){
			Element desc = (Element)descs.item(l);
			String type = desc.getAttribute("type");
			if(type.equals("type") || type.equals("time") || type.equals("level")){
				setAttr(background, type, desc.getTextContent(), true);
			}
		}
		return background;
	}

	//Renvoie l'élément link contenu dans l'élément u si il en a un, renvoie null sinon
	public Element getLinkElement(Element annotU){
		Element link = null;
		//Il ne peut y avoir qu'un élément link par utterance
		NodeList links = annotU.getElementsByTagName("link");
		if(links.getLength() != 0){
			link = (Element) links.item(0);
		}
		return link;
	}

	//Mise à jour de l'attribut d'un élément: si la valeur de l'attribut est nulle, l'attribut n'est pas ajouté.
	public static void setAttr(Element el, String attName, String attValue, boolean required){
		if(Utils.isNotEmptyOrNull(attValue)){
			el.setAttribute(attName, attValue);
		}
		else{
			if(required){
				el.setAttribute(attName, "");
			}
		}
	}

	//Conversion de la valeur sex selon les conventions de Transcriber, qui ne prend que les valeur male, female ou unknown (dans l'attribut de type de speaker)
	public static String convertSex(String sex){
		if(sex.equals("1")){
			return "male";
		}
		else if(sex.equals("2")){
			return "female";
		}
		else{
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
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	/*//Conversion date au format chat (JJ-MMM-AAAA)-> date au formt transcriber (JJMMAA)
	public static String chatDateToTrsDate(String date){
		try{
			String [] months = new DateFormatSymbols(Locale.FRANCE).getShortMonths();
			String patternStr = "(\\d\\d)-(.*)-(\\d\\d\\d\\d)";
			Pattern pattern = Pattern.compile(patternStr);
			Matcher matcher = pattern.matcher(date);
			if (matcher.find()) {
				date = matcher.group(1) + "" + months[Integer.parseInt(matcher.group(2))-1].toUpperCase() + "" + convertYear(matcher.group(3));
			}
			else{
				System.out.println();
			}
		}
		catch(Exception e){
			return date;
		}
		return date;
	}*/

	//Usages du programme
	public static void usage() {
		System.err.println("Description: TeiToTranscriber convertit un fichier au format Tei en un fichier au format Transcriber");
		System.err.println("Usage: TeiToTranscriber [-options] <file"+ Utils.EXT +">");
		System.err.println("	     :-i nom du fichier ou repertoire où se trouvent les fichiers Tei à convertir (les fichiers ont pour extension " + Utils.EXT);
		System.err.println("	     :-o nom du fichier de sortie au format Transcriber (.trs ou .xml.trs) ou du repertoire de résultats");
		System.err.println("	     	si cette option n'est pas spécifié, le fichier de sortie aura le même nom que le fichier d'entrée avec l'extension .trs;");
		System.err.println("	     	si on donne un repertoire comme input et que cette option n'est pas spécifiée, les résultats seront stockés dans le même dossier que l'entrée.\"");
		System.err.println("	     :-usage ou -help = affichage ce message");
		System.exit(1);
	}	

	//Programme principal


	public static void main(String args[]) throws IOException {

		String input = null;
		String output = null;
		//Parcours des arguments

		if (args.length == 0) {
			System.err.println("Vous n'avez spécifié aucun argument\n");
			usage();
		} else {
			for (int i = 0; i < args.length; i++) {
				try {
					if (args[i].equals("-i")) {
						i++;
						input = args[i];
					} else if (args[i].equals("-o")) {
						i++;
						output = args[i];
					} else {
						usage();
					}
				} catch (Exception e) {
					usage();
				}
			}
		}

		File f = new File(input);

		//Permet d'avoir le nom complet du fichier (chemin absolu, sans signes spéciaux(. et .. par ex))
		input = f.getCanonicalPath();

		if (f.isDirectory()){
			File[] teiFiles = f.listFiles();

			String outputDir = "";
			if (output == null){
				if(input.endsWith("/")){
					outputDir = input.substring(0, input.length()-1);
				}
				else{
					outputDir = input + "/";
				}
			}
			else{
				outputDir = output;
				if(!outputDir.endsWith("/")){
					outputDir = output + "/";
				}
			}

			File outFile = new File(outputDir);
			if(outFile.exists()){
				if(!outFile.isDirectory()){
					System.out.println("\n Erreur :"+ output + " est un fichier, vous devez spécifier un nom de dossier pour le stockage des résultats. \n");
					usage();
					System.exit(1);
				}
			}

			new File(outputDir).mkdir();

			for (File file : teiFiles){
				String name = file.getName();
				if (file.isFile() && (name.endsWith(Utils.EXT))){
					String outputFileName = file.getName().split("\\.")[0] + Utils.EXT_PUBLISH + EXT;
					TeiToTranscriber ttt = new TeiToTranscriber(file.getAbsolutePath(), outputDir+outputFileName);
					System.out.println(outputDir+outputFileName);
					ttt.createOutput();
				}
				else if(file.isDirectory()){
					args[0] = "-i";
					args[1] = file.getAbsolutePath();
					main(args);
				}
			}
		}

		else{
			if (output == null) {
				output = input.split("\\.")[0] + Utils.EXT_PUBLISH + EXT;
			}
			else if(new File(output).isDirectory()){
				if(output.endsWith("/")){
					output = output + input.split("\\.")[0] + Utils.EXT_PUBLISH + EXT;
				}
				else{
					output = output + "/"+ input.split("\\.")[0] + Utils.EXT_PUBLISH + EXT;
				}
			}

			if (!(Utils.validFileFormat(input, Utils.EXT))) {
				System.err.println("Le fichier d'entrée du programme doit avoir l'extension " + Utils.EXT);
				usage();
			}
			TeiToTranscriber ttt = new TeiToTranscriber(new File(input).getAbsolutePath(), output);
			System.out.println("Reading " + input);
			ttt.createOutput();
			System.out.println("New file created " + output);
		}
	}
}
