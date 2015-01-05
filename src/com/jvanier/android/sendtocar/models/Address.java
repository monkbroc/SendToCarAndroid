package com.jvanier.android.sendtocar.models;

public class Address implements Cloneable {
	public String title;
	public String latitude;
	public String longitude;
	public String street;
	public String number;
	public String city;
	public String province;
	public String postalCode;
	public String country;
	public String internationalPhone;
	public String phone;
	public String displayAddress;

	public boolean hasAddressDetails() {
		return number != null && number.length() > 0 && city != null && city.length() > 0 && province != null && province.length() > 0
				&& country != null && country.length() > 0;
	}

	public boolean hasLatitudeLongitude() {
		return latitude != null && latitude.length() > 0 && longitude != null && longitude.length() > 0;
	}
}
