/**
 * Conversion d'un fichier TEI en un fichier Chat.
 * @author Myriam Majdoub
 */
package fr.ortolang.teicorpo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
//import java.io.FilenameFilter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.ortolang.teicorpo.TeiFile.Div;

public class TeiToClan extends TeiConverter {

	// Permet d'écrire le fichier de sortie
	private PrintWriter out;
	// Encodage du fichier de sortie
	final static String outputEncoding = "UTF-8";
	// Extension du fichier de sortie
	final static String EXT = ".cha";

	/**
	 * Convertit le fichier TEI donné en argument en un fichier Chat.
	 * 
	 * @param inputName
	 *            Nom du fichier d'entrée (fichier TEI, a donc l'extenstion
	 *            .teiml)
	 * @param outputName
	 *            Nom du fichier de sortie (fichier Chat, a donc l'extenson
	 *            .cha)
	 */
	public TeiToClan(String inputName, String outputName, TierParams optionsTei) {
		super(inputName, outputName, optionsTei);
		if (this.tf == null) return;
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
		// Etapes de conversion
		buildHeader();
		buildText();
	}

	/**
	 * Ecriture de l'en-tête du fichier Chat en fonction du fichier TEI à
	 * convertir
	 */
	public void buildHeader() {
		out.println("@UTF8");
		out.println("@Begin");
		// Language est une ligne obligatoire dans l'en-tête de Chat donc par
		// défaut on mettra français ("fra")
		if (!Utils.isNotEmptyOrNull(tf.language) || tf.language.startsWith("fr")) {
			tf.language = "fra";
		}
		out.println("@Languages:\t" + tf.language);
		// Lignes concernant les locuteurs
		addParticipantProperties();
		// Lignes contenant les informations sur l'enregistrement
		addOtherProperties();
		// Ajout de commentaires pour les informations sur les participants qui
		// ne sont pas relatives à Chat
		partComment();
		// Ecriture de la situation principale
		writeProperty("Situation", tf.trans.sit);
		// Ecriture du time start
		writeProperty("Time start", tf.transInfo.startTime);
	}

	/**
	 * Ajout des lignes concernant les locuteurs: une ligne Participant
	 * contenant une brève description des locuteurs; et une ligne Id par
	 * locuteur contenant dans l'ordre les informations suivantes:
	 * language|corpus|code|age|sex|group|SES|role|education|customField
	 */
	public void addParticipantProperties() {
		ArrayList<TeiParticipant> participants = getParticipants();
		String participantsLine = "";
		String participantsIDS = "";
		for (TeiParticipant p : participants) {
			participantsLine += toString(p.id) + " " + toString(p.name) + " " + getRole(p) + ", ";
			// Chaque locuteur doit obligatoirement avoir un langage, par défaut
			// on mettre le français.
			if (!Utils.isNotEmptyOrNull(p.language)) {
				p.language = "fra";
			}
			// Chaque locuteur doit être rataché à un corpus, par défaut on
			// mettra la valeur "unspecified"
			participantsIDS += "@ID:\t" + toString(p.language).trim() + "|";
			if (!Utils.isNotEmptyOrNull(p.corpus)) {
				p.corpus = "unspecified";
			}
			participantsIDS += toString(p.corpus) + "|";
			participantsIDS += toString(p.id) + "|";
			participantsIDS += toString(p.age) + "|";
			participantsIDS += toString(p.sex) + "|"; // already converted
			participantsIDS += toString(p.adds.get("group")) + "|";
			participantsIDS += toString(p.adds.get("socecStatus")) + "|";
			participantsIDS += getRole(p) + "|";
			participantsIDS += toString(p.adds.get("education")) + "|";
			participantsIDS += toString(p.adds.get("customField")) + "|";
			participantsIDS += "\n";
		}
		try {
			writeProperty("Participants", participantsLine.substring(0, participantsLine.length() - 2));
		} catch (Exception e) {
			writeProperty("Participants", participantsLine);
		}
		out.printf(participantsIDS);
		// Ajouts lignes "birth of" et "birthplace of", doivent immédiatement
		// suivre les lignes @ID
		for (String note : getTransInfo().notes) {
			if (note.toLowerCase().startsWith("birth of")) {
				writeProperty(Utils.getInfoType(note), Utils.getInfo(note));
			} else if (note.toLowerCase().startsWith("birthplace of")) {
				writeProperty(Utils.getInfoType(note), Utils.getInfo(note));
			}
		}
	}

	private String medtype(String mediatype) {
		// System.out.println("XXX: " + mediatype);
		if (mediatype.indexOf("audio") >= 0)
			return "audio";
		else if (mediatype.indexOf("video") >= 0)
			return "video";
		else
			return "";
	}

