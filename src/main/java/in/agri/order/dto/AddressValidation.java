package in.agri.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressValidation {

    private Boolean valid;
    private String line1;
    private String line2;
    private String landmark;
    private String city;
    private String pincode;
    private String missing;
}
