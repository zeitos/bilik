var authController = require('cloud/controllers/authController.js');
var _ = require('underscore');
var Account = Parse.Object.extend("Account");
var Mailgun = require('mailgun');

/**
 * Returns the list of delegates for the logged user
 */
exports.list = function(req, res) {
    authController.ensureAuthenticated(req, res, function(req, res) {
        var delegateAccounts = req.account.get("delegateAccounts");
        var accountQuery = new Parse.Query(Account);
        accountQuery.containedIn("email", delegateAccounts);
        accountQuery.find().then(function(accounts) {
            var data = _.map(accounts, function(account) {
                return {
                    name: account.get("fullName"),
                    email: account.get("email"),
                    role: "ADMIN"
                };
            });
            res.send(data);
        });
    });
};

/**
 * Invites someone to collaborate. Expects:
 * {
 *      email: email-address
 * }
 */
exports.invite = function(req, res) {
    authController.ensureAuthenticated(req, res, function(req, res) {
        // Add the delegate to the list
        var promise = new Parse.Promise.as();
        var delegates = req.account.get("delegateAccounts");
        var newEmail = req.body.email;
        delegates = delegates || [];
        if (!_.contains(delegates, newEmail)) {
            delegates.push(newEmail);
            promise = promise.then(function() {
                console.log("Setting delegate: " + JSON.stringify(delegates));
                return req.account.save({
                    delegateAccounts: delegates
                });
            });
        }
        // Add the delegate as a pending account
        var accountQuery = new Parse.Query(Account);
        accountQuery.equalTo("email", newEmail);
        promise = promise.then(function() {
            accountQuery.first().then(function(account) {
                if (!account) {
                    console.log("Adding account: " + newEmail);
                    var account = new Account();
                    return account.save({
                        fullName: "?",
                        email: newEmail
                    });
                } else {
                    return new Parse.Promise.as(account);
                }
            });
        });
        // Then send the email
        promise = promise.then(function() {
            var sourceEmail = req.account.get("email");
            var sourceName = req.account.get("fullName")
            var body = "<p>" + sourceName + " has invited you to collaborate maintaining the Bilik system for your organization</p>";
            body += "<p>To start collaborating, please click the following link: <a href='http://dashboard.bilikpro.com'>Bilik Dashboard</a></p>";
            body += "<p>For more information about Bilik, please visit <a href='http://www.bilikpro.com'>Bilikpro.com</a></p>";
            body += "<p>Or contact the original sender of this email: <a href='mailto://'" + sourceEmail + ">" + sourceEmail + "</a></p>.";
            body += "<img src='http://dashboard.bilikpro.com/img/BilikProLogoSmall.png'/>";
            return Mailgun.sendEmail({
                to: newEmail,
                from: "noreply@bilik.com",
                subject: "Invitation from " + sourceName + " (" + sourceEmail + ") to collaborate on Bilik",
                html: body
            })
        });
        promise = promise.then(function() {
            res.send({});
        });
    });
}

/**
 * Removes a delegate from the list of delegates for this account.
 * No emails or accounts are removed.
 */
exports.remove = function(req, res) {
    authController.ensureAuthenticated(req, res, function(req, res) {
        // Remove the delegate from the list
        var promise = new Parse.Promise.as();
        var delegates = req.account.get("delegateAccounts");
        var newEmail = req.body.email;
        delegates = delegates || [];
        if (_.contains(delegates, newEmail)) {
            delegates = _.filter(delegates, function(delegate) {
                return delegate !== newEmail;
            });
            promise = promise.then(function() {
                return req.account.save({
                    delegateAccounts: delegates
                });
            });
        }
        promise = promise.then(function() {
            res.send({});
        });
    });
}