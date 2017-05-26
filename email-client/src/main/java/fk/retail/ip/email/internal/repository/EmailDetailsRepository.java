package fk.retail.ip.email.internal.repository;

import fk.retail.ip.email.model.EmailDetails;
import fk.sp.common.extensions.jpa.JpaGenericRepository;

/**
 * Created by agarwal.vaibhav on 09/05/17.
 */
public interface EmailDetailsRepository extends JpaGenericRepository<EmailDetails, Long> {
    EmailDetails getEmailDetails(String stencilId, String groupName);
}
