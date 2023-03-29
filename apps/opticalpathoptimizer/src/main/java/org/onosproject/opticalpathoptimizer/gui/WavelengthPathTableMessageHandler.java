package org.onosproject.opticalpathoptimizer.gui;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.Path;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.openroadm.model.WavelengthPath;
import org.onosproject.opticalpathstore.WavelengthPathStore;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Wavelength-path Table-View message handler.
 */
public class WavelengthPathTableMessageHandler extends UiMessageHandler {

    private static final String WLPATH_TABLE_DATA_REQ = "wavelengthPathDataRequest";
    private static final String WLPATH_TABLE_DATA_RESP = "wavelengthPathDataResponse";
    private static final String WLPATH_TABLES = "wavelengthPaths";

    private static final String WLPATH_TABLE_DETAIL_REQ = "wavelengthPathDetailsRequest";
    private static final String WLPATH_TABLE_DETAIL_RESP = "wavelengthPathDetailsResponse";
    private static final String DETAILS = "details";

    private static final String NO_ROWS_MESSAGE = "No items found";

    private static final String ID = "id";
    private static final String TP1 = "tp1";
    private static final String PATH = "path";
    private static final String TP2 = "tp2";
    private static final String NAME = "name";
    private static final String FREQUENCY = "frequency";
    private static final String CHANNEL = "channel";
    private static final String QVALUE = "qvalue";
    private static final String QMARGIN = "qmargin";
    private static final String SUBMIT_ID = "submitId";
    private static final String REDUNDANCY = "redundancy";
    private static final String STATE = "state";
    private static final String WPID0 = "wpid0";
    private static final String WPID1 = "wpid1";
    private static final String RESULT = "result";

