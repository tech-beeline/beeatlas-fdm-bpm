/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.dto.wsdlSoap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.xml.bind.annotation.*;
import lombok.Data;

import java.util.List;

@Data
@XmlRootElement(name = "definitions", namespace = "http://schemas.xmlsoap.org/wsdl/")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DefinitionsDTO {

    @XmlAttribute
    private String name;

    @XmlElement(name = "portType", namespace = "http://schemas.xmlsoap.org/wsdl/")
    private List<PortTypeDTO> portTypes;

    @XmlElement(name = "service", namespace = "http://schemas.xmlsoap.org/wsdl/")
    private ServiceDTO service;
}
