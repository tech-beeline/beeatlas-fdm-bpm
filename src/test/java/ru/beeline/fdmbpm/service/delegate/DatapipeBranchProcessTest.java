package ru.beeline.fdmbpm.service.delegate;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class DatapipeBranchProcessTest {

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "Process_datapipeline_branch:3:8f1c-11, Process_datapipeline_branch",
            "Process_08wprkp:12:aa01, Process_08wprkp",
            "Process_datapipeline_branch, Process_datapipeline_branch",
            "null, null"
    })
    void extractsDefinitionKey(String processDefinitionId, String key) {
        assertThat(DatapipeBranchProcess.processDefinitionKey(processDefinitionId)).isEqualTo(key);
    }
}
