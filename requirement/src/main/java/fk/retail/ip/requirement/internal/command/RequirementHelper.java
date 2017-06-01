package fk.retail.ip.requirement.internal.command;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import fk.retail.ip.requirement.internal.entities.ProductInfo;
import fk.retail.ip.requirement.internal.entities.Requirement;
import fk.retail.ip.requirement.internal.repository.ProductInfoRepository;
import fk.retail.ip.requirement.internal.repository.WarehouseSupplierSlaRepository;
import fk.retail.ip.requirement.model.RequirementUploadLineItem;
import fk.retail.ip.ssl.client.SslClient;
import fk.retail.ip.ssl.config.SslClientConfiguration;
import fk.retail.ip.ssl.model.SupplierSelectionRequest;
import fk.retail.ip.ssl.model.SupplierSelectionResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
public class RequirementHelper {

    private final WarehouseSupplierSlaRepository warehouseSupplierSlaRepository;
    private final ProductInfoRepository productInfoRepository;
    private final SslClient sslClient;
    private final SslClientConfiguration sslClientConfiguration;

    @Inject
    public RequirementHelper (WarehouseSupplierSlaRepository warehouseSupplierSlaRepository,
                              ProductInfoRepository productInfoRepository, SslClient sslClient,
                              SslClientConfiguration sslClientConfiguration) {
        this.warehouseSupplierSlaRepository = warehouseSupplierSlaRepository;
        this.productInfoRepository = productInfoRepository;
        this.sslClient = sslClient;
        this.sslClientConfiguration = sslClientConfiguration;
    }

    //TODO: optimize this
    public int getSla(String vertical, String warehouse, String supplier, int apiSla) {
        if (vertical == null || warehouse == null) {
            return apiSla;
        }
        try {
            Optional<Integer> sla = warehouseSupplierSlaRepository.getSla(vertical, warehouse, supplier);
            if (sla.isPresent()) {
                return sla.get();
            } else {
                return apiSla;
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return apiSla;
        }
    }

    public List<SupplierSelectionRequest> createSupplierSelectionRequest(List<Requirement> requirements) {
        List<SupplierSelectionRequest> requests = Lists.newArrayList();
        requirements.forEach(req -> {
            SupplierSelectionRequest request = new SupplierSelectionRequest();
            request.setFsn(req.getFsn());
            request.setSku("SKU0000000000000");
            request.setOrderType(req.getProcType());
            request.setQuantity((int) req.getQuantity());
            request.setEntityType("Requirement");
            request.setWarehouseId(req.getWarehouse());
            request.setTenantId("FKI");
            DateTime date = DateTime.now();
            request.setRequiredByDate(date.toString());
            requests.add(request);
        });
        return requests;
    }

    public Map<String, String> createFsnVerticalMap(Set<String> fsns) {
        List<ProductInfo> productInfos = productInfoRepository.getProductInfo(fsns);
        Map<String, String> fsnToVerticalMap = productInfos.stream().collect(Collectors.toMap(ProductInfo::getFsn, ProductInfo::getVertical, (k1, k2) -> k1));
        return fsnToVerticalMap;
    }

    public MultiKeyMap<String, SupplierSelectionResponse> createFsnWhSupplierMap(List<Requirement> supplierOverriddenRequirements) {
        MultiKeyMap<String,SupplierSelectionResponse> fsnWhSupplierMap = null;
        try {
            fsnWhSupplierMap = getSSLResponseMap(supplierOverriddenRequirements);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return fsnWhSupplierMap;
    }

    public List<Requirement> getSupplierOverriddenRequirement(List<RequirementUploadLineItem> requirementUploadLineItems,
                                                               Map<String, Requirement> requirementMap) {
        List<Requirement> requirements = Lists.newArrayList();
        for(RequirementUploadLineItem row : requirementUploadLineItems) {
            String requirementId = row.getRequirementId();
            if(requirementMap.containsKey(requirementId)) {
                String overridenSupplier = row.getCdoSupplierOverride();
                String supplierOverrideReason = row.getCdoSupplierOverrideReason();
                String currentSupplier = row.getSupplier();

                if (overridenSupplier!=null && !overridenSupplier.equals(currentSupplier) && !isEmptyString(overridenSupplier) && !isEmptyString(supplierOverrideReason)) {
                    requirements.add(requirementMap.get(requirementId));
                }
            }
        }
        return requirements;
    }

    public MultiKeyMap<String,SupplierSelectionResponse> getSSLResponseMap (List<Requirement> requirements) throws ExecutionException, InterruptedException {
        log.info("Supplier Override request received for "+ requirements.size() + "number of events");
        List<SupplierSelectionRequest> requests = createSupplierSelectionRequest(requirements);
        List<SupplierSelectionResponse> supplierSelectionResponses = sslClient.getSupplierSelectionResponse(requests);
        MultiKeyMap<String, SupplierSelectionResponse> supplierSelectionResponseMap  = new MultiKeyMap<>();
        supplierSelectionResponses.forEach( response -> {
            supplierSelectionResponseMap.put(response.getFsn(),response.getWarehouseId(), response);
        });
        return supplierSelectionResponseMap;
    }

    private boolean isEmptyString(String comment) {
        return comment == null || comment.trim().isEmpty() ? true : false;
    }


}
