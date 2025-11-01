package http.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import model.Task;
import service.TaskManager;

import java.io.IOException;
import java.util.List;

public class TaskHandler extends BaseHttpHandler {
    private final TaskManager taskManager;

    public TaskHandler(TaskManager taskManager, Gson gson) {
        super(gson);
        this.taskManager = taskManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            String[] pathParts = path.split("/");

            switch (method) {
                case "GET":
                    handleGet(exchange, pathParts);
                    break;
                case "POST":
                    handlePost(exchange);
                    break;
                case "DELETE":
                    handleDelete(exchange, pathParts);
                    break;
                default:
                    sendNotFound(exchange);
            }
        } catch (Exception e) {
            handleExceptions(exchange, e);
        }
    }

    private void handleGet(HttpExchange exchange, String[] pathParts) throws IOException {
        if (pathParts.length == 2) {
            // GET /tasks
            List<Task> tasks = taskManager.getAllTasks();
            sendText(exchange, gson.toJson(tasks));
        } else if (pathParts.length == 3) {
            // GET /tasks/{id}
            int id = parsePathId(pathParts[2]);
            if (id == -1) {
                sendNotFound(exchange);
                return;
            }

            // Используем новый метод, который бросает исключение
            Task task = taskManager.getTaskOrThrow(id);
            sendText(exchange, gson.toJson(task));
        } else {
            sendNotFound(exchange);
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        Task task = parseBody(body, Task.class);

        if (task.getId() == 0) {
            // Создание новой задачи
            Task createdTask = taskManager.createTask(task);
            sendCreated(exchange);
        } else {
            // Обновление существующей задачи
            taskManager.updateTask(task);
            sendCreated(exchange);
        }
    }

    private void handleDelete(HttpExchange exchange, String[] pathParts) throws IOException {
        if (pathParts.length == 3) {
            int id = parsePathId(pathParts[2]);
            if (id == -1) {
                sendNotFound(exchange);
                return;
            }
            taskManager.deleteTaskById(id);
            sendSuccess(exchange);
        } else {
            sendNotFound(exchange);
        }
    }
}