	private String noext(String medianame) {
		int p = medianame.lastIndexOf('.');
		return (p >= 0) ? medianame.substring(0, p) : medianame;
	}

	/**
	 * Ajout des lignes concernant diverses informations (sur l'enregistrement,
	 * sur les locuteurs...)
	 */
	public void addOtherProperties() {
		// @media
		TransInfo teiHeader = getTransInfo();
		// La spécification du type de média est obligatoire, par défaut on
		// mettra video (les seules valeurs acceptées étant audio et video)
		if (!Utils.isNotEmptyOrNull(teiHeader.mediatype)) {
			teiHeader.mediatype = "video";
		}
		if (Utils.isNotEmptyOrNull(teiHeader.medianame)) {
			writeProperty("Media", noext(teiHeader.medianame) + ", " + medtype(teiHeader.mediatype));
		}
		// @Date
		writeProperty("Date", trsDateToChatDate(teiHeader.date));
		// @Location
		writeProperty("Location", teiHeader.place);
		// @Transcriber
		writeProperty("Transcriber", teiHeader.transcriber);
		// @Time Duration
		writeProperty("Time Duration", teiHeader.timeDuration);
		// Autres info dans notes
		for (String note : teiHeader.notes) {
			if (note.toLowerCase().substring(1).startsWith("number")) {
				writeProperty("Number", Utils.getInfo2(note));
			} else if (note.toLowerCase().substring(1).startsWith("tape location")) {
				writeProperty("Tape Location", Utils.getInfo2(note));
			} else if (note.toLowerCase().substring(1).startsWith("room layout")) {
				writeProperty("Room Layout", Utils.getInfo2(note));
			} else if (note.toLowerCase().substring(1).startsWith("recording quality")) {
				writeProperty("Recording Quality", Utils.getInfo2(note));
			} else if (note.toLowerCase().substring(1).startsWith("time start")) {
				writeProperty("Time Start", Utils.getInfo2(note));
			} else if (note.toLowerCase().substring(1).startsWith("transcription")) {
				writeProperty("Transcription", Utils.getInfo2(note));
			} else if (note.toLowerCase().substring(1).startsWith("warning")) {
				writeProperty("Warning", Utils.getInfo2(note));
			} else if (note.toLowerCase().substring(1).startsWith("interaction type")) {
				writeProperty("Interaction Type", Utils.getInfo2(note));
			} else if (note.toLowerCase().substring(1).startsWith("exceptions")) {
				writeProperty("Exceptions", Utils.getInfo2(note));
			} else if (note.toLowerCase().substring(1).startsWith("options")) {
				writeProperty("Options", Utils.getInfo2(note));
			} else if (note.toLowerCase().substring(1).startsWith("@birth")
					|| note.toLowerCase().substring(1).startsWith("@birthplace")) {
				String noteType = Utils.getInfoType(note);
				writeProperty(noteType.substring(1, noteType.length() - 2), Utils.getInfo2(note));
			} else {
				writeProperty("Comment", note);
			}
		}
		// version: comment
		writeProperty("Comment", teiHeader.version);
	}

	/**
	 * Ajout en tant que commentaires d'informations sur les locuteurs ne
	 * faisant pas partie des informations attendues par Chat.
	 */
	public void partComment() {
		for (TeiParticipant p : getParticipants()) {
			if (!p.adds.isEmpty()) {
				String com = p.id + " ";
				for (Entry<String, String> att : p.adds.entrySet()) {
					com += att.getKey() + ": " + att.getValue() + ", ";
				}
				writeProperty("Comment", com.substring(0, com.length() - 2));
			}
		}
	}

