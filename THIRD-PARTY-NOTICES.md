# Third-Party Notices

mago-3d-tiler is licensed under the [Mozilla Public License 2.0](LICENSE).
Binary distributions of mago-3d-tiler (the executable JAR and the `gaia3d/mago-3d-tiler` Docker image)
include third-party components that are distributed under their own license terms, summarized below.

## Complete dependency list

The complete, per-dependency list of Java libraries, including every license text and NOTICE file
embedded in those libraries, is generated at build time and shipped inside the executable JAR:

- `META-INF/third-party/THIRD-PARTY-LICENSES.txt` : full text report
- `META-INF/third-party/index.html` : report grouped by license
- `META-INF/third-party/<artifact>.jar/` : LICENSE and NOTICE files embedded in each library
- `META-INF/third-party/lwjgl/` : license texts of the LWJGL native libraries
- `META-INF/mago-3d-tiler/` : the mago-3d-tiler license and this notice file

To regenerate the report from source:

```shell
./gradlew :mago-tiler:generateLicenseReport
# output: mago-tiler/build/reports/dependency-license/
```

## License summary

| License | Main components |
|---|---|
| Apache License 2.0 | Apache Log4j, Apache Commons, Jackson, Woodstox, Guava, citygml4j, Proj4J, Eclipse ImageN, EJML, ehcache, SQLite JDBC, Kotlin stdlib, KTX-Software, Draco |
| BSD 2/3-Clause | LWJGL, Assimp, jai-imageio-core, HSQLDB, ANTLR, Janino, Units of Measurement (indriya, si-units), stax2-api, PicoContainer, BigInt (Huldra) |
| MIT | JOML, JglTF, SLF4J, DuckDB JDBC, GeographicLib-Java, Checker Framework, stb, libffi, liburing |
| zlib/libpng | GLFW |
| GNU LGPL 2.1 | GeoTools, ImageIO-Ext, laszip4j, jgridshift |
| Eclipse Public License 1.0 / 2.0 | Eclipse EMF, Eclipse XSD, JTS Topology Suite (EPL 2.0 / EDL 1.0), Jakarta Annotations |
| CDDL 1.1 (elected from CDDL / GPL-2.0 with Classpath Exception) | JAXB API/Runtime, javax.activation, istack-commons, stax-ex |
| Eclipse Distribution License 1.0 | relaxng-datatype, XSOM |
| JDOM License (Apache-style) | JDOM 2 |
| Go License (BSD-3-Clause) | RE2/J |
| EPSG Dataset Terms of Use | EPSG Geodetic Parameter Dataset (bundled in gt-epsg-hsql, proj4j-epsg) |

Where a component is offered under a choice of licenses (for example CDDL or GPL-2.0 with Classpath Exception),
mago-3d-tiler distributes it under the non-GPL option.

## Native libraries (LWJGL)

The LWJGL artifacts contain native binaries that do not ship license files in their Maven artifacts.
Their license texts are included in [`licenses/lwjgl/`](licenses/lwjgl) and in the executable JAR
under `META-INF/third-party/lwjgl/`.

| Component | License | License file |
|---|---|---|
| LWJGL 3 — Copyright (c) 2012-present Lightweight Java Game Library | BSD 3-Clause | `LICENSE.md` |
| Open Asset Import Library (assimp) — Copyright (c) 2006-2016, assimp team | BSD 3-Clause | `assimp_license.txt` |
| Draco (bundled with the assimp natives) — Copyright Google LLC | Apache License 2.0 | `draco_license.txt` |
| GLFW — Copyright (c) 2002-2006 Marcus Geelnard, (c) 2006-2019 Camilla Löwy | zlib/libpng | `glfw_license.txt` |
| KTX-Software — Copyright (c) The Khronos Group Inc. | Apache License 2.0 | `ktx_license.txt` |
| OpenGL registry — Copyright (c) The Khronos Group Inc. | MIT-style | `khronos_license.txt` |
| stb — Sean Barrett | MIT or Public Domain | `stb_license.txt` |
| libffi — Copyright (c) Anthony Green, Red Hat, Inc. and others | MIT | `libffi_license.txt` |
| liburing — Copyright 2020 Jens Axboe | MIT | `liburing_license.txt` |

## Bundled data

- **EPSG Geodetic Parameter Dataset** — Copyright © International Association of Oil & Gas Producers (IOGP).
  Used under the EPSG Dataset Terms of Use (<https://epsg.org/terms-of-use.html>).
- **Geoid models** (`mago-tiler/src/main/resources/geoid/`) — derived from the Earth Gravitational Models
  EGM84, EGM96 and EGM2008 published by the U.S. National Geospatial-Intelligence Agency (NGA).

## Source code availability

The source code of the components licensed under the LGPL, EPL and CDDL is available from their project
sites and as `-sources.jar` artifacts in Maven Central (<https://repo1.maven.org/maven2/>) and the OSGeo
repository (<https://repo.osgeo.org/repository/release/>), under the same coordinates and versions listed
in `META-INF/third-party/THIRD-PARTY-LICENSES.txt`.
For questions regarding third-party components, contact Gaia3d, Inc. at <sales@gaia3d.com>.
