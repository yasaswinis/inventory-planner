package fk.retail.ip.email.client;

import fk.retail.ip.email.model.ConnektPayload;

/**
 * Created by agarwal.vaibhav on 05/05/17.
 */
public interface ConnektClient {
    void sendEmail(ConnektPayload connektPayload);
}
