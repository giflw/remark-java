/*
 * Copyright 2011 OverZealous Creations, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.overzealous.remark.nodewalker;

import com.overzealous.remark.Options;
import com.overzealous.remark.util.StringUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is used to clean up plain text fields based on the selected set of options.
 * It optionally escapes certain special characters, as well as replacing various
 * HTML and Unicode entities with their plaintext equivalents.
 *
 * @author Phil DeJarnett
 */
public class TextCleaner {

	/**
	 * Internal class simply used to hold the various escape regexes.
	 */
	private class Escape {
		Pattern pattern;
		String replacement;
		public Escape(String pattern, String replacement) {
			this.pattern = Pattern.compile(pattern);
			this.replacement = replacement;
		}
	}

	/** Used to track the replacements based on matched groups. */
	private Map<String, String> replacements;
	/** Compiled entity replacement pattern. */
	private Pattern entityReplacementsPattern;
	/** Compiled unicode replacement pattern. */
	private Pattern unicodeReplacementsPattern = null;
	/** List of possible escapes */
	private List<Escape> escapes;

	private static final Pattern LINEBREAK_REMOVER = Pattern.compile("(\\s*\\n)+");

	/**
	 * Create a new TextCleaner based on the configured options.
	 * @param options Options that will affect what is cleaned.
	 */
	public TextCleaner(Options options) {
		setupReplacements(options);
		setupEscapes(options);
	}

	/**
	 * Configures the basic replacements based on the configured options.
	 * @param options Options that will affect what is replaced.
	 */
	@SuppressWarnings({"OverlyLongMethod"})
	private void setupReplacements(Options options) {
		this.replacements = new HashMap<String, String> ();

		// build replacement regex
		StringBuilder entities = new StringBuilder(replacements.size()*5);

		// this is a special case for double-encoded HTML entities.
		entities.append("&(?>amp;([#a-z0-9]++;)|(?>");
		addRepl(entities, "&amp;", "&");
		addRepl(entities, "&lt;", "<");
		addRepl(entities, "&gt;", ">");
		addRepl(entities, "&quot;", "\"");
		if(options.reverseHtmlSmartQuotes) {
			addRepl(entities, "&ldquo;", "\"");
			addRepl(entities, "&rdquo;", "\"");
			addRepl(entities, "&lsquo;", "\'");
			addRepl(entities, "&rsquo;", "\'");
			addRepl(entities, "&apos;", "\'");
			addRepl(entities, "&laquo;", "<<");
			addRepl(entities, "&raquo;", ">>");
		}
		if(options.reverseHtmlSmartPunctuation) {
			addRepl(entities, "&ndash;", "--");
			addRepl(entities, "&mdash;", "---");
			addRepl(entities, "&hellip;", "...");
		}
		entities.replace(entities.length()-1, entities.length(), ");)");

		entityReplacementsPattern = Pattern.compile(entities.toString(), Pattern.CASE_INSENSITIVE);

		if(options.reverseUnicodeSmartPunctuation || options.reverseUnicodeSmartQuotes) {
			StringBuilder unicode = new StringBuilder("[\\Q");
			if(options.reverseUnicodeSmartQuotes) {
				addRepl(unicode, "\u201c", "\""); // left double quote: “
				addRepl(unicode, "\u201d", "\""); // right double quote: ”
				addRepl(unicode, "\u2018", "\'"); // left single quote: ‘
				addRepl(unicode, "\u2019", "\'"); // right single quote: ’
				addRepl(unicode, "\u00ab", "<<"); // left angle quote: «
				addRepl(unicode, "\u00bb", ">>"); // right angle quote: »
			}
			if(options.reverseUnicodeSmartPunctuation) {
				addRepl(unicode, "\u2013", "--"); // en-dash: –
				addRepl(unicode, "\u2014", "---"); // em-dash: —
				addRepl(unicode, "\u2026", "..."); // ellipsis: …
			}
			unicode.append("\\E]");
			unicodeReplacementsPattern = Pattern.compile(unicode.toString());
		}
	}

	/**
	 * Utility method to make the code above easier to read.
	 * @param regex A character buffer to append the replacement to
	 * @param original Original character or string.
	 * @param replacement Replacement character or string.
	 */
	private void addRepl(StringBuilder regex, String original, String replacement) {
		replacements.put(original, replacement);
		if(original.charAt(0) == '&') {
			// add entity
			regex.append(original.substring(1, original.length() - 1));
			regex.append('|');
		} else {
			// add single character
			regex.append(original);
		}
	}

