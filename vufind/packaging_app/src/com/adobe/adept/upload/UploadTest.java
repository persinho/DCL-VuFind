/*************************************************************************
*
* ADOBE CONFIDENTIAL
* ___________________
*
*  Copyright 2010 Adobe Systems Incorporated
*  All Rights Reserved.
*
* NOTICE:  All information contained herein is, and remains
* the property of Adobe Systems Incorporated and its suppliers,
* if any.  The intellectual and technical concepts contained
* herein are proprietary to Adobe Systems Incorporated and its
* suppliers and are protected by trade secret or copyright law.
* Dissemination of this information or reproduction of this material
* is strictly forbidden unless prior written permission is obtained
* from Adobe Systems Incorporated.
**************************************************************************/
package com.adobe.adept.upload;

import java.io.File;
import java.io.FilenameFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Random;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.adobe.adept.client.XMLUtil;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


/**
 * UploadTest Class
 * <p>
 * Creates and sends a Package Request to a specified packaging server and
 * receives the server's response. The Package Request is an XML-based
 * communication that specifies information about the book being packaged, and
 * includes the book bytes in Base64 encoding, as well as an HMAC signature
 * based on a SharedSecret (see XMLUtil Class for more information on this) The
 * structure of the Package Request is shown below (elements marked
 * <tt>**OPT</tt> are optional):
 * </p>
 * 
 * <pre>
 *  &lt;package xmlns=&quot;http://ns.adobe.com/adept/&quot;&gt;
 *  	&lt;action&gt;**OPT add|replace&lt;/action&gt;
 *  	&lt;resource&gt;**OPT resource ID&lt;/resource&gt;
 *  	&lt;voucher&gt;**OPT voucher ID for GBLink fulfillment&lt;/voucher&gt;
 *  	&lt;resourceItem&gt;**OPT resource item index&lt;/resourceItem&gt;
 *  	&lt;fileName&gt;**OPT file name to use for this packaged resource&lt;/fileName&gt;
 *  	&lt;location&gt;**OPT upload location for packaged resource (file name or FTP URL) &lt;/location&gt;
 *  	&lt;src&gt;**OPT download location for the packaged resource (HTTP URL)&lt;/src&gt;
 *  	&lt;metadata xmlns:dc=&quot;http://purl.org/dc/elements/1.1/&quot;&gt;**OPT 
 *  		&lt;dc:title&gt;**OPT Book Title&lt;/dc:title&gt; 
 *  		&lt;dc:description&gt;**OPT Book Description&lt;/dc:description&gt; 
 *  		&lt;dc:language&gt;**OPT Book Language&lt;/dc:language&gt; 
 *  		&lt;dc:creator&gt;**OPT Book Creator&lt;/dc:creator&gt;
 *  		&lt;dc:publisher&gt;**OPT Book Publisher&lt;/dc:creator&gt; 
 *  		&lt;dc:format&gt;**OPT Book mimetype&lt;/dc:format&gt; 
 *  		&lt;dc:identifier&gt;**OPT Book identifier&lt;/dc:identifier&gt; 
 *  	&lt;/metadata&gt; 
 *  	&lt;permissions&gt;**OPT 
 *  		&lt;display&gt;**OPT rights elements&lt;/display&gt; 
 *  		&lt;play&gt;**OPT rights elements&lt;/play&gt; 
 *  		&lt;excerpt&gt;**OPT rights elements&lt;/excerpt&gt;
 *  		&lt;print&gt;**OPT rights elements&lt;/print&gt; 
 *  	&lt;/permissions&gt; 
 *  	&lt;dataPath&gt;**OPT instead of data element, specifies local eBook location on packaging server&lt;/dataPath&gt;
 *  	&lt;data&gt; Base64-encoded book bytes &lt;/data&gt; 
 *  	&lt;thumbnailLocation&gt;**OPT thumbnail upload location (file name or FTP URL)&lt;/thumbnailLocation&gt;
 *  	&lt;thumbnailData&gt;**OPT Base64-encoded thumbnail bytes &lt;/thumbnailData&gt; 
 *  	&lt;expiration&gt; W3CDTF expiration &lt;/expiration&gt; 
 *  	&lt;nonce&gt; Base64-encoded nonce &lt;/nonce&gt; 
 *  &lt;hmac&gt; Base64-encoded HMAC &lt;/hmac&gt; 
 *  &lt;/package&gt;
 * </pre>
 * 
 * <p>
 * When you run this tool, the first parameter MUST be the URL of the Packaging
 * server to send the Packaging Request to (EX:
 * "http://ugra.corp.adobe.com/packaging/Package") The second parameter MUST be
 * the file name of the target file to package or a directory name (EX:
 * "/Users/piotrk/documents/testPDF.pdf") If the tool is supplied with a
 * directory name, it will attempt to package every ".pdf" and ".epub" file
 * contained in the directory. It does not matter whether or not you include a
 * single slash (/) at the end of the directory name.
 * </p>
 * 
 * <p>
 * This tool accepts a number of parameter flags that allow the addition of DC
 * metadata, permissions, and a thumbnail image into the Package Request. The
 * order of the parameter flags can be arbitrary. Below is a list of accepted
 * flags:
 * </p>
 * 
 * <pre>
 *  PASSWORD FLAG:
 *  -pass = the next argument contains the server password
 *  
 *  DC METADATA FLAGS: 
 *  -title = the next argument contains the DEFAULT dc:title value
 *  -description = the next argument contains the DEFAULT dc:description value
 *  -language = the next argument contains the DEFAULT dc:language value
 *  -creator = the next argument contains the DEFAULT dc:creator value 
 *  -publisher = the next argument contains the DEFAULT dc:publisher value 
 *  -format = the next argument contains the DEFAULT dc:format value
 *  -identifier = the next argument contains the DEFAULT dc:identifier value
 *  
 *  MISC FLAGS: 
 *  -png = looks for fileNameNoExt.png to upload as a thumbnail
 *  -jpeg = looks for fileNameNoExt.jpeg to upload as a thumbnail 
 *  -jpg = looks for fileNameNoExt.jpg to upload as a thumbnail 
 *  -gif = looks for fileNameNoExt.gif to upload as a thumbnail 
 *  -xml = looks for fileNameNoExt.xml to use as XML source 
 *  -datapath = enables dataPath Mode for the tool
 *  -verbose = displays content of package request and detailed server response
 *  -version = displays tool's version number
 *  -? = displays the list of accepted command-line flags
 * </pre>
 * 
 * <p>
 * When the DC parameter flags are used, the parameter immediately following the
 * flag is the value that is assigned as a default for that DC metadata element.
 * (EX: if you run the tool with "-format application/pdf", the default value of
 * the DC metadata element &lt;format&gt; will be set to application/pdf)
 * </p>
 * 
 * <p>
 * When one of the thumbnail flags is used (-png, -jpeg, -jpg, or -gif), the
 * tool will look for a thumbnail image with the corresponding extension in the
 * same directory as the target file to be packaged. (EX: if you run the tool
 * with -png and the file name foo.epub, the tool will look for foo.png to
 * package as a thumbnail for the .epub book) If the tool is supplied with a
 * directory, it will look for a thumbnail image with the corresponding
 * extension for each file in the directory. If you supply the tool with a
 * directory, and you want to package files with different thumbnail extensions,
 * you need to include all the applicable flags. (EX: if you want to package
 * one.epub with one.jpg, two.pdf with two.png, and three.epub with three.jpeg,
 * you must run the tool with -jpg -jpeg -png)
 * </p>
 * 
 * <p>
 * If the -datapath flag is used, dataPath Mode is enabled for the Tool.
 * dataPath Mode is designed to make packaging large books simpler. Instead of
 * packaging a book that's local on this machine, dataPath lets the user specify
 * a location that's local on the packaging server where the file resides. When
 * dataPath Mode is engaged, the tool will no longer look for .epub and .pdf
 * files to package, and instead will look for .xml files in the directory that
 * it was passed. In order to use dataPath Mode, you must pass the tool a
 * directory (not a file). The XML config files must include a &lt;dataPath&gt;
 * element containing the local path to the book to be packaged (on the
 * packaging server). This &lt;dataPath&gt; element will be used instead of the
 * &lt;data&gt; element that is usually included in the request. A request may
 * not have both data and dataPath elements. If dataPath Mode is not engaged,
 * the Tool will ignore any dataPath elements it finds in the XML config files.
 * </p>
 * 
 * <p>
 * When the -verbose flag is used, the tool will display detailed content to the
 * console. The tool will display the path of the file being packaged, the path
 * of the thumbnail image used (if present), the path of the XML source used (if
 * present), the full Package Request, and the full server response. WARNING:
 * The Package Request includes the Base64-encoded book bytes, and using
 * -verbose will often overflow the default console buffer
 * </p>
 * 
 * <p>
 * When the -xml flag is used, the tool will look for an XML file with the .xml
 * extension in the same directory as the targe file to be packaged. (EX: if you
 * run the tool with -xml and the file name foo.epub, the tool will look for
 * foo.xml to use as XML source) The XML source file can contain specific DC
 * metadata and permissions that will be used for the book. If an XML source
 * file is found, the DC metadata within will be used in place of any default
 * values set by the DC parameter flags. Including an XML source file is the
 * only way to set permissions using this tool.
 * <p>
 * 
 * <p>
 * The XML source file MUST mimic the structure of the package request. The
 * following elements may be present in the XML Source file and will be
 * extracted from the source and added to the package request (if present): <br>
 * <br>
 * <tt>&lt;action&gt;</tt> - optional action add or replace. Default is add <br>
 * <tt>&lt;resource&gt;</tt> - optional resource ID <br>
 * <tt>&lt;voucher&gt;</tt> - optional voucher ID for GBLink fulfillment <br>
 * <tt>&lt;resourceItem&gt;</tt> - optional resource item index (for multiple
 * resource items in one request, which is not supported with this tool) <br>
 * <tt>&lt;fileName&gt;</tt> - optional file name to use for this packaged
 * resource <br>
 * <tt>&lt;location&gt;</tt> - optional upload location for packaged resource
 * (file name or FT URL) <br>
 * <tt>&lt;src&gt;</tt> - optional download location for the packaged resource
 * (HTTP URL) <br>
 * <tt>&lt;metadata&gt;</tt> - optional DC metadata with optional child
 * elements (only title, description, language, creator, publisher, format and identifier are read) <br>
 * <tt>&lt;dataPath&gt;</tt> - optional local dataPath location (local path to
 * file to package on packaging server). Only used if dataPath Mode is engaged.
 * Used instead of data element in request. <br>
 * <tt>&lt;permissions&gt;</tt> - optional permissions grammar <br>
 * <tt>&lt;thumbnailLocation&gt;</tt> - optional thumbnail location (file name
 * or FTP URL) <br>
 * <br>
 * Any elements that are not listed here will be ignored by the tool.
 * Additionally, there are several coocurrence restrictions that must be
 * satisfied by the package request:
 * <ul>
 * <li>If <tt>&lt;action=replace&gt;</tt> is present, then
 * <tt>&lt;resource&gt;</tt> must be present
 * </li>
 * <li>If <tt>&lt;resourceItem&gt;</tt> is present, then
 * <tt>&lt;resource&gt;</tt> must be present </li>
 * <li>If <tt>&lt;fileName&gt;</tt> is present, then
 * <tt>&lt;location&gt;</tt> and <tt>&lt;src&gt;</tt> must not be present
 * </li>
 * <li><tt>&lt;src&gt;</tt> and <tt>&lt;location&gt;</tt> must either be
 * both present or both not present. If both are present, then
 * <tt>&lt;fileName&gt;</tt> must not be present</li>
 * </ul>
 * This tool will not accept XML that is not well-formed. Below is some sample
 * content for an XML source file.
 * </p>
 * 
 * <pre>
 *  &lt;package xmlns=&quot;http://ns.adobe.com/adept&quot;&gt;
 * 	&lt;action&gt;add&lt;/action&gt;
 * 	&lt;resource&gt;urn:uuid:ca801ab6-c8c6-45c6-9736-b08959d43e33&lt;/resource&gt;
 * 	&lt;resourceItem&gt;0&lt;/resourceItem&gt;
 * 	&lt;fileName&gt;xmlConfigTest.pdf&lt;/fileName&gt;
 * 	&lt;metadata xmlns:dc=&quot;http://purl.org/dc/elements/1.1/&quot;&gt;
 *  		&lt;dc:title&gt;XML Config Test&lt;/dc:title&gt;
 *  		&lt;dc:description&gt;This is the XML Config Test&lt;/dc:description&gt;
 *  		&lt;dc:language&gt;en&lt;/dc:language&gt;
 *  		&lt;dc:creator&gt;Piotr Kula&lt;/dc:creator&gt;
 *  		&lt;dc:publisher&gt;Adobe Systems Inc&lt;/dc:publisher&gt;
 *  		&lt;dc:format&gt;application/pdf&lt;/dc:format&gt;
 *  		&lt;dc:identifier&gt;any string value&lt;/dc:identifier&gt;
 *  	&lt;/metadata&gt;
 *  	&lt;permissions&gt;
 *  		&lt;display&gt;
 *  			&lt;device/&gt;
 *  			&lt;until&gt;2008-06-11T10:10:49-07:00&lt;/until&gt;
 *  		&lt;/display&gt;
 *  		&lt;play/&gt;
 *  		&lt;excerpt&gt;
 *  			&lt;until&gt;2008-06-11T10:10:49-07:00&lt;/until&gt;			
 *  		&lt;/excerpt&gt;
 *  		&lt;print&gt;
 *  			&lt;count initial=&quot;10&quot; max=&quot;20&quot; incrementInterval=&quot;3600&quot;/&gt;
 *  			&lt;maxResolution&gt;300&lt;/maxResolution&gt;
 *  		&lt;/print&gt;
 *  	&lt;/permissions&gt;
 *  &lt;/package&gt;
 * </pre>
 * 
 * @see XMLUtil
 * @see Base64
 * @version 1.0.1
 * @author Piotr Kula, Peter Sorotokin
 */
