package fk.retail.ip.email.client;

import com.google.inject.Inject;
import fk.retail.ip.email.model.ConnektPayload;
import fk.retail.ip.email.internal.command.SendEmailCommand;

/**
 * Created by agarwal.vaibhav on 05/05/17.
 */
public class HystrixConnektClient implements ConnektClient {

    private final SendEmailCommand sendEmailCommand;

    @Inject
    public HystrixConnektClient(SendEmailCommand sendEmailCommand) {
        this.sendEmailCommand = sendEmailCommand;
    }

    @Override
    public void sendEmail(ConnektPayload connektPayload) {
        sendEmailCommand.withConnektPayload(connektPayload).queue();
    }
}
