package com.dmbstream.android.activity;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.dmbstream.android.R;
import com.dmbstream.android.helpers.JsonHelper;
import com.dmbstream.android.helpers.ValidationHelper;
import com.dmbstream.android.util.*;
import com.dmbstream.api.ApiConstants;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TableRow.LayoutParams;

public class ChatActivity extends TitleActivity {
	private static final String TAG = ChatActivity.class.getSimpleName();
	private static final int MaxChatMessages = 200;
	private static final String InitialNumberOfChatMessagesToLoad = "50";
	private Long lastChatMessageId = -1L;
	private int rowCounter = 0;
	
	private ScrollView scroller;
	private LinearLayout stickyMessageHolder;
	private TextView stickyMessage;
	private TableLayout chatMessagesTable;
	private EditText message;
	private static CachedImageLoader imageLoader;
	private ImageGetter imageGetter;

    private int errorCount = 0;
    private int refreshRate = 15000;
    private View.OnClickListener trClickListener;

    private static final int MESSAGE_UPDATE = 0x0;
	private static final int MESSAGE_ADD = 0x1;
	private static final int MESSAGE_REFRESH = 0x2;
	private final Handler handler = new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			stopIndeterminateProgressIndicator();
			switch (msg.what) {
				case MESSAGE_ADD:
					// Temporarily disable auto updates since the add call will get the latest messages
			    	handler.removeCallbacks(updateTask);
			        Thread addMessageThread = new Thread(new Runnable() {
			        	public void run() {
					    	try {
								final JSONObject result = ChatActivity.this.sendMessage();
								
								if (result == null)
									return;
								
			    				runOnUiThread(new Runnable() {
									@Override
									public void run() {
										message.setText("");							            
										// Hide the soft keyboard
										InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
							            imm.hideSoftInputFromWindow(message.getApplicationWindowToken(), 0);
										updateChat(result);
							            scrollToBottom();
							            stopIndeterminateProgressIndicator();
									}
			    				});
							} catch (Exception e) {
		    		    		Toast toast = Toast.makeText(ChatActivity.this, "There was a problem sending the message.  Please try again.", Toast.LENGTH_LONG);
		    		    		toast.setGravity(Gravity.CENTER, 0, 0);
		    		    		toast.show();
			    				Log.e(TAG, "Error sending message: " + e);
			    				++errorCount;							
			    			}
			    			handler.sendEmptyMessage(MESSAGE_REFRESH);
			        	}
			        });
			        addMessageThread.start();
			        break;
				case MESSAGE_UPDATE:
			   		startIndeterminateProgressIndicator();
			        Thread updateThread = new Thread(new Runnable() {
			        	public void run() {
			    			try {
			    				final JSONObject result = getLatestChatMessages();
			    				runOnUiThread(new Runnable() {
									@Override
									public void run() {
										updateChat(result);
										stopIndeterminateProgressIndicator();
									}
			    				});
			    			} catch (Exception e) {
			    				Log.e(TAG, "Error updating chat: " + e);
			    				++errorCount;
			    			}
			    			handler.sendEmptyMessage(MESSAGE_REFRESH);
			        	}
			        });
			        updateThread.start();
					break;
				case MESSAGE_REFRESH:
					if (errorCount < 5)
						handler.postDelayed(updateTask, refreshRate);
					else
						handler.postDelayed(updateTask, refreshRate * 10);
					break;
			}
		}
	};

	private Runnable updateTask = new Runnable() {
		@Override
		public void run() {
			handler.sendEmptyMessage(MESSAGE_UPDATE);
		}
	};

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewGroup container = (ViewGroup) findViewById(R.id.Content);
        ViewGroup.inflate(this, R.layout.view_chat, container);
                
        scroller = (ScrollView) findViewById(R.id.scroller);
        stickyMessageHolder = (LinearLayout)findViewById(R.id.stickyMessageHolder);
        stickyMessage = (TextView)findViewById(R.id.stickyMessage);
        chatMessagesTable = (TableLayout)findViewById(R.id.chatMessagesTable);
        message = (EditText) findViewById(R.id.message);
        Button sendMessageButton = (Button) findViewById(R.id.sendMessageButton);
		message.setOnEditorActionListener(new EditText.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE ||
						actionId == EditorInfo.IME_ACTION_GO ||
						actionId == EditorInfo.IME_ACTION_NEXT ||
						actionId == EditorInfo.IME_ACTION_SEND ||
						(event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
					
					AnalyticsUtil.trackEvent(ChatActivity.this, "Chat", "Enter", "Message", 0);
					handler.sendEmptyMessage(MESSAGE_UPDATE);

					return true;
				}
				return false;
			}
		});
		sendMessageButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AnalyticsUtil.trackEvent(ChatActivity.this, "Chat", "Send", "Message", 0);
				handler.sendEmptyMessage(MESSAGE_ADD);
			}
		});        
        
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        imageLoader = new CachedImageLoader(getApplicationContext(), metrics);
        imageGetter = new ImageGetter() {
    		
    		@Override
    		public Drawable getDrawable(String source) {
    			Drawable drawable = null;
    			Bitmap bitmap = imageLoader.getImage(source);
    			if (bitmap != null)
    				drawable = new BitmapDrawable(bitmap);
    			else
    				drawable = getResources().getDrawable(R.drawable.blank);
                // Important
                drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable
                              .getIntrinsicHeight());
                return drawable;
    		}
    	};

		trClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(TAG, "tr click");
				v.showContextMenu();
			}
		};
    }
    @Override
    public CharSequence getMainTitle() {
    	Log.v(TAG, "getMainTitle");
    	return getString(R.string.msg_main_chat);
    }
    @Override
    protected String getAnalyticsPageName() {
    	return "/Chat";
    }
    @Override
    protected void onPause() {
    	Log.d(TAG, "onPause - disable update task");
    	handler.removeCallbacks(updateTask);

    	super.onPause();
    }
    @Override
    public void onResume() {
    	Log.d(TAG, "onResume");
    	super.onResume();
    	handler.sendEmptyMessage(MESSAGE_UPDATE);
    }
    @Override
    public void onLowMemory() {
    	imageLoader.clearCache();
    	
    	super.onLowMemory();
    }
    
    private JSONObject sendMessage() throws Exception {
    	Log.d(TAG, "sendMessage");
    	final String messageText = message.getText().toString();
    	if (ValidationHelper.isNullOrWhitespace(messageText))
    		return null;

    	JSONObject json = new JSONObject();
		json.put("message", messageText);
		json.put("lastMessageId", lastChatMessageId);
		json.put("includeActiveUsers", true);

    	JSONObject result = HttpConnection.postAsJson(ApiConstants.instance().baseUrl("api/chatmessages/add"), Token, json);
    	
    	// Pre-fetch and cache images in the messages, since this will be on the worker thread.  
    	// Better to do this now, than lock up the UI thread when ImageGetter handles them.
    	prefetchImagesFromMessages(result);
    	return result;
    }

    private JSONObject getLatestChatMessages() throws Exception {
    	Map<String, String> params = new HashMap<String, String>();
    	params.put(ApiConstants.PARAM_INCLUDE_ACTIVE_USERS, "true");
    	
    	if (lastChatMessageId > 0) {
        	params.put(ApiConstants.PARAM_LAST_MESSAGE_ID, lastChatMessageId.toString());
    	} else {
        	params.put(ApiConstants.PARAM_MAX_ITEMS, InitialNumberOfChatMessagesToLoad);
    	}
    	String url = ApiConstants.instance().createUrl("api/chatmessages", params);
    	Log.d(TAG, "getLatestChatMessages - " + url);
    	JSONObject result = HttpConnection.getAsJson(url, Token);
    	
    	// Pre-fetch and cache images in the messages, since this will be on the worker thread.  
    	// Better to do this now, than lock up the UI thread when ImageGetter handles them.
    	prefetchImagesFromMessages(result);
    	return result;
    }
    
	private void prefetchImagesFromMessages(JSONObject result) {
		try {
    		Log.d(TAG, "Prefetching & caching images...");
	    	JSONArray items = result.getJSONArray("items");
	    	for (int i = 0; i < items.length(); i++) {
	    		JSONObject item = (JSONObject) items.get(i);
	    		String html = item.getString("text");
	    		
	    		Document doc = Jsoup.parse(html);
	    		Elements images = doc.getElementsByTag("img");
	    		for (Element image : images) {
	    			imageLoader.getImage(image.attr("src"));
	    		}
	    	}    		
    	} catch (Exception ex) {
    		Log.e(TAG, "Error prefetching chat message images", ex);
    	}
	}
    
    private Boolean updateChat(JSONObject messages) {
    	try {
    		Log.d(TAG, "updateChat");
	    	
    		int windowHeight = scroller.getHeight();
    		int scrollY = scroller.getScrollY() + windowHeight;
    		int reasonableScroll = windowHeight / 5;
    		int height = chatMessagesTable.getHeight();
    		Boolean shouldScroll = (height == 0) || (scrollY > (height - reasonableScroll));	    	
    		Log.v(TAG, "ShouldScroll: " + shouldScroll + " - ScrollY: " + scrollY + " - Height: " + height + " - Reasonable Scroll: " + reasonableScroll);

	    	String stickyMessageText = messages.getString("sticky_message");
	    	if (ValidationHelper.isNullOrWhitespace(stickyMessageText)) {
	    		stickyMessageHolder.setVisibility(View.GONE);
	    	} else {
	    		stickyMessageHolder.setVisibility(View.VISIBLE);
	    		stickyMessage.setText(Html.fromHtml(stickyMessageText, imageGetter, null));
	    		stickyMessage.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
	    		stickyMessage.setMovementMethod(LinkMovementMethod.getInstance());
	    	}
	    	
	    	JSONArray items = messages.getJSONArray("items");
	    	for (int i = 0; i < items.length(); i++) {
	    		JSONObject item = (JSONObject) items.get(i);
	    		Long messageId = item.getLong("id");
	    		// Try to avoid duplicates
	    		if (messageId <= lastChatMessageId)
	    			continue;
	    		lastChatMessageId = messageId;
	    		
	    		String text = item.getString("text");
	    		Calendar createdOn = JsonHelper.parseDate(item.getString("created_on"));
	    		JSONObject createdBy = item.getJSONObject("created_by");
	    			    		
	    		addMessage(text, createdOn, createdBy.getString("name"), Integer.parseInt(createdBy.getString("id")));
	    	}

	    	// Remove "old" messages after update
	        int childViewCount = chatMessagesTable.getChildCount();
	        while (childViewCount > MaxChatMessages) {
	        	chatMessagesTable.removeViewAt(0);
	        	childViewCount--;
	        }

	    	JSONArray activeUsers = messages.getJSONArray("active_users");
	    	HashMap<Integer, Boolean> users = new HashMap<Integer, Boolean>();
	    	int activeUserCount = activeUsers.length();
	    	for (int i = 0; i < activeUserCount; i++) {
	    		JSONObject item = (JSONObject) activeUsers.get(i);
	    		users.put(item.getInt("id"), true);
	    	}
	    	if (activeUserCount == 1) {
	    		setTitleRight(getString(R.string.msg_secondary_user));
	    	} else {
	    		setTitleRight(String.format(getString(R.string.msg_secondary_users_formatted), activeUserCount));
	    	}
	    	
	    	Context context = getApplicationContext();
	    	for (int i = 0; i < chatMessagesTable.getChildCount(); i++) {
	        	TableRow tr = (TableRow)chatMessagesTable.getChildAt(i);
	        	
	        	Integer userId = (Integer)tr.getTag(R.id.chat_userid);
        		LinearLayout textLayout = (LinearLayout)tr.getChildAt(0);
        		TextView username = (TextView)textLayout.getChildAt(0);
	        	if (users.containsKey(userId)) {
	        		username.setTextAppearance(context, R.style.chatUsername_Online);
	        	} else {
	        		username.setTextAppearance(context, R.style.chatUsername_Offline);
	        	}
	        }
	    	users = null;

	    	Log.d(TAG, "Done");

	        if (shouldScroll) {
	        	scrollToBottom();
	        }
	        errorCount = 0;    	        	
	        
    		return true;
    	} catch (Exception ex) {
			++errorCount;
    		Log.e(TAG, "Error getting chat messages. " + ex, ex);
    		return false;
    	}
    }
	private void scrollToBottom() {
		scroller.post(new Runnable() {
			@Override
			public void run() {
				View v = getCurrentFocus();
				scroller.fullScroll(ScrollView.FOCUS_DOWN);
				v.requestFocus();
			}
		});
	}
    
	private void addMessage(String text, Calendar createdOn, String createdByName, int createdById) {
		++rowCounter;
		int layout = R.layout.partial_table_row;
		if (rowCounter % 2 != 0)
			layout = R.layout.partial_table_row_alt;
		if (createdById < 0)
			layout = R.layout.partial_table_row_sys;
		
    	TableRow tr = (TableRow) getLayoutInflater().inflate(layout, null);
    	
    	if (createdById > 0) {
	    	tr.setTag(R.id.chat_userid, createdById);
	    	tr.setTag(R.id.chat_username, createdByName);
	    	
	    	registerForContextMenu(tr);
	    	tr.setOnClickListener(trClickListener);
    	}
		
		LayoutParams messageLayout = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		messageLayout.gravity = Gravity.CENTER_VERTICAL;

		LinearLayout messageHolder = new LinearLayout(this);
		messageHolder.setLayoutParams(messageLayout);
		messageHolder.setOrientation(LinearLayout.HORIZONTAL);
		
		if (createdById > 0) {
			TextView usernameText = new TextView(this);
			usernameText.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			usernameText.setText("[" + createdByName + "]");
			usernameText.setTextAppearance(getApplicationContext(), R.style.chatUsername_Offline);
			usernameText.setPadding(5, 2, 0, 2);
			messageHolder.addView(usernameText);
		}
		TextView messageText = new TextView(this);
		messageText.setText(Html.fromHtml(text, imageGetter, null));
		messageText.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		messageText.setTextAppearance(getApplicationContext(), R.style.chatText);
		messageText.setMovementMethod(LinkMovementMethod.getInstance());
		messageText.setPadding(5, 2, 10, 2);
		messageHolder.addView(messageText);
		tr.addView(messageHolder);
		
    	DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(getApplicationContext());
    	Date d = new Date(createdOn.getTimeInMillis());
    	
    	TextView messageDate = new TextView(this);
		messageDate.setText(timeFormat.format(d));
		LayoutParams messageDateLayout = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		messageDateLayout.setMargins(0, 2, 10, 2);
		messageDateLayout.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
		messageDate.setLayoutParams(messageDateLayout);
		messageDate.setTextAppearance(getApplicationContext(), R.style.chatTime);
		messageDate.setBackgroundResource(R.drawable.chat_time_bg);
		messageDate.setVisibility(View.INVISIBLE);
		if (rowCounter % 5 == 0)
			messageDate.setVisibility(View.VISIBLE);
		tr.addView(messageDate);
		
		chatMessagesTable.addView(tr);
	}

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	final String username = (String) v.getTag(R.id.chat_username);
    	
    	if (username != null) {
	    	
	//		menu.setHeaderTitle("Actions");
	    	MenuItem replyButton = menu.add(Menu.NONE, R.id.chat_menu_mention, 0, "Reply");
	    	replyButton.setOnMenuItemClickListener(new OnMenuItemClickListener() {
	    		@Override
	    		public boolean onMenuItemClick(MenuItem arg0) {
					AnalyticsUtil.trackEvent(ChatActivity.this, "Chat", "Click", "Reply", 0);
	    			String mentionName = username.split(" ")[0];
	    			
	    			message.setText((message.getText() + " @" + mentionName).trim() + " ");
	    			message.requestFocus();
	    			return true;
	    		}
	    	});
	/*		MenuItem ignoreButton = menu.add(Menu.NONE, R.id.chatContextMenu_Ignore, 0, "Ignore");
			ignoreButton.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem arg0) {
		        	// TODO: Ignore the user
		        	return true;
				}
			});*/
    	} else {
    		super.onCreateContextMenu(menu, v, menuInfo);
    	}
	}
	
	@Override
    public void onDestroy()
    {
		imageLoader.clearCache();
        super.onDestroy();
    }
}