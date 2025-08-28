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

        assertEquals(10, history.size(), "История должна содержать не более 10 записей");

        for (int historyIndex = 0; historyIndex < 10; historyIndex++) {
            Task taskInHistory = history.get(historyIndex);
            int expectedId = historyIndex + 3;
            assertEquals(
                    expectedId,
                    taskInHistory.getId(),
                    String.format("Неверный ID задачи в истории на позиции %d", historyIndex)
            );
            assertEquals(
                    String.format("Задача %d", expectedId),
                    taskInHistory.getTitle(),
                    String.format("Неверный заголовок задачи в истории на позиции %d", historyIndex)
            );
        }
    }
}
