package service;

import model.*;
import model.enums.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class FileBackedTaskManagerTest extends TaskManagerTest<FileBackedTaskManager> {

    @TempDir
    Path tempDir;

    private File testFile;

    @Override
    protected FileBackedTaskManager createTaskManager() {
        try {
            testFile = tempDir.resolve("test_tasks.csv").toFile();
            return new FileBackedTaskManager(testFile);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка создания FileBackedTaskManager", e);
        }
    }

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
        assertTrue(loadedManager.getPrioritizedTasks().isEmpty());
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

        Optional<Task> loadedTaskOptional = loadedManager.getTaskById(task.getId());
        Optional<Epic> loadedEpicOptional = loadedManager.getEpicById(epic.getId());
        Optional<Subtask> loadedSubtaskOptional = loadedManager.getSubtaskById(subtask.getId());

        assertTrue(loadedTaskOptional.isPresent());
        assertTrue(loadedEpicOptional.isPresent());
        assertTrue(loadedSubtaskOptional.isPresent());

        Task loadedTask = loadedTaskOptional.get();
        Epic loadedEpic = loadedEpicOptional.get();
        Subtask loadedSubtask = loadedSubtaskOptional.get();

        assertEquals(task.getTitle(), loadedTask.getTitle());
        assertEquals(epic.getTitle(), loadedEpic.getTitle());
        assertEquals(subtask.getTitle(), loadedSubtask.getTitle());
    }

    @Test
    void testEpicWithSubtasksAfterLoad() throws IOException {
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

        Optional<Epic> loadedEpicOptional = loadedManager.getEpicById(epic.getId());
        assertTrue(loadedEpicOptional.isPresent());

        Epic loadedEpic = loadedEpicOptional.get();
        assertEquals(2, loadedEpic.getSubtaskIds().size());

        // Проверяем статус эпика (должен быть IN_PROGRESS, т.к. подзадачи в разных статусах)
        assertEquals(Status.IN_PROGRESS, loadedEpic.getStatus());
    }

    //Проверка сохранения и загрузки задачи со временем
    @Test
    void testSaveAndLoadTasksWithTimeParameters() throws IOException {
        File file = tempDir.resolve("test.csv").toFile();
        FileBackedTaskManager manager = new FileBackedTaskManager(file);

        // Создаем задачи с временными параметрами
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        Duration duration = Duration.ofMinutes(45);

        Task task = new Task("Задача со временем", "Описание", Status.NEW, duration, startTime);
        manager.createTask(task);

        Epic epic = new Epic("Эпик со временем", "Описание эпика");
        manager.createEpic(epic);

        Subtask subtask = new Subtask("Подзадача со временем", "Описание подзадачи",
                Status.NEW, epic.getId(), duration, startTime.plusHours(2));
        manager.createSubtask(subtask);

        // Загружаем из файла
        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(file);

        Optional<Task> loadedTaskOptional = loadedManager.getTaskById(task.getId());
        Optional<Subtask> loadedSubtaskOptional = loadedManager.getSubtaskById(subtask.getId());

        assertTrue(loadedTaskOptional.isPresent());
        assertTrue(loadedSubtaskOptional.isPresent());

        Task loadedTask = loadedTaskOptional.get();
        Subtask loadedSubtask = loadedSubtaskOptional.get();

        // Проверяем временные параметры
        assertEquals(startTime, loadedTask.getStartTime());
        assertEquals(duration, loadedTask.getDuration());
        assertEquals(startTime.plus(duration), loadedTask.getEndTime());

        assertEquals(startTime.plusHours(2), loadedSubtask.getStartTime());
        assertEquals(duration, loadedSubtask.getDuration());
    }

    //проверка корректности вычисления времени выполнения эпика после загрузки
    @Test
    void testEpicTimeCalculationAfterLoad() throws IOException {
        File file = tempDir.resolve("test.csv").toFile();
        FileBackedTaskManager manager = new FileBackedTaskManager(file);

        Epic epic = new Epic("Эпик", "Описание эпика");
        manager.createEpic(epic);

        LocalDateTime startTime1 = LocalDateTime.now().plusHours(1);
        LocalDateTime startTime2 = startTime1.plusHours(2);
        Duration duration1 = Duration.ofMinutes(30);
        Duration duration2 = Duration.ofMinutes(45);

        Subtask subtask1 = new Subtask("Подзадача 1", "Описание 1",
                Status.NEW, epic.getId(), duration1, startTime1);
        Subtask subtask2 = new Subtask("Подзадача 2", "Описание 2",
                Status.NEW, epic.getId(), duration2, startTime2);

        manager.createSubtask(subtask1);
        manager.createSubtask(subtask2);

        // Загружаем из файла
        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(file);

        Optional<Epic> loadedEpicOptional = loadedManager.getEpicById(epic.getId());
        assertTrue(loadedEpicOptional.isPresent());

        Epic loadedEpic = loadedEpicOptional.get();

        // Проверяем расчет времени эпика
        assertEquals(startTime1, loadedEpic.getStartTime());
        assertEquals(Duration.ofMinutes(75), loadedEpic.getDuration());
        assertEquals(startTime2.plus(duration2), loadedEpic.getEndTime());
    }

    //проверка загрузки списка приоритетных задач
    @Test
    void testPrioritizedTasksAfterLoad() throws IOException {
        File file = tempDir.resolve("test.csv").toFile();
        FileBackedTaskManager manager = new FileBackedTaskManager(file);

        LocalDateTime time1 = LocalDateTime.now().plusHours(1);
        LocalDateTime time2 = time1.plusHours(2);

        Task task1 = new Task("Задача 1", "Описание 1", Status.NEW, Duration.ofMinutes(30), time2);
        Task task2 = new Task("Задача 2", "Описание 2", Status.NEW, Duration.ofMinutes(45), time1);

        manager.createTask(task1);
        manager.createTask(task2);

        // Загружаем из файла
        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(file);

        // Проверяем приоритетный список (должен быть отсортирован по времени начала)
        var prioritizedTasks = loadedManager.getPrioritizedTasks();
        assertEquals(2, prioritizedTasks.size());
        assertEquals(task2.getId(), prioritizedTasks.get(0).getId()); // task2 должна быть первой
        assertEquals(task1.getId(), prioritizedTasks.get(1).getId());
    }

    //проверка попадания задач без времени в список приоритетных задач после загрузки
    @Test
    void testTasksWithoutTimeNotInPrioritizedListAfterLoad() throws IOException {
        File file = tempDir.resolve("test.csv").toFile();
        FileBackedTaskManager manager = new FileBackedTaskManager(file);

        Task taskWithTime = new Task("Задача со временем", "Описание", Status.NEW,
                Duration.ofMinutes(30), LocalDateTime.now());
        Task taskWithoutTime = new Task("Задача без времени", "Описание", Status.NEW);

        manager.createTask(taskWithTime);
        manager.createTask(taskWithoutTime);

        // Загружаем из файла
        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(file);

        var prioritizedTasks = loadedManager.getPrioritizedTasks();
        assertEquals(1, prioritizedTasks.size());
        assertEquals(taskWithTime.getId(), prioritizedTasks.get(0).getId());
    }

    //проверка того, что история просмотров не сохраняется после загрузки задач
    @Test
    void testHistoryPreservationAfterLoad() throws IOException {
        File file = tempDir.resolve("test.csv").toFile();
        FileBackedTaskManager manager = new FileBackedTaskManager(file);

        Task task = new Task("Задача", "Описание", Status.NEW);
        Epic epic = new Epic("Эпик", "Описание эпика");

        manager.createTask(task);
        manager.createEpic(epic);

        // Добавляем в историю просмотров
        manager.getTaskById(task.getId());
        manager.getEpicById(epic.getId());

        // Сохраняем и загружаем
        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(file);

        // История не должна сохраняться в файл (по условиям задачи сохраняются только задачи)
        assertTrue(loadedManager.getHistory().isEmpty());
    }

    //проверка загрузки из несуществующего файла
    @Test
    void testLoadFromNonExistentFile() {
        File nonExistentFile = new File("non_existent_file.csv");

        assertThrows(ManagerSaveException.class, () -> {
            FileBackedTaskManager.loadFromFile(nonExistentFile);
        }, "Должно быть выброшено исключение при загрузке из несуществующего файла");
    }

    //проверка того, что в файл только для чтения нельзя записать данные
    @Test
    void testSaveToReadOnlyFile() throws IOException {
        File file = tempDir.resolve("readonly.csv").toFile();
        FileBackedTaskManager manager = new FileBackedTaskManager(file);

        // Создаем задачу
        Task task = new Task("Задача", "Описание", Status.NEW);
        manager.createTask(task);

        // Делаем файл доступным только для чтения
        file.setReadOnly();

        // Попытка сохранения должна выбросить исключение
        assertThrows(ManagerSaveException.class, () -> {
            manager.createTask(new Task("Новая задача", "Описание", Status.NEW));
        }, "Должно быть выброшено исключение при сохранении в файл только для чтения");
    }

    //проверка того, что после загрузки взаимосвязи эпика и подзадач корректны
    @Test
    void testEpicSubtaskRelationshipsAfterLoad() throws IOException {
        File file = tempDir.resolve("test.csv").toFile();
        FileBackedTaskManager manager = new FileBackedTaskManager(file);

        Epic epic1 = new Epic("Эпик 1", "Описание эпика 1");
        Epic epic2 = new Epic("Эпик 2", "Описание эпика 2");

        manager.createEpic(epic1);
        manager.createEpic(epic2);

        Subtask subtask1 = new Subtask("Подзадача 1", "Описание 1", Status.NEW, epic1.getId());
        Subtask subtask2 = new Subtask("Подзадача 2", "Описание 2", Status.NEW, epic1.getId());
        Subtask subtask3 = new Subtask("Подзадача 3", "Описание 3", Status.NEW, epic2.getId());

        manager.createSubtask(subtask1);
        manager.createSubtask(subtask2);
        manager.createSubtask(subtask3);

        // Загружаем из файла
        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(file);

        Optional<Epic> loadedEpic1Optional = loadedManager.getEpicById(epic1.getId());
        Optional<Epic> loadedEpic2Optional = loadedManager.getEpicById(epic2.getId());

        assertTrue(loadedEpic1Optional.isPresent());
        assertTrue(loadedEpic2Optional.isPresent());

        Epic loadedEpic1 = loadedEpic1Optional.get();
        Epic loadedEpic2 = loadedEpic2Optional.get();

        // Проверяем связи эпиков и подзадач
        assertEquals(2, loadedEpic1.getSubtaskIds().size());
        assertEquals(1, loadedEpic2.getSubtaskIds().size());
        assertTrue(loadedEpic1.getSubtaskIds().contains(subtask1.getId()));
        assertTrue(loadedEpic1.getSubtaskIds().contains(subtask2.getId()));
        assertTrue(loadedEpic2.getSubtaskIds().contains(subtask3.getId()));
    }

    //проверка того, что данные в задаче после загрузки соответствуют данным при сохранении задачи
    @Test
    void testTaskDataIntegrityAfterLoad() throws IOException {
        File file = tempDir.resolve("test.csv").toFile();
        FileBackedTaskManager manager = new FileBackedTaskManager(file);

        // Создаем задачу с различными параметрами
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        Duration duration = Duration.ofMinutes(90);

        Task originalTask = new Task("Тестовая задача", "Подробное описание тестовой задачи",
                Status.IN_PROGRESS, duration, startTime);
        originalTask.setId(123); // задаем ID вручную

        manager.createTask(originalTask);

        // Загружаем из файла
        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(file);

        Optional<Task> loadedTaskOptional = loadedManager.getTaskById(originalTask.getId());
        assertTrue(loadedTaskOptional.isPresent());

        Task loadedTask = loadedTaskOptional.get();

        // Проверяем целостность всех данных
        assertEquals(originalTask.getId(), loadedTask.getId());
        assertEquals(originalTask.getTitle(), loadedTask.getTitle());
        assertEquals(originalTask.getDescription(), loadedTask.getDescription());
        assertEquals(originalTask.getStatus(), loadedTask.getStatus());
        assertEquals(originalTask.getStartTime(), loadedTask.getStartTime());
        assertEquals(originalTask.getDuration(), loadedTask.getDuration());
        assertEquals(originalTask.getEndTime(), loadedTask.getEndTime());
    }

    //проверка сохранения и загрузки пустого эпика
    @Test
    void testEmptyEpicAfterLoad() throws IOException {
        File file = tempDir.resolve("test.csv").toFile();
        FileBackedTaskManager manager = new FileBackedTaskManager(file);

        Epic epic = new Epic("Пустой эпик", "Эпик без подзадач");
        manager.createEpic(epic);

        // Загружаем из файла
        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(file);

        Optional<Epic> loadedEpicOptional = loadedManager.getEpicById(epic.getId());
        assertTrue(loadedEpicOptional.isPresent());

        Epic loadedEpic = loadedEpicOptional.get();

        // Проверяем, что у пустого эпика корректные временные параметры
        assertTrue(loadedEpic.getSubtaskIds().isEmpty());
        assertNull(loadedEpic.getStartTime());
        assertEquals(Duration.ZERO, loadedEpic.getDuration());
        assertNull(loadedEpic.getEndTime());
        assertEquals(Status.NEW, loadedEpic.getStatus());
    }

    //проверка корректности формата файла с данными
    @Test
    void testFileFormatWithTimeParameters() throws IOException {
        // Создаем задачи с временными параметрами
        LocalDateTime startTime = LocalDateTime.of(2023, 12, 1, 10, 0);
        Duration duration = Duration.ofMinutes(45);

        Task task = new Task("Тест", "Описание", Status.NEW, duration, startTime);
        taskManager.createTask(task);

        // Читаем содержимое файла
        List<String> lines = Files.readAllLines(testFile.toPath());

        // Проверяем, что файл содержит хотя бы 2 строки (заголовок и данные)
        assertTrue(lines.size() >= 2, "Файл должен содержать как минимум 2 строки. Фактическое количество: " + lines.size());

        // Проверяем заголовок
        String headerLine = lines.get(0);
        assertEquals("id,type,name,status,description,epic,startTime,duration", headerLine,
                "Заголовок не совпадает. Фактический: " + headerLine);

        // Проверяем строку с задачей
        String taskLine = lines.get(1);
        String[] fields = taskLine.split(",");

        // Теперь должно быть 8 полей
        assertEquals(8, fields.length,
                "Строка задачи должна содержать 8 полей. Фактическое количество: " + fields.length);

        assertEquals("1", fields[0], "ID задачи не совпадает");
        assertEquals("TASK", fields[1], "Тип задачи не совпадает");
        assertEquals("Тест", fields[2], "Название задачи не совпадает");
        assertEquals("NEW", fields[3], "Статус задачи не совпадает");
        assertEquals("Описание", fields[4], "Описание задачи не совпадает");
        assertEquals("", fields[5], "Поле epic для TASK должно быть пустым");
        assertEquals("2023-12-01T10:00", fields[6], "Время начала не совпадает");
        assertEquals("45", fields[7], "Продолжительность не совпадает");
    }

    //проверка соответствия временных параметров после загрузки данным при создании файла
    @Test
    void testLoadFromFileWithTimeParameters() throws IOException {
        // Создаем CSV содержимое с временными параметрами
        String csvContent = "id,type,name,status,description,epic,startTime,duration\n" +
                "1,TASK,Задача со временем,NEW,Описание,,2023-12-01T10:00:00,45\n" +
                "2,EPIC,Эпик,NEW,Описание эпика,,,\n" +
                "3,SUBTASK,Подзадача,NEW,Описание подзадачи,2,2023-12-01T11:00:00,30\n";

        Files.writeString(testFile.toPath(), csvContent);

        // Загружаем из файла
        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(testFile);

        assertEquals(1, loadedManager.getAllTasks().size());
        assertEquals(1, loadedManager.getAllEpics().size());
        assertEquals(1, loadedManager.getAllSubtasks().size());

        Optional<Task> taskOptional = loadedManager.getTaskById(1);
        Optional<Subtask> subtaskOptional = loadedManager.getSubtaskById(3);

        assertTrue(taskOptional.isPresent());
        assertTrue(subtaskOptional.isPresent());

        Task task = taskOptional.get();
        Subtask subtask = subtaskOptional.get();

        assertEquals(LocalDateTime.of(2023, 12, 1, 10, 0), task.getStartTime());
        assertEquals(Duration.ofMinutes(45), task.getDuration());

        assertEquals(LocalDateTime.of(2023, 12, 1, 11, 0), subtask.getStartTime());
        assertEquals(Duration.ofMinutes(30), subtask.getDuration());
    }

    //проверка автоматического сохранения данных при создании/изменении
    @Test
    void testAutoSaveOnOperations() throws IOException {
        // Проверяем, что операции автоматически сохраняются
        Task task = new Task("Задача", "Описание", Status.NEW);
        taskManager.createTask(task);

        Epic epic = new Epic("Эпик", "Описание");
        taskManager.createEpic(epic);

        Subtask subtask = new Subtask("Подзадача", "Описание", Status.NEW, epic.getId());
        taskManager.createSubtask(subtask);

        // Обновляем задачу
        task.setStatus(Status.IN_PROGRESS);
        taskManager.updateTask(task);

        // Удаляем подзадачу
        taskManager.deleteSubtaskById(subtask.getId());

        // Загружаем и проверяем, что все изменения сохранены
        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(testFile);

        assertEquals(1, loadedManager.getAllTasks().size());
        assertEquals(1, loadedManager.getAllEpics().size());
        assertEquals(0, loadedManager.getAllSubtasks().size());

        Optional<Task> loadedTask = loadedManager.getTaskById(task.getId());
        assertTrue(loadedTask.isPresent());
        assertEquals(Status.IN_PROGRESS, loadedTask.get().getStatus());
    }
}