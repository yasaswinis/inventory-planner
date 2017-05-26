package fk.retail.ip.email.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Created by agarwal.vaibhav on 08/05/17.
 */
@Data
public class ConnektConfiguration {
    String url;
    @JsonProperty("x-api-key")
    String apiKey;
}
