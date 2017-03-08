package fk.retail.ip.requirement.internal.command.upload;

import com.google.common.collect.Lists;
import fk.retail.ip.requirement.config.TestModule;
import fk.retail.ip.requirement.internal.Constants;
import fk.retail.ip.requirement.internal.Constants1;
import fk.retail.ip.requirement.internal.entities.Requirement;
import fk.retail.ip.requirement.internal.entities.RequirementSnapshot;
import fk.retail.ip.requirement.internal.enums.RequirementApprovalState;
import fk.retail.ip.requirement.internal.repository.RequirementRepository;
import fk.retail.ip.requirement.internal.repository.TestHelper;
import fk.retail.ip.requirement.model.RequirementDownloadLineItem;
import fk.retail.ip.requirement.model.RequirementUploadLineItem;
import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.io.IOException;
import java.util.List;

/**
 * Created by agarwal.vaibhav on 02/03/17.
 */
@RunWith(JukitoRunner.class)
@UseModules(TestModule.class)
public class ProposedUploadCommandTest {

    @InjectMocks
    ProposedUploadCommand uploadProposedCommand;

    @Mock
    RequirementRepository requirementRepository;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void uploadTest() throws IOException {
        List<RequirementDownloadLineItem> requirementDownloadLineItems = TestHelper.getProposedRequirementDownloadLineItem();
        List<Requirement> requirements = getRequirements();
        List<RequirementUploadLineItem> requirementUploadLineItems = uploadProposedCommand.execute(requirementDownloadLineItems, requirements);
        ArgumentCaptor<Requirement> argumentCaptor = ArgumentCaptor.forClass(Requirement.class);
        Mockito.verify(requirementRepository, Mockito.times(1)).persist(argumentCaptor.capture());

        Assert.assertEquals(1, argumentCaptor.getAllValues().size());
        Assert.assertEquals(20, (int) argumentCaptor.getAllValues().get(0).getQuantity());
        Assert.assertEquals("{\"quantityOverrideComment\":\"test_ipc\"}", argumentCaptor.getAllValues().get(0).getOverrideComment());
        Assert.assertEquals(3, requirementUploadLineItems.size());
        Assert.assertEquals(Constants1.getKey(Constants.QUANTITY_OVERRIDE_COMMENT_IS_MISSING),
                requirementUploadLineItems.get(0).getFailureReason());
        Assert.assertEquals(Constants1.getKey(Constants.FSN_OR_WAREHOUSE_IS_MISSING),
                requirementUploadLineItems.get(1).getFailureReason());
        Assert.assertEquals(Constants1.getKey(Constants.SUGGESTED_QUANTITY_IS_NOT_GREATER_THAN_ZERO),
                requirementUploadLineItems.get(2).getFailureReason());


    }


    private List<Requirement> getRequirements() {

        RequirementSnapshot snapshot = TestHelper.getRequirementSnapshot("[1,2]", 2, 3, 4, 5, 6);

        RequirementSnapshot snapshot1 = TestHelper.getRequirementSnapshot("[3,4]",7,8,9,10,11);

        List<Requirement> requirements = Lists.newArrayList();

        Requirement requirement = TestHelper.getRequirement("dummy_fsn", "dummy_warehouse_1", RequirementApprovalState.PROPOSED.toString(), true, snapshot , 21, "ABC",
                100, 101, "INR", 3, "", "Daily planning");
        requirement.setId((long) 1);
        requirements.add(requirement);

        requirement = TestHelper.getRequirement("dummy_fsn", "dummy_warehouse_2", RequirementApprovalState.PROPOSED.toString(), true, snapshot1 , 22, "DEF",
                10, 9, "USD", 4, "", "Daily planning");
        requirement.setId((long) 2);
        requirements.add(requirement);

        requirement = TestHelper.getRequirement("dummy_fsn_1", "dummy_warehouse_1", RequirementApprovalState.PROPOSED.toString(), true, snapshot1 , 22, "DEF",
                10, 9, "USD", 4, "", "Daily planning");
        requirement.setId((long) 3);
        requirements.add(requirement);

        requirement = TestHelper.getRequirement("dummy_fsn_1", "dummy_warehouse_2", RequirementApprovalState.PROPOSED.toString(), true, snapshot1 , 22, "DEF",
                10, 9, "USD", 4, "", "Daily planning");
        requirement.setId((long) 4);
        requirements.add(requirement);


        return requirements;
    }

}
