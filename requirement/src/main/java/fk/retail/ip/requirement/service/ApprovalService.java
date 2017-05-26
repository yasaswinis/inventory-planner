package fk.retail.ip.requirement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import fk.retail.ip.email.internal.enums.ApprovalEmailParams;
import fk.retail.ip.email.internal.enums.EmailParams;
import fk.retail.ip.email.model.StencilConfigModel;
import fk.retail.ip.requirement.config.EmailConfiguration;
import fk.retail.ip.requirement.internal.Constants;
import fk.retail.ip.requirement.internal.command.EventLogger;
import fk.retail.ip.requirement.internal.command.FdpRequirementIngestorImpl;
import fk.retail.ip.requirement.internal.command.PayloadCreationHelper;
import fk.retail.ip.requirement.internal.command.emailHelper.ApprovalEmailHelper;
import fk.retail.ip.requirement.internal.entities.Requirement;
import fk.retail.ip.requirement.internal.entities.RequirementApprovalTransition;
import fk.retail.ip.requirement.internal.enums.EventType;
import fk.retail.ip.requirement.internal.enums.FdpRequirementEventType;
import fk.retail.ip.requirement.internal.enums.OverrideKey;
import fk.retail.ip.requirement.internal.enums.RequirementApprovalState;
import fk.retail.ip.requirement.internal.repository.RequirementApprovalTransitionRepository;
import fk.retail.ip.requirement.internal.repository.RequirementEventLogRepository;
import fk.retail.ip.requirement.internal.repository.RequirementRepository;
import fk.retail.ip.requirement.model.RequirementChangeMap;
import fk.retail.ip.requirement.model.RequirementChangeRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 * @author Pragalathan M<pragalathan.m@flipkart.com>
 */

@Slf4j
public class ApprovalService<E> {

    private StencilConfigModel stencilConfigModel;

    public ApprovalService() {
        String approvalEmailConfigurations  = "/stencil-configurations.json";
        try {
            InputStreamReader inputStreamReader = new InputStreamReader
                    (getClass().getResourceAsStream(approvalEmailConfigurations));
            ObjectMapper objectMapper = new ObjectMapper();
            stencilConfigModel = objectMapper.readValue(inputStreamReader, StencilConfigModel.class);
        } catch (IOException ex) {
            log.debug("error in reading file", ex);
        }
    }

    public void changeState(List<E> items,
                            String fromState,
                            String userId,
                            boolean forward,
                            Function<E, String> getter,
                            String groupName,
                            StageChangeAction<E>... actionListeners) {
        validate(items, fromState, getter);
        for (StageChangeAction<E> consumer : actionListeners) {
            consumer.execute(userId, fromState, forward, items, groupName, stencilConfigModel);
        }
    }

    private void validate(List<E> items, String fromState, Function<E, String> getter) {
        for (E item : items) {
            String currentState = getter.apply(item);
            if (!currentState.equals(fromState)) {
                /*TODO: add id here*/
                throw new IllegalStateException("Entity[id=" + item + "] is not in " + fromState + " state");
            }
        }
    }

    public interface StageChangeAction<E> {

        void execute(String userId,
                     String fromState,
                     boolean forward,
                     List<E> entity,
                     String groupName,
                     StencilConfigModel stencilConfigModel);
    }

    public static class CopyOnStateChangeAction implements StageChangeAction<Requirement> {

        private RequirementRepository requirementRepository;
        private RequirementApprovalTransitionRepository requirementApprovalStateTransitionRepository;
        private FdpRequirementIngestorImpl fdpRequirementIngestor;
        private RequirementEventLogRepository requirementEventLogRepository;
        private ApprovalEmailHelper appovalEmailHelper;
        private EmailConfiguration emailConfiguration;

