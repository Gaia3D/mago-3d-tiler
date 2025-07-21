package com.gaia3d.converter.kml.kml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JacksonXmlRootElement(localName = "Document")
public class Scale {
    @JacksonXmlProperty(localName = "x")
    private double x;
    @JacksonXmlProperty(localName = "y")
    private double y;
    @JacksonXmlProperty(localName = "z")
    private double z;
}
