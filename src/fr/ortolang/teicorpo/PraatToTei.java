/**
 * reading praat files
 * file taken from ELAN open sources
 * adaptation by Christophe Parisse
 */

package fr.ortolang.teicorpo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * local temporary structures
 * description of relation between tiers
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
		if (t.equals("-")) return LgqType.ROOT;
		if (t.equals("assoc")) return LgqType.SYMB_ASSOC;
		if (t.equals("incl")) return LgqType.INCLUDED;
		if (t.equals("symbdiv")) return LgqType.SYMB_DIV;
		if (t.equals("timediv")) return LgqType.TIME_DIV;
		if (t.equals("point")) return LgqType.POINT;
		if (t.equals("timeint")) return LgqType.TIME_INT;
		return null;
	}
}

/**
 * local parameters for PraatToTei
 * @author cp
 *
 */
class PraatParams {
	String mediaName;
	String encoding;
	boolean detectEncoding;
	public PraatParams() {
		mediaName = null;
		encoding = null;
		detectEncoding = true;
	}
}

/**
 * A class to extract annotations from a Praat .TextGrid file.
 * Only "IntervalTier"s and "TextTier"s are supported.
 * The expected format is roughly like below, but the format is only loosely checked.
 * 
 * 1 File type = "ooTextFile"
 * 2 Object class = "TextGrid"
 * 3 
 * 4 xmin = 0 
 * 5 xmax = 36.59755102040816 
 * 6 tiers? &lt;exists&; 
 * 7 size = 2 
 * 8 item []: 
 * 9     item [1]:
 * 10         class = "IntervalTier" 
 * 11         name = "One" 
 * 12         xmin = 0 
 * 13         xmax = 36.59755102040816 
 * 14         intervals: size = 5 
 * 15         intervals [1]:
 * 16             xmin = 0 
 * 17             xmax = 1 
 * 18             text = "" 
 * 
 * @version Feb 2013 the short notation format (roughly the same lines without the keys and the indentation)
 * is now also supported
 */
public class PraatToTei {
	static String EXT = ".textgrid";
	
    private final char brack = '[';
    private final String eq = "=";
    private final String item = "item";
    private final String cl = "class";
    private final String tierSpec = "IntervalTier";
    private final String textTierSpec = "TextTier";
    private final String nm = "name";
    private final String interval = "intervals";
    private final String min = "xmin";
    private final String max = "xmax";
    private final String tx = "text";
    private final String points = "points";
    private final String time = "time";
    private final String mark = "mark";
    private final String number = "number";
    private final String escapedInnerQuote = "\"\"";
    private final String escapedOuterQuote = "\"\"\"";
    
    private boolean includeTextTiers = false;
	private String encoding;
    
    private File gridFile;
    private Map<String, String> tierNames;
    private Map<String, ArrayList<Annot>> annotationMap;
    private PraatSpecialChars lookUp;
    
    private enum SN_POSITION {// short notation line position
    	OUTSIDE,// not in any type of tier
    	NEXT_IS_NAME,// next line is a tier name
    	NEXT_IS_MIN,// next line is the min time of interval
    	NEXT_IS_MAX,// next line is the max time of interval
    	NEXT_IS_TEXT,// next line is the text of an interval
    	NEXT_IS_TIME,// next line is the time of a point annotation
    	NEXT_IS_MARK,// next line is the text of a point annotation
    	NEXT_IS_TOTAL_MIN,// next line is the overall min of a tier, ignored
    	NEXT_IS_TOTAL_MAX,// next line is the overall max of a tier, ignored
    	NEXT_IS_SIZE// next line is the number of annotations of a tier, ignored
    }; 

    /**
     * Creates a new Praat TextGrid parser for the file at the specified path
     *
     * @param fileName the path to the file
     *
     * @throws IOException if the file can not be read, for whatever reason
     */
    public PraatToTei(String fileName) throws IOException {
        this(fileName, false, 1);
    }
    
    /**
     * Creates a new Praat TextGrid parser for the file at the specified path. 
     *
     * @param fileName the path to the file
     * @param includeTextTiers if true "TextTiers" will also be parsed
     * @param pointDuration the duration of annotations if texttiers are also parsed  
     *
     * @throws IOException if the file can not be read, for whatever reason
     */
    public PraatToTei(String fileName, boolean includeTextTiers, int pointDuration) 
        throws IOException {
        if (fileName != null) {
            gridFile = new File(fileName);
        }
        this.includeTextTiers = includeTextTiers;
        parse();
    }
    
