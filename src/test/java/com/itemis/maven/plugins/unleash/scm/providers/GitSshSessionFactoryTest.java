package com.itemis.maven.plugins.unleash.scm.providers;

import static com.itemis.maven.plugins.unleash.scm.providers.GitSshSessionFactory.PREFERRED_AUTHENTICATIONS;
import static com.itemis.maven.plugins.unleash.scm.providers.GitSshSessionFactory.PUBLIC_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Hashtable;
import java.util.logging.Logger;

import com.google.common.base.Optional;
import com.itemis.maven.plugins.unleash.scm.ScmProviderInitialization;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository;

import org.eclipse.jgit.util.FS;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GitSshSessionFactoryTest {

    private boolean connectorAvailable;

    @Mock
    private FS mockFS;

    @Mock
    private ScmProviderInitialization mockInitialization;

    @Mock
    private Logger mockLogger;

    private GitSshSessionFactory sessionFactory;

    private JSch sshClient;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        // reset this before every tests b/c it's static :(
        JSch.setConfig(new Hashtable<Object, Object>());

        sessionFactory = new GitSshSessionFactory(mockInitialization, mockLogger) {
            @Override
            boolean isConnectorAvailable() {
                return connectorAvailable;
            }
        };
    }

    @Test
    public void testNoPassphraseOrAgent() throws Exception {
        givenNoPassphraseIsPresent();
        givenNoAgentConnectorIsAvailable();
        whenCreateSshClient();
        thenIdentityRepositoryIsLocal();
        thenPreferredAuthenticationIsNotPublicKey();
    }

    @Test
    public void testUsePassphrase() throws Exception {
        givenAPassphraseIsPresent();
        givenAgentConnectorAvailable();
        whenCreateSshClient();
        thenIdentityRepositoryIsLocal();
    }

    @Test
    public void testUseSshAgent() throws Exception {
        givenNoPassphraseIsPresent();
        givenAgentConnectorAvailable();
        whenCreateSshClient();
        thenPreferredAuthenticationIsPublicKey();
        thenIdentityRepositoryIsRemote();
    }

    private void givenAgentConnectorAvailable() {
        connectorAvailable = true;
    }

    private void givenAPassphraseIsPresent() {
        when(mockInitialization.getSshPrivateKeyPassphrase()).thenReturn(Optional.of("passphrase"));
    }

    private void givenNoAgentConnectorIsAvailable() {
        connectorAvailable = false;
    }

    private void givenNoPassphraseIsPresent() {
        when(mockInitialization.getSshPrivateKeyPassphrase()).thenReturn(Optional.<String> absent());
    }

    private void thenIdentityRepositoryIsLocal() {
        assertFalse(sshClient.getIdentityRepository() instanceof RemoteIdentityRepository);
    }

    private void thenIdentityRepositoryIsRemote() {
        assertTrue(sshClient.getIdentityRepository() instanceof RemoteIdentityRepository);
    }

    private void thenPreferredAuthenticationIsNotPublicKey() {
        // if this isn't explicitly set to 'publickey', no passphrase or agent were found
        assertNotEquals(PUBLIC_KEY, JSch.getConfig(PREFERRED_AUTHENTICATIONS));
    }

    private void thenPreferredAuthenticationIsPublicKey() {
        assertEquals(PUBLIC_KEY, JSch.getConfig(PREFERRED_AUTHENTICATIONS));
    }

    private void whenCreateSshClient() throws Exception {
        when(mockFS.userHome()).thenReturn(null);
        sshClient = sessionFactory.createDefaultJSch(mockFS);
    }
}
