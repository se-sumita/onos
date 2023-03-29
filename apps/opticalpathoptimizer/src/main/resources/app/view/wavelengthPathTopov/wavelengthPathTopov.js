(function () {
    'use strict';

    // injected refs
    var $log, fs, flash, wss;

    // constants
    var displayStart = 'wlpathTopovDisplayStart',
        displayUpdate = 'wlpathTopovDisplayUpdate',
        displayStop = 'wlpathTopovDisplayStop';

    var selectedWavelengthIds = [null, null];

    // === ---------------------------
    // === Helper functions

    function sendDisplayStart(wpid0, wpid1) {
        if (wpid1 == null) {
            flash.flash('Showing wavelength-path id:' + wpid0);
        } else {
            flash.flash('Showing wavelength-path id:' + wpid0 + ', ' + wpid1);
        }

        wss.sendEvent(displayStart, {
            wpid0: wpid0,
            wpid1: wpid1
        });
    }

    function sendDisplayUpdate(what) {
        wss.sendEvent(displayUpdate, {
            id: what ? what.id : ''
        });
    }

    function sendDisplayStop() {
        wss.sendEvent(displayStop);
    }

    // === ---------------------------
    // === Main API functions

    function startDisplay() {
        var query = document.location.hash.split('?', 2)[1]
        var wpid0 = null, wpid1 = null;
        if (query) {
            var params = query.split('&');
            for (var i = 0; i < params.length; i++) {
                var param = params[i].split('=', 2);
                if (param[0] == "wpid0") {
                    wpid0 = param[1];
                } else if (param[0] == "wpid1") {
                    wpid1 = param[1];
                }
            }
        }
        if (wpid0 == null && wpid1 != null) {
            wpid0 = wpid1;
            wpid1 = null;
        } else if (wpid0 != null && wpid0 == wpid1) {
            wpid1 = null;
        }
        console.log("wpid0", wpid0, "wpid1", wpid1);

        if ((selectedWavelengthIds[0] != wpid0
                || selectedWavelengthIds[1] != wpid1)) {
            selectedWavelengthIds = [wpid0, wpid1];
            if (wpid0 == null) {
                stopDisplay();
            } else {
                setTimeout(function() {
                    sendDisplayStart(wpid0, wpid1);
                }, 1000);
            }
        }
    }

    function updateDisplay(m) {
//        sendDisplayUpdate(m);
    }

    function stopDisplay() {
        selectedWavelengthIds = [null, null];
        sendDisplayStop();
        //flash.flash('Canceling wavelength-path');
        return true;
    }

    // === ---------------------------
    // === Module Factory Definition

    angular.module('ovWavelengthPathTopov', [])
        .factory('WavelengthPathTopovService',
        ['$log', 'FnService', 'FlashService', 'WebSocketService',

        function (_$log_, _fs_, _flash_, _wss_) {
            $log = _$log_;
            fs = _fs_;
            flash = _flash_;
            wss = _wss_;

            return {
                startDisplay: startDisplay,
                updateDisplay: updateDisplay,
                stopDisplay: stopDisplay
            };
        }]);
}());