    private static final String[] COLUMN_IDS = {
            ID, TP1, PATH, TP2, NAME, CHANNEL, FREQUENCY, QVALUE, QMARGIN,
            SUBMIT_ID, REDUNDANCY, STATE, WPID0, WPID1
    };

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);
    private final WavelengthPathStore pathStore = DefaultServiceDirectory.getService(WavelengthPathStore.class);

    DecimalFormat frequencyFormat = new DecimalFormat("#.00#");

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new WavelengthPathTableDataRequestHandler(),
                new WlpathTableDetailRequestHandler()
        );
    }

    // handler for wavelength path table requests
    private final class WavelengthPathTableDataRequestHandler extends TableRequestHandler {

        private WavelengthPathTableDataRequestHandler() {
            super(WLPATH_TABLE_DATA_REQ, WLPATH_TABLE_DATA_RESP, WLPATH_TABLES);
        }

        @Override
        protected String[] getColumnIds() {
            return COLUMN_IDS;
        }

        @Override
        protected String noRowsMessage(ObjectNode payload) {
            return NO_ROWS_MESSAGE;
        }

        @Override
        protected void populateTable(TableModel tm, ObjectNode payload) {
            Collection<WavelengthPath> paths = getItems();
            for (WavelengthPath path : paths) {
                populateRow(tm.addRow(), path);
            }
        }

        private void populateRow(TableModel.Row row, WavelengthPath path) {
            List<WavelengthPath> paths = pathStore.findByGroupId(path.groupId());
            long wpid0 = (paths.size() > 0) ? paths.get(0).id() : 0;
            long wpid1 = (paths.size() > 1) ? paths.get(1).id() : 0;
            row.cell(ID, path.id())
                    .cell(TP1, toDevicePortName(path.src()))
                    .cell(PATH, toRoadmPath(path.path()))
                    .cell(TP2, toDevicePortName(path.dst()))
                    .cell(NAME, Strings.isNullOrEmpty(path.name()) ? "-" : path.name())
                    .cell(CHANNEL, path.frequencyId())
                    .cell(FREQUENCY, frequencyFormat.format(path.signal().centralFrequency().asTHz()))
                    .cell(QVALUE, path.qValue())
                    .cell(QMARGIN, path.qValue() - path.qThreshold())
                    .cell(SUBMIT_ID, path.groupId())
                    .cell(REDUNDANCY, paths.size() > 1 ? "YES" : "NO")
                    .cell(STATE, path.isSubmitted() ? "SUBMITTED" : "RESERVED")
                    .cell(WPID0, wpid0)
                    .cell(WPID1, wpid1);
        }

        // if required, override createTableModel() to set column formatters / comparators
        @Override
        protected TableModel createTableModel() {
            return super.createTableModel();
        }
    }

    private String toDevicePortName(ConnectPoint point) {
        Device device = deviceService.getDevice(point.deviceId());
        String deviceName = (device != null) ? toDeviceName(device) : null;
        if (Strings.isNullOrEmpty(deviceName)) {
            deviceName = point.deviceId().toString();
        }

        Port port = deviceService.getPort(point);
        String portName = (port != null) ? port.annotations().value(AnnotationKeys.PORT_NAME) : null;
        if (Strings.isNullOrEmpty(portName)) {
            portName =  point.port().hasName() ? point.port().name() : point.port().toString();
        }
        return deviceName + "|" + portName;
    }

    private String toDeviceName(Device device) {
        return device.annotations().value(AnnotationKeys.NAME);
    }

    private String toRoadmPath(Path path) {
        return path.links().stream()
                .flatMap(l -> Stream.of(l.src(), l.dst()))
                .map(ConnectPoint::deviceId)
                .distinct()
                .map(deviceService::getDevice)
                .filter(this::isRoadm)
                .map(this::toDeviceName)
                .collect(Collectors.joining(" <=> "));
    }

    private boolean isRoadm(Device device) {
        if (device == null) {
            return false;
        }
        return device.type() == Device.Type.ROADM || device.type() == Device.Type.ROADM_OTN;
    }

    // handler for wavelength path item details requests
    private final class WlpathTableDetailRequestHandler extends RequestHandler {

        private WlpathTableDetailRequestHandler() {
            super(WLPATH_TABLE_DETAIL_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            String id = string(payload, ID, "(none)");

            // SomeService ss = get(SomeService.class);
            // Item item = ss.getItemDetails(id)

            WavelengthPath path = getPath(id);

            ObjectNode rootNode = objectNode();
            ObjectNode data = objectNode();
            rootNode.set(DETAILS, data);

            if (path == null) {
                rootNode.put(RESULT, "Item with id '" + id + "' not found");
                log.warn("attempted to get item detail for id '{}'", id);

            } else {
                rootNode.put(RESULT, "Found item with id '" + id + "'");
                data.put(ID, path.id());
                data.put(TP1, toDevicePortName(path.src()) + "\n(" + path.src().toString() + ")");
                data.put(PATH, toRoadmPath(path.path()));
                data.put(TP2, toDevicePortName(path.dst()) + "\n(" + path.dst().toString() + ")");
                data.put(NAME, Strings.isNullOrEmpty(path.name()) ? "-" : path.name());
                data.put(FREQUENCY, String.format(
                        "[%d] %s THz", path.frequencyId(),
                            frequencyFormat.format(path.signal().centralFrequency().asTHz())));
                data.put(QVALUE, String.format("%.3f", path.qValue()));
                data.put(QMARGIN, String.format("%.3f", path.qValue() - path.qThreshold()));
                data.put(SUBMIT_ID, path.groupId());
                data.put(REDUNDANCY,
                        pathStore.findByGroupId(path.groupId()).size() > 1 ? "YES" : "NO");
                data.put(STATE, path.isSubmitted() ? "SUBMITTED" : "RESERVED");
            }

            sendMessage(WLPATH_TABLE_DETAIL_RESP, rootNode);
        }
    }

    // ===================================================================

    // Lookup a single item.
    private WavelengthPath getPath(String id) {
        long wlId;
        try {
            wlId = Long.valueOf(id);
        } catch (NumberFormatException ex) {
            return null;
        }
        return pathStore.get(wlId);
    }

    // Produce a list of items.
    private Collection<WavelengthPath> getItems() {
        return pathStore.getPaths();
    }

    // Simple model class to provide wavelength path data
    private static class Item {
    }
}
