package com.focusings.focusingsworld;


import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Properties;


import com.focusings.focusingsworld.ImageAndTextList.ImageAndText;
import com.focusings.focusingsworld.ImageAndTextList.ImageAndTextListAdapter;
import com.focusings.focusingsworld.TwitterParser.AsyncTwitterParser;
import com.focusings.focusingsworld.TwitterParser.AsyncTwitterResponse;
import com.focusings.focusingsworld.TwitterParser.TweetInfo;
import com.focusings.focusingsworld.TwitterParser.TweetsListAdapter;
import com.focusings.focusingsworld.YoutubeParser.AsyncYoutubeResponse;
import com.focusings.focusingsworld.YoutubeParser.AsyncYoutubeParser;
import com.focusings.focusingsworld.notificationManagement.AsyncNotificationResponse;
import com.focusings.focusingsworld.notificationManagement.CheckNewUpdatesService;
import com.focusings.focusingsworld.notificationManagement.CheckNewUpdatesServiceReceiver;
import com.focusings.focusingsworld.notificationManagement.Update;
import com.focusings.focusingsworld.pullToRefreshLibrary.PullToRefreshListView;
import com.focusings.focusingsworld.pullToRefreshLibrary.PullToRefreshTwitterOnRefreshListener;
import com.focusings.focusingsworld.pullToRefreshLibrary.PullToRefreshYoutubeOnRefreshListener;
import com.focusings.focusingsworld.R;

import android.app.ActionBar;
import android.app.AlarmManager;
import android.app.FragmentTransaction;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

