package uk.gov.hmcts.reform.ccd.parking;

import com.opencsv.bean.CsvToBeanBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uk.gov.hmcts.reform.ccd.parking.config.ApplicationParams;
import uk.gov.hmcts.reform.ccd.parking.data.CaseDataEntity;
import uk.gov.hmcts.reform.ccd.parking.model.Action;
import uk.gov.hmcts.reform.ccd.parking.model.Case;
import uk.gov.hmcts.reform.ccd.parking.data.CaseDataRepository;
import uk.gov.hmcts.reform.ccd.parking.model.ParkingError;
import uk.gov.hmcts.reform.ccd.parking.model.ErrorInput;
import uk.gov.hmcts.reform.ccd.parking.model.ParkingResult;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.ccd.parking.model.Action.PARK;
import static uk.gov.hmcts.reform.ccd.parking.model.Action.UNPARK;

@SpringBootApplication
@Slf4j
public class Application implements CommandLineRunner {

    @Autowired
    private ApplicationParams applicationParams;

    @Autowired
    private CaseDataRepository caseDataRepository;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) throws FileNotFoundException {
        String caseListFile = applicationParams.getCaseListFile();
        log.info("START:: Processing file {}", caseListFile);

        List<Case> casesToUpdate = getCasesFromFile(caseListFile);

        ParkingResult parkingResult = new ParkingResult(applicationParams.getParkingStatePrefix());
        verifyNumberOfCases(casesToUpdate, ErrorInput.builder()
            .allErrors(parkingResult.getErrors())
            .errorBuilder(ParkingError.builder())
            .build());

        for (int i = 0; i < casesToUpdate.size(); i++) {
            processRecord(casesToUpdate.get(i), i, parkingResult);
        }

        processResult(parkingResult);
    }

    private List<Case> getCasesFromFile(String caseListFile) throws FileNotFoundException {
        return new CsvToBeanBuilder(new FileReader(caseListFile))
            .withType(Case.class)
            .build()
            .parse();
    }

    private void processRecord(Case caseToUpdate, int recordIndex, ParkingResult parkingResult) {
        Long caseReference = caseToUpdate.getCaseReferenceAsLong();

        ErrorInput errorInput = ErrorInput.builder()
            .allErrors(parkingResult.getErrors())
            .errorBuilder(ParkingError.builder()
                .recordIndex(recordIndex)
                .reference(caseReference))
            .build();

        if (!isValidCaseReference(caseReference, errorInput)
            || !isValidAction(caseToUpdate, errorInput)) {
            // Stop processing the record if there is already a fundamental issue prior to getting case details
            return;
        }

        Optional<CaseDataEntity> caseDataOpt = caseDataRepository.findCaseDataByReference(caseReference);
        if (caseDataOpt.isPresent()) {
            CaseDataEntity caseData = caseDataOpt.get();
            verifyJurisdiction(caseToUpdate.getJurisdiction(), caseData.getJurisdiction(), errorInput);
            verifyCaseType(caseToUpdate.getCaseType(), caseData.getCaseTypeId(), errorInput);
            verifyStateForAction(caseToUpdate, caseData.getState(), errorInput, parkingResult);
        } else {
            errorInput.addError(String.format("Case reference '%s' cannot be found", caseToUpdate.getCaseReference()));
        }
    }

    private void processResult(ParkingResult parkingResult) {
        if (applicationParams.isDryRun()) {
            log.info("DRY RUN COMPLETE:: No changes have been made");
            if (!parkingResult.getErrors().isEmpty()) {
                parkingResult.logErrors();
                log.info("DRY RUN COMPLETE:: Issues with dataset were found");
            } else {
                log.info("DRY RUN COMPLETE:: No issues with dataset were found");
            }
        } else if (!parkingResult.getErrors().isEmpty()) {
            parkingResult.logErrors();
            log.info("ABORTED:: Issues with dataset - no changes have been made");
        } else {
            int unparkResult = caseDataRepository.unparkCasesByReference(parkingResult.getCaseReferencesToUnpark(),
                parkingResult.getStatePrefix());
            int parkResult = caseDataRepository.parkCasesByReference(parkingResult.getCaseReferencesToPark(),
                parkingResult.getStatePrefix());
            log.info("COMPLETE:: Parked {} case(s) -- Unparked {} case(s) -- Total updated: {}",
                parkResult, unparkResult, parkResult + unparkResult);
        }
    }

    private void verifyStateForAction(Case caseToUpdate,
                                      String currentState,
                                      ErrorInput errorInput,
                                      ParkingResult parkingResult) {
        Action action = caseToUpdate.getActionEnum().orElseThrow();
        if (action == UNPARK) {
            if (!currentState.startsWith(parkingResult.getStatePrefix())) {
                errorInput.addError(String.format("Case is already unparked, state is '%s'", currentState));
            } else {
                parkingResult.getCaseReferencesToUnpark().add(caseToUpdate.getCaseReferenceAsLong());
            }
        } else if (action == PARK) {
            if (currentState.startsWith(parkingResult.getStatePrefix())) {
                errorInput.addError(String.format("Case is already parked, state is '%s'", currentState));
            } else {
                parkingResult.getCaseReferencesToPark().add(caseToUpdate.getCaseReferenceAsLong());
            }
        } else {
            errorInput.addError(String.format("Action '%s' is not yet implemented", action.toString()));
        }
    }

    private void verifyCaseType(String expectedCaseType,
                                String actualCaseType,
                                ErrorInput errorInput) {
        if (!actualCaseType.equals(expectedCaseType)) {
            errorInput.addError(String.format("Expected case type '%s', found '%s'",
                expectedCaseType, actualCaseType));
        }
    }

    private void verifyJurisdiction(String expectedJurisdiction,
                                    String actualJurisdiction,
                                    ErrorInput errorInput) {
        if (!actualJurisdiction.equals(expectedJurisdiction)) {
            errorInput.addError(String.format("Expected jurisdiction '%s', found '%s'",
                expectedJurisdiction, actualJurisdiction));
        }
    }

    private boolean isValidAction(Case caseToUpdate, ErrorInput errorInput) {
        if (caseToUpdate.getActionEnum().isEmpty()) {
            errorInput.addError(String.format("Action '%s' is not valid", caseToUpdate.getAction()));
            return false;
        }
        return true;
    }

    private boolean isValidCaseReference(Long caseReference, ErrorInput errorInput) {
        if (caseReference == null || caseReference == 0L) {
            errorInput.addError("Case reference is mandatory");
            return false;
        }
        return true;
    }

    private void verifyNumberOfCases(List<Case> casesToUpdate, ErrorInput errorInput) {
        Set<Long> caseReferencesToUpdate = casesToUpdate.stream()
            .map(Case::getCaseReferenceAsLong)
            .collect(Collectors.toSet());

        if (caseReferencesToUpdate.size() != casesToUpdate.size()) {
            errorInput.addError(String.format("Duplicate case references found - expected %s unique references, found %s",
                    casesToUpdate.size(), caseReferencesToUpdate.size()));
        } else {
            int dbCaseCount = caseDataRepository.getCaseCount(caseReferencesToUpdate);
            if (dbCaseCount != casesToUpdate.size()) {
                errorInput.addError(String.format("Difference in number of cases found in DB - expected %s, found %s in DB",
                        dbCaseCount, casesToUpdate.size()));
            }
        }
    }

}
