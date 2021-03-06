package uk.gov.hmcts.reform.ccd.parking.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class ApplicationParams {

    @Value("${case-list-file}")
    private String caseListFile;

    @Value("${dry-run}")
    private boolean dryRun;

    @Value("${parking-state-prefix}")
    private String parkingStatePrefix;
}
