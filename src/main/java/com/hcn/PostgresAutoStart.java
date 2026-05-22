package com.hcn;

import java.net.Socket;

public class PostgresAutoStart {

    public static void ensureRunning() {
        if (!isPostgresReachable()) {
            System.out.println("PostgreSQL not reachable, starting via docker...");
            try {
                ProcessBuilder pb = new ProcessBuilder(
                        "docker", "run", "-d", "--name", "hcn-postgres",
                        "-e", "POSTGRES_DB=hcn",
                        "-e", "POSTGRES_USER=hcn",
                        "-e", "POSTGRES_PASSWORD=hcn",
                        "-p", "5433:5432",
                        "postgres:16");
                pb.inheritIO();
                Process process = pb.start();
                process.waitFor();
                for (int i = 0; i < 30; i++) {
                    if (isPostgresReachable()) {
                        System.out.println("PostgreSQL is ready.");
                        return;
                    }
                    Thread.sleep(1000);
                }
                System.err.println("WARNING: PostgreSQL did not become reachable in 30s");
            } catch (Exception e) {
                System.err.println("WARNING: Could not start docker: " + e.getMessage());
            }
        }
    }

    private static boolean isPostgresReachable() {
        try (Socket socket = new Socket("localhost", 5433)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
