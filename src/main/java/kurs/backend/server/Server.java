package kurs.backend.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import kurs.backend.domain.persistence.dao.UserDao;
import kurs.backend.domain.persistence.entity.User;
import kurs.backend.domain.persistence.entity.UserRole;
import kurs.backend.domain.service.ServiceRegistry;
import kurs.backend.server.auth.PasswordUtil;
import kurs.backend.server.handler.ClientHandler;
import kurs.backend.server.handler.HandlerDispatcher;
import kurs.backend.server.handler.HandlerRegistry;

public class Server {

  private final Logger log = LogManager.getLogger(Server.class);

  private final HandlerDispatcher dispatcher;
  private final ServerInfo serverInfo;
  private final ServerConfig config;

  private final Set<Thread> clientThreads = ConcurrentHashMap.newKeySet();
  private final AtomicInteger threadCounter = new AtomicInteger();

  private volatile boolean running = false;
  private ServerSocket serverSocket;

  public Server(ServerConfig config) {
    this.serverInfo = ServerInfo.getInstance();
    this.config = config;

    ServiceRegistry services = ServiceRegistry.create();
    this.dispatcher = HandlerRegistry.create(services, serverInfo, this::shutdown);
    spawnAdmin(services.getUserService().getUserDao());
  }

  public void start() throws IOException {
    serverSocket = new ServerSocket(config.getPort());
    running = true;

    log.info("Server started on port {}", config.getPort());

    while (running) {
      try {
        Socket clientSocket = serverSocket.accept();
        clientSocket.setSoTimeout(30000);
        log.debug("Client connected: {}", clientSocket.getInetAddress().getHostAddress());

        spawnClientThread(clientSocket);

      } catch (SocketException e) {
        if (!running) break;
        log.error("Socket error: {}", e.getMessage());
      } catch (IOException e) {
        if (running) log.error("Accept error: {}", e.getMessage());
      }
    }

    log.info("Server stopped accepting connections");
  }

  private void spawnClientThread(Socket clientSocket) {
    String threadName = "client-" + threadCounter.incrementAndGet();

    ClientHandler handler = new ClientHandler(clientSocket, dispatcher, serverInfo);

    Thread thread =
        new Thread(
            () -> {
              try {
                handler.run();
              } finally {
                clientThreads.remove(Thread.currentThread());
              }
            },
            threadName);

    clientThreads.add(thread);
    thread.start();
  }

  public void shutdown() {
    log.info("Server shutdown initiated");
    running = false;

    try {
      if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
    } catch (IOException e) {
      log.error("Error closing ServerSocket: {}", e.getMessage());
    }

    long deadline = System.currentTimeMillis() + 30000;
    for (Thread t : clientThreads) {
      long remaining = deadline - System.currentTimeMillis();
      if (remaining <= 0) break;
      try {
        t.join(remaining);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }

    if (!clientThreads.isEmpty())
      log.warn("{} client thread(s) did not finish gracefully", clientThreads.size());

    log.info("Server shutdown complete");
  }

  private void spawnAdmin(UserDao dao) {
    if (!dao.findByRole(UserRole.ADMIN).isEmpty()) {
      return;
    }
    User admin =
        User.builder()
            .login("admin")
            .passwordHash(PasswordUtil.hash(config.getDbPassword()))
            .role(UserRole.ADMIN)
            .build();
    try {
      dao.save(admin);
    } catch (Exception e) {
      log.error("Error saving admin: {}", e.getMessage());
    }
  }

  public static void main(String[] args) throws IOException {

    ServerConfig config = ServerConfig.getInstance();
    Server server = new Server(config);

    Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown, "shutdown-hook"));

    server.start();
  }
}