    /**
     * Creates a new Praat TextGrid parser for the file at the specified path. 
     *
     * @param fileName the path to the file
     * @param includeTextTiers if true "TextTiers" will also be parsed
     * @param pointDuration the duration of annotations if texttiers are also parsed  
     * @param encoding the character encoding of the file
     *
     * @throws IOException if the file can not be read, for whatever reason
     */
    public PraatToTei(String fileName, boolean includeTextTiers, int pointDuration, String encoding) 
        throws IOException {
        if (fileName != null) {
            gridFile = new File(fileName);
        }
        this.includeTextTiers = includeTextTiers;
        this.encoding = encoding;
                
        parse();
    }

    /**
     * Creates a new Praat TextGrid parser for the specified file.
     *
     * @param gridFile the TextGrid file
     *
     * @throws IOException if the file can not be read, for whatever reason
     */
    public PraatToTei(File gridFile) throws IOException {
        this(gridFile, false, 1);
    }

    /**
     * Creates a new Praat TextGrid parser for the specified file.
     *
     * @param gridFile the TextGrid file
     * @param includeTextTiers if true "TextTiers" will also be parsed
     * @param pointDuration the duration of annotations if texttiers are also parsed  
     * 
     * @throws IOException if the file can not be read, for whatever reason
     */
    public PraatToTei(File gridFile, boolean includeTextTiers, int pointDuration) throws IOException {
        this(gridFile, includeTextTiers, pointDuration, null);
    }
    
    /**
     * Creates a new Praat TextGrid parser for the specified file.
     *
     * @param gridFile the TextGrid file
     * @param includeTextTiers if true "TextTiers" will also be parsed
     * @param pointDuration the duration of annotations if texttiers are also parsed  
     * @param encoding the character encoding of the file
     * 
     * @throws IOException if the file can not be read, for whatever reason
     */
    public PraatToTei(File gridFile, boolean includeTextTiers, int pointDuration, 
    		String encoding) throws IOException {
        this.gridFile = gridFile;
        
        this.includeTextTiers = includeTextTiers;
        this.encoding = encoding;
        
        parse();
    }
    
    /**
     * Returns a list of detected interval tiers.
     *
     * @return a list of detected interval tiernames
     */
    public Map<String, String> getTierNames() {
        return tierNames;
    }

    /**
     * Returns a list of annotation records for the specified tier.
     *
     * @param tierName the name of the tier
     *
     * @return the annotation records of the specified tier
     */
    public ArrayList<Annot> getAnnotationRecords(String tierName) {
        if ((tierName == null) || (annotationMap == null)) {
            return null;
        }

        ArrayList<Annot> value = annotationMap.get(tierName);

        if (value instanceof ArrayList) {
            return value;
        }

        return null;
    }
    
    /**
     * Reads a few lines and returns whether the file is in short notation.
     * 
     * @param reader the reader object
     * @return true if in short text notation, false otherwise
     */
    private boolean isShortNotation(BufferedReader reader) throws IOException {
    	if (reader == null) {
    		return false;
    	}
    	
    	String line;
    	int lineCount = 0;
    	
    	boolean xmin = false, xmax = false, tiers = false;// are the keys xmin and xmax and tiers? found
    	
    	while ((line = reader.readLine()) != null && lineCount < 5) {
    		if (line.length() == 0) {
    			continue;// skip empty lines
    		}
    		
    		if (lineCount == 2) {
    			xmin = (line.indexOf(min) > -1);
    		}
    		if (lineCount == 3) {
    			xmax = (line.indexOf(max) > -1);
    		}
    		if (lineCount == 4) {
    			tiers = (line.indexOf("tiers?") > -1);
    		}
    		lineCount++;   		
    	}
    	
    	return (!xmin && !xmax && !tiers);
    }

