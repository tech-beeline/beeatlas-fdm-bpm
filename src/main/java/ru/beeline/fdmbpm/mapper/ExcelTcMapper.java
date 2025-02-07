package ru.beeline.fdmbpm.mapper;

import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.dto.importExcel.ExcelDTO;
import ru.beeline.fdmbpm.dto.importExcel.ExcelTcDTO;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ExcelTcMapper {
    public List<ExcelTcDTO> convert(List<ExcelDTO> excelDTOS) {
        List<ExcelTcDTO> result = excelDTOS.stream().map(this::convert).collect(Collectors.toList());
        return result;

    }

    public ExcelTcDTO convert(ExcelDTO excelDTOS) {
        return ExcelTcDTO.builder()
                .code(excelDTOS.getCode())
                .name(excelDTOS.getName())
                .description(excelDTOS.getDescription())
                .status(excelDTOS.getStatus())
                .author(excelDTOS.getAuthor())
                .link(excelDTOS.getLink())
                .owner(excelDTOS.getOwner())
                .parents(excelDTOS.getParents())
                .build();
    }
}
