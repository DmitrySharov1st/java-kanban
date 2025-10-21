package service;

import model.Task;

import java.time.LocalDateTime;
import java.util.Collection;

public class TimeUtils {

    private TimeUtils() {
        // Приватный конструктор предотвращает создание экземпляров
    }

    public static boolean isOverlap(Task task1, Task task2) {
        if (task1 == null || task2 == null) {
            return false;
        }

        LocalDateTime start1 = task1.getStartTime();
        LocalDateTime start2 = task2.getStartTime();

        if (start1 == null || start2 == null) {
            return false;
        }

        LocalDateTime end1 = task1.getEndTime();
        LocalDateTime end2 = task2.getEndTime();

        // Проверка пересечения интервалов по методу наложения отрезков
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    public static boolean hasTimeOverlap(Task newTask, Collection<Task> existingTasks) {
        if (newTask == null || newTask.getStartTime() == null || existingTasks == null) {
            return false;
        }

        return existingTasks.stream()
                .anyMatch(existingTask ->
                        existingTask != null &&
                                !existingTask.equals(newTask) &&
                                existingTask.getStartTime() != null &&
                                isOverlap(newTask, existingTask));
    }
}