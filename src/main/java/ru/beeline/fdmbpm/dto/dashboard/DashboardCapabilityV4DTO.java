package ru.beeline.fdmbpm.dto.dashboard;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardCapabilityV4DTO {

    private String code;
    private Boolean isDomain;
    private String name;
    private String description;
    private String author;
    private String status;
    private DashboardCapabilityV4ParentDTO parent;
    private String owner;
}
