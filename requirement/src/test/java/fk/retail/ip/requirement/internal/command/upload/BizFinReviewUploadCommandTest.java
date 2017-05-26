package fk.retail.ip.requirement.internal.command.upload;

import com.google.common.collect.Lists;
import fk.retail.ip.requirement.config.TestModule;
import fk.retail.ip.requirement.internal.command.FdpRequirementIngestorImpl;
import fk.retail.ip.requirement.internal.entities.Requirement;
import fk.retail.ip.requirement.internal.entities.RequirementEventLog;
import fk.retail.ip.requirement.internal.entities.RequirementSnapshot;
import fk.retail.ip.requirement.internal.enums.EventType;
import fk.retail.ip.requirement.internal.enums.FdpRequirementEventType;
import fk.retail.ip.requirement.internal.enums.OverrideKey;
import fk.retail.ip.requirement.internal.enums.RequirementApprovalState;
import fk.retail.ip.requirement.internal.repository.RequirementEventLogRepository;
import fk.retail.ip.requirement.internal.repository.TestHelper;
import fk.retail.ip.requirement.model.RequirementUploadLineItem;
import fk.retail.ip.requirement.model.UploadOverrideFailureLineItem;
import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by agarwal.vaibhav on 03/03/17.
 */

@RunWith(JukitoRunner.class)
@UseModules(TestModule.class)
public class BizFinReviewUploadCommandTest {

    @InjectMocks
    BizFinReviewUploadCommand bizFinReviewUploadCommand;

    @Mock
    FdpRequirementIngestorImpl fdpRequirementIngestor;

    @Mock
    RequirementEventLogRepository requirementEventLogRepository;

    @Captor
    private ArgumentCaptor<List<RequirementEventLog>> argumentCaptor;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void uploadTest() throws IOException {
        List<RequirementUploadLineItem> requirementUploadLineItems = TestHelper.getBizfinReviewUploadLineItem();
        List<Requirement> requirements = getRequirements();
        List<UploadOverrideFailureLineItem> uploadOverrideFailureLineItems = bizFinReviewUploadCommand
                .execute(requirementUploadLineItems, requirements, "",
                        RequirementApprovalState.BIZFIN_REVIEW.toString()).getUploadOverrideFailureLineItemList();

        Mockito.verify(requirementEventLogRepository).persist(argumentCaptor.capture());

        Map<String, Requirement> requirementMap = requirements.stream().collect
                (Collectors.toMap(Requirement::getId, Function.identity()));

        Assert.assertEquals(2, uploadOverrideFailureLineItems.size());
        Assert.assertEquals(20, (int)requirementMap.get("1").getQuantity());
        Assert.assertEquals("{\"quantityOverrideComment\":\"test_bizfin\"}", requirementMap.get("1").getOverrideComment());
        Assert.assertEquals(100, (int)requirementMap.get("2").getQuantity());
        Assert.assertEquals("{\"quantityOverrideComment\":\"test_bizfin\"}", requirementMap.get("3").getOverrideComment());
        Assert.assertEquals(100, (int)requirementMap.get("3").getQuantity());
        Assert.assertEquals(100, (int)requirementMap.get("4").getQuantity());
        Assert.assertEquals(100, (int)requirementMap.get("5").getQuantity());

        Assert.assertEquals(OverrideKey.QUANTITY.toString(), argumentCaptor.getValue().get(0).getAttribute());
        Assert.assertEquals("100.0", argumentCaptor.getValue().get(0).getOldValue());
        Assert.assertEquals("20", argumentCaptor.getValue().get(0).getNewValue());
        Assert.assertEquals("test_bizfin", argumentCaptor.getValue().get(0).getReason());
        Assert.assertEquals(EventType.OVERRIDE.toString(), argumentCaptor.getValue().get(0).getEventType());

        Assert.assertEquals(OverrideKey.OVERRIDE_COMMENT.toString(), argumentCaptor.getValue().get(1).getAttribute());
        Assert.assertNull(argumentCaptor.getValue().get(1).getOldValue());
        Assert.assertEquals("test_bizfin", argumentCaptor.getValue().get(1).getNewValue());
        Assert.assertEquals(FdpRequirementEventType.BIZFIN_COMMENT_RECOMMENDATION.toString(),
                argumentCaptor.getValue().get(1).getReason());
        Assert.assertEquals(EventType.OVERRIDE.toString(), argumentCaptor.getValue().get(0).getEventType());

    }

    private List<Requirement> getRequirements() {

        RequirementSnapshot snapshot = TestHelper.getRequirementSnapshot("[1,2]", 2, 3, 4, 5, 6);

        RequirementSnapshot snapshot1 = TestHelper.getRequirementSnapshot("[3,4]",7,8,9,10,11);

        List<Requirement> requirements = Lists.newArrayList();

        Requirement requirement = TestHelper.getRequirement(
                "fsn",
                "dummy_warehouse_1",
                RequirementApprovalState.BIZFIN_REVIEW.toString(),
                true,
                snapshot,
                100,
                "ABC",
                100,
                101,
                "INR",
                3,
                "",
                "Daily planning"
        );
        requirement.setId("1");
        requirements.add(requirement);

        requirement = TestHelper.getRequirement(
                "fsn",
                "dummy_warehouse_2",
                RequirementApprovalState.BIZFIN_REVIEW.toString(),
                true,
                snapshot1,
                100,
                "DEF",
                10,
                9,
                "USD",
                4,
                "",
                "Daily planning"
        );
        requirement.setId("2");
        requirements.add(requirement);

        requirement = TestHelper.getRequirement(
                "fsn_1",
                "dummy_warehouse_1",
                RequirementApprovalState.BIZFIN_REVIEW.toString(),
                true,
                snapshot1,
                100,
                "DEF",
                10,
                9,
                "USD",
                4,
                "",
                "Daily planning"
        );
        requirement.setId("3");
        requirements.add(requirement);

        requirement = TestHelper.getRequirement(
                "fsn_1",
                "dummy_warehouse_2",
                RequirementApprovalState.BIZFIN_REVIEW.toString(),
                true,
                snapshot1,
                100,
                "DEF",
                10,
                9,
                "USD",
                4,
                "",
                "Daily planning"
        );
        requirement.setId("4");
        requirements.add(requirement);

        requirement = TestHelper.getRequirement(
                "fsn_1",
                "dummy_warehouse_2",
                RequirementApprovalState.BIZFIN_REVIEW.toString(),
                true,
                snapshot1,
                100,
                "DEF",
                10,
                9,
                "USD",
                4,
                "",
                "Daily planning"
        );
        requirement.setId("5");
        requirements.add(requirement);

        return requirements;
    }
}
