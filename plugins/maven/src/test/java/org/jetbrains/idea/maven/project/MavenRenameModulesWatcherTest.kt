// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.idea.maven.project

import com.intellij.maven.testFramework.MavenDomTestCase
import com.intellij.openapi.application.readAction
import kotlinx.coroutines.runBlocking
import org.junit.Test

class MavenRenameModulesWatcherTest : MavenDomTestCase() {
  override fun runInDispatchThread() = false

  override fun setUp() {
    super.setUp()
    projectsManager.initForTests()
    projectsManager.listenForExternalChanges()
  }

  @Test
  fun testModuleRenameArtifactIdChanged() = runBlocking {
    importProject("""
                  <groupId>group</groupId>
                  <artifactId>module</artifactId>
                  <version>1</version>
                  """.trimIndent())
    val oldModuleName = "module"
    val newModuleName = "newModule"
    renameModule(oldModuleName, newModuleName)
    readAction {
      val tag = findTag("project.artifactId")
      assertEquals(newModuleName, tag.getValue().getText())
    }
  }

  @Test
  fun testModuleRenameImplicitGroupIdArtifactIdChanged() = runBlocking {
    createProjectPom("""
                  <groupId>group</groupId>
                  <artifactId>parent</artifactId>
                  <version>1</version>
                  <packaging>pom</packaging>
                  <modules>
                    <module>m1</module>
                  </modules>
                  """.trimIndent())
    val m1File = createModulePom("m1", """
                  <artifactId>m1</artifactId>
                  <version>1</version>
                  <parent>
                    <groupId>group</groupId>
                    <artifactId>parent</artifactId>
                  </parent>
                  """.trimIndent())
    importProject()
    val oldModuleName = "m1"
    val newModuleName = "m1new"
    renameModule(oldModuleName, newModuleName)
    readAction {
      val tag = findTag(m1File, "project.artifactId")
      assertEquals(newModuleName, tag.getValue().getText())
    }
  }

  @Test
  fun testModuleRenameParentChanged() = runBlocking {
    createProjectPom("""
                  <groupId>group</groupId>
                  <artifactId>parent</artifactId>
                  <version>1</version>
                  <packaging>pom</packaging>
                  <modules>
                    <module>m1</module>
                  </modules>
                  """.trimIndent())
    val m1File = createModulePom("m1", """
                  <artifactId>m1</artifactId>
                  <version>1</version>
                  <parent>
                    <groupId>group</groupId>
                    <artifactId>parent</artifactId>
                  </parent>
                  """.trimIndent())
    importProject()
    val oldModuleName = "parent"
    val newModuleName = "newParent"
    renameModule(oldModuleName, newModuleName)
    readAction {
      val tag = findTag(m1File, "project.parent.artifactId")
      assertEquals(newModuleName, tag.getValue().getText())
    }
  }

  @Test
  fun testModuleRenameDependenciesChanged() = runBlocking {
    createProjectPom("""
                  <groupId>group</groupId>
                  <artifactId>parent</artifactId>
                  <version>1</version>
                  <packaging>pom</packaging>
                  <modules>
                    <module>m1</module>
                    <module>m2</module>
                  </modules>
                  """.trimIndent())
    val m1File = createModulePom("m1", """
                  <artifactId>m1</artifactId>
                  <version>1</version>
                  <parent>
                    <groupId>group</groupId>
                    <artifactId>parent</artifactId>
                  </parent>
                  """.trimIndent())
    val m2File = createModulePom("m2", """
                  <artifactId>m2</artifactId>
                  <version>1</version>
                  <parent>
                    <groupId>group</groupId>
                    <artifactId>parent</artifactId>
                  </parent>
                  <dependencies>
                    <dependency>
                      <version>1</version>
                      <groupId>group</groupId>
                      <artifactId>m1</artifactId>
                    </dependency>
                  </dependencies>
                  """.trimIndent())
    importProject()
    val oldModuleName = "m1"
    val newModuleName = "m1new"
    renameModule(oldModuleName, newModuleName)
    readAction {
      val tag = findTag(m2File, "project.dependencies.dependency.artifactId")
      assertEquals(newModuleName, tag.getValue().getText())
    }
  }

  @Test
  fun testModuleRenameDependencyManagementChanged() = runBlocking {
    createProjectPom("""
                  <groupId>group</groupId>
                  <artifactId>parent</artifactId>
                  <version>1</version>
                  <packaging>pom</packaging>
                  <modules>
                    <module>m1</module>
                    <module>m2</module>
                  </modules>
                  """.trimIndent())
    val m1File = createModulePom("m1", """
                  <artifactId>m1</artifactId>
                  <version>1</version>
                  <parent>
                    <groupId>group</groupId>
                    <artifactId>parent</artifactId>
                  </parent>
                  """.trimIndent())
    val m2File = createModulePom("m2", """
                  <artifactId>m2</artifactId>
                  <version>1</version>
                  <parent>
                    <groupId>group</groupId>
                    <artifactId>parent</artifactId>
                  </parent>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <version>1</version>
                        <groupId>group</groupId>
                        <artifactId>m1</artifactId>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                  """.trimIndent())
    importProject()
    val oldModuleName = "m1"
    val newModuleName = "m1new"
    renameModule(oldModuleName, newModuleName)
    readAction {
      val tag = findTag(m2File, "project.dependencyManagement.dependencies.dependency.artifactId")
      assertEquals(newModuleName, tag.getValue().getText())
    }
  }

