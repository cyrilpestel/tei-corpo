package fr.ortolang.teicorpo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;

/**
 * local temporary structures description of relation between tiers
 */
class DescTier {
	String tier;
	String type;
	String parent;

	public String toString() {
		String s = "Tier: " + tier + " type(" + type + ") parent(" + parent + ")";
		return s;
	}
	// DescTier() { tier=null; type=null; parent=null; };

	/**
	 * check and return type value
	 */
	static String whichType(String t) {
		if (t.equals("-"))
			return LgqType.ROOT;
		if (t.equals("assoc"))
			return LgqType.SYMB_ASSOC;
		if (t.equals("incl"))
			return LgqType.INCLUDED;
		if (t.equals("symbdiv"))
			return LgqType.SYMB_DIV;
		if (t.equals("timediv"))
			return LgqType.TIME_DIV;
		if (t.equals("point"))
			return LgqType.POINT;
		if (t.equals("timeint"))
			return LgqType.TIME_INT;
		return null;
	}
}

class TierParams {
	boolean verbose;
	boolean forceEmpty;
	boolean cleanLine;
	boolean sectionDisplay;
	List <String> input;
	String output;
	String mediaName;
	String encoding;
	String options;
	boolean nospreadtime;
	boolean detectEncoding;
	boolean clearChatFormat;
	HashSet<String> commands;  // for -c parameter
	HashSet<String> doDisplay;
	HashSet<String> dontDisplay;
	TreeMap<String, String> tv;
	int level;
	boolean raw;
	boolean noHeader;
	boolean iramuteq;
	boolean concat;
	String outputFormat;
	String inputFormat;
	boolean locutor;
	boolean dtdValidation;
	boolean erase;
	boolean eraseDone;
	ArrayList<DescTier> ldt = new ArrayList<DescTier>();
	boolean shortextension;

	TierParams() {
		verbose = false;
		input = new ArrayList<String>();
		output = null;
		mediaName = null;
		encoding = null;
		detectEncoding = true;
		commands = new HashSet<String>();
		doDisplay = new HashSet<String>();
		dontDisplay = new HashSet<String>();
		level = 0; // all levels
		forceEmpty = true;
		cleanLine = false;
		noHeader = false;
		clearChatFormat = false;
		raw = false;
		sectionDisplay = false;
		options = "";
		nospreadtime = false;
		iramuteq = false;
		concat = false;
		erase = true;
		eraseDone = false;
		tv = new TreeMap<String, String>();
		locutor = false;
		dtdValidation = false;
		outputFormat = "";
		inputFormat = "";
		shortextension = false;
	}
	void addCommand(String s) {
		commands.add(s);
	}
	void addDoDisplay(String s) {
		doDisplay.add(s.toLowerCase());
	}
	void addDontDisplay(String s) {
		dontDisplay.add(s.toLowerCase());
	}
	void addTv(String info) {
		int p = info.indexOf(":");
		if (p<1 || p >= info.length()) {
			System.err.println("error: txm information ignored (missing :) => " + info);
		} else {
			tv.put(info.substring(0, p), info.substring(p+1));
		}
	}
	void setLevel(int l) {
		level = l;
	}
	/*
	boolean isLevel(int t) {
		return (level == 0 || t <= level) ? true : false; // si level == 0 all else if 't' is <= level
	}
	*/
	private static boolean test(String s, HashSet<String> dd) {
		s = s.toLowerCase();
		for (String f: dd) {
			if (f.startsWith("*") && f.endsWith("*")) {
				if (s.indexOf(f.substring(1, f.length()-2)) >= 0) return true;
			} else if (f.startsWith("*")) {
				if (s.indexOf(f.substring(1)) >= 0) return true;
			} else if (f.endsWith("*")) {
				if (s.indexOf(f.substring(0, f.length()-1)) >= 0) return true;
			} else {
				if (s.equals(f)) return true;
			}
		}
		return false;
	}
	boolean isDoDisplay(String s) {
		return isDoDisplay(s,0);
	}
	boolean isDontDisplay(String s) {
		return isDontDisplay(s,0);
	}
	boolean isDoDisplay(String s, int level) {
		if (doDisplay.size() < 1) return true;
		if (level >= 3) {
			if (TierParams.test("=3", doDisplay)) return true;
		}
		if (level == 2) {
			if (TierParams.test("=2", doDisplay)) return true;
		}
		if (level == 1) {
			if (TierParams.test("=1", doDisplay)) return true;
		}
		return TierParams.test(s, doDisplay);
	}
	boolean isDontDisplay(String s, int level) {
		if (dontDisplay.size() < 1) return false;
		if (level >= 3) {
			if (TierParams.test("=3", dontDisplay)) return true;
		}
		if (level == 2) {
			if (TierParams.test("=2", dontDisplay)) return true;
		}
		if (level == 1) {
			if (TierParams.test("=1", dontDisplay)) return true;
		}
		return TierParams.test(s, dontDisplay);
	}


