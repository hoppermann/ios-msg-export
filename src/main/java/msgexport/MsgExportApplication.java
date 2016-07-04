package msgexport;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MsgExportApplication
{
  private static final Logger LOGGER = LoggerFactory.getLogger(MsgExportApplication.class);

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

        File contactBaseDir = new File(destDir, contact.getName());
        contactBaseDir.mkdirs();

        if(hasAttachements(messages))
        {
          File imageDestDir = new File(contactBaseDir, "images");
          if(!imageDestDir.exists())
          {
            imageDestDir.mkdirs();
          }
          else if(!imageDestDir.canWrite())
          {
            throw new IllegalArgumentException(String.format("can not to write dest image directory '%s/images'", args[1]));
          }

          appendAttachements(backupDir, imageDestDir, messages);
        }

        try
        {
          try (FileWriter fileWriter = new FileWriter(new File(contactBaseDir, String.format("%s.html", contact.getName()))))
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

  private void appendAttachements(File backupDir, File imageOutputDir, List<Message> messages)
  {
    messages.forEach(
      message ->
      {
        String attachmentFilename = message.getAttachmentFilename();
        if(attachmentFilename != null && !attachmentFilename.isEmpty())
        {
          String sha1 = Util.sha1(attachmentFilename.replace("~/Library/", "MediaDomain-Library/"));

          File imageFile = new File(backupDir, sha1);
          if(imageFile.exists() && imageFile.isFile() && imageFile.canRead())
          {
            try
            {
              // todo adjust images
              String imageFilename = getImageFilename(message, sha1);
              Files.copy(imageFile.toPath(), new File(imageOutputDir, imageFilename).toPath(), StandardCopyOption.REPLACE_EXISTING);
              message.setImage(imageOutputDir.getName() + "/" + imageFilename);
            }
            catch(IOException e)
            {
              LOGGER.error(String.format("can not copy image %s", sha1), e);
            }
          }
          else
          {
            LOGGER.info(String.format("can read file for attachment %s", attachmentFilename));
          }
        }
      });
  }

  private String getImageFilename(Message message, String name)
  {
    switch(message.getAttachmentMimetype())
    {
      case "image/jpeg":
        return name + ".jpeg";
      case "image/png":
        return name + ".png";
      default:
        return ".data";
    }
  }

  private List<Message> sortDate(Collection<Message> value)
  {
    List<Message> messages = new ArrayList<>(value);
    Collections.sort(messages, (o1, o2) -> o1.getDate().compareTo(o2.getDate()));
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
}
