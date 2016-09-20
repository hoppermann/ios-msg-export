package msgexport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.LongConsumer;

public class ContactBuilder
{
  private static final Logger LOGGER = LoggerFactory.getLogger(ContactBuilder.class);
  private static final String CONTACT_DB = Util.sha1("HomeDomain-Library/AddressBook/AddressBook.sqlitedb");

  private final String countryPhoneCode;
  private final String areaPhoneCode;

  public ContactBuilder(String countryPhoneCode, String areaPhoneCode)
  {
    this.countryPhoneCode = countryPhoneCode;
    this.areaPhoneCode = areaPhoneCode;
  }

  public Set<Contact> readContacts(File backupDir)
    throws SQLException
  {
    File contactBackupDB = new File(backupDir, Util.createFilename(CONTACT_DB));
    if(contactBackupDB.exists() && contactBackupDB.isFile() && contactBackupDB.canRead())
    {
      Map<String, Contact> contactMap = new HashMap<>();

      try (Connection connection = DriverManager.getConnection(String.format("jdbc:sqlite:%s", contactBackupDB.getAbsolutePath())))
      {
        try (Statement statement = connection.createStatement())
        {
          String sql = "SELECT p.First as First, p.Last as Last, v.value as Value"
                       + " FROM ABMultiValue v, ABPerson p"
                       + " WHERE v.record_id = p.ROWID"
                       + "  AND v.value IS NOT NULL";
          try (ResultSet resultSet = statement.executeQuery(sql))
          {
            while(resultSet.next())
            {
              String first = resultSet.getString("First");
              String last = resultSet.getString("Last");
              String value = resultSet.getString("Value");

              String fullname = createFullname(first, last);
              if(fullname != null)
              {
                contactMap.putIfAbsent(fullname, new Contact(fullname));

                Contact contact = contactMap.get(fullname);
                if(value.contains("@"))
                {
                  // email
                  contact.addEmail(value);
                }
                else if(!value.contains("http") && !value.contains("itunes"))
                {
                  // hopefully a phone number
                  addPhoneNumber(value, contact::addPhone);
                }
              }
              else
              {
                LOGGER.debug("contact has no name");
              }
            }
          }
        }
      }
      return new HashSet<>(contactMap.values());
    }
    else
    {
      throw new IllegalArgumentException(String.format("can not read from %s", contactBackupDB.getAbsolutePath()));
    }
  }

  private static String createFullname(String first, String last)
  {
    if(first != null && !first.isEmpty() && last != null && !last.isEmpty())
    {
      return first + " " + last;
    }
    else if(first != null && !first.isEmpty())
    {
      return first;
    }
    else if(last != null && !last.isEmpty())
    {
      return last;
    }
    else
    {
      return null;
    }
  }

  private void addPhoneNumber(String value, LongConsumer applyPhoneNumberFunction)
  {
    value = value.trim();

    if(value.startsWith("+"))
    {
      value = value.substring(1);
    }
    else if(!value.isEmpty() && !value.startsWith("0") && Character.isDigit(value.charAt(0)))
    {
      // local number
      value = areaPhoneCode + value;
    }

    if(value.startsWith("0"))
    {
      // prefix county number
      value = countryPhoneCode + value.substring(1);
    }

    value = cleanup(value, " ");
    value = cleanup(value, "\u00a0");
    value = cleanup(value, "-");
    value = cleanup(value, "/");

    try
    {
      applyPhoneNumberFunction.accept(Long.parseLong(value));
    }
    catch(NumberFormatException e)
    {
      LOGGER.debug(String.format("can not parse '%s' as phone number", value), e);
    }
  }

  private static String cleanup(String value, String s)
  {
    while(value.contains(s))
    {
      value = value.replace(s, "");
    }

    return value;
  }
}