        public CopyOnStateChangeAction(
                RequirementRepository requirementRepository,
                RequirementApprovalTransitionRepository requirementApprovalStateTransitionRepository,
                FdpRequirementIngestorImpl fdpRequirementIngestor,
                RequirementEventLogRepository requirementEventLogRepository,
                ApprovalEmailHelper appovalEmailHelper,
                EmailConfiguration emailConfiguration
        ) {
            this.requirementRepository = requirementRepository;
            this.requirementApprovalStateTransitionRepository = requirementApprovalStateTransitionRepository;
            this.fdpRequirementIngestor = fdpRequirementIngestor;
            this.requirementEventLogRepository = requirementEventLogRepository;
            this.appovalEmailHelper = appovalEmailHelper;
            this.emailConfiguration = emailConfiguration;
        }


        @Override
        public void execute(String userId, String fromState, boolean forward, List<Requirement> requirements, String groupName, StencilConfigModel stencilConfigModel) {
            Map<Long, String> groupToTargetState = getGroupToTargetStateMap(fromState, forward);
            log.info("Constructed map for group to Target state " + groupToTargetState);
            Map<String, String> requirementToTargetStateMap = getRequirementToTargetStateMap(groupToTargetState, requirements);
            Set<String> fsns = requirements.stream().map(Requirement::getFsn).collect(Collectors.toSet());
            List<RequirementChangeRequest> requirementChangeRequestList = Lists.newArrayList();
            List<Requirement> allEnabledRequirements = requirementRepository.find(fsns, true);
            EventType eventType = EventType.APPROVAL;
            String nextState = requirementToTargetStateMap.get(requirements.get(0).getId());
            for (Requirement requirement : requirements) {
                String toState = requirementToTargetStateMap.get(requirement.getId());
                boolean isIPCReviewState = RequirementApprovalState.IPC_REVIEW.toString().equals(toState);
                boolean isBizFinReviewState = RequirementApprovalState.BIZFIN_REVIEW.toString().equals(toState);
                String cdoState = RequirementApprovalState.CDO_REVIEW.toString();
                Optional<Requirement> toStateEntity = allEnabledRequirements.stream().filter(e -> e.getFsn().equals(requirement.getFsn()) && e.getWarehouse().equals(requirement.getWarehouse()) && e.getState().equals(toState)).findFirst();
                RequirementChangeRequest requirementChangeRequest = new RequirementChangeRequest();
                List<RequirementChangeMap> requirementChangeMaps = Lists.newArrayList();
                if (forward) {
                    //Add APPROVE events to fdp request
                    requirementChangeMaps.add(PayloadCreationHelper.createChangeMap(OverrideKey.STATE.toString(), fromState, toState, FdpRequirementEventType.APPROVE.toString(), "Moved to next state", userId));
                    if (toStateEntity.isPresent()) {
                        if(!isBizFinReviewState)
                            toStateEntity.get().setQuantity(requirement.getQuantity());
                        if (isIPCReviewState) {
                            Optional<Requirement> cdoStateEntity = allEnabledRequirements.stream().filter(e -> e.getFsn().equals(requirement.getFsn()) && e.getWarehouse().equals(requirement.getWarehouse()) && e.getState().equals(cdoState)).findFirst();
                            toStateEntity.get().setQuantity(cdoStateEntity.get().getQuantity());
                        }
                        toStateEntity.get().setSupplier(requirement.getSupplier());
                        toStateEntity.get().setMrp(requirement.getMrp());
                        toStateEntity.get().setApp(requirement.getApp());
                        toStateEntity.get().setSla(requirement.getSla());
                        toStateEntity.get().setCreatedBy(userId);
                        toStateEntity.get().setCurrent(true);
                        requirement.setCurrent(false);
                        requirementChangeRequest.setRequirement(toStateEntity.get());
                    } else {
                        Requirement newEntity = new Requirement(requirement);
                        if (isIPCReviewState) {
                            Optional<Requirement> cdoStateEntity = allEnabledRequirements.stream().filter(e -> e.getFsn().equals(requirement.getFsn()) && e.getWarehouse().equals(requirement.getWarehouse()) && e.getState().equals(cdoState)).findFirst();
                            newEntity.setQuantity(cdoStateEntity.get().getQuantity());
                        }
                        if (isBizFinReviewState)
                            newEntity.setQuantity(-1);
                        newEntity.setState(toState);
                        newEntity.setCreatedBy(userId);
                        newEntity.setCurrent(true);
                        requirementRepository.persist(newEntity);
                        requirement.setCurrent(false);
                        requirementChangeRequest.setRequirement(newEntity);
                    }
                    eventType = EventType.APPROVAL;
                } else {
                    //Add CANCEL events to fdp request
                    requirementChangeMaps.add(PayloadCreationHelper.createChangeMap(OverrideKey.STATE.toString(), fromState, toState, FdpRequirementEventType.CANCEL.toString(), "Moved to previous state", userId));
                    toStateEntity.ifPresent(e -> { // this will always be present
                        e.setCurrent(true);
                        requirement.setCurrent(false);
                        requirementChangeRequest.setRequirement(toStateEntity.get());
                        e.setCreatedBy(userId);
                    });
                    eventType = EventType.CANCELLATION;
                }
                requirementChangeRequest.setRequirementChangeMaps(requirementChangeMaps);
                requirementChangeRequestList.add(requirementChangeRequest);
            }
            appovalEmailHelper.send(createStencilParamsMap(groupName, userId, getCurrentTimestamp(), nextState), fromState, forward, stencilConfigModel);
            log.info("Updating Projections tables for Requirements");
            requirementRepository.updateProjections(requirements, groupToTargetState);
            //Push APPROVE and CANCEL events to fdp
            log.debug("Pushing APPROVE and CANCEL events to fdp");
            fdpRequirementIngestor.pushToFdp(requirementChangeRequestList);
            EventLogger eventLogger = new EventLogger(requirementEventLogRepository);
            eventLogger.insertEvent(requirementChangeRequestList, eventType);
        }


