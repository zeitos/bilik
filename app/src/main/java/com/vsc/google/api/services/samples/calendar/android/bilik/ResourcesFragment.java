package com.vsc.google.api.services.samples.calendar.android.bilik;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.TabHost;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.nhaarman.listviewanimations.appearance.simple.SwingRightInAnimationAdapter;
import com.tresmonos.calendar.CalendarService;
import com.tresmonos.calendar.Configuration;
import com.tresmonos.calendar.model.RemoteResource;
import com.tresmonos.calendar.model.ResourceState;
import com.tresmonos.ui.CustomizableResources;
import com.tresmonos.ui.views.DataUnitViewUtils;
import com.tresmonos.ui.views.ResourceCardFlipView;

import org.jcaki.Strings;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.Nullable;


/**
 * Fragment that displays a list of available rooms
 */
public class ResourcesFragment extends BaseFragment implements TabHost.OnTabChangeListener {
	private static final Map<ResourceState.Status, Integer> ROOM_CARD_COLOR_BY_STATE = ImmutableMap.of(ResourceState.Status.busy, R.color.busy_resource_map_color, ResourceState.Status.empty, R.color.available_room_pressed_button, ResourceState.Status.unplugged, R.color.unplugged_resource_map_color, ResourceState.Status.waitingForConfirmation, R.color.waiting_resource_map_color);
	private static final Predicate<RemoteResource> MANAGED_BY_BILIK_PARTIAL_RESOURCE_PREDICATE = new Predicate<RemoteResource>() {
		@Override
		public boolean apply(RemoteResource resource) {
			return resource.isManagedByBilik();
		}
	};
	private static final long SYNC_PARTIAL_RESOURCES_PERIOD = Duration.standardSeconds(1).getMillis();
	private static final long SYNC_PARTIAL_RESOURCES_DELAY = 0;
	private RoomCardAdapter mRoomCardAdapter;
	private TabHost mTabHost;
	private GridView mGridView;
	private Timer syncPartialResources;

	public static class Factory implements GeneralActivity.FragmentFactory {
		@Override
		public BaseFragment makeFragment() {
			return new ResourcesFragment();
		}

		@Override
		public String title() {
			return "OTHER ROOMS";
		}

		@Override
		public int iconString() {
			return R.string.icon_list;
		}
	}

	public ResourcesFragment() {
		// Required empty public constructor
	}

	@Override
	public void onResume() {
		super.onResume();

		syncPartialResources = new Timer();
		syncPartialResources.schedule(new TimerTask() {
			@Override
			public void run() {
				updateCurrentGridView();
			}
		}, SYNC_PARTIAL_RESOURCES_DELAY, SYNC_PARTIAL_RESOURCES_PERIOD);

	}

	private void updateCurrentGridView() {
		String currentArea = mTabHost != null && !Strings.isNullOrEmpty(mTabHost.getCurrentTabTag()) ? mTabHost.getCurrentTabTag() : Configuration.getInstance(getActivity()).getCurrentArea();
		updateCurrentGridView(currentArea);
	}

