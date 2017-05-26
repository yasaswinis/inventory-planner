package fk.retail.ip.requirement.internal.command.upload;


import com.google.common.collect.Lists;
import fk.retail.ip.requirement.internal.Constants;
import fk.retail.ip.requirement.internal.command.EventLogger;
import fk.retail.ip.requirement.internal.command.FdpRequirementIngestorImpl;
import fk.retail.ip.requirement.internal.command.PayloadCreationHelper;
import fk.retail.ip.requirement.internal.command.RequirementHelper;
import fk.retail.ip.requirement.internal.entities.Requirement;
import fk.retail.ip.requirement.internal.enums.*;
import fk.retail.ip.requirement.internal.repository.RequirementEventLogRepository;
import fk.retail.ip.requirement.internal.repository.RequirementRepository;
import fk.retail.ip.requirement.model.*;
import fk.retail.ip.ssl.model.SupplierSelectionResponse;
import fk.retail.ip.ssl.model.SupplierView;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.map.MultiKeyMap;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by vaibhav.agarwal on 03/02/17.
 * Code never lies.
*/
@Slf4j
public abstract class UploadCommand {

    abstract Map<String, Object> validateAndSetStateSpecificFields(RequirementUploadLineItem row, Requirement requirement,
                                                                   Map<String, String> fsnToVerticalMap,
                                                                   MultiKeyMap<String,SupplierSelectionResponse> fsnWhSupplierMap);

    private final RequirementRepository requirementRepository;
    private final FdpRequirementIngestorImpl fdpRequirementIngestor;
    private final RequirementEventLogRepository requirementEventLogRepository;
    private final RequirementHelper requirementHelper;

    public UploadCommand(
            RequirementRepository requirementRepository,
            FdpRequirementIngestorImpl fdpRequirementIngestor,
            RequirementEventLogRepository requirementEventLogRepository,
            RequirementHelper requirementHelper
    ) {
        this.requirementRepository = requirementRepository;
        this.fdpRequirementIngestor = fdpRequirementIngestor;
        this.requirementEventLogRepository = requirementEventLogRepository;
        this.requirementHelper = requirementHelper;
    }

