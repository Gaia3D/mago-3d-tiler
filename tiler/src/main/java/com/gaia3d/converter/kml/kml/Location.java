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
public class Location {
    @JacksonXmlProperty(localName = "longitude")
    private double longitude;
    @JacksonXmlProperty(localName = "latitude")
    private double latitude;
    @JacksonXmlProperty(localName = "altitude")
    private double altitude;
}
