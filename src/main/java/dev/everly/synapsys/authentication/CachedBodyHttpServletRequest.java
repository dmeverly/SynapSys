package dev.everly.synapsys.authentication;

import java.io.*;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

	private final byte[] cachedBody;

	public CachedBodyHttpServletRequest(HttpServletRequest request, int maxBytes) throws IOException {
		super(request);
		try (InputStream inputStream = request.getInputStream()) {
			this.cachedBody = readUpTo(inputStream, maxBytes);
		}
	}

	private static byte[] readUpTo(InputStream inputStream, int maxBytes) throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(Math.min(maxBytes, 1024));
		byte[] buf = new byte[1024];
		int total = 0;
		int n;
		while ((n = inputStream.read(buf)) != -1) {
			total += n;
			if (total > maxBytes) {
				int allowed = n - (total - maxBytes);
				if (allowed > 0) {
					byteArrayOutputStream.write(buf, 0, allowed);
				}
				break;
			}
			byteArrayOutputStream.write(buf, 0, n);
		}
		return byteArrayOutputStream.toByteArray();
	}

	public byte[] getCachedBody() {
		return cachedBody;
	}

	@Override
	public ServletInputStream getInputStream() {
		ByteArrayInputStream bais = new ByteArrayInputStream(cachedBody);
		return new ServletInputStream() {
			@Override
			public int read() {
				return bais.read();
			}

			@Override
			public boolean isFinished() {
				return bais.available() == 0;
			}

			@Override
			public boolean isReady() {
				return true;
			}

			@Override
			public void setReadListener(ReadListener readListener) {
			}
		};
	}

	@Override
	public BufferedReader getReader() {
		return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
	}
}
