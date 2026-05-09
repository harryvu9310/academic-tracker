package com.tracker.academictracker.service;
import com.tracker.academictracker.model.Student;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class DataManager {
    private static final String FILE_NAME = "student_data.json";
    private static final String APP_DIR_NAME = ".academic-tracker";
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    public static void saveStudent(Student student) throws IOException {
        if (student == null) {
            throw new IllegalArgumentException("Student cannot be null");
        }
        try {
            student.normalizeAfterLoad();
            StudentDataValidator.validateForPersistence(student);
        } catch (IllegalArgumentException e) {
            throw new IOException("Student data is invalid and was not saved: " + e.getMessage(), e);
        }
        String json = gson.toJson(student);
        Path targetPath = getSavePath();
        Files.createDirectories(targetPath.getParent());
        Path tempPath = targetPath.resolveSibling(FILE_NAME + ".tmp");
        Files.writeString(tempPath, json, StandardCharsets.UTF_8);
        Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        System.out.println("Data successfully saved to " + targetPath);
    }

    public static Student loadStudent() throws IOException {
        Path targetPath = getSavePath();
        if (!Files.exists(targetPath)) {
            System.out.println("No existing save file found.");
            return null;
        }

        try {
            String json = Files.readString(targetPath, StandardCharsets.UTF_8);
            Student loadedStudent = gson.fromJson(json, Student.class);
            if (loadedStudent != null) {
                try {
                    loadedStudent.normalizeAfterLoad();
                    StudentDataValidator.validateForPersistence(loadedStudent);
                } catch (IllegalArgumentException e) {
                    throw new IOException("Save file contains invalid Academic Tracker data: " + e.getMessage(), e);
                }
                System.out.println("Data successfully loaded for: " + loadedStudent.getFullName());
            }
            return loadedStudent;
        } catch (JsonSyntaxException e) {
            throw new IOException("Save file is not valid Academic Tracker JSON: " + targetPath, e);
        }
    }

    public static Student previewImport(Path sourcePath) throws IOException {
        try {
            String json = Files.readString(sourcePath, StandardCharsets.UTF_8);
            Student imported = gson.fromJson(json, Student.class);
            if (imported == null) {
                throw new IOException("Imported file is empty.");
            }
            try {
                imported.normalizeAfterLoad();
                StudentDataValidator.validateForPersistence(imported);
            } catch (IllegalArgumentException e) {
                throw new IOException("Imported file contains invalid Academic Tracker data: " + e.getMessage(), e);
            }
            return imported;
        } catch (JsonSyntaxException e) {
            throw new IOException("Imported file is not valid Academic Tracker JSON.", e);
        }
    }

    public static void importStudent(Path sourcePath) throws IOException {
        Student imported = previewImport(sourcePath);
        saveStudent(imported);
    }

    public static void exportStudent(Path destinationPath) throws IOException {
        Path sourcePath = getSavePath();
        if (!Files.exists(sourcePath)) {
            throw new IOException("No save data found at " + sourcePath);
        }
        Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
    }

    public static boolean saveFileExists() {
        return Files.exists(getSavePath());
    }

    public static Path getSavePath() {
        String override = System.getProperty("academic.tracker.data.file");
        if (override == null || override.isBlank()) {
            override = System.getenv("ACADEMIC_TRACKER_DATA_FILE");
        }
        if (override != null && !override.isBlank()) {
            return Paths.get(override).toAbsolutePath();
        }
        return Paths.get(System.getProperty("user.home"), APP_DIR_NAME, FILE_NAME);
    }
}