    /**
     * Parses the file and extracts interval tiers with their annotations.
     *
     * @throws IOException if the file can not be read for any reason
     */
    private void parse() throws IOException {
        if ((gridFile == null) || !gridFile.exists()) {
            System.err.println("No existing file specified.");
            System.exit(1);
        }

        BufferedReader reader = null;

        try {
        	if (encoding == null) {      	
        		reader = new BufferedReader(new InputStreamReader(
                        new FileInputStream(gridFile)));
        	} else {
        		try {
        			reader = new BufferedReader(new InputStreamReader(
                            new FileInputStream(gridFile), encoding));
        		} catch (UnsupportedEncodingException uee) {
        			System.err.println("Unsupported encoding: " + uee.getMessage());
        			reader = new BufferedReader(new InputStreamReader(
                            new FileInputStream(gridFile)));
        		}
        	}
            // Praat files on Windows and Linux are created with encoding "Cp1252"
            // on Mac with encoding "MacRoman". The ui could/should be extended
            // with an option to specify the encoding
            // InputStreamReader isr = new InputStreamReader(
            //        new FileInputStream(gridFile));
            // System.out.println("Encoding: " + isr.getEncoding());
        	// System.out.println("Read encoding: " + encoding);
        	System.out.println("Read encoding: " + encoding);
        	
        	boolean isShortNotation = isShortNotation(reader);
        	System.out.println("Praat TextGrid is in short notation: " + isShortNotation);
        	
        	if (isShortNotation) {
        		parseShortNotation(reader);
        		return;
        	}
        	
            tierNames = new HashMap<String, String>(4);
            annotationMap = new HashMap<String, ArrayList<Annot>>(4);

            ArrayList<Annot> records = new ArrayList<Annot>();
            Annot record = null;

            String line;
            //int lineNum = 0;
            String tierName = null;
            String annValue = "";
            String begin = "-1";
            String end = "-1";
            boolean inTier = false;
            boolean inInterval = false;
            boolean inTextTier = false;
            boolean inPoints = false;
            int eqPos = -1;

            
            while ((line = reader.readLine()) != null) {
                //lineNum++;
                //System.out.println(lineNum + " " + line);

                if ((line.indexOf(cl) >= 0) && 
                        ((line.indexOf(tierSpec) > 5) || (line.indexOf(textTierSpec) > 5))) {
                    // check if we have to include text (point) tiers
                    if (line.indexOf(textTierSpec) > 5) {
                        if (includeTextTiers) {
                            inTextTier = true;
                        } else {
                            inTextTier = false;
                            inTier = false;
                            continue;
                        }
                    }
                    // begin of a new tier
                    records = new ArrayList<Annot>();
                    inTier = true;
            
                    continue;
                }

                if (!inTier) {
                    continue;
                }

                eqPos = line.indexOf(eq);
                
                if (inTextTier) {
                    // text or point tier
                    if (eqPos > 0) {
	                    // split and parse
	                    if (!inPoints && (line.indexOf(nm) >= 0) &&
	                            (line.indexOf(nm) < eqPos)) {
	                        tierName = extractTierName(line, eqPos);
	
	                        if (!annotationMap.containsKey(tierName)) {
	                            annotationMap.put(tierName, records);
	                            tierNames.put(tierName, "TextTier");
	                            System.out.println("Point Tier detected: " + tierName);
	                        } else {
	                        	// the same (sometimes empty) tiername can occur more than once, rename
	                        	int count = 2;
	                        	String nextName = "";
	                        	for (; count < 50; count++) {
	                        		nextName = tierName + "-" + count;
	                        		if (!annotationMap.containsKey(nextName)) {
	    	                            annotationMap.put(nextName, records);
	    	                            tierNames.put(nextName, "TextTier");
	    	                            System.out.println("Point Tier detected: " + tierName + " and renamed to: " + nextName);
	    	                            break;
	                        		}
	                        	}
	                        }
	
	                        continue;
	                    } else if (!inPoints) {
	                        continue;
	                    } else if (line.indexOf(time) > -1 || line.indexOf(number) > -1) {
	                        begin = extractTime(line, eqPos);
	                        //System.out.println("B: " + begin);
	                    } else if (line.indexOf(mark) > -1) {
	                        // extract value
	                        annValue = extractTextValue(line, eqPos);
	                        // finish and add the annotation record
	                        inPoints = false;
	                        //System.out.println("T: " + annValue);
	                        record = new Annot(tierName, annValue,
	                                begin, "-1" /* begin + pointDuration */);
	                        records.add(record);
	                        // reset
	                        annValue = "";
	                        begin = "-1";
	                    }
	                } else {
	                    // points??
	                    if ((line.indexOf(points) >= 0) &&
	                            (line.indexOf(brack) > points.length())) {
	                        inPoints = true;
	
	                        continue;
	                    } else {
	                        if ((line.indexOf(item) >= 0) &&
	                                (line.indexOf(brack) > item.length())) {
	                            // reset
	                            inTextTier = false;
	                            inPoints = false;
	                        }
	                    }
	                } // end point tier
                } else {
                    // interval tier
	                if (eqPos > 0) {
	                    // split and parse
	                    if (!inInterval && (line.indexOf(nm) >= 0) &&
	                            (line.indexOf(nm) < eqPos)) {
	                        tierName = extractTierName(line, eqPos);
	
	                        if (!annotationMap.containsKey(tierName)) {
	                            annotationMap.put(tierName, records);
	                            tierNames.put(tierName, "IntervalTier");
	                            System.out.println("Tier detected: " + tierName);
	                        } else {
	                        	// the same (sometimes empty) tiername can occur more than once, rename
	                        	int count = 2;
	                        	String nextName = "";
	                        	for (; count < 50; count++) {
	                        		nextName = tierName + "-" + count;
	                        		if (!annotationMap.containsKey(nextName)) {
	    	                            annotationMap.put(nextName, records);
	    	                            tierNames.put(nextName, "IntervalTier");
	    	                            System.out.println("Tier detected: " + tierName + " and renamed to: " + nextName);
	    	                            break;
	                        		}
	                        	}
	                        }
	
	                        continue;
	                    } else if (!inInterval) {
	                        continue;
	                    } else if (line.indexOf(min) > -1) {
	                        begin = extractTime(line, eqPos);
	                        //System.out.println("B: " + begin);
	                    } else if (line.indexOf(max) > -1) {
	                        end = extractTime(line, eqPos);
	                        //System.out.println("E: " + end);
	                    } else if (line.indexOf(tx) > -1) {
	                        // extract value
	                        annValue = extractTextValue(line, eqPos);
	                        // finish and add the annotation record
	                        inInterval = false;
	                        //System.out.println("T: " + annValue);
	                        record = new Annot(tierName, annValue,
	                                begin, end);
	                        if(Utils.isNotEmptyOrNull(annValue)){
	                        	records.add(record);
	                        }
	                        // reset
	                        annValue = "";
	                        begin = "-1";
	                        end = "-1";
	                    }
	                } else {
	                    // interval?
	                    if ((line.indexOf(interval) >= 0) &&
	                            (line.indexOf(brack) > interval.length())) {
	                        inInterval = true;
	
	                        continue;
	                    } else {
	                        if ((line.indexOf(item) >= 0) &&
	                                (line.indexOf(brack) > item.length())) {
	                            // reset
	                            inTier = false;
	                            inInterval = false;
	                        }
	                    }
	                }
                }
            }

            reader.close();
        } catch (IOException ioe) {
            if (reader != null) {
                reader.close();
            }

            throw ioe;
        } catch (Exception fe) {
            if (reader != null) {
                reader.close();
            }

            throw new IOException("Error occurred while reading the file: " +
                fe.getMessage());
        }
    }
    
