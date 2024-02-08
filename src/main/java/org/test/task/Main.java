package org.test.task;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class Main {
    private static final Logger logger = LogManager.getLogger("org.test.task.Main");
    static {
        Configurator.setLevel(logger.getName(), Level.INFO);
    }
    public static void main(String[] args) {
        ExecutorService executor = new ThreadPoolExecutor(10, 20, 60, TimeUnit.SECONDS, new SynchronousQueue<>(), new ThreadPoolExecutor.DiscardPolicy());
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            logger.info("Сервер запущен. Ожидание подключений..."+serverSocket.getLocalPort());

            while (true) {
                Socket player1Socket = serverSocket.accept();
                Socket player2Socket = serverSocket.accept();

                logger.info("Игроки подключены. Запуск игры...");

                GameThread gameThread = new GameThread(player1Socket, player2Socket);
                executor.submit(gameThread);
            }
        } catch (IOException e) {
            logger.error(e);
        }
        finally {
            executor.shutdown();
        }
    }

    private static class GameThread implements Runnable {
        private final Socket player1Socket;
        private final Socket player2Socket;

        public GameThread(Socket player1Socket, Socket player2Socket) {
            this.player1Socket = player1Socket;
            this.player2Socket = player2Socket;
        }

        @Override
        public void run() {
            try (
                    BufferedReader player1In = new BufferedReader(new InputStreamReader(player1Socket.getInputStream()));
                    PrintWriter player1Out = new PrintWriter(player1Socket.getOutputStream(), true);
                    BufferedReader player2In = new BufferedReader(new InputStreamReader(player2Socket.getInputStream()));
                    PrintWriter player2Out = new PrintWriter(player2Socket.getOutputStream(), true)
            ) {
                player1Out.println("Добро пожаловать в игру Камень-Ножницы-Бумага!");
                player2Out.println("Добро пожаловать в игру Камень-Ножницы-Бумага!");
                player2Out.println("Ожидайте, пока противник сделает свой выбор.");
                Result result = Result.DRAW;

                while (result == Result.DRAW) {

                    Choice player1Choice = null;
                    while (player1Choice == null) {
                        player1Out.println("Выберите: 1 - Камень, 2 - Ножницы, 3 - Бумага");
                        String player1ChoiceString = player1In.readLine();
                        if (player1ChoiceString == null) {
                            return;
                        }
                        player1Choice = Choice.valueOfChoiceDigit(player1ChoiceString);
                    }
                    player1Out.println("Ожидайте, пока противник сделает свой выбор.");
                    Choice player2Choice = null;
                    while (player2Choice == null) {
                        player2Out.println("Выберите: 1 - Камень, 2 - Ножницы, 3 - Бумага");
                        String player2ChoiceString = player2In.readLine();
                        if (player2ChoiceString == null) {
                            return;
                        }
                        player2Choice = Choice.valueOfChoiceDigit(player2ChoiceString);
                    }

                    result = determineWinner(player1Choice, player2Choice);

                    player1Out.println("Ваш выбор: " + player1Choice);
                    player1Out.println("Выбор противника: " + player2Choice);
                    player1Out.println("Результат: " + result);

                    player2Out.println("Ваш выбор: " + player2Choice);
                    player2Out.println("Выбор противника: " + player1Choice);
                    player2Out.println("Результат: " + result);
                }
            } catch (IOException e) {
                logger.error(e);
            }
        }

        private Result determineWinner(Choice player1Choice, Choice player2Choice) {
            if (player1Choice == player2Choice) {
                return Result.DRAW;
            } else if (
                    (player1Choice == Choice.ROCK && player2Choice == Choice.SCISSORS) ||
                            (player1Choice == Choice.SCISSORS && player2Choice == Choice.PAPER) ||
                            (player1Choice == Choice.PAPER && player2Choice == Choice.ROCK)
            ) {
                return Result.FIRST;
            } else {
                return Result.SECOND;
            }
        }
    }

    private enum Choice {
        ROCK("1", "Камень"),
        SCISSORS("2", "Ножницы"),
        PAPER("3", "Бумага");
        private final String choiceDigit;
        private final String label;

        Choice(String choiceDigit, String label) {
            this.choiceDigit = choiceDigit;
            this.label = label;
        }

        public static Choice valueOfChoiceDigit(String name) {
            for (Choice choice : values()) {
                if (choice.choiceDigit.equals(name)) {
                    return choice;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private enum Result {
        FIRST("Первый игрок победил"),
        SECOND("Второй игрок победил"),
        DRAW("Ничья");
        private final String label;

        Result(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
