package fk.retail.ip.requirement.internal.states;

import fk.retail.ip.requirement.internal.entities.Requirement;
import fk.retail.ip.requirement.model.RequirementUploadLineItem;
import fk.retail.ip.requirement.model.UploadOverrideResult;

import javax.ws.rs.core.StreamingOutput;
import java.util.List;

/**
 * Created by nidhigupta.m on 21/02/17.
 */

public interface RequirementState {

    StreamingOutput download(List<Requirement> requirements, boolean isLastAppSupplierRequired, String state );
    UploadOverrideResult upload(List<Requirement> requirements, List<RequirementUploadLineItem> requirementUploadLineItems, String userID, String state);

}
