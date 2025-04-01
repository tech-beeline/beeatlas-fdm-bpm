package ru.beeline.fdmbpm.mapper;

import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.domain.CamundaProcess;
import ru.beeline.fdmbpm.domain.CamundaProcessStatus;
import ru.beeline.fdmbpm.domain.Context;
import ru.beeline.fdmbpm.domain.StatusProcess;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.dto.camundaProcess.GetContextDTO;
import ru.beeline.fdmbpm.dto.camundaProcess.ShortContextDTO;
import ru.beeline.fdmbpm.dto.camundaProcess.StatusDTO;
import ru.beeline.fdmbpm.dto.camundaProcess.TypeDTO;

@Component
public class ContextDtoMapper {

    public GetContextDTO convert(CamundaProcess camundaProcess, TypeProcess typeProcess, StatusProcess statusProcess,
                                 CamundaProcessStatus camundaProcessStatus) {
        return GetContextDTO.builder()
                .id(camundaProcess.getId())
                .procId(camundaProcess.getProcId())
                .businessKey(camundaProcess.getBusinessKey())
                .type(TypeDTO.builder()
                        .id(typeProcess.getId())
                        .name(typeProcess.getName())
                        .description(typeProcess.getDescription())
                        .build())
                .status(StatusDTO.builder()
                        .id(statusProcess.getId())
                        .alias(statusProcess.getAlias())
                        .name(statusProcess.getName())
                        .createdDate(camundaProcessStatus.getCreatedDate())
                        .build())
                .build();
    }

    public ShortContextDTO shortContextConvert(Context context) {
        return ShortContextDTO.builder().
                name(context.getName())
                .value(context.getValue())
                .build();
    }
}
