package fr.ortolang.teicorpo;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	
	public static String EXT = ".tei_corpo.xml";
	public static String EXT_PUBLISH = ".tei_corpo";
	public static String ANNOTATIONBLOC = "annotationBloc";
	public static String versionTEI = "0.9";
	public static String versionSoft = "1.0"; // full version with Elan, Clan, Transcriber and Praat
	public static String TEI_ALL = "http://ct3.ortolang.fr/tei_corpo/tei_all.dtd";
	public static String TEI_CORPO_DTD = "http://ct3.ortolang.fr/tei_corpo/tei_corpo.dtd";
	public static boolean teiStylePure = false;

	public static String teiCorpoDtd() {
		return teiStylePure == true ? TEI_ALL : TEI_CORPO_DTD;
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
		return fn;
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
		Element head = (Element)annotU.getElementsByTagName("head").item(0);
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

}