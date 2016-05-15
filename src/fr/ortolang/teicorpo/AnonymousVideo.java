/**
 * Création d'un fichier vidéo avec des noirs dans les zones privées
 * @author Christophe Parisse
 */
package fr.ortolang.teicorpo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AnonymousVideo extends TeiConverter {

	// Permet d'écrire le fichier de sortie des commandes FFMPEG
	private PrintWriter outTemporary;
	// Encodage du fichier de sortie
	final static String outputEncoding = "UTF-8";
	// Extension du fichier de sortie
	final static String EXT = ".sh";
	final static String FILESTEMPORARY = "files.concat";
	String transcriptFileName;
	int npart;
	String mediaName;

	/**
	 * Convertit le fichier TEI donné en argument en un fichier sh.
	 * 
	 * @param inputName
	 *            Nom du fichier d'entrée (fichier TEI, a donc l'extenstion
	 *            .tei_corpo.xml)
	 */
	public AnonymousVideo(String inputName, String mediaName) {
		super(inputName, null, null);
		transcriptFileName = inputName;
		this.mediaName = mediaName;
		outputWriter();
		conversion();
		createOutput();
	}

	/**
	 * Ecriture de l'output
	 */
	public void outputWriter() {
		outTemporary = null;
		try {
			FileOutputStream of = new FileOutputStream(FILESTEMPORARY);
			OutputStreamWriter outWriter = new OutputStreamWriter(of, outputEncoding);
			outTemporary = new PrintWriter(outWriter, true);
		} catch (Exception e) {
			System.err.println("cannot create temporary file");
		}
	}

	/**
	 * Conversion
	 */
	public void conversion() {
		// Etapes de conversion
		Node principalMedia;
		ArrayList<String> listFiles = new ArrayList<String>();
		String baseurl;
		XPathExpression expr;
		NodeList nl;
		npart = 0;
		try {
			if (mediaName == null) {
		        expr = tf.xpath.compile("//recordingStmt/recording/media");
		        nl = (NodeList)expr.evaluate(tf.teiDoc, XPathConstants.NODESET);
		        if (nl.getLength() > 0) {
		        	principalMedia = nl.item(0);
		        	String mime = ((Element)principalMedia).getAttribute("mimeType");
		        	if (!Utils.isNotEmptyOrNull(mime) || !mime.contains("video")) {
		        		System.err.println("Le media associé n'est pas une vidéo. Impossible de traiter.");
		        		return;
		        	}
		        	mediaName = ((Element)principalMedia).getAttribute("url");
		        } else {
	        		System.err.println("Media inconnu impossible de traiter.");
					return;
		        }
		        baseurl = Utils.fullbasename(mediaName);
			} else
		        baseurl = Utils.fullbasename(mediaName);
			String prevTime = "0.0";
			nl = Utils.getAllDivs(tf.xpath, tf.teiDoc);
			boolean found = false;
			for (int i=0; i < nl.getLength(); i++) {
				Node n = nl.item(i);
				Element ne = (Element)n;
				String st = Utils.getDivHeadAttr(ne, "subtype");
				String sit = tf.transInfo.situations.get(st);
				if (sit.startsWith("private")) {
					found = true;
					break;
				}
			}
			if (!found) {
				System.out.println("Rien à faire pour " + transcriptFileName);
				return;
			}
			System.out.printf("Version noire de %s :-: %s%n", mediaName, baseurl + "-black.mp4");
        	execAnon(mediaName, baseurl + "-black.mp4");
			for (int i=0; i < nl.getLength(); i++) {
				Node n = nl.item(i);
				Element ne = (Element)n;
				String st = Utils.getDivHeadAttr(ne, "subtype");
				String sit = tf.transInfo.situations.get(st);
				if (sit.startsWith("private")) {
					String start = tf.teiTimeline.getTimeValue(Utils.getDivHeadAttr(ne, "start"));
					String end = tf.teiTimeline.getTimeValue(Utils.getDivHeadAttr(ne, "end"));
					if (Double.parseDouble(start) != 0.0) {
						String fn = baseurl + "-" + npart + ".mp4";
						npart++;
						listFiles.add(fn);
						System.out.printf("Extrait public %s %s %s%n", fn, prevTime, start);
						execPart(mediaName, fn, prevTime, start);
					}
					String fn = baseurl + "-" + npart + ".mp4";
					npart++;
					listFiles.add(fn);
					System.out.printf("Extrait privé %s %s %s%n", fn, start, end);
					execPart(baseurl + "-black.mp4", fn, start, end);
					prevTime = end;
				}
			}
	        expr = tf.xpath.compile("//recordingStmt/recording/date");
	        nl = (NodeList)expr.evaluate(tf.teiDoc, XPathConstants.NODESET);
			String fn = baseurl + "-" + npart + ".mp4";
			npart++;
			listFiles.add(fn);
	        if (nl.getLength() > 0) {
	        	Node n = nl.item(0);
	        	String dur = ((Element)n).getAttribute("dur");
	        	if (Utils.isNotEmptyOrNull(dur)) {
					System.out.printf("Extrait public %s %s %s%n", fn, prevTime, dur);
	        		execPart(mediaName, fn, prevTime, dur);
	        	} else {
					System.out.printf("Extrait public %s %s ... fin%n", fn, prevTime);
	        		execPart(mediaName, fn, prevTime, null);
	        	}
	        } else {
				System.out.printf("Extrait public %s %s ... fin%n", fn, prevTime);
        		execPart(mediaName, fn, prevTime, null);
	        }
        	System.out.printf("Concaténation vers %s%n", baseurl + "-anonym.mp4");
	        for (String s: listFiles) {
	        	outTemporary.printf("file '%s'%n", s);
	        }
    		outTemporary.close();
        	execConcat(baseurl + "-anonym.mp4", FILESTEMPORARY);
        	File f = new File(FILESTEMPORARY);
        	f.delete();
        	f = new File(baseurl + "-black.mp4");
        	f.delete();
	        for (String s: listFiles) {
	        	f = new File(s);
	        	f.delete();
	        }
		} catch (Exception e) {
			//e.printStackTrace();
			System.err.println("Erreur de traitement: " + e.getMessage());
		}
	}

	private void execConcat(String fn, String filestemporary2) {
        try {
            String[] commande = {"ffmpeg", "-f", "concat", "-i", filestemporary2, "-c", "copy", "-strict", "-2", "-y", fn};
//            System.out.println("Execute: " + Utils.join(commande));
            Process p = Runtime.getRuntime().exec(commande);
            p.waitFor();
        } catch (IOException | InterruptedException e) {
			//e.printStackTrace();
			System.err.println("Erreur de traitement de execConcat: " + e.getMessage());
        }
	}

	private void execAnon(String media, String fn) {
        try {
            String[] cmd2 = {"ffmpeg", "-i", media, "-vf", "eq=brightness=-1.0", "-strict", "-2", "-y", fn};
//            System.out.println("Execute: " + Utils.join(cmd2));
            Process p = Runtime.getRuntime().exec(cmd2);
            p.waitFor();
        } catch (IOException | InterruptedException e) {
			//e.printStackTrace();
			System.err.println("Erreur de traitement de execAnon: " + e.getMessage());
        }
	}

	private void execPart(String media, String fn, String start, String end) {
        try {
            String[] commande = {"ffmpeg", "-i", media, "-vcodec", "copy", "-acodec", "copy", "-ss", start, "-t", end, "-strict", "-2", "-y", fn};
//            System.out.println("Execute: " + Utils.join(commande));
            Process p = Runtime.getRuntime().exec(commande);
            p.waitFor();
        } catch (IOException | InterruptedException e) {
			//e.printStackTrace();
			System.err.println("Erreur de traitement de execPart: " + e.getMessage());
        }
	}

	public void createOutput() {
		System.out.printf("%s traité: %d parties anonymes%n", transcriptFileName, npart);
	}

	public static void main(String[] args) {
		String mediaName = null;
		String input = null;
		if (args.length < 2) {
			System.err.println("Vous n'avez spécifié aucun argument\n");
			System.err.println("Usage: java -cp fr.ortolang.teicorpo.AnonymousVideo -i fichier -m media\n");
			return;
		}
		for (int i = 0; i < args.length; i++) {
			try {
				if (args[i].equals("-i")) {
					i++;
					input = args[i];
				} else if (args[i].equals("-m")) {
					i++;
					mediaName = args[i];
				} else {
					System.err.println("Usage: java -cp fr.ortolang.teicorpo.AnonymousVideo -i fichier -m media\n");
					return;
				}
			} catch (Exception e) {
				System.err.println("Usage: java -cp fr.ortolang.teicorpo.AnonymousVideo -i fichier -m media\n");
				return;
			}
		}
		
		if (input == null) {
			System.err.println("Usage: java -cp fr.ortolang.teicorpo.AnonymousVideo -i fichier -m media\n");
			return;
		}
		AnonymousVideo av = new AnonymousVideo(input, mediaName);
	}

	@Override
	public void writeSpeech(String loc, String speechContent, String startTime, String endTime) {
		// TODO Auto-generated method stub
		
	}
}