	public static void printVersionMessage() {
    	System.out.println("Conversions (version "+ Utils.versionSoft +") " + Utils.versionDate + " Version TEI_CORPO: " + Utils.versionTEI);
	}
	
	public static void printUsageMessage(String mess, String ext1, String ext2, int style) {
		System.err.printf("%s%n", mess);
		System.err.println("         :(sans option ou -i) nom du fichier ou repertoire où se trouvent les fichiers Tei à convertir - les fichiers ont pour extension " + ext1);
		System.err.println("         :-o nom du fichier de sortie au format Elan (.eaf) ou du repertoire de résultats");
		System.err.println("            si cette option n'est pas spécifié, le fichier de sortie aura le même nom que le fichier d'entrée avec l'extension " + ext2);
		System.err.println("            si on donne un repertoire comme input et que cette option n'est pas spécifiée, les résultats seront stockés dans le même dossier que l'entrée.\"");
		System.err.println("         :-p fichier_de_parametres: contient les paramètres sous leur format ci-dessous, un jeu de paramètre par ligne.");
		System.err.println("         :-n niveau: niveau d'imbrication (1 pour lignes principales)");
		System.err.println("         :-a name : le locuteur/champ name est produit en sortie (caractères génériques acceptés)");
		System.err.println("         :-s name : le locuteur/champ name est suprimé de la sortie (caractères génériques acceptés)");
		System.err.println("         :-cleanline : exporte des énoncés sans marqueurs spéficiques de l'oral");
		System.err.println("         :-clearchat : remplace les marqueurs + et apostrophes de chat par des marques standard");
		System.err.println("         :-raw : exporte le texte sans aucune marqueurs de locuteur ni marqueurs spéficiques de l'oral");
		System.err.println("         :-iramuteq : headers for iramuteq");
		System.err.println("         :-concat : concaténation des fichiers résultats for iramuteq");
		System.err.println("         :-append : pas d'effacement préalable du fichier destination si concaténation");
		System.err.println("         :-short : les extensions fichiers autre que TEI_CORPO ne contiennent pas tei_corpo");
		System.err.println("         :--dtd cette option permet de vérifier que les fichiers Transcriber sont conformes à leur dtd");
		System.err.println("            si cette option est spécifiée, la dtd (Trans-14.dtd) doit se trouver dans le même repertoire que le fichier Transcriber\n"
						+ "\t\tTéléchargement de la DTD de Transcriber : http://sourceforge.net/p/trans/git/ci/master/tree/etc/trans-14.dtd");
		if (style == 5) {
			printImportPartitionMessage();
		}
		if (style == 4) {
			System.err.println("         :-to format des fichiers input");
			System.err.println("         :-from format des fichiers output");
		}
		if (style == 3) {
			System.err.println("         *** paramètre pour édition de fichier TEI***");
			System.err.printf ("         :-c commands:%n\t-c media=value%n\t-c mimetype=value%n\t-c docname=value%n\t-c chgtime=value%n\t-c replace%n");
		}
		if (style == 2) {
			System.err.println("         *** paramètre pour export dans TXM/Iramuteq/Le Trameur ***");
			System.err.println("         :-tv \"type:valeur\" : un champ type:valeur est ajouté dans les <w> de txm ou lexico ou le trameur");
			System.err.println("         :-section : ajoute un indicateur de section en fin de chaque énoncé (pour lexico/le trameur)");
		}
		System.err.println("	     :-usage ou -help = affichage ce message");
		// System.exit(1);
	}

