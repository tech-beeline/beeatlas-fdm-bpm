package ru.beeline.fdmbpm.dto.importExcel;

import lombok.*;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ExcelBcDTO {

    private String code;
    private String name;
    private String description;
    private String status;
    private String author;
    private String link;
    private String owner;
    private String parents;
    private Boolean isDomain;

}
