package fk.retail.ip.requirement.internal.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by nidhigupta.m on 26/01/17.
 */
@MappedSuperclass
@Data
@NoArgsConstructor
public class AbstractEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Access(AccessType.PROPERTY)
    protected Long id;

    @Temporal(TemporalType.TIMESTAMP)
    protected Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    protected Date updatedAt;

    @NotNull
    @Version
    private Long version;

    @PrePersist
    private void beforePersist() {
        createdAt = new Date();
        updatedAt = new Date();
    }

    @PreUpdate
    private void beforeUpdate() {
        updatedAt = new Date();
    }
}
