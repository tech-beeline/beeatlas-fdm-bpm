/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.domain;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@EqualsAndHashCode
@Table(name = "status_process", schema = "processes")
public class StatusProcess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "type_process_id")
    private Integer typeProcessId;

    @Column(name = "name")
    private String name;

    @Column(name = "alias")
    private String alias;

    @Column(name = "is_done")
    private Boolean isDone;

    @Column(name = "is_error")
    private Boolean isError;

    @Column(name = "sequence")
    private Integer sequence;
}
