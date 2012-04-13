// Copyright 2011 NPR
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.dmbstream.android.activity;

import com.dmbstream.android.R;
import com.dmbstream.android.helpers.ValidationHelper;
import com.dmbstream.android.util.AnalyticsUtil;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TextView;

public abstract class TitleActivity extends RootActivity {
	/**
	 * Implementing classes must override this method to provide a title that
	 * will be shown in the title bar.
	 * <p/>
	 * Note that the title is read in the onCreate so subclasses must be able to
	 * generate the title in this method entirely from data available at the
	 * time it is called. This could mean that subclasses' onCreate calls
	 * super.onCreate after initializing data or it could mean that subclasses
	 * inspect the intent directly in the getMainTitle method, rather than
	 * depending on variables created in onCreate.
	 * 
	 * @return A string for the title bar.
	 */
	protected abstract CharSequence getMainTitle();

	/**
	 * This allows implementing classes to set text that appears on the right
	 * side of the title bar.
	 * 
	 * @param rightText
	 *            The text to render on the right of the title bar
	 */
	protected void setTitleRight(CharSequence rightText) {
		if (rightText != null && rightText.length() > 0) {
			TextView titleRight = (TextView) findViewById(R.id.TitleRight);
			titleRight.setText(rightText);
		}
	}

	private TextView titleText;

	protected void setTitleLeft(CharSequence leftText) {
		if (titleText == null) {
			titleText = (TextView) findViewById(R.id.TitleText);
		}
		titleText.setText(leftText);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ViewGroup container = (ViewGroup) findViewById(R.id.TitleContent);
		ViewGroup.inflate(this, R.layout.layout_title, container);

		setTitleLeft(getMainTitle());
	}

	@Override
	protected void onResume() {
		super.onResume();
		String analyticsPageName = getAnalyticsPageName();
		if (!ValidationHelper.isNullOrWhitespace(analyticsPageName))
			AnalyticsUtil.trackPageView(this, analyticsPageName);
	}
	protected abstract String getAnalyticsPageName();
}
