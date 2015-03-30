package org.nlp4l.gui;

import java.io.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.IllegalArgumentException;
import java.lang.UnsupportedOperationException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.HttpURLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a simple and single threaded http server
 */
public class SimpleHttpServer {

  static final String LF = System.getProperty("line.separator");
  static final int DEFAULT_PORT = 6574;
  static final int MAX_HEADER_SIZE = 8192;
  static final Pattern COMMAND = Pattern.compile("^(\\w+)\\s+(.+?)\\s+HTTP/([\\d.]+)$");
  ServerSocket serverSocket;
  Presentation presentation;
  boolean debug;

  public SimpleHttpServer(Presentation presentation, int port, boolean debug) throws IOException {
    this.presentation = presentation;
    serverSocket = new ServerSocket();
    serverSocket.setReuseAddress(true);
    serverSocket.bind(new InetSocketAddress(port));
    this.debug = debug;

    presentation.setup(port, debug);
  }

  public SimpleHttpServer(Presentation presentation, int port) throws IOException {
    this(presentation, port, false);
  }

  public SimpleHttpServer(Presentation presentation, boolean debug) throws IOException {
    this(presentation, DEFAULT_PORT, debug);
  }

  public SimpleHttpServer(Presentation presentation) throws IOException {
    this(presentation, DEFAULT_PORT, false);
  }

  public void close() {
    presentation.shutdown();
    if (serverSocket == null) {
      return;
    }
    try {
      serverSocket.close();
      serverSocket = null;
    }
    catch (IOException ignored) {
    }
  }

  public void service() throws IOException {
    assert serverSocket != null;

    presentation.start();

    for (Socket sock = accept(); sock != null; sock = accept()) {
      try {
        Request req = new Request(sock, debug);
        if (req.path.equals("/shutdown")) {
          response(200, "OK", sock.getOutputStream());
          close();
          break;
        }
        else {
          response(req, sock.getOutputStream());
        }
      }
      catch (IllegalArgumentException e) {
        if (debug) {
          e.printStackTrace();
        }
        response(sock.getOutputStream(), e);
      }
      catch (java.io.FileNotFoundException e) {
        if (debug) {
          e.printStackTrace();
        }
        response(sock.getOutputStream(), e);
      }
      finally {
        sock.close();
      }
    }
  }

  void response(Request req, OutputStream out) throws IOException {
    if (req.method.equals("GET")) {
      File f = new File(".", req.path);
      if (f.getAbsolutePath().indexOf("..") < 0 && f.isFile()) {
        response(f, out);
      }
      else{
        responseSuccess(presentation.length(), "text/html", out);
        presentation.execute(req.path, out);
      }
    }
    else {
      throw new UnsupportedOperationException("unknown method:" + req.method);
    }
  }

  void responseSuccess(int len, String type, OutputStream out) throws IOException {
    PrintWriter pw = new PrintWriter(out);
    pw.print("HTTP/1.1 200 OK\r\n");
    pw.print("Connection: close\r\n");
    pw.print("Content-Length: ");
    pw.print(len);
    pw.print("\r\n");
    pw.print("Content-Type: ");
    pw.print(type);
    pw.print("\r\n\r\n");
    pw.flush();
  }

  void response(File f, OutputStream out) throws IOException {
    responseSuccess((int)f.length(), "text/html", out);
    BufferedInputStream bi = new BufferedInputStream(new FileInputStream(f));
    try {
      for (int c = bi.read(); c >= 0; c = bi.read()) {
        out.write(c);
      }
    } finally {
      bi.close();
    }
  }

  void response(OutputStream out, IllegalArgumentException e) throws IOException {
    response(HttpURLConnection.HTTP_BAD_REQUEST, e.getMessage(), out);
  }

  void response(OutputStream out, FileNotFoundException e) throws IOException {
    response(HttpURLConnection.HTTP_NOT_FOUND, e.getMessage(), out);
  }

  void response(OutputStream out, UnsupportedOperationException e) throws IOException {
    response(HttpURLConnection.HTTP_BAD_METHOD, e.getMessage(), out);
  }

  void response(int stat, String msg, OutputStream out) throws IOException {
    PrintWriter pw = new PrintWriter(out);
    pw.print("HTTP/1.1 ");
    pw.print(stat);
    pw.print(" ");
    pw.print(msg);
    pw.print("\r\n\r\n");
    pw.flush();
  }

  Socket accept() throws IOException {
    try {
      return serverSocket.accept();
    } catch (SocketException ignored) {
    }
    return null;
  }

  static final class Request {
    String method;
    String version;
    String path;
    String[] metadata;
    InputStream in;
    boolean debug;

    Request(Socket sock, boolean debug) throws IOException {
      this.debug = debug;
      in = sock.getInputStream();
      header();
      if (debug) {
        System.out.println(this);
        for (int i = 0; i < metadata.length; i++) {
          System.out.println(metadata[i]);
        }
      }
    }

    void header() throws IOException {
      byte[] buff = new byte[2000];
      for (int i = 0; ; i++) {
        int c = in.read();
        if (c < 0) {
          throw new IllegalArgumentException("header is too short");
        }
        buff[i] = (byte)c;
        if (i > 3
            && buff[i - 3] == '\r' && buff[i - 2] == '\n'
            && buff[i - 1] == '\r' && buff[i] == '\n') {
          createHeader(buff, i - 4);
          break;
        }
        else if (i == buff.length - 1) {
          if (i > MAX_HEADER_SIZE) {
            throw new IllegalArgumentException("header is too long:" + new String(buff, 0, 256));
          }
          byte[] nbuff = new byte[buff.length * 2];
          System.arraycopy(buff, 0, nbuff, 0, i + 1);
          buff = nbuff;
        }
      }
    }

    void createHeader(byte[] buff, int len) {
      for (int i = 0; i < len; i++) {
        if (i > 2 && buff[i - 1] == '\r' && buff[i] == '\n') {
          Matcher m = COMMAND.matcher(new String(buff, 0, i - 1));
          if (m.matches()) {
            method = m.group(1);
            path = m.group(2);
            version = m.group(3);
          }
          else {
            throw new IllegalArgumentException("header is too long:" + new String(buff, 0, i + 1));
          }
          metadata = new String(buff, i + 1, len - i).split("\\r\\n");
          break;
        }
      }
    }

    public String toString() {
      StringBuffer sb = new StringBuffer(super.toString()).append(LF);
      sb.append(method);
      sb.append(' ').append(path).append(" HTTP/").append(version);
      return sb.toString();
    }
  }
}
