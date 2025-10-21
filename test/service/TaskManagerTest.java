package service;

import model.Epic;
import model.Subtask;
import model.Task;
import model.enums.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public abstract class TaskManagerTest<T extends TaskManager> {
    protected T taskManager;

    protected abstract T createTaskManager();

    @BeforeEach
    void setUp() {
        taskManager = createTaskManager();
    }

    //проверка создания задачи со временем выполнения
    @Test
    void testTaskCreationWithTime() {
        LocalDateTime startTime = LocalDateTime.now();
        Duration duration = Duration.ofMinutes(30);
        Task task = new Task("Задача", "Описание задачи", Status.NEW, duration, startTime);

        Task created = taskManager.createTask(task);

        assertNotNull(created.getId());
        assertEquals(startTime, created.getStartTime());
        assertEquals(duration, created.getDuration());
        assertEquals(startTime.plus(duration), created.getEndTime());
    }

    //проверка вычисления времени выполнения эпика
    @Test
    void testEpicTimeCalculation() {
        Epic epic = taskManager.createEpic(new Epic("Эпик", "Описание эпика"));

        LocalDateTime startTime1 = LocalDateTime.now();
        LocalDateTime startTime2 = startTime1.plusHours(1);
        Duration duration1 = Duration.ofMinutes(30);
        Duration duration2 = Duration.ofMinutes(45);

        Subtask subtask1 = new Subtask("Подзадача 1", "Описание 1",
                Status.NEW, epic.getId(), duration1, startTime1);
        Subtask subtask2 = new Subtask("Подзадача 2", "Описание 2",
                Status.NEW, epic.getId(), duration2, startTime2);

        taskManager.createSubtask(subtask1);
        taskManager.createSubtask(subtask2);

        Optional<Epic> updatedEpic = taskManager.getEpicById(epic.getId());
        assertTrue(updatedEpic.isPresent());
        assertEquals(startTime1, updatedEpic.get().getStartTime());
        assertEquals(Duration.ofMinutes(75), updatedEpic.get().getDuration());
        assertEquals(startTime2.plus(duration2), updatedEpic.get().getEndTime());
    }

    //проверка формирования списка приоритетных задач
    @Test
    void testPrioritizedTasks() {
        LocalDateTime time1 = LocalDateTime.now();
        LocalDateTime time2 = time1.plusHours(1);

        Task task1 = new Task("Задача 1", "Описание 1", Status.NEW, Duration.ofMinutes(30), time2);
        Task task2 = new Task("Задача 2", "Описание 2", Status.NEW, Duration.ofMinutes(45), time1);

        taskManager.createTask(task1);
        taskManager.createTask(task2);

        List<Task> prioritized = taskManager.getPrioritizedTasks();
        assertEquals(2, prioritized.size());
        assertEquals(task2.getId(), prioritized.get(0).getId());//Задача 2 должна быть первой (время у нее более раннее)
        assertEquals(task1.getId(), prioritized.get(1).getId());
    }

    //Проверка пересечения задач во времени
    @Test
    void testTimeOverlapValidation() {
        LocalDateTime startTime = LocalDateTime.now();
        Duration duration = Duration.ofMinutes(60);

        Task task1 = new Task("Задача 1", "Описание 1", Status.NEW, duration, startTime);
        taskManager.createTask(task1);

        Task task2 = new Task("Задача 2", "Описание 2", Status.NEW, duration, startTime.plusMinutes(30));

        assertThrows(ManagerValidateException.class, () -> taskManager.createTask(task2));
    }

    //проверка на то, что задача без времени не попадет в список приоритетных задач
    @Test
    void testTasksWithoutStartTimeNotInPrioritizedList() {
        Task taskWithTime = new Task("Задача со временем", "Описание", Status.NEW,
                Duration.ofMinutes(30), LocalDateTime.now());
        Task taskWithoutTime = new Task("Задача без времени", "Описание", Status.NEW);

        taskManager.createTask(taskWithTime);
        taskManager.createTask(taskWithoutTime);

        List<Task> prioritized = taskManager.getPrioritizedTasks();
        assertEquals(1, prioritized.size());
        assertEquals(taskWithTime.getId(), prioritized.get(0).getId());
    }

    //проверка на то, что задачи с точно существенно разным временем запуска и одной продолжительностью
    //не пересекаются по времени
    @Test
    void testNoTimeOverlapForNonOverlappingTasks() {
        LocalDateTime startTime1 = LocalDateTime.now();
        LocalDateTime startTime2 = startTime1.plusHours(2);
        Duration duration = Duration.ofMinutes(30);

        Task task1 = new Task("Задача 1", "Описание 1", Status.NEW, duration, startTime1);
        Task task2 = new Task("Задача 2", "Описание 2", Status.NEW, duration, startTime2);

        assertDoesNotThrow(() -> {
            taskManager.createTask(task1);
            taskManager.createTask(task2);
        });
    }

    //проверка обновления времени выполнения эпика после удаления подзадачи
    @Test
    void testUpdateEpicTimeAfterSubtaskDeletion() {
        Epic epic = taskManager.createEpic(new Epic("Эпик", "Описание"));

        LocalDateTime startTime = LocalDateTime.now();
        Duration duration = Duration.ofMinutes(30);

        Subtask subtask = new Subtask("Подзадача", "Описание",
                Status.NEW, epic.getId(), duration, startTime);
        Subtask createdSubtask = taskManager.createSubtask(subtask);

        Optional<Epic> epicOptional = taskManager.getEpicById(epic.getId());
        assertTrue(epicOptional.isPresent());
        assertEquals(startTime, epicOptional.get().getStartTime());

        // Удаляем подзадачу
        taskManager.deleteSubtaskById(createdSubtask.getId());

        Optional<Epic> updatedEpicOptional = taskManager.getEpicById(epic.getId());
        assertTrue(updatedEpicOptional.isPresent());
        assertNull(updatedEpicOptional.get().getStartTime());
        assertEquals(Duration.ZERO, updatedEpicOptional.get().getDuration());
    }

    //проверка, что при получении задачи возвращается тип Optional
    @Test
    void testOptionalReturnTypes() {
        Optional<Task> task = taskManager.getTaskById(999);
        assertTrue(task.isEmpty());

        Task newTask = new Task("Задача", "Описание", Status.NEW);
        taskManager.createTask(newTask);

        Optional<Task> foundTask = taskManager.getTaskById(newTask.getId());
        assertTrue(foundTask.isPresent());
        assertEquals(newTask.getId(), foundTask.get().getId());
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

        Optional<Task> foundTaskOptional = taskManager.getTaskById(createdTask.getId());
        Optional<Epic> foundEpicOptional = taskManager.getEpicById(createdEpic.getId());
        Optional<Subtask> foundSubtaskOptional = taskManager.getSubtaskById(createdSubtask.getId());

        assertTrue(foundTaskOptional.isPresent(), "Задача должна быть найдена по ID");
        assertTrue(foundEpicOptional.isPresent(), "Эпик должен быть найден по ID");
        assertTrue(foundSubtaskOptional.isPresent(), "Подзадача должна быть найдена по ID");

        Task foundTask = foundTaskOptional.get();
        Epic foundEpic = foundEpicOptional.get();
        Subtask foundSubtask = foundSubtaskOptional.get();

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

        Optional<Task> foundPredefinedTaskOptional = taskManager.getTaskById(predefinedId);
        assertTrue(foundPredefinedTaskOptional.isPresent(), "Задача с предопределенным ID должна существовать");
        assertEquals(predefinedTask.getTitle(), foundPredefinedTaskOptional.get().getTitle(),
                "Заголовки должны совпадать");

        if (createdNormalTask.getId() != predefinedId) {
            Optional<Task> foundNormalTaskOptional = taskManager.getTaskById(createdNormalTask.getId());
            assertTrue(foundNormalTaskOptional.isPresent(), "Нормальная задача должна существовать");
            assertEquals(normalTask.getTitle(), foundNormalTaskOptional.get().getTitle(),
                    "Заголовки должны совпадать");
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

    //В истории должен оставаться только последний просмотр (и он должен содержать snapshot состояния
    //на момент просмотра)
    @Test
    void testHistoryManagerPreservesTaskData() {

        Task task = new Task("Задача для истории", "Описание задачи для истории", Status.NEW);
        Task createdTask = taskManager.createTask(task);

        Optional<Task> taskFromGetOptional = taskManager.getTaskById(createdTask.getId());
        assertTrue(taskFromGetOptional.isPresent(), "Задача для истории должна существовать");
        Task taskFromGet = taskFromGetOptional.get();

        taskFromGet.setTitle("Обновленная задача");
        taskFromGet.setDescription("Обновленное описание");
        taskFromGet.setStatus(Status.IN_PROGRESS);
        taskManager.updateTask(taskFromGet);

        Optional<Task> updatedTaskFromGetOptional = taskManager.getTaskById(createdTask.getId());
        assertTrue(updatedTaskFromGetOptional.isPresent(), "Обновленная задача должна существовать");

        List<Task> history = taskManager.getHistory();

        assertFalse(history.isEmpty(), "История не должна быть пустой после get-запросов");

        assertEquals(1, history.size(), "История должна содержать только последний просмотр для данного id");

        Task onlyInHistory = history.get(0);
        assertEquals(createdTask.getId(), onlyInHistory.getId());
        assertEquals("Обновленная задача", onlyInHistory.getTitle());
        assertEquals("Обновленное описание", onlyInHistory.getDescription());
        assertEquals(Status.IN_PROGRESS, onlyInHistory.getStatus());

    }

    //проверка статуса эпика в зависимости от статуса подзадач
    @Test
    void testEpicStatusAllNew() {
        Epic epic = taskManager.createEpic(new Epic("Эпик", "Описание"));

        Subtask subtask1 = new Subtask("Подзадача 1", "Описание", Status.NEW, epic.getId());
        Subtask subtask2 = new Subtask("Подзадача 2", "Описание", Status.NEW, epic.getId());

        taskManager.createSubtask(subtask1);
        taskManager.createSubtask(subtask2);

        Optional<Epic> updatedEpic = taskManager.getEpicById(epic.getId());
        assertTrue(updatedEpic.isPresent());
        assertEquals(Status.NEW, updatedEpic.get().getStatus());
    }

    @Test
    void testEpicStatusAllDone() {
        Epic epic = taskManager.createEpic(new Epic("Эпик", "Описание"));

        Subtask subtask1 = new Subtask("Подзадача 1", "Описание", Status.DONE, epic.getId());
        Subtask subtask2 = new Subtask("Подзадача 2", "Описание", Status.DONE, epic.getId());

        taskManager.createSubtask(subtask1);
        taskManager.createSubtask(subtask2);

        Optional<Epic> updatedEpic = taskManager.getEpicById(epic.getId());
        assertTrue(updatedEpic.isPresent());
        assertEquals(Status.DONE, updatedEpic.get().getStatus());
    }

    @Test
    void testEpicStatusMixed() {
        Epic epic = taskManager.createEpic(new Epic("Эпик", "Описание"));

        Subtask subtask1 = new Subtask("Подзадача 1", "Описание", Status.NEW, epic.getId());
        Subtask subtask2 = new Subtask("Подзадача 2", "Описание", Status.DONE, epic.getId());

        taskManager.createSubtask(subtask1);
        taskManager.createSubtask(subtask2);

        Optional<Epic> updatedEpic = taskManager.getEpicById(epic.getId());
        assertTrue(updatedEpic.isPresent());
        assertEquals(Status.IN_PROGRESS, updatedEpic.get().getStatus());
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

        Optional<Subtask> subtaskOptional = taskManager.getSubtaskById(epicId);
        assertTrue(subtaskOptional.isEmpty(), "Подзадача с некорректной ссылкой не должна быть добавлена " +
                "в менеджер");


        Optional<Epic> retrievedEpic = taskManager.getEpicById(epicId);
        assertTrue(retrievedEpic.isPresent(), "Эпик должен существовать после неудачной попытки добавления " +
                "подзадачи");
        assertEquals(createdEpic.getTitle(), retrievedEpic.get().getTitle(), "Заголовок эпика должен " +
                "остаться неизменным");
        assertFalse(retrievedEpic.get().getSubtaskIds().contains(epicId),
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
        assertEquals(potentialId, selfReferencingSubtask.getEpicId(),
                "EpicId подзадачи должен совпадать с её ID");

        assertThrows(IllegalArgumentException.class, () -> taskManager.createSubtask(selfReferencingSubtask),
                "Ожидалось исключение IllegalArgumentException при попытке сделать подзадачу своим эпиком"
        );

        Optional<Subtask> subtaskOptional = taskManager.getSubtaskById(potentialId);
        assertTrue(subtaskOptional.isEmpty(),
                "Подзадача с некорректной ссылкой не должна быть добавлена в менеджер");
    }


}