public class UploadTest {

	/* ************** P U B L I C F I E L D S ************** */

	/** Adobe Adept namespace */
	public final String AdeptNS = "http://ns.adobe.com/adept";

	/** Dublin Core namespace */
	public final String DublinCoreNS = "http://purl.org/dc/elements/1.1/";

	/** Dublin Core namespace prefix */
	public final String DublinCorePrefix = "dc";

	/** Length of time before request expiration in minutes, set by default to 15 */
	public final int EXPIRATION_INTERVAL = 15;

	/** Current version of this tool */
	public final String VERSION = new String("1.2");

	/* *********** G L O B A L V A R I A B L E S *********** */

	/** Holds the HMAC password shared secret for generation of HMAC */
	private String password;

	/**
	 * Counter used to keep track of the number of error responses received from
	 * server
	 */
	private int errors = 0;

	/**
	 * Counter used to keep track of the number of successful packaging
	 * responses received from server
	 */
	private int successes = 0;

	/** Toggles detailed output to console */
	private boolean verboseDisplay = false;

	/** Toggles use of XML file as a source for DC metadata */
	private boolean useXMLSource = false;

	/** Toggles dataPath mode on the tool */
	private boolean useDataPath = false;

	/* *** XML SOURCE ELEMENTS *** */

	/** Indicates whether the &lt;resource&gt; element is present in XML source */
	private boolean hasAction = false;

	/** Indicates whether the &lt;resource&gt; element is present in XML source */
	private boolean hasResource = false;

	/** Indicates whether the &lt;voucher&gt; element is present in XML source */
	private boolean hasVoucher = false;

	/**
	 * Indicates whether the &lt;resourceItem&gt; element is present in XML
	 * source
	 */
	private boolean hasResourceItem = false;

	/** Indicates whether the &lt;fileName&gt; element is present in XML source */
	private boolean hasFileName = false;

	/** Indicates whether the &lt;location&gt; element is present in XML source */
	private boolean hasLocation = false;

	/** Indicates whether the &lt;src&gt; element is present in the XML source */
	private boolean hasSrc = false;

	/**
	 * Indicates whether the &lt;thumbnailLocation&gt; element is present in the
	 * XML source
	 */
	private boolean hasThumbLoc = false;

	/** Toggles use of XML file as a source for permissions */
	private boolean hasPermissions = false;

	/**
	 * Indicates whether the &lt;dataPath&gt; element is present in the XML
	 * source
	 */
	private boolean hasDataPath = false;

	/**
	 * Indicates whether DC metadata from XML source will be included in package
	 * request
	 */
	private boolean hasMetadata = false;

	/**
	 * Indicates whether default DC metadata from command-line flags will be
	 * included in the package request
	 */
	private boolean hasDefaultMetadata = false;

