package fr.ortolang.teicorpo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;

public class TeiToPraat extends GenericMain {

	TeiToPartition ttp = null;

	// Permet d'écrire le fichier de sortie
	private PrintWriter out;
	// Encodage du fichier de sortie
	final static String outputEncoding = "UTF-8";
	// Extension du fichier de sortie: .textgrid
	final static String EXT = ".textgrid";

	// Nom du fichier teiml à convertir
	String inputName;
	// Nom du fichier de sortie
	String outputName;

	// Document TEI à lire
	public Document teiDoc;
	// acces Xpath
	public XPathFactory xPathfactory;
	public XPath xpath;

	// Validation du document Tei par la dtd
	boolean validation = false;

	// Constructeur à partir du nom du fichier TEI et du nom du fichier de
	// sortie au format Elan
	public void transform(String inputName, String outputName, TierParams optionsTei) {
		if (optionsTei == null) optionsTei = new TierParams();
		ttp = new TeiToPartition();
		DocumentBuilderFactory factory = null;
		try {
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
			ttp.init(xpath, teiDoc, optionsTei);
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.inputName = inputName;
		this.outputName = outputName;
		outputWriter();
		conversion();
	}

	// Ecriture du fichier de sortie
	public void outputWriter() {
		out = null;
		try {
			FileOutputStream of = new FileOutputStream(outputName);
			OutputStreamWriter outWriter = new OutputStreamWriter(of, outputEncoding);
			out = new PrintWriter(outWriter, true);
		} catch (Exception e) {
			out = new PrintWriter(System.out, true);
		}
	}

	// Conversion du fichier TEI vers Elan
	public void conversion() {
		// Construction de l'en-tête
		buildHeader();
		// Construction des tiers
		buildTiers();
	}

	// Information contenues dans l'élément annotation_doc et dans l'élément
	// header
	public void buildHeader() {
		out.println("File type = \"ooTextFile\"");
		out.println("Object class = \"TextGrid\"");
		out.println("");
		out.println("xmin = 0");
		out.printf("xmax = %s%n", printDouble(ttp.timeline.xmaxTime));
		out.println("tiers? <exists>");
		out.printf("size = %d%n", ttp.tiers.size());
	}

	private String printDouble(double value) {
		if (value <= 0.0)
			return "0";
		double intpart = Math.floor(value);
		BigDecimal bd = new BigDecimal(value);
		BigDecimal bdintpart = new BigDecimal(intpart);
		bd = bd.setScale(15, RoundingMode.HALF_UP);
		String decpart = (bd.subtract(bdintpart)).toString().substring(2);
		if (decpart.equals("-15"))
			decpart = "000000000000000";
		return bdintpart.toString() + "." + decpart;
	}

	// Tiers...
	void buildTiers() {
		int nth = 1;
		out.println("item []:");
		for (Map.Entry<String, ArrayList<Annot>> entry : ttp.tiers.entrySet()) {
			out.printf("    item [%d]:%n", nth++);
			out.println("        class = \"IntervalTier\"");
			out.printf("        name = \"%s\"%n", entry.getKey());
			out.println("        xmin = 0");

			double kmax = 0.0;
			for (Annot a : entry.getValue()) {
				if (a.timereftype.equals("time")) {
					double start = Double.parseDouble(a.start);
					if (start > kmax)
						kmax = start;
					double end = Double.parseDouble(a.end);
					if (end > kmax)
						kmax = end;
				}
			}

			out.printf("        xmax = %s%n", printDouble(kmax));
			out.printf("        intervals: size = %d%n", entry.getValue().size());

			int nk = 1;
			for (Annot a : entry.getValue()) {
				out.printf("        intervals [%d]:%n", nk);
				double start = Double.parseDouble(a.start);
				double end = Double.parseDouble(a.end);
				out.printf("            xmin = %s%n", printDouble(start));
				out.printf("            xmax = %s%n", printDouble(end));
				String str = a.getContent(ttp.optionsOutput.rawLine);
				String strNorm = NormalizeSpeech.parseText(str, ttp.originalFormat(), ttp.optionsOutput);
				out.printf("            text = \"%s\"%n", strNorm);
				nk++;
			}
		}
	}

	public void createOutput() {
		out.close();
	}

	public static void main(String args[]) throws IOException {
		TierParams.printVersionMessage();
		String usage = "Description: TeiToPraat convertit un fichier au format Tei en un fichier au format Praat%nUsage: TeiToPraat [-options] <file"
				+ Utils.EXT + ">%n";
		TeiToPraat tte = new TeiToPraat();
		tte.mainCommand(args, Utils.EXT, Utils.EXT_PUBLISH + EXT, usage, 0);
	}

	@Override
	public void mainProcess(String input, String output, TierParams options) {
		options.target = ".texgrid";
		transform(input, output, options);
//		System.out.println("Reading " + input);
		createOutput();
//		System.out.println("New file created " + output);
	}
}
