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
@Table(name = "executor_roles", schema = "processes")
public class ExecutorRoles {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "type_id")
    private Integer typeId;

    private String role;

    @ManyToOne
    @JoinColumn(name = "type_id", referencedColumnName = "id", insertable = false, updatable = false)
    private ApplicationTypeEnum applicationType;
}
