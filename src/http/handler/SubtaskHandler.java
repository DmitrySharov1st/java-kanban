package http.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import model.Subtask;
import service.TaskManager;

import java.io.IOException;
import java.util.List;

public class SubtaskHandler extends BaseHttpHandler {
    private final TaskManager taskManager;

    public SubtaskHandler(TaskManager taskManager, Gson gson) {
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
            // GET /subtasks
            List<Subtask> subtasks = taskManager.getAllSubtasks();
            sendText(exchange, gson.toJson(subtasks));
        } else if (pathParts.length == 3) {
            // GET /subtasks/{id}
            int id = parsePathId(pathParts[2]);
            if (id == -1) {
                sendNotFound(exchange);
                return;
            }

            // Используем новый метод, который бросает исключение
            Subtask subtask = taskManager.getSubtaskOrThrow(id);
            sendText(exchange, gson.toJson(subtask));
        } else {
            sendNotFound(exchange);
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        Subtask subtask = parseBody(body, Subtask.class);

        if (subtask.getId() == 0) {
            // Создание новой подзадачи
            taskManager.createSubtask(subtask);
            sendCreated(exchange);
        } else {
            // Обновление существующей подзадачи
            taskManager.updateSubtask(subtask);
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
            taskManager.deleteSubtaskById(id);
            sendSuccess(exchange);
        } else {
            sendNotFound(exchange);
        }
    }
}