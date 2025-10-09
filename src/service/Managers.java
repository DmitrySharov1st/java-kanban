package service;

import java.io.File;
import java.io.IOException;

public class Managers {
    private Managers() {
        // Приватный конструктор предотвращает создание экземпляров
    }

    public static TaskManager getDefault() {
        return new InMemoryTaskManager();
    }

    public static TaskManager getDefaultFileBacked() {
        try {
            File tempFile = File.createTempFile("tasks", ".csv");
            return new FileBackedTaskManager(tempFile);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка создания временного файла для FileBackedTaskManager", e);
        }
    }

    public static HistoryManager getDefaultHistory() {
        return new InMemoryHistoryManager();
    }



}