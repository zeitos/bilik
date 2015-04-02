var express = require('express');
var moment = require('moment');
var app = express();
var common = require('cloud/common');

// Controllers
var authController = require('cloud/controllers/authController.js');
var devicesController = require('cloud/controllers/devicesController.js');
var areasController = require('cloud/controllers/areasController.js');
var delegatesController = require('cloud/controllers/delegatesController.js');

// Global app configuration section
app.set('views', 'cloud/views');  
app.set('view engine', 'ejs');    
app.use(express.bodyParser());    
app.use(express.methodOverride());

// You can use app.locals to store helper methods so that they are accessible
// from templates.

// Date formatter
app.locals.formatTime = function(time) {
	return !!time ? moment(time).format('YYYY-MM-DD h:mm') : "";
};

// Value formatter
app.locals.formatValue = function(value) {
	return !!value ? value : "";
}

// Background color for each device based on their status
app.locals.statusBgColor = function(device) {
	var currentStatus = common.calculateStatus(device);
	var statusBgColor = currentStatus === "ok" ? "#40FF40" :
  		currentStatus === "warning" ? "#FFFF40" : "#FF4040";
  	return statusBgColor;
}

app.post('/auth/google', authController.authenticateGoogle);
app.get('/api/me', authController.me);
app.get('/api/devices', devicesController.list);
app.get('/api/subscriptions', areasController.subscriptions);
app.post('/api/updateSubscription', areasController.updateSubscription);
app.get('/api/delegates', delegatesController.list);
app.post('/api/inviteDelegate', delegatesController.invite);
app.post('/api/removeDelegate', delegatesController.remove);
app.listen();