	/* *** DC METADATA VARS *** */
	/** Holds the DEFAULT value of the DC metadata element &lt;title&gt;, if used */
	private String dcTitleDefault = new String("");

	/** Holds the DEFAULT value of the DC metadata element &lt;description&gt;, if used */
	private String dcDescriptionDefault = new String("");

	/** Holds the DEFAULT value of the DC metadata element &lt;language&gt;, if used */
	private String dcLanguageDefault = new String("");

	/**
	 * Holds the DEFAULT value of the DC metadata element &lt;creator&gt;, if
	 * used
	 */
	private String dcCreatorDefault = new String("");

	/**
	 * Holds the DEFAULT value of the DC metadata element &lt;publisher&gt;, if
	 * used
	 */
	private String dcPublisherDefault = new String("");

	/**
	 * Holds the DEFAULT value of the DC metadata element &lt;format&gt;, if
	 * used
	 */
	private String dcFormatDefault = new String("");

	/** Holds the DEFAULT value of the DC metadata element &lt;identifier&gt;, if used */
	private String dcIdentifierDefault = new String("");

	/** Holds the value of the DC metadata element &lt;title&gt;, if used */
	private String dcTitle = new String("");

	/** Holds the value of the DC metadata element &lt;description&gt;, if used */
	private String dcDescription = new String("");

	/** Holds the value of the DC metadata element &lt;language&gt;, if used */
	private String dcLanguage = new String("");

	/** Holds the value of the DC metadata element &lt;creator&gt;, if used */
	private String dcCreator = new String("");

	/** Holds the value of the DC metadata element &lt;publisher&gt;, if used */
	private String dcPublisher = new String("");

	/** Holds the value of the DC metadata element &lt;format&gt;, if used */
	private String dcFormat = new String("");

	/** Holds the value of the DC metadata element &lt;identifier&gt;, if used */
	private String dcIdentifier = new String("");

	/* *** THUMBNAIL VARS *** */
	/** If true, the tool will look for .png thumbnails */
	private boolean thumbPNG = false;

	/** If true, the tool will look for .jpeg thumbnails */
	private boolean thumbJPEG = false;

	/** If true, the tool will look for .jpg thumbnails */
	private boolean thumbJPG = false;

	/** If true, the tool will look for .gif thumbnails */
	private boolean thumbGIF = false;

	/**
	 * Holds the file name of the file currently being packaged globally (for
	 * use with failedFiles)
	 */
	private String currentFileName = new String("");

	/** Holds the file names of all the files that failed packaging */
	private String failedFiles = new String("");

	/**
	 * Used during the creation of the nonce, holds an incremented counter
	 * started at a random number
	 */
	private static long counter = (new Random()).nextLong();

	/** Used during the creation of the nonce, holds the start time of the server */
	private static byte[] initTime = createInitTime();

	/* ********************************************************* */
	/* ********************* M E T H O D S ********************* */
	/* ********************************************************* */

	/* ************* U T I L I T Y M E T H O D S ************* */

	/**
	 * Fills an array starting at the offset with the bytes of the long
	 * 
	 * @param k
	 *            The source long
	 * @param b
	 *            Byte[] to be filled with the long bytes
	 * @param i
	 *            Offset at which to start filling array
	 */
	public static void longToBytes(long k, byte[] b, int i) {
		b[i] = (byte) (k >> 56);
		b[i + 1] = (byte) (k >> 48);
		b[i + 2] = (byte) (k >> 40);
		b[i + 3] = (byte) (k >> 32);
		b[i + 4] = (byte) (k >> 24);
		b[i + 5] = (byte) (k >> 16);
		b[i + 6] = (byte) (k >> 8);
		b[i + 7] = (byte) k;
	}

	/**
	 * Returns a byte[] with the initial time, used for nonce creation
	 * 
	 * @return a byte array of length 8 containing the bytes of the initial time
	 */
	public static byte[] createInitTime() {
		long time = System.currentTimeMillis() ^ 5792386608507341196L;
		byte[] bytes = new byte[8];
		longToBytes(time, bytes, 0);
		return bytes;
	}

	/**
	 * Reads they bytes from the specified file and returns them in a byte[]
	 * 
	 * @param fileName
	 *            The path of the file to be read
	 * @throws IOException
	 *             If the whole file could not be read
	 * @return Byte array filled with file bytes if successful, otherwise null
	 */
	public byte[] readFromFile(String fileName) {
		try {
			File file = new File(fileName);
			if (!file.exists())
				return null;
			int len = (int) file.length();
			byte[] bytes = new byte[len];
			FileInputStream in = new FileInputStream(file);
			if (in.read(bytes) != len)
				throw new IOException("could not read the whole file");
			in.close();
			return bytes;
		} catch (Exception e) {
			e.printStackTrace();
			errors++;
		}
		return null;
	}

	/**
	 * Strips the file name or path passed to it of the extensions ".pdf" or
	 * ".epub". Used to create path for XML config file and the thumbnail image.
	 * 
	 * @param fileName
	 *            Source file name or path
	 * @return The file name or path without the extension ".pdf" or ".epub", or
	 *         null if fileName did not have one of those extensions.
	 */
	public String removeExtension(String fileName) {
		if (fileName.substring(fileName.length() - 4, fileName.length())
				.equalsIgnoreCase(".pdf"))
			return fileName.substring(0, fileName.length() - 4);
		else if (fileName.substring(fileName.length() - 5, fileName.length())
				.equalsIgnoreCase(".epub"))
			return fileName.substring(0, fileName.length() - 5);
		else if (useDataPath
				&& fileName.substring(fileName.length() - 4, fileName.length())
						.equalsIgnoreCase(".xml"))
			return fileName.substring(0, fileName.length() - 4);
		else {
			if (useDataPath)
				System.err.println("File extension is not .xml. ERROR");
			else
				System.err
						.println("File extension is neither .pdf nor .epub. ERROR");
			return null;
		}
	}

	/**
	 * Creates a new element in the source Document in the Adept namespace and
	 * appends it to the <var>parentElement</var>.
	 * 
	 * @param doc
	 *            Source Document in which new element will be created
	 * @param elementName
	 *            Tag name of the new element to be created
	 * @param elementContent
	 *            Text content of the new element to be created
	 * @param parentElement
	 *            Parent element to which the new element will be appended
	 * @see Document#createElementNS(String, String)
	 */
	public void addNewAdeptElement(Document doc, String elementName,
			String elementContent, Element parentElement) {
		Element newElement = doc.createElementNS(AdeptNS, elementName);
		newElement.setTextContent(elementContent);
		parentElement.appendChild(newElement);
		return;
	}

	/**
	 * Creates a new element in the source Document in the Dublin Core namespace
	 * and appends it to the <var>parentElement</var>.
	 * 
	 * @param doc
	 *            Source Document in which new element will be created
	 * @param elementName
	 *            Tag name of the new element to be created
	 * @param elementContent
	 *            Text content of the new element to be created
	 * @param parentElement
	 *            Parent element to which the new element will be appended
	 * @see Document#createElementNS(String, String)
	 */
	public void addNewDCElement(Document doc, String elementName,
			String elementContent, Element parentElement) {
		Element newElement = doc.createElementNS(DublinCoreNS, DublinCorePrefix
				+ ":" + elementName);
		newElement.setTextContent(elementContent);
		parentElement.appendChild(newElement);
		return;
	}

	/**
	 * Transforms the passed source Document to a string utilizing Transformer
	 * for Documents. This effectively serializes the XML.
	 * 
	 * @param doc
	 *            Source Document to be serialized
	 * @return String containing serialized XML
	 */
	public String transDoc(Document doc) {
		try {
			Transformer trans = TransformerFactory.newInstance()
					.newTransformer();
			trans.setOutputProperty(OutputKeys.INDENT, "yes");
			StreamResult result = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(doc);
			trans.transform(source, result);
			return result.getWriter().toString();
		} catch (Exception e) {
			e.printStackTrace();
			errors++;
		}
		return null;
	}