    public UploadOverrideResult execute(
            List<RequirementUploadLineItem> requirementUploadLineItems,
            List<Requirement> requirements,
            String userId,
            String state
    ) {

        Map<String, String> fsnToVerticalMap = null;
        MultiKeyMap<String,SupplierSelectionResponse> fsnWhSupplierMap = null;
        Map<String, Requirement> requirementMap = requirements.stream().
                collect(Collectors.toMap(Requirement::getId, Function.identity()));

        ArrayList<UploadOverrideFailureLineItem> uploadOverrideFailureLineItems = new ArrayList<>();
        /*Row count has been made to initiliase to 1 to accommodate the headers in the excel*/
        int rowCount = 1;

        /*Stores the count for the number of rows actually updated upon override*/
        int successfulRowCount = 0;
        List<RequirementChangeRequest> requirementChangeRequestList = Lists.newArrayList();
        if(RequirementApprovalState.CDO_REVIEW.toString().equals(state)) {
            Set<String> fsns = requirements.stream().map(Requirement::getFsn).collect(Collectors.toSet());
            fsnToVerticalMap = requirementHelper.createFsnVerticalMap(fsns);
            List<Requirement> supplierOverriddenRequirements = requirementHelper.getSupplierOverriddenRequirement(requirementUploadLineItems, requirementMap);
            fsnWhSupplierMap = requirementHelper.createFsnWhSupplierMap(supplierOverriddenRequirements);
        }
        for(RequirementUploadLineItem row : requirementUploadLineItems) {
            UploadOverrideFailureLineItem uploadOverrideFailureLineItem = new UploadOverrideFailureLineItem();
            rowCount += 1;
            String fsn = row.getFsn();
            String warehouse = row.getWarehouseName();

            Optional<String> genericComment = validateGenericColumns(fsn, warehouse);

            if (genericComment.isPresent()) {
                uploadOverrideFailureLineItem.setFailureReason(genericComment.get());
                uploadOverrideFailureLineItem.setFsn(fsn == null ? "" : fsn);
                uploadOverrideFailureLineItem.setWarehouse(warehouse == null ? "" : warehouse);
                uploadOverrideFailureLineItem.setRowNumber(rowCount);
                uploadOverrideFailureLineItems.add(uploadOverrideFailureLineItem);

            } else {

                String requirementId = row.getRequirementId();
                if (requirementMap.containsKey(requirementId)) {
                    Requirement requirement = requirementMap.get(requirementId);
                    Map<String, Object> overriddenValues = validateAndSetStateSpecificFields(row, requirement,
                            fsnToVerticalMap, fsnWhSupplierMap);
                    String status = overriddenValues.get(Constants.STATUS).toString();
                    OverrideStatus overrideStatus = OverrideStatus.fromString(status);

                    switch (overrideStatus) {
                        case FAILURE:
                            uploadOverrideFailureLineItem.setFailureReason(overriddenValues.get
                                    (OverrideKey.OVERRIDE_COMMENT.toString()).toString());
                            uploadOverrideFailureLineItem.setFsn(fsn);
                            uploadOverrideFailureLineItem.setRowNumber(rowCount);
                            uploadOverrideFailureLineItem.setWarehouse(warehouse);
                            uploadOverrideFailureLineItems.add(uploadOverrideFailureLineItem);
                            break;

                        case UPDATE:
                                //Add IPC_QUANTITY_OVERRIDE, CDO_QUANTITY_OVERRIDE, CDO_APP_OVERRIDE, CDO_SLA_OVERRIDE, CDO_SUPPLIER_OVERRIDE events to fdp request
                                RequirementChangeRequest requirementChangeRequest = new RequirementChangeRequest();
                                List<RequirementChangeMap> requirementChangeMaps = Lists.newArrayList();

                                if (overriddenValues.containsKey(OverrideKey.QUANTITY.toString())) {
                                    String eventType = null;
                                    String overrideReason = null;
                                    if (RequirementApprovalState.PROPOSED.toString().equals(requirement.getState())) {
                                        eventType = FdpRequirementEventType.IPC_QUANTITY_OVERRIDE.toString();
                                        overrideReason = row.getIpcQuantityOverrideReason();
                                    } else if (RequirementApprovalState.CDO_REVIEW.toString().equals(requirement.getState())) {
                                        eventType = FdpRequirementEventType.CDO_QUANTITY_OVERRIDE.toString();
                                        overrideReason = row.getCdoQuantityOverrideReason();
                                    } else if (RequirementApprovalState.BIZFIN_REVIEW.toString().equals(requirement.getState())) {
                                        eventType = FdpRequirementEventType.BIZFIN_QUANTITY_RECOMMENDATION.toString();
                                        overrideReason = row.getBizFinComment();
                                    }
                                    if (eventType != null)
                                        requirementChangeMaps.add(PayloadCreationHelper.createChangeMap
                                                (OverrideKey.QUANTITY.toString(), String.valueOf(requirement.getQuantity()),
                                                        overriddenValues.get(OverrideKey.QUANTITY.toString()).toString(),
                                                        eventType, overrideReason, userId));

                                    requirement.setQuantity
                                            ((Integer) overriddenValues.get(OverrideKey.QUANTITY.toString()));
                                }

                                if (overriddenValues.containsKey(OverrideKey.SLA.toString())) {
                                    requirementChangeMaps.add(PayloadCreationHelper.createChangeMap(OverrideKey.SLA.toString(),
                                            String.valueOf(requirement.getSla()), overriddenValues.get(OverrideKey.SLA.toString()).toString(),
                                            FdpRequirementEventType.CDO_SLA_OVERRIDE.toString(), "Sla overridden by CDO", userId));
                                    requirement.setSla((Integer) overriddenValues.get(OverrideKey.SLA.toString()));
                                }

                                if (overriddenValues.containsKey(OverrideKey.APP.toString())) {
                                    requirementChangeMaps.add(PayloadCreationHelper.createChangeMap(OverrideKey.APP.toString(),
                                            String.valueOf(requirement.getApp()), overriddenValues.get(OverrideKey.APP.toString()).toString(),
                                            FdpRequirementEventType.CDO_APP_OVERRIDE.toString(), row.getCdoPriceOverrideReason(), userId));
                                    requirement.setApp((Double) overriddenValues.get(OverrideKey.APP.toString()));
                                }

                                if (overriddenValues.containsKey(OverrideKey.SUPPLIER.toString())) {
                                    requirementChangeMaps.add(PayloadCreationHelper.createChangeMap(OverrideKey.SUPPLIER.toString(),
                                            String.valueOf(requirement.getSupplier()), row.getCdoSupplierOverride(),
                                            FdpRequirementEventType.CDO_SUPPLIER_OVERRIDE.toString(),
                                            row.getCdoSupplierOverrideReason(), userId));
                                    requirement.setSupplier(row.getCdoSupplierOverride());
                                    SupplierView supplierView = (SupplierView) overriddenValues.get(OverrideKey.SUPPLIER.toString());
                                    requirement.setMrp(supplierView.getMrp());
                                }

                                if (overriddenValues.containsKey(OverrideKey.OVERRIDE_COMMENT.toString())) {
                                    requirement.setOverrideComment
                                            (overriddenValues.get(OverrideKey.OVERRIDE_COMMENT.toString()).toString());
                                }

                                if (!overriddenValues.containsKey(OverrideKey.QUANTITY.toString()) &&
                                        overriddenValues.containsKey(OverrideKey.OVERRIDE_COMMENT.toString()) &&
                                        requirement.getState().equals(RequirementApprovalState.BIZFIN_REVIEW.toString())) {
                                    requirementChangeMaps.add(PayloadCreationHelper.createChangeMap(
                                            OverrideKey.OVERRIDE_COMMENT.toString(),
                                            null,
                                            row.getBizFinComment(),
                                            FdpRequirementEventType.BIZFIN_QUANTITY_RECOMMENDATION.toString(),
                                            FdpRequirementEventType.BIZFIN_COMMENT_RECOMMENDATION.toString(),
                                            userId
                                    ));
                                }

                                if (!requirementChangeMaps.isEmpty()) {
                                    requirementChangeRequest.setRequirement(requirement);
                                    requirementChangeRequest.setRequirementChangeMaps(requirementChangeMaps);
                                    requirementChangeRequestList.add(requirementChangeRequest);
                                }

                                requirement.setUpdatedBy(userId);
                                successfulRowCount += 1;

                            break;

                        case SUCCESS:
                            break;

                    }
                }   else {
                    uploadOverrideFailureLineItem.setFailureReason
                            (Constants.REQUIREMENT_NOT_FOUND_FOR_GIVEN_REQUIREMENT_ID);
                    uploadOverrideFailureLineItem.setFsn(fsn);
                    uploadOverrideFailureLineItem.setRowNumber(rowCount);
                    uploadOverrideFailureLineItem.setWarehouse(warehouse);
                    uploadOverrideFailureLineItems.add(uploadOverrideFailureLineItem);
                }

                }
            }

        //Push IPC_QUANTITY_OVERRIDE, CDO_QUANTITY_OVERRIDE, CDO_APP_OVERRIDE, CDO_SLA_OVERRIDE, CDO_SUPPLIER_OVERRIDE events to fdp
        log.debug("Pushing IPC_QUANTITY_OVERRIDE, CDO_QUANTITY_OVERRIDE, CDO_APP_OVERRIDE, CDO_SLA_OVERRIDE, CDO_SUPPLIER_OVERRIDE events to fdp");
        fdpRequirementIngestor.pushToFdp(requirementChangeRequestList);
        EventLogger eventLogger = new EventLogger(requirementEventLogRepository);
        eventLogger.insertEvent(requirementChangeRequestList, EventType.OVERRIDE);
        UploadOverrideResult uploadOverrideResult = new UploadOverrideResult();
        uploadOverrideResult.setSuccessfulRowCount(successfulRowCount);
        uploadOverrideResult.setUploadOverrideFailureLineItemList(uploadOverrideFailureLineItems);

        return uploadOverrideResult;
    }

