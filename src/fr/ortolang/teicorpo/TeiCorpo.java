/**
 * TeiCorpo permet de convertir des fichiers de transcriptio au format TEI et inversement
 * @author Christophe Parisse
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
import java.util.List;
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

public class TeiCorpo {
	public static boolean containsExtension(String fn, String ext) {
		if (fn.endsWith(ext)) {
			return true;
		}
		return false;
	}
	
	public static void process(String extIn, String extOut, String fileIn, String fileOut, TierParams tp) {
		tp.input = fileIn;
		tp.output = fileOut;
		if (extIn.equals("cha") && extOut.equals(Utils.EXT)) {
			ClanToTei.process(tp);
		} else if (extIn.equals(Utils.EXT) && extOut.equals("cha")) {
			TeiToClan.process(tp);
		}
	}

	public static String inputExtensions(String inputFormat) {
		String lcInputFormat = inputFormat.toLowerCase();
		switch(lcInputFormat) {
			case "cha":
			case "chat":
			case "clan":
				return ".cha";
			case "trs":
			case "transcriber":
				return ".trs";
			case "eaf":
			case "elan":
				return ".eaf";
			case "textgrid":
			case "praat":
				return ".texgrid";
			case "teicorpo.xml":
			case "teicorpo":
			case "tei":
				return Utils.EXT;
			case "trjs":
				return ".trjs";
			case "xml":
				return ".xml";
		}
		return "*";
	}

	public static String outputExtensions(String outputFormat) {
		String lcOutputFormat = outputFormat.toLowerCase();
		switch(lcOutputFormat) {
			case "cha":
			case "chat":
			case "clan":
				return "cha";
			case "trs":
			case "transcriber":
				return "trs";
			case "eaf":
			case "elan":
				return "eaf";
			case "textgrid":
			case "praat":
				return "texgrid";
			case "teicorpo.xml":
			case "teicorpo":
			case "tei":
			case "trjs":
				return "teicorpo.xml";
		}
		return "*";
	}

	public static void usage() {
		System.err.println("Description: TeiCorpo convertit un fichier d'un format à un autre");
		System.err.println("Formats source: TEI_CORPO (.tei_corpo.xml, .trjs, .xml), Clan (.cha), Elan (.eaf), Praat (.textgrid), Transcriber (.trs)");
		System.err.println("Usage: TeiCorpo [-options] [-from FMT1] [-to FMT2] -i filein [-o fileout]");
		System.err.println("     :-i nom du fichier ou repertoire où se trouvent les fichiers input");
		System.err.println("     :-o nom du fichier de sortie ou du repertoire de résultats");
		System.err.println("     :-to format des fichiers input");
		System.err.println("     :-from format des fichiers output");
		System.err.println("si les options -from et -to ne sont pas précisés, les formats sont devinés à partir des extensions de fichier");
		System.err.println("toutes les options non spécifiées ci-dessous sont transmises au sous-programme de conversion");
		System.err.println("     :-usage ou -help = affichage de ce message");
	}

	public static void main(String[] args) throws Exception {
		Utils.printVersionMessage();

		String usageString = "Description: TeiCorpo convertit un fichier d'un format à l'autre%nUsage: TeiCorpo [-options] <file>%n"
				+ "Formats possibles: TEI_CORPO, Clan, Elan, Praat, Transcriber";
		TierParams options = new TierParams();
		// Parcours des arguments
		if (!Utils.processArgs(args, options, usageString, Utils.EXT, ".cha;.trs;.eaf;.textgrid", 0))
			return;
		String input = options.input;
		String output = options.output;
		
		String ExtIn = inputExtensions(options.inputFormat);
		String ExtOut = outputExtensions(options.outputFormat);

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
			} else {
				new File(outputDir).mkdir();
			}

			for (File file : teiFiles) {
				String name = file.getName();
				if (file.isFile() && containsExtension(name, ExtIn)) {
					String outputFileName = Utils.basename(file.getName()) + ExtOut;
					process(ExtIn, ExtOut, file.getAbsolutePath(), outputDir + outputFileName, options);
				} else if (file.isDirectory()) {
					args[0] = "-i";
					args[1] = file.getAbsolutePath();
					main(args);
				}
			}
		} else {
			if (output == null) {
				output = Utils.basename(input) + ExtOut;
			} else if (new File(output).isDirectory()) {
				if (output.endsWith("/")) {
					output = output + Utils.basename(input) + ExtOut;
				} else {
					output = output + "/" + Utils.basename(input) + ExtOut;
				}
			}

			if (!Utils.validFileFormat(output, ExtOut)) {
				System.err.println("\nLe fichier de sortie du programme doit avoir l'extension " + ExtOut);
			}
			process(ExtIn, ExtOut, new File(input).getAbsolutePath(), output, options);
		}
	}
}
