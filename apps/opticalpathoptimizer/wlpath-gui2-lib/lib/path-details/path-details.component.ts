
import { Component, Input, OnInit, OnDestroy, OnChanges } from '@angular/core';
import { trigger, state, style, animate, transition } from '@angular/animations';
import {
    FnService,
    IconService,
    LogService,
    DetailsPanelBaseImpl,
    WebSocketService,
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';

/**
 * ONOS GUI -- Wavelength Path Details Component extends TableBaseImpl
 * The details view when an wavelength path row is clicked from the
 * Wavelength Path view
 *
 * This is expected to be passed an 'id' and it makes a call
 * to the WebSocket with an wavelengthPathDetailsRequest, and gets
 * back an wavelengthPathDetailsResponse.
 *
 * The animated fly-in is controlled by the animation below
 * The wavelengthPathDetailsState is attached to
 * wavelength-path-details-panel and is false (flies out) when id=''
 * and true (flies in) when id has a value
 */
@Component({
    selector: 'onos-wavelength-path-details',
    templateUrl: './path-details.component.html',
    styleUrls: ['./path-details.component.css',
      '../../../../../web/gui2-fw-lib/lib/widget/panel.css',
      '../../../../../web/gui2-fw-lib/lib/widget/panel-theme.css'
    ],
    animations: [
        trigger('wavelengthPathDetailsState', [
            state('true', style({
                transform: 'translateX(-100%)',
                opacity: '100'
            })),
            state('false', style({
                transform: 'translateX(0%)',
                opacity: '0'
            })),
            transition('0 => 1', animate('100ms ease-in')),
            transition('1 => 0', animate('100ms ease-out'))
        ])
    ]
})
export class WavelengthPathDetailsComponent extends DetailsPanelBaseImpl implements OnInit, OnDestroy, OnChanges {
    @Input() id: string;

    constructor(
        protected fs: FnService,
        protected log: LogService,
        protected is: IconService,
        protected wss: WebSocketService
    ) {
        super(fs, log, wss, 'wavelengthPath');
    }

    ngOnInit() {
        this.init();
        this.log.debug('Wavelength Path Table Details Component initialized:', this.id);
    }

    /**
     * Stop listening to alarmTableDetailsResponse on WebSocket
     */
    ngOnDestroy() {
        this.destroy();
        this.log.debug('Wavelength Path Table Details Component destroyed');
    }

    /**
     * Details Panel Data Request on row selection changes
     * Should be called whenever id changes
     * If id is empty, no request is made
     */
    ngOnChanges() {
        if (this.id === '') {
            return '';
        } else {
            const query = {
                'id': this.id
            };
            this.requestDetailsPanelData(query);
        }
    }
}
