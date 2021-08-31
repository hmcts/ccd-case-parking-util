package uk.gov.hmcts.reform.ccd.parking.data;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Table(name = "case_data")
@Entity
@Data
public class CaseDataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "jurisdiction")
    private String jurisdiction;

    @Column(name = "case_type_id")
    private String caseTypeId;

    @Column(name = "state")
    private String state;

    @Column(name = "reference")
    private Long reference;
}
