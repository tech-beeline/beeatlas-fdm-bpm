package ru.beeline.fdmbpm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@EqualsAndHashCode
@Table(name = "application_type_enum", schema = "processes")
public class ApplicationTypeEnum {

    @Id
    private Integer id;

    private String alias;

    private String name;

    private String description;

    @Column(name = "target_call")
    private String targetCall;

    @Column(name = "entity_type")
    private String entityType;
}
