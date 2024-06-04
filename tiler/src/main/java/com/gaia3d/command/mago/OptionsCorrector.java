package com.gaia3d.command.mago;

import com.gaia3d.basic.types.FormatType;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.util.Objects;

@Slf4j
@NoArgsConstructor
public class OptionsCorrector {

    public static boolean isExistInputPath(File path) throws IllegalArgumentException, IOException {
        if (!path.exists()) {
            throw new FileExistsException(String.format("%s path is not exist.", path));
        } else if (!path.canWrite()) {
            throw new IOException(String.format("%s path is not writable.", path));
        }
        return true;
    }

    public static boolean isExistOutput(File path) throws IOException {
        if (!path.exists()) {
            boolean isSuccess = path.mkdirs();
            if (!isSuccess) {
                throw new FileExistsException(String.format("%s output path is not exist.", path));
            } else {
                log.info("Created new output directory. {}", path);
            }
        } else if (!path.isDirectory()) {
            throw new NotDirectoryException(String.format("%s path is not directory.", path));
        } else if (!path.canWrite()) {
            throw new IOException(String.format("%s path is not writable.", path));
        }
        return true;
    }

    public static boolean isRecursive(File path) {
        if (path.isDirectory()) {
            File[] files = path.listFiles();
            assert files != null;
            for (File file : files) {
                if (file.isDirectory()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static FormatType findInputFormatType(File path, boolean isRecursive) {
        if (path.isDirectory()) {
            File[] files;
            if (isRecursive) {
                files = FileUtils.listFiles(path, null, true).toArray(new File[0]);
            } else {
                files = path.listFiles();
            }
            assert files != null;
            for (File file : files) {
                if (file.isFile()) {
                    String extension = getExtension(file);
                    if (FormatType.KML.getExtension().equalsIgnoreCase(extension) || FormatType.KML.getSubExtension().equalsIgnoreCase(extension)) {
                        log.info("Auto Selected Format type: {}", FormatType.KML);
                        return FormatType.KML;
                    }
                }
            }
            for (File file : files) {
                if (file.isFile()) {
                    String extension = getExtension(file);
                    FormatType formatType = FormatType.fromExtension(extension);
                    if (Objects.nonNull(formatType)) {
                        log.info("Auto Selected Format type: {}", formatType);
                        return formatType;
                    } else {
                        throw new IllegalArgumentException("Unsupported format: " + extension);
                    }
                }
            }
        } else if (path.isFile()) {
            String extension = getExtension(path);
            FormatType formatType = FormatType.fromExtension(extension);
            if (Objects.nonNull(formatType)) {
                log.info("Auto Selected Format type: {}", formatType);
                return formatType;
            } else {
                throw new IllegalArgumentException("Unsupported format: " + extension);
            }
        }
        throw new IllegalArgumentException("Can't found input files: " + path.getAbsolutePath());
    }

    public static FormatType findOutputFormatType(FormatType inputFormat) {
        if (FormatType.LAS == inputFormat || FormatType.LAZ == inputFormat) {
            return FormatType.PNTS;
        } else {
            return FormatType.B3DM;
        }
    }

    private static File getInputPath(String path) {
        return new File(path);
    }

    private static String getExtension(File path) {
        String fileName = path.getName();
        if (fileName.lastIndexOf(".") == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
}
