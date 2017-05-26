package fk.retail.ip.email.model;

import lombok.Data;

/**
 * Created by agarwal.vaibhav on 08/05/17.
 */
@Data
public class ConnektPayload {

    String sla;
    String contextId;
    String stencilId;
    ChannelInfo channelInfo;
    ChannelDataModel channelDataModel;

}
