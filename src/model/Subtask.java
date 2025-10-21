package model;

import model.enums.Status;
import model.enums.TaskType;

import java.time.Duration;
import java.time.LocalDateTime;

public class Subtask  extends Task {
    private int epicId;

    public Subtask(String title, String description, Status status, int epicId,
                   Duration duration, LocalDateTime startTime) {
        super(title, description, status, duration, startTime);
        this.epicId = epicId;
    }

    public Subtask(String title, String description, int id, Status status, int epicId,
                   Duration duration, LocalDateTime startTime) {
        super(title, description, id, status, duration, startTime);
        this.epicId = epicId;
    }

    public Subtask(String title, String description, Status status, int epicId) {
        super(title, description, status, Duration.ZERO, null);
        this.epicId = epicId;
    }

    public Subtask(String title, String description, int id, Status status, int epicId) {
        super(title, description, id, status, Duration.ZERO, null);
        this.epicId = epicId;
    }

    public int getEpicId() {
        return epicId;
    }

    public void setEpicId(int epicId) {
        this.epicId = epicId;
    }

    @Override
    public String toString() {
        return "Subtask{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", status=" + status +
                ", duration=" + duration.toMinutes() + "min" +
                ", startTime=" + startTime +
                ", epicId=" + epicId +
                '}';
    }

    @Override
    public TaskType getType() {
        return TaskType.SUBTASK;
    }
}
