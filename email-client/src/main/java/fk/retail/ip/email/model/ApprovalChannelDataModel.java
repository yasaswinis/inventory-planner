package fk.retail.ip.email.model;

import lombok.Data;

/**
 * Created by agarwal.vaibhav on 08/05/17.
 */
@Data
public class ApprovalChannelDataModel extends ChannelDataModel{
    String groupName;
    String userName;
    String timestamp;
    String link;
}