    private Optional<String> validateGenericColumns(String fsn, String warehouse){
        String genericValidationComment;
        if (fsn == null || warehouse == null) {
           genericValidationComment =  Constants.FSN_OR_WAREHOUSE_IS_MISSING;
            return Optional.of(genericValidationComment);
        }
        return Optional.empty();
    }

    protected Optional<String> validateQuantityOverride(
            Integer currentQuantity, Object suggestedQuantity, String overrideComment) {
        String validationComment;
        if (suggestedQuantity == null) {
            return Optional.empty();
        }

        if (suggestedQuantity instanceof Integer) {
            if ((int)suggestedQuantity < 0) {
                validationComment = isEmptyString(overrideComment) ?
                        Constants.INVALID_QUANTITY_WITHOUT_COMMENT :
                        Constants.SUGGESTED_QUANTITY_IS_NOT_GREATER_THAN_ZERO;
                return Optional.of(validationComment);
            } else if (suggestedQuantity != currentQuantity && isEmptyString(overrideComment)) {
                validationComment = Constants.QUANTITY_OVERRIDE_COMMENT_IS_MISSING;
                return Optional.of(validationComment);
            } else {
                return Optional.empty();
            }

        } else {
            validationComment = isEmptyString(overrideComment) ? Constants.INVALID_QUANTITY_WITHOUT_COMMENT:
                    Constants.QUANTITY_IS_NOT_INTEGER;
            return Optional.of(validationComment);
        }
    }

    protected boolean isEmptyString(String comment) {
        return comment == null || comment.trim().isEmpty() ? true : false;
    }

}
