package fk.retail.ip.email.internal.repository;

import com.google.inject.Inject;
import fk.retail.ip.email.model.EmailDetails;
import fk.sp.common.extensions.jpa.SimpleJpaGenericRepository;

import javax.inject.Provider;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * Created by agarwal.vaibhav on 09/05/17.
 */
public class JPAEmailDetailsRepository extends SimpleJpaGenericRepository<EmailDetails, Long> implements EmailDetailsRepository {

    @Inject
    public JPAEmailDetailsRepository(Provider<EntityManager> entityManagerProvider) {
        super(entityManagerProvider);
    }

    public EmailDetails getEmailDetails(String stencilId, String groupName) {
        TypedQuery<EmailDetails> query = getEntityManager().createNamedQuery("findEmailDetailsByStencilId", EmailDetails.class);
        query.setParameter("stencilId", stencilId);
        query.setParameter("groupName", groupName);
        List<EmailDetails> emailDetailsList;
        emailDetailsList = query.getResultList();
        if (emailDetailsList.isEmpty()) {
            return null;
        } else {
            EmailDetails emailDetails = emailDetailsList.get(0);
            return emailDetails;
        }

    }
}
