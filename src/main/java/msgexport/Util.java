package msgexport;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Some small utility functions.
 */
public class Util
{
  private final static char[] hexArray = "0123456789abcdef".toCharArray();

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

  public static String createFilename(String sha1)
  {
    // ios10 prefix sha1 filename
    return sha1.substring(0, 2) + "/" + sha1;
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
