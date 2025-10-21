package service;

import model.Task;
import model.enums.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class inMemoryHistoryManagerTest {
    private HistoryManager historyManager;

    @BeforeEach
    void setUp() {

        historyManager = Managers.getDefaultHistory();
    }

    //проверка, что запись истории более не ограничена 10-ю задачами и что дубликаты при просмотре не появляются
    @Test
    void testHistoryManagerStoresAllRecordsAndRemovesDuplicates() {
        // добавляем 12 уникальных задач
        for (int taskId = 1; taskId <= 12; taskId++) {
            Task task = new Task(
                    String.format("Задача %d", taskId),
                    String.format("Описание %d", taskId),
                    Status.NEW
            );
            task.setId(taskId);
            historyManager.add(task);
        }

        List<Task> history = historyManager.getHistory();
        assertEquals(12, history.size(), "История должна содержать все 12 записей (без лимита)");

        Task duplicate = new Task("Задача 5 (новая)", "Обновленное описание", Status.NEW);
        duplicate.setId(5);
        historyManager.add(duplicate);

        List<Task> historyAfterDup = historyManager.getHistory();
        assertEquals(12, historyAfterDup.size(),
                "После повторного просмотра не должно появляться дубликатов, а только перемещение");

        assertEquals(5, historyAfterDup.get(historyAfterDup.size() - 1).getId());
        // убедимся, что id=5 встречается ровно 1 раз

        int count = 0;
        for(Task listElement : historyAfterDup) {
            if(listElement.getId() == 5) {
                count++;
            }
        }
        assertEquals(1, count, "После повторного просмотра не должно появляться дубликатов");
    }

    // Убедитесь, что утилитарный класс всегда возвращает проинициализированные и готовые к работе экземпляры менеджеров
    @Test
    void testManagersReturnsInitializedInstances() {
        assertNotNull(historyManager, "HistoryManager не должен быть null");
        assertTrue(historyManager.getHistory().isEmpty(), "Новый HistoryManager должен иметь пустую историю");
    }

    //проверка добавления дубликата в историю просмотров
    @Test
    void testDuplicateTaskInHistory() {
        Task task = new Task("Задача", "Описание", Status.NEW);
        task.setId(1);

        historyManager.add(task);
        historyManager.add(task); // Дубликат

        List<Task> history = historyManager.getHistory();
        assertEquals(1, history.size()); // Дубликаты не должны добавляться
    }

    // Проверка пустой истории задач
    @Test
    void testEmptyHistory() {
        List<Task> history = historyManager.getHistory();
        assertTrue(history.isEmpty(), "Новая история должна быть пустой");
        assertEquals(0, history.size(), "Размер пустой истории должен быть 0");
    }

    // Проверка удаления из начала истории
    @Test
    void testRemoveFromBeginning() {
        // Создаем и добавляем три задачи
        Task task1 = createTestTask(1, "Задача 1");
        Task task2 = createTestTask(2, "Задача 2");
        Task task3 = createTestTask(3, "Задача 3");

        historyManager.add(task1);
        historyManager.add(task2);
        historyManager.add(task3);

        // Проверяем начальное состояние
        List<Task> history = historyManager.getHistory();
        assertEquals(3, history.size(), "История должна содержать 3 задачи");
        assertEquals(task1, history.get(0), "Первая задача должна быть task1");
        assertEquals(task2, history.get(1), "Вторая задача должна быть task2");
        assertEquals(task3, history.get(2), "Третья задача должна быть task3");

        // Удаляем из начала
        historyManager.remove(1);

        // Проверяем состояние после удаления
        history = historyManager.getHistory();
        assertEquals(2, history.size(), "После удаления история должна содержать 2 задачи");
        assertEquals(task2, history.get(0), "После удаления первой задачи, первой должна стать task2");
        assertEquals(task3, history.get(1), "После удаления первой задачи, второй должна остаться task3");
    }

    // Проверка удаления из середины истории
    @Test
    void testRemoveFromMiddle() {
        // Создаем и добавляем три задачи
        Task task1 = createTestTask(1, "Задача 1");
        Task task2 = createTestTask(2, "Задача 2");
        Task task3 = createTestTask(3, "Задача 3");

        historyManager.add(task1);
        historyManager.add(task2);
        historyManager.add(task3);

        // Проверяем начальное состояние
        List<Task> history = historyManager.getHistory();
        assertEquals(3, history.size(), "История должна содержать 3 задачи");

        // Удаляем из середины
        historyManager.remove(2);

        // Проверяем состояние после удаления
        history = historyManager.getHistory();
        assertEquals(2, history.size(), "После удаления история должна содержать 2 задачи");
        assertEquals(task1, history.get(0), "После удаления из середины, первая задача должна остаться task1");
        assertEquals(task3, history.get(1), "После удаления из середины, второй должна стать task3");
    }

    // Проверка удаления из конца истории
    @Test
    void testRemoveFromEnd() {
        // Создаем и добавляем три задачи
        Task task1 = createTestTask(1, "Задача 1");
        Task task2 = createTestTask(2, "Задача 2");
        Task task3 = createTestTask(3, "Задача 3");

        historyManager.add(task1);
        historyManager.add(task2);
        historyManager.add(task3);

        // Проверяем начальное состояние
        List<Task> history = historyManager.getHistory();
        assertEquals(3, history.size(), "История должна содержать 3 задачи");

        // Удаляем из конца
        historyManager.remove(3);

        // Проверяем состояние после удаления
        history = historyManager.getHistory();
        assertEquals(2, history.size(), "После удаления история должна содержать 2 задачи");
        assertEquals(task1, history.get(0), "После удаления из конца, первая задача должна остаться task1");
        assertEquals(task2, history.get(1), "После удаления из конца, второй должна остаться task2");
    }

    // Проверка удаления единственной задачи
    @Test
    void testRemoveSingleTask() {
        Task task = createTestTask(1, "Единственная задача");
        historyManager.add(task);

        // Проверяем, что задача добавлена
        List<Task> history = historyManager.getHistory();
        assertEquals(1, history.size(), "История должна содержать 1 задачу");

        // Удаляем задачу
        historyManager.remove(1);

        // Проверяем, что история пуста
        history = historyManager.getHistory();
        assertTrue(history.isEmpty(), "После удаления единственной задачи история должна быть пустой");
    }

    // Проверка удаления несуществующей задачи
    @Test
    void testRemoveNonExistentTask() {
        Task task = createTestTask(1, "Задача");
        historyManager.add(task);

        // Удаляем несуществующую задачу
        assertDoesNotThrow(() -> historyManager.remove(999),
                "Удаление несуществующей задачи не должно вызывать исключений");

        // Проверяем, что исходная задача осталась
        List<Task> history = historyManager.getHistory();
        assertEquals(1, history.size(),
                "После попытки удаления несуществующей задачи история должна содержать 1 задачу");
        assertEquals(task, history.get(0), "Исходная задача должна остаться в истории");
    }

    // Проверка последовательного удаления из разных позиций
    @Test
    void testSequentialRemoval() {
        // Создаем и добавляем пять задач
        Task task1 = createTestTask(1, "Задача 1");
        Task task2 = createTestTask(2, "Задача 2");
        Task task3 = createTestTask(3, "Задача 3");
        Task task4 = createTestTask(4, "Задача 4");
        Task task5 = createTestTask(5, "Задача 5");

        historyManager.add(task1);
        historyManager.add(task2);
        historyManager.add(task3);
        historyManager.add(task4);
        historyManager.add(task5);

        // Проверяем начальное состояние
        assertEquals(5, historyManager.getHistory().size(), "История должна содержать 5 задач");

        // Удаляем из середины
        historyManager.remove(3);
        List<Task> history = historyManager.getHistory();
        assertEquals(4, history.size(), "После первого удаления история должна содержать 4 задачи");
        assertEquals(task4, history.get(2), "После удаления task3, task4 должен занять его позицию");

        // Удаляем из конца
        historyManager.remove(5);
        history = historyManager.getHistory();
        assertEquals(3, history.size(), "После второго удаления история должна содержать 3 задачи");
        assertEquals(task4, history.get(2), "После удаления task5, task4 должен стать последним");

        // Удаляем из начала
        historyManager.remove(1);
        history = historyManager.getHistory();
        assertEquals(2, history.size(), "После третьего удаления история должна содержать 2 задачи");
        assertEquals(task2, history.get(0), "После удаления task1, task2 должен стать первым");
        assertEquals(task4, history.get(1), "task4 должен остаться последним");
    }

    // Вспомогательный метод для создания тестовых задач
    private Task createTestTask(int id, String title) {
        Task task = new Task(title, "Описание " + id, Status.NEW);
        task.setId(id);
        return task;
    }

}
