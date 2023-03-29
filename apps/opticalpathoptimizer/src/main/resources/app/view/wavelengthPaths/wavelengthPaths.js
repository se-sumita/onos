// js for wavelength path app table view
(function () {
    'use strict';

    // injected refs
    var $log, $scope, fs, wss, is, ns;

    function goTopology(path, isGroup) {
        $log.info('Jump to topology with wavelength path overlay', path.id);

        var d = {}
        if (isGroup) {
            d.wpid0 = path.wpid0;
            if (path.wpid1 > 0) {
                d.wpid1 = path.wpid1;
            }
        } else {
            d.wpid0 = path.id;
        }
        d.overlayId = "wavelength-path";
        ns.navTo('topo', d);
    }

    function showWavelengthPathTopology() {
        if ($scope.selId == null) return;
        goTopology($scope.selectedRow, false);
    }

    function showWavelengthPathsTopology() {
        if ($scope.selId == null) return;
        goTopology($scope.selectedRow, true);
    }

    function jumpTopology($event, row, isGroup) {
        $event.stopPropagation();
        goTopology(row, isGroup);
    }

    angular.module('ovWavelengthPaths', [])
        .controller('OvWavelengthPathsCtrl',
        ['$log', '$scope', 'TableBuilderService',
            'FnService', 'WebSocketService', 'IconService', 'NavService',

            function (_$log_, _$scope_, tbs, _fs_, _wss_, _is_, _ns_) {
                $log = _$log_;
                $scope = _$scope_;
                fs = _fs_;
                wss = _wss_;
                is = _is_;
                ns = _ns_

                //var handlers = {};
                $scope.panelDetails = {};
                $scope.selectedRow = null;

                $scope.topoTip = 'Show selected path on topology view';
                $scope.topoTip2 = 'Show selected redundancy paths on topology view';
                $scope.canShowTopology = function canShowTopology() {
                    return $scope.selId != null;
                };
                $scope.showWavelengthPathTopology = showWavelengthPathTopology;
                $scope.showWavelengthPathsTopology = showWavelengthPathsTopology;
                $scope.jumpTopology = jumpTopology;

                // details response handler
                //handlers[detailsResp] = respDetailsCb;
                //wss.bindHandlers(handlers);

                // custom selection callback
                function selCb($event, row) {
                    if ($scope.selId) {
                        $scope.selectedRow = row;
                        //wss.sendEvent(detailsReq, { id: row.id });
                    } else {
                        $scope.selectedRow = null;
                        //$scope.hidePanel();
                    }
                    $log.debug('Got a click on:', row);
                }

                // TableBuilderService creating a table for us
                tbs.buildTable({
                    scope: $scope,
                    tag: 'wavelengthPath',
                    selCb: selCb
                });

                // cleanup
                $scope.$on('$destroy', function () {
                    //wss.unbindHandlers(handlers);
                    $log.log('OvWavelengthPathsCtrl has been destroyed');
                });

                $log.log('OvWavelengthPathsCtrl has been created');
            }])
}());
