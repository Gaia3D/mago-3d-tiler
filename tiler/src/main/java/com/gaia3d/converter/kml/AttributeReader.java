package com.gaia3d.converter.kml;

import java.io.File;
import java.util.List;

public interface AttributeReader {
    KmlInfo read(File file);
    List<KmlInfo> readAll(File file);
}