    /**
     * Parses the short notation of a Praat TextGrid file. 
     * Handling completely separated from the long notation.
     * 
     * @param reader the (configured) reader
     */
    private void parseShortNotation(BufferedReader reader) throws IOException {
    	if (reader == null) {
    		throw new IOException("The reader object is null, cannot read from the file.");
    	}
    	
        tierNames = new HashMap<String, String>(4);
        annotationMap = new HashMap<String, ArrayList<Annot>>(4);

        ArrayList<Annot> records = new ArrayList<Annot>();
        Annot record = null;

        String line;
        String tierName = null;
        String annValue = "";
        String begin = "-1";
        String end = "-1";
        
        boolean inTextTier = false;// in text tier
        boolean inTier = false;// in interval tier
        int eqPos = -1;
        
        SN_POSITION linePos = SN_POSITION.OUTSIDE;

        while ((line = reader.readLine()) != null) {
        	if (line.length() == 0) {
        		continue;
        	}
        	
    		if (line.indexOf(tierSpec) > -1) {
    			linePos = SN_POSITION.NEXT_IS_NAME;
    			inTier = true;
    			inTextTier = false;
    			continue;
    		}
    		if (line.indexOf(textTierSpec) > -1) {
    			linePos = SN_POSITION.NEXT_IS_NAME;
    			inTier = false;
    			inTextTier = true;
    			continue;
    		}
        	
        	if (linePos == SN_POSITION.NEXT_IS_NAME) {// tier name on this line
        		if (!inTier && !inTextTier) {
        			linePos = SN_POSITION.NEXT_IS_TOTAL_MIN;
        			continue;
        		}
        		
        		if (inTier || (inTextTier && includeTextTiers)) {
	        		tierName = removeQuotes(line);
	        		if (tierName.length() == 0) {
	        			tierName = "Noname";
	        		}
	        		
	        		records = new ArrayList<Annot>();	
	        		
	                if (!annotationMap.containsKey(tierName)) {
	                    annotationMap.put(tierName, records);
	                    if (inTextTier) {
		                    tierNames.put(tierName, "TextTier");
	                    	System.out.println("Point Tier detected: " + tierName);
	                    } else {
		                    tierNames.put(tierName, "IntervalTier");
	                    	System.out.println("Interval Tier detected: " + tierName);
	                    }
	                } else {
	                	// the same (sometimes empty) tiername can occur more than once, rename
	                	int count = 2;
	                	String nextName = "";
	                	for (; count < 50; count++) {
	                		nextName = tierName + "-" + count;
	                		if (!annotationMap.containsKey(nextName)) {
	                            annotationMap.put(nextName, records);
	                            if (inTextTier) {
	        	                    tierNames.put(nextName, "TextTier");
	                            	System.out.println("Point Tier detected: " + tierName + " and renamed to: " + nextName);
	                            } else {
	        	                    tierNames.put(nextName, "IntervalTier");
	                            	System.out.println("Interval Tier detected: " + tierName + " and renamed to: " + nextName);
	                            }
	                            break;
	                		}
	                	}
	                }
        		}
        		
                linePos = SN_POSITION.NEXT_IS_TOTAL_MIN;
                continue;
        	}
        	
        	if (linePos == SN_POSITION.NEXT_IS_TOTAL_MIN) {
        		linePos = SN_POSITION.NEXT_IS_TOTAL_MAX;
        		continue;
        	}
        	
        	if (linePos == SN_POSITION.NEXT_IS_TOTAL_MAX) {
        		linePos = SN_POSITION.NEXT_IS_SIZE;
        		continue;
        	}
        	
        	if (linePos == SN_POSITION.NEXT_IS_SIZE) {
        		if (inTextTier) {
        			linePos = SN_POSITION.NEXT_IS_TIME;
        		} else {// interval tier
        			linePos = SN_POSITION.NEXT_IS_MIN;
        		}
        		continue;
        	}
        	// point text tiers
        	if (linePos == SN_POSITION.NEXT_IS_TIME) {
        		if (includeTextTiers) {
        			// hier extract time
        			begin = extractTime(line, eqPos);// eqPos = -1
        		}
        		linePos = SN_POSITION.NEXT_IS_MARK;
        		continue;
        	}
        	
        	if (linePos == SN_POSITION.NEXT_IS_MARK) {
        		if (includeTextTiers) {
                    annValue = extractTextValue(line, eqPos);// eqPos = -1
                    // finish and add the annotation record
                    record = new Annot(tierName, annValue,
                            begin, "-1" /* begin + pointDuration */);
                    if(Utils.isNotEmptyOrNull(annValue)){
                    	records.add(record);
                    }
                    // reset
                    annValue = "";
                    begin = "-1";
        		}
        		linePos = SN_POSITION.NEXT_IS_TIME;
        		continue;
        	}
        	// interval tiers
        	if (linePos == SN_POSITION.NEXT_IS_MIN) {
        		begin = extractTime(line, eqPos);// eqPos = -1
        		 linePos = SN_POSITION.NEXT_IS_MAX;
        		 continue;
        	}
        	
        	if (linePos == SN_POSITION.NEXT_IS_MAX) {
        		end = extractTime(line, eqPos);// eqPos = -1
        		linePos = SN_POSITION.NEXT_IS_TEXT;
        		continue;
        	}
        	
        	if (linePos == SN_POSITION.NEXT_IS_TEXT) {
                // extract value
                annValue = extractTextValue(line, eqPos);// eqPos = -1
                // finish and add the annotation record
                record = new Annot(tierName, annValue,
                        begin, end);
                if(Utils.isNotEmptyOrNull(annValue)){
                	records.add(record);
                }
                // reset
                annValue = "";
                begin = "-1";
                end = "-1";
                
                linePos = SN_POSITION.NEXT_IS_MIN;
                continue;
        	}
        }
    }

