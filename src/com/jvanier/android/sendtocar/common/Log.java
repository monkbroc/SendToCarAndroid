package com.jvanier.android.sendtocar.common;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.Date;

import android.content.Context;

/**
 * Android Log implementation that delegates to LogCat in development and eats
 * all logs in production unless specifically enabled. In production, logs are
 * saved to a file.
 */
public final class Log {

	private static boolean mEnabled = false;
	private static FileOutputStream mLogFileOut;

	private final static String LOG_FILENAME = "sendtocar.log";

	private Log() {
	}

	public static boolean isEnabled() {
		return mEnabled;
	}

	public static boolean hasLogFile() {
		return mLogFileOut != null;
	}

	public static void enableToLogCat() {
		mEnabled = true;
	}

	public static void enableToFile(Context context) {
		mEnabled = true;

		if(mLogFileOut == null) {
			try {
				mLogFileOut = context.openFileOutput(LOG_FILENAME, Context.MODE_PRIVATE | Context.MODE_APPEND);
			} catch(FileNotFoundException e) {
				mLogFileOut = null;
			}
		}
	}

	public static int println(String tag, String msg) {
		String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
		String line = "[" + currentDateTimeString + " - " + tag + "] " + msg;
		try {
			mLogFileOut.write(line.getBytes());
		} catch(IOException e) {
			return -1;
		}
		return 0;
	}

	public static String readLog(Context context) {
		BufferedReader logFileIn = null;
		StringBuilder sb = null;
		try {
			logFileIn = new BufferedReader(new InputStreamReader(context.openFileInput(LOG_FILENAME)));

			sb = new StringBuilder();
			String line;
			while((line = logFileIn.readLine()) != null) {
				sb.append(line);
			}

			return sb.toString();
		} catch(FileNotFoundException e) {
			// do nothing
		} catch(IOException e) {
			// do nothing
		} finally {
			if(logFileIn != null) {
				try {
					logFileIn.close();
				} catch(IOException e) {
					// do nothing
				}
			}
		}

		if(sb != null) {
			return sb.toString();
		} else {
			return null;
		}
	}

	public static void disableAndDeleteFile(Context context) {
		mEnabled = false;
		if(mLogFileOut != null) {
			try {
				mLogFileOut.close();
			} catch(IOException e) {
				// do nothing
			}
		}
		context.deleteFile(LOG_FILENAME);
	}

	/* Delegate to Android Log */

	public static int v(String tag, String msg) {
		if(!mEnabled)
			return 0;
		if(mLogFileOut != null) {
			return println(tag, msg);
		} else {
			return android.util.Log.v(tag, msg);
		}
	}

	public static int v(String tag, String msg, Throwable tr) {
		if(!mEnabled)
			return 0;
		if(mLogFileOut != null) {
			return println(tag, msg + '\n' + android.util.Log.getStackTraceString(tr));
		} else {
			return android.util.Log.v(tag, msg, tr);
		}
	}

	public static int d(String tag, String msg) {
		if(!mEnabled)
			return 0;
		if(mLogFileOut != null) {
			return println(tag, msg);
		} else {
			return android.util.Log.d(tag, msg);
		}
	}

	public static int d(String tag, String msg, Throwable tr) {
		if(!mEnabled)
			return 0;
		if(mLogFileOut != null) {
			return println(tag, msg + '\n' + android.util.Log.getStackTraceString(tr));
		} else {
			return android.util.Log.d(tag, msg, tr);
		}
	}

	public static int i(String tag, String msg) {
		if(!mEnabled)
			return 0;
		if(mLogFileOut != null) {
			return println(tag, msg);
		} else {
			return android.util.Log.d(tag, msg);
		}
	}

	public static int i(String tag, String msg, Throwable tr) {
		if(!mEnabled)
			return 0;
		if(mLogFileOut != null) {
			return println(tag, msg + '\n' + android.util.Log.getStackTraceString(tr));
		} else {
			return android.util.Log.i(tag, msg, tr);
		}
	}

	public static int w(String tag, String msg) {
		if(!mEnabled)
			return 0;
		if(mLogFileOut != null) {
			return println(tag, msg);
		} else {
			return android.util.Log.w(tag, msg);
		}
	}

	public static int w(String tag, String msg, Throwable tr) {
		if(!mEnabled)
			return 0;
		if(mLogFileOut != null) {
			return println(tag, msg + '\n' + android.util.Log.getStackTraceString(tr));
		} else {
			return android.util.Log.w(tag, msg, tr);
		}
	}

	public static int w(String tag, Throwable tr) {
		if(!mEnabled)
			return 0;
		if(mLogFileOut != null) {
			return println(tag, android.util.Log.getStackTraceString(tr));
		} else {
			return android.util.Log.w(tag, tr);
		}
	}

	public static int e(String tag, String msg) {
		if(!mEnabled)
			return 0;
		if(mLogFileOut != null) {
			return println(tag, msg);
		} else {
			return android.util.Log.e(tag, msg);
		}
	}

	public static int e(String tag, String msg, Throwable tr) {
		if(!mEnabled)
			return 0;
		if(mLogFileOut != null) {
			return println(tag, msg + '\n' + android.util.Log.getStackTraceString(tr));
		} else {
			return android.util.Log.e(tag, msg, tr);
		}
	}
}
