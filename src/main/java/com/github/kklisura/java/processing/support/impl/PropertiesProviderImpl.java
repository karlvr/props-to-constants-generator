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

import com.github.kklisura.java.processing.support.PropertiesProvider;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import org.apache.commons.lang3.StringUtils;

/**
 * Properties provider implementation. It reads a resource from the class output. If the class
 * output does not contain the resource, it reads the resource from the directories that the {@link
 * #RESOURCE_DIRS_OPTION} processor option names.
 *
 * @author Kenan Klisura
 */
public class PropertiesProviderImpl implements PropertiesProvider {
  /**
   * The processor option that names the directories to read a resource from when the class output
   * does not contain it. Separate the directories with the platform's path separator. Set it when
   * the compiler can run before the build copies the resources to the class output, as an IDE's
   * build can.
   */
  public static final String RESOURCE_DIRS_OPTION = "propertySourceConstants.resourceDirs";

  private static final String EMPTY_PACKAGE = "";

  @Override
  public Properties loadProperties(String resourceName, ProcessingEnvironment processingEnv)
      throws IOException {
    try {
      FileObject resource =
          processingEnv
              .getFiler()
              .getResource(StandardLocation.CLASS_OUTPUT, EMPTY_PACKAGE, resourceName);
      return loadProperties(resource);
    } catch (IOException e) {
      List<Path> resourceDirs = resourceDirs(processingEnv);
      for (Path resourceDir : resourceDirs) {
        Path file = resourceDir.resolve(resourceName);
        if (Files.isRegularFile(file)) {
          return loadProperties(file);
        }
      }
      throw new IOException(notFoundMessage(resourceName, resourceDirs, e), e);
    }
  }

  private static List<Path> resourceDirs(ProcessingEnvironment processingEnv) {
    List<Path> result = new ArrayList<>();
    String value = processingEnv.getOptions().get(RESOURCE_DIRS_OPTION);
    if (StringUtils.isBlank(value)) {
      return result;
    }
    for (String dir : value.split(Pattern.quote(File.pathSeparator))) {
      if (StringUtils.isNotBlank(dir)) {
        result.add(Paths.get(dir.trim()));
      }
    }
    return result;
  }

  private static String notFoundMessage(
      String resourceName, List<Path> resourceDirs, IOException classOutputException) {
    String message =
        "Failed to read "
            + resourceName
            + " from the class output ("
            + classOutputException.getMessage()
            + ")";
    if (resourceDirs.isEmpty()) {
      return message
          + ". Set the processor option "
          + RESOURCE_DIRS_OPTION
          + " to also read it from the resource directories.";
    }
    return message + " or from " + resourceDirs;
  }

  private Properties loadProperties(Path file) throws IOException {
    Properties properties = new Properties();
    try (Reader reader =
        new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    return properties;
  }

  private Properties loadProperties(FileObject fileObject) throws IOException {
    Properties properties = new Properties();

    InputStream inputStream = null;
    try {
      inputStream = fileObject.openInputStream();
      Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
      properties.load(reader);
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
          // Ignore this exception.
        }
      }
    }

    return properties;
  }
}
