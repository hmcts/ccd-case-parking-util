package uk.gov.hmcts.reform.ccd.parking.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParkingError {

    private Integer recordIndex;
    private Long reference;
    private String errorMessage;
}
