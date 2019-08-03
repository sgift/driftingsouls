package DSBot;

// Einladungslink: https://discordapp.com/oauth2/authorize?&client_id=606503142627999784&scope=bot&permissions=0

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.security.auth.login.LoginException;


//Discord Bot
import net.dv8tion.jda.client.entities.Group;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class Kolonialamt extends ListenerAdapter
{

   /**
   * This is the method where the program starts.
   */
  public static void main(String[] args)
  {
      //We construct a builder for a BOT account. If we wanted to use a CLIENT account
      // we would use AccountType.CLIENT
      try
      {
          JDA jda = new JDABuilder("NjA2NTAzMTQyNjI3OTk5Nzg0.XUWaig.BafPifSfwMDCmV1Kh-SmKrJH2Sk")         // The token of the account that is logging in.
                  .addEventListener(new Kolonialamt())  // An instance of a class that will handle events.
                  .build();
          jda.awaitReady(); // Blocking guarantees that JDA will be completely loaded.
          System.out.println("Finished Building JDA!");   
      }
      catch (LoginException e)
      {
          //If anything goes wrong in terms of authentication, this is the exception that will represent it
          e.printStackTrace();
      }
      catch (InterruptedException e)
      {
          //Due to the fact that awaitReady is a blocking method, one which waits until JDA is fully loaded,
          // the waiting can be interrupted. This is the exception that would fire in that situation.
          //As a note: in this extremely simplified example this will never occur. In fact, this will never occur unless
          // you use awaitReady in a thread that has the possibility of being interrupted (async thread usage and interrupts)
          e.printStackTrace();
      }
  }
  /**
   * NOTE THE @Override!
   * This method is actually overriding a method in the ListenerAdapter class! We place an @Override annotation
   *  right before any method that is overriding another to guarantee to ourselves that it is actually overriding
   *  a method from a super class properly. You should do this every time you override a method!
   *
   * As stated above, this method is overriding a hook method in the
   * {@link net.dv8tion.jda.core.hooks.ListenerAdapter ListenerAdapter} class. It has convience methods for all JDA events!
   * Consider looking through the events it offers if you plan to use the ListenerAdapter.
   *
   * In this example, when a message is received it is printed to the console.
   *
   * @param event
   *          An event containing information about a {@link net.dv8tion.jda.core.entities.Message Message} that was
   *          sent in a channel.
   */
  @Override
  public void onMessageReceived(MessageReceivedEvent event) 
  {
      
      //These are provided with every event in JDA
      JDA jda = event.getJDA();                       //JDA, the core of the api.
      long responseNumber = event.getResponseNumber();//The amount of discord events that JDA has received since the last reconnect.

      //Event specific information
      User author = event.getAuthor();                //The user that sent the message
      Message message = event.getMessage();           //The message that was received.
      MessageChannel channel = event.getChannel();    //This is the MessageChannel that the message was sent to.
                                                      //  This could be a TextChannel, PrivateChannel, or Group!
      

      String msg = message.getContentDisplay();              //This returns a human readable version of the Message. Similar to
                                                      // what you would see in the client.

      boolean bot = author.isBot();                    //This boolean is useful to determine if the User that
                                                      // sent the Message is a BOT or not!
      String name = author.getName();
      if (event.isFromType(ChannelType.TEXT))         //If this message was sent to a Guild TextChannel
      {
          //Because we now know that this message was sent in a Guild, we can do guild specific things
          // Note, if you don't check the ChannelType before using these methods, they might return null due
          // the message possibly not being from a Guild!

          Guild guild = event.getGuild();             //The Guild that this message was sent in. (note, in the API, Guilds are Servers)
          TextChannel textChannel = event.getTextChannel(); //The TextChannel that this message was sent to.
          Member member = event.getMember();          //This Member that sent the message. Contains Guild specific information about the User!

          
          if (message.isWebhookMessage())
          {
              name = author.getName();                //If this is a Webhook message, then there is no Member associated
          }                                           // with the User, thus we default to the author for name.
          else
          {
              name = member.getEffectiveName();       //This will either use the Member's nickname if they have one,
          }                                           // otherwise it will default to their username. (User#getName())

          System.out.printf("(%s)[%s]<%s>: %s\n", guild.getName(), textChannel.getName(), name, msg);
      }
      else if (event.isFromType(ChannelType.PRIVATE)) //If this message was sent to a PrivateChannel
      {
          //The message was sent in a PrivateChannel.
          //In this example we don't directly use the privateChannel, however, be sure, there are uses for it!
          PrivateChannel privateChannel = event.getPrivateChannel();

          System.out.printf("[PRIV]<%s>: %s\n", author.getName(), msg);
      }
      else if (event.isFromType(ChannelType.GROUP))   //If this message was sent to a Group. This is CLIENT only!
      {
          //The message was sent in a Group. It should be noted that Groups are CLIENT only.
          Group group = event.getGroup();
          String groupName = group.getName() != null ? group.getName() : "";  //A group name can be null due to it being unnamed.

          System.out.printf("[GRP: %s]<%s>: %s\n", groupName, author.getName(), msg);
      }


      //Now that you have a grasp on the things that you might see in an event, specifically MessageReceivedEvent,
      // we will look at sending / responding to messages!
      //This will be an extremely simplified example of command processing.

      //Remember, in all of these .equals checks it is actually comparing
      // message.getContentDisplay().equals, which is comparing a string to a string.
      // If you did message.equals() it will fail because you would be comparing a Message to a String!
      if (msg.toLowerCase().trim().equals("!ping")&&!bot)
      {
          //This will send a message, "pong!", by constructing a RestAction and "queueing" the action with the Requester.
          // By calling queue(), we send the Request to the Requester which will send it to discord. Using queue() or any
          // of its different forms will handle ratelimiting for you automatically!

          channel.sendMessage("pong!").queue();
      }
      else if (msg.toLowerCase().trim().startsWith("!help"))
      {
        channel.sendMessage("Willkommen beim Kolonialamt. \nAktuell befinden wir uns in einer Umbauphase. Wenden Sie sich bei Fragen dazu an den Kanal #ds-help. \nVielen Dank").queue();
      }
      else if ((msg.toLowerCase().contains("moin") ||  msg.contains("Tag"))&&!bot){
        channel.sendMessage("Seid gegrüßt, Siedler! Ich wünsche einen ruhigen Tag.").queue();
      }
      else if (msg.toLowerCase().trim().startsWith("!bier")&&!bot)
      {
        channel.sendMessage("Willkommen in der Kolonialamtsbar. Genießen Sie ihr kühles Bier, Siedler.").queue();
      }

      else if (msg.toLowerCase().trim().startsWith("!roll")&&!bot)
      {
          //In this case, we have an example showing how to use the Success consumer for a RestAction. The Success consumer
          // will provide you with the object that results after you execute your RestAction. As a note, not all RestActions
          // have object returns and will instead have Void returns. You can still use the success consumer to determine when
          // the action has been completed!

          Random rand = new Random();
          int roll = rand.nextInt(6) + 1; //This results in 1 - 6 (instead of 0 - 5)
          channel.sendMessage("Your roll: " + roll).queue(sentMessage ->  //This is called a lambda statement. If you don't know
          {                                                               // what they are or how they work, try google!
              if (roll < 3)
              {
                  channel.sendMessage("The roll for messageId: " + sentMessage.getId() + " wasn't very good... Must be bad luck!\n").queue();
              }
          });
      }
      else if (msg.toLowerCase().trim().equals("re"))
      {
    	  channel.sendMessage("Willkommen zurück, Siedler.").queue();
      }
      
  }
 }

