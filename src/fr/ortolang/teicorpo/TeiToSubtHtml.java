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
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.ortolang.teicorpo.TeiFile.AnnotatedUtterance;
import fr.ortolang.teicorpo.TeiFile.Div;
import fr.ortolang.teicorpo.TeiFile.Participant;

public class TeiToSubtHtml extends TeiConverter{

	//Permet d'écrire le fichier de sortie
	private PrintWriter out;
	//Encodage du fichier de sortie
	final static String outputEncoding = "UTF-8";
	//Extension du fichier de sortie
	final static String EXT = ".subt.html";

	/**
	 * Convertit le fichier TEI donné en argument en un fichier Srt.
	 * @param inputName	Nom du fichier d'entrée (fichier TEI, a donc l'extenstion .teiml)
	 * @param outputName	Nom du fichier de sortie (fichier SRT, a donc l'extenson .srt)
	 */
	public TeiToSubtHtml(String inputName, String outputName) {
		super(inputName, outputName);
		outputWriter();
		conversion();
	}

	/**
	 * Ecriture de l'output
	 */
	public void outputWriter(){
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

	/**
	 * Conversion
	 */
	public void conversion() {
		//System.out.println("Conversion (" + (Params.forceEmpty?"true":"false") + ") (" + Params.partDisplay + ") (" + Params.tierDisplay + ")");
		//Etapes de conversion
	    // Remove the extension.
		// System.out.println(tf.transInfo.recordName);
		String filename;
		String inputFileName = new File(inputName).getName();
	    int extensionIndex = inputFileName.lastIndexOf(".");
	    if (extensionIndex == -1)
	        filename = inputFileName;
	    else
	    	filename = inputFileName.substring(0, extensionIndex);
		if (filename.endsWith("-240p"))
			filename = filename.substring(0, filename.length()-5);
		else if (filename.endsWith("-480p"))
			filename = filename.substring(0, filename.length()-5);
		else if (filename.endsWith("-480p"))
			filename = filename.substring(0, filename.length()-5);
		else if (filename.endsWith("-720p"))
			filename = filename.substring(0, filename.length()-5);
		else if (filename.endsWith("-1020p"))
			filename = filename.substring(0, filename.length()-6);
		buildHeader(inputName, filename);
		buildText();
        /* terminate file */
        out.println( "</p></div></div></body></html>" );
       /* end of terminate file */
	}

	/**
	 * ecriture de l'entete des fichier html en fonction du fichier TEI à convertir
	 */
	public void buildHeader(String title, String media) {
        String cssLayout = "/tools/subt/layout.css";
        String jsLocation = "/tools/subt/timesheets.js";

        /* initialise head file */
        out.println( "<html xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:toc=\"http://www.tei-c.org/ns/teioc\" lang=\"en\">" );
        out.println( "<head>" );
        out.println( "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">" );
        out.println( "<meta charset=\"utf-8\">" );
        out.printf( "<title>%s</title>%n", title );
        out.printf( "<link rel=\"stylesheet\" type=\"text/css\" href=\"%s\">%n", cssLayout );
        out.println( "<script type=\"text/javascript\" src=\"" + jsLocation + "\"></script>" );
        out.println( "<script type=\"text/javascript\">" );
        out.println( "EVENTS.onSMILReady(function() {" );
        out.println( "var mediaContainer  = document.getElementById(\"media\");" );
        out.println( "var mediaController = document.getElementById(\"mediaController\");" );
        out.println( "var mediaPlayer = mediaContainer.timing.mediaSyncNode;" );
        out.println( "if (mediaPlayer && mediaPlayer.mediaAPI) {" );
        out.println( "// the video is displayed in a Flash/Silverlight element" );
        out.println( "mediaPlayer = mediaPlayer.mediaAPI;" );
        out.println( "mediaContainer.onclick = function() { // single click = play/pause" );
        out.println( "if (mediaPlayer.paused)" );
        out.println( "mediaPlayer.play();" );
        out.println( "else" );
        out.println( "mediaPlayer.pause(); };" );
        out.println( "mediaContainer.ondblclick = function() { // double click = restart" );
        out.println( "mediaPlayer.setCurrentTime(0);" );
        out.println( "mediaPlayer.play(); }; }" );
        out.println( "else {" );
        out.println( "// the video is displayed in a native HTML element" );
        out.println( "mediaController.style.display = \"none\"; } });" );
        out.println( "window.addEventListener('blur', function() {" );
        out.println( "	//alert('lost focus');" );
        out.println( "	var mediaContainer  = document.getElementById(\"media\");" );
        out.println( "	var mediaController = document.getElementById(\"mediaController\");" );
        out.println( "	var mediaPlayer = mediaContainer.timing.mediaSyncNode;" );
        out.println( "	mediaPlayer.pause();" );
        out.println( "});" );
        out.println( "</script>" );
        out.println( "</head>" );
        out.println( "<body>" );
        out.println( "<div id=\"demo\">" );
        out.println( "<div id=\"media\" class=\"highlight\" data-timecontainer=\"excl\" data-timeaction=\"display\">" );
        out.println( "<video data-syncmaster=\"true\" data-timeaction=\"none\" controls=\"controls\" preload=\"auto\">" );
        out.printf( "<source type=\"video/mp4\" src=\"%s-480p.mp4\"></source><br/>%n", media );
        out.printf( "<source type=\"video/webm\" src=\"%s-480p.webm\"></source>%n", media );
        out.printf( "<source type=\"video/ogg\" src=\"%s-240p.ogv\"></source>%n", media );
        out.printf( "<source type=\"video/mp4\" src=\"%s.mp4\"></source><br/>%n", media );
        out.printf( "<source type=\"video/webm\" src=\"%s.webm\"></source>%n", media );
        out.printf( "<source type=\"video/ogg\" src=\"%s.ogv\"></source>%n", media );
        out.println( "This page requires <strong>&lt;video&gt;</strong> support:<br/>" );
        out.println( "best viewed with Firefox&nbsp;3.5+, Safari&nbsp;4+, Chrome&nbsp;5+, Opera&nbsp;10.60+ or IE9.<br/><br/>" );
        out.println( "Internet Explorer users, please enable Flash or Silverlight." );
        out.println( "</video>" );
        /* end of head file */
        printHeader("0.0", "1.0", inputName, media);
	}
	
	public void printLine(String startTime, String endTime, String loc, String speechContent) {
        if ( Utils.isNotEmptyOrNull(startTime) ) {
   			out.println("</p>");
            if ( speechContent.length() > 75 )
                out.printf( "<p class=\"twolines\" data-begin=\"%d:%d.%d\" data-end=\"%d:%d.%d\"><span class=\"nutt\">%s:</span> %s%n",
                	TimeDivision.toMinutes(startTime), TimeDivision.toSeconds(startTime), TimeDivision.toCentiSeconds(startTime), 
                	TimeDivision.toMinutes(endTime), TimeDivision.toSeconds(endTime), TimeDivision.toCentiSeconds(endTime),
                	loc, speechContent );
            else                          
                out.printf((Params.tierDisplay.equals(";")?"<p " : "<p class=\"twolines\"") + "data-begin=\"%d:%d.%d\" data-end=\"%d:%d.%d\"><span class=\"nutt\">%s:</span> %s%n",
                	TimeDivision.toMinutes(startTime), TimeDivision.toSeconds(startTime), TimeDivision.toCentiSeconds(startTime), 
                	TimeDivision.toMinutes(endTime), TimeDivision.toSeconds(endTime), TimeDivision.toCentiSeconds(endTime),
                    loc, speechContent );
        } else {
        	printContinuation( loc, speechContent );
        }
	}

	public void printHeader(String startTime, String endTime, String loc, String speechContent) {
        if ( Utils.isNotEmptyOrNull(startTime) ) {
   			out.println("</p>");
            out.printf( "<p class=\"header\" data-begin=\"%d:%d.%d\" data-end=\"%d:%d.%d\"><span class=\"nutt\">%s</span> %s%n",
            	TimeDivision.toMinutes(startTime), TimeDivision.toSeconds(startTime), TimeDivision.toCentiSeconds(startTime), 
            	TimeDivision.toMinutes(endTime), TimeDivision.toSeconds(endTime), TimeDivision.toCentiSeconds(endTime),
            	loc, speechContent );
        } else {
        	printContinuation( loc, speechContent );
        }
	}
	
	public void printContinuation(String loc, String speechContent) {
        out.printf( "</br><span class=\"nutt\">%s</span>: %s%n", loc, speechContent );
	}
	
	/**
	 * Ecriture de la partie transcriptions: énoncés + tiers
	 */
	public void buildText(){
		ArrayList<TeiFile.Div> divs = tf.trans.divs;
		for(Div d : divs){
			for(AnnotatedUtterance u : d.utterances){
				if(Utils.isNotEmptyOrNull(u.type)){
					if (!u.start.isEmpty()) {
						float start = Float.parseFloat(u.start);
						if (start < 1.0) // c'est avant l'affichage du titre du morceau
							start = (float)1.0;
						String [] splitType = u.type.split("\t");
						try{
							String theme = Utils.cleanString(tf.transInfo.situations.get(splitType[1]));
							printHeader(Float.toString(start), Float.toString(start+1), splitType[0], theme);
						}
						catch(ArrayIndexOutOfBoundsException e){
							printHeader(Float.toString(start), Float.toString(start+1), splitType[0], "");
						}
					}
				}
				writeUtterance(u);
			}
		}
	}

	/**
	 * Ecriture des utterances
	 * @param u	L'utterance à écrire
	 */
	public void writeUtterance(AnnotatedUtterance u) {
		String speech;
		/*Chaque utterance a une liste d'énoncé, dans un format spécifique:
		 * start;end__speech
		 */
		for(String s : u.speeches){
			s = s.replaceAll("\n", "");
			String start = null;
			String end = null;
			String [] splitS = s.split("__");
			speech = toChatLine(splitS[1]).trim();
			String times = splitS[0];

			try{
				start = times.split(";")[0];
				end = times.split(";")[1];
			}
			catch(Exception e){}		

			//Si le temps de début n'est pas renseigné, on prend le temps de fin de l'énoncé précédent(si présent)
			if(!Utils.isNotEmptyOrNull(start)){
				try{
					start = u.speeches.get(u.speeches.indexOf(s)-1).split("__")[0].split(";")[1];
				}
				catch(Exception e){}
			}

			//Si le temps de fin n'est pas renseigné, on prend le temps de début de l'énoncé suivant(si présent)
			if(!Utils.isNotEmptyOrNull(end)){
				try{
					end = u.speeches.get(u.speeches.indexOf(s)+1).split("__")[0].split(";")[0];
					if(end.equals(start)){
						start = "";
						end = "";
					}
				}
				catch(Exception e){}
			}
			//Si l'énoncé est le premier de la liste de l'utterance, son temps de début est égal au temps de début de l'utterance
			if(u.speeches.indexOf(s) == 0 && !Utils.isNotEmptyOrNull(start)){
				start = u.start;
			}
			//Si l'énoncé est le dernier de la liste de l'utterance, son temps de fin est égal au temps de fin de l'utterance
			if(u.speeches.indexOf(s) == u.speeches.size()-1 && !Utils.isNotEmptyOrNull(end)){
				end = u.end;
			}

			//Ecriture de l'énoncé
			writeSpeech(u.speakerCode, convertSpecialCodes(speech), start, end, Params.forceEmpty);
		}
		// écriture des tiers
		for(Annot tier : u.tiers){
			writeTier(tier);
		}
		writeAddInfo(u);
	}

	/**
	 * Ecriture d'un énonce: lignes qui commencent par le symbole étoile *
	 * @param loc	Locuteur
	 * @param speechContent	Contenu de l'énoncé
	 * @param startTime	Temps de début de l'énoncé
	 * @param endTime	Temps de fin de l'énoncé
	 */
	public void writeSpeech(String loc, String speechContent, String startTime, String endTime, boolean force){
		if (!Params.partDisplay.isEmpty()) {
			String loctest = ";" + loc + ";";
			if (!Params.partDisplay.contains(loctest)) return;
		}
		//System.out.println(loc + ' ' + startTime + ' ' + endTime +' ' + speechContent);
		//Si le temps de début n'est pas renseigné, on mettra par défaut le temps de fin (s'il est renseigné) moins une seconde.
		if(!Utils.isNotEmptyOrNull(startTime)){
			if(Utils.isNotEmptyOrNull(endTime)){
				float start = Float.parseFloat(endTime) - 1;
				startTime = Float.toString(start);
			}
		}
		//Si le temps de fin n'est pas renseigné, on mettra par défaut le temps de début (s'il est renseigné) plus une seconde.
		else if(!Utils.isNotEmptyOrNull(endTime)){
			if(Utils.isNotEmptyOrNull(startTime)){
				float end = Float.parseFloat(startTime) + 1;
				endTime = Float.toString(end);
			}
		}

		//On ajoute les informations temporelles seulement si on a un temps de début et un temps de fin 
		if(Utils.isNotEmptyOrNull(endTime) && Utils.isNotEmptyOrNull(startTime)){
			printLine(startTime, endTime, loc, speechContent);
		} else if (force) {
			printContinuation(loc, speechContent);
		}
	}

	/**
	 * Ajout des info additionnelles (hors-tiers)
	 * @param u
	 */
	public void writeAddInfo(AnnotatedUtterance u){
		if (!Params.tierDisplay.isEmpty()) {
			String loctest = ";com;";
			if (!Params.tierDisplay.contains(loctest)) return;
		}
		//Ajout des informations additionnelles présents dans les fichiers srt
		for(String s : u.coms){
			String infoType = Utils.getInfoType(s);
			String infoContent = Utils.getInfo(s);
			printContinuation(infoType, infoContent);
		}
	}

	/**
	 * Ecriture des tiers: lignes qui commencent par le signe pourcent %
	 * @param tier	Le tier à écrire, au format : Nom du tier \t Contenu du tier
	 */
	public void writeTier(Annot tier){
		if (!Params.tierDisplay.isEmpty()) {
			String loctest = ";" + tier + ";";
			if (!Params.tierDisplay.contains(loctest)) return;
		}
		String type = tier.name;
		String tierContent = tier.content;
		String tierLine = tierContent.trim();
		printContinuation("%"+type+":", tierLine);	
	}

	/**
	 * Dans Chat, les lignes principales doivent se terminer par un symbole de fin de ligne (spécifié dans le fichier depfile.cut).
	 * Cette méthode vérifie si c'est bien le cas et si ça ne l'est pas elle rajoute le signe point (par défaut) à la fin de la ligne à écrire.
	 * @param line	Ligne à renvoyer.
	 * @return
	 */
	public String toChatLine(String line){
		String patternStr = "(\\+\\.\\.\\.|\\+/\\.|\\+!\\?|\\+//\\.|\\+/\\?|\\+\"/\\.|\\+\"\\.|\\+//\\?|\\+\\.\\.\\?|\\+\\.|\\.|\\?|!)\\s*$";
		Pattern pattern = Pattern.compile(patternStr);
		Matcher matcher = pattern.matcher(line);
		if (!matcher.find()) {
			line += ".";
		}
		return line;
	}

	public void createOutput() {
	}

	public static String convertSpecialCodes(String initial){
		initial = initial.replaceAll("yy(\\s|$)", "yyy ");
		initial = initial.replaceAll("xx(\\s|$)", "xxx ");
		initial = initial.replaceAll("ww(\\s|$)", "www ");
		initial = initial.replaceAll("\\*\\*\\*", "xxx");
		return ConventionsToChat.setConv(initial);
	}

	public static void usage() {
		System.err.println("Description: TeiToSubtHtml convertit un fichier au format TEI en un fichier au format Sous-titre HTML");
		System.err.println("Usage: TeiToSubtHtml [-options] <file.subt.html>");
		System.err.println("		:-i nom du fichier ou repertoire où se trouvent les fichiers Tei à convertir (les fichiers ont pour extension .teiml");
		System.err.println("		:-o nom du fichier de sortie au format Srt (.subt.html) ou du repertoire de résultats");
		System.err.println("			si cette option n'est pas spécifié, le fichier de sortie aura le même nom que le fichier d'entrée + \"_fromTEIML\", avec l'extension .subt.html;");
		System.err.println("			si on donne un repertoire comme input et que cette option n'est pas spécifiée, les résultats seront stockées dans un dossier nommé \"input_results/\"");
		System.err.println("		:-p participant = affiche ce participant");
		System.err.println("		:-t tier : affiche ce tier");
		System.err.println("		    ATTENTION: si un participant ou un tier est spécifié, il faut tous les spécifier sinon les autres ne seront pas imprimés");
		System.err.println("		:-t = : affiche tous les tiers");
		System.err.println("		:-a : affiche aussi les participants sans indication temporelle");
		System.err.println("		    ATTENTION: par défaut, tous les participants sont affichés, aucun des tiers n'est affiché et les participants sans indication temporelle ne sont pas affichés");
		System.err.println("		:-usage ou -help ou -h : affiche ce message");
		System.exit(1);
	}


	public static void main(String args[]) throws IOException {

		String input = null;
		String output = null;
		// parcours des arguments

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
					} else if (args[i].equals("-a")) {
						Params.forceEmpty = true;
					} else if (args[i].equals("-h") || args[i].equals("-help") || args[i].equals("-usage")) {
						usage();
						return;
					} else if (args[i].equals("-t")) {
						i++;
						if (args[i].equals("="))
							Params.tierDisplay = "";
						else
							Params.tierDisplay += args[i] + ";";
					} else if (args[i].equals("-p")) {
						if (Params.partDisplay.isEmpty())
							Params.partDisplay = ";";
						i++;
						Params.partDisplay += args[i] + ";";
					} else {
						System.err.println("Paramètre inconnu: " + args[i] + "\n");
						usage();
					}
				} catch (Exception e) {
					usage();
				}
			}
		}

		File f = new File (input);

		//Permet d'avoir le nom complet du fichier (chemin absolu, sans raccourcis spéciaux)
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
					outputDir = output+"/";
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
				if (file.isFile() && (name.endsWith(".teiml"))){
					String outputFileName = file.getName().split("\\.")[0] + EXT;
					TeiToSubtHtml ttc = new TeiToSubtHtml(file.getAbsolutePath(), outputDir+outputFileName);
					System.out.println(outputDir+outputFileName);
					ttc.createOutput();
				}
				else if(file.isDirectory()){
					args[0] = "-i";
					args[1] = file.getAbsolutePath();
					main(args);
				}
			}
		} else {
			if (output == null) {
				output = input.split("\\.")[0] + EXT;
			}
			else if(new File(output).isDirectory()){
				if(output.endsWith("/")){
					output = output + input.split("\\.")[0] + EXT;
				}
				else{
					output = output + "/"+ input.split("\\.")[0] + EXT;
				}
			}

			if (!(Utils.validFileFormat(input, ".teiml") || !Utils.validFileFormat(output, EXT))) {
				System.err.println("Le fichier d'entrée du programme doit avoir l'extension .teiml "
						+ "\nLe fichier de sortie du programme doit avoir l'extension .srt ");
				usage();
			}
			TeiToSubtHtml ttc = new TeiToSubtHtml(new File(input).getAbsolutePath(), output);
			System.out.println("Reading " + input);
			ttc.createOutput();
			System.out.println("New file created " + output);
		}

	}

}
