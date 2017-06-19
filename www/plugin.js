
var exec = require('cordova/exec');

var PLUGIN_NAME = 'HeartRatePlugin';

var HeartRatePlugin = {
  pluginInitialize: function(cb) {
    exec(cb, null, PLUGIN_NAME, 'pluginInitialize', []);
  },
};

module.exports = HeartRatePlugin;
