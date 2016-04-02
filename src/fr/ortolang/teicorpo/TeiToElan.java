package fr.ortolang.teicorpo;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import fr.ortolang.teicorpo.TeiFile.AnnotatedUtterance;
import fr.ortolang.teicorpo.TeiFile.Div;

public class TeiToElan {

	TeiToPartition ttp = null;
	
	//Extension du fichier de sortie: .eaf
	final static String EXT = ".eaf";

	//Nom du fichier teiml à convertir
	String inputName;
	//Nom du fichier de sortie
	String outputName;

	//Document elan
	Document elanDoc;
	//Element ANNOTATION_DOCUMENT: racine du document Elan)
	Element annot_doc;
	
	// maps for the cv
	Map<String, HashMap<String, String>> cvs;

	//Document TEI à lire
	public Document teiDoc;
	// acces Xpath
	public XPathFactory xPathfactory;
	public XPath xpath;

	//Validation du document Tei par la dtd
	boolean validation = false;

	//Constructeur à partir du nom du fichier TEI et du nom du fichier de sortie au format Elan
	public TeiToElan(String inputName, String outputName) {
		DocumentBuilderFactory factory = null;
		try{
			File teiFile = new File(inputName);
			factory = DocumentBuilderFactory.newInstance();
			Utils.setDTDvalidation(factory, validation);
			DocumentBuilder builder = factory.newDocumentBuilder();
			teiDoc = builder.parse(teiFile);
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
			ttp = new TeiToPartition(xpath, teiDoc);
		}catch(Exception e){
			e.printStackTrace();
		}
		this.inputName = inputName;
		this.outputName = outputName;
		outputWriter();
		conversion();
	}

