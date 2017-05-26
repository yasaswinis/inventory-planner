package fk.retail.ip.ssl;

import fk.retail.ip.ssl.client.SslClient;
import fk.retail.ip.ssl.model.SupplierSelectionRequest;
import fk.retail.ip.ssl.model.SupplierSelectionResponse;

import java.util.List;
import java.util.concurrent.Callable;

public class SslClientCallable implements Callable {

    List<SupplierSelectionRequest> requests;
    private final SslClient sslClient;

    public SslClientCallable(SslClient sslClient, List<SupplierSelectionRequest> requests) {
        this.sslClient = sslClient;
        this.requests = requests;
    }

    @Override
    public List<SupplierSelectionResponse> call() throws Exception {
        List<SupplierSelectionResponse> supplierSelectionResponses =  sslClient.getSupplierSelectionResponse(requests);
        return supplierSelectionResponses;
    }

}
