package com.tresmonos.calendar;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.Settings;
import android.util.Log;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.parse.FindCallback;
import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.tresmonos.calendar.model.Account;
import com.tresmonos.calendar.model.Area;
import com.tresmonos.calendar.model.Resource;
import com.tresmonos.calendar.model.ResourceState;
import com.tresmonos.calendar.notifications.CalendarNotificationService;

import org.jcaki.Strings;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Handles local and remote configuration persistance
 */
public class Configuration {
	private static final String PREFS_NAME = "ConfigFile";

	// Keys for the local settings properties
	private static final String RESOURCE_SETTING = "resource";
	private static final String AREA_SETTING = "area";
	private static final String ACCOUNT_SETTING = "account";
	private static final String SUPPORT_EMAIL_SETTING = "supportEmail";
	private static final String ALTERNATIVE_NAME_SETTING = "alternativeName";
	private static final String FEATURES_SETTING = "features";
	private static final String BACKGROUND_IMAGE_FILE = "background.png";
	private static final String IS_MASTER = "isMaster";

	// Tables and columns on the backend server
	private static final String RESOURCES_TABLE = "Resources";
	private static final String RESOURCES_TABLE_DOMAIN_NAME = "domainName";
    private static final String RESOURCES_TABLE_AREA = "area";
	private static final String RESOURCES_TABLE_RESOURCE_NAME = "resourceName";
    private static final String RESOURCES_TABLE_MASTER_DEVICE_ID = "masterDeviceId";
	private static final String RESOURCES_TABLE_ALTERNATIVE_NAME = "alternativeName";
	private static final String RESOURCES_TABLE_ROOM_FEATURES = "roomFeatures";
    private static final String RESOURCES_TABLE_CURRENT_STATE = "currentState";
    private static final String RESOURCES_TABLE_CURRENT_RESERVATION_TITLE = "currentReservationTitle";
    private static final String RESOURCES_TABLE_NEXT_EVENT_DATE = "nextEventDate";

	private static final String AREA_TABLE = "Area";
	private static final String AREA_TABLE_NAME = "name";
	public static final Function<ParseObject, String> PARSE_AREA_TO_AREA_NAME_FUNCTION = new Function<ParseObject, String>() {
		@Nullable
		@Override
		public String apply(ParseObject areaRow) {
			return areaRow.get(AREA_TABLE_NAME).toString();
		}
	};
	private static final String AREA_TABLE_DOMAIN = "domain";

	private static final String DOMAINS_TABLE = "Domains";
	private static final String DOMAINS_TABLE_DOMAIN_NAME = "domainName";
	private static final String DOMAINS_TABLE_SUPPORT_EMAIL = "supportEmail";

	private static final String DEVICES_TABLE = "Devices";
	private static final String DEVICES_TABLE_DOMAIN_NAME = "domainName";
	private static final String DEVICES_TABLE_RESOURCE_NAME = "resourceName";
	private static final String DEVICES_TABLE_DEVICE_ID = "deviceId";
	private static final String DEVICES_TABLE_ACCOUNT_NAME = "accountName";
	private static final String DEVICES_TABLE_BATTERY_LEVEL = "batteryLevel";
	private static final String DEVICES_TABLE_BATTERY_IS_CHARGING = "batteryIsCharging";
	private static final String DEVICES_TABLE_LAST_UPDATED = "lastUpdated";
	private static final String DEVICES_TABLE_REGISTRATION_TIME = "registrationTime";

	public static final String DEFAULT_AREA = "";

	private static final String ACCOUNT_TABLE = "Account";
	private static final String ACCOUNT_TABLE_EMAIL = "email";
	private static final String ACCOUNT_TABLE_FULLNAME = "fullName";

	private SharedPreferences settings;
	private String deviceId;
	private Context context;

	public Bitmap getBackgroundImage() {
		return readImage(BACKGROUND_IMAGE_FILE);
	}

	/**
	 * @return null if the configuration is okay, otherwise it returns a message explaining what part of the
	 * configuration is wrong or missing. Typically this should cause the Setup page to be displayed for the
	 * user to fix the problem.
	 */
	public String checkConfiguration() {
		try {
			refresh();
			return getAccount() != null && getResource() != null ? null : "No configuration found for this device";
		} catch (ParseException ex) {
			return "Unable to communicate with the server";
		} catch (ConfigurationException ex) {
			return ex.getMessage();
		}
	}

