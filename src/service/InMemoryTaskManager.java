package service;

import model.Epic;
import model.Subtask;
import model.Task;
import model.enums.Status;

import java.util.*;
import java.util.stream.Collectors;

public class InMemoryTaskManager implements TaskManager {
    protected int nextId = 1;
    protected final HashMap<Integer, Task> tasks = new HashMap<>();
    protected final HashMap<Integer, Epic> epics = new HashMap<>();
    protected final HashMap<Integer, Subtask> subtasks = new HashMap<>();

    protected final HistoryManager historyManager;

    protected final TreeSet<Task> prioritizedTasks = new TreeSet<>(
            Comparator.comparing(Task::getStartTime,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Task::getId)
    );

    public InMemoryTaskManager(HistoryManager historyManager) {
        this.historyManager = historyManager;
    }

    public InMemoryTaskManager() {
        this.historyManager = Managers.getDefaultHistory();
    }

    private int generateId() {
        return nextId++;
    }

    private void validateNoTimeOverlap(Task task) {
        if (task.getStartTime() != null && TimeUtils.hasTimeOverlap(task, prioritizedTasks)) {
            throw new ManagerValidateException("Задача пересекается по времени с существующей задачей");
        }
    }

    protected void addToPrioritizedTasks(Task task) {
        if (task.getStartTime() != null) {
            prioritizedTasks.add(task);
        }
    }

    private void removeFromPrioritizedTasks(Task task) {
        prioritizedTasks.remove(task);
    }

    private void updatePrioritizedTask(Task oldTask, Task newTask) {
        prioritizedTasks.remove(oldTask);
        addToPrioritizedTasks(newTask);
    }

    @Override
    public Task createTask(Task task) {
        if (task.getId() <= 0) {
            task.setId(generateId());
        }
        validateNoTimeOverlap(task);
        tasks.put(task.getId(), task);
        addToPrioritizedTasks(task);
        return task;
    }

    @Override
    public Optional<Task> getTaskById(int id) {
        Task task = tasks.get(id);

        if (task != null) {
            historyManager.add(task);
            return Optional.of(task);
        }
        return Optional.empty();
    }

    @Override
    public ArrayList<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    @Override
    public void updateTask(Task task) {
        if (tasks.containsKey(task.getId())) {
            Task oldTask = tasks.get(task.getId());
            validateNoTimeOverlap(task);
            tasks.put(task.getId(), task);
            updatePrioritizedTask(oldTask, task);
        }
    }

    @Override
    public void deleteTaskById(int id) {
        Task removed = tasks.remove(id);
        if (removed != null) {
            historyManager.remove(id);
            removeFromPrioritizedTasks(removed);
        }
    }

    @Override
    public void deleteAllTasks() {
        for (Integer id : new ArrayList<>(tasks.keySet())) {
            historyManager.remove(id);
            removeFromPrioritizedTasks(tasks.get(id));
        }
        tasks.clear();
    }

    @Override
    public Epic createEpic(Epic epic) {
        epic.setId(generateId());
        epic.setStatus(Status.NEW);
        epics.put(epic.getId(), epic);
        return epic;
    }

    @Override
    public Optional<Epic> getEpicById(int id) {
        Epic epic = epics.get(id);
        if (epic != null) {
            historyManager.add(epic);
            return Optional.of(epic);
        }
        return Optional.empty();
    }

    @Override
    public ArrayList<Epic> getAllEpics() {
        return new ArrayList<>(epics.values());
    }

    @Override
    public void updateEpic(Epic epic) {
        if (epics.containsKey(epic.getId())) {
            Epic existingEpic = epics.get(epic.getId());
            existingEpic.setTitle(epic.getTitle());
            existingEpic.setDescription(epic.getDescription());
        }
    }

    @Override
    public void deleteEpicById(int id) {
        Epic epic = epics.remove(id);
        if (epic != null) {
            historyManager.remove(id);
            for (Integer subtaskId : epic.getSubtaskIds()) {
                subtasks.remove(subtaskId);
                historyManager.remove(subtaskId);
            }
        }
    }

    @Override
    public void deleteAllEpics() {
        for (Epic epic : new ArrayList<>(epics.values())) {
            historyManager.remove(epic.getId());
            for (Integer subtaskId : epic.getSubtaskIds()) {
                subtasks.remove(subtaskId);
                historyManager.remove(subtaskId);
            }
        }
        epics.clear();
        subtasks.clear();
    }

    @Override
    public Subtask createSubtask(Subtask subtask) {
        if (subtask.getId() <= 0) {
            subtask.setId(generateId());
        }

        // ПРОВЕРКА: существует ли эпик для этой подзадачи
        if (!epics.containsKey(subtask.getEpicId())) {
            throw new NotFoundException(String.format("Эпик с id %d не обнаружен", subtask.getEpicId()));
        }

        if (subtask.getId() != 0 && subtask.getId() == subtask.getEpicId()) {
            throw new IllegalArgumentException("ID подзадачи и эпика не должны совпадать!");
        }

        validateNoTimeOverlap(subtask);
        subtasks.put(subtask.getId(), subtask);
        addToPrioritizedTasks(subtask);

        Epic epic = epics.get(subtask.getEpicId());
        if (epic != null) {
            epic.addSubtaskId(subtask.getId());
            updateEpicStatusAndTime(epic);
        }

        return subtask;
    }

