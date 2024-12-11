package ru.beeline.fdmbpm.gitrepository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmbpm.gitdomain.FdmGitlabLanguages;

import java.sql.Date;
import java.util.List;
import java.util.stream.Collectors;


@Repository
public interface RingRepository extends JpaRepository<FdmGitlabLanguages, Integer> {

    @Query(value = "SELECT DISTINCT ON (cmdb_code, proj_lang) " +
            "       cmdb_code, " +
            "       proj_lang, " +
            "       extraction_date " +
            "FROM v_fdm_gitlab_languages " +
            "ORDER BY cmdb_code, proj_lang, extraction_date DESC",
            nativeQuery = true)
    List<Object[]> findUniqueCmdbCodeAndProjLang();

    default List<FdmGitlabLanguages> findUniqueCmdbCodeAndProjLangModify() {
        List<Object[]> results = findUniqueCmdbCodeAndProjLang();
        return results.stream()
                .map(row -> new FdmGitlabLanguages(row[0].toString(), row[1].toString(), Date.valueOf(row[2].toString())))
                .collect(Collectors.toList());
    }
}