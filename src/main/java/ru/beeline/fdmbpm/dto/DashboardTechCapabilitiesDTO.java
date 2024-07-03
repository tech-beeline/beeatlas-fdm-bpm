package ru.beeline.fdmbpm.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DashboardTechCapabilitiesDTO {

    List<DashboardTechCapabilityDTO> list;
}
