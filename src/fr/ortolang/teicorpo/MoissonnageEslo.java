package fr.ortolang.teicorpo;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class MoissonnageEslo {
	
	
	
	/** Fichier d'entrée, au format .xml. */
	private File inputEsloMetadatasFile;
	/** Document issu du fichier Moissonnage Eslo. */
	private Document docMetadatas;
	/** Racine du document issu du moissonnage d'Eslo. */
	private Element rootMoissonnage;

	/**
	 * décrit par la DTD "tei_corpo.dtd" Utils.TEI_CORPO_DTD .
	 * 
	 * @param inputFile
	 *            : fichier à convertir, au format Transcriber
	 * @throws ParserConfigurationException 
	 */
	public MoissonnageEslo(File inputFile) throws ParserConfigurationException {
		this.inputEsloMetadatasFile = inputFile;
		// Création du document moissonnage
		this.docMetadatas = null;
		this.rootMoissonnage = null;
		DocumentBuilderFactory factory = null;
		try {
			factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			this.docMetadatas = builder.parse(this.inputEsloMetadatasFile);
			this.rootMoissonnage = this.docMetadatas.getDocumentElement();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		createMetaFiles();
	}

	
	public void createMetaFiles() throws ParserConfigurationException{
		
		NodeList records = this.docMetadatas.getElementsByTagName("record");
		for(int i = 0; i<records.getLength(); i++){
			Element record = (Element)records.item(i);
			NodeList identifiers = record.getElementsByTagName("dc:identifier");
			/*for(int j = 0; j<identifiers.getLength(); j++){
				Element identifier = (Element)identifiers.item(j);
				String identifierURL = identifier.getTextContent();
				if(identifierURL.endsWith(".wav") || identifierURL.endsWith(".xml")){
					//System.out.println("curl " +  identifierURL + "> \"$(basename " + identifierURL + ")\"\n");
				}
			}*/
			
			
			/*NodeList dcTerms = record.getElementsByTagName("dcterms:isFormatOf");
			for(int z = 0; z<dcTerms.getLength(); z++){
				Element dcTerm = (Element)dcTerms.item(z);
				String dcTermURL = dcTerm.getTextContent();
				if(dcTermURL.endsWith(".wav") || dcTermURL.endsWith(".mp3")){
					System.out.println("curl " +  dcTermURL + "> \"$(basename " + dcTermURL + ")\"\n");
				}
			}*/
			
			
			//// Ecriture des fichiers metadonnées
			record.setAttribute("xmlns", "http://www.openarchives.org/OAI/2.0/");
			record.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
			record.setAttribute("xsi:schemaLocation", "http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd");			
			
			String recordName = record.getElementsByTagName("identifier").item(0).getTextContent().split("oai:crdo.vjf.cnrs.fr:crdo-")[1];
			String outputFileName = "esloMetadatas/" + recordName + ".meta.xml";
			System.out.println(outputFileName);			
			
			Source source = new DOMSource(record);
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
		
	}
	
	
	public static void main(String args[]) throws IOException, ParserConfigurationException {

		File input = null;
		File output = null;
		// parcours des arguments

		if (args.length == 0) {
			System.err.println("Vous n'avez spécifié aucun argument.\n");
		} else {
			for (int i = 0; i < args.length; i++) {
				try {
					if (args[i].equals("-i")) {
						i++;
						input = new File(args[i]);
					} else if (args[i].equals("-o")) {
						i++;
						output = new File(args[i]);
					}
				} catch (Exception e) {
					System.err.println("Problème arguments.\n");
				}
			}
		}
		
		new MoissonnageEslo(input);
	}

}
