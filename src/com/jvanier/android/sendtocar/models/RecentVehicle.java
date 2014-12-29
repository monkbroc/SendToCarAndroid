package com.jvanier.android.sendtocar.models;

import java.io.Serializable;

public class RecentVehicle implements Comparable<RecentVehicle>, Serializable {
	private static final long serialVersionUID = -6457983665195220159L;
	public String makeId;
	public String make;
	public String account;
	
	@Override
	public int compareTo(RecentVehicle another) {
		int makeCompare = makeId.compareTo(another.makeId);
		if(makeCompare == 0) {
			return account.compareTo(another.account);
		} else {
			return makeCompare;
		}
	}

	@Override
	public String toString() {
		return make + "(" + account + ")"; 
	}
}
