package fk.retail.ip.requirement.internal.command.emailHelper;

import fk.retail.ip.email.client.ConnektClient;
import fk.retail.ip.email.internal.enums.EmailParams;
import fk.retail.ip.email.internal.repository.EmailDetailsRepository;
import fk.retail.ip.email.model.EmailDetails;
import fk.retail.ip.email.model.StencilConfigModel;

import java.util.Map;

/**
 * Created by agarwal.vaibhav on 12/05/17.
 */
public abstract class SendEmail {
    private EmailDetailsRepository emailDetailsRepository;
    protected ConnektClient connektClient;

    public SendEmail(EmailDetailsRepository emailDetailsRepository, ConnektClient connektClient) {
        this.emailDetailsRepository = emailDetailsRepository;
        this.connektClient = connektClient;
    }

    abstract void send(Map<EmailParams, String> params, String state, boolean forward, StencilConfigModel stencilConfigModel);

    public EmailDetails getEmailDetails(String stencilId, String groupName) {
        EmailDetails emailDetails = emailDetailsRepository.getEmailDetails(stencilId, groupName);
        return emailDetails;
    }
}