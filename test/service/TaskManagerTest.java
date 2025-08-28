package service;

import model.Epic;
import model.Subtask;
import model.Task;
import model.enums.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.HistoryManager;
import service.InMemoryHistoryManager;
import service.InMemoryTaskManager;
import service.Managers;
import service.TaskManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskManagerTest {
    private TaskManager taskManager;
    private HistoryManager historyManager;

    @BeforeEach
    void setUp() {
        historyManager = Managers.getDefaultHistory();
        taskManager = Managers.getDefault();
    }

    // Проверьте, что экземпляры класса Task равны друг другу, если равен их id
    @Test
    void testTaskEqualsById() {
        Task task1 = new Task("Задача 1", "Описание 1", Status.NEW);
        task1.setId(1);

        Task task2 = new Task("Задача 2", "Описание 2", Status.DONE);
        task2.setId(1); // Тот же ID

        Task task3 = new Task("Задача 3", "Описание 3", Status.NEW);
        task3.setId(2); // Другой ID

        assertEquals(task1, task2, "Задачи с одинаковым ID должны быть равны");
        assertNotEquals(task1, task3, "Задачи с разными ID не должны быть равны");
        assertEquals(task1.hashCode(), task2.hashCode(), "Хэш-коды задач с одинаковым ID должны совпадать");
    }

    // Проверьте, что наследники класса Task равны друг другу, если равен их id
    @Test
    void testSubtaskEqualsById() {
        Epic epic = new Epic("Эпик", "Описание эпика");
        epic.setId(1);
        taskManager.createEpic(epic); // Создаем эпик, чтобы получить ID

        Subtask subtask1 = new Subtask("Подзадача 1", "Описание 1", Status.NEW, epic.getId());
        subtask1.setId(10);

        Subtask subtask2 = new Subtask("Подзадача 2", "Описание 2", Status.DONE, epic.getId());
        subtask2.setId(10); // Тот же ID

        Subtask subtask3 = new Subtask("Подзадача 3", "Описание 3", Status.NEW, epic.getId());
        subtask3.setId(11); // Другой ID

        assertEquals(subtask1, subtask2, "Подзадачи с одинаковым ID должны быть равны");
        assertNotEquals(subtask1, subtask3, "Подзадачи с разными ID не должны быть равны");
        assertEquals(subtask1.hashCode(), subtask2.hashCode(), "Хэш-коды подзадач с одинаковым ID должны совпадать");
    }

    // Проверьте, что наследники класса Task равны друг другу, если равен их id (Epic)
    @Test
    void testEpicEqualsById() {
        Epic epic1 = new Epic("Эпик 1", "Описание 1");
        epic1.setId(5);

        Epic epic2 = new Epic("Эпик 2", "Описание 2");
        epic2.setId(5); // Тот же ID

        Epic epic3 = new Epic("Эпик 3", "Описание 3");
        epic3.setId(6); // Другой ID

        assertEquals(epic1, epic2, "Эпики с одинаковым ID должны быть равны");
        assertNotEquals(epic1, epic3, "Эпики с разными ID не должны быть равны");
        assertEquals(epic1.hashCode(), epic2.hashCode(), "Хэш-коды эпиков с одинаковым ID должны совпадать");
    }

    // Проверьте, что объект Epic нельзя добавить в самого себя в виде подзадачи
    @Test
    void testEpicCannotAddItselfAsSubtask() {
        Epic epic = new Epic("Тестовый Эпик", "Тестовое описание эпика");
        Epic createdEpic = taskManager.createEpic(epic);
        int epicId = createdEpic.getId();

        Subtask potentialSelfSubtask = new Subtask(
                "Тестовая подзадача",
                "Подзадача с таким же ID, как у Эпика",
                Status.NEW,
                epicId
        );
        potentialSelfSubtask.setId(epicId);

        assertEquals(epicId, potentialSelfSubtask.getId(), "ID подзадачи должен быть установлен");
        assertEquals(epicId, potentialSelfSubtask.getEpicId(), "EpicId подзадачи должен совпадать с её ID");

        assertThrows(IllegalArgumentException.class, () -> taskManager.createSubtask(potentialSelfSubtask),
                "Ожидалось исключение IllegalArgumentException при попытке сделать эпик подзадачей самого себя"
        );

        assertNull(taskManager.getSubtaskById(epicId),
                "Подзадача с некорректной ссылкой не должна быть добавлена в менеджер");

        Epic retrievedEpic = taskManager.getEpicById(epicId);
        assertNotNull(retrievedEpic, "Эпик должен существовать после неудачной попытки добавления подзадачи");
        assertEquals(createdEpic.getTitle(), retrievedEpic.getTitle(), "Заголовок эпика должен остаться неизменным");
        assertFalse(retrievedEpic.getSubtaskIds().contains(epicId),
                "ID эпика не должен присутствовать в его собственном списке подзадач");
    }

    // Проверьте, что объект Subtask нельзя сделать своим же эпиком
    @Test
    void testSubtaskCannotBeItsOwnEpic() {

        int potentialId = 99;
        Subtask selfReferencingSubtask = new Subtask(
                "Тестовая подзадача",
                "Подзадача будет создана как свой же эпик",
                Status.NEW,
                potentialId
        );
        selfReferencingSubtask.setId(potentialId);

        assertEquals(potentialId, selfReferencingSubtask.getId(), "ID подзадачи должен быть установлен");
        assertEquals(potentialId, selfReferencingSubtask.getEpicId(), "EpicId подзадачи должен совпадать с её ID");

        assertThrows(IllegalArgumentException.class, () -> taskManager.createSubtask(selfReferencingSubtask),
                "Ожидалось исключение IllegalArgumentException при попытке сделать подзадачу своим эпиком"
        );

        assertNull(taskManager.getSubtaskById(potentialId),
                "Подзадача с некорректной ссылкой не должна быть добавлена в менеджер");
    }

    // Убедитесь, что утилитарный класс всегда возвращает проинициализированные и готовые к работе экземпляры менеджеров
    @Test
    void testManagersReturnsInitializedInstances() {

        assertNotNull(taskManager, "TaskManager не должен быть null");
        assertNotNull(historyManager, "HistoryManager не должен быть null");

        assertTrue(taskManager.getAllTasks().isEmpty(), "Новый TaskManager должен иметь пустой список задач");
        assertTrue(taskManager.getAllEpics().isEmpty(), "Новый TaskManager должен иметь пустой список эпиков");
        assertTrue(taskManager.getAllSubtasks().isEmpty(), "Новый TaskManager должен иметь пустой список подзадач");
        assertTrue(taskManager.getHistory().isEmpty(), "Новый TaskManager должен иметь пустую историю");

        assertTrue(historyManager.getHistory().isEmpty(), "Новый HistoryManager должен иметь пустую историю");
    }

    // Проверьте, что InMemoryTaskManager действительно добавляет задачи разного типа и может найти их по id
    @Test
    void testTaskManagerAddsAndFindsTasksById() {
        Task task = new Task("Тестовая задача", "Тестовое описание задачи", Status.NEW);
        Epic epic = new Epic("Тестовый эпик", "Тестовое описание эпика");
        taskManager.createEpic(epic); // Создаем эпик, чтобы получить ID для подзадачи
        Subtask subtask = new Subtask("Тестовая подзадача", "Тестовое описание подзадачи",
                Status.NEW, epic.getId());

        Task createdTask = taskManager.createTask(task);
        Epic createdEpic = taskManager.createEpic(epic);
        Subtask createdSubtask = taskManager.createSubtask(subtask);

        assertNotNull(createdTask.getId(), "ID задачи должен быть установлен");
        assertNotNull(createdEpic.getId(), "ID эпика должен быть установлен");
        assertNotNull(createdSubtask.getId(), "ID подзадачи должен быть установлен");

        Task foundTask = taskManager.getTaskById(createdTask.getId());
        Epic foundEpic = taskManager.getEpicById(createdEpic.getId());
        Subtask foundSubtask = taskManager.getSubtaskById(createdSubtask.getId());

        assertNotNull(foundTask, "Задача должна быть найдена по ID");
        assertNotNull(foundEpic, "Эпик должен быть найден по ID");
        assertNotNull(foundSubtask, "Подзадача должна быть найдена по ID");

        assertEquals(createdTask, foundTask, "Найденная задача должна совпадать с созданной");
        assertEquals(createdEpic, foundEpic, "Найденный эпик должен совпадать с созданным");
        assertEquals(createdSubtask, foundSubtask, "Найденная подзадача должна совпадать с созданной");
    }

    // Проверьте, что задачи с заданным id и сгенерированным id не конфликтуют внутри менеджера
    @Test
    void testTasksWithPredefinedAndGeneratedIdsDoNotConflict() {
        int predefinedId = 999;
        Task predefinedTask = new Task("Предопределенная задача", "Описание предопределенной задачи",
                Status.NEW);
        predefinedTask.setId(predefinedId);

        Task normalTask = new Task("Нормальная задача", "Описание нормальной задачи", Status.NEW);
        Task createdNormalTask = taskManager.createTask(normalTask);

        taskManager.createTask(predefinedTask);

        Task foundPredefinedTask = taskManager.getTaskById(predefinedId);
        assertNotNull(foundPredefinedTask, "Задача с предопределенным ID должна существовать");
        assertEquals(predefinedTask.getTitle(), foundPredefinedTask.getTitle(), "Заголовки должны совпадать");

        if (createdNormalTask.getId() != predefinedId) {
            Task foundNormalTask = taskManager.getTaskById(createdNormalTask.getId());
            assertNotNull(foundNormalTask, "Нормальная задача должна существовать");
            assertEquals(normalTask.getTitle(), foundNormalTask.getTitle(), "Заголовки должны совпадать");
        }

    }

    // Создайте тест, в котором проверяется неизменность задачи (по всем полям) при добавлении задачи в менеджер
    @Test
    void testTaskImmutabilityUponAdding() {
        Task originalTask = new Task("Задача", "Описание задачи", Status.NEW);

        String originalTitle = originalTask.getTitle();
        String originalDescription = originalTask.getDescription();
        Status originalStatus = originalTask.getStatus();

        Integer originalId = originalTask.getId();
        Task returnedTask = taskManager.createTask(originalTask);

        assertEquals(originalTitle, returnedTask.getTitle(), "Заголовок должен остаться неизменным");
        assertEquals(originalDescription, returnedTask.getDescription(), "Описание должно остаться неизменным");
        assertEquals(originalStatus, returnedTask.getStatus(), "Статус должен остаться неизменным");
        assertNotNull(returnedTask.getId(), "ID должен быть установлен");

        assertNotNull(originalTask.getId(), "ID в исходном объекте также должен быть установлен createTask");

    }

    // Убедитесь, что задачи, добавляемые в HistoryManager, сохраняют предыдущую версию задачи и её данных.
    @Test
    void testHistoryManagerPreservesTaskData() {

        Task task = new Task("Задача для истории", "Описание задачи для истории", Status.NEW);
        Task createdTask = taskManager.createTask(task);

        Task taskFromGet = taskManager.getTaskById(createdTask.getId());
        assertNotNull(taskFromGet, "Задача для истории должна существовать");

        taskFromGet.setTitle("Обновленная задача");
        taskFromGet.setDescription("Обновленное описание");
        taskFromGet.setStatus(Status.IN_PROGRESS);
        taskManager.updateTask(taskFromGet);

        Task updatedTaskFromGet = taskManager.getTaskById(createdTask.getId());
        assertNotNull(updatedTaskFromGet, "Обновленная задача должна существовать");

        List<Task> history = taskManager.getHistory();

        assertFalse(history.isEmpty(), "История не должна быть пустой после get-запросов");

        assertEquals(2, history.size(), "История должна содержать две записи после двух вызовов getTaskById");

        Task firstInHistory = history.get(0);
        Task lastInHistory = history.get(1);

        assertEquals(createdTask.getId(), firstInHistory.getId(), "ID задач в истории должен совпадать");
        assertEquals(createdTask.getId(), lastInHistory.getId(), "ID задач в истории должен совпадать");

        assertEquals("Задача для истории", firstInHistory.getTitle(),
                "Заголовок в истории должен соответствовать состоянию на момент добавления");
        assertEquals("Описание задачи для истории", firstInHistory.getDescription(),
                "Описание в истории должно соответствовать состоянию на момент добавления");
        assertEquals(Status.NEW, firstInHistory.getStatus(),
                "Статус в истории должен соответствовать состоянию на момент добавления");

        assertEquals("Обновленная задача", lastInHistory.getTitle(),
                "Заголовок в истории должен соответствовать состоянию на момент добавления");
        assertEquals("Обновленное описание", lastInHistory.getDescription(),
                "Описание в истории должно соответствовать состоянию на момент добавления");
        assertEquals(Status.IN_PROGRESS, lastInHistory.getStatus(),
                "Статус в истории должен соответствовать состоянию на момент добавления");

    }
}
