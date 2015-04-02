var authController = require('cloud/controllers/authController.js');
var _ = require('underscore');
var Account = Parse.Object.extend("Account");

var convertSubscriptions = function(areas) {
    var subscriptions = {};
    _.each(areas, function(v) {
        subscriptions[v] = {
            area: v,
            isSubscribed: true
        };
    });
    return subscriptions;
}

/**
 * Generates a JSON object containing all email subscriptions for the logged user.
 * {
 *      areas-name: {
 *          area: area-name
 *          isSubscribed: true/false
 *      }
 * }
 */
exports.subscriptions = function(req, res) {
    authController.ensureAuthenticated(req, res, function(req, res) {
        res.send(convertSubscriptions(req.account.get("subscribedAreaNames")));
    });
};

/**
 * Adds or removes a subscription to an area to the logged user.
 * Expected to receive:
 * {
 *      area-name: area-name,
 *      isSubscribed: true/false
 * }
 */
exports.updateSubscription = function(req, res) {
    authController.ensureAuthenticated(req, res, function(req, res) {
        console.log("Body: " + JSON.stringify(req.body));
        var subscriptions = req.account.get("subscribedAreaNames");
        console.log("Before: " + JSON.stringify(subscriptions));
        if (req.body.isSubscribed && !_.contains(subscriptions, req.body.areaName)) {
            subscriptions = subscriptions || [];
            subscriptions.push(req.body.areaName);
        } else {
            subscriptions = _.filter(subscriptions, function(v) { return v !== req.body.areaName; });
        }
        console.log("After: " + JSON.stringify(subscriptions));
        req.account.save({
            subscribedAreaNames: subscriptions
        }, {
            success: function() {
                res.send(convertSubscriptions(subscriptions));
            },
            error: function(account, error) {
                return res.status(401).send({ message: 'Invalid session' });
            }
        });
    });
}