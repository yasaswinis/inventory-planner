package fk.retail.ip.d42.config;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import fk.retail.ip.d42.client.D42Client;
import fk.retail.ip.d42.client.D42ClientImpl;

/**
 * Created by harshul.jain on 24/04/17.
 */
public class D42ClientModule extends AbstractModule{

    @Override
    protected void configure() {
        bind(D42Client.class).to(D42ClientImpl.class);
    }
}
