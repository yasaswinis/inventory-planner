package fk.retail.ip.d42.client;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.google.common.base.Objects;
import com.google.inject.Inject;
import fk.retail.ip.d42.config.D42Configuration;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;


/**
 * Created by harshul.jain on 24/04/17.
 */
@Slf4j

public class D42ClientImpl implements D42Client{
    private AmazonS3 d42Client;
    private String DEFAULT_CONTENT_TYPE = "application/octet-stream";


    @Inject
    public D42ClientImpl(D42Configuration d42Configuration) {
        AWSCredentials credentials = new BasicAWSCredentials(d42Configuration.getAccessKey(), d42Configuration.getSecretKey());
        ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setProtocol(Protocol.HTTP);
        d42Client = new AmazonS3Client(credentials, clientConfig);
        d42Client.setEndpoint(d42Configuration.getUrl());
        d42Client.setS3ClientOptions(new S3ClientOptions().withPathStyleAccess(true));
    }

    public void put(String bucketName, String key, InputStream inputStream, String contentType) {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        contentType = Objects.firstNonNull(contentType, DEFAULT_CONTENT_TYPE);
        objectMetadata.setContentType(contentType);
        try {
            PutObjectResult putObjectResult = d42Client.putObject(bucketName, key, inputStream, objectMetadata);
        } catch (Exception e) {
            log.error("Unable to write file to D42", e.getStackTrace());
        } finally {
            log.info("File written to D42: ", key);
        }
    }
}
