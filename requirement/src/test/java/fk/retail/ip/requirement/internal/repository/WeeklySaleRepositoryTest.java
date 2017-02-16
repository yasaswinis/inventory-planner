package fk.retail.ip.requirement.internal.repository;

import com.google.inject.Inject;
import fk.retail.ip.requirement.config.TestModule;
import fk.retail.ip.requirement.internal.entities.WeeklySale;
import fk.sp.common.extensions.jpa.TransactionalJpaRepositoryTest;
import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 * Created by nidhigupta.m on 15/02/17.
 */

@RunWith(JukitoRunner.class)
@UseModules(TestModule.class)
public class WeeklySaleRepositoryTest extends TransactionalJpaRepositoryTest {

    @Inject
    WeeklySaleRepository weeklySaleRepository;

    @Test
    public void fetchWeeklySalesForFsnsTest() {
        WeeklySale weeklySale = getWeeklySale();
        weeklySaleRepository.persist(weeklySale);
        List<WeeklySale> weeklySaleList = weeklySaleRepository.fetchWeeklySalesForFsns(new HashSet<String>(Arrays.asList("fsn")));
        Assert.assertEquals(weeklySale, weeklySaleList.get(0));
    }

    private WeeklySale getWeeklySale() {

        WeeklySale weeklySale = new WeeklySale("fsn","wh1", 3, 90);
        weeklySale.setCreatedAt(new Date());
        return weeklySale;
    }
}