var authController = require('cloud/controllers/authController.js');
var _ = require('underscore');
var Devices = Parse.Object.extend("Devices");
var Resources = Parse.Object.extend("Resources");
var Account = Parse.Object.extend("Account");
var Products = Parse.Object.extend("Products");
var common = require('cloud/common.js');

/**
 * Visits all devices and their corresponding resource and area, calling the provided
 * function with the parameters (device, resource, area).
 * @param visitor Visitor that will be notified of each device.
 * @return a promise. By the time 'success' is called the visit will be completed.
 */
exports.visitDevices = function(filterByAccount, visitor) {
	var promise;
	var deviceQuery = new Parse.Query(Devices);
	deviceQuery.descending("registrationTime");

	if (filterByAccount) {
		var accountQuery = new Parse.Query(Account);
		accountQuery.equalTo("delegateAccounts", filterByAccount.get("email"));
		promise = accountQuery.find().then(function(accounts) {
			var accountNames = _.map(accounts, function(account) {
				return account.get("email");
			});
			accountNames = accountNames || [];
			accountNames.push(filterByAccount.get("email"));
			deviceQuery.containedIn("accountName", accountNames);
			console.log("Limiting query to the following emails: " + JSON.stringify(accountNames));
			return deviceQuery.find();
		});
	} else {
		promise = device.find();
	}

	return promise.then(function(devices) {
		var promises = [];
		_.each(devices, function(device) {
			var resourceQuery = new Parse.Query(Resources);
			resourceQuery.equalTo("resourceName", device.get("resourceName"));
			resourceQuery.equalTo("domainName", device.get("domainName"));
			resourceQuery.include("area");
			promises.push(resourceQuery.first().then(function(resource) {
				if (!resource) {
					resource = new Resource();
				}
				visitor(device, resource, resource.get("area"));
			}));
		});
		return Parse.Promise.when(promises);
	});
}

/**
 * Generates a JSON object containing all the information needed to complete the "devices" page of the web-site.
 * This method is supposed to be used as a controller for the corresponding Express page.
 */
exports.list = function(req, res) {
	authController.ensureAuthenticated(req, res, function(req, res) {
		var resultDeviceList = [];
		exports.visitDevices(req.account, function(device, resource, area) {
			var areaName = area ? area.get("name") : "Unknown";
			resultDeviceList.push({
				resourceName: device.get("resourceName"),
				resourceStatus: resource.get("currentState"),
				deviceId: device.get("deviceId"),
				batteryLevel: device.get("batteryLevel") || 0,
				batteryIsCharging: device.get("batteryIsCharging"),
				lastUpdate: common.dateFormat(device.get("lastUpdated")),
				area: areaName,
				status: device.get("lastStatus") ? device.get("lastStatus") : "bad",
				licensed: device.get("licenseStatus")
			});
		}).then(function () {
			res.send(resultDeviceList);
		});
	});
};

