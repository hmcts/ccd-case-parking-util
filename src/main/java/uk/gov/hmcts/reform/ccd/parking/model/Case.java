package uk.gov.hmcts.reform.ccd.parking.model;

import com.google.common.base.Enums;
import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;

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

    @CsvBindByName(column = "Action")
    private String action;

    public Long getCaseReferenceAsLong() {
        return isNullOrEmpty(caseReference) ? null :
            Long.parseLong(getCaseReference().replaceAll("['\"]", ""));
    }

    public Optional<Action> getActionEnum() {
        return Enums.getIfPresent(Action.class, nullToEmpty(getAction())).toJavaUtil();
    }
}
