// Wavelength path topology overlay - client side
//
// This is the glue that binds our business logic (in wavelengthPathTopov.js)
// to the overlay framework.

(function () {
    'use strict';

    // injected refs
    var $log, tov, wlts;

    // internal state should be kept in the service module (not here)
    var multiSelected;

    // our overlay definition
    var overlay = {
        // NOTE: this must match the ID defined in AppUiTopovOverlay
        overlayId: 'wavelength-path',
        glyphId: 'fiber_switch',
        tooltip: 'Wavelength Path Overlay',

        activate: function () {
            $log.debug("Wavelength-path topology overlay ACTIVATED");
            wlts.startDisplay();
        },
        deactivate: function () {
            wlts.stopDisplay();
            $log.debug("Wavelength-path topology overlay DEACTIVATED");
        },

        // detail panel button definitions
        buttons: {
            wavelengthpath: {
                gid: 'fiber_switch',
                tt: 'Show Wavelength Path',
                cb: function (data) {
                    console.log(multiSelected);
                    $log.debug('Show Wavelength Path action invoked with data:', data);
                }
            }
        },

        // Key bindings for overlay buttons
        // NOTE: fully qual. button ID is derived from overlay-id and key-name
        keyBindings: {
            _keyOrder: [
                //'V',
            ]
        },

        hooks: {
        }
    };

    // invoke code to register with the overlay service
    angular.module('ovWavelengthPathTopov')
        .run(['$log', 'TopoOverlayService', 'WavelengthPathTopovService',

        function (_$log_, _tov_, _wlts_) {
            $log = _$log_;
            tov = _tov_;
            wlts = _wlts_;
            tov.register(overlay);
        }]);

}());
