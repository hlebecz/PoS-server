package kurs.backend.server.handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import kurs.backend.domain.model.Request;
import kurs.backend.domain.model.Response;
import kurs.backend.server.JsonUtil;
import kurs.backend.server.ServerInfo;

public class ClientHandler implements Runnable {
  private static final Logger log = LogManager.getLogger(ClientHandler.class);
  private static final Gson GSON = JsonUtil.GSON;

  private final Socket socket;
  private final HandlerDispatcher dispatcher;
  private final ServerInfo serverInfo;

  public ClientHandler(Socket socket, HandlerDispatcher dispatcher, ServerInfo serverInfo) {
    this.socket = socket;
    this.dispatcher = dispatcher;
    this.serverInfo = serverInfo;
  }

  @Override
  public void run() {
    String clientIp = socket.getInetAddress().getHostAddress();
    ThreadContext.put("clientIp", clientIp);

    log.info("Client connected from: {}", clientIp);
    serverInfo.onConnect();

    try (BufferedReader in =
            new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
      String line;

      while ((line = in.readLine()) != null) {
        serverInfo.onRequest();
        String responseJson = handleLine(line, clientIp);
        out.println(responseJson);
      }

    } catch (IOException e) {
      log.error("IO error for client {}: {}", clientIp, e.getMessage());
      serverInfo.onError();
    } finally {
      log.info("Client disconnected: {}", clientIp);
      serverInfo.onDisconnect();
      closeQuietly(socket);
      ThreadContext.clearAll();
    }
  }

  private String handleLine(String line, String clientIp) {
    Request request;
    try {
      request = GSON.fromJson(line, Request.class);
    } catch (JsonSyntaxException e) {
      log.warn("Invalid JSON received from {}: {}", clientIp, e.getMessage());
      serverInfo.onError();
      return GSON.toJson(Response.fail(null, "Некорректный JSON", "PARSE_ERROR"));
    }

    if (request == null || request.getType() == null) {
      log.warn("Invalid request from {}: missing request type", clientIp);
      serverInfo.onError();
      return GSON.toJson(
          Response.fail(
              request != null ? request.getRequestId() : null,
              "Тип запроса не указан",
              "INVALID_REQUEST"));
    }

    // Add request context to MDC
    ThreadContext.put("requestId", request.getRequestId());

    log.info("Request received: type={}, requestId={}", request.getType(), request.getRequestId());

    try {
      Response response = dispatcher.dispatch(request, clientIp);
      if (!response.isSuccess()) {
        log.warn(
            "Request failed: type={}, requestId={}, errorCode={}",
            request.getType(),
            request.getRequestId(),
            response.getErrorCode());
        serverInfo.onError();
      } else {
        log.info(
            "Request successful: type={}, requestId={}", request.getType(), request.getRequestId());
      }
      return GSON.toJson(response);
    } catch (Exception e) {
      log.error(
          "Unexpected error processing request: type={}, requestId={}",
          request.getType(),
          request.getRequestId(),
          e);
      serverInfo.onError();
      return GSON.toJson(
          Response.fail(request.getRequestId(), "Внутренняя ошибка сервера", "INTERNAL_ERROR"));
    } finally {
      ThreadContext.remove("requestId");
    }
  }

  private void closeQuietly(Socket s) {
    try {
      if (!s.isClosed()) s.close();
    } catch (IOException ignored) {
    }
  }
}
