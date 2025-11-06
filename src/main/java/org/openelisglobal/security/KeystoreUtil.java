package org.openelisglobal.security;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import org.springframework.core.io.Resource;

public class KeystoreUtil {

    public static KeyStore readKeyStoreFile(Resource resource, char[] keystorePwd)
            throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        FileInputStream fis;
        try {
            fis = new FileInputStream(resource.getFile());
        } catch (FileNotFoundException e) {
            return null;
        }
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(fis, keystorePwd);
        return keyStore;
    }

    public static PrivateKey getPrivateKeyFromKeyStore(KeyStore keyStore, char[] keystorePwd, String keyAlias,
            char[] keyPwd) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException,
            UnrecoverableKeyException {
        return (PrivateKey) keyStore.getKey(keyAlias, keyPwd);
    }

    public static PrivateKey getPrivateKeyFromKeyStore(KeyStore keyStore, char[] keyPwd) throws CertificateException,
            NoSuchAlgorithmException, KeyStoreException, IOException, UnrecoverableKeyException {
        String alias = keyStore.aliases().nextElement();
        return (PrivateKey) keyStore.getKey(alias, keyPwd);
    }

    public static Certificate getCertFromKeyStore(KeyStore keyStore, String keyAlias) throws CertificateException,
            NoSuchAlgorithmException, KeyStoreException, IOException, UnrecoverableKeyException {
        return keyStore.getCertificate(keyAlias);
    }

    public static Certificate getCertFromKeyStore(KeyStore keyStore, char[] keyPwd) throws CertificateException,
            NoSuchAlgorithmException, KeyStoreException, IOException, UnrecoverableKeyException {
        String alias = keyStore.aliases().nextElement();
        return keyStore.getCertificate(alias);
    }

    public static KeyCertPair getKeyCertFromKeystore(KeyStore keyStore, char[] keystorePwd, String keyAlias,
            char[] keyPwd) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException,
            UnrecoverableKeyException {
        String alias = keyStore.aliases().nextElement();
        return new KeyCertPair((PrivateKey) keyStore.getKey(alias, keyPwd), keyStore.getCertificate(alias));
    }

    public static KeyCertPair getKeyCertFromKeystore(KeyStore keyStore, char[] keystorePwd) throws CertificateException,
            NoSuchAlgorithmException, KeyStoreException, IOException, UnrecoverableKeyException {
        String alias = keyStore.aliases().nextElement();
        return new KeyCertPair((PrivateKey) keyStore.getKey(alias, keystorePwd), keyStore.getCertificate(alias));
    }

    public static class KeyCertPair {

        private PrivateKey key;
        private Certificate cert;

        public KeyCertPair(PrivateKey key, Certificate cert) {
            this.key = key;
            this.cert = cert;
        }

        public PrivateKey getKey() {
            return key;
        }

        public Certificate getCert() {
            return cert;
        }
    }
}
