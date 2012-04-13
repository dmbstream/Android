package com.dmbstream.android.helpers;

import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.widget.TextView;
import android.widget.TextView.BufferType;

public class StringHelper {
	public static String ellipsize(String input, int maxLength) {
	    if (input == null)
	        return null;
	    if ((input.length() < maxLength) || (maxLength < 3))
	        return input;
	    return input.substring (0, maxLength - 3) + "...";
	}
	public static String toFriendlyUrl(String input) {
		return input.toLowerCase().replaceAll("[^\\w\\d]", "-").replace("_", "-").replaceAll("--+", "-");
	}
	public static String textOrEmpty(String input) {
		if (input == null)
			return "";
		return input;
	}
	public static Spanned getFixedErrorString(String input) {
//		SpannableString string = new SpannableString(input);
//		string.setSpan(new ForegroundColorSpan(color.secondary_text_light), 0, input.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//		return string;
		return Html.fromHtml("<font color='black'>" + input + "</font>");
	}
	public static void makeClickable(TextView view, String clickText, ClickableSpan clickLogic) {
		String originalText = view.getText().toString();
		int index = originalText.indexOf(clickText);
		if (index < 0)
			return;
		
		int length = clickText.length();
		SpannableString newText = new SpannableString(originalText);
		while (index > -1) {
			newText.setSpan(clickLogic, index, index + length, 0);
			
			index = originalText.indexOf(clickText, index + length);
		}
		view.setMovementMethod(LinkMovementMethod.getInstance());
		view.setText(newText, BufferType.SPANNABLE);
	}
}
