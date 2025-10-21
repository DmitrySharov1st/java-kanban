package service;

import model.Epic;
import model.Subtask;
import model.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface TaskManager {
    Task createTask(Task task);

    Optional<Task> getTaskById(int id);

    ArrayList<Task> getAllTasks();

    void updateTask(Task task);

    void deleteTaskById(int id);

    void deleteAllTasks();

    Epic createEpic(Epic epic);

    Optional<Epic> getEpicById(int id);

    ArrayList<Epic> getAllEpics();

    void updateEpic(Epic epic);

    void deleteEpicById(int id);

    void deleteAllEpics();

    Subtask createSubtask(Subtask subtask);

    Optional<Subtask> getSubtaskById(int id);

    ArrayList<Subtask> getAllSubtasks();

    void updateSubtask(Subtask subtask);

    void deleteSubtaskById(int id);

    void deleteAllSubtasks();

    ArrayList<Subtask> getSubtasksByEpicId(int epicId);

    List<Task> getHistory();

    List<Task> getPrioritizedTasks();
}
