package com.overzealous.remark;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * @author Phil DeJarnett
 */
public class Remark {

	public static void main(String args[]) throws Exception {
		Document doc = Jsoup.connect("http://www.overzealous.com").get();
		System.out.printf("Found: %s\n", doc.select("h1").get(0).text());
	}

}
