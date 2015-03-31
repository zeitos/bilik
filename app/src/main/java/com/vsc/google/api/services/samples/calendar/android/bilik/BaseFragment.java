package com.vsc.google.api.services.samples.calendar.android.bilik;

import android.support.v4.app.Fragment;

import com.tresmonos.calendar.Configuration;
import com.tresmonos.calendar.model.ResourceState;

import org.joda.time.DateTime;

import de.greenrobot.event.EventBus;

/**
 * Base for all fragments that will be presented on the {@link com.vsc.google.api.services.samples.calendar.android.bilik.GeneralActivity}
 */
public abstract class BaseFragment extends Fragment {
	private static final EventBus eventBus = EventBus.getDefault();

	public void registerAnalyticsEvent(BaseActivity.Category category, BaseActivity.Action action, BaseActivity.Label label, Long value) {
		getBaseActivity().registerAnalyticsEvent(getBaseActivity(), category, action, label, value);
	}

	protected BaseActivity getBaseActivity() {
		return (BaseActivity)getActivity();
	}

	protected Configuration getConfiguration() {
		return getBaseActivity().getConfiguration();
	}

	abstract protected void onResourceStateChange(ResourceState resourceState);

	abstract protected void onClockTick(DateTime currentDatetime);

	/**
	 * Selects the first fragment as the one to be displayed
	 */
	protected void moveToMainFragment() {
		eventBus.post(new GeneralActivity.MoveToFragmentEvent(0));
	}
}
