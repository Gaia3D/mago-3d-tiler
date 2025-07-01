package com.gaia3d.converter.kml.kml;


import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JacksonXmlRootElement(localName = "Model")
public class Model {
    @JacksonXmlProperty(localName = "altitudeMode")
    private String altitudeMode;
    @JacksonXmlProperty(localName = "Location")
    private Location location;
    @JacksonXmlProperty(localName = "Orientation")
    private Orientation orientation;
    @JacksonXmlProperty(localName = "Scale")
    private Scale scale;
    @JacksonXmlProperty(localName = "Link")
    private Link link;
}
