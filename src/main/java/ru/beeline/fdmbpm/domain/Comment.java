/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.domain;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@EqualsAndHashCode
@Table(name = "comments", schema = "processes")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String comment;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "application_id")
    private Integer applicationId;

    @ManyToOne
    @JsonIgnore
    @ToString.Exclude
    @JoinColumn(name = "application_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Application application;
}
