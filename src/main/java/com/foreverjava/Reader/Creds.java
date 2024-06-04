package com.foreverjava.Reader;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Creds {
	@JsonProperty("ACCESS_KEY")
	String ACCESS_KEY;

	@JsonProperty("SECRET_KEY")
	String SECRET_KEY;

	@JsonProperty("PUBLIC_IP")
	String PUBLIC_IP;

	@JsonProperty("EMAIL")
	String EMAIL;

	public Creds(){}
	public Creds(String ACCESS_KEY, String SECRET_KEY, String PUBLIC_IP, String EMAIL) {
		this.ACCESS_KEY = ACCESS_KEY;
		this.SECRET_KEY = SECRET_KEY;
		this.PUBLIC_IP = PUBLIC_IP;
		this.EMAIL = EMAIL;
	}

	@JsonProperty("ACCESS_KEY")
	public String getACCESS_KEY() {
		return ACCESS_KEY;
	}

	public void setACCESS_KEY(String ACCESS_KEY) {
		this.ACCESS_KEY = ACCESS_KEY;
	}

	@JsonProperty("SECRET_KEY")
	public String getSECRET_KEY() {
		return SECRET_KEY;
	}

	public void setSECRET_KEY(String SECRET_KEY) {
		this.SECRET_KEY = SECRET_KEY;
	}

	@JsonProperty("PUBLIC_IP")
	public String getPUBLIC_IP() {
		return PUBLIC_IP;
	}

	public void setPUBLIC_IP(String PUBLIC_IP) {
		this.PUBLIC_IP = PUBLIC_IP;
	}

	@JsonProperty("EMAIL")
	public String getEMAIL() {
		return EMAIL;
	}

	public void setEMAIL(String EMAIL) {
		this.EMAIL = EMAIL;
	}
}