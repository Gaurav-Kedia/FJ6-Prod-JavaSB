package com.foreverjava.Util;

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

	@JsonProperty("AI_API")
	String AI_API;

	public Creds(){}
	public Creds(String ACCESS_KEY, String SECRET_KEY, String PUBLIC_IP, String EMAIL, String AI_API) {
		this.ACCESS_KEY = ACCESS_KEY;
		this.SECRET_KEY = SECRET_KEY;
		this.PUBLIC_IP = PUBLIC_IP;
		this.EMAIL = EMAIL;
		this.AI_API = AI_API;
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

	@JsonProperty("AI_API")
	public String getAI_API() {
		return AI_API;
	}

	public void setAI_API(String AI_API) {
		this.AI_API = AI_API;
	}
}