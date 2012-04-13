package com.dmbstream.android.helpers;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonHelper {
	
	public static Calendar parseDate(String input) {
        Pattern pattern = Pattern.compile("\\/Date\\((\\d+)([\\+\\-]\\d+)?\\)\\/");
        Matcher matcher = pattern.matcher(input);
        matcher.matches();
        String timezone = matcher.group(2);
        String timeInMilliseconds = matcher.group(1);

		Calendar c = new GregorianCalendar();
		c.setTimeZone(TimeZone.getTimeZone("GMT" + timezone));
		c.setTimeInMillis(Long.parseLong(timeInMilliseconds));
		
		return c;
	}
}
