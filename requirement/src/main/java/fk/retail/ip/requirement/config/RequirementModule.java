package fk.retail.ip.requirement.config;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import fk.retail.ip.requirement.internal.repository.*;
import fk.retail.ip.requirement.resource.RequirementResource;
import fk.retail.ip.requirement.resource.TestResource;

/**
 * Created by nidhigupta.m on 26/01/17.
 */
public class RequirementModule extends AbstractModule {

    @Override
    protected void configure() {

        bind(RequirementResource.class);
        bind(TestResource.class);
        bind(FsnBandRepository.class).to(JPAFsnBandRepository.class);
        bind(WeeklySaleRepository.class).to(JPAWeeklySaleRepository.class);
        bind(RequirementRepository.class).to(JPARequirementRepository.class);
        bind(LastAppSupplierRepository.class).to(JPALastAppSupplierRepository.class);
        bind(IwtRequestItemRepository.class).to(JpaIwtRequestItemRepository.class);
        bind(OpenRequirementAndPurchaseOrderRepository.class).to(JPAOpenRequirementAndPurchaseOrderRepository.class);
        bind(PolicyRepository.class).to(JPAPolicyRepository.class);
        bind(GroupFsnRepository.class).to(JPAGroupFsnRepository.class);
        bind(ProductInfoRepository.class).to(JPAProductInfoRepository.class);
        bind(WarehouseRepository.class).to(JPAWarehouseRepository.class);
        bind(WarehouseInventoryRepository.class).to(JPAWarehouseInventoryRepository.class);
        bind(ForecastRepository.class).to(JPAForecastRepository.class);
        bind(ProcPurchaseOrderRepository.class).to(ProcPurchaseOrderRepositoryImpl.class);
        bind(WarehouseSupplierSlaRepository.class).to(WarehouseSupplierSlaRepositoryImpl.class);
        bind(RequirementApprovalTransitionRepository.class).to(JPARequirementApprovalTransitionRepository.class);
        bind(RequirementEventLogRepository.class).to(JPARequirementEventLogRepository.class);
        //TODO:remove
        bind(ProjectionRepository.class).to(ProjectionRepositoryImpl.class);

        bind(String.class).annotatedWith(Names.named("actionConfiguration")).toInstance("/requirement-state-actions.json");
    }
}
