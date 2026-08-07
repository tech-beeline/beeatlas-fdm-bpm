/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@EqualsAndHashCode
@Table(name = "application", schema = "processes")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "type_id")
    private Integer typeId;

    @Column(name = "status_id")
    private Integer statusId;

    @Column(name = "author_id")
    private Integer authorId;

    @Column(name = "process_id")
    private String processId;

    @Column(name = "business_key")
    private String businessKey;

    @Column(name = "executor_id")
    private Integer executorId;

    private String name;

    @Column(name = "responsible_id")
    private Integer responsibleId;

    @Column(name = "create_date")
    private LocalDateTime createDate;

    @Column(name = "update_date")
    private LocalDateTime updateDate;

    @Column(name = "entity_id")
    private Integer entityId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "type_id", referencedColumnName = "id", insertable = false, updatable = false)
    private ApplicationTypeEnum applicationType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "status_id", referencedColumnName = "id", insertable = false, updatable = false)
    private ApplicationTypeStatus status;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Comment> comments;
}
