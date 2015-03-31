var jwt = require('cloud/jwt');
var config = require('cloud/config');
var common = require('cloud/common');
var moment = require('moment');

/**
 * Generate JSON Web Token
 */
function createToken(account) {
	var payload = {
		sub: account.id,
		iat: moment().unix(),
		exp: moment().unix() + (24 * 60 * 60 * 14)
	};
	return jwt.encode(payload, config.TOKEN_SECRET);
}

/**
 * Login Required Middleware
 */
exports.ensureAuthenticated = function (req, res, next) {
	if (!req.headers.authorization) {
		return res.status(401).send({ message: 'Please make sure your request has an Authorization header' });
	}
	var token = req.headers.authorization.split(' ')[1];
	var payload = jwt.decode(token, config.TOKEN_SECRET);
	if (payload.exp <= moment().unix()) {
		return res.status(401).send({ message: 'Token has expired' });
	}
	var Account = Parse.Object.extend("Account");
	var query = new Parse.Query(Account);
	query.get(payload.sub, {
		success: function(account) {
			req.account = account;
			console.log("ensureAuthenticated account: " + JSON.stringify(req.account));
			next(req, res);
		},
		error: function(account, error) {
			return res.status(401).send({ message: 'Invalid session' });
		}
	});
};

/**
 * Exchage the Google access token for a web token
 */
exports.authenticateGoogle = function(req, res) {
	var accessTokenUrl = 'https://accounts.google.com/o/oauth2/token';
	var peopleApiUrl = 'https://www.googleapis.com/plus/v1/people/me/openIdConnect';
	var params = {
		grant_type: 'authorization_code',
		code: req.body.code,
		client_id: req.body.clientId,
		client_secret: config.GOOGLE_SECRET,
		redirect_uri: req.body.redirectUri
	};

	// Step 1. Exchange authorization code for access token
	Parse.Cloud.httpRequest({
		method: 'POST',
		url: accessTokenUrl,
		headers: {
			'Content-type': 'application/x-www-form-urlencoded'
		},
		body: common.queryString(params),
		success: function(httpResponse) {
			// Step 2. Retrieve profile information about the current user.
			var accessToken = httpResponse.data.access_token;
			var headers = { Authorization: 'Bearer ' + accessToken };
			Parse.Cloud.httpRequest({
				method: 'GET',
				url: peopleApiUrl,
				headers: {
					'Content-Type': 'application/json;charset=utf-8',
					'Authorization': 'Bearer ' + accessToken
				},
				success: function(httpResponse) {
					// Step 3. Find user and return token
					var user = httpResponse.data;
					var Account = Parse.Object.extend("Account");
					var query = new Parse.Query(Account);
					query.equalTo("googleId", user.sub);
					query.first().then(function(account) {
						// Step 4. Update the information and return the token
						if (!account) {
							account = new Account();
						}
						account.save({
							fullName: user.name,
							email: user.email,
							googleId: user.sub
						}, {
							success: function(account) {
								var token = createToken(account);
								res.send({ token: token });
							},
							error: function(account, error) {
								return res.status(500).send(error.message);
							}
						});
					}, function(error) {
						return res.status(500).send(error.message);
					});
				},
				error: function(httpResponse) {
					return res.status(httpResponse.status).send(httpResponse.data);
				}
			});
		},
		error: function(httpResponse) {
			return res.status(httpResponse.status).send(httpResponse.data);
		}
	});
};

/**
 * Obtains information about the logged account
 */
exports.me = function(req, res) {
	exports.ensureAuthenticated(req, res, function(req, res) {
		res.send(req.account);
	});
};
 