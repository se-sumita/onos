package org.onosproject.opticalpathoptimizer.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Link model containing section-element.
 */
public class OpenRoadmModelLink {
    private List<Element> sectionElements;

    /**
     * Constructor.
     */
    public OpenRoadmModelLink() {
        this.sectionElements = Lists.newLinkedList();
    }

    public void addElement(Element v) {
        sectionElements.add(v);
    }

    public List<Element> getSectionElements() {
        return ImmutableList.copyOf(sectionElements);
    }
    public Double getTotalSpan() {
        Double length = 0.0;
        for (Element e : sectionElements) {
            if (e instanceof Fiber) {
                length += ((Fiber) e).getSrlgLen();
            }
        }
        return length;
    }
}
