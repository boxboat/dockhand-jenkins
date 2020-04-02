package com.boxboat.jenkins.test.library.config

import com.boxboat.jenkins.library.config.GlobalConfig
import com.boxboat.jenkins.library.git.GitConfig
import org.junit.Test

import static org.junit.Assert.assertEquals

class GitConfigTest {

    @Test
    void gitConfigTest() throws Exception {
        def global = new GlobalConfig().newFromYaml('''
git:
  buildVersionsUrl: git@github.com:boxboat/build-versions.git
  credential: git
  email: jenkins@boxboat.com
  remotePathRegex: github\\.com[:\\/]boxboat\\/(.*)\\.git$
  remoteUrlReplace: git@github.com:boxboat/{{ path }}.git
  branchUrlReplace: https://github.com/boxboat/{{ path }}/tree/{{ branch }}
  commitUrlReplace: https://github.com/boxboat/{{ path }}/commit/{{ hash }}
  gitAlternateMap:
    gitlab:
      remotePathRegex: gitlab\\.com[:\\/]boxboat\\/(.*)\\.git$
      remoteUrlReplace: git@gitlab.com:boxboat/{{ path }}.git
      branchUrlReplace: https://gitlab.com/boxboat/{{ path }}/tree/{{ branch }}
      commitUrlReplace: https://gitlab.com/boxboat/{{ path }}/commit/{{ hash }}
''')
        def rootPath = global.getGitRemotePath(null, "https://github.com/boxboat/path.git")
        assertEquals("path", rootPath)
        assertEquals("git@github.com:boxboat/path.git", global.getGitRemoteUrl(rootPath))
        def selectedPath = global.getGitRemotePath("gitlab", "https://gitlab.com/boxboat/path.git")
        assertEquals("gitlab:path", selectedPath)
        assertEquals("git@gitlab.com:boxboat/path.git", global.getGitRemoteUrl(selectedPath))
    }

}
