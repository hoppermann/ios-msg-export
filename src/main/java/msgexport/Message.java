package msgexport;

import java.util.Date;

public class Message
{
  private final String uniqueID;
  private final Date date;
  private final int type;
  private final String text;
  private final String attachmentFilename;
  private final String attachmentMimetype;
  private final boolean sms;
  private String imageData;

  public Message(String uniqueID, Date date, int type, String text, boolean sms)
  {
    this(uniqueID, date, type, text, sms, null, null);
  }

  public Message(String uniqueID, Date date, int type, String text, boolean sms, String attachmentFilename, String attachmentMimetype)
  {
    this.uniqueID = uniqueID;
    this.date = date;
    this.type = type;
    this.text = text;
    this.sms = sms;
    this.attachmentFilename = attachmentFilename;
    this.attachmentMimetype = attachmentMimetype;
  }

  public String getUniqueID()
  {
    return uniqueID;
  }

  public Date getDate()
  {
    return date;
  }

  public int getType()
  {
    return type;
  }

  public boolean isSms()
  {
    return sms;
  }

  public String getText()
  {
    return text;
  }

  public String getAttachmentFilename()
  {
    return attachmentFilename;
  }

  public String getAttachmentMimetype()
  {
    return attachmentMimetype;
  }

  public void setImageData(String imageData)
  {
    this.imageData = imageData;
  }

  public String getImageData()
  {
    return imageData;
  }
}