    /**
     * Extracts the tiername from a line.
     *
     * @param line the line
     * @param eqPos the indexof the '=' sign
     *
     * @return the tier name
     */
    private String extractTierName(String line, int eqPos) {
        if (line.length() > (eqPos + 1)) {
            String name = line.substring(eqPos + 1).trim();

            if (name.length() < 3) {
            	if ("\"\"".equals(name)) {
            		return "Noname";
            	}
            	
                return name;
            }

            return removeQuotes(name);
        }

        return line; // or null??
    }

    /**
     * Extracts the text value and, if needed, converts Praat's special
     * character sequences into unicode chars.
     *
     * @param value the text value
     * @param eqPos the index of the equals sign
     *
     * @return the annotation value. If necessary Praat's special symbols have
     *         been converted  to Unicode.
     */
    private String extractTextValue(String value, int eqPos) {
        if (value.length() > (eqPos + 1)) {
            String rawV = removeQuotes(value.substring(eqPos + 1).trim()); // should be save

            if (lookUp == null) {
                lookUp = new PraatSpecialChars();
            }
            rawV = lookUp.replaceIllegalXMLChars(rawV);
            
            if (rawV.indexOf('\\') > -1) {
                // convert
//                if (lookUp == null) {
//                    lookUp = new PraatSpecialChars();
//                }

                return lookUp.convertSpecialChars(rawV);
            }

            return rawV;
        }

        return "";
    }

