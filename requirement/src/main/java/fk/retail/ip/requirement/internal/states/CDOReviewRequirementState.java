package fk.retail.ip.requirement.internal.states;

import com.google.inject.Inject;
import com.google.inject.Provider;
import fk.retail.ip.requirement.internal.command.download.DownloadCDOReviewCommand;
import fk.retail.ip.requirement.internal.command.upload.CDOReviewUploadCommand;
import fk.retail.ip.requirement.internal.entities.Requirement;
import fk.retail.ip.requirement.model.RequirementUploadLineItem;
import fk.retail.ip.requirement.model.UploadOverrideResult;

import javax.ws.rs.core.StreamingOutput;
import java.util.List;

/**
 * Created by nidhigupta.m on 21/02/17.
 */
public class CDOReviewRequirementState implements RequirementState {
    private final Provider<DownloadCDOReviewCommand> downloadCDOReviewCommandProvider;
    private final Provider<CDOReviewUploadCommand> uploadCDOReviewCommandProvider;

    @Inject
    public CDOReviewRequirementState(Provider<DownloadCDOReviewCommand> downloadCDOReviewCommandProvider, Provider<CDOReviewUploadCommand> uploadCDOReviewCommandProvider) {
        this.downloadCDOReviewCommandProvider = downloadCDOReviewCommandProvider;
        this.uploadCDOReviewCommandProvider = uploadCDOReviewCommandProvider;
    }

    @Override
    public UploadOverrideResult upload(List<Requirement> requirements,
                                       List<RequirementUploadLineItem> parsedJson,
                                       String userId, String state) {
        return uploadCDOReviewCommandProvider.get().execute(parsedJson, requirements, userId, state);
    }

    @Override
    public StreamingOutput download(List<Requirement> requirements, boolean isLastAppSupplierRequired, String state) {
        return downloadCDOReviewCommandProvider.get().execute(requirements, isLastAppSupplierRequired, state);
    }

}
