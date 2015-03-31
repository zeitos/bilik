package com.vsc.google.api.services.samples.calendar.android.bilik;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Scroller;
import android.widget.TextView;

import com.astuetz.PagerSlidingTabStrip;
import com.google.common.collect.ImmutableList;
import com.parse.ParseAnalytics;
import com.tresmonos.calendar.model.ResourceState;
import com.tresmonos.ui.StatusResources;

import org.joda.time.DateTime;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Main activity, in charge of holding all sliding fragments
 */
public class GeneralActivity extends BaseActivity {
	private SectionsPagerAdapter mSectionsPagerAdapter;
	private ImageView backgroundImageView;
	private ViewPager mViewPager;
	private List<FragmentFactory> fragmentFactories = ImmutableList.of(
		new MainFragment.Factory(),
		new ResourcesFragment.Factory(),
		new FeedbackFragment.Factory()
	);
	private TextView dismissLabel;
	private LinearLayout dismissLabelArea;
	private CountDownTimer dismissTimer;

	/** Request code to open the Setup screen */
	public static final int REQUEST_CODE_SETUP = 0;
	/** Expected results from {@link #startActivityForResult} */
	public static final int RESULT_CANCELLED = 0; // Last operation was cancelled
	public static final int RESULT_CONFIG_CHANGE = 1; // Last operation caused a configuration change

	private static final int AUTOMATIC_DISMISS_TIME_SECS = 20;
	private static final int AUTOMATIC_DISMISS_TIME_WARNING_SECS = 10;

	/**
	 * Factory that all fragments should implement to be added to the list of
	 * pages of this activity.
	 */
	public interface FragmentFactory {
		BaseFragment makeFragment();
		String title();
		int iconString();
	}

	/**
	 * Event used to notify that we want to navigate to certain fragment
	 */
	public static class MoveToFragmentEvent {
		public final int fragmentPosition;

		public MoveToFragmentEvent(int fragmentPosition) {
			this.fragmentPosition = fragmentPosition;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ParseAnalytics.trackAppOpened(getIntent());

		setContentView(R.layout.activity_general);

		// Obtain references to the elements of the screen
		backgroundImageView = (ImageView) findViewById(R.id.background_image);

		// Lets initiate the dismiss timer if needed
		dismissLabel = (TextView) findViewById(R.id.automatic_dismiss);
		dismissLabelArea = (LinearLayout) findViewById(R.id.automatic_dismiss_layout);
		dismissLabelArea.setVisibility(View.GONE);

		// Create the adapter that will return a fragment for each of the three primary sections
		// of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		mViewPager.setOffscreenPageLimit(mSectionsPagerAdapter.getCount() - 1);
		settingSlideSpeed(300, mViewPager);

		// Bind the tabs to the ViewPager
		PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
		tabs.setViewPager(mViewPager);

		Typeface font = Typeface.createFromAsset( getAssets(), "fontawesome-webfont.ttf" );
		tabs.setTypeface(font, 1);

		tabs.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageScrollStateChanged(int state) {
				GeneralActivity.this.onPageScrollStateChanged(state);
			}
		});

		// Detect if we need to ask for configuration
		String configurationError = configuration.checkConfiguration();
		if (configurationError != null) {
			registerAnalyticsEvent(Category.SYSTEM_ACTION, Action.EVENT, Label.MAIN_ACTIVITY_INITIAL_CONFIGURATION_EVENT, null);
			Intent setupIntent = new Intent(GeneralActivity.this, SetupActivity.class);
			setupIntent.putExtra(SetupActivity.ERROR_MESSAGE_INTENT_KEY, configurationError);
			startActivityForResult(setupIntent, 0);
			return;
		}

