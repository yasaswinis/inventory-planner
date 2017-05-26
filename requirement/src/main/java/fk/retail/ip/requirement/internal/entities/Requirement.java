package fk.retail.ip.requirement.internal.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;
import java.util.UUID;



/**
 * Created by nidhigupta.m on 26/01/17.
 */

@Entity
@XmlRootElement
@Getter
@Setter
@NoArgsConstructor
//todo: cleanup
@Table(name = "projection_states")
//@Table(name = "REQUIREMENT")
public class Requirement {

    private static final long serialVersionUID = 1L;

    //todo : add this field in projection_states in old db

    @Id
    private String id;
    @NotNull
    private String fsn;

    @NotNull
    private String warehouse;

    //todo:cleanup
    @Column(name = "qty")
    private double quantity;

    private String supplier;

    private Integer mrp;

    private Double app;

    //todo:cleanup
    @Column(name = "app_currency")
    private String currency;

    private Integer sla;

    private boolean international;

    @NotNull
    private String state;

    private String procType;


    //todo:cleanup
    @Column(name = "enabled")
    private boolean enabled;

    @Column(name = "is_current")
    private boolean current;

    //todo: cleanup
    @Column(name = "comment")
    private String overrideComment;

    private String createdBy;

    private String updatedBy;

    private Long sslId;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "requirement_snapshot_id")
    private RequirementSnapshot requirementSnapshot;

    //TODO: legacy code
    @Column(name = "pan_india")
    private Integer panIndiaQuantity;

    //TODO: legacy code
    @Column(name = "projection_id")
    private Long projectionId;

    //todo:cleanup
    private String mrpCurrency;

    public Requirement(String id) {
        this.id = id;
    }
    private Integer poId;

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
        id = UUID.randomUUID().toString().replace("-", "");
    }

    @PreUpdate
    private void beforeUpdate() {
        updatedAt = new Date();
    }


    public Requirement(Requirement other) {
        fsn = other.fsn;
        warehouse = other.warehouse;
        quantity = other.quantity;
        supplier = other.supplier;
        mrp = other.mrp;
        mrpCurrency = other.mrpCurrency;
        app = other.app;
        currency = other.currency;
        sla = other.sla;
        international = other.international;
        procType = other.procType;
        enabled = other.enabled;
        current = other.current;
        requirementSnapshot = other.requirementSnapshot;
        poId = other.poId;

        //TODO: legacy code
        projectionId = other.projectionId;
        panIndiaQuantity = other.panIndiaQuantity;
        sslId = other.sslId;
    }

    public long getGroup() {
        return this.requirementSnapshot.getGroup().getId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Requirement that = (Requirement) o;

        return new EqualsBuilder()
                .append(quantity, that.quantity)
                .append(international, that.international)
                .append(enabled, that.enabled)
                .append(current, that.current)
                .append(id, that.id)
                .append(fsn, that.fsn)
                .append(warehouse, that.warehouse)
                .append(supplier, that.supplier)
                .append(mrp, that.mrp)
                .append(app, that.app)
                .append(currency, that.currency)
                .append(sla, that.sla)
                .append(state, that.state)
                .append(procType, that.procType)
                .append(overrideComment, that.overrideComment)
                .append(createdBy, that.createdBy)
                .append(updatedBy, that.updatedBy)
                .append(sslId, that.sslId)
                .append(panIndiaQuantity, that.panIndiaQuantity)
                .append(projectionId, that.projectionId)
                .append(mrpCurrency, that.mrpCurrency)
                .append(poId, that.poId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(fsn)
                .append(warehouse)
                .append(quantity)
                .append(supplier)
                .append(mrp)
                .append(app)
                .append(currency)
                .append(sla)
                .append(international)
                .append(state)
                .append(procType)
                .append(enabled)
                .append(current)
                .append(overrideComment)
                .append(createdBy)
                .append(updatedBy)
                .append(sslId)
                .append(panIndiaQuantity)
                .append(projectionId)
                .append(mrpCurrency)
                .append(poId)
                .toHashCode();
    }
}