	private static void printImportPartitionMessage() {
		System.err.println("         *** paramètre pour import fichiers partition ***");
		System.err.println("         :-m nom/adresse du fichier média");
		System.err.println("         :-e encoding (par défaut detect encoding)");
		System.err.println("         :-d default UTF8 encoding ");
		System.err.println("         :-t tiername type parent (describe relations between tiers)");
		System.err.println("             types autorisés: - assoc incl symbdiv timediv");
	}

	public static boolean processArgs(String[] args, TierParams options, String usage, String ext1, String ext2, int style) {
		options.inputFormat = ext1;
		options.outputFormat = ext2.isEmpty() ? Utils.EXT : ext2;
		if (args.length == 0) {
			System.err.println("Vous n'avez spécifié aucun argument\n");
			printUsageMessage(usage, ext1, ext2, style);
			return false;
		} else {
			for (int i = 0; i < args.length; i++) {
				try {
					if (args[i].equals("-i")) {
						i++;
						continue;
					} else if (args[i].equals("-o")) {
						i++;
						continue;
					} else if (args[i].equals("-to")) {
						i++;
						continue;
					} else if (args[i].equals("-from")) {
						i++;
						continue;
					} else if (args[i].equals("-n")) {
						i++;
						continue;
					} else if (args[i].equals("-a")) {
						i++;
						continue;
					} else if (args[i].equals("-s")) {
						i++;
						continue;
					} else if (args[i].equals("-c")) {
						i++;
						continue;
					} else if (args[i].equals("-f")) {
						i++;
						continue;
					} else if (args[i].equals("-tv")) {
						i++;
						continue;
					} else if (args[i].equals("-m")) {
						i++;
						continue;
					} else if (args[i].equals("-e")) {
						i++;
						continue;
					} else if (args[i].equals("-t")) {
						i++;
						i++;
						i++;
						continue;
					} else if (args[i].equals("-p")) {
						if (i+1 >= args.length) {
							printUsageMessage(usage, ext1, ext2, style);
							return false;
						}
						i++;
						getTierParams(args[i], options.ldt, options);
					} else if (args[i].equals("-h") || args[i].equals("-help") || args[i].equals("-usage")) {
						printUsageMessage(usage, ext1, ext2, style);
						return false;
					} else {
						continue;
					}
				} catch (Exception e) {
					printUsageMessage(usage, ext1, ext2, style);
					return false;
				}
			}
			for (int i = 0; i < args.length; i++) {
				try {
					if (args[i].equals("-i")) {
						if (i+1 >= args.length) {
							printUsageMessage(usage, ext1, ext2, style);
							return false;
						}
						i++;
						options.input.add(args[i]);
					} else if (args[i].equals("-o")) {
						if (i+1 >= args.length) {
							printUsageMessage(usage, ext1, ext2, style);
							return false;
						}
						if (i >= args.length || options.output != null) {
							printUsageMessage(usage, ext1, ext2, style);
							return false;
						}
						i++;
						options.output = args[i];
					} else if (args[i].equals("-to")) {
						if (i+1 >= args.length) {
							printUsageMessage(usage, "-", "-", style);
							return false;
						}
						i++;
						options.outputFormat = TeiCorpo.extensions(args[i]);
					} else if (args[i].equals("-from")) {
						if (i+1 >= args.length) {
							printUsageMessage(usage, "-", "-", style);
							return false;
						}
						i++;
						options.inputFormat = TeiCorpo.extensions(args[i]);
					} else if (args[i].equals("-n")) {
						if (i+1 >= args.length) {
							System.err.println("le parametre -n n'est pas suivi d'une valeur");
							// printUsageMessage(usage, ext1, ext2, style);
							return false;
						}
						i++;
						try {
							options.setLevel(Integer.parseInt(args[i]));
						} catch(Exception e) {
							System.err.println("Le paramètre -n n'est pas suivi d'un entier.");
							// Utils.printUsageMessage(usage, ext1, ext2, style);
							return false;
						}
					} else if (args[i].equals("-c")) {
						if (i+1 >= args.length) {
							System.err.println("le parametre -c n'est pas suivi d'une valeur");
							// Utils.printUsageMessage(usage, ext1, ext2, style);
							return false;
						}
						i++;
						options.addCommand(args[i]);
					} else if (args[i].equals("-a")) {
						if (i+1 >= args.length) {
							System.err.println("le parametre -a n'est pas suivi d'une valeur");
							// Utils.printUsageMessage(usage, ext1, ext2, style);
							return false;
						}
						i++;
						options.addDoDisplay(args[i]);
					} else if (args[i].equals("-s")) {
						if (i+1 >= args.length) {
							System.err.println("le parametre -s n'est pas suivi d'une valeur");
							// Utils.printUsageMessage(usage, ext1, ext2, style);
							return false;
						}
						i++;
						options.addDontDisplay(args[i]);
					} else if (args[i].equals("-tv")) {
						if (i+1 >= args.length) {
							System.err.println("le parametre -tv n'est pas suivi d'une valeur");
							// Utils.printUsageMessage(usage, ext1, ext2, style);
							return false;
						}
						i++;
						options.addTv(args[i]);
					} else if (args[i].equals("-f")) {
						if (i+1 >= args.length) {
							System.err.println("le parametre -f n'est pas suivi d'une valeur");
							// Utils.printUsageMessage(usage, ext1, ext2, style);
							return false;
						}
						i++;
						if (args[i].equals("mor")) {
							options.options = "mor";
						} else if (args[i].equals("xmor")) {
							options.options = "xmor";
						} else if (args[i].equals("morext")) {
							options.options = "morext";
						} else if (args[i].equals("xmorext")) {
							options.options = "xmorext";
						} else {
							printUsageMessage(usage, ext1, ext2, style);
							return false;
						}
					} else if (args[i].equals("-p")) {
						i++;
						continue;
					} else if (args[i].equals("-stdevent")) {
						Utils.leftBracket = "<"; // 27EA - "❮"; // "⟨" 27E8 - "❬" 
						Utils.rightBracket = ">"; // 27EB - "❯"; // "⟩" 27E9 - "❭" - 276C à 2771 ❬ ❭ ❮ ❯ ❰ ❱ 
						Utils.leftEvent = "[=! "; // 27E6 - "『"; // 300E - "⌈"; // u2308 
						Utils.rightEvent = "]"; // 27E7 - "』"; // 300F - "⌋"; // u230b
						Utils.leftParent = "[% "; // 2045 // "⁘"; // 2058 // "⁑" // 2051
						Utils.rightParent = "]"; // 2046 // "⁘"; // 2058
						Utils.leftCode = "[% "; // 231C - "⁌"; // 204C
						Utils.rightCode = "]"; // 231F - "⁍"; // 204D
						continue;
					} else if (args[i].equals("-cleanline")) {
						options.cleanLine = true;
						continue;
					} else if (args[i].equals("-nospreadtime")) {
						options.nospreadtime = true;
						continue;
					} else if (args[i].equals("-pure")) {
						Utils.teiStylePure = true;
						continue;
					} else if (args[i].equals("--dtd")) {
						options.dtdValidation = true;
					} else if (args[i].equals("-clearchat")) {
						options.clearChatFormat = true;
						continue;
					} else if (args[i].equals("-noheader")) {
						options.noHeader = true;
						continue;
					} else if (args[i].equals("-v")) {
						options.verbose = true;
						continue;
					} else if (args[i].equals("-raw")) {
						options.raw = true;
						continue;
					} else if (args[i].equals("-concat")) {
						options.concat = true;
						continue;
					} else if (args[i].equals("-short")) {
						options.shortextension = true;
						continue;
					} else if (args[i].equals("-append")) {
						options.erase = false;
						continue;
					} else if (args[i].equals("-iramuteq")) {
						options.iramuteq = true;
						options.raw = true;
						options.concat = true;
						continue;
					} else if (args[i].equals("-loc")) {
						options.locutor = true;
						continue;
					} else if (args[i].equals("-section")) {
						options.sectionDisplay = true;
						continue;
					} else if (args[i].equals("-m")) {
						if (i+1 >= args.length) {
							System.err.println("le parametre -m n'est pas suivi d'une valeur");
							return false;
						}
						i++;
						options.mediaName = args[i];
					} else if (args[i].equals("-e")) {
						if (i+1 >= args.length) {
							System.err.println("le parametre -e n'est pas suivi d'une valeur");
							return false;
						}
						i++;
						options.encoding = args[i];
						options.detectEncoding = false;
					} else if (args[i].equals("-t")) {
						if (i+3 >= args.length) {
							System.err.println("le parametre -m n'est pas suivi d'une valeur");
							return false;
						}
						DescTier d = new DescTier();
						i++;
						d.tier = args[i];
						i++;
						d.type = args[i];
						if (DescTier.whichType(args[i]) == null) {
							System.err.println("le parametre -t ne contient pas un argument connu: " + args[i]);
							return false;
						}
						i++;
						d.parent = args[i];
						options.ldt.add(d);
					} else if (args[i].equals("-d")) {
						options.detectEncoding = false;
						options.encoding = "UTF-8";
					} else {
						options.input.add(args[i]);
						// Utils.printUsageMessage(usage, ext1, ext2, style);
						// return false;
					}
				} catch (Exception e) {
					printUsageMessage(usage, ext1, ext2, style);
					return false;
				}
			}
		}
		if (options.input == null) {
			System.out.println("Pas de fichier à traiter");
			return false;
		}
		return true;
	}