	/**
	 * Ecriture de la partie transcriptions: énoncés + tiers
	 */
	public void buildText() {
		ArrayList<TeiFile.Div> divs = tf.trans.divs;
		boolean eg = false;
		for (Div d : divs) {
			for (AnnotatedUtterance u : d.utterances) {
				// u.print();
				if (Utils.isNotEmptyOrNull(u.type)) {
					String[] splitType = u.type.split("\t");
					// System.out.printf("DIV: %s %s %n", splitType[0],
					// splitType[1]);
					if (splitType.length > 1)
						eg = writeDiv(splitType[0], splitType[1]);
				}
				writeUtterance(u);
				if (eg) {
					out.println("@Eg:");
					eg = false;
				}
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
	public boolean writeDiv(String type, String themeId) {
		if (type.equals("Situation"))
			return false;
		boolean eg = false;
		String theme = Utils.cleanString(tf.transInfo.situations.get(themeId));
		// || type.toLowerCase().startsWith("situation")
		if (type.toLowerCase().startsWith("g") && !theme.toLowerCase().equals(tf.trans.sit.toLowerCase())) {
			out.println("@G:\t" + theme);
		} else if (type.toLowerCase().startsWith("bg")) {
			out.print("@Bg");
			if (Utils.isNotEmptyOrNull(theme)) {
				if (type.endsWith(":")) {
					out.print("\t" + theme);
				} else {
					out.print(":\t" + theme);
				}
			}
			out.println();
		} else if (type.toLowerCase().startsWith("eg")) {
			eg = true;
		} else {
			if (tf.teiDoc.getElementsByTagName("div").getLength() > 1 && Utils.isNotEmptyOrNull(type)) {
				out.println(type + " " + theme);
			}
		}
		return eg;
	}
	
	/**
	 * Ecriture d'une propriété du fichier : lignes qui commencent par le
	 * symbole arobase
	 * 
	 * @param propertyName
	 *            Nom de la propriété
	 * @param propertyContent
	 *            Contenu de la propriété
	 */
	public void writeProperty(String propertyName, String propertyContent) {
		if (Utils.isNotEmptyOrNull(propertyContent)) {
			String[] lines = propertyContent.split("[\\r\\n]+");
			if (lines.length > 0) {
				out.printf("@%s:\t%s%n", propertyName, lines[0].trim());
				for (int i = 1; i < lines.length; i++)
					out.printf("\t%s%n", lines[i].trim());
			}
		}
		/*
		 * else { out.println("@" + propertyName); }
		 */
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
		// System.out.println(speechContent);
		out.print("*" + loc + ":\t" + speechContent);
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
		// début et un temps de fin (Chat ne traite pas les points temporels)
		if (Utils.isNotEmptyOrNull(endTime) && Utils.isNotEmptyOrNull(startTime)) {
			out.print("\u0015" + toMilliseconds(Float.parseFloat(startTime)) + "_"
					+ toMilliseconds(Float.parseFloat(endTime)) + "\u0015");
		}
		out.println();
	}

	/**
	 * Ajout des info additionnelles (hors-tiers)
	 * 
	 * @param u
	 */
	public void writeAddInfo(AnnotatedUtterance u) {
		// Ajout des informations additionnelles présents dans les fichiers
		// chat: lignes commençant par @ hors en-tête
		for (String s : u.coms) {
			String infoType = Utils.getInfoType(s);
			String infoContent = Utils.getInfo(s);
			writeProperty(infoType, infoContent);
		}
	}

	/**
	 * Ecriture des tiers: lignes qui commencent par le signe pourcent %
	 * 
	 * @param tier
	 *            Le tier à écrire, au format : Nom du tier \t Contenu du tier
	 */
	public void writeTier(Annot tier) {
		String type = tier.name;
		String tierContent = tier.content;
		String tierLine = "%" + type + ":\t" + tierContent.replaceAll("\\s+", " ").trim();
		out.println(tierLine);
	}

	/**
	 * Dans Chat, les lignes principales doivent se terminer par un symbole de
	 * fin de ligne (spécifié dans le fichier depfile.cut). Cette méthode
	 * vérifie si c'est bien le cas et si ça ne l'est pas elle rajoute le signe
	 * point (par défaut) à la fin de la ligne à écrire.
	 * 
	 * @param line
	 *            Ligne à renvoyer.
	 * @return
	 */
	public String toChatLine(String line) {
		String patternStr = "(\\+\\.\\.\\.|\\+/\\.|\\+!\\?|\\+//\\.|\\+/\\?|\\+\"/\\.|\\+\"\\.|\\+//\\?|\\+\\.\\.\\?|\\+\\.|\\.|\\?|!)\\s*$";
		Pattern pattern = Pattern.compile(patternStr);
		Matcher matcher = pattern.matcher(line);
		if (!matcher.find()) {
			line += ".";
		}
		return line;
	}

	public void createOutput() {
		if (out != null)
			out.printf("@End");
	}

	public static String convertSpecialCodes(String initial) {
		initial = initial.replaceAll("\\byy(\\s|$)\\b", "yyy ");
		initial = initial.replaceAll("\\bxx(\\s|$)\\b", "xxx ");
		initial = initial.replaceAll("\\bww(\\s|$)\\b", "www ");
		initial = initial.replaceAll("\\*\\*\\*", "xxx");
		return ConventionsToChat.setConv(initial);
	}

	/**
	 * Récupération du role d'un locuteur. Par défaut on lui donnera la valeur
	 * "Unidentified"
	 * 
	 * @param p
	 * @return
	 */
	public static String getRole(TeiParticipant p) {
		String role = "";
		if (Utils.isNotEmptyOrNull(p.role)) {
			role = p.role;
		} else {
			role = "Unidentified";
		}
		return role;
	}

	/**
	 * Conversion de la chaîne de caractère correspondant au sexe du locuteur.
	 * Chat accepte les valeurs "male", "female" et "unknown".
	 * 
	 * @param sex
	 * @return
	 */
	public static String convertSex(String sex) {
		try {
			if (sex.equals("2")) {
				sex = "female";
			} else if (sex.equals("1")) {
				sex = "male";
			} else {
				sex = "unknown";
			}
		} catch (NullPointerException e) {
		}
		return sex;
	}

	// Coversion date au format transcriber (JJMMAA)-> date au formt chat
	// (JJ-MMM-AAAA)
	public static String trsDateToChatDate(String date) {
		try {
			String[] months = new DateFormatSymbols(Locale.ENGLISH).getShortMonths();
			String patternStr = "(\\d\\d)(\\d\\d)(\\d\\d)";
			Pattern pattern = Pattern.compile(patternStr);
			Matcher matcher = pattern.matcher(date);
			if (matcher.find()) {
				date = matcher.group(3) + "-" + months[Integer.parseInt(matcher.group(2)) - 1].toUpperCase() + "-"
						+ convertYear(matcher.group(1));
			}
		} catch (Exception e) {
			return date;
		}
		return date;
	}

	public static void main(String args[]) throws IOException {
		Utils.printVersionMessage();

		String usageString = "Description: TeiToClan convertit un fichier au format TEI en un fichier au format Clan/Chat%nUsage: TeiToClan [-options] <file."
				+ Utils.EXT + ">%n";
		TierParams options = new TierParams();
		// Parcours des arguments
		if (!Utils.processArgs(args, options, usageString, Utils.EXT, EXT, 0))
			return;
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
			} else {
				new File(outputDir).mkdir();
			}

			for (File file : teiFiles) {
				String name = file.getName();
				if (file.isFile() && (name.endsWith(Utils.EXT))) {
					String outputFileName = Utils.basename(file.getName()) + EXT;
					TeiToClan ttc = new TeiToClan(file.getAbsolutePath(), outputDir + outputFileName, options);
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
				System.err.println("\nLe fichier de sortie du programme doit avoir l'extension .cha ");
			}
			TeiToClan ttc = new TeiToClan(new File(input).getAbsolutePath(), output, options);
			if (ttc.tf != null) {
				System.out.println("Reading " + input);
				ttc.createOutput();
				System.out.println("New file created " + output);
			}
		}
	}

	public static void process(TierParams tp) {
		TeiToClan ttc = new TeiToClan(new File(tp.input).getAbsolutePath(), tp.output, tp);
		if (ttc.tf != null) {
			ttc.createOutput();
		} else {
			System.err.printf("Erreur sur %s%n", tp.input);
		}
	}
}

/**
 * Ecriture des utterances
 * 
 * @param u
 *            L'utterance à écrire
public void writeUtterance(AnnotatedUtterance u) {
	String speech;
	 // Chaque utterance a une liste d'énoncé, dans un format spécifique:
	 // start;end__speech
	for (int s=0; s<u.speeches.size(); s++) {
//		System.out.printf("u.speeches: [%s]%n", u.speeches.get(s).toString());
		String start = null;
		String end = null;
		speech = toChatLine(u.speeches.get(s).content).trim();
		speech = speech.replaceAll("\n", "");
		start = u.speeches.get(s).start;
		end = u.speeches.get(s).end;

		// Si le temps de début n'est pas renseigné, on prend le temps de
		// fin de l'énoncé précédent(si présent)
		if (!Utils.isNotEmptyOrNull(start)) {
			if (s<1)
				start = "";
			else
				start = u.speeches.get(s-1).end;
		}

		// Si le temps de fin n'est pas renseigné, on prend le temps de
		// début de l'énoncé suivant(si présent)
		if (!Utils.isNotEmptyOrNull(end)) {
			if (s < u.speeches.size()-1)
				end = u.speeches.get(s+1).start;
			else
				end = "";
		}

		// Si l'énoncé est le premier de la liste de l'utterance, son temps
		// de début est égal au temps de début de l'utterance
		if (s == 0 && !Utils.isNotEmptyOrNull(start)) {
			start = u.start;
		}

		// Si l'énoncé est le dernier de la liste de l'utterance, son temps
		// de fin est égal au temps de fin de l'utterance
		if (s == u.speeches.size()-1 && !Utils.isNotEmptyOrNull(end)) {
			end = u.end;
		}

		// Ecriture de l'énoncé
//		writeSpeech(u.speakerCode, convertSpecialCodes(speech), start, end, optionsOutput.forceEmpty);
		writeSpeech(u.speakerCode, convertSpecialCodes(speech).replaceAll("\\s+", " "), start, end);
	}
	// écriture des tiers
	for (Annot tier : u.tiers) {
		writeTier(tier);
	}
	writeAddInfo(u);
}
*/