	public List<String> getAreas(String accountName) {
		try {
			ParseObject domain;
			if (Strings.isNullOrEmpty(accountName) || (domain = getDomain(accountName)) == null) {
				return Lists.newArrayList();
			}
			return Lists.transform(getParseAreasFromDomain(domain), PARSE_AREA_TO_AREA_NAME_FUNCTION);
		} catch (ParseException e) {
			throw Throwables.propagate(e);
		}
	}

	private List<ParseObject> getParseAreasFromDomain(ParseObject domain) throws ParseException {
		ParseQuery<ParseObject> query = ParseQuery.getQuery(AREA_TABLE);
		return query.whereEqualTo(AREA_TABLE_DOMAIN, domain).find();
	}

	/**
	 * creates an area synchronously!
	 * @param accountName
	 * @param newAreaName
	 * @throws ParseException
	 */
	public void createArea(String accountName, String newAreaName) throws ParseException {
		ParseObject domain = getDomain(accountName);
		ParseObject area = new ParseObject(AREA_TABLE);
		area.put(AREA_TABLE_NAME, newAreaName);
		area.put(AREA_TABLE_DOMAIN, domain);
		area.save();
	}

	public class ConfigurationException extends Exception {
		public ConfigurationException(String message, Throwable cause) {
			super(message, cause);
		}
		public ConfigurationException(String message) {
			super(message);
		}
	}

	/** @return the singleton configuration instance */
	public static Configuration getInstance(Context ctx) {
		return new Configuration(ctx);
	}

	private Configuration(Context ctx) {
		// Retrieve from local storage
		context = ctx;
		settings = context.getSharedPreferences(PREFS_NAME, 0);
		deviceId = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
	}

	/** @return the resource name associated to this device */
	public String getResource() {
        return settings.getString(RESOURCE_SETTING, null);
    }

	/** @return the resource name associated to this device */
	public String getCurrentArea() {
		return settings.getString(AREA_SETTING, DEFAULT_AREA);
	}

	/** @return account associated to this device */
	public String getAccount() {
		return settings.getString(ACCOUNT_SETTING, null);
	}

	/** @return support email that should be used to report problems */
	public String getSupportEmail() {
		return settings.getString(SUPPORT_EMAIL_SETTING, null);
	}

	/** @return alternative resource name to present to the users */
	public String getAlternativeResourceName() {
		return settings.getString(ALTERNATIVE_NAME_SETTING, getResource());
	}

	public boolean isMaster() {
		return true;
		//return settings.getBoolean(IS_MASTER, false);
	}

	/** Updates the configuration */
	public void update(final String accountName,
					   final String supportEmail,
					   final String resourceName,
					   final String areaName,
					   final String alternativeName,
					   final String features,
					   final Bitmap backgroundImage,
					   final boolean makeMaster) throws ConfigurationException {
		if (backgroundImage != null)
			saveImage(backgroundImage, BACKGROUND_IMAGE_FILE);
		try {
			String domainName = Account.extractDomain(accountName);
			persistDeviceInfo(deviceId, domainName, supportEmail, accountName, resourceName, areaName, alternativeName, features);
			if (makeMaster) {
				makeThisDeviceMaster();
			}
			ParseObject resourceObject = findOrCreateResourceInfo();
			boolean isMaster = deviceId.equals(resourceObject.getString(RESOURCES_TABLE_MASTER_DEVICE_ID));
			settings.edit()
					.putString(RESOURCE_SETTING, resourceName)
					.putString(AREA_SETTING, areaName)
					.putString(ACCOUNT_SETTING, accountName)
					.putString(SUPPORT_EMAIL_SETTING, supportEmail)
					.putString(ALTERNATIVE_NAME_SETTING, alternativeName)
					.putString(FEATURES_SETTING, features)
					.putBoolean(IS_MASTER, isMaster)
					.apply();

            // register push notification for this device
		} catch (ParseException ex) {
			handleParseError("Persisting setup information", ex);
			throw new ConfigurationException("Unable to persist configuration changes", ex);
		}
	}

