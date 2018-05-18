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

public class CompileCommandsRules extends RuleSource {
  @Finalize
  void getPlatformToolChains(NativeToolChainRegistry toolChains, ProjectLayout projectLayout) {
    Project project = (Project) projectLayout.getProjectIdentifier();

    CompileCommandsExtension ext = project.getExtensions().getByType(CompileCommandsExtension.class);

    for (NativeToolChain tc : toolChains) {
      if (tc instanceof VisualCpp) {
        VisualCpp vtc = (VisualCpp) tc;
        vtc.eachPlatform(t -> {
          ext._visualCppPlatforms.add(t);
        });
      } else if (tc instanceof GccCompatibleToolChain) {
        GccCompatibleToolChain gtc = (GccCompatibleToolChain) tc;
        gtc.eachPlatform(t -> {
          ext._gccLikePlatforms.add(t);
        });
      }
    }
  }

  @Mutate
  void getBinaries(ModelMap<Task> tasks, BinaryContainer bins, ProjectLayout projectLayout,
      ServiceRegistry serviceRegistry) {

    Project project = (Project) projectLayout.getProjectIdentifier();

    CompileCommandsExtension ext = project.getExtensions().getByType(CompileCommandsExtension.class);

    VisualStudioLocator locator = serviceRegistry.get(VisualStudioLocator.class);
    WindowsSdkLocator sdkLocator = serviceRegistry.get(WindowsSdkLocator.class);
    UcrtLocator ucrtLocator = serviceRegistry.get(UcrtLocator.class);

    ExecActionFactory execActionFactory = serviceRegistry.get(ExecActionFactory.class);

    ext._vsLocator = locator;
    ext._vssdkLocator = sdkLocator;
    ext._vsucrtLocator = ucrtLocator;
    ext._execActionFactory = execActionFactory;

    for (BinarySpec oBin : bins) {
      if (!(oBin instanceof NativeBinarySpec)) {
        continue;
      }
      NativeBinarySpec bin = (NativeBinarySpec) oBin;
      ext._binaries.add(bin);
    }
  }
}