  @Test
  fun testModuleRenameExclusionsChanged() = runBlocking {
    createProjectPom("""
                  <groupId>group</groupId>
                  <artifactId>parent</artifactId>
                  <version>1</version>
                  <packaging>pom</packaging>
                  <modules>
                    <module>m1</module>
                    <module>m2</module>
                  </modules>
                  """.trimIndent())
    val m1File = createModulePom("m1", """
                  <groupId>group</groupId>
                  <artifactId>m1</artifactId>
                  <version>1</version>
                  <parent>
                    <groupId>group</groupId>
                    <artifactId>parent</artifactId>
                  </parent>
                  """.trimIndent())
    val m2File = createModulePom("m2", """
                  <groupId>group</groupId>
                  <artifactId>m2</artifactId>
                  <version>1</version>
                  <parent>
                    <version>1</version>
                    <groupId>group</groupId>
                    <artifactId>parent</artifactId>
                  </parent>
                  <dependencies>
                    <dependency>
                      <version>1</version>
                      <groupId>group</groupId>
                      <artifactId>m1</artifactId>
                    </dependency>
                  </dependencies>
                  """.trimIndent())
    val m3File = createModulePom("m2", """
                  <groupId>group</groupId>
                  <artifactId>m3</artifactId>
                  <version>1</version>
                  <parent>
                    <version>1</version>
                    <groupId>group</groupId>
                    <artifactId>parent</artifactId>
                  </parent>
                  <dependencies>
                    <dependency>
                      <version>1</version>
                      <groupId>group</groupId>
                      <artifactId>m2</artifactId>
                      <exclusions>
                        <exclusion>
                        <groupId>group</groupId>
                        <artifactId>m1</artifactId>
                        </exclusion>
                      </exclusions>
                    </dependency>
                  </dependencies>
                  """.trimIndent())
    importProject()
    val oldModuleName = "m1"
    val newModuleName = "m1new"
    renameModule(oldModuleName, newModuleName)
    readAction {
      val tag = findTag(m3File, "project.dependencies.dependency.exclusions.exclusion.artifactId")
      assertEquals(newModuleName, tag.getValue().getText())
    }
  }

  @Test
  fun testModuleRenameAnotherGroupArtifactIdNotChanged() = runBlocking {
    createProjectPom("""
                  <groupId>group</groupId>
                  <artifactId>parent</artifactId>
                  <version>1</version>
                  <packaging>pom</packaging>
                  <modules>
                    <module>m1</module>
                    <module>m2</module>
                  </modules>
                  """.trimIndent())
    val m1File = createModulePom("m1", """
                  <groupId>group1</groupId>
                  <artifactId>m1</artifactId>
                  <version>1</version>
                  <parent>
                    <version>1</version>
                    <groupId>group</groupId>
                    <artifactId>parent</artifactId>
                  </parent>
                  """.trimIndent())
    val m2File = createModulePom("m2", """
                  <groupId>group</groupId>
                  <artifactId>m2</artifactId>
                  <version>1</version>
                  <parent>
                    <version>1</version>
                    <groupId>group</groupId>
                    <artifactId>parent</artifactId>
                  </parent>
                  <dependencies>
                    <dependency>
                      <version>1</version>
                      <groupId>anotherGroup</groupId>
                      <artifactId>m1</artifactId>
                    </dependency>
                  </dependencies>
                  """.trimIndent())
    importProject()
    val oldModuleName = "m1"
    val newModuleName = "m1new"
    renameModule(oldModuleName, newModuleName)
    readAction {
      val tag = findTag(m2File, "project.dependencies.dependency.artifactId")
      assertEquals(oldModuleName, tag.getValue().getText())
    }
  }

  @Test
  fun test_when_ModuleMovedToGroup_then_ArtifactIdRemains() = runBlocking {
    importProject("""
                  <groupId>group</groupId>
                  <artifactId>module</artifactId>
                  <version>1</version>
                  """.trimIndent())
    val oldModuleName = "module"
    val newModuleName = "group.module"
    renameModule(oldModuleName, newModuleName)
    readAction {
      val tag = findTag("project.artifactId")
      assertEquals(oldModuleName, tag.getValue().getText())
    }
  }
}
