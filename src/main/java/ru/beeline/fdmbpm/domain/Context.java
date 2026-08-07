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
@Table(name = "context", schema = "processes")
public class Context {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "camunda_process_id")
    private Integer camundaProcessId;

    @Column(name = "name")
    private String name;

    @Column(name = "value")
    private String value;
}