    /**
     * Extracts a double time value, multiplies by 1000 (sec to ms) and
     * converts to Time (string).
     *
     * @param value the raw value
     * @param eqPos the index of the equals sign
     *
     * @return the time value rounded to milliseconds
     */
    private String extractTime(String value, int eqPos) {
        if (value.length() > (eqPos + 1)) {
            String v = value.substring(eqPos + 1).trim();
            return v;
        }

        return "-1";
    }

    /**
     * Removes a beginning and end quote mark from the specified string. Does
     * no null check nor are spaces trimmed.
     * 
     * @version Feb 2013 added handling for outer escaped quotes (""") and inner escaped quotes ("")
     *
     * @param value the value of which leading and trailing quote chars should
     *        be removed
     *
     * @return the value without the quotes
     */
    private String removeQuotes(String value) {
    	boolean removeOuterQuotes = true;
    	if (value.startsWith(escapedOuterQuote) && value.endsWith(escapedOuterQuote)) {
    		removeOuterQuotes = false;
    	}
    	// replace all """ sequences by a single "
    	value = value.replaceAll(escapedOuterQuote, "\"");
    	value = value.replaceAll(escapedInnerQuote, "\"");
    	
    	if (removeOuterQuotes) {
	        if (value.charAt(0) == '"') {
	            if (value.charAt(value.length() - 1) == '"' && value.length() > 1) {
	                return value.substring(1, value.length() - 1);
	            } else {
	                return value.substring(1);
	            }
	        } else {
	            if (value.charAt(value.length() - 1) == '"') {
	                return value.substring(0, value.length() - 1);
	            } else {
	                return value;
	            }
	        }
    	} else {
    		return value;
    	}
    }
    
    public static void displayAnnnotations(PraatToTei ptg) {
		/*
		 * display annotations (for debugging purposes)
		 */
		
		System.out.println("ANNOTATIONS");
		for (Map.Entry<String, ArrayList<Annot>> element : ptg.annotationMap.entrySet()) {
			System.out.println(element.getKey() + " :- ");
			ArrayList<Annot> l = element.getValue();
			for (int j=0; j<l.size(); j++) {
				System.out.println("    " + j + " :- " + l.get(j));
			}
		}
    }

