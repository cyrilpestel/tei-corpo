package fr.ortolang.teicorpo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class Utils {
	
	public static final int styleLexicoTxm = 2;
	public static String EXT = ".tei_corpo.xml";
	public static String EXT_PUBLISH = ".tei_corpo";
	public static String ANNOTATIONBLOC = "annotationBlock";
	public static String versionTEI = "0.9";
	public static String versionSoft = "1.046"; // full version with Elan, Clan, Transcriber and Praat
	public static String versionDate = "30/05/2016 09:00";
//	public static String TEI_ALL = "http://localhost/teiconvertbeta/tei_all.dtd";
	public static String TEI_ALL = "http://ct3.ortolang.fr/tei-corpo/tei_all.dtd";
	public static String TEI_CORPO_DTD = "http://ct3.ortolang.fr/tei-corpo/tei_corpo.dtd";
	public static boolean teiStylePure = false;
	
	public static String shortPause = " # ";
	public static String longPause = " ## ";
	public static String veryLongPause = " ### ";
	public static String specificPause = "#";
	
	public static String leftBracket = "⟪"; // 27EA - "❮"; // "⟨" 27E8 - "❬" 
	public static String rightBracket = "⟫"; // 27EB - "❯"; // "⟩" 27E9 - "❭" - 276C à 2771 ❬ ❭ ❮ ❯ ❰ ❱ 
	public static String leftEvent = "⟦"; // 27E6 - "『"; // 300E - "⌈"; // u2308 
	public static String rightEvent = "⟧"; // 27E7 - "』"; // 300F - "⌋"; // u230b
	public static String leftParent = "⁅"; // 2045 // "⁘"; // 2058 // "⁑" // 2051
	public static String rightParent = "⁆"; // 2046 // "⁘"; // 2058
	public static String leftCode = "⌜"; // 231C - "⁌"; // 204C
	public static String rightCode = "⌟"; // 231F - "⁍"; // 204D

	public static String teiCorpoDtd() {
		// return teiStylePure == true ? TEI_ALL : TEI_CORPO_DTD;
		return TEI_ALL;
	}

	public static boolean isElement(Node n){
		return n.getNodeType() == Node.ELEMENT_NODE;
	}

	public static boolean isText(Node n){
		return n.getNodeType() == Node.TEXT_NODE;
	}

	public static boolean isNotEmptyOrNull(String s){
		return s!= null && !s.isEmpty() && s != "";
	}

	public static String cleanString(String s){
		return s.trim().replaceAll(" {2,}", " ").replaceAll("\n", "").trim();
	}

	public static String join(String... args) {
		String result = "";
		for(String st : args){
			result += st + " ";
		}
		return result;
	}

	public static String join(String[] s, String delimiter) {
		StringBuffer buffer = new StringBuffer();
		int i = 0;
		while (i < s.length-1) {
			buffer.append(s[i]);
			buffer.append(delimiter);
			i++;
		}
		buffer.append(s[i]);
		return buffer.toString();
	}
	
	public static String getInfo2(String line){
		if (isNotEmptyOrNull(line)){
			try{
				String [] tab = line.split("] ");
				String info = "";
				for (int i = 1; i< tab.length; i++){
					info += tab[i] + " ";
				}
				return info.substring(0, info.length()-1);
			}
			catch(StringIndexOutOfBoundsException e){}
		}
		return "";
	}

	public static String getInfo(String line){
		if (isNotEmptyOrNull(line)){
			try{
				String [] tab = line.split("\t");
				String info = "";
				for (int i = 1; i< tab.length; i++){
					info += tab[i] + " ";
				}
				return info.substring(0, info.length()-1);
			}
			catch(StringIndexOutOfBoundsException e){}
		}
		return "";
	}

	public static String getInfoType(String line){
		try{
			return line.split("\t")[0].split(":")[0];
		}
		catch(Exception e){
			return "";
		}
	}
	
	/**
	 * Création du fichier TEI à partir de la transcription originale. 
	 * @param outputFileName
	 *            Nom du fichier TEI à créer.
	 */
	public static void createFile(String outputFileName, Document d) {

		Source source = new DOMSource(d);

		Result resultat = new StreamResult(outputFileName);

		try {
			// Configuration du transformer
			TransformerFactory fabrique2 = TransformerFactory.newInstance();
			Transformer transformer = fabrique2.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, Utils.teiCorpoDtd());

			// Transformation
			transformer.transform(source, resultat);
			System.out.println("Fichier TEI créé : " + outputFileName);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void setDTDvalidation(DocumentBuilderFactory factory,	boolean b) {
		try {
			factory.setValidating(b);
			factory.setFeature("http://xml.org/sax/features/namespaces", b);
			factory.setFeature("http://xml.org/sax/features/validation", b);
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar",	b);
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", b);
		}
		catch (Exception e) {
			System.err.println("Votre fichier n'est pas conforme à la DTD passée en argument");
			e.printStackTrace();
		}
	} 

	public static int toHours(float t) {
		t = t / 60;
		t = t / 60;
		return (int)t;
	}

	public static int toMinutes(float t) {
		t = t / 60;
		return (int)(t % 60);
	}

	public static int toXMinutes(float t) {
		t = t / 60;
		return (int)(t);
	}

	public static int toSeconds(float t) {
		return (int)(t % 60);
	}

	public static int toMilliSeconds(float t) {
		return (int)(t * 1000);
	}

	public static int toCentiSeconds(float t) {
		return (int)(t/10);
	}

	public static String printDouble(double value, int precision) {
		if (value <= 0.0) return "0";
		double intpart = Math.floor(value);
	    BigDecimal bd = new BigDecimal(value);
	    BigDecimal bdintpart = new BigDecimal(intpart);
	    bd = bd.setScale(precision, RoundingMode.HALF_UP);
	    BigDecimal bdb = bd.subtract(bdintpart);
	    // System.out.println(bd + " " + bdb);
	    if (bdb.compareTo(new BigDecimal("0E-15")) <= 0)
		    return bdintpart.toString() + ".0";
	    bdb = bdb.setScale(precision, RoundingMode.HALF_UP);
	    return bdintpart.toString() + "." + bdb.toString().substring(2);
	}

	public static String fullbasename(String fileName) {
		int p = fileName.lastIndexOf('.');
		if (p >= 0)
			return fileName.substring(0, p);
		else
			return fileName;
	}

	public static String fullbasename(File file) {
		String fileName = file.toString();
		return fullbasename(fileName);
	}

	public static String lastname(String fn) {
		int p = fn.lastIndexOf(File.separatorChar);
		return fn.substring(p+1);
	}

	public static String basename(String filename) {
		String bn = lastname(filename);
		int p = bn.lastIndexOf('.');
		if (p >= 0)
			return bn.substring(0, p);
		else
			return bn;
	}

	public static String basename(File file) {
		String filename = file.toString();
		return basename(filename);
	}

	public static boolean validFileFormat(String fileName, String extension) {
		return fileName.toLowerCase().endsWith(extension);
	}

	public static String convertMonthStringToInt(String monthString){
		if(monthString.toLowerCase().equals("jan")){
			return "01";
		}
		else if(monthString.toLowerCase().equals("feb") || monthString.toLowerCase().equals("fev")){
			return "02";
		}
		else if(monthString.toLowerCase().equals("mar")){
			return "03";
		}
		else if(monthString.toLowerCase().equals("apr")){
			return "04";
		}
		else if(monthString.toLowerCase().equals("may")){
			return "05";
		}
		else if(monthString.toLowerCase().equals("jun")){
			return "06";
		}
		else if(monthString.toLowerCase().equals("jul")){
			return "07";
		}
		else if(monthString.toLowerCase().equals("aug")){
			return "08";
		}
		else if(monthString.toLowerCase().equals("sep")){
			return "09";
		}
		else if(monthString.toLowerCase().equals("oct")){
			return "10";
		}
		else if(monthString.toLowerCase().equals("nov")){
			return "11";
		}
		else if(monthString.toLowerCase().equals("dec")){
			return "12";
		}
		return monthString;
	}

	public static void setMimeType(Element media, String mediaName){
		//Video
		if(mediaName.endsWith(".mp4")){
			media.setAttribute("mimeType", "video/mp4");
		}
		else if(mediaName.endsWith(".webm")){
			media.setAttribute("mimeType", "video/webm");
		}
		else if(mediaName.endsWith(".ogv")){
			media.setAttribute("mimeType", "video/ogg");
		}
		//Audio
		else if(mediaName.endsWith(".mp3")){
			media.setAttribute("mimeType", "audio/mpeg");
		}
		else if(mediaName.endsWith(".ogg")){
			media.setAttribute("mimeType", "audio/ogg");
		}
		else if(mediaName.endsWith(".wav")){
			media.setAttribute("mimeType", "audio/wav");
		}
	}
	
	public static void sortTimeline(ArrayList<Element> timeline){
		CompareTimeline ct = new CompareTimeline();		
		Collections.sort(timeline, ct);
	}

	public static String findClosestMedia(String dir, String fn, String type) {
		// TODO Auto-generated method stub
		String[] extsVideo = { "-480p.mp4",".mp4","-720p.mp4","-240p.mp4",
				"-480p.webm",".webm","-720p.webm","-240p.webm",
				".mov",".mpg",".avi",".fv", "ogv" };
		String[] extsAudio = { ".aif", ".wav", ".mp3" };
		int p = fn.lastIndexOf(".");
		if (p >= 0) {
			String ext = fn.substring(p);
			if (java.util.Arrays.asList(extsVideo).indexOf(ext) >= 0)
				return fn;
			if (java.util.Arrays.asList(extsAudio).indexOf(ext) >= 0)
				return fn;
		}
		if (Utils.isNotEmptyOrNull(dir))
			fn = dir + File.separator + fn;
		String cleanFn = fn;
		try {
			File ffn = new File(fn);
			cleanFn = ffn.getCanonicalPath();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			cleanFn = fn;
		}
		p = cleanFn.lastIndexOf(".");
		String fnbase = (p >= 0) ? cleanFn.substring(0,p) : cleanFn;
		if (type.indexOf("video") >= 0) {
			for (int i=0; i < extsVideo.length; i++) {
				File file = new File(fnbase + extsVideo[i]);
				if (file.exists())
					return file.getName();
			}
		}
		if (type.indexOf("audio") >= 0) {
			for (int i=0; i < extsAudio.length; i++) {
				File file = new File(fnbase + extsAudio[i]);
				if (file.exists())
					return file.getName();
			}
		}
		if (type.indexOf("video") >= 0)
			return Utils.basename(fn) + ".mp4";
		if (type.indexOf("audio") >= 0)
			return Utils.basename(fn) + ".wav";
		else
			return Utils.basename(fn);
	}
	
	public static String findMediaType(String fn) {
		String ext = "";
		int p = fn.lastIndexOf(".");
		if (p >= 0) {
			ext = fn.substring(p);
		}
		if (ext.isEmpty()) {
			return "unknown";
		} else {
			if (ext.equals(".wav"))
				return "audio/x-wav";
			else if (ext.equals(".mp3"))
				return "audio/mp3";
			else if (ext.equals(".m4a"))
				return "audio/m4a";
			else if (ext.equals(".mov"))
				return "video/quicktime";
			else if (ext.equals(".mp4"))
				return "video/mp4";
			else if (ext.equals(".mpg"))
				return "video/mpg";
			else if (ext.equals(".webm"))
				return "video/webm";
			else
				return "unknown";
		}
	}

	public static Element createDivHead(Document docTEI) {
		Element div = docTEI.createElement("div");
		Element head = docTEI.createElement("head");
		div.appendChild(head);
		return div;
	}

	public static void setDivHeadAttr(Document docTEI, Element div, String type, String value) {
		NodeList head = div.getElementsByTagName("head");
		if (head.getLength() == 0) {
			System.err.println("Div should contain Head");
			try {
				throw new Exception();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		Element note = docTEI.createElement("note");
		note.setAttribute("type", type);
		note.setTextContent(value);
		head.item(0).appendChild(note);
	}

	public static String getDivHeadAttr(Element annotU, String attrName) {
		String a = annotU.getAttribute(attrName);
		if (a == null || a.isEmpty())
			return getHeadAttr(annotU, attrName);
		return a;
	}

	public static String getHeadAttr(Element annotU, String attrName) {
		NodeList nl = annotU.getElementsByTagName("head");
		if (nl == null) return "";
		Element head = (Element)nl.item(0);
		if (head == null) return "";
		NodeList notes = head.getElementsByTagName("note");
		for (int i=0; i < notes.getLength(); i++) {
			Element e = (Element)(notes.item(i));
			String a = e.getAttribute("type");
			if (a.equals(attrName))
				return e.getTextContent();
		}
		return "";
	}

	public static Element createAnnotationBloc(Document docTEI) {
		Element bloc;
		if (Utils.teiStylePure == true) {
			// simulate non existing TEI element with actual existing elements
			bloc = docTEI.createElement("div");
			bloc.setAttribute("type", Utils.ANNOTATIONBLOC);
			Element head = docTEI.createElement("head");
			bloc.appendChild(head);
		} else {
			// use extensions of TEI for Oral
			bloc = docTEI.createElement(Utils.ANNOTATIONBLOC);
		}
		return bloc;
	}

	public static void setAttrAnnotationBloc(Document docTEI, Element bloc, String type, String value) {
		if (Utils.teiStylePure == true) {
			setDivHeadAttr(docTEI, bloc, type, value);
		} else {
			bloc.setAttribute(type, value);
		}
	}

	public static NodeList getAllDivs(XPath xpath, Document docTEI) throws XPathExpressionException {
		NodeList nl;
		if (Utils.teiStylePure == true) {
	        XPathExpression expr = xpath.compile("//div[not(@type=\"" + Utils.ANNOTATIONBLOC + "\")]");
	        nl = (NodeList) expr.evaluate(docTEI, XPathConstants.NODESET);
		} else {
			nl = docTEI.getElementsByTagName("div");
		}
		return nl;
	}

	public static NodeList getAllAnnotationBloc(XPath xpath, Document docTEI) throws XPathExpressionException {
		NodeList nl;
		if (Utils.teiStylePure == true) {
	        XPathExpression expr = xpath.compile("//div[@type=\"" + Utils.ANNOTATIONBLOC + "\"]");
	        nl = (NodeList) expr.evaluate(docTEI, XPathConstants.NODESET);
		} else {
			nl = docTEI.getElementsByTagName(Utils.ANNOTATIONBLOC);
		}
		return nl;
	}

	public static NodeList getSomeAnnotationBloc(XPath xpath, Element eltTop) throws XPathExpressionException {
		NodeList nl;
		if (Utils.teiStylePure == true) {
			NodeList childs = eltTop.getChildNodes();
			return childs;
//			for (int i=0; i < childs.getLength(); i++) {
//				if (((Element)childs.item(i)).getAttribute("type") == Utils.ANNOTATIONBLOC) nl.add(childs.item(i));
//			}
//			return nl;
//	        XPathExpression expr = xpath.compile("//div[@type=\"" + Utils.ANNOTATIONBLOC + "\"]");
//	        nl = (NodeList) expr.evaluate(eltTop, XPathConstants.NODESET);
		} else {
			nl = eltTop.getElementsByTagName(Utils.ANNOTATIONBLOC);
		}
		return nl;
	}

	public static String getAttrAnnotationBloc(Element annotU, String attrName) {
		if (Utils.teiStylePure == true) {
			return getHeadAttr(annotU, attrName);
		} else {
			return annotU.getAttribute(attrName);
		}
	}

	public static boolean isAnnotatedBloc(Element el) {
		if (Utils.teiStylePure == true) {
			return (el.getAttribute("type").equals(Utils.ANNOTATIONBLOC)) ? true : false;
		} else {
			return (el.getNodeName().equals(Utils.ANNOTATIONBLOC)) ? true : false;
		}
	}

	public static String refID(String refid) {
		if (refid.startsWith("#"))
			return refid.substring(1);
		return refid;
	}

	public static void main(String[] args) {
        final String xmlStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"+
                                "<Emp id=\"1\"><name>Pankaj</name><age>25</age>\n"+
                                "<role>Developer</role><gen>Male</gen></Emp>";
        Document doc = convertStringToDocument(args.length<1 ? xmlStr : args[0]);
         
        String str = convertDocumentToString(doc, false);
        System.out.println(str);
    }
 
    public static String convertDocumentToString(Document doc, boolean withHeader) {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = tf.newTransformer();
            // below code to remove XML declaration
            // transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            String output = writer.getBuffer().toString();
            if (withHeader)
            	return output;
            else
            	return output.replaceFirst("<.*?>", "");
        } catch (TransformerException e) {
            e.printStackTrace();
        }
         
        return null;
    }
 
    public static Document convertStringToDocument(String xmlStr) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();  
        DocumentBuilder builder;  
        try 
        {  
            builder = factory.newDocumentBuilder();  
            Document doc = builder.parse( new InputSource( new StringReader( xmlStr ) ) ); 
            return doc;
        } catch (Exception e) {  
            e.printStackTrace();  
        } 
        return null;
    }

    public static List<String> loadTextFile(String filename) throws IOException {
		List<String> ls = new ArrayList<String>();
		String line = "";
		BufferedReader reader = null;
		try {
			reader = new BufferedReader( new InputStreamReader(new FileInputStream(filename)) );
			while((line = reader.readLine()) != null) {
				// Traitement du flux de sortie de l'application si besoin est
				ls.add(line.trim());
			}
		}
		catch (FileNotFoundException fnfe) {
			System.err.println("Erreur fichier : " + filename + " indisponible");
			System.exit(1);
			return null;
		}
		catch(IOException ioe) {
			System.err.println("Erreur sur fichier : " + filename );
			ioe.printStackTrace();
			System.exit(1);
			return null;
		}
		finally {
			if (reader != null) reader.close();
		}
		return ls;
	}

	public static void printVersionMessage() {
    	System.out.println("Conversions (version "+ Utils.versionSoft +") " + Utils.versionDate + " Version TEI_CORPO: " + Utils.versionTEI);
	}
	
	public static void printUsageMessage(String mess, String ext1, String ext2, int style) {
		System.err.printf(mess);
		System.err.println("	     :-i nom du fichier ou repertoire où se trouvent les fichiers Tei à convertir (les fichiers ont pour extension " + ext1);
		System.err.println("	     :-o nom du fichier de sortie au format Elan (.eaf) ou du repertoire de résultats");
		System.err.println("	     	si cette option n'est pas spécifié, le fichier de sortie aura le même nom que le fichier d'entrée avec l'extension " + ext2);
		System.err.println("	     	si on donne un repertoire comme input et que cette option n'est pas spécifiée, les résultats seront stockés dans le même dossier que l'entrée.\"");
		System.err.println("         :-p fichier_de_parametres: contient les paramètres sous leur format ci-dessous, un jeu de paramètre par ligne.");
		System.err.println("	     :-n niveau: niveau d'imbrication (1 pour lignes principales)");
		System.err.println("	     :-a name : le locuteur/champ name est produit en sortie (caractères génériques acceptés)");
		System.err.println("	     :-s name : le locuteur/champ name est suprimé de la sortie (caractères génériques acceptés)");
		System.err.println("	     :-cleanline : exporte des énoncés sans marqueurs spéficiques de l'oral");
		if (style == 2)
			System.err.println("	     :-tv \"type:valeur\" : un champ type:valeur est ajouté dans les <w> de txm ou lexico ou le trameur");
		if (style == 2)
			System.err.println("	     :-section : ajoute un indicateur de section en fin de chaque énoncé (pour lexico/le trameur)");
		System.err.println("	     :-usage ou -help = affichage ce message");
		// System.exit(1);
	}

	public static TierParams getTierParams(String fn, TierParams tp) {
		List<String> ls = null;
		try {
			ls = loadTextFile(fn);
		} catch (IOException e) {
			System.err.println("Impossible de traiter le fichier: " + fn);
			return null;
		}
		for (int k=0; k<ls.size(); k++) {
			String l = ls.get(k);
			String[] p = l.split("\\s+");
			if (p.length > 0) {
				if (p[0].equals("-n") || p[0].equals("n")) {
					if (p.length < 2) {
						System.out.println("Mauvaise ligne [" + l + "] dans le fichier paramètre: " + fn);
						return null;
					}
					tp.setLevel(Integer.parseInt(p[1]));
				} else if (p[0].equals("-s") || p[0].equals("s")) {
					if (p.length < 2) {
						System.out.println("Mauvaise ligne [" + l + "] dans le fichier paramètre: " + fn);
						return null;
					}
					tp.addDontDisplay(p[1]);
				} else if (p[0].equals("-a") || p[0].equals("a")) {
					if (p.length < 2) {
						System.out.println("Mauvaise ligne [" + l + "] dans le fichier paramètre: " + fn);
						return null;
					}
					tp.addDoDisplay(p[1]);
				} else {
					System.out.println("Format inconnu dans le fichier paramètre: " + fn);
					return null;
				}
			}
		}
		return tp;
	}

	public static boolean processArgs(String[] args, TierParams options, String usage, String ext1, String ext2, int style) {
		if (args.length == 0) {
			System.err.println("Vous n'avez spécifié aucun argument\n");
			Utils.printUsageMessage(usage, ext1, ext2, style);
			return true;
		} else {
			for (int i = 0; i < args.length; i++) {
				try {
					if (args[i].equals("-i")) {
						i++;
						continue;
					} else if (args[i].equals("-o")) {
						i++;
						continue;
					} else if (args[i].equals("-n")) {
						i++;
						continue;
					} else if (args[i].equals("-a")) {
						i++;
						continue;
					} else if (args[i].equals("-s")) {
						i++;
						continue;
					} else if (args[i].equals("-tv")) {
						i++;
						continue;
					} else if (args[i].equals("-p")) {
						if (i+1 >= args.length) {
							Utils.printUsageMessage(usage, ext1, ext2, style);
							return false;
						}
						i++;
						Utils.getTierParams(args[i], options);
					} else {
						continue;
					}
				} catch (Exception e) {
					Utils.printUsageMessage(usage, ext1, ext2, style);
					return false;
				}
			}
			for (int i = 0; i < args.length; i++) {
				try {
					if (args[i].equals("-i")) {
						if (i+1 >= args.length) {
							Utils.printUsageMessage(usage, ext1, ext2, style);
							return false;
						}
						i++;
						options.input = args[i];
					} else if (args[i].equals("-o")) {
						if (i+1 >= args.length) {
							Utils.printUsageMessage(usage, ext1, ext2, style);
							return false;
						}
						i++;
						options.output = args[i];
					} else if (args[i].equals("-n")) {
						if (i+1 >= args.length) {
							Utils.printUsageMessage(usage, ext1, ext2, style);
							return false;
						}
						i++;
						try {
							options.setLevel(Integer.parseInt(args[i]));
						} catch(Exception e) {
							System.err.println("Le paramètre -n n'est pas suivi d'un entier.");
							Utils.printUsageMessage(usage, ext1, ext2, style);
							return false;
						}
					} else if (args[i].equals("-a")) {
						if (i+1 >= args.length) {
							Utils.printUsageMessage(usage, ext1, ext2, style);
							return false;
						}
						i++;
						options.addDoDisplay(args[i]);
					} else if (args[i].equals("-s")) {
						if (i+1 >= args.length) {
							Utils.printUsageMessage(usage, ext1, ext2, style);
							return false;
						}
						i++;
						options.addDontDisplay(args[i]);
					} else if (args[i].equals("-tv")) {
						if (i+1 >= args.length) {
							Utils.printUsageMessage(usage, ext1, ext2, style);
							return false;
						}
						i++;
						options.addTv(args[i]);
					} else if (args[i].equals("-p")) {
						i++;
						continue;
					} else if (args[i].equals("-cleanline")) {
						options.cleanLine = true;
						continue;
					} else if (args[i].equals("-section")) {
						options.sectionDisplay = true;
						continue;
					} else {
						Utils.printUsageMessage(usage, ext1, ext2, style);
						return false;
					}
				} catch (Exception e) {
					Utils.printUsageMessage(usage, ext1, ext2, style);
					return false;
				}
			}
		}
		return true;
	}

	public static void setAttrAnnotationBlocSupplement(Document docTEI, Element annotatedU, String string,
			String attValue) {
		NodeList spanGrpList = annotatedU.getElementsByTagName("spanGrp");
		if (spanGrpList != null && spanGrpList.getLength() > 0) {
			for (int i=0; i<spanGrpList.getLength(); i++) {
				Element spanGrp = (Element) spanGrpList.item(i);
				if (spanGrp.getAttribute("type").equals("TurnInformation")) {
					NodeList spanList = spanGrp.getElementsByTagName("span");
					if (spanList != null && spanList.getLength() > 0) {
						for (int j=0; j<spanList.getLength(); j++) {
							Element span = (Element) spanList.item(j);
							if (span.getAttribute("type").equals(string)) {
								span.setAttribute("type", string);
								return;
							}
						}
					}
					// si pas trouvé
					// ou si pas de span dans le spanGrp TurnInformation
					Element s = docTEI.createElement("span");
					s.setAttribute("type", string);
					s.setTextContent(attValue);
					spanGrp.appendChild(s);
					return;
				}
			}
		}
		// si pas de spanGrp s'appelent TurnInformation
		// ou si pas encore de spanGrp
		Element sg = docTEI.createElement("spanGrp");
		sg.setAttribute("type", "TurnInformation");
		Element s = docTEI.createElement("span");
		s.setAttribute("type", string);
		s.setTextContent(attValue);
		sg.appendChild(s);
		annotatedU.appendChild(sg);
	}
}
