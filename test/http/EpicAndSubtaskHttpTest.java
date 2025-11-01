package http;

import model.Epic;
import model.Subtask;
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

class EpicAndSubtaskHttpTest extends HttpTestBase {

    @Test
    void testCreateEpic() throws IOException, InterruptedException {
        Epic epic = new Epic("Тестовый эпик", "Описание тестового эпика");
        String epicJson = gson.toJson(epic);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/epics"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(epicJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
        List<Epic> epics = manager.getAllEpics();
        assertEquals(1, epics.size());
        assertEquals("Тестовый эпик", epics.get(0).getTitle());
        assertEquals(Status.NEW, epics.get(0).getStatus());
    }

    @Test
    void testCreateSubtask() throws IOException, InterruptedException {
        Epic epic = manager.createEpic(new Epic("Родительский эпик", "Описание родительского эпика"));
        Subtask subtask = new Subtask("Тестовая подзадача", "Описание тестовой подзадачи",
                Status.NEW, epic.getId());
        String subtaskJson = gson.toJson(subtask);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/subtasks"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(subtaskJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
        List<Subtask> subtasks = manager.getAllSubtasks();
        assertEquals(1, subtasks.size());
        assertEquals("Тестовая подзадача", subtasks.get(0).getTitle());
        assertEquals(epic.getId(), subtasks.get(0).getEpicId());
    }

    @Test
    void testGetEpicSubtasks() throws IOException, InterruptedException {
        // Given
        Epic epic = manager.createEpic(new Epic("Эпик", "Описание эпика"));
        Subtask subtask1 = manager.createSubtask(
                new Subtask("Подзадача 1", "Описание подзадачи 1", Status.NEW, epic.getId()));
        Subtask subtask2 = manager.createSubtask(
                new Subtask("Подзадача 2", "Описание подзадачи 2", Status.DONE, epic.getId()));

        // When
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/epics/" + epic.getId() + "/subtasks"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(200, response.statusCode());
        Subtask[] subtasks = gson.fromJson(response.body(), Subtask[].class);
        assertEquals(2, subtasks.length);

        // Проверяем, что статус эпика обновился
        Epic updatedEpic = manager.getEpicById(epic.getId()).orElseThrow();
        assertEquals(Status.IN_PROGRESS, updatedEpic.getStatus());
    }

    @Test
    void testEpicTimeCalculation() throws IOException, InterruptedException {
        Epic epic = manager.createEpic(new Epic("Эпик со временем", "Описание эпика"));

        LocalDateTime startTime1 = LocalDateTime.now().plusHours(1);
        LocalDateTime startTime2 = startTime1.plusHours(2);
        Duration duration1 = Duration.ofMinutes(30);
        Duration duration2 = Duration.ofMinutes(45);

        manager.createSubtask(new Subtask("Подзадача 1", "Описание подзадачи 1", Status.NEW,
                epic.getId(), duration1, startTime1));
        manager.createSubtask(new Subtask("Подзадача 2", "Описание подзадачи 2", Status.NEW,
                epic.getId(), duration2, startTime2));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/epics/" + epic.getId()))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        Epic responseEpic = gson.fromJson(response.body(), Epic.class);

        assertNotNull(responseEpic.getStartTime());
        assertNotNull(responseEpic.getDuration());
        assertNotNull(responseEpic.getEndTime());

        assertEquals(startTime1, responseEpic.getStartTime());
        assertEquals(Duration.ofMinutes(75), responseEpic.getDuration());
        assertEquals(startTime2.plus(duration2), responseEpic.getEndTime());
    }

    @Test
    void testUpdateSubtaskChangesEpicStatus() throws IOException, InterruptedException {
        Epic epic = manager.createEpic(new Epic("Эпик", "Описание эпика"));
        Subtask subtask = manager.createSubtask(
                new Subtask("Подзадача", "Описание подзадачи", Status.NEW, epic.getId()));

        //обновляем подзадачу на DONE
        subtask.setStatus(Status.DONE);
        String updatedSubtaskJson = gson.toJson(subtask);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/subtasks"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(updatedSubtaskJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
        Epic updatedEpic = manager.getEpicById(epic.getId()).orElseThrow();
        assertEquals(Status.DONE, updatedEpic.getStatus());
    }

    @Test
    void testDeleteEpicRemovesSubtasks() throws IOException, InterruptedException {
        Epic epic = manager.createEpic(new Epic("Эпик", "Описание эпика"));
        Subtask subtask = manager.createSubtask(
                new Subtask("Подзадача", "Описание подзадачи", Status.NEW, epic.getId()));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/epics/" + epic.getId()))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(manager.getAllEpics().isEmpty());
        assertTrue(manager.getAllSubtasks().isEmpty());
    }

    @Test
    void testCreateSubtaskWithNonExistentEpic() throws IOException, InterruptedException {
        Subtask subtask = new Subtask("Подзадача", "Описание подзадачи", Status.NEW, 999);
        String subtaskJson = gson.toJson(subtask);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/subtasks"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(subtaskJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        //должен вернуться 404, так как эпик не существует
        assertEquals(404, response.statusCode());
        assertTrue(manager.getAllSubtasks().isEmpty(),
                "Подзадача не должна быть создана при несуществующем эпике");
    }
}