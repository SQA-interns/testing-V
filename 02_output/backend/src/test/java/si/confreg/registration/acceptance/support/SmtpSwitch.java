package si.confreg.registration.acceptance.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * TCP forwarder on a fixed local port in front of the mail catcher. Switching it off makes the SMTP
 * server unreachable for the backend (AC-001-27) without changing the backend's settings.
 */
public final class SmtpSwitch {

  private final String targetHost;
  private final int targetPort;
  private final int port;
  private final List<Socket> open = new CopyOnWriteArrayList<>();
  private volatile ServerSocket server;

  public SmtpSwitch(String targetHost, int targetPort) {
    this.targetHost = targetHost;
    this.targetPort = targetPort;
    try (ServerSocket probe = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
      this.port = probe.getLocalPort();
    } catch (IOException e) {
      throw new IllegalStateException("no free port", e);
    }
  }

  public int port() {
    return port;
  }

  public synchronized void on() {
    if (server != null) {
      return;
    }
    try {
      ServerSocket socket = new ServerSocket();
      socket.setReuseAddress(true);
      socket.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), port));
      server = socket;
    } catch (IOException e) {
      throw new IllegalStateException("cannot listen on " + port, e);
    }
    Thread acceptor = new Thread(() -> acceptLoop(server), "smtp-switch-accept");
    acceptor.setDaemon(true);
    acceptor.start();
  }

  public synchronized void off() {
    ServerSocket current = server;
    server = null;
    closeQuietly(current);
    for (Socket socket : open) {
      closeQuietly(socket);
    }
    open.clear();
  }

  private void acceptLoop(ServerSocket listening) {
    while (!listening.isClosed()) {
      try {
        Socket client = listening.accept();
        Socket upstream = new Socket(targetHost, targetPort);
        open.add(client);
        open.add(upstream);
        pump(client, upstream);
        pump(upstream, client);
      } catch (IOException e) {
        // closed by off() or upstream unavailable: the client sees a dropped connection
      }
    }
  }

  private void pump(Socket from, Socket to) {
    Thread thread =
        new Thread(
            () -> {
              try (InputStream in = from.getInputStream();
                  OutputStream out = to.getOutputStream()) {
                in.transferTo(out);
              } catch (IOException e) {
                // connection ended
              } finally {
                closeQuietly(from);
                closeQuietly(to);
              }
            },
            "smtp-switch-pump");
    thread.setDaemon(true);
    thread.start();
  }

  private static void closeQuietly(AutoCloseable closeable) {
    if (closeable == null) {
      return;
    }
    try {
      closeable.close();
    } catch (Exception e) {
      // already closed
    }
  }
}
