package fk.retail.ip.requirement.service;

import fk.retail.ip.requirement.config.EmailConfiguration;
import fk.retail.ip.requirement.config.TestDbModule;
import fk.retail.ip.requirement.internal.Constants;
import fk.retail.ip.requirement.internal.command.FdpRequirementIngestorImpl;
import fk.retail.ip.requirement.internal.command.emailHelper.ApprovalEmailHelper;
import fk.retail.ip.requirement.internal.entities.*;
import fk.retail.ip.requirement.internal.enums.EventType;
import fk.retail.ip.requirement.internal.enums.OverrideKey;
import fk.retail.ip.requirement.internal.enums.RequirementApprovalState;
import fk.retail.ip.requirement.internal.repository.RequirementApprovalTransitionRepository;
import fk.retail.ip.requirement.internal.repository.RequirementEventLogRepository;
import fk.retail.ip.requirement.internal.repository.RequirementRepository;
import fk.retail.ip.requirement.internal.repository.TestHelper;
import fk.sp.common.extensions.jpa.TransactionalJpaRepositoryTest;
import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

/**
 * @author Pragalathan M<pragalathan.m@flipkart.com>
 */
@RunWith(JukitoRunner.class)
@UseModules(TestDbModule.class)
public class ApprovalServiceTest extends TransactionalJpaRepositoryTest {

    @Mock
    private RequirementRepository requirementRepository;

    @Mock
    private RequirementApprovalTransitionRepository requirementApprovalStateTransitionRepository;

    @Mock
    FdpRequirementIngestorImpl fdpRequirementIngestor;

    @InjectMocks
    private ApprovalService approvalService;

    @Mock
    private RequirementEventLogRepository requirementEventLogRepository;

    @Captor
    private ArgumentCaptor<Requirement> captor;

    @Mock
    private ApprovalEmailHelper approvalEmailHelper;

    @Captor
    private ArgumentCaptor<List<RequirementEventLog>> argumentCaptor;

    @Mock
    private EmailConfiguration emailConfiguration;


    @Test(expected = IllegalStateException.class)
    public void testApprovalFlowWithInvalidState() {
        String fromState = "proposed";
        Requirement requirement = createRequirement("verified", true);
        Function<Requirement, String> getter = Requirement::getState;
        approvalService.changeState(Arrays.asList(requirement), fromState, "userId", true, getter, "", new ApprovalService.CopyOnStateChangeAction(requirementRepository, requirementApprovalStateTransitionRepository, fdpRequirementIngestor, requirementEventLogRepository, approvalEmailHelper, emailConfiguration));

    }

    @Test
    public void testApprovalForwardFlow() {
        String fromState = RequirementApprovalState.PRE_PROPOSED.toString();
        String toState =  RequirementApprovalState.CDO_REVIEW.toString();
        boolean forward = true;
        Requirement requirement = createRequirement(fromState, true);
        Function<Requirement, String> getter = Requirement::getState;
        RequirementApprovalTransition requirementApprovalTransition = TestHelper.getRequirementApprovalTransition(256, fromState, toState, forward);
        Mockito.when(requirementApprovalStateTransitionRepository.getApprovalTransition(Mockito.anyString(), Mockito.eq(true))).thenReturn(Arrays.asList(requirementApprovalTransition));
        Mockito.when(requirementRepository.find(Arrays.asList("fsn1"), true)).thenReturn(Arrays.asList(requirement));
        Mockito.doNothing().when(requirementRepository).updateProjections(Mockito.anyList(), Mockito.anyMap());
        approvalService.changeState(Arrays.asList(requirement), fromState, "userId", true, getter, "", new ApprovalService.CopyOnStateChangeAction(requirementRepository, requirementApprovalStateTransitionRepository, fdpRequirementIngestor, requirementEventLogRepository, approvalEmailHelper, emailConfiguration));
        Mockito.verify(requirementEventLogRepository).persist(argumentCaptor.capture());
        Mockito.verify(requirementRepository).persist(captor.capture());
        Assert.assertEquals(toState,captor.getValue().getState());
        Assert.assertEquals(true, captor.getValue().isCurrent());
        Assert.assertEquals(false, requirement.isCurrent());

        Assert.assertEquals(OverrideKey.STATE.toString(), argumentCaptor.getValue().get(0).getAttribute());
        Assert.assertEquals(fromState, argumentCaptor.getValue().get(0).getOldValue());
        Assert.assertEquals(toState, argumentCaptor.getValue().get(0).getNewValue());
        Assert.assertEquals("Moved to next state", argumentCaptor.getValue().get(0).getReason());
        Assert.assertEquals("userId", argumentCaptor.getValue().get(0).getUserId());
        Assert.assertEquals(EventType.APPROVAL.toString(), argumentCaptor.getValue().get(0).getEventType());
    }


