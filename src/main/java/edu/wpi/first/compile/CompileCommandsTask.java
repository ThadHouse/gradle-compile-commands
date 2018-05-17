package edu.wpi.first.compile;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.OutputFile;

public class CompileCommandsTask extends DefaultTask {
  @OutputFile
  public RegularFileProperty compileCommandsFile = newOutputFile();
}
