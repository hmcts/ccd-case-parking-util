package uk.gov.hmcts.reform.ccd.parking.model;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class ErrorInput {

    private ParkingError.ParkingErrorBuilder errorBuilder;
    private Set<ParkingError> allErrors;

    public void addError(String errorMessage) {
        getAllErrors().add(getErrorBuilder()
            .errorMessage(errorMessage)
            .build());
    }
}
