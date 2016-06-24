/**
 * @author Christophe Parisse
 * TeiSyntacticAnalysis: faire l'analyse syntaxique sur la ligne principale
 */

package fr.ortolang.teicorpo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import fr.ortolang.teicorpo.TeiFile.Div;

public class TeiTreeTagger {

	final static String TT_EXT = "_ttg";
	// Document TEI à lire
	public Document teiDoc;
	// acces Xpath
	public XPathFactory xPathfactory;
	public XPath xpath;
	// paramètres
	public TierParams optionsOutput;
	// Nom du fichier teiml à convertir
	String inputName;
	// Nom du fichier de sortie
	String outputName;
	// Validation du document Tei par la dtd
	boolean validation = false;
	// racine du fichier teiDoc
	Element root;
	
	// resultat
	boolean ok;

	public TeiTreeTagger(String pInputName, String pOutputName, TierParams optionsTei) {
		optionsOutput = optionsTei;
		inputName = pInputName;
		outputName = pOutputName;
		DocumentBuilderFactory factory = null;
		root = null;
		ok = false;

		File inputFile = new File(inputName);
		if (!inputFile.exists()) {
			System.err.printf("%s n'existe pas: pas de traitement%n", inputName);
			return;
		}

		try {
			factory = DocumentBuilderFactory.newInstance();
			Utils.setDTDvalidation(factory, validation);
			DocumentBuilder builder = factory.newDocumentBuilder();
			teiDoc = builder.parse(inputFile);
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
		} catch (Exception e) {
			e.printStackTrace();
		}
		process();
		createOutput();
	}
	
	public String getTreeTaggerLocation() {
		String os = System.getProperty("os.name");
		if (os.equals("linux") || os.equals("Mac OS X")) {
			String p = ExternalCommand.getLocation("tree-tagger","TREE_TAGGER");
			if (p != null) return p;
			p = ExternalCommand.getLocation("bin/tree-tagger","TREE_TAGGER");
			if (p != null) return p;
			System.err.println("Cannot find tree-tagger program");
			return null;
		} else {
			String p = ExternalCommand.getLocation("tree-tagger.exe","TREE_TAGGER");
			if (p != null) return p;
			p = ExternalCommand.getLocation("bin/tree-tagger.exe","TREE_TAGGER");
			if (p != null) return p;
			System.err.println("Cannot find tree-tagger.exe program");
			return null;
		}
	}

	private String getTreeTaggerModel() {
		String p = ExternalCommand.getLocation("spoken-french.par","TREE_TAGGER");
		if (p != null) return p;
		p = ExternalCommand.getLocation("model/spoken-french.par","TREE_TAGGER");
		if (p != null) return p;
		System.err.println("Cannot find spoken-french.par model");
		return null;
	}

