// vim: bg=dark sw=2
package se.soy.cryptobice;
import java.security.Key;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import java.security.KeyStore.SecretKeyEntry;
import javax.crypto.SecretKey;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Cipher;
import sun.security.pkcs11.SunPKCS11;
import sun.misc.BASE64Encoder; // FIXME

public class App {
  public static void main(String[] args) throws Exception {
    // Set up the Sun PKCS 11 provider
    // String configName = "Z:\\SOFTHSM_INSTALL\\etc\\softhsm2.conf";

    String configName = "/root/softhsm2.cfg";

    Provider p = new SunPKCS11(configName);

    if (-1 == Security.addProvider(p)) {
      throw new RuntimeException("could not add security provider");
    }

    // Load the key store
    char[] pin = "1234".toCharArray();
    KeyStore keyStore = KeyStore.getInstance("PKCS11", p);
    keyStore.load(null, pin);

    // AES key
    KeyStore.ProtectionParameter protParam =
      new KeyStore.PasswordProtection("1234".toCharArray());

    KeyGenerator kg = KeyGenerator.getInstance("AES");
    kg.init(128);
    SecretKey signingKey = kg.generateKey();
    System.out.println(signingKey);

    KeyStore.SecretKeyEntry skEntry =
      new KeyStore.SecretKeyEntry(signingKey);

    keyStore.setEntry("AA", skEntry, protParam);

    keyStore.store(null);

    Cipher aesCipher = Cipher.getInstance("AES");
    aesCipher.init(Cipher.ENCRYPT_MODE, signingKey);

    String strDataToEncrypt = "Hello World of Encryption using AES";
    byte[] byteCipherText = aesCipher.doFinal(strDataToEncrypt.getBytes());
    System.out.println("Cipher Text generated using AES is " + new BASE64Encoder().encode(byteCipherText));

    KeyStore.SecretKeyEntry secretKeyEntry = (KeyStore.SecretKeyEntry)keyStore.getEntry("AA", protParam);
    SecretKey secretKey = secretKeyEntry.getSecretKey();
    System.out.println(secretKey);

    aesCipher.init(Cipher.DECRYPT_MODE,secretKey,aesCipher.getParameters());
    byte[] byteDecryptedText = aesCipher.doFinal(byteCipherText);
    String strDecryptedText = new String(byteDecryptedText);
    System.out.println("Decrypted Text message is " + strDecryptedText);
  }
}