    public static boolean convertFromPraatToTei(String inputfile, String outputfile, String encoding, ArrayList<DescTier> ldt, String mediaName) {
		try {
			/*
			 * try to find a param file
			 */
			String bn = Utils.basename(inputfile);
			File fnb = new File(bn + ".paramtei");
			if (fnb.exists()) {
				if (ldt.size()>0) {
					System.out.println("Attention paramÃ¨tres commande peut-Ãªtre ignorÃ©s pour " + inputfile);
				}
				System.out.println("Utilisation du fichier paramÃ¨tres " + fnb.toString());
				PraatParams prs = new PraatParams();
				prs.encoding = encoding;
				prs.mediaName = mediaName;
				if (addParams(fnb.toString(), ldt, prs) == false)
					System.err.println("Erreur de traitement du fichier paramÃ¨tres: " + fnb.toString());
			}
			PraatToTei ptg = new PraatToTei(inputfile, true, 100, encoding);
			System.out.println("Fichier " + inputfile + " Encoding: " + (encoding!=null?encoding:"par dÃ©faut"));
			/*
			 * construire un hierarchic trans
			 */
			HierarchicTrans ht = new HierarchicTrans();
			/*
			 * construire tiers info
			 */
			File inputFileObject = new File(inputfile);
			ht.fileName = inputFileObject.getName();
			ht.filePath = inputFileObject.getAbsolutePath();
			
			if(Utils.isNotEmptyOrNull(mediaName)){
				Media m = new Media("", (new File(mediaName)).getAbsolutePath());
				ht.metaInf.medias.add(m);
			} else {
				String url = Utils.findClosestMedia("", inputfile, "audio");
				Media m = new Media("", (new File(url)).getAbsolutePath());
				ht.metaInf.medias.add(m);
			}
			System.out.println("TIERS Information");
			for (Map.Entry<String, String> element : ptg.tierNames.entrySet()) {
				TierInfo value = new TierInfo();
				value.participant = element.getKey();
				// find if the tier is in the list of constraints
				boolean found = false;
				for (int j=0; j<ldt.size(); j++) {
					//System.out.println("    " + j + " :- " + ldt.get(j).print());
					DescTier a = ldt.get(j);
					if (a.tier.equalsIgnoreCase(value.participant)) {
						value.type.lgq_type_id = value.participant;
						value.type.constraint = DescTier.whichType(a.type);
						if(value.type.constraint == LgqType.SYMB_ASSOC || value.type.constraint == LgqType.SYMB_DIV){
							value.type.time_align = false;
						}
						else{
							value.type.time_align = true;
						}
						value.parent = a.parent;
						found = true;
						System.out.println("TIER: " + a.toString());
						break;
					}
				}
				if (!found) {
					value.type.constraint = LgqType.ROOT;
					value.type.time_align = true;
					// si element.getValue() == TextTier alors des points sinon des intervalles
					System.out.println("TIER: " + element.getKey() + " : ROOT");
				}
				ht.tiersInfo.put(element.getKey(), value);
			}
			/*
			 * construire les informations sur les enfants (== dependantsNames) dans ht.tiersInfo
			 */
			TierInfo.buildDependantsNames(ht.tiersInfo);
			/*
			for(Map.Entry<String , TierInfo> entry : ht.tiersInfo.entrySet()){
				System.out.println(entry.getKey() + " " + entry.getValue().toString());
			}
			*/
			ht.initial_format = "Praat";
			ht.metaInf.version = Utils.versionSoft;
			ht.metaInf.time_units = "s";
			
			/*
			 * construire timeline
			 * not necessary will be done later when printing the file
			
			int praatid = 0;
			//unitÃ© timeline
			ht.metaInf.time_units = "s";
			for (Map.Entry<String, ArrayList<Annot>> element : ptg.annotationMap.entrySet()) {
				// System.out.println(element.getKey() + " :- ");
				ArrayList<Annot> l = element.getValue();
				for (int j=0; j<l.size(); j++) {
					//System.out.println("    " + j + " :- " + l.get(j));
					Annot a = l.get(j);
					praatid++;
					String id = "b" + praatid;
					ht.timeline.put(id, a.start);
					ht.times.add(a.start);
					a.start = "#" + id;
					praatid ++;
					id = "e" + praatid;
					ht.timeline.put(id, a.end);
					ht.times.add(a.end);
					a.end = "#" + id;
				}
			}
			 */
			
			// displayAnnotations(ptg); 
			
			for(Entry<String, TierInfo> tierInfo : ht.tiersInfo.entrySet()){
				Participant p = new Participant();
				p.id = tierInfo.getKey();
				ht.metaInf.participants.add(p);
			}
			ht.partionRepresentationToHierachic(ptg.annotationMap);
			HT_ToTei hiertransToTei = new HT_ToTei(ht);
			Utils.createFile(outputfile, hiertransToTei.docTEI);
			
		} catch (IOException ioe) {
			System.err.println("Interrompu!"); // ioe.toString());
			return false;
		}
		return true;
    }
    
	public static void usage() {
		System.err.println("Description: PraatToTei convertit un fichier au format Praaat en un fichier au format TEI");
		System.err.println("Usage: PraatToTei [-options] <file>" + EXT);
		System.err.println("         :-i nom du fichier ou repertoire oÃ¹ se trouvent les fichiers Praat Ã  convertir");
		System.err.println("            (les fichiers ont pour extension " + EXT + ")");
		System.err.println("         :-o nom du fichier de sortie au format TEI (.xml) ou du repertoire de rÃ©sultats");
		System.err.println("            si cette option n'est pas spÃ©cifiÃ©, le fichier de sortie aura le mÃªme nom");
		System.err.println("               que le fichier d'entrÃ©e, avec l'extension .xml;");
		System.err.println("            si on donne un repertoire comme input et que cette option n'est pas spÃ©cifiÃ©e,");
		System.err.println("               les rÃ©sultats seront stockÃ©es dans le mÃªme dossier que l'entrÃ©e.");
		System.err.println("         :-p fichier_de_parametres: contient les paramÃ¨tres sous leur format ci-dessous, un jeu de paramÃ¨tre par ligne.");
		System.err.println("         :-m nom/adresse du fichier mÃ©dia");
		System.err.println("         :-e encoding (par dÃ©faut detect encoding)");
		System.err.println("         :-d default UTF8 encoding ");
		System.err.println("         :-t tiername type parent (describe relations between tiers)");
		System.err.println("             types autorisÃ©s: - assoc incl symbdiv timediv");
		System.err.println("         :-usage ou -help ou -h = affichage de ce message");
		System.exit(1);
	}

