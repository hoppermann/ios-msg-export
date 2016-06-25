package msgexport;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Contact
  implements Comparable<Contact>
{
  private final String name;
  private final Set<Long> phones = new HashSet<>();
  private final Set<String> emails = new HashSet<>();

  public Contact(String name)
  {
    this.name = name;
  }

  public void addPhone(Long phone)
  {
    phones.add(phone);
  }

  public void addEmail(String email)
  {
    emails.add(email);
  }

  public String getName()
  {
    return name;
  }

  public Set<Long> getPhones()
  {
    return phones;
  }

  public Set<String> getEmails()
  {
    return emails;
  }

  @Override
  public boolean equals(Object o)
  {
    if(this == o)
    {
      return true;
    }
    if(o == null || getClass() != o.getClass())
    {
      return false;
    }
    Contact contact = (Contact) o;
    return Objects.equals(name, contact.name) &&
           Objects.equals(phones, contact.phones) &&
           Objects.equals(emails, contact.emails);
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(name, phones, emails);
  }

  @Override
  public int compareTo(Contact o)
  {
    return name.compareTo(o.name);
  }

  @Override
  public String toString()
  {
    return "Contact{" +
           "name='" + name + '\'' +
           ", phones=" + phones +
           ", emails=" + emails +
           '}';
  }
}
