package fk.retail.ip.requirement.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Provider;
import fk.retail.ip.core.poi.SpreadSheetReader;
import fk.retail.ip.d42.client.D42Client;
import fk.retail.ip.proc.model.PushToProcResponse;
import fk.retail.ip.requirement.config.EmailConfiguration;
import fk.retail.ip.requirement.internal.Constants;
import fk.retail.ip.requirement.internal.command.*;
import fk.retail.ip.requirement.internal.command.emailHelper.ApprovalEmailHelper;
import fk.retail.ip.requirement.internal.entities.Requirement;
import fk.retail.ip.requirement.internal.enums.FdpRequirementEventType;
import fk.retail.ip.requirement.internal.enums.OverrideKey;
import fk.retail.ip.requirement.internal.enums.OverrideStatus;
import fk.retail.ip.requirement.internal.enums.RequirementApprovalAction;
import fk.retail.ip.requirement.internal.factory.RequirementStateFactory;
import fk.retail.ip.requirement.internal.repository.RequirementApprovalTransitionRepository;
import fk.retail.ip.requirement.internal.repository.RequirementEventLogRepository;
import fk.retail.ip.requirement.internal.repository.RequirementRepository;
import fk.retail.ip.requirement.internal.states.RequirementState;
import fk.retail.ip.requirement.model.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.json.JSONException;

import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author nidhigupta.m
 * @author Pragalathan M <pragalathan.m@flipkart.com>
 * @author contradiction154
 */
@Slf4j
public class RequirementService {

    private final RequirementRepository requirementRepository;
    private final RequirementApprovalTransitionRepository requirementApprovalStateTransitionRepository;
    private final RequirementStateFactory requirementStateFactory;
    private final ApprovalService approvalService;
    private final Provider<TriggerRequirementCommand> triggerRequirementCommandProvider;
    private final Provider<CalculateRequirementCommand> calculateRequirementCommandProvider;
    private final SearchFilterCommand searchFilterCommand;
    private final Provider<SearchCommand> searchCommandProvider;
    private final PushToProcCommand pushToProcCommand;
    private final FdpRequirementIngestorImpl fdpRequirementIngestor;
    private final RequirementEventLogRepository requirementEventLogRepository;
    private final D42Client d42Client;
    private final int PAGE_SIZE = 20;
    private final String BUCKET_NAME = "ip_requirements";
    private final ApprovalEmailHelper appovalEmailHelper;
    private final EmailConfiguration emailConfiguration;

    @Inject
    public RequirementService(RequirementRepository requirementRepository,
                              RequirementStateFactory requirementStateFactory,
                              ApprovalService approvalService,
                              Provider<CalculateRequirementCommand> calculateRequirementCommandProvider,
                              RequirementApprovalTransitionRepository requirementApprovalStateTransitionRepository,
                              SearchFilterCommand searchFilterCommand, Provider<SearchCommand> searchCommandProvider,
                              FdpRequirementIngestorImpl fdpRequirementIngestor,
                              RequirementEventLogRepository requirementEventLogRepository,
                              Provider<TriggerRequirementCommand> triggerRequirementCommandProvider,
                              PushToProcCommand pushToProcCommand,
                              D42Client d42Client,
                              ApprovalEmailHelper appovalEmailHelper,
                              EmailConfiguration emailConfiguration
                              ) {

        this.requirementRepository = requirementRepository;
        this.requirementStateFactory = requirementStateFactory;
        this.approvalService = approvalService;
        this.calculateRequirementCommandProvider = calculateRequirementCommandProvider;
        this.requirementApprovalStateTransitionRepository = requirementApprovalStateTransitionRepository;
        this.triggerRequirementCommandProvider = triggerRequirementCommandProvider;
        this.searchFilterCommand = searchFilterCommand;
        this.searchCommandProvider = searchCommandProvider;
        this.pushToProcCommand = pushToProcCommand;
        this.fdpRequirementIngestor = fdpRequirementIngestor;
        this.requirementEventLogRepository = requirementEventLogRepository;
        this.d42Client = d42Client;
        this.appovalEmailHelper = appovalEmailHelper;
        this.emailConfiguration = emailConfiguration;
    }

    public StreamingOutput downloadRequirement(DownloadRequirementRequest downloadRequirementRequest) {
        List<Long> requirementIds = downloadRequirementRequest.getRequirementIds();
        String requirementState = downloadRequirementRequest.getState();
        Map<String, Object> filters = downloadRequirementRequest.getFilters();
        boolean isLastAppSupplierRequired = downloadRequirementRequest.isLastAppSupplierRequired();
        List<String> fsns = searchFilterCommand.getSearchFilterFsns(filters);
        List<Requirement> requirements = requirementRepository.findRequirements(requirementIds, requirementState, fsns);
        requirements = requirements.stream().filter(requirement -> !requirement.getWarehouse().equals("all")).collect(Collectors.toList());
        RequirementState state = requirementStateFactory.getRequirementState(requirementState);
        return state.download(requirements, isLastAppSupplierRequired, requirementState);
    }

