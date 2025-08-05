package ru.SorestForest;

import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        if (Objects.equals(System.getenv("DEV_ENV"), "true")) {
            System.out.println("DEV_ENV loading");
            JFrame frame = new JFrame("Консоль");
            JTextArea textArea = new JTextArea();
            textArea.setEditable(false);
            textArea.setFont(new Font("Consolas", Font.PLAIN, 14));
            OutputStream outputStream = new OutputStreamToTextArea(textArea);
            PrintStream printStream = new PrintStream(outputStream, true, StandardCharsets.UTF_8);
            System.setOut(printStream);
            System.setErr(printStream);

            frame.add(new JScrollPane(textArea), BorderLayout.CENTER);
            frame.setSize(600, 400);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        } else {
            PrintStream printStream = new PrintStream(System.out, true, StandardCharsets.UTF_8);
            System.setOut(printStream);
            System.setErr(printStream);
            System.out.println("Loading server version!");
        }
        BotStarter.startBot();
    }
}

class OutputStreamToTextArea extends OutputStream {
    private final JTextArea textArea;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    public OutputStreamToTextArea(JTextArea textArea) {
        this.textArea = textArea;
    }

    @Override
    public void write(int b) {
        buffer.write(b);
        if (b == '\n') flushBuffer();
    }

    private void flushBuffer() {
        String text;
        text = buffer.toString(StandardCharsets.UTF_8);
        buffer.reset();
        SwingUtilities.invokeLater(() -> textArea.append(text));
    }
}