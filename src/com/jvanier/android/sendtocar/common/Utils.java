package com.jvanier.android.sendtocar.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

public class Utils {
	// True if the app build is for development, not Google Play
	public static boolean isDevelopment(Context c) {
		try {
			PackageManager pm = c.getPackageManager();
			PackageInfo pi = pm.getPackageInfo(c.getPackageName(), 0);
			return((pi.applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
		} catch(Exception e) {
			return false;
		}
	}

	public static String ReadInputStream(InputStream in) throws IOException {
		StringBuffer stream = new StringBuffer();
		byte[] b = new byte[4096];
		for(int n; (n = in.read(b)) != -1;) {
			stream.append(new String(b, 0, n));
		}

		return stream.toString();
	}

	public static String htmlSnippet(String s) {
		return TextUtils.htmlEncode(s.substring(0, Math.min(s.length(), 1000)));
	}

	public static String decodeHtml(String s) {
		/* Adapted from the android.net.Uri.decode() function */

		/*
		 * Compared to java.net.URLEncoderDecoder.decode(), this method decodes
		 * a chunk at a time instead of one character at a time, and it doesn't
		 * throw exceptions. It also only allocates memory when necessary--if
		 * there's nothing to decode, this method won't do much.
		 */
		if(s == null) {
			return null;
		}
		// Lazily-initialized buffers.
		StringBuilder decoded = null;
		Pattern escape = Pattern.compile("&#([0-9]+);");

		int oldLength = s.length();
		// This loop alternates between copying over normal characters and
		// decoding in chunks. This results in fewer method calls and
		// allocations than decoding one character at a time.
		int current = 0;
		Matcher matcher = escape.matcher(s);

		while(matcher.find()) {
			// Copy over normal characters until match
			int nextEscape = matcher.start();

			// Prepare buffers.
			if(decoded == null) {
				// Looks like we're going to need the buffers...
				// We know the new string will be shorter. Using the old length
				// may overshoot a bit, but it will save us from resizing the
				// buffer.
				decoded = new StringBuilder(oldLength);
			}

			// Append characters leading up to the escape.
			if(nextEscape > current) {
				decoded.append(s, current, nextEscape);
			} else {
				// assert current == nextEscape
			}
			current = matcher.end();

			// Decode and append escape sequence. Escape sequences look like
			// "&#N;" where &# and ; are literal and N is a decimal number
			// (several digits)
			try {
				// Combine the hex digits into one byte and write.
				char c = (char) Integer.parseInt(matcher.group(1));
				decoded.append(c);
			} catch(NumberFormatException e) {
				throw new AssertionError(e);
			}
		}

		if(decoded == null) {
			// We didn't actually decode anything.
			return s;
		} else {
			// Append the remainder and return the decoded string.
			decoded.append(s, current, oldLength);
			return decoded.toString();
		}
	}

	public static List<String> findURLs(String string) {
		List<String> urls = new ArrayList<String>();

		// match a URL, but try not to grab the punctuation at the end
		Pattern urlPatt = Pattern.compile("\\bhttps?://[a-z0-9_.\\-,@?^=%&:/~+#!]*[a-z0-9_\\-@^=%&:/~+#]", Pattern.CASE_INSENSITIVE);
		Matcher matcher = urlPatt.matcher(string);

		while(matcher.find()) {
			urls.add(matcher.group());
		}

		return urls;
	}

	public static String left(String string, int len) {
		if(string != null) {
			return string.substring(0, Math.min(len, string.length()));
		} else {
			return null;
		}
	}
}