	// Conversion du fichier teiml
	public boolean process() {
		Tokenizer.init("fr", null);
		int numAU = 0;
		for (String cmd: optionsOutput.commands) {
			if (cmd.equals("replace"))
				continue;
			if (cmd.startsWith("model=")) {
				String modelfile = cmd.substring(6);
				System.out.printf("Utilisation du modèle : %s%n", modelfile);
			}
		}
		PrintWriter out = null;
		String outputNameTemp = outputName + "_tmp.vrt";
		try {
			FileOutputStream of = new FileOutputStream(outputNameTemp);
			OutputStreamWriter outWriter = new OutputStreamWriter(of, "UTF-8");
			out = new PrintWriter(outWriter, true);
		} catch (Exception e) {
			return false;
		}
		NodeList aBlocks = teiDoc.getElementsByTagName(Utils.ANNOTATIONBLOC);
		if (aBlocks != null && aBlocks.getLength() > 0) {
			for (int i=0; i < aBlocks.getLength(); i++) {
				Element eAU = (Element) aBlocks.item(i);
				AnnotatedUtterance au = new AnnotatedUtterance();
				au.process(eAU, null, null, null, false);
				// mettre en place l'élément qui recevra l'analyse syntaxique.
				Element syntaxGrp = teiDoc.createElement("spanGrp");
				syntaxGrp.setAttribute("type", "pos");
				numAU++;
				syntaxGrp.setAttribute("id", "pos" + numAU);
				syntaxGrp.setIdAttribute("id", true);
				eAU.appendChild(syntaxGrp);
				// préparer le fichier d'analyse syntaxique
				out.printf("<%s>%n", "pos" + numAU);
				// decouper au.cleanedSpeech
				ArrayList<String> p = Tokenizer.splitTextTT(
						optionsOutput.clearChatFormat
						? ConventionsToChat.clearChatFormat(au.cleanedSpeech)
						: au.cleanedSpeech);
				for (int ti = 0; ti < p.size(); ti++)
					out.printf("%s%n", p.get(ti));
			}
		}
		out.close();
		// démarrer l'analyse
		String outputNameResults = outputName + "_tmp.conll";
        String[] commande = { getTreeTaggerLocation(), "-token",
        		"-lemma", "-sgml", getTreeTaggerModel(),
        		outputNameTemp };
        if (commande[0] == null || commande[4] == null) {
        	// cannot parse
        	System.out.println("tree-tagger files not found: stop.");
        	return false;
        }
        ExternalCommand.command(commande, outputNameResults);
		// récupérer les résultats
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(outputNameResults), "UTF-8"));
			String line = reader.readLine();
			String lastID = "";
			TaggedUtterance tu = new TaggedUtterance();
			while (line != null) {
				line = line.trim();
				if (line.startsWith("<") && line.endsWith(">")) {
					if (!lastID.isEmpty() && !flush(lastID, tu)) return true;
					lastID = line.substring(1, line.length()-1);
					tu.reset();
				} else {
					String[] wcl = line.split("\t");
					if (wcl.length != 3) {
						System.err.println("not enough elements: " + line);
						return false;
					}
					tu.add(wcl);
				}
				line = reader.readLine();
			}
			if (!lastID.isEmpty())
				flush(lastID, tu);
			if (reader != null)
				reader.close();
		} catch (FileNotFoundException fnfe) {
			System.err.println("Erreur fichier : " + outputNameResults + " indisponible");
			return false;
		} catch (IOException ioe) {
			System.err.println("Erreur sur fichier : " + outputNameResults);
			ioe.printStackTrace();
			return false;
		}
		return true;
	}

	private boolean flush(String lastID, TaggedUtterance tu) {
		Element spG = teiDoc.getElementById(lastID);
		if (spG == null) {
			System.err.println("cannot find element: " + lastID);
			return false;
		}
		Element elt = tu.createSpan(teiDoc);
		spG.appendChild(elt);
		spG.removeAttribute("id");
		return true;
	}

	// Création du fichier de sortie
	public void createOutput() {
		File itest = new File(inputName);
		File otest = new File(outputName);
		String itname;
		String otname;
		try {
			itname = itest.getCanonicalPath();
			otname = otest.getCanonicalPath();
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		
		if (itname.equals(otname) && !optionsOutput.commands.contains("replace")) {
			System.err.println("Le fichier sortie est le même que le fichier entrée: utiliser le paramètre -c replace pour remplacer le fichier");
			return;
		}

		Source source = new DOMSource(teiDoc);
		Result resultat = new StreamResult(outputName);

		try {
			TransformerFactory fabrique2 = TransformerFactory.newInstance();
			Transformer transformer = fabrique2.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, Utils.teiCorpoDtd());
			transformer.transform(source, resultat);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * remplacement d'un fichier par lui-même
	 * option -c replace --> ne pas utiliser TT_EXT et mettre sameFile à vrai.
	 */

	// Programme principal
	public static void main(String args[]) throws IOException {
		Utils.printVersionMessage();

		String usageString = "Description: TeiTreeTagger permet d'appliquer le programme TreeTagger sur un fichier Tei.%nUsage: TeiTreeTagger -c command [-options] <"
				+ Utils.EXT + ">%n";
		TierParams options = new TierParams();
		// Parcours des arguments
		if (!Utils.processArgs(args, options, usageString, Utils.EXT, Utils.EXT, 0))
			return;
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
				System.err.println("Attention répertoires entrée et sortie identiques");
			} else {
				outputDir = output;
				if (!outputDir.endsWith("/")) {
					outputDir = output + "/";
				}
				File fdirout = new File(outputDir);
				// Permet d'avoir le nom complet du fichier (chemin absolu, sans
				// signes
				// spéciaux(. et .. par ex))
				output = fdirout.getCanonicalPath();
				if (input.equals(output)) {
					System.err.println("Attention répertoires entrée et sortie identiques");
				}
			}
			
			File outFile = new File(outputDir);
			if (outFile.exists()) {
				if (!outFile.isDirectory()) {
					System.out.println("\n Erreur :" + output
							+ " est un fichier, vous devez spécifier un nom de dossier pour le stockage des résultats. \n");
					System.exit(1);
				}
			} else {
				new File(outputDir).mkdir();
			}

			for (File file : teiFiles) {
				String name = file.getName();
				if (file.isFile() && (name.endsWith(Utils.EXT))) {
					String outputFileName;
					if (options.commands.contains("replace"))
						outputFileName = name;
					else
						outputFileName = Utils.basename(input) + TT_EXT + Utils.EXT;
					TeiTreeTagger ttt = new TeiTreeTagger(file.getAbsolutePath(), outputDir + outputFileName, options);
					if (ttt.ok) {
						System.out.println(outputDir + outputFileName);
						ttt.createOutput();
					}
				} else if (file.isDirectory()) {
					args[0] = "-i";
					args[1] = file.getAbsolutePath();
					main(args);
				}
			}
		}

		else {
			if (!(Utils.validFileFormat(input, Utils.EXT))) {
				System.err.println("Le fichier d'entrée du programme doit avoir l'extension " + Utils.EXT);
				return;
			}

			if (output == null) {
				if (options.commands.contains("replace"))
					output = input;
				else
					output = Utils.basename(input) + TT_EXT + Utils.EXT;
			} else if (new File(output).isDirectory()) {
				if (output.endsWith("/")) {
					if (options.commands.contains("replace"))
						output = output + input;
					else
						output = output + Utils.basename(input) + TT_EXT + Utils.EXT;
				} else {
					if (options.commands.contains("replace"))
						output = output + "/" + input;
					else
						output = output + "/" + Utils.basename(input) + TT_EXT + Utils.EXT;
				}
			}

			System.out.println("Reading " + input);
			TeiTreeTagger ttt = new TeiTreeTagger(new File(input).getAbsolutePath(), output, options);
			if (ttt.ok) {
				ttt.createOutput();
				System.out.println("File modified " + output);
			}
		}
	}
}
