package kurs.backend.server;

import kurs.backend.domain.persistence.HibernateUtil;
import kurs.backend.domain.persistence.entity.User;
import kurs.backend.domain.persistence.entity.UserRole;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.util.ResourceBundle;

import java.io.*;
import java.net.*;

public class Server {
    // Single shared writer that flushes immediately
    private static final PrintStream console = new PrintStream(System.out, true);
    private static final ResourceBundle config = ResourceBundle.getBundle("server");
    private static final int PORT = Integer.parseInt(config.getString("PORT"));

    public static void main(String[] args) throws IOException {
        System.setOut(console);

//        User user = User.builder()
//                .login("admin")
//                .passwordHash("hashed_password")
//                .role(UserRole.admin)
//                .isActive(true)
//                .build();
//
//        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
//            Transaction transaction = session.beginTransaction();
//            session.persist(user);
//            transaction.commit();
//        }catch (Exception e){
//            e.printStackTrace();
//        }

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            console.println("Server listening on port " + PORT);
            while (true) {
                Socket client = serverSocket.accept();
                new Thread(() -> handle(client)).start();
            }
        }
    }

    private static void handle(Socket client) {
        try (
                var in  = new BufferedReader(new InputStreamReader(client.getInputStream()));
                var out = new PrintWriter(client.getOutputStream(), true)
        ) {
            String line;
            while ((line = in.readLine()) != null) {
                char[] chars = line.toCharArray();
                String result = "";
                for(int i = chars.length - 1; i >= 0; i--) {
                    result += chars[i];
                }

                console.println("Received: " + line);  // use console, not System.out
                out.println(result);
            }
        } catch (IOException e) {
            console.println("Client error: " + e.getMessage());
        }
    }
}