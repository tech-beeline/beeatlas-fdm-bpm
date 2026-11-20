package ru.beeline.fdmbpm.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmbpm.dto.ffmanager.PostFfManagerDTO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FfManagerClientPostFfManagerTest {

    @Test
    @DisplayName("run-all без branch — query без branch, ff-manager уйдёт в main")
    void postWithoutBranchOmitsQueryParam() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(Object.class)))
                .thenReturn(ResponseEntity.ok().build());
        FfManagerClient client = new FfManagerClient("http://ff-manager", restTemplate);

        client.postFfManager(PostFfManagerDTO.builder().app("fdmshowcaseapp").build(), 14508, 703, null);

        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).exchange(url.capture(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Object.class));
        assertThat(url.getValue())
                .isEqualTo("http://ff-manager/api/v1/run-all?docId=14508&source_type=pipeline&source_id=703");
        assertThat(url.getValue()).doesNotContain("branch=");
    }

    @Test
    @DisplayName("run-all с branch=develop — branch уходит в query")
    void postWithBranchAddsQueryParam() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(Object.class)))
                .thenReturn(ResponseEntity.ok().build());
        FfManagerClient client = new FfManagerClient("http://ff-manager", restTemplate);

        client.postFfManager(PostFfManagerDTO.builder().app("fdmshowcaseapp").build(), 14508, 703, "develop");

        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).exchange(url.capture(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Object.class));
        assertThat(url.getValue())
                .contains("docId=14508")
                .contains("source_type=pipeline")
                .contains("source_id=703")
                .contains("branch=develop");
    }
}