    @Override
    public Optional<Subtask> getSubtaskById(int id) {
        Subtask subtask = subtasks.get(id);
        if (subtask != null) {
            historyManager.add(subtask);
            return Optional.of(subtask);
        }
        return Optional.empty();
    }

    @Override
    public ArrayList<Subtask> getAllSubtasks() {
        return new ArrayList<>(subtasks.values());
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        if (subtasks.containsKey(subtask.getId())) {

            // ПРОВЕРКА: существует ли новый эпик (если он изменился)
            if (!epics.containsKey(subtask.getEpicId())) {
                throw new NotFoundException(String.format("Эпик с id %d не обнаружен", subtask.getEpicId()));
            }

            Subtask oldSubtask = subtasks.get(subtask.getId());
            validateNoTimeOverlap(subtask);

            subtasks.put(subtask.getId(), subtask);
            updatePrioritizedTask(oldSubtask, subtask);

            if (oldSubtask.getEpicId() != subtask.getEpicId()) {
                Epic oldEpic = epics.get(oldSubtask.getEpicId());
                if (oldEpic != null) {
                    oldEpic.removeSubtaskId(subtask.getId());
                    updateEpicStatusAndTime(oldEpic);
                }

                Epic newEpic = epics.get(subtask.getEpicId());
                if (newEpic != null) {
                    newEpic.addSubtaskId(subtask.getId());
                    updateEpicStatusAndTime(newEpic);
                }
            } else {
                Epic epic = epics.get(subtask.getEpicId());
                if (epic != null) {
                    updateEpicStatusAndTime(epic);
                }
            }
        }
    }

    @Override
    public void deleteSubtaskById(int id) {
        Subtask subtask = subtasks.remove(id);
        if (subtask != null) {
            Epic epic = epics.get(subtask.getEpicId());
            if (epic != null) {
                epic.removeSubtaskId(id);
                updateEpicStatusAndTime(epic);
            }
            historyManager.remove(id);
            removeFromPrioritizedTasks(subtask);
        }
    }

    @Override
    public void deleteAllSubtasks() {
        for (Integer id : new ArrayList<>(subtasks.keySet())) {
            Subtask subtask = subtasks.remove(id);
            if (subtask != null) {
                historyManager.remove(id);
                removeFromPrioritizedTasks(subtask);
            }
        }
        for (Epic epic : epics.values()) {
            epic.clearSubtasks();
            updateEpicStatusAndTime(epic);
        }
    }

    @Override
    public ArrayList<Subtask> getSubtasksByEpicId(int epicId) {
        Epic epic = epics.get(epicId);
        if (epic == null) {
            return new ArrayList<>();
        }

        return epic.getSubtaskIds().stream()
                .map(subtasks::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    protected void updateEpicStatusAndTime(Epic epic) {
        updateEpicStatus(epic);
        updateEpicTime(epic);
    }


    protected void updateEpicStatus(Epic epic) {
        if (epic.getSubtaskIds().isEmpty()) {
            epic.setStatus(Status.NEW);
            return;
        }

        boolean allDone = true;
        boolean allNew = true;

        for (Integer subtaskId : epic.getSubtaskIds()) {
            Subtask subtask = subtasks.get(subtaskId);
            if (subtask == null) continue;

            if (subtask.getStatus() != Status.DONE) {
                allDone = false;
            }
            if (subtask.getStatus() != Status.NEW) {
                allNew = false;
            }
        }

        if (allDone) {
            epic.setStatus(Status.DONE);
        } else if (allNew) {
            epic.setStatus(Status.NEW);
        } else {
            epic.setStatus(Status.IN_PROGRESS);
        }
    }

    protected void updateEpicTime(Epic epic) {
        List<Subtask> epicSubtasks = getSubtasksByEpicId(epic.getId());
        epic.calculateTimeFields(epicSubtasks);
    }

    @Override
    public List<Task> getHistory() {
        return historyManager.getHistory();
    }

    @Override
    public List<Task> getPrioritizedTasks() {
        return prioritizedTasks.stream()
                .filter(task -> task.getStartTime() != null)
                .collect(Collectors.toList());
    }

    // Новые методы, которые бросают исключения (для http обработчиков)
    @Override
    public Task getTaskOrThrow(int id) throws NotFoundException {
        Task task = tasks.get(id);
        if (task == null) {
            throw new NotFoundException(String.format("Задача с id %d не обнаружена", id));
        }
        historyManager.add(task);
        return task;
    }

    @Override
    public Epic getEpicOrThrow(int id) throws NotFoundException {
        Epic epic = epics.get(id);
        if (epic == null) {
            throw new NotFoundException(String.format("Эпик с id %d не обнаружен", id));
        }
        historyManager.add(epic);
        return epic;
    }

    @Override
    public Subtask getSubtaskOrThrow(int id) throws NotFoundException {
        Subtask subtask = subtasks.get(id);
        if (subtask == null) {
            throw new NotFoundException(String.format("Подзадача с id %d не обнаружена", id));
        }
        historyManager.add(subtask);
        return subtask;
    }

}