	private void updateCurrentGridView(String area) {
		String currentResource = getConfiguration().getResource();
		CalendarService service = getBaseActivity().getCalendarService();
		if (service == null)
							return;
		final Iterable<RemoteResource> partialResources = FluentIterable.from(service.getAllResources(area))
				.filter(MANAGED_BY_BILIK_PARTIAL_RESOURCE_PREDICATE)
				.filter(getAllResourcesButOnePredicate(currentResource)).toList();
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (mRoomCardAdapter != null) {
					mRoomCardAdapter.clear();
					mRoomCardAdapter.addAll(Lists.newArrayList(partialResources));
				}
			}
		});
	}

	private Predicate<RemoteResource> getAllResourcesButOnePredicate(final String currentResource) {
		return new Predicate<RemoteResource>() {
			@Override
			public boolean apply(@Nullable RemoteResource resource) {
				return !currentResource.equals(resource.getFullName());
			}
		};
	}

	@Override
	public void onPause() {
		syncPartialResources.cancel();
		super.onPause();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		View view = inflater.inflate(R.layout.fragment_resources, container, false);

		mTabHost = (TabHost) view.findViewById(R.id.tabhost);
		mTabHost.setup();
		mTabHost.setOnTabChangedListener(this);
		mGridView = (GridView) view.findViewById(R.id.activity_googlecards_listview);
		// Initialize the ViewPager and set an adapter

		List<String> areas = getAreas();

		for (String area : areas) {
			mTabHost.addTab(mTabHost.newTabSpec(area).setIndicator(area).setContent(new TabHost.TabContentFactory() {
				public View createTabContent(String arg0) {
					return mGridView;
				}
			}));
		}

		mRoomCardAdapter = new RoomCardAdapter(getActivity(), this);
		SwingRightInAnimationAdapter swingBottomInAnimationAdapter = new SwingRightInAnimationAdapter(mRoomCardAdapter);
		swingBottomInAnimationAdapter.setAbsListView(mGridView);

		mGridView.setAdapter(swingBottomInAnimationAdapter);


		return view;
	}

	private List<String> getAreas() {
		Configuration config = getConfiguration();
		String account = config.getAccount();
		final String currentArea = config.getCurrentArea();

		List<String> areas = Lists.newArrayList(config.getAreas(account));

		Collections.sort(areas, new Comparator<String>() {
			@Override
			public int compare(String lhs, String rhs) {
				return lhs.equals(currentArea) || rhs.equals(currentArea) ? 1 : lhs.compareTo(rhs);
			}

			@Override
			public boolean equals(Object object) {
				return false;
			}
		});
		return areas;
	}

	/**
	 * Implement logic here when a tab is selected
	 */
	public void onTabChanged(String tabName) {
		updateCurrentGridView(tabName);

		mGridView.smoothScrollToPosition(0);
	}

	@Override
	protected void onResourceStateChange(ResourceState resourceState) {
		// TODO: Implement refresh
	}

	@Override
	protected void onClockTick(DateTime currentDatetime) {
		// TODO: Update clock, if needed
	}

	private static class RoomCardAdapter extends ArrayAdapter<RemoteResource> {

		private final Context context;
		private final BaseFragment baseFragment;

		public RoomCardAdapter(final Context context, final BaseFragment baseFragment) {
			super(context, 0);
			this.context = context;
			this.baseFragment = baseFragment;

		}

		@Override
		public View getView(final int position, final View convertView, final ViewGroup parent) {
			ResourceCardFlipView.ViewHolder viewHolder;
			ResourceCardFlipView view = (ResourceCardFlipView) convertView;
			if (view == null) {
				LayoutInflater inflater = LayoutInflater.from(context);
				view = (ResourceCardFlipView) inflater.inflate(R.layout.resource_card_view, parent, false);
				viewHolder = view.createViewHolder();
				view.setTag(viewHolder);

			} else {
				viewHolder = (ResourceCardFlipView.ViewHolder) view.getTag();
			}
			//TODO: resourceName should be retrieved from {@link Resource#getShortName()}
			Configuration configuration = baseFragment.getConfiguration();
			String resourceName = configuration.getAlternativeResourceName();
			final String newReservationTitle = String.format(context.getResources().getString(R.string.adhoc_meeting_from_another_room), resourceName);
			final RemoteResource resource = getItem(position);

			final ResourceCardFlipView resourceCardFlipView = view;
			String shortName = resource.getShortName();

			if (!viewHolder.frontTitleView.getText().equals(shortName)) {
				view.ensureFront();
			}

			viewHolder.frontTitleView.setText(shortName);
			viewHolder.backTitleView.setText(shortName);
			boolean resourceInProgress = baseFragment.getBaseActivity().getCalendarService().isRemoteResourceInProgress(resource.getFullName());
			int loadingVisibility = resourceInProgress ? View.VISIBLE : View.GONE;
			int remainingTimeVisibility = resourceInProgress ? View.GONE : View.VISIBLE;
			viewHolder.resourceInProgresView.setVisibility(loadingVisibility);
			viewHolder.remainingHoursView.setVisibility(remainingTimeVisibility);
			viewHolder.remainingMinutesView.setVisibility(remainingTimeVisibility);

			viewHolder.flipCardIconView.setVisibility(resource.getStatus() == ResourceState.Status.empty ? View.VISIBLE : View.INVISIBLE);
			viewHolder.buttonView.setText(context.getResources().getString(R.string.book_now));
			viewHolder.buttonView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					try {
						DateTime now = DateTime.now();
						DateTime nowPlusDefaultExtension = now.plus(CustomizableResources.DEFAULT_EXTENSION_TIME);
						DateTime endDate = resource.getNextEventDate() == null || nowPlusDefaultExtension.isBefore(resource.getNextEventDate()) ? nowPlusDefaultExtension : resource.getNextEventDate();
						baseFragment.getBaseActivity().getCalendarService().addRemoteReservation(resource,
								newReservationTitle,
								newReservationTitle,
								now,
								endDate);
					} finally {
						resourceCardFlipView.flipOut();
					}
				}
			});
			resourceCardFlipView.setFlippingEnabled(resource.getStatus() == ResourceState.Status.empty && !resourceInProgress);
			updateStatus(viewHolder, resource);

			return view;
		}

		private void updateStatus(final ResourceCardFlipView.ViewHolder viewHolder, RemoteResource resource) {
			DateTime now = DateTime.now();
			ResourceState.Status status = now.isAfter(resource.getNextEventDate()) ? ResourceState.Status.unplugged : resource.getStatus();
			Period period = status == ResourceState.Status.unplugged ? null : new Period(now, resource.getNextEventDate());
			viewHolder.statusLayoutView.setBackgroundColor(context.getResources().getColor(ROOM_CARD_COLOR_BY_STATE.get(status)));

			DataUnitViewUtils.updateRemainingTime(viewHolder.remainingHoursView, viewHolder.remainingMinutesView, period, context.getResources().getString(R.string.unplugged_device));
		}

	}

}

