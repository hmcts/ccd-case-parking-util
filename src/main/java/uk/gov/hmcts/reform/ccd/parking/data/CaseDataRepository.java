package uk.gov.hmcts.reform.ccd.parking.data;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional
public interface CaseDataRepository extends CrudRepository<CaseDataEntity, String> {

    @Query("SELECT count(cd) FROM CaseDataEntity cd WHERE cd.reference in :caseReferences")
    int getCaseCount(List<Long> caseReferences);

    @Query("SELECT cd FROM CaseDataEntity cd WHERE cd.reference = :caseReference")
    Optional<CaseDataEntity> findCaseDataByReference(Long caseReference);

    @Modifying
    @Query("UPDATE CaseDataEntity cd SET cd.state = replace(cd.state, 'PARKED_AT__', '') WHERE cd.reference in :caseReferences")
    int unparkCasesByReference(List<Long> caseReferences);
}
