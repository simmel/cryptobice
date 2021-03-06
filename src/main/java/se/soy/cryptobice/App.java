// vim: bg=dark sw=2 et
package se.soy.cryptobice;
import java.security.Key;
import java.security.KeyStore;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.KeyStore.SecretKeyEntry;
import javax.crypto.SecretKey;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.Cipher;
import sun.security.pkcs11.SunPKCS11;
import sun.misc.BASE64Encoder; // FIXME
import java.io.InputStream;
/*
https://community.oracle.com/thread/2583861
https://docs.oracle.com/javase/9/security/pkcs11-reference-guide1.htm
*/

public class App {
  public static void main(String[] args) throws Exception {
    InputStream configName = App.class.getClassLoader().getResourceAsStream("softhsm2.cfg");

    // Set up the Sun PKCS 11 provider
    Provider p = new SunPKCS11(configName);
    // Java 9+
    // Provider p = Security.getProvider("SunPKCS11");
    // p = p.configure(configName);

    // Load the key store
    char[] pin = "1234".toCharArray();
    KeyStore keyStore = KeyStore.getInstance("PKCS11", p);
    keyStore.load(null, pin);

    // Instantiate SecureRandom
    SecureRandom secureRandom = new SecureRandom().getInstance("NativePRNGNonBlocking");

    // AES key
    byte[] salt = new byte[64];
    secureRandom.nextBytes(salt);

    KeyStore.ProtectionParameter protParam =
      new KeyStore.PasswordProtection(
          "1234".toCharArray(),
          "PBKDF2WithHmacSHA512",
          new PBEParameterSpec(
            salt,
            100_000_000
            )
          );

    KeyGenerator kg = KeyGenerator.getInstance("AES", p);
    kg.init(128, secureRandom);
    SecretKey signingKey = kg.generateKey();
    System.out.println(signingKey);

    KeyStore.SecretKeyEntry skEntry =
      new KeyStore.SecretKeyEntry(signingKey);

    keyStore.setEntry("AA", skEntry, protParam);

    keyStore.store(null);

    Cipher aesCipher = Cipher.getInstance("AES", p);
    aesCipher.init(Cipher.ENCRYPT_MODE, signingKey, secureRandom);

    String strDataToEncrypt = "Hello World of Encryption using AES";
    byte[] byteCipherText = aesCipher.doFinal(strDataToEncrypt.getBytes());
    System.out.println("Cipher Text generated using AES is " + new BASE64Encoder().encode(byteCipherText));

    KeyStore.SecretKeyEntry secretKeyEntry = (KeyStore.SecretKeyEntry)keyStore.getEntry("AA", protParam);
    SecretKey secretKey = secretKeyEntry.getSecretKey();
    System.out.println(secretKey);

    aesCipher.init(Cipher.DECRYPT_MODE, secretKey, aesCipher.getParameters(), secureRandom);
    byte[] byteDecryptedText = aesCipher.doFinal(byteCipherText);
    String strDecryptedText = new String(byteDecryptedText);
    System.out.println("Decrypted Text message is " + strDecryptedText);
  }
}