	public static boolean getTierParams(String fn, ArrayList<DescTier> ldt, TierParams pr) {
		List<String> ls = null;
		try {
			ls = Utils.loadTextFile(fn);
		} catch (IOException e) {
			System.err.println("Impossible de traiter le fichier: " + fn);
			return false;
		}
		for (int k = 0; k < ls.size(); k++) {
			String l = ls.get(k);
			String[] p = l.split("\\s+");
			if (p.length > 0) {
				if (p[0].equals("-e") || p[0].equals("e")) {
					if (p.length > 1) {
						pr.encoding = p[1];
						pr.detectEncoding = false;
					}
				} else if (p[0].equals("-d") || p[0].equals("d")) {
					pr.detectEncoding = false;
					pr.encoding = "UTF-8";
				} else if (p[0].equals("-m") || p[0].equals("m")) {
					if (p.length > 1) {
						pr.mediaName = p[1];
					}
				} else if (p[0].equals("-t") || p[0].equals("t")) {
					DescTier d = new DescTier();
					if (p.length < 4)
						printImportPartitionMessage();
					d.tier = p[1];
					d.type = p[2];
					d.parent = p[3];
					ldt.add(d);
				} else if (p[0].equals("-n") || p[0].equals("n")) {
					if (p.length < 2) {
						System.err.println("Mauvaise ligne [" + l + "] dans le fichier paramètre: " + fn);
						return false;
					}
					pr.setLevel(Integer.parseInt(p[1]));
				} else if (p[0].equals("-s") || p[0].equals("s")) {
					if (p.length < 2) {
						System.err.println("Mauvaise ligne [" + l + "] dans le fichier paramètre: " + fn);
						return false;
					}
					pr.addDontDisplay(p[1]);
				} else if (p[0].equals("-a") || p[0].equals("a")) {
					if (p.length < 2) {
						System.err.println("Mauvaise ligne [" + l + "] dans le fichier paramètre: " + fn);
						return false;
					}
					pr.addDoDisplay(p[1]);
			} else {
					System.err.println("Format inconnu dans le fichier paramètre: " + fn);
					return false;
				}
			}
		}
		return true;
	}
}