    @Test
    public void testApprovalForwardFlowWithDefaultGroupTransition() {
        String fromState = RequirementApprovalState.PRE_PROPOSED.toString();
        String toState =  RequirementApprovalState.PROPOSED.toString();
        boolean forward = true;
        Requirement requirement = createRequirement(fromState, true);
        Function<Requirement, String> getter = Requirement::getState;
        RequirementApprovalTransition requirementApprovalTransition = TestHelper.getRequirementApprovalTransition(Constants.DEFAULT_TRANSITION_GROUP, fromState, toState, forward);
        Mockito.when(requirementApprovalStateTransitionRepository.getApprovalTransition(Mockito.anyString(), Mockito.eq(true))).thenReturn(Arrays.asList(requirementApprovalTransition));
        Mockito.when(requirementRepository.find(Arrays.asList("fsn1"), true)).thenReturn(Arrays.asList(requirement));
        Mockito.doNothing().when(requirementRepository).updateProjections(Mockito.anyList(), Mockito.anyMap());
        approvalService.changeState(Arrays.asList(requirement), fromState, "userId", true, getter, "", new ApprovalService.CopyOnStateChangeAction(requirementRepository, requirementApprovalStateTransitionRepository, fdpRequirementIngestor, requirementEventLogRepository, approvalEmailHelper, emailConfiguration));
        Mockito.verify(requirementEventLogRepository).persist(argumentCaptor.capture());
        Mockito.verify(requirementRepository).persist(captor.capture());
        Assert.assertEquals(toState,captor.getValue().getState());
        Assert.assertEquals(true, captor.getValue().isCurrent());
        Assert.assertEquals(false, requirement.isCurrent());

        Assert.assertEquals(OverrideKey.STATE.toString(), argumentCaptor.getValue().get(0).getAttribute());
        Assert.assertEquals(fromState, argumentCaptor.getValue().get(0).getOldValue());
        Assert.assertEquals(toState, argumentCaptor.getValue().get(0).getNewValue());
        Assert.assertEquals("Moved to next state", argumentCaptor.getValue().get(0).getReason());
        Assert.assertEquals("userId", argumentCaptor.getValue().get(0).getUserId());
        Assert.assertEquals(EventType.APPROVAL.toString(), argumentCaptor.getValue().get(0).getEventType());
    }


