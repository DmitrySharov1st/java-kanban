package service;

import model.*;
import model.enums.Status;
import model.enums.TaskType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class FileBackedTaskManager extends InMemoryTaskManager {
    private final File file;

    public FileBackedTaskManager(File file) {
        this.file = file;
    }

    @Override
    protected void updateEpicStatus(Epic epic) {
        super.updateEpicStatus(epic);
    }

    // Метод сохранения в файл
     void save() {
        try {
            List<String> lines = new ArrayList<>();
            lines.add("id,type,name,status,description,epic");

            for (Task task : getAllTasks()) {
                lines.add(toString(task));
            }
            for (Epic epic : getAllEpics()) {
                lines.add(toString(epic));
            }
            for (Subtask subtask : getAllSubtasks()) {
                lines.add(toString(subtask));
            }

            Files.write(file.toPath(), lines);
        } catch (IOException e) {
            throw new ManagerSaveException("Ошибка сохранения в файл", e);
        }
    }

    // Преобразование задачи в строку CSV
    private String toString(Task task) {
        String[] fields = new String[6];
        fields[0] = String.valueOf(task.getId());
        fields[1] = task.getType().toString();
        fields[2] = task.getTitle();
        fields[3] = task.getStatus().toString();
        fields[4] = task.getDescription();

        if (task.getType() == TaskType.SUBTASK) {
            fields[5] = String.valueOf(((Subtask) task).getEpicId());
        } else {
            fields[5] = "";
        }

        return String.join(",", fields);
    }

    // Восстановление задачи из строки
    private static Task fromString(String value) {
        String[] fields = value.split(",");
        int id = Integer.parseInt(fields[0]);
        TaskType type = TaskType.valueOf(fields[1]);
        String name = fields[2];
        Status status = Status.valueOf(fields[3]);
        String description = fields[4];
        String epicIdStr = fields.length > 5 ? fields[5] : "";

        switch (type) {
            case TASK:
                return new Task(name, description, id, status);
            case EPIC:
                Epic epic = new Epic(name, description, id, status);
                return epic;
            case SUBTASK:
                int epicId = Integer.parseInt(epicIdStr);
                return new Subtask(name, description, id, status, epicId);
            default:
                throw new IllegalArgumentException("Неизвестный тип задачи: " + type);
        }
    }

    public static FileBackedTaskManager loadFromFile(File file) {
        FileBackedTaskManager manager = new FileBackedTaskManager(file);

        try {
            String content = Files.readString(file.toPath());
            String[] lines = content.split("\n");

            // Пропускаем заголовок
            int maxId = 0;
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;

                Task task = fromString(line);
                int id = task.getId();

                if (id > maxId) maxId = id;

                if (task instanceof Epic) {
                    manager.epics.put(id, (Epic) task);
                } else if (task instanceof Subtask) {
                    manager.subtasks.put(id, (Subtask) task);
                } else {
                    manager.tasks.put(id, task);
                }
            }

            manager.nextId = maxId + 1;

            // Восстанавливаем связи Epic-Subtask
            for (Subtask subtask : manager.subtasks.values()) {
                Epic epic = manager.epics.get(subtask.getEpicId());
                if (epic != null) {
                    epic.addSubtaskId(subtask.getId());
                }
            }

            // Обновляем статусы эпиков
            for (Epic epic : manager.epics.values()) {
                manager.updateEpicStatus(epic);
            }

        } catch (IOException e) {
            throw new ManagerSaveException("Ошибка загрузки из файла", e);
        }

        return manager;
    }

    @Override
    public Task createTask(Task task) {
        Task createdTask = super.createTask(task);
        save();
        return createdTask;
    }

    @Override
    public void updateTask(Task task) {
        super.updateTask(task);
        save();
    }

    @Override
    public void deleteTaskById(int id) {
        super.deleteTaskById(id);
        save();
    }

    @Override
    public void deleteAllTasks() {
        super.deleteAllTasks();
        save();
    }

    @Override
    public Epic createEpic(Epic epic) {
        Epic createdEpic = super.createEpic(epic);
        save();
        return createdEpic;
    }

    @Override
    public void updateEpic(Epic epic) {
        super.updateEpic(epic);
        save();
    }

    @Override
    public void deleteEpicById(int id) {
        super.deleteEpicById(id);
        save();
    }

    @Override
    public void deleteAllEpics() {
        super.deleteAllEpics();
        save();
    }

    @Override
    public Subtask createSubtask(Subtask subtask) {
        Subtask createdSubtask = super.createSubtask(subtask);
        save();
        return createdSubtask;
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        super.updateSubtask(subtask);
        save();
    }

    @Override
    public void deleteSubtaskById(int id) {
        super.deleteSubtaskById(id);
        save();
    }

    @Override
    public void deleteAllSubtasks() {
        super.deleteAllSubtasks();
        save();
    }

    public static void main(String[] args) {
        // Создаем временный файл для тестирования
        File file = new File("tasks.csv");

        // Создаем первый менеджер и добавляем задачи
        FileBackedTaskManager manager1 = new FileBackedTaskManager(file);

        Task task1 = new Task("Задача 1", "Описание задачи 1", Status.NEW);
        manager1.createTask(task1);

        Epic epic1 = new Epic("Эпик 1", "Описание эпика 1");
        manager1.createEpic(epic1);

        Subtask subtask1 = new Subtask("Подзадача 1", "Описание подзадачи 1",
                Status.NEW, epic1.getId());
        manager1.createSubtask(subtask1);

        System.out.println("\nВывод всех задач в manager1:");
        System.out.println("Все задачи: " + manager1.getAllTasks());
        System.out.println("Все эпики: " + manager1.getAllEpics());
        System.out.println("Все подзадачи: " + manager1.getAllSubtasks());

        // Загружаем данные из файла во второй менеджер
        FileBackedTaskManager manager2 = FileBackedTaskManager.loadFromFile(file);
        
        System.out.println("\nВывод всех загруженных задач в manager2:");
        System.out.println("Все задачи: " + manager2.getAllTasks());
        System.out.println("Все эпики: " + manager2.getAllEpics());
        System.out.println("Все подзадачи: " + manager2.getAllSubtasks());

        // Проверяем, что задачи идентичны
        Task loadedTask = manager2.getTaskById(task1.getId());
        Epic loadedEpic = manager2.getEpicById(epic1.getId());
        Subtask loadedSubtask = manager2.getSubtaskById(subtask1.getId());

        System.out.println("\nПроверка соответствия задач в manager1 загруженным задачам в manager2:");
        System.out.println("Задача в manager1 соответствует загруженной задаче в manager2: "
                + task1.getTitle().equals(loadedTask.getTitle()));
        System.out.println("Эпик в manager1 соответствует загруженному эпику в manager2: "
                + epic1.getTitle().equals(loadedEpic.getTitle()));
        System.out.println("Подзадача в manager1 соответствует загруженной подзадаче в manager2: "
                + subtask1.getTitle().equals(loadedSubtask.getTitle()));
    }
}