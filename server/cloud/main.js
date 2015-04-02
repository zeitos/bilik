require('cloud/app.js');

var _ = require('underscore');
var moment = require('moment');
var Mailgun = require('mailgun');
var common = require('cloud/common.js');
var devicesController = require('cloud/controllers/devicesController.js');

var Devices = Parse.Object.extend('Devices');
var Resources = Parse.Object.extend('Resources');
var Account = Parse.Object.extend('Account');
var Products = Parse.Object.extend('Products');

Mailgun.initialize('...', '...');

/**
 * Returns a promise that updates the status of all devices and resources, including
 * last registration time, deleting old resources, updating license status, etc.
 */
var statusUpdate = function() {
	var query = new Parse.Query(Devices);
	var resourceNamesFound = [];
	return query.find().then(function(devices) {
		var promises = [];
		_.each(devices, function (device) {
			// Adds a promise that updates the "lastStatus" field
			var lastStatus = device.get("lastStatus");
			var currentStatus = common.calculateStatus(device);
			var resourceName = device.get("resourceName");
			console.log("Device: " + device.get("deviceId") + " status: " + currentStatus);
			if (currentStatus !== "delete") {
				resourceNamesFound.push(resourceName);
			}
			if (lastStatus != currentStatus) {
				if (currentStatus === "delete") {
					promises.push(device.destroy());
				} else {
					device.set("lastStatus", currentStatus);
					device.set("lastStatusTime", moment().format());
					promises.push(device.save());
				}
			}

			// Adds a promise that update the "licensed" field
			promises.push(common.deviceLicenseStatus(device).then(function (licenseStatus) {
				device.set("licenseStatus", licenseStatus);
				return device.save();
			}));
		});
		return Parse.Promise.when(promises);
	}).then(function() {
		var query = new Parse.Query(Resources);
		query.notContainedIn("resourceName", resourceNamesFound);
		return query.find();
	}).then(function(resourcesMissingDevices) {
		var promises = [];
		_.each(resourcesMissingDevices, function(resource) {
			console.log("Deleting resource " + resource.get("resourceName") + " because there are no devices");
			promises.push(resource.destroy());
		});
		return Parse.Promise.when(promises);
	});
}
/**
 * Background job that update the 'lastStatus' and 'lastStatusTime' of each device based on their
 * last report and battery status. This also removes devices and resources if there is no report for
 * more than a week.
 */
Parse.Cloud.job("statusCheck", function(request, status) {
	statusUpdate().then(function() {
		status.success("Check successful.");
	}, function() {
		status.error("Uh oh, something went wrong.");
	});
});

/**
 * Sends an email with the state of each device for each of the subscribed accounts
 */
Parse.Cloud.job("sendEmails", function(request, status) {
	var accountQuery = new Parse.Query(Account);
	accountQuery.exists("subscribedAreaNames");
	accountQuery.find().then(function(accounts) {
		var promises = [];
		_.each(accounts, function(account) {
			var subscribedAreas = account.get("subscribedAreaNames");
			var result = "<p>Status of all devices:</p><br/><table style='border-collapse:collapse;' border='1'><tr>" +
				"<th style='white-space:nowrap;'>Account</th>" +
				"<th style='white-space:nowrap;'>Area</th>" +
				"<th style='white-space:nowrap;'>Resource Name</th>" +
				"<th style='white-space:nowrap;'>Device Id</th>" +
				"<th style='white-space:nowrap;'>Is Charging</th>" +
				"<th style='white-space:nowrap;'>Battery Level</th>" +
				"<th style='white-space:nowrap;'>Current State</th>" +
				"<th style='white-space:nowrap;'>Last Update</th>" +
				"</tr>";
			promises.push(devicesController.visitDevices(account, function (device, resource, area) {
				// Ignore those areas that we are not subscribed
				var currentStatus = common.calculateStatus(device);
				var statusBgColor = currentStatus === "okay" ? "#40FF40" :
					currentStatus === "warning" ? "#FFFF40" : "#FF4040";
				var statusColor = currentStatus === "okay" ||
				currentStatus === "warning" ? "#000000" : "#FFFFFF";
				var areaName = area ? area.get("name") : "Unknown";
				console.log("Filtering subscribedAreas: " + JSON.stringify(subscribedAreas) + " (checking: " + areaName + ")");
				if (_.contains(subscribedAreas, areaName)) {
					result += "<tr style='color: " + statusColor + "; background: " + statusBgColor + ";'>" +
					"<td>" + common.valueFormat(device.get("accountName")) + "</td>" +
					"<td>" + common.valueFormat(areaName) + "</td>" +
					"<td>" + common.valueFormat(device.get("resourceName")) + "</td>" +
					"<td>" + common.valueFormat(device.get("deviceId")) + "</td>" +
					"<td>" + common.valueFormat(device.get("batteryIsCharging")) + "</td>" +
					"<td>" + common.valueFormat(device.get("batteryLevel")) + "</td>" +
					"<td>" + common.valueFormat(resource.get("currentState")) + "</td>" +
					"<td>" + common.dateFormat(device.get("lastUpdated")) + "</td>" +
					"</tr>";
				}
			}).then(function() {
				result += "</table>";
				return Mailgun.sendEmail({
					to: account.get("email"),
					from: "noreply@bilik.com",
					subject: "Device status report",
					html: result
				})
			}));
		});
		return Parse.Promise.when(promises);
	}).then(function() {
		status.success("Check successful.");
	}, function() {
		status.error("Uh oh, something went wrong.");
	});
});

