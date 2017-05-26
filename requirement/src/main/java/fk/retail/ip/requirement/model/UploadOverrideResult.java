package fk.retail.ip.requirement.model;

import lombok.Data;

import java.util.List;

/**
 * Created by agarwal.vaibhav on 05/05/17.
 */
@Data
public class UploadOverrideResult {
    int successfulRowCount;
    List<UploadOverrideFailureLineItem> uploadOverrideFailureLineItemList;
}
