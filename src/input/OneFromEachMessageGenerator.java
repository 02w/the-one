/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package input;

import core.Settings;
import core.SettingsError;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Message creation -external events generator. Creates one message from every source node (defined
 * with {@link MessageEventGenerator#HOST_RANGE_S}) to one of the destination nodes (defined with
 * {@link MessageEventGenerator#TO_HOST_RANGE_S}). The message size, first messages time and the
 * intervals between creating messages can be configured like with {@link MessageEventGenerator}.
 * End time is not respected, but messages are created until every from-node has created a message.
 *
 * @see MessageEventGenerator
 */
public class OneFromEachMessageGenerator extends MessageEventGenerator {
  private final List<Integer> fromIds;

  public OneFromEachMessageGenerator(Settings s) {
    super(s);
    this.fromIds = new ArrayList<>();

    if (this.toHostRange == null) {
      throw new SettingsError("Destination host (" + MessageEventGenerator.TO_HOST_RANGE_S + ") must be defined");
    }
    for (int i = this.hostRange[0]; i < this.hostRange[1]; i++) {
      this.fromIds.add(i);
    }
    Collections.shuffle(this.fromIds, this.rng);
  }

  /**
   * Returns the next message creation event
   *
   * @see input.EventQueue#nextEvent()
   */
  @Override
  public ExternalEvent nextEvent() {
    int responseSize = 0; /* no responses requested */
    int from;
    int to;

    from = this.fromIds.remove(0);
    to = this.drawToAddress(this.toHostRange, -1);

    if (to == from) {
        /* skip self */
      if (this.fromIds.size() == 0) {
          /* oops, no more from addresses */
        this.nextEventsTime = Double.MAX_VALUE;
        return new ExternalEvent(Double.MAX_VALUE);
      } else {
        from = this.fromIds.remove(0);
      }
    }

    if (this.fromIds.size() == 0) {
      this.nextEventsTime = Double.MAX_VALUE; /* no messages left */
    } else {
      this.nextEventsTime += this.drawNextEventTimeDiff();
    }

    MessageCreateEvent mce =
        new MessageCreateEvent(
            from, to, this.getID(), this.drawMessageSize(), responseSize, this.nextEventsTime);

    return mce;
  }
}
