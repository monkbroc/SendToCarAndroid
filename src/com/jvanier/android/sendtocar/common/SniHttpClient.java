package com.jvanier.android.sendtocar.common;

import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;

/* Class that works around the POODLE issue by disabling SSLv3 */
public class SniHttpClient extends DefaultHttpClient {
	public SniHttpClient() {
		super();
		// use our own, SNI-capable LayeredSocketFactory for https://
		SchemeRegistry schemeRegistry = getConnectionManager().getSchemeRegistry();
		schemeRegistry.register(new Scheme("https", new TlsSniSocketFactory(), 443));
	}
}
