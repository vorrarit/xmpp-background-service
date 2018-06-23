<b>xmpp-server</b><br/>
It contains shell script to run either openfire or ejabberd. Ejabberd is recommended because it's more stable and does not sent repeat message.  Ejabberd users are set through environment variable.  However, for openfire, you will need to do web install on the first run.

<b>xmpp-pc-client</b><br/>
<p>
Contain pc code to connect to XMPP Server.  The connection port is based on ejabbered.  You can change to openfire port if you like. 
</p>

<p>
KtXmppClient is the main class.  To send message, use the cli style input 's user2 good morning'.  s is for 'send'. user2 is the target recipient. All string at the back will be treated as message.
</p>

<p>
KtGcmAppServer is working as GCM app server.  It connects to GCM and listen for messages sent from xmpp-android-client
</p>

<b>xmpp-android-client</b><br/>
Android app for testing XMPP background service.  It connects to xmpp-server as 'user2' and listen for message and show the notification.  It use GCM as online debug message, so that I don't need to connect my mobile phone with Android Studio all the time.

