package si.confreg.registration.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Refuses request bodies over {@link #MAX_BODY_BYTES} with 413, by {@code Content-Length} and while
 * reading (SR-02). The accepted body is buffered (at most 16 KiB) and replayed to the application.
 */
public class BodySizeLimitFilter extends OncePerRequestFilter {

  public static final int MAX_BODY_BYTES = 16 * 1024;
  private static final String DETAIL = "The request body is too large.";

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String method = request.getMethod();
    return !("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (request.getContentLengthLong() > MAX_BODY_BYTES) {
      ProblemResponses.write(response, HttpStatus.PAYLOAD_TOO_LARGE, DETAIL);
      return;
    }
    byte[] body;
    try (InputStream in = request.getInputStream()) {
      body = in.readNBytes(MAX_BODY_BYTES + 1);
    }
    if (body.length > MAX_BODY_BYTES) {
      ProblemResponses.write(response, HttpStatus.PAYLOAD_TOO_LARGE, DETAIL);
      return;
    }
    chain.doFilter(new BufferedBodyRequest(request, body), response);
  }

  private static final class BufferedBodyRequest extends HttpServletRequestWrapper {
    private final byte[] body;

    private BufferedBodyRequest(HttpServletRequest request, byte[] body) {
      super(request);
      this.body = body.clone();
    }

    @Override
    public ServletInputStream getInputStream() {
      ByteArrayInputStream in = new ByteArrayInputStream(body);
      return new ServletInputStream() {
        @Override
        public boolean isFinished() {
          return in.available() == 0;
        }

        @Override
        public boolean isReady() {
          return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
          throw new UnsupportedOperationException("Asynchronous reading is not supported");
        }

        @Override
        public int read() {
          return in.read();
        }

        @Override
        public int read(byte[] buffer, int offset, int length) {
          return in.read(buffer, offset, length);
        }
      };
    }

    @Override
    public BufferedReader getReader() {
      String encoding = getCharacterEncoding();
      Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
      return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }

    @Override
    public int getContentLength() {
      return body.length;
    }

    @Override
    public long getContentLengthLong() {
      return body.length;
    }
  }
}