    public UploadResponse uploadRequirement(
            InputStream inputStream,
            FormDataContentDisposition fileDetail,
            FormDataBodyPart formBody,
            String requirementState,
            String userId
    ) throws IOException, InvalidFormatException {
        ByteArrayOutputStream baos  = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inputStream.read(buffer)) > -1 ) {
            baos.write(buffer, 0, len);
        }
        baos.flush();
        String fileName = fileDetail.getFileName();
        String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
        String objectKey = String.format("%s:%s:%s", timeStamp, userId, fileName);
        String contentType = formBody.getMediaType().toString();

        d42Client.put(BUCKET_NAME, objectKey, new ByteArrayInputStream(baos.toByteArray()), contentType);

        SpreadSheetReader spreadSheetReader = new SpreadSheetReader();
        List<Map<String, Object>> parsedMappingList = spreadSheetReader.read(new ByteArrayInputStream(baos.toByteArray()));

        log.info("Uploaded file parsed and contains " + parsedMappingList.size() +  " records");

        if (parsedMappingList.size() == 0) {
            UploadResponse uploadResponse = new UploadResponse();
            uploadResponse.setStatus(Constants.EMPTY_RECORDS);
            uploadResponse.setSuccessfulRowCount(0);
            return uploadResponse;
        }
        ObjectMapper mapper = new ObjectMapper();

        try {
            List<RequirementUploadLineItem> requirementUploadLineItems = mapper.convertValue(parsedMappingList,
                    new TypeReference<List<RequirementUploadLineItem>>() {});
            List<Requirement> requirements;
            List<String> requirementIds = new ArrayList<>();
            requirementUploadLineItems.forEach(row -> {
                requirementIds.add(row.getRequirementId());
            });

            requirements = requirementRepository.findActiveRequirementForState(requirementIds, requirementState);
            log.info("number of requirements found for uploaded records : " + requirements.size());

            if (requirements.size() == 0) {
                UploadResponse uploadResponse = new UploadResponse();
                uploadResponse.setStatus(Constants.NO_REQUIREMENT_FOUND);
                uploadResponse.setSuccessfulRowCount(0);
                return uploadResponse;
            } else {
                RequirementState state = requirementStateFactory.getRequirementState(requirementState);
                try {

                    UploadOverrideResult uploadOverrideResult = state.upload(requirements, requirementUploadLineItems, userId, requirementState);

                    List<UploadOverrideFailureLineItem> uploadOverrideFailureLineItems = uploadOverrideResult.getUploadOverrideFailureLineItemList();

                    UploadResponse uploadResponse = new UploadResponse();
                    uploadResponse.setUploadOverrideFailureLineItems(uploadOverrideFailureLineItems);
                    uploadResponse.setSuccessfulRowCount(uploadOverrideResult.getSuccessfulRowCount());
                    if (uploadOverrideFailureLineItems.isEmpty()) {
                        uploadResponse.setStatus(OverrideStatus.SUCCESS.toString());
                    } else {
                        uploadResponse.setStatus(OverrideStatus.FAILURE.toString());
                    }
                    return uploadResponse;

                } catch(UnsupportedOperationException ex) {
                    log.error("Unsupported operation");
                    UploadResponse uploadResponse = new UploadResponse();
                    uploadResponse.setStatus(Constants.UNSUPPORTED_OPERATION);
                    uploadResponse.setSuccessfulRowCount(0);
                    return uploadResponse;
                }

            }
        } catch(IllegalArgumentException ex) {
            log.warn("One or more fields were unrecognised", ex.getStackTrace());
            UploadResponse uploadResponse = new UploadResponse();
            uploadResponse.setStatus(Constants.UNKNOWN_COLUMN);
            uploadResponse.setSuccessfulRowCount(0);
            return uploadResponse;
        }

    }

    public String changeState(RequirementApprovalRequest request, String userId) throws JSONException {
        log.info("Approval request received for " + request);
        RequirementApprovalAction action = RequirementApprovalAction.valueOf(request.getFilters().get("projection_action").toString());
        boolean forward = action.isForward();
        List<Long> ids = (List<Long>) request.getFilters().get("id");
        String state = (String) request.getFilters().get("state");
        Function<Requirement, String> getter = Requirement::getState;
        List<Requirement> requirements;
        String groupName = request.getFilters().containsKey("group") ? ((List)request.getFilters().get("group")).get(0).toString() : "";
        List<String> fsns = searchFilterCommand.getSearchFilterFsns(request.getFilters());
        requirements = requirementRepository.findRequirements(ids, state, fsns);
        log.info("Change state Request for {} number of requirements", requirements.size());
        approvalService.changeState(
                requirements,
                state,
                userId,
                forward,
                getter,
                groupName,
                new ApprovalService.CopyOnStateChangeAction(requirementRepository,
                        requirementApprovalStateTransitionRepository,
                        fdpRequirementIngestor,
                        requirementEventLogRepository,
                        appovalEmailHelper,
                        emailConfiguration
                )
        );
        log.info("State changed for {} number of requirements", requirements.size());
        return "{\"msg\":\"Moved " + requirements.size() + " requirements to new state.\"}";
    }

    public String pushToProc(RaisePORequest request, String userId) throws JSONException {
        log.info("Push to proc request received " + request);
        List<Long> ids = (List<Long>) request.getFilters().get("id");
        List<String> fsns = searchFilterCommand.getSearchFilterFsns(request.getFilters());
        String state = (String) request.getFilters().get("state");
        List<Requirement> requirements = requirementRepository.findRequirements(ids, state, fsns);
        int pushedRequirements = pushToProcCommand.pushToProc(requirements,userId);
        log.info("Moved {} number of requirements to Procurement", requirements.size());
        return "{\"msg\":\"Moved " + pushedRequirements +" requirements to Procurement.\"}";
    }

    public String setPurchaseOrderId(String reqId, PushToProcResponse callback) {
        log.info("Proc response received for requirement_id: " + reqId);
        List<Requirement> requirementList = requirementRepository.findRequirementByIds(Arrays.asList(reqId));
        Requirement requirement = requirementList.get(0);
        Map<String,Object> response = callback.getProcResponse().get(0); //since we get only one requirement response from proc
        requirement.setPoId((Integer) response.get("id"));
        String userId = requirement.getCreatedBy();
        //Add PROC_CALLBACK_RECEIVED events to fdp request
        List<RequirementChangeRequest> requirementChangeRequestList = Lists.newArrayList();
        RequirementChangeRequest requirementChangeRequest = new RequirementChangeRequest();
        List<RequirementChangeMap> requirementChangeMaps = Lists.newArrayList();
        log.info("Adding PROC_CALLBACK_RECEIVED events to fdp request");
        requirementChangeRequest.setRequirement(requirement);
        requirementChangeMaps.add(PayloadCreationHelper.createChangeMap(OverrideKey.PO_ID.toString(), null, requirement.getPoId().toString(), FdpRequirementEventType.PROC_CALLBACK_RECEIVED.toString(), "Proc callback received", userId));
        requirementChangeRequest.setRequirementChangeMaps(requirementChangeMaps);
        requirementChangeRequestList.add(requirementChangeRequest);
        //Push PROC_CALLBACK_RECEIVED events to fdp
        log.info("Pushing PROC_CALLBACK_RECEIVED events to fdp");
        fdpRequirementIngestor.pushToFdp(requirementChangeRequestList);
        return "{\"msg\":\"Set po_id for requirement_id: " + reqId + " \"}";
    }

    public SearchResponse.GroupedResponse search(RequirementSearchRequest request) throws JSONException {
        log.info("Search Requirement request received " + request);
        Integer pageNo;
        List<Requirement> requirements;
        List<Long> projectionIds;
        int startIndex, endIndex;
        List<Long> batchProjectionIds;
        pageNo = request.getFilters().get("page")!=null ? Integer.parseInt(request.getFilters().get("page").toString()): 1;
        String state = (String) request.getFilters().get("state");
        List<String> fsns = searchFilterCommand.getSearchFilterFsns(request.getFilters());
        if(fsns == null || fsns.isEmpty()) return new SearchResponse.GroupedResponse(0, PAGE_SIZE);
        projectionIds = requirementRepository.findProjectionIds(fsns, state);
        log.info("Search Request for {} number of ProjectionIds", projectionIds.size());
        if(projectionIds==null || projectionIds.isEmpty()) return new SearchResponse.GroupedResponse(0, PAGE_SIZE);
        startIndex = (pageNo-1)*PAGE_SIZE;
        endIndex = (projectionIds.size() >= pageNo*PAGE_SIZE) ? (pageNo*PAGE_SIZE) : projectionIds.size();
        batchProjectionIds = projectionIds.subList(startIndex, endIndex);
        requirements = requirementRepository.findRequirements(batchProjectionIds, state, Lists.newArrayList());
        log.info("Search Request for {} number of requirements", requirements.size());
        Map<String, List<RequirementSearchLineItem>> fsnToSearchItemsMap =  searchCommandProvider.get().execute(requirements, state);
        log.info("Search Request for {} number of fsns", fsnToSearchItemsMap.size());
        SearchResponse.GroupedResponse groupedResponse = new SearchResponse.GroupedResponse(projectionIds.size(), PAGE_SIZE);
        for (String fsn : fsnToSearchItemsMap.keySet()) {
            SearchResponse searchResponse = new SearchResponse(fsnToSearchItemsMap.get(fsn));
            groupedResponse.getProjections().add(searchResponse);
        }
        log.info("Got Search Response for Requirement");
        return groupedResponse;
    }

    public List<String> triggerRequirement(TriggerRequirementRequest triggerRequirementRequest) {
        return triggerRequirementCommandProvider.get().withFsns(triggerRequirementRequest.getFsns()).withGroupIds(triggerRequirementRequest.getGroupIds()).execute();
    }

    public void calculateRequirement(CalculateRequirementRequest calculateRequirementRequest) {
        calculateRequirementCommandProvider.get().withFsns(calculateRequirementRequest.getFsns()).execute();
    }
}
