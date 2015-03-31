var moment = require('moment');
var _ = require('underscore');
var Devices = Parse.Object.extend("Devices");
var Account = Parse.Object.extend("Account");
var Products = Parse.Object.extend("Products");

/**
 * Returns a promise that returns the device associated to the given deviceId
 */
exports.findDevice = function(deviceId) {
	var deviceQuery = new Parse.Query(Devices);
	deviceQuery.equalTo("deviceId", deviceId);
	return deviceQuery.first().then(function(device) {
		if (!device) {
			return Parse.Promise.error("Unknown device");
		} else {
			return Parse.Promise.as(device);
		}
	});
}

/**
 * Returns a promise that returns the account given its account name
 */
exports.findAccount = function(accountName) {
	var accountQuery = new Parse.Query(Account);
	accountQuery.equalTo("email", accountName);
	return accountQuery.first().then(function(account) {
		if (!account) {
			return Parse.Promise.error("Unknown account");
		} else {
			return Parse.Promise.as(account);
		}
	});
};

/**
 * Function used to calculate the status of a device. It uses the
 * amount of time since the last update and the battery status
 * @param device a device to check
 * @returns {string} Either 'okay', 'warning' or 'bad'
 */
exports.calculateStatus = function(device) {
	console.log("Device: " + device.get("deviceId") + " resource: " + device.get("resourceName") +
	" lastUpdated: " + device.get("lastUpdated") + " battery: " + device.get("batteryLevel") +
	" charging: " + device.get("batteryIsCharging"));
	if (device.get("lastUpdated")) {
		var lastUpdateHours = moment().diff(device.get("lastUpdated"), "hours");
		if (lastUpdateHours < 24) {
			if (device.get("batteryIsCharging") && device.get("batteryLevel") > 0.5) {
				return "okay";
			} else {
				return "warning";
			}
		} else if (lastUpdateHours < 24 * 7) {
			return "bad";
		} else {
			return "delete";
		}
	} else {
		return "delete";
	}
};

exports.valueFormat = function(value) {
	return !!value ? value : "";
};

exports.dateFormat = function(date) {
	return !!date ? moment(date).format("YYYY-MM-DD h:mm") : "";
};

exports.queryString = function(parameters) {
	var qs = "";
	for(var key in parameters) {
		var value = parameters[key];
		qs += encodeURIComponent(key) + "=" + encodeURIComponent(value) + "&";
	}
	if (qs.length > 0){
		qs = qs.substring(0, qs.length-1); //chop off last "&"
	}
	return qs;
};

exports.validateRequiredParameters = function(request, response, parameterNames) {
	var missingParams = _.filter(parameterNames, function(parameterName) {
		return !request.params[parameterName];
	});
	if (!_.isEmpty(missingParams)) {
		response.error("Missing parameters: " + missingParams);
		return false;
	}
	return true;
}

/**
 * Returns a promise that returns the maximum number of allowed devices for the given account
 * @param accountName name of the account
 */
exports.maxLicensedDevices = function(accountName) {
	return exports.findAccount(accountName).then(function(account) {
		console.log("Calculating max devices for account: " + accountName);
		var productCodes = account.get("activeProductCodes");
		var productQuery = new Parse.Query(Products);
		productQuery.containedIn("code", productCodes);
		var freeProductsQuery = new Parse.Query(Products);
		freeProductsQuery.equalTo("isFree", true);
		var mainQuery = Parse.Query.or(productQuery, freeProductsQuery);
		return mainQuery.find();
	}).then(function (products) {
		// Add up all the devices and areas
		var maxDevices = 0;
		_.each(products, function (product) {
			console.log("Product: " + product.get("code"));
			maxDevices += (product.get("maxDevices") || 0);
		});
		console.log("Max devices: " + maxDevices);
		return Parse.Promise.as(maxDevices);
	});
}

/**
 * Returns a promise that returns "true" if the device is within license limits or "false" otherwise.
 */
exports.deviceLicenseStatus = function(device) {
	var deviceId = device.get("deviceId");
	var accountName = device.get("accountName");
	return exports.maxLicensedDevices(accountName).then(function(maxDevices) {
		// Find which devices are within this limit, in order of registration
		var devicesQuery = new Parse.Query(Devices);
		devicesQuery.limit(maxDevices);
		devicesQuery.descending("registrationTime");
		devicesQuery.equalTo("accountName", accountName);
		return devicesQuery.find();
	}).then(function (validDevices) {
		var found = _.find(validDevices, function(validDevice) {
			return validDevice.get("deviceId") == deviceId;
		});
		return Parse.Promise.as(!!found);
	});
};

