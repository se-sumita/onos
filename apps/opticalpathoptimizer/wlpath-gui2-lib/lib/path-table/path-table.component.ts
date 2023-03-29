/*
 * Copyright 2020-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {Component, OnInit, OnDestroy, Inject} from '@angular/core';
import {
    WebSocketService, LogService, FnService,
    SortDir, TableBaseImpl, TableResponse
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';
import { HttpClient } from '@angular/common/http';

/**
 * Model of the wavelength path returned from the WebSocket
 */
export interface WavelengthPath {
    id: string;
    tp1: string;
    path: string;
    tp2: string;
    name: string;
    channel: number;
    frequency: string;
    qvalue: number;
    qmargin: number;
    submitId: number;
    redundancy: string;
    state: string;
    wpid0: number;
    wpid1: number;
}

/**
 * Model of the response from the WebSocket
 */
export interface WavelengthPathTableResponse extends TableResponse {
    wavelengthPaths: WavelengthPath[];
}


@Component({
    selector: 'path-table',
    templateUrl: './path-table.component.html',
    styleUrls: [
        './path-table.component.css',
        './path-table.theme.css',
        '../../../../../web/gui2-fw-lib/lib/widget/table.css',
        '../../../../../web/gui2-fw-lib/lib/widget/table.theme.css'
    ]
})
export class WavelengthPathTableComponent extends TableBaseImpl implements OnInit, OnDestroy {
    selectedModel: any = undefined;

    constructor(
        protected log: LogService,
        protected fs: FnService,
        protected wss: WebSocketService,
    ) {
        super(fs, log, wss, 'wavelengthPath');
        this.log.debug('WavelengthPathTableComponent constructed');

        this.responseCallback = this.wavelengthPathResponseCb;

        this.sortParams = {
            firstCol: 'id',
            firstDir: SortDir.desc,
            secondCol: 'name',
            secondDir: SortDir.asc,
        };
        this.log.debug('WavelengthPathTableComponent constructed');
    }

    ngOnInit() {
        this.init();
        this.log.debug('WavelengthPathTableComponent initialized');
    }

    ngOnDestroy() {
        this.destroy();
        this.log.debug('WavelengthPathTableComponent destroyed');
    }

    wavelengthPathResponseCb(data: WavelengthPathTableResponse) {
        this.log.debug('Wavelength path response received for ', data.wavelengthPaths.length, 'paths');
    }

    rowSelection(event: any, selRow: any) {
        this.log.debug('Row ', this.selId, 'selected');
    }
}
