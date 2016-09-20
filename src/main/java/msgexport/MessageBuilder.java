package msgexport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MessageBuilder
{
  private static final Logger LOGGER = LoggerFactory.getLogger(MessageBuilder.class);
  private static final String SMS_DB = Util.sha1("HomeDomain-Library/SMS/sms.db");

  public static List<Message> readMessages(File backupDir, Contact contact)
    throws SQLException
  {
    File smsBackupDB = new File(backupDir, Util.createFilename(SMS_DB));

    if(smsBackupDB.exists() && smsBackupDB.isFile() && smsBackupDB.canRead())
    {
      try (Connection connection = DriverManager.getConnection(String.format("jdbc:sqlite:%s", smsBackupDB.getAbsolutePath())))
      {
        List<Message> messages = new ArrayList<>();
        try
        {
          try (Statement statement = connection.createStatement())
          {
            // build ugly sql query cause sqlite do not implement PreparedStatement.setArray()
            String sql = "SELECT"
                         + "  h.id AS UniqueID,"
                         + "  m.is_from_me AS Type,"
                         + "  m.date AS Date,"
                         + "  m.text AS Text,"
                         + "  a.filename AS AttachementFilename,"
                         + "  a.mime_type AS AttachementMimetype"
                         + " FROM message m"
                         + "  LEFT JOIN handle h ON h.rowid = m.handle_id"
                         + "  LEFT JOIN message_attachment_join maj ON maj.message_id = m.rowid"
                         + "  LEFT JOIN attachment a ON maj.attachment_id = a.rowid"
                         + " WHERE h.id in (" + createEmailAndPhoneLst(contact) + ")"
                         + " ORDER BY Date";
            if(statement.execute(sql))
            {
              try (ResultSet resultSet = statement.getResultSet())
              {
                while(resultSet.next())
                {
                  String uniqueID = resultSet.getString("UniqueID");
                  int type = resultSet.getInt("Type");
                  long date = resultSet.getLong("Date");
                  String text = getUTF8(resultSet.getBytes("Text"));
                  String attachementFilename = resultSet.getString("AttachementFilename");
                  String attachementMimetype = resultSet.getString("AttachementMimetype");

                  if(uniqueID != null && text != null && !text.isEmpty())
                  {
                    Message message;
                    boolean isSms = uniqueID.startsWith("+");
                    if(attachementFilename != null && !attachementFilename.isEmpty()
                       && attachementMimetype != null && attachementMimetype.startsWith("image/"))
                    {
                      message = new Message(uniqueID, getDate(date), type, text, isSms, attachementFilename, attachementMimetype);
                    }
                    else
                    {
                      message = new Message(uniqueID, getDate(date), type, text, isSms);
                    }
                    messages.add(message);
                  }
                }
              }
            }
          }
        }
        catch(SQLException e)
        {
          LOGGER.error(String.format("can not access SQLite DB %s", SMS_DB), e);
        }

        return messages;
      }
    }
    else
    {
      throw new IllegalArgumentException(String.format("can not read from %s", smsBackupDB.getAbsolutePath()));
    }
  }

  private static String createEmailAndPhoneLst(Contact contact)
  {
    StringBuilder builder = new StringBuilder();
    String sep = "";
    for(String email : contact.getEmails())
    {
      builder.append(sep).append('"').append(email).append('"');
      sep = ",";
    }
    for(Long phone : contact.getPhones())
    {
      // phones need a '+' prefix
      builder.append(sep).append('"').append('+').append(phone).append('"');
      sep = ",";
    }

    return builder.toString();
  }

  private static String getUTF8(byte[] text)
  {
    if(text != null)
    {
      try
      {
        return new String(text, "UTF-8");
      }
      catch(UnsupportedEncodingException e)
      {
        // ignore
      }
    }

    return null;
  }

  private static Date getDate(long date)
  {
    // seconds since 01-01-2001
    return new Date((date + 978307200L) * 1000L);
  }
}
