package com.icerovah.task_killer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;

public class Main {

    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) throws IOException {
        System.out.print("请输入端口: ");
        String port = scanner.nextLine();

        String ip_port = "0.0.0.0:" + port;

        String netstatCommand = String.format("cmd.exe /c netstat -ano | findstr \"%s\"", ip_port);
        System.out.printf("执行命令: %s%n", netstatCommand);

        Process netstatProcess = Runtime.getRuntime().exec(netstatCommand);
        BufferedReader netstatReader = new BufferedReader(new InputStreamReader(netstatProcess.getInputStream()));
        List<String> pids = printResultAndGetData(netstatReader, line -> {
            String[] split = line.trim().split("\\s+");
            if (!split[1].equals(ip_port)) {
                return null;
            }
            return split[4];
        });

        if (pids.isEmpty()) {
            System.out.printf("未找到端口%s的进程%n", port);
            return;
        }

        System.out.printf("找到端口%s的进程pid: %s%n", port, String.join(", ", pids));

        for (String pid : pids) {
            String tasklistCommannd = String.format("cmd.exe /c tasklist /fi \"PID eq %s\" /nh", pid);
            System.out.printf("执行命令: %s%n", tasklistCommannd);

            Process tasklistProcess = Runtime.getRuntime().exec(tasklistCommannd);
            BufferedReader tasklistReader = new BufferedReader(new InputStreamReader(tasklistProcess.getInputStream()));
            List<String> taskNames = printResultAndGetData(tasklistReader, line -> {
                String[] split = line.trim().split("\\s+");
                return split[0];
            });

            System.out.printf("请问是否结束PID=%s的进程(%s)[Y/N]?", pid, String.join(", ", taskNames));
            String action = scanner.nextLine();

            if ("Y".equalsIgnoreCase(action)) {
                String killCommand = String.format("cmd.exe /c taskkill /F /PID %s", pid);
                System.out.printf("执行命令: %s%n", killCommand);
                Process taskkillProcess = Runtime.getRuntime().exec(killCommand);
                BufferedReader taskkillReader = new BufferedReader(new InputStreamReader(taskkillProcess.getInputStream()));
                printResult(taskkillReader);
            }
        }

    }

    private static void printResult(BufferedReader reader) throws IOException {
        printResultAndGetData(reader, null);
    }

    private static List<String> printResultAndGetData(BufferedReader reader, Function<String, String> getData) throws IOException {
        String line;
        List<String> results = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) {
                continue;
            }
            System.out.println(line);

            if (getData == null) {
                continue;
            }

            String result = getData.apply(line);
            if (result != null) {
                results.add(result);
            }
        }
        return results;
    }

}
