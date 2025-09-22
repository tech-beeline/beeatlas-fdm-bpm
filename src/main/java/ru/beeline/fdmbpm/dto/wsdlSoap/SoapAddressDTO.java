package ru.beeline.fdmbpm.dto.wsdlSoap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import lombok.Data;


@Data
@XmlAccessorType(XmlAccessType.FIELD)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SoapAddressDTO {

    @XmlAttribute(name = "location")
    private String location;
}