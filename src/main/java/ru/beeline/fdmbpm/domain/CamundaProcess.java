package ru.beeline.fdmbpm.domain;

import jakarta.persistence.*;
import lombok.*;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@EqualsAndHashCode
@Table(name = "camunda_process", schema = "processes")
public class CamundaProcess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "type_process_id")
    private Integer typeProcessId;

    @Column(name = "proc_id")
    private String procId;

    @Column(name = "is_async", nullable = false)
    private Boolean isAsync;

    @Column(name = "business_key")
    private String businessKey;
}