/**
 * Registers the current set of purchased subscriptions. Given them it immediately updates the
 * list of licensed devices and returns whether the device reporting these subscriptions is
 * in the set of valid devices
 * @param deviceId id of the device reporting the subscription
 * @param subscriptionCodes the list of subscriptions currently valid for this device.
 * @return an object with:
 *    - mode: whether the device should work as 'readOnly' or 'full'
 */
Parse.Cloud.define("registerSubscriptions", function(request, response) {
	if (!common.validateRequiredParameters(request, response, ['deviceId', 'productCodes'])) {
		return;
	}
	var deviceId = request.params.deviceId;
	var productCodes = request.params.productCodes;
	common.findDevice(deviceId).then(function(device) {
		return common.findAccount(device.get("accountName")).then(function (account) {
			account.set("activeProductcodes", productCodes);
			return account.save();
		}).then(function() {
			return Parse.Promise.as(device);
		});
	}).then(function(device) {
		statusUpdate().then(function() {
			response.success({
				mode: device.get("licenseStatus") ? "full" : "readOnly"
			});
		});
	}, function(error) {
		response.error(error);
	});
});

/**
 * Given a device and the account registered on that device, this method validates the purchased
 * subscriptions and determines if this particular device is in the set of disallowed devices, or
 * if it should work as fully functional.
 * @param deviceId id of the device reporting the subscription
 * @return an object with:
 *    - mode: whether the device should work as 'readOnly' or 'full'
 */
Parse.Cloud.define("validateSubscriptions", function(request, response) {
	if (!common.validateRequiredParameters(request, response, ['deviceId'])) {
		return;
	}
	return common.findDevice(request.params.deviceId).then(function (device) {
		response.success({
			mode: device.get("licenseStatus") ? "full" : "readOnly"
		})
	}, function(error) {
		response.error(error);
	});
});

/**
 * Given an 'accountName' it returns the products that are available for purchase for
 * the given account.
 * @return an object with:
 *    - productCodes: an array with the list of product codes
 */
Parse.Cloud.define("products", function(request, response) {
	if (!common.validateRequiredParameters(request, response, ['accountName'])) {
		return;
	}
	var accountName = request.params.accountName;

	var publicProductsQuery = new Parse.Query(Products);
	var privateProductsQuery = new Parse.Query(Products);
	publicProductsQuery.doesNotExist('privateForAccountName');
	publicProductsQuery.notEqualTo('isFree', true);
	privateProductsQuery.equalTo('privateForAccountName', accountName);
	privateProductsQuery.notEqualTo('isFree', true);

	var mainQuery = Parse.Query.or(publicProductsQuery, privateProductsQuery);
	mainQuery.find().then(function(products) {
		var productCodes = _.map(products, function(product) {
			return product.get('code');
		});
		response.success({
			products: productCodes
		});
	});
});