public class MainActivity extends FragmentActivity implements
		ActionBar.TabListener, AsyncYoutubeResponse, 
		AsyncNotificationResponse,AsyncTwitterResponse	{
	
	public static Properties properties;
	public static VideoInfo[] lastUpdatePerChannel;
	//Properties with app info
	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
		AsyncYoutubeParser.delegate = this;
		CheckNewUpdatesService.delegate=this;
		AsyncTwitterParser.delegate=this;
		properties = new Properties();
		getProperties();
		setContentView(R.layout.activity_main);

		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the app.
		MainActivity.lastUpdatePerChannel= new VideoInfo[Integer.parseInt(properties.getProperty("number_of_tabs"))];
		
		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		// When swiping between different sections, select the corresponding
		// tab. We can also use ActionBar.Tab#select() to do this if we have
		// a reference to the Tab.
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						actionBar.setSelectedNavigationItem(position);
					}
				});

		actionBar.removeAllTabs();
		
		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < Integer.parseInt(properties.getProperty("number_of_tabs")); i++) {
			// Create a tab with text corresponding to the page title defined by
			// the adapter. Also specify this Activity object, which implements
			// the TabListener interface, as the callback (listener) for when
			// this tab is selected.			
			actionBar.addTab(actionBar.newTab()
						.setText(mSectionsPagerAdapter.getPageTitle(i))
						.setTabListener(this));			
		}
		
		//If Twitter account is enabled, then I add a Twitter tab
		if (properties.getProperty("enable_twitter").equals("yes")){
			actionBar.addTab(actionBar.newTab()
					.setText("Twitter")
					.setTabListener(this));
		}
		
		//If the tab to be selected is passed by parameter, I go to that tab
		Bundle b = getIntent().getExtras();
		int tabToBeSelected=0;
		if (b!=null){
			tabToBeSelected = b.getInt("tabNumber");
			mViewPager.setCurrentItem(tabToBeSelected);
		}
		
		//Setting alarmManager to check if new updates are availables and in such case, show a notification
		setRecurringAlarm(this);
	}

	/** Getting all app properties */
	private void getProperties(){
		try {
            /**
             * getAssets() Return an AssetManager instance for your
             * application's package. AssetManager Provides access to an
             * application's raw asset files;
             */
            AssetManager assetManager = getAssets();
            /**
             * Open an asset using ACCESS_STREAMING mode. This
             */
            InputStream inputStream = assetManager.open("app.properties");
            /**
             * Loads properties from the specified InputStream,
             */
            properties.load(inputStream);

	     } catch (IOException e) {
	            
	     }
	}
	
	//Setting a recurring alarm, so that every 15 minutes it checks if there are new updates
	private void setRecurringAlarm(Context context) {
		Intent downloader = new Intent(context, CheckNewUpdatesServiceReceiver.class);
		downloader.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, downloader, PendingIntent.FLAG_CANCEL_CURRENT);
		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		Calendar updateTime = Calendar.getInstance();
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, updateTime.getTimeInMillis(),AlarmManager.INTERVAL_FIFTEEN_MINUTES, pendingIntent);
    }

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override	 
	public boolean onOptionsItemSelected(MenuItem item){

		//Case of about
		if (item.getTitle().equals(getString(R.string.action_about))){
			
			//Preparing Popup window
			LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
	        View layout = layoutInflater.inflate(R.layout.about, null);
	        
	        final PopupWindow popup = new PopupWindow(this);
	        popup.setContentView(layout);
	        popup.setWidth(LinearLayout.LayoutParams.WRAP_CONTENT);
	        popup.setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);
	        popup.setFocusable(true);
	        
	        //Defining close button in popup window
	        Button close = (Button) layout.findViewById(R.id.closeButton);
	        close.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					popup.dismiss();
				}
			});
	        
	        //Defining close button in popup window
	        Button rateApp = (Button) layout.findViewById(R.id.rateAppButton);
	        rateApp.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + "com.focusings.focusingsworld")));
					popup.dismiss();
				}
			});
	        
	        // Setting the popup in the center of the page
	        popup.showAtLocation(layout, Gravity.CENTER, 0, 0);

		}
			
	    return super.onOptionsItemSelected(item);

    }
		
	@Override
	public void onTabSelected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			// Return a DummySectionFragment (defined as a static inner class
			// below) with the page number as its lone argument.
			Fragment fragment = new DummySectionFragment();
			Bundle args = new Bundle();
			args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, position + 1);
			fragment.setArguments(args);
			return fragment;
		}

		@Override
		public int getCount() {
			// Returns the number of tabs
			
			//If twitter is enabled then I add a tab
			if (properties.getProperty("enable_twitter").equals("yes")){
				return Integer.parseInt(properties.getProperty("number_of_tabs"))+1;
			}
			return Integer.parseInt(properties.getProperty("number_of_tabs"));
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			return properties.getProperty("tab_"+(position+1)+"_channel_public_name").toUpperCase(l);			
		}
	}

	/**
	 * A dummy fragment representing a section of the app, but that simply
	 * displays dummy text.
	 */
	public static class DummySectionFragment extends Fragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		
		public static final String ARG_SECTION_NUMBER = "section_number";

		public DummySectionFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			
			//Get arguments from SectionsPagerAdapter.getItem
			Bundle args=this.getArguments();
			int currentTab=args.getInt(DummySectionFragment.ARG_SECTION_NUMBER);
					
			View rootView=null;
			
			//Cas de la primera tab
			if (currentTab==1){
				rootView = inflater.inflate(R.layout.tab1,container, false);
				
				//I get all data calling services from Youtube
				new AsyncYoutubeParser().execute(properties.getProperty("Youtube_URL_part_1")+properties.getProperty("tab_1_channel_name")+properties.getProperty("Youtube_URL_part_2"),"false");
			}
			//Cas de la segona tab
			if (currentTab==2){				
				rootView = inflater.inflate(R.layout.tab2, container, false);
				//I get all data calling services from Youtube
				new AsyncYoutubeParser().execute(properties.getProperty("Youtube_URL_part_1")+properties.getProperty("tab_2_channel_name")+properties.getProperty("Youtube_URL_part_2"),"false");
			}
			
			//Case Twitter tab
			if (currentTab==Integer.parseInt(properties.getProperty("number_of_tabs"))+1){
				rootView = inflater.inflate(R.layout.twitter_tab, container, false);
				//I get all data calling services from Twitter
				new AsyncTwitterParser().execute("false");
			}
			
			return rootView;
		}
				
	}
	
	public boolean isOnline() {
	    ConnectivityManager cm = 
	    		(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo netInfo = cm.getActiveNetworkInfo();
	    if (netInfo != null && netInfo.isConnectedOrConnecting()) {
	        return true;
	    }
	    return false;
	}

	public void printYoutubeElements(List<ImageAndText> list,int tabNumber,boolean needToCallOnRefreshComplete){
		//this you will received result fired from async class of onPostExecute(result) method.
		ImageAndTextListAdapter adapter = new ImageAndTextListAdapter(this,list);
		
		TextView errorLog=null;
		PullToRefreshListView listView=null;
		if (tabNumber==1){
			errorLog=(TextView)findViewById(R.id.errorLog1);
			listView = (PullToRefreshListView)findViewById(R.id.pull_to_refresh_listview1);
		}
		if (tabNumber==2){
			errorLog=(TextView)findViewById(R.id.errorLog2);
			listView = (PullToRefreshListView)findViewById(R.id.pull_to_refresh_listview2);
		}
		if (tabNumber==3){
			errorLog=(TextView)findViewById(R.id.errorLogTwitter);
			listView = (PullToRefreshListView)findViewById(R.id.pull_to_refresh_listview_twitter);
		}
		
		if (list.size()==0){
			if (isOnline()==false){				
				errorLog.setText(R.string.noInternetConnection);
			}else{
				errorLog.setText(R.string.noYoutubeConnection);
			}
		}else{
			errorLog.setText("");
		}
		
		//According to the pullToRefresh library I'm using, I need to call listView.onRefreshComplete() 
		//so that the spinner stops, but I only need to do this if I come from onRefreshListener (because it
		//means the spinner was already been displayed), if I come from initializations the spinner isn't displayed,
		//so, I shouldn't call listView.onRefreshComplete()
		if (needToCallOnRefreshComplete && listView!=null){
			listView.onRefreshComplete();
		}
		
		//Case of listView
		if (listView!=null){
			listView.setOnRefreshListener(new PullToRefreshYoutubeOnRefreshListener(tabNumber));		
			listView.setAdapter(adapter);
		}/*else{
			GridView gridView=null;
			if (tabNumber==1){
				gridView = (GridView)findViewById(R.id.gridview1);
				gridView.setAdapter(adapter);
			}
			if (tabNumber==2){
				gridView = (GridView)findViewById(R.id.gridview2);
				gridView.setAdapter(adapter);
			}
		}*/
	}
	
	public void printTwitterElements(List<TweetInfo> list,boolean needToCallOnRefreshComplete){
		//this you will received result fired from async class of onPostExecute(result) method.
		TweetsListAdapter adapter = new TweetsListAdapter(this,list);
		
		TextView loadingTwitterView=(TextView)findViewById(R.id.loadingTwitter);
		loadingTwitterView.setText("");
		
		TextView errorLog=null;
		PullToRefreshListView listView=null;
		errorLog=(TextView)findViewById(R.id.errorLogTwitter);
		listView = (PullToRefreshListView)findViewById(R.id.pull_to_refresh_listview_twitter);
		
		if (list.size()==0){
			if (isOnline()==false){				
				errorLog.setText(R.string.noInternetConnection);
			}else{
				errorLog.setText(R.string.noTwitterConnection);
			}
		}else{
			errorLog.setText("");
		}
		
		//According to the pullToRefresh library I'm using, I need to call listView.onRefreshComplete() 
		//so that the spinner stops, but I only need to do this if I come from onRefreshListener (because it
		//means the spinner was already been displayed), if I come from initializations the spinner isn't displayed,
		//so, I shouldn't call listView.onRefreshComplete()
		if (needToCallOnRefreshComplete && listView!=null){
			listView.onRefreshComplete();
		}
		
		//Case of listView
		if (listView!=null){
			listView.setOnRefreshListener(new PullToRefreshTwitterOnRefreshListener());		
			listView.setAdapter(adapter);
		}
	}
	
	public void sendNotification(List<Update> updates){
		for (int i=0;i<updates.size();i++){
			Update currentUpdate=updates.get(i);
			
			// Prepare intent which is triggered if the notification is selected
			Intent intent = new Intent(this, MainActivity.class);
			Bundle b = new Bundle();
			b.putInt("tabNumber", currentUpdate.getTabNumber()); 
			intent.putExtras(b);			
			PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

			// Build notification
			// Actions are just fake
			Notification noti = new Notification.Builder(this)
			        .setContentTitle(getString(R.string.newVideo)+" "+currentUpdate.getChannel())
			        .setContentText(currentUpdate.getTitle())
			        .setSmallIcon(R.drawable.ic_launcher)
			        .setLights(0xff00ff00, 300, 2000)
			        .setContentIntent(pIntent).getNotification();
			    
			NotificationManager notificationManager = 
			  (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

			// Hide the notification after its selected
			noti.flags |= Notification.FLAG_AUTO_CANCEL;
			noti.flags |= Notification.FLAG_SHOW_LIGHTS;

			notificationManager.notify(i, noti); 
			
		}
	}
}
