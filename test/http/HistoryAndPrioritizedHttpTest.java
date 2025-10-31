package http;

import model.Epic;
import model.Subtask;
import model.Task;
import model.enums.Status;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class HistoryAndPrioritizedHttpTest extends HttpTestBase {

    @Test
    void testGetHistory() throws IOException, InterruptedException {
        //создаем задачи и эпик
        Task task1 = manager.createTask(createTestTask("Задача 1", "Описание задачи 1"));
        Task task2 = manager.createTask(createTestTask("Задача 2", "Описание задачи 2"));
        Epic epic = manager.createEpic(new Epic("Эпик", "Описание эпика"));

        // Добавляем в историю через HTTP запросы (это добавит их в историю менеджера)
        HttpRequest getTask1Request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/tasks/" + task1.getId()))
                .GET()
                .build();
        client.send(getTask1Request, HttpResponse.BodyHandlers.ofString());

        HttpRequest getTask2Request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/tasks/" + task2.getId()))
                .GET()
                .build();
        client.send(getTask2Request, HttpResponse.BodyHandlers.ofString());

        HttpRequest getEpicRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/epics/" + epic.getId()))
                .GET()
                .build();
        client.send(getEpicRequest, HttpResponse.BodyHandlers.ofString());

        //получаем историю через API
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/history"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        Task[] history = gson.fromJson(response.body(), Task[].class);

        // Проверяем количество элементов в истории
        // История должна содержать 3 элемента
        assertTrue(history.length >= 3, "История должна содержать как минимум 3 элемента");

        // Проверяем, что все три задачи присутствуют в истории
        List<Integer> historyIds = Arrays.stream(history)
                .map(Task::getId)
                .collect(Collectors.toList());

        assertTrue(historyIds.contains(task1.getId()), "История должна содержать задачу 1");
        assertTrue(historyIds.contains(task2.getId()), "История должна содержать задачу 2");
        assertTrue(historyIds.contains(epic.getId()), "История должна содержать эпик");
    }

    @Test
    void testGetEmptyHistory() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/history"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        Task[] history = gson.fromJson(response.body(), Task[].class);
        assertEquals(0, history.length, "История должна быть пустой");
    }

    @Test
    void testGetPrioritizedTasks() throws IOException, InterruptedException {
        LocalDateTime earlyTime = LocalDateTime.now().plusHours(1);
        LocalDateTime lateTime = LocalDateTime.now().plusHours(3);

        Task task2 = manager.createTask(createTestTaskWithTime("Задача 2", "Описание задачи 2",
                Duration.ofMinutes(30), lateTime));
        Task task1 = manager.createTask(createTestTaskWithTime("Задача 1", "Описание задачи 1",
                Duration.ofMinutes(45), earlyTime));

        Epic epic = manager.createEpic(new Epic("Эпик", "Описание эпика"));
        Subtask subtask = manager.createSubtask(new Subtask("Подзадача", "Описание подзадачи",
                Status.NEW, epic.getId(), Duration.ofMinutes(20), earlyTime.plusHours(1)));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/prioritized"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        Task[] prioritized = gson.fromJson(response.body(), Task[].class);

        // Проверяем количество (должны быть 3 задачи с временем)
        assertEquals(3, prioritized.length, "Должно быть 3 приоритетных задачи");

        // Проверяем, что все задачи с временем присутствуют
        List<Integer> prioritizedIds = Arrays.stream(prioritized)
                .map(Task::getId)
                .collect(Collectors.toList());

        assertTrue(prioritizedIds.contains(task1.getId()));
        assertTrue(prioritizedIds.contains(task2.getId()));
        assertTrue(prioritizedIds.contains(subtask.getId()));

        // Проверяем порядок (по startTime) - Задача 1 должна быть первой (самая ранняя)
        assertEquals(task1.getId(), prioritized[0].getId(), "Задача 1 должна быть первой (самая ранняя)");
    }

    @Test
    void testGetPrioritizedTasksExcludesTasksWithoutTime() throws IOException, InterruptedException {
        Task taskWithTime = manager.createTask(createTestTaskWithTime("Задача со временем", "Описание",
                Duration.ofMinutes(30), LocalDateTime.now().plusHours(1)));
        Task taskWithoutTime = manager.createTask(createTestTask("Задача без времени", "Описание"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/prioritized"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        Task[] prioritized = gson.fromJson(response.body(), Task[].class);
        assertEquals(1, prioritized.length, "Должна быть только одна задача с временем");
        assertEquals(taskWithTime.getId(), prioritized[0].getId(), "Должна быть задача с временем");
    }

    @Test
    void testHistoryUpdatesAfterTaskOperations() throws IOException, InterruptedException {
        Task task = manager.createTask(createTestTask("Задача", "Описание задачи"));

        //получаем задачу через API (должна добавиться в историю)
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/tasks/" + task.getId()))
                .GET()
                .build();
        HttpResponse<String> getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, getResponse.statusCode());

        // Then - проверяем историю
        HttpRequest historyRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/history"))
                .GET()
                .build();

        HttpResponse<String> historyResponse = client.send(historyRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, historyResponse.statusCode());

        Task[] history = gson.fromJson(historyResponse.body(), Task[].class);

        assertEquals(1, history.length, "История должна содержать 1 задачу");
        assertEquals(task.getId(), history[0].getId(), "История должна содержать просмотренную задачу");
    }
}