package edu.wpi.first.compile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.GsonBuilder;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.language.base.LanguageSourceSet;
import org.gradle.language.c.CSourceSet;
import org.gradle.language.cpp.CppSourceSet;
import org.gradle.language.nativeplatform.HeaderExportingSourceSet;
import org.gradle.nativeplatform.NativeBinarySpec;
import org.gradle.nativeplatform.NativeDependencySet;
import org.gradle.nativeplatform.platform.internal.NativePlatformInternal;
import org.gradle.nativeplatform.toolchain.GccPlatformToolChain;
import org.gradle.nativeplatform.toolchain.NativeToolChain;
import org.gradle.nativeplatform.toolchain.VisualCppPlatformToolChain;
import org.gradle.nativeplatform.toolchain.internal.ToolType;
import org.gradle.nativeplatform.toolchain.internal.msvcpp.VisualCpp;
import org.gradle.nativeplatform.toolchain.internal.msvcpp.VisualStudioInstall;
import org.gradle.nativeplatform.toolchain.internal.tools.CommandLineToolConfigurationInternal;
import org.gradle.nativeplatform.toolchain.internal.tools.CommandLineToolSearchResult;
import org.gradle.nativeplatform.toolchain.internal.tools.ToolSearchPath;
import org.gradle.platform.base.internal.toolchain.SearchResult;

public class CompileCommandsTask extends DefaultTask {
  @OutputFile
  public RegularFileProperty compileCommandsFile = newOutputFile();

  static class CompileCommand {
    public String directory = "";
    public String command = "";
    public String file = "";
  }

  @TaskAction
  public void generate() {

    List<CompileCommand> compileCommands = new ArrayList<>();

    CompileCommandsExtension ext = getProject().getExtensions().getByType(CompileCommandsExtension.class);

    for (NativeBinarySpec bin : ext._binaries) {
      if (!bin.isBuildable()) {
        continue;
      }
      CommandLineToolConfigurationInternal cppInternal = null;
      CommandLineToolConfigurationInternal cInternal = null;

      String cPath = "";
      String cppPath = "";

      NativeToolChain toolChain = bin.getToolChain();

      for (VisualCppPlatformToolChain msvcPlat : ext._visualCppPlatforms) {
        if (msvcPlat.getPlatform().equals(bin.getTargetPlatform())) {
          cppInternal = (CommandLineToolConfigurationInternal) msvcPlat.getCppCompiler();
          cInternal = (CommandLineToolConfigurationInternal) msvcPlat.getcCompiler();

          if (toolChain instanceof org.gradle.nativeplatform.toolchain.VisualCpp) {
            org.gradle.nativeplatform.toolchain.VisualCpp vcpp = (org.gradle.nativeplatform.toolchain.VisualCpp) toolChain;
            SearchResult<VisualStudioInstall> vsiSearch = ext._vsLocator.locateComponent(vcpp.getInstallDir());
            if (vsiSearch.isAvailable()) {
              VisualStudioInstall vsi = vsiSearch.getComponent();
              VisualCpp vscpp = vsi.getVisualCpp().forPlatform((NativePlatformInternal) bin.getTargetPlatform());
              cppPath = vscpp.getCompilerExecutable().toString();
              cPath = vscpp.getCompilerExecutable().toString();
              break;
            }
          }
        }

        for (GccPlatformToolChain gccPlat : ext._gccLikePlatforms) {
          if (gccPlat.getPlatform().equals(bin.getTargetPlatform())) {
            cppInternal = (CommandLineToolConfigurationInternal) gccPlat.getCppCompiler();
            cInternal = (CommandLineToolConfigurationInternal) gccPlat.getcCompiler();

            ToolSearchPath tsp = new ToolSearchPath(OperatingSystem.current());
            CommandLineToolSearchResult cppSearch = tsp.locate(ToolType.CPP_COMPILER,
                gccPlat.getCppCompiler().getExecutable());
            if (cppSearch.isAvailable()) {
              cppPath = cppSearch.getTool().toString();
            }
            CommandLineToolSearchResult cSearch = tsp.locate(ToolType.C_COMPILER,
                gccPlat.getcCompiler().getExecutable());
            if (cSearch.isAvailable()) {
              cPath = cSearch.getTool().toString();
            }
            break;
          }
        }
      }

      List<String> cppArgs = new ArrayList<>();
      cppInternal.getArgAction().execute(cppArgs);

      List<String> cArgs = new ArrayList<>();
      cInternal.getArgAction().execute(cArgs);

      for (LanguageSourceSet sourceSet : bin.getInputs()) {
        if (sourceSet instanceof HeaderExportingSourceSet) {
          if (!(sourceSet instanceof CppSourceSet) && !(sourceSet instanceof CSourceSet)) {
            continue;
          }
          HeaderExportingSourceSet headerSourceSet = (HeaderExportingSourceSet)sourceSet;
          boolean cpp = true;
          if (sourceSet instanceof CSourceSet) {
            cpp = false;
          }
          for (File file : headerSourceSet.getSource()) {
            CompileCommand compileCommand = new CompileCommand();
            compileCommand.file = file.toString();
            String command = cpp ? cppPath : cPath;
            for (String arg : cpp ? cppArgs : cArgs) {
              command += " " + arg;
            }
            for (String arg : cpp ? bin.getCppCompiler().getArgs() : bin.getcCompiler().getArgs()) {
              command += " " + arg;
            }
            for (Map.Entry<String, String> arg : cpp ? bin.getCppCompiler().getMacros().entrySet() : bin.getcCompiler().getMacros().entrySet()) {
              command += " -D" + arg.getKey() + "=\"" + arg.getValue() + "\"";
            }
            for (NativeDependencySet dep : bin.getLibs()) {
              for (File f : dep.getIncludeRoots()) {
                command += " -I\"" + f.toString() + "\"";
              }
            }

            for (File f : headerSourceSet.getExportedHeaders().getSrcDirs()) {
              command += " -I\"" + f.toString() + "\"";
            }
            compileCommand.command = command;
            compileCommand.directory = getProject().getBuildDir().toString();
            compileCommands.add(compileCommand);
          }
        }
      }
    }

    GsonBuilder builder = new GsonBuilder();

    builder.setPrettyPrinting();

    String json = builder.create().toJson(compileCommands);

    File file = compileCommandsFile.getAsFile().get();
    file.getParentFile().mkdirs();

    try (BufferedWriter writer = Files.newBufferedWriter(file.toPath())) {
      writer.append(json);
    } catch (IOException ex) {

    }
  }
}
