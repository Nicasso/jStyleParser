package cz.vutbr.web.csskit;

import cz.vutbr.web.css.CodeLocation;

public class CSSError {

	CodeLocation location;
	String message;
	
	public CSSError(CodeLocation location, String message) {
		this.location = location;
		this.message = message;
	}

	public CodeLocation getLocation() {
		return location;
	}
	
	public String getMessage() {
		return message;
	}
	
}
