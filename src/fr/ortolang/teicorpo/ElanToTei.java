package fr.ortolang.teicorpo;

import java.io.File;
import java.io.IOException;

public class ElanToTei {
	
	/** Encodage des fichiers de sortie et d'entrée. */
	static final String outputEncoding = "UTF-8";
	/** Extension Elan **/
	static String EXT = ".eaf";
	

	ElanToHT ElanToHT;
	HT_ToTei ht;

	public ElanToTei(File inputFile, String outputName) throws IOException {
		ElanToHT = new ElanToHT(inputFile);
		ht = new HT_ToTei(ElanToHT.ht);
		
		Utils.createFile(outputName, ht.docTEI);
	}

	/**
	 * Affiche la description et l'usage du programme principal.
	 */
	public static void usage() {
		System.err
		.println("Description: ElanToTei convertit un fichier au format Elan en un fichier au format TEI");
		System.err.println("Usage: ElanToTei [-options] <file" + EXT + ">");
		System.err.println("	:-i nom du fichier ou repertoire où se trouvent les fichiers Elan à convertir.");
		System.err.println("		Les fichiers Elan ont pour extension " + EXT);
		System.err.println("	:-o nom du fichier ou repertoire des résultats au format TEI (." + Utils.EXT+")");
		System.err.println("		si cette option n'est pas spécifiée, le fichier de sortie aura le même nom que le fichier d'entrée, avec l'extension " + Utils.EXT);
		System.err.println("		ou les résultats seont stockés dans le même dossier que le dossier d'entrée.\"");
		System.err.println("	:-usage ou -help = affichage de ce message\n");
		System.exit(1);
	}

	/**
	 * Programme principal: convertit un fichier au format Elan en un
	 * fichier au format TEI.
	 * 
	 * @param args
	 *            Liste des aruments du programme.
	 * @throws IOException 
	 */
	public static void main(String[] args) throws Exception {
		Utils.printVersionMessage();
		submain(args);
	}

	public static void submain(String[] args) throws Exception {
		String input = null;
		String output = null;
		// parcours des arguments

		if (args.length == 0) {
			System.err.println("Vous n'avez spécifié aucun argument.\n");
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
					} else if (args[i].equals("--pure")) {
						Utils.teiStylePure = true;
					} else {
						usage();
					}
				} catch (Exception e) {
					usage();
				}
			}
		}

		File f = new File (input);

		//Permet d'avoir le nom complet du fichier (chemin absolu, sans signes spéciaux(. et .. par exemple)) 
		input = f.getCanonicalPath();

		if (f.isDirectory()){
			//Parcours de tous les fichiers présents dans le repertoire, et descente dans les sous-repertoires...
			// => suppression du filtre pour les extensions de fichier
			File[] elanFiles = f.listFiles();

			String outputDir = "";
			if (output == null){
				if(input.endsWith(input)){
					outputDir = input.substring(0, input.length()) + "/";
				}
				else{
					outputDir = output + "/";
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
					System.out.println("\n Erreur :" + output + " est un fichier, vous devez spécifier un nom de dossier pour le stockage des résultats.\n");					
					usage();
					System.exit(1);
				}
			}

			new File(outputDir).mkdir();

			for (File file : elanFiles){
				String name = file.getName();
				if (file.isFile()) {
					if (name.endsWith(Utils.EXT_PUBLISH + EXT)) {
						System.out.printf("-- ignoré: %s%n", file.toString());
					} else if (name.endsWith(EXT)) {
						String outputFileName = Utils.basename(file) + Utils.EXT;
						// System.out.printf("from %s to %s %n", file.toString(), outputDir + outputFileName);
						new ElanToTei(file, outputDir + outputFileName );
					}
				}
				else if(file.isDirectory()){
					args[0] = "-i";
					args[1] = file.getAbsolutePath();
					submain(args);
				}
			}
		}
		else{
			if (output == null) {
				output = Utils.fullbasename(input) + Utils.EXT;
			}
			else if(new File(output).isDirectory()){
				if(output.endsWith("/")){
					output = output + Utils.basename(input) + Utils.EXT;
				}
				else{
					output = output + "/"+ Utils.basename(input) + Utils.EXT;
				}
			}
			if (!(Utils.validFileFormat(input, EXT))) {
				System.err.println("Le fichier d'entrée du programme doit avoir l'extension " + EXT);
			}
			new ElanToTei(new File(input), output);
		}
	}
}