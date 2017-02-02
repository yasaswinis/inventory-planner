package fk.retail.ip.requirement.internal.repository;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Provider;
import javax.persistence.EntityManager;

import fk.retail.ip.requirement.internal.entities.FsnBand;
import fk.sp.common.extensions.jpa.Page;
import fk.sp.common.extensions.jpa.PageRequest;
import fk.sp.common.extensions.jpa.SimpleJpaGenericRepository;

/**
 * Created by nidhigupta.m on 26/01/17.
 */
public class FsnBandRepository extends SimpleJpaGenericRepository<FsnBand, Long> {

    protected static final int pageSize = 20;

    @Inject
    public FsnBandRepository(Provider<EntityManager> entityManagerProvider) {
        super(entityManagerProvider);
    }

    public List<FsnBand> fetchBandDataForFSNs(Set<String> fsns) {
        List<FsnBand> fsnBands = Lists.newArrayList();
        int pageNo = 0;
        PageRequest pageRequest = getPageRequest(pageNo, pageSize);
        Map<String, Object> params = Maps.newHashMap();
        params.put("fsns", fsns);
        Page<FsnBand> page =  findAllByNamedQuery("fetchBandDataForFSNs", params, pageRequest);
        fsnBands.addAll(page.getContent());
        if (page.isHasMore()) {
            pageNo += 1;
            pageRequest = getPageRequest(pageNo, pageSize);
            page =  findAllByNamedQuery("fetchBandDataForFSNs", params, pageRequest);
            fsnBands.addAll(page.getContent());
        }
        return fsnBands;
    }

    private PageRequest getPageRequest(int pageNo, int pageSize){
        PageRequest pageRequest = PageRequest
                .builder()
                .pageNumber(pageNo)
                .pageSize(pageSize)
                .build();

        return pageRequest;
    }


}