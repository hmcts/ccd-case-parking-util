package uk.gov.hmcts.reform.ccd.parking;

import com.opencsv.bean.CsvToBeanBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uk.gov.hmcts.reform.ccd.parking.config.ApplicationParams;
import uk.gov.hmcts.reform.ccd.parking.data.CaseDataEntity;
import uk.gov.hmcts.reform.ccd.parking.model.Case;
import uk.gov.hmcts.reform.ccd.parking.data.CaseDataRepository;

import java.io.FileReader;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    public void run(String... args) {
        String caseListFile = applicationParams.getCaseListFile();
        log.info("START:: Starting unparking process for file {}", caseListFile);

        try {
            List<Case> casesToUpdate = new CsvToBeanBuilder(new FileReader(caseListFile))
                .withType(Case.class)
                .build()
                .parse();

            List<Long> caseReferencesToUpdate = casesToUpdate.stream()
                .map(Case::getCaseReference)
                .collect(Collectors.toList());

            boolean logOnly = false;

            // Check database figures match provided list
            int dbCaseCount = caseDataRepository.getCaseCount(caseReferencesToUpdate);
            if (dbCaseCount != casesToUpdate.size()) {
                log.error("Difference in number of cases found in DB - expected {}, found {} in DB",
                    dbCaseCount, casesToUpdate.size());
                logOnly = true;
            }

            for (int i = 0; i < casesToUpdate.size(); i++) {
                Case caseToUpdate = casesToUpdate.get(i);

                Long caseReference = caseToUpdate.getCaseReference();
                String jurisdiction = caseToUpdate.getJurisdiction();
                String caseType = caseToUpdate.getCaseType();

                if (caseReference == null || caseReference == 0L) {
                    log.error("[Record #{}] Case reference {} - case reference is mandatory", i, caseReference);
                    logOnly = true;
                }

                Optional<CaseDataEntity> caseDataOpt = caseDataRepository.findCaseDataByReference(caseReference);
                if (caseDataOpt.isPresent()) {
                    CaseDataEntity caseData = caseDataOpt.get();
                    String actualCaseType = caseData.getCaseTypeId();
                    String actualJurisdiction = caseData.getJurisdiction();
                    String state = caseData.getState();

                    // Check jurisdiction matches
                    if (!actualJurisdiction.equals(jurisdiction)) {
                        log.error("[Record #{}] Case reference {} - expected jurisdiction {}, found {}", i,
                            caseReference, jurisdiction, actualJurisdiction);
                        logOnly = true;
                    }

                    // Check case type matches
                    if (!actualCaseType.equals(caseType)) {
                        log.error("[Record #{}] Case reference {} - expected case type {}, found {}", i,
                            caseReference, caseType, actualCaseType);
                        logOnly = true;
                    }

                    // Check state is as expected
                    if (!state.startsWith("PARKED_AT__")) { // TODO: Make prefix configurable (and use in update queries)
                        log.error("[Record #{}] Case reference {} - state is expected to be parked, found {}", i,
                            caseReference, state);
                        logOnly = true;
                    }
                } else {
                    log.error("[Record #{}] Case reference {} cannot be found", i, caseToUpdate.getCaseReference());
                }
            }

            // All sanity checks passed - unpark all
            // TODO: Support parking as well as unparking
            if (!logOnly) {
                int result = caseDataRepository.unparkCasesByReference(caseReferencesToUpdate);
                log.info("COMPLETE:: Unparked {} case(s)", result);
            } else {
                log.info("ABORTED:: Issues with dataset - no changes have been made");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
