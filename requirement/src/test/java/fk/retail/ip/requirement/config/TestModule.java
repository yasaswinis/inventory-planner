
package fk.retail.ip.requirement.config;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import fk.sp.common.extensions.jpa.JpaWithTestDbModule;
public class TestModule extends AbstractModule {

  @Override
  protected void configure() {

    install(new RequirementModule());
    install(new JpaWithTestDbModule("main", Lists.newArrayList("fk.retail.ip.requirement.internal.entities","fk.retail.ip.requirement.sql")));
  }

}