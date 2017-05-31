package fk.retail.ip.requirement.internal.states;

import com.google.inject.Inject;
import com.google.inject.Provider;
import fk.retail.ip.requirement.internal.command.download.DownloadIPCReviewCommand;
import fk.retail.ip.requirement.internal.entities.Requirement;
import fk.retail.ip.requirement.model.RequirementUploadLineItem;
import fk.retail.ip.requirement.model.UploadOverrideResult;

import javax.ws.rs.core.StreamingOutput;
import java.util.List;

/**
 * Created by nidhigupta.m on 21/02/17.
 */
public class IPCReviewRequirementState implements RequirementState {
    private final Provider<DownloadIPCReviewCommand> downloadIPCReviewCommandProvider;

    @Inject
    public IPCReviewRequirementState(Provider<DownloadIPCReviewCommand> downloadIPCReviewCommandProvider) {
        this.downloadIPCReviewCommandProvider = downloadIPCReviewCommandProvider;
    }

    @Override
    public StreamingOutput download(List<Requirement> requirements, boolean isLastAppSupplierRequired, String state) {
        return downloadIPCReviewCommandProvider.get().execute(requirements, isLastAppSupplierRequired,state);
    }

    @Override
    public UploadOverrideResult upload(List<Requirement> requirements,
                                       List<RequirementUploadLineItem> parsedJson,
                                       String userId, String state) {
        throw new UnsupportedOperationException("Invalid Operation");
    }

}

