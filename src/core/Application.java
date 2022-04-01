/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package core;

import java.util.List;

/**
 * Base class for applications. Nodes that have an application running will forward all incoming
 * messages to the application <code>handle()</code> method before they are processed further. The
 * application can change the properties of the message before returning it or return null to signal
 * to the router that it wants the message to be dropped.
 *
 * <p>In addition, the application's <code>update()</code> method is called every simulation cycle.
 *
 * <p>Configuration of application is done by picking a unique application instance name (e.g.,
 * mySimpleApp) and setting its <code>type</code> property to the concrete application class: <code>
 * mySimpleApp.type = SimpleApplication
 * </code>. These application instances can be assigned to node groups using the <code>
 * Group.application</code> setting: <code>Group1.application =
 * mySimpleApp</code>.
 *
 * @author mjpitka
 * @author teemuk
 */
public abstract class Application {

  public String appID = null;
  private List<ApplicationListener> aListeners = null;

  public Application() {}

  /**
   * Copy constructor.
   *
   * @param app
   */
  public Application(Application app) {
    this.aListeners = app.getAppListeners();
    this.appID = app.appID;
  }

  /**
   * Initializes the application when a MessageRouter is created. Hooked into {@link
   * routing.MessageRouter#init}.
   *
   * @param host The host this application instance is attached to.
   */
  public void init(DTNHost host) {}

  /**
   * Called every simulation cycle. This method is not aware of the appID set in {@link
   * core.Message}, so it is called for all messages.
   *
   * @param host The host this application instance is attached to.
   */
  public void update(DTNHost host) {}

  /**
   * Called before msg is sent, hooked into {@link routing.ActiveRouter#startTransfer}. Only
   * applications corresponding to the message's appID are called.
   *
   * @param msg The outgoing message.
   * @param host The host this application instance is attached to.
   * @param anotherHost The host this message is being sent to.
   * @return the modified message, or null if the message should not be sent.
   */
  public Message beforeSending(Message msg, DTNHost host, DTNHost anotherHost) {
    return msg;
  }

  /**
   * This method handles application functionality related to processing of the bundle. Application
   * handles a messages, which arrives to the node hosting this application. After performing
   * application specific handling, this method returns a list of messages. If node wishes to
   * continue forwarding the incoming. Only applications corresponding to the message's appID are
   * called.
   *
   * @param msg The incoming message.
   * @param host The host this application instance is attached to.
   * @return the (possibly modified) message to forward or <code>null</code> if the application
   *     wants the router to stop forwarding the message.
   */
  public Message handle(Message msg, DTNHost host) {
    return msg;
  }

  /**
   * Called when router gets notified about connection change, hooked into {@link
   * routing.MessageRouter#changedConnection}. This method is not aware of the appID set in {@link
   * core.Message}, so it is called for all messages.
   *
   * @param con the connection that changed.
   * @param host the host this application instance is attached to.
   */
  public void onChangedConnection(Connection con, DTNHost host) {}

  /**
   * Returns a unique application ID. The application will only receive messages with this
   * application ID. If the AppID is set to <code>null</code> the application will receive all
   * messages.
   *
   * @return Application ID.
   */
  public String getAppID() {
    return this.appID;
  }

  /**
   * Sets the application ID. Should only set once when the application is created. Changing the
   * value during simulation runtime is not recommended unless you really know what you're doing.
   *
   * @param appID the application ID
   */
  public void setAppID(String appID) {
    this.appID = appID;
  }

  public abstract Application replicate();

  public List<ApplicationListener> getAppListeners() {
    return this.aListeners;
  }

  public void setAppListeners(List<ApplicationListener> aListeners) {
    this.aListeners = aListeners;
  }

  /**
   * Sends an event to all listeners.
   *
   * @param event The event to send.
   * @param params Any additional parameters to send.
   * @param host The host which where the app is running.
   */
  public void sendEventToListeners(String event, Object params, DTNHost host) {
    for (ApplicationListener al : this.aListeners) {
      al.gotEvent(event, params, this, host);
    }
  }
}