	/**
	 * Creates and configures a connection to the packaging server specified by
	 * <var>targetURL</var>. A new connection must be created for every package
	 * request.
	 * 
	 * @param targetURL
	 *            URL of packaging server
	 * @return Properly configured HttpURLConnection to packaging server
	 */
	public HttpURLConnection createConnection(String targetURL) {
		try {
			System.out.println("Creating connection to Packaging Server: "
					+ targetURL);
			URL url = new URL(targetURL);
			final HttpURLConnection conn = (HttpURLConnection) url
					.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type",
					"application/vnd.adobe.adept+xml");
			conn.setDoOutput(true);
			return conn;
		} catch (Exception e) {
			e.printStackTrace();
			errors++;
		}
		return null;
	}

	/**
	 * Creates a new FilenameFilter that only accepts files with the extensions
	 * ".epub" or ".pdf" (the only ones that can be packaged)
	 * 
	 * @return FilenameFilter that only accepts files with the extensions
	 *         ".epub" or ".pdf"
	 */
	public FilenameFilter createFilenameFilter() {
		return new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return (name.endsWith(".pdf") || name.endsWith(".epub"));
			}
		};
	}

	/**
	 * Creates a new FilenameFilter that only accepts files with the extension
	 * ".xml" (the XML Source files for use with dataPath mode)
	 * 
	 * @return FilenameFilter that only accepts files with the extension ".xml"
	 */
	public FilenameFilter createDataPathFilter() {
		return new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return (name.endsWith(".xml"));
			}
		};
	}

	/**
	 * Resets all current global DC metadata vars (in preparation for reading
	 * from a new XML file)
	 */
	private void cleanCurrentDCMetadata() {
		dcTitle = "";
		dcDescription = "";
		dcLanguage = "";
		dcCreator = "";
		dcPublisher = "";
		dcFormat = "";
		dcIdentifier = "";
		hasMetadata = false;
	}

	/**
	 * Displays a list of all accepted flags when the tool is called with the -?
	 * flag.
	 */
	private void displayHelp() {
		System.out.println("UploadTest version " + VERSION);
		System.out
				.println("The first parameter MUST be the packaging server URL");
		System.out
				.println("The second parameter MUST be the file or directory path to package");
		System.out.println("\nList of accepted command-line flags:");
		System.out.println("PASSWORD FLAG");
		System.out
				.println("-pass = the next argument contains the server password");
		System.out.println("DC METADATA FLAGS:");
		System.out
				.println("-title = the next argument contains the DEFAULT dc:title value");
		System.out
				.println("-description = the next argument contains the DEFAULT dc:description value");
		System.out
				.println("-language = the next argument contains the DEFAULT dc:language value");
		System.out
				.println("-creator = the next argument contains the DEFAULT dc:creator value");
		System.out
				.println("-publisher = the next argument contains the DEFAULT dc:publisher value");
		System.out
				.println("-format = the next argument contains the DEFAULT dc:format value");
		System.out
				.println("-identifier = the next argument contains the DEFAULT dc:identifier value");
		System.out.println("\nMISC FLAGS:");
		System.out
				.println("-png = looks for fileNameNoExt.png to upload as a thumbnail");
		System.out
				.println("-jpeg = looks for fileNameNoExt.jpeg to upload as a thumbnail");
		System.out
				.println("-jpg = looks for fileNameNoExt.jpg to upload as a thumbnail");
		System.out
				.println("-gif = looks for fileNameNoExt.gif to upload as a thumbnail");
		System.out
				.println("-xml = looks for fileNameNoExt.xml to use as XML source");
		System.out.println("-datapath = engages dataPath Mode for the Tool");
		System.out
				.println("-verbose = displays content of package request and detailed server response");
		System.out.println("-version = displays tool's version number");
		System.out
				.println("-? = displays the list of accepted command-line flags");
	}

	/* **** C O N T E N T C R E A T I O N M E T H O D S **** */

	/**
	 * Creates a quasi-unique nonce based on the start time and an incremented
	 * counter
	 * 
	 * @return a byte array of length 16 containing the nonce
	 */
	private synchronized static byte[] makeNonce() {
		byte[] nonce = new byte[16];
		counter++;
		System.arraycopy(initTime, 0, nonce, 0, 8);
		longToBytes(counter, nonce, 8);
		return nonce;
	}

	/**
	 * For every argument in the passed array, checks to see if it is a flag by
	 * passing it to hasFlags() and checks to see if it is metadata by passing
	 * it to hasMetadata(). If any of the elements of args is a metadata flag,
	 * sets <var>hasMetadata</var> = true to ensure that the metadata will be
	 * included.
	 * 
	 * <br>
	 * The following flags are accepted:
	 * 
	 * <pre>
	 *  PASSWORD FLAG:
	 *  -pass = the next argument contains the server password
	 *  
	 *  DC METADATA FLAGS: 
	 *  -title = the next argument contains the DEFAULT dc:title value
	 *  -description = the next argument contains the DEFAULT dc:description value
	 *  -language = the next argument contains the DEFAULT dc:language value
	 *  -creator = the next argument contains the DEFAULT dc:creator value 
	 *  -publisher = the next argument contains the DEFAULT dc:publisher value 
	 *  -format = the next argument contains the DEFAULT dc:format value
	 *  -identifier = the next argument contains the DEFAULT dc:identifier value
	 *  
	 *  MISC FLAGS: 
	 *  -png = looks for fileNameNoExt.png to upload as a thumbnail
	 *  -jpeg = looks for fileNameNoExt.jpeg to upload as a thumbnail 
	 *  -jpg = looks for fileNameNoExt.jpg to upload as a thumbnail 
	 *  -gif = looks for fileNameNoExt.gif to upload as a thumbnail 
	 *  -xml = looks for fileNameNoExt.xml to use as XML source 
	 *  -verbose = displays content of package request and detailed server response
	 *  -version = displays tool's version number
	 *  -? = displays the list of accepted command-line flags
	 * </pre>
	 * 
	 * @param args
	 *            Array of arguments passed to main
	 * @see UploadTest#hasFlags(String)
	 * @see UploadTest#hasMetadata(String, String[], int)
	 * @see UploadTest#hasMetadata
	 */
	private void scanArgsForFlags(String[] args) {
		for (int i = 0; i < args.length; i++) {
			hasFlags(args[i].toLowerCase());
			if (hasMetadata(args[i].toLowerCase(), args, i + 1)) {
				i++; // args[i+1] is the value of the metadata element, so
				// skip
				hasDefaultMetadata = true; // if any DC metadata flag is
											// identified,
				// use metadata
			}
			if (hasPassword(args[i].toLowerCase(), args, i + 1)) {
				i++; // skip
			}
		}
	}

	/**
	 * Checks to see if the argument passed to it is an identifiable flag, and
	 * performs the appropriate action if it identifies a flag. <br>
	 * It looks for the following flags:
	 * 
	 * <pre>
	 *  -png = looks for fileNameNoExt.png to upload as a thumbnail 
	 *  -jpeg = looks for fileNameNoExt.jpeg to upload as a thumbnail
	 *  -jpg = looks for fileNameNoExt.jpg to upload as a thumbnail 
	 *  -gif = looks for fileNameNoExt.gif to upload as a thumbnail 
	 *  -xml = looks for fileNameNoExt.xml to use as XML source 
	 *  -verbose = displays content of package request and detailed server response
	 * </pre>
	 * 
	 * @param singleArg
	 *            A single argument to compare against identifiable flags
	 * @see UploadTest#verboseDisplay
	 * @see UploadTest#thumbExt
	 * @see UploadTest#useXMLSource
	 */
	private void hasFlags(String singleArg) {
		if (singleArg.equals("-verbose"))
			verboseDisplay = true; // turn on verboseDisplay
		else if (singleArg.equals("-datapath")) {
			useDataPath = true; // turn on dataPath mode
			useXMLSource = true; // dataPath mode requires the use of XML
									// source
		} else if (singleArg.equals("-png"))
			thumbPNG = true; // search for .png thumbnails
		else if (singleArg.equals("-jpeg"))
			thumbJPEG = true; // serach for .jpeg thumbnails
		else if (singleArg.equals("-jpg"))
			thumbJPG = true; // serach for .jpg thumbnails
		else if (singleArg.equals("-gif"))
			thumbGIF = true; // search for .gif thumbnails
		else if (singleArg.equals("-xml"))
			useXMLSource = true; // turn on useXMLSource
		else if (singleArg.equals("-?"))
			displayHelp(); // display list of command-line flags
		else if (singleArg.equals("-version"))
			System.out.println("UploadTest version " + VERSION);
	}

	/**
	 * Checks to see if the argument passed to it is an identifiable DC metadata
	 * flag. If it is, it assigns the corresponding DC metadata variable the
	 * value of the next argument (next following element of <var>args</var>)
	 * and returns true.
	 * 
	 * @param singleArg
	 *            A single argument to compare against identifiable metadata
	 *            flags
	 * @param args
	 *            Array of arguments passed to main
	 * @param paramIndex
	 *            The index of singleArg + 1 (the index of the following
	 *            element)
	 * @see UploadTest#dcTitle
	 * @see UploadTest#dcDescription
	 * @see UploadTest#dcLanguage
	 * @see UploadTest#dcCreator
	 * @see UploadTest#dcPublisher
	 * @see UploadTest#dcFormat
	 * @see UploadTest#dcIdentifier
	 * @return true if singleArg was a DC metadata flag, otherwise false
	 */
	private boolean hasMetadata(String singleArg, String[] args, int paramIndex) {
		if (singleArg.equals("-title")) {
			dcTitleDefault = args[paramIndex];
			return true;
		} else if (singleArg.equals("-description")) {
			dcDescriptionDefault = args[paramIndex];
			return true;
		} else if (singleArg.equals("-language")) {
			dcLanguageDefault = args[paramIndex];
			return true;
		} else if (singleArg.equals("-creator")) {
			dcCreatorDefault = args[paramIndex];
			return true;
		} else if (singleArg.equals("-publisher")) {
			dcPublisherDefault = args[paramIndex];
			return true;
		} else if (singleArg.equals("-format")) {
			dcFormatDefault = args[paramIndex];
			return true;
		} else if (singleArg.equals("-identifier")) {
			dcIdentifierDefault = args[paramIndex];
			return true;
		}
		return false;
	}

	/**
	 * Checks to see if the argument passed to it is a password flag. If it is,
	 * it assigns the password variable the value of the next argument (next
	 * following element of <var>args</var>) and returns true.
	 * 
	 * @param singleArg
	 *            A single argument to check
	 * @param args
	 *            Array of arguments passed to main
	 * @param paramIndex
	 *            The index of singleArg + 1 (the index of the following
	 *            element)
	 * @return true if singleArg was a password flag, otherwise false
	 */
	private boolean hasPassword(String singleArg, String[] args, int paramIndex) {
		if (singleArg.equals("-pass") || singleArg.equals("-p")) {
			password = args[paramIndex];
			return true;
		}
		return false;
	}

	/**
	 * Uses an XML file as a source for DC metadata and permissions to be
	 * included in the package request. <br>
	 * If <var>useXMLSource</var> = false, this method returns null before
	 * doing anything else. Otherwise, open the specified file, and read its
	 * contents. Store the contents in the String <var>fileContent</var>. <br>
	 * If the XML source file does not exist, this method returns null. <br>
	 * Next, parse the XML from the file and create a DOM document from it.
	 * Extract the DC metadata (if there is any) from the document by passing
	 * the document to metadataFromXML() <br>
	 * Then, the method checks for the existence of optional package elements.
	 * If any of them are, set the appropriate boolean variable to true (to
	 * ensure that the element gets extracted and appended to the package
	 * request). Finally, the method calls runCooccurenceChecks, which makes
	 * sure that all cooccurrence restrictions are satisfied with the request.
	 * 
	 * @param fileNameNoExt
	 *            File or path name without any extensions
	 * @see UploadTest#useXMLSource
	 * @see XMLUtil#parseXML(String)
	 * @see UploadTest#metadataFromXML(Document)
	 * @see XMLUtil#extractElement(Document, String, String)
	 * @see UploadTest#runCoocurrenceChecks()
	 * @return The DOM Document created from the XML Source or null if
	 *         useXMLSource = false or null if the XML source file cannot be
	 *         found
	 */
	private Document useXMLSource(String fileNameNoExt) {
		// Document xmlSourceDoc = XMLUtil.createDocument();
		if (!useXMLSource)
			return null; // if useXMLSource = false, don't use XML source
		// Create the xmlSource file
		File xmlSource = new File(fileNameNoExt.concat(".xml"));

		// If the xmlSource file exists, read it and extract XML
		if (xmlSource.exists()) {
			if (verboseDisplay)
				System.out.println("Found XML source file: "
						+ xmlSource.getAbsolutePath());

			// Parse the fileContent into XML and extract specific contents
			try {
				// Parse fileContent into XML
				Document parsedXML = XMLUtil.parseXML(new FileInputStream(
						xmlSource));

				// Extract DC metadata from XML (if present)
				metadataFromXML(parsedXML);

				// Check for occurence of optional package elements in XML
				// source
				if (XMLUtil.extractElement(parsedXML, AdeptNS, "action") != null)
					hasAction = true;
				else
					hasAction = false;
				if (XMLUtil.extractElement(parsedXML, AdeptNS, "resource") != null)
					hasResource = true;
				else
					hasResource = false;
				if (XMLUtil.extractElement(parsedXML, AdeptNS, "voucher") != null)
					hasVoucher = true;
				else
					hasVoucher = false;
				if (XMLUtil
						.extractElement(parsedXML, AdeptNS, "resourceItem") != null)
					hasResourceItem = true;
				else
					hasResourceItem = false;
				if (XMLUtil.extractElement(parsedXML, AdeptNS, "fileName") != null)
					hasFileName = true;
				else
					hasFileName = false;
				if (XMLUtil.extractElement(parsedXML, AdeptNS, "location") != null)
					hasLocation = true;
				else
					hasLocation = false;
				if (XMLUtil.extractElement(parsedXML, AdeptNS, "src") != null)
					hasSrc = true;
				else
					hasSrc = false;
				if (XMLUtil.extractElement(parsedXML, AdeptNS, "permissions") != null)
					hasPermissions = true;
				else
					hasPermissions = false;
				if (XMLUtil.extractElement(parsedXML, AdeptNS,
						"thumbnailLocation") != null)
					hasThumbLoc = true;
				else
					hasThumbLoc = false;
				if (XMLUtil.extractElement(parsedXML, AdeptNS, "dataPath") != null)
					hasDataPath = true;
				else
					hasDataPath = false;

				// run checks to make sure coocurrence restrictions are
				// satisfied
				runCoocurrenceChecks();

				if (useDataPath && !hasDataPath) {
					System.err
							.println("When dataPath mode is engaged, all XML Source documents must include dataPath element!");
					return null;
				}

				return parsedXML;
			} catch (Exception e) {
				e.printStackTrace();
				errors++;
			}
		} else {
			System.err.println("Could not find XML source: "
					+ fileNameNoExt.concat(".xml"));
		}
		return null;
	}

	/**
	 * Extracts DC metadata (if present) from the source Document that it is
	 * passed. If the source Document contains DC metadata, this method assigns
	 * the corresponding DC metadata variable the appropriate extracted text and
	 * sets <var>hasMetadata</var> = true to ensure that the metadata element
	 * is included in the package request
	 * 
	 * @param parsedXML
	 *            Source Document from which DC metadata is extracted
	 * @see XMLUtil#extractDCElementText(Document, String)
	 * @see UploadTest#hasMetadata
	 * @see UploadTest#dcTitle
	 * @see UploadTest#dcDescription
	 * @see UploadTest#dcLanguage
	 * @see UploadTest#dcCreator
	 * @see UploadTest#dcPublisher
	 * @see UploadTest#dcFormat
	 * @see UploadTest#dcIdentifier
	 */
	private void metadataFromXML(Document parsedXML) {
		if (XMLUtil.extractDCElementText(parsedXML, "title") != null) {
			hasMetadata = true;
			dcTitle = XMLUtil.extractDCElementText(parsedXML, "title");
		}
		if (XMLUtil.extractDCElementText(parsedXML, "description") != null) {
			hasMetadata = true;
			dcDescription = XMLUtil.extractDCElementText(parsedXML, "description");
		}
		if (XMLUtil.extractDCElementText(parsedXML, "language") != null) {
			hasMetadata = true;
			dcLanguage = XMLUtil.extractDCElementText(parsedXML, "language");
		}
		if (XMLUtil.extractDCElementText(parsedXML, "creator") != null) {
			hasMetadata = true;
			dcCreator = XMLUtil.extractDCElementText(parsedXML, "creator");
		}
		if (XMLUtil.extractDCElementText(parsedXML, "publisher") != null) {
			hasMetadata = true;
			dcPublisher = XMLUtil.extractDCElementText(parsedXML, "publisher");
		}
		if (XMLUtil.extractDCElementText(parsedXML, "format") != null) {
			hasMetadata = true;
			dcFormat = XMLUtil.extractDCElementText(parsedXML, "format");
		}
		if (XMLUtil.extractDCElementText(parsedXML, "identifier") != null) {
			hasMetadata = true;
			dcIdentifier = XMLUtil.extractDCElementText(parsedXML, "identifier");
		}
	}

	/**
	 * This method checks to see if the cooccurence restrictions of the XML
	 * Source are satisfied. If the restrictions are not satisfied, this method
	 * will display an error message to console and set the appropriate boolean
	 * variables to ignore the invalid elements. The restrictions are described
	 * below: <br>
	 * <ul>
	 * <li>If <tt>&lt;resourceItem&gt;</tt> is present, then
	 * <tt>&lt;resource&gt;</tt> must be present </li>
	 * <li>If <tt>&lt;fileName&gt;</tt> is present, then
	 * <tt>&lt;location&gt;</tt> and <tt>&lt;src&gt;</tt> must not be
	 * present </li>
	 * <li><tt>&lt;src&gt;</tt> and <tt>&lt;location&gt;</tt> must either
	 * be both present or both not present. If both are present, then
	 * <tt>&lt;fileName&gt;</tt> must not be present</li>
	 * </ul>
	 */
	private void runCoocurrenceChecks() {
		// if <resourceItem> exists, then <resource> must exist
		if (hasResourceItem && !hasResource) {
			System.err.println("XML Source Coocurrence violation!");
			System.err
					.println("If <resourceItem> is used, <resource> must be used as well.");
			System.err.println("<resourceItem> will be ignored!");
			hasResourceItem = false;
		}

		// if <fileName> exists, then <location> and <src> must not exist
		if (hasFileName && (hasLocation || hasSrc)) {
			System.err.println("XML Source Coocurrence violation!");
			System.err
					.println("If <fileName> is used, <location> and <src> must not be used.");
			System.err.println("<location> and <src> will be ignored!");
			hasLocation = false;
			hasSrc = false;
		}

		// if <src> exists, then <location> must exist
		if (hasSrc && !hasLocation) {
			System.err.println("XML Source Coocurrence violation!");
			System.err
					.println("If <src> is used, <location> must be used as well.");
			System.err.println("<src> will be ignored!");
			hasSrc = false;
		}

		// if <location> exists, then <src> must exist
		if (hasLocation && !hasSrc) {
			System.err.println("XML Source Coocurrence violation!");
			System.err
					.println("If <location> is used, <src> must be used as well.");
			System.err.println("<location> will be ignored!");
			hasLocation = false;
		}
	}

	/**
	 * Retrieves the file name of the thumbnail image, based on which thumbnail
	 * flags were used. This method checks to see if there are thumbnail files
	 * with the extensions specified by the thumbnail flags called from
	 * command-line. It checks for the existence of thumbnail files in the
	 * order: png, jpeg, jpg, gif, and returns the first one that it finds. This
	 * method will only check the extensions that correspond to command-line
	 * thumbnail flags used when the tool was started. (EX: in order to use
	 * firstBook.jpg and secondBook.png, the tool must be called with BOTH -jpg
	 * and -png)
	 * 
	 * @param fileNameNoExt
	 *            String containing the base book name with no extension
	 * @return String containing the thumbnail file name (with extension)
	 */
	private String getThumbnail(String fileNameNoExt) {
		if (thumbPNG) {
			if ((new File(fileNameNoExt.concat(".png"))).isFile())
				return fileNameNoExt.concat(".png");
		}
		if (thumbJPEG) {
			if ((new File(fileNameNoExt.concat(".jpeg"))).isFile())
				return fileNameNoExt.concat(".jpeg");
		}
		if (thumbJPG) {
			if ((new File(fileNameNoExt.concat(".jpg"))).isFile())
				return fileNameNoExt.concat(".jpg");
		}
		if (thumbGIF) {
			if ((new File(fileNameNoExt.concat(".gif"))).isFile())
				return fileNameNoExt.concat(".gif");
		}
		return null;
	}

	/**
	 * Retrieves the HMAC secret key and returns it as a String. For the time
	 * being, the key is known to be "One4_all"
	 * 
	 * @return String containing HMAC secret key bytes
	 */
	private String getHmacKey() {
		return password;
	}

	/**
	 * Creates a new DOM Document and appends elements to create the package
	 * request. All elements are in Adept namespace except for the optional
	 * Dublin Core metadata elements, which are in the Dublin Core namespace.
	 * <br>
	 * Specifically: Creates a new DOM Document and package element in AdeptNS.
	 * If there are optional elements that were read from the XML Source file,
	 * these get extracted and appended to the packageElementin the correct
	 * order. If metadata is present, creates <var>metadataElement</var> and
	 * appends the appropriate DC metadata elements to it. Next, it appends the
	 * <var>metadataElement</var> to the <var>packagElement</var>. If there
	 * are permissions present, imports the <var>permissionsElement</var> from
	 * the XML document and appends it to the <var>packageElement</var>. Next,
	 * it appends the required Adept element data (containing the base64 encoded
	 * book bytes). If there is a thumbail file present, it appends the base64
	 * encoded thumbnail bytes. Next, it appends the rest of the required Adept
	 * elements: expiration (containing expiration time in W3CDTF, which is made
	 * to be the current time + <var>EXPIRATION_INTERVAL</var> minutes), and
	 * the nonce (this unique identifier is created with makeNonce() and is
	 * dependent on <var>initTime</var> and and incremented counter). Finally,
	 * the HMAC is calculated and appended to the <var>packageElement</var>.
	 * The <var>packageElement</var> is appended to the Document and the
	 * Document is serialized with transDoc() and returned. <br>
	 * Shown below is the structure of the package request produced. Optional
	 * elements are marked with <tt>**OPT</tt>
	 * 
	 * <pre>
	 *  &lt;package xmlns=&quot;http://ns.adobe.com/adept/&quot;&gt;
	 *  	&lt;action&gt;**OPT add|replace&lt;/action&gt;
	 *  	&lt;resource&gt;**OPT resource ID&lt;/resource&gt;
	 *  	&lt;voucher&gt;**OPT voucher ID for GBLink fulfillment&lt;/voucher&gt;
	 *  	&lt;resourceItem&gt;**OPT resource item index&lt;/resourceItem&gt;
	 *  	&lt;fileName&gt;**OPT file name to use for this packaged resource&lt;/fileName&gt;
	 *  	&lt;location&gt;**OPT upload location for packaged resource (file name or FTP URL) &lt;/location&gt;
	 *  	&lt;src&gt;**OPT download location for the packaged resource (HTTP URL)&lt;/src&gt;
	 *  	&lt;metadata xmlns:dc=&quot;http://purl.org/dc/elements/1.1/&quot;&gt;**OPT 
	 *  		&lt;dc:title&gt;**OPT Book Title&lt;/dc:title&gt; 
	 *  		&lt;dc:description&gt;**OPT Book Description&lt;/dc:description&gt; 
	 *  		&lt;dc:language&gt;**OPT Book Language&lt;/dc:language&gt; 
	 *  		&lt;dc:creator&gt;**OPT Book Creator&lt;/dc:creator&gt;
	 *  		&lt;dc:publisher&gt;**OPT Book Publisher&lt;/dc:creator&gt; 
	 *  		&lt;dc:format&gt;**OPT Book mimetype&lt;/dc:format&gt; 
	 *  		&lt;dc:idntifier&gt;**OPT Book iodentifier&lt;/dc:identifiert&gt; 
	 *  	&lt;/metadata&gt; 
	 *  	&lt;permissions&gt;**OPT 
	 *  		&lt;display&gt;**OPT rights elements&lt;/display&gt; 
	 *  		&lt;play&gt;**OPT rights elements&lt;/play&gt; 
	 *  		&lt;excerpt&gt;**OPT rights elements&lt;/excerpt&gt;
	 *  		&lt;print&gt;**OPT rights elements&lt;/print&gt; 
	 *  	&lt;/permissions&gt; 
	 *  	&lt;dataPath&gt;**OPT instead of data element, specifies local eBook location on packaging server&lt;/dataPath&gt;
	 *  	&lt;data&gt; Base64-encoded book bytes &lt;/data&gt; 
	 *  	&lt;thumbnailLocation&gt;**OPT thumbnail upload location (file name or FTP URL)&lt;/thumbnailLocation&gt;
	 *  	&lt;thumbnailData&gt;**OPT Base64-encoded thumbnail bytes &lt;/thumbnailData&gt; 
	 *  	&lt;expiration&gt; W3CDTF expiration &lt;/expiration&gt; 
	 *  	&lt;nonce&gt; Base64-encoded nonce &lt;/nonce&gt; 
	 *  &lt;hmac&gt; Base64-encoded HMAC &lt;/hmac&gt; 
	 *  &lt;/package&gt;
	 * </pre>
	 * 
	 * @param fileName
	 *            Path of source file used to find XML file
	 * @see UploadTest#removeExtension(String)
	 * @see XMLUtil#createDocument()
	 * @see Document#createElementNS(String, String)
	 * @see UploadTest#useXMLSource(String)
	 * @see UploadTest#hasMetadata
	 * @see UploadTest#addNewDCElement(Document, String, String, Element)
	 * @see UploadTest#hasPermissions
	 * @see UploadTest#addNewAdeptElement(Document, String, String, Element)
	 * @see Base64#encodeBytes(byte[])
	 * @see UploadTest#readFromFile(String)
	 * @see UploadTest#thumbExt
	 * @see XMLUtil#dateToW3CDTF(Date)
	 * @see UploadTest#EXPIRATION_INTERVAL
	 * @see UploadTest#getHmacKey()
	 * @see XMLUtil#hmac(byte[], Element)
	 * @see UploadTest#transDoc(Document)
	 * @return String containing the serialized XML package request
	 */
	private String makeContent(String fileName) {
		try {
			System.out.println("\nCreating package request for: " + fileName);

			// Setting up DOM Document structures
			String fileNameNoExt = removeExtension(fileName);
			Document doc = XMLUtil.createDocument();
			Element packageElement = doc.createElementNS(AdeptNS, "package");
			Document xmlSourceDoc = useXMLSource(fileNameNoExt);
			if (useXMLSource && xmlSourceDoc == null) {
				System.err.println("An error occured with the XML Source!");
				errors++;
				failedFiles = failedFiles.concat(currentFileName + "\n");
				return null;
			}

			// Extract and add appropriate optional elements from XML Source
			if (hasAction) {
				packageElement.setAttribute("action", XMLUtil.extractElement(
						xmlSourceDoc, AdeptNS, "action").getTextContent());
			}
			if (hasResource) {
				packageElement.appendChild((Element) doc.importNode(XMLUtil
						.extractElement(xmlSourceDoc, AdeptNS, "resource"),
						true));
			}
			if (hasVoucher) {
				packageElement.appendChild((Element) doc.importNode(XMLUtil
						.extractElement(xmlSourceDoc, AdeptNS, "voucher"),
						true));
			}
			if (hasResourceItem) {
				packageElement.appendChild((Element) doc.importNode(
						XMLUtil.extractElement(xmlSourceDoc, AdeptNS,
								"resourceItem"), true));
			}
			if (hasFileName) {
				packageElement.appendChild((Element) doc.importNode(XMLUtil
						.extractElement(xmlSourceDoc, AdeptNS, "fileName"),
						true));
			}
			if (hasLocation) {
				packageElement.appendChild((Element) doc.importNode(XMLUtil
						.extractElement(xmlSourceDoc, AdeptNS, "location"),
						true));
			}
			if (hasSrc) {
				packageElement.appendChild((Element) doc.importNode(XMLUtil
						.extractElement(xmlSourceDoc, AdeptNS, "src"), true));
			}

			// optional dc metadata is included if the corresponding
			// dc metadata String has been assigned a value
			if (hasMetadata || hasDefaultMetadata) {
				Element metadataElement = doc.createElementNS(AdeptNS,
						"metadata");
				metadataElement.setAttributeNS("http://www.w3.org/2000/xmlns/",
						"xmlns:" + DublinCorePrefix, DublinCoreNS);
				if (!dcTitle.equals("")) {
					addNewDCElement(doc, "title", dcTitle, metadataElement);
				} else if (dcTitle.equals("") && !dcTitleDefault.equals("")) {
					addNewDCElement(doc, "title", dcTitleDefault,
							metadataElement);
				}
				if (!dcDescription.equals("")) {
					addNewDCElement(doc, "description", dcDescription, metadataElement);
				} else if (dcDescription.equals("") && !dcDescriptionDefault.equals("")) {
					addNewDCElement(doc, "description", dcDescriptionDefault,
							metadataElement);
				}
				if (!dcLanguage.equals("")) {
					addNewDCElement(doc, "language", dcLanguage, metadataElement);
				} else if (dcLanguage.equals("") && !dcLanguageDefault.equals("")) {
					addNewDCElement(doc, "language", dcLanguageDefault,
							metadataElement);
				}
				if (!dcCreator.equals("")) {
					addNewDCElement(doc, "creator", dcCreator, metadataElement);
				} else if (dcCreator.equals("") && !dcCreatorDefault.equals("")) {
					addNewDCElement(doc, "creator", dcCreatorDefault,
							metadataElement);
				}
				if (!dcPublisher.equals("")) {
					addNewDCElement(doc, "publisher", dcPublisher,
							metadataElement);
				} else if (dcPublisher.equals("")
						&& !dcPublisherDefault.equals("")) {
					addNewDCElement(doc, "publisher", dcPublisherDefault,
							metadataElement);
				}
				if (!dcFormat.equals("")) {
					addNewDCElement(doc, "format", dcFormat, metadataElement);
				} else if (dcFormat.equals("") && !dcFormatDefault.equals("")) {
					addNewDCElement(doc, "format", dcFormatDefault,
							metadataElement);
				}
				if (!dcIdentifier.equals("")) {
					addNewDCElement(doc, "identifier", dcIdentifier, metadataElement);
				} else if (dcIdentifier.equals("") && !dcIdentifierDefault.equals("")) {
					addNewDCElement(doc, "identifier", dcIdentifierDefault,
							metadataElement);
				}

				packageElement.appendChild(metadataElement);
			}

			// add permissions, if present
			if (hasPermissions) {
				packageElement.appendChild((Element) doc.importNode(
						XMLUtil.extractElement(xmlSourceDoc, AdeptNS,
								"permissions"), true));
			}

			// if dataPath Mode is engaged and the XML config has dataPath
			// element,
			// use dataPath instead of data element.
			if (hasDataPath && useDataPath) {
				packageElement.appendChild((Element) doc.importNode(XMLUtil
						.extractElement(xmlSourceDoc, AdeptNS, "dataPath"),
						true));
			} else {
				// else use data element with base64 encoded data bytes from the
				// file
				addNewAdeptElement(doc, "data", Base64
						.encodeBytes(readFromFile(fileName)), packageElement);
			}

			// optional <thumbnailLocation> element
			if (hasThumbLoc) {
				packageElement.appendChild((Element) doc.importNode(XMLUtil
						.extractElement(xmlSourceDoc, AdeptNS,
								"thumbnailLocation"), true));
			}

			// base64 encoded thumbnail image is included if at least one
			// thumbnail flag
			// has been used when this tool was called and the corresponding
			// file can be found
			if (thumbPNG || thumbJPEG || thumbJPG || thumbGIF) {
				String thumbnailName = getThumbnail(fileNameNoExt);
				if (thumbnailName != null) {
					if (verboseDisplay)
						System.out.println("Found thumbnail file: "
								+ thumbnailName);
					addNewAdeptElement(doc, "thumbnailData", Base64
							.encodeBytes(readFromFile(thumbnailName)),
							packageElement);
				} else
					System.err.println("Cannot find thumbnail file: "
							+ thumbnailName);
			}

			// expiration set to be EXPIRATION_INTERVAL min from current time
			addNewAdeptElement(doc, "expiration", XMLUtil
					.dateToW3CDTF(new Date(System.currentTimeMillis()
							+ EXPIRATION_INTERVAL * 60 * 1000)), packageElement);

			// base64 encoded nonce based on initTime and incremental counter
			addNewAdeptElement(doc, "nonce", Base64.encodeBytes(makeNonce()),
					packageElement);

			doc.appendChild(packageElement);

			if (getHmacKey() != null) {
				// retrieve HMAC key and run a raw SHA1 HASH on it.
				byte[] hmacKeyBytesSHA1 = XMLUtil.SHA1(getHmacKey());
				// use the resulting bytes to generate HMAC
				XMLUtil.hmac(hmacKeyBytesSHA1, packageElement);
			}

			String requestContent = transDoc(doc);
			if (verboseDisplay) {
				System.out.println("Package Request:\n");
				System.out.println(requestContent);
			}
			return requestContent;

		} catch (Exception e) {
			e.printStackTrace();
			errors++;
		}

		return null;
	}

	/* *** R E S P O N S E C R E A T I O N M E T H O D S *** */

	/**
	 * Using the HttpURLConnection with the packaging server, this method sends
	 * the serialized XML content to the server and recieves back the server's
	 * response. It then calls displayContent() to display the server's
	 * response.
	 * 
	 * @param outputString
	 *            Serialized XML package request to be sent to server
	 * @param conn
	 *            Properly configured HttpURLConnection with the packaging
	 *            server
	 * @see UploadTest#displayContent(int, String, String)
	 * @throws Exception
	 *             When HttpURLConnection, OutputStream, or InputStreamReader
	 *             would throw exeption.
	 */
	private void sendContent(String outputString, HttpURLConnection conn)
			throws Exception {
		System.out.println("Sending Package Request");

		// Send serialized XML
		OutputStream out = conn.getOutputStream();
		out.write(outputString.getBytes("UTF-8"));
		out.close();

		// Make sure connection is still live
		conn.connect();

		// Receive server's response and put into StringBuffer
		final int code = conn.getResponseCode();
		final String contentType = conn.getContentType();
		final StringBuffer responseText = new StringBuffer();
		InputStreamReader in = new InputStreamReader(conn.getInputStream(),
				"UTF-8");

		char[] msg = new char[2048];
		int len;
		while ((len = in.read(msg)) > 0) {
			responseText.append(msg, 0, len);
		}

		// Pass server's response to displayContent()
		displayContent(code, contentType, responseText.toString());
	}

	/**
	 * Displays server's response in console window in a readable fashion. If
	 * the server returns an error, displays the error and increments errors. If
	 * the server returns a valid response, increments successes and if
	 * verboseDisplay = true, displays the complete Response. If
	 * <var>verboseDisplay</var> = false, displays only that the Request was
	 * successful. This method will flag an error if the server response is not
	 * 200 and the content type is not "application/vnd.adobe.adept+xml"
	 * 
	 * @param code
	 *            Server's HTML Response Code
	 * @param contentType
	 *            Server's Response Content Type
	 * @param responseString
	 *            Server's Response
	 * @see UploadTest#verboseDisplay
	 * @see UploadTest#errors
	 * @see UploadTest#successes
	 */
	private void displayContent(int code, String contentType,
			String responseString) {
		/*
		 * The response is an error (or is invalid) if: -> it begins with "<error"
		 * (as this is the way the packaging server returns errors) -> the
		 * response code is not 200 (the request succeeded) -> the response
		 * content type is not application/vdn.adobe.adept+xml (all responses
		 * from Adobe packaging servers will have this content type)
		 */
		if (responseString.substring(1, 6).equals("error") || code != 200
				|| !contentType.equals("application/vnd.adobe.adept+xml")) {
			if (verboseDisplay) {
				System.err.println("HTML Response Code: " + code);
				System.err.println("Response Content Type: " + contentType);
			}
			System.err.println("There was an error with the Package Request");
			System.err.println(responseString);
			failedFiles = failedFiles.concat(currentFileName + "\n");
			errors++;
		} else if (verboseDisplay) {
			System.out.println("HTML Response Code: " + code);
			System.out.println("Response Content Type: " + contentType);
			System.out.println("Response:\n" + responseString);
			successes++;
			if (dcTitle.equals(""))
				System.out.println("The book has been successfully packaged!");
			else
				System.out.println("The book \"" + dcTitle
						+ "\" has been successfully packaged");
		} else {
			successes++;
			if (dcTitle.equals(""))
				System.out.println("The book has been successfully packaged!");
			else
				System.out.println("The book \"" + dcTitle
						+ "\" has been successfully packaged!");
		}
	}

	/* ******** C O N S T R U C T O R A N D M A I N ******** */

	/**
	 * UploadTest Constructor. <br>
	 * Passes arguments from main to scanArgsForFlags() to scan for flags.
	 * Assigns <var>targetURL</var> the value of the first argument and assigns
	 * <var>fileOrDirName</var> the value of the second argument. <br>
	 * Creates a file from <var>fileOrDirName</var> and determines whether it
	 * is a File or a Directory: <br>
	 * If it is a file, calls makeContent() to make the package request and then
	 * sendContent() to send it to the server and display the server's response.
	 * <br>
	 * If it is a directory, for every file in that directory that has the
	 * extension ".pdf" or ".epub", it calls makeContent() to make the package
	 * request and then sendContent() to send it to the server and display the
	 * server's response. A new connection must be made for every file. <br>
	 * Finally, displays a summary of the successes and errors to the console.
	 * 
	 * @param args
	 *            The array of arguments passed to main
	 * @see UploadTest#scanArgsForFlags(String[])
	 * @see UploadTest#createFilenameFilter()
	 * @see UploadTest#makeContent(String)
	 * @see UploadTest#createConnection(String)
	 * @see UploadTest#sendContent(String, HttpURLConnection)
	 * @see UploadTest#errors
	 * @see UploadTest#successes
	 */
	public UploadTest(String[] args) {
		String fileOrDirName = new String("");
		String targetURL = new String("");
		String output;

		scanArgsForFlags(args);

		if (args.length < 2) {
			System.err.println("Too Few Arguments. EXIT");
			return;
		}
		// The first argument must be the URL for the packaging API
		// EX: targetURL = "http://SERVER_NAME/packaging/Package"
		targetURL = args[0];

		// The second argument must be the file or directory for packaging
		// EX: fileOrDirName = "/Users/CURRENT_USER/documents/testPDF.pdf"
		fileOrDirName = args[1];

		FilenameFilter nameFilter;
		if (useDataPath)
			nameFilter = createDataPathFilter();
		else
			nameFilter = createFilenameFilter();

		File dir = new File(fileOrDirName);

		/*
		 * if fileOrDirName specifies a file, generate a package request, create
		 * a new connection to the packaging server, and send it
		 */
		if (dir.isFile()) {
			if (useDataPath) {
				System.err.println("Second argument is a file!");
				System.err
						.println("DataPath mode requires that a directory be passed");
				System.err.println("Tool will EXIT!");
				System.exit(1);
			}
			if (verboseDisplay)
				System.out.println("Found target file: "
						+ dir.getAbsolutePath());

			cleanCurrentDCMetadata();
			currentFileName = fileOrDirName;
			output = makeContent(fileOrDirName);
			try {
				if (output != null)
					sendContent(output, createConnection(targetURL));
			} catch (Exception e) {
				e.printStackTrace();
				errors++;
			}
		}

		/*
		 * else if fileOrDirName specifies a directory, for every child that has
		 * the extension .epub or .pdf generate a package request, create a new
		 * connection to the packaging server, and send it
		 * 
		 * if dataPath mode is engaged, look through
		 */
		else if (dir.isDirectory()) {
			if (verboseDisplay)
				System.out.println("Found target directory: "
						+ dir.getAbsolutePath());

			String[] fileList = dir.list(nameFilter);

			if (fileList == null) {
				if (useDataPath)
					System.err
							.println("No .xml files found in directory. EXIT");
				else
					System.err
							.println("No .pdf or .epub files found in directory. EXIT");
				return;
			} else {
				for (int i = 0; i < fileList.length; i++) {
					hasPermissions = false; // each file may or may not have
					// permissions independently

					/*
					 * to ensure that the correct file path is generated, check
					 * to see if there is a '/' at the end of fileOrDirName. if
					 * there is not, add one
					 */
					if (!fileOrDirName.substring(fileOrDirName.length() - 1,
							fileOrDirName.length()).equals("/")) {
						cleanCurrentDCMetadata();
						currentFileName = fileOrDirName.concat("/").concat(
								fileList[i]);
						output = makeContent(fileOrDirName.concat("/").concat(
								fileList[i]));
					} else {
						cleanCurrentDCMetadata();
						currentFileName = fileOrDirName.concat(fileList[i]);
						output = makeContent(fileOrDirName.concat(fileList[i]));
					}
					try {
						if (output != null)
							sendContent(output, createConnection(targetURL));
					} catch (Exception e) {
						e.printStackTrace();
						errors++;
					}
				}
			}
		} else
		// else the second argument was neither a file name nor a directory
		{
			System.err
					.println("Second argument must be file or directory to package. EXIT");
			return;
		}
		System.out.println("\nFinished! \nSuccessful packages created: "
				+ successes + "\nUnsuccessful package attempts:" + errors);
		if (errors > 0) {
			System.out.println("Here are the files that failed to package: \n"
					+ failedFiles);
		}
	}

}