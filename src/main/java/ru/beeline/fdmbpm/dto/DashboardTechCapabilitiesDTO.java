package ru.beeline.fdmbpm.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardTechCapabilitiesDTO {

    List<DashboardCapabilityDTO> list;
}
