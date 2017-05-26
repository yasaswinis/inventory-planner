package fk.retail.ip.requirement.internal.command.upload;

import com.google.inject.Inject;
import fk.retail.ip.requirement.internal.Constants;
import fk.retail.ip.requirement.internal.command.FdpRequirementIngestorImpl;
import fk.retail.ip.requirement.internal.command.RequirementHelper;
import fk.retail.ip.requirement.internal.entities.Requirement;
import fk.retail.ip.requirement.internal.enums.OverrideKey;
import fk.retail.ip.requirement.internal.enums.OverrideStatus;
import fk.retail.ip.requirement.internal.repository.RequirementEventLogRepository;
import fk.retail.ip.requirement.internal.repository.RequirementRepository;
import fk.retail.ip.requirement.model.RequirementDownloadLineItem;
import fk.retail.ip.ssl.model.SupplierSelectionResponse;
import fk.retail.ip.requirement.model.RequirementUploadLineItem;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created by agarwal.vaibhav on 06/02/17.
 */
@Slf4j
public class ProposedUploadCommand extends UploadCommand {

    @Inject
    public ProposedUploadCommand(
            RequirementRepository requirementRepository,
            FdpRequirementIngestorImpl fdpRequirementIngestor,
            RequirementEventLogRepository requirementEventLogRepository,
            RequirementHelper requirementHelper
    ) {
        super(requirementRepository, fdpRequirementIngestor, requirementEventLogRepository, requirementHelper);
    }

    @Override
    public Map<String, Object> validateAndSetStateSpecificFields(
            RequirementUploadLineItem requirementUploadLineItem, Requirement requirement,
            Map<String, String> fsnToVerticalMap, MultiKeyMap<String,SupplierSelectionResponse> fsnWhSupplierMap) {

        Integer currentQuantity = requirementUploadLineItem.getQuantity();
        Object proposedQuantity = requirementUploadLineItem.getIpcQuantityOverride();
        String quantityOverrideComment = requirementUploadLineItem.getIpcQuantityOverrideReason();

        Map<String, Object> overriddenFields = new HashMap<>();

        Optional<String> validationResponse = validateQuantityOverride(
                currentQuantity, proposedQuantity, quantityOverrideComment);
        if (validationResponse.isPresent()) {
            String validationComment = validationResponse.get();
            overriddenFields.put(Constants.STATUS, OverrideStatus.FAILURE.toString());
            overriddenFields.put(OverrideKey.OVERRIDE_COMMENT.toString(), validationComment);
        } else {
            overriddenFields = getOverriddenFields(currentQuantity, proposedQuantity, quantityOverrideComment);
        }

        return overriddenFields;
    }

    private Map<String, Object> getOverriddenFields(
            Integer currentQuantity,
            Object proposedQuantity,
            String quantityOverrideComment
    ) {
        Map<String, Object> overriddenValues = new HashMap<>();

        if (proposedQuantity != null && proposedQuantity != currentQuantity) {
            overriddenValues.put(OverrideKey.QUANTITY.toString(), proposedQuantity);
            JSONObject comment = new JSONObject();
            comment.put(Constants.QUANTITY_OVERRIDE_COMMENT, quantityOverrideComment);
            overriddenValues.put(OverrideKey.OVERRIDE_COMMENT.toString(), comment);
            overriddenValues.put(Constants.STATUS, OverrideStatus.UPDATE.toString());
        } else {
            overriddenValues.put(Constants.STATUS, OverrideStatus.SUCCESS.toString());
        }
        return overriddenValues;
    }

}
