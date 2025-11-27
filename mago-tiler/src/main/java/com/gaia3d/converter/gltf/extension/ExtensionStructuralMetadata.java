package com.gaia3d.converter.gltf.extension;

import com.gaia3d.basic.types.AttributeType;
import com.gaia3d.process.postprocess.DataType;
import com.gaia3d.process.postprocess.batch.GaiaBatchTableMap;
import com.gaia3d.process.postprocess.pointcloud.PointCloudBuffer;
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
    private final static String EXTENSION_NAME = "mago_metadata_schema";
    private Schema schema;
    private List<PropertyTable> propertyTables;
    private List<PropertyAttribute> propertyAttributes;

    public static ExtensionStructuralMetadata fromBatchTableMap(GaiaBatchTableMap<String, List<String>> batchTableMap) {
        ExtensionStructuralMetadata metadata = new ExtensionStructuralMetadata();

        List<PropertyTable> propertyTables = new ArrayList<>();
        Schema schema = new Schema();
        schema.setId(EXTENSION_NAME); // This should be dynamically determined based on the batch table

        Map<String, SchemaClass> schemaClasses = new HashMap<>();
        schema.setClasses(schemaClasses);

        String schemaName = EXTENSION_NAME; // This should be dynamically determined based on the batch table

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

    public static ExtensionStructuralMetadata fromPointCloudBuffer(PointCloudBuffer pointCloudBuffer) {
        ExtensionStructuralMetadata metadata = new ExtensionStructuralMetadata();

        Schema schema = new Schema();
        metadata.setSchema(schema);

        Map<String, SchemaClass> schemaClasses = new HashMap<>();
        schema.setClasses(schemaClasses);

        String schemaName = "TEST_SCHEMA"; // This should be dynamically determined based on the batch table

        SchemaClass schemaClass = new SchemaClass();
        schemaClasses.put(schemaName, schemaClass);

        // propertyAttributes
        List<PropertyAttribute> propertyAttributes = new ArrayList<>();
        metadata.setPropertyAttributes(propertyAttributes);

        PropertyAttribute propertyAttribute = new PropertyAttribute();
        Map<String, Attribute> valueProperties = new HashMap<>();
        propertyAttribute.setClassName(schemaName);
        propertyAttribute.setProperties(valueProperties);
        metadata.getPropertyAttributes().add(propertyAttribute);

        Map<String, ClassProperty> metaProperties = new HashMap<>();
        schemaClass.setProperties(metaProperties);
        AttributeType intensityAttribute = AttributeType.INTENSITY;
        if (pointCloudBuffer.getIntensities() != null) {
            ClassProperty classProperty = new ClassProperty();
            classProperty.setName(intensityAttribute.getName());
            classProperty.setDescription("Intensity values for point cloud");
            classProperty.setType(DataType.SCALAR);
            classProperty.setComponentType("UINT16");
            classProperty.setRequired(true);
            metaProperties.put(intensityAttribute.getName(), classProperty);

            Attribute attribute = new Attribute();
            attribute.setAttribute(intensityAttribute.getAccessor());
            valueProperties.put(intensityAttribute.getName(), attribute);
        }

        AttributeType classificationAttribute = AttributeType.CLASSIFICATION;
        if (pointCloudBuffer.getClassifications() != null) {
            ClassProperty classProperty = new ClassProperty();
            classProperty.setName(classificationAttribute.getName());
            classProperty.setDescription("Classification values for point cloud");
            classProperty.setType(DataType.SCALAR);
            classProperty.setComponentType("UINT16");
            classProperty.setRequired(true);
            metaProperties.put(classificationAttribute.getName(), classProperty);

            Attribute attribute = new Attribute();
            attribute.setAttribute(classificationAttribute.getAccessor());
            valueProperties.put(classificationAttribute.getName(), attribute);
        }
        return metadata;
    }
}
