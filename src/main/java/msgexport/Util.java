package msgexport;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Some small utility functions.
 */
public class Util
{
  private final static char[] hexArray = "0123456789abcdef".toCharArray();

  public static String hashFilename(String filename, String domain)
  {
    return sha1(filename);
  }

  public static String sha1(String text)
  {
    try
    {
      return bytesToHex(MessageDigest.getInstance("sha1").digest(text.getBytes()));
    }
    catch(NoSuchAlgorithmException e)
    {
      // ignore
    }

    return null;
  }

  private static String bytesToHex(byte[] bytes)
  {
    char[] hexChars = new char[bytes.length * 2];
    for(int j = 0; j < bytes.length; j++)
    {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = hexArray[v >>> 4];
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
  }
}
