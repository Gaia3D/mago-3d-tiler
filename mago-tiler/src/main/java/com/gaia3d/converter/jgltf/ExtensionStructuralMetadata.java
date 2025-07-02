package com.gaia3d.converter.jgltf;

import com.gaia3d.process.postprocess.DataType;
import com.gaia3d.process.postprocess.batch.GaiaBatchTableMap;
import com.gaia3d.process.tileprocess.tile.tileset.schema.ClassProperty;
import com.gaia3d.process.tileprocess.tile.tileset.schema.Schema;
import com.gaia3d.process.tileprocess.tile.tileset.schema.SchemaClass;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EXT_structural_metadata
 */
@Slf4j
@Getter
@Setter
public class ExtensionStructuralMetadata {
    private Schema schema;
    private List<PropertyTable> propertyTables;

    public static ExtensionStructuralMetadata fromBatchTable(GaiaBatchTableMap<String, List<String>> batchTableMap) {
        ExtensionStructuralMetadata metadata = new ExtensionStructuralMetadata();

        List<PropertyTable> propertyTables = new ArrayList<>();
        Schema schema = new Schema();

        Map<String, SchemaClass> schemaClasses = new HashMap<>();
        schema.setClasses(schemaClasses);

        String schemaName = "TEST_SCHEMA"; // This should be dynamically determined based on the batch table

        SchemaClass schemaClass = new SchemaClass();
        schemaClasses.put(schemaName, schemaClass);

        PropertyTable propertyTable = new PropertyTable();
        propertyTable.setClassName(schemaName);

        Map<String, Property> valueProperties = new HashMap<>();
        propertyTable.setProperties(valueProperties);

        int count = batchTableMap.values().stream()
                .mapToInt(List::size)
                .min().orElse(0); // Total count of all properties in the batch table
        propertyTable.setCount(count);
        propertyTables.add(propertyTable);

        Map<String, ClassProperty> metaProperties = new HashMap<>();
        schemaClass.setProperties(metaProperties);
        batchTableMap.forEach((name, list) -> {
            ClassProperty classProperty = new ClassProperty();
            classProperty.setName(name);
            classProperty.setDescription("Description for " + name); // Placeholder description
            classProperty.setRequired(true); // Assuming all properties are required for simplicity
            classProperty.setType(DataType.STRING); // Assuming all properties are of type STRING for simplicity
            metaProperties.put(name, classProperty);

            Property property = new Property();
            property.setName(name);
            property.setPrimaryValues(list);
            // Assuming values and stringOffsets are set to 0 for simplicity
            valueProperties.put(name, property);
        });

        metadata.setSchema(schema);
        metadata.setPropertyTables(propertyTables);

        return metadata;
    }
}
