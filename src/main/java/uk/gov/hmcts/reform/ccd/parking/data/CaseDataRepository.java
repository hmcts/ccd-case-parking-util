package uk.gov.hmcts.reform.ccd.parking.data;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Transactional
public interface CaseDataRepository extends CrudRepository<CaseDataEntity, String> {

    @Query("SELECT count(cd) FROM CaseDataEntity cd WHERE cd.reference in :caseReferences")
    int getCaseCount(Set<Long> caseReferences);

    @Query("SELECT cd FROM CaseDataEntity cd WHERE cd.reference = :caseReference")
    Optional<CaseDataEntity> findCaseDataByReference(Long caseReference);

    @Modifying
    @Query("UPDATE CaseDataEntity cd SET cd.state = replace(cd.state, :parkingPrefix, '') WHERE cd.reference in :caseReferences")
    int unparkCasesByReference(Set<Long> caseReferences, String parkingPrefix);

    @Modifying
    @Query("UPDATE CaseDataEntity cd SET cd.state = :parkingPrefix || cd.state WHERE cd.reference in :caseReferences")
    int parkCasesByReference(Set<Long> caseReferences, String parkingPrefix);
}
