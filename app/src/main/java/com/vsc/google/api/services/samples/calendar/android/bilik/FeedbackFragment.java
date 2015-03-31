package com.vsc.google.api.services.samples.calendar.android.bilik;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.tresmonos.calendar.model.ResourceState;
import com.tresmonos.utils.GMailOauthSender;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;


/**
 * Fragment that allows reporting problems and provide feedback.
 * This is also the one that give access to the setup page.
 */
public class FeedbackFragment extends BaseFragment {
	private ImageView backgroundImage;
	private Button setupButton;
	private GridView feedbackGrid;

	public static class Factory implements GeneralActivity.FragmentFactory {
		@Override
		public BaseFragment makeFragment() {
			return new FeedbackFragment();
		}

		@Override
		public String title() {
			return "FEEDBACK";
		}

		@Override
		public int iconString() {
			return R.string.icon_feedback;
		}
	}

	public FeedbackFragment() {
		// Required empty public constructor
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.fragment_feedback, container, false);
	}

	@Override
	public void onResume() {
		super.onResume();
		backgroundImage = (ImageView) getActivity().findViewById(R.id.background_image);
		backgroundImage.setImageBitmap(getConfiguration().getBackgroundImage());
		setupButton = (Button) getActivity().findViewById(R.id.setup_button);
		feedbackGrid = (GridView) getActivity().findViewById(R.id.feedback_grid);

		setupButton.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View view) {
				registerAnalyticsEvent(BaseActivity.Category.UI_ACTION, BaseActivity.Action.BUTTON_PRESS, BaseActivity.Label.REPORT_ACTIVITY_SETUP_BUTTON, null);
				Intent setupIntent = new Intent(getActivity(), SetupActivity.class);
				startActivityForResult(setupIntent, GeneralActivity.REQUEST_CODE_SETUP);
			}
		});

		List<FeedbackType> feedbackTypes = new ArrayList<FeedbackType>();
		feedbackTypes.add(new FeedbackType("Application error", R.drawable.message_attention, "A user is having problems operating the 'Bilik' application"));
		feedbackTypes.add(new FeedbackType("Network Problems", R.drawable.network_connection, "A user has reported that there are network problems in the room or with the application."));
		feedbackTypes.add(new FeedbackType("Projector problem", R.drawable.easel, "The projector of the room is having problems"));
		feedbackTypes.add(new FeedbackType("Phone not working", R.drawable.phone_settings, "The phone in the room is not working"));
		feedbackTypes.add(new FeedbackType("Videoconf. problem", R.drawable.cinema, "The conferencing software/hardware is having problems"));

		feedbackGrid.setAdapter(new FeedbackAdapter(feedbackTypes));
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	protected void onResourceStateChange(ResourceState resourceState) {
		// TODO: Refresh view
	}

	@Override
	protected void onClockTick(DateTime currentDatetime) {
		// TODO: Update clock, if needed
	}

	private class FeedbackType {
		private String message;
		private int icon;
		private String body;

		public FeedbackType(String message, int icon, String body) {
			this.message = message;
			this.icon = icon;
			this.body = body;
		}

		public Button getButton(Context context, Button convertButton, ViewGroup viewGroup) {
			if (convertButton == null) {
				LayoutInflater inflater = ((Activity)context).getLayoutInflater();
				convertButton = (Button)inflater.inflate(R.layout.report_button_layout, viewGroup, false);
			}
			convertButton.setText(message);
			Drawable iconDrawable = context.getResources().getDrawable(icon);
			convertButton.setCompoundDrawablesWithIntrinsicBounds(iconDrawable, null, null, null);
			convertButton.setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View view) {
					registerAnalyticsEvent(BaseActivity.Category.UI_ACTION, BaseActivity.Action.BUTTON_PRESS, BaseActivity.Label.REPORT_ACTIVITY_FEEDBACK_BUTTON, null);
					onFeedbackClicked(FeedbackType.this);
				}
			});
			return convertButton;
		}
	}

	private class FeedbackAdapter extends BaseAdapter {
		private final List<FeedbackType> feedbackTypes;

		public FeedbackAdapter(List<FeedbackType> feedbackTypes) {
			this.feedbackTypes = feedbackTypes;
		}

		public int getCount() {
			return feedbackTypes.size();
		}

		public Object getItem(int position) {
			return feedbackTypes.get(position);
		}

		public long getItemId(int position) {
			return 0;
		}

		// create a new ImageView for each item referenced by the Adapter
		public View getView(int position, View convertView, ViewGroup parent) {
			return ((FeedbackType)getItem(position)).getButton(getActivity(), (Button)convertView, parent);
		}
	}

	private void onFeedbackClicked(final FeedbackType feedbackType) {
		new AsyncTask<Void, Void, String>() {
			@Override protected String doInBackground(Void... voids) {
				try {
					GMailOauthSender sender = new GMailOauthSender(getActivity(), getConfiguration().getAccount());
					String subject = "[Bilik] " + feedbackType.message + " on " + getConfiguration().getAlternativeResourceName();
					String body = feedbackType.body + "\n";
					body += "Room: " + getConfiguration().getAlternativeResourceName();
					// TODO: Add more information
					sender.sendMail(subject, body, getConfiguration().getSupportEmail());
					return null;
				} catch (Exception ex) {
					Crashlytics.logException(ex);
					return "Sorry, but for some reason we are unable to report your feedback. We will fix this as soon as possible.";
				}
			}

			@Override protected void onPostExecute(String error) {
				Toast.makeText(getActivity(), error != null ? error : "Thanks for sharing! Your feedback has been reported successfully", Toast.LENGTH_LONG).show();
			}
		}.execute();
		moveToMainFragment();
	}
}
