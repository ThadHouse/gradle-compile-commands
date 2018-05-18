package edu.wpi.first.compile;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.language.base.internal.ProjectLayout;
import org.gradle.model.Finalize;
import org.gradle.model.ModelMap;
import org.gradle.model.Mutate;
import org.gradle.model.RuleSource;
import org.gradle.nativeplatform.NativeBinarySpec;
import org.gradle.nativeplatform.toolchain.GccCompatibleToolChain;
import org.gradle.nativeplatform.toolchain.NativeToolChain;
import org.gradle.nativeplatform.toolchain.NativeToolChainRegistry;
import org.gradle.nativeplatform.toolchain.VisualCpp;
import org.gradle.nativeplatform.toolchain.internal.msvcpp.UcrtLocator;
import org.gradle.nativeplatform.toolchain.internal.msvcpp.VisualStudioLocator;
import org.gradle.nativeplatform.toolchain.internal.msvcpp.WindowsSdkLocator;
import org.gradle.platform.base.BinaryContainer;
import org.gradle.platform.base.BinarySpec;
import org.gradle.process.internal.ExecActionFactory;

class GradleCompileCommands implements Plugin<Project> {
  public void apply(Project project) {

    project.getExtensions().create("compileCommands", CompileCommandsExtension.class);

    project.getExtensions().getExtraProperties().set("CompileCommandsTask", CompileCommandsTask.class);

    project.getTasks().create("generateCompileCommands", CompileCommandsTask.class, task -> {
      task.setGroup("CompileCommands");
      task.setDescription("Generate compilecommands.json file");
      task.compileCommandsFile.set(project.getLayout().getBuildDirectory().file("/compile_commands.json"));
    });

    project.getPluginManager().apply(CompileCommandsRules.class);
  }
}
