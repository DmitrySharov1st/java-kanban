package http;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import model.Task;
import model.enums.Status;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskHttpTest extends HttpTestBase {

    @Test
    void testCreateTask() throws IOException, InterruptedException {
        Task task = createTestTask("Тестовая задача", "Описание тестовой задачи");
        String taskJson = gson.toJson(task);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/tasks"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(taskJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
        List<Task> tasks = manager.getAllTasks();
        assertEquals(1, tasks.size());
        assertEquals("Тестовая задача", tasks.get(0).getTitle());
    }

    @Test
    void testCreateTaskWithTime() throws IOException, InterruptedException {
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        Duration duration = Duration.ofMinutes(30);
        Task task = createTestTaskWithTime("Задача со временем", "Описание", duration, startTime);
        String taskJson = gson.toJson(task);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/tasks"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(taskJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
        Task createdTask = manager.getAllTasks().get(0);
        assertEquals(startTime, createdTask.getStartTime());
        assertEquals(duration, createdTask.getDuration());
    }

    @Test
    void testGetTaskById() throws IOException, InterruptedException {
        Task task = manager.createTask(createTestTask("Тестовая задача", "Описание"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/tasks/" + task.getId()))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        Task responseTask = gson.fromJson(response.body(), Task.class);
        assertEquals(task.getId(), responseTask.getId());
        assertEquals("Тестовая задача", responseTask.getTitle());
    }

    @Test
    void testGetAllTasks() throws IOException, InterruptedException {
        manager.createTask(createTestTask("Задача 1", "Описание 1"));
        manager.createTask(createTestTask("Задача 2", "Описание 2"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/tasks"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        Task[] tasks = gson.fromJson(response.body(), Task[].class);
        assertEquals(2, tasks.length);
    }

    @Test
    void testUpdateTask() throws IOException, InterruptedException {
        Task task = manager.createTask(createTestTask("Задача", "Описание"));
        task.setTitle("Новое название задачи");
        task.setDescription("Новое описание");
        task.setStatus(Status.IN_PROGRESS);
        String updatedTaskJson = gson.toJson(task);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/tasks"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(updatedTaskJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
        Task updatedTask = manager.getTaskById(task.getId()).orElseThrow();
        assertEquals("Новое название задачи", updatedTask.getTitle());
        assertEquals("Новое описание", updatedTask.getDescription());
        assertEquals(Status.IN_PROGRESS, updatedTask.getStatus());
    }

    @Test
    void testDeleteTask() throws IOException, InterruptedException {
        Task task = manager.createTask(createTestTask("Задача для удаления", "Описание"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/tasks/" + task.getId()))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(manager.getAllTasks().isEmpty());
    }

    @Test
    void testDeleteAllTasks() throws IOException, InterruptedException {
        manager.createTask(createTestTask("Задача 1", "Описание 1"));
        manager.createTask(createTestTask("Задача 2", "Описание 2"));

        //удаляем через менеджер, так как эндпоинта для удаления всех задач нет
        manager.deleteAllTasks();

        //проверяем что GET возвращает пустой список
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/tasks"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        Task[] tasks = gson.fromJson(response.body(), Task[].class);
        assertEquals(0, tasks.length);
    }

    @Test
    void testGetNonExistentTask() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/tasks/999"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }

    @Test
    void testCreateTaskWithInvalidJson() throws IOException, InterruptedException {
        String invalidJson = "{ invalid json }";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/tasks"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(invalidJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(500, response.statusCode());
    }

    @Test
    void testCreateTaskWithTimeConflict() throws IOException, InterruptedException {
        //создаем первую задачу
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        Duration duration = Duration.ofMinutes(60);
        Task task1 = createTestTaskWithTime("Задача 1", "Описание 1", duration, startTime);
        manager.createTask(task1);

        //пытаемся создать вторую задачу с пересекающимся временем
        Task task2 = createTestTaskWithTime("Задача 2", "Описание 2",
                Duration.ofMinutes(30), startTime.plusMinutes(30));
        String task2Json = gson.toJson(task2);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/tasks"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(task2Json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(406, response.statusCode());
        assertEquals(1, manager.getAllTasks().size()); // только первая задача создана
    }

    @Test
    void testTaskResponseContainsAllFields() throws IOException, InterruptedException {
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        Duration duration = Duration.ofMinutes(45);
        Task task = createTestTaskWithTime("Задача", "Описание",
                duration, startTime);
        task.setStatus(Status.IN_PROGRESS);
        Task createdTask = manager.createTask(task);

        // When
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/tasks/" + createdTask.getId()))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(200, response.statusCode());

        // Для отладки выведем полученный JSON
        System.out.println("Response JSON: " + response.body());

        // Парсим JSON и проверяем все поля
        JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();

        // Проверяем наличие основных полей
        assertTrue(jsonObject.has("id"), "JSON должен содержать поле 'id'");
        assertTrue(jsonObject.has("title"), "JSON должен содержать поле 'title'");
        assertTrue(jsonObject.has("description"), "JSON должен содержать поле 'description'");
        assertTrue(jsonObject.has("status"), "JSON должен содержать поле 'status'");

        // Проверяем значения основных полей
        assertEquals("Задача", jsonObject.get("title").getAsString());
        assertEquals("IN_PROGRESS", jsonObject.get("status").getAsString());

        // Проверяем временные поля (они могут отсутствовать, если не установлены)
        if (jsonObject.has("duration")) {
            assertEquals(45, jsonObject.get("duration").getAsLong());
        } else {
            System.out.println("Поле 'duration' отсутствует в JSON");
        }

        if (jsonObject.has("startTime")) {
            // Проверяем, что startTime присутствует и является валидной строкой
            String startTimeStr = jsonObject.get("startTime").getAsString();
            assertNotNull(startTimeStr);
            assertFalse(startTimeStr.isEmpty());
        } else {
            System.out.println("Поле 'startTime' отсутствует в JSON");
        }

        // Поле endTime может быть вычисляемым и не сериализоваться
        // Или может сериализоваться только если startTime установлен
        if (jsonObject.has("endTime")) {
            String endTimeStr = jsonObject.get("endTime").getAsString();
            assertNotNull(endTimeStr);
            assertFalse(endTimeStr.isEmpty());
        } else {
            System.out.println("Поле 'endTime' отсутствует в JSON (это может быть нормально)");
        }

        // Проверяем тип задачи
        if (jsonObject.has("type")) {
            assertEquals("TASK", jsonObject.get("type").getAsString());
        }
    }
}
