package fk.retail.ip.requirement.internal.enums;

/**
 * Created by agarwal.vaibhav on 24/04/17.
 */
public enum EventType {
    REQUIREMENT_CREATION("REQUIREMENT_CREATION"),
    OVERRIDE("OVERRIDE"),
    APPROVAL("APPROVAL"),
    CANCELLATION("CANCELLATION");

    private String eventValue;
    EventType(String type) {
        this.eventValue = type;
    }

    @Override
    public String toString() {
        return this.eventValue;
    }

}
