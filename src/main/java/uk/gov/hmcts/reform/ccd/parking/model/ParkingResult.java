package uk.gov.hmcts.reform.ccd.parking.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

@Data
@Slf4j
public class ParkingResult {

    private Set<Long> caseReferencesToPark = new HashSet<>();
    private Set<Long> caseReferencesToUnpark = new HashSet<>();
    private Set<ParkingError> errors = new LinkedHashSet<>();
    private String statePrefix;

    public ParkingResult(String statePrefix) {
        this.statePrefix = statePrefix;
    }

    public void logErrors() {
        getErrors().forEach(error -> {
            if (error.getRecordIndex() == null) {
                log.error(error.getErrorMessage());
            } else {
                log.error("[Record #{}] Case: {}. {}", error.getRecordIndex(), error.getReference(), error.getErrorMessage());
            }
        });
    }
}