	public static void main (String[] args) throws Exception{
		Utils.printVersionMessage();

		String input = null;
		String output = null;
		PraatParams prs = new PraatParams();
		
		ArrayList<DescTier> ldt = new ArrayList<DescTier>();
		// parcours des arguments
		if (args.length == 0) {
			System.err.println("Vous n'avez spÃ©cifiÃ© aucun argument.\n");
			usage();
		} else {
			for (int i = 0; i < args.length; i++) {
				try {
					if (args[i].equals("-i")) {
						i++;
						if (i >= args.length) usage();
						input = args[i];
					} else if (args[i].equals("-o")) {
						i++;
						if (i >= args.length) usage();
						output = args[i];
					} else if (args[i].equals("-p")) {
						i++;
						if (i >= args.length) usage();
						if (addParams(args[i], ldt, prs) == false) usage();
					} else if (args[i].equals("--pure")) {
						Utils.teiStylePure = true;
					}
					else if (args[i].equals("-m")) {
						i++;
						if (i >= args.length) usage();
						prs.mediaName = args[i];
					}
					else if (args[i].equals("-e")) {
						i++;
						if (i >= args.length) usage();
						prs.encoding = args[i];
						prs.detectEncoding = false;
					} else if (args[i].equals("-t")) {
						DescTier d = new DescTier();
						i++;
						if (i >= args.length) usage();
						d.tier = args[i];
						i++;
						if (i >= args.length) usage();
						d.type = args[i];
						if (DescTier.whichType(args[i]) == null) {
							usage();
							System.exit(1);
						}
						i++;
						if (i >= args.length) usage();
						d.parent = args[i];
						ldt.add(d);
					} else if (args[i].equals("-d")) {
						prs.detectEncoding = false;
						prs.encoding = "UTF-8";
					} else if (args[i].equals("-h") || args[i].equals("-help") || args[i].equals("-usage")) {
						usage();
						System.exit(1);
					} else {
						System.out.println("Option inconnue: " + args[i]);
						System.exit(1);
					}
				} catch (Exception e) {
					usage();
				}
			}
		}

		File f = new File (input);

		input = f.getCanonicalPath();

		if (f.isDirectory()){
			File[] files = f.listFiles();

			if (output == null){
				if (input.endsWith("/")){
					output= input.substring(0, input.length()-1);
				}
				else{
					output = input;
				}
			}

			File outFile = new File(output);
			if(outFile.exists()){
				if(!outFile.isDirectory()){
					System.out.println("\n Erreur :"+ output + " est un fichier, vous devez spÃ©cifier un nom de dossier pour le stockage des rÃ©sultats. \n");
					usage();
					System.exit(1);
				}
			}

			if(!output.endsWith("/")){
				output += "/";
			}
			new File(output).mkdir();

			for (File file : files){
				if (file.getName().toLowerCase().endsWith(Utils.EXT_PUBLISH + EXT)) {
					System.out.printf("-- ignorÃ©: %s%n", file.getName());
				} else if(file.getName().toLowerCase().endsWith(EXT)){
//					System.out.printf("XX: %s%n", file.getName());
					String outputFileName = Utils.basename(file) + Utils.EXT;
					System.out.println(output+outputFileName);
					if (prs.detectEncoding) {
						prs.encoding = EncodingDetector.detect(file.getAbsolutePath());
						if (prs.encoding == null) {
							System.out.println("Could not detect encoding: use UTF-8");
							prs.encoding = "UTF-8";
						}
					}
					convertFromPraatToTei(file.getAbsolutePath(), output+outputFileName, prs.encoding, ldt, prs.mediaName);
				}
				else if(file.isDirectory()){
					args[0] = "-i";
					args[1] = file.getAbsolutePath();
					main(args);
				}
			}
		} else {
			if (output == null) {
				output = Utils.basename(input) + Utils.EXT;
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
				System.err.println("Le fichier d'entrÃ©e du programme doit avoir l'extension " + EXT);
				usage();
			}

			System.out.println("Reading " +input);
			try {
				if (prs.detectEncoding) {
					prs.encoding = EncodingDetector.detect(input);
					if (prs.encoding == null) {
						System.out.println("Could not detect encoding: use UTF-8");
						prs.encoding = "UTF-8";
					}
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
				// e.printStackTrace();
				System.exit(1);
			}
			convertFromPraatToTei(input, output, prs.encoding, ldt, prs.mediaName);
			System.out.println("New file TEI created: " + output);
		}
	}

	private static boolean addParams(String fn, ArrayList<DescTier> ldt, PraatParams pr) {
		List<String> ls = null;
		try {
			ls = Utils.loadTextFile(fn);
		} catch (IOException e) {
			System.err.println("Impossible de traiter le fichier: " + fn);
			return false;
		}
		for (int k=0; k<ls.size(); k++) {
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
						usage();
					d.tier = p[1];
					d.type = p[2];
					d.parent = p[3];
					ldt.add(d);
				} else {
					System.out.println("Format inconnu dans le fichier paramÃ¨tre: " + fn);
					return false;
				}
			}
		}
		return true;
	}
}
