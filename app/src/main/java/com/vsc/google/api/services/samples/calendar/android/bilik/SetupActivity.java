package com.vsc.google.api.services.samples.calendar.android.bilik;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.tresmonos.calendar.Configuration;
import com.tresmonos.calendar.google.ActionExecutorCallback;
import com.tresmonos.calendar.model.Account;
import com.tresmonos.calendar.model.RemoteResource;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * An activity designed to setup the configuration of this room
 */
public class SetupActivity extends BaseActivity {
	private SetupTask setupTask = null;

    // UI references.
	private Spinner resourceSpinnerView;
	private Spinner accountSpinnerView;
	private Spinner areaSpinnerView;
	private EditText supportEmailView;
	private EditText alternativeNameView;
	private EditText resourceFeaturesView;
	private Timer dismissTimer;
	private int secondsToDismiss;
	private TextView dismissLabel;
	private Button createAreaButton;
	private Button doneButton;
	private ImageView backgroundImage;
	private ProgressBar progressBar;
	private CheckBox makeMasterSwitch;
	private LinearLayout makeMasterSwitchRow;

	/** Request code identifying the result of picking an image */
	private static final int REQUEST_CODE_SELECT_IMAGE = 100;

	/** Key used to receive any potential error message that would trigger the presentation of this screen */
	public static final String ERROR_MESSAGE_INTENT_KEY = "error_message_key";
	private List<String> areas = ImmutableList.of();

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_setup);
		dismissKeyboardOnTouch(findViewById(R.id.setup_form));
		doneButton = (Button) findViewById(R.id.done_button);
		doneButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				attemptSetup();
			}
		});

		backgroundImage = (ImageView) findViewById(R.id.background_image);
		progressBar = (ProgressBar) findViewById(R.id.progress_bar);
		accountSpinnerView = (Spinner) findViewById(R.id.account_selector);
		areaSpinnerView = (Spinner) findViewById(R.id.area_selector);
		createAreaButton = (Button) findViewById(R.id.create_new_area);
		Typeface font = Typeface.createFromAsset( getAssets(), "fontawesome-webfont.ttf" );
		createAreaButton.setTypeface(font, 1);
		createAreaButton.setText(getResources().getString(R.string.create_new_area));

		supportEmailView = (EditText)findViewById(R.id.support_email);
		resourceSpinnerView = (Spinner) findViewById(R.id.resource_selector);
		alternativeNameView = (EditText)findViewById(R.id.alternative_name);
		resourceFeaturesView = (EditText)findViewById(R.id.features);
		Button imageButton = (Button)findViewById(R.id.background_image_button);
		imageButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				registerAnalyticsEvent(Category.UI_ACTION, Action.BUTTON_PRESS, Label.SETUP_ACTIVITY_SELECT_IMAGE_BUTTON, null);
				Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
				photoPickerIntent.setType("image/*");
				startActivityForResult(photoPickerIntent, REQUEST_CODE_SELECT_IMAGE);
			}
		});
		dismissLabel = (TextView)findViewById(R.id.automatic_dismiss);
		makeMasterSwitchRow = (LinearLayout) findViewById(R.id.is_master_switch_row);
		makeMasterSwitch = (CheckBox)findViewById(R.id.is_master_switch);
	}

	private void initializeView() {
		List<Account> accounts = calendarService.getAccounts();
		accountSpinnerView.setAdapter(createArrayAdapter(getString(R.string.please_select_an_account), accounts));
		accountSpinnerView.setSelection(getSelectedPosition(configuration.getAccount(), accounts), false);
		accountSpinnerView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				onAccountSelected(position != 0 ? (String) parent.getSelectedItem() : null);
			}

			@Override
			public void onNothingSelected(AdapterView<?> adapterView) {
				onAccountSelected(null);
			}
		});

		createAreaButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Set an EditText view to get user input
				final EditText input = new EditText(SetupActivity.this);
				new AlertDialog.Builder(SetupActivity.this)
				.setTitle("Create a new Area")
				.setMessage("Enter name of the area")
				.setView(input)
				.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						final String newAreaName = input.getText().toString();
						try {
							showProgressBar(true);
							if (Strings.isNullOrEmpty(getSelectedAccount())) {
								eventBus.post("Please select an account first.");
								return;
							}
							if (Strings.isNullOrEmpty(newAreaName) || newAreaName.trim().length() == 0) {
								eventBus.post("Invalid area name");
								return;
							}
							if (Iterables.any(areas, getPredicateToCompareAreaName(newAreaName))) {
								eventBus.post(String.format("Area %s already exists", newAreaName));
								return;
							}
							String accountName = getSelectedAccount();
							configuration.createArea(accountName, newAreaName);
							loadAndSelectAreas(accountName, newAreaName);
						} catch (Exception e) {
							Crashlytics.logException(e);
							String errorMessage = "Error trying to create area " + newAreaName;
							Log.e(SetupActivity.this.getClass().getName(), errorMessage, e);
							eventBus.post(errorMessage);
						} finally {
							showProgressBar(false);
						}
					}
				}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Do nothing.
					}
				}).show();
			}
		});
		supportEmailView.setText(configuration.getSupportEmail());
		updateDataForAccount(configuration.getAccount());
		resourceSpinnerView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
				onResourceSelected(position > 0 ? (String) resourceSpinnerView.getSelectedItem() : null);
			}

			@Override
			public void onNothingSelected(AdapterView<?> adapterView) {
				onResourceSelected(null);
			}
		});
		alternativeNameView.setText(configuration.getAlternativeResourceName());
		resourceFeaturesView.setText(configuration.getResourceFeatures());
		backgroundImage.setImageBitmap(configuration.getBackgroundImage());

		// Lets initiate the dismiss timer if needed
		String configurationError = getIntent().getStringExtra(ERROR_MESSAGE_INTENT_KEY);
		if (configurationError == null)
			configurationError = configuration.checkConfiguration();
		secondsToDismiss = 20;
		doneButton.setText(getResources().getText(configurationError == null ? R.string.action_dismiss : R.string.action_done));
		if (configurationError == null) {
			dismissLabel.setVisibility(View.VISIBLE);
			dismissTimer = new Timer();
			dismissTimer.schedule(new TimerTask() {
				public void run() {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							onDismissTimer();
						}
					});
				}
			}, 0, 1000);
		} else {
			dismissLabel.setVisibility(View.GONE);
		}
	}

	private Predicate<String> getPredicateToCompareAreaName(final String newAreaName) {
		return new Predicate<String>() {
			@Override
			public boolean apply(String storedArea) {
				return storedArea.equalsIgnoreCase(newAreaName);
			}
		};
	}

	@Override
	protected void onServiceConnected() {
		initializeView();
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
		super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

		switch(requestCode) {
			case REQUEST_CODE_SELECT_IMAGE:
				if(resultCode == RESULT_OK){
					Uri selectedImage = imageReturnedIntent.getData();
					updateSelectedImage(selectedImage.toString());
				}
		}
	}

	private void updateSelectedImage(String imageUri) {
		showProgressBar(true);
		new UpdateBackgroundAsyncTask(imageUri).execute();
	}

	private class UpdateBackgroundAsyncTask extends AsyncTask<Void, Void, Bitmap> {
		private String imageUri;

		public UpdateBackgroundAsyncTask(String imageUri) {
			this.imageUri = imageUri;
		}

		@Override
		public Bitmap doInBackground(Void... params) {
			if (imageUri != null) {
				try {
					Display display = getWindowManager().getDefaultDisplay();
					Point size = new Point();
					display.getRealSize(size);

					InputStream imageStream = getContentResolver().openInputStream(Uri.parse(imageUri));
					Bitmap image = BitmapFactory.decodeStream(imageStream);
					float imageRatio = ((float)image.getHeight()) / image.getWidth(); // 1000 / 3000
					float screenRatio = ((float)size.y) / size.x; // 768 / 1024
					if (imageRatio > screenRatio) {
						image = Bitmap.createScaledBitmap(image, size.x, (int)(imageRatio * size.x), true);
					} else {
						image = Bitmap.createScaledBitmap(image, (int)(size.y / imageRatio), size.y, true);
					}
					image = Bitmap.createBitmap(image, (image.getWidth() - size.x) / 2, (image.getHeight() - size.y) / 2, size.x, size.y);
					image = convertToBlackAndWhite(image);
					return image;
				} catch (FileNotFoundException e) {
					Crashlytics.logException(e);
					Log.e(getClass().getName(), "Error trying to read background image " + imageUri, e);
					return null;
				}
			} else {
				return null;
			}
		}

		private Bitmap convertToBlackAndWhite(Bitmap sampleBitmap){
			ColorMatrix bwMatrix =new ColorMatrix();
			bwMatrix.setSaturation(0);
			ColorMatrixColorFilter colorFilter= new ColorMatrixColorFilter(bwMatrix);
			Bitmap rBitmap = sampleBitmap.copy(Bitmap.Config.ARGB_8888, true);
			Paint paint=new Paint();
			paint.setColorFilter(colorFilter);
			Canvas myCanvas =new Canvas(rBitmap);
			myCanvas.drawBitmap(rBitmap, 0, 0, paint);
			return rBitmap;
		}

		@Override
		public void onPostExecute(Bitmap image) {
			showProgressBar(false);
			if (image != null) {
				backgroundImage.setImageBitmap(image);
			} else if (imageUri != null) {
				Crashlytics.log("Unable to load image (imageUri: " + imageUri + ")");
				eventBus.post(getResources().getText(R.string.error_unable_to_get_image));
			} else {
				backgroundImage.setImageBitmap(null);
			}
		}
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		cancelDismissTimer();
		return super.dispatchTouchEvent(ev);
	}

	private Pair<String, ? extends View> validateSelections() {
		List<Pair<Spinner, Integer>> viewsToValidate = ImmutableList.of(Pair.create(accountSpinnerView, R.string.error_missing_account),
				Pair.create(resourceSpinnerView, R.string.error_missing_resource),
				Pair.create(areaSpinnerView, R.string.error_missing_area));

		for (Pair<Spinner, Integer> pairToValidate : viewsToValidate) {
			Pair<String, ? extends View> result = validateSpinner(pairToValidate.first, pairToValidate.second);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	private Pair<String, ? extends View> validateSpinner(Spinner view, int errorString) {
		if (view.getSelectedItemPosition() == 0 || view.getSelectedItem() == null) {
			return Pair.create(getString(errorString), view);
		}
		return null;
	}

	private void showProgressBar(boolean show) {
		progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
	}

	private void onResourceSelected(String resourceName) {
		Log.d("SetupActivity", String.format("Resource %s was selected.", resourceName));
		registerAnalyticsEvent(Category.UI_ACTION, Action.ITEM_SELECTED, Label.SETUP_ACTIVITY_RESOURCE_SELECTOR, null);
		configuration.getResourceInfo(getSelectedAccount(), (String) resourceSpinnerView.getSelectedItem(), new Configuration.ResourceInfoCallback() {
			@Override
			public void onResourceInformation(String alternativeResourceName, String roomFeatures, boolean isMaster, String areaName, Collection<Configuration.DeviceInfo> devices) {
				if (Strings.isNullOrEmpty(alternativeNameView.getText().toString())) {
					alternativeNameView.setText(alternativeResourceName);
				}
				if (Strings.isNullOrEmpty(resourceFeaturesView.getText().toString())) {
					resourceFeaturesView.setText(roomFeatures);
				}
				areaSpinnerView.setSelection(getSelectedPosition(areaName, areas));
				boolean willBecomeMaster = devices == null || devices.size() == 0 || isMaster;
				// The device will become master anyways, no need to bother the user asking if he wants it or not.
				makeMasterSwitchRow.setVisibility(willBecomeMaster ? View.GONE : View.VISIBLE);
			}
		});
	}

	private String getSelectedAccount() {
		return accountSpinnerView.getSelectedItemPosition() > 0 ? (String) accountSpinnerView.getSelectedItem() : "";
	}

	private void cancelDismissTimer() {
		if (dismissTimer == null)
			return;
		dismissTimer.cancel();
		dismissTimer = null;
		dismissLabel.setVisibility(View.GONE);
	}

	private void onDismissTimer() {
		secondsToDismiss--;
		if (secondsToDismiss <= 0) {
			registerAnalyticsEvent(Category.SYSTEM_ACTION, Action.TIMEOUT, Label.SETUP_ACTIVITY_AUTO_DISMISS_TIMER, null);
			dismissTimer.cancel();
			setResult(GeneralActivity.RESULT_CANCELLED);
			finish();
		} else {
			dismissLabel.setText(String.format("Dismissing in %s seconds", secondsToDismiss));
		}
	}

	private void onAccountSelected(final String accountName) {
		registerAnalyticsEvent(Category.UI_ACTION, Action.ITEM_SELECTED, Label.SETUP_ACTIVITY_ACCOUNT_SELECTOR, null);
		if (accountName != null) {
			updateDataForAccount(accountName);
			configuration.getAccountInfo(accountName, new Configuration.AccountInfoCallback() {
				@Override
				public void onAccountInformation(String supportEmail) {
					supportEmailView.setText(supportEmail);
				}
			});
		} else {
			supportEmailView.setText(null);
		}
		alternativeNameView.setText(null);
	}

	private void updateDataForAccount(String accountName) {
		if (!Strings.isNullOrEmpty(accountName)) {
			final Account account = calendarService.getAccount(accountName);
			showProgressBar(true);
			calendarService.getAllResources(account, new ActionExecutorCallback<List<RemoteResource>>() {
				@Override
				public void onExecutionFinished(final boolean success, final List<RemoteResource> resources) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (success) {
								resourceSpinnerView.setAdapter(createArrayAdapter(getString(R.string.please_select_a_room), resources));
								int resourceIndex = getSelectedPosition(configuration.getResource(), resources);
								resourceSpinnerView.setSelection(resourceIndex, false);
								RemoteResource resource = resources.get(resourceIndex);
								loadAndSelectAreas(account.getName(), resource.getAreaName());
							}
							showProgressBar(false);
						}
					});
				}
			});
		}
	}

	private void loadAndSelectAreas(String accountName, String areaName) {
		areas = Lists.newArrayList(configuration.getAreas(accountName));
		areaSpinnerView.setAdapter(createArrayAdapter(getString(R.string.please_select_an_area), areas));
		areaSpinnerView.setSelection(getSelectedPosition(areaName, areas));
	}

	private ArrayAdapter<String> createArrayAdapter(String defaultValue, List<?> alternatives) {
		List<String> strings = Lists.newArrayList();
		strings.add(defaultValue);
		for (Object alternative : alternatives) {
			strings.add(alternative.toString());
		}
		return new ArrayAdapter<String>(SetupActivity.this, R.layout.spinner_dropdown_item_layout, strings) {
			@Override
			public View getDropDownView(int position, View convertView, ViewGroup parent) {
				View view = super.getDropDownView(position, convertView, parent);
				view.setBackgroundColor(getResources().getColor(R.color.setup_background_color));
				return view;
			}
		};
	}

	private int getSelectedPosition(final String selection, List<?> alternatives) {
		if (selection == null)
			return 0;
		return Iterables.indexOf(alternatives, new Predicate<Object>() {
			@Override
			public boolean apply(Object alternative) {
				return selection.equals(alternative.toString());
			}
		}) + 1;
	}

    /**
     * Attempts to setup the device. Any missing data is reported immediately
     */
    private void attemptSetup() {
		registerAnalyticsEvent(Category.UI_ACTION, Action.BUTTON_PRESS, Label.SETUP_ACTIVITY_READY_BUTTON, null);

        if (setupTask != null) {
            return;
        }

        // Check that all required values are provided
		Pair<String, ? extends View> error = validateSelections();

        if (error != null) {
            // There was an error; don't attempt setup and focus the first
            // form field with an error.
			registerAnalyticsEvent(Category.SYSTEM_ACTION, Action.VALIDATION_ERROR, Label.SETUP_ACTIVITY_ATTEMPT_SETUP, null);
			eventBus.post(error.first);
            error.second.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the setup attempt.
			String resourceName = resourceSpinnerView.getSelectedItem().toString();
			showProgressBar(true);
            setupTask = new SetupTask(resourceName);
            setupTask.execute((Void) null);
        }
    }

    /**
     * Async task that would try registering the selections of
	 * the user. If everything works well then the activity will be dismissed.
     */
    public class SetupTask extends AsyncTask<Void, Void, String> {
        private final String mResourceName;

		SetupTask(String resourceName) {
            mResourceName = resourceName;
        }

        @Override
        protected String doInBackground(Void... params) {
			try {
//TODO: This implies that the resource should be sync in advance
//				if (calendarService.getResource(mResourceName) == null) {
//					return getString(R.string.error_invalid_resource);
//				}
				configuration.update(getSelectedAccount(),
						supportEmailView.getText().toString(),
						(String) resourceSpinnerView.getSelectedItem(),
						(String) areaSpinnerView.getSelectedItem(),
						alternativeNameView.getText().toString(),
						resourceFeaturesView.getText().toString(),
						((BitmapDrawable) backgroundImage.getDrawable()).getBitmap(),
						makeMasterSwitch.isChecked());
			} catch (Configuration.ConfigurationException ex) {
				return getString(R.string.error_unable_to_contact_server);
			} catch (Throwable ex) {
				return getString(R.string.error_invalid_resource);
			}
			return null;
        }

        @Override
        protected void onPostExecute(final String errorMessage) {
            setupTask = null;
			showProgressBar(false);

            if (errorMessage == null) {
				setResult(GeneralActivity.RESULT_CONFIG_CHANGE);
				finish();
            } else {
				registerAnalyticsEvent(Category.SYSTEM_ACTION, Action.VALIDATION_ERROR, Label.SETUP_ACTIVITY_ATTEMPT_SETUP, null);
				Toast.makeText(SetupActivity.this, errorMessage, Toast.LENGTH_LONG).show();
				resourceSpinnerView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
			showProgressBar(false);
			resourceSpinnerView = null;
        }
    }

	private void dismissKeyboardOnTouch(View view) {
		//Set up touch listener for non-text box views to hide keyboard.
		if(!(view instanceof EditText)) {
			view.setOnTouchListener(new View.OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
					inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
					return false;
				}
			});
		}

		//If a layout container, iterate over children and seed recursion.
		if (view instanceof ViewGroup) {
			for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
				View innerView = ((ViewGroup) view).getChildAt(i);
				dismissKeyboardOnTouch(innerView);
			}
		}
	}

	@Override
	public void onBackPressed() {
		if (configuration.checkConfiguration() == null) {
			Toast.makeText(this, getString(R.string.error_incomplete_configuration), Toast.LENGTH_LONG).show();
		} else {
			super.onBackPressed();
		}
	}
}