	/**
	 * Configures the basic escapes based on the configured options.
	 * @param options Options that will affect what is escaped.
	 */
	private void setupEscapes(Options options) {
		escapes = new ArrayList<Escape>();

		// confusingly, this replaces single backslashes with double backslashes.
		// Man, I miss Groovy's slashy strings in these moments...
		escapes.add(new Escape("\\\\", "\\\\"));

		// creates an set of characters that are universally escaped.
		// these characters are wrapped in \Q...\E to ensure they aren't treated as special characters.
		StringBuilder chars = new StringBuilder("([\\Q`*_{}[]#");
		if(options.tables.isConvertedToText() && !options.tables.isRenderedAsCode()) {
			chars.append('|');
		}
		chars.append("\\E])");
		escapes.add(new Escape(chars.toString(), "\\\\$1"));

		// finally, escape certain characters only if they are leading characters
		StringBuilder leadingChars = new StringBuilder("^( ?+)([\\Q-+");
		if(options.definitionLists) {
			leadingChars.append(':');
		}
		leadingChars.append("\\E])");
		escapes.add(new Escape(leadingChars.toString(), "$1\\\\$2"));
	}

	/**
	 * Clean the given input text based on the original configuration Options.
	 * Newlines are also replaced with a single space.
	 *
	 * @param input The text to be cleaned.
	 * @return The cleaned text.
	 */
	public String clean(String input) {
		return clean(input, true);
	}

	/**
	 * Clean the given input text based on the original configuration Options.
	 * The text is treat as code, so it is not escaped, and newlines are preserved.
	 *
	 * @param input The text to be cleaned.
	 * @return The cleaned text.
	 */
	public String cleanCode(String input) {
		return clean(input, false);
	}

	/**
	 * Clean the given input text based on the original configuration Options.
	 * Optionally, don't escape special characters.
	 *
	 * @param input The text to be cleaned.
	 * @param normalText If false, don't escape special characters.  This is usually only used for
	 * 					 inline code or code blocks, because they don't need to be escaped.
	 * @return The cleaned text.
	 */
	protected String clean(String input, boolean normalText) {
		if(normalText) {
			// For non-code text, newlines are _never_ allowed.
			// Replace one or more set of whitespace chars followed by a newline with a single space.
			input = LINEBREAK_REMOVER.matcher(input).replaceAll(" ");

			// now escape special characters.
			for(final Escape rep : escapes) {
				input = rep.pattern.matcher(input).replaceAll(rep.replacement);
			}
			StringBuffer output = doReplacements(input, entityReplacementsPattern);
			if(unicodeReplacementsPattern != null) {
				output = doReplacements(output, unicodeReplacementsPattern);
			}
			return output.toString();
		} else {
			// we have to revert ALL HTML entities for code, because they will end up
			// double-encoded by markdown
			// we also don't need to worry about escaping anything
			// note: we have to manually replace &apos; because it is ignored by StringEscapeUtils for some reason.
			return StringEscapeUtils.unescapeHtml4(input.replace("&apos;", "'"));
		}
	}

	/**
	 * Handles running the regex-based replacements in the input
	 * @param input String to process
	 * @param regex Pattern to use
	 * @return cleaned up input string
	 */
	private StringBuffer doReplacements(CharSequence input, Pattern regex) {
		StringBuffer output = new StringBuffer();

		Matcher m = regex.matcher(input);
		while (m.find()) {
			String repString;
			// if we have a hard match, do a simple replacement.
			String replacementKey = m.group().toLowerCase(Locale.ENGLISH);
			if(replacements.containsKey(replacementKey)) {
				repString = replacements.get(replacementKey);
			} else {
				// special case for escaped HTML entities.
				repString = "\\\\&$1";
			}
			m.appendReplacement(output, repString);
		}
		m.appendTail(output);

		return output;
	}

	/**
	 * Method to clean inline code, and, if necessary, add spaces to make sure that internal, leading, or
	 * trailing {@code '`'} characters don't break the inline code.
	 * Newlines are also replaced with spaces.
	 *
	 * This method also adds the leading and trailing {@code '`'} or {@code '```'} as necessary.
	 *
	 * @param input String to clean
	 * @return The cleaned text.
	 */
	public String cleanInlineCode(String input) {
		String output = clean(input, false).replace('\n', ' ');
		if(output.indexOf('`') != -1) {
			String prepend = "";
			if(output.charAt(0) == '`') {
				prepend = " ";
			}
			String append = "";
			if(output.charAt(output.length()-1) == '`') {
				append = " ";
			}
			String delim = getDelimiter(output);
			output = String.format("%s%s%s%s%s", delim, prepend, output, append, delim);
		} else {
			output = String.format("`%s`", output);
		}
		return output;
	}

	protected String getDelimiter(String input) {
		int max = 0;
		int counter = 0;
		for(int i=0; i<input.length(); i++) {
			if(input.charAt(i) == '`') {
				counter++;
			} else {
				max = Math.max(max, counter);
				counter = 0;
			}
		}
		// check in case the last tick was at the end.
		max = Math.max(max, counter);
		return StringUtils.multiply('`', max+1);
	}

}
