/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.domain;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@EqualsAndHashCode
@Table(name = "camunda_process_status", schema = "processes")
public class CamundaProcessStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "status_process_id")
    private Integer statusProcessId;

    @Column(name = "camunda_process_id")
    private Integer camundaProcessId;

    @Column(name = "created_date")
    private LocalDateTime createdDate;
}
