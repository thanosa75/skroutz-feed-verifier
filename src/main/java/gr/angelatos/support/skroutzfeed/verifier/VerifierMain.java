package gr.angelatos.support.skroutzfeed.verifier;

import java.io.IOException;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.ParsingException;

public class VerifierMain {

	private static final String DEFAULTFEED = "http://angelatos.gr/product_feed.php?type=skroutz";


	public static void main(String args[]) {
		VerifierMain m = new VerifierMain();
		System.out.println("Feed verifier for Skroutz - http://angelatos.gr open source");
		if (args.length > 0) {
			m.verifyFeed(args[0]);
		} else {
			m.verifyFeed(null);
		}
	}

	private void verifyFeed(String feed) {
		if (feed == null) {
			feed = DEFAULTFEED;
		}
		System.out.println("About to verify the feed from "+feed);
		try {
			Builder parser = new Builder();
			Document doc = parser.build(feed);
			
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
		}
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


