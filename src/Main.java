import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {
        TaskManager taskManager = new TaskManager();

        System.out.println("Создание задач:");

        Task task1 = new Task("Задача 1", "Описание задачи 1", Status.NEW);
        Task task2 = new Task("Задача 2", "Описание задачи 2", Status.NEW);

        taskManager.createTask(task1);
        taskManager.createTask(task2);

        System.out.println("Создана задача 1: " + task1);
        System.out.println("Создана задача 2: " + task2);

        Epic epic1 = new Epic("Эпик 1", "Описание эпика 1");
        taskManager.createEpic(epic1);

        Subtask subtask1 = new Subtask("Подзадача 1 эпика 1", "Описание подзадачи 1",
                Status.NEW, epic1.getId());
        Subtask subtask2 = new Subtask("Подзадача 2 эпика 1", "Описание подзадачи 2",
                Status.NEW, epic1.getId());

        taskManager.createSubtask(subtask1);
        taskManager.createSubtask(subtask2);

        System.out.println("\nСоздан эпик 1: " + epic1);
        System.out.println("Создана подзадача 1: " + subtask1);
        System.out.println("Создана подзадача 2: " + subtask2);

        Epic epic2 = new Epic("Эпик 2", "Описание эпика 2");
        taskManager.createEpic(epic2);

        Subtask subtask3 = new Subtask("Подзадача 3 эпика 2", "Описание подзадачи 3", Status.NEW, epic2.getId());
        taskManager.createSubtask(subtask3);

        System.out.println("\nСоздан эпик 2: " + epic2);
        System.out.println("Создана подзадача 3: " + subtask3);

        System.out.println("\nВывод всех задач:");
        System.out.println("Все задачи: " + taskManager.getAllTasks());
        System.out.println("Все эпики: " + taskManager.getAllEpics());
        System.out.println("Все подзадачи: " + taskManager.getAllSubtasks());

        System.out.println("\nИзменение статусов:");

        task1.setStatus(Status.IN_PROGRESS);
        taskManager.updateTask(task1);
        task1.setDescription("Новое описание задачи 1");

        subtask1.setStatus(Status.DONE);
        taskManager.updateSubtask(subtask1);
        subtask1.setDescription("Новое описание подзадачи 1");

        subtask2.setStatus(Status.IN_PROGRESS);
        taskManager.updateSubtask(subtask2);

        epic1.setTitle("Эпик 1 (новое название)");

        System.out.println("Обновлена задача 1: " + taskManager.getTaskById(task1.getId()));
        System.out.println("Обновлена подзадача 1: " + taskManager.getSubtaskById(subtask1.getId()));
        System.out.println("Обновлена подзадача 2: " + taskManager.getSubtaskById(subtask2.getId()));
        System.out.println("Эпик 1 после изменений: " + taskManager.getEpicById(epic1.getId()));

        System.out.println("\nПолучение подзадач эпика:");
        ArrayList<Subtask> epic1Subtasks = taskManager.getSubtasksByEpicId(epic1.getId());
        System.out.println("Подзадачи эпика 1: " + epic1Subtasks);

        System.out.println("\nУдаление задач:");

        taskManager.deleteTaskById(task2.getId());
        System.out.println("После удаления задачи 2, все задачи: " + taskManager.getAllTasks());

        taskManager.deleteEpicById(epic2.getId());
        System.out.println("После удаления эпика 2:");
        System.out.println("Все эпики: " + taskManager.getAllEpics());
        System.out.println("Все подзадачи: " + taskManager.getAllSubtasks());

    }
}
