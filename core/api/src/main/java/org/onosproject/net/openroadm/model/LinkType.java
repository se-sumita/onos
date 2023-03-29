package org.onosproject.net.openroadm.model;

/**
 * Enum of Link types.
 */
public enum LinkType {
    EXPRESS_LINK("EXPRESS-LINK"),
    ADD_LINK("ADD-LINK"),
    DROP_LINK("DROP-LINK"),
    ROADM_TO_ROADM("ROADM-TO-ROADM"),
    XPONDER_INPUT("XPONDER-INPUT"),
    XPONDER_OUTPUT("XPONDER-OUTPUT"),
    OTN_LINK("OTN-LINK");

    private final String text;

    LinkType(String text) {
        this.text = text;
    }

    public String text() {
        return this.text;
    }

    public static LinkType fromString(final String text) {
        LinkType[] types = LinkType.values();
        for (LinkType type : types) {
            if (type.text.equalsIgnoreCase(text)) {
                return type;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return text;
    }
}
