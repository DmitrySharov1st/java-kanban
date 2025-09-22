import model.Epic;
import model.Subtask;
import model.Task;
//import service.InMemoryTaskManager;
import service.Managers;
import service.TaskManager;
import model.enums.Status;

import java.util.List;

public class Main {

    public static void main(String[] args) {

        TaskManager taskManager = Managers.getDefault();

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

        Subtask subtask3 = new Subtask("Подзадача 3 эпика 2", "Описание подзадачи 3",
                Status.NEW, epic2.getId());
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

        task1.setDescription("Более Новое описание задачи 1");

        System.out.println("\nПолучение подзадач эпика:");
        List<Subtask> epic1Subtasks = taskManager.getSubtasksByEpicId(epic1.getId());
        System.out.println("Подзадачи эпика 1: " + epic1Subtasks);

        System.out.println("\nУдаление задач:");

        taskManager.deleteTaskById(task2.getId());
        System.out.println("После удаления задачи 2, все задачи: " + taskManager.getAllTasks());

        taskManager.deleteEpicById(epic2.getId());
        System.out.println("После удаления эпика 2:");
        System.out.println("Все эпики: " + taskManager.getAllEpics());
        System.out.println("Все подзадачи: " + taskManager.getAllSubtasks());

        printHistory(taskManager);

        System.out.println("\nОчистка всех задач в текущей серии проверок");
        taskManager.deleteAllEpics();
        taskManager.deleteAllTasks();


        System.out.println("Создание новой серии задач:");

        Task task11 = new Task("Задача 11", "Описание задачи 11", Status.NEW);
        Task task21 = new Task("Задача 21", "Описание задачи 21", Status.NEW);

        taskManager.createTask(task11);
        taskManager.createTask(task21);

        System.out.println("Создана задача 11: " + task11);
        System.out.println("Создана задача 21: " + task21);

        Epic epic11 = new Epic("Эпик 11", "Описание эпика 11");
        taskManager.createEpic(epic11);

        Subtask subtask11 = new Subtask("Подзадача 11 эпика 11", "Описание подзадачи 11",
                Status.NEW, epic11.getId());
        Subtask subtask21 = new Subtask("Подзадача 21 эпика 11", "Описание подзадачи 21",
                Status.NEW, epic11.getId());
        Subtask subtask31 = new Subtask("Подзадача 31 эпика 11", "Описание подзадачи 31",
                Status.NEW, epic11.getId());

        taskManager.createSubtask(subtask11);
        taskManager.createSubtask(subtask21);
        taskManager.createSubtask(subtask31);

        System.out.println("\nСоздан эпик 11: " + epic11);
        System.out.println("Создана подзадача 11: " + subtask11);
        System.out.println("Создана подзадача 21: " + subtask21);
        System.out.println("Создана подзадача 31: " + subtask31);

        Epic epic21 = new Epic("Эпик 21", "Описание эпика 21");
        taskManager.createEpic(epic21);

        System.out.println("\nСоздан эпик 21: " + epic21);

        System.out.println("\nВывод всех задач:");
        System.out.println("Все задачи: " + taskManager.getAllTasks());
        System.out.println("Все эпики: " + taskManager.getAllEpics());
        System.out.println("Все подзадачи: " + taskManager.getAllSubtasks());

        System.out.println("\nЗапрос задач:");
        System.out.println("Запрос задачи 21: " + taskManager.getTaskById(task21.getId()));
        System.out.println("Запрос задачи 11: " + taskManager.getTaskById(task11.getId()));
        System.out.println("Запрос подзадачи 21: " + taskManager.getSubtaskById(subtask21.getId()));
        System.out.println("Запрос подзадачи 31: " + taskManager.getSubtaskById(subtask31.getId()));
        System.out.println("Запрос эпика 11: " + taskManager.getEpicById(epic11.getId()));
        System.out.println("Запрос повторный подзадачи 21: " + taskManager.getSubtaskById(subtask21.getId()));
        System.out.println("Запрос повторный задачи 11: " + taskManager.getTaskById(task11.getId()));
        System.out.println("Запрос эпика 21: " + taskManager.getEpicById(epic21.getId()));
        System.out.println("Запрос повторный эпика 11: " + taskManager.getEpicById(epic11.getId()));

        printHistory(taskManager);

        System.out.println("\nУдаление задачи 11:");
        taskManager.deleteTaskById(task11.getId());

        printHistory(taskManager);

        System.out.println("\nУдаление эпика с подзадачами 11:");
        taskManager.deleteEpicById(epic11.getId());

        printHistory(taskManager);
    }

    private static void printHistory(TaskManager taskManager) {
        System.out.println("\nИстория просмотров:");
        List<Task> history = taskManager.getHistory();
        for (Task task : history) {
            System.out.println(task);
        }

    }
}
