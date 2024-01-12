// Copyright 2023 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

/*
Package python
Any Python application that can run through the rspec CLI
should be able to use this to perform test intelligence.

Test filtering:
rspec test
*/
package ruby

import (
	"context"
	"fmt"
	"path/filepath"
	"strings"

	"github.com/harness/harness-core/commons/go/lib/exec"
	"github.com/harness/harness-core/commons/go/lib/filesystem"
	"github.com/harness/harness-core/commons/go/lib/utils"
	"github.com/harness/ti-client/types"
	"go.uber.org/zap"
)

var (
	rspecCmd = "bundle exec rspec"
)

type rspecRunner struct {
	fs                filesystem.FileSystem
	log               *zap.SugaredLogger
	cmdContextFactory exec.CmdContextFactory
	agentPath         string
	testGlobs         []string
}

func NewRspecRunner(log *zap.SugaredLogger, fs filesystem.FileSystem, factory exec.CmdContextFactory, agentPath string, testGlobs []string) *rspecRunner {
	return &rspecRunner{
		fs:                fs,
		log:               log,
		cmdContextFactory: factory,
		agentPath:         agentPath,
		testGlobs:         testGlobs,
	}
}

func (b *rspecRunner) AutoDetectPackages() ([]string, error) {
	return []string{}, nil
}

func (b *rspecRunner) AutoDetectTests(ctx context.Context, testGlobs []string) ([]types.RunnableTest, error) {
	if len(testGlobs) == 0 {
		testGlobs = utils.RUBY_TEST_PATTERN
	}
	return utils.GetTestsFromLocal(testGlobs, "rb", utils.LangType_RUBY)
}

func (b *rspecRunner) ReadPackages(files []types.File) []types.File {
	return files
}

func (b *rspecRunner) GetCmd(ctx context.Context, tests []types.RunnableTest, userArgs, agentConfigPath string, ignoreInstr, runAll bool) (string, error) {
	testCmd := ""
	tiFlag := "TI=1"
	installReportCmd := ""
	installAgentCmd := ""

	repoPath := filepath.Join(b.agentPath, "harness", "ruby-agent")
	if !ignoreInstr {
		installAgentCmd = fmt.Sprintf("bundle add harness_ruby_agent --path %q --version %q || true;", repoPath, "0.0.1")
		err := WriteHelperFile(repoPath)
		if err != nil {
			b.log.Errorw("Unable to write rspec helper file automatically", err)
		}
	}

	if userArgs == "" {
		installReportCmd = "bundle add rspec_junit_formatter || true;"
		userArgs = fmt.Sprintf("--format RspecJunitFormatter --out %s${HARNESS_NODE_INDEX}", utils.HarnessDefaultReportPath)
	}
	// Run all the tests
	if runAll {
		rspecGlob := ""
		if len(b.testGlobs) > 0 {
			rspecGlob = strings.Join(b.testGlobs, " ")
		}
		if ignoreInstr {
			return strings.TrimSpace(fmt.Sprintf("%s %s %s %s", installReportCmd, rspecCmd, userArgs, rspecGlob)), nil
		}
		testCmd = strings.TrimSpace(fmt.Sprintf("%s %s %s %s %s %s",
			installReportCmd, installAgentCmd, tiFlag, rspecCmd, userArgs, rspecGlob))
		return testCmd, nil
	}

	if len(tests) == 0 {
		return "echo \"Skipping test run, received no tests to execute\"", nil
	}

	ut := utils.GetUniqueTestStrings(tests)
	testStr := strings.Join(ut, " ")

	if ignoreInstr {
		return strings.TrimSpace(fmt.Sprintf("%s %s %s %s", installAgentCmd, rspecCmd, userArgs, testStr)), nil
	}

	testCmd = fmt.Sprintf("%s %s %s %s %s %s",
		installReportCmd, installAgentCmd, tiFlag, rspecCmd, userArgs, testStr)
	return testCmd, nil
}
