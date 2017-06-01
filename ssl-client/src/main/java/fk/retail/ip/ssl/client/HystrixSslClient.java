package fk.retail.ip.ssl.client;

import com.google.common.collect.Lists;
import com.google.inject.name.Named;
import fk.retail.ip.ssl.config.SslClientConfiguration;
import fk.retail.ip.ssl.internal.command.GetSupplierDetailsCommand;
import fk.retail.ip.ssl.model.SupplierSelectionRequest;

import javax.inject.Provider;
import com.google.inject.Inject;
import fk.retail.ip.ssl.model.SupplierSelectionResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.*;

/**
 * Created by yogeshwari.k on 01/03/17.
 */
@Slf4j
public class HystrixSslClient implements SslClient{

    private final Provider<GetSupplierDetailsCommand> getSupplierDetailsCommandProvider;
    private final SslClientConfiguration sslClientConfiguration;
    private final ExecutorService executorService;

    @Inject
    public HystrixSslClient(Provider<GetSupplierDetailsCommand> getSupplierDetailsCommandProvider,
                            SslClientConfiguration sslClientConfiguration,
                            @Named("inventory-planner") ExecutorService executorService) {
        this.getSupplierDetailsCommandProvider = getSupplierDetailsCommandProvider;
        this.sslClientConfiguration = sslClientConfiguration;
        this.executorService = executorService;
    }

    public List<SupplierSelectionResponse> getSupplierSelectionResponseBatch(List<SupplierSelectionRequest> requests) {
        return getSupplierDetailsCommandProvider.get()
                .withSslRequests(requests)
                .execute();
    }

    @Override
    public List<SupplierSelectionResponse> getSupplierSelectionResponse(List<SupplierSelectionRequest> requests)
            throws ExecutionException, InterruptedException {
        List<Future<List<SupplierSelectionResponse>>> futureList = Lists.newArrayList();
        List<SupplierSelectionResponse> supplierSelectionResponses = Lists.newArrayList();
        int threadCount = 0;
        for(List<SupplierSelectionRequest> requestList : Lists.partition(requests, sslClientConfiguration.getBatchSize())) {
            Callable<List<SupplierSelectionResponse>> callable = null;
            callable = () -> {
                List<SupplierSelectionResponse> supplierSelectionResponseList =  getSupplierSelectionResponseBatch(requestList);
                return supplierSelectionResponseList;
            };
            Future<List<SupplierSelectionResponse>> futureResponse = executorService.submit(callable);
            threadCount = threadCount+1;
            log.debug("Thread number:" + threadCount);
            futureList.add(futureResponse);
        }
        for(Future<List<SupplierSelectionResponse>> future : futureList) {
            supplierSelectionResponses.addAll(future.get());
        }
        return supplierSelectionResponses;
    }

}
