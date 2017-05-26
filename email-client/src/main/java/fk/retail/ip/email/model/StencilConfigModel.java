package fk.retail.ip.email.model;

import lombok.Data;

import java.util.Map;

/**
 * Created by agarwal.vaibhav on 12/05/17.
 */
@Data
public class StencilConfigModel {
    Map<String, Map<String, String>> stateStencilMapping;
}
