package fk.retail.ip.requirement.model;

import io.dropwizard.jackson.JsonSnakeCase;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.Getter;

/**
 * Created by nidhigupta.m on 26/01/17.
 */
@Data

@Getter
public class DownloadRequirementRequest {

    List<Long> requirementIds;
    boolean lastAppSupplierRequired;
    String state;
    Map<String, Object> filters;
}
