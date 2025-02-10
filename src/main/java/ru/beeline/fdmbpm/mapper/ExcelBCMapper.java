package ru.beeline.fdmbpm.mapper;

import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.dto.importExcel.ExcelBcDTO;
import ru.beeline.fdmbpm.dto.importExcel.ExcelDTO;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ExcelBCMapper {

    public List<ExcelBcDTO> convert(List<ExcelDTO> excelDTOS) {
        List<ExcelBcDTO> result = excelDTOS.stream().map(this::convert).collect(Collectors.toList());
        return result;

    }

    public ExcelBcDTO convert(ExcelDTO excelDTOS) {
        return ExcelBcDTO.builder()
                .code(excelDTOS.getCode())
                .name(excelDTOS.getName())
                .description(excelDTOS.getDescription())
                .status(excelDTOS.getStatus())
                .author(excelDTOS.getAuthor())
                .link(excelDTOS.getLink())
                .owner(excelDTOS.getOwner())
                .parent(!excelDTOS.getParents().isEmpty() ? excelDTOS.getParents().get(0) : null)
                .isDomain(excelDTOS.getIsDomain())
                .build();
    }
}
