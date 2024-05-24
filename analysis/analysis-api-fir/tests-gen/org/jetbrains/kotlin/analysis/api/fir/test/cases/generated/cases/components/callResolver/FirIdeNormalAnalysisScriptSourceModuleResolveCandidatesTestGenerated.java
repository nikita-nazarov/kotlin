/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test.cases.generated.cases.components.callResolver;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.analysis.api.fir.test.configurators.AnalysisApiFirTestConfiguratorFactory;
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData;
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator;
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind;
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.FrontendKind;
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisSessionMode;
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiMode;
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.callResolver.AbstractResolveCandidatesTest;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.analysis.api.GenerateAnalysisApiTestsKt}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("analysis/analysis-api/testData/components/resolver/singleByPsi")
@TestDataPath("$PROJECT_ROOT")
public class FirIdeNormalAnalysisScriptSourceModuleResolveCandidatesTestGenerated extends AbstractResolveCandidatesTest {
  @NotNull
  @Override
  public AnalysisApiTestConfigurator getConfigurator() {
    return AnalysisApiFirTestConfiguratorFactory.INSTANCE.createConfigurator(
      new AnalysisApiTestConfiguratorFactoryData(
        FrontendKind.Fir,
        TestModuleKind.ScriptSource,
        AnalysisSessionMode.Normal,
        AnalysisApiMode.Ide
      )
    );
  }

  @Test
  public void testAllFilesPresentInSingleByPsi() {
    KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("analysis/analysis-api/testData/components/resolver/singleByPsi"), Pattern.compile("^(.+)\\.kts$"), null, true);
  }

  @Nested
  @TestMetadata("analysis/analysis-api/testData/components/resolver/singleByPsi/assignments")
  @TestDataPath("$PROJECT_ROOT")
  public class Assignments {
    @Test
    public void testAllFilesPresentInAssignments() {
      KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("analysis/analysis-api/testData/components/resolver/singleByPsi/assignments"), Pattern.compile("^(.+)\\.kts$"), null, true);
    }
  }

  @Nested
  @TestMetadata("analysis/analysis-api/testData/components/resolver/singleByPsi/callableReferences")
  @TestDataPath("$PROJECT_ROOT")
  public class CallableReferences {
    @Test
    public void testAllFilesPresentInCallableReferences() {
      KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("analysis/analysis-api/testData/components/resolver/singleByPsi/callableReferences"), Pattern.compile("^(.+)\\.kts$"), null, true);
    }
  }

  @Nested
  @TestMetadata("analysis/analysis-api/testData/components/resolver/singleByPsi/invalidCode")
  @TestDataPath("$PROJECT_ROOT")
  public class InvalidCode {
    @Test
    public void testAllFilesPresentInInvalidCode() {
      KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("analysis/analysis-api/testData/components/resolver/singleByPsi/invalidCode"), Pattern.compile("^(.+)\\.kts$"), null, true);
    }
  }

  @Nested
  @TestMetadata("analysis/analysis-api/testData/components/resolver/singleByPsi/invokeOnObjects")
  @TestDataPath("$PROJECT_ROOT")
  public class InvokeOnObjects {
    @Test
    public void testAllFilesPresentInInvokeOnObjects() {
      KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("analysis/analysis-api/testData/components/resolver/singleByPsi/invokeOnObjects"), Pattern.compile("^(.+)\\.kts$"), null, true);
    }
  }

  @Nested
  @TestMetadata("analysis/analysis-api/testData/components/resolver/singleByPsi/nonCalls")
  @TestDataPath("$PROJECT_ROOT")
  public class NonCalls {
    @Test
    public void testAllFilesPresentInNonCalls() {
      KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("analysis/analysis-api/testData/components/resolver/singleByPsi/nonCalls"), Pattern.compile("^(.+)\\.kts$"), null, true);
    }
  }

  @Nested
  @TestMetadata("analysis/analysis-api/testData/components/resolver/singleByPsi/withTestCompilerPluginEnabled")
  @TestDataPath("$PROJECT_ROOT")
  public class WithTestCompilerPluginEnabled {
    @Test
    public void testAllFilesPresentInWithTestCompilerPluginEnabled() {
      KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("analysis/analysis-api/testData/components/resolver/singleByPsi/withTestCompilerPluginEnabled"), Pattern.compile("^(.+)\\.kts$"), null, true);
    }
  }
}
