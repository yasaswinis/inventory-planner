package fk.retail.ip.requirement.internal.repository;

import com.google.inject.Inject;
import fk.retail.ip.requirement.internal.entities.RequirementEventLog;
import fk.sp.common.extensions.jpa.SimpleJpaGenericRepository;

import javax.inject.Provider;
import javax.persistence.EntityManager;

/**
 * Created by agarwal.vaibhav on 18/04/17.
 */
public class JPARequirementEventLogRepository extends SimpleJpaGenericRepository<RequirementEventLog, Long> implements RequirementEventLogRepository {

    @Inject
    public JPARequirementEventLogRepository(Provider<EntityManager> entityManagerProvider) {
        super(entityManagerProvider);
    }
}
