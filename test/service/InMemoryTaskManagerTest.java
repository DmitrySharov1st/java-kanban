package service;

import model.Epic;
import model.Subtask;
import model.Task;
import model.enums.Status;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryTaskManagerTest extends TaskManagerTest<InMemoryTaskManager> {

    @Override
    protected InMemoryTaskManager createTaskManager() {
       return new InMemoryTaskManager(Managers.getDefaultHistory());
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
        assertEquals(subtask1.hashCode(), subtask2.hashCode(),
                "Хэш-коды подзадач с одинаковым ID должны совпадать");
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

    // Убедитесь, что утилитарный класс всегда возвращает проинициализированные и готовые к работе экземпляры менеджеров
    @Test
    void testManagersReturnsInitializedInstances() {

        assertNotNull(taskManager, "TaskManager не должен быть null");

        assertTrue(taskManager.getAllTasks().isEmpty(), "Новый TaskManager должен иметь пустой список задач");
        assertTrue(taskManager.getAllEpics().isEmpty(), "Новый TaskManager должен иметь пустой список эпиков");
        assertTrue(taskManager.getAllSubtasks().isEmpty(),
                "Новый TaskManager должен иметь пустой список подзадач");
        assertTrue(taskManager.getHistory().isEmpty(), "Новый TaskManager должен иметь пустую историю");
    }
}
