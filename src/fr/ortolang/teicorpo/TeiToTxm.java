/**
 * Conversion d'un fichier TEI en un fichier TXM.
 * @author Christophe Parisse
 */
package fr.ortolang.teicorpo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
//import java.io.FilenameFilter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import fr.ortolang.teicorpo.AnnotatedUtterance;
import fr.ortolang.teicorpo.TeiFile.Div;

public class TeiToTxm extends TeiConverter {

	// Document Txm à écrire
	public Document txmDoc;
	// Extension du fichier de sortie
	final static String EXT = ".txm.xml";
	
	Element txm; // root of document
	Element head; // put all utterances inside text
	String typeDiv;

	/**
	 * Convertit le fichier TEI donné en argument en un fichier Txm.
	 * 
	 * @param inputName
	 *            Nom du fichier d'entrée (fichier TEI, a donc l'extenstion
	 *            .teiml)
	 * @param outputName
	 *            Nom du fichier de sortie (fichier TXM, a donc l'extenson .txm.xml)
	 */
	public TeiToTxm(String inputName, String outputName, TierParams optionsTei) {
		super(inputName, outputName, optionsTei);
		if (this.tf == null)
			return;
		typeDiv = "";
		outputWriter();
		conversion();
		createOutput();
	}

	@Override
	public void outputWriter() {
		txmDoc = null;
		DocumentBuilderFactory factory = null;
		try {
			factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			txmDoc = builder.newDocument();
			txm = txmDoc.createElement("txm");
			txmDoc.appendChild(txm);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void createOutput() {
		createFile(outputName);
	}

	public void createFile(String outputFileName) {
		Source source = new DOMSource(this.txmDoc);
		Result resultat = new StreamResult(outputFileName);
		try {
			// Configuration du transformer 
			TransformerFactory fabrique2 = TransformerFactory.newInstance();
			Transformer transformer = fabrique2.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			// Transformation
			transformer.transform(source, resultat);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Conversion
	 */
	public void conversion() {
		// Etapes de conversion
		buildHeader();
		buildText();
	}

	/**
	 * Ecriture de l'en-tête du fichier Srt en fonction du fichier TEI à
	 * convertir
	 */
	public void buildHeader() {
		txm.setAttribute("file", inputName);
		head = txmDoc.createElement("text");
		txm.appendChild(head);
		for (Map.Entry<String, String> entry : optionsOutput.tv.entrySet()) {
		    String key = entry.getKey();
		    String value = entry.getValue();
		    Element elt = txmDoc.createElement("div");
		    elt.setAttribute(key, value.replaceAll("\\s+", "_"));
		    head.appendChild(elt);
		    head = elt;
			// out.printf("<%s=%s>%n", key, value.replaceAll("\\s+", "_"));
		}
	}

	/**
	 * Ecriture de la partie transcriptions: énoncés + tiers
	 */
	public void buildText() {
		ArrayList<TeiFile.Div> divs = tf.trans.divs;
		for (Div d : divs) {
			// System.out.println("DIV: " + d.type + " <" + d.theme + ">");
			/*
			if (d.type.toLowerCase().equals("bg") || d.type.toLowerCase().equals("g")) {
				typeDiv = d.theme;
			} else {
				typeDiv = "";
			}
			*/
			for (AnnotatedUtterance u : d.utterances) {
				if (u.type != null) {
					String[] splitType = u.type.split("\t");
					if (splitType != null && splitType.length >= 2) {
						if (splitType[0].toLowerCase().equals("bg") || splitType[0].toLowerCase().equals("g")) {
							String theme = Utils.cleanString(tf.transInfo.situations.get(splitType[1]));
							typeDiv = theme;
						}
					}
				}
				writeUtterance(u);
			}
		}
	}

	/**
	 * Ecriture des div: si c'est un div englobant: G, si c'est un sous-div:
	 * Bg/Eg
	 * 
	 * @param type
	 *            le type de div à écrire
	 * @param themeId
	 *            le theme du div à écrire
	public void writeDiv(String type, String themeId) {
		String theme = Utils.cleanString(tf.transInfo.situations.get(themeId));
		System.out.println("DIV" + type + "\t" + theme);
	}
	 */

	/**
	 * Ecriture d'un énonce: lignes qui commencent par le symbole étoile *
	 * 
	 * @param loc
	 *            Locuteur
	 * @param speechContent
	 *            Contenu de l'énoncé
	 * @param startTime
	 *            Temps de début de l'énoncé
	 * @param endTime
	 *            Temps de fin de l'énoncé
	 */
	public void writeSpeech(String loc, String speechContent, String startTime, String endTime) {
		if (optionsOutput != null) {
			if (optionsOutput.isDontDisplay(loc))
				return;
			if (!optionsOutput.isDoDisplay(loc))
				return;
		}
		// System.out.println(loc + ' ' + startTime + ' ' + endTime +' ' +
		// speechContent);
		// Si le temps de début n'est pas renseigné, on mettra par défaut le
		// temps de fin (s'il est renseigné) moins une seconde.
		if (!Utils.isNotEmptyOrNull(startTime)) {
			if (Utils.isNotEmptyOrNull(endTime)) {
				float start = Float.parseFloat(endTime) - 1;
				startTime = Float.toString(start);
			}
		}
		// Si le temps de fin n'est pas renseigné, on mettra par défaut le temps
		// de début (s'il est renseigné) plus une seconde.
		else if (!Utils.isNotEmptyOrNull(endTime)) {
			if (Utils.isNotEmptyOrNull(startTime)) {
				float end = Float.parseFloat(startTime) + 1;
				endTime = Float.toString(end);
			}
		}

		// On ajoute les informations temporelles seulement si on a un temps de
		// début et un temps de fin
		if (Utils.isNotEmptyOrNull(endTime) && Utils.isNotEmptyOrNull(startTime)) {
			Element u = txmDoc.createElement("u");
			u.setAttribute("who", loc);
			u.setAttribute("start", Double.toString(Double.parseDouble(startTime)));
			u.setAttribute("end", Double.toString(Double.parseDouble(endTime)));
			generateU(u, tf.optionsOutput.cleanLine?ConventionsToChat.chatToText(speechContent):speechContent, loc);
			// u.setTextContent(speechContent);
			head.appendChild(u);
		} else {
			Element u = txmDoc.createElement("u");
			u.setAttribute("who", loc);
			u.setAttribute("start", "");
			u.setAttribute("end", "");
			generateU(u, tf.optionsOutput.cleanLine?ConventionsToChat.chatToText(speechContent):speechContent, loc);
			// u.setTextContent(speechContent);
			head.appendChild(u);
		}
	}

	void generateU(Element u, String speechContent, String loc) {
		String[] s = speechContent.split("\\s+");
		for (String w: s) {
			Element we = txmDoc.createElement("w");
			we.setTextContent(w);
			if (!loc.isEmpty())
				we.setAttribute("loc", loc);
			for (Map.Entry<String, String> entry : optionsOutput.tv.entrySet()) {
			    String key = entry.getKey();
			    String value = entry.getValue();
				we.setAttribute(key, value);
			}
			if (!typeDiv.isEmpty())
				we.setAttribute("div", typeDiv);
			u.appendChild(we);
		}
	}

	/**
	 * Ecriture des tiers: lignes qui commencent par le signe pourcent %
	 * 
	 * @param tier
	 *            Le tier à écrire, au format : Nom du tier \t Contenu du tier
	 * Pas pour l'instant dans txm
	public void writeTier(Annot tier) {
		if (optionsOutput != null) {
			if (optionsOutput.isDontDisplay(tier.name))
				return;
			if (!optionsOutput.isDoDisplay(tier.name))
				return;
		}
		String type = tier.name;
		String tierContent = tier.content;
		String tierLine = "%" + type + ": " + tierContent.trim();
//		out.println(tierLine);
	}
	*/
	
	public static void main(String args[]) throws IOException {
		String usage = "Description: TeiToTxm convertit un fichier au format TEI en un fichier au format Txm%nUsage: TeiToTxm [-options] <file."
				+ Utils.EXT + ">%n";
		TierParams options = new TierParams();
		// Parcours des arguments
		if (!Utils.processArgs(args, options, usage, Utils.EXT, EXT, Utils.styleLexicoTxm))
			System.exit(1);
		String input = options.input;
		String output = options.output;

		File f = new File(input);

		// Permet d'avoir le nom complet du fichier (chemin absolu, sans
		// raccourcis spéciaux)
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
					String outputFileName = Utils.basename(file.getName()) + EXT;
					TeiToTxm ttc = new TeiToTxm(file.getAbsolutePath(), outputDir + outputFileName, options);
					System.out.println(outputDir + outputFileName);
					ttc.createOutput();
				} else if (file.isDirectory()) {
					args[0] = "-i";
					args[1] = file.getAbsolutePath();
					main(args);
				}
			}
		} else {
			if (output == null) {
				output = Utils.basename(input) + EXT;
			} else if (new File(output).isDirectory()) {
				if (output.endsWith("/")) {
					output = output + Utils.basename(input) + EXT;
				} else {
					output = output + "/" + Utils.basename(input) + EXT;
				}
			}

			if (!Utils.validFileFormat(output, EXT)) {
				System.err.println("\nLe fichier de sortie du programme doit avoir l'extension .txm.xml ");
			}
			TeiToTxm ttc = new TeiToTxm(new File(input).getAbsolutePath(), output, options);
			if (ttc.tf != null) {
				System.out.println("Reading " + input);
				ttc.createOutput();
				System.out.println("New file created " + output);
			}
		}

	}
}
