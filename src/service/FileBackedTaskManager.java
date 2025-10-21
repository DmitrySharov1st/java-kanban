package service;

import model.*;
import model.enums.Status;
import model.enums.TaskType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

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
            lines.add("id,type,name,status,description,epic,startTime,duration");

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

        // Должно быть 8 полей согласно заголовку
        String[] fields = new String[8];
        fields[0] = String.valueOf(task.getId());
        fields[1] = task.getType().toString();
        fields[2] = task.getTitle();
        fields[3] = task.getStatus().toString();
        fields[4] = task.getDescription();

        // Поле epic - для подзадач содержит ID эпика, для остальных - пустая строка
        if (task.getType() == TaskType.SUBTASK) {
            fields[5] = String.valueOf(((Subtask) task).getEpicId());
        } else {
            fields[5] = ""; // Пустая строка для TASK и EPIC
        }

        // Временные параметры
        fields[6] = task.getStartTime() != null ? task.getStartTime().toString() : "";
        fields[7] = String.valueOf(task.getDuration().toMinutes());

        return String.join(",", fields);
    }


    // Восстановление задачи из строки
    private static Task fromString(String value) {
        // Используем split с limit -1, чтобы сохранить все поля, включая пустые в конце
        String[] fields = value.split(",", -1);

        // Проверяем минимальное количество полей
        if (fields.length < 8) {
            throw new IllegalArgumentException("Неверный формат строки: ожидается 8 полей, получено "
                    + fields.length + ". Строка: " + value);
        }

        int id = Integer.parseInt(fields[0]);
        TaskType type = TaskType.valueOf(fields[1]);
        String name = fields[2];
        Status status = Status.valueOf(fields[3]);
        String description = fields[4];
        String epicIdStr = fields[5];

        LocalDateTime startTime = fields[6].isEmpty() ? null : LocalDateTime.parse(fields[6]);
        Duration duration = fields[7].isEmpty() ? Duration.ZERO : Duration.ofMinutes(Long.parseLong(fields[7]));

        switch (type) {
            case TASK:
                return new Task(name, description, id, status, duration, startTime);
            case EPIC:
                Epic epic = new Epic(name, description, id, status, duration, startTime, null);
                return epic;
            case SUBTASK:
                int epicId = Integer.parseInt(epicIdStr);
                return new Subtask(name, description, id, status, epicId, duration, startTime);
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
                manager.addToPrioritizedTasks(task);
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
                manager.updateEpicStatusAndTime(epic);
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

        // Создаем задачи с временными параметрами
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

        // Демонстрация новых функций
        System.out.println("\nПриоритетные задачи:");
        System.out.println(manager1.getPrioritizedTasks());

        // Загружаем данные из файла во второй менеджер
        FileBackedTaskManager manager2 = FileBackedTaskManager.loadFromFile(file);
        System.out.println("\nВывод всех загруженных задач в manager2:");
        System.out.println("Все задачи: " + manager2.getAllTasks());
        System.out.println("Все эпики: " + manager2.getAllEpics());
        System.out.println("Все подзадачи: " + manager2.getAllSubtasks());

        // Проверяем, что задачи идентичны (используем Optional)
        System.out.println("\nПроверка соответствия задач в manager1 загруженным задачам в manager2:");

        // Для Task
        manager2.getTaskById(task1.getId()).ifPresentOrElse(
                loadedTask -> {
                    System.out.println("Задача в manager1 соответствует загруженной задаче в manager2: "
                            + task1.getTitle().equals(loadedTask.getTitle()));
                },
                () -> System.out.println("Задача не найдена в manager2")
        );

        // Для Epic
        manager2.getEpicById(epic1.getId()).ifPresentOrElse(
                loadedEpic -> {
                    System.out.println("Эпик в manager1 соответствует загруженному эпику в manager2: "
                            + epic1.getTitle().equals(loadedEpic.getTitle()));
                },
                () -> System.out.println("Эпик не найден в manager2")
        );

        // Для Subtask
        manager2.getSubtaskById(subtask1.getId()).ifPresentOrElse(
                loadedSubtask -> {
                    System.out.println("Подзадача в manager1 соответствует загруженной подзадаче в manager2: "
                            + subtask1.getTitle().equals(loadedSubtask.getTitle()));
                },
                () -> System.out.println("Подзадача не найдена в manager2")
        );

        // Демонстрация работы с временными параметрами
        System.out.println("\n--- Демонстрация временных параметров ---");

        // Создаем задачу с временными параметрами
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        Duration duration = Duration.ofMinutes(45);
        Task taskWithTime = new Task("Задача со временем", "Описание", Status.NEW, duration, startTime);
        manager1.createTask(taskWithTime);

        System.out.println("Задача со временем создана:");
        System.out.println("Start: " + taskWithTime.getStartTime());
        System.out.println("Duration: " + taskWithTime.getDuration().toMinutes() + " мин");
        System.out.println("End: " + taskWithTime.getEndTime());

        System.out.println("\nОбновленный список приоритетных задач:");
        manager1.getPrioritizedTasks().forEach(task ->
                System.out.println(task.getTitle() + " - " + task.getStartTime())
        );

        // ДЕМОНСТРАЦИЯ РАБОТЫ С ЭПИКАМИ И ПОДЗАДАЧАМИ СО ВРЕМЕНЕМ
        System.out.println("\n=== ДЕМОНСТРАЦИЯ РАБОТЫ С ЭПИКАМИ И ПОДЗАДАЧАМИ СО ВРЕМЕНЕМ ===");

        // Создаем эпик для демонстрации временных параметров
        Epic timeEpic = new Epic("Эпик с временем выполнения", "Эпик с несколькими подзадачами с разным временем");
        manager1.createEpic(timeEpic);
        System.out.println("\nСоздан эпик: " + timeEpic.getTitle());

        // Создаем подзадачи с разным временем начала и продолжительностью
        LocalDateTime baseTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);

        Subtask timeSubtask1 = new Subtask("Утренняя подзадача", "Подготовительные работы",
                Status.NEW, timeEpic.getId(), Duration.ofMinutes(60), baseTime);
        manager1.createSubtask(timeSubtask1);

        Subtask timeSubtask2 = new Subtask("Дневная подзадача", "Основные работы",
                Status.NEW, timeEpic.getId(), Duration.ofMinutes(120), baseTime.plusHours(2));
        manager1.createSubtask(timeSubtask2);

        Subtask timeSubtask3 = new Subtask("Вечерняя подзадача", "Завершающие работы",
                Status.NEW, timeEpic.getId(), Duration.ofMinutes(45), baseTime.plusHours(5));
        manager1.createSubtask(timeSubtask3);

        // Показываем информацию о подзадачах
        System.out.println("\nСозданные подзадачи:");
        System.out.println("1. " + timeSubtask1.getTitle() +
                " - Начало: " + timeSubtask1.getStartTime() +
                ", Продолжительность: " + timeSubtask1.getDuration().toMinutes() + " мин");
        System.out.println("2. " + timeSubtask2.getTitle() +
                " - Начало: " + timeSubtask2.getStartTime() +
                ", Продолжительность: " + timeSubtask2.getDuration().toMinutes() + " мин");
        System.out.println("3. " + timeSubtask3.getTitle() +
                " - Начало: " + timeSubtask3.getStartTime() +
                ", Продолжительность: " + timeSubtask3.getDuration().toMinutes() + " мин");

        // Получаем обновленный эпик с рассчитанным временем
        Optional<Epic> updatedEpicOptional = manager1.getEpicById(timeEpic.getId());
        if (updatedEpicOptional.isPresent()) {
            Epic updatedEpic = updatedEpicOptional.get();
            System.out.println("\nАвтоматически рассчитанные параметры эпика:");
            System.out.println("Начало эпика: " + updatedEpic.getStartTime());
            System.out.println("Продолжительность эпика: " + updatedEpic.getDuration().toMinutes() + " мин");
            System.out.println("Завершение эпика: " + updatedEpic.getEndTime());
            System.out.println("Общая продолжительность всех подзадач: " +
                    (timeSubtask1.getDuration().plus(timeSubtask2.getDuration()).plus(timeSubtask3.getDuration())).toMinutes() + " мин");
        }

        // Демонстрация приоритетного списка с задачами и подзадачами
        System.out.println("\nПолный приоритетный список (все задачи и подзадачи с временем):");
        List<Task> allPrioritized = manager1.getPrioritizedTasks();
        if (allPrioritized.isEmpty()) {
            System.out.println("Нет задач с установленным временем начала");
        } else {
            allPrioritized.forEach(task -> {
                String type = task.getClass().getSimpleName();
                System.out.println(type + ": " + task.getTitle() +
                        " - Начало: " + task.getStartTime() +
                        ", Продолжительность: " + task.getDuration().toMinutes() + " мин");
            });
        }

        // Демонстрация проверки пересечений временных интервалов
        System.out.println("\n=== ДЕМОНСТРАЦИЯ ПРОВЕРКИ ПЕРЕСЕЧЕНИЙ ===");

        // Попытка создать задачу с пересекающимся временем
        Task overlappingTask = new Task("Пересекающаяся задача", "Должна вызвать ошибку",
                Status.NEW, Duration.ofMinutes(90), baseTime.plusHours(1));

        try {
            manager1.createTask(overlappingTask);
            System.out.println("Пересекающаяся задача создана (не должно быть этого сообщения)");
        } catch (ManagerValidateException e) {
            System.out.println("ОШИБКА ПЕРЕСЕЧЕНИЯ: " + e.getMessage());
            System.out.println("Задача '" + overlappingTask.getTitle() + "' не была создана из-за пересечения времени");
        }

        // Создаем задачу с непересекающимся временем
        Task nonOverlappingTask = new Task("Непересекающаяся задача", "Можно создать",
                Status.NEW, Duration.ofMinutes(30), baseTime.plusHours(8));

        try {
            manager1.createTask(nonOverlappingTask);
            System.out.println("Задача '" + nonOverlappingTask.getTitle() + "' успешно создана - время не пересекается");
        } catch (ManagerValidateException e) {
            System.out.println("ОШИБКА: " + e.getMessage());
        }

        // Демонстрация обновления времени подзадачи
        System.out.println("\n=== ДЕМОНСТРАЦИЯ ОБНОВЛЕНИЯ ВРЕМЕНИ ПОДЗАДАЧИ ===");

// Сначала посмотрим на текущий приоритетный список, чтобы понять занятые времена
        System.out.println("Текущий приоритетный список для анализа занятых времен:");
        manager1.getPrioritizedTasks().forEach(task -> {
            System.out.println(task.getTitle() + " - Начало: " + task.getStartTime() +
                    ", Окончание: " + task.getEndTime());
        });

// Найдем безопасное время для обновления - после всех существующих задач
        LocalDateTime latestEndTime = manager1.getPrioritizedTasks().stream()
                .map(Task::getEndTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(baseTime.plusHours(10));

        LocalDateTime safeUpdateTime = latestEndTime.plusHours(1);
        System.out.println("\nБезопасное время для обновления: " + safeUpdateTime);

// Обновляем время одной из подзадач на безопасное время
        timeSubtask1.setStartTime(safeUpdateTime);
        timeSubtask1.setDuration(Duration.ofMinutes(90));

        try {
            manager1.updateSubtask(timeSubtask1);
            System.out.println("Обновлена подзадача: " + timeSubtask1.getTitle());
            System.out.println("Новое время начала: " + timeSubtask1.getStartTime());
            System.out.println("Новая продолжительность: " + timeSubtask1.getDuration().toMinutes() + " мин");
        } catch (ManagerValidateException e) {
            System.out.println("Ошибка при обновлении подзадачи: " + e.getMessage());
            // В случае ошибки используем еще более позднее время
            safeUpdateTime = safeUpdateTime.plusHours(2);
            timeSubtask1.setStartTime(safeUpdateTime);
            manager1.updateSubtask(timeSubtask1);
            System.out.println("Подзадача обновлена с альтернативным временем: " + safeUpdateTime);
        }

// Показываем обновленные параметры эпика
        Optional<Epic> finalEpicOptional = manager1.getEpicById(timeEpic.getId());
        if (finalEpicOptional.isPresent()) {
            Epic finalEpic = finalEpicOptional.get();
            System.out.println("\nОбновленные параметры эпика после изменения подзадачи:");
            System.out.println("Начало эпика: " + finalEpic.getStartTime());
            System.out.println("Продолжительность эпика: " + finalEpic.getDuration().toMinutes() + " мин");
            System.out.println("Завершение эпика: " + finalEpic.getEndTime());
        }

// Дополнительная демонстрация: попытка обновления с пересечением
        System.out.println("\n--- Попытка обновления с пересечением времени ---");

// Находим новое безопасное время для временной подзадачи
        LocalDateTime newSafeTime = manager1.getPrioritizedTasks().stream()
                .map(Task::getEndTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(baseTime.plusHours(10))
                .plusHours(1);

// Создаем временную подзадачу в безопасное время
        Subtask tempSubtask = new Subtask("Временная подзадача", "Для демонстрации ошибки",
                Status.NEW, timeEpic.getId(), Duration.ofMinutes(30), newSafeTime);

        try {
            manager1.createSubtask(tempSubtask);
            System.out.println("Создана временная подзадача с временем: " + tempSubtask.getStartTime());
        } catch (ManagerValidateException e) {
            System.out.println("Ошибка при создании временной подзадачи: " + e.getMessage());
            // Если не удалось создать, пропускаем эту демонстрацию
            System.out.println("Пропускаем демонстрацию пересечения времени");
        } finally {
            // Если подзадача была создана, продолжаем демонстрацию
            if (manager1.getSubtaskById(tempSubtask.getId()).isPresent()) {
                // Пытаемся обновить с пересечением - используем время существующей задачи
                LocalDateTime conflictingTime = manager1.getPrioritizedTasks().stream()
                        .filter(task -> task.getStartTime() != null)
                        .findFirst()
                        .map(Task::getStartTime)
                        .orElse(baseTime);

                System.out.println("Попытка обновления подзадачи с пересекающимся временем: " + conflictingTime);
                tempSubtask.setStartTime(conflictingTime);

                try {
                    manager1.updateSubtask(tempSubtask);
                    System.out.println("Обновление прошло успешно (не должно быть этого сообщения)");
                } catch (ManagerValidateException e) {
                    System.out.println("ОШИБКА ПРИ ОБНОВЛЕНИИ: " + e.getMessage());
                    System.out.println("Подзадача не была обновлена из-за пересечения времени");
                }

                // Восстанавливаем корректное время (возвращаем на исходное)
                tempSubtask.setStartTime(newSafeTime);
                try {
                    manager1.updateSubtask(tempSubtask);
                    System.out.println("Подзадача восстановлена с корректным временем: " + tempSubtask.getStartTime());
                } catch (ManagerValidateException e) {
                    System.out.println("Ошибка при восстановлении времени: " + e.getMessage());
                }

                // Удаляем временную подзадачу
                manager1.deleteSubtaskById(tempSubtask.getId());
                System.out.println("Временная подзадача удалена");
            }
        }

        // Финальный приоритетный список
        System.out.println("\nФинальный приоритетный список:");
        manager1.getPrioritizedTasks().forEach(task ->
                System.out.println(task.getTitle() + " - " + task.getStartTime())
        );

        // Сохраняем и загружаем для демонстрации сохранения временных параметров
        System.out.println("\n=== СОХРАНЕНИЕ И ЗАГРУЗКА ВРЕМЕННЫХ ПАРАМЕТРОВ ===");

        FileBackedTaskManager manager3 = FileBackedTaskManager.loadFromFile(file);
        System.out.println("Загруженные задачи с временными параметрами:");

        manager3.getAllTasks().forEach(task -> {
            if (task.getStartTime() != null) {
                System.out.println(task.getTitle() + " - Start: " + task.getStartTime() +
                        ", Duration: " + task.getDuration().toMinutes() + "min");
            }
        });

        Optional<Epic> loadedEpicOptional = manager3.getEpicById(timeEpic.getId());
        if (loadedEpicOptional.isPresent()) {
            Epic loadedEpic = loadedEpicOptional.get();
            System.out.println("\nЗагруженный эпик с временными параметрами:");
            System.out.println("Начало: " + loadedEpic.getStartTime());
            System.out.println("Продолжительность: " + loadedEpic.getDuration().toMinutes() + " мин");
            System.out.println("Завершение: " + loadedEpic.getEndTime());
        }
    }
}