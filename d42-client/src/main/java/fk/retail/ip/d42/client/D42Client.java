package fk.retail.ip.d42.client;

import java.io.InputStream;

/**
 * Created by harshul.jain on 24/04/17.
 */
public interface D42Client {
    public void put(String bucketName, String key, InputStream inputStream, String contentType);
}
