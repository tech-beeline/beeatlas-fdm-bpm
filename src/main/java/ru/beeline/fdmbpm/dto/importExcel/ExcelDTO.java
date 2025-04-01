package ru.beeline.fdmbpm.dto.importExcel;

import lombok.*;

import java.util.List;
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ExcelDTO {

    private String code;
    private String name;
    private String description;
    private String status;
    private String author;
    private String link;
    private String owner;
    private List<String> parents;
    private Boolean isDomain;
}
