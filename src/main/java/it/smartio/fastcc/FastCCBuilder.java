
package it.smartio.fastcc;


import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import it.smartio.fastcc.parser.Options;

/**
 * The {@link FastCCBuilder} class.
 */
public class FastCCBuilder {

  public enum Language {

    Java("Java"),
    Cpp("C++");

    public final String name;

    Language(String name) {
      this.name = name;
    }

  }

  private Language language;
  private File     outputDirectory;

  private File     jj;
  private File     jjt;

  /**
   * Set the code generator.
   *
   * @param language
   */
  public final FastCCBuilder setCodeGenerator(Language language) {
    this.language = language;
    return this;
  }

  /**
   * Set the output directory.
   *
   * @param outputDirectory
   */
  public final FastCCBuilder setOutputDirectory(File outputDirectory, String... pathes) {
    this.outputDirectory = FastCCBuilder.toFile(outputDirectory, pathes);
    return this;
  }

  /**
   * Set the jj file.
   *
   * @param file
   */
  public final FastCCBuilder setJJFile(File file, String... pathes) {
    this.jj = FastCCBuilder.toFile(file, pathes);
    return this;
  }

  /**
   * Set the jj file.
   *
   * @param file
   */
  public final FastCCBuilder setJJTreeFile(File file, String... pathes) {
    this.jjt = FastCCBuilder.toFile(file, pathes);
    return this;
  }

  public static FastCCBuilder of(Language language) {
    FastCCBuilder builder = new FastCCBuilder();
    builder.setCodeGenerator(language);
    return builder;
  }

  /**
   * Run the parser generator.
   */
  public final void build() {
    try {
      List<String> arguments = new ArrayList<>();
      arguments.add("-CODE_GENERATOR=" + this.language.name);
      arguments.add("-OUTPUT_DIRECTORY=" + this.outputDirectory.getAbsolutePath());
      if (this.jjt != null) {
        arguments.add(this.jjt.getAbsolutePath());
        String path = this.jjt.getAbsolutePath();

        JJTree.main(arguments.toArray(new String[arguments.size()]));

        int offset = path.lastIndexOf("/");
        int length = path.lastIndexOf(".");
        arguments.set(arguments.size() - 1, this.outputDirectory + path.substring(offset, length) + ".jj");
      } else {
        arguments.add(this.jj.getAbsolutePath());
      }

      JJParser.main(arguments.toArray(new String[arguments.size()]));
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  /**
   * Run the parser generator.
   */
  public final void interpret(String text) {
    Options options = new Options();
    try {
      FastCCInterpreter interpreter = new FastCCInterpreter(options);
      File file = this.jj;
      if (file == null) {
        String name = this.jjt.getName();
        String jjName = name.substring(0, name.length() - 1);
        file = new File(this.outputDirectory, jjName);
      }
      String grammar = new String(Files.readAllBytes(file.toPath()));
      interpreter.runTokenizer(grammar, text);
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private static File toFile(File file, String... pathes) {
    return (pathes.length == 0) ? file : new File(file, String.join(File.separator, pathes));
  }
}
