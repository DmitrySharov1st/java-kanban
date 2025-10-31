package http;

import com.google.gson.Gson;
import model.Task;
import model.enums.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import service.InMemoryTaskManager;
import service.TaskManager;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDateTime;

public abstract class HttpTestBase {
    protected TaskManager manager;
    protected HttpTaskServer taskServer;
    protected Gson gson;
    protected HttpClient client;
    protected int testPort;
    protected String baseUrl;

    @BeforeEach
    void setUpBase() throws IOException {
        manager = new InMemoryTaskManager();
        taskServer = new HttpTaskServer(manager);
        gson = HttpTaskServer.getGson();
        client = HttpClient.newHttpClient();
        baseUrl = "http://localhost:8080";
        taskServer.start();
    }

    @AfterEach
    void tearDownBase() {
        if (taskServer != null) {
            taskServer.stop();
        }
    }

    protected Task createTestTask(String title, String description) {
        return new Task(title, description, Status.NEW);
    }

    protected Task createTestTaskWithTime(String title, String description,
                                          Duration duration, LocalDateTime startTime) {
        return new Task(title, description, Status.NEW, duration, startTime);
    }
}