package uk.gov.hmcts.reform.ccd.parking.model;

import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Case {

    @CsvBindByName(column = "Jurisdiction")
    private String jurisdiction;

    @CsvBindByName(column = "CaseType")
    private String caseType;

    @CsvBindByName(column = "Reference")
    private String caseReference;

    public Long getCaseReferenceAsLong() {
        return Long.parseLong(getCaseReference().replaceAll("['\"]", ""));
    }
}
