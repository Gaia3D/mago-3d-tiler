package com.gaia3d.converter.kml.kml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JacksonXmlRootElement(localName = "Orientation")
public class Orientation {
    @JacksonXmlProperty(localName = "heading")
    private double heading;
    @JacksonXmlProperty(localName = "tilt")
    private double tilt;
    @JacksonXmlProperty(localName = "roll")
    private double roll;
}
