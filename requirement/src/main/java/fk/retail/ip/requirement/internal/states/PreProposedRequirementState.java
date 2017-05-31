package fk.retail.ip.requirement.internal.states;

import com.google.inject.Inject;
import com.google.inject.Provider;
import fk.retail.ip.requirement.internal.command.download.DownloadPreProposedCommand;
import fk.retail.ip.requirement.internal.entities.Requirement;
import fk.retail.ip.requirement.model.RequirementUploadLineItem;
import fk.retail.ip.requirement.model.UploadOverrideResult;

import javax.ws.rs.core.StreamingOutput;
import java.util.List;

/**
 * Created by yogeshwari.k on 23/02/17.
 */
public class PreProposedRequirementState implements RequirementState {

    private final Provider<DownloadPreProposedCommand> downloadPreProposedCommandProvider;

    @Inject
    public PreProposedRequirementState(Provider<DownloadPreProposedCommand> downloadPreProposedCommandProvider) {
        this.downloadPreProposedCommandProvider = downloadPreProposedCommandProvider;
    }

    @Override
    public StreamingOutput download(List<Requirement> requirements, boolean isLastAppSupplierRequired, String state) {
        return downloadPreProposedCommandProvider.get().execute(requirements, isLastAppSupplierRequired,state);
    }

    @Override
    public UploadOverrideResult upload(List<Requirement> requirements,
                                       List<RequirementUploadLineItem> parsedJson,
                                       String userId, String state) {
        throw new UnsupportedOperationException("Invalid operation");
    }

}
