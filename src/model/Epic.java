package model;

import model.enums.Status;
import model.enums.TaskType;
import java.util.ArrayList;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class Epic extends Task {
    private ArrayList<Integer> subtaskIds;
    private LocalDateTime endTime;

    public Epic(String title, String description) {
        super(title, description, Status.NEW, Duration.ZERO, null);
        this.subtaskIds = new ArrayList<>();
        this.endTime = null;
    }

    public Epic(String title, String description, int id, Status status) {
        super(title, description, id, status, Duration.ZERO, null);
        this.subtaskIds = new ArrayList<>();
        this.endTime = null;
    }

    public Epic(String title, String description, int id, Status status,
                Duration duration, LocalDateTime startTime, LocalDateTime endTime) {
        super(title, description, id, status, duration, startTime);
        this.subtaskIds = new ArrayList<>();
        this.endTime = endTime;
    }

    public ArrayList<Integer> getSubtaskIds() {
        return new ArrayList<>(subtaskIds);
    }

    public void addSubtaskId(int subtaskId) {
        if (!subtaskIds.contains(subtaskId)) {
            subtaskIds.add(subtaskId);
        }
    }

    public void removeSubtaskId(int subtaskId) {
        subtaskIds.remove(Integer.valueOf(subtaskId));
    }

    public void clearSubtasks() {
        subtaskIds.clear();
    }

    @Override
    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void calculateTimeFields(List<Subtask> subtasks) {
        if (subtasks == null || subtasks.isEmpty()) {
            this.setStartTime(null);
            this.setDuration(Duration.ZERO);
            this.endTime = null;
            return;
        }

        LocalDateTime earliestStart = subtasks.stream()
                .map(Subtask::getStartTime)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        Duration totalDuration = subtasks.stream()
                .map(Subtask::getDuration)
                .reduce(Duration.ZERO, Duration::plus);

        LocalDateTime latestEnd = subtasks.stream()
                .map(Subtask::getEndTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        this.setStartTime(earliestStart);
        this.setDuration(totalDuration);
        this.endTime = latestEnd;
    }

    @Override
    public String toString() {
        return "Epic{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", status=" + status +
                ", duration=" + duration.toMinutes() + "min" +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", subtaskIds=" + subtaskIds +
                '}';
    }

    @Override
    public TaskType getType() {
        return TaskType.EPIC;
    }
}