		// Set the clock timer
		Timer refreshTimer = new Timer();
		refreshTimer.schedule(new TimerTask() {
			public void run() {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						updateClock(new DateTime());
					}
				});
			}
		}, TimeUnit.SECONDS.toMillis(1), TimeUnit.SECONDS.toMillis(5));

		// Make a first refresh so everything looks good initially
		updateUIOnConfigurationChange();
	}

	/**
	 * Updates the UI every time the configuration changes
	 */
	private void updateUIOnConfigurationChange() {
		backgroundImageView.setImageBitmap(configuration.getBackgroundImage());
		updateClock(new DateTime());
	}

	/**
	 * Adapter used to present the different fragments of the application in
	 * the {@link android.support.v4.view.ViewPager} view.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {
		private SparseArray<BaseFragment> registeredFragments = new SparseArray<BaseFragment>();

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override public Fragment getItem(int position) {
			return fragmentFactories.get(position).makeFragment();
		}

		@Override public int getCount() {
			return fragmentFactories.size();
		}

		@Override public CharSequence getPageTitle(int position) {
			return getResources().getString(fragmentFactories.get(position).iconString()) + " " + fragmentFactories.get(position).title();
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			BaseFragment fragment = (BaseFragment) super.instantiateItem(container, position);
			registeredFragments.put(position, fragment);
			return fragment;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			registeredFragments.remove(position);
			super.destroyItem(container, position, object);
		}

		public BaseFragment getRegisteredFragment(int position) {
			return registeredFragments.get(position);
		}
	}

	/**
	 * Every time we receive an update on the resource status, we update the UI
	 * @param state new resource status to display
	 */
	public void onEventMainThread(ResourceState state) {
        Log.d("CalendarService", " (!!!REFRESH VIEW!!!)---> posing state: " + state.getStatus().name() + " - with " + state.getResource().getReservations().size() + " reservations - for resourceState: " + state);
		refreshView(state);
	}

	/**
	 * Every time any action requires to move to a certain fragment this event is fired
	 * @param moveToFragmentEvent fragmento to move to
	 */
	public void onEventMainThread(MoveToFragmentEvent moveToFragmentEvent) {
		mViewPager.setCurrentItem(moveToFragmentEvent.fragmentPosition);
	}

	private void refreshView(ResourceState resourceState) {
		StatusResources statusResources = StatusResources.getStatusResources(resourceState.getStatus());
		findViewById(R.id.body_layout).setBackgroundColor(getResources().getColor(statusResources.getBodyBarColor()));
		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			BaseFragment fragment = mSectionsPagerAdapter.getRegisteredFragment(i);
			if (fragment != null) {
				Log.i(this.getClass().getName(), "Updating fragment '" + ((Object)fragment).getClass().getName() + "' due resource state change: " + resourceState);
				fragment.onResourceStateChange(resourceState);
			}
		}
	}

	private void updateClock(DateTime dateTime) {
		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			BaseFragment fragment = mSectionsPagerAdapter.getRegisteredFragment(i);
			if (fragment != null) {
				Log.i(this.getClass().getName(), "Updating fragment '" + ((Object)fragment).getClass().getName() + "' due clock update: " + dateTime);
				fragment.onClockTick(dateTime);
			}
		}
	}

	private void settingSlideSpeed(final int mDuration, ViewPager mPager) {
		try {
			Field mScroller;
			mScroller = ViewPager.class.getDeclaredField("mScroller");
			mScroller.setAccessible(true);
			Scroller scroller = new Scroller(mPager.getContext(), new DecelerateInterpolator()) {
				@Override
				public void startScroll(int startX, int startY, int dx, int dy, int duration) {
					// Ignore received duration, use fixed one instead
					super.startScroll(startX, startY, dx, dy, mDuration);
				}

				@Override
				public void startScroll(int startX, int startY, int dx, int dy) {
					// Ignore received duration, use fixed one instead
					super.startScroll(startX, startY, dx, dy, mDuration);
				}
			};
			mScroller.set(mPager, scroller);
		} catch (NoSuchFieldException e) {
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) {
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		if (supportsFullLayout() && hasFocus) {
			getWindow().getDecorView().setSystemUiVisibility(
					View.SYSTEM_UI_FLAG_LAYOUT_STABLE
							| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
							| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
							| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
							| View.SYSTEM_UI_FLAG_FULLSCREEN
							| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
			);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Upon receiving new configuration from the setup page, we update the UI
		if (resultCode == RESULT_CONFIG_CHANGE) {
			updateUIOnConfigurationChange();
			mViewPager.setCurrentItem(0);
		}
	}

	@Override
	public void onBackPressed() {
		mViewPager.setCurrentItem(0);
	}

	private void onPageScrollStateChanged(int state) {
		if (state == ViewPager.SCROLL_STATE_IDLE && mViewPager.getCurrentItem() != 0 && dismissTimer == null) {
			dismissLabel.setText("");
			dismissTimer = new CountDownTimer(AUTOMATIC_DISMISS_TIME_SECS * 1000, 1000) {
				@Override
				public void onTick(long millisUntilFinished) {
					if (millisUntilFinished < AUTOMATIC_DISMISS_TIME_WARNING_SECS * 1000) {
						dismissLabelArea.setVisibility(View.VISIBLE);
						dismissLabel.setText(String.format("Dismissing in %s seconds", millisUntilFinished / 1000));
					}
				}

				@Override
				public void onFinish() {
					registerAnalyticsEvent(BaseActivity.Category.SYSTEM_ACTION, BaseActivity.Action.TIMEOUT, BaseActivity.Label.REPORT_ACTIVITY_AUTO_DISMISS_TIMER, null);
					mViewPager.setCurrentItem(0);
					dismissTimer = null;
				}
			};
			dismissTimer.start();
		} else {
			dismissLabelArea.setVisibility(View.GONE);
			if (dismissTimer != null)
				dismissTimer.cancel();
			dismissTimer = null;
		}
	}
}
