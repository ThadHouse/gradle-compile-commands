package edu.wpi.first.compile;

import groovy.lang.Closure;
import org.gradle.api.GradleException;
import org.gradle.api.NamedDomainObjectSet;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.language.base.LanguageSourceSet;
import org.gradle.language.base.internal.ProjectLayout;
import org.gradle.language.base.plugins.ComponentModelBasePlugin;
import org.gradle.language.c.CSourceSet;
import org.gradle.language.cpp.CppSourceSet;
import org.gradle.language.nativeplatform.tasks.AbstractNativeSourceCompileTask;
import org.gradle.model.ModelMap;
import org.gradle.model.Mutate;
import org.gradle.model.RuleSource;
import org.gradle.model.Validate;
import org.gradle.nativeplatform.NativeBinarySpec;
import org.gradle.nativeplatform.NativeDependencySet;
import org.gradle.nativeplatform.NativeLibraryBinary;
import org.gradle.nativeplatform.PrebuiltLibraries;
import org.gradle.nativeplatform.PrebuiltLibrary;
import org.gradle.nativeplatform.Repositories;
import org.gradle.nativeplatform.SharedLibraryBinarySpec;
import org.gradle.nativeplatform.platform.internal.NativePlatformInternal;
import org.gradle.nativeplatform.toolchain.NativeToolChain;
import org.gradle.nativeplatform.toolchain.NativeToolChainRegistry;
import org.gradle.nativeplatform.toolchain.internal.PlatformToolProvider;
import org.gradle.nativeplatform.toolchain.internal.ToolType;
import org.gradle.nativeplatform.toolchain.internal.gcc.AbstractGccCompatibleToolChain;
import org.gradle.nativeplatform.toolchain.internal.msvcpp.VisualCppToolChain;
import org.gradle.nativeplatform.toolchain.internal.tools.ToolRegistry;
import org.gradle.platform.base.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

class GradleCompileCommands implements Plugin<Project> {
  public void apply(Project project) {
    project.getTasks().create("generateCompileCommands", CompileCommandsTask.class, (t) -> {

    });
  }

  static class Rules extends RuleSource {
    @Validate
    void getCommandsFromBinaries(ModelMap<Task> tasks, BinaryContainer binaries, ProjectLayout projectLayout, Repositories repos) {
      NamedDomainObjectSet<PrebuiltLibraries> preBuiltLibs = repos.withType(PrebuiltLibraries.class);

      Map<String, Set<File>> libMap = new HashMap<>();

      for (PrebuiltLibraries libs : preBuiltLibs) {
        for (PrebuiltLibrary lib : libs) {
          Set<File> dirs = new HashSet<>();
          for (NativeLibraryBinary b : lib.getBinaries()) {
            for (File f : b.getHeaderDirs()) {
              dirs.add(f);
            }
          }
          for (File h : lib.getHeaders()) {
            dirs.add(h);
          }
          libMap.put(lib.getName(), dirs);
        }
      }

      Project project = (Project)projectLayout.getProjectIdentifier();
      CompileCommandsTask commandsTask = (CompileCommandsTask)project.getTasks().getByPath("generateCompileCommands");
      for (BinarySpec oBinary : binaries) {
        if (!(oBinary instanceof NativeBinarySpec)) {
          continue;
        }
        NativeBinarySpec binary = (NativeBinarySpec)oBinary;
        for (LanguageSourceSet input : binary.getInputs()) {
          if (input instanceof CppSourceSet) {
            CppSourceSet cppInput = (CppSourceSet)input;
            System.out.println("cpp");
            SourceDirectorySet sources = cppInput.getSource();
            for (File source : sources) {
              System.out.println(source);
            }
            Set<File> includeDirs = new HashSet<File>();
            for (File include : cppInput.getExportedHeaders().getSrcDirs()) {
              includeDirs.add(include);
            }
            for (File include : cppInput.getImplicitHeaders().getSrcDirs()) {
              includeDirs.add(include);
            }

            for (Object lib : cppInput.getLibs()) {
              if (lib instanceof NativeDependencySet) {
                NativeDependencySet nlib = (NativeDependencySet)lib;
                for (File f : nlib.getIncludeRoots()) {
                  includeDirs.add(f);
                }

              } else if (lib instanceof Map) {
                Map nlib = (Map)lib;
                for (Object name : nlib.values()) {
                  Set<File> headers = libMap.get(name);
                  for (File f : headers) {
                    includeDirs.add(f);
                  }
                }
              }
            }

            for (File include : includeDirs) {
              System.out.println(include);
            }

            for (String a : binary.getCppCompiler().getArgs()) {
              System.out.println(a);
            }
          }
        }
      }
      System.out.println("Validate Running");
    }
  }
}
