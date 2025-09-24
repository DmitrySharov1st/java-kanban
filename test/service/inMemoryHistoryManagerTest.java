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
}
