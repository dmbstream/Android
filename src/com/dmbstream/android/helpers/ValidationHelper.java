package com.dmbstream.android.helpers;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class ValidationHelper {
	public static Boolean isNullOrWhitespace(String input) {
		if (input == null || StringUtils.isEmpty(input) || StringUtils.isWhitespace(input)) 
			return true;
		return false;
	}
	public static Boolean validateZip(String input) {
		if (isNullOrWhitespace(input)) 
			return false;
		
		// Just validate US ZIP Codes for now
		return Pattern.matches("^\\d{5}([\\-]\\d{4})?$", input);
	}
	public static Boolean validatePhone(String input) {
		if (isNullOrWhitespace(input)) 
			return false;
		
		// Just validate US ZIP Codes for now
		return Pattern.matches("^\\d{3}\\-\\d{3}\\-\\d{4}$", input);
	}
	public static Boolean validateEmail(String input) {
		if (isNullOrWhitespace(input)) 
			return false;
		
		return Pattern.matches("[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?", input);
	}
}
