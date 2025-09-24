package service;

public class Managers {
    private Managers() {
        // Приватный конструктор предотвращает создание экземпляров
    }

    public static TaskManager getDefault() {
        return new InMemoryTaskManager();
    }

    public static HistoryManager getDefaultHistory() {
        return new InMemoryHistoryManager();
    }



}