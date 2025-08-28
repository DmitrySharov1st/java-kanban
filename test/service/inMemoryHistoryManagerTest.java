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

public class inMemoryHistoryManagerTest {
    private HistoryManager historyManager;

    @BeforeEach
    void setUp() {

        historyManager = Managers.getDefaultHistory();
    }

    //проверка, что в историю можно записать не более 10 записей
    @Test
    void testHistoryManagerStoresNoMoreThan10Records() {
        for (int i = 1; i <= 12; i++) {
            Task task = new Task("Задача " + i, "Описание " + i, Status.NEW);
            task.setId(i); // Устанавливаем ID вручную для теста
            historyManager.add(task);
        }

        List<Task> history = historyManager.getHistory();

        assertEquals(10, history.size(), "История должна содержать не более 10 записей");

        for (int i = 0; i < 10; i++) {
            Task taskInHistory = history.get(i);
            int expectedId = i + 3;
            assertEquals(expectedId, taskInHistory.getId(), "Неверный ID задачи в истории на позиции " + i);
            assertEquals("Задача " + expectedId, taskInHistory.getTitle(), "Неверный заголовок задачи в истории на позиции " + i);
        }
    }
}
