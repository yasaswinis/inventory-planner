package fk.retail.ip.manager.config;

import fk.retail.ip.d42.config.D42Configuration;
import fk.retail.ip.email.configuration.ConnektConfiguration;
import fk.retail.ip.fdp.config.FdpConfiguration;
import fk.retail.ip.proc.config.ProcClientConfiguration;
import fk.retail.ip.requirement.config.EmailConfiguration;
import fk.retail.ip.requirement.config.TriggerRequirementConfiguration;
import fk.retail.ip.ssl.config.SslClientConfiguration;
import fk.retail.ip.zulu.config.ZuluConfiguration;
import fk.sp.common.extensions.dropwizard.db.HasDataSourceFactory;
import flipkart.retail.server.admin.config.RotationManagementConfig;
import io.dropwizard.Configuration;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.db.DataSourceFactory;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@EqualsAndHashCode(callSuper = false)
@Data
public class ManagerConfiguration extends Configuration implements HasDataSourceFactory {

    private DataSourceFactory dataSource;

    @Valid
    @NotNull
    private RotationManagementConfig rotationManagementConfig;

    @NotNull
    private ZuluConfiguration zuluConfiguration;

    @NotNull
    private SslClientConfiguration sslClientConfiguration;

    @NotNull
    private ProcClientConfiguration procClientConfiguration;

    @NotNull
    private TriggerRequirementConfiguration triggerRequirementConfiguration;

    @NotNull
    private FdpConfiguration fdpConfiguration;

    @NotNull
    private D42Configuration d42Configuration;

    @Valid
    @NotNull
    private JerseyClientConfiguration clientConfiguration;

    @Override
    public DataSourceFactory getDatabaseConfiguration() {
        return dataSource;
    }

    @NotNull
    @Valid
    public ConnektConfiguration connektConfiguration;

    @NotNull
    public EmailConfiguration emailConfiguration;
}
