package ru.beeline.fdmbpm.gitrepository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmbpm.gitdomain.FdmGitlabLanguages;

import java.util.List;


@Repository
public interface RingRepository extends JpaRepository<FdmGitlabLanguages, Integer> {

    @Query(value = "SELECT DISTINCT ON (cmdb_code, proj_lang) " +
            "       cmdb_code, " +
            "       proj_namespace, " +
            "       proj_name, " +
            "       proj_lang, " +
            "       lang_share, " +
            "       extraction_date " +
            "FROM v_fdm_gitlab_languages " +
            "ORDER BY cmdb_code, proj_lang, extraction_date DESC",
            nativeQuery = true)
    List<FdmGitlabLanguages> findUniqueCmdbCodeAndProjLang();
}
