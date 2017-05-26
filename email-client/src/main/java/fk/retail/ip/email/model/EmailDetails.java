package fk.retail.ip.email.model;

import lombok.Data;

import javax.persistence.*;

/**
 * Created by agarwal.vaibhav on 09/05/17.
 */
@Data
@Entity
public class EmailDetails {

    @Id
    @Access(AccessType.PROPERTY)
    protected Long id;

    String from;
    String toList;
    String ccList;
    String stencilId;
    String group;

}
