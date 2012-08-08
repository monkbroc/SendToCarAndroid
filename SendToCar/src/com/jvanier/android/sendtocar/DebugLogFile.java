package com.jvanier.android.sendtocar;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.Date;

import android.content.Context;
import android.text.TextUtils;

public class DebugLogFile implements DebugLog {
	private final static String FILENAME = "sendtocar.log";

	private FileOutputStream out;

	public DebugLogFile(Context context) {
		try {
			out = context.openFileOutput(FILENAME, Context.MODE_PRIVATE | Context.MODE_APPEND);
		}
		catch(FileNotFoundException e) {
			out = null;
		}
	}

	@Override
	public void d(String s) {
		if(out != null)
		{
			try {
				String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
				String line = "<div><span><b>" + currentDateTimeString + "</b></span><br><span>" + s + "</span></div>";

				out.write(line.getBytes());
			} catch(IOException e) {
				// do nothing
			}
			
		}
	}

	@Override
	public void close() {
		if(out != null)
		{
			try {
				out.close();
			} catch(IOException e) {
				// do nothing
			}
			out = null;
		}
	}
	
	@Override
	public String htmlSnippet(String s) {
		return TextUtils.htmlEncode(s.substring(0, Math.min(s.length(), 1000)));
	}

	public static String readLog(Context context) {
		BufferedReader in = null;
		StringBuilder sb = null;
		try {
			in = new BufferedReader(new InputStreamReader(context.openFileInput(FILENAME)));
			
			sb = new StringBuilder();
			String line;
			while((line = in.readLine()) != null) {
				sb.append(line);
			}
			
			return sb.toString();
		}
		catch(FileNotFoundException e) {
			// do nothing
		}
		catch(IOException e) {
			// do nothing
		}
		finally {
			if (in != null) {
				try {
					in.close();
				} catch(IOException e) {
					// do nothing
				}
			}
		}
		
		if(sb != null)
		{
			return sb.toString();
		}
		else
		{
			return null;
		}
	}
	
	public static void clearLog(Context context) {
		context.deleteFile(FILENAME);
	}

}