        private Map<Long, String> getGroupToTargetStateMap(String fromState, boolean forward) {
            List<RequirementApprovalTransition> transitionList = requirementApprovalStateTransitionRepository.getApprovalTransition(fromState, forward);
            return transitionList.stream().collect(Collectors.toMap(RequirementApprovalTransition::getGroupId, RequirementApprovalTransition::getToState));
        }

        private Map<String,String> getRequirementToTargetStateMap(Map<Long, String> groupToTargetState, List<Requirement> requirements) {
            return requirements.stream().collect(Collectors.toMap(requirement -> requirement.getId(), requirement -> groupToTargetState.get(requirement.getGroup())!= null ? groupToTargetState.get(requirement.getGroup()):groupToTargetState.get(Constants.DEFAULT_TRANSITION_GROUP)));
        }

        private Map<EmailParams, String> createStencilParamsMap(
                String groupName,
                String userName,
                String timestamp,
                String state) {
            Map<EmailParams, String> paramsMap = new HashMap<>();
            paramsMap.put(ApprovalEmailParams.USERNAME, userName);
            paramsMap.put(ApprovalEmailParams.GROUPNAME, groupName);
            paramsMap.put(ApprovalEmailParams.TIMESTAMP, timestamp);
            paramsMap.put(ApprovalEmailParams.LINK, getUrl(groupName, state));
            return paramsMap;
        }

        private String getUrl(String groupName, String state) {
            try {
                String requirementLink;
                String path = emailConfiguration.getPath() + state;
                URI uri = new URIBuilder()
                        .setScheme("http")
                        .setHost(emailConfiguration.getHost())
                        .setPath(path)
                        .setParameter("current_state", state)
                        .setParameter("filter[group]", groupName)
                        .setParameter("commit", "search")
                        .build();

                HttpGet httpGet = new HttpGet(uri);
                requirementLink = httpGet.getURI().toString();
                return requirementLink;

            } catch (URISyntaxException ex) {
                return "";
            }
        }
        private String getCurrentTimestamp() {
            return new Date().toString();
        }

    }
}
