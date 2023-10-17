package com.gaia3d.command;

import lombok.extern.slf4j.Slf4j;
import org.geotools.metadata.iso.citation.Citations;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.factory.PropertyAuthorityFactory;
import org.geotools.referencing.factory.ReferencingFactoryContainer;
import org.geotools.util.factory.Hints;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.IOException;
import java.net.URL;

@Slf4j
public class GeotoolsConfigurator {
    public void setEpsg() throws IOException {
        URL epsg = Thread.currentThread().getContextClassLoader().getResource("epsg.properties");
        if (epsg != null) {
            Hints hints = new Hints(Hints.CRS_AUTHORITY_FACTORY, PropertyAuthorityFactory.class);
            ReferencingFactoryContainer referencingFactoryContainer = ReferencingFactoryContainer.instance(hints);
            PropertyAuthorityFactory factory = new PropertyAuthorityFactory(referencingFactoryContainer, Citations.fromName("EPSG"), epsg);
            ReferencingFactoryFinder.addAuthorityFactory(factory);
            ReferencingFactoryFinder.scanForPlugins();
        }
    }
}
