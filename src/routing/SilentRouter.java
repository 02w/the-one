package routing;

import core.DTNHost;
import core.Message;
import core.Settings;

/**
 * A router which does not send or receive any messages. We use this router simply to get
 * connection information.
 */
public class SilentRouter extends MessageRouter {

  public SilentRouter(Settings s) {
    super(s);
  }

  protected SilentRouter(SilentRouter r) {
    super(r);
  }

  @Override
  public int receiveMessage(Message m, DTNHost from) {
    return MessageRouter.DENIED_POLICY;
  }

  @Override
  public MessageRouter replicate() {
    return new SilentRouter(this);
  }
}
