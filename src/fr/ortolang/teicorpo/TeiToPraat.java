package fr.ortolang.teicorpo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

public class TeiToPraat {

	TeiToPartition ttp = null;
	double xmaxTime = 0.0;
	HashMap<String, Double> timeline;
	
	//Permet d'écrire le fichier de sortie
	private PrintWriter out;
	//Encodage du fichier de sortie
	final static String outputEncoding = "UTF-16";
	//Extension du fichier de sortie: .textgrid
	final static String EXT = ".textgrid";

	//Nom du fichier teiml à convertir
	String inputName;
	//Nom du fichier de sortie
	String outputName;

	//Document TEI à lire
	public Document teiDoc;
	// acces Xpath
	public XPathFactory xPathfactory;
	public XPath xpath;

	//Validation du document Tei par la dtd
	boolean validation = false;

	//Constructeur à partir du nom du fichier TEI et du nom du fichier de sortie au format Elan
	public TeiToPraat(String inputName, String outputName) {
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
		out = null;
		try{
			FileOutputStream of = new FileOutputStream(outputName);
			OutputStreamWriter outWriter = new OutputStreamWriter(of, outputEncoding );
			out = new PrintWriter(outWriter, true);
		}
		catch(Exception e){
			out = new PrintWriter(System.out, true);
		}
	}

	//Conversion du fichier TEI vers Elan
	public void conversion() {
		// récupérer la timeline
		buildTimeline();
		//Construction de l'en-tête
		buildHeader();
		//Construction des tiers
		buildTiers();
	}

	//Information contenues dans l'élément annotation_doc et dans l'élément header
	public void buildHeader(){
		out.println("File type = \"ooTextFile\"");
		out.println("Object class = \"TextGrid\"");
		out.println("");
		out.println("xmin = 0");
		out.printf("xmax = %s%n", printDouble(xmaxTime));
		out.println("tiers? <exists>");
		out.printf("size = %d%n", ttp.tiers.size()); 
	}


	private String printDouble(double value) {
		if (value <= 0.0) return "0";
		double intpart = Math.floor(value);
	    BigDecimal bd = new BigDecimal(value);
	    BigDecimal bdintpart = new BigDecimal(intpart);
	    bd = bd.setScale(15, RoundingMode.HALF_UP);
	    return bdintpart.toString() + "." + (bd.subtract(bdintpart)).toString().substring(2);
	}
	
	//Remplissage de la timeline à partir de la timeline au format TEI : copie exacte: mêmes identifiants et valeurs
	void buildTimeline() {
		try {
			timeline = new HashMap<String, Double>();
			//besoin units et time
	        XPathExpression expr = this.xpath.compile("/TEI/text/timeline/when");
	        NodeList nodes = (NodeList) expr.evaluate(this.teiDoc, XPathConstants.NODESET);
			String prev_time_units = (String)this.xpath.compile("/TEI/text/timeline/@unit").evaluate(this.teiDoc, XPathConstants.STRING);
			double ratio = 1.0;
			if (prev_time_units.equals("s")) {
				ratio = 1.0;
			} else if (prev_time_units.equals("ms")) {
				ratio = 1000.0;
			} else {
				ratio = -1.0;
				System.err.println("unité inconnue pour la timeline: " + prev_time_units + " pas de réajustement");
			}
			for(int i = 0; i<nodes.getLength(); i++){
				String val = "";
				try {
					Element when = (Element)nodes.item(i);
					if(when.hasAttribute("interval")){
						String id = when.getAttribute("xml:id");
						val = when.getAttribute("interval");
						if (ratio < 0.0)
							timeline.put(id, Double.parseDouble(val));
						else if (!val.isEmpty()) {
							double vald;
								vald = Double.parseDouble(val);
								vald *= ratio;
								timeline.put(id, vald);
								if (vald > xmaxTime)
									xmaxTime = vald;
						}
					} else if (when.hasAttribute("absolute")) {
						String id = when.getAttribute("xml:id");
						// here we should decode absolute
						// but it is not functional right now
						val = when.getAttribute("absolute");
						if (ratio < 0.0)
							timeline.put(id, Double.parseDouble(val));
						else {
							timeline.put(id, 0.0);
						}
					}
				} catch (Exception e) {
					System.err.println("not a double: " + val);
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
			System.err.println("ERREUR dans le traitement de la timeline: " + e.toString());
		}
	}

	//Tiers...
	void buildTiers() {
		int nth = 1;
		out.println("item []:");
		for(Map.Entry<String, ArrayList<Annot>> entry: ttp.tiers.entrySet()) {
			out.printf("    item [%d]:%n", nth++); 
			out.println("        class = \"IntervalTier\"");
			out.printf("        name = \"%s\"%n", entry.getKey());
			out.println("        xmin = 0"); 
			
			double kmax = 0.0;
			for(Annot a : entry.getValue()) {
				if(a.type.equals("time")) {
					double start = timeline.get(a.start);
					if (start > kmax) kmax = start;
					double end = timeline.get(a.end);
					if (end > kmax) kmax = end;
				}
			}

			out.printf("        xmax = %s%n", printDouble(kmax));
			out.printf("        intervals: size = %d%n", entry.getValue().size());

			String type = entry.getKey();
			int nk = 1;
			for(Annot a : entry.getValue()) {
				out.printf("        intervals [%d]:%n", nk);
				if(a.type.equals("time")) {
					double start = timeline.get(a.start);
					double end = timeline.get(a.end);
					out.printf("            xmin = %s%n", printDouble(start));
					out.printf("            xmax = %s%n", printDouble(end));
					out.printf("            text = \"%s\"%n", a.content.replace("\"", "\"\""));
				}
				else if (a.type.equals("ref")){
					double start = (kmax / (entry.getValue().size()) * (nk-1));
					double end = (kmax / (entry.getValue().size()) * (nk));
					out.printf("            xmin = %s%n", printDouble(start));
					out.printf("            xmax = %s%n", printDouble(end));
					out.printf("            text = \"%s\"%n", a.content.replace("\"", "\"\""));
				}
				nk++;
			}
		}
	}

	public void createOutput() {
		out.close();
	}

	public static void usage() {
		System.err.println("Description: TeiToPraat convertit un fichier au format Tei en un fichier au format Praat");
		System.err.println("Usage: TeiToPraat [-options] <file" + Utils.EXT + ">");
		System.err.println("	     :-i nom du fichier ou repertoire où se trouvent les fichiers Tei à convertir (les fichiers ont pour extension " + Utils.EXT);
		System.err.println("	     :-o nom du fichier de sortie au format Praat (.textgrid) ou du repertoire de résultats");
		System.err.println("	     	si cette option n'est pas spécifié, le fichier de sortie aura le même nom que le fichier d'entrée avec l'extension .textgrid;");
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
					TeiToPraat tte = new TeiToPraat(file.getAbsolutePath(), outputDir+outputFileName);
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
			TeiToPraat tte = new TeiToPraat(new File(input).getAbsolutePath(), output);
			System.out.println("Reading " + input);
			tte.createOutput();
			System.out.println("New file created " + output);
		}
	}
}
