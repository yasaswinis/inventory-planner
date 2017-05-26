package fk.retail.ip.email.internal.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import fk.retail.ip.email.configuration.ConnektConfiguration;
import fk.retail.ip.email.model.ConnektPayload;
import fk.retail.ip.email.internal.Constants;
import fk.sp.common.extensions.dropwizard.jersey.NoAuthClient;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

/**
 * Created by agarwal.vaibhav on 08/05/17.
 */
@Slf4j
public class SendEmailCommand extends BaseEmailCommand {

    private ConnektPayload connektPayload;

    @Inject
    public SendEmailCommand(@NoAuthClient Client client, ConnektConfiguration connektConfiguration) {
        super(client, connektConfiguration);
    }

    @Override
    protected Object run() throws Exception {
        connektPayload.setSla(Constants.CONNEKT_PAYLOAD_SLA);
        WebTarget webTarget = client.target(connektConfiguration.getUrl());
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(connektPayload);
        Response response = webTarget
                .request()
                .header("x-api-key", connektConfiguration.getApiKey())
                .post(Entity.json(json));

        if (response.getStatus() != Response.Status.ACCEPTED.getStatusCode()) {
            log.info("error sending email");
        }
        return null;
    }

    public SendEmailCommand withConnektPayload(ConnektPayload payload) {
        this.connektPayload = payload;
        return this;
    }
}
