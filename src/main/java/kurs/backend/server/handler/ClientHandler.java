package kurs.backend.server.handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
      serverInfo.onError();
    } finally {
      serverInfo.onDisconnect();
      closeQuietly(socket);
    }
  }

  private String handleLine(String line, String clientIp) {
    Request request;
    try {
      request = GSON.fromJson(line, Request.class);
    } catch (JsonSyntaxException e) {
      serverInfo.onError();
      return GSON.toJson(Response.fail(null, "Некорректный JSON", "PARSE_ERROR"));
    }

    if (request == null || request.getType() == null) {
      serverInfo.onError();
      return GSON.toJson(
          Response.fail(
              request != null ? request.getRequestId() : null,
              "Тип запроса не указан",
              "INVALID_REQUEST"));
    }

    try {
      Response response = dispatcher.dispatch(request, clientIp);
      if (!response.isSuccess()) serverInfo.onError();
      return GSON.toJson(response);
    } catch (Exception e) {
      serverInfo.onError();
      return GSON.toJson(
          Response.fail(request.getRequestId(), "Внутренняя ошибка сервера", "INTERNAL_ERROR"));
    }
  }

  private void closeQuietly(Socket s) {
    try {
      if (!s.isClosed()) s.close();
    } catch (IOException ignored) {
    }
  }
}
