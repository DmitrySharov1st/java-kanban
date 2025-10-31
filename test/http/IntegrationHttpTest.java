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

import static org.junit.jupiter.api.Assertions.*;

class IntegrationHttpTest extends HttpTestBase {

    @Test
    void testCompleteWorkflow() throws IOException, InterruptedException {
        //Создаем эпик через API
        Epic epic = new Epic("Эпик", "Описание эпика");
        String epicJson = gson.toJson(epic);

        HttpRequest createEpicRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/epics"))
                .POST(HttpRequest.BodyPublishers.ofString(epicJson))
                .build();

        HttpResponse<String> epicResponse = client.send(createEpicRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, epicResponse.statusCode()); // ИСПРАВЛЕНО: responseCode() → statusCode()

        // Получаем ID созданного эпика
        Epic createdEpic = manager.getAllEpics().get(0);

        //Создаем подзадачи через API
        Subtask subtask1 = new Subtask("Подзадача 1", "Описание подзадачи 1",
                Status.NEW, createdEpic.getId());
        Subtask subtask2 = new Subtask("Подзадача 2", "Описание подзадачи 2",
                Status.NEW, createdEpic.getId());

        String subtask1Json = gson.toJson(subtask1);
        String subtask2Json = gson.toJson(subtask2);

        HttpResponse<String> subtask1Response = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/subtasks"))
                .POST(HttpRequest.BodyPublishers.ofString(subtask1Json))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(201, subtask1Response.statusCode());

        HttpResponse<String> subtask2Response = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/subtasks"))
                .POST(HttpRequest.BodyPublishers.ofString(subtask2Json))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(201, subtask2Response.statusCode());

        //Создаем отдельную задачу через API
        Task standaloneTask = createTestTask("Одиночная задача", "Описание задачи");
        String taskJson = gson.toJson(standaloneTask);

        HttpResponse<String> taskResponse = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/tasks"))
                .POST(HttpRequest.BodyPublishers.ofString(taskJson))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(201, taskResponse.statusCode());

        //Проверяем что все создалось
        HttpRequest getTasksRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/tasks"))
                .GET()
                .build();

        HttpResponse<String> tasksResponse = client.send(getTasksRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, tasksResponse.statusCode());
        Task[] tasks = gson.fromJson(tasksResponse.body(), Task[].class);
        assertEquals(1, tasks.length);

        HttpRequest getSubtasksRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/subtasks"))
                .GET()
                .build();

        HttpResponse<String> subtasksResponse = client.send(getSubtasksRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, subtasksResponse.statusCode());
        Subtask[] subtasks = gson.fromJson(subtasksResponse.body(), Subtask[].class);
        assertEquals(2, subtasks.length);

        //Обновляем статус подзадачи
        Subtask firstSubtask = subtasks[0];
        firstSubtask.setStatus(Status.DONE);
        String updatedSubtaskJson = gson.toJson(firstSubtask);

        HttpResponse<String> updateSubtaskResponse = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/subtasks"))
                .POST(HttpRequest.BodyPublishers.ofString(updatedSubtaskJson))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(201, updateSubtaskResponse.statusCode());

        //Проверяем что статус эпика обновился
        HttpRequest getEpicRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/epics/" + createdEpic.getId()))
                .GET()
                .build();

        HttpResponse<String> epicGetResponse = client.send(getEpicRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, epicGetResponse.statusCode());
        Epic updatedEpic = gson.fromJson(epicGetResponse.body(), Epic.class);
        assertEquals(Status.IN_PROGRESS, updatedEpic.getStatus());

        //Проверяем историю
        //Сначала получаем несколько задач, чтобы добавить в историю
        HttpResponse<String> getTaskHistoryResponse = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/tasks/" + tasks[0].getId()))
                .GET()
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, getTaskHistoryResponse.statusCode());

        HttpResponse<String> getEpicHistoryResponse = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/epics/" + createdEpic.getId()))
                .GET()
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, getEpicHistoryResponse.statusCode());

        HttpRequest historyRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/history"))
                .GET()
                .build();

        HttpResponse<String> historyResponse = client.send(historyRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, historyResponse.statusCode());
        Task[] history = gson.fromJson(historyResponse.body(), Task[].class);
        assertTrue(history.length >= 2);
    }

    @Test
    void testConcurrentOperations() throws IOException, InterruptedException {
        //создаем несколько задач параллельно
        int numTasks = 5;

        //отправляем запросы "параллельно"
        for (int i = 0; i < numTasks; i++) {
            Task task = createTestTask("Задача " + i, "Описание задачи " + i);
            String taskJson = gson.toJson(task);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/tasks"))
                    .POST(HttpRequest.BodyPublishers.ofString(taskJson))
                    .build();

            // Отправляем синхронно, но сервер должен обрабатывать конкурентно
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(201, response.statusCode());
        }

        //проверяем что все задачи создались
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/tasks"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(getRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        Task[] tasks = gson.fromJson(response.body(), Task[].class);
        assertEquals(numTasks, tasks.length);
    }
}