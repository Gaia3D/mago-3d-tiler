package com.gaia3d.converter.kml;

import java.io.File;
import java.util.List;

public interface AttributeReader {
    public KmlInfo read(File file);
    public List<KmlInfo> readAll(File file);
}