    /** Re-reads the configuration from the backend */
	private void refresh() throws ParseException, ConfigurationException {
		ParseObject resourceObject = findOrCreateResourceInfo();
		String alternativeName = resourceObject.getString(RESOURCES_TABLE_ALTERNATIVE_NAME);
		String roomFeatures = resourceObject.getString(RESOURCES_TABLE_ROOM_FEATURES);
		boolean isMaster = deviceId.equals(resourceObject.getString(RESOURCES_TABLE_MASTER_DEVICE_ID));
		settings.edit()
				.putString(ALTERNATIVE_NAME_SETTING, alternativeName)
				.putString(FEATURES_SETTING, roomFeatures)
				.putBoolean(IS_MASTER, isMaster)
				.apply();
	}

	private Bitmap readImage(String fileName) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		File file = new File(context.getFilesDir(), fileName);
		return BitmapFactory.decodeFile(file.getAbsolutePath(), options);

	}

	private void saveImage(Bitmap image, String fileName) throws ConfigurationException {
		FileOutputStream out = null;
		try {
			File file = new File(context.getFilesDir(), fileName);
			out = new FileOutputStream(file);
			image.compress(Bitmap.CompressFormat.PNG, 90, out);
		} catch (FileNotFoundException ex) {
			throw new ConfigurationException("Unable to save background image", ex);
		} finally {
			try {
				if (out != null)
					out.close();
			} catch (IOException ex) {
				// Ignore.
			}
		}
	}

	public static class DeviceInfo {
		private final String deviceId;
		private final boolean isMaster;

		public DeviceInfo(String deviceId, boolean isMaster) {
			this.deviceId = deviceId;
			this.isMaster = isMaster;
		}

		public String getDeviceId() {
			return  deviceId;
		}

		public boolean isMaster() {
			return isMaster;
		}
	}

	public interface ResourceInfoCallback {
		void onResourceInformation(String alternativeResourceName, String roomFeatures, boolean isMaster, String areaName, Collection<DeviceInfo> devices);
	}

	/**
	 * Obtains information about the given resource for the given account, and calls the provided callback
	 * once all this information has been obtained.
	 */
	public void getResourceInfo(final String accountName, final String resourceName, final ResourceInfoCallback callback) {
		final String domainName = Account.extractDomain(accountName);
		ParseQuery<ParseObject> query = ParseQuery.getQuery(RESOURCES_TABLE);
		query.whereEqualTo(RESOURCES_TABLE_DOMAIN_NAME, domainName);
		query.whereEqualTo(RESOURCES_TABLE_RESOURCE_NAME, resourceName);
		query.findInBackground(new FindCallback<ParseObject>() {
			@Override
			public void done(final List<ParseObject> parseResourceObjects, ParseException e) {
				if (e != null) {
					handleParseError("Retrieving resource info: " + domainName + "/" + resourceName, e);
					return;
				}
				boolean resourceFound = parseResourceObjects != null && parseResourceObjects.size() > 0;
				if (!resourceFound) {
					callback.onResourceInformation(null, null, false, "", new ArrayList<DeviceInfo>());
					return;
				}
				final ParseObject parseResourceObject = parseResourceObjects.get(0);
				ParseQuery<ParseObject> query = ParseQuery.getQuery(DEVICES_TABLE);
				query.whereEqualTo(DEVICES_TABLE_DOMAIN_NAME, domainName);
				query.whereEqualTo(DEVICES_TABLE_RESOURCE_NAME, resourceName);
				query.findInBackground(new FindCallback<ParseObject>() {
					@Override
					public void done(List<ParseObject> parseDeviceObjects, ParseException e) {
						if (e != null) {
							handleParseError("Retrieving devices info: " + domainName + "/" + resourceName, e);
							return;
						}
						String masterDeviceId = parseResourceObject.getString(RESOURCES_TABLE_MASTER_DEVICE_ID);
						String alternativeName = parseResourceObject.getString(RESOURCES_TABLE_ALTERNATIVE_NAME);
						String roomFeatures = parseResourceObject.getString(RESOURCES_TABLE_ROOM_FEATURES);
						String areaName = "";
						try {
							ParseObject parseObject = parseResourceObject.getParseObject(RESOURCES_TABLE_AREA);
							if (parseObject == null) return;
							areaName = parseObject.fetchIfNeeded().getString(AREA_TABLE_NAME);
						} catch (ParseException e1) {
							handleParseError("Retrieving area for resource:" + resourceName, e1);
							return;
						}
						boolean isMaster = masterDeviceId != null && masterDeviceId.equals(deviceId);
						Map<String, DeviceInfo> devices = new HashMap<String, DeviceInfo>();
						for (ParseObject parseDeviceObject : parseDeviceObjects) {
							String deviceId = parseDeviceObject.getString(DEVICES_TABLE_DEVICE_ID);
							DeviceInfo info = new DeviceInfo(deviceId, deviceId.equals(masterDeviceId));
							devices.put(deviceId, info);
						}
						callback.onResourceInformation(alternativeName, roomFeatures, isMaster, areaName, devices.values());
					}
				});
			}
		});
	}

	/**
	 * Marks this device as the master for the resource associated to this device
	 */
	public void makeThisDeviceMaster() throws ConfigurationException {
		try {
			while(true) {
				ParseObject parseResourceObject = findOrCreateResourceInfo();
				String currentMasterDeviceId = parseResourceObject.getString(RESOURCES_TABLE_MASTER_DEVICE_ID);
				if (currentMasterDeviceId != null && currentMasterDeviceId.equals(deviceId))
					return;
				parseResourceObject.put(RESOURCES_TABLE_MASTER_DEVICE_ID, deviceId);
				parseResourceObject.save();
			}
		} catch (ParseException ex) {
			handleParseError("Persisting status information", ex);
			throw new ConfigurationException("Unable to persist resource information", ex);
		}
	}

	private String persistDomainInfo(String domainName, String supportEmail) throws ParseException {
		ParseQuery<ParseObject> query = ParseQuery.getQuery(DOMAINS_TABLE);
		query.whereEqualTo(DOMAINS_TABLE_DOMAIN_NAME, domainName);
		List<ParseObject> domainObjects = query.find();
		ParseObject parseDomainObject;
		if (domainObjects == null || domainObjects.size() < 1) {
			parseDomainObject = ParseObject.create(DOMAINS_TABLE);
			parseDomainObject.put(DOMAINS_TABLE_DOMAIN_NAME, domainName);
		} else {
			parseDomainObject = domainObjects.get(0);
		}
		parseDomainObject.put(DOMAINS_TABLE_SUPPORT_EMAIL, supportEmail);
		parseDomainObject.saveInBackground();
		return parseDomainObject.getObjectId();
	}

	private ParseObject findOrCreateResourceInfo() throws ParseException, ConfigurationException {
		// Find device
		ParseObject parseDeviceObject = findOrCreateDeviceInfo(deviceId);
		String domainName = parseDeviceObject.getString(RESOURCES_TABLE_DOMAIN_NAME);
		String resourceName = parseDeviceObject.getString(RESOURCES_TABLE_RESOURCE_NAME);
		if (domainName == null || resourceName == null)
			throw new ConfigurationException("Unable to find data for this device (device: " + deviceId + ")");
        return findOrCreateResourceInfo(domainName, resourceName);
	}

    private ParseObject findOrCreateResourceInfo(String domainName, String resourceName) throws ParseException {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(RESOURCES_TABLE);
        query.whereEqualTo(RESOURCES_TABLE_DOMAIN_NAME, domainName);
        query.whereEqualTo(RESOURCES_TABLE_RESOURCE_NAME, resourceName);
        List<ParseObject> parseResourceObjects = query.find();
        ParseObject parseResourceObject;
        if (parseResourceObjects == null || parseResourceObjects.size() < 1) {
            parseResourceObject = ParseObject.create("Resources");
            parseResourceObject.put(RESOURCES_TABLE_DOMAIN_NAME, domainName);
            parseResourceObject.put(RESOURCES_TABLE_RESOURCE_NAME, resourceName);
            parseResourceObject.put(RESOURCES_TABLE_MASTER_DEVICE_ID, deviceId);
        } else {
            parseResourceObject = parseResourceObjects.get(0);
        }
        return parseResourceObject;
    }

    private String persistResourceInfo(String alternativeName, String roomFeatures, ParseObject parseArea) throws ParseException, ConfigurationException {
		ParseObject parseResourceObject = findOrCreateResourceInfo();
	    parseResourceObject.put(RESOURCES_TABLE_ALTERNATIVE_NAME, alternativeName);
		parseResourceObject.put(RESOURCES_TABLE_ROOM_FEATURES, roomFeatures);
	    parseResourceObject.put(RESOURCES_TABLE_AREA, parseArea);
		parseResourceObject.save();
		return parseResourceObject.getObjectId();
	}

	private ParseObject findOrCreateArea(String parseDomainId, String areaName) throws ParseException {
		ParseObject domain = ParseQuery.getQuery(DOMAINS_TABLE).get(parseDomainId);
		List<ParseObject> areas = ParseQuery.getQuery(AREA_TABLE)
				.whereEqualTo(AREA_TABLE_DOMAIN, domain)
				.whereEqualTo(AREA_TABLE_NAME, areaName)
				.find();

		ParseObject newParseArea;
		if (areas == null || areas.isEmpty()) {
			newParseArea = new ParseObject(AREA_TABLE);
			newParseArea.put(AREA_TABLE_DOMAIN, domain);
			newParseArea.put(AREA_TABLE_NAME, areaName);
			newParseArea.save();
		} else {
			newParseArea = areas.get(0);
		}
		return newParseArea;
	}

	private String persistDeviceInfo(String deviceId, String domainName, String supportEmail, String accountName, String resourceName, String areaName, String alternativeName, String roomFeatures) throws ParseException, ConfigurationException {
		ParseObject parseDeviceObject = findOrCreateDeviceInfo(deviceId);
		parseDeviceObject.put(DEVICES_TABLE_DOMAIN_NAME, domainName);
		parseDeviceObject.put(DEVICES_TABLE_RESOURCE_NAME, resourceName);
		parseDeviceObject.put(DEVICES_TABLE_ACCOUNT_NAME, accountName);
		parseDeviceObject.save();
		String domainObjectId = persistDomainInfo(domainName, supportEmail);
		ParseObject parseArea = findOrCreateArea(domainObjectId, areaName);
		persistResourceInfo(alternativeName, roomFeatures, parseArea);
		return parseDeviceObject.getObjectId();
	}

	private ParseObject findOrCreateDeviceInfo(String deviceId) throws ParseException {
		ParseQuery<ParseObject> query = ParseQuery.getQuery(DEVICES_TABLE);
		query.whereEqualTo(DEVICES_TABLE_DEVICE_ID, deviceId);
		List<ParseObject> deviceObject = query.find();
		ParseObject parseDeviceObject;
		if (deviceObject == null || deviceObject.size() < 1) {
			parseDeviceObject = ParseObject.create(DEVICES_TABLE);
			parseDeviceObject.put(DEVICES_TABLE_DEVICE_ID, deviceId);
			parseDeviceObject.put(DEVICES_TABLE_REGISTRATION_TIME, DateTime.now().toDate());
		} else {
			parseDeviceObject = deviceObject.get(0);
		}
		return parseDeviceObject;
	}

	/**
	 * Updates the status of a resource on the server. The status involves the state (busy, free, confirmation pending),
	 * the time of the next event and the title of the current event.
	 */
    public void reportResourceInfo(ResourceState resourceState) throws ConfigurationException {
        try {
            Resource resource = resourceState.getResource();
            ParseObject parseResourceObject = findOrCreateResourceInfo(Account.extractDomain(resource.getAccountName()), resource.getShortName());
            DateTime currentDateTime = resourceState.getCurrentDateTime();
            Period timeToNextEvent = resourceState.getTimeToNextEvent(currentDateTime);

            parseResourceObject.put(RESOURCES_TABLE_CURRENT_STATE, resourceState.getStatus().toString());
            if (resourceState.getCurrentReservation() != null) {
                parseResourceObject.put(RESOURCES_TABLE_CURRENT_RESERVATION_TITLE, resourceState.getCurrentReservation().getTitle());
            } else {
                parseResourceObject.remove(RESOURCES_TABLE_CURRENT_RESERVATION_TITLE);
            }
            if (timeToNextEvent != null) {
                parseResourceObject.put(RESOURCES_TABLE_NEXT_EVENT_DATE, currentDateTime.plus(timeToNextEvent).toDate());
            } else {
                parseResourceObject.remove(RESOURCES_TABLE_NEXT_EVENT_DATE);
            }
            parseResourceObject.save();

            CalendarNotificationService.getOrSubscribe(context, resource.getAccountName(), resource.getFullName()).notifyDeviceStatusUpdated();
        } catch (ParseException ex) {
            handleParseError("Persisting status information", ex);
            throw new ConfigurationException("Unable to persist status information", ex);
        }
    }

	public void reportDeviceInfo(float batteryLevel, boolean batteryIsCharging, ResourceState resourceState) throws ConfigurationException {
		try {
			DateTime currentDateTime = resourceState.getCurrentDateTime();
 			ParseObject parseDeviceObject = findOrCreateDeviceInfo(deviceId);
			parseDeviceObject.put(DEVICES_TABLE_BATTERY_LEVEL, batteryLevel);
			parseDeviceObject.put(DEVICES_TABLE_BATTERY_IS_CHARGING, batteryIsCharging);
			parseDeviceObject.put(DEVICES_TABLE_LAST_UPDATED, currentDateTime.toDate());
			parseDeviceObject.save();

        } catch (ParseException ex) {
			handleParseError("Persisting status information", ex);
			throw new ConfigurationException("Unable to persist status information", ex);
        }
    }

	public String getResourceFeatures() {
		return settings.getString(FEATURES_SETTING, null);
	}

	public interface AccountInfoCallback {
		void onAccountInformation(String supportEmail);
	}

	public void getAccountInfo(final String accountName, final AccountInfoCallback callback) {
		ParseQuery<ParseObject> query = ParseQuery.getQuery(DOMAINS_TABLE);
		query.whereEqualTo(DOMAINS_TABLE_DOMAIN_NAME, Account.extractDomain(accountName));
		query.findInBackground(new FindCallback<ParseObject>() {
			@Override
			public void done(List<ParseObject> parseObjects, ParseException e) {
				if (e != null) {
					handleParseError("Retrieving account info: " + accountName, e);
					return;
				}
				boolean accountFound = parseObjects != null && parseObjects.size() > 0;
				String emailSupport = accountFound ? parseObjects.get(0).getString(DOMAINS_TABLE_SUPPORT_EMAIL) : null;
				callback.onAccountInformation(emailSupport);
			}
		});
	}

	/**
	 * Obtains the list of resources associated with the given account
	 */
    public List<ParseResource> getResources(String accountName) throws IOException {
        try {
            String domainName = Account.extractDomain(accountName);

            List<ParseObject> resources = ParseQuery.getQuery(RESOURCES_TABLE)
                    .whereEqualTo(RESOURCES_TABLE_DOMAIN_NAME, domainName)
                    .find();
            return Lists.transform(resources, new Function<ParseObject, ParseResource>() {
                @Override
                public ParseResource apply(ParseObject resource) {
                    return new ParseResource(getString(resource.get(DEVICES_TABLE_ACCOUNT_NAME)),
                            getArea(resource.get(RESOURCES_TABLE_AREA)),
                            getString(resource.get(RESOURCES_TABLE_ALTERNATIVE_NAME)),
                            getString(resource.get(RESOURCES_TABLE_MASTER_DEVICE_ID)),
                            getString(resource.get(RESOURCES_TABLE_RESOURCE_NAME)),
                            getString(resource.get(RESOURCES_TABLE_ROOM_FEATURES)),
                            getString(resource.get(RESOURCES_TABLE_CURRENT_RESERVATION_TITLE)),
                            getString(resource.get(RESOURCES_TABLE_CURRENT_STATE)),
                            getString(resource.get(RESOURCES_TABLE_DOMAIN_NAME)),
                            getDateTime(resource.getUpdatedAt()),
                            getDateTime(resource.get(RESOURCES_TABLE_NEXT_EVENT_DATE)));
                }

                private String getArea(Object o) {
                    if (!(o instanceof ParseObject)) return "";
                    return Area.getOrFindArea(((ParseObject) o).getObjectId()).getName();
                }

                private String getString(Object value) {
                    return value != null ? value.toString() : "";
                }

                private DateTime getDateTime(Object value) {
                    return value == null || !(value instanceof Date) ? null : new DateTime(value);
                }

            });
        } catch (ParseException e) {
            throw new IOException(e);
        }
    }

    private List<ParseObject> getResources(ParseObject domain) throws ParseException {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(RESOURCES_TABLE);
        query.whereEqualTo(RESOURCES_TABLE_DOMAIN_NAME, domain.get(DOMAINS_TABLE_DOMAIN_NAME));
        return query.find();
    }

    private ParseObject getDomain(String accountName) throws ParseException {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(DOMAINS_TABLE);
         query.whereEqualTo(DOMAINS_TABLE_DOMAIN_NAME, Account.extractDomain(accountName));
        return Iterables.getFirst(query.find(), null);
    }

    private void handleParseError(String message, ParseException e) {
		Log.e(getClass().getName(), message, new Throwable(e));
	}

	/**
	 * POJO representing a resource, as seen by the server
	 */
	public static class ParseResource {

        public static final Function<? super ParseResource, String> RESOURCE_NAME_BY_PARSE_RESOURCE = new Function<ParseResource, String>() {
            @Nullable
            @Override
            public String apply(ParseResource parseResource) {
                return parseResource.resourceName;
            }
        };

        public final String accountName;
        public final String area;
        public final String alternativeName;
        public final String deviceId;
        public final String resourceName;
        public final String roomFeatures;
        public final String currentReservationTitle;
        public final String currentState;
        public final String domainName;
        public final DateTime lastUpdateDate;
        public final DateTime nextEventDate;

        private ParseResource(String accountName, String area, String alternativeName, String deviceId, String resourceName, String roomFeatures, String currentReservationTitle, String currentState, String domainName, DateTime lastUpdateDate, DateTime nextEventDate) {
            this.accountName = accountName;
            this.area = area;
            this.alternativeName = alternativeName;
            this.deviceId = deviceId;
            this.resourceName = resourceName;
            this.roomFeatures = roomFeatures;
            this.currentReservationTitle = currentReservationTitle;
            this.currentState = currentState;
            this.domainName = domainName;
            this.lastUpdateDate = lastUpdateDate;
            this.nextEventDate = nextEventDate;
        }
    }

	/**
	 * Generic configuration callback
	 * @param <X> Type of result that will be provided
	 */
	public interface ConfigurationCallback<X> {
		/** Invoked if the results where obtained correctly */
		void onSuccess(X result);
		/** Invoked if there was an error accessing the configuration */
		void onError(ConfigurationException exception);
	}

	/**
	 * Given an 'accountName' returns the list of products that can be purchased by this account.
	 * This method is asynchronous, the results are provided on the given callback.
	 */
	public void getProductCodes(String accountName, final ConfigurationCallback<List<String>> callback) {
		ParseCloud.callFunctionInBackground("products", ImmutableMap.of("accountName", accountName), new FunctionCallback<Map<String, Object>>() {
			public void done(Map<String, Object> response, ParseException e) {
				if (e != null) {
					callback.onError(new ConfigurationException("Unable to obtain the result from Parse", e));
				} else {
					callback.onSuccess((List<String>)response.get("products"));
				}
			}
		});
	}

	/**
	 * Possible modes for this device
	 */
	public enum DeviceMode {
		/** The device should be displayed as fully-functional */
		FULL,
		/** The device should be displayed as read only */
		READ_ONLY,
	}

	/**
	 * Registers the subscriptions for the account associated to this device
	 * @param productCodes product codes to register
	 * @param callback a callback that on success returns the {@link DeviceMode} for this device, based on the
	 *                 registered subscriptions
	 */
	public void registerProductCodes(List<String> productCodes, final ConfigurationCallback<DeviceMode> callback) {
		ParseCloud.callFunctionInBackground("registerSubscriptions", ImmutableMap.of("deviceId", deviceId, "productCodes", productCodes), new FunctionCallback<Map<String, Object>>() {
			@Override
			public void done(Map<String, Object> response, ParseException e) {
				if (e != null) {
					callback.onError(new ConfigurationException("Unable to obtain the result from Parse", e));
				} else {
					String status = (String)response.get("mode");
					callback.onSuccess("full".equals(status) ? DeviceMode.FULL : DeviceMode.READ_ONLY);
				}
			}
		});
	}

	/**
	 * Validates the mode for this device. Similar to {@link #registerProductCodes(java.util.List, com.tresmonos.calendar.Configuration.ConfigurationCallback)} but
	 * this method doesn't alter the existing subscriptions and it doesn't trigger a recalculation of the licensed devices.
	 * @param callback a callback that on success returns the {@link DeviceMode} for this device, based on the
	 *                 registered subscriptions
	 */
	public void validateDeviceMode(final ConfigurationCallback<DeviceMode> callback) {
		ParseCloud.callFunctionInBackground("validateSubscriptions", ImmutableMap.of("deviceId", deviceId), new FunctionCallback<Map<String, Object>>() {
			@Override
			public void done(Map<String, Object> response, ParseException e) {
				if (e != null) {
					callback.onError(new ConfigurationException("Unable to obtain the result from Parse", e));
				} else {
					String status = (String)response.get("mode");
					callback.onSuccess("full".equals(status) ? DeviceMode.FULL : DeviceMode.READ_ONLY);
				}
			}
		});
	}
}