    @Test
    public void testBackwardFlow() {
        String fromState = RequirementApprovalState.PROPOSED.toString();
        String toState =  RequirementApprovalState.PRE_PROPOSED.toString();
        boolean forward = false;
        Requirement requirement = createRequirement(fromState, true);
        Function<Requirement, String> getter = Requirement::getState;
        RequirementApprovalTransition requirementApprovalTransition = TestHelper.getRequirementApprovalTransition(Constants.DEFAULT_TRANSITION_GROUP, fromState, toState, forward);
        Mockito.when(requirementApprovalStateTransitionRepository.getApprovalTransition(Mockito.anyString(), Mockito.eq(true))).thenReturn(Arrays.asList(requirementApprovalTransition));
        Mockito.doNothing().when(requirementRepository).updateProjections(Mockito.anyList(), Mockito.anyMap());
        List<Requirement> allEnabledRequirements = Arrays.asList(createRequirement(toState, false));
        Mockito.when(requirementRepository.find(Arrays.asList("fsn1"), true)).thenReturn(allEnabledRequirements);
        approvalService.changeState(Arrays.asList(requirement), fromState, "userId", true, getter, "", new ApprovalService.CopyOnStateChangeAction(requirementRepository, requirementApprovalStateTransitionRepository, fdpRequirementIngestor, requirementEventLogRepository, approvalEmailHelper, emailConfiguration));
        Mockito.verify(requirementEventLogRepository).persist(argumentCaptor.capture());
        Assert.assertEquals(false, requirement.isCurrent());

        Assert.assertEquals(OverrideKey.STATE.toString(), argumentCaptor.getValue().get(0).getAttribute());
        Assert.assertEquals(fromState, argumentCaptor.getValue().get(0).getOldValue());
        Assert.assertEquals(toState, argumentCaptor.getValue().get(0).getNewValue());
        Assert.assertEquals("Moved to next state", argumentCaptor.getValue().get(0).getReason());
        Assert.assertEquals("userId", argumentCaptor.getValue().get(0).getUserId());
        Assert.assertEquals(EventType.APPROVAL.toString(), argumentCaptor.getValue().get(0).getEventType());
    }


    @Test
    public void testForwardFlowWithToStateEntity() {
        String fromState = RequirementApprovalState.PRE_PROPOSED.toString();
        String toState =  RequirementApprovalState.PROPOSED.toString();
        boolean forward = true;
        Requirement requirement = createRequirement(fromState, true);
        Function<Requirement, String> getter = Requirement::getState;
        RequirementApprovalTransition requirementApprovalTransition = TestHelper.getRequirementApprovalTransition(Constants.DEFAULT_TRANSITION_GROUP, fromState, toState, forward);
        Mockito.when(requirementApprovalStateTransitionRepository.getApprovalTransition(Mockito.anyString(), Mockito.eq(true))).thenReturn(Arrays.asList(requirementApprovalTransition));
        List<Requirement> allEnabledRequirements = Arrays.asList(createRequirement(toState, false));
        Mockito.doNothing().when(requirementRepository).updateProjections(Mockito.anyList(), Mockito.anyMap());
        Mockito.when(requirementRepository.find(Arrays.asList("fsn1"), true)).thenReturn(allEnabledRequirements);
        approvalService.changeState(Arrays.asList(requirement), fromState, "userId", true, getter, "", new ApprovalService.CopyOnStateChangeAction(requirementRepository, requirementApprovalStateTransitionRepository, fdpRequirementIngestor, requirementEventLogRepository, approvalEmailHelper, emailConfiguration));
        Mockito.verify(requirementEventLogRepository).persist(argumentCaptor.capture());
        Assert.assertEquals(false, requirement.isCurrent());

        Assert.assertEquals(OverrideKey.STATE.toString(), argumentCaptor.getValue().get(0).getAttribute());
        Assert.assertEquals(fromState, argumentCaptor.getValue().get(0).getOldValue());
        Assert.assertEquals(toState, argumentCaptor.getValue().get(0).getNewValue());
        Assert.assertEquals("Moved to next state", argumentCaptor.getValue().get(0).getReason());
        Assert.assertEquals("userId", argumentCaptor.getValue().get(0).getUserId());
        Assert.assertEquals(EventType.APPROVAL.toString(), argumentCaptor.getValue().get(0).getEventType());
    }


    private Requirement createRequirement(String state, boolean current) {
        Requirement requirement = new Requirement();
        requirement.setFsn("fsn1");
        requirement.setState(state);
        requirement.setEnabled(true);
        requirement.setCurrent(current);
        requirement.setWarehouse("dummy_warehouse");
        requirement.setCreatedAt(new Date());
        requirement.setUpdatedAt(new Date());
        Group group = TestHelper.getGroup("test_group");
        group.setId(256l);
        RequirementSnapshot requirementSnapshot = new RequirementSnapshot();
        requirementSnapshot.setGroup(group);
        requirement.setRequirementSnapshot(requirementSnapshot);
        return requirement;
    }

    //TODO: legacy code

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }



}
