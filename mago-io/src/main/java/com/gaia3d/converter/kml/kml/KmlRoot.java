package com.gaia3d.converter.kml.kml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JacksonXmlRootElement(localName = "kml")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KmlRoot {

    @JacksonXmlProperty(isAttribute = true, localName = "xmlns")
    private String xmlns;

    @JsonProperty("Document")
    private Document document;

}
