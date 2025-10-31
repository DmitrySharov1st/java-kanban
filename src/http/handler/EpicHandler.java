package http.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import model.Epic;
import model.Subtask;
import service.TaskManager;

import java.io.IOException;
import java.util.List;

public class EpicHandler extends BaseHttpHandler {
    private final TaskManager taskManager;

    public EpicHandler(TaskManager taskManager, Gson gson) {
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
            // GET /epics
            List<Epic> epics = taskManager.getAllEpics();
            sendText(exchange, gson.toJson(epics));
        } else if (pathParts.length == 3) {
            // GET /epics/{id}
            int id = parsePathId(pathParts[2]);
            if (id == -1) {
                sendNotFound(exchange);
                return;
            }
            // Используем новый метод, который бросает исключение
            Epic epic = taskManager.getEpicOrThrow(id);
            sendText(exchange, gson.toJson(epic));

        } else if (pathParts.length == 4 && "subtasks".equals(pathParts[3])) {
            // GET /epics/{id}/subtasks
            int id = parsePathId(pathParts[2]);
            if (id == -1) {
                sendNotFound(exchange);
                return;
            }
            List<Subtask> subtasks = taskManager.getSubtasksByEpicId(id);
            sendText(exchange, gson.toJson(subtasks));
        } else {
            sendNotFound(exchange);
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        Epic epic = parseBody(body, Epic.class);

        if (epic.getId() == 0) {
            // Создание нового эпика
            taskManager.createEpic(epic);
            sendCreated(exchange);
        } else {
            // Обновление существующего эпика
            taskManager.updateEpic(epic);
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
            taskManager.deleteEpicById(id);
            sendSuccess(exchange);
        } else {
            sendNotFound(exchange);
        }
    }
}