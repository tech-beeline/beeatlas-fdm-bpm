/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.domain;


import jakarta.persistence.*;
import lombok.*;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@EqualsAndHashCode
@Table(name = "application_type_status", schema = "processes")
public class ApplicationTypeStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String alias;

    private String name;

    @Column(name = "type_id")
    private Integer typeId;

    @Column(name = "serial_number")
    private Integer serialNumber;

    private String message;

    @Column(name = "is_end_status")
    private Boolean isEndStatus;

    @Column(name = "is_author_responsible")
    private Boolean isAuthorResponsible;

    @ManyToOne
    @JoinColumn(name = "type_id", referencedColumnName = "id", insertable = false, updatable = false)
    private ApplicationTypeEnum applicationType;
}
