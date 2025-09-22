package ru.beeline.fdmbpm.dto.wsdlSoap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Data;
import java.util.List;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceDTO {

    @XmlAttribute
    private String name;

    @XmlElement(name = "port", namespace = "http://schemas.xmlsoap.org/wsdl/")
    private List<PortDTO> ports;
}