package ru.beeline.fdmbpm.dto.dashboard;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardCapabilityV4DTO {

    private Boolean isDomain;
    private String name;
    private String description;
    private String author;
    private String parent;
    private String owner;
    private String status;
    private String self;
}
