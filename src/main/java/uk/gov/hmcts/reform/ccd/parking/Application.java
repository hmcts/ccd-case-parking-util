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

import java.io.FileReader;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.ccd.parking.model.Action.PARK;
import static uk.gov.hmcts.reform.ccd.parking.model.Action.UNPARK;

@SpringBootApplication
@Slf4j
public class Application implements CommandLineRunner {

    private static final String PARKING_PREFIX = "PARKED_AT__";

    @Autowired
    private ApplicationParams applicationParams;

    @Autowired
    private CaseDataRepository caseDataRepository;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) {
        String caseListFile = applicationParams.getCaseListFile();
        log.info("START:: Processing file {}", caseListFile);

        try {
            boolean errorFound = false;

            List<Case> casesToUpdate = new CsvToBeanBuilder(new FileReader(caseListFile))
                .withType(Case.class)
                .build()
                .parse();

            Set<Long> caseReferencesToUpdate = casesToUpdate.stream()
                .map(Case::getCaseReferenceAsLong)
                .collect(Collectors.toSet());

            if (caseReferencesToUpdate.size() != casesToUpdate.size()) {
                log.error("Duplicate case references found - expected {} unique references, found {}",
                    casesToUpdate.size(), caseReferencesToUpdate.size());
                errorFound = true;
            } else {
                int dbCaseCount = caseDataRepository.getCaseCount(caseReferencesToUpdate);
                if (dbCaseCount != casesToUpdate.size()) {
                    log.error("Difference in number of cases found in DB - expected {}, found {} in DB",
                        dbCaseCount, casesToUpdate.size());
                    errorFound = true;
                }
            }

            Set<Long> caseReferencesToPark = new HashSet<>();
            Set<Long> caseReferencesToUnpark = new HashSet<>();

            for (int i = 0; i < casesToUpdate.size(); i++) {
                Case caseToUpdate = casesToUpdate.get(i);

                Long caseReference = caseToUpdate.getCaseReferenceAsLong();
                String jurisdiction = caseToUpdate.getJurisdiction();
                String caseType = caseToUpdate.getCaseType();

                if (caseReference == null || caseReference == 0L) {
                    log.error("[Record #{}] Case reference '{}' - case reference is mandatory", i, caseReference);
                    errorFound = true;
                    continue;
                }

                Optional<Action> actionOpt = caseToUpdate.getActionEnum();
                if (actionOpt.isEmpty()) {
                    log.error("[Record #{}] Case reference '{}' - action '{}' is not valid", i, caseReference,
                        caseToUpdate.getAction());
                    errorFound = true;
                    continue;
                }

                Optional<CaseDataEntity> caseDataOpt = caseDataRepository.findCaseDataByReference(caseReference);
                if (caseDataOpt.isPresent()) {
                    CaseDataEntity caseData = caseDataOpt.get();
                    String actualCaseType = caseData.getCaseTypeId();
                    String actualJurisdiction = caseData.getJurisdiction();
                    String state = caseData.getState();
                    Action action = actionOpt.get();

                    // Check jurisdiction matches
                    if (!actualJurisdiction.equals(jurisdiction)) {
                        log.error("[Record #{}] Case reference '{}' - expected jurisdiction '{}', found '{}'", i,
                            caseReference, jurisdiction, actualJurisdiction);
                        errorFound = true;
                    }

                    // Check case type matches
                    if (!actualCaseType.equals(caseType)) {
                        log.error("[Record #{}] Case reference '{}' - expected case type '{}', found '{}'", i,
                            caseReference, caseType, actualCaseType);
                        errorFound = true;
                    }

                    // Check state is as expected
                    if (action == UNPARK) {
                        if (!state.startsWith(PARKING_PREFIX)) {
                            log.error("[Record #{}] Case reference '{}' - case is already unparked, state is '{}'", i,
                                caseReference, state);
                            errorFound = true;
                        } else {
                            caseReferencesToUnpark.add(caseReference);
                        }
                    } else if (action == PARK) {
                        if (state.startsWith(PARKING_PREFIX)) {
                            log.error("[Record #{}] Case reference '{}' - case is already parked, state is '{}'", i,
                                caseReference, state);
                            errorFound = true;
                        } else {
                            caseReferencesToPark.add(caseReference);
                        }
                    } else {
                        log.error("[Record #{}] Case reference '{}' - action '{}' is not yet implemented", i,
                            caseReference, action.toString());
                        errorFound = true;
                    }
                } else {
                    log.error("[Record #{}] Case reference '{}' cannot be found", i, caseToUpdate.getCaseReference());
                }
            }

            if (applicationParams.isDryRun()) {
                log.info("DRY RUN COMPLETE:: No changes have been made");
                if (errorFound) {
                    log.info("DRY RUN COMPLETE:: Issues with dataset were found");
                } else {
                    log.info("DRY RUN COMPLETE:: No issues with dataset were found");
                }
            } else if (errorFound) {
                log.info("ABORTED:: Issues with dataset - no changes have been made");
            } else {
                int unparkResult = caseDataRepository.unparkCasesByReference(caseReferencesToUnpark, PARKING_PREFIX);
                int parkResult = caseDataRepository.parkCasesByReference(caseReferencesToPark, PARKING_PREFIX);
                log.info("COMPLETE:: Parked {} case(s) -- Unparked {} case(s) -- Total updated: {}",
                    parkResult, unparkResult, casesToUpdate.size());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
