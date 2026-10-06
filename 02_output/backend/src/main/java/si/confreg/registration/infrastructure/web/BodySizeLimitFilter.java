package si.confreg.registration.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;
import si.confreg.registration.application.PayloadTooLargeException;

/** Limits request bodies to {@value #LIMIT_BYTES} bytes (SR-02), declared or streamed. */
public class BodySizeLimitFilter extends OncePerRequestFilter {

  public static final int LIMIT_BYTES = 16 * 1024;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (request.getContentLengthLong() > LIMIT_BYTES) {
      ProblemWriter.write(
          response,
          413,
          "Request too large",
          "The request body exceeds " + LIMIT_BYTES + " bytes.");
      return;
    }
    chain.doFilter(new LimitedRequest(request), response);
  }

  private static final class LimitedRequest extends HttpServletRequestWrapper {

    private ServletInputStream stream;

    LimitedRequest(HttpServletRequest request) {
      super(request);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
      if (stream == null) {
        stream = new LimitedStream(super.getInputStream());
      }
      return stream;
    }
  }

  private static final class LimitedStream extends ServletInputStream {

    private final ServletInputStream delegate;
    private long read;

    LimitedStream(ServletInputStream delegate) {
      this.delegate = delegate;
    }

    @Override
    public int read() throws IOException {
      int value = delegate.read();
      if (value >= 0) {
        count(1);
      }
      return value;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
      int n = delegate.read(buffer, offset, length);
      if (n > 0) {
        count(n);
      }
      return n;
    }

    private void count(int n) throws PayloadTooLargeException {
      read += n;
      if (read > LIMIT_BYTES) {
        throw new PayloadTooLargeException();
      }
    }

    @Override
    public boolean isFinished() {
      return delegate.isFinished();
    }

    @Override
    public boolean isReady() {
      return delegate.isReady();
    }

    @Override
    public void setReadListener(ReadListener listener) {
      delegate.setReadListener(listener);
    }
  }
}
