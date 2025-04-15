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

    @Column(name = "role")
    private String role;
}
