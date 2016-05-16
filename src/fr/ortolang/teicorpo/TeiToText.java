/**
 * Conversion d'un fichier TEI en un fichier SRT.
 * @author Myriam Majdoub
 */
package fr.ortolang.teicorpo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
//import java.io.FilenameFilter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.ortolang.teicorpo.AnnotatedUtterance;
import fr.ortolang.teicorpo.TeiFile.Div;

public class TeiToText extends TeiConverter {

	// Permet d'écrire le fichier de sortie
	private PrintWriter out;
	// Encodage du fichier de sortie
	final static String outputEncoding = "UTF-8";
	// Extension du fichier de sortie
	final static String EXT = ".txt";

	/**
	 * Convertit le fichier TEI donné en argument en un fichier Srt.
	 * 
	 * @param inputName
	 *            Nom du fichier d'entrée (fichier TEI, a donc l'extenstion
	 *            .teiml)
	 * @param outputName
	 *            Nom du fichier de sortie (fichier SRT, a donc l'extenson .txt)
	 */
	public TeiToText(String inputName, String outputName, TierParams optionsTei) {
		super(inputName, outputName, optionsTei);
		if (this.tf == null)
			return;
		optionsOutput = null; // optionsTei;
		outputWriter();
		conversion();
	}

	/**
	 * Ecriture de l'output
	 */
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

	/**
	 * Conversion
	 */
	public void conversion() {
		// System.out.println("Conversion (" +
		// (Params.forceEmpty?"true":"false") + ") (" + Params.partDisplay + ")
		// (" + Params.tierDisplay + ")");
		// Etapes de conversion
		buildHeader();
		buildText();
	}

	/**
	 * Ecriture de l'en-tête du fichier Srt en fonction du fichier TEI à
	 * convertir
	 */
	public void buildHeader() {
		out.println("@Fichier_input:\t" + inputName);
		out.println("@Fichier_output:\t" + outputName);
		out.print(tf.transInfo.toString());
	}

	/**
	 * Ecriture de la partie transcriptions: énoncés + tiers
	 */
	public void buildText() {
		ArrayList<TeiFile.Div> divs = tf.trans.divs;
		for (Div d : divs) {
			for (AnnotatedUtterance u : d.utterances) {
				// u.print();
				if (Utils.isNotEmptyOrNull(u.type)) {
					if (!u.start.isEmpty()) {
						float start = Float.parseFloat(u.start);
						out.printf("%f:%f\t", start, start+1);
						String[] splitType = u.type.split("\t");
						try {
							writeDiv(splitType[0], splitType[1]);
						} catch (ArrayIndexOutOfBoundsException e) {
							out.println(splitType[0]);
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
	 */
	public void writeDiv(String type, String themeId) {
		String theme = Utils.cleanString(tf.transInfo.situations.get(themeId));
		out.println(type + "\t" + theme);
	}

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
			float start = Float.parseFloat(startTime);
			float end = Float.parseFloat(endTime);
			out.printf("%f:%f\t", start, end);
			out.println(loc + "\t" + speechContent);
		} else {
			out.println("\t\t" + loc + "\t" + speechContent);
		}
	}

	/**
	 * Ajout des info additionnelles (hors-tiers)
	 * 
	 * @param u
	 */
	public void writeAddInfo(AnnotatedUtterance u) {
		if (optionsOutput != null) {
			if (optionsOutput.isDontDisplay("com"))
				return;
			if (!optionsOutput.isDoDisplay("com"))
				return;
		}
		// Ajout des informations additionnelles présents dans les fichiers txt
		for (String s : u.coms) {
			String infoType = Utils.getInfoType(s);
			String infoContent = Utils.getInfo(s);
			out.println("\t\t" + infoType + "\t" + infoContent);
		}
	}

	/**
	 * Ecriture des tiers: lignes qui commencent par le signe pourcent %
	 * 
	 * @param tier
	 *            Le tier à écrire, au format : Nom du tier \t Contenu du tier
	 */
	public void writeTier(Annot tier) {
		if (optionsOutput != null) {
			if (optionsOutput.isDontDisplay(tier.name))
				return;
			if (!optionsOutput.isDoDisplay(tier.name))
				return;
		}
		String type = tier.name;
		String tierContent = tier.content;
		String tierLine = "\t\t\t%" + type + "\t" + tierContent.trim();
		out.println(tierLine);
	}

	public void createOutput() {
	}

	public static void main(String args[]) throws IOException {
		String usage = "Description: TeiToText convertit un fichier au format TEI en un fichier au format Text (txt)%nUsage: TeiToText [-options] <file."
				+ Utils.EXT + ">%n";
		TierParams options = new TierParams();
		// Parcours des arguments
		if (!Utils.processArgs(args, options, usage, Utils.EXT, EXT, 0))
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
					TeiToText ttc = new TeiToText(file.getAbsolutePath(), outputDir + outputFileName, options);
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
				System.err.println("\nLe fichier de sortie du programme doit avoir l'extension .txt ");
			}
			TeiToText ttc = new TeiToText(new File(input).getAbsolutePath(), output, options);
			if (ttc.tf != null) {
				System.out.println("Reading " + input);
				ttc.createOutput();
				System.out.println("New file created " + output);
			}
		}

	}

}
