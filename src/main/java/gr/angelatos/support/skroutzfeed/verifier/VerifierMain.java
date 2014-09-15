package gr.angelatos.support.skroutzfeed.verifier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.ParsingException;

public class VerifierMain {

	private static final String DEFAULTFEED = "http://angelatos.gr/product_feed.php?type=skroutz";


	public static void main(String args[]) throws Exception {
		VerifierMain m = new VerifierMain();
		System.out.println("Feed verifier for Skroutz - http://angelatos.gr open source");
		if (args.length > 0) {
			m.verifyFeed(args[0]);
		} else {
			m.verifyFeed(null);
		}
	}

	private void verifyFeed(String feed) throws Exception{
		if (feed == null) {
			feed = DEFAULTFEED;
		}
		System.out.println("About to verify the feed from "+feed);
		try {
			Builder parser = new Builder();
			Document doc = parser.build(retrieveHttpFeed(feed));
			
			Nodes products = doc.query("//product");		

			System.err.println("Error codes ");
			printErrorCodes();
			
			System.err.println("Found "+products.size()+" products - checking....\n\n");
			for (int i = 0; i < products.size(); i++)  {
				Node node = products.get(i);
				checkImage(getTextValue(node, "id"), node);
				checkMPN(getTextValue(node, "id"), node);
			}	
			
		} catch (ParsingException ex) {
			System.err.println("Cannot complete parsing, error = "+ex.getMessage());
			ex.printStackTrace();
		} catch (IOException ex) {
			System.err.println("Could not connect to the site. The site may be down.");
			ex.printStackTrace();
		}
	}

	private InputStream retrieveHttpFeed(String feed) throws Exception {

		URL url = new URL(feed);
		long t0 = System.currentTimeMillis();

		URLConnection con = url.openConnection();
		con.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		con.addRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.64 Safari/537.11");
		con.setConnectTimeout(30000);
		con.setDoInput(true);
		con.setDoOutput(true);
		con.setReadTimeout(180000);
		con.connect();
		System.out.println("Connected to "+feed);
		for (Map.Entry<String, List<String>> k : con.getHeaderFields().entrySet()) {
		    for (String v : k.getValue()){
		         System.out.println(k.getKey() + ":" + v);
		    }
		}
		
		int bytesRead = 0, totalBytes = 0; 
		long t1 = 0;
		InputStream stream = con.getInputStream();
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		
		GZIPOutputStream gzOut = null;
		try {
			gzOut = new GZIPOutputStream(new FileOutputStream(new File("latestfeed.xml.gz")));
			byte[] byteBuffer = new byte[4096];
			boolean first = true;
			while ( (bytesRead = stream.read(byteBuffer)) > -1) {
				if (first) {
					t1 = System.currentTimeMillis();
					first = false;
				}
				bout.write(byteBuffer, 0, bytesRead);
				gzOut.write(byteBuffer, 0, bytesRead);
				
				totalBytes += bytesRead;
				
				System.out.print("Reading "+totalBytes+" so far... \r");
			}
			gzOut.flush();
			long now = System.currentTimeMillis();
			System.out.println();
			System.out.println(" --> waiting time to start getting data : "+(t1-t0)+"ms.");
			System.out.println(" --> time of download : "+(now-t1)+"ms.");
			System.out.println("total time : "+(now-t0)+"ms.");
			
		} finally {
			if (gzOut != null) gzOut.close();
		}
		return new ByteArrayInputStream(bout.toByteArray());
	}

	private void printErrorCodes() {
		for (int i=0; i< errors.length; i++) {
			System.err.println("\tERR-"+errors[i][0]+"\t\t"+errors[i][1]);
		}
	}

	private void checkMPN(String id, Node node) {
		String mpn = getTextValue(node, "MPN");
		String title = getTextValue(node, "title");
		if (!title.contains(mpn)) {
			logError(error("0001"), title(node), id, mpn, "'"+title+"' does not have the MPN '"+mpn+"' in the text");
		}
 		if (mpn.contains("+")) { //|| (mpn.contains(" "))) {
			logError(error("0005"), title(node), id, mpn, "MPN '"+mpn+"' contains + in the code, it is not valid");

 		}
 	}

	private final String getTextValue(Node parent, String tagName) {
		return parent.query(tagName).get(0).getValue();
	}

	
	private void checkImage(String id, Node node) {
		String imgUrl = getTextValue(node, "image");
		if (imgUrl.contains(" ")) {
			logError(error("0002"), title(node), id, getTextValue(node, "MPN"), "image has spaces in filename: "+imgUrl);
		} else if (imgUrl.contains("?")) {
			logError(error("0003"), title(node), id, getTextValue(node, "MPN"), "image has ? in filename: "+imgUrl);
		} else if (!imgUrl.startsWith("http")) {
			logError(error("0004"), title(node), id, getTextValue(node, "MPN"), "image not http:// : "+imgUrl);
		} else {
			//System.out.println("product '"+title(node)+"...' "+id+" img is OK: "+imgUrl);
		}
		
	}

	
	private String error(String code) {
		for (int i=0; i<errors.length; i++) {
			if (code.contentEquals((String) errors[i][0])) {
				return (String) errors[i][0] ;
			}
		}
		return "";
	}

	private static void logError(String errorCode, String prodTitle, String id, String MPN, String message) {
		System.err.println("ERR-"+errorCode+" | '"+prodTitle+"' | "+id+" | "+MPN+" | "+message);
	}
	private String title(Node node) {
		String value = getTextValue(node, "title");
		return value.substring(0,Math.min(value.length(), 15));
	}


	private Object[][] errors = { 
		{ "0001", "Το MPN δεν εμφανιζεται στον τιτλο ή είναι διαφορετικό" },
		{ "0002", "Η εικόνα του προϊοντος έχει κενά ή λάθος χαρακτήρες" },
		{ "0003", "Η εικόνα έχει ???? στο όνομα ή στη διεύθυνση" },
		{ "0004", "Η εικόνα δεν ξεκινά με http://" },
		{ "0005", "Το MPN εχει + μεσα στον κωδικο"}
	};



}


