package com.vsc.google.api.services.samples.calendar.android.bilik;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tresmonos.calendar.model.ResourceState;

import org.joda.time.DateTime;


/**
 * Fragment that provides a map-view of the available rooms
 */
public class MapFragment extends BaseFragment {
	public static class Factory implements GeneralActivity.FragmentFactory {
		@Override
		public BaseFragment makeFragment() {
			return new MapFragment();
		}

		@Override
		public String title() {
			return "MAP";
		}

		@Override
		public int iconString() {
			return R.string.icon_map;
		}
	}

	public MapFragment() {
		// Required empty public constructor
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.fragment_map, container, false);
	}

	@Override
	protected void onResourceStateChange(ResourceState resourceState) {
		// TODO: Implement refresh
	}

	@Override
	protected void onClockTick(DateTime currentDatetime) {
		// TODO: Update clock, if needed
	}
}
