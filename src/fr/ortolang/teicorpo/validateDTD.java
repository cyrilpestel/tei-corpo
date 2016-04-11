package fr.ortolang.teicorpo;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.xml.sax.InputSource;

/**
 * Validate a XML with INTERNAL DTD and use of the standard DOM and SAX parser of Sun implementation
 * 
 * Validate a XML with EXTERNAL XSD and use of the standard DOM and SAX parser of Sun implementation
 * 
 * @author huseyin
 * 
 */
public class validateDTD {

	public static void main(String[] args) {
		try {
/*
			{
				String xmlFileNameWithInternalDTD = args[1];

				System.out.println("\n-------- validateXMLWithDTDAndDOM --------");
				System.out.println("---> Result of validation With Internal DTD: "+validateXMLWithDTDAndDOM(xmlFileNameWithInternalDTD));

				System.out.println("\n-------- validateXMLWithDTDAndSAX --------");
				System.out.println("---> Result of validation With Internal DTD: "+validateXMLWithDTDAndSAX(xmlFileNameWithInternalDTD));
			}
*/		
			{
				String xmlFileNameWithExternalDTD = args[0];
				System.out.println("\n-------- validateXMLWithDTDAndDOM --------");
				System.out.println("---> Result of validation With External DTD: "+validateXMLWithDTDAndDOM(xmlFileNameWithExternalDTD));
/*
				System.out.println("\n-------- validateXMLWithDTDAndSAX --------");
				System.out.println("---> Result of validation With External DTD: "+validateXMLWithDTDAndSAX(xmlFileNameWithExternalDTD));
*/
				
			}
			
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	/**
	 * Validate a XML with DTD and use of the standard DOM parser of Sun implementation
	 * 
	 * @param xmlFileName
	 * @return
	 */
	public static boolean validateXMLWithDTDAndDOM(String xmlFileName) {
		try {
	         DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	         // Enable the document validation as the document is being parsed.
	         factory.setValidating(true);
	         factory.setNamespaceAware(true);

	         DocumentBuilder builder = factory.newDocumentBuilder();
	         builder.setErrorHandler(new MyErrorHandler());
	   
	         // Generates a Document object tree
	         builder.parse(new InputSource(xmlFileName));
	         
//	         Document xmlDocument = builder.parse(new InputSource(xmlFileName));
//	         DOMSource source = new DOMSource(xmlDocument);
//	         StreamResult result = new StreamResult(System.out);
//	         //
//	         TransformerFactory transformerFactory = TransformerFactory.newInstance();
//	         Transformer transformer = transformerFactory.newTransformer();
//	         transformer.transform(source, result);
	         
	         return true;
	         
	      } catch (Throwable e) {
	         e.printStackTrace();
	         return false;
	      }
	}
}