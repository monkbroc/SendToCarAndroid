package com.jvanier.android.sendtocar.models;

import java.io.Serializable;

public class RecentVehicle implements Serializable {
	private static final long serialVersionUID = -6457983665195220159L;
	public String makeId;
	public String make;
	public String account;
	
	@Override
	public boolean equals(Object other) {
		if(other instanceof RecentVehicle) {
			RecentVehicle otherVehicle = (RecentVehicle) other;
			return makeId.equals(otherVehicle.makeId) && account.equals(otherVehicle.account);
		} else {
			return super.equals(other);
		}
	}

	@Override
	public String toString() {
		return make + "(" + account + ")"; 
	}
}
