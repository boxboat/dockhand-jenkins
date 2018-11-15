package com.boxboat.jenkins.test.pipeline

import com.lesfurets.jenkins.unit.BasePipelineTest
import static com.lesfurets.jenkins.unit.global.lib.LibraryConfiguration.library
import static com.lesfurets.jenkins.unit.global.lib.LocalSource.localSource

abstract class PipelineBase extends BasePipelineTest {

    final String scriptBase = 'test-resources/com/boxboat/jenkins/test/pipeline/'

    void setUp() throws Exception {
        super.setUp()
        helper.registerAllowedMethod('file', [Map.class], null)
        helper.registerAllowedMethod('libraryResource', [String.class], { fileName ->
            return new File("resources/${fileName}").getText('Utf8')
        })
        helper.registerAllowedMethod('libraryResource', [Map.class], { config ->
            return config.resource
        })
        helper.registerAllowedMethod('httpRequest', [Map.class], null)
        helper.registerAllowedMethod('writeFile', [Map.class], null)
        helper.registerAllowedMethod('sshUserPrivateKey', [Map.class], null)
        helper.registerAllowedMethod('throttle', [List.class, Closure.class], {
            category, next -> next()
        })
        helper.registerAllowedMethod('usernamePassword', [Map.class], null)

        binding.setVariable('env', [
                'sshKey'     : 'sshKey',
                'username'   : 'username',
                'BRANCH_NAME': 'master',
                'WORKSPACE'  : System.getProperty('java.io.tmpdir'),
        ])
        binding.setVariable('params', [
                'tagCommitHash': '0123456789abcdef0123456789abcdef',
                'testEnv'      : 'true'
        ])
        binding.setVariable('scm', [:])

        def sharedLib = 'dist'
        def library = library()
                .name('jenkins-shared-library')
                .retriever(localSource(sharedLib))
                .targetPath(sharedLib)
                .defaultVersion('jenkins')
                .allowOverride(true)
                .implicit(false)
                .build()
        helper.registerSharedLibrary(library)
    }

}
