package si.confreg.registration.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/** Rejects request bodies larger than the limit with 413 (SR-02, spec 7.3). */
public class RequestBodyLimitFilter extends OncePerRequestFilter {

  /** 16 KiB. */
  public static final int MAX_BODY_BYTES = 16 * 1024;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (request.getContentLengthLong() > MAX_BODY_BYTES) {
      Problems.write(response, HttpStatus.CONTENT_TOO_LARGE);
      return;
    }
    LimitedRequest limited = new LimitedRequest(request);
    try {
      chain.doFilter(limited, response);
    } catch (ServletException | RuntimeException e) {
      if (limited.exceeded && !response.isCommitted()) {
        response.resetBuffer();
        Problems.write(response, HttpStatus.CONTENT_TOO_LARGE);
        return;
      }
      throw e;
    }
  }

  /** Stops a body without (or with a false) Content-Length after the limit. */
  private static final class LimitedRequest extends HttpServletRequestWrapper {

    private boolean exceeded;
    private ServletInputStream stream;

    LimitedRequest(HttpServletRequest request) {
      super(request);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
      if (stream == null) {
        ServletInputStream delegate = super.getInputStream();
        stream =
            new ServletInputStream() {
              private long count;

              @Override
              public int read() throws IOException {
                int value = delegate.read();
                if (value >= 0 && ++count > MAX_BODY_BYTES) {
                  exceeded = true;
                  throw new IOException("request body too large");
                }
                return value;
              }

              @Override
              public int read(byte[] buffer, int offset, int length) throws IOException {
                int read = delegate.read(buffer, offset, length);
                if (read > 0) {
                  count += read;
                  if (count > MAX_BODY_BYTES) {
                    exceeded = true;
                    throw new IOException("request body too large");
                  }
                }
                return read;
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
            };
      }
      return stream;
    }
  }

  /** Lets the exception handler recognise a body that exceeded the limit while being parsed. */
  static boolean exceeded(HttpServletRequest request) {
    HttpServletRequest current = request;
    while (current instanceof HttpServletRequestWrapper wrapper) {
      if (wrapper instanceof LimitedRequest limited) {
        return limited.exceeded;
      }
      current = (HttpServletRequest) wrapper.getRequest();
    }
    return false;
  }
}
