package uk.gov.hmcts.reform.ccd.parking.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class ApplicationParams {

    @Value("${case_list_file}")
    private String caseListFile;

    @Value("${dry_run}")
    private boolean dryRun;
}
