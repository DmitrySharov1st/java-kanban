package service;

import model.*;
import model.enums.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class FileBackedTaskManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void testSaveAndLoadEmptyFile() throws IOException {
        File file = tempDir.resolve("test.csv").toFile();
        FileBackedTaskManager manager = new FileBackedTaskManager(file);

        // Сохраняем пустое состояние
        manager.save();

        // Загружаем из файла
        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(file);

        assertTrue(loadedManager.getAllTasks().isEmpty());
        assertTrue(loadedManager.getAllEpics().isEmpty());
        assertTrue(loadedManager.getAllSubtasks().isEmpty());
    }

    @Test
    void testSaveAndLoadMultipleTasks() throws IOException {
        File file = tempDir.resolve("test.csv").toFile();
        FileBackedTaskManager manager = new FileBackedTaskManager(file);

        // Создаем задачи
        Task task = new Task("Задача 1", "Описание задачи 1", Status.NEW);
        manager.createTask(task);

        Epic epic = new Epic("Эпик 1", "Описание эпика 1");
        manager.createEpic(epic);

        Subtask subtask = new Subtask("Подзадача 1", "Описание подзадачи 1",
                Status.NEW, epic.getId());
        manager.createSubtask(subtask);

        // Загружаем из файла
        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(file);

        assertEquals(1, loadedManager.getAllTasks().size());
        assertEquals(1, loadedManager.getAllEpics().size());
        assertEquals(1, loadedManager.getAllSubtasks().size());

        Task loadedTask = loadedManager.getTaskById(task.getId());
        Epic loadedEpic = loadedManager.getEpicById(epic.getId());
        Subtask loadedSubtask = loadedManager.getSubtaskById(subtask.getId());

        assertNotNull(loadedTask);
        assertNotNull(loadedEpic);
        assertNotNull(loadedSubtask);

        assertEquals(task.getTitle(), loadedTask.getTitle());
        assertEquals(epic.getTitle(), loadedEpic.getTitle());
        assertEquals(subtask.getTitle(), loadedSubtask.getTitle());
    }

    @Test
    void testEpicSubtaskRelationshipsAfterLoad() throws IOException {
        File file = tempDir.resolve("test.csv").toFile();
        FileBackedTaskManager manager = new FileBackedTaskManager(file);

        Epic epic = new Epic("Эпик", "Описание эпика");
        manager.createEpic(epic);

        Subtask subtask1 = new Subtask("Подзадача 1", "Описание подзадачи 1", Status.NEW, epic.getId());
        Subtask subtask2 = new Subtask("Подзадача 2", "Описание подзадачи 2", Status.DONE, epic.getId());

        manager.createSubtask(subtask1);
        manager.createSubtask(subtask2);

        // Загружаем из файла
        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(file);

        Epic loadedEpic = loadedManager.getEpicById(epic.getId());
        assertNotNull(loadedEpic);
        assertEquals(2, loadedEpic.getSubtaskIds().size());

        // Проверяем статус эпика (должен быть IN_PROGRESS, т.к. подзадачи в разных статусах)
        assertEquals(Status.IN_PROGRESS, loadedEpic.getStatus());
    }
}