	//Ecriture du fichier de sortie
	public void outputWriter() {
		elanDoc = null;
		DocumentBuilderFactory factory = null;
		try {
			factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			elanDoc = builder.newDocument();
			annot_doc = elanDoc.createElement("ANNOTATION_DOCUMENT");
			elanDoc.appendChild(annot_doc);
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	//Conversion du fichier TEI vers Elan
	public void conversion(){
		//Construction de l'en-tête
		buildHeader();
		//Construction des vocabulaires contrôlés
		List<Element> cvList = buildControlledVocabularies();
		//Construction des tiers
		buildTiers();
		//Construction des types linguistiques
		buildLgqTypes();
		//Construction des contraintes Elan
		buildConstraints();
		//Addition des vocabulaires contrôlés
		for (Element e: cvList)
			annot_doc.appendChild(e);
	}
	   
    /**
     * Get a date of the form "2014-03-05T15:06:29+01:00".
     * @return
     */
    private static String getDate() {
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        final Calendar calendar = Calendar.getInstance();
		String dateString = dateFmt.format(calendar.getTime());

		return addTimezone(calendar, dateString);
    }
    
    private static String addTimezone(Calendar calendar, String dateString) {
        int GMTOffsetInMinutes = calendar.getTimeZone().getRawOffset() / (60 * 1000);

        char sign = '+';

        if (GMTOffsetInMinutes < 0) {
            sign = '-';
            GMTOffsetInMinutes = -GMTOffsetInMinutes;
        }

        final int hours = GMTOffsetInMinutes / 60;
		final int minutes = GMTOffsetInMinutes % 60;

		String strOffset = String.format("%02d:%02d", hours, minutes);
        
        return dateString + sign + strOffset;
    }

	//Information contenues dans l'élément annotation_doc et dans l'élément header
	public void buildHeader(){
		annot_doc.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		annot_doc.setAttribute("xsi:noNamespaceSchemaLocation", "http://www.mpi.nl/tools/elan/EAFv2.8.xsd");
		Element header = elanDoc.createElement("HEADER");
		//date
		String date = "";
		try {
			date = (String)this.xpath.compile("//appInfo/date/text()").evaluate(this.teiDoc, XPathConstants.STRING);
		} catch(Exception e) {
			date = "";
		}
		if (date.isEmpty()) {
			date = getDate();
		}
		annot_doc.setAttribute("DATE", date);
		annot_doc.setAttribute("AUTHOR", "TEI_CORPO Converter");
		annot_doc.setAttribute("FORMAT", "2.8");
		annot_doc.setAttribute("VERSION", "2.8");
		annot_doc.appendChild(header);
		buildHeaderElement(header);
		//Element timeleine
		Element time_order = elanDoc.createElement("TIME_ORDER");
		annot_doc.appendChild(time_order);
		buildTimeline(time_order);
	}

	//Complétion des informations du header
	void buildHeaderElement(Element header){
		try {
			//Attribut de ANNOTATION_DOCUMENT
			Element teiHeader = (Element) this.teiDoc.getElementsByTagName("teiHeader").item(0);
	/*
			Element app = (Element)teiHeader.getElementsByTagName("application/appInfo").item(0);
			annot_doc.setAttribute("FORMAT", app.getAttribute("version"));
			annot_doc.setAttribute("VERSION", app.getAttribute("version"));
	*/
			//Mise à jour du header
			//Attributs
			//media_file + elements media
			//besoin units et time
	        XPathExpression expr = this.xpath.compile("//recordingStmt/recording/media");
	        NodeList medias = (NodeList) expr.evaluate(this.teiDoc, XPathConstants.NODESET);
			if (medias.getLength()>0) {
				header.setAttribute("MEDIA_FILE", "");
				header.setAttribute("TIME_UNITS", "milliseconds");
				for(int i = 0; i<medias.getLength(); i++) {
					Element elt = (Element)medias.item(i);
					String url = elt.getAttribute("url");
					String mimeType = elt.getAttribute("mimeType");
					File mediaFile = new File(url);
					String mediaName = mediaFile.getName();
					Element eafMedia = elanDoc.createElement("MEDIA_DESCRIPTOR");
					eafMedia.setAttribute("MEDIA_URL", url);
					eafMedia.setAttribute("MIME_TYPE", mimeType);
					eafMedia.setAttribute("RELATIVE_MEDIA_URL", "./" + mediaName);
					header.appendChild(eafMedia);
				}
			}
			//properties : notes additionnelles
			NodeList notes = teiHeader.getElementsByTagName("note");
			for(int j = 0; j<notes.getLength(); j++){
				Element note = (Element) notes.item(j);
				if(note.getAttribute("type").equals("COMMENTS_DESC")){
					NodeList propertyNotes = note.getElementsByTagName("note");
					for(int y = 0; y<propertyNotes.getLength(); y++){
						Element propertyNote = (Element) propertyNotes.item(y);
						Element property = elanDoc.createElement("PROPERTY");
						property.setAttribute("NAME", propertyNote.getAttribute("type"));
						property.setTextContent(propertyNote.getTextContent());
						header.appendChild(property);
					}
				}
			}
		} catch(Exception e) {
			System.err.println("erreur dans le traitement du header");
		}
	}

	//Remplissage de la timeline à partir de la timeline au format TEI : copie exacte: mêmes identifiants et valeurs
	void buildTimeline(Element time_order){
		try {
			//besoin units et time
	        XPathExpression expr = this.xpath.compile("/TEI/text/timeline/when");
	        NodeList nodes = (NodeList) expr.evaluate(this.teiDoc, XPathConstants.NODESET);
			String prev_time_units = (String)this.xpath.compile("/TEI/text/timeline/@unit").evaluate(this.teiDoc, XPathConstants.STRING);
			double ratio = 1.0;
			String time_units;
			if (prev_time_units.equals("s")) {
				ratio = 1000.0;
				time_units = "milliseconds";
			} else if (prev_time_units.equals("ms")) {
				ratio = 1.0;
				time_units = "milliseconds";
			} else {
				ratio = -1.0;
				System.err.println("unité inconnue pour la timeline: " + prev_time_units + " pas de réajustement");
				time_units = prev_time_units;
			}
			Element header = (Element) annot_doc.getElementsByTagName("HEADER").item(0);
			header.setAttribute("TIME_UNITS", time_units);
			for(int i = 0; i<nodes.getLength(); i++){
				Element when = (Element)nodes.item(i);
				if(when.hasAttribute("interval")){
					String id = when.getAttribute("xml:id");
					String val = when.getAttribute("interval");
					Element time_slot = elanDoc.createElement("TIME_SLOT");
					time_slot.setAttribute("TIME_SLOT_ID", id);
					if (ratio < 0.0)
						time_slot.setAttribute("TIME_VALUE", val);
					else if (!val.isEmpty()) {
						double vald;
						try {
							vald = Double.parseDouble(val);
							vald *= ratio;
							time_slot.setAttribute("TIME_VALUE", Integer.toString((int)vald));
						} catch (Exception e) {
							System.err.println("not a double: " + val);
						}
					}
					time_order.appendChild(time_slot);
				} else if (when.hasAttribute("absolute")) {
					String id = when.getAttribute("xml:id");
					// here we should decode absolute
					// but it is not functional right now
					String val = when.getAttribute("absolute");
					Element time_slot = elanDoc.createElement("TIME_SLOT");
					time_slot.setAttribute("TIME_SLOT_ID", id);
					if (ratio < 0.0)
						time_slot.setAttribute("TIME_VALUE", val);
					else {
						time_slot.setAttribute("TIME_VALUE", "0");
						/* -- waiting for bug sold in value of absolute
						if (!val.isEmpty()) {
							double vald;
							try {
								vald = Double.parseDouble(val);
								vald *= ratio;
								time_slot.setAttribute("TIME_VALUE", Integer.toString((int)vald));
							} catch (Exception e) {
								System.err.println("not a double: " + val);
							}
						}
						*/
					}
					time_order.appendChild(time_slot);
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
			System.err.println("ERREUR dans le traitement de la timeline: " + e.toString());
		}
	}

	//Elements linguistic_type
	void buildLgqTypes(){
		Set<String> namesLgqTypes = new HashSet<String>();
		for(int j = 0; j<ttp.tierInfos.size(); j++){
			TierInfo ti = (TierInfo)ttp.tierInfos.get(j);
			if (!namesLgqTypes.contains(ti.type.lgq_type_id)) {
				namesLgqTypes.add(ti.type.lgq_type_id);
				Element lgqType = elanDoc.createElement("LINGUISTIC_TYPE");
				lgqType.setAttribute( "LINGUISTIC_TYPE_ID", ti.type.lgq_type_id);
				if (!ti.type.constraint.equals("-"))
					lgqType.setAttribute( "CONSTRAINTS", ti.type.constraint);
				if (LgqType.isTimeType(ti.type.constraint))
						lgqType.setAttribute( "TIME_ALIGNABLE", "true");
					else
						lgqType.setAttribute( "TIME_ALIGNABLE", "false");
				if(Utils.isNotEmptyOrNull(ti.type.cv_ref))
					lgqType.setAttribute( "CONTROLLED_VOCABULARY_REF", ti.type.cv_ref);
				lgqType.setAttribute("GRAPHIC_REFERENCES", Utils.isNotEmptyOrNull(ti.type.graphic_ref) ? ti.type.graphic_ref : "false");
				annot_doc.appendChild(lgqType);
			}
		}
	}

	//Elements constraint : toujours les mêmes dans les fichiers Elan
	void buildConstraints(){
		Element constraintTimeDiv = elanDoc.createElement("CONSTRAINT");
		constraintTimeDiv.setAttribute("DESCRIPTION", "Time subdivision of parent annotation's time interval, no time gaps allowed within this interval");
		constraintTimeDiv.setAttribute("STEREOTYPE", "Time_Subdivision");
		Element constraintSymbDiv = elanDoc.createElement("CONSTRAINT");
		constraintSymbDiv.setAttribute("DESCRIPTION", "Symbolic subdivision of a parent annotation. Annotations refering to the same parent are ordered");
		constraintSymbDiv.setAttribute("STEREOTYPE", "Symbolic_Subdivision");
		Element constraintSymbAssoc = elanDoc.createElement("CONSTRAINT");
		constraintSymbAssoc.setAttribute("DESCRIPTION", "1-1 association with a parent annotation");
		constraintSymbAssoc.setAttribute("STEREOTYPE", "Symbolic_Association");
		Element constraintInclIn = elanDoc.createElement("CONSTRAINT");
		constraintInclIn.setAttribute("DESCRIPTION", "Time alignable annotations within the parent annotation's time interval, gaps are allowed");
		constraintInclIn.setAttribute("STEREOTYPE", "Included_In");
		annot_doc.appendChild(constraintTimeDiv);
		annot_doc.appendChild(constraintSymbDiv);
		annot_doc.appendChild(constraintSymbAssoc);
		annot_doc.appendChild(constraintInclIn);
	}

	//Reconstruction du vocabulaire contrôlé
	List<Element> buildControlledVocabularies() {
		List<Element> listCV = new ArrayList<Element>();
		cvs = new HashMap<String, HashMap<String, String>>();
		if(this.teiDoc.getElementsByTagName("textClass").getLength()>0){
			Element textClass = (Element) this.teiDoc.getElementsByTagName("textClass").item(0);
			NodeList keywordsList = textClass.getElementsByTagName("keywords");
			for(int i = 0; i<keywordsList.getLength(); i++){
				Element keywords = (Element) keywordsList.item(i);
				HashMap<String, String> cvn = new HashMap<String, String>();
				Element controlled_voc = elanDoc.createElement("CONTROLLED_VOCABULARY");
				controlled_voc.setAttribute("CV_ID", keywords.getAttribute("scheme"));
				cvs.put(keywords.getAttribute("scheme"), cvn);
				Element description = elanDoc.createElement("DESCRIPTION");
				description.setTextContent(keywords.getAttribute("style"));
				description.setAttribute("LANG_REF", keywords.getAttribute("xml:lang"));
				controlled_voc.appendChild(description);
				NodeList terms = keywords.getElementsByTagName("term");
				for (int j = 0; j<terms.getLength(); j++) {
					Element term = (Element)terms.item(j);
					Element cvEntry = elanDoc.createElement("CV_ENTRY_ML");
					cvEntry.setAttribute("CVE_ID", "cveid" + j);
					Element cveValue = elanDoc.createElement("CVE_VALUE");
					cveValue.setTextContent(term.getTextContent());
					cvn.put(term.getTextContent(), "cveid" + j);
					cveValue.setAttribute("DESCRIPTION", term.getAttribute("type"));
					cveValue.setAttribute("LANG_REF", term.getAttribute("xml:lang"));
					cvEntry.appendChild(cveValue);
					controlled_voc.appendChild(cvEntry);
				}
				listCV.add(controlled_voc);
			}
		}
		return listCV;
	}

	//Ajout des attributs des élement TIER
	String setTierAtt (Element tier, String type){
		for(TierInfo ti : ttp.tierInfos) {
			if(ti.tier_id.equals(type)){
				if(Utils.isNotEmptyOrNull(ti.participant)) tier.setAttribute("PARTICIPANT", ti.participant);
				if(Utils.isNotEmptyOrNull(ti.lang)) tier.setAttribute("DEFAULT_LOCALE", ti.lang);
				if(Utils.isNotEmptyOrNull(ti.annotator)) tier.setAttribute("ANNOTATOR", ti.annotator);
				if(Utils.isNotEmptyOrNull(ti.lang_ref)) tier.setAttribute("LANG_REF", ti.lang_ref);
				if(Utils.isNotEmptyOrNull(ti.parent) && !ti.parent.equals("-")) tier.setAttribute("PARENT_REF", ti.parent);
				if(Utils.isNotEmptyOrNull(ti.type.lgq_type_id)) tier.setAttribute("LINGUISTIC_TYPE_REF", ti.type.lgq_type_id);
				return ti.type.cv_ref;
			}
		}
		return "";
	}

	//Tiers...
	void buildTiers(){
		// ttp.getTiers();
		for(Map.Entry<String, ArrayList<Annot>> entry: ttp.tiers.entrySet()){
			Element tier = elanDoc.createElement("TIER");
			String type = entry.getKey();
			tier.setAttribute("TIER_ID", type);
			String cvref;
			if (ttp.newTiers.containsKey(type)) {
				// System.out.printf("%s %s %n", type, ttp.newTiers.get(type));
				cvref = setTierAtt(tier, ttp.newTiers.get(type));
			} else
				cvref = setTierAtt(tier, type);
			this.annot_doc.appendChild(tier);
			for(Annot a : entry.getValue()){
				Element annot = elanDoc.createElement("ANNOTATION");
				if(a.type.equals("time")){
					Element align_annot = elanDoc.createElement("ALIGNABLE_ANNOTATION");
					align_annot.setAttribute("ANNOTATION_ID", a.id);
					align_annot.setAttribute("TIME_SLOT_REF1", a.start);
					align_annot.setAttribute("TIME_SLOT_REF2", a.end);
					annot.appendChild(align_annot);
					Element annotationValue = elanDoc.createElement("ANNOTATION_VALUE");
					annotationValue.setTextContent(a.content);
					if (Utils.isNotEmptyOrNull(cvref)) {
						Map<String, String> cvi = cvs.get(cvref);
						String cve = cvi.get(a.content);
						if (Utils.isNotEmptyOrNull(cve))
							align_annot.setAttribute("CVE_REF", cve);
					}
					align_annot.appendChild(annotationValue);
					annot.appendChild(align_annot);
				}
				else if (a.type.equals("ref")){
					Element ref_annot = elanDoc.createElement("REF_ANNOTATION");
					ref_annot.setAttribute("ANNOTATION_ID", a.id);
					ref_annot.setAttribute("ANNOTATION_REF", a.link);
					if (Utils.isNotEmptyOrNull(a.previous)) ref_annot.setAttribute("PREVIOUS_ANNOTATION", a.previous);
					annot.appendChild(ref_annot);
					Element annotationValue = elanDoc.createElement("ANNOTATION_VALUE");
					annotationValue.setTextContent(a.content);
					if (Utils.isNotEmptyOrNull(cvref)) {
						Map<String, String> cvi = cvs.get(cvref);
						String cve = cvi.get(a.content);
						if (Utils.isNotEmptyOrNull(cve))
							ref_annot.setAttribute("CVE_REF", cve);
					}
					ref_annot.appendChild(annotationValue);
					annot.appendChild(ref_annot);					
				}
				tier.appendChild(annot);
			}
		}
	}

	// Création du fichier de sortie
	public void createOutput() {
		Source source = new DOMSource(elanDoc);
		Result resultat = new StreamResult(outputName);

		try {
			TransformerFactory fabrique2 = TransformerFactory.newInstance();
			Transformer transformer = fabrique2.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.transform(source, resultat);
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	public static void usage() {
		System.err.println("Description: TeiToElan convertit un fichier au format Tei en un fichier au format Elan");
		System.err.println("Usage: TeiToElan [-options] <file" + Utils.EXT + ">");
		System.err.println("	     :-i nom du fichier ou repertoire où se trouvent les fichiers Tei à convertir (les fichiers ont pour extension " + Utils.EXT);
		System.err.println("	     :-o nom du fichier de sortie au format Elan (.eaf) ou du repertoire de résultats");
		System.err.println("	     	si cette option n'est pas spécifié, le fichier de sortie aura le même nom que le fichier d'entrée avec l'extension .eaf;");
		System.err.println("	     	si on donne un repertoire comme input et que cette option n'est pas spécifiée, les résultats seront stockés dans le même dossier que l'entrée.\"");
		System.err.println("	     :-usage ou -help = affichage ce message");
		System.exit(1);
	}	

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
					TeiToElan tte = new TeiToElan(file.getAbsolutePath(), outputDir+outputFileName);
					System.out.println(outputDir+outputFileName);
					tte.createOutput();
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

			if (!Utils.validFileFormat(input, Utils.EXT)) {
				System.err.println("Le fichier d'entrée du programme doit avoir l'extension" + Utils.EXT);
				usage();
			}
			TeiToElan tte = new TeiToElan(new File(input).getAbsolutePath(), output);
			System.out.println("Reading " + input);
			tte.createOutput();
			System.out.println("New file created " + output);
		}
	}
}
