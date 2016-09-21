package msgexport;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MsgExportApplication
{
  private static final Logger LOGGER = LoggerFactory.getLogger(MsgExportApplication.class);

  private static final int IMAGE_MAX_WIDTH = 600;

  public static void main(String[] args)
    throws Exception
  {
    MsgExportApplication msgExportApplication = new MsgExportApplication();
    try
    {
      msgExportApplication.export(args);
    }
    catch(Exception e)
    {
      LOGGER.error(e.getMessage(), e);
    }
  }

  public void export(String[] args)
    throws Exception
  {
    // load sqlite driver
    Class.forName("org.sqlite.JDBC");

    if(args.length < 3)
    {
      throw new IllegalArgumentException("missing application parameter. usage: backup-dir country-code area-code [export-dir]");
    }

    // iOS backup directory
    File backupDir = new File(args[0]);
    if(!(backupDir.exists() && backupDir.isDirectory() && backupDir.canRead()))
    {
      throw new IllegalArgumentException(String.format("can not read user backup directory '%s'", args[0]));
    }

    String countryCode = args[1];
    String areaCode = args[2];

    File destDir = new File(args.length > 3 ? args[3] : ".");
    if(!(destDir.exists() && destDir.isDirectory() && destDir.canWrite()))
    {
      throw new IllegalArgumentException(String.format("can not to write dest directory '%s'", args[1]));
    }

    // setup freemarker template engine
    Configuration cfg = new Configuration(Configuration.VERSION_2_3_23);
    cfg.setClassLoaderForTemplateLoading(this.getClass().getClassLoader(), "templates");
    cfg.setDefaultEncoding("UTF-8");
    Template template = cfg.getTemplate("ioback.ftl");

    ContactBuilder contactBuilder = new ContactBuilder(countryCode, areaCode);

    for(Contact contact : contactBuilder.readContacts(backupDir))
    {
      List<Message> messages = MessageBuilder.readMessages(backupDir, contact);

      if(!messages.isEmpty())
      {
        LOGGER.info(String.format("export %s", contact.getName()));

        if(hasAttachements(messages))
        {
          appendAttachements(backupDir, messages);
        }

        try
        {
          try (FileWriter fileWriter = new FileWriter(new File(destDir, String.format("%s.html", contact.getName()))))
          {
            template.process(new ContactMessages(contact, sortDate(messages)), fileWriter);
          }
        }
        catch(Exception e)
        {
          LOGGER.warn(String.format("can not render message file for %s", contact.getName()), e);
        }
      }
    }
  }

  private boolean hasAttachements(List<Message> messages)
  {
    return messages.stream().anyMatch(message -> message.getAttachmentFilename() != null);
  }

  private void appendAttachements(File backupDir, List<Message> messages)
  {
    messages.forEach(
      message ->
      {
        String attachmentFilename = message.getAttachmentFilename();
        if(attachmentFilename != null && !attachmentFilename.isEmpty())
        {
          String sha1 = Util.sha1(attachmentFilename.replace("~/Library/", "MediaDomain-Library/"));

          File imageFile = new File(backupDir, Util.createFilename(sha1));
          if(imageFile.exists() && imageFile.isFile() && imageFile.canRead())
          {
            try
            {
              ImageData imageData = getImageData(imageFile);
              if(imageData != null)
              {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                if(imageData.getImage().getWidth() <= IMAGE_MAX_WIDTH && imageData.getOrientation() == 1)
                {
                  Files.copy(imageFile.toPath(), byteArrayOutputStream);
                }
                else
                {
                  BufferedImage image = imageData.getImage();

                  // rotate to normal image orientation
                  for(Scalr.Rotation rotation : calcRotations(imageData.getOrientation()))
                  {
                    image = Scalr.rotate(image, rotation);
                  }
                  // resize image to fit in HTML page and reduce size
                  if(imageData.getImage().getWidth() > IMAGE_MAX_WIDTH)
                  {
                    image = Scalr.resize(image, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.FIT_TO_WIDTH, IMAGE_MAX_WIDTH);
                  }

                  ImageIO.write(image, getImageInformalName(message.getAttachmentMimetype()), byteArrayOutputStream);
                }

                // store new image data as inline data
                message.setImageData(createImageData(message.getAttachmentMimetype(), byteArrayOutputStream.toByteArray()));
              }
              else
              {
                // todo handle some other data types
              }
            }
            catch(IOException e)
            {
              LOGGER.error(String.format("can not copy image %s", sha1), e);
            }
            catch(ImageProcessingException e)
            {
              LOGGER.error("", e); // todo need more information
            }
          }
          else
          {
            LOGGER.info(String.format("can read file for attachment %s", attachmentFilename));
          }
        }
      });
  }

  private String createImageData(String mimetype, byte[] image)
  {
    return String.format("data:%s;base64,%s", mimetype, Base64.getEncoder().encodeToString(image));
  }

  private String getImageInformalName(String mimetype)
  {
    switch(mimetype.toLowerCase())
    {
      case "image/png":
        return "png";
      case "image/jpeg":
      default:
        return "jpeg";
    }
  }

  /**
   * calculate needed rotations
   *
   * @param orientation the exif image ortation info
   *
   * @return an array with needed rotations
   */
  private Scalr.Rotation[] calcRotations(int orientation)
  {
    switch(orientation)
    {
      default:
      case 1:
        return new Scalr.Rotation[]{};
      case 2:
        return new Scalr.Rotation[]{Scalr.Rotation.FLIP_HORZ};
      case 3:
        return new Scalr.Rotation[]{Scalr.Rotation.CW_180};
      case 4:
        return new Scalr.Rotation[]{Scalr.Rotation.FLIP_VERT};
      case 5:
        return new Scalr.Rotation[]{Scalr.Rotation.FLIP_HORZ, Scalr.Rotation.CW_270};
      case 6:
        return new Scalr.Rotation[]{Scalr.Rotation.CW_90};
      case 7:
        return new Scalr.Rotation[]{Scalr.Rotation.FLIP_HORZ, Scalr.Rotation.CW_90};
      case 8:
        return new Scalr.Rotation[]{Scalr.Rotation.CW_270};
    }
  }

  private ImageData getImageData(File imageFile)
    throws ImageProcessingException, IOException
  {
    BufferedImage image = ImageIO.read(imageFile);
    if(image != null)
    {
      int orientation = 1; // normal, no rotation needed
      Metadata metadata = ImageMetadataReader.readMetadata(imageFile.getAbsoluteFile());
      ExifIFD0Directory exifIFD0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
      if(exifIFD0Directory != null && exifIFD0Directory.getInteger(ExifIFD0Directory.TAG_ORIENTATION) != null)
      {
        orientation = Math.min(Math.max(exifIFD0Directory.getInteger(ExifIFD0Directory.TAG_ORIENTATION), 1), 8);
      }

      return new ImageData(image, orientation);
    }

    return null;
  }

  private List<Message> sortDate(Collection<Message> value)
  {
    List<Message> messages = new ArrayList<>(value);
    Collections.sort(messages, Comparator.comparing(Message::getDate));
    return messages;
  }

  public static class ContactMessages
  {
    private Contact contact;
    private List<Message> messages;

    public ContactMessages(Contact contact, List<Message> messages)
    {
      this.contact = contact;
      this.messages = messages;
    }

    public Contact getContact()
    {
      return contact;
    }

    public List<Message> getMessages()
    {
      return messages;
    }
  }

  private static class ImageData
  {
    private final BufferedImage image;
    private final int orientation;

    public ImageData(BufferedImage image, int orientation)
    {
      this.image = image;
      this.orientation = orientation;
    }

    public BufferedImage getImage()
    {
      return image;
    }

    public int getOrientation()
    {
      return orientation;
    }
  }
}
