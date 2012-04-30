package com.jvanier.android.sendtocar;


public interface DebugLog {
	public abstract void d(String s);
	public abstract void close();

	public abstract String htmlSnippet(String s);
}
