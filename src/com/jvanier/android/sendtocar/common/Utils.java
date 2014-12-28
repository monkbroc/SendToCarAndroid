package com.jvanier.android.sendtocar.common;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class Utils {
	public static boolean isDebug(Context c) {
		try {
			PackageManager pm = c.getPackageManager();
			PackageInfo pi = pm.getPackageInfo(c.getPackageName(), 0);
			return ((pi.applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
		} catch (Exception e) {
			return false;
		}
	}

	public static String ReadInputStream(InputStream in) throws IOException {
		StringBuffer stream = new StringBuffer();
		byte[] b = new byte[4096];
		for (int n; (n = in.read(b)) != -1;) {
			stream.append(new String(b, 0, n));
		}

		return stream.toString();
	}
}
