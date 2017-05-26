package fk.retail.ip.requirement.internal.entities;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

/**
 * Created by agarwal.vaibhav on 18/04/17.
 */

@Entity
@Data
@Table(name = "requirement_event_log")
public class RequirementEventLog{

    @Id
    private String id;
    private String entityId;
    private String attribute;
    private String oldValue;
    private String newValue;
    private String reason;
    private String userId;
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;
    private String eventType;

    @PrePersist
    private void beforePersist() {
        timestamp = new Date();
        id = UUID.randomUUID().toString().replace("-", "");
    }

}
