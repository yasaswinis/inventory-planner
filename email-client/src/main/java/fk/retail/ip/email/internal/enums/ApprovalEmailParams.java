package fk.retail.ip.email.internal.enums;

/**
 * Created by agarwal.vaibhav on 11/05/17.
 */
public enum ApprovalEmailParams implements EmailParams {
    USERNAME("userName"),
    USER("user"),
    GROUPNAME("groupName"),
    TIMESTAMP("timestamp"),
    LINK("link");

    public String param;

    private ApprovalEmailParams(String param) {
        this.param = param;
    }

    @Override
    public String toString() {
        return param;
    }
}
