package fk.retail.ip.requirement.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Provider;
import fk.retail.ip.core.poi.SpreadSheetReader;
import fk.retail.ip.requirement.internal.Constants;
import fk.retail.ip.requirement.internal.command.CalculateRequirementCommand;
import fk.retail.ip.requirement.internal.command.SearchCommand;
import fk.retail.ip.requirement.internal.command.SearchFilterCommand;
import fk.retail.ip.requirement.internal.entities.Requirement;
import fk.retail.ip.requirement.internal.enums.OverrideStatus;
import fk.retail.ip.requirement.internal.factory.RequirementStateFactory;
import fk.retail.ip.requirement.internal.repository.RequirementRepository;
import fk.retail.ip.requirement.internal.states.RequirementState;
import fk.retail.ip.requirement.model.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.json.JSONException;

import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
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
    private final RequirementStateFactory requirementStateFactory;
    private final ApprovalService approvalService;
    private final Provider<CalculateRequirementCommand> calculateRequirementCommandProvider;
    private final SearchFilterCommand searchFilterCommand;
    private final Provider<SearchCommand> searchCommandProvider;
    private final int PAGE_SIZE = 20;

    @Inject
    public RequirementService(RequirementRepository requirementRepository, RequirementStateFactory requirementStateFactory,
                              ApprovalService approvalService, Provider<CalculateRequirementCommand> calculateRequirementCommandProvider,
                              SearchFilterCommand searchFilterCommand, Provider<SearchCommand> searchCommandProvider) {
        this.requirementRepository = requirementRepository;
        this.requirementStateFactory = requirementStateFactory;
        this.approvalService = approvalService;
        this.calculateRequirementCommandProvider = calculateRequirementCommandProvider;
        this.searchFilterCommand = searchFilterCommand;
        this.searchCommandProvider = searchCommandProvider;
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
        return state.download(requirements, isLastAppSupplierRequired);
    }

    public UploadResponse uploadRequirement(
            InputStream inputStream,
            String requirementState
    ) throws IOException, InvalidFormatException {

        SpreadSheetReader spreadSheetReader = new SpreadSheetReader();
        List<Map<String, Object>> parsedMappingList = spreadSheetReader.read(inputStream);
        log.info("Uploaded file parsed and contains " + parsedMappingList.size() +  " records");

        if (parsedMappingList.size() == 0) {
            UploadResponse uploadResponse = new UploadResponse();
            uploadResponse.setStatus(Constants.EMPTY_RECORDS);
            uploadResponse.setSuccessfulRowCount(0);
            return uploadResponse;
        }
        ObjectMapper mapper = new ObjectMapper();

        try {
            List<RequirementDownloadLineItem> requirementDownloadLineItems = mapper.convertValue(parsedMappingList,
                    new TypeReference<List<RequirementDownloadLineItem>>() {});
            List<Requirement> requirements;
            List<Long> requirementIds = new ArrayList<>();
            requirementDownloadLineItems.forEach(row ->
                            requirementIds.add(row.getRequirementId())
            );

            requirements = requirementRepository.findRequirementByIds(requirementIds);
            log.info("number of requirements found for uploaded records : " + requirements.size());

            if (requirements.size() == 0) {
                UploadResponse uploadResponse = new UploadResponse();
                uploadResponse.setStatus(Constants.NO_REQUIREMENT_FOUND);
                uploadResponse.setSuccessfulRowCount(0);
                return uploadResponse;
            } else {
                RequirementState state = requirementStateFactory.getRequirementState(requirementState);
                try {
                    List<UploadOverrideFailureLineItem> uploadLineItems = state.upload(requirements, requirementDownloadLineItems);
                    int successfulRowCount = requirementDownloadLineItems.size() - uploadLineItems.size();
                    UploadResponse uploadResponse = new UploadResponse();
                    uploadResponse.setUploadOverrideFailureLineItems(uploadLineItems);
                    uploadResponse.setSuccessfulRowCount(successfulRowCount);
                    if (uploadLineItems.isEmpty()) {
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

    public String changeState(RequirementApprovalRequest request) throws JSONException {
        String action = request.getFilters().get("projection_action").toString();
        Function<Requirement, String> getter = Requirement::getState;
        List<Requirement> requirements;
        List<Long> ids = (List<Long>) request.getFilters().get("id");
        String state = (String) request.getFilters().get("state");
        Set<Long> projectionIds = new HashSet<>();
        List<String> fsns = searchFilterCommand.getSearchFilterFsns(request.getFilters());
        requirements = requirementRepository.findRequirements(ids, state, fsns);
        log.info("Change state Request for {} number of requirements", requirements.size());
        requirements.stream().forEach(e -> projectionIds.add(e.getProjectionId()));
        approvalService.changeState(requirements, "dummyUser", action, getter, new ApprovalService.CopyOnStateChangeAction(requirementRepository));
        log.info("State changed for {} number of requirements", requirements.size());
        requirementRepository.updateProjection(projectionIds, approvalService.getTargetState(action));
        log.info("Projections table updated for Requirements");
        return "{\"msg\":\"Moved " + projectionIds.size() + " projections to new state.\"}";
    }

    public SearchResponse.GroupedResponse search(RequirementSearchRequest request, int pageNo) throws JSONException {
        List<Requirement> requirements;
        String state = (String) request.getFilters().get("state");
        List<String> fsns = searchFilterCommand.getSearchFilterFsns(request.getFilters());
        requirements = requirementRepository.findRequirements(null, state, fsns);
        Map<String, List<RequirementSearchLineItem>> fsnToSearchItemsMap =  searchCommandProvider.get().execute(requirements);
        SearchResponse.GroupedResponse groupedResponse = new SearchResponse.GroupedResponse(fsnToSearchItemsMap.size(), PAGE_SIZE);
        for (String fsn : fsnToSearchItemsMap.keySet()) {
            SearchResponse searchResponse = new SearchResponse(fsnToSearchItemsMap.get(fsn));
            groupedResponse.getProjections().add(searchResponse);
        }
        return groupedResponse;
    }


    public void calculateRequirement(CalculateRequirementRequest calculateRequirementRequest) {
        calculateRequirementCommandProvider.get().withFsns(calculateRequirementRequest.getFsns()).execute();
    }
}
