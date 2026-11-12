package com.github.kklisura.java.processing.support.impl;

/*-
 * #%L
 * props-to-constants-generator
 * %%
 * Copyright (C) 2018 Kenan Klisura
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import static com.github.kklisura.java.processing.utils.TestUtils.getFixtureURI;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.github.kklisura.java.processing.support.PropertiesProvider;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.FileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

/**
 * Properties provider test.
 *
 * @author Kenan Klisura
 */
@RunWith(EasyMockRunner.class)
public class PropertiesProviderImplTest extends EasyMockSupport {
  @Mock private Filer filer;

  @Mock private ProcessingEnvironment processingEnvironment;

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private PropertiesProvider propertiesProvider;

  @Before
  public void setUp() throws Exception {
    propertiesProvider = new PropertiesProviderImpl();
  }

  @Test
  public void testLoadProperties() throws IOException {
    FileObject simpleJavaFileObject = new FixtureFileObject("test-properties");

    expect(processingEnvironment.getFiler()).andReturn(filer);
    expect(filer.getResource(StandardLocation.CLASS_OUTPUT, "", "test-properties"))
        .andReturn(simpleJavaFileObject);

    replayAll();

    Properties properties =
        propertiesProvider.loadProperties("test-properties", processingEnvironment);

    verifyAll();

    assertNotNull(properties);

    assertTrue(properties.containsKey("hello"));
    assertTrue(properties.containsKey("hello.world"));
  }

  @Test(expected = RuntimeException.class)
  public void testLoadPropertiesThrowsException() throws IOException {
    FileObject simpleJavaFileObject = new ThrowableFixtureFileObject("test-properties");

    expect(processingEnvironment.getFiler()).andReturn(filer);
    expect(filer.getResource(StandardLocation.CLASS_OUTPUT, "", "test-properties"))
        .andReturn(simpleJavaFileObject);

    propertiesProvider.loadProperties("test-properties", processingEnvironment);
  }

  @Test
  public void testLoadPropertiesThrowsException2() throws IOException {
    AtomicBoolean closed = new AtomicBoolean(false);
    InputStream throwingInputStream =
        new InputStream() {
          @Override
          public int read() throws IOException {
            throw new IOException("exception");
          }

          @Override
          public int read(byte[] b) throws IOException {
            throw new IOException("exception");
          }

          @Override
          public int read(byte[] b, int off, int len) throws IOException {
            throw new IOException("exception");
          }

          @Override
          public void close() {
            closed.set(true);
          }
        };
    FileObject simpleJavaFileObject =
        new InputStreamFileObject("test-properties", throwingInputStream);

    expect(processingEnvironment.getFiler()).andReturn(filer);
    expect(filer.getResource(StandardLocation.CLASS_OUTPUT, "", "test-properties"))
        .andReturn(simpleJavaFileObject);
    expect(processingEnvironment.getOptions()).andReturn(Collections.emptyMap());

    replayAll();

    try {
      propertiesProvider.loadProperties("test-properties", processingEnvironment);
      fail();
    } catch (IOException e) {
      // Ok
    }

    verifyAll();
    assertTrue(closed.get());
  }

  @Test
  public void testLoadPropertiesFromResourceDirWhenClassOutputLacksIt() throws IOException {
    String emptyDir = temporaryFolder.newFolder().getPath();
    String fixturesDir = Paths.get(getFixtureURI("test-properties")).getParent().toString();

    expect(processingEnvironment.getFiler()).andReturn(filer);
    expect(filer.getResource(StandardLocation.CLASS_OUTPUT, "", "test-properties"))
        .andReturn(new ThrowableFixtureFileObject("test-properties"));
    expect(processingEnvironment.getOptions())
        .andReturn(
            Collections.singletonMap(
                PropertiesProviderImpl.RESOURCE_DIRS_OPTION,
                emptyDir + File.pathSeparator + fixturesDir));

    replayAll();

    Properties properties =
        propertiesProvider.loadProperties("test-properties", processingEnvironment);

    verifyAll();

    assertEquals("world", properties.getProperty("hello.world"));
  }

  @Test
  public void testLoadPropertiesWithoutResourceDirsNamesTheOption() throws IOException {
    expect(processingEnvironment.getFiler()).andReturn(filer);
    expect(filer.getResource(StandardLocation.CLASS_OUTPUT, "", "test-properties"))
        .andReturn(new ThrowableFixtureFileObject("test-properties"));
    expect(processingEnvironment.getOptions()).andReturn(Collections.emptyMap());

    replayAll();

    try {
      propertiesProvider.loadProperties("test-properties", processingEnvironment);
      fail();
    } catch (IOException e) {
      assertTrue(e.getMessage().contains(PropertiesProviderImpl.RESOURCE_DIRS_OPTION));
    }

    verifyAll();
  }

  @Test
  public void testLoadPropertiesNamesTheResourceDirsItSearched() throws IOException {
    String emptyDir = temporaryFolder.newFolder().getPath();

    expect(processingEnvironment.getFiler()).andReturn(filer);
    expect(filer.getResource(StandardLocation.CLASS_OUTPUT, "", "test-properties"))
        .andReturn(new ThrowableFixtureFileObject("test-properties"));
    expect(processingEnvironment.getOptions())
        .andReturn(Collections.singletonMap(PropertiesProviderImpl.RESOURCE_DIRS_OPTION, emptyDir));

    replayAll();

    try {
      propertiesProvider.loadProperties("test-properties", processingEnvironment);
      fail();
    } catch (IOException e) {
      assertTrue(e.getMessage().contains(emptyDir));
    }

    verifyAll();
  }

  public static class FixtureFileObject extends SimpleJavaFileObject {
    protected FixtureFileObject(String fixture) {
      super(getFixtureURI(fixture), Kind.OTHER);
    }

    @Override
    public InputStream openInputStream() throws IOException {
      return toUri().toURL().openStream();
    }
  }

  public static class ThrowableFixtureFileObject extends SimpleJavaFileObject {
    protected ThrowableFixtureFileObject(String fixture) {
      super(URI.create(fixture), Kind.OTHER);
    }

    @Override
    public InputStream openInputStream() throws IOException {
      throw new IOException("Exception");
    }
  }

  public static class InputStreamFileObject extends SimpleJavaFileObject {
    private InputStream inputStream;

    protected InputStreamFileObject(String fixture, InputStream inputStream) {
      super(URI.create(fixture), Kind.OTHER);
      this.inputStream = inputStream;
    }

    @Override
    public InputStream openInputStream() throws IOException {
      return inputStream;
    }
  }
}
